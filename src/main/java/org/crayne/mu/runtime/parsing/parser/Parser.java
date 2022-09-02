package org.crayne.mu.runtime.parsing.parser;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mu.lang.Module;
import org.crayne.mu.lang.*;
import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.runtime.parsing.ast.Node;
import org.crayne.mu.runtime.parsing.ast.NodeType;
import org.crayne.mu.runtime.parsing.lexer.Token;
import org.crayne.mu.runtime.parsing.parser.scope.FunctionScope;
import org.crayne.mu.runtime.parsing.parser.scope.Scope;
import org.crayne.mu.runtime.parsing.parser.scope.ScopeType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Parser {

    private static final String SCOPE_BEGIN = "{";
    private static final String SCOPE_END = "}";
    private static final String SEMI = ";";

    protected final MessageHandler output;
    private Token currentToken;
    private int currentTokenIndex = 0;
    private final List<Token> tokens;
    private final List<Module> currentModule = new ArrayList<>();
    private Module buildCurrentModule = null;
    private final Module parentModule = new Module("!PARENT", 0, null);
    private ParserEvaluator evaluator;
    protected boolean encounteredError = false;
    protected boolean skimming = true;
    private int scopeIndent = 0;
    private int actualIndent = 0;
    protected final int stdlibFinishLine;
    protected boolean stdlib = true;

    private final List<Scope> currentScope = new ArrayList<>() {{
        add(new Scope(ScopeType.PARENT, 0, 0));
    }};

    public Parser(@NotNull final MessageHandler output, @NotNull final List<Token> tokens, final int stdlibFinishLine) {
        this.output = output;
        this.tokens = tokens;
        if (stdlibFinishLine == -1) {
            output.errorMsg("Cannot find STANDARDLIB_MU_FINISH_CODE anywhere in the code, please contact the developer of your standard library to fix this issue.");
            encounteredError = true;
        }
        this.stdlibFinishLine = stdlibFinishLine;
        evaluator = new ParserEvaluator(this);
    }

    public Node parse() {
        encounteredError = false;
        evaluator = new ParserEvaluator(this);
        skim();
        final Node parent = new Node(NodeType.PARENT);
        while (currentTokenIndex < tokens.size() && !encounteredError) {
            final Node statement = parseStatement();
            if (statement == null) break;
            parent.addChildren(statement);
        }
        if (encounteredError) return null;
        System.out.println(parentModule);
        return parent;
    }

    public void skim() {
        skimming = true;
        while (currentTokenIndex < tokens.size() && !encounteredError) {
            parseStatement();
        }
        currentTokenIndex = 0;
        currentToken = null;
        scopeIndent = 0;
        currentScope.clear();
        currentScope.add(new Scope(ScopeType.PARENT, 0, 0));

        skimming = false;
    }

    public Node parseStatement() {
        final List<Token> statement = new ArrayList<>();
        iter: for (; currentTokenIndex < tokens.size() && !encounteredError; currentTokenIndex++) {
            currentToken = tokens.get(currentTokenIndex);
            statement.add(currentToken);
            switch (currentToken.token()) {
                case SEMI, SCOPE_BEGIN, SCOPE_END -> {break iter;}
            }
        }
        currentTokenIndex++;
        return evalStatement(statement);
    }

    public Node evalStatement(@NotNull final List<Token> tokens) {
        if (tokens.isEmpty()) return null;
        Node result = null;

        final Token lastToken = tryAndGet(tokens, tokens.size() - 1);
        final Token firstToken = tryAndGet(tokens, 0);
        if (lastToken == null || firstToken == null) return null;

        final String last = lastToken.token();
        final NodeType first = NodeType.of(firstToken);

        switch (last) {
            case SCOPE_BEGIN -> {
                if (skimming) {
                    skimStatement(tokens, first);
                }
                result = evalScoped(tokens, first, Collections.emptyList());
                scopeIndent++;
                if (result == null) {
                    parserError("Could not parse scoped statement", firstToken);
                    return null;
                }
                if (result.type() != NodeType.NOOP) actualIndent++;
                parseScope(result);
                if (skimming && result.type() == NodeType.FUNCTION_DEFINITION) {
                    evaluator.addFunctionFromResult(result);
                }
            }
            case SEMI -> {
                result = evalUnscoped(tokens, first, Collections.emptyList());
                if (skimming && result != null) {
                    final Scope current = scope();
                    if (current != null) {
                        switch (current.type()) {
                            case MODULE, PARENT -> {
                                switch (result.type()) {
                                    case VAR_DEFINITION -> {
                                        final List<Modifier> modifiers = result.child(0).children().stream().map(n -> Modifier.of(n.type())).toList();
                                        if (Variable.isConstant(modifiers))
                                            parserError("Expected value, global constant might not have been initialized yet");
                                        evaluator.addGlobalVarFromResult(result);
                                    }
                                    case VAR_DEF_AND_SET_VALUE -> evaluator.addGlobalVarFromResult(result);
                                }
                            }
                            case FUNCTION -> {
                                switch (result.type()) {
                                    case VAR_DEFINITION, VAR_DEF_AND_SET_VALUE -> evaluator.addLocalVarFromResult(result);
                                }
                            }
                        }

                    }
                }
            }
        }
        if (result == null && tokens.size() != 1 && !last.equals(SCOPE_END)) {
            parserError("Not a statement.");
        }
        return result;
    }

    public Node evalScoped(@NotNull final List<Token> tokens, @NotNull final NodeType first, @NotNull final List<Node> modifiers) {
        if (tokens.size() == 1) {
            scope(ScopeType.NORMAL);
            return new Node(NodeType.NOOP);
        }
        if (first.isModifier()) return evalScopedWithModifiers(tokens);
        switch (first) {
            case LITERAL_MODULE -> {
                return evaluator.evalModuleDefinition(tokens, modifiers);
            }
            case LITERAL_FN -> {
                return evaluator.evalFunctionDefinition(tokens, modifiers);
            }
        }
        return null;
    }

    public Node evalScopedWithModifiers(@NotNull final List<Token> tokens) {
        final List<Node> modifiers = parseModifiers(tokens);
        final List<Token> withoutModifiers = tokens.subList(modifiers.size(), tokens.size());

        if (withoutModifiers.isEmpty()) {
            parserError("Not a statement");
            return null;
        }
        if (encounteredError) return null;

        return evalScoped(withoutModifiers, NodeType.of(withoutModifiers.get(0)), modifiers);
    }

    public Node evalUnscoped(@NotNull final List<Token> tokens, @NotNull final NodeType first, @NotNull final List<Node> modifiers) {
        if (first.isModifier()) return evalUnscopedWithModifiers(tokens);
        if (first.isDatatype()) return evaluator.evalVariableDefinition(tokens, modifiers);
        return switch (first) {
            case STANDARDLIB_MU_FINISH_CODE -> evaluator.evalStdLibFinish(tokens, modifiers);
            case IDENTIFIER -> evaluator.evalFirstIdentifier(tokens, modifiers);
            default -> null;
        };
    }

    public Module findModuleFromIdentifier(@NotNull String identifier, @NotNull final Token identifierTok, final boolean panic) {
        if (identifier.endsWith(".")) {
            parserError("Expected identifier after '.'", identifierTok, true);
            return null;
        }
        char prev = 0;
        for (int i = 0; i < identifier.length(); i++) {
            final char current = identifier.charAt(i);
            if (prev == '.' && current == '.') {
                parserError("Unexpected token '.'", identifierTok.line(), identifierTok.column() + i);
                return null;
            }
            prev = current;
        }

        final String moduleAsString = StringUtils.substringBeforeLast(identifier, ".");
        final boolean relative = identifier.charAt(0) == '.';
        Module foundModule = relative ? lastModule() : null;
        if (relative) identifier = identifier.substring(1);
        final String[] split = identifier.split("\\.");

        if (split.length == 1) {
            foundModule = lastModule();
        } else {
            for (int i = 0; i < split.length - 1; i++) {
                final String mod = split[i];
                if (foundModule == null) {
                    if (i == 0) {
                        foundModule = Module.findModuleByName(parentModule.subModules(), mod);
                        continue;
                    }
                    break;
                }
                foundModule = Module.findModuleByName(foundModule.subModules(), mod);
            }
        }
        if (panic && foundModule == null) {
            if (relative) {
                parserError("Cannot find submodule '" + moduleAsString + "'", identifierTok,
                        "The parser goes in order from top to bottom. It is suggested to have all needed submodules at the top of your module and the functions below it.");
                return null;
            }
            parserError("Cannot find module '" + moduleAsString + "'", identifierTok,
                    "If you were not trying to use a module at root level, try to prefix the module with '.'. That way, the parser will search submodules relative to your current module.",
                    "The parser goes in order from top to bottom. It is suggested to have all needed submodules at the top of your module and the functions below it.");
            return null;
        }
        return foundModule;
    }

    public ParserEvaluator evaluator() {
        return evaluator;
    }

    protected void checkAccessValidity(@NotNull final Module module, @NotNull final IdentifierType type, @NotNull final String identifier, @NotNull final List<Modifier> modifiers) {
        final Module currentModule = lastModule();

        if (module == currentModule) return;
        if (modifiers.isEmpty() || modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.OWN)) return;
        if (modifiers.contains(Modifier.PRIVATE)) { // we have checked before if were already in the same module but this wasnt the case at this point anymore
            parserError(type + " " + identifier + " is private, cannot access from other modules");
            return;
        }
        if (modifiers.contains(Modifier.PROTECTED)) {
            if (module.parent() == currentModule.parent() || module.parent() == currentModule) return;
            parserError(type + " " + identifier + " is protected, can only access from same parent or module, or if the accessor is in the parent module");
        }
    }

    public Node evalUnscopedWithModifiers(@NotNull final List<Token> tokens) {
        final List<Node> modifiers = parseModifiers(tokens);
        final List<Token> withoutModifiers = tokens.subList(modifiers.size(), tokens.size());

        if (withoutModifiers.isEmpty()) {
            parserError("Not a statement");
            return null;
        }
        if (encounteredError) return null;

        return evalUnscoped(withoutModifiers, NodeType.of(withoutModifiers.get(0)), modifiers);
    }

    public void skimStatement(@NotNull final List<Token> tokens, @NotNull final NodeType first) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (first) {
            case LITERAL_MODULE -> skimModuleDefinition(tokens);
        }
    }

    public void skimModuleDefinition(@NotNull final List<Token> tokens) {
        final Token identifier = tryAndGet(tokens, 1);
        if (expect(identifier, identifier, NodeType.IDENTIFIER)) return;
        final Token lbrace = tryAndGet(tokens, 2);
        if (expect(lbrace, lbrace, NodeType.LBRACE)) return;

        assert identifier != null;
        final Module mod = new Module(identifier.token(), scopeIndent, lastModule());
        final Module parent = lastModule();
        if (parent != null) {
            if (parent.findSubmoduleByName(mod.name())) {
                parserError("Redefinition of submodule \"" + mod.name() + "\" inside parent module \"" + parent.name() + "\".", identifier,
                        "Renaming the module should fix this error.");
            }
        } else {
            if (Module.foundModuleByName(parentModule.subModules(), mod.name())) {
                parserError("Redefinition of module \"" + mod.name() + "\".", identifier,
                        "Renaming the module should fix this error.");
            }
        }
        if (buildCurrentModule == null) buildCurrentModule = mod;
        currentModule.add(mod);
    }

    protected void scope(@NotNull final ScopeType type) {
        if (skimming) {
            if (type == ScopeType.FUNCTION) currentScope.add(new FunctionScope(type, null, lastModule()));
            else if (scope() instanceof FunctionScope) currentScope.add(new FunctionScope(type, (FunctionScope) scope(), lastModule()));
            else currentScope.add(new Scope(type, scopeIndent + 1, actualIndent + 1));
        }
    }

    protected Scope scope() {
        return currentScope.isEmpty() ? null : currentScope.get(currentScope.size() - 1);
    }

    public static Token tryAndGet(@NotNull final List<Token> tokens, final int index) {
        return tokens.size() > 0 ? tokens.get(index) : null;
    }

    public boolean expect(final Token token, final Token at, @NotNull final NodeType... toBe) {
        if (token != null && List.of(toBe).contains(NodeType.of(token))) return false;
        parserError("Expected " + helperTypeToString(toBe), at, false);
        return true;
    }

    public boolean expect(final Token token, final Token at, final boolean skipToEnd, @NotNull final NodeType... toBe) {
        if (token != null && List.of(toBe).contains(NodeType.of(token))) return false;
        parserError("Expected " + helperTypeToString(toBe), at, skipToEnd);
        return true;
    }

    public String helperTypeToString(@NotNull final NodeType... type) {
        if (type.length == 1) return helperTypeToString(type[0]);
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < type.length; i++) {
            final boolean isLast = i + 1 >= type.length;
            final boolean isFirst = i == 0;
            builder.append(!isFirst ? (isLast ? " or " : ", ") : "").append(type[i]);
        }
        return builder.toString();
    }

    public String helperTypeToString(@NotNull final NodeType type) {
        final boolean addQuotes = type.getAsString() != null;
        return (addQuotes ? "'" : "") + type.toString().toLowerCase() + (addQuotes ? "'" : "");
    }

    public void unexpected(final Token token) {
        if (token == null) return;
        parserError("Unexpected token '" + token.token() + "'", token);
    }

    public void parseScope(@NotNull final Node parent) {
        final Node scope = new Node(NodeType.SCOPE);
        while (currentTokenIndex < tokens.size()) {
            currentToken = tokens.get(currentTokenIndex);
            if (currentToken.token().equals(SCOPE_END)) {
                closeScope();
                final Node statement = parseStatement();
                if (statement == null) break;
                parent.addChildren(statement);
                return;
            }
            final Node statement = parseStatement();
            if (statement == null) break;
            scope.addChildren(statement);
        }
        parent.addChildren(scope);
    }

    public Module lastModule() {
        return currentModule.isEmpty() ? parentModule : currentModule.get(currentModule.size() - 1);
    }

    public void closeScope() {
        scopeIndent--;
        final Scope current = scope();
        if (current != null && current.type() != ScopeType.NORMAL) actualIndent--;
        if (skimming) {
            if (currentScope.isEmpty()) {
                parserError("Unexpected token '}'");
                return;
            }
            currentScope.remove(currentScope.size() - 1);
        }
        final Module lastModule = lastModule();
        if (current != null && current.type() != ScopeType.MODULE) return;
        if (buildCurrentModule != null) {
            if (scopeIndent != 0) {
                final Module parent = lastModule.parent();
                parent.addSubmodule(lastModule);
            } else {
                parentModule.addSubmodule(buildCurrentModule);
                buildCurrentModule = null;
            }
        }
        if (!currentModule.isEmpty()) currentModule.remove(currentModule.size() - 1);
    }

    private static NodeType oppositeModifier(@NotNull final NodeType modifier) {
        return switch (modifier) {
            case LITERAL_MUT -> NodeType.LITERAL_CONST;
            case LITERAL_CONST -> NodeType.LITERAL_MUT;
            case LITERAL_PUB -> NodeType.LITERAL_PRIV;
            case LITERAL_PRIV -> NodeType.LITERAL_PUB;
            case LITERAL_OWN -> NodeType.LITERAL_PROT;
            case LITERAL_PROT -> NodeType.LITERAL_OWN;
            default -> null;
        };
    }

    protected boolean findConflictingModifiers(@NotNull final List<NodeType> modifiers, @NotNull final NodeType newModifier, @NotNull final Token at) {
        final String modif = newModifier.getAsString();
        if (modifiers.contains(newModifier)) {
            parserError("Unexpected token '" + modif + "', duplicate modifier, cannot have '" + modif + "' twice", at);
            return true;
        }
        final String oppositeModif = oppositeModifier(newModifier).getAsString();
        if (newModifier == NodeType.LITERAL_OWN || newModifier == NodeType.LITERAL_PROT) {
            if (modifiers.contains(NodeType.LITERAL_PUB) || modifiers.contains(NodeType.LITERAL_PRIV) || modifiers.contains(oppositeModifier(newModifier))) {
                parserError("Unexpected token '" + modif + "', conflicting modifiers '" + modif + "' and '" + oppositeModif + "', 'pub' or 'priv'\"", at);
                return true;
            }
            if (newModifier == NodeType.LITERAL_OWN && modifiers.contains(NodeType.LITERAL_CONST)) {
                parserError("Unexpected token '" + modif + "', conflicting modifiers 'own' and 'const'\"", at);
                return true;
            }
            return false;
        }
        if (modifiers.contains(NodeType.LITERAL_OWN) && (newModifier == NodeType.LITERAL_CONST)) {
            parserError("Unexpected token '" + modif + "', conflicting modifiers '" + modif + "' and 'own'", at);
            return true;
        }
        if (modifiers.contains(NodeType.LITERAL_PROT)) {
            parserError("Unexpected token '" + modif + "', conflicting modifiers '" + modif + "' and 'prot'", at);
            return true;
        }
        if (modifiers.contains(oppositeModifier(newModifier))) {
            parserError("Unexpected token '" + modif + "', conflicting modifiers '" + modif + "' and '" + oppositeModif + "'", at);
            return true;
        }
        return false;
    }

    private List<Node> parseModifiers(@NotNull final List<Token> statement) {
        final List<Node> result = new ArrayList<>();
        for (@NotNull final Token token : statement) {
            final NodeType type = NodeType.of(token);
            if (!type.isModifier()) break;
            if (findConflictingModifiers(result.stream().map(Node::type).toList(), type, token)) return new ArrayList<>();
            result.add(Node.of(token));
        }
        return result;
    }

    public void parserError(@NotNull final String message, @NotNull final String... quickFixes) {
        parserError(message, currentToken, quickFixes);
    }

    public void parserError(@NotNull final String message, final int line, final int column, @NotNull final String... quickFixes) {
        output.astHelperError(message, line, column, stdlibFinishLine, quickFixes);
        encounteredError = true;
    }

    public void parserError(@NotNull final String message, @NotNull final Token token, @NotNull final String... quickFixes) {
        parserError(message, token, false, quickFixes);
    }

    public void parserError(@NotNull final String message, @NotNull final Token token, final boolean skipToEndOfToken, @NotNull final String... quickFixes) {
        if (!stdlib) {
            output.astHelperError(message, token.line(), token.column() + (skipToEndOfToken ? token.token().length() : 0), stdlibFinishLine, quickFixes);
        } else {
            output.astHelperError("StandardLib error encountered, please contact the developer of this standard library to fix this issue:\n" + message,
                    token.actualLine(), token.column() + (skipToEndOfToken ? token.token().length() : 0), 1, quickFixes);
        }
        encounteredError = true;
    }


}
