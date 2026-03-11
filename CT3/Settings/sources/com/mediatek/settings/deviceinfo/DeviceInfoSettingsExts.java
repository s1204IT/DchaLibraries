package com.mediatek.settings.deviceinfo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDeviceInfoSettingsExt;
import java.util.List;
import java.util.Locale;

public class DeviceInfoSettingsExts {
    private Activity mActivity;
    private IDeviceInfoSettingsExt mExt;
    private PreferenceFragment mPreferenceFragment;
    private Resources mRes;
    private PreferenceScreen mRootContainer;

    public DeviceInfoSettingsExts(Activity activity, PreferenceFragment fragment) {
        this.mActivity = activity;
        this.mPreferenceFragment = fragment;
        this.mRootContainer = fragment.getPreferenceScreen();
        this.mExt = UtilsExt.getDeviceInfoSettingsPlugin(activity);
        this.mRes = this.mActivity.getResources();
    }

    public void initMTKCustomization(PreferenceGroup parentPreference) {
        boolean z;
        boolean z2;
        boolean isOwner = UserHandle.myUserId() == 0;
        if (FeatureOption.MTK_SYSTEM_UPDATE_SUPPORT || FeatureOption.MTK_MDM_FUMO) {
            z = true;
        } else {
            z = FeatureOption.MTK_FOTA_ENTRY;
        }
        if (FeatureOption.MTK_MDM_SCOMO) {
            z2 = true;
        } else {
            z2 = FeatureOption.MTK_SCOMO_ENTRY;
        }
        Log.d("DeviceInfoSettings", "isOwner : " + isOwner + " isSystemUpdateSupport : " + z + " isSoftwareUpdateSupport : " + z2);
        if (!isOwner || !z) {
            removePreference(findPreference("mtk_system_update"));
        } else {
            Preference pref = findPreference("system_update_settings");
            if (pref != null) {
                removePreference(pref);
                Log.d("DeviceInfoSettings", "reomve the google default OTA entrance for system updates");
            }
            updateTitleToActivityLabel("mtk_system_update");
        }
        if (!isOwner || !z2) {
            removePreference(findPreference("mtk_software_update"));
        }
        initBasebandVersion();
        this.mExt.updateSummary(findPreference("device_model"), Build.MODEL, getString(R.string.device_info_default));
        this.mExt.updateSummary(findPreference("build_number"), Build.DISPLAY, getString(R.string.device_info_default));
        this.mExt.addEpushPreference(this.mRootContainer);
        setValueSummary("custom_build_version", "ro.mediatek.version.release");
        if (!FeatureOption.MTK_A1_FEATURE) {
            return;
        }
        removePreference(findPreference("custom_build_version"));
    }

    private void updateTitleToActivityLabel(String key) {
        Preference preference = findPreference(key);
        if (preference == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
        if (FeatureOption.MTK_SYSTEM_UPDATE_SUPPORT) {
            intent.setClassName("com.mediatek.systemupdate", "com.mediatek.systemupdate.MainEntry");
        } else if (FeatureOption.MTK_FOTA_ENTRY) {
            intent.setClassName("com.mediatek.dm", "com.mediatek.dm.fumo.DmEntry");
        } else if (FeatureOption.MTK_MDM_FUMO) {
            intent.setClassName("com.mediatek.mediatekdm", "com.mediatek.mediatekdm.fumo.DmEntry");
        }
        if (intent == null) {
            return;
        }
        PackageManager pm = this.mActivity.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        int listSize = list.size();
        for (int i = 0; i < listSize; i++) {
            ResolveInfo resolveInfo = list.get(i);
            if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                CharSequence title = resolveInfo.loadLabel(pm);
                preference.setTitle(title);
                Log.d("DeviceInfoSettings", "KEY_MTK_SYSTEM_UPDATE : " + title);
                return;
            }
        }
    }

    private void initBasebandVersion() {
        setValueSummary("baseband_version", "gsm.version.baseband");
        if (FeatureOption.MTK_C2K_SUPPORT) {
            Log.d("DeviceInfoSettings", "baseband2 = cdma.version.baseband");
            setValueSummary("baseband_version_2", "cdma.version.baseband");
            updateBasebandTitle();
            return;
        }
        removePreference(findPreference("baseband_version_2"));
    }

    private Preference findPreference(String key) {
        return this.mPreferenceFragment.findPreference(key);
    }

    private void removePreference(Preference preference) {
        this.mRootContainer.removePreference(preference);
    }

    private String getString(int id) {
        return this.mRes.getString(id);
    }

    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(SystemProperties.get(property, getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
        }
    }

    private void updateBasebandTitle() {
        String slot1;
        String slot2;
        String basebandversion = getString(R.string.baseband_version);
        if (FeatureOption.MTK_C2K_SUPPORT) {
            Locale tr = Locale.getDefault();
            slot1 = "GSM " + basebandversion;
            slot2 = "CDMA " + basebandversion;
            if (tr.getCountry().equals(Locale.CHINA.getCountry()) || tr.getCountry().equals(Locale.TAIWAN.getCountry())) {
                slot1 = slot1.replace("GSM ", "GSM");
                slot2 = slot2.replace("CDMA ", "CDMA");
            }
        } else {
            slot1 = basebandversion + getString(R.string.status_imei_slot1).replace(getString(R.string.status_imei), " ");
            slot2 = basebandversion + getString(R.string.status_imei_slot2).replace(getString(R.string.status_imei), " ");
        }
        if (findPreference("baseband_version") != null) {
            findPreference("baseband_version").setTitle(slot1);
            Log.d("DeviceInfoSettings", "set Baseband, solt1 = " + slot1);
        }
        if (findPreference("baseband_version_2") == null) {
            return;
        }
        findPreference("baseband_version_2").setTitle(slot2);
        Log.d("DeviceInfoSettings", "set Baseband, solt2 = " + slot2);
    }

    public void onCustomizedPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals("mtk_system_update")) {
            systemUpdateEntrance(preference);
        } else if (preference.getKey().equals("mtk_software_update")) {
            softwareUpdateEntrance(preference);
        } else {
            if (!preference.getKey().equals("cdma_epush")) {
                return;
            }
            startActivity("com.ctc.epush", "com.ctc.epush.IndexActivity");
        }
    }

    private void systemUpdateEntrance(Preference preference) {
        if (FeatureOption.MTK_SYSTEM_UPDATE_SUPPORT) {
            startActivity("com.mediatek.systemupdate", "com.mediatek.systemupdate.MainEntry");
        } else {
            if (!FeatureOption.MTK_MDM_FUMO && !FeatureOption.MTK_FOTA_ENTRY) {
                return;
            }
            sendBroadcast("com.mediatek.DMSWUPDATE");
        }
    }

    private void softwareUpdateEntrance(Preference preference) {
        if (FeatureOption.MTK_MDM_SCOMO) {
            startActivity("com.mediatek.mediatekdm", "com.mediatek.mediatekdm.scomo.DmScomoActivity");
        } else {
            if (!FeatureOption.MTK_SCOMO_ENTRY) {
                return;
            }
            startActivity("com.mediatek.dm", "com.mediatek.dm.scomo.DmScomoActivity");
        }
    }

    private void startActivity(String className, String activityName) {
        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
        ComponentName cn = new ComponentName(className, activityName);
        intent.setComponent(cn);
        if (this.mActivity.getPackageManager().resolveActivity(intent, 0) != null) {
            this.mActivity.startActivity(intent);
        } else {
            Log.e("DeviceInfoSettings", "Unable to start activity " + intent.toString());
        }
    }

    private void sendBroadcast(String actionName) {
        Intent intent = new Intent();
        intent.setAction(actionName);
        this.mActivity.sendBroadcast(intent);
    }
}
