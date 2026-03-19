package com.android.contacts.list;

import android.accounts.Account;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.android.contacts.ContactSaveService;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.GoogleAccountType;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class ContactListFilter implements Parcelable, Comparable<ContactListFilter> {
    public static final Parcelable.Creator<ContactListFilter> CREATOR = new Parcelable.Creator<ContactListFilter>() {
        @Override
        public ContactListFilter createFromParcel(Parcel parcel) {
            return new ContactListFilter(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString(), null);
        }

        @Override
        public ContactListFilter[] newArray(int i) {
            return new ContactListFilter[i];
        }
    };
    public final String accountName;
    public final String accountType;
    public final String dataSet;
    public final int filterType;
    public final Drawable icon;
    private String mId;

    public ContactListFilter(int i, String str, String str2, String str3, Drawable drawable) {
        this.filterType = i;
        this.accountType = str;
        this.accountName = str2;
        this.dataSet = str3;
        this.icon = drawable;
    }

    public static ContactListFilter createFilterWithType(int i) {
        return new ContactListFilter(i, null, null, null, null);
    }

    public static ContactListFilter createAccountFilter(String str, String str2, String str3, Drawable drawable) {
        return new ContactListFilter(0, str, str2, str3, drawable);
    }

    public static ContactListFilter createGroupMembersFilter(String str, String str2, String str3) {
        return new ContactListFilter(-7, str, str2, str3, null);
    }

    public static ContactListFilter createDeviceContactsFilter(Drawable drawable, AccountWithDataSet accountWithDataSet) {
        return new ContactListFilter(-8, accountWithDataSet.type, accountWithDataSet.name, accountWithDataSet.dataSet, drawable);
    }

    public boolean isContactsFilterType() {
        return this.filterType == -1 || this.filterType == -2 || this.filterType == -3;
    }

    public int toListType() {
        switch (this.filterType) {
            case -8:
                return 5;
            case -7:
                return 3;
            case -6:
                return 9;
            case -5:
                return 8;
            case -4:
                return 7;
            case -3:
                return 6;
            case -2:
            case -1:
                return 1;
            case 0:
                return 2;
            default:
                return 0;
        }
    }

    public String toString() {
        String str;
        switch (this.filterType) {
            case -8:
                return "device_contacts";
            case -7:
                return "group_members";
            case -6:
                return "single";
            case -5:
                return "with_phones";
            case -4:
                return ContactSaveService.EXTRA_STARRED_FLAG;
            case -3:
                return "custom";
            case -2:
                return "all_accounts";
            case -1:
                return "default";
            case 0:
                StringBuilder sb = new StringBuilder();
                sb.append("account: ");
                sb.append(this.accountType);
                if (this.dataSet != null) {
                    str = "/" + this.dataSet;
                } else {
                    str = "";
                }
                sb.append(str);
                sb.append(" ");
                sb.append(Log.anonymize(this.accountName));
                return sb.toString();
            default:
                return super.toString();
        }
    }

    @Override
    public int compareTo(ContactListFilter contactListFilter) {
        int iCompareTo = this.accountName.compareTo(contactListFilter.accountName);
        if (iCompareTo != 0) {
            return iCompareTo;
        }
        int iCompareTo2 = this.accountType.compareTo(contactListFilter.accountType);
        if (iCompareTo2 != 0) {
            return iCompareTo2;
        }
        return this.filterType - contactListFilter.filterType;
    }

    public int hashCode() {
        int iHashCode = this.filterType;
        if (this.accountType != null) {
            iHashCode = (iHashCode * 31) + this.accountType.hashCode();
        }
        if (this.accountName != null) {
            iHashCode = (iHashCode * 31) + this.accountName.hashCode();
        }
        if (this.dataSet != null) {
            return (iHashCode * 31) + this.dataSet.hashCode();
        }
        return iHashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ContactListFilter)) {
            return false;
        }
        ContactListFilter contactListFilter = (ContactListFilter) obj;
        return this.filterType == contactListFilter.filterType && TextUtils.equals(this.accountName, contactListFilter.accountName) && TextUtils.equals(this.accountType, contactListFilter.accountType) && TextUtils.equals(this.dataSet, contactListFilter.dataSet);
    }

    public static void storeToPreferences(SharedPreferences sharedPreferences, ContactListFilter contactListFilter) {
        if (contactListFilter != null && contactListFilter.filterType == -6) {
            return;
        }
        sharedPreferences.edit().putInt("filter.type", contactListFilter == null ? -1 : contactListFilter.filterType).putString("filter.accountName", contactListFilter == null ? null : contactListFilter.accountName).putString("filter.accountType", contactListFilter == null ? null : contactListFilter.accountType).putString("filter.dataSet", contactListFilter != null ? contactListFilter.dataSet : null).apply();
    }

    public static ContactListFilter restoreDefaultPreferences(SharedPreferences sharedPreferences) {
        ContactListFilter contactListFilterRestoreFromPreferences = restoreFromPreferences(sharedPreferences);
        if (contactListFilterRestoreFromPreferences == null) {
            contactListFilterRestoreFromPreferences = createFilterWithType(-2);
        }
        if (contactListFilterRestoreFromPreferences.filterType == 1 || contactListFilterRestoreFromPreferences.filterType == -6) {
            return createFilterWithType(-2);
        }
        return contactListFilterRestoreFromPreferences;
    }

    private static ContactListFilter restoreFromPreferences(SharedPreferences sharedPreferences) {
        int i = sharedPreferences.getInt("filter.type", -1);
        if (i == -1) {
            return null;
        }
        return new ContactListFilter(i, sharedPreferences.getString("filter.accountType", null), sharedPreferences.getString("filter.accountName", null), sharedPreferences.getString("filter.dataSet", null), null);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.filterType);
        parcel.writeString(this.accountName);
        parcel.writeString(this.accountType);
        parcel.writeString(this.dataSet);
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
                sb.append('-');
                sb.append(this.accountType);
            }
            if (this.dataSet != null) {
                sb.append('/');
                sb.append(this.dataSet);
            }
            if (this.accountName != null) {
                sb.append('-');
                sb.append(this.accountName.replace('-', '_'));
            }
            this.mId = sb.toString();
        }
        return this.mId;
    }

    public Uri.Builder addAccountQueryParameterToUrl(Uri.Builder builder) {
        if (this.filterType != 0 && this.filterType != -7) {
            throw new IllegalStateException("filterType must be FILTER_TYPE_ACCOUNT or FILER_TYPE_GROUP_MEMBERS");
        }
        if (this.accountName != null) {
            builder.appendQueryParameter("account_name", this.accountName);
            builder.appendQueryParameter("account_type", this.accountType);
        }
        if (this.dataSet != null) {
            builder.appendQueryParameter("data_set", this.dataSet);
        }
        return builder;
    }

    public AccountWithDataSet toAccountWithDataSet() {
        if (this.filterType == 0 || this.filterType == -8) {
            return new AccountWithDataSet(this.accountName, this.accountType, this.dataSet);
        }
        throw new IllegalStateException("Cannot create Account from filter type " + filterTypeToString(this.filterType));
    }

    public static final String filterTypeToString(int i) {
        switch (i) {
            case -8:
                return "FILTER_TYPE_DEVICE_CONTACTS";
            case -7:
                return "FILTER_TYPE_GROUP_MEMBERS";
            case -6:
                return "FILTER_TYPE_SINGLE_CONTACT";
            case -5:
                return "FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY";
            case -4:
                return "FILTER_TYPE_STARRED";
            case -3:
                return "FILTER_TYPE_CUSTOM";
            case -2:
                return "FILTER_TYPE_ALL_ACCOUNTS";
            case -1:
                return "FILTER_TYPE_DEFAULT";
            case 0:
                return "FILTER_TYPE_ACCOUNT";
            default:
                return "(unknown)";
        }
    }

    public boolean isSyncable() {
        return isGoogleAccountType() && this.filterType == 0;
    }

    public boolean shouldShowSyncState() {
        return (isGoogleAccountType() && this.filterType == 0) || this.filterType == -2 || this.filterType == -3 || this.filterType == -1;
    }

    public List<Account> getSyncableAccounts(List<AccountWithDataSet> list) {
        ArrayList arrayList = new ArrayList();
        if (isGoogleAccountType() && this.filterType == 0) {
            arrayList.add(new Account(this.accountName, this.accountType));
        } else if ((this.filterType == -2 || this.filterType == -3 || this.filterType == -1) && list != null && list.size() > 0) {
            for (AccountWithDataSet accountWithDataSet : list) {
                if (GoogleAccountType.ACCOUNT_TYPE.equals(accountWithDataSet.type) && accountWithDataSet.dataSet == null) {
                    arrayList.add(new Account(accountWithDataSet.name, accountWithDataSet.type));
                }
            }
        }
        return arrayList;
    }

    public boolean isGoogleAccountType() {
        return GoogleAccountType.ACCOUNT_TYPE.equals(this.accountType) && this.dataSet == null;
    }
}
