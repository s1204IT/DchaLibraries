package com.mediatek.calendarimporter.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

public final class Utils {
    public static final int DEFAULT_COLOR = 17170450;
    public static final String MAIL_TYPE_EXCHANGE = "com.android.exchange";
    public static final String MAIL_TYPE_GOOGLE = "com.google";

    private Utils() {
    }

    public static boolean hasExchangeOrGoogleAccount(Context context) {
        for (Account account : AccountManager.get(context.getApplicationContext()).getAccounts()) {
            if (isExchangeOrGoogleAccount(account)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExchangeOrGoogleAccount(Account account) {
        return account.type.equals("com.android.exchange") || account.type.equals("com.google");
    }

    public static int getThemeMainColor(Context context, int i) {
        return i;
    }
}
