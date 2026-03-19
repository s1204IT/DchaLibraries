package com.android.contacts.group;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.ContactSaveService;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.google.common.base.MoreObjects;
import com.mediatek.contacts.util.Log;

public final class GroupMetaData implements Parcelable {
    public static final Parcelable.Creator<GroupMetaData> CREATOR = new Parcelable.Creator<GroupMetaData>() {
        @Override
        public GroupMetaData createFromParcel(Parcel parcel) {
            return new GroupMetaData(parcel);
        }

        @Override
        public GroupMetaData[] newArray(int i) {
            return new GroupMetaData[i];
        }
    };
    public final String accountName;
    public final String accountType;
    public final String dataSet;
    public final boolean defaultGroup;
    public final boolean editable;
    public final boolean favorites;
    public final long groupId;
    public final String groupName;
    public final boolean readOnly;
    public final Uri uri;

    public GroupMetaData(Context context, Cursor cursor) {
        boolean zIsGroupMembershipEditable;
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(context);
        long j = cursor.getLong(3);
        Uri uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, j);
        AccountType accountType = accountTypeManager.getAccountType(cursor.getString(1), cursor.getString(2));
        if (accountType != null) {
            zIsGroupMembershipEditable = accountType.isGroupMembershipEditable();
        } else {
            zIsGroupMembershipEditable = false;
        }
        this.uri = uriWithAppendedId;
        this.accountName = cursor.getString(0);
        this.accountType = cursor.getString(1);
        this.dataSet = cursor.getString(2);
        this.groupId = j;
        this.groupName = cursor.getString(4);
        this.readOnly = getBoolean(cursor, 7);
        this.defaultGroup = getBoolean(cursor, 5);
        this.favorites = getBoolean(cursor, 6);
        this.editable = zIsGroupMembershipEditable;
    }

    private static boolean getBoolean(Cursor cursor, int i) {
        return (cursor.isNull(i) || cursor.getInt(i) == 0) ? false : true;
    }

    private GroupMetaData(Parcel parcel) {
        this.uri = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
        this.accountName = parcel.readString();
        this.accountType = parcel.readString();
        this.dataSet = parcel.readString();
        this.groupId = parcel.readLong();
        this.groupName = parcel.readString();
        this.readOnly = parcel.readInt() == 1;
        this.defaultGroup = parcel.readInt() == 1;
        this.favorites = parcel.readInt() == 1;
        this.editable = parcel.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.uri, 0);
        parcel.writeString(this.accountName);
        parcel.writeString(this.accountType);
        parcel.writeString(this.dataSet);
        parcel.writeLong(this.groupId);
        parcel.writeString(this.groupName);
        parcel.writeInt(this.readOnly ? 1 : 0);
        parcel.writeInt(this.defaultGroup ? 1 : 0);
        parcel.writeInt(this.favorites ? 1 : 0);
        parcel.writeInt(this.editable ? 1 : 0);
    }

    public boolean isValid() {
        return (this.uri == null || TextUtils.isEmpty(this.accountName) || TextUtils.isEmpty(this.groupName) || this.groupId <= 0) ? false : true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this).add(ContactSaveService.EXTRA_ACCOUNT_NAME, Log.anonymize(this.accountName)).add(ContactSaveService.EXTRA_ACCOUNT_TYPE, this.accountType).add(ContactSaveService.EXTRA_DATA_SET, this.dataSet).add(ContactSaveService.EXTRA_GROUP_ID, this.groupId).add("groupName", Log.anonymize(this.groupName)).add("readOnly", this.readOnly).add("defaultGroup", this.defaultGroup).add("favorites", this.favorites).add("editable", this.editable).add("isValid", isValid()).toString();
    }
}
