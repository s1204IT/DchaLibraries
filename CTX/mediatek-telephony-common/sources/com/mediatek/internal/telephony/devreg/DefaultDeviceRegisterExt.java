package com.mediatek.internal.telephony.devreg;

import android.content.Context;
import android.util.Log;

public class DefaultDeviceRegisterExt implements IDeviceRegisterExt {
    private static final String TAG = "DefaultDeviceRegisterExt";
    protected Context mContext;
    protected DeviceRegisterController mDeviceRegisterController;

    public DefaultDeviceRegisterExt(Context context, DeviceRegisterController deviceRegisterController) {
        this.mContext = context;
        this.mDeviceRegisterController = deviceRegisterController;
    }

    @Override
    public void setCdmaCardEsnOrMeid(String str) {
        Log.i(TAG, "setCdmaCardEsnOrMeid " + str);
    }

    @Override
    public void handleAutoRegMessage(byte[] bArr) {
        Log.i(TAG, "handleAutoRegMessage");
    }
}
