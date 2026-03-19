package android.appwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class AppWidgetProvider extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras;
        int[] intArray;
        String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            Bundle extras2 = intent.getExtras();
            if (extras2 != null && (intArray = extras2.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)) != null && intArray.length > 0) {
                onUpdate(context, AppWidgetManager.getInstance(context), intArray);
                return;
            }
            return;
        }
        if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
            Bundle extras3 = intent.getExtras();
            if (extras3 != null && extras3.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
                onDeleted(context, new int[]{extras3.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)});
                return;
            }
            return;
        }
        if (AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED.equals(action)) {
            Bundle extras4 = intent.getExtras();
            if (extras4 != null && extras4.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID) && extras4.containsKey(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS)) {
                onAppWidgetOptionsChanged(context, AppWidgetManager.getInstance(context), extras4.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID), extras4.getBundle(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS));
                return;
            }
            return;
        }
        if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action)) {
            onEnabled(context);
            return;
        }
        if (AppWidgetManager.ACTION_APPWIDGET_DISABLED.equals(action)) {
            onDisabled(context);
            return;
        }
        if (AppWidgetManager.ACTION_APPWIDGET_RESTORED.equals(action) && (extras = intent.getExtras()) != null) {
            int[] intArray2 = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_OLD_IDS);
            int[] intArray3 = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (intArray2 != null && intArray2.length > 0) {
                onRestored(context, intArray2, intArray3);
                onUpdate(context, AppWidgetManager.getInstance(context), intArray3);
            }
        }
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
    }

    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int i, Bundle bundle) {
    }

    public void onDeleted(Context context, int[] iArr) {
    }

    public void onEnabled(Context context) {
    }

    public void onDisabled(Context context) {
    }

    public void onRestored(Context context, int[] iArr, int[] iArr2) {
    }
}
