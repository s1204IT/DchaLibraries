package com.android.contacts.list;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ContactListFilterControllerImpl extends ContactListFilterController {
    private final Context mContext;
    private final List<ContactListFilterController.ContactListFilterListener> mListeners = new ArrayList();
    private ContactListFilter mFilter = ContactListFilter.restoreDefaultPreferences(getSharedPreferences());

    public ContactListFilterControllerImpl(Context context) {
        this.mContext = context.getApplicationContext();
        checkFilterValidity(true);
    }

    @Override
    public void addListener(ContactListFilterController.ContactListFilterListener contactListFilterListener) {
        this.mListeners.add(contactListFilterListener);
    }

    @Override
    public void removeListener(ContactListFilterController.ContactListFilterListener contactListFilterListener) {
        this.mListeners.remove(contactListFilterListener);
    }

    @Override
    public ContactListFilter getFilter() {
        return this.mFilter;
    }

    @Override
    public int getFilterListType() {
        if (this.mFilter == null) {
            return 0;
        }
        return this.mFilter.toListType();
    }

    @Override
    public boolean isCustomFilterPersisted() {
        ContactListFilter persistedFilter = getPersistedFilter();
        return persistedFilter != null && persistedFilter.filterType == -3;
    }

    @Override
    public ContactListFilter getPersistedFilter() {
        return ContactListFilter.restoreDefaultPreferences(getSharedPreferences());
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this.mContext);
    }

    @Override
    public void setContactListFilter(ContactListFilter contactListFilter, boolean z) {
        setContactListFilter(contactListFilter, z, true);
    }

    private void setContactListFilter(ContactListFilter contactListFilter, boolean z, boolean z2) {
        if (!contactListFilter.equals(this.mFilter)) {
            this.mFilter = contactListFilter;
            if (z) {
                ContactListFilter.storeToPreferences(getSharedPreferences(), this.mFilter);
            }
            if (z2 && !this.mListeners.isEmpty()) {
                notifyContactListFilterChanged();
            }
        }
    }

    @Override
    public void selectCustomFilter() {
        setContactListFilter(ContactListFilter.createFilterWithType(-3), true);
    }

    private void notifyContactListFilterChanged() {
        Iterator<ContactListFilterController.ContactListFilterListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onContactListFilterChanged();
        }
    }

    @Override
    public void checkFilterValidity(boolean z) {
        if (this.mFilter == null) {
            return;
        }
        int i = this.mFilter.filterType;
        if (i == -6) {
            setContactListFilter(ContactListFilter.restoreDefaultPreferences(getSharedPreferences()), false, z);
        } else if (i == 0 && !filterAccountExists()) {
            setContactListFilter(ContactListFilter.createFilterWithType(-2), true, z);
        }
    }

    private boolean filterAccountExists() {
        return AccountTypeManager.getInstance(this.mContext).exists(new AccountWithDataSet(this.mFilter.accountName, this.mFilter.accountType, this.mFilter.dataSet));
    }
}
