package com.android.bluetooth.a2dp;

import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import com.android.bluetooth.R;

class A2dpCodecConfig {
    private static final boolean DBG = true;
    private static final String TAG = "A2dpCodecConfig";
    private A2dpNativeInterface mA2dpNativeInterface;
    private Context mContext;
    private int mA2dpSourceCodecPrioritySbc = 0;
    private int mA2dpSourceCodecPriorityAac = 0;
    private int mA2dpSourceCodecPriorityAptx = 0;
    private int mA2dpSourceCodecPriorityAptxHd = 0;
    private int mA2dpSourceCodecPriorityLdac = 0;
    private BluetoothCodecConfig[] mCodecConfigPriorities = assignCodecConfigPriorities();

    A2dpCodecConfig(Context context, A2dpNativeInterface a2dpNativeInterface) {
        this.mContext = context;
        this.mA2dpNativeInterface = a2dpNativeInterface;
    }

    BluetoothCodecConfig[] codecConfigPriorities() {
        return this.mCodecConfigPriorities;
    }

    void setCodecConfigPreference(BluetoothDevice bluetoothDevice, BluetoothCodecConfig bluetoothCodecConfig) {
        this.mA2dpNativeInterface.setCodecConfigPreference(bluetoothDevice, new BluetoothCodecConfig[]{bluetoothCodecConfig});
    }

    void enableOptionalCodecs(BluetoothDevice bluetoothDevice) {
        BluetoothCodecConfig[] bluetoothCodecConfigArrAssignCodecConfigPriorities = assignCodecConfigPriorities();
        if (bluetoothCodecConfigArrAssignCodecConfigPriorities == null) {
            return;
        }
        for (int i = 0; i < bluetoothCodecConfigArrAssignCodecConfigPriorities.length; i++) {
            if (!bluetoothCodecConfigArrAssignCodecConfigPriorities[i].isMandatoryCodec()) {
                bluetoothCodecConfigArrAssignCodecConfigPriorities[i] = null;
            }
        }
        this.mA2dpNativeInterface.setCodecConfigPreference(bluetoothDevice, bluetoothCodecConfigArrAssignCodecConfigPriorities);
    }

    void disableOptionalCodecs(BluetoothDevice bluetoothDevice) {
        BluetoothCodecConfig[] bluetoothCodecConfigArrAssignCodecConfigPriorities = assignCodecConfigPriorities();
        if (bluetoothCodecConfigArrAssignCodecConfigPriorities == null) {
            return;
        }
        for (int i = 0; i < bluetoothCodecConfigArrAssignCodecConfigPriorities.length; i++) {
            BluetoothCodecConfig bluetoothCodecConfig = bluetoothCodecConfigArrAssignCodecConfigPriorities[i];
            if (bluetoothCodecConfig.isMandatoryCodec()) {
                bluetoothCodecConfig.setCodecPriority(1000000);
            } else {
                bluetoothCodecConfigArrAssignCodecConfigPriorities[i] = null;
            }
        }
        this.mA2dpNativeInterface.setCodecConfigPreference(bluetoothDevice, bluetoothCodecConfigArrAssignCodecConfigPriorities);
    }

    private BluetoothCodecConfig[] assignCodecConfigPriorities() {
        int integer;
        int integer2;
        int integer3;
        int integer4;
        int integer5;
        Resources resources = this.mContext.getResources();
        if (resources == null) {
            return null;
        }
        try {
            integer = resources.getInteger(R.integer.a2dp_source_codec_priority_sbc);
        } catch (Resources.NotFoundException e) {
            integer = 0;
        }
        if (integer >= -1 && integer < 1000000) {
            this.mA2dpSourceCodecPrioritySbc = integer;
        }
        try {
            integer2 = resources.getInteger(R.integer.a2dp_source_codec_priority_aac);
        } catch (Resources.NotFoundException e2) {
            integer2 = 0;
        }
        if (integer2 >= -1 && integer2 < 1000000) {
            this.mA2dpSourceCodecPriorityAac = integer2;
        }
        try {
            integer3 = resources.getInteger(R.integer.a2dp_source_codec_priority_aptx);
        } catch (Resources.NotFoundException e3) {
            integer3 = 0;
        }
        if (integer3 >= -1 && integer3 < 1000000) {
            this.mA2dpSourceCodecPriorityAptx = integer3;
        }
        try {
            integer4 = resources.getInteger(R.integer.a2dp_source_codec_priority_aptx_hd);
        } catch (Resources.NotFoundException e4) {
            integer4 = 0;
        }
        if (integer4 >= -1 && integer4 < 1000000) {
            this.mA2dpSourceCodecPriorityAptxHd = integer4;
        }
        try {
            integer5 = resources.getInteger(R.integer.a2dp_source_codec_priority_ldac);
        } catch (Resources.NotFoundException e5) {
            integer5 = 0;
        }
        if (integer5 >= -1 && integer5 < 1000000) {
            this.mA2dpSourceCodecPriorityLdac = integer5;
        }
        return new BluetoothCodecConfig[]{new BluetoothCodecConfig(0, this.mA2dpSourceCodecPrioritySbc, 0, 0, 0, 0L, 0L, 0L, 0L), new BluetoothCodecConfig(1, this.mA2dpSourceCodecPriorityAac, 0, 0, 0, 0L, 0L, 0L, 0L), new BluetoothCodecConfig(2, this.mA2dpSourceCodecPriorityAptx, 0, 0, 0, 0L, 0L, 0L, 0L), new BluetoothCodecConfig(3, this.mA2dpSourceCodecPriorityAptxHd, 0, 0, 0, 0L, 0L, 0L, 0L), new BluetoothCodecConfig(4, this.mA2dpSourceCodecPriorityLdac, 0, 0, 0, 0L, 0L, 0L, 0L)};
    }
}
