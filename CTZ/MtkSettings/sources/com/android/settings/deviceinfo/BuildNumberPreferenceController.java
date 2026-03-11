package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDeviceInfoSettingsExt;

public class BuildNumberPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnResume {
    private final Activity mActivity;
    private RestrictedLockUtils.EnforcedAdmin mDebuggingFeaturesDisallowedAdmin;
    private boolean mDebuggingFeaturesDisallowedBySystem;
    private int mDevHitCountdown;
    private Toast mDevHitToast;
    private IDeviceInfoSettingsExt mExt;
    private final Fragment mFragment;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private boolean mProcessingLastDevHit;
    private final UserManager mUm;

    public BuildNumberPreferenceController(Context context, Activity activity, Fragment fragment, Lifecycle lifecycle) {
        super(context);
        this.mActivity = activity;
        this.mFragment = fragment;
        this.mUm = (UserManager) context.getSystemService("user");
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        this.mExt = UtilsExt.getDeviceInfoSettingsExt(this.mActivity);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        Preference preferenceFindPreference = preferenceScreen.findPreference("build_number");
        if (preferenceFindPreference != null) {
            try {
                preferenceFindPreference.setSummary(BidiFormatter.getInstance().unicodeWrap(Build.DISPLAY));
                preferenceFindPreference.setEnabled(true);
                this.mExt.updateSummary(preferenceFindPreference, Build.DISPLAY, this.mContext.getString(R.string.device_info_default));
            } catch (Exception e) {
                preferenceFindPreference.setSummary(R.string.device_info_default);
            }
        }
    }

    @Override
    public String getPreferenceKey() {
        return "build_number";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onResume() {
        this.mDebuggingFeaturesDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_debugging_features", UserHandle.myUserId());
        this.mDebuggingFeaturesDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_debugging_features", UserHandle.myUserId());
        this.mDevHitCountdown = DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(this.mContext) ? -1 : 7;
        this.mDevHitToast = null;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        ComponentName deviceOwnerComponent;
        if (!TextUtils.equals(preference.getKey(), "build_number") || Utils.isMonkeyRunning()) {
            return false;
        }
        if (!this.mUm.isAdminUser() && !this.mUm.isDemoUser()) {
            this.mMetricsFeatureProvider.action(this.mContext, 847, new Pair[0]);
            return false;
        }
        if (!Utils.isDeviceProvisioned(this.mContext)) {
            this.mMetricsFeatureProvider.action(this.mContext, 847, new Pair[0]);
            return false;
        }
        if (this.mUm.hasUserRestriction("no_debugging_features")) {
            if (this.mUm.isDemoUser() && (deviceOwnerComponent = Utils.getDeviceOwnerComponent(this.mContext)) != null) {
                Intent action = new Intent().setPackage(deviceOwnerComponent.getPackageName()).setAction("com.android.settings.action.REQUEST_DEBUG_FEATURES");
                if (this.mContext.getPackageManager().resolveActivity(action, 0) != null) {
                    this.mContext.startActivity(action);
                    return false;
                }
            }
            if (this.mDebuggingFeaturesDisallowedAdmin != null && !this.mDebuggingFeaturesDisallowedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, this.mDebuggingFeaturesDisallowedAdmin);
            }
            this.mMetricsFeatureProvider.action(this.mContext, 847, new Pair[0]);
            return false;
        }
        if (this.mDevHitCountdown > 0) {
            this.mDevHitCountdown--;
            if (this.mDevHitCountdown == 0 && !this.mProcessingLastDevHit) {
                this.mDevHitCountdown++;
                this.mProcessingLastDevHit = new ChooseLockSettingsHelper(this.mActivity, this.mFragment).launchConfirmationActivity(100, this.mContext.getString(R.string.unlock_set_unlock_launch_picker_title));
                if (!this.mProcessingLastDevHit) {
                    enableDevelopmentSettings();
                }
                this.mMetricsFeatureProvider.action(this.mContext, 847, Pair.create(848, Integer.valueOf(!this.mProcessingLastDevHit ? 1 : 0)));
            } else if (this.mDevHitCountdown > 0 && this.mDevHitCountdown < 5) {
                if (this.mDevHitToast != null) {
                    this.mDevHitToast.cancel();
                }
                this.mDevHitToast = Toast.makeText(this.mContext, this.mContext.getResources().getQuantityString(R.plurals.show_dev_countdown, this.mDevHitCountdown, Integer.valueOf(this.mDevHitCountdown)), 0);
                this.mDevHitToast.show();
            }
            this.mMetricsFeatureProvider.action(this.mContext, 847, Pair.create(848, 0));
        } else if (this.mDevHitCountdown < 0) {
            if (this.mDevHitToast != null) {
                this.mDevHitToast.cancel();
            }
            this.mDevHitToast = Toast.makeText(this.mContext, R.string.show_dev_already, 1);
            this.mDevHitToast.show();
            this.mMetricsFeatureProvider.action(this.mContext, 847, Pair.create(848, 1));
        }
        return true;
    }

    public boolean onActivityResult(int i, int i2, Intent intent) {
        if (i != 100) {
            return false;
        }
        if (i2 == -1) {
            enableDevelopmentSettings();
        }
        this.mProcessingLastDevHit = false;
        return true;
    }

    private void enableDevelopmentSettings() {
        this.mDevHitCountdown = 0;
        this.mProcessingLastDevHit = false;
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(this.mContext, true);
        if (this.mDevHitToast != null) {
            this.mDevHitToast.cancel();
        }
        this.mDevHitToast = Toast.makeText(this.mContext, R.string.show_dev_on, 1);
        this.mDevHitToast.show();
    }
}
