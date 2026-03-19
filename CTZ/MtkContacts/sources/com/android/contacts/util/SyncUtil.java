package com.android.contacts.util;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.GoogleAccountType;
import java.util.Iterator;
import java.util.List;

public final class SyncUtil {
    public static final int SYNC_SETTING_ACCOUNT_SYNC_OFF = 2;
    public static final int SYNC_SETTING_GLOBAL_SYNC_OFF = 1;
    public static final int SYNC_SETTING_SYNC_ON = 0;
    private static final String TAG = "SyncUtil";

    private SyncUtil() {
    }

    public static final boolean isSyncStatusPendingOrActive(Account account) {
        if (account == null) {
            return false;
        }
        return ContentResolver.isSyncPending(account, "com.android.contacts") || ContentResolver.isSyncActive(account, "com.android.contacts");
    }

    public static final boolean isAnySyncing(List<AccountWithDataSet> list) {
        Iterator<AccountWithDataSet> it = list.iterator();
        while (it.hasNext()) {
            if (isSyncStatusPendingOrActive(it.next().getAccountOrNull())) {
                return true;
            }
        }
        return false;
    }

    public static final boolean isUnsyncableGoogleAccount(Account account) {
        return account != null && GoogleAccountType.ACCOUNT_TYPE.equals(account.type) && ContentResolver.getIsSyncable(account, "com.android.contacts") <= 0;
    }

    public static final boolean hasSyncableAccount(AccountTypeManager accountTypeManager) {
        return !accountTypeManager.getWritableGoogleAccounts().isEmpty();
    }

    public static boolean isAlertVisible(Context context, Account account, int i) {
        return i == 1 ? SharedPreferenceUtil.getNumOfDismissesForAutoSyncOff(context) == 0 : i == 2 && account != null && SharedPreferenceUtil.getNumOfDismissesforAccountSyncOff(context, account.name) == 0;
    }

    public static int calculateReasonSyncOff(Context context, Account account) {
        if (!ContentResolver.getMasterSyncAutomatically()) {
            if (account != null) {
                SharedPreferenceUtil.resetNumOfDismissesForAccountSyncOff(context, account.name);
                return 1;
            }
            return 1;
        }
        SharedPreferenceUtil.resetNumOfDismissesForAutoSyncOff(context);
        if (account != null) {
            if (!ContentResolver.getSyncAutomatically(account, "com.android.contacts")) {
                return 2;
            }
            SharedPreferenceUtil.resetNumOfDismissesForAccountSyncOff(context, account.name);
            return 0;
        }
        return 0;
    }

    public static boolean isNetworkConnected(Context context) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}
