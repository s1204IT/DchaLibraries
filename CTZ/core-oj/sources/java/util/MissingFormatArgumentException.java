package java.util;

public class MissingFormatArgumentException extends IllegalFormatException {
    private static final long serialVersionUID = 19190115;
    private String s;

    public MissingFormatArgumentException(String str) {
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
        return "Format specifier '" + this.s + "'";
    }
}
