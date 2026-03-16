package java.lang.ref;

public class PhantomReference<T> extends Reference<T> {
    public PhantomReference(T r, ReferenceQueue<? super T> q) {
        super(r, q);
    }

    @Override
    public T get() {
        return null;
    }
}
