package com.android.systemui.recents;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoader;

public class RecentsAppWidgetHost extends AppWidgetHost {
    RecentsAppWidgetHostCallbacks mCb;
    RecentsConfiguration mConfig;
    Context mContext;
    boolean mIsListening;

    interface RecentsAppWidgetHostCallbacks {
        void refreshSearchWidget();
    }

    public RecentsAppWidgetHost(Context context, int hostId) {
        super(context, hostId);
        this.mContext = context;
        this.mConfig = RecentsConfiguration.getInstance();
    }

    public void startListening(RecentsAppWidgetHostCallbacks cb) {
        this.mCb = cb;
        if (!this.mIsListening) {
            this.mIsListening = true;
            super.startListening();
        }
    }

    @Override
    public void stopListening() {
        if (this.mIsListening) {
            super.stopListening();
        }
        this.mCb = null;
        this.mContext = null;
        this.mIsListening = false;
    }

    @Override
    protected void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidgetInfo) {
        if (this.mCb != null) {
            SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
            if (appWidgetId > -1 && appWidgetId == this.mConfig.searchBarAppWidgetId) {
                ssp.unbindSearchAppWidget(this, appWidgetId);
                this.mConfig.updateSearchBarAppWidgetId(this.mContext, -1);
                this.mCb.refreshSearchWidget();
            }
        }
    }
}
