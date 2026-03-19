package java.util;

public class FormatFlagsConversionMismatchException extends IllegalFormatException {
    private static final long serialVersionUID = 19120414;
    private char c;
    private String f;

    public FormatFlagsConversionMismatchException(String str, char c) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.f = str;
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
        return "Conversion = " + this.c + ", Flags = " + this.f;
    }
}
