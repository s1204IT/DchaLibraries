package android.telephony;

import com.android.internal.telephony.RILConstants;
import com.mediatek.internal.telephony.MtkPhoneNumberFormatUtil;
import com.mediatek.internal.telephony.MtkRILConstants;

public class MtkRadioAccessFamily {
    private static final int CDMA = 112;
    private static final int EVDO = 12672;
    private static final int GSM = 65542;
    private static final int HS = 36352;
    private static final int LTE = 540672;
    public static final int RAF_1xRTT = 64;
    public static final int RAF_EDGE = 4;
    public static final int RAF_EHRPD = 8192;
    public static final int RAF_EVDO_0 = 128;
    public static final int RAF_EVDO_A = 256;
    public static final int RAF_EVDO_B = 4096;
    public static final int RAF_GPRS = 2;
    public static final int RAF_GSM = 65536;
    public static final int RAF_HSDPA = 512;
    public static final int RAF_HSPA = 2048;
    public static final int RAF_HSPAP = 32768;
    public static final int RAF_HSUPA = 1024;
    public static final int RAF_IS95A = 16;
    public static final int RAF_IS95B = 32;
    public static final int RAF_LTE = 16384;
    public static final int RAF_LTE_CA = 524288;
    public static final int RAF_TD_SCDMA = 131072;
    public static final int RAF_UMTS = 8;
    public static final int RAF_UNKNOWN = 1;
    private static final int WCDMA = 167432;

    public static int getRafFromNetworkType(int i) {
        switch (i) {
            case 0:
            case 3:
            case MtkPhoneNumberFormatUtil.FORMAT_VIETNAM:
                return 232974;
            case 1:
                return GSM;
            case 2:
            case 14:
                return WCDMA;
            case 4:
                return 12784;
            case 5:
                return CDMA;
            case 6:
                return EVDO;
            case 7:
            case 21:
                return 245758;
            case 8:
                return 553456;
            case MtkPhoneNumberFormatUtil.FORMAT_ITALY:
            case MtkPhoneNumberFormatUtil.FORMAT_POLAND:
                return 773646;
            case 10:
            case MtkPhoneNumberFormatUtil.FORMAT_NEW_ZEALAND:
                return 786430;
            case 11:
                break;
            case 12:
            case MtkPhoneNumberFormatUtil.FORMAT_PORTUGAL:
                return 708104;
            case 13:
                return RAF_TD_SCDMA;
            case 15:
                return 671744;
            case 16:
                return 196614;
            case MtkPhoneNumberFormatUtil.FORMAT_THAILAND:
                return 737286;
            default:
                switch (i) {
                    case MtkRILConstants.NETWORK_MODE_LTE_GSM:
                        return 606214;
                    case MtkRILConstants.NETWORK_MODE_LTE_TDD_ONLY:
                        break;
                    case 32:
                        return 65654;
                    case MtkRILConstants.NETWORK_MODE_CDMA_EVDO_GSM:
                        return 78326;
                    case MtkRILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM:
                        return 618998;
                    default:
                        return 1;
                }
                break;
        }
        return LTE;
    }

    private static int getAdjustedRaf(int i) {
        if ((GSM & i) > 0) {
            i |= GSM;
        }
        if ((WCDMA & i) > 0) {
            i |= WCDMA;
        }
        if ((CDMA & i) > 0) {
            i |= CDMA;
        }
        if ((EVDO & i) > 0) {
            i |= EVDO;
        }
        if ((LTE & i) > 0) {
            return i | LTE;
        }
        return i;
    }

    public static int getNetworkTypeFromRaf(int i) {
        switch (getAdjustedRaf(i)) {
            case CDMA:
                return 5;
            case EVDO:
                return 6;
            case 12784:
                return 4;
            case GSM:
                return 1;
            case 65654:
                return 32;
            case 78326:
                return 33;
            case RAF_TD_SCDMA:
                return 13;
            case WCDMA:
                return 2;
            case 196614:
                return 16;
            case 232974:
                return 0;
            case 245758:
                return 7;
            case LTE:
                return 11;
            case 553456:
                return 8;
            case 606214:
                return 30;
            case 618998:
                return 34;
            case 671744:
                return 15;
            case 708104:
                return 12;
            case 737286:
                return 17;
            case 773646:
                return 9;
            case 786430:
                return 10;
            default:
                return RILConstants.PREFERRED_NETWORK_MODE;
        }
    }
}
