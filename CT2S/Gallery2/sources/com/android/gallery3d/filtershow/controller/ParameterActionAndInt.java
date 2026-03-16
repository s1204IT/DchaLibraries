package com.android.gallery3d.filtershow.controller;

public interface ParameterActionAndInt extends ParameterInteger {
    void fireLeftAction();

    void fireRightAction();

    int getLeftIcon();

    int getRightIcon();
}
