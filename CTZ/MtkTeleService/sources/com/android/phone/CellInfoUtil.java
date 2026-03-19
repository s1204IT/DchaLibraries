package com.android.phone;

import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.OperatorInfo;
import java.util.Iterator;
import java.util.List;

public final class CellInfoUtil {
    private static final String TAG = "NetworkSelectSetting";

    private CellInfoUtil() {
    }

    public static int getNetworkType(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoLte) {
            return 13;
        }
        if (cellInfo instanceof CellInfoWcdma) {
            return 3;
        }
        if (cellInfo instanceof CellInfoGsm) {
            return 16;
        }
        if (cellInfo instanceof CellInfoCdma) {
            return 4;
        }
        Log.e(TAG, "Invalid CellInfo type");
        return 0;
    }

    public static int getLevel(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoLte) {
            return ((CellInfoLte) cellInfo).getCellSignalStrength().getLevel();
        }
        if (cellInfo instanceof CellInfoWcdma) {
            return ((CellInfoWcdma) cellInfo).getCellSignalStrength().getLevel();
        }
        if (cellInfo instanceof CellInfoGsm) {
            return ((CellInfoGsm) cellInfo).getCellSignalStrength().getLevel();
        }
        if (cellInfo instanceof CellInfoCdma) {
            return ((CellInfoCdma) cellInfo).getCellSignalStrength().getLevel();
        }
        Log.e(TAG, "Invalid CellInfo type");
        return 0;
    }

    public static CellInfo wrapCellInfoWithCellIdentity(CellIdentity cellIdentity) {
        if (cellIdentity instanceof CellIdentityLte) {
            CellInfoLte cellInfoLte = new CellInfoLte();
            cellInfoLte.setCellIdentity((CellIdentityLte) cellIdentity);
            return cellInfoLte;
        }
        if (cellIdentity instanceof CellIdentityCdma) {
            CellInfoCdma cellInfoCdma = new CellInfoCdma();
            cellInfoCdma.setCellIdentity((CellIdentityCdma) cellIdentity);
            return cellInfoCdma;
        }
        if (cellIdentity instanceof CellIdentityWcdma) {
            CellInfoWcdma cellInfoWcdma = new CellInfoWcdma();
            cellInfoWcdma.setCellIdentity((CellIdentityWcdma) cellIdentity);
            return cellInfoWcdma;
        }
        if (cellIdentity instanceof CellIdentityGsm) {
            CellInfoGsm cellInfoGsm = new CellInfoGsm();
            cellInfoGsm.setCellIdentity((CellIdentityGsm) cellIdentity);
            return cellInfoGsm;
        }
        Log.e(TAG, "Invalid CellInfo type");
        return null;
    }

    public static String getNetworkTitle(CellInfo cellInfo) {
        OperatorInfo operatorInfoFromCellInfo = getOperatorInfoFromCellInfo(cellInfo);
        if (!TextUtils.isEmpty(operatorInfoFromCellInfo.getOperatorAlphaLong())) {
            return operatorInfoFromCellInfo.getOperatorAlphaLong();
        }
        if (!TextUtils.isEmpty(operatorInfoFromCellInfo.getOperatorAlphaShort())) {
            return operatorInfoFromCellInfo.getOperatorAlphaShort();
        }
        return BidiFormatter.getInstance().unicodeWrap(operatorInfoFromCellInfo.getOperatorNumeric(), TextDirectionHeuristics.LTR);
    }

    public static OperatorInfo getOperatorInfoFromCellInfo(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoLte) {
            CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
            return new OperatorInfo((String) cellInfoLte.getCellIdentity().getOperatorAlphaLong(), (String) cellInfoLte.getCellIdentity().getOperatorAlphaShort(), cellInfoLte.getCellIdentity().getMobileNetworkOperator());
        }
        if (cellInfo instanceof CellInfoWcdma) {
            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo;
            return new OperatorInfo((String) cellInfoWcdma.getCellIdentity().getOperatorAlphaLong(), (String) cellInfoWcdma.getCellIdentity().getOperatorAlphaShort(), cellInfoWcdma.getCellIdentity().getMobileNetworkOperator());
        }
        if (cellInfo instanceof CellInfoGsm) {
            CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
            return new OperatorInfo((String) cellInfoGsm.getCellIdentity().getOperatorAlphaLong(), (String) cellInfoGsm.getCellIdentity().getOperatorAlphaShort(), cellInfoGsm.getCellIdentity().getMobileNetworkOperator());
        }
        if (cellInfo instanceof CellInfoCdma) {
            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
            return new OperatorInfo((String) cellInfoCdma.getCellIdentity().getOperatorAlphaLong(), (String) cellInfoCdma.getCellIdentity().getOperatorAlphaShort(), "");
        }
        Log.e(TAG, "Invalid CellInfo type");
        return new OperatorInfo("", "", "");
    }

    public static boolean isForbidden(CellInfo cellInfo, List<String> list) {
        String operatorNumeric = getOperatorInfoFromCellInfo(cellInfo).getOperatorNumeric();
        if (list == null) {
            return false;
        }
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().indexOf(operatorNumeric) > -1) {
                Log.d(TAG, "Forbidden plmn: " + operatorNumeric);
                return true;
            }
        }
        return false;
    }
}
