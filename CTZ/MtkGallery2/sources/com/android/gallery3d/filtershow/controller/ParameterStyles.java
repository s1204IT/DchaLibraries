package com.android.gallery3d.filtershow.controller;

public interface ParameterStyles extends Parameter {
    void getIcon(int i, BitmapCaller bitmapCaller);

    int getNumberOfStyles();

    int getSelected();

    void setSelected(int i);
}
