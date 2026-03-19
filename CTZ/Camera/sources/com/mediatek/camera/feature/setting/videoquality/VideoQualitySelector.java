package com.mediatek.camera.feature.setting.videoquality;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.ArrayList;
import java.util.List;

public class VideoQualitySelector extends PreferenceFragment {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VideoQualitySelector.class.getSimpleName());
    private Activity mActivity;
    private int mCameraID;
    private OnItemClickListener mListener;
    private Preference.OnPreferenceClickListener mOnPreferenceClickListener = new MyOnPreferenceClickListener();
    private List<String> mEntryValues = new ArrayList();
    private List<String> mSummaryList = new ArrayList();
    private List<String> mTitleList = new ArrayList();
    private String mSelectedValue = null;

    public interface OnItemClickListener {
        void onItemClick(String str);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        filterValuesOnShown();
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).setTitle(getActivity().getResources().getString(R.string.video_quality_title));
        addPreferencesFromResource(R.xml.videoquality_selector_preference);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int i = 0; i < this.mEntryValues.size(); i++) {
            RadioPreference radioPreference = new RadioPreference(getActivity());
            if (this.mEntryValues.get(i).equals(this.mSelectedValue)) {
                radioPreference.setChecked(true);
            }
            radioPreference.setTitle(this.mTitleList.get(i));
            radioPreference.setSummary(this.mSummaryList.get(i));
            radioPreference.setOnPreferenceClickListener(this.mOnPreferenceClickListener);
            preferenceScreen.addPreference(radioPreference);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mListener = onItemClickListener;
    }

    public void setValue(String str) {
        this.mSelectedValue = str;
    }

    public void setEntryValues(List<String> list) {
        this.mEntryValues.clear();
        this.mEntryValues.addAll(list);
    }

    public void setCurrentID(int i) {
        this.mCameraID = i;
    }

    public void setActivity(Activity activity) {
        this.mActivity = activity;
    }

    private void filterValuesOnShown() {
        ArrayList arrayList = new ArrayList(this.mEntryValues);
        this.mEntryValues.clear();
        this.mTitleList.clear();
        this.mSummaryList.clear();
        for (int i = 0; i < arrayList.size(); i++) {
            String str = (String) arrayList.get(i);
            String currentResolution = VideoQualityHelper.getCurrentResolution(this.mCameraID, str);
            String qualityTitle = VideoQualityHelper.getQualityTitle(this.mActivity, str, this.mCameraID);
            if (qualityTitle != null) {
                this.mTitleList.add(qualityTitle);
                this.mEntryValues.add(str);
                this.mSummaryList.add(currentResolution);
            }
        }
    }

    private class MyOnPreferenceClickListener implements Preference.OnPreferenceClickListener {
        private MyOnPreferenceClickListener() {
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String str = (String) VideoQualitySelector.this.mEntryValues.get(VideoQualitySelector.this.mTitleList.indexOf((String) preference.getTitle()));
            VideoQualitySelector.this.mListener.onItemClick(str);
            VideoQualitySelector.this.mSelectedValue = str;
            VideoQualitySelector.this.getActivity().getFragmentManager().popBackStack();
            return true;
        }
    }
}
