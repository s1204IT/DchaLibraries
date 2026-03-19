package com.mediatek.internal.telephony.devreg;

import android.app.PendingIntent;
import android.content.Context;
import com.android.internal.telephony.Phone;
import com.mediatek.internal.telephony.MtkUiccSmsController;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;

public class DeviceRegisterController {
    private static IDeviceRegisterExt sDeviceRegisterExt = null;
    private DeviceRegisterHandler[] mHandler;
    private MtkUiccSmsController mSmsController;

    public DeviceRegisterController(Context context, Phone[] phoneArr, MtkUiccSmsController mtkUiccSmsController) {
        this.mSmsController = null;
        this.mHandler = null;
        this.mSmsController = mtkUiccSmsController;
        try {
            sDeviceRegisterExt = OpTelephonyCustomizationUtils.getOpFactory(context).makeDeviceRegisterExt(context, this);
        } catch (Exception e) {
            sDeviceRegisterExt = new DefaultDeviceRegisterExt(context, this);
        }
        this.mHandler = new DeviceRegisterHandler[phoneArr.length];
        for (int i = 0; i < phoneArr.length; i++) {
            this.mHandler[i] = new DeviceRegisterHandler(phoneArr[i], this);
        }
    }

    public void sendDataSms(int i, String str, String str2, int i2, int i3, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        this.mSmsController.sendData(i, str, str2, i2, i3, bArr, pendingIntent, pendingIntent2);
    }

    private static IDeviceRegisterExt getDeviceRegisterExt() {
        return sDeviceRegisterExt;
    }

    public void setCdmaCardEsnOrMeid(String str) {
        if (sDeviceRegisterExt != null) {
            sDeviceRegisterExt.setCdmaCardEsnOrMeid(str);
        }
    }

    public void handleAutoRegMessage(byte[] bArr) {
        if (sDeviceRegisterExt != null) {
            sDeviceRegisterExt.handleAutoRegMessage(bArr);
        }
    }
}
