package com.android.gallery3d.filtershow.filters;

import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.filtershow.editors.BasicEditor;
import java.io.IOException;
import java.util.ArrayList;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

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

    public FilterRepresentation(String str) {
        this.mName = str;
    }

    public FilterRepresentation copy() {
        FilterRepresentation filterRepresentation = new FilterRepresentation(this.mName);
        filterRepresentation.useParametersFrom(this);
        return filterRepresentation;
    }

    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        filterRepresentation.setName(getName());
        filterRepresentation.setFilterClass(getFilterClass());
        filterRepresentation.setFilterType(getFilterType());
        filterRepresentation.setSupportsPartialRendering(supportsPartialRendering());
        filterRepresentation.setTextId(getTextId());
        filterRepresentation.setEditorId(getEditorId());
        filterRepresentation.setOverlayId(getOverlayId());
        filterRepresentation.setOverlayOnly(getOverlayOnly());
        filterRepresentation.setShowParameterValue(showParameterValue());
        filterRepresentation.mSerializationName = this.mSerializationName;
        filterRepresentation.setIsBooleanFilter(isBooleanFilter());
    }

    public boolean equals(FilterRepresentation filterRepresentation) {
        if (filterRepresentation == null || filterRepresentation.mFilterClass != this.mFilterClass || !filterRepresentation.mName.equalsIgnoreCase(this.mName) || filterRepresentation.mPriority != this.mPriority || filterRepresentation.supportsPartialRendering() != supportsPartialRendering() || filterRepresentation.mTextId != this.mTextId || filterRepresentation.mEditorId != this.mEditorId || filterRepresentation.mButtonId != this.mButtonId || filterRepresentation.mOverlayId != this.mOverlayId || filterRepresentation.mOverlayOnly != this.mOverlayOnly || filterRepresentation.mShowParameterValue != this.mShowParameterValue || filterRepresentation.mIsBooleanFilter != this.mIsBooleanFilter) {
            return false;
        }
        return true;
    }

    public boolean isBooleanFilter() {
        return this.mIsBooleanFilter;
    }

    public void setIsBooleanFilter(boolean z) {
        this.mIsBooleanFilter = z;
    }

    public String toString() {
        return this.mName;
    }

    public void setName(String str) {
        this.mName = str;
    }

    public String getName() {
        return this.mName;
    }

    public void setSerializationName(String str) {
        this.mSerializationName = str;
    }

    public String getSerializationName() {
        return this.mSerializationName;
    }

    public void setFilterType(int i) {
        this.mPriority = i;
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

    public void setSupportsPartialRendering(boolean z) {
        this.mSupportsPartialRendering = z;
    }

    public void useParametersFrom(FilterRepresentation filterRepresentation) {
    }

    public boolean allowsSingleInstanceOnly() {
        return false;
    }

    public Class<?> getFilterClass() {
        return this.mFilterClass;
    }

    public void setFilterClass(Class<?> cls) {
        this.mFilterClass = cls;
    }

    public boolean same(FilterRepresentation filterRepresentation) {
        return filterRepresentation != null && getFilterClass() == filterRepresentation.getFilterClass();
    }

    public int getTextId() {
        return this.mTextId;
    }

    public void setTextId(int i) {
        this.mTextId = i;
    }

    public int getOverlayId() {
        return this.mOverlayId;
    }

    public void setOverlayId(int i) {
        this.mOverlayId = i;
    }

    public boolean getOverlayOnly() {
        return this.mOverlayOnly;
    }

    public void setOverlayOnly(boolean z) {
        this.mOverlayOnly = z;
    }

    public final int getEditorId() {
        return this.mEditorId;
    }

    public void setEditorId(int i) {
        this.mEditorId = i;
    }

    public boolean showParameterValue() {
        return this.mShowParameterValue;
    }

    public void setShowParameterValue(boolean z) {
        this.mShowParameterValue = z;
    }

    public String getStateRepresentation() {
        return "";
    }

    public void serializeRepresentation(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        String[][] strArrSerializeRepresentation = serializeRepresentation();
        for (int i = 0; i < strArrSerializeRepresentation.length; i++) {
            jsonWriter.name(strArrSerializeRepresentation[i][0]);
            jsonWriter.value(strArrSerializeRepresentation[i][1]);
        }
        jsonWriter.endObject();
    }

    public String[][] serializeRepresentation() {
        return new String[][]{new String[]{SchemaSymbols.ATTVAL_NAME, getName()}};
    }

    public void deSerializeRepresentation(JsonReader jsonReader) throws IOException {
        ArrayList arrayList = new ArrayList();
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            arrayList.add(new String[]{jsonReader.nextName(), jsonReader.nextString()});
        }
        jsonReader.endObject();
        deSerializeRepresentation((String[][]) arrayList.toArray(new String[arrayList.size()][]));
    }

    public void deSerializeRepresentation(String[][] strArr) {
        for (int i = 0; i < strArr.length; i++) {
            if (SchemaSymbols.ATTVAL_NAME.equals(strArr[i][0])) {
                this.mName = strArr[i][1];
                return;
            }
        }
    }

    public boolean canMergeWith(FilterRepresentation filterRepresentation) {
        if (getFilterType() == 7 && filterRepresentation.getFilterType() == 7) {
            return true;
        }
        return false;
    }

    public void resetRepresentation() {
    }
}
