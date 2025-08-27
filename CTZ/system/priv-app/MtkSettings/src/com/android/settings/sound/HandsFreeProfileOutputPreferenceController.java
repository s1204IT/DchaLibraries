package com.android.settings.sound;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;

/* loaded from: classes.dex */
public class HandsFreeProfileOutputPreferenceController extends AudioSwitchPreferenceController {
    public HandsFreeProfileOutputPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }
        if (!Utils.isAudioModeOngoingCall(this.mContext)) {
            this.mPreference.setVisible(false);
            preference.setSummary(this.mContext.getText(R.string.media_output_default_summary));
            return;
        }
        this.mConnectedDevices.clear();
        this.mConnectedDevices.addAll(getConnectedHfpDevices());
        this.mConnectedDevices.addAll(getConnectedHearingAidDevices());
        int size = this.mConnectedDevices.size();
        if (size != 0) {
            this.mPreference.setVisible(true);
            int i = size + 1;
            CharSequence[] charSequenceArr = new CharSequence[i];
            CharSequence[] charSequenceArr2 = new CharSequence[i];
            setupPreferenceEntries(charSequenceArr, charSequenceArr2, findActiveDevice(0));
            if (isStreamFromOutputDevice(0, 67108864)) {
                this.mSelectedIndex = getDefaultDeviceIndex();
            }
            setPreference(charSequenceArr, charSequenceArr2, preference);
            return;
        }
        this.mPreference.setVisible(false);
        CharSequence text = this.mContext.getText(R.string.media_output_default_summary);
        CharSequence[] charSequenceArr3 = {text};
        this.mSelectedIndex = getDefaultDeviceIndex();
        preference.setSummary(text);
        setPreference(charSequenceArr3, charSequenceArr3, preference);
    }

    @Override // com.android.settings.sound.AudioSwitchPreferenceController
    public void setActiveBluetoothDevice(BluetoothDevice bluetoothDevice) {
        if (!Utils.isAudioModeOngoingCall(this.mContext)) {
            return;
        }
        HearingAidProfile hearingAidProfile = this.mProfileManager.getHearingAidProfile();
        HeadsetProfile headsetProfile = this.mProfileManager.getHeadsetProfile();
        if (hearingAidProfile != null && headsetProfile != null && bluetoothDevice == null) {
            headsetProfile.setActiveDevice(null);
            hearingAidProfile.setActiveDevice(null);
            return;
        }
        if (hearingAidProfile != null && hearingAidProfile.getHiSyncId(bluetoothDevice) != 0) {
            hearingAidProfile.setActiveDevice(bluetoothDevice);
        }
        if (headsetProfile != null) {
            headsetProfile.setActiveDevice(bluetoothDevice);
        }
    }
}
