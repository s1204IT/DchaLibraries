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
        FilterRepresentation representation = new FilterBasicRepresentation("Sharpen", 0, 0, 100);
        representation.setSerializationName("SHARPEN");
        representation.setShowParameterValue(true);
        representation.setFilterClass(ImageFilterSharpen.class);
        representation.setTextId(R.string.sharpness);
        representation.setOverlayId(R.drawable.filtershow_button_colors_sharpen);
        representation.setEditorId(BasicEditor.ID);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        FilterBasicRepresentation parameters = (FilterBasicRepresentation) representation;
        this.mParameters = parameters;
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
        if (this.mScript == null) {
            this.mScript = new ScriptC_convolve3x3(getRenderScriptContext(), res, R.raw.convolve3x3);
        }
    }

    private void computeKernel() {
        float scaleFactor = getEnvironment().getScaleFactor();
        float p1 = this.mParameters.getValue() * scaleFactor;
        float value = p1 / 100.0f;
        float[] f = {-value, -value, -value, -value, (8.0f * value) + 1.0f, -value, -value, -value, -value};
        this.mScript.set_gCoeffs(f);
    }

    @Override
    protected void bindScriptValues() {
        int w = getInPixelsAllocation().getType().getX();
        int h = getInPixelsAllocation().getType().getY();
        this.mScript.set_gWidth(w);
        this.mScript.set_gHeight(h);
    }

    @Override
    protected void runFilter() {
        if (this.mParameters != null) {
            computeKernel();
            this.mScript.set_gIn(getInPixelsAllocation());
            this.mScript.bind_gPixels(getInPixelsAllocation());
            this.mScript.forEach_root(getInPixelsAllocation(), getOutPixelsAllocation());
        }
    }
}
