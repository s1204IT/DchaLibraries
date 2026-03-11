package android.support.v4.util;

public class Pools$SimplePool<T> implements Pools$Pool<T> {
    private final Object[] mPool;
    private int mPoolSize;

    public Pools$SimplePool(int maxPoolSize) {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("The max pool size must be > 0");
        }
        this.mPool = new Object[maxPoolSize];
    }

    @Override
    public T acquire() {
        if (this.mPoolSize <= 0) {
            return null;
        }
        int i = this.mPoolSize - 1;
        T t = (T) this.mPool[i];
        this.mPool[i] = null;
        this.mPoolSize--;
        return t;
    }

    @Override
    public boolean release(T instance) {
        if (isInPool(instance)) {
            throw new IllegalStateException("Already in the pool!");
        }
        if (this.mPoolSize < this.mPool.length) {
            this.mPool[this.mPoolSize] = instance;
            this.mPoolSize++;
            return true;
        }
        return false;
    }

    private boolean isInPool(T instance) {
        for (int i = 0; i < this.mPoolSize; i++) {
            if (this.mPool[i] == instance) {
                return true;
            }
        }
        return false;
    }
}
