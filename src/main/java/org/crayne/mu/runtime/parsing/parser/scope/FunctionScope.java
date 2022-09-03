package org.crayne.mu.runtime.parsing.parser.scope;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.crayne.mu.lang.EqualOperation;
import org.crayne.mu.lang.LocalVariable;
import org.crayne.mu.lang.Module;
import org.crayne.mu.lang.Variable;
import org.crayne.mu.runtime.parsing.ast.Node;
import org.crayne.mu.runtime.parsing.ast.NodeType;
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

    public void addLocalVariable(@NotNull final Parser parser, @NotNull final Variable var) {
        final String name = var.name();
        final Variable existingVar = localVariable(parser, Token.of(name), false);
        if (existingVar != null) {
            parser.parserError("A variable with name '" + name + "' already exists in this scope.",
                    "Enclose the already existing variable into its own local scope or rename it.",
                    "Renaming your new variable works too.");
            return;
        }
        localVariables.add(new LocalVariable(var, this));
    }

    public void scopeEnd() {
        localVariables.clear();
    }

    public boolean localVariableValue(@NotNull final Parser parser, @NotNull final Token identifierTok, @NotNull final ValueParser.TypedNode value, @NotNull final EqualOperation eq) {
        final LocalVariable var = localVariable(parser, identifierTok);
        if (var == null) return false;

        if (immutable(parser, var, identifierTok)) return false;
        if (!ValueParser.validVarset(value.type(), var.type())) {
            parser.parserError("Cannot assign value of type " + value.type().getName() + " to variable with type " + var.type().getName(), identifierTok);
            return false;
        }
        final Node newVal = eq == EqualOperation.EQUAL
                ? new Node(NodeType.VALUE, value.node())
                : new Node(NodeType.VALUE,
                        new Node(NodeType.of(eq),
                                new Node(NodeType.IDENTIFIER, Token.of(var.name())),
                                new Node(NodeType.VALUE,
                                        value.node()
                                )
                        )
                );

        var.changedAt(this);
        return true;
    }

    public LocalVariable localVariable(@NotNull final Parser parser, @NotNull final Token identifierTok) {
        return localVariable(parser, identifierTok, true);
    }

    public LocalVariable localVariable(@NotNull final Parser parser, @NotNull final Token identifierTok, final boolean panic) {
        final String identifier = identifierTok.token();
        final LocalVariable var = Variable.findLocalVariableByName(localVariables, identifier);

        if (var == null) {
            if (parent == null) {
                if (panic) handleUndefinedLocalVariable(parser, identifierTok);
                return null;
            }
            // search in the parent scope, because you can do that and find local vars there
            return parent.localVariable(parser, identifierTok, panic);
        }
        return var;
    }

    private boolean immutable(@NotNull final Parser parser, @NotNull final LocalVariable var, @NotNull final Token identifierTok) {
        if (var.isConstant()) {
            final FunctionScope changedAt = var.changedAt();
            if (changedAt == null) return false; /*
                                                    allow changing constants if they have not been initialized yet (if both declaration and definition are in same actual scope):
                                                    int i; // (yes, this is constant because everything here is constant by default, so add 'mut' to make it mutable)
                                                    i = 5;
                                                    */

            final ScopeType changedAtType = changedAt.type;
            if (changedAtType == ScopeType.NORMAL || changedAtType == ScopeType.FUNCTION) {
                parser.parserError("Local variable '" + var.name() + "' is constant and has already been assigned to, cannot not change value.", identifierTok);
                return true;
            }
            if (changedAtType == ScopeType.IF) {
                int indent = actualIndent;
                FunctionScope searchElseParent = this;
                while (indent >= changedAt.actualIndent) {
                    searchElseParent = searchElseParent.parent;
                    indent = searchElseParent.actualIndent;
                }
                if (searchElseParent.type == ScopeType.ELSE) return false;
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
        final Module globalMod = parser.findModuleFromIdentifier(identifier, identifierTok, false);
        if (globalMod != null && globalMod.findVariableByName(ParserEvaluator.identOf(identifier)) != null) return;

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
                ", module=" + module +
                ", type=" + type +
                '}';
    }
}
