package com.android.org.conscrypt;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CertBlacklist {
    private final Set<byte[]> pubkeyBlacklist;
    private final Set<BigInteger> serialBlacklist;
    private static final Logger logger = Logger.getLogger(CertBlacklist.class.getName());
    private static final byte[] HEX_TABLE = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98, 99, 100, 101, 102};

    public CertBlacklist(Set<BigInteger> set, Set<byte[]> set2) {
        this.serialBlacklist = set;
        this.pubkeyBlacklist = set2;
    }

    public static CertBlacklist getDefault() {
        String str = System.getenv("ANDROID_DATA") + "/misc/keychain/";
        String str2 = str + "pubkey_blacklist.txt";
        return new CertBlacklist(readSerialBlackList(str + "serial_blacklist.txt"), readPublicKeyBlackList(str2));
    }

    private static boolean isHex(String str) {
        try {
            new BigInteger(str, 16);
            return true;
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Could not parse hex value " + str, (Throwable) e);
            return false;
        }
    }

    private static boolean isPubkeyHash(String str) {
        if (str.length() != 40) {
            logger.log(Level.WARNING, "Invalid pubkey hash length: " + str.length());
            return false;
        }
        return isHex(str);
    }

    private static String readBlacklist(String str) {
        try {
            return readFileAsString(str);
        } catch (FileNotFoundException e) {
            return "";
        } catch (IOException e2) {
            logger.log(Level.WARNING, "Could not read blacklist", (Throwable) e2);
            return "";
        }
    }

    private static String readFileAsString(String str) throws IOException {
        return readFileAsBytes(str).toString("UTF-8");
    }

    private static ByteArrayOutputStream readFileAsBytes(String str) throws Throwable {
        RandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new RandomAccessFile(str, "r");
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int) randomAccessFile.length());
                byte[] bArr = new byte[8192];
                while (true) {
                    int i = randomAccessFile.read(bArr);
                    if (i != -1) {
                        byteArrayOutputStream.write(bArr, 0, i);
                    } else {
                        closeQuietly(randomAccessFile);
                        return byteArrayOutputStream;
                    }
                }
            } catch (Throwable th) {
                th = th;
                closeQuietly(randomAccessFile);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            randomAccessFile = null;
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e2) {
            }
        }
    }

    private static Set<BigInteger> readSerialBlackList(String str) {
        HashSet hashSet = new HashSet(Arrays.asList(new BigInteger("077a59bcd53459601ca6907267a6dd1c", 16), new BigInteger("047ecbe9fca55f7bd09eae36e10cae1e", 16), new BigInteger("d8f35f4eb7872b2dab0692e315382fb0", 16), new BigInteger("b0b7133ed096f9b56fae91c874bd3ac0", 16), new BigInteger("9239d5348f40d1695a745470e1f23f43", 16), new BigInteger("e9028b9578e415dc1a710a2b88154447", 16), new BigInteger("d7558fdaf5f1105bb213282b707729a3", 16), new BigInteger("f5c86af36162f13a64f54f6dc9587c06", 16), new BigInteger("392a434f0e07df1f8aa305de34e0c229", 16), new BigInteger("3e75ced46b693021218830ae86a82a71", 16)));
        String blacklist = readBlacklist(str);
        if (!blacklist.equals("")) {
            for (String str2 : blacklist.split(",")) {
                try {
                    hashSet.add(new BigInteger(str2, 16));
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Tried to blacklist invalid serial number " + str2, (Throwable) e);
                }
            }
        }
        return Collections.unmodifiableSet(hashSet);
    }

    private static Set<byte[]> readPublicKeyBlackList(String str) {
        HashSet hashSet = new HashSet(Arrays.asList("bae78e6bed65a2bf60ddedde7fd91e825865e93d".getBytes(StandardCharsets.UTF_8), "410f36363258f30b347d12ce4863e433437806a8".getBytes(StandardCharsets.UTF_8), "ba3e7bd38cd7e1e6b9cd4c219962e59d7a2f4e37".getBytes(StandardCharsets.UTF_8), "e23b8d105f87710a68d9248050ebefc627be4ca6".getBytes(StandardCharsets.UTF_8), "7b2e16bc39bcd72b456e9f055d1de615b74945db".getBytes(StandardCharsets.UTF_8), "e8f91200c65cee16e039b9f883841661635f81c5".getBytes(StandardCharsets.UTF_8), "0129bcd5b448ae8d2496d1c3e19723919088e152".getBytes(StandardCharsets.UTF_8), "5f3ab33d55007054bc5e3e5553cd8d8465d77c61".getBytes(StandardCharsets.UTF_8), "783333c9687df63377efceddd82efa9101913e8e".getBytes(StandardCharsets.UTF_8), "3ecf4bbbe46096d514bb539bb913d77aa4ef31bf".getBytes(StandardCharsets.UTF_8)));
        String blacklist = readBlacklist(str);
        if (!blacklist.equals("")) {
            for (String str2 : blacklist.split(",")) {
                String strTrim = str2.trim();
                if (isPubkeyHash(strTrim)) {
                    hashSet.add(strTrim.getBytes(StandardCharsets.UTF_8));
                } else {
                    logger.log(Level.WARNING, "Tried to blacklist invalid pubkey " + strTrim);
                }
            }
        }
        return hashSet;
    }

    public boolean isPublicKeyBlackListed(PublicKey publicKey) {
        try {
            byte[] hex = toHex(MessageDigest.getInstance("SHA1").digest(publicKey.getEncoded()));
            Iterator<byte[]> it = this.pubkeyBlacklist.iterator();
            while (it.hasNext()) {
                if (Arrays.equals(it.next(), hex)) {
                    return true;
                }
            }
            return false;
        } catch (GeneralSecurityException e) {
            logger.log(Level.SEVERE, "Unable to get SHA1 MessageDigest", (Throwable) e);
            return false;
        }
    }

    private static byte[] toHex(byte[] bArr) {
        byte[] bArr2 = new byte[bArr.length * 2];
        int i = 0;
        for (byte b : bArr) {
            int i2 = b & 255;
            int i3 = i + 1;
            bArr2[i] = HEX_TABLE[i2 >> 4];
            i = i3 + 1;
            bArr2[i3] = HEX_TABLE[i2 & 15];
        }
        return bArr2;
    }

    public boolean isSerialNumberBlackListed(BigInteger bigInteger) {
        return this.serialBlacklist.contains(bigInteger);
    }
}
