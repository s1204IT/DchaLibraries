package mediatek.text.util;

import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.util.Patterns;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class LinkifyExt {
    private static final boolean DEBUG = "eng".equals(Build.TYPE);
    private static final String TAG = "LinkifyExt";
    private static Method sGetMtkWebUrlPattern;
    private static Method sGetWebUrl;
    private static Method sGetWebUrlNames;
    private static Class sMtkPatterns;

    static {
        try {
            sMtkPatterns = Class.forName("com.mediatek.util.MtkPatterns");
            sGetWebUrlNames = sMtkPatterns.getDeclaredMethod("getWebProtocolNames", String[].class);
            sGetWebUrlNames.setAccessible(true);
            sGetWebUrl = sMtkPatterns.getDeclaredMethod("getWebUrl", String.class, Integer.TYPE, Integer.TYPE);
            sGetWebUrl.setAccessible(true);
            sGetMtkWebUrlPattern = sMtkPatterns.getDeclaredMethod("getMtkWebUrlPattern", Pattern.class);
            sGetMtkWebUrlPattern.setAccessible(true);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "no extended class found!");
        } catch (NoSuchMethodException e2) {
            Log.d(TAG, "no extended method found!");
            e2.printStackTrace();
        }
    }

    public static String[] getExtWebProtocolNames(String[] strArr) {
        if (sGetWebUrlNames != null) {
            try {
                String[] strArr2 = (String[]) sGetWebUrlNames.invoke(null, strArr);
                try {
                    if (DEBUG) {
                        Log.d(TAG, "getExtWebProtocolNames(), webProtocolNames = " + strArr2);
                    }
                    return strArr2;
                } catch (IllegalAccessException e) {
                    strArr = strArr2;
                    Log.e(TAG, "sGetWebUrlNames access failed");
                    return strArr;
                } catch (InvocationTargetException e2) {
                    strArr = strArr2;
                    Log.e(TAG, "sGetWebUrlNames invoke failed");
                    return strArr;
                }
            } catch (IllegalAccessException e3) {
            } catch (InvocationTargetException e4) {
            }
        } else {
            return strArr;
        }
    }

    public static Pattern getExtWebUrlPattern(Pattern pattern) {
        if (sGetMtkWebUrlPattern != null) {
            try {
                return (Pattern) sGetMtkWebUrlPattern.invoke(null, pattern);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "sGetWebUrlNames access failed");
                return pattern;
            } catch (InvocationTargetException e2) {
                Log.e(TAG, "sGetWebUrlNames invoke failed");
                return pattern;
            }
        }
        return pattern;
    }

    public static Bundle getExtWebUrl(String str, int i, int i2, Pattern pattern) {
        if (pattern != Patterns.AUTOLINK_EMAIL_ADDRESS && sGetWebUrl != null) {
            try {
                Bundle bundle = (Bundle) sGetWebUrl.invoke(null, str, Integer.valueOf(i), Integer.valueOf(i2));
                if (DEBUG) {
                    Log.e(TAG, "getExtWebUrl, urlData = " + bundle);
                }
                return bundle;
            } catch (IllegalAccessException e) {
                Log.e(TAG, "can't access sGetWebUrl");
            } catch (InvocationTargetException e2) {
                Log.e(TAG, "can't invoke sGetWebUrl");
            }
        }
        Bundle bundle2 = new Bundle();
        bundle2.putString("value", str);
        bundle2.putInt(Telephony.BaseMmsColumns.START, i);
        bundle2.putInt("end", i2);
        return bundle2;
    }
}
