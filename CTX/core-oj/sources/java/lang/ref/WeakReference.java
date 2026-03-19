package java.lang.ref;

public class WeakReference<T> extends Reference<T> {
    public WeakReference(T t) {
        super(t);
    }

    public WeakReference(T t, ReferenceQueue<? super T> referenceQueue) {
        super(t, referenceQueue);
    }
}
