package com.android.certinstaller;

import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Util {
    static byte[] toBytes(Object obj) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            objectOutputStream.close();
        } catch (Exception e) {
            Log.w("certinstaller.Util", "toBytes(): " + e + ": " + obj);
        }
        return byteArrayOutputStream.toByteArray();
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

    static String toMd5(byte[] bArr) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(bArr);
            return toHexString(messageDigest.digest(), "");
        } catch (NoSuchAlgorithmException e) {
            Log.w("certinstaller.Util", "toMd5(): " + e);
            throw new RuntimeException(e);
        }
    }

    private static String toHexString(byte[] bArr, String str) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bArr) {
            sb.append(Integer.toHexString(b & 255));
            sb.append(str);
        }
        return sb.toString();
    }
}
