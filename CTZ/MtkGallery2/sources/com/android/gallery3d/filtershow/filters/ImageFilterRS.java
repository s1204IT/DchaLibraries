package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.RSIllegalArgumentException;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import com.android.gallery3d.filtershow.pipeline.PipelineInterface;
import com.mediatek.gallery3d.util.Log;

public abstract class ImageFilterRS extends ImageFilter {
    public static boolean PERF_LOGGING = false;
    private static ScriptC_grey mGreyConvert = null;
    private static RenderScript mRScache = null;
    private boolean DEBUG = false;
    private int mLastInputWidth = 0;
    private int mLastInputHeight = 0;
    private volatile boolean mResourcesLoaded = false;

    protected abstract void bindScriptValues();

    protected abstract void createFilter(Resources resources, float f, int i);

    protected abstract void resetAllocations();

    public abstract void resetScripts();

    protected abstract void runFilter();

    protected void createFilter(Resources resources, float f, int i, Allocation allocation) {
    }

    protected void update(Bitmap bitmap) {
        getOutPixelsAllocation().copyTo(bitmap);
    }

    protected RenderScript getRenderScriptContext() {
        return getEnvironment().getPipeline().getRSContext();
    }

    protected Allocation getInPixelsAllocation() {
        return getEnvironment().getPipeline().getInPixelsAllocation();
    }

    protected Allocation getOutPixelsAllocation() {
        return getEnvironment().getPipeline().getOutPixelsAllocation();
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            return bitmap;
        }
        try {
            PipelineInterface pipeline = getEnvironment().getPipeline();
            if (this.DEBUG) {
                Log.v("ImageFilterRS", "apply filter " + getName() + " in pipeline " + pipeline.getName());
            }
            Resources resources = pipeline.getResources();
            boolean z = false;
            if (getInPixelsAllocation() != null && (getInPixelsAllocation().getType().getX() != this.mLastInputWidth || getInPixelsAllocation().getType().getY() != this.mLastInputHeight)) {
                z = true;
            }
            if (pipeline.prepareRenderscriptAllocations(bitmap) || !isResourcesLoaded() || z) {
                freeResources();
                createFilter(resources, f, i);
                setResourcesLoaded(true);
                this.mLastInputWidth = getInPixelsAllocation().getType().getX();
                this.mLastInputHeight = getInPixelsAllocation().getType().getY();
            }
            bindScriptValues();
            runFilter();
            update(bitmap);
            if (this.DEBUG) {
                Log.v("ImageFilterRS", "DONE apply filter " + getName() + " in pipeline " + pipeline.getName());
            }
        } catch (RSIllegalArgumentException e) {
            Log.e("ImageFilterRS", "Illegal argument? " + e);
        } catch (RSRuntimeException e2) {
            Log.e("ImageFilterRS", "RS runtime exception ? " + e2);
        } catch (OutOfMemoryError e3) {
            System.gc();
            displayLowMemoryToast();
            Log.e("ImageFilterRS", "not enough memory for filter " + getName(), e3);
        }
        return bitmap;
    }

    private boolean isResourcesLoaded() {
        return this.mResourcesLoaded;
    }

    private void setResourcesLoaded(boolean z) {
        this.mResourcesLoaded = z;
    }

    @Override
    public void freeResources() {
        if (!isResourcesLoaded()) {
            return;
        }
        resetAllocations();
        this.mLastInputWidth = 0;
        this.mLastInputHeight = 0;
        setResourcesLoaded(false);
    }
}
