package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.BasicParameterInt;
import com.android.gallery3d.filtershow.controller.Parameter;
import java.io.IOException;

public class FilterChanSatRepresentation extends FilterRepresentation {
    private BasicParameterInt[] mAllParam;
    private BasicParameterInt mParamBlue;
    private BasicParameterInt mParamCyan;
    private BasicParameterInt mParamGreen;
    private BasicParameterInt mParamMagenta;
    private BasicParameterInt mParamMaster;
    private BasicParameterInt mParamRed;
    private BasicParameterInt mParamYellow;
    private int mParameterMode;
    private static int MINSAT = -100;
    private static int MAXSAT = 100;

    public FilterChanSatRepresentation() {
        super("ChannelSaturation");
        this.mParameterMode = 0;
        this.mParamMaster = new BasicParameterInt(0, 0, MINSAT, MAXSAT);
        this.mParamRed = new BasicParameterInt(1, 0, MINSAT, MAXSAT);
        this.mParamYellow = new BasicParameterInt(2, 0, MINSAT, MAXSAT);
        this.mParamGreen = new BasicParameterInt(3, 0, MINSAT, MAXSAT);
        this.mParamCyan = new BasicParameterInt(4, 0, MINSAT, MAXSAT);
        this.mParamBlue = new BasicParameterInt(5, 0, MINSAT, MAXSAT);
        this.mParamMagenta = new BasicParameterInt(6, 0, MINSAT, MAXSAT);
        this.mAllParam = new BasicParameterInt[]{this.mParamMaster, this.mParamRed, this.mParamYellow, this.mParamGreen, this.mParamCyan, this.mParamBlue, this.mParamMagenta};
        setTextId(R.string.saturation);
        setFilterType(5);
        setSerializationName("channelsaturation");
        setFilterClass(ImageFilterChanSat.class);
        setEditorId(R.id.editorChanSat);
        setSupportsPartialRendering(true);
    }

    @Override
    public String toString() {
        return getName() + " : " + this.mParamRed + ", " + this.mParamCyan + ", " + this.mParamRed + ", " + this.mParamGreen + ", " + this.mParamMaster + ", " + this.mParamYellow;
    }

    @Override
    public FilterRepresentation copy() {
        FilterChanSatRepresentation filterChanSatRepresentation = new FilterChanSatRepresentation();
        copyAllParameters(filterChanSatRepresentation);
        return filterChanSatRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (filterRepresentation instanceof FilterChanSatRepresentation) {
            for (int i = 0; i < this.mAllParam.length; i++) {
                this.mAllParam[i].copyFrom(filterRepresentation.mAllParam[i]);
            }
        }
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        if (!super.equals(filterRepresentation) || !(filterRepresentation instanceof FilterChanSatRepresentation)) {
            return false;
        }
        for (int i = 0; i < this.mAllParam.length; i++) {
            if (filterRepresentation.getValue(i) != getValue(i)) {
                return false;
            }
        }
        return true;
    }

    public int getValue(int i) {
        return this.mAllParam[i].getValue();
    }

    public void setValue(int i, int i2) {
        this.mAllParam[i].setValue(i2);
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

    public Parameter getFilterParameter(int i) {
        return this.mAllParam[i];
    }

    @Override
    public void serializeRepresentation(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("ARGS");
        jsonWriter.beginArray();
        jsonWriter.value(getValue(0));
        jsonWriter.value(getValue(1));
        jsonWriter.value(getValue(2));
        jsonWriter.value(getValue(3));
        jsonWriter.value(getValue(4));
        jsonWriter.value(getValue(5));
        jsonWriter.value(getValue(6));
        jsonWriter.endArray();
        jsonWriter.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            if (jsonReader.nextName().startsWith("ARGS")) {
                jsonReader.beginArray();
                jsonReader.hasNext();
                setValue(0, jsonReader.nextInt());
                jsonReader.hasNext();
                setValue(1, jsonReader.nextInt());
                jsonReader.hasNext();
                setValue(2, jsonReader.nextInt());
                jsonReader.hasNext();
                setValue(3, jsonReader.nextInt());
                jsonReader.hasNext();
                setValue(4, jsonReader.nextInt());
                jsonReader.hasNext();
                setValue(5, jsonReader.nextInt());
                jsonReader.hasNext();
                setValue(6, jsonReader.nextInt());
                jsonReader.hasNext();
                jsonReader.endArray();
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
    }
}
