package java.util;

public class DuplicateFormatFlagsException extends IllegalFormatException {
    private static final long serialVersionUID = 18890531;
    private final String flags;

    public DuplicateFormatFlagsException(String f) {
        if (f == null) {
            throw new NullPointerException("f == null");
        }
        this.flags = f;
    }

    public String getFlags() {
        return this.flags;
    }

    @Override
    public String getMessage() {
        return this.flags;
    }
}
