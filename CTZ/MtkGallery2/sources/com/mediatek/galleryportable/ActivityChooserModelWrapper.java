package com.mediatek.galleryportable;

import android.app.Activity;
import android.content.Intent;
import android.widget.ActivityChooserModel;

public class ActivityChooserModelWrapper {
    private static boolean sHasChecked = false;
    private static boolean sIsActivityChooserModelExist = false;
    private ActivityChooserModel mActivityChooserModel;
    private OnChooseActivityListenerWrapper mChooseActivityListenerWrapper;

    public interface OnChooseActivityListenerWrapper {
        boolean onChooseActivity(ActivityChooserModelWrapper activityChooserModelWrapper, Intent intent);
    }

    class MyOnChooseActivityListener implements ActivityChooserModel.OnChooseActivityListener {
        MyOnChooseActivityListener() {
        }

        public boolean onChooseActivity(ActivityChooserModel host, Intent intent) {
            return ActivityChooserModelWrapper.this.mChooseActivityListenerWrapper.onChooseActivity(ActivityChooserModelWrapper.this, intent);
        }
    }

    public ActivityChooserModelWrapper(Activity activity, String xml) {
        checkWetherSupport();
        if (sIsActivityChooserModelExist) {
            this.mActivityChooserModel = ActivityChooserModel.get(activity, xml);
        }
    }

    public void setOnChooseActivityListener(OnChooseActivityListenerWrapper listener) {
        checkWetherSupport();
        if (sIsActivityChooserModelExist) {
            this.mChooseActivityListenerWrapper = listener;
            if (this.mChooseActivityListenerWrapper == null) {
                this.mActivityChooserModel.setOnChooseActivityListener((ActivityChooserModel.OnChooseActivityListener) null);
            } else {
                this.mActivityChooserModel.setOnChooseActivityListener(new MyOnChooseActivityListener());
            }
        }
    }

    private void checkWetherSupport() {
        if (!sHasChecked) {
            try {
                Class<?> clazz = ActivityChooserModelWrapper.class.getClassLoader().loadClass("android.widget.ActivityChooserModel");
                sIsActivityChooserModelExist = clazz != null;
            } catch (ClassNotFoundException e) {
                sIsActivityChooserModelExist = false;
            }
            sHasChecked = true;
        }
    }
}
