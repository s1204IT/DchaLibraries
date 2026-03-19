package com.android.contacts.model.account;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AccountWithDataSet implements Parcelable {
    public static final String LOCAL_ACCOUNT_SELECTION = "account_type IS NULL AND account_name IS NULL AND data_set IS NULL";
    public final String dataSet;
    private final AccountTypeWithDataSet mAccountTypeWithDataSet;
    public final String name;
    public final String type;
    private static final String STRINGIFY_SEPARATOR = "\u0001";
    private static final Pattern STRINGIFY_SEPARATOR_PAT = Pattern.compile(Pattern.quote(STRINGIFY_SEPARATOR));
    private static final String ARRAY_STRINGIFY_SEPARATOR = "\u0002";
    private static final Pattern ARRAY_STRINGIFY_SEPARATOR_PAT = Pattern.compile(Pattern.quote(ARRAY_STRINGIFY_SEPARATOR));
    private static final String[] ID_PROJECTION = {"_id"};
    private static final Uri RAW_CONTACTS_URI_LIMIT_1 = ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter("limit", "1").build();
    public static final Parcelable.Creator<AccountWithDataSet> CREATOR = new Parcelable.Creator<AccountWithDataSet>() {
        @Override
        public AccountWithDataSet createFromParcel(Parcel parcel) {
            return new AccountWithDataSet(parcel);
        }

        @Override
        public AccountWithDataSet[] newArray(int i) {
            return new AccountWithDataSet[i];
        }
    };

    public AccountWithDataSet(String str, String str2, String str3) {
        this.name = emptyToNull(str);
        this.type = emptyToNull(str2);
        this.dataSet = emptyToNull(str3);
        this.mAccountTypeWithDataSet = AccountTypeWithDataSet.get(str2, str3);
    }

    private static final String emptyToNull(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return str;
    }

    public AccountWithDataSet(Parcel parcel) {
        this.name = parcel.readString();
        this.type = parcel.readString();
        this.dataSet = parcel.readString();
        this.mAccountTypeWithDataSet = AccountTypeWithDataSet.get(this.type, this.dataSet);
    }

    public boolean isNullAccount() {
        return this.name == null && this.type == null && this.dataSet == null;
    }

    public static AccountWithDataSet getNullAccount() {
        return new AccountWithDataSet(null, null, null);
    }

    public Account getAccountOrNull() {
        if (this.name != null && this.type != null) {
            return new Account(this.name, this.type);
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.name);
        parcel.writeString(this.type);
        parcel.writeString(this.dataSet);
    }

    public AccountTypeWithDataSet getAccountTypeWithDataSet() {
        return this.mAccountTypeWithDataSet;
    }

    public boolean hasData(Context context) {
        String str;
        String[] strArr;
        if (isNullAccount()) {
            str = LOCAL_ACCOUNT_SELECTION;
            strArr = null;
        } else if (TextUtils.isEmpty(this.dataSet)) {
            str = "account_type = ? AND account_name = ? AND data_set IS NULL";
            strArr = new String[]{this.type, this.name};
        } else {
            str = "account_type = ? AND account_name = ? AND data_set = ?";
            strArr = new String[]{this.type, this.name, this.dataSet};
        }
        Cursor cursorQuery = context.getContentResolver().query(RAW_CONTACTS_URI_LIMIT_1, ID_PROJECTION, str + " AND deleted=0", strArr, null);
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
        if (!(obj instanceof AccountWithDataSet)) {
            return false;
        }
        AccountWithDataSet accountWithDataSet = (AccountWithDataSet) obj;
        return Objects.equal(this.name, accountWithDataSet.name) && Objects.equal(this.type, accountWithDataSet.type) && Objects.equal(this.dataSet, accountWithDataSet.dataSet);
    }

    public int hashCode() {
        return (31 * (((527 + (this.name != null ? this.name.hashCode() : 0)) * 31) + (this.type != null ? this.type.hashCode() : 0))) + (this.dataSet != null ? this.dataSet.hashCode() : 0);
    }

    public String toString() {
        return "AccountWithDataSet {name=" + Log.anonymize(this.name) + ", type=" + this.type + ", dataSet=" + this.dataSet + "}";
    }

    private static StringBuilder addStringified(StringBuilder sb, AccountWithDataSet accountWithDataSet) {
        if (!TextUtils.isEmpty(accountWithDataSet.name)) {
            sb.append(accountWithDataSet.name);
        }
        sb.append(STRINGIFY_SEPARATOR);
        if (!TextUtils.isEmpty(accountWithDataSet.type)) {
            sb.append(accountWithDataSet.type);
        }
        sb.append(STRINGIFY_SEPARATOR);
        if (!TextUtils.isEmpty(accountWithDataSet.dataSet)) {
            sb.append(accountWithDataSet.dataSet);
        }
        return sb;
    }

    public String stringify() {
        return addStringified(new StringBuilder(), this).toString();
    }

    public ContentProviderOperation newRawContactOperation() {
        ContentProviderOperation.Builder builderWithValue = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue("account_name", this.name).withValue("account_type", this.type);
        if (this.dataSet != null) {
            builderWithValue.withValue("data_set", this.dataSet);
        }
        return builderWithValue.build();
    }

    public static AccountWithDataSet unstringify(String str) {
        String[] strArrSplit = STRINGIFY_SEPARATOR_PAT.split(str, 3);
        if (strArrSplit.length < 3) {
            throw new IllegalArgumentException("Invalid string " + str);
        }
        return new AccountWithDataSet(strArrSplit[0], strArrSplit[1], TextUtils.isEmpty(strArrSplit[2]) ? null : strArrSplit[2]);
    }

    public static String stringifyList(List<AccountWithDataSet> list) {
        StringBuilder sb = new StringBuilder();
        for (AccountWithDataSet accountWithDataSet : list) {
            if (sb.length() > 0) {
                sb.append(ARRAY_STRINGIFY_SEPARATOR);
            }
            addStringified(sb, accountWithDataSet);
        }
        return sb.toString();
    }

    public static List<AccountWithDataSet> unstringifyList(String str) {
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        if (TextUtils.isEmpty(str)) {
            return arrayListNewArrayList;
        }
        for (String str2 : ARRAY_STRINGIFY_SEPARATOR_PAT.split(str)) {
            arrayListNewArrayList.add(unstringify(str2));
        }
        return arrayListNewArrayList;
    }
}
