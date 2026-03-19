package com.mediatek.camera.feature.setting.whitebalance;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.Preference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.feature.setting.whitebalance.WhiteBalanceSelector;
import java.util.ArrayList;
import java.util.List;

public class WhiteBalanceSettingView implements ICameraSettingView, WhiteBalanceSelector.OnItemClickListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(WhiteBalanceSettingView.class.getSimpleName());
    private Activity mActivity;
    private boolean mEnabled;
    private String mKey;
    private OnValueChangeListener mListener;
    private Preference mPreference;
    private WhiteBalanceSelector mSelector;
    private List<String> mOriginalEntries = new ArrayList();
    private List<String> mOriginalEntryValues = new ArrayList();
    private List<Integer> mOriginalIcons = new ArrayList();
    private List<String> mEntries = new ArrayList();
    private List<String> mEntryValues = new ArrayList();
    private List<Integer> mIcons = new ArrayList();
    private String mSummary = null;
    private String mSelectedValue = null;

    public interface OnValueChangeListener {
        void onValueChanged(String str);
    }

    @Override
    public void onItemClick(String str) {
        setValue(str);
        if (this.mListener != null) {
            this.mListener.onValueChanged(str);
        }
    }

    public WhiteBalanceSettingView(Activity activity, String str) {
        this.mKey = null;
        this.mActivity = activity;
        this.mKey = str;
        String[] stringArray = this.mActivity.getResources().getStringArray(R.array.white_balance_entries);
        String[] stringArray2 = this.mActivity.getResources().getStringArray(R.array.white_balance_entryvalues);
        TypedArray typedArrayObtainTypedArray = this.mActivity.getResources().obtainTypedArray(R.array.white_balance_icons);
        int length = typedArrayObtainTypedArray.length();
        int[] iArr = new int[length];
        for (int i = 0; i < length; i++) {
            iArr[i] = typedArrayObtainTypedArray.getResourceId(i, 0);
        }
        typedArrayObtainTypedArray.recycle();
        for (String str2 : stringArray) {
            this.mOriginalEntries.add(str2);
        }
        for (String str3 : stringArray2) {
            this.mOriginalEntryValues.add(str3);
        }
        for (int i2 : iArr) {
            this.mOriginalIcons.add(Integer.valueOf(i2));
        }
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        if (this.mSelector == null) {
            this.mSelector = new WhiteBalanceSelector();
            this.mSelector.setOnItemClickListener(this);
        }
        preferenceFragment.addPreferencesFromResource(R.xml.white_balance_preference);
        this.mPreference = (Preference) preferenceFragment.getPreferenceManager().findPreference(this.mKey);
        this.mPreference.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPreference.setId(R.id.white_balance_setting);
        this.mPreference.setContentDescription(preferenceFragment.getActivity().getResources().getString(R.string.pref_camera_whitebalance_content_description));
        this.mPreference.setSummary(this.mSummary);
        this.mPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                WhiteBalanceSettingView.this.mSelector.setSelectedValue(WhiteBalanceSettingView.this.mSelectedValue);
                WhiteBalanceSettingView.this.mSelector.setEntriesAndEntryValues(WhiteBalanceSettingView.this.mEntries, WhiteBalanceSettingView.this.mEntryValues, WhiteBalanceSettingView.this.mIcons);
                FragmentTransaction fragmentTransactionBeginTransaction = WhiteBalanceSettingView.this.mActivity.getFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.addToBackStack(null);
                fragmentTransactionBeginTransaction.replace(R.id.setting_container, WhiteBalanceSettingView.this.mSelector, "white_balance_selector").commit();
                return true;
            }
        });
        this.mPreference.setEnabled(this.mEnabled);
    }

    @Override
    public void refreshView() {
        if (this.mPreference != null) {
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

    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        this.mListener = onValueChangeListener;
    }

    public void setEntryValues(List<String> list) {
        this.mEntries.clear();
        this.mEntryValues.clear();
        this.mIcons.clear();
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
                        this.mIcons.add(this.mOriginalIcons.get(i));
                        break;
                    }
                }
            }
        }
    }

    public void setValue(String str) {
        this.mSelectedValue = str;
        int iIndexOf = this.mEntryValues.indexOf(this.mSelectedValue);
        if (iIndexOf >= 0) {
            this.mSummary = this.mEntries.get(iIndexOf);
        }
    }
}
