package com.android.launcher3.qsb;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;

public class QsbWidgetHostView extends AppWidgetHostView {

    @ViewDebug.ExportedProperty(category = "launcher")
    private int mPreviousOrientation;

    public QsbWidgetHostView(Context context) {
        super(context);
    }

    @Override
    public void setAppWidget(int i, AppWidgetProviderInfo appWidgetProviderInfo) {
        QsbContainerView.updateDefaultLayout(getContext(), appWidgetProviderInfo);
        super.setAppWidget(i, appWidgetProviderInfo);
    }

    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        this.mPreviousOrientation = getResources().getConfiguration().orientation;
        super.updateAppWidget(remoteViews);
    }

    public boolean isReinflateRequired(int i) {
        return this.mPreviousOrientation != i;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        try {
            super.onLayout(z, i, i2, i3, i4);
        } catch (RuntimeException e) {
            post(new Runnable() {
                @Override
                public void run() {
                    QsbWidgetHostView.this.updateAppWidget(new RemoteViews(QsbWidgetHostView.this.getAppWidgetInfo().provider.getPackageName(), 0));
                }
            });
        }
    }

    @Override
    protected View getErrorView() {
        return getDefaultView(this);
    }

    @Override
    protected View getDefaultView() {
        View defaultView = super.getDefaultView();
        defaultView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Launcher.getLauncher(QsbWidgetHostView.this.getContext()).startSearch("", false, null, true);
            }
        });
        return defaultView;
    }

    public static View getDefaultView(ViewGroup viewGroup) {
        View viewInflate = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.qsb_default_view, viewGroup, false);
        viewInflate.findViewById(R.id.btn_qsb_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Launcher.getLauncher(view.getContext()).startSearch("", false, null, true);
            }
        });
        return viewInflate;
    }
}
