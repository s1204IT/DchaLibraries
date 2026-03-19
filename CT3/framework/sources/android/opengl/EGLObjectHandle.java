package android.opengl;

import android.security.keymaster.KeymasterArguments;

public abstract class EGLObjectHandle {
    private final long mHandle;

    @Deprecated
    protected EGLObjectHandle(int handle) {
        this.mHandle = handle;
    }

    protected EGLObjectHandle(long handle) {
        this.mHandle = handle;
    }

    @Deprecated
    public int getHandle() {
        if ((this.mHandle & KeymasterArguments.UINT32_MAX_VALUE) != this.mHandle) {
            throw new UnsupportedOperationException();
        }
        return (int) this.mHandle;
    }

    public long getNativeHandle() {
        return this.mHandle;
    }

    public int hashCode() {
        int result = ((int) (this.mHandle ^ (this.mHandle >>> 32))) + 527;
        return result;
    }
}
