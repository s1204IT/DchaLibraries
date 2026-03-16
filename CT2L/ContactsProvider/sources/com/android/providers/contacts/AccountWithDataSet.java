package com.android.providers.contacts;

import android.accounts.Account;
import android.text.TextUtils;
import com.google.common.base.Objects;

public class AccountWithDataSet {
    public static final AccountWithDataSet LOCAL = new AccountWithDataSet(null, null, null);
    private final String mAccountName;
    private final String mAccountType;
    private final String mDataSet;

    public AccountWithDataSet(String accountName, String accountType, String dataSet) {
        this.mAccountName = emptyToNull(accountName);
        this.mAccountType = emptyToNull(accountType);
        this.mDataSet = emptyToNull(dataSet);
    }

    private static final String emptyToNull(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        return text;
    }

    public static AccountWithDataSet get(String accountName, String accountType, String dataSet) {
        return new AccountWithDataSet(accountName, accountType, dataSet);
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
        return this.mAccountName == null && this.mAccountType == null;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AccountWithDataSet)) {
            return false;
        }
        AccountWithDataSet other = (AccountWithDataSet) obj;
        return Objects.equal(this.mAccountName, other.getAccountName()) && Objects.equal(this.mAccountType, other.getAccountType()) && Objects.equal(this.mDataSet, other.getDataSet());
    }

    public int hashCode() {
        int result = this.mAccountName != null ? this.mAccountName.hashCode() : 0;
        return (((result * 31) + (this.mAccountType != null ? this.mAccountType.hashCode() : 0)) * 31) + (this.mDataSet != null ? this.mDataSet.hashCode() : 0);
    }

    public String toString() {
        return "AccountWithDataSet {name=" + this.mAccountName + ", type=" + this.mAccountType + ", dataSet=" + this.mDataSet + "}";
    }

    public boolean inSystemAccounts(Account[] systemAccounts) {
        for (Account systemAccount : systemAccounts) {
            if (Objects.equal(systemAccount.name, getAccountName()) && Objects.equal(systemAccount.type, getAccountType())) {
                return true;
            }
        }
        return false;
    }
}
