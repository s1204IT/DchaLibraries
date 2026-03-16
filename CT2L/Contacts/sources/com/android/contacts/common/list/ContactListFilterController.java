package com.android.contacts.common.list;

import android.content.Context;

public abstract class ContactListFilterController {
    private static ContactListFilterControllerImpl sFilterController = null;

    public interface ContactListFilterListener {
        void onContactListFilterChanged();
    }

    public abstract void addListener(ContactListFilterListener contactListFilterListener);

    public abstract void checkFilterValidity(boolean z);

    public abstract ContactListFilter getFilter();

    public abstract void removeListener(ContactListFilterListener contactListFilterListener);

    public abstract void selectCustomFilter();

    public abstract void setContactListFilter(ContactListFilter contactListFilter, boolean z);

    public static ContactListFilterController getInstance(Context context) {
        if (sFilterController == null) {
            sFilterController = new ContactListFilterControllerImpl(context);
        }
        return sFilterController;
    }
}
