package com.android.settings.display;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;
import java.util.ArrayList;
import java.util.List;

public class VrDisplayPreferencePicker extends RadioButtonPickerFragment {
    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.vr_display_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 921;
    }

    @Override
    protected List<VrCandidateInfo> getCandidates() {
        ArrayList arrayList = new ArrayList();
        Context context = getContext();
        arrayList.add(new VrCandidateInfo(context, 0, R.string.display_vr_pref_low_persistence));
        arrayList.add(new VrCandidateInfo(context, 1, R.string.display_vr_pref_off));
        return arrayList;
    }

    @Override
    protected String getDefaultKey() {
        return "vr_display_pref_" + Settings.Secure.getIntForUser(getContext().getContentResolver(), "vr_display_mode", 0, this.mUserId);
    }

    @Override
    protected boolean setDefaultKey(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        switch (str) {
        }
        return false;
    }

    static class VrCandidateInfo extends CandidateInfo {
        public final String label;
        public final int value;

        public VrCandidateInfo(Context context, int i, int i2) {
            super(true);
            this.value = i;
            this.label = context.getString(i2);
        }

        @Override
        public CharSequence loadLabel() {
            return this.label;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return "vr_display_pref_" + this.value;
        }
    }
}
