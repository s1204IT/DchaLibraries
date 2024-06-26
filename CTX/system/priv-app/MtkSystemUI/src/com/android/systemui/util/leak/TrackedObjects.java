package com.android.systemui.util.leak;

import java.util.Collection;
import java.util.WeakHashMap;
/* loaded from: classes.dex */
public class TrackedObjects {
    private final WeakHashMap<Class<?>, TrackedClass<?>> mTrackedClasses = new WeakHashMap<>();
    private final TrackedCollections mTrackedCollections;

    public TrackedObjects(TrackedCollections trackedCollections) {
        this.mTrackedCollections = trackedCollections;
    }

    public synchronized <T> void track(T t) {
        Class<?> cls = t.getClass();
        TrackedClass<?> trackedClass = this.mTrackedClasses.get(cls);
        if (trackedClass == null) {
            trackedClass = new TrackedClass<>();
            this.mTrackedClasses.put(cls, trackedClass);
        }
        trackedClass.track(t);
        this.mTrackedCollections.track(trackedClass, cls.getName());
    }

    public static boolean isTrackedObject(Collection<?> collection) {
        return collection instanceof TrackedClass;
    }

    /* loaded from: classes.dex */
    private static class TrackedClass<T> extends AbstractCollection<T> {
        final WeakIdentityHashMap<T, Void> instances;

        private TrackedClass() {
            this.instances = new WeakIdentityHashMap<>();
        }

        void track(T t) {
            this.instances.put(t, null);
        }

        @Override // java.util.Collection
        public int size() {
            return this.instances.size();
        }

        @Override // java.util.Collection
        public boolean isEmpty() {
            return this.instances.isEmpty();
        }
    }
}
