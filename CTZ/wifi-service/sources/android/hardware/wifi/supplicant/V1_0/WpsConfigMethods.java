package android.hardware.wifi.supplicant.V1_0;

import java.util.ArrayList;

public final class WpsConfigMethods {
    public static final short DISPLAY = 8;
    public static final short ETHERNET = 2;
    public static final short EXT_NFC_TOKEN = 16;
    public static final short INT_NFC_TOKEN = 32;
    public static final short KEYPAD = 256;
    public static final short LABEL = 4;
    public static final short NFC_INTERFACE = 64;
    public static final short P2PS = 4096;
    public static final short PHY_DISPLAY = 16392;
    public static final short PHY_PUSHBUTTON = 1152;
    public static final short PUSHBUTTON = 128;
    public static final short USBA = 1;
    public static final short VIRT_DISPLAY = 8200;
    public static final short VIRT_PUSHBUTTON = 640;

    public static final String toString(short s) {
        if (s == 1) {
            return "USBA";
        }
        if (s == 2) {
            return "ETHERNET";
        }
        if (s == 4) {
            return "LABEL";
        }
        if (s == 8) {
            return "DISPLAY";
        }
        if (s == 16) {
            return "EXT_NFC_TOKEN";
        }
        if (s == 32) {
            return "INT_NFC_TOKEN";
        }
        if (s == 64) {
            return "NFC_INTERFACE";
        }
        if (s == 128) {
            return "PUSHBUTTON";
        }
        if (s == 256) {
            return "KEYPAD";
        }
        if (s == 640) {
            return "VIRT_PUSHBUTTON";
        }
        if (s == 1152) {
            return "PHY_PUSHBUTTON";
        }
        if (s == 4096) {
            return "P2PS";
        }
        if (s == 8200) {
            return "VIRT_DISPLAY";
        }
        if (s == 16392) {
            return "PHY_DISPLAY";
        }
        return "0x" + Integer.toHexString(Short.toUnsignedInt(s));
    }

    public static final String dumpBitfield(short s) {
        short s2;
        ArrayList arrayList = new ArrayList();
        if ((s & 1) == 1) {
            arrayList.add("USBA");
            s2 = (short) 1;
        } else {
            s2 = 0;
        }
        if ((s & 2) == 2) {
            arrayList.add("ETHERNET");
            s2 = (short) (s2 | 2);
        }
        if ((s & 4) == 4) {
            arrayList.add("LABEL");
            s2 = (short) (s2 | 4);
        }
        if ((s & 8) == 8) {
            arrayList.add("DISPLAY");
            s2 = (short) (s2 | 8);
        }
        if ((s & 16) == 16) {
            arrayList.add("EXT_NFC_TOKEN");
            s2 = (short) (s2 | 16);
        }
        if ((s & 32) == 32) {
            arrayList.add("INT_NFC_TOKEN");
            s2 = (short) (s2 | 32);
        }
        if ((s & 64) == 64) {
            arrayList.add("NFC_INTERFACE");
            s2 = (short) (s2 | 64);
        }
        if ((s & PUSHBUTTON) == 128) {
            arrayList.add("PUSHBUTTON");
            s2 = (short) (s2 | PUSHBUTTON);
        }
        if ((s & KEYPAD) == 256) {
            arrayList.add("KEYPAD");
            s2 = (short) (s2 | KEYPAD);
        }
        if ((s & VIRT_PUSHBUTTON) == 640) {
            arrayList.add("VIRT_PUSHBUTTON");
            s2 = (short) (s2 | VIRT_PUSHBUTTON);
        }
        if ((s & PHY_PUSHBUTTON) == 1152) {
            arrayList.add("PHY_PUSHBUTTON");
            s2 = (short) (s2 | PHY_PUSHBUTTON);
        }
        if ((s & P2PS) == 4096) {
            arrayList.add("P2PS");
            s2 = (short) (s2 | P2PS);
        }
        if ((s & VIRT_DISPLAY) == 8200) {
            arrayList.add("VIRT_DISPLAY");
            s2 = (short) (s2 | VIRT_DISPLAY);
        }
        if ((s & PHY_DISPLAY) == 16392) {
            arrayList.add("PHY_DISPLAY");
            s2 = (short) (s2 | PHY_DISPLAY);
        }
        if (s != s2) {
            arrayList.add("0x" + Integer.toHexString(Short.toUnsignedInt((short) (s & (~s2)))));
        }
        return String.join(" | ", arrayList);
    }
}
