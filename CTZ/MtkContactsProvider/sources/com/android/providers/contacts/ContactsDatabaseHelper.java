package com.android.providers.contacts;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Base64;
import android.util.Log;
import com.android.common.content.SyncStateContentProviderHelper;
import com.android.internal.annotations.VisibleForTesting;
import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;
import com.android.providers.contacts.database.ContactsTableUtil;
import com.android.providers.contacts.database.DeletedContactsTableUtil;
import com.android.providers.contacts.database.MoreDatabaseUtils;
import com.android.providers.contacts.sqlite.SqlChecker;
import com.android.providers.contacts.util.PropertyUtils;
import com.mediatek.providers.contacts.AccountUtils;
import com.mediatek.providers.contacts.SimCardUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import libcore.icu.ICU;

public class ContactsDatabaseHelper extends SQLiteOpenHelper {

    @VisibleForTesting
    static final boolean DISALLOW_SUB_QUERIES = false;
    private SqlChecker mCachedSqlChecker;
    private CharArrayBuffer mCharArrayBuffer;

    @VisibleForTesting
    final ArrayMap<String, Long> mCommonMimeTypeIdsCache;
    private final Context mContext;
    private final CountryMonitor mCountryMonitor;
    private long mDatabaseCreationTime;
    private final boolean mDatabaseOptimizationEnabled;
    private final boolean mIsTestInstance;
    private MessageDigest mMessageDigest;
    private NameSplitter.Name mName;
    private NameSplitter mNameSplitter;
    private String[] mSelectionArgs1;
    private final SyncStateContentProviderHelper mSyncState;
    private boolean mUseStrictPhoneNumberComparison;
    private static ContactsDatabaseHelper sSingleton = null;

    @VisibleForTesting
    static final String[] COMMON_MIME_TYPES = {"vnd.android.cursor.item/email_v2", "vnd.android.cursor.item/im", "vnd.android.cursor.item/nickname", "vnd.android.cursor.item/organization", "vnd.android.cursor.item/phone_v2", "vnd.android.cursor.item/sip_address", "vnd.android.cursor.item/name", "vnd.android.cursor.item/postal-address_v2", "vnd.android.cursor.item/identity", "vnd.android.cursor.item/photo", "vnd.android.cursor.item/group_membership", "vnd.android.cursor.item/note", "vnd.android.cursor.item/contact_event", "vnd.android.cursor.item/website", "vnd.android.cursor.item/relation", "vnd.com.google.cursor.item/contact_misc", "vnd.android.cursor.item/ims"};

    private interface EmailQuery {
        public static final String[] COLUMNS = {"_id", "raw_contact_id", "data1"};
    }

    private interface NicknameQuery {
        public static final String[] COLUMNS = {"_id", "raw_contact_id", "data1"};
    }

    public interface Projections {
        public static final String[] ID = {"_id"};
        public static final String[] LITERAL_ONE = {"1"};
    }

    private interface StructuredNameQuery {
        public static final String[] COLUMNS = {"_id", "raw_contact_id", "data1"};
    }

    public interface Tables {
        public static final String[] SEQUENCE_TABLES = {"contacts", "raw_contacts", "stream_items", "stream_item_photos", "photo_files", "data", "groups", "directories"};
    }

    public interface Clauses {
        public static final String LOCAL_ACCOUNT_ID = "(SELECT _id FROM accounts WHERE " + AccountUtils.getLocalAccountSelection() + " )";
        public static final String RAW_CONTACT_IS_LOCAL;

        static {
            StringBuilder sb = new StringBuilder();
            sb.append("raw_contacts.account_id=");
            sb.append(LOCAL_ACCOUNT_ID);
            RAW_CONTACT_IS_LOCAL = sb.toString();
        }
    }

    private class StructuredNameLookupBuilder extends NameLookupBuilder {
        private final CommonNicknameCache mCommonNicknameCache;
        private final SQLiteStatement mNameLookupInsert;

        public StructuredNameLookupBuilder(NameSplitter nameSplitter, CommonNicknameCache commonNicknameCache, SQLiteStatement sQLiteStatement) {
            super(nameSplitter);
            this.mCommonNicknameCache = commonNicknameCache;
            this.mNameLookupInsert = sQLiteStatement;
        }

        @Override
        protected void insertNameLookup(long j, long j2, int i, String str) {
            if (!TextUtils.isEmpty(str)) {
                ContactsDatabaseHelper.this.insertNormalizedNameLookup(this.mNameLookupInsert, j, j2, i, str);
            }
        }

        @Override
        protected String[] getCommonNicknameClusters(String str) {
            return this.mCommonNicknameCache.getCommonNicknameClusters(str);
        }
    }

    @VisibleForTesting
    interface LowRes {
        public static final String TEMPLATE_TIMES_USED = "cast(ifnull((case when (XX) <= 0 then 0 when (XX) < (YY) then (XX) else (cast((XX) as int) / (YY)) * (YY) end), 0) as int)".replaceAll("YY", String.valueOf(10));
        public static final String TEMPLATE_LAST_TIME_USED = "cast((cast((XX) as int) / (YY)) * (YY) as int)".replaceAll("YY", String.valueOf(86400));

        static String getTimesUsedExpression(String str) {
            return TEMPLATE_TIMES_USED.replaceAll("XX", str);
        }

        static String getLastTimeUsedExpression(String str) {
            return TEMPLATE_LAST_TIME_USED.replaceAll("XX", str);
        }
    }

    public static synchronized ContactsDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new ContactsDatabaseHelper(context, "contacts2.db", true, DISALLOW_SUB_QUERIES);
        }
        return sSingleton;
    }

    public static ContactsDatabaseHelper getNewInstanceForTest(Context context, String str) {
        return new ContactsDatabaseHelper(context, str, DISALLOW_SUB_QUERIES, true);
    }

    protected ContactsDatabaseHelper(Context context, String str, boolean z, boolean z2) {
        super(context, str, null, 1300, 700, null);
        this.mCommonMimeTypeIdsCache = new ArrayMap<>();
        try {
            this.mMessageDigest = MessageDigest.getInstance("SHA-1");
            this.mSelectionArgs1 = new String[1];
            this.mName = new NameSplitter.Name();
            this.mCharArrayBuffer = new CharArrayBuffer(128);
            setWriteAheadLoggingEnabled((dbForProfile() != 0 || ActivityManager.isLowRamDeviceStatic()) ? false : Settings.Global.getInt(context.getContentResolver(), "contacts_database_wal_enabled", 1) == 1);
            setIdleConnectionTimeout(30000L);
            this.mDatabaseOptimizationEnabled = z;
            this.mIsTestInstance = z2;
            Resources resources = context.getResources();
            this.mContext = context;
            this.mSyncState = new SyncStateContentProviderHelper();
            this.mCountryMonitor = new CountryMonitor(context);
            this.mUseStrictPhoneNumberComparison = resources.getBoolean(android.R.^attr-private.pointerIconWait);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm.", e);
        }
    }

    public SQLiteDatabase getDatabase(boolean z) {
        return z ? getWritableDatabase() : getReadableDatabase();
    }

    private void prepopulateCommonMimeTypes(SQLiteDatabase sQLiteDatabase) {
        this.mCommonMimeTypeIdsCache.clear();
        for (String str : COMMON_MIME_TYPES) {
            this.mCommonMimeTypeIdsCache.put(str, Long.valueOf(insertMimeType(sQLiteDatabase, str)));
        }
    }

    public void onBeforeDelete(SQLiteDatabase sQLiteDatabase) {
        Log.w("ContactsDatabaseHelper", "Database version " + sQLiteDatabase.getVersion() + " for contacts2.db is no longer supported. Data will be lost on upgrading to 1300");
    }

    @Override
    public void onOpen(SQLiteDatabase sQLiteDatabase) {
        Log.d("ContactsDatabaseHelper", "WAL enabled for " + getDatabaseName() + ": " + sQLiteDatabase.isWriteAheadLoggingEnabled());
        preprocessToAddWhenOpen(sQLiteDatabase);
        prepopulateCommonMimeTypes(sQLiteDatabase);
        this.mSyncState.onDatabaseOpened(sQLiteDatabase);
        sQLiteDatabase.execSQL("DELETE FROM presence;");
        sQLiteDatabase.execSQL("DELETE FROM agg_presence;");
        loadDatabaseCreationTime(sQLiteDatabase);
    }

    protected void setDatabaseCreationTime(SQLiteDatabase sQLiteDatabase) {
        this.mDatabaseCreationTime = System.currentTimeMillis();
        PropertyUtils.setProperty(sQLiteDatabase, "database_time_created", String.valueOf(this.mDatabaseCreationTime));
    }

    protected void loadDatabaseCreationTime(SQLiteDatabase sQLiteDatabase) {
        this.mDatabaseCreationTime = 0L;
        String property = PropertyUtils.getProperty(sQLiteDatabase, "database_time_created", "");
        if (!TextUtils.isEmpty(property)) {
            try {
                this.mDatabaseCreationTime = Long.parseLong(property);
            } catch (NumberFormatException e) {
                Log.w("ContactsDatabaseHelper", "Failed to parse timestamp: " + property);
            }
        }
        if (AbstractContactsProvider.VERBOSE_LOGGING) {
            Log.v("ContactsDatabaseHelper", "Open: creation time=" + this.mDatabaseCreationTime);
        }
        if (this.mDatabaseCreationTime == 0) {
            Log.w("ContactsDatabaseHelper", "Unable to load creating time; resetting.");
            this.mDatabaseCreationTime = System.currentTimeMillis();
            PropertyUtils.setProperty(sQLiteDatabase, "database_time_created", Long.toString(this.mDatabaseCreationTime));
        }
    }

    private void createPresenceTables(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS presence (presence_data_id INTEGER PRIMARY KEY REFERENCES data(_id),protocol INTEGER NOT NULL,custom_protocol TEXT,im_handle TEXT,im_account TEXT,presence_contact_id INTEGER REFERENCES contacts(_id),presence_raw_contact_id INTEGER REFERENCES raw_contacts(_id),mode INTEGER,chat_capability INTEGER NOT NULL DEFAULT 0,UNIQUE(protocol, custom_protocol, im_handle, im_account));");
        sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS presenceIndex ON presence (presence_raw_contact_id);");
        sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS presenceIndex2 ON presence (presence_contact_id);");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS agg_presence (presence_contact_id INTEGER PRIMARY KEY REFERENCES contacts(_id),mode INTEGER,chat_capability INTEGER NOT NULL DEFAULT 0);");
        sQLiteDatabase.execSQL("CREATE TRIGGER IF NOT EXISTS presence_deleted BEFORE DELETE ON presence BEGIN    DELETE FROM agg_presence     WHERE presence_contact_id = (SELECT presence_contact_id FROM presence WHERE presence_raw_contact_id=OLD.presence_raw_contact_id AND NOT EXISTS(SELECT presence_raw_contact_id FROM presence WHERE presence_contact_id=OLD.presence_contact_id AND presence_raw_contact_id!=OLD.presence_raw_contact_id)); END");
        sQLiteDatabase.execSQL("CREATE TRIGGER IF NOT EXISTS presence_inserted AFTER INSERT ON presence BEGIN INSERT OR REPLACE INTO agg_presence(presence_contact_id, mode, chat_capability) SELECT presence_contact_id,mode,chat_capability FROM presence WHERE  (ifnull(mode,0)  * 10 + ifnull(chat_capability, 0)) = (SELECT MAX (ifnull(mode,0)  * 10 + ifnull(chat_capability, 0)) FROM presence WHERE presence_contact_id=NEW.presence_contact_id) AND presence_contact_id=NEW.presence_contact_id; END");
        sQLiteDatabase.execSQL("CREATE TRIGGER IF NOT EXISTS presence_updated AFTER UPDATE ON presence BEGIN INSERT OR REPLACE INTO agg_presence(presence_contact_id, mode, chat_capability) SELECT presence_contact_id,mode,chat_capability FROM presence WHERE  (ifnull(mode,0)  * 10 + ifnull(chat_capability, 0)) = (SELECT MAX (ifnull(mode,0)  * 10 + ifnull(chat_capability, 0)) FROM presence WHERE presence_contact_id=NEW.presence_contact_id) AND presence_contact_id=NEW.presence_contact_id; END");
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        Log.i("ContactsDatabaseHelper", "Bootstrapping database contacts2.db version: 1300");
        this.mSyncState.createDatabase(sQLiteDatabase);
        PropertyUtils.createPropertiesTable(sQLiteDatabase);
        setDatabaseCreationTime(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TABLE accounts (_id INTEGER PRIMARY KEY AUTOINCREMENT,account_name TEXT, account_type TEXT, data_set TEXT);");
        sQLiteDatabase.execSQL("CREATE TABLE contacts (_id INTEGER PRIMARY KEY AUTOINCREMENT,name_raw_contact_id INTEGER REFERENCES raw_contacts(_id),photo_id INTEGER REFERENCES data(_id),photo_file_id INTEGER REFERENCES photo_files(_id),custom_ringtone TEXT,send_to_voicemail INTEGER NOT NULL DEFAULT 0,x_times_contacted INTEGER NOT NULL DEFAULT 0,x_last_time_contacted INTEGER,times_contacted INTEGER NOT NULL DEFAULT 0,last_time_contacted INTEGER,starred INTEGER NOT NULL DEFAULT 0,pinned INTEGER NOT NULL DEFAULT 0,has_phone_number INTEGER NOT NULL DEFAULT 0,lookup TEXT,status_update_id INTEGER REFERENCES data(_id),contact_last_updated_timestamp INTEGER,send_to_voicemail_vt INTEGER NOT NULL DEFAULT 0,send_to_voicemail_sip INTEGER NOT NULL DEFAULT 0,indicate_phone_or_sim_contact INTEGER NOT NULL DEFAULT -1,index_in_sim INTEGER NOT NULL DEFAULT -1,filter INTEGER NOT NULL DEFAULT 0,is_sdn_contact INTEGER NOT NULL DEFAULT 0);");
        ContactsTableUtil.createIndexes(sQLiteDatabase);
        DeletedContactsTableUtil.create(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TABLE raw_contacts (_id INTEGER PRIMARY KEY AUTOINCREMENT,account_id INTEGER REFERENCES accounts(_id),sourceid TEXT,backup_id TEXT,raw_contact_is_read_only INTEGER NOT NULL DEFAULT 0,version INTEGER NOT NULL DEFAULT 1,dirty INTEGER NOT NULL DEFAULT 0,deleted INTEGER NOT NULL DEFAULT 0,metadata_dirty INTEGER NOT NULL DEFAULT 0,contact_id INTEGER REFERENCES contacts(_id),aggregation_mode INTEGER NOT NULL DEFAULT 0,aggregation_needed INTEGER NOT NULL DEFAULT 1,custom_ringtone TEXT,send_to_voicemail INTEGER NOT NULL DEFAULT 0,x_times_contacted INTEGER NOT NULL DEFAULT 0,x_last_time_contacted INTEGER,times_contacted INTEGER NOT NULL DEFAULT 0,last_time_contacted INTEGER,starred INTEGER NOT NULL DEFAULT 0,pinned INTEGER NOT NULL DEFAULT 0,display_name TEXT,display_name_alt TEXT,display_name_source INTEGER NOT NULL DEFAULT 0,phonetic_name TEXT,phonetic_name_style TEXT,sort_key TEXT COLLATE PHONEBOOK,phonebook_label TEXT,phonebook_bucket INTEGER,sort_key_alt TEXT COLLATE PHONEBOOK,phonebook_label_alt TEXT,phonebook_bucket_alt INTEGER,name_verified INTEGER NOT NULL DEFAULT 0,sync1 TEXT, sync2 TEXT, sync3 TEXT, sync4 TEXT, timestamp INTEGER,send_to_voicemail_vt INTEGER NOT NULL DEFAULT 0,send_to_voicemail_sip INTEGER NOT NULL DEFAULT 0,indicate_phone_or_sim_contact INTEGER NOT NULL DEFAULT -1,index_in_sim INTEGER NOT NULL DEFAULT -1,is_sdn_contact INTEGER NOT NULL DEFAULT 0);");
        sQLiteDatabase.execSQL("CREATE INDEX raw_contacts_contact_id_index ON raw_contacts (contact_id);");
        sQLiteDatabase.execSQL("CREATE INDEX raw_contacts_source_id_account_id_index ON raw_contacts (sourceid, account_id);");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS raw_contacts_backup_id_account_id_index ON raw_contacts (backup_id, account_id);");
        sQLiteDatabase.execSQL("CREATE TABLE stream_items (_id INTEGER PRIMARY KEY AUTOINCREMENT, raw_contact_id INTEGER NOT NULL, res_package TEXT, icon TEXT, label TEXT, text TEXT, timestamp INTEGER NOT NULL, comments TEXT, stream_item_sync1 TEXT, stream_item_sync2 TEXT, stream_item_sync3 TEXT, stream_item_sync4 TEXT, FOREIGN KEY(raw_contact_id) REFERENCES raw_contacts(_id));");
        sQLiteDatabase.execSQL("CREATE TABLE stream_item_photos (_id INTEGER PRIMARY KEY AUTOINCREMENT, stream_item_id INTEGER NOT NULL, sort_index INTEGER, photo_file_id INTEGER NOT NULL, stream_item_photo_sync1 TEXT, stream_item_photo_sync2 TEXT, stream_item_photo_sync3 TEXT, stream_item_photo_sync4 TEXT, FOREIGN KEY(stream_item_id) REFERENCES stream_items(_id));");
        sQLiteDatabase.execSQL("CREATE TABLE photo_files (_id INTEGER PRIMARY KEY AUTOINCREMENT, height INTEGER NOT NULL, width INTEGER NOT NULL, filesize INTEGER NOT NULL);");
        sQLiteDatabase.execSQL("CREATE TABLE packages (_id INTEGER PRIMARY KEY AUTOINCREMENT,package TEXT NOT NULL);");
        sQLiteDatabase.execSQL("CREATE TABLE mimetypes (_id INTEGER PRIMARY KEY AUTOINCREMENT,mimetype TEXT NOT NULL);");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX mime_type ON mimetypes (mimetype);");
        sQLiteDatabase.execSQL("CREATE TABLE data (_id INTEGER PRIMARY KEY AUTOINCREMENT,package_id INTEGER REFERENCES package(_id),mimetype_id INTEGER REFERENCES mimetype(_id) NOT NULL,raw_contact_id INTEGER REFERENCES raw_contacts(_id) NOT NULL,hash_id TEXT,is_read_only INTEGER NOT NULL DEFAULT 0,is_primary INTEGER NOT NULL DEFAULT 0,is_super_primary INTEGER NOT NULL DEFAULT 0,data_version INTEGER NOT NULL DEFAULT 0,data1 TEXT,data2 TEXT,data3 TEXT,data4 TEXT,data5 TEXT,data6 TEXT,data7 TEXT,data8 TEXT,data9 TEXT,data10 TEXT,data11 TEXT,data12 TEXT,data13 TEXT,data14 TEXT,data15 TEXT,data_sync1 TEXT, data_sync2 TEXT, data_sync3 TEXT, data_sync4 TEXT, carrier_presence INTEGER NOT NULL DEFAULT 0, preferred_phone_account_component_name TEXT, preferred_phone_account_id TEXT, is_additional_number INTEGER NOT NULL DEFAULT 0 );");
        sQLiteDatabase.execSQL("CREATE INDEX data_raw_contact_id ON data (raw_contact_id);");
        sQLiteDatabase.execSQL("CREATE INDEX data_mimetype_data1_index ON data (mimetype_id,data1);");
        sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS data_hash_id_index ON data (hash_id);");
        sQLiteDatabase.execSQL("CREATE TABLE phone_lookup (data_id INTEGER REFERENCES data(_id) NOT NULL,raw_contact_id INTEGER REFERENCES raw_contacts(_id) NOT NULL,normalized_number TEXT NOT NULL,min_match TEXT NOT NULL);");
        sQLiteDatabase.execSQL("CREATE INDEX phone_lookup_index ON phone_lookup (normalized_number,raw_contact_id,data_id);");
        sQLiteDatabase.execSQL("CREATE INDEX phone_lookup_min_match_index ON phone_lookup (min_match,raw_contact_id,data_id);");
        sQLiteDatabase.execSQL("CREATE INDEX phone_lookup_data_id_min_match_index ON phone_lookup (data_id, min_match);");
        sQLiteDatabase.execSQL("CREATE TABLE name_lookup (data_id INTEGER REFERENCES data(_id) NOT NULL,raw_contact_id INTEGER REFERENCES raw_contacts(_id) NOT NULL,normalized_name TEXT NOT NULL,name_type INTEGER NOT NULL,PRIMARY KEY (data_id, normalized_name, name_type));");
        sQLiteDatabase.execSQL("CREATE INDEX name_lookup_raw_contact_id_index ON name_lookup (raw_contact_id);");
        sQLiteDatabase.execSQL("CREATE TABLE nickname_lookup (name TEXT,cluster TEXT);");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX nickname_lookup_index ON nickname_lookup (name, cluster);");
        sQLiteDatabase.execSQL("CREATE TABLE groups (_id INTEGER PRIMARY KEY AUTOINCREMENT,package_id INTEGER REFERENCES package(_id),account_id INTEGER REFERENCES accounts(_id),sourceid TEXT,version INTEGER NOT NULL DEFAULT 1,dirty INTEGER NOT NULL DEFAULT 0,title TEXT,title_res INTEGER,notes TEXT,system_id TEXT,deleted INTEGER NOT NULL DEFAULT 0,group_visible INTEGER NOT NULL DEFAULT 0,should_sync INTEGER NOT NULL DEFAULT 1,auto_add INTEGER NOT NULL DEFAULT 0,favorites INTEGER NOT NULL DEFAULT 0,group_is_read_only INTEGER NOT NULL DEFAULT 0,sync1 TEXT, sync2 TEXT, sync3 TEXT, sync4 TEXT );");
        sQLiteDatabase.execSQL("CREATE INDEX groups_source_id_account_id_index ON groups (sourceid, account_id);");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS agg_exceptions (_id INTEGER PRIMARY KEY AUTOINCREMENT,type INTEGER NOT NULL, raw_contact_id1 INTEGER REFERENCES raw_contacts(_id), raw_contact_id2 INTEGER REFERENCES raw_contacts(_id));");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS aggregation_exception_index1 ON agg_exceptions (raw_contact_id1, raw_contact_id2);");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS aggregation_exception_index2 ON agg_exceptions (raw_contact_id2, raw_contact_id1);");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS settings (account_name STRING NOT NULL,account_type STRING NOT NULL,data_set STRING,ungrouped_visible INTEGER NOT NULL DEFAULT 0,should_sync INTEGER NOT NULL DEFAULT 1);");
        sQLiteDatabase.execSQL("CREATE TABLE visible_contacts (_id INTEGER PRIMARY KEY);");
        sQLiteDatabase.execSQL("CREATE TABLE default_directory (_id INTEGER PRIMARY KEY);");
        sQLiteDatabase.execSQL("CREATE TABLE status_updates (status_update_data_id INTEGER PRIMARY KEY REFERENCES data(_id),status TEXT,status_ts INTEGER,status_res_package TEXT, status_label INTEGER, status_icon INTEGER);");
        createDirectoriesTable(sQLiteDatabase);
        createSearchIndexTable(sQLiteDatabase, DISALLOW_SUB_QUERIES);
        sQLiteDatabase.execSQL("CREATE TABLE data_usage_stat(stat_id INTEGER PRIMARY KEY AUTOINCREMENT, data_id INTEGER NOT NULL, usage_type INTEGER NOT NULL DEFAULT 0, x_times_used INTEGER NOT NULL DEFAULT 0, x_last_time_used INTEGER NOT NULL DEFAULT 0, times_used INTEGER NOT NULL DEFAULT 0, last_time_used INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(data_id) REFERENCES data(_id));");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX data_usage_stat_index ON data_usage_stat (data_id, usage_type);");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS metadata_sync (_id INTEGER PRIMARY KEY AUTOINCREMENT,raw_contact_backup_id TEXT NOT NULL,account_id INTEGER NOT NULL,data TEXT,deleted INTEGER NOT NULL DEFAULT 0);");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS metadata_sync_index ON metadata_sync (raw_contact_backup_id, account_id);");
        sQLiteDatabase.execSQL("CREATE TABLE pre_authorized_uris (_id INTEGER PRIMARY KEY AUTOINCREMENT, uri STRING NOT NULL, expiration INTEGER NOT NULL DEFAULT 0);");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS metadata_sync_state (_id INTEGER PRIMARY KEY AUTOINCREMENT,account_id INTEGER NOT NULL,state BLOB);");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS metadata_sync_state_index ON metadata_sync_state (account_id);");
        createContactsViews(sQLiteDatabase);
        createGroupsView(sQLiteDatabase);
        createContactsTriggers(sQLiteDatabase);
        createContactsIndexes(sQLiteDatabase, DISALLOW_SUB_QUERIES);
        createPresenceTables(sQLiteDatabase);
        loadNicknameLookupTable(sQLiteDatabase);
        initializeAutoIncrementSequences(sQLiteDatabase);
        LegacyApiSupport.createDatabase(sQLiteDatabase);
        if (this.mDatabaseOptimizationEnabled) {
            sQLiteDatabase.execSQL("ANALYZE;");
            updateSqliteStats(sQLiteDatabase);
        }
        postOnCreate();
    }

    protected void postOnCreate() {
        notifyProviderStatusChange(this.mContext);
        ContentResolver.requestSync(null, "com.android.contacts", new Bundle());
        Intent intent = new Intent("android.provider.Contacts.DATABASE_CREATED");
        intent.addFlags(67108864);
        this.mContext.sendBroadcast(intent, "android.permission.READ_CONTACTS");
    }

    protected void initializeAutoIncrementSequences(SQLiteDatabase sQLiteDatabase) {
    }

    private void createDirectoriesTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE directories(_id INTEGER PRIMARY KEY AUTOINCREMENT,packageName TEXT NOT NULL,authority TEXT NOT NULL,typeResourceId INTEGER,typeResourceName TEXT,accountType TEXT,accountName TEXT,displayName TEXT, exportSupport INTEGER NOT NULL DEFAULT 0,shortcutSupport INTEGER NOT NULL DEFAULT 0,photoSupport INTEGER NOT NULL DEFAULT 0);");
        PropertyUtils.setProperty(sQLiteDatabase, "directoryScanComplete", "0");
    }

    public void createSearchIndexTable(SQLiteDatabase sQLiteDatabase, boolean z) {
        sQLiteDatabase.beginTransactionNonExclusive();
        try {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS search_index");
            sQLiteDatabase.execSQL("CREATE VIRTUAL TABLE search_index USING FTS4 (contact_id INTEGER REFERENCES contacts(_id) NOT NULL,content TEXT, name TEXT, tokens TEXT)");
            if (z) {
                updateSqliteStats(sQLiteDatabase);
            }
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    private void createContactsTriggers(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS raw_contacts_deleted;");
        sQLiteDatabase.execSQL("CREATE TRIGGER raw_contacts_deleted    BEFORE DELETE ON raw_contacts BEGIN    DELETE FROM data     WHERE raw_contact_id=OLD._id;   DELETE FROM agg_exceptions     WHERE raw_contact_id1=OLD._id        OR raw_contact_id2=OLD._id;   DELETE FROM visible_contacts     WHERE _id=OLD.contact_id       AND (SELECT COUNT(*) FROM raw_contacts            WHERE contact_id=OLD.contact_id           )=1;   DELETE FROM default_directory     WHERE _id=OLD.contact_id       AND (SELECT COUNT(*) FROM raw_contacts            WHERE contact_id=OLD.contact_id           )=1;   DELETE FROM contacts     WHERE _id=OLD.contact_id       AND (SELECT COUNT(*) FROM raw_contacts            WHERE contact_id=OLD.contact_id           )=1;   DELETE FROM search_index     WHERE contact_id=OLD.contact_id       AND (SELECT COUNT(*) FROM raw_contacts            WHERE contact_id=OLD.contact_id           )=1; END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS contacts_times_contacted;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS raw_contacts_times_contacted;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS raw_contacts_marked_deleted;");
        sQLiteDatabase.execSQL("CREATE TRIGGER raw_contacts_marked_deleted    AFTER UPDATE ON raw_contacts BEGIN    UPDATE raw_contacts     SET version=OLD.version+1 , timestamp=strftime(\"%s\", 'now') * 1000     WHERE _id=OLD._id       AND NEW.deleted!= OLD.deleted; END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS data_updated;");
        sQLiteDatabase.execSQL("CREATE TRIGGER data_updated AFTER UPDATE ON data BEGIN    UPDATE data     SET data_version=OLD.data_version+1      WHERE _id=OLD._id;   UPDATE raw_contacts     SET version=version+1      WHERE _id=OLD.raw_contact_id; END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS data_deleted;");
        sQLiteDatabase.execSQL("CREATE TRIGGER data_deleted BEFORE DELETE ON data BEGIN    UPDATE raw_contacts     SET version=version+1      WHERE _id=OLD.raw_contact_id;   DELETE FROM phone_lookup     WHERE data_id=OLD._id;   DELETE FROM status_updates     WHERE status_update_data_id=OLD._id;   DELETE FROM name_lookup     WHERE data_id=OLD._id; END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS groups_updated1;");
        sQLiteDatabase.execSQL("CREATE TRIGGER groups_updated1    AFTER UPDATE ON groups BEGIN    UPDATE groups     SET version=OLD.version+1     WHERE _id=OLD._id; END");
        String str = " INSERT OR IGNORE INTO default_directory     SELECT contact_id     FROM raw_contacts     WHERE raw_contacts.account_id=" + Clauses.LOCAL_ACCOUNT_ID + ";";
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS groups_auto_add_updated1;");
        sQLiteDatabase.execSQL("CREATE TRIGGER groups_auto_add_updated1    AFTER UPDATE OF auto_add ON groups BEGIN    DELETE FROM default_directory;" + str + " INSERT OR IGNORE INTO default_directory     SELECT contact_id         FROM raw_contacts     WHERE NOT EXISTS         (SELECT _id             FROM groups             WHERE raw_contacts.account_id = groups.account_id             AND auto_add != 0); INSERT OR IGNORE INTO default_directory     SELECT contact_id         FROM raw_contacts     JOIN data           ON (raw_contacts._id=raw_contact_id)     WHERE mimetype_id=(SELECT _id FROM mimetypes WHERE mimetype='vnd.android.cursor.item/group_membership')     AND EXISTS         (SELECT _id             FROM groups                 WHERE raw_contacts.account_id = groups.account_id                 AND auto_add != 0); END");
    }

    private void createContactsIndexes(SQLiteDatabase sQLiteDatabase, boolean z) {
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS name_lookup_index");
        sQLiteDatabase.execSQL("CREATE INDEX name_lookup_index ON name_lookup (normalized_name,name_type, raw_contact_id, data_id);");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS raw_contact_sort_key1_index");
        sQLiteDatabase.execSQL("CREATE INDEX raw_contact_sort_key1_index ON raw_contacts (sort_key);");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS raw_contact_sort_key2_index");
        sQLiteDatabase.execSQL("CREATE INDEX raw_contact_sort_key2_index ON raw_contacts (sort_key_alt);");
        if (z) {
            updateSqliteStats(sQLiteDatabase);
        }
    }

    private void createContactsViews(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_contacts;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_data;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_raw_contacts;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_raw_entities;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_entities;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_data_usage_stat;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_data_usage;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_stream_items;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_metadata_sync_state;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_metadata_sync;");
        sQLiteDatabase.execSQL("CREATE VIEW view_data AS " + ("SELECT data._id AS _id,hash_id, raw_contact_id, raw_contacts.contact_id AS contact_id, raw_contacts.account_id,accounts.account_name AS account_name,accounts.account_type AS account_type,accounts.data_set AS data_set,(CASE WHEN accounts.data_set IS NULL THEN accounts.account_type ELSE accounts.account_type||'/'||accounts.data_set END) AS account_type_and_data_set,raw_contacts.sourceid AS sourceid,raw_contacts.backup_id AS backup_id,raw_contacts.version AS version,raw_contacts.dirty AS dirty,raw_contacts.sync1 AS sync1,raw_contacts.sync2 AS sync2,raw_contacts.sync3 AS sync3,raw_contacts.sync4 AS sync4,raw_contacts.timestamp AS timestamp, is_primary, is_super_primary, data_version, data.package_id,package AS res_package,data.mimetype_id,mimetype AS mimetype, is_read_only, data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, data12, data13, data14, data15, carrier_presence, preferred_phone_account_component_name, preferred_phone_account_id, data_sync1, data_sync2, data_sync3, data_sync4, is_additional_number, " + ("contacts.custom_ringtone AS custom_ringtone,contacts.send_to_voicemail AS send_to_voicemail,contacts.x_last_time_contacted AS x_last_time_contacted,contacts.x_times_contacted AS x_times_contacted," + LowRes.getLastTimeUsedExpression("contacts.x_last_time_contacted") + " AS last_time_contacted," + LowRes.getTimesUsedExpression("contacts.x_times_contacted") + " AS times_contacted,contacts.starred AS starred,contacts.pinned AS pinned,contacts.send_to_voicemail_vt AS send_to_voicemail_vt,contacts.send_to_voicemail_sip AS send_to_voicemail_sip") + ", name_raw_contact.display_name_source AS display_name_source, name_raw_contact.display_name AS display_name, name_raw_contact.display_name_alt AS display_name_alt, name_raw_contact.phonetic_name AS phonetic_name, name_raw_contact.phonetic_name_style AS phonetic_name_style, name_raw_contact.sort_key AS sort_key, name_raw_contact.phonebook_label AS phonebook_label, name_raw_contact.phonebook_bucket AS phonebook_bucket, name_raw_contact.sort_key_alt AS sort_key_alt, name_raw_contact.phonebook_label_alt AS phonebook_label_alt, name_raw_contact.phonebook_bucket_alt AS phonebook_bucket_alt, name_raw_contact.indicate_phone_or_sim_contact AS indicate_phone_or_sim_contact, name_raw_contact.index_in_sim AS index_in_sim, name_raw_contact.is_sdn_contact AS is_sdn_contact, has_phone_number, name_raw_contact_id, lookup, photo_id, photo_file_id, CAST(EXISTS (SELECT _id FROM visible_contacts WHERE contacts._id=visible_contacts._id) AS INTEGER) AS in_visible_group, CAST(EXISTS (SELECT _id FROM default_directory WHERE contacts._id=default_directory._id) AS INTEGER) AS in_default_directory, status_update_id, contacts.contact_last_updated_timestamp, " + buildDisplayPhotoUriAlias("raw_contacts.contact_id", "photo_uri") + ", " + buildThumbnailPhotoUriAlias("raw_contacts.contact_id", "photo_thumb_uri") + ", " + dbForProfile() + " AS raw_contact_is_user_profile, groups.sourceid AS group_sourceid FROM data JOIN mimetypes ON (data.mimetype_id=mimetypes._id) JOIN raw_contacts ON (data.raw_contact_id=raw_contacts._id) JOIN accounts ON (raw_contacts.account_id=accounts._id) JOIN contacts ON (raw_contacts.contact_id=contacts._id) JOIN raw_contacts AS name_raw_contact ON(name_raw_contact_id=name_raw_contact._id) LEFT OUTER JOIN packages ON (data.package_id=packages._id) LEFT OUTER JOIN groups ON (mimetypes.mimetype='vnd.android.cursor.item/group_membership' AND groups._id=data.data1)"));
        sQLiteDatabase.execSQL("CREATE VIEW view_raw_contacts AS " + ("SELECT raw_contacts._id AS _id,contact_id, aggregation_mode, raw_contact_is_read_only, deleted, raw_contacts.metadata_dirty, display_name_source, display_name, display_name_alt, phonetic_name, phonetic_name_style, sort_key, phonebook_label, phonebook_bucket, sort_key_alt, phonebook_label_alt, phonebook_bucket_alt, " + dbForProfile() + " AS raw_contact_is_user_profile, " + ("custom_ringtone,send_to_voicemail,x_last_time_contacted," + LowRes.getLastTimeUsedExpression("x_last_time_contacted") + " AS last_time_contacted,x_times_contacted," + LowRes.getTimesUsedExpression("x_times_contacted") + " AS times_contacted,starred,pinned,send_to_voicemail_vt, send_to_voicemail_sip") + ", raw_contacts.account_id,accounts.account_name AS account_name,accounts.account_type AS account_type,accounts.data_set AS data_set,(CASE WHEN accounts.data_set IS NULL THEN accounts.account_type ELSE accounts.account_type||'/'||accounts.data_set END) AS account_type_and_data_set,raw_contacts.sourceid AS sourceid,raw_contacts.backup_id AS backup_id,raw_contacts.version AS version,raw_contacts.dirty AS dirty,raw_contacts.sync1 AS sync1,raw_contacts.sync2 AS sync2,raw_contacts.sync3 AS sync3,raw_contacts.sync4 AS sync4,raw_contacts.timestamp AS timestamp, indicate_phone_or_sim_contact, index_in_sim, is_sdn_contact FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id)"));
        String str = "contacts.custom_ringtone AS custom_ringtone, name_raw_contact.display_name_source AS display_name_source, name_raw_contact.display_name AS display_name, name_raw_contact.display_name_alt AS display_name_alt, name_raw_contact.phonetic_name AS phonetic_name, name_raw_contact.phonetic_name_style AS phonetic_name_style, name_raw_contact.sort_key AS sort_key, name_raw_contact.phonebook_label AS phonebook_label, name_raw_contact.phonebook_bucket AS phonebook_bucket, name_raw_contact.sort_key_alt AS sort_key_alt, name_raw_contact.phonebook_label_alt AS phonebook_label_alt, name_raw_contact.phonebook_bucket_alt AS phonebook_bucket_alt, name_raw_contact.indicate_phone_or_sim_contact AS indicate_phone_or_sim_contact, name_raw_contact.index_in_sim AS index_in_sim, name_raw_contact.is_sdn_contact AS is_sdn_contact, has_phone_number, name_raw_contact_id, lookup, photo_id, photo_file_id, CAST(EXISTS (SELECT _id FROM visible_contacts WHERE contacts._id=visible_contacts._id) AS INTEGER) AS in_visible_group, CAST(EXISTS (SELECT _id FROM default_directory WHERE contacts._id=default_directory._id) AS INTEGER) AS in_default_directory, status_update_id, contacts.contact_last_updated_timestamp, contacts.x_last_time_contacted AS x_last_time_contacted, " + LowRes.getLastTimeUsedExpression("contacts.x_last_time_contacted") + " AS last_time_contacted, contacts.send_to_voicemail AS send_to_voicemail, contacts.starred AS starred, contacts.pinned AS pinned, contacts.x_times_contacted AS x_times_contacted, " + LowRes.getTimesUsedExpression("contacts.x_times_contacted") + " AS times_contacted, contacts.filter AS filter, contacts.send_to_voicemail_vt AS send_to_voicemail_vt, contacts.send_to_voicemail_sip AS send_to_voicemail_sip";
        sQLiteDatabase.execSQL("CREATE VIEW view_contacts AS " + ("SELECT contacts._id AS _id," + str + ", " + buildDisplayPhotoUriAlias("contacts._id", "photo_uri") + ", " + buildThumbnailPhotoUriAlias("contacts._id", "photo_thumb_uri") + ", " + dbForProfile() + " AS is_user_profile FROM contacts JOIN raw_contacts AS name_raw_contact ON(name_raw_contact_id=name_raw_contact._id)"));
        sQLiteDatabase.execSQL("CREATE VIEW view_raw_entities AS " + ("SELECT contact_id, raw_contacts.deleted AS deleted,raw_contacts.metadata_dirty, is_primary, is_super_primary, data_version, data.package_id,package AS res_package,data.mimetype_id,mimetype AS mimetype, is_read_only, data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, data12, data13, data14, data15, carrier_presence, preferred_phone_account_component_name, preferred_phone_account_id, data_sync1, data_sync2, data_sync3, data_sync4, is_additional_number, raw_contacts.account_id,accounts.account_name AS account_name,accounts.account_type AS account_type,accounts.data_set AS data_set,(CASE WHEN accounts.data_set IS NULL THEN accounts.account_type ELSE accounts.account_type||'/'||accounts.data_set END) AS account_type_and_data_set,raw_contacts.sourceid AS sourceid,raw_contacts.backup_id AS backup_id,raw_contacts.version AS version,raw_contacts.dirty AS dirty,raw_contacts.sync1 AS sync1,raw_contacts.sync2 AS sync2,raw_contacts.sync3 AS sync3,raw_contacts.sync4 AS sync4,raw_contacts.timestamp AS timestamp, data_sync1, data_sync2, data_sync3, data_sync4, raw_contacts._id AS _id, data._id AS data_id,raw_contacts.starred AS starred," + dbForProfile() + " AS raw_contact_is_user_profile,groups.sourceid AS group_sourceid, is_sdn_contact, raw_contacts.indicate_phone_or_sim_contact AS indicate_phone_or_sim_contact, raw_contacts.index_in_sim AS index_in_sim, raw_contacts.is_sdn_contact AS is_sdn_contact FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id) LEFT OUTER JOIN data ON (data.raw_contact_id=raw_contacts._id) LEFT OUTER JOIN packages ON (data.package_id=packages._id) LEFT OUTER JOIN mimetypes ON (data.mimetype_id=mimetypes._id) LEFT OUTER JOIN groups ON (mimetypes.mimetype='vnd.android.cursor.item/group_membership' AND groups._id=data.data1)"));
        sQLiteDatabase.execSQL("CREATE VIEW view_entities AS " + ("SELECT raw_contacts.contact_id AS _id, raw_contacts.contact_id AS contact_id, raw_contacts.deleted AS deleted,raw_contacts.metadata_dirty, is_primary, is_super_primary, data_version, data.package_id,package AS res_package,data.mimetype_id,mimetype AS mimetype, is_read_only, data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, data12, data13, data14, data15, carrier_presence, preferred_phone_account_component_name, preferred_phone_account_id, data_sync1, data_sync2, data_sync3, data_sync4, is_additional_number, raw_contacts.account_id,accounts.account_name AS account_name,accounts.account_type AS account_type,accounts.data_set AS data_set,(CASE WHEN accounts.data_set IS NULL THEN accounts.account_type ELSE accounts.account_type||'/'||accounts.data_set END) AS account_type_and_data_set,raw_contacts.sourceid AS sourceid,raw_contacts.backup_id AS backup_id,raw_contacts.version AS version,raw_contacts.dirty AS dirty,raw_contacts.sync1 AS sync1,raw_contacts.sync2 AS sync2,raw_contacts.sync3 AS sync3,raw_contacts.sync4 AS sync4,raw_contacts.timestamp AS timestamp, " + str + ", " + buildDisplayPhotoUriAlias("raw_contacts.contact_id", "photo_uri") + ", " + buildThumbnailPhotoUriAlias("raw_contacts.contact_id", "photo_thumb_uri") + ", " + dbForProfile() + " AS is_user_profile, data_sync1, data_sync2, data_sync3, data_sync4, raw_contacts._id AS raw_contact_id, data._id AS data_id,groups.sourceid AS group_sourceid FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id) JOIN contacts ON (raw_contacts.contact_id=contacts._id) JOIN raw_contacts AS name_raw_contact ON(name_raw_contact_id=name_raw_contact._id) LEFT OUTER JOIN data ON (data.raw_contact_id=raw_contacts._id) LEFT OUTER JOIN packages ON (data.package_id=packages._id) LEFT OUTER JOIN mimetypes ON (data.mimetype_id=mimetypes._id) LEFT OUTER JOIN groups ON (mimetypes.mimetype='vnd.android.cursor.item/group_membership' AND groups._id=data.data1)"));
        sQLiteDatabase.execSQL("CREATE VIEW view_data_usage AS " + ("SELECT stat_id, data_id, usage_type, x_times_used, x_last_time_used," + LowRes.getTimesUsedExpression("x_times_used") + " AS times_used," + LowRes.getLastTimeUsedExpression("x_last_time_used") + " AS last_time_used FROM data_usage_stat"));
        sQLiteDatabase.execSQL("CREATE VIEW view_data_usage_stat AS SELECT data_usage_stat.stat_id AS stat_id, data_id, raw_contacts.contact_id AS contact_id, mimetypes.mimetype AS mimetype, usage_type, x_times_used, x_last_time_used, times_used, last_time_used FROM view_data_usage AS data_usage_stat JOIN data ON (data._id=data_usage_stat.data_id) JOIN raw_contacts ON (raw_contacts._id=data.raw_contact_id ) JOIN mimetypes ON (mimetypes._id=data.mimetype_id)");
        sQLiteDatabase.execSQL("CREATE VIEW view_stream_items AS SELECT stream_items._id, contacts._id AS contact_id, contacts.lookup AS contact_lookup, accounts.account_name, accounts.account_type, accounts.data_set, stream_items.raw_contact_id as raw_contact_id, raw_contacts.sourceid as raw_contact_source_id, stream_items.res_package, stream_items.icon, stream_items.label, stream_items.text, stream_items.timestamp, stream_items.comments, stream_items.stream_item_sync1, stream_items.stream_item_sync2, stream_items.stream_item_sync3, stream_items.stream_item_sync4 FROM stream_items JOIN raw_contacts ON (stream_items.raw_contact_id=raw_contacts._id) JOIN accounts ON (raw_contacts.account_id=accounts._id) JOIN contacts ON (raw_contacts.contact_id=contacts._id)");
        sQLiteDatabase.execSQL("CREATE VIEW view_metadata_sync AS SELECT metadata_sync._id, raw_contact_backup_id, account_name, account_type, data_set, data, deleted FROM metadata_sync JOIN accounts ON (metadata_sync.account_id=accounts._id)");
        sQLiteDatabase.execSQL("CREATE VIEW view_metadata_sync_state AS SELECT metadata_sync_state._id, account_name, account_type, data_set, state FROM metadata_sync_state JOIN accounts ON (metadata_sync_state.account_id=accounts._id)");
    }

    private static String buildDisplayPhotoUriAlias(String str, String str2) {
        return "(CASE WHEN photo_file_id IS NULL THEN (CASE WHEN photo_id IS NULL OR photo_id=0 THEN NULL ELSE '" + ContactsContract.Contacts.CONTENT_URI + "/'||" + str + "|| '/photo' END) ELSE '" + ContactsContract.DisplayPhoto.CONTENT_URI + "/'||photo_file_id END) AS " + str2;
    }

    private static String buildThumbnailPhotoUriAlias(String str, String str2) {
        return "(CASE WHEN photo_id IS NULL OR photo_id=0 THEN NULL ELSE '" + ContactsContract.Contacts.CONTENT_URI + "/'||" + str + "|| '/photo' END) AS " + str2;
    }

    protected int dbForProfile() {
        return 0;
    }

    private void createGroupsView(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_groups;");
        sQLiteDatabase.execSQL("CREATE VIEW view_groups AS " + ("SELECT groups._id AS _id,groups.account_id AS account_id,accounts.account_name AS account_name,accounts.account_type AS account_type,accounts.data_set AS data_set,(CASE WHEN accounts.data_set IS NULL THEN accounts.account_type ELSE accounts.account_type||'/'||accounts.data_set END) AS account_type_and_data_set,sourceid,version,dirty,title,title_res,notes,system_id,deleted,group_visible,should_sync,auto_add,favorites,group_is_read_only,sync1,sync2,sync3,sync4,package AS res_package FROM groups JOIN accounts ON (groups.account_id=accounts._id) LEFT OUTER JOIN packages ON (groups.package_id=packages._id)"));
    }

    @Override
    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        Log.i("ContactsDatabaseHelper", "ContactsProvider cannot proceed because downgrading your database is not supported. To continue, please either re-upgrade to your previous Android version, or clear all application data in Contacts Storage (this will result in the loss of all local contacts that are not synced). To avoid data loss, your contacts database will not be wiped automatically.");
        super.onDowngrade(sQLiteDatabase, i, i2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8;
        Log.i("ContactsDatabaseHelper", "Upgrading contacts2.db from version " + i + " to " + i2);
        prepopulateCommonMimeTypes(sQLiteDatabase);
        if (i < 701) {
            upgradeToVersion701(sQLiteDatabase);
            i = 701;
        }
        if (i < 702) {
            upgradeToVersion702(sQLiteDatabase);
            i = 702;
        }
        boolean z9 = true;
        if (i < 703) {
            i = 703;
            z = true;
        } else {
            z = false;
        }
        if (i < 704) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS activities;");
            i = 704;
        }
        if (i < 706) {
            i = 706;
            z2 = true;
        } else {
            z2 = false;
        }
        if (i < 745) {
            z4 = true;
            i = 745;
            z3 = true;
        } else {
            z3 = false;
            z4 = false;
        }
        if (i < 746) {
            i = 746;
        }
        if (i < 747) {
            z = true;
            i = 747;
        }
        if (i < 748) {
            z2 = true;
            i = 748;
        }
        if (z3) {
            upgradeDeleteAllSimData(sQLiteDatabase);
        }
        if (i < 767) {
            upgradeToVersion767(sQLiteDatabase);
            z4 = true;
            i = 767;
        }
        if (i < 768) {
            i = 768;
            z5 = true;
        } else {
            z5 = false;
        }
        if (i < 769) {
            z5 = true;
            i = 769;
        }
        if (i < 770) {
            upgradeToVersion770(sQLiteDatabase);
            i = 770;
            z4 = true;
        }
        if (i < 800) {
            upgradeToVersion800(sQLiteDatabase);
            i = 800;
        }
        if (i < 801) {
            PropertyUtils.setProperty(sQLiteDatabase, "database_time_created", String.valueOf(System.currentTimeMillis()));
            i = 801;
        }
        if (i < 802) {
            upgradeToVersion802(sQLiteDatabase);
            i = 802;
            z4 = true;
        }
        if (i < 803) {
            z = true;
            i = 803;
        }
        if (i == 803) {
            Log.i("ContactsDatabaseHelper", "[onUpgrade]mapping version 803 to 850");
            i = 850;
        }
        if (i < 804) {
            i = 804;
        }
        if (i < 900) {
            i = 900;
            z4 = true;
        }
        if (i < 901) {
            i = 901;
            z = true;
        }
        if (i < 902) {
            upgradeToVersion902(sQLiteDatabase);
            i = 902;
        }
        if (i < 903) {
            upgradeToVersion903(sQLiteDatabase);
            i = 903;
        }
        if (i < 904) {
            upgradeToVersion904(sQLiteDatabase);
            i = 904;
        }
        if (i < 905) {
            upgradeToVersion905(sQLiteDatabase);
            i = 905;
        }
        if (i < 906) {
            upgradeToVersion906(sQLiteDatabase);
            i = 906;
        }
        if (i < 907) {
            i = 907;
            z6 = true;
        } else {
            z6 = false;
        }
        if (i < 908) {
            upgradeToVersion908(sQLiteDatabase);
            i = 908;
        }
        if (i < 909) {
            upgradeToVersion909(sQLiteDatabase);
            i = 909;
        }
        if (i < 910) {
            upgradeToVersion910(sQLiteDatabase);
            i = 910;
        }
        if (i < 1000) {
            upgradeToVersion1000(sQLiteDatabase);
            i = 1000;
            z4 = true;
        }
        if (i < 1002) {
            upgradeToVersion1002(sQLiteDatabase);
            i = 1002;
            z2 = true;
        }
        if (i < 1003) {
            upgradeToVersion1003(sQLiteDatabase);
            i = 1003;
        }
        if (i < 1004) {
            upgradeToVersion1004(sQLiteDatabase);
            i = 1004;
        }
        if (i < 1005) {
            upgradeToVersion1005(sQLiteDatabase);
            i = 1005;
        }
        if (i < 1006) {
            i = 1006;
            z4 = true;
        }
        if (i < 1007) {
            upgradeToVersion1007(sQLiteDatabase);
            i = 1007;
        }
        if (i < 1009) {
            upgradeToVersion1009(sQLiteDatabase);
            i = 1009;
        }
        if (i < 1100) {
            upgradeToVersion1100(sQLiteDatabase);
            i = 1100;
            z4 = true;
        }
        if (i < 1101) {
            upgradeToVersion1101(sQLiteDatabase);
            i = 1101;
        }
        if (i < 1102) {
            upgradeToVersion1009(sQLiteDatabase);
            i = 1102;
        }
        if (i < 1103) {
            i = 1103;
            z4 = true;
        }
        if (i < 1104) {
            upgradeToVersion1104(sQLiteDatabase);
            i = 1104;
            z4 = true;
        }
        if (i < 1105) {
            upgradeToVersion1105(sQLiteDatabase);
            i = 1105;
            z4 = true;
        }
        if (i < 1106) {
            upgradeToVersion1106(sQLiteDatabase);
            i = 1106;
        }
        if (i < 1107) {
            upgradeToVersion1107(sQLiteDatabase);
            i = 1107;
        }
        if (i < 1108) {
            upgradeToVersion1108(sQLiteDatabase);
            i = 1108;
        }
        if (isUpgradeRequired(i, i2, 1109)) {
            upgradeToVersion1109(sQLiteDatabase);
            i = 1109;
        }
        if (isUpgradeRequired(i, i2, 1110)) {
            upgradeToVersion1110(sQLiteDatabase);
            i = 1110;
        }
        if (isUpgradeRequired(i, i2, 1111)) {
            upgradeToVersion1111(sQLiteDatabase);
            i = 1111;
        }
        if (isUpgradeRequired(i, i2, 1200)) {
            createPresenceTables(sQLiteDatabase);
            i = 1200;
        }
        if (isUpgradeRequired(i, i2, 1201)) {
            upgradeToVersion1201(sQLiteDatabase);
            i = 1201;
            z4 = true;
        }
        if (isUpgradeRequired(i, i2, 1202)) {
            i = 1202;
            z4 = true;
        }
        if (isUpgradeRequired(i, i2, 1300)) {
            upgradeToVersion1300(sQLiteDatabase);
            i = 1300;
            z4 = true;
        }
        addMissingColumnsIfNeed(sQLiteDatabase);
        if (z4) {
            createContactsViews(sQLiteDatabase);
            createGroupsView(sQLiteDatabase);
            createContactsTriggers(sQLiteDatabase);
            createContactsIndexes(sQLiteDatabase, DISALLOW_SUB_QUERIES);
            z8 = true;
            z7 = true;
        } else {
            z7 = z2;
            z8 = false;
        }
        if (z8) {
            LegacyApiSupport.createViews(sQLiteDatabase);
        }
        if (z5) {
            upgradeLocaleData(sQLiteDatabase, DISALLOW_SUB_QUERIES);
            z6 = false;
            z = true;
            z7 = true;
        }
        if (z6) {
            rebuildNameLookup(sQLiteDatabase, DISALLOW_SUB_QUERIES);
            z7 = true;
        }
        if (z) {
            rebuildSearchIndex(sQLiteDatabase, DISALLOW_SUB_QUERIES);
        } else {
            z9 = z7;
        }
        if (z9) {
            updateSqliteStats(sQLiteDatabase);
        }
        if (i != i2) {
            throw new IllegalStateException("error upgrading the database to version " + i2);
        }
    }

    private static boolean isUpgradeRequired(int i, int i2, int i3) {
        if (i >= i3 || i2 < i3) {
            return DISALLOW_SUB_QUERIES;
        }
        return true;
    }

    private void rebuildNameLookup(SQLiteDatabase sQLiteDatabase, boolean z) {
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS name_lookup_index");
        insertNameLookup(sQLiteDatabase);
        createContactsIndexes(sQLiteDatabase, z);
    }

    protected void rebuildSearchIndex() {
        rebuildSearchIndex(getWritableDatabase(), true);
    }

    private void rebuildSearchIndex(SQLiteDatabase sQLiteDatabase, boolean z) {
        createSearchIndexTable(sQLiteDatabase, z);
        PropertyUtils.setProperty(sQLiteDatabase, "search_index", "0");
    }

    public boolean needsToUpdateLocaleData(LocaleSet localeSet) {
        if (!getProperty("locale", "").equals(localeSet.toString())) {
            return true;
        }
        String icuVersion = ICU.getIcuVersion();
        String property = getProperty("icu_version", "(unknown)");
        if (!icuVersion.equals(property)) {
            Log.i("ContactsDatabaseHelper", "ICU version has changed. Current version is " + icuVersion + "; DB was built with " + property);
            return true;
        }
        return DISALLOW_SUB_QUERIES;
    }

    private void upgradeLocaleData(SQLiteDatabase sQLiteDatabase, boolean z) {
        LocaleSet localeSetNewDefault = LocaleSet.newDefault();
        Log.i("ContactsDatabaseHelper", "Upgrading locale data for " + localeSetNewDefault + " (ICU v" + ICU.getIcuVersion() + ")");
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        rebuildLocaleData(sQLiteDatabase, localeSetNewDefault, z);
        Log.i("ContactsDatabaseHelper", "Locale update completed in " + (SystemClock.elapsedRealtime() - jElapsedRealtime) + "ms");
    }

    private void rebuildLocaleData(SQLiteDatabase sQLiteDatabase, LocaleSet localeSet, boolean z) {
        sQLiteDatabase.execSQL("DROP INDEX raw_contact_sort_key1_index");
        sQLiteDatabase.execSQL("DROP INDEX raw_contact_sort_key2_index");
        sQLiteDatabase.execSQL("DROP INDEX IF EXISTS name_lookup_index");
        loadNicknameLookupTable(sQLiteDatabase);
        insertNameLookup(sQLiteDatabase);
        rebuildSortKeys(sQLiteDatabase);
        createContactsIndexes(sQLiteDatabase, z);
        FastScrollingIndexCache.getInstance(this.mContext).invalidate();
        PropertyUtils.setProperty(sQLiteDatabase, "icu_version", ICU.getIcuVersion());
        PropertyUtils.setProperty(sQLiteDatabase, "locale", localeSet.toString());
    }

    public void setLocale(LocaleSet localeSet) {
        if (!needsToUpdateLocaleData(localeSet)) {
            return;
        }
        Log.i("ContactsDatabaseHelper", "Switching to locale " + localeSet + " (ICU v" + ICU.getIcuVersion() + ")");
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.setLocale(localeSet.getPrimaryLocale());
        writableDatabase.beginTransaction();
        try {
            rebuildLocaleData(writableDatabase, localeSet, true);
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
            Log.i("ContactsDatabaseHelper", "Locale change completed in " + (SystemClock.elapsedRealtime() - jElapsedRealtime) + "ms");
        } catch (Throwable th) {
            writableDatabase.endTransaction();
            throw th;
        }
    }

    private void rebuildSortKeys(SQLiteDatabase sQLiteDatabase) {
        Cursor cursorQuery = sQLiteDatabase.query("raw_contacts", new String[]{"_id"}, null, null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                updateRawContactDisplayName(sQLiteDatabase, cursorQuery.getLong(0));
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void insertNameLookup(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DELETE FROM name_lookup");
        SQLiteStatement sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT OR IGNORE INTO name_lookup(raw_contact_id,data_id,name_type,normalized_name) VALUES (?,?,?,?)");
        try {
            insertStructuredNameLookup(sQLiteDatabase, sQLiteStatementCompileStatement);
            insertEmailLookup(sQLiteDatabase, sQLiteStatementCompileStatement);
            insertNicknameLookup(sQLiteDatabase, sQLiteStatementCompileStatement);
        } finally {
            sQLiteStatementCompileStatement.close();
        }
    }

    private void insertStructuredNameLookup(SQLiteDatabase sQLiteDatabase, SQLiteStatement sQLiteStatement) {
        NameSplitter nameSplitterCreateNameSplitter = createNameSplitter();
        StructuredNameLookupBuilder structuredNameLookupBuilder = new StructuredNameLookupBuilder(nameSplitterCreateNameSplitter, new CommonNicknameCache(sQLiteDatabase), sQLiteStatement);
        Cursor cursorQuery = sQLiteDatabase.query("data", StructuredNameQuery.COLUMNS, "mimetype_id=? AND data1 NOT NULL", new String[]{String.valueOf(lookupMimeTypeId(sQLiteDatabase, "vnd.android.cursor.item/name"))}, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                long j = cursorQuery.getLong(0);
                long j2 = cursorQuery.getLong(1);
                String string = cursorQuery.getString(2);
                structuredNameLookupBuilder.insertNameLookup(j2, j, string, nameSplitterCreateNameSplitter.getAdjustedFullNameStyle(nameSplitterCreateNameSplitter.guessFullNameStyle(string)));
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void insertEmailLookup(SQLiteDatabase sQLiteDatabase, SQLiteStatement sQLiteStatement) {
        Cursor cursorQuery = sQLiteDatabase.query("data", EmailQuery.COLUMNS, "mimetype_id=? AND data1 NOT NULL", new String[]{String.valueOf(lookupMimeTypeId(sQLiteDatabase, "vnd.android.cursor.item/email_v2"))}, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                insertNameLookup(sQLiteStatement, cursorQuery.getLong(1), cursorQuery.getLong(0), 4, extractHandleFromEmailAddress(cursorQuery.getString(2)));
            } finally {
                cursorQuery.close();
            }
        }
    }

    private void insertNicknameLookup(SQLiteDatabase sQLiteDatabase, SQLiteStatement sQLiteStatement) {
        Cursor cursorQuery = sQLiteDatabase.query("data", NicknameQuery.COLUMNS, "mimetype_id=? AND data1 NOT NULL", new String[]{String.valueOf(lookupMimeTypeId(sQLiteDatabase, "vnd.android.cursor.item/nickname"))}, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                insertNameLookup(sQLiteStatement, cursorQuery.getLong(1), cursorQuery.getLong(0), 3, cursorQuery.getString(2));
            } finally {
                cursorQuery.close();
            }
        }
    }

    public void insertNameLookup(SQLiteStatement sQLiteStatement, long j, long j2, int i, String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        String strNormalize = NameNormalizer.normalize(str);
        if (TextUtils.isEmpty(strNormalize)) {
            return;
        }
        insertNormalizedNameLookup(sQLiteStatement, j, j2, i, strNormalize);
    }

    private void insertNormalizedNameLookup(SQLiteStatement sQLiteStatement, long j, long j2, int i, String str) {
        sQLiteStatement.bindLong(1, j);
        sQLiteStatement.bindLong(2, j2);
        sQLiteStatement.bindLong(3, i);
        sQLiteStatement.bindString(4, str);
        sQLiteStatement.executeInsert();
    }

    private void upgradeDeleteAllSimData(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DELETE FROM raw_contacts WHERE indicate_phone_or_sim_contact>0");
        sQLiteDatabase.execSQL("DELETE FROM groups WHERE account_id IN ( SELECT _id FROM accounts WHERE account_type ='" + SimCardUtils.getSimAccountType(1) + "') ");
    }

    private void upgradeToVersion701(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET last_time_contacted = max(ifnull(last_time_contacted, 0),  ifnull((SELECT max(last_time_used)  FROM data JOIN data_usage_stat ON (data._id = data_usage_stat.data_id) WHERE data.raw_contact_id = raw_contacts._id), 0))");
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET last_time_contacted = null where last_time_contacted = 0");
    }

    private void upgradeToVersion702(SQLiteDatabase sQLiteDatabase) {
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id, raw_contact_id, data1 FROM data  WHERE mimetype_id=(SELECT _id FROM mimetypes WHERE mimetype='vnd.android.cursor.item/phone_v2') AND data4 not like '+%'", null);
        try {
            int count = cursorRawQuery.getCount();
            if (count == 0) {
                return;
            }
            long[] jArr = new long[count];
            long[] jArr2 = new long[count];
            String[] strArr = new String[count];
            StringBuilder sb = new StringBuilder();
            cursorRawQuery.moveToPosition(-1);
            while (cursorRawQuery.moveToNext()) {
                int position = cursorRawQuery.getPosition();
                jArr[position] = cursorRawQuery.getLong(0);
                jArr2[position] = cursorRawQuery.getLong(1);
                strArr[position] = cursorRawQuery.getString(2);
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(jArr[position]);
            }
            cursorRawQuery.close();
            String string = sb.toString();
            sQLiteDatabase.execSQL("UPDATE data SET data4 = null WHERE _id IN (" + string + ")");
            sQLiteDatabase.execSQL("DELETE FROM phone_lookup WHERE data_id IN (" + string + ")");
            for (int i = 0; i < count; i++) {
                String str = strArr[i];
                if (!TextUtils.isEmpty(str)) {
                    String strNormalizeNumber = PhoneNumberUtils.normalizeNumber(str);
                    if (!TextUtils.isEmpty(strNormalizeNumber)) {
                        sQLiteDatabase.execSQL("INSERT INTO phone_lookup(data_id, raw_contact_id, normalized_number, min_match) VALUES(?,?,?,?)", new String[]{String.valueOf(jArr[i]), String.valueOf(jArr2[i]), strNormalizeNumber, PhoneNumberUtils.toCallerIDMinMatch(strNormalizeNumber)});
                    }
                }
            }
        } finally {
            cursorRawQuery.close();
        }
    }

    private void upgradeToVersion767(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE raw_contacts ADD phonebook_label TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE raw_contacts ADD phonebook_bucket INTEGER;");
        sQLiteDatabase.execSQL("ALTER TABLE raw_contacts ADD phonebook_label_alt TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE raw_contacts ADD phonebook_bucket_alt INTEGER;");
    }

    private void upgradeToVersion770(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE contacts ADD contact_last_updated_timestamp INTEGER;");
        sQLiteDatabase.execSQL("UPDATE contacts SET contact_last_updated_timestamp = " + System.currentTimeMillis());
        sQLiteDatabase.execSQL("CREATE INDEX contacts_contact_last_updated_timestamp_index ON contacts(contact_last_updated_timestamp)");
        sQLiteDatabase.execSQL("CREATE TABLE deleted_contacts (contact_id INTEGER PRIMARY KEY,contact_deleted_timestamp INTEGER NOT NULL default 0);");
        sQLiteDatabase.execSQL("CREATE INDEX deleted_contacts_contact_deleted_timestamp_index ON deleted_contacts(contact_deleted_timestamp)");
    }

    private void upgradeToVersion800(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD presentation INTEGER NOT NULL DEFAULT 1;");
        sQLiteDatabase.execSQL("UPDATE calls SET presentation=2, number='' WHERE number='-2';");
        sQLiteDatabase.execSQL("UPDATE calls SET presentation=3, number='' WHERE number='-1';");
        sQLiteDatabase.execSQL("UPDATE calls SET presentation=4, number='' WHERE number='-3';");
    }

    private void upgradeToVersion802(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE contacts ADD pinned INTEGER NOT NULL DEFAULT 0;");
        sQLiteDatabase.execSQL("ALTER TABLE raw_contacts ADD pinned INTEGER NOT NULL DEFAULT  0;");
    }

    private void upgradeToVersion902(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD subscription_component_name TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD subscription_id TEXT;");
    }

    private void upgradeToVersion903(SQLiteDatabase sQLiteDatabase) {
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id, number, countryiso FROM calls  WHERE (normalized_number is null OR normalized_number = '')  AND countryiso != '' AND countryiso is not null  AND number != '' AND number is not null;", null);
        try {
            if (cursorRawQuery.getCount() == 0) {
                return;
            }
            sQLiteDatabase.beginTransaction();
            try {
                cursorRawQuery.moveToPosition(-1);
                while (cursorRawQuery.moveToNext()) {
                    long j = cursorRawQuery.getLong(0);
                    String numberToE164 = PhoneNumberUtils.formatNumberToE164(cursorRawQuery.getString(1), cursorRawQuery.getString(2));
                    if (!TextUtils.isEmpty(numberToE164)) {
                        sQLiteDatabase.execSQL("UPDATE calls set normalized_number = ? where _id = ?;", new String[]{numberToE164, String.valueOf(j)});
                    }
                }
                sQLiteDatabase.setTransactionSuccessful();
            } finally {
                sQLiteDatabase.endTransaction();
            }
        } finally {
            cursorRawQuery.close();
        }
    }

    private void upgradeToVersion904(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD features INTEGER NOT NULL DEFAULT 0;");
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD data_usage INTEGER;");
    }

    private void upgradeToVersion905(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD transcription TEXT;");
    }

    @VisibleForTesting
    public void upgradeToVersion906(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("UPDATE contacts SET pinned = pinned + 1 WHERE pinned >= 0 AND pinned < 2147483647;");
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET pinned = pinned + 1 WHERE pinned >= 0 AND pinned < 2147483647;");
        sQLiteDatabase.execSQL("UPDATE contacts SET pinned = 0 WHERE pinned = 2147483647;");
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET pinned = 0 WHERE pinned = 2147483647;");
    }

    private void upgradeToVersion908(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("UPDATE contacts SET pinned = 0 WHERE pinned = 2147483647;");
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET pinned = 0 WHERE pinned = 2147483647;");
    }

    private void upgradeToVersion909(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("ALTER TABLE calls ADD sub_id INTEGER DEFAULT -1;");
        } catch (SQLiteException e) {
            sQLiteDatabase.execSQL("UPDATE calls SET subscription_component_name='com.android.phone/com.android.services.telephony.TelephonyConnectionService';");
            sQLiteDatabase.execSQL("UPDATE calls SET subscription_id=sub_id;");
        }
    }

    private void upgradeToVersion910(SQLiteDatabase sQLiteDatabase) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager.getUserInfo(userManager.getUserHandle()).isManagedProfile()) {
            sQLiteDatabase.execSQL("DELETE FROM calls;");
        }
    }

    private void upgradeToVersion1000(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE raw_contacts ADD backup_id TEXT;");
        sQLiteDatabase.execSQL("ALTER TABLE data ADD hash_id TEXT;");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS raw_contacts_backup_id_account_id_index ON raw_contacts (backup_id, account_id);");
        sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS data_hash_id_index ON data (hash_id);");
    }

    @VisibleForTesting
    public void upgradeToVersion1002(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS pre_authorized_uris;");
        sQLiteDatabase.execSQL("CREATE TABLE pre_authorized_uris (_id INTEGER PRIMARY KEY AUTOINCREMENT, uri STRING NOT NULL, expiration INTEGER NOT NULL DEFAULT 0);");
    }

    public void upgradeToVersion1003(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD phone_account_address TEXT;");
        SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(this.mContext);
        if (subscriptionManagerFrom != null) {
            Log.i("ContactsDatabaseHelper", "count: " + subscriptionManagerFrom.getAllSubscriptionInfoCount());
            for (SubscriptionInfo subscriptionInfo : subscriptionManagerFrom.getAllSubscriptionInfoList()) {
                String iccId = subscriptionInfo.getIccId();
                int subscriptionId = subscriptionInfo.getSubscriptionId();
                if (!TextUtils.isEmpty(iccId) && subscriptionId != -1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("UPDATE calls SET subscription_id=");
                    DatabaseUtils.appendEscapedSQLString(sb, iccId);
                    sb.append(" WHERE subscription_id=");
                    sb.append(subscriptionId);
                    sb.append(" AND subscription_component_name='com.android.phone/com.android.services.telephony.TelephonyConnectionService';");
                    sQLiteDatabase.execSQL(sb.toString());
                }
            }
        }
    }

    public void upgradeToVersion1004(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD phone_account_hidden INTEGER NOT NULL DEFAULT 0;");
    }

    public void upgradeToVersion1005(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD photo_uri TEXT;");
    }

    public void upgradeToVersion1007(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("ALTER TABLE voicemail_status ADD phone_account_component_name TEXT;");
            sQLiteDatabase.execSQL("ALTER TABLE voicemail_status ADD phone_account_id TEXT;");
            sQLiteDatabase.execSQL("ALTER TABLE calls ADD dirty INTEGER NOT NULL DEFAULT 0;");
            sQLiteDatabase.execSQL("ALTER TABLE calls ADD deleted INTEGER NOT NULL DEFAULT 0;");
        } catch (SQLiteException e) {
            Log.v("ContactsDatabaseHelper", "Version 1007: Columns already exist, skipping upgrade steps.");
        }
    }

    public void upgradeToVersion1009(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("ALTER TABLE data ADD carrier_presence INTEGER NOT NULL DEFAULT 0");
        } catch (SQLiteException e) {
        }
    }

    private void upgradeToVersion1100(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE raw_contacts ADD metadata_dirty INTEGER NOT NULL DEFAULT 0;");
    }

    public void upgradeToVersion1101(SQLiteDatabase sQLiteDatabase) {
        SQLiteStatement sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("UPDATE data SET hash_id=? WHERE _id=?");
        Cursor cursorQuery = sQLiteDatabase.query("data", new String[]{"_id", "data1", "data2", "data15"}, null, null, null, null, "_id");
        while (cursorQuery.moveToNext()) {
            try {
                long j = cursorQuery.getLong(0);
                String strLegacyGenerateHashId = legacyGenerateHashId(cursorQuery.getString(1), cursorQuery.getString(2), cursorQuery.getBlob(3));
                if (!TextUtils.isEmpty(strLegacyGenerateHashId)) {
                    sQLiteStatementCompileStatement.bindString(1, strLegacyGenerateHashId);
                    sQLiteStatementCompileStatement.bindLong(2, j);
                    sQLiteStatementCompileStatement.execute();
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    public void upgradeToVersion1104(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS metadata_sync;");
        sQLiteDatabase.execSQL("CREATE TABLE metadata_sync (_id INTEGER PRIMARY KEY AUTOINCREMENT, raw_contact_backup_id TEXT NOT NULL, account_id INTEGER NOT NULL, data TEXT, deleted INTEGER NOT NULL DEFAULT 0);");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX metadata_sync_index ON metadata_sync (raw_contact_backup_id, account_id);");
    }

    public void upgradeToVersion1105(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS metadata_sync_state;");
        sQLiteDatabase.execSQL("CREATE TABLE metadata_sync_state (_id INTEGER PRIMARY KEY AUTOINCREMENT, account_id INTEGER NOT NULL, state BLOB);");
        sQLiteDatabase.execSQL("CREATE UNIQUE INDEX metadata_sync_state_index ON metadata_sync_state (account_id);");
    }

    public void upgradeToVersion1106(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD post_dial_digits TEXT NOT NULL DEFAULT ''");
    }

    public void upgradeToVersion1107(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("ALTER TABLE calls ADD post_dial_digits TEXT NOT NULL DEFAULT ''");
        } catch (SQLiteException e) {
        }
    }

    public void upgradeToVersion1108(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD add_for_all_users INTEGER NOT NULL DEFAULT 1");
    }

    public void upgradeToVersion1109(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE voicemail_status ADD quota_occupied INTEGER DEFAULT -1;");
        sQLiteDatabase.execSQL("ALTER TABLE voicemail_status ADD quota_total INTEGER DEFAULT -1;");
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD last_modified INTEGER DEFAULT 0;");
    }

    public void upgradeToVersion1110(SQLiteDatabase sQLiteDatabase) {
        long jLookupMimeTypeId = lookupMimeTypeId(sQLiteDatabase, "vnd.android.cursor.item/photo");
        ContentValues contentValues = new ContentValues();
        contentValues.put("hash_id", getPhotoHashId());
        sQLiteDatabase.update("data", contentValues, "mimetype_id = " + jLookupMimeTypeId, null);
    }

    public String getPhotoHashId() {
        return generateHashId("vnd.android.cursor.item/photo", null);
    }

    @VisibleForTesting
    public void upgradeToVersion1111(SQLiteDatabase sQLiteDatabase) {
        ContactLocaleUtils contactLocaleUtils = ContactLocaleUtils.getInstance();
        int numberBucketIndex = contactLocaleUtils.getNumberBucketIndex();
        String bucketLabel = contactLocaleUtils.getBucketLabel(numberBucketIndex);
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET phonebook_bucket = " + numberBucketIndex + ", phonebook_label='" + bucketLabel + "' WHERE sort_key IS NULL AND phonebook_bucket=0;");
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET phonebook_bucket_alt = " + numberBucketIndex + ", phonebook_label_alt='" + bucketLabel + "' WHERE sort_key_alt IS NULL AND phonebook_bucket_alt=0;");
        FastScrollingIndexCache.getInstance(this.mContext).invalidate();
    }

    private void upgradeToVersion1201(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE contacts ADD x_times_contacted INTEGER NOT NULL DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE contacts ADD x_last_time_contacted INTEGER");
        sQLiteDatabase.execSQL("ALTER TABLE raw_contacts ADD x_times_contacted INTEGER NOT NULL DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE raw_contacts ADD x_last_time_contacted INTEGER");
        sQLiteDatabase.execSQL("ALTER TABLE data_usage_stat ADD x_times_used INTEGER NOT NULL DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE data_usage_stat ADD x_last_time_used INTEGER NOT NULL DEFAULT 0");
        sQLiteDatabase.execSQL("UPDATE contacts SET x_times_contacted = ifnull(times_contacted,0),x_last_time_contacted = ifnull(last_time_contacted,0),times_contacted = 0,last_time_contacted = 0");
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET x_times_contacted = ifnull(times_contacted,0),x_last_time_contacted = ifnull(last_time_contacted,0),times_contacted = 0,last_time_contacted = 0");
        sQLiteDatabase.execSQL("UPDATE data_usage_stat SET x_times_used = ifnull(times_used,0),x_last_time_used = ifnull(last_time_used,0),times_used = 0,last_time_used = 0");
    }

    public void upgradeToVersion1300(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("ALTER TABLE data ADD preferred_phone_account_component_name TEXT;");
            sQLiteDatabase.execSQL("ALTER TABLE data ADD preferred_phone_account_id TEXT;");
        } catch (SQLiteException e) {
        }
    }

    public String legacyGenerateHashId(String str, String str2, byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(str) || !TextUtils.isEmpty(str2)) {
            sb.append(str);
            sb.append(str2);
            bArr = sb.toString().getBytes();
        } else if (bArr == null) {
            bArr = null;
        }
        if (bArr != null) {
            return generateHashIdForData(bArr);
        }
        return null;
    }

    public String generateHashId(String str, String str2) {
        byte[] bytes;
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(str) || !TextUtils.isEmpty(str2)) {
            sb.append(str);
            sb.append(str2);
            bytes = sb.toString().getBytes();
        } else {
            bytes = null;
        }
        if (bytes != null) {
            return generateHashIdForData(bytes);
        }
        return null;
    }

    @VisibleForTesting
    String generateHashIdForData(byte[] bArr) {
        String strEncodeToString;
        synchronized (this.mMessageDigest) {
            strEncodeToString = Base64.encodeToString(this.mMessageDigest.digest(bArr), 0);
        }
        return strEncodeToString;
    }

    public String extractHandleFromEmailAddress(String str) {
        String address;
        int iIndexOf;
        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(str);
        if (rfc822TokenArr.length == 0 || (iIndexOf = (address = rfc822TokenArr[0].getAddress()).indexOf(64)) == -1) {
            return null;
        }
        return address.substring(0, iIndexOf);
    }

    public String extractAddressFromEmailAddress(String str) {
        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(str);
        if (rfc822TokenArr.length == 0) {
            return null;
        }
        return rfc822TokenArr[0].getAddress().trim();
    }

    private long insertMimeType(SQLiteDatabase sQLiteDatabase, String str) {
        long jInsertWithOneArgAndReturnId = insertWithOneArgAndReturnId(sQLiteDatabase, "INSERT INTO mimetypes(mimetype) VALUES (?)", str);
        if (jInsertWithOneArgAndReturnId >= 0) {
            return jInsertWithOneArgAndReturnId;
        }
        return lookupMimeTypeId(sQLiteDatabase, str);
    }

    private long lookupMimeTypeId(SQLiteDatabase sQLiteDatabase, String str) {
        Long l = this.mCommonMimeTypeIdsCache.get(str);
        if (l != null) {
            return l.longValue();
        }
        Long lValueOf = Long.valueOf(queryIdWithOneArg(sQLiteDatabase, "SELECT _id FROM mimetypes WHERE mimetype=?", str));
        if (lValueOf.longValue() < 0) {
            Log.e("ContactsDatabaseHelper", "Mimetype " + str + " not found in the MIMETYPES table");
        }
        return lValueOf.longValue();
    }

    private static void bindString(SQLiteStatement sQLiteStatement, int i, String str) {
        if (str == null) {
            sQLiteStatement.bindNull(i);
        } else {
            sQLiteStatement.bindString(i, str);
        }
    }

    private void bindLong(SQLiteStatement sQLiteStatement, int i, Number number) {
        if (number == null) {
            sQLiteStatement.bindNull(i);
        } else {
            sQLiteStatement.bindLong(i, number.longValue());
        }
    }

    private void updateSqliteStats(SQLiteDatabase sQLiteDatabase) {
        if (!this.mDatabaseOptimizationEnabled) {
            return;
        }
        try {
            sQLiteDatabase.execSQL("DELETE FROM sqlite_stat1");
            updateIndexStats(sQLiteDatabase, "contacts", "contacts_has_phone_index", "9000 500");
            updateIndexStats(sQLiteDatabase, "contacts", "contacts_name_raw_contact_id_index", "9000 1");
            updateIndexStats(sQLiteDatabase, "contacts", MoreDatabaseUtils.buildIndexName("contacts", "contact_last_updated_timestamp"), "9000 10");
            updateIndexStats(sQLiteDatabase, "raw_contacts", "raw_contacts_contact_id_index", "10000 2");
            updateIndexStats(sQLiteDatabase, "raw_contacts", "raw_contact_sort_key2_index", "10000 2");
            updateIndexStats(sQLiteDatabase, "raw_contacts", "raw_contact_sort_key1_index", "10000 2");
            updateIndexStats(sQLiteDatabase, "raw_contacts", "raw_contacts_source_id_account_id_index", "10000 1 1");
            updateIndexStats(sQLiteDatabase, "name_lookup", "name_lookup_raw_contact_id_index", "35000 4");
            updateIndexStats(sQLiteDatabase, "name_lookup", "name_lookup_index", "35000 2 2 2 1");
            updateIndexStats(sQLiteDatabase, "name_lookup", "sqlite_autoindex_name_lookup_1", "35000 3 2 1");
            updateIndexStats(sQLiteDatabase, "phone_lookup", "phone_lookup_index", "3500 3 2 1");
            updateIndexStats(sQLiteDatabase, "phone_lookup", "phone_lookup_min_match_index", "3500 3 2 2");
            updateIndexStats(sQLiteDatabase, "phone_lookup", "phone_lookup_data_id_min_match_index", "3500 2 2");
            updateIndexStats(sQLiteDatabase, "data", "data_mimetype_data1_index", "60000 5000 2");
            updateIndexStats(sQLiteDatabase, "data", "data_raw_contact_id", "60000 10");
            updateIndexStats(sQLiteDatabase, "groups", "groups_source_id_account_id_index", "50 2 2 1 1");
            updateIndexStats(sQLiteDatabase, "nickname_lookup", "nickname_lookup_index", "500 2 1");
            updateIndexStats(sQLiteDatabase, "status_updates", null, "100");
            updateIndexStats(sQLiteDatabase, "stream_items", null, "500");
            updateIndexStats(sQLiteDatabase, "stream_item_photos", null, "50");
            updateIndexStats(sQLiteDatabase, "accounts", null, "3");
            updateIndexStats(sQLiteDatabase, "pre_authorized_uris", null, "1");
            updateIndexStats(sQLiteDatabase, "visible_contacts", null, "2000");
            updateIndexStats(sQLiteDatabase, "photo_files", null, "50");
            updateIndexStats(sQLiteDatabase, "default_directory", null, "1500");
            updateIndexStats(sQLiteDatabase, "mimetypes", "mime_type", "18 1");
            updateIndexStats(sQLiteDatabase, "data_usage_stat", "data_usage_stat_index", "20 2 1");
            updateIndexStats(sQLiteDatabase, "metadata_sync", "metadata_sync_index", "10000 1 1");
            updateIndexStats(sQLiteDatabase, "agg_exceptions", null, "10");
            updateIndexStats(sQLiteDatabase, "settings", null, "10");
            updateIndexStats(sQLiteDatabase, "packages", null, "0");
            updateIndexStats(sQLiteDatabase, "directories", null, "3");
            updateIndexStats(sQLiteDatabase, "v1_settings", null, "0");
            updateIndexStats(sQLiteDatabase, "android_metadata", null, "1");
            updateIndexStats(sQLiteDatabase, "_sync_state", "sqlite_autoindex__sync_state_1", "2 1 1");
            updateIndexStats(sQLiteDatabase, "_sync_state_metadata", null, "1");
            updateIndexStats(sQLiteDatabase, "properties", "sqlite_autoindex_properties_1", "4 1");
            updateIndexStats(sQLiteDatabase, "metadata_sync_state", "metadata_sync_state_index", "2 1 1");
            updateIndexStats(sQLiteDatabase, "search_index_docsize", null, "9000");
            updateIndexStats(sQLiteDatabase, "search_index_content", null, "9000");
            updateIndexStats(sQLiteDatabase, "search_index_stat", null, "1");
            updateIndexStats(sQLiteDatabase, "search_index_segments", null, "450");
            updateIndexStats(sQLiteDatabase, "search_index_segdir", "sqlite_autoindex_search_index_segdir_1", "9 5 1");
            updateIndexStats(sQLiteDatabase, "presence", "presenceIndex", "1 1");
            updateIndexStats(sQLiteDatabase, "presence", "presenceIndex2", "1 1");
            updateIndexStats(sQLiteDatabase, "agg_presence", null, "1");
            sQLiteDatabase.execSQL("ANALYZE sqlite_master;");
        } catch (SQLException e) {
            Log.e("ContactsDatabaseHelper", "Could not update index stats", e);
        }
    }

    private void updateIndexStats(SQLiteDatabase sQLiteDatabase, String str, String str2, String str3) {
        if (str2 == null) {
            sQLiteDatabase.execSQL("DELETE FROM sqlite_stat1 WHERE tbl=? AND idx IS NULL", new String[]{str});
        } else {
            sQLiteDatabase.execSQL("DELETE FROM sqlite_stat1 WHERE tbl=? AND idx=?", new String[]{str, str2});
        }
        sQLiteDatabase.execSQL("INSERT INTO sqlite_stat1 (tbl,idx,stat) VALUES (?,?,?)", new String[]{str, str2, str3});
    }

    public void wipeData() {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.execSQL("DELETE FROM accounts;");
        writableDatabase.execSQL("DELETE FROM contacts;");
        writableDatabase.execSQL("DELETE FROM raw_contacts;");
        writableDatabase.execSQL("DELETE FROM stream_items;");
        writableDatabase.execSQL("DELETE FROM stream_item_photos;");
        writableDatabase.execSQL("DELETE FROM photo_files;");
        writableDatabase.execSQL("DELETE FROM data;");
        writableDatabase.execSQL("DELETE FROM phone_lookup;");
        writableDatabase.execSQL("DELETE FROM name_lookup;");
        writableDatabase.execSQL("DELETE FROM groups;");
        writableDatabase.execSQL("DELETE FROM agg_exceptions;");
        writableDatabase.execSQL("DELETE FROM settings;");
        writableDatabase.execSQL("DELETE FROM directories;");
        writableDatabase.execSQL("DELETE FROM search_index;");
        writableDatabase.execSQL("DELETE FROM deleted_contacts;");
        writableDatabase.execSQL("DELETE FROM mimetypes;");
        writableDatabase.execSQL("DELETE FROM packages;");
        writableDatabase.execSQL("DELETE FROM presence;");
        writableDatabase.execSQL("DELETE FROM agg_presence;");
        prepopulateCommonMimeTypes(writableDatabase);
    }

    public NameSplitter createNameSplitter() {
        return createNameSplitter(Locale.getDefault());
    }

    public NameSplitter createNameSplitter(Locale locale) {
        this.mNameSplitter = new NameSplitter(this.mContext.getString(android.R.string.accessibility_shortcut_spoken_feedback), this.mContext.getString(android.R.string.accessibility_shortcut_on), this.mContext.getString(android.R.string.accessibility_shortcut_toogle_warning), this.mContext.getString(android.R.string.accessibility_shortcut_single_service_warning_title), locale);
        return this.mNameSplitter;
    }

    @VisibleForTesting
    static long queryIdWithOneArg(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        SQLiteStatement sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement(str);
        try {
            bindString(sQLiteStatementCompileStatement, 1, str2);
            try {
                return sQLiteStatementCompileStatement.simpleQueryForLong();
            } catch (SQLiteDoneException e) {
                return -1L;
            }
        } finally {
            sQLiteStatementCompileStatement.close();
        }
    }

    @VisibleForTesting
    static long insertWithOneArgAndReturnId(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        SQLiteStatement sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement(str);
        try {
            bindString(sQLiteStatementCompileStatement, 1, str2);
            try {
                return sQLiteStatementCompileStatement.executeInsert();
            } catch (SQLiteConstraintException e) {
                return -1L;
            }
        } finally {
            sQLiteStatementCompileStatement.close();
        }
    }

    public long getPackageId(String str) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        long jQueryIdWithOneArg = queryIdWithOneArg(writableDatabase, "SELECT _id FROM packages WHERE package=?", str);
        if (jQueryIdWithOneArg >= 0) {
            return jQueryIdWithOneArg;
        }
        long jInsertWithOneArgAndReturnId = insertWithOneArgAndReturnId(writableDatabase, "INSERT INTO packages(package) VALUES (?)", str);
        if (jInsertWithOneArgAndReturnId >= 0) {
            return jInsertWithOneArgAndReturnId;
        }
        return queryIdWithOneArg(writableDatabase, "SELECT _id FROM packages WHERE package=?", str);
    }

    public long getMimeTypeId(String str) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        long jLookupMimeTypeId = lookupMimeTypeId(writableDatabase, str);
        if (jLookupMimeTypeId < 0) {
            return insertMimeType(writableDatabase, str);
        }
        return jLookupMimeTypeId;
    }

    public long getMimeTypeIdForStructuredName() {
        return lookupMimeTypeId(getWritableDatabase(), "vnd.android.cursor.item/name");
    }

    public long getMimeTypeIdForStructuredPostal() {
        return lookupMimeTypeId(getWritableDatabase(), "vnd.android.cursor.item/postal-address_v2");
    }

    public long getMimeTypeIdForOrganization() {
        return lookupMimeTypeId(getWritableDatabase(), "vnd.android.cursor.item/organization");
    }

    public long getMimeTypeIdForIm() {
        return lookupMimeTypeId(getWritableDatabase(), "vnd.android.cursor.item/im");
    }

    public long getMimeTypeIdForEmail() {
        return lookupMimeTypeId(getWritableDatabase(), "vnd.android.cursor.item/email_v2");
    }

    public long getMimeTypeIdForPhone() {
        return lookupMimeTypeId(getWritableDatabase(), "vnd.android.cursor.item/phone_v2");
    }

    public long getMimeTypeIdForSip() {
        return lookupMimeTypeId(getWritableDatabase(), "vnd.android.cursor.item/sip_address");
    }

    public long getMimeTypeIdForImsCall() {
        return lookupMimeTypeId(getWritableDatabase(), "vnd.android.cursor.item/ims");
    }

    private int getDisplayNameSourceForMimeTypeId(int i) {
        long j = i;
        if (j == this.mCommonMimeTypeIdsCache.get("vnd.android.cursor.item/name").longValue()) {
            return 40;
        }
        if (j == this.mCommonMimeTypeIdsCache.get("vnd.android.cursor.item/email_v2").longValue()) {
            return 10;
        }
        if (j == this.mCommonMimeTypeIdsCache.get("vnd.android.cursor.item/phone_v2").longValue()) {
            return 20;
        }
        if (j == this.mCommonMimeTypeIdsCache.get("vnd.android.cursor.item/organization").longValue()) {
            return 30;
        }
        if (j == this.mCommonMimeTypeIdsCache.get("vnd.android.cursor.item/nickname").longValue()) {
            return 35;
        }
        return 0;
    }

    public String getDataMimeType(long j) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("SELECT mimetype FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) WHERE data._id=?");
        try {
            sQLiteStatementCompileStatement.bindLong(1, j);
            return sQLiteStatementCompileStatement.simpleQueryForString();
        } catch (SQLiteDoneException e) {
            return null;
        }
    }

    public Set<AccountWithDataSet> getAllAccountsWithDataSets() {
        ArraySet arraySet = new ArraySet();
        Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT DISTINCT _id,account_name,account_type,data_set FROM accounts", null);
        while (cursorRawQuery.moveToNext()) {
            try {
                arraySet.add(AccountWithDataSet.get(cursorRawQuery.getString(1), cursorRawQuery.getString(2), cursorRawQuery.getString(3)));
            } finally {
                cursorRawQuery.close();
            }
        }
        return arraySet;
    }

    public Long getAccountIdOrNull(AccountWithDataSet accountWithDataSet) {
        if (accountWithDataSet == null) {
            accountWithDataSet = AccountWithDataSet.LOCAL;
        }
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("SELECT _id FROM accounts WHERE ((?1 IS NULL AND account_name IS NULL) OR (account_name=?1)) AND ((?2 IS NULL AND account_type IS NULL) OR (account_type=?2)) AND ((?3 IS NULL AND data_set IS NULL) OR (data_set=?3))");
        try {
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 1, accountWithDataSet.getAccountName());
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 2, accountWithDataSet.getAccountType());
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 3, accountWithDataSet.getDataSet());
            try {
                return Long.valueOf(sQLiteStatementCompileStatement.simpleQueryForLong());
            } catch (SQLiteDoneException e) {
                return null;
            }
        } finally {
            sQLiteStatementCompileStatement.close();
        }
    }

    public long getOrCreateAccountIdInTransaction(AccountWithDataSet accountWithDataSet) {
        if (accountWithDataSet == null) {
            accountWithDataSet = AccountWithDataSet.LOCAL;
        }
        Long accountIdOrNull = getAccountIdOrNull(accountWithDataSet);
        if (accountIdOrNull != null) {
            createSimSettingInTransaction(accountWithDataSet);
            return accountIdOrNull.longValue();
        }
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("INSERT INTO accounts (account_name, account_type, data_set) VALUES (?, ?, ?)");
        try {
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 1, accountWithDataSet.getAccountName());
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 2, accountWithDataSet.getAccountType());
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 3, accountWithDataSet.getDataSet());
            Long lValueOf = Long.valueOf(sQLiteStatementCompileStatement.executeInsert());
            sQLiteStatementCompileStatement.close();
            createSimSettingInTransaction(accountWithDataSet);
            return lValueOf.longValue();
        } catch (Throwable th) {
            sQLiteStatementCompileStatement.close();
            throw th;
        }
    }

    public String getSettingAccountNameOrNull(AccountWithDataSet accountWithDataSet) {
        if (accountWithDataSet == null) {
            accountWithDataSet = AccountWithDataSet.LOCAL;
        }
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("SELECT account_name FROM settings WHERE account_name= ?");
        try {
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 1, accountWithDataSet.getAccountName());
            try {
                return sQLiteStatementCompileStatement.simpleQueryForString();
            } catch (SQLiteDoneException e) {
                return null;
            }
        } finally {
            sQLiteStatementCompileStatement.close();
        }
    }

    public void createSimSettingInTransaction(AccountWithDataSet accountWithDataSet) {
        String accountType = accountWithDataSet.getAccountType();
        if (accountWithDataSet == null || accountType == null || !AccountUtils.isSimAccount(accountType) || getSettingAccountNameOrNull(accountWithDataSet) != null) {
            return;
        }
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("INSERT INTO settings (account_name, account_type, data_set, ungrouped_visible, should_sync) VALUES (?, ?, ?, ?, ?)");
        try {
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 1, accountWithDataSet.getAccountName());
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 2, accountWithDataSet.getAccountType());
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 3, accountWithDataSet.getDataSet());
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 4, "1");
            DatabaseUtils.bindObjectToProgram(sQLiteStatementCompileStatement, 5, "1");
            sQLiteStatementCompileStatement.executeInsert();
        } finally {
            sQLiteStatementCompileStatement.close();
        }
    }

    public void updateAllVisible() {
        updateCustomContactVisibility(getWritableDatabase(), -1L);
    }

    public boolean updateContactVisibleOnlyIfChanged(TransactionContext transactionContext, long j) {
        return updateContactVisible(transactionContext, j, true);
    }

    public void updateContactVisible(TransactionContext transactionContext, long j) {
        updateContactVisible(transactionContext, j, DISALLOW_SUB_QUERIES);
    }

    public boolean updateContactVisible(TransactionContext transactionContext, long j, boolean z) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        updateCustomContactVisibility(writableDatabase, j);
        String strValueOf = String.valueOf(j);
        long mimeTypeId = getMimeTypeId("vnd.android.cursor.item/group_membership");
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT EXISTS (SELECT contact_id FROM raw_contacts JOIN data   ON (raw_contacts._id=raw_contact_id) WHERE contact_id=?1   AND mimetype_id=?2) OR EXISTS (SELECT _id FROM raw_contacts WHERE contact_id=?1   AND NOT EXISTS (SELECT _id  FROM groups  WHERE raw_contacts.account_id = groups.account_id  AND auto_add != 0)) OR EXISTS (SELECT _id FROM raw_contacts WHERE contact_id=?1   AND raw_contacts.account_id=");
        sb.append(Clauses.LOCAL_ACCOUNT_ID);
        sb.append(")");
        boolean z2 = DatabaseUtils.longForQuery(writableDatabase, sb.toString(), new String[]{strValueOf, String.valueOf(mimeTypeId)}) != 0;
        if (z && isContactInDefaultDirectory(writableDatabase, j) == z2) {
            return DISALLOW_SUB_QUERIES;
        }
        if (z2) {
            writableDatabase.execSQL("INSERT OR IGNORE INTO default_directory VALUES(?)", new String[]{strValueOf});
            transactionContext.invalidateSearchIndexForContact(j);
        } else {
            writableDatabase.execSQL("DELETE FROM default_directory WHERE _id=?", new String[]{strValueOf});
            writableDatabase.execSQL("DELETE FROM search_index WHERE contact_id=CAST(? AS int)", new String[]{strValueOf});
        }
        return true;
    }

    public boolean isContactInDefaultDirectory(SQLiteDatabase sQLiteDatabase, long j) {
        SQLiteStatement sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("SELECT EXISTS (SELECT 1 FROM default_directory WHERE _id=?)");
        sQLiteStatementCompileStatement.bindLong(1, j);
        if (sQLiteStatementCompileStatement.simpleQueryForLong() != 0) {
            return true;
        }
        return DISALLOW_SUB_QUERIES;
    }

    private void updateCustomContactVisibility(SQLiteDatabase sQLiteDatabase, long j) {
        String str;
        String[] strArr = {String.valueOf(getMimeTypeId("vnd.android.cursor.item/group_membership"))};
        if (j < 0) {
            str = "";
        } else {
            str = "_id=" + j + " AND ";
        }
        sQLiteDatabase.execSQL("DELETE FROM visible_contacts WHERE _id IN(SELECT _id FROM contacts WHERE " + str + "(SELECT MAX((SELECT (CASE WHEN (CASE WHEN COUNT(groups._id)=0 THEN ungrouped_visible ELSE MAX(group_visible)END)=1 THEN 1 ELSE 0 END) FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id)LEFT OUTER JOIN settings ON ( (accounts.account_name IS NULL AND (settings.account_name = 'Phone' OR settings.account_name = 'Tablet') AND accounts.account_type IS NULL AND settings.account_type = 'Local Phone Account' ) OR accounts.account_name=settings.account_name AND accounts.account_type=settings.account_type AND ((accounts.data_set IS NULL AND settings.data_set IS NULL) OR (accounts.data_set=settings.data_set))) LEFT OUTER JOIN data ON (data.mimetype_id=? AND data.raw_contact_id = raw_contacts._id) LEFT OUTER JOIN groups ON (groups._id = data.data1) WHERE raw_contacts._id=outer_raw_contacts._id)) FROM raw_contacts AS outer_raw_contacts WHERE contact_id=contacts._id GROUP BY contact_id)=0) ", strArr);
        sQLiteDatabase.execSQL("INSERT INTO visible_contacts SELECT _id FROM contacts WHERE " + str + "_id NOT IN visible_contacts AND (SELECT MAX((SELECT (CASE WHEN (CASE WHEN COUNT(groups._id)=0 THEN ungrouped_visible ELSE MAX(group_visible)END)=1 THEN 1 ELSE 0 END) FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id)LEFT OUTER JOIN settings ON ( (accounts.account_name IS NULL AND (settings.account_name = 'Phone' OR settings.account_name = 'Tablet') AND accounts.account_type IS NULL AND settings.account_type = 'Local Phone Account' ) OR accounts.account_name=settings.account_name AND accounts.account_type=settings.account_type AND ((accounts.data_set IS NULL AND settings.data_set IS NULL) OR (accounts.data_set=settings.data_set))) LEFT OUTER JOIN data ON (data.mimetype_id=? AND data.raw_contact_id = raw_contacts._id) LEFT OUTER JOIN groups ON (groups._id = data.data1) WHERE raw_contacts._id=outer_raw_contacts._id)) FROM raw_contacts AS outer_raw_contacts WHERE contact_id=contacts._id GROUP BY contact_id)=1 ", strArr);
    }

    public long getContactId(long j) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("SELECT contact_id FROM raw_contacts WHERE _id=?");
        try {
            sQLiteStatementCompileStatement.bindLong(1, j);
            return sQLiteStatementCompileStatement.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            return 0L;
        }
    }

    public int getAggregationMode(long j) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("SELECT aggregation_mode FROM raw_contacts WHERE _id=?");
        try {
            sQLiteStatementCompileStatement.bindLong(1, j);
            return (int) sQLiteStatementCompileStatement.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            return 3;
        }
    }

    public void buildPhoneLookupAndContactQuery(SQLiteQueryBuilder sQLiteQueryBuilder, String str, String str2) {
        String callerIDMinMatch = PhoneNumberUtils.toCallerIDMinMatch(str);
        StringBuilder sb = new StringBuilder();
        appendPhoneLookupTables(sb, callerIDMinMatch, true);
        appendPresenceAndStautsTable(sb);
        sQLiteQueryBuilder.setTables(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        appendPhoneLookupSelection(sb2, str, str2);
        sQLiteQueryBuilder.appendWhere(sb2.toString());
    }

    public void buildFallbackPhoneLookupAndContactQuery(SQLiteQueryBuilder sQLiteQueryBuilder, String str) {
        String callerIDMinMatch = PhoneNumberUtils.toCallerIDMinMatch(str);
        StringBuilder sb = new StringBuilder();
        sb.append("raw_contacts");
        sb.append(" JOIN view_contacts as contacts_view ON (contacts_view._id = raw_contacts.contact_id) JOIN (SELECT data_id,normalized_number FROM phone_lookup WHERE (phone_lookup.min_match = '");
        sb.append(callerIDMinMatch);
        sb.append("')) AS lookup ON lookup.data_id=data._id JOIN data ON data.raw_contact_id=raw_contacts._id");
        appendPresenceAndStautsTable(sb);
        sQLiteQueryBuilder.setTables(sb.toString());
        sb.setLength(0);
        sb.append("PHONE_NUMBERS_EQUAL(data.data1, ");
        DatabaseUtils.appendEscapedSQLString(sb, str);
        sb.append(this.mUseStrictPhoneNumberComparison ? ", 1)" : ", 0)");
        sQLiteQueryBuilder.appendWhere(sb.toString());
    }

    public String[] buildSipContactQuery(StringBuilder sb, String str) {
        sb.append("upper(");
        sb.append("data1");
        sb.append(")=upper(?) AND ");
        sb.append("mimetype_id");
        sb.append(" IN ('" + Long.toString(getMimeTypeIdForSip()) + "', '" + Long.toString(getMimeTypeIdForImsCall()) + "', '" + Long.toString(getMimeTypeIdForPhone()) + "')");
        return new String[]{str};
    }

    public String buildPhoneLookupAsNestedQuery(String str) {
        StringBuilder sb = new StringBuilder();
        String callerIDMinMatch = PhoneNumberUtils.toCallerIDMinMatch(str);
        sb.append("(SELECT DISTINCT raw_contact_id FROM ");
        appendPhoneLookupTables(sb, callerIDMinMatch, DISALLOW_SUB_QUERIES);
        sb.append(" WHERE ");
        appendPhoneLookupSelection(sb, str, null);
        sb.append(")");
        return sb.toString();
    }

    private void appendPhoneLookupTables(StringBuilder sb, String str, boolean z) {
        sb.append("raw_contacts");
        if (z) {
            sb.append(" JOIN view_contacts contacts_view ON (contacts_view._id = raw_contacts.contact_id)");
        }
        sb.append(", (SELECT data_id, normalized_number, length(normalized_number) as len  FROM phone_lookup  WHERE (phone_lookup.min_match = '");
        sb.append(str);
        sb.append("')) AS lookup, data");
    }

    private void appendPhoneLookupSelection(StringBuilder sb, String str, String str2) {
        sb.append("lookup.data_id=data._id AND data.raw_contact_id=raw_contacts._id");
        boolean z = !TextUtils.isEmpty(str2);
        boolean z2 = !TextUtils.isEmpty(str);
        if (z || z2) {
            sb.append(" AND ( ");
            if (z) {
                sb.append(" lookup.normalized_number = ");
                DatabaseUtils.appendEscapedSQLString(sb, str2);
            }
            if (z && z2) {
                sb.append(" OR ");
            }
            if (z2) {
                if (!this.mUseStrictPhoneNumberComparison) {
                    int length = str.length();
                    sb.append(" lookup.len <= ");
                    sb.append(length);
                    sb.append(" AND substr(");
                    DatabaseUtils.appendEscapedSQLString(sb, str);
                    sb.append(',');
                    sb.append(length);
                    sb.append(" - lookup.len + 1) = lookup.normalized_number");
                    sb.append(" OR (");
                    sb.append(" lookup.len > ");
                    sb.append(length);
                    sb.append(" AND substr(lookup.normalized_number,");
                    sb.append("lookup.len + 1 - ");
                    sb.append(length);
                    sb.append(") = ");
                    DatabaseUtils.appendEscapedSQLString(sb, str);
                    sb.append(")");
                } else {
                    sb.append("0");
                }
            }
            sb.append(')');
        }
    }

    public String getUseStrictPhoneNumberComparisonParameter() {
        return this.mUseStrictPhoneNumberComparison ? "1" : "0";
    }

    private void loadNicknameLookupTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DELETE FROM nickname_lookup");
        String[] stringArray = this.mContext.getResources().getStringArray(android.R.array.carrier_properties);
        if (stringArray == null || stringArray.length == 0) {
            return;
        }
        SQLiteStatement sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("INSERT INTO nickname_lookup(name,cluster) VALUES (?,?)");
        for (int i = 0; i < stringArray.length; i++) {
            try {
                for (String str : stringArray[i].split(",")) {
                    try {
                        sQLiteStatementCompileStatement.bindString(1, NameNormalizer.normalize(str));
                        sQLiteStatementCompileStatement.bindString(2, String.valueOf(i));
                        sQLiteStatementCompileStatement.executeInsert();
                    } catch (SQLiteException e) {
                        Log.e("ContactsDatabaseHelper", "Cannot insert nickname: " + str, e);
                    }
                }
            } finally {
                sQLiteStatementCompileStatement.close();
            }
        }
    }

    public static void copyStringValue(ContentValues contentValues, String str, ContentValues contentValues2, String str2) {
        if (contentValues2.containsKey(str2)) {
            contentValues.put(str, contentValues2.getAsString(str2));
        }
    }

    public static void copyLongValue(ContentValues contentValues, String str, ContentValues contentValues2, String str2) {
        long jLongValue;
        if (contentValues2.containsKey(str2)) {
            Object obj = contentValues2.get(str2);
            if (obj instanceof Boolean) {
                jLongValue = ((Boolean) obj).booleanValue() ? 1L : 0L;
            } else if (obj instanceof String) {
                jLongValue = Long.parseLong((String) obj);
            } else {
                jLongValue = ((Number) obj).longValue();
            }
            contentValues.put(str, Long.valueOf(jLongValue));
        }
    }

    public SyncStateContentProviderHelper getSyncState() {
        return this.mSyncState;
    }

    public String getProperty(String str, String str2) {
        return PropertyUtils.getProperty(getReadableDatabase(), str, str2);
    }

    public void setProperty(String str, String str2) {
        PropertyUtils.setProperty(getWritableDatabase(), str, str2);
    }

    public void forceDirectoryRescan() {
        setProperty("directoryScanComplete", "0");
    }

    public static boolean isInProjection(String[] strArr, String str) {
        if (strArr == null) {
            return true;
        }
        for (String str2 : strArr) {
            if (str.equals(str2)) {
                return true;
            }
        }
        return DISALLOW_SUB_QUERIES;
    }

    public static boolean isInProjection(String[] strArr, String... strArr2) {
        if (strArr == null) {
            return true;
        }
        if (strArr2.length == 1) {
            return isInProjection(strArr, strArr2[0]);
        }
        for (String str : strArr) {
            for (String str2 : strArr2) {
                if (str2.equals(str)) {
                    return true;
                }
            }
        }
        return DISALLOW_SUB_QUERIES;
    }

    public String exceptionMessage(Uri uri) {
        return exceptionMessage(null, uri);
    }

    public String exceptionMessage(String str, Uri uri) {
        StringBuilder sb = new StringBuilder();
        if (str != null) {
            sb.append(str);
            sb.append("; ");
        }
        sb.append("URI: ");
        sb.append(uri);
        PackageManager packageManager = this.mContext.getPackageManager();
        int callingUid = Binder.getCallingUid();
        sb.append(", calling user: ");
        Object nameForUid = packageManager.getNameForUid(callingUid);
        if (nameForUid == null) {
            nameForUid = Integer.valueOf(callingUid);
        }
        sb.append(nameForUid);
        String[] packagesForUid = packageManager.getPackagesForUid(callingUid);
        if (packagesForUid != null && packagesForUid.length > 0) {
            if (packagesForUid.length == 1) {
                sb.append(", calling package:");
                sb.append(packagesForUid[0]);
            } else {
                sb.append(", calling package is one of: [");
                for (int i = 0; i < packagesForUid.length; i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append(packagesForUid[i]);
                }
                sb.append("]");
            }
        }
        return sb.toString();
    }

    public void deleteStatusUpdate(long j) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("DELETE FROM status_updates WHERE status_update_data_id=?");
        sQLiteStatementCompileStatement.bindLong(1, j);
        sQLiteStatementCompileStatement.execute();
    }

    public void replaceStatusUpdate(Long l, long j, String str, String str2, Integer num, Integer num2) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("INSERT OR REPLACE INTO status_updates(status_update_data_id, status_ts,status,status_res_package,status_icon,status_label) VALUES (?,?,?,?,?,?)");
        sQLiteStatementCompileStatement.bindLong(1, l.longValue());
        sQLiteStatementCompileStatement.bindLong(2, j);
        bindString(sQLiteStatementCompileStatement, 3, str);
        bindString(sQLiteStatementCompileStatement, 4, str2);
        bindLong(sQLiteStatementCompileStatement, 5, num);
        bindLong(sQLiteStatementCompileStatement, 6, num2);
        sQLiteStatementCompileStatement.execute();
    }

    public void insertStatusUpdate(Long l, String str, String str2, Integer num, Integer num2) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("INSERT INTO status_updates(status_update_data_id, status,status_res_package,status_icon,status_label) VALUES (?,?,?,?,?)");
        try {
            sQLiteStatementCompileStatement.bindLong(1, l.longValue());
            bindString(sQLiteStatementCompileStatement, 2, str);
            bindString(sQLiteStatementCompileStatement, 3, str2);
            bindLong(sQLiteStatementCompileStatement, 4, num);
            bindLong(sQLiteStatementCompileStatement, 5, num2);
            sQLiteStatementCompileStatement.executeInsert();
        } catch (SQLiteConstraintException e) {
            SQLiteStatement sQLiteStatementCompileStatement2 = getWritableDatabase().compileStatement("UPDATE status_updates SET status_ts=?,status=? WHERE status_update_data_id=? AND status!=?");
            sQLiteStatementCompileStatement2.bindLong(1, System.currentTimeMillis());
            bindString(sQLiteStatementCompileStatement2, 2, str);
            sQLiteStatementCompileStatement2.bindLong(3, l.longValue());
            bindString(sQLiteStatementCompileStatement2, 4, str);
            sQLiteStatementCompileStatement2.execute();
            SQLiteStatement sQLiteStatementCompileStatement3 = getWritableDatabase().compileStatement("UPDATE status_updates SET status_res_package=?,status_icon=?,status_label=? WHERE status_update_data_id=?");
            bindString(sQLiteStatementCompileStatement3, 1, str2);
            bindLong(sQLiteStatementCompileStatement3, 2, num);
            bindLong(sQLiteStatementCompileStatement3, 3, num2);
            sQLiteStatementCompileStatement3.bindLong(4, l.longValue());
            sQLiteStatementCompileStatement3.execute();
        }
    }

    public void updateRawContactDisplayName(SQLiteDatabase sQLiteDatabase, long j) {
        int adjustedFullNameStyle;
        String strJoin;
        String strJoin2;
        String strJoin3;
        String strJoin4;
        int i;
        String str;
        String str2;
        int iGuessPhoneticNameStyle;
        String str3;
        String str4;
        String str5;
        NameSplitter.Name name;
        if (this.mNameSplitter == null) {
            createNameSplitter();
        }
        int i2 = 0;
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT mimetype_id,is_primary,data1,data2,data3,data4,data5,data6,data7,data8,data9,data10,data11 FROM data WHERE raw_contact_id=? AND (data1 NOT NULL OR data8 NOT NULL OR data9 NOT NULL OR data10 NOT NULL OR data4 NOT NULL)", this.mSelectionArgs1);
        int i3 = 0;
        int i4 = 0;
        NameSplitter.Name name2 = null;
        String str6 = null;
        String string = null;
        while (cursorRawQuery.moveToNext()) {
            try {
                int i5 = cursorRawQuery.getInt(i2);
                int displayNameSourceForMimeTypeId = getDisplayNameSourceForMimeTypeId(i5);
                if (displayNameSourceForMimeTypeId == 40) {
                    String string2 = cursorRawQuery.getString(3);
                    String string3 = cursorRawQuery.getString(6);
                    String string4 = cursorRawQuery.getString(4);
                    String string5 = cursorRawQuery.getString(7);
                    String string6 = cursorRawQuery.getString(5);
                    if (TextUtils.isEmpty(string2) && TextUtils.isEmpty(string3) && TextUtils.isEmpty(string4) && TextUtils.isEmpty(string5) && TextUtils.isEmpty(string6)) {
                        displayNameSourceForMimeTypeId = 37;
                    }
                }
                if (displayNameSourceForMimeTypeId >= i3 && displayNameSourceForMimeTypeId != 0 && (displayNameSourceForMimeTypeId != i3 || cursorRawQuery.getInt(1) != 0)) {
                    long j2 = i5;
                    if (j2 == getMimeTypeIdForStructuredName()) {
                        if (name2 != null) {
                            name = new NameSplitter.Name();
                        } else {
                            name = this.mName;
                            name.clear();
                        }
                        name.prefix = cursorRawQuery.getString(5);
                        name.givenNames = cursorRawQuery.getString(3);
                        name.middleName = cursorRawQuery.getString(6);
                        name.familyName = cursorRawQuery.getString(4);
                        name.suffix = cursorRawQuery.getString(7);
                        name.fullNameStyle = cursorRawQuery.isNull(11) ? 0 : cursorRawQuery.getInt(11);
                        name.phoneticFamilyName = cursorRawQuery.getString(10);
                        name.phoneticMiddleName = cursorRawQuery.getString(9);
                        name.phoneticGivenName = cursorRawQuery.getString(8);
                        name.phoneticNameStyle = cursorRawQuery.isNull(12) ? 0 : cursorRawQuery.getInt(12);
                        if (name.isEmpty()) {
                            displayNameSourceForMimeTypeId = i3;
                            name = name2;
                        }
                        name2 = name;
                    } else if (j2 == getMimeTypeIdForOrganization()) {
                        this.mCharArrayBuffer.sizeCopied = 0;
                        cursorRawQuery.copyStringToBuffer(2, this.mCharArrayBuffer);
                        if (this.mCharArrayBuffer.sizeCopied != 0) {
                            String str7 = new String(this.mCharArrayBuffer.data, 0, this.mCharArrayBuffer.sizeCopied);
                            string = cursorRawQuery.getString(9);
                            i4 = cursorRawQuery.isNull(11) ? 0 : cursorRawQuery.getInt(11);
                            str6 = str7;
                        } else {
                            cursorRawQuery.copyStringToBuffer(5, this.mCharArrayBuffer);
                            if (this.mCharArrayBuffer.sizeCopied != 0) {
                                str5 = new String(this.mCharArrayBuffer.data, 0, this.mCharArrayBuffer.sizeCopied);
                                str6 = str5;
                                i3 = displayNameSourceForMimeTypeId;
                                string = null;
                                i4 = 0;
                            }
                        }
                    } else {
                        this.mCharArrayBuffer.sizeCopied = 0;
                        cursorRawQuery.copyStringToBuffer(2, this.mCharArrayBuffer);
                        if (this.mCharArrayBuffer.sizeCopied != 0) {
                            str5 = new String(this.mCharArrayBuffer.data, 0, this.mCharArrayBuffer.sizeCopied);
                            str6 = str5;
                            i3 = displayNameSourceForMimeTypeId;
                            string = null;
                            i4 = 0;
                        }
                    }
                    i3 = displayNameSourceForMimeTypeId;
                }
                i2 = 0;
            } finally {
                cursorRawQuery.close();
            }
        }
        if (i3 == 40 || i3 == 37) {
            int adjustedFullNameStyle2 = name2.fullNameStyle;
            if (adjustedFullNameStyle2 == 2 || adjustedFullNameStyle2 == 0) {
                adjustedFullNameStyle2 = this.mNameSplitter.getAdjustedFullNameStyle(adjustedFullNameStyle2);
                name2.fullNameStyle = adjustedFullNameStyle2;
            }
            adjustedFullNameStyle = adjustedFullNameStyle2;
            strJoin = this.mNameSplitter.join(name2, true, true);
            strJoin2 = this.mNameSplitter.join(name2, DISALLOW_SUB_QUERIES, true);
            if (TextUtils.isEmpty(name2.prefix)) {
                strJoin3 = strJoin;
                strJoin4 = strJoin2;
            } else {
                strJoin3 = this.mNameSplitter.join(name2, true, DISALLOW_SUB_QUERIES);
                strJoin4 = this.mNameSplitter.join(name2, DISALLOW_SUB_QUERIES, DISALLOW_SUB_QUERIES);
            }
            String strJoinPhoneticName = this.mNameSplitter.joinPhoneticName(name2);
            i = name2.phoneticNameStyle;
            str = strJoin4;
            str2 = strJoinPhoneticName;
        } else {
            strJoin = str6;
            strJoin3 = strJoin;
            str2 = string;
            i = i4;
            adjustedFullNameStyle = 0;
            strJoin2 = strJoin3;
            str = strJoin2;
        }
        if (str2 != null) {
            if (strJoin == null) {
                strJoin = str2;
            }
            if (strJoin2 == null) {
                strJoin2 = str2;
            }
            if (i == 0) {
                str3 = str2;
                str4 = str3;
                iGuessPhoneticNameStyle = this.mNameSplitter.guessPhoneticNameStyle(str2);
            } else {
                str3 = str2;
                str4 = str3;
                iGuessPhoneticNameStyle = i;
            }
        } else {
            if (adjustedFullNameStyle == 0) {
                int iGuessFullNameStyle = this.mNameSplitter.guessFullNameStyle(str6);
                if (iGuessFullNameStyle == 0 || iGuessFullNameStyle == 2) {
                    iGuessPhoneticNameStyle = 0;
                    iGuessFullNameStyle = this.mNameSplitter.getAdjustedNameStyleBasedOnPhoneticNameStyle(iGuessFullNameStyle, 0);
                } else {
                    iGuessPhoneticNameStyle = 0;
                }
                adjustedFullNameStyle = this.mNameSplitter.getAdjustedFullNameStyle(iGuessFullNameStyle);
            } else {
                iGuessPhoneticNameStyle = 0;
            }
            if (adjustedFullNameStyle == 3 || adjustedFullNameStyle == 2) {
                str3 = strJoin3;
                str4 = str3;
            } else {
                str3 = null;
                str4 = null;
            }
        }
        if (str4 == null) {
            str3 = str;
        } else {
            strJoin3 = str4;
        }
        ContactLocaleUtils contactLocaleUtils = ContactLocaleUtils.getInstance();
        int numberBucketIndex = TextUtils.isEmpty(strJoin3) ? contactLocaleUtils.getNumberBucketIndex() : contactLocaleUtils.getBucketIndex(strJoin3);
        String bucketLabel = contactLocaleUtils.getBucketLabel(numberBucketIndex);
        int numberBucketIndex2 = TextUtils.isEmpty(str3) ? contactLocaleUtils.getNumberBucketIndex() : contactLocaleUtils.getBucketIndex(str3);
        String bucketLabel2 = contactLocaleUtils.getBucketLabel(numberBucketIndex2);
        SQLiteStatement sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("UPDATE raw_contacts SET display_name_source=?,display_name=?,display_name_alt=?,phonetic_name=?,phonetic_name_style=?,sort_key=?,phonebook_label=?,phonebook_bucket=?,sort_key_alt=?,phonebook_label_alt=?,phonebook_bucket_alt=? WHERE _id=?");
        sQLiteStatementCompileStatement.bindLong(1, i3);
        bindString(sQLiteStatementCompileStatement, 2, strJoin);
        bindString(sQLiteStatementCompileStatement, 3, strJoin2);
        bindString(sQLiteStatementCompileStatement, 4, str2);
        sQLiteStatementCompileStatement.bindLong(5, iGuessPhoneticNameStyle);
        bindString(sQLiteStatementCompileStatement, 6, strJoin3);
        bindString(sQLiteStatementCompileStatement, 7, bucketLabel);
        sQLiteStatementCompileStatement.bindLong(8, numberBucketIndex);
        bindString(sQLiteStatementCompileStatement, 9, str3);
        bindString(sQLiteStatementCompileStatement, 10, bucketLabel2);
        sQLiteStatementCompileStatement.bindLong(11, numberBucketIndex2);
        sQLiteStatementCompileStatement.bindLong(12, j);
        sQLiteStatementCompileStatement.execute();
    }

    public void setIsPrimary(long j, long j2, long j3) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("UPDATE data SET is_primary=(_id=?) WHERE mimetype_id=?   AND raw_contact_id=?");
        sQLiteStatementCompileStatement.bindLong(1, j2);
        sQLiteStatementCompileStatement.bindLong(2, j3);
        sQLiteStatementCompileStatement.bindLong(3, j);
        sQLiteStatementCompileStatement.execute();
    }

    public void clearSuperPrimary(long j, long j2) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("UPDATE data SET is_super_primary=0 WHERE mimetype_id=?   AND raw_contact_id=?");
        sQLiteStatementCompileStatement.bindLong(1, j2);
        sQLiteStatementCompileStatement.bindLong(2, j);
        sQLiteStatementCompileStatement.execute();
    }

    public void setIsSuperPrimary(long j, long j2, long j3) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("UPDATE data SET is_super_primary=(_id=?) WHERE mimetype_id=?   AND raw_contact_id IN (SELECT _id FROM raw_contacts WHERE contact_id =(SELECT contact_id FROM raw_contacts WHERE _id=?))");
        sQLiteStatementCompileStatement.bindLong(1, j2);
        sQLiteStatementCompileStatement.bindLong(2, j3);
        sQLiteStatementCompileStatement.bindLong(3, j);
        sQLiteStatementCompileStatement.execute();
    }

    public void insertNameLookup(long j, long j2, int i, String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("INSERT OR IGNORE INTO name_lookup(raw_contact_id,data_id,name_type,normalized_name) VALUES (?,?,?,?)");
        sQLiteStatementCompileStatement.bindLong(1, j);
        sQLiteStatementCompileStatement.bindLong(2, j2);
        sQLiteStatementCompileStatement.bindLong(3, i);
        bindString(sQLiteStatementCompileStatement, 4, str);
        sQLiteStatementCompileStatement.executeInsert();
    }

    public void deleteNameLookup(long j) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("DELETE FROM name_lookup WHERE data_id=?");
        sQLiteStatementCompileStatement.bindLong(1, j);
        sQLiteStatementCompileStatement.execute();
    }

    public String insertNameLookupForEmail(long j, long j2, String str) {
        String strExtractHandleFromEmailAddress;
        if (TextUtils.isEmpty(str) || (strExtractHandleFromEmailAddress = extractHandleFromEmailAddress(str)) == null) {
            return null;
        }
        insertNameLookup(j, j2, 4, NameNormalizer.normalize(strExtractHandleFromEmailAddress));
        return strExtractHandleFromEmailAddress;
    }

    public void insertNameLookupForNickname(long j, long j2, String str) {
        if (!TextUtils.isEmpty(str)) {
            insertNameLookup(j, j2, 3, NameNormalizer.normalize(str));
        }
    }

    public boolean rawContactHasSuperPrimary(long j, long j2) {
        SQLiteDatabase readableDatabase = getReadableDatabase();
        String strValueOf = String.valueOf(j);
        boolean z = DISALLOW_SUB_QUERIES;
        Cursor cursorRawQuery = readableDatabase.rawQuery("SELECT EXISTS(SELECT 1 FROM data WHERE raw_contact_id=? AND mimetype_id=? AND is_super_primary<>0)", new String[]{strValueOf, String.valueOf(j2)});
        try {
            if (!cursorRawQuery.moveToFirst()) {
                throw new IllegalStateException();
            }
            if (cursorRawQuery.getInt(0) != 0) {
                z = true;
            }
            return z;
        } finally {
            cursorRawQuery.close();
        }
    }

    public String getCurrentCountryIso() {
        return this.mCountryMonitor.getCountryIso();
    }

    void setUseStrictPhoneNumberComparisonForTest(boolean z) {
        this.mUseStrictPhoneNumberComparison = z;
    }

    boolean getUseStrictPhoneNumberComparisonForTest() {
        return this.mUseStrictPhoneNumberComparison;
    }

    String querySearchIndexContentForTest(long j) {
        return DatabaseUtils.stringForQuery(getReadableDatabase(), "SELECT content FROM search_index WHERE contact_id=CAST(? AS int)", new String[]{String.valueOf(j)});
    }

    String querySearchIndexTokensForTest(long j) {
        return DatabaseUtils.stringForQuery(getReadableDatabase(), "SELECT tokens FROM search_index WHERE contact_id=CAST(? AS int)", new String[]{String.valueOf(j)});
    }

    public long upsertMetadataSync(String str, Long l, String str2, Integer num) {
        SQLiteStatement sQLiteStatementCompileStatement = getWritableDatabase().compileStatement("INSERT OR REPLACE INTO metadata_sync(raw_contact_backup_id, account_id, data,deleted) VALUES (?,?,?,?)");
        sQLiteStatementCompileStatement.bindString(1, str);
        sQLiteStatementCompileStatement.bindLong(2, l.longValue());
        if (str2 == null) {
            str2 = "";
        }
        sQLiteStatementCompileStatement.bindString(3, str2);
        sQLiteStatementCompileStatement.bindLong(4, num.intValue());
        return sQLiteStatementCompileStatement.executeInsert();
    }

    private void appendPresenceAndStautsTable(StringBuilder sb) {
        sb.append(" LEFT OUTER JOIN status_updates contacts_status_updates ON (status_update_id=contacts_status_updates.status_update_data_id)");
        sb.append(" LEFT OUTER JOIN agg_presence ON (contact_id = agg_presence.presence_contact_id)");
    }

    public static void notifyProviderStatusChange(Context context) {
        context.getContentResolver().notifyChange(ContactsContract.ProviderStatus.CONTENT_URI, (ContentObserver) null, DISALLOW_SUB_QUERIES);
    }

    public long getDatabaseCreationTime() {
        return this.mDatabaseCreationTime;
    }

    private SqlChecker getSqlChecker() {
        if (this.mCachedSqlChecker != null) {
            return this.mCachedSqlChecker;
        }
        this.mCachedSqlChecker = new SqlChecker(new ArrayList());
        return this.mCachedSqlChecker;
    }

    public void validateSql(String str, final String str2) {
        runSqlValidation(str, new Runnable() {
            @Override
            public void run() {
                ContactsDatabaseHelper.this.getSqlChecker().ensureNoInvalidTokens(str2);
            }
        });
    }

    public void validateContentValues(String str, final ContentValues contentValues) {
        runSqlValidation(str, new Runnable() {
            @Override
            public void run() {
                Iterator<String> it = contentValues.keySet().iterator();
                while (it.hasNext()) {
                    ContactsDatabaseHelper.this.getSqlChecker().ensureSingleTokenOnly(it.next());
                }
            }
        });
    }

    public void validateProjection(String str, final String[] strArr) {
        if (strArr != null) {
            runSqlValidation(str, new Runnable() {
                @Override
                public void run() {
                    for (String str2 : strArr) {
                        ContactsDatabaseHelper.this.getSqlChecker().ensureSingleTokenOnly(str2);
                    }
                }
            });
        }
    }

    private void runSqlValidation(String str, Runnable runnable) {
        try {
            runnable.run();
        } catch (SqlChecker.InvalidSqlException e) {
            reportInvalidSql(str, e);
        }
    }

    private void reportInvalidSql(String str, SqlChecker.InvalidSqlException invalidSqlException) {
        Log.e("ContactsDatabaseHelper", String.format("%s caller=%s", invalidSqlException.getMessage(), str));
        throw invalidSqlException;
    }

    private void preprocessToAddWhenOpen(SQLiteDatabase sQLiteDatabase) {
        if (isNeededToAddColumns(sQLiteDatabase)) {
            addContactsColumns(sQLiteDatabase);
            addRawContactsColumns(sQLiteDatabase);
            addDataColumns(sQLiteDatabase);
            createContactsViews(sQLiteDatabase);
            createGroupsView(sQLiteDatabase);
            createContactsTriggers(sQLiteDatabase);
            createContactsIndexes(sQLiteDatabase, DISALLOW_SUB_QUERIES);
            updateSqliteStats(sQLiteDatabase);
        }
    }

    private void addMissingColumnsIfNeed(SQLiteDatabase sQLiteDatabase) {
        addContactsColumns(sQLiteDatabase);
        addRawContactsColumns(sQLiteDatabase);
        addDataColumns(sQLiteDatabase);
    }

    private boolean isNeededToAddColumns(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.rawQuery("SELECT is_sdn_contact FROM contacts", null);
            return DISALLOW_SUB_QUERIES;
        } catch (SQLiteException e) {
            e.printStackTrace();
            if (e.toString().contains("no such column")) {
                return true;
            }
            return DISALLOW_SUB_QUERIES;
        }
    }

    private void addMissingColumns(SQLiteDatabase sQLiteDatabase, String str, ArrayList<String> arrayList) {
        try {
            sQLiteDatabase.rawQuery(str, null);
        } catch (SQLiteException e) {
            e.printStackTrace();
            if (e.toString().contains("no such column")) {
                Iterator<String> it = arrayList.iterator();
                while (it.hasNext()) {
                    try {
                        sQLiteDatabase.execSQL(it.next());
                    } catch (SQLiteException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }
    }

    private void addContactsColumns(SQLiteDatabase sQLiteDatabase) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("ALTER TABLE contacts ADD send_to_voicemail_vt INTEGER NOT NULL DEFAULT 0");
        arrayList.add("ALTER TABLE contacts ADD send_to_voicemail_sip INTEGER NOT NULL DEFAULT 0");
        arrayList.add("ALTER TABLE contacts ADD indicate_phone_or_sim_contact INTEGER NOT NULL DEFAULT -1");
        arrayList.add("ALTER TABLE contacts ADD index_in_sim INTEGER NOT NULL DEFAULT -1");
        arrayList.add("ALTER TABLE contacts ADD filter INTEGER NOT NULL DEFAULT 0");
        arrayList.add("ALTER TABLE contacts ADD is_sdn_contact INTEGER NOT NULL DEFAULT 0");
        addMissingColumns(sQLiteDatabase, "SELECT is_sdn_contact FROM contacts", arrayList);
    }

    private void addRawContactsColumns(SQLiteDatabase sQLiteDatabase) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("ALTER TABLE raw_contacts ADD timestamp INTEGER");
        arrayList.add("ALTER TABLE raw_contacts ADD send_to_voicemail_vt INTEGER NOT NULL DEFAULT 0");
        arrayList.add("ALTER TABLE raw_contacts ADD send_to_voicemail_sip INTEGER NOT NULL DEFAULT 0");
        arrayList.add("ALTER TABLE raw_contacts ADD indicate_phone_or_sim_contact INTEGER NOT NULL DEFAULT -1");
        arrayList.add("ALTER TABLE raw_contacts ADD index_in_sim INTEGER NOT NULL DEFAULT -1");
        arrayList.add("ALTER TABLE raw_contacts ADD is_sdn_contact INTEGER NOT NULL DEFAULT 0");
        addMissingColumns(sQLiteDatabase, "SELECT timestamp FROM raw_contacts", arrayList);
    }

    private void addDataColumns(SQLiteDatabase sQLiteDatabase) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("ALTER TABLE data ADD is_additional_number INTEGER NOT NULL DEFAULT 0");
        addMissingColumns(sQLiteDatabase, "SELECT is_additional_number FROM data", arrayList);
    }
}
