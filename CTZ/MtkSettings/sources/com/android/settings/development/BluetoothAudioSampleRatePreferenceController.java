package com.android.settings.development;

import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class BluetoothAudioSampleRatePreferenceController extends AbstractBluetoothA2dpPreferenceController {
    public BluetoothAudioSampleRatePreferenceController(Context context, Lifecycle lifecycle, BluetoothA2dpConfigStore bluetoothA2dpConfigStore) {
        super(context, lifecycle, bluetoothA2dpConfigStore);
    }

    @Override
    public String getPreferenceKey() {
        return "bluetooth_select_a2dp_sample_rate";
    }

    @Override
    protected String[] getListValues() {
        return this.mContext.getResources().getStringArray(R.array.bluetooth_a2dp_codec_sample_rate_values);
    }

    @Override
    protected String[] getListSummaries() {
        return this.mContext.getResources().getStringArray(R.array.bluetooth_a2dp_codec_sample_rate_summaries);
    }

    @Override
    protected int getDefaultIndex() {
        return 0;
    }

    @Override
    protected void writeConfigurationValues(Object obj) {
        int i = 0;
        switch (this.mPreference.findIndexOfValue(obj.toString())) {
            case 1:
                i = 1;
                break;
            case 2:
                i = 2;
                break;
            case 3:
                i = 4;
                break;
            case 4:
                i = 8;
                break;
        }
        this.mBluetoothA2dpConfigStore.setSampleRate(i);
    }

    @Override
    protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig bluetoothCodecConfig) {
        int sampleRate = bluetoothCodecConfig.getSampleRate();
        if (sampleRate == 4) {
            return 3;
        }
        if (sampleRate == 8) {
            return 4;
        }
        switch (sampleRate) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
        }
    }
}
