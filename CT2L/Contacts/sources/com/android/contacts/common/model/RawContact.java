package com.android.contacts.common.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.dataitem.DataItem;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;

public final class RawContact implements Parcelable {
    public static final Parcelable.Creator<RawContact> CREATOR = new Parcelable.Creator<RawContact>() {
        @Override
        public RawContact createFromParcel(Parcel parcel) {
            return new RawContact(parcel);
        }

        @Override
        public RawContact[] newArray(int i) {
            return new RawContact[i];
        }
    };
    private AccountTypeManager mAccountTypeManager;
    private final ArrayList<NamedDataItem> mDataItems;
    private final ContentValues mValues;

    public static final class NamedDataItem implements Parcelable {
        public static final Parcelable.Creator<NamedDataItem> CREATOR = new Parcelable.Creator<NamedDataItem>() {
            @Override
            public NamedDataItem createFromParcel(Parcel parcel) {
                return new NamedDataItem(parcel);
            }

            @Override
            public NamedDataItem[] newArray(int i) {
                return new NamedDataItem[i];
            }
        };
        public final ContentValues mContentValues;
        public final Uri mUri;

        public NamedDataItem(Uri uri, ContentValues values) {
            this.mUri = uri;
            this.mContentValues = values;
        }

        public NamedDataItem(Parcel parcel) {
            this.mUri = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
            this.mContentValues = (ContentValues) parcel.readParcelable(ContentValues.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(this.mUri, i);
            parcel.writeParcelable(this.mContentValues, i);
        }

        public int hashCode() {
            return Objects.hashCode(this.mUri, this.mContentValues);
        }

        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            NamedDataItem other = (NamedDataItem) obj;
            return Objects.equal(this.mUri, other.mUri) && Objects.equal(this.mContentValues, other.mContentValues);
        }
    }

    public static RawContact createFrom(Entity entity) {
        ContentValues values = entity.getEntityValues();
        ArrayList<Entity.NamedContentValues> subValues = entity.getSubValues();
        RawContact rawContact = new RawContact(values);
        for (Entity.NamedContentValues subValue : subValues) {
            rawContact.addNamedDataItemValues(subValue.uri, subValue.values);
        }
        return rawContact;
    }

    public RawContact() {
        this(new ContentValues());
    }

    public RawContact(ContentValues values) {
        this.mValues = values;
        this.mDataItems = new ArrayList<>();
    }

    private RawContact(Parcel parcel) {
        this.mValues = (ContentValues) parcel.readParcelable(ContentValues.class.getClassLoader());
        this.mDataItems = Lists.newArrayList();
        parcel.readTypedList(this.mDataItems, NamedDataItem.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mValues, i);
        parcel.writeTypedList(this.mDataItems);
    }

    public AccountTypeManager getAccountTypeManager(Context context) {
        if (this.mAccountTypeManager == null) {
            this.mAccountTypeManager = AccountTypeManager.getInstance(context);
        }
        return this.mAccountTypeManager;
    }

    public ContentValues getValues() {
        return this.mValues;
    }

    public Long getId() {
        return getValues().getAsLong("_id");
    }

    public String getAccountName() {
        return getValues().getAsString("account_name");
    }

    public String getAccountTypeString() {
        return getValues().getAsString("account_type");
    }

    public String getDataSet() {
        return getValues().getAsString("data_set");
    }

    public AccountType getAccountType(Context context) {
        return getAccountTypeManager(context).getAccountType(getAccountTypeString(), getDataSet());
    }

    private void setAccount(String accountName, String accountType, String dataSet) {
        ContentValues values = getValues();
        if (accountName == null) {
            if (accountType == null && dataSet == null) {
                values.putNull("account_name");
                values.putNull("account_type");
                values.putNull("data_set");
                return;
            }
        } else if (accountType != null) {
            values.put("account_name", accountName);
            values.put("account_type", accountType);
            if (dataSet == null) {
                values.putNull("data_set");
                return;
            } else {
                values.put("data_set", dataSet);
                return;
            }
        }
        throw new IllegalArgumentException("Not a valid combination of account name, type, and data set.");
    }

    public void setAccount(AccountWithDataSet accountWithDataSet) {
        setAccount(accountWithDataSet.name, accountWithDataSet.type, accountWithDataSet.dataSet);
    }

    public void addDataItemValues(ContentValues values) {
        addNamedDataItemValues(ContactsContract.Data.CONTENT_URI, values);
    }

    public NamedDataItem addNamedDataItemValues(Uri uri, ContentValues values) {
        NamedDataItem namedItem = new NamedDataItem(uri, values);
        this.mDataItems.add(namedItem);
        return namedItem;
    }

    public ArrayList<ContentValues> getContentValues() {
        ArrayList<ContentValues> list = Lists.newArrayListWithCapacity(this.mDataItems.size());
        for (NamedDataItem dataItem : this.mDataItems) {
            if (ContactsContract.Data.CONTENT_URI.equals(dataItem.mUri)) {
                list.add(dataItem.mContentValues);
            }
        }
        return list;
    }

    public List<DataItem> getDataItems() {
        ArrayList<DataItem> list = Lists.newArrayListWithCapacity(this.mDataItems.size());
        for (NamedDataItem dataItem : this.mDataItems) {
            if (ContactsContract.Data.CONTENT_URI.equals(dataItem.mUri)) {
                list.add(DataItem.createFrom(dataItem.mContentValues));
            }
        }
        return list;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RawContact: ").append(this.mValues);
        for (NamedDataItem namedDataItem : this.mDataItems) {
            sb.append("\n  ").append(namedDataItem.mUri);
            sb.append("\n  -> ").append(namedDataItem.mContentValues);
        }
        return sb.toString();
    }

    public int hashCode() {
        return Objects.hashCode(this.mValues, this.mDataItems);
    }

    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RawContact other = (RawContact) obj;
        return Objects.equal(this.mValues, other.mValues) && Objects.equal(this.mDataItems, other.mDataItems);
    }
}
