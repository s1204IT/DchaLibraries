package com.mediatek.camera.feature.setting.antiflicker;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.Preference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.feature.setting.antiflicker.AntiFlickerSelector;
import java.util.ArrayList;
import java.util.List;

public class AntiFlickerSettingView implements ICameraSettingView, AntiFlickerSelector.OnItemClickListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AntiFlickerSettingView.class.getSimpleName());
    private Activity mActivity;
    private AntiFlickerSelector mAntiFlickerSelector;
    private boolean mEnabled;
    private String mKey;
    private OnValueChangeListener mOnValueChangeListener;
    private Preference mPreference;
    private String mSelectedValue;
    private List<String> mOriginalEntries = new ArrayList();
    private List<String> mOriginalEntryValues = new ArrayList();
    private List<String> mEntries = new ArrayList();
    private List<String> mEntryValues = new ArrayList();
    private String mSummary = null;

    public interface OnValueChangeListener {
        void onValueChanged(String str);
    }

    public AntiFlickerSettingView(Activity activity, String str) {
        this.mActivity = activity;
        this.mKey = str;
        String[] stringArray = this.mActivity.getResources().getStringArray(R.array.anti_flicker_entries);
        String[] stringArray2 = this.mActivity.getResources().getStringArray(R.array.anti_flicker_entryvalues);
        for (String str2 : stringArray) {
            this.mOriginalEntries.add(str2);
        }
        for (String str3 : stringArray2) {
            this.mOriginalEntryValues.add(str3);
        }
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        preferenceFragment.addPreferencesFromResource(R.xml.anti_flicker_preference);
        if (this.mAntiFlickerSelector == null) {
            this.mAntiFlickerSelector = new AntiFlickerSelector();
            this.mAntiFlickerSelector.setOnItemClickListener(this);
        }
        this.mPreference = (Preference) preferenceFragment.findPreference(this.mKey);
        this.mPreference.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPreference.setId(R.id.anti_flicker_setting);
        this.mPreference.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.pref_camera_antibanding_content_description));
        this.mPreference.setSummary(this.mSummary);
        this.mPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                AntiFlickerSettingView.this.mAntiFlickerSelector.setValue(AntiFlickerSettingView.this.mSelectedValue);
                AntiFlickerSettingView.this.mAntiFlickerSelector.setEntriesAndEntryValues(AntiFlickerSettingView.this.mEntries, AntiFlickerSettingView.this.mEntryValues);
                FragmentTransaction fragmentTransactionBeginTransaction = AntiFlickerSettingView.this.mActivity.getFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.addToBackStack(null);
                fragmentTransactionBeginTransaction.replace(R.id.setting_container, AntiFlickerSettingView.this.mAntiFlickerSelector, "anti_flicker_selector").commit();
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
