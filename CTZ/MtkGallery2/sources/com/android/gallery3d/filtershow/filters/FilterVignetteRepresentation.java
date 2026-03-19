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
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        super.useParametersFrom(filterRepresentation);
        FilterVignetteRepresentation filterVignetteRepresentation = (FilterVignetteRepresentation) filterRepresentation;
        this.mCenterX = filterVignetteRepresentation.mCenterX;
        this.mCenterY = filterVignetteRepresentation.mCenterY;
        this.mRadiusX = filterVignetteRepresentation.mRadiusX;
        this.mRadiusY = filterVignetteRepresentation.mRadiusY;
        this.mParamVignette.setValue(filterVignetteRepresentation.mParamVignette.getValue());
        this.mParamExposure.setValue(filterVignetteRepresentation.mParamExposure.getValue());
        this.mParamSaturation.setValue(filterVignetteRepresentation.mParamSaturation.getValue());
        this.mParamContrast.setValue(filterVignetteRepresentation.mParamContrast.getValue());
        this.mParamFalloff.setValue(filterVignetteRepresentation.mParamFalloff.getValue());
    }

    public int getValue(int i) {
        return this.mAllParam[i].getValue();
    }

    public void setValue(int i, int i2) {
        this.mAllParam[i].setValue(i2);
    }

    @Override
    public String toString() {
        return getName() + " : " + this.mCenterX + ", " + this.mCenterY + " radius: " + this.mRadiusX;
    }

    @Override
    public FilterRepresentation copy() {
        FilterVignetteRepresentation filterVignetteRepresentation = new FilterVignetteRepresentation();
        copyAllParameters(filterVignetteRepresentation);
        return filterVignetteRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public void setCenter(float f, float f2) {
        this.mCenterX = f;
        this.mCenterY = f2;
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
    public void setRadius(float f, float f2) {
        this.mRadiusX = f;
        this.mRadiusY = f2;
    }

    @Override
    public void setRadiusX(float f) {
        this.mRadiusX = f;
    }

    @Override
    public void setRadiusY(float f) {
        this.mRadiusY = f;
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
    public boolean equals(FilterRepresentation filterRepresentation) {
        if (super.equals(filterRepresentation) && (filterRepresentation instanceof FilterVignetteRepresentation)) {
            for (int i = 0; i < this.mAllParam.length; i++) {
                if (this.mAllParam[i].getValue() != filterRepresentation.mAllParam[i].getValue()) {
                    return false;
                }
            }
            if (filterRepresentation.getCenterX() == getCenterX() && filterRepresentation.getCenterY() == getCenterY() && filterRepresentation.getRadiusX() == getRadiusX() && filterRepresentation.getRadiusY() == getRadiusY()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void serializeRepresentation(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("ellipse");
        jsonWriter.beginArray();
        jsonWriter.value(this.mCenterX);
        jsonWriter.value(this.mCenterY);
        jsonWriter.value(this.mRadiusX);
        jsonWriter.value(this.mRadiusY);
        jsonWriter.endArray();
        jsonWriter.name("adjust");
        jsonWriter.beginArray();
        jsonWriter.value(this.mParamVignette.getValue());
        jsonWriter.value(this.mParamExposure.getValue());
        jsonWriter.value(this.mParamSaturation.getValue());
        jsonWriter.value(this.mParamContrast.getValue());
        jsonWriter.value(this.mParamFalloff.getValue());
        jsonWriter.endArray();
        jsonWriter.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            if (strNextName.startsWith("ellipse")) {
                jsonReader.beginArray();
                jsonReader.hasNext();
                this.mCenterX = (float) jsonReader.nextDouble();
                jsonReader.hasNext();
                this.mCenterY = (float) jsonReader.nextDouble();
                jsonReader.hasNext();
                this.mRadiusX = (float) jsonReader.nextDouble();
                jsonReader.hasNext();
                this.mRadiusY = (float) jsonReader.nextDouble();
                jsonReader.hasNext();
                jsonReader.endArray();
            } else if (strNextName.startsWith("adjust")) {
                jsonReader.beginArray();
                jsonReader.hasNext();
                this.mParamVignette.setValue(jsonReader.nextInt());
                jsonReader.hasNext();
                this.mParamExposure.setValue(jsonReader.nextInt());
                jsonReader.hasNext();
                this.mParamSaturation.setValue(jsonReader.nextInt());
                jsonReader.hasNext();
                this.mParamContrast.setValue(jsonReader.nextInt());
                jsonReader.hasNext();
                this.mParamFalloff.setValue(jsonReader.nextInt());
                jsonReader.hasNext();
                jsonReader.endArray();
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
    }

    public int getParameterMode() {
        return this.mParameterMode;
    }

    public void setParameterMode(int i) {
        this.mParameterMode = i;
    }

    public int getCurrentParameter() {
        return getValue(this.mParameterMode);
    }

    public void setCurrentParameter(int i) {
        setValue(this.mParameterMode, i);
    }

    public BasicParameterInt getFilterParameter(int i) {
        return this.mAllParam[i];
    }
}
