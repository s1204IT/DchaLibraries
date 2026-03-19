package com.android.gallery3d.filtershow.filters;

import com.android.gallery3d.R;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

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
        FilterTinyPlanetRepresentation filterTinyPlanetRepresentation = new FilterTinyPlanetRepresentation();
        copyAllParameters(filterTinyPlanetRepresentation);
        return filterTinyPlanetRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        FilterTinyPlanetRepresentation filterTinyPlanetRepresentation = (FilterTinyPlanetRepresentation) filterRepresentation;
        super.useParametersFrom(filterRepresentation);
        this.mAngle = filterTinyPlanetRepresentation.mAngle;
        setZoom(filterTinyPlanetRepresentation.getZoom());
    }

    public void setAngle(float f) {
        this.mAngle = f;
    }

    public float getAngle() {
        return this.mAngle;
    }

    public int getZoom() {
        return getValue();
    }

    public void setZoom(int i) {
        setValue(i);
    }

    @Override
    public boolean isNil() {
        return false;
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        return super.equals(filterRepresentation) && this.mAngle == ((FilterTinyPlanetRepresentation) filterRepresentation).mAngle;
    }

    @Override
    public String[][] serializeRepresentation() {
        return new String[][]{new String[]{SchemaSymbols.ATTVAL_NAME, getName()}, new String[]{"Value", Integer.toString(getValue())}, new String[]{"Angle", Float.toString(this.mAngle)}};
    }

    @Override
    public void deSerializeRepresentation(String[][] strArr) {
        super.deSerializeRepresentation(strArr);
        for (int i = 0; i < strArr.length; i++) {
            if ("Value".equals(strArr[i][0])) {
                setValue(Integer.parseInt(strArr[i][1]));
            } else if ("Angle".equals(strArr[i][0])) {
                setAngle(Float.parseFloat(strArr[i][1]));
            }
        }
    }
}
