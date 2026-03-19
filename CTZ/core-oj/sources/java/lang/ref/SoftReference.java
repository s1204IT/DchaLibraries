package java.lang.ref;

public class SoftReference<T> extends Reference<T> {
    private static long clock;
    private long timestamp;

    public SoftReference(T t) {
        super(t);
        this.timestamp = clock;
    }

    public SoftReference(T t, ReferenceQueue<? super T> referenceQueue) {
        super(t, referenceQueue);
        this.timestamp = clock;
    }

    @Override
    public T get() {
        T t = (T) super.get();
        if (t != null && this.timestamp != clock) {
            this.timestamp = clock;
        }
        return t;
    }
}
