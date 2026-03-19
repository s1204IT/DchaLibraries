package com.android.gallery3d.filtershow.filters;

import com.android.gallery3d.R;

public class FilterImageBorderRepresentation extends FilterRepresentation {
    private int mDrawableResource;

    public FilterImageBorderRepresentation(int i) {
        super("ImageBorder");
        this.mDrawableResource = 0;
        setFilterClass(ImageFilterBorder.class);
        this.mDrawableResource = i;
        setFilterType(1);
        setTextId(R.string.borders);
        setEditorId(R.id.imageOnlyEditor);
        setShowParameterValue(false);
    }

    @Override
    public String toString() {
        return "FilterBorder: " + getName();
    }

    @Override
    public FilterRepresentation copy() {
        FilterImageBorderRepresentation filterImageBorderRepresentation = new FilterImageBorderRepresentation(this.mDrawableResource);
        copyAllParameters(filterImageBorderRepresentation);
        return filterImageBorderRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (filterRepresentation instanceof FilterImageBorderRepresentation) {
            setName(filterRepresentation.getName());
            setDrawableResource(filterRepresentation.getDrawableResource());
        }
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        return super.equals(filterRepresentation) && (filterRepresentation instanceof FilterImageBorderRepresentation) && filterRepresentation.mDrawableResource == this.mDrawableResource;
    }

    @Override
    public int getTextId() {
        return R.string.none;
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    public int getDrawableResource() {
        return this.mDrawableResource;
    }

    public void setDrawableResource(int i) {
        this.mDrawableResource = i;
    }
}
