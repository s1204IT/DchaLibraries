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

    public void copyFrom(Parameter src) {
        if (!(src instanceof BasicParameterInt)) {
            throw new IllegalArgumentException(src.getClass().getName());
        }
        BasicParameterInt p = (BasicParameterInt) src;
        this.mMaximum = p.mMaximum;
        this.mMinimum = p.mMinimum;
        this.mDefaultValue = p.mDefaultValue;
        this.mValue = p.mValue;
    }

    public BasicParameterInt(int id, int value, int min, int max) {
        this.mMaximum = 100;
        this.mMinimum = 0;
        this.ID = id;
        this.mValue = value;
        this.mMinimum = min;
        this.mMaximum = max;
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
    public void setValue(int value) {
        this.mValue = value;
        if (this.mEditor != null) {
            this.mEditor.commitLocalRepresentation();
        }
    }

    public String toString() {
        return getValueString();
    }

    @Override
    public void setFilterView(FilterView editor) {
        this.mEditor = editor;
    }
}
