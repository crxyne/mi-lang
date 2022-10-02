package org.crayne.mi.parsing.parser;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mi.lang.*;
import org.crayne.mi.lang.Module;
import org.crayne.mi.log.MessageHandler;
import org.crayne.mi.parsing.ast.Node;
import org.crayne.mi.parsing.ast.NodeType;
import org.crayne.mi.util.SyntaxTree;
import org.crayne.mi.parsing.lexer.Token;
import org.crayne.mi.parsing.parser.scope.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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
    private Module parentModule = new Module("!PARENT", 0, null);
    private ParserEvaluator evaluator;
    public boolean encounteredError = false;
    public boolean skimming = true;
    protected int scopeIndent = 0;
    protected int actualIndent = 0;
    protected Datatype currentFuncReturnType = null;
    protected final int stdlibFinishLine;
    protected boolean stdlib = true;
    private final String code;
    protected Module currentParsingModule;
    private final File inputFile;

    public Module currentParsingModule() {
        return currentParsingModule;
    }

    private final List<Scope> currentScope = new ArrayList<>() {{
        add(new Scope(ScopeType.PARENT, 0, 0));
    }};

    public Parser(@NotNull final MessageHandler output, @NotNull final List<Token> tokens, final int stdlibFinishLine, @NotNull final String code, final File inputFile) {
        this.output = output;
        this.tokens = tokens;
        if (stdlibFinishLine == -1) {
            output.errorMsg("Cannot find STANDARDLIB_MI_FINISH_CODE anywhere in the code, please contact the developer of your standard library to fix this issue.");
            encounteredError = true;
        }
        this.code = code;
        this.stdlibFinishLine = stdlibFinishLine;
        this.inputFile = inputFile;
        evaluator = new ParserEvaluator(this);
    }

    public SyntaxTree parse() {
        encounteredError = false;
        evaluator = new ParserEvaluator(this);
        skim();
        final Node parent = new Node(NodeType.PARENT);
        currentParsingModule = parentModule;
        while (currentTokenIndex < tokens.size() && !encounteredError) {
            final Node statement = parseStatement(false);
            if (statement == null) break;
            parent.addChildren(statement);
        }
        if (encounteredError) return null;
        final SyntaxTree result = new SyntaxTree(parent, output, code, stdlibFinishLine, inputFile);
        parentModule = new Module("!PARENT", 0, null);
        return result;
    }

    public void reset() {
        currentTokenIndex = 0;
        currentToken = null;
        scopeIndent = 0;
        actualIndent = 0;
        currentScope.clear();
        currentScope.add(new Scope(ScopeType.PARENT, 0, 0));
        skimming = false;
    }

    public void skim() {
        reset();
        skimming = true;
        while (currentTokenIndex < tokens.size() && !encounteredError) {
            parseStatement(false);
        }
        if (!currentScope.isEmpty() && scopeIndent != 0) {
            parserError("Expected '}', unfinished scope", tokens.get(currentTokenIndex - 1));
        }
        reset();
    }

    public Node parseStatement(final boolean expectedUnscopedWhile) {
        final List<Token> statement = new ArrayList<>();
        final Optional<Scope> current = scope();
        Token previous = null;
        iter: for (; currentTokenIndex < tokens.size() && !encounteredError; currentTokenIndex++) {
            currentToken = tokens.get(currentTokenIndex);
            statement.add(currentToken);
            switch (currentToken.token()) {
                case SEMI, SCOPE_BEGIN -> {break iter;}
                case SCOPE_END -> {
                    if (statement.size() == 1) break iter;

                    if (current.isPresent() && current.get() instanceof EnumScope) {
                        statement.remove(statement.size() - 1);
                        statement.add(Token.of(";"));
                        return evalStatement(statement, expectedUnscopedWhile);
                    }
                    if (previous == null) {
                        parserError("Expected ';' after statement");
                        break iter;
                    }
                    parserError("Expected ';' after statement", previous);
                    break iter;
                }
            }
            previous = currentToken;
        }
        skipToken();
        return evalStatement(statement, expectedUnscopedWhile);
    }

    private void skipToken() {
        currentTokenIndex++;
        currentToken = currentTokenIndex < tokens.size() ? tokens.get(currentTokenIndex) : null;
    }

    private int scopeEndCheck = 0;
    protected ClassScope currentClass;

    public Node evalStatement(@NotNull final List<Token> tokens, final boolean expectedUnscopedWhile) {
        if (tokens.isEmpty()) return null;
        Node result = null;

        final Token lastToken = getAny(tokens, tokens.size() - 1);
        final Token firstToken = getAny(tokens, 0);
        if (anyNull(lastToken, firstToken)) return null;

        final String last = lastToken.token();
        final NodeType first = NodeType.of(firstToken);

        final Optional<Scope> curScope = scope();
        if (tokens.size() != 1 && !last.equals(SCOPE_END) && curScope.isPresent() && curScope.get() instanceof final FunctionScope functionScope) {
            if (functionScope.unreachable()) {
                parserError("Unreachable statement", firstToken, "Delete the unreachable statement or move it before the last statement");
                return null;
            }
        }
        switch (last) {
            case SCOPE_BEGIN -> {
                if (skimming) {
                    skimStatement(tokens, first);
                }
                scopeEndCheck++;
                final List<String> using = curScope.isEmpty() || !(curScope.get() instanceof FunctionScope) ? null : ((FunctionScope) curScope.get()).using();
                result = evalScoped(tokens, first, Collections.emptyList());
                scopeIndent++;
                if (result == null) {
                    parserError("Could not parse scoped statement", firstToken);
                    return null;
                }
                if (result.type() != NodeType.NOOP) actualIndent++;
                parseScope(result);

                switch (result.type()) {
                    case FUNCTION_DEFINITION -> evaluator.addFunctionFromResult(result);
                    //case CREATE_CLASS -> evaluator.addClassFromResult(result);
                    case IF_STATEMENT -> {
                        if (NodeType.of(currentToken) == NodeType.LITERAL_ELSE) {
                            skipToken();
                            scope(ScopeType.ELSE);
                            if (using == null) return null;
                            scopeIndent++;
                            actualIndent++;
                            final Node elseStatement = parseStatement(false);
                            if (elseStatement == null) return null;
                            result.addChildren(new Node(NodeType.ELSE_STATEMENT, currentToken.actualLine(), new Node(NodeType.SCOPE, currentToken.actualLine(), elseStatement)));
                        }
                    }
                    case DO_STATEMENT -> {
                        if (NodeType.of(currentToken) != NodeType.LITERAL_WHILE) {
                            parserError("Expected 'while' after 'do' statement scope", currentToken);
                            return null;
                        }
                        final Node whileStatement = parseStatement(true);
                        if (whileStatement == null) return null;
                        if (whileStatement.type() != NodeType.WHILE_STATEMENT_UNSCOPED || whileStatement.children().size() != 1) {
                            parserError("Expected ';' after 'while' statement of 'do' statement scope");
                            return null;
                        }
                        result.addChildren(whileStatement.child(0));
                    }
                }
            }
            case SEMI -> {
                result = evalUnscoped(tokens, first, Collections.emptyList());
                if (result != null) {
                    if (result.type() == NodeType.WHILE_STATEMENT_UNSCOPED && !expectedUnscopedWhile) {
                        parserError("Expected '{' after 'while' statement", lastToken);
                        return null;
                    }
                    final Optional<Scope> current = scope();
                    if (current.isPresent()) {
                        final Scope scope = current.get();
                        if (scope instanceof FunctionScope) {
                            switch (result.type()) {
                                case VAR_DEFINITION, VAR_DEF_AND_SET_VALUE -> evaluator.addLocalVarFromResult(result);
                            }
                        } else switch (current.get().type()) {
                            case MODULE, PARENT, CLASS -> {
                                if (skimming) {
                                    switch (result.type()) {
                                        case VAR_DEFINITION -> {
                                            final List<Modifier> modifiers = result.child(0).children().stream().map(n -> Modifier.of(n.type())).toList();
                                            if (Variable.isConstant(modifiers) && current.get().type() != ScopeType.CLASS)
                                                parserError("Expected value, global constant might not have been initialized yet", lastToken);
                                            evaluator.addGlobalVarFromResult(result);
                                        }
                                        case VAR_DEF_AND_SET_VALUE -> evaluator.addGlobalVarFromResult(result);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (result == null && tokens.size() != 1 && !last.equals(SCOPE_END)) {
            parserError("Not a statement.", tokens.get(tokens.size() - 1));
        }
        if (tokens.size() == 1 && last.equals(SCOPE_END)) {
            scopeEndCheck--;
            if (scopeEndCheck < 0 && skimming) {
                parserError("Unexpected token '}'", lastToken);
                return null;
            }
        }
        return result;
    }

    public Node evalScoped(@NotNull final List<Token> tokens, @NotNull final NodeType first, @NotNull final List<Node> modifiers) {
        if (tokens.size() == 1) {
            final Optional<Scope> current = scope();
            if (current.isEmpty() || !(current.get() instanceof FunctionScope)) {
                parserError("Unexpected local scope outside of function scope");
                return null;
            }
            scope(ScopeType.NORMAL);
            return new Node(NodeType.NOOP);
        }
        if (first.isModifier()) return evalScopedWithModifiers(tokens);
        return switch (first) {
            case LITERAL_MODULE -> evaluator.evalModuleDefinition(tokens, modifiers);
            case LITERAL_FN -> evaluator.evalFunctionDefinition(tokens, modifiers);
            case LITERAL_IF -> evaluator.evalIfStatement(tokens, modifiers);
            case LITERAL_WHILE -> evaluator.evalWhileStatement(tokens, modifiers, false);
            //case LITERAL_CLASS -> evaluator.evalClassDefinition(tokens, modifiers);
            case LITERAL_NEW -> evaluator.evalNewStatement(tokens, modifiers);
            case LITERAL_FOR -> evaluator.evalForStatement(tokens, modifiers);
            case LITERAL_DO -> evaluator.evalDoStatement(tokens, modifiers);
            case LITERAL_ENUM -> evaluator.evalEnumDefinition(tokens, modifiers);
            case LITERAL_ELSE -> {
                parserError("Unexpected token 'else' without 'if' scope", tokens.get(0));
                yield null;
            }
            default -> null;
        };
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
            case STANDARDLIB_MI_FINISH_CODE -> evaluator.evalStdLibFinish(tokens, modifiers);
            case IDENTIFIER -> evaluator.evalFirstIdentifier(tokens, modifiers);
            case LITERAL_WHILE -> evaluator.evalWhileStatement(tokens, modifiers, true);
            case LITERAL_FN -> evaluator.evalFunctionDefinition(tokens, modifiers);
            case LITERAL_RET -> evaluator.evalReturnStatement(tokens, modifiers);
            case LITERAL_USE -> evaluator.evalUseStatement(tokens, modifiers);
            case LITERAL_BREAK -> evaluator.evalBreak(tokens, modifiers);
            case LITERAL_CONTINUE -> evaluator.evalContinue(tokens, modifiers);
            case LITERAL_ELSE -> {
                parserError("Unexpected token 'else' without 'if' scope", tokens.get(0));
                yield null;
            }
            default -> null;
        };
    }

    public Optional<Module> findModuleFromIdentifier(@NotNull String identifier, @NotNull final Token identifierTok, final boolean panic) {
        if (identifier.endsWith(".")) {
            parserError("Expected identifier after '.'", identifierTok, true);
            return Optional.empty();
        }
        char prev = 0;
        for (int i = 0; i < identifier.length(); i++) {
            final char current = identifier.charAt(i);
            if (prev == '.' && current == '.') {
                parserError("Unexpected token '.'", identifierTok.line(), identifierTok.column() + i);
                return Optional.empty();
            }
            prev = current;
        }
        if (lastModule() != null && identifier.startsWith(lastModule().fullName())) identifier = identifier.substring(lastModule().fullName().length());

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
                return Optional.empty();
            }
            parserError("Cannot find module '" + moduleAsString + "'", identifierTok,
                    "If you were not trying to use a module at root level, try to prefix the module with '.'. That way, the parser will search submodules relative to your current module.",
                    "The parser goes in order from top to bottom. It is suggested to have all needed submodules at the top of your module and the functions below it.");
            return Optional.empty();
        }
        return Optional.ofNullable(foundModule);
    }

    public ParserEvaluator evaluator() {
        return evaluator;
    }

    public void checkAccessValidity(@NotNull final Module module, @NotNull final IdentifierType type, @NotNull final Token identifier, @NotNull final List<Modifier> modifiers) {
        final Module currentModule = lastModule();

        if (module == currentModule) return;
        if (modifiers.isEmpty() || modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.OWN)) return;
        if (modifiers.contains(Modifier.PRIVATE)) { // we have checked before if were already in the same module but this wasnt the case at this point anymore
            parserError(type + " " + identifier.token() + " is private, cannot access from other modules", identifier);
            return;
        }
        if (modifiers.contains(Modifier.PROTECTED)) {
            if (module.parent() == currentModule.parent() || module.parent() == currentModule) return;
            parserError(type + " " + identifier.token() + " is protected, can only access from same parent or module, or if the accessor is in the parent module", identifier);
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
        final Token identifier = getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token lbrace = getAndExpect(tokens, 2, NodeType.LBRACE);
        if (anyNull(identifier, lbrace)) return;

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

    public static boolean anyNull(final Token... tok) {
        return Arrays.stream(tok).anyMatch(Objects::isNull);
    }

    protected void scope(@NotNull final ScopeType type) {
        final Optional<Scope> current = scope();

        if (type == ScopeType.ENUM) enumScope();
        else if (type == ScopeType.CLASS) classScope();
        else if (type == ScopeType.FOR || type == ScopeType.WHILE || type == ScopeType.DO) loopScope(type);
        else if (current.isPresent() && current.get() instanceof FunctionScope) functionScope(type);
        else currentScope.add(new Scope(type, scopeIndent + 1, actualIndent + 1));
    }

    protected void functionRootScope(@NotNull final FunctionDefinition definition) {
        currentScope.add(new FunctionScope(ScopeType.FUNCTION, scopeIndent + 1, actualIndent + 1, null, definition));
    }

    private void loopScope(@NotNull final ScopeType type) {
        final Optional<Scope> current = scope();

        current.ifPresent(scope -> {
            final FunctionScope functionScope = (FunctionScope) scope;
            currentScope.add(new LoopScope(type, scopeIndent + 1, actualIndent + 1, (FunctionScope) scope, functionScope.using()));
        });
    }

    private void enumScope() {
        currentScope.add(new EnumScope(ScopeType.ENUM, scopeIndent + 1, actualIndent + 1));
    }

    private void classScope() {
        currentScope.add(new ClassScope(ScopeType.CLASS, scopeIndent + 1, actualIndent + 1));
    }

    private void functionScope(@NotNull final ScopeType type) {
        final Optional<Scope> current = scope();

        current.ifPresent(scope -> {
            final FunctionScope functionScope = (FunctionScope) scope;

            if (scope instanceof final LoopScope loopScope) currentScope.add(new LoopScope(type, scopeIndent + 1, actualIndent + 1, loopScope, functionScope.using()));
            else currentScope.add(new FunctionScope(type, scopeIndent + 1, actualIndent + 1, Collections.emptyList(), (FunctionScope) scope, functionScope.using()));
        });
    }

    public Optional<Scope> scope() {
        return Optional.ofNullable(currentScope.isEmpty() ? null : currentScope.get(currentScope.size() - 1));
    }

    private static Optional<Token> tryAndGet(@NotNull final List<Token> tokens, final int index) {
        return Optional.ofNullable(tokens.size() > 0 && index < tokens.size() && index >= 0 ? tokens.get(index) : null);
    }

    public Token getAny(@NotNull final List<Token> tokens, final int index) {
        return tryAndGet(tokens, index).orElse(null);
    }

    public Token getAndExpect(@NotNull final List<Token> tokens, final int index, @NotNull final NodeType... toBe) {
        return getAndExpect(tokens, index, false, toBe);
    }

    public Token getAndExpect(@NotNull final List<Token> tokens, final int index, final boolean skipToEnd, @NotNull final NodeType... toBe) {
        final Optional<Token> tok = tryAndGet(tokens, index);
        final boolean unexpected = expect(tok.orElse(null), skipToEnd, toBe) || tok.isEmpty();
        return unexpected ? null : tok.get();
    }

    public boolean expect(final Token token, final boolean skipToEnd, @NotNull final NodeType... toBe) {
        if (token != null && List.of(toBe).contains(NodeType.of(token))) return false;
        parserError("Expected " + helperTypeToString(toBe), thisOrCurrent(token), skipToEnd);
        return true;
    }

    public Token thisOrCurrent(final Token token) {
        return token == null ? currentToken : token;
    }

    public Datatype currentFuncReturnType() {
        return currentFuncReturnType;
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
                final Node statement = parseStatement(false);
                if (statement == null) break;
                parent.addChildren(statement);
                return;
            }
            final Node statement = parseStatement(false);
            if (statement == null) break;
            scope.addChildren(statement);
        }
        parent.addChildren(scope);
    }

    public Module lastModule() {
        return currentModule.isEmpty() ? parentModule : currentModule.get(currentModule.size() - 1);
    }

    public Module parentModule() {
        return parentModule;
    }

    public Token currentToken() {
        return currentToken;
    }

    public void closeScope() {
        final Optional<Scope> ocurrent = scope();
        if (currentScope.isEmpty() || ocurrent.isEmpty()) {
            parserError("Unexpected token '}'");
            return;
        }
        boolean removeFakeScope = false;
        final Scope current = ocurrent.get();
        final ScopeType type = current.type();
        scopeIndent--;
        if (type != ScopeType.NORMAL) actualIndent--;
        if (type == ScopeType.FOR || type == ScopeType.ELSE) {
            removeFakeScope = true;
        }
        current.scopeEnd(this);
        if (type == ScopeType.FUNCTION) currentFuncReturnType = null;

        currentScope.remove(currentScope.size() - 1);
        if (removeFakeScope) closeScope(); // for loops have a hidden scope wrapped around them, which doesnt actually exist in the code. similarly for else statements, which always have a hidden scope next to them

        if (type != ScopeType.MODULE) return;
        if (!skimming) currentParsingModule = currentParsingModule.parent();
        closeModule();
    }

    public void closeModule() {
        final Module lastModule = lastModule();
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
            case LITERAL_NAT -> NodeType.LITERAL_INTERN;
            case LITERAL_INTERN -> NodeType.LITERAL_NAT;
            case LITERAL_NULLABLE -> NodeType.LITERAL_NONNULL;
            case LITERAL_NONNULL -> NodeType.LITERAL_NULLABLE;
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
        if (encounteredError) return;
        output.astHelperError(message, line, column, stdlibFinishLine, stdlib, quickFixes);
        encounteredError = true;
    }

    public void parserError(@NotNull final String message, @NotNull final Token token, @NotNull final String... quickFixes) {
        parserError(message, token, false, quickFixes);
    }

    public void parserError(@NotNull final String message, @NotNull final Token token, final boolean skipToEndOfToken, @NotNull final String... quickFixes) {
        if (encounteredError) return;
        if (token.line() == -1 || token.column() == -1) {
            parserError(message, currentToken, skipToEndOfToken, quickFixes);
            return;
        }
        if (!stdlib) {
            output.astHelperError(message, token.line(), token.column() + (skipToEndOfToken ? token.token().length() : 0), stdlibFinishLine, false, quickFixes);
        } else {
            output.astHelperError("StandardLib error encountered, please contact the developer of this standard library to fix this issue:\n" + message,
                    token.actualLine(), token.column() + (skipToEndOfToken ? token.token().length() : 0), 1, true, quickFixes);
        }
        encounteredError = true;
    }

    public void parserWarning(@NotNull final String message, @NotNull final Token token, @NotNull final String... quickFixes) {
        if (encounteredError) return;
        if (token.line() == -1 || token.column() == -1) {
            parserWarning(message, currentToken, quickFixes);
            return;
        }
        if (!stdlib) {
            output.astHelperWarning(message, token.line(), token.column(), stdlibFinishLine, false, quickFixes);
        } else {
            output.astHelperWarning(message, token.actualLine(), token.column(), 1, true, quickFixes);
        }
    }
}
