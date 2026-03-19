package java.util;

public class IllegalFormatCodePointException extends IllegalFormatException {
    private static final long serialVersionUID = 19080630;
    private int c;

    public IllegalFormatCodePointException(int i) {
        this.c = i;
    }

    public int getCodePoint() {
        return this.c;
    }

    @Override
    public String getMessage() {
        return String.format("Code point = %#x", Integer.valueOf(this.c));
    }
}
