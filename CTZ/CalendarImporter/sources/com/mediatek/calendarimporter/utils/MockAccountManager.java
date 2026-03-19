package com.mediatek.calendarimporter.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

public class MockAccountManager extends AccountManager {
    public Account[] mAccounts;

    public MockAccountManager(Context context, Account[] accountArr) {
        super(context, null);
        this.mAccounts = accountArr;
    }

    @Override
    public Account[] getAccountsByType(String str) {
        if (this.mAccounts == null) {
            this.mAccounts = new Account[0];
        }
        return this.mAccounts;
    }

    @Override
    public Account[] getAccounts() {
        if (this.mAccounts == null) {
            this.mAccounts = new Account[0];
        }
        return this.mAccounts;
    }

    public void removeAccount() {
        this.mAccounts = null;
    }
}
