package com.android.providers.contacts;

import android.accounts.Account;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.providers.contacts.NameSplitter;
import com.mediatek.providers.contacts.AccountUtils;
import java.util.Locale;

public class LegacyApiSupport {
    private static final ArrayMap<String, String> sContactMethodProjectionMap;
    private static final ArrayMap<String, String> sExtensionProjectionMap;
    private static final ArrayMap<String, String> sGroupMembershipProjectionMap;
    private static final ArrayMap<String, String> sGroupProjectionMap;
    private static final ArrayMap<String, String> sOrganizationProjectionMap;
    private static final ArrayMap<String, String> sPeopleProjectionMap;
    private static final ArrayMap<String, String> sPhoneProjectionMap;
    private static final ArrayMap<String, String> sPhotoProjectionMap;
    private Account mAccount;
    private final ContactsProvider2 mContactsProvider;
    private final Context mContext;
    private final SQLiteStatement mDataMimetypeQuery;
    private final SQLiteStatement mDataRawContactIdQuery;
    private final ContactsDatabaseHelper mDbHelper;
    private boolean mDefaultAccountKnown;
    private final GlobalSearchSupport mGlobalSearchSupport;
    private final long mMimetypeEmail;
    private final long mMimetypeIm;
    private final long mMimetypePostal;
    private final NameSplitter mPhoneticNameSplitter;
    private String[] mSelectionArgs1 = new String[1];
    private String[] mSelectionArgs2 = new String[2];
    private final ContentValues mValues = new ContentValues();
    private final ContentValues mValues2 = new ContentValues();
    private final ContentValues mValues3 = new ContentValues();
    private static final UriMatcher sUriMatcher = new UriMatcher(-1);
    private static String CONTACT_METHOD_DATA_SQL = "(CASE WHEN mimetype='vnd.android.cursor.item/im' THEN (CASE WHEN data.data5=-1 THEN 'custom:'||data.data6 ELSE 'pre:'||data.data5 END) ELSE data.data1 END)";
    private static final String[] ORGANIZATION_MIME_TYPES = {"vnd.android.cursor.item/organization"};
    private static final String[] CONTACT_METHOD_MIME_TYPES = {"vnd.android.cursor.item/email_v2", "vnd.android.cursor.item/im", "vnd.android.cursor.item/postal-address_v2"};
    private static final String[] PHONE_MIME_TYPES = {"vnd.android.cursor.item/phone_v2"};
    private static final String[] PHOTO_MIME_TYPES = {"vnd.android.cursor.item/photo"};
    private static final String[] GROUP_MEMBERSHIP_MIME_TYPES = {"vnd.android.cursor.item/group_membership"};
    private static final String[] EXTENSION_MIME_TYPES = {"vnd.android.cursor.item/contact_extensions"};

    private interface IdQuery {
        public static final String[] COLUMNS = {"_id"};
    }

    static {
        UriMatcher uriMatcher = sUriMatcher;
        uriMatcher.addURI("contacts", "extensions", 14);
        uriMatcher.addURI("contacts", "extensions/#", 15);
        uriMatcher.addURI("contacts", "groups", 18);
        uriMatcher.addURI("contacts", "groups/#", 19);
        uriMatcher.addURI("contacts", "groups/name/*/members", 40);
        uriMatcher.addURI("contacts", "groups/system_id/*/members", 41);
        uriMatcher.addURI("contacts", "groupmembership", 20);
        uriMatcher.addURI("contacts", "groupmembership/#", 21);
        uriMatcher.addURI("contacts", "people", 1);
        uriMatcher.addURI("contacts", "people/filter/*", 29);
        uriMatcher.addURI("contacts", "people/#", 2);
        uriMatcher.addURI("contacts", "people/#/extensions", 16);
        uriMatcher.addURI("contacts", "people/#/extensions/#", 17);
        uriMatcher.addURI("contacts", "people/#/phones", 10);
        uriMatcher.addURI("contacts", "people/#/phones/#", 11);
        uriMatcher.addURI("contacts", "people/#/photo", 24);
        uriMatcher.addURI("contacts", "people/#/contact_methods", 6);
        uriMatcher.addURI("contacts", "people/#/contact_methods/#", 7);
        uriMatcher.addURI("contacts", "people/#/organizations", 42);
        uriMatcher.addURI("contacts", "people/#/organizations/#", 43);
        uriMatcher.addURI("contacts", "people/#/groupmembership", 22);
        uriMatcher.addURI("contacts", "people/#/groupmembership/#", 23);
        uriMatcher.addURI("contacts", "people/#/update_contact_time", 3);
        uriMatcher.addURI("contacts", "deleted_people", 30);
        uriMatcher.addURI("contacts", "deleted_groups", 31);
        uriMatcher.addURI("contacts", "phones", 12);
        uriMatcher.addURI("contacts", "phones/filter/*", 34);
        uriMatcher.addURI("contacts", "phones/#", 13);
        uriMatcher.addURI("contacts", "photos", 25);
        uriMatcher.addURI("contacts", "photos/#", 26);
        uriMatcher.addURI("contacts", "contact_methods", 8);
        uriMatcher.addURI("contacts", "contact_methods/email", 39);
        uriMatcher.addURI("contacts", "contact_methods/#", 9);
        uriMatcher.addURI("contacts", "organizations", 4);
        uriMatcher.addURI("contacts", "organizations/#", 5);
        uriMatcher.addURI("contacts", "search_suggest_query", 32);
        uriMatcher.addURI("contacts", "search_suggest_query/*", 32);
        uriMatcher.addURI("contacts", "search_suggest_shortcut/*", 33);
        uriMatcher.addURI("contacts", "settings", 44);
        ArrayMap arrayMap = new ArrayMap();
        arrayMap.put("name", "name");
        arrayMap.put("display_name", "display_name");
        arrayMap.put("phonetic_name", "phonetic_name");
        arrayMap.put("notes", "notes");
        arrayMap.put("times_contacted", "times_contacted");
        arrayMap.put("last_time_contacted", "last_time_contacted");
        arrayMap.put("custom_ringtone", "custom_ringtone");
        arrayMap.put("send_to_voicemail", "send_to_voicemail");
        arrayMap.put("starred", "starred");
        arrayMap.put("primary_organization", "primary_organization");
        arrayMap.put("primary_email", "primary_email");
        arrayMap.put("primary_phone", "primary_phone");
        sPeopleProjectionMap = new ArrayMap<>(arrayMap);
        sPeopleProjectionMap.put("_id", "_id");
        sPeopleProjectionMap.put("number", "number");
        sPeopleProjectionMap.put("type", "type");
        sPeopleProjectionMap.put("label", "label");
        sPeopleProjectionMap.put("number_key", "number_key");
        sPeopleProjectionMap.put("im_protocol", "(CASE WHEN protocol=-1 THEN 'custom:'||custom_protocol ELSE 'pre:'||protocol END) AS im_protocol");
        sPeopleProjectionMap.put("im_handle", "im_handle");
        sPeopleProjectionMap.put("im_account", "im_account");
        sPeopleProjectionMap.put("mode", "mode");
        sPeopleProjectionMap.put("status", "(SELECT status FROM status_updates JOIN data   ON(status_update_data_id=data._id) WHERE data.raw_contact_id=people._id ORDER BY status_ts DESC  LIMIT 1) AS status");
        sOrganizationProjectionMap = new ArrayMap<>();
        sOrganizationProjectionMap.put("_id", "_id");
        sOrganizationProjectionMap.put("person", "person");
        sOrganizationProjectionMap.put("isprimary", "isprimary");
        sOrganizationProjectionMap.put("company", "company");
        sOrganizationProjectionMap.put("type", "type");
        sOrganizationProjectionMap.put("label", "label");
        sOrganizationProjectionMap.put("title", "title");
        sContactMethodProjectionMap = new ArrayMap<>(arrayMap);
        sContactMethodProjectionMap.put("_id", "_id");
        sContactMethodProjectionMap.put("person", "person");
        sContactMethodProjectionMap.put("kind", "kind");
        sContactMethodProjectionMap.put("isprimary", "isprimary");
        sContactMethodProjectionMap.put("type", "type");
        sContactMethodProjectionMap.put("data", "data");
        sContactMethodProjectionMap.put("label", "label");
        sContactMethodProjectionMap.put("aux_data", "aux_data");
        sPhoneProjectionMap = new ArrayMap<>(arrayMap);
        sPhoneProjectionMap.put("_id", "_id");
        sPhoneProjectionMap.put("person", "person");
        sPhoneProjectionMap.put("isprimary", "isprimary");
        sPhoneProjectionMap.put("number", "number");
        sPhoneProjectionMap.put("type", "type");
        sPhoneProjectionMap.put("label", "label");
        sPhoneProjectionMap.put("number_key", "number_key");
        sExtensionProjectionMap = new ArrayMap<>();
        sExtensionProjectionMap.put("_id", "_id");
        sExtensionProjectionMap.put("person", "person");
        sExtensionProjectionMap.put("name", "name");
        sExtensionProjectionMap.put("value", "value");
        sGroupProjectionMap = new ArrayMap<>();
        sGroupProjectionMap.put("_id", "_id");
        sGroupProjectionMap.put("name", "name");
        sGroupProjectionMap.put("notes", "notes");
        sGroupProjectionMap.put("system_id", "system_id");
        sGroupMembershipProjectionMap = new ArrayMap<>(sGroupProjectionMap);
        sGroupMembershipProjectionMap.put("_id", "_id");
        sGroupMembershipProjectionMap.put("person", "person");
        sGroupMembershipProjectionMap.put("group_id", "group_id");
        sGroupMembershipProjectionMap.put("group_sync_id", "group_sync_id");
        sGroupMembershipProjectionMap.put("group_sync_account", "group_sync_account");
        sGroupMembershipProjectionMap.put("group_sync_account_type", "group_sync_account_type");
        sPhotoProjectionMap = new ArrayMap<>();
        sPhotoProjectionMap.put("_id", "_id");
        sPhotoProjectionMap.put("person", "person");
        sPhotoProjectionMap.put("data", "data");
        sPhotoProjectionMap.put("local_version", "local_version");
        sPhotoProjectionMap.put("download_required", "download_required");
        sPhotoProjectionMap.put("exists_on_server", "exists_on_server");
        sPhotoProjectionMap.put("sync_error", "sync_error");
    }

    public LegacyApiSupport(Context context, ContactsDatabaseHelper contactsDatabaseHelper, ContactsProvider2 contactsProvider2, GlobalSearchSupport globalSearchSupport) {
        this.mContext = context;
        this.mContactsProvider = contactsProvider2;
        this.mDbHelper = contactsDatabaseHelper;
        this.mGlobalSearchSupport = globalSearchSupport;
        this.mPhoneticNameSplitter = new NameSplitter("", "", "", context.getString(android.R.string.accessibility_shortcut_single_service_warning_title), Locale.getDefault());
        SQLiteDatabase readableDatabase = this.mDbHelper.getReadableDatabase();
        this.mDataMimetypeQuery = readableDatabase.compileStatement("SELECT mimetype_id FROM data WHERE _id=?");
        this.mDataRawContactIdQuery = readableDatabase.compileStatement("SELECT raw_contact_id FROM data WHERE _id=?");
        this.mMimetypeEmail = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/email_v2");
        this.mMimetypeIm = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/im");
        this.mMimetypePostal = this.mDbHelper.getMimeTypeId("vnd.android.cursor.item/postal-address_v2");
    }

    private void ensureDefaultAccount() {
        if (!this.mDefaultAccountKnown) {
            this.mAccount = this.mContactsProvider.getDefaultAccount();
            this.mDefaultAccountKnown = true;
        }
    }

    public static void createDatabase(SQLiteDatabase sQLiteDatabase) {
        Log.i("ContactsProviderV1", "Bootstrapping database legacy support");
        createViews(sQLiteDatabase);
        createSettingsTable(sQLiteDatabase);
    }

    public static void createViews(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_v1_people;");
        sQLiteDatabase.execSQL("CREATE VIEW view_v1_people AS SELECT raw_contacts._id AS _id, name.data1 AS name, raw_contacts.display_name AS display_name, trim(trim(ifnull(name.data7,' ')||' '||ifnull(name.data8,' '))||' '||ifnull(name.data9,' '))  AS phonetic_name , note.data1 AS notes, accounts.account_name, accounts.account_type, cast(0 as int) AS times_contacted, cast(0 as int) AS last_time_contacted, raw_contacts.custom_ringtone AS custom_ringtone, raw_contacts.send_to_voicemail AS send_to_voicemail, raw_contacts.starred AS starred, organization._id AS primary_organization, email._id AS primary_email, phone._id AS primary_phone, phone.data1 AS number, phone.data2 AS type, phone.data3 AS label, _PHONE_NUMBER_STRIPPED_REVERSED(phone.data1) AS number_key FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id) LEFT OUTER JOIN data name ON (raw_contacts._id = name.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = name.mimetype_id)='vnd.android.cursor.item/name') LEFT OUTER JOIN data organization ON (raw_contacts._id = organization.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = organization.mimetype_id)='vnd.android.cursor.item/organization' AND organization.is_primary) LEFT OUTER JOIN data email ON (raw_contacts._id = email.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = email.mimetype_id)='vnd.android.cursor.item/email_v2' AND email.is_primary) LEFT OUTER JOIN data note ON (raw_contacts._id = note.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = note.mimetype_id)='vnd.android.cursor.item/note') LEFT OUTER JOIN data phone ON (raw_contacts._id = phone.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = phone.mimetype_id)='vnd.android.cursor.item/phone_v2' AND phone.is_primary) WHERE raw_contacts.deleted=0;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_v1_organizations;");
        sQLiteDatabase.execSQL("CREATE VIEW view_v1_organizations AS SELECT data._id AS _id, raw_contact_id AS person, is_primary AS isprimary, accounts.account_name, accounts.account_type, data1 AS company, data2 AS type, data3 AS label, data4 AS title FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) JOIN accounts ON (raw_contacts.account_id=accounts._id) WHERE mimetypes.mimetype='vnd.android.cursor.item/organization' AND raw_contacts.deleted=0;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_v1_contact_methods;");
        sQLiteDatabase.execSQL("CREATE VIEW view_v1_contact_methods AS SELECT data._id AS _id, data.raw_contact_id AS person, CAST ((CASE WHEN mimetype='vnd.android.cursor.item/email_v2' THEN 1 ELSE (CASE WHEN mimetype='vnd.android.cursor.item/im' THEN 3 ELSE (CASE WHEN mimetype='vnd.android.cursor.item/postal-address_v2' THEN 2 ELSE NULL END) END) END) AS INTEGER) AS kind, data.is_primary AS isprimary, data.data2 AS type, " + CONTACT_METHOD_DATA_SQL + " AS data, data.data3 AS label, data.data14 AS aux_data, name.data1 AS name, raw_contacts.display_name AS display_name, trim(trim(ifnull(name.data7,' ')||' '||ifnull(name.data8,' '))||' '||ifnull(name.data9,' '))  AS phonetic_name , note.data1 AS notes, accounts.account_name, accounts.account_type, cast(0 as int) AS times_contacted, cast(0 as int) AS last_time_contacted, raw_contacts.custom_ringtone AS custom_ringtone, raw_contacts.send_to_voicemail AS send_to_voicemail, raw_contacts.starred AS starred, organization._id AS primary_organization, email._id AS primary_email, phone._id AS primary_phone, phone.data1 AS number, phone.data2 AS type, phone.data3 AS label, _PHONE_NUMBER_STRIPPED_REVERSED(phone.data1) AS number_key FROM data JOIN mimetypes ON (mimetypes._id = data.mimetype_id) JOIN raw_contacts ON (raw_contacts._id = data.raw_contact_id) JOIN accounts ON (raw_contacts.account_id=accounts._id) LEFT OUTER JOIN data name ON (raw_contacts._id = name.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = name.mimetype_id)='vnd.android.cursor.item/name') LEFT OUTER JOIN data organization ON (raw_contacts._id = organization.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = organization.mimetype_id)='vnd.android.cursor.item/organization' AND organization.is_primary) LEFT OUTER JOIN data email ON (raw_contacts._id = email.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = email.mimetype_id)='vnd.android.cursor.item/email_v2' AND email.is_primary) LEFT OUTER JOIN data note ON (raw_contacts._id = note.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = note.mimetype_id)='vnd.android.cursor.item/note') LEFT OUTER JOIN data phone ON (raw_contacts._id = phone.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = phone.mimetype_id)='vnd.android.cursor.item/phone_v2' AND phone.is_primary) WHERE kind IS NOT NULL AND raw_contacts.deleted=0;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_v1_phones;");
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE VIEW view_v1_phones AS SELECT DISTINCT data._id AS _id, data.raw_contact_id AS person, data.is_primary AS isprimary, data.data1 AS number, data.data2 AS type, data.data3 AS label, _PHONE_NUMBER_STRIPPED_REVERSED(data.data1) AS number_key, ");
        sb.append("name.data1 AS name, raw_contacts.display_name AS display_name, trim(trim(ifnull(name.data7,' ')||' '||ifnull(name.data8,' '))||' '||ifnull(name.data9,' '))  AS phonetic_name , note.data1 AS notes, accounts.account_name, accounts.account_type, cast(0 as int) AS times_contacted, cast(0 as int) AS last_time_contacted, raw_contacts.custom_ringtone AS custom_ringtone, raw_contacts.send_to_voicemail AS send_to_voicemail, raw_contacts.starred AS starred, organization._id AS primary_organization, email._id AS primary_email, phone._id AS primary_phone, phone.data1 AS number, phone.data2 AS type, phone.data3 AS label, _PHONE_NUMBER_STRIPPED_REVERSED(phone.data1) AS number_key");
        sb.append(" FROM ");
        sb.append("data");
        sb.append(" JOIN ");
        sb.append("phone_lookup");
        sb.append(" ON (");
        sb.append("data");
        sb.append("._id = ");
        sb.append("phone_lookup");
        sb.append(".");
        sb.append("data_id");
        sb.append(")");
        sb.append(" JOIN mimetypes ON (mimetypes._id = data.mimetype_id) JOIN raw_contacts ON (raw_contacts._id = data.raw_contact_id) JOIN accounts ON (raw_contacts.account_id=accounts._id) LEFT OUTER JOIN data name ON (raw_contacts._id = name.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = name.mimetype_id)='vnd.android.cursor.item/name') LEFT OUTER JOIN data organization ON (raw_contacts._id = organization.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = organization.mimetype_id)='vnd.android.cursor.item/organization' AND organization.is_primary) LEFT OUTER JOIN data email ON (raw_contacts._id = email.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = email.mimetype_id)='vnd.android.cursor.item/email_v2' AND email.is_primary) LEFT OUTER JOIN data note ON (raw_contacts._id = note.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = note.mimetype_id)='vnd.android.cursor.item/note') LEFT OUTER JOIN data phone ON (raw_contacts._id = phone.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = phone.mimetype_id)='vnd.android.cursor.item/phone_v2' AND phone.is_primary)");
        sb.append(" WHERE ");
        sb.append("mimetypes.mimetype");
        sb.append("='");
        sb.append("vnd.android.cursor.item/phone_v2");
        sb.append("' AND ");
        sb.append("raw_contacts");
        sb.append(".");
        sb.append("deleted");
        sb.append("=0;");
        sQLiteDatabase.execSQL(sb.toString());
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_v1_extensions;");
        sQLiteDatabase.execSQL("CREATE VIEW view_v1_extensions AS SELECT data._id AS _id, data.raw_contact_id AS person, accounts.account_name, accounts.account_type, data1 AS name, data2 AS value FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) JOIN accounts ON (raw_contacts.account_id=accounts._id) WHERE mimetypes.mimetype='vnd.android.cursor.item/contact_extensions' AND raw_contacts.deleted=0;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_v1_groups;");
        sQLiteDatabase.execSQL("CREATE VIEW view_v1_groups AS SELECT groups._id AS _id, accounts.account_name, accounts.account_type, title AS name, notes AS notes , system_id AS system_id FROM groups JOIN accounts ON (groups.account_id=accounts._id);");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_v1_group_membership;");
        sQLiteDatabase.execSQL("CREATE VIEW view_v1_group_membership AS SELECT data._id AS _id, data.raw_contact_id AS person, accounts.account_name, accounts.account_type, data1 AS group_id, title AS name, notes AS notes, system_id AS system_id, groups.sourceid AS group_sync_id, accounts.account_name AS group_sync_account, accounts.account_type AS group_sync_account_type FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id)  JOIN accounts ON (raw_contacts.account_id=accounts._id)LEFT OUTER JOIN packages ON (data.package_id = packages._id) LEFT OUTER JOIN groups   ON (mimetypes.mimetype='vnd.android.cursor.item/group_membership'       AND groups._id = data.data1)  WHERE mimetypes.mimetype='vnd.android.cursor.item/group_membership' AND raw_contacts.deleted=0;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_v1_photos;");
        sQLiteDatabase.execSQL("CREATE VIEW view_v1_photos AS SELECT data._id AS _id, data.raw_contact_id AS person, accounts.account_name, accounts.account_type, data.data15 AS data, legacy_photo.data4 AS exists_on_server, legacy_photo.data3 AS download_required, legacy_photo.data2 AS local_version, legacy_photo.data5 AS sync_error FROM data JOIN mimetypes ON (mimetypes._id = data.mimetype_id) JOIN raw_contacts ON (raw_contacts._id = data.raw_contact_id) JOIN accounts ON (raw_contacts.account_id=accounts._id) LEFT OUTER JOIN data name ON (raw_contacts._id = name.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = name.mimetype_id)='vnd.android.cursor.item/name') LEFT OUTER JOIN data organization ON (raw_contacts._id = organization.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = organization.mimetype_id)='vnd.android.cursor.item/organization' AND organization.is_primary) LEFT OUTER JOIN data email ON (raw_contacts._id = email.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = email.mimetype_id)='vnd.android.cursor.item/email_v2' AND email.is_primary) LEFT OUTER JOIN data note ON (raw_contacts._id = note.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = note.mimetype_id)='vnd.android.cursor.item/note') LEFT OUTER JOIN data phone ON (raw_contacts._id = phone.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = phone.mimetype_id)='vnd.android.cursor.item/phone_v2' AND phone.is_primary) LEFT OUTER JOIN data legacy_photo ON (raw_contacts._id = legacy_photo.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = legacy_photo.mimetype_id)='vnd.android.cursor.item/photo_v1_extras' AND data._id = legacy_photo.data1) WHERE mimetypes.mimetype='vnd.android.cursor.item/photo' AND raw_contacts.deleted=0;");
    }

    public static void createSettingsTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS v1_settings;");
        sQLiteDatabase.execSQL("CREATE TABLE v1_settings (_id INTEGER PRIMARY KEY,_sync_account TEXT,_sync_account_type TEXT,key STRING NOT NULL,value STRING );");
    }

    public Uri insert(Uri uri, ContentValues contentValues) throws Throwable {
        long jInsertPeople;
        ensureDefaultAccount();
        int iMatch = sUriMatcher.match(uri);
        if (iMatch == 1) {
            jInsertPeople = insertPeople(contentValues);
        } else if (iMatch == 4) {
            jInsertPeople = insertOrganization(contentValues);
        } else if (iMatch == 6) {
            jInsertPeople = insertContactMethod(Long.parseLong(uri.getPathSegments().get(1)), contentValues);
        } else if (iMatch == 8) {
            jInsertPeople = insertContactMethod(getRequiredValue(contentValues, "person"), contentValues);
        } else if (iMatch == 10) {
            jInsertPeople = insertPhone(Long.parseLong(uri.getPathSegments().get(1)), contentValues);
        } else if (iMatch == 12) {
            jInsertPeople = insertPhone(getRequiredValue(contentValues, "person"), contentValues);
        } else if (iMatch == 14) {
            jInsertPeople = insertExtension(getRequiredValue(contentValues, "person"), contentValues);
        } else if (iMatch == 18) {
            jInsertPeople = insertGroup(contentValues);
        } else if (iMatch == 20) {
            jInsertPeople = insertGroupMembership(getRequiredValue(contentValues, "person"), getRequiredValue(contentValues, "group_id"));
        } else {
            throw new UnsupportedOperationException(this.mDbHelper.exceptionMessage(uri));
        }
        if (jInsertPeople < 0) {
            return null;
        }
        Uri uriWithAppendedId = ContentUris.withAppendedId(uri, jInsertPeople);
        onChange(uriWithAppendedId);
        return uriWithAppendedId;
    }

    private long getRequiredValue(ContentValues contentValues, String str) {
        Long asLong = contentValues.getAsLong(str);
        if (asLong == null) {
            throw new RuntimeException("Required value: " + str);
        }
        return asLong.longValue();
    }

    private long insertPeople(ContentValues contentValues) throws Throwable {
        parsePeopleValues(contentValues);
        long id = ContentUris.parseId(this.mContactsProvider.insertInTransaction(ContactsContract.RawContacts.CONTENT_URI, this.mValues));
        if (this.mValues2.size() != 0) {
            this.mValues2.put("raw_contact_id", Long.valueOf(id));
            this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues2);
        }
        if (this.mValues3.size() != 0) {
            this.mValues3.put("raw_contact_id", Long.valueOf(id));
            this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues3);
        }
        return id;
    }

    private long insertOrganization(ContentValues contentValues) {
        parseOrganizationValues(contentValues);
        ContactsDatabaseHelper.copyLongValue(this.mValues, "raw_contact_id", contentValues, "person");
        return ContentUris.parseId(this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues));
    }

    private long insertPhone(long j, ContentValues contentValues) {
        parsePhoneValues(contentValues);
        this.mValues.put("raw_contact_id", Long.valueOf(j));
        return ContentUris.parseId(this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues));
    }

    private long insertContactMethod(long j, ContentValues contentValues) {
        Integer asInteger = contentValues.getAsInteger("kind");
        if (asInteger == null) {
            throw new RuntimeException("Required value: kind");
        }
        parseContactMethodValues(asInteger.intValue(), contentValues);
        this.mValues.put("raw_contact_id", Long.valueOf(j));
        return ContentUris.parseId(this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues));
    }

    private long insertExtension(long j, ContentValues contentValues) {
        this.mValues.clear();
        this.mValues.put("raw_contact_id", Long.valueOf(j));
        this.mValues.put("mimetype", "vnd.android.cursor.item/contact_extensions");
        parseExtensionValues(contentValues);
        return ContentUris.parseId(this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues));
    }

    private long insertGroup(ContentValues contentValues) {
        parseGroupValues(contentValues);
        if (this.mAccount != null) {
            this.mValues.put("account_name", this.mAccount.name);
            this.mValues.put("account_type", this.mAccount.type);
        }
        return ContentUris.parseId(this.mContactsProvider.insertInTransaction(ContactsContract.Groups.CONTENT_URI, this.mValues));
    }

    private long insertGroupMembership(long j, long j2) {
        this.mValues.clear();
        this.mValues.put("mimetype", "vnd.android.cursor.item/group_membership");
        this.mValues.put("raw_contact_id", Long.valueOf(j));
        this.mValues.put("data1", Long.valueOf(j2));
        return ContentUris.parseId(this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues));
    }

    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int iUpdateAll;
        ensureDefaultAccount();
        int iMatch = sUriMatcher.match(uri);
        if (iMatch != -1) {
            if (iMatch == 3) {
                iUpdateAll = 0;
            } else {
                if (iMatch == 24) {
                    return updatePhoto(Long.parseLong(uri.getPathSegments().get(1)), contentValues);
                }
                if (iMatch == 44) {
                    return updateSettings(contentValues);
                }
                switch (iMatch) {
                    case 20:
                    case 21:
                        break;
                    default:
                        iUpdateAll = updateAll(uri, iMatch, contentValues, str, strArr);
                        break;
                }
            }
            if (iUpdateAll > 0) {
                this.mContext.getContentResolver().notifyChange(uri, null);
            }
            return iUpdateAll;
        }
        throw new UnsupportedOperationException(this.mDbHelper.exceptionMessage(uri));
    }

    private int updateAll(Uri uri, int i, ContentValues contentValues, String str, String[] strArr) {
        Cursor cursorQuery = query(uri, IdQuery.COLUMNS, str, strArr, null, null);
        if (cursorQuery == null) {
            return 0;
        }
        int iUpdate = 0;
        while (cursorQuery.moveToNext()) {
            try {
                iUpdate += update(i, cursorQuery.getLong(0), contentValues);
            } finally {
                cursorQuery.close();
            }
        }
        return iUpdate;
    }

    public int update(int i, long j, ContentValues contentValues) {
        switch (i) {
            case 1:
            case 2:
                return updatePeople(j, contentValues);
            case 3:
            case 6:
            case 7:
            case 10:
            case 11:
            case 16:
            case 17:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            default:
                return 0;
            case 4:
            case 5:
                return updateOrganizations(j, contentValues);
            case 8:
            case 9:
                return updateContactMethods(j, contentValues);
            case 12:
            case 13:
                return updatePhones(j, contentValues);
            case 14:
            case 15:
                return updateExtensions(j, contentValues);
            case 18:
            case 19:
                return updateGroups(j, contentValues);
            case 25:
            case 26:
                return updatePhotoByDataId(j, contentValues);
        }
    }

    private int updatePeople(long j, ContentValues contentValues) throws Throwable {
        parsePeopleValues(contentValues);
        int iUpdateInTransaction = this.mContactsProvider.updateInTransaction(ContactsContract.RawContacts.CONTENT_URI, this.mValues, "_id=" + j, null);
        if (iUpdateInTransaction == 0) {
            return 0;
        }
        if (this.mValues2.size() != 0) {
            Uri uriFindFirstDataRow = findFirstDataRow(j, "vnd.android.cursor.item/name");
            if (uriFindFirstDataRow != null) {
                this.mContactsProvider.updateInTransaction(uriFindFirstDataRow, this.mValues2, null, null);
            } else {
                this.mValues2.put("raw_contact_id", Long.valueOf(j));
                this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues2);
            }
        }
        if (this.mValues3.size() != 0) {
            Uri uriFindFirstDataRow2 = findFirstDataRow(j, "vnd.android.cursor.item/note");
            if (uriFindFirstDataRow2 != null) {
                this.mContactsProvider.updateInTransaction(uriFindFirstDataRow2, this.mValues3, null, null);
            } else {
                this.mValues3.put("raw_contact_id", Long.valueOf(j));
                this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues3);
            }
        }
        return iUpdateInTransaction;
    }

    private int updateOrganizations(long j, ContentValues contentValues) {
        parseOrganizationValues(contentValues);
        return this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "_id=" + j, null);
    }

    private int updatePhones(long j, ContentValues contentValues) {
        parsePhoneValues(contentValues);
        return this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "_id=" + j, null);
    }

    private int updateContactMethods(long j, ContentValues contentValues) {
        int i = 1;
        this.mDataMimetypeQuery.bindLong(1, j);
        try {
            long jSimpleQueryForLong = this.mDataMimetypeQuery.simpleQueryForLong();
            if (jSimpleQueryForLong != this.mMimetypeEmail) {
                if (jSimpleQueryForLong == this.mMimetypeIm) {
                    i = 3;
                } else {
                    if (jSimpleQueryForLong != this.mMimetypePostal) {
                        return 0;
                    }
                    i = 2;
                }
            }
            parseContactMethodValues(i, contentValues);
            return this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "_id=" + j, null);
        } catch (SQLiteDoneException e) {
            return 0;
        }
    }

    private int updateExtensions(long j, ContentValues contentValues) {
        parseExtensionValues(contentValues);
        return this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "_id=" + j, null);
    }

    private int updateGroups(long j, ContentValues contentValues) {
        parseGroupValues(contentValues);
        return this.mContactsProvider.updateInTransaction(ContactsContract.Groups.CONTENT_URI, this.mValues, "_id=" + j, null);
    }

    private int updatePhoto(long j, ContentValues contentValues) throws Throwable {
        int iUpdateInTransaction;
        long jFindFirstDataId = findFirstDataId(j, "vnd.android.cursor.item/photo");
        this.mValues.clear();
        this.mValues.put("data15", contentValues.getAsByteArray("data"));
        if (jFindFirstDataId == -1) {
            this.mValues.put("mimetype", "vnd.android.cursor.item/photo");
            this.mValues.put("raw_contact_id", Long.valueOf(j));
            jFindFirstDataId = ContentUris.parseId(this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues));
            iUpdateInTransaction = 1;
        } else {
            iUpdateInTransaction = this.mContactsProvider.updateInTransaction(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, jFindFirstDataId), this.mValues, null, null);
        }
        updateLegacyPhotoData(j, jFindFirstDataId, contentValues);
        return iUpdateInTransaction;
    }

    private int updatePhotoByDataId(long j, ContentValues contentValues) throws Throwable {
        this.mDataRawContactIdQuery.bindLong(1, j);
        try {
            long jSimpleQueryForLong = this.mDataRawContactIdQuery.simpleQueryForLong();
            if (contentValues.containsKey("data")) {
                byte[] asByteArray = contentValues.getAsByteArray("data");
                this.mValues.clear();
                this.mValues.put("data15", asByteArray);
                this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "_id=" + j, null);
            }
            updateLegacyPhotoData(jSimpleQueryForLong, j, contentValues);
            return 1;
        } catch (SQLiteDoneException e) {
            return 0;
        }
    }

    private void updateLegacyPhotoData(long j, long j2, ContentValues contentValues) throws Throwable {
        this.mValues.clear();
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data2", contentValues, "local_version");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data3", contentValues, "download_required");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data4", contentValues, "exists_on_server");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data5", contentValues, "sync_error");
        if (this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "mimetype='vnd.android.cursor.item/photo_v1_extras' AND raw_contact_id=" + j + " AND data1=" + j2, null) == 0) {
            this.mValues.put("raw_contact_id", Long.valueOf(j));
            this.mValues.put("mimetype", "vnd.android.cursor.item/photo_v1_extras");
            this.mValues.put("data1", Long.valueOf(j2));
            this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues);
        }
    }

    private int updateSettings(ContentValues contentValues) throws Throwable {
        String[] strArr;
        String str;
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        String asString = contentValues.getAsString("_sync_account");
        String asString2 = contentValues.getAsString("_sync_account_type");
        String asString3 = contentValues.getAsString("key");
        if (asString3 == null) {
            throw new IllegalArgumentException("you must specify the key when updating settings");
        }
        updateSetting(writableDatabase, asString, asString2, contentValues);
        if (asString3.equals("syncEverything")) {
            this.mValues.clear();
            this.mValues.put("should_sync", contentValues.getAsInteger("value"));
            if (asString != null && asString2 != null) {
                strArr = new String[]{asString, asString2};
                str = "account_name=? AND account_type=? AND data_set IS NULL";
            } else {
                strArr = null;
                str = "account_name IS NULL AND account_type IS NULL AND data_set IS NULL";
            }
            if (this.mContactsProvider.updateInTransaction(ContactsContract.Settings.CONTENT_URI, this.mValues, str, strArr) == 0) {
                this.mValues.put("account_name", asString);
                this.mValues.put("account_type", asString2);
                this.mContactsProvider.insertInTransaction(ContactsContract.Settings.CONTENT_URI, this.mValues);
            }
        }
        return 1;
    }

    private void updateSetting(SQLiteDatabase sQLiteDatabase, String str, String str2, ContentValues contentValues) {
        String asString = contentValues.getAsString("key");
        if (str == null || str2 == null) {
            sQLiteDatabase.delete("v1_settings", "_sync_account IS NULL AND key=?", new String[]{asString});
        } else {
            sQLiteDatabase.delete("v1_settings", "_sync_account=? AND _sync_account_type=? AND key=?", new String[]{str, str2, asString});
        }
        if (sQLiteDatabase.insert("v1_settings", "key", contentValues) < 0) {
            throw new SQLException("error updating settings with " + contentValues);
        }
    }

    public void copySettingsToLegacySettings() {
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        Cursor cursorRawQuery = writableDatabase.rawQuery("SELECT account_name,account_type,should_sync FROM settings LEFT OUTER JOIN v1_settings ON (account_name=_sync_account AND account_type=_sync_account_type AND data_set IS NULL AND key='syncEverything') WHERE should_sync<>value", null);
        while (cursorRawQuery.moveToNext()) {
            try {
                String string = cursorRawQuery.getString(0);
                String string2 = cursorRawQuery.getString(1);
                String string3 = cursorRawQuery.getString(2);
                this.mValues.clear();
                this.mValues.put("_sync_account", string);
                this.mValues.put("_sync_account_type", string2);
                this.mValues.put("key", "syncEverything");
                this.mValues.put("value", string3);
                updateSetting(writableDatabase, string, string2, this.mValues);
            } finally {
                cursorRawQuery.close();
            }
        }
    }

    private void parsePeopleValues(ContentValues contentValues) {
        this.mValues.clear();
        this.mValues2.clear();
        this.mValues3.clear();
        ContactsDatabaseHelper.copyStringValue(this.mValues, "custom_ringtone", contentValues, "custom_ringtone");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "send_to_voicemail", contentValues, "send_to_voicemail");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "starred", contentValues, "starred");
        if (this.mAccount != null) {
            this.mValues.put("account_name", this.mAccount.name);
            this.mValues.put("account_type", this.mAccount.type);
        }
        if (contentValues.containsKey("name") || contentValues.containsKey("phonetic_name")) {
            this.mValues2.put("mimetype", "vnd.android.cursor.item/name");
            ContactsDatabaseHelper.copyStringValue(this.mValues2, "data1", contentValues, "name");
            if (contentValues.containsKey("phonetic_name")) {
                String asString = contentValues.getAsString("phonetic_name");
                NameSplitter.Name name = new NameSplitter.Name();
                this.mPhoneticNameSplitter.split(name, asString);
                this.mValues2.put("data7", name.getGivenNames());
                this.mValues2.put("data8", name.getMiddleName());
                this.mValues2.put("data9", name.getFamilyName());
            }
        }
        if (contentValues.containsKey("notes")) {
            this.mValues3.put("mimetype", "vnd.android.cursor.item/note");
            ContactsDatabaseHelper.copyStringValue(this.mValues3, "data1", contentValues, "notes");
        }
    }

    private void parseOrganizationValues(ContentValues contentValues) {
        this.mValues.clear();
        this.mValues.put("mimetype", "vnd.android.cursor.item/organization");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "is_primary", contentValues, "isprimary");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data1", contentValues, "company");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "data2", contentValues, "type");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data3", contentValues, "label");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data4", contentValues, "title");
    }

    private void parsePhoneValues(ContentValues contentValues) {
        this.mValues.clear();
        this.mValues.put("mimetype", "vnd.android.cursor.item/phone_v2");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "is_primary", contentValues, "isprimary");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data1", contentValues, "number");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "data2", contentValues, "type");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data3", contentValues, "label");
    }

    private void parseContactMethodValues(int i, ContentValues contentValues) {
        this.mValues.clear();
        ContactsDatabaseHelper.copyLongValue(this.mValues, "is_primary", contentValues, "isprimary");
        switch (i) {
            case 1:
                copyCommonFields(contentValues, "vnd.android.cursor.item/email_v2", "data2", "data3", "data14");
                ContactsDatabaseHelper.copyStringValue(this.mValues, "data1", contentValues, "data");
                break;
            case 2:
                copyCommonFields(contentValues, "vnd.android.cursor.item/postal-address_v2", "data2", "data3", "data14");
                ContactsDatabaseHelper.copyStringValue(this.mValues, "data1", contentValues, "data");
                break;
            case 3:
                String asString = contentValues.getAsString("data");
                if (asString.startsWith("pre:")) {
                    this.mValues.put("data5", Integer.valueOf(Integer.parseInt(asString.substring(4))));
                } else if (asString.startsWith("custom:")) {
                    this.mValues.put("data5", (Integer) (-1));
                    this.mValues.put("data6", asString.substring(7));
                }
                copyCommonFields(contentValues, "vnd.android.cursor.item/im", "data2", "data3", "data14");
                break;
        }
    }

    private void copyCommonFields(ContentValues contentValues, String str, String str2, String str3, String str4) {
        this.mValues.put("mimetype", str);
        ContactsDatabaseHelper.copyLongValue(this.mValues, str2, contentValues, "type");
        ContactsDatabaseHelper.copyStringValue(this.mValues, str3, contentValues, "label");
        ContactsDatabaseHelper.copyStringValue(this.mValues, str4, contentValues, "aux_data");
    }

    private void parseGroupValues(ContentValues contentValues) {
        this.mValues.clear();
        ContactsDatabaseHelper.copyStringValue(this.mValues, "title", contentValues, "name");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "notes", contentValues, "notes");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "system_id", contentValues, "system_id");
    }

    private void parseExtensionValues(ContentValues contentValues) {
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data1", contentValues, "name");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data2", contentValues, "value");
    }

    private Uri findFirstDataRow(long j, String str) {
        long jFindFirstDataId = findFirstDataId(j, str);
        if (jFindFirstDataId == -1) {
            return null;
        }
        return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, jFindFirstDataId);
    }

    private long findFirstDataId(long j, String str) {
        long j2;
        Cursor cursorQuery = this.mContactsProvider.query(ContactsContract.Data.CONTENT_URI, IdQuery.COLUMNS, "raw_contact_id=" + j + " AND mimetype='" + str + "'", null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                j2 = cursorQuery.getLong(0);
            } else {
                j2 = -1;
            }
            return j2;
        } finally {
            cursorQuery.close();
        }
    }

    public int delete(Uri uri, String str, String[] strArr) {
        int iMatch = sUriMatcher.match(uri);
        if (iMatch == -1 || iMatch == 44) {
            throw new UnsupportedOperationException(this.mDbHelper.exceptionMessage(uri));
        }
        Cursor cursorQuery = query(uri, IdQuery.COLUMNS, str, strArr, null, null);
        if (cursorQuery == null) {
            return 0;
        }
        int iDelete = 0;
        while (cursorQuery.moveToNext()) {
            try {
                iDelete += delete(uri, iMatch, cursorQuery.getLong(0));
            } finally {
                cursorQuery.close();
            }
        }
        return iDelete;
    }

    public int delete(Uri uri, int i, long j) throws Throwable {
        switch (i) {
            case 1:
            case 2:
                return this.mContactsProvider.deleteRawContact(j, this.mDbHelper.getContactId(j), false);
            case 3:
            case 6:
            case 7:
            case 10:
            case 11:
            case 16:
            case 17:
            case 22:
            case 23:
            default:
                throw new UnsupportedOperationException(this.mDbHelper.exceptionMessage(uri));
            case 4:
            case 5:
                return this.mContactsProvider.deleteData(j, ORGANIZATION_MIME_TYPES);
            case 8:
            case 9:
                return this.mContactsProvider.deleteData(j, CONTACT_METHOD_MIME_TYPES);
            case 12:
            case 13:
                return this.mContactsProvider.deleteData(j, PHONE_MIME_TYPES);
            case 14:
            case 15:
                return this.mContactsProvider.deleteData(j, EXTENSION_MIME_TYPES);
            case 18:
            case 19:
                return this.mContactsProvider.deleteGroup(uri, j, false);
            case 20:
            case 21:
                return this.mContactsProvider.deleteData(j, GROUP_MEMBERSHIP_MIME_TYPES);
            case 24:
                this.mValues.clear();
                this.mValues.putNull("data");
                updatePhoto(j, this.mValues);
                return 0;
            case 25:
            case 26:
                return this.mContactsProvider.deleteData(j, PHOTO_MIME_TYPES);
        }
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2, String str3) {
        String str4;
        Cursor cursorQuery;
        ensureDefaultAccount();
        SQLiteDatabase readableDatabase = this.mDbHelper.getReadableDatabase();
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case 1:
                sQLiteQueryBuilder.setTables("view_v1_people people  LEFT OUTER JOIN presence ON (presence.presence_data_id=(SELECT MAX(presence_data_id) FROM presence WHERE people._id = presence_raw_contact_id) )");
                sQLiteQueryBuilder.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                    cursorQuery.setNotificationUri(this.mContext.getContentResolver(), Contacts.CONTENT_URI);
                }
                return cursorQuery;
            case 2:
                sQLiteQueryBuilder.setTables("view_v1_people people  LEFT OUTER JOIN presence ON (presence.presence_data_id=(SELECT MAX(presence_data_id) FROM presence WHERE people._id = presence_raw_contact_id) )");
                sQLiteQueryBuilder.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 3:
            case 27:
            case 28:
            case 35:
            case 36:
            case 37:
            case 38:
            default:
                throw new IllegalArgumentException(this.mDbHelper.exceptionMessage(uri));
            case 4:
                sQLiteQueryBuilder.setTables("view_v1_organizations organizations");
                sQLiteQueryBuilder.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 5:
                sQLiteQueryBuilder.setTables("view_v1_organizations organizations");
                sQLiteQueryBuilder.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 6:
                sQLiteQueryBuilder.setTables("view_v1_contact_methods contact_methods");
                sQLiteQueryBuilder.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                sQLiteQueryBuilder.appendWhere(" AND kind IS NOT NULL");
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 7:
                sQLiteQueryBuilder.setTables("view_v1_contact_methods contact_methods");
                sQLiteQueryBuilder.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(3));
                sQLiteQueryBuilder.appendWhere(" AND kind IS NOT NULL");
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 8:
                sQLiteQueryBuilder.setTables("view_v1_contact_methods contact_methods");
                sQLiteQueryBuilder.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 9:
                sQLiteQueryBuilder.setTables("view_v1_contact_methods contact_methods");
                sQLiteQueryBuilder.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 10:
                sQLiteQueryBuilder.setTables("view_v1_phones phones");
                sQLiteQueryBuilder.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 11:
                sQLiteQueryBuilder.setTables("view_v1_phones phones");
                sQLiteQueryBuilder.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(3));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 12:
                sQLiteQueryBuilder.setTables("view_v1_phones phones");
                sQLiteQueryBuilder.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 13:
                sQLiteQueryBuilder.setTables("view_v1_phones phones");
                sQLiteQueryBuilder.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 14:
                sQLiteQueryBuilder.setTables("view_v1_extensions extensions");
                sQLiteQueryBuilder.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 15:
                sQLiteQueryBuilder.setTables("view_v1_extensions extensions");
                sQLiteQueryBuilder.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 16:
                sQLiteQueryBuilder.setTables("view_v1_extensions extensions");
                sQLiteQueryBuilder.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 17:
                sQLiteQueryBuilder.setTables("view_v1_extensions extensions");
                sQLiteQueryBuilder.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(3));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 18:
                sQLiteQueryBuilder.setTables("view_v1_groups groups");
                sQLiteQueryBuilder.setProjectionMap(sGroupProjectionMap);
                applyGroupAccount(sQLiteQueryBuilder);
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 19:
                sQLiteQueryBuilder.setTables("view_v1_groups groups");
                sQLiteQueryBuilder.setProjectionMap(sGroupProjectionMap);
                applyGroupAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 20:
                sQLiteQueryBuilder.setTables("view_v1_group_membership groupmembership");
                sQLiteQueryBuilder.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 21:
                sQLiteQueryBuilder.setTables("view_v1_group_membership groupmembership");
                sQLiteQueryBuilder.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 22:
                sQLiteQueryBuilder.setTables("view_v1_group_membership groupmembership");
                sQLiteQueryBuilder.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 23:
                sQLiteQueryBuilder.setTables("view_v1_group_membership groupmembership");
                sQLiteQueryBuilder.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(3));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 24:
                sQLiteQueryBuilder.setTables("view_v1_photos photos");
                sQLiteQueryBuilder.setProjectionMap(sPhotoProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = "1";
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 25:
                sQLiteQueryBuilder.setTables("view_v1_photos photos");
                sQLiteQueryBuilder.setProjectionMap(sPhotoProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 26:
                sQLiteQueryBuilder.setTables("view_v1_photos photos");
                sQLiteQueryBuilder.setProjectionMap(sPhotoProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 29:
                sQLiteQueryBuilder.setTables("view_v1_people people  LEFT OUTER JOIN presence ON (presence.presence_data_id=(SELECT MAX(presence_data_id) FROM presence WHERE people._id = presence_raw_contact_id) )");
                sQLiteQueryBuilder.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND _id IN " + getRawContactsByFilterAsNestedQuery(uri.getPathSegments().get(2)));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 30:
            case 31:
                throw new UnsupportedOperationException(this.mDbHelper.exceptionMessage(uri));
            case 32:
                return this.mGlobalSearchSupport.handleSearchSuggestionsQuery(readableDatabase, uri, strArr, str3, null);
            case 33:
                return this.mGlobalSearchSupport.handleSearchShortcutRefresh(readableDatabase, strArr, uri.getLastPathSegment(), ContactsProvider2.getQueryParameter(uri, "filter"), null);
            case 34:
                sQLiteQueryBuilder.setTables("view_v1_phones phones");
                sQLiteQueryBuilder.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                if (uri.getPathSegments().size() > 2) {
                    String lastPathSegment = uri.getLastPathSegment();
                    sQLiteQueryBuilder.appendWhere(" AND person =");
                    sQLiteQueryBuilder.appendWhere(this.mDbHelper.buildPhoneLookupAsNestedQuery(lastPathSegment));
                    sQLiteQueryBuilder.setDistinct(true);
                }
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 39:
                sQLiteQueryBuilder.setTables("view_v1_contact_methods contact_methods");
                sQLiteQueryBuilder.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND kind=1");
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 40:
                sQLiteQueryBuilder.setTables("view_v1_people people  LEFT OUTER JOIN presence ON (presence.presence_data_id=(SELECT MAX(presence_data_id) FROM presence WHERE people._id = presence_raw_contact_id) )");
                sQLiteQueryBuilder.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND " + buildGroupNameMatchWhereClause(uri.getPathSegments().get(2)));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 41:
                sQLiteQueryBuilder.setTables("view_v1_people people  LEFT OUTER JOIN presence ON (presence.presence_data_id=(SELECT MAX(presence_data_id) FROM presence WHERE people._id = presence_raw_contact_id) )");
                sQLiteQueryBuilder.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND " + buildGroupSystemIdMatchWhereClause(uri.getPathSegments().get(2)));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 42:
                sQLiteQueryBuilder.setTables("view_v1_organizations organizations");
                sQLiteQueryBuilder.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 43:
                sQLiteQueryBuilder.setTables("view_v1_organizations organizations");
                sQLiteQueryBuilder.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(sQLiteQueryBuilder);
                sQLiteQueryBuilder.appendWhere(" AND person=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(1));
                sQLiteQueryBuilder.appendWhere(" AND _id=");
                sQLiteQueryBuilder.appendWhere(uri.getPathSegments().get(3));
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 44:
                copySettingsToLegacySettings();
                sQLiteQueryBuilder.setTables("v1_settings");
                str4 = str3;
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str, strArr2, null, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
        }
    }

    private void applyRawContactsAccount(SQLiteQueryBuilder sQLiteQueryBuilder) {
        StringBuilder sb = new StringBuilder();
        appendRawContactsAccount(sb);
        sQLiteQueryBuilder.appendWhere(sb.toString());
    }

    private void appendRawContactsAccount(StringBuilder sb) {
        if (this.mAccount != null) {
            sb.append("account_name=");
            DatabaseUtils.appendEscapedSQLString(sb, this.mAccount.name);
            sb.append(" AND account_type=");
            DatabaseUtils.appendEscapedSQLString(sb, this.mAccount.type);
            return;
        }
        sb.append(AccountUtils.getLocalAccountSelection());
    }

    private void applyGroupAccount(SQLiteQueryBuilder sQLiteQueryBuilder) {
        StringBuilder sb = new StringBuilder();
        appendGroupAccount(sb);
        sQLiteQueryBuilder.appendWhere(sb.toString());
    }

    private void appendGroupAccount(StringBuilder sb) {
        if (this.mAccount != null) {
            sb.append("account_name=");
            DatabaseUtils.appendEscapedSQLString(sb, this.mAccount.name);
            sb.append(" AND account_type=");
            DatabaseUtils.appendEscapedSQLString(sb, this.mAccount.type);
            return;
        }
        sb.append(AccountUtils.getLocalSupportGroupAccountSelection());
    }

    private String buildGroupNameMatchWhereClause(String str) {
        return "people._id IN (SELECT data.raw_contact_id FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) WHERE mimetype='vnd.android.cursor.item/group_membership' AND data1=(SELECT groups._id FROM groups WHERE title=" + DatabaseUtils.sqlEscapeString(str) + "))";
    }

    private String buildGroupSystemIdMatchWhereClause(String str) {
        return "people._id IN (SELECT data.raw_contact_id FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) WHERE mimetype='vnd.android.cursor.item/group_membership' AND data1=(SELECT groups._id FROM groups WHERE system_id=" + DatabaseUtils.sqlEscapeString(str) + "))";
    }

    private String getRawContactsByFilterAsNestedQuery(String str) {
        StringBuilder sb = new StringBuilder();
        String strNormalize = NameNormalizer.normalize(str);
        if (TextUtils.isEmpty(strNormalize)) {
            sb.append("(0)");
        } else {
            sb.append("(SELECT raw_contact_id FROM name_lookup WHERE normalized_name GLOB '");
            sb.append(strNormalize);
            sb.append("*' AND name_type IN (2,3");
            sb.append(",4");
            sb.append("))");
        }
        return sb.toString();
    }

    private void onChange(Uri uri) {
        this.mContext.getContentResolver().notifyChange(Contacts.CONTENT_URI, null);
    }

    public String getType(Uri uri) {
        int iMatch = sUriMatcher.match(uri);
        switch (iMatch) {
            case 1:
                return "vnd.android.cursor.dir/person";
            case 2:
                return "vnd.android.cursor.item/person";
            default:
                switch (iMatch) {
                    case 4:
                        return "vnd.android.cursor.dir/organizations";
                    case 5:
                        return "vnd.android.cursor.item/organization";
                    case 6:
                        return "vnd.android.cursor.dir/contact-methods";
                    case 7:
                        return getContactMethodType(uri);
                    case 8:
                        return "vnd.android.cursor.dir/contact-methods";
                    case 9:
                        return getContactMethodType(uri);
                    case 10:
                        return "vnd.android.cursor.dir/phone";
                    case 11:
                        return "vnd.android.cursor.item/phone";
                    case 12:
                        return "vnd.android.cursor.dir/phone";
                    case 13:
                        return "vnd.android.cursor.item/phone";
                    case 14:
                    case 16:
                        return "vnd.android.cursor.dir/contact_extensions";
                    case 15:
                    case 17:
                        return "vnd.android.cursor.item/contact_extensions";
                    default:
                        switch (iMatch) {
                            case 24:
                                return "vnd.android.cursor.item/photo";
                            case 25:
                                return "vnd.android.cursor.dir/photo";
                            case 26:
                                return "vnd.android.cursor.item/photo";
                            default:
                                switch (iMatch) {
                                    case 32:
                                        return "vnd.android.cursor.dir/vnd.android.search.suggest";
                                    case 33:
                                        return "vnd.android.cursor.item/vnd.android.search.suggest";
                                    case 34:
                                        return "vnd.android.cursor.dir/phone";
                                    default:
                                        throw new IllegalArgumentException(this.mDbHelper.exceptionMessage(uri));
                                }
                        }
                }
        }
    }

    private String getContactMethodType(Uri uri) {
        Cursor cursorQuery = query(uri, new String[]{"kind"}, null, null, null, null);
        String str = null;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    switch (cursorQuery.getInt(0)) {
                        case 1:
                            str = "vnd.android.cursor.item/email";
                            break;
                        case 2:
                            str = "vnd.android.cursor.item/postal-address";
                            break;
                        case 3:
                            str = "vnd.android.cursor.item/jabber-im";
                            break;
                    }
                }
            } finally {
                cursorQuery.close();
            }
        }
        return str;
    }
}
