package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;

public final class SIMFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "SIMFileHandler";
    private UiccCardApplication mApp;

    public SIMFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        super(app, aid, ci);
        this.mApp = null;
        this.mApp = app;
    }

    @Override
    protected String getEFPath(int efid) {
        switch (efid) {
            case IccConstants.EF_PL:
            case IccConstants.EF_ICCID:
                return IccConstants.MF_SIM;
            case IccConstants.EF_IMG:
                return "3F007F105F50";
            case IccConstants.EF_PBR:
                return "3F007F105F3A";
            case IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS:
            case IccConstants.EF_CFF_CPHS:
            case IccConstants.EF_SPN_CPHS:
            case IccConstants.EF_CSP_CPHS:
            case IccConstants.EF_INFO_CPHS:
            case IccConstants.EF_MAILBOX_CPHS:
            case IccConstants.EF_SPN_SHORT_CPHS:
                return "3F007F20";
            default:
                if (this.mApp != null && this.mApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                    switch (efid) {
                        case IccConstants.EF_ARR:
                        case 28472:
                        case IccConstants.EF_FDN:
                        case IccConstants.EF_SMS:
                        case IccConstants.EF_MSISDN:
                        case IccConstants.EF_SMSP:
                        case IccConstants.EF_SMSS:
                        case 28484:
                        case IccConstants.EF_SPN:
                        case IccConstants.EF_SMSR:
                        case IccConstants.EF_SDN:
                        case IccConstants.EF_EXT2:
                        case IccConstants.EF_EXT3:
                        case 28493:
                        case IccConstants.EF_EXT4:
                        case IccConstants.EF_ECCP:
                        case IccConstants.EF_RMA:
                        case IccConstants.EF_SUME:
                        case IccConstants.EF_AD:
                        case IccConstants.EF_ECC:
                        case IccConstants.EF_PNN:
                        case IccConstants.EF_MBDN:
                        case IccConstants.EF_EXT6:
                        case IccConstants.EF_MBI:
                        case IccConstants.EF_MWIS:
                        case IccConstants.EF_CFIS:
                        case IccConstants.EF_SPDI:
                        case IccConstants.EF_ICEDN:
                        case IccConstants.EF_ICEFF:
                        case 28645:
                            return IccConstants.DF_ADF;
                    }
                }
                switch (efid) {
                    case 28472:
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
                    case 28474:
                    case IccConstants.EF_EXT1:
                        return "3F007F10";
                    case IccConstants.EF_FDN:
                    case IccConstants.EF_SMS:
                    case IccConstants.EF_MSISDN:
                    case IccConstants.EF_SMSP:
                    case IccConstants.EF_SMSS:
                    case 28484:
                    case IccConstants.EF_SMSR:
                    case IccConstants.EF_SDN:
                    case IccConstants.EF_EXT2:
                    case IccConstants.EF_EXT3:
                    case IccConstants.EF_EXT4:
                    case IccConstants.EF_ECCP:
                        return "3F007F10";
                    case IccConstants.EF_ECC:
                        return "3F007F20";
                }
                if (this.mApp != null && this.mApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                    return "3F007F105F3A";
                }
                loge("Error: EF Path being returned in null");
                return null;
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
