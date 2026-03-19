package com.android.gallery3d.filtershow.filters;

public class SimpleImageFilter extends ImageFilter {
    private FilterBasicRepresentation mParameters;

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation filterBasicRepresentation = new FilterBasicRepresentation("Default", 0, 50, 100);
        filterBasicRepresentation.setShowParameterValue(true);
        return filterBasicRepresentation;
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterBasicRepresentation) filterRepresentation;
    }

    public FilterBasicRepresentation getParameters() {
        return this.mParameters;
    }
}
