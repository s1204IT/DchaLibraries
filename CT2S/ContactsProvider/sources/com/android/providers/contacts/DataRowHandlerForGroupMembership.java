package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.aggregation.ContactAggregator;
import java.util.ArrayList;
import java.util.HashMap;

public class DataRowHandlerForGroupMembership extends DataRowHandler {
    private final HashMap<String, ArrayList<ContactsProvider2.GroupIdCacheEntry>> mGroupIdCache;

    interface RawContactsQuery {
        public static final String[] COLUMNS = {"deleted", "account_id"};
    }

    public DataRowHandlerForGroupMembership(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator, HashMap<String, ArrayList<ContactsProvider2.GroupIdCacheEntry>> groupIdCache) {
        super(context, dbHelper, aggregator, "vnd.android.cursor.item/group_membership");
        this.mGroupIdCache = groupIdCache;
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId, ContentValues values) {
        resolveGroupSourceIdInValues(txContext, rawContactId, db, values, true);
        long dataId = super.insert(db, txContext, rawContactId, values);
        if (hasFavoritesGroupMembership(db, rawContactId)) {
            updateRawContactsStar(db, rawContactId, true);
        }
        updateVisibility(txContext, rawContactId);
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        long rawContactId = c.getLong(1);
        boolean wasStarred = hasFavoritesGroupMembership(db, rawContactId);
        resolveGroupSourceIdInValues(txContext, rawContactId, db, values, false);
        if (!super.update(db, txContext, values, c, callerIsSyncAdapter)) {
            return false;
        }
        boolean isStarred = hasFavoritesGroupMembership(db, rawContactId);
        if (wasStarred != isStarred) {
            updateRawContactsStar(db, rawContactId, isStarred);
        }
        updateVisibility(txContext, rawContactId);
        return true;
    }

    private void updateRawContactsStar(SQLiteDatabase db, long rawContactId, boolean starred) {
        ContentValues rawContactValues = new ContentValues();
        rawContactValues.put("starred", Integer.valueOf(starred ? 1 : 0));
        if (db.update("raw_contacts", rawContactValues, "_id=?", new String[]{Long.toString(rawContactId)}) > 0) {
            this.mContactAggregator.updateStarred(rawContactId);
        }
    }

    private boolean hasFavoritesGroupMembership(SQLiteDatabase db, long rawContactId) {
        long groupMembershipMimetypeId = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/group_membership");
        return 0 < DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM data LEFT OUTER JOIN groups ON data.data1=groups._id WHERE mimetype_id=? AND data.raw_contact_id=? AND favorites!=0", new String[]{Long.toString(groupMembershipMimetypeId), Long.toString(rawContactId)});
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long rawContactId = c.getLong(2);
        boolean wasStarred = hasFavoritesGroupMembership(db, rawContactId);
        int count = super.delete(db, txContext, c);
        boolean isStarred = hasFavoritesGroupMembership(db, rawContactId);
        if (wasStarred && !isStarred) {
            updateRawContactsStar(db, rawContactId, false);
        }
        updateVisibility(txContext, rawContactId);
        return count;
    }

    private void updateVisibility(TransactionContext txContext, long rawContactId) {
        long contactId = this.mDbHelper.getContactId(rawContactId);
        if (contactId != 0 && this.mDbHelper.updateContactVisibleOnlyIfChanged(txContext, contactId)) {
            this.mContactAggregator.updateAggregationAfterVisibilityChange(contactId);
        }
    }

    private void resolveGroupSourceIdInValues(TransactionContext txContext, long rawContactId, SQLiteDatabase db, ContentValues values, boolean isInsert) {
        boolean containsGroupSourceId = values.containsKey("group_sourceid");
        boolean containsGroupId = values.containsKey("data1");
        if (containsGroupSourceId && containsGroupId) {
            throw new IllegalArgumentException("you are not allowed to set both the GroupMembership.GROUP_SOURCE_ID and GroupMembership.GROUP_ROW_ID");
        }
        if (!containsGroupSourceId && !containsGroupId) {
            if (isInsert) {
                throw new IllegalArgumentException("you must set exactly one of GroupMembership.GROUP_SOURCE_ID and GroupMembership.GROUP_ROW_ID");
            }
        } else if (containsGroupSourceId) {
            String sourceId = values.getAsString("group_sourceid");
            long groupId = getOrMakeGroup(db, rawContactId, sourceId, txContext.getAccountIdOrNullForRawContact(rawContactId));
            values.remove("group_sourceid");
            values.put("data1", Long.valueOf(groupId));
        }
    }

    private long getOrMakeGroup(SQLiteDatabase db, long rawContactId, String sourceId, Long accountIdOrNull) {
        Cursor c;
        if (accountIdOrNull == null) {
            this.mSelectionArgs1[0] = String.valueOf(rawContactId);
            c = db.query("raw_contacts", RawContactsQuery.COLUMNS, "raw_contacts._id=?", this.mSelectionArgs1, null, null, null);
            try {
                if (c.moveToFirst()) {
                    accountIdOrNull = Long.valueOf(c.getLong(1));
                }
            } finally {
            }
        }
        if (accountIdOrNull == null) {
            throw new IllegalArgumentException("Raw contact not found for _ID=" + rawContactId);
        }
        long accountId = accountIdOrNull.longValue();
        ArrayList<ContactsProvider2.GroupIdCacheEntry> entries = this.mGroupIdCache.get(sourceId);
        if (entries == null) {
            entries = new ArrayList<>(1);
            this.mGroupIdCache.put(sourceId, entries);
        }
        int count = entries.size();
        for (int i = 0; i < count; i++) {
            ContactsProvider2.GroupIdCacheEntry entry = entries.get(i);
            if (entry.accountId == accountId) {
                return entry.groupId;
            }
        }
        ContactsProvider2.GroupIdCacheEntry entry2 = new ContactsProvider2.GroupIdCacheEntry();
        entry2.accountId = accountId;
        entry2.sourceId = sourceId;
        entries.add(0, entry2);
        c = db.query("groups", ContactsDatabaseHelper.Projections.ID, "sourceid=? AND account_id=?", new String[]{sourceId, Long.toString(accountId)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                entry2.groupId = c.getLong(0);
            } else {
                ContentValues groupValues = new ContentValues();
                groupValues.put("account_id", Long.valueOf(accountId));
                groupValues.put("sourceid", sourceId);
                long groupId = db.insert("groups", null, groupValues);
                if (groupId < 0) {
                    throw new IllegalStateException("unable to create a new group with this sourceid: " + groupValues);
                }
                entry2.groupId = groupId;
            }
            c.close();
            return entry2.groupId;
        } finally {
        }
    }
}
