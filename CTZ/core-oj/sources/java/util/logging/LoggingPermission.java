package java.util.logging;

import java.security.BasicPermission;

public final class LoggingPermission extends BasicPermission {
    public LoggingPermission(String str, String str2) throws IllegalArgumentException {
        super("", "");
    }
}
