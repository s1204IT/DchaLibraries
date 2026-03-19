package android.graphics;

import libcore.util.NativeAllocationRegistry;

public class Shader {
    private Runnable mCleaner;
    private Matrix mLocalMatrix;
    private long mNativeInstance;

    private static native long nativeGetFinalizer();

    private static class NoImagePreloadHolder {
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(Shader.class.getClassLoader(), Shader.nativeGetFinalizer(), 50);

        private NoImagePreloadHolder() {
        }
    }

    @Deprecated
    public Shader() {
    }

    public enum TileMode {
        CLAMP(0),
        REPEAT(1),
        MIRROR(2);

        final int nativeInt;

        TileMode(int i) {
            this.nativeInt = i;
        }
    }

    public boolean getLocalMatrix(Matrix matrix) {
        if (this.mLocalMatrix != null) {
            matrix.set(this.mLocalMatrix);
            return true;
        }
        return false;
    }

    public void setLocalMatrix(Matrix matrix) {
        if (matrix == null || matrix.isIdentity()) {
            if (this.mLocalMatrix != null) {
                this.mLocalMatrix = null;
                discardNativeInstance();
                return;
            }
            return;
        }
        if (this.mLocalMatrix == null) {
            this.mLocalMatrix = new Matrix(matrix);
            discardNativeInstance();
        } else if (!this.mLocalMatrix.equals(matrix)) {
            this.mLocalMatrix.set(matrix);
            discardNativeInstance();
        }
    }

    long createNativeInstance(long j) {
        return 0L;
    }

    protected final void discardNativeInstance() {
        if (this.mNativeInstance != 0) {
            this.mCleaner.run();
            this.mCleaner = null;
            this.mNativeInstance = 0L;
        }
    }

    protected void verifyNativeInstance() {
    }

    protected Shader copy() {
        Shader shader = new Shader();
        copyLocalMatrix(shader);
        return shader;
    }

    protected void copyLocalMatrix(Shader shader) {
        shader.mLocalMatrix.set(this.mLocalMatrix);
    }

    public final long getNativeInstance() {
        long j;
        verifyNativeInstance();
        if (this.mNativeInstance == 0) {
            if (this.mLocalMatrix == null) {
                j = 0;
            } else {
                j = this.mLocalMatrix.native_instance;
            }
            this.mNativeInstance = createNativeInstance(j);
            if (this.mNativeInstance != 0) {
                this.mCleaner = NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mNativeInstance);
            }
        }
        return this.mNativeInstance;
    }
}
