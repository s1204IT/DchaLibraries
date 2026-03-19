package android.content.res;

import android.content.res.Resources;

public abstract class ComplexColor {
    private int mChangingConfigurations;

    public abstract boolean canApplyTheme();

    public abstract ConstantState<ComplexColor> getConstantState();

    public abstract int getDefaultColor();

    public abstract ComplexColor obtainForTheme(Resources.Theme theme);

    public boolean isStateful() {
        return false;
    }

    final void setBaseChangingConfigurations(int i) {
        this.mChangingConfigurations = i;
    }

    public int getChangingConfigurations() {
        return this.mChangingConfigurations;
    }
}
