package com.android.providers.media;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaFile;
import android.media.MediaScanner;
import android.media.MediaScannerConnection;
import android.media.MiniThumbFile;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import com.android.providers.media.IMtpService;
import com.android.providers.media.MediaThumbRequest;
import com.mediatek.media.MtkMediaStore;
import com.mediatek.storage.StorageManagerEx;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;

public class MediaProvider extends ContentProvider {
    private AppOpsManager mAppOpsManager;
    private String mCachePath;
    private HashMap<String, DatabaseHelper> mDatabases;
    private String mExternalPath;
    private Handler mHandler;
    private String mLegacyPath;
    private String mMediaScannerVolume;
    private IMtpService mMtpService;
    private String mMtpTransferFile;
    private PackageManager mPackageManager;
    private StorageManager mStorageManager;
    private Handler mThumbHandler;
    private static final Uri MEDIA_URI = Uri.parse("content://media");
    private static final Uri ALBUMART_URI = Uri.parse("content://media/external/audio/albumart");
    private static final HashMap<String, String> sArtistAlbumsMap = new HashMap<>();
    private static final HashMap<String, String> sFolderArtMap = new HashMap<>();
    private static final String[] sMediaTableColumns = {"_id", "media_type"};
    private static final String[] sIdOnlyColumn = {"_id"};
    private static final String[] sDataOnlyColumn = {"_data"};
    private static final String[] sMediaTypeDataId = {"media_type", "_data", "_id"};
    private static final String[] sPlaylistIdPlayOrder = {"playlist_id", "play_order"};
    private static final String[] sDataId = {"_data", "_id"};
    private static final Uri sAlbumArtBaseUri = Uri.parse("content://media/external/audio/albumart");
    private static ArrayList<String> sExternalVolumeIdList = new ArrayList<>();
    private static final String[] sDefaultFolderNames = {Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_PODCASTS, Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS, Environment.DIRECTORY_PICTURES, Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_DOWNLOADS, Environment.DIRECTORY_DCIM};
    private static final String[] GENRE_LOOKUP_PROJECTION = {"_id", "name"};
    private static final String[] openFileColumns = {"_data"};
    private static String TAG = "MediaProvider";
    private static final UriMatcher URI_MATCHER = new UriMatcher(-1);
    private static final String[] ID_PROJECTION = {"_id"};
    private static final String[] PATH_PROJECTION = {"_id", "_data"};
    private static final String[] MIME_TYPE_PROJECTION = {"_id", "mime_type"};
    private static final String[] READY_FLAG_PROJECTION = {"_id", "_data", "mini_thumb_magic"};
    HashMap<String, Long> mDirectoryCache = new HashMap<>();
    private HashSet mPendingThumbs = new HashSet();
    private Stack mThumbRequestStack = new Stack();
    private MediaThumbRequest mCurrentThumbRequest = null;
    private PriorityQueue<MediaThumbRequest> mMediaThumbQueue = new PriorityQueue<>(10, MediaThumbRequest.getComparator());
    private String[] mExternalStoragePaths = EmptyArray.STRING;
    private String[] mSearchColsLegacy = {"_id", "mime_type", "(CASE WHEN grouporder=1 THEN 2130837510 ELSE CASE WHEN grouporder=2 THEN 2130837509 ELSE 2130837511 END END) AS suggest_icon_1", "0 AS suggest_icon_2", "text1 AS suggest_text_1", "text1 AS suggest_intent_query", "CASE when grouporder=1 THEN data1 ELSE artist END AS data1", "CASE when grouporder=1 THEN data2 ELSE CASE WHEN grouporder=2 THEN NULL ELSE album END END AS data2", "match as ar", "suggest_intent_data", "grouporder", "NULL AS itemorder"};
    private String[] mSearchColsFancy = {"_id", "mime_type", "artist", "album", "title", "data1", "data2"};
    private String[] mSearchColsBasic = {"_id", "mime_type", "(CASE WHEN grouporder=1 THEN 2130837510 ELSE CASE WHEN grouporder=2 THEN 2130837509 ELSE 2130837511 END END) AS suggest_icon_1", "text1 AS suggest_text_1", "text1 AS suggest_intent_query", "(CASE WHEN grouporder=1 THEN '%1' ELSE CASE WHEN grouporder=3 THEN artist || ' - ' || album ELSE CASE WHEN text2!='<unknown>' THEN text2 ELSE NULL END END END) AS suggest_text_2", "suggest_intent_data"};
    private final int SEARCH_COLUMN_BASIC_TEXT2 = 5;
    private boolean mNeedWaitStorageStateChange = false;
    private MediaProviderUtils mMediaProviderUtils = null;
    private boolean mUnderPrescanning = false;
    private BroadcastReceiver mUnmountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(MediaProvider.TAG, "unmountReceiver: intent=" + intent);
            if (!"android.intent.action.MEDIA_EJECT".equals(action)) {
                if ("android.intent.action.MEDIA_UNMOUNTED".equals(action)) {
                    MediaScannerThreadPool.updateFolderMap();
                    return;
                } else {
                    if ("android.intent.action.ACTION_SHUTDOWN".equals(action) || "mediatek.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                        context.unregisterReceiver(MediaProvider.this.mUnmountReceiver);
                        Log.v(MediaProvider.TAG, "unregisterReceiver mUnmountReceiver when device shutdown");
                        return;
                    }
                    return;
                }
            }
            StorageVolume storageVolume = (StorageVolume) intent.getParcelableExtra("android.os.storage.extra.STORAGE_VOLUME");
            boolean booleanExtra = intent.getBooleanExtra("mount_unmount_all", false);
            MediaProvider.this.initVolumeInfo();
            if (storageVolume.isPrimary()) {
                MediaProvider.this.mNeedWaitStorageStateChange = true;
                MediaProvider.this.detachVolume(Uri.parse("content://media/external"));
                MediaProvider.sFolderArtMap.clear();
                MiniThumbFile.reset();
                return;
            }
            if (booleanExtra) {
                return;
            }
            if (MediaScannerReceiver.sIsShutdown) {
                Log.w(MediaProvider.TAG, "trigger to delete all entries when shutdown, do nothing!");
                return;
            }
            MediaProvider.this.deleteAllEntriesForStorage(context, storageVolume);
            context.getContentResolver().notifyChange(MediaStore.Audio.Media.getContentUri("external"), null);
            context.getContentResolver().notifyChange(MediaStore.Images.Media.getContentUri("external"), null);
            context.getContentResolver().notifyChange(MediaStore.Video.Media.getContentUri("external"), null);
            context.getContentResolver().notifyChange(MediaStore.Files.getContentUri("external"), null);
        }
    };
    private final SQLiteDatabase.CustomFunction mObjectRemovedCallback = new SQLiteDatabase.CustomFunction() {
        public void callback(String[] strArr) {
            MediaProvider.this.mDirectoryCache.clear();
        }
    };
    private final ServiceConnection mMtpServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (this) {
                Log.v(MediaProvider.TAG, "MtpService: ServiceConnection!!");
                MediaProvider.this.mMtpService = IMtpService.Stub.asInterface(iBinder);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (this) {
                Log.v(MediaProvider.TAG, "MtpService: ServiceDisconnected!!");
                MediaProvider.this.mMtpService = null;
            }
        }
    };
    private int mVolumeId = -1;
    private AtomicInteger mBulkInsertCount = new AtomicInteger();
    private boolean mDatabaseToBeClosed = false;

    static {
        URI_MATCHER.addURI("media", "*/images/media", 1);
        URI_MATCHER.addURI("media", "*/images/media/#", 2);
        URI_MATCHER.addURI("media", "*/images/thumbnails", 3);
        URI_MATCHER.addURI("media", "*/images/thumbnails/#", 4);
        URI_MATCHER.addURI("media", "*/audio/media", 100);
        URI_MATCHER.addURI("media", "*/audio/media/#", 101);
        URI_MATCHER.addURI("media", "*/audio/media/#/genres", 102);
        URI_MATCHER.addURI("media", "*/audio/media/#/genres/#", 103);
        URI_MATCHER.addURI("media", "*/audio/media/#/playlists", 104);
        URI_MATCHER.addURI("media", "*/audio/media/#/playlists/#", 105);
        URI_MATCHER.addURI("media", "*/audio/genres", 106);
        URI_MATCHER.addURI("media", "*/audio/genres/#", 107);
        URI_MATCHER.addURI("media", "*/audio/genres/#/members", 108);
        URI_MATCHER.addURI("media", "*/audio/genres/all/members", 109);
        URI_MATCHER.addURI("media", "*/audio/playlists", 110);
        URI_MATCHER.addURI("media", "*/audio/playlists/#", 111);
        URI_MATCHER.addURI("media", "*/audio/playlists/#/members", 112);
        URI_MATCHER.addURI("media", "*/audio/playlists/#/members/#", 113);
        URI_MATCHER.addURI("media", "*/audio/artists", 114);
        URI_MATCHER.addURI("media", "*/audio/artists/#", 115);
        URI_MATCHER.addURI("media", "*/audio/artists/#/albums", 118);
        URI_MATCHER.addURI("media", "*/audio/albums", 116);
        URI_MATCHER.addURI("media", "*/audio/albums/#", 117);
        URI_MATCHER.addURI("media", "*/audio/albumart", 119);
        URI_MATCHER.addURI("media", "*/audio/albumart/#", 120);
        URI_MATCHER.addURI("media", "*/audio/media/#/albumart", 121);
        URI_MATCHER.addURI("media", "*/video/media", 200);
        URI_MATCHER.addURI("media", "*/video/media/#", 201);
        URI_MATCHER.addURI("media", "*/video/thumbnails", 202);
        URI_MATCHER.addURI("media", "*/video/thumbnails/#", 203);
        URI_MATCHER.addURI("media", "*/media_scanner", 500);
        URI_MATCHER.addURI("media", "*/fs_id", 600);
        URI_MATCHER.addURI("media", "*/version", 601);
        URI_MATCHER.addURI("media", "*/mtp_connected", 705);
        URI_MATCHER.addURI("media", "*", 301);
        URI_MATCHER.addURI("media", null, 300);
        URI_MATCHER.addURI("media", "*/file", 700);
        URI_MATCHER.addURI("media", "*/file/#", 701);
        URI_MATCHER.addURI("media", "*/object", 702);
        URI_MATCHER.addURI("media", "*/object/#", 703);
        URI_MATCHER.addURI("media", "*/object/#/references", 704);
        URI_MATCHER.addURI("media", "*/dir", 706);
        URI_MATCHER.addURI("media", "*/audio/search_suggest_query", 400);
        URI_MATCHER.addURI("media", "*/audio/search_suggest_query/*", 400);
        URI_MATCHER.addURI("media", "*/audio/search/search_suggest_query", 401);
        URI_MATCHER.addURI("media", "*/audio/search/search_suggest_query/*", 401);
        URI_MATCHER.addURI("media", "*/audio/search/fancy", 402);
        URI_MATCHER.addURI("media", "*/audio/search/fancy/*", 402);
        URI_MATCHER.addURI("media", "*/bookmark", 1101);
        URI_MATCHER.addURI("media", "*/bookmark/#", 1102);
        URI_MATCHER.addURI("media", "*/mtp_transfer_file", 1201);
        URI_MATCHER.addURI("media", "*/file/search/search_suggest_query", 1300);
        URI_MATCHER.addURI("media", "*/file/search/search_suggest_query/*", 1300);
        URI_MATCHER.addURI("media", "*/file/search/search_suggest_shortcut/*", 1301);
    }

    private void updateStoragePaths() {
        this.mExternalStoragePaths = this.mStorageManager.getVolumePaths();
        try {
            this.mExternalPath = Environment.getExternalStorageDirectory().getCanonicalPath() + File.separator;
            this.mCachePath = Environment.getDownloadCacheDirectory().getCanonicalPath() + File.separator;
            this.mLegacyPath = Environment.getLegacyExternalStorageDirectory().getCanonicalPath() + File.separator;
        } catch (IOException e) {
            throw new RuntimeException("Unable to resolve canonical paths", e);
        }
    }

    private synchronized void setAsPrescanState(boolean z) {
        this.mUnderPrescanning = z;
        Log.d(TAG, "setAsPrescanState() mUnderPrescanning = " + this.mUnderPrescanning);
    }

    private synchronized boolean isPreScanning() {
        return this.mUnderPrescanning;
    }

    private void deleteAllEntriesForStorage(Context context, StorageVolume storageVolume) {
        Intent intent;
        DatabaseHelper databaseForUri = getDatabaseForUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        Uri uri = Uri.parse("file://" + storageVolume.getPath());
        if (databaseForUri != null) {
            try {
                try {
                    context.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_STARTED", uri));
                    Log.d(TAG, "deleting all entries for storage " + storageVolume);
                    Uri.Builder builderBuildUpon = MediaStore.Files.getMtpObjectsUri("external").buildUpon();
                    builderBuildUpon.appendQueryParameter("deletedata", "false");
                    delete(builderBuildUpon.build(), "_data LIKE ?1 AND lower(substr(_data,1,?2))=lower(?3)", new String[]{storageVolume.getPath() + "/%", Integer.toString(storageVolume.getPath().length() + 1), storageVolume.getPath() + "/"});
                    context.getContentResolver().notifyChange(MediaStore.Audio.Media.getContentUri("external"), null);
                    context.getContentResolver().notifyChange(MediaStore.Images.Media.getContentUri("external"), null);
                    context.getContentResolver().notifyChange(MediaStore.Video.Media.getContentUri("external"), null);
                    context.getContentResolver().notifyChange(MediaStore.Files.getContentUri("external"), null);
                    intent = new Intent("android.intent.action.MEDIA_SCANNER_FINISHED", uri);
                } catch (Exception e) {
                    Log.e(TAG, "exception deleting storage entries", e);
                    intent = new Intent("android.intent.action.MEDIA_SCANNER_FINISHED", uri);
                }
                context.sendBroadcast(intent);
                databaseForUri.mDeleteAllSdcardEntries = true;
            } catch (Throwable th) {
                context.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_FINISHED", uri));
                databaseForUri.mDeleteAllSdcardEntries = true;
                throw th;
            }
        }
    }

    static final class DatabaseHelper extends SQLiteOpenHelper {
        ConcurrentHashMap<String, Long> mAlbumCache;
        ConcurrentHashMap<String, Long> mArtistCache;
        final Context mContext;
        private boolean mDeleteAllSdcardEntries;
        final boolean mEarlyUpgrade;
        final boolean mInternal;
        private long mLastModified;
        final String mName;
        int mNumDeletes;
        int mNumInserts;
        int mNumQueries;
        int mNumUpdates;
        final SQLiteDatabase.CustomFunction mObjectRemovedCallback;
        long mScanStartTime;
        long mScanStopTime;
        boolean mUpgradeAttempted;

        public DatabaseHelper(Context context, String str, boolean z, boolean z2, SQLiteDatabase.CustomFunction customFunction) {
            super(context, str, (SQLiteDatabase.CursorFactory) null, MediaProvider.getDatabaseVersion(context));
            this.mDeleteAllSdcardEntries = false;
            this.mLastModified = System.currentTimeMillis();
            this.mArtistCache = new ConcurrentHashMap<>();
            this.mAlbumCache = new ConcurrentHashMap<>();
            this.mContext = context;
            this.mName = str;
            this.mInternal = z;
            this.mEarlyUpgrade = z2;
            this.mObjectRemovedCallback = customFunction;
            setWriteAheadLoggingEnabled(true);
            setIdleConnectionTimeout(30000L);
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) throws Throwable {
            MediaProvider.updateDatabase(this.mContext, sQLiteDatabase, this.mInternal, 0, MediaProvider.getDatabaseVersion(this.mContext));
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws Throwable {
            this.mUpgradeAttempted = true;
            MediaProvider.updateDatabase(this.mContext, sQLiteDatabase, this.mInternal, i, i2);
        }

        @Override
        public synchronized SQLiteDatabase getWritableDatabase() {
            SQLiteDatabase writableDatabase;
            this.mUpgradeAttempted = false;
            writableDatabase = null;
            try {
                writableDatabase = super.getWritableDatabase();
            } catch (Exception e) {
                if (!this.mUpgradeAttempted) {
                    Log.e(MediaProvider.TAG, "failed to open database " + this.mName, e);
                    return null;
                }
                Log.e(MediaProvider.TAG, "DatabaseHelper: failed to open database " + this.mName, e);
            }
            if (writableDatabase == null && this.mUpgradeAttempted) {
                Log.e(MediaProvider.TAG, "DatabaseHelper: delete database " + this.mName);
                this.mContext.deleteDatabase(this.mName);
                writableDatabase = super.getWritableDatabase();
            }
            return writableDatabase;
        }

        @Override
        public void onOpen(SQLiteDatabase sQLiteDatabase) {
            if (this.mInternal || this.mEarlyUpgrade) {
                return;
            }
            if (this.mObjectRemovedCallback != null) {
                sQLiteDatabase.addCustomFunction("_OBJECT_REMOVED", 1, this.mObjectRemovedCallback);
            }
            if (Environment.isExternalStorageRemovable()) {
                File file = new File(sQLiteDatabase.getPath());
                long jCurrentTimeMillis = System.currentTimeMillis();
                file.setLastModified(jCurrentTimeMillis);
                String[] strArrDatabaseList = this.mContext.databaseList();
                ArrayList arrayList = new ArrayList();
                for (String str : strArrDatabaseList) {
                    if (str != null && str.endsWith(".db")) {
                        arrayList.add(str);
                    }
                }
                String[] strArr = (String[]) arrayList.toArray(new String[0]);
                long j = jCurrentTimeMillis - 5184000000L;
                int i = 3;
                int length = strArr.length;
                for (int i2 = 0; i2 < strArr.length; i2++) {
                    File databasePath = this.mContext.getDatabasePath(strArr[i2]);
                    if ("internal.db".equals(strArr[i2]) || file.equals(databasePath)) {
                        strArr[i2] = null;
                        length--;
                        if (file.equals(databasePath)) {
                            i--;
                        }
                    } else if (databasePath.lastModified() < j) {
                        this.mContext.deleteDatabase(strArr[i2]);
                        strArr[i2] = null;
                        length--;
                    }
                }
                while (length > i) {
                    long j2 = 0;
                    int i3 = -1;
                    for (int i4 = 0; i4 < strArr.length; i4++) {
                        if (strArr[i4] != null) {
                            long jLastModified = this.mContext.getDatabasePath(strArr[i4]).lastModified();
                            if (j2 == 0 || jLastModified < j2) {
                                i3 = i4;
                                j2 = jLastModified;
                            }
                        }
                    }
                    if (i3 != -1) {
                        this.mContext.deleteDatabase(strArr[i3]);
                        strArr[i3] = null;
                        length--;
                    }
                }
            }
        }
    }

    private void ensureDefaultFolders(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase) {
        StorageVolume primaryVolume = this.mStorageManager.getPrimaryVolume();
        String str = "emulated".equals(primaryVolume.getId()) ? "created_default_folders" : "created_default_folders_" + primaryVolume.getUuid();
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (defaultSharedPreferences.getInt(str, 0) == 0) {
            for (String str2 : sDefaultFolderNames) {
                File file = new File(primaryVolume.getPathFile(), str2);
                if (!file.exists()) {
                    file.mkdirs();
                    insertDirectory(databaseHelper, sQLiteDatabase, file.getAbsolutePath());
                }
            }
            SharedPreferences.Editor editorEdit = defaultSharedPreferences.edit();
            editorEdit.putInt(str, 1);
            editorEdit.commit();
            return;
        }
        File file2 = new File(primaryVolume.getPathFile(), Environment.DIRECTORY_DCIM);
        boolean zExists = file2.exists();
        Log.d(TAG, "ensureDefaultFolders dcimExist: " + zExists);
        if (!zExists) {
            file2.mkdirs();
        }
    }

    public static int getDatabaseVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("couldn't get version code for " + context);
        }
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "onCreate>>>");
        Context context = getContext();
        this.mMediaProviderUtils = new MediaProviderUtils(context);
        this.mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mPackageManager = context.getPackageManager();
        sArtistAlbumsMap.put("_id", "audio.album_id AS _id");
        sArtistAlbumsMap.put("album", "album");
        sArtistAlbumsMap.put("album_key", "album_key");
        sArtistAlbumsMap.put("minyear", "MIN(year) AS minyear");
        sArtistAlbumsMap.put("maxyear", "MAX(year) AS maxyear");
        sArtistAlbumsMap.put("artist", "artist");
        sArtistAlbumsMap.put("artist_id", "artist");
        sArtistAlbumsMap.put("artist_key", "artist_key");
        sArtistAlbumsMap.put("numsongs", "count(*) AS numsongs");
        sArtistAlbumsMap.put("album_art", "album_art._data AS album_art");
        this.mSearchColsBasic[5] = this.mSearchColsBasic[5].replaceAll("%1", context.getString(R.string.artist_label));
        this.mDatabases = new HashMap<>();
        attachVolume("internal");
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_EJECT");
        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addDataScheme("file");
        context.registerReceiver(this.mUnmountReceiver, intentFilter);
        this.mExternalStoragePaths = this.mStorageManager.getVolumePaths();
        initVolumeInfo();
        String externalStorageState = Environment.getExternalStorageState();
        if ("mounted".equals(externalStorageState) || "mounted_ro".equals(externalStorageState)) {
            attachVolume("external");
        }
        HandlerThread handlerThread = new HandlerThread("thumbs thread", 10);
        handlerThread.start();
        this.mThumbHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                ThumbData thumbData;
                if (!MediaProvider.this.mMediaThumbQueue.isEmpty() && message.what == 1) {
                    MediaProvider.this.mThumbHandler.sendEmptyMessage(1);
                    message.what = 2;
                    Log.d(MediaProvider.TAG, "there are image thumbnail request exsit,extract albumart later");
                }
                if (message.what != 2) {
                    if (message.what == 1) {
                        synchronized (MediaProvider.this.mThumbRequestStack) {
                            thumbData = (ThumbData) MediaProvider.this.mThumbRequestStack.pop();
                            try {
                            } catch (Throwable th) {
                                synchronized (MediaProvider.this.mPendingThumbs) {
                                    MediaProvider.this.mPendingThumbs.remove(thumbData.path);
                                    throw th;
                                }
                            }
                        }
                        try {
                            try {
                                IoUtils.closeQuietly(MediaProvider.this.makeThumbInternal(thumbData));
                                synchronized (MediaProvider.this.mPendingThumbs) {
                                    HashSet hashSet = MediaProvider.this.mPendingThumbs;
                                    String str = thumbData.path;
                                    hashSet.remove(str);
                                    thumbData = str;
                                }
                            } catch (UnsupportedOperationException e) {
                                Log.e(MediaProvider.TAG, "ThumbHandler: UnsupportedOperationException", e);
                                synchronized (MediaProvider.this.mPendingThumbs) {
                                    HashSet hashSet2 = MediaProvider.this.mPendingThumbs;
                                    String str2 = thumbData.path;
                                    hashSet2.remove(str2);
                                    thumbData = str2;
                                }
                            }
                        } catch (SQLiteException e2) {
                            Log.e(MediaProvider.TAG, "ThumbHandler: SQLiteException", e2);
                            synchronized (MediaProvider.this.mPendingThumbs) {
                                HashSet hashSet3 = MediaProvider.this.mPendingThumbs;
                                String str3 = thumbData.path;
                                hashSet3.remove(str3);
                                thumbData = str3;
                            }
                        } catch (IllegalStateException e3) {
                            Log.e(MediaProvider.TAG, "ThumbHandler: IllegalStateException", e3);
                            synchronized (MediaProvider.this.mPendingThumbs) {
                                HashSet hashSet4 = MediaProvider.this.mPendingThumbs;
                                String str4 = thumbData.path;
                                hashSet4.remove(str4);
                                thumbData = str4;
                            }
                        }
                        return;
                    }
                    return;
                }
                synchronized (MediaProvider.this.mMediaThumbQueue) {
                    MediaProvider.this.mCurrentThumbRequest = (MediaThumbRequest) MediaProvider.this.mMediaThumbQueue.poll();
                }
                try {
                    if (MediaProvider.this.mCurrentThumbRequest == null) {
                        Log.w(MediaProvider.TAG, "Have message but no request, may have been executed!");
                        return;
                    }
                    try {
                        try {
                            try {
                                try {
                                    try {
                                        if (MediaProvider.this.mCurrentThumbRequest.mPath != null) {
                                            File file = new File(MediaProvider.this.mCurrentThumbRequest.mPath);
                                            if (!file.exists() || file.length() <= 0) {
                                                synchronized (MediaProvider.this.mMediaThumbQueue) {
                                                    Log.w(MediaProvider.TAG, "original file hasn't been stored yet: " + MediaProvider.this.mCurrentThumbRequest.mPath);
                                                }
                                            } else {
                                                MediaProvider.this.mCurrentThumbRequest.execute();
                                                synchronized (MediaProvider.this.mMediaThumbQueue) {
                                                    for (MediaThumbRequest mediaThumbRequest : MediaProvider.this.mMediaThumbQueue) {
                                                        if (mediaThumbRequest.mOrigId == MediaProvider.this.mCurrentThumbRequest.mOrigId && mediaThumbRequest.mIsVideo == MediaProvider.this.mCurrentThumbRequest.mIsVideo && mediaThumbRequest.mMagic == 0 && mediaThumbRequest.mState == MediaThumbRequest.State.WAIT) {
                                                            mediaThumbRequest.mMagic = MediaProvider.this.mCurrentThumbRequest.mMagic;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        synchronized (MediaProvider.this.mCurrentThumbRequest) {
                                            MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                                            MediaProvider.this.mCurrentThumbRequest.notifyAll();
                                        }
                                    } catch (OutOfMemoryError e4) {
                                        Log.w(MediaProvider.TAG, e4);
                                        synchronized (MediaProvider.this.mCurrentThumbRequest) {
                                            MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                                            MediaProvider.this.mCurrentThumbRequest.notifyAll();
                                        }
                                    }
                                } catch (IllegalStateException e5) {
                                    Log.e(MediaProvider.TAG, "ThumbHandler: IllegalStateException!", e5);
                                    synchronized (MediaProvider.this.mCurrentThumbRequest) {
                                        MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                                        MediaProvider.this.mCurrentThumbRequest.notifyAll();
                                    }
                                }
                            } catch (UnsupportedOperationException e6) {
                                Log.w(MediaProvider.TAG, "", e6);
                                synchronized (MediaProvider.this.mCurrentThumbRequest) {
                                    MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                                    MediaProvider.this.mCurrentThumbRequest.notifyAll();
                                }
                            }
                        } catch (IOException e7) {
                            Log.w(MediaProvider.TAG, "", e7);
                            synchronized (MediaProvider.this.mCurrentThumbRequest) {
                                MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                                MediaProvider.this.mCurrentThumbRequest.notifyAll();
                            }
                        }
                    } catch (SQLiteException e8) {
                        Log.e(MediaProvider.TAG, "ThumbHandler: SQLiteException!", e8);
                        synchronized (MediaProvider.this.mCurrentThumbRequest) {
                            MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                            MediaProvider.this.mCurrentThumbRequest.notifyAll();
                        }
                    }
                } catch (Throwable th2) {
                    synchronized (MediaProvider.this.mCurrentThumbRequest) {
                        MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                        MediaProvider.this.mCurrentThumbRequest.notifyAll();
                        throw th2;
                    }
                }
            }
        };
        if (!getHandler().hasMessages(1)) {
            getHandler().sendEmptyMessageDelayed(1, 1200000L);
        }
        return true;
    }

    private void enforceShellRestrictions() {
        if (UserHandle.getCallingAppId() == 2000 && ((UserManager) getContext().getSystemService(UserManager.class)).hasUserRestriction("no_usb_file_transfer")) {
            throw new SecurityException("Shell user cannot access files for user " + UserHandle.myUserId());
        }
    }

    protected int enforceReadPermissionInner(Uri uri, String str, IBinder iBinder) throws SecurityException {
        enforceShellRestrictions();
        return super.enforceReadPermissionInner(uri, str, iBinder);
    }

    protected int enforceWritePermissionInner(Uri uri, String str, IBinder iBinder) throws SecurityException {
        enforceShellRestrictions();
        return super.enforceWritePermissionInner(uri, str, iBinder);
    }

    private Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler() {
                @Override
                public void handleMessage(Message message) {
                    if (1 == message.what) {
                        Intent intent = new Intent();
                        Context context = MediaProvider.this.getContext();
                        intent.setAction("com.android.providers.media.ACTIVATE_MEDIAPROCESS");
                        context.sendBroadcast(intent);
                        sendEmptyMessageDelayed(1, 1200000L);
                    }
                }
            };
        }
        return this.mHandler;
    }

    private static void makePristine(SQLiteDatabase sQLiteDatabase) {
        Cursor cursorQuery = sQLiteDatabase.query("sqlite_master", new String[]{"name"}, "type is 'trigger'", null, null, null, null);
        while (cursorQuery.moveToNext()) {
            sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + cursorQuery.getString(0));
        }
        cursorQuery.close();
        Cursor cursorQuery2 = sQLiteDatabase.query("sqlite_master", new String[]{"name"}, "type is 'view'", null, null, null, null);
        while (cursorQuery2.moveToNext()) {
            sQLiteDatabase.execSQL("DROP VIEW IF EXISTS " + cursorQuery2.getString(0));
        }
        cursorQuery2.close();
        Cursor cursorQuery3 = sQLiteDatabase.query("sqlite_master", new String[]{"name"}, "type is 'index'", null, null, null, null);
        while (cursorQuery3.moveToNext()) {
            sQLiteDatabase.execSQL("DROP INDEX IF EXISTS " + cursorQuery3.getString(0));
        }
        cursorQuery3.close();
        Cursor cursorQuery4 = sQLiteDatabase.query("sqlite_master", new String[]{"name"}, "type is 'table'", null, null, null, null);
        while (cursorQuery4.moveToNext()) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS " + cursorQuery4.getString(0));
        }
        cursorQuery4.close();
    }

    private static void createLatestSchema(SQLiteDatabase sQLiteDatabase, boolean z) {
        makePristine(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TABLE android_metadata (locale TEXT)");
        sQLiteDatabase.execSQL("CREATE TABLE thumbnails (_id INTEGER PRIMARY KEY,_data TEXT,image_id INTEGER,kind INTEGER,width INTEGER,height INTEGER)");
        sQLiteDatabase.execSQL("CREATE TABLE artists (artist_id INTEGER PRIMARY KEY,artist_key TEXT NOT NULL UNIQUE,artist TEXT NOT NULL)");
        sQLiteDatabase.execSQL("CREATE TABLE albums (album_id INTEGER PRIMARY KEY,album_key TEXT NOT NULL UNIQUE,album TEXT NOT NULL)");
        sQLiteDatabase.execSQL("CREATE TABLE album_art (album_id INTEGER PRIMARY KEY,_data TEXT)");
        sQLiteDatabase.execSQL("CREATE TABLE videothumbnails (_id INTEGER PRIMARY KEY,_data TEXT,video_id INTEGER,kind INTEGER,width INTEGER,height INTEGER)");
        sQLiteDatabase.execSQL("CREATE TABLE files (_id INTEGER PRIMARY KEY AUTOINCREMENT,_data TEXT UNIQUE COLLATE NOCASE,_size INTEGER,format INTEGER,parent INTEGER,date_added INTEGER,date_modified INTEGER,mime_type TEXT,title TEXT,description TEXT,_display_name TEXT,picasa_id TEXT,orientation INTEGER,latitude DOUBLE,longitude DOUBLE,datetaken INTEGER,mini_thumb_magic INTEGER,bucket_id TEXT,bucket_display_name TEXT,isprivate INTEGER,title_key TEXT,artist_id INTEGER,album_id INTEGER,composer TEXT,track INTEGER,year INTEGER CHECK(year!=0),is_ringtone INTEGER,is_music INTEGER,is_alarm INTEGER,is_notification INTEGER,is_podcast INTEGER,album_artist TEXT,duration INTEGER,bookmark INTEGER,artist TEXT,album TEXT,resolution TEXT,tags TEXT,category TEXT,language TEXT,mini_thumb_data TEXT,name TEXT,media_type INTEGER,old_id INTEGER,is_drm INTEGER,width INTEGER, height INTEGER)");
        sQLiteDatabase.execSQL("CREATE TABLE log (time DATETIME, message TEXT)");
        if (!z) {
            sQLiteDatabase.execSQL("CREATE TABLE audio_genres (_id INTEGER PRIMARY KEY,name TEXT NOT NULL)");
            sQLiteDatabase.execSQL("CREATE TABLE audio_genres_map (_id INTEGER PRIMARY KEY,audio_id INTEGER NOT NULL,genre_id INTEGER NOT NULL,UNIQUE (audio_id,genre_id) ON CONFLICT IGNORE)");
            sQLiteDatabase.execSQL("CREATE TABLE audio_playlists_map (_id INTEGER PRIMARY KEY,audio_id INTEGER NOT NULL,playlist_id INTEGER NOT NULL,play_order INTEGER NOT NULL)");
            sQLiteDatabase.execSQL("CREATE TRIGGER audio_genres_cleanup DELETE ON audio_genres BEGIN DELETE FROM audio_genres_map WHERE genre_id = old._id;END");
            sQLiteDatabase.execSQL("CREATE TRIGGER audio_playlists_cleanup DELETE ON files WHEN old.media_type=4 BEGIN DELETE FROM audio_playlists_map WHERE playlist_id = old._id;SELECT _DELETE_FILE(old._data);END");
            sQLiteDatabase.execSQL("CREATE TRIGGER files_cleanup DELETE ON files BEGIN SELECT _OBJECT_REMOVED(old._id);END");
            sQLiteDatabase.execSQL("CREATE VIEW audio_playlists AS SELECT _id,_data,name,date_added,date_modified FROM files WHERE media_type=4");
        }
        sQLiteDatabase.execSQL("CREATE INDEX image_id_index on thumbnails(image_id)");
        sQLiteDatabase.execSQL("CREATE INDEX album_idx on albums(album)");
        sQLiteDatabase.execSQL("CREATE INDEX albumkey_index on albums(album_key)");
        sQLiteDatabase.execSQL("CREATE INDEX artist_idx on artists(artist)");
        sQLiteDatabase.execSQL("CREATE INDEX artistkey_index on artists(artist_key)");
        sQLiteDatabase.execSQL("CREATE INDEX video_id_index on videothumbnails(video_id)");
        sQLiteDatabase.execSQL("CREATE INDEX album_id_idx ON files(album_id)");
        sQLiteDatabase.execSQL("CREATE INDEX artist_id_idx ON files(artist_id)");
        sQLiteDatabase.execSQL("CREATE INDEX bucket_index on files(bucket_id,media_type,datetaken, _id)");
        sQLiteDatabase.execSQL("CREATE INDEX bucket_name on files(bucket_id,media_type,bucket_display_name)");
        sQLiteDatabase.execSQL("CREATE INDEX format_index ON files(format)");
        sQLiteDatabase.execSQL("CREATE INDEX media_type_index ON files(media_type)");
        sQLiteDatabase.execSQL("CREATE INDEX parent_index ON files(parent)");
        sQLiteDatabase.execSQL("CREATE INDEX path_index ON files(_data)");
        sQLiteDatabase.execSQL("CREATE INDEX sort_index ON files(datetaken ASC, _id ASC)");
        sQLiteDatabase.execSQL("CREATE INDEX title_idx ON files(title)");
        sQLiteDatabase.execSQL("CREATE INDEX titlekey_index ON files(title_key)");
        sQLiteDatabase.execSQL("CREATE VIEW audio_meta AS SELECT _id,_data,_display_name,_size,mime_type,date_added,is_drm,date_modified,title,title_key,duration,artist_id,composer,album_id,track,year,is_ringtone,is_music,is_alarm,is_notification,is_podcast,bookmark,album_artist FROM files WHERE media_type=2");
        sQLiteDatabase.execSQL("CREATE VIEW artists_albums_map AS SELECT DISTINCT artist_id, album_id FROM audio_meta");
        sQLiteDatabase.execSQL("CREATE VIEW audio as SELECT * FROM audio_meta LEFT OUTER JOIN artists ON audio_meta.artist_id=artists.artist_id LEFT OUTER JOIN albums ON audio_meta.album_id=albums.album_id");
        sQLiteDatabase.execSQL("CREATE VIEW album_info AS SELECT audio.album_id AS _id, album, album_key, MIN(year) AS minyear, MAX(year) AS maxyear, artist, artist_id, artist_key, count(*) AS numsongs,album_art._data AS album_art FROM audio LEFT OUTER JOIN album_art ON audio.album_id=album_art.album_id WHERE is_music=1 GROUP BY audio.album_id;");
        sQLiteDatabase.execSQL("CREATE VIEW searchhelpertitle AS SELECT * FROM audio ORDER BY title_key");
        sQLiteDatabase.execSQL("CREATE VIEW artist_info AS SELECT artist_id AS _id, artist, artist_key, COUNT(DISTINCT album_key) AS number_of_albums, COUNT(*) AS number_of_tracks FROM audio WHERE is_music=1 GROUP BY artist_key");
        sQLiteDatabase.execSQL("CREATE VIEW search AS SELECT _id,'artist' AS mime_type,artist,NULL AS album,NULL AS title,artist AS text1,NULL AS text2,number_of_albums AS data1,number_of_tracks AS data2,artist_key AS match,'content://media/external/audio/artists/'||_id AS suggest_intent_data,1 AS grouporder FROM artist_info WHERE (artist!='<unknown>') UNION ALL SELECT _id,'album' AS mime_type,artist,album,NULL AS title,album AS text1,artist AS text2,NULL AS data1,NULL AS data2,artist_key||' '||album_key AS match,'content://media/external/audio/albums/'||_id AS suggest_intent_data,2 AS grouporder FROM album_info WHERE (album!='<unknown>') UNION ALL SELECT searchhelpertitle._id AS _id,mime_type,artist,album,title,title AS text1,artist AS text2,NULL AS data1,NULL AS data2,artist_key||' '||album_key||' '||title_key AS match,'content://media/external/audio/media/'||searchhelpertitle._id AS suggest_intent_data,3 AS grouporder FROM searchhelpertitle WHERE (title != '')");
        if (!z) {
            sQLiteDatabase.execSQL("CREATE VIEW audio_genres_map_noid AS SELECT audio_id,genre_id FROM audio_genres_map");
        }
        sQLiteDatabase.execSQL("CREATE VIEW images AS SELECT _id,_data,_size,_display_name,mime_type,title,date_added,date_modified,description,picasa_id,isprivate,latitude,longitude,datetaken,orientation,mini_thumb_magic,bucket_id,bucket_display_name,width,height,is_drm,drm_content_uri,drm_offset,drm_dataLen,drm_rights_issuer,drm_content_name,drm_content_description,drm_content_vendor,drm_icon_uri,drm_method FROM files WHERE media_type=1;");
        sQLiteDatabase.execSQL("CREATE VIEW video AS SELECT _id,_data,_display_name,_size,mime_type,date_added,date_modified,title,duration,artist,album,resolution,description,isprivate,tags,category,language,mini_thumb_data,latitude,longitude,datetaken,mini_thumb_magic,bucket_id,bucket_display_name,bookmark,width,height,is_drm,drm_content_uri,drm_offset,drm_dataLen,drm_rights_issuer,drm_content_name,drm_content_description,drm_content_vendor,drm_icon_uri,drm_method FROM files WHERE media_type=3;");
        sQLiteDatabase.execSQL("CREATE TRIGGER albumart_cleanup1 DELETE ON albums BEGIN DELETE FROM album_art WHERE album_id = old.album_id;END");
        sQLiteDatabase.execSQL("CREATE TRIGGER albumart_cleanup2 DELETE ON album_art BEGIN SELECT _DELETE_FILE(old._data);END");
    }

    private static void updateFromKKSchema(SQLiteDatabase sQLiteDatabase, boolean z, int i) {
        sQLiteDatabase.execSQL("DELETE from albums");
        sQLiteDatabase.execSQL("DELETE from artists");
        sQLiteDatabase.execSQL("UPDATE files SET date_modified=0;");
    }

    private static void updateDatabase(Context context, SQLiteDatabase sQLiteDatabase, boolean z, int i, int i2) throws Throwable {
        int databaseVersion = getDatabaseVersion(context);
        if (i2 != databaseVersion) {
            Log.e(TAG, "Illegal update request. Got " + i2 + ", expected " + databaseVersion);
            throw new IllegalArgumentException();
        }
        if (i > i2) {
            Log.e(TAG, "Illegal update request: can't downgrade from " + i + " to " + i2 + ". Did you forget to wipe data?");
            throw new IllegalArgumentException();
        }
        long jCurrentTimeMicro = SystemClock.currentTimeMicro();
        if (i < 700) {
            createLatestSchema(sQLiteDatabase, z);
        }
        if (i < 800) {
            updateFromKKSchema(sQLiteDatabase, z, i);
        }
        if (i < 802) {
            String[] strArrSplit = ",drm_content_uri,drm_offset,drm_dataLen,drm_rights_issuer,drm_content_name,drm_content_description,drm_content_vendor,drm_icon_uri,drm_method".replaceFirst(",", "").split(",");
            ArrayList arrayList = new ArrayList();
            arrayList.add("drm_content_uri TEXT;");
            arrayList.add("drm_offset INTEGER;");
            arrayList.add("drm_dataLen INTEGER;");
            arrayList.add("drm_rights_issuer TEXT;");
            arrayList.add("drm_content_name TEXT;");
            arrayList.add("drm_content_description TEXT;");
            arrayList.add("drm_content_vendor TEXT;");
            arrayList.add("drm_icon_uri TEXT;");
            arrayList.add("drm_method INTEGER;");
            checkColumnsExist(sQLiteDatabase, strArrSplit, arrayList);
            ArrayList arrayList2 = new ArrayList();
            arrayList2.add("file_name TEXT;");
            arrayList2.add("file_type INTEGER DEFAULT 0;");
            checkColumnsExist(sQLiteDatabase, new String[]{"file_name", "file_type"}, arrayList2);
            sQLiteDatabase.execSQL("DROP VIEW IF EXISTS video");
            sQLiteDatabase.execSQL("CREATE VIEW video AS SELECT _id,_data,_display_name,_size,mime_type,date_added,date_modified,title,duration,artist,album,resolution,description,isprivate,tags,category,language,mini_thumb_data,latitude,longitude,datetaken,mini_thumb_magic,bucket_id,bucket_display_name,bookmark,width,height,is_drm,drm_content_uri,drm_offset,drm_dataLen,drm_rights_issuer,drm_content_name,drm_content_description,drm_content_vendor,drm_icon_uri,drm_method FROM files WHERE media_type=3;");
            sQLiteDatabase.execSQL("DROP VIEW IF EXISTS audio_meta");
            sQLiteDatabase.execSQL("CREATE VIEW audio_meta AS SELECT _id,_data,_display_name,_size,mime_type,date_added,is_drm,date_modified,title,title_key,duration,artist_id,composer,album_id,track,year,is_ringtone,is_music,is_alarm,is_notification,is_podcast,bookmark,album_artist,drm_content_uri,drm_offset,drm_dataLen,drm_rights_issuer,drm_content_name,drm_content_description,drm_content_vendor,drm_icon_uri,drm_method FROM files WHERE media_type=2;");
            sQLiteDatabase.execSQL("DROP VIEW IF EXISTS images");
            sQLiteDatabase.execSQL("CREATE VIEW images AS SELECT _id,_data,_size,_display_name,mime_type,title,date_added,date_modified,description,picasa_id,isprivate,latitude,longitude,datetaken,orientation,mini_thumb_magic,bucket_id,bucket_display_name,width,height,is_drm,drm_content_uri,drm_offset,drm_dataLen,drm_rights_issuer,drm_content_name,drm_content_description,drm_content_vendor,drm_icon_uri,drm_method FROM files WHERE media_type=1;");
            sQLiteDatabase.execSQL("DROP VIEW IF EXISTS artists_albums_map");
            sQLiteDatabase.execSQL("CREATE VIEW artists_albums_map AS SELECT DISTINCT artist_id, album_id FROM audio_meta");
            sQLiteDatabase.execSQL("DROP VIEW IF EXISTS album_info");
            sQLiteDatabase.execSQL("CREATE VIEW album_info AS SELECT audio.album_id AS _id, album, album_key, MIN(year) AS minyear, MAX(year) AS maxyear, artist, artist_id, artist_key, count(*) AS numsongs,album_art._data AS album_art FROM audio LEFT OUTER JOIN album_art ON audio.album_id=album_art.album_id WHERE is_music=1 GROUP BY audio.album_id;");
            sQLiteDatabase.execSQL("DROP VIEW IF EXISTS artist_info");
            sQLiteDatabase.execSQL("CREATE VIEW artist_info AS SELECT artist_id AS _id, artist, artist_key, COUNT(DISTINCT album_key) AS number_of_albums, COUNT(*) AS number_of_tracks FROM audio WHERE is_music=1 GROUP BY artist_key");
            sQLiteDatabase.execSQL("DROP VIEW IF EXISTS search");
            sQLiteDatabase.execSQL("CREATE VIEW search AS SELECT _id,'artist' AS mime_type,artist,NULL AS album,NULL AS title,artist AS text1,NULL AS text2,number_of_albums AS data1,number_of_tracks AS data2,artist_key AS match,'content://media/external/audio/artists/'||_id AS suggest_intent_data,1 AS grouporder FROM artist_info WHERE (artist!='<unknown>') UNION ALL SELECT _id,'album' AS mime_type,artist,album,NULL AS title,album AS text1,artist AS text2,NULL AS data1,NULL AS data2,artist_key||' '||album_key AS match,'content://media/external/audio/albums/'||_id AS suggest_intent_data,2 AS grouporder FROM album_info WHERE (album!='<unknown>') UNION ALL SELECT searchhelpertitle._id AS _id,mime_type,artist,album,title,title AS text1,artist AS text2,NULL AS data1,NULL AS data2,artist_key||' '||album_key||' '||title_key AS match,'content://media/external/audio/media/'||searchhelpertitle._id AS suggest_intent_data,3 AS grouporder FROM searchhelpertitle WHERE (title != '')");
        }
        if (i < 803) {
            sQLiteDatabase.execSQL("DROP VIEW IF EXISTS video");
            sQLiteDatabase.execSQL("CREATE VIEW video AS SELECT _id,_data,_display_name,_size,mime_type,date_added,date_modified,title,duration,artist,album,resolution,description,isprivate,tags,category,language,mini_thumb_data,latitude,longitude,datetaken,mini_thumb_magic,bucket_id,bucket_display_name,bookmark,width,height,is_drm,drm_content_uri,drm_offset,drm_dataLen,drm_rights_issuer,drm_content_name,drm_content_description,drm_content_vendor,drm_icon_uri,drm_method,orientation FROM files WHERE media_type=3;");
        }
        if (i < 804) {
            ArrayList arrayList3 = new ArrayList();
            arrayList3.add("camera_refocus INTEGER DEFAULT 0;");
            checkColumnsExist(sQLiteDatabase, new String[]{"camera_refocus"}, arrayList3);
            sQLiteDatabase.execSQL("DROP VIEW IF EXISTS images");
            sQLiteDatabase.execSQL("CREATE VIEW images AS SELECT _id,_data,_size,_display_name,mime_type,title,date_added,date_modified,description,picasa_id,isprivate,latitude,longitude,datetaken,orientation,mini_thumb_magic,bucket_id,bucket_display_name,width,height,is_drm,drm_content_uri,drm_offset,drm_dataLen,drm_rights_issuer,drm_content_name,drm_content_description,drm_content_vendor,drm_icon_uri,drm_method,camera_refocus FROM files WHERE media_type=1;");
        }
        if (i < 902) {
            sQLiteDatabase.execSQL("ALTER TABLE files ADD COLUMN title_resource_uri TEXT DEFAULT NULL");
        }
        sanityCheck(sQLiteDatabase, i);
        long jCurrentTimeMicro2 = (SystemClock.currentTimeMicro() - jCurrentTimeMicro) / 1000000;
        logToDb(sQLiteDatabase, "Database upgraded from version " + i + " to " + i2 + " in " + jCurrentTimeMicro2 + " seconds");
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("updateDatabase<<< use ");
        sb.append(jCurrentTimeMicro2);
        sb.append("s");
        Log.d(str, sb.toString());
    }

    static void logToDb(SQLiteDatabase sQLiteDatabase, String str) {
        try {
            sQLiteDatabase.execSQL("INSERT OR REPLACE INTO log (time,message) VALUES (strftime('%Y-%m-%d %H:%M:%f','now'),?);", new String[]{str});
            sQLiteDatabase.execSQL("DELETE FROM log WHERE rowid IN (SELECT rowid FROM log ORDER BY rowid DESC LIMIT 500,-1);");
        } catch (SQLiteException e) {
            Log.e(TAG, "logToDb: msg=" + str, e);
        }
    }

    private static void sanityCheck(SQLiteDatabase sQLiteDatabase, int i) throws Throwable {
        Cursor cursorQuery;
        Cursor cursorQuery2;
        try {
            cursorQuery = sQLiteDatabase.query("audio_meta", new String[]{"count(*)"}, null, null, null, null, null);
            try {
                cursorQuery2 = sQLiteDatabase.query("audio_meta", new String[]{"count(distinct _data)"}, null, null, null, null, null);
                try {
                    cursorQuery.moveToFirst();
                    cursorQuery2.moveToFirst();
                    int i2 = cursorQuery.getInt(0);
                    int i3 = cursorQuery2.getInt(0);
                    if (i2 != i3) {
                        Log.e(TAG, "audio_meta._data column is not unique while upgrading from schema " + i + " : " + i2 + "/" + i3);
                        sQLiteDatabase.execSQL("DELETE FROM audio_meta;");
                    }
                    IoUtils.closeQuietly(cursorQuery);
                    IoUtils.closeQuietly(cursorQuery2);
                } catch (Throwable th) {
                    th = th;
                    IoUtils.closeQuietly(cursorQuery);
                    IoUtils.closeQuietly(cursorQuery2);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                cursorQuery2 = null;
            }
        } catch (Throwable th3) {
            th = th3;
            cursorQuery = null;
            cursorQuery2 = null;
        }
    }

    private static void computeBucketValues(String str, ContentValues contentValues) {
        File parentFile = new File(str).getParentFile();
        if (parentFile == null) {
            parentFile = new File("/");
        }
        String lowerCase = parentFile.toString().toLowerCase(Locale.ENGLISH);
        String name = parentFile.getName();
        contentValues.put("bucket_id", Integer.valueOf(lowerCase.hashCode()));
        contentValues.put("bucket_display_name", name);
    }

    private static void computeDisplayName(String str, ContentValues contentValues) {
        String string = str == null ? "" : str.toString();
        int iLastIndexOf = string.lastIndexOf(47);
        if (iLastIndexOf >= 0) {
            string = string.substring(iLastIndexOf + 1);
        }
        Log.d(TAG, "[computeDisplayName] _display_name = " + string);
        contentValues.put("_display_name", string);
    }

    private static void computeTakenTime(ContentValues contentValues) {
        Long asLong;
        if (!contentValues.containsKey("datetaken") && (asLong = contentValues.getAsLong("date_modified")) != null) {
            contentValues.put("datetaken", Long.valueOf(asLong.longValue() * 1000));
        }
    }

    private boolean waitForThumbnailReady(Uri uri) throws Throwable {
        Cursor cursorQuery = query(uri, new String[]{"_id", "_data", "mini_thumb_magic"}, null, null, null);
        boolean z = false;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    cursorQuery.getLong(0);
                    MediaThumbRequest mediaThumbRequestRequestMediaThumbnail = requestMediaThumbnail(cursorQuery.getString(1), uri, 5, cursorQuery.getLong(2));
                    if (mediaThumbRequestRequestMediaThumbnail != null) {
                        synchronized (mediaThumbRequestRequestMediaThumbnail) {
                            while (mediaThumbRequestRequestMediaThumbnail.mState == MediaThumbRequest.State.WAIT) {
                                try {
                                    mediaThumbRequestRequestMediaThumbnail.wait();
                                } catch (InterruptedException e) {
                                    Log.w(TAG, "", e);
                                }
                            }
                            if (mediaThumbRequestRequestMediaThumbnail.mState == MediaThumbRequest.State.DONE) {
                                z = true;
                            }
                        }
                    }
                }
            } finally {
                IoUtils.closeQuietly(cursorQuery);
            }
        }
        return z;
    }

    private boolean matchThumbRequest(MediaThumbRequest mediaThumbRequest, int i, long j, long j2, boolean z) {
        boolean z2 = j == -1;
        boolean z3 = j2 == -1;
        if (mediaThumbRequest.mCallingPid != i) {
            return false;
        }
        if (z3 || mediaThumbRequest.mGroupId == j2) {
            return (z2 || mediaThumbRequest.mOrigId == j) && mediaThumbRequest.mIsVideo == z;
        }
        return false;
    }

    private boolean queryThumbnail(SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String str, String str2, boolean z) throws Throwable {
        PriorityQueue<MediaThumbRequest> priorityQueue;
        sQLiteQueryBuilder.setTables(str);
        if (z) {
            sQLiteQueryBuilder.appendWhere("_id = " + uri.getPathSegments().get(3));
            return true;
        }
        String queryParameter = uri.getQueryParameter("orig_id");
        if (queryParameter == null) {
            Log.w(TAG, "queryThumbnail: Null origId! uri=" + uri);
            return true;
        }
        boolean zEquals = "1".equals(uri.getQueryParameter("blocking"));
        boolean zEquals2 = "1".equals(uri.getQueryParameter("cancel"));
        Uri uriBuild = uri.buildUpon().encodedPath(uri.getPath().replaceFirst("thumbnails", "media")).appendPath(queryParameter).build();
        if (zEquals && !waitForThumbnailReady(uriBuild)) {
            Log.w(TAG, "queryThumbnail: failed! uri=" + uriBuild);
            Log.w(TAG, "original media doesn't exist or it's canceled.");
            return false;
        }
        if (zEquals2) {
            String queryParameter2 = uri.getQueryParameter("group_id");
            boolean zEquals3 = "video".equals(uri.getPathSegments().get(1));
            int callingPid = Binder.getCallingPid();
            try {
                long j = Long.parseLong(queryParameter);
                long j2 = Long.parseLong(queryParameter2);
                PriorityQueue<MediaThumbRequest> priorityQueue2 = this.mMediaThumbQueue;
                synchronized (priorityQueue2) {
                    try {
                        if (this.mCurrentThumbRequest != null) {
                            priorityQueue = priorityQueue2;
                            try {
                                if (matchThumbRequest(this.mCurrentThumbRequest, callingPid, j, j2, zEquals3)) {
                                    synchronized (this.mCurrentThumbRequest) {
                                        this.mCurrentThumbRequest.mState = MediaThumbRequest.State.CANCEL;
                                        this.mCurrentThumbRequest.notifyAll();
                                    }
                                }
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } else {
                            priorityQueue = priorityQueue2;
                        }
                        Iterator<MediaThumbRequest> it = this.mMediaThumbQueue.iterator();
                        while (it.hasNext()) {
                            MediaThumbRequest next = it.next();
                            Iterator<MediaThumbRequest> it2 = it;
                            if (matchThumbRequest(next, callingPid, j, j2, zEquals3)) {
                                synchronized (next) {
                                    next.mState = MediaThumbRequest.State.CANCEL;
                                    next.notifyAll();
                                }
                                this.mMediaThumbQueue.remove(next);
                            }
                            it = it2;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        priorityQueue = priorityQueue2;
                    }
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "queryThumbnail: NumberFormatException!", e);
                return false;
            }
        }
        if (queryParameter != null) {
            sQLiteQueryBuilder.appendWhere(str2 + " = " + queryParameter);
            return true;
        }
        return true;
    }

    @Override
    public Uri canonicalize(Uri uri) throws Throwable {
        if (URI_MATCHER.match(uri) != 101) {
            return null;
        }
        Cursor cursorQuery = query(uri, null, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() == 1 && cursorQuery.moveToNext()) {
                    Uri.Builder builderBuildUpon = uri.buildUpon();
                    builderBuildUpon.appendQueryParameter("canonical", "1");
                    String defaultTitleFromCursor = getDefaultTitleFromCursor(cursorQuery);
                    IoUtils.closeQuietly(cursorQuery);
                    if (TextUtils.isEmpty(defaultTitleFromCursor)) {
                        return null;
                    }
                    builderBuildUpon.appendQueryParameter("title", defaultTitleFromCursor);
                    return builderBuildUpon.build();
                }
            } finally {
                IoUtils.closeQuietly(cursorQuery);
            }
        }
        return null;
    }

    @Override
    public Uri uncanonicalize(Uri uri) throws Throwable {
        String queryParameter;
        if (uri == null || !"1".equals(uri.getQueryParameter("canonical"))) {
            return uri;
        }
        if (URI_MATCHER.match(uri) != 101 || (queryParameter = uri.getQueryParameter("title")) == null) {
            return null;
        }
        Uri uriBuild = uri.buildUpon().clearQuery().build();
        Cursor cursorQuery = query(uriBuild, null, null, null, null);
        try {
            cursorQuery.getColumnIndex("title");
            if (cursorQuery != null && cursorQuery.getCount() == 1 && cursorQuery.moveToNext() && queryParameter.equals(getDefaultTitleFromCursor(cursorQuery))) {
                IoUtils.closeQuietly(cursorQuery);
                return uriBuild;
            }
            IoUtils.closeQuietly(cursorQuery);
            Uri contentUri = MediaStore.Audio.Media.getContentUri(uriBuild.getPathSegments().get(0));
            Cursor cursorQuery2 = query(contentUri, null, "title=?", new String[]{queryParameter}, null);
            if (cursorQuery2 == null) {
                IoUtils.closeQuietly(cursorQuery2);
                return null;
            }
            try {
                if (!cursorQuery2.moveToNext()) {
                    IoUtils.closeQuietly(cursorQuery2);
                    return null;
                }
                Uri uriWithAppendedId = ContentUris.withAppendedId(contentUri, cursorQuery2.getLong(cursorQuery2.getColumnIndex("_id")));
                IoUtils.closeQuietly(cursorQuery2);
                return uriWithAppendedId;
            } catch (Throwable th) {
                th = th;
                cursorQuery = cursorQuery2;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        IoUtils.closeQuietly(cursorQuery);
        throw th;
    }

    private Uri safeUncanonicalize(Uri uri) throws Throwable {
        Uri uriUncanonicalize = uncanonicalize(uri);
        if (uriUncanonicalize != null) {
            return uriUncanonicalize;
        }
        return uri;
    }

    private String getUriString(Uri uri) {
        String string = "";
        if (uri == null) {
            Log.d(TAG, "getUriString()-uri is null");
        } else {
            string = uri.toString();
            if (string.isEmpty()) {
                Log.d(TAG, "getUriString()-uri is empty");
            }
        }
        return string;
    }

    private void initVolumeInfo() {
    }

    private String generateStorageIdFilter(Uri uri) {
        String str = "";
        String uriString = getUriString(uri);
        if (uriString.isEmpty()) {
            return "";
        }
        String queryParameter = uri.getQueryParameter("force");
        int i = 0;
        if (uriString.startsWith("content://media/external/")) {
            i = 1;
        }
        int size = sExternalVolumeIdList.size();
        if (size <= 0 && MediaUtils.LOG_QUERY) {
            Log.d(TAG, "generateStorageIdFilter() sExternalVolumeIdList is empty");
        }
        while (i < size) {
            str = str + "storage_id='" + sExternalVolumeIdList.get(i) + "'";
            if (isPreScanning() && (queryParameter == null || !queryParameter.equals("1"))) {
                break;
            }
            if (size > 1 && i < size - 1) {
                str = str + " OR ";
            }
            i++;
        }
        if (!str.equals("")) {
            str = "(" + str + ")";
        }
        if (MediaUtils.LOG_QUERY) {
            Log.d(TAG, "generateStorageIdFilter() forceQuerying = " + queryParameter + ", queryFilter = " + str);
        }
        return str;
    }

    private String generatePlaylistQueryFilter(Uri uri) {
        if (getUriString(uri).isEmpty()) {
            return "";
        }
        String strGenerateStorageIdFilter = generateStorageIdFilter(uri);
        if (strGenerateStorageIdFilter.isEmpty()) {
            Log.d(TAG, "generatePlaylistQueryFilter()-queryFilter is empty");
            return strGenerateStorageIdFilter;
        }
        return "audio_id in (SELECT _id FROM audio WHERE " + strGenerateStorageIdFilter + ")";
    }

    private String generateArtistsQueryFilter(Uri uri) {
        if (getUriString(uri).isEmpty()) {
            return "";
        }
        String strGenerateStorageIdFilter = generateStorageIdFilter(uri);
        if (strGenerateStorageIdFilter.isEmpty()) {
            Log.d(TAG, "generateArtistsQueryFilter()-queryFilter is empty");
            return strGenerateStorageIdFilter;
        }
        return "audio.album_id in (SELECT album_id FROM artists_albums_map WHERE " + strGenerateStorageIdFilter + ")";
    }

    private String generateAlbumArtQueryFilter(Uri uri) {
        if (getUriString(uri).isEmpty()) {
            return "";
        }
        String strGenerateStorageIdFilter = generateStorageIdFilter(uri);
        if (strGenerateStorageIdFilter.isEmpty()) {
            Log.d(TAG, "generateAlbumArtQueryFilter()-queryFilter is empty");
            return strGenerateStorageIdFilter;
        }
        return "album_id in (SELECT album_id FROM audio WHERE " + strGenerateStorageIdFilter + ")";
    }

    private String generateGenresQueryFilter(Uri uri) {
        if (getUriString(uri).isEmpty()) {
            return "";
        }
        String strGenerateStorageIdFilter = generateStorageIdFilter(uri);
        if (strGenerateStorageIdFilter.isEmpty()) {
            Log.d(TAG, "generateGenresQueryFilter()-queryFilter is empty");
            return strGenerateStorageIdFilter;
        }
        return "audio_id in (SELECT audio_id FROM audio WHERE " + strGenerateStorageIdFilter + ")";
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) throws Throwable {
        String[] strArr3;
        String[] strArr4;
        String str3;
        String[] strArr5;
        String str4;
        Cursor cursorQuery;
        String str5;
        String[] strArr6;
        String queryParameter;
        boolean z;
        String str6;
        String strGenerateArtistsQueryFilter;
        boolean z2;
        String[] strArr7 = strArr;
        Uri uriSafeUncanonicalize = safeUncanonicalize(uri);
        int iMatch = URI_MATCHER.match(uriSafeUncanonicalize);
        ArrayList arrayList = new ArrayList();
        if (iMatch == 500) {
            if (this.mMediaScannerVolume == null) {
                return null;
            }
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"volume"});
            matrixCursor.addRow(new String[]{this.mMediaScannerVolume});
            return matrixCursor;
        }
        if (iMatch == 1201) {
            if (this.mMtpTransferFile == null) {
                return null;
            }
            MatrixCursor matrixCursor2 = new MatrixCursor(new String[]{"mtp_transfer_file_path"});
            matrixCursor2.addRow(new String[]{this.mMtpTransferFile});
            return matrixCursor2;
        }
        if (iMatch == 600) {
            MatrixCursor matrixCursor3 = new MatrixCursor(new String[]{"fsid"});
            matrixCursor3.addRow(new Integer[]{Integer.valueOf(this.mVolumeId)});
            return matrixCursor3;
        }
        if (iMatch == 601) {
            MatrixCursor matrixCursor4 = new MatrixCursor(new String[]{"version"});
            matrixCursor4.addRow(new Integer[]{Integer.valueOf(getDatabaseVersion(getContext()))});
            return matrixCursor4;
        }
        DatabaseHelper databaseForUri = getDatabaseForUri(uriSafeUncanonicalize);
        if (databaseForUri == null) {
            return null;
        }
        databaseForUri.mNumQueries++;
        try {
            SQLiteDatabase readableDatabase = databaseForUri.getReadableDatabase();
            if (readableDatabase == null) {
                return null;
            }
            SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
            String queryParameter2 = uriSafeUncanonicalize.getQueryParameter("limit");
            String queryParameter3 = uriSafeUncanonicalize.getQueryParameter("filter");
            if (queryParameter3 != null) {
                String strTrim = Uri.decode(queryParameter3).trim();
                if (TextUtils.isEmpty(strTrim)) {
                    strArr3 = null;
                } else {
                    String[] strArrSplit = strTrim.split(" ");
                    strArr3 = new String[strArrSplit.length];
                    int i = 0;
                    while (i < strArrSplit.length) {
                        strArr3[i] = MediaStore.Audio.keyFor(strArrSplit[i]).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                        i++;
                        strArrSplit = strArrSplit;
                    }
                }
            }
            if (uriSafeUncanonicalize.getQueryParameter("distinct") != null) {
                sQLiteQueryBuilder.setDistinct(true);
            }
            boolean zIsPermitedAccessDrm = DrmHelper.isPermitedAccessDrm(getContext(), Binder.getCallingPid());
            String strGenerateStorageIdFilter = generateStorageIdFilter(uriSafeUncanonicalize);
            if (MediaUtils.LOG_QUERY) {
                String str7 = TAG;
                StringBuilder sb = new StringBuilder();
                strArr4 = strArr3;
                sb.append("query: table=");
                sb.append(iMatch);
                Log.v(str7, sb.toString());
            } else {
                strArr4 = strArr3;
            }
            if (iMatch != 120) {
                switch (iMatch) {
                    case 1:
                        str3 = queryParameter2;
                        sQLiteQueryBuilder.setTables("images");
                        if (!strGenerateStorageIdFilter.isEmpty()) {
                            sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                            if (!zIsPermitedAccessDrm) {
                                sQLiteQueryBuilder.appendWhere(" AND (is_drm=0 OR is_drm IS NULL)");
                            }
                        } else if (!zIsPermitedAccessDrm) {
                            sQLiteQueryBuilder.appendWhere("(is_drm=0 OR is_drm IS NULL)");
                        }
                        if (uriSafeUncanonicalize.getQueryParameter("distinct") != null) {
                            sQLiteQueryBuilder.setDistinct(true);
                        }
                        break;
                    case 2:
                        str3 = queryParameter2;
                        sQLiteQueryBuilder.setTables("images");
                        if (uriSafeUncanonicalize.getQueryParameter("distinct") != null) {
                            sQLiteQueryBuilder.setDistinct(true);
                        }
                        sQLiteQueryBuilder.appendWhere("_id=?");
                        arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                        if (!strGenerateStorageIdFilter.isEmpty()) {
                            sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                        }
                        break;
                    case 3:
                        str3 = queryParameter2;
                        z = false;
                        if (!queryThumbnail(sQLiteQueryBuilder, uriSafeUncanonicalize, "thumbnails", "image_id", z)) {
                            return null;
                        }
                        break;
                    case 4:
                        str3 = queryParameter2;
                        z = true;
                        if (!queryThumbnail(sQLiteQueryBuilder, uriSafeUncanonicalize, "thumbnails", "image_id", z)) {
                        }
                        break;
                    default:
                        switch (iMatch) {
                            case 100:
                                str3 = queryParameter2;
                                String[] strArr8 = strArr4;
                                if (strArr7 != null && strArr7.length == 1 && strArr2 == null && ((str == null || str.equalsIgnoreCase("is_music=1") || str.equalsIgnoreCase("is_podcast=1")) && strArr7[0].equalsIgnoreCase("count(*)") && strArr8 != null)) {
                                    sQLiteQueryBuilder.setTables("audio_meta");
                                    if (!strGenerateStorageIdFilter.isEmpty()) {
                                        sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                                        if (!zIsPermitedAccessDrm) {
                                            sQLiteQueryBuilder.appendWhere(" AND (is_drm=0 OR is_drm IS NULL)");
                                        }
                                    } else if (!zIsPermitedAccessDrm) {
                                        sQLiteQueryBuilder.appendWhere("(is_drm=0 OR is_drm IS NULL)");
                                    }
                                } else {
                                    sQLiteQueryBuilder.setTables("audio");
                                    if (strGenerateStorageIdFilter.isEmpty()) {
                                        for (int i2 = 0; strArr8 != null && i2 < strArr8.length; i2++) {
                                            if (i2 > 0) {
                                                sQLiteQueryBuilder.appendWhere(" AND ");
                                            }
                                            sQLiteQueryBuilder.appendWhere("artist_key||album_key||title_key LIKE ? ESCAPE '\\'");
                                            arrayList.add("%" + strArr8[i2] + "%");
                                        }
                                        if (!zIsPermitedAccessDrm) {
                                            if (strArr8 == null || strArr8.length <= 0) {
                                                sQLiteQueryBuilder.appendWhere("(is_drm=0 OR is_drm IS NULL)");
                                            } else {
                                                sQLiteQueryBuilder.appendWhere(" AND (is_drm=0 OR is_drm IS NULL)");
                                            }
                                        }
                                    } else {
                                        sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                                        for (int i3 = 0; strArr8 != null && i3 < strArr8.length; i3++) {
                                            sQLiteQueryBuilder.appendWhere(" AND ");
                                            sQLiteQueryBuilder.appendWhere("artist_key||album_key||title_key LIKE ? ESCAPE '\\'");
                                            arrayList.add("%" + strArr8[i3] + "%");
                                        }
                                        if (!zIsPermitedAccessDrm) {
                                            sQLiteQueryBuilder.appendWhere(" AND (is_drm=0 OR is_drm IS NULL)");
                                        }
                                    }
                                }
                                break;
                            case 101:
                                str3 = queryParameter2;
                                sQLiteQueryBuilder.setTables("audio");
                                sQLiteQueryBuilder.appendWhere("_id=?");
                                arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                if (!strGenerateStorageIdFilter.isEmpty()) {
                                    sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                                }
                                break;
                            case 102:
                                str3 = queryParameter2;
                                sQLiteQueryBuilder.setTables("audio_genres");
                                if (strGenerateStorageIdFilter.isEmpty()) {
                                    sQLiteQueryBuilder.appendWhere("_id IN (SELECT genre_id FROM audio_genres_map WHERE audio_id=?)");
                                } else {
                                    sQLiteQueryBuilder.appendWhere("_id IN (SELECT genre_id FROM audio_genres_map WHERE audio_id=? AND audio_id IN (SELECT _id FROM audio WHERE " + strGenerateStorageIdFilter + "))");
                                }
                                arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                break;
                            case 103:
                                str3 = queryParameter2;
                                if (strArr7 == null) {
                                    strArr7 = new String[]{"audio_genres._id AS _id", "audio_genres.name AS name"};
                                } else {
                                    for (int i4 = 0; i4 < strArr7.length; i4++) {
                                        if (strArr7[i4].equalsIgnoreCase("_id")) {
                                            strArr7[i4] = "audio_genres._id AS _id";
                                        } else if (strArr7[i4].equalsIgnoreCase("name")) {
                                            strArr7[i4] = "audio_genres.name AS name";
                                        }
                                    }
                                }
                                sQLiteQueryBuilder.setTables("audio_genres CROSS JOIN audio_genres_map ON audio_genres._id=audio_genres_map.genre_id CROSS JOIN audio on audio_id=audio._id");
                                sQLiteQueryBuilder.appendWhere("audio_genres._id=?");
                                arrayList.add(uriSafeUncanonicalize.getPathSegments().get(5));
                                if (!strGenerateStorageIdFilter.isEmpty()) {
                                    sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                                }
                                str6 = "audio_genres._id";
                                strArr5 = strArr7;
                                str4 = str6;
                                String[] strArr9 = strArr5;
                                SQLiteDatabase sQLiteDatabase = readableDatabase;
                                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr5, str, combine(arrayList, strArr2), str4, null, str2, str3);
                                if (MediaUtils.LOG_QUERY) {
                                    String str8 = TAG;
                                    StringBuilder sb2 = new StringBuilder();
                                    sb2.append("query: uri = ");
                                    sb2.append(uriSafeUncanonicalize);
                                    sb2.append(", projection = ");
                                    strArr6 = strArr9;
                                    sb2.append(Arrays.toString(strArr6));
                                    sb2.append(", selection = ");
                                    sb2.append(str);
                                    sb2.append(", selectionArgs = ");
                                    sb2.append(Arrays.toString(strArr2));
                                    sb2.append(", sort = ");
                                    str5 = str2;
                                    sb2.append(str5);
                                    sb2.append(", caller pid = ");
                                    sb2.append(Binder.getCallingPid());
                                    sb2.append(", c.getCount() = ");
                                    sb2.append(cursorQuery == null ? "null" : Integer.valueOf(cursorQuery.getCount()));
                                    Log.d(str8, sb2.toString());
                                } else {
                                    str5 = str2;
                                    strArr6 = strArr9;
                                }
                                if (iMatch == 107) {
                                    Log.d(TAG, "Requery AUDIO_GENRES_ID again with default cmd");
                                    if (cursorQuery != null && cursorQuery.getCount() == 0) {
                                        for (int i5 = 0; i5 < strArr6.length; i5++) {
                                            if (strArr6[i5].equalsIgnoreCase("audio_genres._id AS _id")) {
                                                strArr6[i5] = "_id";
                                            } else if (strArr6[i5].equalsIgnoreCase("audio_genres.name AS name")) {
                                                strArr6[i5] = "name";
                                            }
                                        }
                                        cursorQuery.close();
                                        SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
                                        sQLiteQueryBuilder2.setTables("audio_genres");
                                        sQLiteQueryBuilder2.appendWhere("_id=?");
                                        Log.v(TAG, "query = " + sQLiteQueryBuilder2.buildQuery(strArr6, str, combine(arrayList, strArr2), null, null, str5, str3));
                                        cursorQuery = sQLiteQueryBuilder2.query(sQLiteDatabase, strArr6, str, combine(arrayList, strArr2), null, null, str2, str3);
                                    } else if (cursorQuery == null) {
                                        for (int i6 = 0; i6 < strArr6.length; i6++) {
                                            if (strArr6[i6].equalsIgnoreCase("audio_genres._id AS _id")) {
                                                strArr6[i6] = "_id";
                                            } else if (strArr6[i6].equalsIgnoreCase("audio_genres.name AS name")) {
                                                strArr6[i6] = "name";
                                            }
                                        }
                                        SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
                                        sQLiteQueryBuilder3.setTables("audio_genres");
                                        sQLiteQueryBuilder3.appendWhere("_id=?");
                                        Log.v(TAG, "query = " + sQLiteQueryBuilder3.buildQuery(strArr6, str, combine(arrayList, strArr2), null, null, str2, str3));
                                        cursorQuery = sQLiteQueryBuilder3.query(sQLiteDatabase, strArr6, str, combine(arrayList, strArr2), null, null, str2, str3);
                                    }
                                }
                                if (cursorQuery != null && ((queryParameter = uriSafeUncanonicalize.getQueryParameter("nonotify")) == null || !queryParameter.equals("1"))) {
                                    cursorQuery.setNotificationUri(getContext().getContentResolver(), uriSafeUncanonicalize);
                                }
                                return cursorQuery;
                            case 104:
                                str3 = queryParameter2;
                                sQLiteQueryBuilder.setTables("audio_playlists");
                                sQLiteQueryBuilder.appendWhere("_id IN (SELECT playlist_id FROM audio_playlists_map WHERE audio_id=?)");
                                arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                break;
                            case 105:
                                str3 = queryParameter2;
                                sQLiteQueryBuilder.setTables("audio_playlists");
                                sQLiteQueryBuilder.appendWhere("_id=?");
                                arrayList.add(uriSafeUncanonicalize.getPathSegments().get(5));
                                break;
                            case 106:
                                str3 = queryParameter2;
                                if (strArr7 == null) {
                                    strArr7 = new String[]{"audio_genres._id AS _id", "audio_genres.name AS name"};
                                } else {
                                    for (int i7 = 0; i7 < strArr7.length; i7++) {
                                        if (strArr7[i7].equalsIgnoreCase("_id")) {
                                            strArr7[i7] = "audio_genres._id AS _id";
                                        } else if (strArr7[i7].equalsIgnoreCase("name")) {
                                            strArr7[i7] = "audio_genres.name AS name";
                                        }
                                    }
                                }
                                sQLiteQueryBuilder.setTables("audio_genres CROSS JOIN audio_genres_map ON audio_genres._id=audio_genres_map.genre_id CROSS JOIN audio on audio_id=audio._id");
                                if (!strGenerateStorageIdFilter.isEmpty()) {
                                    sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                                }
                                str6 = "audio_genres._id";
                                strArr5 = strArr7;
                                str4 = str6;
                                String[] strArr92 = strArr5;
                                SQLiteDatabase sQLiteDatabase2 = readableDatabase;
                                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr5, str, combine(arrayList, strArr2), str4, null, str2, str3);
                                if (MediaUtils.LOG_QUERY) {
                                }
                                if (iMatch == 107) {
                                }
                                if (cursorQuery != null) {
                                    cursorQuery.setNotificationUri(getContext().getContentResolver(), uriSafeUncanonicalize);
                                }
                                return cursorQuery;
                            case 107:
                                str3 = queryParameter2;
                                Log.d(TAG, "query-AUDIO_GENRES_ID");
                                if (strArr7 == null) {
                                    strArr7 = new String[]{"audio_genres._id AS _id", "audio_genres.name AS name"};
                                } else {
                                    for (int i8 = 0; i8 < strArr7.length; i8++) {
                                        if (strArr7[i8].equalsIgnoreCase("_id")) {
                                            strArr7[i8] = "audio_genres._id AS _id";
                                        } else if (strArr7[i8].equalsIgnoreCase("name")) {
                                            strArr7[i8] = "audio_genres.name AS name";
                                        }
                                    }
                                }
                                sQLiteQueryBuilder.setTables("audio_genres CROSS JOIN audio_genres_map ON audio_genres._id=audio_genres_map.genre_id CROSS JOIN audio on audio_id=audio._id");
                                sQLiteQueryBuilder.appendWhere("audio_genres._id=?");
                                arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                if (!strGenerateStorageIdFilter.isEmpty()) {
                                    sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                                }
                                str6 = "audio_genres._id";
                                strArr5 = strArr7;
                                str4 = str6;
                                String[] strArr922 = strArr5;
                                SQLiteDatabase sQLiteDatabase22 = readableDatabase;
                                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr5, str, combine(arrayList, strArr2), str4, null, str2, str3);
                                if (MediaUtils.LOG_QUERY) {
                                }
                                if (iMatch == 107) {
                                }
                                if (cursorQuery != null) {
                                }
                                return cursorQuery;
                            case 108:
                            case 109:
                                str3 = queryParameter2;
                                String[] strArr10 = strArr4;
                                String strGenerateGenresQueryFilter = generateGenresQueryFilter(uriSafeUncanonicalize);
                                boolean z3 = strArr10 == null && strArr7 != null && (str == null || str.equalsIgnoreCase("genre_id=?"));
                                if (strArr7 != null) {
                                    for (String str9 : strArr7) {
                                        if (str9.equals("_id")) {
                                            z3 = false;
                                        }
                                        if (z3 && !str9.equals("audio_id") && !str9.equals("genre_id")) {
                                            z3 = false;
                                        }
                                    }
                                }
                                if (z3) {
                                    sQLiteQueryBuilder.setTables("audio_genres_map_noid");
                                    if (iMatch == 108) {
                                        sQLiteQueryBuilder.appendWhere("genre_id=?");
                                        arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                        if (!strGenerateGenresQueryFilter.isEmpty()) {
                                            sQLiteQueryBuilder.appendWhere(" AND " + strGenerateGenresQueryFilter);
                                        }
                                    } else if (!strGenerateGenresQueryFilter.isEmpty()) {
                                        sQLiteQueryBuilder.appendWhere(strGenerateGenresQueryFilter);
                                    }
                                } else {
                                    sQLiteQueryBuilder.setTables("audio_genres_map_noid, audio");
                                    sQLiteQueryBuilder.appendWhere("audio._id = audio_id");
                                    if (iMatch == 108) {
                                        sQLiteQueryBuilder.appendWhere(" AND genre_id=?");
                                        arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                    }
                                    for (int i9 = 0; strArr10 != null && i9 < strArr10.length; i9++) {
                                        sQLiteQueryBuilder.appendWhere(" AND ");
                                        sQLiteQueryBuilder.appendWhere("artist_key||album_key||title_key LIKE ? ESCAPE '\\'");
                                        arrayList.add("%" + strArr10[i9] + "%");
                                    }
                                    if (!strGenerateGenresQueryFilter.isEmpty()) {
                                        sQLiteQueryBuilder.appendWhere(" AND " + strGenerateGenresQueryFilter);
                                    }
                                }
                                break;
                            case 110:
                                str3 = queryParameter2;
                                sQLiteQueryBuilder.setTables("audio_playlists");
                                break;
                            case 111:
                                str3 = queryParameter2;
                                sQLiteQueryBuilder.setTables("audio_playlists");
                                sQLiteQueryBuilder.appendWhere("_id=?");
                                arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                break;
                            case 112:
                            case 113:
                                str3 = queryParameter2;
                                String[] strArr11 = strArr4;
                                boolean z4 = strArr11 == null && strArr7 != null && (str == null || str.equalsIgnoreCase("playlist_id=?"));
                                if (strArr7 != null) {
                                    for (int i10 = 0; i10 < strArr7.length; i10++) {
                                        String str10 = strArr7[i10];
                                        if (z4 && !str10.equals("audio_id") && !str10.equals("playlist_id") && !str10.equals("play_order")) {
                                            z4 = false;
                                        }
                                        if (str10.equals("_id")) {
                                            strArr7[i10] = "audio_playlists_map._id AS _id";
                                        }
                                    }
                                }
                                if (z4) {
                                    sQLiteQueryBuilder.setTables("audio_playlists_map");
                                    sQLiteQueryBuilder.appendWhere("playlist_id=?");
                                    arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                } else {
                                    sQLiteQueryBuilder.setTables("audio_playlists_map, audio");
                                    sQLiteQueryBuilder.appendWhere("audio._id = audio_id AND playlist_id=?");
                                    arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                    for (int i11 = 0; strArr11 != null && i11 < strArr11.length; i11++) {
                                        sQLiteQueryBuilder.appendWhere(" AND ");
                                        sQLiteQueryBuilder.appendWhere("artist_key||album_key||title_key LIKE ? ESCAPE '\\'");
                                        arrayList.add("%" + strArr11[i11] + "%");
                                    }
                                }
                                if (iMatch == 113) {
                                    sQLiteQueryBuilder.appendWhere(" AND audio_playlists_map._id=?");
                                    arrayList.add(uriSafeUncanonicalize.getPathSegments().get(5));
                                }
                                String strGeneratePlaylistQueryFilter = generatePlaylistQueryFilter(uriSafeUncanonicalize);
                                if (!strGeneratePlaylistQueryFilter.isEmpty()) {
                                    sQLiteQueryBuilder.appendWhere(" AND " + strGeneratePlaylistQueryFilter);
                                }
                                break;
                            case 114:
                                str3 = queryParameter2;
                                String[] strArr12 = strArr4;
                                if (strArr7 != null && strArr7.length == 1 && strArr2 == null && ((str == null || str.length() == 0) && strArr7[0].equalsIgnoreCase("count(*)") && strArr12 != null)) {
                                    sQLiteQueryBuilder.setTables("audio_meta");
                                    strArr7[0] = "count(distinct artist_id)";
                                    sQLiteQueryBuilder.appendWhere("is_music=1");
                                    if (!strGenerateStorageIdFilter.isEmpty()) {
                                        sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                                    }
                                } else {
                                    sQLiteQueryBuilder.setTables("artist_info");
                                    if (!strGenerateStorageIdFilter.isEmpty()) {
                                        sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                                        if (strArr7 == null) {
                                            strArr7 = new String[]{"_id", "artist", "artist_key", "SUM(number_of_albums) AS number_of_albums", "SUM(number_of_tracks) AS number_of_tracks"};
                                        } else {
                                            for (int i12 = 0; i12 < strArr7.length; i12++) {
                                                if (strArr7[i12].equalsIgnoreCase("number_of_albums") || strArr7[i12].equalsIgnoreCase("number_of_tracks")) {
                                                    strArr7[i12] = "SUM(" + strArr7[i12] + ") AS " + strArr7[i12];
                                                }
                                            }
                                        }
                                        for (int i13 = 0; strArr12 != null && i13 < strArr12.length; i13++) {
                                            sQLiteQueryBuilder.appendWhere(" AND ");
                                            sQLiteQueryBuilder.appendWhere("artist_key LIKE ? ESCAPE '\\'");
                                            arrayList.add("%" + strArr12[i13] + "%");
                                        }
                                        str6 = "_id";
                                        strArr5 = strArr7;
                                        str4 = str6;
                                        String[] strArr9222 = strArr5;
                                        SQLiteDatabase sQLiteDatabase222 = readableDatabase;
                                        cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr5, str, combine(arrayList, strArr2), str4, null, str2, str3);
                                        if (MediaUtils.LOG_QUERY) {
                                        }
                                        if (iMatch == 107) {
                                        }
                                        if (cursorQuery != null) {
                                        }
                                        return cursorQuery;
                                    }
                                    for (int i14 = 0; strArr12 != null && i14 < strArr12.length; i14++) {
                                        if (i14 > 0) {
                                            sQLiteQueryBuilder.appendWhere(" AND ");
                                        }
                                        sQLiteQueryBuilder.appendWhere("artist_key LIKE ? ESCAPE '\\'");
                                        arrayList.add("%" + strArr12[i14] + "%");
                                    }
                                }
                                break;
                            case 115:
                                str3 = queryParameter2;
                                sQLiteQueryBuilder.setTables("artist_info");
                                sQLiteQueryBuilder.appendWhere("_id=?");
                                arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                if (!strGenerateStorageIdFilter.isEmpty()) {
                                    sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                                }
                                break;
                            case 116:
                                str3 = queryParameter2;
                                String[] strArr13 = strArr4;
                                if (strArr7 != null && strArr7.length == 1 && strArr2 == null && ((str == null || str.length() == 0) && strArr7[0].equalsIgnoreCase("count(*)") && strArr13 != null)) {
                                    sQLiteQueryBuilder.setTables("audio_meta");
                                    strArr7[0] = "count(distinct album_id)";
                                    sQLiteQueryBuilder.appendWhere("is_music=1");
                                    if (!strGenerateStorageIdFilter.isEmpty()) {
                                        sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                                    }
                                } else {
                                    sQLiteQueryBuilder.setTables("album_info");
                                    if (strGenerateStorageIdFilter.isEmpty()) {
                                        for (int i15 = 0; strArr13 != null && i15 < strArr13.length; i15++) {
                                            if (i15 > 0) {
                                                sQLiteQueryBuilder.appendWhere(" AND ");
                                            }
                                            sQLiteQueryBuilder.appendWhere("artist_key||album_key LIKE ? ESCAPE '\\'");
                                            arrayList.add("%" + strArr13[i15] + "%");
                                        }
                                    } else {
                                        sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                                        for (int i16 = 0; strArr13 != null && i16 < strArr13.length; i16++) {
                                            sQLiteQueryBuilder.appendWhere(" AND ");
                                            sQLiteQueryBuilder.appendWhere("artist_key||album_key LIKE ? ESCAPE '\\'");
                                            arrayList.add("%" + strArr13[i16] + "%");
                                        }
                                    }
                                }
                                break;
                            case 117:
                                str3 = queryParameter2;
                                sQLiteQueryBuilder.setTables("album_info");
                                sQLiteQueryBuilder.appendWhere("_id=?");
                                arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                if (!strGenerateStorageIdFilter.isEmpty()) {
                                    sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                                }
                                break;
                            case 118:
                                str3 = queryParameter2;
                                String str11 = uriSafeUncanonicalize.getPathSegments().get(3);
                                sQLiteQueryBuilder.setTables("audio LEFT OUTER JOIN album_art ON audio.album_id=album_art.album_id");
                                sQLiteQueryBuilder.appendWhere("is_music=1 AND audio.album_id IN (SELECT album_id FROM artists_albums_map WHERE artist_id=?)");
                                arrayList.add(str11);
                                int i17 = 0;
                                while (strArr4 != null) {
                                    String[] strArr14 = strArr4;
                                    if (i17 >= strArr14.length) {
                                        strGenerateArtistsQueryFilter = generateArtistsQueryFilter(uriSafeUncanonicalize);
                                        if (!strGenerateArtistsQueryFilter.isEmpty()) {
                                            sQLiteQueryBuilder.appendWhere(" AND " + strGenerateArtistsQueryFilter);
                                        }
                                        sArtistAlbumsMap.put("numsongs_by_artist", "count(CASE WHEN artist_id==" + str11 + " THEN 'foo' ELSE NULL END) AS numsongs_by_artist");
                                        sQLiteQueryBuilder.setProjectionMap(sArtistAlbumsMap);
                                        strArr5 = strArr7;
                                        str4 = "audio.album_id";
                                        String[] strArr92222 = strArr5;
                                        SQLiteDatabase sQLiteDatabase2222 = readableDatabase;
                                        cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr5, str, combine(arrayList, strArr2), str4, null, str2, str3);
                                        if (MediaUtils.LOG_QUERY) {
                                        }
                                        if (iMatch == 107) {
                                        }
                                        if (cursorQuery != null) {
                                        }
                                        return cursorQuery;
                                    }
                                    sQLiteQueryBuilder.appendWhere(" AND ");
                                    sQLiteQueryBuilder.appendWhere("artist_key||album_key LIKE ? ESCAPE '\\'");
                                    arrayList.add("%" + strArr14[i17] + "%");
                                    i17++;
                                    strArr4 = strArr14;
                                }
                                strGenerateArtistsQueryFilter = generateArtistsQueryFilter(uriSafeUncanonicalize);
                                if (!strGenerateArtistsQueryFilter.isEmpty()) {
                                }
                                sArtistAlbumsMap.put("numsongs_by_artist", "count(CASE WHEN artist_id==" + str11 + " THEN 'foo' ELSE NULL END) AS numsongs_by_artist");
                                sQLiteQueryBuilder.setProjectionMap(sArtistAlbumsMap);
                                strArr5 = strArr7;
                                str4 = "audio.album_id";
                                String[] strArr922222 = strArr5;
                                SQLiteDatabase sQLiteDatabase22222 = readableDatabase;
                                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr5, str, combine(arrayList, strArr2), str4, null, str2, str3);
                                if (MediaUtils.LOG_QUERY) {
                                }
                                if (iMatch == 107) {
                                }
                                if (cursorQuery != null) {
                                }
                                return cursorQuery;
                            default:
                                switch (iMatch) {
                                    case 200:
                                        str3 = queryParameter2;
                                        sQLiteQueryBuilder.setTables("video");
                                        if (!strGenerateStorageIdFilter.isEmpty()) {
                                            sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                                            if (!zIsPermitedAccessDrm) {
                                                sQLiteQueryBuilder.appendWhere(" AND (is_drm=0 OR is_drm IS NULL)");
                                            }
                                        } else if (!zIsPermitedAccessDrm) {
                                            sQLiteQueryBuilder.appendWhere("(is_drm=0 OR is_drm IS NULL)");
                                        }
                                        break;
                                    case 201:
                                        str3 = queryParameter2;
                                        sQLiteQueryBuilder.setTables("video");
                                        sQLiteQueryBuilder.appendWhere("_id=?");
                                        arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                                        if (!strGenerateStorageIdFilter.isEmpty()) {
                                            sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                                        }
                                        break;
                                    case 202:
                                        str3 = queryParameter2;
                                        z2 = false;
                                        if (!queryThumbnail(sQLiteQueryBuilder, uriSafeUncanonicalize, "videothumbnails", "video_id", z2)) {
                                            return null;
                                        }
                                        break;
                                    case 203:
                                        str3 = queryParameter2;
                                        z2 = true;
                                        if (!queryThumbnail(sQLiteQueryBuilder, uriSafeUncanonicalize, "videothumbnails", "video_id", z2)) {
                                        }
                                        break;
                                    default:
                                        switch (iMatch) {
                                            case 400:
                                                Log.w(TAG, "Legacy media search Uri used. Please update your code.");
                                            case 401:
                                            case 402:
                                                if (!strGenerateStorageIdFilter.isEmpty()) {
                                                    sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                                                }
                                                Cursor cursorDoAudioSearch = doAudioSearch(readableDatabase, sQLiteQueryBuilder, uriSafeUncanonicalize, strArr7, str, combine(arrayList, strArr2), str2, iMatch, queryParameter2);
                                                if (cursorDoAudioSearch != null) {
                                                    cursorDoAudioSearch.setNotificationUri(getContext().getContentResolver(), uriSafeUncanonicalize);
                                                }
                                                return cursorDoAudioSearch;
                                            default:
                                                switch (iMatch) {
                                                    case 700:
                                                    case 702:
                                                        sQLiteQueryBuilder.setTables("files");
                                                        if (!strGenerateStorageIdFilter.isEmpty()) {
                                                            sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                                                        }
                                                        break;
                                                    case 701:
                                                    case 703:
                                                        sQLiteQueryBuilder.appendWhere("_id=?");
                                                        arrayList.add(uriSafeUncanonicalize.getPathSegments().get(2));
                                                        sQLiteQueryBuilder.setTables("files");
                                                        if (!strGenerateStorageIdFilter.isEmpty()) {
                                                            sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                                                        }
                                                        break;
                                                    case 704:
                                                        return getObjectReferences(databaseForUri, readableDatabase, Integer.parseInt(uriSafeUncanonicalize.getPathSegments().get(2)));
                                                    default:
                                                        switch (iMatch) {
                                                            case 1101:
                                                                sQLiteQueryBuilder.setTables("bookmark");
                                                                break;
                                                            case 1102:
                                                                sQLiteQueryBuilder.setTables("bookmark");
                                                                sQLiteQueryBuilder.appendWhere("_id = " + uriSafeUncanonicalize.getPathSegments().get(2));
                                                                break;
                                                            default:
                                                                switch (iMatch) {
                                                                    case 1300:
                                                                        if (!strGenerateStorageIdFilter.isEmpty()) {
                                                                            sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                                                                        }
                                                                        return FileSearchHelper.doFileSearch(readableDatabase, sQLiteQueryBuilder, uriSafeUncanonicalize, queryParameter2);
                                                                    case 1301:
                                                                        if (!strGenerateStorageIdFilter.isEmpty()) {
                                                                            sQLiteQueryBuilder.appendWhere(strGenerateStorageIdFilter);
                                                                        }
                                                                        return FileSearchHelper.doShortcutSearch(readableDatabase, sQLiteQueryBuilder, uriSafeUncanonicalize, queryParameter2);
                                                                    default:
                                                                        throw new IllegalStateException("Unknown URL: " + uriSafeUncanonicalize.toString());
                                                                }
                                                        }
                                                        break;
                                                }
                                                str3 = queryParameter2;
                                                break;
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
            } else {
                str3 = queryParameter2;
                sQLiteQueryBuilder.setTables("album_art");
                sQLiteQueryBuilder.appendWhere("album_id=?");
                arrayList.add(uriSafeUncanonicalize.getPathSegments().get(3));
                String strGenerateAlbumArtQueryFilter = generateAlbumArtQueryFilter(uriSafeUncanonicalize);
                if (!strGenerateAlbumArtQueryFilter.isEmpty()) {
                    sQLiteQueryBuilder.appendWhere(" AND " + strGenerateAlbumArtQueryFilter);
                }
            }
            strArr5 = strArr7;
            str4 = null;
            String[] strArr9222222 = strArr5;
            SQLiteDatabase sQLiteDatabase222222 = readableDatabase;
            cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr5, str, combine(arrayList, strArr2), str4, null, str2, str3);
            if (MediaUtils.LOG_QUERY) {
            }
            if (iMatch == 107) {
            }
            if (cursorQuery != null) {
            }
            return cursorQuery;
        } catch (SQLiteDiskIOException e) {
            MtkLog.e(TAG, "low memory error: msg=", e);
            return null;
        }
    }

    private String[] combine(List<String> list, String[] strArr) {
        int size = list.size();
        if (size == 0) {
            return strArr;
        }
        int length = strArr != null ? strArr.length : 0;
        String[] strArr2 = new String[size + length];
        for (int i = 0; i < size; i++) {
            strArr2[i] = list.get(i);
        }
        for (int i2 = 0; i2 < length; i2++) {
            strArr2[size + i2] = strArr[i2];
        }
        return strArr2;
    }

    private Cursor doAudioSearch(SQLiteDatabase sQLiteDatabase, SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String[] strArr, String str, String[] strArr2, String str2, int i, String str3) {
        String[] strArr3;
        String lowerCase = (uri.getPath().endsWith("/") ? "" : uri.getLastPathSegment()).replaceAll("  ", " ").trim().toLowerCase();
        String[] strArrSplit = lowerCase.length() > 0 ? lowerCase.split(" ") : new String[0];
        String[] strArr4 = new String[strArrSplit.length];
        int length = strArrSplit.length;
        for (int i2 = 0; i2 < length; i2++) {
            strArr4[i2] = (strArrSplit[i2].equals("a") || strArrSplit[i2].equals("an") || strArrSplit[i2].equals("the")) ? "%" : "%" + MediaStore.Audio.keyFor(strArrSplit[i2]).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        }
        String str4 = "";
        for (int i3 = 0; i3 < strArrSplit.length; i3++) {
            str4 = i3 == 0 ? "match LIKE ? ESCAPE '\\'" : str4 + " AND match LIKE ? ESCAPE '\\'";
        }
        sQLiteQueryBuilder.setTables("search");
        if (i == 402) {
            strArr3 = this.mSearchColsFancy;
        } else if (i == 401) {
            strArr3 = this.mSearchColsBasic;
        } else {
            strArr3 = this.mSearchColsLegacy;
        }
        String[] strArr5 = strArr3;
        if (MediaUtils.LOG_QUERY) {
            Log.d(TAG, "doAudioSearch: uri = " + uri + ", selection = " + str4 + ", selectionArgs = " + Arrays.toString(strArr4) + ", caller pid = " + Binder.getCallingPid());
        }
        return sQLiteQueryBuilder.query(sQLiteDatabase, strArr5, str4, strArr4, null, null, null, str3);
    }

    @Override
    public String getType(Uri uri) throws Throwable {
        Cursor cursorQuery;
        int iMatch = URI_MATCHER.match(uri);
        if (iMatch == 120) {
            return "image/jpeg";
        }
        if (iMatch != 701) {
            switch (iMatch) {
                case 1:
                case 3:
                    return "vnd.android.cursor.dir/image";
                case 2:
                    try {
                        cursorQuery = query(uri, MIME_TYPE_PROJECTION, null, null, null);
                        if (cursorQuery != null) {
                            try {
                                if (cursorQuery.getCount() == 1) {
                                    cursorQuery.moveToFirst();
                                    String string = cursorQuery.getString(1);
                                    cursorQuery.deactivate();
                                    IoUtils.closeQuietly(cursorQuery);
                                    return string;
                                }
                            } catch (Throwable th) {
                                th = th;
                                IoUtils.closeQuietly(cursorQuery);
                                throw th;
                            }
                        }
                        IoUtils.closeQuietly(cursorQuery);
                    } catch (Throwable th2) {
                        th = th2;
                        cursorQuery = null;
                    }
                    break;
                case 4:
                    return "image/jpeg";
                default:
                    switch (iMatch) {
                        case 100:
                        case 108:
                            return "vnd.android.cursor.dir/audio";
                        case 101:
                            break;
                        case 102:
                        case 106:
                            return "vnd.android.cursor.dir/genre";
                        case 103:
                        case 107:
                            return "vnd.android.cursor.item/genre";
                        case 104:
                            return "vnd.android.cursor.dir/playlist";
                        case 105:
                            return "vnd.android.cursor.item/playlist";
                        default:
                            switch (iMatch) {
                                case 110:
                                    return "vnd.android.cursor.dir/playlist";
                                case 111:
                                    return "vnd.android.cursor.item/playlist";
                                case 112:
                                    return "vnd.android.cursor.dir/audio";
                                default:
                                    switch (iMatch) {
                                        case 200:
                                            return "vnd.android.cursor.dir/video";
                                    }
                                case 113:
                                    break;
                            }
                            break;
                    }
                    break;
            }
        }
        throw new IllegalStateException("Unknown URL : " + uri);
    }

    private ContentValues ensureFile(boolean z, ContentValues contentValues, String str, String str2) {
        if (TextUtils.isEmpty(contentValues.getAsString("_data"))) {
            String strGenerateFileName = generateFileName(z, str, str2);
            ContentValues contentValues2 = new ContentValues(contentValues);
            contentValues2.put("_data", strGenerateFileName);
            return contentValues2;
        }
        return contentValues;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        int length;
        int iMatch = URI_MATCHER.match(uri);
        if (iMatch == 300) {
            return super.bulkInsert(uri, contentValuesArr);
        }
        DatabaseHelper databaseForUri = getDatabaseForUri(uri);
        if (databaseForUri == null) {
            throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        SQLiteDatabase writableDatabase = databaseForUri.getWritableDatabase();
        if (writableDatabase == null) {
            throw new IllegalStateException("Couldn't open database for " + uri);
        }
        if (iMatch == 111 || iMatch == 112) {
            return playlistBulkInsert(writableDatabase, uri, contentValuesArr);
        }
        if (iMatch == 704) {
            return setObjectReferences(databaseForUri, writableDatabase, Integer.parseInt(uri.getPathSegments().get(2)), contentValuesArr);
        }
        synchronized (this.mDirectoryCache) {
            writableDatabase.beginTransaction();
            boolean z = !databaseForUri.mInternal;
            ArrayList<Long> arrayList = new ArrayList<>();
            try {
                length = contentValuesArr.length;
                for (int i = 0; i < length; i++) {
                    if (contentValuesArr[i] != null) {
                        if (z && this.mDatabaseToBeClosed) {
                            throw new UnsupportedOperationException("Databae to be closed for URI: " + uri);
                        }
                        insertInternal(uri, iMatch, contentValuesArr[i], arrayList);
                    }
                }
                writableDatabase.setTransactionSuccessful();
                writableDatabase.endTransaction();
                ContentResolver contentResolver = getContext().getContentResolver();
                contentResolver.notifyChange(uri, null);
                if (iMatch == 100 && "external".equals(uri.getPathSegments().get(0))) {
                    contentResolver.notifyChange(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null);
                    contentResolver.notifyChange(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, null);
                }
            } catch (Throwable th) {
                writableDatabase.endTransaction();
                throw th;
            }
        }
        return length;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int i;
        int iMatch = URI_MATCHER.match(uri);
        Uri uriInsertInternal = insertInternal(uri, iMatch, contentValues, new ArrayList<>());
        if (uriInsertInternal != null && iMatch != 702) {
            ContentResolver contentResolver = getContext().getContentResolver();
            if (iMatch == 500) {
                i = 0;
            } else {
                i = 2;
            }
            contentResolver.notifyChange(uri, (ContentObserver) null, i);
            if (iMatch != 500) {
                getContext().getContentResolver().notifyChange(uriInsertInternal, (ContentObserver) null, 0);
            }
        }
        return uriInsertInternal;
    }

    private int playlistBulkInsert(SQLiteDatabase sQLiteDatabase, Uri uri, ContentValues[] contentValuesArr) {
        DatabaseUtils.InsertHelper insertHelper = new DatabaseUtils.InsertHelper(sQLiteDatabase, "audio_playlists_map");
        int columnIndex = insertHelper.getColumnIndex("audio_id");
        int columnIndex2 = insertHelper.getColumnIndex("playlist_id");
        int columnIndex3 = insertHelper.getColumnIndex("play_order");
        long j = Long.parseLong(uri.getPathSegments().get(3));
        sQLiteDatabase.beginTransaction();
        try {
            int length = contentValuesArr.length;
            for (int i = 0; i < length; i++) {
                insertHelper.prepareForInsert();
                insertHelper.bind(columnIndex, ((Number) contentValuesArr[i].get("audio_id")).longValue());
                insertHelper.bind(columnIndex2, j);
                insertHelper.bind(columnIndex3, ((Number) contentValuesArr[i].get("play_order")).intValue());
                insertHelper.execute();
            }
            sQLiteDatabase.setTransactionSuccessful();
            sQLiteDatabase.endTransaction();
            insertHelper.close();
            getContext().getContentResolver().notifyChange(uri, null);
            return length;
        } catch (Throwable th) {
            sQLiteDatabase.endTransaction();
            insertHelper.close();
            throw th;
        }
    }

    private long insertDirectory(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, String str) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("format", (Integer) 12289);
        contentValues.put("_data", str);
        contentValues.put("parent", Long.valueOf(getParent(databaseHelper, sQLiteDatabase, str)));
        File file = new File(str);
        if (file.exists()) {
            contentValues.put("date_modified", Long.valueOf(file.lastModified() / 1000));
        }
        FileSearchHelper.computeFileName(str, contentValues);
        databaseHelper.mNumInserts++;
        return sQLiteDatabase.insert("files", "date_modified", contentValues);
    }

    private long getParent(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, String str) {
        long jInsertDirectory;
        int iLastIndexOf = str.lastIndexOf(47);
        if (iLastIndexOf <= 0) {
            return 0L;
        }
        String strSubstring = str.substring(0, iLastIndexOf);
        for (int i = 0; i < this.mExternalStoragePaths.length; i++) {
            if (strSubstring.equals(this.mExternalStoragePaths[i])) {
                return 0L;
            }
        }
        synchronized (this.mDirectoryCache) {
            Long l = this.mDirectoryCache.get(strSubstring);
            if (l != null) {
                return l.longValue();
            }
            databaseHelper.mNumQueries++;
            Cursor cursorQuery = sQLiteDatabase.query("files", sIdOnlyColumn, "_data=?", new String[]{strSubstring}, null, null, null);
            if (cursorQuery == null) {
                jInsertDirectory = insertDirectory(databaseHelper, sQLiteDatabase, strSubstring);
                if (jInsertDirectory != -1) {
                }
                IoUtils.closeQuietly(cursorQuery);
                return jInsertDirectory;
            }
            try {
                if (cursorQuery.getCount() != 0) {
                    if (cursorQuery.getCount() > 1) {
                        Log.e(TAG, "more than one match for " + strSubstring);
                    }
                    cursorQuery.moveToFirst();
                    jInsertDirectory = cursorQuery.getLong(0);
                    if (jInsertDirectory != -1) {
                    }
                    IoUtils.closeQuietly(cursorQuery);
                    return jInsertDirectory;
                }
                jInsertDirectory = insertDirectory(databaseHelper, sQLiteDatabase, strSubstring);
                if (jInsertDirectory != -1) {
                    this.mDirectoryCache.put(strSubstring, Long.valueOf(jInsertDirectory));
                }
                IoUtils.closeQuietly(cursorQuery);
                return jInsertDirectory;
            } catch (Throwable th) {
                IoUtils.closeQuietly(cursorQuery);
                throw th;
            }
        }
    }

    private String getDefaultTitleFromCursor(Cursor cursor) {
        String defaultTitle;
        String string;
        int columnIndex = cursor.getColumnIndex("title_resource_uri");
        if (columnIndex > -1 && (string = cursor.getString(columnIndex)) != null) {
            try {
                defaultTitle = getDefaultTitle(string);
            } catch (Exception e) {
                defaultTitle = null;
            }
        } else {
            defaultTitle = null;
        }
        if (defaultTitle == null) {
            return cursor.getString(cursor.getColumnIndex("title"));
        }
        return defaultTitle;
    }

    private String getDefaultTitle(String str) throws Exception {
        try {
            return getTitleFromResourceUri(str, false);
        } catch (Exception e) {
            Log.e(TAG, "Error getting default title for " + str, e);
            throw e;
        }
    }

    private String getLocalizedTitle(String str) throws Exception {
        try {
            return getTitleFromResourceUri(str, true);
        } catch (Exception e) {
            Log.e(TAG, "Error getting localized title for " + str, e);
            throw e;
        }
    }

    private String getTitleFromResourceUri(String str, boolean z) throws Exception {
        Resources resources;
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        Uri uri = Uri.parse(str);
        if (!"android.resource".equals(uri.getScheme())) {
            return null;
        }
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 2) {
            Log.e(TAG, "Error getting localized title for " + str + ", must have 2 path segments");
            return null;
        }
        String str2 = pathSegments.get(0);
        if (!"string".equals(str2)) {
            Log.e(TAG, "Error getting localized title for " + str + ", first path segment must be \"string\"");
            return null;
        }
        String authority = uri.getAuthority();
        if (!z) {
            Context contextCreatePackageContext = getContext().createPackageContext(authority, 0);
            Configuration configuration = contextCreatePackageContext.getResources().getConfiguration();
            configuration.setLocale(Locale.US);
            resources = contextCreatePackageContext.createConfigurationContext(configuration).getResources();
        } else {
            resources = this.mPackageManager.getResourcesForApplication(authority);
        }
        return resources.getString(resources.getIdentifier(pathSegments.get(1), str2, authority));
    }

    private void localizeTitles() throws Exception {
        Iterator<DatabaseHelper> it = this.mDatabases.values().iterator();
        while (it.hasNext()) {
            SQLiteDatabase writableDatabase = it.next().getWritableDatabase();
            Cursor cursorQuery = writableDatabase.query("files", new String[]{"_id", "title_resource_uri"}, "title_resource_uri IS NOT NULL", null, null, null, null);
            Throwable th = null;
            while (cursorQuery.moveToNext()) {
                try {
                    try {
                        String string = cursorQuery.getString(0);
                        String string2 = cursorQuery.getString(1);
                        ContentValues contentValues = new ContentValues();
                        try {
                            String localizedTitle = getLocalizedTitle(string2);
                            contentValues.put("title_key", MediaStore.Audio.keyFor(localizedTitle));
                            contentValues.put("title", localizedTitle.trim());
                            writableDatabase.update("files", contentValues, "_id=?", new String[]{string});
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating localized title for " + string2 + ", keeping old localization");
                        }
                    } finally {
                    }
                } catch (Throwable th2) {
                    if (cursorQuery != null) {
                        $closeResource(th, cursorQuery);
                    }
                    throw th2;
                }
            }
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private long insertFile(DatabaseHelper databaseHelper, Uri uri, ContentValues contentValues, int i, boolean z, ArrayList<Long> arrayList) {
        int i2;
        SQLiteDatabase sQLiteDatabase;
        MediaProvider mediaProvider;
        DatabaseHelper databaseHelper2;
        ContentValues contentValues2;
        ContentValues contentValuesEnsureFile;
        String str;
        String str2;
        String str3;
        ContentValues contentValues3;
        int i3;
        long jLongValue;
        String str4;
        int iHashCode;
        Long l;
        ContentValues contentValues4;
        long j;
        long jLongValue2;
        String localizedTitle;
        long jInsert;
        SQLiteDatabase sQLiteDatabase2;
        int i4;
        SQLiteDatabase writableDatabase = databaseHelper.getWritableDatabase();
        switch (i) {
            case 1:
                i2 = 0;
                sQLiteDatabase = writableDatabase;
                mediaProvider = this;
                databaseHelper2 = databaseHelper;
                contentValues2 = contentValues;
                contentValuesEnsureFile = mediaProvider.ensureFile(databaseHelper2.mInternal, contentValues2, ".jpg", Environment.DIRECTORY_PICTURES);
                contentValuesEnsureFile.put("date_added", Long.valueOf(System.currentTimeMillis() / 1000));
                String asString = contentValuesEnsureFile.getAsString("_data");
                if (!contentValuesEnsureFile.containsKey("_display_name")) {
                    computeDisplayName(asString, contentValuesEnsureFile);
                }
                computeTakenTime(contentValuesEnsureFile);
                break;
            case 2:
                ContentValues contentValues5 = new ContentValues(contentValues);
                String asString2 = contentValues5.getAsString("album_artist");
                String asString3 = contentValues5.getAsString("compilation");
                contentValues5.remove("compilation");
                Object obj = contentValues5.get("artist");
                String string = obj == null ? "" : obj.toString();
                contentValues5.remove("artist");
                ConcurrentHashMap<String, Long> concurrentHashMap = databaseHelper.mArtistCache;
                String asString4 = contentValues5.getAsString("_data");
                Long l2 = concurrentHashMap.get(string);
                if (l2 == null) {
                    str = asString4;
                    str2 = string;
                    str3 = asString3;
                    contentValues3 = contentValues5;
                    i3 = 0;
                    sQLiteDatabase = writableDatabase;
                    jLongValue = getKeyIdForName(databaseHelper, writableDatabase, "artists", "artist_key", "artist", string, str2, str, 0, null, concurrentHashMap, uri);
                } else {
                    str = asString4;
                    str2 = string;
                    str3 = asString3;
                    contentValues3 = contentValues5;
                    i3 = 0;
                    sQLiteDatabase = writableDatabase;
                    jLongValue = l2.longValue();
                }
                long j2 = jLongValue;
                ContentValues contentValues6 = contentValues3;
                Object obj2 = contentValues6.get("album");
                String string2 = obj2 == null ? "" : obj2.toString();
                contentValues6.remove("album");
                ConcurrentHashMap<String, Long> concurrentHashMap2 = databaseHelper.mAlbumCache;
                if (asString2 != null) {
                    iHashCode = asString2.hashCode();
                } else {
                    String str5 = str3;
                    if (str5 == null || !str5.equals("1")) {
                        str4 = str;
                        iHashCode = str4.substring(i3, str4.lastIndexOf(47)).hashCode();
                        String str6 = string2 + iHashCode;
                        l = concurrentHashMap2.get(str6);
                        if (l != null) {
                            i2 = i3;
                            contentValues4 = contentValues6;
                            j = j2;
                            jLongValue2 = getKeyIdForName(databaseHelper, sQLiteDatabase, "albums", "album_key", "album", string2, str6, str4, iHashCode, str2, concurrentHashMap2, uri);
                        } else {
                            contentValues4 = contentValues6;
                            i2 = i3;
                            j = j2;
                            jLongValue2 = l.longValue();
                        }
                        ContentValues contentValues7 = contentValues4;
                        contentValues7.put("artist_id", Integer.toString((int) j));
                        contentValues7.put("album_id", Integer.toString((int) jLongValue2));
                        String asString5 = contentValues7.getAsString("title");
                        String string3 = asString5 != null ? "" : asString5.toString();
                        mediaProvider = this;
                        localizedTitle = mediaProvider.getLocalizedTitle(string3);
                        if (localizedTitle == null) {
                            contentValues7.put("title_resource_uri", string3);
                            string3 = localizedTitle;
                        } else {
                            contentValues7.putNull("title_resource_uri");
                        }
                        contentValues7.put("title_key", MediaStore.Audio.keyFor(string3));
                        contentValues7.put("title", string3.trim());
                        computeDisplayName(contentValues7.getAsString("_data"), contentValues7);
                        contentValuesEnsureFile = contentValues7;
                        databaseHelper2 = databaseHelper;
                        contentValues2 = contentValues;
                    } else {
                        iHashCode = i3;
                    }
                }
                str4 = str;
                String str62 = string2 + iHashCode;
                l = concurrentHashMap2.get(str62);
                if (l != null) {
                }
                ContentValues contentValues72 = contentValues4;
                contentValues72.put("artist_id", Integer.toString((int) j));
                contentValues72.put("album_id", Integer.toString((int) jLongValue2));
                String asString52 = contentValues72.getAsString("title");
                String string32 = asString52 != null ? "" : asString52.toString();
                mediaProvider = this;
                localizedTitle = mediaProvider.getLocalizedTitle(string32);
                if (localizedTitle == null) {
                }
                contentValues72.put("title_key", MediaStore.Audio.keyFor(string32));
                contentValues72.put("title", string32.trim());
                computeDisplayName(contentValues72.getAsString("_data"), contentValues72);
                contentValuesEnsureFile = contentValues72;
                databaseHelper2 = databaseHelper;
                contentValues2 = contentValues;
                break;
            case 3:
                contentValuesEnsureFile = ensureFile(databaseHelper.mInternal, contentValues, ".3gp", "video");
                computeDisplayName(contentValuesEnsureFile.getAsString("_data"), contentValuesEnsureFile);
                computeTakenTime(contentValuesEnsureFile);
                i2 = 0;
                sQLiteDatabase = writableDatabase;
                contentValues2 = contentValues;
                mediaProvider = this;
                databaseHelper2 = databaseHelper;
                break;
            default:
                i2 = 0;
                sQLiteDatabase = writableDatabase;
                contentValues2 = contentValues;
                mediaProvider = this;
                databaseHelper2 = databaseHelper;
                contentValuesEnsureFile = null;
                break;
        }
        if (contentValuesEnsureFile == null) {
            contentValuesEnsureFile = new ContentValues(contentValues2);
        }
        String asString6 = contentValuesEnsureFile.getAsString("_data");
        if (asString6 != null) {
            computeBucketValues(asString6, contentValuesEnsureFile);
            FileSearchHelper.computeFileName(asString6, contentValuesEnsureFile);
            FileSearchHelper.computeFileType(asString6, contentValuesEnsureFile);
        }
        contentValuesEnsureFile.put("date_added", Long.valueOf(System.currentTimeMillis() / 1000));
        Integer asInteger = contentValuesEnsureFile.getAsInteger("media_scanner_new_object_id");
        if (asInteger != null) {
            jInsert = asInteger.intValue();
            ContentValues contentValues8 = new ContentValues(contentValuesEnsureFile);
            contentValues8.remove("media_scanner_new_object_id");
            contentValuesEnsureFile = contentValues8;
        } else {
            jInsert = 0;
        }
        String asString7 = contentValuesEnsureFile.getAsString("title");
        if (asString7 == null && asString6 != null) {
            asString7 = MediaFile.getFileTitle(asString6);
        }
        contentValuesEnsureFile.put("title", asString7);
        String asString8 = contentValuesEnsureFile.getAsString("mime_type");
        Integer asInteger2 = contentValuesEnsureFile.getAsInteger("format");
        int iIntValue = asInteger2 == null ? i2 : asInteger2.intValue();
        if (iIntValue != 0) {
            sQLiteDatabase2 = sQLiteDatabase;
        } else if (!TextUtils.isEmpty(asString6)) {
            sQLiteDatabase2 = sQLiteDatabase;
            iIntValue = MediaFile.getFormatCode(asString6, asString8);
        } else if (i == 4) {
            contentValuesEnsureFile.put("format", (Integer) 47621);
            asString6 = mediaProvider.mExternalPath + "Playlists/" + contentValuesEnsureFile.getAsString("name");
            contentValuesEnsureFile.put("_data", asString6);
            sQLiteDatabase2 = sQLiteDatabase;
            contentValuesEnsureFile.put("parent", Long.valueOf(mediaProvider.getParent(databaseHelper2, sQLiteDatabase2, asString6)));
        } else {
            sQLiteDatabase2 = sQLiteDatabase;
            Log.e(TAG, "path is empty in insertFile()");
        }
        if (asString6 != null && asString6.endsWith("/")) {
            Log.e(TAG, "directory has trailing slash: " + asString6);
            return 0L;
        }
        if (iIntValue != 0) {
            contentValuesEnsureFile.put("format", Integer.valueOf(iIntValue));
            if (asString8 == null) {
                asString8 = MediaFile.getMimeTypeForFormatCode(iIntValue);
            }
        }
        if (asString8 == null && asString6 != null && iIntValue != 12289) {
            asString8 = MediaFile.getMimeTypeForFile(asString6);
        }
        if (asString8 != null) {
            contentValuesEnsureFile.put("mime_type", asString8);
            if (contentValuesEnsureFile.containsKey("media_type")) {
                i4 = i;
            } else {
                i4 = i;
                if (i4 == 0 && !MediaScanner.isNoMediaPath(asString6)) {
                    int fileTypeForMimeType = MediaFile.getFileTypeForMimeType(asString8);
                    if (MediaFile.isAudioFileType(fileTypeForMimeType)) {
                        i4 = 2;
                    } else if (MediaFile.isVideoFileType(fileTypeForMimeType)) {
                        i4 = 3;
                    } else if (MediaFile.isImageFileType(fileTypeForMimeType)) {
                        i4 = 1;
                    } else if (MediaFile.isPlayListFileType(fileTypeForMimeType)) {
                        i4 = 4;
                    }
                }
            }
        }
        contentValuesEnsureFile.put("media_type", Integer.valueOf(i4));
        if (jInsert == 0) {
            if (i4 == 4) {
                if (contentValuesEnsureFile.getAsString("name") == null && asString6 == null) {
                    throw new IllegalArgumentException("no name was provided when inserting abstract playlist");
                }
            } else if (asString6 == null) {
                throw new IllegalArgumentException("no path was provided when inserting new file");
            }
            if (asString6 != null) {
                File file = new File(asString6);
                if (file.exists()) {
                    contentValuesEnsureFile.put("date_modified", Long.valueOf(file.lastModified() / 1000));
                    if (!contentValuesEnsureFile.containsKey("_size")) {
                        contentValuesEnsureFile.put("_size", Long.valueOf(file.length()));
                    }
                    if (i4 == 1 || i4 == 3) {
                        computeTakenTime(contentValuesEnsureFile);
                    }
                }
            }
            if (contentValuesEnsureFile.getAsLong("parent") == null && asString6 != null) {
                contentValuesEnsureFile.put("parent", Long.valueOf(mediaProvider.getParent(databaseHelper2, sQLiteDatabase2, asString6)));
            }
            databaseHelper2.mNumInserts++;
            jInsert = sQLiteDatabase2.insert("files", "date_modified", contentValuesEnsureFile);
            if (MediaUtils.LOG_INSERT) {
                Log.v(TAG, "insert: uri=" + uri + ", mediaType=" + i4 + ", old values=" + contentValues2 + ", values=" + contentValuesEnsureFile + ", rowId=" + jInsert);
            }
            if (jInsert != -1 && z && !databaseHelper2.mInternal) {
                arrayList.add(Long.valueOf(jInsert));
            }
        } else {
            databaseHelper2.mNumUpdates++;
            String[] strArr = new String[1];
            strArr[i2] = Long.toString(jInsert);
            sQLiteDatabase2.update("files", contentValuesEnsureFile, "_id=?", strArr);
            if (MediaUtils.LOG_INSERT) {
                Log.v(TAG, "insert: row id not 0, need do update , old values=" + contentValues2 + ", values=" + contentValuesEnsureFile + ", rowId=" + jInsert);
            }
        }
        if (jInsert != -1 && iIntValue == 12289) {
            synchronized (mediaProvider.mDirectoryCache) {
                mediaProvider.mDirectoryCache.put(asString6, Long.valueOf(jInsert));
            }
        }
        return jInsert;
    }

    private Cursor getObjectReferences(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, int i) {
        databaseHelper.mNumQueries++;
        Cursor cursorQuery = sQLiteDatabase.query("files", sMediaTableColumns, "_id=?", new String[]{Integer.toString(i)}, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToNext()) {
                    long j = cursorQuery.getLong(0);
                    if (cursorQuery.getInt(1) != 4) {
                        return null;
                    }
                    databaseHelper.mNumQueries++;
                    return sQLiteDatabase.rawQuery("SELECT audio_id FROM audio_playlists_map WHERE playlist_id=? ORDER BY play_order", new String[]{Long.toString(j)});
                }
            } finally {
                IoUtils.closeQuietly(cursorQuery);
            }
        }
        return null;
    }

    private int setObjectReferences(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, int i, ContentValues[] contentValuesArr) {
        long j;
        long j2;
        int i2;
        long j3;
        int i3;
        databaseHelper.mNumQueries++;
        Cursor cursorQuery = sQLiteDatabase.query("files", sMediaTableColumns, "_id=?", new String[]{Integer.toString(i)}, null, null, null);
        if (cursorQuery != null) {
            try {
                if (!cursorQuery.moveToNext()) {
                    j = 0;
                } else {
                    if (cursorQuery.getInt(1) != 4) {
                        return 0;
                    }
                    j = cursorQuery.getLong(0);
                }
            } finally {
            }
        }
        IoUtils.closeQuietly(cursorQuery);
        if (j == 0) {
            return 0;
        }
        databaseHelper.mNumDeletes++;
        sQLiteDatabase.delete("audio_playlists_map", "playlist_id=?", new String[]{Long.toString(j)});
        int length = contentValuesArr.length;
        ContentValues[] contentValuesArr2 = new ContentValues[length];
        int i4 = 0;
        int i5 = 0;
        while (i5 < length) {
            long jLongValue = contentValuesArr[i5].getAsLong("_id").longValue();
            databaseHelper.mNumQueries++;
            int i6 = i4;
            int i7 = i5;
            ContentValues[] contentValuesArr3 = contentValuesArr2;
            long j4 = j;
            cursorQuery = sQLiteDatabase.query("files", sMediaTableColumns, "_id=?", new String[]{Long.toString(jLongValue)}, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (!cursorQuery.moveToNext()) {
                        j2 = 0;
                    } else if (cursorQuery.getInt(1) == 2) {
                        j2 = cursorQuery.getLong(0);
                    } else {
                        IoUtils.closeQuietly(cursorQuery);
                        i2 = i6;
                        j = j4;
                        j3 = 0;
                        i3 = i2;
                    }
                    IoUtils.closeQuietly(cursorQuery);
                    j3 = 0;
                    if (j2 == 0) {
                        i2 = i6;
                        j = j4;
                        i3 = i2;
                    } else {
                        ContentValues contentValues = new ContentValues();
                        j = j4;
                        contentValues.put("playlist_id", Long.valueOf(j));
                        contentValues.put("audio_id", Long.valueOf(j2));
                        contentValues.put("play_order", Integer.valueOf(i6));
                        i3 = i6 + 1;
                        contentValuesArr3[i6] = contentValues;
                    }
                } finally {
                }
            }
            contentValuesArr2 = contentValuesArr3;
            i5 = i7 + 1;
            i4 = i3;
        }
        int i8 = i4;
        ContentValues[] contentValuesArr4 = contentValuesArr2;
        if (i8 < length) {
            ContentValues[] contentValuesArr5 = new ContentValues[i8];
            System.arraycopy(contentValuesArr4, 0, contentValuesArr5, 0, i8);
            contentValuesArr4 = contentValuesArr5;
        }
        return playlistBulkInsert(sQLiteDatabase, MediaStore.Audio.Playlists.Members.getContentUri("external", j), contentValuesArr4);
    }

    private void updateGenre(long j, String str) throws Throwable {
        Cursor cursorQuery;
        Uri uriWithAppendedId;
        Uri contentUri = MediaStore.Audio.Genres.getContentUri("external");
        try {
            cursorQuery = query(contentUri, GENRE_LOOKUP_PROJECTION, "name=?", new String[]{str}, null);
            if (cursorQuery == null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("name", str);
                uriWithAppendedId = insert(contentUri, contentValues);
            } else {
                try {
                    if (cursorQuery.getCount() == 0) {
                        ContentValues contentValues2 = new ContentValues();
                        contentValues2.put("name", str);
                        uriWithAppendedId = insert(contentUri, contentValues2);
                    } else {
                        cursorQuery.moveToNext();
                        uriWithAppendedId = ContentUris.withAppendedId(contentUri, cursorQuery.getLong(0));
                    }
                } catch (Throwable th) {
                    th = th;
                    IoUtils.closeQuietly(cursorQuery);
                    throw th;
                }
            }
            if (uriWithAppendedId != null) {
                uriWithAppendedId = Uri.withAppendedPath(uriWithAppendedId, "members");
            }
            IoUtils.closeQuietly(cursorQuery);
            if (uriWithAppendedId != null) {
                ContentValues contentValues3 = new ContentValues();
                contentValues3.put("audio_id", Long.valueOf(j));
                insert(uriWithAppendedId, contentValues3);
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private Uri insertInternal(Uri uri, int i, ContentValues contentValues, ArrayList<Long> arrayList) throws Throwable {
        String str;
        String asString;
        SQLiteDatabase sQLiteDatabase;
        Uri uriWithAppendedId;
        ContentValues contentValuesEnsureFile;
        String volumeName = getVolumeName(uri);
        if (i == 500) {
            this.mMediaScannerVolume = contentValues.getAsString("volume");
            DatabaseHelper databaseForUri = getDatabaseForUri(Uri.parse("content://media/" + this.mMediaScannerVolume + "/audio"));
            if (databaseForUri == null) {
                Log.w(TAG, "insertInternal: no database for scanned volume " + this.mMediaScannerVolume);
            } else {
                databaseForUri.mScanStartTime = SystemClock.currentTimeMicro();
            }
            Log.v(TAG, "insertInternal: retrun MediaScannerUri" + MediaStore.getMediaScannerUri());
            return MediaStore.getMediaScannerUri();
        }
        if (i == 1201) {
            this.mMtpTransferFile = contentValues.getAsString("mtp_transfer_file_path");
            if (MediaUtils.LOG_INSERT) {
                Log.v(TAG, "insertInternal: retrun MtpTransferFileUri" + MtkMediaStore.getMtpTransferFileUri());
            }
            return MtkMediaStore.getMtpTransferFileUri();
        }
        if (contentValues == null) {
            str = null;
            asString = null;
        } else {
            String asString2 = contentValues.getAsString("genre");
            contentValues.remove("genre");
            str = asString2;
            asString = contentValues.getAsString("_data");
        }
        DatabaseHelper databaseForUri2 = getDatabaseForUri(uri);
        if (databaseForUri2 == null && i != 300 && i != 705) {
            throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        if (i == 300 || i == 705) {
            sQLiteDatabase = null;
        } else {
            SQLiteDatabase writableDatabase = databaseForUri2.getWritableDatabase();
            if (writableDatabase == null) {
                Log.e(TAG, "insertInternal: Null db!");
                return null;
            }
            sQLiteDatabase = writableDatabase;
        }
        long j = 0;
        switch (i) {
            case 1:
                long jInsertFile = insertFile(databaseForUri2, uri, contentValues, 1, true, arrayList);
                if (jInsertFile > 0) {
                    MediaDocumentsProvider.onMediaStoreInsert(getContext(), volumeName, 1, jInsertFile);
                    uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(volumeName), jInsertFile);
                } else {
                    uriWithAppendedId = null;
                }
                if (asString != null && asString.toLowerCase(Locale.US).endsWith("/.nomedia")) {
                    processNewNoMediaPath(databaseForUri2, sQLiteDatabase, asString);
                }
                return uriWithAppendedId;
            case 3:
                ContentValues contentValuesEnsureFile2 = ensureFile(databaseForUri2.mInternal, contentValues, ".jpg", "DCIM/.thumbnails");
                databaseForUri2.mNumInserts++;
                long jInsert = sQLiteDatabase.insert("thumbnails", "name", contentValuesEnsureFile2);
                if (jInsert > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Images.Thumbnails.getContentUri(volumeName), jInsert);
                } else {
                    uriWithAppendedId = null;
                }
                if (MediaUtils.LOG_INSERT) {
                    Log.v(TAG, "insert IMAGES_THUMBNAILS: insert uri=" + uri + ", match=" + i + ", initValues=" + contentValues + ", new uri=" + uriWithAppendedId);
                }
                if (asString != null) {
                    processNewNoMediaPath(databaseForUri2, sQLiteDatabase, asString);
                }
                return uriWithAppendedId;
            case 100:
                long jInsertFile2 = insertFile(databaseForUri2, uri, contentValues, 2, true, arrayList);
                if (jInsertFile2 > 0) {
                    MediaDocumentsProvider.onMediaStoreInsert(getContext(), volumeName, 2, jInsertFile2);
                    uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Audio.Media.getContentUri(volumeName), jInsertFile2);
                    if (str != null) {
                        updateGenre(jInsertFile2, str);
                    }
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 102:
                Long lValueOf = Long.valueOf(Long.parseLong(uri.getPathSegments().get(2)));
                ContentValues contentValues2 = new ContentValues(contentValues);
                contentValues2.put("audio_id", lValueOf);
                databaseForUri2.mNumInserts++;
                long jInsert2 = sQLiteDatabase.insert("audio_genres_map", "genre_id", contentValues2);
                if (jInsert2 > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert2);
                } else {
                    uriWithAppendedId = null;
                }
                if (MediaUtils.LOG_INSERT) {
                    Log.v(TAG, "insert AUDIO_MEDIA_ID_GENRES: insert uri=" + uri + ", match=" + i + ", initValues=" + contentValues + ", new uri=" + uriWithAppendedId);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 104:
                Long lValueOf2 = Long.valueOf(Long.parseLong(uri.getPathSegments().get(2)));
                ContentValues contentValues3 = new ContentValues(contentValues);
                contentValues3.put("audio_id", lValueOf2);
                databaseForUri2.mNumInserts++;
                long jInsert3 = sQLiteDatabase.insert("audio_playlists_map", "playlist_id", contentValues3);
                if (jInsert3 > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert3);
                } else {
                    uriWithAppendedId = null;
                }
                if (MediaUtils.LOG_INSERT) {
                    Log.v(TAG, "insert AUDIO_MEDIA_ID_PLAYLISTS: insert uri=" + uri + ", match=" + i + ", initValues=" + contentValues + ", new uri=" + uriWithAppendedId);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 106:
                databaseForUri2.mNumInserts++;
                long jInsert4 = sQLiteDatabase.insert("audio_genres", "audio_id", contentValues);
                if (jInsert4 > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Audio.Genres.getContentUri(volumeName), jInsert4);
                } else {
                    uriWithAppendedId = null;
                }
                if (MediaUtils.LOG_INSERT) {
                    Log.v(TAG, "insert AUDIO_GENRES: insert uri=" + uri + ", match=" + i + ", initValues=" + contentValues + ", new uri=" + uriWithAppendedId);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 108:
                Long lValueOf3 = Long.valueOf(Long.parseLong(uri.getPathSegments().get(3)));
                ContentValues contentValues4 = new ContentValues(contentValues);
                contentValues4.put("genre_id", lValueOf3);
                databaseForUri2.mNumInserts++;
                long jInsert5 = sQLiteDatabase.insert("audio_genres_map", "genre_id", contentValues4);
                if (jInsert5 > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert5);
                } else {
                    uriWithAppendedId = null;
                }
                if (MediaUtils.LOG_INSERT) {
                    Log.v(TAG, "insert AUDIO_GENRES_ID_MEMBERS: insert uri=" + uri + ", match=" + i + ", initValues=" + contentValues + ", new uri=" + uriWithAppendedId);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 110:
                ContentValues contentValues5 = new ContentValues(contentValues);
                contentValues5.put("date_added", Long.valueOf(System.currentTimeMillis() / 1000));
                long jInsertFile3 = insertFile(databaseForUri2, uri, contentValues5, 4, true, arrayList);
                if (jInsertFile3 > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Audio.Playlists.getContentUri(volumeName), jInsertFile3);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 111:
            case 112:
                Long lValueOf4 = Long.valueOf(Long.parseLong(uri.getPathSegments().get(3)));
                ContentValues contentValues6 = new ContentValues(contentValues);
                contentValues6.put("playlist_id", lValueOf4);
                databaseForUri2.mNumInserts++;
                long jInsert6 = sQLiteDatabase.insert("audio_playlists_map", "playlist_id", contentValues6);
                if (jInsert6 > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert6);
                } else {
                    uriWithAppendedId = null;
                }
                if (MediaUtils.LOG_INSERT) {
                    Log.v(TAG, "insert AUDIO_PLAYLISTS: insert uri=" + uri + ", match=" + i + ", initValues=" + contentValues + ", new uri=" + uriWithAppendedId);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 119:
                if (databaseForUri2.mInternal) {
                    throw new UnsupportedOperationException("no internal album art allowed");
                }
                try {
                    contentValuesEnsureFile = ensureFile(false, contentValues, "", "Android/data/com.android.providers.media/albumthumbs");
                    break;
                } catch (IllegalStateException e) {
                    contentValuesEnsureFile = contentValues;
                }
                databaseForUri2.mNumInserts++;
                long jInsert7 = sQLiteDatabase.insert("album_art", "_data", contentValuesEnsureFile);
                if (jInsert7 > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert7);
                } else {
                    uriWithAppendedId = null;
                }
                if (MediaUtils.LOG_INSERT) {
                    Log.v(TAG, "insert AUDIO_ALBUMART: insert uri=" + uri + ", match=" + i + ", initValues=" + contentValues + ", new uri=" + uriWithAppendedId);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 200:
                long jInsertFile4 = insertFile(databaseForUri2, uri, contentValues, 3, true, arrayList);
                if (jInsertFile4 > 0) {
                    MediaDocumentsProvider.onMediaStoreInsert(getContext(), volumeName, 3, jInsertFile4);
                    uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri(volumeName), jInsertFile4);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 202:
                ContentValues contentValuesEnsureFile3 = ensureFile(databaseForUri2.mInternal, contentValues, ".jpg", "DCIM/.thumbnails");
                databaseForUri2.mNumInserts++;
                long jInsert8 = sQLiteDatabase.insert("videothumbnails", "name", contentValuesEnsureFile3);
                if (jInsert8 > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Video.Thumbnails.getContentUri(volumeName), jInsert8);
                } else {
                    uriWithAppendedId = null;
                }
                if (MediaUtils.LOG_INSERT) {
                    Log.v(TAG, "insert VIDEO_THUMBNAILS: insert uri=" + uri + ", match=" + i + ", initValues=" + contentValues + ", new uri=" + uriWithAppendedId);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 300:
                String asString3 = contentValues.getAsString("name");
                Uri uriAttachVolume = Uri.parse("content://media/" + asString3);
                long jCurrentTimeMillis = System.currentTimeMillis();
                while (this.mNeedWaitStorageStateChange && j < 5000) {
                    try {
                        Thread.sleep(1000L);
                        long jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
                        try {
                            Log.v(TAG, "insert to attchvolume, sleep " + jCurrentTimeMillis2 + "ms to wait storage state change");
                            j = jCurrentTimeMillis2;
                        } catch (InterruptedException e2) {
                            e = e2;
                            j = jCurrentTimeMillis2;
                            Log.w(TAG, "insert to attchvolume with InterruptedException" + e);
                        }
                    } catch (InterruptedException e3) {
                        e = e3;
                    }
                }
                String externalStorageState = Environment.getExternalStorageState();
                if ("mounted".equals(externalStorageState) || "mounted_ro".equals(externalStorageState)) {
                    this.mNeedWaitStorageStateChange = false;
                    uriAttachVolume = attachVolume(asString3);
                }
                if (this.mMediaScannerVolume != null && this.mMediaScannerVolume.equals(asString3)) {
                    DatabaseHelper databaseForUri3 = getDatabaseForUri(uriAttachVolume);
                    if (databaseForUri3 == null) {
                        Log.e(TAG, "no database for attached volume " + uriAttachVolume);
                    } else {
                        databaseForUri3.mScanStartTime = SystemClock.currentTimeMicro();
                    }
                }
                if (MediaUtils.LOG_INSERT) {
                    Log.v(TAG, "insert VOLUMES: insert uri=" + uri + ", match=" + i + ", initValues=" + contentValues);
                }
                return uriAttachVolume;
            case 700:
                long jInsertFile5 = insertFile(databaseForUri2, uri, contentValues, 0, true, arrayList);
                if (jInsertFile5 > 0) {
                    uriWithAppendedId = MediaStore.Files.getContentUri(volumeName, jInsertFile5);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 702:
                long jInsertFile6 = insertFile(databaseForUri2, uri, contentValues, 0, false, arrayList);
                if (jInsertFile6 > 0) {
                    uriWithAppendedId = MediaStore.Files.getMtpObjectsUri(volumeName, jInsertFile6);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 705:
                Log.v(TAG, "insert: match = " + i + ", MTP_CONNECTED= 705");
                synchronized (this.mMtpServiceConnection) {
                    if (this.mMtpService == null) {
                        Log.v(TAG, "MtpService is null, new the service and bind the connection");
                        Context context = getContext();
                        context.bindService(new Intent(context, (Class<?>) MtpService.class), this.mMtpServiceConnection, 1);
                    } else {
                        Log.v(TAG, "MtpService is not null!!");
                    }
                    break;
                }
                fixParentIdIfNeeded();
                uriWithAppendedId = null;
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 706:
                long jInsertDirectory = insertDirectory(databaseForUri2, databaseForUri2.getWritableDatabase(), contentValues.getAsString("_data"));
                if (jInsertDirectory > 0) {
                    uriWithAppendedId = MediaStore.Files.getContentUri(volumeName, jInsertDirectory);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            case 1101:
                long jInsert9 = sQLiteDatabase.insert("bookmark", "mime_type", contentValues);
                if (jInsert9 > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert9);
                } else {
                    uriWithAppendedId = null;
                }
                if (MediaUtils.LOG_INSERT) {
                    Log.v(TAG, "insert MEDIA_BOOKMARK: insert uri=" + uri + ", match=" + i + ", initValues=" + contentValues + ", new uri=" + uriWithAppendedId);
                }
                if (asString != null) {
                }
                return uriWithAppendedId;
            default:
                throw new UnsupportedOperationException("Invalid URI " + uri);
        }
    }

    private void fixParentIdIfNeeded() {
        String str;
        int i;
        int i2;
        int i3;
        DatabaseHelper databaseForUri = getDatabaseForUri(Uri.parse("content://media/external/file"));
        if (databaseForUri == null) {
            return;
        }
        SQLiteDatabase writableDatabase = databaseForUri.getWritableDatabase();
        long j = -1;
        int i4 = 0;
        int count = 0;
        boolean z = true;
        while (true) {
            String[] strArr = sDataId;
            StringBuilder sb = new StringBuilder();
            sb.append("parent != 0 AND parent NOT IN (SELECT _id FROM files)");
            if (j < 0) {
                str = "";
            } else {
                str = " AND _id > " + j;
            }
            sb.append(str);
            i = i4;
            i2 = count;
            Cursor cursorQuery = writableDatabase.query("files", strArr, sb.toString(), null, null, null, "_id ASC", "500");
            if (cursorQuery == null) {
                break;
            }
            try {
                if (cursorQuery.getCount() == 0) {
                    break;
                }
                count = i2 + cursorQuery.getCount();
                if (z) {
                    synchronized (this.mDirectoryCache) {
                        this.mDirectoryCache.clear();
                    }
                    z = false;
                }
                i4 = i;
                while (cursorQuery.moveToNext()) {
                    String string = cursorQuery.getString(0);
                    j = cursorQuery.getLong(1);
                    if (!new File(string).exists()) {
                        writableDatabase.delete("files", "_id=" + j, null);
                    } else {
                        long parent = getParent(databaseForUri, writableDatabase, string);
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("parent", Long.valueOf(parent));
                        int iUpdate = writableDatabase.update("files", contentValues, "_id=" + j, null);
                        databaseForUri.mNumUpdates = databaseForUri.mNumUpdates + iUpdate;
                        i4 += iUpdate;
                    }
                }
            } finally {
                IoUtils.closeQuietly(cursorQuery);
            }
        }
        if (i2 > 0) {
            String str2 = TAG;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("fixParentIdIfNeeded: found: ");
            sb2.append(i2);
            sb2.append(", fixed: ");
            i3 = i;
            sb2.append(i3);
            Log.d(str2, sb2.toString());
        } else {
            i3 = i;
        }
        if (i3 > 0) {
            getContext().getContentResolver().notifyChange(Uri.parse("content://media/"), null);
        }
    }

    private void processNewNoMediaPath(final DatabaseHelper databaseHelper, final SQLiteDatabase sQLiteDatabase, final String str) {
        final File file = new File(str);
        if (file.exists()) {
            hidePath(databaseHelper, sQLiteDatabase, str);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SystemClock.sleep(2000L);
                    if (file.exists()) {
                        MediaProvider.this.hidePath(databaseHelper, sQLiteDatabase, str);
                        return;
                    }
                    Log.w(MediaProvider.TAG, "does not exist: " + str, new Exception());
                }
            }).start();
        }
    }

    private void hidePath(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, String str) {
        String parent;
        MediaScanner.clearMediaPathCache(true, false);
        File file = new File(str);
        if (!file.isDirectory()) {
            parent = file.getParent();
        } else {
            parent = str;
        }
        Cursor cursorQuery = sQLiteDatabase.query("files", new String[]{"_id", "media_type"}, "_data >= ? AND _data < ? AND (media_type=1 OR media_type=3) AND mini_thumb_magic IS NOT NULL", new String[]{parent + "/", parent + "0"}, null, null, null);
        if (cursorQuery != null) {
            if (cursorQuery.getCount() != 0) {
                Uri uri = Uri.parse("content://media/external/images/media");
                Uri uri2 = Uri.parse("content://media/external/videos/media");
                while (cursorQuery.moveToNext()) {
                    long j = cursorQuery.getLong(0);
                    long j2 = cursorQuery.getLong(1);
                    Log.i(TAG, "hiding image " + j + ", removing thumbnail");
                    removeThumbnailFor(j2 == 1 ? uri : uri2, sQLiteDatabase, j);
                }
            }
            IoUtils.closeQuietly(cursorQuery);
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("media_type", (Integer) 0);
        databaseHelper.mNumUpdates += sQLiteDatabase.update("files", contentValues, "_data >= ? AND _data < ?", new String[]{parent + "/", parent + "0"});
        getContext().getContentResolver().notifyChange(Uri.parse("content://media/"), null);
    }

    private void processRemovedNoMediaPath(String str) {
        MediaScanner.clearMediaPathCache(false, true);
        String[] strArr = {Environment.getRootDirectory() + "/media", Environment.getOemDirectory() + "/media"};
        Log.v(TAG, " path=" + str + ", mExternalStoragePaths[0]=" + this.mExternalStoragePaths[0] + ", internalPaths[0] = " + strArr[0] + ", internalPaths[1] = " + strArr[1]);
        if (isSecondaryExternalPath(str) || str.startsWith(this.mExternalStoragePaths[0])) {
            new ScannerClient(getContext(), getDatabaseForUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).getWritableDatabase(), str);
        } else if (str.startsWith(strArr[0]) || str.startsWith(strArr[1])) {
            new ScannerClient(getContext(), getDatabaseForUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI).getWritableDatabase(), str);
        }
    }

    private static final class ScannerClient implements MediaScannerConnection.MediaScannerConnectionClient {
        SQLiteDatabase mDb;
        String mPath;
        MediaScannerConnection mScannerConnection;

        public ScannerClient(Context context, SQLiteDatabase sQLiteDatabase, String str) {
            this.mPath = null;
            this.mDb = sQLiteDatabase;
            this.mPath = str;
            this.mScannerConnection = new MediaScannerConnection(context, this);
            this.mScannerConnection.connect();
        }

        @Override
        public void onMediaScannerConnected() throws Throwable {
            ?? r0 = 0;
            Cursor cursor = null;
            try {
                try {
                    Cursor cursorQuery = this.mDb.query("files", MediaProvider.openFileColumns, "_data >= ? AND _data < ?", new String[]{this.mPath + "/", this.mPath + "0"}, null, null, null);
                    while (cursorQuery != null) {
                        try {
                            if (!cursorQuery.moveToNext()) {
                                break;
                            }
                            String string = cursorQuery.getString(0);
                            if (new File(string).isFile()) {
                                this.mScannerConnection.scanFile(string, null);
                            }
                        } catch (IllegalStateException e) {
                            e = e;
                            cursor = cursorQuery;
                            Log.e(MediaProvider.TAG, "IllegalStateException in onMediaScannerConnected", e);
                            this.mScannerConnection.disconnect();
                            IoUtils.closeQuietly(cursor);
                            r0 = cursor;
                        } catch (Throwable th) {
                            th = th;
                            r0 = cursorQuery;
                            this.mScannerConnection.disconnect();
                            IoUtils.closeQuietly((AutoCloseable) r0);
                            throw th;
                        }
                    }
                    MediaScannerConnection mediaScannerConnection = this.mScannerConnection;
                    mediaScannerConnection.disconnect();
                    IoUtils.closeQuietly(cursorQuery);
                    r0 = mediaScannerConnection;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IllegalStateException e2) {
                e = e2;
            }
        }

        @Override
        public void onScanCompleted(String str, Uri uri) {
            Log.v(MediaProvider.TAG, "onScanCompleted: path=" + str + ", uri=" + uri);
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws OperationApplicationException {
        SQLiteDatabase writableDatabase;
        ContentProviderResult[] contentProviderResultArrApplyBatch;
        synchronized (this.mDirectoryCache) {
            DatabaseHelper databaseForUri = getDatabaseForUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
            DatabaseHelper databaseForUri2 = getDatabaseForUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            SQLiteDatabase writableDatabase2 = databaseForUri.getWritableDatabase();
            writableDatabase2.beginTransaction();
            if (databaseForUri2 != null) {
                try {
                    writableDatabase = databaseForUri2.getWritableDatabase();
                } catch (Throwable th) {
                    th = th;
                    writableDatabase = null;
                    try {
                        writableDatabase2.endTransaction();
                        if (writableDatabase != null) {
                        }
                    } catch (Throwable th2) {
                        if (writableDatabase == null) {
                            throw th2;
                        }
                        writableDatabase.endTransaction();
                        throw th2;
                    }
                }
            } else {
                writableDatabase = null;
            }
            if (writableDatabase != null) {
                try {
                    writableDatabase.beginTransaction();
                } catch (Throwable th3) {
                    th = th3;
                    writableDatabase2.endTransaction();
                    if (writableDatabase != null) {
                        throw th;
                    }
                    writableDatabase.endTransaction();
                    throw th;
                }
            }
            contentProviderResultArrApplyBatch = super.applyBatch(arrayList);
            writableDatabase2.setTransactionSuccessful();
            if (writableDatabase != null) {
                writableDatabase.setTransactionSuccessful();
            }
            getContext().getContentResolver().notifyChange(Uri.parse("content://media/"), null);
            try {
                writableDatabase2.endTransaction();
                if (writableDatabase != null) {
                    writableDatabase.endTransaction();
                }
            } catch (Throwable th4) {
                if (writableDatabase == null) {
                    throw th4;
                }
                writableDatabase.endTransaction();
                throw th4;
            }
        }
        return contentProviderResultArrApplyBatch;
    }

    private MediaThumbRequest requestMediaThumbnail(String str, Uri uri, int i, long j) {
        MediaThumbRequest mediaThumbRequest;
        synchronized (this.mMediaThumbQueue) {
            try {
                mediaThumbRequest = new MediaThumbRequest(getContext().getContentResolver(), str, uri, i, j);
            } catch (Throwable th) {
                th = th;
                mediaThumbRequest = null;
            }
            try {
                this.mMediaThumbQueue.add(mediaThumbRequest);
                this.mThumbHandler.obtainMessage(2).sendToTarget();
            } catch (Throwable th2) {
                th = th2;
                Log.w(TAG, th);
            }
        }
        return mediaThumbRequest;
    }

    private String generateFileName(boolean z, String str, String str2) {
        String strValueOf = String.valueOf(System.currentTimeMillis());
        if (z) {
            throw new UnsupportedOperationException("Writing to internal storage is not supported.");
        }
        String str3 = this.mExternalStoragePaths[0] + "/" + str2;
        new File(str3).mkdirs();
        return str3 + "/" + strValueOf + str;
    }

    private boolean ensureFileExists(Uri uri, String str) {
        File file = new File(str);
        if (file.exists()) {
            return true;
        }
        try {
            checkAccess(uri, file, 939524096);
            int iIndexOf = str.indexOf(47, 1);
            if (iIndexOf < 1 || !new File(str.substring(0, iIndexOf)).exists()) {
                return false;
            }
            file.getParentFile().mkdirs();
            try {
                return file.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "File creation failed", e);
                return false;
            }
        } catch (FileNotFoundException e2) {
            return false;
        }
    }

    private static final class TableAndWhere {
        public String table;
        public String where;

        private TableAndWhere() {
        }
    }

    private TableAndWhere getTableAndWhere(Uri uri, int i, String str) {
        AnonymousClass1 anonymousClass1 = 0;
        String str2 = null;
        anonymousClass1 = 0;
        String str3 = null;
        String str4 = 0;
        String str5 = null;
        TableAndWhere tableAndWhere = new TableAndWhere();
        if (i == 120) {
            tableAndWhere.table = "album_art";
            str4 = "album_id=" + uri.getPathSegments().get(3);
        } else if (i != 706) {
            switch (i) {
                case 1:
                    tableAndWhere.table = "files";
                    str4 = "media_type=1";
                    break;
                case 2:
                    tableAndWhere.table = "files";
                    str4 = "_id = " + uri.getPathSegments().get(3);
                    break;
                case 4:
                    str5 = "_id=" + uri.getPathSegments().get(3);
                case 3:
                    tableAndWhere.table = "thumbnails";
                    str4 = str5;
                    break;
                default:
                    switch (i) {
                        case 100:
                            tableAndWhere.table = "files";
                            str4 = "media_type=2";
                            break;
                        case 101:
                            tableAndWhere.table = "files";
                            str4 = "_id=" + uri.getPathSegments().get(3);
                            break;
                        case 102:
                            tableAndWhere.table = "audio_genres";
                            str4 = "audio_id=" + uri.getPathSegments().get(3);
                            break;
                        case 103:
                            tableAndWhere.table = "audio_genres";
                            str4 = "audio_id=" + uri.getPathSegments().get(3) + " AND genre_id=" + uri.getPathSegments().get(5);
                            break;
                        case 104:
                            tableAndWhere.table = "audio_playlists";
                            str4 = "audio_id=" + uri.getPathSegments().get(3);
                            break;
                        case 105:
                            tableAndWhere.table = "audio_playlists";
                            str4 = "audio_id=" + uri.getPathSegments().get(3) + " AND playlists_id=" + uri.getPathSegments().get(5);
                            break;
                        case 106:
                            tableAndWhere.table = "audio_genres";
                            break;
                        case 107:
                            tableAndWhere.table = "audio_genres";
                            str4 = "_id=" + uri.getPathSegments().get(3);
                            break;
                        case 108:
                            tableAndWhere.table = "audio_genres";
                            str4 = "genre_id=" + uri.getPathSegments().get(3);
                            break;
                        default:
                            switch (i) {
                                case 110:
                                    tableAndWhere.table = "files";
                                    str4 = "media_type=4";
                                    break;
                                case 111:
                                    tableAndWhere.table = "files";
                                    str4 = "_id=" + uri.getPathSegments().get(3);
                                    break;
                                case 112:
                                    tableAndWhere.table = "audio_playlists_map";
                                    str4 = "playlist_id=" + uri.getPathSegments().get(3);
                                    break;
                                case 113:
                                    tableAndWhere.table = "audio_playlists_map";
                                    str4 = "playlist_id=" + uri.getPathSegments().get(3) + " AND _id=" + uri.getPathSegments().get(5);
                                    break;
                                default:
                                    switch (i) {
                                        case 200:
                                            tableAndWhere.table = "files";
                                            str4 = "media_type=3";
                                            break;
                                        case 201:
                                            tableAndWhere.table = "files";
                                            str4 = "_id=" + uri.getPathSegments().get(3);
                                            break;
                                        case 203:
                                            str3 = "_id=" + uri.getPathSegments().get(3);
                                        case 202:
                                            tableAndWhere.table = "videothumbnails";
                                            str4 = str3;
                                            break;
                                        default:
                                            switch (i) {
                                                case 701:
                                                case 703:
                                                    anonymousClass1 = "_id=" + uri.getPathSegments().get(2);
                                                case 700:
                                                case 702:
                                                    tableAndWhere.table = "files";
                                                    str4 = anonymousClass1;
                                                    break;
                                                default:
                                                    switch (i) {
                                                        case 1102:
                                                            str2 = "_id=" + uri.getPathSegments().get(2);
                                                        case 1101:
                                                            tableAndWhere.table = "bookmark";
                                                            str4 = str2;
                                                            break;
                                                        default:
                                                            throw new UnsupportedOperationException("Unknown or unsupported URL: " + uri.toString());
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
        }
        if (!TextUtils.isEmpty(str)) {
            if (!TextUtils.isEmpty(str4)) {
                tableAndWhere.where = str4 + " AND (" + str + ")";
            } else {
                tableAndWhere.where = str;
            }
        } else {
            tableAndWhere.where = str4;
        }
        return tableAndWhere;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) throws Throwable {
        TableAndWhere tableAndWhere;
        SQLiteDatabase sQLiteDatabase;
        String str2;
        int i;
        DatabaseHelper databaseHelper;
        Cursor cursorQuery;
        int iDelete;
        TableAndWhere tableAndWhere2;
        DatabaseHelper databaseHelper2;
        String str3;
        String str4;
        Uri uriSafeUncanonicalize = safeUncanonicalize(uri);
        int iMatch = URI_MATCHER.match(uriSafeUncanonicalize);
        int i2 = 0;
        int i3 = 1;
        if (iMatch == 500) {
            if (this.mMediaScannerVolume == null) {
                return 0;
            }
            DatabaseHelper databaseForUri = getDatabaseForUri(Uri.parse("content://media/" + this.mMediaScannerVolume + "/audio"));
            if (databaseForUri == null) {
                Log.w(TAG, "no database for scanned volume " + this.mMediaScannerVolume);
            } else {
                databaseForUri.mScanStopTime = SystemClock.currentTimeMicro();
                logToDb(databaseForUri.getWritableDatabase(), dump(databaseForUri, false));
            }
            if ("internal".equals(this.mMediaScannerVolume)) {
                SharedPreferences.Editor editorEdit = getContext().getSharedPreferences("MediaScanBuild", 0).edit();
                editorEdit.putString("lastScanFingerprint", Build.FINGERPRINT);
                editorEdit.apply();
            }
            this.mMediaScannerVolume = null;
            pruneThumbnails();
            return 1;
        }
        if (iMatch == 1201) {
            return deleteMtpTransferFile();
        }
        if (iMatch == 301) {
            detachVolume(uriSafeUncanonicalize);
        } else if (iMatch == 705) {
            synchronized (this.mMtpServiceConnection) {
                Log.v(TAG, "delete: match = " + iMatch + ", MTP_CONNECTED= 705");
                if (this.mMtpService != null) {
                    Log.v(TAG, "unbind the MtpServiceConnection and delete the MtpService ");
                    getContext().unbindService(this.mMtpServiceConnection);
                    this.mMtpService = null;
                    deleteMtpTransferFile();
                } else {
                    Log.v(TAG, "there is no MtpService, recount!! ");
                    i3 = 0;
                }
            }
        } else {
            String volumeName = getVolumeName(uriSafeUncanonicalize);
            boolean zEquals = "external".equals(volumeName);
            DatabaseHelper databaseForUri2 = getDatabaseForUri(uriSafeUncanonicalize);
            if (databaseForUri2 == null) {
                throw new UnsupportedOperationException("Unknown URI: " + uriSafeUncanonicalize + " match: " + iMatch);
            }
            databaseForUri2.mNumDeletes++;
            SQLiteDatabase writableDatabase = databaseForUri2.getWritableDatabase();
            if (writableDatabase == null) {
                Log.e(TAG, "delete with Null db!");
                return -1;
            }
            TableAndWhere tableAndWhere3 = getTableAndWhere(uriSafeUncanonicalize, iMatch, str);
            if (tableAndWhere3.table.equals("files")) {
                String queryParameter = uriSafeUncanonicalize.getQueryParameter("deletedata");
                if (queryParameter == null || !queryParameter.equals("false")) {
                    databaseForUri2.mNumQueries++;
                    TableAndWhere tableAndWhere4 = tableAndWhere3;
                    sQLiteDatabase = writableDatabase;
                    DatabaseHelper databaseHelper3 = databaseForUri2;
                    String str5 = volumeName;
                    cursorQuery = writableDatabase.query(tableAndWhere3.table, sMediaTypeDataId, tableAndWhere3.where, strArr, null, null, null);
                    String[] strArr2 = {""};
                    String[] strArr3 = {"", ""};
                    if (cursorQuery != null) {
                        ArrayList<String> arrayList = new ArrayList(cursorQuery.getCount());
                        sQLiteDatabase.beginTransaction();
                        MiniThumbFile miniThumbFileInstance = null;
                        MiniThumbFile miniThumbFileInstance2 = null;
                        while (cursorQuery.moveToNext()) {
                            try {
                                int i4 = cursorQuery.getInt(i2);
                                String string = cursorQuery.getString(1);
                                int i5 = iMatch;
                                TableAndWhere tableAndWhere5 = tableAndWhere4;
                                long j = cursorQuery.getLong(2);
                                if (i4 == 1) {
                                    arrayList.add(string);
                                    String str6 = str5;
                                    MediaDocumentsProvider.onMediaStoreDelete(getContext(), str6, 1, j);
                                    strArr2[0] = String.valueOf(j);
                                    databaseHelper2 = databaseHelper3;
                                    databaseHelper2.mNumQueries++;
                                    cursorQuery = sQLiteDatabase.query("thumbnails", sDataOnlyColumn, "image_id=?", strArr2, null, null, null);
                                    if (cursorQuery != null) {
                                        while (cursorQuery.moveToNext()) {
                                            try {
                                                arrayList.add(cursorQuery.getString(0));
                                                str6 = str6;
                                            } finally {
                                            }
                                        }
                                        str4 = str6;
                                        databaseHelper2.mNumDeletes++;
                                        sQLiteDatabase.delete("thumbnails", "image_id=?", strArr2);
                                        IoUtils.closeQuietly(cursorQuery);
                                        if (zEquals) {
                                            if (miniThumbFileInstance == null) {
                                                miniThumbFileInstance = MiniThumbFile.instance(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                            }
                                            miniThumbFileInstance.eraseMiniThumb(j);
                                        }
                                    } else {
                                        str4 = str6;
                                    }
                                    str3 = str4;
                                } else {
                                    databaseHelper2 = databaseHelper3;
                                    str3 = str5;
                                    if (i4 == 3) {
                                        arrayList.add(string);
                                        MediaDocumentsProvider.onMediaStoreDelete(getContext(), str3, 3, j);
                                        strArr2[0] = String.valueOf(j);
                                        databaseHelper2.mNumQueries++;
                                        cursorQuery = sQLiteDatabase.query("videothumbnails", sDataOnlyColumn, "video_id=?", strArr2, null, null, null);
                                        while (cursorQuery.moveToNext()) {
                                            try {
                                                deleteIfAllowed(uriSafeUncanonicalize, cursorQuery.getString(0));
                                            } finally {
                                            }
                                        }
                                        databaseHelper2.mNumDeletes++;
                                        sQLiteDatabase.delete("videothumbnails", "video_id=?", strArr2);
                                        IoUtils.closeQuietly(cursorQuery);
                                        if (zEquals) {
                                            if (miniThumbFileInstance2 == null) {
                                                miniThumbFileInstance2 = MiniThumbFile.instance(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                                            }
                                            miniThumbFileInstance2.eraseMiniThumb(j);
                                        }
                                    } else if (i4 == 2 && !databaseHelper2.mInternal) {
                                        MediaDocumentsProvider.onMediaStoreDelete(getContext(), str3, 2, j);
                                        strArr2[0] = String.valueOf(j);
                                        databaseHelper2.mNumDeletes += 2;
                                        sQLiteDatabase.delete("audio_genres_map", "audio_id=?", strArr2);
                                        cursorQuery = sQLiteDatabase.query("audio_playlists_map", sPlaylistIdPlayOrder, "audio_id=?", strArr2, null, null, null);
                                        if (cursorQuery != null) {
                                            while (cursorQuery.moveToNext()) {
                                                try {
                                                    strArr3[0] = "" + cursorQuery.getLong(0);
                                                    strArr3[1] = "" + cursorQuery.getInt(1);
                                                    databaseHelper2.mNumUpdates = databaseHelper2.mNumUpdates + 1;
                                                    sQLiteDatabase.execSQL("UPDATE audio_playlists_map SET play_order=play_order-1 WHERE playlist_id=? AND play_order>?", strArr3);
                                                } finally {
                                                }
                                            }
                                            sQLiteDatabase.delete("audio_playlists_map", "audio_id=?", strArr2);
                                            IoUtils.closeQuietly(cursorQuery);
                                        }
                                    }
                                }
                                str5 = str3;
                                databaseHelper3 = databaseHelper2;
                                iMatch = i5;
                                tableAndWhere4 = tableAndWhere5;
                                i2 = 0;
                            } catch (Throwable th) {
                                if (miniThumbFileInstance != null) {
                                    miniThumbFileInstance.deactivate();
                                }
                                if (miniThumbFileInstance2 != null) {
                                    miniThumbFileInstance2.deactivate();
                                }
                                sQLiteDatabase.endTransaction();
                                throw th;
                            }
                        }
                        i = iMatch;
                        tableAndWhere2 = tableAndWhere4;
                        databaseHelper = databaseHelper3;
                        str2 = str5;
                        sQLiteDatabase.setTransactionSuccessful();
                        if (miniThumbFileInstance != null) {
                            miniThumbFileInstance.deactivate();
                        }
                        if (miniThumbFileInstance2 != null) {
                            miniThumbFileInstance2.deactivate();
                        }
                        sQLiteDatabase.endTransaction();
                        if (!arrayList.isEmpty()) {
                            for (String str7 : arrayList) {
                                deleteIfAllowed(uriSafeUncanonicalize, str7);
                                Log.d(TAG, "delete real file " + str7);
                            }
                        }
                    } else {
                        i = iMatch;
                        tableAndWhere2 = tableAndWhere4;
                        databaseHelper = databaseHelper3;
                        str2 = str5;
                    }
                } else {
                    tableAndWhere2 = tableAndWhere3;
                    sQLiteDatabase = writableDatabase;
                    str2 = volumeName;
                    i = iMatch;
                    databaseHelper = databaseForUri2;
                }
                tableAndWhere = tableAndWhere2;
                if (TextUtils.isEmpty(tableAndWhere.where)) {
                    tableAndWhere.where = "_id NOT IN (SELECT parent FROM files)";
                } else {
                    tableAndWhere.where = "(" + tableAndWhere.where + ") AND (_id NOT IN (SELECT parent FROM files WHERE NOT (" + tableAndWhere.where + ")))";
                }
            } else {
                tableAndWhere = tableAndWhere3;
                sQLiteDatabase = writableDatabase;
                str2 = volumeName;
                i = iMatch;
                databaseHelper = databaseForUri2;
            }
            switch (i) {
                case 3:
                case 4:
                case 202:
                case 203:
                    DatabaseHelper databaseHelper4 = databaseHelper;
                    cursorQuery = sQLiteDatabase.query(tableAndWhere.table, sDataOnlyColumn, tableAndWhere.where, strArr, null, null, null);
                    if (cursorQuery != null) {
                        while (cursorQuery.moveToNext()) {
                            try {
                                deleteIfAllowed(uriSafeUncanonicalize, cursorQuery.getString(0));
                            } finally {
                            }
                        }
                    }
                    databaseHelper4.mNumDeletes++;
                    iDelete = sQLiteDatabase.delete(tableAndWhere.table, tableAndWhere.where, strArr);
                    break;
                case 108:
                    databaseHelper.mNumDeletes++;
                    iDelete = sQLiteDatabase.delete("audio_genres_map", tableAndWhere.where, strArr);
                    break;
                case 702:
                case 703:
                    databaseHelper.mNumDeletes++;
                    iDelete = sQLiteDatabase.delete("files", tableAndWhere.where, strArr);
                    break;
                default:
                    databaseHelper.mNumDeletes++;
                    iDelete = sQLiteDatabase.delete(tableAndWhere.table, tableAndWhere.where, strArr);
                    break;
            }
            i3 = iDelete;
            getContext().getContentResolver().notifyChange(Uri.parse("content://media/" + str2), null);
            if (MediaUtils.LOG_DELETE) {
                Log.d(TAG, "delete: uri=" + uriSafeUncanonicalize + ", count=" + i3 + ", match=" + i + ", userWhere=" + str + ", whereArgs=" + Arrays.toString(strArr) + ", caller pid = " + Binder.getCallingPid());
            }
            return i3;
        }
        i = iMatch;
        if (MediaUtils.LOG_DELETE) {
        }
        return i3;
    }

    @Override
    public Bundle call(String str, String str2, Bundle bundle) throws Exception {
        String str3 = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("call: method = ");
        sb.append(str == null ? "null" : str);
        Log.d(str3, sb.toString());
        if ("unhide".equals(str)) {
            processRemovedNoMediaPath(str2);
            return null;
        }
        if ("update_titles".equals(str)) {
            localizeTitles();
            return null;
        }
        if ("action_media_unmounted".equals(str)) {
            DatabaseHelper databaseForUri = getDatabaseForUri(Uri.parse("content://media/external"));
            StorageVolume storageVolume = (StorageVolume) bundle.getParcelable("android.os.storage.extra.STORAGE_VOLUME");
            boolean z = bundle.getBoolean("mount_unmount_all", false);
            if (storageVolume.isPrimary()) {
                this.mNeedWaitStorageStateChange = false;
            } else if (databaseForUri != null && !databaseForUri.mDeleteAllSdcardEntries && !z) {
                deleteAllEntriesForStorage(getContext(), storageVolume);
            }
            return null;
        }
        if ("action_remove_overtime".equals(str)) {
            this.mMediaProviderUtils.removeOverTimeDbItems();
            return null;
        }
        if ("action_prescan_done".equals(str)) {
            setAsPrescanState(false);
            getContext().getContentResolver().notifyChange(MediaStore.Audio.Media.getContentUri("external"), null);
            getContext().getContentResolver().notifyChange(MediaStore.Images.Media.getContentUri("external"), null);
            getContext().getContentResolver().notifyChange(MediaStore.Video.Media.getContentUri("external"), null);
            getContext().getContentResolver().notifyChange(MediaStore.Files.getContentUri("external"), null);
            return null;
        }
        if ("action_prescan_started".equals(str)) {
            setAsPrescanState(true);
            return null;
        }
        throw new UnsupportedOperationException("Unsupported call: " + str);
    }

    private void pruneThumbnails() {
        Log.v(TAG, "pruneThumbnails ");
        SQLiteDatabase writableDatabase = getDatabaseForUri(MediaStore.Images.Thumbnails.getContentUri("external")).getWritableDatabase();
        writableDatabase.execSQL("delete from thumbnails where image_id not in (select _id from images)");
        writableDatabase.execSQL("delete from videothumbnails where video_id not in (select _id from video)");
        HashSet hashSet = new HashSet();
        try {
            File canonicalFile = new File("/sdcard/DCIM/.thumbnails").getCanonicalFile();
            String[] list = canonicalFile.list();
            if (list == null) {
                list = new String[0];
            }
            String path = canonicalFile.getPath();
            for (int i = 0; i < list.length; i++) {
                if (list[i].endsWith(".jpg")) {
                    hashSet.add(path + "/" + list[i]);
                }
            }
            for (String str : new String[]{"thumbnails", "videothumbnails"}) {
                Cursor cursorQuery = writableDatabase.query(str, new String[]{"_data"}, null, null, null, null, null);
                if (cursorQuery != null && cursorQuery.moveToFirst()) {
                    do {
                        hashSet.remove(cursorQuery.getString(0));
                    } while (cursorQuery.moveToNext());
                }
                IoUtils.closeQuietly(cursorQuery);
            }
            Iterator it = hashSet.iterator();
            while (it.hasNext()) {
                try {
                    new File((String) it.next()).delete();
                } catch (SecurityException e) {
                }
            }
            Log.v(TAG, "/pruneDeadThumbnailFiles... ");
        } catch (IOException e2) {
        }
    }

    private void removeThumbnailFor(Uri uri, SQLiteDatabase sQLiteDatabase, long j) {
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("select _data from thumbnails where image_id=" + j + " union all select _data from videothumbnails where video_id=" + j, null);
        if (cursorRawQuery != null) {
            while (cursorRawQuery.moveToNext()) {
                deleteIfAllowed(uri, cursorRawQuery.getString(0));
            }
            IoUtils.closeQuietly(cursorRawQuery);
            sQLiteDatabase.execSQL("delete from thumbnails where image_id=" + j);
            sQLiteDatabase.execSQL("delete from videothumbnails where video_id=" + j);
        }
        MiniThumbFile miniThumbFileInstance = MiniThumbFile.instance(uri);
        miniThumbFileInstance.eraseMiniThumb(j);
        miniThumbFileInstance.deactivate();
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) throws Throwable {
        String str2;
        int i;
        TableAndWhere tableAndWhere;
        String str3;
        int i2;
        TableAndWhere tableAndWhere2;
        int i3;
        int i4;
        int i5;
        String[] strArr2;
        Cursor cursorQuery;
        String string;
        long j;
        File file;
        ContentObserver contentObserver;
        char c;
        int i6;
        SQLiteDatabase sQLiteDatabase;
        Uri uri2;
        int iUpdate;
        int i7;
        String str4;
        String str5;
        TableAndWhere tableAndWhere3;
        SQLiteDatabase sQLiteDatabase2;
        DatabaseHelper databaseHelper;
        int i8;
        Uri uri3;
        DatabaseHelper databaseHelper2;
        int i9;
        Uri uri4;
        Uri uri5;
        int i10;
        int i11;
        Uri uri6;
        String str6;
        int iHashCode;
        Long l;
        long jLongValue;
        long jLongValue2;
        String asString;
        int i12;
        int i13;
        Uri uriSafeUncanonicalize = safeUncanonicalize(uri);
        if (MediaUtils.LOG_UPDATE) {
            MtkLog.v(TAG, "update for uri=" + uriSafeUncanonicalize + ", initValues=" + contentValues);
        }
        int iMatch = URI_MATCHER.match(uriSafeUncanonicalize);
        DatabaseHelper databaseForUri = getDatabaseForUri(uriSafeUncanonicalize);
        if (databaseForUri == null) {
            throw new UnsupportedOperationException("Unknown URI: " + uriSafeUncanonicalize);
        }
        databaseForUri.mNumUpdates++;
        SQLiteDatabase writableDatabase = databaseForUri.getWritableDatabase();
        if (writableDatabase == null) {
            Log.e(TAG, "update with Null db!");
            return -1;
        }
        if (contentValues != null) {
            String asString2 = contentValues.getAsString("genre");
            contentValues.remove("genre");
            str2 = asString2;
        } else {
            str2 = null;
        }
        TableAndWhere tableAndWhere4 = getTableAndWhere(uriSafeUncanonicalize, iMatch, str);
        if (contentValues.containsKey("media_type")) {
            long jLongValue3 = contentValues.getAsLong("media_type").longValue();
            databaseForUri.mNumQueries++;
            tableAndWhere = tableAndWhere4;
            str3 = str2;
            cursorQuery = writableDatabase.query(tableAndWhere4.table, sMediaTableColumns, tableAndWhere4.where, strArr, null, null, null);
            while (cursorQuery != null) {
                try {
                    if (!cursorQuery.moveToNext()) {
                        break;
                    }
                    long j2 = cursorQuery.getLong(1);
                    if (j2 == 1 && jLongValue3 != 1) {
                        Log.i(TAG, "need to remove image thumbnail for id " + cursorQuery.getString(0));
                        removeThumbnailFor(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, writableDatabase, cursorQuery.getLong(0));
                    } else if (j2 == 3 && jLongValue3 != 3) {
                        Log.i(TAG, "need to remove video thumbnail for id " + cursorQuery.getString(0));
                        removeThumbnailFor(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, writableDatabase, cursorQuery.getLong(0));
                    }
                } finally {
                }
            }
            i = 0;
        } else {
            i = 0;
            tableAndWhere = tableAndWhere4;
            str3 = str2;
        }
        if ((iMatch == 702 || iMatch == 703 || iMatch == 706) && contentValues != null && ((contentValues.size() == 1 && contentValues.containsKey("_data")) || (contentValues.size() == 2 && contentValues.containsKey("_data") && contentValues.containsKey("parent")))) {
            String asString3 = contentValues.getAsString("_data");
            this.mDirectoryCache.remove(asString3);
            File file2 = new File(asString3);
            if (asString3 == null || !file2.isDirectory()) {
                i2 = 2;
                tableAndWhere2 = tableAndWhere;
                i3 = 3;
                i4 = 5;
                i5 = i;
                strArr2 = strArr;
                if (asString3.toLowerCase(Locale.US).endsWith("/.nomedia")) {
                    processNewNoMediaPath(databaseForUri, writableDatabase, asString3);
                }
            } else {
                databaseForUri.mNumQueries++;
                TableAndWhere tableAndWhere5 = tableAndWhere;
                cursorQuery = writableDatabase.query(tableAndWhere5.table, PATH_PROJECTION, str, strArr, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToNext()) {
                            string = cursorQuery.getString(1);
                            i5 = 0;
                            j = cursorQuery.getLong(0);
                        } else {
                            i5 = 0;
                            j = 0;
                            string = null;
                        }
                        if (string != null) {
                            this.mDirectoryCache.remove(string);
                            FileSearchHelper.computeFileName(asString3, contentValues);
                            contentValues.put("parent", Long.valueOf(getParent(databaseForUri, writableDatabase, asString3)));
                            databaseForUri.mNumUpdates++;
                            int iUpdate2 = writableDatabase.update(tableAndWhere5.table, contentValues, tableAndWhere5.where, strArr);
                            if (iUpdate2 > 0) {
                                Object[] objArr = new Object[6];
                                objArr[i5] = asString3;
                                objArr[1] = Integer.valueOf(string.length() + 1);
                                c = 2;
                                objArr[2] = string + "/";
                                objArr[3] = string + "0";
                                file = file2;
                                objArr[4] = file.getName();
                                objArr[5] = Integer.valueOf(file.toString().toLowerCase().hashCode());
                                databaseForUri.mNumUpdates++;
                                writableDatabase.execSQL("UPDATE files SET _data=?1||SUBSTR(_data, ?2),bucket_display_name=?5,bucket_id=?6 WHERE _data >= ?3 AND _data < ?4;", objArr);
                                if (j > 0) {
                                    databaseForUri.mNumUpdates++;
                                    ContentValues contentValues2 = new ContentValues();
                                    FileSearchHelper.computeRingtoneAttributes(string, asString3, contentValues2);
                                    if (contentValues2.size() > 0) {
                                        String str7 = tableAndWhere5.table;
                                        contentObserver = null;
                                        writableDatabase.update(str7, contentValues2, "parent=" + j + " AND media_type=2", null);
                                    } else {
                                        contentObserver = null;
                                    }
                                }
                            } else {
                                file = file2;
                                contentObserver = null;
                                c = 2;
                            }
                            if (iUpdate2 > 0 && !writableDatabase.inTransaction()) {
                                getContext().getContentResolver().notifyChange(Uri.parse("content://media/" + getVolumeName(uriSafeUncanonicalize)), contentObserver);
                            }
                            if (file.getName().startsWith(".")) {
                                processNewNoMediaPath(databaseForUri, writableDatabase, asString3);
                            }
                            if (string.contains("/.") && !asString3.contains("/.")) {
                                String[] strArr3 = new String[3];
                                strArr3[0] = asString3 + "/%";
                                strArr3[1] = "" + (asString3.length() + 1);
                                strArr3[c] = asString3 + "/";
                                databaseForUri.mNumDeletes = databaseForUri.mNumDeletes + writableDatabase.delete("files", "_data LIKE ? AND lower(substr(_data,1,?))=lower(?)", strArr3);
                            }
                            return iUpdate2;
                        }
                        strArr2 = strArr;
                        tableAndWhere2 = tableAndWhere5;
                        i3 = 3;
                        i2 = 2;
                        i4 = 5;
                    } finally {
                    }
                }
            }
        } else {
            i2 = 2;
            tableAndWhere2 = tableAndWhere;
            i3 = 3;
            i4 = 5;
            i5 = i;
            strArr2 = strArr;
        }
        switch (iMatch) {
            case 1:
            case 2:
            case 200:
            case 201:
                i6 = iMatch;
                String[] strArr4 = strArr2;
                sQLiteDatabase = writableDatabase;
                TableAndWhere tableAndWhere6 = tableAndWhere2;
                ContentValues contentValues3 = new ContentValues(contentValues);
                contentValues3.remove("bucket_id");
                contentValues3.remove("bucket_display_name");
                String asString4 = contentValues3.getAsString("_data");
                if (asString4 != null) {
                    computeBucketValues(asString4, contentValues3);
                    FileSearchHelper.computeFileName(asString4, contentValues3);
                    FileSearchHelper.computeFileType(asString4, contentValues3);
                }
                computeTakenTime(contentValues3);
                databaseForUri.mNumUpdates++;
                int iUpdate3 = sQLiteDatabase.update(tableAndWhere6.table, contentValues3, tableAndWhere6.where, strArr4);
                if (iUpdate3 > 0 && contentValues3.getAsString("_data") != null) {
                    databaseForUri.mNumQueries++;
                    String str8 = tableAndWhere6.table;
                    String[] strArr5 = READY_FLAG_PROJECTION;
                    String str9 = tableAndWhere6.where;
                    int i14 = i2;
                    cursorQuery = sQLiteDatabase.query(str8, strArr5, str9, strArr4, null, null, null);
                    if (cursorQuery != null) {
                        while (cursorQuery.moveToNext()) {
                            try {
                                if (cursorQuery.getLong(i14) == 0) {
                                    requestMediaThumbnail(cursorQuery.getString(1), uriSafeUncanonicalize, 10, 0L);
                                }
                            } finally {
                            }
                        }
                    }
                    break;
                }
                uri2 = uriSafeUncanonicalize;
                iUpdate = iUpdate3;
                i7 = 0;
                if (iUpdate > 0 && !sQLiteDatabase.inTransaction()) {
                    if (i6 != 702 || i6 == 703 || i6 == 700 || i6 == 701) {
                        getContext().getContentResolver().notifyChange(Uri.parse("content://media/" + uri2.getPathSegments().get(i7)), null);
                    } else {
                        getContext().getContentResolver().notifyChange(uri2, null);
                    }
                }
                return iUpdate;
            case 100:
            case 101:
                ContentValues contentValues4 = new ContentValues(contentValues);
                String asString5 = contentValues4.getAsString("album_artist");
                String asString6 = contentValues4.getAsString("compilation");
                contentValues4.remove("compilation");
                String asString7 = contentValues4.getAsString("artist");
                contentValues4.remove("artist");
                if (asString7 != null) {
                    ConcurrentHashMap<String, Long> concurrentHashMap = databaseForUri.mArtistCache;
                    Long l2 = concurrentHashMap.get(asString7);
                    if (l2 == null) {
                        str4 = asString7;
                        str5 = asString6;
                        tableAndWhere3 = tableAndWhere2;
                        sQLiteDatabase2 = writableDatabase;
                        databaseHelper = databaseForUri;
                        i8 = iMatch;
                        uri3 = uriSafeUncanonicalize;
                        jLongValue2 = getKeyIdForName(databaseForUri, writableDatabase, "artists", "artist_key", "artist", str4, str4, null, 0, null, concurrentHashMap, uriSafeUncanonicalize);
                    } else {
                        str4 = asString7;
                        str5 = asString6;
                        tableAndWhere3 = tableAndWhere2;
                        sQLiteDatabase2 = writableDatabase;
                        databaseHelper = databaseForUri;
                        i8 = iMatch;
                        uri3 = uriSafeUncanonicalize;
                        jLongValue2 = l2.longValue();
                    }
                    contentValues4.put("artist_id", Integer.toString((int) jLongValue2));
                } else {
                    str4 = asString7;
                    str5 = asString6;
                    tableAndWhere3 = tableAndWhere2;
                    sQLiteDatabase2 = writableDatabase;
                    databaseHelper = databaseForUri;
                    i8 = iMatch;
                    uri3 = uriSafeUncanonicalize;
                }
                String asString8 = contentValues4.getAsString("album");
                contentValues4.remove("album");
                if (asString8 != null) {
                    String asString9 = contentValues4.getAsString("_data");
                    if (asString5 != null) {
                        iHashCode = asString5.hashCode();
                        str6 = asString9;
                        i10 = i8;
                        uri6 = uri3;
                    } else {
                        String str10 = str5;
                        if (str10 == null || !str10.equals("1")) {
                            if (asString9 == null) {
                                i10 = i8;
                                if (i10 == 100) {
                                    Log.w(TAG, "Possible multi row album name update without path could give wrong album key");
                                } else {
                                    Cursor cursorQuery2 = query(uri3, new String[]{"_data"}, null, null, null);
                                    if (cursorQuery2 != null) {
                                        try {
                                            int count = cursorQuery2.getCount();
                                            if (count == 1) {
                                                cursorQuery2.moveToFirst();
                                                i11 = 0;
                                                asString9 = cursorQuery2.getString(0);
                                                uri6 = uri3;
                                            } else {
                                                i11 = 0;
                                                String str11 = TAG;
                                                StringBuilder sb = new StringBuilder();
                                                sb.append("");
                                                sb.append(count);
                                                sb.append(" rows for ");
                                                uri6 = uri3;
                                                sb.append(uri6);
                                                Log.e(str11, sb.toString());
                                            }
                                        } finally {
                                            IoUtils.closeQuietly(cursorQuery2);
                                        }
                                    }
                                    if (asString9 == null) {
                                        iHashCode = asString9.substring(i11, asString9.lastIndexOf(47)).hashCode();
                                        str6 = asString9;
                                    } else {
                                        str6 = asString9;
                                        iHashCode = i11;
                                    }
                                    String string2 = asString8.toString();
                                    DatabaseHelper databaseHelper3 = databaseHelper;
                                    ConcurrentHashMap<String, Long> concurrentHashMap2 = databaseHelper3.mAlbumCache;
                                    String str12 = string2 + iHashCode;
                                    l = concurrentHashMap2.get(str12);
                                    if (l == null) {
                                        databaseHelper2 = databaseHelper3;
                                        uri4 = uri6;
                                        i9 = i10;
                                        jLongValue = getKeyIdForName(databaseHelper3, sQLiteDatabase2, "albums", "album_key", "album", string2, str12, str6, iHashCode, str4, concurrentHashMap2, uri4);
                                    } else {
                                        databaseHelper2 = databaseHelper3;
                                        uri4 = uri6;
                                        i9 = i10;
                                        jLongValue = l.longValue();
                                    }
                                    contentValues4.put("album_id", Integer.toString((int) jLongValue));
                                }
                            } else {
                                i10 = i8;
                            }
                            uri6 = uri3;
                            i11 = 0;
                            if (asString9 == null) {
                            }
                            String string22 = asString8.toString();
                            DatabaseHelper databaseHelper32 = databaseHelper;
                            ConcurrentHashMap<String, Long> concurrentHashMap22 = databaseHelper32.mAlbumCache;
                            String str122 = string22 + iHashCode;
                            l = concurrentHashMap22.get(str122);
                            if (l == null) {
                            }
                            contentValues4.put("album_id", Integer.toString((int) jLongValue));
                        } else {
                            str6 = asString9;
                            i10 = i8;
                            uri6 = uri3;
                            iHashCode = 0;
                        }
                    }
                    String string222 = asString8.toString();
                    DatabaseHelper databaseHelper322 = databaseHelper;
                    ConcurrentHashMap<String, Long> concurrentHashMap222 = databaseHelper322.mAlbumCache;
                    String str1222 = string222 + iHashCode;
                    l = concurrentHashMap222.get(str1222);
                    if (l == null) {
                    }
                    contentValues4.put("album_id", Integer.toString((int) jLongValue));
                } else {
                    databaseHelper2 = databaseHelper;
                    i9 = i8;
                    uri4 = uri3;
                }
                contentValues4.remove("title_key");
                String asString10 = contentValues4.getAsString("title");
                if (asString10 != null) {
                    try {
                        String localizedTitle = getLocalizedTitle(asString10);
                        if (localizedTitle != null) {
                            contentValues4.put("title_resource_uri", asString10);
                            asString10 = localizedTitle;
                        } else {
                            contentValues4.putNull("title_resource_uri");
                        }
                    } catch (Exception e) {
                        contentValues4.put("title_resource_uri", asString10);
                    }
                    contentValues4.put("title_key", MediaStore.Audio.keyFor(asString10));
                    contentValues4.put("title", asString10.trim());
                    break;
                }
                String asString11 = contentValues4.getAsString("_data");
                if (asString11 != null) {
                    FileSearchHelper.computeFileName(asString11, contentValues4);
                    FileSearchHelper.computeFileType(asString11, contentValues4);
                }
                databaseHelper2.mNumUpdates++;
                TableAndWhere tableAndWhere7 = tableAndWhere3;
                sQLiteDatabase = sQLiteDatabase2;
                iUpdate = sQLiteDatabase.update(tableAndWhere7.table, contentValues4, tableAndWhere7.where, strArr);
                String str13 = str3;
                if (str13 != null) {
                    if (iUpdate == 1) {
                        i6 = i9;
                        if (i6 == 101) {
                            uri5 = uri4;
                            updateGenre(Long.parseLong(uri5.getPathSegments().get(3)), str13);
                        } else {
                            uri5 = uri4;
                        }
                    } else {
                        uri5 = uri4;
                        i6 = i9;
                    }
                    Log.w(TAG, "ignoring genre in update: count = " + iUpdate + " match = " + i6);
                } else {
                    uri5 = uri4;
                    i6 = i9;
                }
                uri2 = uri5;
                i7 = 0;
                if (iUpdate > 0) {
                    if (i6 != 702) {
                        getContext().getContentResolver().notifyChange(Uri.parse("content://media/" + uri2.getPathSegments().get(i7)), null);
                    }
                }
                return iUpdate;
            case 110:
            case 111:
                contentValues.getAsString("name");
                databaseForUri.mNumUpdates++;
                iUpdate = writableDatabase.update(tableAndWhere2.table, contentValues, tableAndWhere2.where, strArr2);
                i7 = i5;
                i6 = iMatch;
                uri2 = uriSafeUncanonicalize;
                sQLiteDatabase = writableDatabase;
                if (iUpdate > 0) {
                }
                return iUpdate;
            case 113:
                if (uriSafeUncanonicalize.getQueryParameter("move") != null) {
                    if (contentValues.containsKey("play_order")) {
                        int iIntValue = contentValues.getAsInteger("play_order").intValue();
                        List<String> pathSegments = uriSafeUncanonicalize.getPathSegments();
                        return movePlaylistEntry(databaseForUri, writableDatabase, Long.parseLong(pathSegments.get(i3)), Integer.parseInt(pathSegments.get(i4)), iIntValue);
                    }
                    throw new IllegalArgumentException("Need to specify play_order when using 'move' parameter");
                }
            default:
                i6 = iMatch;
                String[] strArr6 = strArr2;
                sQLiteDatabase = writableDatabase;
                TableAndWhere tableAndWhere8 = tableAndWhere2;
                if (!"files".equals(tableAndWhere8.table) || (asString = contentValues.getAsString("_data")) == null) {
                    uri2 = uriSafeUncanonicalize;
                    i7 = 0;
                } else {
                    FileSearchHelper.computeFileName(asString, contentValues);
                    FileSearchHelper.computeFileType(asString, contentValues);
                    if (asString.endsWith(".mudp")) {
                        contentValues.put("is_drm", (Boolean) true);
                        i12 = 0;
                        contentValues.put("media_type", (Integer) 0);
                    } else {
                        i12 = 0;
                    }
                    if ("true".equalsIgnoreCase(uriSafeUncanonicalize.getQueryParameter("need_update_media_values"))) {
                        i7 = i12;
                        cursorQuery = sQLiteDatabase.query(tableAndWhere8.table, sMediaTypeDataId, tableAndWhere8.where, strArr6, null, null, null);
                        if (cursorQuery != null) {
                            try {
                                if (cursorQuery.moveToFirst()) {
                                    int i15 = cursorQuery.getInt(i7);
                                    cursorQuery.getLong(2);
                                    contentValues.put("parent", Long.valueOf(getParent(databaseForUri, sQLiteDatabase, asString)));
                                    if (!contentValues.containsKey("_display_name")) {
                                        computeDisplayName(asString, contentValues);
                                    }
                                    if (i15 != 3) {
                                        i13 = 1;
                                        if (i15 == 1) {
                                        }
                                        if (i15 == i13 && !contentValues.containsKey("title")) {
                                            contentValues.put("title", MediaFile.getFileTitle(asString));
                                        }
                                        if (!MediaUtils.LOG_UPDATE) {
                                            String str14 = TAG;
                                            StringBuilder sb2 = new StringBuilder();
                                            sb2.append("update: values=");
                                            sb2.append(contentValues);
                                            sb2.append(",uri=");
                                            uri2 = uriSafeUncanonicalize;
                                            sb2.append(uri2);
                                            MtkLog.v(str14, sb2.toString());
                                        } else {
                                            uri2 = uriSafeUncanonicalize;
                                        }
                                    } else {
                                        i13 = 1;
                                    }
                                    computeBucketValues(asString, contentValues);
                                    if (i15 == i13) {
                                        contentValues.put("title", MediaFile.getFileTitle(asString));
                                    }
                                    if (!MediaUtils.LOG_UPDATE) {
                                    }
                                    break;
                                }
                            } finally {
                            }
                        }
                        if (iUpdate > 0) {
                        }
                        return iUpdate;
                    }
                    i7 = i12;
                    uri2 = uriSafeUncanonicalize;
                }
                databaseForUri.mNumUpdates++;
                iUpdate = sQLiteDatabase.update(tableAndWhere8.table, contentValues, tableAndWhere8.where, strArr6);
                if (iUpdate > 0) {
                }
                return iUpdate;
        }
    }

    private int movePlaylistEntry(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, long j, int i, int i2) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor;
        int i3;
        if (i == i2) {
            return 0;
        }
        sQLiteDatabase.beginTransaction();
        try {
            databaseHelper.mNumUpdates += 3;
            Cursor cursorQuery2 = sQLiteDatabase.query("audio_playlists_map", new String[]{"play_order"}, "playlist_id=?", new String[]{"" + j}, null, null, "play_order", i + ",1");
            try {
                cursorQuery2.moveToFirst();
                int i4 = cursorQuery2.getInt(0);
                IoUtils.closeQuietly(cursorQuery2);
                cursor = cursorQuery2;
                try {
                    cursorQuery = sQLiteDatabase.query("audio_playlists_map", new String[]{"play_order"}, "playlist_id=?", new String[]{"" + j}, null, null, "play_order", i2 + ",1");
                    try {
                        cursorQuery.moveToFirst();
                        int i5 = cursorQuery.getInt(0);
                        sQLiteDatabase.execSQL("UPDATE audio_playlists_map SET play_order=-1 WHERE play_order=" + i4 + " AND playlist_id=" + j);
                        if (i < i2) {
                            sQLiteDatabase.execSQL("UPDATE audio_playlists_map SET play_order=play_order-1 WHERE play_order<=" + i5 + " AND play_order>" + i4 + " AND playlist_id=" + j);
                            i3 = (i2 - i) + 1;
                        } else {
                            sQLiteDatabase.execSQL("UPDATE audio_playlists_map SET play_order=play_order+1 WHERE play_order>=" + i5 + " AND play_order<" + i4 + " AND playlist_id=" + j);
                            i3 = (i - i2) + 1;
                        }
                        sQLiteDatabase.execSQL("UPDATE audio_playlists_map SET play_order=" + i5 + " WHERE play_order=-1 AND playlist_id=" + j);
                        sQLiteDatabase.setTransactionSuccessful();
                        sQLiteDatabase.endTransaction();
                        IoUtils.closeQuietly(cursorQuery);
                        getContext().getContentResolver().notifyChange(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI.buildUpon().appendEncodedPath(String.valueOf(j)).build(), null);
                        return i3;
                    } catch (Throwable th) {
                        th = th;
                        sQLiteDatabase.endTransaction();
                        IoUtils.closeQuietly(cursorQuery);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    cursorQuery = cursor;
                    sQLiteDatabase.endTransaction();
                    IoUtils.closeQuietly(cursorQuery);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                cursor = cursorQuery2;
            }
        } catch (Throwable th4) {
            th = th4;
            cursorQuery = null;
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws Throwable {
        ParcelFileDescriptor thumb;
        Cursor cursorQuery;
        ParcelFileDescriptor thumb2;
        Uri uriSafeUncanonicalize = safeUncanonicalize(uri);
        ParcelFileDescriptor thumb3 = null;
        if (URI_MATCHER.match(uriSafeUncanonicalize) != 121) {
            try {
                ParcelFileDescriptor parcelFileDescriptorOpenFileAndEnforcePathPermissionsHelper = openFileAndEnforcePathPermissionsHelper(uriSafeUncanonicalize, str);
                try {
                    thumb = openFileAndEnforcePathPermissionsHelper(uriSafeUncanonicalize, str);
                    if (thumb == null) {
                        try {
                            if (URI_MATCHER.match(uriSafeUncanonicalize) == 120) {
                                Log.w(TAG, "openFile AUDIO_ALBUMART_ID: get ablbum null, try getThumb");
                                DatabaseHelper databaseForUri = getDatabaseForUri(uriSafeUncanonicalize);
                                SQLiteDatabase readableDatabase = databaseForUri.getReadableDatabase();
                                if (readableDatabase == null) {
                                    throw new IllegalStateException("Couldn't open database for " + uriSafeUncanonicalize);
                                }
                                SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
                                int i = Integer.parseInt(uriSafeUncanonicalize.getPathSegments().get(3));
                                sQLiteQueryBuilder.setTables("audio_meta");
                                sQLiteQueryBuilder.appendWhere("album_id=" + i);
                                String strGenerateStorageIdFilter = generateStorageIdFilter(uriSafeUncanonicalize);
                                if (!strGenerateStorageIdFilter.isEmpty()) {
                                    sQLiteQueryBuilder.appendWhere(" AND " + strGenerateStorageIdFilter);
                                }
                                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, new String[]{"_data"}, null, null, null, null, "track");
                                try {
                                    if (cursorQuery.moveToFirst()) {
                                        thumb3 = getThumb(databaseForUri, readableDatabase, cursorQuery.getString(0), i, uriSafeUncanonicalize);
                                    } else {
                                        Log.w(TAG, "openFile: moveToFirst failed! albumId=" + i);
                                        thumb3 = thumb;
                                    }
                                    return thumb3;
                                } finally {
                                }
                            }
                            return thumb;
                        } catch (FileNotFoundException e) {
                            e = e;
                        }
                    } else {
                        return thumb;
                    }
                } catch (FileNotFoundException e2) {
                    e = e2;
                    thumb = parcelFileDescriptorOpenFileAndEnforcePathPermissionsHelper;
                }
            } catch (FileNotFoundException e3) {
                e = e3;
                thumb = thumb3;
            }
            if (!str.contains("w")) {
                if (URI_MATCHER.match(uriSafeUncanonicalize) == 120) {
                    DatabaseHelper databaseForUri2 = getDatabaseForUri(uriSafeUncanonicalize);
                    if (databaseForUri2 == null) {
                        throw e;
                    }
                    SQLiteDatabase readableDatabase2 = databaseForUri2.getReadableDatabase();
                    if (readableDatabase2 == null) {
                        throw new IllegalStateException("Couldn't open database for " + uriSafeUncanonicalize);
                    }
                    SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
                    int i2 = Integer.parseInt(uriSafeUncanonicalize.getPathSegments().get(3));
                    sQLiteQueryBuilder2.setTables("audio_meta");
                    sQLiteQueryBuilder2.appendWhere("album_id=" + i2);
                    String strGenerateStorageIdFilter2 = generateStorageIdFilter(uriSafeUncanonicalize);
                    if (!strGenerateStorageIdFilter2.isEmpty()) {
                        sQLiteQueryBuilder2.appendWhere(" AND " + strGenerateStorageIdFilter2);
                    }
                    cursorQuery = sQLiteQueryBuilder2.query(readableDatabase2, new String[]{"_data"}, null, null, null, null, "track");
                    try {
                        if (cursorQuery.moveToFirst()) {
                            thumb = getThumb(databaseForUri2, readableDatabase2, cursorQuery.getString(0), i2, uriSafeUncanonicalize);
                        } else {
                            Log.w(TAG, "openFile: moveToFirst failed! albumId=" + i2);
                        }
                    } finally {
                    }
                }
                if (thumb == null) {
                    Log.e(TAG, "openFile: failed! uri=" + uriSafeUncanonicalize + ", mode=" + str);
                    throw e;
                }
                return thumb;
            }
            Log.e(TAG, "openFile: FileNotFoundException! uri=" + uriSafeUncanonicalize + ", mode=" + str, e);
            throw e;
        }
        DatabaseHelper databaseForUri3 = getDatabaseForUri(uriSafeUncanonicalize);
        if (databaseForUri3 == null) {
            throw new IllegalStateException("Couldn't open database for " + uriSafeUncanonicalize);
        }
        SQLiteDatabase readableDatabase3 = databaseForUri3.getReadableDatabase();
        if (readableDatabase3 == null) {
            throw new IllegalStateException("Couldn't open database for " + uriSafeUncanonicalize);
        }
        SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
        int i3 = Integer.parseInt(uriSafeUncanonicalize.getPathSegments().get(3));
        sQLiteQueryBuilder3.setTables("audio_meta");
        sQLiteQueryBuilder3.appendWhere("_id=" + i3);
        String strGenerateStorageIdFilter3 = generateStorageIdFilter(uriSafeUncanonicalize);
        if (!strGenerateStorageIdFilter3.isEmpty()) {
            sQLiteQueryBuilder3.appendWhere(" AND " + strGenerateStorageIdFilter3);
        }
        cursorQuery = sQLiteQueryBuilder3.query(readableDatabase3, new String[]{"_data", "album_id"}, null, null, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                String string = cursorQuery.getString(0);
                int i4 = cursorQuery.getInt(1);
                long j = i4;
                try {
                    thumb2 = openFileAndEnforcePathPermissionsHelper(ContentUris.withAppendedId(ALBUMART_URI, j), str);
                    if (thumb2 == null) {
                        Log.w(TAG, "openFile AUDIO_ALBUMART_FILE_ID:get ablbum null, try getThumb");
                        thumb2 = getThumb(databaseForUri3, readableDatabase3, string, j, null);
                        if (thumb2 == null) {
                            Log.w(TAG, "openFile AUDIO_ALBUMART_FILE_ID: get ablbum null try getThumb fail agian");
                        }
                    }
                } catch (FileNotFoundException e4) {
                    Log.w(TAG, "openFile: Can not get thumbnail.albumId=" + i4 + ", audio=" + string);
                    thumb2 = getThumb(databaseForUri3, readableDatabase3, string, j, null);
                }
                thumb3 = thumb2;
            } else {
                Log.w(TAG, "openFile: moveToFirst failed! audioId=" + i3);
            }
            return thumb3;
        } finally {
        }
    }

    private File queryForDataFile(Uri uri) throws Throwable {
        Cursor cursorQuery = query(uri, new String[]{"_data"}, null, null, null);
        if (cursorQuery == null) {
            throw new FileNotFoundException("Missing cursor for " + uri);
        }
        try {
            switch (cursorQuery.getCount()) {
                case 0:
                    throw new FileNotFoundException("No entry for " + uri);
                case 1:
                    if (cursorQuery.moveToFirst()) {
                        String string = cursorQuery.getString(0);
                        if (string == null) {
                            throw new FileNotFoundException("Null path for " + uri);
                        }
                        return new File(string);
                    }
                    throw new FileNotFoundException("Unable to read entry for " + uri);
                default:
                    throw new FileNotFoundException("Multiple items at " + uri);
            }
        } finally {
            IoUtils.closeQuietly(cursorQuery);
        }
    }

    private ParcelFileDescriptor openFileAndEnforcePathPermissionsHelper(Uri uri, String str) throws Throwable {
        int mode = ParcelFileDescriptor.parseMode(str);
        File fileQueryForDataFile = queryForDataFile(uri);
        checkAccess(uri, fileQueryForDataFile, mode);
        if (mode == 268435456) {
            fileQueryForDataFile = Environment.maybeTranslateEmulatedPathToInternal(fileQueryForDataFile);
        }
        return ParcelFileDescriptor.open(fileQueryForDataFile, mode);
    }

    private void deleteIfAllowed(Uri uri, String str) {
        try {
            File file = new File(str);
            checkAccess(uri, file, 536870912);
            file.delete();
        } catch (Exception e) {
            Log.e(TAG, "Couldn't delete " + str);
        }
    }

    private void checkAccess(Uri uri, File file, int i) throws FileNotFoundException {
        boolean z;
        boolean z2;
        boolean z3 = (i & 536870912) != 0;
        try {
            String canonicalPath = file.getCanonicalPath();
            Context context = getContext();
            if (z3) {
                z2 = context.checkCallingOrSelfUriPermission(uri, 2) == 0;
                z = false;
            } else {
                z = context.checkCallingOrSelfUriPermission(uri, 1) == 0;
                z2 = false;
            }
            if (canonicalPath.startsWith(this.mExternalPath) || canonicalPath.startsWith(this.mLegacyPath)) {
                if (z3) {
                    if (z2) {
                        return;
                    }
                    enforceCallingOrSelfPermissionAndAppOps("android.permission.WRITE_EXTERNAL_STORAGE", "External path: " + canonicalPath);
                    return;
                }
                if (z) {
                    return;
                }
                enforceCallingOrSelfPermissionAndAppOps("android.permission.READ_EXTERNAL_STORAGE", "External path: " + canonicalPath);
                return;
            }
            if (canonicalPath.startsWith(this.mCachePath)) {
                if ((!z3 || z2) && z) {
                    return;
                }
                context.enforceCallingOrSelfPermission("android.permission.ACCESS_CACHE_FILESYSTEM", "Cache path: " + canonicalPath);
                return;
            }
            if (isSecondaryExternalPath(canonicalPath)) {
                if (!z && context.checkCallingOrSelfPermission("android.permission.WRITE_MEDIA_STORAGE") == -1) {
                    enforceCallingOrSelfPermissionAndAppOps("android.permission.READ_EXTERNAL_STORAGE", "External path: " + canonicalPath);
                }
                if (!z3 || context.checkCallingOrSelfUriPermission(uri, 2) == 0) {
                    return;
                }
                context.enforceCallingOrSelfPermission("android.permission.WRITE_MEDIA_STORAGE", "External path: " + canonicalPath);
                return;
            }
            if (z3) {
                throw new FileNotFoundException("Can't access " + file);
            }
            boolean z4 = context.checkCallingOrSelfPermission("android.permission.WRITE_MEDIA_STORAGE") == 0;
            boolean z5 = context.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") == 0;
            if (z4 || z5 || !isOtherUserExternalDir(canonicalPath)) {
                checkWorldReadAccess(canonicalPath);
                return;
            }
            throw new FileNotFoundException("Can't access across users " + file);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to resolve canonical path for " + file, e);
        }
    }

    private boolean isOtherUserExternalDir(String str) {
        Iterator it = this.mStorageManager.getVolumes().iterator();
        while (true) {
            if (!it.hasNext()) {
                return false;
            }
            VolumeInfo volumeInfo = (VolumeInfo) it.next();
            if (FileUtils.contains(volumeInfo.path, str)) {
                for (String str2 : this.mExternalStoragePaths) {
                    if (FileUtils.contains(volumeInfo.path, str2) && !FileUtils.contains(str2, str)) {
                        return true;
                    }
                }
            }
        }
    }

    private boolean isSecondaryExternalPath(String str) {
        StorageVolume[] volumeList = this.mStorageManager.getVolumeList();
        if (volumeList == null || volumeList.length <= 0) {
            return false;
        }
        this.mExternalStoragePaths = new String[volumeList.length];
        for (int i = 0; i < volumeList.length; i++) {
            this.mExternalStoragePaths[i] = volumeList[i].getPath();
        }
        for (int i2 = 1; i2 < this.mExternalStoragePaths.length; i2++) {
            String str2 = this.mExternalStoragePaths[i2];
            if (str2 != null && str.startsWith(str2)) {
                return true;
            }
        }
        return false;
    }

    private static void checkWorldReadAccess(String str) throws FileNotFoundException {
        int i = str.startsWith("/storage/") ? OsConstants.S_IRGRP : OsConstants.S_IROTH;
        try {
            StructStat structStatStat = Os.stat(str);
            if (OsConstants.S_ISREG(structStatStat.st_mode) && (structStatStat.st_mode & i) == i) {
                checkLeadingPathComponentsWorldExecutable(str);
                return;
            }
        } catch (ErrnoException e) {
        }
        throw new FileNotFoundException("Can't access " + str);
    }

    private static void checkLeadingPathComponentsWorldExecutable(String str) throws FileNotFoundException {
        int i = str.startsWith("/storage/") ? OsConstants.S_IXGRP : OsConstants.S_IXOTH;
        for (File parentFile = new File(str).getParentFile(); parentFile != null; parentFile = parentFile.getParentFile()) {
            if (!parentFile.exists()) {
                throw new FileNotFoundException("access denied");
            }
            try {
                if ((Os.stat(parentFile.getPath()).st_mode & i) != i) {
                    throw new FileNotFoundException("Can't access " + str);
                }
            } catch (ErrnoException e) {
                throw new FileNotFoundException("Can't access " + str);
            }
        }
    }

    private class ThumbData {
        long album_id;
        Uri albumart_uri;
        SQLiteDatabase db;
        DatabaseHelper helper;
        String path;

        private ThumbData() {
        }
    }

    private void makeThumbAsync(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, String str, long j) {
        synchronized (this.mPendingThumbs) {
            if (this.mPendingThumbs.contains(str)) {
                return;
            }
            this.mPendingThumbs.add(str);
            ThumbData thumbData = new ThumbData();
            thumbData.helper = databaseHelper;
            thumbData.db = sQLiteDatabase;
            thumbData.path = str;
            thumbData.album_id = j;
            thumbData.albumart_uri = ContentUris.withAppendedId(sAlbumArtBaseUri, j);
            synchronized (this.mThumbRequestStack) {
                this.mThumbRequestStack.push(thumbData);
            }
            this.mThumbHandler.obtainMessage(1).sendToTarget();
        }
    }

    private static boolean isRootStorageDir(String[] strArr, String str) {
        for (String str2 : strArr) {
            if (str2 != null && str2.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] getCompressedAlbumArt(Context context, String[] strArr, String str) throws Exception {
        Throwable th;
        int iLastIndexOf;
        String str2;
        ?? r13;
        ?? fileInputStream;
        ?? r132;
        ?? r1;
        ?? r0 = 0;
         = 0;
         = 0;
        ?? r02 = 0;
        try {
            ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(new File(str), 268435456);
            MediaScanner mediaScanner = new MediaScanner(context, "internal");
            try {
                ?? ExtractAlbumArt = mediaScanner.extractAlbumArt(parcelFileDescriptorOpen.getFileDescriptor());
                try {
                    $closeResource(null, mediaScanner);
                    parcelFileDescriptorOpen.close();
                    if (ExtractAlbumArt == 0 && str != null && (iLastIndexOf = str.lastIndexOf(47)) > 0) {
                        String strSubstring = str.substring(0, iLastIndexOf);
                        String absolutePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                        synchronized (sFolderArtMap) {
                            if (sFolderArtMap.containsKey(strSubstring)) {
                                str2 = sFolderArtMap.get(strSubstring);
                            } else if (isRootStorageDir(strArr, strSubstring) || strSubstring.equalsIgnoreCase(absolutePath)) {
                                str2 = null;
                            } else {
                                String[] list = new File(strSubstring).list();
                                if (list == null) {
                                    Log.v(TAG, "getCompressedAlbumArt: No files under " + strSubstring);
                                    return null;
                                }
                                int length = list.length - 1;
                                char c = 1000;
                                String str3 = null;
                                while (true) {
                                    if (length < 0) {
                                        str2 = str3;
                                        break;
                                    }
                                    String lowerCase = list[length].toLowerCase();
                                    if (lowerCase.equals("albumart.jpg")) {
                                        str2 = list[length];
                                        break;
                                    }
                                    if (lowerCase.startsWith("albumart") && lowerCase.endsWith("large.jpg") && c > 1) {
                                        str3 = list[length];
                                        c = 1;
                                    } else if (lowerCase.contains("albumart") && lowerCase.endsWith(".jpg") && c > 2) {
                                        str3 = list[length];
                                        c = 2;
                                    } else if (lowerCase.endsWith(".jpg") && c > 3) {
                                        str3 = list[length];
                                        c = 3;
                                    } else if (lowerCase.endsWith(".png") && c > 4) {
                                        str3 = list[length];
                                        c = 4;
                                    }
                                    length--;
                                    c = c;
                                }
                                sFolderArtMap.put(strSubstring, str2);
                            }
                            if (str2 != null) {
                                File file = new File(strSubstring, str2);
                                ?? Exists = file.exists();
                                try {
                                    if (Exists != 0) {
                                        try {
                                            Exists = new byte[(int) file.length()];
                                        } catch (IOException e) {
                                            e = e;
                                            Exists = ExtractAlbumArt;
                                        } catch (OutOfMemoryError e2) {
                                            e = e2;
                                            Exists = ExtractAlbumArt;
                                        } catch (Throwable th2) {
                                            th = th2;
                                            r13 = 0;
                                            if (r13 != 0) {
                                                r13.close();
                                            }
                                            throw th;
                                        }
                                        try {
                                            fileInputStream = new FileInputStream(file);
                                        } catch (IOException e3) {
                                            e = e3;
                                            fileInputStream = 0;
                                            Log.e(TAG, "getCompressedAlbumArtIOException! best=" + str2, e);
                                            Exists = Exists;
                                            ExtractAlbumArt = fileInputStream;
                                            r1 = Exists;
                                            r132 = fileInputStream;
                                            if (fileInputStream != 0) {
                                                r132.close();
                                                Exists = r1;
                                                ExtractAlbumArt = r132;
                                            }
                                            return r02;
                                        } catch (OutOfMemoryError e4) {
                                            e = e4;
                                            fileInputStream = 0;
                                            Log.e(TAG, "getCompressedAlbumArt:OutOfMemoryError! best=" + str2, e);
                                            Exists = Exists;
                                            ExtractAlbumArt = fileInputStream;
                                            r1 = Exists;
                                            r132 = fileInputStream;
                                            if (fileInputStream != 0) {
                                            }
                                            return r02;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            ExtractAlbumArt = 0;
                                            r13 = ExtractAlbumArt;
                                            if (r13 != 0) {
                                            }
                                            throw th;
                                        }
                                        try {
                                            fileInputStream.read(Exists);
                                            try {
                                                fileInputStream.close();
                                                r02 = Exists;
                                                Exists = Exists;
                                                ExtractAlbumArt = fileInputStream;
                                            } catch (IOException e5) {
                                                e = e5;
                                                r0 = Exists;
                                                Log.e(TAG, "getCompressedAlbumArt: IOException! path=" + str, e);
                                                return r0;
                                            }
                                        } catch (IOException e6) {
                                            e = e6;
                                            Log.e(TAG, "getCompressedAlbumArtIOException! best=" + str2, e);
                                            Exists = Exists;
                                            ExtractAlbumArt = fileInputStream;
                                            r1 = Exists;
                                            r132 = fileInputStream;
                                            if (fileInputStream != 0) {
                                            }
                                        } catch (OutOfMemoryError e7) {
                                            e = e7;
                                            Log.e(TAG, "getCompressedAlbumArt:OutOfMemoryError! best=" + str2, e);
                                            Exists = Exists;
                                            ExtractAlbumArt = fileInputStream;
                                            r1 = Exists;
                                            r132 = fileInputStream;
                                            if (fileInputStream != 0) {
                                            }
                                        }
                                        return r02;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                }
                            }
                        }
                    }
                    return ExtractAlbumArt;
                } catch (IOException e8) {
                    r0 = ExtractAlbumArt;
                    e = e8;
                }
            } catch (Throwable th5) {
                th = th5;
                th = null;
                $closeResource(th, mediaScanner);
                throw th;
            }
        } catch (IOException e9) {
            e = e9;
        }
    }

    Uri getAlbumArtOutputUri(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, long j, Uri uri) throws Throwable {
        Uri uri2 = null;
        if (uri != null) {
            Cursor cursorQuery = query(uri, new String[]{"_data"}, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        if (ensureFileExists(uri, cursorQuery.getString(0))) {
                            uri2 = uri;
                        }
                    } else {
                        uri = null;
                    }
                } finally {
                    IoUtils.closeQuietly(cursorQuery);
                }
            }
        }
        if (uri == null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("album_id", Long.valueOf(j));
            try {
                ContentValues contentValuesEnsureFile = ensureFile(false, contentValues, "", "Android/data/com.android.providers.media/albumthumbs");
                databaseHelper.mNumInserts++;
                long jInsert = sQLiteDatabase.insert("album_art", "_data", contentValuesEnsureFile);
                if (jInsert > 0) {
                    Uri uriWithAppendedId = ContentUris.withAppendedId(ALBUMART_URI, jInsert);
                    try {
                        ensureFileExists(uriWithAppendedId, contentValuesEnsureFile.getAsString("_data"));
                        return uriWithAppendedId;
                    } catch (IllegalStateException e) {
                        uri2 = uriWithAppendedId;
                    }
                } else {
                    return uri2;
                }
            } catch (IllegalStateException e2) {
            }
            Log.e(TAG, "error creating album thumb file");
            return uri2;
        }
        return uri2;
    }

    private void writeAlbumArt(boolean z, Uri uri, byte[] bArr, Bitmap bitmap) throws Throwable {
        OutputStream outputStreamOpenOutputStream;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            outputStreamOpenOutputStream = getContext().getContentResolver().openOutputStream(uri);
        } catch (Throwable th) {
            th = th;
            outputStreamOpenOutputStream = null;
        }
        try {
            if (!z) {
                outputStreamOpenOutputStream.write(bArr);
            } else if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStreamOpenOutputStream)) {
                throw new IOException("failed to compress bitmap");
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            IoUtils.closeQuietly(outputStreamOpenOutputStream);
        } catch (Throwable th2) {
            th = th2;
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            IoUtils.closeQuietly(outputStreamOpenOutputStream);
            throw th;
        }
    }

    private ParcelFileDescriptor getThumb(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, String str, long j, Uri uri) {
        ThumbData thumbData = new ThumbData();
        thumbData.helper = databaseHelper;
        thumbData.db = sQLiteDatabase;
        thumbData.path = str;
        thumbData.album_id = j;
        thumbData.albumart_uri = uri;
        return makeThumbInternal(thumbData);
    }

    private ParcelFileDescriptor makeThumbInternal(ThumbData thumbData) throws Exception {
        Bitmap bitmapDecodeByteArray;
        boolean z;
        ?? albumArtOutputUri;
        ParcelFileDescriptor parcelFileDescriptorOpenFileHelper;
        long jClearCallingIdentity;
        SQLiteDatabase sQLiteDatabase;
        String str;
        StringBuilder sb;
        ?? r6;
        int i;
        Bitmap bitmapCopy;
        byte[] compressedAlbumArt = getCompressedAlbumArt(getContext(), this.mExternalStoragePaths, thumbData.path);
        if (compressedAlbumArt == null) {
            Log.v(TAG, "makeThumbInternal<<<compressed=null!");
            return null;
        }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            options.inSampleSize = 1;
            z = false;
            BitmapFactory.decodeByteArray(compressedAlbumArt, 0, compressedAlbumArt.length, options);
            int dimensionPixelSize = getContext().getResources().getDimensionPixelSize(R.dimen.maximum_thumb_size);
            while (true) {
                if (options.outHeight <= dimensionPixelSize && (i = options.outWidth) <= dimensionPixelSize) {
                    break;
                }
                options.outHeight /= 2;
                options.outWidth /= 2;
                options.inSampleSize *= 2;
            }
            if (options.inSampleSize == 1) {
                bitmapDecodeByteArray = null;
                albumArtOutputUri = i;
            } else {
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmapDecodeByteArray = BitmapFactory.decodeByteArray(compressedAlbumArt, 0, compressedAlbumArt.length, options);
                if (bitmapDecodeByteArray != null) {
                    try {
                        if (bitmapDecodeByteArray.getConfig() != null || (bitmapCopy = bitmapDecodeByteArray.copy(Bitmap.Config.RGB_565, false)) == null || bitmapCopy == bitmapDecodeByteArray) {
                            z = true;
                            albumArtOutputUri = i;
                        } else {
                            bitmapDecodeByteArray.recycle();
                            z = true;
                            bitmapDecodeByteArray = bitmapCopy;
                            albumArtOutputUri = i;
                        }
                    } catch (Exception e) {
                        e = e;
                        Log.e(TAG, "makeThumbInternal: Exception!", e);
                        z = true;
                        albumArtOutputUri = "makeThumbInternal: Exception!";
                    }
                }
            }
        } catch (Exception e2) {
            e = e2;
            bitmapDecodeByteArray = null;
        }
        if (z && bitmapDecodeByteArray == null) {
            Log.e(TAG, "makeThumbInternal<<<need_to_recompress=true but bm=null!");
            return null;
        }
        if (thumbData.albumart_uri == null) {
            try {
                return ParcelFileDescriptor.fromData(compressedAlbumArt, "albumthumb");
            } catch (IOException e3) {
                Log.e(TAG, "makeThumbInternal: ParcelFileDescriptor.fromData IOException!", e3);
            }
        } else {
            thumbData.db.beginTransaction();
            try {
                try {
                    albumArtOutputUri = getAlbumArtOutputUri(thumbData.helper, thumbData.db, thumbData.album_id, thumbData.albumart_uri);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                } catch (IOException e4) {
                    e = e4;
                    parcelFileDescriptorOpenFileHelper = null;
                } catch (UnsupportedOperationException e5) {
                    e = e5;
                    parcelFileDescriptorOpenFileHelper = null;
                } catch (Throwable th2) {
                    th = th2;
                    compressedAlbumArt = null;
                    thumbData.db.endTransaction();
                    if (bitmapDecodeByteArray != null) {
                        bitmapDecodeByteArray.recycle();
                    }
                    if (compressedAlbumArt == null && albumArtOutputUri != 0) {
                        long jClearCallingIdentity2 = Binder.clearCallingIdentity();
                        thumbData.helper.mNumDeletes++;
                        thumbData.db.delete("album_art", "album_id=" + albumArtOutputUri.getPathSegments().get(3), null);
                        Binder.restoreCallingIdentity(jClearCallingIdentity2);
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e = e6;
                parcelFileDescriptorOpenFileHelper = null;
                albumArtOutputUri = 0;
            } catch (UnsupportedOperationException e7) {
                e = e7;
                parcelFileDescriptorOpenFileHelper = null;
                albumArtOutputUri = 0;
            } catch (Throwable th3) {
                th = th3;
                compressedAlbumArt = null;
                albumArtOutputUri = 0;
            }
            if (albumArtOutputUri != 0) {
                writeAlbumArt(z, albumArtOutputUri, compressedAlbumArt, bitmapDecodeByteArray);
                getContext().getContentResolver().notifyChange(MEDIA_URI, null);
                parcelFileDescriptorOpenFileHelper = openFileHelper(albumArtOutputUri, "r");
                try {
                    thumbData.db.setTransactionSuccessful();
                    thumbData.db.endTransaction();
                    if (bitmapDecodeByteArray != null) {
                        bitmapDecodeByteArray.recycle();
                    }
                    if (parcelFileDescriptorOpenFileHelper == null && albumArtOutputUri != 0) {
                        long jClearCallingIdentity3 = Binder.clearCallingIdentity();
                        thumbData.helper.mNumDeletes++;
                        thumbData.db.delete("album_art", "album_id=" + albumArtOutputUri.getPathSegments().get(3), null);
                        Binder.restoreCallingIdentity(jClearCallingIdentity3);
                    }
                    return parcelFileDescriptorOpenFileHelper;
                } catch (IOException e8) {
                    e = e8;
                    Log.e(TAG, "makeThumbInternal: IOException! albumId=" + thumbData.album_id, e);
                    thumbData.db.endTransaction();
                    if (bitmapDecodeByteArray != null) {
                        bitmapDecodeByteArray.recycle();
                    }
                    if (parcelFileDescriptorOpenFileHelper == null && albumArtOutputUri != 0) {
                        jClearCallingIdentity = Binder.clearCallingIdentity();
                        thumbData.helper.mNumDeletes++;
                        sQLiteDatabase = thumbData.db;
                        str = "album_art";
                        sb = new StringBuilder();
                        r6 = albumArtOutputUri;
                        sb.append("album_id=");
                        sb.append(r6.getPathSegments().get(3));
                        sQLiteDatabase.delete(str, sb.toString(), null);
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                    return null;
                } catch (UnsupportedOperationException e9) {
                    e = e9;
                    Log.e(TAG, "makeThumbInternal:UnsupportedOperationException! albumId=" + thumbData.album_id, e);
                    thumbData.db.endTransaction();
                    if (bitmapDecodeByteArray != null) {
                        bitmapDecodeByteArray.recycle();
                    }
                    if (parcelFileDescriptorOpenFileHelper == null && albumArtOutputUri != 0) {
                        jClearCallingIdentity = Binder.clearCallingIdentity();
                        thumbData.helper.mNumDeletes++;
                        sQLiteDatabase = thumbData.db;
                        str = "album_art";
                        sb = new StringBuilder();
                        r6 = albumArtOutputUri;
                        sb.append("album_id=");
                        sb.append(r6.getPathSegments().get(3));
                        sQLiteDatabase.delete(str, sb.toString(), null);
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                    return null;
                }
            }
            Log.w(TAG, "makeThumbInternal: Nulluri to save album thumb data! albumId=" + thumbData.album_id);
            thumbData.db.endTransaction();
            if (bitmapDecodeByteArray != null) {
                bitmapDecodeByteArray.recycle();
            }
            if (albumArtOutputUri != 0) {
                jClearCallingIdentity = Binder.clearCallingIdentity();
                thumbData.helper.mNumDeletes++;
                sQLiteDatabase = thumbData.db;
                str = "album_art";
                sb = new StringBuilder();
                r6 = albumArtOutputUri;
                sb.append("album_id=");
                sb.append(r6.getPathSegments().get(3));
                sQLiteDatabase.delete(str, sb.toString(), null);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        return null;
    }

    private long getKeyIdForName(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, String str, String str2, String str3, String str4, String str5, String str6, int i, String str7, ConcurrentHashMap<String, Long> concurrentHashMap, Uri uri) {
        String str8;
        int i2;
        ContentObserver contentObserver;
        String str9;
        long j;
        if (str4 == null || str4.length() == 0) {
            str8 = "<unknown>";
        } else {
            str8 = str4;
        }
        String strKeyFor = MediaStore.Audio.keyFor(str8);
        long jLongValue = -1;
        if (strKeyFor == null) {
            Log.e(TAG, "null key", new Exception());
            return -1L;
        }
        boolean zEquals = str.equals("albums");
        boolean zEquals2 = "<unknown>".equals(str8);
        if (zEquals) {
            strKeyFor = strKeyFor + i;
            if (zEquals2) {
                strKeyFor = strKeyFor + str7;
            }
        }
        String str10 = strKeyFor;
        databaseHelper.mNumQueries++;
        Cursor cursorQuery = sQLiteDatabase.query(str, null, str2 + "=?", new String[]{str10}, null, null, null);
        try {
            switch (cursorQuery.getCount()) {
                case 0:
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(str2, str10);
                    contentValues.put(str3, str8);
                    databaseHelper.mNumInserts++;
                    long jInsert = sQLiteDatabase.insert(str, "duration", contentValues);
                    if (str6 != null && zEquals && !zEquals2) {
                        i2 = 16;
                        contentObserver = null;
                        makeThumbAsync(databaseHelper, sQLiteDatabase, str6, jInsert);
                    } else {
                        i2 = 16;
                        contentObserver = null;
                    }
                    if (jInsert > 0) {
                        getContext().getContentResolver().notifyChange(Uri.parse("content://media/" + uri.toString().substring(i2, 24) + "/audio/" + str + "/" + jInsert), contentObserver);
                        str9 = str5;
                    } else {
                        str9 = str5;
                        Long l = concurrentHashMap.get(str9);
                        if (l != null) {
                            jLongValue = l.longValue();
                        }
                        j = jLongValue;
                    }
                    jLongValue = jInsert;
                    j = jLongValue;
                    break;
                case 1:
                    cursorQuery.moveToFirst();
                    j = cursorQuery.getLong(0);
                    String string = cursorQuery.getString(2);
                    String strMakeBestName = makeBestName(str8, string);
                    if (!strMakeBestName.equals(string)) {
                        ContentValues contentValues2 = new ContentValues();
                        contentValues2.put(str3, strMakeBestName);
                        databaseHelper.mNumUpdates++;
                        sQLiteDatabase.update(str, contentValues2, "rowid=" + Integer.toString((int) j), null);
                        getContext().getContentResolver().notifyChange(Uri.parse("content://media/" + uri.toString().substring(16, 24) + "/audio/" + str + "/" + j), null);
                    }
                    str9 = str5;
                    break;
                default:
                    str9 = str5;
                    Log.e(TAG, "Multiple entries in table " + str + " for key " + str10);
                    j = jLongValue;
                    break;
            }
            if (concurrentHashMap != null && !zEquals2 && j > 0) {
                concurrentHashMap.put(str9, Long.valueOf(j));
            }
            return j;
        } finally {
            IoUtils.closeQuietly(cursorQuery);
        }
    }

    String makeBestName(String str, String str2) {
        if (str.length() <= str2.length() && str.toLowerCase().compareTo(str2.toLowerCase()) <= 0) {
            str = str2;
        }
        if (str.endsWith(", the") || str.endsWith(",the") || str.endsWith(", an") || str.endsWith(",an") || str.endsWith(", a") || str.endsWith(",a")) {
            return str.substring(1 + str.lastIndexOf(44)).trim() + " " + str.substring(0, str.lastIndexOf(44));
        }
        return str;
    }

    private DatabaseHelper getDatabaseForUri(Uri uri) {
        synchronized (this.mDatabases) {
            if (uri.getPathSegments().size() >= 1) {
                return this.mDatabases.get(uri.getPathSegments().get(0));
            }
            return null;
        }
    }

    static boolean isMediaDatabaseName(String str) {
        if ("internal.db".equals(str) || "external.db".equals(str)) {
            return true;
        }
        return str.startsWith("external-") && str.endsWith(".db");
    }

    static boolean isInternalMediaDatabaseName(String str) {
        if ("internal.db".equals(str)) {
            return true;
        }
        return false;
    }

    private Uri attachVolume(String str) {
        DatabaseHelper databaseHelper;
        Log.v(TAG, "attachVolume>>> volume=" + str);
        if (Binder.getCallingPid() != Process.myPid()) {
            throw new SecurityException("Opening and closing databases not allowed.");
        }
        updateStoragePaths();
        synchronized (this.mDatabases) {
            DatabaseHelper databaseHelper2 = this.mDatabases.get(str);
            if (databaseHelper2 != null) {
                Log.v(TAG, "attachVolume<<< Already attached " + databaseHelper2.mName);
                if ("external".equals(str)) {
                    ensureDefaultFolders(databaseHelper2, databaseHelper2.getWritableDatabase());
                    this.mMediaProviderUtils.removeOverTimeDbItems();
                }
                return Uri.parse("content://media/" + str);
            }
            Context context = getContext();
            if (!"internal".equals(str)) {
                if (!"external".equals(str)) {
                    throw new IllegalArgumentException("There is no volume named " + str);
                }
                if (this.mStorageManager.getPrimaryPhysicalVolume() != null) {
                    String path = Environment.getExternalStorageDirectory().getPath();
                    String externalStoragePath = StorageManagerEx.getExternalStoragePath();
                    Log.v(TAG, "attachVolume: primaryPath=" + path + ", externalPath=" + externalStoragePath + ", isExternalStorageRemovable=" + (path != null && path.equals(externalStoragePath)));
                    StorageVolume primaryVolume = this.mStorageManager.getPrimaryVolume();
                    int fatVolumeId = primaryVolume.getFatVolumeId();
                    Log.v(TAG, path + " volume Id: " + fatVolumeId + ", " + primaryVolume);
                    if (fatVolumeId == -1) {
                        String externalStorageState = Environment.getExternalStorageState();
                        if (!"mounted".equals(externalStorageState) && !"mounted_ro".equals(externalStorageState)) {
                            Log.i(TAG, "External volume is not (yet) mounted, cannot attach.");
                            throw new IllegalArgumentException("Can't obtain external volume ID for " + str + " volume.");
                        }
                        Log.e(TAG, "Can't obtain external volume ID even though it's mounted.");
                        throw new IllegalArgumentException("Can't obtain external volume ID for " + str + " volume.");
                    }
                    String str2 = "external-" + Integer.toHexString(fatVolumeId) + ".db";
                    DatabaseHelper detachedDatabaseFromCache = getDetachedDatabaseFromCache(str2);
                    if (detachedDatabaseFromCache == null && !"external-ffffffff.db".equals(str2) && (detachedDatabaseFromCache = getDetachedDatabaseFromCache("external-ffffffff.db")) != null) {
                        Log.d(TAG, "close external-ffffffff.db and rename it to right db later");
                        detachedDatabaseFromCache.close();
                        detachedDatabaseFromCache = null;
                    }
                    if (detachedDatabaseFromCache == null) {
                        File databasePath = context.getDatabasePath("external-ffffffff.db");
                        File databasePath2 = context.getDatabasePath(str2);
                        if (fatVolumeId != -1 && !databasePath2.exists() && databasePath.exists()) {
                            Log.d(TAG, "renamed invalid database external-ffffffff.db to " + str2 + ", succuss = " + databasePath.renameTo(databasePath2));
                        }
                        detachedDatabaseFromCache = new DatabaseHelper(context, str2, false, false, this.mObjectRemovedCallback);
                    }
                    this.mVolumeId = fatVolumeId;
                    databaseHelper = detachedDatabaseFromCache;
                } else {
                    File databasePath3 = context.getDatabasePath("external.db");
                    if (!databasePath3.exists() && Build.VERSION.SDK_INT < 14) {
                        closeAllDeatchedDatabaseInCache();
                        File file = null;
                        for (String str3 : context.databaseList()) {
                            if (str3.startsWith("external-") && str3.endsWith(".db")) {
                                File databasePath4 = context.getDatabasePath(str3);
                                if (file != null) {
                                    if (databasePath4.lastModified() > file.lastModified()) {
                                        context.deleteDatabase(file.getName());
                                    } else {
                                        context.deleteDatabase(databasePath4.getName());
                                    }
                                }
                                file = databasePath4;
                            }
                        }
                        if (file != null) {
                            if (file.renameTo(databasePath3)) {
                                Log.d(TAG, "renamed database " + file.getName() + " to external.db");
                            } else {
                                Log.e(TAG, "Failed to rename database " + file.getName() + " to external.db");
                                databasePath3 = file;
                            }
                        }
                    }
                    DatabaseHelper detachedDatabaseFromCache2 = getDetachedDatabaseFromCache(databasePath3.getName());
                    databaseHelper = detachedDatabaseFromCache2 == null ? new DatabaseHelper(context, databasePath3.getName(), false, false, this.mObjectRemovedCallback) : detachedDatabaseFromCache2;
                }
            } else {
                databaseHelper = new DatabaseHelper(context, "internal.db", true, false, this.mObjectRemovedCallback);
            }
            this.mDatabases.put(str, databaseHelper);
            this.mDatabaseToBeClosed = false;
            if (!databaseHelper.mInternal) {
                File[] fileArrListFiles = new File(this.mExternalPath, "Android/data/com.android.providers.media/albumthumbs").listFiles();
                Log.v(TAG, "test album art path: " + new File(this.mExternalPath, "Android/data/com.android.providers.media/albumthumbs").getPath());
                HashSet hashSet = new HashSet();
                for (int i = 0; fileArrListFiles != null && i < fileArrListFiles.length; i++) {
                    hashSet.add(fileArrListFiles[i].getPath());
                }
                Cursor cursorQuery = query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{"album_art"}, null, null, null);
                while (cursorQuery != null) {
                    try {
                        if (!cursorQuery.moveToNext()) {
                            break;
                        }
                        hashSet.remove(cursorQuery.getString(0));
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(cursorQuery);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(cursorQuery);
                Iterator it = hashSet.iterator();
                while (it.hasNext()) {
                    new File((String) it.next()).delete();
                }
            }
            Log.v(TAG, "attachVolume<<< database=" + this.mDatabases.get(str).mName);
            if ("external".equals(str)) {
                ensureDefaultFolders(databaseHelper, databaseHelper.getWritableDatabase());
                this.mMediaProviderUtils.removeOverTimeDbItems();
            }
            return Uri.parse("content://media/" + str);
        }
    }

    private void detachVolume(Uri uri) {
        if (Binder.getCallingPid() != Process.myPid()) {
            throw new SecurityException("Opening and closing databases not allowed.");
        }
        updateStoragePaths();
        String str = uri.getPathSegments().get(0);
        if ("internal".equals(str)) {
            throw new UnsupportedOperationException("Deleting the internal volume is not allowed");
        }
        if (!"external".equals(str)) {
            throw new IllegalArgumentException("There is no volume named " + str);
        }
        synchronized (this.mDatabases) {
            DatabaseHelper databaseHelper = this.mDatabases.get(str);
            if (databaseHelper == null) {
                return;
            }
            try {
                new File(databaseHelper.getReadableDatabase().getPath()).setLastModified(System.currentTimeMillis());
                databaseHelper.mLastModified = System.currentTimeMillis();
            } catch (Exception e) {
                Log.e(TAG, "Can't touch database file", e);
            }
            this.mDatabases.remove(str);
            this.mDatabaseToBeClosed = true;
            addDetachedDatabaseToCache(databaseHelper);
            Log.v(TAG, "Detached volume: " + databaseHelper.mName);
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    private void addDetachedDatabaseToCache(DatabaseHelper databaseHelper) {
        String key = "detached_" + databaseHelper.mName;
        this.mDatabases.put(key, databaseHelper);
        if (this.mDatabases.size() >= 4) {
            long j = databaseHelper.mLastModified;
            for (Map.Entry<String, DatabaseHelper> entry : this.mDatabases.entrySet()) {
                if (entry.getKey().startsWith("detached_") && entry.getValue().mLastModified < j) {
                    key = entry.getKey();
                    j = entry.getValue().mLastModified;
                }
            }
            this.mDatabases.remove(key).close();
            Log.d(TAG, "addDetachedDatabaseToCache: remove lru database " + key);
        }
        Log.d(TAG, "addDetachedDatabaseToCache: " + this.mDatabases);
    }

    private DatabaseHelper getDetachedDatabaseFromCache(String str) {
        Log.d(TAG, "getDetachedDatabaseFromCache: databaseName = " + str + ", mDatabases = " + this.mDatabases);
        return this.mDatabases.remove("detached_" + str);
    }

    private void closeAllDeatchedDatabaseInCache() {
        Log.d(TAG, "closeAllDeatchedDatabaseInCache: " + this.mDatabases);
        Iterator<Map.Entry<String, DatabaseHelper>> it = this.mDatabases.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, DatabaseHelper> next = it.next();
            if (next.getKey().startsWith("detached_")) {
                next.getValue().close();
                it.remove();
            }
        }
    }

    private static String getVolumeName(Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments != null && pathSegments.size() > 0) {
            return pathSegments.get(0);
        }
        return null;
    }

    private String getCallingPackageOrSelf() {
        String callingPackage = getCallingPackage();
        if (callingPackage == null) {
            return getContext().getOpPackageName();
        }
        return callingPackage;
    }

    private void enforceCallingOrSelfPermissionAndAppOps(String str, String str2) {
        getContext().enforceCallingOrSelfPermission(str, str2);
        String strPermissionToOp = AppOpsManager.permissionToOp(str);
        if (strPermissionToOp != null) {
            String callingPackageOrSelf = getCallingPackageOrSelf();
            if (this.mAppOpsManager.noteProxyOp(strPermissionToOp, callingPackageOrSelf) != 0) {
                throw new SecurityException(str2 + ": " + callingPackageOrSelf + " is not allowed to " + str);
            }
        }
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        Iterator<DatabaseHelper> it = this.mDatabases.values().iterator();
        while (it.hasNext()) {
            printWriter.println(dump(it.next(), true));
        }
        printWriter.flush();
    }

    private String dump(DatabaseHelper databaseHelper, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append(databaseHelper.mName);
        sb.append(": ");
        SQLiteDatabase readableDatabase = databaseHelper.getReadableDatabase();
        if (readableDatabase == null) {
            sb.append("null");
        } else {
            sb.append("version " + readableDatabase.getVersion() + ", ");
            ?? Query = readableDatabase.query("files", new String[]{"count(*)"}, null, null, null, null, null);
            if (Query != 0) {
                try {
                    if (Query.moveToFirst()) {
                        sb.append(Query.getInt(0) + " rows, ");
                    } else {
                        sb.append("couldn't get row count, ");
                    }
                    IoUtils.closeQuietly((AutoCloseable) Query);
                    sb.append(databaseHelper.mNumInserts + " inserts, ");
                    sb.append(databaseHelper.mNumUpdates + " updates, ");
                    sb.append(databaseHelper.mNumDeletes + " deletes, ");
                    sb.append(databaseHelper.mNumQueries + " queries, ");
                    Query = (databaseHelper.mScanStartTime > 0L ? 1 : (databaseHelper.mScanStartTime == 0L ? 0 : -1));
                    if (Query != 0) {
                        sb.append("scan started " + DateUtils.formatDateTime(getContext(), databaseHelper.mScanStartTime / 1000, 524305));
                        long jCurrentTimeMicro = databaseHelper.mScanStopTime;
                        if (jCurrentTimeMicro < databaseHelper.mScanStartTime) {
                            jCurrentTimeMicro = SystemClock.currentTimeMicro();
                        }
                        sb.append(" (" + DateUtils.formatElapsedTime((jCurrentTimeMicro - databaseHelper.mScanStartTime) / 1000000) + ")");
                        if (databaseHelper.mScanStopTime < databaseHelper.mScanStartTime) {
                            if (this.mMediaScannerVolume != null && databaseHelper.mName.startsWith(this.mMediaScannerVolume)) {
                                sb.append(" (ongoing)");
                            } else {
                                sb.append(" (scanning " + this.mMediaScannerVolume + ")");
                            }
                        }
                    }
                    if (z) {
                        Cursor cursorQuery = readableDatabase.query("log", new String[]{"time", "message"}, null, null, null, null, "rowid");
                        if (cursorQuery != null) {
                            while (cursorQuery.moveToNext()) {
                                try {
                                    sb.append("\n" + cursorQuery.getString(0) + " : " + cursorQuery.getString(1));
                                } finally {
                                    IoUtils.closeQuietly(cursorQuery);
                                }
                            }
                        }
                    }
                } catch (Throwable th) {
                    IoUtils.closeQuietly((AutoCloseable) Query);
                    throw th;
                }
            }
        }
        return sb.toString();
    }

    private int deleteMtpTransferFile() {
        Log.v(TAG, "deleteMtpTransferFile: path=" + this.mMtpTransferFile);
        if (this.mMtpTransferFile != null) {
            this.mMtpTransferFile = null;
            getContext().getContentResolver().notifyChange(MtkMediaStore.getMtpTransferFileUri(), null);
            return 1;
        }
        return 0;
    }

    private static void checkColumnsExist(SQLiteDatabase sQLiteDatabase, String[] strArr, ArrayList<String> arrayList) {
        try {
            try {
                IoUtils.closeQuietly(sQLiteDatabase.query("files", strArr, null, null, null, null, null));
            } catch (SQLiteException e) {
                Iterator<String> it = arrayList.iterator();
                while (it.hasNext()) {
                    sQLiteDatabase.execSQL("ALTER TABLE files ADD COLUMN " + it.next());
                }
                MtkLog.d(TAG, "checkColumnsExist: " + arrayList + " not exist in old table files, added them to it. " + e);
                IoUtils.closeQuietly((AutoCloseable) null);
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    public final class MediaProviderUtils {
        private final Context mContext;
        private final int mMaxSdCount;
        private final SharedPreferences mSharedPref;
        private final Object mAccessLock = new Object();
        private HashMap<String, String> mSdInfoMap = new HashMap<>();
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message != null) {
                    int i = message.what;
                }
                super.handleMessage(message);
            }
        };

        public MediaProviderUtils(Context context) {
            int iIndexOf;
            int i;
            this.mContext = context;
            this.mSharedPref = this.mContext.getSharedPreferences("sd_info", 0);
            if (((ActivityManager) this.mContext.getSystemService("activity")).isLowRamDevice()) {
                this.mMaxSdCount = 1;
            } else {
                this.mMaxSdCount = Integer.MAX_VALUE;
            }
            synchronized (this.mAccessLock) {
                this.mSdInfoMap.clear();
                Set<String> stringSet = this.mSharedPref.getStringSet("info_set", null);
                if (stringSet != null) {
                    for (String str : stringSet) {
                        if (str != null && (iIndexOf = str.indexOf(",")) != -1 && str.length() > (i = iIndexOf + 1)) {
                            String strSubstring = str.substring(0, iIndexOf);
                            String strSubstring2 = str.substring(i);
                            Log.e("MediaProviderUtils", "MediaProviderUtils()-uuid=" + strSubstring + ",time=" + strSubstring2);
                            put(strSubstring, strSubstring2);
                        }
                    }
                }
            }
        }

        private void writeSharedPreferences(HashMap<String, String> map) {
            if (map == null || map.isEmpty()) {
                Log.e("MediaProviderUtils", "writeSharedPreferences()-infoMap is null or empty");
                return;
            }
            HashSet hashSet = new HashSet();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                    hashSet.add(key + "," + value);
                }
            }
            SharedPreferences.Editor editorEdit = this.mSharedPref.edit();
            editorEdit.clear();
            if (!hashSet.isEmpty()) {
                editorEdit.putStringSet("info_set", hashSet);
            }
            editorEdit.apply();
        }

        private void removeInvalidDbItems(ArrayList<String> arrayList) {
            if (arrayList == null || arrayList.isEmpty()) {
                Log.e("MediaProviderUtils", "removeInvalidDbItems()-clearList is null or empty");
                return;
            }
            this.mHandler.removeMessages(1);
            Message messageObtain = Message.obtain();
            messageObtain.what = 1;
            messageObtain.getData().putStringArrayList("uuid_list", arrayList);
            this.mHandler.sendMessageAtTime(messageObtain, 10000L);
        }

        public void put(String str, String str2) {
            if (str == null || str2 == null) {
                Log.d("MediaProviderUtils", "put() storageId or time is null");
                return;
            }
            Log.d("MediaProviderUtils", "put() storageId = " + str + " , time=" + str2);
            synchronized (this.mAccessLock) {
                this.mSdInfoMap.put(str, str2);
                if (this.mSdInfoMap.size() > this.mMaxSdCount) {
                    ArrayList<String> arrayList = new ArrayList<>();
                    long j = Long.MAX_VALUE;
                    String key = "";
                    for (Map.Entry<String, String> entry : this.mSdInfoMap.entrySet()) {
                        long jLongValue = Long.valueOf(entry.getValue()).longValue();
                        if (jLongValue < j) {
                            key = entry.getKey();
                            j = jLongValue;
                        }
                    }
                    Log.d("MediaProviderUtils", "put()-removalItemId=" + key);
                    arrayList.add(key);
                    this.mSdInfoMap.remove(key);
                    removeInvalidDbItems(arrayList);
                }
                writeSharedPreferences(this.mSdInfoMap);
            }
        }

        public void removeOverTimeDbItems() {
        }
    }
}
