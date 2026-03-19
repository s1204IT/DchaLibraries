package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManagerInterface;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import java.util.HashMap;

public class FilterEnvironment {
    private BitmapCache mBitmapCache;
    private FiltersManagerInterface mFiltersManager;
    private ImagePreset mImagePreset;
    private PipelineInterface mPipeline;
    private int mQuality;
    private float mScaleFactor;
    private volatile boolean mStop = false;
    private HashMap<Integer, Integer> generalParameters = new HashMap<>();

    public synchronized boolean needsStop() {
        return this.mStop;
    }

    public synchronized void setStop(boolean z) {
        this.mStop = z;
    }

    public void setBitmapCache(BitmapCache bitmapCache) {
        this.mBitmapCache = bitmapCache;
    }

    public void cache(Bitmap bitmap) {
        this.mBitmapCache.cache(bitmap);
    }

    public Bitmap getBitmap(int i, int i2, int i3) {
        return this.mBitmapCache.getBitmap(i, i2, i3);
    }

    public Bitmap getBitmapCopy(Bitmap bitmap, int i) {
        return this.mBitmapCache.getBitmapCopy(bitmap, i);
    }

    public void setImagePreset(ImagePreset imagePreset) {
        this.mImagePreset = imagePreset;
    }

    public ImagePreset getImagePreset() {
        return this.mImagePreset;
    }

    public void setScaleFactor(float f) {
        this.mScaleFactor = f;
    }

    public float getScaleFactor() {
        return this.mScaleFactor;
    }

    public void setQuality(int i) {
        this.mQuality = i;
    }

    public int getQuality() {
        return this.mQuality;
    }

    public void setFiltersManager(FiltersManagerInterface filtersManagerInterface) {
        this.mFiltersManager = filtersManagerInterface;
    }

    public Bitmap applyRepresentation(FilterRepresentation filterRepresentation, Bitmap bitmap) {
        if (filterRepresentation instanceof FilterUserPresetRepresentation) {
            return bitmap;
        }
        ImageFilter filterForRepresentation = this.mFiltersManager.getFilterForRepresentation(filterRepresentation);
        if (filterForRepresentation == null) {
            Log.e("FilterEnvironment", "No ImageFilter for " + filterRepresentation.getSerializationName());
        }
        filterForRepresentation.useRepresentation(filterRepresentation);
        filterForRepresentation.setEnvironment(this);
        Bitmap bitmapApply = filterForRepresentation.apply(bitmap, this.mScaleFactor, this.mQuality);
        if (bitmap != bitmapApply) {
            cache(bitmap);
        }
        filterForRepresentation.setGeneralParameters();
        filterForRepresentation.setEnvironment(null);
        return bitmapApply;
    }

    public PipelineInterface getPipeline() {
        return this.mPipeline;
    }

    public void setPipeline(PipelineInterface pipelineInterface) {
        this.mPipeline = pipelineInterface;
    }

    public synchronized void clearGeneralParameters() {
        this.generalParameters = null;
    }

    public BitmapCache getBimapCache() {
        return this.mBitmapCache;
    }
}
