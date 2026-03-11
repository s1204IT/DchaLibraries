package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.widget.RadioButtonPreference;

public class UsbDetailsDataRoleController extends UsbDetailsController implements RadioButtonPreference.OnClickListener {
    private RadioButtonPreference mDevicePref;
    private final Runnable mFailureCallback;
    private RadioButtonPreference mHostPref;
    private RadioButtonPreference mNextRolePref;
    private PreferenceCategory mPreferenceCategory;

    public static void lambda$new$0(UsbDetailsDataRoleController usbDetailsDataRoleController) {
        if (usbDetailsDataRoleController.mNextRolePref != null) {
            usbDetailsDataRoleController.mNextRolePref.setSummary(R.string.usb_switching_failed);
            usbDetailsDataRoleController.mNextRolePref = null;
        }
    }

    public UsbDetailsDataRoleController(Context context, UsbDetailsFragment usbDetailsFragment, UsbBackend usbBackend) {
        super(context, usbDetailsFragment, usbBackend);
        this.mFailureCallback = new Runnable() {
            @Override
            public final void run() {
                UsbDetailsDataRoleController.lambda$new$0(this.f$0);
            }
        };
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreferenceCategory = (PreferenceCategory) preferenceScreen.findPreference(getPreferenceKey());
        this.mHostPref = makeRadioPreference(UsbBackend.dataRoleToString(1), R.string.usb_control_host);
        this.mDevicePref = makeRadioPreference(UsbBackend.dataRoleToString(2), R.string.usb_control_device);
    }

    @Override
    protected void refresh(boolean z, long j, int i, int i2) {
        if (i2 == 2) {
            this.mDevicePref.setChecked(true);
            this.mHostPref.setChecked(false);
            this.mPreferenceCategory.setEnabled(true);
        } else if (i2 == 1) {
            this.mDevicePref.setChecked(false);
            this.mHostPref.setChecked(true);
            this.mPreferenceCategory.setEnabled(true);
        } else if (!z || i2 == 0) {
            this.mPreferenceCategory.setEnabled(false);
            if (this.mNextRolePref == null) {
                this.mHostPref.setSummary("");
                this.mDevicePref.setSummary("");
            }
        }
        if (this.mNextRolePref != null && i2 != 0) {
            if (UsbBackend.dataRoleFromString(this.mNextRolePref.getKey()) == i2) {
                this.mNextRolePref.setSummary("");
            } else {
                this.mNextRolePref.setSummary(R.string.usb_switching_failed);
            }
            this.mNextRolePref = null;
            this.mHandler.removeCallbacks(this.mFailureCallback);
        }
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference radioButtonPreference) {
        int iDataRoleFromString = UsbBackend.dataRoleFromString(radioButtonPreference.getKey());
        if (iDataRoleFromString != this.mUsbBackend.getDataRole() && this.mNextRolePref == null && !Utils.isMonkeyRunning()) {
            this.mUsbBackend.setDataRole(iDataRoleFromString);
            this.mNextRolePref = radioButtonPreference;
            radioButtonPreference.setSummary(R.string.usb_switching);
            this.mHandler.postDelayed(this.mFailureCallback, this.mUsbBackend.areAllRolesSupported() ? 3000L : 15000L);
        }
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning();
    }

    @Override
    public String getPreferenceKey() {
        return "usb_details_data_role";
    }

    private RadioButtonPreference makeRadioPreference(String str, int i) {
        RadioButtonPreference radioButtonPreference = new RadioButtonPreference(this.mPreferenceCategory.getContext());
        radioButtonPreference.setKey(str);
        radioButtonPreference.setTitle(i);
        radioButtonPreference.setOnClickListener(this);
        this.mPreferenceCategory.addPreference(radioButtonPreference);
        return radioButtonPreference;
    }
}
