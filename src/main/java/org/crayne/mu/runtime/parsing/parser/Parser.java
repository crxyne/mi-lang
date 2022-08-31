package org.crayne.mu.runtime.parsing.parser;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mu.lang.Module;
import org.crayne.mu.lang.*;
import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.runtime.parsing.ast.Node;
import org.crayne.mu.runtime.parsing.ast.NodeType;
import org.crayne.mu.runtime.parsing.lexer.Token;
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
    protected boolean encounteredError = false;
    private boolean skimming = true;
    private int scopeIndent = 0;
    protected final int stdlibFinishLine;

    private boolean stdlib = true;

    private final List<Scope> currentScope = new ArrayList<>() {{
        add(new Scope(ScopeType.PARENT, 0));
    }};

    public Parser(@NotNull final MessageHandler output, @NotNull final List<Token> tokens, final int stdlibFinishLine) {
        this.output = output;
        this.tokens = tokens;
        if (stdlibFinishLine == -1) {
            output.errorMsg("Cannot find STANDARDLIB_MU_FINISH_CODE anywhere in the code, please contact the developer of your standard library to fix this issue.");
            encounteredError = true;
        }
        this.stdlibFinishLine = stdlibFinishLine;
    }

    public Node parse() {
        encounteredError = false;
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
        currentScope.add(new Scope(ScopeType.PARENT, 0));

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

        final String last = tryAndGet(tokens, tokens.size() - 1).token();
        final Token first = tryAndGet(tokens, 0);
        final NodeType first_t = NodeType.of(first);

        switch (last) {
            case SCOPE_BEGIN -> {
                if (skimming) {
                    skimStatement(tokens, first_t);
                }
                result = evalScoped(tokens, first_t, Collections.emptyList());
                scopeIndent++;
                if (result == null) {
                    parserError("Could not parse scoped statement", first);
                    return null;
                }
                parseScope(result);
                if (skimming && result.type() == NodeType.FUNCTION_DEFINITION) {
                    addFunctionFromResult(result);
                }
            }
            case SEMI -> {
                result = evalUnscoped(tokens, first_t, Collections.emptyList());
                if (skimming && result != null) {
                    final Scope current = scope();
                    if (current == null || current.type() == ScopeType.MODULE || current.type() == ScopeType.PARENT) {
                        switch (result.type()) {
                            case VAR_DEFINITION -> {
                                final List<Modifier> modifiers = result.child(0).children().stream().map(n -> Modifier.of(n.type())).toList();
                                if (Variable.isConstant(modifiers))
                                    parserError("Expected value, global constant might not have been initialized yet");
                                addGlobalVarFromResult(result);
                            }
                            case VAR_DEF_AND_SET_VALUE -> addGlobalVarFromResult(result);
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

    private void addGlobalVarFromResult(@NotNull final Node result) {
        final Module module = lastModule();
        final List<Modifier> modifiers = result.child(0).children().stream().map(n -> Modifier.of(n.type())).toList();

        final Scope current = scope();
        if (!stdlib && (current == null || current.type() != ScopeType.MODULE)) {
            parserError("Cannot define global variables at root level",
                    "Move your variable into a module or create a new module for it");
            return;
        }

        if (result.children().size() == 4) {
            module.addGlobalVariable(new Variable(
                    result.child(1).value().token(),
                    NodeType.of(result.child(2).value()).getAsDataType(),
                    modifiers,
                    result.child(3)
            ));
            return;
        }
        module.addGlobalVariable(new Variable(
                result.child(1).value().token(),
                NodeType.of(result.child(2).value()).getAsDataType(),
                modifiers,
                null
        ));
    }

    private void addFunctionFromResult(@NotNull final Node result) {
        final Module module = lastModule();
        try {
            final List<Modifier> modifiers = result.child(2).children().stream().map(n -> Modifier.of(n.type())).toList();

            final List<Node> paramNodes = result.child(3).children().stream().toList();
            final List<FunctionParameter> params = paramNodes.stream().map(n -> new FunctionParameter(
                    Datatype.valueOf(n.child(0).value().token().toUpperCase()),
                    n.child(1).value().token(),
                    n.child(2).children().stream().map(n2 -> Modifier.of(n2.type())).toList()
            )).toList();

            if (result.children().size() == 5) {
                module.addFunction(new FunctionDefinition(
                        result.child(0).value().token(),
                        Datatype.valueOf(result.child(1).value().token().toUpperCase()),
                        params,
                        modifiers,
                        result.child(4)
                ));
                return;
            }
            module.addFunction(new FunctionDefinition(
                    result.child(0).value().token(),
                    Datatype.valueOf(result.child(1).value().token().toUpperCase()),
                    params,
                    modifiers
            ));
        } catch (final NullPointerException e) {
            e.printStackTrace();
            output.errorMsg("Could not parse function definition");
        }
    }

    public Node evalScoped(@NotNull final List<Token> tokens, @NotNull final NodeType first, @NotNull final List<Node> modifiers) {
        if (first.isModifier()) return evalScopedWithModifiers(tokens);
        switch (first) {
            case LITERAL_MODULE -> {
                return evalModuleDefinition(tokens, modifiers);
            }
            case LITERAL_FN -> {
                return evalFunctionDefinition(tokens, modifiers);
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
        if (first.isDatatype()) return evalVariableDefinition(tokens, modifiers);
        return switch (first) {
            case STANDARDLIB_MU_FINISH_CODE -> evalStdLibFinish(tokens, modifiers);
            case IDENTIFIER -> evalFirstIdentifier(tokens, modifiers);
            default -> null;
        };
    }

    public Node evalFirstIdentifier(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (tokens.size() <= 1) return null;
        final NodeType second = NodeType.of(tryAndGet(tokens, 1));
        if (second == NodeType.LPAREN) return evalFunctionCall(tokens, modifiers);
        else if (second == NodeType.SET) return evalVariableChange(tokens, modifiers);
        return null;
    }

    public Node evalVariableChange(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;
        return null;
    }

    public Node evalFunctionCall(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token identifierTok = tryAndGet(tokens, 0);
        if (expect(identifierTok, identifierTok, NodeType.IDENTIFIER)) return null;
        final List<ValueParser.TypedNode> params = parseParametersCallFunction(tokens.subList(2, tokens.size() - 2));

        if (skimming) {
            final String identifier = identifierTok.token();
            final String moduleAsString = StringUtils.substringBeforeLast(identifier, ".");
            final Module functionModule = findModuleFromIdentifier(identifier, identifierTok);
            if (functionModule == null) return null;
            final String function = identifier.contains(".") ? StringUtils.substringAfterLast(identifier, ".") : identifier;
            final FunctionConcept funcConcept = functionModule.findFunctionConceptByName(function);

            if (funcConcept == null) {
                parserError("Cannot find any function called '" + function + "' in module '" + (moduleAsString.isEmpty() ? lastModule().name() : moduleAsString) + "'", identifierTok.line(), identifierTok.column() + moduleAsString.length() + 1);
                return null;
            }
            final FunctionDefinition def = funcConcept.definitionByCallParameters(params);

            if (def == null) {
                if (params.isEmpty()) {
                    parserError("Cannot find any implementation for function '" + function + "' with no arguments", identifierTok, true);
                    return null;
                }
                parserError("Cannot find any implementation for function '" + function + "' with argument types " + callArgsToString(params), identifierTok, true);
                return null;
            }
            checkAccessValidity(functionModule, def, IdentifierType.FUNCTION, identifierTok);
        }
        return new Node(NodeType.FUNCTION_CALL,
                new Node(NodeType.IDENTIFIER, identifierTok),
                new Node(NodeType.PARAMETERS, params.stream().map(n -> new Node(NodeType.VALUE, n.node())).toList())
        );
    }

    private String callArgsToString(@NotNull final List<ValueParser.TypedNode> params) {
        return String.join(", ", params.stream().map(n -> n.type().getName().toLowerCase()).toList());
    }

    public List<ValueParser.TypedNode> parseParametersCallFunction(@NotNull final List<Token> tokens) {
        return ValueParser.parseParametersCallFunction(tokens, this);
    }

    private Module findModuleFromIdentifier(@NotNull String identifier, @NotNull final Token identifierTok) {
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
        if (foundModule == null) {
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

     private <T> void checkAccessValidity(@NotNull final Module module, @NotNull final T identifier, @NotNull final IdentifierType type, @NotNull final Token identifierTok) {
        if (!skimming) return;
        final Module currentModule = lastModule();
        if (currentModule == parentModule) return; // at root level access to anything is allowed, because only the stdlib can do this anyway

        switch (type) {
            case FUNCTION -> {
                if (!(identifier instanceof final FunctionDefinition def)) throw new IllegalArgumentException("Expected FunctionDefinition as identifier");
                final List<Modifier> modifiers = def.modifiers();
                checkBasicAccessValidity(module, type, def.name(), modifiers);
            }
            default -> {
                parserError("Access checking not implemented yet");
                return;
            }
        }
    }

    private void checkBasicAccessValidity(@NotNull final Module module, @NotNull final IdentifierType type, @NotNull final String identifier, @NotNull final List<Modifier> modifiers) {
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

    public Node evalVariableDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final Token identifier = tryAndGet(tokens, 1);
        if (expect(identifier, identifier, NodeType.IDENTIFIER)) return null;
        final Token equalsOrSemi = tryAndGet(tokens, 2);
        if (expect(equalsOrSemi, equalsOrSemi, NodeType.SET, NodeType.SEMI)) return null;
        final Token datatype = tokens.get(0);
        final boolean indefinite = NodeType.of(datatype) == NodeType.QUESTION_MARK;

        if (NodeType.of(equalsOrSemi) == NodeType.SEMI) {
            if (indefinite) {
                parserError("Unexpected token '?', expected a definite datatype", datatype,
                        "The '?' cannot be used as a datatype when there is no value directly specified, so change the datatype to a definite.");
                return null;
            }

            return new Node(NodeType.VAR_DEFINITION,
                    new Node(NodeType.MODIFIERS, modifiers),
                    new Node(NodeType.IDENTIFIER, identifier),
                    new Node(NodeType.TYPE, datatype)
            );
        }
        final ValueParser.TypedNode value = parseExpression(tokens.subList(3, tokens.size() - 1));
        final Node finalType = indefinite ? new Node(NodeType.TYPE, Token.of(NodeType.of(value.type()).getAsString())) : new Node(NodeType.TYPE, datatype);

        if (!indefinite && !ValueParser.validVarset(value.type(), NodeType.of(datatype).getAsDataType())) {
            parserError("Datatypes are not equal on both sides, trying to assign " + value.type().getName() + " to a " + datatype.token() + " variable.", datatype,
                    "Change the datatype to the correct one, try casting values inside the expression to the needed datatype or set the variable type to '?'.");
            return null;
        }

        return new Node(NodeType.VAR_DEF_AND_SET_VALUE,
                new Node(NodeType.MODIFIERS, modifiers),
                new Node(NodeType.IDENTIFIER, identifier),
                finalType,
                new Node(NodeType.VALUE, value.node())
        );
    }

    public ValueParser.TypedNode parseExpression(@NotNull final List<Token> tokens) {
        return new ValueParser(tokens, this).parse();
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

    public List<Node> parseParametersDefineFunction(@NotNull final List<Token> tokens) {
        final List<Node> result = new ArrayList<>();
        if (tokens.isEmpty()) return result;
        Node currentNode = new Node(NodeType.VAR_DEFINITION);
        final List<NodeType> currentNodeModifiers = new ArrayList<>();
        boolean addedNode = false;
        boolean parsedDatatype = false;
        boolean parsedIdentifier = false;

        for (@NotNull final Token token : tokens) {
            final Node asNode = new Node(NodeType.of(token), token);
            final NodeType type = asNode.type();
            if (type == NodeType.COMMA) {
                currentNode.addChildren(new Node(NodeType.MODIFIERS, currentNodeModifiers.stream().map(Node::of).toList()));
                result.add(currentNode);
                currentNode = new Node(NodeType.VAR_DEFINITION);
                currentNodeModifiers.clear();
                parsedDatatype = false;
                parsedIdentifier = false;
                addedNode = true;
                continue;
            }
            addedNode = false;
            if (type.isModifier()) {
                if (parsedIdentifier || parsedDatatype) {
                    parserError("Unexpected token '" + token.token() + "' while parsing function parameters, expected modifiers before datatype before identifier");
                    return new ArrayList<>();
                }
                if (type.isVisibilityModifier()) {
                    parserError("Unexpected token '" + token.token() + "', cannot use visibility modifiers (pub, priv, own) for function parameters");
                }
                if (findConflictingModifiers(currentNodeModifiers, type, token)) return new ArrayList<>();
                currentNodeModifiers.add(type);
                continue;
            }
            if (type.isDatatype()) {
                if (parsedIdentifier) {
                    parserError("Unexpected token '" + token.token() + "' while parsing function parameters, expected datatype before identifier");
                    return new ArrayList<>();
                }
                parsedDatatype = true;
                currentNode.addChildren(asNode);
                continue;
            }
            if (type == NodeType.IDENTIFIER) {
                if (!parsedDatatype) {
                    parserError("Unexpected token '" + token.token() + "' while parsing function parameters, expected datatype before identifier");
                    return new ArrayList<>();
                }
                parsedIdentifier = true;
                currentNode.addChildren(asNode);
                continue;
            }
            parserError("Could not parse function argument, unexpected token '" + token.token() + "'");
            return new ArrayList<>();
        }
        if (!addedNode) {
            currentNode.addChildren(new Node(NodeType.MODIFIERS, currentNodeModifiers.stream().map(Node::of).toList()));
            result.add(currentNode);
        }
        return result;
    }

    private void scope(@NotNull final ScopeType type) {
        if (skimming) currentScope.add(new Scope(type, scopeIndent + 1));
    }

    private Scope scope() {
        return currentScope.isEmpty() ? null : currentScope.get(currentScope.size() - 1);
    }

    public Node evalFunctionDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final Token fnToken = tryAndGet(tokens, 0);
        if (expect(fnToken, fnToken, NodeType.LITERAL_FN)) return null;
        final Token identifier = tryAndGet(tokens, 1);
        if (expect(identifier, identifier, NodeType.IDENTIFIER)) return null;
        final Token retDef = tryAndGet(tokens, 2);
        if (expect(retDef, retDef, NodeType.TILDE, NodeType.DOUBLE_COLON, NodeType.LBRACE)) return null;

        final Optional<Node> firstMutabilityModif = modifiers.stream().filter(m -> m.type().isMutabilityModifier()).findFirst();
        if (firstMutabilityModif.isPresent()) {
            parserError("Cannot declare functions as own, const or mut, they are automatically constant because they cannot be redefined", firstMutabilityModif.get().value());
            return null;
        }
        final NodeType ret = NodeType.of(retDef);
        final Scope current = scope();
        if (skimming) {
            if (current == null) {
                parserError("Unexpected parsing error", "Could not create function at root level");
                return null;
            }
            if (!stdlib ? current.type() != ScopeType.MODULE : current.type() != ScopeType.PARENT && current.type() != ScopeType.MODULE) {
                if (stdlib) {
                    parserError("Expected function definition to be inside of a module or at root level",
                            "Cannot define functions inside of other functions");
                    return null;
                }
                parserError("Expected function definition to be inside of a module",
                        "Cannot define functions at root level, create a module for your function or move it to an existing module",
                        "Cannot define functions inside of other functions either");
                return null;
            }
            scope(ScopeType.FUNCTION);
        }

        if (ret == NodeType.LBRACE) {
            return new Node(NodeType.FUNCTION_DEFINITION,
                    new Node(NodeType.IDENTIFIER, identifier),
                    new Node(NodeType.TYPE, Token.of("void")),
                    new Node(NodeType.MODIFIERS, modifiers),
                    new Node(NodeType.PARAMETERS, Collections.emptyList())
            );
        }
        final int extraIndex = ret == NodeType.TILDE ? 0 : 1;

        final Datatype returnType;
        if (extraIndex == 0) returnType = Datatype.VOID;
        else returnType = NodeType.of(tryAndGet(tokens, 3)).getAsDataType();

        final Token parenOpen = tryAndGet(tokens, 3 + extraIndex);
        if (expect(parenOpen, parenOpen, NodeType.LPAREN)) return null;
        final Token parenClose = tryAndGet(tokens, tokens.size() - 2);
        if (expect(parenClose, parenClose, true, NodeType.RPAREN)) return null;
        final List<Node> params = parseParametersDefineFunction(tokens.subList(4 + extraIndex, tokens.size() - 2));



        return new Node(NodeType.FUNCTION_DEFINITION,
                new Node(NodeType.IDENTIFIER, identifier),
                new Node(NodeType.TYPE, Token.of(returnType.name().toLowerCase())),
                new Node(NodeType.MODIFIERS, modifiers),
                new Node(NodeType.PARAMETERS, params)
        );
    }

    private boolean unexpectedModifiers(@NotNull final List<Node> modifiers) {
        if (!modifiers.isEmpty()) {
            final Token firstModif = modifiers.stream().map(Node::value).findFirst().orElse(null);
            unexpected(firstModif);
            return true;
        }
        return false;
    }

    public Node evalModuleDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token identifier = tryAndGet(tokens, 1);
        if (expect(identifier, identifier, NodeType.IDENTIFIER)) return null;
        final Token lbrace = tryAndGet(tokens, 2);
        if (expect(lbrace, lbrace, NodeType.LBRACE)) return null;

        final Scope current = scope();
        if (skimming) {
            if (current == null) {
                parserError("Unexpected parsing error, could not create module at root level");
                return null;
            }
            if (current.type() != ScopeType.PARENT && current.type() != ScopeType.MODULE) {
                parserError("Expected module definition to be at root level or inside of another module");
                return null;
            }
            scope(ScopeType.MODULE);
        }

        return new Node(NodeType.CREATE_MODULE,
                new Node(NodeType.IDENTIFIER, identifier)
        );
    }

    public Node evalStdLibFinish(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token stdlibFinish = tryAndGet(tokens, 0);
        if (expect(stdlibFinish, stdlibFinish, NodeType.STANDARDLIB_MU_FINISH_CODE)) return null;
        final Token semi = tryAndGet(tokens, 1);
        if (expect(semi, semi, NodeType.SEMI)) return null;

        stdlib = false;

        return new Node(NodeType.STANDARDLIB_MU_FINISH_CODE);
    }

    public Token tryAndGet(@NotNull final List<Token> tokens, final int index) {
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

    private boolean findConflictingModifiers(@NotNull final List<NodeType> modifiers, @NotNull final NodeType newModifier, @NotNull final Token at) {
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

    protected void parserError(@NotNull final String message, @NotNull final String... quickFixes) {
        parserError(message, currentToken, quickFixes);
    }

    private void parserError(@NotNull final String message, final int line, final int column, @NotNull final String... quickFixes) {
        output.astHelperError(message, line, column, stdlibFinishLine, quickFixes);
        encounteredError = true;
    }

    private void parserError(@NotNull final String message, @NotNull final Token token, @NotNull final String... quickFixes) {
        parserError(message, token, false, quickFixes);
    }

    private void parserError(@NotNull final String message, @NotNull final Token token, final boolean skipToEndOfToken, @NotNull final String... quickFixes) {
        if (!stdlib) {
            output.astHelperError(message, token.line(), token.column() + (skipToEndOfToken ? token.token().length() : 0), stdlibFinishLine, quickFixes);
        } else {
            output.astHelperError("StandardLib error encountered, please contact the developer of this standard library to fix this issue:\n" + message,
                    token.actualLine(), token.column() + (skipToEndOfToken ? token.token().length() : 0), 1, quickFixes);
        }
        encounteredError = true;
    }


}
