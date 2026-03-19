package sun.nio.ch;

import java.nio.ByteBuffer;
import sun.misc.Cleaner;

class IOVecWrapper {
    private static final int BASE_OFFSET = 0;
    final long address;
    private final ByteBuffer[] buf;
    private final int[] position;
    private final int[] remaining;
    private final ByteBuffer[] shadow;
    private final int size;
    private final AllocatedNativeObject vecArray;
    private static final ThreadLocal<IOVecWrapper> cached = new ThreadLocal<>();
    static int addressSize = Util.unsafe().addressSize();
    private static final int LEN_OFFSET = addressSize;
    private static final int SIZE_IOVEC = (short) (addressSize * 2);

    private static class Deallocator implements Runnable {
        private final AllocatedNativeObject obj;

        Deallocator(AllocatedNativeObject allocatedNativeObject) {
            this.obj = allocatedNativeObject;
        }

        @Override
        public void run() {
            this.obj.free();
        }
    }

    private IOVecWrapper(int i) {
        this.size = i;
        this.buf = new ByteBuffer[i];
        this.position = new int[i];
        this.remaining = new int[i];
        this.shadow = new ByteBuffer[i];
        this.vecArray = new AllocatedNativeObject(i * SIZE_IOVEC, false);
        this.address = this.vecArray.address();
    }

    static IOVecWrapper get(int i) {
        IOVecWrapper iOVecWrapper = cached.get();
        if (iOVecWrapper != null && iOVecWrapper.size < i) {
            iOVecWrapper.vecArray.free();
            iOVecWrapper = null;
        }
        if (iOVecWrapper == null) {
            IOVecWrapper iOVecWrapper2 = new IOVecWrapper(i);
            Cleaner.create(iOVecWrapper2, new Deallocator(iOVecWrapper2.vecArray));
            cached.set(iOVecWrapper2);
            return iOVecWrapper2;
        }
        return iOVecWrapper;
    }

    void setBuffer(int i, ByteBuffer byteBuffer, int i2, int i3) {
        this.buf[i] = byteBuffer;
        this.position[i] = i2;
        this.remaining[i] = i3;
    }

    void setShadow(int i, ByteBuffer byteBuffer) {
        this.shadow[i] = byteBuffer;
    }

    ByteBuffer getBuffer(int i) {
        return this.buf[i];
    }

    int getPosition(int i) {
        return this.position[i];
    }

    int getRemaining(int i) {
        return this.remaining[i];
    }

    ByteBuffer getShadow(int i) {
        return this.shadow[i];
    }

    void clearRefs(int i) {
        this.buf[i] = null;
        this.shadow[i] = null;
    }

    void putBase(int i, long j) {
        int i2 = (SIZE_IOVEC * i) + 0;
        if (addressSize == 4) {
            this.vecArray.putInt(i2, (int) j);
        } else {
            this.vecArray.putLong(i2, j);
        }
    }

    void putLen(int i, long j) {
        int i2 = (SIZE_IOVEC * i) + LEN_OFFSET;
        if (addressSize == 4) {
            this.vecArray.putInt(i2, (int) j);
        } else {
            this.vecArray.putLong(i2, j);
        }
    }
}
