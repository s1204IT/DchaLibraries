package com.android.gallery3d.filtershow.controller;

public class ParameterHue extends BasicParameterInt {
    public static String sParameterType = "ParameterHue";
    float[] mHSVO;

    @Override
    public String getParameterType() {
        return sParameterType;
    }

    public float[] getColor() {
        this.mHSVO[3] = getValue() / getMaximum();
        return this.mHSVO;
    }
}
