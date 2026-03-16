package com.android.providers.contacts;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.CharArrayBuffer;
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
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import com.android.common.content.SyncStateContentProviderHelper;
import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;
import com.android.providers.contacts.database.ContactsTableUtil;
import com.android.providers.contacts.database.DeletedContactsTableUtil;
import com.android.providers.contacts.database.MoreDatabaseUtils;
import com.google.android.collect.Sets;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import libcore.icu.ICU;

public class ContactsDatabaseHelper extends SQLiteOpenHelper {
    private static ContactsDatabaseHelper sSingleton = null;
    private SQLiteStatement mAggregationModeQuery;
    private CharArrayBuffer mCharArrayBuffer;
    private SQLiteStatement mClearSuperPrimaryStatement;
    private SQLiteStatement mContactIdQuery;
    private SQLiteStatement mContactInDefaultDirectoryQuery;
    private final Context mContext;
    private final CountryMonitor mCountryMonitor;
    private SQLiteStatement mDataMimetypeQuery;
    private final boolean mDatabaseOptimizationEnabled;
    private long mMimeTypeIdEmail;
    private long mMimeTypeIdIm;
    private long mMimeTypeIdNickname;
    private long mMimeTypeIdOrganization;
    private long mMimeTypeIdPhone;
    private long mMimeTypeIdSip;
    private long mMimeTypeIdStructuredName;
    private long mMimeTypeIdStructuredPostal;
    final ConcurrentHashMap<String, Long> mMimetypeCache;
    private NameSplitter.Name mName;
    private SQLiteStatement mNameLookupDelete;
    private SQLiteStatement mNameLookupInsert;
    private NameSplitter mNameSplitter;
    final ConcurrentHashMap<String, Long> mPackageCache;
    private SQLiteStatement mRawContactDisplayNameUpdate;
    private SQLiteStatement mResetNameVerifiedForOtherRawContacts;
    private StringBuilder mSb;
    private String[] mSelectionArgs1;
    private SQLiteStatement mSetPrimaryStatement;
    private SQLiteStatement mSetSuperPrimaryStatement;
    private SQLiteStatement mStatusAttributionUpdate;
    private SQLiteStatement mStatusUpdateAutoTimestamp;
    private SQLiteStatement mStatusUpdateDelete;
    private SQLiteStatement mStatusUpdateInsert;
    private SQLiteStatement mStatusUpdateReplace;
    private final SyncStateContentProviderHelper mSyncState;
    private boolean mUseStrictPhoneNumberComparison;

    private interface EmailQuery {
        public static final String[] COLUMNS = {"_id", "raw_contact_id", "data1"};
    }

    private interface NicknameQuery {
        public static final String[] COLUMNS = {"_id", "raw_contact_id", "data1"};
    }

    private interface Organization205Query {
        public static final String[] COLUMNS = {"data._id", "raw_contact_id", "data1", "data8"};
    }

    public interface Projections {
        public static final String[] ID = {"_id"};
        public static final String[] LITERAL_ONE = {"1"};
    }

    private interface StructName205Query {
        public static final String[] COLUMNS = {"data._id", "raw_contact_id", "display_name_source", "display_name", "data4", "data2", "data5", "data3", "data6", "data9", "data8", "data7"};
    }

    private interface StructuredNameQuery {
        public static final String[] COLUMNS = {"_id", "raw_contact_id", "data1"};
    }

    public interface Tables {
        public static final String[] SEQUENCE_TABLES = {"contacts", "raw_contacts", "stream_items", "stream_item_photos", "photo_files", "data", "groups", "calls", "directories"};
    }

    private interface Upgrade303Query {
        public static final String[] COLUMNS = {"_id", "raw_contact_id", "data1"};
    }

    private class StructuredNameLookupBuilder extends NameLookupBuilder {
        private final CommonNicknameCache mCommonNicknameCache;
        private final SQLiteStatement mNameLookupInsert;

        public StructuredNameLookupBuilder(NameSplitter splitter, CommonNicknameCache commonNicknameCache, SQLiteStatement nameLookupInsert) {
            super(splitter);
            this.mCommonNicknameCache = commonNicknameCache;
            this.mNameLookupInsert = nameLookupInsert;
        }

        @Override
        protected void insertNameLookup(long rawContactId, long dataId, int lookupType, String name) {
            if (!TextUtils.isEmpty(name)) {
                ContactsDatabaseHelper.this.insertNormalizedNameLookup(this.mNameLookupInsert, rawContactId, dataId, lookupType, name);
            }
        }

        @Override
        protected String[] getCommonNicknameClusters(String normalizedName) {
            return this.mCommonNicknameCache.getCommonNicknameClusters(normalizedName);
        }
    }

    public static synchronized ContactsDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new ContactsDatabaseHelper(context, "contacts2.db", true);
        }
        return sSingleton;
    }

    static ContactsDatabaseHelper getNewInstanceForTest(Context context) {
        return new ContactsDatabaseHelper(context, null, false);
    }

    protected ContactsDatabaseHelper(Context context, String databaseName, boolean optimizationEnabled) {
        super(context, databaseName, (SQLiteDatabase.CursorFactory) null, 911);
        this.mMimetypeCache = new ConcurrentHashMap<>();
        this.mPackageCache = new ConcurrentHashMap<>();
        this.mSb = new StringBuilder();
        this.mSelectionArgs1 = new String[1];
        this.mName = new NameSplitter.Name();
        this.mCharArrayBuffer = new CharArrayBuffer(128);
        this.mDatabaseOptimizationEnabled = optimizationEnabled;
        Resources resources = context.getResources();
        this.mContext = context;
        this.mSyncState = new SyncStateContentProviderHelper();
        this.mCountryMonitor = new CountryMonitor(context);
        this.mUseStrictPhoneNumberComparison = resources.getBoolean(android.R.^attr-private.defaultQueryHint);
    }

    public SQLiteDatabase getDatabase(boolean writable) {
        return writable ? getWritableDatabase() : getReadableDatabase();
    }

    private void refreshDatabaseCaches(SQLiteDatabase db) {
        this.mStatusUpdateDelete = null;
        this.mStatusUpdateReplace = null;
        this.mStatusUpdateInsert = null;
        this.mStatusUpdateAutoTimestamp = null;
        this.mStatusAttributionUpdate = null;
        this.mResetNameVerifiedForOtherRawContacts = null;
        this.mRawContactDisplayNameUpdate = null;
        this.mSetPrimaryStatement = null;
        this.mClearSuperPrimaryStatement = null;
        this.mSetSuperPrimaryStatement = null;
        this.mNameLookupInsert = null;
        this.mNameLookupDelete = null;
        this.mDataMimetypeQuery = null;
        this.mContactIdQuery = null;
        this.mAggregationModeQuery = null;
        this.mContactInDefaultDirectoryQuery = null;
        initializeCache(db);
    }

    private void initializeCache(SQLiteDatabase db) {
        this.mMimetypeCache.clear();
        this.mPackageCache.clear();
        this.mMimeTypeIdEmail = lookupMimeTypeId("vnd.android.cursor.item/email_v2", db);
        this.mMimeTypeIdIm = lookupMimeTypeId("vnd.android.cursor.item/im", db);
        this.mMimeTypeIdNickname = lookupMimeTypeId("vnd.android.cursor.item/nickname", db);
        this.mMimeTypeIdOrganization = lookupMimeTypeId("vnd.android.cursor.item/organization", db);
        this.mMimeTypeIdPhone = lookupMimeTypeId("vnd.android.cursor.item/phone_v2", db);
        this.mMimeTypeIdSip = lookupMimeTypeId("vnd.android.cursor.item/sip_address", db);
        this.mMimeTypeIdStructuredName = lookupMimeTypeId("vnd.android.cursor.item/name", db);
        this.mMimeTypeIdStructuredPostal = lookupMimeTypeId("vnd.android.cursor.item/postal-address_v2", db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        refreshDatabaseCaches(db);
        this.mSyncState.onDatabaseOpened(db);
        db.execSQL("ATTACH DATABASE ':memory:' AS presence_db;");
        db.execSQL("CREATE TABLE IF NOT EXISTS presence_db.presence (presence_data_id INTEGER PRIMARY KEY REFERENCES data(_id),protocol INTEGER NOT NULL,custom_protocol TEXT,im_handle TEXT,im_account TEXT,presence_contact_id INTEGER REFERENCES contacts(_id),presence_raw_contact_id INTEGER REFERENCES raw_contacts(_id),mode INTEGER,chat_capability INTEGER NOT NULL DEFAULT 0,UNIQUE(protocol, custom_protocol, im_handle, im_account));");
        db.execSQL("CREATE INDEX IF NOT EXISTS presence_db.presenceIndex ON presence (presence_raw_contact_id);");
        db.execSQL("CREATE INDEX IF NOT EXISTS presence_db.presenceIndex2 ON presence (presence_contact_id);");
        db.execSQL("CREATE TABLE IF NOT EXISTS presence_db.agg_presence (presence_contact_id INTEGER PRIMARY KEY REFERENCES contacts(_id),mode INTEGER,chat_capability INTEGER NOT NULL DEFAULT 0);");
        db.execSQL("CREATE TRIGGER presence_db.presence_deleted BEFORE DELETE ON presence_db.presence BEGIN    DELETE FROM agg_presence     WHERE presence_contact_id = (SELECT presence_contact_id FROM presence WHERE presence_raw_contact_id=OLD.presence_raw_contact_id AND NOT EXISTS(SELECT presence_raw_contact_id FROM presence WHERE presence_contact_id=OLD.presence_contact_id AND presence_raw_contact_id!=OLD.presence_raw_contact_id)); END");
        db.execSQL("CREATE TRIGGER presence_db.presence_inserted AFTER INSERT ON presence_db.presence BEGIN INSERT OR REPLACE INTO agg_presence(presence_contact_id, mode, chat_capability) SELECT presence_contact_id,mode,chat_capability FROM presence WHERE  (ifnull(mode,0)  * 10 + ifnull(chat_capability, 0)) = (SELECT MAX (ifnull(mode,0)  * 10 + ifnull(chat_capability, 0)) FROM presence WHERE presence_contact_id=NEW.presence_contact_id) AND presence_contact_id=NEW.presence_contact_id; END");
        db.execSQL("CREATE TRIGGER presence_db.presence_updated AFTER UPDATE ON presence_db.presence BEGIN INSERT OR REPLACE INTO agg_presence(presence_contact_id, mode, chat_capability) SELECT presence_contact_id,mode,chat_capability FROM presence WHERE  (ifnull(mode,0)  * 10 + ifnull(chat_capability, 0)) = (SELECT MAX (ifnull(mode,0)  * 10 + ifnull(chat_capability, 0)) FROM presence WHERE presence_contact_id=NEW.presence_contact_id) AND presence_contact_id=NEW.presence_contact_id; END");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("ContactsDatabaseHelper", "Bootstrapping database version: 911");
        this.mSyncState.createDatabase(db);
        db.execSQL("CREATE TABLE properties (property_key TEXT PRIMARY KEY, property_value TEXT );");
        setProperty(db, "database_time_created", String.valueOf(System.currentTimeMillis()));
        db.execSQL("CREATE TABLE accounts (_id INTEGER PRIMARY KEY AUTOINCREMENT,account_name TEXT, account_type TEXT, data_set TEXT);");
        db.execSQL("CREATE TABLE contacts (_id INTEGER PRIMARY KEY AUTOINCREMENT,name_raw_contact_id INTEGER REFERENCES raw_contacts(_id),photo_id INTEGER REFERENCES data(_id),photo_file_id INTEGER REFERENCES photo_files(_id),custom_ringtone TEXT,send_to_voicemail INTEGER NOT NULL DEFAULT 0,times_contacted INTEGER NOT NULL DEFAULT 0,last_time_contacted INTEGER,starred INTEGER NOT NULL DEFAULT 0,pinned INTEGER NOT NULL DEFAULT 0,has_phone_number INTEGER NOT NULL DEFAULT 0,lookup TEXT,status_update_id INTEGER REFERENCES data(_id),contact_last_updated_timestamp INTEGER);");
        ContactsTableUtil.createIndexes(db);
        DeletedContactsTableUtil.create(db);
        db.execSQL("CREATE TABLE raw_contacts (_id INTEGER PRIMARY KEY AUTOINCREMENT,account_id INTEGER REFERENCES accounts(_id),sourceid TEXT,raw_contact_is_read_only INTEGER NOT NULL DEFAULT 0,version INTEGER NOT NULL DEFAULT 1,dirty INTEGER NOT NULL DEFAULT 0,deleted INTEGER NOT NULL DEFAULT 0,contact_id INTEGER REFERENCES contacts(_id),aggregation_mode INTEGER NOT NULL DEFAULT 0,aggregation_needed INTEGER NOT NULL DEFAULT 1,custom_ringtone TEXT,send_to_voicemail INTEGER NOT NULL DEFAULT 0,times_contacted INTEGER NOT NULL DEFAULT 0,last_time_contacted INTEGER,starred INTEGER NOT NULL DEFAULT 0,pinned INTEGER NOT NULL DEFAULT 0,display_name TEXT,display_name_alt TEXT,display_name_source INTEGER NOT NULL DEFAULT 0,phonetic_name TEXT,phonetic_name_style TEXT,sort_key TEXT COLLATE PHONEBOOK,phonebook_label TEXT,phonebook_bucket INTEGER,sort_key_alt TEXT COLLATE PHONEBOOK,phonebook_label_alt TEXT,phonebook_bucket_alt INTEGER,name_verified INTEGER NOT NULL DEFAULT 0,sync1 TEXT, sync2 TEXT, sync3 TEXT, sync4 TEXT );");
        db.execSQL("CREATE INDEX raw_contacts_contact_id_index ON raw_contacts (contact_id);");
        db.execSQL("CREATE INDEX raw_contacts_source_id_account_id_index ON raw_contacts (sourceid, account_id);");
        db.execSQL("CREATE TABLE stream_items (_id INTEGER PRIMARY KEY AUTOINCREMENT, raw_contact_id INTEGER NOT NULL, res_package TEXT, icon TEXT, label TEXT, text TEXT, timestamp INTEGER NOT NULL, comments TEXT, stream_item_sync1 TEXT, stream_item_sync2 TEXT, stream_item_sync3 TEXT, stream_item_sync4 TEXT, FOREIGN KEY(raw_contact_id) REFERENCES raw_contacts(_id));");
        db.execSQL("CREATE TABLE stream_item_photos (_id INTEGER PRIMARY KEY AUTOINCREMENT, stream_item_id INTEGER NOT NULL, sort_index INTEGER, photo_file_id INTEGER NOT NULL, stream_item_photo_sync1 TEXT, stream_item_photo_sync2 TEXT, stream_item_photo_sync3 TEXT, stream_item_photo_sync4 TEXT, FOREIGN KEY(stream_item_id) REFERENCES stream_items(_id));");
        db.execSQL("CREATE TABLE photo_files (_id INTEGER PRIMARY KEY AUTOINCREMENT, height INTEGER NOT NULL, width INTEGER NOT NULL, filesize INTEGER NOT NULL);");
        db.execSQL("CREATE TABLE packages (_id INTEGER PRIMARY KEY AUTOINCREMENT,package TEXT NOT NULL);");
        db.execSQL("CREATE TABLE mimetypes (_id INTEGER PRIMARY KEY AUTOINCREMENT,mimetype TEXT NOT NULL);");
        db.execSQL("CREATE UNIQUE INDEX mime_type ON mimetypes (mimetype);");
        db.execSQL("CREATE TABLE data (_id INTEGER PRIMARY KEY AUTOINCREMENT,package_id INTEGER REFERENCES package(_id),mimetype_id INTEGER REFERENCES mimetype(_id) NOT NULL,raw_contact_id INTEGER REFERENCES raw_contacts(_id) NOT NULL,is_read_only INTEGER NOT NULL DEFAULT 0,is_primary INTEGER NOT NULL DEFAULT 0,is_super_primary INTEGER NOT NULL DEFAULT 0,data_version INTEGER NOT NULL DEFAULT 0,data1 TEXT,data2 TEXT,data3 TEXT,data4 TEXT,data5 TEXT,data6 TEXT,data7 TEXT,data8 TEXT,data9 TEXT,data10 TEXT,data11 TEXT,data12 TEXT,data13 TEXT,data14 TEXT,data15 TEXT,data_sync1 TEXT, data_sync2 TEXT, data_sync3 TEXT, data_sync4 TEXT );");
        db.execSQL("CREATE INDEX data_raw_contact_id ON data (raw_contact_id);");
        db.execSQL("CREATE INDEX data_mimetype_data1_index ON data (mimetype_id,data1);");
        db.execSQL("CREATE TABLE phone_lookup (data_id INTEGER REFERENCES data(_id) NOT NULL,raw_contact_id INTEGER REFERENCES raw_contacts(_id) NOT NULL,normalized_number TEXT NOT NULL,min_match TEXT NOT NULL);");
        db.execSQL("CREATE INDEX phone_lookup_index ON phone_lookup (normalized_number,raw_contact_id,data_id);");
        db.execSQL("CREATE INDEX phone_lookup_min_match_index ON phone_lookup (min_match,raw_contact_id,data_id);");
        db.execSQL("CREATE INDEX phone_lookup_data_id_min_match_index ON phone_lookup (data_id, min_match);");
        db.execSQL("CREATE TABLE name_lookup (data_id INTEGER REFERENCES data(_id) NOT NULL,raw_contact_id INTEGER REFERENCES raw_contacts(_id) NOT NULL,normalized_name TEXT NOT NULL,name_type INTEGER NOT NULL,PRIMARY KEY (data_id, normalized_name, name_type));");
        db.execSQL("CREATE INDEX name_lookup_raw_contact_id_index ON name_lookup (raw_contact_id);");
        db.execSQL("CREATE TABLE nickname_lookup (name TEXT,cluster TEXT);");
        db.execSQL("CREATE UNIQUE INDEX nickname_lookup_index ON nickname_lookup (name, cluster);");
        db.execSQL("CREATE TABLE groups (_id INTEGER PRIMARY KEY AUTOINCREMENT,package_id INTEGER REFERENCES package(_id),account_id INTEGER REFERENCES accounts(_id),sourceid TEXT,version INTEGER NOT NULL DEFAULT 1,dirty INTEGER NOT NULL DEFAULT 0,title TEXT,title_res INTEGER,notes TEXT,system_id TEXT,deleted INTEGER NOT NULL DEFAULT 0,group_visible INTEGER NOT NULL DEFAULT 0,should_sync INTEGER NOT NULL DEFAULT 1,auto_add INTEGER NOT NULL DEFAULT 0,favorites INTEGER NOT NULL DEFAULT 0,group_is_read_only INTEGER NOT NULL DEFAULT 0,sync1 TEXT, sync2 TEXT, sync3 TEXT, sync4 TEXT );");
        db.execSQL("CREATE INDEX groups_source_id_account_id_index ON groups (sourceid, account_id);");
        db.execSQL("CREATE TABLE IF NOT EXISTS agg_exceptions (_id INTEGER PRIMARY KEY AUTOINCREMENT,type INTEGER NOT NULL, raw_contact_id1 INTEGER REFERENCES raw_contacts(_id), raw_contact_id2 INTEGER REFERENCES raw_contacts(_id));");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS aggregation_exception_index1 ON agg_exceptions (raw_contact_id1, raw_contact_id2);");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS aggregation_exception_index2 ON agg_exceptions (raw_contact_id2, raw_contact_id1);");
        db.execSQL("CREATE TABLE IF NOT EXISTS settings (account_name STRING NOT NULL,account_type STRING NOT NULL,data_set STRING,ungrouped_visible INTEGER NOT NULL DEFAULT 0,should_sync INTEGER NOT NULL DEFAULT 1);");
        db.execSQL("CREATE TABLE visible_contacts (_id INTEGER PRIMARY KEY);");
        db.execSQL("CREATE TABLE default_directory (_id INTEGER PRIMARY KEY);");
        db.execSQL("CREATE TABLE calls (_id INTEGER PRIMARY KEY AUTOINCREMENT,number TEXT,presentation INTEGER NOT NULL DEFAULT 1,date INTEGER,duration INTEGER,data_usage INTEGER,type INTEGER,features INTEGER NOT NULL DEFAULT 0,subscription_component_name TEXT,subscription_id TEXT,sub_id INTEGER DEFAULT -1,new INTEGER,name TEXT,numbertype INTEGER,numberlabel TEXT,countryiso TEXT,voicemail_uri TEXT,is_read INTEGER,geocoded_location TEXT,lookup_uri TEXT,matched_number TEXT,normalized_number TEXT,photo_id INTEGER NOT NULL DEFAULT 0,formatted_number TEXT,_data TEXT,has_content INTEGER,mime_type TEXT,source_data TEXT,source_package TEXT,transcription TEXT,state INTEGER);");
        db.execSQL("CREATE TABLE voicemail_status (_id INTEGER PRIMARY KEY AUTOINCREMENT,source_package TEXT UNIQUE NOT NULL,settings_uri TEXT,voicemail_access_uri TEXT,configuration_state INTEGER,data_channel_state INTEGER,notification_channel_state INTEGER);");
        db.execSQL("CREATE TABLE status_updates (status_update_data_id INTEGER PRIMARY KEY REFERENCES data(_id),status TEXT,status_ts INTEGER,status_res_package TEXT, status_label INTEGER, status_icon INTEGER);");
        createDirectoriesTable(db);
        createSearchIndexTable(db, false);
        db.execSQL("CREATE TABLE data_usage_stat(stat_id INTEGER PRIMARY KEY AUTOINCREMENT, data_id INTEGER NOT NULL, usage_type INTEGER NOT NULL DEFAULT 0, times_used INTEGER NOT NULL DEFAULT 0, last_time_used INTERGER NOT NULL DEFAULT 0, FOREIGN KEY(data_id) REFERENCES data(_id));");
        db.execSQL("CREATE UNIQUE INDEX data_usage_stat_index ON data_usage_stat (data_id, usage_type);");
        createContactsViews(db);
        createGroupsView(db);
        createContactsTriggers(db);
        createContactsIndexes(db, false);
        loadNicknameLookupTable(db);
        initializeAutoIncrementSequences(db);
        LegacyApiSupport.createDatabase(db);
        if (this.mDatabaseOptimizationEnabled) {
            db.execSQL("ANALYZE;");
            updateSqliteStats(db);
        }
        ContentResolver.requestSync(null, "com.android.contacts", new Bundle());
        if (dbForProfile() == 0) {
            Intent dbCreatedIntent = new Intent("android.provider.Contacts.DATABASE_CREATED");
            dbCreatedIntent.addFlags(67108864);
            this.mContext.sendBroadcast(dbCreatedIntent, "android.permission.READ_CONTACTS");
        }
    }

    protected void initializeAutoIncrementSequences(SQLiteDatabase db) {
    }

    private void createDirectoriesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE directories(_id INTEGER PRIMARY KEY AUTOINCREMENT,packageName TEXT NOT NULL,authority TEXT NOT NULL,typeResourceId INTEGER,typeResourceName TEXT,accountType TEXT,accountName TEXT,displayName TEXT, exportSupport INTEGER NOT NULL DEFAULT 0,shortcutSupport INTEGER NOT NULL DEFAULT 0,photoSupport INTEGER NOT NULL DEFAULT 0);");
        setProperty(db, "directoryScanComplete", "0");
    }

    public void createSearchIndexTable(SQLiteDatabase db, boolean rebuildSqliteStats) {
        db.execSQL("DROP TABLE IF EXISTS search_index");
        db.execSQL("CREATE VIRTUAL TABLE search_index USING FTS4 (contact_id INTEGER REFERENCES contacts(_id) NOT NULL,content TEXT, name TEXT, tokens TEXT)");
        if (rebuildSqliteStats) {
            updateSqliteStats(db);
        }
    }

    private void createContactsTriggers(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS raw_contacts_deleted;");
        db.execSQL("CREATE TRIGGER raw_contacts_deleted    BEFORE DELETE ON raw_contacts BEGIN    DELETE FROM data     WHERE raw_contact_id=OLD._id;   DELETE FROM agg_exceptions     WHERE raw_contact_id1=OLD._id        OR raw_contact_id2=OLD._id;   DELETE FROM visible_contacts     WHERE _id=OLD.contact_id       AND (SELECT COUNT(*) FROM raw_contacts            WHERE contact_id=OLD.contact_id           )=1;   DELETE FROM default_directory     WHERE _id=OLD.contact_id       AND (SELECT COUNT(*) FROM raw_contacts            WHERE contact_id=OLD.contact_id           )=1;   DELETE FROM contacts     WHERE _id=OLD.contact_id       AND (SELECT COUNT(*) FROM raw_contacts            WHERE contact_id=OLD.contact_id           )=1; END");
        db.execSQL("DROP TRIGGER IF EXISTS contacts_times_contacted;");
        db.execSQL("DROP TRIGGER IF EXISTS raw_contacts_times_contacted;");
        db.execSQL("DROP TRIGGER IF EXISTS raw_contacts_marked_deleted;");
        db.execSQL("CREATE TRIGGER raw_contacts_marked_deleted    AFTER UPDATE ON raw_contacts BEGIN    UPDATE raw_contacts     SET version=OLD.version+1      WHERE _id=OLD._id       AND NEW.deleted!= OLD.deleted; END");
        db.execSQL("DROP TRIGGER IF EXISTS data_updated;");
        db.execSQL("CREATE TRIGGER data_updated AFTER UPDATE ON data BEGIN    UPDATE data     SET data_version=OLD.data_version+1      WHERE _id=OLD._id;   UPDATE raw_contacts     SET version=version+1      WHERE _id=OLD.raw_contact_id; END");
        db.execSQL("DROP TRIGGER IF EXISTS data_deleted;");
        db.execSQL("CREATE TRIGGER data_deleted BEFORE DELETE ON data BEGIN    UPDATE raw_contacts     SET version=version+1      WHERE _id=OLD.raw_contact_id;   DELETE FROM phone_lookup     WHERE data_id=OLD._id;   DELETE FROM status_updates     WHERE status_update_data_id=OLD._id;   DELETE FROM name_lookup     WHERE data_id=OLD._id; END");
        db.execSQL("DROP TRIGGER IF EXISTS groups_updated1;");
        db.execSQL("CREATE TRIGGER groups_updated1    AFTER UPDATE ON groups BEGIN    UPDATE groups     SET version=OLD.version+1     WHERE _id=OLD._id; END");
        db.execSQL("DROP TRIGGER IF EXISTS groups_auto_add_updated1;");
        db.execSQL("CREATE TRIGGER groups_auto_add_updated1    AFTER UPDATE OF auto_add ON groups BEGIN    DELETE FROM default_directory; INSERT OR IGNORE INTO default_directory     SELECT contact_id     FROM raw_contacts     WHERE raw_contacts.account_id=(SELECT _id FROM accounts WHERE account_name IS NULL AND account_type IS NULL AND data_set IS NULL); INSERT OR IGNORE INTO default_directory     SELECT contact_id         FROM raw_contacts     WHERE NOT EXISTS         (SELECT _id             FROM groups             WHERE raw_contacts.account_id = groups.account_id             AND auto_add != 0); INSERT OR IGNORE INTO default_directory     SELECT contact_id         FROM raw_contacts     JOIN data           ON (raw_contacts._id=raw_contact_id)     WHERE mimetype_id=(SELECT _id FROM mimetypes WHERE mimetype='vnd.android.cursor.item/group_membership')     AND EXISTS         (SELECT _id             FROM groups                 WHERE raw_contacts.account_id = groups.account_id                 AND auto_add != 0); END");
    }

    private void createContactsIndexes(SQLiteDatabase db, boolean rebuildSqliteStats) {
        db.execSQL("DROP INDEX IF EXISTS name_lookup_index");
        db.execSQL("CREATE INDEX name_lookup_index ON name_lookup (normalized_name,name_type, raw_contact_id, data_id);");
        db.execSQL("DROP INDEX IF EXISTS raw_contact_sort_key1_index");
        db.execSQL("CREATE INDEX raw_contact_sort_key1_index ON raw_contacts (sort_key);");
        db.execSQL("DROP INDEX IF EXISTS raw_contact_sort_key2_index");
        db.execSQL("CREATE INDEX raw_contact_sort_key2_index ON raw_contacts (sort_key_alt);");
        if (rebuildSqliteStats) {
            updateSqliteStats(db);
        }
    }

    private void createContactsViews(SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS view_contacts;");
        db.execSQL("DROP VIEW IF EXISTS view_data;");
        db.execSQL("DROP VIEW IF EXISTS view_raw_contacts;");
        db.execSQL("DROP VIEW IF EXISTS view_raw_entities;");
        db.execSQL("DROP VIEW IF EXISTS view_entities;");
        db.execSQL("DROP VIEW IF EXISTS view_data_usage_stat;");
        db.execSQL("DROP VIEW IF EXISTS view_stream_items;");
        String dataSelect = "SELECT data._id AS _id,raw_contact_id, raw_contacts.contact_id AS contact_id, raw_contacts.account_id,accounts.account_name AS account_name,accounts.account_type AS account_type,accounts.data_set AS data_set,(CASE WHEN accounts.data_set IS NULL THEN accounts.account_type ELSE accounts.account_type||'/'||accounts.data_set END) AS account_type_and_data_set,raw_contacts.sourceid AS sourceid,raw_contacts.name_verified AS name_verified,raw_contacts.version AS version,raw_contacts.dirty AS dirty,raw_contacts.sync1 AS sync1,raw_contacts.sync2 AS sync2,raw_contacts.sync3 AS sync3,raw_contacts.sync4 AS sync4, is_primary, is_super_primary, data_version, data.package_id,package AS res_package,data.mimetype_id,mimetype AS mimetype, is_read_only, data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, data12, data13, data14, data15, data_sync1, data_sync2, data_sync3, data_sync4, contacts.custom_ringtone AS custom_ringtone,contacts.send_to_voicemail AS send_to_voicemail,contacts.last_time_contacted AS last_time_contacted,contacts.times_contacted AS times_contacted,contacts.starred AS starred,contacts.pinned AS pinned, name_raw_contact.display_name_source AS display_name_source, name_raw_contact.display_name AS display_name, name_raw_contact.display_name_alt AS display_name_alt, name_raw_contact.phonetic_name AS phonetic_name, name_raw_contact.phonetic_name_style AS phonetic_name_style, name_raw_contact.sort_key AS sort_key, name_raw_contact.phonebook_label AS phonebook_label, name_raw_contact.phonebook_bucket AS phonebook_bucket, name_raw_contact.sort_key_alt AS sort_key_alt, name_raw_contact.phonebook_label_alt AS phonebook_label_alt, name_raw_contact.phonebook_bucket_alt AS phonebook_bucket_alt, has_phone_number, name_raw_contact_id, lookup, photo_id, photo_file_id, CAST(EXISTS (SELECT _id FROM visible_contacts WHERE contacts._id=visible_contacts._id) AS INTEGER) AS in_visible_group, CAST(EXISTS (SELECT _id FROM default_directory WHERE contacts._id=default_directory._id) AS INTEGER) AS in_default_directory, status_update_id, contacts.contact_last_updated_timestamp, " + buildDisplayPhotoUriAlias("raw_contacts.contact_id", "photo_uri") + ", " + buildThumbnailPhotoUriAlias("raw_contacts.contact_id", "photo_thumb_uri") + ", " + dbForProfile() + " AS raw_contact_is_user_profile, groups.sourceid AS group_sourceid FROM data JOIN mimetypes ON (data.mimetype_id=mimetypes._id) JOIN raw_contacts ON (data.raw_contact_id=raw_contacts._id) JOIN accounts ON (raw_contacts.account_id=accounts._id) JOIN contacts ON (raw_contacts.contact_id=contacts._id) JOIN raw_contacts AS name_raw_contact ON(name_raw_contact_id=name_raw_contact._id) LEFT OUTER JOIN packages ON (data.package_id=packages._id) LEFT OUTER JOIN groups ON (mimetypes.mimetype='vnd.android.cursor.item/group_membership' AND groups._id=data.data1)";
        db.execSQL("CREATE VIEW view_data AS " + dataSelect);
        String rawContactsSelect = "SELECT raw_contacts._id AS _id,contact_id, aggregation_mode, raw_contact_is_read_only, deleted, display_name_source, display_name, display_name_alt, phonetic_name, phonetic_name_style, sort_key, phonebook_label, phonebook_bucket, sort_key_alt, phonebook_label_alt, phonebook_bucket_alt, " + dbForProfile() + " AS raw_contact_is_user_profile, custom_ringtone,send_to_voicemail,last_time_contacted,times_contacted,starred,pinned, raw_contacts.account_id,accounts.account_name AS account_name,accounts.account_type AS account_type,accounts.data_set AS data_set,(CASE WHEN accounts.data_set IS NULL THEN accounts.account_type ELSE accounts.account_type||'/'||accounts.data_set END) AS account_type_and_data_set,raw_contacts.sourceid AS sourceid,raw_contacts.name_verified AS name_verified,raw_contacts.version AS version,raw_contacts.dirty AS dirty,raw_contacts.sync1 AS sync1,raw_contacts.sync2 AS sync2,raw_contacts.sync3 AS sync3,raw_contacts.sync4 AS sync4 FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id)";
        db.execSQL("CREATE VIEW view_raw_contacts AS " + rawContactsSelect);
        String contactsColumns = "contacts.custom_ringtone AS custom_ringtone, name_raw_contact.display_name_source AS display_name_source, name_raw_contact.display_name AS display_name, name_raw_contact.display_name_alt AS display_name_alt, name_raw_contact.phonetic_name AS phonetic_name, name_raw_contact.phonetic_name_style AS phonetic_name_style, name_raw_contact.sort_key AS sort_key, name_raw_contact.phonebook_label AS phonebook_label, name_raw_contact.phonebook_bucket AS phonebook_bucket, name_raw_contact.sort_key_alt AS sort_key_alt, name_raw_contact.phonebook_label_alt AS phonebook_label_alt, name_raw_contact.phonebook_bucket_alt AS phonebook_bucket_alt, has_phone_number, name_raw_contact_id, lookup, photo_id, photo_file_id, CAST(EXISTS (SELECT _id FROM visible_contacts WHERE contacts._id=visible_contacts._id) AS INTEGER) AS in_visible_group, CAST(EXISTS (SELECT _id FROM default_directory WHERE contacts._id=default_directory._id) AS INTEGER) AS in_default_directory, status_update_id, contacts.contact_last_updated_timestamp, contacts.last_time_contacted AS last_time_contacted, contacts.send_to_voicemail AS send_to_voicemail, contacts.starred AS starred, contacts.pinned AS pinned, contacts.times_contacted AS times_contacted";
        String contactsSelect = "SELECT contacts._id AS _id," + contactsColumns + ", " + buildDisplayPhotoUriAlias("contacts._id", "photo_uri") + ", " + buildThumbnailPhotoUriAlias("contacts._id", "photo_thumb_uri") + ", " + dbForProfile() + " AS is_user_profile,accounts.account_type AS account_type FROM contacts JOIN raw_contacts AS name_raw_contact ON(name_raw_contact_id=name_raw_contact._id) JOIN accounts ON (name_raw_contact.account_id=accounts._id)";
        db.execSQL("CREATE VIEW view_contacts AS " + contactsSelect);
        String rawEntitiesSelect = "SELECT contact_id, raw_contacts.deleted AS deleted,is_primary, is_super_primary, data_version, data.package_id,package AS res_package,data.mimetype_id,mimetype AS mimetype, is_read_only, data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, data12, data13, data14, data15, data_sync1, data_sync2, data_sync3, data_sync4, raw_contacts.account_id,accounts.account_name AS account_name,accounts.account_type AS account_type,accounts.data_set AS data_set,(CASE WHEN accounts.data_set IS NULL THEN accounts.account_type ELSE accounts.account_type||'/'||accounts.data_set END) AS account_type_and_data_set,raw_contacts.sourceid AS sourceid,raw_contacts.name_verified AS name_verified,raw_contacts.version AS version,raw_contacts.dirty AS dirty,raw_contacts.sync1 AS sync1,raw_contacts.sync2 AS sync2,raw_contacts.sync3 AS sync3,raw_contacts.sync4 AS sync4, data_sync1, data_sync2, data_sync3, data_sync4, raw_contacts._id AS _id, data._id AS data_id,raw_contacts.starred AS starred," + dbForProfile() + " AS raw_contact_is_user_profile,groups.sourceid AS group_sourceid FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id) LEFT OUTER JOIN data ON (data.raw_contact_id=raw_contacts._id) LEFT OUTER JOIN packages ON (data.package_id=packages._id) LEFT OUTER JOIN mimetypes ON (data.mimetype_id=mimetypes._id) LEFT OUTER JOIN groups ON (mimetypes.mimetype='vnd.android.cursor.item/group_membership' AND groups._id=data.data1)";
        db.execSQL("CREATE VIEW view_raw_entities AS " + rawEntitiesSelect);
        String entitiesSelect = "SELECT raw_contacts.contact_id AS _id, raw_contacts.contact_id AS contact_id, raw_contacts.deleted AS deleted,is_primary, is_super_primary, data_version, data.package_id,package AS res_package,data.mimetype_id,mimetype AS mimetype, is_read_only, data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, data12, data13, data14, data15, data_sync1, data_sync2, data_sync3, data_sync4, raw_contacts.account_id,accounts.account_name AS account_name,accounts.account_type AS account_type,accounts.data_set AS data_set,(CASE WHEN accounts.data_set IS NULL THEN accounts.account_type ELSE accounts.account_type||'/'||accounts.data_set END) AS account_type_and_data_set,raw_contacts.sourceid AS sourceid,raw_contacts.name_verified AS name_verified,raw_contacts.version AS version,raw_contacts.dirty AS dirty,raw_contacts.sync1 AS sync1,raw_contacts.sync2 AS sync2,raw_contacts.sync3 AS sync3,raw_contacts.sync4 AS sync4, " + contactsColumns + ", " + buildDisplayPhotoUriAlias("raw_contacts.contact_id", "photo_uri") + ", " + buildThumbnailPhotoUriAlias("raw_contacts.contact_id", "photo_thumb_uri") + ", " + dbForProfile() + " AS is_user_profile, data_sync1, data_sync2, data_sync3, data_sync4, raw_contacts._id AS raw_contact_id, data._id AS data_id,groups.sourceid AS group_sourceid FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id) JOIN contacts ON (raw_contacts.contact_id=contacts._id) JOIN raw_contacts AS name_raw_contact ON(name_raw_contact_id=name_raw_contact._id) LEFT OUTER JOIN data ON (data.raw_contact_id=raw_contacts._id) LEFT OUTER JOIN packages ON (data.package_id=packages._id) LEFT OUTER JOIN mimetypes ON (data.mimetype_id=mimetypes._id) LEFT OUTER JOIN groups ON (mimetypes.mimetype='vnd.android.cursor.item/group_membership' AND groups._id=data.data1)";
        db.execSQL("CREATE VIEW view_entities AS " + entitiesSelect);
        db.execSQL("CREATE VIEW view_data_usage_stat AS SELECT data_usage_stat.stat_id AS stat_id, data_id, raw_contacts.contact_id AS contact_id, mimetypes.mimetype AS mimetype, usage_type, times_used, last_time_used FROM data_usage_stat JOIN data ON (data._id=data_usage_stat.data_id) JOIN raw_contacts ON (raw_contacts._id=data.raw_contact_id ) JOIN mimetypes ON (mimetypes._id=data.mimetype_id)");
        db.execSQL("CREATE VIEW view_stream_items AS SELECT stream_items._id, contacts._id AS contact_id, contacts.lookup AS contact_lookup, accounts.account_name, accounts.account_type, accounts.data_set, stream_items.raw_contact_id as raw_contact_id, raw_contacts.sourceid as raw_contact_source_id, stream_items.res_package, stream_items.icon, stream_items.label, stream_items.text, stream_items.timestamp, stream_items.comments, stream_items.stream_item_sync1, stream_items.stream_item_sync2, stream_items.stream_item_sync3, stream_items.stream_item_sync4 FROM stream_items JOIN raw_contacts ON (stream_items.raw_contact_id=raw_contacts._id) JOIN accounts ON (raw_contacts.account_id=accounts._id) JOIN contacts ON (raw_contacts.contact_id=contacts._id)");
    }

    private static String buildDisplayPhotoUriAlias(String contactIdColumn, String alias) {
        return "(CASE WHEN photo_file_id IS NULL THEN (CASE WHEN photo_id IS NULL OR photo_id=0 THEN NULL ELSE '" + ContactsContract.Contacts.CONTENT_URI + "/'||" + contactIdColumn + "|| '/photo' END) ELSE '" + ContactsContract.DisplayPhoto.CONTENT_URI + "/'||photo_file_id END) AS " + alias;
    }

    private static String buildThumbnailPhotoUriAlias(String contactIdColumn, String alias) {
        return "(CASE WHEN photo_id IS NULL OR photo_id=0 THEN NULL ELSE '" + ContactsContract.Contacts.CONTENT_URI + "/'||" + contactIdColumn + "|| '/photo' END) AS " + alias;
    }

    protected int dbForProfile() {
        return 0;
    }

    private void createGroupsView(SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS view_groups;");
        String groupsSelect = "SELECT groups._id AS _id,groups.account_id AS account_id,accounts.account_name AS account_name,accounts.account_type AS account_type,accounts.data_set AS data_set,(CASE WHEN accounts.data_set IS NULL THEN accounts.account_type ELSE accounts.account_type||'/'||accounts.data_set END) AS account_type_and_data_set,sourceid,version,dirty,title,title_res,notes,system_id,deleted,group_visible,should_sync,auto_add,favorites,group_is_read_only,sync1,sync2,sync3,sync4,package AS res_package FROM groups JOIN accounts ON (groups.account_id=accounts._id) LEFT OUTER JOIN packages ON (groups.package_id=packages._id)";
        db.execSQL("CREATE VIEW view_groups AS " + groupsSelect);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("ContactsDatabaseHelper", "ContactsProvider cannot proceed because downgrading your database is not supported. To continue, please either re-upgrade to your previous Android version, or clear all application data in Contacts Storage (this will result in the loss of all local contacts that are not synced). To avoid data loss, your contacts database will not be wiped automatically.");
        super.onDowngrade(db, oldVersion, newVersion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 99) {
            Log.i("ContactsDatabaseHelper", "Upgrading from version " + oldVersion + " to " + newVersion + ", data will be lost!");
            db.execSQL("DROP TABLE IF EXISTS contacts;");
            db.execSQL("DROP TABLE IF EXISTS raw_contacts;");
            db.execSQL("DROP TABLE IF EXISTS packages;");
            db.execSQL("DROP TABLE IF EXISTS mimetypes;");
            db.execSQL("DROP TABLE IF EXISTS data;");
            db.execSQL("DROP TABLE IF EXISTS phone_lookup;");
            db.execSQL("DROP TABLE IF EXISTS name_lookup;");
            db.execSQL("DROP TABLE IF EXISTS nickname_lookup;");
            db.execSQL("DROP TABLE IF EXISTS groups;");
            db.execSQL("DROP TABLE IF EXISTS activities;");
            db.execSQL("DROP TABLE IF EXISTS calls;");
            db.execSQL("DROP TABLE IF EXISTS settings;");
            db.execSQL("DROP TABLE IF EXISTS status_updates;");
            db.execSQL("DROP TABLE IF EXISTS agg_exceptions;");
            onCreate(db);
            return;
        }
        Log.i("ContactsDatabaseHelper", "Upgrading from version " + oldVersion + " to " + newVersion);
        boolean upgradeViewsAndTriggers = false;
        boolean upgradeNameLookup = false;
        boolean upgradeLegacyApiSupport = false;
        boolean upgradeSearchIndex = false;
        boolean rescanDirectories = false;
        boolean rebuildSqliteStats = false;
        boolean upgradeLocaleSpecificData = false;
        if (oldVersion == 99) {
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }
        if (oldVersion == 100) {
            db.execSQL("CREATE INDEX IF NOT EXISTS mimetypes_mimetype_index ON mimetypes (mimetype,_id);");
            updateIndexStats(db, "mimetypes", "mimetypes_mimetype_index", "50 1 1");
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }
        if (oldVersion == 101) {
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }
        if (oldVersion == 102) {
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }
        if (oldVersion == 103) {
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }
        if (oldVersion == 104 || oldVersion == 201) {
            LegacyApiSupport.createSettingsTable(db);
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }
        if (oldVersion == 105) {
            upgradeToVersion202(db);
            upgradeNameLookup = true;
            oldVersion = 202;
        }
        if (oldVersion == 202) {
            upgradeToVersion203(db);
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }
        if (oldVersion == 203) {
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }
        if (oldVersion == 204) {
            upgradeToVersion205(db);
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }
        if (oldVersion == 205) {
            upgrateToVersion206(db);
            upgradeViewsAndTriggers = true;
            oldVersion++;
        }
        if (oldVersion == 206) {
            oldVersion = 300;
        }
        if (oldVersion == 300) {
            upgradeViewsAndTriggers = true;
            oldVersion = 301;
        }
        if (oldVersion == 301) {
            upgradeViewsAndTriggers = true;
            oldVersion = 302;
        }
        if (oldVersion == 302) {
            upgradeEmailToVersion303(db);
            upgradeNicknameToVersion303(db);
            oldVersion = 303;
        }
        if (oldVersion == 303) {
            upgradeToVersion304(db);
            oldVersion = 304;
        }
        if (oldVersion == 304) {
            upgradeNameLookup = true;
            oldVersion = 305;
        }
        if (oldVersion == 305) {
            upgradeToVersion306(db);
            oldVersion = 306;
        }
        if (oldVersion == 306) {
            upgradeToVersion307(db);
            oldVersion = 307;
        }
        if (oldVersion == 307) {
            upgradeToVersion308(db);
            oldVersion = 308;
        }
        if (oldVersion < 350) {
            upgradeViewsAndTriggers = true;
            oldVersion = 351;
        }
        if (oldVersion == 351) {
            upgradeNameLookup = true;
            oldVersion = 352;
        }
        if (oldVersion == 352) {
            upgradeToVersion353(db);
            oldVersion = 353;
        }
        if (oldVersion < 400) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion400(db);
            oldVersion = 400;
        }
        if (oldVersion == 400) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion401(db);
            oldVersion = 401;
        }
        if (oldVersion == 401) {
            upgradeToVersion402(db);
            oldVersion = 402;
        }
        if (oldVersion == 402) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion403(db);
            oldVersion = 403;
        }
        if (oldVersion == 403) {
            upgradeViewsAndTriggers = true;
            oldVersion = 404;
        }
        if (oldVersion == 404) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion405(db);
            oldVersion = 405;
        }
        if (oldVersion == 405) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion406(db);
            oldVersion = 406;
        }
        if (oldVersion == 406) {
            upgradeViewsAndTriggers = true;
            oldVersion = 407;
        }
        if (oldVersion == 407) {
            oldVersion = 408;
        }
        if (oldVersion == 408) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion409(db);
            oldVersion = 409;
        }
        if (oldVersion == 409) {
            upgradeViewsAndTriggers = true;
            oldVersion = 410;
        }
        if (oldVersion == 410) {
            upgradeToVersion411(db);
            oldVersion = 411;
        }
        if (oldVersion == 411) {
            upgradeToVersion353(db);
            oldVersion = 412;
        }
        if (oldVersion == 412) {
            upgradeToVersion413(db);
            oldVersion = 413;
        }
        if (oldVersion == 413) {
            upgradeNameLookup = true;
            oldVersion = 414;
        }
        if (oldVersion == 414) {
            upgradeToVersion415(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 415;
        }
        if (oldVersion == 415) {
            upgradeToVersion416(db);
            oldVersion = 416;
        }
        if (oldVersion == 416) {
            upgradeLegacyApiSupport = true;
            oldVersion = 417;
        }
        if (oldVersion < 500) {
            upgradeSearchIndex = true;
        }
        if (oldVersion < 501) {
            upgradeSearchIndex = true;
            upgradeToVersion501(db);
            oldVersion = 501;
        }
        if (oldVersion < 502) {
            upgradeSearchIndex = true;
            upgradeToVersion502(db);
            oldVersion = 502;
        }
        if (oldVersion < 503) {
            upgradeSearchIndex = true;
            oldVersion = 503;
        }
        if (oldVersion < 504) {
            upgradeToVersion504(db);
            oldVersion = 504;
        }
        if (oldVersion < 600) {
            upgradeViewsAndTriggers = true;
            oldVersion = 600;
        }
        if (oldVersion < 601) {
            upgradeToVersion601(db);
            oldVersion = 601;
        }
        if (oldVersion < 602) {
            upgradeToVersion602(db);
            oldVersion = 602;
        }
        if (oldVersion < 603) {
            upgradeViewsAndTriggers = true;
            oldVersion = 603;
        }
        if (oldVersion < 604) {
            upgradeToVersion604(db);
            oldVersion = 604;
        }
        if (oldVersion < 605) {
            upgradeViewsAndTriggers = true;
            oldVersion = 605;
        }
        if (oldVersion < 606) {
            upgradeViewsAndTriggers = true;
            upgradeLegacyApiSupport = true;
            upgradeToVersion606(db);
            oldVersion = 606;
        }
        if (oldVersion < 607) {
            upgradeViewsAndTriggers = true;
            oldVersion = 607;
        }
        if (oldVersion < 608) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion608(db);
            oldVersion = 608;
        }
        if (oldVersion < 609) {
            oldVersion = 609;
        }
        if (oldVersion < 610) {
            upgradeToVersion610(db);
            oldVersion = 610;
        }
        if (oldVersion < 611) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion611(db);
            oldVersion = 611;
        }
        if (oldVersion < 612) {
            upgradeViewsAndTriggers = true;
            upgradeToVersion612(db);
            oldVersion = 612;
        }
        if (oldVersion < 613) {
            upgradeToVersion613(db);
            oldVersion = 613;
        }
        if (oldVersion < 614) {
            upgradeViewsAndTriggers = true;
            oldVersion = 614;
        }
        if (oldVersion < 615) {
            upgradeToVersion615(db);
            oldVersion = 615;
        }
        if (oldVersion < 616) {
            upgradeViewsAndTriggers = true;
            oldVersion = 616;
        }
        if (oldVersion < 617) {
            upgradeViewsAndTriggers = true;
            oldVersion = 617;
        }
        if (oldVersion < 618) {
            upgradeToVersion618(db);
            oldVersion = 618;
        }
        if (oldVersion < 619) {
            upgradeViewsAndTriggers = true;
            oldVersion = 619;
        }
        if (oldVersion < 620) {
            upgradeViewsAndTriggers = true;
            oldVersion = 620;
        }
        if (oldVersion < 621) {
            upgradeSearchIndex = true;
            oldVersion = 621;
        }
        if (oldVersion < 622) {
            upgradeToVersion622(db);
            oldVersion = 622;
        }
        if (oldVersion < 623) {
            upgradeSearchIndex = true;
            oldVersion = 623;
        }
        if (oldVersion < 624) {
            upgradeViewsAndTriggers = true;
            oldVersion = 624;
        }
        if (oldVersion < 625) {
            upgradeSearchIndex = true;
            oldVersion = 625;
        }
        if (oldVersion < 626) {
            upgradeToVersion626(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 626;
        }
        if (oldVersion < 700) {
            rescanDirectories = true;
            oldVersion = 700;
        }
        if (oldVersion < 701) {
            upgradeToVersion701(db);
            oldVersion = 701;
        }
        if (oldVersion < 702) {
            upgradeToVersion702(db);
            oldVersion = 702;
        }
        if (oldVersion < 703) {
            upgradeSearchIndex = true;
            oldVersion = 703;
        }
        if (oldVersion < 704) {
            db.execSQL("DROP TABLE IF EXISTS activities;");
            oldVersion = 704;
        }
        if (oldVersion < 705) {
            upgradeSearchIndex = true;
            oldVersion = 705;
        }
        if (oldVersion < 706) {
            rebuildSqliteStats = true;
            oldVersion = 706;
        }
        if (oldVersion < 707) {
            upgradeToVersion707(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 707;
        }
        if (oldVersion < 708) {
            upgradeLocaleSpecificData = true;
            oldVersion = 708;
        }
        if (oldVersion < 709) {
            upgradeLocaleSpecificData = true;
            oldVersion = 709;
        }
        if (oldVersion < 710) {
            upgradeToVersion710(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 710;
        }
        if (oldVersion < 800) {
            upgradeToVersion800(db);
            oldVersion = 800;
        }
        if (oldVersion < 801) {
            setProperty(db, "database_time_created", String.valueOf(System.currentTimeMillis()));
            oldVersion = 801;
        }
        if (oldVersion < 802) {
            upgradeToVersion802(db);
            upgradeViewsAndTriggers = true;
            oldVersion = 802;
        }
        if (oldVersion < 803) {
            upgradeSearchIndex = true;
            oldVersion = 803;
        }
        if (oldVersion < 804) {
            oldVersion = 804;
        }
        if (oldVersion < 900) {
            upgradeViewsAndTriggers = true;
            oldVersion = 900;
        }
        if (oldVersion < 901) {
            upgradeSearchIndex = true;
            oldVersion = 901;
        }
        if (oldVersion < 902) {
            upgradeToVersion902(db);
            oldVersion = 902;
        }
        if (oldVersion < 903) {
            upgradeToVersion903(db);
            oldVersion = 903;
        }
        if (oldVersion < 904) {
            upgradeToVersion904(db);
            oldVersion = 904;
        }
        if (oldVersion < 905) {
            upgradeToVersion905(db);
            oldVersion = 905;
        }
        if (oldVersion < 906) {
            upgradeToVersion906(db);
            oldVersion = 906;
        }
        if (oldVersion < 907) {
            upgradeNameLookup = true;
            oldVersion = 907;
        }
        if (oldVersion < 908) {
            upgradeToVersion908(db);
            oldVersion = 908;
        }
        if (oldVersion < 909) {
            upgradeToVersion909(db);
            oldVersion = 909;
        }
        if (oldVersion < 910) {
            upgradeToVersion910(db);
            oldVersion = 910;
        }
        if (oldVersion < 911) {
            upgradeViewsAndTriggers = true;
            oldVersion = 911;
        }
        if (upgradeViewsAndTriggers) {
            createContactsViews(db);
            createGroupsView(db);
            createContactsTriggers(db);
            createContactsIndexes(db, false);
            upgradeLegacyApiSupport = true;
            rebuildSqliteStats = true;
        }
        if (upgradeLegacyApiSupport) {
            LegacyApiSupport.createViews(db);
        }
        if (upgradeLocaleSpecificData) {
            upgradeLocaleData(db, false);
            upgradeNameLookup = false;
            upgradeSearchIndex = true;
            rebuildSqliteStats = true;
        }
        if (upgradeNameLookup) {
            rebuildNameLookup(db, false);
            rebuildSqliteStats = true;
        }
        if (upgradeSearchIndex) {
            rebuildSearchIndex(db, false);
            rebuildSqliteStats = true;
        }
        if (rescanDirectories) {
            setProperty(db, "directoryScanComplete", "0");
        }
        if (rebuildSqliteStats) {
            updateSqliteStats(db);
        }
        if (oldVersion != newVersion) {
            throw new IllegalStateException("error upgrading the database to version " + newVersion);
        }
    }

    private void upgradeToVersion202(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE phone_lookup ADD min_match TEXT;");
        db.execSQL("CREATE INDEX phone_lookup_min_match_index ON phone_lookup (min_match,raw_contact_id,data_id);");
        updateIndexStats(db, "phone_lookup", "phone_lookup_min_match_index", "10000 2 2 1");
        SQLiteStatement update = db.compileStatement("UPDATE phone_lookup SET min_match=? WHERE data_id=?");
        Cursor c = db.query("phone_lookup JOIN data ON (data_id=data._id)", new String[]{"_id", "data1"}, null, null, null, null, null);
        while (c.moveToNext()) {
            try {
                long dataId = c.getLong(0);
                String number = c.getString(1);
                if (!TextUtils.isEmpty(number)) {
                    update.bindString(1, PhoneNumberUtils.toCallerIDMinMatch(number));
                    update.bindLong(2, dataId);
                    update.execute();
                }
            } finally {
                c.close();
            }
        }
    }

    private void upgradeToVersion203(SQLiteDatabase db) {
        db.execSQL("DELETE FROM raw_contacts WHERE contact_id NOT NULL AND contact_id NOT IN (SELECT _id FROM contacts)");
        db.execSQL("ALTER TABLE contacts ADD name_raw_contact_id INTEGER REFERENCES raw_contacts(_id)");
        db.execSQL("ALTER TABLE raw_contacts ADD contact_in_visible_group INTEGER NOT NULL DEFAULT 0");
        db.execSQL("UPDATE contacts SET name_raw_contact_id=( SELECT _id FROM raw_contacts WHERE contact_id=contacts._id AND raw_contacts.display_name=contacts.display_name ORDER BY _id LIMIT 1)");
        db.execSQL("CREATE INDEX contacts_name_raw_contact_id_index ON contacts (name_raw_contact_id);");
        db.execSQL("UPDATE contacts SET name_raw_contact_id=( SELECT _id FROM raw_contacts WHERE contact_id=contacts._id ORDER BY _id LIMIT 1) WHERE name_raw_contact_id IS NULL");
        db.execSQL("UPDATE contacts SET display_name=NULL");
        db.execSQL("UPDATE raw_contacts SET contact_in_visible_group=(SELECT in_visible_group FROM contacts WHERE _id=contact_id) WHERE contact_id NOT NULL");
        db.execSQL("CREATE INDEX raw_contact_sort_key1_index ON raw_contacts (contact_in_visible_group,display_name COLLATE LOCALIZED ASC);");
        db.execSQL("DROP INDEX contacts_visible_index");
        db.execSQL("CREATE INDEX contacts_visible_index ON contacts (in_visible_group);");
    }

    private void upgradeToVersion205(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE raw_contacts ADD display_name_alt TEXT;");
        db.execSQL("ALTER TABLE raw_contacts ADD phonetic_name TEXT;");
        db.execSQL("ALTER TABLE raw_contacts ADD phonetic_name_style INTEGER;");
        db.execSQL("ALTER TABLE raw_contacts ADD sort_key TEXT COLLATE PHONEBOOK;");
        db.execSQL("ALTER TABLE raw_contacts ADD sort_key_alt TEXT COLLATE PHONEBOOK;");
        NameSplitter splitter = createNameSplitter();
        SQLiteStatement rawContactUpdate = db.compileStatement("UPDATE raw_contacts SET display_name=?,display_name_alt=?,phonetic_name=?,phonetic_name_style=?,sort_key=?,sort_key_alt=? WHERE _id=?");
        upgradeStructuredNamesToVersion205(db, rawContactUpdate, splitter);
        upgradeOrganizationsToVersion205(db, rawContactUpdate, splitter);
        db.execSQL("DROP INDEX raw_contact_sort_key1_index");
        db.execSQL("CREATE INDEX raw_contact_sort_key1_index ON raw_contacts (contact_in_visible_group,sort_key);");
        db.execSQL("CREATE INDEX raw_contact_sort_key2_index ON raw_contacts (contact_in_visible_group,sort_key_alt);");
    }

    private void upgradeStructuredNamesToVersion205(SQLiteDatabase db, SQLiteStatement rawContactUpdate, NameSplitter splitter) {
        try {
            long mMimeType = DatabaseUtils.longForQuery(db, "SELECT _id FROM mimetypes WHERE mimetype='vnd.android.cursor.item/name'", null);
            SQLiteStatement structuredNameUpdate = db.compileStatement("UPDATE data SET data10=?,data1=?,data11=? WHERE _id=?");
            NameSplitter.Name name = new NameSplitter.Name();
            Cursor cursor = db.query("data JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id)", StructName205Query.COLUMNS, "mimetype_id=" + mMimeType, null, null, null, null);
            while (cursor.moveToNext()) {
                try {
                    long dataId = cursor.getLong(0);
                    long rawContactId = cursor.getLong(1);
                    int displayNameSource = cursor.getInt(2);
                    name.clear();
                    name.prefix = cursor.getString(4);
                    name.givenNames = cursor.getString(5);
                    name.middleName = cursor.getString(6);
                    name.familyName = cursor.getString(7);
                    name.suffix = cursor.getString(8);
                    name.phoneticFamilyName = cursor.getString(9);
                    name.phoneticMiddleName = cursor.getString(10);
                    name.phoneticGivenName = cursor.getString(11);
                    upgradeNameToVersion205(dataId, rawContactId, displayNameSource, name, structuredNameUpdate, rawContactUpdate, splitter);
                } finally {
                    cursor.close();
                }
            }
        } catch (SQLiteDoneException e) {
        }
    }

    private void upgradeNameToVersion205(long dataId, long rawContactId, int displayNameSource, NameSplitter.Name name, SQLiteStatement structuredNameUpdate, SQLiteStatement rawContactUpdate, NameSplitter splitter) {
        splitter.guessNameStyle(name);
        int unadjustedFullNameStyle = name.fullNameStyle;
        name.fullNameStyle = splitter.getAdjustedFullNameStyle(name.fullNameStyle);
        String displayName = splitter.join(name, true, true);
        structuredNameUpdate.bindLong(1, unadjustedFullNameStyle);
        DatabaseUtils.bindObjectToProgram(structuredNameUpdate, 2, displayName);
        structuredNameUpdate.bindLong(3, name.phoneticNameStyle);
        structuredNameUpdate.bindLong(4, dataId);
        structuredNameUpdate.execute();
        if (displayNameSource == 40) {
            String displayNameAlternative = splitter.join(name, false, false);
            String phoneticName = splitter.joinPhoneticName(name);
            String sortKey = null;
            String sortKeyAlternative = null;
            if (phoneticName != null) {
                sortKeyAlternative = phoneticName;
                sortKey = phoneticName;
            } else if (name.fullNameStyle == 3 || name.fullNameStyle == 2) {
                sortKeyAlternative = displayName;
                sortKey = displayName;
            }
            if (sortKey == null) {
                sortKey = displayName;
                sortKeyAlternative = displayNameAlternative;
            }
            updateRawContact205(rawContactUpdate, rawContactId, displayName, displayNameAlternative, name.phoneticNameStyle, phoneticName, sortKey, sortKeyAlternative);
        }
    }

    private void upgradeOrganizationsToVersion205(SQLiteDatabase db, SQLiteStatement rawContactUpdate, NameSplitter splitter) {
        long mimeType = lookupMimeTypeId(db, "vnd.android.cursor.item/organization");
        SQLiteStatement organizationUpdate = db.compileStatement("UPDATE data SET data10=? WHERE _id=?");
        Cursor cursor = db.query("data JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id)", Organization205Query.COLUMNS, "mimetype_id=" + mimeType + " AND display_name_source=30", null, null, null, null);
        while (cursor.moveToNext()) {
            try {
                long dataId = cursor.getLong(0);
                long rawContactId = cursor.getLong(1);
                String company = cursor.getString(2);
                String phoneticName = cursor.getString(3);
                int phoneticNameStyle = splitter.guessPhoneticNameStyle(phoneticName);
                organizationUpdate.bindLong(1, phoneticNameStyle);
                organizationUpdate.bindLong(2, dataId);
                organizationUpdate.execute();
                updateRawContact205(rawContactUpdate, rawContactId, company, company, phoneticNameStyle, phoneticName, company, company);
            } finally {
                cursor.close();
            }
        }
    }

    private void updateRawContact205(SQLiteStatement rawContactUpdate, long rawContactId, String displayName, String displayNameAlternative, int phoneticNameStyle, String phoneticName, String sortKeyPrimary, String sortKeyAlternative) {
        bindString(rawContactUpdate, 1, displayName);
        bindString(rawContactUpdate, 2, displayNameAlternative);
        bindString(rawContactUpdate, 3, phoneticName);
        rawContactUpdate.bindLong(4, phoneticNameStyle);
        bindString(rawContactUpdate, 5, sortKeyPrimary);
        bindString(rawContactUpdate, 6, sortKeyAlternative);
        rawContactUpdate.bindLong(7, rawContactId);
        rawContactUpdate.execute();
    }

    private void upgrateToVersion206(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE raw_contacts ADD name_verified INTEGER NOT NULL DEFAULT 0;");
    }

    private void upgradeEmailToVersion303(SQLiteDatabase db) {
        long mimeTypeId = lookupMimeTypeId(db, "vnd.android.cursor.item/email_v2");
        if (mimeTypeId != -1) {
            ContentValues values = new ContentValues();
            Cursor cursor = db.query("data", Upgrade303Query.COLUMNS, "mimetype_id=? AND _id NOT IN (SELECT data_id FROM name_lookup) AND data1 NOT NULL", new String[]{String.valueOf(mimeTypeId)}, null, null, null);
            while (cursor.moveToNext()) {
                try {
                    long dataId = cursor.getLong(0);
                    long rawContactId = cursor.getLong(1);
                    String value = extractHandleFromEmailAddress(cursor.getString(2));
                    if (value != null) {
                        values.put("data_id", Long.valueOf(dataId));
                        values.put("raw_contact_id", Long.valueOf(rawContactId));
                        values.put("name_type", (Integer) 4);
                        values.put("normalized_name", NameNormalizer.normalize(value));
                        db.insert("name_lookup", null, values);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    private void upgradeNicknameToVersion303(SQLiteDatabase db) {
        long mimeTypeId = lookupMimeTypeId(db, "vnd.android.cursor.item/nickname");
        if (mimeTypeId != -1) {
            ContentValues values = new ContentValues();
            Cursor cursor = db.query("data", Upgrade303Query.COLUMNS, "mimetype_id=? AND _id NOT IN (SELECT data_id FROM name_lookup) AND data1 NOT NULL", new String[]{String.valueOf(mimeTypeId)}, null, null, null);
            while (cursor.moveToNext()) {
                try {
                    long dataId = cursor.getLong(0);
                    long rawContactId = cursor.getLong(1);
                    String value = cursor.getString(2);
                    values.put("data_id", Long.valueOf(dataId));
                    values.put("raw_contact_id", Long.valueOf(rawContactId));
                    values.put("name_type", (Integer) 3);
                    values.put("normalized_name", NameNormalizer.normalize(value));
                    db.insert("name_lookup", null, values);
                } finally {
                    cursor.close();
                }
            }
        }
    }

    private void upgradeToVersion304(SQLiteDatabase db) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS mime_type ON mimetypes (mimetype);");
    }

    private void upgradeToVersion306(SQLiteDatabase db) {
        StringBuilder lookupKeyBuilder = new StringBuilder();
        SQLiteStatement updateStatement = db.compileStatement("UPDATE contacts SET lookup=? WHERE _id=?");
        Cursor c = db.rawQuery("SELECT DISTINCT contact_id FROM raw_contacts WHERE deleted=0 AND account_type='com.android.exchange'", null);
        while (c.moveToNext()) {
            try {
                long contactId = c.getLong(0);
                lookupKeyBuilder.setLength(0);
                c = db.rawQuery("SELECT account_type, account_name, _id, sourceid, display_name FROM raw_contacts WHERE contact_id=? ORDER BY _id", new String[]{String.valueOf(contactId)});
                while (c.moveToNext()) {
                    try {
                        ContactLookupKey.appendToLookupKey(lookupKeyBuilder, c.getString(0), c.getString(1), c.getLong(2), c.getString(3), c.getString(4));
                    } finally {
                        c.close();
                    }
                }
                c.close();
                if (lookupKeyBuilder.length() == 0) {
                    updateStatement.bindNull(1);
                } else {
                    updateStatement.bindString(1, Uri.encode(lookupKeyBuilder.toString()));
                }
                updateStatement.bindLong(2, contactId);
                updateStatement.execute();
            } finally {
                updateStatement.close();
            }
        }
    }

    private void upgradeToVersion307(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE properties (property_key TEXT PRIMARY_KEY, property_value TEXT);");
    }

    private void upgradeToVersion308(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accounts (account_name TEXT, account_type TEXT );");
        db.execSQL("INSERT INTO accounts SELECT DISTINCT account_name, account_type FROM raw_contacts");
    }

    private void upgradeToVersion400(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE groups ADD favorites INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE groups ADD auto_add INTEGER NOT NULL DEFAULT 0;");
    }

    private void upgradeToVersion353(SQLiteDatabase db) {
        db.execSQL("DELETE FROM contacts WHERE NOT EXISTS (SELECT 1 FROM raw_contacts WHERE contact_id=contacts._id)");
    }

    private void rebuildNameLookup(SQLiteDatabase db, boolean rebuildSqliteStats) {
        db.execSQL("DROP INDEX IF EXISTS name_lookup_index");
        insertNameLookup(db);
        createContactsIndexes(db, rebuildSqliteStats);
    }

    protected void rebuildSearchIndex() {
        rebuildSearchIndex(getWritableDatabase(), true);
    }

    private void rebuildSearchIndex(SQLiteDatabase db, boolean rebuildSqliteStats) {
        createSearchIndexTable(db, rebuildSqliteStats);
        setProperty(db, "search_index", "0");
    }

    public boolean needsToUpdateLocaleData(LocaleSet locales) {
        String dbLocale = getProperty("locale", "");
        if (!dbLocale.equals(locales.toString())) {
            return true;
        }
        String curICUVersion = ICU.getIcuVersion();
        String dbICUVersion = getProperty("icu_version", "(unknown)");
        if (!curICUVersion.equals(dbICUVersion)) {
            Log.i("ContactsDatabaseHelper", "ICU version has changed. Current version is " + curICUVersion + "; DB was built with " + dbICUVersion);
            return true;
        }
        return false;
    }

    private void upgradeLocaleData(SQLiteDatabase db, boolean rebuildSqliteStats) {
        LocaleSet locales = LocaleSet.getDefault();
        Log.i("ContactsDatabaseHelper", "Upgrading locale data for " + locales + " (ICU v" + ICU.getIcuVersion() + ")");
        long start = SystemClock.elapsedRealtime();
        initializeCache(db);
        rebuildLocaleData(db, locales, rebuildSqliteStats);
        Log.i("ContactsDatabaseHelper", "Locale update completed in " + (SystemClock.elapsedRealtime() - start) + "ms");
    }

    private void rebuildLocaleData(SQLiteDatabase db, LocaleSet locales, boolean rebuildSqliteStats) {
        db.execSQL("DROP INDEX raw_contact_sort_key1_index");
        db.execSQL("DROP INDEX raw_contact_sort_key2_index");
        db.execSQL("DROP INDEX IF EXISTS name_lookup_index");
        loadNicknameLookupTable(db);
        insertNameLookup(db);
        rebuildSortKeys(db);
        createContactsIndexes(db, rebuildSqliteStats);
        FastScrollingIndexCache.getInstance(this.mContext).invalidate();
        setProperty(db, "icu_version", ICU.getIcuVersion());
        setProperty(db, "locale", locales.toString());
    }

    public void setLocale(LocaleSet locales) {
        if (needsToUpdateLocaleData(locales)) {
            Log.i("ContactsDatabaseHelper", "Switching to locale " + locales + " (ICU v" + ICU.getIcuVersion() + ")");
            long start = SystemClock.elapsedRealtime();
            SQLiteDatabase db = getWritableDatabase();
            db.setLocale(locales.getPrimaryLocale());
            db.beginTransaction();
            try {
                rebuildLocaleData(db, locales, true);
                db.setTransactionSuccessful();
                db.endTransaction();
                Log.i("ContactsDatabaseHelper", "Locale change completed in " + (SystemClock.elapsedRealtime() - start) + "ms");
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
    }

    private void rebuildSortKeys(SQLiteDatabase db) {
        Cursor cursor = db.query("raw_contacts", new String[]{"_id"}, null, null, null, null, null);
        while (cursor.moveToNext()) {
            try {
                long rawContactId = cursor.getLong(0);
                updateRawContactDisplayName(db, rawContactId);
            } finally {
                cursor.close();
            }
        }
    }

    private void insertNameLookup(SQLiteDatabase db) {
        db.execSQL("DELETE FROM name_lookup");
        SQLiteStatement nameLookupInsert = db.compileStatement("INSERT OR IGNORE INTO name_lookup(raw_contact_id,data_id,name_type,normalized_name) VALUES (?,?,?,?)");
        try {
            insertStructuredNameLookup(db, nameLookupInsert);
            insertEmailLookup(db, nameLookupInsert);
            insertNicknameLookup(db, nameLookupInsert);
        } finally {
            nameLookupInsert.close();
        }
    }

    private void insertStructuredNameLookup(SQLiteDatabase db, SQLiteStatement nameLookupInsert) {
        NameSplitter nameSplitter = createNameSplitter();
        NameLookupBuilder nameLookupBuilder = new StructuredNameLookupBuilder(nameSplitter, new CommonNicknameCache(db), nameLookupInsert);
        long mimeTypeId = lookupMimeTypeId(db, "vnd.android.cursor.item/name");
        Cursor cursor = db.query("data", StructuredNameQuery.COLUMNS, "mimetype_id=? AND data1 NOT NULL", new String[]{String.valueOf(mimeTypeId)}, null, null, null);
        while (cursor.moveToNext()) {
            try {
                long dataId = cursor.getLong(0);
                long rawContactId = cursor.getLong(1);
                String name = cursor.getString(2);
                int fullNameStyle = nameSplitter.guessFullNameStyle(name);
                nameLookupBuilder.insertNameLookup(rawContactId, dataId, name, nameSplitter.getAdjustedFullNameStyle(fullNameStyle));
            } finally {
                cursor.close();
            }
        }
    }

    private void insertEmailLookup(SQLiteDatabase db, SQLiteStatement nameLookupInsert) {
        long mimeTypeId = lookupMimeTypeId(db, "vnd.android.cursor.item/email_v2");
        Cursor cursor = db.query("data", EmailQuery.COLUMNS, "mimetype_id=? AND data1 NOT NULL", new String[]{String.valueOf(mimeTypeId)}, null, null, null);
        while (cursor.moveToNext()) {
            try {
                long dataId = cursor.getLong(0);
                long rawContactId = cursor.getLong(1);
                String address = cursor.getString(2);
                insertNameLookup(nameLookupInsert, rawContactId, dataId, 4, extractHandleFromEmailAddress(address));
            } finally {
                cursor.close();
            }
        }
    }

    private void insertNicknameLookup(SQLiteDatabase db, SQLiteStatement nameLookupInsert) {
        long mimeTypeId = lookupMimeTypeId(db, "vnd.android.cursor.item/nickname");
        Cursor cursor = db.query("data", NicknameQuery.COLUMNS, "mimetype_id=? AND data1 NOT NULL", new String[]{String.valueOf(mimeTypeId)}, null, null, null);
        while (cursor.moveToNext()) {
            try {
                long dataId = cursor.getLong(0);
                long rawContactId = cursor.getLong(1);
                String nickname = cursor.getString(2);
                insertNameLookup(nameLookupInsert, rawContactId, dataId, 3, nickname);
            } finally {
                cursor.close();
            }
        }
    }

    public void insertNameLookup(SQLiteStatement stmt, long rawContactId, long dataId, int lookupType, String name) {
        if (!TextUtils.isEmpty(name)) {
            String normalized = NameNormalizer.normalize(name);
            if (!TextUtils.isEmpty(normalized)) {
                insertNormalizedNameLookup(stmt, rawContactId, dataId, lookupType, normalized);
            }
        }
    }

    private void insertNormalizedNameLookup(SQLiteStatement stmt, long rawContactId, long dataId, int lookupType, String normalizedName) {
        stmt.bindLong(1, rawContactId);
        stmt.bindLong(2, dataId);
        stmt.bindLong(3, lookupType);
        stmt.bindString(4, normalizedName);
        stmt.executeInsert();
    }

    private void upgradeToVersion401(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE visible_contacts (_id INTEGER PRIMARY KEY);");
        db.execSQL("INSERT INTO visible_contacts SELECT _id FROM contacts WHERE in_visible_group!=0");
        db.execSQL("DROP INDEX contacts_visible_index");
    }

    private void upgradeToVersion402(SQLiteDatabase db) {
        createDirectoriesTable(db);
    }

    private void upgradeToVersion403(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS directories;");
        createDirectoriesTable(db);
        db.execSQL("ALTER TABLE raw_contacts ADD raw_contact_is_read_only INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE data ADD is_read_only INTEGER NOT NULL DEFAULT 0;");
    }

    private void upgradeToVersion405(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS phone_lookup;");
        db.execSQL("CREATE TABLE phone_lookup (data_id INTEGER REFERENCES data(_id) NOT NULL,raw_contact_id INTEGER REFERENCES raw_contacts(_id) NOT NULL,normalized_number TEXT NOT NULL,min_match TEXT NOT NULL);");
        db.execSQL("CREATE INDEX phone_lookup_index ON phone_lookup (normalized_number,raw_contact_id,data_id);");
        db.execSQL("CREATE INDEX phone_lookup_min_match_index ON phone_lookup (min_match,raw_contact_id,data_id);");
        long mimeTypeId = lookupMimeTypeId(db, "vnd.android.cursor.item/phone_v2");
        if (mimeTypeId != -1) {
            Cursor cursor = db.rawQuery("SELECT _id, raw_contact_id, data1 FROM data WHERE mimetype_id=" + mimeTypeId + " AND data1 NOT NULL", null);
            ContentValues phoneValues = new ContentValues();
            while (cursor.moveToNext()) {
                try {
                    long dataID = cursor.getLong(0);
                    long rawContactID = cursor.getLong(1);
                    String number = cursor.getString(2);
                    String normalizedNumber = PhoneNumberUtils.normalizeNumber(number);
                    if (!TextUtils.isEmpty(normalizedNumber)) {
                        phoneValues.clear();
                        phoneValues.put("raw_contact_id", Long.valueOf(rawContactID));
                        phoneValues.put("data_id", Long.valueOf(dataID));
                        phoneValues.put("normalized_number", normalizedNumber);
                        phoneValues.put("min_match", PhoneNumberUtils.toCallerIDMinMatch(normalizedNumber));
                        db.insert("phone_lookup", null, phoneValues);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    private void upgradeToVersion406(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD countryiso TEXT;");
    }

    private void upgradeToVersion409(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS directories;");
        createDirectoriesTable(db);
    }

    private void upgradeToVersion411(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS default_directory");
        db.execSQL("CREATE TABLE default_directory (_id INTEGER PRIMARY KEY);");
        db.execSQL("INSERT OR IGNORE INTO default_directory  SELECT contact_id  FROM raw_contacts  WHERE raw_contacts.account_name IS NULL    AND raw_contacts.account_type IS NULL ");
        db.execSQL("INSERT OR IGNORE INTO default_directory  SELECT contact_id  FROM raw_contacts  WHERE NOT EXISTS (SELECT _id   FROM groups   WHERE raw_contacts.account_name = groups.account_name    AND raw_contacts.account_type = groups.account_type    AND groups.auto_add != 0)");
        long mimetype = lookupMimeTypeId(db, "vnd.android.cursor.item/group_membership");
        db.execSQL("INSERT OR IGNORE INTO default_directory  SELECT contact_id  FROM raw_contacts  JOIN data    ON (raw_contacts._id=raw_contact_id) WHERE mimetype_id=" + mimetype + " AND EXISTS (SELECT _id  FROM groups  WHERE raw_contacts.account_name = groups.account_name    AND raw_contacts.account_type = groups.account_type    AND groups.auto_add != 0)");
    }

    private void upgradeToVersion413(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS directories;");
        createDirectoriesTable(db);
    }

    private void upgradeToVersion415(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE groups ADD group_is_read_only INTEGER NOT NULL DEFAULT 0");
        db.execSQL("UPDATE groups   SET group_is_read_only=1 WHERE system_id NOT NULL");
    }

    private void upgradeToVersion416(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX phone_lookup_data_id_min_match_index ON phone_lookup (data_id, min_match);");
    }

    private void upgradeToVersion501(SQLiteDatabase db) {
        db.execSQL("DELETE FROM name_lookup WHERE name_type=5");
    }

    private void upgradeToVersion502(SQLiteDatabase db) {
        db.execSQL("DELETE FROM name_lookup WHERE name_type IN (6, 7)");
    }

    private void upgradeToVersion504(SQLiteDatabase db) {
        initializeCache(db);
        Cursor cursor = db.rawQuery("SELECT raw_contact_id FROM data WHERE mimetype_id=? AND data4 NOT NULL", new String[]{String.valueOf(this.mMimeTypeIdStructuredName)});
        while (cursor.moveToNext()) {
            try {
                long rawContactId = cursor.getLong(0);
                updateRawContactDisplayName(db, rawContactId);
            } finally {
                cursor.close();
            }
        }
    }

    private void upgradeToVersion601(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE data_usage_stat(stat_id INTEGER PRIMARY KEY AUTOINCREMENT, data_id INTEGER NOT NULL, usage_type INTEGER NOT NULL DEFAULT 0, times_used INTEGER NOT NULL DEFAULT 0, last_time_used INTERGER NOT NULL DEFAULT 0, FOREIGN KEY(data_id) REFERENCES data(_id));");
        db.execSQL("CREATE UNIQUE INDEX data_usage_stat_index ON data_usage_stat (data_id, usage_type)");
    }

    private void upgradeToVersion602(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD voicemail_uri TEXT;");
        db.execSQL("ALTER TABLE calls ADD _data TEXT;");
        db.execSQL("ALTER TABLE calls ADD has_content INTEGER;");
        db.execSQL("ALTER TABLE calls ADD mime_type TEXT;");
        db.execSQL("ALTER TABLE calls ADD source_data TEXT;");
        db.execSQL("ALTER TABLE calls ADD source_package TEXT;");
        db.execSQL("ALTER TABLE calls ADD state INTEGER;");
    }

    private void upgradeToVersion604(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE voicemail_status (_id INTEGER PRIMARY KEY AUTOINCREMENT,source_package TEXT UNIQUE NOT NULL,settings_uri TEXT,voicemail_access_uri TEXT,configuration_state INTEGER,data_channel_state INTEGER,notification_channel_state INTEGER);");
    }

    private void upgradeToVersion606(SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS view_contacts_restricted;");
        db.execSQL("DROP VIEW IF EXISTS view_data_restricted;");
        db.execSQL("DROP VIEW IF EXISTS view_raw_contacts_restricted;");
        db.execSQL("DROP VIEW IF EXISTS view_raw_entities_restricted;");
        db.execSQL("DROP VIEW IF EXISTS view_entities_restricted;");
        db.execSQL("DROP VIEW IF EXISTS view_data_usage_stat_restricted;");
        db.execSQL("DROP INDEX IF EXISTS contacts_restricted_index");
    }

    private void upgradeToVersion608(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE contacts ADD photo_file_id INTEGER REFERENCES photo_files(_id);");
        db.execSQL("CREATE TABLE photo_files(_id INTEGER PRIMARY KEY AUTOINCREMENT, height INTEGER NOT NULL, width INTEGER NOT NULL, filesize INTEGER NOT NULL);");
    }

    private void upgradeToVersion610(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD is_read INTEGER;");
    }

    private void upgradeToVersion611(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE raw_contacts ADD data_set TEXT DEFAULT NULL;");
        db.execSQL("ALTER TABLE groups ADD data_set TEXT DEFAULT NULL;");
        db.execSQL("ALTER TABLE accounts ADD data_set TEXT DEFAULT NULL;");
        db.execSQL("CREATE INDEX raw_contacts_source_id_data_set_index ON raw_contacts (sourceid, account_type, account_name, data_set);");
        db.execSQL("CREATE INDEX groups_source_id_data_set_index ON groups (sourceid, account_type, account_name, data_set);");
    }

    private void upgradeToVersion612(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD geocoded_location TEXT DEFAULT NULL;");
    }

    private void upgradeToVersion613(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS stream_items");
        db.execSQL("DROP TABLE IF EXISTS stream_item_photos");
        db.execSQL("CREATE TABLE stream_items(_id INTEGER PRIMARY KEY AUTOINCREMENT, raw_contact_id INTEGER NOT NULL, res_package TEXT, icon TEXT, label TEXT, text TEXT, timestamp INTEGER NOT NULL, comments TEXT, stream_item_sync1 TEXT, stream_item_sync2 TEXT, stream_item_sync3 TEXT, stream_item_sync4 TEXT, FOREIGN KEY(raw_contact_id) REFERENCES raw_contacts(_id));");
        db.execSQL("CREATE TABLE stream_item_photos(_id INTEGER PRIMARY KEY AUTOINCREMENT, stream_item_id INTEGER NOT NULL, sort_index INTEGER, photo_file_id INTEGER NOT NULL, stream_item_photo_sync1 TEXT, stream_item_photo_sync2 TEXT, stream_item_photo_sync3 TEXT, stream_item_photo_sync4 TEXT, FOREIGN KEY(stream_item_id) REFERENCES stream_items(_id));");
    }

    private void upgradeToVersion615(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD lookup_uri TEXT DEFAULT NULL;");
        db.execSQL("ALTER TABLE calls ADD matched_number TEXT DEFAULT NULL;");
        db.execSQL("ALTER TABLE calls ADD normalized_number TEXT DEFAULT NULL;");
        db.execSQL("ALTER TABLE calls ADD photo_id INTEGER NOT NULL DEFAULT 0;");
    }

    private void upgradeToVersion618(SQLiteDatabase db) {
        db.execSQL("CREATE TEMPORARY TABLE settings_backup(account_name STRING NOT NULL,account_type STRING NOT NULL,ungrouped_visible INTEGER NOT NULL DEFAULT 0,should_sync INTEGER NOT NULL DEFAULT 1);");
        db.execSQL("INSERT INTO settings_backup SELECT account_name, account_type, ungrouped_visible, should_sync FROM settings");
        db.execSQL("DROP TABLE settings");
        db.execSQL("CREATE TABLE settings (account_name STRING NOT NULL,account_type STRING NOT NULL,data_set STRING,ungrouped_visible INTEGER NOT NULL DEFAULT 0,should_sync INTEGER NOT NULL DEFAULT 1);");
        db.execSQL("INSERT INTO settings SELECT account_name, account_type, NULL, ungrouped_visible, should_sync FROM settings_backup");
        db.execSQL("DROP TABLE settings_backup");
    }

    private void upgradeToVersion622(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD formatted_number TEXT DEFAULT NULL;");
    }

    private void upgradeToVersion626(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS accounts");
        db.execSQL("CREATE TABLE accounts (_id INTEGER PRIMARY KEY AUTOINCREMENT,account_name TEXT, account_type TEXT, data_set TEXT);");
        db.execSQL("ALTER TABLE raw_contacts ADD account_id INTEGER REFERENCES accounts(_id)");
        db.execSQL("ALTER TABLE groups ADD account_id INTEGER REFERENCES accounts(_id)");
        db.execSQL("DROP INDEX IF EXISTS raw_contacts_source_id_index");
        db.execSQL("DROP INDEX IF EXISTS raw_contacts_source_id_data_set_index");
        db.execSQL("DROP INDEX IF EXISTS groups_source_id_index");
        db.execSQL("DROP INDEX IF EXISTS groups_source_id_data_set_index");
        db.execSQL("CREATE INDEX raw_contacts_source_id_account_id_index ON raw_contacts (sourceid, account_id);");
        db.execSQL("CREATE INDEX groups_source_id_account_id_index ON groups (sourceid, account_id);");
        Set<AccountWithDataSet> accountsWithDataSets = Sets.newHashSet();
        upgradeToVersion626_findAccountsWithDataSets(accountsWithDataSets, db, "raw_contacts");
        upgradeToVersion626_findAccountsWithDataSets(accountsWithDataSets, db, "groups");
        for (AccountWithDataSet accountWithDataSet : accountsWithDataSets) {
            db.execSQL("INSERT INTO accounts (account_name,account_type,data_set)VALUES(?, ?, ?)", new String[]{accountWithDataSet.getAccountName(), accountWithDataSet.getAccountType(), accountWithDataSet.getDataSet()});
        }
        upgradeToVersion626_fillAccountId(db, "raw_contacts");
        upgradeToVersion626_fillAccountId(db, "groups");
    }

    private static void upgradeToVersion626_findAccountsWithDataSets(Set<AccountWithDataSet> result, SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery("SELECT DISTINCT account_name, account_type, data_set FROM " + table, null);
        while (c.moveToNext()) {
            try {
                result.add(AccountWithDataSet.get(c.getString(0), c.getString(1), c.getString(2)));
            } finally {
                c.close();
            }
        }
    }

    private static void upgradeToVersion626_fillAccountId(SQLiteDatabase db, String table) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE " + table + " SET account_id = (SELECT _id FROM accounts WHERE ");
        addJoinExpressionAllowingNull(sb, table + ".account_name", "accounts.account_name");
        sb.append("AND");
        addJoinExpressionAllowingNull(sb, table + ".account_type", "accounts.account_type");
        sb.append("AND");
        addJoinExpressionAllowingNull(sb, table + ".data_set", "accounts.data_set");
        sb.append("), account_name = null, account_type = null, data_set = null");
        db.execSQL(sb.toString());
    }

    private void upgradeToVersion701(SQLiteDatabase db) {
        db.execSQL("UPDATE raw_contacts SET last_time_contacted = max(ifnull(last_time_contacted, 0),  ifnull((SELECT max(last_time_used)  FROM data JOIN data_usage_stat ON (data._id = data_usage_stat.data_id) WHERE data.raw_contact_id = raw_contacts._id), 0))");
        db.execSQL("UPDATE raw_contacts SET last_time_contacted = null where last_time_contacted = 0");
    }

    private void upgradeToVersion702(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT _id, raw_contact_id, data1 FROM data  WHERE mimetype_id=(SELECT _id FROM mimetypes WHERE mimetype='vnd.android.cursor.item/phone_v2') AND data4 not like '+%'", null);
        try {
            int count = c.getCount();
            if (count != 0) {
                long[] dataIds = new long[count];
                long[] rawContactIds = new long[count];
                String[] phoneNumbers = new String[count];
                StringBuilder sbDataIds = new StringBuilder();
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    int i = c.getPosition();
                    dataIds[i] = c.getLong(0);
                    rawContactIds[i] = c.getLong(1);
                    phoneNumbers[i] = c.getString(2);
                    if (sbDataIds.length() > 0) {
                        sbDataIds.append(",");
                    }
                    sbDataIds.append(dataIds[i]);
                }
                c.close();
                String dataIdList = sbDataIds.toString();
                db.execSQL("UPDATE data SET data4 = null WHERE _id IN (" + dataIdList + ")");
                db.execSQL("DELETE FROM phone_lookup WHERE data_id IN (" + dataIdList + ")");
                for (int i2 = 0; i2 < count; i2++) {
                    String phoneNumber = phoneNumbers[i2];
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        String normalized = PhoneNumberUtils.normalizeNumber(phoneNumber);
                        if (!TextUtils.isEmpty(normalized)) {
                            db.execSQL("INSERT INTO phone_lookup(data_id, raw_contact_id, normalized_number, min_match) VALUES(?,?,?,?)", new String[]{String.valueOf(dataIds[i2]), String.valueOf(rawContactIds[i2]), normalized, PhoneNumberUtils.toCallerIDMinMatch(normalized)});
                        }
                    }
                }
            }
        } finally {
            c.close();
        }
    }

    private void upgradeToVersion707(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE raw_contacts ADD phonebook_label TEXT;");
        db.execSQL("ALTER TABLE raw_contacts ADD phonebook_bucket INTEGER;");
        db.execSQL("ALTER TABLE raw_contacts ADD phonebook_label_alt TEXT;");
        db.execSQL("ALTER TABLE raw_contacts ADD phonebook_bucket_alt INTEGER;");
    }

    private void upgradeToVersion710(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE contacts ADD contact_last_updated_timestamp INTEGER;");
        db.execSQL("UPDATE contacts SET contact_last_updated_timestamp = " + System.currentTimeMillis());
        db.execSQL("CREATE INDEX contacts_contact_last_updated_timestamp_index ON contacts(contact_last_updated_timestamp)");
        db.execSQL("CREATE TABLE deleted_contacts (contact_id INTEGER PRIMARY KEY,contact_deleted_timestamp INTEGER NOT NULL default 0);");
        db.execSQL("CREATE INDEX deleted_contacts_contact_deleted_timestamp_index ON deleted_contacts(contact_deleted_timestamp)");
    }

    private void upgradeToVersion800(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD presentation INTEGER NOT NULL DEFAULT 1;");
        db.execSQL("UPDATE calls SET presentation=2, number='' WHERE number='-2';");
        db.execSQL("UPDATE calls SET presentation=3, number='' WHERE number='-1';");
        db.execSQL("UPDATE calls SET presentation=4, number='' WHERE number='-3';");
    }

    private void upgradeToVersion802(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE contacts ADD pinned INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE raw_contacts ADD pinned INTEGER NOT NULL DEFAULT  0;");
    }

    private void upgradeToVersion902(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD subscription_component_name TEXT;");
        db.execSQL("ALTER TABLE calls ADD subscription_id TEXT;");
    }

    private void upgradeToVersion903(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT _id, number, countryiso FROM calls  WHERE (normalized_number is null OR normalized_number = '')  AND countryiso != '' AND countryiso is not null  AND number != '' AND number is not null;", null);
        try {
            if (c.getCount() != 0) {
                db.beginTransaction();
                try {
                    c.moveToPosition(-1);
                    while (c.moveToNext()) {
                        long callId = c.getLong(0);
                        String unNormalizedNumber = c.getString(1);
                        String countryIso = c.getString(2);
                        String normalizedNumber = PhoneNumberUtils.formatNumberToE164(unNormalizedNumber, countryIso);
                        if (!TextUtils.isEmpty(normalizedNumber)) {
                            db.execSQL("UPDATE calls set normalized_number = ? where _id = ?;", new String[]{normalizedNumber, String.valueOf(callId)});
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        } finally {
            c.close();
        }
    }

    private void upgradeToVersion904(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD features INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE calls ADD data_usage INTEGER;");
    }

    private void upgradeToVersion905(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE calls ADD transcription TEXT;");
    }

    public void upgradeToVersion906(SQLiteDatabase db) {
        db.execSQL("UPDATE contacts SET pinned = pinned + 1 WHERE pinned >= 0 AND pinned < 2147483647;");
        db.execSQL("UPDATE raw_contacts SET pinned = pinned + 1 WHERE pinned >= 0 AND pinned < 2147483647;");
        db.execSQL("UPDATE contacts SET pinned = 0 WHERE pinned = 2147483647;");
        db.execSQL("UPDATE raw_contacts SET pinned = 0 WHERE pinned = 2147483647;");
    }

    private void upgradeToVersion908(SQLiteDatabase db) {
        db.execSQL("UPDATE contacts SET pinned = 0 WHERE pinned = 2147483647;");
        db.execSQL("UPDATE raw_contacts SET pinned = 0 WHERE pinned = 2147483647;");
    }

    private void upgradeToVersion909(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE calls ADD sub_id INTEGER DEFAULT -1;");
        } catch (SQLiteException e) {
            db.execSQL("UPDATE calls SET subscription_component_name='com.android.phone/com.android.services.telephony.TelephonyConnectionService';");
            db.execSQL("UPDATE calls SET subscription_id=sub_id;");
        }
    }

    public void upgradeToVersion910(SQLiteDatabase db) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        UserInfo user = userManager.getUserInfo(userManager.getUserHandle());
        if (user.isManagedProfile()) {
            db.execSQL("DELETE FROM calls;");
        }
    }

    public String extractHandleFromEmailAddress(String email) {
        String address;
        int index;
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(email);
        if (tokens.length == 0 || (index = (address = tokens[0].getAddress()).indexOf(64)) == -1) {
            return null;
        }
        return address.substring(0, index);
    }

    public String extractAddressFromEmailAddress(String email) {
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(email);
        if (tokens.length == 0) {
            return null;
        }
        return tokens[0].getAddress().trim();
    }

    private static long lookupMimeTypeId(SQLiteDatabase db, String mimeType) {
        try {
            return DatabaseUtils.longForQuery(db, "SELECT _id FROM mimetypes WHERE mimetype='" + mimeType + "'", null);
        } catch (SQLiteDoneException e) {
            return -1L;
        }
    }

    private void bindString(SQLiteStatement stmt, int index, String value) {
        if (value == null) {
            stmt.bindNull(index);
        } else {
            stmt.bindString(index, value);
        }
    }

    private void bindLong(SQLiteStatement stmt, int index, Number value) {
        if (value == null) {
            stmt.bindNull(index);
        } else {
            stmt.bindLong(index, value.longValue());
        }
    }

    private static StringBuilder addJoinExpressionAllowingNull(StringBuilder sb, String column1, String column2) {
        sb.append("(((").append(column1).append(")=(").append(column2);
        sb.append("))OR((");
        sb.append(column1).append(") IS NULL AND (").append(column2).append(") IS NULL))");
        return sb;
    }

    private void updateSqliteStats(SQLiteDatabase db) {
        if (this.mDatabaseOptimizationEnabled) {
            try {
                db.execSQL("DELETE FROM sqlite_stat1");
                updateIndexStats(db, "contacts", "contacts_has_phone_index", "9000 500");
                updateIndexStats(db, "contacts", "contacts_name_raw_contact_id_index", "9000 1");
                updateIndexStats(db, "contacts", MoreDatabaseUtils.buildIndexName("contacts", "contact_last_updated_timestamp"), "9000 10");
                updateIndexStats(db, "raw_contacts", "raw_contacts_contact_id_index", "10000 2");
                updateIndexStats(db, "raw_contacts", "raw_contact_sort_key2_index", "10000 2");
                updateIndexStats(db, "raw_contacts", "raw_contact_sort_key1_index", "10000 2");
                updateIndexStats(db, "raw_contacts", "raw_contacts_source_id_account_id_index", "10000 1 1 1 1");
                updateIndexStats(db, "name_lookup", "name_lookup_raw_contact_id_index", "35000 4");
                updateIndexStats(db, "name_lookup", "name_lookup_index", "35000 2 2 2 1");
                updateIndexStats(db, "name_lookup", "sqlite_autoindex_name_lookup_1", "35000 3 2 1");
                updateIndexStats(db, "phone_lookup", "phone_lookup_index", "3500 3 2 1");
                updateIndexStats(db, "phone_lookup", "phone_lookup_min_match_index", "3500 3 2 2");
                updateIndexStats(db, "phone_lookup", "phone_lookup_data_id_min_match_index", "3500 2 2");
                updateIndexStats(db, "data", "data_mimetype_data1_index", "60000 5000 2");
                updateIndexStats(db, "data", "data_raw_contact_id", "60000 10");
                updateIndexStats(db, "groups", "groups_source_id_account_id_index", "50 2 2 1 1");
                updateIndexStats(db, "nickname_lookup", "nickname_lookup_index", "500 2 1");
                updateIndexStats(db, "calls", null, "250");
                updateIndexStats(db, "status_updates", null, "100");
                updateIndexStats(db, "stream_items", null, "500");
                updateIndexStats(db, "stream_item_photos", null, "50");
                updateIndexStats(db, "voicemail_status", null, "5");
                updateIndexStats(db, "accounts", null, "3");
                updateIndexStats(db, "visible_contacts", null, "2000");
                updateIndexStats(db, "photo_files", null, "50");
                updateIndexStats(db, "default_directory", null, "1500");
                updateIndexStats(db, "mimetypes", "mime_type", "18 1");
                updateIndexStats(db, "data_usage_stat", "data_usage_stat_index", "20 2 1");
                updateIndexStats(db, "agg_exceptions", null, "10");
                updateIndexStats(db, "settings", null, "10");
                updateIndexStats(db, "packages", null, "0");
                updateIndexStats(db, "directories", null, "3");
                updateIndexStats(db, "v1_settings", null, "0");
                updateIndexStats(db, "android_metadata", null, "1");
                updateIndexStats(db, "_sync_state", "sqlite_autoindex__sync_state_1", "2 1 1");
                updateIndexStats(db, "_sync_state_metadata", null, "1");
                updateIndexStats(db, "properties", "sqlite_autoindex_properties_1", "4 1");
                updateIndexStats(db, "search_index_docsize", null, "9000");
                updateIndexStats(db, "search_index_content", null, "9000");
                updateIndexStats(db, "search_index_stat", null, "1");
                updateIndexStats(db, "search_index_segments", null, "450");
                updateIndexStats(db, "search_index_segdir", "sqlite_autoindex_search_index_segdir_1", "9 5 1");
                db.execSQL("ANALYZE sqlite_master;");
            } catch (SQLException e) {
                Log.e("ContactsDatabaseHelper", "Could not update index stats", e);
            }
        }
    }

    private void updateIndexStats(SQLiteDatabase db, String table, String index, String stats) {
        if (index == null) {
            db.execSQL("DELETE FROM sqlite_stat1 WHERE tbl=? AND idx IS NULL", new String[]{table});
        } else {
            db.execSQL("DELETE FROM sqlite_stat1 WHERE tbl=? AND idx=?", new String[]{table, index});
        }
        db.execSQL("INSERT INTO sqlite_stat1 (tbl,idx,stat) VALUES (?,?,?)", new String[]{table, index, stats});
    }

    public void wipeData() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM accounts;");
        db.execSQL("DELETE FROM contacts;");
        db.execSQL("DELETE FROM raw_contacts;");
        db.execSQL("DELETE FROM stream_items;");
        db.execSQL("DELETE FROM stream_item_photos;");
        db.execSQL("DELETE FROM photo_files;");
        db.execSQL("DELETE FROM data;");
        db.execSQL("DELETE FROM phone_lookup;");
        db.execSQL("DELETE FROM name_lookup;");
        db.execSQL("DELETE FROM groups;");
        db.execSQL("DELETE FROM agg_exceptions;");
        db.execSQL("DELETE FROM settings;");
        db.execSQL("DELETE FROM calls;");
        db.execSQL("DELETE FROM directories;");
        db.execSQL("DELETE FROM search_index;");
        db.execSQL("DELETE FROM deleted_contacts;");
        db.execSQL("DELETE FROM mimetypes;");
        db.execSQL("DELETE FROM packages;");
        initializeCache(db);
    }

    public NameSplitter createNameSplitter() {
        return createNameSplitter(Locale.getDefault());
    }

    public NameSplitter createNameSplitter(Locale locale) {
        this.mNameSplitter = new NameSplitter(this.mContext.getString(android.R.string.EmergencyCallWarningSummary), this.mContext.getString(android.R.string.Midnight), this.mContext.getString(android.R.string.EmergencyCallWarningTitle), this.mContext.getString(android.R.string.NetworkPreferenceSwitchSummary), locale);
        return this.mNameSplitter;
    }

    private static long getIdCached(SQLiteDatabase db, ConcurrentHashMap<String, Long> cache, String querySql, String insertSql, String value) {
        if (cache.containsKey(value)) {
            return cache.get(value).longValue();
        }
        long id = queryIdWithOneArg(db, querySql, value);
        if (id >= 0) {
            cache.put(value, Long.valueOf(id));
            return id;
        }
        long id2 = insertWithOneArgAndReturnId(db, insertSql, value);
        if (id2 >= 0) {
            cache.put(value, Long.valueOf(id2));
            return id2;
        }
        Log.i("ContactsDatabaseHelper", "Cache conflict detected: value=" + value);
        try {
            Thread.sleep(1L);
        } catch (InterruptedException e) {
        }
        return getIdCached(db, cache, querySql, insertSql, value);
    }

    static long queryIdWithOneArg(SQLiteDatabase db, String sql, String sqlArgument) {
        SQLiteStatement query = db.compileStatement(sql);
        try {
            DatabaseUtils.bindObjectToProgram(query, 1, sqlArgument);
            try {
                return query.simpleQueryForLong();
            } catch (SQLiteDoneException e) {
                return -1L;
            }
        } finally {
            query.close();
        }
    }

    static long insertWithOneArgAndReturnId(SQLiteDatabase db, String sql, String sqlArgument) {
        SQLiteStatement insert = db.compileStatement(sql);
        try {
            DatabaseUtils.bindObjectToProgram(insert, 1, sqlArgument);
            try {
                return insert.executeInsert();
            } catch (SQLiteConstraintException e) {
                return -1L;
            }
        } finally {
            insert.close();
        }
    }

    public long getPackageId(String packageName) {
        return getIdCached(getWritableDatabase(), this.mPackageCache, "SELECT _id FROM packages WHERE package=?", "INSERT INTO packages(package) VALUES (?)", packageName);
    }

    public long getMimeTypeId(String mimetype) {
        return lookupMimeTypeId(mimetype, getWritableDatabase());
    }

    private long lookupMimeTypeId(String mimetype, SQLiteDatabase db) {
        return getIdCached(db, this.mMimetypeCache, "SELECT _id FROM mimetypes WHERE mimetype=?", "INSERT INTO mimetypes(mimetype) VALUES (?)", mimetype);
    }

    public long getMimeTypeIdForStructuredName() {
        return this.mMimeTypeIdStructuredName;
    }

    public long getMimeTypeIdForStructuredPostal() {
        return this.mMimeTypeIdStructuredPostal;
    }

    public long getMimeTypeIdForOrganization() {
        return this.mMimeTypeIdOrganization;
    }

    public long getMimeTypeIdForIm() {
        return this.mMimeTypeIdIm;
    }

    public long getMimeTypeIdForEmail() {
        return this.mMimeTypeIdEmail;
    }

    public long getMimeTypeIdForPhone() {
        return this.mMimeTypeIdPhone;
    }

    public long getMimeTypeIdForSip() {
        return this.mMimeTypeIdSip;
    }

    public int getDisplayNameSourceForMimeTypeId(int mimeTypeId) {
        if (mimeTypeId == this.mMimeTypeIdStructuredName) {
            return 40;
        }
        if (mimeTypeId == this.mMimeTypeIdEmail) {
            return 10;
        }
        if (mimeTypeId == this.mMimeTypeIdPhone) {
            return 20;
        }
        if (mimeTypeId == this.mMimeTypeIdOrganization) {
            return 30;
        }
        if (mimeTypeId == this.mMimeTypeIdNickname) {
            return 35;
        }
        return 0;
    }

    public String getDataMimeType(long dataId) {
        if (this.mDataMimetypeQuery == null) {
            this.mDataMimetypeQuery = getWritableDatabase().compileStatement("SELECT mimetype FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) WHERE data._id=?");
        }
        try {
            DatabaseUtils.bindObjectToProgram(this.mDataMimetypeQuery, 1, Long.valueOf(dataId));
            return this.mDataMimetypeQuery.simpleQueryForString();
        } catch (SQLiteDoneException e) {
            return null;
        }
    }

    public void invalidateAllCache() {
        Log.w("ContactsDatabaseHelper", "invalidateAllCache: [" + getClass().getSimpleName() + "]");
        this.mMimetypeCache.clear();
        this.mPackageCache.clear();
    }

    public Set<AccountWithDataSet> getAllAccountsWithDataSets() {
        Set<AccountWithDataSet> result = Sets.newHashSet();
        Cursor c = getReadableDatabase().rawQuery("SELECT DISTINCT _id,account_name,account_type,data_set FROM accounts", null);
        while (c.moveToNext()) {
            try {
                result.add(AccountWithDataSet.get(c.getString(1), c.getString(2), c.getString(3)));
            } finally {
                c.close();
            }
        }
        return result;
    }

    public Long getAccountIdOrNull(AccountWithDataSet accountWithDataSet) {
        if (accountWithDataSet == null) {
            accountWithDataSet = AccountWithDataSet.LOCAL;
        }
        SQLiteStatement select = getWritableDatabase().compileStatement("SELECT _id FROM accounts WHERE ((?1 IS NULL AND account_name IS NULL) OR (account_name=?1)) AND ((?2 IS NULL AND account_type IS NULL) OR (account_type=?2)) AND ((?3 IS NULL AND data_set IS NULL) OR (data_set=?3))");
        try {
            DatabaseUtils.bindObjectToProgram(select, 1, accountWithDataSet.getAccountName());
            DatabaseUtils.bindObjectToProgram(select, 2, accountWithDataSet.getAccountType());
            DatabaseUtils.bindObjectToProgram(select, 3, accountWithDataSet.getDataSet());
            try {
                return Long.valueOf(select.simpleQueryForLong());
            } catch (SQLiteDoneException e) {
                return null;
            }
        } finally {
            select.close();
        }
    }

    public long getOrCreateAccountIdInTransaction(AccountWithDataSet accountWithDataSet) {
        if (accountWithDataSet == null) {
            accountWithDataSet = AccountWithDataSet.LOCAL;
        }
        Long id = getAccountIdOrNull(accountWithDataSet);
        if (id != null) {
            return id.longValue();
        }
        SQLiteStatement insert = getWritableDatabase().compileStatement("INSERT INTO accounts (account_name, account_type, data_set) VALUES (?, ?, ?)");
        try {
            DatabaseUtils.bindObjectToProgram(insert, 1, accountWithDataSet.getAccountName());
            DatabaseUtils.bindObjectToProgram(insert, 2, accountWithDataSet.getAccountType());
            DatabaseUtils.bindObjectToProgram(insert, 3, accountWithDataSet.getDataSet());
            Long id2 = Long.valueOf(insert.executeInsert());
            insert.close();
            return id2.longValue();
        } catch (Throwable th) {
            insert.close();
            throw th;
        }
    }

    public void updateAllVisible() {
        updateCustomContactVisibility(getWritableDatabase(), -1L);
    }

    public boolean updateContactVisibleOnlyIfChanged(TransactionContext txContext, long contactId) {
        return updateContactVisible(txContext, contactId, true);
    }

    public void updateContactVisible(TransactionContext txContext, long contactId) {
        updateContactVisible(txContext, contactId, false);
    }

    public boolean updateContactVisible(TransactionContext txContext, long contactId, boolean onlyIfChanged) {
        SQLiteDatabase db = getWritableDatabase();
        updateCustomContactVisibility(db, contactId);
        String contactIdAsString = String.valueOf(contactId);
        long mimetype = getMimeTypeId("vnd.android.cursor.item/group_membership");
        boolean newVisibility = DatabaseUtils.longForQuery(db, "SELECT EXISTS (SELECT contact_id FROM raw_contacts JOIN data   ON (raw_contacts._id=raw_contact_id) WHERE contact_id=?1   AND mimetype_id=?2) OR EXISTS (SELECT _id FROM raw_contacts WHERE contact_id=?1   AND NOT EXISTS (SELECT _id  FROM groups  WHERE raw_contacts.account_id = groups.account_id  AND auto_add != 0)) OR EXISTS (SELECT _id FROM raw_contacts WHERE contact_id=?1   AND raw_contacts.account_id=(SELECT _id FROM accounts WHERE account_name IS NULL AND account_type IS NULL AND data_set IS NULL))", new String[]{contactIdAsString, String.valueOf(mimetype)}) != 0;
        if (onlyIfChanged) {
            boolean oldVisibility = isContactInDefaultDirectory(db, contactId);
            if (oldVisibility == newVisibility) {
                return false;
            }
        }
        if (newVisibility) {
            db.execSQL("INSERT OR IGNORE INTO default_directory VALUES(?)", new String[]{contactIdAsString});
            txContext.invalidateSearchIndexForContact(contactId);
        } else {
            db.execSQL("DELETE FROM default_directory WHERE _id=?", new String[]{contactIdAsString});
            db.execSQL("DELETE FROM search_index WHERE contact_id=CAST(? AS int)", new String[]{contactIdAsString});
        }
        return true;
    }

    public boolean isContactInDefaultDirectory(SQLiteDatabase db, long contactId) {
        if (this.mContactInDefaultDirectoryQuery == null) {
            this.mContactInDefaultDirectoryQuery = db.compileStatement("SELECT EXISTS (SELECT 1 FROM default_directory WHERE _id=?)");
        }
        this.mContactInDefaultDirectoryQuery.bindLong(1, contactId);
        return this.mContactInDefaultDirectoryQuery.simpleQueryForLong() != 0;
    }

    private void updateCustomContactVisibility(SQLiteDatabase db, long optionalContactId) {
        long groupMembershipMimetypeId = getMimeTypeId("vnd.android.cursor.item/group_membership");
        String[] selectionArgs = {String.valueOf(groupMembershipMimetypeId)};
        String contactIdSelect = optionalContactId < 0 ? "" : "_id=" + optionalContactId + " AND ";
        db.execSQL("DELETE FROM visible_contacts WHERE _id IN(SELECT _id FROM contacts WHERE " + contactIdSelect + "(SELECT MAX((SELECT (CASE WHEN (CASE WHEN raw_contacts.account_id=(SELECT _id FROM accounts WHERE account_name IS NULL AND account_type IS NULL AND data_set IS NULL) THEN 1  WHEN COUNT(groups._id)=0 THEN ungrouped_visible ELSE MAX(group_visible)END)=1 THEN 1 ELSE 0 END) FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id)LEFT OUTER JOIN settings ON (accounts.account_name=settings.account_name AND accounts.account_type=settings.account_type AND ((accounts.data_set IS NULL AND settings.data_set IS NULL) OR (accounts.data_set=settings.data_set))) LEFT OUTER JOIN data ON (data.mimetype_id=? AND data.raw_contact_id = raw_contacts._id) LEFT OUTER JOIN groups ON (groups._id = data.data1) WHERE raw_contacts._id=outer_raw_contacts._id)) FROM raw_contacts AS outer_raw_contacts WHERE contact_id=contacts._id GROUP BY contact_id)=0) ", selectionArgs);
        db.execSQL("INSERT INTO visible_contacts SELECT _id FROM contacts WHERE " + contactIdSelect + "_id NOT IN visible_contacts AND (SELECT MAX((SELECT (CASE WHEN (CASE WHEN raw_contacts.account_id=(SELECT _id FROM accounts WHERE account_name IS NULL AND account_type IS NULL AND data_set IS NULL) THEN 1  WHEN COUNT(groups._id)=0 THEN ungrouped_visible ELSE MAX(group_visible)END)=1 THEN 1 ELSE 0 END) FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id)LEFT OUTER JOIN settings ON (accounts.account_name=settings.account_name AND accounts.account_type=settings.account_type AND ((accounts.data_set IS NULL AND settings.data_set IS NULL) OR (accounts.data_set=settings.data_set))) LEFT OUTER JOIN data ON (data.mimetype_id=? AND data.raw_contact_id = raw_contacts._id) LEFT OUTER JOIN groups ON (groups._id = data.data1) WHERE raw_contacts._id=outer_raw_contacts._id)) FROM raw_contacts AS outer_raw_contacts WHERE contact_id=contacts._id GROUP BY contact_id)=1 ", selectionArgs);
    }

    public long getContactId(long rawContactId) {
        if (this.mContactIdQuery == null) {
            this.mContactIdQuery = getWritableDatabase().compileStatement("SELECT contact_id FROM raw_contacts WHERE _id=?");
        }
        try {
            DatabaseUtils.bindObjectToProgram(this.mContactIdQuery, 1, Long.valueOf(rawContactId));
            return this.mContactIdQuery.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            return 0L;
        }
    }

    public int getAggregationMode(long rawContactId) {
        if (this.mAggregationModeQuery == null) {
            this.mAggregationModeQuery = getWritableDatabase().compileStatement("SELECT aggregation_mode FROM raw_contacts WHERE _id=?");
        }
        try {
            DatabaseUtils.bindObjectToProgram(this.mAggregationModeQuery, 1, Long.valueOf(rawContactId));
            return (int) this.mAggregationModeQuery.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            return 3;
        }
    }

    public void buildPhoneLookupAndContactQuery(SQLiteQueryBuilder qb, String normalizedNumber, String numberE164) {
        String minMatch = PhoneNumberUtils.toCallerIDMinMatch(normalizedNumber);
        StringBuilder sb = new StringBuilder();
        appendPhoneLookupTables(sb, minMatch, true);
        qb.setTables(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        appendPhoneLookupSelection(sb2, normalizedNumber, numberE164);
        qb.appendWhere(sb2.toString());
    }

    public void buildFallbackPhoneLookupAndContactQuery(SQLiteQueryBuilder qb, String number) {
        String minMatch = PhoneNumberUtils.toCallerIDMinMatch(number);
        StringBuilder sb = new StringBuilder();
        sb.append("raw_contacts");
        sb.append(" JOIN view_contacts as contacts_view ON (contacts_view._id = raw_contacts.contact_id) JOIN (SELECT data_id,normalized_number FROM phone_lookup WHERE (phone_lookup.min_match = '");
        sb.append(minMatch);
        sb.append("')) AS lookup ON lookup.data_id=data._id JOIN data ON data.raw_contact_id=raw_contacts._id");
        qb.setTables(sb.toString());
        sb.setLength(0);
        sb.append("PHONE_NUMBERS_EQUAL(data.data1, ");
        DatabaseUtils.appendEscapedSQLString(sb, number);
        sb.append(this.mUseStrictPhoneNumberComparison ? ", 1)" : ", 0)");
        qb.appendWhere(sb.toString());
    }

    public String[] buildSipContactQuery(StringBuilder sb, String sipAddress) {
        sb.append("upper(");
        sb.append("data1");
        sb.append(")=upper(?) AND ");
        sb.append("mimetype_id");
        sb.append("=");
        sb.append(Long.toString(getMimeTypeIdForSip()));
        return new String[]{sipAddress};
    }

    public String buildPhoneLookupAsNestedQuery(String number) {
        StringBuilder sb = new StringBuilder();
        String minMatch = PhoneNumberUtils.toCallerIDMinMatch(number);
        sb.append("(SELECT DISTINCT raw_contact_id FROM ");
        appendPhoneLookupTables(sb, minMatch, false);
        sb.append(" WHERE ");
        appendPhoneLookupSelection(sb, number, null);
        sb.append(")");
        return sb.toString();
    }

    private void appendPhoneLookupTables(StringBuilder sb, String minMatch, boolean joinContacts) {
        sb.append("raw_contacts");
        if (joinContacts) {
            sb.append(" JOIN view_contacts contacts_view ON (contacts_view._id = raw_contacts.contact_id)");
        }
        sb.append(", (SELECT data_id, normalized_number, length(normalized_number) as len  FROM phone_lookup  WHERE (phone_lookup.min_match = '");
        sb.append(minMatch);
        sb.append("')) AS lookup, data");
    }

    private void appendPhoneLookupSelection(StringBuilder sb, String number, String numberE164) {
        sb.append("lookup.data_id=data._id AND data.raw_contact_id=raw_contacts._id");
        boolean hasNumberE164 = !TextUtils.isEmpty(numberE164);
        boolean hasNumber = !TextUtils.isEmpty(number);
        if (hasNumberE164 || hasNumber) {
            sb.append(" AND ( ");
            if (hasNumberE164) {
                sb.append(" lookup.normalized_number = ");
                DatabaseUtils.appendEscapedSQLString(sb, numberE164);
            }
            if (hasNumberE164 && hasNumber) {
                sb.append(" OR ");
            }
            if (hasNumber) {
                if (!this.mUseStrictPhoneNumberComparison) {
                    int numberLen = number.length();
                    sb.append(" lookup.len <= ");
                    sb.append(numberLen);
                    sb.append(" AND substr(");
                    DatabaseUtils.appendEscapedSQLString(sb, number);
                    sb.append(',');
                    sb.append(numberLen);
                    sb.append(" - lookup.len + 1) = lookup.normalized_number");
                    sb.append(" OR (");
                    sb.append(" lookup.len > ");
                    sb.append(numberLen);
                    sb.append(" AND substr(lookup.normalized_number,");
                    sb.append("lookup.len + 1 - ");
                    sb.append(numberLen);
                    sb.append(") = ");
                    DatabaseUtils.appendEscapedSQLString(sb, number);
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

    private void loadNicknameLookupTable(SQLiteDatabase db) {
        db.execSQL("DELETE FROM nickname_lookup");
        String[] strings = this.mContext.getResources().getStringArray(android.R.array.config_deviceStatesAvailableForAppRequests);
        if (strings != null && strings.length != 0) {
            SQLiteStatement nicknameLookupInsert = db.compileStatement("INSERT INTO nickname_lookup(name,cluster) VALUES (?,?)");
            for (int clusterId = 0; clusterId < strings.length; clusterId++) {
                try {
                    String[] names = strings[clusterId].split(",");
                    for (String name : names) {
                        String normalizedName = NameNormalizer.normalize(name);
                        try {
                            DatabaseUtils.bindObjectToProgram(nicknameLookupInsert, 1, normalizedName);
                            DatabaseUtils.bindObjectToProgram(nicknameLookupInsert, 2, String.valueOf(clusterId));
                            nicknameLookupInsert.executeInsert();
                        } catch (SQLiteException e) {
                            Log.e("ContactsDatabaseHelper", "Cannot insert nickname: " + name, e);
                        }
                    }
                } finally {
                    nicknameLookupInsert.close();
                }
            }
        }
    }

    public static void copyStringValue(ContentValues toValues, String toKey, ContentValues fromValues, String fromKey) {
        if (fromValues.containsKey(fromKey)) {
            toValues.put(toKey, fromValues.getAsString(fromKey));
        }
    }

    public static void copyLongValue(ContentValues toValues, String toKey, ContentValues fromValues, String fromKey) {
        long longValue;
        if (fromValues.containsKey(fromKey)) {
            Object value = fromValues.get(fromKey);
            if (value instanceof Boolean) {
                longValue = ((Boolean) value).booleanValue() ? 1L : 0L;
            } else if (value instanceof String) {
                longValue = Long.parseLong((String) value);
            } else {
                longValue = ((Number) value).longValue();
            }
            toValues.put(toKey, Long.valueOf(longValue));
        }
    }

    public SyncStateContentProviderHelper getSyncState() {
        return this.mSyncState;
    }

    public String getProperty(String key, String defaultValue) {
        return getProperty(getReadableDatabase(), key, defaultValue);
    }

    public String getProperty(SQLiteDatabase db, String key, String defaultValue) {
        Cursor cursor = db.query("properties", new String[]{"property_value"}, "property_key=?", new String[]{key}, null, null, null);
        String value = null;
        try {
            if (cursor.moveToFirst()) {
                value = cursor.getString(0);
            }
            return value != null ? value : defaultValue;
        } finally {
            cursor.close();
        }
    }

    public void setProperty(String key, String value) {
        setProperty(getWritableDatabase(), key, value);
    }

    private void setProperty(SQLiteDatabase db, String key, String value) {
        ContentValues values = new ContentValues();
        values.put("property_key", key);
        values.put("property_value", value);
        db.replace("properties", null, values);
    }

    public static boolean isInProjection(String[] projection, String column) {
        if (projection == null) {
            return true;
        }
        for (String test : projection) {
            if (column.equals(test)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInProjection(String[] projection, String... columns) {
        if (projection == null) {
            return true;
        }
        if (columns.length == 1) {
            return isInProjection(projection, columns[0]);
        }
        for (String test : projection) {
            for (String column : columns) {
                if (column.equals(test)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String exceptionMessage(Uri uri) {
        return exceptionMessage(null, uri);
    }

    public String exceptionMessage(String message, Uri uri) {
        StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(message).append("; ");
        }
        sb.append("URI: ").append(uri);
        PackageManager pm = this.mContext.getPackageManager();
        int callingUid = Binder.getCallingUid();
        sb.append(", calling user: ");
        Object nameForUid = pm.getNameForUid(callingUid);
        if (nameForUid == null) {
            nameForUid = Integer.valueOf(callingUid);
        }
        sb.append(nameForUid);
        String[] callerPackages = pm.getPackagesForUid(callingUid);
        if (callerPackages != null && callerPackages.length > 0) {
            if (callerPackages.length == 1) {
                sb.append(", calling package:");
                sb.append(callerPackages[0]);
            } else {
                sb.append(", calling package is one of: [");
                for (int i = 0; i < callerPackages.length; i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append(callerPackages[i]);
                }
                sb.append("]");
            }
        }
        return sb.toString();
    }

    public void deleteStatusUpdate(long dataId) {
        if (this.mStatusUpdateDelete == null) {
            this.mStatusUpdateDelete = getWritableDatabase().compileStatement("DELETE FROM status_updates WHERE status_update_data_id=?");
        }
        this.mStatusUpdateDelete.bindLong(1, dataId);
        this.mStatusUpdateDelete.execute();
    }

    public void replaceStatusUpdate(Long dataId, long timestamp, String status, String resPackage, Integer iconResource, Integer labelResource) {
        if (this.mStatusUpdateReplace == null) {
            this.mStatusUpdateReplace = getWritableDatabase().compileStatement("INSERT OR REPLACE INTO status_updates(status_update_data_id, status_ts,status,status_res_package,status_icon,status_label) VALUES (?,?,?,?,?,?)");
        }
        this.mStatusUpdateReplace.bindLong(1, dataId.longValue());
        this.mStatusUpdateReplace.bindLong(2, timestamp);
        bindString(this.mStatusUpdateReplace, 3, status);
        bindString(this.mStatusUpdateReplace, 4, resPackage);
        bindLong(this.mStatusUpdateReplace, 5, iconResource);
        bindLong(this.mStatusUpdateReplace, 6, labelResource);
        this.mStatusUpdateReplace.execute();
    }

    public void insertStatusUpdate(Long dataId, String status, String resPackage, Integer iconResource, Integer labelResource) {
        if (this.mStatusUpdateInsert == null) {
            this.mStatusUpdateInsert = getWritableDatabase().compileStatement("INSERT INTO status_updates(status_update_data_id, status,status_res_package,status_icon,status_label) VALUES (?,?,?,?,?)");
        }
        try {
            this.mStatusUpdateInsert.bindLong(1, dataId.longValue());
            bindString(this.mStatusUpdateInsert, 2, status);
            bindString(this.mStatusUpdateInsert, 3, resPackage);
            bindLong(this.mStatusUpdateInsert, 4, iconResource);
            bindLong(this.mStatusUpdateInsert, 5, labelResource);
            this.mStatusUpdateInsert.executeInsert();
        } catch (SQLiteConstraintException e) {
            if (this.mStatusUpdateAutoTimestamp == null) {
                this.mStatusUpdateAutoTimestamp = getWritableDatabase().compileStatement("UPDATE status_updates SET status_ts=?,status=? WHERE status_update_data_id=? AND status!=?");
            }
            long timestamp = System.currentTimeMillis();
            this.mStatusUpdateAutoTimestamp.bindLong(1, timestamp);
            bindString(this.mStatusUpdateAutoTimestamp, 2, status);
            this.mStatusUpdateAutoTimestamp.bindLong(3, dataId.longValue());
            bindString(this.mStatusUpdateAutoTimestamp, 4, status);
            this.mStatusUpdateAutoTimestamp.execute();
            if (this.mStatusAttributionUpdate == null) {
                this.mStatusAttributionUpdate = getWritableDatabase().compileStatement("UPDATE status_updates SET status_res_package=?,status_icon=?,status_label=? WHERE status_update_data_id=?");
            }
            bindString(this.mStatusAttributionUpdate, 1, resPackage);
            bindLong(this.mStatusAttributionUpdate, 2, iconResource);
            bindLong(this.mStatusAttributionUpdate, 3, labelResource);
            this.mStatusAttributionUpdate.bindLong(4, dataId.longValue());
            this.mStatusAttributionUpdate.execute();
        }
    }

    public void resetNameVerifiedForOtherRawContacts(long rawContactId) {
        if (this.mResetNameVerifiedForOtherRawContacts == null) {
            this.mResetNameVerifiedForOtherRawContacts = getWritableDatabase().compileStatement("UPDATE raw_contacts SET name_verified=0 WHERE contact_id=(SELECT contact_id FROM raw_contacts WHERE _id=?) AND _id!=?");
        }
        this.mResetNameVerifiedForOtherRawContacts.bindLong(1, rawContactId);
        this.mResetNameVerifiedForOtherRawContacts.bindLong(2, rawContactId);
        this.mResetNameVerifiedForOtherRawContacts.execute();
    }

    public void updateRawContactDisplayName(SQLiteDatabase db, long rawContactId) throws Throwable {
        String displayNameAlternative;
        String displayNamePrimary;
        String sortNameAlternative;
        String sortNamePrimary;
        String bestDisplayName;
        NameSplitter.Name name;
        if (this.mNameSplitter == null) {
            createNameSplitter();
        }
        int bestDisplayNameSource = 0;
        NameSplitter.Name bestName = null;
        String bestPhoneticName = null;
        int bestPhoneticNameStyle = 0;
        this.mSelectionArgs1[0] = String.valueOf(rawContactId);
        Cursor c = db.rawQuery("SELECT mimetype_id,is_primary,data1,data2,data3,data4,data5,data6,data7,data8,data9,data10,data11 FROM data WHERE raw_contact_id=? AND (data1 NOT NULL OR data8 NOT NULL OR data9 NOT NULL OR data10 NOT NULL OR data4 NOT NULL)", this.mSelectionArgs1);
        String bestDisplayName2 = null;
        while (c.moveToNext()) {
            try {
                int mimeType = c.getInt(0);
                int source = getDisplayNameSourceForMimeTypeId(mimeType);
                if (source >= bestDisplayNameSource && source != 0 && (source != bestDisplayNameSource || c.getInt(1) != 0)) {
                    if (mimeType == getMimeTypeIdForStructuredName()) {
                        if (bestName != null) {
                            name = new NameSplitter.Name();
                        } else {
                            name = this.mName;
                            name.clear();
                        }
                        name.prefix = c.getString(5);
                        name.givenNames = c.getString(3);
                        name.middleName = c.getString(6);
                        name.familyName = c.getString(4);
                        name.suffix = c.getString(7);
                        name.fullNameStyle = c.isNull(11) ? 0 : c.getInt(11);
                        name.phoneticFamilyName = c.getString(10);
                        name.phoneticMiddleName = c.getString(9);
                        name.phoneticGivenName = c.getString(8);
                        name.phoneticNameStyle = c.isNull(12) ? 0 : c.getInt(12);
                        if (!name.isEmpty()) {
                            bestDisplayNameSource = source;
                            bestName = name;
                        }
                        bestDisplayName = bestDisplayName2;
                    } else if (mimeType == getMimeTypeIdForOrganization()) {
                        this.mCharArrayBuffer.sizeCopied = 0;
                        c.copyStringToBuffer(2, this.mCharArrayBuffer);
                        if (this.mCharArrayBuffer.sizeCopied != 0) {
                            bestDisplayNameSource = source;
                            bestDisplayName = new String(this.mCharArrayBuffer.data, 0, this.mCharArrayBuffer.sizeCopied);
                            try {
                                bestPhoneticName = c.getString(9);
                                bestPhoneticNameStyle = c.isNull(11) ? 0 : c.getInt(11);
                            } catch (Throwable th) {
                                th = th;
                                c.close();
                                throw th;
                            }
                        } else {
                            c.copyStringToBuffer(5, this.mCharArrayBuffer);
                            if (this.mCharArrayBuffer.sizeCopied != 0) {
                                bestDisplayNameSource = source;
                                bestDisplayName = new String(this.mCharArrayBuffer.data, 0, this.mCharArrayBuffer.sizeCopied);
                                bestPhoneticName = null;
                                bestPhoneticNameStyle = 0;
                            } else {
                                bestDisplayName = bestDisplayName2;
                            }
                        }
                    } else {
                        this.mCharArrayBuffer.sizeCopied = 0;
                        c.copyStringToBuffer(2, this.mCharArrayBuffer);
                        if (this.mCharArrayBuffer.sizeCopied != 0) {
                            bestDisplayNameSource = source;
                            bestDisplayName = new String(this.mCharArrayBuffer.data, 0, this.mCharArrayBuffer.sizeCopied);
                            bestPhoneticName = null;
                            bestPhoneticNameStyle = 0;
                        }
                    }
                    bestDisplayName2 = bestDisplayName;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        c.close();
        String sortKeyPrimary = null;
        String sortKeyAlternative = null;
        int displayNameStyle = 0;
        if (bestDisplayNameSource == 40) {
            displayNameStyle = bestName.fullNameStyle;
            if (displayNameStyle == 2 || displayNameStyle == 0) {
                displayNameStyle = this.mNameSplitter.getAdjustedFullNameStyle(displayNameStyle);
                bestName.fullNameStyle = displayNameStyle;
            }
            displayNamePrimary = this.mNameSplitter.join(bestName, true, true);
            displayNameAlternative = this.mNameSplitter.join(bestName, false, true);
            if (TextUtils.isEmpty(bestName.prefix)) {
                sortNamePrimary = displayNamePrimary;
                sortNameAlternative = displayNameAlternative;
            } else {
                sortNamePrimary = this.mNameSplitter.join(bestName, true, false);
                sortNameAlternative = this.mNameSplitter.join(bestName, false, false);
            }
            bestPhoneticName = this.mNameSplitter.joinPhoneticName(bestName);
            bestPhoneticNameStyle = bestName.phoneticNameStyle;
        } else {
            displayNameAlternative = bestDisplayName2;
            displayNamePrimary = bestDisplayName2;
            sortNameAlternative = bestDisplayName2;
            sortNamePrimary = bestDisplayName2;
        }
        if (bestPhoneticName != null) {
            if (displayNamePrimary == null) {
                displayNamePrimary = bestPhoneticName;
            }
            if (displayNameAlternative == null) {
                displayNameAlternative = bestPhoneticName;
            }
            sortKeyAlternative = bestPhoneticName;
            sortKeyPrimary = bestPhoneticName;
            if (bestPhoneticNameStyle == 0) {
                bestPhoneticNameStyle = this.mNameSplitter.guessPhoneticNameStyle(bestPhoneticName);
            }
        } else {
            bestPhoneticNameStyle = 0;
            if (displayNameStyle == 0) {
                int displayNameStyle2 = this.mNameSplitter.guessFullNameStyle(bestDisplayName2);
                if (displayNameStyle2 == 0 || displayNameStyle2 == 2) {
                    displayNameStyle2 = this.mNameSplitter.getAdjustedNameStyleBasedOnPhoneticNameStyle(displayNameStyle2, 0);
                }
                displayNameStyle = this.mNameSplitter.getAdjustedFullNameStyle(displayNameStyle2);
            }
            if (displayNameStyle == 3 || displayNameStyle == 2) {
                sortKeyAlternative = sortNamePrimary;
                sortKeyPrimary = sortNamePrimary;
            }
        }
        if (sortKeyPrimary == null) {
            sortKeyPrimary = sortNamePrimary;
            sortKeyAlternative = sortNameAlternative;
        }
        String phonebookLabelPrimary = "";
        String phonebookLabelAlternative = "";
        int phonebookBucketPrimary = 0;
        int phonebookBucketAlternative = 0;
        ContactLocaleUtils localeUtils = ContactLocaleUtils.getInstance();
        if (sortKeyPrimary != null) {
            phonebookBucketPrimary = localeUtils.getBucketIndex(sortKeyPrimary);
            phonebookLabelPrimary = localeUtils.getBucketLabel(phonebookBucketPrimary);
        }
        if (sortKeyAlternative != null) {
            phonebookBucketAlternative = localeUtils.getBucketIndex(sortKeyAlternative);
            phonebookLabelAlternative = localeUtils.getBucketLabel(phonebookBucketAlternative);
        }
        if (this.mRawContactDisplayNameUpdate == null) {
            this.mRawContactDisplayNameUpdate = db.compileStatement("UPDATE raw_contacts SET display_name_source=?,display_name=?,display_name_alt=?,phonetic_name=?,phonetic_name_style=?,sort_key=?,phonebook_label=?,phonebook_bucket=?,sort_key_alt=?,phonebook_label_alt=?,phonebook_bucket_alt=? WHERE _id=?");
        }
        this.mRawContactDisplayNameUpdate.bindLong(1, bestDisplayNameSource);
        bindString(this.mRawContactDisplayNameUpdate, 2, displayNamePrimary);
        bindString(this.mRawContactDisplayNameUpdate, 3, displayNameAlternative);
        bindString(this.mRawContactDisplayNameUpdate, 4, bestPhoneticName);
        this.mRawContactDisplayNameUpdate.bindLong(5, bestPhoneticNameStyle);
        bindString(this.mRawContactDisplayNameUpdate, 6, sortKeyPrimary);
        bindString(this.mRawContactDisplayNameUpdate, 7, phonebookLabelPrimary);
        this.mRawContactDisplayNameUpdate.bindLong(8, phonebookBucketPrimary);
        bindString(this.mRawContactDisplayNameUpdate, 9, sortKeyAlternative);
        bindString(this.mRawContactDisplayNameUpdate, 10, phonebookLabelAlternative);
        this.mRawContactDisplayNameUpdate.bindLong(11, phonebookBucketAlternative);
        this.mRawContactDisplayNameUpdate.bindLong(12, rawContactId);
        this.mRawContactDisplayNameUpdate.execute();
    }

    public void setIsPrimary(long rawContactId, long dataId, long mimeTypeId) {
        if (this.mSetPrimaryStatement == null) {
            this.mSetPrimaryStatement = getWritableDatabase().compileStatement("UPDATE data SET is_primary=(_id=?) WHERE mimetype_id=?   AND raw_contact_id=?");
        }
        this.mSetPrimaryStatement.bindLong(1, dataId);
        this.mSetPrimaryStatement.bindLong(2, mimeTypeId);
        this.mSetPrimaryStatement.bindLong(3, rawContactId);
        this.mSetPrimaryStatement.execute();
    }

    public void clearSuperPrimary(long rawContactId, long mimeTypeId) {
        if (this.mClearSuperPrimaryStatement == null) {
            this.mClearSuperPrimaryStatement = getWritableDatabase().compileStatement("UPDATE data SET is_super_primary=0 WHERE mimetype_id=?   AND raw_contact_id=?");
        }
        this.mClearSuperPrimaryStatement.bindLong(1, mimeTypeId);
        this.mClearSuperPrimaryStatement.bindLong(2, rawContactId);
        this.mClearSuperPrimaryStatement.execute();
    }

    public void setIsSuperPrimary(long rawContactId, long dataId, long mimeTypeId) {
        if (this.mSetSuperPrimaryStatement == null) {
            this.mSetSuperPrimaryStatement = getWritableDatabase().compileStatement("UPDATE data SET is_super_primary=(_id=?) WHERE mimetype_id=?   AND raw_contact_id IN (SELECT _id FROM raw_contacts WHERE contact_id =(SELECT contact_id FROM raw_contacts WHERE _id=?))");
        }
        this.mSetSuperPrimaryStatement.bindLong(1, dataId);
        this.mSetSuperPrimaryStatement.bindLong(2, mimeTypeId);
        this.mSetSuperPrimaryStatement.bindLong(3, rawContactId);
        this.mSetSuperPrimaryStatement.execute();
    }

    public void insertNameLookup(long rawContactId, long dataId, int lookupType, String name) {
        if (!TextUtils.isEmpty(name)) {
            if (this.mNameLookupInsert == null) {
                this.mNameLookupInsert = getWritableDatabase().compileStatement("INSERT OR IGNORE INTO name_lookup(raw_contact_id,data_id,name_type,normalized_name) VALUES (?,?,?,?)");
            }
            this.mNameLookupInsert.bindLong(1, rawContactId);
            this.mNameLookupInsert.bindLong(2, dataId);
            this.mNameLookupInsert.bindLong(3, lookupType);
            bindString(this.mNameLookupInsert, 4, name);
            this.mNameLookupInsert.executeInsert();
        }
    }

    public void deleteNameLookup(long dataId) {
        if (this.mNameLookupDelete == null) {
            this.mNameLookupDelete = getWritableDatabase().compileStatement("DELETE FROM name_lookup WHERE data_id=?");
        }
        this.mNameLookupDelete.bindLong(1, dataId);
        this.mNameLookupDelete.execute();
    }

    public String insertNameLookupForEmail(long rawContactId, long dataId, String email) {
        String address;
        if (!TextUtils.isEmpty(email) && (address = extractHandleFromEmailAddress(email)) != null) {
            insertNameLookup(rawContactId, dataId, 4, NameNormalizer.normalize(address));
            return address;
        }
        return null;
    }

    public void insertNameLookupForNickname(long rawContactId, long dataId, String nickname) {
        if (!TextUtils.isEmpty(nickname)) {
            insertNameLookup(rawContactId, dataId, 3, NameNormalizer.normalize(nickname));
        }
    }

    public boolean rawContactHasSuperPrimary(long rawContactId, long mimeTypeId) {
        Cursor existsCursor = getReadableDatabase().rawQuery("SELECT EXISTS(SELECT 1 FROM data WHERE raw_contact_id=? AND mimetype_id=? AND is_super_primary<>0)", new String[]{String.valueOf(rawContactId), String.valueOf(mimeTypeId)});
        try {
            if (existsCursor.moveToFirst()) {
                return existsCursor.getInt(0) != 0;
            }
            throw new IllegalStateException();
        } finally {
            existsCursor.close();
        }
    }

    public String getCurrentCountryIso() {
        return this.mCountryMonitor.getCountryIso();
    }

    void setUseStrictPhoneNumberComparisonForTest(boolean useStrict) {
        this.mUseStrictPhoneNumberComparison = useStrict;
    }

    boolean getUseStrictPhoneNumberComparisonForTest() {
        return this.mUseStrictPhoneNumberComparison;
    }

    String querySearchIndexContentForTest(long contactId) {
        return DatabaseUtils.stringForQuery(getReadableDatabase(), "SELECT content FROM search_index WHERE contact_id=CAST(? AS int)", new String[]{String.valueOf(contactId)});
    }

    String querySearchIndexTokensForTest(long contactId) {
        return DatabaseUtils.stringForQuery(getReadableDatabase(), "SELECT tokens FROM search_index WHERE contact_id=CAST(? AS int)", new String[]{String.valueOf(contactId)});
    }
}
