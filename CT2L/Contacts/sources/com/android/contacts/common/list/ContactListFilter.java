package com.android.contacts.common.list;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class ContactListFilter implements Parcelable, Comparable<ContactListFilter> {
    public static final Parcelable.Creator<ContactListFilter> CREATOR = new Parcelable.Creator<ContactListFilter>() {
        @Override
        public ContactListFilter createFromParcel(Parcel source) {
            int filterType = source.readInt();
            String accountName = source.readString();
            String accountType = source.readString();
            String dataSet = source.readString();
            return new ContactListFilter(filterType, accountType, accountName, dataSet, null);
        }

        @Override
        public ContactListFilter[] newArray(int size) {
            return new ContactListFilter[size];
        }
    };
    public final String accountName;
    public final String accountType;
    public final String dataSet;
    public final int filterType;
    public final Drawable icon;
    private String mId;

    public ContactListFilter(int filterType, String accountType, String accountName, String dataSet, Drawable icon) {
        this.filterType = filterType;
        this.accountType = accountType;
        this.accountName = accountName;
        this.dataSet = dataSet;
        this.icon = icon;
    }

    public static ContactListFilter createFilterWithType(int filterType) {
        return new ContactListFilter(filterType, null, null, null, null);
    }

    public static ContactListFilter createAccountFilter(String accountType, String accountName, String dataSet, Drawable icon) {
        return new ContactListFilter(0, accountType, accountName, dataSet, icon);
    }

    public String toString() {
        switch (this.filterType) {
            case -6:
                return "single";
            case -5:
                return "with_phones";
            case -4:
                return "starred";
            case -3:
                return "custom";
            case -2:
                return "all_accounts";
            case -1:
                return "default";
            case 0:
                return "account: " + this.accountType + (this.dataSet != null ? "/" + this.dataSet : "") + " " + this.accountName;
            default:
                return super.toString();
        }
    }

    @Override
    public int compareTo(ContactListFilter another) {
        int res = this.accountName.compareTo(another.accountName);
        if (res != 0) {
            return res;
        }
        int res2 = this.accountType.compareTo(another.accountType);
        return res2 != 0 ? res2 : this.filterType - another.filterType;
    }

    public int hashCode() {
        int code = this.filterType;
        if (this.accountType != null) {
            code = (((code * 31) + this.accountType.hashCode()) * 31) + this.accountName.hashCode();
        }
        if (this.dataSet != null) {
            return (code * 31) + this.dataSet.hashCode();
        }
        return code;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContactListFilter)) {
            return false;
        }
        ContactListFilter otherFilter = (ContactListFilter) other;
        return this.filterType == otherFilter.filterType && TextUtils.equals(this.accountName, otherFilter.accountName) && TextUtils.equals(this.accountType, otherFilter.accountType) && TextUtils.equals(this.dataSet, otherFilter.dataSet);
    }

    public static void storeToPreferences(SharedPreferences prefs, ContactListFilter filter) {
        if (filter == null || filter.filterType != -6) {
            prefs.edit().putInt("filter.type", filter == null ? -1 : filter.filterType).putString("filter.accountName", filter == null ? null : filter.accountName).putString("filter.accountType", filter == null ? null : filter.accountType).putString("filter.dataSet", filter != null ? filter.dataSet : null).apply();
        }
    }

    public static ContactListFilter restoreDefaultPreferences(SharedPreferences prefs) {
        ContactListFilter filter = restoreFromPreferences(prefs);
        if (filter == null) {
            filter = createFilterWithType(-2);
        }
        if (filter.filterType == 1 || filter.filterType == -6) {
            return createFilterWithType(-2);
        }
        return filter;
    }

    private static ContactListFilter restoreFromPreferences(SharedPreferences prefs) {
        int filterType = prefs.getInt("filter.type", -1);
        if (filterType == -1) {
            return null;
        }
        String accountName = prefs.getString("filter.accountName", null);
        String accountType = prefs.getString("filter.accountType", null);
        String dataSet = prefs.getString("filter.dataSet", null);
        return new ContactListFilter(filterType, accountType, accountName, dataSet, null);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.filterType);
        dest.writeString(this.accountName);
        dest.writeString(this.accountType);
        dest.writeString(this.dataSet);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getId() {
        if (this.mId == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.filterType);
            if (this.accountType != null) {
                sb.append('-').append(this.accountType);
            }
            if (this.dataSet != null) {
                sb.append('/').append(this.dataSet);
            }
            if (this.accountName != null) {
                sb.append('-').append(this.accountName.replace('-', '_'));
            }
            this.mId = sb.toString();
        }
        return this.mId;
    }

    public Uri.Builder addAccountQueryParameterToUrl(Uri.Builder uriBuilder) {
        if (this.filterType != 0) {
            throw new IllegalStateException("filterType must be FILTER_TYPE_ACCOUNT");
        }
        uriBuilder.appendQueryParameter("account_name", this.accountName);
        uriBuilder.appendQueryParameter("account_type", this.accountType);
        if (!TextUtils.isEmpty(this.dataSet)) {
            uriBuilder.appendQueryParameter("data_set", this.dataSet);
        }
        return uriBuilder;
    }
}
