package android.appwidget;

import android.app.IServiceConnection;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.widget.RemoteViews;
import com.android.internal.appwidget.IAppWidgetService;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AppWidgetManager {
    public static final String ACTION_APPWIDGET_BIND = "android.appwidget.action.APPWIDGET_BIND";
    public static final String ACTION_APPWIDGET_CONFIGURE = "android.appwidget.action.APPWIDGET_CONFIGURE";
    public static final String ACTION_APPWIDGET_DELETED = "android.appwidget.action.APPWIDGET_DELETED";
    public static final String ACTION_APPWIDGET_DISABLED = "android.appwidget.action.APPWIDGET_DISABLED";
    public static final String ACTION_APPWIDGET_ENABLED = "android.appwidget.action.APPWIDGET_ENABLED";
    public static final String ACTION_APPWIDGET_HOST_RESTORED = "android.appwidget.action.APPWIDGET_HOST_RESTORED";
    public static final String ACTION_APPWIDGET_OPTIONS_CHANGED = "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS";
    public static final String ACTION_APPWIDGET_PICK = "android.appwidget.action.APPWIDGET_PICK";
    public static final String ACTION_APPWIDGET_RESTORED = "android.appwidget.action.APPWIDGET_RESTORED";
    public static final String ACTION_APPWIDGET_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE";
    public static final String ACTION_KEYGUARD_APPWIDGET_PICK = "android.appwidget.action.KEYGUARD_APPWIDGET_PICK";
    public static final String EXTRA_APPWIDGET_ID = "appWidgetId";
    public static final String EXTRA_APPWIDGET_IDS = "appWidgetIds";
    public static final String EXTRA_APPWIDGET_OLD_IDS = "appWidgetOldIds";
    public static final String EXTRA_APPWIDGET_OPTIONS = "appWidgetOptions";
    public static final String EXTRA_APPWIDGET_PREVIEW = "appWidgetPreview";
    public static final String EXTRA_APPWIDGET_PROVIDER = "appWidgetProvider";
    public static final String EXTRA_APPWIDGET_PROVIDER_PROFILE = "appWidgetProviderProfile";
    public static final String EXTRA_CATEGORY_FILTER = "categoryFilter";
    public static final String EXTRA_CUSTOM_EXTRAS = "customExtras";
    public static final String EXTRA_CUSTOM_INFO = "customInfo";
    public static final String EXTRA_CUSTOM_SORT = "customSort";
    public static final String EXTRA_HOST_ID = "hostId";
    public static final int INVALID_APPWIDGET_ID = 0;
    public static final String META_DATA_APPWIDGET_PROVIDER = "android.appwidget.provider";
    public static final String OPTION_APPWIDGET_HOST_CATEGORY = "appWidgetCategory";
    public static final String OPTION_APPWIDGET_MAX_HEIGHT = "appWidgetMaxHeight";
    public static final String OPTION_APPWIDGET_MAX_WIDTH = "appWidgetMaxWidth";
    public static final String OPTION_APPWIDGET_MIN_HEIGHT = "appWidgetMinHeight";
    public static final String OPTION_APPWIDGET_MIN_WIDTH = "appWidgetMinWidth";
    private final Context mContext;
    private final DisplayMetrics mDisplayMetrics;
    private final String mPackageName;
    private final IAppWidgetService mService;

    public static AppWidgetManager getInstance(Context context) {
        return (AppWidgetManager) context.getSystemService(Context.APPWIDGET_SERVICE);
    }

    public AppWidgetManager(Context context, IAppWidgetService iAppWidgetService) {
        this.mContext = context;
        this.mPackageName = context.getOpPackageName();
        this.mService = iAppWidgetService;
        this.mDisplayMetrics = context.getResources().getDisplayMetrics();
    }

    public void updateAppWidget(int[] iArr, RemoteViews remoteViews) {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.updateAppWidgetIds(this.mPackageName, iArr, remoteViews);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateAppWidgetOptions(int i, Bundle bundle) {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.updateAppWidgetOptions(this.mPackageName, i, bundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Bundle getAppWidgetOptions(int i) {
        if (this.mService == null) {
            return Bundle.EMPTY;
        }
        try {
            return this.mService.getAppWidgetOptions(this.mPackageName, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateAppWidget(int i, RemoteViews remoteViews) {
        if (this.mService == null) {
            return;
        }
        updateAppWidget(new int[]{i}, remoteViews);
    }

    public void partiallyUpdateAppWidget(int[] iArr, RemoteViews remoteViews) {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.partiallyUpdateAppWidgetIds(this.mPackageName, iArr, remoteViews);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void partiallyUpdateAppWidget(int i, RemoteViews remoteViews) {
        if (this.mService == null) {
            return;
        }
        partiallyUpdateAppWidget(new int[]{i}, remoteViews);
    }

    public void updateAppWidget(ComponentName componentName, RemoteViews remoteViews) {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.updateAppWidgetProvider(componentName, remoteViews);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateAppWidgetProviderInfo(ComponentName componentName, String str) {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.updateAppWidgetProviderInfo(componentName, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void notifyAppWidgetViewDataChanged(int[] iArr, int i) {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.notifyAppWidgetViewDataChanged(this.mPackageName, iArr, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void notifyAppWidgetViewDataChanged(int i, int i2) {
        if (this.mService == null) {
            return;
        }
        notifyAppWidgetViewDataChanged(new int[]{i}, i2);
    }

    public List<AppWidgetProviderInfo> getInstalledProvidersForProfile(UserHandle userHandle) {
        if (this.mService == null) {
            return Collections.emptyList();
        }
        return getInstalledProvidersForProfile(1, userHandle, null);
    }

    public List<AppWidgetProviderInfo> getInstalledProvidersForPackage(String str, UserHandle userHandle) {
        if (str == null) {
            throw new NullPointerException("A non-null package must be passed to this method. If you want all widgets regardless of package, see getInstalledProvidersForProfile(UserHandle)");
        }
        if (this.mService == null) {
            return Collections.emptyList();
        }
        return getInstalledProvidersForProfile(1, userHandle, str);
    }

    public List<AppWidgetProviderInfo> getInstalledProviders() {
        if (this.mService == null) {
            return Collections.emptyList();
        }
        return getInstalledProvidersForProfile(1, null, null);
    }

    public List<AppWidgetProviderInfo> getInstalledProviders(int i) {
        if (this.mService == null) {
            return Collections.emptyList();
        }
        return getInstalledProvidersForProfile(i, null, null);
    }

    public List<AppWidgetProviderInfo> getInstalledProvidersForProfile(int i, UserHandle userHandle, String str) {
        if (this.mService == null) {
            return Collections.emptyList();
        }
        if (userHandle == null) {
            userHandle = this.mContext.getUser();
        }
        try {
            ParceledListSlice installedProvidersForProfile = this.mService.getInstalledProvidersForProfile(i, userHandle.getIdentifier(), str);
            if (installedProvidersForProfile == null) {
                return Collections.emptyList();
            }
            Iterator it = installedProvidersForProfile.getList().iterator();
            while (it.hasNext()) {
                ((AppWidgetProviderInfo) it.next()).updateDimensions(this.mDisplayMetrics);
            }
            return installedProvidersForProfile.getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AppWidgetProviderInfo getAppWidgetInfo(int i) {
        if (this.mService == null) {
            return null;
        }
        try {
            AppWidgetProviderInfo appWidgetInfo = this.mService.getAppWidgetInfo(this.mPackageName, i);
            if (appWidgetInfo != null) {
                appWidgetInfo.updateDimensions(this.mDisplayMetrics);
            }
            return appWidgetInfo;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void bindAppWidgetId(int i, ComponentName componentName) {
        if (this.mService == null) {
            return;
        }
        bindAppWidgetId(i, componentName, null);
    }

    public void bindAppWidgetId(int i, ComponentName componentName, Bundle bundle) {
        if (this.mService == null) {
            return;
        }
        bindAppWidgetIdIfAllowed(i, this.mContext.getUser(), componentName, bundle);
    }

    public boolean bindAppWidgetIdIfAllowed(int i, ComponentName componentName) {
        if (this.mService == null) {
            return false;
        }
        return bindAppWidgetIdIfAllowed(i, this.mContext.getUserId(), componentName, (Bundle) null);
    }

    public boolean bindAppWidgetIdIfAllowed(int i, ComponentName componentName, Bundle bundle) {
        if (this.mService == null) {
            return false;
        }
        return bindAppWidgetIdIfAllowed(i, this.mContext.getUserId(), componentName, bundle);
    }

    public boolean bindAppWidgetIdIfAllowed(int i, UserHandle userHandle, ComponentName componentName, Bundle bundle) {
        if (this.mService == null) {
            return false;
        }
        return bindAppWidgetIdIfAllowed(i, userHandle.getIdentifier(), componentName, bundle);
    }

    public boolean hasBindAppWidgetPermission(String str, int i) {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.hasBindAppWidgetPermission(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasBindAppWidgetPermission(String str) {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.hasBindAppWidgetPermission(str, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setBindAppWidgetPermission(String str, boolean z) {
        if (this.mService == null) {
            return;
        }
        setBindAppWidgetPermission(str, this.mContext.getUserId(), z);
    }

    public void setBindAppWidgetPermission(String str, int i, boolean z) {
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.setBindAppWidgetPermission(str, i, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean bindRemoteViewsService(Context context, int i, Intent intent, IServiceConnection iServiceConnection, int i2) {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.bindRemoteViewsService(context.getOpPackageName(), i, intent, context.getIApplicationThread(), context.getActivityToken(), iServiceConnection, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int[] getAppWidgetIds(ComponentName componentName) {
        if (this.mService == null) {
            return new int[0];
        }
        try {
            return this.mService.getAppWidgetIds(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isBoundWidgetPackage(String str, int i) {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.isBoundWidgetPackage(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean bindAppWidgetIdIfAllowed(int i, int i2, ComponentName componentName, Bundle bundle) {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.bindAppWidgetId(this.mPackageName, i, i2, componentName, bundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isRequestPinAppWidgetSupported() {
        try {
            return this.mService.isRequestPinAppWidgetSupported();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean requestPinAppWidget(ComponentName componentName, PendingIntent pendingIntent) {
        return requestPinAppWidget(componentName, null, pendingIntent);
    }

    public boolean requestPinAppWidget(ComponentName componentName, Bundle bundle, PendingIntent pendingIntent) {
        try {
            return this.mService.requestPinAppWidget(this.mPackageName, componentName, bundle, pendingIntent == null ? null : pendingIntent.getIntentSender());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
