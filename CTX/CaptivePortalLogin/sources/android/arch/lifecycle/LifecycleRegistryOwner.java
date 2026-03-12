package android.arch.lifecycle;

@Deprecated
public interface LifecycleRegistryOwner extends LifecycleOwner {
    @Override
    LifecycleRegistry getLifecycle();
}
