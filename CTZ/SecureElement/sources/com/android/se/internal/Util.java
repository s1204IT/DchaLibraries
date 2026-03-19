package com.android.se.internal;

import android.content.Context;
import android.content.pm.PackageManager;
import java.security.AccessControlException;

public class Util {
    public static final byte END = -1;

    public static byte[] mergeBytes(byte[] bArr, byte[] bArr2) {
        byte[] bArr3 = new byte[bArr.length + bArr2.length];
        System.arraycopy(bArr, 0, bArr3, 0, bArr.length);
        System.arraycopy(bArr2, 0, bArr3, bArr.length, bArr2.length);
        return bArr3;
    }

    public static byte[] getMid(byte[] bArr, int i, int i2) {
        byte[] bArr2 = new byte[i2];
        System.arraycopy(bArr, i, bArr2, 0, i2);
        return bArr2;
    }

    public static byte[] appendResponse(byte[] bArr, byte[] bArr2, int i) {
        byte[] bArr3 = new byte[bArr.length + i];
        System.arraycopy(bArr, 0, bArr3, 0, bArr.length);
        System.arraycopy(bArr2, 0, bArr3, bArr.length, i);
        return bArr3;
    }

    public static String createMessage(String str, int i) {
        StringBuilder sb = new StringBuilder();
        if (str != null) {
            sb.append(str);
            sb.append(" ");
        }
        sb.append("SW1/2 error: ");
        sb.append(Integer.toHexString(65536 | i).substring(1));
        return sb.toString();
    }

    public static String createMessage(String str, String str2) {
        if (str == null) {
            return str2;
        }
        return str + " " + str2;
    }

    public static String getPackageNameFromCallingUid(Context context, int i) {
        String[] packagesForUid;
        PackageManager packageManager = context.getPackageManager();
        if (packageManager != null && (packagesForUid = packageManager.getPackagesForUid(i)) != null && packagesForUid.length > 0) {
            return packagesForUid[0];
        }
        throw new AccessControlException("Caller PackageName can not be determined");
    }

    public static byte setChannelToClassByte(byte b, int i) {
        if (i < 4) {
            return (byte) ((b & 188) | i);
        }
        if (i < 20) {
            boolean z = (b & 12) != 0;
            byte b2 = (byte) ((b & 176) | 64 | (i - 4));
            if (z) {
                return (byte) (b2 | 32);
            }
            return b2;
        }
        throw new IllegalArgumentException("Channel number must be within [0..19]");
    }

    public static byte clearChannelNumber(byte b) {
        if ((b & 64) == 0) {
            return (byte) (b & 252);
        }
        return (byte) (b & 240);
    }

    public static int parseChannelNumber(byte b) {
        if ((b & 64) == 0) {
            return b & 3;
        }
        return (b & 15) + 4;
    }
}
