package com.android.gallery3d.filtershow.filters;

import android.util.Log;
import com.android.gallery3d.filtershow.controller.Control;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.controller.ParameterInteger;

public class FilterBasicRepresentation extends FilterRepresentation implements ParameterInteger {
    private int mDefaultValue;
    private boolean mLogVerbose;
    private int mMaximum;
    private int mMinimum;
    private int mPreviewValue;
    private int mValue;

    public FilterBasicRepresentation(String name, int minimum, int value, int maximum) {
        super(name);
        this.mLogVerbose = Log.isLoggable("FilterBasicRep", 2);
        this.mMinimum = minimum;
        this.mMaximum = maximum;
        setValue(value);
    }

    @Override
    public String toString() {
        return getName() + " : " + this.mMinimum + " < " + this.mValue + " < " + this.mMaximum;
    }

    @Override
    public FilterRepresentation copy() {
        FilterBasicRepresentation representation = new FilterBasicRepresentation(getName(), 0, 0, 0);
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
        if (a instanceof FilterBasicRepresentation) {
            FilterBasicRepresentation representation = (FilterBasicRepresentation) a;
            setMinimum(representation.getMinimum());
            setMaximum(representation.getMaximum());
            setValue(representation.getValue());
            setDefaultValue(representation.getDefaultValue());
            setPreviewValue(representation.getPreviewValue());
        }
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation) || !(representation instanceof FilterBasicRepresentation)) {
            return false;
        }
        FilterBasicRepresentation basic = (FilterBasicRepresentation) representation;
        return basic.mMinimum == this.mMinimum && basic.mMaximum == this.mMaximum && basic.mValue == this.mValue && basic.mDefaultValue == this.mDefaultValue && basic.mPreviewValue == this.mPreviewValue;
    }

    @Override
    public int getMinimum() {
        return this.mMinimum;
    }

    public void setMinimum(int minimum) {
        this.mMinimum = minimum;
    }

    @Override
    public int getValue() {
        return this.mValue;
    }

    @Override
    public void setValue(int value) {
        this.mValue = value;
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

    public void setMaximum(int maximum) {
        this.mMaximum = maximum;
    }

    public void setDefaultValue(int defaultValue) {
        this.mDefaultValue = defaultValue;
    }

    public int getDefaultValue() {
        return this.mDefaultValue;
    }

    public int getPreviewValue() {
        return this.mPreviewValue;
    }

    public void setPreviewValue(int previewValue) {
        this.mPreviewValue = previewValue;
    }

    @Override
    public String getStateRepresentation() {
        int val = getValue();
        return (val > 0 ? "+" : "") + val;
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
    public void setFilterView(FilterView editor) {
    }

    @Override
    public String[][] serializeRepresentation() {
        String[][] ret = {new String[]{"Name", getName()}, new String[]{"Value", Integer.toString(this.mValue)}};
        return ret;
    }

    @Override
    public void deSerializeRepresentation(String[][] rep) {
        super.deSerializeRepresentation(rep);
        for (int i = 0; i < rep.length; i++) {
            if ("Value".equals(rep[i][0])) {
                this.mValue = Integer.parseInt(rep[i][1]);
                return;
            }
        }
    }
}
