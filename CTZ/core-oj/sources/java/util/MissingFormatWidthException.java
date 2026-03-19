package java.util;

public class MissingFormatWidthException extends IllegalFormatException {
    private static final long serialVersionUID = 15560123;
    private String s;

    public MissingFormatWidthException(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.s = str;
    }

    public String getFormatSpecifier() {
        return this.s;
    }

    @Override
    public String getMessage() {
        return this.s;
    }
}
