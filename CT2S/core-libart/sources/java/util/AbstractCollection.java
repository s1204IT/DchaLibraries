package java.util;

public abstract class AbstractCollection<E> implements Collection<E> {
    @Override
    public abstract Iterator<E> iterator();

    @Override
    public abstract int size();

    protected AbstractCollection() {
    }

    @Override
    public boolean add(E object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        boolean result = false;
        Iterator<? extends E> it = collection.iterator();
        while (it.hasNext()) {
            if (add(it.next())) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public void clear() {
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    @Override
    public boolean contains(Object object) {
        Iterator<E> it = iterator();
        if (object != null) {
            while (it.hasNext()) {
                if (object.equals(it.next())) {
                    return true;
                }
            }
        } else {
            while (it.hasNext()) {
                if (it.next() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            if (!contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean remove(Object object) {
        Iterator<E> it = iterator();
        if (object != null) {
            while (it.hasNext()) {
                if (object.equals(it.next())) {
                    it.remove();
                    return true;
                }
            }
        } else {
            while (it.hasNext()) {
                if (it.next() == null) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean result = false;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (collection.contains(it.next())) {
                it.remove();
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean result = false;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (!collection.contains(it.next())) {
                it.remove();
                result = true;
            }
        }
        return result;
    }

    @Override
    public Object[] toArray() {
        return toArrayList().toArray();
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        return (T[]) toArrayList().toArray(tArr);
    }

    private ArrayList<Object> toArrayList() {
        ArrayList<Object> result = new ArrayList<>(size());
        for (E entry : this) {
            result.add(entry);
        }
        return result;
    }

    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        StringBuilder buffer = new StringBuilder(size() * 16);
        buffer.append('[');
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            Object next = it.next();
            if (next != this) {
                buffer.append(next);
            } else {
                buffer.append("(this Collection)");
            }
            if (it.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append(']');
        return buffer.toString();
    }
}
