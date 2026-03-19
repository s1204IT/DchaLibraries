package com.android.gallery3d.filtershow.controller;

public class BasicParameterStyle implements ParameterStyles {
    public final int ID;
    protected Control mControl;
    protected FilterView mEditor;
    protected int mNumberOfStyles;
    protected String mParameterName;
    protected int mSelectedStyle;
    protected int mDefaultStyle = 0;
    private final String LOGTAG = "BasicParameterStyle";

    public BasicParameterStyle(int i, int i2) {
        this.ID = i;
        this.mNumberOfStyles = i2;
    }

    @Override
    public String getParameterName() {
        return this.mParameterName;
    }

    @Override
    public String getParameterType() {
        return "ParameterStyles";
    }

    public String getValueString() {
        return this.mParameterName + this.mSelectedStyle;
    }

    @Override
    public void setController(Control control) {
        this.mControl = control;
    }

    @Override
    public int getNumberOfStyles() {
        return this.mNumberOfStyles;
    }

    @Override
    public int getSelected() {
        return this.mSelectedStyle;
    }

    @Override
    public void setSelected(int i) {
        this.mSelectedStyle = i;
        if (this.mEditor != null) {
            this.mEditor.commitLocalRepresentation();
        }
    }

    @Override
    public void getIcon(int i, BitmapCaller bitmapCaller) {
        this.mEditor.computeIcon(i, bitmapCaller);
    }

    public String toString() {
        return getValueString();
    }

    @Override
    public void setFilterView(FilterView filterView) {
        this.mEditor = filterView;
    }
}
