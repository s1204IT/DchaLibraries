package com.android.settings.dream;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.CandidateInfo;
import java.util.ArrayList;
import java.util.List;

public class WhenToDreamPicker extends RadioButtonPickerFragment {
    private DreamBackend mBackend;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mBackend = DreamBackend.getInstance(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.when_to_dream_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 47;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        String[] strArrEntries = entries();
        String[] strArrKeys = keys();
        ArrayList arrayList = new ArrayList();
        if (strArrEntries == null || strArrEntries.length <= 0) {
            return null;
        }
        if (strArrKeys == null || strArrKeys.length != strArrEntries.length) {
            throw new IllegalArgumentException("Entries and values must be of the same length.");
        }
        for (int i = 0; i < strArrEntries.length; i++) {
            arrayList.add(new WhenToDreamCandidateInfo(strArrEntries[i], strArrKeys[i]));
        }
        return arrayList;
    }

    private String[] entries() {
        return getResources().getStringArray(R.array.when_to_start_screensaver_entries);
    }

    private String[] keys() {
        return getResources().getStringArray(R.array.when_to_start_screensaver_values);
    }

    @Override
    protected String getDefaultKey() {
        return DreamSettings.getKeyFromSetting(this.mBackend.getWhenToDreamSetting());
    }

    @Override
    protected boolean setDefaultKey(String str) {
        this.mBackend.setWhenToDream(DreamSettings.getSettingFromPrefKey(str));
        return true;
    }

    @Override
    protected void onSelectionPerformed(boolean z) {
        super.onSelectionPerformed(z);
        getActivity().finish();
    }

    private final class WhenToDreamCandidateInfo extends CandidateInfo {
        private final String key;
        private final String name;

        WhenToDreamCandidateInfo(String str, String str2) {
            super(true);
            this.name = str;
            this.key = str2;
        }

        @Override
        public CharSequence loadLabel() {
            return this.name;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return this.key;
        }
    }
}
