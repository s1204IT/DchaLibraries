package com.android.settings.network;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemProperties;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.AirplaneModeEnabler;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AirplaneModePreferenceController extends TogglePreferenceController implements AirplaneModeEnabler.OnAirplaneModeChangedListener, LifecycleObserver, OnPause, OnResume {
    private static final String EXIT_ECM_RESULT = "exit_ecm_result";
    public static final int REQUEST_CODE_EXIT_ECM = 1;
    private static final String TAG = "AirplaneModePreferenceController";
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private SwitchPreference mAirplaneModePreference;
    private final Context mContext;
    private Fragment mFragment;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public AirplaneModePreferenceController(Context context, String str) {
        super(context, str);
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        this.mContext = context;
        this.mAirplaneModeEnabler = new AirplaneModeEnabler(this.mContext, this.mMetricsFeatureProvider, this);
    }

    public void setFragment(Fragment fragment) {
        this.mFragment = fragment;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if ("airplane_mode".equals(preference.getKey())) {
            String str = SystemProperties.get("ril.cdma.inecmmode", "false");
            boolean zIsAdminUser = UserManager.get(this.mContext).isAdminUser();
            StringBuilder sb = new StringBuilder();
            sb.append("Click airplane mode, ECM=");
            sb.append(str);
            sb.append(", isAdmin=");
            sb.append(zIsAdminUser);
            sb.append(", fragment=");
            sb.append(this.mFragment == null ? "null" : this.mFragment);
            Log.d(TAG, sb.toString());
            if (str != null && str.contains("true") && zIsAdminUser) {
                if (this.mFragment != null) {
                    this.mFragment.startActivityForResult(new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", (Uri) null), 1);
                }
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mAirplaneModePreference = (SwitchPreference) preferenceScreen.findPreference(getPreferenceKey());
        }
    }

    public static boolean isAvailable(Context context) {
        return context.getResources().getBoolean(R.bool.config_show_toggle_airplane) && !context.getPackageManager().hasSystemFeature("android.software.leanback");
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "toggle_airplane");
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailable(this.mContext) ? 0 : 2;
    }

    @Override
    public void onResume() {
        if (isAvailable()) {
            this.mAirplaneModeEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        if (isAvailable()) {
            this.mAirplaneModeEnabler.pause();
        }
    }

    public boolean onActivityResult(int i, int i2, Intent intent) {
        if (i != 1) {
            return false;
        }
        Boolean boolValueOf = Boolean.valueOf(intent != null ? intent.getBooleanExtra(EXIT_ECM_RESULT, false) : false);
        StringBuilder sb = new StringBuilder();
        sb.append("Exit ECM, result=");
        sb.append(boolValueOf);
        sb.append(", data=");
        Object obj = intent;
        if (intent == null) {
            obj = "null";
        }
        sb.append(obj);
        Log.d(TAG, sb.toString());
        if (this.mAirplaneModePreference != null && this.mAirplaneModeEnabler != null) {
            Log.d(TAG, "Exit ECM, checked=" + this.mAirplaneModePreference.isChecked());
            this.mAirplaneModeEnabler.setAirplaneModeInECM(boolValueOf.booleanValue(), this.mAirplaneModePreference.isChecked());
        }
        return true;
    }

    @Override
    public boolean isChecked() {
        return this.mAirplaneModeEnabler.isAirplaneModeOn();
    }

    @Override
    public boolean setChecked(boolean z) {
        if (isChecked() == z) {
            return false;
        }
        this.mAirplaneModeEnabler.setAirplaneMode(z);
        return true;
    }

    @Override
    public void onAirplaneModeChanged(boolean z) {
        if (this.mAirplaneModePreference != null) {
            this.mAirplaneModePreference.setChecked(z);
        }
    }
}
