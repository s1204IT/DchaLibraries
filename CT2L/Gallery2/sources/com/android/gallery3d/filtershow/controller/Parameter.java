package com.android.gallery3d.filtershow.controller;

public interface Parameter {
    String getParameterName();

    String getParameterType();

    void setController(Control control);

    void setFilterView(FilterView filterView);
}
