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

/* loaded from: classes.dex */
public class VrDisplayPreferencePicker extends RadioButtonPickerFragment {
    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.vr_display_settings;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 921;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected List<VrCandidateInfo> getCandidates() {
        ArrayList arrayList = new ArrayList();
        Context context = getContext();
        arrayList.add(new VrCandidateInfo(context, 0, R.string.display_vr_pref_low_persistence));
        arrayList.add(new VrCandidateInfo(context, 1, R.string.display_vr_pref_off));
        return arrayList;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected String getDefaultKey() {
        return "vr_display_pref_" + Settings.Secure.getIntForUser(getContext().getContentResolver(), "vr_display_mode", 0, this.mUserId);
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
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

        @Override // com.android.settingslib.widget.CandidateInfo
        public CharSequence loadLabel() {
            return this.label;
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public Drawable loadIcon() {
            return null;
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public String getKey() {
            return "vr_display_pref_" + this.value;
        }
    }
}
