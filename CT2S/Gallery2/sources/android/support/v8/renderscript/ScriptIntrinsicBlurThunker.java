package android.support.v8.renderscript;

import android.support.v8.renderscript.Script;

class ScriptIntrinsicBlurThunker extends ScriptIntrinsicBlur {
    android.renderscript.ScriptIntrinsicBlur mN;

    @Override
    android.renderscript.ScriptIntrinsicBlur getNObj() {
        return this.mN;
    }

    protected ScriptIntrinsicBlurThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicBlurThunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker) e;
        ScriptIntrinsicBlurThunker blur = new ScriptIntrinsicBlurThunker(0, rs);
        try {
            blur.mN = android.renderscript.ScriptIntrinsicBlur.create(rst.mN, et.getNObj());
            return blur;
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
    public void setRadius(float radius) {
        try {
            this.mN.setRadius(radius);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void forEach(Allocation aout) {
        AllocationThunker aoutt = (AllocationThunker) aout;
        if (aoutt != null) {
            try {
                this.mN.forEach(aoutt.getNObj());
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
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
