package android.hardware.broadcastradio.V2_0;

import java.util.ArrayList;

public final class MetadataKey {
    public static final int ALBUM_ART = 9;
    public static final int DAB_COMPONENT_NAME = 15;
    public static final int DAB_COMPONENT_NAME_SHORT = 16;
    public static final int DAB_ENSEMBLE_NAME = 11;
    public static final int DAB_ENSEMBLE_NAME_SHORT = 12;
    public static final int DAB_SERVICE_NAME = 13;
    public static final int DAB_SERVICE_NAME_SHORT = 14;
    public static final int PROGRAM_NAME = 10;
    public static final int RBDS_PTY = 3;
    public static final int RDS_PS = 1;
    public static final int RDS_PTY = 2;
    public static final int RDS_RT = 4;
    public static final int SONG_ALBUM = 7;
    public static final int SONG_ARTIST = 6;
    public static final int SONG_TITLE = 5;
    public static final int STATION_ICON = 8;

    public static final String toString(int i) {
        if (i == 1) {
            return "RDS_PS";
        }
        if (i == 2) {
            return "RDS_PTY";
        }
        if (i == 3) {
            return "RBDS_PTY";
        }
        if (i == 4) {
            return "RDS_RT";
        }
        if (i == 5) {
            return "SONG_TITLE";
        }
        if (i == 6) {
            return "SONG_ARTIST";
        }
        if (i == 7) {
            return "SONG_ALBUM";
        }
        if (i == 8) {
            return "STATION_ICON";
        }
        if (i == 9) {
            return "ALBUM_ART";
        }
        if (i == 10) {
            return "PROGRAM_NAME";
        }
        if (i == 11) {
            return "DAB_ENSEMBLE_NAME";
        }
        if (i == 12) {
            return "DAB_ENSEMBLE_NAME_SHORT";
        }
        if (i == 13) {
            return "DAB_SERVICE_NAME";
        }
        if (i == 14) {
            return "DAB_SERVICE_NAME_SHORT";
        }
        if (i == 15) {
            return "DAB_COMPONENT_NAME";
        }
        if (i == 16) {
            return "DAB_COMPONENT_NAME_SHORT";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("RDS_PS");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("RDS_PTY");
            i2 |= 2;
        }
        if ((i & 3) == 3) {
            arrayList.add("RBDS_PTY");
            i2 |= 3;
        }
        if ((i & 4) == 4) {
            arrayList.add("RDS_RT");
            i2 |= 4;
        }
        if ((i & 5) == 5) {
            arrayList.add("SONG_TITLE");
            i2 |= 5;
        }
        if ((i & 6) == 6) {
            arrayList.add("SONG_ARTIST");
            i2 |= 6;
        }
        if ((i & 7) == 7) {
            arrayList.add("SONG_ALBUM");
            i2 |= 7;
        }
        if ((i & 8) == 8) {
            arrayList.add("STATION_ICON");
            i2 |= 8;
        }
        if ((i & 9) == 9) {
            arrayList.add("ALBUM_ART");
            i2 |= 9;
        }
        if ((i & 10) == 10) {
            arrayList.add("PROGRAM_NAME");
            i2 |= 10;
        }
        if ((i & 11) == 11) {
            arrayList.add("DAB_ENSEMBLE_NAME");
            i2 |= 11;
        }
        if ((i & 12) == 12) {
            arrayList.add("DAB_ENSEMBLE_NAME_SHORT");
            i2 |= 12;
        }
        if ((i & 13) == 13) {
            arrayList.add("DAB_SERVICE_NAME");
            i2 |= 13;
        }
        if ((i & 14) == 14) {
            arrayList.add("DAB_SERVICE_NAME_SHORT");
            i2 |= 14;
        }
        if ((i & 15) == 15) {
            arrayList.add("DAB_COMPONENT_NAME");
            i2 |= 15;
        }
        if ((i & 16) == 16) {
            arrayList.add("DAB_COMPONENT_NAME_SHORT");
            i2 |= 16;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
