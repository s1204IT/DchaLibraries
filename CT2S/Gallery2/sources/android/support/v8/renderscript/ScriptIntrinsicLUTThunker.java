package android.support.v8.renderscript;

import android.support.v8.renderscript.Script;

class ScriptIntrinsicLUTThunker extends ScriptIntrinsicLUT {
    android.renderscript.ScriptIntrinsicLUT mN;

    @Override
    android.renderscript.ScriptIntrinsicLUT getNObj() {
        return this.mN;
    }

    private ScriptIntrinsicLUTThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicLUTThunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker) e;
        ScriptIntrinsicLUTThunker si = new ScriptIntrinsicLUTThunker(0, rs);
        try {
            si.mN = android.renderscript.ScriptIntrinsicLUT.create(rst.mN, et.getNObj());
            return si;
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    @Override
    public void setRed(int index, int value) {
        try {
            this.mN.setRed(index, value);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setGreen(int index, int value) {
        try {
            this.mN.setGreen(index, value);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setBlue(int index, int value) {
        try {
            this.mN.setBlue(index, value);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setAlpha(int index, int value) {
        try {
            this.mN.setAlpha(index, value);
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
