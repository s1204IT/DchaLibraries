package com.android.providers.contacts;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.providers.contacts.util.PackageUtils;

public class ContactsPackageMonitor {
    private static final boolean VERBOSE_LOGGING = AbstractContactsProvider.VERBOSE_LOGGING;
    private static ContactsPackageMonitor sInstance;
    private Context mContext;
    private final ContactsTaskScheduler mTaskScheduler = new ContactsTaskScheduler(getClass().getSimpleName()) {
        @Override
        public void onPerformTask(int i, Object obj) {
            if (i == 0) {
                ContactsPackageMonitor.this.onPackageChanged((PackageEventArg) obj);
            }
        }
    };

    private static class PackageEventArg {
        final BroadcastReceiver.PendingResult broadcastPendingResult;
        final String packageName;

        private PackageEventArg(String str, BroadcastReceiver.PendingResult pendingResult) {
            this.packageName = str;
            this.broadcastPendingResult = pendingResult;
        }
    }

    private ContactsPackageMonitor(Context context) {
        this.mContext = context;
    }

    private void start() {
        if (VERBOSE_LOGGING) {
            Log.v("ContactsPackageMonitor", "Starting... user=" + Process.myUserHandle().getIdentifier());
        }
        registerReceiver();
    }

    public static synchronized void start(Context context) {
        if (sInstance == null) {
            sInstance = new ContactsPackageMonitor(context);
            sInstance.start();
        }
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getData() == null) {
                    return;
                }
                ContactsPackageMonitor.this.mTaskScheduler.scheduleTask(0, new PackageEventArg(intent.getData().getSchemeSpecificPart(), goAsync()));
            }
        }, intentFilter);
    }

    private void onPackageChanged(PackageEventArg packageEventArg) {
        try {
            String str = packageEventArg.packageName;
            if (TextUtils.isEmpty(str)) {
                Log.w("ContactsPackageMonitor", "Empty package name detected.");
                return;
            }
            if (VERBOSE_LOGGING) {
                Log.d("ContactsPackageMonitor", "onPackageChanged: Scanning package: " + str);
            }
            ContactsProvider2 contactsProvider2 = (ContactsProvider2) getProvider(this.mContext, "com.android.contacts");
            if (contactsProvider2 != null) {
                contactsProvider2.onPackageChanged(str);
            }
            cleanupVoicemail(this.mContext, str);
            if (VERBOSE_LOGGING) {
                Log.v("ContactsPackageMonitor", "Calling PendingResult.finish()...");
            }
            packageEventArg.broadcastPendingResult.finish();
        } finally {
            if (VERBOSE_LOGGING) {
                Log.v("ContactsPackageMonitor", "Calling PendingResult.finish()...");
            }
            packageEventArg.broadcastPendingResult.finish();
        }
    }

    static void cleanupVoicemail(Context context, String str) {
        if (PackageUtils.isPackageInstalled(context, str)) {
            return;
        }
        if (VERBOSE_LOGGING) {
            Log.d("ContactsPackageMonitor", "Cleaning up data for package: " + str);
        }
        VoicemailContentProvider voicemailContentProvider = (VoicemailContentProvider) getProvider(context, "com.android.voicemail");
        if (voicemailContentProvider != null) {
            voicemailContentProvider.removeBySourcePackage(str);
        }
    }

    private static <T extends ContentProvider> T getProvider(Context context, String str) {
        T t = (T) ContentProvider.coerceToLocalContentProvider(context.getContentResolver().acquireProvider(str));
        if (t != null) {
            return t;
        }
        Slog.wtf("ContactsPackageMonitor", "Provider for " + str + " not found");
        return null;
    }
}
