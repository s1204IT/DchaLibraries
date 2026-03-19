package com.android.deskclock.data;

import android.content.SharedPreferences;
import android.support.annotation.StringRes;
import com.android.deskclock.R;
import com.android.deskclock.events.Events;

final class WidgetModel {
    private final SharedPreferences mPrefs;

    WidgetModel(SharedPreferences sharedPreferences) {
        this.mPrefs = sharedPreferences;
    }

    void updateWidgetCount(Class cls, int i, @StringRes int i2) {
        int iUpdateWidgetCount = WidgetDAO.updateWidgetCount(this.mPrefs, cls, i);
        while (iUpdateWidgetCount > 0) {
            Events.sendEvent(i2, R.string.action_create, 0);
            iUpdateWidgetCount--;
        }
        while (iUpdateWidgetCount < 0) {
            Events.sendEvent(i2, R.string.action_delete, 0);
            iUpdateWidgetCount++;
        }
    }
}
