package com.android.contacts.common.model.account;

import android.accounts.Account;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AccountWithDataSet extends Account {
    public final String dataSet;
    private final AccountTypeWithDataSet mAccountTypeWithDataSet;
    private static final Pattern STRINGIFY_SEPARATOR_PAT = Pattern.compile(Pattern.quote("\u0001"));
    private static final Pattern ARRAY_STRINGIFY_SEPARATOR_PAT = Pattern.compile(Pattern.quote("\u0002"));
    private static final String[] ID_PROJECTION = {"_id"};
    private static final Uri RAW_CONTACTS_URI_LIMIT_1 = ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter("limit", "1").build();
    public static final Parcelable.Creator<AccountWithDataSet> CREATOR = new Parcelable.Creator<AccountWithDataSet>() {
        @Override
        public AccountWithDataSet createFromParcel(Parcel source) {
            return new AccountWithDataSet(source);
        }

        @Override
        public AccountWithDataSet[] newArray(int size) {
            return new AccountWithDataSet[size];
        }
    };

    public AccountWithDataSet(String name, String type, String dataSet) {
        super(name, type);
        this.dataSet = dataSet;
        this.mAccountTypeWithDataSet = AccountTypeWithDataSet.get(type, dataSet);
    }

    public AccountWithDataSet(Parcel in) {
        super(in);
        this.dataSet = in.readString();
        this.mAccountTypeWithDataSet = AccountTypeWithDataSet.get(this.type, this.dataSet);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.dataSet);
    }

    public AccountTypeWithDataSet getAccountTypeWithDataSet() {
        return this.mAccountTypeWithDataSet;
    }

    public boolean hasData(Context context) {
        String selection;
        String[] args;
        if (TextUtils.isEmpty(this.dataSet)) {
            selection = "account_type = ? AND account_name = ? AND data_set IS NULL";
            args = new String[]{this.type, this.name};
        } else {
            selection = "account_type = ? AND account_name = ? AND data_set = ?";
            args = new String[]{this.type, this.name, this.dataSet};
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

    @Override
    public boolean equals(Object o) {
        return (o instanceof AccountWithDataSet) && super.equals(o) && Objects.equal(((AccountWithDataSet) o).dataSet, this.dataSet);
    }

    @Override
    public int hashCode() {
        return (this.dataSet == null ? 0 : this.dataSet.hashCode()) + (super.hashCode() * 31);
    }

    @Override
    public String toString() {
        return "AccountWithDataSet {name=" + this.name + ", type=" + this.type + ", dataSet=" + this.dataSet + "}";
    }

    private static StringBuilder addStringified(StringBuilder sb, AccountWithDataSet account) {
        sb.append(account.name);
        sb.append("\u0001");
        sb.append(account.type);
        sb.append("\u0001");
        if (!TextUtils.isEmpty(account.dataSet)) {
            sb.append(account.dataSet);
        }
        return sb;
    }

    public String stringify() {
        return addStringified(new StringBuilder(), this).toString();
    }

    public static AccountWithDataSet unstringify(String s) {
        String[] array = STRINGIFY_SEPARATOR_PAT.split(s, 3);
        if (array.length < 3) {
            throw new IllegalArgumentException("Invalid string " + s);
        }
        return new AccountWithDataSet(array[0], array[1], TextUtils.isEmpty(array[2]) ? null : array[2]);
    }

    public static String stringifyList(List<AccountWithDataSet> accounts) {
        StringBuilder sb = new StringBuilder();
        for (AccountWithDataSet account : accounts) {
            if (sb.length() > 0) {
                sb.append("\u0002");
            }
            addStringified(sb, account);
        }
        return sb.toString();
    }

    public static List<AccountWithDataSet> unstringifyList(String s) {
        ArrayList<AccountWithDataSet> ret = Lists.newArrayList();
        if (!TextUtils.isEmpty(s)) {
            String[] array = ARRAY_STRINGIFY_SEPARATOR_PAT.split(s);
            for (String str : array) {
                ret.add(unstringify(str));
            }
        }
        return ret;
    }
}
