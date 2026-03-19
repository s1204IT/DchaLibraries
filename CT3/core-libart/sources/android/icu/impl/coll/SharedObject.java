package android.icu.impl.coll;

import android.icu.util.ICUCloneNotSupportedException;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedObject implements Cloneable {
    private AtomicInteger refCount = new AtomicInteger();

    public static final class Reference<T extends SharedObject> implements Cloneable {
        private T ref;

        public Reference(T r) {
            this.ref = r;
            if (r == null) {
                return;
            }
            r.addRef();
        }

        public Reference<T> m77clone() {
            try {
                Reference<T> c = (Reference) super.clone();
                if (this.ref != null) {
                    this.ref.addRef();
                }
                return c;
            } catch (CloneNotSupportedException e) {
                throw new ICUCloneNotSupportedException(e);
            }
        }

        public T readOnly() {
            return this.ref;
        }

        public T copyOnWrite() {
            T t = this.ref;
            if (t.getRefCount() <= 1) {
                return t;
            }
            T t2 = (T) t.m76clone();
            t.removeRef();
            this.ref = t2;
            t2.addRef();
            return t2;
        }

        public void clear() {
            if (this.ref == null) {
                return;
            }
            this.ref.removeRef();
            this.ref = null;
        }

        protected void finalize() throws Throwable {
            super.finalize();
            clear();
        }
    }

    public SharedObject m76clone() {
        try {
            SharedObject c = (SharedObject) super.clone();
            c.refCount = new AtomicInteger();
            return c;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    public final void addRef() {
        this.refCount.incrementAndGet();
    }

    public final void removeRef() {
        this.refCount.decrementAndGet();
    }

    public final int getRefCount() {
        return this.refCount.get();
    }

    public final void deleteIfZeroRefCount() {
    }
}
