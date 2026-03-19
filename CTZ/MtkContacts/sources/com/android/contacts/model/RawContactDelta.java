package com.android.contacts.model;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class RawContactDelta implements Parcelable {
    public static final Parcelable.Creator<RawContactDelta> CREATOR = new Parcelable.Creator<RawContactDelta>() {
        @Override
        public RawContactDelta createFromParcel(Parcel parcel) {
            RawContactDelta rawContactDelta = new RawContactDelta();
            rawContactDelta.readFromParcel(parcel);
            return rawContactDelta;
        }

        @Override
        public RawContactDelta[] newArray(int i) {
            return new RawContactDelta[i];
        }
    };
    private static final boolean DEBUG = false;
    private static final String TAG = "EntityDelta";
    private Uri mContactsQueryUri = ContactsContract.RawContacts.CONTENT_URI;
    private final HashMap<String, ArrayList<ValuesDelta>> mEntries = Maps.newHashMap();
    private ValuesDelta mValues;

    public RawContactDelta() {
    }

    public RawContactDelta(ValuesDelta valuesDelta) {
        this.mValues = valuesDelta;
    }

    public static RawContactDelta fromBefore(RawContact rawContact) {
        RawContactDelta rawContactDelta = new RawContactDelta();
        rawContactDelta.mValues = ValuesDelta.fromBefore(rawContact.getValues());
        rawContactDelta.mValues.setIdColumn("_id");
        Iterator<ContentValues> it = rawContact.getContentValues().iterator();
        while (it.hasNext()) {
            rawContactDelta.addEntry(ValuesDelta.fromBefore(it.next()));
        }
        return rawContactDelta;
    }

    public static RawContactDelta mergeAfter(RawContactDelta rawContactDelta, RawContactDelta rawContactDelta2) {
        ValuesDelta valuesDelta = rawContactDelta2.mValues;
        if (rawContactDelta == null && (valuesDelta.isDelete() || valuesDelta.isTransient())) {
            return null;
        }
        if (rawContactDelta == null) {
            rawContactDelta = new RawContactDelta();
        }
        rawContactDelta.mValues = ValuesDelta.mergeAfter(rawContactDelta.mValues, rawContactDelta2.mValues);
        Iterator<ArrayList<ValuesDelta>> it = rawContactDelta2.mEntries.values().iterator();
        while (it.hasNext()) {
            for (ValuesDelta valuesDelta2 : it.next()) {
                ValuesDelta entry = rawContactDelta.getEntry(valuesDelta2.getId());
                ValuesDelta valuesDeltaMergeAfter = ValuesDelta.mergeAfter(entry, valuesDelta2);
                if (entry == null && valuesDeltaMergeAfter != null) {
                    rawContactDelta.addEntry(valuesDeltaMergeAfter);
                }
            }
        }
        return rawContactDelta;
    }

    public ValuesDelta getValues() {
        return this.mValues;
    }

    public boolean isContactInsert() {
        return this.mValues.isInsert();
    }

    public ValuesDelta getPrimaryEntry(String str) {
        ArrayList<ValuesDelta> mimeEntries = getMimeEntries(str, false);
        if (mimeEntries == null) {
            return null;
        }
        for (ValuesDelta valuesDelta : mimeEntries) {
            if (valuesDelta.isPrimary()) {
                return valuesDelta;
            }
        }
        if (mimeEntries.size() > 0) {
            return mimeEntries.get(0);
        }
        return null;
    }

    public ValuesDelta getSuperPrimaryEntry(String str) {
        return getSuperPrimaryEntry(str, true);
    }

    public ValuesDelta getSuperPrimaryEntry(String str, boolean z) {
        ArrayList<ValuesDelta> mimeEntries = getMimeEntries(str, false);
        if (mimeEntries == null) {
            return null;
        }
        ValuesDelta valuesDelta = null;
        for (ValuesDelta valuesDelta2 : mimeEntries) {
            if (valuesDelta2.isSuperPrimary()) {
                return valuesDelta2;
            }
            if (valuesDelta2.isPrimary()) {
                valuesDelta = valuesDelta2;
            }
        }
        if (!z) {
            return null;
        }
        if (valuesDelta != null) {
            return valuesDelta;
        }
        if (mimeEntries.size() > 0) {
            return mimeEntries.get(0);
        }
        return null;
    }

    public AccountType getRawContactAccountType(Context context) {
        ContentValues completeValues = getValues().getCompleteValues();
        return AccountTypeManager.getInstance(context).getAccountType(completeValues.getAsString("account_type"), completeValues.getAsString("data_set"));
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

    public AccountType getAccountType(AccountTypeManager accountTypeManager) {
        return accountTypeManager.getAccountType(getAccountType(), getDataSet());
    }

    public AccountWithDataSet getAccountWithDataSet() {
        return new AccountWithDataSet(getAccountName(), getAccountType(), getDataSet());
    }

    public boolean isVisible() {
        return getValues().isVisible();
    }

    private ArrayList<ValuesDelta> getMimeEntries(String str, boolean z) {
        ArrayList<ValuesDelta> arrayList = this.mEntries.get(str);
        if (arrayList == null && z) {
            ArrayList<ValuesDelta> arrayListNewArrayList = Lists.newArrayList();
            this.mEntries.put(str, arrayListNewArrayList);
            return arrayListNewArrayList;
        }
        return arrayList;
    }

    public ArrayList<ValuesDelta> getMimeEntries(String str) {
        return getMimeEntries(str, false);
    }

    public int getMimeEntriesCount(String str, boolean z) {
        ArrayList<ValuesDelta> mimeEntries = getMimeEntries(str);
        int i = 0;
        if (mimeEntries == null) {
            return 0;
        }
        for (ValuesDelta valuesDelta : mimeEntries) {
            if (!z || valuesDelta.isVisible()) {
                i++;
            }
        }
        return i;
    }

    public boolean hasMimeEntries(String str) {
        return this.mEntries.containsKey(str);
    }

    public ValuesDelta addEntry(ValuesDelta valuesDelta) {
        getMimeEntries(valuesDelta.getMimetype(), true).add(valuesDelta);
        return valuesDelta;
    }

    public ArrayList<ContentValues> getContentValues() {
        ArrayList<ContentValues> arrayListNewArrayList = Lists.newArrayList();
        Iterator<ArrayList<ValuesDelta>> it = this.mEntries.values().iterator();
        while (it.hasNext()) {
            for (ValuesDelta valuesDelta : it.next()) {
                if (!valuesDelta.isDelete()) {
                    arrayListNewArrayList.add(valuesDelta.getCompleteValues());
                }
            }
        }
        return arrayListNewArrayList;
    }

    public ValuesDelta getEntry(Long l) {
        if (l == null) {
            return null;
        }
        Iterator<ArrayList<ValuesDelta>> it = this.mEntries.values().iterator();
        while (it.hasNext()) {
            for (ValuesDelta valuesDelta : it.next()) {
                if (l.equals(valuesDelta.getId())) {
                    return valuesDelta;
                }
            }
        }
        return null;
    }

    public int getEntryCount(boolean z) {
        Iterator<String> it = this.mEntries.keySet().iterator();
        int mimeEntriesCount = 0;
        while (it.hasNext()) {
            mimeEntriesCount += getMimeEntriesCount(it.next(), z);
        }
        return mimeEntriesCount;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof RawContactDelta)) {
            return false;
        }
        RawContactDelta rawContactDelta = (RawContactDelta) obj;
        if (!rawContactDelta.mValues.equals(this.mValues)) {
            return false;
        }
        Iterator<ArrayList<ValuesDelta>> it = this.mEntries.values().iterator();
        while (it.hasNext()) {
            Iterator<ValuesDelta> it2 = it.next().iterator();
            while (it2.hasNext()) {
                if (!rawContactDelta.containsEntry(it2.next())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean containsEntry(ValuesDelta valuesDelta) {
        Iterator<ArrayList<ValuesDelta>> it = this.mEntries.values().iterator();
        while (it.hasNext()) {
            Iterator<ValuesDelta> it2 = it.next().iterator();
            while (it2.hasNext()) {
                if (it2.next().equals(valuesDelta)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void markDeleted() {
        this.mValues.markDeleted();
        Iterator<ArrayList<ValuesDelta>> it = this.mEntries.values().iterator();
        while (it.hasNext()) {
            Iterator<ValuesDelta> it2 = it.next().iterator();
            while (it2.hasNext()) {
                it2.next().markDeleted();
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n(");
        sb.append("Uri=");
        sb.append(this.mContactsQueryUri);
        sb.append(", Values=");
        sb.append(this.mValues != null ? this.mValues.toString() : "null");
        sb.append(", Entries={");
        Iterator<ArrayList<ValuesDelta>> it = this.mEntries.values().iterator();
        while (it.hasNext()) {
            for (ValuesDelta valuesDelta : it.next()) {
                sb.append("\n\t");
                valuesDelta.toString(sb);
            }
        }
        sb.append("\n})\n");
        return sb.toString();
    }

    private void possibleAdd(ArrayList<ContentProviderOperation> arrayList, ContentProviderOperation.Builder builder) {
        if (builder != null) {
            arrayList.add(builder.build());
        }
    }

    private void possibleAddWrapper(ArrayList<CPOWrapper> arrayList, BuilderWrapper builderWrapper) {
        if (builderWrapper != null && builderWrapper.getBuilder() != null) {
            arrayList.add(new CPOWrapper(builderWrapper.getBuilder().build(), builderWrapper.getType()));
        }
    }

    public void buildAssert(ArrayList<ContentProviderOperation> arrayList) {
        ContentProviderOperation.Builder builderBuildAssertHelper = buildAssertHelper();
        if (builderBuildAssertHelper != null) {
            arrayList.add(builderBuildAssertHelper.build());
        }
    }

    public void buildAssertWrapper(ArrayList<CPOWrapper> arrayList) {
        ContentProviderOperation.Builder builderBuildAssertHelper = buildAssertHelper();
        if (builderBuildAssertHelper != null) {
            arrayList.add(new CPOWrapper(builderBuildAssertHelper.build(), 4));
        }
    }

    private ContentProviderOperation.Builder buildAssertHelper() {
        if (this.mValues.isInsert()) {
            return null;
        }
        Long id = this.mValues.getId();
        Long asLong = this.mValues.getAsLong("version");
        if (id == null || asLong == null) {
            return null;
        }
        ContentProviderOperation.Builder builderNewAssertQuery = ContentProviderOperation.newAssertQuery(this.mContactsQueryUri);
        builderNewAssertQuery.withSelection("_id=" + id, null);
        builderNewAssertQuery.withValue("version", asLong);
        return builderNewAssertQuery;
    }

    public void buildDiff(ArrayList<ContentProviderOperation> arrayList) {
        ContentProviderOperation.Builder builderBuildDiff;
        int size = arrayList.size();
        boolean zIsInsert = this.mValues.isInsert();
        boolean zIsDelete = this.mValues.isDelete();
        boolean z = (zIsInsert || zIsDelete) ? false : true;
        Long id = this.mValues.getId();
        if (zIsInsert) {
            this.mValues.put("aggregation_mode", 2);
        }
        possibleAdd(arrayList, this.mValues.buildDiff(this.mContactsQueryUri));
        Iterator<ArrayList<ValuesDelta>> it = this.mEntries.values().iterator();
        while (it.hasNext()) {
            for (ValuesDelta valuesDelta : it.next()) {
                if (!zIsDelete) {
                    if (this.mContactsQueryUri.equals(ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI)) {
                        builderBuildDiff = valuesDelta.buildDiff(Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, "data"));
                    } else {
                        builderBuildDiff = valuesDelta.buildDiff(ContactsContract.Data.CONTENT_URI);
                    }
                    if (valuesDelta.isInsert()) {
                        if (zIsInsert) {
                            builderBuildDiff.withValueBackReference("raw_contact_id", size);
                        } else {
                            builderBuildDiff.withValue("raw_contact_id", id);
                        }
                    } else if (zIsInsert && builderBuildDiff != null) {
                        throw new IllegalArgumentException("When parent insert, child must be also");
                    }
                    possibleAdd(arrayList, builderBuildDiff);
                }
            }
        }
        if ((arrayList.size() > size) && z) {
            arrayList.add(size, buildSetAggregationMode(id, 2).build());
            arrayList.add(buildSetAggregationMode(id, 0).build());
        } else if (zIsInsert) {
            ContentProviderOperation.Builder builderNewUpdate = ContentProviderOperation.newUpdate(this.mContactsQueryUri);
            builderNewUpdate.withValue("aggregation_mode", 0);
            builderNewUpdate.withSelection("_id=?", new String[1]);
            builderNewUpdate.withSelectionBackReference(0, size);
            arrayList.add(builderNewUpdate.build());
        }
    }

    public void buildDiffWrapper(ArrayList<CPOWrapper> arrayList) {
        BuilderWrapper builderWrapperBuildDiffWrapper;
        int size = arrayList.size();
        boolean zIsInsert = this.mValues.isInsert();
        boolean zIsDelete = this.mValues.isDelete();
        boolean z = (zIsInsert || zIsDelete) ? false : true;
        Long id = this.mValues.getId();
        if (zIsInsert) {
            this.mValues.put("aggregation_mode", 2);
        }
        possibleAddWrapper(arrayList, this.mValues.buildDiffWrapper(this.mContactsQueryUri));
        Iterator<ArrayList<ValuesDelta>> it = this.mEntries.values().iterator();
        while (it.hasNext()) {
            for (ValuesDelta valuesDelta : it.next()) {
                if (!zIsDelete) {
                    if (this.mContactsQueryUri.equals(ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI)) {
                        builderWrapperBuildDiffWrapper = valuesDelta.buildDiffWrapper(Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, "data"));
                    } else {
                        builderWrapperBuildDiffWrapper = valuesDelta.buildDiffWrapper(ContactsContract.Data.CONTENT_URI);
                    }
                    if (valuesDelta.isInsert()) {
                        if (zIsInsert) {
                            builderWrapperBuildDiffWrapper.getBuilder().withValueBackReference("raw_contact_id", size);
                        } else {
                            builderWrapperBuildDiffWrapper.getBuilder().withValue("raw_contact_id", id);
                        }
                    } else if (zIsInsert && builderWrapperBuildDiffWrapper != null && builderWrapperBuildDiffWrapper.getBuilder() != null) {
                        throw new IllegalArgumentException("When parent insert, child must be also");
                    }
                    possibleAddWrapper(arrayList, builderWrapperBuildDiffWrapper);
                }
            }
        }
        if ((arrayList.size() > size) && z) {
            arrayList.add(size, new CPOWrapper(buildSetAggregationMode(id, 2).build(), 2));
            arrayList.add(new CPOWrapper(buildSetAggregationMode(id, 0).build(), 2));
        } else if (zIsInsert) {
            ContentProviderOperation.Builder builderNewUpdate = ContentProviderOperation.newUpdate(this.mContactsQueryUri);
            builderNewUpdate.withValue("aggregation_mode", 0);
            builderNewUpdate.withSelection("_id=?", new String[1]);
            builderNewUpdate.withSelectionBackReference(0, size);
            arrayList.add(new CPOWrapper(builderNewUpdate.build(), 2));
        }
    }

    protected ContentProviderOperation.Builder buildSetAggregationMode(Long l, int i) {
        ContentProviderOperation.Builder builderNewUpdate = ContentProviderOperation.newUpdate(this.mContactsQueryUri);
        builderNewUpdate.withValue("aggregation_mode", Integer.valueOf(i));
        builderNewUpdate.withSelection("_id=" + l, null);
        return builderNewUpdate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(getEntryCount(false));
        parcel.writeParcelable(this.mValues, i);
        parcel.writeParcelable(this.mContactsQueryUri, i);
        Iterator<ArrayList<ValuesDelta>> it = this.mEntries.values().iterator();
        while (it.hasNext()) {
            Iterator<ValuesDelta> it2 = it.next().iterator();
            while (it2.hasNext()) {
                parcel.writeParcelable(it2.next(), i);
            }
        }
    }

    public void readFromParcel(Parcel parcel) {
        ClassLoader classLoader = getClass().getClassLoader();
        int i = parcel.readInt();
        this.mValues = (ValuesDelta) parcel.readParcelable(classLoader);
        this.mContactsQueryUri = (Uri) parcel.readParcelable(classLoader);
        for (int i2 = 0; i2 < i; i2++) {
            addEntry((ValuesDelta) parcel.readParcelable(classLoader));
        }
    }

    public void setProfileQueryUri() {
        this.mContactsQueryUri = ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI;
    }

    public void removeEntry(String str) {
        this.mEntries.remove(str);
    }
}
