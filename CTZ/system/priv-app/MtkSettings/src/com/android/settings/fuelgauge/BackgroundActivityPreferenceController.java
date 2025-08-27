package com.android.settings.fuelgauge;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipDialogFragment;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;

/* loaded from: classes.dex */
public class BackgroundActivityPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    static final String KEY_BACKGROUND_ACTIVITY = "background_activity";
    private final AppOpsManager mAppOpsManager;
    BatteryUtils mBatteryUtils;
    DevicePolicyManager mDpm;
    private InstrumentedPreferenceFragment mFragment;
    private PowerWhitelistBackend mPowerWhitelistBackend;
    private String mTargetPackage;
    private final int mUid;
    private final UserManager mUserManager;

    public BackgroundActivityPreferenceController(Context context, InstrumentedPreferenceFragment instrumentedPreferenceFragment, int i, String str) {
        this(context, instrumentedPreferenceFragment, i, str, PowerWhitelistBackend.getInstance(context));
    }

    BackgroundActivityPreferenceController(Context context, InstrumentedPreferenceFragment instrumentedPreferenceFragment, int i, String str, PowerWhitelistBackend powerWhitelistBackend) {
        super(context);
        this.mPowerWhitelistBackend = powerWhitelistBackend;
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mDpm = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mUid = i;
        this.mFragment = instrumentedPreferenceFragment;
        this.mTargetPackage = str;
        this.mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        int iCheckOpNoThrow = this.mAppOpsManager.checkOpNoThrow(70, this.mUid, this.mTargetPackage);
        if (this.mPowerWhitelistBackend.isWhitelisted(this.mTargetPackage) || iCheckOpNoThrow == 2 || Utils.isProfileOrDeviceOwner(this.mUserManager, this.mDpm, this.mTargetPackage)) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
        }
        updateSummary(preference);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mTargetPackage != null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return KEY_BACKGROUND_ACTIVITY;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_BACKGROUND_ACTIVITY.equals(preference.getKey())) {
            boolean z = true;
            if (this.mAppOpsManager.checkOpNoThrow(70, this.mUid, this.mTargetPackage) != 1) {
                z = false;
            }
            showDialog(z);
        }
        return false;
    }

    public void updateSummary(Preference preference) {
        if (this.mPowerWhitelistBackend.isWhitelisted(this.mTargetPackage)) {
            preference.setSummary(R.string.background_activity_summary_whitelisted);
            return;
        }
        int iCheckOpNoThrow = this.mAppOpsManager.checkOpNoThrow(70, this.mUid, this.mTargetPackage);
        if (iCheckOpNoThrow == 2) {
            preference.setSummary(R.string.background_activity_summary_disabled);
        } else {
            preference.setSummary(iCheckOpNoThrow == 1 ? R.string.restricted_true_label : R.string.restricted_false_label);
        }
    }

    void showDialog(boolean z) {
        BatteryTip restrictAppTip;
        AppInfo appInfoBuild = new AppInfo.Builder().setUid(this.mUid).setPackageName(this.mTargetPackage).build();
        if (z) {
            restrictAppTip = new UnrestrictAppTip(0, appInfoBuild);
        } else {
            restrictAppTip = new RestrictAppTip(0, appInfoBuild);
        }
        BatteryTipDialogFragment batteryTipDialogFragmentNewInstance = BatteryTipDialogFragment.newInstance(restrictAppTip, this.mFragment.getMetricsCategory());
        batteryTipDialogFragmentNewInstance.setTargetFragment(this.mFragment, 0);
        batteryTipDialogFragmentNewInstance.show(this.mFragment.getFragmentManager(), "BgActivityPrefContr");
    }
}
