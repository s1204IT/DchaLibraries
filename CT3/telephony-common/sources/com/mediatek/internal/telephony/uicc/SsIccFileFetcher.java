package com.mediatek.internal.telephony.uicc;

import android.content.Context;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccConstants;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public final class SsIccFileFetcher extends IccFileFetcherBase {
    public static final String[] FCNAME = {"Number", "CD", "dCD", "CFB", "vCFB", "dCFB", "actCFB", "dactCFB", "CFD", "vCFD", "dCFD", "actCFD", "dactCFD", "CFNA", "vCFNA", "dCFNA", "actCFNA", "dactCFNA", "CFU", "vCFU", "dCFU", "actCFU", "dactCFU", "CW", "dCW", "CCW", "CNIR", "dCNIR", "CC", "dCC", "DND", "dDND", "aMWN", "daMWN", "MWN", "dMWN", "CMWN", "PACA", "VMR", "CNAP", "dCNAP", "CNAR", "dCNAR", CommandsInterface.CB_FACILITY_BA_MT, "dAC", "AR", "dAR", "USCF", "RUAC", "dRUAC", "AOC", "COT"};
    private static final String SSFC = "ef_ssfc";
    private static final String TAG = "SsIccFileFetcher";
    ArrayList<String> mFileList;

    public SsIccFileFetcher(Context c, Phone phone) {
        super(c, phone);
        this.mFileList = new ArrayList<>();
        this.mFileList.add(SSFC);
    }

    @Override
    public ArrayList<String> onGetKeys() {
        return this.mFileList;
    }

    @Override
    public IccFileRequest onGetFilePara(String key) {
        if (key.equals(SSFC)) {
            return new IccFileRequest(IccConstants.EF_GID2, 1, 0, "3F007F25", null, -1, null);
        }
        return null;
    }

    @Override
    public void onParseResult(String key, byte[] transparent, ArrayList<byte[]> linearfixed) {
        if (!SSFC.equals(key)) {
            return;
        }
        Rlog.d(TAG, "featureCode = " + Arrays.toString(transparent));
        if (transparent == null || transparent.length != 103) {
            return;
        }
        HashMap<String, Object> map = new HashMap<>();
        String number = String.valueOf((int) transparent[0]);
        if ("-1".equals(number)) {
            return;
        }
        map.put(FCNAME[0], number);
        for (int i = 1; i < transparent.length; i += 2) {
            String featureCode = getFcFromRuim(transparent[i], transparent[i + 1]);
            if (!"-1".equals(featureCode)) {
                Rlog.d(TAG, "onParseResult featureCode = " + featureCode);
                map.put(FCNAME[(i / 2) + 1], featureCode);
            }
        }
        this.mData = map;
    }

    private static String getFcFromRuim(byte b1, byte b2) {
        String fc1 = getFcFromRuim(b1);
        String fc2 = getFcFromRuim(b2);
        if (UsimPBMemInfo.STRING_NOT_SET.equals(fc1) && UsimPBMemInfo.STRING_NOT_SET.equals(fc2)) {
            return "-1";
        }
        String featureCode = fc1 + fc2;
        return featureCode;
    }

    private static String getFcFromRuim(byte b) {
        if ((b & PplMessageManager.Type.INVALID) == 255) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        if ((b & 240) == 240) {
            if ((b & 15) <= 9) {
                String ret = UsimPBMemInfo.STRING_NOT_SET + (b & 15);
                return ret;
            }
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        int tmp = 0;
        if ((b & 15) <= 9) {
            tmp = b & 15;
        }
        if ((b & 240) > 144) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        int temp = (b >> 4) & 15;
        if (temp == 0) {
            String ret2 = "0" + tmp;
            return ret2;
        }
        String ret3 = UsimPBMemInfo.STRING_NOT_SET + (tmp + (temp * 10));
        return ret3;
    }

    public int[] getFcsForApp(int start, int end, int subId) {
        Rlog.d(TAG, "getFcsForApp start=" + start + ";end=" + end);
        if (this.mData == null || this.mData.size() < 1 || end < start) {
            return null;
        }
        int size = (end - start) + 1;
        int[] featureCodes = new int[size];
        for (int i = 0; i < size; i++) {
            int index = start + i;
            featureCodes[i] = getFcForApp(index, this.mData);
            Rlog.d(TAG, "getFcsForApp featureCodes=" + featureCodes[i]);
        }
        return featureCodes;
    }

    private int getFcForApp(int index, HashMap<String, Object> fcs) {
        Object code = fcs.get(FCNAME[index]);
        if (code == null) {
            return -1;
        }
        int fc = Integer.parseInt((String) code);
        return fc;
    }
}
