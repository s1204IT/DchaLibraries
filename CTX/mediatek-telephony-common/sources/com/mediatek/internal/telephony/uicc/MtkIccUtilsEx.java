package com.mediatek.internal.telephony.uicc;

import android.os.SystemProperties;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.uicc.IccUtils;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.Arrays;

public class MtkIccUtilsEx extends IccUtils {
    public static final int CDMA_CARD_TYPE_NOT_3GCARD = 0;
    public static final int CDMA_CARD_TYPE_RUIM_SIM = 2;
    public static final int CDMA_CARD_TYPE_UIM_ONLY = 1;
    static final String MTK_LOG_TAG = "MtkIccUtilsEx";
    protected static final int TAG_FULL_NETWORK_NAME = 67;
    protected static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
    protected static final String[] PROPERTY_RIL_CT3G = {"vendor.gsm.ril.ct3g", "vendor.gsm.ril.ct3g.2", "vendor.gsm.ril.ct3g.3", "vendor.gsm.ril.ct3g.4"};

    public static String parseSpnToString(int i, byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        if (1 == i) {
            return IccUtils.adnStringFieldToString(bArr, 1, bArr.length - 1);
        }
        if (2 == i) {
            byte b = bArr[1];
            byte b2 = bArr[2];
            byte[] bArr2 = new byte[32];
            System.arraycopy(bArr, 3, bArr2, 0, bArr.length - 3 < 32 ? bArr.length - 3 : 32);
            int i2 = 0;
            while (i2 < bArr2.length && (bArr2[i2] & PplMessageManager.Type.INVALID) != 255) {
                i2++;
            }
            if (i2 == 0) {
                return "";
            }
            try {
                if (b != 0) {
                    switch (b) {
                        case 2:
                            String str = new String(bArr2, 0, i2, "US-ASCII");
                            if (!TextUtils.isPrintableAsciiOnly(str)) {
                                return GsmAlphabet.gsm7BitPackedToString(bArr2, 0, (i2 * 8) / 7);
                            }
                            return str;
                        case 3:
                            return GsmAlphabet.gsm7BitPackedToString(bArr2, 0, (i2 * 8) / 7);
                        case 4:
                            return new String(bArr2, 0, i2, "utf-16");
                        default:
                            switch (b) {
                                case 8:
                                    break;
                                case 9:
                                    break;
                                default:
                                    Rlog.d(MTK_LOG_TAG, "spn decode error: " + ((int) b));
                                    break;
                            }
                            break;
                    }
                }
                return new String(bArr2, 0, i2, "ISO-8859-1");
            } catch (Exception e) {
                Rlog.d(MTK_LOG_TAG, "spn decode error: " + e);
            }
        }
        return null;
    }

    public static String parsePnnToString(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        SimTlv simTlv = new SimTlv(bArr, 0, bArr.length);
        while (simTlv.isValidObject()) {
            if (simTlv.getTag() != TAG_FULL_NETWORK_NAME) {
                simTlv.nextObject();
            } else {
                return networkNameToString(simTlv.getData(), 0, simTlv.getData().length);
            }
        }
        return null;
    }

    public static int checkCdma3gCard(int i) {
        String[] strArrSplit;
        int i2 = -1;
        if (i < 0 || i >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            Rlog.d(MTK_LOG_TAG, "checkCdma3gCard: invalid slotId " + i);
            return -1;
        }
        String str = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[i]);
        if (str != null && str.length() > 0) {
            strArrSplit = str.split(",");
        } else {
            strArrSplit = null;
        }
        if (strArrSplit != null) {
            if (Arrays.asList(strArrSplit).contains("RUIM") && Arrays.asList(strArrSplit).contains("SIM")) {
                i2 = 2;
            } else if ((!Arrays.asList(strArrSplit).contains("USIM") && !Arrays.asList(strArrSplit).contains("SIM")) || (Arrays.asList(strArrSplit).contains("SIM") && "1".equals(SystemProperties.get(PROPERTY_RIL_CT3G[i])))) {
                i2 = 1;
            } else {
                i2 = 0;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("checkCdma3gCard slotId ");
        sb.append(i);
        sb.append(", prop value = ");
        sb.append(str);
        sb.append(", size = ");
        sb.append(strArrSplit != null ? strArrSplit.length : 0);
        sb.append(", cdma3gCardType = ");
        sb.append(i2);
        Rlog.d(MTK_LOG_TAG, sb.toString());
        return i2;
    }
}
