package java.util;

public class UnknownFormatConversionException extends IllegalFormatException {
    private static final long serialVersionUID = 19060418;
    private String s;

    public UnknownFormatConversionException(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.s = str;
    }

    public String getConversion() {
        return this.s;
    }

    @Override
    public String getMessage() {
        return String.format("Conversion = '%s'", this.s);
    }
}
