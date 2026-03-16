package com.android.gallery3d.filtershow.filters;

import com.android.gallery3d.R;

public class FilterImageBorderRepresentation extends FilterRepresentation {
    private int mDrawableResource;

    public FilterImageBorderRepresentation(int resource, int drawableResource) {
        super("ImageBorder");
        this.mDrawableResource = 0;
        setFilterClass(ImageFilterBorder.class);
        this.mDrawableResource = drawableResource;
        setFilterType(1);
        setTextId(resource);
        setEditorId(R.id.imageOnlyEditor);
        setShowParameterValue(false);
    }

    @Override
    public String toString() {
        return "FilterBorder: " + getName();
    }

    @Override
    public FilterRepresentation copy() {
        FilterImageBorderRepresentation representation = new FilterImageBorderRepresentation(getTextId(), this.mDrawableResource);
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (a instanceof FilterImageBorderRepresentation) {
            FilterImageBorderRepresentation representation = (FilterImageBorderRepresentation) a;
            setName(representation.getName());
            setDrawableResource(representation.getDrawableResource());
        }
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation) || !(representation instanceof FilterImageBorderRepresentation)) {
            return false;
        }
        FilterImageBorderRepresentation border = (FilterImageBorderRepresentation) representation;
        return border.mDrawableResource == this.mDrawableResource;
    }

    @Override
    public int getTextId() {
        return super.getTextId() == 0 ? R.string.borders : super.getTextId();
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    public int getDrawableResource() {
        return this.mDrawableResource;
    }

    public void setDrawableResource(int drawableResource) {
        this.mDrawableResource = drawableResource;
    }
}
