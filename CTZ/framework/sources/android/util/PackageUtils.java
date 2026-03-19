package android.util;

import android.content.pm.Signature;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class PackageUtils {
    private PackageUtils() {
    }

    public static String[] computeSignaturesSha256Digests(Signature[] signatureArr) {
        int length = signatureArr.length;
        String[] strArr = new String[length];
        for (int i = 0; i < length; i++) {
            strArr[i] = computeSha256Digest(signatureArr[i].toByteArray());
        }
        return strArr;
    }

    public static String computeSignaturesSha256Digest(Signature[] signatureArr) {
        if (signatureArr.length == 1) {
            return computeSha256Digest(signatureArr[0].toByteArray());
        }
        return computeSignaturesSha256Digest(computeSignaturesSha256Digests(signatureArr));
    }

    public static String computeSignaturesSha256Digest(String[] strArr) {
        if (strArr.length == 1) {
            return strArr[0];
        }
        Arrays.sort(strArr);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (String str : strArr) {
            try {
                byteArrayOutputStream.write(str.getBytes());
            } catch (IOException e) {
            }
        }
        return computeSha256Digest(byteArrayOutputStream.toByteArray());
    }

    public static byte[] computeSha256DigestBytes(byte[] bArr) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            messageDigest.update(bArr);
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static String computeSha256Digest(byte[] bArr) {
        return ByteStringUtils.toHexString(computeSha256DigestBytes(bArr));
    }
}
