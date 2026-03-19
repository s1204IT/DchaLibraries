package javax.net.ssl;

import java.security.BasicPermission;

public final class SSLPermission extends BasicPermission {
    public SSLPermission(String str) {
        super("");
    }

    public SSLPermission(String str, String str2) {
        super("", "");
    }
}
