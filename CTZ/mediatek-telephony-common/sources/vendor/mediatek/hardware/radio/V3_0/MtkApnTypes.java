package vendor.mediatek.hardware.radio.V3_0;

import java.util.ArrayList;

public final class MtkApnTypes {
    public static final int ALL = 1023;
    public static final int BIP = 8192;
    public static final int CBS = 128;
    public static final int DEFAULT = 1;
    public static final int DUN = 8;
    public static final int EMERGENCY = 512;
    public static final int FOTA = 32;
    public static final int HIPRI = 16;
    public static final int IA = 256;
    public static final int IMS = 64;
    public static final int MMS = 2;
    public static final int MTKALL = 32767;
    public static final int NONE = 0;
    public static final int RCS = 4096;
    public static final int SUPL = 4;
    public static final int VSIM = 16384;
    public static final int WAP = 1024;
    public static final int XCAP = 2048;

    public static final String toString(int i) {
        if (i == 0) {
            return "NONE";
        }
        if (i == 1) {
            return "DEFAULT";
        }
        if (i == 2) {
            return "MMS";
        }
        if (i == 4) {
            return "SUPL";
        }
        if (i == 8) {
            return "DUN";
        }
        if (i == 16) {
            return "HIPRI";
        }
        if (i == 32) {
            return "FOTA";
        }
        if (i == 64) {
            return "IMS";
        }
        if (i == 128) {
            return "CBS";
        }
        if (i == 256) {
            return "IA";
        }
        if (i == 512) {
            return "EMERGENCY";
        }
        if (i == 1023) {
            return "ALL";
        }
        if (i == 1024) {
            return "WAP";
        }
        if (i == 2048) {
            return "XCAP";
        }
        if (i == 4096) {
            return "RCS";
        }
        if (i == 8192) {
            return "BIP";
        }
        if (i == 16384) {
            return "VSIM";
        }
        if (i == 32767) {
            return "MTKALL";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("NONE");
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("DEFAULT");
        } else {
            i2 = 0;
        }
        if ((i & 2) == 2) {
            arrayList.add("MMS");
            i2 |= 2;
        }
        if ((i & 4) == 4) {
            arrayList.add("SUPL");
            i2 |= 4;
        }
        if ((i & 8) == 8) {
            arrayList.add("DUN");
            i2 |= 8;
        }
        if ((i & 16) == 16) {
            arrayList.add("HIPRI");
            i2 |= 16;
        }
        if ((i & 32) == 32) {
            arrayList.add("FOTA");
            i2 |= 32;
        }
        if ((i & 64) == 64) {
            arrayList.add("IMS");
            i2 |= 64;
        }
        if ((i & 128) == 128) {
            arrayList.add("CBS");
            i2 |= 128;
        }
        if ((i & 256) == 256) {
            arrayList.add("IA");
            i2 |= 256;
        }
        if ((i & 512) == 512) {
            arrayList.add("EMERGENCY");
            i2 |= 512;
        }
        if ((i & 1023) == 1023) {
            arrayList.add("ALL");
            i2 |= 1023;
        }
        if ((i & 1024) == 1024) {
            arrayList.add("WAP");
            i2 |= 1024;
        }
        if ((i & 2048) == 2048) {
            arrayList.add("XCAP");
            i2 |= 2048;
        }
        if ((i & 4096) == 4096) {
            arrayList.add("RCS");
            i2 |= 4096;
        }
        if ((i & 8192) == 8192) {
            arrayList.add("BIP");
            i2 |= 8192;
        }
        if ((i & 16384) == 16384) {
            arrayList.add("VSIM");
            i2 |= 16384;
        }
        if ((i & MTKALL) == 32767) {
            arrayList.add("MTKALL");
            i2 |= MTKALL;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
