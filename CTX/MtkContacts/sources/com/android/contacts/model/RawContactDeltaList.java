package com.android.contacts.model;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Entity;
import android.content.EntityIterator;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import com.android.contacts.model.account.BaseAccountType;
import com.google.common.collect.Lists;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class RawContactDeltaList extends ArrayList<RawContactDelta> implements Parcelable {
    private long[] mJoinWithRawContactIds;
    private boolean mSplitRawContacts;
    private static final String TAG = RawContactDeltaList.class.getSimpleName();
    private static final boolean VERBOSE_LOGGING = Log.isLoggable(TAG, 2);
    public static final Parcelable.Creator<RawContactDeltaList> CREATOR = new Parcelable.Creator<RawContactDeltaList>() {
        @Override
        public RawContactDeltaList createFromParcel(Parcel parcel) {
            RawContactDeltaList rawContactDeltaList = new RawContactDeltaList();
            rawContactDeltaList.readFromParcel(parcel);
            return rawContactDeltaList;
        }

        @Override
        public RawContactDeltaList[] newArray(int i) {
            return new RawContactDeltaList[i];
        }
    };

    public static RawContactDeltaList fromQuery(Uri uri, ContentResolver contentResolver, String str, String[] strArr, String str2) {
        EntityIterator entityIteratorNewEntityIterator = ContactsContract.RawContacts.newEntityIterator(contentResolver.query(uri, null, str, strArr, str2));
        try {
            return fromIterator(entityIteratorNewEntityIterator);
        } finally {
            entityIteratorNewEntityIterator.close();
        }
    }

    public static RawContactDeltaList fromIterator(Iterator<?> it) {
        RawContactDeltaList rawContactDeltaList = new RawContactDeltaList();
        rawContactDeltaList.addAll(it);
        return rawContactDeltaList;
    }

    public void addAll(Iterator<?> it) {
        RawContact rawContactCreateFrom;
        while (it.hasNext()) {
            Object next = it.next();
            if (next instanceof Entity) {
                rawContactCreateFrom = RawContact.createFrom((Entity) next);
            } else {
                rawContactCreateFrom = (RawContact) next;
            }
            add(RawContactDelta.fromBefore(rawContactCreateFrom));
        }
    }

    public static RawContactDeltaList mergeAfter(RawContactDeltaList rawContactDeltaList, RawContactDeltaList rawContactDeltaList2) {
        if (rawContactDeltaList == null) {
            rawContactDeltaList = new RawContactDeltaList();
        }
        for (RawContactDelta rawContactDelta : rawContactDeltaList2) {
            RawContactDelta byRawContactId = rawContactDeltaList.getByRawContactId(rawContactDelta.getValues().getId());
            RawContactDelta rawContactDeltaMergeAfter = RawContactDelta.mergeAfter(byRawContactId, rawContactDelta);
            if (byRawContactId == null && rawContactDeltaMergeAfter != null) {
                rawContactDeltaList.add(rawContactDeltaMergeAfter);
            }
        }
        return rawContactDeltaList;
    }

    public ArrayList<CPOWrapper> buildDiffWrapper() {
        int i;
        if (VERBOSE_LOGGING) {
            Log.v(TAG, "buildDiffWrapper: list=" + toString());
        }
        ArrayList<CPOWrapper> arrayListNewArrayList = Lists.newArrayList();
        long jFindRawContactId = findRawContactId();
        Iterator<RawContactDelta> it = iterator();
        while (it.hasNext()) {
            it.next().buildAssertWrapper(arrayListNewArrayList);
        }
        int size = arrayListNewArrayList.size();
        int[] iArr = new int[size()];
        Iterator<RawContactDelta> it2 = iterator();
        int i2 = 0;
        int i3 = -1;
        while (it2.hasNext()) {
            RawContactDelta next = it2.next();
            int size2 = arrayListNewArrayList.size();
            boolean zIsContactInsert = next.isContactInsert();
            int i4 = i2 + 1;
            iArr[i2] = zIsContactInsert ? size2 : -1;
            next.buildDiffWrapper(arrayListNewArrayList);
            if (this.mJoinWithRawContactIds != null) {
                long[] jArr = this.mJoinWithRawContactIds;
                int length = jArr.length;
                int i5 = 0;
                while (i5 < length) {
                    int i6 = size2;
                    Long lValueOf = Long.valueOf(jArr[i5]);
                    ContentProviderOperation.Builder builderBeginKeepTogether = beginKeepTogether();
                    builderBeginKeepTogether.withValue("raw_contact_id1", lValueOf);
                    if (jFindRawContactId != -1) {
                        builderBeginKeepTogether.withValue("raw_contact_id2", Long.valueOf(jFindRawContactId));
                        i = i6;
                    } else {
                        i = i6;
                        builderBeginKeepTogether.withValueBackReference("raw_contact_id2", i);
                    }
                    arrayListNewArrayList.add(new CPOWrapper(builderBeginKeepTogether.build(), 2));
                    i5++;
                    size2 = i;
                    it2 = it2;
                }
            }
            Iterator<RawContactDelta> it3 = it2;
            int i7 = size2;
            if (zIsContactInsert && !this.mSplitRawContacts) {
                if (jFindRawContactId != -1) {
                    ContentProviderOperation.Builder builderBeginKeepTogether2 = beginKeepTogether();
                    builderBeginKeepTogether2.withValue("raw_contact_id1", Long.valueOf(jFindRawContactId));
                    builderBeginKeepTogether2.withValueBackReference("raw_contact_id2", i7);
                    arrayListNewArrayList.add(new CPOWrapper(builderBeginKeepTogether2.build(), 2));
                } else if (i3 != -1) {
                    ContentProviderOperation.Builder builderBeginKeepTogether3 = beginKeepTogether();
                    builderBeginKeepTogether3.withValueBackReference("raw_contact_id1", i3);
                    builderBeginKeepTogether3.withValueBackReference("raw_contact_id2", i7);
                    arrayListNewArrayList.add(new CPOWrapper(builderBeginKeepTogether3.build(), 2));
                } else {
                    i3 = i7;
                }
            }
            i2 = i4;
            it2 = it3;
        }
        if (this.mSplitRawContacts) {
            buildSplitContactDiffWrapper(arrayListNewArrayList, iArr);
        }
        if (arrayListNewArrayList.size() == size) {
            arrayListNewArrayList.clear();
        }
        if (VERBOSE_LOGGING) {
            Log.v(TAG, "buildDiff: ops=" + diffToStringWrapper(arrayListNewArrayList));
        }
        return arrayListNewArrayList;
    }

    private static String diffToString(ArrayList<ContentProviderOperation> arrayList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        Iterator<ContentProviderOperation> it = arrayList.iterator();
        while (it.hasNext()) {
            sb.append(it.next().toString());
            sb.append(",\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static String diffToStringWrapper(ArrayList<CPOWrapper> arrayList) {
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        Iterator<CPOWrapper> it = arrayList.iterator();
        while (it.hasNext()) {
            arrayListNewArrayList.add(it.next().getOperation());
        }
        return diffToString(arrayListNewArrayList);
    }

    protected ContentProviderOperation.Builder beginKeepTogether() {
        ContentProviderOperation.Builder builderNewUpdate = ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI);
        builderNewUpdate.withValue(BaseAccountType.Attr.TYPE, 1);
        return builderNewUpdate;
    }

    private void buildSplitContactDiffWrapper(ArrayList<CPOWrapper> arrayList, int[] iArr) {
        ContentProviderOperation.Builder builderBuildSplitContactDiffHelper;
        int size = size();
        for (int i = 0; i < size; i++) {
            for (int i2 = 0; i2 < size; i2++) {
                if (i != i2 && (builderBuildSplitContactDiffHelper = buildSplitContactDiffHelper(i, i2, iArr)) != null) {
                    arrayList.add(new CPOWrapper(builderBuildSplitContactDiffHelper.build(), 2));
                }
            }
        }
    }

    private ContentProviderOperation.Builder buildSplitContactDiffHelper(int i, int i2, int[] iArr) {
        ContentProviderOperation.Builder builderNewUpdate = ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI);
        builderNewUpdate.withValue(BaseAccountType.Attr.TYPE, 2);
        Long asLong = get(i).getValues().getAsLong("_id");
        int i3 = iArr[i];
        if (asLong != null && asLong.longValue() >= 0) {
            builderNewUpdate.withValue("raw_contact_id1", asLong);
        } else {
            if (i3 < 0) {
                return null;
            }
            builderNewUpdate.withValueBackReference("raw_contact_id1", i3);
        }
        Long asLong2 = get(i2).getValues().getAsLong("_id");
        int i4 = iArr[i2];
        if (asLong2 != null && asLong2.longValue() >= 0) {
            builderNewUpdate.withValue("raw_contact_id2", asLong2);
        } else {
            if (i4 < 0) {
                return null;
            }
            builderNewUpdate.withValueBackReference("raw_contact_id2", i4);
        }
        return builderNewUpdate;
    }

    public long findRawContactId() {
        Iterator<RawContactDelta> it = iterator();
        while (it.hasNext()) {
            Long asLong = it.next().getValues().getAsLong("_id");
            if (asLong != null && asLong.longValue() >= 0) {
                return asLong.longValue();
            }
        }
        return -1L;
    }

    public Long getRawContactId(int i) {
        if (i >= 0 && i < size()) {
            ValuesDelta values = get(i).getValues();
            if (values.isVisible()) {
                return values.getAsLong("_id");
            }
            return null;
        }
        return null;
    }

    public RawContactDelta getByRawContactId(Long l) {
        int iIndexOfRawContactId = indexOfRawContactId(l);
        if (iIndexOfRawContactId == -1) {
            return null;
        }
        return get(iIndexOfRawContactId);
    }

    public int indexOfRawContactId(Long l) {
        if (l == null) {
            return -1;
        }
        int size = size();
        for (int i = 0; i < size; i++) {
            if (l.equals(getRawContactId(i))) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfFirstWritableRawContact(Context context) {
        Iterator<RawContactDelta> it = iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().getRawContactAccountType(context).areContactsWritable()) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public RawContactDelta getFirstWritableRawContact(Context context) {
        int iIndexOfFirstWritableRawContact = indexOfFirstWritableRawContact(context);
        if (iIndexOfFirstWritableRawContact == -1) {
            return null;
        }
        return get(iIndexOfFirstWritableRawContact);
    }

    public ValuesDelta getSuperPrimaryEntry(String str) {
        Iterator<RawContactDelta> it = iterator();
        ValuesDelta valuesDelta = null;
        ValuesDelta valuesDelta2 = null;
        while (it.hasNext()) {
            ArrayList<ValuesDelta> mimeEntries = it.next().getMimeEntries(str);
            if (mimeEntries == null) {
                return null;
            }
            for (ValuesDelta valuesDelta3 : mimeEntries) {
                if (valuesDelta3.isSuperPrimary()) {
                    return valuesDelta3;
                }
                if (valuesDelta != null || !valuesDelta3.isPrimary()) {
                    if (valuesDelta2 == null) {
                        valuesDelta2 = valuesDelta3;
                    }
                } else {
                    valuesDelta = valuesDelta3;
                }
            }
        }
        if (valuesDelta != null) {
            return valuesDelta;
        }
        return valuesDelta2;
    }

    public void markRawContactsForSplitting() {
        this.mSplitRawContacts = true;
    }

    public boolean isMarkedForSplitting() {
        return this.mSplitRawContacts;
    }

    public void setJoinWithRawContacts(long[] jArr) {
        this.mJoinWithRawContactIds = jArr;
    }

    public boolean isMarkedForJoining() {
        return this.mJoinWithRawContactIds != null && this.mJoinWithRawContactIds.length > 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(size());
        Iterator<RawContactDelta> it = iterator();
        while (it.hasNext()) {
            parcel.writeParcelable(it.next(), i);
        }
        parcel.writeLongArray(this.mJoinWithRawContactIds);
        parcel.writeInt(this.mSplitRawContacts ? 1 : 0);
    }

    public void readFromParcel(Parcel parcel) {
        ClassLoader classLoader = getClass().getClassLoader();
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            add((RawContactDelta) parcel.readParcelable(classLoader));
        }
        this.mJoinWithRawContactIds = parcel.createLongArray();
        this.mSplitRawContacts = parcel.readInt() != 0;
    }

    @Override
    public String toString() {
        return "(Split=" + this.mSplitRawContacts + ", Join=[" + Arrays.toString(this.mJoinWithRawContactIds) + "], Values=" + super.toString() + ")";
    }
}
