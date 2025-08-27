package com.android.settings.search;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Utils;
import java.util.List;
import java.util.Locale;

/* loaded from: classes.dex */
public interface DeviceIndexFeatureProvider {
    public static final String VERSION = Build.FINGERPRINT;
    public static final Locale LANGUAGE = Locale.getDefault();

    void clearIndex(Context context);

    void index(Context context, CharSequence charSequence, Uri uri, Uri uri2, List<String> list);

    boolean isIndexingEnabled();

    default void updateIndex(Context context, boolean z) {
        if (!isIndexingEnabled()) {
            Log.i("DeviceIndex", "Skipping: device index is not enabled");
            return;
        }
        if (!Utils.isDeviceProvisioned(context)) {
            Log.w("DeviceIndex", "Skipping: device is not provisioned");
            return;
        }
        ComponentName componentName = new ComponentName(context.getPackageName(), DeviceIndexUpdateJobService.class.getName());
        try {
            int callingUid = Binder.getCallingUid();
            ServiceInfo serviceInfo = context.getPackageManager().getServiceInfo(componentName, 786432);
            if (serviceInfo == null) {
                Log.w("DeviceIndex", "Skipping: No such service " + componentName);
                return;
            }
            if (serviceInfo.applicationInfo.uid != callingUid) {
                Log.w("DeviceIndex", "Skipping: Uid cannot schedule DeviceIndexUpdate: " + callingUid);
                return;
            }
            if (!z && skipIndex(context)) {
                Log.i("DeviceIndex", "Skipping: already indexed.");
                return;
            }
            setIndexState(context);
            ((JobScheduler) context.getSystemService(JobScheduler.class)).schedule(new JobInfo.Builder(context.getResources().getInteger(R.integer.device_index_update), componentName).setPersisted(true).setMinimumLatency(1000L).setOverrideDeadline(1L).build());
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("DeviceIndex", "Skipping: error finding DeviceIndexUpdateJobService from packageManager");
        }
    }

    static Uri createDeepLink(String str) {
        return new Uri.Builder().scheme("settings").authority("com.android.settings.slices").appendQueryParameter("intent", str).build();
    }

    static boolean skipIndex(Context context) {
        return TextUtils.equals(Settings.Secure.getString(context.getContentResolver(), "settings:language"), LANGUAGE.toString()) && TextUtils.equals(Settings.Secure.getString(context.getContentResolver(), "settings:index_version"), VERSION);
    }

    static void setIndexState(Context context) {
        Settings.Secure.putString(context.getContentResolver(), "settings:index_version", VERSION);
        Settings.Secure.putString(context.getContentResolver(), "settings:language", LANGUAGE.toString());
    }
}
