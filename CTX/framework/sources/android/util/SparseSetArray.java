package android.util;

public class SparseSetArray<T> {
    private final SparseArray<ArraySet<T>> mData = new SparseArray<>();

    public boolean add(int i, T t) {
        ArraySet<T> arraySet = this.mData.get(i);
        if (arraySet == null) {
            arraySet = new ArraySet<>();
            this.mData.put(i, arraySet);
        }
        if (arraySet.contains(t)) {
            return true;
        }
        arraySet.add(t);
        return false;
    }

    public boolean contains(int i, T t) {
        ArraySet<T> arraySet = this.mData.get(i);
        if (arraySet == null) {
            return false;
        }
        return arraySet.contains(t);
    }

    public boolean remove(int i, T t) {
        ArraySet<T> arraySet = this.mData.get(i);
        if (arraySet == null) {
            return false;
        }
        boolean zRemove = arraySet.remove(t);
        if (arraySet.size() == 0) {
            this.mData.remove(i);
        }
        return zRemove;
    }

    public void remove(int i) {
        this.mData.remove(i);
    }

    public int size() {
        return this.mData.size();
    }

    public int keyAt(int i) {
        return this.mData.keyAt(i);
    }

    public int sizeAt(int i) {
        ArraySet<T> arraySetValueAt = this.mData.valueAt(i);
        if (arraySetValueAt == null) {
            return 0;
        }
        return arraySetValueAt.size();
    }

    public T valueAt(int i, int i2) {
        return this.mData.valueAt(i).valueAt(i2);
    }
}
