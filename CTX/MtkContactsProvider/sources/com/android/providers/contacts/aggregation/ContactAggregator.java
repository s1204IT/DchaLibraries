package com.android.providers.contacts.aggregation;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.PhotoPriorityResolver;
import com.android.providers.contacts.TransactionContext;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;
import com.android.providers.contacts.aggregation.util.ContactMatcher;
import com.android.providers.contacts.aggregation.util.MatchScore;
import com.android.providers.contacts.database.ContactsTableUtil;
import com.google.android.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ContactAggregator extends AbstractContactAggregator {
    private final ContactMatcher mMatcher;

    interface AggregateExceptionQuery {
        public static final String[] COLUMNS = {"type", "raw_contact_id1", "raw_contacts1.contact_id", "raw_contacts1.aggregation_needed", "raw_contacts2.contact_id", "raw_contacts2.aggregation_needed"};
    }

    private interface ContactNameLookupQuery {
        public static final String[] COLUMNS = {"contact_id", "normalized_name", "name_type"};
    }

    private interface IdentityLookupMatchQuery {
        public static final String[] COLUMNS = {"contact_id"};
    }

    private interface NameLookupMatchQuery {
        public static final String[] COLUMNS = {"contact_id", "nameA.normalized_name", "nameA.name_type", "nameB.name_type"};
    }

    public ContactAggregator(ContactsProvider2 contactsProvider2, ContactsDatabaseHelper contactsDatabaseHelper, PhotoPriorityResolver photoPriorityResolver, NameSplitter nameSplitter, CommonNicknameCache commonNicknameCache) {
        super(contactsProvider2, contactsDatabaseHelper, photoPriorityResolver, nameSplitter, commonNicknameCache);
        this.mMatcher = new ContactMatcher();
    }

    @Override
    synchronized void aggregateContact(TransactionContext transactionContext, SQLiteDatabase sQLiteDatabase, long j, long j2, long j3, AbstractContactAggregator.MatchCandidateList matchCandidateList) {
        int iIntValue;
        Collection<? extends Long> collection;
        long j4;
        boolean z;
        int i;
        long jSimpleQueryForLong;
        long j5;
        Set<Long> set;
        int i2;
        int iCanJoinIntoContact;
        Set<Long> set2;
        long j6 = j3;
        synchronized (this) {
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "aggregateContact: rid=" + j + " cid=" + j6);
            }
            Integer numRemove = this.mRawContactsMarkedForAggregation.remove(Long.valueOf(j));
            if (numRemove == null) {
                iIntValue = 0;
            } else {
                iIntValue = numRemove.intValue();
            }
            ContactMatcher contactMatcher = new ContactMatcher();
            Set<Long> arraySet = new ArraySet<>();
            Set<Long> arraySet2 = new ArraySet<>();
            if (iIntValue == 0) {
                matchCandidateList.clear();
                contactMatcher.clear();
                long jPickBestMatchBasedOnExceptions = pickBestMatchBasedOnExceptions(sQLiteDatabase, j, contactMatcher);
                if (jPickBestMatchBasedOnExceptions == -1) {
                    if (j6 == 0 || this.mDbHelper.isContactInDefaultDirectory(sQLiteDatabase, j6)) {
                        set = arraySet2;
                        jPickBestMatchBasedOnExceptions = pickBestMatchBasedOnData(sQLiteDatabase, j, matchCandidateList, contactMatcher);
                    } else {
                        set = arraySet2;
                    }
                    long j7 = jPickBestMatchBasedOnExceptions;
                    if (j7 != -1 && j7 != j6) {
                        this.mSelectionArgs2[0] = String.valueOf(j7);
                        this.mSelectionArgs2[1] = String.valueOf(j);
                        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id, account_id FROM raw_contacts WHERE contact_id=? AND _id!=?", this.mSelectionArgs2);
                        try {
                            cursorRawQuery.moveToPosition(-1);
                            while (cursorRawQuery.moveToNext()) {
                                long j8 = j7;
                                long j9 = cursorRawQuery.getLong(0);
                                if (cursorRawQuery.getLong(1) == j2) {
                                    arraySet.add(Long.valueOf(j9));
                                    set2 = set;
                                } else {
                                    set2 = set;
                                    set2.add(Long.valueOf(j9));
                                }
                                set = set2;
                                j7 = j8;
                            }
                            long j10 = j7;
                            Set<Long> set3 = set;
                            cursorRawQuery.close();
                            int size = arraySet.size() + set3.size();
                            if (size >= 50) {
                                if (VERBOSE_LOGGING) {
                                    Log.v("ContactAggregator", "Too many raw contacts (" + size + ") in the best matching contact, so skip aggregation");
                                }
                                collection = set3;
                                iCanJoinIntoContact = 0;
                                j4 = j10;
                                i2 = -1;
                            } else {
                                i2 = -1;
                                j4 = j10;
                                collection = set3;
                                iCanJoinIntoContact = canJoinIntoContact(sQLiteDatabase, j, arraySet, set3);
                            }
                            if (iCanJoinIntoContact != 0) {
                                z = iCanJoinIntoContact == i2;
                                if (j6 != 0) {
                                    i = 1;
                                    this.mRawContactCountQuery.bindLong(1, j6);
                                    this.mRawContactCountQuery.bindLong(2, j);
                                    jSimpleQueryForLong = this.mRawContactCountQuery.simpleQueryForLong();
                                } else {
                                    i = 1;
                                    jSimpleQueryForLong = 0;
                                }
                                j5 = (j4 == -1 && j6 != 0 && (jSimpleQueryForLong == 0 || iIntValue == 2)) ? j6 : j4;
                                if (j5 == j6) {
                                    markAggregated(sQLiteDatabase, String.valueOf(j));
                                    if (VERBOSE_LOGGING) {
                                        Log.v("ContactAggregator", "Aggregation unchanged");
                                    }
                                } else if (j5 == -1) {
                                    Long[] lArr = new Long[i];
                                    lArr[0] = Long.valueOf(j);
                                    createContactForRawContacts(sQLiteDatabase, transactionContext, Sets.newHashSet(lArr), null);
                                    if (jSimpleQueryForLong > 0) {
                                        updateAggregateData(transactionContext, j6);
                                    }
                                    if (VERBOSE_LOGGING) {
                                        Log.v("ContactAggregator", "create new contact for rid=" + j);
                                    }
                                } else if (z) {
                                    Set<Long> arraySet3 = new ArraySet<>();
                                    arraySet3.addAll(arraySet);
                                    arraySet3.addAll(collection);
                                    if (j6 == 0 || jSimpleQueryForLong != 0) {
                                        j6 = 0;
                                    }
                                    long j11 = j5;
                                    reAggregateRawContacts(transactionContext, sQLiteDatabase, j11, j6, j, arraySet3);
                                    if (VERBOSE_LOGGING) {
                                        Log.v("ContactAggregator", "Re-aggregating rid=" + j + " and cid=" + j11);
                                    }
                                } else {
                                    long j12 = j5;
                                    if (jSimpleQueryForLong == 0) {
                                        ContactsTableUtil.deleteContact(sQLiteDatabase, j6);
                                        this.mAggregatedPresenceDelete.bindLong(i, j6);
                                        this.mAggregatedPresenceDelete.execute();
                                    }
                                    clearSuperPrimarySetting(sQLiteDatabase, j12, j);
                                    setContactIdAndMarkAggregated(j, j12);
                                    computeAggregateData(sQLiteDatabase, j12, this.mContactUpdate);
                                    this.mContactUpdate.bindLong(13, j12);
                                    this.mContactUpdate.execute();
                                    this.mDbHelper.updateContactVisible(transactionContext, j12);
                                    updateAggregatedStatusUpdate(j12);
                                    if (j6 != 0) {
                                        updateAggregateData(transactionContext, j6);
                                    }
                                    if (VERBOSE_LOGGING) {
                                        Log.v("ContactAggregator", "Join rid=" + j + " with cid=" + j12);
                                    }
                                }
                            }
                            j4 = -1;
                            if (j6 != 0) {
                            }
                            if (j4 == -1) {
                            }
                            if (j5 == j6) {
                            }
                        } catch (Throwable th) {
                            cursorRawQuery.close();
                            throw th;
                        }
                    }
                    collection = set;
                    j4 = j7;
                } else {
                    collection = arraySet2;
                    j4 = jPickBestMatchBasedOnExceptions;
                }
            } else {
                collection = arraySet2;
                if (iIntValue == 3) {
                    return;
                } else {
                    j4 = -1;
                }
            }
            z = false;
            if (j6 != 0) {
            }
            if (j4 == -1) {
            }
            if (j5 == j6) {
            }
        }
    }

    private void clearSuperPrimarySetting(SQLiteDatabase sQLiteDatabase, long j, long j2) {
        String[] strArr = {String.valueOf(j), String.valueOf(j2)};
        StringBuilder sb = new StringBuilder();
        sb.append(" AND mimetype_id IN (");
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT DISTINCT(a.mimetype_id) FROM (SELECT mimetype_id FROM data WHERE is_super_primary =1 AND raw_contact_id IN (SELECT _id FROM raw_contacts WHERE contact_id=?1)) AS a JOIN  (SELECT mimetype_id FROM data WHERE is_super_primary =1 AND raw_contact_id=?2) AS b ON a.mimetype_id=b.mimetype_id", strArr);
        try {
            cursorRawQuery.moveToPosition(-1);
            int i = 0;
            while (cursorRawQuery.moveToNext()) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(cursorRawQuery.getLong(0));
                i++;
            }
            if (i == 0) {
                return;
            }
            sb.append(')');
            sQLiteDatabase.execSQL("UPDATE data SET is_super_primary=0 WHERE (raw_contact_id IN (SELECT _id FROM raw_contacts WHERE contact_id=?1) OR raw_contact_id=?2)" + sb.toString(), strArr);
        } finally {
            cursorRawQuery.close();
        }
    }

    private int canJoinIntoContact(SQLiteDatabase sQLiteDatabase, long j, Set<Long> set, Set<Long> set2) {
        if (set.isEmpty()) {
            String strValueOf = String.valueOf(j);
            String strJoin = TextUtils.join(",", set2);
            if (DatabaseUtils.longForQuery(sQLiteDatabase, buildIdentityMatchingSql(strValueOf, strJoin, true, true), null) == 0 && DatabaseUtils.longForQuery(sQLiteDatabase, buildIdentityMatchingSql(strValueOf, strJoin, false, true), null) > 0) {
                if (VERBOSE_LOGGING) {
                    Log.v("ContactAggregator", "canJoinIntoContact: no duplicates, but has no matching identity and has mis-matching identity on the same namespace between rid=" + strValueOf + " and ridsInOtherAccts=" + strJoin);
                }
                return 0;
            }
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "canJoinIntoContact: can join the first raw contact from the same account without any identity mismatch.");
            }
            return 1;
        }
        if (VERBOSE_LOGGING) {
            Log.v("ContactAggregator", "canJoinIntoContact: " + set.size() + " duplicate(s) found");
        }
        ArraySet arraySet = new ArraySet();
        arraySet.add(Long.valueOf(j));
        if (set.size() > 0 && isDataMaching(sQLiteDatabase, arraySet, set)) {
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "canJoinIntoContact: join if there is a data matching found in the same account");
            }
            return 1;
        }
        if (VERBOSE_LOGGING) {
            Log.v("ContactAggregator", "canJoinIntoContact: re-aggregate rid=" + j + " with its best matching contact to connected component");
            return -1;
        }
        return -1;
    }

    private boolean isDataMaching(SQLiteDatabase sQLiteDatabase, Set<Long> set, Set<Long> set2) {
        String strJoin = TextUtils.join(",", set);
        String strJoin2 = TextUtils.join(",", set2);
        if (isFirstColumnGreaterThanZero(sQLiteDatabase, buildIdentityMatchingSql(strJoin, strJoin2, true, true))) {
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "canJoinIntoContact: identity match found between " + strJoin + " and " + strJoin2);
            }
            return true;
        }
        if (isFirstColumnGreaterThanZero(sQLiteDatabase, buildEmailMatchingSql(strJoin, strJoin2, true))) {
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "canJoinIntoContact: email match found between " + strJoin + " and " + strJoin2);
            }
            return true;
        }
        if (isFirstColumnGreaterThanZero(sQLiteDatabase, buildPhoneMatchingSql(strJoin, strJoin2, true))) {
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "canJoinIntoContact: phone match found between " + strJoin + " and " + strJoin2);
            }
            return true;
        }
        return false;
    }

    private void reAggregateRawContacts(TransactionContext transactionContext, SQLiteDatabase sQLiteDatabase, long j, long j2, long j3, Set<Long> set) {
        Long lValueOf;
        ArraySet arraySet = new ArraySet();
        arraySet.add(Long.valueOf(j3));
        arraySet.addAll(set);
        Set<Set<Long>> setFindConnectedRawContacts = findConnectedRawContacts(sQLiteDatabase, arraySet);
        if (setFindConnectedRawContacts.size() == 1) {
            createContactForRawContacts(sQLiteDatabase, transactionContext, setFindConnectedRawContacts.iterator().next(), Long.valueOf(j));
            return;
        }
        Iterator<Set<Long>> it = setFindConnectedRawContacts.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Set<Long> next = it.next();
            if (next.contains(Long.valueOf(j3))) {
                if (j2 == 0) {
                    lValueOf = null;
                } else {
                    lValueOf = Long.valueOf(j2);
                }
                createContactForRawContacts(sQLiteDatabase, transactionContext, next, lValueOf);
                setFindConnectedRawContacts.remove(next);
            }
        }
        int size = setFindConnectedRawContacts.size();
        for (Set<Long> set2 : setFindConnectedRawContacts) {
            if (size > 1) {
                createContactForRawContacts(sQLiteDatabase, transactionContext, set2, null);
                size--;
            } else {
                createContactForRawContacts(sQLiteDatabase, transactionContext, set2, Long.valueOf(j));
            }
        }
    }

    @Override
    public void updateAggregationAfterVisibilityChange(long j) {
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        if (this.mDbHelper.isContactInDefaultDirectory(writableDatabase, j)) {
            markContactForAggregation(writableDatabase, j);
            return;
        }
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = writableDatabase.query("raw_contacts", AbstractContactAggregator.RawContactIdQuery.COLUMNS, "contact_id=?", this.mSelectionArgs1, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                long j2 = cursorQuery.getLong(0);
                this.mMatcher.clear();
                updateMatchScoresBasedOnIdentityMatch(writableDatabase, j2, this.mMatcher);
                updateMatchScoresBasedOnNameMatches(writableDatabase, j2, this.mMatcher);
                Iterator<MatchScore> it = this.mMatcher.pickBestMatches(70).iterator();
                while (it.hasNext()) {
                    markContactForAggregation(writableDatabase, it.next().getContactId());
                }
                this.mMatcher.clear();
                updateMatchScoresBasedOnEmailMatches(writableDatabase, j2, this.mMatcher);
                updateMatchScoresBasedOnPhoneMatches(writableDatabase, j2, this.mMatcher);
                Iterator<MatchScore> it2 = this.mMatcher.pickBestMatches(50).iterator();
                while (it2.hasNext()) {
                    markContactForAggregation(writableDatabase, it2.next().getContactId());
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void setContactIdAndMarkAggregated(long j, long j2) {
        this.mContactIdAndMarkAggregatedUpdate.bindLong(1, j2);
        this.mContactIdAndMarkAggregatedUpdate.bindLong(2, j);
        this.mContactIdAndMarkAggregatedUpdate.execute();
    }

    private long pickBestMatchBasedOnExceptions(SQLiteDatabase sQLiteDatabase, long j, ContactMatcher contactMatcher) {
        long j2;
        if (!this.mAggregationExceptionIdsValid) {
            prefetchAggregationExceptionIds(sQLiteDatabase);
        }
        if (!this.mAggregationExceptionIds.contains(Long.valueOf(j))) {
            return -1L;
        }
        Cursor cursorQuery = sQLiteDatabase.query("agg_exceptions JOIN raw_contacts raw_contacts1  ON (agg_exceptions.raw_contact_id1 = raw_contacts1._id)  JOIN raw_contacts raw_contacts2  ON (agg_exceptions.raw_contact_id2 = raw_contacts2._id) ", AggregateExceptionQuery.COLUMNS, "raw_contact_id1=" + j + " OR raw_contact_id2=" + j, null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                int i = cursorQuery.getInt(0);
                if (j == cursorQuery.getLong(1)) {
                    if (cursorQuery.getInt(5) == 0 && !cursorQuery.isNull(4)) {
                        j2 = cursorQuery.getLong(4);
                    } else {
                        j2 = -1;
                    }
                } else if (cursorQuery.getInt(3) == 0 && !cursorQuery.isNull(2)) {
                    j2 = cursorQuery.getLong(2);
                }
                if (j2 != -1) {
                    if (i == 1) {
                        contactMatcher.keepIn(j2);
                    } else {
                        contactMatcher.keepOut(j2);
                    }
                }
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return contactMatcher.pickBestMatch(100, true);
    }

    private long pickBestMatchBasedOnData(SQLiteDatabase sQLiteDatabase, long j, AbstractContactAggregator.MatchCandidateList matchCandidateList, ContactMatcher contactMatcher) {
        long jUpdateMatchScoresBasedOnDataMatches = updateMatchScoresBasedOnDataMatches(sQLiteDatabase, j, contactMatcher);
        if (jUpdateMatchScoresBasedOnDataMatches == -2) {
            return -1L;
        }
        if (jUpdateMatchScoresBasedOnDataMatches == -1) {
            jUpdateMatchScoresBasedOnDataMatches = pickBestMatchBasedOnSecondaryData(sQLiteDatabase, j, matchCandidateList, contactMatcher);
            if (jUpdateMatchScoresBasedOnDataMatches == -2) {
                return -1L;
            }
        }
        return jUpdateMatchScoresBasedOnDataMatches;
    }

    private long pickBestMatchBasedOnSecondaryData(SQLiteDatabase sQLiteDatabase, long j, AbstractContactAggregator.MatchCandidateList matchCandidateList, ContactMatcher contactMatcher) {
        List<Long> listPrepareSecondaryMatchCandidates = contactMatcher.prepareSecondaryMatchCandidates(70);
        if (listPrepareSecondaryMatchCandidates == null || listPrepareSecondaryMatchCandidates.size() > 20) {
            return -1L;
        }
        loadNameMatchCandidates(sQLiteDatabase, j, matchCandidateList, true);
        this.mSb.setLength(0);
        StringBuilder sb = this.mSb;
        sb.append("contact_id");
        sb.append(" IN (");
        for (int i = 0; i < listPrepareSecondaryMatchCandidates.size(); i++) {
            if (i != 0) {
                this.mSb.append(',');
            }
            this.mSb.append(listPrepareSecondaryMatchCandidates.get(i));
        }
        this.mSb.append(") AND name_type IN (0,1,2)");
        matchAllCandidates(sQLiteDatabase, this.mSb.toString(), matchCandidateList, contactMatcher, 1, null);
        return contactMatcher.pickBestMatch(50, false);
    }

    private long updateMatchScoresBasedOnDataMatches(SQLiteDatabase sQLiteDatabase, long j, ContactMatcher contactMatcher) {
        updateMatchScoresBasedOnIdentityMatch(sQLiteDatabase, j, contactMatcher);
        updateMatchScoresBasedOnNameMatches(sQLiteDatabase, j, contactMatcher);
        long jPickBestMatch = contactMatcher.pickBestMatch(70, false);
        if (jPickBestMatch != -1) {
            return jPickBestMatch;
        }
        updateMatchScoresBasedOnEmailMatches(sQLiteDatabase, j, contactMatcher);
        updateMatchScoresBasedOnPhoneMatches(sQLiteDatabase, j, contactMatcher);
        return -1L;
    }

    private void updateMatchScoresBasedOnIdentityMatch(SQLiteDatabase sQLiteDatabase, long j, ContactMatcher contactMatcher) {
        this.mSelectionArgs2[0] = String.valueOf(j);
        this.mSelectionArgs2[1] = String.valueOf(this.mMimeTypeIdIdentity);
        Cursor cursorQuery = sQLiteDatabase.query("data dataA JOIN data dataB ON (dataA.data2=dataB.data2 AND dataA.data1=dataB.data1) JOIN raw_contacts ON (dataB.raw_contact_id = raw_contacts._id)", IdentityLookupMatchQuery.COLUMNS, "dataA.raw_contact_id=?1 AND dataA.mimetype_id=?2 AND dataA.data2 NOT NULL AND dataA.data1 NOT NULL AND dataB.mimetype_id=?2 AND aggregation_needed=0 AND contact_id IN default_directory", this.mSelectionArgs2, "contact_id", null, null);
        while (cursorQuery.moveToNext()) {
            try {
                contactMatcher.matchIdentity(cursorQuery.getLong(0));
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void updateMatchScoresBasedOnNameMatches(SQLiteDatabase sQLiteDatabase, long j, ContactMatcher contactMatcher) {
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = sQLiteDatabase.query("name_lookup nameA JOIN name_lookup nameB ON (nameA.normalized_name=nameB.normalized_name) JOIN raw_contacts ON (nameB.raw_contact_id = raw_contacts._id)", NameLookupMatchQuery.COLUMNS, "nameA.raw_contact_id=? AND aggregation_needed=0 AND contact_id IN default_directory AND indicate_phone_or_sim_contact < 0 ", this.mSelectionArgs1, null, null, null, PRIMARY_HIT_LIMIT_STRING);
        while (cursorQuery.moveToNext()) {
            try {
                long j2 = cursorQuery.getLong(0);
                String string = cursorQuery.getString(1);
                int i = cursorQuery.getInt(2);
                int i2 = cursorQuery.getInt(3);
                contactMatcher.matchName(j2, i, string, i2, string, 0);
                if (i == 3 && i2 == 3) {
                    contactMatcher.updateScoreWithNicknameMatch(j2);
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void updateMatchScoresBasedOnEmailMatches(SQLiteDatabase sQLiteDatabase, long j, ContactMatcher contactMatcher) {
        this.mSelectionArgs2[0] = String.valueOf(j);
        this.mSelectionArgs2[1] = String.valueOf(this.mMimeTypeIdEmail);
        Cursor cursorQuery = sQLiteDatabase.query("data dataA JOIN data dataB ON dataA.data1= dataB.data1 JOIN raw_contacts ON (dataB.raw_contact_id = raw_contacts._id)", AbstractContactAggregator.EmailLookupQuery.COLUMNS, "dataA.raw_contact_id=?1 AND dataA.mimetype_id=?2 AND dataA.data1 NOT NULL AND dataB.mimetype_id=?2 AND aggregation_needed=0 AND contact_id IN default_directory", this.mSelectionArgs2, null, null, null, SECONDARY_HIT_LIMIT_STRING);
        while (cursorQuery.moveToNext()) {
            try {
                contactMatcher.updateScoreWithEmailMatch(cursorQuery.getLong(1));
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void updateMatchScoresBasedOnPhoneMatches(SQLiteDatabase sQLiteDatabase, long j, ContactMatcher contactMatcher) {
        this.mSelectionArgs2[0] = String.valueOf(j);
        this.mSelectionArgs2[1] = this.mDbHelper.getUseStrictPhoneNumberComparisonParameter();
        Cursor cursorQuery = sQLiteDatabase.query("phone_lookup phoneA JOIN data dataA ON (dataA._id=phoneA.data_id) JOIN phone_lookup phoneB ON (phoneA.min_match=phoneB.min_match) JOIN data dataB ON (dataB._id=phoneB.data_id) JOIN raw_contacts ON (dataB.raw_contact_id = raw_contacts._id)", AbstractContactAggregator.PhoneLookupQuery.COLUMNS, "dataA.raw_contact_id=? AND PHONE_NUMBERS_EQUAL(dataA.data1, dataB.data1,?) AND aggregation_needed=0 AND contact_id IN default_directory", this.mSelectionArgs2, null, null, null, SECONDARY_HIT_LIMIT_STRING);
        while (cursorQuery.moveToNext()) {
            try {
                contactMatcher.updateScoreWithPhoneNumberMatch(cursorQuery.getLong(1));
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void lookupApproximateNameMatches(SQLiteDatabase sQLiteDatabase, AbstractContactAggregator.MatchCandidateList matchCandidateList, ContactMatcher contactMatcher) {
        ArraySet arraySet = new ArraySet();
        for (int i = 0; i < matchCandidateList.mCount; i++) {
            AbstractContactAggregator.NameMatchCandidate nameMatchCandidate = matchCandidateList.mList.get(i);
            if (nameMatchCandidate.mName.length() >= 2) {
                String strSubstring = nameMatchCandidate.mName.substring(0, 2);
                if (!arraySet.contains(strSubstring)) {
                    arraySet.add(strSubstring);
                    matchAllCandidates(sQLiteDatabase, "(normalized_name GLOB '" + strSubstring + "*') AND (name_type IN(2,4,3)) AND contact_id IN default_directory", matchCandidateList, contactMatcher, 2, String.valueOf(100));
                }
            }
        }
    }

    private void matchAllCandidates(SQLiteDatabase sQLiteDatabase, String str, AbstractContactAggregator.MatchCandidateList matchCandidateList, ContactMatcher contactMatcher, int i, String str2) {
        Cursor cursorQuery = sQLiteDatabase.query("name_lookup INNER JOIN view_raw_contacts ON (name_lookup.raw_contact_id = view_raw_contacts._id)", ContactNameLookupQuery.COLUMNS, str, null, null, null, null, str2);
        while (cursorQuery.moveToNext()) {
            try {
                Long lValueOf = Long.valueOf(cursorQuery.getLong(0));
                String string = cursorQuery.getString(1);
                int i2 = cursorQuery.getInt(2);
                for (int i3 = 0; i3 < matchCandidateList.mCount; i3++) {
                    AbstractContactAggregator.NameMatchCandidate nameMatchCandidate = matchCandidateList.mList.get(i3);
                    contactMatcher.matchName(lValueOf.longValue(), nameMatchCandidate.mLookupType, nameMatchCandidate.mName, i2, string, i);
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    @Override
    protected List<MatchScore> findMatchingContacts(SQLiteDatabase sQLiteDatabase, long j, ArrayList<AbstractContactAggregator.AggregationSuggestionParameter> arrayList) {
        AbstractContactAggregator.MatchCandidateList matchCandidateList = new AbstractContactAggregator.MatchCandidateList();
        ContactMatcher contactMatcher = new ContactMatcher();
        contactMatcher.keepOut(j);
        if (arrayList != null && arrayList.size() != 0) {
            updateMatchScoresForSuggestionsBasedOnDataMatches(sQLiteDatabase, matchCandidateList, contactMatcher, arrayList);
        } else {
            SQLiteDatabase sQLiteDatabase2 = sQLiteDatabase;
            Cursor cursorQuery = sQLiteDatabase2.query("raw_contacts", AbstractContactAggregator.RawContactIdQuery.COLUMNS, "contact_id=" + j, null, null, null, null);
            while (cursorQuery.moveToNext()) {
                try {
                    updateMatchScoresForSuggestionsBasedOnDataMatches(sQLiteDatabase2, cursorQuery.getLong(0), matchCandidateList, contactMatcher);
                    sQLiteDatabase2 = sQLiteDatabase;
                } finally {
                    cursorQuery.close();
                }
            }
        }
        return contactMatcher.pickBestMatches(50);
    }

    private void updateMatchScoresForSuggestionsBasedOnDataMatches(SQLiteDatabase sQLiteDatabase, long j, AbstractContactAggregator.MatchCandidateList matchCandidateList, ContactMatcher contactMatcher) {
        updateMatchScoresBasedOnIdentityMatch(sQLiteDatabase, j, contactMatcher);
        updateMatchScoresBasedOnNameMatches(sQLiteDatabase, j, contactMatcher);
        updateMatchScoresBasedOnEmailMatches(sQLiteDatabase, j, contactMatcher);
        updateMatchScoresBasedOnPhoneMatches(sQLiteDatabase, j, contactMatcher);
        loadNameMatchCandidates(sQLiteDatabase, j, matchCandidateList, false);
        lookupApproximateNameMatches(sQLiteDatabase, matchCandidateList, contactMatcher);
    }

    private void updateMatchScoresForSuggestionsBasedOnDataMatches(SQLiteDatabase sQLiteDatabase, AbstractContactAggregator.MatchCandidateList matchCandidateList, ContactMatcher contactMatcher, ArrayList<AbstractContactAggregator.AggregationSuggestionParameter> arrayList) {
        for (AbstractContactAggregator.AggregationSuggestionParameter aggregationSuggestionParameter : arrayList) {
            if ("name".equals(aggregationSuggestionParameter.kind)) {
                updateMatchScoresBasedOnNameMatches(sQLiteDatabase, aggregationSuggestionParameter.value, matchCandidateList, contactMatcher);
            }
        }
    }
}
