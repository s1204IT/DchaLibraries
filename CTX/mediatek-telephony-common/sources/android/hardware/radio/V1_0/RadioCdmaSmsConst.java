package android.hardware.radio.V1_0;

import java.util.ArrayList;

public final class RadioCdmaSmsConst {
    public static final int ADDRESS_MAX = 36;
    public static final int BEARER_DATA_MAX = 255;
    public static final int IP_ADDRESS_SIZE = 4;
    public static final int MAX_UD_HEADERS = 7;
    public static final int SUBADDRESS_MAX = 36;
    public static final int UDH_ANIM_NUM_BITMAPS = 4;
    public static final int UDH_EO_DATA_SEGMENT_MAX = 131;
    public static final int UDH_LARGE_BITMAP_SIZE = 32;
    public static final int UDH_LARGE_PIC_SIZE = 128;
    public static final int UDH_MAX_SND_SIZE = 128;
    public static final int UDH_OTHER_SIZE = 226;
    public static final int UDH_SMALL_BITMAP_SIZE = 8;
    public static final int UDH_SMALL_PIC_SIZE = 32;
    public static final int UDH_VAR_PIC_SIZE = 134;
    public static final int USER_DATA_MAX = 229;

    public static final String toString(int i) {
        if (i == 36) {
            return "ADDRESS_MAX";
        }
        if (i == 36) {
            return "SUBADDRESS_MAX";
        }
        if (i == 255) {
            return "BEARER_DATA_MAX";
        }
        if (i == 128) {
            return "UDH_MAX_SND_SIZE";
        }
        if (i == 131) {
            return "UDH_EO_DATA_SEGMENT_MAX";
        }
        if (i == 7) {
            return "MAX_UD_HEADERS";
        }
        if (i == 229) {
            return "USER_DATA_MAX";
        }
        if (i == 128) {
            return "UDH_LARGE_PIC_SIZE";
        }
        if (i == 32) {
            return "UDH_SMALL_PIC_SIZE";
        }
        if (i == 134) {
            return "UDH_VAR_PIC_SIZE";
        }
        if (i == 4) {
            return "UDH_ANIM_NUM_BITMAPS";
        }
        if (i == 32) {
            return "UDH_LARGE_BITMAP_SIZE";
        }
        if (i == 8) {
            return "UDH_SMALL_BITMAP_SIZE";
        }
        if (i == 226) {
            return "UDH_OTHER_SIZE";
        }
        if (i == 4) {
            return "IP_ADDRESS_SIZE";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        int i2;
        ArrayList arrayList = new ArrayList();
        int i3 = i & 36;
        if (i3 == 36) {
            arrayList.add("ADDRESS_MAX");
            i2 = 36;
        } else {
            i2 = 0;
        }
        if (i3 == 36) {
            arrayList.add("SUBADDRESS_MAX");
            i2 |= 36;
        }
        if ((i & 255) == 255) {
            arrayList.add("BEARER_DATA_MAX");
            i2 |= 255;
        }
        int i4 = i & 128;
        if (i4 == 128) {
            arrayList.add("UDH_MAX_SND_SIZE");
            i2 |= 128;
        }
        if ((i & UDH_EO_DATA_SEGMENT_MAX) == 131) {
            arrayList.add("UDH_EO_DATA_SEGMENT_MAX");
            i2 |= UDH_EO_DATA_SEGMENT_MAX;
        }
        if ((i & 7) == 7) {
            arrayList.add("MAX_UD_HEADERS");
            i2 |= 7;
        }
        if ((i & USER_DATA_MAX) == 229) {
            arrayList.add("USER_DATA_MAX");
            i2 |= USER_DATA_MAX;
        }
        if (i4 == 128) {
            arrayList.add("UDH_LARGE_PIC_SIZE");
            i2 |= 128;
        }
        int i5 = i & 32;
        if (i5 == 32) {
            arrayList.add("UDH_SMALL_PIC_SIZE");
            i2 |= 32;
        }
        if ((i & UDH_VAR_PIC_SIZE) == 134) {
            arrayList.add("UDH_VAR_PIC_SIZE");
            i2 |= UDH_VAR_PIC_SIZE;
        }
        int i6 = i & 4;
        if (i6 == 4) {
            arrayList.add("UDH_ANIM_NUM_BITMAPS");
            i2 |= 4;
        }
        if (i5 == 32) {
            arrayList.add("UDH_LARGE_BITMAP_SIZE");
            i2 |= 32;
        }
        if ((i & 8) == 8) {
            arrayList.add("UDH_SMALL_BITMAP_SIZE");
            i2 |= 8;
        }
        if ((i & UDH_OTHER_SIZE) == 226) {
            arrayList.add("UDH_OTHER_SIZE");
            i2 |= UDH_OTHER_SIZE;
        }
        if (i6 == 4) {
            arrayList.add("IP_ADDRESS_SIZE");
            i2 |= 4;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
