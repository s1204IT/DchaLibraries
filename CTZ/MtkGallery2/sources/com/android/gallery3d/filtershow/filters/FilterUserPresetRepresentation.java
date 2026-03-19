package com.android.gallery3d.filtershow.filters;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class FilterUserPresetRepresentation extends FilterRepresentation {
    private int mId;
    private ImagePreset mPreset;

    public FilterUserPresetRepresentation(String str, ImagePreset imagePreset, int i) {
        super(str);
        setEditorId(R.id.imageOnlyEditor);
        setFilterType(2);
        setSupportsPartialRendering(true);
        this.mPreset = imagePreset;
        this.mId = i;
    }

    public ImagePreset getImagePreset() {
        return this.mPreset;
    }

    public int getId() {
        return this.mId;
    }

    @Override
    public FilterRepresentation copy() {
        return new FilterUserPresetRepresentation(getName(), new ImagePreset(this.mPreset), this.mId);
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }
}
