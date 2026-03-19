package com.mediatek.galleryportable;

import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import android.widget.ActivityChooserView;

public class ActivityChooserViewWrapper {
    private static boolean sHasChecked = false;
    private static boolean sIsActivityChooserViewExist = false;
    private ActivityChooserView mActivityChooserView;

    public ActivityChooserViewWrapper(MenuItem menuItem) {
        checkWetherSupport();
        if (sIsActivityChooserViewExist) {
            this.mActivityChooserView = menuItem.getActionView();
        }
    }

    public void setExpandActivityOverflowButtonDrawable(Drawable drawable) {
        checkWetherSupport();
        if (sIsActivityChooserViewExist) {
            this.mActivityChooserView.setExpandActivityOverflowButtonDrawable(drawable);
        }
    }

    private static void checkWetherSupport() {
        if (!sHasChecked) {
            try {
                Class<?> clazz = ActivityChooserViewWrapper.class.getClassLoader().loadClass("android.widget.ActivityChooserView");
                sIsActivityChooserViewExist = clazz != null;
            } catch (ClassNotFoundException e) {
                sIsActivityChooserViewExist = false;
            }
            sHasChecked = true;
        }
    }
}
