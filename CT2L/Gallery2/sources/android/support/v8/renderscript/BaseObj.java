package android.support.v8.renderscript;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BaseObj {
    private boolean mDestroyed;
    private int mID;
    RenderScript mRS;

    BaseObj(int id, RenderScript rs) {
        rs.validate();
        this.mRS = rs;
        this.mID = id;
        this.mDestroyed = false;
    }

    void setID(int id) {
        if (this.mID != 0) {
            throw new RSRuntimeException("Internal Error, reset of object ID.");
        }
        this.mID = id;
    }

    int getID(RenderScript rs) {
        this.mRS.validate();
        if (this.mDestroyed) {
            throw new RSInvalidStateException("using a destroyed object.");
        }
        if (this.mID == 0) {
            throw new RSRuntimeException("Internal error: Object id 0.");
        }
        if (rs != null && rs != this.mRS) {
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
        boolean shouldDestroy = false;
        synchronized (this) {
            if (!this.mDestroyed) {
                shouldDestroy = true;
                this.mDestroyed = true;
            }
        }
        if (shouldDestroy) {
            ReentrantReadWriteLock.ReadLock rlock = this.mRS.mRWLock.readLock();
            rlock.lock();
            if (this.mRS.isAlive()) {
                this.mRS.nObjDestroy(this.mID);
            }
            rlock.unlock();
            this.mRS = null;
            this.mID = 0;
        }
    }

    protected void finalize() throws Throwable {
        helpDestroy();
        super.finalize();
    }

    public void destroy() {
        if (this.mDestroyed) {
            throw new RSInvalidStateException("Object already destroyed.");
        }
        helpDestroy();
    }

    public int hashCode() {
        return this.mID;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            RenderScript renderScript = this.mRS;
            if (RenderScript.isNative) {
                return ((RenderScriptThunker) this.mRS).equals(this, obj);
            }
            BaseObj b = (BaseObj) obj;
            return this.mID == b.mID;
        }
        return false;
    }
}
