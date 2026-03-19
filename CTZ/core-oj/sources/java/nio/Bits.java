package java.nio;

import java.security.AccessController;
import sun.misc.Unsafe;
import sun.misc.VM;
import sun.security.action.GetPropertyAction;

class Bits {
    static final int JNI_COPY_FROM_ARRAY_THRESHOLD = 6;
    static final int JNI_COPY_TO_ARRAY_THRESHOLD = 6;
    static final long UNSAFE_COPY_THRESHOLD = 1048576;
    private static volatile long count;
    private static volatile long reservedMemory;
    private static volatile long totalCapacity;
    private static boolean unaligned;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private static int pageSize = -1;
    static final boolean $assertionsDisabled = false;
    private static boolean unalignedKnown = $assertionsDisabled;
    private static volatile long maxMemory = VM.maxDirectMemory();
    private static boolean memoryLimitSet = $assertionsDisabled;

    static native void copyFromIntArray(Object obj, long j, long j2, long j3);

    static native void copyFromLongArray(Object obj, long j, long j2, long j3);

    static native void copyFromShortArray(Object obj, long j, long j2, long j3);

    static native void copyToIntArray(long j, Object obj, long j2, long j3);

    static native void copyToLongArray(long j, Object obj, long j2, long j3);

    static native void copyToShortArray(long j, Object obj, long j2, long j3);

    private Bits() {
    }

    static short swap(short s) {
        return Short.reverseBytes(s);
    }

    static char swap(char c) {
        return Character.reverseBytes(c);
    }

    static int swap(int i) {
        return Integer.reverseBytes(i);
    }

    static long swap(long j) {
        return Long.reverseBytes(j);
    }

    private static char makeChar(byte b, byte b2) {
        return (char) ((b << 8) | (b2 & Character.DIRECTIONALITY_UNDEFINED));
    }

    static char getCharL(ByteBuffer byteBuffer, int i) {
        return makeChar(byteBuffer._get(i + 1), byteBuffer._get(i));
    }

    static char getCharL(long j) {
        return makeChar(_get(1 + j), _get(j));
    }

    static char getCharB(ByteBuffer byteBuffer, int i) {
        return makeChar(byteBuffer._get(i), byteBuffer._get(i + 1));
    }

    static char getCharB(long j) {
        return makeChar(_get(j), _get(j + 1));
    }

    static char getChar(ByteBuffer byteBuffer, int i, boolean z) {
        return z ? getCharB(byteBuffer, i) : getCharL(byteBuffer, i);
    }

    static char getChar(long j, boolean z) {
        return z ? getCharB(j) : getCharL(j);
    }

    private static byte char1(char c) {
        return (byte) (c >> '\b');
    }

    private static byte char0(char c) {
        return (byte) c;
    }

    static void putCharL(ByteBuffer byteBuffer, int i, char c) {
        byteBuffer._put(i, char0(c));
        byteBuffer._put(i + 1, char1(c));
    }

    static void putCharL(long j, char c) {
        _put(j, char0(c));
        _put(j + 1, char1(c));
    }

    static void putCharB(ByteBuffer byteBuffer, int i, char c) {
        byteBuffer._put(i, char1(c));
        byteBuffer._put(i + 1, char0(c));
    }

    static void putCharB(long j, char c) {
        _put(j, char1(c));
        _put(j + 1, char0(c));
    }

    static void putChar(ByteBuffer byteBuffer, int i, char c, boolean z) {
        if (z) {
            putCharB(byteBuffer, i, c);
        } else {
            putCharL(byteBuffer, i, c);
        }
    }

    static void putChar(long j, char c, boolean z) {
        if (z) {
            putCharB(j, c);
        } else {
            putCharL(j, c);
        }
    }

    private static short makeShort(byte b, byte b2) {
        return (short) ((b << 8) | (b2 & Character.DIRECTIONALITY_UNDEFINED));
    }

    static short getShortL(ByteBuffer byteBuffer, int i) {
        return makeShort(byteBuffer._get(i + 1), byteBuffer._get(i));
    }

    static short getShortL(long j) {
        return makeShort(_get(1 + j), _get(j));
    }

    static short getShortB(ByteBuffer byteBuffer, int i) {
        return makeShort(byteBuffer._get(i), byteBuffer._get(i + 1));
    }

    static short getShortB(long j) {
        return makeShort(_get(j), _get(j + 1));
    }

    static short getShort(ByteBuffer byteBuffer, int i, boolean z) {
        return z ? getShortB(byteBuffer, i) : getShortL(byteBuffer, i);
    }

    static short getShort(long j, boolean z) {
        return z ? getShortB(j) : getShortL(j);
    }

    private static byte short1(short s) {
        return (byte) (s >> 8);
    }

    private static byte short0(short s) {
        return (byte) s;
    }

    static void putShortL(ByteBuffer byteBuffer, int i, short s) {
        byteBuffer._put(i, short0(s));
        byteBuffer._put(i + 1, short1(s));
    }

    static void putShortL(long j, short s) {
        _put(j, short0(s));
        _put(j + 1, short1(s));
    }

    static void putShortB(ByteBuffer byteBuffer, int i, short s) {
        byteBuffer._put(i, short1(s));
        byteBuffer._put(i + 1, short0(s));
    }

    static void putShortB(long j, short s) {
        _put(j, short1(s));
        _put(j + 1, short0(s));
    }

    static void putShort(ByteBuffer byteBuffer, int i, short s, boolean z) {
        if (z) {
            putShortB(byteBuffer, i, s);
        } else {
            putShortL(byteBuffer, i, s);
        }
    }

    static void putShort(long j, short s, boolean z) {
        if (z) {
            putShortB(j, s);
        } else {
            putShortL(j, s);
        }
    }

    private static int makeInt(byte b, byte b2, byte b3, byte b4) {
        return (b << 24) | ((b2 & Character.DIRECTIONALITY_UNDEFINED) << 16) | ((b3 & Character.DIRECTIONALITY_UNDEFINED) << 8) | (b4 & Character.DIRECTIONALITY_UNDEFINED);
    }

    static int getIntL(ByteBuffer byteBuffer, int i) {
        return makeInt(byteBuffer._get(i + 3), byteBuffer._get(i + 2), byteBuffer._get(i + 1), byteBuffer._get(i));
    }

    static int getIntL(long j) {
        return makeInt(_get(3 + j), _get(2 + j), _get(1 + j), _get(j));
    }

    static int getIntB(ByteBuffer byteBuffer, int i) {
        return makeInt(byteBuffer._get(i), byteBuffer._get(i + 1), byteBuffer._get(i + 2), byteBuffer._get(i + 3));
    }

    static int getIntB(long j) {
        return makeInt(_get(j), _get(1 + j), _get(2 + j), _get(j + 3));
    }

    static int getInt(ByteBuffer byteBuffer, int i, boolean z) {
        return z ? getIntB(byteBuffer, i) : getIntL(byteBuffer, i);
    }

    static int getInt(long j, boolean z) {
        return z ? getIntB(j) : getIntL(j);
    }

    private static byte int3(int i) {
        return (byte) (i >> 24);
    }

    private static byte int2(int i) {
        return (byte) (i >> 16);
    }

    private static byte int1(int i) {
        return (byte) (i >> 8);
    }

    private static byte int0(int i) {
        return (byte) i;
    }

    static void putIntL(ByteBuffer byteBuffer, int i, int i2) {
        byteBuffer._put(i + 3, int3(i2));
        byteBuffer._put(i + 2, int2(i2));
        byteBuffer._put(i + 1, int1(i2));
        byteBuffer._put(i, int0(i2));
    }

    static void putIntL(long j, int i) {
        _put(3 + j, int3(i));
        _put(2 + j, int2(i));
        _put(1 + j, int1(i));
        _put(j, int0(i));
    }

    static void putIntB(ByteBuffer byteBuffer, int i, int i2) {
        byteBuffer._put(i, int3(i2));
        byteBuffer._put(i + 1, int2(i2));
        byteBuffer._put(i + 2, int1(i2));
        byteBuffer._put(i + 3, int0(i2));
    }

    static void putIntB(long j, int i) {
        _put(j, int3(i));
        _put(1 + j, int2(i));
        _put(2 + j, int1(i));
        _put(j + 3, int0(i));
    }

    static void putInt(ByteBuffer byteBuffer, int i, int i2, boolean z) {
        if (z) {
            putIntB(byteBuffer, i, i2);
        } else {
            putIntL(byteBuffer, i, i2);
        }
    }

    static void putInt(long j, int i, boolean z) {
        if (z) {
            putIntB(j, i);
        } else {
            putIntL(j, i);
        }
    }

    private static long makeLong(byte b, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7, byte b8) {
        return ((((long) b2) & 255) << 48) | (((long) b) << 56) | ((((long) b3) & 255) << 40) | ((((long) b4) & 255) << 32) | ((((long) b5) & 255) << 24) | ((((long) b6) & 255) << 16) | ((((long) b7) & 255) << 8) | (((long) b8) & 255);
    }

    static long getLongL(ByteBuffer byteBuffer, int i) {
        return makeLong(byteBuffer._get(i + 7), byteBuffer._get(i + 6), byteBuffer._get(i + 5), byteBuffer._get(i + 4), byteBuffer._get(i + 3), byteBuffer._get(i + 2), byteBuffer._get(i + 1), byteBuffer._get(i));
    }

    static long getLongL(long j) {
        return makeLong(_get(7 + j), _get(6 + j), _get(5 + j), _get(4 + j), _get(3 + j), _get(2 + j), _get(1 + j), _get(j));
    }

    static long getLongB(ByteBuffer byteBuffer, int i) {
        return makeLong(byteBuffer._get(i), byteBuffer._get(i + 1), byteBuffer._get(i + 2), byteBuffer._get(i + 3), byteBuffer._get(i + 4), byteBuffer._get(i + 5), byteBuffer._get(i + 6), byteBuffer._get(i + 7));
    }

    static long getLongB(long j) {
        return makeLong(_get(j), _get(1 + j), _get(2 + j), _get(3 + j), _get(4 + j), _get(5 + j), _get(6 + j), _get(j + 7));
    }

    static long getLong(ByteBuffer byteBuffer, int i, boolean z) {
        return z ? getLongB(byteBuffer, i) : getLongL(byteBuffer, i);
    }

    static long getLong(long j, boolean z) {
        return z ? getLongB(j) : getLongL(j);
    }

    private static byte long7(long j) {
        return (byte) (j >> 56);
    }

    private static byte long6(long j) {
        return (byte) (j >> 48);
    }

    private static byte long5(long j) {
        return (byte) (j >> 40);
    }

    private static byte long4(long j) {
        return (byte) (j >> 32);
    }

    private static byte long3(long j) {
        return (byte) (j >> 24);
    }

    private static byte long2(long j) {
        return (byte) (j >> 16);
    }

    private static byte long1(long j) {
        return (byte) (j >> 8);
    }

    private static byte long0(long j) {
        return (byte) j;
    }

    static void putLongL(ByteBuffer byteBuffer, int i, long j) {
        byteBuffer._put(i + 7, long7(j));
        byteBuffer._put(i + 6, long6(j));
        byteBuffer._put(i + 5, long5(j));
        byteBuffer._put(i + 4, long4(j));
        byteBuffer._put(i + 3, long3(j));
        byteBuffer._put(i + 2, long2(j));
        byteBuffer._put(i + 1, long1(j));
        byteBuffer._put(i, long0(j));
    }

    static void putLongL(long j, long j2) {
        _put(7 + j, long7(j2));
        _put(6 + j, long6(j2));
        _put(5 + j, long5(j2));
        _put(4 + j, long4(j2));
        _put(3 + j, long3(j2));
        _put(2 + j, long2(j2));
        _put(1 + j, long1(j2));
        _put(j, long0(j2));
    }

    static void putLongB(ByteBuffer byteBuffer, int i, long j) {
        byteBuffer._put(i, long7(j));
        byteBuffer._put(i + 1, long6(j));
        byteBuffer._put(i + 2, long5(j));
        byteBuffer._put(i + 3, long4(j));
        byteBuffer._put(i + 4, long3(j));
        byteBuffer._put(i + 5, long2(j));
        byteBuffer._put(i + 6, long1(j));
        byteBuffer._put(i + 7, long0(j));
    }

    static void putLongB(long j, long j2) {
        _put(j, long7(j2));
        _put(1 + j, long6(j2));
        _put(2 + j, long5(j2));
        _put(3 + j, long4(j2));
        _put(4 + j, long3(j2));
        _put(5 + j, long2(j2));
        _put(6 + j, long1(j2));
        _put(j + 7, long0(j2));
    }

    static void putLong(ByteBuffer byteBuffer, int i, long j, boolean z) {
        if (z) {
            putLongB(byteBuffer, i, j);
        } else {
            putLongL(byteBuffer, i, j);
        }
    }

    static void putLong(long j, long j2, boolean z) {
        if (z) {
            putLongB(j, j2);
        } else {
            putLongL(j, j2);
        }
    }

    static float getFloatL(ByteBuffer byteBuffer, int i) {
        return Float.intBitsToFloat(getIntL(byteBuffer, i));
    }

    static float getFloatL(long j) {
        return Float.intBitsToFloat(getIntL(j));
    }

    static float getFloatB(ByteBuffer byteBuffer, int i) {
        return Float.intBitsToFloat(getIntB(byteBuffer, i));
    }

    static float getFloatB(long j) {
        return Float.intBitsToFloat(getIntB(j));
    }

    static float getFloat(ByteBuffer byteBuffer, int i, boolean z) {
        return z ? getFloatB(byteBuffer, i) : getFloatL(byteBuffer, i);
    }

    static float getFloat(long j, boolean z) {
        return z ? getFloatB(j) : getFloatL(j);
    }

    static void putFloatL(ByteBuffer byteBuffer, int i, float f) {
        putIntL(byteBuffer, i, Float.floatToRawIntBits(f));
    }

    static void putFloatL(long j, float f) {
        putIntL(j, Float.floatToRawIntBits(f));
    }

    static void putFloatB(ByteBuffer byteBuffer, int i, float f) {
        putIntB(byteBuffer, i, Float.floatToRawIntBits(f));
    }

    static void putFloatB(long j, float f) {
        putIntB(j, Float.floatToRawIntBits(f));
    }

    static void putFloat(ByteBuffer byteBuffer, int i, float f, boolean z) {
        if (z) {
            putFloatB(byteBuffer, i, f);
        } else {
            putFloatL(byteBuffer, i, f);
        }
    }

    static void putFloat(long j, float f, boolean z) {
        if (z) {
            putFloatB(j, f);
        } else {
            putFloatL(j, f);
        }
    }

    static double getDoubleL(ByteBuffer byteBuffer, int i) {
        return Double.longBitsToDouble(getLongL(byteBuffer, i));
    }

    static double getDoubleL(long j) {
        return Double.longBitsToDouble(getLongL(j));
    }

    static double getDoubleB(ByteBuffer byteBuffer, int i) {
        return Double.longBitsToDouble(getLongB(byteBuffer, i));
    }

    static double getDoubleB(long j) {
        return Double.longBitsToDouble(getLongB(j));
    }

    static double getDouble(ByteBuffer byteBuffer, int i, boolean z) {
        return z ? getDoubleB(byteBuffer, i) : getDoubleL(byteBuffer, i);
    }

    static double getDouble(long j, boolean z) {
        return z ? getDoubleB(j) : getDoubleL(j);
    }

    static void putDoubleL(ByteBuffer byteBuffer, int i, double d) {
        putLongL(byteBuffer, i, Double.doubleToRawLongBits(d));
    }

    static void putDoubleL(long j, double d) {
        putLongL(j, Double.doubleToRawLongBits(d));
    }

    static void putDoubleB(ByteBuffer byteBuffer, int i, double d) {
        putLongB(byteBuffer, i, Double.doubleToRawLongBits(d));
    }

    static void putDoubleB(long j, double d) {
        putLongB(j, Double.doubleToRawLongBits(d));
    }

    static void putDouble(ByteBuffer byteBuffer, int i, double d, boolean z) {
        if (z) {
            putDoubleB(byteBuffer, i, d);
        } else {
            putDoubleL(byteBuffer, i, d);
        }
    }

    static void putDouble(long j, double d, boolean z) {
        if (z) {
            putDoubleB(j, d);
        } else {
            putDoubleL(j, d);
        }
    }

    private static byte _get(long j) {
        return unsafe.getByte(j);
    }

    private static void _put(long j, byte b) {
        unsafe.putByte(j, b);
    }

    static Unsafe unsafe() {
        return unsafe;
    }

    static ByteOrder byteOrder() {
        return byteOrder;
    }

    static int pageSize() {
        if (pageSize == -1) {
            pageSize = unsafe().pageSize();
        }
        return pageSize;
    }

    static int pageCount(long j) {
        return ((int) ((j + ((long) pageSize())) - 1)) / pageSize();
    }

    static boolean unaligned() {
        if (unalignedKnown) {
            return unaligned;
        }
        String str = (String) AccessController.doPrivileged(new GetPropertyAction("os.arch"));
        unaligned = (str.equals("i386") || str.equals("x86") || str.equals("amd64") || str.equals("x86_64")) ? true : $assertionsDisabled;
        unalignedKnown = true;
        return unaligned;
    }

    static void reserveMemory(long j, int i) {
        synchronized (Bits.class) {
            if (!memoryLimitSet && VM.isBooted()) {
                maxMemory = VM.maxDirectMemory();
                memoryLimitSet = true;
            }
            long j2 = i;
            if (j2 <= maxMemory - totalCapacity) {
                reservedMemory += j;
                totalCapacity += j2;
                count++;
                return;
            }
            System.gc();
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (Bits.class) {
                if (totalCapacity + j2 > maxMemory) {
                    throw new OutOfMemoryError("Direct buffer memory");
                }
                reservedMemory += j;
                totalCapacity += j2;
                count++;
            }
        }
    }

    static synchronized void unreserveMemory(long j, int i) {
        if (reservedMemory > 0) {
            reservedMemory -= j;
            totalCapacity -= (long) i;
            count--;
        }
    }

    static void copyFromArray(Object obj, long j, long j2, long j3, long j4) {
        long j5 = j3;
        long j6 = j + j2;
        long j7 = j4;
        while (j7 > 0) {
            long j8 = j7 > UNSAFE_COPY_THRESHOLD ? 1048576L : j7;
            unsafe.copyMemoryFromPrimitiveArray(obj, j6, j5, j8);
            j7 -= j8;
            j6 += j8;
            j5 += j8;
        }
    }

    static void copyToArray(long j, Object obj, long j2, long j3, long j4) {
        long j5 = j;
        long j6 = j2 + j3;
        long j7 = j4;
        while (j7 > 0) {
            long j8 = j7 > UNSAFE_COPY_THRESHOLD ? 1048576L : j7;
            unsafe.copyMemoryToPrimitiveArray(j5, obj, j6, j8);
            j7 -= j8;
            j5 += j8;
            j6 += j8;
        }
    }

    static void copyFromCharArray(Object obj, long j, long j2, long j3) {
        copyFromShortArray(obj, j, j2, j3);
    }

    static void copyToCharArray(long j, Object obj, long j2, long j3) {
        copyToShortArray(j, obj, j2, j3);
    }
}
