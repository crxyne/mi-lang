package org.crayne.mi.lang;

import org.crayne.mi.parsing.ast.Node;
import org.crayne.mi.parsing.ast.NodeType;
import org.crayne.mi.parsing.lexer.Token;
import org.crayne.mi.parsing.parser.ASTErrorChecker;
import org.crayne.mi.parsing.parser.ASTGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public enum MiModifier {

    PUB("public"),
    PRIV("private"),
    PROT("protected"),
    MUT("mutable"),
    CONST("constant"),
    NONNULL("nonnull"),
    NULLABLE("nullable"),
    OWN("own"),
    NAT("native"),
    INTERN("internal");

    private final String name;

    MiModifier(@NotNull final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Optional<MiModifier> of(@NotNull final NodeType nodeType) {
        return Optional.ofNullable(switch (nodeType) {
            case LITERAL_PUB -> PUB;
            case LITERAL_PRIV -> PRIV;
            case LITERAL_PROT -> PROT;
            case LITERAL_OWN -> OWN;
            case LITERAL_MUT -> MUT;
            case LITERAL_CONST -> CONST;
            case LITERAL_NULLABLE -> NULLABLE;
            case LITERAL_NONNULL -> NONNULL;
            case LITERAL_NAT -> NAT;
            case LITERAL_INTERN -> INTERN;
            default -> null;
        });
    }

    public boolean visibilityModifier() {
        return switch (this) {
            case PUB, PRIV, PROT -> true;
            default -> false;
        };
    }

    public boolean mutabilityModifier() {
        return switch (this) {
            case MUT, CONST, OWN -> true;
            default -> false;
        };
    }

    public boolean conflictsWith(@NotNull final MiModifier other) {
        return switch (this) {
            case PUB -> other == PRIV || other == PROT;
            case PRIV -> other == PUB || other == PROT;
            case PROT -> other == PRIV || other == PUB;
            case MUT -> other == CONST || other == OWN;
            case CONST -> other == MUT || other == OWN;
            case NONNULL -> other == NULLABLE;
            case NULLABLE -> other == NONNULL;
            case OWN -> other == MUT || other == CONST;
            case NAT -> other == INTERN;
            case INTERN -> other == NAT;
        };
    }

    public static Optional<Token> firstConflicting(@NotNull final Collection<Node> modifiers) {
        return modifiers.stream().filter(m -> {
                    final Optional<MiModifier> thisModif = MiModifier.of(m.type());

                    return thisModif.isEmpty() || modifiers.stream().anyMatch(n -> {
                        final Optional<MiModifier> modif = MiModifier.of(n.type());
                        return modif.isEmpty() || thisModif.get().conflictsWith(modif.get());
                    });
                })
                .map(Node::value)
                .findFirst();
    }

    public static Optional<Token> firstDuplicate(@NotNull final Collection<Node> modifiers) {
        return modifiers.stream().filter(m -> {
            final Optional<MiModifier> thisModif = MiModifier.of(m.type());
            final List<MiModifier> modifierSet = ASTErrorChecker.definiteModifiers(ASTGenerator.modifiersOfNodes(modifiers));
            return modifierSet.stream().filter(mod -> thisModif.isPresent() && mod == thisModif.get()).toList().size() > 1;
        }).map(Node::value).findFirst();
    }

    public static MiModifier effectiveVisibilityModifier(@NotNull final Collection<MiModifier> modifiers) {
        return modifiers
                .stream()
                .filter(MiModifier::visibilityModifier)
                .findAny()
                .orElse(PROT); // remove any non-visibility modifiers and get the single one standing (there will definetly only be one, or none)
                               // if there is none (which is also valid) then the prot modifier is used
    }

    public static MiModifier effectiveMutabilityModifier(@NotNull final Collection<MiModifier> modifiers) {
        return modifiers
                .stream()
                .filter(MiModifier::mutabilityModifier)
                .findAny()
                .orElse(CONST); // similar how in the above function, except with mutability modifiers.
                                // default modifier for this one is "const", as every variable is constant by default
    }

    public static boolean invalidGlobalMutation(@NotNull final MiVariable variable, @NotNull final MiModule own) {
        final Set<MiModifier> modifiers = variable.modifiers();
        final MiModifier mmodifier = effectiveMutabilityModifier(modifiers);
        return !switch (mmodifier) {
            case MUT -> validMutAccess(variable);
            case CONST -> validConstAccess(variable);
            case OWN -> validOwnAccess(variable, own);
            default -> // should never happen
                    throw new RuntimeException("Invalid effective mutability modifier for global variable from given modifier set (" + modifiers + ")");
        };
    }

    public static boolean invalidLocalMutation(@NotNull final MiVariable variable) {
        final Set<MiModifier> modifiers = variable.modifiers();
        final MiModifier mmodifier = effectiveMutabilityModifier(modifiers);
        return !switch (mmodifier) {
            case MUT -> validMutAccess(variable);
            case CONST -> validConstAccess(variable);
            default -> // should never happen
            throw new RuntimeException("Invalid effective mutability modifier for local variable from given modifier set (" + modifiers + ")");
        };
    }

    public static boolean validMutAccess(@SuppressWarnings("unused") @NotNull final MiVariable variable) {
        return true;
    }

    public static boolean validConstAccess(@NotNull final MiVariable variable) {
        return !variable.initialized(); // only allow changing constants when they dont have a value yet
    }

    public static boolean validOwnAccess(@SuppressWarnings("unused") @NotNull final MiVariable variable, @SuppressWarnings("unused") @NotNull final MiModule own) {
        final MiContainer container = variable.container();
        if (!(container instanceof final MiModule module)) throw new RuntimeException("Unexpected error; global variable container is not a module");
        return validProtAccess(module, own);
    }

    public static boolean invalidAccess(@NotNull final Collection<MiModifier> modifiers, @NotNull final MiModule accessing, @NotNull final MiModule own) {
        final MiModifier vmodifier = effectiveVisibilityModifier(modifiers);
        return !switch (vmodifier) {
            case PUB -> validPubAccess(accessing, own); // no more work when accessing something public
            case PROT -> validProtAccess(accessing, own);
            case PRIV -> validPrivAccess(accessing, own);
            default -> // should never happen
                    throw new RuntimeException("Invalid effective visibility modifier from given modifier collection (" + modifiers + ")");
        };
    }

    public static boolean validPubAccess(@SuppressWarnings("unused") @NotNull final MiModule accessing, @SuppressWarnings("unused") @NotNull final MiModule own) {
        return true;
    }

    public static boolean validProtAccess(@NotNull final MiModule accessing, @NotNull final MiModule own) {
        MiModule current = own;
        while (current != null) { // check if the module we access from is either the module we want to access (shared module) or if were a submodule of given module
            if (current == accessing) return true;
            current = current.parent().orElse(null);
        }
        return false;
    }

    public static boolean validPrivAccess(@NotNull final MiModule accessing, @NotNull final MiModule own) {
        return accessing == own;
    }

}
