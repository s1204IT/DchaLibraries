package javax.security.auth;

import java.security.BasicPermission;

public final class AuthPermission extends BasicPermission {
    public AuthPermission(String str) {
        super("");
    }

    public AuthPermission(String str, String str2) {
        super("", "");
    }
}
