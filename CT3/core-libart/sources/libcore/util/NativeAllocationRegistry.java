package libcore.util;

import dalvik.system.VMRuntime;
import sun.misc.Cleaner;

public class NativeAllocationRegistry {
    private final ClassLoader classLoader;
    private final long freeFunction;
    private final long size;

    public interface Allocator {
        long allocate();
    }

    public static native void applyFreeFunction(long j, long j2);

    public NativeAllocationRegistry(ClassLoader classLoader, long freeFunction, long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Invalid native allocation size: " + size);
        }
        this.classLoader = classLoader;
        this.freeFunction = freeFunction;
        this.size = size;
    }

    public Runnable registerNativeAllocation(Object referent, long nativePtr) {
        if (referent == null) {
            throw new IllegalArgumentException("referent is null");
        }
        if (nativePtr == 0) {
            throw new IllegalArgumentException("nativePtr is null");
        }
        try {
            registerNativeAllocation(this.size);
            Cleaner cleaner = Cleaner.create(referent, new CleanerThunk(nativePtr));
            return new CleanerRunner(cleaner);
        } catch (OutOfMemoryError oome) {
            applyFreeFunction(this.freeFunction, nativePtr);
            throw oome;
        }
    }

    public Runnable registerNativeAllocation(Object referent, Allocator allocator) {
        if (referent == null) {
            throw new IllegalArgumentException("referent is null");
        }
        registerNativeAllocation(this.size);
        CleanerThunk thunk = new CleanerThunk();
        Cleaner cleaner = Cleaner.create(referent, thunk);
        long nativePtr = allocator.allocate();
        if (nativePtr == 0) {
            cleaner.clean();
            return null;
        }
        thunk.setNativePtr(nativePtr);
        return new CleanerRunner(cleaner);
    }

    private class CleanerThunk implements Runnable {
        private long nativePtr;

        public CleanerThunk() {
            this.nativePtr = 0L;
        }

        public CleanerThunk(long nativePtr) {
            this.nativePtr = nativePtr;
        }

        @Override
        public void run() {
            if (this.nativePtr != 0) {
                NativeAllocationRegistry.applyFreeFunction(NativeAllocationRegistry.this.freeFunction, this.nativePtr);
            }
            NativeAllocationRegistry.registerNativeFree(NativeAllocationRegistry.this.size);
        }

        public void setNativePtr(long nativePtr) {
            this.nativePtr = nativePtr;
        }
    }

    private static class CleanerRunner implements Runnable {
        private final Cleaner cleaner;

        public CleanerRunner(Cleaner cleaner) {
            this.cleaner = cleaner;
        }

        @Override
        public void run() {
            this.cleaner.clean();
        }
    }

    private static void registerNativeAllocation(long size) {
        VMRuntime.getRuntime().registerNativeAllocation((int) Math.min(size, 2147483647L));
    }

    private static void registerNativeFree(long size) {
        VMRuntime.getRuntime().registerNativeFree((int) Math.min(size, 2147483647L));
    }
}
