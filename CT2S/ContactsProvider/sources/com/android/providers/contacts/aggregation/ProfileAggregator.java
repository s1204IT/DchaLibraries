package com.android.providers.contacts.aggregation;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.PhotoPriorityResolver;
import com.android.providers.contacts.TransactionContext;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;

public class ProfileAggregator extends ContactAggregator {
    private long mContactId;

    public ProfileAggregator(ContactsProvider2 contactsProvider, ContactsDatabaseHelper contactsDatabaseHelper, PhotoPriorityResolver photoPriorityResolver, NameSplitter nameSplitter, CommonNicknameCache commonNicknameCache) {
        super(contactsProvider, contactsDatabaseHelper, photoPriorityResolver, nameSplitter, commonNicknameCache);
    }

    @Override
    protected String computeLookupKeyForContact(SQLiteDatabase db, long contactId) {
        return "profile";
    }

    @Override
    protected void appendLookupKey(StringBuilder sb, String accountTypeWithDataSet, String accountName, long rawContactId, String sourceId, String displayName) {
        sb.setLength(0);
        sb.append("profile");
    }

    @Override
    public long onRawContactInsert(TransactionContext txContext, SQLiteDatabase db, long rawContactId) {
        aggregateContact(txContext, db, rawContactId);
        return this.mContactId;
    }

    @Override
    public void aggregateInTransaction(TransactionContext txContext, SQLiteDatabase db) {
    }

    @Override
    public void aggregateContact(TransactionContext txContext, SQLiteDatabase db, long rawContactId) {
        SQLiteStatement profileContactIdLookup = db.compileStatement("SELECT _id FROM contacts ORDER BY _id LIMIT 1");
        try {
            this.mContactId = profileContactIdLookup.simpleQueryForLong();
            updateAggregateData(txContext, this.mContactId);
        } catch (SQLiteDoneException e) {
            this.mContactId = insertContact(db, rawContactId);
        } finally {
            profileContactIdLookup.close();
        }
        setContactId(rawContactId, this.mContactId);
    }
}
