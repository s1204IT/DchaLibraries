package android.opengl;

import android.widget.ExpandableListView;

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
        if ((this.mHandle & ExpandableListView.PACKED_POSITION_VALUE_NULL) != this.mHandle) {
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
