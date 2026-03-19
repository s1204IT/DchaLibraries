package java.nio;

public final class NIOAccess {
    public static long getBasePointer(Buffer buffer) {
        long j = buffer.address;
        if (j == 0) {
            return 0L;
        }
        return j + ((long) (buffer.position << buffer._elementSizeShift));
    }

    static Object getBaseArray(Buffer buffer) {
        if (buffer.hasArray()) {
            return buffer.array();
        }
        return null;
    }

    static int getBaseArrayOffset(Buffer buffer) {
        if (buffer.hasArray()) {
            return (buffer.arrayOffset() + buffer.position) << buffer._elementSizeShift;
        }
        return 0;
    }
}
