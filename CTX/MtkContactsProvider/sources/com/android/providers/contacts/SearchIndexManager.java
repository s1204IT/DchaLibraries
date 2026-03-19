package com.android.providers.contacts;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.providers.contacts.util.CappedStringBuilder;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.Iterator;
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
        private CappedStringBuilder mSbContent = new CappedStringBuilder(10240);
        private CappedStringBuilder mSbName = new CappedStringBuilder(10240);
        private CappedStringBuilder mSbTokens = new CappedStringBuilder(10240);
        private CappedStringBuilder mSbElementContent = new CappedStringBuilder(10240);
        private ArraySet<String> mUniqueElements = new ArraySet<>();

        void setCursor(Cursor cursor) {
            this.mCursor = cursor;
        }

        void reset() {
            this.mSbContent.clear();
            this.mSbTokens.clear();
            this.mSbName.clear();
            this.mSbElementContent.clear();
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

        public String getString(String str) {
            return this.mCursor.getString(this.mCursor.getColumnIndex(str));
        }

        public int getInt(String str) {
            return this.mCursor.getInt(this.mCursor.getColumnIndex(str));
        }

        public String toString() {
            return "Content: " + this.mSbContent + "\n Name: " + this.mSbName + "\n Tokens: " + this.mSbTokens;
        }

        public void commit() {
            if (this.mSbElementContent.length() != 0) {
                String strReplace = this.mSbElementContent.toString().replace('\n', ' ');
                if (!this.mUniqueElements.contains(strReplace)) {
                    if (this.mSbContent.length() != 0) {
                        this.mSbContent.append('\n');
                    }
                    this.mSbContent.append(strReplace);
                    this.mUniqueElements.add(strReplace);
                }
                this.mSbElementContent.clear();
            }
        }

        public void appendContentFromColumn(String str) {
            appendContentFromColumn(str, 0);
        }

        public void appendContentFromColumn(String str, int i) {
            appendContent(getString(str), i);
        }

        public void appendContent(String str) {
            appendContent(str, 0);
        }

        private void appendContent(String str, int i) {
            if (TextUtils.isEmpty(str)) {
            }
            switch (i) {
                case 0:
                    if (this.mSbElementContent.length() > 0) {
                        this.mSbElementContent.append(' ');
                    }
                    this.mSbElementContent.append(str);
                    break;
                case 1:
                    if (this.mSbElementContent.length() > 0) {
                        this.mSbElementContent.append(' ');
                    }
                    this.mSbElementContent.append('(').append(str).append(')');
                    break;
                case 2:
                    this.mSbElementContent.append('/').append(str);
                    break;
                case 3:
                    if (this.mSbElementContent.length() > 0) {
                        this.mSbElementContent.append(", ");
                    }
                    this.mSbElementContent.append(str);
                    break;
            }
        }

        public void appendToken(String str) {
            if (TextUtils.isEmpty(str)) {
                return;
            }
            if (this.mSbTokens.length() != 0) {
                this.mSbTokens.append(' ');
            }
            this.mSbTokens.append(str);
        }

        public void appendNameFromColumn(String str) {
            appendName(getString(str));
        }

        public void appendName(String str) {
            if (TextUtils.isEmpty(str)) {
                return;
            }
            appendNameInternal(str);
            List<String> listSplitIntoFtsTokens = SearchIndexManager.splitIntoFtsTokens(str);
            if (listSplitIntoFtsTokens.size() > 1) {
                for (String str2 : listSplitIntoFtsTokens) {
                    if (!TextUtils.isEmpty(str2)) {
                        appendNameInternal(str2);
                    }
                }
            }
        }

        private void appendNameInternal(String str) {
            if (this.mSbName.length() != 0) {
                this.mSbName.append(' ');
            }
            this.mSbName.append(NameNormalizer.normalize(str));
        }
    }

    public SearchIndexManager(ContactsProvider2 contactsProvider2) {
        this.mContactsProvider = contactsProvider2;
        this.mDbHelper = this.mContactsProvider.getDatabaseHelper();
    }

    public void updateIndex(boolean z) {
        if (!z) {
            if (getSearchIndexVersion() == 1) {
                return;
            }
        } else {
            setSearchIndexVersion(0);
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            if (getSearchIndexVersion() != 1) {
                rebuildIndex(writableDatabase);
                setSearchIndexVersion(1);
                writableDatabase.setTransactionSuccessful();
            }
        } finally {
            writableDatabase.endTransaction();
        }
    }

    private void rebuildIndex(SQLiteDatabase sQLiteDatabase) {
        this.mContactsProvider.setProviderStatus(1);
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        try {
            this.mDbHelper.createSearchIndexTable(sQLiteDatabase, true);
            int iBuildAndInsertIndex = buildAndInsertIndex(sQLiteDatabase, null);
            this.mContactsProvider.setProviderStatus(0);
            Log.i("ContactsFTS", "Rebuild contact search index in " + (SystemClock.elapsedRealtime() - jElapsedRealtime) + "ms, " + iBuildAndInsertIndex + " contacts");
        } catch (Throwable th) {
            this.mContactsProvider.setProviderStatus(0);
            Log.i("ContactsFTS", "Rebuild contact search index in " + (SystemClock.elapsedRealtime() - jElapsedRealtime) + "ms, 0 contacts");
            throw th;
        }
    }

    public void updateIndexForRawContacts(Set<Long> set, Set<Long> set2) {
        if (VERBOSE_LOGGING) {
            Log.v("ContactsFTS", "Updating search index for " + set.size() + " contacts / " + set2.size() + " raw contacts");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (!set.isEmpty()) {
            sb.append("contact_id IN (");
            sb.append(TextUtils.join(",", set));
            sb.append(')');
        }
        if (!set2.isEmpty()) {
            if (!set.isEmpty()) {
                sb.append(" OR ");
            }
            sb.append("contact_id IN (SELECT contact_id FROM raw_contacts WHERE raw_contacts._id IN (");
            sb.append(TextUtils.join(",", set2));
            sb.append("))");
        }
        sb.append(")");
        String string = sb.toString();
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        writableDatabase.delete("search_index", "contact_id IN (SELECT contact_id FROM raw_contacts WHERE " + string + ")", null);
        int iBuildAndInsertIndex = buildAndInsertIndex(writableDatabase, string);
        if (VERBOSE_LOGGING) {
            Log.v("ContactsFTS", "Updated search index for " + iBuildAndInsertIndex + " contacts");
        }
    }

    private int buildAndInsertIndex(SQLiteDatabase sQLiteDatabase, String str) {
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
        Cursor cursorQuery = sQLiteDatabase.query("data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) JOIN accounts ON (raw_contacts.account_id=accounts._id)", ContactIndexQuery.COLUMNS, str, null, null, null, this.mSb.toString());
        this.mIndexBuilder.setCursor(cursorQuery);
        this.mIndexBuilder.reset();
        int i = 0;
        long j = -1;
        while (cursorQuery.moveToNext()) {
            try {
                long j2 = cursorQuery.getLong(0);
                if (j2 != j) {
                    if (j != -1) {
                        insertIndexRow(sQLiteDatabase, j, this.mIndexBuilder);
                        i++;
                    }
                    this.mIndexBuilder.reset();
                    j = j2;
                }
                DataRowHandler dataRowHandler = this.mContactsProvider.getDataRowHandler(cursorQuery.getString(1));
                if (dataRowHandler.hasSearchableData()) {
                    dataRowHandler.appendSearchableData(this.mIndexBuilder);
                    this.mIndexBuilder.commit();
                }
            } finally {
                cursorQuery.close();
            }
        }
        if (j != -1) {
            insertIndexRow(sQLiteDatabase, j, this.mIndexBuilder);
            i++;
        }
        return i;
    }

    private void insertIndexRow(SQLiteDatabase sQLiteDatabase, long j, IndexBuilder indexBuilder) {
        this.mValues.clear();
        this.mValues.put("content", indexBuilder.getContent());
        this.mValues.put("name", indexBuilder.getName());
        this.mValues.put("tokens", indexBuilder.getTokens());
        this.mValues.put("contact_id", Long.valueOf(j));
        long jInsert = sQLiteDatabase.insert("search_index", null, this.mValues);
        if (jInsert <= 0) {
            jInsert = sQLiteDatabase.insert("search_index", null, this.mValues);
        }
        if (jInsert <= 0) {
            setSearchIndexVersion(0);
        }
    }

    private int getSearchIndexVersion() {
        return Integer.parseInt(this.mDbHelper.getProperty("search_index", "0"));
    }

    private void setSearchIndexVersion(int i) {
        this.mDbHelper.setProperty("search_index", String.valueOf(i));
    }

    static List<String> splitIntoFtsTokens(String str) {
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        for (String str2 : FTS_TOKEN_SEPARATOR_RE.split(str)) {
            if (!TextUtils.isEmpty(str2)) {
                arrayListNewArrayList.add(str2);
            }
        }
        return arrayListNewArrayList;
    }

    public static String getFtsMatchQuery(String str, FtsQueryBuilder ftsQueryBuilder) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = splitIntoFtsTokens(str).iterator();
        while (it.hasNext()) {
            ftsQueryBuilder.addToken(sb, it.next());
        }
        return sb.toString();
    }

    public static abstract class FtsQueryBuilder {
        public static final FtsQueryBuilder SCOPED_NAME_NORMALIZING;
        public static final FtsQueryBuilder UNSCOPED_NORMALIZING;

        public abstract void addToken(StringBuilder sb, String str);

        static {
            UNSCOPED_NORMALIZING = new UnscopedNormalizingBuilder();
            SCOPED_NAME_NORMALIZING = new ScopedNameNormalizingBuilder();
        }

        public static FtsQueryBuilder getDigitsQueryBuilder(final String str) {
            return new FtsQueryBuilder() {
                @Override
                public void addToken(StringBuilder sb, String str2) {
                    if (sb.length() != 0) {
                        sb.append(' ');
                    }
                    sb.append("content:");
                    sb.append(str2);
                    sb.append("* ");
                    String strNormalize = NameNormalizer.normalize(str2);
                    if (!TextUtils.isEmpty(strNormalize)) {
                        sb.append(" OR name:");
                        sb.append(strNormalize);
                        sb.append('*');
                    }
                    sb.append(str);
                }
            };
        }
    }

    private static class UnscopedNormalizingBuilder extends FtsQueryBuilder {
        private UnscopedNormalizingBuilder() {
        }

        @Override
        public void addToken(StringBuilder sb, String str) {
            if (sb.length() != 0) {
                sb.append(' ');
            }
            sb.append(NameNormalizer.normalize(str));
            sb.append('*');
        }
    }

    private static class ScopedNameNormalizingBuilder extends FtsQueryBuilder {
        private ScopedNameNormalizingBuilder() {
        }

        @Override
        public void addToken(StringBuilder sb, String str) {
            if (sb.length() != 0) {
                sb.append(' ');
            }
            sb.append("content:");
            sb.append(str);
            sb.append('*');
            String strNormalize = NameNormalizer.normalize(str);
            if (!TextUtils.isEmpty(strNormalize)) {
                sb.append(" OR name:");
                sb.append(strNormalize);
                sb.append('*');
            }
            sb.append(" OR tokens:");
            sb.append(str);
            sb.append("*");
        }
    }
}
