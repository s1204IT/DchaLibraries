package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.BasicParameterInt;
import com.android.gallery3d.filtershow.imageshow.Oval;
import java.io.IOException;

public class FilterVignetteRepresentation extends FilterRepresentation implements Oval {
    private BasicParameterInt[] mAllParam;
    private float mCenterX;
    private float mCenterY;
    private BasicParameterInt mParamContrast;
    private BasicParameterInt mParamExposure;
    private BasicParameterInt mParamFalloff;
    private BasicParameterInt mParamSaturation;
    private BasicParameterInt mParamVignette;
    private int mParameterMode;
    private float mRadiusX;
    private float mRadiusY;
    private static int MIN = -100;
    private static int MAX = 100;
    private static int MAXFALLOF = 200;

    public FilterVignetteRepresentation() {
        super("Vignette");
        this.mCenterX = 0.5f;
        this.mCenterY = 0.5f;
        this.mRadiusX = 0.5f;
        this.mRadiusY = 0.5f;
        this.mParamVignette = new BasicParameterInt(0, 50, MIN, MAX);
        this.mParamExposure = new BasicParameterInt(1, 0, MIN, MAX);
        this.mParamSaturation = new BasicParameterInt(2, 0, MIN, MAX);
        this.mParamContrast = new BasicParameterInt(3, 0, MIN, MAX);
        this.mParamFalloff = new BasicParameterInt(4, 40, 0, MAXFALLOF);
        this.mAllParam = new BasicParameterInt[]{this.mParamVignette, this.mParamExposure, this.mParamSaturation, this.mParamContrast, this.mParamFalloff};
        setSerializationName("VIGNETTE");
        setShowParameterValue(true);
        setFilterType(4);
        setTextId(R.string.vignette);
        setEditorId(R.id.vignetteEditor);
        setName("Vignette");
        setFilterClass(ImageFilterVignette.class);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        super.useParametersFrom(a);
        FilterVignetteRepresentation rep = (FilterVignetteRepresentation) a;
        this.mCenterX = rep.mCenterX;
        this.mCenterY = rep.mCenterY;
        this.mRadiusX = rep.mRadiusX;
        this.mRadiusY = rep.mRadiusY;
        this.mParamVignette.setValue(rep.mParamVignette.getValue());
        this.mParamExposure.setValue(rep.mParamExposure.getValue());
        this.mParamSaturation.setValue(rep.mParamSaturation.getValue());
        this.mParamContrast.setValue(rep.mParamContrast.getValue());
        this.mParamFalloff.setValue(rep.mParamFalloff.getValue());
    }

    public int getValue(int mode) {
        return this.mAllParam[mode].getValue();
    }

    public void setValue(int mode, int value) {
        this.mAllParam[mode].setValue(value);
    }

    @Override
    public String toString() {
        return getName() + " : " + this.mCenterX + ", " + this.mCenterY + " radius: " + this.mRadiusX;
    }

    @Override
    public FilterRepresentation copy() {
        FilterVignetteRepresentation representation = new FilterVignetteRepresentation();
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public void setCenter(float centerX, float centerY) {
        this.mCenterX = centerX;
        this.mCenterY = centerY;
    }

    @Override
    public float getCenterX() {
        return this.mCenterX;
    }

    @Override
    public float getCenterY() {
        return this.mCenterY;
    }

    @Override
    public void setRadius(float radiusX, float radiusY) {
        this.mRadiusX = radiusX;
        this.mRadiusY = radiusY;
    }

    @Override
    public void setRadiusX(float radiusX) {
        this.mRadiusX = radiusX;
    }

    @Override
    public void setRadiusY(float radiusY) {
        this.mRadiusY = radiusY;
    }

    @Override
    public float getRadiusX() {
        return this.mRadiusX;
    }

    @Override
    public float getRadiusY() {
        return this.mRadiusY;
    }

    public boolean isCenterSet() {
        return this.mCenterX != Float.NaN;
    }

    @Override
    public boolean isNil() {
        return false;
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation) || !(representation instanceof FilterVignetteRepresentation)) {
            return false;
        }
        FilterVignetteRepresentation rep = (FilterVignetteRepresentation) representation;
        for (int i = 0; i < this.mAllParam.length; i++) {
            if (this.mAllParam[i].getValue() != rep.mAllParam[i].getValue()) {
                return false;
            }
        }
        return rep.getCenterX() == getCenterX() && rep.getCenterY() == getCenterY() && rep.getRadiusX() == getRadiusX() && rep.getRadiusY() == getRadiusY();
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("ellipse");
        writer.beginArray();
        writer.value(this.mCenterX);
        writer.value(this.mCenterY);
        writer.value(this.mRadiusX);
        writer.value(this.mRadiusY);
        writer.endArray();
        writer.name("adjust");
        writer.beginArray();
        writer.value(this.mParamVignette.getValue());
        writer.value(this.mParamExposure.getValue());
        writer.value(this.mParamSaturation.getValue());
        writer.value(this.mParamContrast.getValue());
        writer.value(this.mParamFalloff.getValue());
        writer.endArray();
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader sreader) throws IOException {
        sreader.beginObject();
        while (sreader.hasNext()) {
            String name = sreader.nextName();
            if (name.startsWith("ellipse")) {
                sreader.beginArray();
                sreader.hasNext();
                this.mCenterX = (float) sreader.nextDouble();
                sreader.hasNext();
                this.mCenterY = (float) sreader.nextDouble();
                sreader.hasNext();
                this.mRadiusX = (float) sreader.nextDouble();
                sreader.hasNext();
                this.mRadiusY = (float) sreader.nextDouble();
                sreader.hasNext();
                sreader.endArray();
            } else if (name.startsWith("adjust")) {
                sreader.beginArray();
                sreader.hasNext();
                this.mParamVignette.setValue(sreader.nextInt());
                sreader.hasNext();
                this.mParamExposure.setValue(sreader.nextInt());
                sreader.hasNext();
                this.mParamSaturation.setValue(sreader.nextInt());
                sreader.hasNext();
                this.mParamContrast.setValue(sreader.nextInt());
                sreader.hasNext();
                this.mParamFalloff.setValue(sreader.nextInt());
                sreader.hasNext();
                sreader.endArray();
            } else {
                sreader.skipValue();
            }
        }
        sreader.endObject();
    }

    public int getParameterMode() {
        return this.mParameterMode;
    }

    public void setParameterMode(int parameterMode) {
        this.mParameterMode = parameterMode;
    }

    public int getCurrentParameter() {
        return getValue(this.mParameterMode);
    }

    public void setCurrentParameter(int value) {
        setValue(this.mParameterMode, value);
    }

    public BasicParameterInt getFilterParameter(int index) {
        return this.mAllParam[index];
    }
}
