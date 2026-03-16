package libcore.util;

import dalvik.bytecode.Opcodes;
import java.lang.reflect.Field;
import java.util.Arrays;

public final class Objects {
    private Objects() {
    }

    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static int hashCode(Object o) {
        if (o == null) {
            return 0;
        }
        return o.hashCode();
    }

    public static String toString(Object o) {
        int i;
        Class<?> c = o.getClass();
        StringBuilder sb = new StringBuilder();
        sb.append(c.getSimpleName()).append('[');
        Field[] arr$ = c.getDeclaredFields();
        int len$ = arr$.length;
        int i$ = 0;
        int i2 = 0;
        while (i$ < len$) {
            Field f = arr$[i$];
            if ((f.getModifiers() & Opcodes.OP_FLOAT_TO_LONG) != 0) {
                i = i2;
            } else {
                f.setAccessible(true);
                try {
                    Object value = f.get(o);
                    i = i2 + 1;
                    if (i2 > 0) {
                        try {
                            sb.append(',');
                        } catch (IllegalAccessException e) {
                            unexpected = e;
                            throw new AssertionError(unexpected);
                        }
                    }
                    sb.append(f.getName());
                    sb.append('=');
                    if (value.getClass().isArray()) {
                        if (value.getClass() == boolean[].class) {
                            sb.append(Arrays.toString((boolean[]) value));
                        } else if (value.getClass() == byte[].class) {
                            sb.append(Arrays.toString((byte[]) value));
                        } else if (value.getClass() == char[].class) {
                            sb.append(Arrays.toString((char[]) value));
                        } else if (value.getClass() == double[].class) {
                            sb.append(Arrays.toString((double[]) value));
                        } else if (value.getClass() == float[].class) {
                            sb.append(Arrays.toString((float[]) value));
                        } else if (value.getClass() == int[].class) {
                            sb.append(Arrays.toString((int[]) value));
                        } else if (value.getClass() == long[].class) {
                            sb.append(Arrays.toString((long[]) value));
                        } else if (value.getClass() == short[].class) {
                            sb.append(Arrays.toString((short[]) value));
                        } else {
                            sb.append(Arrays.toString((Object[]) value));
                        }
                    } else if (value.getClass() == Character.class) {
                        sb.append('\'').append(value).append('\'');
                    } else if (value.getClass() == String.class) {
                        sb.append('\"').append(value).append('\"');
                    } else {
                        sb.append(value);
                    }
                } catch (IllegalAccessException e2) {
                    unexpected = e2;
                }
            }
            i$++;
            i2 = i;
        }
        sb.append("]");
        return sb.toString();
    }
}
