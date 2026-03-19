package com.android.contacts.editor;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import com.android.contacts.model.AccountTypeManager;
import java.util.ArrayList;
import java.util.HashMap;

public class PickRawContactLoader extends AsyncTaskLoader<RawContactsMetadata> {
    private static final String[] RAW_CONTACT_PROJECTION = {"account_name", "account_type", "data_set", "_id", "display_name", "display_name_alt"};
    private RawContactsMetadata mCachedResult;
    private Uri mContactUri;

    public PickRawContactLoader(Context context, Uri uri) {
        super(context);
        this.mContactUri = ensureIsContactUri(uri);
    }

    @Override
    public RawContactsMetadata loadInBackground() {
        Uri uri;
        Uri uriWithAppendedPath;
        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursorQuery = contentResolver.query(this.mContactUri, new String[]{"_id", "is_user_profile"}, null, null, null);
        if (cursorQuery == null) {
            return null;
        }
        if (cursorQuery.getCount() < 1) {
            return null;
        }
        RawContactsMetadata rawContactsMetadata = new RawContactsMetadata();
        try {
            cursorQuery.moveToFirst();
            rawContactsMetadata.contactId = cursorQuery.getLong(0);
            rawContactsMetadata.isUserProfile = cursorQuery.getInt(1) == 1;
            cursorQuery.close();
            if (rawContactsMetadata.isUserProfile) {
                uri = ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI;
            } else {
                uri = ContactsContract.RawContacts.CONTENT_URI;
            }
            cursorQuery = contentResolver.query(uri, RAW_CONTACT_PROJECTION, "contact_id=?", new String[]{Long.toString(rawContactsMetadata.contactId)}, null);
            if (cursorQuery == null) {
                return null;
            }
            if (cursorQuery.getCount() < 1) {
                return null;
            }
            cursorQuery.moveToPosition(-1);
            StringBuilder sb = new StringBuilder("raw_contact_id IN (");
            HashMap map = new HashMap();
            while (cursorQuery.moveToNext()) {
                try {
                    RawContact rawContact = new RawContact();
                    rawContact.id = cursorQuery.getLong(3);
                    sb.append(rawContact.id);
                    sb.append(',');
                    rawContact.displayName = cursorQuery.getString(4);
                    rawContact.displayNameAlt = cursorQuery.getString(5);
                    rawContact.accountName = cursorQuery.getString(0);
                    rawContact.accountType = cursorQuery.getString(1);
                    rawContact.accountDataSet = cursorQuery.getString(2);
                    rawContactsMetadata.rawContacts.add(rawContact);
                    map.put(Long.valueOf(rawContact.id), rawContact);
                } finally {
                }
            }
            cursorQuery.close();
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(") AND mimetype=\"vnd.android.cursor.item/photo\"");
            if (rawContactsMetadata.isUserProfile) {
                uriWithAppendedPath = Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.Data.CONTENT_URI.getPath());
            } else {
                uriWithAppendedPath = ContactsContract.Data.CONTENT_URI;
            }
            cursorQuery = contentResolver.query(uriWithAppendedPath, new String[]{"raw_contact_id", "_id"}, sb.toString(), null, null);
            if (cursorQuery != null) {
                try {
                    cursorQuery.moveToPosition(-1);
                    while (cursorQuery.moveToNext()) {
                        ((RawContact) map.get(Long.valueOf(cursorQuery.getLong(0)))).photoId = cursorQuery.getLong(1);
                    }
                } finally {
                }
            }
            return rawContactsMetadata;
        } finally {
        }
    }

    @Override
    public void deliverResult(RawContactsMetadata rawContactsMetadata) {
        this.mCachedResult = rawContactsMetadata;
        if (isStarted()) {
            super.deliverResult(rawContactsMetadata);
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (this.mCachedResult == null) {
            forceLoad();
        } else {
            deliverResult(this.mCachedResult);
        }
    }

    private static Uri ensureIsContactUri(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri must not be null");
        }
        if (!uri.toString().startsWith(ContactsContract.Contacts.CONTENT_URI.toString()) && !uri.toString().equals(ContactsContract.Profile.CONTENT_URI.toString())) {
            throw new IllegalArgumentException("Invalid contact Uri: " + uri);
        }
        return uri;
    }

    public static class RawContactsMetadata implements Parcelable {
        public static final Parcelable.Creator<RawContactsMetadata> CREATOR = new Parcelable.Creator<RawContactsMetadata>() {
            @Override
            public RawContactsMetadata createFromParcel(Parcel parcel) {
                return new RawContactsMetadata(parcel);
            }

            @Override
            public RawContactsMetadata[] newArray(int i) {
                return new RawContactsMetadata[i];
            }
        };
        public long contactId;
        public boolean isUserProfile;
        public ArrayList<RawContact> rawContacts;
        public boolean showReadOnly;

        public RawContactsMetadata() {
            this.showReadOnly = false;
            this.rawContacts = new ArrayList<>();
        }

        private RawContactsMetadata(Parcel parcel) {
            this.showReadOnly = false;
            this.rawContacts = new ArrayList<>();
            this.contactId = parcel.readLong();
            this.isUserProfile = parcel.readInt() == 1;
            this.showReadOnly = parcel.readInt() == 1;
            parcel.readTypedList(this.rawContacts, RawContact.CREATOR);
        }

        public void trimReadOnly(AccountTypeManager accountTypeManager) {
            for (int size = this.rawContacts.size() - 1; size >= 0; size--) {
                RawContact rawContact = this.rawContacts.get(size);
                if (!accountTypeManager.getAccountType(rawContact.accountType, rawContact.accountDataSet).areContactsWritable()) {
                    this.rawContacts.remove(size);
                }
            }
        }

        public int getIndexOfFirstWritableAccount(AccountTypeManager accountTypeManager) {
            for (int i = 0; i < this.rawContacts.size(); i++) {
                RawContact rawContact = this.rawContacts.get(i);
                if (accountTypeManager.getAccountType(rawContact.accountType, rawContact.accountDataSet).areContactsWritable()) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(this.contactId);
            parcel.writeInt(this.isUserProfile ? 1 : 0);
            parcel.writeInt(this.showReadOnly ? 1 : 0);
            parcel.writeTypedList(this.rawContacts);
        }
    }

    public static class RawContact implements Parcelable {
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
        public String accountDataSet;
        public String accountName;
        public String accountType;
        public String displayName;
        public String displayNameAlt;
        public long id;
        public long photoId;

        public RawContact() {
        }

        private RawContact(Parcel parcel) {
            this.id = parcel.readLong();
            this.photoId = parcel.readLong();
            this.displayName = parcel.readString();
            this.displayNameAlt = parcel.readString();
            this.accountName = parcel.readString();
            this.accountType = parcel.readString();
            this.accountDataSet = parcel.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(this.id);
            parcel.writeLong(this.photoId);
            parcel.writeString(this.displayName);
            parcel.writeString(this.displayNameAlt);
            parcel.writeString(this.accountName);
            parcel.writeString(this.accountType);
            parcel.writeString(this.accountDataSet);
        }
    }
}
