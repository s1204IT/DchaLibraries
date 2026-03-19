package com.android.contacts.model.account;

import android.content.Context;

public class DeviceLocalAccountType extends FallbackAccountType {
    private final boolean mGroupsEditable;

    public DeviceLocalAccountType(Context context, boolean z) {
        super(context);
        this.mGroupsEditable = z;
    }

    public DeviceLocalAccountType(Context context) {
        this(context, false);
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return this.mGroupsEditable;
    }

    @Override
    public AccountInfo wrapAccount(Context context, AccountWithDataSet accountWithDataSet) {
        return new AccountInfo(new AccountDisplayInfo(accountWithDataSet, getDisplayLabel(context), getDisplayLabel(context), getDisplayIcon(context), true), this);
    }
}
