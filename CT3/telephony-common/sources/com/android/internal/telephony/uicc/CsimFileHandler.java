package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccFileHandler;

public final class CsimFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "CsimFH";

    public CsimFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        super(app, aid, ci);
    }

    @Override
    protected String getEFPath(int efid) {
        logd("GetEFPath : " + efid);
        switch (efid) {
            case IccConstants.EF_CSIM_IMSIM:
            case IccConstants.EF_CSIM_CDMAHOME:
            case IccConstants.EF_CST:
            case 28474:
            case IccConstants.EF_FDN:
            case IccConstants.EF_MSISDN:
            case 28481:
            case IccConstants.EF_CSIM_MDN:
            case IccConstants.EF_CDMA_ECC:
            case IccConstants.EF_CSIM_MIPUPP:
            case IccConstants.EF_CSIM_EPRL:
            case IccConstants.EF_EST:
                return "3F007FFF";
            case IccConstants.EF_SMS:
                return "3F007F25";
            default:
                String path = getCommonIccEFPath(efid);
                if (path == null) {
                    return "3F007F105F3A";
                }
                return path;
        }
    }

    protected String getEFPath(int efid, boolean is7FFF) {
        return getEFPath(efid);
    }

    @Override
    protected String getCommonIccEFPath(int efid) {
        logd("getCommonIccEFPath : " + efid);
        switch (efid) {
            case 12037:
            case IccConstants.EF_ICCID:
                return IccConstants.MF_SIM;
            case IccConstants.EF_IMG:
                return "3F007F105F50";
            case IccConstants.EF_PBR:
                return "3F007F105F3A";
            case 28474:
            case IccConstants.EF_FDN:
            case IccConstants.EF_MSISDN:
            case IccConstants.EF_SDN:
            case IccConstants.EF_EXT1:
            case IccConstants.EF_EXT2:
            case IccConstants.EF_EXT3:
                return "3F007F10";
            default:
                return null;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 8:
                AsyncResult ar = (AsyncResult) msg.obj;
                IccFileHandler.LoadLinearFixedContext lc = (IccFileHandler.LoadLinearFixedContext) ar.userObj;
                IccIoResult result = (IccIoResult) ar.result;
                Message response = lc.mOnLoaded;
                if (ar.exception != null || result.getException() != null) {
                    super.handleMessage(msg);
                } else {
                    byte[] data = result.payload;
                    if (4 != data[6] || 1 != data[13]) {
                        super.handleMessage(msg);
                    } else {
                        int recordSize = data[14] & 255;
                        super.handleMessage(msg);
                    }
                }
                break;
            default:
                super.handleMessage(msg);
                break;
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
