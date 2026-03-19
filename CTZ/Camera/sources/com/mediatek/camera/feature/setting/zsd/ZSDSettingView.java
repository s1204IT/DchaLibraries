package com.mediatek.camera.feature.setting.zsd;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.KeyEvent;
import com.mediatek.camera.R;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.SwitchPreference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.common.utils.CameraUtil;

public class ZSDSettingView implements ICameraSettingView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ZSDSettingView.class.getSimpleName());
    private boolean mChecked;
    private boolean mEnabled;
    private String mKey;
    private OnZsdClickListener mListener;
    private SwitchPreference mPref;

    public interface OnZsdClickListener {
        void onZsdClicked(boolean z);
    }

    public ZSDSettingView(String str) {
        this.mKey = str;
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        preferenceFragment.addPreferencesFromResource(R.xml.zsd_preference);
        this.mPref = (SwitchPreference) preferenceFragment.findPreference(this.mKey);
        this.mPref.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPref.setId(R.id.zsd_setting);
        this.mPref.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.zsd_content_description));
        this.mPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                boolean zBooleanValue = ((Boolean) obj).booleanValue();
                ZSDSettingView.this.mChecked = zBooleanValue;
                ZSDSettingView.this.mListener.onZsdClicked(zBooleanValue);
                return true;
            }
        });
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
        LogHelper.d(TAG, "[unloadView]");
    }

    public void setEnabled(boolean z) {
        this.mEnabled = z;
    }

    @Override
    public boolean isEnabled() {
        return this.mEnabled;
    }

    public void setZsdOnClickListener(OnZsdClickListener onZsdClickListener) {
        this.mListener = onZsdClickListener;
    }

    public void setChecked(boolean z) {
        this.mChecked = z;
    }

    public IApp.KeyEventListener getKeyEventListener() {
        return new IApp.KeyEventListener() {
            @Override
            public boolean onKeyDown(int i, KeyEvent keyEvent) {
                if ((i != 32 && i != 33) || !CameraUtil.isSpecialKeyCodeEnabled()) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean onKeyUp(int i, KeyEvent keyEvent) {
                if (!CameraUtil.isSpecialKeyCodeEnabled()) {
                    return false;
                }
                if (i != 32 && i != 33) {
                    return false;
                }
                if (ZSDSettingView.this.mPref == null) {
                    LogHelper.e(ZSDSettingView.TAG, "onKeyUp mPref  of zsd is null");
                    return false;
                }
                LogHelper.d(ZSDSettingView.TAG, "onKeyUp mPref of zsd is " + ZSDSettingView.this.mPref.isEnabled());
                if (i == 32 && ZSDSettingView.this.mPref.isEnabled()) {
                    ZSDSettingView.this.mChecked = true;
                    ZSDSettingView.this.mListener.onZsdClicked(true);
                } else if (i == 33 && ZSDSettingView.this.mPref.isEnabled()) {
                    ZSDSettingView.this.mChecked = false;
                    ZSDSettingView.this.mListener.onZsdClicked(false);
                }
                return true;
            }
        };
    }
}
