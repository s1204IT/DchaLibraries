package android.support.v8.renderscript;

import android.support.v8.renderscript.Script;

class ScriptIntrinsicYuvToRGBThunker extends ScriptIntrinsicYuvToRGB {
    android.renderscript.ScriptIntrinsicYuvToRGB mN;

    @Override
    android.renderscript.ScriptIntrinsicYuvToRGB getNObj() {
        return this.mN;
    }

    private ScriptIntrinsicYuvToRGBThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicYuvToRGBThunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker) e;
        ScriptIntrinsicYuvToRGBThunker si = new ScriptIntrinsicYuvToRGBThunker(0, rs);
        try {
            si.mN = android.renderscript.ScriptIntrinsicYuvToRGB.create(rst.mN, et.getNObj());
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
        Script.FieldID f = createFieldID(0, null);
        try {
            f.mN = this.mN.getFieldID_Input();
            return f;
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }
}
