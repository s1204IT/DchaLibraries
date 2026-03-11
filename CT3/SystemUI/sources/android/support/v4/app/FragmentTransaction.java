package android.support.v4.app;

import android.support.annotation.IdRes;
import android.support.annotation.Nullable;

public abstract class FragmentTransaction {
    public abstract FragmentTransaction add(@IdRes int i, Fragment fragment, @Nullable String str);

    public abstract FragmentTransaction attach(Fragment fragment);

    public abstract int commit();

    public abstract FragmentTransaction detach(Fragment fragment);
}
