package java.util;

import java.io.Serializable;

public class IllegalFormatConversionException extends IllegalFormatException implements Serializable {
    private static final long serialVersionUID = 17000126;
    private final Class<?> arg;
    private final char c;

    public IllegalFormatConversionException(char c, Class<?> arg) {
        this.c = c;
        if (arg == null) {
            throw new NullPointerException("arg == null");
        }
        this.arg = arg;
    }

    public Class<?> getArgumentClass() {
        return this.arg;
    }

    public char getConversion() {
        return this.c;
    }

    @Override
    public String getMessage() {
        return "%" + this.c + " can't format " + this.arg.getName() + " arguments";
    }
}
