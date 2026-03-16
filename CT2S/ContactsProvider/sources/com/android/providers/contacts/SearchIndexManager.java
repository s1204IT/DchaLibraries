package com.android.providers.contacts;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SearchIndexManager {
    private final ContactsProvider2 mContactsProvider;
    private final ContactsDatabaseHelper mDbHelper;
    private static final boolean VERBOSE_LOGGING = Log.isLoggable("ContactsFTS", 2);
    private static final Pattern FTS_TOKEN_SEPARATOR_RE = Pattern.compile("[^\u0080-\uffff\\p{Alnum}_]");
    private StringBuilder mSb = new StringBuilder();
    private IndexBuilder mIndexBuilder = new IndexBuilder();
    private ContentValues mValues = new ContentValues();
    private String[] mSelectionArgs1 = new String[1];

    private static final class ContactIndexQuery {
        public static final String[] COLUMNS = {"contact_id", "mimetype", "data1", "data2", "data3", "data4", "data5", "data6", "data7", "data8", "data9", "data10", "data11", "data12", "data13", "data14"};
    }

    public static class IndexBuilder {
        private Cursor mCursor;
        private StringBuilder mSbContent = new StringBuilder();
        private StringBuilder mSbName = new StringBuilder();
        private StringBuilder mSbTokens = new StringBuilder();
        private StringBuilder mSbElementContent = new StringBuilder();
        private HashSet<String> mUniqueElements = new HashSet<>();

        void setCursor(Cursor cursor) {
            this.mCursor = cursor;
        }

        void reset() {
            this.mSbContent.setLength(0);
            this.mSbTokens.setLength(0);
            this.mSbName.setLength(0);
            this.mSbElementContent.setLength(0);
            this.mUniqueElements.clear();
        }

        public String getContent() {
            if (this.mSbContent.length() == 0) {
                return null;
            }
            return this.mSbContent.toString();
        }

        public String getName() {
            if (this.mSbName.length() == 0) {
                return null;
            }
            return this.mSbName.toString();
        }

        public String getTokens() {
            if (this.mSbTokens.length() == 0) {
                return null;
            }
            return this.mSbTokens.toString();
        }

        public String getString(String columnName) {
            return this.mCursor.getString(this.mCursor.getColumnIndex(columnName));
        }

        public int getInt(String columnName) {
            return this.mCursor.getInt(this.mCursor.getColumnIndex(columnName));
        }

        public String toString() {
            return "Content: " + ((Object) this.mSbContent) + "\n Name: " + ((Object) this.mSbTokens) + "\n Tokens: " + ((Object) this.mSbTokens);
        }

        public void commit() {
            if (this.mSbElementContent.length() != 0) {
                String content = this.mSbElementContent.toString().replace('\n', ' ');
                if (!this.mUniqueElements.contains(content)) {
                    if (this.mSbContent.length() != 0) {
                        this.mSbContent.append('\n');
                    }
                    this.mSbContent.append(content);
                    this.mUniqueElements.add(content);
                }
                this.mSbElementContent.setLength(0);
            }
        }

        public void appendContentFromColumn(String columnName) {
            appendContentFromColumn(columnName, 0);
        }

        public void appendContentFromColumn(String columnName, int format) {
            appendContent(getString(columnName), format);
        }

        public void appendContent(String value) {
            appendContent(value, 0);
        }

        private void appendContent(String value, int format) {
            if (!TextUtils.isEmpty(value)) {
                switch (format) {
                    case 0:
                        if (this.mSbElementContent.length() > 0) {
                            this.mSbElementContent.append(' ');
                        }
                        this.mSbElementContent.append(value);
                        break;
                    case 1:
                        if (this.mSbElementContent.length() > 0) {
                            this.mSbElementContent.append(' ');
                        }
                        this.mSbElementContent.append('(').append(value).append(')');
                        break;
                    case 2:
                        this.mSbElementContent.append('/').append(value);
                        break;
                    case 3:
                        if (this.mSbElementContent.length() > 0) {
                            this.mSbElementContent.append(", ");
                        }
                        this.mSbElementContent.append(value);
                        break;
                }
            }
        }

        public void appendToken(String token) {
            if (!TextUtils.isEmpty(token)) {
                if (this.mSbTokens.length() != 0) {
                    this.mSbTokens.append(' ');
                }
                this.mSbTokens.append(token);
            }
        }

        public void appendNameFromColumn(String columnName) {
            appendName(getString(columnName));
        }

        public void appendName(String name) {
            if (!TextUtils.isEmpty(name)) {
                appendNameInternal(name);
                List<String> nameParts = SearchIndexManager.splitIntoFtsTokens(name);
                if (nameParts.size() > 1) {
                    for (String namePart : nameParts) {
                        if (!TextUtils.isEmpty(namePart)) {
                            appendNameInternal(namePart);
                        }
                    }
                }
            }
        }

        private void appendNameInternal(String name) {
            if (this.mSbName.length() != 0) {
                this.mSbName.append(' ');
            }
            this.mSbName.append(NameNormalizer.normalize(name));
        }
    }

    public SearchIndexManager(ContactsProvider2 contactsProvider) {
        this.mContactsProvider = contactsProvider;
        this.mDbHelper = (ContactsDatabaseHelper) this.mContactsProvider.getDatabaseHelper();
    }

    public void updateIndex(boolean force) {
        if (force) {
            setSearchIndexVersion(0);
        } else if (getSearchIndexVersion() == 1) {
            return;
        }
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (getSearchIndexVersion() != 1) {
                rebuildIndex(db);
                setSearchIndexVersion(1);
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }
    }

    private void rebuildIndex(SQLiteDatabase db) {
        this.mContactsProvider.setProviderStatus(1);
        long start = SystemClock.elapsedRealtime();
        int count = 0;
        try {
            this.mDbHelper.createSearchIndexTable(db, true);
            count = buildAndInsertIndex(db, null);
        } finally {
            this.mContactsProvider.setProviderStatus(0);
            long end = SystemClock.elapsedRealtime();
            Log.i("ContactsFTS", "Rebuild contact search index in " + (end - start) + "ms, " + count + " contacts");
        }
    }

    public void updateIndexForRawContacts(Set<Long> contactIds, Set<Long> rawContactIds) {
        if (VERBOSE_LOGGING) {
            Log.v("ContactsFTS", "Updating search index for " + contactIds.size() + " contacts / " + rawContactIds.size() + " raw contacts");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (!contactIds.isEmpty()) {
            sb.append("contact_id IN (");
            sb.append(TextUtils.join(",", contactIds));
            sb.append(')');
        }
        if (!rawContactIds.isEmpty()) {
            if (!contactIds.isEmpty()) {
                sb.append(" OR ");
            }
            sb.append("contact_id IN (SELECT contact_id FROM raw_contacts WHERE raw_contacts._id IN (");
            sb.append(TextUtils.join(",", rawContactIds));
            sb.append("))");
        }
        sb.append(")");
        String rawContactsSelection = sb.toString();
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        db.delete("search_index", "contact_id IN (SELECT contact_id FROM raw_contacts WHERE " + rawContactsSelection + ")", null);
        int count = buildAndInsertIndex(db, rawContactsSelection);
        if (VERBOSE_LOGGING) {
            Log.v("ContactsFTS", "Updated search index for " + count + " contacts");
        }
    }

    private int buildAndInsertIndex(SQLiteDatabase db, String selection) {
        this.mSb.setLength(0);
        this.mSb.append("contact_id, ");
        this.mSb.append("(CASE WHEN mimetype_id=");
        this.mSb.append(this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/nickname"));
        this.mSb.append(" THEN -4 ");
        this.mSb.append(" WHEN mimetype_id=");
        this.mSb.append(this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/organization"));
        this.mSb.append(" THEN -3 ");
        this.mSb.append(" WHEN mimetype_id=");
        this.mSb.append(this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/postal-address_v2"));
        this.mSb.append(" THEN -2");
        this.mSb.append(" WHEN mimetype_id=");
        this.mSb.append(this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/email_v2"));
        this.mSb.append(" THEN -1");
        this.mSb.append(" ELSE mimetype_id");
        this.mSb.append(" END), is_super_primary, data._id");
        int count = 0;
        Cursor cursor = db.query("data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) JOIN accounts ON (raw_contacts.account_id=accounts._id)", ContactIndexQuery.COLUMNS, selection, null, null, null, this.mSb.toString());
        this.mIndexBuilder.setCursor(cursor);
        this.mIndexBuilder.reset();
        long currentContactId = -1;
        while (cursor.moveToNext()) {
            try {
                long contactId = cursor.getLong(0);
                if (contactId != currentContactId) {
                    if (currentContactId != -1) {
                        insertIndexRow(db, currentContactId, this.mIndexBuilder);
                        count++;
                    }
                    currentContactId = contactId;
                    this.mIndexBuilder.reset();
                }
                String mimetype = cursor.getString(1);
                DataRowHandler dataRowHandler = this.mContactsProvider.getDataRowHandler(mimetype);
                if (dataRowHandler.hasSearchableData()) {
                    dataRowHandler.appendSearchableData(this.mIndexBuilder);
                    this.mIndexBuilder.commit();
                }
            } finally {
                cursor.close();
            }
        }
        if (currentContactId != -1) {
            insertIndexRow(db, currentContactId, this.mIndexBuilder);
            count++;
        }
        return count;
    }

    private void insertIndexRow(SQLiteDatabase db, long contactId, IndexBuilder builder) {
        this.mValues.clear();
        this.mValues.put("content", builder.getContent());
        this.mValues.put("name", builder.getName());
        this.mValues.put("tokens", builder.getTokens());
        this.mValues.put("contact_id", Long.valueOf(contactId));
        db.insert("search_index", null, this.mValues);
    }

    private int getSearchIndexVersion() {
        return Integer.parseInt(this.mDbHelper.getProperty("search_index", "0"));
    }

    private void setSearchIndexVersion(int version) {
        this.mDbHelper.setProperty("search_index", String.valueOf(version));
    }

    static List<String> splitIntoFtsTokens(String s) {
        ArrayList<String> ret = Lists.newArrayList();
        String[] arr$ = FTS_TOKEN_SEPARATOR_RE.split(s);
        for (String token : arr$) {
            if (!TextUtils.isEmpty(token)) {
                ret.add(token);
            }
        }
        return ret;
    }

    public static String getFtsMatchQuery(String query, FtsQueryBuilder ftsQueryBuilder) {
        StringBuilder result = new StringBuilder();
        for (String token : splitIntoFtsTokens(query)) {
            ftsQueryBuilder.addToken(result, token);
        }
        return result.toString();
    }

    public static abstract class FtsQueryBuilder {
        public static final FtsQueryBuilder SCOPED_NAME_NORMALIZING;
        public static final FtsQueryBuilder UNSCOPED_NORMALIZING;

        public abstract void addToken(StringBuilder sb, String str);

        static {
            UNSCOPED_NORMALIZING = new UnscopedNormalizingBuilder();
            SCOPED_NAME_NORMALIZING = new ScopedNameNormalizingBuilder();
        }

        public static FtsQueryBuilder getDigitsQueryBuilder(final String commonCriteria) {
            return new FtsQueryBuilder() {
                @Override
                public void addToken(StringBuilder builder, String token) {
                    if (builder.length() != 0) {
                        builder.append(' ');
                    }
                    builder.append("content:");
                    builder.append(token);
                    builder.append("* ");
                    String normalizedToken = NameNormalizer.normalize(token);
                    if (!TextUtils.isEmpty(normalizedToken)) {
                        builder.append(" OR name:");
                        builder.append(normalizedToken);
                        builder.append('*');
                    }
                    builder.append(commonCriteria);
                }
            };
        }
    }

    private static class UnscopedNormalizingBuilder extends FtsQueryBuilder {
        private UnscopedNormalizingBuilder() {
        }

        @Override
        public void addToken(StringBuilder builder, String token) {
            if (builder.length() != 0) {
                builder.append(' ');
            }
            builder.append(NameNormalizer.normalize(token));
            builder.append('*');
        }
    }

    private static class ScopedNameNormalizingBuilder extends FtsQueryBuilder {
        private ScopedNameNormalizingBuilder() {
        }

        @Override
        public void addToken(StringBuilder builder, String token) {
            if (builder.length() != 0) {
                builder.append(' ');
            }
            builder.append("content:");
            builder.append(token);
            builder.append('*');
            String normalizedToken = NameNormalizer.normalize(token);
            if (!TextUtils.isEmpty(normalizedToken)) {
                builder.append(" OR name:");
                builder.append(normalizedToken);
                builder.append('*');
            }
            builder.append(" OR tokens:");
            builder.append(token);
            builder.append("*");
        }
    }
}
