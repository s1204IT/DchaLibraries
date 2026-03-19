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

    public NativeAllocationRegistry(ClassLoader classLoader, long j, long j2) {
        if (j2 < 0) {
            throw new IllegalArgumentException("Invalid native allocation size: " + j2);
        }
        this.classLoader = classLoader;
        this.freeFunction = j;
        this.size = j2;
    }

    public Runnable registerNativeAllocation(Object obj, long j) {
        if (obj == null) {
            throw new IllegalArgumentException("referent is null");
        }
        if (j == 0) {
            throw new IllegalArgumentException("nativePtr is null");
        }
        try {
            CleanerThunk cleanerThunk = new CleanerThunk();
            CleanerRunner cleanerRunner = new CleanerRunner(Cleaner.create(obj, cleanerThunk));
            registerNativeAllocation(this.size);
            cleanerThunk.setNativePtr(j);
            return cleanerRunner;
        } catch (VirtualMachineError e) {
            applyFreeFunction(this.freeFunction, j);
            throw e;
        }
    }

    public Runnable registerNativeAllocation(Object obj, Allocator allocator) {
        if (obj == null) {
            throw new IllegalArgumentException("referent is null");
        }
        CleanerThunk cleanerThunk = new CleanerThunk();
        Cleaner cleanerCreate = Cleaner.create(obj, cleanerThunk);
        CleanerRunner cleanerRunner = new CleanerRunner(cleanerCreate);
        long jAllocate = allocator.allocate();
        if (jAllocate == 0) {
            cleanerCreate.clean();
            return null;
        }
        registerNativeAllocation(this.size);
        cleanerThunk.setNativePtr(jAllocate);
        return cleanerRunner;
    }

    private class CleanerThunk implements Runnable {
        private long nativePtr = 0;

        public CleanerThunk() {
        }

        @Override
        public void run() {
            if (this.nativePtr != 0) {
                NativeAllocationRegistry.applyFreeFunction(NativeAllocationRegistry.this.freeFunction, this.nativePtr);
                NativeAllocationRegistry.registerNativeFree(NativeAllocationRegistry.this.size);
            }
        }

        public void setNativePtr(long j) {
            this.nativePtr = j;
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

    private static void registerNativeAllocation(long j) {
        VMRuntime.getRuntime().registerNativeAllocation((int) Math.min(j, 2147483647L));
    }

    private static void registerNativeFree(long j) {
        VMRuntime.getRuntime().registerNativeFree((int) Math.min(j, 2147483647L));
    }
}
