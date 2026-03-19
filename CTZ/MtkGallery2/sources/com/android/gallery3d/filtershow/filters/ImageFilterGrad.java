package com.android.gallery3d.filtershow.filters;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.Type;

public class ImageFilterGrad extends ImageFilterRS {
    static final boolean $assertionsDisabled = false;
    FilterGradRepresentation mParameters = new FilterGradRepresentation();
    private ScriptC_grad mScript;
    private Bitmap mSourceBitmap;

    public ImageFilterGrad() {
        this.mName = "grad";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterGradRepresentation();
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterGradRepresentation) filterRepresentation;
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
    protected void createFilter(Resources resources, float f, int i) {
        createFilter(resources, f, i, getInPixelsAllocation());
    }

    @Override
    protected void createFilter(Resources resources, float f, int i, Allocation allocation) {
        RenderScript renderScriptContext = getRenderScriptContext();
        Type.Builder builder = new Type.Builder(renderScriptContext, Element.F32_4(renderScriptContext));
        builder.setX(allocation.getType().getX());
        builder.setY(allocation.getType().getY());
        this.mScript = new ScriptC_grad(renderScriptContext);
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        if (i == 0) {
            return bitmap;
        }
        this.mSourceBitmap = bitmap;
        Bitmap bitmapApply = super.apply(bitmap, f, i);
        this.mSourceBitmap = null;
        return bitmapApply;
    }

    @Override
    protected void bindScriptValues() {
        int x = getInPixelsAllocation().getType().getX();
        int y = getInPixelsAllocation().getType().getY();
        if (this.mScript == null) {
            return;
        }
        this.mScript.set_inputWidth(x);
        this.mScript.set_inputHeight(y);
    }

    @Override
    protected void runFilter() {
        if (this.mScript == null) {
            return;
        }
        int[] xPos1 = this.mParameters.getXPos1();
        int[] yPos1 = this.mParameters.getYPos1();
        int[] xPos2 = this.mParameters.getXPos2();
        int[] yPos2 = this.mParameters.getYPos2();
        Matrix originalToScreenMatrix = getOriginalToScreenMatrix(getInPixelsAllocation().getType().getX(), getInPixelsAllocation().getType().getY());
        float[] fArr = new float[2];
        for (int i = 0; i < xPos1.length; i++) {
            fArr[0] = xPos1[i];
            fArr[1] = yPos1[i];
            originalToScreenMatrix.mapPoints(fArr);
            xPos1[i] = (int) fArr[0];
            yPos1[i] = (int) fArr[1];
            fArr[0] = xPos2[i];
            fArr[1] = yPos2[i];
            originalToScreenMatrix.mapPoints(fArr);
            xPos2[i] = (int) fArr[0];
            yPos2[i] = (int) fArr[1];
        }
        this.mScript.set_mask(this.mParameters.getMask());
        this.mScript.set_xPos1(xPos1);
        this.mScript.set_yPos1(yPos1);
        this.mScript.set_xPos2(xPos2);
        this.mScript.set_yPos2(yPos2);
        this.mScript.set_brightness(this.mParameters.getBrightness());
        this.mScript.set_contrast(this.mParameters.getContrast());
        this.mScript.set_saturation(this.mParameters.getSaturation());
        this.mScript.invoke_setupGradParams();
        runSelectiveAdjust(getInPixelsAllocation(), getOutPixelsAllocation());
    }

    @SuppressLint({"NewApi"})
    private void runSelectiveAdjust(Allocation allocation, Allocation allocation2) {
        int x = allocation.getType().getX();
        int y = allocation.getType().getY();
        Script.LaunchOptions launchOptions = new Script.LaunchOptions();
        int i = 0;
        launchOptions.setX(0, x);
        while (i < y) {
            int i2 = i + 64;
            launchOptions.setY(i, i2 > y ? y : i2);
            if (this.mScript == null) {
                return;
            }
            this.mScript.forEach_selectiveAdjust(allocation, allocation2, launchOptions);
            if (!checkStop()) {
                i = i2;
            } else {
                return;
            }
        }
    }

    private boolean checkStop() {
        getRenderScriptContext().finish();
        if (getEnvironment().needsStop()) {
            return true;
        }
        return false;
    }
}
