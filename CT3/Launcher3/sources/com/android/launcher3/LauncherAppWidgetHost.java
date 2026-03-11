package com.android.launcher3;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.DeadObjectException;
import android.os.TransactionTooLargeException;
import android.view.LayoutInflater;
import android.view.View;
import java.util.ArrayList;

public class LauncherAppWidgetHost extends AppWidgetHost {
    private Launcher mLauncher;
    private final ArrayList<Runnable> mProviderChangeListeners;
    private int mQsbWidgetId;

    public LauncherAppWidgetHost(Launcher launcher, int hostId) {
        super(launcher, hostId);
        this.mProviderChangeListeners = new ArrayList<>();
        this.mQsbWidgetId = -1;
        this.mLauncher = launcher;
    }

    public void setQsbWidgetId(int widgetId) {
        this.mQsbWidgetId = widgetId;
    }

    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        if (appWidgetId == this.mQsbWidgetId) {
            return new LauncherAppWidgetHostView(context) {
                @Override
                protected View getErrorView() {
                    return new View(getContext());
                }
            };
        }
        return new LauncherAppWidgetHostView(context);
    }

    @Override
    public void startListening() {
        try {
            super.startListening();
        } catch (Exception e) {
            if ((e.getCause() instanceof TransactionTooLargeException) || (e.getCause() instanceof DeadObjectException)) {
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void stopListening() {
        super.stopListening();
        clearViews();
    }

    public void addProviderChangeListener(Runnable callback) {
        this.mProviderChangeListeners.add(callback);
    }

    public void removeProviderChangeListener(Runnable callback) {
        this.mProviderChangeListeners.remove(callback);
    }

    @Override
    protected void onProvidersChanged() {
        if (!this.mProviderChangeListeners.isEmpty()) {
            for (Runnable callback : new ArrayList(this.mProviderChangeListeners)) {
                callback.run();
            }
        }
        if (!Utilities.ATLEAST_MARSHMALLOW) {
            return;
        }
        this.mLauncher.notifyWidgetProvidersChanged();
    }

    public AppWidgetHostView createView(Context context, int appWidgetId, LauncherAppWidgetProviderInfo appWidget) {
        if (appWidget.isCustomWidget) {
            LauncherAppWidgetHostView lahv = new LauncherAppWidgetHostView(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
            inflater.inflate(appWidget.initialLayout, lahv);
            lahv.setAppWidget(0, appWidget);
            lahv.updateLastInflationOrientation();
            return lahv;
        }
        return super.createView(context, appWidgetId, (AppWidgetProviderInfo) appWidget);
    }

    @Override
    protected void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidget) {
        LauncherAppWidgetProviderInfo info = LauncherAppWidgetProviderInfo.fromProviderInfo(this.mLauncher, appWidget);
        super.onProviderChanged(appWidgetId, info);
        info.initSpans();
    }
}
