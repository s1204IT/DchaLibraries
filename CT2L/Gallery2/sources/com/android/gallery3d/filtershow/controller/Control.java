package com.android.gallery3d.filtershow.controller;

import android.view.ViewGroup;
import com.android.gallery3d.filtershow.editors.Editor;

public interface Control {
    void setPrameter(Parameter parameter);

    void setUp(ViewGroup viewGroup, Parameter parameter, Editor editor);

    void updateUI();
}
