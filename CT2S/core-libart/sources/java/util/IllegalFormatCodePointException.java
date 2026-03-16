package java.util;

import java.io.Serializable;

public class IllegalFormatCodePointException extends IllegalFormatException implements Serializable {
    private static final long serialVersionUID = 19080630;
    private final int c;

    public IllegalFormatCodePointException(int c) {
        this.c = c;
    }

    public int getCodePoint() {
        return this.c;
    }

    @Override
    public String getMessage() {
        return Integer.toHexString(this.c);
    }
}
