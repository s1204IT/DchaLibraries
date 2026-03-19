package com.android.gallery3d.filtershow.history;

import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class HistoryItem {
    private FilterRepresentation mFilterRepresentation;
    private ImagePreset mImagePreset;

    public HistoryItem(ImagePreset imagePreset, FilterRepresentation filterRepresentation) {
        this.mImagePreset = imagePreset;
        if (filterRepresentation != null) {
            this.mFilterRepresentation = filterRepresentation.copy();
        }
    }

    public ImagePreset getImagePreset() {
        return this.mImagePreset;
    }

    public FilterRepresentation getFilterRepresentation() {
        return this.mFilterRepresentation;
    }
}
