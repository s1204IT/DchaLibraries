package com.android.org.conscrypt.ct;

import com.android.org.conscrypt.InternalUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class CTLogStoreImpl implements CTLogStore {
    private static final char[] HEX_DIGITS;
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static volatile CTLogInfo[] defaultFallbackLogs = null;
    private static final File defaultSystemLogDir;
    private static final File defaultUserLogDir;
    private CTLogInfo[] fallbackLogs;
    private HashMap<ByteBuffer, CTLogInfo> logCache;
    private Set<ByteBuffer> missingLogCache;
    private File systemLogDir;
    private File userLogDir;

    static {
        String str = System.getenv("ANDROID_DATA");
        String str2 = System.getenv("ANDROID_ROOT");
        defaultUserLogDir = new File(str + "/misc/keychain/trusted_ct_logs/current/");
        defaultSystemLogDir = new File(str2 + "/etc/security/ct_known_logs/");
        HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    }

    public static class InvalidLogFileException extends Exception {
        public InvalidLogFileException() {
        }

        public InvalidLogFileException(String str) {
            super(str);
        }

        public InvalidLogFileException(String str, Throwable th) {
            super(str, th);
        }

        public InvalidLogFileException(Throwable th) {
            super(th);
        }
    }

    public CTLogStoreImpl() {
        this(defaultUserLogDir, defaultSystemLogDir, getDefaultFallbackLogs());
    }

    public CTLogStoreImpl(File file, File file2, CTLogInfo[] cTLogInfoArr) {
        this.logCache = new HashMap<>();
        this.missingLogCache = Collections.synchronizedSet(new HashSet());
        this.userLogDir = file;
        this.systemLogDir = file2;
        this.fallbackLogs = cTLogInfoArr;
    }

    @Override
    public CTLogInfo getKnownLog(byte[] bArr) {
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        CTLogInfo cTLogInfo = this.logCache.get(byteBufferWrap);
        if (cTLogInfo != null) {
            return cTLogInfo;
        }
        if (this.missingLogCache.contains(byteBufferWrap)) {
            return null;
        }
        CTLogInfo cTLogInfoFindKnownLog = findKnownLog(bArr);
        if (cTLogInfoFindKnownLog != null) {
            this.logCache.put(byteBufferWrap, cTLogInfoFindKnownLog);
        } else {
            this.missingLogCache.add(byteBufferWrap);
        }
        return cTLogInfoFindKnownLog;
    }

    private CTLogInfo findKnownLog(byte[] bArr) {
        String strHexEncode = hexEncode(bArr);
        try {
            return loadLog(new File(this.userLogDir, strHexEncode));
        } catch (InvalidLogFileException e) {
            return null;
        } catch (FileNotFoundException e2) {
            try {
                return loadLog(new File(this.systemLogDir, strHexEncode));
            } catch (InvalidLogFileException e3) {
                return null;
            } catch (FileNotFoundException e4) {
                if (!this.userLogDir.exists()) {
                    for (CTLogInfo cTLogInfo : this.fallbackLogs) {
                        if (Arrays.equals(bArr, cTLogInfo.getID())) {
                            return cTLogInfo;
                        }
                    }
                }
                return null;
            }
        }
    }

    public static CTLogInfo[] getDefaultFallbackLogs() {
        CTLogInfo[] cTLogInfoArr = defaultFallbackLogs;
        if (cTLogInfoArr == null) {
            CTLogInfo[] cTLogInfoArrCreateDefaultFallbackLogs = createDefaultFallbackLogs();
            defaultFallbackLogs = cTLogInfoArrCreateDefaultFallbackLogs;
            return cTLogInfoArrCreateDefaultFallbackLogs;
        }
        return cTLogInfoArr;
    }

    private static CTLogInfo[] createDefaultFallbackLogs() {
        CTLogInfo[] cTLogInfoArr = new CTLogInfo[8];
        for (int i = 0; i < 8; i++) {
            try {
                cTLogInfoArr[i] = new CTLogInfo(InternalUtil.logKeyToPublicKey(KnownLogs.LOG_KEYS[i]), KnownLogs.LOG_DESCRIPTIONS[i], KnownLogs.LOG_URLS[i]);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        defaultFallbackLogs = cTLogInfoArr;
        return cTLogInfoArr;
    }

    public static CTLogInfo loadLog(File file) throws InvalidLogFileException, FileNotFoundException {
        return loadLog(new FileInputStream(file));
    }

    public static CTLogInfo loadLog(InputStream inputStream) throws InvalidLogFileException {
        Scanner scanner = new Scanner(inputStream, "UTF-8");
        scanner.useDelimiter("\n");
        try {
            String str = null;
            if (!scanner.hasNext()) {
                return null;
            }
            String str2 = null;
            String str3 = null;
            while (scanner.hasNext()) {
                String[] strArrSplit = scanner.next().split(":", 2);
                if (strArrSplit.length >= 2) {
                    byte b = 0;
                    String str4 = strArrSplit[0];
                    String str5 = strArrSplit[1];
                    int iHashCode = str4.hashCode();
                    if (iHashCode == -1724546052) {
                        if (!str4.equals("description")) {
                        }
                        switch (b) {
                        }
                    } else if (iHashCode != 106079) {
                        if (iHashCode == 116079 && str4.equals("url")) {
                            b = 1;
                        } else {
                            b = -1;
                        }
                        switch (b) {
                            case 0:
                                str = str5;
                                break;
                            case 1:
                                str2 = str5;
                                break;
                            case 2:
                                str3 = str5;
                                break;
                        }
                    } else {
                        if (str4.equals("key")) {
                            b = 2;
                        }
                        switch (b) {
                        }
                    }
                }
            }
            if (str == null || str2 == null || str3 == null) {
                throw new InvalidLogFileException("Missing one of 'description', 'url' or 'key'");
            }
            try {
                return new CTLogInfo(InternalUtil.readPublicKeyPem(new ByteArrayInputStream(("-----BEGIN PUBLIC KEY-----\n" + str3 + "\n-----END PUBLIC KEY-----").getBytes(US_ASCII))), str, str2);
            } catch (InvalidKeyException e) {
                throw new InvalidLogFileException(e);
            } catch (NoSuchAlgorithmException e2) {
                throw new InvalidLogFileException(e2);
            }
        } finally {
            scanner.close();
        }
    }

    private static String hexEncode(byte[] bArr) {
        StringBuilder sb = new StringBuilder(bArr.length * 2);
        for (byte b : bArr) {
            sb.append(HEX_DIGITS[(b >> 4) & 15]);
            sb.append(HEX_DIGITS[b & 15]);
        }
        return sb.toString();
    }
}
