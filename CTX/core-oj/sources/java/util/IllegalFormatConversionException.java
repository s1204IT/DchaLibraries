package java.util;

public class IllegalFormatConversionException extends IllegalFormatException {
    private static final long serialVersionUID = 17000126;
    private Class<?> arg;
    private char c;

    public IllegalFormatConversionException(char c, Class<?> cls) {
        if (cls == null) {
            throw new NullPointerException();
        }
        this.c = c;
        this.arg = cls;
    }

    public char getConversion() {
        return this.c;
    }

    public Class<?> getArgumentClass() {
        return this.arg;
    }

    @Override
    public String getMessage() {
        return String.format("%c != %s", Character.valueOf(this.c), this.arg.getName());
    }
}
