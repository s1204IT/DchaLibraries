package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;
import java.util.ArrayList;
import java.util.Map;

public class DataRowHandlerForGroupMembership extends DataRowHandler {
    private final Map<String, ArrayList<ContactsProvider2.GroupIdCacheEntry>> mGroupIdCache;

    interface RawContactsQuery {
        public static final String[] COLUMNS = {"deleted", "account_id"};
    }

    public DataRowHandlerForGroupMembership(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator, Map<String, ArrayList<ContactsProvider2.GroupIdCacheEntry>> map) {
        super(context, contactsDatabaseHelper, abstractContactAggregator, "vnd.android.cursor.item/group_membership");
        this.mGroupIdCache = map;
    }

    @Override
    public long insert(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, long j, ContentValues contentValues) {
        resolveGroupSourceIdInValues(transactionContext, j, sQLiteDatabase, contentValues, true);
        long jInsert = super.insert(sQLiteDatabase, transactionContext, j, contentValues);
        if (hasFavoritesGroupMembership(sQLiteDatabase, j)) {
            updateRawContactsStar(sQLiteDatabase, j, true);
        }
        updateVisibility(transactionContext, j);
        return jInsert;
    }

    @Override
    public boolean update(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, ContentValues contentValues, Cursor cursor, boolean z, boolean z2) {
        long j = cursor.getLong(1);
        boolean zHasFavoritesGroupMembership = hasFavoritesGroupMembership(sQLiteDatabase, j);
        resolveGroupSourceIdInValues(transactionContext, j, sQLiteDatabase, contentValues, false);
        if (!super.update(sQLiteDatabase, transactionContext, contentValues, cursor, z, z2)) {
            return false;
        }
        boolean zHasFavoritesGroupMembership2 = hasFavoritesGroupMembership(sQLiteDatabase, j);
        if (zHasFavoritesGroupMembership != zHasFavoritesGroupMembership2) {
            updateRawContactsStar(sQLiteDatabase, j, zHasFavoritesGroupMembership2);
        }
        updateVisibility(transactionContext, j);
        return true;
    }

    private void updateRawContactsStar(SQLiteDatabase sQLiteDatabase, long j, boolean z) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("starred", Integer.valueOf(z ? 1 : 0));
        if (sQLiteDatabase.update("raw_contacts", contentValues, "_id=?", new String[]{Long.toString(j)}) > 0) {
            this.mContactAggregator.updateStarred(j);
        }
    }

    private boolean hasFavoritesGroupMembership(SQLiteDatabase sQLiteDatabase, long j) {
        return 0 < DatabaseUtils.longForQuery(sQLiteDatabase, "SELECT COUNT(*) FROM data LEFT OUTER JOIN groups ON data.data1=groups._id WHERE mimetype_id=? AND data.raw_contact_id=? AND favorites!=0", new String[]{Long.toString(this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/group_membership")), Long.toString(j)});
    }

    @Override
    public int delete(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, Cursor cursor) {
        long j = cursor.getLong(2);
        boolean zHasFavoritesGroupMembership = hasFavoritesGroupMembership(sQLiteDatabase, j);
        int iDelete = super.delete(sQLiteDatabase, transactionContext, cursor);
        boolean zHasFavoritesGroupMembership2 = hasFavoritesGroupMembership(sQLiteDatabase, j);
        if (zHasFavoritesGroupMembership && !zHasFavoritesGroupMembership2) {
            updateRawContactsStar(sQLiteDatabase, j, false);
        }
        updateVisibility(transactionContext, j);
        return iDelete;
    }

    private void updateVisibility(TransactionContext transactionContext, long j) {
        long contactId = this.mDbHelper.getContactId(j);
        if (contactId != 0 && this.mDbHelper.updateContactVisibleOnlyIfChanged(transactionContext, contactId)) {
            this.mContactAggregator.updateAggregationAfterVisibilityChange(contactId);
        }
    }

    private void resolveGroupSourceIdInValues(TransactionContext transactionContext, long j, SQLiteDatabase sQLiteDatabase, ContentValues contentValues, boolean z) {
        boolean zContainsKey = contentValues.containsKey("group_sourceid");
        boolean zContainsKey2 = contentValues.containsKey("data1");
        if (zContainsKey && zContainsKey2) {
            throw new IllegalArgumentException("you are not allowed to set both the GroupMembership.GROUP_SOURCE_ID and GroupMembership.GROUP_ROW_ID");
        }
        if (!zContainsKey && !zContainsKey2) {
            if (z) {
                throw new IllegalArgumentException("you must set exactly one of GroupMembership.GROUP_SOURCE_ID and GroupMembership.GROUP_ROW_ID");
            }
        } else if (zContainsKey) {
            long orMakeGroup = getOrMakeGroup(sQLiteDatabase, j, contentValues.getAsString("group_sourceid"), transactionContext.getAccountIdOrNullForRawContact(j));
            contentValues.remove("group_sourceid");
            contentValues.put("data1", Long.valueOf(orMakeGroup));
        }
    }

    private long getOrMakeGroup(SQLiteDatabase sQLiteDatabase, long j, String str, Long l) {
        Long lValueOf;
        Cursor cursorQuery;
        if (l == null) {
            this.mSelectionArgs1[0] = String.valueOf(j);
            cursorQuery = sQLiteDatabase.query("raw_contacts", RawContactsQuery.COLUMNS, "raw_contacts._id=?", this.mSelectionArgs1, null, null, null);
            try {
                if (cursorQuery.moveToFirst()) {
                    lValueOf = Long.valueOf(cursorQuery.getLong(1));
                } else {
                    lValueOf = l;
                }
            } finally {
            }
        } else {
            lValueOf = l;
        }
        if (lValueOf == null) {
            throw new IllegalArgumentException("Raw contact not found for _ID=" + j);
        }
        long jLongValue = lValueOf.longValue();
        ArrayList<ContactsProvider2.GroupIdCacheEntry> arrayList = this.mGroupIdCache.get(str);
        if (arrayList == null) {
            arrayList = new ArrayList<>(1);
            this.mGroupIdCache.put(str, arrayList);
        }
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            ContactsProvider2.GroupIdCacheEntry groupIdCacheEntry = arrayList.get(i);
            if (groupIdCacheEntry.accountId == jLongValue) {
                return groupIdCacheEntry.groupId;
            }
        }
        ContactsProvider2.GroupIdCacheEntry groupIdCacheEntry2 = new ContactsProvider2.GroupIdCacheEntry();
        groupIdCacheEntry2.accountId = jLongValue;
        groupIdCacheEntry2.sourceId = str;
        arrayList.add(0, groupIdCacheEntry2);
        cursorQuery = sQLiteDatabase.query("groups", ContactsDatabaseHelper.Projections.ID, "sourceid=? AND account_id=?", new String[]{str, Long.toString(jLongValue)}, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                groupIdCacheEntry2.groupId = cursorQuery.getLong(0);
            } else {
                ContentValues contentValues = new ContentValues();
                contentValues.put("account_id", Long.valueOf(jLongValue));
                contentValues.put("sourceid", str);
                long jInsert = sQLiteDatabase.insert("groups", null, contentValues);
                if (jInsert < 0) {
                    throw new IllegalStateException("unable to create a new group with this sourceid: " + contentValues);
                }
                groupIdCacheEntry2.groupId = jInsert;
            }
            cursorQuery.close();
            return groupIdCacheEntry2.groupId;
        } finally {
        }
    }
}
