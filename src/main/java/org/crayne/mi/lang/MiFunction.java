package org.crayne.mi.lang;

import java.util.Set;

public interface MiFunction {

    MiModule module();

    MiDatatype returnType();

    Set<MiModifier> modifiers();

    String name();

    Set<MiVariable> parameters();
    Set<MiDatatype> parameterTypes();


}
