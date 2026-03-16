package android.support.v8.renderscript;

import android.support.v4.app.NotificationCompat;
import android.support.v8.renderscript.Script;

public class ScriptIntrinsicLUT extends ScriptIntrinsic {
    private final byte[] mCache;
    private boolean mDirty;
    private final Matrix4f mMatrix;
    private Allocation mTables;

    protected ScriptIntrinsicLUT(int id, RenderScript rs) {
        super(id, rs);
        this.mMatrix = new Matrix4f();
        this.mCache = new byte[1024];
        this.mDirty = true;
    }

    public static ScriptIntrinsicLUT create(RenderScript rs, Element e) {
        if (RenderScript.isNative) {
            return ScriptIntrinsicLUTThunker.create(rs, e);
        }
        int id = rs.nScriptIntrinsicCreate(3, e.getID(rs));
        ScriptIntrinsicLUT si = new ScriptIntrinsicLUT(id, rs);
        si.mTables = Allocation.createSized(rs, Element.U8(rs), 1024);
        for (int ct = 0; ct < 256; ct++) {
            si.mCache[ct] = (byte) ct;
            si.mCache[ct + NotificationCompat.FLAG_LOCAL_ONLY] = (byte) ct;
            si.mCache[ct + NotificationCompat.FLAG_GROUP_SUMMARY] = (byte) ct;
            si.mCache[ct + 768] = (byte) ct;
        }
        si.setVar(0, si.mTables);
        return si;
    }

    private void validate(int index, int value) {
        if (index < 0 || index > 255) {
            throw new RSIllegalArgumentException("Index out of range (0-255).");
        }
        if (value < 0 || value > 255) {
            throw new RSIllegalArgumentException("Value out of range (0-255).");
        }
    }

    public void setRed(int index, int value) {
        validate(index, value);
        this.mCache[index] = (byte) value;
        this.mDirty = true;
    }

    public void setGreen(int index, int value) {
        validate(index, value);
        this.mCache[index + NotificationCompat.FLAG_LOCAL_ONLY] = (byte) value;
        this.mDirty = true;
    }

    public void setBlue(int index, int value) {
        validate(index, value);
        this.mCache[index + NotificationCompat.FLAG_GROUP_SUMMARY] = (byte) value;
        this.mDirty = true;
    }

    public void setAlpha(int index, int value) {
        validate(index, value);
        this.mCache[index + 768] = (byte) value;
        this.mDirty = true;
    }

    public void forEach(Allocation ain, Allocation aout) {
        if (this.mDirty) {
            this.mDirty = false;
            this.mTables.copyFromUnchecked(this.mCache);
        }
        forEach(0, ain, aout, null);
    }

    public Script.KernelID getKernelID() {
        return createKernelID(0, 3, null, null);
    }
}
