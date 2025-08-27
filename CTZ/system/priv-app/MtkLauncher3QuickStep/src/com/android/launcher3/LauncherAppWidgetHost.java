package com.android.launcher3;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.BenesseExtension;
import android.os.Handler;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.widget.Toast;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.widget.DeferredAppWidgetHostView;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class LauncherAppWidgetHost extends AppWidgetHost {
    public static final int APPWIDGET_HOST_ID = 1024;
    private static final int FLAG_LISTENING = 1;
    private static final int FLAG_LISTEN_IF_RESUMED = 4;
    private static final int FLAG_RESUMED = 2;
    private final Context mContext;
    private int mFlags;
    private final ArrayList<ProviderChangedListener> mProviderChangeListeners;
    private final SparseArray<LauncherAppWidgetHostView> mViews;

    public interface ProviderChangedListener {
        void notifyWidgetProvidersChanged();
    }

    public LauncherAppWidgetHost(Context context) {
        super(context, 1024);
        this.mProviderChangeListeners = new ArrayList<>();
        this.mViews = new SparseArray<>();
        this.mFlags = 2;
        this.mContext = context;
    }

    /* JADX DEBUG: Method merged with bridge method: onCreateView(Landroid/content/Context;ILandroid/appwidget/AppWidgetProviderInfo;)Landroid/appwidget/AppWidgetHostView; */
    @Override // android.appwidget.AppWidgetHost
    protected LauncherAppWidgetHostView onCreateView(Context context, int i, AppWidgetProviderInfo appWidgetProviderInfo) {
        LauncherAppWidgetHostView launcherAppWidgetHostView = new LauncherAppWidgetHostView(context);
        this.mViews.put(i, launcherAppWidgetHostView);
        return launcherAppWidgetHostView;
    }

    @Override // android.appwidget.AppWidgetHost
    public void startListening() {
        this.mFlags |= 1;
        try {
            super.startListening();
        } catch (Exception e) {
            if (!Utilities.isBinderSizeError(e)) {
                throw new RuntimeException(e);
            }
        }
        for (int size = this.mViews.size() - 1; size >= 0; size--) {
            LauncherAppWidgetHostView launcherAppWidgetHostViewValueAt = this.mViews.valueAt(size);
            if (launcherAppWidgetHostViewValueAt instanceof DeferredAppWidgetHostView) {
                launcherAppWidgetHostViewValueAt.reInflate();
            }
        }
    }

    @Override // android.appwidget.AppWidgetHost
    public void stopListening() {
        this.mFlags &= -2;
        super.stopListening();
    }

    public void setResumed(boolean z) {
        if (z == ((this.mFlags & 2) != 0)) {
            return;
        }
        if (z) {
            this.mFlags |= 2;
            if ((this.mFlags & 4) != 0 && (this.mFlags & 1) == 0) {
                startListening();
                return;
            }
            return;
        }
        this.mFlags &= -3;
    }

    public void setListenIfResumed(boolean z) {
        if (!Utilities.ATLEAST_NOUGAT_MR1) {
            return;
        }
        if (z == ((this.mFlags & 4) != 0)) {
            return;
        }
        if (z) {
            this.mFlags |= 4;
            if ((this.mFlags & 2) != 0) {
                startListening();
                return;
            }
            return;
        }
        this.mFlags &= -5;
        stopListening();
    }

    @Override // android.appwidget.AppWidgetHost
    public int allocateAppWidgetId() {
        return super.allocateAppWidgetId();
    }

    public void addProviderChangeListener(ProviderChangedListener providerChangedListener) {
        this.mProviderChangeListeners.add(providerChangedListener);
    }

    public void removeProviderChangeListener(ProviderChangedListener providerChangedListener) {
        this.mProviderChangeListeners.remove(providerChangedListener);
    }

    @Override // android.appwidget.AppWidgetHost
    protected void onProvidersChanged() {
        if (!this.mProviderChangeListeners.isEmpty()) {
            Iterator it = new ArrayList(this.mProviderChangeListeners).iterator();
            while (it.hasNext()) {
                ((ProviderChangedListener) it.next()).notifyWidgetProvidersChanged();
            }
        }
    }

    public AppWidgetHostView createView(Context context, int i, LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo) {
        if (launcherAppWidgetProviderInfo.isCustomWidget()) {
            LauncherAppWidgetHostView launcherAppWidgetHostView = new LauncherAppWidgetHostView(context);
            ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(launcherAppWidgetProviderInfo.initialLayout, launcherAppWidgetHostView);
            launcherAppWidgetHostView.setAppWidget(0, launcherAppWidgetProviderInfo);
            return launcherAppWidgetHostView;
        }
        if ((this.mFlags & 1) == 0) {
            DeferredAppWidgetHostView deferredAppWidgetHostView = new DeferredAppWidgetHostView(context);
            deferredAppWidgetHostView.setAppWidget(i, launcherAppWidgetProviderInfo);
            this.mViews.put(i, deferredAppWidgetHostView);
            return deferredAppWidgetHostView;
        }
        try {
            return super.createView(context, i, (AppWidgetProviderInfo) launcherAppWidgetProviderInfo);
        } catch (Exception e) {
            if (!Utilities.isBinderSizeError(e)) {
                throw new RuntimeException(e);
            }
            LauncherAppWidgetHostView launcherAppWidgetHostViewOnCreateView = this.mViews.get(i);
            if (launcherAppWidgetHostViewOnCreateView == null) {
                launcherAppWidgetHostViewOnCreateView = onCreateView(this.mContext, i, (AppWidgetProviderInfo) launcherAppWidgetProviderInfo);
            }
            launcherAppWidgetHostViewOnCreateView.setAppWidget(i, launcherAppWidgetProviderInfo);
            launcherAppWidgetHostViewOnCreateView.switchToErrorView();
            return launcherAppWidgetHostViewOnCreateView;
        }
    }

    @Override // android.appwidget.AppWidgetHost
    protected void onProviderChanged(int i, AppWidgetProviderInfo appWidgetProviderInfo) {
        LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfoFromProviderInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(this.mContext, appWidgetProviderInfo);
        super.onProviderChanged(i, launcherAppWidgetProviderInfoFromProviderInfo);
        launcherAppWidgetProviderInfoFromProviderInfo.initSpans(this.mContext);
    }

    @Override // android.appwidget.AppWidgetHost
    public void deleteAppWidgetId(int i) {
        super.deleteAppWidgetId(i);
        this.mViews.remove(i);
    }

    @Override // android.appwidget.AppWidgetHost
    public void clearViews() {
        super.clearViews();
        this.mViews.clear();
    }

    public void startBindFlow(BaseActivity baseActivity, int i, AppWidgetProviderInfo appWidgetProviderInfo, int i2) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        baseActivity.startActivityForResult(new Intent("android.appwidget.action.APPWIDGET_BIND").putExtra(LauncherSettings.Favorites.APPWIDGET_ID, i).putExtra(LauncherSettings.Favorites.APPWIDGET_PROVIDER, appWidgetProviderInfo.provider).putExtra("appWidgetProviderProfile", appWidgetProviderInfo.getProfile()), i2);
    }

    public void startConfigActivity(BaseActivity baseActivity, int i, int i2) {
        try {
            startAppWidgetConfigureActivityForResult(baseActivity, i, 0, i2, null);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(baseActivity, R.string.activity_not_found, 0).show();
            sendActionCancelled(baseActivity, i2);
        }
    }

    private void sendActionCancelled(final BaseActivity baseActivity, final int i) {
        new Handler().post(new Runnable() { // from class: com.android.launcher3.-$$Lambda$LauncherAppWidgetHost$bo_Aea8BPKolyYi4d072d4Fwf-o
            @Override // java.lang.Runnable
            public final void run() {
                baseActivity.onActivityResult(i, 0, null);
            }
        });
    }
}
