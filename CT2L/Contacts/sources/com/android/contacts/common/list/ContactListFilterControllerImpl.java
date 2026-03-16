package com.android.contacts.common.list;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import java.util.ArrayList;
import java.util.List;

class ContactListFilterControllerImpl extends ContactListFilterController {
    private final Context mContext;
    private final List<ContactListFilterController.ContactListFilterListener> mListeners = new ArrayList();
    private ContactListFilter mFilter = ContactListFilter.restoreDefaultPreferences(getSharedPreferences());

    public ContactListFilterControllerImpl(Context context) {
        this.mContext = context;
        checkFilterValidity(true);
    }

    @Override
    public void addListener(ContactListFilterController.ContactListFilterListener listener) {
        this.mListeners.add(listener);
    }

    @Override
    public void removeListener(ContactListFilterController.ContactListFilterListener listener) {
        this.mListeners.remove(listener);
    }

    @Override
    public ContactListFilter getFilter() {
        return this.mFilter;
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this.mContext);
    }

    @Override
    public void setContactListFilter(ContactListFilter filter, boolean persistent) {
        setContactListFilter(filter, persistent, true);
    }

    private void setContactListFilter(ContactListFilter filter, boolean persistent, boolean notifyListeners) {
        if (!filter.equals(this.mFilter)) {
            this.mFilter = filter;
            if (persistent) {
                ContactListFilter.storeToPreferences(getSharedPreferences(), this.mFilter);
            }
            if (notifyListeners && !this.mListeners.isEmpty()) {
                notifyContactListFilterChanged();
            }
        }
    }

    @Override
    public void selectCustomFilter() {
        setContactListFilter(ContactListFilter.createFilterWithType(-3), true);
    }

    private void notifyContactListFilterChanged() {
        for (ContactListFilterController.ContactListFilterListener listener : this.mListeners) {
            listener.onContactListFilterChanged();
        }
    }

    @Override
    public void checkFilterValidity(boolean notifyListeners) {
        if (this.mFilter != null) {
            switch (this.mFilter.filterType) {
                case -6:
                    setContactListFilter(ContactListFilter.restoreDefaultPreferences(getSharedPreferences()), false, notifyListeners);
                    break;
                case 0:
                    if (!filterAccountExists()) {
                        setContactListFilter(ContactListFilter.createFilterWithType(-2), true, notifyListeners);
                    }
                    break;
            }
        }
    }

    private boolean filterAccountExists() {
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(this.mContext);
        AccountWithDataSet filterAccount = new AccountWithDataSet(this.mFilter.accountName, this.mFilter.accountType, this.mFilter.dataSet);
        return accountTypeManager.contains(filterAccount, false);
    }
}
