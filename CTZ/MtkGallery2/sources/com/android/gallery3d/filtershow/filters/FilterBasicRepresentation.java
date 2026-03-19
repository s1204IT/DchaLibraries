package com.android.gallery3d.filtershow.filters;

import android.util.Log;
import com.android.gallery3d.filtershow.controller.Control;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.controller.ParameterInteger;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class FilterBasicRepresentation extends FilterRepresentation implements ParameterInteger {
    private int mDefaultValue;
    private boolean mLogVerbose;
    private int mMaximum;
    private int mMinimum;
    private int mPreviewValue;
    private int mValue;

    public FilterBasicRepresentation(String str, int i, int i2, int i3) {
        super(str);
        this.mLogVerbose = Log.isLoggable("FilterBasicRep", 2);
        this.mMinimum = i;
        this.mMaximum = i3;
        setValue(i2);
    }

    @Override
    public String toString() {
        return getName() + " : " + this.mMinimum + " < " + this.mValue + " < " + this.mMaximum;
    }

    @Override
    public FilterRepresentation copy() {
        FilterBasicRepresentation filterBasicRepresentation = new FilterBasicRepresentation(getName(), 0, 0, 0);
        copyAllParameters(filterBasicRepresentation);
        return filterBasicRepresentation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation filterRepresentation) {
        super.copyAllParameters(filterRepresentation);
        filterRepresentation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation filterRepresentation) {
        if (filterRepresentation instanceof FilterBasicRepresentation) {
            setMinimum(filterRepresentation.getMinimum());
            setMaximum(filterRepresentation.getMaximum());
            setValue(filterRepresentation.getValue());
            setDefaultValue(filterRepresentation.getDefaultValue());
            setPreviewValue(filterRepresentation.getPreviewValue());
        }
    }

    @Override
    public boolean equals(FilterRepresentation filterRepresentation) {
        return super.equals(filterRepresentation) && (filterRepresentation instanceof FilterBasicRepresentation) && filterRepresentation.mMinimum == this.mMinimum && filterRepresentation.mMaximum == this.mMaximum && filterRepresentation.mValue == this.mValue && filterRepresentation.mDefaultValue == this.mDefaultValue && filterRepresentation.mPreviewValue == this.mPreviewValue;
    }

    @Override
    public int getMinimum() {
        return this.mMinimum;
    }

    public void setMinimum(int i) {
        this.mMinimum = i;
    }

    @Override
    public int getValue() {
        return this.mValue;
    }

    @Override
    public void setValue(int i) {
        this.mValue = i;
        if (this.mValue < this.mMinimum) {
            this.mValue = this.mMinimum;
        }
        if (this.mValue > this.mMaximum) {
            this.mValue = this.mMaximum;
        }
    }

    @Override
    public int getMaximum() {
        return this.mMaximum;
    }

    public void setMaximum(int i) {
        this.mMaximum = i;
    }

    public void setDefaultValue(int i) {
        this.mDefaultValue = i;
    }

    public int getDefaultValue() {
        return this.mDefaultValue;
    }

    public int getPreviewValue() {
        return this.mPreviewValue;
    }

    public void setPreviewValue(int i) {
        this.mPreviewValue = i;
    }

    @Override
    public String getStateRepresentation() {
        int value = getValue();
        StringBuilder sb = new StringBuilder();
        sb.append(value > 0 ? "+" : "");
        sb.append(value);
        return sb.toString();
    }

    @Override
    public String getParameterType() {
        return "ParameterInteger";
    }

    @Override
    public void setController(Control control) {
    }

    @Override
    public String getParameterName() {
        return getName();
    }

    @Override
    public void setFilterView(FilterView filterView) {
    }

    @Override
    public String[][] serializeRepresentation() {
        return new String[][]{new String[]{SchemaSymbols.ATTVAL_NAME, getName()}, new String[]{"Value", Integer.toString(this.mValue)}};
    }

    @Override
    public void deSerializeRepresentation(String[][] strArr) {
        super.deSerializeRepresentation(strArr);
        for (int i = 0; i < strArr.length; i++) {
            if ("Value".equals(strArr[i][0])) {
                this.mValue = Integer.parseInt(strArr[i][1]);
                return;
            }
        }
    }
}
