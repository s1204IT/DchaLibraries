package java.util;

public class UnknownFormatFlagsException extends IllegalFormatException {
    private static final long serialVersionUID = 19370506;
    private String flags;

    public UnknownFormatFlagsException(String str) {
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
        return "Flags = " + this.flags;
    }
}
