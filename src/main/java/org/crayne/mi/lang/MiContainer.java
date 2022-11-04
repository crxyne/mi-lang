package org.crayne.mi.lang;

import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface MiContainer { // anything container-like tbh, like modules, structs, functions etc and anything else to come in this fashion

    Set<MiVariable> variables();
    void add(@NotNull final MiVariable var);
    void addAll(@NotNull final Collection<MiVariable> var);
    void addAll(@NotNull final MiVariable... var);
    void pop();
    Optional<MiVariable> find(@NotNull final String name);
    Token identifier();

}
