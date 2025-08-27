package com.android.settings.overlay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;

/* loaded from: classes.dex */
public interface SurveyFeatureProvider {
    BroadcastReceiver createAndRegisterReceiver(Activity activity);

    void downloadSurvey(Activity activity, String str, String str2);

    long getSurveyExpirationDate(Context context, String str);

    String getSurveyId(Context context, String str);

    boolean showSurveyIfAvailable(Activity activity, String str);

    static void unregisterReceiver(Activity activity, BroadcastReceiver broadcastReceiver) {
        if (activity == null) {
            throw new IllegalStateException("Cannot unregister receiver if activity is null");
        }
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiver);
    }
}
