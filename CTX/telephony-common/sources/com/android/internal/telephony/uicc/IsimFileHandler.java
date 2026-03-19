package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public class IsimFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "IsimFH";

    public IsimFileHandler(UiccCardApplication uiccCardApplication, String str, CommandsInterface commandsInterface) {
        super(uiccCardApplication, str, commandsInterface);
    }

    @Override
    protected String getEFPath(int i) {
        if (i == 28423 || i == 28425) {
            return "3F007FFF";
        }
        switch (i) {
            case IccConstants.EF_IMPI:
            case IccConstants.EF_DOMAIN:
            case IccConstants.EF_IMPU:
                return "3F007FFF";
            default:
                return getCommonIccEFPath(i);
        }
    }

    @Override
    protected void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    @Override
    protected void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }
}
