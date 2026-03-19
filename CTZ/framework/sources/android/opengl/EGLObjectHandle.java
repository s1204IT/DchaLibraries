package android.opengl;

import com.android.internal.logging.nano.MetricsProto;

public abstract class EGLObjectHandle {
    private final long mHandle;

    @Deprecated
    protected EGLObjectHandle(int i) {
        this.mHandle = i;
    }

    protected EGLObjectHandle(long j) {
        this.mHandle = j;
    }

    @Deprecated
    public int getHandle() {
        if ((this.mHandle & 4294967295L) != this.mHandle) {
            throw new UnsupportedOperationException();
        }
        return (int) this.mHandle;
    }

    public long getNativeHandle() {
        return this.mHandle;
    }

    public int hashCode() {
        return MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + ((int) (this.mHandle ^ (this.mHandle >>> 32)));
    }
}
