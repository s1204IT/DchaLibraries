package sun.nio.fs;

import sun.misc.Cleaner;
import sun.misc.Unsafe;

class NativeBuffer {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private final long address;
    private final Cleaner cleaner;
    private Object owner;
    private final int size;

    private static class Deallocator implements Runnable {
        private final long address;

        Deallocator(long j) {
            this.address = j;
        }

        @Override
        public void run() {
            NativeBuffer.unsafe.freeMemory(this.address);
        }
    }

    NativeBuffer(int i) {
        this.address = unsafe.allocateMemory(i);
        this.size = i;
        this.cleaner = Cleaner.create(this, new Deallocator(this.address));
    }

    void release() {
        NativeBuffers.releaseNativeBuffer(this);
    }

    long address() {
        return this.address;
    }

    int size() {
        return this.size;
    }

    Cleaner cleaner() {
        return this.cleaner;
    }

    void setOwner(Object obj) {
        this.owner = obj;
    }

    Object owner() {
        return this.owner;
    }
}
