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
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(myComponentName(context));
        SearchWidgetState[] searchWidgetStateArr = new SearchWidgetState[appWidgetIds.length];
        for (int i = 0; i < appWidgetIds.length; i++) {
            searchWidgetStateArr[i] = getSearchWidgetState(context, appWidgetIds[i]);
        }
        return searchWidgetStateArr;
    }

    public static void updateSearchWidgets(Context context) {
        for (SearchWidgetState searchWidgetState : getSearchWidgetStates(context)) {
            searchWidgetState.updateWidget(context, AppWidgetManager.getInstance(context));
        }
    }

    private static ComponentName myComponentName(Context context) {
        String packageName = context.getPackageName();
        return new ComponentName(packageName, packageName + ".SearchWidgetProvider");
    }

    private static Intent createQsbActivityIntent(Context context, String str, Bundle bundle) {
        Intent intent = new Intent(str);
        intent.setPackage(context.getPackageName());
        intent.setFlags(337641472);
        intent.putExtra("app_data", bundle);
        return intent;
    }

    private static SearchWidgetState getSearchWidgetState(Context context, int i) {
        SearchWidgetState searchWidgetState = new SearchWidgetState(i);
        Bundle bundle = new Bundle();
        bundle.putString("source", "launcher-widget");
        searchWidgetState.setQueryTextViewIntent(createQsbActivityIntent(context, "android.search.action.GLOBAL_SEARCH", bundle));
        searchWidgetState.setVoiceSearchIntent(getVoiceSearchIntent(context, bundle));
        return searchWidgetState;
    }

    private static Intent getVoiceSearchIntent(Context context, Bundle bundle) {
        return QsbApplication.get(context).getVoiceSearch().createVoiceWebSearchIntent(bundle);
    }

    private static class SearchWidgetState {
        private final int mAppWidgetId;
        private Intent mQueryTextViewIntent;
        private Intent mVoiceSearchIntent;

        public SearchWidgetState(int i) {
            this.mAppWidgetId = i;
        }

        public void setQueryTextViewIntent(Intent intent) {
            this.mQueryTextViewIntent = intent;
        }

        public void setVoiceSearchIntent(Intent intent) {
            this.mVoiceSearchIntent = intent;
        }

        public void updateWidget(Context context, AppWidgetManager appWidgetManager) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.search_widget);
            setOnClickActivityIntent(context, remoteViews, R.id.search_widget_text, this.mQueryTextViewIntent);
            if (this.mVoiceSearchIntent != null) {
                setOnClickActivityIntent(context, remoteViews, R.id.search_widget_voice_btn, this.mVoiceSearchIntent);
                remoteViews.setViewVisibility(R.id.search_widget_voice_btn, 0);
            } else {
                remoteViews.setViewVisibility(R.id.search_widget_voice_btn, 8);
            }
            appWidgetManager.updateAppWidget(this.mAppWidgetId, remoteViews);
        }

        private void setOnClickActivityIntent(Context context, RemoteViews remoteViews, int i, Intent intent) {
            intent.setPackage(context.getPackageName());
            remoteViews.setOnClickPendingIntent(i, PendingIntent.getActivity(context, 0, intent, 0));
        }
    }
}
