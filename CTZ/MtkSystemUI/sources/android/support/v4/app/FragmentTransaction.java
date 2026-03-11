package android.support.v4.app;

public abstract class FragmentTransaction {
    public abstract FragmentTransaction add(int i, Fragment fragment, String str);

    public abstract FragmentTransaction add(Fragment fragment, String str);

    public abstract FragmentTransaction attach(Fragment fragment);

    public abstract int commit();

    public abstract int commitAllowingStateLoss();

    public abstract FragmentTransaction detach(Fragment fragment);

    public abstract FragmentTransaction remove(Fragment fragment);
}
