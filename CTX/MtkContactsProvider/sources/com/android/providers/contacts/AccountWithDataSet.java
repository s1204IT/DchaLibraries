package com.android.providers.contacts;

import android.accounts.Account;
import android.text.TextUtils;
import com.google.common.base.Objects;
import com.mediatek.providers.contacts.AccountUtils;

public class AccountWithDataSet {
    public static final AccountWithDataSet LOCAL = new AccountWithDataSet(null, null, null);
    private final String mAccountName;
    private final String mAccountType;
    private final String mDataSet;

    public AccountWithDataSet(String str, String str2, String str3) {
        this.mAccountName = emptyToNull(str);
        this.mAccountType = emptyToNull(str2);
        this.mDataSet = emptyToNull(str3);
    }

    private static final String emptyToNull(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return str;
    }

    public static AccountWithDataSet get(String str, String str2, String str3) {
        return new AccountWithDataSet(str, str2, str3);
    }

    public String getAccountName() {
        return this.mAccountName;
    }

    public String getAccountType() {
        return this.mAccountType;
    }

    public String getDataSet() {
        return this.mDataSet;
    }

    public boolean isLocalAccount() {
        return AccountUtils.isLocalAccount(this.mAccountType, this.mAccountName);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AccountWithDataSet)) {
            return false;
        }
        AccountWithDataSet accountWithDataSet = (AccountWithDataSet) obj;
        return Objects.equal(this.mAccountName, accountWithDataSet.getAccountName()) && Objects.equal(this.mAccountType, accountWithDataSet.getAccountType()) && Objects.equal(this.mDataSet, accountWithDataSet.getDataSet());
    }

    public int hashCode() {
        return (31 * (((this.mAccountName != null ? this.mAccountName.hashCode() : 0) * 31) + (this.mAccountType != null ? this.mAccountType.hashCode() : 0))) + (this.mDataSet != null ? this.mDataSet.hashCode() : 0);
    }

    public String toString() {
        return "AccountWithDataSet {name=" + this.mAccountName + ", type=" + this.mAccountType + ", dataSet=" + this.mDataSet + "}";
    }

    public boolean inSystemAccounts(Account[] accountArr) {
        for (Account account : accountArr) {
            if (Objects.equal(account.name, getAccountName()) && Objects.equal(account.type, getAccountType())) {
                return true;
            }
        }
        return false;
    }
}
