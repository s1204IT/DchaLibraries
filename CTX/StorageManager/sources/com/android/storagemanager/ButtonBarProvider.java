package com.android.storagemanager;

import android.view.ViewGroup;
import android.widget.Button;

public interface ButtonBarProvider {
    ViewGroup getButtonBar();

    Button getNextButton();

    Button getSkipButton();
}
