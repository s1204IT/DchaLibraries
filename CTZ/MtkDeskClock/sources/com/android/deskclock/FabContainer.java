package com.android.deskclock;

public interface FabContainer {
    public static final int BUTTONS_ANIMATION_MASK = 24;
    public static final int BUTTONS_DISABLE = 32;
    public static final int BUTTONS_DISABLE_MASK = 32;
    public static final int BUTTONS_IMMEDIATE = 8;
    public static final int BUTTONS_SHRINK_AND_EXPAND = 16;
    public static final int FAB_AND_BUTTONS_EXPAND = 64;
    public static final int FAB_AND_BUTTONS_IMMEDIATE = 9;
    public static final int FAB_AND_BUTTONS_SHRINK = 128;
    public static final int FAB_AND_BUTTONS_SHRINK_AND_EXPAND = 18;
    public static final int FAB_AND_BUTTONS_SHRINK_EXPAND_MASK = 192;
    public static final int FAB_ANIMATION_MASK = 3;
    public static final int FAB_IMMEDIATE = 1;
    public static final int FAB_MORPH = 3;
    public static final int FAB_REQUEST_FOCUS = 4;
    public static final int FAB_REQUEST_FOCUS_MASK = 4;
    public static final int FAB_SHRINK_AND_EXPAND = 2;

    public @interface UpdateFabFlag {
    }

    void updateFab(@UpdateFabFlag int i);
}
