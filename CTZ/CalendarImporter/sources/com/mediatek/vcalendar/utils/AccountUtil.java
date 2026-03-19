package com.mediatek.vcalendar.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

public final class AccountUtil {
    public static final String MAIL_TYPE_EXCHANGE = "com.android.exchange";
    public static final String MAIL_TYPE_GOOGLE = "com.google";
    private static final String TAG = "AccountUtil";

    private AccountUtil() {
    }

    public static boolean hasExchangeOrGoogleAccount(Context context) {
        LogUtil.i(TAG, "hasExchangeOrGoogleAccount()");
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].type.equals("com.android.exchange") || accounts[i].type.equals("com.google")) {
                return true;
            }
        }
        return false;
    }
}
