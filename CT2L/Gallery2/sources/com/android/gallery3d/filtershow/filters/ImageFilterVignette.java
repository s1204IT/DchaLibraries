package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v8.renderscript.RenderScript;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class ImageFilterVignette extends ImageFilterRS {
    private Bitmap mOverlayBitmap;
    FilterVignetteRepresentation mParameters;
    private ScriptC_vignette mScript;

    public ImageFilterVignette() {
        this.mName = "Vignette";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterVignetteRepresentation representation = new FilterVignetteRepresentation();
        return representation;
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        this.mParameters = (FilterVignetteRepresentation) representation;
    }

    private float calcRadius(float cx, float cy, int w, int h) {
        float d = cx;
        if (d < w - cx) {
            d = w - cx;
        }
        if (d < cy) {
            d = cy;
        }
        if (d < h - cy) {
            d = h - cy;
        }
        return d * d * 2.0f;
    }

    @Override
    protected void createFilter(Resources res, float scaleFactor, int quality) {
        RenderScript rsCtx = getRenderScriptContext();
        this.mScript = new ScriptC_vignette(rsCtx, res, R.raw.vignette);
    }

    @Override
    protected void runFilter() {
        int w = getInPixelsAllocation().getType().getX();
        int h = getInPixelsAllocation().getType().getY();
        float cx = w / 2;
        float cy = h / 2;
        float r = calcRadius(cx, cy, w, h);
        float rx = r;
        float ry = r;
        float[] c = new float[2];
        if (this.mParameters.isCenterSet()) {
            Matrix m = getOriginalToScreenMatrix(w, h);
            Rect bounds = MasterImage.getImage().getOriginalBounds();
            c[0] = bounds.right * this.mParameters.getCenterX();
            c[1] = bounds.bottom * this.mParameters.getCenterY();
            m.mapPoints(c);
            cx = c[0];
            cy = c[1];
            c[0] = bounds.right * this.mParameters.getRadiusX();
            c[1] = bounds.bottom * this.mParameters.getRadiusY();
            m.mapVectors(c);
            rx = c[0];
            ry = c[1];
        }
        this.mScript.set_inputWidth(w);
        this.mScript.set_inputHeight(h);
        int v = this.mParameters.getValue(0);
        this.mScript.set_finalSubtract(v < 0 ? v : 0.0f);
        this.mScript.set_finalBright(v > 0 ? -v : 0.0f);
        this.mScript.set_finalSaturation(this.mParameters.getValue(2));
        this.mScript.set_finalContrast(this.mParameters.getValue(3));
        this.mScript.set_centerx(cx);
        this.mScript.set_centery(cy);
        this.mScript.set_radiusx(rx);
        this.mScript.set_radiusy(ry);
        this.mScript.set_strength(this.mParameters.getValue(4) / 10.0f);
        this.mScript.invoke_setupVignetteParams();
        this.mScript.forEach_vignette(getInPixelsAllocation(), getOutPixelsAllocation());
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (quality == 0) {
            if (this.mOverlayBitmap == null) {
                Resources res = getEnvironment().getPipeline().getResources();
                this.mOverlayBitmap = IconUtilities.getFXBitmap(res, R.drawable.filtershow_icon_vignette);
            }
            Canvas c = new Canvas(bitmap);
            int dim = Math.max(bitmap.getWidth(), bitmap.getHeight());
            Rect r = new Rect(0, 0, dim, dim);
            c.drawBitmap(this.mOverlayBitmap, (Rect) null, r, (Paint) null);
        } else {
            super.apply(bitmap, scaleFactor, quality);
        }
        return bitmap;
    }

    @Override
    protected void resetAllocations() {
    }

    @Override
    public void resetScripts() {
    }

    @Override
    protected void bindScriptValues() {
        int width = getInPixelsAllocation().getType().getX();
        int height = getInPixelsAllocation().getType().getY();
        this.mScript.set_inputWidth(width);
        this.mScript.set_inputHeight(height);
    }
}
