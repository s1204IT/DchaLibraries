package java.util;

public class Stack<E> extends Vector<E> {
    private static final long serialVersionUID = 1224463164541339165L;

    public E push(E e) {
        addElement(e);
        return e;
    }

    public synchronized E pop() {
        E ePeek;
        int size = size();
        ePeek = peek();
        removeElementAt(size - 1);
        return ePeek;
    }

    public synchronized E peek() {
        int size;
        size = size();
        if (size == 0) {
            throw new EmptyStackException();
        }
        return elementAt(size - 1);
    }

    public boolean empty() {
        return size() == 0;
    }

    public synchronized int search(Object obj) {
        int iLastIndexOf = lastIndexOf(obj);
        if (iLastIndexOf >= 0) {
            return size() - iLastIndexOf;
        }
        return -1;
    }
}
