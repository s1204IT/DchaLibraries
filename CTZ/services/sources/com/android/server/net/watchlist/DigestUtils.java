package com.android.server.net.watchlist;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {
    private static final int FILE_READ_BUFFER_SIZE = 16384;

    private DigestUtils() {
    }

    public static byte[] getSha256Hash(File file) throws Throwable {
        Throwable th;
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] sha256Hash = getSha256Hash(fileInputStream);
            fileInputStream.close();
            return sha256Hash;
        } catch (Throwable th2) {
            th = th2;
            th = null;
            if (th == null) {
            }
            throw th;
        }
    }

    public static byte[] getSha256Hash(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
        byte[] bArr = new byte[16384];
        while (true) {
            int i = inputStream.read(bArr);
            if (i >= 0) {
                messageDigest.update(bArr, 0, i);
            } else {
                return messageDigest.digest();
            }
        }
    }
}
