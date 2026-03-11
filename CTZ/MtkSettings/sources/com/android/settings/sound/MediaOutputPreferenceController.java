package com.android.settings.sound;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;

public class MediaOutputPreferenceController extends AudioSwitchPreferenceController {
    public MediaOutputPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }
        if (isStreamFromOutputDevice(3, 32768)) {
            this.mPreference.setVisible(false);
            preference.setSummary(this.mContext.getText(R.string.media_output_summary_unavailable));
            return;
        }
        if (Utils.isAudioModeOngoingCall(this.mContext)) {
            this.mPreference.setVisible(false);
            preference.setSummary(this.mContext.getText(R.string.media_out_summary_ongoing_call_state));
            return;
        }
        this.mConnectedDevices.clear();
        if (this.mAudioManager.getMode() == 0) {
            this.mConnectedDevices.addAll(getConnectedA2dpDevices());
            this.mConnectedDevices.addAll(getConnectedHearingAidDevices());
        }
        int size = this.mConnectedDevices.size();
        if (size == 0) {
            this.mPreference.setVisible(false);
            CharSequence text = this.mContext.getText(R.string.media_output_default_summary);
            CharSequence[] charSequenceArr = {text};
            this.mSelectedIndex = getDefaultDeviceIndex();
            preference.setSummary(text);
            setPreference(charSequenceArr, charSequenceArr, preference);
            return;
        }
        if (getAvailabilityStatus() == 0) {
            this.mPreference.setVisible(true);
        }
        int i = size + 1;
        CharSequence[] charSequenceArr2 = new CharSequence[i];
        CharSequence[] charSequenceArr3 = new CharSequence[i];
        setupPreferenceEntries(charSequenceArr2, charSequenceArr3, findActiveDevice(3));
        if (isStreamFromOutputDevice(3, 67108864)) {
            this.mSelectedIndex = getDefaultDeviceIndex();
        }
        setPreference(charSequenceArr2, charSequenceArr3, preference);
    }

    @Override
    public void setActiveBluetoothDevice(BluetoothDevice bluetoothDevice) {
        if (this.mAudioManager.getMode() != 0) {
            return;
        }
        HearingAidProfile hearingAidProfile = this.mProfileManager.getHearingAidProfile();
        A2dpProfile a2dpProfile = this.mProfileManager.getA2dpProfile();
        if (hearingAidProfile != null && a2dpProfile != null && bluetoothDevice == null) {
            hearingAidProfile.setActiveDevice(null);
            a2dpProfile.setActiveDevice(null);
            return;
        }
        if (hearingAidProfile != null && hearingAidProfile.getHiSyncId(bluetoothDevice) != 0) {
            hearingAidProfile.setActiveDevice(bluetoothDevice);
        }
        if (a2dpProfile != null) {
            a2dpProfile.setActiveDevice(bluetoothDevice);
        }
    }
}
