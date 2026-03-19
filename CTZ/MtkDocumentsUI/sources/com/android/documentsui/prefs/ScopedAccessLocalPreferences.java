package com.android.documentsui.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.documentsui.base.SharedMinimal;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScopedAccessLocalPreferences {
    private static final Pattern KEY_PATTERN = Pattern.compile("^.+\\|(.+)\\|(.*)\\|(.+)$");

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static int getScopedAccessPermissionStatus(Context context, String str, String str2, String str3) {
        return getPrefs(context).getInt(getScopedAccessDenialsKey(str, str2, str3), 0);
    }

    public static void setScopedAccessPermissionStatus(Context context, String str, String str2, String str3, int i) {
        Preconditions.checkArgument(!TextUtils.isEmpty(str3), "Cannot pass empty directory - did you mean %s?", new Object[]{"ROOT_DIRECTORY"});
        String scopedAccessDenialsKey = getScopedAccessDenialsKey(str, str2, str3);
        if (SharedMinimal.DEBUG) {
            Log.d("ScopedAccessLocalPreferences", "Setting permission of " + str + ":" + str2 + ":" + str3 + " to " + statusAsString(i));
        }
        getPrefs(context).edit().putInt(scopedAccessDenialsKey, i).apply();
    }

    public static int clearScopedAccessPreferences(Context context, String str) {
        String str2 = "|" + str + "|";
        SharedPreferences prefs = getPrefs(context);
        SharedPreferences.Editor editorEdit = null;
        int i = 0;
        for (String str3 : prefs.getAll().keySet()) {
            if (str3.contains(str2)) {
                if (editorEdit == null) {
                    editorEdit = prefs.edit();
                }
                editorEdit.remove(str3);
                i++;
            }
        }
        if (editorEdit != null) {
            editorEdit.apply();
        }
        return i;
    }

    private static String getScopedAccessDenialsKey(String str, String str2, String str3) {
        int iMyUserId = UserHandle.myUserId();
        if (str2 == null) {
            return iMyUserId + "|" + str + "||" + str3;
        }
        return iMyUserId + "|" + str + "|" + str2 + "|" + str3;
    }

    public static void clearPackagePreferences(Context context, String str) {
        clearScopedAccessPreferences(context, str);
    }

    public static Set<String> getAllPackages(Context context) {
        SharedPreferences prefs = getPrefs(context);
        ArraySet arraySet = new ArraySet();
        Iterator<Map.Entry<String, ?>> it = prefs.getAll().entrySet().iterator();
        while (it.hasNext()) {
            String key = it.next().getKey();
            String str = getPackage(key);
            if (str == null) {
                Log.w("ScopedAccessLocalPreferences", "getAllPackages(): error parsing pref '" + key + "'");
            } else {
                arraySet.add(str);
            }
        }
        return arraySet;
    }

    public static List<Permission> getAllPermissions(Context context) {
        SharedPreferences prefs = getPrefs(context);
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            try {
                Permission permission = getPermission(key, (Integer) value);
                if (permission != null) {
                    arrayList.add(permission);
                }
            } catch (Exception e) {
                Log.w("ScopedAccessLocalPreferences", "error gettting value for key '" + key + "': " + value);
            }
        }
        return arrayList;
    }

    public static String statusAsString(int i) {
        switch (i) {
            case -1:
                return "PERMISSION_NEVER_ASK";
            case 0:
                return "PERMISSION_ASK";
            case 1:
                return "PERMISSION_ASK_AGAIN";
            case 2:
                return "PERMISSION_GRANTED";
            default:
                return "UNKNOWN";
        }
    }

    private static String getPackage(String str) {
        Matcher matcher = KEY_PATTERN.matcher(str);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private static Permission getPermission(String str, Integer num) {
        Matcher matcher = KEY_PATTERN.matcher(str);
        if (matcher.matches()) {
            return new Permission(matcher.group(1), matcher.group(2), matcher.group(3), num);
        }
        return null;
    }

    public static final class Permission {
        public final String directory;
        public final String pkg;
        public final int status;
        public final String uuid;

        public Permission(String str, String str2, String str3, Integer num) {
            this.pkg = str;
            this.uuid = TextUtils.isEmpty(str2) ? null : str2;
            this.directory = str3;
            this.status = num.intValue();
        }

        public String toString() {
            return "Permission: [pkg=" + this.pkg + ", uuid=" + this.uuid + ", dir=" + this.directory + ", status=" + ScopedAccessLocalPreferences.statusAsString(this.status) + " (" + this.status + ")]";
        }
    }
}
