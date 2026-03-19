package android.hardware.camera2.utils;

public final class HashCodeHelpers {
    public static int hashCode(int... iArr) {
        if (iArr == null) {
            return 0;
        }
        int i = 1;
        for (int i2 : iArr) {
            i = ((i << 5) - i) ^ i2;
        }
        return i;
    }

    public static int hashCode(float... fArr) {
        if (fArr == null) {
            return 0;
        }
        int iFloatToIntBits = 1;
        for (float f : fArr) {
            iFloatToIntBits = ((iFloatToIntBits << 5) - iFloatToIntBits) ^ Float.floatToIntBits(f);
        }
        return iFloatToIntBits;
    }

    public static <T> int hashCodeGeneric(T... tArr) {
        int iHashCode;
        if (tArr == null) {
            return 0;
        }
        int i = 1;
        for (T t : tArr) {
            if (t != null) {
                iHashCode = t.hashCode();
            } else {
                iHashCode = 0;
            }
            i = ((i << 5) - i) ^ iHashCode;
        }
        return i;
    }
}
