package sun.nio.ch;

class AllocatedNativeObject extends NativeObject {
    AllocatedNativeObject(int i, boolean z) {
        super(i, z);
    }

    synchronized void free() {
        if (this.allocationAddress != 0) {
            unsafe.freeMemory(this.allocationAddress);
            this.allocationAddress = 0L;
        }
    }
}
