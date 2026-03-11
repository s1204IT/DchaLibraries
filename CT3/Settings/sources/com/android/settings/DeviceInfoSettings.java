package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.app.PlatLogoActivity;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.mediatek.settings.deviceinfo.DeviceInfoSettingsExts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceInfoSettings extends SettingsPreferenceFragment implements Indexable {
    private RestrictedLockUtils.EnforcedAdmin mDebuggingFeaturesDisallowedAdmin;
    private boolean mDebuggingFeaturesDisallowedBySystem;
    int mDevHitCountdown;
    Toast mDevHitToast;
    private DeviceInfoSettingsExts mExts;
    private RestrictedLockUtils.EnforcedAdmin mFunDisallowedAdmin;
    private boolean mFunDisallowedBySystem;
    long[] mHits = new long[3];
    private UserManager mUm;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.device_info_settings;
            return Arrays.asList(sir);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> keys = new ArrayList<>();
            if (isPropertyMissing("ro.build.selinux")) {
                keys.add("selinux_status");
            }
            if (isPropertyMissing("ro.url.safetylegal")) {
                keys.add("safetylegal");
            }
            if (isPropertyMissing("ro.ril.fccid")) {
                keys.add("fcc_equipment_id");
            }
            if (Utils.isWifiOnly(context)) {
                keys.add("baseband_version");
            }
            if (TextUtils.isEmpty(DeviceInfoUtils.getFeedbackReporterPackage(context))) {
                keys.add("device_feedback");
            }
            UserManager um = UserManager.get(context);
            if (!um.isAdminUser()) {
                keys.add("system_update_settings");
            }
            if (!context.getResources().getBoolean(R.bool.config_additional_system_update_setting_enable)) {
                keys.add("additional_system_update_settings");
            }
            return keys;
        }

        private boolean isPropertyMissing(String property) {
            return SystemProperties.get(property).equals("");
        }
    };

    @Override
    protected int getMetricsCategory() {
        return 40;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    public void onCreate(Bundle icicle) {
        Preference pref;
        super.onCreate(icicle);
        this.mUm = UserManager.get(getActivity());
        addPreferencesFromResource(R.xml.device_info_settings);
        setStringSummary("firmware_version", Build.VERSION.RELEASE);
        findPreference("firmware_version").setEnabled(true);
        String patch = DeviceInfoUtils.getSecurityPatch();
        if (!TextUtils.isEmpty(patch)) {
            setStringSummary("security_patch", patch);
        } else {
            getPreferenceScreen().removePreference(findPreference("security_patch"));
        }
        setValueSummary("baseband_version", "gsm.version.baseband");
        setStringSummary("device_model", Build.MODEL + DeviceInfoUtils.getMsvSuffix());
        setValueSummary("fcc_equipment_id", "ro.ril.fccid");
        setStringSummary("device_model", Build.MODEL);
        setStringSummary("build_number", Build.DISPLAY);
        findPreference("build_number").setEnabled(true);
        findPreference("kernel_version").setSummary(DeviceInfoUtils.getFormattedKernelVersion());
        if (!SELinux.isSELinuxEnabled()) {
            String status = getResources().getString(R.string.selinux_status_disabled);
            setStringSummary("selinux_status", status);
        } else if (!SELinux.isSELinuxEnforced()) {
            String status2 = getResources().getString(R.string.selinux_status_permissive);
            setStringSummary("selinux_status", status2);
        }
        removePreferenceIfPropertyMissing(getPreferenceScreen(), "selinux_status", "ro.build.selinux");
        removePreferenceIfPropertyMissing(getPreferenceScreen(), "safetylegal", "ro.url.safetylegal");
        removePreferenceIfPropertyMissing(getPreferenceScreen(), "fcc_equipment_id", "ro.ril.fccid");
        if (Utils.isWifiOnly(getActivity())) {
            getPreferenceScreen().removePreference(findPreference("baseband_version"));
        }
        if (TextUtils.isEmpty(DeviceInfoUtils.getFeedbackReporterPackage(getActivity()))) {
            getPreferenceScreen().removePreference(findPreference("device_feedback"));
        }
        Activity act = getActivity();
        PreferenceGroup parentPreference = getPreferenceScreen();
        if (this.mUm.isAdminUser()) {
            Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, "system_update_settings", 1);
        } else {
            removePreference("system_update_settings");
        }
        removePreferenceIfBoolFalse("additional_system_update_settings", R.bool.config_additional_system_update_setting_enable);
        removePreferenceIfBoolFalse("manual", R.bool.config_show_manual);
        Intent intent = new Intent("android.settings.SHOW_REGULATORY_INFO");
        if (getPackageManager().queryIntentActivities(intent, 0).isEmpty() && (pref = findPreference("regulatory_info")) != null) {
            getPreferenceScreen().removePreference(pref);
        }
        this.mExts = new DeviceInfoSettingsExts(getActivity(), this);
        this.mExts.initMTKCustomization(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mDevHitCountdown = getActivity().getSharedPreferences("development", 0).getBoolean("show", Build.TYPE.equals("eng")) ? -1 : 7;
        this.mDevHitToast = null;
        this.mFunDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_fun", UserHandle.myUserId());
        this.mFunDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_fun", UserHandle.myUserId());
        this.mDebuggingFeaturesDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_debugging_features", UserHandle.myUserId());
        this.mDebuggingFeaturesDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_debugging_features", UserHandle.myUserId());
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals("firmware_version")) {
            System.arraycopy(this.mHits, 1, this.mHits, 0, this.mHits.length - 1);
            this.mHits[this.mHits.length - 1] = SystemClock.uptimeMillis();
            if (this.mHits[0] >= SystemClock.uptimeMillis() - 500) {
                if (this.mUm.hasUserRestriction("no_fun")) {
                    if (this.mFunDisallowedAdmin != null && !this.mFunDisallowedBySystem) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), this.mFunDisallowedAdmin);
                    }
                    Log.d("DeviceInfoSettings", "Sorry, no fun for you!");
                    return false;
                }
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setClassName("android", PlatLogoActivity.class.getName());
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("DeviceInfoSettings", "Unable to start activity " + intent.toString());
                }
            }
        } else if (preference.getKey().equals("build_number")) {
            if (!this.mUm.isAdminUser() || !Utils.isDeviceProvisioned(getActivity())) {
                return true;
            }
            if (this.mUm.hasUserRestriction("no_debugging_features")) {
                if (this.mDebuggingFeaturesDisallowedAdmin != null && !this.mDebuggingFeaturesDisallowedBySystem) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), this.mDebuggingFeaturesDisallowedAdmin);
                }
                return true;
            }
            if (this.mDevHitCountdown > 0) {
                this.mDevHitCountdown--;
                if (this.mDevHitCountdown == 0) {
                    getActivity().getSharedPreferences("development", 0).edit().putBoolean("show", true).apply();
                    if (this.mDevHitToast != null) {
                        this.mDevHitToast.cancel();
                    }
                    this.mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_on, 1);
                    this.mDevHitToast.show();
                    Index.getInstance(getActivity().getApplicationContext()).updateFromClassNameResource(DevelopmentSettings.class.getName(), true, true);
                } else if (this.mDevHitCountdown > 0 && this.mDevHitCountdown < 5) {
                    if (this.mDevHitToast != null) {
                        this.mDevHitToast.cancel();
                    }
                    this.mDevHitToast = Toast.makeText(getActivity(), getResources().getQuantityString(R.plurals.show_dev_countdown, this.mDevHitCountdown, Integer.valueOf(this.mDevHitCountdown)), 0);
                    this.mDevHitToast.show();
                }
            } else if (this.mDevHitCountdown < 0) {
                if (this.mDevHitToast != null) {
                    this.mDevHitToast.cancel();
                }
                this.mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_already, 1);
                this.mDevHitToast.show();
            }
        } else if (preference.getKey().equals("device_feedback")) {
            sendFeedback();
        } else if (preference.getKey().equals("system_update_settings")) {
            CarrierConfigManager configManager = (CarrierConfigManager) getSystemService("carrier_config");
            PersistableBundle b = configManager.getConfig();
            if (b.getBoolean("ci_action_on_sys_update_bool")) {
                ciActionOnSysUpdate(b);
            }
        }
        this.mExts.onCustomizedPreferenceTreeClick(preference);
        return super.onPreferenceTreeClick(preference);
    }

    private void ciActionOnSysUpdate(PersistableBundle b) {
        String intentStr = b.getString("ci_action_on_sys_update_intent_string");
        if (TextUtils.isEmpty(intentStr)) {
            return;
        }
        String extra = b.getString("ci_action_on_sys_update_extra_string");
        String extraVal = b.getString("ci_action_on_sys_update_extra_val_string");
        Intent intent = new Intent(intentStr);
        if (!TextUtils.isEmpty(extra)) {
            intent.putExtra(extra, extraVal);
        }
        Log.d("DeviceInfoSettings", "ciActionOnSysUpdate: broadcasting intent " + intentStr + " with extra " + extra + ", " + extraVal);
        getActivity().getApplicationContext().sendBroadcast(intent);
    }

    private void removePreferenceIfPropertyMissing(PreferenceGroup preferenceGroup, String preference, String property) {
        if (!SystemProperties.get(property).equals("")) {
            return;
        }
        try {
            preferenceGroup.removePreference(findPreference(preference));
        } catch (RuntimeException e) {
            Log.d("DeviceInfoSettings", "Property '" + property + "' missing and no '" + preference + "' preference");
        }
    }

    private void removePreferenceIfBoolFalse(String preference, int resId) {
        Preference pref;
        if (getResources().getBoolean(resId) || (pref = findPreference(preference)) == null) {
            return;
        }
        getPreferenceScreen().removePreference(pref);
    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(SystemProperties.get(property, getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
        }
    }

    private void sendFeedback() {
        String reporterPackage = DeviceInfoUtils.getFeedbackReporterPackage(getActivity());
        if (TextUtils.isEmpty(reporterPackage)) {
            return;
        }
        Intent intent = new Intent("android.intent.action.BUG_REPORT");
        intent.setPackage(reporterPackage);
        startActivityForResult(intent, 0);
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            this.mSummaryLoader.setSummary(this, this.mContext.getString(R.string.about_summary, Build.VERSION.RELEASE));
        }
    }
}
