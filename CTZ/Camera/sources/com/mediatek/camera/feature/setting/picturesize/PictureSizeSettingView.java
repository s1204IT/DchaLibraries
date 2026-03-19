package com.mediatek.camera.feature.setting.picturesize;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.Preference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.feature.setting.picturesize.PictureSizeSelector;
import java.util.ArrayList;
import java.util.List;

public class PictureSizeSettingView implements ICameraSettingView, PictureSizeSelector.OnItemClickListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PictureSizeSettingView.class.getSimpleName());
    private Activity mActivity;
    private boolean mEnabled;
    private List<String> mEntryValues = new ArrayList();
    private String mKey;
    private OnValueChangeListener mListener;
    private Preference mPref;
    private String mSelectedValue;
    private PictureSizeSelector mSizeSelector;
    private String mSummary;

    public interface OnValueChangeListener {
        void onValueChanged(String str);
    }

    public PictureSizeSettingView(String str) {
        this.mKey = str;
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        LogHelper.d(TAG, "[loadView]");
        this.mActivity = preferenceFragment.getActivity();
        if (this.mSizeSelector == null) {
            this.mSizeSelector = new PictureSizeSelector();
            this.mSizeSelector.setOnItemClickListener(this);
        }
        preferenceFragment.addPreferencesFromResource(R.xml.picturesize_preference);
        this.mPref = (Preference) preferenceFragment.findPreference(this.mKey);
        this.mPref.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPref.setId(R.id.picture_size_setting);
        this.mPref.setContentDescription(this.mActivity.getResources().getString(R.string.picture_size_content_description));
        this.mPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                PictureSizeSettingView.this.mSizeSelector.setValue(PictureSizeSettingView.this.mSelectedValue);
                PictureSizeSettingView.this.mSizeSelector.setEntryValues(PictureSizeSettingView.this.mEntryValues);
                FragmentTransaction fragmentTransactionBeginTransaction = PictureSizeSettingView.this.mActivity.getFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.addToBackStack(null);
                fragmentTransactionBeginTransaction.replace(R.id.setting_container, PictureSizeSettingView.this.mSizeSelector, "picture_size_selector").commit();
                return true;
            }
        });
        this.mPref.setEnabled(this.mEnabled);
        if (this.mSelectedValue != null) {
            this.mSummary = PictureSizeHelper.getPixelsAndRatio(this.mSelectedValue);
        }
    }

    @Override
    public void refreshView() {
        if (this.mPref != null) {
            LogHelper.d(TAG, "[refreshView]");
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

    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        this.mListener = onValueChangeListener;
    }

    public void setValue(String str) {
        this.mSelectedValue = str;
    }

    public void setEntryValues(List<String> list) {
        this.mEntryValues = list;
    }

    @Override
    public void onItemClick(String str) {
        this.mSelectedValue = str;
        this.mSummary = PictureSizeHelper.getPixelsAndRatio(str);
        if (this.mListener != null) {
            this.mListener.onValueChanged(str);
        }
    }
}
