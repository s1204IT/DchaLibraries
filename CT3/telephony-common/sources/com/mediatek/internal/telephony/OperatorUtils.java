package com.mediatek.internal.telephony;

import android.telephony.Rlog;
import com.android.internal.telephony.test.SimulatedCommands;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperatorUtils {
    private static final String TAG = "OperatorUtils";
    private static final Map<OPID, List> mOPMap = new HashMap<OPID, List>() {
        {
            put(OPID.OP01, Arrays.asList("46000", "46002", "46004", "46007", "46008"));
            put(OPID.OP03, Arrays.asList("20801", "20802"));
            put(OPID.OP05, Arrays.asList("26201", "26206", "26278"));
            put(OPID.OP06, Arrays.asList("20205", "20404", "21401", "21406", "21670", "22210", "22601", "23415", "23591", "26202", "26204", "26209", "26801", "27201", "27402", "27403", "27801", "28602", "90128"));
            put(OPID.OP07, Arrays.asList("31030", "31070", "31080", "31090", "310150", "310170", "310280", "310380", "310410", "310560", "310680", "311180"));
            put(OPID.OP08, Arrays.asList("20416", "20420", "21630", "21901", "22004", "23001", "23203", "23204", "23430", "26002", "29702", "310160", SimulatedCommands.FAKE_MCC_MNC, "310490", "310580", "310660"));
            put(OPID.OP11, Arrays.asList("23420", "23594"));
            put(OPID.OP15, Arrays.asList("23410", "26203", "26207", "26208", "26211", "26277"));
            put(OPID.OP18, Arrays.asList("405840", "405854", "405855", "405856", "405857", "405858", "405859", "405860", "405861", "405862", "405863", "405864", "405865", "405866", "405867", "405868", "405869", "405870", "405871", "405872", "405873", "405874"));
            put(OPID.OP50, Arrays.asList("44020"));
            put(OPID.OP100, Arrays.asList("45400", "45402", "45410", "45418"));
            put(OPID.OP101, Arrays.asList("45416", "45419", "45420", "45429"));
            put(OPID.OP102, Arrays.asList("45406", "45415", "45417", "45500", "45506"));
            put(OPID.OP106, Arrays.asList("45403", "45404", "45405"));
            put(OPID.OP107, Arrays.asList("20809", "20810", "20811", "20813"));
            put(OPID.OP108, Arrays.asList("46697"));
            put(OPID.OP110, Arrays.asList("46601", "46602", "46603", "46606", "46607", "46688"));
            put(OPID.OP131, Arrays.asList("52004", "52099"));
        }
    };
    private static final List<String> mNotSupportXcapList = Arrays.asList("22210", "23003", "23099", "28602");
    private static final List<String> mTbClirList = Arrays.asList("23415", "23591", "26202", "26204", "26209", "25001", "25002");

    public enum OPID {
        OP01,
        OP03,
        OP05,
        OP06,
        OP07,
        OP08,
        OP11,
        OP15,
        OP18,
        OP50,
        OP100,
        OP101,
        OP102,
        OP106,
        OP107,
        OP108,
        OP110,
        OP131;

        public static OPID[] valuesCustom() {
            return values();
        }
    }

    public static boolean isOperator(String mccMnc, OPID id) {
        boolean r = false;
        if (mOPMap.get(id).contains(mccMnc)) {
            r = true;
        }
        Rlog.d(TAG, UsimPBMemInfo.STRING_NOT_SET + mccMnc + (r ? " = " : " != ") + idToString(id));
        return r;
    }

    public static boolean isGsmUtSupport(String mccMnc) {
        boolean r = mOPMap.get(OPID.OP01).contains(mccMnc) || mOPMap.get(OPID.OP03).contains(mccMnc) || mOPMap.get(OPID.OP05).contains(mccMnc) || mOPMap.get(OPID.OP06).contains(mccMnc) || mOPMap.get(OPID.OP07).contains(mccMnc) || mOPMap.get(OPID.OP15).contains(mccMnc) || mOPMap.get(OPID.OP18).contains(mccMnc) || mOPMap.get(OPID.OP50).contains(mccMnc);
        Rlog.d(TAG, "isGsmUtSupport: " + r + ", " + mccMnc);
        return r;
    }

    public static boolean isNotSupportXcap(String mccMnc) {
        boolean r = mNotSupportXcapList.contains(mccMnc) || mOPMap.get(OPID.OP100).contains(mccMnc) || mOPMap.get(OPID.OP101).contains(mccMnc) || mOPMap.get(OPID.OP102).contains(mccMnc) || mOPMap.get(OPID.OP106).contains(mccMnc) || mOPMap.get(OPID.OP108).contains(mccMnc) || mOPMap.get(OPID.OP110).contains(mccMnc) || mOPMap.get(OPID.OP131).contains(mccMnc);
        Rlog.d(TAG, "isNotSupportXcap: " + r + ", " + mccMnc);
        return r;
    }

    public static boolean isTbClir(String mccMnc) {
        boolean r = mTbClirList.contains(mccMnc) || mOPMap.get(OPID.OP03).contains(mccMnc) || mOPMap.get(OPID.OP05).contains(mccMnc) || mOPMap.get(OPID.OP07).contains(mccMnc) || mOPMap.get(OPID.OP08).contains(mccMnc) || mOPMap.get(OPID.OP50).contains(mccMnc) || mOPMap.get(OPID.OP107).contains(mccMnc);
        Rlog.d(TAG, "isTbClir: " + r + ", " + mccMnc);
        return r;
    }

    private static String idToString(OPID id) {
        if (id == OPID.OP01) {
            return "OP01";
        }
        if (id == OPID.OP03) {
            return "OP03";
        }
        if (id == OPID.OP05) {
            return "OP05";
        }
        if (id == OPID.OP06) {
            return "OP06";
        }
        if (id == OPID.OP07) {
            return "OP07";
        }
        if (id == OPID.OP08) {
            return "OP08";
        }
        if (id == OPID.OP11) {
            return "OP11";
        }
        if (id == OPID.OP15) {
            return "OP15";
        }
        if (id == OPID.OP18) {
            return "OP18";
        }
        if (id == OPID.OP50) {
            return "OP50";
        }
        if (id == OPID.OP100) {
            return "OP100";
        }
        if (id == OPID.OP101) {
            return "OP101";
        }
        if (id == OPID.OP102) {
            return "OP102";
        }
        if (id == OPID.OP106) {
            return "OP106";
        }
        if (id == OPID.OP107) {
            return "OP107";
        }
        if (id == OPID.OP108) {
            return "OP108";
        }
        if (id == OPID.OP110) {
            return "OP110";
        }
        if (id == OPID.OP131) {
            return "OP131";
        }
        return "ERR";
    }
}
