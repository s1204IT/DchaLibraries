package com.android.server.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Slog;
import android.view.WindowManager;
import com.android.server.policy.WindowManagerPolicy;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class PolicyControl {
    private static final String NAME_IMMERSIVE_FULL = "immersive.full";
    private static final String NAME_IMMERSIVE_NAVIGATION = "immersive.navigation";
    private static final String NAME_IMMERSIVE_PRECONFIRMATIONS = "immersive.preconfirms";
    private static final String NAME_IMMERSIVE_STATUS = "immersive.status";
    private static Filter sImmersiveNavigationFilter;
    private static Filter sImmersivePreconfirmationsFilter;
    private static Filter sImmersiveStatusFilter;
    private static String sSettingValue;
    private static String TAG = "PolicyControl";
    private static boolean DEBUG = false;

    public static int getSystemUiVisibility(WindowManagerPolicy.WindowState windowState, WindowManager.LayoutParams layoutParams) {
        if (layoutParams == null) {
            layoutParams = windowState.getAttrs();
        }
        int systemUiVisibility = windowState != null ? windowState.getSystemUiVisibility() : layoutParams.systemUiVisibility | layoutParams.subtreeSystemUiVisibility;
        if (sImmersiveStatusFilter != null && sImmersiveStatusFilter.matches(layoutParams)) {
            systemUiVisibility = (systemUiVisibility | 5124) & (-1073742081);
        }
        if (sImmersiveNavigationFilter != null && sImmersiveNavigationFilter.matches(layoutParams)) {
            return (systemUiVisibility | 4610) & 2147483391;
        }
        return systemUiVisibility;
    }

    public static int getWindowFlags(WindowManagerPolicy.WindowState windowState, WindowManager.LayoutParams layoutParams) {
        if (layoutParams == null) {
            layoutParams = windowState.getAttrs();
        }
        int i = layoutParams.flags;
        if (sImmersiveStatusFilter != null && sImmersiveStatusFilter.matches(layoutParams)) {
            i = (i | 1024) & (-67110913);
        }
        if (sImmersiveNavigationFilter != null && sImmersiveNavigationFilter.matches(layoutParams)) {
            return i & (-134217729);
        }
        return i;
    }

    public static int adjustClearableFlags(WindowManagerPolicy.WindowState windowState, int i) {
        WindowManager.LayoutParams attrs = windowState != null ? windowState.getAttrs() : null;
        if (sImmersiveStatusFilter != null && sImmersiveStatusFilter.matches(attrs)) {
            return i & (-5);
        }
        return i;
    }

    public static boolean disableImmersiveConfirmation(String str) {
        return (sImmersivePreconfirmationsFilter != null && sImmersivePreconfirmationsFilter.matches(str)) || ActivityManager.isRunningInTestHarness();
    }

    public static void reloadFromSetting(Context context) {
        Throwable th;
        String stringForUser;
        if (DEBUG) {
            Slog.d(TAG, "reloadFromSetting()");
        }
        try {
            stringForUser = Settings.Global.getStringForUser(context.getContentResolver(), "policy_control", -2);
            try {
                if (sSettingValue == null || !sSettingValue.equals(stringForUser)) {
                    setFilters(stringForUser);
                    sSettingValue = stringForUser;
                }
            } catch (Throwable th2) {
                th = th2;
                Slog.w(TAG, "Error loading policy control, value=" + stringForUser, th);
            }
        } catch (Throwable th3) {
            th = th3;
            stringForUser = null;
        }
    }

    public static void dump(String str, PrintWriter printWriter) {
        dump("sImmersiveStatusFilter", sImmersiveStatusFilter, str, printWriter);
        dump("sImmersiveNavigationFilter", sImmersiveNavigationFilter, str, printWriter);
        dump("sImmersivePreconfirmationsFilter", sImmersivePreconfirmationsFilter, str, printWriter);
    }

    private static void dump(String str, Filter filter, String str2, PrintWriter printWriter) {
        printWriter.print(str2);
        printWriter.print("PolicyControl.");
        printWriter.print(str);
        printWriter.print('=');
        if (filter == null) {
            printWriter.println("null");
        } else {
            filter.dump(printWriter);
            printWriter.println();
        }
    }

    private static void setFilters(String str) {
        if (DEBUG) {
            Slog.d(TAG, "setFilters: " + str);
        }
        sImmersiveStatusFilter = null;
        sImmersiveNavigationFilter = null;
        sImmersivePreconfirmationsFilter = null;
        if (str != null) {
            for (String str2 : str.split(":")) {
                int iIndexOf = str2.indexOf(61);
                if (iIndexOf != -1) {
                    String strSubstring = str2.substring(0, iIndexOf);
                    String strSubstring2 = str2.substring(iIndexOf + 1);
                    if (strSubstring.equals(NAME_IMMERSIVE_FULL)) {
                        Filter filter = Filter.parse(strSubstring2);
                        sImmersiveNavigationFilter = filter;
                        sImmersiveStatusFilter = filter;
                        if (sImmersivePreconfirmationsFilter == null) {
                            sImmersivePreconfirmationsFilter = filter;
                        }
                    } else if (strSubstring.equals(NAME_IMMERSIVE_STATUS)) {
                        sImmersiveStatusFilter = Filter.parse(strSubstring2);
                    } else if (strSubstring.equals(NAME_IMMERSIVE_NAVIGATION)) {
                        Filter filter2 = Filter.parse(strSubstring2);
                        sImmersiveNavigationFilter = filter2;
                        if (sImmersivePreconfirmationsFilter == null) {
                            sImmersivePreconfirmationsFilter = filter2;
                        }
                    } else if (strSubstring.equals(NAME_IMMERSIVE_PRECONFIRMATIONS)) {
                        sImmersivePreconfirmationsFilter = Filter.parse(strSubstring2);
                    }
                }
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "immersiveStatusFilter: " + sImmersiveStatusFilter);
            Slog.d(TAG, "immersiveNavigationFilter: " + sImmersiveNavigationFilter);
            Slog.d(TAG, "immersivePreconfirmationsFilter: " + sImmersivePreconfirmationsFilter);
        }
    }

    private static class Filter {
        private static final String ALL = "*";
        private static final String APPS = "apps";
        private final ArraySet<String> mBlacklist;
        private final ArraySet<String> mWhitelist;

        private Filter(ArraySet<String> arraySet, ArraySet<String> arraySet2) {
            this.mWhitelist = arraySet;
            this.mBlacklist = arraySet2;
        }

        boolean matches(WindowManager.LayoutParams layoutParams) {
            if (layoutParams == null) {
                return false;
            }
            boolean z = layoutParams.type >= 1 && layoutParams.type <= 99;
            if ((z && this.mBlacklist.contains(APPS)) || onBlacklist(layoutParams.packageName)) {
                return false;
            }
            if (z && this.mWhitelist.contains(APPS)) {
                return true;
            }
            return onWhitelist(layoutParams.packageName);
        }

        boolean matches(String str) {
            return !onBlacklist(str) && onWhitelist(str);
        }

        private boolean onBlacklist(String str) {
            return this.mBlacklist.contains(str) || this.mBlacklist.contains(ALL);
        }

        private boolean onWhitelist(String str) {
            return this.mWhitelist.contains(ALL) || this.mWhitelist.contains(str);
        }

        void dump(PrintWriter printWriter) {
            printWriter.print("Filter[");
            dump("whitelist", this.mWhitelist, printWriter);
            printWriter.print(',');
            dump("blacklist", this.mBlacklist, printWriter);
            printWriter.print(']');
        }

        private void dump(String str, ArraySet<String> arraySet, PrintWriter printWriter) {
            printWriter.print(str);
            printWriter.print("=(");
            int size = arraySet.size();
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    printWriter.print(',');
                }
                printWriter.print(arraySet.valueAt(i));
            }
            printWriter.print(')');
        }

        public String toString() {
            StringWriter stringWriter = new StringWriter();
            dump(new PrintWriter((Writer) stringWriter, true));
            return stringWriter.toString();
        }

        static Filter parse(String str) {
            if (str == null) {
                return null;
            }
            ArraySet arraySet = new ArraySet();
            ArraySet arraySet2 = new ArraySet();
            for (String str2 : str.split(",")) {
                String strTrim = str2.trim();
                if (strTrim.startsWith("-") && strTrim.length() > 1) {
                    arraySet2.add(strTrim.substring(1));
                } else {
                    arraySet.add(strTrim);
                }
            }
            return new Filter(arraySet, arraySet2);
        }
    }
}
