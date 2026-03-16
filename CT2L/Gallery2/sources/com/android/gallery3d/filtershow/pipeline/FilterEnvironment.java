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

    public synchronized void setStop(boolean stop) {
        this.mStop = stop;
    }

    public void setBitmapCache(BitmapCache cache) {
        this.mBitmapCache = cache;
    }

    public void cache(Bitmap bitmap) {
        this.mBitmapCache.cache(bitmap);
    }

    public Bitmap getBitmap(int w, int h, int type) {
        return this.mBitmapCache.getBitmap(w, h, type);
    }

    public Bitmap getBitmapCopy(Bitmap source, int type) {
        return this.mBitmapCache.getBitmapCopy(source, type);
    }

    public void setImagePreset(ImagePreset imagePreset) {
        this.mImagePreset = imagePreset;
    }

    public ImagePreset getImagePreset() {
        return this.mImagePreset;
    }

    public void setScaleFactor(float scaleFactor) {
        this.mScaleFactor = scaleFactor;
    }

    public float getScaleFactor() {
        return this.mScaleFactor;
    }

    public void setQuality(int quality) {
        this.mQuality = quality;
    }

    public int getQuality() {
        return this.mQuality;
    }

    public void setFiltersManager(FiltersManagerInterface filtersManager) {
        this.mFiltersManager = filtersManager;
    }

    public Bitmap applyRepresentation(FilterRepresentation representation, Bitmap bitmap) {
        if (!(representation instanceof FilterUserPresetRepresentation)) {
            ImageFilter filter = this.mFiltersManager.getFilterForRepresentation(representation);
            if (filter == null) {
                Log.e("FilterEnvironment", "No ImageFilter for " + representation.getSerializationName());
            }
            filter.useRepresentation(representation);
            filter.setEnvironment(this);
            Bitmap ret = filter.apply(bitmap, this.mScaleFactor, this.mQuality);
            if (bitmap != ret) {
                cache(bitmap);
            }
            filter.setGeneralParameters();
            filter.setEnvironment(null);
            return ret;
        }
        return bitmap;
    }

    public PipelineInterface getPipeline() {
        return this.mPipeline;
    }

    public void setPipeline(PipelineInterface cachingPipeline) {
        this.mPipeline = cachingPipeline;
    }

    public synchronized void clearGeneralParameters() {
        this.generalParameters = null;
    }

    public BitmapCache getBimapCache() {
        return this.mBitmapCache;
    }
}
