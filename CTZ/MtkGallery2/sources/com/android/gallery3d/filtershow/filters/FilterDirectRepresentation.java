package com.android.gallery3d.filtershow.filters;

public class FilterDirectRepresentation extends FilterRepresentation {
    @Override
    public FilterRepresentation copy() {
        FilterDirectRepresentation filterDirectRepresentation = new FilterDirectRepresentation(getName());
        copyAllParameters(filterDirectRepresentation);
        return filterDirectRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    public FilterDirectRepresentation(String str) {
        super(str);
    }
}
