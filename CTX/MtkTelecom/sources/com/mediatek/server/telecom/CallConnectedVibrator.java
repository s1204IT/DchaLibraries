package com.mediatek.server.telecom;

import android.content.Context;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.VibrationEffect;
import android.os.Vibrator;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallsManagerListenerBase;

public class CallConnectedVibrator extends CallsManagerListenerBase {
    private static final VibrationEffect EFFECT_CONNECTED = VibrationEffect.createOneShot(200, -1);
    private final Vibrator mVibrator;

    public CallConnectedVibrator(Context context) {
        this.mVibrator = new SystemVibrator(context);
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        if (!call.isCdma()) {
            if ((i == 1 || i == 3) && i2 == 5 && SystemProperties.getInt("persist.vendor.radio.telecom.vibrate", 1) == 1) {
                this.mVibrator.vibrate(EFFECT_CONNECTED);
            }
        }
    }
}
