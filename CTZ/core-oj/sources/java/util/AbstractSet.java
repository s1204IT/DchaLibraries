package java.util;

public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {
    protected AbstractSet() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Set)) {
            return false;
        }
        Collection<?> collection = (Collection) obj;
        if (collection.size() != size()) {
            return false;
        }
        try {
            return containsAll(collection);
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int iHashCode = 0;
        for (E e : this) {
            if (e != null) {
                iHashCode += e.hashCode();
            }
        }
        return iHashCode;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        Objects.requireNonNull(collection);
        boolean zRemove = false;
        if (size() > collection.size()) {
            Iterator<?> it = collection.iterator();
            while (it.hasNext()) {
                zRemove |= remove(it.next());
            }
        } else {
            Iterator<E> it2 = iterator();
            while (it2.hasNext()) {
                if (collection.contains(it2.next())) {
                    it2.remove();
                    zRemove = true;
                }
            }
        }
        return zRemove;
    }
}
