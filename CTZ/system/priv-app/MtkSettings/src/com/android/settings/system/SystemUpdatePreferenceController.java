package com.android.settings.system;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemUpdateManager;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/* loaded from: classes.dex */
public class SystemUpdatePreferenceController extends BasePreferenceController {
    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";
    private static final String TAG = "SysUpdatePrefContr";
    private final UserManager mUm;
    private final SystemUpdateManager mUpdateManager;

    public SystemUpdatePreferenceController(Context context) {
        super(context, KEY_SYSTEM_UPDATE_SETTINGS);
        this.mUm = UserManager.get(context);
        this.mUpdateManager = (SystemUpdateManager) context.getSystemService("system_update");
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mContext.getResources().getBoolean(R.bool.config_show_system_update_settings) && this.mUm.isAdminUser()) {
            return 0;
        }
        return 2;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            Utils.updatePreferenceToSpecificActivityOrRemove(this.mContext, preferenceScreen, getPreferenceKey(), 1);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        PersistableBundle config;
        if (TextUtils.equals(getPreferenceKey(), preference.getKey()) && (config = ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfig()) != null && config.getBoolean("ci_action_on_sys_update_bool")) {
            ciActionOnSysUpdate(config);
            return false;
        }
        return false;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        String string = this.mContext.getString(R.string.android_version_summary, Build.VERSION.RELEASE);
        FutureTask futureTask = new FutureTask(new Callable() { // from class: com.android.settings.system.-$$Lambda$SystemUpdatePreferenceController$XHnSEfghEOzLX1wZid9rCEinHuU
            @Override // java.util.concurrent.Callable
            public final Object call() {
                return this.f$0.mUpdateManager.retrieveSystemUpdateInfo();
            }
        });
        try {
            futureTask.run();
            Bundle bundle = (Bundle) futureTask.get();
            switch (bundle.getInt("status")) {
                case 0:
                    Log.d(TAG, "Update statue unknown");
                    break;
                case 1:
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                    return this.mContext.getText(R.string.android_version_pending_update_summary);
                default:
                    return string;
            }
            String string2 = bundle.getString("title");
            return !TextUtils.isEmpty(string2) ? this.mContext.getString(R.string.android_version_summary, string2) : string;
        } catch (InterruptedException | ExecutionException e) {
            Log.w(TAG, "Error getting system update info.");
            return string;
        }
    }

    private void ciActionOnSysUpdate(PersistableBundle persistableBundle) {
        String string = persistableBundle.getString("ci_action_on_sys_update_intent_string");
        if (!TextUtils.isEmpty(string)) {
            String string2 = persistableBundle.getString("ci_action_on_sys_update_extra_string");
            String string3 = persistableBundle.getString("ci_action_on_sys_update_extra_val_string");
            Intent intent = new Intent(string);
            if (!TextUtils.isEmpty(string2)) {
                intent.putExtra(string2, string3);
            }
            Log.d(TAG, "ciActionOnSysUpdate: broadcasting intent " + string + " with extra " + string2 + ", " + string3);
            this.mContext.getApplicationContext().sendBroadcast(intent);
        }
    }
}
