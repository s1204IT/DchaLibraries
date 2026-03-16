package com.android.contacts.common.model.account;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.google.common.base.Objects;

public class AccountTypeWithDataSet {
    private static final String[] ID_PROJECTION = {"_id"};
    private static final Uri RAW_CONTACTS_URI_LIMIT_1 = ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter("limit", "1").build();
    public final String accountType;
    public final String dataSet;

    private AccountTypeWithDataSet(String accountType, String dataSet) {
        this.accountType = TextUtils.isEmpty(accountType) ? null : accountType;
        this.dataSet = TextUtils.isEmpty(dataSet) ? null : dataSet;
    }

    public static AccountTypeWithDataSet get(String accountType, String dataSet) {
        return new AccountTypeWithDataSet(accountType, dataSet);
    }

    public boolean hasData(Context context) {
        String selection;
        String[] args;
        if (TextUtils.isEmpty(this.dataSet)) {
            selection = "account_type = ? AND data_set IS NULL";
            args = new String[]{this.accountType};
        } else {
            selection = "account_type = ? AND data_set = ?";
            args = new String[]{this.accountType, this.dataSet};
        }
        Cursor c = context.getContentResolver().query(RAW_CONTACTS_URI_LIMIT_1, ID_PROJECTION, selection, args, null);
        if (c == null) {
            return false;
        }
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof AccountTypeWithDataSet)) {
            return false;
        }
        AccountTypeWithDataSet other = (AccountTypeWithDataSet) o;
        return Objects.equal(this.accountType, other.accountType) && Objects.equal(this.dataSet, other.dataSet);
    }

    public int hashCode() {
        return (this.accountType == null ? 0 : this.accountType.hashCode()) ^ (this.dataSet != null ? this.dataSet.hashCode() : 0);
    }

    public String toString() {
        return "[" + this.accountType + "/" + this.dataSet + "]";
    }
}
