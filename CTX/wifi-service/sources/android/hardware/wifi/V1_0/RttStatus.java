package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class RttStatus {
    public static final int ABORTED = 8;
    public static final int FAILURE = 1;
    public static final int FAIL_AP_ON_DIFF_CHANNEL = 6;
    public static final int FAIL_BUSY_TRY_LATER = 12;
    public static final int FAIL_FTM_PARAM_OVERRIDE = 15;
    public static final int FAIL_INVALID_TS = 9;
    public static final int FAIL_NOT_SCHEDULED_YET = 4;
    public static final int FAIL_NO_CAPABILITY = 7;
    public static final int FAIL_NO_RSP = 2;
    public static final int FAIL_PROTOCOL = 10;
    public static final int FAIL_REJECTED = 3;
    public static final int FAIL_SCHEDULE = 11;
    public static final int FAIL_TM_TIMEOUT = 5;
    public static final int INVALID_REQ = 13;
    public static final int NO_WIFI = 14;
    public static final int SUCCESS = 0;

    public static final String toString(int i) {
        if (i == 0) {
            return "SUCCESS";
        }
        if (i == 1) {
            return "FAILURE";
        }
        if (i == 2) {
            return "FAIL_NO_RSP";
        }
        if (i == 3) {
            return "FAIL_REJECTED";
        }
        if (i == 4) {
            return "FAIL_NOT_SCHEDULED_YET";
        }
        if (i == 5) {
            return "FAIL_TM_TIMEOUT";
        }
        if (i == 6) {
            return "FAIL_AP_ON_DIFF_CHANNEL";
        }
        if (i == 7) {
            return "FAIL_NO_CAPABILITY";
        }
        if (i == 8) {
            return "ABORTED";
        }
        if (i == 9) {
            return "FAIL_INVALID_TS";
        }
        if (i == 10) {
            return "FAIL_PROTOCOL";
        }
        if (i == 11) {
            return "FAIL_SCHEDULE";
        }
        if (i == 12) {
            return "FAIL_BUSY_TRY_LATER";
        }
        if (i == 13) {
            return "INVALID_REQ";
        }
        if (i == 14) {
            return "NO_WIFI";
        }
        if (i == 15) {
            return "FAIL_FTM_PARAM_OVERRIDE";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("SUCCESS");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("FAILURE");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("FAIL_NO_RSP");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("FAIL_REJECTED");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("FAIL_NOT_SCHEDULED_YET");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("FAIL_TM_TIMEOUT");
            i2 |= 5;
        }
        if ((i & 6) == 6) {
            arrayList.add("FAIL_AP_ON_DIFF_CHANNEL");
            i2 |= 6;
        }
        if ((i & 7) == 7) {
            arrayList.add("FAIL_NO_CAPABILITY");
            i2 |= 7;
        }
        if ((i & 8) == 8) {
            arrayList.add("ABORTED");
            i2 |= 8;
        }
        if ((i & 9) == 9) {
            arrayList.add("FAIL_INVALID_TS");
            i2 |= 9;
        }
        if ((i & 10) == 10) {
            arrayList.add("FAIL_PROTOCOL");
            i2 |= 10;
        }
        if ((i & 11) == 11) {
            arrayList.add("FAIL_SCHEDULE");
            i2 |= 11;
        }
        if ((i & 12) == 12) {
            arrayList.add("FAIL_BUSY_TRY_LATER");
            i2 |= 12;
        }
        if ((i & 13) == 13) {
            arrayList.add("INVALID_REQ");
            i2 |= 13;
        }
        if ((i & 14) == 14) {
            arrayList.add("NO_WIFI");
            i2 |= 14;
        }
        if ((i & 15) == 15) {
            arrayList.add("FAIL_FTM_PARAM_OVERRIDE");
            i2 |= 15;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
