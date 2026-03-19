package com.android.se;

public class CommandApduValidator {
    private static final int CMD_APDU_LENGTH_CASE1 = 4;
    private static final int CMD_APDU_LENGTH_CASE2 = 5;
    private static final int CMD_APDU_LENGTH_CASE2_EXTENDED = 7;
    private static final int CMD_APDU_LENGTH_CASE3_WITHOUT_DATA = 5;
    private static final int CMD_APDU_LENGTH_CASE3_WITHOUT_DATA_EXTENDED = 7;
    private static final int CMD_APDU_LENGTH_CASE4_WITHOUT_DATA = 6;
    private static final int CMD_APDU_LENGTH_CASE4_WITHOUT_DATA_EXTENDED = 9;
    private static final int MAX_EXPECTED_DATA_LENGTH = 65536;
    private static final int OFFSET_CLA = 0;
    private static final int OFFSET_DATA = 5;
    private static final int OFFSET_DATA_EXTENDED = 7;
    private static final int OFFSET_INS = 1;
    private static final int OFFSET_P3 = 4;

    private CommandApduValidator() {
    }

    public static void execute(byte[] bArr) throws IllegalArgumentException {
        if (bArr.length < 4) {
            throw new IllegalArgumentException("Invalid length for command (" + bArr.length + ").");
        }
        checkCla(bArr[0]);
        checkIns(bArr[1]);
        if (bArr.length == 4) {
            return;
        }
        if (bArr.length == 5) {
            checkLe(bArr[4] & 255);
            return;
        }
        if (bArr[4] != 0) {
            int i = bArr[4] & 255;
            if (bArr.length == 5 + i) {
                return;
            }
            if (bArr.length == CMD_APDU_LENGTH_CASE4_WITHOUT_DATA + i) {
                checkLe(bArr[bArr.length - 1] & 255);
                return;
            }
            throw new IllegalArgumentException("Unexpected value of Lc (" + i + ")");
        }
        if (bArr.length == 7) {
            checkLe(((bArr[5] & 255) << 8) + (bArr[CMD_APDU_LENGTH_CASE4_WITHOUT_DATA] & 255));
            return;
        }
        if (bArr.length <= 7) {
            throw new IllegalArgumentException("Unexpected value of Lc or Le" + bArr.length);
        }
        int i2 = ((bArr[5] & 255) << 8) + (bArr[CMD_APDU_LENGTH_CASE4_WITHOUT_DATA] & 255);
        if (i2 == 0) {
            throw new IllegalArgumentException("Lc can't be 0");
        }
        if (bArr.length == 7 + i2) {
            return;
        }
        if (bArr.length == CMD_APDU_LENGTH_CASE4_WITHOUT_DATA_EXTENDED + i2) {
            checkLe(((bArr[bArr.length - 2] & 255) << 8) + (bArr[bArr.length - 1] & 255));
            return;
        }
        throw new IllegalArgumentException("Unexpected value of Lc (" + i2 + ")");
    }

    private static void checkCla(byte b) throws IllegalArgumentException {
        if (b == -1) {
            throw new IllegalArgumentException("Invalid value of CLA (" + Integer.toHexString(b) + ")");
        }
    }

    private static void checkIns(byte b) throws IllegalArgumentException {
        int i = b & 240;
        if (i == 96 || i == 144) {
            throw new IllegalArgumentException("Invalid value of INS (" + Integer.toHexString(b) + "). 0x6X and 0x9X are not valid values");
        }
    }

    private static void checkLe(int i) throws IllegalArgumentException {
        if (i < 0 || i > MAX_EXPECTED_DATA_LENGTH) {
            throw new IllegalArgumentException("Invalid value for le parameter (" + i + ").");
        }
    }
}
