package com.mediatek.camera.feature.setting.shutterspeed;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.util.ArrayList;
import java.util.List;

public class ShutterSpeedSelector extends PreferenceFragment {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ShutterSpeedSelector.class.getSimpleName());
    private OnItemClickListener mListener;
    private List<String> mEntryValues = new ArrayList();
    private List<String> mTitleList = new ArrayList();
    private String mSelectedValue = null;
    private Preference.OnPreferenceClickListener mOnPreferenceClickListener = new MyOnPreferenceClickListener();

    public interface OnItemClickListener {
        void onItemClick(String str);
    }

    @Override
    public void onCreate(Bundle bundle) {
        LogHelper.d(TAG, "[onCreate]");
        super.onCreate(bundle);
        filterValuesOnShown();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getActivity().getResources().getString(R.string.shutter_speed_title));
        }
        addPreferencesFromResource(R.xml.shutter_speed_selector);
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
        this.mEntryValues = list2;
    }

    private class MyOnPreferenceClickListener implements Preference.OnPreferenceClickListener {
        private MyOnPreferenceClickListener() {
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String str = (String) ShutterSpeedSelector.this.mEntryValues.get(ShutterSpeedSelector.this.mTitleList.indexOf((String) preference.getTitle()));
            if (ShutterSpeedSelector.this.mListener != null) {
                ShutterSpeedSelector.this.mListener.onItemClick(str);
            }
            ShutterSpeedSelector.this.mSelectedValue = str;
            ShutterSpeedSelector.this.getActivity().getFragmentManager().popBackStack();
            return true;
        }
    }

    private void filterValuesOnShown() {
        ArrayList arrayList = new ArrayList(this.mEntryValues);
        this.mEntryValues.clear();
        this.mTitleList.clear();
        for (int i = 0; i < arrayList.size(); i++) {
            String str = (String) arrayList.get(i);
            this.mTitleList.add(getTitlePattern(str));
            this.mEntryValues.add(str);
        }
    }

    private String getTitlePattern(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != 52) {
            if (iHashCode != 56) {
                if (iHashCode != 1573) {
                    switch (iHashCode) {
                        case 49:
                            b = !str.equals("1") ? (byte) -1 : (byte) 0;
                            break;
                        case 50:
                            if (str.equals("2")) {
                                b = 1;
                                break;
                            }
                            break;
                    }
                } else if (str.equals("16")) {
                    b = 4;
                }
            } else if (str.equals("8")) {
                b = 3;
            }
        } else if (str.equals("4")) {
            b = 2;
        }
        switch (b) {
            case 0:
                return getActivity().getString(R.string.shutter_speed_entry_1);
            case Camera2Proxy.TEMPLATE_PREVIEW:
                return getActivity().getString(R.string.shutter_speed_entry_2);
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                return getActivity().getString(R.string.shutter_speed_entry_4);
            case Camera2Proxy.TEMPLATE_RECORD:
                return getActivity().getString(R.string.shutter_speed_entry_8);
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                return getActivity().getString(R.string.shutter_speed_entry_16);
            default:
                return getActivity().getString(R.string.shutter_speed_entry_auto);
        }
    }
}
