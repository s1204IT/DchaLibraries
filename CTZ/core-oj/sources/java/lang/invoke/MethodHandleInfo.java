package java.lang.invoke;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Objects;

public interface MethodHandleInfo {
    public static final int REF_getField = 1;
    public static final int REF_getStatic = 2;
    public static final int REF_invokeInterface = 9;
    public static final int REF_invokeSpecial = 7;
    public static final int REF_invokeStatic = 6;
    public static final int REF_invokeVirtual = 5;
    public static final int REF_newInvokeSpecial = 8;
    public static final int REF_putField = 3;
    public static final int REF_putStatic = 4;

    Class<?> getDeclaringClass();

    MethodType getMethodType();

    int getModifiers();

    String getName();

    int getReferenceKind();

    <T extends Member> T reflectAs(Class<T> cls, MethodHandles.Lookup lookup);

    default boolean isVarArgs() {
        if (refKindIsField(getReferenceKind())) {
            return false;
        }
        return Modifier.isTransient(getModifiers());
    }

    static String referenceKindToString(int i) {
        if (!refKindIsValid(i)) {
            throw MethodHandleStatics.newIllegalArgumentException("invalid reference kind", Integer.valueOf(i));
        }
        return refKindName(i);
    }

    static String toString(int i, Class<?> cls, String str, MethodType methodType) {
        Objects.requireNonNull(str);
        Objects.requireNonNull(methodType);
        return String.format("%s %s.%s:%s", referenceKindToString(i), cls.getName(), str, methodType);
    }

    static boolean refKindIsValid(int i) {
        return i >= 1 && i <= 9;
    }

    static boolean refKindIsField(int i) {
        return i <= 4;
    }

    static String refKindName(int i) {
        switch (i) {
            case 1:
                return "getField";
            case 2:
                return "getStatic";
            case 3:
                return "putField";
            case 4:
                return "putStatic";
            case 5:
                return "invokeVirtual";
            case 6:
                return "invokeStatic";
            case 7:
                return "invokeSpecial";
            case 8:
                return "newInvokeSpecial";
            case 9:
                return "invokeInterface";
            default:
                return "REF_???";
        }
    }
}
