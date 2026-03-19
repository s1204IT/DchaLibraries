package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
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
        return new FilterVignetteRepresentation();
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterVignetteRepresentation) filterRepresentation;
    }

    private float calcRadius(float f, float f2, int i, int i2) {
        float f3 = i - f;
        if (f < f3) {
            f = f3;
        }
        if (f < f2) {
            f = f2;
        }
        float f4 = i2 - f2;
        if (f < f4) {
            f = f4;
        }
        return f * f * 2.0f;
    }

    @Override
    protected void createFilter(Resources resources, float f, int i) {
        this.mScript = new ScriptC_vignette(getRenderScriptContext());
    }

    @Override
    protected void runFilter() {
        float f;
        float radiusY;
        float f2;
        float radiusX;
        int x = getInPixelsAllocation().getType().getX();
        int y = getInPixelsAllocation().getType().getY();
        float f3 = x / 2;
        float f4 = y / 2;
        float fCalcRadius = calcRadius(f3, f4, x, y);
        float[] fArr = new float[2];
        if (this.mParameters.isCenterSet()) {
            Matrix originalToScreenMatrix = getOriginalToScreenMatrix(x, y);
            Rect originalBounds = MasterImage.getImage().getOriginalBounds();
            fArr[0] = originalBounds.right * this.mParameters.getCenterX();
            fArr[1] = originalBounds.bottom * this.mParameters.getCenterY();
            originalToScreenMatrix.mapPoints(fArr);
            f2 = fArr[0];
            f = fArr[1];
            fArr[0] = originalBounds.right * this.mParameters.getRadiusX();
            fArr[1] = originalBounds.bottom * this.mParameters.getRadiusY();
            originalToScreenMatrix.mapVectors(fArr);
            radiusX = fArr[0];
            radiusY = fArr[1];
            if (fArr[0] == 0.0f) {
                radiusX = this.mParameters.getRadiusX() * fArr[1];
                radiusY = this.mParameters.getRadiusY() * fArr[1];
            }
            if (fArr[1] == 0.0f) {
                radiusX = this.mParameters.getRadiusX() * fArr[0];
                radiusY = this.mParameters.getRadiusY() * fArr[0];
            }
        } else {
            f = f4;
            radiusY = fCalcRadius;
            f2 = f3;
            radiusX = radiusY;
        }
        this.mScript.set_inputWidth(x);
        this.mScript.set_inputHeight(y);
        int value = this.mParameters.getValue(0);
        this.mScript.set_finalSubtract(value < 0 ? value : 0.0f);
        this.mScript.set_finalBright(value > 0 ? -value : 0.0f);
        this.mScript.set_finalSaturation(this.mParameters.getValue(2));
        this.mScript.set_finalContrast(this.mParameters.getValue(3));
        this.mScript.set_centerx(f2);
        this.mScript.set_centery(f);
        this.mScript.set_radiusx(radiusX);
        this.mScript.set_radiusy(radiusY);
        this.mScript.set_strength(this.mParameters.getValue(4) / 10.0f);
        this.mScript.invoke_setupVignetteParams();
        this.mScript.forEach_vignette(getInPixelsAllocation(), getOutPixelsAllocation());
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        if (i == 0) {
            if (this.mOverlayBitmap == null) {
                this.mOverlayBitmap = IconUtilities.getFXBitmap(getEnvironment().getPipeline().getResources(), R.drawable.filtershow_icon_vignette);
            }
            Canvas canvas = new Canvas(bitmap);
            int iMax = Math.max(bitmap.getWidth(), bitmap.getHeight());
            canvas.drawBitmap(this.mOverlayBitmap, (Rect) null, new Rect(0, 0, iMax, iMax), (Paint) null);
            return bitmap;
        }
        super.apply(bitmap, f, i);
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
        int x = getInPixelsAllocation().getType().getX();
        int y = getInPixelsAllocation().getType().getY();
        this.mScript.set_inputWidth(x);
        this.mScript.set_inputHeight(y);
    }
}
