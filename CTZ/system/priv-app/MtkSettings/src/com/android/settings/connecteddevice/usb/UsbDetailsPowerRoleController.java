package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.Utils;

/* loaded from: classes.dex */
public class UsbDetailsPowerRoleController extends UsbDetailsController implements Preference.OnPreferenceClickListener {
    private final Runnable mFailureCallback;
    private int mNextPowerRole;
    private PreferenceCategory mPreferenceCategory;
    private SwitchPreference mSwitchPreference;

    public static /* synthetic */ void lambda$new$0(UsbDetailsPowerRoleController usbDetailsPowerRoleController) {
        if (usbDetailsPowerRoleController.mNextPowerRole != 0) {
            usbDetailsPowerRoleController.mSwitchPreference.setSummary(R.string.usb_switching_failed);
            usbDetailsPowerRoleController.mNextPowerRole = 0;
        }
    }

    public UsbDetailsPowerRoleController(Context context, UsbDetailsFragment usbDetailsFragment, UsbBackend usbBackend) {
        super(context, usbDetailsFragment, usbBackend);
        this.mFailureCallback = new Runnable() { // from class: com.android.settings.connecteddevice.usb.-$$Lambda$UsbDetailsPowerRoleController$jiVF0c0jApWPiJapsUjjyYudYlM
            @Override // java.lang.Runnable
            public final void run() {
                UsbDetailsPowerRoleController.lambda$new$0(this.f$0);
            }
        };
        this.mNextPowerRole = 0;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreferenceCategory = (PreferenceCategory) preferenceScreen.findPreference(getPreferenceKey());
        this.mSwitchPreference = new SwitchPreference(this.mPreferenceCategory.getContext());
        this.mSwitchPreference.setTitle(R.string.usb_use_power_only);
        this.mSwitchPreference.setOnPreferenceClickListener(this);
        this.mPreferenceCategory.addPreference(this.mSwitchPreference);
    }

    @Override // com.android.settings.connecteddevice.usb.UsbDetailsController
    protected void refresh(boolean z, long j, int i, int i2) {
        if (z && !this.mUsbBackend.areAllRolesSupported()) {
            this.mFragment.getPreferenceScreen().removePreference(this.mPreferenceCategory);
        } else if (z && this.mUsbBackend.areAllRolesSupported()) {
            this.mFragment.getPreferenceScreen().addPreference(this.mPreferenceCategory);
        }
        if (i == 1) {
            this.mSwitchPreference.setChecked(true);
            this.mPreferenceCategory.setEnabled(true);
        } else if (i == 2) {
            this.mSwitchPreference.setChecked(false);
            this.mPreferenceCategory.setEnabled(true);
        } else if (!z || i == 0) {
            this.mPreferenceCategory.setEnabled(false);
            if (this.mNextPowerRole == 0) {
                this.mSwitchPreference.setSummary("");
            }
        }
        if (this.mNextPowerRole != 0 && i != 0) {
            if (this.mNextPowerRole == i) {
                this.mSwitchPreference.setSummary("");
            } else {
                this.mSwitchPreference.setSummary(R.string.usb_switching_failed);
            }
            this.mNextPowerRole = 0;
            this.mHandler.removeCallbacks(this.mFailureCallback);
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        int i;
        if (!this.mSwitchPreference.isChecked()) {
            i = 2;
        } else {
            i = 1;
        }
        if (this.mUsbBackend.getPowerRole() != i && this.mNextPowerRole == 0 && !Utils.isMonkeyRunning()) {
            this.mUsbBackend.setPowerRole(i);
            this.mNextPowerRole = i;
            this.mSwitchPreference.setSummary(R.string.usb_switching);
            this.mHandler.postDelayed(this.mFailureCallback, this.mUsbBackend.areAllRolesSupported() ? 3000L : 15000L);
        }
        this.mSwitchPreference.setChecked(!this.mSwitchPreference.isChecked());
        return true;
    }

    @Override // com.android.settings.connecteddevice.usb.UsbDetailsController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "usb_details_power_role";
    }
}
