package com.android.settings.utils;

import android.content.Context;
import android.view.ContextThemeWrapper;

public class LocalClassLoaderContextThemeWrapper extends ContextThemeWrapper {
    private Class mLocalClass;

    public LocalClassLoaderContextThemeWrapper(Class cls, Context context, int i) {
        super(context, i);
        this.mLocalClass = cls;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.mLocalClass.getClassLoader();
    }
}
