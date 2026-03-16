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
        FilterChanSatRepresentation representation = new FilterChanSatRepresentation();
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
        if (a instanceof FilterChanSatRepresentation) {
            FilterChanSatRepresentation representation = (FilterChanSatRepresentation) a;
            for (int i = 0; i < this.mAllParam.length; i++) {
                this.mAllParam[i].copyFrom(representation.mAllParam[i]);
            }
        }
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation) || !(representation instanceof FilterChanSatRepresentation)) {
            return false;
        }
        FilterChanSatRepresentation rep = (FilterChanSatRepresentation) representation;
        for (int i = 0; i < this.mAllParam.length; i++) {
            if (rep.getValue(i) != getValue(i)) {
                return false;
            }
        }
        return true;
    }

    public int getValue(int mode) {
        return this.mAllParam[mode].getValue();
    }

    public void setValue(int mode, int value) {
        this.mAllParam[mode].setValue(value);
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

    public Parameter getFilterParameter(int index) {
        return this.mAllParam[index];
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("ARGS");
        writer.beginArray();
        writer.value(getValue(0));
        writer.value(getValue(1));
        writer.value(getValue(2));
        writer.value(getValue(3));
        writer.value(getValue(4));
        writer.value(getValue(5));
        writer.value(getValue(6));
        writer.endArray();
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader sreader) throws IOException {
        sreader.beginObject();
        while (sreader.hasNext()) {
            String name = sreader.nextName();
            if (name.startsWith("ARGS")) {
                sreader.beginArray();
                sreader.hasNext();
                setValue(0, sreader.nextInt());
                sreader.hasNext();
                setValue(1, sreader.nextInt());
                sreader.hasNext();
                setValue(2, sreader.nextInt());
                sreader.hasNext();
                setValue(3, sreader.nextInt());
                sreader.hasNext();
                setValue(4, sreader.nextInt());
                sreader.hasNext();
                setValue(5, sreader.nextInt());
                sreader.hasNext();
                setValue(6, sreader.nextInt());
                sreader.hasNext();
                sreader.endArray();
            } else {
                sreader.skipValue();
            }
        }
        sreader.endObject();
    }
}
