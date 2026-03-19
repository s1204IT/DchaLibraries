package java.util;

public class IllegalFormatFlagsException extends IllegalFormatException {
    private static final long serialVersionUID = 790824;
    private String flags;

    public IllegalFormatFlagsException(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.flags = str;
    }

    public String getFlags() {
        return this.flags;
    }

    @Override
    public String getMessage() {
        return "Flags = '" + this.flags + "'";
    }
}
