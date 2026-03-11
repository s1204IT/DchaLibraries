package com.android.settings.nfc;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;

public class NfcEnabler extends BaseNfcEnabler {
    private final SwitchPreference mPreference;

    public NfcEnabler(Context context, SwitchPreference switchPreference) {
        super(context);
        this.mPreference = switchPreference;
    }

    @Override
    protected void handleNfcStateChanged(int i) {
        switch (i) {
            case 1:
                this.mPreference.setChecked(false);
                this.mPreference.setEnabled(true);
                break;
            case 2:
                this.mPreference.setChecked(true);
                this.mPreference.setEnabled(false);
                break;
            case 3:
                this.mPreference.setChecked(true);
                this.mPreference.setEnabled(true);
                break;
            case 4:
                this.mPreference.setChecked(false);
                this.mPreference.setEnabled(false);
                break;
        }
    }
}
