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

public enum MiModifier {

    PUB, PRIV, PROT, MUT, CONST, NONNULL, NULLABLE, OWN, NAT, INTERN;

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

}
