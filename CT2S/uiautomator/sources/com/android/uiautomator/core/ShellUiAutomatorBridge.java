package com.android.uiautomator.core;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.UiAutomation;
import android.content.IContentProvider;
import android.database.Cursor;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Binder;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.IWindowManager;

public class ShellUiAutomatorBridge extends UiAutomatorBridge {
    private static final String LOG_TAG = ShellUiAutomatorBridge.class.getSimpleName();

    public ShellUiAutomatorBridge(UiAutomation uiAutomation) {
        super(uiAutomation);
    }

    @Override
    public Display getDefaultDisplay() {
        return DisplayManagerGlobal.getInstance().getRealDisplay(0);
    }

    @Override
    public long getSystemLongPressTime() {
        long longPressTimeout = 0;
        IContentProvider provider = null;
        Cursor cursor = null;
        try {
            IActivityManager activityManager = ActivityManagerNative.getDefault();
            String providerName = Settings.Secure.CONTENT_URI.getAuthority();
            IBinder token = new Binder();
            try {
                IActivityManager.ContentProviderHolder holder = activityManager.getContentProviderExternal(providerName, 0, token);
                if (holder == null) {
                    throw new IllegalStateException("Could not find provider: " + providerName);
                }
                provider = holder.provider;
                cursor = provider.query((String) null, Settings.Secure.CONTENT_URI, new String[]{"value"}, "name=?", new String[]{"long_press_timeout"}, (String) null, (ICancellationSignal) null);
                if (cursor.moveToFirst()) {
                    longPressTimeout = cursor.getInt(0);
                }
                return longPressTimeout;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (provider != null) {
                    activityManager.removeContentProviderExternal(providerName, token);
                }
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error reading long press timeout setting.", e);
            throw new RuntimeException("Error reading long press timeout setting.", e);
        }
    }

    @Override
    public int getRotation() {
        IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        try {
            int ret = wm.getRotation();
            return ret;
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error getting screen rotation", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isScreenOn() {
        IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
        try {
            boolean ret = pm.isInteractive();
            return ret;
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error getting screen status", e);
            throw new RuntimeException(e);
        }
    }
}
