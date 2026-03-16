package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.filtershow.editors.BasicEditor;
import java.io.IOException;
import java.util.ArrayList;

public class FilterRepresentation {
    private Class<?> mFilterClass;
    private String mName;
    private String mSerializationName;
    private int mPriority = 5;
    private boolean mSupportsPartialRendering = false;
    private int mTextId = 0;
    private int mEditorId = BasicEditor.ID;
    private int mButtonId = 0;
    private int mOverlayId = 0;
    private boolean mOverlayOnly = false;
    private boolean mShowParameterValue = true;
    private boolean mIsBooleanFilter = false;

    public FilterRepresentation(String name) {
        this.mName = name;
    }

    public FilterRepresentation copy() {
        FilterRepresentation representation = new FilterRepresentation(this.mName);
        representation.useParametersFrom(this);
        return representation;
    }

    protected void copyAllParameters(FilterRepresentation representation) {
        representation.setName(getName());
        representation.setFilterClass(getFilterClass());
        representation.setFilterType(getFilterType());
        representation.setSupportsPartialRendering(supportsPartialRendering());
        representation.setTextId(getTextId());
        representation.setEditorId(getEditorId());
        representation.setOverlayId(getOverlayId());
        representation.setOverlayOnly(getOverlayOnly());
        representation.setShowParameterValue(showParameterValue());
        representation.mSerializationName = this.mSerializationName;
        representation.setIsBooleanFilter(isBooleanFilter());
    }

    public boolean equals(FilterRepresentation representation) {
        return representation != null && representation.mFilterClass == this.mFilterClass && representation.mName.equalsIgnoreCase(this.mName) && representation.mPriority == this.mPriority && representation.supportsPartialRendering() == supportsPartialRendering() && representation.mTextId == this.mTextId && representation.mEditorId == this.mEditorId && representation.mButtonId == this.mButtonId && representation.mOverlayId == this.mOverlayId && representation.mOverlayOnly == this.mOverlayOnly && representation.mShowParameterValue == this.mShowParameterValue && representation.mIsBooleanFilter == this.mIsBooleanFilter;
    }

    public boolean isBooleanFilter() {
        return this.mIsBooleanFilter;
    }

    public void setIsBooleanFilter(boolean value) {
        this.mIsBooleanFilter = value;
    }

    public String toString() {
        return this.mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return this.mName;
    }

    public void setSerializationName(String sname) {
        this.mSerializationName = sname;
    }

    public String getSerializationName() {
        return this.mSerializationName;
    }

    public void setFilterType(int priority) {
        this.mPriority = priority;
    }

    public int getFilterType() {
        return this.mPriority;
    }

    public boolean isNil() {
        return false;
    }

    public boolean supportsPartialRendering() {
        return this.mSupportsPartialRendering;
    }

    public void setSupportsPartialRendering(boolean value) {
        this.mSupportsPartialRendering = value;
    }

    public void useParametersFrom(FilterRepresentation a) {
    }

    public boolean allowsSingleInstanceOnly() {
        return false;
    }

    public Class<?> getFilterClass() {
        return this.mFilterClass;
    }

    public void setFilterClass(Class<?> filterClass) {
        this.mFilterClass = filterClass;
    }

    public boolean same(FilterRepresentation b) {
        return b != null && getFilterClass() == b.getFilterClass();
    }

    public int getTextId() {
        return this.mTextId;
    }

    public void setTextId(int textId) {
        this.mTextId = textId;
    }

    public int getOverlayId() {
        return this.mOverlayId;
    }

    public void setOverlayId(int overlayId) {
        this.mOverlayId = overlayId;
    }

    public boolean getOverlayOnly() {
        return this.mOverlayOnly;
    }

    public void setOverlayOnly(boolean value) {
        this.mOverlayOnly = value;
    }

    public final int getEditorId() {
        return this.mEditorId;
    }

    public void setEditorId(int editorId) {
        this.mEditorId = editorId;
    }

    public boolean showParameterValue() {
        return this.mShowParameterValue;
    }

    public void setShowParameterValue(boolean showParameterValue) {
        this.mShowParameterValue = showParameterValue;
    }

    public String getStateRepresentation() {
        return "";
    }

    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        String[][] rep = serializeRepresentation();
        for (int k = 0; k < rep.length; k++) {
            writer.name(rep[k][0]);
            writer.value(rep[k][1]);
        }
        writer.endObject();
    }

    public String[][] serializeRepresentation() {
        String[][] ret = {new String[]{"Name", getName()}};
        return ret;
    }

    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        ArrayList<String[]> al = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String[] kv = {reader.nextName(), reader.nextString()};
            al.add(kv);
        }
        reader.endObject();
        String[][] oldFormat = (String[][]) al.toArray(new String[al.size()][]);
        deSerializeRepresentation(oldFormat);
    }

    public void deSerializeRepresentation(String[][] rep) {
        for (int i = 0; i < rep.length; i++) {
            if ("Name".equals(rep[i][0])) {
                this.mName = rep[i][1];
                return;
            }
        }
    }

    public boolean canMergeWith(FilterRepresentation representation) {
        return getFilterType() == 7 && representation.getFilterType() == 7;
    }
}
