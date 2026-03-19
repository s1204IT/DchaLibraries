package com.android.se.internal;

import android.hidl.base.V1_0.DebugInfo;
import android.os.HwParcel;

public final class ByteArrayConverter {
    private ByteArrayConverter() {
    }

    public static String byteArrayToPathString(byte[] bArr) throws IllegalArgumentException {
        if (bArr.length % 2 != 0) {
            throw new IllegalArgumentException("Invald path");
        }
        byte[] bArr2 = new byte[2];
        String strConcat = "";
        for (int i = 0; i < bArr.length; i += 2) {
            System.arraycopy(bArr, i, bArr2, 0, 2);
            String strByteArrayToHexString = byteArrayToHexString(bArr2);
            if (!strByteArrayToHexString.equalsIgnoreCase("3F00")) {
                strConcat = strConcat.concat(strByteArrayToHexString);
                if (i != bArr.length - 2) {
                    strConcat = strConcat.concat(":");
                }
            }
        }
        return strConcat;
    }

    public static String byteArrayToHexString(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i3 = 0; i3 < i2; i3++) {
            sb.append(String.format("%02x", Integer.valueOf(bArr[i + i3] & 255)));
        }
        return sb.toString();
    }

    public static String byteArrayToHexString(byte[] bArr, int i) {
        StringBuffer stringBuffer = new StringBuffer();
        while (i < bArr.length) {
            stringBuffer.append(Integer.toHexString(256 + (bArr[i] & 255)).substring(1));
            i++;
        }
        return stringBuffer.toString();
    }

    public static String byteArrayToHexString(byte[] bArr) {
        if (bArr == null) {
            return "";
        }
        return byteArrayToHexString(bArr, 0, bArr.length);
    }

    public static byte[] hexStringToByteArray(String str, int i, int i2) {
        if (i2 % 2 != 0) {
            throw new IllegalArgumentException("length must be multiple of 2");
        }
        String upperCase = str.toUpperCase();
        byte[] bArr = new byte[upperCase.length() / 2];
        for (int i3 = 0; i3 < i2; i3 += 2) {
            char cCharAt = upperCase.charAt(i3 + i);
            char cCharAt2 = upperCase.charAt(i3 + 1 + i);
            if (!isHexChar(cCharAt) || !isHexChar(cCharAt2)) {
                throw new IllegalArgumentException("Invalid char found");
            }
            bArr[i3 / 2] = (byte) ((Character.digit(cCharAt, 16) << 4) + Character.digit(cCharAt2, 16));
        }
        return bArr;
    }

    public static byte[] hexStringToByteArray(String str) {
        return hexStringToByteArray(str, 0, str.length());
    }

    public static byte[] intToByteArray(int i) {
        return new byte[]{(byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i};
    }

    public static int byteArrayToInt(byte[] bArr) {
        switch (bArr.length) {
            case HwParcel.STATUS_SUCCESS:
                return 0;
            case DebugInfo.Architecture.IS_64BIT:
                return bArr[0] & 255;
            case DebugInfo.Architecture.IS_32BIT:
                return (bArr[1] & 255) | ((bArr[0] & 255) << 8);
            case 3:
                return (bArr[2] & 255) | ((bArr[0] & 255) << 16) | ((bArr[1] & 255) << 8);
            default:
                return (bArr[3] & 255) | ((bArr[0] & 255) << 24) | ((bArr[1] & 255) << 16) | ((bArr[2] & 255) << 8);
        }
    }

    public static boolean isHexChar(char c) {
        if (Character.isLowerCase(c)) {
            c = Character.toUpperCase(c);
        }
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F');
    }
}
