package com.android.internal.util;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.function.Predicate;

public final class DumpUtils {
    private static final boolean DEBUG = false;
    private static final String TAG = "DumpUtils";

    public interface Dump {
        void dump(PrintWriter printWriter, String str);
    }

    private DumpUtils() {
    }

    public static void dumpAsync(Handler handler, final Dump dump, PrintWriter printWriter, final String str, long j) {
        final StringWriter stringWriter = new StringWriter();
        if (handler.runWithScissors(new Runnable() {
            @Override
            public void run() {
                FastPrintWriter fastPrintWriter = new FastPrintWriter(stringWriter);
                dump.dump(fastPrintWriter, str);
                fastPrintWriter.close();
            }
        }, j)) {
            printWriter.print(stringWriter.toString());
        } else {
            printWriter.println("... timed out");
        }
    }

    private static void logMessage(PrintWriter printWriter, String str) {
        printWriter.println(str);
    }

    public static boolean checkDumpPermission(Context context, String str, PrintWriter printWriter) {
        if (context.checkCallingOrSelfPermission(Manifest.permission.DUMP) != 0) {
            logMessage(printWriter, "Permission Denial: can't dump " + str + " from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " due to missing android.permission.DUMP permission");
            return false;
        }
        return true;
    }

    public static boolean checkUsageStatsPermission(Context context, String str, PrintWriter printWriter) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0 || callingUid == 1000 || callingUid == 1067 || callingUid == 2000) {
            return true;
        }
        if (context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) != 0) {
            logMessage(printWriter, "Permission Denial: can't dump " + str + " from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " due to missing android.permission.PACKAGE_USAGE_STATS permission");
            return false;
        }
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        String[] packagesForUid = context.getPackageManager().getPackagesForUid(callingUid);
        if (packagesForUid != null) {
            for (String str2 : packagesForUid) {
                int iNoteOpNoThrow = appOpsManager.noteOpNoThrow(43, callingUid, str2);
                if (iNoteOpNoThrow == 0 || iNoteOpNoThrow == 3) {
                    return true;
                }
            }
        }
        logMessage(printWriter, "Permission Denial: can't dump " + str + " from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " due to android:get_usage_stats app-op not allowed");
        return false;
    }

    public static boolean checkDumpAndUsageStatsPermission(Context context, String str, PrintWriter printWriter) {
        return checkDumpPermission(context, str, printWriter) && checkUsageStatsPermission(context, str, printWriter);
    }

    public static boolean isPlatformPackage(String str) {
        return str != null && (str.equals(ZenModeConfig.SYSTEM_AUTHORITY) || str.startsWith("android.") || str.startsWith("com.android."));
    }

    public static boolean isPlatformPackage(ComponentName componentName) {
        return componentName != null && isPlatformPackage(componentName.getPackageName());
    }

    public static boolean isPlatformPackage(ComponentName.WithComponentName withComponentName) {
        return withComponentName != null && isPlatformPackage(withComponentName.getComponentName());
    }

    public static boolean isNonPlatformPackage(String str) {
        return (str == null || isPlatformPackage(str)) ? false : true;
    }

    public static boolean isNonPlatformPackage(ComponentName componentName) {
        return componentName != null && isNonPlatformPackage(componentName.getPackageName());
    }

    public static boolean isNonPlatformPackage(ComponentName.WithComponentName withComponentName) {
        return (withComponentName == null || isPlatformPackage(withComponentName.getComponentName())) ? false : true;
    }

    public static <TRec extends ComponentName.WithComponentName> Predicate<TRec> filterRecord(final String str) {
        if (TextUtils.isEmpty(str)) {
            return new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return DumpUtils.lambda$filterRecord$0((ComponentName.WithComponentName) obj);
                }
            };
        }
        if ("all".equals(str)) {
            return new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return Objects.nonNull((ComponentName.WithComponentName) obj);
                }
            };
        }
        if ("all-platform".equals(str)) {
            return new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return DumpUtils.isPlatformPackage((ComponentName.WithComponentName) obj);
                }
            };
        }
        if ("all-non-platform".equals(str)) {
            return new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return DumpUtils.isNonPlatformPackage((ComponentName.WithComponentName) obj);
                }
            };
        }
        final ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(str);
        if (componentNameUnflattenFromString != null) {
            return new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return DumpUtils.lambda$filterRecord$1(componentNameUnflattenFromString, (ComponentName.WithComponentName) obj);
                }
            };
        }
        final int intWithBase = ParseUtils.parseIntWithBase(str, 16, -1);
        return new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DumpUtils.lambda$filterRecord$2(intWithBase, str, (ComponentName.WithComponentName) obj);
            }
        };
    }

    static boolean lambda$filterRecord$0(ComponentName.WithComponentName withComponentName) {
        return false;
    }

    static boolean lambda$filterRecord$1(ComponentName componentName, ComponentName.WithComponentName withComponentName) {
        return withComponentName != null && componentName.equals(withComponentName.getComponentName());
    }

    static boolean lambda$filterRecord$2(int i, String str, ComponentName.WithComponentName withComponentName) {
        return (i != -1 && System.identityHashCode(withComponentName) == i) || withComponentName.getComponentName().flattenToString().toLowerCase().contains(str.toLowerCase());
    }
}
