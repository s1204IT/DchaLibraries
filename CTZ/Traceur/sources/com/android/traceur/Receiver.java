package com.android.traceur;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.statusbar.IStatusBarService;
import com.google.android.collect.Sets;
import java.util.Set;
import java.util.TreeMap;

public class Receiver extends BroadcastReceiver {
    private static final Set<String> ATRACE_TAGS = Sets.newArraySet(new String[]{"am", "binder_driver", "camera", "dalvik", "freq", "gfx", "hal", "idle", "input", "irq", "res", "sched", "sync", "view", "wm", "workq"});
    private static final Set<String> ATRACE_TAGS_USER = Sets.newArraySet(new String[]{"am", "binder_driver", "camera", "dalvik", "freq", "gfx", "hal", "idle", "input", "res", "sched", "view", "wm"});
    private static ContentObserver mDeveloperOptionsObserver;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            if (BenesseExtension.getDchaState() != 0) {
                AtraceUtils.clearSavedTraces();
                context.getPackageManager().setApplicationEnabledSetting("com.android.traceur", 2, 0);
                return;
            } else {
                createNotificationChannel(context);
                updateDeveloperOptionsWatcher(context, defaultSharedPreferences.getBoolean(context.getString(R.string.pref_key_quick_setting), false));
                updateTracing(context);
                return;
            }
        }
        if ("com.android.traceur.STOP".equals(intent.getAction())) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            defaultSharedPreferences.edit().putBoolean(context.getString(R.string.pref_key_tracing_on), false).apply();
            updateTracing(context);
            return;
        }
        if (!"com.android.traceur.OPEN".equals(intent.getAction()) || BenesseExtension.getDchaState() != 0) {
            return;
        }
        context.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        context.startActivity(new Intent(context, (Class<?>) MainActivity.class).setFlags(268435456));
    }

    public static void updateTracing(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean z = defaultSharedPreferences.getBoolean(context.getString(R.string.pref_key_tracing_on), false);
        if (z != AtraceUtils.isTracingOn()) {
            if (z) {
                String activeTags = getActiveTags(context, defaultSharedPreferences, true);
                if (!TextUtils.equals(activeTags, getActiveTags(context, defaultSharedPreferences, false))) {
                    postCategoryNotification(context, defaultSharedPreferences);
                }
                AtraceService.startTracing(context, activeTags, Integer.parseInt(defaultSharedPreferences.getString(context.getString(R.string.pref_key_buffer_size), context.getString(R.string.default_buffer_size))), defaultSharedPreferences.getBoolean(context.getString(R.string.pref_key_apps), true));
            } else {
                AtraceService.stopTracing(context);
            }
        }
        context.sendBroadcast(new Intent("com.android.traceur.REFRESH_TAGS"));
        QsService.updateTile();
    }

    public static void updateQuickSettings(Context context) {
        boolean z = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.pref_key_quick_setting), false);
        ComponentName componentName = new ComponentName(context, (Class<?>) QsService.class);
        context.getPackageManager().setComponentEnabledSetting(componentName, z ? 1 : 2, 1);
        IStatusBarService iStatusBarServiceAsInterface = IStatusBarService.Stub.asInterface(ServiceManager.checkService("statusbar"));
        if (iStatusBarServiceAsInterface != null) {
            try {
                if (z) {
                    iStatusBarServiceAsInterface.addTile(componentName);
                } else {
                    iStatusBarServiceAsInterface.remTile(componentName);
                }
            } catch (RemoteException e) {
                Log.e("Traceur", "Failed to modify QS tile for Traceur.", e);
            }
        }
        QsService.updateTile();
        updateDeveloperOptionsWatcher(context, z);
    }

    private static void updateDeveloperOptionsWatcher(final Context context, boolean z) {
        Uri uriFor = Settings.Global.getUriFor("development_settings_enabled");
        if (z) {
            mDeveloperOptionsObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean z2) {
                    super.onChange(z2);
                    if (!(1 == Settings.Global.getInt(context.getContentResolver(), "development_settings_enabled", 0))) {
                        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.pref_key_quick_setting), false).apply();
                        Receiver.updateQuickSettings(context);
                    }
                }
            };
            context.getContentResolver().registerContentObserver(uriFor, false, mDeveloperOptionsObserver);
        } else if (mDeveloperOptionsObserver != null) {
            context.getContentResolver().unregisterContentObserver(mDeveloperOptionsObserver);
            mDeveloperOptionsObserver = null;
        }
    }

    private static void postCategoryNotification(Context context, SharedPreferences sharedPreferences) {
        Intent intent = new Intent(context, (Class<?>) MainActivity.class);
        String string = context.getString(R.string.tracing_categories_unavailable);
        ((NotificationManager) context.getSystemService(NotificationManager.class)).notify(Receiver.class.getName(), 0, new Notification.Builder(context, "system-tracing").setSmallIcon(R.drawable.stat_sys_adb).setContentTitle(string).setTicker(string).setContentText(getActiveUnavailableTags(context, sharedPreferences)).setContentIntent(PendingIntent.getActivity(context, 0, intent, 1342177280)).setAutoCancel(true).setLocalOnly(true).setColor(context.getColor(android.R.color.car_colorPrimary)).build());
    }

    private static void createNotificationChannel(Context context) {
        NotificationChannel notificationChannel = new NotificationChannel("system-tracing", context.getString(R.string.system_tracing), 4);
        notificationChannel.setBypassDnd(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setSound(null, null);
        ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannel(notificationChannel);
    }

    public static String getActiveTags(Context context, SharedPreferences sharedPreferences, boolean z) {
        Set<String> stringSet = sharedPreferences.getStringSet(context.getString(R.string.pref_key_tags), getDefaultTagList());
        StringBuilder sb = new StringBuilder(10 * stringSet.size());
        TreeMap<String, String> treeMapAtraceListCategories = z ? AtraceUtils.atraceListCategories() : null;
        for (String str : stringSet) {
            if (!z || treeMapAtraceListCategories.containsKey(str)) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(str);
            }
        }
        String string = sb.toString();
        Log.v("Traceur", "getActiveTags(onlyAvailable=" + z + ") = \"" + string + "\"");
        return string;
    }

    public static String getActiveUnavailableTags(Context context, SharedPreferences sharedPreferences) {
        Set<String> stringSet = sharedPreferences.getStringSet(context.getString(R.string.pref_key_tags), getDefaultTagList());
        StringBuilder sb = new StringBuilder(10 * stringSet.size());
        TreeMap<String, String> treeMapAtraceListCategories = AtraceUtils.atraceListCategories();
        for (String str : stringSet) {
            if (!treeMapAtraceListCategories.containsKey(str)) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(str);
            }
        }
        String string = sb.toString();
        Log.v("Traceur", "getActiveUnavailableTags() = \"" + string + "\"");
        return string;
    }

    public static Set<String> getDefaultTagList() {
        return Build.TYPE.equals("user") ? ATRACE_TAGS_USER : ATRACE_TAGS;
    }
}
