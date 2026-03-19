package com.android.statementservice.retriever;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public final class Utils {
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String joinStrings(String str, List<String> list) {
        switch (list.size()) {
            case 0:
                return "";
            case 1:
                return list.get(0);
            default:
                StringBuilder sb = new StringBuilder();
                boolean z = true;
                for (String str2 : list) {
                    if (!z) {
                        sb.append(str);
                    } else {
                        z = false;
                    }
                    sb.append(str2);
                }
                return sb.toString();
        }
    }

    public static List<String> getCertFingerprintsFromPackageManager(String str, Context context) throws PackageManager.NameNotFoundException {
        Signature[] signatureArr = context.getPackageManager().getPackageInfo(str, 64).signatures;
        ArrayList arrayList = new ArrayList(signatureArr.length);
        for (Signature signature : signatureArr) {
            arrayList.add(computeNormalizedSha256Fingerprint(signature.toByteArray()));
        }
        return arrayList;
    }

    public static String computeNormalizedSha256Fingerprint(byte[] bArr) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(bArr);
            return byteArrayToHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("No SHA-256 implementation found.");
        }
    }

    public static boolean hasCommonString(List<String> list, List<String> list2) {
        HashSet hashSet = new HashSet(list2);
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (hashSet.contains(it.next())) {
                return true;
            }
        }
        return false;
    }

    private static String byteArrayToHexString(byte[] bArr) {
        if (bArr.length == 0) {
            return "";
        }
        char[] cArr = new char[(bArr.length * 3) - 1];
        int i = 0;
        for (int i2 = 0; i2 < bArr.length; i2++) {
            byte b = bArr[i2];
            if (i2 > 0) {
                cArr[i] = ':';
                i++;
            }
            int i3 = i + 1;
            cArr[i] = HEX_DIGITS[(b >>> 4) & 15];
            i = i3 + 1;
            cArr[i3] = HEX_DIGITS[b & 15];
        }
        return new String(cArr);
    }
}
