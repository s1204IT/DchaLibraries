package com.android.gallery3d.filtershow.filters;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class FilterUserPresetRepresentation extends FilterRepresentation {
    private int mId;
    private ImagePreset mPreset;

    public FilterUserPresetRepresentation(String name, ImagePreset preset, int id) {
        super(name);
        setEditorId(R.id.imageOnlyEditor);
        setFilterType(2);
        setSupportsPartialRendering(true);
        this.mPreset = preset;
        this.mId = id;
    }

    public ImagePreset getImagePreset() {
        return this.mPreset;
    }

    public int getId() {
        return this.mId;
    }

    @Override
    public FilterRepresentation copy() {
        FilterRepresentation representation = new FilterUserPresetRepresentation(getName(), new ImagePreset(this.mPreset), this.mId);
        return representation;
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }
}
