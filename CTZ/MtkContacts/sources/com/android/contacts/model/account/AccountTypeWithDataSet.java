package com.android.contacts.model.account;

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

    private AccountTypeWithDataSet(String str, String str2) {
        this.accountType = TextUtils.isEmpty(str) ? null : str;
        this.dataSet = TextUtils.isEmpty(str2) ? null : str2;
    }

    public static AccountTypeWithDataSet get(String str, String str2) {
        return new AccountTypeWithDataSet(str, str2);
    }

    public boolean hasData(Context context) {
        String str;
        String[] strArr;
        if (TextUtils.isEmpty(this.dataSet)) {
            str = "account_type = ? AND data_set IS NULL";
            strArr = new String[]{this.accountType};
        } else {
            str = "account_type = ? AND data_set = ?";
            strArr = new String[]{this.accountType, this.dataSet};
        }
        Cursor cursorQuery = context.getContentResolver().query(RAW_CONTACTS_URI_LIMIT_1, ID_PROJECTION, str, strArr, null);
        if (cursorQuery == null) {
            return false;
        }
        try {
            return cursorQuery.moveToFirst();
        } finally {
            cursorQuery.close();
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AccountTypeWithDataSet)) {
            return false;
        }
        AccountTypeWithDataSet accountTypeWithDataSet = (AccountTypeWithDataSet) obj;
        return Objects.equal(this.accountType, accountTypeWithDataSet.accountType) && Objects.equal(this.dataSet, accountTypeWithDataSet.dataSet);
    }

    public int hashCode() {
        int iHashCode;
        if (this.accountType != null) {
            iHashCode = this.accountType.hashCode();
        } else {
            iHashCode = 0;
        }
        return iHashCode ^ (this.dataSet != null ? this.dataSet.hashCode() : 0);
    }

    public String toString() {
        return "[" + this.accountType + "/" + this.dataSet + "]";
    }
}
