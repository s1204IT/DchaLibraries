package com.android.launcher3;

import android.content.ComponentName;
import android.text.TextUtils;
import android.util.Log;

public abstract class AppFilter {
    public abstract boolean shouldShowApp(ComponentName componentName);

    public static AppFilter loadByName(String className) {
        if (TextUtils.isEmpty(className)) {
            return null;
        }
        try {
            Class<?> cls = Class.forName(className);
            return (AppFilter) cls.newInstance();
        } catch (ClassCastException e) {
            Log.e("AppFilter", "Bad AppFilter class", e);
            return null;
        } catch (ClassNotFoundException e2) {
            Log.e("AppFilter", "Bad AppFilter class", e2);
            return null;
        } catch (IllegalAccessException e3) {
            Log.e("AppFilter", "Bad AppFilter class", e3);
            return null;
        } catch (InstantiationException e4) {
            Log.e("AppFilter", "Bad AppFilter class", e4);
            return null;
        }
    }
}
