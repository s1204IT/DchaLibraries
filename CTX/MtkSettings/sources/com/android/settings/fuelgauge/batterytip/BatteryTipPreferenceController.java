package com.android.settings.fuelgauge.batterytip;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.SettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.actions.BatteryTipAction;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SummaryTip;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatteryTipPreferenceController extends BasePreferenceController {
    private static final String KEY_BATTERY_TIPS = "key_battery_tips";
    private static final int REQUEST_ANOMALY_ACTION = 0;
    private static final String TAG = "BatteryTipPreferenceController";
    private BatteryTipListener mBatteryTipListener;
    private Map<String, BatteryTip> mBatteryTipMap;
    private List<BatteryTip> mBatteryTips;
    InstrumentedPreferenceFragment mFragment;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private boolean mNeedUpdate;
    Context mPrefContext;
    PreferenceGroup mPreferenceGroup;
    private SettingsActivity mSettingsActivity;

    public interface BatteryTipListener {
        void onBatteryTipHandled(BatteryTip batteryTip);
    }

    public BatteryTipPreferenceController(Context context, String str) {
        this(context, str, null, null, null);
    }

    public BatteryTipPreferenceController(Context context, String str, SettingsActivity settingsActivity, InstrumentedPreferenceFragment instrumentedPreferenceFragment, BatteryTipListener batteryTipListener) {
        super(context, str);
        this.mBatteryTipListener = batteryTipListener;
        this.mBatteryTipMap = new HashMap();
        this.mFragment = instrumentedPreferenceFragment;
        this.mSettingsActivity = settingsActivity;
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        this.mNeedUpdate = true;
    }

    @Override
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPrefContext = preferenceScreen.getContext();
        this.mPreferenceGroup = (PreferenceGroup) preferenceScreen.findPreference(getPreferenceKey());
        this.mPreferenceGroup.addPreference(new SummaryTip(0, -1L).buildPreference(this.mPrefContext));
    }

    public void updateBatteryTips(List<BatteryTip> list) {
        if (list == null) {
            return;
        }
        if (this.mBatteryTips == null) {
            this.mBatteryTips = list;
        } else {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                this.mBatteryTips.get(i).updateState(list.get(i));
            }
        }
        this.mPreferenceGroup.removeAll();
        int size2 = list.size();
        for (int i2 = 0; i2 < size2; i2++) {
            BatteryTip batteryTip = this.mBatteryTips.get(i2);
            if (batteryTip.getState() != 2) {
                Preference preferenceBuildPreference = batteryTip.buildPreference(this.mPrefContext);
                this.mBatteryTipMap.put(preferenceBuildPreference.getKey(), batteryTip);
                this.mPreferenceGroup.addPreference(preferenceBuildPreference);
                batteryTip.log(this.mContext, this.mMetricsFeatureProvider);
                this.mNeedUpdate = batteryTip.needUpdate();
                return;
            }
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        BatteryTip batteryTip = this.mBatteryTipMap.get(preference.getKey());
        if (batteryTip != null) {
            if (batteryTip.shouldShowDialog()) {
                BatteryTipDialogFragment batteryTipDialogFragmentNewInstance = BatteryTipDialogFragment.newInstance(batteryTip, this.mFragment.getMetricsCategory());
                batteryTipDialogFragmentNewInstance.setTargetFragment(this.mFragment, 0);
                batteryTipDialogFragmentNewInstance.show(this.mFragment.getFragmentManager(), TAG);
                return true;
            }
            BatteryTipAction actionForBatteryTip = BatteryTipUtils.getActionForBatteryTip(batteryTip, this.mSettingsActivity, this.mFragment);
            if (actionForBatteryTip != null) {
                actionForBatteryTip.handlePositiveAction(this.mFragment.getMetricsCategory());
            }
            if (this.mBatteryTipListener != null) {
                this.mBatteryTipListener.onBatteryTipHandled(batteryTip);
                return true;
            }
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    public void restoreInstanceState(Bundle bundle) {
        if (bundle != null) {
            updateBatteryTips(bundle.getParcelableArrayList(KEY_BATTERY_TIPS));
        }
    }

    public void saveInstanceState(Bundle bundle) {
        bundle.putParcelableList(KEY_BATTERY_TIPS, this.mBatteryTips);
    }

    public boolean needUpdate() {
        return this.mNeedUpdate;
    }
}
