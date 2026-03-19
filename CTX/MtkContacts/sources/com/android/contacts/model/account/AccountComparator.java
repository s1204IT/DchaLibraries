package com.android.contacts.model.account;

import com.google.common.base.Objects;
import java.util.Comparator;

public class AccountComparator implements Comparator<AccountWithDataSet> {
    private AccountWithDataSet mDefaultAccount;

    public AccountComparator(AccountWithDataSet accountWithDataSet) {
        this.mDefaultAccount = accountWithDataSet;
    }

    @Override
    public int compare(AccountWithDataSet accountWithDataSet, AccountWithDataSet accountWithDataSet2) {
        if (Objects.equal(accountWithDataSet.name, accountWithDataSet2.name) && Objects.equal(accountWithDataSet.type, accountWithDataSet2.type) && Objects.equal(accountWithDataSet.dataSet, accountWithDataSet2.dataSet)) {
            return 0;
        }
        if (accountWithDataSet2.name == null || accountWithDataSet2.type == null) {
            return -1;
        }
        if (accountWithDataSet.name == null || accountWithDataSet.type == null) {
            return 1;
        }
        if (isWritableGoogleAccount(accountWithDataSet) && accountWithDataSet.equals(this.mDefaultAccount)) {
            return -1;
        }
        if (isWritableGoogleAccount(accountWithDataSet2) && accountWithDataSet2.equals(this.mDefaultAccount)) {
            return 1;
        }
        if (isWritableGoogleAccount(accountWithDataSet) && !isWritableGoogleAccount(accountWithDataSet2)) {
            return -1;
        }
        if (isWritableGoogleAccount(accountWithDataSet2) && !isWritableGoogleAccount(accountWithDataSet)) {
            return 1;
        }
        int iCompareToIgnoreCase = accountWithDataSet.name.compareToIgnoreCase(accountWithDataSet2.name);
        if (iCompareToIgnoreCase != 0) {
            return iCompareToIgnoreCase;
        }
        int iCompareToIgnoreCase2 = accountWithDataSet.type.compareToIgnoreCase(accountWithDataSet2.type);
        if (iCompareToIgnoreCase2 != 0) {
            return iCompareToIgnoreCase2;
        }
        if (accountWithDataSet.dataSet == null) {
            return -1;
        }
        if (accountWithDataSet2.dataSet == null) {
            return 1;
        }
        return accountWithDataSet.dataSet.compareToIgnoreCase(accountWithDataSet2.dataSet);
    }

    private static boolean isWritableGoogleAccount(AccountWithDataSet accountWithDataSet) {
        return GoogleAccountType.ACCOUNT_TYPE.equals(accountWithDataSet.type) && accountWithDataSet.dataSet == null;
    }
}
