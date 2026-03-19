package android.view.animation;

public abstract class BaseInterpolator implements Interpolator {
    private int mChangingConfiguration;

    public int getChangingConfiguration() {
        return this.mChangingConfiguration;
    }

    void setChangingConfiguration(int i) {
        this.mChangingConfiguration = i;
    }
}
