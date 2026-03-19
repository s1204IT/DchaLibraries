package android.telephony;

public class AccessNetworkUtils {
    public static final int INVALID_BAND = -1;

    private AccessNetworkUtils() {
    }

    public static int getDuplexModeForEutranBand(int i) {
        if (i == -1 || i >= 68) {
            return 0;
        }
        if (i >= 65) {
            return 1;
        }
        if (i >= 47) {
            return 0;
        }
        if (i >= 33) {
            return 2;
        }
        return i >= 1 ? 1 : 0;
    }

    public static int getOperatingBandForEarfcn(int i) {
        if (i > 67535 || i >= 67366) {
            return -1;
        }
        if (i >= 66436) {
            return 66;
        }
        if (i >= 65536) {
            return 65;
        }
        if (i > 54339) {
            return -1;
        }
        if (i >= 46790) {
            return 46;
        }
        if (i >= 46590) {
            return 45;
        }
        if (i >= 45590) {
            return 44;
        }
        if (i >= 43590) {
            return 43;
        }
        if (i >= 41590) {
            return 42;
        }
        if (i >= 39650) {
            return 41;
        }
        if (i >= 38650) {
            return 40;
        }
        if (i >= 38250) {
            return 39;
        }
        if (i >= 37750) {
            return 38;
        }
        if (i >= 37550) {
            return 37;
        }
        if (i >= 36950) {
            return 36;
        }
        if (i >= 36350) {
            return 35;
        }
        if (i >= 36200) {
            return 34;
        }
        if (i >= 36000) {
            return 33;
        }
        if (i > 10359 || i >= 9920) {
            return -1;
        }
        if (i >= 9870) {
            return 31;
        }
        if (i >= 9770) {
            return 30;
        }
        if (i >= 9660) {
            return -1;
        }
        if (i >= 9210) {
            return 28;
        }
        if (i >= 9040) {
            return 27;
        }
        if (i >= 8690) {
            return 26;
        }
        if (i >= 8040) {
            return 25;
        }
        if (i >= 7700) {
            return 24;
        }
        if (i >= 7500) {
            return 23;
        }
        if (i >= 6600) {
            return 22;
        }
        if (i >= 6450) {
            return 21;
        }
        if (i >= 6150) {
            return 20;
        }
        if (i >= 6000) {
            return 19;
        }
        if (i >= 5850) {
            return 18;
        }
        if (i >= 5730) {
            return 17;
        }
        if (i > 5379) {
            return -1;
        }
        if (i >= 5280) {
            return 14;
        }
        if (i >= 5180) {
            return 13;
        }
        if (i >= 5010) {
            return 12;
        }
        if (i >= 4750) {
            return 11;
        }
        if (i >= 4150) {
            return 10;
        }
        if (i >= 3800) {
            return 9;
        }
        if (i >= 3450) {
            return 8;
        }
        if (i >= 2750) {
            return 7;
        }
        if (i >= 2650) {
            return 6;
        }
        if (i >= 2400) {
            return 5;
        }
        if (i >= 1950) {
            return 4;
        }
        if (i >= 1200) {
            return 3;
        }
        if (i >= 600) {
            return 2;
        }
        if (i < 0) {
            return -1;
        }
        return 1;
    }
}
