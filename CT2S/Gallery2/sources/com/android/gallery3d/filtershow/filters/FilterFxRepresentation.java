package com.android.gallery3d.filtershow.filters;

import com.android.gallery3d.R;

public class FilterFxRepresentation extends FilterRepresentation {
    private int mBitmapResource;
    private int mNameResource;

    public FilterFxRepresentation(String name, int bitmapResource, int nameResource) {
        super(name);
        this.mBitmapResource = 0;
        this.mNameResource = 0;
        setFilterClass(ImageFilterFx.class);
        this.mBitmapResource = bitmapResource;
        this.mNameResource = nameResource;
        setFilterType(2);
        setTextId(nameResource);
        setEditorId(R.id.imageOnlyEditor);
        setShowParameterValue(false);
        setSupportsPartialRendering(true);
    }

    @Override
    public String toString() {
        return "FilterFx: " + hashCode() + " : " + getName() + " bitmap rsc: " + this.mBitmapResource;
    }

    @Override
    public FilterRepresentation copy() {
        FilterFxRepresentation representation = new FilterFxRepresentation(getName(), 0, 0);
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public synchronized void useParametersFrom(FilterRepresentation a) {
        if (a instanceof FilterFxRepresentation) {
            FilterFxRepresentation representation = (FilterFxRepresentation) a;
            setName(representation.getName());
            setSerializationName(representation.getSerializationName());
            setBitmapResource(representation.getBitmapResource());
            setNameResource(representation.getNameResource());
        }
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation) || !(representation instanceof FilterFxRepresentation)) {
            return false;
        }
        FilterFxRepresentation fx = (FilterFxRepresentation) representation;
        return fx.mNameResource == this.mNameResource && fx.mBitmapResource == this.mBitmapResource;
    }

    @Override
    public boolean same(FilterRepresentation representation) {
        if (super.same(representation)) {
            return equals(representation);
        }
        return false;
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    public int getNameResource() {
        return this.mNameResource;
    }

    public void setNameResource(int nameResource) {
        this.mNameResource = nameResource;
    }

    public int getBitmapResource() {
        return this.mBitmapResource;
    }

    public void setBitmapResource(int bitmapResource) {
        this.mBitmapResource = bitmapResource;
    }
}
