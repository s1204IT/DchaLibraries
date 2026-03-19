package com.android.deskclock.data;

import android.content.SharedPreferences;

final class WidgetDAO {
    private static final String WIDGET_COUNT = "_widget_count";

    private WidgetDAO() {
    }

    static int updateWidgetCount(SharedPreferences sharedPreferences, Class cls, int i) {
        String str = cls.getSimpleName() + WIDGET_COUNT;
        int i2 = sharedPreferences.getInt(str, 0);
        if (i == 0) {
            sharedPreferences.edit().remove(str).apply();
        } else {
            sharedPreferences.edit().putInt(str, i).apply();
        }
        return i - i2;
    }
}
