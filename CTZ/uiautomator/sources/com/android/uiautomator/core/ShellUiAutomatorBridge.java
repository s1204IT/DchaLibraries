package com.android.uiautomator.core;

import android.app.ActivityManager;
import android.app.ContentProviderHolder;
import android.app.IActivityManager;
import android.app.UiAutomation;
import android.content.ContentResolver;
import android.content.IContentProvider;
import android.database.Cursor;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Binder;
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
    public long getSystemLongPressTime() throws Throwable {
        IContentProvider iContentProvider;
        long j;
        try {
            IActivityManager service = ActivityManager.getService();
            String authority = Settings.Secure.CONTENT_URI.getAuthority();
            Binder binder = new Binder();
            Cursor cursor = null;
            try {
                ContentProviderHolder contentProviderExternal = service.getContentProviderExternal(authority, 0, binder);
                if (contentProviderExternal == null) {
                    throw new IllegalStateException("Could not find provider: " + authority);
                }
                iContentProvider = contentProviderExternal.provider;
                try {
                    Cursor cursorQuery = iContentProvider.query((String) null, Settings.Secure.CONTENT_URI, new String[]{"value"}, ContentResolver.createSqlQueryBundle("name=?", new String[]{"long_press_timeout"}, null), (ICancellationSignal) null);
                    try {
                        if (cursorQuery.moveToFirst()) {
                            j = cursorQuery.getInt(0);
                        } else {
                            j = 0;
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        if (iContentProvider != null) {
                            service.removeContentProviderExternal(authority, binder);
                        }
                        return j;
                    } catch (Throwable th) {
                        th = th;
                        cursor = cursorQuery;
                        if (cursor != null) {
                            cursor.close();
                        }
                        if (iContentProvider != null) {
                            service.removeContentProviderExternal(authority, binder);
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
                iContentProvider = null;
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error reading long press timeout setting.", e);
            throw new RuntimeException("Error reading long press timeout setting.", e);
        }
    }

    @Override
    public int getRotation() {
        try {
            return IWindowManager.Stub.asInterface(ServiceManager.getService("window")).getDefaultDisplayRotation();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error getting screen rotation", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isScreenOn() {
        try {
            return IPowerManager.Stub.asInterface(ServiceManager.getService("power")).isInteractive();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error getting screen status", e);
            throw new RuntimeException(e);
        }
    }
}
