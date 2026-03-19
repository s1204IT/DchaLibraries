package android.os;

import android.annotation.SystemApi;
import android.util.Log;
import android.util.MutableInt;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;
import java.util.HashMap;

@SystemApi
public class SystemProperties {
    public static final int PROP_NAME_MAX = Integer.MAX_VALUE;
    public static final int PROP_VALUE_MAX = 91;
    private static final String TAG = "SystemProperties";
    private static final boolean TRACK_KEY_ACCESS = false;

    @GuardedBy("sChangeCallbacks")
    private static final ArrayList<Runnable> sChangeCallbacks = new ArrayList<>();

    @GuardedBy("sRoReads")
    private static final HashMap<String, MutableInt> sRoReads = null;

    private static native void native_add_change_callback();

    private static native String native_get(String str);

    private static native String native_get(String str, String str2);

    private static native boolean native_get_boolean(String str, boolean z);

    private static native int native_get_int(String str, int i);

    private static native long native_get_long(String str, long j);

    private static native void native_report_sysprop_change();

    private static native void native_set(String str, String str2);

    private static void onKeyAccess(String str) {
    }

    @SystemApi
    public static String get(String str) {
        return native_get(str);
    }

    @SystemApi
    public static String get(String str, String str2) {
        return native_get(str, str2);
    }

    @SystemApi
    public static int getInt(String str, int i) {
        return native_get_int(str, i);
    }

    @SystemApi
    public static long getLong(String str, long j) {
        return native_get_long(str, j);
    }

    @SystemApi
    public static boolean getBoolean(String str, boolean z) {
        return native_get_boolean(str, z);
    }

    public static void set(String str, String str2) {
        if (str2 != null && !str2.startsWith("ro.") && str2.length() > 91) {
            throw new IllegalArgumentException("value of system property '" + str + "' is longer than 91 characters: " + str2);
        }
        native_set(str, str2);
    }

    public static void addChangeCallback(Runnable runnable) {
        synchronized (sChangeCallbacks) {
            if (sChangeCallbacks.size() == 0) {
                native_add_change_callback();
            }
            sChangeCallbacks.add(runnable);
        }
    }

    private static void callChangeCallbacks() {
        synchronized (sChangeCallbacks) {
            if (sChangeCallbacks.size() == 0) {
                return;
            }
            ArrayList arrayList = new ArrayList(sChangeCallbacks);
            for (int i = 0; i < arrayList.size(); i++) {
                try {
                    ((Runnable) arrayList.get(i)).run();
                } catch (Throwable th) {
                    Log.wtf(TAG, "Exception in SystemProperties change callback", th);
                }
            }
        }
    }

    public static void reportSyspropChanged() {
        native_report_sysprop_change();
    }

    private SystemProperties() {
    }
}
