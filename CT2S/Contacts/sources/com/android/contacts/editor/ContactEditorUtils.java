package com.android.contacts.editor;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ContactEditorUtils {
    private static final List<AccountWithDataSet> EMPTY_ACCOUNTS = ImmutableList.of();
    private static ContactEditorUtils sInstance;
    private final AccountTypeManager mAccountTypes;
    private final Context mContext;
    private final SharedPreferences mPrefs;

    private ContactEditorUtils(Context context) {
        this(context, AccountTypeManager.getInstance(context));
    }

    ContactEditorUtils(Context context, AccountTypeManager accountTypes) {
        this.mContext = context.getApplicationContext();
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        this.mAccountTypes = accountTypes;
    }

    public static synchronized ContactEditorUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ContactEditorUtils(context.getApplicationContext());
        }
        return sInstance;
    }

    void cleanupForTest() {
        this.mPrefs.edit().remove("ContactEditorUtils_default_account").remove("ContactEditorUtils_known_accounts").remove("ContactEditorUtils_anything_saved").apply();
    }

    void removeDefaultAccountForTest() {
        this.mPrefs.edit().remove("ContactEditorUtils_default_account").apply();
    }

    private void resetPreferenceValues() {
        this.mPrefs.edit().putString("ContactEditorUtils_known_accounts", "").putString("ContactEditorUtils_default_account", "").apply();
    }

    private List<AccountWithDataSet> getWritableAccounts() {
        return this.mAccountTypes.getAccounts(true);
    }

    private boolean isFirstLaunch() {
        return !this.mPrefs.getBoolean("ContactEditorUtils_anything_saved", false);
    }

    public void saveDefaultAndAllAccounts(AccountWithDataSet defaultAccount) {
        SharedPreferences.Editor editor = this.mPrefs.edit().putBoolean("ContactEditorUtils_anything_saved", true);
        if (defaultAccount == null) {
            editor.putString("ContactEditorUtils_known_accounts", "");
            editor.putString("ContactEditorUtils_default_account", "");
        } else {
            editor.putString("ContactEditorUtils_known_accounts", AccountWithDataSet.stringifyList(getWritableAccounts()));
            editor.putString("ContactEditorUtils_default_account", defaultAccount.stringify());
        }
        editor.apply();
    }

    public AccountWithDataSet getDefaultAccount() {
        String saved = this.mPrefs.getString("ContactEditorUtils_default_account", null);
        if (TextUtils.isEmpty(saved)) {
            return null;
        }
        try {
            return AccountWithDataSet.unstringify(saved);
        } catch (IllegalArgumentException exception) {
            Log.e("ContactEditorUtils", "Error with retrieving default account " + exception.toString());
            resetPreferenceValues();
            return null;
        }
    }

    boolean isValidAccount(AccountWithDataSet account) {
        if (account == null) {
            return true;
        }
        return getWritableAccounts().contains(account);
    }

    List<AccountWithDataSet> getSavedAccounts() {
        String saved = this.mPrefs.getString("ContactEditorUtils_known_accounts", null);
        if (TextUtils.isEmpty(saved)) {
            return EMPTY_ACCOUNTS;
        }
        try {
            return AccountWithDataSet.unstringifyList(saved);
        } catch (IllegalArgumentException exception) {
            Log.e("ContactEditorUtils", "Error with retrieving saved accounts " + exception.toString());
            resetPreferenceValues();
            return EMPTY_ACCOUNTS;
        }
    }

    public boolean shouldShowAccountChangedNotification() {
        if (isFirstLaunch()) {
            return true;
        }
        List<AccountWithDataSet> savedAccounts = getSavedAccounts();
        List<AccountWithDataSet> currentWritableAccounts = getWritableAccounts();
        for (AccountWithDataSet account : currentWritableAccounts) {
            if (!savedAccounts.contains(account)) {
                return true;
            }
        }
        AccountWithDataSet defaultAccount = getDefaultAccount();
        if (!isValidAccount(defaultAccount)) {
            return true;
        }
        if (defaultAccount == null && currentWritableAccounts.size() > 0) {
            Log.e("ContactEditorUtils", "Preferences file in an inconsistent state, request that the default account and current writable accounts be saved again");
            return true;
        }
        return false;
    }

    String[] getWritableAccountTypeStrings() {
        Set<String> types = Sets.newHashSet();
        for (AccountType type : this.mAccountTypes.getAccountTypes(true)) {
            types.add(type.accountType);
        }
        return (String[]) types.toArray(new String[types.size()]);
    }

    public Intent createAddWritableAccountIntent() {
        return AccountManager.newChooseAccountIntent(null, new ArrayList(), getWritableAccountTypeStrings(), false, null, null, null, null);
    }

    public AccountWithDataSet getCreatedAccount(int resultCode, Intent resultData) {
        if (resultData == null) {
            return null;
        }
        String accountType = resultData.getStringExtra("accountType");
        String accountName = resultData.getStringExtra("authAccount");
        if (TextUtils.isEmpty(accountType) || TextUtils.isEmpty(accountName)) {
            return null;
        }
        return new AccountWithDataSet(accountName, accountType, null);
    }
}
