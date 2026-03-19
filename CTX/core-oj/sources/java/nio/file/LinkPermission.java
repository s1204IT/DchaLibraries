package java.nio.file;

import java.security.BasicPermission;

public final class LinkPermission extends BasicPermission {
    static final long serialVersionUID = -1441492453772213220L;

    private void checkName(String str) {
        if (!str.equals("hard") && !str.equals("symbolic")) {
            throw new IllegalArgumentException("name: " + str);
        }
    }

    public LinkPermission(String str) {
        super(str);
        checkName(str);
    }

    public LinkPermission(String str, String str2) {
        super(str);
        checkName(str);
        if (str2 != null && str2.length() > 0) {
            throw new IllegalArgumentException("actions: " + str2);
        }
    }
}
