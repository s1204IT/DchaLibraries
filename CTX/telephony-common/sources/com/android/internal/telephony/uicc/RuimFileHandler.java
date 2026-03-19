package com.android.internal.telephony.uicc;

import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public class RuimFileHandler extends IccFileHandler {
    static final String LOG_TAG = "RuimFH";

    public RuimFileHandler(UiccCardApplication uiccCardApplication, String str, CommandsInterface commandsInterface) {
        super(uiccCardApplication, str, commandsInterface);
    }

    @Override
    public void loadEFImgTransparent(int i, int i2, int i3, int i4, Message message) {
        this.mCi.iccIOForApp(192, i, getEFPath(IccConstants.EF_IMG), 0, 0, 10, null, null, this.mAid, obtainMessage(10, i, 0, message));
    }

    @Override
    protected String getEFPath(int i) {
        if (i == 28450 || i == 28456 || i == 28466 || i == 28474 || i == 28476 || i == 28481 || i == 28484 || i == 28493 || i == 28506) {
            return "3F007F25";
        }
        return getCommonIccEFPath(i);
    }

    @Override
    protected void logd(String str) {
        Rlog.d(LOG_TAG, "[RuimFileHandler] " + str);
    }

    @Override
    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[RuimFileHandler] " + str);
    }
}
