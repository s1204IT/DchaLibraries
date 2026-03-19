package com.android.contacts.model.account;

import android.content.Context;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.util.DeviceLocalAccountTypeFactory;
import com.android.contactsbind.ObjectFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AccountDisplayInfoFactory {
    private final AccountTypeManager mAccountTypeManager;
    private final Context mContext;
    private final int mDeviceAccountCount;
    private final DeviceLocalAccountTypeFactory mDeviceAccountTypeFactory;
    private final int mSimAccountCount;

    public AccountDisplayInfoFactory(Context context, List<AccountWithDataSet> list) {
        this(context, AccountTypeManager.getInstance(context), ObjectFactory.getDeviceLocalAccountTypeFactory(context), list);
    }

    public AccountDisplayInfoFactory(Context context, AccountTypeManager accountTypeManager, DeviceLocalAccountTypeFactory deviceLocalAccountTypeFactory, List<AccountWithDataSet> list) {
        this.mContext = context;
        this.mAccountTypeManager = accountTypeManager;
        this.mDeviceAccountTypeFactory = deviceLocalAccountTypeFactory;
        this.mSimAccountCount = countOfType(2, list);
        this.mDeviceAccountCount = countOfType(1, list);
    }

    public AccountDisplayInfo getAccountDisplayInfo(AccountWithDataSet accountWithDataSet) {
        CharSequence displayLabel;
        AccountType accountTypeForAccount = this.mAccountTypeManager.getAccountTypeForAccount(accountWithDataSet);
        if (shouldUseTypeLabelForName(accountWithDataSet)) {
            displayLabel = accountTypeForAccount.getDisplayLabel(this.mContext);
        } else {
            displayLabel = accountWithDataSet.name;
        }
        return new AccountDisplayInfo(accountWithDataSet, displayLabel, accountTypeForAccount.getDisplayLabel(this.mContext), accountTypeForAccount.getDisplayIcon(this.mContext), DeviceLocalAccountTypeFactory.Util.isLocalAccountType(this.mDeviceAccountTypeFactory, accountTypeForAccount.accountType));
    }

    public AccountDisplayInfo getAccountDisplayInfoFor(ContactListFilter contactListFilter) {
        return getAccountDisplayInfo(contactListFilter.toAccountWithDataSet());
    }

    public AccountDisplayInfo getAccountDisplayInfoFor(RawContactDelta rawContactDelta) {
        return getAccountDisplayInfo(new AccountWithDataSet(rawContactDelta.getAccountName(), rawContactDelta.getAccountType(), rawContactDelta.getDataSet()));
    }

    public static AccountDisplayInfoFactory fromListFilters(Context context, List<ContactListFilter> list) {
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<ContactListFilter> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().toAccountWithDataSet());
        }
        return new AccountDisplayInfoFactory(context, arrayList);
    }

    private boolean shouldUseTypeLabelForName(AccountWithDataSet accountWithDataSet) {
        int iClassifyAccount = this.mDeviceAccountTypeFactory.classifyAccount(accountWithDataSet.type);
        if (iClassifyAccount == 2 && this.mSimAccountCount == 1) {
            return true;
        }
        return (iClassifyAccount == 1 && this.mDeviceAccountCount == 1) || accountWithDataSet.name == null;
    }

    private int countOfType(int i, List<AccountWithDataSet> list) {
        Iterator<AccountWithDataSet> it = list.iterator();
        int i2 = 0;
        while (it.hasNext()) {
            if (this.mDeviceAccountTypeFactory.classifyAccount(it.next().type) == i) {
                i2++;
            }
        }
        return i2;
    }
}
