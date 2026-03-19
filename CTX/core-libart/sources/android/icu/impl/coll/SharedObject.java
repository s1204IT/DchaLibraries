package android.icu.impl.coll;

import android.icu.util.ICUCloneNotSupportedException;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedObject implements Cloneable {
    private AtomicInteger refCount = new AtomicInteger();

    public static final class Reference<T extends SharedObject> implements Cloneable {
        private T ref;

        public Reference(T t) {
            this.ref = t;
            if (t != null) {
                t.addRef();
            }
        }

        public Reference<T> m2clone() {
            try {
                Reference<T> reference = (Reference) super.clone();
                if (this.ref != null) {
                    this.ref.addRef();
                }
                return reference;
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
            T t2 = (T) t.mo1clone();
            t.removeRef();
            this.ref = t2;
            t2.addRef();
            return t2;
        }

        public void clear() {
            if (this.ref != null) {
                this.ref.removeRef();
                this.ref = null;
            }
        }

        protected void finalize() throws Throwable {
            super.finalize();
            clear();
        }
    }

    @Override
    public SharedObject mo1clone() {
        try {
            SharedObject sharedObject = (SharedObject) super.clone();
            sharedObject.refCount = new AtomicInteger();
            return sharedObject;
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
