package com.android.packageinstaller.wear;

import android.net.Uri;
import android.os.Bundle;

public class WearPackageArgs {
    public static String getPackageName(Bundle bundle) {
        return bundle.getString("com.google.android.clockwork.EXTRA_PACKAGE_NAME");
    }

    public static Bundle setPackageName(Bundle bundle, String str) {
        bundle.putString("com.google.android.clockwork.EXTRA_PACKAGE_NAME", str);
        return bundle;
    }

    public static Uri getAssetUri(Bundle bundle) {
        return (Uri) bundle.getParcelable("com.google.android.clockwork.EXTRA_ASSET_URI");
    }

    public static Uri getPermUri(Bundle bundle) {
        return (Uri) bundle.getParcelable("com.google.android.clockwork.EXTRA_PERM_URI");
    }

    public static boolean checkPerms(Bundle bundle) {
        return bundle.getBoolean("com.google.android.clockwork.EXTRA_CHECK_PERMS");
    }

    public static boolean skipIfSameVersion(Bundle bundle) {
        return bundle.getBoolean("com.google.android.clockwork.EXTRA_SKIP_IF_SAME_VERSION");
    }

    public static int getCompanionSdkVersion(Bundle bundle) {
        return bundle.getInt("com.google.android.clockwork.EXTRA_KEY_COMPANION_SDK_VERSION");
    }

    public static int getCompanionDeviceVersion(Bundle bundle) {
        return bundle.getInt("com.google.android.clockwork.EXTRA_KEY_COMPANION_DEVICE_VERSION");
    }

    public static String getCompressionAlg(Bundle bundle) {
        return bundle.getString("com.google.android.clockwork.EXTRA_KEY_COMPRESSION_ALG");
    }

    public static int getStartId(Bundle bundle) {
        return bundle.getInt("com.google.android.clockwork.EXTRA_START_ID");
    }

    public static boolean skipIfLowerVersion(Bundle bundle) {
        return bundle.getBoolean("com.google.android.clockwork.EXTRA_SKIP_IF_LOWER_VERSION", false);
    }

    public static Bundle setStartId(Bundle bundle, int i) {
        bundle.putInt("com.google.android.clockwork.EXTRA_START_ID", i);
        return bundle;
    }
}
