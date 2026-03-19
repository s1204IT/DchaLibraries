package com.android.gallery3d.filtershow.filters;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.Type;

public class ImageFilterChanSat extends ImageFilterRS {
    static final boolean $assertionsDisabled = false;
    FilterChanSatRepresentation mParameters = new FilterChanSatRepresentation();
    private ScriptC_saturation mScript;
    private Bitmap mSourceBitmap;

    public ImageFilterChanSat() {
        this.mName = "ChannelSat";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterChanSatRepresentation();
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterChanSatRepresentation) filterRepresentation;
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
        this.mScript = new ScriptC_saturation(renderScriptContext);
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
        getInPixelsAllocation().getType().getX();
        getInPixelsAllocation().getType().getY();
    }

    @Override
    protected void runFilter() {
        if (this.mScript == null) {
            return;
        }
        int[] iArr = new int[7];
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = this.mParameters.getValue(i);
        }
        getOriginalToScreenMatrix(getInPixelsAllocation().getType().getX(), getInPixelsAllocation().getType().getY());
        this.mScript.set_saturation(iArr);
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
