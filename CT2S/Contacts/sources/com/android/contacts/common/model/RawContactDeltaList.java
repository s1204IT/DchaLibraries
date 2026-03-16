package com.android.contacts.common.model;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Entity;
import android.content.EntityIterator;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.Log;
import com.google.common.collect.Lists;
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
        public RawContactDeltaList createFromParcel(Parcel in) {
            RawContactDeltaList state = new RawContactDeltaList();
            state.readFromParcel(in);
            return state;
        }

        @Override
        public RawContactDeltaList[] newArray(int size) {
            return new RawContactDeltaList[size];
        }
    };

    public static RawContactDeltaList fromQuery(Uri entityUri, ContentResolver resolver, String selection, String[] selectionArgs, String sortOrder) {
        EntityIterator iterator = ContactsContract.RawContacts.newEntityIterator(resolver.query(entityUri, null, selection, selectionArgs, sortOrder));
        try {
            return fromIterator(iterator);
        } finally {
            iterator.close();
        }
    }

    public static RawContactDeltaList fromIterator(Iterator<?> iterator) {
        RawContactDeltaList state = new RawContactDeltaList();
        state.addAll(iterator);
        return state;
    }

    public void addAll(Iterator<?> iterator) {
        while (iterator.hasNext()) {
            Object nextObject = iterator.next();
            RawContact before = nextObject instanceof Entity ? RawContact.createFrom((Entity) nextObject) : (RawContact) nextObject;
            RawContactDelta rawContactDelta = RawContactDelta.fromBefore(before);
            add(rawContactDelta);
        }
    }

    public static RawContactDeltaList mergeAfter(RawContactDeltaList local, RawContactDeltaList remote) {
        if (local == null) {
            local = new RawContactDeltaList();
        }
        for (RawContactDelta remoteEntity : remote) {
            Long rawContactId = remoteEntity.getValues().getId();
            RawContactDelta localEntity = local.getByRawContactId(rawContactId);
            RawContactDelta merged = RawContactDelta.mergeAfter(localEntity, remoteEntity);
            if (localEntity == null && merged != null) {
                local.add(merged);
            }
        }
        return local;
    }

    public ArrayList<ContentProviderOperation> buildDiff() {
        if (VERBOSE_LOGGING) {
            Log.v(TAG, "buildDiff: list=" + toString());
        }
        ArrayList<ContentProviderOperation> diff = Lists.newArrayList();
        long rawContactId = findRawContactId();
        int firstInsertRow = -1;
        Iterator<RawContactDelta> it = iterator();
        while (it.hasNext()) {
            it.next().buildAssert(diff);
        }
        int assertMark = diff.size();
        int[] backRefs = new int[size()];
        int rawContactIndex = 0;
        for (RawContactDelta delta : this) {
            int firstBatch = diff.size();
            boolean isInsert = delta.isContactInsert();
            int rawContactIndex2 = rawContactIndex + 1;
            backRefs[rawContactIndex] = isInsert ? firstBatch : -1;
            delta.buildDiff(diff);
            if (this.mJoinWithRawContactIds != null) {
                long[] arr$ = this.mJoinWithRawContactIds;
                for (long j : arr$) {
                    Long joinedRawContactId = Long.valueOf(j);
                    ContentProviderOperation.Builder builder = beginKeepTogether();
                    builder.withValue("raw_contact_id1", joinedRawContactId);
                    if (rawContactId != -1) {
                        builder.withValue("raw_contact_id2", Long.valueOf(rawContactId));
                    } else {
                        builder.withValueBackReference("raw_contact_id2", firstBatch);
                    }
                    diff.add(builder.build());
                }
            }
            if (!isInsert) {
                rawContactIndex = rawContactIndex2;
            } else if (this.mSplitRawContacts) {
                rawContactIndex = rawContactIndex2;
            } else {
                if (rawContactId != -1) {
                    ContentProviderOperation.Builder builder2 = beginKeepTogether();
                    builder2.withValue("raw_contact_id1", Long.valueOf(rawContactId));
                    builder2.withValueBackReference("raw_contact_id2", firstBatch);
                    diff.add(builder2.build());
                } else if (firstInsertRow == -1) {
                    firstInsertRow = firstBatch;
                } else {
                    ContentProviderOperation.Builder builder3 = beginKeepTogether();
                    builder3.withValueBackReference("raw_contact_id1", firstInsertRow);
                    builder3.withValueBackReference("raw_contact_id2", firstBatch);
                    diff.add(builder3.build());
                }
                rawContactIndex = rawContactIndex2;
            }
        }
        if (this.mSplitRawContacts) {
            buildSplitContactDiff(diff, backRefs);
        }
        if (diff.size() == assertMark) {
            diff.clear();
        }
        if (VERBOSE_LOGGING) {
            Log.v(TAG, "buildDiff: ops=" + diffToString(diff));
        }
        return diff;
    }

    private static String diffToString(ArrayList<ContentProviderOperation> ops) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (ContentProviderOperation op : ops) {
            sb.append(op.toString());
            sb.append(",\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    protected ContentProviderOperation.Builder beginKeepTogether() {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI);
        builder.withValue("type", 1);
        return builder;
    }

    private void buildSplitContactDiff(ArrayList<ContentProviderOperation> diff, int[] backRefs) {
        int count = size();
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                if (i != j) {
                    buildSplitContactDiff(diff, i, j, backRefs);
                }
            }
        }
    }

    private void buildSplitContactDiff(ArrayList<ContentProviderOperation> diff, int index1, int index2, int[] backRefs) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI);
        builder.withValue("type", 2);
        Long rawContactId1 = get(index1).getValues().getAsLong("_id");
        int backRef1 = backRefs[index1];
        if (rawContactId1 != null && rawContactId1.longValue() >= 0) {
            builder.withValue("raw_contact_id1", rawContactId1);
        } else if (backRef1 >= 0) {
            builder.withValueBackReference("raw_contact_id1", backRef1);
        } else {
            return;
        }
        Long rawContactId2 = get(index2).getValues().getAsLong("_id");
        int backRef2 = backRefs[index2];
        if (rawContactId2 != null && rawContactId2.longValue() >= 0) {
            builder.withValue("raw_contact_id2", rawContactId2);
        } else if (backRef2 >= 0) {
            builder.withValueBackReference("raw_contact_id2", backRef2);
        } else {
            return;
        }
        diff.add(builder.build());
    }

    public long findRawContactId() {
        for (RawContactDelta delta : this) {
            Long rawContactId = delta.getValues().getAsLong("_id");
            if (rawContactId != null && rawContactId.longValue() >= 0) {
                return rawContactId.longValue();
            }
        }
        return -1L;
    }

    public Long getRawContactId(int index) {
        if (index >= 0 && index < size()) {
            RawContactDelta delta = get(index);
            ValuesDelta values = delta.getValues();
            if (values.isVisible()) {
                return values.getAsLong("_id");
            }
        }
        return null;
    }

    public RawContactDelta getByRawContactId(Long rawContactId) {
        int index = indexOfRawContactId(rawContactId);
        if (index == -1) {
            return null;
        }
        return get(index);
    }

    public int indexOfRawContactId(Long rawContactId) {
        if (rawContactId == null) {
            return -1;
        }
        int size = size();
        for (int i = 0; i < size; i++) {
            Long currentId = getRawContactId(i);
            if (rawContactId.equals(currentId)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfFirstWritableRawContact(Context context) {
        int entityIndex = 0;
        for (RawContactDelta delta : this) {
            if (!delta.getRawContactAccountType(context).areContactsWritable()) {
                entityIndex++;
            } else {
                return entityIndex;
            }
        }
        return -1;
    }

    public RawContactDelta getFirstWritableRawContact(Context context) {
        int index = indexOfFirstWritableRawContact(context);
        if (index == -1) {
            return null;
        }
        return get(index);
    }

    public void markRawContactsForSplitting() {
        this.mSplitRawContacts = true;
    }

    public boolean isMarkedForSplitting() {
        return this.mSplitRawContacts;
    }

    public void setJoinWithRawContacts(long[] rawContactIds) {
        this.mJoinWithRawContactIds = rawContactIds;
    }

    public boolean isMarkedForJoining() {
        return this.mJoinWithRawContactIds != null && this.mJoinWithRawContactIds.length > 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        int size = size();
        dest.writeInt(size);
        for (RawContactDelta delta : this) {
            dest.writeParcelable(delta, flags);
        }
        dest.writeLongArray(this.mJoinWithRawContactIds);
        dest.writeInt(this.mSplitRawContacts ? 1 : 0);
    }

    public void readFromParcel(Parcel source) {
        ClassLoader loader = getClass().getClassLoader();
        int size = source.readInt();
        for (int i = 0; i < size; i++) {
            add(source.readParcelable(loader));
        }
        this.mJoinWithRawContactIds = source.createLongArray();
        this.mSplitRawContacts = source.readInt() != 0;
    }

    @Override
    public String toString() {
        return "(Split=" + this.mSplitRawContacts + ", Join=[" + Arrays.toString(this.mJoinWithRawContactIds) + "], Values=" + super.toString() + ")";
    }
}
