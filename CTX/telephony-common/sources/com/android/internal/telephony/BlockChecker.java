package com.android.internal.telephony;

import android.content.Context;
import android.os.Bundle;
import android.provider.BlockedNumberContract;
import android.telephony.Rlog;

public class BlockChecker {
    private static final String TAG = "BlockChecker";
    private static final boolean VDBG = false;

    @Deprecated
    public static boolean isBlocked(Context context, String str) {
        return isBlocked(context, str, null);
    }

    public static boolean isBlocked(Context context, String str, Bundle bundle) {
        long jNanoTime = System.nanoTime();
        boolean z = false;
        try {
            if (BlockedNumberContract.SystemContract.shouldSystemBlockNumber(context, str, bundle)) {
                Rlog.d(TAG, str + " is blocked.");
                z = true;
            }
        } catch (Exception e) {
            Rlog.e(TAG, "Exception checking for blocked number: " + e);
        }
        int iNanoTime = (int) ((System.nanoTime() - jNanoTime) / 1000000);
        if (iNanoTime > 500) {
            Rlog.d(TAG, "Blocked number lookup took: " + iNanoTime + " ms.");
        }
        return z;
    }
}
