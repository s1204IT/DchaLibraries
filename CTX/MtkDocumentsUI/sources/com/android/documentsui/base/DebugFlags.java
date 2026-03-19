package com.android.documentsui.base;

import android.os.Bundle;

public final class DebugFlags {
    private static String mQvPackage;
    private static boolean sDocumentDetailsEnabled;
    private static int sForcedPageOffset = -1;
    private static int sForcedPageLimit = -1;

    public static void setQuickViewer(String str) {
        mQvPackage = str;
    }

    public static String getQuickViewer() {
        return mQvPackage;
    }

    public static void setDocumentDetailsEnabled(boolean z) {
        sDocumentDetailsEnabled = z;
    }

    public static boolean getDocumentDetailsEnabled() {
        return sDocumentDetailsEnabled;
    }

    public static void setForcedPaging(int i, int i2) {
        sForcedPageOffset = i;
        sForcedPageLimit = i2;
    }

    public static boolean addForcedPagingArgs(Bundle bundle) {
        boolean z;
        if (sForcedPageOffset >= 0) {
            bundle.putInt("android:query-arg-offset", sForcedPageOffset);
            z = true;
        } else {
            z = false;
        }
        if (sForcedPageLimit >= 0) {
            bundle.putInt("android:query-arg-limit", sForcedPageLimit);
            return z | true;
        }
        return z;
    }
}
