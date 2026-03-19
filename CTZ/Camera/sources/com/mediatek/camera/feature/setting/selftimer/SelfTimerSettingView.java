package com.mediatek.camera.feature.setting.selftimer;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.Preference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.feature.setting.selftimer.ISelfTimerViewListener;
import java.util.ArrayList;
import java.util.List;

public class SelfTimerSettingView implements ICameraSettingView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SelfTimerSettingView.class.getSimpleName());
    private Activity mContext;
    private boolean mEnabled;
    private List<String> mEntryValues = new ArrayList();
    private ISelfTimerViewListener.OnItemClickListener mOnItemClickListener = new ISelfTimerViewListener.OnItemClickListener() {
        @Override
        public void onItemClick(String str) {
            SelfTimerSettingView.this.mSelectedValue = str;
            if (SelfTimerSettingView.this.mOnValueChangeListener != null) {
                SelfTimerSettingView.this.mOnValueChangeListener.onValueChanged(str);
            }
        }
    };
    private ISelfTimerViewListener.OnValueChangeListener mOnValueChangeListener;
    private String mSelectedValue;
    private Preference mSelfTimerPreference;
    private SelfTimerSelector mSelfTimerSelector;

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        preferenceFragment.addPreferencesFromResource(R.xml.self_timer_preference);
        this.mContext = preferenceFragment.getActivity();
        if (this.mSelfTimerSelector == null) {
            this.mSelfTimerSelector = new SelfTimerSelector();
            this.mSelfTimerSelector.setOnItemClickListener(this.mOnItemClickListener);
        }
        this.mSelfTimerPreference = (Preference) preferenceFragment.findPreference("key_self_timer");
        this.mSelfTimerPreference.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mSelfTimerPreference.setId(R.id.self_timer_setting);
        this.mSelfTimerPreference.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.self_timer_content_description));
        this.mSelfTimerPreference.setSummary(getSummary());
        this.mSelfTimerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                SelfTimerSettingView.this.mSelfTimerSelector.setValue(SelfTimerSettingView.this.mSelectedValue);
                SelfTimerSettingView.this.mSelfTimerSelector.setEntryValues(SelfTimerSettingView.this.mEntryValues);
                FragmentTransaction fragmentTransactionBeginTransaction = SelfTimerSettingView.this.mContext.getFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.addToBackStack(null);
                fragmentTransactionBeginTransaction.replace(R.id.setting_container, SelfTimerSettingView.this.mSelfTimerSelector, "self_timer_selector").commit();
                return true;
            }
        });
        this.mSelfTimerPreference.setEnabled(this.mEnabled);
    }

    @Override
    public void refreshView() {
        if (this.mSelfTimerPreference != null) {
            LogHelper.d(TAG, "[refreshView]");
            this.mSelfTimerPreference.setSummary(getSummary());
            this.mSelfTimerPreference.setEnabled(this.mEnabled);
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

    public void setOnValueChangeListener(ISelfTimerViewListener.OnValueChangeListener onValueChangeListener) {
        this.mOnValueChangeListener = onValueChangeListener;
    }

    public void setValue(String str) {
        this.mSelectedValue = str;
    }

    public void setEntryValues(List<String> list) {
        this.mEntryValues = list;
    }

    private String getSummary() {
        if ("10".equals(this.mSelectedValue)) {
            return this.mContext.getString(R.string.self_timer_entry_10);
        }
        if ("2".equals(this.mSelectedValue)) {
            return this.mContext.getString(R.string.self_timer_entry_2);
        }
        return this.mContext.getString(R.string.self_timer_entry_off);
    }
}
