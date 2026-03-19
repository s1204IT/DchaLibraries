package com.mediatek.camera.feature.setting.format;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.Preference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.feature.setting.format.IFormatViewListener;
import java.util.ArrayList;
import java.util.List;

public class FormatSettingView implements ICameraSettingView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FormatSettingView.class.getSimpleName());
    private Activity mContext;
    private boolean mEnabled;
    private List<String> mEntryValues = new ArrayList();
    private IFormatViewListener.OnItemClickListener mOnItemClickListener = new IFormatViewListener.OnItemClickListener() {
        @Override
        public void onItemClick(String str) {
            FormatSettingView.this.mSelectedValue = str;
            if (FormatSettingView.this.mOnValueChangeListener != null) {
                FormatSettingView.this.mOnValueChangeListener.onValueChanged(str);
            }
        }
    };
    private IFormatViewListener.OnValueChangeListener mOnValueChangeListener;
    private String mSelectedValue;
    private Preference mSelfTimerPreference;
    private FormatSelector mSelfTimerSelector;

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        preferenceFragment.addPreferencesFromResource(R.xml.format_preference);
        this.mContext = preferenceFragment.getActivity();
        if (this.mSelfTimerSelector == null) {
            this.mSelfTimerSelector = new FormatSelector();
            this.mSelfTimerSelector.setOnItemClickListener(this.mOnItemClickListener);
        }
        this.mSelfTimerPreference = (Preference) preferenceFragment.findPreference("key_format");
        this.mSelfTimerPreference.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mSelfTimerPreference.setId(R.id.format_setting);
        this.mSelfTimerPreference.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.self_timer_content_description));
        this.mSelfTimerPreference.setSummary(getSummary());
        this.mSelfTimerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                FormatSettingView.this.mSelfTimerSelector.setValue(FormatSettingView.this.mSelectedValue);
                FormatSettingView.this.mSelfTimerSelector.setEntryValues(FormatSettingView.this.mEntryValues);
                FragmentTransaction fragmentTransactionBeginTransaction = FormatSettingView.this.mContext.getFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.addToBackStack(null);
                fragmentTransactionBeginTransaction.replace(R.id.setting_container, FormatSettingView.this.mSelfTimerSelector, "self_timer_selector").commit();
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

    public void setOnValueChangeListener(IFormatViewListener.OnValueChangeListener onValueChangeListener) {
        this.mOnValueChangeListener = onValueChangeListener;
    }

    public void setValue(String str) {
        this.mSelectedValue = str;
    }

    public void setEntryValues(List<String> list) {
        this.mEntryValues = list;
    }

    private String getSummary() {
        if (Format.FORMAT_JPEG.equals(this.mSelectedValue)) {
            return this.mContext.getString(R.string.format_entry_0);
        }
        if (Format.FORMAT_HEIF.equals(this.mSelectedValue)) {
            return this.mContext.getString(R.string.format_entry_1);
        }
        return this.mContext.getString(R.string.format_entry_0);
    }
}
