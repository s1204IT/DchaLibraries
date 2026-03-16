package java.util;

import java.io.Serializable;

public class FormatFlagsConversionMismatchException extends IllegalFormatException implements Serializable {
    private static final long serialVersionUID = 19120414;
    private final char c;
    private final String f;

    public FormatFlagsConversionMismatchException(String f, char c) {
        if (f == null) {
            throw new NullPointerException("f == null");
        }
        this.f = f;
        this.c = c;
    }

    public String getFlags() {
        return this.f;
    }

    public char getConversion() {
        return this.c;
    }

    @Override
    public String getMessage() {
        return "%" + this.c + " does not support '" + this.f + "'";
    }
}
