package com.android.systemui.plugins;

import android.view.View;
import com.android.systemui.plugins.annotations.ProvidesInterface;

@ProvidesInterface(action = OverlayPlugin.ACTION, version = 2)
public interface OverlayPlugin extends Plugin {
    public static final String ACTION = "com.android.systemui.action.PLUGIN_OVERLAY";
    public static final int VERSION = 2;

    void setup(View view, View view2);

    default boolean holdStatusBarOpen() {
        return false;
    }

    default void setCollapseDesired(boolean z) {
    }
}
