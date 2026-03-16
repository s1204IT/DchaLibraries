package com.android.quicksearchbox;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

public class SearchWidgetProvider extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!"android.appwidget.action.APPWIDGET_ENABLED".equals(action) && "android.appwidget.action.APPWIDGET_UPDATE".equals(action)) {
            updateSearchWidgets(context);
        }
    }

    private static SearchWidgetState[] getSearchWidgetStates(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(myComponentName(context));
        SearchWidgetState[] states = new SearchWidgetState[appWidgetIds.length];
        for (int i = 0; i < appWidgetIds.length; i++) {
            states[i] = getSearchWidgetState(context, appWidgetIds[i]);
        }
        return states;
    }

    public static void updateSearchWidgets(Context context) {
        SearchWidgetState[] states = getSearchWidgetStates(context);
        for (SearchWidgetState state : states) {
            state.updateWidget(context, AppWidgetManager.getInstance(context));
        }
    }

    private static ComponentName myComponentName(Context context) {
        String pkg = context.getPackageName();
        String cls = pkg + ".SearchWidgetProvider";
        return new ComponentName(pkg, cls);
    }

    private static Intent createQsbActivityIntent(Context context, String action, Bundle widgetAppData) {
        Intent qsbIntent = new Intent(action);
        qsbIntent.setPackage(context.getPackageName());
        qsbIntent.setFlags(337641472);
        qsbIntent.putExtra("app_data", widgetAppData);
        return qsbIntent;
    }

    private static SearchWidgetState getSearchWidgetState(Context context, int appWidgetId) {
        SearchWidgetState state = new SearchWidgetState(appWidgetId);
        Bundle widgetAppData = new Bundle();
        widgetAppData.putString("source", "launcher-widget");
        Intent qsbIntent = createQsbActivityIntent(context, "android.search.action.GLOBAL_SEARCH", widgetAppData);
        state.setQueryTextViewIntent(qsbIntent);
        Intent voiceSearchIntent = getVoiceSearchIntent(context, widgetAppData);
        state.setVoiceSearchIntent(voiceSearchIntent);
        return state;
    }

    private static Intent getVoiceSearchIntent(Context context, Bundle widgetAppData) {
        VoiceSearch voiceSearch = QsbApplication.get(context).getVoiceSearch();
        return voiceSearch.createVoiceWebSearchIntent(widgetAppData);
    }

    private static class SearchWidgetState {
        private final int mAppWidgetId;
        private Intent mQueryTextViewIntent;
        private Intent mVoiceSearchIntent;

        public SearchWidgetState(int appWidgetId) {
            this.mAppWidgetId = appWidgetId;
        }

        public void setQueryTextViewIntent(Intent queryTextViewIntent) {
            this.mQueryTextViewIntent = queryTextViewIntent;
        }

        public void setVoiceSearchIntent(Intent voiceSearchIntent) {
            this.mVoiceSearchIntent = voiceSearchIntent;
        }

        public void updateWidget(Context context, AppWidgetManager appWidgetMgr) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.search_widget);
            setOnClickActivityIntent(context, views, R.id.search_widget_text, this.mQueryTextViewIntent);
            if (this.mVoiceSearchIntent != null) {
                setOnClickActivityIntent(context, views, R.id.search_widget_voice_btn, this.mVoiceSearchIntent);
                views.setViewVisibility(R.id.search_widget_voice_btn, 0);
            } else {
                views.setViewVisibility(R.id.search_widget_voice_btn, 8);
            }
            appWidgetMgr.updateAppWidget(this.mAppWidgetId, views);
        }

        private void setOnClickActivityIntent(Context context, RemoteViews views, int viewId, Intent intent) {
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(viewId, pendingIntent);
        }
    }
}
