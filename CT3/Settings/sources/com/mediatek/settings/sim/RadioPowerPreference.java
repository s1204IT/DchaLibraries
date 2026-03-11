package com.mediatek.settings.sim;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;

public class RadioPowerPreference extends Preference {
    private RadioPowerController mController;
    private boolean mPowerEnabled;
    private boolean mPowerState;
    private Switch mRadioSwith;
    private int mSubId;

    public RadioPowerPreference(Context context) {
        super(context);
        this.mPowerEnabled = true;
        this.mSubId = -1;
        this.mRadioSwith = null;
        this.mController = RadioPowerController.getInstance(context);
        setWidgetLayoutResource(R.layout.radio_power_switch);
    }

    public void setRadioOn(boolean state) {
        Log.d("RadioPowerPreference", "setRadioOn " + state + " subId = " + this.mSubId);
        this.mPowerState = state;
        if (this.mRadioSwith == null) {
            return;
        }
        this.mRadioSwith.setChecked(state);
    }

    public void setRadioEnabled(boolean enable) {
        this.mPowerEnabled = enable;
        if (this.mRadioSwith == null) {
            return;
        }
        this.mRadioSwith.setEnabled(enable);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        this.mRadioSwith = (Switch) view.findViewById(R.id.radio_state);
        if (this.mRadioSwith == null) {
            return;
        }
        if (FeatureOption.MTK_A1_FEATURE) {
            this.mRadioSwith.setVisibility(8);
        }
        this.mRadioSwith.setEnabled(this.mPowerEnabled);
        this.mRadioSwith.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("RadioPowerPreference", "onCheckedChanged, mPowerState = " + RadioPowerPreference.this.mPowerState + ", isChecked = " + isChecked + ", subId = " + RadioPowerPreference.this.mSubId);
                if (RadioPowerPreference.this.mPowerState != isChecked) {
                    if (!RadioPowerPreference.this.mController.setRadionOn(RadioPowerPreference.this.mSubId, isChecked)) {
                        Log.w("RadioPowerPreference", "set radio power FAIL!");
                        RadioPowerPreference.this.setRadioOn(isChecked ? false : true);
                    } else {
                        Log.d("RadioPowerPreference", "onCheckedChanged mPowerState = " + isChecked);
                        RadioPowerPreference.this.mPowerState = isChecked;
                        RadioPowerPreference.this.setRadioEnabled(false);
                    }
                }
            }
        });
        Log.d("RadioPowerPreference", "onBindViewHolder mPowerState = " + this.mPowerState + " subid = " + this.mSubId);
        this.mRadioSwith.setChecked(this.mPowerState);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.mPowerEnabled = enabled;
        super.setEnabled(enabled);
    }

    public void bindRadioPowerState(int subId, boolean radioSwitchComplete) {
        this.mSubId = subId;
        if (radioSwitchComplete) {
            setRadioOn(TelephonyUtils.isRadioOn(subId, getContext()));
            setRadioEnabled(SubscriptionManager.isValidSubscriptionId(subId));
        } else {
            setRadioEnabled(false);
            setRadioOn(this.mController.isExpectedRadioStateOn(SubscriptionManager.getSlotId(subId)));
        }
    }
}
