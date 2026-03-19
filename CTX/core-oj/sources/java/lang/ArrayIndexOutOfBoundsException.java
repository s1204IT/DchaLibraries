package java.lang;

public class ArrayIndexOutOfBoundsException extends IndexOutOfBoundsException {
    private static final long serialVersionUID = -5116101128118950844L;

    public ArrayIndexOutOfBoundsException() {
    }

    public ArrayIndexOutOfBoundsException(int i) {
        super("Array index out of range: " + i);
    }

    public ArrayIndexOutOfBoundsException(String str) {
        super(str);
    }

    public ArrayIndexOutOfBoundsException(int i, int i2) {
        super("length=" + i + "; index=" + i2);
    }

    public ArrayIndexOutOfBoundsException(int i, int i2, int i3) {
        super("length=" + i + "; regionStart=" + i2 + "; regionLength=" + i3);
    }
}
