package com.android.settings.utils;

import android.content.Context;
import android.view.ContextThemeWrapper;

/* loaded from: classes.dex */
public class LocalClassLoaderContextThemeWrapper extends ContextThemeWrapper {
    private Class mLocalClass;

    public LocalClassLoaderContextThemeWrapper(Class cls, Context context, int i) {
        super(context, i);
        this.mLocalClass = cls;
    }

    @Override // android.content.ContextWrapper, android.content.Context
    public ClassLoader getClassLoader() {
        return this.mLocalClass.getClassLoader();
    }
}
