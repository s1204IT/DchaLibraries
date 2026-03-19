package com.android.traceur;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import java.io.File;

public class AtraceService extends IntentService {
    private static String INTENT_ACTION_START_TRACING = "com.android.traceur.START_TRACING";
    private static String INTENT_ACTION_STOP_TRACING = "com.android.traceur.STOP_TRACING";
    private static String INTENT_EXTRA_FILENAME = "filename";
    private static String INTENT_EXTRA_TAGS = "tags";
    private static String INTENT_EXTRA_BUFFER = "buffer";
    private static String INTENT_EXTRA_APPS = "apps";
    private static int TRACE_NOTIFICATION = 1;
    private static int SAVING_TRACE_NOTIFICATION = 2;

    public static void startTracing(Context context, String str, int i, boolean z) {
        Intent intent = new Intent(context, (Class<?>) AtraceService.class);
        intent.setAction(INTENT_ACTION_START_TRACING);
        intent.putExtra(INTENT_EXTRA_TAGS, str);
        intent.putExtra(INTENT_EXTRA_BUFFER, i);
        intent.putExtra(INTENT_EXTRA_APPS, z);
        context.startService(intent);
    }

    public static void stopTracing(Context context) {
        Intent intent = new Intent(context, (Class<?>) AtraceService.class);
        intent.setAction(INTENT_ACTION_STOP_TRACING);
        intent.putExtra(INTENT_EXTRA_FILENAME, AtraceUtils.getOutputFilename());
        context.startService(intent);
    }

    public AtraceService() {
        super("AtraceService");
        setIntentRedelivery(true);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(INTENT_ACTION_START_TRACING)) {
            startTracingInternal(intent.getStringExtra(INTENT_EXTRA_TAGS), intent.getIntExtra(INTENT_EXTRA_BUFFER, Integer.parseInt(getApplicationContext().getString(R.string.default_buffer_size))), intent.getBooleanExtra(INTENT_EXTRA_APPS, false));
        } else if (intent.getAction().equals(INTENT_ACTION_STOP_TRACING)) {
            stopTracingInternal(intent.getStringExtra(INTENT_EXTRA_FILENAME));
        }
    }

    private void startTracingInternal(String str, int i, boolean z) {
        Context applicationContext = getApplicationContext();
        Intent intent = new Intent("com.android.traceur.STOP", null, applicationContext, Receiver.class);
        String string = applicationContext.getString(R.string.trace_is_being_recorded);
        startForeground(TRACE_NOTIFICATION, new Notification.Builder(applicationContext, "system-tracing").setSmallIcon(R.drawable.stat_sys_adb).setContentTitle(string).setTicker(string).setContentText(applicationContext.getString(R.string.tap_to_stop_tracing)).setContentIntent(PendingIntent.getBroadcast(applicationContext, 0, intent, 0)).setOngoing(true).setLocalOnly(true).setColor(getColor(android.R.color.car_colorPrimary)).build());
        if (AtraceUtils.atraceStart(str, i, z)) {
            stopForeground(2);
            return;
        }
        AtraceUtils.atraceStop();
        PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().putBoolean(applicationContext.getString(R.string.pref_key_tracing_on), false).apply();
        QsService.updateTile();
        stopForeground(1);
    }

    private void stopTracingInternal(String str) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
        startForeground(SAVING_TRACE_NOTIFICATION, new Notification.Builder(this, "system-tracing").setSmallIcon(R.drawable.stat_sys_adb).setContentTitle(getString(R.string.saving_trace)).setTicker(getString(R.string.saving_trace)).setLocalOnly(true).setProgress(1, 0, true).setColor(getColor(android.R.color.car_colorPrimary)).build());
        notificationManager.cancel(TRACE_NOTIFICATION);
        File outputFile = AtraceUtils.getOutputFile(str);
        if (AtraceUtils.atraceDump(outputFile)) {
            FileSender.postNotification(getApplicationContext(), outputFile);
        }
        stopForeground(1);
    }
}
