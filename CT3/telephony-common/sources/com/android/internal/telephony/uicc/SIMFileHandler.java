package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public final class SIMFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "SIMFileHandler";

    public SIMFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        super(app, aid, ci);
    }

    @Override
    protected String getEFPath(int efid) {
        switch (efid) {
            case IccConstants.EF_RAT:
                return "7FFF7F665F30";
            case IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS:
            case IccConstants.EF_CFF_CPHS:
            case IccConstants.EF_SPN_CPHS:
            case IccConstants.EF_CSP_CPHS:
            case IccConstants.EF_INFO_CPHS:
            case IccConstants.EF_MAILBOX_CPHS:
            case IccConstants.EF_SPN_SHORT_CPHS:
                return "3F007F20";
            case IccConstants.EF_CSIM_IMSIM:
                return "3F007F25";
            case IccConstants.EF_SST:
            case IccConstants.EF_GID1:
            case IccConstants.EF_GID2:
            case IccConstants.EF_SPN:
            case IccConstants.EF_AD:
            case IccConstants.EF_PNN:
            case IccConstants.EF_MBDN:
            case IccConstants.EF_EXT6:
            case IccConstants.EF_MBI:
            case IccConstants.EF_MWIS:
            case IccConstants.EF_CFIS:
            case IccConstants.EF_SPDI:
                return "3F007F20";
            case IccConstants.EF_SMS:
            case IccConstants.EF_SMSP:
                return "3F007F10";
            case IccConstants.EF_ECC:
            case IccConstants.EF_OPL:
                return "3F007F20";
            default:
                String path = getCommonIccEFPath(efid);
                if (path == null) {
                    Rlog.e(LOG_TAG, "Error: EF Path being returned in null");
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
