package com.android.providers.contacts.aggregation;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
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
import com.android.providers.contacts.aggregation.util.ContactAggregatorHelper;
import com.android.providers.contacts.aggregation.util.ContactMatcher;
import com.android.providers.contacts.aggregation.util.MatchScore;
import com.android.providers.contacts.util.Clock;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class AbstractContactAggregator {

    @VisibleForTesting
    static final int AGGREGATION_CONTACT_SIZE_LIMIT = 50;
    protected SQLiteStatement mAggregatedPresenceDelete;
    protected SQLiteStatement mAggregatedPresenceReplace;
    protected boolean mAggregationExceptionIdsValid;
    protected final CommonNicknameCache mCommonNicknameCache;
    protected SQLiteStatement mContactIdAndMarkAggregatedUpdate;
    protected SQLiteStatement mContactIdUpdate;
    protected SQLiteStatement mContactInsert;
    protected SQLiteStatement mContactUpdate;
    protected final ContactsProvider2 mContactsProvider;
    protected final ContactsDatabaseHelper mDbHelper;
    protected SQLiteStatement mDisplayNameUpdate;
    protected SQLiteStatement mLookupKeyUpdate;
    protected SQLiteStatement mMarkForAggregation;
    protected long mMimeTypeIdEmail;
    protected long mMimeTypeIdIdentity;
    protected long mMimeTypeIdPhone;
    protected long mMimeTypeIdPhoto;
    protected final NameSplitter mNameSplitter;
    protected SQLiteStatement mPhotoIdUpdate;
    protected PhotoPriorityResolver mPhotoPriorityResolver;
    protected SQLiteStatement mPinnedUpdate;
    protected SQLiteStatement mPresenceContactIdUpdate;
    protected SQLiteStatement mRawContactCountQuery;
    protected String mRawContactsQueryByContactId;
    protected String mRawContactsQueryByRawContactId;
    protected SQLiteStatement mResetPinnedForRawContact;
    protected SQLiteStatement mSendToVoicemailUpdate;
    protected SQLiteStatement mStarredUpdate;
    protected static final boolean DEBUG_LOGGING = Log.isLoggable("ContactAggregator", 3);
    protected static final boolean VERBOSE_LOGGING = Log.isLoggable("ContactAggregator", 2);
    protected static final String PRIMARY_HIT_LIMIT_STRING = String.valueOf(15);
    protected static final String SECONDARY_HIT_LIMIT_STRING = String.valueOf(20);
    protected boolean mEnabled = true;
    protected ArrayMap<Long, Integer> mRawContactsMarkedForAggregation = new ArrayMap<>();
    protected String[] mSelectionArgs1 = new String[1];
    protected String[] mSelectionArgs2 = new String[2];
    protected StringBuilder mSb = new StringBuilder();
    protected MatchCandidateList mCandidates = new MatchCandidateList();
    protected DisplayNameCandidate mDisplayNameCandidate = new DisplayNameCandidate();
    protected final ArraySet<Long> mAggregationExceptionIds = new ArraySet<>();

    interface AggregateExceptionPrefetchQuery {
        public static final String[] COLUMNS = {"raw_contact_id1", "raw_contact_id2"};
    }

    interface AggregateExceptionQuery {
        public static final String[] COLUMNS = {"type", "raw_contact_id1", "raw_contacts1.contact_id", "raw_contacts1.account_id", "raw_contacts1.aggregation_needed", "raw_contact_id2", "raw_contacts2.contact_id", "raw_contacts2.account_id", "raw_contacts2.aggregation_needed"};
    }

    private interface ContactIdQuery {
        public static final String[] COLUMNS = {"_id"};
    }

    protected interface EmailLookupQuery {
        public static final String[] COLUMNS = {"raw_contacts._id", "contact_id", "account_id"};
    }

    private interface LookupKeyQuery {
        public static final String[] COLUMNS = {"_id", "display_name", "account_type_and_data_set", "account_name", "sourceid"};
    }

    protected interface NameLookupMatchQueryWithParameter {
        public static final String[] COLUMNS = {"_id", "contact_id", "account_id", "normalized_name", "name_type"};
    }

    protected interface NameLookupQuery {
        public static final String[] COLUMNS = {"normalized_name", "name_type"};
    }

    protected interface PhoneLookupQuery {
        public static final String[] COLUMNS = {"raw_contacts._id", "contact_id", "account_id"};
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

    protected static class RawContactIdQuery {
        public static final String[] COLUMNS = {"_id", "account_id"};
    }

    abstract void aggregateContact(TransactionContext transactionContext, SQLiteDatabase sQLiteDatabase, long j, long j2, long j3, MatchCandidateList matchCandidateList);

    protected abstract List<MatchScore> findMatchingContacts(SQLiteDatabase sQLiteDatabase, long j, ArrayList<AggregationSuggestionParameter> arrayList);

    public abstract void updateAggregationAfterVisibilityChange(long j);

    public static final class AggregationSuggestionParameter {
        public final String kind;
        public final String value;

        public AggregationSuggestionParameter(String str, String str2) {
            this.kind = str;
            this.value = str2;
        }
    }

    protected static class NameMatchCandidate {
        int mLookupType;
        String mName;

        public NameMatchCandidate(String str, int i) {
            this.mName = str;
            this.mLookupType = i;
        }
    }

    protected static class MatchCandidateList {
        protected int mCount;
        protected final ArrayList<NameMatchCandidate> mList = new ArrayList<>();

        protected MatchCandidateList() {
        }

        public void add(String str, int i) {
            if (this.mCount >= this.mList.size()) {
                this.mList.add(new NameMatchCandidate(str, i));
            } else {
                NameMatchCandidate nameMatchCandidate = this.mList.get(this.mCount);
                nameMatchCandidate.mName = str;
                nameMatchCandidate.mLookupType = i;
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
        boolean isNameSuperPrimary;
        long rawContactId;
        boolean writableAccount;

        public DisplayNameCandidate() {
            clear();
        }

        public void clear() {
            this.rawContactId = -1L;
            this.displayName = null;
            this.displayNameSource = 0;
            this.isNameSuperPrimary = false;
            this.writableAccount = false;
        }
    }

    public AbstractContactAggregator(ContactsProvider2 contactsProvider2, ContactsDatabaseHelper contactsDatabaseHelper, PhotoPriorityResolver photoPriorityResolver, NameSplitter nameSplitter, CommonNicknameCache commonNicknameCache) {
        this.mContactsProvider = contactsProvider2;
        this.mDbHelper = contactsDatabaseHelper;
        this.mPhotoPriorityResolver = photoPriorityResolver;
        this.mNameSplitter = nameSplitter;
        this.mCommonNicknameCache = commonNicknameCache;
        SQLiteDatabase readableDatabase = this.mDbHelper.getReadableDatabase();
        this.mAggregatedPresenceReplace = readableDatabase.compileStatement("INSERT OR REPLACE INTO agg_presence(presence_contact_id, mode, chat_capability) SELECT presence_contact_id,mode,chat_capability FROM presence WHERE  (mode * 10 + chat_capability) = (SELECT MAX (mode * 10 + chat_capability) FROM presence WHERE presence_contact_id=?) AND presence_contact_id=?;");
        this.mRawContactCountQuery = readableDatabase.compileStatement("SELECT COUNT(_id) FROM raw_contacts WHERE contact_id=? AND _id<>?");
        this.mAggregatedPresenceDelete = readableDatabase.compileStatement("DELETE FROM agg_presence WHERE presence_contact_id=?");
        this.mMarkForAggregation = readableDatabase.compileStatement("UPDATE raw_contacts SET aggregation_needed=1 WHERE _id=? AND aggregation_needed=0");
        this.mPhotoIdUpdate = readableDatabase.compileStatement("UPDATE contacts SET photo_id=?,photo_file_id=?  WHERE _id=?");
        this.mDisplayNameUpdate = readableDatabase.compileStatement("UPDATE contacts SET name_raw_contact_id=?  WHERE _id=?");
        this.mLookupKeyUpdate = readableDatabase.compileStatement("UPDATE contacts SET lookup=?  WHERE _id=?");
        this.mStarredUpdate = readableDatabase.compileStatement("UPDATE contacts SET starred=(SELECT (CASE WHEN COUNT(starred)=0 THEN 0 ELSE 1 END) FROM raw_contacts WHERE contact_id=contacts._id AND starred=1) WHERE _id=?");
        this.mSendToVoicemailUpdate = readableDatabase.compileStatement("UPDATE contacts SET send_to_voicemail=(CASE WHEN (SELECT COUNT( send_to_voicemail) FROM raw_contacts WHERE contact_id=contacts._id AND send_to_voicemail=1) = (SELECT COUNT(send_to_voicemail) FROM raw_contacts WHERE contact_id=contacts._id) THEN 1 ELSE 0 END) WHERE _id=?");
        this.mPinnedUpdate = readableDatabase.compileStatement("UPDATE contacts SET pinned = IFNULL((SELECT MIN(pinned) FROM raw_contacts WHERE contact_id=contacts._id AND pinned>0),0) WHERE _id=?");
        this.mContactIdAndMarkAggregatedUpdate = readableDatabase.compileStatement("UPDATE raw_contacts SET contact_id=?, aggregation_needed=0 WHERE _id=?");
        this.mContactIdUpdate = readableDatabase.compileStatement("UPDATE raw_contacts SET contact_id=? WHERE _id=?");
        this.mPresenceContactIdUpdate = readableDatabase.compileStatement("UPDATE presence SET presence_contact_id=? WHERE presence_raw_contact_id=?");
        this.mContactUpdate = readableDatabase.compileStatement("UPDATE contacts SET name_raw_contact_id=?, photo_id=?, photo_file_id=?, send_to_voicemail=?, custom_ringtone=?, x_last_time_contacted=?, x_times_contacted=?, starred=?, pinned=?, has_phone_number=?, lookup=?, contact_last_updated_timestamp=?  WHERE _id=?");
        this.mContactInsert = readableDatabase.compileStatement("INSERT INTO contacts (name_raw_contact_id, photo_id, photo_file_id, send_to_voicemail, custom_ringtone, x_last_time_contacted, x_times_contacted, starred, pinned, has_phone_number, lookup, contact_last_updated_timestamp)  VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
        this.mResetPinnedForRawContact = readableDatabase.compileStatement("UPDATE raw_contacts SET pinned=0 WHERE _id=?");
        this.mMimeTypeIdEmail = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/email_v2");
        this.mMimeTypeIdIdentity = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/identity");
        this.mMimeTypeIdPhoto = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/photo");
        this.mMimeTypeIdPhone = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/phone_v2");
        this.mRawContactsQueryByRawContactId = String.format(Locale.US, "SELECT raw_contacts._id,display_name,display_name_source,accounts.account_type,accounts.account_name,accounts.data_set,sourceid,custom_ringtone,send_to_voicemail,x_last_time_contacted,x_times_contacted,starred,pinned,data._id,data.mimetype_id,is_super_primary,data14, EXISTS(SELECT 1  FROM data d  WHERE d.mimetype_id=%d  AND d.raw_contact_id=raw_contacts._id AND d.is_super_primary=1) FROM raw_contacts JOIN accounts ON (accounts._id=raw_contacts.account_id) LEFT OUTER JOIN data ON (data.raw_contact_id=raw_contacts._id AND ((mimetype_id=%d AND data15 NOT NULL) OR (mimetype_id=%d AND data1 NOT NULL))) WHERE raw_contacts._id=?", Long.valueOf(this.mDbHelper.getMimeTypeIdForStructuredName()), Long.valueOf(this.mMimeTypeIdPhoto), Long.valueOf(this.mMimeTypeIdPhone));
        this.mRawContactsQueryByContactId = String.format(Locale.US, "SELECT raw_contacts._id,display_name,display_name_source,accounts.account_type,accounts.account_name,accounts.data_set,sourceid,custom_ringtone,send_to_voicemail,x_last_time_contacted,x_times_contacted,starred,pinned,data._id,data.mimetype_id,is_super_primary,data14, EXISTS(SELECT 1  FROM data d  WHERE d.mimetype_id=%d  AND d.raw_contact_id=raw_contacts._id AND d.is_super_primary=1) FROM raw_contacts JOIN accounts ON (accounts._id=raw_contacts.account_id) LEFT OUTER JOIN data ON (data.raw_contact_id=raw_contacts._id AND ((mimetype_id=%d AND data15 NOT NULL) OR (mimetype_id=%d AND data1 NOT NULL))) WHERE contact_id=? AND deleted=0", Long.valueOf(this.mDbHelper.getMimeTypeIdForStructuredName()), Long.valueOf(this.mMimeTypeIdPhoto), Long.valueOf(this.mMimeTypeIdPhone));
    }

    public final void setEnabled(boolean z) {
        this.mEnabled = z;
    }

    public final boolean isEnabled() {
        return this.mEnabled;
    }

    public void aggregateInTransaction(TransactionContext transactionContext, SQLiteDatabase sQLiteDatabase) {
        String str;
        int size = this.mRawContactsMarkedForAggregation.size();
        if (size == 0) {
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (DEBUG_LOGGING) {
            Log.d("ContactAggregator", "aggregateInTransaction for " + size + " contacts");
        }
        EventLog.writeEvent(2747, Long.valueOf(jCurrentTimeMillis), Integer.valueOf(-size));
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT _id,contact_id, account_id FROM raw_contacts WHERE indicate_phone_or_sim_contact<0 AND _id IN(");
        Iterator<Long> it = this.mRawContactsMarkedForAggregation.keySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            long jLongValue = it.next().longValue();
            if (i > 0) {
                sb.append(',');
            }
            sb.append(jLongValue);
            i++;
        }
        sb.append(')');
        SQLiteDatabase sQLiteDatabase2 = sQLiteDatabase;
        Cursor cursorRawQuery = sQLiteDatabase2.rawQuery(sb.toString(), null);
        try {
            int count = cursorRawQuery.getCount();
            long[] jArr = new long[count];
            long[] jArr2 = new long[count];
            long[] jArr3 = new long[count];
            int i2 = 0;
            while (cursorRawQuery.moveToNext()) {
                jArr[i2] = cursorRawQuery.getLong(0);
                jArr2[i2] = cursorRawQuery.getLong(1);
                jArr3[i2] = cursorRawQuery.getLong(2);
                i2++;
            }
            cursorRawQuery.close();
            if (DEBUG_LOGGING) {
                Log.d("ContactAggregator", "aggregateInTransaction: initial query done.");
            }
            int i3 = 0;
            while (i3 < count) {
                aggregateContact(transactionContext, sQLiteDatabase2, jArr[i3], jArr3[i3], jArr2[i3], this.mCandidates);
                i3++;
                sQLiteDatabase2 = sQLiteDatabase;
                count = count;
                jArr = jArr;
                jArr2 = jArr2;
                jArr3 = jArr3;
            }
            int i4 = count;
            long jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
            EventLog.writeEvent(2747, Long.valueOf(jCurrentTimeMillis2), Integer.valueOf(i4));
            if (DEBUG_LOGGING) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Contact aggregation complete: ");
                sb2.append(i4);
                if (i4 == 0) {
                    str = "";
                } else {
                    str = ", " + (jCurrentTimeMillis2 / ((long) i4)) + " ms per raw contact";
                }
                sb2.append(str);
                Log.d("ContactAggregator", sb2.toString());
            }
        } catch (Throwable th) {
            cursorRawQuery.close();
            throw th;
        }
    }

    public final void triggerAggregation(TransactionContext transactionContext, long j) {
        if (!this.mEnabled) {
        }
        int aggregationMode = this.mDbHelper.getAggregationMode(j);
        switch (aggregationMode) {
            case 0:
                markForAggregation(j, aggregationMode, false);
                break;
            case 1:
                aggregateContact(transactionContext, this.mDbHelper.getWritableDatabase(), j);
                break;
            case 2:
                long contactId = this.mDbHelper.getContactId(j);
                if (contactId != 0) {
                    updateAggregateData(transactionContext, contactId);
                }
                break;
        }
    }

    public final void clearPendingAggregations() {
        this.mRawContactsMarkedForAggregation = new ArrayMap<>();
    }

    public final void markNewForAggregation(long j, int i) {
        this.mRawContactsMarkedForAggregation.put(Long.valueOf(j), Integer.valueOf(i));
    }

    public final void markForAggregation(long j, int i, boolean z) {
        if (!z && this.mRawContactsMarkedForAggregation.containsKey(Long.valueOf(j))) {
            if (i == 0) {
                i = this.mRawContactsMarkedForAggregation.get(Long.valueOf(j)).intValue();
            }
        } else {
            this.mMarkForAggregation.bindLong(1, j);
            this.mMarkForAggregation.execute();
        }
        this.mRawContactsMarkedForAggregation.put(Long.valueOf(j), Integer.valueOf(i));
    }

    protected final void markContactForAggregation(SQLiteDatabase sQLiteDatabase, long j) {
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = sQLiteDatabase.query("raw_contacts", RawContactIdAndAggregationModeQuery.COLUMNS, "contact_id=?", this.mSelectionArgs1, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                long j2 = cursorQuery.getLong(0);
                int i = cursorQuery.getInt(1);
                if (i == 0) {
                    markForAggregation(j2, i, true);
                }
            }
        } finally {
            cursorQuery.close();
        }
    }

    public final int markAllVisibleForAggregation(SQLiteDatabase sQLiteDatabase) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET aggregation_needed=1 WHERE contact_id IN default_directory AND aggregation_mode=0");
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id FROM raw_contacts WHERE aggregation_needed=1 AND deleted=0", null);
        try {
            int count = cursorRawQuery.getCount();
            cursorRawQuery.moveToPosition(-1);
            while (cursorRawQuery.moveToNext()) {
                this.mRawContactsMarkedForAggregation.put(Long.valueOf(cursorRawQuery.getLong(0)), 0);
            }
            cursorRawQuery.close();
            Log.i("ContactAggregator", "Marked all visible contacts for aggregation: " + count + " raw contacts, " + (System.currentTimeMillis() - jCurrentTimeMillis) + " ms");
            return count;
        } catch (Throwable th) {
            cursorRawQuery.close();
            throw th;
        }
    }

    public long onRawContactInsert(TransactionContext transactionContext, SQLiteDatabase sQLiteDatabase, long j) {
        long jInsertContact = insertContact(sQLiteDatabase, j);
        setContactId(j, jInsertContact);
        this.mDbHelper.updateContactVisible(transactionContext, jInsertContact);
        return jInsertContact;
    }

    protected final long insertContact(SQLiteDatabase sQLiteDatabase, long j) {
        this.mSelectionArgs1[0] = String.valueOf(j);
        computeAggregateData(sQLiteDatabase, this.mRawContactsQueryByRawContactId, this.mSelectionArgs1, this.mContactInsert);
        return this.mContactInsert.executeInsert();
    }

    public void aggregateContact(TransactionContext transactionContext, SQLiteDatabase sQLiteDatabase, long j) {
        long j2;
        long j3;
        if (!this.mEnabled) {
            return;
        }
        MatchCandidateList matchCandidateList = new MatchCandidateList();
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = sQLiteDatabase.query("raw_contacts", RawContactIdAndAccountQuery.COLUMNS, "_id=?", this.mSelectionArgs1, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                j3 = cursorQuery.getLong(0);
                j2 = cursorQuery.getLong(1);
            } else {
                j2 = 0;
                j3 = 0;
            }
            cursorQuery.close();
            aggregateContact(transactionContext, sQLiteDatabase, j, j2, j3, matchCandidateList);
        } catch (Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    public void updateAggregateData(TransactionContext transactionContext, long j) {
        if (!this.mEnabled) {
            return;
        }
        computeAggregateData(this.mDbHelper.getWritableDatabase(), j, this.mContactUpdate);
        this.mContactUpdate.bindLong(13, j);
        this.mContactUpdate.execute();
        this.mDbHelper.updateContactVisible(transactionContext, j);
        updateAggregatedStatusUpdate(j);
    }

    protected final void updateAggregatedStatusUpdate(long j) {
        this.mAggregatedPresenceReplace.bindLong(1, j);
        this.mAggregatedPresenceReplace.bindLong(2, j);
        this.mAggregatedPresenceReplace.execute();
        updateLastStatusUpdateId(j);
    }

    public final void updateLastStatusUpdateId(long j) {
        String strValueOf = String.valueOf(j);
        this.mDbHelper.getWritableDatabase().execSQL("UPDATE contacts SET status_update_id=(SELECT data._id FROM status_updates JOIN data   ON (status_update_data_id=data._id) JOIN raw_contacts   ON (data.raw_contact_id=raw_contacts._id) WHERE contact_id=? ORDER BY status_ts DESC,status LIMIT 1) WHERE contacts._id=?", new String[]{strValueOf, strValueOf});
    }

    protected final String buildIdentityMatchingSql(String str, String str2, boolean z, boolean z2) {
        String strValueOf = String.valueOf(this.mMimeTypeIdIdentity);
        String str3 = " FROM data AS d1 JOIN data AS d2 ON (d1.data1" + (z ? "=" : "!=") + " d2.data1 AND d1.data2 = d2.data2 ) WHERE d1.mimetype_id = " + strValueOf + " AND d2.mimetype_id = " + strValueOf + " AND d1.raw_contact_id IN (" + str + ") AND d2.raw_contact_id IN (" + str2 + ")";
        if (z2) {
            return "SELECT count(*) " + str3;
        }
        return "SELECT d1.raw_contact_id,d2.raw_contact_id" + str3;
    }

    protected final String buildEmailMatchingSql(String str, String str2, boolean z) {
        String strValueOf = String.valueOf(this.mMimeTypeIdEmail);
        String str3 = " FROM data AS d1 JOIN data AS d2 ON d1.data1= d2.data1 WHERE d1.mimetype_id = " + strValueOf + " AND d2.mimetype_id = " + strValueOf + " AND d1.raw_contact_id IN (" + str + ") AND d2.raw_contact_id IN (" + str2 + ")";
        if (z) {
            return "SELECT count(*) " + str3;
        }
        return "SELECT d1.raw_contact_id,d2.raw_contact_id" + str3;
    }

    protected final String buildPhoneMatchingSql(String str, String str2, boolean z) {
        String strValueOf = String.valueOf(this.mMimeTypeIdPhone);
        String str3 = " FROM phone_lookup AS p1 JOIN data AS d1 ON (d1._id=p1.data_id) JOIN phone_lookup AS p2 ON (p1.min_match=p2.min_match) JOIN data AS d2 ON (d2._id=p2.data_id) WHERE d1.mimetype_id = " + strValueOf + " AND d2.mimetype_id = " + strValueOf + " AND d1.raw_contact_id IN (" + str + ") AND d2.raw_contact_id IN (" + str2 + ") AND PHONE_NUMBERS_EQUAL(d1.data1,d2.data1," + String.valueOf(this.mDbHelper.getUseStrictPhoneNumberComparisonParameter()) + ")";
        if (z) {
            return "SELECT count(*) " + str3;
        }
        return "SELECT d1.raw_contact_id,d2.raw_contact_id" + str3;
    }

    protected final String buildExceptionMatchingSql(String str, String str2) {
        return "SELECT raw_contact_id1, raw_contact_id2 FROM agg_exceptions WHERE raw_contact_id1 IN (" + str + ") AND raw_contact_id2 IN (" + str2 + ") AND type=1";
    }

    protected final boolean isFirstColumnGreaterThanZero(SQLiteDatabase sQLiteDatabase, String str) {
        return DatabaseUtils.longForQuery(sQLiteDatabase, str, null) > 0;
    }

    protected final Set<Set<Long>> findConnectedRawContacts(SQLiteDatabase sQLiteDatabase, Set<Long> set) {
        HashMultimap hashMultimapCreate = HashMultimap.create();
        String strJoin = TextUtils.join(",", set);
        findIdPairs(sQLiteDatabase, buildExceptionMatchingSql(strJoin, strJoin), hashMultimapCreate);
        findIdPairs(sQLiteDatabase, buildIdentityMatchingSql(strJoin, strJoin, true, false), hashMultimapCreate);
        findIdPairs(sQLiteDatabase, buildEmailMatchingSql(strJoin, strJoin, false), hashMultimapCreate);
        findIdPairs(sQLiteDatabase, buildPhoneMatchingSql(strJoin, strJoin, false), hashMultimapCreate);
        return ContactAggregatorHelper.findConnectedComponents(set, hashMultimapCreate);
    }

    protected final void findIdPairs(SQLiteDatabase sQLiteDatabase, String str, Multimap<Long, Long> multimap) {
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery(str, null);
        try {
            cursorRawQuery.moveToPosition(-1);
            while (cursorRawQuery.moveToNext()) {
                long j = cursorRawQuery.getLong(0);
                long j2 = cursorRawQuery.getLong(1);
                if (j != j2) {
                    multimap.put(Long.valueOf(j), Long.valueOf(j2));
                    multimap.put(Long.valueOf(j2), Long.valueOf(j));
                }
            }
        } finally {
            cursorRawQuery.close();
        }
    }

    protected final void createContactForRawContacts(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, Set<Long> set, Long l) {
        if (set.isEmpty()) {
            return;
        }
        if (l == null) {
            this.mSelectionArgs1[0] = String.valueOf(set.iterator().next());
            computeAggregateData(sQLiteDatabase, this.mRawContactsQueryByRawContactId, this.mSelectionArgs1, this.mContactInsert);
            l = Long.valueOf(this.mContactInsert.executeInsert());
        }
        for (Long l2 : set) {
            setContactIdAndMarkAggregated(l2.longValue(), l.longValue());
            setPresenceContactId(l2.longValue(), l.longValue());
        }
        updateAggregateData(transactionContext, l.longValue());
    }

    protected final void setContactId(long j, long j2) {
        this.mContactIdUpdate.bindLong(1, j2);
        this.mContactIdUpdate.bindLong(2, j);
        this.mContactIdUpdate.execute();
    }

    protected final void markAggregated(SQLiteDatabase sQLiteDatabase, String str) {
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET aggregation_needed=0 WHERE _id in (" + str + ")");
    }

    private void setContactIdAndMarkAggregated(long j, long j2) {
        if (j2 == 0) {
            Slog.wtfStack("ContactAggregator", "Detected contact-id 0");
        }
        this.mContactIdAndMarkAggregatedUpdate.bindLong(1, j2);
        this.mContactIdAndMarkAggregatedUpdate.bindLong(2, j);
        this.mContactIdAndMarkAggregatedUpdate.execute();
    }

    private void setPresenceContactId(long j, long j2) {
        this.mPresenceContactIdUpdate.bindLong(1, j2);
        this.mPresenceContactIdUpdate.bindLong(2, j);
        this.mPresenceContactIdUpdate.execute();
    }

    public final void invalidateAggregationExceptionCache() {
        this.mAggregationExceptionIdsValid = false;
    }

    protected final void prefetchAggregationExceptionIds(SQLiteDatabase sQLiteDatabase) {
        this.mAggregationExceptionIds.clear();
        Cursor cursorQuery = sQLiteDatabase.query("agg_exceptions", AggregateExceptionPrefetchQuery.COLUMNS, null, null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                long j = cursorQuery.getLong(0);
                long j2 = cursorQuery.getLong(1);
                this.mAggregationExceptionIds.add(Long.valueOf(j));
                this.mAggregationExceptionIds.add(Long.valueOf(j2));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        this.mAggregationExceptionIdsValid = true;
    }

    protected final void loadNameMatchCandidates(SQLiteDatabase sQLiteDatabase, long j, MatchCandidateList matchCandidateList, boolean z) {
        matchCandidateList.clear();
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = sQLiteDatabase.query("name_lookup", NameLookupQuery.COLUMNS, z ? "raw_contact_id=? AND name_type IN (0,1,2)" : "raw_contact_id=?", this.mSelectionArgs1, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                matchCandidateList.add(cursorQuery.getString(0), cursorQuery.getInt(1));
            } finally {
                cursorQuery.close();
            }
        }
    }

    protected final class NameLookupSelectionBuilder extends NameLookupBuilder {
        private final MatchCandidateList mNameLookupCandidates;
        private StringBuilder mSelection;

        public NameLookupSelectionBuilder(NameSplitter nameSplitter, MatchCandidateList matchCandidateList) {
            super(nameSplitter);
            this.mSelection = new StringBuilder("normalized_name IN(");
            this.mNameLookupCandidates = matchCandidateList;
        }

        @Override
        protected String[] getCommonNicknameClusters(String str) {
            return AbstractContactAggregator.this.mCommonNicknameCache.getCommonNicknameClusters(str);
        }

        @Override
        protected void insertNameLookup(long j, long j2, int i, String str) {
            this.mNameLookupCandidates.add(str, i);
            DatabaseUtils.appendEscapedSQLString(this.mSelection, str);
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

        public int getLookupType(String str) {
            for (int i = 0; i < this.mNameLookupCandidates.mCount; i++) {
                if (this.mNameLookupCandidates.mList.get(i).mName.equals(str)) {
                    return this.mNameLookupCandidates.mList.get(i).mLookupType;
                }
            }
            throw new IllegalStateException();
        }
    }

    protected final void updateMatchScoresBasedOnNameMatches(SQLiteDatabase sQLiteDatabase, String str, MatchCandidateList matchCandidateList, ContactMatcher contactMatcher) {
        matchCandidateList.clear();
        NameLookupSelectionBuilder nameLookupSelectionBuilder = new NameLookupSelectionBuilder(this.mNameSplitter, matchCandidateList);
        nameLookupSelectionBuilder.insertNameLookup(0L, 0L, str, 0);
        if (nameLookupSelectionBuilder.isEmpty()) {
            return;
        }
        Cursor cursorQuery = sQLiteDatabase.query("name_lookup JOIN raw_contacts ON (raw_contact_id = raw_contacts._id)", NameLookupMatchQueryWithParameter.COLUMNS, nameLookupSelectionBuilder.getSelection(), null, null, null, null, PRIMARY_HIT_LIMIT_STRING);
        while (cursorQuery.moveToNext()) {
            try {
                long j = cursorQuery.getLong(1);
                String string = cursorQuery.getString(3);
                int lookupType = nameLookupSelectionBuilder.getLookupType(string);
                int i = cursorQuery.getInt(4);
                contactMatcher.matchName(j, lookupType, string, i, string, 0);
                if (lookupType == 3 && i == 3) {
                    contactMatcher.updateScoreWithNicknameMatch(j);
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    protected void computeAggregateData(SQLiteDatabase sQLiteDatabase, long j, SQLiteStatement sQLiteStatement) {
        this.mSelectionArgs1[0] = String.valueOf(j);
        computeAggregateData(sQLiteDatabase, this.mRawContactsQueryByContactId, this.mSelectionArgs1, sQLiteStatement);
    }

    private boolean hasHigherPhotoPriority(PhotoEntry photoEntry, int i, PhotoEntry photoEntry2, int i2) {
        int iCompareTo = photoEntry.compareTo(photoEntry2);
        return iCompareTo < 0 || (iCompareTo == 0 && i > i2);
    }

    protected final void computeAggregateData(SQLiteDatabase sQLiteDatabase, String str, String[] strArr, SQLiteStatement sQLiteStatement) {
        long j;
        boolean z;
        StringBuilder sb;
        long j2;
        int i;
        int i2;
        PhotoEntry photoEntry;
        int i3;
        int i4;
        long j3;
        int i5;
        int i6;
        StringBuilder sb2 = new StringBuilder();
        this.mDisplayNameCandidate.clear();
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery(str, strArr);
        String string = null;
        PhotoEntry photoEntry2 = null;
        int i7 = -1;
        long j4 = -1;
        int i8 = 0;
        int i9 = 0;
        long j5 = -1;
        int i10 = Integer.MAX_VALUE;
        int i11 = 0;
        long j6 = 0;
        boolean z2 = false;
        long j7 = 0;
        int i12 = 0;
        int i13 = 0;
        while (cursorRawQuery.moveToNext()) {
            try {
                long j8 = cursorRawQuery.getLong(0);
                if (j8 == j4) {
                    j = j5;
                    z = false;
                    sb = sb2;
                } else {
                    int i14 = i8 + 1;
                    String string2 = cursorRawQuery.getString(3);
                    String string3 = cursorRawQuery.getString(5);
                    if (!TextUtils.isEmpty(string3)) {
                        string2 = string2 + "/" + string3;
                    }
                    String str2 = string2;
                    String string4 = cursorRawQuery.getString(1);
                    int i15 = i9;
                    j = j5;
                    int iMin = i10;
                    processDisplayNameCandidate(j8, string4, cursorRawQuery.getInt(2), this.mContactsProvider.isWritableAccountWithDataSet(str2), cursorRawQuery.getInt(17) != 0);
                    if (!cursorRawQuery.isNull(8)) {
                        if (cursorRawQuery.getInt(8) != 0) {
                            i4 = i15 + 1;
                        }
                        if (string == null) {
                            string = cursorRawQuery.getString(7);
                        }
                        j3 = cursorRawQuery.getLong(9);
                        if (j3 > j6) {
                        }
                        i5 = cursorRawQuery.getInt(10);
                        if (i5 > i11) {
                        }
                        if (cursorRawQuery.getInt(11) != 0) {
                        }
                        i6 = cursorRawQuery.getInt(12);
                        if (i6 > 0) {
                        }
                        sb = sb2;
                        z = false;
                        appendLookupKey(sb2, str2, cursorRawQuery.getString(4), j8, cursorRawQuery.getString(6), string4);
                        i10 = iMin;
                        j4 = j8;
                        i9 = i4;
                        i8 = i14;
                    } else {
                        i4 = i15;
                        if (string == null && !cursorRawQuery.isNull(7)) {
                            string = cursorRawQuery.getString(7);
                        }
                        j3 = cursorRawQuery.getLong(9);
                        if (j3 > j6) {
                            j6 = j3;
                        }
                        i5 = cursorRawQuery.getInt(10);
                        if (i5 > i11) {
                            i11 = i5;
                        }
                        if (cursorRawQuery.getInt(11) != 0) {
                            i12 = 1;
                        }
                        i6 = cursorRawQuery.getInt(12);
                        if (i6 > 0) {
                            iMin = Math.min(iMin, i6);
                        }
                        sb = sb2;
                        z = false;
                        appendLookupKey(sb2, str2, cursorRawQuery.getString(4), j8, cursorRawQuery.getString(6), string4);
                        i10 = iMin;
                        j4 = j8;
                        i9 = i4;
                        i8 = i14;
                    }
                }
                if (cursorRawQuery.isNull(13)) {
                    j2 = j4;
                    i = i8;
                    i2 = i9;
                    photoEntry = photoEntry2;
                    i3 = i7;
                } else {
                    long j9 = cursorRawQuery.getLong(13);
                    long j10 = cursorRawQuery.getLong(16);
                    int i16 = cursorRawQuery.getInt(14);
                    j2 = j4;
                    boolean z3 = cursorRawQuery.getInt(15) != 0 ? true : z;
                    i = i8;
                    long j11 = i16;
                    i2 = i9;
                    if (j11 != this.mMimeTypeIdPhoto) {
                        photoEntry = photoEntry2;
                        i3 = i7;
                        if (j11 == this.mMimeTypeIdPhone) {
                            photoEntry2 = photoEntry;
                            i7 = i3;
                            j5 = j;
                            i13 = 1;
                            sb2 = sb;
                            j4 = j2;
                            i8 = i;
                            i9 = i2;
                        }
                    } else if (z2) {
                        photoEntry = photoEntry2;
                        i3 = i7;
                    } else {
                        PhotoEntry photoMetadata = getPhotoMetadata(sQLiteDatabase, j10);
                        int photoPriority = this.mPhotoPriorityResolver.getPhotoPriority(cursorRawQuery.getString(3));
                        if (z3) {
                            z2 |= z3;
                            j7 = j10;
                            j = j9;
                            photoEntry2 = photoMetadata;
                            i7 = photoPriority;
                            j5 = j;
                        } else {
                            PhotoEntry photoEntry3 = photoEntry2;
                            int i17 = i7;
                            if (hasHigherPhotoPriority(photoMetadata, photoPriority, photoEntry3, i17)) {
                                z2 |= z3;
                                j7 = j10;
                                j = j9;
                                photoEntry2 = photoMetadata;
                                i7 = photoPriority;
                                j5 = j;
                            } else {
                                photoMetadata = photoEntry3;
                                photoPriority = i17;
                                photoEntry2 = photoMetadata;
                                i7 = photoPriority;
                                j5 = j;
                            }
                        }
                        sb2 = sb;
                        j4 = j2;
                        i8 = i;
                        i9 = i2;
                    }
                }
                photoEntry2 = photoEntry;
                i7 = i3;
                j5 = j;
                sb2 = sb;
                j4 = j2;
                i8 = i;
                i9 = i2;
            } catch (Throwable th) {
                cursorRawQuery.close();
                throw th;
            }
        }
        int i18 = i9;
        long j12 = j5;
        int i19 = i10;
        StringBuilder sb3 = sb2;
        cursorRawQuery.close();
        if (i19 == Integer.MAX_VALUE) {
            i19 = 0;
        }
        sQLiteStatement.bindLong(1, this.mDisplayNameCandidate.rawContactId);
        if (j12 != -1) {
            sQLiteStatement.bindLong(2, j12);
        } else {
            sQLiteStatement.bindNull(2);
        }
        long j13 = j7;
        if (j13 != 0) {
            sQLiteStatement.bindLong(3, j13);
        } else {
            sQLiteStatement.bindNull(3);
        }
        sQLiteStatement.bindLong(4, i8 == i18 ? 1L : 0L);
        DatabaseUtils.bindObjectToProgram(sQLiteStatement, 5, string);
        sQLiteStatement.bindLong(6, j6);
        sQLiteStatement.bindLong(7, i11);
        sQLiteStatement.bindLong(8, i12);
        sQLiteStatement.bindLong(9, i19);
        sQLiteStatement.bindLong(10, i13);
        sQLiteStatement.bindString(11, Uri.encode(sb3.toString()));
        sQLiteStatement.bindLong(12, Clock.getInstance().currentTimeMillis());
    }

    protected void appendLookupKey(StringBuilder sb, String str, String str2, long j, String str3, String str4) {
        ContactLookupKey.appendToLookupKey(sb, str, str2, j, str3, str4);
    }

    private void processDisplayNameCandidate(long j, String str, int i, boolean z, boolean z2) {
        boolean z3 = true;
        if (this.mDisplayNameCandidate.rawContactId != -1 && (TextUtils.isEmpty(str) || (!z2 && (this.mDisplayNameCandidate.isNameSuperPrimary != z2 || (this.mDisplayNameCandidate.displayNameSource >= i && (this.mDisplayNameCandidate.displayNameSource != i || ((this.mDisplayNameCandidate.writableAccount || !z) && (this.mDisplayNameCandidate.writableAccount != z || NameNormalizer.compareComplexity(str, this.mDisplayNameCandidate.displayName) <= 0)))))))) {
            z3 = false;
        }
        if (z3) {
            this.mDisplayNameCandidate.rawContactId = j;
            this.mDisplayNameCandidate.displayName = str;
            this.mDisplayNameCandidate.displayNameSource = i;
            this.mDisplayNameCandidate.isNameSuperPrimary = z2;
            this.mDisplayNameCandidate.writableAccount = z;
        }
    }

    public final void updatePhotoId(SQLiteDatabase sQLiteDatabase, long j) {
        long contactId = this.mDbHelper.getContactId(j);
        if (contactId == 0) {
            return;
        }
        int i = -1;
        String str = "raw_contacts JOIN accounts ON (accounts._id=raw_contacts.account_id) JOIN data ON(data.raw_contact_id=raw_contacts._id AND (mimetype_id=" + this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/photo") + " AND data15 NOT NULL))";
        int i2 = 0;
        this.mSelectionArgs1[0] = String.valueOf(contactId);
        Cursor cursorQuery = sQLiteDatabase.query(str, PhotoIdQuery.COLUMNS, "contact_id=?", this.mSelectionArgs1, null, null, null);
        PhotoEntry photoEntry = null;
        long j2 = 0;
        long j3 = -1;
        while (true) {
            try {
                if (!cursorQuery.moveToNext()) {
                    break;
                }
                long j4 = cursorQuery.getLong(1);
                long j5 = cursorQuery.getLong(3);
                int i3 = cursorQuery.getInt(2) != 0 ? 1 : i2;
                PhotoEntry photoMetadata = getPhotoMetadata(sQLiteDatabase, j5);
                int photoPriority = this.mPhotoPriorityResolver.getPhotoPriority(cursorQuery.getString(i2));
                if (i3 != 0 || hasHigherPhotoPriority(photoMetadata, photoPriority, photoEntry, i)) {
                    if (i3 == 0) {
                        i = photoPriority;
                        photoEntry = photoMetadata;
                        j2 = j5;
                        j3 = j4;
                    } else {
                        j2 = j5;
                        j3 = j4;
                        break;
                    }
                }
                i2 = 0;
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        if (j3 == -1) {
            this.mPhotoIdUpdate.bindNull(1);
        } else {
            this.mPhotoIdUpdate.bindLong(1, j3);
        }
        if (j2 == 0) {
            this.mPhotoIdUpdate.bindNull(2);
        } else {
            this.mPhotoIdUpdate.bindLong(2, j2);
        }
        this.mPhotoIdUpdate.bindLong(3, contactId);
        this.mPhotoIdUpdate.execute();
    }

    private class PhotoEntry implements Comparable<PhotoEntry> {
        final int fileSize;
        final int pixelCount;

        private PhotoEntry(int i, int i2) {
            this.pixelCount = i;
            this.fileSize = i2;
        }

        @Override
        public int compareTo(PhotoEntry photoEntry) {
            if (photoEntry == null) {
                return -1;
            }
            if (this.pixelCount == photoEntry.pixelCount) {
                return photoEntry.fileSize - this.fileSize;
            }
            return photoEntry.pixelCount - this.pixelCount;
        }
    }

    private PhotoEntry getPhotoMetadata(SQLiteDatabase sQLiteDatabase, long j) {
        int i = 0;
        if (j == 0) {
            int maxThumbnailDim = this.mContactsProvider.getMaxThumbnailDim();
            return new PhotoEntry(maxThumbnailDim * maxThumbnailDim, i);
        }
        Cursor cursorQuery = sQLiteDatabase.query("photo_files", PhotoFileQuery.COLUMNS, "_id=?", new String[]{String.valueOf(j)}, null, null, null);
        try {
            if (cursorQuery.getCount() == 1) {
                cursorQuery.moveToFirst();
                return new PhotoEntry(cursorQuery.getInt(0) * cursorQuery.getInt(1), cursorQuery.getInt(2));
            }
            cursorQuery.close();
            return new PhotoEntry(i, i);
        } finally {
            cursorQuery.close();
        }
    }

    public final void updateDisplayNameForRawContact(SQLiteDatabase sQLiteDatabase, long j) {
        long contactId = this.mDbHelper.getContactId(j);
        if (contactId == 0) {
            return;
        }
        updateDisplayNameForContact(sQLiteDatabase, contactId);
    }

    public final void updateDisplayNameForContact(SQLiteDatabase sQLiteDatabase, long j) {
        this.mDisplayNameCandidate.clear();
        this.mSelectionArgs2[0] = String.valueOf(this.mDbHelper.getMimeTypeIdForStructuredName());
        this.mSelectionArgs2[1] = String.valueOf(j);
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id,display_name,display_name_source, EXISTS(SELECT 1  FROM data d  WHERE d.mimetype_id=?  AND d.raw_contact_id=view_raw_contacts._id AND d.is_super_primary=1),sourceid,account_type_and_data_set FROM view_raw_contacts WHERE contact_id=? ", this.mSelectionArgs2);
        boolean zIsNull = false;
        while (cursorRawQuery.moveToNext()) {
            try {
                processDisplayNameCandidate(cursorRawQuery.getLong(0), cursorRawQuery.getString(1), cursorRawQuery.getInt(2), this.mContactsProvider.isWritableAccountWithDataSet(cursorRawQuery.getString(5)), cursorRawQuery.getInt(3) != 0);
                zIsNull |= cursorRawQuery.isNull(4);
            } catch (Throwable th) {
                cursorRawQuery.close();
                throw th;
            }
        }
        cursorRawQuery.close();
        if (this.mDisplayNameCandidate.rawContactId != -1) {
            this.mDisplayNameUpdate.bindLong(1, this.mDisplayNameCandidate.rawContactId);
            this.mDisplayNameUpdate.bindLong(2, j);
            this.mDisplayNameUpdate.execute();
        }
        if (zIsNull) {
            updateLookupKeyForContact(sQLiteDatabase, j);
        }
    }

    public final void updateHasPhoneNumber(SQLiteDatabase sQLiteDatabase, long j) {
        long contactId = this.mDbHelper.getContactId(j);
        if (contactId == 0) {
            return;
        }
        SQLiteStatement sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("UPDATE contacts SET has_phone_number=(SELECT (CASE WHEN COUNT(*)=0 THEN 0 ELSE 1 END) FROM data JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) WHERE mimetype_id=? AND data1 NOT NULL AND contact_id=?) WHERE _id=?");
        try {
            sQLiteStatementCompileStatement.bindLong(1, this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/phone_v2"));
            sQLiteStatementCompileStatement.bindLong(2, contactId);
            sQLiteStatementCompileStatement.bindLong(3, contactId);
            sQLiteStatementCompileStatement.execute();
        } finally {
            sQLiteStatementCompileStatement.close();
        }
    }

    public final void updateLookupKeyForRawContact(SQLiteDatabase sQLiteDatabase, long j) {
        long contactId = this.mDbHelper.getContactId(j);
        if (contactId == 0) {
            return;
        }
        updateLookupKeyForContact(sQLiteDatabase, contactId);
    }

    private void updateLookupKeyForContact(SQLiteDatabase sQLiteDatabase, long j) {
        String strComputeLookupKeyForContact = computeLookupKeyForContact(sQLiteDatabase, j);
        if (strComputeLookupKeyForContact == null) {
            this.mLookupKeyUpdate.bindNull(1);
        } else {
            this.mLookupKeyUpdate.bindString(1, Uri.encode(strComputeLookupKeyForContact));
        }
        this.mLookupKeyUpdate.bindLong(2, j);
        this.mLookupKeyUpdate.execute();
    }

    protected String computeLookupKeyForContact(SQLiteDatabase sQLiteDatabase, long j) {
        StringBuilder sb = new StringBuilder();
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = sQLiteDatabase.query("view_raw_contacts", LookupKeyQuery.COLUMNS, "contact_id=?", this.mSelectionArgs1, null, null, "_id");
        while (cursorQuery.moveToNext()) {
            try {
                ContactLookupKey.appendToLookupKey(sb, cursorQuery.getString(2), cursorQuery.getString(3), cursorQuery.getLong(0), cursorQuery.getString(4), cursorQuery.getString(1));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    public final void updateStarred(long j) {
        long contactId = this.mDbHelper.getContactId(j);
        if (contactId == 0) {
            return;
        }
        this.mStarredUpdate.bindLong(1, contactId);
        this.mStarredUpdate.execute();
    }

    public final void updateSendToVoicemail(long j) {
        long contactId = this.mDbHelper.getContactId(j);
        if (contactId == 0) {
            return;
        }
        this.mSendToVoicemailUpdate.bindLong(1, contactId);
        this.mSendToVoicemailUpdate.execute();
    }

    public final void updatePinned(long j) {
        long contactId = this.mDbHelper.getContactId(j);
        if (contactId == 0) {
            return;
        }
        this.mPinnedUpdate.bindLong(1, contactId);
        this.mPinnedUpdate.execute();
    }

    public final Cursor queryAggregationSuggestions(SQLiteQueryBuilder sQLiteQueryBuilder, String[] strArr, long j, int i, String str, ArrayList<AggregationSuggestionParameter> arrayList) {
        SQLiteDatabase readableDatabase = this.mDbHelper.getReadableDatabase();
        readableDatabase.beginTransaction();
        try {
            List<MatchScore> listFindMatchingContacts = findMatchingContacts(readableDatabase, j, arrayList);
            ArrayList arrayList2 = new ArrayList();
            ArraySet arraySet = new ArraySet();
            for (MatchScore matchScore : listFindMatchingContacts) {
                long contactId = matchScore.getContactId();
                if (!arraySet.contains(Long.valueOf(contactId)) && contactId != j) {
                    arrayList2.add(matchScore);
                    arraySet.add(Long.valueOf(contactId));
                }
            }
            return queryMatchingContacts(sQLiteQueryBuilder, readableDatabase, strArr, arrayList2, i, str);
        } finally {
            readableDatabase.endTransaction();
        }
    }

    private Cursor queryMatchingContacts(SQLiteQueryBuilder sQLiteQueryBuilder, SQLiteDatabase sQLiteDatabase, String[] strArr, List<MatchScore> list, int i, String str) {
        List<MatchScore> listSubList = list;
        StringBuilder sb = new StringBuilder();
        sb.append("_id");
        sb.append(" IN (");
        for (int i2 = 0; i2 < list.size(); i2++) {
            MatchScore matchScore = listSubList.get(i2);
            if (i2 != 0) {
                sb.append(",");
            }
            sb.append(matchScore.getContactId());
        }
        sb.append(")");
        if (!TextUtils.isEmpty(str)) {
            sb.append(" AND _id IN ");
            this.mContactsProvider.appendContactFilterAsNestedQuery(sb, str);
        }
        sb.append(" AND indicate_phone_or_sim_contact < 0 ");
        ArraySet arraySet = new ArraySet();
        Cursor cursorQuery = sQLiteDatabase.query(sQLiteQueryBuilder.getTables(), ContactIdQuery.COLUMNS, sb.toString(), null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                arraySet.add(Long.valueOf(cursorQuery.getLong(0)));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        Iterator<MatchScore> it = list.iterator();
        while (it.hasNext()) {
            if (!arraySet.contains(Long.valueOf(it.next().getContactId()))) {
                it.remove();
            }
        }
        if (list.size() > i) {
            listSubList = listSubList.subList(0, i);
        }
        sb.setLength(0);
        sb.append("_id");
        sb.append(" IN (");
        for (int i3 = 0; i3 < listSubList.size(); i3++) {
            MatchScore matchScore2 = listSubList.get(i3);
            if (i3 != 0) {
                sb.append(",");
            }
            sb.append(matchScore2.getContactId());
        }
        sb.append(")");
        Cursor cursorQuery2 = sQLiteQueryBuilder.query(sQLiteDatabase, strArr, sb.toString(), null, null, null, "_id");
        ArrayList arrayList = new ArrayList(listSubList.size());
        Iterator<MatchScore> it2 = listSubList.iterator();
        while (it2.hasNext()) {
            arrayList.add(Long.valueOf(it2.next().getContactId()));
        }
        Collections.sort(arrayList);
        int[] iArr = new int[listSubList.size()];
        for (int i4 = 0; i4 < iArr.length; i4++) {
            iArr[i4] = arrayList.indexOf(Long.valueOf(listSubList.get(i4).getContactId()));
        }
        return new ReorderingCursorWrapper(cursorQuery2, iArr);
    }

    public Cursor queryAggregationSuggestions(SQLiteQueryBuilder sQLiteQueryBuilder, String[] strArr, long j, int i, String str, ArrayList<AggregationSuggestionParameter> arrayList, String str2) {
        SQLiteDatabase readableDatabase = this.mDbHelper.getReadableDatabase();
        readableDatabase.beginTransaction();
        try {
            return queryMatchingContacts(sQLiteQueryBuilder, readableDatabase, strArr, findMatchingContacts(readableDatabase, j, arrayList), i, str, str2);
        } finally {
            readableDatabase.endTransaction();
        }
    }

    private Cursor queryMatchingContacts(SQLiteQueryBuilder sQLiteQueryBuilder, SQLiteDatabase sQLiteDatabase, String[] strArr, List<MatchScore> list, int i, String str, String str2) {
        List<MatchScore> listSubList = list;
        StringBuilder sb = new StringBuilder();
        sb.append("_id");
        sb.append(" IN (");
        for (int i2 = 0; i2 < list.size(); i2++) {
            MatchScore matchScore = listSubList.get(i2);
            if (i2 != 0) {
                sb.append(",");
            }
            sb.append(matchScore.getContactId());
        }
        sb.append(")");
        if (!TextUtils.isEmpty(str2)) {
            sb.append(" AND ");
            sb.append(str2);
        }
        if (!TextUtils.isEmpty(str)) {
            sb.append(" AND _id IN ");
            this.mContactsProvider.appendContactFilterAsNestedQuery(sb, str);
        }
        HashSet hashSet = new HashSet();
        Cursor cursorQuery = sQLiteDatabase.query(sQLiteQueryBuilder.getTables(), ContactIdQuery.COLUMNS, sb.toString(), null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                hashSet.add(Long.valueOf(cursorQuery.getLong(0)));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        Iterator<MatchScore> it = list.iterator();
        while (it.hasNext()) {
            if (!hashSet.contains(Long.valueOf(it.next().getContactId()))) {
                it.remove();
            }
        }
        if (list.size() > i) {
            listSubList = listSubList.subList(0, i);
        }
        sb.setLength(0);
        sb.append("_id");
        sb.append(" IN (");
        for (int i3 = 0; i3 < listSubList.size(); i3++) {
            MatchScore matchScore2 = listSubList.get(i3);
            if (i3 != 0) {
                sb.append(",");
            }
            sb.append(matchScore2.getContactId());
        }
        sb.append(")");
        Cursor cursorQuery2 = sQLiteQueryBuilder.query(sQLiteDatabase, strArr, sb.toString(), null, null, null, "_id");
        ArrayList arrayList = new ArrayList(listSubList.size());
        Iterator<MatchScore> it2 = listSubList.iterator();
        while (it2.hasNext()) {
            arrayList.add(Long.valueOf(it2.next().getContactId()));
        }
        Collections.sort(arrayList);
        int[] iArr = new int[listSubList.size()];
        for (int i4 = 0; i4 < iArr.length; i4++) {
            iArr[i4] = arrayList.indexOf(Long.valueOf(listSubList.get(i4).getContactId()));
        }
        return new ReorderingCursorWrapper(cursorQuery2, iArr);
    }
}
