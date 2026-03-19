package com.mediatek.tb.telephony;

import android.content.Context;
import android.content.Intent;
import android.telephony.Rlog;
import com.mediatek.common.PluginImpl;
import com.mediatek.common.telephony.IOnlyOwnerSimSupport;

@PluginImpl(interfaceName = "com.mediatek.common.telephony.IOnlyOwnerSimSupport")
public class OnlyOwnerSimSupport implements IOnlyOwnerSimSupport {
    private static final String TAG = "OnlyOwnerSimSupport";

    public OnlyOwnerSimSupport() {
    }

    public OnlyOwnerSimSupport(Context context) {
        if (context == null) {
            Rlog.e(TAG, "FAIL! context is null");
        } else {
            Rlog.d(TAG, "OnlyOwnerSimSupport in default ");
        }
    }

    public OnlyOwnerSimSupport(Context context, boolean enableNormalUserReceived) {
        if (context == null) {
            Rlog.e(TAG, "FAIL! context is null");
        } else {
            Rlog.d(TAG, "OnlyOwnerSimSupport in default ");
        }
    }

    public boolean isCurrentUserOwner() {
        return true;
    }

    public boolean isNetworkTypeMobile(int networkType) {
        return false;
    }

    public boolean isOnlyOwnerSimSupport() {
        return false;
    }

    public boolean isMsgDispatchOwner(Intent intent, String permission, int appOp) {
        return false;
    }

    public void intercept(Object obj, int resultCode) {
    }

    public void dispatchMsgOwner(Intent intent, int simId, String permission, int appOp) {
    }
}
