package com.android.gallery3d.filtershow.filters;

public class SimpleImageFilter extends ImageFilter {
    private FilterBasicRepresentation mParameters;

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterRepresentation representation = new FilterBasicRepresentation("Default", 0, 50, 100);
        representation.setShowParameterValue(true);
        return representation;
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        FilterBasicRepresentation parameters = (FilterBasicRepresentation) representation;
        this.mParameters = parameters;
    }

    public FilterBasicRepresentation getParameters() {
        return this.mParameters;
    }
}
