package com.mediatek.camera.feature.setting.scenemode;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.List;

public class SceneModeSelector extends PreferenceFragment {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SceneModeSelector.class.getSimpleName());
    private List<String> mEntries;
    private List<String> mEntryValues;
    private List<Integer> mIcons;
    private OnItemClickListener mListener;
    private Preference.OnPreferenceClickListener mOnPreferenceClickListener = new MyOnPreferenceClickListener();
    private String mSelectedValue;

    public interface OnItemClickListener {
        void onItemClick(String str);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getActivity().getResources().getString(R.string.pref_camera_scenemode_title));
        }
        addPreferencesFromResource(R.xml.scene_mode_selector_preference);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int i = 0; i < this.mEntries.size(); i++) {
            RadioPreference radioPreference = new RadioPreference(getActivity());
            if (this.mEntryValues.get(i).equals(this.mSelectedValue)) {
                radioPreference.setChecked(true);
            }
            radioPreference.setTitle(this.mEntries.get(i));
            radioPreference.setIcon(this.mIcons.get(i).intValue());
            radioPreference.setOnPreferenceClickListener(this.mOnPreferenceClickListener);
            preferenceScreen.addPreference(radioPreference);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mListener = onItemClickListener;
    }

    public void setSelectedValue(String str) {
        this.mSelectedValue = str;
    }

    public void setEntriesAndEntryValues(List<String> list, List<String> list2, List<Integer> list3) {
        this.mEntries = list;
        this.mEntryValues = list2;
        this.mIcons = list3;
    }

    private class MyOnPreferenceClickListener implements Preference.OnPreferenceClickListener {
        private MyOnPreferenceClickListener() {
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String str = (String) SceneModeSelector.this.mEntryValues.get(SceneModeSelector.this.mEntries.indexOf((String) preference.getTitle()));
            SceneModeSelector.this.mListener.onItemClick(str);
            SceneModeSelector.this.mSelectedValue = str;
            SceneModeSelector.this.getActivity().getFragmentManager().popBackStack();
            return true;
        }
    }
}
