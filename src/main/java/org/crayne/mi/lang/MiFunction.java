package org.crayne.mi.lang;

import java.util.List;
import java.util.Set;

public interface MiFunction {

    MiModule module();

    MiDatatype returnType();

    Set<MiModifier> modifiers();

    String name();

    List<MiVariable> parameters();
    List<MiDatatype> parameterTypes();


}
