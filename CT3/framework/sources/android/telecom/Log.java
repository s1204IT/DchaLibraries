package android.telecom;

import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.AsyncTask;
import android.os.BatteryStats;
import android.security.keystore.KeyProperties;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.IllegalFormatException;
import java.util.Locale;

public final class Log {
    public static final boolean FORCE_LOGGING = true;
    private static final String TAG = "TelecomFramework";
    private static MessageDigest sMessageDigest;
    public static final boolean DEBUG = isLoggable(3);
    public static final boolean INFO = isLoggable(4);
    public static final boolean VERBOSE = isLoggable(2);
    public static final boolean WARN = isLoggable(5);
    public static final boolean ERROR = isLoggable(6);
    private static final Object sMessageDigestLock = new Object();

    private Log() {
    }

    public static void initMd5Sum() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... args) {
                MessageDigest messageDigest;
                try {
                    messageDigest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA1);
                } catch (NoSuchAlgorithmException e) {
                    messageDigest = null;
                }
                synchronized (Log.sMessageDigestLock) {
                    MessageDigest unused = Log.sMessageDigest = messageDigest;
                }
                return null;
            }
        }.execute(new Void[0]);
    }

    public static boolean isLoggable(int level) {
        return true;
    }

    public static void d(String prefix, String format, Object... args) {
        if (!DEBUG) {
            return;
        }
        android.util.Log.d(TAG, buildMessage(prefix, format, args));
    }

    public static void d(Object objectPrefix, String format, Object... args) {
        if (!DEBUG) {
            return;
        }
        android.util.Log.d(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
    }

    public static void i(String prefix, String format, Object... args) {
        if (!INFO) {
            return;
        }
        android.util.Log.i(TAG, buildMessage(prefix, format, args));
    }

    public static void i(Object objectPrefix, String format, Object... args) {
        if (!INFO) {
            return;
        }
        android.util.Log.i(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
    }

    public static void v(String prefix, String format, Object... args) {
        if (!VERBOSE) {
            return;
        }
        android.util.Log.v(TAG, buildMessage(prefix, format, args));
    }

    public static void v(Object objectPrefix, String format, Object... args) {
        if (!VERBOSE) {
            return;
        }
        android.util.Log.v(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
    }

    public static void w(String prefix, String format, Object... args) {
        if (!WARN) {
            return;
        }
        android.util.Log.w(TAG, buildMessage(prefix, format, args));
    }

    public static void w(Object objectPrefix, String format, Object... args) {
        if (!WARN) {
            return;
        }
        android.util.Log.w(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
    }

    public static void e(String prefix, Throwable tr, String format, Object... args) {
        if (!ERROR) {
            return;
        }
        android.util.Log.e(TAG, buildMessage(prefix, format, args), tr);
    }

    public static void e(Object objectPrefix, Throwable tr, String format, Object... args) {
        if (!ERROR) {
            return;
        }
        android.util.Log.e(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args), tr);
    }

    public static void wtf(String prefix, Throwable tr, String format, Object... args) {
        android.util.Log.wtf(TAG, buildMessage(prefix, format, args), tr);
    }

    public static void wtf(Object objectPrefix, Throwable tr, String format, Object... args) {
        android.util.Log.wtf(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args), tr);
    }

    public static void wtf(String prefix, String format, Object... args) {
        String msg = buildMessage(prefix, format, args);
        android.util.Log.wtf(TAG, msg, new IllegalStateException(msg));
    }

    public static void wtf(Object objectPrefix, String format, Object... args) {
        String msg = buildMessage(getPrefixFromObject(objectPrefix), format, args);
        android.util.Log.wtf(TAG, msg, new IllegalStateException(msg));
    }

    public static String pii(Object pii) {
        if (pii == null || VERBOSE) {
            return String.valueOf(pii);
        }
        if (pii instanceof Uri) {
            return piiUri((Uri) pii);
        }
        return "[" + secureHash(String.valueOf(pii).getBytes()) + "]";
    }

    private static String piiUri(Uri handle) {
        StringBuilder sb = new StringBuilder();
        String scheme = handle.getScheme();
        if (!TextUtils.isEmpty(scheme)) {
            sb.append(scheme).append(":");
        }
        String value = handle.getSchemeSpecificPart();
        if (!TextUtils.isEmpty(value)) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (PhoneNumberUtils.isStartsPostDial(c)) {
                    sb.append(c);
                } else if (PhoneNumberUtils.isDialable(c)) {
                    sb.append(NetworkCapabilities.MATCH_ALL_REQUESTS_NETWORK_SPECIFIER);
                } else if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
                    sb.append(NetworkCapabilities.MATCH_ALL_REQUESTS_NETWORK_SPECIFIER);
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static String secureHash(byte[] input) {
        synchronized (sMessageDigestLock) {
            if (sMessageDigest != null) {
                sMessageDigest.reset();
                sMessageDigest.update(input);
                byte[] result = sMessageDigest.digest();
                return encodeHex(result);
            }
            return "Uninitialized SHA1";
        }
    }

    private static String encodeHex(byte[] bytes) {
        StringBuffer hex = new StringBuffer(bytes.length * 2);
        for (byte b : bytes) {
            int byteIntValue = b & BatteryStats.HistoryItem.CMD_NULL;
            if (byteIntValue < 16) {
                hex.append(WifiEnterpriseConfig.ENGINE_DISABLE);
            }
            hex.append(Integer.toString(byteIntValue, 16));
        }
        return hex.toString();
    }

    private static String getPrefixFromObject(Object obj) {
        return obj == null ? "<null>" : obj.getClass().getSimpleName();
    }

    private static String buildMessage(String prefix, String format, Object... args) {
        String msg;
        if (args != null) {
            try {
                msg = args.length == 0 ? format : String.format(Locale.US, format, args);
            } catch (IllegalFormatException ife) {
                wtf("Log", (Throwable) ife, "IllegalFormatException: formatString='%s' numArgs=%d", format, Integer.valueOf(args.length));
                msg = format + " (An error occurred while formatting the message.)";
            }
        }
        return String.format(Locale.US, "%s: %s", prefix, msg);
    }
}
