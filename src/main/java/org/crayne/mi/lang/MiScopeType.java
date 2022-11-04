package org.crayne.mi.lang;

public enum MiScopeType {

    FUNCTION_ROOT(false, false),
    FUNCTION_LOCAL(false, false),
    IF(true, false),
    ELSE(false, false),
    WHILE(true, true),
    FOR(true, true),
    DO(true, true);

    private final boolean conditional;
    private final boolean looping;

    MiScopeType(final boolean conditional, final boolean looping) {
        this.conditional = conditional;
        this.looping = looping;
    }

    public boolean conditional() {
        return conditional;
    }

    public boolean looping() {
        return looping;
    }

}
