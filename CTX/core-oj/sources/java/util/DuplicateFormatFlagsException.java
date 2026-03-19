package java.util;

public class DuplicateFormatFlagsException extends IllegalFormatException {
    private static final long serialVersionUID = 18890531;
    private String flags;

    public DuplicateFormatFlagsException(String str) {
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
        return String.format("Flags = '%s'", this.flags);
    }
}
