package com.android.server.telecom;

import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.util.Slog;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.IllegalFormatException;
import java.util.Locale;

public class Log {
    public static final boolean DEBUG = isLoggable(3);
    public static final boolean INFO = isLoggable(4);
    public static final boolean VERBOSE = isLoggable(2);
    public static final boolean WARN = isLoggable(5);
    public static final boolean ERROR = isLoggable(6);

    public static boolean isLoggable(int i) {
        return android.util.Log.isLoggable("Telecom", i);
    }

    public static void d(String str, String str2, Object... objArr) {
        if (DEBUG) {
            Slog.d("Telecom", buildMessage(str, str2, objArr));
        }
    }

    public static void d(Object obj, String str, Object... objArr) {
        if (DEBUG) {
            Slog.d("Telecom", buildMessage(getPrefixFromObject(obj), str, objArr));
        }
    }

    public static void i(String str, String str2, Object... objArr) {
        if (INFO) {
            Slog.i("Telecom", buildMessage(str, str2, objArr));
        }
    }

    public static void i(Object obj, String str, Object... objArr) {
        if (INFO) {
            Slog.i("Telecom", buildMessage(getPrefixFromObject(obj), str, objArr));
        }
    }

    public static void v(String str, String str2, Object... objArr) {
        if (VERBOSE) {
            Slog.v("Telecom", buildMessage(str, str2, objArr));
        }
    }

    public static void v(Object obj, String str, Object... objArr) {
        if (VERBOSE) {
            Slog.v("Telecom", buildMessage(getPrefixFromObject(obj), str, objArr));
        }
    }

    public static void w(String str, String str2, Object... objArr) {
        if (WARN) {
            Slog.w("Telecom", buildMessage(str, str2, objArr));
        }
    }

    public static void w(Object obj, String str, Object... objArr) {
        if (WARN) {
            Slog.w("Telecom", buildMessage(getPrefixFromObject(obj), str, objArr));
        }
    }

    public static void e(String str, Throwable th, String str2, Object... objArr) {
        if (ERROR) {
            Slog.e("Telecom", buildMessage(str, str2, objArr), th);
        }
    }

    public static void e(Object obj, Throwable th, String str, Object... objArr) {
        if (ERROR) {
            Slog.e("Telecom", buildMessage(getPrefixFromObject(obj), str, objArr), th);
        }
    }

    public static void wtf(String str, Throwable th, String str2, Object... objArr) {
        Slog.wtf("Telecom", buildMessage(str, str2, objArr), th);
    }

    public static void wtf(String str, String str2, Object... objArr) {
        String strBuildMessage = buildMessage(str, str2, objArr);
        Slog.wtf("Telecom", strBuildMessage, new IllegalStateException(strBuildMessage));
    }

    public static void wtf(Object obj, String str, Object... objArr) {
        String strBuildMessage = buildMessage(getPrefixFromObject(obj), str, objArr);
        Slog.wtf("Telecom", strBuildMessage, new IllegalStateException(strBuildMessage));
    }

    public static String piiHandle(Object obj) {
        if (obj == null || VERBOSE) {
            return String.valueOf(obj);
        }
        if (obj instanceof Uri) {
            Uri uri = (Uri) obj;
            if (!"tel".equals(uri.getScheme())) {
                return pii(obj);
            }
            obj = uri.getSchemeSpecificPart();
        }
        String strValueOf = String.valueOf(obj);
        StringBuilder sb = new StringBuilder(strValueOf.length());
        char[] charArray = strValueOf.toCharArray();
        for (char c : charArray) {
            if (PhoneNumberUtils.isDialable(c)) {
                sb.append('*');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String pii(Object obj) {
        return (obj == null || VERBOSE) ? String.valueOf(obj) : "[" + secureHash(String.valueOf(obj).getBytes()) + "]";
    }

    private static String secureHash(byte[] bArr) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(bArr);
            return encodeHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String encodeHex(byte[] bArr) {
        StringBuffer stringBuffer = new StringBuffer(bArr.length * 2);
        for (byte b : bArr) {
            int i = b & 255;
            if (i < 16) {
                stringBuffer.append("0");
            }
            stringBuffer.append(Integer.toString(i, 16));
        }
        return stringBuffer.toString();
    }

    private static String getPrefixFromObject(Object obj) {
        return obj == null ? "<null>" : obj.getClass().getSimpleName();
    }

    private static String buildMessage(String str, String str2, Object... objArr) {
        if (objArr != null) {
            try {
                if (objArr.length != 0) {
                    str2 = String.format(Locale.US, str2, objArr);
                }
            } catch (IllegalFormatException e) {
                e("Log", (Throwable) e, "IllegalFormatException: formatString='%s' numArgs=%d", str2, Integer.valueOf(objArr.length));
                str2 = str2 + " (An error occurred while formatting the message.)";
            }
        }
        return String.format(Locale.US, "%s: %s", str, str2);
    }
}
