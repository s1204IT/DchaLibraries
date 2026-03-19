package com.android.gallery3d.filtershow.controller;

public class BasicParameterInt implements ParameterInteger {
    public final int ID;
    private final String LOGTAG = "BasicParameterInt";
    protected Control mControl;
    protected int mDefaultValue;
    protected FilterView mEditor;
    protected int mMaximum;
    protected int mMinimum;
    protected String mParameterName;
    protected int mValue;

    public void copyFrom(Parameter parameter) {
        if (!(parameter instanceof BasicParameterInt)) {
            throw new IllegalArgumentException(parameter.getClass().getName());
        }
        this.mMaximum = parameter.mMaximum;
        this.mMinimum = parameter.mMinimum;
        this.mDefaultValue = parameter.mDefaultValue;
        this.mValue = parameter.mValue;
    }

    public BasicParameterInt(int i, int i2, int i3, int i4) {
        this.mMaximum = 100;
        this.mMinimum = 0;
        this.ID = i;
        this.mValue = i2;
        this.mMinimum = i3;
        this.mMaximum = i4;
    }

    @Override
    public String getParameterName() {
        return this.mParameterName;
    }

    @Override
    public String getParameterType() {
        return "ParameterInteger";
    }

    public String getValueString() {
        return this.mParameterName + this.mValue;
    }

    @Override
    public void setController(Control control) {
        this.mControl = control;
    }

    @Override
    public int getMaximum() {
        return this.mMaximum;
    }

    @Override
    public int getMinimum() {
        return this.mMinimum;
    }

    @Override
    public int getValue() {
        return this.mValue;
    }

    @Override
    public void setValue(int i) {
        this.mValue = i;
        if (this.mEditor != null) {
            this.mEditor.commitLocalRepresentation();
        }
    }

    public String toString() {
        return getValueString();
    }

    @Override
    public void setFilterView(FilterView filterView) {
        this.mEditor = filterView;
    }
}
