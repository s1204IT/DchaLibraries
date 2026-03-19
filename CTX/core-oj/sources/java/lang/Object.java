package java.lang;

import dalvik.annotation.optimization.FastNative;

public class Object {
    private transient Class<?> shadow$_klass_;
    private transient int shadow$_monitor_;

    @FastNative
    private static native int identityHashCodeNative(Object obj);

    @FastNative
    private native Object internalClone();

    @FastNative
    public final native void notify();

    @FastNative
    public final native void notifyAll();

    @FastNative
    public final native void wait() throws InterruptedException;

    @FastNative
    public final native void wait(long j, int i) throws InterruptedException;

    public final Class<?> getClass() {
        return this.shadow$_klass_;
    }

    public int hashCode() {
        return identityHashCode(this);
    }

    static int identityHashCode(Object obj) {
        int i = obj.shadow$_monitor_;
        if (((-1073741824) & i) == Integer.MIN_VALUE) {
            return 268435455 & i;
        }
        return identityHashCodeNative(obj);
    }

    public boolean equals(Object obj) {
        return this == obj;
    }

    protected Object clone() throws CloneNotSupportedException {
        if (!(this instanceof Cloneable)) {
            throw new CloneNotSupportedException("Class " + getClass().getName() + " doesn't implement Cloneable");
        }
        return internalClone();
    }

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    public final void wait(long j) throws InterruptedException {
        wait(j, 0);
    }

    protected void finalize() throws Throwable {
    }
}
