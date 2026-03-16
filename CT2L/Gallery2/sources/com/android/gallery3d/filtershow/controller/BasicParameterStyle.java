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

    public BasicParameterStyle(int id, int numberOfStyles) {
        this.ID = id;
        this.mNumberOfStyles = numberOfStyles;
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

    public int getSelected() {
        return this.mSelectedStyle;
    }

    @Override
    public void setSelected(int selectedStyle) {
        this.mSelectedStyle = selectedStyle;
        if (this.mEditor != null) {
            this.mEditor.commitLocalRepresentation();
        }
    }

    @Override
    public void getIcon(int index, BitmapCaller caller) {
        this.mEditor.computeIcon(index, caller);
    }

    public String toString() {
        return getValueString();
    }

    @Override
    public void setFilterView(FilterView editor) {
        this.mEditor = editor;
    }
}
