package com.mediatek.camera.feature.setting.videoquality;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.preference.Preference;
import com.mediatek.camera.common.setting.ICameraSettingView;
import com.mediatek.camera.feature.setting.videoquality.VideoQualitySelector;
import java.util.ArrayList;
import java.util.List;

public class VideoQualitySettingView implements ICameraSettingView, VideoQualitySelector.OnItemClickListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VideoQualitySettingView.class.getSimpleName());
    private Activity mActivity;
    private boolean mEnabled;
    private List<String> mEntryValues = new ArrayList();
    private String mKey;
    private OnValueChangeListener mListener;
    private Preference mPref;
    private VideoQualitySelector mQualitySelector;
    private String mSelectedValue;
    private String mSummary;
    private VideoQuality mVideoQuality;

    public interface OnValueChangeListener {
        void onValueChanged(String str);
    }

    public VideoQualitySettingView(String str, VideoQuality videoQuality) {
        this.mKey = str;
        this.mVideoQuality = videoQuality;
    }

    @Override
    public void loadView(PreferenceFragment preferenceFragment) {
        this.mActivity = preferenceFragment.getActivity();
        if (this.mQualitySelector == null) {
            this.mQualitySelector = new VideoQualitySelector();
            this.mQualitySelector.setOnItemClickListener(this);
        }
        this.mQualitySelector.setActivity(this.mActivity);
        this.mQualitySelector.setCurrentID(Integer.parseInt(this.mVideoQuality.getCameraId()));
        this.mQualitySelector.setValue(this.mSelectedValue);
        this.mQualitySelector.setEntryValues(this.mEntryValues);
        preferenceFragment.addPreferencesFromResource(R.xml.videoquality_preference);
        this.mPref = (Preference) preferenceFragment.findPreference(this.mKey);
        this.mPref.setRootPreference(preferenceFragment.getPreferenceScreen());
        this.mPref.setId(R.id.video_quality_setting);
        this.mPref.setContentDescription(this.mActivity.getResources().getString(R.string.video_quality_content_description));
        this.mPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(android.preference.Preference preference) {
                FragmentTransaction fragmentTransactionBeginTransaction = VideoQualitySettingView.this.mActivity.getFragmentManager().beginTransaction();
                fragmentTransactionBeginTransaction.addToBackStack(null);
                fragmentTransactionBeginTransaction.replace(R.id.setting_container, VideoQualitySettingView.this.mQualitySelector, "video_quality_selector").commit();
                return true;
            }
        });
        this.mPref.setEnabled(this.mEnabled);
        this.mSummary = VideoQualityHelper.getCurrentResolution(Integer.parseInt(this.mVideoQuality.getCameraId()), this.mSelectedValue);
    }

    @Override
    public void refreshView() {
        if (this.mPref != null) {
            this.mPref.setSummary(this.mSummary);
            this.mPref.setEnabled(this.mEnabled);
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

    public void setValue(String str) {
        this.mSelectedValue = str;
    }

    public void setEntryValues(List<String> list) {
        this.mEntryValues = list;
    }

    @Override
    public void onItemClick(String str) {
        this.mSelectedValue = str;
        this.mSummary = VideoQualityHelper.getCurrentResolution(Integer.parseInt(this.mVideoQuality.getCameraId()), str);
        this.mListener.onValueChanged(str);
    }
}
