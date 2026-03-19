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
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.AbstractCursor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.common.content.ProjectionMap;
import com.android.common.io.MoreCloseables;
import com.android.internal.util.ArrayUtils;
import com.android.providers.contacts.ContactLookupKey;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.DataRowHandler;
import com.android.providers.contacts.MetadataEntryParser;
import com.android.providers.contacts.PhotoStore;
import com.android.providers.contacts.SearchIndexManager;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;
import com.android.providers.contacts.aggregation.ContactAggregator;
import com.android.providers.contacts.aggregation.ContactAggregator2;
import com.android.providers.contacts.aggregation.ProfileAggregator;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;
import com.android.providers.contacts.database.ContactsTableUtil;
import com.android.providers.contacts.database.DeletedContactsTableUtil;
import com.android.providers.contacts.database.MoreDatabaseUtils;
import com.android.providers.contacts.enterprise.EnterpriseContactsCursorWrapper;
import com.android.providers.contacts.enterprise.EnterprisePolicyGuard;
import com.android.providers.contacts.util.Clock;
import com.android.providers.contacts.util.ContactsPermissions;
import com.android.providers.contacts.util.DbQueryUtils;
import com.android.providers.contacts.util.UserUtils;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import com.google.android.collect.Sets;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.mediatek.providers.contacts.AccountUtils;
import com.mediatek.providers.contacts.ContactsProviderUtils;
import com.mediatek.providers.contacts.DataRowHandlerForImsCall;
import com.mediatek.providers.contacts.DataRowHandlerForWebsite;
import com.mediatek.providers.contacts.SimCardUtils;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static final Set<String> MODIFIED_KEY_SET_FOR_ENTERPRISE_FILTER;
    private static final String[] PROJECTION_GROUP_ID;
    private static int PROPERTY_AGGREGATION_ALGORITHM_VERSION;
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
    private CommonNicknameCache mCommonNicknameCache;
    private AbstractContactAggregator mContactAggregator;
    private ContactDirectoryManager mContactDirectoryManager;
    private int mContactsAccountCount;
    private ContactsDatabaseHelper mContactsHelper;
    private PhotoStore mContactsPhotoStore;
    private LocaleSet mCurrentLocales;
    private ArrayMap<String, DataRowHandler> mDataRowHandlers;
    private EnterprisePolicyGuard mEnterprisePolicyGuard;
    private FastScrollingIndexCache mFastScrollingIndexCache;
    private int mFastScrollingIndexCacheMissCount;
    private int mFastScrollingIndexCacheRequestCount;
    private GlobalSearchSupport mGlobalSearchSupport;
    private boolean mIsPhone;
    private boolean mIsPhoneInitialized;
    private LegacyApiSupport mLegacyApiSupport;
    private boolean mMetadataSyncEnabled;
    private NameLookupBuilder mNameLookupBuilder;
    private NameSplitter mNameSplitter;
    private PostalSplitter mPostalSplitter;
    private long mPreAuthorizedUriDuration;
    private AbstractContactAggregator mProfileAggregator;
    private ArrayMap<String, DataRowHandler> mProfileDataRowHandlers;
    private ProfileDatabaseHelper mProfileHelper;
    private PhotoStore mProfilePhotoStore;
    private ProfileProvider mProfileProvider;
    private boolean mProviderStatusUpdateNeeded;
    private volatile CountDownLatch mReadAccessLatch;
    private SearchIndexManager mSearchIndexManager;
    private boolean mSyncToMetadataNetWork;
    private boolean mSyncToNetwork;
    private ContactsTaskScheduler mTaskScheduler;
    private long mTotalTimeFastScrollingIndexGenerate;
    private volatile CountDownLatch mWriteAccessLatch;
    public static final ProfileAwareUriMatcher sUriMatcher = new ProfileAwareUriMatcher(-1);
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
    private final ThreadLocal<AbstractContactAggregator> mAggregator = new ThreadLocal<>();
    private final ThreadLocal<PhotoStore> mPhotoStore = new ThreadLocal<>();
    private final TransactionContext mContactTransactionContext = new TransactionContext(false);
    private final TransactionContext mProfileTransactionContext = new TransactionContext(true);
    private final ThreadLocal<TransactionContext> mTransactionContext = new ThreadLocal<>();
    private final SecureRandom mRandom = new SecureRandom();
    private final ArrayMap<String, Boolean> mAccountWritability = new ArrayMap<>();
    private ArrayMap<String, DirectoryInfo> mDirectoryCache = new ArrayMap<>();
    private boolean mDirectoryCacheValid = false;
    private ArrayMap<String, ArrayList<GroupIdCacheEntry>> mGroupIdCache = new ArrayMap<>();
    private int mProviderStatus = 0;
    private boolean mOkToOpenAccess = true;
    private boolean mVisibleTouched = false;
    private long mLastPhotoCleanup = 0;

    private static final class AddressBookIndexQuery {
        public static final String[] COLUMNS = {"name", "bucket", "label", "count"};
    }

    interface AggregationExceptionQuery {
        public static final String[] COLUMNS = {"raw_contact_id1", "raw_contact_id2"};
    }

    private interface DataContactsQuery {
        public static final String[] PROJECTION = {"raw_contacts._id", "accounts.account_type", "accounts.account_name", "accounts.data_set", "data._id", "contacts._id"};
    }

    interface DataHashQuery {
        public static final String[] COLUMNS = {"_id"};
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

    interface MetadataSyncQuery {
        public static final String[] COLUMNS = {"metadata_sync._id", "data"};
    }

    interface RawContactsBackupQuery {
        public static final String[] COLUMNS = {"_id"};
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
        DISTINCT_DATA_PROHIBITING_COLUMNS = new String[]{"_id", "raw_contact_id", "name_raw_contact_id", "account_name", "account_type", "data_set", "account_type_and_data_set", "dirty", "sourceid", "version"};
        sContactsColumns = ProjectionMap.builder().add("custom_ringtone").add("display_name").add("display_name_alt").add("display_name_source").add("in_default_directory").add("in_visible_group").add("last_time_contacted").add("lookup").add("phonetic_name").add("phonetic_name_style").add("photo_id").add("photo_file_id").add("photo_uri").add("photo_thumb_uri").add("send_to_voicemail").add("sort_key_alt").add("sort_key").add("phonebook_label").add("phonebook_bucket").add("phonebook_label_alt").add("phonebook_bucket_alt").add("starred").add("pinned").add("times_contacted").add("has_phone_number").add("contact_last_updated_timestamp").add("indicate_phone_or_sim_contact").add("index_in_sim").add("send_to_voicemail_vt").add("send_to_voicemail_sip").add("is_sdn_contact").build();
        sContactsPresenceColumns = ProjectionMap.builder().add("contact_presence", "agg_presence.mode").add("contact_chat_capability", "agg_presence.chat_capability").add("contact_status", "contacts_status_updates.status").add("contact_status_ts", "contacts_status_updates.status_ts").add("contact_status_res_package", "contacts_status_updates.status_res_package").add("contact_status_label", "contacts_status_updates.status_label").add("contact_status_icon", "contacts_status_updates.status_icon").build();
        sSnippetColumns = ProjectionMap.builder().add("snippet").build();
        sRawContactColumns = ProjectionMap.builder().add("account_name").add("account_type").add("data_set").add("account_type_and_data_set").add("dirty").add("sourceid").add("backup_id").add("version").add("indicate_phone_or_sim_contact").add("index_in_sim").add("timestamp").add("is_sdn_contact").build();
        sRawContactSyncColumns = ProjectionMap.builder().add("sync1").add("sync2").add("sync3").add("sync4").build();
        sDataColumns = ProjectionMap.builder().add("data1").add("data2").add("data3").add("data4").add("data5").add("data6").add("data7").add("data8").add("data9").add("data10").add("data11").add("data12").add("data13").add("data14").add("data15").add("carrier_presence").add("preferred_phone_account_component_name").add("preferred_phone_account_id").add("data_version").add("is_primary").add("is_super_primary").add("mimetype").add("res_package").add("data_sync1").add("data_sync2").add("data_sync3").add("data_sync4").add("group_sourceid").add("is_additional_number").build();
        sContactPresenceColumns = ProjectionMap.builder().add("contact_presence", "agg_presence.mode").add("contact_chat_capability", "agg_presence.chat_capability").add("contact_status", "contacts_status_updates.status").add("contact_status_ts", "contacts_status_updates.status_ts").add("contact_status_res_package", "contacts_status_updates.status_res_package").add("contact_status_label", "contacts_status_updates.status_label").add("contact_status_icon", "contacts_status_updates.status_icon").build();
        sDataPresenceColumns = ProjectionMap.builder().add("mode", "presence.mode").add("chat_capability", "presence.chat_capability").add("status", "status_updates.status").add("status_ts", "status_updates.status_ts").add("status_res_package", "status_updates.status_res_package").add("status_label", "status_updates.status_label").add("status_icon", "status_updates.status_icon").build();
        sDataUsageColumns = ProjectionMap.builder().add("times_used", "data_usage_stat.times_used").add("last_time_used", "data_usage_stat.last_time_used").build();
        sCountProjectionMap = ProjectionMap.builder().add("_count", "COUNT(*)").build();
        sContactsProjectionMap = ProjectionMap.builder().add("_id").add("has_phone_number").add("name_raw_contact_id").add("is_user_profile").addAll(sContactsColumns).addAll(sContactsPresenceColumns).build();
        sContactsProjectionWithSnippetMap = ProjectionMap.builder().addAll(sContactsProjectionMap).addAll(sSnippetColumns).build();
        sStrequentStarredProjectionMap = ProjectionMap.builder().addAll(sContactsProjectionMap).add("times_used", String.valueOf(Long.MAX_VALUE)).add("last_time_used", String.valueOf(Long.MAX_VALUE)).build();
        sStrequentFrequentProjectionMap = ProjectionMap.builder().addAll(sContactsProjectionMap).add("times_used", "SUM(data_usage_stat.times_used)").add("last_time_used", "MAX(data_usage_stat.last_time_used)").build();
        sStrequentPhoneOnlyProjectionMap = ProjectionMap.builder().addAll(sContactsProjectionMap).add("times_used").add("last_time_used").add("data1").add("data2").add("data3").add("is_super_primary").add("contact_id").add("is_user_profile", "NULL").build();
        sContactsVCardProjectionMap = ProjectionMap.builder().add("_id").add("_display_name", "display_name || '.vcf'").add("_size", "NULL").build();
        sRawContactsProjectionMap = ProjectionMap.builder().add("_id").add("contact_id").add("deleted").add("display_name").add("display_name_alt").add("display_name_source").add("phonetic_name").add("phonetic_name_style").add("sort_key").add("sort_key_alt").add("phonebook_label").add("phonebook_bucket").add("phonebook_label_alt").add("phonebook_bucket_alt").add("times_contacted").add("last_time_contacted").add("custom_ringtone").add("send_to_voicemail").add("starred").add("pinned").add("aggregation_mode").add("raw_contact_is_user_profile").add("metadata_dirty").add("timestamp").add("send_to_voicemail_vt").add("send_to_voicemail_sip").addAll(sRawContactColumns).addAll(sRawContactSyncColumns).build();
        sRawEntityProjectionMap = ProjectionMap.builder().add("_id").add("contact_id").add("data_id").add("deleted").add("starred").add("raw_contact_is_user_profile").addAll(sRawContactColumns).addAll(sRawContactSyncColumns).addAll(sDataColumns).build();
        sEntityProjectionMap = ProjectionMap.builder().add("_id").add("contact_id").add("raw_contact_id").add("data_id").add("name_raw_contact_id").add("deleted").add("is_user_profile").addAll(sContactsColumns).addAll(sContactPresenceColumns).addAll(sRawContactColumns).addAll(sRawContactSyncColumns).addAll(sDataColumns).addAll(sDataPresenceColumns).addAll(sDataUsageColumns).build();
        sSipLookupColumns = ProjectionMap.builder().add("data_id", "_id").add("number", "data1").add("type", "0").add("label", "NULL").add("normalized_number", "NULL").build();
        sDataProjectionMap = ProjectionMap.builder().add("_id").add("raw_contact_id").add("hash_id").add("contact_id").add("name_raw_contact_id").add("raw_contact_is_user_profile").addAll(sDataColumns).addAll(sDataPresenceColumns).addAll(sRawContactColumns).addAll(sContactsColumns).addAll(sContactPresenceColumns).addAll(sDataUsageColumns).build();
        sDataSipLookupProjectionMap = ProjectionMap.builder().addAll(sDataProjectionMap).addAll(sSipLookupColumns).build();
        sDistinctDataProjectionMap = ProjectionMap.builder().add("_id", "MIN(_id)").add("contact_id").add("raw_contact_is_user_profile").add("hash_id").addAll(sDataColumns).addAll(sDataPresenceColumns).addAll(sContactsColumns).addAll(sContactPresenceColumns).addAll(sDataUsageColumns).build();
        sDistinctDataSipLookupProjectionMap = ProjectionMap.builder().addAll(sDistinctDataProjectionMap).addAll(sSipLookupColumns).build();
        sPhoneLookupProjectionMap = ProjectionMap.builder().add("_id", "contacts_view._id").add("contact_id", "contacts_view._id").add("data_id", "data_id").add("lookup", "contacts_view.lookup").add("display_name_source", "contacts_view.display_name_source").add("display_name", "contacts_view.display_name").add("display_name_alt", "contacts_view.display_name_alt").add("phonetic_name", "contacts_view.phonetic_name").add("phonetic_name_style", "contacts_view.phonetic_name_style").add("sort_key", "contacts_view.sort_key").add("sort_key_alt", "contacts_view.sort_key_alt").add("last_time_contacted", "contacts_view.last_time_contacted").add("times_contacted", "contacts_view.times_contacted").add("starred", "contacts_view.starred").add("in_default_directory", "contacts_view.in_default_directory").add("in_visible_group", "contacts_view.in_visible_group").add("photo_id", "contacts_view.photo_id").add("photo_file_id", "contacts_view.photo_file_id").add("photo_uri", "contacts_view.photo_uri").add("photo_thumb_uri", "contacts_view.photo_thumb_uri").add("custom_ringtone", "contacts_view.custom_ringtone").add("has_phone_number", "contacts_view.has_phone_number").add("send_to_voicemail", "contacts_view.send_to_voicemail").add("number", "data1").add("type", "data2").add("label", "data3").add("normalized_number", "data4").add("send_to_voicemail_vt", "contacts_view.send_to_voicemail_vt").add("send_to_voicemail_sip", "contacts_view.send_to_voicemail_sip").add("indicate_phone_or_sim_contact", "contacts_view.indicate_phone_or_sim_contact").add("index_in_sim", "contacts_view.index_in_sim").add("filter", "contacts_view.filter").add("data_id", "data_id").add("raw_contact_id", "raw_contacts._id").add("is_sdn_contact", "raw_contacts.is_sdn_contact").add("_id", "contacts_view._id").add("data1", "data1").add("data3", "data3").add("display_name", "contacts_view.display_name").add("contact_id", "contacts_view._id").add("contact_presence", "mode").add("contact_status", "status").add("data4", "data4").add("send_to_voicemail", "contacts_view.send_to_voicemail").build();
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
        ProfileAwareUriMatcher profileAwareUriMatcher = sUriMatcher;
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts", 1000);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/#", 1001);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/#/data", 1004);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/#/entities", 1019);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/#/suggestions", 8000);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/#/suggestions/*", 8000);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/#/photo", 1009);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/#/display_photo", 1012);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts_corp/#/photo", 1027);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts_corp/#/display_photo", 1028);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/#/stream_items", 1022);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/filter", 1005);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/filter/*", 1005);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*", 1002);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/data", 1017);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/photo", 1010);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/#", 1003);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/#/data", 1018);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/#/photo", 1011);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/display_photo", 1013);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/#/display_photo", 1014);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/entities", 1020);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/#/entities", 1021);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/stream_items", 1023);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/lookup/*/#/stream_items", 1024);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/as_vcard/*", 1015);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/as_multi_vcard/*", 1016);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/strequent/", 1006);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/strequent/filter/*", 1007);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/group/*", 1008);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/frequent", 1025);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/delete_usage", 1026);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/filter_enterprise", 1029);
        profileAwareUriMatcher.addURI("com.android.contacts", "contacts/filter_enterprise/*", 1029);
        profileAwareUriMatcher.addURI("com.android.contacts", "raw_contacts", 2002);
        profileAwareUriMatcher.addURI("com.android.contacts", "raw_contacts/#", 2003);
        profileAwareUriMatcher.addURI("com.android.contacts", "raw_contacts/#/data", 2004);
        profileAwareUriMatcher.addURI("com.android.contacts", "raw_contacts/#/display_photo", 2006);
        profileAwareUriMatcher.addURI("com.android.contacts", "raw_contacts/#/entity", 2005);
        profileAwareUriMatcher.addURI("com.android.contacts", "raw_contacts/#/stream_items", 2007);
        profileAwareUriMatcher.addURI("com.android.contacts", "raw_contacts/#/stream_items/#", 2008);
        profileAwareUriMatcher.addURI("com.android.contacts", "raw_contact_entities", 15001);
        profileAwareUriMatcher.addURI("com.android.contacts", "raw_contact_entities_corp", 15002);
        profileAwareUriMatcher.addURI("com.android.contacts", "data", 3000);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/#", 3001);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/phones", 3002);
        profileAwareUriMatcher.addURI("com.android.contacts", "data_enterprise/phones", 3016);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/phones/#", 3003);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/phones/filter", 3004);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/phones/filter/*", 3004);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/phones/filter_enterprise", 3018);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/phones/filter_enterprise/*", 3018);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/emails", 3005);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/emails/#", 3006);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/emails/lookup", 3007);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/emails/lookup/*", 3007);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/emails/filter", 3008);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/emails/filter/*", 3008);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/emails/filter_enterprise", 3020);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/emails/filter_enterprise/*", 3020);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/emails/lookup_enterprise", 3017);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/emails/lookup_enterprise/*", 3017);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/postals", 3009);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/postals/#", 3010);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/usagefeedback/*", 20001);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/callables/", 3011);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/callables/#", 3012);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/callables/filter", 3013);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/callables/filter/*", 3013);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/callables/filter_enterprise", 3019);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/callables/filter_enterprise/*", 3019);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/contactables/", 3014);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/contactables/filter", 3015);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/contactables/filter/*", 3015);
        profileAwareUriMatcher.addURI("com.android.contacts", "groups", 10000);
        profileAwareUriMatcher.addURI("com.android.contacts", "groups/#", 10001);
        profileAwareUriMatcher.addURI("com.android.contacts", "groups_summary", 10003);
        profileAwareUriMatcher.addURI("com.android.contacts", "syncstate", 11000);
        profileAwareUriMatcher.addURI("com.android.contacts", "syncstate/#", 11001);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/syncstate", 11002);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/syncstate/#", 11003);
        profileAwareUriMatcher.addURI("com.android.contacts", "phone_lookup", 4000);
        profileAwareUriMatcher.addURI("com.android.contacts", "phone_lookup/*", 4000);
        profileAwareUriMatcher.addURI("com.android.contacts", "phone_lookup_enterprise/*", 4001);
        profileAwareUriMatcher.addURI("com.android.contacts", "aggregation_exceptions", 6000);
        profileAwareUriMatcher.addURI("com.android.contacts", "aggregation_exceptions/*", 6001);
        profileAwareUriMatcher.addURI("com.android.contacts", "settings", 9000);
        profileAwareUriMatcher.addURI("com.android.contacts", "status_updates", 7000);
        profileAwareUriMatcher.addURI("com.android.contacts", "status_updates/#", 7001);
        profileAwareUriMatcher.addURI("com.android.contacts", "search_suggest_query", 12001);
        profileAwareUriMatcher.addURI("com.android.contacts", "search_suggest_query/*", 12001);
        profileAwareUriMatcher.addURI("com.android.contacts", "search_suggest_shortcut/*", 12002);
        profileAwareUriMatcher.addURI("com.android.contacts", "provider_status", 16001);
        profileAwareUriMatcher.addURI("com.android.contacts", "directories", 17001);
        profileAwareUriMatcher.addURI("com.android.contacts", "directories/#", 17002);
        profileAwareUriMatcher.addURI("com.android.contacts", "directories_enterprise", 17003);
        profileAwareUriMatcher.addURI("com.android.contacts", "directories_enterprise/#", 17004);
        profileAwareUriMatcher.addURI("com.android.contacts", "complete_name", 18000);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile", 19000);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/entities", 19001);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/data", 19002);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/data/#", 19003);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/photo", 19011);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/display_photo", 19012);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/as_vcard", 19004);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/raw_contacts", 19005);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/raw_contacts/#", 19006);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/raw_contacts/#/data", 19007);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/raw_contacts/#/entity", 19008);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/status_updates", 19009);
        profileAwareUriMatcher.addURI("com.android.contacts", "profile/raw_contact_entities", 19010);
        profileAwareUriMatcher.addURI("com.android.contacts", "stream_items", 21000);
        profileAwareUriMatcher.addURI("com.android.contacts", "stream_items/photo", 21001);
        profileAwareUriMatcher.addURI("com.android.contacts", "stream_items/#", 21002);
        profileAwareUriMatcher.addURI("com.android.contacts", "stream_items/#/photo", 21003);
        profileAwareUriMatcher.addURI("com.android.contacts", "stream_items/#/photo/#", 21004);
        profileAwareUriMatcher.addURI("com.android.contacts", "stream_items_limit", 21005);
        profileAwareUriMatcher.addURI("com.android.contacts", "display_photo/#", 22000);
        profileAwareUriMatcher.addURI("com.android.contacts", "photo_dimensions", 22001);
        profileAwareUriMatcher.addURI("com.android.contacts", "deleted_contacts", 23000);
        profileAwareUriMatcher.addURI("com.android.contacts", "deleted_contacts/#", 23001);
        profileAwareUriMatcher.addURI("com.android.contacts", "directory_file_enterprise/*", 24000);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/phone_email", 3101);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/phone_email/filter", 3102);
        profileAwareUriMatcher.addURI("com.android.contacts", "data/phone_email/filter/*", 3102);
        MODIFIED_KEY_SET_FOR_ENTERPRISE_FILTER = new ArraySet(Arrays.asList("directory"));
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
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "onCreate user=" + Process.myUserHandle().getIdentifier());
        }
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "ContactsProvider2.onCreate start");
        }
        super.onCreate();
        setAppOps(4, 5);
        try {
            try {
                boolean zInitialize = initialize();
                if (Log.isLoggable("ContactsPerf", 3)) {
                    Log.d("ContactsPerf", "ContactsProvider2.onCreate finish");
                }
                return zInitialize;
            } catch (RuntimeException e) {
                Log.e("ContactsProvider", "Cannot start provider", e);
                if (shouldThrowExceptionForInitializationError()) {
                    throw e;
                }
                if (Log.isLoggable("ContactsPerf", 3)) {
                    Log.d("ContactsPerf", "ContactsProvider2.onCreate finish");
                }
                return false;
            }
        } catch (Throwable th) {
            if (Log.isLoggable("ContactsPerf", 3)) {
                Log.d("ContactsPerf", "ContactsProvider2.onCreate finish");
            }
            throw th;
        }
    }

    protected boolean shouldThrowExceptionForInitializationError() {
        return false;
    }

    private boolean initialize() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
        this.mFastScrollingIndexCache = FastScrollingIndexCache.getInstance(getContext());
        this.mMetadataSyncEnabled = Settings.Global.getInt(getContext().getContentResolver(), "contact_metadata_sync_enabled", 0) == 1;
        this.mContactsHelper = getDatabaseHelper();
        this.mDbHelper.set(this.mContactsHelper);
        setDbHelperToSerializeOn(this.mContactsHelper, "contacts", this);
        this.mContactDirectoryManager = new ContactDirectoryManager(this);
        this.mGlobalSearchSupport = new GlobalSearchSupport(this);
        this.mReadAccessLatch = new CountDownLatch(1);
        this.mWriteAccessLatch = new CountDownLatch(1);
        this.mTaskScheduler = new ContactsTaskScheduler(getClass().getSimpleName()) {
            @Override
            public void onPerformTask(int i, Object obj) {
                ContactsProvider2.this.performBackgroundTask(i, obj);
            }
        };
        this.mProfileProvider = newProfileProvider();
        this.mProfileProvider.setDbHelperToSerializeOn(this.mContactsHelper, "contacts", this);
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = "com.android.contacts";
        this.mProfileProvider.attachInfo(getContext(), providerInfo);
        this.mProfileHelper = this.mProfileProvider.getDatabaseHelper();
        this.mEnterprisePolicyGuard = new EnterprisePolicyGuard(getContext());
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
        ContactsPackageMonitor.start(getContext());
        return true;
    }

    public void setNewAggregatorForTest(boolean z) {
        AbstractContactAggregator contactAggregator;
        if (z) {
            contactAggregator = new ContactAggregator2(this, this.mContactsHelper, createPhotoPriorityResolver(getContext()), this.mNameSplitter, this.mCommonNicknameCache);
        } else {
            contactAggregator = new ContactAggregator(this, this.mContactsHelper, createPhotoPriorityResolver(getContext()), this.mNameSplitter, this.mCommonNicknameCache);
        }
        this.mContactAggregator = contactAggregator;
        this.mContactAggregator.setEnabled(SystemProperties.getBoolean("sync.contacts.aggregate", true));
        initDataRowHandlers(this.mDataRowHandlers, this.mContactsHelper, this.mContactAggregator, this.mContactsPhotoStore);
    }

    private void initForDefaultLocale() {
        int i;
        AbstractContactAggregator contactAggregator2;
        Context context = getContext();
        this.mLegacyApiSupport = new LegacyApiSupport(context, this.mContactsHelper, this, this.mGlobalSearchSupport);
        this.mCurrentLocales = LocaleSet.newDefault();
        this.mNameSplitter = this.mContactsHelper.createNameSplitter(this.mCurrentLocales.getPrimaryLocale());
        this.mNameLookupBuilder = new StructuredNameLookupBuilder(this.mNameSplitter);
        this.mPostalSplitter = new PostalSplitter(this.mCurrentLocales.getPrimaryLocale());
        this.mCommonNicknameCache = new CommonNicknameCache(this.mContactsHelper.getReadableDatabase());
        ContactLocaleUtils.setLocales(this.mCurrentLocales);
        int i2 = Settings.Global.getInt(context.getContentResolver(), "new_contact_aggregator", 1);
        if (i2 == 0) {
            i = 4;
        } else {
            i = 5;
        }
        PROPERTY_AGGREGATION_ALGORITHM_VERSION = i;
        if (i2 == 0) {
            contactAggregator2 = new ContactAggregator(this, this.mContactsHelper, createPhotoPriorityResolver(context), this.mNameSplitter, this.mCommonNicknameCache);
        } else {
            contactAggregator2 = new ContactAggregator2(this, this.mContactsHelper, createPhotoPriorityResolver(context), this.mNameSplitter, this.mCommonNicknameCache);
        }
        this.mContactAggregator = contactAggregator2;
        this.mContactAggregator.setEnabled(SystemProperties.getBoolean("sync.contacts.aggregate", true));
        this.mProfileAggregator = new ProfileAggregator(this, this.mProfileHelper, createPhotoPriorityResolver(context), this.mNameSplitter, this.mCommonNicknameCache);
        this.mProfileAggregator.setEnabled(SystemProperties.getBoolean("sync.contacts.aggregate", true));
        this.mSearchIndexManager = new SearchIndexManager(this);
        this.mContactsPhotoStore = new PhotoStore(getContext().getFilesDir(), this.mContactsHelper);
        this.mProfilePhotoStore = new PhotoStore(new File(getContext().getFilesDir(), "profile"), this.mProfileHelper);
        this.mDataRowHandlers = new ArrayMap<>();
        initDataRowHandlers(this.mDataRowHandlers, this.mContactsHelper, this.mContactAggregator, this.mContactsPhotoStore);
        this.mProfileDataRowHandlers = new ArrayMap<>();
        initDataRowHandlers(this.mProfileDataRowHandlers, this.mProfileHelper, this.mProfileAggregator, this.mProfilePhotoStore);
        switchToContactMode();
    }

    private void initDataRowHandlers(Map<String, DataRowHandler> map, ContactsDatabaseHelper contactsDatabaseHelper, AbstractContactAggregator abstractContactAggregator, PhotoStore photoStore) {
        Context context = getContext();
        map.put("vnd.android.cursor.item/email_v2", new DataRowHandlerForEmail(context, contactsDatabaseHelper, abstractContactAggregator));
        map.put("vnd.android.cursor.item/im", new DataRowHandlerForIm(context, contactsDatabaseHelper, abstractContactAggregator));
        map.put("vnd.android.cursor.item/organization", new DataRowHandlerForOrganization(context, contactsDatabaseHelper, abstractContactAggregator));
        map.put("vnd.android.cursor.item/phone_v2", new DataRowHandlerForPhoneNumber(context, contactsDatabaseHelper, abstractContactAggregator));
        map.put("vnd.android.cursor.item/nickname", new DataRowHandlerForNickname(context, contactsDatabaseHelper, abstractContactAggregator));
        map.put("vnd.android.cursor.item/name", new DataRowHandlerForStructuredName(context, contactsDatabaseHelper, abstractContactAggregator, this.mNameSplitter, this.mNameLookupBuilder));
        map.put("vnd.android.cursor.item/postal-address_v2", new DataRowHandlerForStructuredPostal(context, contactsDatabaseHelper, abstractContactAggregator, this.mPostalSplitter));
        map.put("vnd.android.cursor.item/group_membership", new DataRowHandlerForGroupMembership(context, contactsDatabaseHelper, abstractContactAggregator, this.mGroupIdCache));
        map.put("vnd.android.cursor.item/photo", new DataRowHandlerForPhoto(context, contactsDatabaseHelper, abstractContactAggregator, photoStore, getMaxDisplayPhotoDim(), getMaxThumbnailDim()));
        map.put("vnd.android.cursor.item/note", new DataRowHandlerForNote(context, contactsDatabaseHelper, abstractContactAggregator));
        map.put("vnd.android.cursor.item/identity", new DataRowHandlerForIdentity(context, contactsDatabaseHelper, abstractContactAggregator));
        map.put("vnd.android.cursor.item/website", new DataRowHandlerForWebsite(context, contactsDatabaseHelper, abstractContactAggregator));
        if (ContactsProviderUtils.isImsCallEnabled() && ContactsProviderUtils.isVolteEnabled()) {
            map.put("vnd.android.cursor.item/ims", new DataRowHandlerForImsCall(context, contactsDatabaseHelper, abstractContactAggregator));
        }
    }

    PhotoPriorityResolver createPhotoPriorityResolver(Context context) {
        return new PhotoPriorityResolver(context);
    }

    protected void scheduleBackgroundTask(int i) {
        scheduleBackgroundTask(i, null);
    }

    protected void scheduleBackgroundTask(int i, Object obj) {
        this.mTaskScheduler.scheduleTask(i, obj);
    }

    protected void performBackgroundTask(int i, Object obj) {
        Log.d("ContactsProvider", "performBackgroundTask()+ **** task=" + i);
        switchToContactMode();
        switch (i) {
            case 0:
                int threadPriority = Process.getThreadPriority(Process.myTid());
                Process.setThreadPriority(0);
                initForDefaultLocale();
                this.mReadAccessLatch.countDown();
                this.mReadAccessLatch = null;
                loadLocalPhoneAccounts();
                Process.setThreadPriority(threadPriority);
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
                boolean zUpdateAccountsInBackground = updateAccountsInBackground(accounts);
                switchToProfileMode();
                boolean zUpdateAccountsInBackground2 = zUpdateAccountsInBackground | updateAccountsInBackground(accounts);
                switchToContactMode();
                updateContactsAccountCount(accounts);
                updateDirectoriesInBackground(zUpdateAccountsInBackground2);
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
            case 9:
                changeLocaleInBackground();
                break;
            case 10:
                long jCurrentTimeMillis = System.currentTimeMillis();
                if (jCurrentTimeMillis - this.mLastPhotoCleanup > 86400000) {
                    this.mLastPhotoCleanup = jCurrentTimeMillis;
                    switchToContactMode();
                    cleanupPhotoStore();
                    switchToProfileMode();
                    cleanupPhotoStore();
                    switchToContactMode();
                }
                break;
            case 11:
                DeletedContactsTableUtil.deleteOldLogs(this.mDbHelper.get().getWritableDatabase());
                break;
            case 12:
                updateDirectoriesInBackground(true);
                break;
        }
        Log.d("ContactsProvider", "performBackgroundTask()- **** task=" + i);
    }

    public void onLocaleChanged() {
        if (this.mProviderStatus != 0 && this.mProviderStatus != 3) {
            return;
        }
        scheduleBackgroundTask(9);
    }

    private static boolean needsToUpdateLocaleData(SharedPreferences sharedPreferences, LocaleSet localeSet, ContactsDatabaseHelper contactsDatabaseHelper, ProfileDatabaseHelper profileDatabaseHelper) {
        String string = sharedPreferences.getString("locale", null);
        if (localeSet.toString().equals(string)) {
            return contactsDatabaseHelper.needsToUpdateLocaleData(localeSet) || profileDatabaseHelper.needsToUpdateLocaleData(localeSet);
        }
        Log.i("ContactsProvider", "Locale has changed from " + string + " to " + localeSet);
        return true;
    }

    protected void updateLocaleInBackground() {
        if (this.mProviderStatus == 2) {
            return;
        }
        LocaleSet localeSet = this.mCurrentLocales;
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (!needsToUpdateLocaleData(defaultSharedPreferences, localeSet, this.mContactsHelper, this.mProfileHelper)) {
            return;
        }
        int i = this.mProviderStatus;
        setProviderStatus(2);
        this.mContactsHelper.setLocale(localeSet);
        this.mProfileHelper.setLocale(localeSet);
        this.mSearchIndexManager.updateIndex(true);
        defaultSharedPreferences.edit().putString("locale", localeSet.toString()).commit();
        setProviderStatus(i);
        if (!this.mCurrentLocales.isCurrent()) {
            scheduleBackgroundTask(9);
        }
    }

    protected static void updateLocaleOffline(Context context, ContactsDatabaseHelper contactsDatabaseHelper, ProfileDatabaseHelper profileDatabaseHelper) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        LocaleSet localeSetNewDefault = LocaleSet.newDefault();
        if (!needsToUpdateLocaleData(defaultSharedPreferences, localeSetNewDefault, contactsDatabaseHelper, profileDatabaseHelper)) {
            return;
        }
        contactsDatabaseHelper.setLocale(localeSetNewDefault);
        profileDatabaseHelper.setLocale(localeSetNewDefault);
        contactsDatabaseHelper.rebuildSearchIndex();
        defaultSharedPreferences.edit().putString("locale", localeSetNewDefault.toString()).commit();
    }

    private void changeLocaleInBackground() {
        SQLiteDatabase writableDatabase = this.mContactsHelper.getWritableDatabase();
        SQLiteDatabase writableDatabase2 = this.mProfileHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        writableDatabase2.beginTransaction();
        try {
            initForDefaultLocale();
            writableDatabase.setTransactionSuccessful();
            writableDatabase2.setTransactionSuccessful();
            writableDatabase.endTransaction();
            writableDatabase2.endTransaction();
            updateLocaleInBackground();
        } catch (Throwable th) {
            writableDatabase.endTransaction();
            writableDatabase2.endTransaction();
            throw th;
        }
    }

    protected void updateSearchIndexInBackground() {
        this.mSearchIndexManager.updateIndex(false);
    }

    protected void updateDirectoriesInBackground(boolean z) {
        this.mContactDirectoryManager.scanAllPackages(z);
    }

    private void updateProviderStatus() {
        if (this.mProviderStatus != 0 && this.mProviderStatus != 3) {
            return;
        }
        if (this.mContactsAccountCount == 0) {
            boolean zQueryIsEmpty = DatabaseUtils.queryIsEmpty(this.mContactsHelper.getReadableDatabase(), "contacts");
            long jQueryNumEntries = DatabaseUtils.queryNumEntries(this.mProfileHelper.getReadableDatabase(), "contacts", null);
            if (zQueryIsEmpty && jQueryNumEntries <= 1) {
                setProviderStatus(3);
                return;
            } else {
                setProviderStatus(0);
                return;
            }
        }
        setProviderStatus(0);
    }

    protected void cleanupPhotoStore() {
        int i;
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        Cursor cursorQuery = writableDatabase.query("view_data", new String[]{"_id", "data14"}, "mimetype_id=" + this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/photo") + " AND data14 IS NOT NULL", null, null, null, null);
        HashSet hashSetNewHashSet = Sets.newHashSet();
        HashMap mapNewHashMap = Maps.newHashMap();
        while (true) {
            try {
                i = 0;
                if (!cursorQuery.moveToNext()) {
                    break;
                }
                long j = cursorQuery.getLong(0);
                long j2 = cursorQuery.getLong(1);
                hashSetNewHashSet.add(Long.valueOf(j2));
                mapNewHashMap.put(Long.valueOf(j2), Long.valueOf(j));
            } finally {
            }
        }
        cursorQuery.close();
        cursorQuery = writableDatabase.query("stream_item_photos JOIN stream_items ON stream_item_id=stream_items._id", new String[]{"stream_item_photos._id", "stream_item_photos.stream_item_id", "photo_file_id"}, null, null, null, null, null);
        HashMap mapNewHashMap2 = Maps.newHashMap();
        HashMap mapNewHashMap3 = Maps.newHashMap();
        while (cursorQuery.moveToNext()) {
            try {
                long j3 = cursorQuery.getLong(i);
                long j4 = cursorQuery.getLong(1);
                long j5 = cursorQuery.getLong(2);
                hashSetNewHashSet.add(Long.valueOf(j5));
                mapNewHashMap2.put(Long.valueOf(j5), Long.valueOf(j3));
                mapNewHashMap3.put(Long.valueOf(j3), Long.valueOf(j4));
                i = 0;
            } finally {
            }
        }
        cursorQuery.close();
        Set<Long> setCleanup = this.mPhotoStore.get().cleanup(hashSetNewHashSet);
        try {
            if (!setCleanup.isEmpty()) {
                try {
                    writableDatabase.beginTransactionWithListener(inProfileMode() ? this.mProfileProvider : this);
                    Iterator<Long> it = setCleanup.iterator();
                    while (it.hasNext()) {
                        long jLongValue = it.next().longValue();
                        if (mapNewHashMap.containsKey(Long.valueOf(jLongValue))) {
                            long jLongValue2 = ((Long) mapNewHashMap.get(Long.valueOf(jLongValue))).longValue();
                            ContentValues contentValues = new ContentValues();
                            contentValues.putNull("data14");
                            updateData(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, jLongValue2), contentValues, null, null, false, false);
                        }
                        if (mapNewHashMap2.containsKey(Long.valueOf(jLongValue))) {
                            writableDatabase.delete("stream_item_photos", "_id=?", new String[]{String.valueOf(((Long) mapNewHashMap2.get(Long.valueOf(jLongValue))).longValue())});
                        }
                    }
                    writableDatabase.setTransactionSuccessful();
                } catch (Exception e) {
                    Log.e("ContactsProvider", "Failed to clean up outdated photo references", e);
                }
            }
        } finally {
            writableDatabase.endTransaction();
        }
    }

    @Override
    public ContactsDatabaseHelper newDatabaseHelper(Context context) {
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
        Boolean bool = this.mInProfileMode.get();
        return bool != null && bool.booleanValue();
    }

    void wipeData() {
        invalidateFastScrollingIndexCache();
        this.mContactsHelper.wipeData();
        this.mProfileHelper.wipeData();
        this.mContactsPhotoStore.clear();
        this.mProfilePhotoStore.clear();
        this.mProviderStatus = 3;
        initForDefaultLocale();
    }

    private void waitForAccess(CountDownLatch countDownLatch) {
        if (countDownLatch == null) {
            return;
        }
        while (true) {
            try {
                countDownLatch.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private int getIntValue(ContentValues contentValues, String str, int i) {
        Integer asInteger = contentValues.getAsInteger(str);
        return asInteger != null ? asInteger.intValue() : i;
    }

    private boolean flagExists(ContentValues contentValues, String str) {
        return contentValues.getAsInteger(str) != null;
    }

    private boolean flagIsSet(ContentValues contentValues, String str) {
        return getIntValue(contentValues, str, 0) != 0;
    }

    private boolean flagIsClear(ContentValues contentValues, String str) {
        return getIntValue(contentValues, str, 1) == 0;
    }

    private boolean mapsToProfileDb(Uri uri) {
        return sUriMatcher.mapsToProfile(uri);
    }

    private boolean mapsToProfileDbWithInsertedValues(Uri uri, ContentValues contentValues) {
        Long asLong;
        if (mapsToProfileDb(uri)) {
            return true;
        }
        int iMatch = sUriMatcher.match(uri);
        return INSERT_URI_ID_VALUE_MAP.containsKey(Integer.valueOf(iMatch)) && (asLong = contentValues.getAsLong(INSERT_URI_ID_VALUE_MAP.get(Integer.valueOf(iMatch)))) != null && ContactsContract.isProfileId(asLong.longValue());
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
    public Uri insert(Uri uri, ContentValues contentValues) {
        waitForAccess(this.mWriteAccessLatch);
        this.mContactsHelper.validateContentValues(getCallingPackage(), contentValues);
        if (mapsToProfileDbWithInsertedValues(uri, contentValues)) {
            switchToProfileMode();
            return this.mProfileProvider.insert(uri, contentValues);
        }
        switchToContactMode();
        return super.insert(uri, contentValues);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        waitForAccess(this.mWriteAccessLatch);
        this.mContactsHelper.validateContentValues(getCallingPackage(), contentValues);
        this.mContactsHelper.validateSql(getCallingPackage(), str);
        if (mapsToProfileDb(uri)) {
            switchToProfileMode();
            return this.mProfileProvider.update(uri, contentValues, str, strArr);
        }
        switchToContactMode();
        return super.update(uri, contentValues, str, strArr);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        waitForAccess(this.mWriteAccessLatch);
        this.mContactsHelper.validateSql(getCallingPackage(), str);
        try {
            if (mapsToProfileDb(uri)) {
                switchToProfileMode();
                return this.mProfileProvider.delete(uri, str, strArr);
            }
            switchToContactMode();
            return super.delete(uri, str, strArr);
        } catch (SQLiteDiskIOException e) {
            Log.w("ContactsProvider", "[delete]catch SQLiteDiskIOException!");
            return 0;
        }
    }

    @Override
    public Bundle call(String str, String str2, Bundle bundle) {
        waitForAccess(this.mReadAccessLatch);
        switchToContactMode();
        if ("authorize".equals(str)) {
            Uri uri = (Uri) bundle.getParcelable("uri_to_authorize");
            ContactsPermissions.enforceCallingOrSelfPermission(getContext(), "android.permission.READ_CONTACTS");
            Uri uriPreAuthorizeUri = preAuthorizeUri(uri);
            Bundle bundle2 = new Bundle();
            bundle2.putParcelable("authorized_uri", uriPreAuthorizeUri);
            return bundle2;
        }
        if ("undemote".equals(str)) {
            ContactsPermissions.enforceCallingOrSelfPermission(getContext(), "android.permission.WRITE_CONTACTS");
            try {
                undemoteContact(this.mDbHelper.get().getWritableDatabase(), Long.valueOf(str2).longValue());
                return null;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Contact ID must be a valid long number.");
            }
        }
        if (!"get_aas".equals(str)) {
            return null;
        }
        String aASLabel = SimCardUtils.getAASLabel(str2);
        Bundle bundle3 = new Bundle();
        bundle3.putCharSequence("aas", aASLabel);
        return bundle3;
    }

    private Uri preAuthorizeUri(Uri uri) {
        Uri uriBuild = uri.buildUpon().appendQueryParameter("perm_token", String.valueOf(this.mRandom.nextLong())).build();
        long jCurrentTimeMillis = Clock.getInstance().currentTimeMillis() + this.mPreAuthorizedUriDuration;
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("expiration", Long.valueOf(jCurrentTimeMillis));
        contentValues.put("uri", uriBuild.toString());
        writableDatabase.insert("pre_authorized_uris", null, contentValues);
        return uriBuild;
    }

    public boolean isValidPreAuthorizedUri(Uri uri) {
        if (uri.getQueryParameter("perm_token") == null) {
            return false;
        }
        long jCurrentTimeMillis = Clock.getInstance().currentTimeMillis();
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        writableDatabase.beginTransactionNonExclusive();
        try {
            boolean z = true;
            writableDatabase.delete("pre_authorized_uris", "expiration < ?1", new String[]{String.valueOf(jCurrentTimeMillis)});
            if (writableDatabase.query("pre_authorized_uris", null, "uri=?1", new String[]{uri.toString()}, null, null, null).getCount() == 0) {
                z = false;
            }
            writableDatabase.setTransactionSuccessful();
            return z;
        } finally {
            writableDatabase.endTransaction();
        }
    }

    @Override
    protected boolean yield(ContactsTransaction contactsTransaction) {
        SQLiteDatabase sQLiteDatabaseRemoveDbForTag = contactsTransaction.removeDbForTag("profile");
        if (sQLiteDatabaseRemoveDbForTag != null) {
            sQLiteDatabaseRemoveDbForTag.setTransactionSuccessful();
            sQLiteDatabaseRemoveDbForTag.endTransaction();
        }
        SQLiteDatabase dbForTag = contactsTransaction.getDbForTag("contacts");
        return dbForTag != null && dbForTag.yieldIfContendedSafely(4000L);
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws OperationApplicationException {
        waitForAccess(this.mWriteAccessLatch);
        return super.applyBatch(arrayList);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        waitForAccess(this.mWriteAccessLatch);
        return super.bulkInsert(uri, contentValuesArr);
    }

    @Override
    public void onBegin() {
        onBeginTransactionInternal(false);
    }

    protected void onBeginTransactionInternal(boolean z) {
        if (z) {
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

    protected void onCommitTransactionInternal(boolean z) throws Throwable {
        if (z) {
            switchToProfileMode();
        } else {
            switchToContactMode();
        }
        flushTransactionalChanges();
        this.mAggregator.get().aggregateInTransaction(this.mTransactionContext.get(), this.mDbHelper.get().getWritableDatabase());
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

    protected void onRollbackTransactionInternal(boolean z) {
        if (z) {
            switchToProfileMode();
        } else {
            switchToContactMode();
        }
    }

    private void updateSearchIndexInTransaction() {
        Set<Long> staleSearchIndexContactIds = this.mTransactionContext.get().getStaleSearchIndexContactIds();
        Set<Long> staleSearchIndexRawContactIds = this.mTransactionContext.get().getStaleSearchIndexRawContactIds();
        if (!staleSearchIndexContactIds.isEmpty() || !staleSearchIndexRawContactIds.isEmpty()) {
            this.mSearchIndexManager.updateIndexForRawContacts(staleSearchIndexContactIds, staleSearchIndexRawContactIds);
            this.mTransactionContext.get().clearSearchIndexUpdates();
        }
    }

    private void flushTransactionalChanges() throws Throwable {
        if (VERBOSE_LOGGING) {
            StringBuilder sb = new StringBuilder();
            sb.append("flushTransactionalChanges: ");
            sb.append(inProfileMode() ? "profile" : "contacts");
            Log.v("ContactsProvider", sb.toString());
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        Iterator<Long> it = this.mTransactionContext.get().getInsertedRawContactIds().iterator();
        while (it.hasNext()) {
            long jLongValue = it.next().longValue();
            this.mDbHelper.get().updateRawContactDisplayName(writableDatabase, jLongValue);
            this.mAggregator.get().onRawContactInsert(this.mTransactionContext.get(), writableDatabase, jLongValue);
            if (this.mMetadataSyncEnabled) {
                updateMetadataOnRawContactInsert(writableDatabase, jLongValue);
            }
        }
        if (this.mMetadataSyncEnabled) {
            Iterator<Long> it2 = this.mTransactionContext.get().getBackupIdChangedRawContacts().iterator();
            while (it2.hasNext()) {
                updateMetadataOnRawContactInsert(writableDatabase, it2.next().longValue());
            }
        }
        Set<Long> dirtyRawContactIds = this.mTransactionContext.get().getDirtyRawContactIds();
        if (!dirtyRawContactIds.isEmpty()) {
            this.mSb.setLength(0);
            this.mSb.append("UPDATE raw_contacts SET dirty=1 WHERE _id IN (");
            appendIds(this.mSb, dirtyRawContactIds);
            this.mSb.append(")");
            writableDatabase.execSQL(this.mSb.toString());
        }
        Set<Long> updatedRawContactIds = this.mTransactionContext.get().getUpdatedRawContactIds();
        if (!updatedRawContactIds.isEmpty()) {
            this.mSb.setLength(0);
            this.mSb.append("UPDATE raw_contacts SET version = version + 1 WHERE _id IN (");
            appendIds(this.mSb, updatedRawContactIds);
            this.mSb.append(")");
            writableDatabase.execSQL(this.mSb.toString());
        }
        Set<Long> metadataDirtyRawContactIds = this.mTransactionContext.get().getMetadataDirtyRawContactIds();
        if (!metadataDirtyRawContactIds.isEmpty() && this.mMetadataSyncEnabled) {
            this.mSb.setLength(0);
            this.mSb.append("UPDATE raw_contacts SET metadata_dirty=1 WHERE _id IN (");
            appendIds(this.mSb, metadataDirtyRawContactIds);
            this.mSb.append(")");
            writableDatabase.execSQL(this.mSb.toString());
            this.mSyncToMetadataNetWork = true;
        }
        Set<Long> changedRawContactIds = this.mTransactionContext.get().getChangedRawContactIds();
        ContactsTableUtil.updateContactLastUpdateByRawContactId(writableDatabase, changedRawContactIds);
        if (!changedRawContactIds.isEmpty() && this.mMetadataSyncEnabled) {
            this.mSb.setLength(0);
            this.mSb.append("UPDATE metadata_sync SET deleted=1 WHERE _id IN (SELECT metadata_sync._id FROM raw_contacts JOIN metadata_sync ON (raw_contacts.backup_id=metadata_sync.raw_contact_backup_id AND raw_contacts.account_id=metadata_sync.account_id) WHERE raw_contacts.deleted=1 AND raw_contacts._id IN (");
            appendIds(this.mSb, changedRawContactIds);
            this.mSb.append("))");
            writableDatabase.execSQL(this.mSb.toString());
            this.mSyncToMetadataNetWork = true;
        }
        for (Map.Entry<Long, Object> entry : this.mTransactionContext.get().getUpdatedSyncStates()) {
            if (this.mDbHelper.get().getSyncState().update(writableDatabase, entry.getKey().longValue(), entry.getValue()) <= 0) {
                throw new IllegalStateException("unable to update sync state, does it still exist?");
            }
        }
        this.mTransactionContext.get().clearExceptSearchIndexUpdates();
    }

    void setMetadataSyncForTest(boolean z) {
        this.mMetadataSyncEnabled = z;
    }

    private String queryMetadataSyncData(SQLiteDatabase sQLiteDatabase, long j) {
        String string;
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = sQLiteDatabase.query("raw_contacts JOIN metadata_sync ON (raw_contacts.backup_id=metadata_sync.raw_contact_backup_id AND raw_contacts.account_id=metadata_sync.account_id)", MetadataSyncQuery.COLUMNS, "metadata_sync.deleted=0 AND raw_contacts._id=?", this.mSelectionArgs1, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                string = cursorQuery.getString(1);
            } else {
                string = null;
            }
            return string;
        } finally {
            cursorQuery.close();
        }
    }

    private void updateMetadataOnRawContactInsert(SQLiteDatabase sQLiteDatabase, long j) throws Throwable {
        String strQueryMetadataSyncData = queryMetadataSyncData(sQLiteDatabase, j);
        if (TextUtils.isEmpty(strQueryMetadataSyncData)) {
            return;
        }
        updateFromMetaDataEntry(sQLiteDatabase, MetadataEntryParser.parseDataToMetaDataEntry(strQueryMetadataSyncData));
    }

    private void appendIds(StringBuilder sb, Set<Long> set) {
        Iterator<Long> it = set.iterator();
        while (it.hasNext()) {
            sb.append(it.next().longValue());
            sb.append(',');
        }
        sb.setLength(sb.length() - 1);
    }

    @Override
    protected void notifyChange() {
        notifyChange(this.mSyncToNetwork, this.mSyncToMetadataNetWork);
        this.mSyncToNetwork = false;
        this.mSyncToMetadataNetWork = false;
    }

    protected void notifyChange(boolean z, boolean z2) {
        getContext().getContentResolver().notifyChange(ContactsContract.AUTHORITY_URI, (ContentObserver) null, z || z2);
        getContext().getContentResolver().notifyChange(ContactsContract.MetadataSync.METADATA_AUTHORITY_URI, (ContentObserver) null, z2);
    }

    protected void setProviderStatus(int i) {
        if (this.mProviderStatus != i) {
            this.mProviderStatus = i;
            ContactsDatabaseHelper.notifyProviderStatusChange(getContext());
        }
    }

    public DataRowHandler getDataRowHandler(String str) {
        if (inProfileMode()) {
            return getDataRowHandlerForProfile(str);
        }
        DataRowHandler dataRowHandler = this.mDataRowHandlers.get(str);
        if (dataRowHandler == null) {
            DataRowHandlerForCustomMimetype dataRowHandlerForCustomMimetype = new DataRowHandlerForCustomMimetype(getContext(), this.mContactsHelper, this.mContactAggregator, str);
            this.mDataRowHandlers.put(str, dataRowHandlerForCustomMimetype);
            return dataRowHandlerForCustomMimetype;
        }
        return dataRowHandler;
    }

    public DataRowHandler getDataRowHandlerForProfile(String str) {
        DataRowHandler dataRowHandler = this.mProfileDataRowHandlers.get(str);
        if (dataRowHandler == null) {
            DataRowHandlerForCustomMimetype dataRowHandlerForCustomMimetype = new DataRowHandlerForCustomMimetype(getContext(), this.mProfileHelper, this.mProfileAggregator, str);
            this.mProfileDataRowHandlers.put(str, dataRowHandlerForCustomMimetype);
            return dataRowHandlerForCustomMimetype;
        }
        return dataRowHandler;
    }

    @Override
    protected Uri insertInTransaction(Uri uri, ContentValues contentValues) throws Throwable {
        long jInsertRawContact;
        int i;
        long jInsertStatusUpdate;
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "insertInTransaction: uri=" + uri + "  values=[" + contentValues + "] CPID=" + Binder.getCallingPid());
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        boolean booleanQueryParameter = readBooleanQueryParameter(uri, "caller_is_syncadapter", false);
        int iMatch = sUriMatcher.match(uri);
        switch (iMatch) {
            case 1000:
                invalidateFastScrollingIndexCache();
                insertContact(contentValues);
                jInsertRawContact = 0;
                if (jInsertRawContact >= 0) {
                    return null;
                }
                return ContentUris.withAppendedId(uri, jInsertRawContact);
            case 2002:
            case 19005:
                invalidateFastScrollingIndexCache();
                jInsertRawContact = insertRawContact(uri, contentValues, booleanQueryParameter);
                this.mSyncToNetwork |= !booleanQueryParameter;
                if (jInsertRawContact >= 0) {
                }
                break;
            case 2004:
            case 19007:
                invalidateFastScrollingIndexCache();
                if (iMatch != 2004) {
                    i = 2;
                } else {
                    i = 1;
                }
                contentValues.put("raw_contact_id", uri.getPathSegments().get(i));
                jInsertRawContact = insertData(contentValues, booleanQueryParameter);
                this.mSyncToNetwork |= !booleanQueryParameter;
                if (jInsertRawContact >= 0) {
                }
                break;
            case 2007:
                contentValues.put("raw_contact_id", uri.getPathSegments().get(1));
                jInsertRawContact = insertStreamItem(uri, contentValues);
                this.mSyncToNetwork |= !booleanQueryParameter;
                if (jInsertRawContact >= 0) {
                }
                break;
            case 3000:
            case 19002:
                invalidateFastScrollingIndexCache();
                jInsertRawContact = insertData(contentValues, booleanQueryParameter);
                this.mSyncToNetwork |= !booleanQueryParameter;
                if (jInsertRawContact >= 0) {
                }
                break;
            case 7000:
            case 19009:
                jInsertStatusUpdate = insertStatusUpdate(contentValues);
                jInsertRawContact = jInsertStatusUpdate;
                if (jInsertRawContact >= 0) {
                }
                break;
            case 9000:
                jInsertRawContact = insertSettings(contentValues);
                this.mSyncToNetwork |= !booleanQueryParameter;
                if (jInsertRawContact >= 0) {
                }
                break;
            case 10000:
                jInsertRawContact = insertGroup(uri, contentValues, booleanQueryParameter);
                this.mSyncToNetwork |= !booleanQueryParameter;
                if (jInsertRawContact >= 0) {
                }
                break;
            case 11000:
            case 11002:
                jInsertStatusUpdate = this.mDbHelper.get().getSyncState().insert(writableDatabase, contentValues);
                jInsertRawContact = jInsertStatusUpdate;
                if (jInsertRawContact >= 0) {
                }
                break;
            case 19000:
                throw new UnsupportedOperationException("The profile contact is created automatically");
            case 21000:
                jInsertRawContact = insertStreamItem(uri, contentValues);
                this.mSyncToNetwork |= !booleanQueryParameter;
                if (jInsertRawContact >= 0) {
                }
                break;
            case 21001:
                jInsertRawContact = insertStreamItemPhoto(uri, contentValues);
                this.mSyncToNetwork |= !booleanQueryParameter;
                if (jInsertRawContact >= 0) {
                }
                break;
            case 21003:
                contentValues.put("stream_item_id", uri.getPathSegments().get(1));
                jInsertRawContact = insertStreamItemPhoto(uri, contentValues);
                this.mSyncToNetwork |= !booleanQueryParameter;
                if (jInsertRawContact >= 0) {
                }
                break;
            default:
                this.mSyncToNetwork = true;
                return this.mLegacyApiSupport.insert(uri, contentValues);
        }
    }

    private Account resolveAccount(Uri uri, ContentValues contentValues) throws IllegalArgumentException {
        String queryParameter = getQueryParameter(uri, "account_name");
        String queryParameter2 = getQueryParameter(uri, "account_type");
        boolean zIsEmpty = TextUtils.isEmpty(queryParameter) ^ TextUtils.isEmpty(queryParameter2);
        String asString = contentValues.getAsString("account_name");
        String asString2 = contentValues.getAsString("account_type");
        boolean zIsEmpty2 = TextUtils.isEmpty(asString) ^ TextUtils.isEmpty(asString2);
        if (zIsEmpty || zIsEmpty2) {
            throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Must specify both or neither of ACCOUNT_NAME and ACCOUNT_TYPE", uri));
        }
        boolean z = !TextUtils.isEmpty(queryParameter);
        boolean z2 = !TextUtils.isEmpty(asString);
        if (z2 && z) {
            if (!(TextUtils.equals(queryParameter, asString) && TextUtils.equals(queryParameter2, asString2))) {
                throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("When both specified, ACCOUNT_NAME and ACCOUNT_TYPE must match", uri));
            }
        } else if (z) {
            contentValues.put("account_name", queryParameter);
            contentValues.put("account_type", queryParameter2);
        } else {
            if (!z2) {
                return null;
            }
            queryParameter = asString;
            queryParameter2 = asString2;
        }
        if (this.mAccount == null || !this.mAccount.name.equals(queryParameter) || !this.mAccount.type.equals(queryParameter2)) {
            this.mAccount = new Account(queryParameter, queryParameter2);
        }
        return this.mAccount;
    }

    private AccountWithDataSet resolveAccountWithDataSet(Uri uri, ContentValues contentValues) {
        Account accountResolveAccount = resolveAccount(uri, contentValues);
        if (accountResolveAccount != null) {
            String queryParameter = getQueryParameter(uri, "data_set");
            if (queryParameter == null) {
                queryParameter = contentValues.getAsString("data_set");
            } else {
                contentValues.put("data_set", queryParameter);
            }
            return AccountWithDataSet.get(accountResolveAccount.name, accountResolveAccount.type, queryParameter);
        }
        return null;
    }

    private long insertContact(ContentValues contentValues) {
        throw new UnsupportedOperationException("Aggregate contacts are created automatically");
    }

    private long insertRawContact(Uri uri, ContentValues contentValues, boolean z) {
        ContentValues contentValues2 = new ContentValues(fixUpUsageColumnsForEdit(contentValues));
        contentValues2.putNull("contact_id");
        long jReplaceAccountInfoByAccountId = replaceAccountInfoByAccountId(uri, contentValues2);
        if (flagIsSet(contentValues2, "deleted")) {
            contentValues2.put("aggregation_mode", (Integer) 3);
        }
        boolean zShouldMarkMetadataDirtyForRawContact = shouldMarkMetadataDirtyForRawContact(contentValues2);
        if (!contentValues2.containsKey("pinned")) {
            contentValues2.put("pinned", (Integer) 0);
        }
        long jInsert = this.mDbHelper.get().getWritableDatabase().insert("raw_contacts", "contact_id", contentValues2);
        if (zShouldMarkMetadataDirtyForRawContact) {
            this.mTransactionContext.get().markRawContactMetadataDirty(jInsert, false);
        }
        this.mSyncToMetadataNetWork |= z;
        this.mAggregator.get().markNewForAggregation(jInsert, getIntValue(contentValues2, "aggregation_mode", 0));
        this.mTransactionContext.get().rawContactInserted(jInsert, jReplaceAccountInfoByAccountId);
        if (!z) {
            addAutoAddMembership(jInsert);
            if (flagIsSet(contentValues2, "starred")) {
                updateFavoritesMembership(jInsert, true);
            }
        }
        this.mProviderStatusUpdateNeeded = true;
        return jInsert;
    }

    private void addAutoAddMembership(long j) {
        Long lFindGroupByRawContactId = findGroupByRawContactId("raw_contacts._id=? AND groups.account_id=raw_contacts.account_id AND auto_add != 0", j);
        if (lFindGroupByRawContactId != null) {
            insertDataGroupMembership(j, lFindGroupByRawContactId.longValue());
        }
    }

    private Long findGroupByRawContactId(String str, long j) {
        Cursor cursorQuery = this.mDbHelper.get().getReadableDatabase().query("groups,raw_contacts", PROJECTION_GROUP_ID, str, new String[]{Long.toString(j)}, null, null, null);
        try {
            if (cursorQuery.moveToNext()) {
                return Long.valueOf(cursorQuery.getLong(0));
            }
            return null;
        } finally {
            cursorQuery.close();
        }
    }

    private void updateFavoritesMembership(long j, boolean z) {
        Long lFindGroupByRawContactId = findGroupByRawContactId("raw_contacts._id=? AND groups.account_id=raw_contacts.account_id AND favorites != 0", j);
        if (lFindGroupByRawContactId != null) {
            if (z) {
                insertDataGroupMembership(j, lFindGroupByRawContactId.longValue());
            } else {
                deleteDataGroupMembership(j, lFindGroupByRawContactId.longValue());
            }
        }
    }

    private void insertDataGroupMembership(long j, long j2) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("data1", Long.valueOf(j2));
        contentValues.put("raw_contact_id", Long.valueOf(j));
        contentValues.put("mimetype_id", Long.valueOf(this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/group_membership")));
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        getDataRowHandler("vnd.android.cursor.item/group_membership").handleHashIdForInsert(contentValues);
        writableDatabase.insert("data", null, contentValues);
    }

    private void deleteDataGroupMembership(long j, long j2) {
        this.mDbHelper.get().getWritableDatabase().delete("data", "mimetype_id=? AND data1=? AND raw_contact_id=?", new String[]{Long.toString(this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/group_membership")), Long.toString(j2), Long.toString(j)});
    }

    private long insertData(ContentValues contentValues, boolean z) {
        Long asLong = contentValues.getAsLong("raw_contact_id");
        if (asLong == null) {
            throw new IllegalArgumentException("raw_contact_id is required");
        }
        String asString = contentValues.getAsString("mimetype");
        if (TextUtils.isEmpty(asString)) {
            throw new IllegalArgumentException("mimetype is required");
        }
        if ("vnd.android.cursor.item/phone_v2".equals(asString)) {
            maybeTrimLongPhoneNumber(contentValues);
        }
        ContentValues contentValues2 = new ContentValues(contentValues);
        replacePackageNameByPackageId(contentValues2);
        contentValues2.put("mimetype_id", Long.valueOf(this.mDbHelper.get().getMimeTypeId(asString)));
        contentValues2.remove("mimetype");
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        TransactionContext transactionContext = this.mTransactionContext.get();
        long jInsert = getDataRowHandler(asString).insert(writableDatabase, transactionContext, asLong.longValue(), contentValues2);
        transactionContext.markRawContactDirtyAndChanged(asLong.longValue(), z);
        transactionContext.rawContactUpdated(asLong.longValue());
        return jInsert;
    }

    private long insertStreamItem(Uri uri, ContentValues contentValues) {
        Long asLong = contentValues.getAsLong("raw_contact_id");
        if (asLong == null) {
            throw new IllegalArgumentException("raw_contact_id is required");
        }
        ContentValues contentValues2 = new ContentValues(contentValues);
        contentValues2.remove("account_name");
        contentValues2.remove("account_type");
        long jInsert = this.mDbHelper.get().getWritableDatabase().insert("stream_items", null, contentValues2);
        if (jInsert == -1) {
            return 0L;
        }
        return cleanUpOldStreamItems(asLong.longValue(), jInsert);
    }

    private long insertStreamItemPhoto(Uri uri, ContentValues contentValues) {
        Long asLong = contentValues.getAsLong("stream_item_id");
        if (asLong == null || asLong.longValue() == 0) {
            return 0L;
        }
        ContentValues contentValues2 = new ContentValues(contentValues);
        contentValues2.remove("account_name");
        contentValues2.remove("account_type");
        if (!processStreamItemPhoto(contentValues2, false)) {
            return 0L;
        }
        return this.mDbHelper.get().getWritableDatabase().insert("stream_item_photos", null, contentValues2);
    }

    private boolean processStreamItemPhoto(ContentValues contentValues, boolean z) {
        byte[] asByteArray = contentValues.getAsByteArray("photo");
        if (asByteArray == null) {
            return z;
        }
        IOException iOException = null;
        try {
            long jInsert = this.mPhotoStore.get().insert(new PhotoProcessor(asByteArray, getMaxDisplayPhotoDim(), getMaxThumbnailDim(), true), true);
            if (jInsert != 0) {
                contentValues.put("photo_file_id", Long.valueOf(jInsert));
                contentValues.remove("photo");
                return true;
            }
        } catch (IOException e) {
            iOException = e;
        }
        Log.e("ContactsProvider", "Could not process stream item photo for insert", iOException);
        return false;
    }

    private long cleanUpOldStreamItems(long j, long j2) {
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        Cursor cursorQuery = writableDatabase.query("stream_items", new String[]{"_id"}, "raw_contact_id=?", new String[]{String.valueOf(j)}, null, null, "timestamp DESC, _id DESC");
        try {
            if (cursorQuery.getCount() <= 5) {
                return j2;
            }
            cursorQuery.moveToLast();
            long j3 = j2;
            while (cursorQuery.getPosition() >= 5) {
                if (j2 == cursorQuery.getLong(0)) {
                    j3 = 0;
                }
                deleteStreamItem(writableDatabase, cursorQuery.getLong(0));
                cursorQuery.moveToPrevious();
            }
            return j3;
        } finally {
            cursorQuery.close();
        }
    }

    private int deleteData(String str, String[] strArr, boolean z) {
        Uri uriWithAppendedPath;
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        if (inProfileMode()) {
            uriWithAppendedPath = Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, "data");
        } else {
            uriWithAppendedPath = ContactsContract.Data.CONTENT_URI;
        }
        Cursor cursorQuery = query(uriWithAppendedPath, DataRowHandler.DataDeleteQuery.COLUMNS, str, strArr, null);
        int iDelete = 0;
        while (cursorQuery.moveToNext()) {
            try {
                long j = cursorQuery.getLong(2);
                iDelete += getDataRowHandler(cursorQuery.getString(1)).delete(writableDatabase, this.mTransactionContext.get(), cursorQuery);
                this.mTransactionContext.get().markRawContactDirtyAndChanged(j, z);
            } finally {
                cursorQuery.close();
            }
        }
        return iDelete;
    }

    public int deleteData(long j, String[] strArr) {
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        boolean z = false;
        this.mSelectionArgs1[0] = String.valueOf(j);
        Cursor cursorQuery = query(ContactsContract.Data.CONTENT_URI, DataRowHandler.DataDeleteQuery.COLUMNS, "_id=?", this.mSelectionArgs1, null);
        try {
            if (!cursorQuery.moveToFirst()) {
                return 0;
            }
            String string = cursorQuery.getString(1);
            int length = strArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (!TextUtils.equals(string, strArr[i])) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            }
            if (!z) {
                throw new IllegalArgumentException("Data type mismatch: expected " + Lists.newArrayList(strArr));
            }
            return getDataRowHandler(string).delete(writableDatabase, this.mTransactionContext.get(), cursorQuery);
        } finally {
            cursorQuery.close();
        }
    }

    private long insertGroup(Uri uri, ContentValues contentValues, boolean z) {
        long j;
        ContentValues contentValues2 = new ContentValues(contentValues);
        long jReplaceAccountInfoByAccountId = replaceAccountInfoByAccountId(uri, contentValues2);
        replacePackageNameByPackageId(contentValues2);
        if (!z) {
            contentValues2.put("dirty", (Integer) 1);
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        long jInsert = writableDatabase.insert("groups", "title", contentValues2);
        boolean zFlagIsSet = flagIsSet(contentValues2, "favorites");
        if (!z && zFlagIsSet) {
            this.mSelectionArgs1[0] = Long.toString(jReplaceAccountInfoByAccountId);
            long j2 = jInsert;
            Cursor cursorQuery = writableDatabase.query("raw_contacts", new String[]{"_id", "starred"}, "raw_contacts.account_id=?", this.mSelectionArgs1, null, null, null);
            while (cursorQuery.moveToNext()) {
                try {
                    if (cursorQuery.getLong(1) != 0) {
                        long j3 = cursorQuery.getLong(0);
                        long j4 = j2;
                        insertDataGroupMembership(j3, j4);
                        this.mTransactionContext.get().markRawContactDirtyAndChanged(j3, z);
                        j2 = j4;
                    }
                } finally {
                    cursorQuery.close();
                }
            }
            j = j2;
        } else {
            j = jInsert;
        }
        if (contentValues2.containsKey("group_visible")) {
            this.mVisibleTouched = true;
        }
        return j;
    }

    private long insertSettings(ContentValues contentValues) throws Throwable {
        String str;
        String[] strArr;
        String asString = contentValues.getAsString("account_name");
        String asString2 = contentValues.getAsString("account_type");
        String asString3 = contentValues.getAsString("data_set");
        Uri.Builder builderBuildUpon = ContactsContract.Settings.CONTENT_URI.buildUpon();
        if (asString != null) {
            builderBuildUpon.appendQueryParameter("account_name", asString);
        }
        if (asString2 != null) {
            builderBuildUpon.appendQueryParameter("account_type", asString2);
        }
        if (asString3 != null) {
            builderBuildUpon.appendQueryParameter("data_set", asString3);
        }
        Cursor cursorQueryLocal = queryLocal(builderBuildUpon.build(), null, null, null, null, 0L, null);
        try {
            if (cursorQueryLocal.getCount() > 0) {
                if (asString == null || asString2 == null) {
                    str = null;
                    strArr = null;
                } else if (asString3 == null) {
                    str = "account_name=? AND account_type=? AND data_set IS NULL";
                    strArr = new String[]{asString, asString2};
                } else {
                    String[] strArr2 = {asString, asString2, asString3};
                    str = "account_name=? AND account_type=? AND data_set=?";
                    strArr = strArr2;
                }
                return updateSettings(contentValues, str, strArr);
            }
            cursorQueryLocal.close();
            long jInsert = this.mDbHelper.get().getWritableDatabase().insert("settings", null, contentValues);
            if (contentValues.containsKey("ungrouped_visible")) {
                this.mVisibleTouched = true;
            }
            return jInsert;
        } finally {
            cursorQueryLocal.close();
        }
    }

    private long insertStatusUpdate(ContentValues contentValues) throws Throwable {
        String asString;
        Cursor cursor;
        String str;
        String str2;
        Object obj;
        long j;
        Long l;
        Resources resourcesForApplication;
        Integer asInteger;
        Integer numValueOf;
        long j2;
        String str3;
        String str4;
        String str5;
        String str6;
        int i;
        int i2;
        String asString2 = contentValues.getAsString("im_handle");
        Integer asInteger2 = contentValues.getAsInteger("protocol");
        ContactsDatabaseHelper contactsDatabaseHelper = this.mDbHelper.get();
        SQLiteDatabase writableDatabase = contactsDatabaseHelper.getWritableDatabase();
        if (asInteger2 != null && asInteger2.intValue() == -1) {
            asString = contentValues.getAsString("custom_protocol");
            if (TextUtils.isEmpty(asString)) {
                throw new IllegalArgumentException("CUSTOM_PROTOCOL is required when PROTOCOL=PROTOCOL_CUSTOM");
            }
        } else {
            asString = null;
        }
        Long asLong = contentValues.getAsLong("presence_data_id");
        this.mSb.setLength(0);
        this.mSelectionArgs.clear();
        if (asLong != null) {
            this.mSb.append("data._id=?");
            this.mSelectionArgs.add(String.valueOf(asLong));
        } else {
            if (TextUtils.isEmpty(asString2) || asInteger2 == null) {
                throw new IllegalArgumentException("PROTOCOL and IM_HANDLE are required");
            }
            boolean z = 5 == asInteger2.intValue();
            String strValueOf = String.valueOf(contactsDatabaseHelper.getMimeTypeIdForIm());
            if (z) {
                String strValueOf2 = String.valueOf(contactsDatabaseHelper.getMimeTypeIdForEmail());
                this.mSb.append("mimetype_id IN (?,?) AND data1=? AND ((mimetype_id=? AND data5=?");
                this.mSelectionArgs.add(strValueOf2);
                this.mSelectionArgs.add(strValueOf);
                this.mSelectionArgs.add(asString2);
                this.mSelectionArgs.add(strValueOf);
                this.mSelectionArgs.add(String.valueOf(asInteger2));
                if (asString != null) {
                    this.mSb.append(" AND data6=?");
                    this.mSelectionArgs.add(asString);
                }
                this.mSb.append(") OR (mimetype_id=?))");
                this.mSelectionArgs.add(strValueOf2);
            } else {
                this.mSb.append("mimetype_id=? AND data5=? AND data1=?");
                this.mSelectionArgs.add(strValueOf);
                this.mSelectionArgs.add(String.valueOf(asInteger2));
                this.mSelectionArgs.add(asString2);
                if (asString != null) {
                    this.mSb.append(" AND data6=?");
                    this.mSelectionArgs.add(asString);
                }
            }
            String asString3 = contentValues.getAsString("presence_data_id");
            if (asString3 != null) {
                this.mSb.append(" AND data._id=?");
                this.mSelectionArgs.add(asString3);
            }
        }
        try {
            Cursor cursorQuery = writableDatabase.query("data JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) JOIN accounts ON (accounts._id=raw_contacts.account_id)JOIN contacts ON (raw_contacts.contact_id = contacts._id)", DataContactsQuery.PROJECTION, this.mSb.toString(), (String[]) this.mSelectionArgs.toArray(EMPTY_STRING_ARRAY), null, null, "EXISTS (SELECT _id FROM visible_contacts WHERE contacts._id=visible_contacts._id) DESC, raw_contact_id");
            try {
                if (cursorQuery.moveToFirst()) {
                    Long lValueOf = Long.valueOf(cursorQuery.getLong(4));
                    long j3 = cursorQuery.getLong(0);
                    String string = cursorQuery.getString(1);
                    String string2 = cursorQuery.getString(2);
                    long j4 = cursorQuery.getLong(5);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    String asString4 = contentValues.getAsString("mode");
                    if (asString4 != null) {
                        if (asString == null) {
                            asString = "";
                        }
                        ContentValues contentValues2 = new ContentValues();
                        str = string2;
                        contentValues2.put("presence_data_id", lValueOf);
                        str2 = string;
                        contentValues2.put("presence_raw_contact_id", Long.valueOf(j3));
                        contentValues2.put("presence_contact_id", Long.valueOf(j4));
                        contentValues2.put("protocol", asInteger2);
                        contentValues2.put("custom_protocol", asString);
                        contentValues2.put("im_handle", asString2);
                        String asString5 = contentValues.getAsString("im_account");
                        if (asString5 != null) {
                            contentValues2.put("im_account", asString5);
                        }
                        contentValues2.put("mode", asString4);
                        contentValues2.put("chat_capability", contentValues.getAsString("chat_capability"));
                        obj = null;
                        writableDatabase.replace("presence", null, contentValues2);
                    } else {
                        str = string2;
                        str2 = string;
                        obj = null;
                    }
                    if (contentValues.containsKey("status")) {
                        String asString6 = contentValues.getAsString("status");
                        String asString7 = contentValues.getAsString("status_res_package");
                        Resources resources = getContext().getResources();
                        if (!TextUtils.isEmpty(asString7)) {
                            try {
                                resourcesForApplication = getContext().getPackageManager().getResourcesForApplication(asString7);
                            } catch (PackageManager.NameNotFoundException e) {
                                Log.w("ContactsProvider", "Contact status update resource package not found: " + asString7);
                                resourcesForApplication = resources;
                            }
                            asInteger = contentValues.getAsInteger("status_label");
                            if ((asInteger != null || asInteger.intValue() == 0) && asInteger2 != null) {
                                numValueOf = Integer.valueOf(ContactsContract.CommonDataKinds.Im.getProtocolLabelResource(asInteger2.intValue()));
                            } else {
                                numValueOf = asInteger;
                            }
                            String resourceName = getResourceName(resourcesForApplication, "string", numValueOf);
                            Integer asInteger3 = contentValues.getAsInteger("status_icon");
                            String resourceName2 = getResourceName(resourcesForApplication, "drawable", asInteger3);
                            if (!TextUtils.isEmpty(asString6)) {
                                Long asLong2 = contentValues.getAsLong("status_ts");
                                if (asLong2 != null) {
                                    str3 = str;
                                    j2 = j3;
                                    l = lValueOf;
                                    contactsDatabaseHelper.replaceStatusUpdate(lValueOf, asLong2.longValue(), asString6, asString7, asInteger3, numValueOf);
                                    str4 = str2;
                                    str5 = resourceName;
                                    str6 = asString7;
                                    j = j4;
                                    i2 = 1;
                                    i = 0;
                                } else {
                                    j2 = j3;
                                    l = lValueOf;
                                    str3 = str;
                                    str4 = str2;
                                    str5 = resourceName;
                                    str6 = asString7;
                                    j = j4;
                                    i = 0;
                                    i2 = 1;
                                    contactsDatabaseHelper.insertStatusUpdate(l, asString6, asString7, asInteger3, numValueOf);
                                }
                                long j5 = j2;
                                if (j5 != -1 && !TextUtils.isEmpty(asString6)) {
                                    ContentValues contentValues3 = new ContentValues();
                                    contentValues3.put("raw_contact_id", Long.valueOf(j5));
                                    contentValues3.put("text", statusUpdateToHtml(asString6));
                                    contentValues3.put("comments", "");
                                    contentValues3.put("res_package", str6);
                                    contentValues3.put("icon", resourceName2);
                                    contentValues3.put("label", str5);
                                    contentValues3.put("timestamp", Long.valueOf(asLong2 == null ? System.currentTimeMillis() : asLong2.longValue()));
                                    String str7 = str3;
                                    if (str7 != null && str4 != null) {
                                        contentValues3.put("account_name", str7);
                                        contentValues3.put("account_type", str4);
                                    }
                                    Uri uri = ContactsContract.StreamItems.CONTENT_URI;
                                    String[] strArr = new String[i2];
                                    strArr[i] = String.valueOf(j5);
                                    Cursor cursorQueryLocal = queryLocal(uri, new String[]{"_id"}, "raw_contact_id=?", strArr, null, -1L, null);
                                    try {
                                        if (cursorQueryLocal.getCount() > 0) {
                                            cursorQueryLocal.moveToFirst();
                                            updateInTransaction(ContentUris.withAppendedId(uri, cursorQueryLocal.getLong(i)), contentValues3, null, null);
                                        } else {
                                            insertInTransaction(uri, contentValues3);
                                        }
                                    } finally {
                                        cursorQueryLocal.close();
                                    }
                                }
                            } else {
                                contactsDatabaseHelper.deleteStatusUpdate(lValueOf.longValue());
                                j = j4;
                                l = lValueOf;
                            }
                        } else {
                            resourcesForApplication = resources;
                            asInteger = contentValues.getAsInteger("status_label");
                            if (asInteger != null) {
                                numValueOf = Integer.valueOf(ContactsContract.CommonDataKinds.Im.getProtocolLabelResource(asInteger2.intValue()));
                                String resourceName3 = getResourceName(resourcesForApplication, "string", numValueOf);
                                Integer asInteger32 = contentValues.getAsInteger("status_icon");
                                String resourceName22 = getResourceName(resourcesForApplication, "drawable", asInteger32);
                                if (!TextUtils.isEmpty(asString6)) {
                                }
                            } else {
                                numValueOf = Integer.valueOf(ContactsContract.CommonDataKinds.Im.getProtocolLabelResource(asInteger2.intValue()));
                                String resourceName32 = getResourceName(resourcesForApplication, "string", numValueOf);
                                Integer asInteger322 = contentValues.getAsInteger("status_icon");
                                String resourceName222 = getResourceName(resourcesForApplication, "drawable", asInteger322);
                                if (!TextUtils.isEmpty(asString6)) {
                                }
                            }
                        }
                    } else {
                        j = j4;
                        l = lValueOf;
                    }
                    long j6 = j;
                    if (j6 != -1) {
                        this.mAggregator.get().updateLastStatusUpdateId(j6);
                    }
                    return l.longValue();
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return -1L;
            } catch (Throwable th) {
                th = th;
                cursor = cursorQuery;
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            cursor = null;
        }
    }

    private String statusUpdateToHtml(String str) {
        return TextUtils.htmlEncode(str);
    }

    private String getResourceName(Resources resources, String str, Integer num) {
        if (num != null) {
            try {
                if (num.intValue() != 0) {
                    String resourceEntryName = resources.getResourceEntryName(num.intValue());
                    String resourceTypeName = resources.getResourceTypeName(num.intValue());
                    if (!str.equals(resourceTypeName)) {
                        Log.w("ContactsProvider", "Resource " + num + " (" + resourceEntryName + ") is of type " + resourceTypeName + " but " + str + " is required.");
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
    protected int deleteInTransaction(Uri uri, String str, String[] strArr) throws Throwable {
        String[] strArr2;
        Cursor cursorQuery;
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "deleteInTransaction: uri=" + uri + "  selection=[" + str + "]  args=" + Arrays.toString(strArr) + " CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        flushTransactionalChanges();
        boolean booleanQueryParameter = readBooleanQueryParameter(uri, "caller_is_syncadapter", false);
        switch (sUriMatcher.match(uri)) {
            case 1000:
                invalidateFastScrollingIndexCache();
                if ("true".equals(uri.getQueryParameter("batch"))) {
                    return deleteContactInOneBatch(writableDatabase, strArr, booleanQueryParameter);
                }
                return 0;
            case 1001:
                invalidateFastScrollingIndexCache();
                return deleteContact(ContentUris.parseId(uri), booleanQueryParameter);
            case 1002:
                invalidateFastScrollingIndexCache();
                List<String> pathSegments = uri.getPathSegments();
                if (pathSegments.size() < 3) {
                    throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Missing a lookup key", uri));
                }
                return deleteContact(lookupContactIdByLookupKey(writableDatabase, pathSegments.get(2)), booleanQueryParameter);
            case 1003:
                invalidateFastScrollingIndexCache();
                String str2 = uri.getPathSegments().get(2);
                SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
                setTablesAndProjectionMapForContacts(sQLiteQueryBuilder, null);
                long id = ContentUris.parseId(uri);
                if (strArr == null) {
                    strArr2 = new String[2];
                } else {
                    strArr2 = new String[strArr.length + 2];
                    System.arraycopy(strArr, 0, strArr2, 2, strArr.length);
                }
                String[] strArr3 = strArr2;
                strArr3[0] = String.valueOf(id);
                strArr3[1] = Uri.encode(str2);
                sQLiteQueryBuilder.appendWhere("_id=? AND lookup=?");
                Cursor cursorDoQuery = doQuery(writableDatabase, sQLiteQueryBuilder, null, str, strArr3, null, null, null, null, null);
                try {
                    if (cursorDoQuery.getCount() == 1) {
                        return deleteContact(id, booleanQueryParameter);
                    }
                    cursorDoQuery.close();
                    return 0;
                } finally {
                    cursorDoQuery.close();
                }
            case 1026:
                return deleteDataUsage();
            case 2002:
            case 19005:
                invalidateFastScrollingIndexCache();
                if ("true".equals(uri.getQueryParameter("sim"))) {
                    this.mProviderStatusUpdateNeeded = true;
                    int iDelete = writableDatabase.delete("raw_contacts", str, strArr);
                    if (iDelete > 0) {
                        writableDatabase.execSQL("DELETE FROM search_index WHERE contact_id NOT IN ( SELECT _id FROM contacts)");
                    }
                    return iDelete;
                }
                Log.v("ContactsProvider", "deleteInTransaction for raw_contacts uri is " + uri.toString() + " callerIsSyncAdapter is " + booleanQueryParameter);
                if (booleanQueryParameter && "true".equals(uri.getQueryParameter("batch"))) {
                    Log.i("ContactsProvider", "Delete in one batch begin");
                    int iDeleteRawContactInOneBatch = deleteRawContactInOneBatch(uri, str, strArr);
                    Log.i("ContactsProvider", "deleteRawContactInOneBatch count is " + iDeleteRawContactInOneBatch);
                    Log.i("ContactsProvider", "Delete in one batch end");
                    return iDeleteRawContactInOneBatch;
                }
                cursorQuery = writableDatabase.query("view_raw_contacts", new String[]{"_id", "contact_id"}, appendAccountIdToSelection(uri, str), strArr, null, null, null);
                int iDeleteRawContact = 0;
                while (cursorQuery.moveToNext()) {
                    try {
                        iDeleteRawContact += deleteRawContact(cursorQuery.getLong(0), cursorQuery.getLong(1), booleanQueryParameter);
                    } finally {
                    }
                    break;
                }
                return iDeleteRawContact;
            case 2003:
            case 19006:
                invalidateFastScrollingIndexCache();
                long id2 = ContentUris.parseId(uri);
                return deleteRawContact(id2, this.mDbHelper.get().getContactId(id2), booleanQueryParameter);
            case 2008:
                this.mSyncToNetwork |= !booleanQueryParameter;
                return deleteStreamItems("raw_contact_id=? AND _id=?", new String[]{uri.getPathSegments().get(1), uri.getLastPathSegment()});
            case 3000:
            case 19002:
                invalidateFastScrollingIndexCache();
                this.mSyncToNetwork |= !booleanQueryParameter;
                return deleteData(appendAccountToSelection(uri, str), strArr, booleanQueryParameter);
            case 3001:
            case 3003:
            case 3006:
            case 3010:
            case 3012:
            case 19003:
                invalidateFastScrollingIndexCache();
                long id3 = ContentUris.parseId(uri);
                this.mSyncToNetwork |= !booleanQueryParameter;
                this.mSelectionArgs1[0] = String.valueOf(id3);
                return deleteData("_id=?", this.mSelectionArgs1, booleanQueryParameter);
            case 7000:
            case 19009:
                return deleteStatusUpdates(str, strArr);
            case 9000:
                this.mSyncToNetwork |= !booleanQueryParameter;
                return deleteSettings(appendAccountToSelection(uri, str), strArr);
            case 10000:
                cursorQuery = writableDatabase.query("view_groups", ContactsDatabaseHelper.Projections.ID, appendAccountIdToSelection(uri, str), strArr, null, null, null);
                int iDeleteGroup = 0;
                while (cursorQuery.moveToNext()) {
                    try {
                        iDeleteGroup += deleteGroup(uri, cursorQuery.getLong(0), booleanQueryParameter);
                    } finally {
                    }
                    break;
                }
                if (iDeleteGroup > 0) {
                    this.mSyncToNetwork |= !booleanQueryParameter;
                }
                return iDeleteGroup;
            case 10001:
                this.mSyncToNetwork |= !booleanQueryParameter;
                return deleteGroup(uri, ContentUris.parseId(uri), booleanQueryParameter);
            case 11000:
            case 11002:
                return this.mDbHelper.get().getSyncState().delete(writableDatabase, str, strArr);
            case 11001:
                StringBuilder sb = new StringBuilder();
                sb.append("_id=");
                sb.append(ContentUris.parseId(uri));
                sb.append(" ");
                sb.append(str == null ? "" : " AND (" + str + ")");
                return this.mDbHelper.get().getSyncState().delete(writableDatabase, sb.toString(), strArr);
            case 11003:
                StringBuilder sb2 = new StringBuilder();
                sb2.append("_id=");
                sb2.append(ContentUris.parseId(uri));
                sb2.append(" ");
                sb2.append(str == null ? "" : " AND (" + str + ")");
                return this.mProfileHelper.getSyncState().delete(writableDatabase, sb2.toString(), strArr);
            case 21000:
                this.mSyncToNetwork |= !booleanQueryParameter;
                return deleteStreamItems(str, strArr);
            case 21002:
                this.mSyncToNetwork |= !booleanQueryParameter;
                return deleteStreamItems("_id=?", new String[]{uri.getLastPathSegment()});
            case 21003:
                this.mSyncToNetwork |= !booleanQueryParameter;
                String str3 = uri.getPathSegments().get(1);
                StringBuilder sb3 = new StringBuilder();
                sb3.append("stream_item_id=");
                sb3.append(str3);
                sb3.append(" ");
                sb3.append(str == null ? "" : " AND (" + str + ")");
                return deleteStreamItemPhotos(sb3.toString(), strArr);
            case 21004:
                this.mSyncToNetwork |= !booleanQueryParameter;
                return deleteStreamItemPhotos("stream_item_photos._id=? AND stream_item_id=?", new String[]{uri.getPathSegments().get(3), uri.getPathSegments().get(1)});
            default:
                this.mSyncToNetwork = true;
                return this.mLegacyApiSupport.delete(uri, str, strArr);
        }
    }

    public int deleteGroup(Uri uri, long j, boolean z) {
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        this.mGroupIdCache.clear();
        writableDatabase.delete("data", "mimetype_id=" + this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/group_membership") + " AND data1=" + j, null);
        try {
            if (z) {
                return writableDatabase.delete("groups", "_id=" + j, null);
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("deleted", (Integer) 1);
            contentValues.put("dirty", (Integer) 1);
            return writableDatabase.update("groups", contentValues, "_id=" + j, null);
        } finally {
            this.mVisibleTouched = true;
        }
    }

    private int deleteSettings(String str, String[] strArr) {
        int iDelete = this.mDbHelper.get().getWritableDatabase().delete("settings", str, strArr);
        this.mVisibleTouched = true;
        return iDelete;
    }

    private int deleteContact(long j, boolean z) {
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        this.mSelectionArgs1[0] = Long.toString(j);
        Cursor cursorQuery = writableDatabase.query("raw_contacts", new String[]{"_id"}, "contact_id=?", this.mSelectionArgs1, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                markRawContactAsDeleted(writableDatabase, cursorQuery.getLong(0), z);
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        this.mProviderStatusUpdateNeeded = true;
        int iDeleteContact = ContactsTableUtil.deleteContact(writableDatabase, j);
        scheduleBackgroundTask(11);
        return iDeleteContact;
    }

    public int deleteRawContact(long j, long j2, boolean z) {
        int iMarkRawContactAsDeleted;
        this.mAggregator.get().invalidateAggregationExceptionCache();
        this.mProviderStatusUpdateNeeded = true;
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        Cursor cursorQuery = writableDatabase.query("stream_items", new String[]{"_id"}, "raw_contact_id=?", new String[]{String.valueOf(j)}, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                deleteStreamItem(writableDatabase, cursorQuery.getLong(0));
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        boolean z2 = ContactsTableUtil.deleteContactIfSingleton(writableDatabase, j) == 1;
        if (z || rawContactIsLocal(j)) {
            writableDatabase.delete("presence", "presence_raw_contact_id=" + j, null);
            int iDelete = writableDatabase.delete("raw_contacts", "_id=" + j, null);
            this.mTransactionContext.get().markRawContactChangedOrDeletedOrInserted(j);
            iMarkRawContactAsDeleted = iDelete;
        } else {
            iMarkRawContactAsDeleted = markRawContactAsDeleted(writableDatabase, j, z);
        }
        if (!z2) {
            this.mAggregator.get().updateAggregateData(this.mTransactionContext.get(), j2);
        }
        return iMarkRawContactAsDeleted;
    }

    private boolean rawContactIsLocal(long j) {
        Cursor cursorQuery = this.mDbHelper.get().getReadableDatabase().query("raw_contacts", ContactsDatabaseHelper.Projections.LITERAL_ONE, "raw_contacts._id=? AND account_id=" + ContactsDatabaseHelper.Clauses.LOCAL_ACCOUNT_ID, new String[]{String.valueOf(j)}, null, null, null);
        try {
            return cursorQuery.getCount() > 0;
        } finally {
            cursorQuery.close();
        }
    }

    private int deleteStatusUpdates(String str, String[] strArr) {
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "deleting data from status_updates for " + str);
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        writableDatabase.delete("status_updates", getWhereClauseForStatusUpdatesTable(str), strArr);
        return writableDatabase.delete("presence", str, strArr);
    }

    private int deleteStreamItems(String str, String[] strArr) {
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        Cursor cursorQuery = writableDatabase.query("view_stream_items", ContactsDatabaseHelper.Projections.ID, str, strArr, null, null, null);
        try {
            cursorQuery.moveToPosition(-1);
            int iDeleteStreamItem = 0;
            while (cursorQuery.moveToNext()) {
                iDeleteStreamItem += deleteStreamItem(writableDatabase, cursorQuery.getLong(0));
            }
            return iDeleteStreamItem;
        } finally {
            cursorQuery.close();
        }
    }

    private int deleteStreamItem(SQLiteDatabase sQLiteDatabase, long j) {
        deleteStreamItemPhotos(j);
        return sQLiteDatabase.delete("stream_items", "_id=?", new String[]{String.valueOf(j)});
    }

    private int deleteStreamItemPhotos(String str, String[] strArr) {
        return this.mDbHelper.get().getWritableDatabase().delete("stream_item_photos", str, strArr);
    }

    private int deleteStreamItemPhotos(long j) {
        return this.mDbHelper.get().getWritableDatabase().delete("stream_item_photos", "stream_item_id=?", new String[]{String.valueOf(j)});
    }

    private int markRawContactAsDeleted(SQLiteDatabase sQLiteDatabase, long j, boolean z) {
        this.mSyncToNetwork = true;
        ContentValues contentValues = new ContentValues();
        contentValues.put("deleted", (Integer) 1);
        contentValues.put("aggregation_mode", (Integer) 3);
        contentValues.put("aggregation_needed", (Integer) 1);
        contentValues.putNull("contact_id");
        contentValues.put("dirty", (Integer) 1);
        return updateRawContact(sQLiteDatabase, j, contentValues, z, false);
    }

    private int deleteDataUsage() {
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        writableDatabase.execSQL("UPDATE raw_contacts SET x_times_contacted=0,x_last_time_contacted=NULL");
        writableDatabase.execSQL("UPDATE contacts SET x_times_contacted=0,x_last_time_contacted=NULL");
        writableDatabase.delete("data_usage_stat", null, null);
        return 1;
    }

    @Override
    protected int updateInTransaction(Uri uri, ContentValues contentValues, String str, String[] strArr) throws Throwable {
        int iUpdateData;
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "updateInTransaction: uri=" + uri + "  selection=[" + str + "]  args=" + Arrays.toString(strArr) + "  values=[" + contentValues + "] CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        int iMatch = sUriMatcher.match(uri);
        if (iMatch == 11001 && str == null) {
            this.mTransactionContext.get().syncStateUpdated(ContentUris.parseId(uri), contentValues.get("data"));
            return 1;
        }
        flushTransactionalChanges();
        boolean booleanQueryParameter = readBooleanQueryParameter(uri, "caller_is_syncadapter", false);
        switch (iMatch) {
            case 1000:
            case 19000:
                invalidateFastScrollingIndexCache();
                return updateContactOptions(contentValues, str, strArr, booleanQueryParameter);
            case 1001:
                invalidateFastScrollingIndexCache();
                return updateContactOptions(writableDatabase, ContentUris.parseId(uri), contentValues, booleanQueryParameter);
            case 1002:
            case 1003:
                invalidateFastScrollingIndexCache();
                List<String> pathSegments = uri.getPathSegments();
                int size = pathSegments.size();
                if (size < 3) {
                    throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Missing a lookup key", uri));
                }
                if (size > 3) {
                    String lastPathSegment = uri.getLastPathSegment();
                    Log.i("ContactsProvider", "[updateInTransaction]contactId:" + Long.parseLong(lastPathSegment));
                    Log.i("ContactsProvider", "[updateInTransaction]callerIsSyncAdapter:" + booleanQueryParameter);
                    return updateContactOptions(writableDatabase, Long.parseLong(lastPathSegment), contentValues, booleanQueryParameter);
                }
                return updateContactOptions(writableDatabase, lookupContactIdByLookupKey(writableDatabase, pathSegments.get(2)), contentValues, booleanQueryParameter);
            case 2002:
            case 19005:
                invalidateFastScrollingIndexCache();
                return updateRawContacts(contentValues, appendAccountIdToSelection(uri, str), strArr, booleanQueryParameter);
            case 2003:
                invalidateFastScrollingIndexCache();
                long id = ContentUris.parseId(uri);
                if (str != null) {
                    return updateRawContacts(contentValues, "_id=? AND(" + str + ")", insertSelectionArg(strArr, String.valueOf(id)), booleanQueryParameter);
                }
                this.mSelectionArgs1[0] = String.valueOf(id);
                return updateRawContacts(contentValues, "_id=?", this.mSelectionArgs1, booleanQueryParameter);
            case 2004:
            case 19007:
                invalidateFastScrollingIndexCache();
                String str2 = uri.getPathSegments().get(iMatch == 2004 ? 1 : 2);
                StringBuilder sb = new StringBuilder();
                sb.append("raw_contact_id=");
                sb.append(str2);
                sb.append(" ");
                sb.append(str == null ? "" : " AND " + str);
                return updateData(uri, contentValues, sb.toString(), strArr, booleanQueryParameter, false);
            case 2008:
                return updateStreamItems(contentValues, "raw_contact_id=? AND _id=?", new String[]{uri.getPathSegments().get(1), uri.getLastPathSegment()});
            case 3000:
            case 19002:
                invalidateFastScrollingIndexCache();
                iUpdateData = updateData(uri, contentValues, appendAccountToSelection(uri, str), strArr, booleanQueryParameter, false);
                if (iUpdateData > 0) {
                    this.mSyncToNetwork |= !booleanQueryParameter;
                }
                break;
            case 3001:
            case 3003:
            case 3006:
            case 3010:
            case 3012:
                invalidateFastScrollingIndexCache();
                iUpdateData = updateData(uri, contentValues, str, strArr, booleanQueryParameter, false);
                if (iUpdateData > 0) {
                    this.mSyncToNetwork |= !booleanQueryParameter;
                }
                break;
            case 6000:
                int iUpdateAggregationException = updateAggregationException(writableDatabase, contentValues, false);
                invalidateFastScrollingIndexCache();
                return iUpdateAggregationException;
            case 7000:
            case 19009:
                return updateStatusUpdate(contentValues, str, strArr);
            case 9000:
                iUpdateData = updateSettings(contentValues, appendAccountToSelection(uri, str), strArr);
                this.mSyncToNetwork |= !booleanQueryParameter;
                break;
            case 10000:
                iUpdateData = updateGroups(contentValues, appendAccountIdToSelection(uri, str), strArr, booleanQueryParameter);
                if (iUpdateData > 0) {
                    this.mSyncToNetwork |= !booleanQueryParameter;
                }
                break;
            case 10001:
                String[] strArrInsertSelectionArg = insertSelectionArg(strArr, String.valueOf(ContentUris.parseId(uri)));
                StringBuilder sb2 = new StringBuilder();
                sb2.append("_id=? ");
                sb2.append(str == null ? "" : " AND " + str);
                iUpdateData = updateGroups(contentValues, sb2.toString(), strArrInsertSelectionArg, booleanQueryParameter);
                if (iUpdateData > 0) {
                    this.mSyncToNetwork |= !booleanQueryParameter;
                }
                break;
            case 11000:
            case 11002:
                return this.mDbHelper.get().getSyncState().update(writableDatabase, contentValues, appendAccountToSelection(uri, str), strArr);
            case 11001:
                String strAppendAccountToSelection = appendAccountToSelection(uri, str);
                StringBuilder sb3 = new StringBuilder();
                sb3.append("_id=");
                sb3.append(ContentUris.parseId(uri));
                sb3.append(" ");
                sb3.append(strAppendAccountToSelection == null ? "" : " AND (" + strAppendAccountToSelection + ")");
                return this.mDbHelper.get().getSyncState().update(writableDatabase, contentValues, sb3.toString(), strArr);
            case 11003:
                String strAppendAccountToSelection2 = appendAccountToSelection(uri, str);
                StringBuilder sb4 = new StringBuilder();
                sb4.append("_id=");
                sb4.append(ContentUris.parseId(uri));
                sb4.append(" ");
                sb4.append(strAppendAccountToSelection2 == null ? "" : " AND (" + strAppendAccountToSelection2 + ")");
                return this.mProfileHelper.getSyncState().update(writableDatabase, contentValues, sb4.toString(), strArr);
            case 17001:
                this.mContactDirectoryManager.setDirectoriesForceUpdated(true);
                scanPackagesByUid(Binder.getCallingUid());
                return 1;
            case 20001:
                return handleDataUsageFeedback(uri) ? 1 : 0;
            case 21000:
                return updateStreamItems(contentValues, str, strArr);
            case 21001:
                return updateStreamItemPhotos(contentValues, str, strArr);
            case 21002:
                return updateStreamItems(contentValues, "_id=?", new String[]{uri.getLastPathSegment()});
            case 21003:
                return updateStreamItemPhotos(contentValues, "stream_item_id=?", new String[]{uri.getPathSegments().get(1)});
            case 21004:
                return updateStreamItemPhotos(contentValues, "stream_item_photos._id=? AND stream_item_photos.stream_item_id=?", new String[]{uri.getPathSegments().get(3), uri.getPathSegments().get(1)});
            default:
                this.mSyncToNetwork = true;
                return this.mLegacyApiSupport.update(uri, contentValues, str, strArr);
        }
        return iUpdateData;
    }

    private void scanPackagesByUid(int i) {
        String[] packagesForUid = getContext().getPackageManager().getPackagesForUid(i);
        if (packagesForUid != null) {
            for (String str : packagesForUid) {
                onPackageChanged(str);
            }
        }
    }

    private int updateStatusUpdate(ContentValues contentValues, String str, String[] strArr) {
        int iUpdate;
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        ContentValues settableColumnsForStatusUpdatesTable = getSettableColumnsForStatusUpdatesTable(contentValues);
        if (settableColumnsForStatusUpdatesTable.size() > 0) {
            iUpdate = writableDatabase.update("status_updates", settableColumnsForStatusUpdatesTable, getWhereClauseForStatusUpdatesTable(str), strArr);
        } else {
            iUpdate = 0;
        }
        ContentValues settableColumnsForPresenceTable = getSettableColumnsForPresenceTable(contentValues);
        if (settableColumnsForPresenceTable.size() > 0) {
            return writableDatabase.update("presence", settableColumnsForPresenceTable, str, strArr);
        }
        return iUpdate;
    }

    private int updateStreamItems(ContentValues contentValues, String str, String[] strArr) {
        contentValues.remove("raw_contact_id");
        contentValues.remove("account_name");
        contentValues.remove("account_type");
        return this.mDbHelper.get().getWritableDatabase().update("stream_items", contentValues, str, strArr);
    }

    private int updateStreamItemPhotos(ContentValues contentValues, String str, String[] strArr) {
        contentValues.remove("stream_item_id");
        contentValues.remove("account_name");
        contentValues.remove("account_type");
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        if (processStreamItemPhoto(contentValues, true)) {
            return writableDatabase.update("stream_item_photos", contentValues, str, strArr);
        }
        return 0;
    }

    private String getWhereClauseForStatusUpdatesTable(String str) {
        this.mSb.setLength(0);
        this.mSb.append("status_update_data_id IN (SELECT Distinct presence_data_id FROM status_updates LEFT OUTER JOIN presence ON status_update_data_id = presence_data_id WHERE ");
        this.mSb.append(str);
        this.mSb.append(")");
        return this.mSb.toString();
    }

    private ContentValues getSettableColumnsForStatusUpdatesTable(ContentValues contentValues) {
        ContentValues contentValues2 = new ContentValues();
        ContactsDatabaseHelper.copyStringValue(contentValues2, "status", contentValues, "status");
        ContactsDatabaseHelper.copyStringValue(contentValues2, "status_ts", contentValues, "status_ts");
        ContactsDatabaseHelper.copyStringValue(contentValues2, "status_res_package", contentValues, "status_res_package");
        ContactsDatabaseHelper.copyStringValue(contentValues2, "status_label", contentValues, "status_label");
        ContactsDatabaseHelper.copyStringValue(contentValues2, "status_icon", contentValues, "status_icon");
        return contentValues2;
    }

    private ContentValues getSettableColumnsForPresenceTable(ContentValues contentValues) {
        ContentValues contentValues2 = new ContentValues();
        ContactsDatabaseHelper.copyStringValue(contentValues2, "mode", contentValues, "mode");
        ContactsDatabaseHelper.copyStringValue(contentValues2, "chat_capability", contentValues, "chat_capability");
        return contentValues2;
    }

    private int updateGroups(ContentValues contentValues, String str, String[] strArr, boolean z) {
        String string;
        String string2;
        String string3;
        HashSet hashSet;
        this.mGroupIdCache.clear();
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        ContactsDatabaseHelper contactsDatabaseHelper = this.mDbHelper.get();
        ContentValues contentValues2 = new ContentValues();
        contentValues2.putAll(contentValues);
        if (!z && !contentValues2.containsKey("dirty")) {
            contentValues2.put("dirty", (Integer) 1);
        }
        if (contentValues2.containsKey("group_visible")) {
            this.mVisibleTouched = true;
        }
        boolean zContainsKey = contentValues2.containsKey("account_name");
        boolean zContainsKey2 = contentValues2.containsKey("account_type");
        boolean zContainsKey3 = contentValues2.containsKey("data_set");
        boolean z2 = zContainsKey || zContainsKey2 || zContainsKey3;
        String asString = contentValues2.getAsString("account_name");
        String asString2 = contentValues2.getAsString("account_type");
        String asString3 = contentValues2.getAsString("data_set");
        contentValues2.remove("account_name");
        contentValues2.remove("account_type");
        contentValues2.remove("data_set");
        HashSet hashSetNewHashSet = Sets.newHashSet();
        Cursor cursorQuery = writableDatabase.query("view_groups", GroupAccountQuery.COLUMNS, str, strArr, null, null, null);
        try {
            cursorQuery.moveToPosition(-1);
            int i = 0;
            while (cursorQuery.moveToNext()) {
                this.mSelectionArgs1[0] = Long.toString(cursorQuery.getLong(0));
                if (!zContainsKey) {
                    string = cursorQuery.getString(2);
                } else {
                    string = asString;
                }
                if (!zContainsKey2) {
                    string2 = cursorQuery.getString(1);
                } else {
                    string2 = asString2;
                }
                if (!zContainsKey3) {
                    string3 = cursorQuery.getString(3);
                } else {
                    string3 = asString3;
                }
                if (z2) {
                    contentValues2.put("account_id", Long.valueOf(contactsDatabaseHelper.getOrCreateAccountIdInTransaction(AccountWithDataSet.get(string, string2, string3))));
                }
                int iUpdate = writableDatabase.update("groups", contentValues2, "groups._id=?", this.mSelectionArgs1);
                if (iUpdate > 0 && !TextUtils.isEmpty(string) && !TextUtils.isEmpty(string2)) {
                    Account account = new Account(string, string2);
                    hashSet = hashSetNewHashSet;
                    hashSet.add(account);
                } else {
                    hashSet = hashSetNewHashSet;
                }
                i += iUpdate;
                hashSetNewHashSet = hashSet;
            }
            HashSet hashSet2 = hashSetNewHashSet;
            cursorQuery.close();
            if (flagIsSet(contentValues2, "should_sync")) {
                Iterator it = hashSet2.iterator();
                while (it.hasNext()) {
                    ContentResolver.requestSync((Account) it.next(), "com.android.contacts", new Bundle());
                }
            }
            return i;
        } catch (Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    private int updateSettings(ContentValues contentValues, String str, String[] strArr) {
        int iUpdate = this.mDbHelper.get().getWritableDatabase().update("settings", contentValues, str, strArr);
        if (contentValues.containsKey("ungrouped_visible")) {
            this.mVisibleTouched = true;
        }
        return iUpdate;
    }

    private int updateRawContacts(ContentValues contentValues, String str, String[] strArr, boolean z) {
        if (contentValues.containsKey("contact_id")) {
            throw new IllegalArgumentException("contact_id should not be included in content values. Contact IDs are assigned automatically");
        }
        if (!z) {
            str = DatabaseUtils.concatenateWhere(str, "raw_contact_is_read_only=0");
        }
        String str2 = str;
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        Cursor cursorQuery = writableDatabase.query("view_raw_contacts", ContactsDatabaseHelper.Projections.ID, str2, strArr, null, null, null);
        int i = 0;
        while (cursorQuery.moveToNext()) {
            try {
                updateRawContact(writableDatabase, cursorQuery.getLong(0), contentValues, z, false);
                i++;
            } finally {
                cursorQuery.close();
            }
        }
        return i;
    }

    private ContentValues fixUpUsageColumnsForEdit(ContentValues contentValues) {
        if (!contentValues.containsKey("last_time_contacted") && !contentValues.containsKey("times_contacted")) {
            return contentValues;
        }
        ContentValues contentValues2 = new ContentValues(contentValues);
        ContactsDatabaseHelper.copyLongValue(contentValues2, "x_last_time_contacted", contentValues2, "last_time_contacted");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "x_times_contacted", contentValues2, "times_contacted");
        contentValues2.remove("last_time_contacted");
        contentValues2.remove("times_contacted");
        return contentValues2;
    }

    private int updateRawContact(SQLiteDatabase sQLiteDatabase, long j, ContentValues contentValues, boolean z, boolean z2) {
        Cursor cursorQuery;
        int i;
        String asString;
        String asString2;
        int i2;
        long j2;
        int i3;
        long j3;
        AbstractContactAggregator abstractContactAggregator;
        long j4;
        int i4;
        int i5;
        AbstractContactAggregator abstractContactAggregator2;
        int i6;
        this.mSelectionArgs1[0] = Long.toString(j);
        ContentValues contentValuesFixUpUsageColumnsForEdit = fixUpUsageColumnsForEdit(contentValues);
        if (contentValuesFixUpUsageColumnsForEdit.size() == 0) {
            return 0;
        }
        ContactsDatabaseHelper contactsDatabaseHelper = this.mDbHelper.get();
        boolean zFlagIsClear = flagIsClear(contentValuesFixUpUsageColumnsForEdit, "deleted");
        boolean zContainsKey = contentValuesFixUpUsageColumnsForEdit.containsKey("account_name");
        boolean zContainsKey2 = contentValuesFixUpUsageColumnsForEdit.containsKey("account_type");
        boolean zContainsKey3 = contentValuesFixUpUsageColumnsForEdit.containsKey("data_set");
        boolean z3 = zContainsKey || zContainsKey2 || zContainsKey3;
        boolean zContainsKey4 = contentValuesFixUpUsageColumnsForEdit.containsKey("backup_id");
        if (zFlagIsClear || z3) {
            cursorQuery = sQLiteDatabase.query("raw_contacts JOIN accounts ON (accounts._id=raw_contacts.account_id)", RawContactsQuery.COLUMNS, "raw_contacts._id = ?", this.mSelectionArgs1, null, null, null);
            try {
                String asString3 = null;
                if (!cursorQuery.moveToFirst()) {
                    i = 1;
                    asString = null;
                    asString2 = null;
                    i2 = 0;
                    j2 = 0;
                } else {
                    i2 = cursorQuery.getInt(0);
                    i = 1;
                    long j5 = cursorQuery.getLong(1);
                    asString = cursorQuery.getString(2);
                    String string = cursorQuery.getString(3);
                    asString2 = cursorQuery.getString(4);
                    j2 = j5;
                    asString3 = string;
                }
                if (z3) {
                    ContentValues contentValues2 = new ContentValues();
                    contentValues2.clear();
                    contentValues2.putAll(contentValuesFixUpUsageColumnsForEdit);
                    if (zContainsKey) {
                        asString3 = contentValues2.getAsString("account_name");
                    }
                    if (zContainsKey2) {
                        asString = contentValues2.getAsString("account_type");
                    }
                    if (zContainsKey3) {
                        asString2 = contentValues2.getAsString("data_set");
                    }
                    long orCreateAccountIdInTransaction = contactsDatabaseHelper.getOrCreateAccountIdInTransaction(AccountWithDataSet.get(asString3, asString, asString2));
                    contentValues2.put("account_id", Long.valueOf(orCreateAccountIdInTransaction));
                    contentValues2.remove("account_name");
                    contentValues2.remove("account_type");
                    contentValues2.remove("data_set");
                    contentValuesFixUpUsageColumnsForEdit = contentValues2;
                    i3 = i2;
                    j3 = orCreateAccountIdInTransaction;
                } else {
                    i3 = i2;
                    j3 = j2;
                }
            } finally {
            }
        } else {
            i = 1;
            i3 = 0;
            j3 = 0;
        }
        if (zFlagIsClear) {
            contentValuesFixUpUsageColumnsForEdit.put("aggregation_mode", (Integer) 0);
        }
        int iUpdate = sQLiteDatabase.update("raw_contacts", contentValuesFixUpUsageColumnsForEdit, "raw_contacts._id = ?", this.mSelectionArgs1);
        if (iUpdate != 0) {
            AbstractContactAggregator abstractContactAggregator3 = this.mAggregator.get();
            int intValue = getIntValue(contentValuesFixUpUsageColumnsForEdit, "aggregation_mode", 0);
            if (intValue != 0) {
                abstractContactAggregator3.markForAggregation(j, intValue, false);
            }
            if (shouldMarkMetadataDirtyForRawContact(contentValuesFixUpUsageColumnsForEdit)) {
                this.mTransactionContext.get().markRawContactMetadataDirty(j, z2);
            }
            if (zContainsKey4) {
                abstractContactAggregator = abstractContactAggregator3;
                j4 = j3;
                i4 = i3;
                cursorQuery = sQLiteDatabase.query("raw_contacts", new String[]{"raw_contacts.metadata_dirty"}, "raw_contacts._id = ?", this.mSelectionArgs1, null, null, null);
                try {
                    if (cursorQuery.moveToFirst()) {
                        i6 = cursorQuery.getInt(0);
                    } else {
                        i6 = 0;
                    }
                    cursorQuery.close();
                    i5 = 1;
                    if (i6 == 1) {
                        this.mTransactionContext.get().markRawContactMetadataDirty(j, z2);
                    } else {
                        this.mTransactionContext.get().markBackupIdChangedRawContact(j);
                    }
                } finally {
                }
            } else {
                abstractContactAggregator = abstractContactAggregator3;
                j4 = j3;
                i4 = i3;
                i5 = i;
            }
            if (flagExists(contentValuesFixUpUsageColumnsForEdit, "starred")) {
                if (!z) {
                    updateFavoritesMembership(j, flagIsSet(contentValuesFixUpUsageColumnsForEdit, "starred"));
                    this.mTransactionContext.get().markRawContactDirtyAndChanged(j, z);
                    this.mSyncToNetwork |= !z;
                }
                abstractContactAggregator2 = abstractContactAggregator;
                abstractContactAggregator2.updateStarred(j);
                abstractContactAggregator2.updatePinned(j);
            } else {
                abstractContactAggregator2 = abstractContactAggregator;
                if (!z && z3) {
                    String[] strArr = new String[i5];
                    strArr[0] = Long.toString(j);
                    updateFavoritesMembership(j, 0 != DatabaseUtils.longForQuery(sQLiteDatabase, "SELECT starred FROM raw_contacts WHERE _id=?", strArr) ? i5 : 0);
                    this.mTransactionContext.get().markRawContactDirtyAndChanged(j, z);
                    this.mSyncToNetwork |= !z;
                }
            }
            if (flagExists(contentValuesFixUpUsageColumnsForEdit, "send_to_voicemail")) {
                abstractContactAggregator2.updateSendToVoicemail(j);
            }
            if (!z && z3) {
                addAutoAddMembership(j);
            }
            if (contentValuesFixUpUsageColumnsForEdit.containsKey("sourceid")) {
                abstractContactAggregator2.updateLookupKeyForRawContact(sQLiteDatabase, j);
            }
            if (zFlagIsClear && i4 == i5) {
                this.mTransactionContext.get().rawContactInserted(j, j4);
            }
            this.mTransactionContext.get().markRawContactChangedOrDeletedOrInserted(j);
        }
        return iUpdate;
    }

    private int updateData(Uri uri, ContentValues contentValues, String str, String[] strArr, boolean z, boolean z2) throws Throwable {
        ContentValues contentValues2 = new ContentValues(contentValues);
        contentValues2.remove("_id");
        contentValues2.remove("raw_contact_id");
        contentValues2.remove("mimetype");
        String asString = contentValues.getAsString("res_package");
        if (asString != null) {
            contentValues2.remove("res_package");
            contentValues2.put("package_id", Long.valueOf(this.mDbHelper.get().getPackageId(asString)));
        }
        int iUpdateData = 0;
        Cursor cursorQueryLocal = queryLocal(uri, DataRowHandler.DataUpdateQuery.COLUMNS, !z ? DatabaseUtils.concatenateWhere(str, "is_read_only=0") : str, strArr, null, -1L, null);
        while (cursorQueryLocal.moveToNext()) {
            try {
                iUpdateData += updateData(contentValues2, cursorQueryLocal, z, z2);
            } finally {
                cursorQueryLocal.close();
            }
        }
        return iUpdateData;
    }

    private void maybeTrimLongPhoneNumber(ContentValues contentValues) {
        String asString = contentValues.getAsString("data1");
        if (asString != null && asString.length() > 1000) {
            contentValues.put("data1", asString.substring(0, 1000));
        }
    }

    private int updateData(ContentValues contentValues, Cursor cursor, boolean z, boolean z2) {
        if (contentValues.size() == 0) {
            return 0;
        }
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        String string = cursor.getString(2);
        if ("vnd.android.cursor.item/phone_v2".equals(string)) {
            maybeTrimLongPhoneNumber(contentValues);
        }
        boolean zUpdate = getDataRowHandler(string).update(writableDatabase, this.mTransactionContext.get(), contentValues, cursor, z, z2);
        if ("vnd.android.cursor.item/photo".equals(string)) {
            scheduleBackgroundTask(10);
        }
        return zUpdate ? 1 : 0;
    }

    private int updateContactOptions(ContentValues contentValues, String str, String[] strArr, boolean z) {
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        Cursor cursorQuery = writableDatabase.query("view_contacts", new String[]{"_id"}, str, strArr, null, null, null);
        int i = 0;
        while (cursorQuery.moveToNext()) {
            try {
                updateContactOptions(writableDatabase, cursorQuery.getLong(0), contentValues, z);
                i++;
            } finally {
                cursorQuery.close();
            }
        }
        return i;
    }

    private int updateContactOptions(SQLiteDatabase sQLiteDatabase, long j, ContentValues contentValues, boolean z) {
        ContentValues contentValuesFixUpUsageColumnsForEdit = fixUpUsageColumnsForEdit(contentValues);
        ContentValues contentValues2 = new ContentValues();
        if (contentValuesFixUpUsageColumnsForEdit.containsKey("filter")) {
            contentValues2.clear();
            ContactsDatabaseHelper.copyStringValue(contentValues2, "filter", contentValuesFixUpUsageColumnsForEdit, "filter");
            int iUpdate = sQLiteDatabase.update("contacts", contentValues2, "_id=" + j, null);
            Log.i("ContactsProvider", "[updateContactOptions]update contact filter column " + iUpdate);
            if (contentValuesFixUpUsageColumnsForEdit.size() == 1) {
                return iUpdate;
            }
        }
        ContactsDatabaseHelper.copyStringValue(contentValues2, "custom_ringtone", contentValuesFixUpUsageColumnsForEdit, "custom_ringtone");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "send_to_voicemail", contentValuesFixUpUsageColumnsForEdit, "send_to_voicemail");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "x_last_time_contacted", contentValuesFixUpUsageColumnsForEdit, "x_last_time_contacted");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "x_times_contacted", contentValuesFixUpUsageColumnsForEdit, "x_times_contacted");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "starred", contentValuesFixUpUsageColumnsForEdit, "starred");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "pinned", contentValuesFixUpUsageColumnsForEdit, "pinned");
        if (contentValues2.size() == 0) {
            return 0;
        }
        boolean zFlagExists = flagExists(contentValues2, "starred");
        boolean zFlagExists2 = flagExists(contentValues2, "pinned");
        boolean zFlagExists3 = flagExists(contentValues2, "send_to_voicemail");
        if (zFlagExists) {
            contentValues2.put("dirty", (Integer) 1);
        }
        if (this.mMetadataSyncEnabled && (zFlagExists || zFlagExists2 || zFlagExists3)) {
            contentValues2.put("metadata_dirty", (Integer) 1);
        }
        this.mSelectionArgs1[0] = String.valueOf(j);
        sQLiteDatabase.update("raw_contacts", contentValues2, "contact_id=? AND raw_contact_is_read_only=0", this.mSelectionArgs1);
        if (!z) {
            Cursor cursorQuery = sQLiteDatabase.query("view_raw_contacts", new String[]{"_id"}, "contact_id=?", this.mSelectionArgs1, null, null, null);
            while (cursorQuery.moveToNext()) {
                try {
                    long j2 = cursorQuery.getLong(0);
                    if (zFlagExists) {
                        updateFavoritesMembership(j2, flagIsSet(contentValues2, "starred"));
                        this.mSyncToNetwork |= !z;
                    }
                    if (zFlagExists || zFlagExists2 || zFlagExists3) {
                        this.mTransactionContext.get().markRawContactMetadataDirty(j2, false);
                    }
                } finally {
                    cursorQuery.close();
                }
            }
        }
        contentValues2.clear();
        ContactsDatabaseHelper.copyStringValue(contentValues2, "custom_ringtone", contentValuesFixUpUsageColumnsForEdit, "custom_ringtone");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "send_to_voicemail", contentValuesFixUpUsageColumnsForEdit, "send_to_voicemail");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "x_last_time_contacted", contentValuesFixUpUsageColumnsForEdit, "x_last_time_contacted");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "x_times_contacted", contentValuesFixUpUsageColumnsForEdit, "x_times_contacted");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "starred", contentValuesFixUpUsageColumnsForEdit, "starred");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "pinned", contentValuesFixUpUsageColumnsForEdit, "pinned");
        contentValues2.put("contact_last_updated_timestamp", Long.valueOf(Clock.getInstance().currentTimeMillis()));
        ContactsDatabaseHelper.copyLongValue(contentValues2, "send_to_voicemail_vt", contentValuesFixUpUsageColumnsForEdit, "send_to_voicemail_vt");
        ContactsDatabaseHelper.copyLongValue(contentValues2, "send_to_voicemail_sip", contentValuesFixUpUsageColumnsForEdit, "send_to_voicemail_sip");
        int iUpdate2 = sQLiteDatabase.update("contacts", contentValues2, "_id=?", this.mSelectionArgs1);
        if (contentValuesFixUpUsageColumnsForEdit.containsKey("x_last_time_contacted") && !contentValuesFixUpUsageColumnsForEdit.containsKey("x_times_contacted")) {
            sQLiteDatabase.execSQL("UPDATE contacts SET x_times_contacted= ifnull(x_times_contacted,0)+1 WHERE _id=?", this.mSelectionArgs1);
            sQLiteDatabase.execSQL("UPDATE raw_contacts SET x_times_contacted= ifnull(x_times_contacted,0)+1  WHERE contact_id=?", this.mSelectionArgs1);
        }
        return iUpdate2;
    }

    private int updateAggregationException(SQLiteDatabase sQLiteDatabase, ContentValues contentValues, boolean z) {
        long jLongValue;
        long jLongValue2;
        Integer asInteger = contentValues.getAsInteger("type");
        Long asLong = contentValues.getAsLong("raw_contact_id1");
        Long asLong2 = contentValues.getAsLong("raw_contact_id2");
        if (asInteger == null || asLong == null || asLong2 == null) {
            return 0;
        }
        if (asLong.longValue() < asLong2.longValue()) {
            jLongValue2 = asLong.longValue();
            jLongValue = asLong2.longValue();
        } else {
            jLongValue = asLong.longValue();
            jLongValue2 = asLong2.longValue();
        }
        if (asInteger.intValue() == 0) {
            this.mSelectionArgs2[0] = String.valueOf(jLongValue2);
            this.mSelectionArgs2[1] = String.valueOf(jLongValue);
            sQLiteDatabase.delete("agg_exceptions", "raw_contact_id1=? AND raw_contact_id2=?", this.mSelectionArgs2);
        } else {
            if (!ContactsProviderUtils.hasPresenceRawContact(sQLiteDatabase, jLongValue2, jLongValue) && (isSimContact(sQLiteDatabase, jLongValue2) || isSimContact(sQLiteDatabase, jLongValue))) {
                return 1;
            }
            ContentValues contentValues2 = new ContentValues(3);
            contentValues2.put("type", asInteger);
            contentValues2.put("raw_contact_id1", Long.valueOf(jLongValue2));
            contentValues2.put("raw_contact_id2", Long.valueOf(jLongValue));
            sQLiteDatabase.replace("agg_exceptions", "_id", contentValues2);
        }
        AbstractContactAggregator abstractContactAggregator = this.mAggregator.get();
        abstractContactAggregator.invalidateAggregationExceptionCache();
        abstractContactAggregator.markForAggregation(jLongValue2, 0, true);
        abstractContactAggregator.markForAggregation(jLongValue, 0, true);
        abstractContactAggregator.aggregateContact(this.mTransactionContext.get(), sQLiteDatabase, jLongValue2);
        abstractContactAggregator.aggregateContact(this.mTransactionContext.get(), sQLiteDatabase, jLongValue);
        this.mTransactionContext.get().markRawContactMetadataDirty(jLongValue2, z);
        this.mTransactionContext.get().markRawContactMetadataDirty(jLongValue, z);
        return 1;
    }

    private boolean shouldMarkMetadataDirtyForRawContact(ContentValues contentValues) {
        return flagExists(contentValues, "starred") || flagExists(contentValues, "pinned") || flagExists(contentValues, "send_to_voicemail");
    }

    @Override
    public void onAccountsUpdated(Account[] accountArr) {
        scheduleBackgroundTask(3);
    }

    public void scheduleRescanDirectories() {
        scheduleBackgroundTask(12);
    }

    private long queryRawContactId(SQLiteDatabase sQLiteDatabase, String str, long j) {
        if (TextUtils.isEmpty(str)) {
            return 0L;
        }
        this.mSelectionArgs2[0] = str;
        this.mSelectionArgs2[1] = String.valueOf(j);
        Cursor cursorQuery = sQLiteDatabase.query("raw_contacts", RawContactsBackupQuery.COLUMNS, "deleted=0 AND backup_id=? AND account_id=?", this.mSelectionArgs2, null, null, null);
        try {
            return cursorQuery.moveToFirst() ? cursorQuery.getLong(0) : 0L;
        } finally {
            cursorQuery.close();
        }
    }

    private ArrayList<Long> queryDataId(SQLiteDatabase sQLiteDatabase, long j, String str) {
        if (j == 0 || TextUtils.isEmpty(str)) {
            return new ArrayList<>();
        }
        this.mSelectionArgs2[0] = String.valueOf(j);
        this.mSelectionArgs2[1] = str;
        ArrayList<Long> arrayList = new ArrayList<>();
        Cursor cursorQuery = sQLiteDatabase.query("data", DataHashQuery.COLUMNS, "raw_contact_id=? AND hash_id=?", this.mSelectionArgs2, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add(Long.valueOf(cursorQuery.getLong(0)));
            } finally {
                cursorQuery.close();
            }
        }
        return arrayList;
    }

    private long searchRawContactIdForRawContactInfo(SQLiteDatabase sQLiteDatabase, MetadataEntryParser.RawContactInfo rawContactInfo) {
        if (rawContactInfo == null) {
            return 0L;
        }
        String str = rawContactInfo.mBackupId;
        String str2 = rawContactInfo.mAccountType;
        String str3 = rawContactInfo.mAccountName;
        String str4 = rawContactInfo.mDataSet;
        ContentValues contentValues = new ContentValues();
        contentValues.put("account_type", str2);
        contentValues.put("account_name", str3);
        if (str4 != null) {
            contentValues.put("data_set", str4);
        }
        return queryRawContactId(sQLiteDatabase, str, replaceAccountInfoByAccountId(ContactsContract.RawContacts.CONTENT_URI, contentValues));
    }

    private Set<Long> queryAggregationRawContactIds(SQLiteDatabase sQLiteDatabase, long j) {
        this.mSelectionArgs2[0] = String.valueOf(j);
        this.mSelectionArgs2[1] = String.valueOf(j);
        ArraySet arraySet = new ArraySet();
        Cursor cursorQuery = sQLiteDatabase.query("agg_exceptions", AggregationExceptionQuery.COLUMNS, "raw_contact_id1=? OR raw_contact_id2=?", this.mSelectionArgs2, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                long j2 = cursorQuery.getLong(0);
                long j3 = cursorQuery.getLong(1);
                if (j2 != j) {
                    arraySet.add(Long.valueOf(j2));
                }
                if (j3 != j) {
                    arraySet.add(Long.valueOf(j3));
                }
            } finally {
                cursorQuery.close();
            }
        }
        return arraySet;
    }

    void updateFromMetaDataEntry(SQLiteDatabase sQLiteDatabase, MetadataEntryParser.MetadataEntry metadataEntry) throws Throwable {
        long jSearchRawContactIdForRawContactInfo = searchRawContactIdForRawContactInfo(sQLiteDatabase, metadataEntry.mRawContactInfo);
        if (jSearchRawContactIdForRawContactInfo == 0) {
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("send_to_voicemail", Integer.valueOf(metadataEntry.mSendToVoicemail));
        contentValues.put("starred", Integer.valueOf(metadataEntry.mStarred));
        contentValues.put("pinned", Integer.valueOf(metadataEntry.mPinned));
        updateRawContact(sQLiteDatabase, jSearchRawContactIdForRawContactInfo, contentValues, true, true);
        for (int i = 0; i < metadataEntry.mFieldDatas.size(); i++) {
            MetadataEntryParser.FieldData fieldData = metadataEntry.mFieldDatas.get(i);
            Iterator<Long> it = queryDataId(sQLiteDatabase, jSearchRawContactIdForRawContactInfo, fieldData.mDataHashId).iterator();
            while (it.hasNext()) {
                long jLongValue = it.next().longValue();
                ContentValues contentValues2 = new ContentValues();
                contentValues2.put("is_primary", Integer.valueOf(fieldData.mIsPrimary ? 1 : 0));
                contentValues2.put("is_super_primary", Integer.valueOf(fieldData.mIsSuperPrimary ? 1 : 0));
                Iterator<Long> it2 = it;
                MetadataEntryParser.FieldData fieldData2 = fieldData;
                long j = jSearchRawContactIdForRawContactInfo;
                updateData(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, jLongValue), contentValues2, null, null, true, true);
                for (int i2 = 0; i2 < fieldData2.mUsageStatsList.size(); i2++) {
                    MetadataEntryParser.UsageStats usageStats = fieldData2.mUsageStatsList.get(i2);
                    int dataUsageFeedbackType = getDataUsageFeedbackType(usageStats.mUsageType.toLowerCase(), null);
                    long j2 = usageStats.mLastTimeUsed;
                    int i3 = usageStats.mTimesUsed;
                    ContentValues contentValues3 = new ContentValues();
                    contentValues3.put("data_id", Long.valueOf(jLongValue));
                    contentValues3.put("usage_type", Integer.valueOf(dataUsageFeedbackType));
                    contentValues3.put("x_last_time_used", Long.valueOf(j2));
                    contentValues3.put("x_times_used", Integer.valueOf(i3));
                    updateDataUsageStats(sQLiteDatabase, contentValues3);
                }
                fieldData = fieldData2;
                it = it2;
                jSearchRawContactIdForRawContactInfo = j;
            }
        }
        long j3 = jSearchRawContactIdForRawContactInfo;
        Integer num = null;
        ArraySet arraySet = new ArraySet();
        int i4 = 0;
        while (i4 < metadataEntry.mAggregationDatas.size()) {
            MetadataEntryParser.AggregationData aggregationData = metadataEntry.mAggregationDatas.get(i4);
            int aggregationType = getAggregationType(aggregationData.mType, num);
            MetadataEntryParser.RawContactInfo rawContactInfo = aggregationData.mRawContactInfo1;
            MetadataEntryParser.RawContactInfo rawContactInfo2 = aggregationData.mRawContactInfo2;
            long jSearchRawContactIdForRawContactInfo2 = searchRawContactIdForRawContactInfo(sQLiteDatabase, rawContactInfo);
            long jSearchRawContactIdForRawContactInfo3 = searchRawContactIdForRawContactInfo(sQLiteDatabase, rawContactInfo2);
            if (jSearchRawContactIdForRawContactInfo2 != 0 && jSearchRawContactIdForRawContactInfo3 != 0) {
                ContentValues contentValues4 = new ContentValues();
                contentValues4.put("raw_contact_id1", Long.valueOf(jSearchRawContactIdForRawContactInfo2));
                contentValues4.put("raw_contact_id2", Long.valueOf(jSearchRawContactIdForRawContactInfo3));
                contentValues4.put("type", Integer.valueOf(aggregationType));
                updateAggregationException(sQLiteDatabase, contentValues4, true);
                if (jSearchRawContactIdForRawContactInfo2 != j3) {
                    arraySet.add(Long.valueOf(jSearchRawContactIdForRawContactInfo2));
                }
                if (jSearchRawContactIdForRawContactInfo3 != j3) {
                    arraySet.add(Long.valueOf(jSearchRawContactIdForRawContactInfo3));
                }
            }
            i4++;
            num = null;
        }
        for (Long l : com.google.common.collect.Sets.difference(queryAggregationRawContactIds(sQLiteDatabase, j3), arraySet)) {
            ContentValues contentValues5 = new ContentValues();
            contentValues5.put("raw_contact_id1", Long.valueOf(j3));
            contentValues5.put("raw_contact_id2", l);
            contentValues5.put("type", (Integer) 0);
            updateAggregationException(sQLiteDatabase, contentValues5, true);
        }
    }

    static String accountsToString(Set<Account> set) {
        StringBuilder sb = new StringBuilder();
        for (Account account : set) {
            if (sb.length() > 0) {
                sb.append("\u0001");
            }
            sb.append(account.name);
            sb.append("\u0002");
            sb.append(account.type);
        }
        return sb.toString();
    }

    static Set<Account> stringToAccounts(String str) {
        HashSet hashSetNewHashSet = Sets.newHashSet();
        if (str.length() == 0) {
            return hashSetNewHashSet;
        }
        try {
            for (String str2 : str.split("\u0001")) {
                String[] strArrSplit = str2.split("\u0002");
                hashSetNewHashSet.add(new Account(strArrSplit[0], strArrSplit[1]));
            }
            return hashSetNewHashSet;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Malformed string", e);
        }
    }

    boolean haveAccountsChanged(Account[] accountArr) {
        try {
            return !stringToAccounts(this.mDbHelper.get().getProperty("known_accounts", "")).equals(Sets.newHashSet(accountArr));
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    void saveAccounts(Account[] accountArr) {
        this.mDbHelper.get().setProperty("known_accounts", accountsToString(Sets.newHashSet(accountArr)));
    }

    private boolean updateAccountsInBackground(Account[] accountArr) {
        Cursor cursorRawQuery;
        if (!haveAccountsChanged(accountArr)) {
            return false;
        }
        if ("1".equals(SystemProperties.get("debug.contacts.ksad"))) {
            Log.w("ContactsProvider", "Accounts changed, but not removing stale data for debug.contacts.ksad");
            return true;
        }
        Log.i("ContactsProvider", "Accounts changed");
        invalidateFastScrollingIndexCache();
        ContactsDatabaseHelper contactsDatabaseHelper = this.mDbHelper.get();
        SQLiteDatabase writableDatabase = contactsDatabaseHelper.getWritableDatabase();
        try {
            writableDatabase.beginTransaction();
            try {
                try {
                    Set<AccountWithDataSet> allAccountsWithDataSets = contactsDatabaseHelper.getAllAccountsWithDataSets();
                    ArrayList arrayListNewArrayList = Lists.newArrayList();
                    for (AccountWithDataSet accountWithDataSet : allAccountsWithDataSets) {
                        if (!accountWithDataSet.isLocalAccount() && canDeleteAccount(accountWithDataSet) && !accountWithDataSet.inSystemAccounts(accountArr)) {
                            arrayListNewArrayList.add(accountWithDataSet);
                        }
                    }
                    if (!arrayListNewArrayList.isEmpty()) {
                        Iterator it = arrayListNewArrayList.iterator();
                        while (it.hasNext()) {
                            Long accountIdOrNull = contactsDatabaseHelper.getAccountIdOrNull((AccountWithDataSet) it.next());
                            if (accountIdOrNull != null) {
                                String[] strArr = {Long.toString(accountIdOrNull.longValue())};
                                cursorRawQuery = writableDatabase.rawQuery("SELECT raw_contacts._id FROM raw_contacts WHERE account_id = ?", strArr);
                                ArrayList arrayList = new ArrayList();
                                if (cursorRawQuery != null) {
                                    while (cursorRawQuery.moveToNext()) {
                                        arrayList.add(Long.valueOf(cursorRawQuery.getLong(0)));
                                    }
                                }
                                writableDatabase.execSQL("DELETE FROM groups WHERE account_id = ?", strArr);
                                writableDatabase.execSQL("DELETE FROM presence WHERE presence_raw_contact_id IN (SELECT _id FROM raw_contacts WHERE account_id = ?)", strArr);
                                writableDatabase.execSQL("DELETE FROM stream_item_photos WHERE stream_item_id IN (SELECT _id FROM stream_items WHERE raw_contact_id IN (SELECT _id FROM raw_contacts WHERE account_id=?))", strArr);
                                writableDatabase.execSQL("DELETE FROM stream_items WHERE raw_contact_id IN (SELECT _id FROM raw_contacts WHERE account_id = ?)", strArr);
                                writableDatabase.execSQL("DELETE FROM metadata_sync WHERE account_id = ?", strArr);
                                writableDatabase.execSQL("DELETE FROM metadata_sync_state WHERE account_id = ?", strArr);
                                if (!inProfileMode()) {
                                    Cursor cursorRawQuery2 = writableDatabase.rawQuery("SELECT raw_contacts.contact_id FROM raw_contacts WHERE account_id = ?1 AND raw_contacts.contact_id IS NOT NULL AND raw_contacts.contact_id NOT IN (    SELECT raw_contacts.contact_id    FROM raw_contacts    WHERE account_id != ?1  AND raw_contacts.contact_id    IS NOT NULL)", strArr);
                                    while (cursorRawQuery2.moveToNext()) {
                                        try {
                                            ContactsTableUtil.deleteContact(writableDatabase, cursorRawQuery2.getLong(0));
                                        } finally {
                                        }
                                    }
                                    MoreCloseables.closeQuietly(cursorRawQuery2);
                                    cursorRawQuery2 = writableDatabase.rawQuery("SELECT DISTINCT raw_contacts.contact_id FROM raw_contacts WHERE account_id = ?1 AND raw_contacts.contact_id IN (    SELECT raw_contacts.contact_id    FROM raw_contacts    WHERE account_id != ?1)", strArr);
                                    while (cursorRawQuery2.moveToNext()) {
                                        try {
                                            ContactsTableUtil.updateContactLastUpdateByContactId(writableDatabase, cursorRawQuery2.getLong(0));
                                        } finally {
                                        }
                                    }
                                    MoreCloseables.closeQuietly(cursorRawQuery2);
                                }
                                writableDatabase.execSQL("DELETE FROM raw_contacts WHERE account_id = ?", strArr);
                                writableDatabase.execSQL("DELETE FROM accounts WHERE _id=?", strArr);
                            }
                        }
                        ArraySet arraySet = new ArraySet();
                        cursorRawQuery = writableDatabase.rawQuery("SELECT _id FROM contacts WHERE (name_raw_contact_id NOT NULL AND name_raw_contact_id NOT IN (SELECT _id FROM raw_contacts)) OR (photo_id NOT NULL AND photo_id NOT IN (SELECT _id FROM data))", null);
                        while (cursorRawQuery.moveToNext()) {
                            try {
                                arraySet.add(Long.valueOf(cursorRawQuery.getLong(0)));
                            } finally {
                            }
                        }
                        cursorRawQuery.close();
                        Iterator it2 = arraySet.iterator();
                        while (it2.hasNext()) {
                            this.mAggregator.get().updateAggregateData(this.mTransactionContext.get(), ((Long) it2.next()).longValue());
                        }
                        contactsDatabaseHelper.updateAllVisible();
                        if (!inProfileMode()) {
                            updateSearchIndexInTransaction();
                        }
                    }
                    removeStaleAccountRows("settings", "account_name", "account_type", accountArr);
                    removeStaleAccountRows("directories", "accountName", "accountType", accountArr);
                    cursorRawQuery = writableDatabase.rawQuery("SELECT account_name,account_type FROM accounts", null);
                    try {
                        StringBuilder sb = new StringBuilder();
                        sb.append("onAccountsUpdated -c.count:");
                        sb.append(cursorRawQuery == null ? 0 : cursorRawQuery.getCount());
                        Log.i("ContactsProvider", sb.toString());
                        if (cursorRawQuery != null && cursorRawQuery.getCount() == 0) {
                            writableDatabase.execSQL("INSERT INTO accounts(account_name,account_type) VALUES(NULL, NULL)");
                        } else if (cursorRawQuery != null && cursorRawQuery.getCount() > 1) {
                            int i = 0;
                            while (cursorRawQuery.moveToNext()) {
                                if (TextUtils.isEmpty(cursorRawQuery.getString(0))) {
                                    i++;
                                }
                            }
                            if (i > 1) {
                                writableDatabase.execSQL("DELETE FROM accounts WHERE account_name IS NULL or account_name ='' ");
                                writableDatabase.execSQL("INSERT INTO accounts(account_name,account_type) VALUES(NULL, NULL)");
                            }
                        }
                        cursorRawQuery.close();
                        contactsDatabaseHelper.getSyncState().onAccountsChanged(writableDatabase, accountArr);
                        saveAccounts(accountArr);
                        writableDatabase.setTransactionSuccessful();
                        this.mAccountWritability.clear();
                        updateContactsAccountCount(accountArr);
                        updateProviderStatus();
                        return true;
                    } finally {
                    }
                } finally {
                    writableDatabase.endTransaction();
                }
            } catch (SQLiteCantOpenDatabaseException e) {
                Log.e("ContactsProvider", "[updateAccountsInBackground]catch SQLiteCantOpenDatabaseException of endTransaction(), return false");
                return false;
            } catch (SQLiteDiskIOException e2) {
                Log.w("ContactsProvider", "[updateAccountsInBackground]catch SQLiteDiskIOException.");
                return false;
            }
        } catch (SQLiteDiskIOException e3) {
            Log.w("ContactsProvider", "[updateAccountsInBackground]catch SQLiteDiskIOException.");
            return false;
        }
    }

    private void updateContactsAccountCount(Account[] accountArr) {
        int i = 0;
        for (Account account : accountArr) {
            if (isContactsAccount(account)) {
                i++;
            }
        }
        this.mContactsAccountCount = i;
    }

    protected boolean isContactsAccount(Account account) {
        try {
            return ContentResolver.getContentService().getIsSyncable(account, "com.android.contacts") > 0;
        } catch (RemoteException e) {
            Log.e("ContactsProvider", "Cannot obtain sync flag for account", e);
            return false;
        }
    }

    public void onPackageChanged(String str) {
        this.mContactDirectoryManager.onPackageChanged(str);
    }

    private void removeStaleAccountRows(String str, String str2, String str3, Account[] accountArr) {
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        Cursor cursorRawQuery = writableDatabase.rawQuery("SELECT DISTINCT " + str2 + "," + str3 + " FROM " + str, null);
        try {
            cursorRawQuery.moveToPosition(-1);
            while (cursorRawQuery.moveToNext()) {
                AccountWithDataSet accountWithDataSet = AccountWithDataSet.get(cursorRawQuery.getString(0), cursorRawQuery.getString(1), null);
                if (!accountWithDataSet.isLocalAccount() && !accountWithDataSet.inSystemAccounts(accountArr)) {
                    writableDatabase.execSQL("DELETE FROM " + str + " WHERE " + str2 + "=? AND " + str3 + "=?", new String[]{accountWithDataSet.getAccountName(), accountWithDataSet.getAccountType()});
                }
            }
        } finally {
            cursorRawQuery.close();
        }
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return query(uri, strArr, str, strArr2, str2, null);
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) {
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "query: uri=" + uri + "  projection=" + Arrays.toString(strArr) + "  selection=[" + str + "]  args=" + Arrays.toString(strArr2) + "  order=[" + str2 + "] CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        this.mContactsHelper.validateProjection(getCallingPackage(), strArr);
        this.mContactsHelper.validateSql(getCallingPackage(), str);
        this.mContactsHelper.validateSql(getCallingPackage(), str2);
        waitForAccess(this.mReadAccessLatch);
        if (!isDirectoryParamValid(uri)) {
            return null;
        }
        if (!isCallerFromSameUser() && !this.mEnterprisePolicyGuard.isCrossProfileAllowed(uri)) {
            return createEmptyCursor(uri, strArr);
        }
        if (mapsToProfileDb(uri)) {
            switchToProfileMode();
            return this.mProfileProvider.query(uri, strArr, str, strArr2, str2, cancellationSignal);
        }
        int callingUid = Binder.getCallingUid();
        this.mStats.incrementQueryStats(callingUid);
        try {
            switchToContactMode();
            return queryDirectoryIfNecessary(uri, strArr, str, strArr2, str2, cancellationSignal);
        } finally {
            this.mStats.finishOperation(callingUid);
        }
    }

    private boolean isCallerFromSameUser() {
        return Binder.getCallingUserHandle().getIdentifier() == UserUtils.getCurrentUserHandle(getContext());
    }

    private Cursor queryDirectoryIfNecessary(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) throws Throwable {
        long j;
        long j2;
        boolean zIsValidEnterpriseUri;
        String queryParameter = getQueryParameter(uri, "directory");
        if (queryParameter == null) {
            j2 = -1;
        } else if (queryParameter.equals("0")) {
            j2 = 0;
        } else {
            if (!queryParameter.equals("1")) {
                j = Long.MIN_VALUE;
                zIsValidEnterpriseUri = this.mEnterprisePolicyGuard.isValidEnterpriseUri(uri);
                if (!zIsValidEnterpriseUri || j > Long.MIN_VALUE) {
                    Cursor cursorQueryLocal = queryLocal(uri, strArr, str, strArr2, str2, j, cancellationSignal);
                    return !zIsValidEnterpriseUri ? cursorQueryLocal : addSnippetExtrasToCursor(uri, cursorQueryLocal);
                }
                return queryDirectoryAuthority(uri, strArr, str, strArr2, str2, queryParameter, cancellationSignal);
            }
            j2 = 1;
        }
        j = j2;
        zIsValidEnterpriseUri = this.mEnterprisePolicyGuard.isValidEnterpriseUri(uri);
        if (!zIsValidEnterpriseUri) {
        }
        Cursor cursorQueryLocal2 = queryLocal(uri, strArr, str, strArr2, str2, j, cancellationSignal);
        if (!zIsValidEnterpriseUri) {
        }
    }

    protected static boolean isDirectoryParamValid(Uri uri) {
        String queryParameter = getQueryParameter(uri, "directory");
        if (queryParameter == null) {
            return true;
        }
        try {
            Long.parseLong(queryParameter);
            return true;
        } catch (NumberFormatException e) {
            Log.e("ContactsProvider", "Invalid directory ID: " + queryParameter);
            return false;
        }
    }

    private static Cursor createEmptyCursor(Uri uri, String[] strArr) {
        if (strArr == null) {
            strArr = getDefaultProjection(uri);
        }
        if (strArr == null) {
            return null;
        }
        return new MatrixCursor(strArr);
    }

    private String getRealCallerPackageName(Uri uri) {
        if (calledByAnotherSelf()) {
            String queryParameter = uri.getQueryParameter("callerPackage");
            if (TextUtils.isEmpty(queryParameter)) {
                Log.wtfStack("ContactsProvider", "Cross-profile query with no callerPackage");
                return "UNKNOWN";
            }
            return queryParameter;
        }
        return getCallingPackage();
    }

    private boolean calledByAnotherSelf() {
        int iMyUid = Process.myUid();
        int callingUid = Binder.getCallingUid();
        return iMyUid != callingUid && UserHandle.isSameApp(iMyUid, callingUid);
    }

    private Cursor queryDirectoryAuthority(Uri uri, String[] strArr, String str, String[] strArr2, String str2, String str3, CancellationSignal cancellationSignal) {
        DirectoryInfo directoryAuthority = getDirectoryAuthority(str3);
        if (directoryAuthority == null) {
            Log.e("ContactsProvider", "Invalid directory ID");
            return null;
        }
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("content");
        builder.authority(directoryAuthority.authority);
        builder.encodedPath(uri.getEncodedPath());
        if (directoryAuthority.accountName != null) {
            builder.appendQueryParameter("account_name", directoryAuthority.accountName);
        }
        if (directoryAuthority.accountType != null) {
            builder.appendQueryParameter("account_type", directoryAuthority.accountType);
        }
        builder.appendQueryParameter("callerPackage", getRealCallerPackageName(uri));
        String limit = getLimit(uri);
        if (limit != null) {
            builder.appendQueryParameter("limit", limit);
        }
        Uri uriBuild = builder.build();
        if (strArr == null) {
            strArr = getDefaultProjection(uri);
        }
        String[] strArr3 = strArr;
        try {
            if (VERBOSE_LOGGING) {
                Log.v("ContactsProvider", "Making directory query: uri=" + uriBuild + "  projection=" + Arrays.toString(strArr3) + "  selection=[" + str + "]  args=" + Arrays.toString(strArr2) + "  order=[" + str2 + "]  Caller=" + getCallingPackage() + "  User=" + UserUtils.getCurrentUserHandle(getContext()));
            }
            Cursor cursorQuery = getContext().getContentResolver().query(uriBuild, strArr3, str, strArr2, str2);
            if (cursorQuery == null) {
                return null;
            }
            try {
                MemoryCursor memoryCursor = new MemoryCursor(null, cursorQuery.getColumnNames());
                memoryCursor.fillFromCursor(cursorQuery);
                return memoryCursor;
            } finally {
                cursorQuery.close();
            }
        } catch (RuntimeException e) {
            Log.w("ContactsProvider", "Directory query failed", e);
            return null;
        }
    }

    protected Cursor queryCorpContactsProvider(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) {
        int corpUserId = UserUtils.getCorpUserId(getContext());
        if (corpUserId < 0) {
            return createEmptyCursor(uri, strArr);
        }
        if (!"com.android.contacts".equals(uri.getAuthority())) {
            Log.w("ContactsProvider", "Invalid authority: " + uri.getAuthority());
            throw new IllegalArgumentException("Authority " + uri.getAuthority() + " is not a valid CP2 authority.");
        }
        Cursor cursorQuery = getContext().getContentResolver().query(maybeAddUserId(uri, corpUserId).buildUpon().appendQueryParameter("callerPackage", getCallingPackage()).build(), strArr, str, strArr2, str2, cancellationSignal);
        if (cursorQuery == null) {
            return createEmptyCursor(uri, strArr);
        }
        return cursorQuery;
    }

    private Cursor addSnippetExtrasToCursor(Uri uri, Cursor cursor) {
        if (cursor.getColumnIndex("snippet") < 0) {
            return cursor;
        }
        String lastPathSegment = uri.getLastPathSegment();
        if ((cursor instanceof AbstractCursor) && deferredSnippetingRequested(uri)) {
            Bundle extras = cursor.getExtras();
            Bundle bundle = new Bundle();
            if (extras != null) {
                bundle.putAll(extras);
            }
            bundle.putString("deferred_snippeting_query", lastPathSegment);
            ((AbstractCursor) cursor).setExtras(bundle);
        }
        return cursor;
    }

    private Cursor addDeferredSnippetingExtra(Cursor cursor) {
        if (cursor instanceof AbstractCursor) {
            Bundle extras = cursor.getExtras();
            Bundle bundle = new Bundle();
            if (extras != null) {
                bundle.putAll(extras);
            }
            bundle.putBoolean("deferred_snippeting", true);
            ((AbstractCursor) cursor).setExtras(bundle);
        }
        return cursor;
    }

    private DirectoryInfo getDirectoryAuthority(String str) {
        DirectoryInfo directoryInfo;
        synchronized (this.mDirectoryCache) {
            if (!this.mDirectoryCacheValid) {
                this.mDirectoryCache.clear();
                Cursor cursorQuery = this.mDbHelper.get().getReadableDatabase().query("directories", DirectoryQuery.COLUMNS, null, null, null, null, null);
                while (cursorQuery.moveToNext()) {
                    try {
                        DirectoryInfo directoryInfo2 = new DirectoryInfo();
                        String string = cursorQuery.getString(0);
                        directoryInfo2.authority = cursorQuery.getString(1);
                        directoryInfo2.accountName = cursorQuery.getString(2);
                        directoryInfo2.accountType = cursorQuery.getString(3);
                        this.mDirectoryCache.put(string, directoryInfo2);
                    } catch (Throwable th) {
                        cursorQuery.close();
                        throw th;
                    }
                }
                cursorQuery.close();
                this.mDirectoryCacheValid = true;
            }
            directoryInfo = this.mDirectoryCache.get(str);
        }
        return directoryInfo;
    }

    public void resetDirectoryCache() {
        synchronized (this.mDirectoryCache) {
            this.mDirectoryCacheValid = false;
        }
    }

    protected android.database.Cursor queryLocal(android.net.Uri r48, java.lang.String[] r49, java.lang.String r50, java.lang.String[] r51, java.lang.String r52, long r53, android.os.CancellationSignal r55) throws java.lang.Throwable {
        throw new UnsupportedOperationException("Method not decompiled: com.android.providers.contacts.ContactsProvider2.queryLocal(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String, long, android.os.CancellationSignal):android.database.Cursor");
    }

    protected static String getLocalizedSortOrder(String str) {
        String strSubstring;
        String strSubstring2;
        if (str != null) {
            int iIndexOf = str.indexOf(32);
            if (iIndexOf == -1) {
                strSubstring = "";
                strSubstring2 = str;
            } else {
                strSubstring2 = str.substring(0, iIndexOf);
                strSubstring = str.substring(iIndexOf);
            }
            if (TextUtils.equals(strSubstring2, "sort_key")) {
                return "phonebook_bucket" + strSubstring + ", " + str;
            }
            if (TextUtils.equals(strSubstring2, "sort_key_alt")) {
                return "phonebook_bucket_alt" + strSubstring + ", " + str;
            }
            return str;
        }
        return str;
    }

    private Cursor doQuery(SQLiteDatabase sQLiteDatabase, SQLiteQueryBuilder sQLiteQueryBuilder, String[] strArr, String str, String[] strArr2, String str2, String str3, String str4, String str5, CancellationSignal cancellationSignal) {
        SQLiteQueryBuilder sQLiteQueryBuilder2;
        if (strArr != null && strArr.length == 1 && "_count".equals(strArr[0])) {
            sQLiteQueryBuilder2 = sQLiteQueryBuilder;
            sQLiteQueryBuilder2.setProjectionMap(sCountProjectionMap);
        } else {
            sQLiteQueryBuilder2 = sQLiteQueryBuilder;
        }
        Cursor cursorQuery = sQLiteQueryBuilder2.query(sQLiteDatabase, strArr, str, strArr2, str3, str4, str2, str5, cancellationSignal);
        if (cursorQuery != null) {
            if (VERBOSE_LOGGING) {
                Log.d("ContactsProvider", "[query] c.count(): " + cursorQuery.getCount());
            }
            cursorQuery.setNotificationUri(getContext().getContentResolver(), ContactsContract.AUTHORITY_URI);
        }
        return cursorQuery;
    }

    private Cursor queryMergedDirectories(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) throws Throwable {
        Cursor cursorQueryCorpContactsProvider;
        Uri uri2 = ContactsContract.Directory.CONTENT_URI;
        Cursor cursorQueryLocal = queryLocal(uri2, strArr, str, strArr2, str2, 0L, cancellationSignal);
        try {
            try {
                cursorQueryCorpContactsProvider = queryCorpContactsProvider(uri2, strArr, str, strArr2, str2, cancellationSignal);
                if (cursorQueryCorpContactsProvider == null) {
                    if (cursorQueryCorpContactsProvider != null) {
                        cursorQueryCorpContactsProvider.close();
                    }
                    return cursorQueryLocal;
                }
                try {
                    MergeCursor mergeCursor = new MergeCursor(new Cursor[]{cursorQueryLocal, rewriteCorpDirectories(cursorQueryCorpContactsProvider)});
                    if (cursorQueryCorpContactsProvider != null) {
                        cursorQueryCorpContactsProvider.close();
                    }
                    return mergeCursor;
                } catch (Throwable th) {
                    th = th;
                    if (cursorQueryCorpContactsProvider != null) {
                        cursorQueryCorpContactsProvider.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                cursorQueryCorpContactsProvider = null;
            }
        } catch (Throwable th3) {
            th = th3;
        }
    }

    private Cursor queryMergedDataPhones(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) throws Throwable {
        long j;
        Cursor cursorQueryCorpContacts;
        List<String> pathSegments = uri.getPathSegments();
        int size = pathSegments.size();
        StringBuilder sb = new StringBuilder(ContactsContract.CommonDataKinds.Phone.CONTENT_URI.getPath());
        for (int i = 2; i < size; i++) {
            sb.append('/');
            sb.append(pathSegments.get(i));
        }
        Uri uriBuild = uri.buildUpon().path(sb.toString()).build();
        String queryParameter = getQueryParameter(uri, "directory");
        if (queryParameter == null) {
            j = -1;
        } else if (queryParameter.equals("0")) {
            j = 0;
        } else {
            j = queryParameter.equals("1") ? 1L : Long.MIN_VALUE;
        }
        Cursor cursorQueryLocal = queryLocal(uriBuild, strArr, str, strArr2, str2, j, null);
        try {
            if (UserUtils.getCorpUserId(getContext()) < 0 || (cursorQueryCorpContacts = queryCorpContacts(uriBuild, strArr, str, strArr2, str2, new String[]{"contact_id"}, null, cancellationSignal)) == null) {
                return cursorQueryLocal;
            }
            return new MergeCursor(new Cursor[]{cursorQueryLocal, cursorQueryCorpContacts});
        } catch (Throwable th) {
            if (cursorQueryLocal != null) {
                cursorQueryLocal.close();
            }
            throw th;
        }
    }

    private static String[] addContactIdColumnIfNotPresent(String[] strArr, String[] strArr2) {
        if (strArr == null) {
            return null;
        }
        int length = strArr.length;
        for (String str : strArr) {
            if (ArrayUtils.contains(strArr2, str)) {
                return strArr;
            }
        }
        String[] strArr3 = new String[length + 1];
        System.arraycopy(strArr, 0, strArr3, 0, length);
        strArr3[strArr.length] = strArr2[0];
        return strArr3;
    }

    private Cursor queryCorpContacts(Uri uri, String[] strArr, String str, String[] strArr2, String str2, String[] strArr3, Long l, CancellationSignal cancellationSignal) {
        String[] strArrAddContactIdColumnIfNotPresent = addContactIdColumnIfNotPresent(strArr, strArr3);
        boolean z = false;
        if (strArr != null && strArrAddContactIdColumnIfNotPresent.length != strArr.length) {
            z = true;
        }
        boolean z2 = z;
        Cursor cursorQueryCorpContactsProvider = queryCorpContactsProvider(uri, strArrAddContactIdColumnIfNotPresent, str, strArr2, str2, cancellationSignal);
        int[] contactIdColumnIndices = getContactIdColumnIndices(cursorQueryCorpContactsProvider, strArr3);
        if (contactIdColumnIndices.length == 0) {
            throw new IllegalStateException("column id is missing in the returned cursor.");
        }
        return new EnterpriseContactsCursorWrapper(cursorQueryCorpContactsProvider, z2 ? removeLastColumn(cursorQueryCorpContactsProvider.getColumnNames()) : cursorQueryCorpContactsProvider.getColumnNames(), contactIdColumnIndices, l);
    }

    private static String[] removeLastColumn(String[] strArr) {
        String[] strArr2 = new String[strArr.length - 1];
        System.arraycopy(strArr, 0, strArr2, 0, strArr2.length);
        return strArr2;
    }

    private Cursor queryCorpLookupIfNecessary(Uri uri, String[] strArr, String str, String[] strArr2, String str2, String[] strArr3, CancellationSignal cancellationSignal) throws Throwable {
        String queryParameter = getQueryParameter(uri, "directory");
        long j = queryParameter != null ? Long.parseLong(queryParameter) : 0L;
        if (ContactsContract.Directory.isEnterpriseDirectoryId(j)) {
            throw new IllegalArgumentException("Directory id must be a current profile id");
        }
        if (ContactsContract.Directory.isRemoteDirectoryId(j)) {
            throw new IllegalArgumentException("Directory id must be a local directory id");
        }
        int corpUserId = UserUtils.getCorpUserId(getContext());
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "queryCorpLookupIfNecessary: local query URI=" + uri);
        }
        Cursor cursorQueryLocal = queryLocal(uri, strArr, str, strArr2, str2, j, null);
        try {
            if (VERBOSE_LOGGING) {
                MoreDatabaseUtils.dumpCursor("ContactsProvider", "local", cursorQueryLocal);
            }
            if (cursorQueryLocal.getCount() > 0 || corpUserId < 0) {
                return cursorQueryLocal;
            }
            try {
                Cursor cursorQueryCorpContacts = queryCorpContacts(uri, strArr, str, strArr2, str2, strArr3, null, cancellationSignal);
                if (cursorQueryCorpContacts != null) {
                    return cursorQueryCorpContacts;
                }
                return cursorQueryLocal;
            } finally {
            }
        } finally {
        }
    }

    private Cursor queryFilterEnterprise(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal, Uri uri2, String str3) {
        String queryParameter = getQueryParameter(uri, "directory");
        if (queryParameter == null) {
            throw new IllegalArgumentException("Directory id missing in URI: " + uri);
        }
        long j = Long.parseLong(queryParameter);
        Uri uriConvertToLocalUri = convertToLocalUri(uri, uri2);
        if (ContactsContract.Directory.isEnterpriseDirectoryId(j)) {
            return queryCorpContacts(uriConvertToLocalUri, strArr, str, strArr2, str2, new String[]{str3}, Long.valueOf(j), cancellationSignal);
        }
        return queryDirectoryIfNecessary(uriConvertToLocalUri, strArr, str, strArr2, str2, cancellationSignal);
    }

    public static Uri convertToLocalUri(Uri uri, Uri uri2) {
        String lastPathSegment;
        if (uri.getPathSegments().size() > uri2.getPathSegments().size()) {
            lastPathSegment = uri.getLastPathSegment();
        } else {
            lastPathSegment = "";
        }
        Uri.Builder builderAppendPath = uri2.buildUpon().appendPath(lastPathSegment);
        addQueryParametersFromUri(builderAppendPath, uri, MODIFIED_KEY_SET_FOR_ENTERPRISE_FILTER);
        String queryParameter = getQueryParameter(uri, "directory");
        if (!TextUtils.isEmpty(queryParameter)) {
            long j = Long.parseLong(queryParameter);
            if (ContactsContract.Directory.isEnterpriseDirectoryId(j)) {
                builderAppendPath.appendQueryParameter("directory", String.valueOf(j - 1000000000));
            } else {
                builderAppendPath.appendQueryParameter("directory", String.valueOf(j));
            }
        }
        return builderAppendPath.build();
    }

    protected static final Uri.Builder addQueryParametersFromUri(Uri.Builder builder, Uri uri, Set<String> set) {
        for (String str : uri.getQueryParameterNames()) {
            if (set == null || !set.contains(str)) {
                builder.appendQueryParameter(str, getQueryParameter(uri, str));
            }
        }
        return builder;
    }

    private Cursor queryPhoneLookupEnterprise(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) {
        return queryLookupEnterprise(uri, strArr, str, strArr2, str2, cancellationSignal, ContactsContract.PhoneLookup.CONTENT_FILTER_URI, uri.getBooleanQueryParameter("sip", false) ? new String[]{"contact_id"} : new String[]{"_id", "contact_id"});
    }

    private Cursor queryEmailsLookupEnterprise(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) {
        return queryLookupEnterprise(uri, strArr, str, strArr2, str2, cancellationSignal, ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI, new String[]{"contact_id"});
    }

    private Cursor queryLookupEnterprise(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal, Uri uri2, String[] strArr3) {
        Uri uriConvertToLocalUri = convertToLocalUri(uri, uri2);
        String queryParameter = getQueryParameter(uri, "directory");
        if (!TextUtils.isEmpty(queryParameter)) {
            long j = Long.parseLong(queryParameter);
            if (ContactsContract.Directory.isEnterpriseDirectoryId(j)) {
                return queryCorpContacts(uriConvertToLocalUri, strArr, str, strArr2, str2, strArr3, Long.valueOf(j), cancellationSignal);
            }
            return queryDirectoryIfNecessary(uriConvertToLocalUri, strArr, str, strArr2, str2, cancellationSignal);
        }
        return queryCorpLookupIfNecessary(uriConvertToLocalUri, strArr, str, strArr2, str2, strArr3, cancellationSignal);
    }

    static Cursor rewriteCorpDirectories(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        String[] columnNames = cursor.getColumnNames();
        MatrixCursor matrixCursor = new MatrixCursor(columnNames);
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
            for (String str : columnNames) {
                int columnIndex = cursor.getColumnIndex(str);
                if (str.equals("_id")) {
                    rowBuilderNewRow.add(Long.valueOf(cursor.getLong(columnIndex) + 1000000000));
                } else {
                    switch (cursor.getType(columnIndex)) {
                        case 0:
                            rowBuilderNewRow.add(null);
                            break;
                        case 1:
                            rowBuilderNewRow.add(Long.valueOf(cursor.getLong(columnIndex)));
                            break;
                        case 2:
                            rowBuilderNewRow.add(Float.valueOf(cursor.getFloat(columnIndex)));
                            break;
                        case 3:
                            rowBuilderNewRow.add(cursor.getString(columnIndex));
                            break;
                        case 4:
                            rowBuilderNewRow.add(cursor.getBlob(columnIndex));
                            break;
                    }
                }
            }
        }
        return matrixCursor;
    }

    private static int[] getContactIdColumnIndices(Cursor cursor, String[] strArr) {
        ArrayList arrayList = new ArrayList();
        if (cursor != null) {
            for (String str : strArr) {
                int columnIndex = cursor.getColumnIndex(str);
                if (columnIndex != -1) {
                    arrayList.add(Integer.valueOf(columnIndex));
                }
            }
        }
        return Ints.toArray(arrayList);
    }

    private Cursor queryWithContactIdAndLookupKey(SQLiteQueryBuilder sQLiteQueryBuilder, SQLiteDatabase sQLiteDatabase, String[] strArr, String str, String[] strArr2, String str2, String str3, String str4, String str5, long j, String str6, String str7, CancellationSignal cancellationSignal) {
        String[] strArr3;
        if (strArr2 == null) {
            strArr3 = new String[2];
        } else {
            String[] strArr4 = new String[strArr2.length + 2];
            System.arraycopy(strArr2, 0, strArr4, 2, strArr2.length);
            strArr3 = strArr4;
        }
        strArr3[0] = String.valueOf(j);
        strArr3[1] = Uri.encode(str7);
        sQLiteQueryBuilder.appendWhere(str5 + "=? AND " + str6 + "=?");
        Cursor cursorDoQuery = doQuery(sQLiteDatabase, sQLiteQueryBuilder, strArr, str, strArr3, str2, str3, null, str4, cancellationSignal);
        if (cursorDoQuery.getCount() != 0) {
            return cursorDoQuery;
        }
        cursorDoQuery.close();
        return null;
    }

    private void invalidateFastScrollingIndexCache() {
        this.mFastScrollingIndexCache.invalidate();
    }

    private void bundleFastScrollingIndexExtras(Cursor cursor, Uri uri, SQLiteDatabase sQLiteDatabase, SQLiteQueryBuilder sQLiteQueryBuilder, String str, String[] strArr, String str2, String str3, CancellationSignal cancellationSignal) {
        Bundle fastScrollingIndexExtras;
        if (!(cursor instanceof AbstractCursor)) {
            Log.w("ContactsProvider", "Unable to bundle extras.  Cursor is not AbstractCursor.");
            return;
        }
        synchronized (this.mFastScrollingIndexCache) {
            this.mFastScrollingIndexCacheRequestCount++;
            Bundle bundle = this.mFastScrollingIndexCache.get(uri, str, strArr, str2, str3);
            if (!checkFastScrollingIndexCacheSanity(bundle, cursor)) {
                this.mFastScrollingIndexCache.invalidate();
                bundle = null;
            }
            if (bundle == null) {
                this.mFastScrollingIndexCacheMissCount++;
                long jCurrentTimeMillis = System.currentTimeMillis();
                fastScrollingIndexExtras = getFastScrollingIndexExtras(sQLiteDatabase, sQLiteQueryBuilder, str, strArr, str2, str3, cancellationSignal);
                int iCurrentTimeMillis = (int) (System.currentTimeMillis() - jCurrentTimeMillis);
                this.mTotalTimeFastScrollingIndexGenerate += (long) iCurrentTimeMillis;
                if (VERBOSE_LOGGING) {
                    Log.v("ContactsProvider", "getLetterCountExtraBundle took " + iCurrentTimeMillis + "ms");
                }
                this.mFastScrollingIndexCache.put(uri, str, strArr, str2, str3, fastScrollingIndexExtras);
            } else {
                fastScrollingIndexExtras = bundle;
            }
        }
        ((AbstractCursor) cursor).setExtras(fastScrollingIndexExtras);
    }

    private boolean checkFastScrollingIndexCacheSanity(Bundle bundle, Cursor cursor) {
        if (bundle == null) {
            return true;
        }
        int i = 0;
        for (int i2 : bundle.getIntArray("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS")) {
            i += i2;
        }
        return i == cursor.getCount();
    }

    private static Bundle getFastScrollingIndexExtras(SQLiteDatabase sQLiteDatabase, SQLiteQueryBuilder sQLiteQueryBuilder, String str, String[] strArr, String str2, String str3, CancellationSignal cancellationSignal) {
        String str4;
        String str5;
        String str6;
        String str7 = str2;
        String strSubstring = "";
        if (str7 != null) {
            int iIndexOf = str7.indexOf(32);
            if (iIndexOf != -1) {
                String strSubstring2 = str7.substring(0, iIndexOf);
                strSubstring = str7.substring(iIndexOf);
                str7 = strSubstring2;
            }
        } else {
            str7 = "sort_key";
        }
        if (TextUtils.equals(str7, "sort_key")) {
            str4 = "phonebook_bucket";
            str5 = "phonebook_label";
        } else if (TextUtils.equals(str7, "sort_key_alt")) {
            str4 = "phonebook_bucket_alt";
            str5 = "phonebook_label_alt";
        } else {
            return null;
        }
        ArrayMap arrayMap = new ArrayMap();
        arrayMap.put("name", str7 + " AS name");
        arrayMap.put("bucket", str4 + " AS bucket");
        arrayMap.put("label", str5 + " AS label");
        if (TextUtils.isEmpty(str3)) {
            str6 = "*";
        } else {
            str6 = str3;
        }
        arrayMap.put("count", "COUNT(" + str6 + ") AS count");
        sQLiteQueryBuilder.setProjectionMap(arrayMap);
        Cursor cursorQuery = sQLiteQueryBuilder.query(sQLiteDatabase, AddressBookIndexQuery.COLUMNS, str, strArr, "bucket, label", null, "bucket" + strSubstring + ", name COLLATE PHONEBOOK" + strSubstring, null, cancellationSignal);
        try {
            int count = cursorQuery.getCount();
            String[] strArr2 = new String[count];
            int[] iArr = new int[count];
            for (int i = 0; i < count; i++) {
                cursorQuery.moveToNext();
                strArr2[i] = cursorQuery.getString(2);
                iArr[i] = cursorQuery.getInt(3);
            }
            return FastScrollingIndexCache.buildExtraBundle(strArr2, iArr);
        } finally {
            cursorQuery.close();
        }
    }

    public long lookupContactIdByLookupKey(SQLiteDatabase sQLiteDatabase, String str) {
        long jLookupContactIdByRawContactIds;
        ArrayList<ContactLookupKey.LookupKeySegment> arrayList = new ContactLookupKey().parse(str);
        if (lookupKeyContainsType(arrayList, 3)) {
            jLookupContactIdByRawContactIds = lookupSingleContactId(sQLiteDatabase);
        } else {
            jLookupContactIdByRawContactIds = -1;
        }
        if (lookupKeyContainsType(arrayList, 0)) {
            jLookupContactIdByRawContactIds = lookupContactIdBySourceIds(sQLiteDatabase, arrayList);
            if (jLookupContactIdByRawContactIds != -1) {
                return jLookupContactIdByRawContactIds;
            }
        }
        boolean zLookupKeyContainsType = lookupKeyContainsType(arrayList, 2);
        if (zLookupKeyContainsType) {
            jLookupContactIdByRawContactIds = lookupContactIdByRawContactIds(sQLiteDatabase, arrayList);
            if (jLookupContactIdByRawContactIds != -1) {
                return jLookupContactIdByRawContactIds;
            }
        }
        if (zLookupKeyContainsType || lookupKeyContainsType(arrayList, 1)) {
            return lookupContactIdByDisplayNames(sQLiteDatabase, arrayList);
        }
        return jLookupContactIdByRawContactIds;
    }

    private long lookupSingleContactId(SQLiteDatabase sQLiteDatabase) {
        Cursor cursorQuery = sQLiteDatabase.query("contacts", new String[]{"_id"}, null, null, null, null, null, "1");
        try {
            if (cursorQuery.moveToFirst()) {
                return cursorQuery.getLong(0);
            }
            return -1L;
        } finally {
            cursorQuery.close();
        }
    }

    private long lookupContactIdBySourceIds(SQLiteDatabase sQLiteDatabase, ArrayList<ContactLookupKey.LookupKeySegment> arrayList) {
        StringBuilder sb = new StringBuilder();
        sb.append("sourceid IN (");
        for (ContactLookupKey.LookupKeySegment lookupKeySegment : arrayList) {
            if (lookupKeySegment.lookupType == 0) {
                DatabaseUtils.appendEscapedSQLString(sb, lookupKeySegment.key);
                sb.append(",");
            }
        }
        sb.setLength(sb.length() - 1);
        sb.append(") AND contact_id NOT NULL");
        Cursor cursorQuery = sQLiteDatabase.query("view_raw_contacts", LookupBySourceIdQuery.COLUMNS, sb.toString(), null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                int accountHashCode = ContactLookupKey.getAccountHashCode(cursorQuery.getString(1), cursorQuery.getString(2));
                String string = cursorQuery.getString(3);
                int i = 0;
                while (true) {
                    if (i < arrayList.size()) {
                        ContactLookupKey.LookupKeySegment lookupKeySegment2 = arrayList.get(i);
                        if (lookupKeySegment2.lookupType != 0 || accountHashCode != lookupKeySegment2.accountHashCode || !lookupKeySegment2.key.equals(string)) {
                            i++;
                        } else {
                            lookupKeySegment2.contactId = cursorQuery.getLong(0);
                            break;
                        }
                    }
                }
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return getMostReferencedContactId(arrayList);
    }

    private long lookupContactIdByRawContactIds(SQLiteDatabase sQLiteDatabase, ArrayList<ContactLookupKey.LookupKeySegment> arrayList) {
        StringBuilder sb = new StringBuilder();
        sb.append("_id IN (");
        for (ContactLookupKey.LookupKeySegment lookupKeySegment : arrayList) {
            if (lookupKeySegment.lookupType == 2) {
                sb.append(lookupKeySegment.rawContactId);
                sb.append(",");
            }
        }
        sb.setLength(sb.length() - 1);
        sb.append(") AND contact_id NOT NULL");
        Cursor cursorQuery = sQLiteDatabase.query("view_raw_contacts", LookupByRawContactIdQuery.COLUMNS, sb.toString(), null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                int accountHashCode = ContactLookupKey.getAccountHashCode(cursorQuery.getString(1), cursorQuery.getString(2));
                String string = cursorQuery.getString(3);
                Iterator<ContactLookupKey.LookupKeySegment> it = arrayList.iterator();
                while (true) {
                    if (it.hasNext()) {
                        ContactLookupKey.LookupKeySegment next = it.next();
                        if (next.lookupType == 2 && accountHashCode == next.accountHashCode && next.rawContactId.equals(string)) {
                            next.contactId = cursorQuery.getLong(0);
                            break;
                        }
                    }
                }
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return getMostReferencedContactId(arrayList);
    }

    private long lookupContactIdByDisplayNames(SQLiteDatabase sQLiteDatabase, ArrayList<ContactLookupKey.LookupKeySegment> arrayList) {
        StringBuilder sb = new StringBuilder();
        sb.append("normalized_name IN (");
        for (ContactLookupKey.LookupKeySegment lookupKeySegment : arrayList) {
            if (lookupKeySegment.lookupType == 1 || lookupKeySegment.lookupType == 2) {
                DatabaseUtils.appendEscapedSQLString(sb, lookupKeySegment.key);
                sb.append(",");
            }
        }
        sb.setLength(sb.length() - 1);
        sb.append(") AND name_type=2 AND contact_id NOT NULL");
        Cursor cursorQuery = sQLiteDatabase.query("name_lookup INNER JOIN view_raw_contacts ON (name_lookup.raw_contact_id = view_raw_contacts._id)", LookupByDisplayNameQuery.COLUMNS, sb.toString(), null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                int accountHashCode = ContactLookupKey.getAccountHashCode(cursorQuery.getString(1), cursorQuery.getString(2));
                String string = cursorQuery.getString(3);
                Iterator<ContactLookupKey.LookupKeySegment> it = arrayList.iterator();
                while (true) {
                    if (it.hasNext()) {
                        ContactLookupKey.LookupKeySegment next = it.next();
                        if (next.lookupType == 1 || next.lookupType == 2) {
                            if (accountHashCode == next.accountHashCode && next.key.equals(string)) {
                                next.contactId = cursorQuery.getLong(0);
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return getMostReferencedContactId(arrayList);
    }

    private boolean lookupKeyContainsType(ArrayList<ContactLookupKey.LookupKeySegment> arrayList, int i) {
        Iterator<ContactLookupKey.LookupKeySegment> it = arrayList.iterator();
        while (it.hasNext()) {
            if (it.next().lookupType == i) {
                return true;
            }
        }
        return false;
    }

    private long getMostReferencedContactId(ArrayList<ContactLookupKey.LookupKeySegment> arrayList) {
        Collections.sort(arrayList);
        int i = 0;
        int i2 = 0;
        long j = -1;
        long j2 = -1;
        for (ContactLookupKey.LookupKeySegment lookupKeySegment : arrayList) {
            if (lookupKeySegment.contactId != -1) {
                if (lookupKeySegment.contactId == j) {
                    i++;
                } else {
                    if (i <= i2) {
                        i = i2;
                        j = j2;
                    }
                    i2 = i;
                    i = 1;
                    long j3 = j;
                    j = lookupKeySegment.contactId;
                    j2 = j3;
                }
            }
        }
        if (i > i2) {
            return j;
        }
        return j2;
    }

    private void setTablesAndProjectionMapForContacts(SQLiteQueryBuilder sQLiteQueryBuilder, String[] strArr) {
        setTablesAndProjectionMapForContacts(sQLiteQueryBuilder, strArr, false);
    }

    private void setTablesAndProjectionMapForContacts(SQLiteQueryBuilder sQLiteQueryBuilder, String[] strArr, boolean z) {
        StringBuilder sb = new StringBuilder();
        if (z) {
            sb.append("view_data_usage_stat AS data_usage_stat");
            sb.append(" INNER JOIN ");
        }
        sb.append("view_contacts");
        if (z) {
            sb.append(" ON (" + DbQueryUtils.concatenateClauses("data_usage_stat.x_times_used > 0", "contact_id=view_contacts._id") + ")");
        }
        appendContactPresenceJoin(sb, strArr, "_id");
        appendContactStatusUpdateJoin(sb, strArr, "status_update_id");
        sQLiteQueryBuilder.setTables(sb.toString());
        sQLiteQueryBuilder.setProjectionMap(sContactsProjectionMap);
    }

    private void setTablesAndProjectionMapForContactsWithSnippet(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String[] strArr, String str, long j, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("view_contacts");
        if (str != null) {
            str = str.trim();
        }
        String str2 = str;
        if (TextUtils.isEmpty(str2) || (j != -1 && j != 0)) {
            sb.append(" JOIN (SELECT NULL AS snippet WHERE 0)");
        } else {
            appendSearchIndexJoin(sb, uri, strArr, str2, z);
        }
        appendContactPresenceJoin(sb, strArr, "_id");
        appendContactStatusUpdateJoin(sb, strArr, "status_update_id");
        sQLiteQueryBuilder.setTables(sb.toString());
        sQLiteQueryBuilder.setProjectionMap(sContactsProjectionWithSnippetMap);
    }

    private void appendSearchIndexJoin(StringBuilder sb, Uri uri, String[] strArr, String str, boolean z) {
        if (snippetNeeded(strArr)) {
            String[] strArrSplit = null;
            String queryParameter = getQueryParameter(uri, "snippet_args");
            if (queryParameter != null) {
                strArrSplit = queryParameter.split(",");
            }
            appendSearchIndexJoin(sb, str, true, (strArrSplit == null || strArrSplit.length <= 0) ? "[" : strArrSplit[0], (strArrSplit == null || strArrSplit.length <= 1) ? "]" : strArrSplit[1], (strArrSplit == null || strArrSplit.length <= 2) ? "…" : strArrSplit[2], (strArrSplit == null || strArrSplit.length <= 3) ? 5 : Integer.parseInt(strArrSplit[3]), z);
            return;
        }
        appendSearchIndexJoin(sb, str, false, null, null, null, 0, false);
    }

    public void appendSearchIndexJoin(StringBuilder sb, String str, boolean z, String str2, String str3, String str4, int i, boolean z2) {
        boolean z3;
        String str5;
        String numberToE164;
        String str6;
        boolean z4;
        String str7;
        if (str.indexOf(64) != -1) {
            String strExtractAddressFromEmailAddress = this.mDbHelper.get().extractAddressFromEmailAddress(str);
            z3 = !TextUtils.isEmpty(strExtractAddressFromEmailAddress);
            str6 = strExtractAddressFromEmailAddress;
            str5 = null;
            numberToE164 = null;
            z4 = false;
        } else {
            boolean zIsPhoneNumber = isPhoneNumber(str);
            if (zIsPhoneNumber) {
                String strNormalizeNumber = PhoneNumberUtils.normalizeNumber(str);
                str6 = null;
                numberToE164 = PhoneNumberUtils.formatNumberToE164(strNormalizeNumber, this.mDbHelper.get().getCurrentCountryIso());
                z4 = zIsPhoneNumber;
                str5 = strNormalizeNumber;
                z3 = false;
            } else {
                z3 = false;
                str5 = null;
                numberToE164 = null;
                str6 = null;
                z4 = zIsPhoneNumber;
            }
        }
        sb.append(" JOIN (SELECT contact_id AS snippet_contact_id");
        if (z) {
            sb.append(", ");
            if (z3) {
                sb.append("ifnull(");
                if (!z2) {
                    DatabaseUtils.appendEscapedSQLString(sb, str2);
                    sb.append("||");
                }
                sb.append("(SELECT MIN(data1)");
                sb.append(" FROM data JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id)");
                sb.append(" WHERE  search_index.contact_id");
                sb.append("=contact_id AND data1 LIKE ");
                DatabaseUtils.appendEscapedSQLString(sb, str + "%");
                sb.append(")");
                if (!z2) {
                    sb.append("||");
                    DatabaseUtils.appendEscapedSQLString(sb, str3);
                }
                sb.append(",");
                if (z2) {
                    sb.append("content");
                } else {
                    appendSnippetFunction(sb, str2, str3, str4, i);
                }
                sb.append(")");
            } else if (z4) {
                sb.append("ifnull(");
                if (!z2) {
                    DatabaseUtils.appendEscapedSQLString(sb, str2);
                    sb.append("||");
                }
                sb.append("(SELECT MIN(data1)");
                sb.append(" FROM data JOIN raw_contacts ON (data.raw_contact_id = raw_contacts._id) JOIN phone_lookup");
                sb.append(" ON data._id");
                sb.append("=phone_lookup.data_id");
                sb.append(" WHERE  search_index.contact_id");
                sb.append("=contact_id");
                sb.append(" AND normalized_number LIKE '");
                sb.append(str5);
                sb.append("%'");
                if (!TextUtils.isEmpty(numberToE164)) {
                    sb.append(" OR normalized_number LIKE '");
                    sb.append(numberToE164);
                    sb.append("%'");
                }
                sb.append(")");
                if (!z2) {
                    sb.append("||");
                    DatabaseUtils.appendEscapedSQLString(sb, str3);
                }
                sb.append(",");
                if (z2) {
                    sb.append("content");
                } else {
                    appendSnippetFunction(sb, str2, str3, str4, i);
                }
                sb.append(")");
            } else {
                String strNormalize = NameNormalizer.normalize(str);
                if (!TextUtils.isEmpty(strNormalize)) {
                    if (z2) {
                        sb.append("content");
                    } else {
                        sb.append("(CASE WHEN EXISTS (SELECT 1 FROM ");
                        sb.append("raw_contacts AS rc INNER JOIN ");
                        sb.append("name_lookup AS nl ON (rc._id");
                        sb.append("=nl.raw_contact_id");
                        sb.append(") WHERE nl.normalized_name");
                        sb.append(" GLOB '" + strNormalize + "*' AND ");
                        sb.append("nl.name_type=");
                        sb.append("2 AND ");
                        sb.append("search_index.contact_id");
                        sb.append("=rc.contact_id");
                        sb.append(") THEN NULL ELSE ");
                        appendSnippetFunction(sb, str2, str3, str4, i);
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
        if (z3) {
            String strSanitizeMatch = str6 == null ? "" : sanitizeMatch(str6);
            sb.append("\"");
            sb.append(strSanitizeMatch);
            sb.append("*\"");
        } else if (z4) {
            String str8 = " OR tokens:" + str5 + "*";
            if (numberToE164 != null && !TextUtils.equals(numberToE164, str5)) {
                str7 = " OR tokens:" + numberToE164 + "*";
            } else {
                str7 = "";
            }
            sb.append(SearchIndexManager.getFtsMatchQuery(str, SearchIndexManager.FtsQueryBuilder.getDigitsQueryBuilder(str8 + str7)));
        } else {
            sb.append(SearchIndexManager.getFtsMatchQuery(str, SearchIndexManager.FtsQueryBuilder.SCOPED_NAME_NORMALIZING));
        }
        sb.append("' AND snippet_contact_id IN default_directory)");
        sb.append(" ON (_id=snippet_contact_id)");
    }

    private static String sanitizeMatch(String str) {
        return str.replace("'", "").replace("*", "").replace("-", "").replace("\"", "");
    }

    private void appendSnippetFunction(StringBuilder sb, String str, String str2, String str3, int i) {
        sb.append("snippet(search_index,");
        DatabaseUtils.appendEscapedSQLString(sb, str);
        sb.append(",");
        DatabaseUtils.appendEscapedSQLString(sb, str2);
        sb.append(",");
        DatabaseUtils.appendEscapedSQLString(sb, str3);
        sb.append(",1,");
        sb.append(i);
        sb.append(")");
    }

    private void setTablesAndProjectionMapForRawContacts(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri) {
        sQLiteQueryBuilder.setTables("view_raw_contacts");
        sQLiteQueryBuilder.setProjectionMap(sRawContactsProjectionMap);
        appendAccountIdFromParameter(sQLiteQueryBuilder, uri);
    }

    private void setTablesAndProjectionMapForRawEntities(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri) {
        sQLiteQueryBuilder.setTables("view_raw_entities");
        sQLiteQueryBuilder.setProjectionMap(sRawEntityProjectionMap);
        appendAccountIdFromParameter(sQLiteQueryBuilder, uri);
    }

    private void setTablesAndProjectionMapForData(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String[] strArr, boolean z) {
        setTablesAndProjectionMapForData(sQLiteQueryBuilder, uri, strArr, z, false, null);
    }

    private void setTablesAndProjectionMapForData(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String[] strArr, boolean z, boolean z2) {
        setTablesAndProjectionMapForData(sQLiteQueryBuilder, uri, strArr, z, z2, null);
    }

    private void setTablesAndProjectionMapForData(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String[] strArr, boolean z, Integer num) {
        setTablesAndProjectionMapForData(sQLiteQueryBuilder, uri, strArr, z, false, num);
    }

    private void setTablesAndProjectionMapForData(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String[] strArr, boolean z, boolean z2, Integer num) {
        ProjectionMap projectionMap;
        StringBuilder sb = new StringBuilder();
        sb.append("view_data");
        sb.append(" data");
        appendContactPresenceJoin(sb, strArr, "contact_id");
        appendContactStatusUpdateJoin(sb, strArr, "status_update_id");
        appendDataPresenceJoin(sb, strArr, "data._id");
        appendDataStatusUpdateJoin(sb, strArr, "data._id");
        appendDataUsageStatJoin(sb, num == null ? -1 : num.intValue(), "data._id");
        sQLiteQueryBuilder.setTables(sb.toString());
        boolean z3 = z || !ContactsDatabaseHelper.isInProjection(strArr, DISTINCT_DATA_PROHIBITING_COLUMNS);
        sQLiteQueryBuilder.setDistinct(z3);
        if (z2) {
            projectionMap = z3 ? sDistinctDataSipLookupProjectionMap : sDataSipLookupProjectionMap;
        } else {
            projectionMap = z3 ? sDistinctDataProjectionMap : sDataProjectionMap;
        }
        sQLiteQueryBuilder.setProjectionMap(projectionMap);
        appendAccountIdFromParameter(sQLiteQueryBuilder, uri);
    }

    private void setTableAndProjectionMapForStatusUpdates(SQLiteQueryBuilder sQLiteQueryBuilder, String[] strArr) {
        StringBuilder sb = new StringBuilder();
        sb.append("view_data");
        sb.append(" data");
        appendDataPresenceJoin(sb, strArr, "data._id");
        appendDataStatusUpdateJoin(sb, strArr, "data._id");
        sQLiteQueryBuilder.setTables(sb.toString());
        sQLiteQueryBuilder.setProjectionMap(sStatusUpdatesProjectionMap);
    }

    private void setTablesAndProjectionMapForStreamItems(SQLiteQueryBuilder sQLiteQueryBuilder) {
        sQLiteQueryBuilder.setTables("view_stream_items");
        sQLiteQueryBuilder.setProjectionMap(sStreamItemsProjectionMap);
    }

    private void setTablesAndProjectionMapForStreamItemPhotos(SQLiteQueryBuilder sQLiteQueryBuilder) {
        sQLiteQueryBuilder.setTables("photo_files JOIN stream_item_photos ON (stream_item_photos.photo_file_id=photo_files._id) JOIN stream_items ON (stream_item_photos.stream_item_id=stream_items._id) JOIN raw_contacts ON (stream_items.raw_contact_id=raw_contacts._id)");
        sQLiteQueryBuilder.setProjectionMap(sStreamItemPhotosProjectionMap);
    }

    private void setTablesAndProjectionMapForEntities(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String[] strArr) {
        StringBuilder sb = new StringBuilder();
        sb.append("view_entities");
        sb.append(" data");
        appendContactPresenceJoin(sb, strArr, "contact_id");
        appendContactStatusUpdateJoin(sb, strArr, "status_update_id");
        appendDataPresenceJoin(sb, strArr, "data_id");
        appendDataStatusUpdateJoin(sb, strArr, "data_id");
        appendDataUsageStatJoin(sb, -1, "data_id");
        sQLiteQueryBuilder.setTables(sb.toString());
        sQLiteQueryBuilder.setProjectionMap(sEntityProjectionMap);
        appendAccountIdFromParameter(sQLiteQueryBuilder, uri);
    }

    private void appendContactStatusUpdateJoin(StringBuilder sb, String[] strArr, String str) {
        if (ContactsDatabaseHelper.isInProjection(strArr, "contact_status", "contact_status_res_package", "contact_status_icon", "contact_status_label", "contact_status_ts")) {
            sb.append(" LEFT OUTER JOIN status_updates contacts_status_updates ON (" + str + "=contacts_status_updates.status_update_data_id)");
        }
    }

    private void appendDataStatusUpdateJoin(StringBuilder sb, String[] strArr, String str) {
        if (ContactsDatabaseHelper.isInProjection(strArr, "status", "status_res_package", "status_icon", "status_label", "status_ts")) {
            sb.append(" LEFT OUTER JOIN status_updates ON (status_updates.status_update_data_id=" + str + ")");
        }
    }

    private void appendDataUsageStatJoin(StringBuilder sb, int i, String str) {
        if (i != -1) {
            sb.append(" LEFT OUTER JOIN view_data_usage as data_usage_stat ON (data_usage_stat.data_id=");
            sb.append(str);
            sb.append(" AND data_usage_stat.usage_type=");
            sb.append(i);
            sb.append(")");
            return;
        }
        sb.append(" LEFT OUTER JOIN (SELECT data_id as STAT_DATA_ID, SUM(ifnull(x_times_used,0)) as x_times_used,  MAX(ifnull(x_last_time_used,0)) as x_last_time_used, SUM(ifnull(times_used,0)) as times_used,  MAX(ifnull(last_time_used,0)) as last_time_used FROM view_data_usage GROUP BY data_id) as data_usage_stat");
        sb.append(" ON (STAT_DATA_ID=");
        sb.append(str);
        sb.append(")");
    }

    private void appendContactPresenceJoin(StringBuilder sb, String[] strArr, String str) {
        if (ContactsDatabaseHelper.isInProjection(strArr, "contact_presence", "contact_chat_capability")) {
            sb.append(" LEFT OUTER JOIN agg_presence ON (" + str + " = agg_presence.presence_contact_id)");
        }
    }

    private void appendDataPresenceJoin(StringBuilder sb, String[] strArr, String str) {
        if (ContactsDatabaseHelper.isInProjection(strArr, "mode", "chat_capability")) {
            sb.append(" LEFT OUTER JOIN presence ON (presence_data_id=" + str + ")");
        }
    }

    private void appendLocalDirectoryAndAccountSelectionIfNeeded(SQLiteQueryBuilder sQLiteQueryBuilder, long j, Uri uri) {
        StringBuilder sb = new StringBuilder();
        if (j == 0) {
            sb.append("(_id IN default_directory)");
        } else if (j == 1) {
            sb.append("(_id NOT IN default_directory)");
        } else {
            sb.append("(1)");
        }
        AccountWithDataSet accountWithDataSetFromUri = getAccountWithDataSetFromUri(uri);
        if (!TextUtils.isEmpty(accountWithDataSetFromUri.getAccountName())) {
            Long accountIdOrNull = this.mDbHelper.get().getAccountIdOrNull(accountWithDataSetFromUri);
            if (accountIdOrNull == null) {
                sb.setLength(0);
                sb.append("(1=2)");
            } else {
                sb.append(" AND (_id IN (SELECT contact_id FROM raw_contacts WHERE account_id=" + accountIdOrNull.toString() + "))");
            }
        }
        sQLiteQueryBuilder.appendWhere(sb.toString());
    }

    private void appendAccountFromParameter(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri) {
        String str;
        AccountWithDataSet accountWithDataSetFromUri = getAccountWithDataSetFromUri(uri);
        if (!TextUtils.isEmpty(accountWithDataSetFromUri.getAccountName())) {
            String str2 = "(account_name=" + DatabaseUtils.sqlEscapeString(accountWithDataSetFromUri.getAccountName()) + " AND account_type=" + DatabaseUtils.sqlEscapeString(accountWithDataSetFromUri.getAccountType());
            if (accountWithDataSetFromUri.getDataSet() == null) {
                str = str2 + " AND data_set IS NULL";
            } else {
                str = str2 + " AND data_set=" + DatabaseUtils.sqlEscapeString(accountWithDataSetFromUri.getDataSet());
            }
            sQLiteQueryBuilder.appendWhere(str + ")");
            return;
        }
        sQLiteQueryBuilder.appendWhere("1");
    }

    private void appendAccountIdFromParameter(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri) {
        AccountWithDataSet accountWithDataSetFromUri = getAccountWithDataSetFromUri(uri);
        if (!TextUtils.isEmpty(accountWithDataSetFromUri.getAccountName())) {
            Long accountIdOrNull = this.mDbHelper.get().getAccountIdOrNull(accountWithDataSetFromUri);
            if (accountIdOrNull == null) {
                sQLiteQueryBuilder.appendWhere("(1=2)");
                return;
            }
            sQLiteQueryBuilder.appendWhere("(account_id=" + accountIdOrNull.toString() + ")");
            return;
        }
        sQLiteQueryBuilder.appendWhere("1");
    }

    private AccountWithDataSet getAccountWithDataSetFromUri(Uri uri) {
        String queryParameter = getQueryParameter(uri, "account_name");
        String queryParameter2 = getQueryParameter(uri, "account_type");
        String queryParameter3 = getQueryParameter(uri, "data_set");
        if (TextUtils.isEmpty(queryParameter) ^ TextUtils.isEmpty(queryParameter2)) {
            throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Must specify both or neither of ACCOUNT_NAME and ACCOUNT_TYPE", uri));
        }
        return AccountWithDataSet.get(queryParameter, queryParameter2, queryParameter3);
    }

    private String appendAccountToSelection(Uri uri, String str) {
        AccountWithDataSet accountWithDataSetFromUri = getAccountWithDataSetFromUri(uri);
        if (!TextUtils.isEmpty(accountWithDataSetFromUri.getAccountName())) {
            StringBuilder sb = new StringBuilder("account_name=");
            sb.append(DatabaseUtils.sqlEscapeString(accountWithDataSetFromUri.getAccountName()));
            sb.append(" AND account_type=");
            sb.append(DatabaseUtils.sqlEscapeString(accountWithDataSetFromUri.getAccountType()));
            if (accountWithDataSetFromUri.getDataSet() == null) {
                sb.append(" AND data_set IS NULL");
            } else {
                sb.append(" AND data_set=");
                sb.append(DatabaseUtils.sqlEscapeString(accountWithDataSetFromUri.getDataSet()));
            }
            if (!TextUtils.isEmpty(str)) {
                sb.append(" AND (");
                sb.append(str);
                sb.append(')');
            }
            return sb.toString();
        }
        return str;
    }

    private String appendAccountIdToSelection(Uri uri, String str) {
        AccountWithDataSet accountWithDataSetFromUri = getAccountWithDataSetFromUri(uri);
        if (!TextUtils.isEmpty(accountWithDataSetFromUri.getAccountName())) {
            StringBuilder sb = new StringBuilder();
            Long accountIdOrNull = this.mDbHelper.get().getAccountIdOrNull(accountWithDataSetFromUri);
            if (accountIdOrNull == null) {
                sb.append("(1=2)");
            } else {
                sb.append("account_id=");
                sb.append(Long.toString(accountIdOrNull.longValue()));
            }
            if (!TextUtils.isEmpty(str)) {
                sb.append(" AND (");
                sb.append(str);
                sb.append(')');
            }
            return sb.toString();
        }
        return str;
    }

    static String getLimit(Uri uri) {
        String queryParameter = getQueryParameter(uri, "limit");
        if (queryParameter == null) {
            return null;
        }
        try {
            int i = Integer.parseInt(queryParameter);
            if (i < 0) {
                Log.w("ContactsProvider", "Invalid limit parameter: " + queryParameter);
                return null;
            }
            return String.valueOf(i);
        } catch (NumberFormatException e) {
            Log.w("ContactsProvider", "Invalid limit parameter: " + queryParameter);
            return null;
        }
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String str) throws FileNotFoundException {
        AssetFileDescriptor assetFileDescriptorOpenAssetFileLocal;
        try {
            if (!isDirectoryParamValid(uri)) {
                if (VERBOSE_LOGGING) {
                    Log.v("ContactsProvider", "openAssetFile uri=" + uri + " mode=" + str + " success=false CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
                }
                return null;
            }
            if (!isCallerFromSameUser() && !this.mEnterprisePolicyGuard.isCrossProfileAllowed(uri)) {
                if (VERBOSE_LOGGING) {
                    Log.v("ContactsProvider", "openAssetFile uri=" + uri + " mode=" + str + " success=false CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
                }
                return null;
            }
            waitForAccess(str.equals("r") ? this.mReadAccessLatch : this.mWriteAccessLatch);
            if (mapsToProfileDb(uri)) {
                switchToProfileMode();
                assetFileDescriptorOpenAssetFileLocal = this.mProfileProvider.openAssetFile(uri, str);
            } else {
                switchToContactMode();
                assetFileDescriptorOpenAssetFileLocal = openAssetFileLocal(uri, str);
            }
            if (VERBOSE_LOGGING) {
                Log.v("ContactsProvider", "openAssetFile uri=" + uri + " mode=" + str + " success=true CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
            }
            return assetFileDescriptorOpenAssetFileLocal;
        } catch (Throwable th) {
            if (VERBOSE_LOGGING) {
                Log.v("ContactsProvider", "openAssetFile uri=" + uri + " mode=" + str + " success=false CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
            }
            throw th;
        }
    }

    public AssetFileDescriptor openAssetFileLocal(Uri uri, String str) throws FileNotFoundException {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return openAssetFileInner(uri, str);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private AssetFileDescriptor openAssetFileInner(Uri uri, String str) throws Throwable {
        Cursor cursorQuery;
        long j;
        long j2;
        Cursor cursorQuery2;
        String[] strArr;
        String str2;
        char c;
        int i;
        SQLiteDatabase sQLiteDatabase;
        SQLiteDatabase database = this.mDbHelper.get().getDatabase(str.contains("w"));
        int iMatch = sUriMatcher.match(uri);
        if (iMatch == 2006) {
            long j3 = Long.parseLong(uri.getPathSegments().get(1));
            boolean z = !str.equals("r");
            SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
            String[] strArr2 = {"_id", "data14"};
            setTablesAndProjectionMapForData(sQLiteQueryBuilder, uri, strArr2, false);
            cursorQuery = sQLiteQueryBuilder.query(database, strArr2, "raw_contact_id=? AND mimetype_id=?", new String[]{String.valueOf(j3), String.valueOf(this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/photo"))}, null, null, "is_primary DESC");
            try {
                if (cursorQuery.getCount() >= 1) {
                    cursorQuery.moveToFirst();
                    j2 = cursorQuery.getLong(0);
                    j = cursorQuery.getLong(1);
                } else {
                    j = 0;
                    j2 = 0;
                }
                if (z) {
                    return openDisplayPhotoForWrite(j3, j2, uri, str);
                }
                return openDisplayPhotoForRead(j);
            } finally {
            }
        }
        if (iMatch == 3001) {
            return openPhotoAssetFile(database, uri, str, "_id=? AND mimetype_id=" + this.mDbHelper.get().getMimeTypeId("vnd.android.cursor.item/photo"), new String[]{String.valueOf(Long.parseLong(uri.getPathSegments().get(1)))});
        }
        if (iMatch == 19004) {
            if (!str.equals("r")) {
                throw new IllegalArgumentException("Write is not supported.");
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            outputRawContactsAsVCard(uri, byteArrayOutputStream, null, null);
            return buildAssetFileDescriptor(byteArrayOutputStream);
        }
        if (iMatch == 19012) {
            if (!str.equals("r")) {
                throw new IllegalArgumentException("Display photos retrieved by contact ID can only be read.");
            }
            cursorQuery2 = database.query("contacts", new String[]{"photo_file_id"}, null, null, null, null, null);
            try {
                if (cursorQuery2.moveToFirst()) {
                    return openDisplayPhotoForRead(cursorQuery2.getLong(0));
                }
                throw new FileNotFoundException(uri.toString());
            } finally {
            }
        }
        if (iMatch == 22000) {
            long id = ContentUris.parseId(uri);
            if (!str.equals("r")) {
                throw new IllegalArgumentException("Display photos retrieved by key can only be read.");
            }
            return openDisplayPhotoForRead(id);
        }
        if (iMatch == 24000) {
            return openDirectoryFileEnterprise(uri, str);
        }
        switch (iMatch) {
            case 1009:
                return openPhotoAssetFile(database, uri, str, "_id=photo_id AND contact_id=?", new String[]{String.valueOf(Long.parseLong(uri.getPathSegments().get(1)))});
            case 1010:
            case 1011:
            case 1013:
            case 1014:
                if (!str.equals("r")) {
                    throw new IllegalArgumentException("Photos retrieved by contact lookup key can only be read.");
                }
                List<String> pathSegments = uri.getPathSegments();
                int size = pathSegments.size();
                if (size < 4) {
                    throw new IllegalArgumentException(this.mDbHelper.get().exceptionMessage("Missing a lookup key", uri));
                }
                boolean z2 = iMatch == 1014 || iMatch == 1013;
                String str3 = pathSegments.get(2);
                String[] strArr3 = {"photo_id", "photo_file_id"};
                if (size == 5) {
                    long j4 = Long.parseLong(pathSegments.get(3));
                    SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
                    setTablesAndProjectionMapForContacts(sQLiteQueryBuilder2, strArr3);
                    strArr = strArr3;
                    str2 = str3;
                    sQLiteDatabase = database;
                    Cursor cursorQueryWithContactIdAndLookupKey = queryWithContactIdAndLookupKey(sQLiteQueryBuilder2, database, strArr3, null, null, null, null, null, "_id", j4, "lookup", str2, null);
                    if (cursorQueryWithContactIdAndLookupKey != null) {
                        try {
                            cursorQueryWithContactIdAndLookupKey.moveToFirst();
                            if (z2) {
                                return openDisplayPhotoForRead(cursorQueryWithContactIdAndLookupKey.getLong(cursorQueryWithContactIdAndLookupKey.getColumnIndex("photo_file_id")));
                            }
                            return openPhotoAssetFile(sQLiteDatabase, uri, str, "_id=?", new String[]{String.valueOf(cursorQueryWithContactIdAndLookupKey.getLong(cursorQueryWithContactIdAndLookupKey.getColumnIndex("photo_id")))});
                        } finally {
                            cursorQueryWithContactIdAndLookupKey.close();
                        }
                    }
                    c = 0;
                    i = 1;
                } else {
                    strArr = strArr3;
                    str2 = str3;
                    c = 0;
                    i = 1;
                    sQLiteDatabase = database;
                }
                SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
                String[] strArr4 = strArr;
                setTablesAndProjectionMapForContacts(sQLiteQueryBuilder3, strArr4);
                SQLiteDatabase sQLiteDatabase2 = sQLiteDatabase;
                String[] strArr5 = new String[i];
                strArr5[c] = String.valueOf(lookupContactIdByLookupKey(sQLiteDatabase2, str2));
                cursorQuery = sQLiteQueryBuilder3.query(sQLiteDatabase2, strArr4, "_id=?", strArr5, null, null, null);
                try {
                    cursorQuery.moveToFirst();
                    if (z2) {
                        return openDisplayPhotoForRead(cursorQuery.getLong(cursorQuery.getColumnIndex("photo_file_id")));
                    }
                    String[] strArr6 = new String[i];
                    strArr6[c] = String.valueOf(cursorQuery.getLong(cursorQuery.getColumnIndex("photo_id")));
                    return openPhotoAssetFile(sQLiteDatabase2, uri, str, "_id=?", strArr6);
                } finally {
                }
            case 1012:
                if (!str.equals("r")) {
                    throw new IllegalArgumentException("Display photos retrieved by contact ID can only be read.");
                }
                cursorQuery2 = database.query("contacts", new String[]{"photo_file_id"}, "_id=?", new String[]{String.valueOf(Long.parseLong(uri.getPathSegments().get(1)))}, null, null, null);
                try {
                    if (cursorQuery2.moveToFirst()) {
                        return openDisplayPhotoForRead(cursorQuery2.getLong(0));
                    }
                    throw new FileNotFoundException(uri.toString());
                } finally {
                }
            case 1015:
                if (!str.equals("r")) {
                    throw new IllegalArgumentException("Write is not supported.");
                }
                ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
                outputRawContactsAsVCard(uri, byteArrayOutputStream2, null, null);
                return buildAssetFileDescriptor(byteArrayOutputStream2);
            case 1016:
                if (!str.equals("r")) {
                    throw new IllegalArgumentException("Write is not supported.");
                }
                String[] strArrSplit = uri.getPathSegments().get(2).split(":");
                StringBuilder sb = new StringBuilder();
                Uri uriBuild = ContactsContract.Contacts.CONTENT_URI;
                if (uri.getBooleanQueryParameter("no_photo", false)) {
                    uriBuild = ContactsContract.Contacts.CONTENT_URI.buildUpon().appendQueryParameter("no_photo", "true").build();
                }
                int i2 = 0;
                for (String str4 : strArrSplit) {
                    String strDecode = Uri.decode(str4);
                    sb.append(i2 == 0 ? "(" : ",");
                    sb.append(lookupContactIdByLookupKey(database, strDecode));
                    i2++;
                }
                sb.append(')');
                String str5 = "_id IN " + sb.toString();
                ByteArrayOutputStream byteArrayOutputStream3 = new ByteArrayOutputStream();
                outputRawContactsAsVCard(uriBuild, byteArrayOutputStream3, str5, null);
                return buildAssetFileDescriptor(byteArrayOutputStream3);
            default:
                switch (iMatch) {
                    case 1027:
                        return openCorpContactPicture(Long.parseLong(uri.getPathSegments().get(1)), uri, str, false);
                    case 1028:
                        return openCorpContactPicture(Long.parseLong(uri.getPathSegments().get(1)), uri, str, true);
                    default:
                        throw new FileNotFoundException(this.mDbHelper.get().exceptionMessage("Stream I/O not supported on this URI.", uri));
                }
        }
    }

    private AssetFileDescriptor openDirectoryFileEnterprise(Uri uri, String str) throws FileNotFoundException {
        Uri uriBuild;
        String queryParameter = getQueryParameter(uri, "directory");
        if (queryParameter == null) {
            throw new IllegalArgumentException("Directory id missing in URI: " + uri);
        }
        long j = Long.parseLong(queryParameter);
        if (!ContactsContract.Directory.isRemoteDirectoryId(j)) {
            throw new IllegalArgumentException("Directory is not a remote directory: " + uri);
        }
        if (ContactsContract.Directory.isEnterpriseDirectoryId(j)) {
            int corpUserId = UserUtils.getCorpUserId(getContext());
            if (corpUserId < 0) {
                throw new FileNotFoundException(uri.toString());
            }
            Uri.Builder builderBuildUpon = ContactsContract.AUTHORITY_URI.buildUpon();
            builderBuildUpon.encodedPath(uri.getEncodedPath());
            builderBuildUpon.appendQueryParameter("directory", String.valueOf(j - 1000000000));
            addQueryParametersFromUri(builderBuildUpon, uri, MODIFIED_KEY_SET_FOR_ENTERPRISE_FILTER);
            uriBuild = maybeAddUserId(builderBuildUpon.build(), corpUserId);
        } else {
            DirectoryInfo directoryAuthority = getDirectoryAuthority(queryParameter);
            if (directoryAuthority == null) {
                Log.e("ContactsProvider", "Invalid directory ID: " + uri);
                return null;
            }
            Uri uri2 = Uri.parse(uri.getLastPathSegment());
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("content");
            builder.authority(directoryAuthority.authority);
            builder.encodedPath(uri2.getEncodedPath());
            addQueryParametersFromUri(builder, uri2, null);
            uriBuild = builder.build();
        }
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "openDirectoryFileEnterprise: " + uriBuild);
        }
        return getContext().getContentResolver().openAssetFileDescriptor(uriBuild, str);
    }

    private AssetFileDescriptor openCorpContactPicture(long j, Uri uri, String str, boolean z) throws FileNotFoundException {
        if (!str.equals("r")) {
            throw new IllegalArgumentException("Photos retrieved by contact ID can only be read.");
        }
        int corpUserId = UserUtils.getCorpUserId(getContext());
        if (corpUserId < 0) {
            throw new FileNotFoundException(uri.toString());
        }
        return getContext().getContentResolver().openAssetFileDescriptor(maybeAddUserId(ContentUris.appendId(ContactsContract.Contacts.CONTENT_URI.buildUpon(), j).appendPath(z ? "display_photo" : "photo").build(), corpUserId), str);
    }

    private AssetFileDescriptor openPhotoAssetFile(SQLiteDatabase sQLiteDatabase, Uri uri, String str, String str2, String[] strArr) throws FileNotFoundException {
        if (!"r".equals(str)) {
            throw new FileNotFoundException(this.mDbHelper.get().exceptionMessage("Mode " + str + " not supported.", uri));
        }
        try {
            return makeAssetFileDescriptor(DatabaseUtils.blobFileDescriptorForQuery(sQLiteDatabase, "SELECT data15 FROM view_data WHERE " + str2, strArr));
        } catch (SQLiteDoneException e) {
            throw new FileNotFoundException(uri.toString());
        }
    }

    private AssetFileDescriptor openDisplayPhotoForRead(long j) throws FileNotFoundException {
        PhotoStore.Entry entry = this.mPhotoStore.get().get(j);
        if (entry != null) {
            try {
                return makeAssetFileDescriptor(ParcelFileDescriptor.open(new File(entry.path), 268435456), entry.size);
            } catch (FileNotFoundException e) {
                scheduleBackgroundTask(10);
                throw e;
            }
        }
        scheduleBackgroundTask(10);
        throw new FileNotFoundException("No photo file found for ID " + j);
    }

    private AssetFileDescriptor openDisplayPhotoForWrite(long j, long j2, Uri uri, String str) {
        try {
            ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
            new PipeMonitor(j, j2, parcelFileDescriptorArrCreatePipe[0]).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[]) null);
            return new AssetFileDescriptor(parcelFileDescriptorArrCreatePipe[1], 0L, -1L);
        } catch (IOException e) {
            Log.e("ContactsProvider", "Could not create temp image file in mode " + str);
            return null;
        }
    }

    private class PipeMonitor extends AsyncTask<Object, Object, Object> {
        private final long mDataId;
        private final ParcelFileDescriptor mDescriptor;
        private final long mRawContactId;

        private PipeMonitor(long j, long j2, ParcelFileDescriptor parcelFileDescriptor) {
            this.mRawContactId = j;
            this.mDataId = j2;
            this.mDescriptor = parcelFileDescriptor;
        }

        @Override
        protected Object doInBackground(Object... objArr) {
            ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(this.mDescriptor);
            try {
                try {
                    Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(autoCloseInputStream);
                    if (bitmapDecodeStream != null) {
                        ContactsProvider2.this.waitForAccess(ContactsProvider2.this.mWriteAccessLatch);
                        PhotoProcessor photoProcessor = new PhotoProcessor(bitmapDecodeStream, ContactsProvider2.this.getMaxDisplayPhotoDim(), ContactsProvider2.this.getMaxThumbnailDim());
                        long jInsert = (ContactsContract.isProfileId(this.mRawContactId) ? ContactsProvider2.this.mProfilePhotoStore : ContactsProvider2.this.mContactsPhotoStore).insert(photoProcessor);
                        if (this.mDataId != 0) {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("skip_processing", (Boolean) true);
                            if (jInsert != 0) {
                                contentValues.put("data14", Long.valueOf(jInsert));
                            }
                            contentValues.put("data15", photoProcessor.getThumbnailPhotoBytes());
                            ContactsProvider2.this.update(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, this.mDataId), contentValues, null, null);
                        } else {
                            ContentValues contentValues2 = new ContentValues();
                            contentValues2.put("skip_processing", (Boolean) true);
                            contentValues2.put("mimetype", "vnd.android.cursor.item/photo");
                            contentValues2.put("is_primary", (Integer) 1);
                            if (jInsert != 0) {
                                contentValues2.put("data14", Long.valueOf(jInsert));
                            }
                            contentValues2.put("data15", photoProcessor.getThumbnailPhotoBytes());
                            ContactsProvider2.this.insert(ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendPath(String.valueOf(this.mRawContactId)).appendPath("data").build(), contentValues2);
                        }
                    }
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                IoUtils.closeQuietly(autoCloseInputStream);
            }
        }
    }

    private AssetFileDescriptor buildAssetFileDescriptor(final ByteArrayOutputStream byteArrayOutputStream) {
        try {
            byteArrayOutputStream.flush();
            ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
            final FileDescriptor fileDescriptor = parcelFileDescriptorArrCreatePipe[1].getFileDescriptor();
            new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object... objArr) {
                    FileOutputStream fileOutputStream;
                    Throwable th;
                    Throwable th2;
                    try {
                        fileOutputStream = new FileOutputStream(fileDescriptor);
                    } catch (IOException | RuntimeException e) {
                        Log.w("ContactsProvider", "Failure closing pipe", e);
                    }
                    try {
                        fileOutputStream.write(byteArrayOutputStream.toByteArray());
                        fileOutputStream.close();
                        IoUtils.closeQuietly(fileDescriptor);
                        return null;
                    } catch (Throwable th3) {
                        try {
                            throw th3;
                        } catch (Throwable th4) {
                            th = th3;
                            th2 = th4;
                            if (th != null) {
                                fileOutputStream.close();
                                throw th2;
                            }
                            try {
                                fileOutputStream.close();
                                throw th2;
                            } catch (Throwable th5) {
                                th.addSuppressed(th5);
                                throw th2;
                            }
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[]) null);
            return makeAssetFileDescriptor(parcelFileDescriptorArrCreatePipe[0]);
        } catch (IOException e) {
            Log.w("ContactsProvider", "Problem writing stream into an ParcelFileDescriptor: " + e.toString());
            return null;
        }
    }

    private AssetFileDescriptor makeAssetFileDescriptor(ParcelFileDescriptor parcelFileDescriptor) {
        return makeAssetFileDescriptor(parcelFileDescriptor, -1L);
    }

    private AssetFileDescriptor makeAssetFileDescriptor(ParcelFileDescriptor parcelFileDescriptor, long j) {
        if (parcelFileDescriptor != null) {
            return new AssetFileDescriptor(parcelFileDescriptor, 0L, j);
        }
        return null;
    }

    private void outputRawContactsAsVCard(Uri uri, OutputStream outputStream, String str, String[] strArr) throws Throwable {
        String str2;
        StringBuilder sb;
        BufferedWriter bufferedWriter;
        Context context = getContext();
        int i = VCardConfig.VCARD_TYPE_DEFAULT;
        if (uri.getBooleanQueryParameter("no_photo", false)) {
            i |= 8388608;
        }
        VCardComposer vCardComposer = new VCardComposer(context, i, false);
        BufferedWriter bufferedWriter2 = null;
        Uri uriPreAuthorizeUri = mapsToProfileDb(uri) ? preAuthorizeUri(ContactsContract.RawContactsEntity.PROFILE_CONTENT_URI) : ContactsContract.RawContactsEntity.CONTENT_URI;
        try {
            try {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
        } catch (IOException e2) {
            e = e2;
            bufferedWriter2 = bufferedWriter;
            Log.e("ContactsProvider", "IOException: " + e);
            vCardComposer.terminate();
            if (bufferedWriter2 != null) {
                try {
                    bufferedWriter2.close();
                } catch (IOException e3) {
                    e = e3;
                    str2 = "ContactsProvider";
                    sb = new StringBuilder();
                    sb.append("IOException during closing output stream: ");
                    sb.append(e);
                    Log.w(str2, sb.toString());
                }
            }
        } catch (Throwable th2) {
            th = th2;
            bufferedWriter2 = bufferedWriter;
            vCardComposer.terminate();
            if (bufferedWriter2 != null) {
                try {
                    bufferedWriter2.close();
                } catch (IOException e4) {
                    Log.w("ContactsProvider", "IOException during closing output stream: " + e4);
                }
            }
            throw th;
        }
        if (!vCardComposer.init(uri, str, strArr, null, uriPreAuthorizeUri)) {
            Log.w("ContactsProvider", "Failed to init VCardComposer");
            vCardComposer.terminate();
            try {
                bufferedWriter.close();
                return;
            } catch (IOException e5) {
                Log.w("ContactsProvider", "IOException during closing output stream: " + e5);
                return;
            }
        }
        while (!vCardComposer.isAfterLast()) {
            bufferedWriter.write(vCardComposer.createOneEntry());
        }
        vCardComposer.terminate();
        try {
            bufferedWriter.close();
        } catch (IOException e6) {
            e = e6;
            str2 = "ContactsProvider";
            sb = new StringBuilder();
            sb.append("IOException during closing output stream: ");
            sb.append(e);
            Log.w(str2, sb.toString());
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
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
            case 3016:
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
            case 16001:
                return "vnd.android.cursor.dir/provider_status";
            case 17001:
            case 17003:
                return "vnd.android.cursor.dir/contact_directories";
            case 17002:
            case 17004:
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

    private static String[] getDefaultProjection(Uri uri) {
        int iMatch = sUriMatcher.match(uri);
        switch (iMatch) {
            case 1000:
            case 1001:
            case 1002:
            case 1003:
                break;
            default:
                switch (iMatch) {
                    default:
                        switch (iMatch) {
                            default:
                                switch (iMatch) {
                                    case 3001:
                                    case 3002:
                                    case 3003:
                                    case 3005:
                                    case 3006:
                                    case 3007:
                                    case 3009:
                                    case 3010:
                                        return sDataProjectionMap.getColumnNames();
                                    case 3004:
                                    case 3008:
                                        return sDistinctDataProjectionMap.getColumnNames();
                                    default:
                                        switch (iMatch) {
                                            case 3016:
                                            case 3017:
                                                break;
                                            case 3018:
                                            case 3019:
                                            case 3020:
                                                break;
                                            default:
                                                switch (iMatch) {
                                                    case 4000:
                                                    case 4001:
                                                        return sPhoneLookupProjectionMap.getColumnNames();
                                                    default:
                                                        switch (iMatch) {
                                                            case 6000:
                                                            case 6001:
                                                                return sAggregationExceptionsProjectionMap.getColumnNames();
                                                            default:
                                                                switch (iMatch) {
                                                                    case 15001:
                                                                    case 15002:
                                                                        return sRawEntityProjectionMap.getColumnNames();
                                                                    default:
                                                                        switch (iMatch) {
                                                                            case 17001:
                                                                            case 17002:
                                                                            case 17003:
                                                                            case 17004:
                                                                                return sDirectoryProjectionMap.getColumnNames();
                                                                            default:
                                                                                switch (iMatch) {
                                                                                    case 19000:
                                                                                        break;
                                                                                    case 19001:
                                                                                        return sEntityProjectionMap.getColumnNames();
                                                                                    case 19002:
                                                                                        break;
                                                                                    default:
                                                                                        switch (iMatch) {
                                                                                            case 19004:
                                                                                                break;
                                                                                            case 19005:
                                                                                            case 19006:
                                                                                                break;
                                                                                            default:
                                                                                                switch (iMatch) {
                                                                                                    case 1019:
                                                                                                        break;
                                                                                                    case 1029:
                                                                                                        return sContactsProjectionWithSnippetMap.getColumnNames();
                                                                                                    case 3013:
                                                                                                        break;
                                                                                                    case 8000:
                                                                                                        break;
                                                                                                    case 9000:
                                                                                                        return sSettingsProjectionMap.getColumnNames();
                                                                                                    default:
                                                                                                        return null;
                                                                                                }
                                                                                                break;
                                                                                        }
                                                                                        break;
                                                                                }
                                                                                break;
                                                                        }
                                                                        break;
                                                                }
                                                                break;
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                        break;
                                }
                            case 2002:
                            case 2003:
                                return sRawContactsProjectionMap.getColumnNames();
                        }
                    case 1015:
                    case 1016:
                        return sContactsVCardProjectionMap.getColumnNames();
                }
                break;
        }
        return sContactsProjectionMap.getColumnNames();
    }

    private class StructuredNameLookupBuilder extends NameLookupBuilder {
        public StructuredNameLookupBuilder(NameSplitter nameSplitter) {
            super(nameSplitter);
        }

        @Override
        protected void insertNameLookup(long j, long j2, int i, String str) {
            ((ContactsDatabaseHelper) ContactsProvider2.this.mDbHelper.get()).insertNameLookup(j, j2, i, str);
        }

        @Override
        protected String[] getCommonNicknameClusters(String str) {
            return ContactsProvider2.this.mCommonNicknameCache.getCommonNicknameClusters(str);
        }
    }

    public void appendContactFilterAsNestedQuery(StringBuilder sb, String str) {
        sb.append("(SELECT DISTINCT contact_id FROM raw_contacts JOIN name_lookup ON(raw_contacts._id=raw_contact_id) WHERE normalized_name GLOB '");
        sb.append(NameNormalizer.normalize(str));
        sb.append("*' AND name_type IN(2,4,3))");
    }

    private boolean isPhoneNumber(String str) {
        return !TextUtils.isEmpty(str) && countPhoneNumberDigits(str) > 0;
    }

    public static int countPhoneNumberDigits(String str) {
        int length = str.length();
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            if (Character.isDigit(cCharAt)) {
                i++;
            } else if (cCharAt != '*' && cCharAt != '#' && cCharAt != 'N' && cCharAt != '.' && cCharAt != ';' && cCharAt != '-' && cCharAt != '(' && cCharAt != ')' && cCharAt != ' ' && (cCharAt != '+' || i != 0)) {
                return 0;
            }
        }
        return i;
    }

    private Cursor completeName(Uri uri, String[] strArr) {
        if (strArr == null) {
            strArr = sDataProjectionMap.getColumnNames();
        }
        ContentValues contentValues = new ContentValues();
        DataRowHandlerForStructuredName dataRowHandlerForStructuredName = (DataRowHandlerForStructuredName) getDataRowHandler("vnd.android.cursor.item/name");
        copyQueryParamsToContentValues(contentValues, uri, "data1", "data4", "data2", "data5", "data3", "data6", "phonetic_name", "data9", "data8", "data7");
        dataRowHandlerForStructuredName.fixStructuredNameComponents(contentValues, contentValues);
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        Object[] objArr = new Object[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            objArr[i] = contentValues.get(strArr[i]);
        }
        matrixCursor.addRow(objArr);
        return matrixCursor;
    }

    private void copyQueryParamsToContentValues(ContentValues contentValues, Uri uri, String... strArr) {
        for (String str : strArr) {
            String queryParameter = uri.getQueryParameter(str);
            if (queryParameter != null) {
                contentValues.put(str, queryParameter);
            }
        }
    }

    private String[] insertSelectionArg(String[] strArr, String str) {
        if (strArr == null) {
            return new String[]{str};
        }
        String[] strArr2 = new String[strArr.length + 1];
        strArr2[0] = str;
        System.arraycopy(strArr, 0, strArr2, 1, strArr.length);
        return strArr2;
    }

    private String[] appendSelectionArg(String[] strArr, String str) {
        if (strArr == null) {
            return new String[]{str};
        }
        int length = strArr.length + 1;
        String[] strArr2 = new String[length];
        strArr2[length] = str;
        System.arraycopy(strArr, 0, strArr2, 0, strArr.length - 1);
        return strArr2;
    }

    protected Account getDefaultAccount() {
        try {
            Account[] accountsByType = AccountManager.get(getContext()).getAccountsByType("com.google");
            if (accountsByType != null && accountsByType.length > 0) {
                return accountsByType[0];
            }
            return null;
        } catch (Throwable th) {
            Log.e("ContactsProvider", "Cannot determine the default account for contacts compatibility", th);
            return null;
        }
    }

    public boolean isWritableAccountWithDataSet(String str) {
        if (str == null) {
            return true;
        }
        Boolean boolValueOf = this.mAccountWritability.get(str);
        if (boolValueOf != null) {
            return boolValueOf.booleanValue();
        }
        try {
            SyncAdapterType[] syncAdapterTypes = ContentResolver.getContentService().getSyncAdapterTypes();
            int length = syncAdapterTypes.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                SyncAdapterType syncAdapterType = syncAdapterTypes[i];
                if ("com.android.contacts".equals(syncAdapterType.authority) && str.equals(syncAdapterType.accountType)) {
                    break;
                }
                i++;
            }
        } catch (RemoteException e) {
            Log.e("ContactsProvider", "Could not acquire sync adapter types");
        }
        if (boolValueOf == null) {
            boolValueOf = false;
        }
        this.mAccountWritability.put(str, boolValueOf);
        return boolValueOf.booleanValue();
    }

    static boolean readBooleanQueryParameter(Uri uri, String str, boolean z) {
        int iIndexOf;
        String encodedQuery = uri.getEncodedQuery();
        if (encodedQuery == null || (iIndexOf = encodedQuery.indexOf(str)) == -1) {
            return z;
        }
        int length = iIndexOf + str.length();
        return (matchQueryParameter(encodedQuery, length, "=0", false) || matchQueryParameter(encodedQuery, length, "=false", true)) ? false : true;
    }

    private static boolean matchQueryParameter(String str, int i, String str2, boolean z) {
        int i2;
        int length = str2.length();
        return str.regionMatches(z, i, str2, 0, length) && (str.length() == (i2 = i + length) || str.charAt(i2) == '&');
    }

    static String getQueryParameter(Uri uri, String str) {
        String strSubstring;
        char cCharAt;
        String encodedQuery = uri.getEncodedQuery();
        if (encodedQuery == null) {
            return null;
        }
        int length = encodedQuery.length();
        int length2 = str.length();
        int i = 0;
        while (true) {
            int iIndexOf = encodedQuery.indexOf(str, i);
            if (iIndexOf == -1) {
                return null;
            }
            if (iIndexOf > 0 && (cCharAt = encodedQuery.charAt(iIndexOf - 1)) != '?' && cCharAt != '&') {
                i = iIndexOf + length2;
            } else {
                i = iIndexOf + length2;
                if (length == i) {
                    return null;
                }
                if (encodedQuery.charAt(i) == '=') {
                    int i2 = i + 1;
                    int iIndexOf2 = encodedQuery.indexOf(38, i2);
                    if (iIndexOf2 == -1) {
                        strSubstring = encodedQuery.substring(i2);
                    } else {
                        strSubstring = encodedQuery.substring(i2, iIndexOf2);
                    }
                    return Uri.decode(strSubstring);
                }
            }
        }
    }

    private boolean isAggregationUpgradeNeeded() {
        return this.mContactAggregator.isEnabled() && Integer.parseInt(this.mContactsHelper.getProperty("aggregation_v2", "1")) < PROPERTY_AGGREGATION_ALGORITHM_VERSION;
    }

    private void upgradeAggregationAlgorithmInBackground() {
        SQLiteDatabase writableDatabase;
        int iMarkAllVisibleForAggregation;
        Log.i("ContactsProvider", "Upgrading aggregation algorithm");
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        boolean z = true;
        setProviderStatus(1);
        try {
        } catch (RuntimeException e) {
            Log.e("ContactsProvider", "Failed to upgrade aggregation algorithm; continuing anyway.", e);
            try {
                SQLiteDatabase writableDatabase2 = this.mContactsHelper.getWritableDatabase();
                writableDatabase2.beginTransactionNonExclusive();
                try {
                    updateAggregationAlgorithmVersion();
                    writableDatabase2.setTransactionSuccessful();
                    writableDatabase2.endTransaction();
                } catch (Throwable th) {
                    writableDatabase2.endTransaction();
                    throw th;
                }
            } catch (RuntimeException e2) {
                Log.e("ContactsProvider", "Failed to bump aggregation algorithm version; continuing anyway.", e2);
            }
        }
        try {
            try {
                switchToContactMode();
                writableDatabase = this.mContactsHelper.getWritableDatabase();
                try {
                    writableDatabase.beginTransaction();
                    try {
                        iMarkAllVisibleForAggregation = this.mContactAggregator.markAllVisibleForAggregation(writableDatabase);
                        try {
                            this.mContactAggregator.aggregateInTransaction(this.mTransactionContext.get(), writableDatabase);
                            updateSearchIndexInTransaction();
                            updateAggregationAlgorithmVersion();
                            writableDatabase.setTransactionSuccessful();
                            this.mTransactionContext.get().clearAll();
                            writableDatabase.endTransaction();
                            long jElapsedRealtime2 = SystemClock.elapsedRealtime();
                            StringBuilder sb = new StringBuilder();
                            sb.append("Aggregation algorithm upgraded for ");
                            sb.append(iMarkAllVisibleForAggregation);
                            sb.append(" raw contacts");
                            sb.append(" in " + (jElapsedRealtime2 - jElapsedRealtime) + "ms");
                            Log.i("ContactsProvider", sb.toString());
                        } catch (Throwable th2) {
                            th = th2;
                            this.mTransactionContext.get().clearAll();
                            if (z) {
                                writableDatabase.endTransaction();
                            }
                            SystemClock.elapsedRealtime();
                            Log.i("ContactsProvider", "Aggregation algorithm upgraded for " + iMarkAllVisibleForAggregation + " raw contacts failed");
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        iMarkAllVisibleForAggregation = 0;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    z = false;
                    iMarkAllVisibleForAggregation = 0;
                    this.mTransactionContext.get().clearAll();
                    if (z) {
                    }
                    SystemClock.elapsedRealtime();
                    Log.i("ContactsProvider", "Aggregation algorithm upgraded for " + iMarkAllVisibleForAggregation + " raw contacts failed");
                    throw th;
                }
            } finally {
                setProviderStatus(0);
            }
        } catch (Throwable th5) {
            th = th5;
            writableDatabase = null;
        }
    }

    private void updateAggregationAlgorithmVersion() {
        this.mContactsHelper.setProperty("aggregation_v2", String.valueOf(PROPERTY_AGGREGATION_ALGORITHM_VERSION));
    }

    protected boolean isPhone() {
        if (!this.mIsPhoneInitialized) {
            this.mIsPhone = new TelephonyManager(getContext()).isVoiceCapable();
            this.mIsPhoneInitialized = true;
        }
        return this.mIsPhone;
    }

    private void undemoteContact(SQLiteDatabase sQLiteDatabase, long j) {
        String[] strArr = {String.valueOf(j)};
        sQLiteDatabase.execSQL("UPDATE contacts SET pinned = 0 WHERE _id = ?1 AND pinned <= -1", strArr);
        sQLiteDatabase.execSQL("UPDATE raw_contacts SET pinned = 0 WHERE contact_id = ?1 AND pinned <= -1", strArr);
    }

    private boolean handleDataUsageFeedback(Uri uri) {
        int i;
        Cursor cursorRawQuery;
        long jCurrentTimeMillis = Clock.getInstance().currentTimeMillis();
        String queryParameter = uri.getQueryParameter("type");
        String[] strArrSplit = uri.getLastPathSegment().trim().split(",");
        ArrayList arrayList = new ArrayList(strArrSplit.length);
        for (String str : strArrSplit) {
            arrayList.add(Long.valueOf(str));
        }
        try {
            if (TextUtils.isEmpty(queryParameter)) {
                Log.w("ContactsProvider", "Method for data usage feedback isn't specified. Ignoring.");
            } else {
                boolean z = updateDataUsageStat(arrayList, queryParameter, jCurrentTimeMillis) > 0;
                StringBuilder sb = new StringBuilder();
                sb.append("SELECT raw_contact_id FROM data WHERE _id IN (");
                for (i = 0; i < strArrSplit.length; i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(strArrSplit[i]);
                }
                sb.append(")");
                SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
                ArraySet arraySet = new ArraySet();
                cursorRawQuery = writableDatabase.rawQuery(sb.toString(), null);
                cursorRawQuery.moveToPosition(-1);
                while (cursorRawQuery.moveToNext()) {
                    long j = cursorRawQuery.getLong(0);
                    this.mTransactionContext.get().markRawContactMetadataDirty(j, false);
                    arraySet.add(Long.valueOf(j));
                }
                cursorRawQuery.close();
                this.mSelectionArgs1[0] = String.valueOf(jCurrentTimeMillis);
                String strJoin = TextUtils.join(",", arraySet);
                writableDatabase.execSQL("UPDATE raw_contacts SET x_last_time_contacted=?,x_times_contacted=ifnull(x_times_contacted,0) + 1 WHERE _id IN (" + strJoin + ")", this.mSelectionArgs1);
                writableDatabase.execSQL("UPDATE contacts SET x_last_time_contacted=?1,x_times_contacted=ifnull(x_times_contacted,0) + 1,contact_last_updated_timestamp=?1 WHERE _id IN (SELECT contact_id FROM raw_contacts WHERE _id IN (" + strJoin + "))", this.mSelectionArgs1);
                return z;
            }
            cursorRawQuery.moveToPosition(-1);
            while (cursorRawQuery.moveToNext()) {
            }
            cursorRawQuery.close();
            this.mSelectionArgs1[0] = String.valueOf(jCurrentTimeMillis);
            String strJoin2 = TextUtils.join(",", arraySet);
            writableDatabase.execSQL("UPDATE raw_contacts SET x_last_time_contacted=?,x_times_contacted=ifnull(x_times_contacted,0) + 1 WHERE _id IN (" + strJoin2 + ")", this.mSelectionArgs1);
            writableDatabase.execSQL("UPDATE contacts SET x_last_time_contacted=?1,x_times_contacted=ifnull(x_times_contacted,0) + 1,contact_last_updated_timestamp=?1 WHERE _id IN (SELECT contact_id FROM raw_contacts WHERE _id IN (" + strJoin2 + "))", this.mSelectionArgs1);
            return z;
        } catch (Throwable th) {
            cursorRawQuery.close();
            throw th;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("SELECT raw_contact_id FROM data WHERE _id IN (");
        while (i < strArrSplit.length) {
        }
        sb2.append(")");
        SQLiteDatabase writableDatabase2 = this.mDbHelper.get().getWritableDatabase();
        ArraySet arraySet2 = new ArraySet();
        cursorRawQuery = writableDatabase2.rawQuery(sb2.toString(), null);
    }

    int updateDataUsageStat(List<Long> list, String str, long j) {
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        String strValueOf = String.valueOf(getDataUsageFeedbackType(str, null));
        String strValueOf2 = String.valueOf(j);
        Iterator<Long> it = list.iterator();
        while (it.hasNext()) {
            String strValueOf3 = String.valueOf(it.next().longValue());
            this.mSelectionArgs2[0] = strValueOf3;
            this.mSelectionArgs2[1] = strValueOf;
            Cursor cursorQuery = writableDatabase.query("data_usage_stat", DataUsageStatQuery.COLUMNS, "data_id =? AND usage_type =?", this.mSelectionArgs2, null, null, null);
            try {
                if (cursorQuery.moveToFirst()) {
                    long j2 = cursorQuery.getLong(0);
                    this.mSelectionArgs2[0] = strValueOf2;
                    this.mSelectionArgs2[1] = String.valueOf(j2);
                    writableDatabase.execSQL("UPDATE data_usage_stat SET x_times_used=ifnull(x_times_used,0)+1,x_last_time_used=? WHERE stat_id=?", this.mSelectionArgs2);
                } else {
                    this.mSelectionArgs4[0] = strValueOf3;
                    this.mSelectionArgs4[1] = strValueOf;
                    this.mSelectionArgs4[2] = "1";
                    this.mSelectionArgs4[3] = strValueOf2;
                    writableDatabase.execSQL("INSERT INTO data_usage_stat(data_id,usage_type,x_times_used,x_last_time_used) VALUES (?,?,?,?)", this.mSelectionArgs4);
                }
            } finally {
                cursorQuery.close();
            }
        }
        return list.size();
    }

    private void updateDataUsageStats(SQLiteDatabase sQLiteDatabase, ContentValues contentValues) {
        String asString = contentValues.getAsString("data_id");
        String asString2 = contentValues.getAsString("usage_type");
        String asString3 = contentValues.getAsString("x_last_time_used");
        String asString4 = contentValues.getAsString("x_times_used");
        this.mSelectionArgs2[0] = asString;
        this.mSelectionArgs2[1] = asString2;
        Cursor cursorQuery = sQLiteDatabase.query("data_usage_stat", DataUsageStatQuery.COLUMNS, "data_id =? AND usage_type =?", this.mSelectionArgs2, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                long j = cursorQuery.getLong(0);
                this.mSelectionArgs3[0] = asString3;
                this.mSelectionArgs3[1] = asString4;
                this.mSelectionArgs3[2] = String.valueOf(j);
                sQLiteDatabase.execSQL("UPDATE data_usage_stat SET x_last_time_used=?,x_times_used=? WHERE stat_id=?", this.mSelectionArgs3);
            } else {
                this.mSelectionArgs4[0] = asString;
                this.mSelectionArgs4[1] = asString2;
                this.mSelectionArgs4[2] = asString4;
                this.mSelectionArgs4[3] = asString3;
                sQLiteDatabase.execSQL("INSERT INTO data_usage_stat(data_id,usage_type,x_times_used,x_last_time_used) VALUES (?,?,?,?)", this.mSelectionArgs4);
            }
        } finally {
            cursorQuery.close();
        }
    }

    private String getAccountPromotionSortOrder(Uri uri) {
        String queryParameter = uri.getQueryParameter("name_for_primary_account");
        String queryParameter2 = uri.getQueryParameter("type_for_primary_account");
        if (!TextUtils.isEmpty(queryParameter)) {
            StringBuilder sb = new StringBuilder();
            sb.append("(CASE WHEN account_name=");
            DatabaseUtils.appendEscapedSQLString(sb, queryParameter);
            if (!TextUtils.isEmpty(queryParameter2)) {
                sb.append(" AND account_type=");
                DatabaseUtils.appendEscapedSQLString(sb, queryParameter2);
            }
            sb.append(" THEN 0 ELSE 1 END)");
            return sb.toString();
        }
        return null;
    }

    private boolean deferredSnippetingRequested(Uri uri) {
        String queryParameter = getQueryParameter(uri, "deferred_snippeting");
        return !TextUtils.isEmpty(queryParameter) && queryParameter.equals("1");
    }

    private boolean isSingleWordQuery(String str) {
        int i = 0;
        for (String str2 : str.split("[^\\w@]+", 0)) {
            if (!"".equals(str2)) {
                i++;
            }
        }
        return i == 1;
    }

    private boolean snippetNeeded(String[] strArr) {
        return ContactsDatabaseHelper.isInProjection(strArr, "snippet");
    }

    private void replacePackageNameByPackageId(ContentValues contentValues) {
        if (contentValues != null) {
            String asString = contentValues.getAsString("res_package");
            if (asString != null) {
                contentValues.put("package_id", Long.valueOf(this.mDbHelper.get().getPackageId(asString)));
            }
            contentValues.remove("res_package");
        }
    }

    private long replaceAccountInfoByAccountId(Uri uri, ContentValues contentValues) {
        long orCreateAccountIdInTransaction = this.mDbHelper.get().getOrCreateAccountIdInTransaction(resolveAccountWithDataSet(uri, contentValues));
        contentValues.put("account_id", Long.valueOf(orCreateAccountIdInTransaction));
        contentValues.remove("account_name");
        contentValues.remove("account_type");
        contentValues.remove("data_set");
        return orCreateAccountIdInTransaction;
    }

    static Cursor buildSingleRowResult(String[] strArr, String[] strArr2, Object[] objArr) {
        boolean z;
        Preconditions.checkArgument(strArr2.length == objArr.length);
        if (strArr == null) {
            strArr = strArr2;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr, 1);
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        for (int i = 0; i < matrixCursor.getColumnCount(); i++) {
            String columnName = matrixCursor.getColumnName(i);
            int i2 = 0;
            while (true) {
                if (i2 < strArr2.length) {
                    if (!strArr2[i2].equals(columnName)) {
                        i2++;
                    } else {
                        rowBuilderNewRow.add(objArr[i2]);
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                throw new IllegalArgumentException("Invalid column " + strArr[i]);
            }
        }
        return matrixCursor;
    }

    protected ContactsDatabaseHelper getThreadActiveDatabaseHelperForTest() {
        return this.mDbHelper.get();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mContactAggregator != null) {
            printWriter.println();
            printWriter.print("Contact aggregator type: " + this.mContactAggregator.getClass() + "\n");
        }
        printWriter.println();
        printWriter.print("FastScrollingIndex stats:\n");
        printWriter.printf("  request=%d  miss=%d (%d%%)  avg time=%dms\n", Integer.valueOf(this.mFastScrollingIndexCacheRequestCount), Integer.valueOf(this.mFastScrollingIndexCacheMissCount), Long.valueOf(safeDiv(this.mFastScrollingIndexCacheMissCount * 100, this.mFastScrollingIndexCacheRequestCount)), Long.valueOf(safeDiv(this.mTotalTimeFastScrollingIndexGenerate, this.mFastScrollingIndexCacheMissCount)));
        printWriter.println();
        printWriter.println();
        dump(printWriter, "Contacts");
        printWriter.println();
        this.mProfileProvider.dump(fileDescriptor, printWriter, strArr);
    }

    private static final long safeDiv(long j, long j2) {
        if (j2 == 0) {
            return 0L;
        }
        return j / j2;
    }

    private static final int getDataUsageFeedbackType(String str, Integer num) {
        if ("call".equals(str)) {
            return 0;
        }
        if ("long_text".equals(str)) {
            return 1;
        }
        if ("short_text".equals(str)) {
            return 2;
        }
        if (num != null) {
            return num.intValue();
        }
        throw new IllegalArgumentException("Invalid usage type " + str);
    }

    private static final int getAggregationType(String str, Integer num) {
        if ("TOGETHER".equalsIgnoreCase(str)) {
            return 1;
        }
        if ("SEPARATE".equalsIgnoreCase(str)) {
            return 2;
        }
        if ("AUTOMATIC".equalsIgnoreCase(str)) {
            return 0;
        }
        if (num != null) {
            return num.intValue();
        }
        throw new IllegalArgumentException("Invalid aggregation type " + str);
    }

    public String toString() {
        return "ContactsProvider2";
    }

    public void switchToProfileModeForTest() {
        switchToProfileMode();
    }

    @Override
    public void shutdown() {
        this.mTaskScheduler.shutdownForTest();
    }

    public ContactsDatabaseHelper getContactsDatabaseHelperForTest() {
        return this.mContactsHelper;
    }

    public ProfileProvider getProfileProviderForTest() {
        return this.mProfileProvider;
    }

    protected void loadLocalPhoneAccounts() {
        Log.d("ContactsProvider", "loadLocalPhoneAccounts()+ ");
        switchToContactMode();
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        String str = SystemProperties.get("ro.build.characteristics");
        try {
            try {
                writableDatabase.beginTransaction();
                try {
                    Cursor cursorRawQuery = writableDatabase.rawQuery("SELECT account_name,account_type FROM settings WHERE account_name = 'Phone'", null);
                    try {
                        StringBuilder sb = new StringBuilder();
                        sb.append("loadLocal Phone Accounts -cursor.count:");
                        sb.append(cursorRawQuery == null ? 0 : cursorRawQuery.getCount());
                        Log.i("ContactsProvider", sb.toString());
                        if (cursorRawQuery != null && cursorRawQuery.getCount() == 0) {
                            if (str != null && str.equals("tablet")) {
                                writableDatabase.execSQL("INSERT INTO settings (account_name, account_type, data_set, ungrouped_visible, should_sync) VALUES (?, ?, ?, ?, ?)", new String[]{"Tablet", "Local Phone Account", null, "1", "1"});
                            } else {
                                writableDatabase.execSQL("INSERT INTO settings (account_name, account_type, data_set, ungrouped_visible, should_sync) VALUES (?, ?, ?, ?, ?)", new String[]{"Phone", "Local Phone Account", null, "1", "1"});
                            }
                        }
                        cursorRawQuery.close();
                        writableDatabase.setTransactionSuccessful();
                    } catch (Throwable th) {
                        cursorRawQuery.close();
                        throw th;
                    }
                } finally {
                    writableDatabase.endTransaction();
                }
            } catch (SQLiteDiskIOException e) {
                Log.w("ContactsProvider", "[loadLocalPhoneAccounts]catch SQLiteDiskIOException.");
            }
        } catch (SQLiteCantOpenDatabaseException e2) {
            Log.e("ContactsProvider", "[loadLocalPhoneAccounts]catch SQLiteCantOpenDatabaseException. for endTransaction()");
        } catch (SQLiteDiskIOException e3) {
            Log.w("ContactsProvider", "[loadLocalPhoneAccounts]catch SQLiteDiskIOException.");
        } catch (SQLiteException e4) {
            Log.e("ContactsProvider", "[loadLocalPhoneAccounts]catch SQLiteException for endTransaction()");
        }
    }

    private boolean canDeleteAccount(AccountWithDataSet accountWithDataSet) {
        if (accountWithDataSet.getDataSet() == null && AccountUtils.isLocalAccount(accountWithDataSet.getAccountType(), accountWithDataSet.getAccountName())) {
            Log.d("ContactsProvider", "[canDeleteAccount] -> not delete: " + accountWithDataSet);
            return false;
        }
        return true;
    }

    public static class RawContactEntry {
        long mContactId;
        long mRawContactId;

        public RawContactEntry(long j, long j2) {
            this.mRawContactId = j;
            this.mContactId = j2;
        }
    }

    public int deleteRawContactInOneBatch(Uri uri, String str, String[] strArr) {
        this.mAggregator.get().invalidateAggregationExceptionCache();
        this.mProviderStatusUpdateNeeded = true;
        SQLiteDatabase writableDatabase = this.mDbHelper.get().getWritableDatabase();
        Cursor cursorQuery = writableDatabase.query("stream_items", new String[]{"_id"}, "raw_contact_id IN (SELECT _id FROM view_raw_contacts WHERE (" + str + "))", strArr, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                deleteStreamItem(writableDatabase, cursorQuery.getLong(0));
            } finally {
            }
        }
        cursorQuery.close();
        writableDatabase.delete("presence", "presence_raw_contact_id IN (SELECT _id FROM view_raw_contacts WHERE (" + str + "))", strArr);
        ArrayList<RawContactEntry> arrayList = new ArrayList();
        cursorQuery = writableDatabase.query("view_raw_contacts", new String[]{"_id", "contact_id"}, appendAccountToSelection(uri, str), strArr, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add(new RawContactEntry(cursorQuery.getLong(0), cursorQuery.getLong(1)));
            } finally {
            }
        }
        cursorQuery.close();
        int iDelete = writableDatabase.delete("raw_contacts", "_id IN (SELECT _id FROM view_raw_contacts WHERE (" + str + "))", strArr);
        if (arrayList.size() > 0) {
            for (RawContactEntry rawContactEntry : arrayList) {
                Log.d("ContactsProvider", "updateAggregateData begin");
                this.mAggregator.get().updateAggregateData(this.mTransactionContext.get(), rawContactEntry.mContactId);
                Log.d("ContactsProvider", "updateAggregateData end");
            }
        }
        return iDelete;
    }

    private Uri requestCheckedIds(Uri uri) {
        String queryParameter = getQueryParameter(uri, "checked_ids_arg");
        if (TextUtils.isEmpty(queryParameter)) {
            return null;
        }
        return Uri.parse(queryParameter);
    }

    private void bundleNonFilterIdsExtras(Cursor cursor, Uri uri, SQLiteDatabase sQLiteDatabase, SQLiteQueryBuilder sQLiteQueryBuilder, String str, String[] strArr, String str2, String str3, String str4, String str5, CancellationSignal cancellationSignal) throws Throwable {
        Cursor cursorQuery;
        try {
            cursorQuery = query(uri, new String[]{"_id"}, str, strArr, "_id", cancellationSignal);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() != 0) {
                        int[] iArr = new int[cursorQuery.getCount()];
                        cursorQuery.moveToPosition(-1);
                        int i = 0;
                        while (cursorQuery.moveToNext()) {
                            iArr[i] = cursorQuery.getInt(0);
                            i++;
                        }
                        Bundle extras = cursor.getExtras();
                        Bundle bundle = new Bundle();
                        if (extras != null) {
                            bundle.putAll(extras);
                        }
                        bundle.putIntArray("checked_ids", iArr);
                        ((AbstractCursor) cursor).setExtras(bundle);
                        if (VERBOSE_LOGGING) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("bundleNonFilterIdsExtras ids: ");
                            for (int i2 : iArr) {
                                sb.append(String.valueOf(i2) + ",");
                            }
                            Log.v("ContactsProvider", sb.toString());
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private int deleteContactInOneBatch(SQLiteDatabase sQLiteDatabase, String[] strArr, boolean z) {
        StringBuilder sb = new StringBuilder();
        String[] strArr2 = new String[strArr.length];
        Arrays.fill(strArr2, "?");
        sb.append("contact_id IN (");
        sb.append(TextUtils.join(",", strArr2));
        sb.append(")");
        int iDeleteRawContactInOneBatch = deleteRawContactInOneBatch(ContactsContract.RawContacts.CONTENT_URI, sb.toString() + " AND " + AccountUtils.getLocalAccountSelection(), strArr);
        Cursor cursorQuery = sQLiteDatabase.query("view_raw_contacts", new String[]{"_id", "contact_id"}, sb.toString() + " AND " + AccountUtils.getSyncAccountSelection(), strArr, null, null, null);
        int iDeleteContact = 0;
        while (cursorQuery.moveToNext()) {
            try {
                iDeleteContact += deleteContact(cursorQuery.getLong(1), z);
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        Log.d("ContactsProvider", "deleteContactInSyncAccount deleted local: " + iDeleteRawContactInOneBatch + " remote: " + iDeleteContact);
        return iDeleteRawContactInOneBatch + iDeleteContact;
    }

    private boolean isSimContact(SQLiteDatabase sQLiteDatabase, long j) {
        Cursor cursorQuery = sQLiteDatabase.query("raw_contacts", new String[]{"indicate_phone_or_sim_contact"}, "_id=?", new String[]{Long.toString(j)}, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                return cursorQuery.getLong(cursorQuery.getColumnIndexOrThrow("indicate_phone_or_sim_contact")) >= 0;
            }
            return false;
        } finally {
            cursorQuery.close();
        }
    }
}
