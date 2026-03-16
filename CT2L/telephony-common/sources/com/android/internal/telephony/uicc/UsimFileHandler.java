package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public final class UsimFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "UsimFH";

    public UsimFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        super(app, aid, ci);
    }

    @Override
    protected String getEFPath(int efid) {
        switch (efid) {
            case IccConstants.EF_PBR:
                return "3F007F105F3A";
            case IccConstants.EF_LI:
            case IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS:
            case IccConstants.EF_CFF_CPHS:
            case IccConstants.EF_SPN_CPHS:
            case IccConstants.EF_CSP_CPHS:
            case IccConstants.EF_INFO_CPHS:
            case IccConstants.EF_MAILBOX_CPHS:
            case IccConstants.EF_SPN_SHORT_CPHS:
            case 28472:
            case IccConstants.EF_FDN:
            case IccConstants.EF_SMS:
            case IccConstants.EF_GID1:
            case IccConstants.EF_MSISDN:
            case IccConstants.EF_SPN:
            case IccConstants.EF_EXT2:
            case IccConstants.EF_AD:
            case IccConstants.EF_PNN:
            case IccConstants.EF_OPL:
            case IccConstants.EF_MBDN:
            case IccConstants.EF_EXT6:
            case IccConstants.EF_MBI:
            case IccConstants.EF_MWIS:
            case IccConstants.EF_CFIS:
            case IccConstants.EF_SPDI:
                return "3F007FFF";
            default:
                String path = getCommonIccEFPath(efid);
                if (path == null) {
                    return "3F007F105F3A";
                }
                return path;
        }
    }

    @Override
    protected void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    @Override
    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
