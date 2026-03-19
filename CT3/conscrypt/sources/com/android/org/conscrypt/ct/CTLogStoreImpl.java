package com.android.org.conscrypt.ct;

import com.android.org.conscrypt.NativeCrypto;
import com.android.org.conscrypt.OpenSSLKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class CTLogStoreImpl implements CTLogStore {
    private static final char[] HEX_DIGITS;
    private static volatile CTLogInfo[] defaultFallbackLogs = null;
    private static final File defaultSystemLogDir;
    private static final File defaultUserLogDir;
    private CTLogInfo[] fallbackLogs;
    private Map<ByteBuffer, CTLogInfo> logCache;
    private Set<ByteBuffer> missingLogCache;
    private File systemLogDir;
    private File userLogDir;

    public static class InvalidLogFileException extends Exception {
        public InvalidLogFileException() {
        }

        public InvalidLogFileException(String message) {
            super(message);
        }

        public InvalidLogFileException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidLogFileException(Throwable cause) {
            super(cause);
        }
    }

    static {
        String ANDROID_DATA = System.getenv("ANDROID_DATA");
        String ANDROID_ROOT = System.getenv("ANDROID_ROOT");
        defaultUserLogDir = new File(ANDROID_DATA + "/misc/keychain/ct_known_logs/");
        defaultSystemLogDir = new File(ANDROID_ROOT + "/etc/security/ct_known_logs/");
        HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    }

    public CTLogStoreImpl() {
        this(defaultUserLogDir, defaultSystemLogDir, getDefaultFallbackLogs());
    }

    public CTLogStoreImpl(File userLogDir, File systemLogDir, CTLogInfo[] fallbackLogs) {
        this.logCache = new Hashtable();
        this.missingLogCache = Collections.synchronizedSet(new HashSet());
        this.userLogDir = userLogDir;
        this.systemLogDir = systemLogDir;
        this.fallbackLogs = fallbackLogs;
    }

    @Override
    public CTLogInfo getKnownLog(byte[] logId) {
        ByteBuffer buf = ByteBuffer.wrap(logId);
        CTLogInfo log = this.logCache.get(buf);
        if (log != null) {
            return log;
        }
        if (this.missingLogCache.contains(buf)) {
            return null;
        }
        CTLogInfo log2 = findKnownLog(logId);
        if (log2 != null) {
            this.logCache.put(buf, log2);
        } else {
            this.missingLogCache.add(buf);
        }
        return log2;
    }

    private CTLogInfo findKnownLog(byte[] logId) {
        String filename = hexEncode(logId);
        try {
            return loadLog(new File(this.userLogDir, filename));
        } catch (InvalidLogFileException e) {
            return null;
        } catch (FileNotFoundException e2) {
            try {
                return loadLog(new File(this.systemLogDir, filename));
            } catch (InvalidLogFileException e3) {
                return null;
            } catch (FileNotFoundException e4) {
                for (CTLogInfo log : this.fallbackLogs) {
                    if (Arrays.equals(logId, log.getID())) {
                        return log;
                    }
                }
                return null;
            }
        }
    }

    public static CTLogInfo[] getDefaultFallbackLogs() {
        CTLogInfo[] result = defaultFallbackLogs;
        if (result == null) {
            CTLogInfo[] result2 = createDefaultFallbackLogs();
            defaultFallbackLogs = result2;
            return result2;
        }
        return result;
    }

    private static CTLogInfo[] createDefaultFallbackLogs() {
        CTLogInfo[] logs = new CTLogInfo[8];
        for (int i = 0; i < 8; i++) {
            try {
                PublicKey key = new OpenSSLKey(NativeCrypto.d2i_PUBKEY(KnownLogs.LOG_KEYS[i])).getPublicKey();
                logs[i] = new CTLogInfo(key, KnownLogs.LOG_DESCRIPTIONS[i], KnownLogs.LOG_URLS[i]);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        defaultFallbackLogs = logs;
        return logs;
    }

    public static CTLogInfo loadLog(File file) throws InvalidLogFileException, FileNotFoundException {
        return loadLog(new FileInputStream(file));
    }

    public static CTLogInfo loadLog(InputStream input) throws InvalidLogFileException {
        Scanner scan = new Scanner(input).useDelimiter(",");
        if (!scan.hasNext()) {
            return null;
        }
        String description = null;
        String url = null;
        String str = null;
        while (scan.hasNext()) {
            String[] parts = scan.next().split(":", 2);
            if (parts.length >= 2) {
                String name = parts[0];
                String value = parts[1];
                if (!name.equals("description")) {
                    if (!name.equals("url")) {
                        if (name.equals("key")) {
                            str = value;
                        }
                    } else {
                        url = value;
                    }
                } else {
                    description = value;
                }
            }
        }
        if (description == null || url == null || str == null) {
            throw new InvalidLogFileException("Missing one of 'description', 'url' or 'key'");
        }
        try {
            PublicKey pubkey = OpenSSLKey.fromPublicKeyPemInputStream(new StringBufferInputStream("-----BEGIN PUBLIC KEY-----\n" + str + "\n-----END PUBLIC KEY-----")).getPublicKey();
            return new CTLogInfo(pubkey, description, url);
        } catch (InvalidKeyException e) {
            throw new InvalidLogFileException(e);
        } catch (NoSuchAlgorithmException e2) {
            throw new InvalidLogFileException(e2);
        }
    }

    private static String hexEncode(byte[] data) {
        StringBuffer sb = new StringBuffer(data.length * 2);
        for (byte b : data) {
            sb.append(HEX_DIGITS[(b >> 4) & 15]);
            sb.append(HEX_DIGITS[b & 15]);
        }
        return sb.toString();
    }
}
