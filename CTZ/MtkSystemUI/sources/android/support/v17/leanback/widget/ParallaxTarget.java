package android.support.v17.leanback.widget;

public abstract class ParallaxTarget {
    public void update(float fraction) {
    }

    public boolean isDirectMapping() {
        return false;
    }

    public void directUpdate(Number value) {
    }
}
