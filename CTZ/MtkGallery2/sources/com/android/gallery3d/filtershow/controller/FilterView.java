package com.android.gallery3d.filtershow.controller;

public interface FilterView {
    void commitLocalRepresentation();

    void computeIcon(int i, BitmapCaller bitmapCaller);
}
