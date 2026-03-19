package com.mediatek.internal.telephony.uicc;

import android.content.Context;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public class MtkSimHandler implements IMtkSimHandler {
    private static String TAG = "MtkSimHandler";
    protected int mPhoneId = -1;

    public MtkSimHandler() {
        mtkLog(TAG, "Enter MtkSimHandler");
    }

    public MtkSimHandler(Context context, CommandsInterface commandsInterface) {
        mtkLog(TAG, "Enter MtkSimHandler context");
    }

    @Override
    public void setPhoneId(int i) {
        this.mPhoneId = i;
    }

    @Override
    public void dispose() {
    }

    protected void mtkLog(String str, String str2) {
        Rlog.d(str, str2 + " (slot " + this.mPhoneId + ")");
    }

    protected void mtkLoge(String str, String str2) {
        Rlog.e(str, str2 + " (slot " + this.mPhoneId + ")");
    }
}
