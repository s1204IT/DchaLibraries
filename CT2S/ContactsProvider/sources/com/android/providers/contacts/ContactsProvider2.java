package com.android.providers.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.IContentService;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncAdapterType;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.AbstractCursor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.common.content.ProjectionMap;
import com.android.common.io.MoreCloseables;
import com.android.internal.util.ArrayUtils;
import com.android.providers.contacts.ContactLookupKey;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.DataRowHandler;
import com.android.providers.contacts.PhotoStore;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.ContactAggregator;
import com.android.providers.contacts.aggregation.ProfileAggregator;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;
import com.android.providers.contacts.database.ContactsTableUtil;
import com.android.providers.contacts.database.DeletedContactsTableUtil;
import com.android.providers.contacts.database.MoreDatabaseUtils;
import com.android.providers.contacts.util.Clock;
import com.android.providers.contacts.util.DbQueryUtils;
import com.android.providers.contacts.util.UserUtils;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import com.google.android.collect.Sets;
import com.google.common.base.Preconditions;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import libcore.io.IoUtils;

public class ContactsProvider2 extends AbstractContactsProvider implements OnAccountsUpdateListener {
    private static final String[] DISTINCT_DATA_PROHIBITING_COLUMNS;
    private static final String[] EMPTY_STRING_ARRAY;
    private static final String[] PROJECTION_GROUP_ID;
    private static final List<Integer> SOCIAL_STREAM_URIS;
    private static final ProjectionMap sAggregationExceptionsProjectionMap;
    private static final ProjectionMap sContactPresenceColumns;
    private static final ProjectionMap sContactsColumns;
    private static final ProjectionMap sContactsPresenceColumns;
    private static final ProjectionMap sContactsProjectionMap;
    private static final ProjectionMap sContactsProjectionWithSnippetMap;
    private static final ProjectionMap sContactsVCardProjectionMap;
    private static final ProjectionMap sCountProjectionMap;
    private static final ProjectionMap sDataColumns;
    private static final ProjectionMap sDataPresenceColumns;
    private static final ProjectionMap sDataProjectionMap;
    private static final ProjectionMap sDataSipLookupProjectionMap;
    private static final ProjectionMap sDataUsageColumns;
    private static final ProjectionMap sDeletedContactsProjectionMap;
    private static final ProjectionMap sDirectoryProjectionMap;
    private static final ProjectionMap sDistinctDataProjectionMap;
    private static final ProjectionMap sDistinctDataSipLookupProjectionMap;
    private static final ProjectionMap sEntityProjectionMap;
    private static final ProjectionMap sGroupsProjectionMap;
    private static final ProjectionMap sGroupsSummaryProjectionMap;
    private static final ProjectionMap sPhoneLookupProjectionMap;
    private static final ProjectionMap sRawContactColumns;
    private static final ProjectionMap sRawContactSyncColumns;
    private static final ProjectionMap sRawContactsProjectionMap;
    private static final ProjectionMap sRawEntityProjectionMap;
    private static final ProjectionMap sSettingsProjectionMap;
    private static final ProjectionMap sSipLookupColumns;
    private static final ProjectionMap sSnippetColumns;
    private static final ProjectionMap sStatusUpdatesProjectionMap;
    private static final ProjectionMap sStreamItemPhotosProjectionMap;
    private static final ProjectionMap sStreamItemsProjectionMap;
    private static final ProjectionMap sStrequentFrequentProjectionMap;
    private static final ProjectionMap sStrequentPhoneOnlyProjectionMap;
    private static final ProjectionMap sStrequentStarredProjectionMap;
    private Account mAccount;
    private boolean mAccountUpdateListenerRegistered;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private CommonNicknameCache mCommonNicknameCache;
    private ContactAggregator mContactAggregator;
    private ContactDirectoryManager mContactDirectoryManager;
    private int mContactsAccountCount;
    private ContactsDatabaseHelper mContactsHelper;
    private PhotoStore mContactsPhotoStore;
    private LocaleSet mCurrentLocales;
    private HashMap<String, DataRowHandler> mDataRowHandlers;
    private FastScrollingIndexCache mFastScrollingIndexCache;
    private int mFastScrollingIndexCacheMissCount;
    private int mFastScrollingIndexCacheRequestCount;
    private GlobalSearchSupport mGlobalSearchSupport;
    private boolean mIsPhone;
    private boolean mIsPhoneInitialized;
    private LegacyApiSupport mLegacyApiSupport;
    private NameLookupBuilder mNameLookupBuilder;
    private NameSplitter mNameSplitter;
    private PostalSplitter mPostalSplitter;
    private long mPreAuthorizedUriDuration;
    private ContactAggregator mProfileAggregator;
    private HashMap<String, DataRowHandler> mProfileDataRowHandlers;
    private ProfileDatabaseHelper mProfileHelper;
    private PhotoStore mProfilePhotoStore;
    private ProfileProvider mProfileProvider;
    private boolean mProviderStatusUpdateNeeded;
    private volatile CountDownLatch mReadAccessLatch;
    private SearchIndexManager mSearchIndexManager;
    private boolean mSyncToNetwork;
    private long mTotalTimeFastScrollingIndexGenerate;
    private volatile CountDownLatch mWriteAccessLatch;
    private static final ProfileAwareUriMatcher sUriMatcher = new ProfileAwareUriMatcher(-1);
    private static final Map<Integer, String> INSERT_URI_ID_VALUE_MAP = Maps.newHashMap();
    private final StringBuilder mSb = new StringBuilder();
    private final String[] mSelectionArgs1 = new String[1];
    private final String[] mSelectionArgs2 = new String[2];
    private final String[] mSelectionArgs3 = new String[3];
    private final String[] mSelectionArgs4 = new String[4];
    private final ArrayList<String> mSelectionArgs = Lists.newArrayList();
    private final ThreadLocal<ContactsTransaction> mTransactionHolder = new ThreadLocal<>();
    private final ThreadLocal<Boolean> mInProfileMode = new ThreadLocal<>();
    private final ThreadLocal<ContactsDatabaseHelper> mDbHelper = new ThreadLocal<>();
    private final ThreadLocal<ContactAggregator> mAggregator = new ThreadLocal<>();
    private final ThreadLocal<PhotoStore> mPhotoStore = new ThreadLocal<>();
    private final TransactionContext mContactTransactionContext = new TransactionContext(false);
    private final TransactionContext mProfileTransactionContext = new TransactionContext(true);
    private final ThreadLocal<TransactionContext> mTransactionContext = new ThreadLocal<>();
    private final Map<Uri, Long> mPreAuthorizedUris = Maps.newHashMap();
    private final SecureRandom mRandom = new SecureRandom();
    private final HashMap<String, Boolean> mAccountWritability = Maps.newHashMap();
    private HashMap<String, DirectoryInfo> mDirectoryCache = new HashMap<>();
    private boolean mDirectoryCacheValid = false;
    private HashMap<String, ArrayList<GroupIdCacheEntry>> mGroupIdCache = Maps.newHashMap();
    private int mProviderStatus = 0;
    private long mEstimatedStorageRequirement = 0;
    private boolean mOkToOpenAccess = true;
    private boolean mVisibleTouched = false;
    private long mLastPhotoCleanup = 0;

    private static final class AddressBookIndexQuery {
        public static final String[] COLUMNS = {"name", "bucket", "label", "count"};
    }

    private interface DataContactsQuery {
        public static final String[] PROJECTION = {"raw_contacts._id", "accounts.account_type", "accounts.account_name", "accounts.data_set", "data._id", "contacts._id"};
    }

    private interface DataUsageStatQuery {
        public static final String[] COLUMNS = {"stat_id"};
    }

    private static final class DirectoryQuery {
        public static final String[] COLUMNS = {"_id", "authority", "accountName", "accountType"};
    }

    private interface GroupAccountQuery {
        public static final String[] COLUMNS = {"_id", "account_type", "account_name", "data_set"};
    }

    public static class GroupIdCacheEntry {
        long accountId;
        long groupId;
        String sourceId;
    }

    private interface LookupByDisplayNameQuery {
        public static final String[] COLUMNS = {"contact_id", "account_type_and_data_set", "account_name", "normalized_name"};
    }

    private interface LookupByRawContactIdQuery {
        public static final String[] COLUMNS = {"contact_id", "account_type_and_data_set", "account_name", "_id"};
    }

    private interface LookupBySourceIdQuery {
        public static final String[] COLUMNS = {"contact_id", "account_type_and_data_set", "account_name", "sourceid"};
    }

    interface RawContactsQuery {
        public static final String[] COLUMNS = {"deleted", "account_id", "accounts.account_type", "accounts.account_name", "accounts.data_set"};
    }

    static {
        INSERT_URI_ID_VALUE_MAP.put(3000, "raw_contact_id");
        INSERT_URI_ID_VALUE_MAP.put(2004, "raw_contact_id");
        INSERT_URI_ID_VALUE_MAP.put(7000, "presence_data_id");
        INSERT_URI_ID_VALUE_MAP.put(21000, "raw_contact_id");
        INSERT_URI_ID_VALUE_MAP.put(2007, "raw_contact_id");
        INSERT_URI_ID_VALUE_MAP.put(21001, "stream_item_id");
        INSERT_URI_ID_VALUE_MAP.put(21003, "stream_item_id");
        SOCIAL_STREAM_URIS = Lists.newArrayList(new Integer[]{1022, 1023, 1024, 2007, 2008, 21000, 21001, 21002, 21003, 21004});
        PROJECTION_GROUP_ID = new String[]{"groups._id"};
        DISTINCT_DATA_PROHIBITING_COLUMNS = new String[]{"_id", "raw_contact_id", "name_raw_contact_id", "account_name", "account_type", "data_set", "account_type_and_data_set", "dirty", "name_verified", "sourceid", "version"};
        sContactsColumns = ProjectionMap.builder().add("custom_ringtone").add("display_name").add("display_name_alt").add("display_name_source").add("in_default_directory").add("in_visible_group").add("last_time_contacted").add("lookup").add("phonetic_name").add("phonetic_name_style").add("photo_id").add("photo_file_id").add("photo_uri").add("photo_thumb_uri").add("send_to_voicemail").add("sort_key_alt").add("sort_key").add("phonebook_label").add("phonebook_bucket").add("phonebook_label_alt").add("phonebook_bucket_alt").add("starred").add("pinned").add("times_contacted").add("has_phone_number").add("contact_last_updated_timestamp").build();
        sContactsPresenceColumns = ProjectionMap.builder().add("contact_presence", "agg_presence.mode").add("contact_chat_capability", "agg_presence.chat_capability").add("contact_status", "contacts_status_updates.status").add("contact_status_ts", "contacts_status_updates.status_ts").add("contact_status_res_package", "contacts_status_updates.status_res_package").add("contact_status_label", "contacts_status_updates.status_label").add("contact_status_icon", "contacts_status_updates.status_icon").build();
        sSnippetColumns = ProjectionMap.builder().add("snippet").build();
        sRawContactColumns = ProjectionMap.builder().add("account_name").add("account_type").add("data_set").add("account_type_and_data_set").add("dirty").add("name_verified").add("sourceid").add("version").build();
        sRawContactSyncColumns = ProjectionMap.builder().add("sync1").add("sync2").add("sync3").add("sync4").build();
        sDataColumns = ProjectionMap.builder().add("data1").add("data2").add("data3").add("data4").add("data5").add("data6").add("data7").add("data8").add("data9").add("data10").add("data11").add("data12").add("data13").add("data14").add("data15").add("data_version").add("is_primary").add("is_super_primary").add("mimetype").add("res_package").add("data_sync1").add("data_sync2").add("data_sync3").add("data_sync4").add("group_sourceid").build();
        sContactPresenceColumns = ProjectionMap.builder().add("contact_presence", "agg_presence.mode").add("contact_chat_capability", "agg_presence.chat_capability").add("contact_status", "contacts_status_updates.status").add("contact_status_ts", "contacts_status_updates.status_ts").add("contact_status_res_package", "contacts_status_updates.status_res_package").add("contact_status_label", "contacts_status_updates.status_label").add("contact_status_icon", "contacts_status_updates.status_icon").build();
        sDataPresenceColumns = ProjectionMap.builder().add("mode", "presence.mode").add("chat_capability", "presence.chat_capability").add("status", "status_updates.status").add("status_ts", "status_updates.status_ts").add("status_res_package", "status_updates.status_res_package").add("status_label", "status_updates.status_label").add("status_icon", "status_updates.status_icon").build();
        sDataUsageColumns = ProjectionMap.builder().add("times_used", "data_usage_stat.times_used").add("last_time_used", "data_usage_stat.last_time_used").build();
        sCountProjectionMap = ProjectionMap.builder().add("_count", "COUNT(*)").build();
        sContactsProjectionMap = ProjectionMap.builder().add("_id").add("has_phone_number").add("name_raw_contact_id").add("is_user_profile").add("account_type").addAll(sContactsColumns).addAll(sContactsPresenceColumns).build();
        sContactsProjectionWithSnippetMap = ProjectionMap.builder().addAll(sContactsProjectionMap).addAll(sSnippetColumns).build();
        sStrequentStarredProjectionMap = ProjectionMap.builder().addAll(sContactsProjectionMap).add("times_used", String.valueOf(Long.MAX_VALUE)).add("last_time_used", String.valueOf(Long.MAX_VALUE)).build();
        sStrequentFrequentProjectionMap = ProjectionMap.builder().addAll(sContactsProjectionMap).add("times_used", "SUM(data_usage_stat.times_used)").add("last_time_used", "MAX(data_usage_stat.last_time_used)").build();
        sStrequentPhoneOnlyProjectionMap = ProjectionMap.builder().addAll(sContactsProjectionMap).add("times_used", "data_usage_stat.times_used").add("last_time_used", "data_usage_stat.last_time_used").add("data1").add("data2").add("data3").add("is_super_primary").add("contact_id").add("is_user_profile", "NULL").build();
        sContactsVCardProjectionMap = ProjectionMap.builder().add("_id").add("_display_name", "display_name || '.vcf'").add("_size", "NULL").build();
        sRawContactsProjectionMap = ProjectionMap.builder().add("_id").add("contact_id").add("deleted").add("display_name").add("display_name_alt").add("display_name_source").add("phonetic_name").add("phonetic_name_style").add("sort_key").add("sort_key_alt").add("phonebook_label").add("phonebook_bucket").add("phonebook_label_alt").add("phonebook_bucket_alt").add("times_contacted").add("last_time_contacted").add("custom_ringtone").add("send_to_voicemail").add("starred").add("pinned").add("aggregation_mode").add("raw_contact_is_user_profile").addAll(sRawContactColumns).addAll(sRawContactSyncColumns).build();
        sRawEntityProjectionMap = ProjectionMap.builder().add("_id").add("contact_id").add("data_id").add("deleted").add("starred").add("raw_contact_is_user_profile").addAll(sRawContactColumns).addAll(sRawContactSyncColumns).addAll(sDataColumns).build();
        sEntityProjectionMap = ProjectionMap.builder().add("_id").add("contact_id").add("raw_contact_id").add("data_id").add("name_raw_contact_id").add("deleted").add("is_user_profile").addAll(sContactsColumns).addAll(sContactPresenceColumns).addAll(sRawContactColumns).addAll(sRawContactSyncColumns).addAll(sDataColumns).addAll(sDataPresenceColumns).addAll(sDataUsageColumns).build();
        sSipLookupColumns = ProjectionMap.builder().add("number", "data1").add("type", "0").add("label", "NULL").add("normalized_number", "NULL").build();
        sDataProjectionMap = ProjectionMap.builder().add("_id").add("raw_contact_id").add("contact_id").add("name_raw_contact_id").add("raw_contact_is_user_profile").addAll(sDataColumns).addAll(sDataPresenceColumns).addAll(sRawContactColumns).addAll(sContactsColumns).addAll(sContactPresenceColumns).addAll(sDataUsageColumns).build();
        sDataSipLookupProjectionMap = ProjectionMap.builder().addAll(sDataProjectionMap).addAll(sSipLookupColumns).build();
        sDistinctDataProjectionMap = ProjectionMap.builder().add("_id", "MIN(_id)").add("contact_id").add("raw_contact_is_user_profile").add("account_type").addAll(sDataColumns).addAll(sDataPresenceColumns).addAll(sContactsColumns).addAll(sContactPresenceColumns).addAll(sDataUsageColumns).build();
        sDistinctDataSipLookupProjectionMap = ProjectionMap.builder().addAll(sDistinctDataProjectionMap).addAll(sSipLookupColumns).build();
        sPhoneLookupProjectionMap = ProjectionMap.builder().add("_id", "contacts_view._id").add("lookup", "contacts_view.lookup").add("display_name", "contacts_view.display_name").add("last_time_contacted", "contacts_view.last_time_contacted").add("times_contacted", "contacts_view.times_contacted").add("starred", "contacts_view.starred").add("in_default_directory", "contacts_view.in_default_directory").add("in_visible_group", "contacts_view.in_visible_group").add("photo_id", "contacts_view.photo_id").add("photo_file_id", "contacts_view.photo_file_id").add("photo_uri", "contacts_view.photo_uri").add("photo_thumb_uri", "contacts_view.photo_thumb_uri").add("custom_ringtone", "contacts_view.custom_ringtone").add("has_phone_number", "contacts_view.has_phone_number").add("send_to_voicemail", "contacts_view.send_to_voicemail").add("number", "data1").add("type", "data2").add("label", "data3").add("normalized_number", "data4").build();
        sGroupsProjectionMap = ProjectionMap.builder().add("_id").add("account_name").add("account_type").add("data_set").add("account_type_and_data_set").add("sourceid").add("dirty").add("version").add("res_package").add("title").add("title_res").add("group_visible").add("system_id").add("deleted").add("notes").add("should_sync").add("favorites").add("auto_add").add("group_is_read_only").add("sync1").add("sync2").add("sync3").add("sync4").build();
        sDeletedContactsProjectionMap = ProjectionMap.builder().add("contact_id").add("contact_deleted_timestamp").build();
        sGroupsSummaryProjectionMap = ProjectionMap.builder().addAll(sGroupsProjectionMap).add("summ_count", "ifnull(group_member_count, 0)").add("summ_phones", "(SELECT COUNT(contacts._id) FROM contacts INNER JOIN raw_contacts ON (raw_contacts.contact_id=contacts._id) INNER JOIN data ON (data.data1=groups._id AND data.raw_contact_id=raw_contacts._id AND data.mimetype_id=(SELECT _id FROM mimetypes WHERE mimetypes.mimetype='vnd.android.cursor.item/group_membership')) WHERE has_phone_number)").add("group_count_per_account", "0").build();
        sAggregationExceptionsProjectionMap = ProjectionMap.builder().add("_id", "agg_exceptions._id").add("type").add("raw_contact_id1").add("raw_contact_id2").build();
        sSettingsProjectionMap = ProjectionMap.builder().add("account_name").add("account_type").add("data_set").add("ungrouped_visible").add("should_sync").add("any_unsynced", "(CASE WHEN MIN(should_sync,(SELECT (CASE WHEN MIN(should_sync) IS NULL THEN 1 ELSE MIN(should_sync) END) FROM view_groups WHERE view_groups.account_name=settings.account_name AND view_groups.account_type=settings.account_type AND ((view_groups.data_set IS NULL AND settings.data_set IS NULL) OR (view_groups.data_set=settings.data_set))))=0 THEN 1 ELSE 0 END)").add("summ_count", "(SELECT COUNT(*) FROM (SELECT 1 FROM settings LEFT OUTER JOIN raw_contacts ON (raw_contacts.account_id=(SELECT accounts._id FROM accounts WHERE (accounts.account_name=settings.account_name) AND (accounts.account_type=settings.account_type)))LEFT OUTER JOIN data ON (data.mimetype_id=? AND data.raw_contact_id = raw_contacts._id) LEFT OUTER JOIN contacts ON (raw_contacts.contact_id = contacts._id) GROUP BY settings.account_name,settings.account_type,contact_id HAVING COUNT(data.data1) == 0))").add("summ_phones", "(SELECT COUNT(*) FROM (SELECT 1 FROM settings LEFT OUTER JOIN raw_contacts ON (raw_contacts.account_id=(SELECT accounts._id FROM accounts WHERE (accounts.account_name=settings.account_name) AND (accounts.account_type=settings.account_type)))LEFT OUTER JOIN data ON (data.mimetype_id=? AND data.raw_contact_id = raw_contacts._id) LEFT OUTER JOIN contacts ON (raw_contacts.contact_id = contacts._id) WHERE has_phone_number GROUP BY settings.account_name,settings.account_type,contact_id HAVING COUNT(data.data1) == 0))").build();
        sStatusUpdatesProjectionMap = ProjectionMap.builder().add("presence_raw_contact_id").add("presence_data_id", "data._id").add("im_account").add("im_handle").add("protocol").add("custom_protocol", "(CASE WHEN custom_protocol='' THEN NULL ELSE custom_protocol END)").add("mode").add("chat_capability").add("status").add("status_ts").add("status_res_package").add("status_icon").add("status_label").build();
        sStreamItemsProjectionMap = ProjectionMap.builder().add("_id").add("contact_id").add("contact_lookup").add("account_name").add("account_type").add("data_set").add("raw_contact_id").add("raw_contact_source_id").add("res_package").add("icon").add("label").add("text").add("timestamp").add("comments").add("stream_item_sync1").add("stream_item_sync2").add("stream_item_sync3").add("stream_item_sync4").build();
        sStreamItemPhotosProjectionMap = ProjectionMap.builder().add("_id", "stream_item_photos._id").add("raw_contact_id").add("raw_contact_source_id", "raw_contacts.sourceid").add("stream_item_id").add("sort_index").add("photo_file_id").add("photo_uri", "'" + ContactsContract.DisplayPhoto.CONTENT_URI + "'||'/'||photo_file_id").add("height").add("width").add("filesize").add("stream_item_photo_sync1").add("stream_item_photo_sync2").add("stream_item_photo_sync3").add("stream_item_photo_sync4").build();
        sDirectoryProjectionMap = ProjectionMap.builder().add("_id").add("packageName").add("typeResourceId").add("displayName").add("authority").add("accountType").add("accountName").add("exportSupport").add("shortcutSupport").add("photoSupport").build();
        EMPTY_STRING_ARRAY = new String[0];
        UriMatcher matcher = sUriMatcher;
        matcher.addURI("com.android.contacts", "contacts", 1000);
        matcher.addURI("com.android.contacts", "contacts/#", 1001);
        matcher.addURI("com.android.contacts", "contacts/#/data", 1004);
        matcher.addURI("com.android.contacts", "contacts/#/entities", 1019);
        matcher.addURI("com.android.contacts", "contacts/#/suggestions", 8000);
        matcher.addURI("com.android.contacts", "contacts/#/suggestions/*", 8000);
        matcher.addURI("com.android.contacts", "contacts/#/photo", 1009);
        matcher.addURI("com.android.contacts", "contacts/#/display_photo", 1012);
        matcher.addURI("com.android.contacts", "contacts_corp/#/photo", 1027);
        matcher.addURI("com.android.contacts", "contacts_corp/#/display_photo", 1028);
        matcher.addURI("com.android.contacts", "contacts/#/stream_items", 1022);
        matcher.addURI("com.android.contacts", "contacts/filter", 1005);
        matcher.addURI("com.android.contacts", "contacts/filter/*", 1005);
        matcher.addURI("com.android.contacts", "contacts/lookup/*", 1002);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/data", 1017);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/photo", 1010);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/#", 1003);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/#/data", 1018);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/#/photo", 1011);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/display_photo", 1013);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/#/display_photo", 1014);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/entities", 1020);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/#/entities", 1021);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/stream_items", 1023);
        matcher.addURI("com.android.contacts", "contacts/lookup/*/#/stream_items", 1024);
        matcher.addURI("com.android.contacts", "contacts/as_vcard/*", 1015);
        matcher.addURI("com.android.contacts", "contacts/as_multi_vcard/*", 1016);
        matcher.addURI("com.android.contacts", "contacts/strequent/", 1006);
        matcher.addURI("com.android.contacts", "contacts/strequent/filter/*", 1007);
        matcher.addURI("com.android.contacts", "contacts/group/*", 1008);
        matcher.addURI("com.android.contacts", "contacts/frequent", 1025);
        matcher.addURI("com.android.contacts", "contacts/delete_usage", 1026);
        matcher.addURI("com.android.contacts", "raw_contacts", 2002);
        matcher.addURI("com.android.contacts", "raw_contacts/#", 2003);
        matcher.addURI("com.android.contacts", "raw_contacts/#/data", 2004);
        matcher.addURI("com.android.contacts", "raw_contacts/#/display_photo", 2006);
        matcher.addURI("com.android.contacts", "raw_contacts/#/entity", 2005);
        matcher.addURI("com.android.contacts", "raw_contacts/#/stream_items", 2007);
        matcher.addURI("com.android.contacts", "raw_contacts/#/stream_items/#", 2008);
        matcher.addURI("com.android.contacts", "raw_contact_entities", 15001);
        matcher.addURI("com.android.contacts", "raw_contacts_distinct", 15002);
        matcher.addURI("com.android.contacts", "data", 3000);
        matcher.addURI("com.android.contacts", "data/#", 3001);
        matcher.addURI("com.android.contacts", "data/phones", 3002);
        matcher.addURI("com.android.contacts", "data/phones/#", 3003);
        matcher.addURI("com.android.contacts", "data/phones/filter", 3004);
        matcher.addURI("com.android.contacts", "data/phones/filter/*", 3004);
        matcher.addURI("com.android.contacts", "data/emails", 3005);
        matcher.addURI("com.android.contacts", "data/emails/#", 3006);
        matcher.addURI("com.android.contacts", "data/emails/lookup", 3007);
        matcher.addURI("com.android.contacts", "data/emails/lookup/*", 3007);
        matcher.addURI("com.android.contacts", "data/emails/filter", 3008);
        matcher.addURI("com.android.contacts", "data/emails/filter/*", 3008);
        matcher.addURI("com.android.contacts", "data/postals", 3009);
        matcher.addURI("com.android.contacts", "data/postals/#", 3010);
        matcher.addURI("com.android.contacts", "data/usagefeedback/*", 20001);
        matcher.addURI("com.android.contacts", "data/callables/", 3011);
        matcher.addURI("com.android.contacts", "data/callables/#", 3012);
        matcher.addURI("com.android.contacts", "data/callables/filter", 3013);
        matcher.addURI("com.android.contacts", "data/callables/filter/*", 3013);
        matcher.addURI("com.android.contacts", "data/contactables/", 3014);
        matcher.addURI("com.android.contacts", "data/contactables/filter", 3015);
        matcher.addURI("com.android.contacts", "data/contactables/filter/*", 3015);
        matcher.addURI("com.android.contacts", "groups", 10000);
        matcher.addURI("com.android.contacts", "groups/#", 10001);
        matcher.addURI("com.android.contacts", "groups_summary", 10003);
        matcher.addURI("com.android.contacts", "syncstate", 11000);
        matcher.addURI("com.android.contacts", "syncstate/#", 11001);
        matcher.addURI("com.android.contacts", "profile/syncstate", 11002);
        matcher.addURI("com.android.contacts", "profile/syncstate/#", 11003);
        matcher.addURI("com.android.contacts", "phone_lookup/*", 4000);
        matcher.addURI("com.android.contacts", "phone_lookup_enterprise/*", 4001);
        matcher.addURI("com.android.contacts", "aggregation_exceptions", 6000);
        matcher.addURI("com.android.contacts", "aggregation_exceptions/*", 6001);
        matcher.addURI("com.android.contacts", "settings", 9000);
        matcher.addURI("com.android.contacts", "status_updates", 7000);
        matcher.addURI("com.android.contacts", "status_updates/#", 7001);
        matcher.addURI("com.android.contacts", "search_suggest_query", 12001);
        matcher.addURI("com.android.contacts", "search_suggest_query/*", 12001);
        matcher.addURI("com.android.contacts", "search_suggest_shortcut/*", 12002);
        matcher.addURI("com.android.contacts", "provider_status", 16001);
        matcher.addURI("com.android.contacts", "directories", 17001);
        matcher.addURI("com.android.contacts", "directories/#", 17002);
        matcher.addURI("com.android.contacts", "complete_name", 18000);
        matcher.addURI("com.android.contacts", "profile", 19000);
        matcher.addURI("com.android.contacts", "profile/entities", 19001);
        matcher.addURI("com.android.contacts", "profile/data", 19002);
        matcher.addURI("com.android.contacts", "profile/data/#", 19003);
        matcher.addURI("com.android.contacts", "profile/photo", 19011);
        matcher.addURI("com.android.contacts", "profile/display_photo", 19012);
        matcher.addURI("com.android.contacts", "profile/as_vcard", 19004);
        matcher.addURI("com.android.contacts", "profile/raw_contacts", 19005);
        matcher.addURI("com.android.contacts", "profile/raw_contacts/#", 19006);
        matcher.addURI("com.android.contacts", "profile/raw_contacts/#/data", 19007);
        matcher.addURI("com.android.contacts", "profile/raw_contacts/#/entity", 19008);
        matcher.addURI("com.android.contacts", "profile/status_updates", 19009);
        matcher.addURI("com.android.contacts", "profile/raw_contact_entities", 19010);
        matcher.addURI("com.android.contacts", "stream_items", 21000);
        matcher.addURI("com.android.contacts", "stream_items/photo", 21001);
        matcher.addURI("com.android.contacts", "stream_items/#", 21002);
        matcher.addURI("com.android.contacts", "stream_items/#/photo", 21003);
        matcher.addURI("com.android.contacts", "stream_items/#/photo/#", 21004);
        matcher.addURI("com.android.contacts", "stream_items_limit", 21005);
        matcher.addURI("com.android.contacts", "display_photo/#", 22000);
        matcher.addURI("com.android.contacts", "photo_dimensions", 22001);
        matcher.addURI("com.android.contacts", "deleted_contacts", 23000);
        matcher.addURI("com.android.contacts", "deleted_contacts/#", 23001);
    }

    private static class DirectoryInfo {
        String accountName;
        String accountType;
        String authority;

        private DirectoryInfo() {
        }
    }

    @Override
    public boolean onCreate() {
        boolean zInitialize;
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "ContactsProvider2.onCreate start");
        }
        super.onCreate();
        setAppOps(4, 5);
        try {
            try {
                zInitialize = initialize();
            } catch (RuntimeException e) {
                Log.e("ContactsProvider", "Cannot start provider", e);
                if (shouldThrowExceptionForInitializationError()) {
                    throw e;
                }
                zInitialize = false;
                if (Log.isLoggable("ContactsPerf", 3)) {
                    Log.d("ContactsPerf", "ContactsProvider2.onCreate finish");
                }
            }
            return zInitialize;
        } finally {
            if (Log.isLoggable("ContactsPerf", 3)) {
                Log.d("ContactsPerf", "ContactsProvider2.onCreate finish");
            }
        }
    }

    protected boolean shouldThrowExceptionForInitializationError() {
        return false;
    }

    private boolean initialize() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
        this.mFastScrollingIndexCache = FastScrollingIndexCache.getInstance(getContext());
        this.mContactsHelper = getDatabaseHelper(getContext());
        this.mDbHelper.set(this.mContactsHelper);
        setDbHelperToSerializeOn(this.mContactsHelper, "contacts", this);
        this.mContactDirectoryManager = new ContactDirectoryManager(this);
        this.mGlobalSearchSupport = new GlobalSearchSupport(this);
        this.mReadAccessLatch = new CountDownLatch(1);
        this.mWriteAccessLatch = new CountDownLatch(1);
        this.mBackgroundThread = new HandlerThread("ContactsProviderWorker", 10);
        this.mBackgroundThread.start();
        this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                ContactsProvider2.this.performBackgroundTask(msg.what, msg.obj);
            }
        };
        this.mProfileProvider = newProfileProvider();
        this.mProfileProvider.setDbHelperToSerializeOn(this.mContactsHelper, "contacts", this);
        ProviderInfo profileInfo = new ProviderInfo();
        profileInfo.readPermission = "android.permission.READ_PROFILE";
        profileInfo.writePermission = "android.permission.WRITE_PROFILE";
        profileInfo.authority = "com.android.contacts";
        this.mProfileProvider.attachInfo(getContext(), profileInfo);
        this.mProfileHelper = this.mProfileProvider.getDatabaseHelper(getContext());
        this.mPreAuthorizedUriDuration = 300000L;
        scheduleBackgroundTask(0);
        scheduleBackgroundTask(3);
        scheduleBackgroundTask(4);
        scheduleBackgroundTask(5);
        scheduleBackgroundTask(6);
        scheduleBackgroundTask(7);
        scheduleBackgroundTask(1);
        scheduleBackgroundTask(10);
        scheduleBackgroundTask(11);
        return true;
    }

    private static LocaleSet updateLocaleSet(LocaleSet oldLocales, Locale newLocale) {
        Locale prevLocale = oldLocales.getPrimaryLocale();
        return newLocale.equals(prevLocale) ? oldLocales : new LocaleSet(newLocale, prevLocale).normalize();
    }

    private static LocaleSet getProviderPrefLocales(SharedPreferences prefs) {
        String providerLocaleString = prefs.getString("locale", null);
        return LocaleSet.getLocaleSet(providerLocaleString);
    }

    private LocaleSet getLocaleSet() {
        Locale curLocale = getLocale();
        if (this.mCurrentLocales != null) {
            return updateLocaleSet(this.mCurrentLocales, curLocale);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return updateLocaleSet(getProviderPrefLocales(prefs), curLocale);
    }

    private static LocaleSet getLocaleSet(SharedPreferences prefs, Locale curLocale) {
        return updateLocaleSet(getProviderPrefLocales(prefs), curLocale);
    }

    private void initForDefaultLocale() {
        Context context = getContext();
        this.mLegacyApiSupport = new LegacyApiSupport(context, this.mContactsHelper, this, this.mGlobalSearchSupport);
        this.mCurrentLocales = getLocaleSet();
        this.mNameSplitter = this.mContactsHelper.createNameSplitter(this.mCurrentLocales.getPrimaryLocale());
        this.mNameLookupBuilder = new StructuredNameLookupBuilder(this.mNameSplitter);
        this.mPostalSplitter = new PostalSplitter(this.mCurrentLocales.getPrimaryLocale());
        this.mCommonNicknameCache = new CommonNicknameCache(this.mContactsHelper.getReadableDatabase());
        ContactLocaleUtils.setLocales(this.mCurrentLocales);
        this.mContactAggregator = new ContactAggregator(this, this.mContactsHelper, createPhotoPriorityResolver(context), this.mNameSplitter, this.mCommonNicknameCache);
        this.mContactAggregator.setEnabled(SystemProperties.getBoolean("sync.contacts.aggregate", true));
        this.mProfileAggregator = new ProfileAggregator(this, this.mProfileHelper, createPhotoPriorityResolver(context), this.mNameSplitter, this.mCommonNicknameCache);
        this.mProfileAggregator.setEnabled(SystemProperties.getBoolean("sync.contacts.aggregate", true));
        this.mSearchIndexManager = new SearchIndexManager(this);
        this.mContactsPhotoStore = new PhotoStore(getContext().getFilesDir(), this.mContactsHelper);
        this.mProfilePhotoStore = new PhotoStore(new File(getContext().getFilesDir(), "profile"), this.mProfileHelper);
        this.mDataRowHandlers = new HashMap<>();
        initDataRowHandlers(this.mDataRowHandlers, this.mContactsHelper, this.mContactAggregator, this.mContactsPhotoStore);
        this.mProfileDataRowHandlers = new HashMap<>();
        initDataRowHandlers(this.mProfileDataRowHandlers, this.mProfileHelper, this.mProfileAggregator, this.mProfilePhotoStore);
        switchToContactMode();
    }

    private void initDataRowHandlers(Map<String, DataRowHandler> handlerMap, ContactsDatabaseHelper dbHelper, ContactAggregator contactAggregator, PhotoStore photoStore) {
        Context context = getContext();
        handlerMap.put("vnd.android.cursor.item/email_v2", new DataRowHandlerForEmail(context, dbHelper, contactAggregator));
        handlerMap.put("vnd.android.cursor.item/im", new DataRowHandlerForIm(context, dbHelper, contactAggregator));
        handlerMap.put("vnd.android.cursor.item/organization", new DataRowHandlerForOrganization(context, dbHelper, contactAggregator));
        handlerMap.put("vnd.android.cursor.item/phone_v2", new DataRowHandlerForPhoneNumber(context, dbHelper, contactAggregator));
        handlerMap.put("vnd.android.cursor.item/nickname", new DataRowHandlerForNickname(context, dbHelper, contactAggregator));
        handlerMap.put("vnd.android.cursor.item/name", new DataRowHandlerForStructuredName(context, dbHelper, contactAggregator, this.mNameSplitter, this.mNameLookupBuilder));
        handlerMap.put("vnd.android.cursor.item/postal-address_v2", new DataRowHandlerForStructuredPostal(context, dbHelper, contactAggregator, this.mPostalSplitter));
        handlerMap.put("vnd.android.cursor.item/group_membership", new DataRowHandlerForGroupMembership(context, dbHelper, contactAggregator, this.mGroupIdCache));
        handlerMap.put("vnd.android.cursor.item/photo", new DataRowHandlerForPhoto(context, dbHelper, contactAggregator, photoStore, getMaxDisplayPhotoDim(), getMaxThumbnailDim()));
        handlerMap.put("vnd.android.cursor.item/note", new DataRowHandlerForNote(context, dbHelper, contactAggregator));
        handlerMap.put("vnd.android.cursor.item/identity", new DataRowHandlerForIdentity(context, dbHelper, contactAggregator));
    }

    PhotoPriorityResolver createPhotoPriorityResolver(Context context) {
        return new PhotoPriorityResolver(context);
    }

    protected void scheduleBackgroundTask(int task) {
        this.mBackgroundHandler.sendEmptyMessage(task);
    }

    protected void scheduleBackgroundTask(int task, Object arg) {
        this.mBackgroundHandler.sendMessage(this.mBackgroundHandler.obtainMessage(task, arg));
    }

    protected void performBackgroundTask(int task, Object arg) {
        switchToContactMode();
        switch (task) {
            case 0:
                initForDefaultLocale();
                this.mReadAccessLatch.countDown();
                this.mReadAccessLatch = null;
                break;
            case 1:
                if (this.mOkToOpenAccess) {
                    this.mWriteAccessLatch.countDown();
                    this.mWriteAccessLatch = null;
                }
                break;
            case 3:
                Context context = getContext();
                if (!this.mAccountUpdateListenerRegistered) {
                    AccountManager.get(context).addOnAccountsUpdatedListener(this, null, false);
                    this.mAccountUpdateListenerRegistered = true;
                }
                Account[] accounts = AccountManager.get(context).getAccounts();
                switchToContactMode();
                boolean accountsChanged = updateAccountsInBackground(accounts);
                switchToProfileMode();
                boolean accountsChanged2 = accountsChanged | updateAccountsInBackground(accounts);
                switchToContactMode();
                updateContactsAccountCount(accounts);
                updateDirectoriesInBackground(accountsChanged2);
                break;
            case 4:
                updateLocaleInBackground();
                break;
            case 5:
                if (isAggregationUpgradeNeeded()) {
                    upgradeAggregationAlgorithmInBackground();
                    invalidateFastScrollingIndexCache();
                }
                break;
            case 6:
                updateSearchIndexInBackground();
                break;
            case 7:
                updateProviderStatus();
                break;
            case 8:
                if (arg != null) {
                    this.mContactDirectoryManager.onPackageChanged((String) arg);
                }
                break;
            case 9:
                changeLocaleInBackground();
                break;
            case 10:
                long now = System.currentTimeMillis();
                if (now - this.mLastPhotoCleanup > 86400000) {
                    this.mLastPhotoCleanup = now;
                    switchToContactMode();
                    cleanupPhotoStore();
                    switchToProfileMode();
                    cleanupPhotoStore();
                    switchToContactMode();
                }
                break;
            case 11:
                SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
                DeletedContactsTableUtil.deleteOldLogs(db);
                break;
        }
    }

    public void onLocaleChanged() {
        if (this.mProviderStatus == 0 || this.mProviderStatus == 4) {
            scheduleBackgroundTask(9);
        }
    }

    private static boolean needsToUpdateLocaleData(SharedPreferences prefs, LocaleSet locales, ContactsDatabaseHelper contactsHelper, ProfileDatabaseHelper profileHelper) {
        String providerLocales = prefs.getString("locale", null);
        if (locales.toString().equals(providerLocales)) {
            return contactsHelper.needsToUpdateLocaleData(locales) || profileHelper.needsToUpdateLocaleData(locales);
        }
        Log.i("ContactsProvider", "Locale has changed from " + providerLocales + " to " + locales);
        return true;
    }

    protected void updateLocaleInBackground() {
        if (this.mProviderStatus != 3) {
            LocaleSet currentLocales = this.mCurrentLocales;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            if (needsToUpdateLocaleData(prefs, currentLocales, this.mContactsHelper, this.mProfileHelper)) {
                int providerStatus = this.mProviderStatus;
                setProviderStatus(3);
                this.mContactsHelper.setLocale(currentLocales);
                this.mProfileHelper.setLocale(currentLocales);
                this.mSearchIndexManager.updateIndex(true);
                prefs.edit().putString("locale", currentLocales.toString()).commit();
                setProviderStatus(providerStatus);
            }
        }
    }

    protected static void updateLocaleOffline(Context context, ContactsDatabaseHelper contactsHelper, ProfileDatabaseHelper profileHelper) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        LocaleSet currentLocales = getLocaleSet(prefs, Locale.getDefault());
        if (needsToUpdateLocaleData(prefs, currentLocales, contactsHelper, profileHelper)) {
            contactsHelper.setLocale(currentLocales);
            profileHelper.setLocale(currentLocales);
            contactsHelper.rebuildSearchIndex();
            prefs.edit().putString("locale", currentLocales.toString()).commit();
        }
    }

    private void changeLocaleInBackground() {
        SQLiteDatabase db = this.mContactsHelper.getWritableDatabase();
        SQLiteDatabase profileDb = this.mProfileHelper.getWritableDatabase();
        db.beginTransaction();
        profileDb.beginTransaction();
        try {
            initForDefaultLocale();
            db.setTransactionSuccessful();
            profileDb.setTransactionSuccessful();
            db.endTransaction();
            profileDb.endTransaction();
            updateLocaleInBackground();
        } catch (Throwable th) {
            db.endTransaction();
            profileDb.endTransaction();
            throw th;
        }
    }

    protected void updateSearchIndexInBackground() {
        this.mSearchIndexManager.updateIndex(false);
    }

    protected void updateDirectoriesInBackground(boolean rescan) {
        this.mContactDirectoryManager.scanAllPackages(rescan);
    }

    private void updateProviderStatus() {
        if (this.mProviderStatus == 0 || this.mProviderStatus == 4) {
            if (this.mContactsAccountCount == 0) {
                boolean isContactsEmpty = DatabaseUtils.queryIsEmpty(this.mContactsHelper.getReadableDatabase(), "contacts");
                long profileNum = DatabaseUtils.queryNumEntries(this.mProfileHelper.getReadableDatabase(), "contacts", null);
                if (isContactsEmpty && profileNum <= 1) {
                    setProviderStatus(4);
                    return;
                } else {
                    setProviderStatus(0);
                    return;
                }
            }
            setProviderStatus(0);
        }
    }

    protected void cleanupPhotoStore() {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        long photoMimeTypeId = this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/photo");
        Cursor c = db.query("view_data", new String[]{"_id", "data14"}, "mimetype_id=" + photoMimeTypeId + " AND data14 IS NOT NULL", null, null, null, null);
        Set<Long> usedPhotoFileIds = Sets.newHashSet();
        Map<Long, Long> photoFileIdToDataId = Maps.newHashMap();
        while (c.moveToNext()) {
            try {
                long dataId = c.getLong(0);
                long photoFileId = c.getLong(1);
                usedPhotoFileIds.add(Long.valueOf(photoFileId));
                photoFileIdToDataId.put(Long.valueOf(photoFileId), Long.valueOf(dataId));
            } finally {
            }
        }
        c.close();
        c = db.query("stream_item_photos JOIN stream_items ON stream_item_id=stream_items._id", new String[]{"stream_item_photos._id", "stream_item_photos.stream_item_id", "photo_file_id"}, null, null, null, null, null);
        Map<Long, Long> photoFileIdToStreamItemPhotoId = Maps.newHashMap();
        Map<Long, Long> streamItemPhotoIdToStreamItemId = Maps.newHashMap();
        while (c.moveToNext()) {
            try {
                long streamItemPhotoId = c.getLong(0);
                long streamItemId = c.getLong(1);
                long photoFileId2 = c.getLong(2);
                usedPhotoFileIds.add(Long.valueOf(photoFileId2));
                photoFileIdToStreamItemPhotoId.put(Long.valueOf(photoFileId2), Long.valueOf(streamItemPhotoId));
                streamItemPhotoIdToStreamItemId.put(Long.valueOf(streamItemPhotoId), Long.valueOf(streamItemId));
            } finally {
            }
        }
        c.close();
        Set<Long> missingPhotoIds = this.mPhotoStore.get().cleanup(usedPhotoFileIds);
        try {
            if (!missingPhotoIds.isEmpty()) {
                db.beginTransactionWithListener(inProfileMode() ? this.mProfileProvider : this);
                Iterator<Long> it = missingPhotoIds.iterator();
                while (it.hasNext()) {
                    long missingPhotoId = it.next().longValue();
                    if (photoFileIdToDataId.containsKey(Long.valueOf(missingPhotoId))) {
                        long dataId2 = photoFileIdToDataId.get(Long.valueOf(missingPhotoId)).longValue();
                        ContentValues updateValues = new ContentValues();
                        updateValues.putNull("data14");
                        updateData(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId2), updateValues, null, null, false);
                    }
                    if (photoFileIdToStreamItemPhotoId.containsKey(Long.valueOf(missingPhotoId))) {
                        db.delete("stream_item_photos", "_id=?", new String[]{String.valueOf(photoFileIdToStreamItemPhotoId.get(Long.valueOf(missingPhotoId)).longValue())});
                    }
                }
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            Log.e("ContactsProvider", "Failed to clean up outdated photo references", e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    protected ContactsDatabaseHelper getDatabaseHelper(Context context) {
        return ContactsDatabaseHelper.getInstance(context);
    }

    @Override
    protected ThreadLocal<ContactsTransaction> getTransactionHolder() {
        return this.mTransactionHolder;
    }

    public ProfileProvider newProfileProvider() {
        return new ProfileProvider(this);
    }

    PhotoStore getPhotoStore() {
        return this.mContactsPhotoStore;
    }

    PhotoStore getProfilePhotoStore() {
        return this.mProfilePhotoStore;
    }

    public int getMaxThumbnailDim() {
        return PhotoProcessor.getMaxThumbnailSize();
    }

    public int getMaxDisplayPhotoDim() {
        return PhotoProcessor.getMaxDisplayPhotoSize();
    }

    public ContactDirectoryManager getContactDirectoryManagerForTest() {
        return this.mContactDirectoryManager;
    }

    protected Locale getLocale() {
        return Locale.getDefault();
    }

    final boolean inProfileMode() {
        Boolean profileMode = this.mInProfileMode.get();
        return profileMode != null && profileMode.booleanValue();
    }

    void wipeData() {
        invalidateFastScrollingIndexCache();
        this.mContactsHelper.wipeData();
        this.mProfileHelper.wipeData();
        this.mContactsPhotoStore.clear();
        this.mProfilePhotoStore.clear();
        this.mProviderStatus = 4;
        initForDefaultLocale();
    }

    private void waitForAccess(CountDownLatch latch) {
        if (latch == null) {
            return;
        }
        while (true) {
            try {
                latch.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private int getIntValue(ContentValues values, String key, int defaultValue) {
        Integer value = values.getAsInteger(key);
        if (value == null) {
            return defaultValue;
        }
        int defaultValue2 = value.intValue();
        return defaultValue2;
    }

    private boolean flagExists(ContentValues values, String key) {
        return values.getAsInteger(key) != null;
    }

    private boolean flagIsSet(ContentValues values, String key) {
        return getIntValue(values, key, 0) != 0;
    }

    private boolean flagIsClear(ContentValues values, String key) {
        return getIntValue(values, key, 1) == 0;
    }

    private boolean mapsToProfileDb(Uri uri) {
        return sUriMatcher.mapsToProfile(uri);
    }

    private boolean mapsToProfileDbWithInsertedValues(Uri uri, ContentValues values) {
        if (mapsToProfileDb(uri)) {
            return true;
        }
        int match = sUriMatcher.match(uri);
        if (INSERT_URI_ID_VALUE_MAP.containsKey(Integer.valueOf(match))) {
            String idField = INSERT_URI_ID_VALUE_MAP.get(Integer.valueOf(match));
            Long id = values.getAsLong(idField);
            if (id != null && ContactsContract.isProfileId(id.longValue())) {
                return true;
            }
        }
        return false;
    }

    private void switchToProfileMode() {
        this.mDbHelper.set(this.mProfileHelper);
        this.mTransactionContext.set(this.mProfileTransactionContext);
        this.mAggregator.set(this.mProfileAggregator);
        this.mPhotoStore.set(this.mProfilePhotoStore);
        this.mInProfileMode.set(true);
    }

    private void switchToContactMode() {
        this.mDbHelper.set(this.mContactsHelper);
        this.mTransactionContext.set(this.mContactTransactionContext);
        this.mAggregator.set(this.mContactAggregator);
        this.mPhotoStore.set(this.mContactsPhotoStore);
        this.mInProfileMode.set(false);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        waitForAccess(this.mWriteAccessLatch);
        enforceSocialStreamWritePermission(uri);
        if (mapsToProfileDbWithInsertedValues(uri, values)) {
            switchToProfileMode();
            return this.mProfileProvider.insert(uri, values);
        }
        switchToContactMode();
        return super.insert(uri, values);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (this.mWriteAccessLatch != null) {
            int match = sUriMatcher.match(uri);
            if (match == 16001) {
                return 0;
            }
        }
        waitForAccess(this.mWriteAccessLatch);
        enforceSocialStreamWritePermission(uri);
        if (mapsToProfileDb(uri)) {
            switchToProfileMode();
            return this.mProfileProvider.update(uri, values, selection, selectionArgs);
        }
        switchToContactMode();
        return super.update(uri, values, selection, selectionArgs);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        waitForAccess(this.mWriteAccessLatch);
        enforceSocialStreamWritePermission(uri);
        if (mapsToProfileDb(uri)) {
            switchToProfileMode();
            return this.mProfileProvider.delete(uri, selection, selectionArgs);
        }
        switchToContactMode();
        return super.delete(uri, selection, selectionArgs);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        waitForAccess(this.mReadAccessLatch);
        switchToContactMode();
        if ("authorize".equals(method)) {
            Uri uri = (Uri) extras.getParcelable("uri_to_authorize");
            enforceSocialStreamReadPermission(uri);
            if (mapsToProfileDb(uri)) {
                this.mProfileProvider.enforceReadPermission(uri);
            }
            Uri authUri = preAuthorizeUri(uri);
            Bundle response = new Bundle();
            response.putParcelable("authorized_uri", authUri);
            return response;
        }
        if (!"undemote".equals(method)) {
            return null;
        }
        getContext().enforceCallingOrSelfPermission("android.permission.WRITE_CONTACTS", null);
        try {
            long id = Long.valueOf(arg).longValue();
            undemoteContact(this.mDbHelper.get().getWritableDatabase(), id);
            return null;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Contact ID must be a valid long number.");
        }
    }

    private Uri preAuthorizeUri(Uri uri) {
        String token = String.valueOf(this.mRandom.nextLong());
        Uri authUri = uri.buildUpon().appendQueryParameter("perm_token", token).build();
        long expiration = SystemClock.elapsedRealtime() + this.mPreAuthorizedUriDuration;
        this.mPreAuthorizedUris.put(authUri, Long.valueOf(expiration));
        return authUri;
    }

    public boolean isValidPreAuthorizedUri(Uri uri) {
        if (uri.getQueryParameter("perm_token") != null) {
            long now = SystemClock.elapsedRealtime();
            Set<Uri> expiredUris = Sets.newHashSet();
            for (Uri preAuthUri : this.mPreAuthorizedUris.keySet()) {
                if (this.mPreAuthorizedUris.get(preAuthUri).longValue() < now) {
                    expiredUris.add(preAuthUri);
                }
            }
            for (Uri expiredUri : expiredUris) {
                this.mPreAuthorizedUris.remove(expiredUri);
            }
            if (this.mPreAuthorizedUris.containsKey(uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean yield(ContactsTransaction transaction) {
        SQLiteDatabase profileDb = transaction.removeDbForTag("profile");
        if (profileDb != null) {
            profileDb.setTransactionSuccessful();
            profileDb.endTransaction();
        }
        SQLiteDatabase contactsDb = transaction.getDbForTag("contacts");
        return contactsDb != null && contactsDb.yieldIfContendedSafely(4000L);
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        waitForAccess(this.mWriteAccessLatch);
        return super.applyBatch(operations);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        waitForAccess(this.mWriteAccessLatch);
        return super.bulkInsert(uri, values);
    }

    @Override
    public void onBegin() {
        onBeginTransactionInternal(false);
    }

    protected void onBeginTransactionInternal(boolean forProfile) {
        if (forProfile) {
            switchToProfileMode();
            this.mProfileAggregator.clearPendingAggregations();
            this.mProfileTransactionContext.clearExceptSearchIndexUpdates();
        } else {
            switchToContactMode();
            this.mContactAggregator.clearPendingAggregations();
            this.mContactTransactionContext.clearExceptSearchIndexUpdates();
        }
    }

    @Override
    public void onCommit() throws Throwable {
        onCommitTransactionInternal(false);
    }

    protected void onCommitTransactionInternal(boolean forProfile) throws Throwable {
        if (forProfile) {
            switchToProfileMode();
        } else {
            switchToContactMode();
        }
        flushTransactionalChanges();
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        this.mAggregator.get().aggregateInTransaction(this.mTransactionContext.get(), db);
        if (this.mVisibleTouched) {
            this.mVisibleTouched = false;
            this.mDbHelper.get().updateAllVisible();
            invalidateFastScrollingIndexCache();
        }
        updateSearchIndexInTransaction();
        if (this.mProviderStatusUpdateNeeded) {
            updateProviderStatus();
            this.mProviderStatusUpdateNeeded = false;
        }
    }

    @Override
    public void onRollback() {
        onRollbackTransactionInternal(false);
    }

    protected void onRollbackTransactionInternal(boolean forProfile) {
        if (forProfile) {
            switchToProfileMode();
        } else {
            switchToContactMode();
        }
        this.mDbHelper.get().invalidateAllCache();
    }

    private void updateSearchIndexInTransaction() {
        Set<Long> staleContacts = this.mTransactionContext.get().getStaleSearchIndexContactIds();
        Set<Long> staleRawContacts = this.mTransactionContext.get().getStaleSearchIndexRawContactIds();
        if (!staleContacts.isEmpty() || !staleRawContacts.isEmpty()) {
            this.mSearchIndexManager.updateIndexForRawContacts(staleContacts, staleRawContacts);
            this.mTransactionContext.get().clearSearchIndexUpdates();
        }
    }

    private void flushTransactionalChanges() throws Throwable {
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "flushTransactionalChanges: " + (inProfileMode() ? "profile" : "contacts"));
        }
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        Iterator<Long> it = this.mTransactionContext.get().getInsertedRawContactIds().iterator();
        while (it.hasNext()) {
            long rawContactId = it.next().longValue();
            this.mDbHelper.get().updateRawContactDisplayName(db, rawContactId);
            this.mAggregator.get().onRawContactInsert(this.mTransactionContext.get(), db, rawContactId);
        }
        Set<Long> dirtyRawContacts = this.mTransactionContext.get().getDirtyRawContactIds();
        if (!dirtyRawContacts.isEmpty()) {
            this.mSb.setLength(0);
            this.mSb.append("UPDATE raw_contacts SET dirty=1 WHERE _id IN (");
            appendIds(this.mSb, dirtyRawContacts);
            this.mSb.append(")");
            db.execSQL(this.mSb.toString());
        }
        Set<Long> updatedRawContacts = this.mTransactionContext.get().getUpdatedRawContactIds();
        if (!updatedRawContacts.isEmpty()) {
            this.mSb.setLength(0);
            this.mSb.append("UPDATE raw_contacts SET version = version + 1 WHERE _id IN (");
            appendIds(this.mSb, updatedRawContacts);
            this.mSb.append(")");
            db.execSQL(this.mSb.toString());
        }
        Set<Long> changedRawContacts = this.mTransactionContext.get().getChangedRawContactIds();
        ContactsTableUtil.updateContactLastUpdateByRawContactId(db, changedRawContacts);
        for (Map.Entry<Long, Object> entry : this.mTransactionContext.get().getUpdatedSyncStates()) {
            long id = entry.getKey().longValue();
            if (this.mDbHelper.get().getSyncState().update(db, id, entry.getValue()) <= 0) {
                throw new IllegalStateException("unable to update sync state, does it still exist?");
            }
        }
        this.mTransactionContext.get().clearExceptSearchIndexUpdates();
    }

    private void appendIds(StringBuilder sb, Set<Long> ids) {
        Iterator<Long> it = ids.iterator();
        while (it.hasNext()) {
            long id = it.next().longValue();
            sb.append(id).append(',');
        }
        sb.setLength(sb.length() - 1);
    }

    @Override
    protected void notifyChange() {
        notifyChange(this.mSyncToNetwork);
        this.mSyncToNetwork = false;
    }

    protected void notifyChange(boolean syncToNetwork) {
        getContext().getContentResolver().notifyChange(ContactsContract.AUTHORITY_URI, (ContentObserver) null, syncToNetwork);
    }

    protected void setProviderStatus(int status) {
        if (this.mProviderStatus != status) {
            this.mProviderStatus = status;
            getContext().getContentResolver().notifyChange(ContactsContract.ProviderStatus.CONTENT_URI, (ContentObserver) null, false);
        }
    }

    public DataRowHandler getDataRowHandler(String mimeType) {
        if (inProfileMode()) {
            return getDataRowHandlerForProfile(mimeType);
        }
        DataRowHandler handler = this.mDataRowHandlers.get(mimeType);
        if (handler == null) {
            DataRowHandler handler2 = new DataRowHandlerForCustomMimetype(getContext(), this.mContactsHelper, this.mContactAggregator, mimeType);
            this.mDataRowHandlers.put(mimeType, handler2);
            return handler2;
        }
        return handler;
    }

    public DataRowHandler getDataRowHandlerForProfile(String mimeType) {
        DataRowHandler handler = this.mProfileDataRowHandlers.get(mimeType);
        if (handler == null) {
            DataRowHandler handler2 = new DataRowHandlerForCustomMimetype(getContext(), this.mProfileHelper, this.mProfileAggregator, mimeType);
            this.mProfileDataRowHandlers.put(mimeType, handler2);
            return handler2;
        }
        return handler;
    }

    @Override
    protected Uri insertInTransaction(Uri uri, ContentValues values) throws Throwable {
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "insertInTransaction: uri=" + uri + "  values=[" + values + "] CPID=" + Binder.getCallingPid());
        }
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        boolean callerIsSyncAdapter = readBooleanQueryParameter(uri, "caller_is_syncadapter", false);
        int match = sUriMatcher.match(uri);
        long id = 0;
        switch (match) {
            case 1000:
                invalidateFastScrollingIndexCache();
                insertContact(values);
                break;
            case 2002:
            case 19005:
                invalidateFastScrollingIndexCache();
                id = insertRawContact(uri, values, callerIsSyncAdapter);
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                break;
            case 2004:
            case 19007:
                invalidateFastScrollingIndexCache();
                int segment = match == 2004 ? 1 : 2;
                values.put("raw_contact_id", uri.getPathSegments().get(segment));
                id = insertData(values, callerIsSyncAdapter);
                this.mSyncToNetwork |= callerIsSyncAdapter ? false : true;
                break;
            case 2007:
                values.put("raw_contact_id", uri.getPathSegments().get(1));
                id = insertStreamItem(uri, values);
                this.mSyncToNetwork |= callerIsSyncAdapter ? false : true;
                break;
            case 3000:
            case 19002:
                invalidateFastScrollingIndexCache();
                id = insertData(values, callerIsSyncAdapter);
                this.mSyncToNetwork |= callerIsSyncAdapter ? false : true;
                break;
            case 7000:
            case 19009:
                id = insertStatusUpdate(values);
                break;
            case 9000:
                id = insertSettings(values);
                this.mSyncToNetwork |= callerIsSyncAdapter ? false : true;
                break;
            case 10000:
                id = insertGroup(uri, values, callerIsSyncAdapter);
                this.mSyncToNetwork |= callerIsSyncAdapter ? false : true;
                break;
            case 11000:
            case 11002:
                id = this.mDbHelper.get().getSyncState().insert(db, values);
                break;
            case 19000:
                throw new UnsupportedOperationException("The profile contact is created automatically");
            case 21000:
                id = insertStreamItem(uri, values);
                this.mSyncToNetwork |= callerIsSyncAdapter ? false : true;
                break;
            case 21001:
                id = insertStreamItemPhoto(uri, values);
                this.mSyncToNetwork |= callerIsSyncAdapter ? false : true;
                break;
            case 21003:
                values.put("stream_item_id", uri.getPathSegments().get(1));
                id = insertStreamItemPhoto(uri, values);
                this.mSyncToNetwork |= callerIsSyncAdapter ? false : true;
                break;
            default:
                this.mSyncToNetwork = true;
                return this.mLegacyApiSupport.insert(uri, values);
        }
        if (id < 0) {
            return null;
        }
        return ContentUris.withAppendedId(uri, id);
    }

    private Account resolveAccount(Uri uri, ContentValues values) throws IllegalArgumentException {
        String accountName = getQueryParameter(uri, "account_name");
        String accountType = getQueryParameter(uri, "account_type");
        boolean partialUri = TextUtils.isEmpty(accountName) ^ TextUtils.isEmpty(accountType);
        String valueAccountName = values.getAsString("account_name");
        String valueAccountType = values.getAsString("account_type");
        boolean partialValues = TextUtils.isEmpty(valueAccountName) ^ TextUtils.isEmpty(valueAccountType);
        if (partialUri || partialValues) {
            throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Must specify both or neither of ACCOUNT_NAME and ACCOUNT_TYPE", uri));
        }
        boolean validUri = !TextUtils.isEmpty(accountName);
        boolean validValues = !TextUtils.isEmpty(valueAccountName);
        if (validValues && validUri) {
            boolean accountMatch = TextUtils.equals(accountName, valueAccountName) && TextUtils.equals(accountType, valueAccountType);
            if (!accountMatch) {
                throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("When both specified, ACCOUNT_NAME and ACCOUNT_TYPE must match", uri));
            }
        } else if (validUri) {
            values.put("account_name", accountName);
            values.put("account_type", accountType);
        } else if (validValues) {
            accountName = valueAccountName;
            accountType = valueAccountType;
        } else {
            return null;
        }
        if (this.mAccount == null || !this.mAccount.name.equals(accountName) || !this.mAccount.type.equals(accountType)) {
            this.mAccount = new Account(accountName, accountType);
        }
        return this.mAccount;
    }

    private AccountWithDataSet resolveAccountWithDataSet(Uri uri, ContentValues values) {
        Account account = resolveAccount(uri, values);
        if (account == null) {
            return null;
        }
        String dataSet = getQueryParameter(uri, "data_set");
        if (dataSet == null) {
            dataSet = values.getAsString("data_set");
        } else {
            values.put("data_set", dataSet);
        }
        AccountWithDataSet accountWithDataSet = AccountWithDataSet.get(account.name, account.type, dataSet);
        return accountWithDataSet;
    }

    private long insertContact(ContentValues values) {
        throw new UnsupportedOperationException("Aggregate contacts are created automatically");
    }

    private long insertRawContact(Uri uri, ContentValues inputValues, boolean callerIsSyncAdapter) {
        ContentValues values = new ContentValues(inputValues);
        values.putNull("contact_id");
        long accountId = replaceAccountInfoByAccountId(uri, values);
        if (flagIsSet(values, "deleted")) {
            values.put("aggregation_mode", (Integer) 3);
        }
        if (!values.containsKey("pinned")) {
            values.put("pinned", (Integer) 0);
        }
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        long rawContactId = db.insert("raw_contacts", "contact_id", values);
        int aggregationMode = getIntValue(values, "aggregation_mode", 0);
        this.mAggregator.get().markNewForAggregation(rawContactId, aggregationMode);
        this.mTransactionContext.get().rawContactInserted(rawContactId, accountId);
        if (!callerIsSyncAdapter) {
            addAutoAddMembership(rawContactId);
            if (flagIsSet(values, "starred")) {
                updateFavoritesMembership(rawContactId, true);
            }
        }
        this.mProviderStatusUpdateNeeded = true;
        return rawContactId;
    }

    private void addAutoAddMembership(long rawContactId) {
        Long groupId = findGroupByRawContactId("raw_contacts._id=? AND groups.account_id=raw_contacts.account_id AND auto_add != 0", rawContactId);
        if (groupId != null) {
            insertDataGroupMembership(rawContactId, groupId.longValue());
        }
    }

    private Long findGroupByRawContactId(String selection, long rawContactId) {
        Long lValueOf = null;
        SQLiteDatabase db = this.mDbHelper.get().getReadableDatabase();
        Cursor c = db.query("groups,raw_contacts", PROJECTION_GROUP_ID, selection, new String[]{Long.toString(rawContactId)}, null, null, null);
        try {
            if (c.moveToNext()) {
                lValueOf = Long.valueOf(c.getLong(0));
            }
            return lValueOf;
        } finally {
            c.close();
        }
    }

    private void updateFavoritesMembership(long rawContactId, boolean isStarred) {
        Long groupId = findGroupByRawContactId("raw_contacts._id=? AND groups.account_id=raw_contacts.account_id AND favorites != 0", rawContactId);
        if (groupId != null) {
            if (isStarred) {
                insertDataGroupMembership(rawContactId, groupId.longValue());
            } else {
                deleteDataGroupMembership(rawContactId, groupId.longValue());
            }
        }
    }

    private void insertDataGroupMembership(long rawContactId, long groupId) {
        ContentValues groupMembershipValues = new ContentValues();
        groupMembershipValues.put("data1", Long.valueOf(groupId));
        groupMembershipValues.put("raw_contact_id", Long.valueOf(rawContactId));
        groupMembershipValues.put("mimetype_id", Long.valueOf(this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/group_membership")));
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        db.insert("data", null, groupMembershipValues);
    }

    private void deleteDataGroupMembership(long rawContactId, long groupId) {
        String[] selectionArgs = {Long.toString(this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/group_membership")), Long.toString(groupId), Long.toString(rawContactId)};
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        db.delete("data", "mimetype_id=? AND data1=? AND raw_contact_id=?", selectionArgs);
    }

    private long insertData(ContentValues inputValues, boolean callerIsSyncAdapter) {
        Long rawContactId = inputValues.getAsLong("raw_contact_id");
        if (rawContactId == null) {
            throw new IllegalArgumentException("raw_contact_id is required");
        }
        String mimeType = inputValues.getAsString("mimetype");
        if (TextUtils.isEmpty(mimeType)) {
            throw new IllegalArgumentException("mimetype is required");
        }
        if ("vnd.android.cursor.item/phone_v2".equals(mimeType)) {
            maybeTrimLongPhoneNumber(inputValues);
        }
        ContentValues values = new ContentValues(inputValues);
        replacePackageNameByPackageId(values);
        values.put("mimetype_id", Long.valueOf(this.mDbHelper.get().getMimeTypeId(mimeType)));
        values.remove("mimetype");
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        TransactionContext context = this.mTransactionContext.get();
        long dataId = getDataRowHandler(mimeType).insert(db, context, rawContactId.longValue(), values);
        context.markRawContactDirtyAndChanged(rawContactId.longValue(), callerIsSyncAdapter);
        context.rawContactUpdated(rawContactId.longValue());
        return dataId;
    }

    private long insertStreamItem(Uri uri, ContentValues inputValues) {
        Long rawContactId = inputValues.getAsLong("raw_contact_id");
        if (rawContactId == null) {
            throw new IllegalArgumentException("raw_contact_id is required");
        }
        ContentValues values = new ContentValues(inputValues);
        values.remove("account_name");
        values.remove("account_type");
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        long id = db.insert("stream_items", null, values);
        if (id == -1) {
            return 0L;
        }
        return cleanUpOldStreamItems(rawContactId.longValue(), id);
    }

    private long insertStreamItemPhoto(Uri uri, ContentValues inputValues) {
        Long streamItemId = inputValues.getAsLong("stream_item_id");
        if (streamItemId == null || streamItemId.longValue() == 0) {
            return 0L;
        }
        ContentValues values = new ContentValues(inputValues);
        values.remove("account_name");
        values.remove("account_type");
        if (!processStreamItemPhoto(values, false)) {
            return 0L;
        }
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        return db.insert("stream_item_photos", null, values);
    }

    private boolean processStreamItemPhoto(ContentValues values, boolean forUpdate) {
        byte[] photoBytes = values.getAsByteArray("photo");
        if (photoBytes != null) {
            IOException exception = null;
            try {
                PhotoProcessor processor = new PhotoProcessor(photoBytes, getMaxDisplayPhotoDim(), getMaxThumbnailDim(), true);
                long photoFileId = this.mPhotoStore.get().insert(processor, true);
                if (photoFileId != 0) {
                    values.put("photo_file_id", Long.valueOf(photoFileId));
                    values.remove("photo");
                    return true;
                }
            } catch (IOException ioe) {
                exception = ioe;
            }
            Log.e("ContactsProvider", "Could not process stream item photo for insert", exception);
            return false;
        }
        return forUpdate;
    }

    private void enforceSocialStreamReadPermission(Uri uri) {
        if (SOCIAL_STREAM_URIS.contains(Integer.valueOf(sUriMatcher.match(uri))) && !isValidPreAuthorizedUri(uri)) {
            getContext().enforceCallingOrSelfPermission("android.permission.READ_SOCIAL_STREAM", null);
        }
    }

    private void enforceSocialStreamWritePermission(Uri uri) {
        if (SOCIAL_STREAM_URIS.contains(Integer.valueOf(sUriMatcher.match(uri)))) {
            getContext().enforceCallingOrSelfPermission("android.permission.WRITE_SOCIAL_STREAM", null);
        }
    }

    private long cleanUpOldStreamItems(long rawContactId, long insertedStreamItemId) {
        long postCleanupInsertedStreamId = insertedStreamItemId;
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        Cursor c = db.query("stream_items", new String[]{"_id"}, "raw_contact_id=?", new String[]{String.valueOf(rawContactId)}, null, null, "timestamp DESC, _id DESC");
        try {
            int streamItemCount = c.getCount();
            if (streamItemCount > 5) {
                c.moveToLast();
                while (c.getPosition() >= 5) {
                    long streamItemId = c.getLong(0);
                    if (insertedStreamItemId == streamItemId) {
                        postCleanupInsertedStreamId = 0;
                    }
                    deleteStreamItem(db, c.getLong(0));
                    c.moveToPrevious();
                }
                c.close();
                return postCleanupInsertedStreamId;
            }
            return insertedStreamItemId;
        } finally {
            c.close();
        }
    }

    private int deleteData(String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        int count = 0;
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        Uri dataUri = inProfileMode() ? Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, "data") : ContactsContract.Data.CONTENT_URI;
        Cursor c = query(dataUri, DataRowHandler.DataDeleteQuery.COLUMNS, selection, selectionArgs, null);
        while (c.moveToNext()) {
            try {
                long rawContactId = c.getLong(2);
                String mimeType = c.getString(1);
                DataRowHandler rowHandler = getDataRowHandler(mimeType);
                count += rowHandler.delete(db, this.mTransactionContext.get(), c);
                this.mTransactionContext.get().markRawContactDirtyAndChanged(rawContactId, callerIsSyncAdapter);
            } finally {
                c.close();
            }
        }
        return count;
    }

    public int deleteData(long dataId, String[] allowedMimeTypes) {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        this.mSelectionArgs1[0] = String.valueOf(dataId);
        Cursor c = query(ContactsContract.Data.CONTENT_URI, DataRowHandler.DataDeleteQuery.COLUMNS, "_id=?", this.mSelectionArgs1, null);
        try {
            if (c.moveToFirst()) {
                String mimeType = c.getString(1);
                boolean valid = false;
                int len$ = allowedMimeTypes.length;
                int i$ = 0;
                while (true) {
                    if (i$ >= len$) {
                        break;
                    }
                    String type = allowedMimeTypes[i$];
                    if (!TextUtils.equals(mimeType, type)) {
                        i$++;
                    } else {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    throw new IllegalArgumentException("Data type mismatch: expected " + Lists.newArrayList(allowedMimeTypes));
                }
                DataRowHandler rowHandler = getDataRowHandler(mimeType);
                return rowHandler.delete(db, this.mTransactionContext.get(), c);
            }
            return 0;
        } finally {
            c.close();
        }
    }

    private long insertGroup(Uri uri, ContentValues inputValues, boolean callerIsSyncAdapter) {
        ContentValues values = new ContentValues(inputValues);
        long accountId = replaceAccountInfoByAccountId(uri, values);
        replacePackageNameByPackageId(values);
        if (!callerIsSyncAdapter) {
            values.put("dirty", (Integer) 1);
        }
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        long groupId = db.insert("groups", "title", values);
        boolean isFavoritesGroup = flagIsSet(values, "favorites");
        if (!callerIsSyncAdapter && isFavoritesGroup) {
            this.mSelectionArgs1[0] = Long.toString(accountId);
            Cursor c = db.query("raw_contacts", new String[]{"_id", "starred"}, "raw_contacts.account_id=?", this.mSelectionArgs1, null, null, null);
            while (c.moveToNext()) {
                try {
                    if (c.getLong(1) != 0) {
                        long rawContactId = c.getLong(0);
                        insertDataGroupMembership(rawContactId, groupId);
                        this.mTransactionContext.get().markRawContactDirtyAndChanged(rawContactId, callerIsSyncAdapter);
                    }
                } finally {
                    c.close();
                }
            }
        }
        if (values.containsKey("group_visible")) {
            this.mVisibleTouched = true;
        }
        return groupId;
    }

    private long insertSettings(ContentValues values) throws Throwable {
        String accountName = values.getAsString("account_name");
        String accountType = values.getAsString("account_type");
        String dataSet = values.getAsString("data_set");
        Uri.Builder settingsUri = ContactsContract.Settings.CONTENT_URI.buildUpon();
        if (accountName != null) {
            settingsUri.appendQueryParameter("account_name", accountName);
        }
        if (accountType != null) {
            settingsUri.appendQueryParameter("account_type", accountType);
        }
        if (dataSet != null) {
            settingsUri.appendQueryParameter("data_set", dataSet);
        }
        Cursor c = queryLocal(settingsUri.build(), null, null, null, null, 0L, null);
        try {
            if (c.getCount() > 0) {
                String selection = null;
                String[] selectionArgs = null;
                if (accountName != null && accountType != null) {
                    if (dataSet == null) {
                        selection = "account_name=? AND account_type=? AND data_set IS NULL";
                        selectionArgs = new String[]{accountName, accountType};
                    } else {
                        selection = "account_name=? AND account_type=? AND data_set=?";
                        selectionArgs = new String[]{accountName, accountType, dataSet};
                    }
                }
                return updateSettings(values, selection, selectionArgs);
            }
            c.close();
            SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
            long jInsert = db.insert("settings", null, values);
            if (values.containsKey("ungrouped_visible")) {
                this.mVisibleTouched = true;
                return jInsert;
            }
            return jInsert;
        } finally {
            c.close();
        }
    }

    private long insertStatusUpdate(ContentValues inputValues) throws Throwable {
        String handle = inputValues.getAsString("im_handle");
        Integer protocol = inputValues.getAsInteger("protocol");
        String customProtocol = null;
        ContactsDatabaseHelper dbHelper = this.mDbHelper.get();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (protocol != null && protocol.intValue() == -1) {
            customProtocol = inputValues.getAsString("custom_protocol");
            if (TextUtils.isEmpty(customProtocol)) {
                throw new IllegalArgumentException("CUSTOM_PROTOCOL is required when PROTOCOL=PROTOCOL_CUSTOM");
            }
        }
        Long dataId = inputValues.getAsLong("presence_data_id");
        this.mSb.setLength(0);
        this.mSelectionArgs.clear();
        if (dataId != null) {
            this.mSb.append("data._id=?");
            this.mSelectionArgs.add(String.valueOf(dataId));
        } else {
            if (TextUtils.isEmpty(handle) || protocol == null) {
                throw new IllegalArgumentException("PROTOCOL and IM_HANDLE are required");
            }
            boolean matchEmail = 5 == protocol.intValue();
            String mimeTypeIdIm = String.valueOf(dbHelper.getMimeTypeIdForIm());
            if (matchEmail) {
                String mimeTypeIdEmail = String.valueOf(dbHelper.getMimeTypeIdForEmail());
                this.mSb.append("mimetype_id IN (?,?) AND data1=? AND ((mimetype_id=? AND data5=?");
                this.mSelectionArgs.add(mimeTypeIdEmail);
                this.mSelectionArgs.add(mimeTypeIdIm);
                this.mSelectionArgs.add(handle);
                this.mSelectionArgs.add(mimeTypeIdIm);
                this.mSelectionArgs.add(String.valueOf(protocol));
                if (customProtocol != null) {
                    this.mSb.append(" AND data6=?");
                    this.mSelectionArgs.add(customProtocol);
                }
                this.mSb.append(") OR (mimetype_id=?))");
                this.mSelectionArgs.add(mimeTypeIdEmail);
            } else {
                this.mSb.append("mimetype_id=? AND data5=? AND data1=?");
                this.mSelectionArgs.add(mimeTypeIdIm);
                this.mSelectionArgs.add(String.valueOf(protocol));
                this.mSelectionArgs.add(handle);
                if (customProtocol != null) {
                    this.mSb.append(" AND data6=?");
                    this.mSelectionArgs.add(customProtocol);
                }
            }
            String dataID = inputValues.getAsString("presence_data_id");
            if (dataID != null) {
                this.mSb.append(" AND data._id=?");
                this.mSelectionArgs.add(dataID);
            }
        }
        Cursor cursor = null;
        try {
            cursor = db.query("data JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) JOIN accounts ON (accounts._id=raw_contacts.account_id)JOIN contacts ON (raw_contacts.contact_id = contacts._id)", DataContactsQuery.PROJECTION, this.mSb.toString(), (String[]) this.mSelectionArgs.toArray(EMPTY_STRING_ARRAY), null, null, "EXISTS (SELECT _id FROM visible_contacts WHERE contacts._id=visible_contacts._id) DESC, raw_contact_id");
            if (!cursor.moveToFirst()) {
                if (cursor != null) {
                    cursor.close();
                }
                return -1L;
            }
            Long dataId2 = Long.valueOf(cursor.getLong(4));
            try {
                long rawContactId = cursor.getLong(0);
                String accountType = cursor.getString(1);
                String accountName = cursor.getString(2);
                long contactId = cursor.getLong(5);
                if (cursor != null) {
                    cursor.close();
                }
                String presence = inputValues.getAsString("mode");
                if (presence != null) {
                    if (customProtocol == null) {
                        customProtocol = "";
                    }
                    ContentValues values = new ContentValues();
                    values.put("presence_data_id", dataId2);
                    values.put("presence_raw_contact_id", Long.valueOf(rawContactId));
                    values.put("presence_contact_id", Long.valueOf(contactId));
                    values.put("protocol", protocol);
                    values.put("custom_protocol", customProtocol);
                    values.put("im_handle", handle);
                    String imAccount = inputValues.getAsString("im_account");
                    if (imAccount != null) {
                        values.put("im_account", imAccount);
                    }
                    values.put("mode", presence);
                    values.put("chat_capability", inputValues.getAsString("chat_capability"));
                    db.replace("presence", null, values);
                }
                if (inputValues.containsKey("status")) {
                    String status = inputValues.getAsString("status");
                    String resPackage = inputValues.getAsString("status_res_package");
                    Resources resources = getContext().getResources();
                    if (!TextUtils.isEmpty(resPackage)) {
                        PackageManager pm = getContext().getPackageManager();
                        try {
                            resources = pm.getResourcesForApplication(resPackage);
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.w("ContactsProvider", "Contact status update resource package not found: " + resPackage);
                        }
                    }
                    Integer labelResourceId = inputValues.getAsInteger("status_label");
                    if ((labelResourceId == null || labelResourceId.intValue() == 0) && protocol != null) {
                        labelResourceId = Integer.valueOf(ContactsContract.CommonDataKinds.Im.getProtocolLabelResource(protocol.intValue()));
                    }
                    String labelResource = getResourceName(resources, "string", labelResourceId);
                    Integer iconResourceId = inputValues.getAsInteger("status_icon");
                    String iconResource = getResourceName(resources, "drawable", iconResourceId);
                    if (TextUtils.isEmpty(status)) {
                        dbHelper.deleteStatusUpdate(dataId2.longValue());
                    } else {
                        Long timestamp = inputValues.getAsLong("status_ts");
                        if (timestamp != null) {
                            dbHelper.replaceStatusUpdate(dataId2, timestamp.longValue(), status, resPackage, iconResourceId, labelResourceId);
                        } else {
                            dbHelper.insertStatusUpdate(dataId2, status, resPackage, iconResourceId, labelResourceId);
                        }
                        if (rawContactId != -1 && !TextUtils.isEmpty(status)) {
                            ContentValues streamItemValues = new ContentValues();
                            streamItemValues.put("raw_contact_id", Long.valueOf(rawContactId));
                            streamItemValues.put("text", statusUpdateToHtml(status));
                            streamItemValues.put("comments", "");
                            streamItemValues.put("res_package", resPackage);
                            streamItemValues.put("icon", iconResource);
                            streamItemValues.put("label", labelResource);
                            streamItemValues.put("timestamp", Long.valueOf(timestamp == null ? System.currentTimeMillis() : timestamp.longValue()));
                            if (accountName != null && accountType != null) {
                                streamItemValues.put("account_name", accountName);
                                streamItemValues.put("account_type", accountType);
                            }
                            Uri streamUri = ContactsContract.StreamItems.CONTENT_URI;
                            Cursor c = queryLocal(streamUri, new String[]{"_id"}, "raw_contact_id=?", new String[]{String.valueOf(rawContactId)}, null, -1L, null);
                            try {
                                if (c.getCount() > 0) {
                                    c.moveToFirst();
                                    updateInTransaction(ContentUris.withAppendedId(streamUri, c.getLong(0)), streamItemValues, null, null);
                                } else {
                                    insertInTransaction(streamUri, streamItemValues);
                                }
                            } finally {
                                c.close();
                            }
                        }
                    }
                }
                if (contactId != -1) {
                    this.mAggregator.get().updateLastStatusUpdateId(contactId);
                }
                return dataId2.longValue();
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        if (cursor != null) {
            cursor.close();
        }
        throw th;
    }

    private String statusUpdateToHtml(String status) {
        return TextUtils.htmlEncode(status);
    }

    private String getResourceName(Resources resources, String expectedType, Integer resourceId) {
        if (resourceId != null) {
            try {
                if (resourceId.intValue() != 0) {
                    String resourceEntryName = resources.getResourceEntryName(resourceId.intValue());
                    String resourceTypeName = resources.getResourceTypeName(resourceId.intValue());
                    if (!expectedType.equals(resourceTypeName)) {
                        Log.w("ContactsProvider", "Resource " + resourceId + " (" + resourceEntryName + ") is of type " + resourceTypeName + " but " + expectedType + " is required.");
                        return null;
                    }
                    return resourceEntryName;
                }
            } catch (Resources.NotFoundException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    protected int deleteInTransaction(Uri uri, String selection, String[] selectionArgs) throws Throwable {
        Cursor c;
        String[] args;
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "deleteInTransaction: uri=" + uri + "  selection=[" + selection + "]  args=" + Arrays.toString(selectionArgs) + " CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        flushTransactionalChanges();
        boolean callerIsSyncAdapter = readBooleanQueryParameter(uri, "caller_is_syncadapter", false);
        int match = sUriMatcher.match(uri);
        switch (match) {
            case 1000:
                invalidateFastScrollingIndexCache();
                return 0;
            case 1001:
                invalidateFastScrollingIndexCache();
                return deleteContact(ContentUris.parseId(uri), callerIsSyncAdapter);
            case 1002:
                invalidateFastScrollingIndexCache();
                List<String> pathSegments = uri.getPathSegments();
                int segmentCount = pathSegments.size();
                if (segmentCount < 3) {
                    throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Missing a lookup key", uri));
                }
                String lookupKey = pathSegments.get(2);
                return deleteContact(lookupContactIdByLookupKey(db, lookupKey), callerIsSyncAdapter);
            case 1003:
                invalidateFastScrollingIndexCache();
                String lookupKey2 = uri.getPathSegments().get(2);
                SQLiteQueryBuilder lookupQb = new SQLiteQueryBuilder();
                setTablesAndProjectionMapForContacts(lookupQb, null);
                long contactId = ContentUris.parseId(uri);
                if (selectionArgs == null) {
                    args = new String[2];
                } else {
                    args = new String[selectionArgs.length + 2];
                    System.arraycopy(selectionArgs, 0, args, 2, selectionArgs.length);
                }
                args[0] = String.valueOf(contactId);
                args[1] = Uri.encode(lookupKey2);
                lookupQb.appendWhere("_id=? AND lookup=?");
                c = query(db, lookupQb, null, selection, args, null, null, null, null, null);
                try {
                    if (c.getCount() == 1) {
                        return deleteContact(contactId, callerIsSyncAdapter);
                    }
                    return 0;
                } finally {
                }
            case 1026:
                return deleteDataUsage();
            case 2002:
            case 19005:
                invalidateFastScrollingIndexCache();
                int numDeletes = 0;
                c = db.query("view_raw_contacts", new String[]{"_id", "contact_id"}, appendAccountIdToSelection(uri, selection), selectionArgs, null, null, null);
                while (c.moveToNext()) {
                    try {
                        long rawContactId = c.getLong(0);
                        numDeletes += deleteRawContact(rawContactId, c.getLong(1), callerIsSyncAdapter);
                    } finally {
                    }
                    break;
                }
                return numDeletes;
            case 2003:
            case 19006:
                invalidateFastScrollingIndexCache();
                long rawContactId2 = ContentUris.parseId(uri);
                return deleteRawContact(rawContactId2, this.mDbHelper.get().getContactId(rawContactId2), callerIsSyncAdapter);
            case 2008:
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                String rawContactId3 = uri.getPathSegments().get(1);
                String streamItemId = uri.getLastPathSegment();
                return deleteStreamItems("raw_contact_id=? AND _id=?", new String[]{rawContactId3, streamItemId});
            case 3000:
            case 19002:
                invalidateFastScrollingIndexCache();
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                return deleteData(appendAccountToSelection(uri, selection), selectionArgs, callerIsSyncAdapter);
            case 3001:
            case 3003:
            case 3006:
            case 3010:
            case 3012:
            case 19003:
                invalidateFastScrollingIndexCache();
                long dataId = ContentUris.parseId(uri);
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                this.mSelectionArgs1[0] = String.valueOf(dataId);
                return deleteData("_id=?", this.mSelectionArgs1, callerIsSyncAdapter);
            case 7000:
            case 19009:
                return deleteStatusUpdates(selection, selectionArgs);
            case 9000:
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                return deleteSettings(appendAccountToSelection(uri, selection), selectionArgs);
            case 10000:
                int numDeletes2 = 0;
                c = db.query("view_groups", ContactsDatabaseHelper.Projections.ID, appendAccountIdToSelection(uri, selection), selectionArgs, null, null, null);
                while (c.moveToNext()) {
                    try {
                        numDeletes2 += deleteGroup(uri, c.getLong(0), callerIsSyncAdapter);
                    } finally {
                    }
                }
                if (numDeletes2 <= 0) {
                    return numDeletes2;
                }
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                return numDeletes2;
            case 10001:
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                return deleteGroup(uri, ContentUris.parseId(uri), callerIsSyncAdapter);
            case 11000:
            case 11002:
                return this.mDbHelper.get().getSyncState().delete(db, selection, selectionArgs);
            case 11001:
                String selectionWithId = "_id=" + ContentUris.parseId(uri) + " " + (selection == null ? "" : " AND (" + selection + ")");
                return this.mDbHelper.get().getSyncState().delete(db, selectionWithId, selectionArgs);
            case 11003:
                String selectionWithId2 = "_id=" + ContentUris.parseId(uri) + " " + (selection == null ? "" : " AND (" + selection + ")");
                return this.mProfileHelper.getSyncState().delete(db, selectionWithId2, selectionArgs);
            case 21000:
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                return deleteStreamItems(selection, selectionArgs);
            case 21002:
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                return deleteStreamItems("_id=?", new String[]{uri.getLastPathSegment()});
            case 21003:
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                String streamItemId2 = uri.getPathSegments().get(1);
                String selectionWithId3 = "stream_item_id=" + streamItemId2 + " " + (selection == null ? "" : " AND (" + selection + ")");
                return deleteStreamItemPhotos(selectionWithId3, selectionArgs);
            case 21004:
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                String streamItemId3 = uri.getPathSegments().get(1);
                String streamItemPhotoId = uri.getPathSegments().get(3);
                return deleteStreamItemPhotos("stream_item_photos._id=? AND stream_item_id=?", new String[]{streamItemPhotoId, streamItemId3});
            default:
                this.mSyncToNetwork = true;
                return this.mLegacyApiSupport.delete(uri, selection, selectionArgs);
        }
    }

    public int deleteGroup(Uri uri, long groupId, boolean callerIsSyncAdapter) {
        int iUpdate;
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        this.mGroupIdCache.clear();
        long groupMembershipMimetypeId = this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/group_membership");
        db.delete("data", "mimetype_id=" + groupMembershipMimetypeId + " AND data1=" + groupId, null);
        try {
            if (callerIsSyncAdapter) {
                iUpdate = db.delete("groups", "_id=" + groupId, null);
            } else {
                ContentValues values = new ContentValues();
                values.put("deleted", (Integer) 1);
                values.put("dirty", (Integer) 1);
                iUpdate = db.update("groups", values, "_id=" + groupId, null);
                this.mVisibleTouched = true;
            }
            return iUpdate;
        } finally {
            this.mVisibleTouched = true;
        }
    }

    private int deleteSettings(String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        int count = db.delete("settings", selection, selectionArgs);
        this.mVisibleTouched = true;
        return count;
    }

    private int deleteContact(long contactId, boolean callerIsSyncAdapter) {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        this.mSelectionArgs1[0] = Long.toString(contactId);
        Cursor c = db.query("raw_contacts", new String[]{"_id"}, "contact_id=?", this.mSelectionArgs1, null, null, null);
        while (c.moveToNext()) {
            try {
                long rawContactId = c.getLong(0);
                markRawContactAsDeleted(db, rawContactId, callerIsSyncAdapter);
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        this.mProviderStatusUpdateNeeded = true;
        int result = ContactsTableUtil.deleteContact(db, contactId);
        scheduleBackgroundTask(11);
        return result;
    }

    public int deleteRawContact(long rawContactId, long contactId, boolean callerIsSyncAdapter) {
        this.mAggregator.get().invalidateAggregationExceptionCache();
        this.mProviderStatusUpdateNeeded = true;
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        Cursor c = db.query("stream_items", new String[]{"_id"}, "raw_contact_id=?", new String[]{String.valueOf(rawContactId)}, null, null, null);
        while (c.moveToNext()) {
            try {
                deleteStreamItem(db, c.getLong(0));
            } finally {
                c.close();
            }
        }
        if (callerIsSyncAdapter || rawContactIsLocal(rawContactId)) {
            ContactsTableUtil.deleteContactIfSingleton(db, rawContactId);
            db.delete("presence", "presence_raw_contact_id=" + rawContactId, null);
            int count = db.delete("raw_contacts", "_id=" + rawContactId, null);
            this.mAggregator.get().updateAggregateData(this.mTransactionContext.get(), contactId);
            this.mTransactionContext.get().markRawContactChangedOrDeletedOrInserted(rawContactId);
            return count;
        }
        ContactsTableUtil.deleteContactIfSingleton(db, rawContactId);
        int count2 = markRawContactAsDeleted(db, rawContactId, callerIsSyncAdapter);
        return count2;
    }

    private boolean rawContactIsLocal(long rawContactId) {
        SQLiteDatabase db = this.mDbHelper.get().getReadableDatabase();
        Cursor c = db.query("raw_contacts", ContactsDatabaseHelper.Projections.LITERAL_ONE, "raw_contacts._id=? AND account_id=(SELECT _id FROM accounts WHERE account_name IS NULL AND account_type IS NULL AND data_set IS NULL)", new String[]{String.valueOf(rawContactId)}, null, null, null);
        try {
            return c.getCount() > 0;
        } finally {
            c.close();
        }
    }

    private int deleteStatusUpdates(String selection, String[] selectionArgs) {
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "deleting data from status_updates for " + selection);
        }
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        db.delete("status_updates", getWhereClauseForStatusUpdatesTable(selection), selectionArgs);
        return db.delete("presence", selection, selectionArgs);
    }

    private int deleteStreamItems(String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        int count = 0;
        Cursor c = db.query("view_stream_items", ContactsDatabaseHelper.Projections.ID, selection, selectionArgs, null, null, null);
        try {
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                count += deleteStreamItem(db, c.getLong(0));
            }
            return count;
        } finally {
            c.close();
        }
    }

    private int deleteStreamItem(SQLiteDatabase db, long streamItemId) {
        deleteStreamItemPhotos(streamItemId);
        return db.delete("stream_items", "_id=?", new String[]{String.valueOf(streamItemId)});
    }

    private int deleteStreamItemPhotos(String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        return db.delete("stream_item_photos", selection, selectionArgs);
    }

    private int deleteStreamItemPhotos(long streamItemId) {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        return db.delete("stream_item_photos", "stream_item_id=?", new String[]{String.valueOf(streamItemId)});
    }

    private int markRawContactAsDeleted(SQLiteDatabase db, long rawContactId, boolean callerIsSyncAdapter) {
        this.mSyncToNetwork = true;
        ContentValues values = new ContentValues();
        values.put("deleted", (Integer) 1);
        values.put("aggregation_mode", (Integer) 3);
        values.put("aggregation_needed", (Integer) 1);
        values.putNull("contact_id");
        values.put("dirty", (Integer) 1);
        return updateRawContact(db, rawContactId, values, callerIsSyncAdapter);
    }

    private int deleteDataUsage() {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        db.execSQL("UPDATE raw_contacts SET times_contacted=0,last_time_contacted=NULL");
        db.execSQL("UPDATE contacts SET times_contacted=0,last_time_contacted=NULL");
        db.delete("data_usage_stat", null, null);
        return 1;
    }

    @Override
    protected int updateInTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs) throws Throwable {
        int count;
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "updateInTransaction: uri=" + uri + "  selection=[" + selection + "]  args=" + Arrays.toString(selectionArgs) + "  values=[" + values + "] CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        int match = sUriMatcher.match(uri);
        if (match == 11001 && selection == null) {
            long rowId = ContentUris.parseId(uri);
            Object data = values.get("data");
            this.mTransactionContext.get().syncStateUpdated(rowId, data);
            return 1;
        }
        flushTransactionalChanges();
        boolean callerIsSyncAdapter = readBooleanQueryParameter(uri, "caller_is_syncadapter", false);
        switch (match) {
            case 1000:
            case 19000:
                invalidateFastScrollingIndexCache();
                count = updateContactOptions(values, selection, selectionArgs, callerIsSyncAdapter);
                break;
            case 1001:
                invalidateFastScrollingIndexCache();
                count = updateContactOptions(db, ContentUris.parseId(uri), values, callerIsSyncAdapter);
                break;
            case 1002:
            case 1003:
                invalidateFastScrollingIndexCache();
                List<String> pathSegments = uri.getPathSegments();
                int segmentCount = pathSegments.size();
                if (segmentCount < 3) {
                    throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Missing a lookup key", uri));
                }
                String lookupKey = pathSegments.get(2);
                long contactId = lookupContactIdByLookupKey(db, lookupKey);
                count = updateContactOptions(db, contactId, values, callerIsSyncAdapter);
                break;
                break;
            case 2002:
            case 19005:
                invalidateFastScrollingIndexCache();
                count = updateRawContacts(values, appendAccountIdToSelection(uri, selection), selectionArgs, callerIsSyncAdapter);
                break;
            case 2003:
                invalidateFastScrollingIndexCache();
                long rawContactId = ContentUris.parseId(uri);
                if (selection != null) {
                    count = updateRawContacts(values, "_id=? AND(" + selection + ")", insertSelectionArg(selectionArgs, String.valueOf(rawContactId)), callerIsSyncAdapter);
                } else {
                    this.mSelectionArgs1[0] = String.valueOf(rawContactId);
                    count = updateRawContacts(values, "_id=?", this.mSelectionArgs1, callerIsSyncAdapter);
                }
                break;
            case 2004:
            case 19007:
                invalidateFastScrollingIndexCache();
                int segment = match == 2004 ? 1 : 2;
                String rawContactId2 = uri.getPathSegments().get(segment);
                String selectionWithId = "raw_contact_id=" + rawContactId2 + " " + (selection == null ? "" : " AND " + selection);
                count = updateData(uri, values, selectionWithId, selectionArgs, callerIsSyncAdapter);
                break;
            case 2008:
                String rawContactId3 = uri.getPathSegments().get(1);
                String streamItemId = uri.getLastPathSegment();
                count = updateStreamItems(values, "raw_contact_id=? AND _id=?", new String[]{rawContactId3, streamItemId});
                break;
            case 3000:
            case 19002:
                invalidateFastScrollingIndexCache();
                count = updateData(uri, values, appendAccountToSelection(uri, selection), selectionArgs, callerIsSyncAdapter);
                if (count > 0) {
                    this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                }
                break;
            case 3001:
            case 3003:
            case 3006:
            case 3010:
            case 3012:
                invalidateFastScrollingIndexCache();
                count = updateData(uri, values, selection, selectionArgs, callerIsSyncAdapter);
                if (count > 0) {
                    this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                }
                break;
            case 6000:
                count = updateAggregationException(db, values);
                invalidateFastScrollingIndexCache();
                break;
            case 7000:
            case 19009:
                count = updateStatusUpdate(values, selection, selectionArgs);
                break;
            case 9000:
                count = updateSettings(values, appendAccountToSelection(uri, selection), selectionArgs);
                this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                break;
            case 10000:
                count = updateGroups(values, appendAccountIdToSelection(uri, selection), selectionArgs, callerIsSyncAdapter);
                if (count > 0) {
                    this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                }
                break;
            case 10001:
                long groupId = ContentUris.parseId(uri);
                String[] selectionArgs2 = insertSelectionArg(selectionArgs, String.valueOf(groupId));
                String selectionWithId2 = "_id=? " + (selection == null ? "" : " AND " + selection);
                count = updateGroups(values, selectionWithId2, selectionArgs2, callerIsSyncAdapter);
                if (count > 0) {
                    this.mSyncToNetwork = (!callerIsSyncAdapter) | this.mSyncToNetwork;
                }
                break;
            case 11000:
            case 11002:
                return this.mDbHelper.get().getSyncState().update(db, values, appendAccountToSelection(uri, selection), selectionArgs);
            case 11001:
                String selection2 = appendAccountToSelection(uri, selection);
                String selectionWithId3 = "_id=" + ContentUris.parseId(uri) + " " + (selection2 == null ? "" : " AND (" + selection2 + ")");
                return this.mDbHelper.get().getSyncState().update(db, values, selectionWithId3, selectionArgs);
            case 11003:
                String selection3 = appendAccountToSelection(uri, selection);
                String selectionWithId4 = "_id=" + ContentUris.parseId(uri) + " " + (selection3 == null ? "" : " AND (" + selection3 + ")");
                return this.mProfileHelper.getSyncState().update(db, values, selectionWithId4, selectionArgs);
            case 17001:
                this.mContactDirectoryManager.scanPackagesByUid(Binder.getCallingUid());
                count = 1;
                break;
            case 20001:
                count = !handleDataUsageFeedback(uri) ? 0 : 1;
                break;
            case 21000:
                count = updateStreamItems(values, selection, selectionArgs);
                break;
            case 21001:
                count = updateStreamItemPhotos(values, selection, selectionArgs);
                break;
            case 21002:
                count = updateStreamItems(values, "_id=?", new String[]{uri.getLastPathSegment()});
                break;
            case 21003:
                String streamItemId2 = uri.getPathSegments().get(1);
                count = updateStreamItemPhotos(values, "stream_item_id=?", new String[]{streamItemId2});
                break;
            case 21004:
                String streamItemId3 = uri.getPathSegments().get(1);
                String streamItemPhotoId = uri.getPathSegments().get(3);
                count = updateStreamItemPhotos(values, "stream_item_photos._id=? AND stream_item_photos.stream_item_id=?", new String[]{streamItemPhotoId, streamItemId3});
                break;
            default:
                this.mSyncToNetwork = true;
                return this.mLegacyApiSupport.update(uri, values, selection, selectionArgs);
        }
        return count;
    }

    private int updateStatusUpdate(ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        int updateCount = 0;
        ContentValues settableValues = getSettableColumnsForStatusUpdatesTable(values);
        if (settableValues.size() > 0) {
            updateCount = db.update("status_updates", settableValues, getWhereClauseForStatusUpdatesTable(selection), selectionArgs);
        }
        ContentValues settableValues2 = getSettableColumnsForPresenceTable(values);
        if (settableValues2.size() > 0) {
            int updateCount2 = db.update("presence", settableValues2, selection, selectionArgs);
            return updateCount2;
        }
        return updateCount;
    }

    private int updateStreamItems(ContentValues values, String selection, String[] selectionArgs) {
        values.remove("raw_contact_id");
        values.remove("account_name");
        values.remove("account_type");
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        return db.update("stream_items", values, selection, selectionArgs);
    }

    private int updateStreamItemPhotos(ContentValues values, String selection, String[] selectionArgs) {
        values.remove("stream_item_id");
        values.remove("account_name");
        values.remove("account_type");
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        if (processStreamItemPhoto(values, true)) {
            return db.update("stream_item_photos", values, selection, selectionArgs);
        }
        return 0;
    }

    private String getWhereClauseForStatusUpdatesTable(String selection) {
        this.mSb.setLength(0);
        this.mSb.append("status_update_data_id IN (SELECT Distinct presence_data_id FROM status_updates LEFT OUTER JOIN presence ON status_update_data_id = presence_data_id WHERE ");
        this.mSb.append(selection);
        this.mSb.append(")");
        return this.mSb.toString();
    }

    private ContentValues getSettableColumnsForStatusUpdatesTable(ContentValues inputValues) {
        ContentValues values = new ContentValues();
        ContactsDatabaseHelper.copyStringValue(values, "status", inputValues, "status");
        ContactsDatabaseHelper.copyStringValue(values, "status_ts", inputValues, "status_ts");
        ContactsDatabaseHelper.copyStringValue(values, "status_res_package", inputValues, "status_res_package");
        ContactsDatabaseHelper.copyStringValue(values, "status_label", inputValues, "status_label");
        ContactsDatabaseHelper.copyStringValue(values, "status_icon", inputValues, "status_icon");
        return values;
    }

    private ContentValues getSettableColumnsForPresenceTable(ContentValues inputValues) {
        ContentValues values = new ContentValues();
        ContactsDatabaseHelper.copyStringValue(values, "mode", inputValues, "mode");
        ContactsDatabaseHelper.copyStringValue(values, "chat_capability", inputValues, "chat_capability");
        return values;
    }

    private int updateGroups(ContentValues originalValues, String selectionWithId, String[] selectionArgs, boolean callerIsSyncAdapter) {
        this.mGroupIdCache.clear();
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        ContactsDatabaseHelper dbHelper = this.mDbHelper.get();
        ContentValues updatedValues = new ContentValues();
        updatedValues.putAll(originalValues);
        if (!callerIsSyncAdapter && !updatedValues.containsKey("dirty")) {
            updatedValues.put("dirty", (Integer) 1);
        }
        if (updatedValues.containsKey("group_visible")) {
            this.mVisibleTouched = true;
        }
        boolean isAccountNameChanging = updatedValues.containsKey("account_name");
        boolean isAccountTypeChanging = updatedValues.containsKey("account_type");
        boolean isDataSetChanging = updatedValues.containsKey("data_set");
        boolean isAccountChanging = isAccountNameChanging || isAccountTypeChanging || isDataSetChanging;
        String updatedAccountName = updatedValues.getAsString("account_name");
        String updatedAccountType = updatedValues.getAsString("account_type");
        String updatedDataSet = updatedValues.getAsString("data_set");
        updatedValues.remove("account_name");
        updatedValues.remove("account_type");
        updatedValues.remove("data_set");
        Set<Account> affectedAccounts = Sets.newHashSet();
        Cursor c = db.query("view_groups", GroupAccountQuery.COLUMNS, selectionWithId, selectionArgs, null, null, null);
        int returnCount = 0;
        try {
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                long groupId = c.getLong(0);
                this.mSelectionArgs1[0] = Long.toString(groupId);
                String accountName = isAccountNameChanging ? updatedAccountName : c.getString(2);
                String accountType = isAccountTypeChanging ? updatedAccountType : c.getString(1);
                String dataSet = isDataSetChanging ? updatedDataSet : c.getString(3);
                if (isAccountChanging) {
                    long accountId = dbHelper.getOrCreateAccountIdInTransaction(AccountWithDataSet.get(accountName, accountType, dataSet));
                    updatedValues.put("account_id", Long.valueOf(accountId));
                }
                int count = db.update("groups", updatedValues, "groups._id=?", this.mSelectionArgs1);
                if (count > 0 && !TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                    affectedAccounts.add(new Account(accountName, accountType));
                }
                returnCount += count;
            }
            c.close();
            if (flagIsSet(updatedValues, "should_sync")) {
                for (Account account : affectedAccounts) {
                    ContentResolver.requestSync(account, "com.android.contacts", new Bundle());
                }
            }
            return returnCount;
        } catch (Throwable th) {
            c.close();
            throw th;
        }
    }

    private int updateSettings(ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        int count = db.update("settings", values, selection, selectionArgs);
        if (values.containsKey("ungrouped_visible")) {
            this.mVisibleTouched = true;
        }
        return count;
    }

    private int updateRawContacts(ContentValues values, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        if (values.containsKey("contact_id")) {
            throw new IllegalArgumentException("contact_id should not be included in content values. Contact IDs are assigned automatically");
        }
        if (!callerIsSyncAdapter) {
            selection = DatabaseUtils.concatenateWhere(selection, "raw_contact_is_read_only=0");
        }
        int count = 0;
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        Cursor cursor = db.query("view_raw_contacts", ContactsDatabaseHelper.Projections.ID, selection, selectionArgs, null, null, null);
        while (cursor.moveToNext()) {
            try {
                long rawContactId = cursor.getLong(0);
                updateRawContact(db, rawContactId, values, callerIsSyncAdapter);
                count++;
            } finally {
                cursor.close();
            }
        }
        return count;
    }

    private int updateRawContact(SQLiteDatabase db, long rawContactId, ContentValues values, boolean callerIsSyncAdapter) {
        this.mSelectionArgs1[0] = Long.toString(rawContactId);
        ContactsDatabaseHelper dbHelper = this.mDbHelper.get();
        boolean requestUndoDelete = flagIsClear(values, "deleted");
        boolean isAccountNameChanging = values.containsKey("account_name");
        boolean isAccountTypeChanging = values.containsKey("account_type");
        boolean isDataSetChanging = values.containsKey("data_set");
        boolean isAccountChanging = isAccountNameChanging || isAccountTypeChanging || isDataSetChanging;
        int previousDeleted = 0;
        long accountId = 0;
        String oldAccountType = null;
        String oldAccountName = null;
        String oldDataSet = null;
        if (requestUndoDelete || isAccountChanging) {
            Cursor cursor = db.query("raw_contacts JOIN accounts ON (accounts._id=raw_contacts.account_id)", RawContactsQuery.COLUMNS, "raw_contacts._id = ?", this.mSelectionArgs1, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    previousDeleted = cursor.getInt(0);
                    accountId = cursor.getLong(1);
                    oldAccountType = cursor.getString(2);
                    oldAccountName = cursor.getString(3);
                    oldDataSet = cursor.getString(4);
                }
                if (isAccountChanging) {
                    values = new ContentValues();
                    values.clear();
                    values.putAll(values);
                    AccountWithDataSet newAccountWithDataSet = AccountWithDataSet.get(isAccountNameChanging ? values.getAsString("account_name") : oldAccountName, isAccountTypeChanging ? values.getAsString("account_type") : oldAccountType, isDataSetChanging ? values.getAsString("data_set") : oldDataSet);
                    accountId = dbHelper.getOrCreateAccountIdInTransaction(newAccountWithDataSet);
                    values.put("account_id", Long.valueOf(accountId));
                    values.remove("account_name");
                    values.remove("account_type");
                    values.remove("data_set");
                }
            } finally {
                cursor.close();
            }
        }
        if (requestUndoDelete) {
            values.put("aggregation_mode", (Integer) 0);
        }
        int count = db.update("raw_contacts", values, "raw_contacts._id = ?", this.mSelectionArgs1);
        if (count != 0) {
            ContactAggregator aggregator = this.mAggregator.get();
            int aggregationMode = getIntValue(values, "aggregation_mode", 0);
            if (aggregationMode != 0) {
                aggregator.markForAggregation(rawContactId, aggregationMode, false);
            }
            if (flagExists(values, "starred")) {
                if (!callerIsSyncAdapter) {
                    updateFavoritesMembership(rawContactId, flagIsSet(values, "starred"));
                }
                aggregator.updateStarred(rawContactId);
                aggregator.updatePinned(rawContactId);
            } else if (!callerIsSyncAdapter && isAccountChanging) {
                boolean starred = 0 != DatabaseUtils.longForQuery(db, "SELECT starred FROM raw_contacts WHERE _id=?", new String[]{Long.toString(rawContactId)});
                updateFavoritesMembership(rawContactId, starred);
            }
            if (!callerIsSyncAdapter && isAccountChanging) {
                addAutoAddMembership(rawContactId);
            }
            if (values.containsKey("sourceid")) {
                aggregator.updateLookupKeyForRawContact(db, rawContactId);
            }
            if (flagExists(values, "name_verified")) {
                if (flagIsSet(values, "name_verified")) {
                    this.mDbHelper.get().resetNameVerifiedForOtherRawContacts(rawContactId);
                }
                aggregator.updateDisplayNameForRawContact(db, rawContactId);
            }
            if (requestUndoDelete && previousDeleted == 1) {
                this.mTransactionContext.get().rawContactInserted(rawContactId, accountId);
            }
            this.mTransactionContext.get().markRawContactChangedOrDeletedOrInserted(rawContactId);
        }
        return count;
    }

    private int updateData(Uri uri, ContentValues inputValues, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) throws Throwable {
        ContentValues values = new ContentValues(inputValues);
        values.remove("_id");
        values.remove("raw_contact_id");
        values.remove("mimetype");
        String packageName = inputValues.getAsString("res_package");
        if (packageName != null) {
            values.remove("res_package");
            values.put("package_id", Long.valueOf(this.mDbHelper.get().getPackageId(packageName)));
        }
        if (!callerIsSyncAdapter) {
            selection = DatabaseUtils.concatenateWhere(selection, "is_read_only=0");
        }
        int count = 0;
        Cursor c = queryLocal(uri, DataRowHandler.DataUpdateQuery.COLUMNS, selection, selectionArgs, null, -1L, null);
        while (c.moveToNext()) {
            try {
                count += updateData(values, c, callerIsSyncAdapter);
            } finally {
                c.close();
            }
        }
        return count;
    }

    private void maybeTrimLongPhoneNumber(ContentValues values) {
        String data1 = values.getAsString("data1");
        if (data1 != null && data1.length() > 1000) {
            values.put("data1", data1.substring(0, 1000));
        }
    }

    private int updateData(ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        if (values.size() == 0) {
            return 0;
        }
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        String mimeType = c.getString(2);
        if ("vnd.android.cursor.item/phone_v2".equals(mimeType)) {
            maybeTrimLongPhoneNumber(values);
        }
        DataRowHandler rowHandler = getDataRowHandler(mimeType);
        boolean updated = rowHandler.update(db, this.mTransactionContext.get(), values, c, callerIsSyncAdapter);
        if ("vnd.android.cursor.item/photo".equals(mimeType)) {
            scheduleBackgroundTask(10);
        }
        return updated ? 1 : 0;
    }

    private int updateContactOptions(ContentValues values, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        int count = 0;
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        Cursor cursor = db.query("view_contacts", new String[]{"_id"}, selection, selectionArgs, null, null, null);
        while (cursor.moveToNext()) {
            try {
                long contactId = cursor.getLong(0);
                updateContactOptions(db, contactId, values, callerIsSyncAdapter);
                count++;
            } finally {
                cursor.close();
            }
        }
        return count;
    }

    private int updateContactOptions(SQLiteDatabase db, long contactId, ContentValues inputValues, boolean callerIsSyncAdapter) {
        ContentValues values = new ContentValues();
        ContactsDatabaseHelper.copyStringValue(values, "custom_ringtone", inputValues, "custom_ringtone");
        ContactsDatabaseHelper.copyLongValue(values, "send_to_voicemail", inputValues, "send_to_voicemail");
        ContactsDatabaseHelper.copyLongValue(values, "last_time_contacted", inputValues, "last_time_contacted");
        ContactsDatabaseHelper.copyLongValue(values, "times_contacted", inputValues, "times_contacted");
        ContactsDatabaseHelper.copyLongValue(values, "starred", inputValues, "starred");
        ContactsDatabaseHelper.copyLongValue(values, "pinned", inputValues, "pinned");
        if (values.size() == 0) {
            return 0;
        }
        boolean hasStarredValue = flagExists(values, "starred");
        if (hasStarredValue) {
            values.put("dirty", (Integer) 1);
        }
        this.mSelectionArgs1[0] = String.valueOf(contactId);
        db.update("raw_contacts", values, "contact_id=? AND raw_contact_is_read_only=0", this.mSelectionArgs1);
        if (hasStarredValue && !callerIsSyncAdapter) {
            Cursor cursor = db.query("view_raw_contacts", new String[]{"_id"}, "contact_id=?", this.mSelectionArgs1, null, null, null);
            while (cursor.moveToNext()) {
                try {
                    long rawContactId = cursor.getLong(0);
                    updateFavoritesMembership(rawContactId, flagIsSet(values, "starred"));
                } finally {
                    cursor.close();
                }
            }
        }
        values.clear();
        ContactsDatabaseHelper.copyStringValue(values, "custom_ringtone", inputValues, "custom_ringtone");
        ContactsDatabaseHelper.copyLongValue(values, "send_to_voicemail", inputValues, "send_to_voicemail");
        ContactsDatabaseHelper.copyLongValue(values, "last_time_contacted", inputValues, "last_time_contacted");
        ContactsDatabaseHelper.copyLongValue(values, "times_contacted", inputValues, "times_contacted");
        ContactsDatabaseHelper.copyLongValue(values, "starred", inputValues, "starred");
        ContactsDatabaseHelper.copyLongValue(values, "pinned", inputValues, "pinned");
        values.put("contact_last_updated_timestamp", Long.valueOf(Clock.getInstance().currentTimeMillis()));
        int iUpdate = db.update("contacts", values, "_id=?", this.mSelectionArgs1);
        if (inputValues.containsKey("last_time_contacted") && !inputValues.containsKey("times_contacted")) {
            db.execSQL("UPDATE contacts SET times_contacted= ifnull(times_contacted,0)+1 WHERE _id=?", this.mSelectionArgs1);
            db.execSQL("UPDATE raw_contacts SET times_contacted= ifnull(times_contacted,0)+1  WHERE contact_id=?", this.mSelectionArgs1);
            return iUpdate;
        }
        return iUpdate;
    }

    private int updateAggregationException(SQLiteDatabase db, ContentValues values) {
        long rawContactId2;
        long rawContactId1;
        Integer exceptionType = values.getAsInteger("type");
        Long rcId1 = values.getAsLong("raw_contact_id1");
        Long rcId2 = values.getAsLong("raw_contact_id2");
        if (exceptionType == null || rcId1 == null || rcId2 == null) {
            return 0;
        }
        if (rcId1.longValue() < rcId2.longValue()) {
            rawContactId1 = rcId1.longValue();
            rawContactId2 = rcId2.longValue();
        } else {
            rawContactId2 = rcId1.longValue();
            rawContactId1 = rcId2.longValue();
        }
        if (exceptionType.intValue() == 0) {
            this.mSelectionArgs2[0] = String.valueOf(rawContactId1);
            this.mSelectionArgs2[1] = String.valueOf(rawContactId2);
            db.delete("agg_exceptions", "raw_contact_id1=? AND raw_contact_id2=?", this.mSelectionArgs2);
        } else {
            ContentValues exceptionValues = new ContentValues(3);
            exceptionValues.put("type", exceptionType);
            exceptionValues.put("raw_contact_id1", Long.valueOf(rawContactId1));
            exceptionValues.put("raw_contact_id2", Long.valueOf(rawContactId2));
            db.replace("agg_exceptions", "_id", exceptionValues);
        }
        ContactAggregator aggregator = this.mAggregator.get();
        aggregator.invalidateAggregationExceptionCache();
        aggregator.markForAggregation(rawContactId1, 0, true);
        aggregator.markForAggregation(rawContactId2, 0, true);
        aggregator.aggregateContact(this.mTransactionContext.get(), db, rawContactId1);
        aggregator.aggregateContact(this.mTransactionContext.get(), db, rawContactId2);
        return 1;
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        scheduleBackgroundTask(3);
    }

    static String accountsToString(Set<Account> accounts) {
        StringBuilder sb = new StringBuilder();
        for (Account account : accounts) {
            if (sb.length() > 0) {
                sb.append("\u0001");
            }
            sb.append(account.name);
            sb.append("\u0002");
            sb.append(account.type);
        }
        return sb.toString();
    }

    static Set<Account> stringToAccounts(String accountsString) {
        Set<Account> ret = Sets.newHashSet();
        if (accountsString.length() != 0) {
            try {
                String[] arr$ = accountsString.split("\u0001");
                for (String accountString : arr$) {
                    String[] nameAndType = accountString.split("\u0002");
                    ret.add(new Account(nameAndType[0], nameAndType[1]));
                }
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Malformed string", ex);
            }
        }
        return ret;
    }

    boolean haveAccountsChanged(Account[] currentSystemAccounts) {
        ContactsDatabaseHelper dbHelper = this.mDbHelper.get();
        try {
            Set<Account> knownAccountSet = stringToAccounts(dbHelper.getProperty("known_accounts", ""));
            Set<Account> currentAccounts = Sets.newHashSet(currentSystemAccounts);
            return !knownAccountSet.equals(currentAccounts);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    void saveAccounts(Account[] systemAccounts) {
        ContactsDatabaseHelper dbHelper = this.mDbHelper.get();
        dbHelper.setProperty("known_accounts", accountsToString(Sets.newHashSet(systemAccounts)));
    }

    private boolean updateAccountsInBackground(Account[] systemAccounts) {
        if (!haveAccountsChanged(systemAccounts)) {
            return false;
        }
        if ("1".equals(SystemProperties.get("debug.contacts.ksad"))) {
            Log.w("ContactsProvider", "Accounts changed, but not removing stale data for debug.contacts.ksad");
            return true;
        }
        Log.i("ContactsProvider", "Accounts changed");
        invalidateFastScrollingIndexCache();
        ContactsDatabaseHelper dbHelper = this.mDbHelper.get();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Set<AccountWithDataSet> knownAccountsWithDataSets = dbHelper.getAllAccountsWithDataSets();
            List<AccountWithDataSet> accountsWithDataSetsToDelete = Lists.newArrayList();
            for (AccountWithDataSet knownAccountWithDataSet : knownAccountsWithDataSets) {
                if (!knownAccountWithDataSet.isLocalAccount() && !knownAccountWithDataSet.inSystemAccounts(systemAccounts) && !knownAccountWithDataSet.getAccountType().equals("com.android.contact.sim") && !knownAccountWithDataSet.getAccountType().equals("Phone") && !knownAccountWithDataSet.getAccountType().equals("Me")) {
                    accountsWithDataSetsToDelete.add(knownAccountWithDataSet);
                }
            }
            if (!accountsWithDataSetsToDelete.isEmpty()) {
                for (AccountWithDataSet accountWithDataSet : accountsWithDataSetsToDelete) {
                    Log.d("ContactsProvider", "removing data for removed account " + accountWithDataSet);
                    Long accountIdOrNull = dbHelper.getAccountIdOrNull(accountWithDataSet);
                    if (accountIdOrNull != null) {
                        String accountId = Long.toString(accountIdOrNull.longValue());
                        String[] accountIdParams = {accountId};
                        db.execSQL("DELETE FROM groups WHERE account_id = ?", accountIdParams);
                        db.execSQL("DELETE FROM presence WHERE presence_raw_contact_id IN (SELECT _id FROM raw_contacts WHERE account_id = ?)", accountIdParams);
                        db.execSQL("DELETE FROM stream_item_photos WHERE stream_item_id IN (SELECT _id FROM stream_items WHERE raw_contact_id IN (SELECT _id FROM raw_contacts WHERE account_id=?))", accountIdParams);
                        db.execSQL("DELETE FROM stream_items WHERE raw_contact_id IN (SELECT _id FROM raw_contacts WHERE account_id = ?)", accountIdParams);
                        if (!inProfileMode()) {
                            Cursor cursor = db.rawQuery("SELECT raw_contacts.contact_id FROM raw_contacts WHERE account_id = ?1 AND raw_contacts.contact_id IS NOT NULL AND raw_contacts.contact_id NOT IN (    SELECT raw_contacts.contact_id    FROM raw_contacts    WHERE account_id != ?1  AND raw_contacts.contact_id    IS NOT NULL)", accountIdParams);
                            while (cursor.moveToNext()) {
                                try {
                                    long contactId = cursor.getLong(0);
                                    ContactsTableUtil.deleteContact(db, contactId);
                                } finally {
                                }
                            }
                            MoreCloseables.closeQuietly(cursor);
                            cursor = db.rawQuery("SELECT DISTINCT raw_contacts.contact_id FROM raw_contacts WHERE account_id = ?1 AND raw_contacts.contact_id IN (    SELECT raw_contacts.contact_id    FROM raw_contacts    WHERE account_id != ?1)", accountIdParams);
                            while (cursor.moveToNext()) {
                                try {
                                    long contactId2 = cursor.getLong(0);
                                    ContactsTableUtil.updateContactLastUpdateByContactId(db, contactId2);
                                } finally {
                                }
                            }
                        }
                        db.execSQL("DELETE FROM raw_contacts WHERE account_id = ?", accountIdParams);
                        db.execSQL("DELETE FROM accounts WHERE _id=?", accountIdParams);
                    }
                }
                HashSet<Long> orphanContactIds = Sets.newHashSet();
                Cursor cursor2 = db.rawQuery("SELECT _id FROM contacts WHERE (name_raw_contact_id NOT NULL AND name_raw_contact_id NOT IN (SELECT _id FROM raw_contacts)) OR (photo_id NOT NULL AND photo_id NOT IN (SELECT _id FROM data))", null);
                while (cursor2.moveToNext()) {
                    try {
                        orphanContactIds.add(Long.valueOf(cursor2.getLong(0)));
                    } catch (Throwable th) {
                        cursor2.close();
                        throw th;
                    }
                }
                cursor2.close();
                for (Long contactId3 : orphanContactIds) {
                    this.mAggregator.get().updateAggregateData(this.mTransactionContext.get(), contactId3.longValue());
                }
                dbHelper.updateAllVisible();
                if (!inProfileMode()) {
                    updateSearchIndexInTransaction();
                }
            }
            removeStaleAccountRows("settings", "account_name", "account_type", systemAccounts);
            removeStaleAccountRows("directories", "accountName", "accountType", systemAccounts);
            dbHelper.getSyncState().onAccountsChanged(db, systemAccounts);
            saveAccounts(systemAccounts);
            db.setTransactionSuccessful();
            db.endTransaction();
            this.mAccountWritability.clear();
            updateContactsAccountCount(systemAccounts);
            updateProviderStatus();
            return true;
        } catch (Throwable th2) {
            db.endTransaction();
            throw th2;
        }
    }

    private void updateContactsAccountCount(Account[] accounts) {
        int count = 0;
        for (Account account : accounts) {
            if (isContactsAccount(account)) {
                count++;
            }
        }
        this.mContactsAccountCount = count;
    }

    protected boolean isContactsAccount(Account account) {
        IContentService cs = ContentResolver.getContentService();
        try {
            return cs.getIsSyncable(account, "com.android.contacts") > 0;
        } catch (RemoteException e) {
            Log.e("ContactsProvider", "Cannot obtain sync flag for account: " + account, e);
            return false;
        }
    }

    public void onPackageChanged(String packageName) {
        scheduleBackgroundTask(8, packageName);
    }

    private void removeStaleAccountRows(String table, String accountNameColumn, String accountTypeColumn, Account[] systemAccounts) {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        Cursor c = db.rawQuery("SELECT DISTINCT " + accountNameColumn + "," + accountTypeColumn + " FROM " + table, null);
        try {
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                AccountWithDataSet accountWithDataSet = AccountWithDataSet.get(c.getString(0), c.getString(1), null);
                if (!accountWithDataSet.isLocalAccount() && !accountWithDataSet.inSystemAccounts(systemAccounts)) {
                    db.execSQL("DELETE FROM " + table + " WHERE " + accountNameColumn + "=? AND " + accountTypeColumn + "=?", new String[]{accountWithDataSet.getAccountName(), accountWithDataSet.getAccountType()});
                }
            }
        } finally {
            c.close();
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return query(uri, projection, selection, selectionArgs, sortOrder, null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        long directoryId;
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "query: uri=" + uri + "  projection=" + Arrays.toString(projection) + "  selection=[" + selection + "]  args=" + Arrays.toString(selectionArgs) + "  order=[" + sortOrder + "] CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        waitForAccess(this.mReadAccessLatch);
        enforceSocialStreamReadPermission(uri);
        if (mapsToProfileDb(uri)) {
            switchToProfileMode();
            return this.mProfileProvider.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
        }
        switchToContactMode();
        String directory = getQueryParameter(uri, "directory");
        if (directory == null) {
            directoryId = -1;
        } else {
            directoryId = directory.equals("0") ? 0L : directory.equals("1") ? 1L : Long.MIN_VALUE;
        }
        if (directoryId > Long.MIN_VALUE) {
            return addSnippetExtrasToCursor(uri, queryLocal(uri, projection, selection, selectionArgs, sortOrder, directoryId, cancellationSignal));
        }
        DirectoryInfo directoryInfo = getDirectoryAuthority(directory);
        if (directoryInfo == null) {
            Log.e("ContactsProvider", "Invalid directory ID: " + uri);
            return null;
        }
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("content");
        builder.authority(directoryInfo.authority);
        builder.encodedPath(uri.getEncodedPath());
        if (directoryInfo.accountName != null) {
            builder.appendQueryParameter("account_name", directoryInfo.accountName);
        }
        if (directoryInfo.accountType != null) {
            builder.appendQueryParameter("account_type", directoryInfo.accountType);
        }
        String limit = getLimit(uri);
        if (limit != null) {
            builder.appendQueryParameter("limit", limit);
        }
        Uri directoryUri = builder.build();
        if (projection == null) {
            projection = getDefaultProjection(uri);
        }
        Cursor cursor = getContext().getContentResolver().query(directoryUri, projection, selection, selectionArgs, sortOrder);
        if (cursor == null) {
            return null;
        }
        try {
            MemoryCursor memCursor = new MemoryCursor(null, cursor.getColumnNames());
            memCursor.fillFromCursor(cursor);
            return memCursor;
        } finally {
            cursor.close();
        }
    }

    private Cursor addSnippetExtrasToCursor(Uri uri, Cursor cursor) {
        if (cursor.getColumnIndex("snippet") >= 0) {
            String query = uri.getLastPathSegment();
            if ((cursor instanceof AbstractCursor) && deferredSnippetingRequested(uri)) {
                Bundle oldExtras = cursor.getExtras();
                Bundle extras = new Bundle();
                if (oldExtras != null) {
                    extras.putAll(oldExtras);
                }
                extras.putString("deferred_snippeting_query", query);
                ((AbstractCursor) cursor).setExtras(extras);
            }
        }
        return cursor;
    }

    private Cursor addDeferredSnippetingExtra(Cursor cursor) {
        if (cursor instanceof AbstractCursor) {
            Bundle oldExtras = cursor.getExtras();
            Bundle extras = new Bundle();
            if (oldExtras != null) {
                extras.putAll(oldExtras);
            }
            extras.putBoolean("deferred_snippeting", true);
            ((AbstractCursor) cursor).setExtras(extras);
        }
        return cursor;
    }

    private DirectoryInfo getDirectoryAuthority(String directoryId) {
        DirectoryInfo directoryInfo;
        synchronized (this.mDirectoryCache) {
            if (!this.mDirectoryCacheValid) {
                this.mDirectoryCache.clear();
                SQLiteDatabase db = this.mDbHelper.get().getReadableDatabase();
                Cursor cursor = db.query("directories", DirectoryQuery.COLUMNS, null, null, null, null, null);
                while (cursor.moveToNext()) {
                    try {
                        DirectoryInfo info = new DirectoryInfo();
                        String id = cursor.getString(0);
                        info.authority = cursor.getString(1);
                        info.accountName = cursor.getString(2);
                        info.accountType = cursor.getString(3);
                        this.mDirectoryCache.put(id, info);
                    } catch (Throwable th) {
                        cursor.close();
                        throw th;
                    }
                }
                cursor.close();
                this.mDirectoryCacheValid = true;
            }
            directoryInfo = this.mDirectoryCache.get(directoryId);
        }
        return directoryInfo;
    }

    public void resetDirectoryCache() {
        synchronized (this.mDirectoryCache) {
            this.mDirectoryCacheValid = false;
        }
    }

    protected Cursor queryLocal(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, long directoryId, CancellationSignal cancellationSignal) throws Throwable {
        Cursor cursorRemoveNonStarMatchesFromCursor;
        int index;
        String starredInnerQuery;
        SQLiteQueryBuilder qb;
        String frequentInnerQuery;
        SQLiteDatabase db = this.mDbHelper.get().getReadableDatabase();
        SQLiteQueryBuilder qb2 = new SQLiteQueryBuilder();
        String groupBy = null;
        String having = null;
        String limit = getLimit(uri);
        boolean snippetDeferred = false;
        String addressBookIndexerCountExpression = null;
        int match = sUriMatcher.match(uri);
        switch (match) {
            case 1000:
                setTablesAndProjectionMapForContacts(qb2, projection);
                appendLocalDirectoryAndAccountSelectionIfNeeded(qb2, directoryId, uri);
                qb2.setStrict(true);
                String localizedSortOrder = getLocalizedSortOrder(sortOrder);
                Cursor cursor = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                    bundleFastScrollingIndexExtras(cursor, uri, db, qb2, selection, selectionArgs, sortOrder, addressBookIndexerCountExpression, cancellationSignal);
                }
                if (snippetDeferred) {
                    cursor = addDeferredSnippetingExtra(cursor);
                }
                return cursor;
            case 1001:
                long contactId = ContentUris.parseId(uri);
                setTablesAndProjectionMapForContacts(qb2, projection);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(contactId));
                qb2.appendWhere("_id=?");
                qb2.setStrict(true);
                String localizedSortOrder2 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2;
            case 1002:
            case 1003:
                List<String> pathSegments = uri.getPathSegments();
                int segmentCount = pathSegments.size();
                if (segmentCount < 3) {
                    throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Missing a lookup key", uri));
                }
                String lookupKey = pathSegments.get(2);
                if (segmentCount == 4) {
                    long contactId2 = Long.parseLong(pathSegments.get(3));
                    SQLiteQueryBuilder lookupQb = new SQLiteQueryBuilder();
                    setTablesAndProjectionMapForContacts(lookupQb, projection);
                    Cursor c = queryWithContactIdAndLookupKey(lookupQb, db, projection, selection, selectionArgs, sortOrder, null, limit, "_id", contactId2, "lookup", lookupKey, cancellationSignal);
                    if (c != null) {
                        return c;
                    }
                }
                setTablesAndProjectionMapForContacts(qb2, projection);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(lookupContactIdByLookupKey(db, lookupKey)));
                qb2.appendWhere("_id=?");
                qb2.setStrict(true);
                String localizedSortOrder22 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22;
            case 1004:
                long contactId3 = Long.parseLong(uri.getPathSegments().get(1));
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(contactId3));
                qb2.appendWhere(" AND contact_id=?");
                qb2.setStrict(true);
                String localizedSortOrder222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222;
            case 1005:
                boolean deferredSnipRequested = deferredSnippetingRequested(uri);
                String filterParam = uri.getPathSegments().size() > 2 ? uri.getLastPathSegment() : "";
                snippetDeferred = isSingleWordQuery(filterParam) && deferredSnipRequested && snippetNeeded(projection);
                setTablesAndProjectionMapForContactsWithSnippet(qb2, uri, projection, filterParam, directoryId, snippetDeferred);
                qb2.setStrict(true);
                String localizedSortOrder2222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222;
            case 1006:
            case 1007:
                boolean phoneOnly = readBooleanQueryParameter(uri, "strequent_phone_only", false);
                if (match == 1007 && uri.getPathSegments().size() > 3) {
                    String filterParam2 = uri.getLastPathSegment();
                    StringBuilder sb = new StringBuilder();
                    sb.append("_id IN ");
                    appendContactFilterAsNestedQuery(sb, filterParam2);
                    selection = DbQueryUtils.concatenateClauses(selection, sb.toString());
                }
                String[] subProjection = null;
                if (projection != null) {
                    subProjection = new String[projection.length + 2];
                    System.arraycopy(projection, 0, subProjection, 0, projection.length);
                    subProjection[projection.length + 0] = "times_used";
                    subProjection[projection.length + 1] = "last_time_used";
                }
                if (phoneOnly) {
                    StringBuilder tableBuilder = new StringBuilder();
                    tableBuilder.append("(SELECT * FROM view_data WHERE starred=1) AS data LEFT OUTER JOIN data_usage_stat ON (data_usage_stat.data_id=data._id AND data_usage_stat.usage_type=0)");
                    appendContactPresenceJoin(tableBuilder, projection, "contact_id");
                    appendContactStatusUpdateJoin(tableBuilder, projection, "status_update_id");
                    qb2.setTables(tableBuilder.toString());
                    qb2.setProjectionMap(sStrequentPhoneOnlyProjectionMap);
                    long phoneMimeTypeId = this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/phone_v2");
                    long sipMimeTypeId = this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/sip_address");
                    qb2.appendWhere(DbQueryUtils.concatenateClauses(selection, "(starred=1", "mimetype_id IN (" + phoneMimeTypeId + ", " + sipMimeTypeId + ")) AND (contact_id IN default_directory)"));
                    starredInnerQuery = qb2.buildQuery(subProjection, null, null, null, "is_super_primary DESC,(CASE WHEN (strftime('%s', 'now') - last_time_used/1000) < 259200 THEN 0  WHEN (strftime('%s', 'now') - last_time_used/1000) < 604800 THEN 1  WHEN (strftime('%s', 'now') - last_time_used/1000) < 1209600 THEN 2  WHEN (strftime('%s', 'now') - last_time_used/1000) < 2592000 THEN 3  ELSE 4 END), times_used DESC", null);
                    qb = new SQLiteQueryBuilder();
                    qb.setStrict(true);
                    tableBuilder.setLength(0);
                    tableBuilder.append("data_usage_stat INNER JOIN view_data data ON (data_usage_stat.data_id=data._id AND data_usage_stat.usage_type=0)");
                    appendContactPresenceJoin(tableBuilder, projection, "contact_id");
                    appendContactStatusUpdateJoin(tableBuilder, projection, "status_update_id");
                    qb.setTables(tableBuilder.toString());
                    qb.setProjectionMap(sStrequentPhoneOnlyProjectionMap);
                    qb.appendWhere(DbQueryUtils.concatenateClauses(selection, "(starred=0 OR starred IS NULL", "mimetype_id IN (" + phoneMimeTypeId + ", " + sipMimeTypeId + ")) AND (contact_id IN default_directory)"));
                    frequentInnerQuery = qb.buildQuery(subProjection, null, null, null, "(CASE WHEN (strftime('%s', 'now') - last_time_used/1000) < 259200 THEN 0  WHEN (strftime('%s', 'now') - last_time_used/1000) < 604800 THEN 1  WHEN (strftime('%s', 'now') - last_time_used/1000) < 1209600 THEN 2  WHEN (strftime('%s', 'now') - last_time_used/1000) < 2592000 THEN 3  ELSE 4 END), times_used DESC", "25");
                } else {
                    qb2.setStrict(true);
                    setTablesAndProjectionMapForContacts(qb2, projection, false);
                    qb2.setProjectionMap(sStrequentStarredProjectionMap);
                    starredInnerQuery = qb2.buildQuery(subProjection, DbQueryUtils.concatenateClauses(selection, "starred=1"), "_id", null, "display_name COLLATE LOCALIZED ASC", null);
                    qb = new SQLiteQueryBuilder();
                    qb.setStrict(true);
                    setTablesAndProjectionMapForContacts(qb, projection, true);
                    qb.setProjectionMap(sStrequentFrequentProjectionMap);
                    qb.appendWhere(DbQueryUtils.concatenateClauses(selection, "(starred =0 OR starred IS NULL)"));
                    frequentInnerQuery = qb.buildQuery(subProjection, null, "_id", "contact_id IN default_directory", "(CASE WHEN (strftime('%s', 'now') - last_time_used/1000) < 259200 THEN 0  WHEN (strftime('%s', 'now') - last_time_used/1000) < 604800 THEN 1  WHEN (strftime('%s', 'now') - last_time_used/1000) < 1209600 THEN 2  WHEN (strftime('%s', 'now') - last_time_used/1000) < 2592000 THEN 3  ELSE 4 END), times_used DESC", "25");
                }
                String frequentQuery = "SELECT * FROM (" + frequentInnerQuery + ") WHERE (strftime('%s', 'now') - last_time_used/1000)<2592000";
                String starredQuery = "SELECT * FROM (" + starredInnerQuery + ")";
                String unionQuery = qb.buildUnionQuery(new String[]{starredQuery, frequentQuery}, null, null);
                String[] doubledSelectionArgs = null;
                if (selectionArgs != null) {
                    int length = selectionArgs.length;
                    doubledSelectionArgs = new String[length * 2];
                    System.arraycopy(selectionArgs, 0, doubledSelectionArgs, 0, length);
                    System.arraycopy(selectionArgs, 0, doubledSelectionArgs, length, length);
                }
                Cursor cursor3 = db.rawQuery(unionQuery, doubledSelectionArgs);
                if (cursor3 != null) {
                    cursor3.setNotificationUri(getContext().getContentResolver(), ContactsContract.AUTHORITY_URI);
                }
                return cursor3;
            case 1008:
                setTablesAndProjectionMapForContacts(qb2, projection);
                if (uri.getPathSegments().size() > 2) {
                    qb2.appendWhere("_id IN (SELECT contact_id FROM raw_contacts WHERE raw_contacts._id IN (SELECT data.raw_contact_id FROM data JOIN mimetypes ON (data.mimetype_id = mimetypes._id) WHERE mimetype_id=? AND data1=(SELECT groups._id FROM groups WHERE title=?)))");
                    String groupMimeTypeId = String.valueOf(this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/group_membership"));
                    selectionArgs = insertSelectionArg(insertSelectionArg(selectionArgs, uri.getLastPathSegment()), groupMimeTypeId);
                }
                qb2.setStrict(true);
                String localizedSortOrder22222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222;
            case 1009:
                long contactId4 = Long.parseLong(uri.getPathSegments().get(1));
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(contactId4));
                qb2.appendWhere(" AND contact_id=?");
                qb2.appendWhere(" AND _id=photo_id");
                qb2.setStrict(true);
                String localizedSortOrder222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222;
            case 1010:
            case 1011:
            case 1017:
            case 1018:
                List<String> pathSegments2 = uri.getPathSegments();
                int segmentCount2 = pathSegments2.size();
                if (segmentCount2 < 4) {
                    throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Missing a lookup key", uri));
                }
                String lookupKey2 = pathSegments2.get(2);
                if (segmentCount2 == 5) {
                    long contactId5 = Long.parseLong(pathSegments2.get(3));
                    SQLiteQueryBuilder lookupQb2 = new SQLiteQueryBuilder();
                    setTablesAndProjectionMapForData(lookupQb2, uri, projection, false);
                    if (match == 1010 || match == 1011) {
                        lookupQb2.appendWhere(" AND _id=photo_id");
                    }
                    lookupQb2.appendWhere(" AND ");
                    Cursor c2 = queryWithContactIdAndLookupKey(lookupQb2, db, projection, selection, selectionArgs, sortOrder, null, limit, "contact_id", contactId5, "lookup", lookupKey2, cancellationSignal);
                    if (c2 != null) {
                        return c2;
                    }
                }
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                long contactId6 = lookupContactIdByLookupKey(db, lookupKey2);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(contactId6));
                if (match == 1010 || match == 1011) {
                    qb2.appendWhere(" AND _id=photo_id");
                }
                qb2.appendWhere(" AND contact_id=?");
                qb2.setStrict(true);
                String localizedSortOrder2222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222;
            case 1015:
                String lookupKey3 = Uri.encode(uri.getPathSegments().get(2));
                long contactId7 = lookupContactIdByLookupKey(db, lookupKey3);
                qb2.setTables("view_contacts");
                qb2.setProjectionMap(sContactsVCardProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(contactId7));
                qb2.appendWhere("_id=?");
                qb2.setStrict(true);
                String localizedSortOrder22222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222;
            case 1016:
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                String currentDateString = dateFormat.format(new Date()).toString();
                return db.rawQuery("SELECT 'vcards_' || ? || '.vcf' AS _display_name, NULL AS _size", new String[]{currentDateString});
            case 1019:
                long contactId8 = Long.parseLong(uri.getPathSegments().get(1));
                setTablesAndProjectionMapForEntities(qb2, uri, projection);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(contactId8));
                qb2.appendWhere(" AND contact_id=?");
                qb2.setStrict(true);
                String localizedSortOrder222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222;
            case 1020:
            case 1021:
                List<String> pathSegments3 = uri.getPathSegments();
                int segmentCount3 = pathSegments3.size();
                if (segmentCount3 < 4) {
                    throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Missing a lookup key", uri));
                }
                String lookupKey4 = pathSegments3.get(2);
                if (segmentCount3 == 5) {
                    long contactId9 = Long.parseLong(pathSegments3.get(3));
                    SQLiteQueryBuilder lookupQb3 = new SQLiteQueryBuilder();
                    setTablesAndProjectionMapForEntities(lookupQb3, uri, projection);
                    lookupQb3.appendWhere(" AND ");
                    Cursor c3 = queryWithContactIdAndLookupKey(lookupQb3, db, projection, selection, selectionArgs, sortOrder, null, limit, "contact_id", contactId9, "lookup", lookupKey4, cancellationSignal);
                    if (c3 != null) {
                        return c3;
                    }
                }
                setTablesAndProjectionMapForEntities(qb2, uri, projection);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(lookupContactIdByLookupKey(db, lookupKey4)));
                qb2.appendWhere(" AND contact_id=?");
                qb2.setStrict(true);
                String localizedSortOrder2222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222;
            case 1022:
                long contactId10 = Long.parseLong(uri.getPathSegments().get(1));
                setTablesAndProjectionMapForStreamItems(qb2);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(contactId10));
                qb2.appendWhere("contact_id=?");
                qb2.setStrict(true);
                String localizedSortOrder22222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222;
            case 1023:
            case 1024:
                List<String> pathSegments4 = uri.getPathSegments();
                int segmentCount4 = pathSegments4.size();
                if (segmentCount4 < 4) {
                    throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Missing a lookup key", uri));
                }
                String lookupKey5 = pathSegments4.get(2);
                if (segmentCount4 == 5) {
                    long contactId11 = Long.parseLong(pathSegments4.get(3));
                    SQLiteQueryBuilder lookupQb4 = new SQLiteQueryBuilder();
                    setTablesAndProjectionMapForStreamItems(lookupQb4);
                    Cursor c4 = queryWithContactIdAndLookupKey(lookupQb4, db, projection, selection, selectionArgs, sortOrder, null, limit, "contact_id", contactId11, "contact_lookup", lookupKey5, cancellationSignal);
                    if (c4 != null) {
                        return c4;
                    }
                }
                setTablesAndProjectionMapForStreamItems(qb2);
                long contactId12 = lookupContactIdByLookupKey(db, lookupKey5);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(contactId12));
                qb2.appendWhere("contact_id=?");
                qb2.setStrict(true);
                String localizedSortOrder222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222;
            case 1025:
                setTablesAndProjectionMapForContacts(qb2, projection, true);
                qb2.setProjectionMap(sStrequentFrequentProjectionMap);
                groupBy = "_id";
                having = "_id IN default_directory";
                sortOrder = !TextUtils.isEmpty(sortOrder) ? "times_used DESC,display_name COLLATE LOCALIZED ASC, " + sortOrder : "times_used DESC,display_name COLLATE LOCALIZED ASC";
                qb2.setStrict(true);
                String localizedSortOrder2222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222;
            case 2002:
            case 19005:
                setTablesAndProjectionMapForRawContacts(qb2, uri);
                qb2.setStrict(true);
                String localizedSortOrder22222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222;
            case 2003:
            case 19006:
                long rawContactId = ContentUris.parseId(uri);
                setTablesAndProjectionMapForRawContacts(qb2, uri);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(rawContactId));
                qb2.appendWhere(" AND _id=?");
                qb2.setStrict(true);
                String localizedSortOrder222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222;
            case 2004:
            case 19007:
                int segment = match == 2004 ? 1 : 2;
                long rawContactId2 = Long.parseLong(uri.getPathSegments().get(segment));
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(rawContactId2));
                qb2.appendWhere(" AND raw_contact_id=?");
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222;
            case 2005:
                long rawContactId3 = Long.parseLong(uri.getPathSegments().get(1));
                setTablesAndProjectionMapForRawEntities(qb2, uri);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(rawContactId3));
                qb2.appendWhere(" AND _id=?");
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222;
            case 2007:
                long rawContactId4 = Long.parseLong(uri.getPathSegments().get(1));
                setTablesAndProjectionMapForStreamItems(qb2);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(rawContactId4));
                qb2.appendWhere("raw_contact_id=?");
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222;
            case 2008:
                long rawContactId5 = Long.parseLong(uri.getPathSegments().get(1));
                long streamItemId = Long.parseLong(uri.getPathSegments().get(3));
                setTablesAndProjectionMapForStreamItems(qb2);
                selectionArgs = insertSelectionArg(insertSelectionArg(selectionArgs, String.valueOf(streamItemId)), String.valueOf(rawContactId5));
                qb2.appendWhere("raw_contact_id=? AND _id=?");
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222;
            case 3000:
            case 19002:
                String usageType = uri.getQueryParameter("type");
                int typeInt = getDataUsageFeedbackType(usageType, -1);
                setTablesAndProjectionMapForData(qb2, uri, projection, false, Integer.valueOf(typeInt));
                if (uri.getBooleanQueryParameter("visible_contacts_only", false)) {
                    qb2.appendWhere(" AND contact_id in default_directory");
                }
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222;
            case 3001:
            case 19003:
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                qb2.appendWhere(" AND _id=?");
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222;
            case 3002:
            case 3011:
                String mimeTypeIsPhoneExpression = "mimetype_id=" + this.mDbHelper.get().getMimeTypeIdForPhone();
                String mimeTypeIsSipExpression = "mimetype_id=" + this.mDbHelper.get().getMimeTypeIdForSip();
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                if (match == 3011) {
                    qb2.appendWhere(" AND ((" + mimeTypeIsPhoneExpression + ") OR (" + mimeTypeIsSipExpression + "))");
                } else {
                    qb2.appendWhere(" AND " + mimeTypeIsPhoneExpression);
                }
                boolean removeDuplicates = readBooleanQueryParameter(uri, "remove_duplicate_entries", false);
                if (removeDuplicates) {
                    groupBy = "contact_id, data1";
                    addressBookIndexerCountExpression = "DISTINCT contact_id||','||data1";
                }
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222;
            case 3003:
            case 3012:
                String mimeTypeIsPhoneExpression2 = "mimetype_id=" + this.mDbHelper.get().getMimeTypeIdForPhone();
                String mimeTypeIsSipExpression2 = "mimetype_id=" + this.mDbHelper.get().getMimeTypeIdForSip();
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                if (match == 3012) {
                    qb2.appendWhere(" AND ((" + mimeTypeIsPhoneExpression2 + ") OR (" + mimeTypeIsSipExpression2 + "))");
                } else {
                    qb2.appendWhere(" AND " + mimeTypeIsPhoneExpression2);
                }
                qb2.appendWhere(" AND _id=?");
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222222;
            case 3004:
            case 3013:
                String mimeTypeIsPhoneExpression3 = "mimetype_id=" + this.mDbHelper.get().getMimeTypeIdForPhone();
                String mimeTypeIsSipExpression3 = "mimetype_id=" + this.mDbHelper.get().getMimeTypeIdForSip();
                String typeParam = uri.getQueryParameter("type");
                int typeInt2 = getDataUsageFeedbackType(typeParam, 0);
                setTablesAndProjectionMapForData(qb2, uri, projection, true, Integer.valueOf(typeInt2));
                if (match == 3013) {
                    qb2.appendWhere(" AND ((" + mimeTypeIsPhoneExpression3 + ") OR (" + mimeTypeIsSipExpression3 + "))");
                } else {
                    qb2.appendWhere(" AND " + mimeTypeIsPhoneExpression3);
                }
                if (uri.getPathSegments().size() > 2) {
                    String filterParam3 = uri.getLastPathSegment();
                    boolean searchDisplayName = uri.getBooleanQueryParameter("search_display_name", true);
                    boolean searchPhoneNumber = uri.getBooleanQueryParameter("search_phone_number", true);
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(" AND (");
                    boolean hasCondition = false;
                    String ftsMatchQuery = searchDisplayName ? SearchIndexManager.getFtsMatchQuery(filterParam3, SearchIndexManager.FtsQueryBuilder.UNSCOPED_NORMALIZING) : null;
                    if (!TextUtils.isEmpty(ftsMatchQuery)) {
                        sb2.append("raw_contact_id IN (SELECT raw_contacts._id FROM search_index JOIN raw_contacts ON (search_index.contact_id=raw_contacts.contact_id) WHERE name MATCH '");
                        sb2.append(ftsMatchQuery);
                        sb2.append("')");
                        hasCondition = true;
                    }
                    if (searchPhoneNumber) {
                        String number = PhoneNumberUtils.normalizeNumber(filterParam3);
                        if (!TextUtils.isEmpty(number)) {
                            if (hasCondition) {
                                sb2.append(" OR ");
                            }
                            sb2.append("_id IN (SELECT DISTINCT data_id FROM phone_lookup WHERE normalized_number LIKE '");
                            sb2.append(number);
                            sb2.append("%')");
                            hasCondition = true;
                        }
                        if (!TextUtils.isEmpty(filterParam3) && match == 3013) {
                            if (hasCondition) {
                                sb2.append(" OR ");
                            }
                            sb2.append("(");
                            sb2.append(mimeTypeIsSipExpression3);
                            sb2.append(" AND ((data1 LIKE ");
                            DatabaseUtils.appendEscapedSQLString(sb2, filterParam3 + '%');
                            sb2.append(") OR (data1 LIKE ");
                            DatabaseUtils.appendEscapedSQLString(sb2, "sip:" + filterParam3 + '%');
                            sb2.append(")))");
                            hasCondition = true;
                        }
                    }
                    if (!hasCondition) {
                        sb2.append("0");
                    }
                    sb2.append(")");
                    qb2.appendWhere(sb2);
                }
                if (match == 3013) {
                    String isPhoneAndHasNormalized = "(" + mimeTypeIsPhoneExpression3 + " AND data4 IS NOT NULL)";
                    groupBy = "(CASE WHEN " + isPhoneAndHasNormalized + " THEN data4 ELSE data1 END), contact_id";
                } else {
                    groupBy = "(CASE WHEN data4 IS NOT NULL THEN data4 ELSE data1 END), contact_id";
                }
                if (sortOrder == null) {
                    String accountPromotionSortOrder = getAccountPromotionSortOrder(uri);
                    sortOrder = !TextUtils.isEmpty(accountPromotionSortOrder) ? accountPromotionSortOrder + ", starred DESC, is_super_primary DESC, (CASE WHEN (strftime('%s', 'now') - last_time_used/1000) < 259200 THEN 0  WHEN (strftime('%s', 'now') - last_time_used/1000) < 604800 THEN 1  WHEN (strftime('%s', 'now') - last_time_used/1000) < 1209600 THEN 2  WHEN (strftime('%s', 'now') - last_time_used/1000) < 2592000 THEN 3  ELSE 4 END), times_used DESC, in_visible_group DESC, display_name COLLATE LOCALIZED ASC, contact_id, is_primary DESC" : "starred DESC, is_super_primary DESC, (CASE WHEN (strftime('%s', 'now') - last_time_used/1000) < 259200 THEN 0  WHEN (strftime('%s', 'now') - last_time_used/1000) < 604800 THEN 1  WHEN (strftime('%s', 'now') - last_time_used/1000) < 1209600 THEN 2  WHEN (strftime('%s', 'now') - last_time_used/1000) < 2592000 THEN 3  ELSE 4 END), times_used DESC, in_visible_group DESC, display_name COLLATE LOCALIZED ASC, contact_id, is_primary DESC";
                }
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222;
            case 3005:
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                qb2.appendWhere(" AND mimetype_id = " + this.mDbHelper.get().getMimeTypeIdForEmail());
                boolean removeDuplicates2 = readBooleanQueryParameter(uri, "remove_duplicate_entries", false);
                if (removeDuplicates2) {
                    groupBy = "contact_id, data1";
                    addressBookIndexerCountExpression = "DISTINCT contact_id||','||data1";
                }
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222;
            case 3006:
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                qb2.appendWhere(" AND mimetype_id = " + this.mDbHelper.get().getMimeTypeIdForEmail() + " AND _id=?");
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222222222;
            case 3007:
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                qb2.appendWhere(" AND mimetype_id = " + this.mDbHelper.get().getMimeTypeIdForEmail());
                if (uri.getPathSegments().size() > 2) {
                    String email = uri.getLastPathSegment();
                    String address = this.mDbHelper.get().extractAddressFromEmailAddress(email);
                    selectionArgs = insertSelectionArg(selectionArgs, address);
                    qb2.appendWhere(" AND UPPER(data1)=UPPER(?)");
                }
                if (sortOrder == null) {
                    sortOrder = "(contact_id IN default_directory) DESC";
                }
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222222;
            case 3008:
                String typeParam2 = uri.getQueryParameter("type");
                int typeInt3 = getDataUsageFeedbackType(typeParam2, 1);
                setTablesAndProjectionMapForData(qb2, uri, projection, true, Integer.valueOf(typeInt3));
                String filterParam4 = null;
                if (uri.getPathSegments().size() > 3) {
                    filterParam4 = uri.getLastPathSegment();
                    if (TextUtils.isEmpty(filterParam4)) {
                        filterParam4 = null;
                    }
                }
                if (filterParam4 == null) {
                    qb2.appendWhere(" AND 0");
                } else {
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append(" AND _id IN (");
                    sb3.append("SELECT _id FROM data WHERE mimetype_id=");
                    sb3.append(this.mDbHelper.get().getMimeTypeIdForEmail());
                    sb3.append(" AND data1 LIKE ");
                    DatabaseUtils.appendEscapedSQLString(sb3, filterParam4 + '%');
                    if (!filterParam4.contains("@")) {
                        sb3.append(" UNION SELECT _id FROM data WHERE +mimetype_id=");
                        sb3.append(this.mDbHelper.get().getMimeTypeIdForEmail());
                        sb3.append(" AND raw_contact_id IN (SELECT raw_contacts._id FROM search_index JOIN raw_contacts ON (search_index.contact_id=raw_contacts.contact_id) WHERE name MATCH '");
                        String ftsMatchQuery2 = SearchIndexManager.getFtsMatchQuery(filterParam4, SearchIndexManager.FtsQueryBuilder.UNSCOPED_NORMALIZING);
                        sb3.append(ftsMatchQuery2);
                        sb3.append("')");
                    }
                    sb3.append(")");
                    qb2.appendWhere(sb3);
                }
                groupBy = "data1,contact_id,account_name,account_type";
                if (sortOrder == null) {
                    String accountPromotionSortOrder2 = getAccountPromotionSortOrder(uri);
                    sortOrder = !TextUtils.isEmpty(accountPromotionSortOrder2) ? accountPromotionSortOrder2 + ", starred DESC, is_super_primary DESC, (CASE WHEN (strftime('%s', 'now') - last_time_used/1000) < 259200 THEN 0  WHEN (strftime('%s', 'now') - last_time_used/1000) < 604800 THEN 1  WHEN (strftime('%s', 'now') - last_time_used/1000) < 1209600 THEN 2  WHEN (strftime('%s', 'now') - last_time_used/1000) < 2592000 THEN 3  ELSE 4 END), times_used DESC, in_visible_group DESC, display_name COLLATE LOCALIZED ASC, contact_id, is_primary DESC" : "starred DESC, is_super_primary DESC, (CASE WHEN (strftime('%s', 'now') - last_time_used/1000) < 259200 THEN 0  WHEN (strftime('%s', 'now') - last_time_used/1000) < 604800 THEN 1  WHEN (strftime('%s', 'now') - last_time_used/1000) < 1209600 THEN 2  WHEN (strftime('%s', 'now') - last_time_used/1000) < 2592000 THEN 3  ELSE 4 END), times_used DESC, in_visible_group DESC, display_name COLLATE LOCALIZED ASC, contact_id, is_primary DESC";
                    String primaryAccountName = uri.getQueryParameter("name_for_primary_account");
                    if (!TextUtils.isEmpty(primaryAccountName) && (index = primaryAccountName.indexOf(64)) != -1) {
                        String domain = primaryAccountName.substring(index);
                        StringBuilder likeValue = new StringBuilder();
                        likeValue.append('%');
                        DbQueryUtils.escapeLikeValue(likeValue, domain, '\\');
                        selectionArgs = appendSelectionArg(selectionArgs, likeValue.toString());
                        sortOrder = sortOrder + ", (CASE WHEN data1 like ? ESCAPE '\\' THEN 0 ELSE 1 END)";
                    }
                }
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222222;
            case 3009:
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                qb2.appendWhere(" AND mimetype_id = " + this.mDbHelper.get().getMimeTypeIdForStructuredPostal());
                boolean removeDuplicates3 = readBooleanQueryParameter(uri, "remove_duplicate_entries", false);
                if (removeDuplicates3) {
                    groupBy = "contact_id, data1";
                    addressBookIndexerCountExpression = "DISTINCT contact_id||','||data1";
                }
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222222222222;
            case 3010:
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                qb2.appendWhere(" AND mimetype_id = " + this.mDbHelper.get().getMimeTypeIdForStructuredPostal());
                qb2.appendWhere(" AND _id=?");
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222222222;
            case 3014:
            case 3015:
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                String filterParam5 = null;
                int uriPathSize = uri.getPathSegments().size();
                if (uriPathSize > 3) {
                    filterParam5 = uri.getLastPathSegment();
                    if (TextUtils.isEmpty(filterParam5)) {
                        filterParam5 = null;
                    }
                }
                if (uriPathSize <= 2 || filterParam5 != null) {
                    if (uri.getBooleanQueryParameter("visible_contacts_only", false)) {
                        qb2.appendWhere(" AND contact_id in default_directory");
                    }
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append(" AND (");
                    sb4.append("mimetype_id IN (");
                    sb4.append(this.mDbHelper.get().getMimeTypeIdForEmail());
                    sb4.append(",");
                    sb4.append(this.mDbHelper.get().getMimeTypeIdForPhone());
                    sb4.append("))");
                    if (uriPathSize < 3) {
                        qb2.appendWhere(sb4);
                    } else {
                        sb4.append(" AND ");
                        sb4.append("(contact_id IN (");
                        sb4.append("SELECT contact_id FROM data JOIN raw_contacts ON data.raw_contact_id=raw_contacts._id WHERE (mimetype_id=");
                        sb4.append(this.mDbHelper.get().getMimeTypeIdForEmail());
                        sb4.append(" AND data1 LIKE ");
                        DatabaseUtils.appendEscapedSQLString(sb4, filterParam5 + '%');
                        sb4.append(")");
                        String number2 = PhoneNumberUtils.normalizeNumber(filterParam5);
                        if (!TextUtils.isEmpty(number2)) {
                            sb4.append("UNION SELECT DISTINCT contact_id FROM phone_lookup JOIN raw_contacts ON (phone_lookup.raw_contact_id=raw_contacts._id) WHERE normalized_number LIKE '");
                            sb4.append(number2);
                            sb4.append("%'");
                        }
                        sb4.append(" UNION SELECT contact_id FROM data JOIN raw_contacts ON data.raw_contact_id=raw_contacts._id WHERE raw_contact_id IN (SELECT raw_contacts._id FROM search_index JOIN raw_contacts ON (search_index.contact_id=raw_contacts.contact_id) WHERE name MATCH '");
                        String ftsMatchQuery3 = SearchIndexManager.getFtsMatchQuery(filterParam5, SearchIndexManager.FtsQueryBuilder.UNSCOPED_NORMALIZING);
                        sb4.append(ftsMatchQuery3);
                        sb4.append("')");
                        sb4.append("))");
                        qb2.appendWhere(sb4);
                    }
                } else {
                    qb2.appendWhere(" AND 0");
                }
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222222222;
            case 4000:
                if (uri.getBooleanQueryParameter("sip", false)) {
                    if (TextUtils.isEmpty(sortOrder)) {
                        sortOrder = "display_name COLLATE LOCALIZED ASC";
                    }
                    String sipAddress = uri.getPathSegments().size() > 1 ? Uri.decode(uri.getLastPathSegment()) : "";
                    setTablesAndProjectionMapForData(qb2, uri, (String[]) null, false, true);
                    StringBuilder sb5 = new StringBuilder();
                    selectionArgs = this.mDbHelper.get().buildSipContactQuery(sb5, sipAddress);
                    selection = sb5.toString();
                    qb2.setStrict(true);
                    String localizedSortOrder22222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                    Cursor cursor22222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                    if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                    }
                    if (snippetDeferred) {
                    }
                    return cursor22222222222222222222222222222222;
                }
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = " length(lookup.normalized_number) DESC";
                }
                String number3 = uri.getPathSegments().size() > 1 ? uri.getLastPathSegment() : "";
                String numberE164 = PhoneNumberUtils.formatNumberToE164(number3, this.mDbHelper.get().getCurrentCountryIso());
                String normalizedNumber = PhoneNumberUtils.normalizeNumber(number3);
                this.mDbHelper.get().buildPhoneLookupAndContactQuery(qb2, normalizedNumber, numberE164);
                qb2.setProjectionMap(sPhoneLookupProjectionMap);
                String[] projectionWithNumber = projection;
                if (projection != null && !ArrayUtils.contains(projection, "number")) {
                    projectionWithNumber = (String[]) ArrayUtils.appendElement(String.class, projection, "number");
                }
                qb2.setStrict(true);
                boolean foundResult = false;
                Cursor cursor4 = query(db, qb2, projectionWithNumber, null, null, sortOrder, null, null, limit, cancellationSignal);
                try {
                    if (cursor4.getCount() > 0) {
                        foundResult = true;
                        cursorRemoveNonStarMatchesFromCursor = PhoneLookupWithStarPrefix.removeNonStarMatchesFromCursor(number3, cursor4);
                        if (1 == 0) {
                            cursor4.close();
                        }
                    } else {
                        SQLiteQueryBuilder qb3 = new SQLiteQueryBuilder();
                        try {
                            qb3.setProjectionMap(sPhoneLookupProjectionMap);
                            qb3.setStrict(true);
                            this.mDbHelper.get().buildFallbackPhoneLookupAndContactQuery(qb3, number3);
                            Cursor fallbackCursor = query(db, qb3, projectionWithNumber, null, null, sortOrder, null, null, limit, cancellationSignal);
                            cursorRemoveNonStarMatchesFromCursor = PhoneLookupWithStarPrefix.removeNonStarMatchesFromCursor(number3, fallbackCursor);
                            if (0 == 0) {
                                cursor4.close();
                            }
                        } catch (Throwable th) {
                            th = th;
                            if (!foundResult) {
                                cursor4.close();
                            }
                            throw th;
                        }
                    }
                    return cursorRemoveNonStarMatchesFromCursor;
                } catch (Throwable th2) {
                    th = th2;
                }
                break;
            case 4001:
                if (uri.getPathSegments().size() != 2) {
                    throw new IllegalArgumentException("Phone number missing in URI: " + uri);
                }
                String phoneNumber = Uri.decode(uri.getLastPathSegment());
                boolean isSipAddress = uri.getBooleanQueryParameter("sip", false);
                return queryPhoneLookupEnterprise(phoneNumber, projection, isSipAddress);
            case 6000:
                qb2.setTables("agg_exceptions");
                qb2.setProjectionMap(sAggregationExceptionsProjectionMap);
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222222222222;
            case 7000:
            case 19009:
                setTableAndProjectionMapForStatusUpdates(qb2, projection);
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222222222222;
            case 7001:
                setTableAndProjectionMapForStatusUpdates(qb2, projection);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                qb2.appendWhere("data._id=?");
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222222222222222222;
            case 8000:
                long contactId13 = Long.parseLong(uri.getPathSegments().get(1));
                String filter = null;
                if (uri.getPathSegments().size() > 3) {
                    String filter2 = uri.getPathSegments().get(3);
                    filter = filter2;
                }
                int maxSuggestions = limit != null ? Integer.parseInt(limit) : 5;
                ArrayList<ContactAggregator.AggregationSuggestionParameter> parameters = null;
                List<String> query = uri.getQueryParameters("query");
                if (query != null && !query.isEmpty()) {
                    parameters = new ArrayList<>(query.size());
                    for (String parameter : query) {
                        int offset = parameter.indexOf(58);
                        parameters.add(offset == -1 ? new ContactAggregator.AggregationSuggestionParameter("name", parameter) : new ContactAggregator.AggregationSuggestionParameter(parameter.substring(0, offset), parameter.substring(offset + 1)));
                    }
                }
                setTablesAndProjectionMapForContacts(qb2, projection);
                return this.mAggregator.get().queryAggregationSuggestions(qb2, projection, contactId13, maxSuggestions, filter, parameters);
            case 9000:
                qb2.setTables("settings");
                qb2.setProjectionMap(sSettingsProjectionMap);
                appendAccountFromParameter(qb2, uri);
                String groupMembershipMimetypeId = Long.toString(this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/group_membership"));
                if (projection != null && projection.length != 0 && ContactsDatabaseHelper.isInProjection(projection, "summ_count")) {
                    selectionArgs = insertSelectionArg(selectionArgs, groupMembershipMimetypeId);
                }
                if (projection != null && projection.length != 0 && ContactsDatabaseHelper.isInProjection(projection, "summ_phones")) {
                    selectionArgs = insertSelectionArg(selectionArgs, groupMembershipMimetypeId);
                }
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222222222222222;
            case 10000:
                qb2.setTables("view_groups");
                qb2.setProjectionMap(sGroupsProjectionMap);
                appendAccountIdFromParameter(qb2, uri);
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222222222222222;
            case 10001:
                qb2.setTables("view_groups");
                qb2.setProjectionMap(sGroupsProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                qb2.appendWhere("_id=?");
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222222222222222222222;
            case 10003:
                String tables = ContactsDatabaseHelper.isInProjection(projection, "summ_count") ? "view_groups AS groups LEFT OUTER JOIN (SELECT data.data1 AS member_count_group_id, COUNT(data.raw_contact_id) AS group_member_count FROM data WHERE data.mimetype_id = (SELECT _id FROM mimetypes WHERE mimetypes.mimetype = 'vnd.android.cursor.item/group_membership')GROUP BY member_count_group_id) AS member_count_table ON (groups._id = member_count_table.member_count_group_id)" : "view_groups AS groups";
                if (ContactsDatabaseHelper.isInProjection(projection, "group_count_per_account")) {
                    Log.w("ContactsProvider", "group_count_per_account is not supported yet");
                }
                qb2.setTables(tables);
                qb2.setProjectionMap(sGroupsSummaryProjectionMap);
                appendAccountIdFromParameter(qb2, uri);
                groupBy = "groups._id";
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222222222222222222;
            case 11000:
            case 11002:
                return this.mDbHelper.get().getSyncState().query(db, projection, selection, selectionArgs, sortOrder);
            case 12001:
                return this.mGlobalSearchSupport.handleSearchSuggestionsQuery(db, uri, projection, limit, cancellationSignal);
            case 12002:
                String lookupKey6 = uri.getLastPathSegment();
                String filter3 = getQueryParameter(uri, "suggest_intent_extra_data");
                return this.mGlobalSearchSupport.handleSearchShortcutRefresh(db, projection, lookupKey6, filter3, cancellationSignal);
            case 15001:
            case 19010:
                setTablesAndProjectionMapForRawEntities(qb2, uri);
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222222222222222222;
            case 15002:
                groupBy = "contact_id";
                setTablesAndProjectionMapForRawContacts(qb2, uri);
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222222222222222222222222;
            case 16001:
                return buildSingleRowResult(projection, new String[]{"status", "data1"}, new Object[]{Integer.valueOf(this.mProviderStatus), Long.valueOf(this.mEstimatedStorageRequirement)});
            case 17001:
                qb2.setTables("directories");
                qb2.setProjectionMap(sDirectoryProjectionMap);
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222222222222222222222;
            case 17002:
                long id = ContentUris.parseId(uri);
                qb2.setTables("directories");
                qb2.setProjectionMap(sDirectoryProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(id));
                qb2.appendWhere("_id=?");
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222222222222222222222;
            case 18000:
                return completeName(uri, projection);
            case 19000:
                setTablesAndProjectionMapForContacts(qb2, projection);
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222222222222222222222222222;
            case 19001:
                setTablesAndProjectionMapForEntities(qb2, uri, projection);
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222222222222222222222222;
            case 19004:
                qb2.setTables("view_contacts");
                qb2.setProjectionMap(sContactsVCardProjectionMap);
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222222222222222222222222;
            case 19008:
                long rawContactId6 = Long.parseLong(uri.getPathSegments().get(2));
                selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(rawContactId6));
                setTablesAndProjectionMapForRawEntities(qb2, uri);
                qb2.appendWhere(" AND _id=?");
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222222222222222222222222222222;
            case 19011:
                setTablesAndProjectionMapForData(qb2, uri, projection, false);
                qb2.appendWhere(" AND _id=photo_id");
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222222222222222222222222222;
            case 21000:
                setTablesAndProjectionMapForStreamItems(qb2);
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222222222222222222222222222;
            case 21001:
                setTablesAndProjectionMapForStreamItemPhotos(qb2);
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222222222222222222222222222222222;
            case 21002:
                setTablesAndProjectionMapForStreamItems(qb2);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                qb2.appendWhere("_id=?");
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222222222222222222222222222222;
            case 21003:
                setTablesAndProjectionMapForStreamItemPhotos(qb2);
                String streamItemId2 = uri.getPathSegments().get(1);
                selectionArgs = insertSelectionArg(selectionArgs, streamItemId2);
                qb2.appendWhere("stream_item_photos.stream_item_id=?");
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222222222222222222222222222222;
            case 21004:
                setTablesAndProjectionMapForStreamItemPhotos(qb2);
                String streamItemId3 = uri.getPathSegments().get(1);
                String streamItemPhotoId = uri.getPathSegments().get(3);
                selectionArgs = insertSelectionArg(insertSelectionArg(selectionArgs, streamItemPhotoId), streamItemId3);
                qb2.appendWhere("stream_item_photos.stream_item_id=? AND stream_item_photos._id=?");
                qb2.setStrict(true);
                String localizedSortOrder22222222222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor22222222222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder22222222222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor22222222222222222222222222222222222222222222222222222;
            case 21005:
                return buildSingleRowResult(projection, new String[]{"max_items"}, new Object[]{5});
            case 22001:
                return buildSingleRowResult(projection, new String[]{"display_max_dim", "thumbnail_max_dim"}, new Object[]{Integer.valueOf(getMaxDisplayPhotoDim()), Integer.valueOf(getMaxThumbnailDim())});
            case 23000:
                qb2.setTables("deleted_contacts");
                qb2.setProjectionMap(sDeletedContactsProjectionMap);
                qb2.setStrict(true);
                String localizedSortOrder222222222222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor222222222222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder222222222222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor222222222222222222222222222222222222222222222222222222;
            case 23001:
                String id2 = uri.getLastPathSegment();
                qb2.setTables("deleted_contacts");
                qb2.setProjectionMap(sDeletedContactsProjectionMap);
                qb2.appendWhere("contact_id=?");
                selectionArgs = insertSelectionArg(selectionArgs, id2);
                qb2.setStrict(true);
                String localizedSortOrder2222222222222222222222222222222222222222222222222222222 = getLocalizedSortOrder(sortOrder);
                Cursor cursor2222222222222222222222222222222222222222222222222222222 = query(db, qb2, projection, selection, selectionArgs, localizedSortOrder2222222222222222222222222222222222222222222222222222222, groupBy, having, limit, cancellationSignal);
                if (readBooleanQueryParameter(uri, "android.provider.extra.ADDRESS_BOOK_INDEX", false)) {
                }
                if (snippetDeferred) {
                }
                return cursor2222222222222222222222222222222222222222222222222222222;
            default:
                return this.mLegacyApiSupport.query(uri, projection, selection, selectionArgs, sortOrder, limit);
        }
    }

    protected static String getLocalizedSortOrder(String sortOrder) {
        String sortKey;
        if (sortOrder == null) {
            return sortOrder;
        }
        String sortOrderSuffix = "";
        int spaceIndex = sortOrder.indexOf(32);
        if (spaceIndex != -1) {
            sortKey = sortOrder.substring(0, spaceIndex);
            sortOrderSuffix = sortOrder.substring(spaceIndex);
        } else {
            sortKey = sortOrder;
        }
        if (TextUtils.equals(sortKey, "sort_key")) {
            String localizedSortOrder = "phonebook_bucket" + sortOrderSuffix + ", " + sortOrder;
            return localizedSortOrder;
        }
        if (!TextUtils.equals(sortKey, "sort_key_alt")) {
            return sortOrder;
        }
        String localizedSortOrder2 = "phonebook_bucket_alt" + sortOrderSuffix + ", " + sortOrder;
        return localizedSortOrder2;
    }

    private Cursor query(SQLiteDatabase db, SQLiteQueryBuilder qb, String[] projection, String selection, String[] selectionArgs, String sortOrder, String groupBy, String having, String limit, CancellationSignal cancellationSignal) {
        if (projection != null && projection.length == 1 && "_count".equals(projection[0])) {
            qb.setProjectionMap(sCountProjectionMap);
        }
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder, limit, cancellationSignal);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), ContactsContract.AUTHORITY_URI);
        }
        return c;
    }

    private Cursor queryPhoneLookupEnterprise(String phoneNumber, String[] projection, boolean isSipAddress) throws Throwable {
        int corpUserId = UserUtils.getCorpUserId(getContext());
        Uri localUri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon().appendPath(phoneNumber).appendQueryParameter("sip", String.valueOf(isSipAddress)).build();
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "queryPhoneLookupEnterprise: local query URI=" + localUri);
        }
        Cursor local = queryLocal(localUri, projection, null, null, null, 0L, null);
        try {
            if (VERBOSE_LOGGING) {
                MoreDatabaseUtils.dumpCursor("ContactsProvider", "local", local);
            }
            if (local.getCount() > 0 || corpUserId < 0) {
                return local;
            }
            Uri remoteUri = maybeAddUserId(localUri, corpUserId);
            if (VERBOSE_LOGGING) {
                Log.v("ContactsProvider", "queryPhoneLookupEnterprise: corp query URI=" + remoteUri);
            }
            Cursor corp = getContext().getContentResolver().query(remoteUri, projection, null, null, null, null);
            try {
                if (VERBOSE_LOGGING) {
                    MoreDatabaseUtils.dumpCursor("ContactsProvider", "corp raw", corp);
                }
                Cursor rewritten = rewriteCorpPhoneLookup(corp);
                if (VERBOSE_LOGGING) {
                    MoreDatabaseUtils.dumpCursor("ContactsProvider", "corp rewritten", rewritten);
                }
                return rewritten;
            } finally {
                corp.close();
            }
        } catch (Throwable th) {
            local.close();
            throw th;
        }
    }

    static Cursor rewriteCorpPhoneLookup(Cursor original) {
        String[] columns = original.getColumnNames();
        MatrixCursor ret = new MatrixCursor(columns);
        original.moveToPosition(-1);
        while (original.moveToNext()) {
            int contactId = original.getInt(original.getColumnIndex("_id"));
            MatrixCursor.RowBuilder builder = ret.newRow();
            for (int i = 0; i < columns.length; i++) {
                switch (columns[i]) {
                    case "photo_thumb_uri":
                        builder.add(getCorpThumbnailUri(contactId, original));
                        break;
                    case "photo_uri":
                        builder.add(getCorpDisplayPhotoUri(contactId, original));
                        break;
                    case "_id":
                        builder.add(Long.valueOf(original.getLong(i) + ContactsContract.Contacts.ENTERPRISE_CONTACT_ID_BASE));
                        break;
                    case "photo_file_id":
                    case "photo_id":
                    case "custom_ringtone":
                        builder.add(null);
                        break;
                    default:
                        switch (original.getType(i)) {
                            case 0:
                                builder.add(null);
                                break;
                            case 1:
                                builder.add(Long.valueOf(original.getLong(i)));
                                break;
                            case 2:
                                builder.add(Float.valueOf(original.getFloat(i)));
                                break;
                            case 3:
                                builder.add(original.getString(i));
                                break;
                            case 4:
                                builder.add(original.getBlob(i));
                                break;
                        }
                        break;
                }
            }
        }
        return ret;
    }

    private static String getCorpThumbnailUri(long contactId, Cursor originalCursor) {
        if (originalCursor.isNull(originalCursor.getColumnIndex("photo_thumb_uri"))) {
            return null;
        }
        return ContentUris.appendId(ContactsContract.Contacts.CORP_CONTENT_URI.buildUpon(), contactId).appendPath("photo").build().toString();
    }

    private static String getCorpDisplayPhotoUri(long contactId, Cursor originalCursor) {
        return originalCursor.isNull(originalCursor.getColumnIndex("photo_file_id")) ? getCorpThumbnailUri(contactId, originalCursor) : ContentUris.appendId(ContactsContract.Contacts.CORP_CONTENT_URI.buildUpon(), contactId).appendPath("display_photo").build().toString();
    }

    private Cursor queryWithContactIdAndLookupKey(SQLiteQueryBuilder lookupQb, SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder, String groupBy, String limit, String contactIdColumn, long contactId, String lookupKeyColumn, String lookupKey, CancellationSignal cancellationSignal) {
        String[] args;
        if (selectionArgs == null) {
            args = new String[2];
        } else {
            args = new String[selectionArgs.length + 2];
            System.arraycopy(selectionArgs, 0, args, 2, selectionArgs.length);
        }
        args[0] = String.valueOf(contactId);
        args[1] = Uri.encode(lookupKey);
        lookupQb.appendWhere(contactIdColumn + "=? AND " + lookupKeyColumn + "=?");
        Cursor c = query(db, lookupQb, projection, selection, args, sortOrder, groupBy, null, limit, cancellationSignal);
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        return c;
    }

    private void invalidateFastScrollingIndexCache() {
        this.mFastScrollingIndexCache.invalidate();
    }

    private void bundleFastScrollingIndexExtras(Cursor cursor, Uri queryUri, SQLiteDatabase db, SQLiteQueryBuilder qb, String selection, String[] selectionArgs, String sortOrder, String countExpression, CancellationSignal cancellationSignal) {
        Bundle b;
        if (!(cursor instanceof AbstractCursor)) {
            Log.w("ContactsProvider", "Unable to bundle extras.  Cursor is not AbstractCursor.");
            return;
        }
        synchronized (this.mFastScrollingIndexCache) {
            this.mFastScrollingIndexCacheRequestCount++;
            b = this.mFastScrollingIndexCache.get(queryUri, selection, selectionArgs, sortOrder, countExpression);
            if (b == null) {
                this.mFastScrollingIndexCacheMissCount++;
                long start = System.currentTimeMillis();
                b = getFastScrollingIndexExtras(db, qb, selection, selectionArgs, sortOrder, countExpression, cancellationSignal);
                long end = System.currentTimeMillis();
                int time = (int) (end - start);
                this.mTotalTimeFastScrollingIndexGenerate += (long) time;
                if (VERBOSE_LOGGING) {
                    Log.v("ContactsProvider", "getLetterCountExtraBundle took " + time + "ms");
                }
                this.mFastScrollingIndexCache.put(queryUri, selection, selectionArgs, sortOrder, countExpression, b);
            }
        }
        ((AbstractCursor) cursor).setExtras(b);
    }

    private static Bundle getFastScrollingIndexExtras(SQLiteDatabase db, SQLiteQueryBuilder qb, String selection, String[] selectionArgs, String sortOrder, String countExpression, CancellationSignal cancellationSignal) {
        String sortKey;
        String bucketKey;
        String labelKey;
        String sortOrderSuffix = "";
        if (sortOrder != null) {
            int spaceIndex = sortOrder.indexOf(32);
            if (spaceIndex != -1) {
                sortKey = sortOrder.substring(0, spaceIndex);
                sortOrderSuffix = sortOrder.substring(spaceIndex);
            } else {
                sortKey = sortOrder;
            }
        } else {
            sortKey = "sort_key";
        }
        if (TextUtils.equals(sortKey, "sort_key")) {
            bucketKey = "phonebook_bucket";
            labelKey = "phonebook_label";
        } else if (TextUtils.equals(sortKey, "sort_key_alt")) {
            bucketKey = "phonebook_bucket_alt";
            labelKey = "phonebook_label_alt";
        } else {
            return null;
        }
        HashMap<String, String> projectionMap = Maps.newHashMap();
        projectionMap.put("name", sortKey + " AS name");
        projectionMap.put("bucket", bucketKey + " AS bucket");
        projectionMap.put("label", labelKey + " AS label");
        if (TextUtils.isEmpty(countExpression)) {
            countExpression = "*";
        }
        projectionMap.put("count", "COUNT(" + countExpression + ") AS count");
        qb.setProjectionMap(projectionMap);
        String orderBy = "bucket" + sortOrderSuffix + ", name COLLATE PHONEBOOK" + sortOrderSuffix;
        Cursor indexCursor = qb.query(db, AddressBookIndexQuery.COLUMNS, selection, selectionArgs, "bucket, label", null, orderBy, null, cancellationSignal);
        try {
            int numLabels = indexCursor.getCount();
            String[] labels = new String[numLabels];
            int[] counts = new int[numLabels];
            for (int i = 0; i < numLabels; i++) {
                indexCursor.moveToNext();
                labels[i] = indexCursor.getString(2);
                counts[i] = indexCursor.getInt(3);
            }
            return FastScrollingIndexCache.buildExtraBundle(labels, counts);
        } finally {
            indexCursor.close();
        }
    }

    public long lookupContactIdByLookupKey(SQLiteDatabase db, String lookupKey) {
        ContactLookupKey key = new ContactLookupKey();
        ArrayList<ContactLookupKey.LookupKeySegment> segments = key.parse(lookupKey);
        long contactId = -1;
        if (lookupKeyContainsType(segments, 3)) {
            contactId = lookupSingleContactId(db);
        }
        if (lookupKeyContainsType(segments, 0)) {
            contactId = lookupContactIdBySourceIds(db, segments);
            if (contactId != -1) {
                return contactId;
            }
        }
        boolean hasRawContactIds = lookupKeyContainsType(segments, 2);
        if (hasRawContactIds) {
            contactId = lookupContactIdByRawContactIds(db, segments);
            if (contactId != -1) {
                return contactId;
            }
        }
        if (hasRawContactIds || lookupKeyContainsType(segments, 1)) {
            contactId = lookupContactIdByDisplayNames(db, segments);
        }
        return contactId;
    }

    private long lookupSingleContactId(SQLiteDatabase db) {
        Cursor c = db.query("contacts", new String[]{"_id"}, null, null, null, null, null, "1");
        try {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
            return -1L;
        } finally {
            c.close();
        }
    }

    private long lookupContactIdBySourceIds(SQLiteDatabase db, ArrayList<ContactLookupKey.LookupKeySegment> segments) {
        StringBuilder sb = new StringBuilder();
        sb.append("sourceid IN (");
        for (ContactLookupKey.LookupKeySegment segment : segments) {
            if (segment.lookupType == 0) {
                DatabaseUtils.appendEscapedSQLString(sb, segment.key);
                sb.append(",");
            }
        }
        sb.setLength(sb.length() - 1);
        sb.append(") AND contact_id NOT NULL");
        Cursor c = db.query("view_raw_contacts", LookupBySourceIdQuery.COLUMNS, sb.toString(), null, null, null, null);
        while (c.moveToNext()) {
            try {
                String accountTypeAndDataSet = c.getString(1);
                String accountName = c.getString(2);
                int accountHashCode = ContactLookupKey.getAccountHashCode(accountTypeAndDataSet, accountName);
                String sourceId = c.getString(3);
                int i = 0;
                while (true) {
                    if (i < segments.size()) {
                        ContactLookupKey.LookupKeySegment segment2 = segments.get(i);
                        if (segment2.lookupType != 0 || accountHashCode != segment2.accountHashCode || !segment2.key.equals(sourceId)) {
                            i++;
                        } else {
                            segment2.contactId = c.getLong(0);
                            break;
                        }
                    }
                }
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        return getMostReferencedContactId(segments);
    }

    private long lookupContactIdByRawContactIds(SQLiteDatabase db, ArrayList<ContactLookupKey.LookupKeySegment> segments) {
        StringBuilder sb = new StringBuilder();
        sb.append("_id IN (");
        for (ContactLookupKey.LookupKeySegment segment : segments) {
            if (segment.lookupType == 2) {
                sb.append(segment.rawContactId);
                sb.append(",");
            }
        }
        sb.setLength(sb.length() - 1);
        sb.append(") AND contact_id NOT NULL");
        Cursor c = db.query("view_raw_contacts", LookupByRawContactIdQuery.COLUMNS, sb.toString(), null, null, null, null);
        while (c.moveToNext()) {
            try {
                String accountTypeAndDataSet = c.getString(1);
                String accountName = c.getString(2);
                int accountHashCode = ContactLookupKey.getAccountHashCode(accountTypeAndDataSet, accountName);
                String rawContactId = c.getString(3);
                Iterator<ContactLookupKey.LookupKeySegment> it = segments.iterator();
                while (true) {
                    if (it.hasNext()) {
                        ContactLookupKey.LookupKeySegment segment2 = it.next();
                        if (segment2.lookupType == 2 && accountHashCode == segment2.accountHashCode && segment2.rawContactId.equals(rawContactId)) {
                            segment2.contactId = c.getLong(0);
                            break;
                        }
                    }
                }
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        return getMostReferencedContactId(segments);
    }

    private long lookupContactIdByDisplayNames(SQLiteDatabase db, ArrayList<ContactLookupKey.LookupKeySegment> segments) {
        StringBuilder sb = new StringBuilder();
        sb.append("normalized_name IN (");
        for (ContactLookupKey.LookupKeySegment segment : segments) {
            if (segment.lookupType == 1 || segment.lookupType == 2) {
                DatabaseUtils.appendEscapedSQLString(sb, segment.key);
                sb.append(",");
            }
        }
        sb.setLength(sb.length() - 1);
        sb.append(") AND name_type=2 AND contact_id NOT NULL");
        Cursor c = db.query("name_lookup INNER JOIN view_raw_contacts ON (name_lookup.raw_contact_id = view_raw_contacts._id)", LookupByDisplayNameQuery.COLUMNS, sb.toString(), null, null, null, null);
        while (c.moveToNext()) {
            try {
                String accountTypeAndDataSet = c.getString(1);
                String accountName = c.getString(2);
                int accountHashCode = ContactLookupKey.getAccountHashCode(accountTypeAndDataSet, accountName);
                String name = c.getString(3);
                Iterator<ContactLookupKey.LookupKeySegment> it = segments.iterator();
                while (true) {
                    if (it.hasNext()) {
                        ContactLookupKey.LookupKeySegment segment2 = it.next();
                        if (segment2.lookupType == 1 || segment2.lookupType == 2) {
                            if (accountHashCode == segment2.accountHashCode && segment2.key.equals(name)) {
                                segment2.contactId = c.getLong(0);
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                c.close();
                throw th;
            }
        }
        c.close();
        return getMostReferencedContactId(segments);
    }

    private boolean lookupKeyContainsType(ArrayList<ContactLookupKey.LookupKeySegment> segments, int lookupType) {
        for (ContactLookupKey.LookupKeySegment segment : segments) {
            if (segment.lookupType == lookupType) {
                return true;
            }
        }
        return false;
    }

    private long getMostReferencedContactId(ArrayList<ContactLookupKey.LookupKeySegment> segments) {
        long bestContactId = -1;
        int bestRefCount = 0;
        long contactId = -1;
        int count = 0;
        Collections.sort(segments);
        for (ContactLookupKey.LookupKeySegment segment : segments) {
            if (segment.contactId != -1) {
                if (segment.contactId == contactId) {
                    count++;
                } else {
                    if (count > bestRefCount) {
                        bestContactId = contactId;
                        bestRefCount = count;
                    }
                    contactId = segment.contactId;
                    count = 1;
                }
            }
        }
        if (count > bestRefCount) {
            return contactId;
        }
        long contactId2 = bestContactId;
        return contactId2;
    }

    private void setTablesAndProjectionMapForContacts(SQLiteQueryBuilder qb, String[] projection) {
        setTablesAndProjectionMapForContacts(qb, projection, false);
    }

    private void setTablesAndProjectionMapForContacts(SQLiteQueryBuilder qb, String[] projection, boolean includeDataUsageStat) {
        StringBuilder sb = new StringBuilder();
        if (includeDataUsageStat) {
            sb.append("view_data_usage_stat AS data_usage_stat");
            sb.append(" INNER JOIN ");
        }
        sb.append("view_contacts");
        if (includeDataUsageStat) {
            sb.append(" ON (" + DbQueryUtils.concatenateClauses("data_usage_stat.times_used > 0", "contact_id=view_contacts._id") + ")");
        }
        appendContactPresenceJoin(sb, projection, "_id");
        appendContactStatusUpdateJoin(sb, projection, "status_update_id");
        qb.setTables(sb.toString());
        qb.setProjectionMap(sContactsProjectionMap);
    }

    private void setTablesAndProjectionMapForContactsWithSnippet(SQLiteQueryBuilder qb, Uri uri, String[] projection, String filter, long directoryId, boolean deferSnippeting) {
        StringBuilder sb = new StringBuilder();
        sb.append("view_contacts");
        if (filter != null) {
            filter = filter.trim();
        }
        if (TextUtils.isEmpty(filter) || (directoryId != -1 && directoryId != 0)) {
            sb.append(" JOIN (SELECT NULL AS snippet WHERE 0)");
        } else {
            appendSearchIndexJoin(sb, uri, projection, filter, deferSnippeting);
        }
        appendContactPresenceJoin(sb, projection, "_id");
        appendContactStatusUpdateJoin(sb, projection, "status_update_id");
        qb.setTables(sb.toString());
        qb.setProjectionMap(sContactsProjectionWithSnippetMap);
    }

    private void appendSearchIndexJoin(StringBuilder sb, Uri uri, String[] projection, String filter, boolean deferSnippeting) {
        if (snippetNeeded(projection)) {
            String[] args = null;
            String snippetArgs = getQueryParameter(uri, "snippet_args");
            if (snippetArgs != null) {
                args = snippetArgs.split(",");
            }
            String startMatch = (args == null || args.length <= 0) ? "[" : args[0];
            String endMatch = (args == null || args.length <= 1) ? "]" : args[1];
            String ellipsis = (args == null || args.length <= 2) ? "…" : args[2];
            int maxTokens = (args == null || args.length <= 3) ? 5 : Integer.parseInt(args[3]);
            appendSearchIndexJoin(sb, filter, true, startMatch, endMatch, ellipsis, maxTokens, deferSnippeting);
            return;
        }
        appendSearchIndexJoin(sb, filter, false, null, null, null, 0, false);
    }

    public void appendSearchIndexJoin(StringBuilder sb, String filter, boolean snippetNeeded, String startMatch, String endMatch, String ellipsis, int maxTokens, boolean deferSnippeting) {
        boolean isEmailAddress = false;
        String emailAddress = null;
        boolean isPhoneNumber = false;
        String phoneNumber = null;
        String numberE164 = null;
        if (filter.indexOf(64) != -1) {
            emailAddress = this.mDbHelper.get().extractAddressFromEmailAddress(filter);
            isEmailAddress = !TextUtils.isEmpty(emailAddress);
        } else {
            isPhoneNumber = isPhoneNumber(filter);
            if (isPhoneNumber) {
                phoneNumber = PhoneNumberUtils.normalizeNumber(filter);
                numberE164 = PhoneNumberUtils.formatNumberToE164(phoneNumber, this.mDbHelper.get().getCurrentCountryIso());
            }
        }
        sb.append(" JOIN (SELECT contact_id AS snippet_contact_id");
        if (snippetNeeded) {
            sb.append(", ");
            if (isEmailAddress) {
                sb.append("ifnull(");
                if (!deferSnippeting) {
                    DatabaseUtils.appendEscapedSQLString(sb, startMatch);
                    sb.append("||");
                }
                sb.append("(SELECT MIN(data1)");
                sb.append(" FROM data JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id)");
                sb.append(" WHERE  search_index.contact_id");
                sb.append("=contact_id AND data1 LIKE ");
                DatabaseUtils.appendEscapedSQLString(sb, filter + "%");
                sb.append(")");
                if (!deferSnippeting) {
                    sb.append("||");
                    DatabaseUtils.appendEscapedSQLString(sb, endMatch);
                }
                sb.append(",");
                if (deferSnippeting) {
                    sb.append("content");
                } else {
                    appendSnippetFunction(sb, startMatch, endMatch, ellipsis, maxTokens);
                }
                sb.append(")");
            } else if (isPhoneNumber) {
                sb.append("ifnull(");
                if (!deferSnippeting) {
                    DatabaseUtils.appendEscapedSQLString(sb, startMatch);
                    sb.append("||");
                }
                sb.append("(SELECT MIN(data1)");
                sb.append(" FROM data JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) JOIN phone_lookup");
                sb.append(" ON data._id");
                sb.append("=phone_lookup.data_id");
                sb.append(" WHERE  search_index.contact_id");
                sb.append("=contact_id");
                sb.append(" AND normalized_number LIKE '");
                sb.append(phoneNumber);
                sb.append("%'");
                if (!TextUtils.isEmpty(numberE164)) {
                    sb.append(" OR normalized_number LIKE '");
                    sb.append(numberE164);
                    sb.append("%'");
                }
                sb.append(")");
                if (!deferSnippeting) {
                    sb.append("||");
                    DatabaseUtils.appendEscapedSQLString(sb, endMatch);
                }
                sb.append(",");
                if (deferSnippeting) {
                    sb.append("content");
                } else {
                    appendSnippetFunction(sb, startMatch, endMatch, ellipsis, maxTokens);
                }
                sb.append(")");
            } else {
                String normalizedFilter = NameNormalizer.normalize(filter);
                if (!TextUtils.isEmpty(normalizedFilter)) {
                    if (deferSnippeting) {
                        sb.append("content");
                    } else {
                        sb.append("(CASE WHEN EXISTS (SELECT 1 FROM ");
                        sb.append("raw_contacts AS rc INNER JOIN ");
                        sb.append("name_lookup AS nl ON (rc._id");
                        sb.append("=nl.raw_contact_id");
                        sb.append(") WHERE nl.normalized_name");
                        sb.append(" GLOB '" + normalizedFilter + "*' AND ");
                        sb.append("nl.name_type=");
                        sb.append("2 AND ");
                        sb.append("search_index.contact_id");
                        sb.append("=rc.contact_id");
                        sb.append(") THEN NULL ELSE ");
                        appendSnippetFunction(sb, startMatch, endMatch, ellipsis, maxTokens);
                        sb.append(" END)");
                    }
                } else {
                    sb.append("NULL");
                }
            }
            sb.append(" AS snippet");
        }
        sb.append(" FROM search_index");
        sb.append(" WHERE ");
        sb.append("search_index MATCH '");
        if (isEmailAddress) {
            String sanitizedEmailAddress = emailAddress == null ? "" : sanitizeMatch(emailAddress);
            sb.append("\"");
            sb.append(sanitizedEmailAddress);
            sb.append("*\"");
        } else if (isPhoneNumber) {
            String phoneNumberCriteria = " OR tokens:" + phoneNumber + "*";
            String numberE164Criteria = (numberE164 == null || TextUtils.equals(numberE164, phoneNumber)) ? "" : " OR tokens:" + numberE164 + "*";
            String commonCriteria = phoneNumberCriteria + numberE164Criteria;
            sb.append(SearchIndexManager.getFtsMatchQuery(filter, SearchIndexManager.FtsQueryBuilder.getDigitsQueryBuilder(commonCriteria)));
        } else {
            sb.append(SearchIndexManager.getFtsMatchQuery(filter, SearchIndexManager.FtsQueryBuilder.SCOPED_NAME_NORMALIZING));
        }
        sb.append("' AND snippet_contact_id IN default_directory)");
        sb.append(" ON (_id=snippet_contact_id)");
    }

    private static String sanitizeMatch(String filter) {
        return filter.replace("'", "").replace("*", "").replace("-", "").replace("\"", "");
    }

    private void appendSnippetFunction(StringBuilder sb, String startMatch, String endMatch, String ellipsis, int maxTokens) {
        sb.append("snippet(search_index,");
        DatabaseUtils.appendEscapedSQLString(sb, startMatch);
        sb.append(",");
        DatabaseUtils.appendEscapedSQLString(sb, endMatch);
        sb.append(",");
        DatabaseUtils.appendEscapedSQLString(sb, ellipsis);
        sb.append(",1,");
        sb.append(maxTokens);
        sb.append(")");
    }

    private void setTablesAndProjectionMapForRawContacts(SQLiteQueryBuilder qb, Uri uri) {
        qb.setTables("view_raw_contacts");
        qb.setProjectionMap(sRawContactsProjectionMap);
        appendAccountIdFromParameter(qb, uri);
    }

    private void setTablesAndProjectionMapForRawEntities(SQLiteQueryBuilder qb, Uri uri) {
        qb.setTables("view_raw_entities");
        qb.setProjectionMap(sRawEntityProjectionMap);
        appendAccountIdFromParameter(qb, uri);
    }

    private void setTablesAndProjectionMapForData(SQLiteQueryBuilder qb, Uri uri, String[] projection, boolean distinct) {
        setTablesAndProjectionMapForData(qb, uri, projection, distinct, false, null);
    }

    private void setTablesAndProjectionMapForData(SQLiteQueryBuilder qb, Uri uri, String[] projection, boolean distinct, boolean addSipLookupColumns) {
        setTablesAndProjectionMapForData(qb, uri, projection, distinct, addSipLookupColumns, null);
    }

    private void setTablesAndProjectionMapForData(SQLiteQueryBuilder qb, Uri uri, String[] projection, boolean distinct, Integer usageType) {
        setTablesAndProjectionMapForData(qb, uri, projection, distinct, false, usageType);
    }

    private void setTablesAndProjectionMapForData(SQLiteQueryBuilder qb, Uri uri, String[] projection, boolean distinct, boolean addSipLookupColumns, Integer usageType) {
        ProjectionMap projectionMap;
        StringBuilder sb = new StringBuilder();
        sb.append("view_data");
        sb.append(" data");
        appendContactPresenceJoin(sb, projection, "contact_id");
        appendContactStatusUpdateJoin(sb, projection, "status_update_id");
        appendDataPresenceJoin(sb, projection, "data._id");
        appendDataStatusUpdateJoin(sb, projection, "data._id");
        appendDataUsageStatJoin(sb, usageType == null ? -1 : usageType.intValue(), "data._id");
        qb.setTables(sb.toString());
        boolean useDistinct = distinct || !ContactsDatabaseHelper.isInProjection(projection, DISTINCT_DATA_PROHIBITING_COLUMNS);
        qb.setDistinct(useDistinct);
        if (addSipLookupColumns) {
            projectionMap = useDistinct ? sDistinctDataSipLookupProjectionMap : sDataSipLookupProjectionMap;
        } else {
            projectionMap = useDistinct ? sDistinctDataProjectionMap : sDataProjectionMap;
        }
        qb.setProjectionMap(projectionMap);
        appendAccountIdFromParameter(qb, uri);
    }

    private void setTableAndProjectionMapForStatusUpdates(SQLiteQueryBuilder qb, String[] projection) {
        StringBuilder sb = new StringBuilder();
        sb.append("view_data");
        sb.append(" data");
        appendDataPresenceJoin(sb, projection, "data._id");
        appendDataStatusUpdateJoin(sb, projection, "data._id");
        qb.setTables(sb.toString());
        qb.setProjectionMap(sStatusUpdatesProjectionMap);
    }

    private void setTablesAndProjectionMapForStreamItems(SQLiteQueryBuilder qb) {
        qb.setTables("view_stream_items");
        qb.setProjectionMap(sStreamItemsProjectionMap);
    }

    private void setTablesAndProjectionMapForStreamItemPhotos(SQLiteQueryBuilder qb) {
        qb.setTables("photo_files JOIN stream_item_photos ON (stream_item_photos.photo_file_id=photo_files._id) JOIN stream_items ON (stream_item_photos.stream_item_id=stream_items._id) JOIN raw_contacts ON (stream_items.raw_contact_id=raw_contacts._id)");
        qb.setProjectionMap(sStreamItemPhotosProjectionMap);
    }

    private void setTablesAndProjectionMapForEntities(SQLiteQueryBuilder qb, Uri uri, String[] projection) {
        StringBuilder sb = new StringBuilder();
        sb.append("view_entities");
        sb.append(" data");
        appendContactPresenceJoin(sb, projection, "contact_id");
        appendContactStatusUpdateJoin(sb, projection, "status_update_id");
        appendDataPresenceJoin(sb, projection, "data_id");
        appendDataStatusUpdateJoin(sb, projection, "data_id");
        appendDataUsageStatJoin(sb, -1, "data_id");
        qb.setTables(sb.toString());
        qb.setProjectionMap(sEntityProjectionMap);
        appendAccountIdFromParameter(qb, uri);
    }

    private void appendContactStatusUpdateJoin(StringBuilder sb, String[] projection, String lastStatusUpdateIdColumn) {
        if (ContactsDatabaseHelper.isInProjection(projection, "contact_status", "contact_status_res_package", "contact_status_icon", "contact_status_label", "contact_status_ts")) {
            sb.append(" LEFT OUTER JOIN status_updates contacts_status_updates ON (" + lastStatusUpdateIdColumn + "=contacts_status_updates.status_update_data_id)");
        }
    }

    private void appendDataStatusUpdateJoin(StringBuilder sb, String[] projection, String dataIdColumn) {
        if (ContactsDatabaseHelper.isInProjection(projection, "status", "status_res_package", "status_icon", "status_label", "status_ts")) {
            sb.append(" LEFT OUTER JOIN status_updates ON (status_updates.status_update_data_id=" + dataIdColumn + ")");
        }
    }

    private void appendDataUsageStatJoin(StringBuilder sb, int usageType, String dataIdColumn) {
        if (usageType != -1) {
            sb.append(" LEFT OUTER JOIN data_usage_stat ON (data_usage_stat.data_id=");
            sb.append(dataIdColumn);
            sb.append(" AND data_usage_stat.usage_type=");
            sb.append(usageType);
            sb.append(")");
            return;
        }
        sb.append(" LEFT OUTER JOIN (SELECT data_usage_stat.data_id as STAT_DATA_ID, SUM(data_usage_stat.times_used) as times_used, MAX(data_usage_stat.last_time_used) as last_time_used FROM data_usage_stat GROUP BY data_usage_stat.data_id) as data_usage_stat");
        sb.append(" ON (STAT_DATA_ID=");
        sb.append(dataIdColumn);
        sb.append(")");
    }

    private void appendContactPresenceJoin(StringBuilder sb, String[] projection, String contactIdColumn) {
        if (ContactsDatabaseHelper.isInProjection(projection, "contact_presence", "contact_chat_capability")) {
            sb.append(" LEFT OUTER JOIN agg_presence ON (" + contactIdColumn + " = agg_presence.presence_contact_id)");
        }
    }

    private void appendDataPresenceJoin(StringBuilder sb, String[] projection, String dataIdColumn) {
        if (ContactsDatabaseHelper.isInProjection(projection, "mode", "chat_capability")) {
            sb.append(" LEFT OUTER JOIN presence ON (presence_data_id=" + dataIdColumn + ")");
        }
    }

    private void appendLocalDirectoryAndAccountSelectionIfNeeded(SQLiteQueryBuilder qb, long directoryId, Uri uri) {
        StringBuilder sb = new StringBuilder();
        if (directoryId == 0) {
            sb.append("(_id IN default_directory)");
        } else if (directoryId == 1) {
            sb.append("(_id NOT IN default_directory)");
        } else {
            sb.append("(1)");
        }
        AccountWithDataSet accountWithDataSet = getAccountWithDataSetFromUri(uri);
        boolean validAccount = !TextUtils.isEmpty(accountWithDataSet.getAccountName());
        if (validAccount) {
            Long accountId = this.mDbHelper.get().getAccountIdOrNull(accountWithDataSet);
            if (accountId == null) {
                sb.setLength(0);
                sb.append("(1=2)");
            } else {
                sb.append(" AND (_id IN (SELECT contact_id FROM raw_contacts WHERE account_id=" + accountId.toString() + "))");
            }
        }
        qb.appendWhere(sb.toString());
    }

    private void appendAccountFromParameter(SQLiteQueryBuilder qb, Uri uri) {
        String toAppend;
        AccountWithDataSet accountWithDataSet = getAccountWithDataSetFromUri(uri);
        boolean validAccount = !TextUtils.isEmpty(accountWithDataSet.getAccountName());
        if (validAccount) {
            String toAppend2 = "(account_name=" + DatabaseUtils.sqlEscapeString(accountWithDataSet.getAccountName()) + " AND account_type=" + DatabaseUtils.sqlEscapeString(accountWithDataSet.getAccountType());
            if (accountWithDataSet.getDataSet() == null) {
                toAppend = toAppend2 + " AND data_set IS NULL";
            } else {
                toAppend = toAppend2 + " AND data_set=" + DatabaseUtils.sqlEscapeString(accountWithDataSet.getDataSet());
            }
            qb.appendWhere(toAppend + ")");
            return;
        }
        qb.appendWhere("1");
    }

    private void appendAccountIdFromParameter(SQLiteQueryBuilder qb, Uri uri) {
        AccountWithDataSet accountWithDataSet = getAccountWithDataSetFromUri(uri);
        boolean validAccount = !TextUtils.isEmpty(accountWithDataSet.getAccountName());
        if (validAccount) {
            Long accountId = this.mDbHelper.get().getAccountIdOrNull(accountWithDataSet);
            if (accountId == null) {
                qb.appendWhere("(1=2)");
                return;
            } else {
                qb.appendWhere("(account_id=" + accountId.toString() + ")");
                return;
            }
        }
        qb.appendWhere("1");
    }

    private AccountWithDataSet getAccountWithDataSetFromUri(Uri uri) {
        String accountName = getQueryParameter(uri, "account_name");
        String accountType = getQueryParameter(uri, "account_type");
        String dataSet = getQueryParameter(uri, "data_set");
        boolean partialUri = TextUtils.isEmpty(accountName) ^ TextUtils.isEmpty(accountType);
        if (partialUri) {
            throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Must specify both or neither of ACCOUNT_NAME and ACCOUNT_TYPE", uri));
        }
        return AccountWithDataSet.get(accountName, accountType, dataSet);
    }

    private String appendAccountToSelection(Uri uri, String selection) {
        AccountWithDataSet accountWithDataSet = getAccountWithDataSetFromUri(uri);
        boolean validAccount = !TextUtils.isEmpty(accountWithDataSet.getAccountName());
        if (validAccount) {
            StringBuilder selectionSb = new StringBuilder("account_name=");
            selectionSb.append(DatabaseUtils.sqlEscapeString(accountWithDataSet.getAccountName()));
            selectionSb.append(" AND account_type=");
            selectionSb.append(DatabaseUtils.sqlEscapeString(accountWithDataSet.getAccountType()));
            if (accountWithDataSet.getDataSet() == null) {
                selectionSb.append(" AND data_set IS NULL");
            } else {
                selectionSb.append(" AND data_set=").append(DatabaseUtils.sqlEscapeString(accountWithDataSet.getDataSet()));
            }
            if (!TextUtils.isEmpty(selection)) {
                selectionSb.append(" AND (");
                selectionSb.append(selection);
                selectionSb.append(')');
            }
            return selectionSb.toString();
        }
        return selection;
    }

    private String appendAccountIdToSelection(Uri uri, String selection) {
        AccountWithDataSet accountWithDataSet = getAccountWithDataSetFromUri(uri);
        boolean validAccount = !TextUtils.isEmpty(accountWithDataSet.getAccountName());
        if (validAccount) {
            StringBuilder selectionSb = new StringBuilder();
            Long accountId = this.mDbHelper.get().getAccountIdOrNull(accountWithDataSet);
            if (accountId == null) {
                selectionSb.append("(1=2)");
            } else {
                selectionSb.append("account_id=");
                selectionSb.append(Long.toString(accountId.longValue()));
            }
            if (!TextUtils.isEmpty(selection)) {
                selectionSb.append(" AND (");
                selectionSb.append(selection);
                selectionSb.append(')');
            }
            return selectionSb.toString();
        }
        return selection;
    }

    private String getLimit(Uri uri) {
        String strValueOf = null;
        String limitParam = getQueryParameter(uri, "limit");
        if (limitParam != null) {
            try {
                int l = Integer.parseInt(limitParam);
                if (l < 0) {
                    Log.w("ContactsProvider", "Invalid limit parameter: " + limitParam);
                } else {
                    strValueOf = String.valueOf(l);
                }
            } catch (NumberFormatException e) {
                Log.w("ContactsProvider", "Invalid limit parameter: " + limitParam);
            }
        }
        return strValueOf;
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        AssetFileDescriptor ret;
        boolean success = false;
        try {
            waitForAccess(mode.equals("r") ? this.mReadAccessLatch : this.mWriteAccessLatch);
            if (mapsToProfileDb(uri)) {
                switchToProfileMode();
                ret = this.mProfileProvider.openAssetFile(uri, mode);
            } else {
                switchToContactMode();
                ret = openAssetFileLocal(uri, mode);
            }
            success = true;
            return ret;
        } finally {
            if (VERBOSE_LOGGING) {
                Log.v("ContactsProvider", "openAssetFile uri=" + uri + " mode=" + mode + " success=" + success + " CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
            }
        }
    }

    public AssetFileDescriptor openAssetFileLocal(Uri uri, String mode) throws FileNotFoundException {
        long ident = Binder.clearCallingIdentity();
        try {
            return openAssetFileInner(uri, mode);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private AssetFileDescriptor openAssetFileInner(Uri uri, String mode) throws Throwable {
        Cursor c;
        AssetFileDescriptor assetFileDescriptorOpenPhotoAssetFile;
        AssetFileDescriptor assetFileDescriptorOpenPhotoAssetFile2;
        boolean writing = mode.contains("w");
        SQLiteDatabase db = this.mDbHelper.get().getDatabase(writing);
        int match = sUriMatcher.match(uri);
        switch (match) {
            case 1009:
                long contactId = Long.parseLong(uri.getPathSegments().get(1));
                return openPhotoAssetFile(db, uri, mode, "_id=photo_id AND contact_id=?", new String[]{String.valueOf(contactId)});
            case 1010:
            case 1011:
            case 1013:
            case 1014:
                if (!mode.equals("r")) {
                    throw new IllegalArgumentException("Photos retrieved by contact lookup key can only be read.");
                }
                List<String> pathSegments = uri.getPathSegments();
                int segmentCount = pathSegments.size();
                if (segmentCount < 4) {
                    throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Missing a lookup key", uri));
                }
                boolean forDisplayPhoto = match == 1014 || match == 1013;
                String lookupKey = pathSegments.get(2);
                String[] projection = {"photo_id", "photo_file_id"};
                if (segmentCount == 5) {
                    long contactId2 = Long.parseLong(pathSegments.get(3));
                    SQLiteQueryBuilder lookupQb = new SQLiteQueryBuilder();
                    setTablesAndProjectionMapForContacts(lookupQb, projection);
                    c = queryWithContactIdAndLookupKey(lookupQb, db, projection, null, null, null, null, null, "_id", contactId2, "lookup", lookupKey, null);
                    if (c != null) {
                        try {
                            c.moveToFirst();
                            if (forDisplayPhoto) {
                                long photoFileId = c.getLong(c.getColumnIndex("photo_file_id"));
                                assetFileDescriptorOpenPhotoAssetFile2 = openDisplayPhotoForRead(photoFileId);
                            } else {
                                long photoId = c.getLong(c.getColumnIndex("photo_id"));
                                assetFileDescriptorOpenPhotoAssetFile2 = openPhotoAssetFile(db, uri, mode, "_id=?", new String[]{String.valueOf(photoId)});
                                c.close();
                            }
                            return assetFileDescriptorOpenPhotoAssetFile2;
                        } finally {
                        }
                    }
                }
                SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
                setTablesAndProjectionMapForContacts(qb, projection);
                long contactId3 = lookupContactIdByLookupKey(db, lookupKey);
                c = qb.query(db, projection, "_id=?", new String[]{String.valueOf(contactId3)}, null, null, null);
                try {
                    c.moveToFirst();
                    if (forDisplayPhoto) {
                        long photoFileId2 = c.getLong(c.getColumnIndex("photo_file_id"));
                        assetFileDescriptorOpenPhotoAssetFile = openDisplayPhotoForRead(photoFileId2);
                    } else {
                        long photoId2 = c.getLong(c.getColumnIndex("photo_id"));
                        assetFileDescriptorOpenPhotoAssetFile = openPhotoAssetFile(db, uri, mode, "_id=?", new String[]{String.valueOf(photoId2)});
                        c.close();
                    }
                    return assetFileDescriptorOpenPhotoAssetFile;
                } finally {
                }
            case 1012:
                if (!mode.equals("r")) {
                    throw new IllegalArgumentException("Display photos retrieved by contact ID can only be read.");
                }
                long contactId4 = Long.parseLong(uri.getPathSegments().get(1));
                c = db.query("contacts", new String[]{"photo_file_id"}, "_id=?", new String[]{String.valueOf(contactId4)}, null, null, null);
                try {
                    if (c.moveToFirst()) {
                        long photoFileId3 = c.getLong(0);
                        return openDisplayPhotoForRead(photoFileId3);
                    }
                    throw new FileNotFoundException(uri.toString());
                } finally {
                }
            case 1015:
                ByteArrayOutputStream localStream = new ByteArrayOutputStream();
                outputRawContactsAsVCard(uri, localStream, null, null);
                return buildAssetFileDescriptor(localStream);
            case 1016:
                String lookupKeys = uri.getPathSegments().get(2);
                String[] lookupKeyList = lookupKeys.split(":");
                StringBuilder inBuilder = new StringBuilder();
                Uri queryUri = ContactsContract.Contacts.CONTENT_URI;
                int index = 0;
                for (String lookupKey2 : lookupKeyList) {
                    inBuilder.append(index == 0 ? "(" : ",");
                    long contactId5 = lookupContactIdByLookupKey(db, lookupKey2);
                    inBuilder.append(contactId5);
                    index++;
                }
                inBuilder.append(')');
                String selection = "_id IN " + inBuilder.toString();
                ByteArrayOutputStream localStream2 = new ByteArrayOutputStream();
                outputRawContactsAsVCard(queryUri, localStream2, selection, null);
                return buildAssetFileDescriptor(localStream2);
            case 1027:
                long contactId6 = Long.parseLong(uri.getPathSegments().get(1));
                return openCorpContactPicture(contactId6, uri, mode, false);
            case 1028:
                long contactId7 = Long.parseLong(uri.getPathSegments().get(1));
                return openCorpContactPicture(contactId7, uri, mode, true);
            case 2006:
                long rawContactId = Long.parseLong(uri.getPathSegments().get(1));
                boolean writeable = !mode.equals("r");
                SQLiteQueryBuilder qb2 = new SQLiteQueryBuilder();
                String[] projection2 = {"_id", "data14"};
                setTablesAndProjectionMapForData(qb2, uri, projection2, false);
                long photoMimetypeId = this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/photo");
                c = qb2.query(db, projection2, "raw_contact_id=? AND mimetype_id=?", new String[]{String.valueOf(rawContactId), String.valueOf(photoMimetypeId)}, null, null, "is_primary DESC");
                long dataId = 0;
                long photoFileId4 = 0;
                try {
                    if (c.getCount() >= 1) {
                        c.moveToFirst();
                        dataId = c.getLong(0);
                        photoFileId4 = c.getLong(1);
                        break;
                    }
                    if (writeable) {
                        return openDisplayPhotoForWrite(rawContactId, dataId, uri, mode);
                    }
                    return openDisplayPhotoForRead(photoFileId4);
                } finally {
                }
            case 3001:
                long dataId2 = Long.parseLong(uri.getPathSegments().get(1));
                long photoMimetypeId2 = this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/photo");
                return openPhotoAssetFile(db, uri, mode, "_id=? AND mimetype_id=" + photoMimetypeId2, new String[]{String.valueOf(dataId2)});
            case 19004:
                ByteArrayOutputStream localStream3 = new ByteArrayOutputStream();
                outputRawContactsAsVCard(uri, localStream3, null, null);
                return buildAssetFileDescriptor(localStream3);
            case 19012:
                if (!mode.equals("r")) {
                    throw new IllegalArgumentException("Display photos retrieved by contact ID can only be read.");
                }
                c = db.query("contacts", new String[]{"photo_file_id"}, null, null, null, null, null);
                try {
                    if (c.moveToFirst()) {
                        long photoFileId5 = c.getLong(0);
                        return openDisplayPhotoForRead(photoFileId5);
                    }
                    throw new FileNotFoundException(uri.toString());
                } finally {
                }
            case 22000:
                long photoFileId6 = ContentUris.parseId(uri);
                if (!mode.equals("r")) {
                    throw new IllegalArgumentException("Display photos retrieved by key can only be read.");
                }
                return openDisplayPhotoForRead(photoFileId6);
            default:
                throw new FileNotFoundException(this.mDbHelper.get().exceptionMessage("File does not exist", uri));
        }
    }

    private AssetFileDescriptor openCorpContactPicture(long contactId, Uri uri, String mode, boolean displayPhoto) throws FileNotFoundException {
        if (!mode.equals("r")) {
            throw new IllegalArgumentException("Photos retrieved by contact ID can only be read.");
        }
        int corpUserId = UserUtils.getCorpUserId(getContext());
        if (corpUserId < 0) {
            throw new FileNotFoundException(uri.toString());
        }
        Uri corpUri = maybeAddUserId(ContentUris.appendId(ContactsContract.Contacts.CONTENT_URI.buildUpon(), contactId).appendPath(displayPhoto ? "display_photo" : "photo").build(), corpUserId);
        return getContext().getContentResolver().openAssetFileDescriptor(corpUri, mode);
    }

    private AssetFileDescriptor openPhotoAssetFile(SQLiteDatabase db, Uri uri, String mode, String selection, String[] selectionArgs) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException(this.mDbHelper.get().exceptionMessage("Mode " + mode + " not supported.", uri));
        }
        String sql = "SELECT data15 FROM view_data WHERE " + selection;
        try {
            return makeAssetFileDescriptor(DatabaseUtils.blobFileDescriptorForQuery(db, sql, selectionArgs));
        } catch (SQLiteDoneException e) {
            throw new FileNotFoundException(uri.toString());
        }
    }

    private AssetFileDescriptor openDisplayPhotoForRead(long photoFileId) throws FileNotFoundException {
        PhotoStore.Entry entry = this.mPhotoStore.get().get(photoFileId);
        if (entry != null) {
            try {
                return makeAssetFileDescriptor(ParcelFileDescriptor.open(new File(entry.path), 268435456), entry.size);
            } catch (FileNotFoundException fnfe) {
                scheduleBackgroundTask(10);
                throw fnfe;
            }
        }
        scheduleBackgroundTask(10);
        throw new FileNotFoundException("No photo file found for ID " + photoFileId);
    }

    private AssetFileDescriptor openDisplayPhotoForWrite(long rawContactId, long dataId, Uri uri, String mode) {
        try {
            ParcelFileDescriptor[] pipeFds = ParcelFileDescriptor.createPipe();
            PipeMonitor pipeMonitor = new PipeMonitor(rawContactId, dataId, pipeFds[0]);
            pipeMonitor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[]) null);
            return new AssetFileDescriptor(pipeFds[1], 0L, -1L);
        } catch (IOException e) {
            Log.e("ContactsProvider", "Could not create temp image file in mode " + mode);
            return null;
        }
    }

    private class PipeMonitor extends AsyncTask<Object, Object, Object> {
        private final long mDataId;
        private final ParcelFileDescriptor mDescriptor;
        private final long mRawContactId;

        private PipeMonitor(long rawContactId, long dataId, ParcelFileDescriptor descriptor) {
            this.mRawContactId = rawContactId;
            this.mDataId = dataId;
            this.mDescriptor = descriptor;
        }

        @Override
        protected Object doInBackground(Object... params) {
            ParcelFileDescriptor.AutoCloseInputStream is = new ParcelFileDescriptor.AutoCloseInputStream(this.mDescriptor);
            try {
                try {
                    Bitmap b = BitmapFactory.decodeStream(is);
                    if (b != null) {
                        ContactsProvider2.this.waitForAccess(ContactsProvider2.this.mWriteAccessLatch);
                        PhotoProcessor processor = new PhotoProcessor(b, ContactsProvider2.this.getMaxDisplayPhotoDim(), ContactsProvider2.this.getMaxThumbnailDim());
                        PhotoStore photoStore = ContactsContract.isProfileId(this.mRawContactId) ? ContactsProvider2.this.mProfilePhotoStore : ContactsProvider2.this.mContactsPhotoStore;
                        long photoFileId = photoStore.insert(processor);
                        if (this.mDataId != 0) {
                            ContentValues updateValues = new ContentValues();
                            updateValues.put("skip_processing", (Boolean) true);
                            if (photoFileId != 0) {
                                updateValues.put("data14", Long.valueOf(photoFileId));
                            }
                            updateValues.put("data15", processor.getThumbnailPhotoBytes());
                            ContactsProvider2.this.update(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, this.mDataId), updateValues, null, null);
                        } else {
                            ContentValues insertValues = new ContentValues();
                            insertValues.put("skip_processing", (Boolean) true);
                            insertValues.put("mimetype", "vnd.android.cursor.item/photo");
                            insertValues.put("is_primary", (Integer) 1);
                            if (photoFileId != 0) {
                                insertValues.put("data14", Long.valueOf(photoFileId));
                            }
                            insertValues.put("data15", processor.getThumbnailPhotoBytes());
                            ContactsProvider2.this.insert(ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendPath(String.valueOf(this.mRawContactId)).appendPath("data").build(), insertValues);
                        }
                    }
                    IoUtils.closeQuietly(is);
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(is);
                throw th;
            }
        }
    }

    private AssetFileDescriptor buildAssetFileDescriptor(ByteArrayOutputStream stream) {
        try {
            stream.flush();
            byte[] byteData = stream.toByteArray();
            return makeAssetFileDescriptor(ParcelFileDescriptor.fromData(byteData, "contactAssetFile"), byteData.length);
        } catch (IOException e) {
            Log.w("ContactsProvider", "Problem writing stream into an ParcelFileDescriptor: " + e.toString());
            return null;
        }
    }

    private AssetFileDescriptor makeAssetFileDescriptor(ParcelFileDescriptor fd) {
        return makeAssetFileDescriptor(fd, -1L);
    }

    private AssetFileDescriptor makeAssetFileDescriptor(ParcelFileDescriptor fd, long length) {
        if (fd != null) {
            return new AssetFileDescriptor(fd, 0L, length);
        }
        return null;
    }

    private void outputRawContactsAsVCard(Uri uri, OutputStream stream, String selection, String[] selectionArgs) throws Throwable {
        Writer writer;
        Context context = getContext();
        int vcardconfig = VCardConfig.VCARD_TYPE_DEFAULT;
        if (uri.getBooleanQueryParameter("nophoto", false)) {
            vcardconfig |= 8388608;
        }
        VCardComposer composer = new VCardComposer(context, vcardconfig, false);
        Writer writer2 = null;
        Uri rawContactsUri = mapsToProfileDb(uri) ? preAuthorizeUri(ContactsContract.RawContactsEntity.PROFILE_CONTENT_URI) : ContactsContract.RawContactsEntity.CONTENT_URI;
        try {
            try {
                writer = new BufferedWriter(new OutputStreamWriter(stream));
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            if (!composer.init(uri, selection, selectionArgs, null, rawContactsUri)) {
                Log.w("ContactsProvider", "Failed to init VCardComposer");
                composer.terminate();
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e2) {
                        Log.w("ContactsProvider", "IOException during closing output stream: " + e2);
                    }
                }
                return;
            }
            while (!composer.isAfterLast()) {
                writer.write(composer.createOneEntry());
            }
            composer.terminate();
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e3) {
                    Log.w("ContactsProvider", "IOException during closing output stream: " + e3);
                }
            }
        } catch (IOException e4) {
            e = e4;
            writer2 = writer;
            Log.e("ContactsProvider", "IOException: " + e);
            composer.terminate();
            if (writer2 != null) {
                try {
                    writer2.close();
                } catch (IOException e5) {
                    Log.w("ContactsProvider", "IOException during closing output stream: " + e5);
                }
            }
        } catch (Throwable th2) {
            th = th2;
            writer2 = writer;
            composer.terminate();
            if (writer2 != null) {
                try {
                    writer2.close();
                } catch (IOException e6) {
                    Log.w("ContactsProvider", "IOException during closing output stream: " + e6);
                }
            }
            throw th;
        }
    }

    @Override
    public String getType(Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case 1000:
                return "vnd.android.cursor.dir/contact";
            case 1001:
            case 1002:
            case 1003:
            case 19000:
                return "vnd.android.cursor.item/contact";
            case 1009:
            case 1010:
            case 1011:
            case 1012:
            case 1013:
            case 1014:
            case 2006:
            case 22000:
                return "image/jpeg";
            case 1015:
            case 1016:
            case 19004:
                return "text/x-vcard";
            case 2002:
            case 19005:
                return "vnd.android.cursor.dir/raw_contact";
            case 2003:
            case 19006:
                return "vnd.android.cursor.item/raw_contact";
            case 3000:
            case 19002:
                return "vnd.android.cursor.dir/data";
            case 3001:
                waitForAccess(this.mReadAccessLatch);
                long id = ContentUris.parseId(uri);
                if (ContactsContract.isProfileId(id)) {
                    return this.mProfileHelper.getDataMimeType(id);
                }
                return this.mContactsHelper.getDataMimeType(id);
            case 3002:
                return "vnd.android.cursor.dir/phone_v2";
            case 3003:
                return "vnd.android.cursor.item/phone_v2";
            case 3005:
                return "vnd.android.cursor.dir/email_v2";
            case 3006:
                return "vnd.android.cursor.item/email_v2";
            case 3009:
                return "vnd.android.cursor.dir/postal-address_v2";
            case 3010:
                return "vnd.android.cursor.item/postal-address_v2";
            case 4000:
            case 4001:
                return "vnd.android.cursor.dir/phone_lookup";
            case 6000:
                return "vnd.android.cursor.dir/aggregation_exception";
            case 6001:
                return "vnd.android.cursor.item/aggregation_exception";
            case 8000:
                return "vnd.android.cursor.dir/contact";
            case 9000:
                return "vnd.android.cursor.dir/setting";
            case 12001:
                return "vnd.android.cursor.dir/vnd.android.search.suggest";
            case 12002:
                return "vnd.android.cursor.item/vnd.android.search.suggest";
            case 17001:
                return "vnd.android.cursor.dir/contact_directories";
            case 17002:
                return "vnd.android.cursor.item/contact_directory";
            case 21000:
                return "vnd.android.cursor.dir/stream_item";
            case 21001:
                throw new UnsupportedOperationException("Not supported for write-only URI " + uri);
            case 21002:
                return "vnd.android.cursor.item/stream_item";
            case 21003:
                return "vnd.android.cursor.dir/stream_item_photo";
            case 21004:
                return "vnd.android.cursor.item/stream_item_photo";
            default:
                waitForAccess(this.mReadAccessLatch);
                return this.mLegacyApiSupport.getType(uri);
        }
    }

    private String[] getDefaultProjection(Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case 1000:
            case 1001:
            case 1002:
            case 1003:
            case 8000:
            case 19000:
                return sContactsProjectionMap.getColumnNames();
            case 1015:
            case 1016:
            case 19004:
                return sContactsVCardProjectionMap.getColumnNames();
            case 1019:
            case 19001:
                return sEntityProjectionMap.getColumnNames();
            case 2002:
            case 2003:
            case 19005:
            case 19006:
                return sRawContactsProjectionMap.getColumnNames();
            case 3001:
            case 3002:
            case 3003:
            case 3005:
            case 3006:
            case 3009:
            case 3010:
            case 19002:
                return sDataProjectionMap.getColumnNames();
            case 4000:
            case 4001:
                return sPhoneLookupProjectionMap.getColumnNames();
            case 6000:
            case 6001:
                return sAggregationExceptionsProjectionMap.getColumnNames();
            case 9000:
                return sSettingsProjectionMap.getColumnNames();
            case 17001:
            case 17002:
                return sDirectoryProjectionMap.getColumnNames();
            default:
                return null;
        }
    }

    private class StructuredNameLookupBuilder extends NameLookupBuilder {
        public StructuredNameLookupBuilder(NameSplitter splitter) {
            super(splitter);
        }

        @Override
        protected void insertNameLookup(long rawContactId, long dataId, int lookupType, String name) {
            ((ContactsDatabaseHelper) ContactsProvider2.this.mDbHelper.get()).insertNameLookup(rawContactId, dataId, lookupType, name);
        }

        @Override
        protected String[] getCommonNicknameClusters(String normalizedName) {
            return ContactsProvider2.this.mCommonNicknameCache.getCommonNicknameClusters(normalizedName);
        }
    }

    public void appendContactFilterAsNestedQuery(StringBuilder sb, String filterParam) {
        sb.append("(SELECT DISTINCT contact_id FROM raw_contacts JOIN name_lookup ON(raw_contacts._id=raw_contact_id) WHERE normalized_name GLOB '");
        sb.append(NameNormalizer.normalize(filterParam));
        sb.append("*' AND name_type IN(2,4,3))");
    }

    private boolean isPhoneNumber(String query) {
        return !TextUtils.isEmpty(query) && countPhoneNumberDigits(query) > 0;
    }

    public static int countPhoneNumberDigits(String query) {
        int numDigits = 0;
        int len = query.length();
        for (int i = 0; i < len; i++) {
            char c = query.charAt(i);
            if (Character.isDigit(c)) {
                numDigits++;
            } else if (c != '*' && c != '#' && c != 'N' && c != '.' && c != ';' && c != '-' && c != '(' && c != ')' && c != ' ' && (c != '+' || numDigits != 0)) {
                return 0;
            }
        }
        return numDigits;
    }

    private Cursor completeName(Uri uri, String[] projection) {
        if (projection == null) {
            projection = sDataProjectionMap.getColumnNames();
        }
        ContentValues values = new ContentValues();
        DataRowHandlerForStructuredName handler = (DataRowHandlerForStructuredName) getDataRowHandler("vnd.android.cursor.item/name");
        copyQueryParamsToContentValues(values, uri, "data1", "data4", "data2", "data5", "data3", "data6", "phonetic_name", "data9", "data8", "data7");
        handler.fixStructuredNameComponents(values, values);
        MatrixCursor cursor = new MatrixCursor(projection);
        Object[] row = new Object[projection.length];
        for (int i = 0; i < projection.length; i++) {
            row[i] = values.get(projection[i]);
        }
        cursor.addRow(row);
        return cursor;
    }

    private void copyQueryParamsToContentValues(ContentValues values, Uri uri, String... columns) {
        for (String column : columns) {
            String param = uri.getQueryParameter(column);
            if (param != null) {
                values.put(column, param);
            }
        }
    }

    private String[] insertSelectionArg(String[] selectionArgs, String arg) {
        if (selectionArgs == null) {
            return new String[]{arg};
        }
        int newLength = selectionArgs.length + 1;
        String[] newSelectionArgs = new String[newLength];
        newSelectionArgs[0] = arg;
        System.arraycopy(selectionArgs, 0, newSelectionArgs, 1, selectionArgs.length);
        return newSelectionArgs;
    }

    private String[] appendSelectionArg(String[] selectionArgs, String arg) {
        if (selectionArgs == null) {
            return new String[]{arg};
        }
        int newLength = selectionArgs.length + 1;
        String[] newSelectionArgs = new String[newLength];
        newSelectionArgs[newLength] = arg;
        System.arraycopy(selectionArgs, 0, newSelectionArgs, 0, selectionArgs.length - 1);
        return newSelectionArgs;
    }

    protected Account getDefaultAccount() {
        AccountManager accountManager = AccountManager.get(getContext());
        try {
            Account[] accounts = accountManager.getAccountsByType("com.google");
            if (accounts != null && accounts.length > 0) {
                return accounts[0];
            }
        } catch (Throwable e) {
            Log.e("ContactsProvider", "Cannot determine the default account for contacts compatibility", e);
        }
        return null;
    }

    public boolean isWritableAccountWithDataSet(String accountTypeAndDataSet) {
        if (accountTypeAndDataSet == null) {
            return true;
        }
        Boolean writable = this.mAccountWritability.get(accountTypeAndDataSet);
        if (writable != null) {
            return writable.booleanValue();
        }
        IContentService contentService = ContentResolver.getContentService();
        try {
            SyncAdapterType[] arr$ = contentService.getSyncAdapterTypes();
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                SyncAdapterType sync = arr$[i$];
                if ("com.android.contacts".equals(sync.authority) && accountTypeAndDataSet.equals(sync.accountType)) {
                    break;
                }
                i$++;
            }
        } catch (RemoteException e) {
            Log.e("ContactsProvider", "Could not acquire sync adapter types");
        }
        if (writable == null) {
            writable = false;
        }
        this.mAccountWritability.put(accountTypeAndDataSet, writable);
        return writable.booleanValue();
    }

    static boolean readBooleanQueryParameter(Uri uri, String parameter, boolean defaultValue) {
        int index;
        String query = uri.getEncodedQuery();
        if (query != null && (index = query.indexOf(parameter)) != -1) {
            int index2 = index + parameter.length();
            return (matchQueryParameter(query, index2, "=0", false) || matchQueryParameter(query, index2, "=false", true)) ? false : true;
        }
        return defaultValue;
    }

    private static boolean matchQueryParameter(String query, int index, String value, boolean ignoreCase) {
        int length = value.length();
        if (query.regionMatches(ignoreCase, index, value, 0, length)) {
            return query.length() == index + length || query.charAt(index + length) == '&';
        }
        return false;
    }

    static String getQueryParameter(Uri uri, String parameter) {
        String value;
        char prevChar;
        String query = uri.getEncodedQuery();
        if (query == null) {
            return null;
        }
        int queryLength = query.length();
        int parameterLength = parameter.length();
        int index = 0;
        while (true) {
            int index2 = query.indexOf(parameter, index);
            if (index2 == -1) {
                return null;
            }
            if (index2 > 0 && (prevChar = query.charAt(index2 - 1)) != '?' && prevChar != '&') {
                index = index2 + parameterLength;
            } else {
                index = index2 + parameterLength;
                if (queryLength == index) {
                    return null;
                }
                if (query.charAt(index) == '=') {
                    int index3 = index + 1;
                    int ampIndex = query.indexOf(38, index3);
                    if (ampIndex == -1) {
                        value = query.substring(index3);
                    } else {
                        value = query.substring(index3, ampIndex);
                    }
                    return Uri.decode(value);
                }
            }
        }
    }

    private boolean isAggregationUpgradeNeeded() {
        if (!this.mContactAggregator.isEnabled()) {
            return false;
        }
        int version = Integer.parseInt(this.mContactsHelper.getProperty("aggregation_v2", "1"));
        return version < 4;
    }

    private void upgradeAggregationAlgorithmInBackground() {
        SQLiteDatabase db;
        Log.i("ContactsProvider", "Upgrading aggregation algorithm");
        long start = SystemClock.elapsedRealtime();
        setProviderStatus(1);
        int count = 0;
        SQLiteDatabase db2 = null;
        boolean transactionStarted = false;
        try {
            try {
                try {
                    switchToContactMode();
                    db = this.mContactsHelper.getWritableDatabase();
                    db.beginTransaction();
                    transactionStarted = true;
                    count = this.mContactAggregator.markAllVisibleForAggregation(db);
                    this.mContactAggregator.aggregateInTransaction(this.mTransactionContext.get(), db);
                    updateSearchIndexInTransaction();
                    updateAggregationAlgorithmVersion();
                    db.setTransactionSuccessful();
                    this.mTransactionContext.get().clearAll();
                    if (1 != 0) {
                    }
                    long end = SystemClock.elapsedRealtime();
                    Log.i("ContactsProvider", "Aggregation algorithm upgraded for " + count + " raw contacts" + (1 != 0 ? " in " + (end - start) + "ms" : " failed"));
                    setProviderStatus(0);
                } catch (RuntimeException e) {
                    Log.e("ContactsProvider", "Failed to upgrade aggregation algorithm; continuing anyway.", e);
                    try {
                        db = this.mContactsHelper.getWritableDatabase();
                        db.beginTransaction();
                    } catch (RuntimeException e2) {
                        Log.e("ContactsProvider", "Failed to bump aggregation algorithm version; continuing anyway.", e2);
                    }
                    try {
                        updateAggregationAlgorithmVersion();
                        db.setTransactionSuccessful();
                        setProviderStatus(0);
                    } finally {
                        db.endTransaction();
                    }
                }
            } catch (Throwable th) {
                this.mTransactionContext.get().clearAll();
                if (transactionStarted) {
                }
                long end2 = SystemClock.elapsedRealtime();
                Log.i("ContactsProvider", "Aggregation algorithm upgraded for " + count + " raw contacts" + (0 != 0 ? " in " + (end2 - start) + "ms" : " failed"));
                throw th;
            }
        } catch (Throwable th2) {
            setProviderStatus(0);
            throw th2;
        }
    }

    private void updateAggregationAlgorithmVersion() {
        this.mContactsHelper.setProperty("aggregation_v2", String.valueOf(4));
    }

    protected boolean isPhone() {
        if (!this.mIsPhoneInitialized) {
            this.mIsPhone = new TelephonyManager(getContext()).isVoiceCapable();
            this.mIsPhoneInitialized = true;
        }
        return this.mIsPhone;
    }

    private void undemoteContact(SQLiteDatabase db, long id) {
        String[] arg = {String.valueOf(id)};
        db.execSQL("UPDATE contacts SET pinned = 0 WHERE _id = ?1 AND pinned <= -1", arg);
        db.execSQL("UPDATE raw_contacts SET pinned = 0 WHERE contact_id = ?1 AND pinned <= -1", arg);
    }

    private boolean handleDataUsageFeedback(Uri uri) {
        boolean successful;
        long currentTimeMillis = Clock.getInstance().currentTimeMillis();
        String usageType = uri.getQueryParameter("type");
        String[] ids = uri.getLastPathSegment().trim().split(",");
        ArrayList<Long> dataIds = new ArrayList<>(ids.length);
        for (String id : ids) {
            dataIds.add(Long.valueOf(id));
        }
        if (TextUtils.isEmpty(usageType)) {
            Log.w("ContactsProvider", "Method for data usage feedback isn't specified. Ignoring.");
            successful = false;
        } else {
            successful = updateDataUsageStat(dataIds, usageType, currentTimeMillis) > 0;
        }
        StringBuilder rawContactIdSelect = new StringBuilder();
        rawContactIdSelect.append("SELECT raw_contact_id FROM data WHERE _id IN (");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                rawContactIdSelect.append(",");
            }
            rawContactIdSelect.append(ids[i]);
        }
        rawContactIdSelect.append(")");
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        this.mSelectionArgs1[0] = String.valueOf(currentTimeMillis);
        db.execSQL("UPDATE raw_contacts SET last_time_contacted=?,times_contacted=ifnull(times_contacted,0) + 1 WHERE _id IN (" + rawContactIdSelect.toString() + ")", this.mSelectionArgs1);
        db.execSQL("UPDATE contacts SET last_time_contacted=?1,times_contacted=ifnull(times_contacted,0) + 1,contact_last_updated_timestamp=?1 WHERE _id IN (SELECT contact_id FROM raw_contacts WHERE _id IN (" + rawContactIdSelect.toString() + "))", this.mSelectionArgs1);
        return successful;
    }

    int updateDataUsageStat(List<Long> dataIds, String type, long currentTimeMillis) {
        SQLiteDatabase db = this.mDbHelper.get().getWritableDatabase();
        String typeString = String.valueOf(getDataUsageFeedbackType(type, null));
        String currentTimeMillisString = String.valueOf(currentTimeMillis);
        Iterator<Long> it = dataIds.iterator();
        while (it.hasNext()) {
            long dataId = it.next().longValue();
            String dataIdString = String.valueOf(dataId);
            this.mSelectionArgs2[0] = dataIdString;
            this.mSelectionArgs2[1] = typeString;
            Cursor cursor = db.query("data_usage_stat", DataUsageStatQuery.COLUMNS, "data_id =? AND usage_type =?", this.mSelectionArgs2, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    long id = cursor.getLong(0);
                    this.mSelectionArgs2[0] = currentTimeMillisString;
                    this.mSelectionArgs2[1] = String.valueOf(id);
                    db.execSQL("UPDATE data_usage_stat SET times_used=ifnull(times_used,0)+1,last_time_used=? WHERE stat_id=?", this.mSelectionArgs2);
                } else {
                    this.mSelectionArgs4[0] = dataIdString;
                    this.mSelectionArgs4[1] = typeString;
                    this.mSelectionArgs4[2] = "1";
                    this.mSelectionArgs4[3] = currentTimeMillisString;
                    db.execSQL("INSERT INTO data_usage_stat(data_id,usage_type,times_used,last_time_used) VALUES (?,?,?,?)", this.mSelectionArgs4);
                }
            } finally {
                cursor.close();
            }
        }
        return dataIds.size();
    }

    private String getAccountPromotionSortOrder(Uri uri) {
        String primaryAccountName = uri.getQueryParameter("name_for_primary_account");
        String primaryAccountType = uri.getQueryParameter("type_for_primary_account");
        if (TextUtils.isEmpty(primaryAccountName)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(CASE WHEN account_name=");
        DatabaseUtils.appendEscapedSQLString(sb, primaryAccountName);
        if (!TextUtils.isEmpty(primaryAccountType)) {
            sb.append(" AND account_type=");
            DatabaseUtils.appendEscapedSQLString(sb, primaryAccountType);
        }
        sb.append(" THEN 0 ELSE 1 END)");
        return sb.toString();
    }

    private boolean deferredSnippetingRequested(Uri uri) {
        String deferredSnippeting = getQueryParameter(uri, "deferred_snippeting");
        return !TextUtils.isEmpty(deferredSnippeting) && deferredSnippeting.equals("1");
    }

    private boolean isSingleWordQuery(String query) {
        String[] tokens = query.split("[^\\w@]+", 0);
        int count = 0;
        for (String token : tokens) {
            if (!"".equals(token)) {
                count++;
            }
        }
        return count == 1;
    }

    private boolean snippetNeeded(String[] projection) {
        return ContactsDatabaseHelper.isInProjection(projection, "snippet");
    }

    private void replacePackageNameByPackageId(ContentValues values) {
        if (values != null) {
            String packageName = values.getAsString("res_package");
            if (packageName != null) {
                values.put("package_id", Long.valueOf(this.mDbHelper.get().getPackageId(packageName)));
            }
            values.remove("res_package");
        }
    }

    private long replaceAccountInfoByAccountId(Uri uri, ContentValues values) {
        AccountWithDataSet account = resolveAccountWithDataSet(uri, values);
        long id = this.mDbHelper.get().getOrCreateAccountIdInTransaction(account);
        values.put("account_id", Long.valueOf(id));
        values.remove("account_name");
        values.remove("account_type");
        values.remove("data_set");
        return id;
    }

    static Cursor buildSingleRowResult(String[] projection, String[] availableColumns, Object[] data) {
        Preconditions.checkArgument(availableColumns.length == data.length);
        if (projection == null) {
            projection = availableColumns;
        }
        MatrixCursor c = new MatrixCursor(projection, 1);
        MatrixCursor.RowBuilder row = c.newRow();
        for (int i = 0; i < c.getColumnCount(); i++) {
            String columnName = c.getColumnName(i);
            boolean found = false;
            int j = 0;
            while (true) {
                if (j >= availableColumns.length) {
                    break;
                }
                if (!availableColumns[j].equals(columnName)) {
                    j++;
                } else {
                    row.add(data[j]);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Invalid column " + projection[i]);
            }
        }
        return c;
    }

    protected ContactsDatabaseHelper getThreadActiveDatabaseHelperForTest() {
        return this.mDbHelper.get();
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("FastScrollingIndex stats:\n");
        pw.printf("request=%d  miss=%d (%d%%)  avg time=%dms\n", Integer.valueOf(this.mFastScrollingIndexCacheRequestCount), Integer.valueOf(this.mFastScrollingIndexCacheMissCount), Long.valueOf(safeDiv(this.mFastScrollingIndexCacheMissCount * 100, this.mFastScrollingIndexCacheRequestCount)), Long.valueOf(safeDiv(this.mTotalTimeFastScrollingIndexGenerate, this.mFastScrollingIndexCacheMissCount)));
    }

    private static final long safeDiv(long dividend, long divisor) {
        if (divisor == 0) {
            return 0L;
        }
        return dividend / divisor;
    }

    private static final int getDataUsageFeedbackType(String type, Integer defaultType) {
        if ("call".equals(type)) {
            return 0;
        }
        if ("long_text".equals(type)) {
            return 1;
        }
        if ("short_text".equals(type)) {
            return 2;
        }
        if (defaultType != null) {
            return defaultType.intValue();
        }
        throw new IllegalArgumentException("Invalid usage type " + type);
    }

    public String toString() {
        return "ContactsProvider2";
    }

    public void switchToProfileModeForTest() {
        switchToProfileMode();
    }
}
