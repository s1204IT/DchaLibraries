package android.appwidget;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.IntentSender;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.widget.RemoteViews;
import com.android.internal.appwidget.IAppWidgetHost;
import com.android.internal.appwidget.IAppWidgetService;
import java.lang.ref.WeakReference;
import java.util.List;

public class AppWidgetHost {
    static final int HANDLE_PROVIDERS_CHANGED = 3;
    static final int HANDLE_PROVIDER_CHANGED = 2;
    static final int HANDLE_UPDATE = 1;
    static final int HANDLE_VIEW_DATA_CHANGED = 4;
    private static final String TAG = "AppWidgetHost";
    static IAppWidgetService sService;
    static final Object sServiceLock = new Object();
    private final Callbacks mCallbacks;
    private String mContextOpPackageName;
    private DisplayMetrics mDisplayMetrics;
    private final Handler mHandler;
    private final int mHostId;
    private RemoteViews.OnClickHandler mOnClickHandler;
    private final SparseArray<AppWidgetHostView> mViews;

    static class Callbacks extends IAppWidgetHost.Stub {
        private final WeakReference<Handler> mWeakHandler;

        public Callbacks(Handler handler) {
            this.mWeakHandler = new WeakReference<>(handler);
        }

        public void updateAppWidget(int appWidgetId, RemoteViews views) {
            if (isLocalBinder() && views != null) {
                views = views.clone();
            }
            Handler handler = this.mWeakHandler.get();
            if (handler == null) {
                return;
            }
            Message msg = handler.obtainMessage(1, appWidgetId, 0, views);
            msg.sendToTarget();
        }

        public void providerChanged(int appWidgetId, AppWidgetProviderInfo info) {
            if (isLocalBinder() && info != null) {
                info = info.m317clone();
            }
            Handler handler = this.mWeakHandler.get();
            if (handler == null) {
                return;
            }
            Message msg = handler.obtainMessage(2, appWidgetId, 0, info);
            msg.sendToTarget();
        }

        public void providersChanged() {
            Handler handler = this.mWeakHandler.get();
            if (handler == null) {
                return;
            }
            handler.obtainMessage(3).sendToTarget();
        }

        public void viewDataChanged(int appWidgetId, int viewId) {
            Handler handler = this.mWeakHandler.get();
            if (handler == null) {
                return;
            }
            Message msg = handler.obtainMessage(4, appWidgetId, viewId);
            msg.sendToTarget();
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
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.d(AppWidgetHost.TAG, "updateAppWidgetView HANDLE_UPDATE ");
                    AppWidgetHost.this.updateAppWidgetView(msg.arg1, (RemoteViews) msg.obj);
                    break;
                case 2:
                    AppWidgetHost.this.onProviderChanged(msg.arg1, (AppWidgetProviderInfo) msg.obj);
                    break;
                case 3:
                    AppWidgetHost.this.onProvidersChanged();
                    break;
                case 4:
                    Log.d(AppWidgetHost.TAG, "viewDataChanged HANDLE_VIEW_DATA_CHANGED ");
                    AppWidgetHost.this.viewDataChanged(msg.arg1, msg.arg2);
                    break;
            }
        }
    }

    public AppWidgetHost(Context context, int hostId) {
        this(context, hostId, null, context.getMainLooper());
    }

    public AppWidgetHost(Context context, int hostId, RemoteViews.OnClickHandler handler, Looper looper) {
        this.mViews = new SparseArray<>();
        Log.d(TAG, "new  AppWidgetHost " + this);
        this.mContextOpPackageName = context.getOpPackageName();
        this.mHostId = hostId;
        this.mOnClickHandler = handler;
        this.mHandler = new UpdateHandler(looper);
        this.mCallbacks = new Callbacks(this.mHandler);
        this.mDisplayMetrics = context.getResources().getDisplayMetrics();
        bindService();
    }

    private static void bindService() {
        synchronized (sServiceLock) {
            if (sService == null) {
                IBinder b = ServiceManager.getService(Context.APPWIDGET_SERVICE);
                sService = IAppWidgetService.Stub.asInterface(b);
            }
        }
    }

    public void startListening() {
        int[] idsToUpdate;
        Log.d(TAG, "startListening " + this);
        synchronized (this.mViews) {
            int N = this.mViews.size();
            idsToUpdate = new int[N];
            for (int i = 0; i < N; i++) {
                idsToUpdate[i] = this.mViews.keyAt(i);
            }
        }
        int[] updatedIds = new int[idsToUpdate.length];
        try {
            List<RemoteViews> updatedViews = sService.startListening(this.mCallbacks, this.mContextOpPackageName, this.mHostId, idsToUpdate, updatedIds).getList();
            int N2 = updatedViews.size();
            for (int i2 = 0; i2 < N2; i2++) {
                updateAppWidgetView(updatedIds[i2], updatedViews.get(i2));
            }
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public void stopListening() {
        Log.d(TAG, "stopListening ");
        try {
            sService.stopListening(this.mContextOpPackageName, this.mHostId);
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public int allocateAppWidgetId() {
        try {
            return sService.allocateAppWidgetId(this.mContextOpPackageName, this.mHostId);
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public final void startAppWidgetConfigureActivityForResult(Activity activity, int appWidgetId, int intentFlags, int requestCode, Bundle options) {
        try {
            IntentSender intentSender = sService.createAppWidgetConfigIntentSender(this.mContextOpPackageName, appWidgetId, intentFlags);
            if (intentSender != null) {
                activity.startIntentSenderForResult(intentSender, requestCode, null, 0, 0, 0, options);
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
        try {
            if (sService == null) {
                bindService();
            }
            return sService.getAppWidgetIdsForHost(this.mContextOpPackageName, this.mHostId);
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public void deleteAppWidgetId(int appWidgetId) {
        Log.d(TAG, "deleteAppWidgetId appWidgetId " + appWidgetId);
        synchronized (this.mViews) {
            this.mViews.remove(appWidgetId);
            try {
                sService.deleteAppWidgetId(this.mContextOpPackageName, appWidgetId);
            } catch (RemoteException e) {
                throw new RuntimeException("system server dead?", e);
            }
        }
    }

    public void deleteHost() {
        Log.d(TAG, "deleteHost");
        try {
            sService.deleteHost(this.mContextOpPackageName, this.mHostId);
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public static void deleteAllHosts() {
        try {
            sService.deleteAllHosts();
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    public final AppWidgetHostView createView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        Log.d(TAG, "createView appWidgetId " + appWidgetId);
        AppWidgetHostView view = onCreateView(context, appWidgetId, appWidget);
        view.setOnClickHandler(this.mOnClickHandler);
        view.setAppWidget(appWidgetId, appWidget);
        synchronized (this.mViews) {
            Log.d(TAG, "createView mViews put " + this);
            this.mViews.put(appWidgetId, view);
        }
        try {
            RemoteViews views = sService.getAppWidgetViews(this.mContextOpPackageName, appWidgetId);
            view.updateAppWidget(views);
            return view;
        } catch (RemoteException e) {
            throw new RuntimeException("system server dead?", e);
        }
    }

    protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        return new AppWidgetHostView(context, this.mOnClickHandler);
    }

    protected void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidget) {
        AppWidgetHostView v;
        Log.d(TAG, "onProviderChanged appWidgetId " + appWidgetId);
        appWidget.minWidth = TypedValue.complexToDimensionPixelSize(appWidget.minWidth, this.mDisplayMetrics);
        appWidget.minHeight = TypedValue.complexToDimensionPixelSize(appWidget.minHeight, this.mDisplayMetrics);
        appWidget.minResizeWidth = TypedValue.complexToDimensionPixelSize(appWidget.minResizeWidth, this.mDisplayMetrics);
        appWidget.minResizeHeight = TypedValue.complexToDimensionPixelSize(appWidget.minResizeHeight, this.mDisplayMetrics);
        synchronized (this.mViews) {
            v = this.mViews.get(appWidgetId);
        }
        if (v == null) {
            return;
        }
        v.resetAppWidget(appWidget);
    }

    protected void onProvidersChanged() {
    }

    void updateAppWidgetView(int appWidgetId, RemoteViews views) {
        AppWidgetHostView v;
        Log.d(TAG, "updateAppWidgetView appWidgetId " + appWidgetId + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this);
        synchronized (this.mViews) {
            v = this.mViews.get(appWidgetId);
        }
        if (v == null) {
            return;
        }
        v.updateAppWidget(views);
    }

    void viewDataChanged(int appWidgetId, int viewId) {
        AppWidgetHostView v;
        Log.d(TAG, "viewDataChanged appWidgetId " + appWidgetId);
        synchronized (this.mViews) {
            Log.d(TAG, "viewDataChanged mViews get ");
            v = this.mViews.get(appWidgetId);
            Log.d(TAG, "viewDataChanged mViews get 1111 " + v);
        }
        if (v != null) {
            Log.d(TAG, "viewDataChanged v != null ");
            v.viewDataChanged(viewId);
        } else {
            Log.d(TAG, "viewDataChanged v == null ");
        }
    }

    protected void clearViews() {
        Log.d(TAG, "clearViews " + this);
        synchronized (this.mViews) {
            this.mViews.clear();
        }
    }
}
