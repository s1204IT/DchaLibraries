package com.android.contacts.model;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.GoogleAccountType;
import com.android.contactsbind.ObjectFactory;
import com.android.contactsbind.experiments.Flags;
import com.mediatek.contacts.util.Log;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class DeviceLocalAccountLocator {
    public static final DeviceLocalAccountLocator NULL_ONLY = new DeviceLocalAccountLocator() {
        @Override
        public List<AccountWithDataSet> getDeviceLocalAccounts() {
            return Collections.singletonList(AccountWithDataSet.getNullAccount());
        }
    };

    public abstract List<AccountWithDataSet> getDeviceLocalAccounts();

    public static DeviceLocalAccountLocator create(Context context, Set<String> set) {
        Flags.getInstance().setBoolean("Account__cp2_device_account_detection_enabled", true);
        if (Flags.getInstance().getBoolean("Account__cp2_device_account_detection_enabled")) {
            return new Cp2DeviceLocalAccountLocator(context.getContentResolver(), ObjectFactory.getDeviceLocalAccountTypeFactory(context), set);
        }
        Log.i("DeviceLocalAccountLocator", "Create(context) return NULL_ONLY");
        return NULL_ONLY;
    }

    public static DeviceLocalAccountLocator create(Context context) {
        Flags.getInstance().setBoolean("Account__cp2_device_account_detection_enabled", true);
        AccountManager accountManager = (AccountManager) context.getSystemService("account");
        HashSet hashSet = new HashSet();
        for (Account account : accountManager.getAccounts()) {
            Log.i("DeviceLocalAccountLocator", "knownTypes:" + Log.anonymize(account.name) + ", " + account.type);
            hashSet.add(account.type);
        }
        if (Flags.getInstance().getBoolean("Account__cp2_device_account_detection_enabled")) {
            return new Cp2DeviceLocalAccountLocator(context.getContentResolver(), ObjectFactory.getDeviceLocalAccountTypeFactory(context), hashSet);
        }
        return new NexusDeviceAccountLocator(accountManager);
    }

    public static class NexusDeviceAccountLocator extends DeviceLocalAccountLocator {
        private final AccountManager mAccountManager;

        public NexusDeviceAccountLocator(AccountManager accountManager) {
            this.mAccountManager = accountManager;
        }

        @Override
        public List<AccountWithDataSet> getDeviceLocalAccounts() {
            if (this.mAccountManager.getAccountsByType(GoogleAccountType.ACCOUNT_TYPE).length > 0) {
                return Collections.emptyList();
            }
            return Collections.singletonList(AccountWithDataSet.getNullAccount());
        }
    }
}
