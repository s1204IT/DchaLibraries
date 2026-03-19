package java.lang;

import java.security.BasicPermission;

public final class RuntimePermission extends BasicPermission {
    private static final long serialVersionUID = 7399184964622342223L;

    public RuntimePermission(String str) {
        super(str);
    }

    public RuntimePermission(String str, String str2) {
        super(str, str2);
    }
}
