package com.android.settings.development;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.view.IWindowManager;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class AnimatorDurationScalePreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int ANIMATOR_DURATION_SCALE_SELECTOR = 2;
    static final float DEFAULT_VALUE = 1.0f;
    private final String[] mListSummaries;
    private final String[] mListValues;
    private final IWindowManager mWindowManager;

    public AnimatorDurationScalePreferenceController(Context context) {
        super(context);
        this.mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        this.mListValues = context.getResources().getStringArray(R.array.animator_duration_scale_values);
        this.mListSummaries = context.getResources().getStringArray(R.array.animator_duration_scale_entries);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "animator_duration_scale";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) throws NumberFormatException {
        writeAnimationScaleOption(obj);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        updateAnimationScaleValue();
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() throws NumberFormatException {
        super.onDeveloperOptionsSwitchDisabled();
        writeAnimationScaleOption(null);
    }

    private void writeAnimationScaleOption(Object obj) throws NumberFormatException {
        float f;
        if (obj == null) {
            f = DEFAULT_VALUE;
        } else {
            try {
                f = Float.parseFloat(obj.toString());
            } catch (RemoteException e) {
                return;
            }
        }
        this.mWindowManager.setAnimationScale(2, f);
        updateAnimationScaleValue();
    }

    private void updateAnimationScaleValue() {
        try {
            float animationScale = this.mWindowManager.getAnimationScale(2);
            int i = 0;
            int i2 = 0;
            while (true) {
                if (i2 >= this.mListValues.length) {
                    break;
                }
                if (animationScale > Float.parseFloat(this.mListValues[i2])) {
                    i2++;
                } else {
                    i = i2;
                    break;
                }
            }
            ListPreference listPreference = (ListPreference) this.mPreference;
            listPreference.setValue(this.mListValues[i]);
            listPreference.setSummary(this.mListSummaries[i]);
        } catch (RemoteException e) {
        }
    }
}
