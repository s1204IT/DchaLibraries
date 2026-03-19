package com.android.server.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Slog;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;
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

    public static int getSystemUiVisibility(WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs) {
        if (attrs == null) {
            attrs = win.getAttrs();
        }
        int vis = win != null ? win.getSystemUiVisibility() : attrs.systemUiVisibility;
        if (sImmersiveStatusFilter != null && sImmersiveStatusFilter.matches(attrs)) {
            vis = (vis | 5124) & (-1073742081);
        }
        if (sImmersiveNavigationFilter != null && sImmersiveNavigationFilter.matches(attrs)) {
            return (vis | 4610) & 2147483391;
        }
        return vis;
    }

    public static int getWindowFlags(WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs) {
        if (attrs == null) {
            attrs = win.getAttrs();
        }
        int flags = attrs.flags;
        if (sImmersiveStatusFilter != null && sImmersiveStatusFilter.matches(attrs)) {
            flags = (flags | 1024) & (-67110913);
        }
        if (sImmersiveNavigationFilter != null && sImmersiveNavigationFilter.matches(attrs)) {
            return flags & (-134217729);
        }
        return flags;
    }

    public static int adjustClearableFlags(WindowManagerPolicy.WindowState win, int clearableFlags) {
        WindowManager.LayoutParams attrs = win != null ? win.getAttrs() : null;
        if (sImmersiveStatusFilter != null && sImmersiveStatusFilter.matches(attrs)) {
            return clearableFlags & (-5);
        }
        return clearableFlags;
    }

    public static boolean disableImmersiveConfirmation(String pkg) {
        if (sImmersivePreconfirmationsFilter == null || !sImmersivePreconfirmationsFilter.matches(pkg)) {
            return ActivityManager.isRunningInTestHarness();
        }
        return true;
    }

    public static void reloadFromSetting(Context context) {
        if (DEBUG) {
            Slog.d(TAG, "reloadFromSetting()");
        }
        String value = null;
        try {
            value = Settings.Global.getStringForUser(context.getContentResolver(), "policy_control", -2);
            if (sSettingValue == null || !sSettingValue.equals(value)) {
                setFilters(value);
                sSettingValue = value;
            }
        } catch (Throwable t) {
            Slog.w(TAG, "Error loading policy control, value=" + value, t);
        }
    }

    public static void dump(String prefix, PrintWriter pw) {
        dump("sImmersiveStatusFilter", sImmersiveStatusFilter, prefix, pw);
        dump("sImmersiveNavigationFilter", sImmersiveNavigationFilter, prefix, pw);
        dump("sImmersivePreconfirmationsFilter", sImmersivePreconfirmationsFilter, prefix, pw);
    }

    private static void dump(String name, Filter filter, String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("PolicyControl.");
        pw.print(name);
        pw.print('=');
        if (filter == null) {
            pw.println("null");
        } else {
            filter.dump(pw);
            pw.println();
        }
    }

    private static void setFilters(String value) {
        if (DEBUG) {
            Slog.d(TAG, "setFilters: " + value);
        }
        sImmersiveStatusFilter = null;
        sImmersiveNavigationFilter = null;
        sImmersivePreconfirmationsFilter = null;
        if (value != null) {
            String[] nvps = value.split(":");
            for (String nvp : nvps) {
                int i = nvp.indexOf(61);
                if (i != -1) {
                    String n = nvp.substring(0, i);
                    String v = nvp.substring(i + 1);
                    if (n.equals(NAME_IMMERSIVE_FULL)) {
                        Filter f = Filter.parse(v);
                        sImmersiveNavigationFilter = f;
                        sImmersiveStatusFilter = f;
                        if (sImmersivePreconfirmationsFilter == null) {
                            sImmersivePreconfirmationsFilter = f;
                        }
                    } else if (n.equals(NAME_IMMERSIVE_STATUS)) {
                        sImmersiveStatusFilter = Filter.parse(v);
                    } else if (n.equals(NAME_IMMERSIVE_NAVIGATION)) {
                        Filter f2 = Filter.parse(v);
                        sImmersiveNavigationFilter = f2;
                        if (sImmersivePreconfirmationsFilter == null) {
                            sImmersivePreconfirmationsFilter = f2;
                        }
                    } else if (n.equals(NAME_IMMERSIVE_PRECONFIRMATIONS)) {
                        sImmersivePreconfirmationsFilter = Filter.parse(v);
                    }
                }
            }
        }
        if (!DEBUG) {
            return;
        }
        Slog.d(TAG, "immersiveStatusFilter: " + sImmersiveStatusFilter);
        Slog.d(TAG, "immersiveNavigationFilter: " + sImmersiveNavigationFilter);
        Slog.d(TAG, "immersivePreconfirmationsFilter: " + sImmersivePreconfirmationsFilter);
    }

    private static class Filter {
        private static final String ALL = "*";
        private static final String APPS = "apps";
        private final ArraySet<String> mBlacklist;
        private final ArraySet<String> mWhitelist;

        private Filter(ArraySet<String> whitelist, ArraySet<String> blacklist) {
            this.mWhitelist = whitelist;
            this.mBlacklist = blacklist;
        }

        boolean matches(WindowManager.LayoutParams attrs) {
            if (attrs == null) {
                return false;
            }
            boolean isApp = attrs.type >= 1 && attrs.type <= 99;
            if ((isApp && this.mBlacklist.contains(APPS)) || onBlacklist(attrs.packageName)) {
                return false;
            }
            if (isApp && this.mWhitelist.contains(APPS)) {
                return true;
            }
            return onWhitelist(attrs.packageName);
        }

        boolean matches(String packageName) {
            if (onBlacklist(packageName)) {
                return false;
            }
            return onWhitelist(packageName);
        }

        private boolean onBlacklist(String packageName) {
            if (this.mBlacklist.contains(packageName)) {
                return true;
            }
            return this.mBlacklist.contains(ALL);
        }

        private boolean onWhitelist(String packageName) {
            if (this.mWhitelist.contains(ALL)) {
                return true;
            }
            return this.mWhitelist.contains(packageName);
        }

        void dump(PrintWriter pw) {
            pw.print("Filter[");
            dump("whitelist", this.mWhitelist, pw);
            pw.print(',');
            dump("blacklist", this.mBlacklist, pw);
            pw.print(']');
        }

        private void dump(String name, ArraySet<String> set, PrintWriter pw) {
            pw.print(name);
            pw.print("=(");
            int n = set.size();
            for (int i = 0; i < n; i++) {
                if (i > 0) {
                    pw.print(',');
                }
                pw.print(set.valueAt(i));
            }
            pw.print(')');
        }

        public String toString() {
            StringWriter sw = new StringWriter();
            dump(new PrintWriter((Writer) sw, true));
            return sw.toString();
        }

        static Filter parse(String value) {
            if (value == null) {
                return null;
            }
            ArraySet<String> whitelist = new ArraySet<>();
            ArraySet<String> blacklist = new ArraySet<>();
            for (String str : value.split(",")) {
                String token = str.trim();
                if (token.startsWith("-") && token.length() > 1) {
                    blacklist.add(token.substring(1));
                } else {
                    whitelist.add(token);
                }
            }
            return new Filter(whitelist, blacklist);
        }
    }
}
