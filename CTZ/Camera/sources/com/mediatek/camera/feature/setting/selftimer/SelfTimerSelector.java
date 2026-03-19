package com.mediatek.camera.feature.setting.selftimer;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.feature.setting.picturesize.RadioPreference;
import com.mediatek.camera.feature.setting.selftimer.ISelfTimerViewListener;
import java.util.ArrayList;
import java.util.List;

public class SelfTimerSelector extends PreferenceFragment {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SelfTimerSelector.class.getSimpleName());
    private ISelfTimerViewListener.OnItemClickListener mListener;
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
            toolbar.setTitle(getActivity().getString(R.string.self_timer_title));
        }
        addPreferencesFromResource(R.xml.self_timer_selector_preference);
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

    public void setOnItemClickListener(ISelfTimerViewListener.OnItemClickListener onItemClickListener) {
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
            String str = (String) SelfTimerSelector.this.mEntryValues.get(SelfTimerSelector.this.mTitleList.indexOf((String) preference.getTitle()));
            SelfTimerSelector.this.mListener.onItemClick(str);
            SelfTimerSelector.this.mSelectedValue = str;
            SelfTimerSelector.this.getActivity().getFragmentManager().popBackStack();
            return true;
        }
    }

    private String getTitlePattern(String str) {
        if (str.equals("2")) {
            return getActivity().getString(R.string.self_timer_entry_2);
        }
        if (str.equals("10")) {
            return getActivity().getString(R.string.self_timer_entry_10);
        }
        return getActivity().getString(R.string.self_timer_entry_off);
    }
}
