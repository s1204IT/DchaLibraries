package com.android.providers.contacts.aggregation;

import android.database.Cursor;
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
import com.android.providers.contacts.aggregation.util.ContactAggregatorHelper;
import com.android.providers.contacts.aggregation.util.MatchScore;
import com.android.providers.contacts.aggregation.util.RawContactMatcher;
import com.android.providers.contacts.aggregation.util.RawContactMatchingCandidates;
import com.android.providers.contacts.database.ContactsTableUtil;
import com.google.android.collect.Sets;
import com.google.common.collect.HashMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContactAggregator2 extends AbstractContactAggregator {
    private final RawContactMatcher mMatcher;

    private interface ContactNameLookupQuery {
        public static final String[] COLUMNS = {"_id", "contact_id", "account_id", "normalized_name", "name_type"};
    }

    protected interface EmailLookupQuery {
        public static final String[] COLUMNS = {"raw_contacts._id", "contact_id", "account_id"};
    }

    protected interface IdentityLookupMatchQuery {
        public static final String[] COLUMNS = {"raw_contacts._id", "contact_id", "account_id"};
    }

    protected interface NameLookupMatchQuery {
        public static final String[] COLUMNS = {"_id", "contact_id", "account_id", "nameA.normalized_name", "nameA.name_type", "nameB.name_type"};
    }

    protected interface NullNameRawContactsIdsQuery {
        public static final String[] COLUMNS = {"_id", "contact_id", "account_id", "normalized_name"};
    }

    protected interface PhoneLookupQuery {
        public static final String[] COLUMNS = {"raw_contacts._id", "contact_id", "account_id"};
    }

    public ContactAggregator2(ContactsProvider2 contactsProvider2, ContactsDatabaseHelper contactsDatabaseHelper, PhotoPriorityResolver photoPriorityResolver, NameSplitter nameSplitter, CommonNicknameCache commonNicknameCache) {
        super(contactsProvider2, contactsDatabaseHelper, photoPriorityResolver, nameSplitter, commonNicknameCache);
        this.mMatcher = new RawContactMatcher();
    }

    @Override
    synchronized void aggregateContact(TransactionContext transactionContext, SQLiteDatabase sQLiteDatabase, long j, long j2, long j3, AbstractContactAggregator.MatchCandidateList matchCandidateList) {
        int iIntValue;
        long jSimpleQueryForLong;
        byte b;
        if (!needAggregate(sQLiteDatabase, j)) {
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "Skip rid=" + j + " which has already been aggregated.");
            }
            return;
        }
        if (VERBOSE_LOGGING) {
            Log.v("ContactAggregator", "aggregateContact: rid=" + j + " cid=" + j3);
        }
        Integer numRemove = this.mRawContactsMarkedForAggregation.remove(Long.valueOf(j));
        if (numRemove == null) {
            iIntValue = 0;
        } else {
            iIntValue = numRemove.intValue();
        }
        RawContactMatcher rawContactMatcher = new RawContactMatcher();
        RawContactMatchingCandidates rawContactMatchingCandidates = new RawContactMatchingCandidates();
        if (iIntValue == 0) {
            if (j3 == 0 || this.mDbHelper.isContactInDefaultDirectory(sQLiteDatabase, j3)) {
                rawContactMatchingCandidates = findRawContactMatchingCandidates(sQLiteDatabase, j, matchCandidateList, rawContactMatcher);
            }
        } else if (iIntValue == 3) {
            return;
        }
        RawContactMatchingCandidates rawContactMatchingCandidates2 = rawContactMatchingCandidates;
        if (j3 == 0) {
            jSimpleQueryForLong = 0;
        } else {
            this.mRawContactCountQuery.bindLong(1, j3);
            this.mRawContactCountQuery.bindLong(2, j);
            jSimpleQueryForLong = this.mRawContactCountQuery.simpleQueryForLong();
        }
        int count = rawContactMatchingCandidates2.getCount();
        if (count >= 50) {
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "Too many matching raw contacts (" + count + ") are found, so skip aggregation");
            }
        } else {
            if (count > 0) {
                b = -1;
            } else if (j3 == 0 || (jSimpleQueryForLong != 0 && iIntValue != 2)) {
                b = 1;
            }
            if (b != 0) {
                if (VERBOSE_LOGGING) {
                    Log.v("ContactAggregator", "Aggregation unchanged");
                }
                markAggregated(sQLiteDatabase, String.valueOf(j));
            } else if (b == 1) {
                if (VERBOSE_LOGGING) {
                    Log.v("ContactAggregator", "create new contact for rid=" + j);
                }
                createContactForRawContacts(sQLiteDatabase, transactionContext, Sets.newHashSet(new Long[]{Long.valueOf(j)}), null);
                if (jSimpleQueryForLong > 0) {
                    updateAggregateData(transactionContext, j3);
                }
                markAggregated(sQLiteDatabase, String.valueOf(j));
            } else {
                if (VERBOSE_LOGGING) {
                    Log.v("ContactAggregator", "Re-aggregating rids=" + j + "," + TextUtils.join(",", rawContactMatchingCandidates2.getRawContactIdSet()));
                }
                reAggregateRawContacts(transactionContext, sQLiteDatabase, j3, j, j2, jSimpleQueryForLong, rawContactMatchingCandidates2);
            }
        }
        b = 0;
        if (b != 0) {
        }
    }

    private boolean needAggregate(SQLiteDatabase sQLiteDatabase, long j) {
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id FROM raw_contacts WHERE aggregation_needed=1 AND _id=?", this.mSelectionArgs1);
        try {
            return cursorRawQuery.getCount() != 0;
        } finally {
            cursorRawQuery.close();
        }
    }

    private RawContactMatchingCandidates findRawContactMatchingCandidates(SQLiteDatabase sQLiteDatabase, long j, AbstractContactAggregator.MatchCandidateList matchCandidateList, RawContactMatcher rawContactMatcher) {
        updateMatchScores(sQLiteDatabase, j, matchCandidateList, rawContactMatcher);
        RawContactMatchingCandidates rawContactMatchingCandidates = new RawContactMatchingCandidates(rawContactMatcher.pickBestMatches());
        ArraySet arraySet = new ArraySet();
        arraySet.addAll(rawContactMatchingCandidates.getRawContactIdSet());
        while (!arraySet.isEmpty()) {
            if (rawContactMatchingCandidates.getCount() >= 50) {
                return rawContactMatchingCandidates;
            }
            ArraySet arraySet2 = new ArraySet();
            Iterator it = arraySet.iterator();
            while (it.hasNext()) {
                long jLongValue = ((Long) it.next()).longValue();
                RawContactMatcher rawContactMatcher2 = new RawContactMatcher();
                updateMatchScores(sQLiteDatabase, jLongValue, new AbstractContactAggregator.MatchCandidateList(), rawContactMatcher2);
                for (MatchScore matchScore : rawContactMatcher2.pickBestMatches()) {
                    long rawContactId = matchScore.getRawContactId();
                    if (!rawContactMatchingCandidates.getRawContactIdSet().contains(Long.valueOf(rawContactId))) {
                        arraySet2.add(Long.valueOf(rawContactId));
                        rawContactMatchingCandidates.add(matchScore);
                    }
                }
            }
            arraySet.clear();
            arraySet.addAll((Collection) arraySet2);
        }
        return rawContactMatchingCandidates;
    }

    private void clearSuperPrimarySetting(SQLiteDatabase sQLiteDatabase, String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(" AND mimetype_id IN (");
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT mimetype_id, count(1) c  FROM data WHERE is_super_primary = 1 AND raw_contact_id IN (" + str + ") group by mimetype_id HAVING c > 1", null);
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
            sQLiteDatabase.execSQL(("UPDATE data SET is_super_primary=0 WHERE raw_contact_id IN (" + str + ")") + sb.toString());
        } finally {
            cursorRawQuery.close();
        }
    }

    private String buildExceptionMatchingSql(String str, String str2, int i, boolean z) {
        String str3 = " FROM agg_exceptions WHERE raw_contact_id1 IN (" + str + ") AND raw_contact_id2 IN (" + str2 + ") AND type=" + i;
        if (z) {
            return "SELECT count(*) " + str3;
        }
        return "SELECT raw_contact_id1, raw_contact_id2" + str3;
    }

    private void reAggregateRawContacts(TransactionContext transactionContext, SQLiteDatabase sQLiteDatabase, long j, long j2, long j3, long j4, RawContactMatchingCandidates rawContactMatchingCandidates) {
        Long lValueOf;
        long jSimpleQueryForLong;
        ArraySet arraySet = new ArraySet();
        arraySet.add(Long.valueOf(j2));
        arraySet.addAll(rawContactMatchingCandidates.getRawContactIdSet());
        Set<Set<Long>> setFindConnectedRawContacts = findConnectedRawContacts(sQLiteDatabase, arraySet);
        Map<Long, Long> rawContactToAccount = rawContactMatchingCandidates.getRawContactToAccount();
        rawContactToAccount.put(Long.valueOf(j2), Long.valueOf(j3));
        ContactAggregatorHelper.mergeComponentsWithDisjointAccounts(setFindConnectedRawContacts, rawContactToAccount);
        breakComponentsByExceptions(sQLiteDatabase, setFindConnectedRawContacts);
        for (Set<Long> set : setFindConnectedRawContacts) {
            ArraySet<Long> arraySet2 = new ArraySet();
            if (set.contains(Long.valueOf(j2))) {
                if ((j != 0 && j4 == 0) || canBeReused(sQLiteDatabase, Long.valueOf(j), set)) {
                    lValueOf = Long.valueOf(j);
                    Iterator<Long> it = set.iterator();
                    while (it.hasNext()) {
                        Long contactId = rawContactMatchingCandidates.getContactId(it.next());
                        if (contactId != null && !contactId.equals(lValueOf)) {
                            arraySet2.add(contactId);
                        }
                    }
                } else {
                    if (j != 0) {
                        arraySet2.add(Long.valueOf(j));
                    }
                    lValueOf = null;
                }
            } else {
                Iterator<Long> it2 = set.iterator();
                boolean z = false;
                lValueOf = null;
                while (it2.hasNext()) {
                    Long contactId2 = rawContactMatchingCandidates.getContactId(it2.next());
                    if (z || contactId2 == null || !canBeReused(sQLiteDatabase, contactId2, set)) {
                        arraySet2.add(contactId2);
                    } else {
                        lValueOf = contactId2;
                        z = true;
                    }
                }
            }
            String strJoin = TextUtils.join(",", set);
            clearSuperPrimarySetting(sQLiteDatabase, strJoin);
            createContactForRawContacts(sQLiteDatabase, transactionContext, set, lValueOf);
            if (VERBOSE_LOGGING) {
                Log.v("ContactAggregator", "Aggregating rids=" + set);
            }
            markAggregated(sQLiteDatabase, strJoin);
            for (Long l : arraySet2) {
                long j5 = 0;
                if (l.longValue() != 0) {
                    this.mRawContactCountQuery.bindLong(1, l.longValue());
                    j5 = 0;
                    this.mRawContactCountQuery.bindLong(2, 0L);
                    jSimpleQueryForLong = this.mRawContactCountQuery.simpleQueryForLong();
                } else {
                    jSimpleQueryForLong = 0;
                }
                if (jSimpleQueryForLong == j5) {
                    ContactsTableUtil.deleteContact(sQLiteDatabase, l.longValue());
                    this.mAggregatedPresenceDelete.bindLong(1, l.longValue());
                    this.mAggregatedPresenceDelete.execute();
                } else {
                    updateAggregateData(transactionContext, l.longValue());
                }
            }
        }
    }

    private boolean canBeReused(SQLiteDatabase sQLiteDatabase, Long l, Set<Long> set) {
        this.mSelectionArgs1[0] = String.valueOf(l);
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT raw_contacts._id FROM raw_contacts WHERE contact_id=? AND deleted=0", this.mSelectionArgs1);
        try {
            cursorRawQuery.moveToPosition(-1);
            while (cursorRawQuery.moveToNext()) {
                if (!set.contains(Long.valueOf(cursorRawQuery.getLong(0)))) {
                    return false;
                }
            }
            cursorRawQuery.close();
            return true;
        } finally {
            cursorRawQuery.close();
        }
    }

    private void breakComponentsByExceptions(SQLiteDatabase sQLiteDatabase, Set<Set<Long>> set) {
        for (Set set2 : new ArraySet(set)) {
            String strJoin = TextUtils.join(",", set2);
            if (isFirstColumnGreaterThanZero(sQLiteDatabase, buildExceptionMatchingSql(strJoin, strJoin, 2, true))) {
                HashMultimap hashMultimapCreate = HashMultimap.create();
                findIdPairs(sQLiteDatabase, buildExceptionMatchingSql(strJoin, strJoin), hashMultimapCreate);
                set.remove(set2);
                set.addAll(ContactAggregatorHelper.findConnectedComponents(set2, hashMultimapCreate));
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

    private void updateMatchScoresBasedOnExceptions(SQLiteDatabase sQLiteDatabase, long j, RawContactMatcher rawContactMatcher) {
        long j2;
        long j3;
        long j4;
        if (!this.mAggregationExceptionIdsValid) {
            prefetchAggregationExceptionIds(sQLiteDatabase);
        }
        if (!this.mAggregationExceptionIds.contains(Long.valueOf(j))) {
            return;
        }
        Cursor cursorQuery = sQLiteDatabase.query("agg_exceptions JOIN raw_contacts raw_contacts1  ON (agg_exceptions.raw_contact_id1 = raw_contacts1._id)  JOIN raw_contacts raw_contacts2  ON (agg_exceptions.raw_contact_id2 = raw_contacts2._id) ", AbstractContactAggregator.AggregateExceptionQuery.COLUMNS, "raw_contact_id1=" + j + " OR raw_contact_id2=" + j, null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                int i = cursorQuery.getInt(0);
                if (j == cursorQuery.getLong(1)) {
                    if (!cursorQuery.isNull(5)) {
                        j2 = cursorQuery.getLong(5);
                        j3 = cursorQuery.getLong(6);
                        j4 = cursorQuery.getLong(7);
                    } else {
                        j2 = -1;
                        j3 = -1;
                        j4 = -1;
                    }
                } else if (!cursorQuery.isNull(1)) {
                    j2 = cursorQuery.getLong(1);
                    j3 = cursorQuery.getLong(2);
                    j4 = cursorQuery.getLong(3);
                }
                if (j2 != -1) {
                    if (i == 1) {
                        rawContactMatcher.keepIn(j2, j3, j4);
                    } else {
                        rawContactMatcher.keepOut(j2, j3, j4);
                    }
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void updateMatchScoresBasedOnIdentityMatch(SQLiteDatabase sQLiteDatabase, long j, RawContactMatcher rawContactMatcher) {
        this.mSelectionArgs2[0] = String.valueOf(j);
        this.mSelectionArgs2[1] = String.valueOf(this.mMimeTypeIdIdentity);
        Cursor cursorQuery = sQLiteDatabase.query("data dataA JOIN data dataB ON (dataA.data2=dataB.data2 AND dataA.data1=dataB.data1) JOIN raw_contacts ON (dataB.raw_contact_id = raw_contacts._id)", IdentityLookupMatchQuery.COLUMNS, "dataA.raw_contact_id=?1 AND dataA.mimetype_id=?2 AND dataA.data2 NOT NULL AND dataA.data1 NOT NULL AND dataB.mimetype_id=?2 AND contact_id IN default_directory", this.mSelectionArgs2, "contact_id", null, null);
        while (cursorQuery.moveToNext()) {
            try {
                long j2 = cursorQuery.getLong(0);
                if (j2 != j) {
                    rawContactMatcher.matchIdentity(j2, cursorQuery.getLong(1), cursorQuery.getLong(2));
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void updateMatchScoresBasedOnNameMatches(SQLiteDatabase sQLiteDatabase, long j, RawContactMatcher rawContactMatcher) {
        int i = 0;
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = sQLiteDatabase.query("name_lookup nameA JOIN name_lookup nameB ON (nameA.normalized_name=nameB.normalized_name) JOIN raw_contacts ON (nameB.raw_contact_id = raw_contacts._id)", NameLookupMatchQuery.COLUMNS, "nameA.raw_contact_id=? AND contact_id IN default_directory AND indicate_phone_or_sim_contact < 0 ", this.mSelectionArgs1, null, null, null, PRIMARY_HIT_LIMIT_STRING);
        while (cursorQuery.moveToNext()) {
            try {
                long j2 = cursorQuery.getLong(i);
                if (j2 != j) {
                    long j3 = cursorQuery.getLong(1);
                    long j4 = cursorQuery.getLong(2);
                    String string = cursorQuery.getString(3);
                    int i2 = cursorQuery.getInt(4);
                    int i3 = cursorQuery.getInt(5);
                    rawContactMatcher.matchName(j2, j3, j4, i2, string, i3, string, 0);
                    if (i2 == 3 && i3 == 3) {
                        rawContactMatcher.updateScoreWithNicknameMatch(j2, j3, j4);
                    }
                    i = 0;
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void updateMatchScoresBasedOnEmailMatches(SQLiteDatabase sQLiteDatabase, long j, RawContactMatcher rawContactMatcher) {
        this.mSelectionArgs2[0] = String.valueOf(j);
        this.mSelectionArgs2[1] = String.valueOf(this.mMimeTypeIdEmail);
        Cursor cursorQuery = sQLiteDatabase.query("data dataA JOIN data dataB ON dataA.data1= dataB.data1 JOIN raw_contacts ON (dataB.raw_contact_id = raw_contacts._id)", EmailLookupQuery.COLUMNS, "dataA.raw_contact_id=?1 AND dataA.mimetype_id=?2 AND dataA.data1 NOT NULL AND dataB.mimetype_id=?2 AND contact_id IN default_directory", this.mSelectionArgs2, null, null, null, SECONDARY_HIT_LIMIT_STRING);
        while (cursorQuery.moveToNext()) {
            try {
                long j2 = cursorQuery.getLong(0);
                if (j2 != j) {
                    rawContactMatcher.updateScoreWithEmailMatch(j2, cursorQuery.getLong(1), cursorQuery.getLong(2));
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void updateMatchScoresBasedOnNameMatches(SQLiteDatabase sQLiteDatabase, String str, AbstractContactAggregator.MatchCandidateList matchCandidateList, RawContactMatcher rawContactMatcher) {
        matchCandidateList.clear();
        AbstractContactAggregator.NameLookupSelectionBuilder nameLookupSelectionBuilder = new AbstractContactAggregator.NameLookupSelectionBuilder(this.mNameSplitter, matchCandidateList);
        nameLookupSelectionBuilder.insertNameLookup(0L, 0L, str, 0);
        if (nameLookupSelectionBuilder.isEmpty()) {
            return;
        }
        Cursor cursorQuery = sQLiteDatabase.query("name_lookup JOIN raw_contacts ON (raw_contact_id = raw_contacts._id)", AbstractContactAggregator.NameLookupMatchQueryWithParameter.COLUMNS, nameLookupSelectionBuilder.getSelection(), null, null, null, null, PRIMARY_HIT_LIMIT_STRING);
        while (cursorQuery.moveToNext()) {
            try {
                long j = cursorQuery.getLong(0);
                long j2 = cursorQuery.getLong(1);
                long j3 = cursorQuery.getLong(2);
                String string = cursorQuery.getString(3);
                int lookupType = nameLookupSelectionBuilder.getLookupType(string);
                int i = cursorQuery.getInt(4);
                rawContactMatcher.matchName(j, j2, j3, lookupType, string, i, string, 0);
                if (lookupType == 3 && i == 3) {
                    rawContactMatcher.updateScoreWithNicknameMatch(j, j2, j3);
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void updateMatchScoresBasedOnPhoneMatches(SQLiteDatabase sQLiteDatabase, long j, RawContactMatcher rawContactMatcher) {
        this.mSelectionArgs2[0] = String.valueOf(j);
        this.mSelectionArgs2[1] = this.mDbHelper.getUseStrictPhoneNumberComparisonParameter();
        Cursor cursorQuery = sQLiteDatabase.query("phone_lookup phoneA JOIN data dataA ON (dataA._id=phoneA.data_id) JOIN phone_lookup phoneB ON (phoneA.min_match=phoneB.min_match) JOIN data dataB ON (dataB._id=phoneB.data_id) JOIN raw_contacts ON (dataB.raw_contact_id = raw_contacts._id)", PhoneLookupQuery.COLUMNS, "dataA.raw_contact_id=? AND PHONE_NUMBERS_EQUAL(dataA.data1, dataB.data1,?) AND contact_id IN default_directory", this.mSelectionArgs2, null, null, null, SECONDARY_HIT_LIMIT_STRING);
        while (cursorQuery.moveToNext()) {
            try {
                long j2 = cursorQuery.getLong(0);
                if (j2 != j) {
                    rawContactMatcher.updateScoreWithPhoneNumberMatch(j2, cursorQuery.getLong(1), cursorQuery.getLong(2));
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void lookupApproximateNameMatches(SQLiteDatabase sQLiteDatabase, AbstractContactAggregator.MatchCandidateList matchCandidateList, RawContactMatcher rawContactMatcher) {
        ArraySet arraySet = new ArraySet();
        for (int i = 0; i < matchCandidateList.mCount; i++) {
            AbstractContactAggregator.NameMatchCandidate nameMatchCandidate = matchCandidateList.mList.get(i);
            if (nameMatchCandidate.mName.length() >= 2) {
                String strSubstring = nameMatchCandidate.mName.substring(0, 2);
                if (!arraySet.contains(strSubstring)) {
                    arraySet.add(strSubstring);
                    matchAllCandidates(sQLiteDatabase, "(normalized_name GLOB '" + strSubstring + "*') AND (name_type IN(2,4,3)) AND contact_id IN default_directory", matchCandidateList, rawContactMatcher, 2, String.valueOf(100));
                }
            }
        }
    }

    private void matchAllCandidates(SQLiteDatabase sQLiteDatabase, String str, AbstractContactAggregator.MatchCandidateList matchCandidateList, RawContactMatcher rawContactMatcher, int i, String str2) {
        Cursor cursorQuery = sQLiteDatabase.query("name_lookup INNER JOIN view_raw_contacts ON (name_lookup.raw_contact_id = view_raw_contacts._id)", ContactNameLookupQuery.COLUMNS, str, null, null, null, null, str2);
        while (cursorQuery.moveToNext()) {
            try {
                Long lValueOf = Long.valueOf(cursorQuery.getLong(0));
                Long lValueOf2 = Long.valueOf(cursorQuery.getLong(1));
                Long lValueOf3 = Long.valueOf(cursorQuery.getLong(2));
                String string = cursorQuery.getString(3);
                int i2 = cursorQuery.getInt(4);
                for (int i3 = 0; i3 < matchCandidateList.mCount; i3++) {
                    AbstractContactAggregator.NameMatchCandidate nameMatchCandidate = matchCandidateList.mList.get(i3);
                    rawContactMatcher.matchName(lValueOf.longValue(), lValueOf2.longValue(), lValueOf3.longValue(), nameMatchCandidate.mLookupType, nameMatchCandidate.mName, i2, string, i);
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    @Override
    protected List<MatchScore> findMatchingContacts(SQLiteDatabase sQLiteDatabase, long j, ArrayList<AbstractContactAggregator.AggregationSuggestionParameter> arrayList) throws Throwable {
        Cursor cursor;
        long j2;
        AbstractContactAggregator.MatchCandidateList matchCandidateList = new AbstractContactAggregator.MatchCandidateList();
        RawContactMatcher rawContactMatcher = new RawContactMatcher();
        if (arrayList != null && arrayList.size() != 0) {
            updateMatchScoresForSuggestionsBasedOnDataMatches(sQLiteDatabase, matchCandidateList, rawContactMatcher, arrayList);
        } else {
            SQLiteDatabase sQLiteDatabase2 = sQLiteDatabase;
            String[] strArr = AbstractContactAggregator.RawContactIdQuery.COLUMNS;
            StringBuilder sb = new StringBuilder();
            sb.append("contact_id=");
            long j3 = j;
            sb.append(j3);
            Cursor cursorQuery = sQLiteDatabase2.query("raw_contacts", strArr, sb.toString(), null, null, null, null);
            while (cursorQuery.moveToNext()) {
                try {
                    j2 = cursorQuery.getLong(0);
                    rawContactMatcher.keepOut(j2, j3, cursorQuery.getLong(1));
                    cursor = cursorQuery;
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                }
                try {
                    updateMatchScoresForSuggestionsBasedOnDataMatches(sQLiteDatabase2, j2, matchCandidateList, rawContactMatcher);
                    sQLiteDatabase2 = sQLiteDatabase;
                    j3 = j;
                    cursorQuery = cursor;
                } catch (Throwable th2) {
                    th = th2;
                    cursor.close();
                    throw th;
                }
            }
            cursorQuery.close();
        }
        return rawContactMatcher.pickBestMatches(50);
    }

    private void updateMatchScoresForSuggestionsBasedOnDataMatches(SQLiteDatabase sQLiteDatabase, long j, AbstractContactAggregator.MatchCandidateList matchCandidateList, RawContactMatcher rawContactMatcher) {
        updateMatchScoresBasedOnIdentityMatch(sQLiteDatabase, j, rawContactMatcher);
        updateMatchScoresBasedOnNameMatches(sQLiteDatabase, j, rawContactMatcher);
        updateMatchScoresBasedOnEmailMatches(sQLiteDatabase, j, rawContactMatcher);
        updateMatchScoresBasedOnPhoneMatches(sQLiteDatabase, j, rawContactMatcher);
        loadNameMatchCandidates(sQLiteDatabase, j, matchCandidateList, false);
        lookupApproximateNameMatches(sQLiteDatabase, matchCandidateList, rawContactMatcher);
    }

    private void updateMatchScores(SQLiteDatabase sQLiteDatabase, long j, AbstractContactAggregator.MatchCandidateList matchCandidateList, RawContactMatcher rawContactMatcher) {
        updateMatchScoresBasedOnExceptions(sQLiteDatabase, j, rawContactMatcher);
        updateMatchScoresBasedOnNameMatches(sQLiteDatabase, j, rawContactMatcher);
        if (rawContactWithoutName(sQLiteDatabase, j)) {
            updateMatchScoresBasedOnIdentityMatch(sQLiteDatabase, j, rawContactMatcher);
            updateMatchScoresBasedOnEmailMatches(sQLiteDatabase, j, rawContactMatcher);
            updateMatchScoresBasedOnPhoneMatches(sQLiteDatabase, j, rawContactMatcher);
            List<Long> listPrepareSecondaryMatchCandidates = rawContactMatcher.prepareSecondaryMatchCandidates();
            if (listPrepareSecondaryMatchCandidates != null && listPrepareSecondaryMatchCandidates.size() <= 20) {
                updateScoreForCandidatesWithoutName(sQLiteDatabase, listPrepareSecondaryMatchCandidates, rawContactMatcher);
            }
        }
    }

    private void updateMatchScoresForSuggestionsBasedOnDataMatches(SQLiteDatabase sQLiteDatabase, AbstractContactAggregator.MatchCandidateList matchCandidateList, RawContactMatcher rawContactMatcher, ArrayList<AbstractContactAggregator.AggregationSuggestionParameter> arrayList) {
        for (AbstractContactAggregator.AggregationSuggestionParameter aggregationSuggestionParameter : arrayList) {
            if ("name".equals(aggregationSuggestionParameter.kind)) {
                updateMatchScoresBasedOnNameMatches(sQLiteDatabase, aggregationSuggestionParameter.value, matchCandidateList, rawContactMatcher);
            }
        }
    }

    private boolean rawContactWithoutName(SQLiteDatabase sQLiteDatabase, long j) {
        Cursor cursorQuery = sQLiteDatabase.query("raw_contacts LEFT OUTER JOIN name_lookup ON _id = raw_contact_id AND name_type = 0", NullNameRawContactsIdsQuery.COLUMNS, "_id =" + j, null, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                return TextUtils.isEmpty(cursorQuery.getString(3));
            }
            cursorQuery.close();
            return false;
        } finally {
            cursorQuery.close();
        }
    }

    private void updateScoreForCandidatesWithoutName(SQLiteDatabase sQLiteDatabase, List<Long> list, RawContactMatcher rawContactMatcher) {
        this.mSb.setLength(0);
        StringBuilder sb = this.mSb;
        sb.append("_id");
        sb.append(" IN (");
        for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
                this.mSb.append(",");
            }
            this.mSb.append(list.get(i));
        }
        this.mSb.append(")");
        Cursor cursorQuery = sQLiteDatabase.query("raw_contacts LEFT OUTER JOIN name_lookup ON _id = raw_contact_id AND name_type = 0", NullNameRawContactsIdsQuery.COLUMNS, this.mSb.toString(), null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                Long lValueOf = Long.valueOf(cursorQuery.getLong(0));
                Long lValueOf2 = Long.valueOf(cursorQuery.getLong(1));
                Long lValueOf3 = Long.valueOf(cursorQuery.getLong(2));
                if (TextUtils.isEmpty(cursorQuery.getString(3))) {
                    rawContactMatcher.matchNoName(lValueOf, lValueOf2, lValueOf3);
                }
            } finally {
                cursorQuery.close();
            }
        }
    }
}
