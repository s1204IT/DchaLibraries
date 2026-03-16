package com.android.certinstaller;

import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Util {
    static byte[] toBytes(Object object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(object);
            os.close();
        } catch (Exception e) {
            Log.w("certinstaller.Util", "toBytes(): " + e + ": " + object);
        }
        return baos.toByteArray();
    }

    static <T> T fromBytes(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        try {
            return (T) new ObjectInputStream(new ByteArrayInputStream(bArr)).readObject();
        } catch (Exception e) {
            Log.w("certinstaller.Util", "fromBytes(): " + e);
            return null;
        }
    }

    static String toMd5(byte[] bytes) {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(bytes);
            return toHexString(algorithm.digest(), "");
        } catch (NoSuchAlgorithmException e) {
            Log.w("certinstaller.Util", "toMd5(): " + e);
            throw new RuntimeException(e);
        }
    }

    private static String toHexString(byte[] bytes, String separator) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(Integer.toHexString(b & 255)).append(separator);
        }
        return hexString.toString();
    }
}
