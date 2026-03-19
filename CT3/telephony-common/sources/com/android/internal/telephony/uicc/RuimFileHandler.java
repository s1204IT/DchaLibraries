package com.android.internal.telephony.uicc;

import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public final class RuimFileHandler extends IccFileHandler {
    static final String LOG_TAG = "RuimFH";

    public RuimFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        super(app, aid, ci);
    }

    @Override
    public void loadEFImgTransparent(int fileid, int highOffset, int lowOffset, int length, Message onLoaded) {
        Message response = obtainMessage(10, fileid, 0, onLoaded);
        this.mCi.iccIOForApp(192, fileid, getEFPath(IccConstants.EF_IMG), 0, 0, 10, null, null, this.mAid, response);
    }

    @Override
    protected String getEFPath(int efid) {
        switch (efid) {
            case IccConstants.EF_CSIM_IMSIM:
            case IccConstants.EF_CSIM_CDMAHOME:
            case IccConstants.EF_CST:
            case 28474:
            case IccConstants.EF_SMS:
            case 28481:
            case IccConstants.EF_CSIM_MDN:
            case IccConstants.EF_CDMA_ECC:
            case IccConstants.EF_CSIM_MIPUPP:
            case IccConstants.EF_CSIM_EPRL:
                return "3F007F25";
            default:
                return getCommonIccEFPath(efid);
        }
    }

    protected String getEFPath(int efid, boolean is7FFF) {
        logd("[RuimFH]GetEFPath : efid = " + efid + ", is7FFF = " + is7FFF);
        return getEFPath(efid);
    }

    @Override
    protected void logd(String msg) {
        Rlog.d(LOG_TAG, "[RuimFileHandler] " + msg);
    }

    @Override
    protected void loge(String msg) {
        Rlog.e(LOG_TAG, "[RuimFileHandler] " + msg);
    }
}
