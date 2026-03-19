package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public class SIMFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "SIMFileHandler";

    public SIMFileHandler(UiccCardApplication uiccCardApplication, String str, CommandsInterface commandsInterface) {
        super(uiccCardApplication, str, commandsInterface);
    }

    @Override
    protected String getEFPath(int i) {
        if (i == 28433 || i == 28472) {
            return "3F007F20";
        }
        if (i == 28476) {
            return "3F007F10";
        }
        if (i == 28486 || i == 28589 || i == 28613 || i == 28621) {
            return "3F007F20";
        }
        switch (i) {
            case IccConstants.EF_CFF_CPHS:
            case IccConstants.EF_SPN_CPHS:
            case IccConstants.EF_CSP_CPHS:
            case IccConstants.EF_INFO_CPHS:
            case IccConstants.EF_MAILBOX_CPHS:
            case IccConstants.EF_SPN_SHORT_CPHS:
                return "3F007F20";
            default:
                switch (i) {
                    case IccConstants.EF_GID1:
                    case IccConstants.EF_GID2:
                        return "3F007F20";
                    default:
                        switch (i) {
                            case IccConstants.EF_MBDN:
                            case IccConstants.EF_EXT6:
                            case IccConstants.EF_MBI:
                            case IccConstants.EF_MWIS:
                            case IccConstants.EF_CFIS:
                                return "3F007F20";
                            default:
                                String commonIccEFPath = getCommonIccEFPath(i);
                                if (commonIccEFPath == null) {
                                    Rlog.e(LOG_TAG, "Error: EF Path being returned in null");
                                }
                                return commonIccEFPath;
                        }
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
