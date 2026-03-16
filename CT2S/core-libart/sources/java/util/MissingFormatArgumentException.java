package java.util;

public class MissingFormatArgumentException extends IllegalFormatException {
    private static final long serialVersionUID = 19190115;
    private final String s;

    public MissingFormatArgumentException(String s) {
        if (s == null) {
            throw new NullPointerException("s == null");
        }
        this.s = s;
    }

    public String getFormatSpecifier() {
        return this.s;
    }

    @Override
    public String getMessage() {
        return "Format specifier: " + this.s;
    }
}
