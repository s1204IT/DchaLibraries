package com.android.gallery3d.filtershow.controller;

public interface ParameterInteger extends Parameter {
    int getMaximum();

    int getMinimum();

    int getValue();

    void setValue(int i);
}
