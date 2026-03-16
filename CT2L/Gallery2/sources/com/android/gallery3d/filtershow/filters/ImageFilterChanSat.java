package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Script;
import android.support.v8.renderscript.Type;
import com.android.gallery3d.R;

public class ImageFilterChanSat extends ImageFilterRS {
    static final boolean $assertionsDisabled;
    FilterChanSatRepresentation mParameters = new FilterChanSatRepresentation();
    private ScriptC_saturation mScript;
    private Bitmap mSourceBitmap;

    static {
        $assertionsDisabled = !ImageFilterChanSat.class.desiredAssertionStatus();
    }

    public ImageFilterChanSat() {
        this.mName = "ChannelSat";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterChanSatRepresentation();
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        this.mParameters = (FilterChanSatRepresentation) representation;
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
        this.mScript = new ScriptC_saturation(rsCtx, res, R.raw.saturation);
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
        getInPixelsAllocation().getType().getX();
        getInPixelsAllocation().getType().getY();
    }

    @Override
    protected void runFilter() {
        int[] sat = new int[7];
        for (int i = 0; i < sat.length; i++) {
            sat[i] = this.mParameters.getValue(i);
        }
        int width = getInPixelsAllocation().getType().getX();
        int height = getInPixelsAllocation().getType().getY();
        getOriginalToScreenMatrix(width, height);
        this.mScript.set_saturation(sat);
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
