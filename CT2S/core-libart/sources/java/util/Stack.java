package java.util;

public class Stack<E> extends Vector<E> {
    private static final long serialVersionUID = 1224463164541339165L;

    public boolean empty() {
        return isEmpty();
    }

    public synchronized E peek() {
        try {
        } catch (IndexOutOfBoundsException e) {
            throw new EmptyStackException();
        }
        return (E) this.elementData[this.elementCount - 1];
    }

    public synchronized E pop() {
        E e;
        if (this.elementCount == 0) {
            throw new EmptyStackException();
        }
        int i = this.elementCount - 1;
        this.elementCount = i;
        e = (E) this.elementData[i];
        this.elementData[i] = null;
        this.modCount++;
        return e;
    }

    public E push(E object) {
        addElement(object);
        return object;
    }

    public synchronized int search(Object o) {
        int i;
        Object[] dumpArray = this.elementData;
        int size = this.elementCount;
        if (o != null) {
            for (int i2 = size - 1; i2 >= 0; i2--) {
                if (o.equals(dumpArray[i2])) {
                    i = size - i2;
                    break;
                }
            }
            i = -1;
        } else {
            for (int i3 = size - 1; i3 >= 0; i3--) {
                if (dumpArray[i3] == null) {
                    i = size - i3;
                    break;
                }
            }
            i = -1;
        }
        return i;
    }
}
