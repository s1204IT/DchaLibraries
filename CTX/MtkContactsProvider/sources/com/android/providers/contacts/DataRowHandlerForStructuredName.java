package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

public class DataRowHandlerForStructuredName extends DataRowHandler {
    private final String[] STRUCTURED_FIELDS;
    private final NameLookupBuilder mNameLookupBuilder;
    private final StringBuilder mSb;
    private final NameSplitter mSplitter;

    public DataRowHandlerForStructuredName(Context context, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator, NameSplitter nameSplitter, NameLookupBuilder nameLookupBuilder) {
        super(context, contactsDatabaseHelper, abstractContactAggregator, "vnd.android.cursor.item/name");
        this.mSb = new StringBuilder();
        this.STRUCTURED_FIELDS = new String[]{"data4", "data2", "data5", "data3", "data6"};
        this.mSplitter = nameSplitter;
        this.mNameLookupBuilder = nameLookupBuilder;
    }

    @Override
    public long insert(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, long j, ContentValues contentValues) {
        int adjustedFullNameStyle;
        fixStructuredNameComponents(contentValues, contentValues);
        long jInsert = super.insert(sQLiteDatabase, transactionContext, j, contentValues);
        String asString = contentValues.getAsString("data1");
        Integer asInteger = contentValues.getAsInteger("data10");
        NameLookupBuilder nameLookupBuilder = this.mNameLookupBuilder;
        if (asInteger != null) {
            adjustedFullNameStyle = this.mSplitter.getAdjustedFullNameStyle(asInteger.intValue());
        } else {
            adjustedFullNameStyle = 0;
        }
        nameLookupBuilder.insertNameLookup(j, jInsert, asString, adjustedFullNameStyle);
        fixRawContactDisplayName(sQLiteDatabase, transactionContext, j);
        triggerAggregation(transactionContext, j);
        return jInsert;
    }

    @Override
    public boolean update(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, ContentValues contentValues, Cursor cursor, boolean z, boolean z2) {
        int adjustedFullNameStyle = 0;
        long j = cursor.getLong(0);
        long j2 = cursor.getLong(1);
        ContentValues augmentedValues = getAugmentedValues(sQLiteDatabase, j, contentValues);
        if (augmentedValues == null) {
            return false;
        }
        fixStructuredNameComponents(augmentedValues, contentValues);
        super.update(sQLiteDatabase, transactionContext, contentValues, cursor, z, z2);
        if (contentValues.containsKey("data1")) {
            augmentedValues.putAll(contentValues);
            String asString = augmentedValues.getAsString("data1");
            this.mDbHelper.deleteNameLookup(j);
            Integer asInteger = augmentedValues.getAsInteger("data10");
            NameLookupBuilder nameLookupBuilder = this.mNameLookupBuilder;
            if (asInteger != null) {
                adjustedFullNameStyle = this.mSplitter.getAdjustedFullNameStyle(asInteger.intValue());
            }
            nameLookupBuilder.insertNameLookup(j2, j, asString, adjustedFullNameStyle);
        }
        fixRawContactDisplayName(sQLiteDatabase, transactionContext, j2);
        triggerAggregation(transactionContext, j2);
        return true;
    }

    @Override
    public int delete(SQLiteDatabase sQLiteDatabase, TransactionContext transactionContext, Cursor cursor) {
        long j = cursor.getLong(0);
        long j2 = cursor.getLong(2);
        int iDelete = super.delete(sQLiteDatabase, transactionContext, cursor);
        this.mDbHelper.deleteNameLookup(j);
        fixRawContactDisplayName(sQLiteDatabase, transactionContext, j2);
        triggerAggregation(transactionContext, j2);
        return iDelete;
    }

    public void fixStructuredNameComponents(ContentValues contentValues, ContentValues contentValues2) {
        String asString = contentValues2.getAsString("data1");
        boolean z = !TextUtils.isEmpty(asString);
        boolean z2 = !areAllEmpty(contentValues2, this.STRUCTURED_FIELDS);
        if (z && !z2) {
            NameSplitter.Name name = new NameSplitter.Name();
            this.mSplitter.split(name, asString);
            name.toValues(contentValues2);
            return;
        }
        if (!z && (z2 || areAnySpecified(contentValues2, this.STRUCTURED_FIELDS))) {
            NameSplitter.Name name2 = new NameSplitter.Name();
            name2.fromValues(contentValues);
            name2.fullNameStyle = 0;
            name2.phoneticNameStyle = 0;
            this.mSplitter.guessNameStyle(name2);
            int i = name2.fullNameStyle;
            name2.fullNameStyle = this.mSplitter.getAdjustedFullNameStyle(name2.fullNameStyle);
            contentValues2.put("data1", this.mSplitter.join(name2, true, true));
            contentValues2.put("data10", Integer.valueOf(i));
            contentValues2.put("data11", Integer.valueOf(name2.phoneticNameStyle));
            return;
        }
        if (z && z2) {
            if (!contentValues2.containsKey("data10")) {
                contentValues2.put("data10", Integer.valueOf(this.mSplitter.guessFullNameStyle(asString)));
            }
            if (!contentValues2.containsKey("data11")) {
                NameSplitter.Name name3 = new NameSplitter.Name();
                name3.fromValues(contentValues2);
                name3.phoneticNameStyle = 0;
                this.mSplitter.guessNameStyle(name3);
                contentValues2.put("data11", Integer.valueOf(name3.phoneticNameStyle));
            }
        }
    }

    @Override
    public boolean hasSearchableData() {
        return true;
    }

    @Override
    public boolean containsSearchableColumns(ContentValues contentValues) {
        return contentValues.containsKey("data3") || contentValues.containsKey("data2") || contentValues.containsKey("data5") || contentValues.containsKey("data9") || contentValues.containsKey("data7") || contentValues.containsKey("data8") || contentValues.containsKey("data4") || contentValues.containsKey("data6");
    }

    @Override
    public void appendSearchableData(SearchIndexManager.IndexBuilder indexBuilder) {
        int adjustedFullNameStyle;
        String string = indexBuilder.getString("data1");
        Integer numValueOf = Integer.valueOf(indexBuilder.getInt("data10"));
        NameLookupBuilder nameLookupBuilder = this.mNameLookupBuilder;
        if (numValueOf == null) {
            adjustedFullNameStyle = 0;
        } else {
            adjustedFullNameStyle = this.mSplitter.getAdjustedFullNameStyle(numValueOf.intValue());
        }
        nameLookupBuilder.appendToSearchIndex(indexBuilder, string, adjustedFullNameStyle);
        String string2 = indexBuilder.getString("data9");
        String string3 = indexBuilder.getString("data8");
        String string4 = indexBuilder.getString("data7");
        if (!TextUtils.isEmpty(string2) || !TextUtils.isEmpty(string3) || !TextUtils.isEmpty(string4)) {
            this.mSb.setLength(0);
            if (!TextUtils.isEmpty(string2)) {
                indexBuilder.appendName(string2);
                this.mSb.append(string2);
            }
            if (!TextUtils.isEmpty(string3)) {
                indexBuilder.appendName(string3);
                this.mSb.append(string3);
            }
            if (!TextUtils.isEmpty(string4)) {
                indexBuilder.appendName(string4);
                this.mSb.append(string4);
            }
            String strTrim = this.mSb.toString().trim();
            int iGuessPhoneticNameStyle = indexBuilder.getInt("data11");
            if (iGuessPhoneticNameStyle == 0) {
                iGuessPhoneticNameStyle = this.mSplitter.guessPhoneticNameStyle(strTrim);
            }
            indexBuilder.appendName(strTrim);
            this.mNameLookupBuilder.appendNameShorthandLookup(indexBuilder, strTrim, iGuessPhoneticNameStyle);
        }
    }
}
