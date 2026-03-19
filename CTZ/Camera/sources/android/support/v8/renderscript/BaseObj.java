package android.support.v8.renderscript;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BaseObj {
    private boolean mDestroyed;
    private long mID;
    RenderScript mRS;

    BaseObj(long j, RenderScript renderScript) {
        renderScript.validate();
        this.mRS = renderScript;
        this.mID = j;
        this.mDestroyed = false;
    }

    long getID(RenderScript renderScript) {
        this.mRS.validate();
        if (this.mDestroyed) {
            throw new RSInvalidStateException("using a destroyed object.");
        }
        if (this.mID == 0) {
            throw new RSRuntimeException("Internal error: Object id 0.");
        }
        if (renderScript != null && renderScript != this.mRS) {
            throw new RSInvalidStateException("using object with mismatched context.");
        }
        return this.mID;
    }

    android.renderscript.BaseObj getNObj() {
        return null;
    }

    void checkValid() {
        if (this.mID == 0 && getNObj() == null) {
            throw new RSIllegalArgumentException("Invalid object.");
        }
    }

    private void helpDestroy() {
        boolean z;
        synchronized (this) {
            z = true;
            if (!this.mDestroyed) {
                this.mDestroyed = true;
            } else {
                z = false;
            }
        }
        if (z) {
            ReentrantReadWriteLock.ReadLock lock = this.mRS.mRWLock.readLock();
            lock.lock();
            if (this.mRS.isAlive()) {
                this.mRS.nObjDestroy(this.mID);
            }
            lock.unlock();
            this.mRS = null;
            this.mID = 0L;
        }
    }

    protected void finalize() throws Throwable {
        helpDestroy();
        super.finalize();
    }

    public int hashCode() {
        return (int) ((this.mID & 268435455) ^ (this.mID >> 32));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass() && this.mID == ((BaseObj) obj).mID) {
            return true;
        }
        return false;
    }
}
