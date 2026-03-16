package android.support.v8.renderscript;

import android.support.v8.renderscript.Script;

class ScriptIntrinsicColorMatrixThunker extends ScriptIntrinsicColorMatrix {
    android.renderscript.ScriptIntrinsicColorMatrix mN;

    @Override
    android.renderscript.ScriptIntrinsicColorMatrix getNObj() {
        return this.mN;
    }

    private ScriptIntrinsicColorMatrixThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicColorMatrixThunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker) e;
        ScriptIntrinsicColorMatrixThunker cm = new ScriptIntrinsicColorMatrixThunker(0, rs);
        try {
            cm.mN = android.renderscript.ScriptIntrinsicColorMatrix.create(rst.mN, et.getNObj());
            return cm;
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    @Override
    public void setColorMatrix(Matrix4f m) {
        try {
            this.mN.setColorMatrix(new android.renderscript.Matrix4f(m.getArray()));
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setColorMatrix(Matrix3f m) {
        try {
            this.mN.setColorMatrix(new android.renderscript.Matrix3f(m.getArray()));
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setGreyscale() {
        try {
            this.mN.setGreyscale();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setYUVtoRGB() {
        try {
            this.mN.setYUVtoRGB();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setRGBtoYUV() {
        try {
            this.mN.setRGBtoYUV();
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
