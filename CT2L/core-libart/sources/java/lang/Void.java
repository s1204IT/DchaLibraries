package java.lang;

import java.lang.reflect.Method;
import libcore.util.EmptyArray;

public final class Void {
    public static final Class<Void> TYPE = lookupType();

    private static Class<Void> lookupType() {
        try {
            Method method = Runnable.class.getMethod("run", EmptyArray.CLASS);
            return method.getReturnType();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private Void() {
    }
}
