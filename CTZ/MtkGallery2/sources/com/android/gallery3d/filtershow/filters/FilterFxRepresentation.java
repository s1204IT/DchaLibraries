package com.android.gallery3d.filtershow.filters;

import com.android.gallery3d.R;

public class FilterFxRepresentation extends FilterRepresentation {
    private int mBitmapResource;
    private int mNameResource;

    public FilterFxRepresentation(String str, int i, int i2) {
        super(str);
        this.mBitmapResource = 0;
        this.mNameResource = 0;
        setFilterClass(ImageFilterFx.class);
        this.mBitmapResource = i;
        this.mNameResource = i2;
        setFilterType(2);
        setTextId(i2);
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
        FilterFxRepresentation filterFxRepresentation = new FilterFxRepresentation(getName(), 0, 0);
        copyAllParameters(filterFxRepresentation);
        return filterFxRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public synchronized void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (filterRepresentation instanceof FilterFxRepresentation) {
            setName(filterRepresentation.getName());
            setSerializationName(filterRepresentation.getSerializationName());
            setBitmapResource(filterRepresentation.getBitmapResource());
            setNameResource(filterRepresentation.getNameResource());
        }
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        return super.equals(filterRepresentation) && (filterRepresentation instanceof FilterFxRepresentation) && filterRepresentation.mNameResource == this.mNameResource && filterRepresentation.mBitmapResource == this.mBitmapResource;
    }

    @Override
    public boolean same(FilterRepresentation filterRepresentation) {
        if (!super.same(filterRepresentation)) {
            return false;
        }
        return equals(filterRepresentation);
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    public int getNameResource() {
        return this.mNameResource;
    }

    public void setNameResource(int i) {
        this.mNameResource = i;
    }

    public int getBitmapResource() {
        return this.mBitmapResource;
    }

    public void setBitmapResource(int i) {
        this.mBitmapResource = i;
    }
}
