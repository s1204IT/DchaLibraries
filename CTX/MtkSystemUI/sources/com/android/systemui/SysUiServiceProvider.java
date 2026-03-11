package com.android.systemui;

import android.content.Context;

public interface SysUiServiceProvider {
    <T> T getComponent(Class<T> cls);

    static <T> T getComponent(Context context, Class<T> cls) {
        return (T) ((SysUiServiceProvider) context.getApplicationContext()).getComponent(cls);
    }
}
