package com.android.systemui.plugins.statusbar.phone;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.annotations.ProvidesInterface;

@ProvidesInterface(action = NavBarButtonProvider.ACTION, version = 2)
public interface NavBarButtonProvider extends Plugin {
    public static final String ACTION = "com.android.systemui.action.PLUGIN_NAV_BUTTON";
    public static final int VERSION = 2;

    View createView(String str, ViewGroup viewGroup);

    public interface ButtonInterface {
        void abortCurrentGesture();

        void setDarkIntensity(float f);

        void setDelayTouchFeedback(boolean z);

        void setImageDrawable(Drawable drawable);

        void setVertical(boolean z);

        default void setCarMode(boolean z) {
        }
    }
}
