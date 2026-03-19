package com.mediatek.internal.telephony;

import android.telephony.Rlog;
import mediatek.telephony.ISignalStrengthExt;

public class SignalStrengthExt implements ISignalStrengthExt {
    private static final boolean DBG = true;
    static final String TAG = "SignalStrengthExt";

    @Override
    public int mapUmtsSignalLevel(int i, int i2) {
        log("mapUmtsSignalLevel, phoneId=" + i);
        int i3 = 0;
        if (i2 <= -25 && i2 != Integer.MAX_VALUE) {
            if (i2 >= -72) {
                i3 = 4;
            } else if (i2 >= -88) {
                i3 = 3;
            } else if (i2 >= -104) {
                i3 = 2;
            } else if (i2 >= -120) {
                i3 = 1;
            }
        }
        log("mapUmtsSignalLevel, level=" + i3);
        return i3;
    }

    @Override
    public int mapLteSignalLevel(int i, int i2, int i3) {
        int i4;
        int i5;
        int i6 = 0;
        if (i <= -44) {
            i4 = i >= -85 ? 4 : i >= -95 ? 3 : i >= -105 ? 2 : i >= -115 ? 1 : i >= -140 ? 0 : -1;
        }
        if (i2 <= 300) {
            i5 = i2 >= 130 ? 4 : i2 >= 45 ? 3 : i2 >= 10 ? 2 : i2 >= -30 ? 1 : i2 >= -200 ? 0 : -1;
        }
        Rlog.i(TAG, "getLTELevel - rsrp:" + i + " snr:" + i2 + " rsrpIconLevel:" + i4 + " snrIconLevel:" + i5);
        if (i5 != -1 && i4 != -1) {
            return i4 < i5 ? i4 : i5;
        }
        if (i5 != -1) {
            return i5;
        }
        if (i4 != -1) {
            return i4;
        }
        if (i3 <= 63) {
            if (i3 >= 12) {
                i6 = 4;
            } else if (i3 >= 8) {
                i6 = 3;
            } else if (i3 >= 5) {
                i6 = 2;
            } else if (i3 >= 0) {
                i6 = 1;
            }
        }
        Rlog.i(TAG, "getLTELevel - rssi:" + i3 + " rssiIconLevel:" + i6);
        return i6;
    }

    public static void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str) {
        Rlog.e(TAG, str);
    }
}
