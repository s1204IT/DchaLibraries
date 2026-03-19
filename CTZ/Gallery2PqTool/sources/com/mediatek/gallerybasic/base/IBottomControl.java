package com.mediatek.gallerybasic.base;

import android.content.Intent;
import android.view.ViewGroup;

public interface IBottomControl {
    public static final int DISPLAY_FALSE = 2;
    public static final int DISPLAY_IGNORE = 0;
    public static final int DISPLAY_TRUE = 1;

    int canDisplayBottomControlButton(int i, MediaData mediaData);

    int canDisplayBottomControls();

    void init(ViewGroup viewGroup, BackwardBottomController backwardBottomController);

    boolean onActivityResult(int i, int i2, Intent intent);

    boolean onBackPressed();

    boolean onBottomControlButtonClicked(int i, MediaData mediaData);

    void onBottomControlCreated();

    boolean onUpPressed();
}
