package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.BasicEditor;

public class ImageFilterSharpen extends ImageFilterRS {
    private FilterBasicRepresentation mParameters;
    private ScriptC_convolve3x3 mScript;

    public ImageFilterSharpen() {
        this.mName = "Sharpen";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation filterBasicRepresentation = new FilterBasicRepresentation("Sharpen", -100, 0, 100);
        filterBasicRepresentation.setSerializationName("SHARPEN");
        filterBasicRepresentation.setShowParameterValue(true);
        filterBasicRepresentation.setFilterClass(ImageFilterSharpen.class);
        filterBasicRepresentation.setTextId(R.string.sharpness);
        filterBasicRepresentation.setOverlayId(R.drawable.filtershow_button_colors_sharpen);
        filterBasicRepresentation.setEditorId(BasicEditor.ID);
        filterBasicRepresentation.setSupportsPartialRendering(true);
        return filterBasicRepresentation;
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterBasicRepresentation) filterRepresentation;
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
        if (this.mScript == null) {
            this.mScript = new ScriptC_convolve3x3(getRenderScriptContext());
        }
    }

    private void computeKernel() {
        float value = (this.mParameters.getValue() * getEnvironment().getScaleFactor()) / 100.0f;
        float f = -value;
        this.mScript.set_gCoeffs(new float[]{f, f, f, f, (8.0f * value) + 1.0f, f, f, f, f});
    }

    @Override
    protected void bindScriptValues() {
        if (this.mScript == null) {
            return;
        }
        int x = getInPixelsAllocation().getType().getX();
        int y = getInPixelsAllocation().getType().getY();
        this.mScript.set_gWidth(x);
        this.mScript.set_gHeight(y);
    }

    @Override
    protected void runFilter() {
        if (this.mParameters == null || this.mScript == null) {
            return;
        }
        computeKernel();
        this.mScript.set_gIn(getInPixelsAllocation());
        this.mScript.bind_gPixels(getInPixelsAllocation());
        this.mScript.forEach_root(getInPixelsAllocation(), getOutPixelsAllocation());
    }
}
