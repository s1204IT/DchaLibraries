package java.io;

class Bits {
    Bits() {
    }

    static boolean getBoolean(byte[] bArr, int i) {
        return bArr[i] != 0;
    }

    static char getChar(byte[] bArr, int i) {
        return (char) ((bArr[i + 1] & Character.DIRECTIONALITY_UNDEFINED) + (bArr[i] << 8));
    }

    static short getShort(byte[] bArr, int i) {
        return (short) ((bArr[i + 1] & Character.DIRECTIONALITY_UNDEFINED) + (bArr[i] << 8));
    }

    static int getInt(byte[] bArr, int i) {
        return (bArr[i + 3] & Character.DIRECTIONALITY_UNDEFINED) + ((bArr[i + 2] & Character.DIRECTIONALITY_UNDEFINED) << 8) + ((bArr[i + 1] & Character.DIRECTIONALITY_UNDEFINED) << 16) + (bArr[i] << 24);
    }

    static float getFloat(byte[] bArr, int i) {
        return Float.intBitsToFloat(getInt(bArr, i));
    }

    static long getLong(byte[] bArr, int i) {
        return (((long) bArr[i + 7]) & 255) + ((((long) bArr[i + 6]) & 255) << 8) + ((((long) bArr[i + 5]) & 255) << 16) + ((((long) bArr[i + 4]) & 255) << 24) + ((((long) bArr[i + 3]) & 255) << 32) + ((((long) bArr[i + 2]) & 255) << 40) + ((255 & ((long) bArr[i + 1])) << 48) + (((long) bArr[i]) << 56);
    }

    static double getDouble(byte[] bArr, int i) {
        return Double.longBitsToDouble(getLong(bArr, i));
    }

    static void putBoolean(byte[] bArr, int i, boolean z) {
        bArr[i] = z ? (byte) 1 : (byte) 0;
    }

    static void putChar(byte[] bArr, int i, char c) {
        bArr[i + 1] = (byte) c;
        bArr[i] = (byte) (c >>> '\b');
    }

    static void putShort(byte[] bArr, int i, short s) {
        bArr[i + 1] = (byte) s;
        bArr[i] = (byte) (s >>> 8);
    }

    static void putInt(byte[] bArr, int i, int i2) {
        bArr[i + 3] = (byte) i2;
        bArr[i + 2] = (byte) (i2 >>> 8);
        bArr[i + 1] = (byte) (i2 >>> 16);
        bArr[i] = (byte) (i2 >>> 24);
    }

    static void putFloat(byte[] bArr, int i, float f) {
        putInt(bArr, i, Float.floatToIntBits(f));
    }

    static void putLong(byte[] bArr, int i, long j) {
        bArr[i + 7] = (byte) j;
        bArr[i + 6] = (byte) (j >>> 8);
        bArr[i + 5] = (byte) (j >>> 16);
        bArr[i + 4] = (byte) (j >>> 24);
        bArr[i + 3] = (byte) (j >>> 32);
        bArr[i + 2] = (byte) (j >>> 40);
        bArr[i + 1] = (byte) (j >>> 48);
        bArr[i] = (byte) (j >>> 56);
    }

    static void putDouble(byte[] bArr, int i, double d) {
        putLong(bArr, i, Double.doubleToLongBits(d));
    }
}
