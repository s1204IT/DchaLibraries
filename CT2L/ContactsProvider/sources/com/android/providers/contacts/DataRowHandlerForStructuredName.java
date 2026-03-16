package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.ContactAggregator;

public class DataRowHandlerForStructuredName extends DataRowHandler {
    private final String[] STRUCTURED_FIELDS;
    private final NameLookupBuilder mNameLookupBuilder;
    private final StringBuilder mSb;
    private final NameSplitter mSplitter;

    public DataRowHandlerForStructuredName(Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator, NameSplitter splitter, NameLookupBuilder nameLookupBuilder) {
        super(context, dbHelper, aggregator, "vnd.android.cursor.item/name");
        this.mSb = new StringBuilder();
        this.STRUCTURED_FIELDS = new String[]{"data4", "data2", "data5", "data3", "data6"};
        this.mSplitter = splitter;
        this.mNameLookupBuilder = nameLookupBuilder;
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId, ContentValues values) {
        fixStructuredNameComponents(values, values);
        long dataId = super.insert(db, txContext, rawContactId, values);
        String name = values.getAsString("data1");
        Integer fullNameStyle = values.getAsInteger("data10");
        this.mNameLookupBuilder.insertNameLookup(rawContactId, dataId, name, fullNameStyle != null ? this.mSplitter.getAdjustedFullNameStyle(fullNameStyle.intValue()) : 0);
        fixRawContactDisplayName(db, txContext, rawContactId);
        triggerAggregation(txContext, rawContactId);
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        long dataId = c.getLong(0);
        long rawContactId = c.getLong(1);
        ContentValues augmented = getAugmentedValues(db, dataId, values);
        if (augmented == null) {
            return false;
        }
        fixStructuredNameComponents(augmented, values);
        super.update(db, txContext, values, c, callerIsSyncAdapter);
        if (values.containsKey("data1")) {
            augmented.putAll(values);
            String name = augmented.getAsString("data1");
            this.mDbHelper.deleteNameLookup(dataId);
            Integer fullNameStyle = augmented.getAsInteger("data10");
            this.mNameLookupBuilder.insertNameLookup(rawContactId, dataId, name, fullNameStyle != null ? this.mSplitter.getAdjustedFullNameStyle(fullNameStyle.intValue()) : 0);
        }
        fixRawContactDisplayName(db, txContext, rawContactId);
        triggerAggregation(txContext, rawContactId);
        return true;
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long dataId = c.getLong(0);
        long rawContactId = c.getLong(2);
        int count = super.delete(db, txContext, c);
        this.mDbHelper.deleteNameLookup(dataId);
        fixRawContactDisplayName(db, txContext, rawContactId);
        triggerAggregation(txContext, rawContactId);
        return count;
    }

    public void fixStructuredNameComponents(ContentValues augmented, ContentValues update) {
        String unstruct = update.getAsString("data1");
        boolean touchedUnstruct = !TextUtils.isEmpty(unstruct);
        boolean touchedStruct = !areAllEmpty(update, this.STRUCTURED_FIELDS);
        if (touchedUnstruct && !touchedStruct) {
            NameSplitter.Name name = new NameSplitter.Name();
            this.mSplitter.split(name, unstruct);
            name.toValues(update);
            return;
        }
        if (!touchedUnstruct && (touchedStruct || areAnySpecified(update, this.STRUCTURED_FIELDS))) {
            NameSplitter.Name name2 = new NameSplitter.Name();
            name2.fromValues(augmented);
            name2.fullNameStyle = 0;
            name2.phoneticNameStyle = 0;
            this.mSplitter.guessNameStyle(name2);
            int unadjustedFullNameStyle = name2.fullNameStyle;
            name2.fullNameStyle = this.mSplitter.getAdjustedFullNameStyle(name2.fullNameStyle);
            String joined = this.mSplitter.join(name2, true, true);
            update.put("data1", joined);
            update.put("data10", Integer.valueOf(unadjustedFullNameStyle));
            update.put("data11", Integer.valueOf(name2.phoneticNameStyle));
            return;
        }
        if (touchedUnstruct && touchedStruct) {
            if (!update.containsKey("data10")) {
                update.put("data10", Integer.valueOf(this.mSplitter.guessFullNameStyle(unstruct)));
            }
            if (!update.containsKey("data11")) {
                NameSplitter.Name name3 = new NameSplitter.Name();
                name3.fromValues(update);
                name3.phoneticNameStyle = 0;
                this.mSplitter.guessNameStyle(name3);
                update.put("data11", Integer.valueOf(name3.phoneticNameStyle));
            }
        }
    }

    @Override
    public boolean hasSearchableData() {
        return true;
    }

    @Override
    public boolean containsSearchableColumns(ContentValues values) {
        return values.containsKey("data3") || values.containsKey("data2") || values.containsKey("data5") || values.containsKey("data9") || values.containsKey("data7") || values.containsKey("data8") || values.containsKey("data4") || values.containsKey("data6");
    }

    @Override
    public void appendSearchableData(SearchIndexManager.IndexBuilder builder) {
        String name = builder.getString("data1");
        Integer fullNameStyle = Integer.valueOf(builder.getInt("data10"));
        this.mNameLookupBuilder.appendToSearchIndex(builder, name, fullNameStyle != null ? this.mSplitter.getAdjustedFullNameStyle(fullNameStyle.intValue()) : 0);
        String phoneticFamily = builder.getString("data9");
        String phoneticMiddle = builder.getString("data8");
        String phoneticGiven = builder.getString("data7");
        if (!TextUtils.isEmpty(phoneticFamily) || !TextUtils.isEmpty(phoneticMiddle) || !TextUtils.isEmpty(phoneticGiven)) {
            this.mSb.setLength(0);
            if (!TextUtils.isEmpty(phoneticFamily)) {
                builder.appendName(phoneticFamily);
                this.mSb.append(phoneticFamily);
            }
            if (!TextUtils.isEmpty(phoneticMiddle)) {
                builder.appendName(phoneticMiddle);
                this.mSb.append(phoneticMiddle);
            }
            if (!TextUtils.isEmpty(phoneticGiven)) {
                builder.appendName(phoneticGiven);
                this.mSb.append(phoneticGiven);
            }
            String phoneticName = this.mSb.toString().trim();
            int phoneticNameStyle = builder.getInt("data11");
            if (phoneticNameStyle == 0) {
                phoneticNameStyle = this.mSplitter.guessPhoneticNameStyle(phoneticName);
            }
            builder.appendName(phoneticName);
            this.mNameLookupBuilder.appendNameShorthandLookup(builder, phoneticName, phoneticNameStyle);
        }
    }
}
