package android.hardware.broadcastradio.V2_0;

import java.util.ArrayList;

public final class IdentifierType {
    public static final int AMFM_FREQUENCY = 1;
    public static final int DAB_ENSEMBLE = 6;
    public static final int DAB_FREQUENCY = 8;
    public static final int DAB_SCID = 7;
    public static final int DAB_SID_EXT = 5;
    public static final int DRMO_FREQUENCY = 10;
    public static final int DRMO_SERVICE_ID = 9;
    public static final int HD_STATION_ID_EXT = 3;
    public static final int HD_STATION_NAME = 4;
    public static final int INVALID = 0;
    public static final int RDS_PI = 2;
    public static final int SXM_CHANNEL = 13;
    public static final int SXM_SERVICE_ID = 12;
    public static final int VENDOR_END = 1999;
    public static final int VENDOR_START = 1000;

    public static final String toString(int i) {
        if (i == 1000) {
            return "VENDOR_START";
        }
        if (i == 1999) {
            return "VENDOR_END";
        }
        if (i == 0) {
            return "INVALID";
        }
        if (i == 1) {
            return "AMFM_FREQUENCY";
        }
        if (i == 2) {
            return "RDS_PI";
        }
        if (i == 3) {
            return "HD_STATION_ID_EXT";
        }
        if (i == 4) {
            return "HD_STATION_NAME";
        }
        if (i == 5) {
            return "DAB_SID_EXT";
        }
        if (i == 6) {
            return "DAB_ENSEMBLE";
        }
        if (i == 7) {
            return "DAB_SCID";
        }
        if (i == 8) {
            return "DAB_FREQUENCY";
        }
        if (i == 9) {
            return "DRMO_SERVICE_ID";
        }
        if (i == 10) {
            return "DRMO_FREQUENCY";
        }
        if (i == 12) {
            return "SXM_SERVICE_ID";
        }
        if (i == 13) {
            return "SXM_CHANNEL";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1000;
        if ((i & 1000) == 1000) {
            arrayList.add("VENDOR_START");
        } else {
            i2 = 0;
        }
        if ((i & VENDOR_END) == 1999) {
            arrayList.add("VENDOR_END");
            i2 |= VENDOR_END;
        }
        arrayList.add("INVALID");
        if ((i & 1) == 1) {
            arrayList.add("AMFM_FREQUENCY");
            i2 |= 1;
        }
        if ((i & 2) == 2) {
            arrayList.add("RDS_PI");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("HD_STATION_ID_EXT");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("HD_STATION_NAME");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("DAB_SID_EXT");
            i2 |= 5;
        }
        if ((i & 6) == 6) {
            arrayList.add("DAB_ENSEMBLE");
            i2 |= 6;
        }
        if ((i & 7) == 7) {
            arrayList.add("DAB_SCID");
            i2 |= 7;
        }
        if ((i & 8) == 8) {
            arrayList.add("DAB_FREQUENCY");
            i2 |= 8;
        }
        if ((i & 9) == 9) {
            arrayList.add("DRMO_SERVICE_ID");
            i2 |= 9;
        }
        if ((i & 10) == 10) {
            arrayList.add("DRMO_FREQUENCY");
            i2 |= 10;
        }
        if ((i & 12) == 12) {
            arrayList.add("SXM_SERVICE_ID");
            i2 |= 12;
        }
        if ((i & 13) == 13) {
            arrayList.add("SXM_CHANNEL");
            i2 |= 13;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
