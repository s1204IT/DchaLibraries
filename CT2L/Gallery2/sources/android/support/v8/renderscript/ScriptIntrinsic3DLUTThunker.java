package android.support.v8.renderscript;

import android.support.v8.renderscript.Script;

class ScriptIntrinsic3DLUTThunker extends ScriptIntrinsic3DLUT {
    android.renderscript.ScriptIntrinsic3DLUT mN;

    @Override
    android.renderscript.ScriptIntrinsic3DLUT getNObj() {
        return this.mN;
    }

    private ScriptIntrinsic3DLUTThunker(int id, RenderScript rs, Element e) {
        super(id, rs, e);
    }

    public static ScriptIntrinsic3DLUTThunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker) e;
        ScriptIntrinsic3DLUTThunker lut = new ScriptIntrinsic3DLUTThunker(0, rs, e);
        try {
            lut.mN = android.renderscript.ScriptIntrinsic3DLUT.create(rst.mN, et.getNObj());
            return lut;
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    @Override
    public void setLUT(Allocation lut) {
        AllocationThunker lutt = (AllocationThunker) lut;
        try {
            this.mN.setLUT(lutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void forEach(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker) ain;
        AllocationThunker aoutt = (AllocationThunker) aout;
        try {
            this.mN.forEach(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public Script.KernelID getKernelID() {
        Script.KernelID k = createKernelID(0, 3, null, null);
        try {
            k.mN = this.mN.getKernelID();
            return k;
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
}
