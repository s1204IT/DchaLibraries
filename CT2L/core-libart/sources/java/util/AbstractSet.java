package java.util;

public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {
    protected AbstractSet() {
    }

    @Override
    public boolean equals(Object object) {
        boolean z;
        if (this == object) {
            return true;
        }
        if (!(object instanceof Set)) {
            return false;
        }
        Set<?> s = (Set) object;
        try {
            if (size() == s.size()) {
                z = containsAll(s);
            }
            return z;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = 0;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            Object next = it.next();
            result += next == null ? 0 : next.hashCode();
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean result = false;
        if (size() <= collection.size()) {
            Iterator<E> it = iterator();
            while (it.hasNext()) {
                if (collection.contains(it.next())) {
                    it.remove();
                    result = true;
                }
            }
        } else {
            Iterator<?> it2 = collection.iterator();
            while (it2.hasNext()) {
                result = remove(it2.next()) || result;
            }
        }
        return result;
    }
}
