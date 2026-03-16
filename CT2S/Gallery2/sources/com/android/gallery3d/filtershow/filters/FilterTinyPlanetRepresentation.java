package com.android.gallery3d.filtershow.filters;

import com.android.gallery3d.R;

public class FilterTinyPlanetRepresentation extends FilterBasicRepresentation {
    private float mAngle;

    public FilterTinyPlanetRepresentation() {
        super("TinyPlanet", 0, 50, 100);
        this.mAngle = 0.0f;
        setSerializationName("TINYPLANET");
        setShowParameterValue(true);
        setFilterClass(ImageFilterTinyPlanet.class);
        setFilterType(6);
        setTextId(R.string.tinyplanet);
        setEditorId(R.id.tinyPlanetEditor);
        setMinimum(1);
        setSupportsPartialRendering(false);
    }

    @Override
    public FilterRepresentation copy() {
        FilterTinyPlanetRepresentation representation = new FilterTinyPlanetRepresentation();
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
        FilterTinyPlanetRepresentation representation = (FilterTinyPlanetRepresentation) a;
        super.useParametersFrom(a);
        this.mAngle = representation.mAngle;
        setZoom(representation.getZoom());
    }

    public void setAngle(float angle) {
        this.mAngle = angle;
    }

    public float getAngle() {
        return this.mAngle;
    }

    public int getZoom() {
        return getValue();
    }

    public void setZoom(int zoom) {
        setValue(zoom);
    }

    @Override
    public boolean isNil() {
        return false;
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        return super.equals(representation) && this.mAngle == ((FilterTinyPlanetRepresentation) representation).mAngle;
    }

    @Override
    public String[][] serializeRepresentation() {
        String[][] ret = {new String[]{"Name", getName()}, new String[]{"Value", Integer.toString(getValue())}, new String[]{"Angle", Float.toString(this.mAngle)}};
        return ret;
    }

    @Override
    public void deSerializeRepresentation(String[][] rep) {
        super.deSerializeRepresentation(rep);
        for (int i = 0; i < rep.length; i++) {
            if ("Value".equals(rep[i][0])) {
                setValue(Integer.parseInt(rep[i][1]));
            } else if ("Angle".equals(rep[i][0])) {
                setAngle(Float.parseFloat(rep[i][1]));
            }
        }
    }
}
