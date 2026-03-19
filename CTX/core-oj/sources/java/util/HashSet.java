package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, Serializable {
    private static final Object PRESENT = new Object();
    static final long serialVersionUID = -5024744406713321676L;
    private transient HashMap<E, Object> map;

    public HashSet() {
        this.map = new HashMap<>();
    }

    public HashSet(Collection<? extends E> collection) {
        this.map = new HashMap<>(Math.max(((int) (collection.size() / 0.75f)) + 1, 16));
        addAll(collection);
    }

    public HashSet(int i, float f) {
        this.map = new HashMap<>(i, f);
    }

    public HashSet(int i) {
        this.map = new HashMap<>(i);
    }

    HashSet(int i, float f, boolean z) {
        this.map = new LinkedHashMap(i, f);
    }

    @Override
    public Iterator<E> iterator() {
        return this.map.keySet().iterator();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        return this.map.containsKey(obj);
    }

    @Override
    public boolean add(E e) {
        return this.map.put(e, PRESENT) == null;
    }

    @Override
    public boolean remove(Object obj) {
        return this.map.remove(obj) == PRESENT;
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    public Object clone() {
        try {
            HashSet hashSet = (HashSet) super.clone();
            hashSet.map = (HashMap) this.map.clone();
            return hashSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(this.map.capacity());
        objectOutputStream.writeFloat(this.map.loadFactor());
        objectOutputStream.writeInt(this.map.size());
        Iterator<E> it = this.map.keySet().iterator();
        while (it.hasNext()) {
            objectOutputStream.writeObject(it.next());
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        HashMap<E, Object> map;
        objectInputStream.defaultReadObject();
        int i = objectInputStream.readInt();
        if (i < 0) {
            throw new InvalidObjectException("Illegal capacity: " + i);
        }
        float f = objectInputStream.readFloat();
        if (f <= 0.0f || Float.isNaN(f)) {
            throw new InvalidObjectException("Illegal load factor: " + f);
        }
        int i2 = objectInputStream.readInt();
        if (i2 < 0) {
            throw new InvalidObjectException("Illegal size: " + i2);
        }
        int iMin = (int) Math.min(i2 * Math.min(1.0f / f, 4.0f), 1.0737418E9f);
        if (this instanceof LinkedHashSet) {
            map = new LinkedHashMap<>(iMin, f);
        } else {
            map = new HashMap<>(iMin, f);
        }
        this.map = map;
        for (int i3 = 0; i3 < i2; i3++) {
            this.map.put((E) objectInputStream.readObject(), PRESENT);
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new HashMap.KeySpliterator(this.map, 0, -1, 0, 0);
    }
}
