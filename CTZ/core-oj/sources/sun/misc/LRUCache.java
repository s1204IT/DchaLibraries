package sun.misc;

public abstract class LRUCache<N, V> {
    private V[] oa = null;
    private final int size;

    protected abstract V create(N n);

    protected abstract boolean hasName(V v, N n);

    public LRUCache(int i) {
        this.size = i;
    }

    public static void moveToFront(Object[] objArr, int i) {
        Object obj = objArr[i];
        while (i > 0) {
            objArr[i] = objArr[i - 1];
            i--;
        }
        objArr[0] = obj;
    }

    public V forName(N n) {
        if (this.oa == null) {
            this.oa = (V[]) new Object[this.size];
        } else {
            for (int i = 0; i < this.oa.length; i++) {
                V v = this.oa[i];
                if (v != null && hasName(v, n)) {
                    if (i > 0) {
                        moveToFront(this.oa, i);
                    }
                    return v;
                }
            }
        }
        V vCreate = create(n);
        this.oa[this.oa.length - 1] = vCreate;
        moveToFront(this.oa, this.oa.length - 1);
        return vCreate;
    }
}
