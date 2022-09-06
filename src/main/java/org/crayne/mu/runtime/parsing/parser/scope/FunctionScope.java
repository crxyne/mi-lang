package org.crayne.mu.runtime.parsing.parser.scope;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.crayne.mu.lang.*;
import org.crayne.mu.lang.Module;
import org.crayne.mu.runtime.parsing.lexer.Token;
import org.crayne.mu.runtime.parsing.parser.Parser;
import org.crayne.mu.runtime.parsing.parser.ParserEvaluator;
import org.crayne.mu.runtime.parsing.parser.ValueParser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FunctionScope extends Scope {

    private final List<LocalVariable> localVariables;
    private final FunctionScope parent;
    private final Module module;
    private boolean reachedEnd;

    public FunctionScope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent, final FunctionScope parent, @NotNull final Module module) {
        super(type, scopeIndent, actualIndent);
        this.localVariables = new ArrayList<>();
        this.parent = parent;
        this.module = module;
    }

    public FunctionScope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent, @NotNull final List<LocalVariable> localVariables, final FunctionScope parent, @NotNull final Module module) {
        super(type, scopeIndent, actualIndent);
        this.localVariables = localVariables;
        this.parent = parent;
        this.module = module;
    }

    public ScopeType type() {
        return type;
    }

    public List<LocalVariable> localVariables() {
        return localVariables;
    }

    private static boolean isConditionalScope(@NotNull final ScopeType type) {
        return type == ScopeType.IF || type == ScopeType.FOR || type == ScopeType.WHILE;
    }

    public void reachedEnd() {
        if (parent != null) {
            FunctionScope scope = this;
            while (!isConditionalScope(scope.type)) {
                scope = scope.parent;
                if (scope == null) break;
                scope.reachedEnd();
            }
        }
        reachedEnd = true;
    }

    public boolean hasReachedEnd() {
        return reachedEnd;
    }

    public void addLocalVariable(@NotNull final Parser parser, @NotNull final Variable var) {
        final String name = var.name();
        final Optional<LocalVariable> existingVar = localVariable(parser, Token.of(name), false);
        if (existingVar.isPresent()) {
            parser.parserError("A variable with name '" + name + "' already exists in this scope.",
                    "Enclose the already existing variable into its own local scope or rename it.",
                    "Renaming your new variable works too.");
            return;
        }
        localVariables.add(new LocalVariable(var, this));
    }

    public void scopeEnd(@NotNull final Parser parser) {
        if (!reachedEnd && parent == null) {
            parser.parserError("Missing 'ret' or '::' statement", "Every branch in a function has to return some value, except in void functions");
            return;
        }
        localVariables.clear();
    }

    public boolean localVariableValue(@NotNull final Parser parser, @NotNull final Token identifierTok, @NotNull final ValueParser.TypedNode value, @NotNull final EqualOperation eq) {
        final Optional<Variable> ovar = localVariable(parser, identifierTok);
        if (ovar.isEmpty()) return false;

        final LocalVariable var = (LocalVariable) ovar.get();
        if (eq != EqualOperation.EQUAL && !var.initialized()) {
            parser.parserError("Variable '" + identifierTok.token() + "' might not have been initialized yet", identifierTok, "Set the value of the variable upon declaration");
            return false;
        }

        if (immutable(parser, var, identifierTok)) return false;
        if (!ValueParser.validVarset(value.type(), var.type())) {
            parser.parserError("Cannot assign value of type " + value.type().getName() + " to variable with type " + var.type().getName(), identifierTok);
            return false;
        }
        final FunctionScope searchNotNormal = anyAbnormal();
        var.changedAt(searchNotNormal);
        return true;
    }

    private FunctionScope anyAbnormal() {
        FunctionScope searchNotNormal = this;
        while (searchNotNormal.type == ScopeType.NORMAL) {
            searchNotNormal = searchNotNormal.parent;
        }
        return searchNotNormal;
    }

    private FunctionScope anyEqualIndent(@NotNull final FunctionScope at) {
        int indent = actualIndent;
        FunctionScope searchParent = this;
        while (indent > at.actualIndent) {
            searchParent = searchParent.parent;
            indent = searchParent.actualIndent;
        }
        return searchParent;
    }

    public Optional<Variable> localVariable(@NotNull final Parser parser, @NotNull final Token identifierTok) {
        return Optional.ofNullable(localVariable(parser, identifierTok, true).orElse(null));
    }

    public Optional<LocalVariable> localVariable(@NotNull final Parser parser, @NotNull final Token identifierTok, final boolean panic) {
        final String identifier = identifierTok.token();
        final Optional<LocalVariable> var = Variable.findLocalVariableByName(localVariables, identifier);

        if (var.isEmpty()) {
            if (parent == null) {
                if (panic) handleUndefinedLocalVariable(parser, identifierTok);
                return var;
            }
            // search in the parent scope, because you can do that and find local vars there
            return parent.localVariable(parser, identifierTok, panic);
        }
        return var;
    }

    private boolean immutable(@NotNull final Parser parser, @NotNull final LocalVariable var, @NotNull final Token identifierTok) {
        if (var.isConstant()) {
            final FunctionScope searchNotNormal = anyAbnormal();
            final Optional<FunctionScope> ochangedAt = var.changedAt();
            if (ochangedAt.isEmpty()) {
                switch (searchNotNormal.type) {
                    case WHILE, FOR, DO -> {
                        parser.parserError("Local variable '" + var.name() + "' is constant and may have been changed already, cannot change value.", identifierTok);
                        return true;
                    }
                }
                if (searchNotNormal.type != ScopeType.IF) var.initialize();
                return false;
            }                                    /* allow changing constants if they have not been initialized yet (if both declaration and definition are in same actual scope):
                                                    int i; // (yes, this is constant because everything here is constant by default, so add 'mut' to make it mutable)
                                                    i = 5; */

            final FunctionScope changedAt = ochangedAt.get();
            final ScopeType changedAtType = changedAt.type;
            if (changedAtType == ScopeType.NORMAL || changedAtType == ScopeType.FUNCTION) {
                parser.parserError("Local variable '" + var.name() + "' is constant and has already been assigned to, cannot not change value.", identifierTok);
                return true;
            }
            if (changedAtType == ScopeType.IF) {
                final FunctionScope searchElseParent = anyEqualIndent(changedAt);
                if (searchElseParent.type == ScopeType.ELSE) {
                    if (searchNotNormal.type != ScopeType.IF) var.initialize();
                    return false;
                }
                // allow changing constants that changed in conditional 'if', only if the change happens inside of an 'else' directly linked to the conditional
            }
            parser.parserError("Local variable '" + var.name() + "' is constant and may have been changed already, cannot change value.", identifierTok);
            return true;
        }
        return false;
    }

    private void handleUndefinedLocalVariable(@NotNull final Parser parser, @NotNull final Token identifierTok) {
        final String identifier = identifierTok.token();
        // if we couldn't find any local variable, check if theres a global one (if there is, return from the function with 'false' with no error so the parser can do the rest)
        final Optional<Module> globalMod = parser.findModuleFromIdentifier(identifier, identifierTok, false);
        if (globalMod.isPresent() && globalMod.get()
                .findVariableByName(ParserEvaluator.identOf(identifier))
                .isPresent()) return;

        final Optional<String> closestMatch = localVariables
                .stream()
                .map(Variable::name)
                .filter(s -> {
                    final Integer dist = new LevenshteinDistance().apply(s, identifier);
                    return dist != null && dist < 3;
                })
                .findFirst();

        // suggest variable names that resemble the wanted variable name (if there was a typo/spelling mistake)
        if (closestMatch.isPresent()) {
            parser.parserError("Cannot find variable '" + identifier + "'.", identifierTok, "Did you mean '" + closestMatch.get() + "'?");
            return;
        }
        parser.parserError("Cannot find variable '" + identifier + "'.", identifierTok);
    }

    @Override
    public String toString() {
        return "FunctionScope{" +
                "localVariables=" + localVariables +
                ", parent=" + parent +
                ", actualIndent=" + actualIndent +
                ", type=" + type +
                '}';
    }
}
