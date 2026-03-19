package com.android.documentsui.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import com.android.documentsui.R;
import java.text.Collator;

public final class Shared {
    private static final Collator sCollator = Collator.getInstance();

    static {
        sCollator.setStrength(1);
    }

    @Deprecated
    public static final String getQuantityString(Context context, int i, int i2) {
        return context.getResources().getQuantityString(i, i2, Integer.valueOf(i2));
    }

    public static String formatTime(Context context, long j) {
        int i;
        Time time = new Time();
        time.set(j);
        Time time2 = new Time();
        time2.setToNow();
        if (time.year != time2.year) {
            i = 526868;
        } else if (time.yearDay != time2.yearDay) {
            i = 526864;
        } else {
            i = 526849;
        }
        return DateUtils.formatDateTime(context, j, i);
    }

    public static int compareToIgnoreCaseNullable(String str, String str2) {
        boolean zIsEmpty = TextUtils.isEmpty(str);
        boolean zIsEmpty2 = TextUtils.isEmpty(str2);
        if (zIsEmpty && zIsEmpty2) {
            return 0;
        }
        if (zIsEmpty) {
            return -1;
        }
        if (zIsEmpty2) {
            return 1;
        }
        return sCollator.compare(str, str2);
    }

    public static String getCallingPackageName(Activity activity) {
        String stringExtra;
        String callingPackage = activity.getCallingPackage();
        try {
            ApplicationInfo applicationInfo = activity.getPackageManager().getApplicationInfo(callingPackage, 0);
            if ((!applicationInfo.isSystemApp() && !applicationInfo.isUpdatedSystemApp()) || (stringExtra = activity.getIntent().getStringExtra("android.content.extra.PACKAGE_NAME")) == null) {
                return callingPackage;
            }
            if (!TextUtils.isEmpty(stringExtra)) {
                return stringExtra;
            }
            return callingPackage;
        } catch (PackageManager.NameNotFoundException e) {
            return callingPackage;
        }
    }

    public static Uri getDefaultRootUri(Activity activity) {
        Uri uri = Uri.parse(activity.getResources().getString(R.string.default_root_uri));
        if (!DocumentsContract.isRootUri(activity, uri)) {
            throw new RuntimeException("Default Root URI is not a valid root URI.");
        }
        return uri;
    }

    public static boolean isHardwareKeyboardAvailable(Context context) {
        return context.getResources().getConfiguration().keyboard != 1;
    }

    public static void ensureKeyboardPresent(Context context, AlertDialog alertDialog) {
        if (!isHardwareKeyboardAvailable(context)) {
            alertDialog.getWindow().setSoftInputMode(4);
        }
    }

    public static boolean shouldShowDocumentsRoot(Context context) {
        return context.getResources().getBoolean(R.bool.show_documents_root);
    }

    public static boolean mustShowDeviceRoot(Intent intent) {
        return intent.getBooleanExtra("android.content.extra.SHOW_ADVANCED", false);
    }

    public static void checkMainLoop() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            Log.e("Documents", "Calling from non-UI thread!");
        }
    }

    public static <T> T findView(Activity activity, int... iArr) {
        for (int i : iArr) {
            T t = (T) activity.findViewById(i);
            if (t != null) {
                return t;
            }
        }
        return null;
    }
}
