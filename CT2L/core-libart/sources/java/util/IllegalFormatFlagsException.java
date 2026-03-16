package java.util;

import java.io.Serializable;

public class IllegalFormatFlagsException extends IllegalFormatException implements Serializable {
    private static final long serialVersionUID = 790824;
    private final String flags;

    public IllegalFormatFlagsException(String flags) {
        if (flags == null) {
            throw new NullPointerException("flags == null");
        }
        this.flags = flags;
    }

    public String getFlags() {
        return this.flags;
    }

    @Override
    public String getMessage() {
        return this.flags;
    }
}
