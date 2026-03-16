package java.lang;

public class Object {
    private transient Class<?> shadow$_klass_;
    private transient int shadow$_monitor_;

    private native Object internalClone();

    public final native void notify();

    public final native void notifyAll();

    public final native void wait() throws InterruptedException;

    public final native void wait(long j, int i) throws InterruptedException;

    protected Object clone() throws CloneNotSupportedException {
        if (!(this instanceof Cloneable)) {
            throw new CloneNotSupportedException("Class " + getClass().getName() + " doesn't implement Cloneable");
        }
        return internalClone();
    }

    public boolean equals(Object o) {
        return this == o;
    }

    @FindBugsSuppressWarnings({"FI_EMPTY"})
    protected void finalize() throws Throwable {
    }

    public final Class<?> getClass() {
        return this.shadow$_klass_;
    }

    public int hashCode() {
        int lockWord = this.shadow$_monitor_;
        return ((-1073741824) & lockWord) == Integer.MIN_VALUE ? 1073741823 & lockWord : System.identityHashCode(this);
    }

    public String toString() {
        return getClass().getName() + '@' + Integer.toHexString(hashCode());
    }

    public final void wait(long millis) throws InterruptedException {
        wait(millis, 0);
    }
}
