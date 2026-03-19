package android.content.res;

import android.content.res.Resources;

public abstract class ConstantState<T> {
    public abstract int getChangingConfigurations();

    public abstract T newInstance();

    public T newInstance(Resources resources) {
        return newInstance();
    }

    public T newInstance(Resources resources, Resources.Theme theme) {
        return newInstance(resources);
    }
}
