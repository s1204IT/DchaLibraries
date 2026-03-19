package com.mediatek.camera.feature.setting.shutterspeed;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.Preference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.feature.setting.shutterspeed.ShutterSpeedSelector;
import java.util.ArrayList;
import java.util.List;

public class ShutterSpeedSettingView implements ICameraSettingView, ShutterSpeedSelector.OnItemClickListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ShutterSpeedSettingView.class.getSimpleName());
    private Activity mActivity;
    private String mAutoEntry;
    private boolean mEnabled;
    private List<String> mEntries = new ArrayList();
    private List<String> mEntryValues = new ArrayList();
    private String mKey;
    private OnValueChangeListener mListener;
    private Preference mPref;
    private String mSelectedValue;
    private ShutterSpeedSelector mSelector;

    public interface OnValueChangeListener {
        void onValueChanged(String str);
    }

    public ShutterSpeedSettingView(String str, Activity activity) {
        this.mAutoEntry = null;
        this.mKey = str;
        this.mActivity = activity;
        this.mAutoEntry = activity.getResources().getString(R.string.shutter_speed_entry_auto);
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        LogHelper.d(TAG, "[loadView]");
        if (this.mSelector == null) {
            this.mSelector = new ShutterSpeedSelector();
            this.mSelector.setOnItemClickListener(this);
        }
        preferenceFragment.addPreferencesFromResource(R.xml.shutter_speed_preference);
        this.mPref = (Preference) preferenceFragment.findPreference(this.mKey);
        this.mPref.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPref.setId(R.id.shutter_speed_setting);
        this.mPref.setContentDescription(this.mActivity.getResources().getString(R.string.shutter_speed_content_description));
        this.mPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                ShutterSpeedSettingView.this.mSelector.setValue(ShutterSpeedSettingView.this.mSelectedValue);
                ShutterSpeedSettingView.this.mSelector.setEntriesAndEntryValues(ShutterSpeedSettingView.this.mEntries, ShutterSpeedSettingView.this.mEntryValues);
                FragmentTransaction fragmentTransactionBeginTransaction = ShutterSpeedSettingView.this.mActivity.getFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.addToBackStack(null);
                fragmentTransactionBeginTransaction.replace(R.id.setting_container, ShutterSpeedSettingView.this.mSelector, "shutter_speed_selector").commit();
                return true;
            }
        });
        this.mPref.setEnabled(this.mEnabled);
    }

    @Override
    public void refreshView() {
        if (this.mPref != null) {
            this.mPref.setSummary(getSummary());
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
    }

    public void setEntryValues(List<String> list) {
        this.mEntries.clear();
        this.mEntryValues.clear();
        this.mEntries.addAll(list);
        this.mEntryValues.addAll(list);
        this.mEntries.set(0, this.mAutoEntry);
    }

    private String getSummary() {
        if ("1".equals(this.mSelectedValue)) {
            return this.mActivity.getString(R.string.shutter_speed_entry_1);
        }
        if ("2".equals(this.mSelectedValue)) {
            return this.mActivity.getString(R.string.shutter_speed_entry_2);
        }
        if ("4".equals(this.mSelectedValue)) {
            return this.mActivity.getString(R.string.shutter_speed_entry_4);
        }
        if ("8".equals(this.mSelectedValue)) {
            return this.mActivity.getString(R.string.shutter_speed_entry_8);
        }
        if ("16".equals(this.mSelectedValue)) {
            return this.mActivity.getString(R.string.shutter_speed_entry_16);
        }
        return this.mActivity.getString(R.string.shutter_speed_entry_auto);
    }
}
