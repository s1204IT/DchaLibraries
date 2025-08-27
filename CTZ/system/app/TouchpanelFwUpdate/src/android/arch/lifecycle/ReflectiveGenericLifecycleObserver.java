package android.arch.lifecycle;

import android.arch.lifecycle.ClassesInfoCache;
import android.arch.lifecycle.Lifecycle;
import java.lang.reflect.InvocationTargetException;

/* loaded from: classes.dex */
class ReflectiveGenericLifecycleObserver implements GenericLifecycleObserver {
    private final ClassesInfoCache.CallbackInfo mInfo;
    private final Object mWrapped;

    ReflectiveGenericLifecycleObserver(Object wrapped) {
        this.mWrapped = wrapped;
        this.mInfo = ClassesInfoCache.sInstance.getInfo(this.mWrapped.getClass());
    }

    @Override // android.arch.lifecycle.GenericLifecycleObserver
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.mInfo.invokeCallbacks(source, event, this.mWrapped);
    }
}
