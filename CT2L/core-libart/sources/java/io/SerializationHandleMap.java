package java.io;

final class SerializationHandleMap {
    private static final int LOAD_FACTOR = 7500;
    private Object[] keys;
    private int size = 0;
    private int threshold = 21;
    private int[] values;

    public SerializationHandleMap() {
        int arraySize = (int) ((((long) this.threshold) * 10000) / 7500);
        resizeArrays(arraySize);
    }

    private void resizeArrays(int newSize) {
        Object[] oldKeys = this.keys;
        int[] oldValues = this.values;
        this.keys = new Object[newSize];
        this.values = new int[newSize];
        if (oldKeys != null) {
            for (int i = 0; i < oldKeys.length; i++) {
                Object key = oldKeys[i];
                int value = oldValues[i];
                int index = findIndex(key, this.keys);
                this.keys[index] = key;
                this.values[index] = value;
            }
        }
    }

    public int get(Object key) {
        int index = findIndex(key, this.keys);
        if (this.keys[index] == key) {
            return this.values[index];
        }
        return -1;
    }

    private int findIndex(Object key, Object[] array) {
        int length = array.length;
        int index = getModuloHash(key, length);
        int last = ((index + length) - 1) % length;
        while (index != last && array[index] != key && array[index] != null) {
            index = (index + 1) % length;
        }
        return index;
    }

    private int getModuloHash(Object key, int length) {
        return (System.identityHashCode(key) & Integer.MAX_VALUE) % length;
    }

    public int put(Object key, int value) {
        int index = findIndex(key, this.keys);
        if (this.keys[index] != key) {
            int i = this.size + 1;
            this.size = i;
            if (i > this.threshold) {
                rehash();
                index = findIndex(key, this.keys);
            }
            this.keys[index] = key;
            this.values[index] = -1;
        }
        int result = this.values[index];
        this.values[index] = value;
        return result;
    }

    private void rehash() {
        int newSize = this.keys.length * 2;
        resizeArrays(newSize);
        this.threshold = (int) ((((long) this.keys.length) * 7500) / 10000);
    }

    public int remove(Object key) {
        boolean hashedOk;
        int next = findIndex(key, this.keys);
        int index = next;
        if (this.keys[index] != key) {
            return -1;
        }
        int i = this.values[index];
        int length = this.keys.length;
        while (true) {
            next = (next + 2) % length;
            Object object = this.keys[next];
            if (object != null) {
                int hash = getModuloHash(object, length);
                boolean hashedOk2 = hash > index;
                if (next < index) {
                    hashedOk = hashedOk2 || hash <= next;
                } else {
                    hashedOk = hashedOk2 && hash <= next;
                }
                if (!hashedOk) {
                    this.keys[index] = object;
                    this.values[index] = this.values[next];
                    index = next;
                }
            } else {
                this.size--;
                this.keys[index] = null;
                this.values[index] = -1;
                return i;
            }
        }
    }

    public boolean isEmpty() {
        return this.size == 0;
    }
}
