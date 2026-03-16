package com.android.services.telephony;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.IllegalFormatException;
import java.util.Locale;

public final class Log {
    public static final boolean DEBUG = isLoggable(3);
    public static final boolean INFO = isLoggable(4);
    public static final boolean VERBOSE = isLoggable(2);
    public static final boolean WARN = isLoggable(5);
    public static final boolean ERROR = isLoggable(6);

    public static boolean isLoggable(int level) {
        return android.util.Log.isLoggable("Telephony", level);
    }

    public static void d(Object objectPrefix, String format, Object... args) {
        if (DEBUG) {
            android.util.Log.d("Telephony", buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void i(Object objectPrefix, String format, Object... args) {
        if (INFO) {
            android.util.Log.i("Telephony", buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void v(Object objectPrefix, String format, Object... args) {
        if (VERBOSE) {
            android.util.Log.v("Telephony", buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void w(String prefix, String format, Object... args) {
        if (WARN) {
            android.util.Log.w("Telephony", buildMessage(prefix, format, args));
        }
    }

    public static void w(Object objectPrefix, String format, Object... args) {
        if (WARN) {
            android.util.Log.w("Telephony", buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void e(Object objectPrefix, Throwable tr, String format, Object... args) {
        if (ERROR) {
            android.util.Log.e("Telephony", buildMessage(getPrefixFromObject(objectPrefix), format, args), tr);
        }
    }

    public static void wtf(String prefix, Throwable tr, String format, Object... args) {
        android.util.Log.wtf("Telephony", buildMessage(prefix, format, args), tr);
    }

    public static void wtf(Object objectPrefix, String format, Object... args) {
        String msg = buildMessage(getPrefixFromObject(objectPrefix), format, args);
        android.util.Log.wtf("Telephony", msg, new IllegalStateException(msg));
    }

    public static String pii(Object pii) {
        return (pii == null || VERBOSE) ? String.valueOf(pii) : "[" + secureHash(String.valueOf(pii).getBytes()) + "]";
    }

    private static String secureHash(byte[] input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(input);
            byte[] result = messageDigest.digest();
            return encodeHex(result);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String encodeHex(byte[] bytes) {
        StringBuffer hex = new StringBuffer(bytes.length * 2);
        for (byte b : bytes) {
            int byteIntValue = b & 255;
            if (byteIntValue < 16) {
                hex.append("0");
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
                wtf("Log", ife, "IllegalFormatException: formatString='%s' numArgs=%d", format, Integer.valueOf(args.length));
                msg = format + " (An error occurred while formatting the message.)";
            }
        }
        return String.format(Locale.US, "%s: %s", prefix, msg);
    }
}
