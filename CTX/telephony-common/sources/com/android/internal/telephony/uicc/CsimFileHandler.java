package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public class CsimFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "CsimFH";

    public CsimFileHandler(UiccCardApplication uiccCardApplication, String str, CommandsInterface commandsInterface) {
        super(uiccCardApplication, str, commandsInterface);
    }

    @Override
    protected String getEFPath(int i) {
        if (i == 28450 || i == 28456 || i == 28466 || i == 28484 || i == 28493 || i == 28506) {
            return "3F007FFF";
        }
        switch (i) {
            case 28474:
            case IccConstants.EF_FDN:
            case IccConstants.EF_SMS:
                return "3F007FFF";
            default:
                switch (i) {
                    case IccConstants.EF_MSISDN:
                    case 28481:
                        return "3F007FFF";
                    default:
                        String commonIccEFPath = getCommonIccEFPath(i);
                        if (commonIccEFPath == null) {
                            return "3F007F105F3A";
                        }
                        return commonIccEFPath;
                }
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
