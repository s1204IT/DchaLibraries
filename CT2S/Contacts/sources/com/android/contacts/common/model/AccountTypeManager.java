package com.android.contacts.common.model;

import android.accounts.Account;
import android.content.Context;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountTypeWithDataSet;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.dataitem.DataKind;
import java.util.List;
import java.util.Map;

public abstract class AccountTypeManager {
    private static AccountTypeManager mAccountTypeManager;
    private static final Object mInitializationLock = new Object();

    public abstract AccountType getAccountType(AccountTypeWithDataSet accountTypeWithDataSet);

    public abstract List<AccountType> getAccountTypes(boolean z);

    public abstract List<AccountWithDataSet> getAccounts(boolean z);

    public abstract List<AccountWithDataSet> getGroupWritableAccounts();

    public abstract Map<AccountTypeWithDataSet, AccountType> getUsableInvitableAccountTypes();

    public abstract List<AccountWithDataSet> getWritableAccountsWithoutSim();

    public abstract void onAccountsUpdated(Account[] accountArr);

    public static AccountTypeManager getInstance(Context context) {
        synchronized (mInitializationLock) {
            if (mAccountTypeManager == null) {
                mAccountTypeManager = new AccountTypeManagerImpl(context.getApplicationContext());
            }
        }
        return mAccountTypeManager;
    }

    public static void setInstanceForTest(AccountTypeManager mockManager) {
        synchronized (mInitializationLock) {
            mAccountTypeManager = mockManager;
        }
    }

    public final AccountType getAccountType(String accountType, String dataSet) {
        return getAccountType(AccountTypeWithDataSet.get(accountType, dataSet));
    }

    public final AccountType getAccountTypeForAccount(AccountWithDataSet account) {
        return getAccountType(account.getAccountTypeWithDataSet());
    }

    public DataKind getKindOrFallback(AccountType type, String mimeType) {
        if (type == null) {
            return null;
        }
        return type.getKindForMimetype(mimeType);
    }

    public boolean contains(AccountWithDataSet account, boolean contactWritableOnly) {
        for (AccountWithDataSet account_2 : getAccounts(false)) {
            if (account.equals(account_2)) {
                return true;
            }
        }
        return false;
    }
}
