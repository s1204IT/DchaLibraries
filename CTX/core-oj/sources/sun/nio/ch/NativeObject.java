package sun.nio.ch;

import java.nio.ByteOrder;
import sun.misc.Unsafe;

class NativeObject {
    static final boolean $assertionsDisabled = false;
    private final long address;
    protected long allocationAddress;
    protected static final Unsafe unsafe = Unsafe.getUnsafe();
    private static ByteOrder byteOrder = null;
    private static int pageSize = -1;

    NativeObject(long j) {
        this.allocationAddress = j;
        this.address = j;
    }

    NativeObject(long j, long j2) {
        this.allocationAddress = j;
        this.address = j + j2;
    }

    protected NativeObject(int i, boolean z) {
        if (!z) {
            this.allocationAddress = unsafe.allocateMemory(i);
            this.address = this.allocationAddress;
        } else {
            int iPageSize = pageSize();
            long jAllocateMemory = unsafe.allocateMemory(i + iPageSize);
            this.allocationAddress = jAllocateMemory;
            this.address = (((long) iPageSize) + jAllocateMemory) - (((long) (iPageSize - 1)) & jAllocateMemory);
        }
    }

    long address() {
        return this.address;
    }

    long allocationAddress() {
        return this.allocationAddress;
    }

    NativeObject subObject(int i) {
        return new NativeObject(((long) i) + this.address);
    }

    NativeObject getObject(int i) {
        long j;
        int iAddressSize = addressSize();
        if (iAddressSize == 4) {
            j = unsafe.getInt(((long) i) + this.address) & (-1);
        } else if (iAddressSize == 8) {
            j = unsafe.getLong(((long) i) + this.address);
        } else {
            throw new InternalError("Address size not supported");
        }
        return new NativeObject(j);
    }

    void putObject(int i, NativeObject nativeObject) {
        int iAddressSize = addressSize();
        if (iAddressSize == 4) {
            putInt(i, (int) (nativeObject.address & (-1)));
        } else {
            if (iAddressSize == 8) {
                putLong(i, nativeObject.address);
                return;
            }
            throw new InternalError("Address size not supported");
        }
    }

    final byte getByte(int i) {
        return unsafe.getByte(((long) i) + this.address);
    }

    final void putByte(int i, byte b) {
        unsafe.putByte(((long) i) + this.address, b);
    }

    final short getShort(int i) {
        return unsafe.getShort(((long) i) + this.address);
    }

    final void putShort(int i, short s) {
        unsafe.putShort(((long) i) + this.address, s);
    }

    final char getChar(int i) {
        return unsafe.getChar(((long) i) + this.address);
    }

    final void putChar(int i, char c) {
        unsafe.putChar(((long) i) + this.address, c);
    }

    final int getInt(int i) {
        return unsafe.getInt(((long) i) + this.address);
    }

    final void putInt(int i, int i2) {
        unsafe.putInt(((long) i) + this.address, i2);
    }

    final long getLong(int i) {
        return unsafe.getLong(((long) i) + this.address);
    }

    final void putLong(int i, long j) {
        unsafe.putLong(((long) i) + this.address, j);
    }

    final float getFloat(int i) {
        return unsafe.getFloat(((long) i) + this.address);
    }

    final void putFloat(int i, float f) {
        unsafe.putFloat(((long) i) + this.address, f);
    }

    final double getDouble(int i) {
        return unsafe.getDouble(((long) i) + this.address);
    }

    final void putDouble(int i, double d) {
        unsafe.putDouble(((long) i) + this.address, d);
    }

    static int addressSize() {
        return unsafe.addressSize();
    }

    static ByteOrder byteOrder() {
        if (byteOrder != null) {
            return byteOrder;
        }
        long jAllocateMemory = unsafe.allocateMemory(8L);
        try {
            unsafe.putLong(jAllocateMemory, 72623859790382856L);
            byte b = unsafe.getByte(jAllocateMemory);
            if (b == 1) {
                byteOrder = ByteOrder.BIG_ENDIAN;
            } else if (b == 8) {
                byteOrder = ByteOrder.LITTLE_ENDIAN;
            }
            unsafe.freeMemory(jAllocateMemory);
            return byteOrder;
        } catch (Throwable th) {
            unsafe.freeMemory(jAllocateMemory);
            throw th;
        }
    }

    static int pageSize() {
        if (pageSize == -1) {
            pageSize = unsafe.pageSize();
        }
        return pageSize;
    }
}
