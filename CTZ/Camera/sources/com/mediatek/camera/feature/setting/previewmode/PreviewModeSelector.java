package com.mediatek.camera.feature.setting.previewmode;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.feature.setting.picturesize.RadioPreference;
import java.util.List;

public class PreviewModeSelector extends PreferenceFragment {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PreviewModeSelector.class.getSimpleName());
    private List<String> mEntries;
    private List<String> mEntryValues;
    private OnItemClickListener mListener;
    private String mSelectedValue = null;
    private Preference.OnPreferenceClickListener mOnPreferenceClickListener = new MyPreferenceClickListener();

    public interface OnItemClickListener {
        void onItemClick(String str);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getActivity().getString(R.string.pref_camera_previewmode_title));
        }
        addPreferencesFromResource(R.xml.previewmode_selector_preference);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int i = 0; i < this.mEntryValues.size(); i++) {
            RadioPreference radioPreference = new RadioPreference(getActivity());
            if (this.mEntryValues.get(i).equals(this.mSelectedValue)) {
                radioPreference.setChecked(true);
            }
            radioPreference.setTitle(this.mEntries.get(i));
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

    public void setEntriesAndEntryValues(List<String> list, List<String> list2) {
        this.mEntries = list;
        this.mEntryValues = list2;
    }

    private class MyPreferenceClickListener implements Preference.OnPreferenceClickListener {
        private MyPreferenceClickListener() {
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String str = (String) PreviewModeSelector.this.mEntryValues.get(PreviewModeSelector.this.mEntries.indexOf((String) preference.getTitle()));
            PreviewModeSelector.this.mListener.onItemClick(str);
            PreviewModeSelector.this.mSelectedValue = str;
            PreviewModeSelector.this.getActivity().getFragmentManager().popBackStack();
            return true;
        }
    }
}
