package android.appwidget;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.widget.RemoteViews;
import com.android.internal.R;
import com.android.internal.appwidget.IAppWidgetHost;
import com.android.internal.appwidget.IAppWidgetService;
import java.lang.ref.WeakReference;
import java.util.List;

public class AppWidgetHost {
    static final int HANDLE_PROVIDERS_CHANGED = 3;
    static final int HANDLE_PROVIDER_CHANGED = 2;
    static final int HANDLE_UPDATE = 1;
    static final int HANDLE_VIEW_DATA_CHANGED = 4;
    static IAppWidgetService sService;
    private final Callbacks mCallbacks;
    private String mContextOpPackageName;
    private DisplayMetrics mDisplayMetrics;
    private final Handler mHandler;
    private final int mHostId;
    private RemoteViews.OnClickHandler mOnClickHandler;
    private final SparseArray<AppWidgetHostView> mViews;
    static final Object sServiceLock = new Object();
    static boolean sServiceInitialized = false;

    static class Callbacks extends IAppWidgetHost.Stub {
        private final WeakReference<Handler> mWeakHandler;

        public Callbacks(Handler handler) {
            this.mWeakHandler = new WeakReference<>(handler);
        }

        @Override
        public void updateAppWidget(int i, RemoteViews remoteViews) {
            if (isLocalBinder() && remoteViews != null) {
                remoteViews = remoteViews.mo11clone();
            }
            Handler handler = this.mWeakHandler.get();
            if (handler == null) {
                return;
            }
            handler.obtainMessage(1, i, 0, remoteViews).sendToTarget();
        }

        @Override
        public void providerChanged(int i, AppWidgetProviderInfo appWidgetProviderInfo) {
            if (isLocalBinder() && appWidgetProviderInfo != null) {
                appWidgetProviderInfo = appWidgetProviderInfo.m15clone();
            }
            Handler handler = this.mWeakHandler.get();
            if (handler == null) {
                return;
            }
            handler.obtainMessage(2, i, 0, appWidgetProviderInfo).sendToTarget();
        }

        @Override
        public void providersChanged() {
            Handler handler = this.mWeakHandler.get();
            if (handler == null) {
                return;
            }
            handler.obtainMessage(3).sendToTarget();
        }

        @Override
        public void viewDataChanged(int i, int i2) {
            Handler handler = this.mWeakHandler.get();
            if (handler == null) {
                return;
            }
            handler.obtainMessage(4, i, i2).sendToTarget();
        }

        private static boolean isLocalBinder() {
            return Process.myPid() == Binder.getCallingPid();
        }
    }

    class UpdateHandler extends Handler {
        public UpdateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    AppWidgetHost.this.updateAppWidgetView(message.arg1, (RemoteViews) message.obj);
                    break;
                case 2:
                    AppWidgetHost.this.onProviderChanged(message.arg1, (AppWidgetProviderInfo) message.obj);
                    break;
                case 3:
                    AppWidgetHost.this.onProvidersChanged();
                    break;
                case 4:
                    AppWidgetHost.this.viewDataChanged(message.arg1, message.arg2);
                    break;
            }
        }
    }

    public AppWidgetHost(Context context, int i) {
        this(context, i, null, context.getMainLooper());
    }

    public AppWidgetHost(Context context, int i, RemoteViews.OnClickHandler onClickHandler, Looper looper) {
        this.mViews = new SparseArray<>();
        this.mContextOpPackageName = context.getOpPackageName();
        this.mHostId = i;
        this.mOnClickHandler = onClickHandler;
        this.mHandler = new UpdateHandler(looper);
        this.mCallbacks = new Callbacks(this.mHandler);
        this.mDisplayMetrics = context.getResources().getDisplayMetrics();
        bindService(context);
    }

    private static void bindService(Context context) {
        synchronized (sServiceLock) {
            if (sServiceInitialized) {
                return;
            }
            sServiceInitialized = true;
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS) || context.getResources().getBoolean(R.bool.config_enableAppWidgetService)) {
                sService = IAppWidgetService.Stub.asInterface(ServiceManager.getService(Context.APPWIDGET_SERVICE));
            }
        }
    }

    public void startListening() {
        int[] iArr;
        int i;
        if (sService == null) {
            return;
        }
        synchronized (this.mViews) {
            int size = this.mViews.size();
            iArr = new int[size];
            for (int i2 = 0; i2 < size; i2++) {
                iArr[i2] = this.mViews.keyAt(i2);
            }
        }
        try {
            List list = sService.startListening(this.mCallbacks, this.mContextOpPackageName, this.mHostId, iArr).getList();
            int size2 = list.size();
            for (i = 0; i < size2; i++) {
                PendingHostUpdate pendingHostUpdate = (PendingHostUpdate) list.get(i);
                switch (pendingHostUpdate.type) {
                    case 0:
                        updateAppWidgetView(pendingHostUpdate.appWidgetId, pendingHostUpdate.views);
                        break;
                    case 1:
                        onProviderChanged(pendingHostUpdate.appWidgetId, pendingHostUpdate.widgetInfo);
                        break;
                    case 2:
                        viewDataChanged(pendingHostUpdate.appWidgetId, pendingHostUpdate.viewId);
                        break;
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public void stopListening() {
        if (sService == null) {
            return;
        }
        try {
            sService.stopListening(this.mContextOpPackageName, this.mHostId);
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public int allocateAppWidgetId() {
        if (sService == null) {
            return -1;
        }
        try {
            return sService.allocateAppWidgetId(this.mContextOpPackageName, this.mHostId);
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public final void startAppWidgetConfigureActivityForResult(Activity activity, int i, int i2, int i3, Bundle bundle) {
        if (sService == null) {
            return;
        }
        try {
            IntentSender intentSenderCreateAppWidgetConfigIntentSender = sService.createAppWidgetConfigIntentSender(this.mContextOpPackageName, i, i2);
            if (intentSenderCreateAppWidgetConfigIntentSender != null) {
                activity.startIntentSenderForResult(intentSenderCreateAppWidgetConfigIntentSender, i3, null, 0, 0, 0, bundle);
                return;
            }
            throw new ActivityNotFoundException();
        } catch (IntentSender.SendIntentException e) {
            throw new ActivityNotFoundException();
        } catch (RemoteException e2) {
            throw new RuntimeException("system server dead?", e2);
        }
    }

    public int[] getAppWidgetIds() {
        if (sService == null) {
            return new int[0];
        }
        try {
            return sService.getAppWidgetIdsForHost(this.mContextOpPackageName, this.mHostId);
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public void deleteAppWidgetId(int i) {
        if (sService == null) {
            return;
        }
        synchronized (this.mViews) {
            this.mViews.remove(i);
            try {
                sService.deleteAppWidgetId(this.mContextOpPackageName, i);
            } catch (RemoteException e) {
                throw new RuntimeException("system server dead?", e);
            }
        }
    }

    public void deleteHost() {
        if (sService == null) {
            return;
        }
        try {
            sService.deleteHost(this.mContextOpPackageName, this.mHostId);
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public static void deleteAllHosts() {
        if (sService == null) {
            return;
        }
        try {
            sService.deleteAllHosts();
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public final AppWidgetHostView createView(Context context, int i, AppWidgetProviderInfo appWidgetProviderInfo) {
        if (sService == null) {
            return null;
        }
        AppWidgetHostView appWidgetHostViewOnCreateView = onCreateView(context, i, appWidgetProviderInfo);
        appWidgetHostViewOnCreateView.setOnClickHandler(this.mOnClickHandler);
        appWidgetHostViewOnCreateView.setAppWidget(i, appWidgetProviderInfo);
        synchronized (this.mViews) {
            this.mViews.put(i, appWidgetHostViewOnCreateView);
        }
        try {
            appWidgetHostViewOnCreateView.updateAppWidget(sService.getAppWidgetViews(this.mContextOpPackageName, i));
            return appWidgetHostViewOnCreateView;
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    protected AppWidgetHostView onCreateView(Context context, int i, AppWidgetProviderInfo appWidgetProviderInfo) {
        return new AppWidgetHostView(context, this.mOnClickHandler);
    }

    protected void onProviderChanged(int i, AppWidgetProviderInfo appWidgetProviderInfo) {
        AppWidgetHostView appWidgetHostView;
        appWidgetProviderInfo.updateDimensions(this.mDisplayMetrics);
        synchronized (this.mViews) {
            appWidgetHostView = this.mViews.get(i);
        }
        if (appWidgetHostView != null) {
            appWidgetHostView.resetAppWidget(appWidgetProviderInfo);
        }
    }

    protected void onProvidersChanged() {
    }

    void updateAppWidgetView(int i, RemoteViews remoteViews) {
        AppWidgetHostView appWidgetHostView;
        synchronized (this.mViews) {
            appWidgetHostView = this.mViews.get(i);
        }
        if (appWidgetHostView != null) {
            appWidgetHostView.updateAppWidget(remoteViews);
        }
    }

    void viewDataChanged(int i, int i2) {
        AppWidgetHostView appWidgetHostView;
        synchronized (this.mViews) {
            appWidgetHostView = this.mViews.get(i);
        }
        if (appWidgetHostView != null) {
            appWidgetHostView.viewDataChanged(i2);
        }
    }

    protected void clearViews() {
        synchronized (this.mViews) {
            this.mViews.clear();
        }
    }
}
