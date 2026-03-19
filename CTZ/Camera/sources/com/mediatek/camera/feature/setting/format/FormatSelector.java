package com.mediatek.camera.feature.setting.format;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.feature.setting.format.IFormatViewListener;
import com.mediatek.camera.feature.setting.picturesize.RadioPreference;
import java.util.ArrayList;
import java.util.List;

public class FormatSelector extends PreferenceFragment {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FormatSelector.class.getSimpleName());
    private IFormatViewListener.OnItemClickListener mListener;
    private List<String> mEntryValues = new ArrayList();
    private List<String> mTitleList = new ArrayList();
    private String mSelectedValue = null;
    private Preference.OnPreferenceClickListener mOnPreferenceClickListener = new SelfTimerPreferenceClickListener();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        filterValuesOnShown();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getActivity().getString(R.string.format_title));
        }
        addPreferencesFromResource(R.xml.format_selector_preference);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int i = 0; i < this.mEntryValues.size(); i++) {
            RadioPreference radioPreference = new RadioPreference(getActivity());
            if (this.mEntryValues.get(i).equals(this.mSelectedValue)) {
                radioPreference.setChecked(true);
            }
            radioPreference.setTitle(this.mTitleList.get(i));
            radioPreference.setOnPreferenceClickListener(this.mOnPreferenceClickListener);
            preferenceScreen.addPreference(radioPreference);
        }
    }

    public void setOnItemClickListener(IFormatViewListener.OnItemClickListener onItemClickListener) {
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
        ArrayList arrayList = new ArrayList(this.mEntryValues);
        this.mEntryValues.clear();
        this.mTitleList.clear();
        for (int i = 0; i < arrayList.size(); i++) {
            String str = (String) arrayList.get(i);
            String titlePattern = getTitlePattern(str);
            if (titlePattern != null) {
                this.mTitleList.add(titlePattern);
                this.mEntryValues.add(str);
            }
        }
    }

    private class SelfTimerPreferenceClickListener implements Preference.OnPreferenceClickListener {
        private SelfTimerPreferenceClickListener() {
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String str = (String) FormatSelector.this.mEntryValues.get(FormatSelector.this.mTitleList.indexOf((String) preference.getTitle()));
            FormatSelector.this.mListener.onItemClick(str);
            FormatSelector.this.mSelectedValue = str;
            FormatSelector.this.getActivity().getFragmentManager().popBackStack();
            return true;
        }
    }

    private String getTitlePattern(String str) {
        if (str.equals(Format.FORMAT_JPEG)) {
            return getActivity().getString(R.string.format_entry_0);
        }
        if (str.equals(Format.FORMAT_HEIF)) {
            return getActivity().getString(R.string.format_entry_1);
        }
        return getActivity().getString(R.string.format_entry_0);
    }
}
