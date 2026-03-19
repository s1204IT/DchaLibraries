package com.mediatek.camera.feature.setting.previewmode;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.Preference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.feature.setting.previewmode.PreviewModeSelector;
import java.util.ArrayList;
import java.util.List;

public class PreviewModeSettingView implements ICameraSettingView, PreviewModeSelector.OnItemClickListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PreviewModeSettingView.class.getSimpleName());
    private Activity mActivity;
    private boolean mEnabled;
    private String mKey;
    private OnValueChangeListener mOnValueChangeListener;
    private Preference mPreference;
    private PreviewModeSelector mPreviewModeSelector;
    private String mSelectedValue;
    private List<String> mOriginalEntries = new ArrayList();
    private List<String> mOriginalEntryValues = new ArrayList();
    private List<String> mEntries = new ArrayList();
    private List<String> mEntryValues = new ArrayList();
    private String mSummary = null;

    public interface OnValueChangeListener {
        void onValueChanged(String str);
    }

    public PreviewModeSettingView(Activity activity, String str) {
        this.mActivity = activity;
        this.mKey = str;
        String[] stringArray = this.mActivity.getResources().getStringArray(R.array.preview_mode_entries);
        String[] stringArray2 = this.mActivity.getResources().getStringArray(R.array.preview_mode_entryvalues);
        for (String str2 : stringArray) {
            this.mOriginalEntries.add(str2);
        }
        for (String str3 : stringArray2) {
            this.mOriginalEntryValues.add(str3);
        }
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        preferenceFragment.addPreferencesFromResource(R.xml.previewmode_preference);
        if (this.mPreviewModeSelector == null) {
            this.mPreviewModeSelector = new PreviewModeSelector();
            this.mPreviewModeSelector.setOnItemClickListener(this);
        }
        this.mPreference = (Preference) preferenceFragment.findPreference(this.mKey);
        this.mPreference.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPreference.setId(R.id.preview_mode_setting);
        this.mPreference.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.preview_mode_content_description));
        this.mPreference.setSummary(this.mSummary);
        this.mPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                PreviewModeSettingView.this.mPreviewModeSelector.setValue(PreviewModeSettingView.this.mSelectedValue);
                PreviewModeSettingView.this.mPreviewModeSelector.setEntriesAndEntryValues(PreviewModeSettingView.this.mEntries, PreviewModeSettingView.this.mEntryValues);
                FragmentTransaction fragmentTransactionBeginTransaction = PreviewModeSettingView.this.mActivity.getFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.addToBackStack(null);
                fragmentTransactionBeginTransaction.replace(R.id.setting_container, PreviewModeSettingView.this.mPreviewModeSelector, "preview_mode_selector").commit();
                return true;
            }
        });
        this.mPreference.setEnabled(this.mEnabled);
    }

    @Override
    public void refreshView() {
        if (this.mPreference != null) {
            LogHelper.d(TAG, "[refreshView]");
            this.mPreference.setSummary(this.mSummary);
            this.mPreference.setEnabled(this.mEnabled);
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

    @Override
    public void onItemClick(String str) {
        setValue(str);
        if (this.mOnValueChangeListener != null) {
            this.mOnValueChangeListener.onValueChanged(str);
        }
    }

    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        this.mOnValueChangeListener = onValueChangeListener;
    }

    public void setValue(String str) {
        this.mSelectedValue = str;
        int iIndexOf = this.mEntryValues.indexOf(this.mSelectedValue);
        if (iIndexOf >= 0 && iIndexOf < this.mEntries.size()) {
            this.mSummary = this.mEntries.get(iIndexOf);
        }
    }

    public void setEntryValues(List<String> list) {
        this.mEntries.clear();
        this.mEntryValues.clear();
        for (int i = 0; i < this.mOriginalEntryValues.size(); i++) {
            String str = this.mOriginalEntryValues.get(i);
            int i2 = 0;
            while (true) {
                if (i2 < list.size()) {
                    String str2 = list.get(i2);
                    if (!str2.equals(str)) {
                        i2++;
                    } else {
                        this.mEntryValues.add(str2);
                        this.mEntries.add(this.mOriginalEntries.get(i));
                        break;
                    }
                }
            }
        }
    }
}
