package com.android.settings.development;

import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class BluetoothAudioQualityPreferenceController extends AbstractBluetoothA2dpPreferenceController {
    public BluetoothAudioQualityPreferenceController(Context context, Lifecycle lifecycle, BluetoothA2dpConfigStore bluetoothA2dpConfigStore) {
        super(context, lifecycle, bluetoothA2dpConfigStore);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bluetooth_select_a2dp_ldac_playback_quality";
    }

    @Override // com.android.settings.development.AbstractBluetoothA2dpPreferenceController
    protected String[] getListValues() {
        return this.mContext.getResources().getStringArray(R.array.bluetooth_a2dp_codec_ldac_playback_quality_values);
    }

    @Override // com.android.settings.development.AbstractBluetoothA2dpPreferenceController
    protected String[] getListSummaries() {
        return this.mContext.getResources().getStringArray(R.array.bluetooth_a2dp_codec_ldac_playback_quality_summaries);
    }

    @Override // com.android.settings.development.AbstractBluetoothA2dpPreferenceController
    protected int getDefaultIndex() {
        return 3;
    }

    @Override // com.android.settings.development.AbstractBluetoothA2dpPreferenceController
    protected void writeConfigurationValues(Object obj) {
        int i;
        int iFindIndexOfValue = this.mPreference.findIndexOfValue(obj.toString());
        switch (iFindIndexOfValue) {
            case 0:
            case 1:
            case 2:
            case 3:
                i = 1000 + iFindIndexOfValue;
                break;
            default:
                i = 0;
                break;
        }
        this.mBluetoothA2dpConfigStore.setCodecSpecific1Value(i);
    }

    @Override // com.android.settings.development.AbstractBluetoothA2dpPreferenceController
    protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig bluetoothCodecConfig) {
        int i;
        int codecSpecific1 = (int) bluetoothCodecConfig.getCodecSpecific1();
        if (codecSpecific1 > 0) {
            i = codecSpecific1 % 10;
        } else {
            i = 3;
        }
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
                return i;
            default:
                return 3;
        }
    }
}
