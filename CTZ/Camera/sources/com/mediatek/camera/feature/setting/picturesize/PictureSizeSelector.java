package com.mediatek.camera.feature.setting.picturesize;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.portability.SystemProperties;
import java.util.ArrayList;
import java.util.List;

public class PictureSizeSelector extends PreferenceFragment {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PictureSizeSelector.class.getSimpleName());
    private static boolean sFilterPictureSize;
    private OnItemClickListener mListener;
    private List<String> mEntryValues = new ArrayList();
    private List<String> mTitleList = new ArrayList();
    private List<String> mSummaryList = new ArrayList();
    private String mSelectedValue = null;
    private Preference.OnPreferenceClickListener mOnPreferenceClickListener = new MyOnPreferenceClickListener();

    public interface OnItemClickListener {
        void onItemClick(String str);
    }

    static {
        sFilterPictureSize = SystemProperties.getInt("vendor.mtk.camera.app.filter.picture.size", 1) == 1;
    }

    @Override
    public void onCreate(Bundle bundle) {
        LogHelper.d(TAG, "[onCreate]");
        super.onCreate(bundle);
        filterValuesOnShown();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getActivity().getResources().getString(R.string.pref_camera_picturesize_title));
        }
        addPreferencesFromResource(R.xml.picturesize_selector_preference);
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

    @Override
    public void onResume() {
        LogHelper.d(TAG, "[onResume]");
        super.onResume();
    }

    @Override
    public void onPause() {
        LogHelper.d(TAG, "[onPause]");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        LogHelper.d(TAG, "[onDestroy]");
        super.onDestroy();
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

    private void filterValuesOnShown() {
        List arrayList;
        PictureSizeHelper.setFilterParameters(0.5d, 3);
        LogHelper.d(TAG, "[filterValuesOnShown] sFilterPictureSize = " + sFilterPictureSize);
        if (sFilterPictureSize) {
            arrayList = PictureSizeHelper.filterSizes(this.mEntryValues);
        } else {
            arrayList = new ArrayList(this.mEntryValues);
        }
        this.mEntryValues.clear();
        this.mTitleList.clear();
        this.mSummaryList.clear();
        for (int i = 0; i < arrayList.size(); i++) {
            String str = (String) arrayList.get(i);
            String pixelsAndRatio = PictureSizeHelper.getPixelsAndRatio(str);
            if (pixelsAndRatio != null) {
                this.mTitleList.add(pixelsAndRatio);
                this.mEntryValues.add(str);
                this.mSummaryList.add(str);
            }
        }
    }

    private class MyOnPreferenceClickListener implements Preference.OnPreferenceClickListener {
        private MyOnPreferenceClickListener() {
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String str = (String) PictureSizeSelector.this.mEntryValues.get(PictureSizeSelector.this.mTitleList.indexOf((String) preference.getTitle()));
            PictureSizeSelector.this.mListener.onItemClick(str);
            PictureSizeSelector.this.mSelectedValue = str;
            PictureSizeSelector.this.getActivity().getFragmentManager().popBackStack();
            return true;
        }
    }
}
