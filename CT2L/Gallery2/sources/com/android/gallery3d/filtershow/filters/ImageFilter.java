package com.android.gallery3d.filtershow.filters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.widget.Toast;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.FilterEnvironment;

public abstract class ImageFilter implements Cloneable {
    private static Activity sActivity = null;
    private FilterEnvironment mEnvironment = null;
    protected String mName = "Original";
    private final String LOGTAG = "ImageFilter";

    protected native void nativeApplyGradientFilter(Bitmap bitmap, int i, int i2, int[] iArr, int[] iArr2, int[] iArr3);

    public abstract void useRepresentation(FilterRepresentation filterRepresentation);

    public static void setActivityForMemoryToasts(Activity activity) {
        sActivity = activity;
    }

    public static void resetStatics() {
        sActivity = null;
    }

    public void freeResources() {
    }

    public void displayLowMemoryToast() {
        if (sActivity != null) {
            sActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ImageFilter.sActivity, "Memory too low for filter " + ImageFilter.this.getName() + ", please file a bug report", 0).show();
                }
            });
        }
    }

    public String getName() {
        return this.mName;
    }

    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        setGeneralParameters();
        return bitmap;
    }

    public FilterRepresentation getDefaultRepresentation() {
        return null;
    }

    protected Matrix getOriginalToScreenMatrix(int w, int h) {
        return GeometryMathUtils.getImageToScreenMatrix(getEnvironment().getImagePreset().getGeometryFilters(), true, MasterImage.getImage().getOriginalBounds(), w, h);
    }

    public void setEnvironment(FilterEnvironment environment) {
        this.mEnvironment = environment;
    }

    public FilterEnvironment getEnvironment() {
        return this.mEnvironment;
    }

    public void setGeneralParameters() {
        this.mEnvironment.clearGeneralParameters();
    }
}
