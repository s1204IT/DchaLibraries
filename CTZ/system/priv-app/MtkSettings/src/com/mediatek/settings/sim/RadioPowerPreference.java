package com.mediatek.settings.sim;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.android.settings.R;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISimManagementExt;

/* loaded from: classes.dex */
public class RadioPowerPreference extends Preference {
    private static final boolean ENG_LOAD;
    private RadioPowerController mController;
    private ISimManagementExt mExt;
    private boolean mPowerEnabled;
    private boolean mPowerState;
    private Switch mRadioSwith;
    private int mSubId;

    static {
        ENG_LOAD = SystemProperties.get("ro.build.type").equals("eng") ? true : Log.isLoggable("RadioPowerPreference", 3);
    }

    public RadioPowerPreference(Context context) {
        super(context);
        this.mPowerEnabled = true;
        this.mSubId = -1;
        this.mRadioSwith = null;
        this.mExt = UtilsExt.getSimManagementExt(context);
        this.mController = RadioPowerController.getInstance(context);
        setWidgetLayoutResource(R.layout.radio_power_switch);
    }

    public void setRadioOn(boolean z) {
        logInEng("setRadioOn, state=" + z + ", subId=" + this.mSubId);
        this.mPowerState = z;
        if (this.mRadioSwith != null) {
            this.mRadioSwith.setChecked(z);
        }
    }

    public void setRadioEnabled(boolean z) {
        logInEng("setRadioEnabled, enable=" + z);
        this.mPowerEnabled = z;
        if (this.mRadioSwith != null) {
            this.mRadioSwith.setEnabled(z);
        }
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mRadioSwith = (Switch) preferenceViewHolder.findViewById(R.id.radio_state);
        if (this.mRadioSwith != null) {
            this.mRadioSwith.setEnabled(this.mPowerEnabled);
            this.mRadioSwith.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.mediatek.settings.sim.RadioPowerPreference.1
                @Override // android.widget.CompoundButton.OnCheckedChangeListener
                public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                    RadioPowerPreference.this.logInEng("onCheckedChanged, mPowerState=" + RadioPowerPreference.this.mPowerState + ", isChecked=" + z + ", subId=" + RadioPowerPreference.this.mSubId);
                    if (RadioPowerPreference.this.mPowerState != z) {
                        if (!RadioPowerPreference.this.mController.setRadionOn(RadioPowerPreference.this.mSubId, z)) {
                            RadioPowerPreference.this.logInEng("onCheckedChanged, set radio power FAIL.");
                            RadioPowerPreference.this.setRadioOn(!z);
                            return;
                        }
                        RadioPowerPreference.this.logInEng("onCheckedChanged, mPowerState=" + z);
                        RadioPowerPreference.this.mPowerState = z;
                        RadioPowerPreference.this.setRadioEnabled(false);
                        RadioPowerPreference.this.mExt.customizeMainCapabily(RadioPowerPreference.this.mPowerState, RadioPowerPreference.this.mSubId);
                    }
                }
            });
            Log.d("RadioPowerPreference", "onBindViewHolder, mPowerState=" + this.mPowerState + ", subid=" + this.mSubId);
            this.mRadioSwith.setChecked(this.mPowerState);
        }
    }

    @Override // android.support.v7.preference.Preference
    public void setEnabled(boolean z) {
        this.mPowerEnabled = z;
        super.setEnabled(z);
    }

    public void bindRadioPowerState(int i, boolean z, boolean z2, boolean z3) {
        this.mSubId = i;
        if (z) {
            setRadioOn(z2);
            boolean zIsValidSubscriptionId = SubscriptionManager.isValidSubscriptionId(i);
            logInEng("bindRadioPowerState, isValidSub=" + zIsValidSubscriptionId);
            setRadioEnabled(zIsValidSubscriptionId);
            return;
        }
        logInEng("bindRadioPowerState, normal=false");
        boolean z4 = false;
        setRadioEnabled(false);
        if (!z3 && this.mController.isExpectedRadioStateOn(SubscriptionManager.getSlotIndex(i))) {
            z4 = true;
        }
        setRadioOn(z4);
    }

    private void logInEng(String str) {
        if (ENG_LOAD) {
            Log.d("RadioPowerPreference", str);
        }
    }
}
