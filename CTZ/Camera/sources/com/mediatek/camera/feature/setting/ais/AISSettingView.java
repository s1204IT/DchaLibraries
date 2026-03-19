package com.mediatek.camera.feature.setting.ais;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.SwitchPreference;
import com.mediatek.camera.common.setting.ICameraSettingView;

public class AISSettingView implements ICameraSettingView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AISSettingView.class.getSimpleName());
    private OnAisClickListener mAisClickListener;
    private boolean mChecked;
    private boolean mEnabled;
    private SwitchPreference mPref;

    interface OnAisClickListener {
        void onAisClicked(boolean z);
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        preferenceFragment.addPreferencesFromResource(R.xml.ais_preference);
        this.mPref = (SwitchPreference) preferenceFragment.findPreference("key_ais");
        this.mPref.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPref.setId(R.id.ais_setting);
        this.mPref.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.ais_content_description));
        this.mPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                boolean zBooleanValue = ((Boolean) obj).booleanValue();
                AISSettingView.this.mChecked = zBooleanValue;
                AISSettingView.this.mAisClickListener.onAisClicked(zBooleanValue);
                return true;
            }
        });
        this.mPref.setChecked(this.mChecked);
        this.mPref.setEnabled(this.mEnabled);
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
    }

    public void setEnabled(boolean z) {
        this.mEnabled = z;
    }

    @Override
    public boolean isEnabled() {
        return this.mEnabled;
    }

    public void setAisClickListener(OnAisClickListener onAisClickListener) {
        this.mAisClickListener = onAisClickListener;
    }

    public void setChecked(boolean z) {
        this.mChecked = z;
        refreshView();
    }
}
