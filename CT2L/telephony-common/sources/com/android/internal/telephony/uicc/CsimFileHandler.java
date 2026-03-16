package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public final class CsimFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "CsimFH";

    public CsimFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        super(app, aid, ci);
    }

    @Override
    protected String getEFPath(int efid) {
        switch (efid) {
            case IccConstants.EF_CSIM_IMSIM:
            case IccConstants.EF_CSIM_CDMAHOME:
            case IccConstants.EF_CST:
            case 28474:
            case IccConstants.EF_FDN:
            case IccConstants.EF_SMS:
            case IccConstants.EF_MSISDN:
            case 28481:
            case 28484:
            case 28493:
            case IccConstants.EF_CSIM_EPRL:
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
