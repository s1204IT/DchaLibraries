package com.android.settings.development;

import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class BluetoothAudioChannelModePreferenceController extends AbstractBluetoothA2dpPreferenceController {
    public BluetoothAudioChannelModePreferenceController(Context context, Lifecycle lifecycle, BluetoothA2dpConfigStore bluetoothA2dpConfigStore) {
        super(context, lifecycle, bluetoothA2dpConfigStore);
    }

    @Override
    public String getPreferenceKey() {
        return "bluetooth_select_a2dp_channel_mode";
    }

    @Override
    protected String[] getListValues() {
        return this.mContext.getResources().getStringArray(R.array.bluetooth_a2dp_codec_channel_mode_values);
    }

    @Override
    protected String[] getListSummaries() {
        return this.mContext.getResources().getStringArray(R.array.bluetooth_a2dp_codec_channel_mode_summaries);
    }

    @Override
    protected int getDefaultIndex() {
        return 0;
    }

    @Override
    protected void writeConfigurationValues(Object obj) {
        int i;
        switch (this.mPreference.findIndexOfValue(obj.toString())) {
            case 0:
            default:
                i = 0;
                break;
            case 1:
                i = 1;
                break;
            case 2:
                i = 2;
                break;
        }
        this.mBluetoothA2dpConfigStore.setChannelMode(i);
    }

    @Override
    protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig bluetoothCodecConfig) {
        switch (bluetoothCodecConfig.getChannelMode()) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
        }
    }
}
