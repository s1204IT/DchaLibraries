package android.support.v8.renderscript;

import android.support.v8.renderscript.Script;

class ScriptIntrinsicConvolve3x3Thunker extends ScriptIntrinsicConvolve3x3 {
    android.renderscript.ScriptIntrinsicConvolve3x3 mN;

    @Override
    android.renderscript.ScriptIntrinsicConvolve3x3 getNObj() {
        return this.mN;
    }

    ScriptIntrinsicConvolve3x3Thunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicConvolve3x3Thunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker) e;
        ScriptIntrinsicConvolve3x3Thunker si = new ScriptIntrinsicConvolve3x3Thunker(0, rs);
        try {
            si.mN = android.renderscript.ScriptIntrinsicConvolve3x3.create(rst.mN, et.getNObj());
            return si;
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    @Override
    public void setInput(Allocation ain) {
        AllocationThunker aint = (AllocationThunker) ain;
        try {
            this.mN.setInput(aint.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setCoefficients(float[] v) {
        try {
            this.mN.setCoefficients(v);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void forEach(Allocation aout) {
        AllocationThunker aoutt = (AllocationThunker) aout;
        try {
            this.mN.forEach(aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public Script.KernelID getKernelID() {
        Script.KernelID k = createKernelID(0, 2, null, null);
        try {
            k.mN = this.mN.getKernelID();
            return k;
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public Script.FieldID getFieldID_Input() {
        Script.FieldID f = createFieldID(1, null);
        try {
            f.mN = this.mN.getFieldID_Input();
            return f;
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
}
