package java.sql;

import java.security.BasicPermission;

public final class SQLPermission extends BasicPermission {
    public SQLPermission(String str) {
        super("");
    }

    public SQLPermission(String str, String str2) {
        super("", "");
    }
}
