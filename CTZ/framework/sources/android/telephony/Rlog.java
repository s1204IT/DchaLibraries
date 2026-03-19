package android.telephony;

import android.os.Build;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Rlog {
    private static final boolean USER_BUILD = Build.IS_USER;

    private Rlog() {
    }

    public static int v(String str, String str2) {
        return Log.println_native(1, 2, str, str2);
    }

    public static int v(String str, String str2, Throwable th) {
        return Log.println_native(1, 2, str, str2 + '\n' + Log.getStackTraceString(th));
    }

    public static int d(String str, String str2) {
        return Log.println_native(1, 3, str, str2);
    }

    public static int d(String str, String str2, Throwable th) {
        return Log.println_native(1, 3, str, str2 + '\n' + Log.getStackTraceString(th));
    }

    public static int i(String str, String str2) {
        return Log.println_native(1, 4, str, str2);
    }

    public static int i(String str, String str2, Throwable th) {
        return Log.println_native(1, 4, str, str2 + '\n' + Log.getStackTraceString(th));
    }

    public static int w(String str, String str2) {
        return Log.println_native(1, 5, str, str2);
    }

    public static int w(String str, String str2, Throwable th) {
        return Log.println_native(1, 5, str, str2 + '\n' + Log.getStackTraceString(th));
    }

    public static int w(String str, Throwable th) {
        return Log.println_native(1, 5, str, Log.getStackTraceString(th));
    }

    public static int e(String str, String str2) {
        return Log.println_native(1, 6, str, str2);
    }

    public static int e(String str, String str2, Throwable th) {
        return Log.println_native(1, 6, str, str2 + '\n' + Log.getStackTraceString(th));
    }

    public static int println(int i, String str, String str2) {
        return Log.println_native(1, i, str, str2);
    }

    public static boolean isLoggable(String str, int i) {
        return Log.isLoggable(str, i);
    }

    public static String pii(String str, Object obj) {
        String strValueOf = String.valueOf(obj);
        if (obj == null || TextUtils.isEmpty(strValueOf) || isLoggable(str, 2)) {
            return strValueOf;
        }
        return "[" + secureHash(strValueOf.getBytes()) + "]";
    }

    public static String pii(boolean z, Object obj) {
        String strValueOf = String.valueOf(obj);
        if (obj == null || TextUtils.isEmpty(strValueOf) || z) {
            return strValueOf;
        }
        return "[" + secureHash(strValueOf.getBytes()) + "]";
    }

    private static String secureHash(byte[] bArr) {
        if (USER_BUILD) {
            return "****";
        }
        try {
            return Base64.encodeToString(MessageDigest.getInstance(KeyProperties.DIGEST_SHA1).digest(bArr), 11);
        } catch (NoSuchAlgorithmException e) {
            return "####";
        }
    }
}
