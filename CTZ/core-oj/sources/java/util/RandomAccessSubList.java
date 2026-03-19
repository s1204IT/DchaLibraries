package java.util;

class RandomAccessSubList<E> extends SubList<E> implements RandomAccess {
    RandomAccessSubList(AbstractList<E> abstractList, int i, int i2) {
        super(abstractList, i, i2);
    }

    @Override
    public List<E> subList(int i, int i2) {
        return new RandomAccessSubList(this, i, i2);
    }
}
