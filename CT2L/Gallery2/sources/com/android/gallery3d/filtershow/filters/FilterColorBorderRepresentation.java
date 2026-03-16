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
    private ParameterColor mParamColor;
    private BasicParameterInt mParamRadius;
    private BasicParameterInt mParamSize;
    private int mPramMode;

    public FilterColorBorderRepresentation(int resource, int color, int size, int radius) {
        super("ColorBorder");
        this.mParamSize = new BasicParameterInt(0, 3, 2, 30);
        this.mParamRadius = new BasicParameterInt(1, 2, 0, 100);
        this.mParamColor = new ParameterColor(2, DEFAULT_MENU_COLOR1);
        this.mAllParam = new Parameter[]{this.mParamSize, this.mParamRadius, this.mParamColor};
        setSerializationName("COLORBORDER");
        setFilterType(1);
        setTextId(resource);
        setEditorId(R.id.editorColorBorder);
        setShowParameterValue(false);
        setFilterClass(ImageFilterColorBorder.class);
        this.mParamColor.setValue(color);
        this.mParamSize.setValue(size);
        this.mParamRadius.setValue(radius);
        this.mParamColor.setColorpalette(new int[]{DEFAULT_MENU_COLOR1, DEFAULT_MENU_COLOR2, DEFAULT_MENU_COLOR3, DEFAULT_MENU_COLOR4, DEFAULT_MENU_COLOR5});
    }

    @Override
    public String toString() {
        return "FilterBorder: " + getName();
    }

    @Override
    public FilterRepresentation copy() {
        FilterColorBorderRepresentation representation = new FilterColorBorderRepresentation(getTextId(), 0, 0, 0);
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
        if (a instanceof FilterColorBorderRepresentation) {
            FilterColorBorderRepresentation representation = (FilterColorBorderRepresentation) a;
            setName(representation.getName());
            setColor(representation.getColor());
            this.mParamColor.copyPalletFrom(representation.mParamColor);
            setBorderSize(representation.getBorderSize());
            setBorderRadius(representation.getBorderRadius());
        }
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation) || !(representation instanceof FilterColorBorderRepresentation)) {
            return false;
        }
        FilterColorBorderRepresentation border = (FilterColorBorderRepresentation) representation;
        return border.mParamColor.getValue() == this.mParamColor.getValue() && border.mParamRadius.getValue() == this.mParamRadius.getValue() && border.mParamSize.getValue() == this.mParamSize.getValue();
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    public Parameter getParam(int mode) {
        return this.mAllParam[mode];
    }

    @Override
    public int getTextId() {
        return super.getTextId() == 0 ? R.string.borders : super.getTextId();
    }

    public int getColor() {
        return this.mParamColor.getValue();
    }

    public void setColor(int color) {
        this.mParamColor.setValue(color);
    }

    public int getBorderSize() {
        return this.mParamSize.getValue();
    }

    public void setBorderSize(int borderSize) {
        this.mParamSize.setValue(borderSize);
    }

    public int getBorderRadius() {
        return this.mParamRadius.getValue();
    }

    public void setBorderRadius(int borderRadius) {
        this.mParamRadius.setValue(borderRadius);
    }

    public void setPramMode(int pramMode) {
        this.mPramMode = pramMode;
    }

    public Parameter getCurrentParam() {
        return this.mAllParam[this.mPramMode];
    }

    public String getValueString() {
        return "";
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("size");
        writer.value(this.mParamSize.getValue());
        writer.name("radius");
        writer.value(this.mParamRadius.getValue());
        writer.name("color");
        writer.value(this.mParamColor.getValue());
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equalsIgnoreCase("size")) {
                this.mParamSize.setValue(reader.nextInt());
            } else if (name.equalsIgnoreCase("radius")) {
                this.mParamRadius.setValue(reader.nextInt());
            } else if (name.equalsIgnoreCase("color")) {
                this.mParamColor.setValue(reader.nextInt());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
}
