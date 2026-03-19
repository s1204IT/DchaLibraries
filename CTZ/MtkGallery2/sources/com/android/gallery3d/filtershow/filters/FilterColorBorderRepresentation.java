package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.BasicParameterInt;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.controller.ParameterColor;
import java.io.IOException;

public class FilterColorBorderRepresentation extends FilterRepresentation {
    public static int DEFAULT_MENU_COLOR1 = -1;
    public static int DEFAULT_MENU_COLOR2 = -16777216;
    public static int DEFAULT_MENU_COLOR3 = -7829368;
    public static int DEFAULT_MENU_COLOR4 = -13142;
    public static int DEFAULT_MENU_COLOR5 = -5592406;
    private Parameter[] mAllParam;
    private int mInitColor;
    private int mInitRadius;
    private int mInitSize;
    private ParameterColor mParamColor;
    private BasicParameterInt mParamRadius;
    private BasicParameterInt mParamSize;
    private int mPramMode;

    public FilterColorBorderRepresentation(int i, int i2, int i3) {
        super("ColorBorder");
        this.mParamSize = new BasicParameterInt(0, 3, 2, 30);
        this.mParamRadius = new BasicParameterInt(1, 2, 0, 100);
        this.mParamColor = new ParameterColor(2, DEFAULT_MENU_COLOR1);
        this.mAllParam = new Parameter[]{this.mParamSize, this.mParamRadius, this.mParamColor};
        setSerializationName("COLORBORDER");
        setFilterType(1);
        setTextId(R.string.borders);
        setEditorId(R.id.editorColorBorder);
        setShowParameterValue(false);
        setFilterClass(ImageFilterColorBorder.class);
        this.mParamColor.setValue(i);
        this.mParamSize.setValue(i2);
        this.mParamRadius.setValue(i3);
        this.mParamColor.setColorpalette(new int[]{DEFAULT_MENU_COLOR1, DEFAULT_MENU_COLOR2, DEFAULT_MENU_COLOR3, DEFAULT_MENU_COLOR4, DEFAULT_MENU_COLOR5});
        this.mInitColor = i;
        this.mInitSize = i2;
        this.mInitRadius = i3;
    }

    @Override
    public String toString() {
        return "FilterBorder: " + getName();
    }

    @Override
    public FilterRepresentation copy() {
        FilterColorBorderRepresentation filterColorBorderRepresentation = new FilterColorBorderRepresentation(0, 0, 0);
        copyAllParameters(filterColorBorderRepresentation);
        filterColorBorderRepresentation.mInitColor = this.mInitColor;
        filterColorBorderRepresentation.mInitSize = this.mInitSize;
        filterColorBorderRepresentation.mInitRadius = this.mInitRadius;
        return filterColorBorderRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (filterRepresentation instanceof FilterColorBorderRepresentation) {
            setName(filterRepresentation.getName());
            setColor(filterRepresentation.getColor());
            this.mParamColor.copyPalletFrom(filterRepresentation.mParamColor);
            setBorderSize(filterRepresentation.getBorderSize());
            setBorderRadius(filterRepresentation.getBorderRadius());
        }
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        return super.equals(filterRepresentation) && (filterRepresentation instanceof FilterColorBorderRepresentation) && filterRepresentation.mParamColor.getValue() == this.mParamColor.getValue() && filterRepresentation.mParamRadius.getValue() == this.mParamRadius.getValue() && filterRepresentation.mParamSize.getValue() == this.mParamSize.getValue();
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    public Parameter getParam(int i) {
        return this.mAllParam[i];
    }

    @Override
    public int getTextId() {
        if (super.getTextId() == 0) {
            return R.string.borders;
        }
        return super.getTextId();
    }

    public int getColor() {
        return this.mParamColor.getValue();
    }

    public void setColor(int i) {
        this.mParamColor.setValue(i);
    }

    public int getBorderSize() {
        return this.mParamSize.getValue();
    }

    public void setBorderSize(int i) {
        this.mParamSize.setValue(i);
    }

    public int getBorderRadius() {
        return this.mParamRadius.getValue();
    }

    public void setBorderRadius(int i) {
        this.mParamRadius.setValue(i);
    }

    public void setPramMode(int i) {
        this.mPramMode = i;
    }

    public Parameter getCurrentParam() {
        return this.mAllParam[this.mPramMode];
    }

    public String getValueString() {
        return "";
    }

    @Override
    public void serializeRepresentation(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("size");
        jsonWriter.value(this.mParamSize.getValue());
        jsonWriter.name("radius");
        jsonWriter.value(this.mParamRadius.getValue());
        jsonWriter.name("color");
        jsonWriter.value(this.mParamColor.getValue());
        jsonWriter.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            if (strNextName.equalsIgnoreCase("size")) {
                this.mParamSize.setValue(jsonReader.nextInt());
            } else if (strNextName.equalsIgnoreCase("radius")) {
                this.mParamRadius.setValue(jsonReader.nextInt());
            } else if (strNextName.equalsIgnoreCase("color")) {
                this.mParamColor.setValue(jsonReader.nextInt());
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
    }

    public void clearToDefault() {
        this.mParamSize.setValue(this.mInitSize);
        this.mParamRadius.setValue(this.mInitRadius);
        this.mParamColor.setValue(this.mInitColor);
        this.mParamColor.setColorpalette(new int[]{DEFAULT_MENU_COLOR1, DEFAULT_MENU_COLOR2, DEFAULT_MENU_COLOR3, DEFAULT_MENU_COLOR4, DEFAULT_MENU_COLOR5});
    }
}
