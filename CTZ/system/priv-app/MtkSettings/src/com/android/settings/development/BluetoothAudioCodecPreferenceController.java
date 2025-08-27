package com.android.settings.development;

import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class BluetoothAudioCodecPreferenceController extends AbstractBluetoothA2dpPreferenceController {
    public BluetoothAudioCodecPreferenceController(Context context, Lifecycle lifecycle, BluetoothA2dpConfigStore bluetoothA2dpConfigStore) {
        super(context, lifecycle, bluetoothA2dpConfigStore);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bluetooth_select_a2dp_codec";
    }

    @Override // com.android.settings.development.AbstractBluetoothA2dpPreferenceController
    protected String[] getListValues() {
        return this.mContext.getResources().getStringArray(R.array.bluetooth_a2dp_codec_values);
    }

    @Override // com.android.settings.development.AbstractBluetoothA2dpPreferenceController
    protected String[] getListSummaries() {
        return this.mContext.getResources().getStringArray(R.array.bluetooth_a2dp_codec_summaries);
    }

    @Override // com.android.settings.development.AbstractBluetoothA2dpPreferenceController
    protected int getDefaultIndex() {
        return 0;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [126=9] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:43:0x0076  */
    @Override // com.android.settings.development.AbstractBluetoothA2dpPreferenceController
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected void writeConfigurationValues(Object obj) {
        int i = 0;
        int i2 = 1000000;
        switch (this.mPreference.findIndexOfValue(obj.toString())) {
            case 0:
                switch (this.mPreference.findIndexOfValue(this.mPreference.getValue())) {
                    case 0:
                    case 1:
                    default:
                        i2 = 0;
                        break;
                    case 2:
                        i2 = 0;
                        i = 1;
                        break;
                    case 3:
                        i2 = 0;
                        i = 2;
                        break;
                    case 4:
                        i2 = 0;
                        i = 3;
                        break;
                    case 5:
                        i2 = 0;
                        i = 4;
                        break;
                }
                this.mBluetoothA2dpConfigStore.setCodecType(i);
                this.mBluetoothA2dpConfigStore.setCodecPriority(i2);
                return;
            case 1:
                this.mBluetoothA2dpConfigStore.setCodecType(i);
                this.mBluetoothA2dpConfigStore.setCodecPriority(i2);
                return;
            case 2:
                i = 1;
                this.mBluetoothA2dpConfigStore.setCodecType(i);
                this.mBluetoothA2dpConfigStore.setCodecPriority(i2);
                return;
            case 3:
                i = 2;
                this.mBluetoothA2dpConfigStore.setCodecType(i);
                this.mBluetoothA2dpConfigStore.setCodecPriority(i2);
                return;
            case 4:
                i = 3;
                this.mBluetoothA2dpConfigStore.setCodecType(i);
                this.mBluetoothA2dpConfigStore.setCodecPriority(i2);
                return;
            case 5:
                i = 4;
                this.mBluetoothA2dpConfigStore.setCodecType(i);
                this.mBluetoothA2dpConfigStore.setCodecPriority(i2);
                return;
            case 6:
                synchronized (this.mBluetoothA2dpConfigStore) {
                    if (this.mBluetoothA2dp != null) {
                        this.mBluetoothA2dp.enableOptionalCodecs(null);
                    }
                }
                return;
            case 7:
                synchronized (this.mBluetoothA2dpConfigStore) {
                    if (this.mBluetoothA2dp != null) {
                        this.mBluetoothA2dp.disableOptionalCodecs(null);
                    }
                }
                return;
        }
    }

    @Override // com.android.settings.development.AbstractBluetoothA2dpPreferenceController
    protected int getCurrentA2dpSettingIndex(BluetoothCodecConfig bluetoothCodecConfig) {
        switch (bluetoothCodecConfig.getCodecType()) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            default:
                return 0;
        }
    }
}
