package java.nio;

final class NIOAccess {
    NIOAccess() {
    }

    static long getBasePointer(Buffer b) {
        long address = b.effectiveDirectAddress;
        if (address == 0) {
            return 0L;
        }
        return ((long) (b.position << b._elementSizeShift)) + address;
    }

    static Object getBaseArray(Buffer b) {
        if (b.hasArray()) {
            return b.array();
        }
        return null;
    }

    static int getBaseArrayOffset(Buffer b) {
        if (b.hasArray()) {
            return (b.arrayOffset() + b.position) << b._elementSizeShift;
        }
        return 0;
    }
}
