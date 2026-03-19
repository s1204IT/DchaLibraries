package android.support.v8.renderscript;

import android.util.SparseArray;

public class Script extends BaseObj {
    private final SparseArray<Object> mFIDs;
    private final SparseArray<Object> mIIDs;
    private final SparseArray<Object> mKIDs;
    private boolean mUseIncSupp;

    protected void setIncSupp(boolean z) {
        this.mUseIncSupp = z;
    }

    long getDummyAlloc(Allocation allocation) {
        if (allocation != null) {
            Type type = allocation.getType();
            long jNIncAllocationCreateTyped = this.mRS.nIncAllocationCreateTyped(allocation.getID(this.mRS), type.getDummyType(this.mRS, type.getElement().getDummyElement(this.mRS)), type.getX() * type.getElement().getBytesSize());
            allocation.setIncAllocID(jNIncAllocationCreateTyped);
            return jNIncAllocationCreateTyped;
        }
        return 0L;
    }

    protected void forEach(int i, Allocation allocation, Allocation allocation2, FieldPacker fieldPacker) {
        long id;
        if (allocation == null && allocation2 == null) {
            throw new RSIllegalArgumentException("At least one of ain or aout is required to be non-null.");
        }
        long id2 = 0;
        if (allocation == null) {
            id = 0;
        } else {
            id = allocation.getID(this.mRS);
        }
        if (allocation2 != null) {
            id2 = allocation2.getID(this.mRS);
        }
        long j = id2;
        byte[] data = fieldPacker != null ? fieldPacker.getData() : null;
        if (this.mUseIncSupp) {
            this.mRS.nScriptForEach(getID(this.mRS), i, getDummyAlloc(allocation), getDummyAlloc(allocation2), data, this.mUseIncSupp);
        } else {
            this.mRS.nScriptForEach(getID(this.mRS), i, id, j, data, this.mUseIncSupp);
        }
    }

    Script(long j, RenderScript renderScript) {
        super(j, renderScript);
        this.mKIDs = new SparseArray<>();
        this.mIIDs = new SparseArray<>();
        this.mFIDs = new SparseArray<>();
        this.mUseIncSupp = false;
    }

    public void setVar(int i, float f) {
        this.mRS.nScriptSetVarF(getID(this.mRS), i, f, this.mUseIncSupp);
    }

    public void setVar(int i, BaseObj baseObj) {
        if (!this.mUseIncSupp) {
            this.mRS.nScriptSetVarObj(getID(this.mRS), i, baseObj != null ? baseObj.getID(this.mRS) : 0L, this.mUseIncSupp);
        } else {
            this.mRS.nScriptSetVarObj(getID(this.mRS), i, baseObj == null ? 0L : getDummyAlloc((Allocation) baseObj), this.mUseIncSupp);
        }
    }
}
