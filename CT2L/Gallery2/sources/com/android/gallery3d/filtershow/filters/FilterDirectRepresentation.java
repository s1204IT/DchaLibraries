package com.android.gallery3d.filtershow.filters;

public class FilterDirectRepresentation extends FilterRepresentation {
    @Override
    public FilterRepresentation copy() {
        FilterDirectRepresentation representation = new FilterDirectRepresentation(getName());
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    public FilterDirectRepresentation(String name) {
        super(name);
    }
}
