package com.mediatek.camera.feature.setting.iso;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.ArrayList;
import java.util.List;

public class ISOSelector extends PreferenceFragment {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ISOSelector.class.getSimpleName());
    private OnItemClickListener mListener;
    private List<String> mEntries = new ArrayList();
    private List<String> mEntryValues = new ArrayList();
    private String mSelectedValue = null;
    private Preference.OnPreferenceClickListener mOnPreferenceClickListener = new MyOnPreferenceClickListener();

    public interface OnItemClickListener {
        void onItemClick(String str);
    }

    @Override
    public void onCreate(Bundle bundle) {
        LogHelper.d(TAG, "[onCreate]");
        super.onCreate(bundle);
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getActivity().getResources().getString(R.string.iso_title));
        }
        addPreferencesFromResource(R.xml.iso_selector_preference);
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

    public void setEntriesAndEntryValues(List<String> list, List<String> list2) {
        this.mEntries = list;
        this.mEntryValues = list2;
    }

    private class MyOnPreferenceClickListener implements Preference.OnPreferenceClickListener {
        private MyOnPreferenceClickListener() {
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String str = (String) ISOSelector.this.mEntryValues.get(ISOSelector.this.mEntries.indexOf((String) preference.getTitle()));
            ISOSelector.this.mListener.onItemClick(str);
            ISOSelector.this.mSelectedValue = str;
            ISOSelector.this.getActivity().getFragmentManager().popBackStack();
            return true;
        }
    }
}
