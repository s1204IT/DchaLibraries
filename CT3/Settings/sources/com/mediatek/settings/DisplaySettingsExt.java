package com.mediatek.settings;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.display.ScreenZoomPreference;
import com.mediatek.hdmi.IMtkHdmiManager;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IDisplaySettingsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.IStatusBarPlmnDisplayExt;
import com.mediatek.settings.fuelgauge.PowerUsageExts;

public class DisplaySettingsExt implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private IDisplaySettingsExt displayExt;
    private SwitchPreference mAodSwitchPrf;
    private Preference mClearMotion;
    private Context mContext;
    private ISettingsMiscExt mExt;
    private IMtkHdmiManager mHdmiManager;
    private Preference mHdmiSettings;
    private Preference mMiraVision;
    private IStatusBarPlmnDisplayExt mPlmnName;
    private DropDownPreference mRotatePreference;
    private Preference mScreenTimeoutPreference;
    private Intent mMiraIntent = new Intent("com.android.settings.MIRA_VISION");
    private RotationPolicy.RotationPolicyListener mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
        public void onChange() {
            if (DisplaySettingsExt.this.mRotatePreference == null) {
                return;
            }
            DisplaySettingsExt.this.mRotatePreference.setValueIndex(RotationPolicy.isRotationLocked(DisplaySettingsExt.this.mContext) ? 1 : 0);
        }
    };

    public DisplaySettingsExt(Context context) {
        Log.d("mediatek.DisplaySettings", "DisplaySettingsExt");
        this.mContext = context;
    }

    private Preference createPreference(int type, int titleRes, String key) {
        Preference preference = null;
        switch (type) {
            case DefaultWfcSettingsExt.RESUME:
                preference = new Preference(this.mContext);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                preference = new CheckBoxPreference(this.mContext);
                preference.setOnPreferenceClickListener(this);
                break;
            case DefaultWfcSettingsExt.CREATE:
                preference = new ListPreference(this.mContext);
                preference.setOnPreferenceClickListener(this);
                break;
        }
        preference.setKey(key);
        preference.setTitle(titleRes);
        return preference;
    }

    private void initPreference(PreferenceScreen screen) {
        this.mClearMotion = createPreference(0, R.string.clear_motion_title, "clearMotion");
        this.mClearMotion.setOrder(-100);
        this.mClearMotion.setSummary(R.string.clear_motion_summary);
        if (FeatureOption.MTK_CLEARMOTION_SUPPORT) {
            screen.addPreference(this.mClearMotion);
        }
        this.mMiraVision = createPreference(0, R.string.mira_vision_title, "mira_vision");
        this.mMiraVision.setSummary(R.string.mira_vision_summary);
        this.mMiraVision.setOrder(-99);
        this.mHdmiManager = IMtkHdmiManager.Stub.asInterface(ServiceManager.getService("mtkhdmi"));
        if (this.mHdmiManager != null) {
            this.mHdmiSettings = createPreference(0, R.string.hdmi_settings, "hdmi_settings");
            this.mHdmiSettings.setSummary(R.string.hdmi_settings_summary);
            this.mHdmiSettings.setFragment("com.mediatek.hdmi.HdmiSettings");
            try {
                String hdmi = this.mContext.getString(R.string.hdmi_replace_hdmi);
                if (this.mHdmiManager.getDisplayType() == 2) {
                    String mhl = this.mContext.getString(R.string.hdmi_replace_mhl);
                    this.mHdmiSettings.setTitle(this.mHdmiSettings.getTitle().toString().replaceAll(hdmi, mhl));
                    this.mHdmiSettings.setSummary(this.mHdmiSettings.getSummary().toString().replaceAll(hdmi, mhl));
                } else if (this.mHdmiManager.getDisplayType() == 3) {
                    String slimport = this.mContext.getString(R.string.slimport_replace_hdmi);
                    this.mHdmiSettings.setTitle(this.mHdmiSettings.getTitle().toString().replaceAll(hdmi, slimport));
                    this.mHdmiSettings.setSummary(this.mHdmiSettings.getSummary().toString().replaceAll(hdmi, slimport));
                }
            } catch (RemoteException e) {
                Log.d("mediatek.DisplaySettings", "getDisplayType RemoteException");
            }
            this.mHdmiSettings.setOrder(-98);
            screen.addPreference(this.mHdmiSettings);
        }
        this.mScreenTimeoutPreference = screen.findPreference("screen_timeout");
        this.mExt = UtilsExt.getMiscPlugin(this.mContext);
        this.mExt.setTimeoutPrefTitle(this.mScreenTimeoutPreference);
        this.mPlmnName = UtilsExt.getStatusBarPlmnPlugin(this.mContext);
        this.mPlmnName.createCheckBox(screen, 1000);
        if (screen.findPreference("screensaver") != null && FeatureOption.MTK_GMO_RAM_OPTIMIZE) {
            screen.removePreference(screen.findPreference("screensaver"));
        }
        this.displayExt = UtilsExt.getDisplaySettingsPlugin(this.mContext);
        this.displayExt.addPreference(this.mContext, screen);
        this.displayExt.removePreference(this.mContext, screen);
        Log.d("mediatek.DisplaySettings", "removePreference ");
        if (FeatureOption.MTK_AOD_SUPPORT) {
            this.mAodSwitchPrf = new SwitchPreference(this.mContext);
            this.mAodSwitchPrf.setKey("always_on_display");
            this.mAodSwitchPrf.setTitle(R.string.aod_title);
            this.mAodSwitchPrf.setSummary(R.string.aod_summary);
            this.mAodSwitchPrf.setChecked(Settings.Secure.getInt(this.mContext.getContentResolver(), "doze_enabled", 1) != 0);
            this.mAodSwitchPrf.setOnPreferenceChangeListener(this);
            screen.addPreference(this.mAodSwitchPrf);
            Preference dozePreference = screen.findPreference("doze");
            if (dozePreference != null) {
                screen.removePreference(dozePreference);
            }
        }
        if (!FeatureOption.MTK_RESOLUTION_SWITCH_SUPPORT) {
            return;
        }
        ScreenZoomPreference screenZoom = (ScreenZoomPreference) screen.findPreference("screen_zoom");
        screenZoom.setEnabled(PowerUsageExts.isResulotionSwitchEnabled(this.mContext) ? false : true);
    }

    public void onCreate(PreferenceScreen screen) {
        if (FeatureOption.MTK_A1_FEATURE) {
            return;
        }
        Log.d("mediatek.DisplaySettings", "onCreate");
        initPreference(screen);
    }

    public boolean isCustomPrefPresent() {
        return this.displayExt.isCustomPrefPresent();
    }

    public String[] getFontEntries(String[] defaultStr) {
        return this.displayExt.getFontEntries(defaultStr);
    }

    public String[] getFontEntryValues(String[] defaultStr) {
        return this.displayExt.getFontEntryValues(defaultStr);
    }

    public void onResume() {
        if (FeatureOption.MTK_A1_FEATURE) {
            return;
        }
        Log.d("mediatek.DisplaySettings", "onResume of DisplaySettings");
        if (!RotationPolicy.isRotationSupported(this.mContext)) {
            return;
        }
        RotationPolicy.registerRotationPolicyListener(this.mContext, this.mRotationPolicyListener);
    }

    public void onPause() {
        if (FeatureOption.MTK_A1_FEATURE) {
            return;
        }
        Log.d("mediatek.DisplaySettings", "onPause of DisplaySettings");
        if (!RotationPolicy.isRotationSupported(this.mContext)) {
            return;
        }
        RotationPolicy.unregisterRotationPolicyListener(this.mContext, this.mRotationPolicyListener);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (FeatureOption.MTK_A1_FEATURE) {
            return false;
        }
        if (preference == this.mClearMotion) {
            Intent intent = new Intent();
            intent.setClass(this.mContext, ClearMotionSettings.class);
            this.mContext.startActivity(intent);
            return true;
        }
        if (preference == this.mMiraVision) {
            this.mContext.startActivity(this.mMiraIntent);
            return true;
        }
        return true;
    }

    public void setRotatePreference(DropDownPreference preference) {
        this.mRotatePreference = preference;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == this.mAodSwitchPrf) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(this.mContext.getContentResolver(), "doze_enabled", enabled ? 1 : 0);
        }
        return true;
    }
}
