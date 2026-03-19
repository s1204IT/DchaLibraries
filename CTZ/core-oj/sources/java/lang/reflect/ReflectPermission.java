package java.lang.reflect;

import java.security.BasicPermission;

public final class ReflectPermission extends BasicPermission {
    public ReflectPermission(String str) {
        super(str);
    }

    public ReflectPermission(String str, String str2) {
        super("", "");
    }
}
