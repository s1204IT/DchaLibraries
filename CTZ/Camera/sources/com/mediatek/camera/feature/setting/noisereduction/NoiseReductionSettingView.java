package com.mediatek.camera.feature.setting.noisereduction;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.SwitchPreference;
import com.mediatek.camera.common.setting.ICameraSettingView;

public class NoiseReductionSettingView implements ICameraSettingView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(NoiseReductionSettingView.class.getSimpleName());
    private boolean mChecked;
    private boolean mEnabled;
    private SwitchPreference mPref;
    private OnNoiseReductionViewListener mViewListener;

    interface OnNoiseReductionViewListener {
        boolean onCachedValue();

        void onItemViewClick(boolean z);
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        preferenceFragment.addPreferencesFromResource(R.xml.noise_reduction_preference);
        this.mPref = (SwitchPreference) preferenceFragment.findPreference("key_noise_reduction");
        this.mPref.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPref.setId(R.id.noisereduction_setting);
        this.mPref.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.noise_reduction_content_description));
        this.mPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                boolean zBooleanValue = ((Boolean) obj).booleanValue();
                NoiseReductionSettingView.this.mChecked = zBooleanValue;
                NoiseReductionSettingView.this.mViewListener.onItemViewClick(zBooleanValue);
                return true;
            }
        });
        this.mPref.setChecked(this.mViewListener.onCachedValue());
        this.mPref.setEnabled(this.mEnabled);
    }

    @Override
    public void refreshView() {
        if (this.mPref != null) {
            this.mPref.setChecked(this.mChecked);
        }
    }

    @Override
    public void unloadView() {
        LogHelper.d(TAG, "[unloadView]");
    }

    public void setEnabled(boolean z) {
        this.mEnabled = z;
    }

    @Override
    public boolean isEnabled() {
        return this.mEnabled;
    }

    public void setChecked(boolean z) {
        this.mChecked = z;
        refreshView();
    }
}
