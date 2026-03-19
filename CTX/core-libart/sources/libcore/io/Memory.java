package libcore.io;

import android.icu.lang.UCharacterEnums;
import dalvik.annotation.optimization.FastNative;
import java.nio.ByteOrder;

public final class Memory {
    public static native void memmove(Object obj, int i, Object obj2, int i2, long j);

    @FastNative
    public static native byte peekByte(long j);

    public static native void peekByteArray(long j, byte[] bArr, int i, int i2);

    public static native void peekCharArray(long j, char[] cArr, int i, int i2, boolean z);

    public static native void peekDoubleArray(long j, double[] dArr, int i, int i2, boolean z);

    public static native void peekFloatArray(long j, float[] fArr, int i, int i2, boolean z);

    public static native void peekIntArray(long j, int[] iArr, int i, int i2, boolean z);

    @FastNative
    private static native int peekIntNative(long j);

    public static native void peekLongArray(long j, long[] jArr, int i, int i2, boolean z);

    @FastNative
    private static native long peekLongNative(long j);

    public static native void peekShortArray(long j, short[] sArr, int i, int i2, boolean z);

    @FastNative
    private static native short peekShortNative(long j);

    @FastNative
    public static native void pokeByte(long j, byte b);

    public static native void pokeByteArray(long j, byte[] bArr, int i, int i2);

    public static native void pokeCharArray(long j, char[] cArr, int i, int i2, boolean z);

    public static native void pokeDoubleArray(long j, double[] dArr, int i, int i2, boolean z);

    public static native void pokeFloatArray(long j, float[] fArr, int i, int i2, boolean z);

    public static native void pokeIntArray(long j, int[] iArr, int i, int i2, boolean z);

    @FastNative
    private static native void pokeIntNative(long j, int i);

    public static native void pokeLongArray(long j, long[] jArr, int i, int i2, boolean z);

    @FastNative
    private static native void pokeLongNative(long j, long j2);

    public static native void pokeShortArray(long j, short[] sArr, int i, int i2, boolean z);

    @FastNative
    private static native void pokeShortNative(long j, short s);

    public static native void unsafeBulkGet(Object obj, int i, int i2, byte[] bArr, int i3, int i4, boolean z);

    public static native void unsafeBulkPut(byte[] bArr, int i, int i2, Object obj, int i3, int i4, boolean z);

    private Memory() {
    }

    public static int peekInt(byte[] bArr, int i, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            int i2 = i + 1;
            int i3 = i2 + 1;
            int i4 = ((bArr[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 24) | ((bArr[i2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16);
            int i5 = i3 + 1;
            return ((bArr[i5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 0) | i4 | ((bArr[i3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8);
        }
        int i6 = i + 1;
        int i7 = i6 + 1;
        int i8 = ((bArr[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 0) | ((bArr[i6] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8);
        int i9 = i7 + 1;
        return ((bArr[i9] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 24) | i8 | ((bArr[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16);
    }

    public static long peekLong(byte[] bArr, int i, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            int i2 = i + 1;
            int i3 = i2 + 1;
            int i4 = ((bArr[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 24) | ((bArr[i2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16);
            int i5 = i3 + 1;
            int i6 = i4 | ((bArr[i3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8);
            int i7 = i5 + 1;
            int i8 = i6 | ((bArr[i5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 0);
            int i9 = i7 + 1;
            int i10 = i9 + 1;
            int i11 = ((bArr[i9] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16) | ((bArr[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 24);
            int i12 = i10 + 1;
            return (((long) i8) << 32) | (((long) (((bArr[i12] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 0) | i11 | ((bArr[i10] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8))) & 4294967295L);
        }
        int i13 = i + 1;
        int i14 = i13 + 1;
        int i15 = ((bArr[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 0) | ((bArr[i13] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8);
        int i16 = i14 + 1;
        int i17 = i15 | ((bArr[i14] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16);
        int i18 = i16 + 1;
        int i19 = i17 | ((bArr[i16] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 24);
        int i20 = i18 + 1;
        int i21 = i20 + 1;
        int i22 = ((bArr[i20] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 8) | ((bArr[i18] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 0);
        int i23 = i22 | ((bArr[i21] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 16);
        return (((long) i19) & 4294967295L) | (((long) (((bArr[i21 + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) << 24) | i23)) << 32);
    }

    public static short peekShort(byte[] bArr, int i, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            return (short) ((bArr[i + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) | (bArr[i] << 8));
        }
        return (short) ((bArr[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) | (bArr[i + 1] << 8));
    }

    public static void pokeInt(byte[] bArr, int i, int i2, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            int i3 = i + 1;
            bArr[i] = (byte) ((i2 >> 24) & 255);
            int i4 = i3 + 1;
            bArr[i3] = (byte) ((i2 >> 16) & 255);
            bArr[i4] = (byte) ((i2 >> 8) & 255);
            bArr[i4 + 1] = (byte) ((i2 >> 0) & 255);
            return;
        }
        int i5 = i + 1;
        bArr[i] = (byte) ((i2 >> 0) & 255);
        int i6 = i5 + 1;
        bArr[i5] = (byte) ((i2 >> 8) & 255);
        bArr[i6] = (byte) ((i2 >> 16) & 255);
        bArr[i6 + 1] = (byte) ((i2 >> 24) & 255);
    }

    public static void pokeLong(byte[] bArr, int i, long j, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            int i2 = (int) (j >> 32);
            int i3 = i + 1;
            bArr[i] = (byte) ((i2 >> 24) & 255);
            int i4 = i3 + 1;
            bArr[i3] = (byte) ((i2 >> 16) & 255);
            int i5 = i4 + 1;
            bArr[i4] = (byte) ((i2 >> 8) & 255);
            int i6 = i5 + 1;
            bArr[i5] = (byte) ((i2 >> 0) & 255);
            int i7 = (int) j;
            int i8 = i6 + 1;
            bArr[i6] = (byte) ((i7 >> 24) & 255);
            int i9 = i8 + 1;
            bArr[i8] = (byte) ((i7 >> 16) & 255);
            bArr[i9] = (byte) ((i7 >> 8) & 255);
            bArr[i9 + 1] = (byte) ((i7 >> 0) & 255);
            return;
        }
        int i10 = (int) j;
        int i11 = i + 1;
        bArr[i] = (byte) ((i10 >> 0) & 255);
        int i12 = i11 + 1;
        bArr[i11] = (byte) ((i10 >> 8) & 255);
        int i13 = i12 + 1;
        bArr[i12] = (byte) ((i10 >> 16) & 255);
        int i14 = i13 + 1;
        bArr[i13] = (byte) ((i10 >> 24) & 255);
        int i15 = (int) (j >> 32);
        int i16 = i14 + 1;
        bArr[i14] = (byte) ((i15 >> 0) & 255);
        int i17 = i16 + 1;
        bArr[i16] = (byte) ((i15 >> 8) & 255);
        bArr[i17] = (byte) ((i15 >> 16) & 255);
        bArr[i17 + 1] = (byte) ((i15 >> 24) & 255);
    }

    public static void pokeShort(byte[] bArr, int i, short s, ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            bArr[i] = (byte) ((s >> 8) & 255);
            bArr[i + 1] = (byte) ((s >> 0) & 255);
        } else {
            bArr[i] = (byte) ((s >> 0) & 255);
            bArr[i + 1] = (byte) ((s >> 8) & 255);
        }
    }

    public static int peekInt(long j, boolean z) {
        int iPeekIntNative = peekIntNative(j);
        if (z) {
            return Integer.reverseBytes(iPeekIntNative);
        }
        return iPeekIntNative;
    }

    public static long peekLong(long j, boolean z) {
        long jPeekLongNative = peekLongNative(j);
        if (z) {
            return Long.reverseBytes(jPeekLongNative);
        }
        return jPeekLongNative;
    }

    public static short peekShort(long j, boolean z) {
        short sPeekShortNative = peekShortNative(j);
        if (z) {
            return Short.reverseBytes(sPeekShortNative);
        }
        return sPeekShortNative;
    }

    public static void pokeInt(long j, int i, boolean z) {
        if (z) {
            i = Integer.reverseBytes(i);
        }
        pokeIntNative(j, i);
    }

    public static void pokeLong(long j, long j2, boolean z) {
        if (z) {
            j2 = Long.reverseBytes(j2);
        }
        pokeLongNative(j, j2);
    }

    public static void pokeShort(long j, short s, boolean z) {
        if (z) {
            s = Short.reverseBytes(s);
        }
        pokeShortNative(j, s);
    }
}
