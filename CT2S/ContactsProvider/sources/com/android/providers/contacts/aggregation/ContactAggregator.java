package com.android.providers.contacts.aggregation;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import com.android.providers.contacts.ContactLookupKey;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.NameLookupBuilder;
import com.android.providers.contacts.NameNormalizer;
import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.PhotoPriorityResolver;
import com.android.providers.contacts.ReorderingCursorWrapper;
import com.android.providers.contacts.TransactionContext;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;
import com.android.providers.contacts.aggregation.util.ContactMatcher;
import com.android.providers.contacts.database.ContactsTableUtil;
import com.android.providers.contacts.util.Clock;
import com.google.android.collect.Maps;
import com.google.android.collect.Sets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ContactAggregator {
    private SQLiteStatement mAggregatedPresenceDelete;
    private SQLiteStatement mAggregatedPresenceReplace;
    private boolean mAggregationExceptionIdsValid;
    private final CommonNicknameCache mCommonNicknameCache;
    private SQLiteStatement mContactIdAndMarkAggregatedUpdate;
    private SQLiteStatement mContactIdUpdate;
    private SQLiteStatement mContactInsert;
    private SQLiteStatement mContactUpdate;
    private final ContactsProvider2 mContactsProvider;
    private final ContactsDatabaseHelper mDbHelper;
    private SQLiteStatement mDisplayNameUpdate;
    private SQLiteStatement mLookupKeyUpdate;
    private SQLiteStatement mMarkAggregatedUpdate;
    private SQLiteStatement mMarkForAggregation;
    private long mMimeTypeIdEmail;
    private long mMimeTypeIdIdentity;
    private long mMimeTypeIdPhone;
    private long mMimeTypeIdPhoto;
    private final NameSplitter mNameSplitter;
    private SQLiteStatement mPhotoIdUpdate;
    private PhotoPriorityResolver mPhotoPriorityResolver;
    private SQLiteStatement mPinnedUpdate;
    private SQLiteStatement mPresenceContactIdUpdate;
    private SQLiteStatement mRawContactCountQuery;
    private String mRawContactsQueryByContactId;
    private String mRawContactsQueryByRawContactId;
    private SQLiteStatement mResetPinnedForRawContact;
    private SQLiteStatement mStarredUpdate;
    private static final boolean DEBUG_LOGGING = Log.isLoggable("ContactAggregator", 3);
    private static final boolean VERBOSE_LOGGING = Log.isLoggable("ContactAggregator", 2);
    private static final String PRIMARY_HIT_LIMIT_STRING = String.valueOf(15);
    private static final String SECONDARY_HIT_LIMIT_STRING = String.valueOf(20);
    private boolean mEnabled = true;
    private HashMap<Long, Integer> mRawContactsMarkedForAggregation = Maps.newHashMap();
    private String[] mSelectionArgs1 = new String[1];
    private String[] mSelectionArgs2 = new String[2];
    private StringBuilder mSb = new StringBuilder();
    private MatchCandidateList mCandidates = new MatchCandidateList();
    private ContactMatcher mMatcher = new ContactMatcher();
    private DisplayNameCandidate mDisplayNameCandidate = new DisplayNameCandidate();
    private final HashSet<Long> mAggregationExceptionIds = new HashSet<>();

    interface AggregateExceptionPrefetchQuery {
        public static final String[] COLUMNS = {"raw_contact_id1", "raw_contact_id2"};
    }

    interface AggregateExceptionQuery {
        public static final String[] COLUMNS = {"type", "raw_contact_id1", "raw_contacts1.contact_id", "raw_contacts1.aggregation_needed", "raw_contacts2.contact_id", "raw_contacts2.aggregation_needed"};
    }

    private interface ContactIdQuery {
        public static final String[] COLUMNS = {"_id"};
    }

    private interface ContactNameLookupQuery {
        public static final String[] COLUMNS = {"contact_id", "normalized_name", "name_type"};
    }

    private interface DisplayNameQuery {
        public static final String[] COLUMNS = {"_id", "display_name", "display_name_source", "name_verified", "sourceid", "account_type_and_data_set"};
    }

    private interface EmailLookupQuery {
        public static final String[] COLUMNS = {"contact_id"};
    }

    private interface IdentityLookupMatchQuery {
        public static final String[] COLUMNS = {"contact_id"};
    }

    private interface LookupKeyQuery {
        public static final String[] COLUMNS = {"_id", "display_name", "account_type_and_data_set", "account_name", "sourceid"};
    }

    private interface NameLookupMatchQuery {
        public static final String[] COLUMNS = {"contact_id", "nameA.normalized_name", "nameA.name_type", "nameB.name_type"};
    }

    private interface NameLookupMatchQueryWithParameter {
        public static final String[] COLUMNS = {"contact_id", "normalized_name", "name_type"};
    }

    private interface NameLookupQuery {
        public static final String[] COLUMNS = {"normalized_name", "name_type"};
    }

    private interface PhoneLookupQuery {
        public static final String[] COLUMNS = {"contact_id"};
    }

    private interface PhotoFileQuery {
        public static final String[] COLUMNS = {"height", "width", "filesize"};
    }

    private interface PhotoIdQuery {
        public static final String[] COLUMNS = {"accounts.account_type", "data._id", "is_super_primary", "data14"};
    }

    private static final class RawContactIdAndAccountQuery {
        public static final String[] COLUMNS = {"contact_id", "account_id"};
    }

    private static class RawContactIdAndAggregationModeQuery {
        public static final String[] COLUMNS = {"_id", "aggregation_mode"};
    }

    private static class RawContactIdQuery {
        public static final String[] COLUMNS = {"_id"};
    }

    public static final class AggregationSuggestionParameter {
        public final String kind;
        public final String value;

        public AggregationSuggestionParameter(String kind, String value) {
            this.kind = kind;
            this.value = value;
        }
    }

    private static class NameMatchCandidate {
        int mLookupType;
        String mName;

        public NameMatchCandidate(String name, int nameLookupType) {
            this.mName = name;
            this.mLookupType = nameLookupType;
        }
    }

    private static class MatchCandidateList {
        private int mCount;
        private final ArrayList<NameMatchCandidate> mList;

        private MatchCandidateList() {
            this.mList = new ArrayList<>();
        }

        public void add(String name, int nameLookupType) {
            if (this.mCount >= this.mList.size()) {
                this.mList.add(new NameMatchCandidate(name, nameLookupType));
            } else {
                NameMatchCandidate candidate = this.mList.get(this.mCount);
                candidate.mName = name;
                candidate.mLookupType = nameLookupType;
            }
            this.mCount++;
        }

        public void clear() {
            this.mCount = 0;
        }

        public boolean isEmpty() {
            return this.mCount == 0;
        }
    }

    private static class DisplayNameCandidate {
        String displayName;
        int displayNameSource;
        long rawContactId;
        boolean verified;
        boolean writableAccount;

        public DisplayNameCandidate() {
            clear();
        }

        public void clear() {
            this.rawContactId = -1L;
            this.displayName = null;
            this.displayNameSource = 0;
            this.verified = false;
            this.writableAccount = false;
        }
    }

    public ContactAggregator(ContactsProvider2 contactsProvider, ContactsDatabaseHelper contactsDatabaseHelper, PhotoPriorityResolver photoPriorityResolver, NameSplitter nameSplitter, CommonNicknameCache commonNicknameCache) {
        this.mContactsProvider = contactsProvider;
        this.mDbHelper = contactsDatabaseHelper;
        this.mPhotoPriorityResolver = photoPriorityResolver;
        this.mNameSplitter = nameSplitter;
        this.mCommonNicknameCache = commonNicknameCache;
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        this.mAggregatedPresenceReplace = db.compileStatement("INSERT OR REPLACE INTO agg_presence(presence_contact_id, mode, chat_capability) SELECT presence_contact_id,mode,chat_capability FROM presence WHERE  (mode * 10 + chat_capability) = (SELECT MAX (mode * 10 + chat_capability) FROM presence WHERE presence_contact_id=?) AND presence_contact_id=?;");
        this.mRawContactCountQuery = db.compileStatement("SELECT COUNT(_id) FROM raw_contacts WHERE contact_id=? AND _id<>?");
        this.mAggregatedPresenceDelete = db.compileStatement("DELETE FROM agg_presence WHERE presence_contact_id=?");
        this.mMarkForAggregation = db.compileStatement("UPDATE raw_contacts SET aggregation_needed=1 WHERE _id=? AND aggregation_needed=0");
        this.mPhotoIdUpdate = db.compileStatement("UPDATE contacts SET photo_id=?,photo_file_id=?  WHERE _id=?");
        this.mDisplayNameUpdate = db.compileStatement("UPDATE contacts SET name_raw_contact_id=?  WHERE _id=?");
        this.mLookupKeyUpdate = db.compileStatement("UPDATE contacts SET lookup=?  WHERE _id=?");
        this.mStarredUpdate = db.compileStatement("UPDATE contacts SET starred=(SELECT (CASE WHEN COUNT(starred)=0 THEN 0 ELSE 1 END) FROM raw_contacts WHERE contact_id=contacts._id AND starred=1) WHERE _id=?");
        this.mPinnedUpdate = db.compileStatement("UPDATE contacts SET pinned = IFNULL((SELECT MIN(pinned) FROM raw_contacts WHERE contact_id=contacts._id AND pinned>0),0) WHERE _id=?");
        this.mContactIdAndMarkAggregatedUpdate = db.compileStatement("UPDATE raw_contacts SET contact_id=?, aggregation_needed=0 WHERE _id=?");
        this.mContactIdUpdate = db.compileStatement("UPDATE raw_contacts SET contact_id=? WHERE _id=?");
        this.mMarkAggregatedUpdate = db.compileStatement("UPDATE raw_contacts SET aggregation_needed=0 WHERE _id=?");
        this.mPresenceContactIdUpdate = db.compileStatement("UPDATE presence SET presence_contact_id=? WHERE presence_raw_contact_id=?");
        this.mContactUpdate = db.compileStatement("UPDATE contacts SET name_raw_contact_id=?, photo_id=?, photo_file_id=?, send_to_voicemail=?, custom_ringtone=?, last_time_contacted=?, times_contacted=?, starred=?, pinned=?, has_phone_number=?, lookup=?, contact_last_updated_timestamp=?  WHERE _id=?");
        this.mContactInsert = db.compileStatement("INSERT INTO contacts (name_raw_contact_id, photo_id, photo_file_id, send_to_voicemail, custom_ringtone, last_time_contacted, times_contacted, starred, pinned, has_phone_number, lookup, contact_last_updated_timestamp)  VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
        this.mResetPinnedForRawContact = db.compileStatement("UPDATE raw_contacts SET pinned=0 WHERE _id=?");
        this.mMimeTypeIdEmail = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/email_v2");
        this.mMimeTypeIdIdentity = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/identity");
        this.mMimeTypeIdPhoto = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/photo");
        this.mMimeTypeIdPhone = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/phone_v2");
        this.mRawContactsQueryByRawContactId = String.format(Locale.US, "SELECT raw_contacts._id,display_name,display_name_source,accounts.account_type,accounts.account_name,accounts.data_set,sourceid,custom_ringtone,send_to_voicemail,last_time_contacted,times_contacted,starred,pinned,name_verified,data._id,data.mimetype_id,is_super_primary,data14 FROM raw_contacts JOIN accounts ON (accounts._id=raw_contacts.account_id) LEFT OUTER JOIN data ON (data.raw_contact_id=raw_contacts._id AND ((mimetype_id=%d AND data15 NOT NULL) OR (mimetype_id=%d AND data1 NOT NULL))) WHERE raw_contacts._id=?", Long.valueOf(this.mMimeTypeIdPhoto), Long.valueOf(this.mMimeTypeIdPhone));
        this.mRawContactsQueryByContactId = String.format(Locale.US, "SELECT raw_contacts._id,display_name,display_name_source,accounts.account_type,accounts.account_name,accounts.data_set,sourceid,custom_ringtone,send_to_voicemail,last_time_contacted,times_contacted,starred,pinned,name_verified,data._id,data.mimetype_id,is_super_primary,data14 FROM raw_contacts JOIN accounts ON (accounts._id=raw_contacts.account_id) LEFT OUTER JOIN data ON (data.raw_contact_id=raw_contacts._id AND ((mimetype_id=%d AND data15 NOT NULL) OR (mimetype_id=%d AND data1 NOT NULL))) WHERE contact_id=? AND deleted=0", Long.valueOf(this.mMimeTypeIdPhoto), Long.valueOf(this.mMimeTypeIdPhone));
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public void aggregateInTransaction(TransactionContext txContext, SQLiteDatabase db) {
        int markedCount = this.mRawContactsMarkedForAggregation.size();
        if (markedCount != 0) {
            long start = System.currentTimeMillis();
            if (DEBUG_LOGGING) {
                Log.d("ContactAggregator", "aggregateInTransaction for " + markedCount + " contacts");
            }
            EventLog.writeEvent(2747, Long.valueOf(start), Integer.valueOf(-markedCount));
            int index = 0;
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append("SELECT _id,contact_id, account_id FROM raw_contacts WHERE _id IN(");
            Iterator<Long> it = this.mRawContactsMarkedForAggregation.keySet().iterator();
            while (it.hasNext()) {
                long rawContactId = it.next().longValue();
                if (index > 0) {
                    sbQuery.append(',');
                }
                sbQuery.append(rawContactId);
                index++;
            }
            sbQuery.append(')');
            Cursor c = db.rawQuery(sbQuery.toString(), null);
            try {
                int actualCount = c.getCount();
                long[] rawContactIds = new long[actualCount];
                long[] contactIds = new long[actualCount];
                long[] accountIds = new long[actualCount];
                int index2 = 0;
                while (c.moveToNext()) {
                    rawContactIds[index2] = c.getLong(0);
                    contactIds[index2] = c.getLong(1);
                    accountIds[index2] = c.getLong(2);
                    index2++;
                }
                c.close();
                if (DEBUG_LOGGING) {
                    Log.d("ContactAggregator", "aggregateInTransaction: initial query done.");
                }
                for (int i = 0; i < actualCount; i++) {
                    aggregateContact(txContext, db, rawContactIds[i], accountIds[i], contactIds[i], this.mCandidates, this.mMatcher);
                }
                long elapsedTime = System.currentTimeMillis() - start;
                EventLog.writeEvent(2747, Long.valueOf(elapsedTime), Integer.valueOf(actualCount));
                if (DEBUG_LOGGING) {
                    Log.d("ContactAggregator", "Contact aggregation complete: " + actualCount + (actualCount == 0 ? "" : ", " + (elapsedTime / ((long) actualCount)) + " ms per raw contact"));
                }
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
    }

    public void triggerAggregation(TransactionContext txContext, long rawContactId) {
        if (this.mEnabled) {
            int aggregationMode = this.mDbHelper.getAggregationMode(rawContactId);
            switch (aggregationMode) {
                case 0:
                    markForAggregation(rawContactId, aggregationMode, false);
                    break;
                case 1:
                    aggregateContact(txContext, this.mDbHelper.getWritableDatabase(), rawContactId);
                    break;
                case 2:
                    long contactId = this.mDbHelper.getContactId(rawContactId);
                    if (contactId != 0) {
                        updateAggregateData(txContext, contactId);
                    }
                    break;
            }
        }
    }

    public void clearPendingAggregations() {
        this.mRawContactsMarkedForAggregation = Maps.newHashMap();
    }

    public void markNewForAggregation(long rawContactId, int aggregationMode) {
        this.mRawContactsMarkedForAggregation.put(Long.valueOf(rawContactId), Integer.valueOf(aggregationMode));
    }

    public void markForAggregation(long rawContactId, int aggregationMode, boolean force) {
        int effectiveAggregationMode;
        if (!force && this.mRawContactsMarkedForAggregation.containsKey(Long.valueOf(rawContactId))) {
            if (aggregationMode == 0) {
                effectiveAggregationMode = this.mRawContactsMarkedForAggregation.get(Long.valueOf(rawContactId)).intValue();
            } else {
                effectiveAggregationMode = aggregationMode;
            }
        } else {
            this.mMarkForAggregation.bindLong(1, rawContactId);
            this.mMarkForAggregation.execute();
            effectiveAggregationMode = aggregationMode;
        }
        this.mRawContactsMarkedForAggregation.put(Long.valueOf(rawContactId), Integer.valueOf(effectiveAggregationMode));
    }

    private void markContactForAggregation(SQLiteDatabase db, long contactId) {
        this.mSelectionArgs1[0] = String.valueOf(contactId);
        Cursor cursor = db.query("raw_contacts", RawContactIdAndAggregationModeQuery.COLUMNS, "contact_id=?", this.mSelectionArgs1, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                long rawContactId = cursor.getLong(0);
                int aggregationMode = cursor.getInt(1);
                if (aggregationMode == 0) {
                    markForAggregation(rawContactId, aggregationMode, true);
                }
            }
        } finally {
            cursor.close();
        }
    }

    public int markAllVisibleForAggregation(SQLiteDatabase db) {
        long start = System.currentTimeMillis();
        db.execSQL("UPDATE raw_contacts SET aggregation_needed=1 WHERE contact_id IN default_directory AND aggregation_mode=0");
        Cursor cursor = db.rawQuery("SELECT _id FROM raw_contacts WHERE aggregation_needed=1", null);
        try {
            int count = cursor.getCount();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                long rawContactId = cursor.getLong(0);
                this.mRawContactsMarkedForAggregation.put(Long.valueOf(rawContactId), 0);
            }
            cursor.close();
            long end = System.currentTimeMillis();
            Log.i("ContactAggregator", "Marked all visible contacts for aggregation: " + count + " raw contacts, " + (end - start) + " ms");
            return count;
        } catch (Throwable th) {
            cursor.close();
            throw th;
        }
    }

    public long onRawContactInsert(TransactionContext txContext, SQLiteDatabase db, long rawContactId) {
        long contactId = insertContact(db, rawContactId);
        setContactId(rawContactId, contactId);
        this.mDbHelper.updateContactVisible(txContext, contactId);
        return contactId;
    }

    protected long insertContact(SQLiteDatabase db, long rawContactId) {
        this.mSelectionArgs1[0] = String.valueOf(rawContactId);
        computeAggregateData(db, this.mRawContactsQueryByRawContactId, this.mSelectionArgs1, this.mContactInsert);
        return this.mContactInsert.executeInsert();
    }

    public void aggregateContact(TransactionContext txContext, SQLiteDatabase db, long rawContactId) {
        long accountId;
        if (this.mEnabled) {
            MatchCandidateList candidates = new MatchCandidateList();
            ContactMatcher matcher = new ContactMatcher();
            long contactId = 0;
            this.mSelectionArgs1[0] = String.valueOf(rawContactId);
            Cursor cursor = db.query("raw_contacts", RawContactIdAndAccountQuery.COLUMNS, "_id=?", this.mSelectionArgs1, null, null, null);
            try {
                if (!cursor.moveToFirst()) {
                    accountId = 0;
                } else {
                    contactId = cursor.getLong(0);
                    accountId = cursor.getLong(1);
                }
                cursor.close();
                aggregateContact(txContext, db, rawContactId, accountId, contactId, candidates, matcher);
            } catch (Throwable th) {
                cursor.close();
                throw th;
            }
        }
    }

    public void updateAggregateData(TransactionContext txContext, long contactId) {
        if (this.mEnabled) {
            SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
            computeAggregateData(db, contactId, this.mContactUpdate);
            this.mContactUpdate.bindLong(13, contactId);
            this.mContactUpdate.execute();
            this.mDbHelper.updateContactVisible(txContext, contactId);
            updateAggregatedStatusUpdate(contactId);
        }
    }

    private void updateAggregatedStatusUpdate(long contactId) {
        this.mAggregatedPresenceReplace.bindLong(1, contactId);
        this.mAggregatedPresenceReplace.bindLong(2, contactId);
        this.mAggregatedPresenceReplace.execute();
        updateLastStatusUpdateId(contactId);
    }

    public void updateLastStatusUpdateId(long contactId) {
        String contactIdString = String.valueOf(contactId);
        this.mDbHelper.getWritableDatabase().execSQL("UPDATE contacts SET status_update_id=(SELECT data._id FROM status_updates JOIN data   ON (status_update_data_id=data._id) JOIN raw_contacts   ON (data.raw_contact_id=raw_contacts._id) WHERE contact_id=? ORDER BY status_ts DESC,status LIMIT 1) WHERE contacts._id=?", new String[]{contactIdString, contactIdString});
    }

    private synchronized void aggregateContact(TransactionContext txContext, SQLiteDatabase db, long rawContactId, long accountId, long currentContactId, MatchCandidateList candidates, ContactMatcher matcher) {
        long contactId;
        int actionCode;
        if (VERBOSE_LOGGING) {
            Log.v("ContactAggregator", "aggregateContact: rid=" + rawContactId + " cid=" + currentContactId);
        }
        int aggregationMode = 0;
        Integer aggModeObject = this.mRawContactsMarkedForAggregation.remove(Long.valueOf(rawContactId));
        if (aggModeObject != null) {
            aggregationMode = aggModeObject.intValue();
        }
        long contactId2 = -1;
        boolean needReaggregate = false;
        HashSet hashSet = new HashSet();
        HashSet hashSet2 = new HashSet();
        if (aggregationMode == 0) {
            candidates.clear();
            matcher.clear();
            contactId2 = pickBestMatchBasedOnExceptions(db, rawContactId, matcher);
            if (contactId2 == -1) {
                if (currentContactId == 0 || this.mDbHelper.isContactInDefaultDirectory(db, currentContactId)) {
                    long contactId3 = pickBestMatchBasedOnData(db, rawContactId, candidates, matcher);
                    contactId = contactId3;
                } else {
                    contactId = contactId2;
                }
                if (contactId == -1 || contactId == currentContactId) {
                    contactId2 = contactId;
                } else {
                    this.mSelectionArgs2[0] = String.valueOf(contactId);
                    this.mSelectionArgs2[1] = String.valueOf(rawContactId);
                    Cursor rawContactsToAccountsCursor = db.rawQuery("SELECT _id, account_id FROM raw_contacts WHERE contact_id=? AND _id!=?", this.mSelectionArgs2);
                    try {
                        rawContactsToAccountsCursor.moveToPosition(-1);
                        while (rawContactsToAccountsCursor.moveToNext()) {
                            long rcId = rawContactsToAccountsCursor.getLong(0);
                            long rc_accountId = rawContactsToAccountsCursor.getLong(1);
                            if (rc_accountId == accountId) {
                                hashSet.add(Long.valueOf(rcId));
                            } else {
                                hashSet2.add(Long.valueOf(rcId));
                            }
                        }
                        rawContactsToAccountsCursor.close();
                        int totalNumOfRawContactsInCandidate = hashSet.size() + hashSet2.size();
                        if (totalNumOfRawContactsInCandidate >= 50) {
                            if (VERBOSE_LOGGING) {
                                Log.v("ContactAggregator", "Too many raw contacts (" + totalNumOfRawContactsInCandidate + ") in the best matching contact, so skip aggregation");
                            }
                            actionCode = 0;
                        } else {
                            actionCode = canJoinIntoContact(db, rawContactId, hashSet, hashSet2);
                        }
                        if (actionCode == 0) {
                            contactId2 = -1;
                        } else if (actionCode == -1) {
                            needReaggregate = true;
                            contactId2 = contactId;
                        }
                    } catch (Throwable th) {
                        rawContactsToAccountsCursor.close();
                        throw th;
                    }
                }
            }
        } else if (aggregationMode == 3) {
        }
        long currentContactContentsCount = 0;
        if (currentContactId != 0) {
            this.mRawContactCountQuery.bindLong(1, currentContactId);
            this.mRawContactCountQuery.bindLong(2, rawContactId);
            currentContactContentsCount = this.mRawContactCountQuery.simpleQueryForLong();
        }
        if (contactId2 == -1 && currentContactId != 0 && (currentContactContentsCount == 0 || aggregationMode == 2)) {
            contactId2 = currentContactId;
        }
        if (contactId2 == currentContactId) {
            markAggregated(rawContactId);
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "Aggregation unchanged");
            }
        } else if (contactId2 == -1) {
            createContactForRawContacts(db, txContext, Sets.newHashSet(new Long[]{Long.valueOf(rawContactId)}), null);
            if (currentContactContentsCount > 0) {
                updateAggregateData(txContext, currentContactId);
            }
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "create new contact for rid=" + rawContactId);
            }
        } else if (needReaggregate) {
            Set<Long> allRawContactIdSet = new HashSet<>();
            allRawContactIdSet.addAll(hashSet);
            allRawContactIdSet.addAll(hashSet2);
            if (currentContactId == 0 || currentContactContentsCount != 0) {
                currentContactId = 0;
            }
            reAggregateRawContacts(txContext, db, contactId2, currentContactId, rawContactId, allRawContactIdSet);
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "Re-aggregating rid=" + rawContactId + " and cid=" + contactId2);
            }
        } else {
            if (currentContactContentsCount == 0) {
                ContactsTableUtil.deleteContact(db, currentContactId);
                this.mAggregatedPresenceDelete.bindLong(1, currentContactId);
                this.mAggregatedPresenceDelete.execute();
            }
            clearSuperPrimarySetting(db, contactId2, rawContactId);
            setContactIdAndMarkAggregated(rawContactId, contactId2);
            computeAggregateData(db, contactId2, this.mContactUpdate);
            this.mContactUpdate.bindLong(13, contactId2);
            this.mContactUpdate.execute();
            this.mDbHelper.updateContactVisible(txContext, contactId2);
            updateAggregatedStatusUpdate(contactId2);
            if (currentContactId != 0) {
                updateAggregateData(txContext, currentContactId);
            }
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "Join rid=" + rawContactId + " with cid=" + contactId2);
            }
        }
    }

    private void clearSuperPrimarySetting(SQLiteDatabase db, long contactId, long rawContactId) {
        String[] args = {String.valueOf(contactId), String.valueOf(rawContactId)};
        int index = 0;
        StringBuilder mimeTypeCondition = new StringBuilder();
        mimeTypeCondition.append(" AND mimetype_id IN (");
        Cursor c = db.rawQuery("SELECT DISTINCT(a.mimetype_id) FROM (SELECT mimetype_id FROM data WHERE raw_contact_id IN (SELECT _id FROM raw_contacts WHERE contact_id=?1)) AS a JOIN  (SELECT mimetype_id FROM data WHERE raw_contact_id=?2) AS b ON a.mimetype_id=b.mimetype_id", args);
        try {
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                if (index > 0) {
                    mimeTypeCondition.append(',');
                }
                mimeTypeCondition.append(c.getLong(0));
                index++;
            }
            if (index != 0) {
                mimeTypeCondition.append(')');
                String superPrimaryUpdateSql = "UPDATE data SET is_super_primary=0 WHERE (raw_contact_id IN (SELECT _id FROM raw_contacts WHERE contact_id=?1) OR raw_contact_id=?2)" + mimeTypeCondition.toString();
                db.execSQL(superPrimaryUpdateSql, args);
            }
        } finally {
            c.close();
        }
    }

    private int canJoinIntoContact(SQLiteDatabase db, long rawContactId, Set<Long> rawContactIdsInSameAccount, Set<Long> rawContactIdsInOtherAccount) {
        if (rawContactIdsInSameAccount.isEmpty()) {
            String rid = String.valueOf(rawContactId);
            String ridsInOtherAccts = TextUtils.join(",", rawContactIdsInOtherAccount);
            if (DatabaseUtils.longForQuery(db, buildIdentityMatchingSql(rid, ridsInOtherAccts, true, true), null) == 0 && DatabaseUtils.longForQuery(db, buildIdentityMatchingSql(rid, ridsInOtherAccts, false, true), null) > 0) {
                if (VERBOSE_LOGGING) {
                    Log.v("ContactAggregator", "canJoinIntoContact: no duplicates, but has no matching identity and has mis-matching identity on the same namespace between rid=" + rid + " and ridsInOtherAccts=" + ridsInOtherAccts);
                }
                return 0;
            }
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "canJoinIntoContact: can join the first raw contact from the same account without any identity mismatch.");
            }
            return 1;
        }
        if (VERBOSE_LOGGING) {
            Log.v("ContactAggregator", "canJoinIntoContact: " + rawContactIdsInSameAccount.size() + " duplicate(s) found");
        }
        Set<Long> rawContactIdSet = new HashSet<>();
        rawContactIdSet.add(Long.valueOf(rawContactId));
        if (rawContactIdsInSameAccount.size() > 0 && isDataMaching(db, rawContactIdSet, rawContactIdsInSameAccount)) {
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "canJoinIntoContact: join if there is a data matching found in the same account");
            }
            return 1;
        }
        if (VERBOSE_LOGGING) {
            Log.v("ContactAggregator", "canJoinIntoContact: re-aggregate rid=" + rawContactId + " with its best matching contact to connected component");
        }
        return -1;
    }

    private String buildIdentityMatchingSql(String rawContactIdSet1, String rawContactIdSet2, boolean isIdentityMatching, boolean countOnly) {
        String identityType = String.valueOf(this.mMimeTypeIdIdentity);
        String matchingOperator = isIdentityMatching ? "=" : "!=";
        String sql = " FROM data AS d1 JOIN data AS d2 ON (d1.data1" + matchingOperator + " d2.data1 AND d1.data2 = d2.data2 ) WHERE d1.mimetype_id = " + identityType + " AND d2.mimetype_id = " + identityType + " AND d1.raw_contact_id IN (" + rawContactIdSet1 + ") AND d2.raw_contact_id IN (" + rawContactIdSet2 + ")";
        return countOnly ? "SELECT count(*) " + sql : "SELECT d1.raw_contact_id,d2.raw_contact_id" + sql;
    }

    private String buildEmailMatchingSql(String rawContactIdSet1, String rawContactIdSet2, boolean countOnly) {
        String emailType = String.valueOf(this.mMimeTypeIdEmail);
        String sql = " FROM data AS d1 JOIN data AS d2 ON lower(d1.data1)= lower(d2.data1) WHERE d1.mimetype_id = " + emailType + " AND d2.mimetype_id = " + emailType + " AND d1.raw_contact_id IN (" + rawContactIdSet1 + ") AND d2.raw_contact_id IN (" + rawContactIdSet2 + ")";
        return countOnly ? "SELECT count(*) " + sql : "SELECT d1.raw_contact_id,d2.raw_contact_id" + sql;
    }

    private String buildPhoneMatchingSql(String rawContactIdSet1, String rawContactIdSet2, boolean countOnly) {
        String phoneType = String.valueOf(this.mMimeTypeIdPhone);
        String sql = " FROM phone_lookup AS p1 JOIN data AS d1 ON (d1._id=p1.data_id) JOIN phone_lookup AS p2 ON (p1.min_match=p2.min_match) JOIN data AS d2 ON (d2._id=p2.data_id) WHERE d1.mimetype_id = " + phoneType + " AND d2.mimetype_id = " + phoneType + " AND d1.raw_contact_id IN (" + rawContactIdSet1 + ") AND d2.raw_contact_id IN (" + rawContactIdSet2 + ") AND PHONE_NUMBERS_EQUAL(d1.data1,d2.data1," + String.valueOf(this.mDbHelper.getUseStrictPhoneNumberComparisonParameter()) + ")";
        return countOnly ? "SELECT count(*) " + sql : "SELECT d1.raw_contact_id,d2.raw_contact_id" + sql;
    }

    private String buildExceptionMatchingSql(String rawContactIdSet1, String rawContactIdSet2) {
        return "SELECT raw_contact_id1, raw_contact_id2 FROM agg_exceptions WHERE raw_contact_id1 IN (" + rawContactIdSet1 + ") AND raw_contact_id2 IN (" + rawContactIdSet2 + ") AND type=1";
    }

    private boolean isFirstColumnGreaterThanZero(SQLiteDatabase db, String query) {
        return DatabaseUtils.longForQuery(db, query, null) > 0;
    }

    private boolean isDataMaching(SQLiteDatabase db, Set<Long> rawContactIdSet1, Set<Long> rawContactIdSet2) {
        String rawContactIds1 = TextUtils.join(",", rawContactIdSet1);
        String rawContactIds2 = TextUtils.join(",", rawContactIdSet2);
        if (isFirstColumnGreaterThanZero(db, buildIdentityMatchingSql(rawContactIds1, rawContactIds2, true, true))) {
            if (!VERBOSE_LOGGING) {
                return true;
            }
            Log.v("ContactAggregator", "canJoinIntoContact: identity match found between " + rawContactIds1 + " and " + rawContactIds2);
            return true;
        }
        if (isFirstColumnGreaterThanZero(db, buildEmailMatchingSql(rawContactIds1, rawContactIds2, true))) {
            if (!VERBOSE_LOGGING) {
                return true;
            }
            Log.v("ContactAggregator", "canJoinIntoContact: email match found between " + rawContactIds1 + " and " + rawContactIds2);
            return true;
        }
        if (isFirstColumnGreaterThanZero(db, buildPhoneMatchingSql(rawContactIds1, rawContactIds2, true))) {
            if (!VERBOSE_LOGGING) {
                return true;
            }
            Log.v("ContactAggregator", "canJoinIntoContact: phone match found between " + rawContactIds1 + " and " + rawContactIds2);
            return true;
        }
        return false;
    }

    private void reAggregateRawContacts(TransactionContext txContext, SQLiteDatabase db, long contactId, long currentContactId, long rawContactId, Set<Long> existingRawContactIds) {
        Set<Long> allIds = new HashSet<>();
        allIds.add(Long.valueOf(rawContactId));
        allIds.addAll(existingRawContactIds);
        Set<Set<Long>> connectedRawContactSets = findConnectedRawContacts(db, allIds);
        if (connectedRawContactSets.size() == 1) {
            createContactForRawContacts(db, txContext, connectedRawContactSets.iterator().next(), Long.valueOf(contactId));
            return;
        }
        Iterator<Set<Long>> it = connectedRawContactSets.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Set<Long> connectedRawContactIds = it.next();
            if (connectedRawContactIds.contains(Long.valueOf(rawContactId))) {
                createContactForRawContacts(db, txContext, connectedRawContactIds, currentContactId == 0 ? null : Long.valueOf(currentContactId));
                connectedRawContactSets.remove(connectedRawContactIds);
            }
        }
        int index = connectedRawContactSets.size();
        for (Set<Long> connectedRawContactIds2 : connectedRawContactSets) {
            if (index > 1) {
                createContactForRawContacts(db, txContext, connectedRawContactIds2, null);
                index--;
            } else {
                createContactForRawContacts(db, txContext, connectedRawContactIds2, Long.valueOf(contactId));
            }
        }
    }

    private Set<Set<Long>> findConnectedRawContacts(SQLiteDatabase db, Set<Long> rawContactIdSet) {
        Multimap<Long, Long> matchingRawIdPairs = HashMultimap.create();
        String rawContactIds = TextUtils.join(",", rawContactIdSet);
        findIdPairs(db, buildExceptionMatchingSql(rawContactIds, rawContactIds), matchingRawIdPairs);
        findIdPairs(db, buildIdentityMatchingSql(rawContactIds, rawContactIds, true, false), matchingRawIdPairs);
        findIdPairs(db, buildEmailMatchingSql(rawContactIds, rawContactIds, false), matchingRawIdPairs);
        findIdPairs(db, buildPhoneMatchingSql(rawContactIds, rawContactIds, false), matchingRawIdPairs);
        return findConnectedComponents(rawContactIdSet, matchingRawIdPairs);
    }

    static Set<Set<Long>> findConnectedComponents(Set<Long> rawContactIdSet, Multimap<Long, Long> matchingRawIdPairs) {
        Set<Set<Long>> connectedRawContactSets = new HashSet<>();
        Set<Long> visited = new HashSet<>();
        for (Long id : rawContactIdSet) {
            if (!visited.contains(id)) {
                Set<Long> set = new HashSet<>();
                findConnectedComponentForRawContact(matchingRawIdPairs, visited, id, set);
                connectedRawContactSets.add(set);
            }
        }
        return connectedRawContactSets;
    }

    private static void findConnectedComponentForRawContact(Multimap<Long, Long> connections, Set<Long> visited, Long rawContactId, Set<Long> results) {
        visited.add(rawContactId);
        results.add(rawContactId);
        Iterator<Long> it = connections.get(rawContactId).iterator();
        while (it.hasNext()) {
            long match = it.next().longValue();
            if (!visited.contains(Long.valueOf(match))) {
                findConnectedComponentForRawContact(connections, visited, Long.valueOf(match), results);
            }
        }
    }

    private void findIdPairs(SQLiteDatabase db, String query, Multimap<Long, Long> results) {
        Cursor cursor = db.rawQuery(query, null);
        try {
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                long idA = cursor.getLong(0);
                long idB = cursor.getLong(1);
                if (idA != idB) {
                    results.put(Long.valueOf(idA), Long.valueOf(idB));
                    results.put(Long.valueOf(idB), Long.valueOf(idA));
                }
            }
        } finally {
            cursor.close();
        }
    }

    private void createContactForRawContacts(SQLiteDatabase db, TransactionContext txContext, Set<Long> rawContactIds, Long contactId) {
        if (!rawContactIds.isEmpty()) {
            if (contactId == null) {
                this.mSelectionArgs1[0] = String.valueOf(rawContactIds.iterator().next());
                computeAggregateData(db, this.mRawContactsQueryByRawContactId, this.mSelectionArgs1, this.mContactInsert);
                contactId = Long.valueOf(this.mContactInsert.executeInsert());
            }
            for (Long rawContactId : rawContactIds) {
                unpinRawContact(rawContactId.longValue());
                setContactIdAndMarkAggregated(rawContactId.longValue(), contactId.longValue());
                setPresenceContactId(rawContactId.longValue(), contactId.longValue());
            }
            updateAggregateData(txContext, contactId.longValue());
        }
    }

    public void updateAggregationAfterVisibilityChange(long contactId) {
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        boolean visible = this.mDbHelper.isContactInDefaultDirectory(db, contactId);
        if (visible) {
            markContactForAggregation(db, contactId);
            return;
        }
        this.mSelectionArgs1[0] = String.valueOf(contactId);
        Cursor cursor = db.query("raw_contacts", RawContactIdQuery.COLUMNS, "contact_id=?", this.mSelectionArgs1, null, null, null);
        while (cursor.moveToNext()) {
            try {
                long rawContactId = cursor.getLong(0);
                this.mMatcher.clear();
                updateMatchScoresBasedOnIdentityMatch(db, rawContactId, this.mMatcher);
                updateMatchScoresBasedOnNameMatches(db, rawContactId, this.mMatcher);
                List<ContactMatcher.MatchScore> bestMatches = this.mMatcher.pickBestMatches(70);
                for (ContactMatcher.MatchScore matchScore : bestMatches) {
                    markContactForAggregation(db, matchScore.getContactId());
                }
                this.mMatcher.clear();
                updateMatchScoresBasedOnEmailMatches(db, rawContactId, this.mMatcher);
                updateMatchScoresBasedOnPhoneMatches(db, rawContactId, this.mMatcher);
                List<ContactMatcher.MatchScore> bestMatches2 = this.mMatcher.pickBestMatches(50);
                for (ContactMatcher.MatchScore matchScore2 : bestMatches2) {
                    markContactForAggregation(db, matchScore2.getContactId());
                }
            } finally {
                cursor.close();
            }
        }
    }

    protected void setContactId(long rawContactId, long contactId) {
        this.mContactIdUpdate.bindLong(1, contactId);
        this.mContactIdUpdate.bindLong(2, rawContactId);
        this.mContactIdUpdate.execute();
    }

    private void markAggregated(long rawContactId) {
        this.mMarkAggregatedUpdate.bindLong(1, rawContactId);
        this.mMarkAggregatedUpdate.execute();
    }

    private void setContactIdAndMarkAggregated(long rawContactId, long contactId) {
        this.mContactIdAndMarkAggregatedUpdate.bindLong(1, contactId);
        this.mContactIdAndMarkAggregatedUpdate.bindLong(2, rawContactId);
        this.mContactIdAndMarkAggregatedUpdate.execute();
    }

    private void setPresenceContactId(long rawContactId, long contactId) {
        this.mPresenceContactIdUpdate.bindLong(1, contactId);
        this.mPresenceContactIdUpdate.bindLong(2, rawContactId);
        this.mPresenceContactIdUpdate.execute();
    }

    private void unpinRawContact(long rawContactId) {
        this.mResetPinnedForRawContact.bindLong(1, rawContactId);
        this.mResetPinnedForRawContact.execute();
    }

    public void invalidateAggregationExceptionCache() {
        this.mAggregationExceptionIdsValid = false;
    }

    private void prefetchAggregationExceptionIds(SQLiteDatabase db) {
        this.mAggregationExceptionIds.clear();
        Cursor c = db.query("agg_exceptions", AggregateExceptionPrefetchQuery.COLUMNS, null, null, null, null, null);
        while (c.moveToNext()) {
            try {
                long rawContactId1 = c.getLong(0);
                long rawContactId2 = c.getLong(1);
                this.mAggregationExceptionIds.add(Long.valueOf(rawContactId1));
                this.mAggregationExceptionIds.add(Long.valueOf(rawContactId2));
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        this.mAggregationExceptionIdsValid = true;
    }

    private long pickBestMatchBasedOnExceptions(SQLiteDatabase db, long rawContactId, ContactMatcher matcher) {
        if (!this.mAggregationExceptionIdsValid) {
            prefetchAggregationExceptionIds(db);
        }
        if (!this.mAggregationExceptionIds.contains(Long.valueOf(rawContactId))) {
            return -1L;
        }
        Cursor c = db.query("agg_exceptions JOIN raw_contacts raw_contacts1  ON (agg_exceptions.raw_contact_id1 = raw_contacts1._id)  JOIN raw_contacts raw_contacts2  ON (agg_exceptions.raw_contact_id2 = raw_contacts2._id) ", AggregateExceptionQuery.COLUMNS, "raw_contact_id1=" + rawContactId + " OR raw_contact_id2=" + rawContactId, null, null, null, null);
        while (c.moveToNext()) {
            try {
                int type = c.getInt(0);
                long rawContactId1 = c.getLong(1);
                long contactId = -1;
                if (rawContactId == rawContactId1) {
                    if (c.getInt(5) == 0 && !c.isNull(4)) {
                        contactId = c.getLong(4);
                    }
                } else if (c.getInt(3) == 0 && !c.isNull(2)) {
                    contactId = c.getLong(2);
                }
                if (contactId != -1) {
                    if (type == 1) {
                        matcher.keepIn(contactId);
                    } else {
                        matcher.keepOut(contactId);
                    }
                }
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        return matcher.pickBestMatch(100, true);
    }

    private long pickBestMatchBasedOnData(SQLiteDatabase db, long rawContactId, MatchCandidateList candidates, ContactMatcher matcher) {
        long bestMatch = updateMatchScoresBasedOnDataMatches(db, rawContactId, matcher);
        if (bestMatch == -2) {
            return -1L;
        }
        if (bestMatch == -1) {
            bestMatch = pickBestMatchBasedOnSecondaryData(db, rawContactId, candidates, matcher);
            if (bestMatch == -2) {
                return -1L;
            }
        }
        return bestMatch;
    }

    private long pickBestMatchBasedOnSecondaryData(SQLiteDatabase db, long rawContactId, MatchCandidateList candidates, ContactMatcher matcher) {
        List<Long> secondaryContactIds = matcher.prepareSecondaryMatchCandidates(70);
        if (secondaryContactIds == null || secondaryContactIds.size() > 20) {
            return -1L;
        }
        loadNameMatchCandidates(db, rawContactId, candidates, true);
        this.mSb.setLength(0);
        this.mSb.append("contact_id").append(" IN (");
        for (int i = 0; i < secondaryContactIds.size(); i++) {
            if (i != 0) {
                this.mSb.append(',');
            }
            this.mSb.append(secondaryContactIds.get(i));
        }
        this.mSb.append(") AND name_type IN (0,1,2)");
        matchAllCandidates(db, this.mSb.toString(), candidates, matcher, 1, null);
        return matcher.pickBestMatch(50, false);
    }

    private void loadNameMatchCandidates(SQLiteDatabase db, long rawContactId, MatchCandidateList candidates, boolean structuredNameBased) {
        candidates.clear();
        this.mSelectionArgs1[0] = String.valueOf(rawContactId);
        Cursor c = db.query("name_lookup", NameLookupQuery.COLUMNS, structuredNameBased ? "raw_contact_id=? AND name_type IN (0,1,2)" : "raw_contact_id=?", this.mSelectionArgs1, null, null, null);
        while (c.moveToNext()) {
            try {
                String normalizedName = c.getString(0);
                int type = c.getInt(1);
                candidates.add(normalizedName, type);
            } finally {
                c.close();
            }
        }
    }

    private long updateMatchScoresBasedOnDataMatches(SQLiteDatabase db, long rawContactId, ContactMatcher matcher) {
        updateMatchScoresBasedOnIdentityMatch(db, rawContactId, matcher);
        updateMatchScoresBasedOnNameMatches(db, rawContactId, matcher);
        long bestMatch = matcher.pickBestMatch(70, false);
        if (bestMatch == -1) {
            updateMatchScoresBasedOnEmailMatches(db, rawContactId, matcher);
            updateMatchScoresBasedOnPhoneMatches(db, rawContactId, matcher);
            return -1L;
        }
        return bestMatch;
    }

    private void updateMatchScoresBasedOnIdentityMatch(SQLiteDatabase db, long rawContactId, ContactMatcher matcher) {
        this.mSelectionArgs2[0] = String.valueOf(rawContactId);
        this.mSelectionArgs2[1] = String.valueOf(this.mMimeTypeIdIdentity);
        Cursor c = db.query("data dataA JOIN data dataB ON (dataA.data2=dataB.data2 AND dataA.data1=dataB.data1) JOIN raw_contacts ON (dataB.raw_contact_id = raw_contacts._id)", IdentityLookupMatchQuery.COLUMNS, "dataA.raw_contact_id=?1 AND dataA.mimetype_id=?2 AND dataA.data2 NOT NULL AND dataA.data1 NOT NULL AND dataB.mimetype_id=?2 AND aggregation_needed=0 AND contact_id IN default_directory", this.mSelectionArgs2, "contact_id", null, null);
        while (c.moveToNext()) {
            try {
                long contactId = c.getLong(0);
                matcher.matchIdentity(contactId);
            } finally {
                c.close();
            }
        }
    }

    private void updateMatchScoresBasedOnNameMatches(SQLiteDatabase db, long rawContactId, ContactMatcher matcher) {
        this.mSelectionArgs1[0] = String.valueOf(rawContactId);
        Cursor c = db.query("name_lookup nameA JOIN name_lookup nameB ON (nameA.normalized_name=nameB.normalized_name) JOIN raw_contacts ON (nameB.raw_contact_id = raw_contacts._id)", NameLookupMatchQuery.COLUMNS, "nameA.raw_contact_id=? AND aggregation_needed=0 AND contact_id IN default_directory", this.mSelectionArgs1, null, null, null, PRIMARY_HIT_LIMIT_STRING);
        while (c.moveToNext()) {
            try {
                long contactId = c.getLong(0);
                String name = c.getString(1);
                int nameTypeA = c.getInt(2);
                int nameTypeB = c.getInt(3);
                matcher.matchName(contactId, nameTypeA, name, nameTypeB, name, 0);
                if (nameTypeA == 3 && nameTypeB == 3) {
                    matcher.updateScoreWithNicknameMatch(contactId);
                }
            } finally {
                c.close();
            }
        }
    }

    private final class NameLookupSelectionBuilder extends NameLookupBuilder {
        private final MatchCandidateList mNameLookupCandidates;
        private StringBuilder mSelection;

        public NameLookupSelectionBuilder(NameSplitter splitter, MatchCandidateList candidates) {
            super(splitter);
            this.mSelection = new StringBuilder("normalized_name IN(");
            this.mNameLookupCandidates = candidates;
        }

        @Override
        protected String[] getCommonNicknameClusters(String normalizedName) {
            return ContactAggregator.this.mCommonNicknameCache.getCommonNicknameClusters(normalizedName);
        }

        @Override
        protected void insertNameLookup(long rawContactId, long dataId, int lookupType, String string) {
            this.mNameLookupCandidates.add(string, lookupType);
            DatabaseUtils.appendEscapedSQLString(this.mSelection, string);
            this.mSelection.append(',');
        }

        public boolean isEmpty() {
            return this.mNameLookupCandidates.isEmpty();
        }

        public String getSelection() {
            this.mSelection.setLength(this.mSelection.length() - 1);
            this.mSelection.append(')');
            return this.mSelection.toString();
        }

        public int getLookupType(String name) {
            for (int i = 0; i < this.mNameLookupCandidates.mCount; i++) {
                if (((NameMatchCandidate) this.mNameLookupCandidates.mList.get(i)).mName.equals(name)) {
                    return ((NameMatchCandidate) this.mNameLookupCandidates.mList.get(i)).mLookupType;
                }
            }
            throw new IllegalStateException();
        }
    }

    private void updateMatchScoresBasedOnNameMatches(SQLiteDatabase db, String query, MatchCandidateList candidates, ContactMatcher matcher) {
        candidates.clear();
        NameLookupSelectionBuilder builder = new NameLookupSelectionBuilder(this.mNameSplitter, candidates);
        builder.insertNameLookup(0L, 0L, query, 0);
        if (!builder.isEmpty()) {
            Cursor c = db.query("name_lookup JOIN raw_contacts ON (raw_contact_id = raw_contacts._id)", NameLookupMatchQueryWithParameter.COLUMNS, builder.getSelection(), null, null, null, null, PRIMARY_HIT_LIMIT_STRING);
            while (c.moveToNext()) {
                try {
                    long contactId = c.getLong(0);
                    String name = c.getString(1);
                    int nameTypeA = builder.getLookupType(name);
                    int nameTypeB = c.getInt(2);
                    matcher.matchName(contactId, nameTypeA, name, nameTypeB, name, 0);
                    if (nameTypeA == 3 && nameTypeB == 3) {
                        matcher.updateScoreWithNicknameMatch(contactId);
                    }
                } finally {
                    c.close();
                }
            }
        }
    }

    private void updateMatchScoresBasedOnEmailMatches(SQLiteDatabase db, long rawContactId, ContactMatcher matcher) {
        this.mSelectionArgs2[0] = String.valueOf(rawContactId);
        this.mSelectionArgs2[1] = String.valueOf(this.mMimeTypeIdEmail);
        Cursor c = db.query("data dataA JOIN data dataB ON lower(dataA.data1)=lower(dataB.data1) JOIN raw_contacts ON (dataB.raw_contact_id = raw_contacts._id)", EmailLookupQuery.COLUMNS, "dataA.raw_contact_id=?1 AND dataA.mimetype_id=?2 AND dataA.data1 NOT NULL AND dataB.mimetype_id=?2 AND aggregation_needed=0 AND contact_id IN default_directory", this.mSelectionArgs2, null, null, null, SECONDARY_HIT_LIMIT_STRING);
        while (c.moveToNext()) {
            try {
                long contactId = c.getLong(0);
                matcher.updateScoreWithEmailMatch(contactId);
            } finally {
                c.close();
            }
        }
    }

    private void updateMatchScoresBasedOnPhoneMatches(SQLiteDatabase db, long rawContactId, ContactMatcher matcher) {
        this.mSelectionArgs2[0] = String.valueOf(rawContactId);
        this.mSelectionArgs2[1] = this.mDbHelper.getUseStrictPhoneNumberComparisonParameter();
        Cursor c = db.query("phone_lookup phoneA JOIN data dataA ON (dataA._id=phoneA.data_id) JOIN phone_lookup phoneB ON (phoneA.min_match=phoneB.min_match) JOIN data dataB ON (dataB._id=phoneB.data_id) JOIN raw_contacts ON (dataB.raw_contact_id = raw_contacts._id)", PhoneLookupQuery.COLUMNS, "dataA.raw_contact_id=? AND PHONE_NUMBERS_EQUAL(dataA.data1, dataB.data1,?) AND aggregation_needed=0 AND contact_id IN default_directory", this.mSelectionArgs2, null, null, null, SECONDARY_HIT_LIMIT_STRING);
        while (c.moveToNext()) {
            try {
                long contactId = c.getLong(0);
                matcher.updateScoreWithPhoneNumberMatch(contactId);
            } finally {
                c.close();
            }
        }
    }

    private void lookupApproximateNameMatches(SQLiteDatabase db, MatchCandidateList candidates, ContactMatcher matcher) {
        HashSet<String> firstLetters = new HashSet<>();
        for (int i = 0; i < candidates.mCount; i++) {
            NameMatchCandidate candidate = (NameMatchCandidate) candidates.mList.get(i);
            if (candidate.mName.length() >= 2) {
                String firstLetter = candidate.mName.substring(0, 2);
                if (!firstLetters.contains(firstLetter)) {
                    firstLetters.add(firstLetter);
                    String selection = "(normalized_name GLOB '" + firstLetter + "*') AND (name_type IN(2,4,3)) AND contact_id IN default_directory";
                    matchAllCandidates(db, selection, candidates, matcher, 2, String.valueOf(100));
                }
            }
        }
    }

    private void matchAllCandidates(SQLiteDatabase db, String selection, MatchCandidateList candidates, ContactMatcher matcher, int algorithm, String limit) {
        Cursor c = db.query("name_lookup INNER JOIN view_raw_contacts ON (name_lookup.raw_contact_id = view_raw_contacts._id)", ContactNameLookupQuery.COLUMNS, selection, null, null, null, null, limit);
        while (c.moveToNext()) {
            try {
                Long contactId = Long.valueOf(c.getLong(0));
                String name = c.getString(1);
                int nameType = c.getInt(2);
                for (int i = 0; i < candidates.mCount; i++) {
                    NameMatchCandidate candidate = (NameMatchCandidate) candidates.mList.get(i);
                    matcher.matchName(contactId.longValue(), candidate.mLookupType, candidate.mName, nameType, name, algorithm);
                }
            } finally {
                c.close();
            }
        }
    }

    private void computeAggregateData(SQLiteDatabase db, long contactId, SQLiteStatement statement) {
        this.mSelectionArgs1[0] = String.valueOf(contactId);
        computeAggregateData(db, this.mRawContactsQueryByContactId, this.mSelectionArgs1, statement);
    }

    private boolean hasHigherPhotoPriority(PhotoEntry photoEntry, int priority, PhotoEntry bestPhotoEntry, int bestPriority) {
        int photoComparison = photoEntry.compareTo(bestPhotoEntry);
        return photoComparison < 0 || (photoComparison == 0 && priority > bestPriority);
    }

    private void computeAggregateData(SQLiteDatabase db, String sql, String[] sqlArgs, SQLiteStatement statement) {
        long currentRawContactId = -1;
        long bestPhotoId = -1;
        long bestPhotoFileId = 0;
        PhotoEntry bestPhotoEntry = null;
        boolean foundSuperPrimaryPhoto = false;
        int photoPriority = -1;
        int totalRowCount = 0;
        int contactSendToVoicemail = 0;
        String contactCustomRingtone = null;
        long contactLastTimeContacted = 0;
        int contactTimesContacted = 0;
        int contactStarred = 0;
        int contactPinned = Integer.MAX_VALUE;
        int hasPhoneNumber = 0;
        StringBuilder lookupKey = new StringBuilder();
        this.mDisplayNameCandidate.clear();
        Cursor c = db.rawQuery(sql, sqlArgs);
        while (c.moveToNext()) {
            try {
                long rawContactId = c.getLong(0);
                if (rawContactId != currentRawContactId) {
                    currentRawContactId = rawContactId;
                    totalRowCount++;
                    String accountType = c.getString(3);
                    String dataSet = c.getString(5);
                    String accountWithDataSet = !TextUtils.isEmpty(dataSet) ? accountType + "/" + dataSet : accountType;
                    String displayName = c.getString(1);
                    int displayNameSource = c.getInt(2);
                    int nameVerified = c.getInt(13);
                    processDisplayNameCandidate(rawContactId, displayName, displayNameSource, this.mContactsProvider.isWritableAccountWithDataSet(accountWithDataSet), nameVerified != 0);
                    if (!c.isNull(8)) {
                        boolean sendToVoicemail = c.getInt(8) != 0;
                        if (sendToVoicemail) {
                            contactSendToVoicemail++;
                        }
                    }
                    if (contactCustomRingtone == null && !c.isNull(7)) {
                        contactCustomRingtone = c.getString(7);
                    }
                    long lastTimeContacted = c.getLong(9);
                    if (lastTimeContacted > contactLastTimeContacted) {
                        contactLastTimeContacted = lastTimeContacted;
                    }
                    int timesContacted = c.getInt(10);
                    if (timesContacted > contactTimesContacted) {
                        contactTimesContacted = timesContacted;
                    }
                    if (c.getInt(11) != 0) {
                        contactStarred = 1;
                    }
                    int rawContactPinned = c.getInt(12);
                    if (rawContactPinned > 0) {
                        contactPinned = Math.min(contactPinned, rawContactPinned);
                    }
                    appendLookupKey(lookupKey, accountWithDataSet, c.getString(4), rawContactId, c.getString(6), displayName);
                }
                if (!c.isNull(14)) {
                    long dataId = c.getLong(14);
                    long photoFileId = c.getLong(17);
                    int mimetypeId = c.getInt(15);
                    boolean superPrimary = c.getInt(16) != 0;
                    if (mimetypeId == this.mMimeTypeIdPhoto) {
                        if (!foundSuperPrimaryPhoto) {
                            PhotoEntry photoEntry = getPhotoMetadata(db, photoFileId);
                            int priority = this.mPhotoPriorityResolver.getPhotoPriority(c.getString(3));
                            if (superPrimary || hasHigherPhotoPriority(photoEntry, priority, bestPhotoEntry, photoPriority)) {
                                bestPhotoEntry = photoEntry;
                                photoPriority = priority;
                                bestPhotoId = dataId;
                                bestPhotoFileId = photoFileId;
                                foundSuperPrimaryPhoto |= superPrimary;
                            }
                        }
                    } else if (mimetypeId == this.mMimeTypeIdPhone) {
                        hasPhoneNumber = 1;
                    }
                }
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        if (contactPinned == Integer.MAX_VALUE) {
            contactPinned = 0;
        }
        statement.bindLong(1, this.mDisplayNameCandidate.rawContactId);
        if (bestPhotoId != -1) {
            statement.bindLong(2, bestPhotoId);
        } else {
            statement.bindNull(2);
        }
        if (bestPhotoFileId != 0) {
            statement.bindLong(3, bestPhotoFileId);
        } else {
            statement.bindNull(3);
        }
        statement.bindLong(4, totalRowCount == contactSendToVoicemail ? 1L : 0L);
        DatabaseUtils.bindObjectToProgram(statement, 5, contactCustomRingtone);
        statement.bindLong(6, contactLastTimeContacted);
        statement.bindLong(7, contactTimesContacted);
        statement.bindLong(8, contactStarred);
        statement.bindLong(9, contactPinned);
        statement.bindLong(10, hasPhoneNumber);
        statement.bindString(11, Uri.encode(lookupKey.toString()));
        statement.bindLong(12, Clock.getInstance().currentTimeMillis());
    }

    protected void appendLookupKey(StringBuilder sb, String accountTypeWithDataSet, String accountName, long rawContactId, String sourceId, String displayName) {
        ContactLookupKey.appendToLookupKey(sb, accountTypeWithDataSet, accountName, rawContactId, sourceId, displayName);
    }

    private void processDisplayNameCandidate(long rawContactId, String displayName, int displayNameSource, boolean writableAccount, boolean verified) {
        boolean replace = false;
        if (this.mDisplayNameCandidate.rawContactId == -1) {
            replace = true;
        } else if (!TextUtils.isEmpty(displayName)) {
            if (!this.mDisplayNameCandidate.verified && verified) {
                replace = true;
            } else if (this.mDisplayNameCandidate.verified == verified) {
                if (this.mDisplayNameCandidate.displayNameSource < displayNameSource) {
                    replace = true;
                } else if (this.mDisplayNameCandidate.displayNameSource == displayNameSource) {
                    if (!this.mDisplayNameCandidate.writableAccount && writableAccount) {
                        replace = true;
                    } else if (this.mDisplayNameCandidate.writableAccount == writableAccount && NameNormalizer.compareComplexity(displayName, this.mDisplayNameCandidate.displayName) > 0) {
                        replace = true;
                    }
                }
            }
        }
        if (replace) {
            this.mDisplayNameCandidate.rawContactId = rawContactId;
            this.mDisplayNameCandidate.displayName = displayName;
            this.mDisplayNameCandidate.displayNameSource = displayNameSource;
            this.mDisplayNameCandidate.verified = verified;
            this.mDisplayNameCandidate.writableAccount = writableAccount;
        }
    }

    public void updatePhotoId(SQLiteDatabase db, long rawContactId) {
        long contactId = this.mDbHelper.getContactId(rawContactId);
        if (contactId != 0) {
            long bestPhotoId = -1;
            long bestPhotoFileId = 0;
            int photoPriority = -1;
            long photoMimeType = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/photo");
            String tables = "raw_contacts JOIN accounts ON (accounts._id=raw_contacts.account_id) JOIN data ON(data.raw_contact_id=raw_contacts._id AND (mimetype_id=" + photoMimeType + " AND data15 NOT NULL))";
            this.mSelectionArgs1[0] = String.valueOf(contactId);
            Cursor c = db.query(tables, PhotoIdQuery.COLUMNS, "contact_id=?", this.mSelectionArgs1, null, null, null);
            PhotoEntry bestPhotoEntry = null;
            while (c.moveToNext()) {
                try {
                    long dataId = c.getLong(1);
                    long photoFileId = c.getLong(3);
                    boolean superPrimary = c.getInt(2) != 0;
                    PhotoEntry photoEntry = getPhotoMetadata(db, photoFileId);
                    String accountType = c.getString(0);
                    int priority = this.mPhotoPriorityResolver.getPhotoPriority(accountType);
                    if (superPrimary || hasHigherPhotoPriority(photoEntry, priority, bestPhotoEntry, photoPriority)) {
                        bestPhotoEntry = photoEntry;
                        photoPriority = priority;
                        bestPhotoId = dataId;
                        bestPhotoFileId = photoFileId;
                        if (superPrimary) {
                            break;
                        }
                    }
                } catch (Throwable th) {
                    c.close();
                    throw th;
                }
            }
            c.close();
            if (bestPhotoId == -1) {
                this.mPhotoIdUpdate.bindNull(1);
            } else {
                this.mPhotoIdUpdate.bindLong(1, bestPhotoId);
            }
            if (bestPhotoFileId == 0) {
                this.mPhotoIdUpdate.bindNull(2);
            } else {
                this.mPhotoIdUpdate.bindLong(2, bestPhotoFileId);
            }
            this.mPhotoIdUpdate.bindLong(3, contactId);
            this.mPhotoIdUpdate.execute();
        }
    }

    private class PhotoEntry implements Comparable<PhotoEntry> {
        final int fileSize;
        final int pixelCount;

        private PhotoEntry(int pixelCount, int fileSize) {
            this.pixelCount = pixelCount;
            this.fileSize = fileSize;
        }

        @Override
        public int compareTo(PhotoEntry pe) {
            if (pe == null) {
                return -1;
            }
            if (this.pixelCount == pe.pixelCount) {
                return pe.fileSize - this.fileSize;
            }
            return pe.pixelCount - this.pixelCount;
        }
    }

    private PhotoEntry getPhotoMetadata(SQLiteDatabase db, long photoFileId) {
        if (photoFileId == 0) {
            int thumbDim = this.mContactsProvider.getMaxThumbnailDim();
            return new PhotoEntry(thumbDim * thumbDim, 0);
        }
        Cursor c = db.query("photo_files", PhotoFileQuery.COLUMNS, "_id=?", new String[]{String.valueOf(photoFileId)}, null, null, null);
        try {
            if (c.getCount() == 1) {
                c.moveToFirst();
                int pixelCount = c.getInt(0) * c.getInt(1);
                return new PhotoEntry(pixelCount, c.getInt(2));
            }
            c.close();
            return new PhotoEntry(0, 0);
        } finally {
            c.close();
        }
    }

    public void updateDisplayNameForRawContact(SQLiteDatabase db, long rawContactId) {
        long contactId = this.mDbHelper.getContactId(rawContactId);
        if (contactId != 0) {
            updateDisplayNameForContact(db, contactId);
        }
    }

    public void updateDisplayNameForContact(SQLiteDatabase db, long contactId) {
        boolean lookupKeyUpdateNeeded = false;
        this.mDisplayNameCandidate.clear();
        this.mSelectionArgs1[0] = String.valueOf(contactId);
        Cursor c = db.query("view_raw_contacts", DisplayNameQuery.COLUMNS, "contact_id=?", this.mSelectionArgs1, null, null, null);
        while (c.moveToNext()) {
            try {
                long rawContactId = c.getLong(0);
                String displayName = c.getString(1);
                int displayNameSource = c.getInt(2);
                int nameVerified = c.getInt(3);
                String accountTypeAndDataSet = c.getString(5);
                processDisplayNameCandidate(rawContactId, displayName, displayNameSource, this.mContactsProvider.isWritableAccountWithDataSet(accountTypeAndDataSet), nameVerified != 0);
                lookupKeyUpdateNeeded |= c.isNull(4);
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        if (this.mDisplayNameCandidate.rawContactId != -1) {
            this.mDisplayNameUpdate.bindLong(1, this.mDisplayNameCandidate.rawContactId);
            this.mDisplayNameUpdate.bindLong(2, contactId);
            this.mDisplayNameUpdate.execute();
        }
        if (lookupKeyUpdateNeeded) {
            updateLookupKeyForContact(db, contactId);
        }
    }

    public void updateHasPhoneNumber(SQLiteDatabase db, long rawContactId) {
        long contactId = this.mDbHelper.getContactId(rawContactId);
        if (contactId != 0) {
            SQLiteStatement hasPhoneNumberUpdate = db.compileStatement("UPDATE contacts SET has_phone_number=(SELECT (CASE WHEN COUNT(*)=0 THEN 0 ELSE 1 END) FROM data JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) WHERE mimetype_id=? AND data1 NOT NULL AND contact_id=?) WHERE _id=?");
            try {
                hasPhoneNumberUpdate.bindLong(1, this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/phone_v2"));
                hasPhoneNumberUpdate.bindLong(2, contactId);
                hasPhoneNumberUpdate.bindLong(3, contactId);
                hasPhoneNumberUpdate.execute();
            } finally {
                hasPhoneNumberUpdate.close();
            }
        }
    }

    public void updateLookupKeyForRawContact(SQLiteDatabase db, long rawContactId) {
        long contactId = this.mDbHelper.getContactId(rawContactId);
        if (contactId != 0) {
            updateLookupKeyForContact(db, contactId);
        }
    }

    private void updateLookupKeyForContact(SQLiteDatabase db, long contactId) {
        String lookupKey = computeLookupKeyForContact(db, contactId);
        if (lookupKey == null) {
            this.mLookupKeyUpdate.bindNull(1);
        } else {
            this.mLookupKeyUpdate.bindString(1, Uri.encode(lookupKey));
        }
        this.mLookupKeyUpdate.bindLong(2, contactId);
        this.mLookupKeyUpdate.execute();
    }

    protected String computeLookupKeyForContact(SQLiteDatabase db, long contactId) {
        StringBuilder sb = new StringBuilder();
        this.mSelectionArgs1[0] = String.valueOf(contactId);
        Cursor c = db.query("view_raw_contacts", LookupKeyQuery.COLUMNS, "contact_id=?", this.mSelectionArgs1, null, null, "_id");
        while (c.moveToNext()) {
            try {
                ContactLookupKey.appendToLookupKey(sb, c.getString(2), c.getString(3), c.getLong(0), c.getString(4), c.getString(1));
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    public void updateStarred(long rawContactId) {
        long contactId = this.mDbHelper.getContactId(rawContactId);
        if (contactId != 0) {
            this.mStarredUpdate.bindLong(1, contactId);
            this.mStarredUpdate.execute();
        }
    }

    public void updatePinned(long rawContactId) {
        long contactId = this.mDbHelper.getContactId(rawContactId);
        if (contactId != 0) {
            this.mPinnedUpdate.bindLong(1, contactId);
            this.mPinnedUpdate.execute();
        }
    }

    public Cursor queryAggregationSuggestions(SQLiteQueryBuilder qb, String[] projection, long contactId, int maxSuggestions, String filter, ArrayList<AggregationSuggestionParameter> parameters) {
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            List<ContactMatcher.MatchScore> bestMatches = findMatchingContacts(db, contactId, parameters);
            return queryMatchingContacts(qb, db, projection, bestMatches, maxSuggestions, filter);
        } finally {
            db.endTransaction();
        }
    }

    private Cursor queryMatchingContacts(SQLiteQueryBuilder qb, SQLiteDatabase db, String[] projection, List<ContactMatcher.MatchScore> bestMatches, int maxSuggestions, String filter) {
        List<ContactMatcher.MatchScore> limitedMatches;
        StringBuilder sb = new StringBuilder();
        sb.append("_id");
        sb.append(" IN (");
        for (int i = 0; i < bestMatches.size(); i++) {
            ContactMatcher.MatchScore matchScore = bestMatches.get(i);
            if (i != 0) {
                sb.append(",");
            }
            sb.append(matchScore.getContactId());
        }
        sb.append(")");
        if (!TextUtils.isEmpty(filter)) {
            sb.append(" AND _id IN ");
            this.mContactsProvider.appendContactFilterAsNestedQuery(sb, filter);
        }
        HashSet<Long> foundIds = new HashSet<>();
        Cursor cursor = db.query(qb.getTables(), ContactIdQuery.COLUMNS, sb.toString(), null, null, null, null);
        while (cursor.moveToNext()) {
            try {
                foundIds.add(Long.valueOf(cursor.getLong(0)));
            } catch (Throwable th) {
                cursor.close();
                throw th;
            }
        }
        cursor.close();
        Iterator<ContactMatcher.MatchScore> iter = bestMatches.iterator();
        while (iter.hasNext()) {
            long id = iter.next().getContactId();
            if (!foundIds.contains(Long.valueOf(id))) {
                iter.remove();
            }
        }
        if (bestMatches.size() > maxSuggestions) {
            limitedMatches = bestMatches.subList(0, maxSuggestions);
        } else {
            limitedMatches = bestMatches;
        }
        sb.setLength(0);
        sb.append("_id");
        sb.append(" IN (");
        for (int i2 = 0; i2 < limitedMatches.size(); i2++) {
            ContactMatcher.MatchScore matchScore2 = limitedMatches.get(i2);
            if (i2 != 0) {
                sb.append(",");
            }
            sb.append(matchScore2.getContactId());
        }
        sb.append(")");
        Cursor cursor2 = qb.query(db, projection, sb.toString(), null, null, null, "_id");
        ArrayList<Long> sortedContactIds = new ArrayList<>(limitedMatches.size());
        for (ContactMatcher.MatchScore matchScore3 : limitedMatches) {
            sortedContactIds.add(Long.valueOf(matchScore3.getContactId()));
        }
        Collections.sort(sortedContactIds);
        int[] positionMap = new int[limitedMatches.size()];
        for (int i3 = 0; i3 < positionMap.length; i3++) {
            long id2 = limitedMatches.get(i3).getContactId();
            positionMap[i3] = sortedContactIds.indexOf(Long.valueOf(id2));
        }
        return new ReorderingCursorWrapper(cursor2, positionMap);
    }

    private List<ContactMatcher.MatchScore> findMatchingContacts(SQLiteDatabase db, long contactId, ArrayList<AggregationSuggestionParameter> parameters) {
        MatchCandidateList candidates = new MatchCandidateList();
        ContactMatcher matcher = new ContactMatcher();
        matcher.keepOut(contactId);
        if (parameters == null || parameters.size() == 0) {
            Cursor c = db.query("raw_contacts", RawContactIdQuery.COLUMNS, "contact_id=" + contactId, null, null, null, null);
            while (c.moveToNext()) {
                try {
                    long rawContactId = c.getLong(0);
                    updateMatchScoresForSuggestionsBasedOnDataMatches(db, rawContactId, candidates, matcher);
                } finally {
                    c.close();
                }
            }
        } else {
            updateMatchScoresForSuggestionsBasedOnDataMatches(db, candidates, matcher, parameters);
        }
        return matcher.pickBestMatches(50);
    }

    private void updateMatchScoresForSuggestionsBasedOnDataMatches(SQLiteDatabase db, long rawContactId, MatchCandidateList candidates, ContactMatcher matcher) {
        updateMatchScoresBasedOnIdentityMatch(db, rawContactId, matcher);
        updateMatchScoresBasedOnNameMatches(db, rawContactId, matcher);
        updateMatchScoresBasedOnEmailMatches(db, rawContactId, matcher);
        updateMatchScoresBasedOnPhoneMatches(db, rawContactId, matcher);
        loadNameMatchCandidates(db, rawContactId, candidates, false);
        lookupApproximateNameMatches(db, candidates, matcher);
    }

    private void updateMatchScoresForSuggestionsBasedOnDataMatches(SQLiteDatabase db, MatchCandidateList candidates, ContactMatcher matcher, ArrayList<AggregationSuggestionParameter> parameters) {
        for (AggregationSuggestionParameter parameter : parameters) {
            if ("name".equals(parameter.kind)) {
                updateMatchScoresBasedOnNameMatches(db, parameter.value, candidates, matcher);
            }
        }
    }
}
