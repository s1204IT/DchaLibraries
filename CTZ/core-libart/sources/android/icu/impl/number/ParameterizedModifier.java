package android.icu.impl.number;

import android.icu.impl.StandardPlural;

public class ParameterizedModifier {
    static final boolean $assertionsDisabled = false;
    boolean frozen;
    final Modifier[] mods;
    private final Modifier negative;
    private final Modifier positive;

    public ParameterizedModifier(Modifier modifier, Modifier modifier2) {
        this.positive = modifier;
        this.negative = modifier2;
        this.mods = null;
        this.frozen = true;
    }

    public ParameterizedModifier() {
        this.positive = null;
        this.negative = null;
        this.mods = new Modifier[2 * StandardPlural.COUNT];
        this.frozen = false;
    }

    public void setModifier(boolean z, StandardPlural standardPlural, Modifier modifier) {
        this.mods[getModIndex(z, standardPlural)] = modifier;
    }

    public void freeze() {
        this.frozen = true;
    }

    public Modifier getModifier(boolean z) {
        return z ? this.negative : this.positive;
    }

    public Modifier getModifier(boolean z, StandardPlural standardPlural) {
        return this.mods[getModIndex(z, standardPlural)];
    }

    private static int getModIndex(boolean z, StandardPlural standardPlural) {
        return (standardPlural.ordinal() * 2) + (z ? 1 : 0);
    }
}
