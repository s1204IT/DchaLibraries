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
import android.util.Log;
import com.android.providers.contacts.NameSplitter;
import java.util.HashMap;
import java.util.Locale;

public class LegacyApiSupport {
    private static final HashMap<String, String> sContactMethodProjectionMap;
    private static final HashMap<String, String> sExtensionProjectionMap;
    private static final HashMap<String, String> sGroupMembershipProjectionMap;
    private static final HashMap<String, String> sGroupProjectionMap;
    private static final HashMap<String, String> sOrganizationProjectionMap;
    private static final HashMap<String, String> sPeopleProjectionMap;
    private static final HashMap<String, String> sPhoneProjectionMap;
    private static final HashMap<String, String> sPhotoProjectionMap;
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
    private static final Uri LIVE_FOLDERS_CONTACTS_URI = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "live_folders/contacts");
    private static final Uri LIVE_FOLDERS_CONTACTS_WITH_PHONES_URI = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "live_folders/contacts_with_phones");
    private static final Uri LIVE_FOLDERS_CONTACTS_FAVORITES_URI = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "live_folders/favorites");
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
        UriMatcher matcher = sUriMatcher;
        matcher.addURI("contacts", "extensions", 14);
        matcher.addURI("contacts", "extensions/#", 15);
        matcher.addURI("contacts", "groups", 18);
        matcher.addURI("contacts", "groups/#", 19);
        matcher.addURI("contacts", "groups/name/*/members", 40);
        matcher.addURI("contacts", "groups/system_id/*/members", 41);
        matcher.addURI("contacts", "groupmembership", 20);
        matcher.addURI("contacts", "groupmembership/#", 21);
        matcher.addURI("contacts", "people", 1);
        matcher.addURI("contacts", "people/filter/*", 29);
        matcher.addURI("contacts", "people/#", 2);
        matcher.addURI("contacts", "people/#/extensions", 16);
        matcher.addURI("contacts", "people/#/extensions/#", 17);
        matcher.addURI("contacts", "people/#/phones", 10);
        matcher.addURI("contacts", "people/#/phones/#", 11);
        matcher.addURI("contacts", "people/#/photo", 24);
        matcher.addURI("contacts", "people/#/contact_methods", 6);
        matcher.addURI("contacts", "people/#/contact_methods/#", 7);
        matcher.addURI("contacts", "people/#/organizations", 42);
        matcher.addURI("contacts", "people/#/organizations/#", 43);
        matcher.addURI("contacts", "people/#/groupmembership", 22);
        matcher.addURI("contacts", "people/#/groupmembership/#", 23);
        matcher.addURI("contacts", "people/#/update_contact_time", 3);
        matcher.addURI("contacts", "deleted_people", 30);
        matcher.addURI("contacts", "deleted_groups", 31);
        matcher.addURI("contacts", "phones", 12);
        matcher.addURI("contacts", "phones/filter/*", 34);
        matcher.addURI("contacts", "phones/#", 13);
        matcher.addURI("contacts", "photos", 25);
        matcher.addURI("contacts", "photos/#", 26);
        matcher.addURI("contacts", "contact_methods", 8);
        matcher.addURI("contacts", "contact_methods/email", 39);
        matcher.addURI("contacts", "contact_methods/#", 9);
        matcher.addURI("contacts", "organizations", 4);
        matcher.addURI("contacts", "organizations/#", 5);
        matcher.addURI("contacts", "search_suggest_query", 32);
        matcher.addURI("contacts", "search_suggest_query/*", 32);
        matcher.addURI("contacts", "search_suggest_shortcut/*", 33);
        matcher.addURI("contacts", "settings", 44);
        matcher.addURI("contacts", "live_folders/people", 35);
        matcher.addURI("contacts", "live_folders/people/*", 36);
        matcher.addURI("contacts", "live_folders/people_with_phones", 37);
        matcher.addURI("contacts", "live_folders/favorites", 38);
        HashMap<String, String> peopleProjectionMap = new HashMap<>();
        peopleProjectionMap.put("name", "name");
        peopleProjectionMap.put("display_name", "display_name");
        peopleProjectionMap.put("phonetic_name", "phonetic_name");
        peopleProjectionMap.put("notes", "notes");
        peopleProjectionMap.put("times_contacted", "times_contacted");
        peopleProjectionMap.put("last_time_contacted", "last_time_contacted");
        peopleProjectionMap.put("custom_ringtone", "custom_ringtone");
        peopleProjectionMap.put("send_to_voicemail", "send_to_voicemail");
        peopleProjectionMap.put("starred", "starred");
        peopleProjectionMap.put("primary_organization", "primary_organization");
        peopleProjectionMap.put("primary_email", "primary_email");
        peopleProjectionMap.put("primary_phone", "primary_phone");
        sPeopleProjectionMap = new HashMap<>(peopleProjectionMap);
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
        sOrganizationProjectionMap = new HashMap<>();
        sOrganizationProjectionMap.put("_id", "_id");
        sOrganizationProjectionMap.put("person", "person");
        sOrganizationProjectionMap.put("isprimary", "isprimary");
        sOrganizationProjectionMap.put("company", "company");
        sOrganizationProjectionMap.put("type", "type");
        sOrganizationProjectionMap.put("label", "label");
        sOrganizationProjectionMap.put("title", "title");
        sContactMethodProjectionMap = new HashMap<>(peopleProjectionMap);
        sContactMethodProjectionMap.put("_id", "_id");
        sContactMethodProjectionMap.put("person", "person");
        sContactMethodProjectionMap.put("kind", "kind");
        sContactMethodProjectionMap.put("isprimary", "isprimary");
        sContactMethodProjectionMap.put("type", "type");
        sContactMethodProjectionMap.put("data", "data");
        sContactMethodProjectionMap.put("label", "label");
        sContactMethodProjectionMap.put("aux_data", "aux_data");
        sPhoneProjectionMap = new HashMap<>(peopleProjectionMap);
        sPhoneProjectionMap.put("_id", "_id");
        sPhoneProjectionMap.put("person", "person");
        sPhoneProjectionMap.put("isprimary", "isprimary");
        sPhoneProjectionMap.put("number", "number");
        sPhoneProjectionMap.put("type", "type");
        sPhoneProjectionMap.put("label", "label");
        sPhoneProjectionMap.put("number_key", "number_key");
        sExtensionProjectionMap = new HashMap<>();
        sExtensionProjectionMap.put("_id", "_id");
        sExtensionProjectionMap.put("person", "person");
        sExtensionProjectionMap.put("name", "name");
        sExtensionProjectionMap.put("value", "value");
        sGroupProjectionMap = new HashMap<>();
        sGroupProjectionMap.put("_id", "_id");
        sGroupProjectionMap.put("name", "name");
        sGroupProjectionMap.put("notes", "notes");
        sGroupProjectionMap.put("system_id", "system_id");
        sGroupMembershipProjectionMap = new HashMap<>(sGroupProjectionMap);
        sGroupMembershipProjectionMap.put("_id", "_id");
        sGroupMembershipProjectionMap.put("person", "person");
        sGroupMembershipProjectionMap.put("group_id", "group_id");
        sGroupMembershipProjectionMap.put("group_sync_id", "group_sync_id");
        sGroupMembershipProjectionMap.put("group_sync_account", "group_sync_account");
        sGroupMembershipProjectionMap.put("group_sync_account_type", "group_sync_account_type");
        sPhotoProjectionMap = new HashMap<>();
        sPhotoProjectionMap.put("_id", "_id");
        sPhotoProjectionMap.put("person", "person");
        sPhotoProjectionMap.put("data", "data");
        sPhotoProjectionMap.put("local_version", "local_version");
        sPhotoProjectionMap.put("download_required", "download_required");
        sPhotoProjectionMap.put("exists_on_server", "exists_on_server");
        sPhotoProjectionMap.put("sync_error", "sync_error");
    }

    public LegacyApiSupport(Context context, ContactsDatabaseHelper contactsDatabaseHelper, ContactsProvider2 contactsProvider, GlobalSearchSupport globalSearchSupport) {
        this.mContext = context;
        this.mContactsProvider = contactsProvider;
        this.mDbHelper = contactsDatabaseHelper;
        this.mGlobalSearchSupport = globalSearchSupport;
        this.mPhoneticNameSplitter = new NameSplitter("", "", "", context.getString(android.R.string.NetworkPreferenceSwitchSummary), Locale.getDefault());
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        this.mDataMimetypeQuery = db.compileStatement("SELECT mimetype_id FROM data WHERE _id=?");
        this.mDataRawContactIdQuery = db.compileStatement("SELECT raw_contact_id FROM data WHERE _id=?");
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

    public static void createDatabase(SQLiteDatabase db) {
        Log.i("ContactsProviderV1", "Bootstrapping database legacy support");
        createViews(db);
        createSettingsTable(db);
    }

    public static void createViews(SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS view_v1_people;");
        db.execSQL("CREATE VIEW view_v1_people AS SELECT raw_contacts._id AS _id, name.data1 AS name, raw_contacts.display_name AS display_name, trim(trim(ifnull(name.data7,' ')||' '||ifnull(name.data8,' '))||' '||ifnull(name.data9,' '))  AS phonetic_name , note.data1 AS notes, accounts.account_name, accounts.account_type, raw_contacts.times_contacted AS times_contacted, raw_contacts.last_time_contacted AS last_time_contacted, raw_contacts.custom_ringtone AS custom_ringtone, raw_contacts.send_to_voicemail AS send_to_voicemail, raw_contacts.starred AS starred, organization._id AS primary_organization, email._id AS primary_email, phone._id AS primary_phone, phone.data1 AS number, phone.data2 AS type, phone.data3 AS label, _PHONE_NUMBER_STRIPPED_REVERSED(phone.data1) AS number_key FROM raw_contacts JOIN accounts ON (raw_contacts.account_id=accounts._id) LEFT OUTER JOIN data name ON (raw_contacts._id = name.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = name.mimetype_id)='vnd.android.cursor.item/name') LEFT OUTER JOIN data organization ON (raw_contacts._id = organization.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = organization.mimetype_id)='vnd.android.cursor.item/organization' AND organization.is_primary) LEFT OUTER JOIN data email ON (raw_contacts._id = email.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = email.mimetype_id)='vnd.android.cursor.item/email_v2' AND email.is_primary) LEFT OUTER JOIN data note ON (raw_contacts._id = note.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = note.mimetype_id)='vnd.android.cursor.item/note') LEFT OUTER JOIN data phone ON (raw_contacts._id = phone.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = phone.mimetype_id)='vnd.android.cursor.item/phone_v2' AND phone.is_primary) WHERE raw_contacts.deleted=0;");
        db.execSQL("DROP VIEW IF EXISTS view_v1_organizations;");
        db.execSQL("CREATE VIEW view_v1_organizations AS SELECT data._id AS _id, raw_contact_id AS person, is_primary AS isprimary, accounts.account_name, accounts.account_type, data1 AS company, data2 AS type, data3 AS label, data4 AS title FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) JOIN accounts ON (raw_contacts.account_id=accounts._id) WHERE mimetypes.mimetype='vnd.android.cursor.item/organization' AND raw_contacts.deleted=0;");
        db.execSQL("DROP VIEW IF EXISTS view_v1_contact_methods;");
        db.execSQL("CREATE VIEW view_v1_contact_methods AS SELECT data._id AS _id, data.raw_contact_id AS person, CAST ((CASE WHEN mimetype='vnd.android.cursor.item/email_v2' THEN 1 ELSE (CASE WHEN mimetype='vnd.android.cursor.item/im' THEN 3 ELSE (CASE WHEN mimetype='vnd.android.cursor.item/postal-address_v2' THEN 2 ELSE NULL END) END) END) AS INTEGER) AS kind, data.is_primary AS isprimary, data.data2 AS type, " + CONTACT_METHOD_DATA_SQL + " AS data, data.data3 AS label, data.data14 AS aux_data, name.data1 AS name, raw_contacts.display_name AS display_name, trim(trim(ifnull(name.data7,' ')||' '||ifnull(name.data8,' '))||' '||ifnull(name.data9,' '))  AS phonetic_name , note.data1 AS notes, accounts.account_name, accounts.account_type, raw_contacts.times_contacted AS times_contacted, raw_contacts.last_time_contacted AS last_time_contacted, raw_contacts.custom_ringtone AS custom_ringtone, raw_contacts.send_to_voicemail AS send_to_voicemail, raw_contacts.starred AS starred, organization._id AS primary_organization, email._id AS primary_email, phone._id AS primary_phone, phone.data1 AS number, phone.data2 AS type, phone.data3 AS label, _PHONE_NUMBER_STRIPPED_REVERSED(phone.data1) AS number_key FROM data JOIN mimetypes ON (mimetypes._id = data.mimetype_id) JOIN raw_contacts ON (raw_contacts._id = data.raw_contact_id) JOIN accounts ON (raw_contacts.account_id=accounts._id) LEFT OUTER JOIN data name ON (raw_contacts._id = name.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = name.mimetype_id)='vnd.android.cursor.item/name') LEFT OUTER JOIN data organization ON (raw_contacts._id = organization.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = organization.mimetype_id)='vnd.android.cursor.item/organization' AND organization.is_primary) LEFT OUTER JOIN data email ON (raw_contacts._id = email.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = email.mimetype_id)='vnd.android.cursor.item/email_v2' AND email.is_primary) LEFT OUTER JOIN data note ON (raw_contacts._id = note.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = note.mimetype_id)='vnd.android.cursor.item/note') LEFT OUTER JOIN data phone ON (raw_contacts._id = phone.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = phone.mimetype_id)='vnd.android.cursor.item/phone_v2' AND phone.is_primary) WHERE kind IS NOT NULL AND raw_contacts.deleted=0;");
        db.execSQL("DROP VIEW IF EXISTS view_v1_phones;");
        db.execSQL("CREATE VIEW view_v1_phones AS SELECT DISTINCT data._id AS _id, data.raw_contact_id AS person, data.is_primary AS isprimary, data.data1 AS number, data.data2 AS type, data.data3 AS label, _PHONE_NUMBER_STRIPPED_REVERSED(data.data1) AS number_key, name.data1 AS name, raw_contacts.display_name AS display_name, trim(trim(ifnull(name.data7,' ')||' '||ifnull(name.data8,' '))||' '||ifnull(name.data9,' '))  AS phonetic_name , note.data1 AS notes, accounts.account_name, accounts.account_type, raw_contacts.times_contacted AS times_contacted, raw_contacts.last_time_contacted AS last_time_contacted, raw_contacts.custom_ringtone AS custom_ringtone, raw_contacts.send_to_voicemail AS send_to_voicemail, raw_contacts.starred AS starred, organization._id AS primary_organization, email._id AS primary_email, phone._id AS primary_phone, phone.data1 AS number, phone.data2 AS type, phone.data3 AS label, _PHONE_NUMBER_STRIPPED_REVERSED(phone.data1) AS number_key FROM data JOIN phone_lookup ON (data._id = phone_lookup.data_id) JOIN mimetypes ON (mimetypes._id = data.mimetype_id) JOIN raw_contacts ON (raw_contacts._id = data.raw_contact_id) JOIN accounts ON (raw_contacts.account_id=accounts._id) LEFT OUTER JOIN data name ON (raw_contacts._id = name.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = name.mimetype_id)='vnd.android.cursor.item/name') LEFT OUTER JOIN data organization ON (raw_contacts._id = organization.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = organization.mimetype_id)='vnd.android.cursor.item/organization' AND organization.is_primary) LEFT OUTER JOIN data email ON (raw_contacts._id = email.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = email.mimetype_id)='vnd.android.cursor.item/email_v2' AND email.is_primary) LEFT OUTER JOIN data note ON (raw_contacts._id = note.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = note.mimetype_id)='vnd.android.cursor.item/note') LEFT OUTER JOIN data phone ON (raw_contacts._id = phone.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = phone.mimetype_id)='vnd.android.cursor.item/phone_v2' AND phone.is_primary) WHERE mimetypes.mimetype='vnd.android.cursor.item/phone_v2' AND raw_contacts.deleted=0;");
        db.execSQL("DROP VIEW IF EXISTS view_v1_extensions;");
        db.execSQL("CREATE VIEW view_v1_extensions AS SELECT data._id AS _id, data.raw_contact_id AS person, accounts.account_name, accounts.account_type, data1 AS name, data2 AS value FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) JOIN accounts ON (raw_contacts.account_id=accounts._id) WHERE mimetypes.mimetype='vnd.android.cursor.item/contact_extensions' AND raw_contacts.deleted=0;");
        db.execSQL("DROP VIEW IF EXISTS view_v1_groups;");
        db.execSQL("CREATE VIEW view_v1_groups AS SELECT groups._id AS _id, accounts.account_name, accounts.account_type, title AS name, notes AS notes , system_id AS system_id FROM groups JOIN accounts ON (groups.account_id=accounts._id);");
        db.execSQL("DROP VIEW IF EXISTS view_v1_group_membership;");
        db.execSQL("CREATE VIEW view_v1_group_membership AS SELECT data._id AS _id, data.raw_contact_id AS person, accounts.account_name, accounts.account_type, data1 AS group_id, title AS name, notes AS notes, system_id AS system_id, groups.sourceid AS group_sync_id, accounts.account_name AS group_sync_account, accounts.account_type AS group_sync_account_type FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id)  JOIN accounts ON (raw_contacts.account_id=accounts._id)LEFT OUTER JOIN packages ON (data.package_id = packages._id) LEFT OUTER JOIN groups   ON (mimetypes.mimetype='vnd.android.cursor.item/group_membership'       AND groups._id = data.data1)  WHERE mimetypes.mimetype='vnd.android.cursor.item/group_membership' AND raw_contacts.deleted=0;");
        db.execSQL("DROP VIEW IF EXISTS view_v1_photos;");
        db.execSQL("CREATE VIEW view_v1_photos AS SELECT data._id AS _id, data.raw_contact_id AS person, accounts.account_name, accounts.account_type, data.data15 AS data, legacy_photo.data4 AS exists_on_server, legacy_photo.data3 AS download_required, legacy_photo.data2 AS local_version, legacy_photo.data5 AS sync_error FROM data JOIN mimetypes ON (mimetypes._id = data.mimetype_id) JOIN raw_contacts ON (raw_contacts._id = data.raw_contact_id) JOIN accounts ON (raw_contacts.account_id=accounts._id) LEFT OUTER JOIN data name ON (raw_contacts._id = name.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = name.mimetype_id)='vnd.android.cursor.item/name') LEFT OUTER JOIN data organization ON (raw_contacts._id = organization.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = organization.mimetype_id)='vnd.android.cursor.item/organization' AND organization.is_primary) LEFT OUTER JOIN data email ON (raw_contacts._id = email.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = email.mimetype_id)='vnd.android.cursor.item/email_v2' AND email.is_primary) LEFT OUTER JOIN data note ON (raw_contacts._id = note.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = note.mimetype_id)='vnd.android.cursor.item/note') LEFT OUTER JOIN data phone ON (raw_contacts._id = phone.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = phone.mimetype_id)='vnd.android.cursor.item/phone_v2' AND phone.is_primary) LEFT OUTER JOIN data legacy_photo ON (raw_contacts._id = legacy_photo.raw_contact_id AND (SELECT mimetype FROM mimetypes WHERE mimetypes._id = legacy_photo.mimetype_id)='vnd.android.cursor.item/photo_v1_extras' AND data._id = legacy_photo.data1) WHERE mimetypes.mimetype='vnd.android.cursor.item/photo' AND raw_contacts.deleted=0;");
    }

    public static void createSettingsTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS v1_settings;");
        db.execSQL("CREATE TABLE v1_settings (_id INTEGER PRIMARY KEY,_sync_account TEXT,_sync_account_type TEXT,key STRING NOT NULL,value STRING );");
    }

    public Uri insert(Uri uri, ContentValues values) throws Throwable {
        long id;
        ensureDefaultAccount();
        int match = sUriMatcher.match(uri);
        switch (match) {
            case 1:
                id = insertPeople(values);
                break;
            case 2:
            case 3:
            case 5:
            case 7:
            case 9:
            case 11:
            case 13:
            case 15:
            case 16:
            case 17:
            case 19:
            default:
                throw new UnsupportedOperationException(this.mDbHelper.exceptionMessage(uri));
            case 4:
                id = insertOrganization(values);
                break;
            case 6:
                long rawContactId = Long.parseLong(uri.getPathSegments().get(1));
                id = insertContactMethod(rawContactId, values);
                break;
            case 8:
                long rawContactId2 = getRequiredValue(values, "person");
                id = insertContactMethod(rawContactId2, values);
                break;
            case 10:
                long rawContactId3 = Long.parseLong(uri.getPathSegments().get(1));
                id = insertPhone(rawContactId3, values);
                break;
            case 12:
                long rawContactId4 = getRequiredValue(values, "person");
                id = insertPhone(rawContactId4, values);
                break;
            case 14:
                long rawContactId5 = getRequiredValue(values, "person");
                id = insertExtension(rawContactId5, values);
                break;
            case 18:
                id = insertGroup(values);
                break;
            case 20:
                long rawContactId6 = getRequiredValue(values, "person");
                long groupId = getRequiredValue(values, "group_id");
                id = insertGroupMembership(rawContactId6, groupId);
                break;
        }
        if (id < 0) {
            return null;
        }
        Uri result = ContentUris.withAppendedId(uri, id);
        onChange(result);
        return result;
    }

    private long getRequiredValue(ContentValues values, String column) {
        Long value = values.getAsLong(column);
        if (value == null) {
            throw new RuntimeException("Required value: " + column);
        }
        return value.longValue();
    }

    private long insertPeople(ContentValues values) throws Throwable {
        parsePeopleValues(values);
        Uri contactUri = this.mContactsProvider.insertInTransaction(ContactsContract.RawContacts.CONTENT_URI, this.mValues);
        long rawContactId = ContentUris.parseId(contactUri);
        if (this.mValues2.size() != 0) {
            this.mValues2.put("raw_contact_id", Long.valueOf(rawContactId));
            this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues2);
        }
        if (this.mValues3.size() != 0) {
            this.mValues3.put("raw_contact_id", Long.valueOf(rawContactId));
            this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues3);
        }
        return rawContactId;
    }

    private long insertOrganization(ContentValues values) throws Throwable {
        parseOrganizationValues(values);
        ContactsDatabaseHelper.copyLongValue(this.mValues, "raw_contact_id", values, "person");
        Uri uri = this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues);
        return ContentUris.parseId(uri);
    }

    private long insertPhone(long rawContactId, ContentValues values) throws Throwable {
        parsePhoneValues(values);
        this.mValues.put("raw_contact_id", Long.valueOf(rawContactId));
        Uri uri = this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues);
        return ContentUris.parseId(uri);
    }

    private long insertContactMethod(long rawContactId, ContentValues values) throws Throwable {
        Integer kind = values.getAsInteger("kind");
        if (kind == null) {
            throw new RuntimeException("Required value: kind");
        }
        parseContactMethodValues(kind.intValue(), values);
        this.mValues.put("raw_contact_id", Long.valueOf(rawContactId));
        Uri uri = this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues);
        return ContentUris.parseId(uri);
    }

    private long insertExtension(long rawContactId, ContentValues values) throws Throwable {
        this.mValues.clear();
        this.mValues.put("raw_contact_id", Long.valueOf(rawContactId));
        this.mValues.put("mimetype", "vnd.android.cursor.item/contact_extensions");
        parseExtensionValues(values);
        Uri uri = this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues);
        return ContentUris.parseId(uri);
    }

    private long insertGroup(ContentValues values) throws Throwable {
        parseGroupValues(values);
        if (this.mAccount != null) {
            this.mValues.put("account_name", this.mAccount.name);
            this.mValues.put("account_type", this.mAccount.type);
        }
        Uri uri = this.mContactsProvider.insertInTransaction(ContactsContract.Groups.CONTENT_URI, this.mValues);
        return ContentUris.parseId(uri);
    }

    private long insertGroupMembership(long rawContactId, long groupId) throws Throwable {
        this.mValues.clear();
        this.mValues.put("mimetype", "vnd.android.cursor.item/group_membership");
        this.mValues.put("raw_contact_id", Long.valueOf(rawContactId));
        this.mValues.put("data1", Long.valueOf(groupId));
        Uri uri = this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues);
        return ContentUris.parseId(uri);
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count;
        ensureDefaultAccount();
        int match = sUriMatcher.match(uri);
        switch (match) {
            case -1:
            case 20:
            case 21:
                throw new UnsupportedOperationException(this.mDbHelper.exceptionMessage(uri));
            case 3:
                count = updateContactTime(uri, values);
                break;
            case 24:
                long rawContactId = Long.parseLong(uri.getPathSegments().get(1));
                return updatePhoto(rawContactId, values);
            case 44:
                return updateSettings(values);
            default:
                count = updateAll(uri, match, values, selection, selectionArgs);
                break;
        }
        if (count > 0) {
            this.mContext.getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    private int updateAll(Uri uri, int match, ContentValues values, String selection, String[] selectionArgs) {
        Cursor c = query(uri, IdQuery.COLUMNS, selection, selectionArgs, null, null);
        if (c == null) {
            return 0;
        }
        int count = 0;
        while (c.moveToNext()) {
            try {
                long id = c.getLong(0);
                count += update(match, id, values);
            } finally {
                c.close();
            }
        }
        return count;
    }

    public int update(int match, long id, ContentValues values) throws Throwable {
        switch (match) {
            case 1:
            case 2:
                int count = updatePeople(id, values);
                return count;
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
                int count2 = updateOrganizations(id, values);
                return count2;
            case 8:
            case 9:
                int count3 = updateContactMethods(id, values);
                return count3;
            case 12:
            case 13:
                int count4 = updatePhones(id, values);
                return count4;
            case 14:
            case 15:
                int count5 = updateExtensions(id, values);
                return count5;
            case 18:
            case 19:
                int count6 = updateGroups(id, values);
                return count6;
            case 25:
            case 26:
                int count7 = updatePhotoByDataId(id, values);
                return count7;
        }
    }

    private int updatePeople(long rawContactId, ContentValues values) throws Throwable {
        parsePeopleValues(values);
        int count = this.mContactsProvider.updateInTransaction(ContactsContract.RawContacts.CONTENT_URI, this.mValues, "_id=" + rawContactId, null);
        if (count == 0) {
            return 0;
        }
        if (this.mValues2.size() != 0) {
            Uri dataUri = findFirstDataRow(rawContactId, "vnd.android.cursor.item/name");
            if (dataUri != null) {
                this.mContactsProvider.updateInTransaction(dataUri, this.mValues2, null, null);
            } else {
                this.mValues2.put("raw_contact_id", Long.valueOf(rawContactId));
                this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues2);
            }
        }
        if (this.mValues3.size() != 0) {
            Uri dataUri2 = findFirstDataRow(rawContactId, "vnd.android.cursor.item/note");
            if (dataUri2 != null) {
                this.mContactsProvider.updateInTransaction(dataUri2, this.mValues3, null, null);
            } else {
                this.mValues3.put("raw_contact_id", Long.valueOf(rawContactId));
                this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues3);
            }
        }
        if (values.containsKey("last_time_contacted") && !values.containsKey("times_contacted")) {
            updateContactTime(rawContactId, values);
            return count;
        }
        return count;
    }

    private int updateOrganizations(long dataId, ContentValues values) {
        parseOrganizationValues(values);
        return this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "_id=" + dataId, null);
    }

    private int updatePhones(long dataId, ContentValues values) {
        parsePhoneValues(values);
        return this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "_id=" + dataId, null);
    }

    private int updateContactMethods(long dataId, ContentValues values) {
        int kind;
        this.mDataMimetypeQuery.bindLong(1, dataId);
        try {
            long mimetype_id = this.mDataMimetypeQuery.simpleQueryForLong();
            if (mimetype_id == this.mMimetypeEmail) {
                kind = 1;
            } else if (mimetype_id == this.mMimetypeIm) {
                kind = 3;
            } else {
                if (mimetype_id != this.mMimetypePostal) {
                    return 0;
                }
                kind = 2;
            }
            parseContactMethodValues(kind, values);
            return this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "_id=" + dataId, null);
        } catch (SQLiteDoneException e) {
            return 0;
        }
    }

    private int updateExtensions(long dataId, ContentValues values) {
        parseExtensionValues(values);
        return this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "_id=" + dataId, null);
    }

    private int updateGroups(long groupId, ContentValues values) {
        parseGroupValues(values);
        return this.mContactsProvider.updateInTransaction(ContactsContract.Groups.CONTENT_URI, this.mValues, "_id=" + groupId, null);
    }

    private int updateContactTime(Uri uri, ContentValues values) {
        long rawContactId = Long.parseLong(uri.getPathSegments().get(1));
        updateContactTime(rawContactId, values);
        return 1;
    }

    private void updateContactTime(long rawContactId, ContentValues values) {
        Long storedTimeContacted = values.getAsLong("last_time_contacted");
        long lastTimeContacted = storedTimeContacted != null ? storedTimeContacted.longValue() : System.currentTimeMillis();
        long contactId = this.mDbHelper.getContactId(rawContactId);
        SQLiteDatabase mDb = this.mDbHelper.getWritableDatabase();
        this.mSelectionArgs2[0] = String.valueOf(lastTimeContacted);
        if (contactId != 0) {
            this.mSelectionArgs2[1] = String.valueOf(contactId);
            mDb.execSQL("UPDATE contacts SET last_time_contacted=? WHERE _id=?", this.mSelectionArgs2);
            this.mSelectionArgs1[0] = String.valueOf(contactId);
            mDb.execSQL("UPDATE contacts SET times_contacted= ifnull(times_contacted,0)+1 WHERE _id=?", this.mSelectionArgs1);
        }
        this.mSelectionArgs2[1] = String.valueOf(rawContactId);
        mDb.execSQL("UPDATE raw_contacts SET last_time_contacted=? WHERE _id=?", this.mSelectionArgs2);
        this.mSelectionArgs1[0] = String.valueOf(contactId);
        mDb.execSQL("UPDATE raw_contacts SET times_contacted= ifnull(times_contacted,0)+1  WHERE contact_id=?", this.mSelectionArgs1);
    }

    private int updatePhoto(long rawContactId, ContentValues values) throws Throwable {
        int count;
        long dataId = findFirstDataId(rawContactId, "vnd.android.cursor.item/photo");
        this.mValues.clear();
        byte[] bytes = values.getAsByteArray("data");
        this.mValues.put("data15", bytes);
        if (dataId == -1) {
            this.mValues.put("mimetype", "vnd.android.cursor.item/photo");
            this.mValues.put("raw_contact_id", Long.valueOf(rawContactId));
            Uri dataUri = this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues);
            dataId = ContentUris.parseId(dataUri);
            count = 1;
        } else {
            Uri dataUri2 = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId);
            count = this.mContactsProvider.updateInTransaction(dataUri2, this.mValues, null, null);
        }
        updateLegacyPhotoData(rawContactId, dataId, values);
        return count;
    }

    private int updatePhotoByDataId(long dataId, ContentValues values) throws Throwable {
        this.mDataRawContactIdQuery.bindLong(1, dataId);
        try {
            long rawContactId = this.mDataRawContactIdQuery.simpleQueryForLong();
            if (values.containsKey("data")) {
                byte[] bytes = values.getAsByteArray("data");
                this.mValues.clear();
                this.mValues.put("data15", bytes);
                this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "_id=" + dataId, null);
            }
            updateLegacyPhotoData(rawContactId, dataId, values);
            return 1;
        } catch (SQLiteDoneException e) {
            return 0;
        }
    }

    private void updateLegacyPhotoData(long rawContactId, long dataId, ContentValues values) throws Throwable {
        this.mValues.clear();
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data2", values, "local_version");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data3", values, "download_required");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data4", values, "exists_on_server");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data5", values, "sync_error");
        int updated = this.mContactsProvider.updateInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues, "mimetype='vnd.android.cursor.item/photo_v1_extras' AND raw_contact_id=" + rawContactId + " AND data1=" + dataId, null);
        if (updated == 0) {
            this.mValues.put("raw_contact_id", Long.valueOf(rawContactId));
            this.mValues.put("mimetype", "vnd.android.cursor.item/photo_v1_extras");
            this.mValues.put("data1", Long.valueOf(dataId));
            this.mContactsProvider.insertInTransaction(ContactsContract.Data.CONTENT_URI, this.mValues);
        }
    }

    private int updateSettings(ContentValues values) throws Throwable {
        String[] selectionArgs;
        String selection;
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        String accountName = values.getAsString("_sync_account");
        String accountType = values.getAsString("_sync_account_type");
        String key = values.getAsString("key");
        if (key == null) {
            throw new IllegalArgumentException("you must specify the key when updating settings");
        }
        updateSetting(db, accountName, accountType, values);
        if (key.equals("syncEverything")) {
            this.mValues.clear();
            this.mValues.put("should_sync", values.getAsInteger("value"));
            if (accountName != null && accountType != null) {
                selectionArgs = new String[]{accountName, accountType};
                selection = "account_name=? AND account_type=? AND data_set IS NULL";
            } else {
                selectionArgs = null;
                selection = "account_name IS NULL AND account_type IS NULL AND data_set IS NULL";
            }
            int count = this.mContactsProvider.updateInTransaction(ContactsContract.Settings.CONTENT_URI, this.mValues, selection, selectionArgs);
            if (count == 0) {
                this.mValues.put("account_name", accountName);
                this.mValues.put("account_type", accountType);
                this.mContactsProvider.insertInTransaction(ContactsContract.Settings.CONTENT_URI, this.mValues);
            }
        }
        return 1;
    }

    private void updateSetting(SQLiteDatabase db, String accountName, String accountType, ContentValues values) {
        String key = values.getAsString("key");
        if (accountName == null || accountType == null) {
            db.delete("v1_settings", "_sync_account IS NULL AND key=?", new String[]{key});
        } else {
            db.delete("v1_settings", "_sync_account=? AND _sync_account_type=? AND key=?", new String[]{accountName, accountType, key});
        }
        long rowId = db.insert("v1_settings", "key", values);
        if (rowId < 0) {
            throw new SQLException("error updating settings with " + values);
        }
    }

    public void copySettingsToLegacySettings() {
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT account_name,account_type,should_sync FROM settings LEFT OUTER JOIN v1_settings ON (account_name=_sync_account AND account_type=_sync_account_type AND data_set IS NULL AND key='syncEverything') WHERE should_sync<>value", null);
        while (cursor.moveToNext()) {
            try {
                String accountName = cursor.getString(0);
                String accountType = cursor.getString(1);
                String value = cursor.getString(2);
                this.mValues.clear();
                this.mValues.put("_sync_account", accountName);
                this.mValues.put("_sync_account_type", accountType);
                this.mValues.put("key", "syncEverything");
                this.mValues.put("value", value);
                updateSetting(db, accountName, accountType, this.mValues);
            } finally {
                cursor.close();
            }
        }
    }

    private void parsePeopleValues(ContentValues values) {
        this.mValues.clear();
        this.mValues2.clear();
        this.mValues3.clear();
        ContactsDatabaseHelper.copyStringValue(this.mValues, "custom_ringtone", values, "custom_ringtone");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "send_to_voicemail", values, "send_to_voicemail");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "last_time_contacted", values, "last_time_contacted");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "times_contacted", values, "times_contacted");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "starred", values, "starred");
        if (this.mAccount != null) {
            this.mValues.put("account_name", this.mAccount.name);
            this.mValues.put("account_type", this.mAccount.type);
        }
        if (values.containsKey("name") || values.containsKey("phonetic_name")) {
            this.mValues2.put("mimetype", "vnd.android.cursor.item/name");
            ContactsDatabaseHelper.copyStringValue(this.mValues2, "data1", values, "name");
            if (values.containsKey("phonetic_name")) {
                String phoneticName = values.getAsString("phonetic_name");
                NameSplitter.Name parsedName = new NameSplitter.Name();
                this.mPhoneticNameSplitter.split(parsedName, phoneticName);
                this.mValues2.put("data7", parsedName.getGivenNames());
                this.mValues2.put("data8", parsedName.getMiddleName());
                this.mValues2.put("data9", parsedName.getFamilyName());
            }
        }
        if (values.containsKey("notes")) {
            this.mValues3.put("mimetype", "vnd.android.cursor.item/note");
            ContactsDatabaseHelper.copyStringValue(this.mValues3, "data1", values, "notes");
        }
    }

    private void parseOrganizationValues(ContentValues values) {
        this.mValues.clear();
        this.mValues.put("mimetype", "vnd.android.cursor.item/organization");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "is_primary", values, "isprimary");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data1", values, "company");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "data2", values, "type");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data3", values, "label");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data4", values, "title");
    }

    private void parsePhoneValues(ContentValues values) {
        this.mValues.clear();
        this.mValues.put("mimetype", "vnd.android.cursor.item/phone_v2");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "is_primary", values, "isprimary");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data1", values, "number");
        ContactsDatabaseHelper.copyLongValue(this.mValues, "data2", values, "type");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data3", values, "label");
    }

    private void parseContactMethodValues(int kind, ContentValues values) {
        this.mValues.clear();
        ContactsDatabaseHelper.copyLongValue(this.mValues, "is_primary", values, "isprimary");
        switch (kind) {
            case 1:
                copyCommonFields(values, "vnd.android.cursor.item/email_v2", "data2", "data3", "data14");
                ContactsDatabaseHelper.copyStringValue(this.mValues, "data1", values, "data");
                break;
            case 2:
                copyCommonFields(values, "vnd.android.cursor.item/postal-address_v2", "data2", "data3", "data14");
                ContactsDatabaseHelper.copyStringValue(this.mValues, "data1", values, "data");
                break;
            case 3:
                String protocol = values.getAsString("data");
                if (protocol.startsWith("pre:")) {
                    this.mValues.put("data5", Integer.valueOf(Integer.parseInt(protocol.substring(4))));
                } else if (protocol.startsWith("custom:")) {
                    this.mValues.put("data5", (Integer) (-1));
                    this.mValues.put("data6", protocol.substring(7));
                }
                copyCommonFields(values, "vnd.android.cursor.item/im", "data2", "data3", "data14");
                break;
        }
    }

    private void copyCommonFields(ContentValues values, String mimeType, String typeColumn, String labelColumn, String auxDataColumn) {
        this.mValues.put("mimetype", mimeType);
        ContactsDatabaseHelper.copyLongValue(this.mValues, typeColumn, values, "type");
        ContactsDatabaseHelper.copyStringValue(this.mValues, labelColumn, values, "label");
        ContactsDatabaseHelper.copyStringValue(this.mValues, auxDataColumn, values, "aux_data");
    }

    private void parseGroupValues(ContentValues values) {
        this.mValues.clear();
        ContactsDatabaseHelper.copyStringValue(this.mValues, "title", values, "name");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "notes", values, "notes");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "system_id", values, "system_id");
    }

    private void parseExtensionValues(ContentValues values) {
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data1", values, "name");
        ContactsDatabaseHelper.copyStringValue(this.mValues, "data2", values, "value");
    }

    private Uri findFirstDataRow(long rawContactId, String contentItemType) {
        long dataId = findFirstDataId(rawContactId, contentItemType);
        if (dataId == -1) {
            return null;
        }
        return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId);
    }

    private long findFirstDataId(long rawContactId, String mimeType) {
        long dataId = -1;
        Cursor c = this.mContactsProvider.query(ContactsContract.Data.CONTENT_URI, IdQuery.COLUMNS, "raw_contact_id=" + rawContactId + " AND mimetype='" + mimeType + "'", null, null);
        try {
            if (c.moveToFirst()) {
                dataId = c.getLong(0);
            }
            return dataId;
        } finally {
            c.close();
        }
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        int match = sUriMatcher.match(uri);
        if (match == -1 || match == 44) {
            throw new UnsupportedOperationException(this.mDbHelper.exceptionMessage(uri));
        }
        Cursor c = query(uri, IdQuery.COLUMNS, selection, selectionArgs, null, null);
        if (c != null) {
            count = 0;
            while (c.moveToNext()) {
                try {
                    long id = c.getLong(0);
                    count += delete(uri, match, id);
                } finally {
                    c.close();
                }
            }
        }
        return count;
    }

    public int delete(Uri uri, int match, long id) throws Throwable {
        switch (match) {
            case 1:
            case 2:
                int count = this.mContactsProvider.deleteRawContact(id, this.mDbHelper.getContactId(id), false);
                return count;
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
                int count2 = this.mContactsProvider.deleteData(id, ORGANIZATION_MIME_TYPES);
                return count2;
            case 8:
            case 9:
                int count3 = this.mContactsProvider.deleteData(id, CONTACT_METHOD_MIME_TYPES);
                return count3;
            case 12:
            case 13:
                int count4 = this.mContactsProvider.deleteData(id, PHONE_MIME_TYPES);
                return count4;
            case 14:
            case 15:
                int count5 = this.mContactsProvider.deleteData(id, EXTENSION_MIME_TYPES);
                return count5;
            case 18:
            case 19:
                int count6 = this.mContactsProvider.deleteGroup(uri, id, false);
                return count6;
            case 20:
            case 21:
                int count7 = this.mContactsProvider.deleteData(id, GROUP_MEMBERSHIP_MIME_TYPES);
                return count7;
            case 24:
                this.mValues.clear();
                this.mValues.putNull("data");
                updatePhoto(id, this.mValues);
                return 0;
            case 25:
            case 26:
                int count8 = this.mContactsProvider.deleteData(id, PHOTO_MIME_TYPES);
                return count8;
        }
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, String limit) {
        ensureDefaultAccount();
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        int match = sUriMatcher.match(uri);
        switch (match) {
            case 1:
                qb.setTables("view_v1_people people  LEFT OUTER JOIN presence ON (presence.presence_data_id=(SELECT MAX(presence_data_id) FROM presence WHERE people._id = presence_raw_contact_id) )");
                qb.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(qb);
                break;
            case 2:
                qb.setTables("view_v1_people people  LEFT OUTER JOIN presence ON (presence.presence_data_id=(SELECT MAX(presence_data_id) FROM presence WHERE people._id = presence_raw_contact_id) )");
                qb.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 3:
            case 27:
            case 28:
            default:
                throw new IllegalArgumentException(this.mDbHelper.exceptionMessage(uri));
            case 4:
                qb.setTables("view_v1_organizations organizations");
                qb.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(qb);
                break;
            case 5:
                qb.setTables("view_v1_organizations organizations");
                qb.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 6:
                qb.setTables("view_v1_contact_methods contact_methods");
                qb.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND kind IS NOT NULL");
                break;
            case 7:
                qb.setTables("view_v1_contact_methods contact_methods");
                qb.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(3));
                qb.appendWhere(" AND kind IS NOT NULL");
                break;
            case 8:
                qb.setTables("view_v1_contact_methods contact_methods");
                qb.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(qb);
                break;
            case 9:
                qb.setTables("view_v1_contact_methods contact_methods");
                qb.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 10:
                qb.setTables("view_v1_phones phones");
                qb.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 11:
                qb.setTables("view_v1_phones phones");
                qb.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(3));
                break;
            case 12:
                qb.setTables("view_v1_phones phones");
                qb.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(qb);
                break;
            case 13:
                qb.setTables("view_v1_phones phones");
                qb.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 14:
                qb.setTables("view_v1_extensions extensions");
                qb.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(qb);
                break;
            case 15:
                qb.setTables("view_v1_extensions extensions");
                qb.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 16:
                qb.setTables("view_v1_extensions extensions");
                qb.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 17:
                qb.setTables("view_v1_extensions extensions");
                qb.setProjectionMap(sExtensionProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(3));
                break;
            case 18:
                qb.setTables("view_v1_groups groups");
                qb.setProjectionMap(sGroupProjectionMap);
                applyGroupAccount(qb);
                break;
            case 19:
                qb.setTables("view_v1_groups groups");
                qb.setProjectionMap(sGroupProjectionMap);
                applyGroupAccount(qb);
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 20:
                qb.setTables("view_v1_group_membership groupmembership");
                qb.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(qb);
                break;
            case 21:
                qb.setTables("view_v1_group_membership groupmembership");
                qb.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 22:
                qb.setTables("view_v1_group_membership groupmembership");
                qb.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 23:
                qb.setTables("view_v1_group_membership groupmembership");
                qb.setProjectionMap(sGroupMembershipProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(3));
                break;
            case 24:
                qb.setTables("view_v1_photos photos");
                qb.setProjectionMap(sPhotoProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                limit = "1";
                break;
            case 25:
                qb.setTables("view_v1_photos photos");
                qb.setProjectionMap(sPhotoProjectionMap);
                applyRawContactsAccount(qb);
                break;
            case 26:
                qb.setTables("view_v1_photos photos");
                qb.setProjectionMap(sPhotoProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 29:
                qb.setTables("view_v1_people people  LEFT OUTER JOIN presence ON (presence.presence_data_id=(SELECT MAX(presence_data_id) FROM presence WHERE people._id = presence_raw_contact_id) )");
                qb.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(qb);
                String filterParam = uri.getPathSegments().get(2);
                qb.appendWhere(" AND _id IN " + getRawContactsByFilterAsNestedQuery(filterParam));
                break;
            case 30:
            case 31:
                throw new UnsupportedOperationException(this.mDbHelper.exceptionMessage(uri));
            case 32:
                Cursor c = this.mGlobalSearchSupport.handleSearchSuggestionsQuery(db, uri, projection, limit, null);
                return c;
            case 33:
                String lookupKey = uri.getLastPathSegment();
                String filter = ContactsProvider2.getQueryParameter(uri, "filter");
                Cursor c2 = this.mGlobalSearchSupport.handleSearchShortcutRefresh(db, projection, lookupKey, filter, null);
                return c2;
            case 34:
                qb.setTables("view_v1_phones phones");
                qb.setProjectionMap(sPhoneProjectionMap);
                applyRawContactsAccount(qb);
                if (uri.getPathSegments().size() > 2) {
                    String filterParam2 = uri.getLastPathSegment();
                    qb.appendWhere(" AND person =");
                    qb.appendWhere(this.mDbHelper.buildPhoneLookupAsNestedQuery(filterParam2));
                    qb.setDistinct(true);
                }
                break;
            case 35:
                Cursor c3 = this.mContactsProvider.query(LIVE_FOLDERS_CONTACTS_URI, projection, selection, selectionArgs, sortOrder);
                return c3;
            case 36:
                Cursor c4 = this.mContactsProvider.query(Uri.withAppendedPath(LIVE_FOLDERS_CONTACTS_URI, Uri.encode(uri.getLastPathSegment())), projection, selection, selectionArgs, sortOrder);
                return c4;
            case 37:
                Cursor c5 = this.mContactsProvider.query(LIVE_FOLDERS_CONTACTS_WITH_PHONES_URI, projection, selection, selectionArgs, sortOrder);
                return c5;
            case 38:
                Cursor c6 = this.mContactsProvider.query(LIVE_FOLDERS_CONTACTS_FAVORITES_URI, projection, selection, selectionArgs, sortOrder);
                return c6;
            case 39:
                qb.setTables("view_v1_contact_methods contact_methods");
                qb.setProjectionMap(sContactMethodProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND kind=1");
                break;
            case 40:
                qb.setTables("view_v1_people people  LEFT OUTER JOIN presence ON (presence.presence_data_id=(SELECT MAX(presence_data_id) FROM presence WHERE people._id = presence_raw_contact_id) )");
                qb.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(qb);
                String group = uri.getPathSegments().get(2);
                qb.appendWhere(" AND " + buildGroupNameMatchWhereClause(group));
                break;
            case 41:
                qb.setTables("view_v1_people people  LEFT OUTER JOIN presence ON (presence.presence_data_id=(SELECT MAX(presence_data_id) FROM presence WHERE people._id = presence_raw_contact_id) )");
                qb.setProjectionMap(sPeopleProjectionMap);
                applyRawContactsAccount(qb);
                String systemId = uri.getPathSegments().get(2);
                qb.appendWhere(" AND " + buildGroupSystemIdMatchWhereClause(systemId));
                break;
            case 42:
                qb.setTables("view_v1_organizations organizations");
                qb.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                break;
            case 43:
                qb.setTables("view_v1_organizations organizations");
                qb.setProjectionMap(sOrganizationProjectionMap);
                applyRawContactsAccount(qb);
                qb.appendWhere(" AND person=");
                qb.appendWhere(uri.getPathSegments().get(1));
                qb.appendWhere(" AND _id=");
                qb.appendWhere(uri.getPathSegments().get(3));
                break;
            case 44:
                copySettingsToLegacySettings();
                qb.setTables("v1_settings");
                break;
        }
        Cursor c7 = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);
        if (c7 != null) {
            c7.setNotificationUri(this.mContext.getContentResolver(), Contacts.CONTENT_URI);
            return c7;
        }
        return c7;
    }

    private void applyRawContactsAccount(SQLiteQueryBuilder qb) {
        StringBuilder sb = new StringBuilder();
        appendRawContactsAccount(sb);
        qb.appendWhere(sb.toString());
    }

    private void appendRawContactsAccount(StringBuilder sb) {
        if (this.mAccount != null) {
            sb.append("account_name=");
            DatabaseUtils.appendEscapedSQLString(sb, this.mAccount.name);
            sb.append(" AND account_type=");
            DatabaseUtils.appendEscapedSQLString(sb, this.mAccount.type);
            return;
        }
        sb.append("account_name IS NULL AND account_type IS NULL");
    }

    private void applyGroupAccount(SQLiteQueryBuilder qb) {
        StringBuilder sb = new StringBuilder();
        appendGroupAccount(sb);
        qb.appendWhere(sb.toString());
    }

    private void appendGroupAccount(StringBuilder sb) {
        if (this.mAccount != null) {
            sb.append("account_name=");
            DatabaseUtils.appendEscapedSQLString(sb, this.mAccount.name);
            sb.append(" AND account_type=");
            DatabaseUtils.appendEscapedSQLString(sb, this.mAccount.type);
            return;
        }
        sb.append("account_name IS NULL AND account_type IS NULL");
    }

    private String buildGroupNameMatchWhereClause(String groupName) {
        return "people._id IN (SELECT data.raw_contact_id FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) WHERE mimetype='vnd.android.cursor.item/group_membership' AND data1=(SELECT groups._id FROM groups WHERE title=" + DatabaseUtils.sqlEscapeString(groupName) + "))";
    }

    private String buildGroupSystemIdMatchWhereClause(String systemId) {
        return "people._id IN (SELECT data.raw_contact_id FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) WHERE mimetype='vnd.android.cursor.item/group_membership' AND data1=(SELECT groups._id FROM groups WHERE system_id=" + DatabaseUtils.sqlEscapeString(systemId) + "))";
    }

    private String getRawContactsByFilterAsNestedQuery(String filterParam) {
        StringBuilder sb = new StringBuilder();
        String normalizedName = NameNormalizer.normalize(filterParam);
        if (TextUtils.isEmpty(normalizedName)) {
            sb.append("(0)");
        } else {
            sb.append("(SELECT raw_contact_id FROM name_lookup WHERE normalized_name GLOB '");
            sb.append(normalizedName);
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
        int match = sUriMatcher.match(uri);
        switch (match) {
            case 1:
                return "vnd.android.cursor.dir/person";
            case 2:
                return "vnd.android.cursor.item/person";
            case 3:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            default:
                throw new IllegalArgumentException(this.mDbHelper.exceptionMessage(uri));
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
            case 24:
                return "vnd.android.cursor.item/photo";
            case 25:
                return "vnd.android.cursor.dir/photo";
            case 26:
                return "vnd.android.cursor.item/photo";
            case 32:
                return "vnd.android.cursor.dir/vnd.android.search.suggest";
            case 33:
                return "vnd.android.cursor.item/vnd.android.search.suggest";
            case 34:
                return "vnd.android.cursor.dir/phone";
        }
    }

    private String getContactMethodType(Uri url) {
        String mime = null;
        Cursor c = query(url, new String[]{"kind"}, null, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int kind = c.getInt(0);
                    switch (kind) {
                        case 1:
                            mime = "vnd.android.cursor.item/email";
                            break;
                        case 2:
                            mime = "vnd.android.cursor.item/postal-address";
                            break;
                        case 3:
                            mime = "vnd.android.cursor.item/jabber-im";
                            break;
                    }
                }
            } finally {
                c.close();
            }
        }
        return mime;
    }
}
