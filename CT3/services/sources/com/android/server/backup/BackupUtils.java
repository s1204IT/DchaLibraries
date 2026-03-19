package com.android.server.backup;

import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.util.Slog;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BackupUtils {
    private static final boolean DEBUG = false;
    private static final String TAG = "BackupUtils";

    public static boolean signaturesMatch(ArrayList<byte[]> storedSigHashes, PackageInfo target) {
        if (target == null) {
            return false;
        }
        if ((target.applicationInfo.flags & 1) != 0) {
            return true;
        }
        Signature[] deviceSigs = target.signatures;
        if ((storedSigHashes == null || storedSigHashes.size() == 0) && (deviceSigs == null || deviceSigs.length == 0)) {
            return true;
        }
        if (storedSigHashes == null || deviceSigs == null) {
            return false;
        }
        int nStored = storedSigHashes.size();
        int nDevice = deviceSigs.length;
        ArrayList<byte[]> deviceHashes = new ArrayList<>(nDevice);
        for (Signature signature : deviceSigs) {
            deviceHashes.add(hashSignature(signature));
        }
        for (int n = 0; n < nStored; n++) {
            boolean match = false;
            byte[] storedHash = storedSigHashes.get(n);
            int i = 0;
            while (true) {
                if (i >= nDevice) {
                    break;
                }
                if (!Arrays.equals(storedHash, deviceHashes.get(i))) {
                    i++;
                } else {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }

    public static byte[] hashSignature(byte[] signature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(signature);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            Slog.w(TAG, "No SHA-256 algorithm found!");
            return null;
        }
    }

    public static byte[] hashSignature(Signature signature) {
        return hashSignature(signature.toByteArray());
    }

    public static ArrayList<byte[]> hashSignatureArray(Signature[] sigs) {
        if (sigs == null) {
            return null;
        }
        ArrayList<byte[]> hashes = new ArrayList<>(sigs.length);
        for (Signature s : sigs) {
            hashes.add(hashSignature(s));
        }
        return hashes;
    }

    public static ArrayList<byte[]> hashSignatureArray(List<byte[]> sigs) {
        if (sigs == null) {
            return null;
        }
        ArrayList<byte[]> hashes = new ArrayList<>(sigs.size());
        for (byte[] s : sigs) {
            hashes.add(hashSignature(s));
        }
        return hashes;
    }
}
