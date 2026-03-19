package com.mediatek.camera.feature.setting.iso;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.Preference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.feature.setting.iso.ISOSelector;
import java.util.ArrayList;
import java.util.List;

public class ISOSettingView implements ICameraSettingView, ISOSelector.OnItemClickListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ISOSettingView.class.getSimpleName());
    private Activity mActivity;
    private String mAutoEntry;
    private boolean mEnabled;
    private List<String> mEntries = new ArrayList();
    private List<String> mEntryValues = new ArrayList();
    private String mKey;
    private OnValueChangeListener mListener;
    private Preference mPref;
    private String mSelectedValue;
    private ISOSelector mSelector;
    private String mSummary;

    public interface OnValueChangeListener {
        void onValueChanged(String str);
    }

    public ISOSettingView(String str, Activity activity) {
        this.mAutoEntry = null;
        this.mKey = str;
        this.mActivity = activity;
        this.mAutoEntry = activity.getResources().getString(R.string.pref_camera_iso_entry_auto);
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        LogHelper.d(TAG, "[loadView]");
        if (this.mSelector == null) {
            this.mSelector = new ISOSelector();
            this.mSelector.setOnItemClickListener(this);
        }
        preferenceFragment.addPreferencesFromResource(R.xml.iso_preference);
        this.mPref = (Preference) preferenceFragment.findPreference(this.mKey);
        this.mPref.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPref.setId(R.id.iso_setting);
        this.mPref.setContentDescription(this.mActivity.getResources().getString(R.string.iso_content_description));
        this.mPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                ISOSettingView.this.mSelector.setValue(ISOSettingView.this.mSelectedValue);
                ISOSettingView.this.mSelector.setEntriesAndEntryValues(ISOSettingView.this.mEntries, ISOSettingView.this.mEntryValues);
                FragmentTransaction fragmentTransactionBeginTransaction = ISOSettingView.this.mActivity.getFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.addToBackStack(null);
                fragmentTransactionBeginTransaction.replace(R.id.setting_container, ISOSettingView.this.mSelector, "iso_selector").commit();
                return true;
            }
        });
        this.mPref.setEnabled(this.mEnabled);
    }

    @Override
    public void refreshView() {
        if (this.mPref != null) {
            LogHelper.d(TAG, "[refreshView], mEntryValues:" + this.mEntryValues);
            this.mPref.setSummary(this.mSummary);
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

    @Override
    public void onItemClick(String str) {
        setValue(str);
        if (this.mListener != null) {
            this.mListener.onValueChanged(str);
        }
    }

    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        this.mListener = onValueChangeListener;
    }

    public void setValue(String str) {
        this.mSelectedValue = str;
        int iIndexOf = this.mEntryValues.indexOf(this.mSelectedValue);
        if (iIndexOf >= 0) {
            this.mSummary = this.mEntries.get(iIndexOf);
        }
    }

    public void setEntryValues(List<String> list) {
        this.mEntries.clear();
        this.mEntryValues.clear();
        this.mEntries.addAll(list);
        this.mEntryValues.addAll(list);
        this.mEntries.set(0, this.mAutoEntry);
    }
}
