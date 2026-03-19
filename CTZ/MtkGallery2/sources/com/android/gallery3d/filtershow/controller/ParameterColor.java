package com.android.gallery3d.filtershow.controller;

import android.graphics.Color;
import java.util.Arrays;

public class ParameterColor implements Parameter {
    public static String sParameterType = "ParameterColor";
    public final int ID;
    protected Control mControl;
    protected FilterView mEditor;
    String mParameterName;
    int mValue;
    float[] mHSVO = new float[4];
    int[] mBasColors = {-2130771968, -2147418368, -2147483393, Integer.MIN_VALUE, -2130706433};

    public ParameterColor(int i, int i2) {
        this.ID = i;
        Color.colorToHSV(i2, this.mHSVO);
        this.mHSVO[3] = ((i2 >> 24) & 255) / 255.0f;
    }

    @Override
    public String getParameterType() {
        return sParameterType;
    }

    @Override
    public String getParameterName() {
        return this.mParameterName;
    }

    public String getValueString() {
        return "(" + Integer.toHexString(this.mValue) + ")";
    }

    @Override
    public void setController(Control control) {
        this.mControl = control;
    }

    public int getValue() {
        return this.mValue;
    }

    public void setValue(int i) {
        this.mValue = i;
        Color.colorToHSV(this.mValue, this.mHSVO);
        this.mHSVO[3] = ((this.mValue >> 24) & 255) / 255.0f;
    }

    public String toString() {
        return getValueString();
    }

    @Override
    public void setFilterView(FilterView filterView) {
        this.mEditor = filterView;
    }

    public void copyPalletFrom(ParameterColor parameterColor) {
        System.arraycopy(parameterColor.mBasColors, 0, this.mBasColors, 0, this.mBasColors.length);
    }

    public void setColorpalette(int[] iArr) {
        this.mBasColors = Arrays.copyOf(iArr, iArr.length);
    }

    public int[] getColorPalette() {
        return this.mBasColors;
    }
}
