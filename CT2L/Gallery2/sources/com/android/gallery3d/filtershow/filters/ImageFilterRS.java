package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.renderscript.RSIllegalArgumentException;
import android.renderscript.RSRuntimeException;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;
import com.android.gallery3d.filtershow.pipeline.PipelineInterface;

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

    protected void createFilter(Resources res, float scaleFactor, int quality, Allocation in) {
    }

    protected void update(Bitmap bitmap) {
        getOutPixelsAllocation().copyTo(bitmap);
    }

    protected RenderScript getRenderScriptContext() {
        PipelineInterface pipeline = getEnvironment().getPipeline();
        return pipeline.getRSContext();
    }

    protected Allocation getInPixelsAllocation() {
        PipelineInterface pipeline = getEnvironment().getPipeline();
        return pipeline.getInPixelsAllocation();
    }

    protected Allocation getOutPixelsAllocation() {
        PipelineInterface pipeline = getEnvironment().getPipeline();
        return pipeline.getOutPixelsAllocation();
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (bitmap != null && bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
            try {
                PipelineInterface pipeline = getEnvironment().getPipeline();
                if (this.DEBUG) {
                    Log.v("ImageFilterRS", "apply filter " + getName() + " in pipeline " + pipeline.getName());
                }
                Resources rsc = pipeline.getResources();
                boolean sizeChanged = false;
                if (getInPixelsAllocation() != null && (getInPixelsAllocation().getType().getX() != this.mLastInputWidth || getInPixelsAllocation().getType().getY() != this.mLastInputHeight)) {
                    sizeChanged = true;
                }
                if (pipeline.prepareRenderscriptAllocations(bitmap) || !isResourcesLoaded() || sizeChanged) {
                    freeResources();
                    createFilter(rsc, scaleFactor, quality);
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
        }
        return bitmap;
    }

    private boolean isResourcesLoaded() {
        return this.mResourcesLoaded;
    }

    private void setResourcesLoaded(boolean resourcesLoaded) {
        this.mResourcesLoaded = resourcesLoaded;
    }

    @Override
    public void freeResources() {
        if (isResourcesLoaded()) {
            resetAllocations();
            this.mLastInputWidth = 0;
            this.mLastInputHeight = 0;
            setResourcesLoaded(false);
        }
    }
}
