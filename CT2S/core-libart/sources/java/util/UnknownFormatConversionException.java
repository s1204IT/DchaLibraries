package java.util;

public class UnknownFormatConversionException extends IllegalFormatException {
    private static final long serialVersionUID = 19060418;
    private final String s;

    public UnknownFormatConversionException(String s) {
        if (s == null) {
            throw new NullPointerException("s == null");
        }
        this.s = s;
    }

    public String getConversion() {
        return this.s;
    }

    @Override
    public String getMessage() {
        return "Conversion: " + this.s;
    }
}
