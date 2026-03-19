package android.hardware.camera2.marshal;

import android.util.Rational;
import com.android.internal.util.Preconditions;

public final class MarshalHelpers {
    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_DOUBLE = 8;
    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_INT32 = 4;
    public static final int SIZEOF_INT64 = 8;
    public static final int SIZEOF_RATIONAL = 8;

    public static int getPrimitiveTypeSize(int i) {
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 4;
            case 2:
                return 4;
            case 3:
                return 8;
            case 4:
                return 8;
            case 5:
                return 8;
            default:
                throw new UnsupportedOperationException("Unknown type, can't get size for " + i);
        }
    }

    public static <T> Class<T> checkPrimitiveClass(Class<T> cls) {
        Preconditions.checkNotNull(cls, "klass must not be null");
        if (isPrimitiveClass(cls)) {
            return cls;
        }
        throw new UnsupportedOperationException("Unsupported class '" + cls + "'; expected a metadata primitive class");
    }

    public static <T> boolean isPrimitiveClass(Class<T> cls) {
        if (cls == null) {
            return false;
        }
        return cls == Byte.TYPE || cls == Byte.class || cls == Integer.TYPE || cls == Integer.class || cls == Float.TYPE || cls == Float.class || cls == Long.TYPE || cls == Long.class || cls == Double.TYPE || cls == Double.class || cls == Rational.class;
    }

    public static <T> Class<T> wrapClassIfPrimitive(Class<T> cls) {
        if (cls == Byte.TYPE) {
            return Byte.class;
        }
        if (cls == Integer.TYPE) {
            return Integer.class;
        }
        if (cls == Float.TYPE) {
            return Float.class;
        }
        if (cls == Long.TYPE) {
            return Long.class;
        }
        if (cls == Double.TYPE) {
            return Double.class;
        }
        return cls;
    }

    public static String toStringNativeType(int i) {
        switch (i) {
            case 0:
                return "TYPE_BYTE";
            case 1:
                return "TYPE_INT32";
            case 2:
                return "TYPE_FLOAT";
            case 3:
                return "TYPE_INT64";
            case 4:
                return "TYPE_DOUBLE";
            case 5:
                return "TYPE_RATIONAL";
            default:
                return "UNKNOWN(" + i + ")";
        }
    }

    public static int checkNativeType(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return i;
            default:
                throw new UnsupportedOperationException("Unknown nativeType " + i);
        }
    }

    public static int checkNativeTypeEquals(int i, int i2) {
        if (i != i2) {
            throw new UnsupportedOperationException(String.format("Expected native type %d, but got %d", Integer.valueOf(i), Integer.valueOf(i2)));
        }
        return i2;
    }

    private MarshalHelpers() {
        throw new AssertionError();
    }
}
