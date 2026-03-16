package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Script;
import android.support.v8.renderscript.Type;
import com.android.gallery3d.R;

public class ImageFilterGrad extends ImageFilterRS {
    static final boolean $assertionsDisabled;
    FilterGradRepresentation mParameters = new FilterGradRepresentation();
    private ScriptC_grad mScript;
    private Bitmap mSourceBitmap;

    static {
        $assertionsDisabled = !ImageFilterGrad.class.desiredAssertionStatus();
    }

    public ImageFilterGrad() {
        this.mName = "grad";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterGradRepresentation();
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        this.mParameters = (FilterGradRepresentation) representation;
    }

    @Override
    protected void resetAllocations() {
    }

    @Override
    public void resetScripts() {
        if (this.mScript != null) {
            this.mScript.destroy();
            this.mScript = null;
        }
    }

    @Override
    protected void createFilter(Resources res, float scaleFactor, int quality) {
        createFilter(res, scaleFactor, quality, getInPixelsAllocation());
    }

    @Override
    protected void createFilter(Resources res, float scaleFactor, int quality, Allocation in) {
        RenderScript rsCtx = getRenderScriptContext();
        Type.Builder tb_float = new Type.Builder(rsCtx, Element.F32_4(rsCtx));
        tb_float.setX(in.getType().getX());
        tb_float.setY(in.getType().getY());
        this.mScript = new ScriptC_grad(rsCtx, res, R.raw.grad);
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (quality != 0) {
            this.mSourceBitmap = bitmap;
            Bitmap ret = super.apply(bitmap, scaleFactor, quality);
            this.mSourceBitmap = null;
            return ret;
        }
        return bitmap;
    }

    @Override
    protected void bindScriptValues() {
        int width = getInPixelsAllocation().getType().getX();
        int height = getInPixelsAllocation().getType().getY();
        this.mScript.set_inputWidth(width);
        this.mScript.set_inputHeight(height);
    }

    @Override
    protected void runFilter() {
        int[] x1 = this.mParameters.getXPos1();
        int[] y1 = this.mParameters.getYPos1();
        int[] x2 = this.mParameters.getXPos2();
        int[] y2 = this.mParameters.getYPos2();
        int width = getInPixelsAllocation().getType().getX();
        int height = getInPixelsAllocation().getType().getY();
        Matrix m = getOriginalToScreenMatrix(width, height);
        float[] coord = new float[2];
        for (int i = 0; i < x1.length; i++) {
            coord[0] = x1[i];
            coord[1] = y1[i];
            m.mapPoints(coord);
            x1[i] = (int) coord[0];
            y1[i] = (int) coord[1];
            coord[0] = x2[i];
            coord[1] = y2[i];
            m.mapPoints(coord);
            x2[i] = (int) coord[0];
            y2[i] = (int) coord[1];
        }
        this.mScript.set_mask(this.mParameters.getMask());
        this.mScript.set_xPos1(x1);
        this.mScript.set_yPos1(y1);
        this.mScript.set_xPos2(x2);
        this.mScript.set_yPos2(y2);
        this.mScript.set_brightness(this.mParameters.getBrightness());
        this.mScript.set_contrast(this.mParameters.getContrast());
        this.mScript.set_saturation(this.mParameters.getSaturation());
        this.mScript.invoke_setupGradParams();
        runSelectiveAdjust(getInPixelsAllocation(), getOutPixelsAllocation());
    }

    private void runSelectiveAdjust(Allocation in, Allocation out) {
        int width = in.getType().getX();
        int height = in.getType().getY();
        Script.LaunchOptions options = new Script.LaunchOptions();
        options.setX(0, width);
        for (int ty = 0; ty < height; ty += 64) {
            int endy = ty + 64;
            if (endy > height) {
                endy = height;
            }
            options.setY(ty, endy);
            this.mScript.forEach_selectiveAdjust(in, out, options);
            if (checkStop()) {
                return;
            }
        }
    }

    private boolean checkStop() {
        RenderScript rsCtx = getRenderScriptContext();
        rsCtx.finish();
        return getEnvironment().needsStop();
    }
}
