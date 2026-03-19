package com.mediatek.camera.feature.setting.dng;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.SwitchPreference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.dng.DngViewCtrl;
import java.util.List;

public class DngSettingView implements ICameraSettingView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(DngSettingView.class.getSimpleName());
    private DngViewCtrl.OnDngSettingViewListener mDngViewListener;
    private boolean mEnabled;
    private List<String> mEntryValues;
    private SwitchPreference mPref;
    private ISettingManager.SettingDevice2Requester mSettingDevice2Requester;

    @Override
    public void loadView(final PreferenceFragment preferenceFragment) {
        LogHelper.d(TAG, "[loadView]");
        preferenceFragment.addPreferencesFromResource(R.xml.dng_preference);
        this.mPref = (SwitchPreference) preferenceFragment.findPreference("key_dng");
        this.mPref.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPref.setId(R.id.dng_setting);
        this.mPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (DngSettingView.this.mSettingDevice2Requester != null && DngSettingView.this.mSettingDevice2Requester.getCurrentCaptureSession() == null) {
                    return false;
                }
                boolean zBooleanValue = ((Boolean) obj).booleanValue();
                if (zBooleanValue) {
                    DngSettingView.this.mPref.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.dng_content_description_on));
                } else {
                    DngSettingView.this.mPref.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.dng_content_description_off));
                }
                DngSettingView.this.mDngViewListener.onItemViewClick(zBooleanValue);
                return true;
            }
        });
        this.mPref.setChecked(this.mDngViewListener.onUpdatedValue());
        this.mPref.setEnabled(this.mEnabled);
        if (this.mDngViewListener.onUpdatedValue()) {
            this.mPref.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.dng_content_description_on));
        } else {
            this.mPref.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.dng_content_description_off));
        }
    }

    @Override
    public void refreshView() {
        if (this.mPref == null) {
            return;
        }
        this.mPref.setChecked(this.mDngViewListener.onUpdatedValue());
        this.mPref.setEnabled(this.mEnabled);
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

    public void setDngViewListener(DngViewCtrl.OnDngSettingViewListener onDngSettingViewListener) {
        this.mDngViewListener = onDngSettingViewListener;
    }

    public void setEntryValue(List<String> list) {
        this.mEntryValues = list;
    }

    public void setSettingRequester(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mSettingDevice2Requester = settingDevice2Requester;
    }
}
