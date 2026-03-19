package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public final class IsimFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "IsimFH";

    public IsimFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        super(app, aid, ci);
    }

    @Override
    protected String getEFPath(int efid) {
        switch (efid) {
            case IccConstants.EF_IMPI:
            case IccConstants.EF_DOMAIN:
            case IccConstants.EF_IMPU:
            case 28423:
            case IccConstants.EF_PCSCF:
                return "3F007FFF";
            case IccConstants.EF_LI:
            case 28422:
            case 28424:
            default:
                String path = getCommonIccEFPath(efid);
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
