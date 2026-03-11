package com.android.systemui;

import android.app.Notification;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Map;

public abstract class SystemUI implements SysUiServiceProvider {
    public Map<Class<?>, Object> mComponents;
    public Context mContext;

    public abstract void start();

    protected void onConfigurationChanged(Configuration configuration) {
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    protected void onBootCompleted() {
    }

    @Override
    public <T> T getComponent(Class<T> cls) {
        if (this.mComponents != null) {
            return (T) this.mComponents.get(cls);
        }
        return null;
    }

    public <T, C extends T> void putComponent(Class<T> cls, C c) {
        if (this.mComponents != null) {
            this.mComponents.put(cls, c);
        }
    }

    public static void overrideNotificationAppName(Context context, Notification.Builder builder, boolean z) {
        String string;
        Bundle bundle = new Bundle();
        if (z) {
            string = context.getString(android.R.string.ext_media_unsupported_notification_title);
        } else {
            string = context.getString(android.R.string.ext_media_unsupported_notification_message);
        }
        bundle.putString("android.substName", string);
        builder.addExtras(bundle);
    }
}
