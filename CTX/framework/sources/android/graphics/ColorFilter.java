package android.graphics;

import libcore.util.NativeAllocationRegistry;

public class ColorFilter {
    private Runnable mCleaner;
    private long mNativeInstance;

    private static native long nativeGetFinalizer();

    private static class NoImagePreloadHolder {
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(ColorFilter.class.getClassLoader(), ColorFilter.nativeGetFinalizer(), 50);

        private NoImagePreloadHolder() {
        }
    }

    @Deprecated
    public ColorFilter() {
    }

    long createNativeInstance() {
        return 0L;
    }

    void discardNativeInstance() {
        if (this.mNativeInstance != 0) {
            this.mCleaner.run();
            this.mCleaner = null;
            this.mNativeInstance = 0L;
        }
    }

    public long getNativeInstance() {
        if (this.mNativeInstance == 0) {
            this.mNativeInstance = createNativeInstance();
            if (this.mNativeInstance != 0) {
                this.mCleaner = NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mNativeInstance);
            }
        }
        return this.mNativeInstance;
    }
}
