package com.android.contacts.common.model;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import com.android.contacts.common.model.account.AccountType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;

public class RawContactDelta implements Parcelable {
    public static final Parcelable.Creator<RawContactDelta> CREATOR = new Parcelable.Creator<RawContactDelta>() {
        @Override
        public RawContactDelta createFromParcel(Parcel in) {
            RawContactDelta state = new RawContactDelta();
            state.readFromParcel(in);
            return state;
        }

        @Override
        public RawContactDelta[] newArray(int size) {
            return new RawContactDelta[size];
        }
    };
    private Uri mContactsQueryUri = ContactsContract.RawContacts.CONTENT_URI;
    private final HashMap<String, ArrayList<ValuesDelta>> mEntries = Maps.newHashMap();
    private ValuesDelta mValues;

    public RawContactDelta() {
    }

    public RawContactDelta(ValuesDelta values) {
        this.mValues = values;
    }

    public static RawContactDelta fromBefore(RawContact before) {
        RawContactDelta rawContactDelta = new RawContactDelta();
        rawContactDelta.mValues = ValuesDelta.fromBefore(before.getValues());
        rawContactDelta.mValues.setIdColumn("_id");
        for (ContentValues values : before.getContentValues()) {
            rawContactDelta.addEntry(ValuesDelta.fromBefore(values));
        }
        return rawContactDelta;
    }

    public static RawContactDelta mergeAfter(RawContactDelta local, RawContactDelta remote) {
        ValuesDelta remoteValues = remote.mValues;
        if (local == null && (remoteValues.isDelete() || remoteValues.isTransient())) {
            return null;
        }
        if (local == null) {
            local = new RawContactDelta();
        }
        local.mValues = ValuesDelta.mergeAfter(local.mValues, remote.mValues);
        for (ArrayList<ValuesDelta> mimeEntries : remote.mEntries.values()) {
            for (ValuesDelta remoteEntry : mimeEntries) {
                Long childId = remoteEntry.getId();
                ValuesDelta localEntry = local.getEntry(childId);
                ValuesDelta merged = ValuesDelta.mergeAfter(localEntry, remoteEntry);
                if (localEntry == null && merged != null) {
                    local.addEntry(merged);
                }
            }
        }
        return local;
    }

    public ValuesDelta getValues() {
        return this.mValues;
    }

    public boolean isContactInsert() {
        return this.mValues.isInsert();
    }

    public ValuesDelta getPrimaryEntry(String mimeType) {
        ArrayList<ValuesDelta> mimeEntries = getMimeEntries(mimeType, false);
        if (mimeEntries == null) {
            return null;
        }
        for (ValuesDelta entry : mimeEntries) {
            if (entry.isPrimary()) {
                return entry;
            }
        }
        if (mimeEntries.size() > 0) {
            return mimeEntries.get(0);
        }
        return null;
    }

    public ValuesDelta getSuperPrimaryEntry(String mimeType, boolean forceSelection) {
        ArrayList<ValuesDelta> mimeEntries = getMimeEntries(mimeType, false);
        if (mimeEntries == null) {
            return null;
        }
        ValuesDelta primary = null;
        for (ValuesDelta entry : mimeEntries) {
            if (entry.isSuperPrimary()) {
                return entry;
            }
            if (entry.isPrimary()) {
                primary = entry;
            }
        }
        if (!forceSelection) {
            return null;
        }
        if (primary != null) {
            return primary;
        }
        if (mimeEntries.size() > 0) {
            return mimeEntries.get(0);
        }
        return null;
    }

    public AccountType getRawContactAccountType(Context context) {
        ContentValues entityValues = getValues().getCompleteValues();
        String type = entityValues.getAsString("account_type");
        String dataSet = entityValues.getAsString("data_set");
        return AccountTypeManager.getInstance(context).getAccountType(type, dataSet);
    }

    public Long getRawContactId() {
        return getValues().getAsLong("_id");
    }

    public String getAccountName() {
        return getValues().getAsString("account_name");
    }

    public String getAccountType() {
        return getValues().getAsString("account_type");
    }

    public String getDataSet() {
        return getValues().getAsString("data_set");
    }

    public AccountType getAccountType(AccountTypeManager manager) {
        return manager.getAccountType(getAccountType(), getDataSet());
    }

    public boolean isVisible() {
        return getValues().isVisible();
    }

    private ArrayList<ValuesDelta> getMimeEntries(String mimeType, boolean lazyCreate) {
        ArrayList<ValuesDelta> mimeEntries = this.mEntries.get(mimeType);
        if (mimeEntries == null && lazyCreate) {
            ArrayList<ValuesDelta> mimeEntries2 = Lists.newArrayList();
            this.mEntries.put(mimeType, mimeEntries2);
            return mimeEntries2;
        }
        return mimeEntries;
    }

    public ArrayList<ValuesDelta> getMimeEntries(String mimeType) {
        return getMimeEntries(mimeType, false);
    }

    public int getMimeEntriesCount(String mimeType, boolean onlyVisible) {
        ArrayList<ValuesDelta> mimeEntries = getMimeEntries(mimeType);
        if (mimeEntries == null) {
            return 0;
        }
        int count = 0;
        for (ValuesDelta child : mimeEntries) {
            if (!onlyVisible || child.isVisible()) {
                count++;
            }
        }
        return count;
    }

    public boolean hasMimeEntries(String mimeType) {
        return this.mEntries.containsKey(mimeType);
    }

    public ValuesDelta addEntry(ValuesDelta entry) {
        String mimeType = entry.getMimetype();
        getMimeEntries(mimeType, true).add(entry);
        return entry;
    }

    public ArrayList<ContentValues> getContentValues() {
        ArrayList<ContentValues> values = Lists.newArrayList();
        for (ArrayList<ValuesDelta> mimeEntries : this.mEntries.values()) {
            for (ValuesDelta entry : mimeEntries) {
                if (!entry.isDelete()) {
                    values.add(entry.getCompleteValues());
                }
            }
        }
        return values;
    }

    public ValuesDelta getEntry(Long childId) {
        if (childId == null) {
            return null;
        }
        for (ArrayList<ValuesDelta> mimeEntries : this.mEntries.values()) {
            for (ValuesDelta entry : mimeEntries) {
                if (childId.equals(entry.getId())) {
                    return entry;
                }
            }
        }
        return null;
    }

    public int getEntryCount(boolean onlyVisible) {
        int count = 0;
        for (String mimeType : this.mEntries.keySet()) {
            count += getMimeEntriesCount(mimeType, onlyVisible);
        }
        return count;
    }

    public boolean equals(Object object) {
        if (!(object instanceof RawContactDelta)) {
            return false;
        }
        RawContactDelta other = (RawContactDelta) object;
        if (!other.mValues.equals(this.mValues)) {
            return false;
        }
        for (ArrayList<ValuesDelta> mimeEntries : this.mEntries.values()) {
            for (ValuesDelta child : mimeEntries) {
                if (!other.containsEntry(child)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean containsEntry(ValuesDelta entry) {
        for (ArrayList<ValuesDelta> mimeEntries : this.mEntries.values()) {
            for (ValuesDelta child : mimeEntries) {
                if (child.equals(entry)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void markDeleted() {
        this.mValues.markDeleted();
        for (ArrayList<ValuesDelta> mimeEntries : this.mEntries.values()) {
            for (ValuesDelta child : mimeEntries) {
                child.markDeleted();
            }
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n(");
        builder.append("Uri=");
        builder.append(this.mContactsQueryUri);
        builder.append(", Values=");
        builder.append(this.mValues != null ? this.mValues.toString() : "null");
        builder.append(", Entries={");
        for (ArrayList<ValuesDelta> mimeEntries : this.mEntries.values()) {
            for (ValuesDelta child : mimeEntries) {
                builder.append("\n\t");
                child.toString(builder);
            }
        }
        builder.append("\n})\n");
        return builder.toString();
    }

    private void possibleAdd(ArrayList<ContentProviderOperation> diff, ContentProviderOperation.Builder builder) {
        if (builder != null) {
            diff.add(builder.build());
        }
    }

    public void buildAssert(ArrayList<ContentProviderOperation> buildInto) {
        boolean isContactInsert = this.mValues.isInsert();
        if (!isContactInsert) {
            Long beforeId = this.mValues.getId();
            Long beforeVersion = this.mValues.getAsLong("version");
            if (beforeId != null && beforeVersion != null) {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newAssertQuery(this.mContactsQueryUri);
                builder.withSelection("_id=" + beforeId, null);
                builder.withValue("version", beforeVersion);
                buildInto.add(builder.build());
            }
        }
    }

    public void buildDiff(ArrayList<ContentProviderOperation> buildInto) {
        ContentProviderOperation.Builder builder;
        int firstIndex = buildInto.size();
        boolean isContactInsert = this.mValues.isInsert();
        boolean isContactDelete = this.mValues.isDelete();
        boolean isContactUpdate = (isContactInsert || isContactDelete) ? false : true;
        Long beforeId = this.mValues.getId();
        if (isContactInsert) {
            this.mValues.put("aggregation_mode", 2);
        }
        possibleAdd(buildInto, this.mValues.buildDiff(this.mContactsQueryUri));
        for (ArrayList<ValuesDelta> mimeEntries : this.mEntries.values()) {
            for (ValuesDelta child : mimeEntries) {
                if (!isContactDelete) {
                    if (this.mContactsQueryUri.equals(ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI)) {
                        builder = child.buildDiff(Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, "data"));
                    } else {
                        builder = child.buildDiff(ContactsContract.Data.CONTENT_URI);
                    }
                    if (child.isInsert()) {
                        if (isContactInsert) {
                            builder.withValueBackReference("raw_contact_id", firstIndex);
                        } else {
                            builder.withValue("raw_contact_id", beforeId);
                        }
                    } else if (isContactInsert && builder != null) {
                        throw new IllegalArgumentException("When parent insert, child must be also");
                    }
                    possibleAdd(buildInto, builder);
                }
            }
        }
        boolean addedOperations = buildInto.size() > firstIndex;
        if (addedOperations && isContactUpdate) {
            buildInto.add(firstIndex, buildSetAggregationMode(beforeId, 2).build());
            buildInto.add(buildSetAggregationMode(beforeId, 0).build());
        } else if (isContactInsert) {
            ContentProviderOperation.Builder builder2 = ContentProviderOperation.newUpdate(this.mContactsQueryUri);
            builder2.withValue("aggregation_mode", 0);
            builder2.withSelection("_id=?", new String[1]);
            builder2.withSelectionBackReference(0, firstIndex);
            buildInto.add(builder2.build());
        }
    }

    protected ContentProviderOperation.Builder buildSetAggregationMode(Long beforeId, int mode) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(this.mContactsQueryUri);
        builder.withValue("aggregation_mode", Integer.valueOf(mode));
        builder.withSelection("_id=" + beforeId, null);
        return builder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        int size = getEntryCount(false);
        dest.writeInt(size);
        dest.writeParcelable(this.mValues, flags);
        dest.writeParcelable(this.mContactsQueryUri, flags);
        for (ArrayList<ValuesDelta> mimeEntries : this.mEntries.values()) {
            for (ValuesDelta child : mimeEntries) {
                dest.writeParcelable(child, flags);
            }
        }
    }

    public void readFromParcel(Parcel source) {
        ClassLoader loader = getClass().getClassLoader();
        int size = source.readInt();
        this.mValues = (ValuesDelta) source.readParcelable(loader);
        this.mContactsQueryUri = (Uri) source.readParcelable(loader);
        for (int i = 0; i < size; i++) {
            ValuesDelta child = (ValuesDelta) source.readParcelable(loader);
            addEntry(child);
        }
    }

    public void setProfileQueryUri() {
        this.mContactsQueryUri = ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI;
    }
}
