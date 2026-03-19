package com.mediatek.camera.feature.setting.microphone;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.SwitchPreference;
import com.mediatek.camera.common.setting.ICameraSettingView;

public class MicroPhoneSettingView implements ICameraSettingView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MicroPhoneSettingView.class.getSimpleName());
    private boolean mChecked;
    private boolean mEnabled;
    private SwitchPreference mPref;
    private OnMicroViewListener mViewListener;

    interface OnMicroViewListener {
        boolean onCachedValue();

        void onItemViewClick(boolean z);
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        LogHelper.d(TAG, "[loadView] + ");
        preferenceFragment.addPreferencesFromResource(R.xml.microphone_preference);
        this.mPref = (SwitchPreference) preferenceFragment.findPreference("key_microphone");
        this.mPref.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPref.setId(R.id.microphone_setting);
        this.mPref.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.microphone_content_description));
        this.mPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                boolean zBooleanValue = ((Boolean) obj).booleanValue();
                MicroPhoneSettingView.this.mChecked = zBooleanValue;
                MicroPhoneSettingView.this.mViewListener.onItemViewClick(zBooleanValue);
                return true;
            }
        });
        this.mPref.setChecked(this.mViewListener.onCachedValue());
        this.mPref.setEnabled(this.mEnabled);
        LogHelper.d(TAG, "[loadView] - ");
    }

    @Override
    public void refreshView() {
        if (this.mPref != null) {
            this.mPref.setChecked(this.mChecked);
            this.mPref.setEnabled(this.mEnabled);
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

    public void setMicroViewListener(OnMicroViewListener onMicroViewListener) {
        this.mViewListener = onMicroViewListener;
    }

    public void setChecked(boolean z) {
        this.mChecked = z;
        refreshView();
    }
}
