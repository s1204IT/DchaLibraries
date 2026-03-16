package com.android.providers.media;

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
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaFile;
import android.media.MediaScanner;
import android.media.MediaScannerConnection;
import android.media.MiniThumbFile;
import android.mtp.MtpStorage;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
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
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Stack;
import libcore.io.IoUtils;

public class MediaProvider extends ContentProvider {
    private static final String[] GENRE_LOOKUP_PROJECTION;
    private static final String[] ID_PROJECTION;
    private static final String[] MIME_TYPE_PROJECTION;
    private static final String[] PATH_PROJECTION;
    private static final String[] READY_FLAG_PROJECTION;
    private static String TAG;
    private static final UriMatcher URI_MATCHER;
    private static String[] mExternalStoragePaths;
    private static final String[] openFileColumns;
    private static final String sCachePath;
    private static final String[] sDataOnlyColumn;
    private static final String[] sDefaultFolderNames;
    private static final String sExternalPath;
    static final GetTableAndWhereOutParameter sGetTableAndWhereParam;
    private static final String[] sIdOnlyColumn;
    private static final String sLegacyPath;
    private static final String[] sMediaTableColumns;
    private static final String[] sMediaTypeDataId;
    private static final String[] sPlaylistIdPlayOrder;
    private HashMap<String, DatabaseHelper> mDatabases;
    private boolean mDisableMtpObjectCallbacks;
    private String mMediaScannerVolume;
    private IMtpService mMtpService;
    private StorageManager mStorageManager;
    private Handler mThumbHandler;
    private static final Uri MEDIA_URI = Uri.parse("content://media");
    private static final Uri ALBUMART_URI = Uri.parse("content://media/external/audio/albumart");
    private static final HashMap<String, String> sArtistAlbumsMap = new HashMap<>();
    private static final HashMap<String, String> sFolderArtMap = new HashMap<>();
    HashMap<String, Long> mDirectoryCache = new HashMap<>();
    private HashSet mPendingThumbs = new HashSet();
    private Stack mThumbRequestStack = new Stack();
    private MediaThumbRequest mCurrentThumbRequest = null;
    private PriorityQueue<MediaThumbRequest> mMediaThumbQueue = new PriorityQueue<>(10, MediaThumbRequest.getComparator());
    private String[] mSearchColsLegacy = {"_id", "mime_type", "(CASE WHEN grouporder=1 THEN 2130837505 ELSE CASE WHEN grouporder=2 THEN 2130837504 ELSE 2130837506 END END) AS suggest_icon_1", "0 AS suggest_icon_2", "text1 AS suggest_text_1", "text1 AS suggest_intent_query", "CASE when grouporder=1 THEN data1 ELSE artist END AS data1", "CASE when grouporder=1 THEN data2 ELSE CASE WHEN grouporder=2 THEN NULL ELSE album END END AS data2", "match as ar", "suggest_intent_data", "grouporder", "NULL AS itemorder"};
    private String[] mSearchColsFancy = {"_id", "mime_type", "artist", "album", "title", "data1", "data2"};
    private String[] mSearchColsBasic = {"_id", "mime_type", "(CASE WHEN grouporder=1 THEN 2130837505 ELSE CASE WHEN grouporder=2 THEN 2130837504 ELSE 2130837506 END END) AS suggest_icon_1", "text1 AS suggest_text_1", "text1 AS suggest_intent_query", "(CASE WHEN grouporder=1 THEN '%1' ELSE CASE WHEN grouporder=3 THEN artist || ' - ' || album ELSE CASE WHEN text2!='<unknown>' THEN text2 ELSE NULL END END END) AS suggest_text_2", "suggest_intent_data"};
    private final int SEARCH_COLUMN_BASIC_TEXT2 = 5;
    private Uri mAlbumArtBaseUri = Uri.parse("content://media/external/audio/albumart");
    private BroadcastReceiver mUnmountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DatabaseHelper database;
            if ("android.intent.action.MEDIA_EJECT".equals(intent.getAction())) {
                StorageVolume storage = (StorageVolume) intent.getParcelableExtra("storage_volume");
                if (storage.getPath().equals(MediaProvider.mExternalStoragePaths[0])) {
                    MediaProvider.this.detachVolume(Uri.parse("content://media/external"));
                    MediaProvider.sFolderArtMap.clear();
                    MiniThumbFile.reset();
                    return;
                }
                synchronized (MediaProvider.this.mDatabases) {
                    database = (DatabaseHelper) MediaProvider.this.mDatabases.get("external");
                }
                Uri uri = Uri.parse("file://" + storage.getPath());
                if (database != null) {
                    try {
                        context.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_STARTED", uri));
                        MediaProvider.this.mDisableMtpObjectCallbacks = true;
                        Log.d(MediaProvider.TAG, "deleting all entries for storage " + storage);
                        SQLiteDatabase db = database.getWritableDatabase();
                        ContentValues values = new ContentValues();
                        values.putNull("_data");
                        String[] whereArgs = {Integer.toString(storage.getStorageId())};
                        database.mNumUpdates++;
                        db.update("files", values, "storage_id=?", whereArgs);
                        database.mNumDeletes++;
                        int numpurged = db.delete("files", "storage_id=?", whereArgs);
                        MediaProvider.logToDb(db, "removed " + numpurged + " rows for ejected filesystem " + storage.getPath());
                        context.getContentResolver().notifyChange(MediaStore.Audio.Media.getContentUri("external"), null);
                        context.getContentResolver().notifyChange(MediaStore.Images.Media.getContentUri("external"), null);
                        context.getContentResolver().notifyChange(MediaStore.Video.Media.getContentUri("external"), null);
                        context.getContentResolver().notifyChange(MediaStore.Files.getContentUri("external"), null);
                    } catch (Exception e) {
                        Log.e(MediaProvider.TAG, "exception deleting storage entries", e);
                    } finally {
                        context.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_FINISHED", uri));
                        MediaProvider.this.mDisableMtpObjectCallbacks = false;
                    }
                }
            }
        }
    };
    private final SQLiteDatabase.CustomFunction mObjectRemovedCallback = new SQLiteDatabase.CustomFunction() {
        public void callback(String[] args) {
            MediaProvider.this.mDirectoryCache.clear();
            if (!MediaProvider.this.mDisableMtpObjectCallbacks) {
                Log.d(MediaProvider.TAG, "object removed " + args[0]);
                IMtpService mtpService = MediaProvider.this.mMtpService;
                if (mtpService != null) {
                    try {
                        MediaProvider.this.sendObjectRemoved(Integer.parseInt(args[0]));
                    } catch (NumberFormatException e) {
                        Log.e(MediaProvider.TAG, "NumberFormatException in mObjectRemovedCallback", e);
                    }
                }
            }
        }
    };
    private final ServiceConnection mMtpServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            synchronized (this) {
                MediaProvider.this.mMtpService = IMtpService.Stub.asInterface(service);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            synchronized (this) {
                MediaProvider.this.mMtpService = null;
            }
        }
    };
    private int mVolumeId = -1;

    static {
        try {
            sExternalPath = Environment.getExternalStorageDirectory().getCanonicalPath() + File.separator;
            sCachePath = Environment.getDownloadCacheDirectory().getCanonicalPath() + File.separator;
            sLegacyPath = Environment.getLegacyExternalStorageDirectory().getCanonicalPath() + File.separator;
            sMediaTableColumns = new String[]{"_id", "media_type"};
            sIdOnlyColumn = new String[]{"_id"};
            sDataOnlyColumn = new String[]{"_data"};
            sMediaTypeDataId = new String[]{"media_type", "_data", "_id"};
            sPlaylistIdPlayOrder = new String[]{"playlist_id", "play_order"};
            sDefaultFolderNames = new String[]{Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_PODCASTS, Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS, Environment.DIRECTORY_PICTURES, Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_DOWNLOADS, Environment.DIRECTORY_DCIM};
            GENRE_LOOKUP_PROJECTION = new String[]{"_id", "name"};
            sGetTableAndWhereParam = new GetTableAndWhereOutParameter();
            openFileColumns = new String[]{"_data"};
            TAG = "MediaProvider";
            URI_MATCHER = new UriMatcher(-1);
            ID_PROJECTION = new String[]{"_id"};
            PATH_PROJECTION = new String[]{"_id", "_data"};
            MIME_TYPE_PROJECTION = new String[]{"_id", "mime_type"};
            READY_FLAG_PROJECTION = new String[]{"_id", "_data", "mini_thumb_magic"};
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
            URI_MATCHER.addURI("media", "*/audio/search_suggest_query", 400);
            URI_MATCHER.addURI("media", "*/audio/search_suggest_query/*", 400);
            URI_MATCHER.addURI("media", "*/audio/search/search_suggest_query", 401);
            URI_MATCHER.addURI("media", "*/audio/search/search_suggest_query/*", 401);
            URI_MATCHER.addURI("media", "*/audio/search/fancy", 402);
            URI_MATCHER.addURI("media", "*/audio/search/fancy/*", 402);
        } catch (IOException e) {
            throw new RuntimeException("Unable to resolve canonical paths", e);
        }
    }

    static final class DatabaseHelper extends SQLiteOpenHelper {
        HashMap<String, Long> mAlbumCache;
        HashMap<String, Long> mArtistCache;
        final Context mContext;
        final boolean mEarlyUpgrade;
        final boolean mInternal;
        final String mName;
        int mNumDeletes;
        int mNumInserts;
        int mNumQueries;
        int mNumUpdates;
        final SQLiteDatabase.CustomFunction mObjectRemovedCallback;
        long mScanStartTime;
        long mScanStopTime;
        boolean mUpgradeAttempted;

        public DatabaseHelper(Context context, String name, boolean internal, boolean earlyUpgrade, SQLiteDatabase.CustomFunction objectRemovedCallback) {
            super(context, name, (SQLiteDatabase.CursorFactory) null, MediaProvider.getDatabaseVersion(context));
            this.mArtistCache = new HashMap<>();
            this.mAlbumCache = new HashMap<>();
            this.mContext = context;
            this.mName = name;
            this.mInternal = internal;
            this.mEarlyUpgrade = earlyUpgrade;
            this.mObjectRemovedCallback = objectRemovedCallback;
            setWriteAheadLoggingEnabled(true);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            MediaProvider.updateDatabase(this.mContext, db, this.mInternal, 0, MediaProvider.getDatabaseVersion(this.mContext));
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            this.mUpgradeAttempted = true;
            MediaProvider.updateDatabase(this.mContext, db, this.mInternal, oldV, newV);
        }

        @Override
        public synchronized SQLiteDatabase getWritableDatabase() {
            SQLiteDatabase sQLiteDatabase;
            SQLiteDatabase result = null;
            this.mUpgradeAttempted = false;
            try {
                result = super.getWritableDatabase();
            } catch (Exception e) {
                if (!this.mUpgradeAttempted) {
                    Log.e(MediaProvider.TAG, "failed to open database " + this.mName, e);
                    sQLiteDatabase = null;
                }
                return sQLiteDatabase;
            }
            if (result == null && this.mUpgradeAttempted) {
                this.mContext.deleteDatabase(this.mName);
                result = super.getWritableDatabase();
            }
            sQLiteDatabase = result;
            return sQLiteDatabase;
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (!this.mInternal && !this.mEarlyUpgrade) {
                if (this.mObjectRemovedCallback != null) {
                    db.addCustomFunction("_OBJECT_REMOVED", 1, this.mObjectRemovedCallback);
                }
                if (Environment.isExternalStorageRemovable()) {
                    File file = new File(db.getPath());
                    long now = System.currentTimeMillis();
                    file.setLastModified(now);
                    String[] databases = this.mContext.databaseList();
                    List<String> dbList = new ArrayList<>();
                    for (String database : databases) {
                        if (database != null && database.endsWith(".db")) {
                            dbList.add(database);
                        }
                    }
                    String[] databases2 = (String[]) dbList.toArray(new String[0]);
                    int count = databases2.length;
                    int limit = 3;
                    long twoMonthsAgo = now - 5184000000L;
                    for (int i = 0; i < databases2.length; i++) {
                        File other = this.mContext.getDatabasePath(databases2[i]);
                        if ("internal.db".equals(databases2[i]) || file.equals(other)) {
                            databases2[i] = null;
                            count--;
                            if (file.equals(other)) {
                                limit--;
                            }
                        } else if (other.lastModified() < twoMonthsAgo) {
                            this.mContext.deleteDatabase(databases2[i]);
                            databases2[i] = null;
                            count--;
                        }
                    }
                    while (count > limit) {
                        int lruIndex = -1;
                        long lruTime = 0;
                        for (int i2 = 0; i2 < databases2.length; i2++) {
                            if (databases2[i2] != null) {
                                long time = this.mContext.getDatabasePath(databases2[i2]).lastModified();
                                if (lruTime == 0 || time < lruTime) {
                                    lruIndex = i2;
                                    lruTime = time;
                                }
                            }
                        }
                        if (lruIndex != -1) {
                            this.mContext.deleteDatabase(databases2[lruIndex]);
                            databases2[lruIndex] = null;
                            count--;
                        }
                    }
                }
            }
        }
    }

    private void createDefaultFolders(DatabaseHelper helper, SQLiteDatabase db) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (prefs.getInt("created_default_folders", 0) == 0) {
            String[] arr$ = sDefaultFolderNames;
            for (String folderName : arr$) {
                File file = Environment.getExternalStoragePublicDirectory(folderName);
                if (!file.exists()) {
                    file.mkdirs();
                    insertDirectory(helper, db, file.getAbsolutePath());
                }
            }
            SharedPreferences.Editor e = prefs.edit();
            e.clear();
            e.putInt("created_default_folders", 1);
            e.commit();
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
        Context context = getContext();
        this.mStorageManager = (StorageManager) context.getSystemService("storage");
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
        IntentFilter iFilter = new IntentFilter("android.intent.action.MEDIA_EJECT");
        iFilter.addDataScheme("file");
        context.registerReceiver(this.mUnmountReceiver, iFilter);
        StorageManager storageManager = (StorageManager) context.getSystemService("storage");
        mExternalStoragePaths = storageManager.getVolumePaths();
        String state = Environment.getExternalStorageState();
        if ("mounted".equals(state) || "mounted_ro".equals(state)) {
            attachVolume("external");
        }
        HandlerThread ht = new HandlerThread("thumbs thread", 10);
        ht.start();
        this.mThumbHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                ThumbData d;
                if (msg.what != 2) {
                    if (msg.what == 1) {
                        synchronized (MediaProvider.this.mThumbRequestStack) {
                            d = (ThumbData) MediaProvider.this.mThumbRequestStack.pop();
                        }
                        IoUtils.closeQuietly(MediaProvider.this.makeThumbInternal(d));
                        synchronized (MediaProvider.this.mPendingThumbs) {
                            MediaProvider.this.mPendingThumbs.remove(d.path);
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
                        Log.w(MediaProvider.TAG, "Have message but no request?");
                        return;
                    }
                    try {
                        if (MediaProvider.this.mCurrentThumbRequest.mPath != null) {
                            File origFile = new File(MediaProvider.this.mCurrentThumbRequest.mPath);
                            if (!origFile.exists() || origFile.length() <= 0) {
                                synchronized (MediaProvider.this.mMediaThumbQueue) {
                                    Log.w(MediaProvider.TAG, "original file hasn't been stored yet: " + MediaProvider.this.mCurrentThumbRequest.mPath);
                                }
                            } else {
                                MediaProvider.this.mCurrentThumbRequest.execute();
                                synchronized (MediaProvider.this.mMediaThumbQueue) {
                                    for (MediaThumbRequest mtq : MediaProvider.this.mMediaThumbQueue) {
                                        if (mtq.mOrigId == MediaProvider.this.mCurrentThumbRequest.mOrigId && mtq.mIsVideo == MediaProvider.this.mCurrentThumbRequest.mIsVideo && mtq.mMagic == 0 && mtq.mState == MediaThumbRequest.State.WAIT) {
                                            mtq.mMagic = MediaProvider.this.mCurrentThumbRequest.mMagic;
                                        }
                                    }
                                }
                            }
                        }
                        synchronized (MediaProvider.this.mCurrentThumbRequest) {
                            MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                            MediaProvider.this.mCurrentThumbRequest.notifyAll();
                        }
                    } catch (IOException ex) {
                        Log.w(MediaProvider.TAG, ex);
                        synchronized (MediaProvider.this.mCurrentThumbRequest) {
                            MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                            MediaProvider.this.mCurrentThumbRequest.notifyAll();
                        }
                    } catch (OutOfMemoryError err) {
                        Log.w(MediaProvider.TAG, err);
                        synchronized (MediaProvider.this.mCurrentThumbRequest) {
                            MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                            MediaProvider.this.mCurrentThumbRequest.notifyAll();
                        }
                    } catch (UnsupportedOperationException ex2) {
                        Log.w(MediaProvider.TAG, ex2);
                        synchronized (MediaProvider.this.mCurrentThumbRequest) {
                            MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                            MediaProvider.this.mCurrentThumbRequest.notifyAll();
                        }
                    }
                } catch (Throwable th) {
                    synchronized (MediaProvider.this.mCurrentThumbRequest) {
                        MediaProvider.this.mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                        MediaProvider.this.mCurrentThumbRequest.notifyAll();
                        throw th;
                    }
                }
            }
        };
        return true;
    }

    private static void updateDatabase(Context context, SQLiteDatabase db, boolean internal, int fromVersion, int toVersion) {
        int dbversion = getDatabaseVersion(context);
        if (toVersion != dbversion) {
            Log.e(TAG, "Illegal update request. Got " + toVersion + ", expected " + dbversion);
            throw new IllegalArgumentException();
        }
        if (fromVersion > toVersion) {
            Log.e(TAG, "Illegal update request: can't downgrade from " + fromVersion + " to " + toVersion + ". Did you forget to wipe data?");
            throw new IllegalArgumentException();
        }
        long startTime = SystemClock.currentTimeMicro();
        if (fromVersion < 63 || ((fromVersion >= 84 && fromVersion <= 89) || (fromVersion >= 92 && fromVersion <= 94))) {
            Log.i(TAG, "Upgrading media database from version " + fromVersion + " to " + toVersion + ", which will destroy all old data");
            fromVersion = 63;
            db.execSQL("DROP TABLE IF EXISTS images");
            db.execSQL("DROP TRIGGER IF EXISTS images_cleanup");
            db.execSQL("DROP TABLE IF EXISTS thumbnails");
            db.execSQL("DROP TRIGGER IF EXISTS thumbnails_cleanup");
            db.execSQL("DROP TABLE IF EXISTS audio_meta");
            db.execSQL("DROP TABLE IF EXISTS artists");
            db.execSQL("DROP TABLE IF EXISTS albums");
            db.execSQL("DROP TABLE IF EXISTS album_art");
            db.execSQL("DROP VIEW IF EXISTS artist_info");
            db.execSQL("DROP VIEW IF EXISTS album_info");
            db.execSQL("DROP VIEW IF EXISTS artists_albums_map");
            db.execSQL("DROP TRIGGER IF EXISTS audio_meta_cleanup");
            db.execSQL("DROP TABLE IF EXISTS audio_genres");
            db.execSQL("DROP TABLE IF EXISTS audio_genres_map");
            db.execSQL("DROP TRIGGER IF EXISTS audio_genres_cleanup");
            db.execSQL("DROP TABLE IF EXISTS audio_playlists");
            db.execSQL("DROP TABLE IF EXISTS audio_playlists_map");
            db.execSQL("DROP TRIGGER IF EXISTS audio_playlists_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS albumart_cleanup1");
            db.execSQL("DROP TRIGGER IF EXISTS albumart_cleanup2");
            db.execSQL("DROP TABLE IF EXISTS video");
            db.execSQL("DROP TRIGGER IF EXISTS video_cleanup");
            db.execSQL("DROP TABLE IF EXISTS objects");
            db.execSQL("DROP TRIGGER IF EXISTS images_objects_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS audio_objects_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS video_objects_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS playlists_objects_cleanup");
            db.execSQL("CREATE TABLE IF NOT EXISTS images (_id INTEGER PRIMARY KEY,_data TEXT,_size INTEGER,_display_name TEXT,mime_type TEXT,title TEXT,date_added INTEGER,date_modified INTEGER,description TEXT,picasa_id TEXT,isprivate INTEGER,latitude DOUBLE,longitude DOUBLE,datetaken INTEGER,orientation INTEGER,mini_thumb_magic INTEGER,bucket_id TEXT,bucket_display_name TEXT);");
            db.execSQL("CREATE INDEX IF NOT EXISTS mini_thumb_magic_index on images(mini_thumb_magic);");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS images_cleanup DELETE ON images BEGIN DELETE FROM thumbnails WHERE image_id = old._id;SELECT _DELETE_FILE(old._data);END");
            db.execSQL("CREATE TABLE IF NOT EXISTS thumbnails (_id INTEGER PRIMARY KEY,_data TEXT,image_id INTEGER,kind INTEGER,width INTEGER,height INTEGER);");
            db.execSQL("CREATE INDEX IF NOT EXISTS image_id_index on thumbnails(image_id);");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS thumbnails_cleanup DELETE ON thumbnails BEGIN SELECT _DELETE_FILE(old._data);END");
            db.execSQL("CREATE TABLE IF NOT EXISTS audio_meta (_id INTEGER PRIMARY KEY,_data TEXT UNIQUE NOT NULL,_display_name TEXT,_size INTEGER,mime_type TEXT,date_added INTEGER,date_modified INTEGER,title TEXT NOT NULL,title_key TEXT NOT NULL,duration INTEGER,artist_id INTEGER,composer TEXT,album_id INTEGER,track INTEGER,year INTEGER CHECK(year!=0),is_ringtone INTEGER,is_music INTEGER,is_alarm INTEGER,is_notification INTEGER);");
            db.execSQL("CREATE TABLE IF NOT EXISTS artists (artist_id INTEGER PRIMARY KEY,artist_key TEXT NOT NULL UNIQUE,artist TEXT NOT NULL);");
            db.execSQL("CREATE TABLE IF NOT EXISTS albums (album_id INTEGER PRIMARY KEY,album_key TEXT NOT NULL UNIQUE,album TEXT NOT NULL);");
            db.execSQL("CREATE TABLE IF NOT EXISTS album_art (album_id INTEGER PRIMARY KEY,_data TEXT);");
            recreateAudioView(db);
            db.execSQL("CREATE VIEW IF NOT EXISTS artist_info AS SELECT artist_id AS _id, artist, artist_key, COUNT(DISTINCT album) AS number_of_albums, COUNT(*) AS number_of_tracks FROM audio WHERE is_music=1 GROUP BY artist_key;");
            db.execSQL("CREATE VIEW IF NOT EXISTS album_info AS SELECT audio.album_id AS _id, album, album_key, MIN(year) AS minyear, MAX(year) AS maxyear, artist, artist_id, artist_key, count(*) AS numsongs,album_art._data AS album_art FROM audio LEFT OUTER JOIN album_art ON audio.album_id=album_art.album_id WHERE is_music=1 GROUP BY audio.album_id;");
            db.execSQL("CREATE VIEW IF NOT EXISTS artists_albums_map AS SELECT DISTINCT artist_id, album_id FROM audio_meta;");
            if (!internal) {
                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_meta_cleanup DELETE ON audio_meta BEGIN DELETE FROM audio_genres_map WHERE audio_id = old._id;DELETE FROM audio_playlists_map WHERE audio_id = old._id;END");
                db.execSQL("CREATE TABLE IF NOT EXISTS audio_genres (_id INTEGER PRIMARY KEY,name TEXT NOT NULL);");
                db.execSQL("CREATE TABLE IF NOT EXISTS audio_genres_map (_id INTEGER PRIMARY KEY,audio_id INTEGER NOT NULL,genre_id INTEGER NOT NULL);");
                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_genres_cleanup DELETE ON audio_genres BEGIN DELETE FROM audio_genres_map WHERE genre_id = old._id;END");
                db.execSQL("CREATE TABLE IF NOT EXISTS audio_playlists (_id INTEGER PRIMARY KEY,_data TEXT,name TEXT NOT NULL,date_added INTEGER,date_modified INTEGER);");
                db.execSQL("CREATE TABLE IF NOT EXISTS audio_playlists_map (_id INTEGER PRIMARY KEY,audio_id INTEGER NOT NULL,playlist_id INTEGER NOT NULL,play_order INTEGER NOT NULL);");
                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_playlists_cleanup DELETE ON audio_playlists BEGIN DELETE FROM audio_playlists_map WHERE playlist_id = old._id;SELECT _DELETE_FILE(old._data);END");
                db.execSQL("CREATE TRIGGER IF NOT EXISTS albumart_cleanup1 DELETE ON albums BEGIN DELETE FROM album_art WHERE album_id = old.album_id;END");
                db.execSQL("CREATE TRIGGER IF NOT EXISTS albumart_cleanup2 DELETE ON album_art BEGIN SELECT _DELETE_FILE(old._data);END");
            }
            db.execSQL("CREATE TABLE IF NOT EXISTS video (_id INTEGER PRIMARY KEY,_data TEXT NOT NULL,_display_name TEXT,_size INTEGER,mime_type TEXT,date_added INTEGER,date_modified INTEGER,title TEXT,duration INTEGER,artist TEXT,album TEXT,resolution TEXT,description TEXT,isprivate INTEGER,tags TEXT,category TEXT,language TEXT,mini_thumb_data TEXT,latitude DOUBLE,longitude DOUBLE,datetaken INTEGER,mini_thumb_magic INTEGER);");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS video_cleanup DELETE ON video BEGIN SELECT _DELETE_FILE(old._data);END");
        }
        if (fromVersion < 64) {
            db.execSQL("CREATE INDEX IF NOT EXISTS sort_index on images(datetaken ASC, _id ASC);");
        }
        if (fromVersion < 65) {
            db.execSQL("CREATE INDEX IF NOT EXISTS titlekey_index on audio_meta(title_key);");
        }
        if (fromVersion < 67) {
            db.execSQL("CREATE INDEX IF NOT EXISTS albumkey_index on albums(album_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS artistkey_index on artists(artist_key);");
        }
        if (fromVersion < 68) {
            db.execSQL("ALTER TABLE video ADD COLUMN bucket_id TEXT;");
            db.execSQL("ALTER TABLE video ADD COLUMN bucket_display_name TEXT");
        }
        if (fromVersion < 69) {
            updateDisplayName(db, "images");
        }
        if (fromVersion < 70) {
            db.execSQL("ALTER TABLE video ADD COLUMN bookmark INTEGER;");
        }
        if (fromVersion < 71) {
            db.execSQL("UPDATE audio_meta SET date_modified=0 WHERE _id IN (SELECT _id FROM audio where mime_type='audio/mp4' AND artist='<unknown>' AND album='<unknown>');");
        }
        if (fromVersion < 72) {
            db.execSQL("ALTER TABLE audio_meta ADD COLUMN is_podcast INTEGER;");
            db.execSQL("UPDATE audio_meta SET is_podcast=1 WHERE _data LIKE '%/podcasts/%';");
            db.execSQL("UPDATE audio_meta SET is_music=0 WHERE is_podcast=1 AND _data NOT LIKE '%/music/%';");
            db.execSQL("ALTER TABLE audio_meta ADD COLUMN bookmark INTEGER;");
            recreateAudioView(db);
        }
        if (fromVersion < 73) {
            db.execSQL("UPDATE audio_meta SET is_music=1 WHERE is_music=0 AND _data LIKE '%/music/%';");
            db.execSQL("UPDATE audio_meta SET is_ringtone=1 WHERE is_ringtone=0 AND _data LIKE '%/ringtones/%';");
            db.execSQL("UPDATE audio_meta SET is_notification=1 WHERE is_notification=0 AND _data LIKE '%/notifications/%';");
            db.execSQL("UPDATE audio_meta SET is_alarm=1 WHERE is_alarm=0 AND _data LIKE '%/alarms/%';");
            db.execSQL("UPDATE audio_meta SET is_podcast=1 WHERE is_podcast=0 AND _data LIKE '%/podcasts/%';");
        }
        if (fromVersion < 74) {
            db.execSQL("CREATE VIEW IF NOT EXISTS searchhelpertitle AS SELECT * FROM audio ORDER BY title_key;");
            db.execSQL("CREATE VIEW IF NOT EXISTS search AS SELECT _id,'artist' AS mime_type,artist,NULL AS album,NULL AS title,artist AS text1,NULL AS text2,number_of_albums AS data1,number_of_tracks AS data2,artist_key AS match,'content://media/external/audio/artists/'||_id AS suggest_intent_data,1 AS grouporder FROM artist_info WHERE (artist!='<unknown>') UNION ALL SELECT _id,'album' AS mime_type,artist,album,NULL AS title,album AS text1,artist AS text2,NULL AS data1,NULL AS data2,artist_key||' '||album_key AS match,'content://media/external/audio/albums/'||_id AS suggest_intent_data,2 AS grouporder FROM album_info WHERE (album!='<unknown>') UNION ALL SELECT searchhelpertitle._id AS _id,mime_type,artist,album,title,title AS text1,artist AS text2,NULL AS data1,NULL AS data2,artist_key||' '||album_key||' '||title_key AS match,'content://media/external/audio/media/'||searchhelpertitle._id AS suggest_intent_data,3 AS grouporder FROM searchhelpertitle WHERE (title != '') ");
        }
        if (fromVersion < 75) {
            db.execSQL("UPDATE audio_meta SET date_modified=0;");
            db.execSQL("DELETE FROM albums");
        }
        if (fromVersion < 76) {
            db.execSQL("UPDATE audio_meta SET title_key=REPLACE(title_key,x'081D08C29F081D',x'081D') WHERE title_key LIKE '%'||x'081D08C29F081D'||'%';");
            db.execSQL("UPDATE albums SET album_key=REPLACE(album_key,x'081D08C29F081D',x'081D') WHERE album_key LIKE '%'||x'081D08C29F081D'||'%';");
            db.execSQL("UPDATE artists SET artist_key=REPLACE(artist_key,x'081D08C29F081D',x'081D') WHERE artist_key LIKE '%'||x'081D08C29F081D'||'%';");
        }
        if (fromVersion < 77) {
            db.execSQL("CREATE TABLE IF NOT EXISTS videothumbnails (_id INTEGER PRIMARY KEY,_data TEXT,video_id INTEGER,kind INTEGER,width INTEGER,height INTEGER);");
            db.execSQL("CREATE INDEX IF NOT EXISTS video_id_index on videothumbnails(video_id);");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS videothumbnails_cleanup DELETE ON videothumbnails BEGIN SELECT _DELETE_FILE(old._data);END");
        }
        if (fromVersion < 78) {
            db.execSQL("UPDATE video SET date_modified=0;");
        }
        if (fromVersion < 79) {
            String oldthumbspath = mExternalStoragePaths[0] + "/albumthumbs";
            String newthumbspath = mExternalStoragePaths[0] + "/Android/data/com.android.providers.media/albumthumbs";
            File thumbsfolder = new File(oldthumbspath);
            if (thumbsfolder.exists()) {
                File newthumbsfolder = new File(newthumbspath);
                newthumbsfolder.getParentFile().mkdirs();
                if (thumbsfolder.renameTo(newthumbsfolder)) {
                    db.execSQL("UPDATE album_art SET _data=REPLACE(_data, '" + oldthumbspath + "','" + newthumbspath + "');");
                }
            }
        }
        if (fromVersion < 80) {
            db.execSQL("UPDATE images SET date_modified=0;");
        }
        if (fromVersion < 81 && !internal) {
            db.execSQL("UPDATE audio_playlists SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE images SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE video SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE videothumbnails SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE thumbnails SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE album_art SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE audio_meta SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("DELETE FROM audio_playlists WHERE _data IS '////';");
            db.execSQL("DELETE FROM images WHERE _data IS '////';");
            db.execSQL("DELETE FROM video WHERE _data IS '////';");
            db.execSQL("DELETE FROM videothumbnails WHERE _data IS '////';");
            db.execSQL("DELETE FROM thumbnails WHERE _data IS '////';");
            db.execSQL("DELETE FROM audio_meta WHERE _data  IS '////';");
            db.execSQL("DELETE FROM album_art WHERE _data  IS '////';");
            db.execSQL("UPDATE audio_meta SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE audio_playlists SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE images SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE video SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE videothumbnails SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE thumbnails SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE album_art SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("DELETE from albums");
            db.execSQL("DELETE from artists");
            db.execSQL("UPDATE audio_meta SET date_modified=0;");
        }
        if (fromVersion < 82) {
            db.execSQL("DROP VIEW IF EXISTS artist_info");
            db.execSQL("CREATE VIEW IF NOT EXISTS artist_info AS SELECT artist_id AS _id, artist, artist_key, COUNT(DISTINCT album_key) AS number_of_albums, COUNT(*) AS number_of_tracks FROM audio WHERE is_music=1 GROUP BY artist_key;");
        }
        if (fromVersion < 87) {
            db.execSQL("CREATE INDEX IF NOT EXISTS title_idx on audio_meta(title);");
            db.execSQL("CREATE INDEX IF NOT EXISTS artist_idx on artists(artist);");
            db.execSQL("CREATE INDEX IF NOT EXISTS album_idx on albums(album);");
        }
        if (fromVersion < 88) {
            db.execSQL("DROP TRIGGER IF EXISTS albums_update1;");
            db.execSQL("DROP TRIGGER IF EXISTS albums_update2;");
            db.execSQL("DROP TRIGGER IF EXISTS albums_update3;");
            db.execSQL("DROP TRIGGER IF EXISTS albums_update4;");
            db.execSQL("DROP TRIGGER IF EXISTS artist_update1;");
            db.execSQL("DROP TRIGGER IF EXISTS artist_update2;");
            db.execSQL("DROP TRIGGER IF EXISTS artist_update3;");
            db.execSQL("DROP TRIGGER IF EXISTS artist_update4;");
            db.execSQL("DROP VIEW IF EXISTS album_artists;");
            db.execSQL("CREATE INDEX IF NOT EXISTS album_id_idx on audio_meta(album_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS artist_id_idx on audio_meta(artist_id);");
            db.execSQL("CREATE VIEW IF NOT EXISTS artists_albums_map AS SELECT DISTINCT artist_id, album_id FROM audio_meta;");
        }
        if (fromVersion < 91) {
            db.execSQL("DROP INDEX IF EXISTS mini_thumb_magic_index");
            db.execSQL("CREATE INDEX IF NOT EXISTS image_bucket_index ON images(bucket_id, datetaken)");
            db.execSQL("CREATE INDEX IF NOT EXISTS video_bucket_index ON video(bucket_id, datetaken)");
        }
        if (fromVersion <= 100) {
            db.execSQL("DROP TABLE IF EXISTS objects");
            db.execSQL("DROP TABLE IF EXISTS files");
            db.execSQL("DROP TRIGGER IF EXISTS images_objects_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS audio_objects_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS video_objects_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS playlists_objects_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup_images;");
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup_audio;");
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup_video;");
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup_playlists;");
            db.execSQL("DROP TRIGGER IF EXISTS media_cleanup;");
            db.execSQL("CREATE TABLE files (_id INTEGER PRIMARY KEY AUTOINCREMENT,_data TEXT,_size INTEGER,format INTEGER,parent INTEGER,date_added INTEGER,date_modified INTEGER,mime_type TEXT,title TEXT,description TEXT,_display_name TEXT,picasa_id TEXT,orientation INTEGER,latitude DOUBLE,longitude DOUBLE,datetaken INTEGER,mini_thumb_magic INTEGER,bucket_id TEXT,bucket_display_name TEXT,isprivate INTEGER,title_key TEXT,artist_id INTEGER,album_id INTEGER,composer TEXT,track INTEGER,year INTEGER CHECK(year!=0),is_ringtone INTEGER,is_music INTEGER,is_alarm INTEGER,is_notification INTEGER,is_podcast INTEGER,album_artist TEXT,duration INTEGER,bookmark INTEGER,artist TEXT,album TEXT,resolution TEXT,tags TEXT,category TEXT,language TEXT,mini_thumb_data TEXT,name TEXT,media_type INTEGER,old_id INTEGER);");
            db.execSQL("CREATE INDEX path_index ON files(_data);");
            db.execSQL("CREATE INDEX media_type_index ON files(media_type);");
            db.execSQL("INSERT INTO files (_id,_data,_display_name,_size,mime_type,date_added,date_modified,title,title_key,duration,artist_id,composer,album_id,track,year,is_ringtone,is_music,is_alarm,is_notification,is_podcast,bookmark,old_id,media_type) SELECT _id,_data,_display_name,_size,mime_type,date_added,date_modified,title,title_key,duration,artist_id,composer,album_id,track,year,is_ringtone,is_music,is_alarm,is_notification,is_podcast,bookmark,_id,2 FROM audio_meta;");
            db.execSQL("INSERT INTO files (_data,_size,_display_name,mime_type,title,date_added,date_modified,description,picasa_id,isprivate,latitude,longitude,datetaken,orientation,mini_thumb_magic,bucket_id,bucket_display_name,old_id,media_type) SELECT _data,_size,_display_name,mime_type,title,date_added,date_modified,description,picasa_id,isprivate,latitude,longitude,datetaken,orientation,mini_thumb_magic,bucket_id,bucket_display_name,_id,1 FROM images;");
            db.execSQL("INSERT INTO files (_data,_display_name,_size,mime_type,date_added,date_modified,title,duration,artist,album,resolution,description,isprivate,tags,category,language,mini_thumb_data,latitude,longitude,datetaken,mini_thumb_magic,bucket_id,bucket_display_name, bookmark,old_id,media_type) SELECT _data,_display_name,_size,mime_type,date_added,date_modified,title,duration,artist,album,resolution,description,isprivate,tags,category,language,mini_thumb_data,latitude,longitude,datetaken,mini_thumb_magic,bucket_id,bucket_display_name, bookmark,_id,3 FROM video;");
            if (!internal) {
                db.execSQL("INSERT INTO files (_data,name,date_added,date_modified,old_id,media_type) SELECT _data,name,date_added,date_modified,_id,4 FROM audio_playlists;");
            }
            db.execSQL("DROP TABLE IF EXISTS images");
            db.execSQL("DROP TABLE IF EXISTS audio_meta");
            db.execSQL("DROP TABLE IF EXISTS video");
            db.execSQL("DROP TABLE IF EXISTS audio_playlists");
            db.execSQL("CREATE VIEW images AS SELECT _id,_data,_size,_display_name,mime_type,title,date_added,date_modified,description,picasa_id,isprivate,latitude,longitude,datetaken,orientation,mini_thumb_magic,bucket_id,bucket_display_name FROM files WHERE media_type=1;");
            db.execSQL("CREATE VIEW audio_meta AS SELECT _id,_data,_display_name,_size,mime_type,date_added,date_modified,title,title_key,duration,artist_id,composer,album_id,track,year,is_ringtone,is_music,is_alarm,is_notification,is_podcast,bookmark,album_artist FROM files WHERE media_type=2;");
            db.execSQL("CREATE VIEW video AS SELECT _id,_data,_display_name,_size,mime_type,date_added,date_modified,title,duration,artist,album,resolution,description,isprivate,tags,category,language,mini_thumb_data,latitude,longitude,datetaken,mini_thumb_magic,bucket_id,bucket_display_name, bookmark FROM files WHERE media_type=3;");
            if (!internal) {
                db.execSQL("CREATE VIEW audio_playlists AS SELECT _id,_data,name,date_added,date_modified FROM files WHERE media_type=4;");
            }
            db.execSQL("CREATE INDEX tmp ON files(old_id);");
            db.execSQL("UPDATE thumbnails SET image_id = (SELECT _id FROM files WHERE files.old_id = thumbnails.image_id AND files.media_type = 1);");
            if (!internal) {
                db.execSQL("UPDATE audio_genres_map SET audio_id = (SELECT _id FROM files WHERE files.old_id = audio_genres_map.audio_id AND files.media_type = 2);");
                db.execSQL("UPDATE audio_playlists_map SET audio_id = (SELECT _id FROM files WHERE files.old_id = audio_playlists_map.audio_id AND files.media_type = 2);");
                db.execSQL("UPDATE audio_playlists_map SET playlist_id = (SELECT _id FROM files WHERE files.old_id = audio_playlists_map.playlist_id AND files.media_type = 4);");
            }
            db.execSQL("UPDATE videothumbnails SET video_id = (SELECT _id FROM files WHERE files.old_id = videothumbnails.video_id AND files.media_type = 3);");
            db.execSQL("DROP INDEX tmp;");
            db.execSQL("DROP INDEX IF EXISTS title_idx");
            db.execSQL("DROP INDEX IF EXISTS album_id_idx");
            db.execSQL("DROP INDEX IF EXISTS image_bucket_index");
            db.execSQL("DROP INDEX IF EXISTS video_bucket_index");
            db.execSQL("DROP INDEX IF EXISTS sort_index");
            db.execSQL("DROP INDEX IF EXISTS titlekey_index");
            db.execSQL("DROP INDEX IF EXISTS artist_id_idx");
            db.execSQL("CREATE INDEX title_idx ON files(title);");
            db.execSQL("CREATE INDEX album_id_idx ON files(album_id);");
            db.execSQL("CREATE INDEX bucket_index ON files(bucket_id, datetaken);");
            db.execSQL("CREATE INDEX sort_index ON files(datetaken ASC, _id ASC);");
            db.execSQL("CREATE INDEX titlekey_index ON files(title_key);");
            db.execSQL("CREATE INDEX artist_id_idx ON files(artist_id);");
            db.execSQL("DROP TRIGGER IF EXISTS images_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS audio_meta_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS video_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS audio_playlists_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS audio_delete");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS images_cleanup DELETE ON files WHEN old.media_type = 1 BEGIN DELETE FROM thumbnails WHERE image_id = old._id;SELECT _DELETE_FILE(old._data);END");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS video_cleanup DELETE ON files WHEN old.media_type = 3 BEGIN SELECT _DELETE_FILE(old._data);END");
            if (!internal) {
                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_meta_cleanup DELETE ON files WHEN old.media_type = 2 BEGIN DELETE FROM audio_genres_map WHERE audio_id = old._id;DELETE FROM audio_playlists_map WHERE audio_id = old._id;END");
                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_playlists_cleanup DELETE ON files WHEN old.media_type = 4 BEGIN DELETE FROM audio_playlists_map WHERE playlist_id = old._id;SELECT _DELETE_FILE(old._data);END");
                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_delete INSTEAD OF DELETE ON audio BEGIN DELETE from files where _id=old._id;DELETE from audio_playlists_map where audio_id=old._id;DELETE from audio_genres_map where audio_id=old._id;END");
            }
        }
        if (fromVersion < 301) {
            db.execSQL("DROP INDEX IF EXISTS bucket_index");
            db.execSQL("CREATE INDEX bucket_index on files(bucket_id, media_type, datetaken, _id)");
            db.execSQL("CREATE INDEX bucket_name on files(bucket_id, media_type, bucket_display_name)");
        }
        if (fromVersion < 302) {
            db.execSQL("CREATE INDEX parent_index ON files(parent);");
            db.execSQL("CREATE INDEX format_index ON files(format);");
        }
        if (fromVersion < 303) {
            db.execSQL("DELETE from albums");
            db.execSQL("UPDATE files SET date_modified=0 WHERE media_type=2;");
        }
        if (fromVersion < 304 && !internal) {
            db.execSQL("CREATE TRIGGER IF NOT EXISTS files_cleanup DELETE ON files BEGIN SELECT _OBJECT_REMOVED(old._id);END");
        }
        if (fromVersion < 305 && internal) {
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup");
        }
        if (fromVersion < 306 && !internal) {
            db.execSQL("UPDATE files SET date_modified=0 WHERE media_type=2;");
            db.execSQL("DELETE FROM audio_genres_map");
            db.execSQL("DELETE FROM audio_genres");
        }
        if (fromVersion < 307 && !internal) {
            db.execSQL("UPDATE files SET date_modified=0 WHERE media_type=1;");
        }
        if (fromVersion < 401 || (fromVersion == 401 && internal)) {
            db.execSQL("ALTER TABLE files ADD COLUMN storage_id INTEGER;");
            db.execSQL("UPDATE files SET storage_id=" + MtpStorage.getStorageId(0) + ";");
        }
        if (fromVersion < 403 && !internal) {
            db.execSQL("CREATE VIEW audio_genres_map_noid AS SELECT audio_id,genre_id from audio_genres_map;");
        }
        if (fromVersion < 404) {
            db.execSQL("DELETE from albums");
            db.execSQL("UPDATE files SET date_modified=0 WHERE media_type=2;");
        }
        if (fromVersion < 405) {
            db.execSQL("ALTER TABLE files ADD COLUMN is_drm INTEGER;");
            db.execSQL("DROP VIEW IF EXISTS audio_meta");
            db.execSQL("CREATE VIEW audio_meta AS SELECT _id,_data,_display_name,_size,mime_type,date_added,is_drm,date_modified,title,title_key,duration,artist_id,composer,album_id,track,year,is_ringtone,is_music,is_alarm,is_notification,is_podcast,bookmark,album_artist FROM files WHERE media_type=2;");
            recreateAudioView(db);
        }
        if (fromVersion < 407) {
            db.execSQL("UPDATE files SET date_modified=0;");
        }
        if (fromVersion < 408) {
            db.execSQL("ALTER TABLE files ADD COLUMN width INTEGER;");
            db.execSQL("ALTER TABLE files ADD COLUMN height INTEGER;");
            db.execSQL("UPDATE files SET date_modified=0;");
            db.execSQL("DROP VIEW IF EXISTS images");
            db.execSQL("DROP VIEW IF EXISTS video");
            db.execSQL("CREATE VIEW images AS SELECT _id,_data,_size,_display_name,mime_type,title,date_added,date_modified,description,picasa_id,isprivate,latitude,longitude,datetaken,orientation,mini_thumb_magic,bucket_id,bucket_display_name,width,height FROM files WHERE media_type=1;");
            db.execSQL("CREATE VIEW video AS SELECT _id,_data,_display_name,_size,mime_type,date_added,date_modified,title,duration,artist,album,resolution,description,isprivate,tags,category,language,mini_thumb_data,latitude,longitude,datetaken,mini_thumb_magic,bucket_id,bucket_display_name,bookmark,width,height FROM files WHERE media_type=3;");
        }
        if (fromVersion < 409 && !internal) {
            db.execSQL("UPDATE files SET date_modified=0 WHERE media_type=2;");
            db.execSQL("DELETE FROM audio_genres_map");
            db.execSQL("DELETE FROM audio_genres");
        }
        if (fromVersion < 500) {
            db.execSQL("DROP TRIGGER IF EXISTS videothumbnails_cleanup;");
        }
        if (fromVersion < 501) {
            db.execSQL("DROP TRIGGER IF EXISTS images_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS thumbnails_cleanup;");
        }
        if (fromVersion < 502) {
            db.execSQL("DROP TRIGGER IF EXISTS video_cleanup;");
        }
        if (fromVersion < 503) {
            db.execSQL("DROP TRIGGER IF EXISTS audio_delete");
            db.execSQL("DROP TRIGGER IF EXISTS audio_meta_cleanup");
        }
        if (fromVersion < 504) {
            db.execSQL("CREATE INDEX IF NOT EXISTS path_index_lower ON files(_data COLLATE NOCASE);");
        }
        if (fromVersion < 505) {
            db.execSQL("UPDATE files SET date_modified=0 WHERE media_type=3;");
        }
        if (fromVersion < 506) {
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup");
            db.execSQL("DELETE FROM files WHERE _data LIKE '/storage/%';");
            db.execSQL("DELETE FROM album_art WHERE _data LIKE '/storage/%';");
            db.execSQL("DELETE FROM thumbnails WHERE _data LIKE '/storage/%';");
            db.execSQL("DELETE FROM videothumbnails WHERE _data LIKE '/storage/%';");
            db.execSQL("UPDATE files SET _data='/storage/sdcard0'||SUBSTR(_data,12) WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE files SET _data='/storage/sdcard1'||SUBSTR(_data,15) WHERE _data LIKE '/mnt/external1/%';");
            db.execSQL("UPDATE album_art SET _data='/storage/sdcard0'||SUBSTR(_data,12) WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE album_art SET _data='/storage/sdcard1'||SUBSTR(_data,15) WHERE _data LIKE '/mnt/external1/%';");
            db.execSQL("UPDATE thumbnails SET _data='/storage/sdcard0'||SUBSTR(_data,12) WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE thumbnails SET _data='/storage/sdcard1'||SUBSTR(_data,15) WHERE _data LIKE '/mnt/external1/%';");
            db.execSQL("UPDATE videothumbnails SET _data='/storage/sdcard0'||SUBSTR(_data,12) WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE videothumbnails SET _data='/storage/sdcard1'||SUBSTR(_data,15) WHERE _data LIKE '/mnt/external1/%';");
            if (!internal) {
                db.execSQL("CREATE TRIGGER IF NOT EXISTS files_cleanup DELETE ON files BEGIN SELECT _OBJECT_REMOVED(old._id);END");
            }
        }
        if (fromVersion < 507) {
            updateBucketNames(db);
        }
        if (fromVersion < 508 && !internal) {
            db.execSQL("CREATE TABLE IF NOT EXISTS audio_genres_map_tmp (_id INTEGER PRIMARY KEY,audio_id INTEGER NOT NULL,genre_id INTEGER NOT NULL,UNIQUE (audio_id,genre_id) ON CONFLICT IGNORE);");
            db.execSQL("INSERT INTO audio_genres_map_tmp (audio_id,genre_id) SELECT DISTINCT audio_id,genre_id FROM audio_genres_map;");
            db.execSQL("DROP TABLE audio_genres_map;");
            db.execSQL("ALTER TABLE audio_genres_map_tmp RENAME TO audio_genres_map;");
        }
        if (fromVersion < 509) {
            db.execSQL("CREATE TABLE IF NOT EXISTS log (time DATETIME PRIMARY KEY, message TEXT);");
        }
        if (fromVersion < 510 && Environment.isExternalStorageEmulated()) {
            String externalStorage = Environment.getExternalStorageDirectory().toString();
            Log.d(TAG, "Adjusting external storage paths to: " + externalStorage);
            String[] tables = {"files", "album_art", "thumbnails", "videothumbnails"};
            for (String table : tables) {
                db.execSQL("UPDATE " + table + " SET _data='" + externalStorage + "'||SUBSTR(_data,17) WHERE _data LIKE '/storage/sdcard0/%';");
            }
        }
        if (fromVersion < 511) {
            updateBucketNames(db);
        }
        if (fromVersion < 600) {
            db.execSQL("CREATE TABLE files2 (_id INTEGER PRIMARY KEY AUTOINCREMENT,_data TEXT UNIQUE" + (internal ? "," : " COLLATE NOCASE,") + "_size INTEGER,format INTEGER,parent INTEGER,date_added INTEGER,date_modified INTEGER,mime_type TEXT,title TEXT,description TEXT,_display_name TEXT,picasa_id TEXT,orientation INTEGER,latitude DOUBLE,longitude DOUBLE,datetaken INTEGER,mini_thumb_magic INTEGER,bucket_id TEXT,bucket_display_name TEXT,isprivate INTEGER,title_key TEXT,artist_id INTEGER,album_id INTEGER,composer TEXT,track INTEGER,year INTEGER CHECK(year!=0),is_ringtone INTEGER,is_music INTEGER,is_alarm INTEGER,is_notification INTEGER,is_podcast INTEGER,album_artist TEXT,duration INTEGER,bookmark INTEGER,artist TEXT,album TEXT,resolution TEXT,tags TEXT,category TEXT,language TEXT,mini_thumb_data TEXT,name TEXT,media_type INTEGER,old_id INTEGER,storage_id INTEGER,is_drm INTEGER,width INTEGER, height INTEGER);");
            db.execSQL("INSERT OR REPLACE INTO files2 SELECT * FROM files;");
            db.execSQL("DROP TABLE files;");
            db.execSQL("ALTER TABLE files2 RENAME TO files;");
            db.execSQL("CREATE INDEX album_id_idx ON files(album_id);");
            db.execSQL("CREATE INDEX artist_id_idx ON files(artist_id);");
            db.execSQL("CREATE INDEX bucket_index on files(bucket_id,media_type,datetaken, _id);");
            db.execSQL("CREATE INDEX bucket_name on files(bucket_id,media_type,bucket_display_name);");
            db.execSQL("CREATE INDEX format_index ON files(format);");
            db.execSQL("CREATE INDEX media_type_index ON files(media_type);");
            db.execSQL("CREATE INDEX parent_index ON files(parent);");
            db.execSQL("CREATE INDEX path_index ON files(_data);");
            db.execSQL("CREATE INDEX sort_index ON files(datetaken ASC, _id ASC);");
            db.execSQL("CREATE INDEX title_idx ON files(title);");
            db.execSQL("CREATE INDEX titlekey_index ON files(title_key);");
            if (!internal) {
                db.execSQL("CREATE TRIGGER audio_playlists_cleanup DELETE ON files WHEN old.media_type=4 BEGIN DELETE FROM audio_playlists_map WHERE playlist_id = old._id;SELECT _DELETE_FILE(old._data);END;");
                db.execSQL("CREATE TRIGGER files_cleanup DELETE ON files BEGIN SELECT _OBJECT_REMOVED(old._id);END;");
            }
        }
        if (fromVersion < 601) {
            db.execSQL("CREATE TABLE IF NOT EXISTS log_tmp (time DATETIME, message TEXT);");
            db.execSQL("DELETE FROM log_tmp;");
            db.execSQL("INSERT INTO log_tmp SELECT time, message FROM log order by rowid;");
            db.execSQL("DROP TABLE log;");
            db.execSQL("ALTER TABLE log_tmp RENAME TO log;");
        }
        if (fromVersion < 700) {
            db.execSQL("UPDATE files set datetaken=date_modified*1000 WHERE date_modified IS NOT NULL AND datetaken IS NOT NULL AND datetaken<date_modified*5;");
        }
        if (fromVersion < 800) {
            db.execSQL("DELETE from albums");
            db.execSQL("DELETE from artists");
            db.execSQL("UPDATE files SET date_modified=0;");
        }
        sanityCheck(db, fromVersion);
        long elapsedSeconds = (SystemClock.currentTimeMicro() - startTime) / 1000000;
        logToDb(db, "Database upgraded from version " + fromVersion + " to " + toVersion + " in " + elapsedSeconds + " seconds");
    }

    static void logToDb(SQLiteDatabase db, String message) {
        db.execSQL("INSERT OR REPLACE INTO log (time,message) VALUES (strftime('%Y-%m-%d %H:%M:%f','now'),?);", new String[]{message});
        db.execSQL("DELETE FROM log WHERE rowid IN (SELECT rowid FROM log ORDER BY rowid DESC LIMIT 500,-1);");
    }

    private static void sanityCheck(SQLiteDatabase db, int fromVersion) {
        Cursor c1 = null;
        Cursor c2 = null;
        try {
            c1 = db.query("audio_meta", new String[]{"count(*)"}, null, null, null, null, null);
            c2 = db.query("audio_meta", new String[]{"count(distinct _data)"}, null, null, null, null, null);
            c1.moveToFirst();
            c2.moveToFirst();
            int num1 = c1.getInt(0);
            int num2 = c2.getInt(0);
            if (num1 != num2) {
                Log.e(TAG, "audio_meta._data column is not unique while upgrading from schema " + fromVersion + " : " + num1 + "/" + num2);
                db.execSQL("DELETE FROM audio_meta;");
            }
        } finally {
            IoUtils.closeQuietly(c1);
            IoUtils.closeQuietly(c2);
        }
    }

    private static void recreateAudioView(SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS audio");
        db.execSQL("CREATE VIEW IF NOT EXISTS audio as SELECT * FROM audio_meta LEFT OUTER JOIN artists ON audio_meta.artist_id=artists.artist_id LEFT OUTER JOIN albums ON audio_meta.album_id=albums.album_id;");
    }

    private static void updateBucketNames(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            String[] columns = {"_id", "_data"};
            Cursor cursor = db.query("files", columns, "media_type=1 OR media_type=3", null, null, null, null);
            try {
                int idColumnIndex = cursor.getColumnIndex("_id");
                int dataColumnIndex = cursor.getColumnIndex("_data");
                String[] rowId = new String[1];
                ContentValues values = new ContentValues();
                while (cursor.moveToNext()) {
                    String data = cursor.getString(dataColumnIndex);
                    rowId[0] = cursor.getString(idColumnIndex);
                    if (data != null) {
                        values.clear();
                        computeBucketValues(data, values);
                        db.update("files", values, "_id=?", rowId);
                    } else {
                        Log.w(TAG, "null data at id " + rowId);
                    }
                }
                IoUtils.closeQuietly(cursor);
                db.setTransactionSuccessful();
            } catch (Throwable th) {
                IoUtils.closeQuietly(cursor);
                throw th;
            }
        } finally {
            db.endTransaction();
        }
    }

    private static void updateDisplayName(SQLiteDatabase db, String tableName) {
        db.beginTransaction();
        try {
            String[] columns = {"_id", "_data", "_display_name"};
            Cursor cursor = db.query(tableName, columns, null, null, null, null, null);
            try {
                int idColumnIndex = cursor.getColumnIndex("_id");
                int dataColumnIndex = cursor.getColumnIndex("_data");
                int displayNameIndex = cursor.getColumnIndex("_display_name");
                ContentValues values = new ContentValues();
                while (cursor.moveToNext()) {
                    String displayName = cursor.getString(displayNameIndex);
                    if (displayName == null) {
                        String data = cursor.getString(dataColumnIndex);
                        values.clear();
                        computeDisplayName(data, values);
                        int rowId = cursor.getInt(idColumnIndex);
                        db.update(tableName, values, "_id=" + rowId, null);
                    }
                }
                IoUtils.closeQuietly(cursor);
                db.setTransactionSuccessful();
            } catch (Throwable th) {
                IoUtils.closeQuietly(cursor);
                throw th;
            }
        } finally {
            db.endTransaction();
        }
    }

    private static void computeBucketValues(String data, ContentValues values) {
        File parentFile = new File(data).getParentFile();
        if (parentFile == null) {
            parentFile = new File("/");
        }
        String path = parentFile.toString().toLowerCase();
        String name = parentFile.getName();
        values.put("bucket_id", Integer.valueOf(path.hashCode()));
        values.put("bucket_display_name", name);
    }

    private static void computeDisplayName(String data, ContentValues values) {
        String s = data == null ? "" : data.toString();
        int idx = s.lastIndexOf(47);
        if (idx >= 0) {
            s = s.substring(idx + 1);
        }
        values.put("_display_name", s);
    }

    private static void computeTakenTime(ContentValues values) {
        Long lastModified;
        if (!values.containsKey("datetaken") && (lastModified = values.getAsLong("date_modified")) != null) {
            values.put("datetaken", Long.valueOf(lastModified.longValue() * 1000));
        }
    }

    private boolean waitForThumbnailReady(Uri origUri) {
        Cursor c = query(origUri, new String[]{"_id", "_data", "mini_thumb_magic"}, null, null, null);
        boolean result = false;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    c.getLong(0);
                    String path = c.getString(1);
                    long magic = c.getLong(2);
                    MediaThumbRequest req = requestMediaThumbnail(path, origUri, 5, magic);
                    if (req != null) {
                        synchronized (req) {
                            while (req.mState == MediaThumbRequest.State.WAIT) {
                                try {
                                    req.wait();
                                } catch (InterruptedException e) {
                                    Log.w(TAG, e);
                                }
                            }
                            if (req.mState == MediaThumbRequest.State.DONE) {
                                result = true;
                            }
                        }
                    }
                }
            } finally {
                IoUtils.closeQuietly(c);
            }
        }
        return result;
    }

    private boolean matchThumbRequest(MediaThumbRequest req, int pid, long id, long gid, boolean isVideo) {
        boolean cancelAllOrigId = id == -1;
        boolean cancelAllGroupId = gid == -1;
        return req.mCallingPid == pid && (cancelAllGroupId || req.mGroupId == gid) && ((cancelAllOrigId || req.mOrigId == id) && req.mIsVideo == isVideo);
    }

    private boolean queryThumbnail(SQLiteQueryBuilder qb, Uri uri, String table, String column, boolean hasThumbnailId) {
        qb.setTables(table);
        if (hasThumbnailId) {
            qb.appendWhere("_id = " + uri.getPathSegments().get(3));
            return true;
        }
        String origId = uri.getQueryParameter("orig_id");
        if (origId == null) {
            return true;
        }
        boolean needBlocking = "1".equals(uri.getQueryParameter("blocking"));
        boolean cancelRequest = "1".equals(uri.getQueryParameter("cancel"));
        Uri origUri = uri.buildUpon().encodedPath(uri.getPath().replaceFirst("thumbnails", "media")).appendPath(origId).build();
        if (needBlocking && !waitForThumbnailReady(origUri)) {
            Log.w(TAG, "original media doesn't exist or it's canceled.");
            return false;
        }
        if (cancelRequest) {
            String groupId = uri.getQueryParameter("group_id");
            boolean isVideo = "video".equals(uri.getPathSegments().get(1));
            int pid = Binder.getCallingPid();
            try {
                long id = Long.parseLong(origId);
                long gid = Long.parseLong(groupId);
                synchronized (this.mMediaThumbQueue) {
                    if (this.mCurrentThumbRequest != null && matchThumbRequest(this.mCurrentThumbRequest, pid, id, gid, isVideo)) {
                        synchronized (this.mCurrentThumbRequest) {
                            this.mCurrentThumbRequest.mState = MediaThumbRequest.State.CANCEL;
                            this.mCurrentThumbRequest.notifyAll();
                        }
                    }
                    for (MediaThumbRequest mtq : this.mMediaThumbQueue) {
                        if (matchThumbRequest(mtq, pid, id, gid, isVideo)) {
                            synchronized (mtq) {
                                mtq.mState = MediaThumbRequest.State.CANCEL;
                                mtq.notifyAll();
                            }
                            this.mMediaThumbQueue.remove(mtq);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (origId != null) {
            qb.appendWhere(column + " = " + origId);
        }
        return true;
    }

    @Override
    public Uri canonicalize(Uri uri) {
        int match = URI_MATCHER.match(uri);
        if (match != 101) {
            return null;
        }
        Cursor c = query(uri, null, null, null, null);
        if (c != null) {
            try {
                if (c.getCount() == 1 && c.moveToNext()) {
                    Uri.Builder builder = uri.buildUpon();
                    builder.appendQueryParameter("canonical", "1");
                    String title = c.getString(c.getColumnIndex("title"));
                    IoUtils.closeQuietly(c);
                    if (TextUtils.isEmpty(title)) {
                        return null;
                    }
                    builder.appendQueryParameter("title", title);
                    Uri newUri = builder.build();
                    return newUri;
                }
            } finally {
                IoUtils.closeQuietly(c);
            }
        }
        return null;
    }

    @Override
    public Uri uncanonicalize(Uri uri) {
        String titleFromUri;
        if (uri == null || !"1".equals(uri.getQueryParameter("canonical"))) {
            return uri;
        }
        int match = URI_MATCHER.match(uri);
        if (match != 101 || (titleFromUri = uri.getQueryParameter("title")) == null) {
            return null;
        }
        Uri uri2 = uri.buildUpon().clearQuery().build();
        Cursor c = query(uri2, null, null, null, null);
        try {
            int titleIdx = c.getColumnIndex("title");
            if (c != null && c.getCount() == 1 && c.moveToNext() && titleFromUri.equals(c.getString(titleIdx))) {
                return uri2;
            }
            IoUtils.closeQuietly(c);
            Uri newUri = MediaStore.Audio.Media.getContentUri(uri2.getPathSegments().get(0));
            c = query(newUri, null, "title=?", new String[]{titleFromUri}, null);
            if (c == null) {
                return null;
            }
            if (!c.moveToNext()) {
                return null;
            }
            long id = c.getLong(c.getColumnIndex("_id"));
            return ContentUris.withAppendedId(newUri, id);
        } finally {
            IoUtils.closeQuietly(c);
        }
    }

    private Uri safeUncanonicalize(Uri uri) {
        Uri newUri = uncanonicalize(uri);
        return newUri != null ? newUri : uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
        Cursor c;
        Uri uri2 = safeUncanonicalize(uri);
        int table = URI_MATCHER.match(uri2);
        List<String> prependArgs = new ArrayList<>();
        if (table == 500) {
            if (this.mMediaScannerVolume == null) {
                return null;
            }
            MatrixCursor c2 = new MatrixCursor(new String[]{"volume"});
            c2.addRow(new String[]{this.mMediaScannerVolume});
            return c2;
        }
        if (table == 600) {
            MatrixCursor c3 = new MatrixCursor(new String[]{"fsid"});
            c3.addRow(new Integer[]{Integer.valueOf(this.mVolumeId)});
            return c3;
        }
        if (table == 601) {
            MatrixCursor c4 = new MatrixCursor(new String[]{"version"});
            c4.addRow(new Integer[]{Integer.valueOf(getDatabaseVersion(getContext()))});
            return c4;
        }
        String groupBy = null;
        DatabaseHelper helper = getDatabaseForUri(uri2);
        if (helper == null) {
            return null;
        }
        helper.mNumQueries++;
        SQLiteDatabase db = helper.getReadableDatabase();
        if (db == null) {
            return null;
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String limit = uri2.getQueryParameter("limit");
        String filter = uri2.getQueryParameter("filter");
        String[] keywords = null;
        if (filter != null) {
            String filter2 = Uri.decode(filter).trim();
            if (!TextUtils.isEmpty(filter2)) {
                String[] searchWords = filter2.split(" ");
                keywords = new String[searchWords.length];
                for (int i = 0; i < searchWords.length; i++) {
                    String key = MediaStore.Audio.keyFor(searchWords[i]);
                    keywords[i] = key.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                }
            }
        }
        if (uri2.getQueryParameter("distinct") != null) {
            qb.setDistinct(true);
        }
        boolean hasThumbnailId = false;
        switch (table) {
            case 1:
                qb.setTables("images");
                if (uri2.getQueryParameter("distinct") != null) {
                    qb.setDistinct(true);
                }
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                    String nonotify = uri2.getQueryParameter("nonotify");
                    if (nonotify == null || !nonotify.equals("1")) {
                        c.setNotificationUri(getContext().getContentResolver(), uri2);
                        return c;
                    }
                    return c;
                }
                return c;
            case 2:
                qb.setTables("images");
                if (uri2.getQueryParameter("distinct") != null) {
                    qb.setDistinct(true);
                }
                qb.appendWhere("_id=?");
                prependArgs.add(uri2.getPathSegments().get(3));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 4:
                hasThumbnailId = true;
            case 3:
                if (!queryThumbnail(qb, uri2, "thumbnails", "image_id", hasThumbnailId)) {
                    return null;
                }
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 100:
                if (projectionIn != null && projectionIn.length == 1 && selectionArgs == null && ((selection == null || selection.equalsIgnoreCase("is_music=1") || selection.equalsIgnoreCase("is_podcast=1")) && projectionIn[0].equalsIgnoreCase("count(*)") && keywords != null)) {
                    qb.setTables("audio_meta");
                } else {
                    qb.setTables("audio");
                    for (int i2 = 0; keywords != null && i2 < keywords.length; i2++) {
                        if (i2 > 0) {
                            qb.appendWhere(" AND ");
                        }
                        qb.appendWhere("artist_key||album_key||title_key LIKE ? ESCAPE '\\'");
                        prependArgs.add("%" + keywords[i2] + "%");
                    }
                }
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 101:
                qb.setTables("audio");
                qb.appendWhere("_id=?");
                prependArgs.add(uri2.getPathSegments().get(3));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 102:
                qb.setTables("audio_genres");
                qb.appendWhere("_id IN (SELECT genre_id FROM audio_genres_map WHERE audio_id=?)");
                prependArgs.add(uri2.getPathSegments().get(3));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 103:
                qb.setTables("audio_genres");
                qb.appendWhere("_id=?");
                prependArgs.add(uri2.getPathSegments().get(5));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 104:
                qb.setTables("audio_playlists");
                qb.appendWhere("_id IN (SELECT playlist_id FROM audio_playlists_map WHERE audio_id=?)");
                prependArgs.add(uri2.getPathSegments().get(3));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 105:
                qb.setTables("audio_playlists");
                qb.appendWhere("_id=?");
                prependArgs.add(uri2.getPathSegments().get(5));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 106:
                qb.setTables("audio_genres");
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 107:
                qb.setTables("audio_genres");
                qb.appendWhere("_id=?");
                prependArgs.add(uri2.getPathSegments().get(3));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 108:
            case 109:
                boolean simpleQuery = keywords == null && projectionIn != null && (selection == null || selection.equalsIgnoreCase("genre_id=?"));
                if (projectionIn != null) {
                    for (String p : projectionIn) {
                        if (p.equals("_id")) {
                            simpleQuery = false;
                        }
                        if (simpleQuery && !p.equals("audio_id") && !p.equals("genre_id")) {
                            simpleQuery = false;
                        }
                    }
                }
                if (simpleQuery) {
                    qb.setTables("audio_genres_map_noid");
                    if (table == 108) {
                        qb.appendWhere("genre_id=?");
                        prependArgs.add(uri2.getPathSegments().get(3));
                    }
                } else {
                    qb.setTables("audio_genres_map_noid, audio");
                    qb.appendWhere("audio._id = audio_id");
                    if (table == 108) {
                        qb.appendWhere(" AND genre_id=?");
                        prependArgs.add(uri2.getPathSegments().get(3));
                    }
                    for (int i3 = 0; keywords != null && i3 < keywords.length; i3++) {
                        qb.appendWhere(" AND ");
                        qb.appendWhere("artist_key||album_key||title_key LIKE ? ESCAPE '\\'");
                        prependArgs.add("%" + keywords[i3] + "%");
                    }
                }
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 110:
                qb.setTables("audio_playlists");
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 111:
                qb.setTables("audio_playlists");
                qb.appendWhere("_id=?");
                prependArgs.add(uri2.getPathSegments().get(3));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 112:
            case 113:
                boolean simpleQuery2 = keywords == null && projectionIn != null && (selection == null || selection.equalsIgnoreCase("playlist_id=?"));
                if (projectionIn != null) {
                    for (int i4 = 0; i4 < projectionIn.length; i4++) {
                        String p2 = projectionIn[i4];
                        if (simpleQuery2 && !p2.equals("audio_id") && !p2.equals("playlist_id") && !p2.equals("play_order")) {
                            simpleQuery2 = false;
                        }
                        if (p2.equals("_id")) {
                            projectionIn[i4] = "audio_playlists_map._id AS _id";
                        }
                    }
                }
                if (simpleQuery2) {
                    qb.setTables("audio_playlists_map");
                    qb.appendWhere("playlist_id=?");
                    prependArgs.add(uri2.getPathSegments().get(3));
                } else {
                    qb.setTables("audio_playlists_map, audio");
                    qb.appendWhere("audio._id = audio_id AND playlist_id=?");
                    prependArgs.add(uri2.getPathSegments().get(3));
                    for (int i5 = 0; keywords != null && i5 < keywords.length; i5++) {
                        qb.appendWhere(" AND ");
                        qb.appendWhere("artist_key||album_key||title_key LIKE ? ESCAPE '\\'");
                        prependArgs.add("%" + keywords[i5] + "%");
                    }
                }
                if (table == 113) {
                    qb.appendWhere(" AND audio_playlists_map._id=?");
                    prependArgs.add(uri2.getPathSegments().get(5));
                }
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 114:
                if (projectionIn != null && projectionIn.length == 1 && selectionArgs == null && ((selection == null || selection.length() == 0) && projectionIn[0].equalsIgnoreCase("count(*)") && keywords != null)) {
                    qb.setTables("audio_meta");
                    projectionIn[0] = "count(distinct artist_id)";
                    qb.appendWhere("is_music=1");
                } else {
                    qb.setTables("artist_info");
                    for (int i6 = 0; keywords != null && i6 < keywords.length; i6++) {
                        if (i6 > 0) {
                            qb.appendWhere(" AND ");
                        }
                        qb.appendWhere("artist_key LIKE ? ESCAPE '\\'");
                        prependArgs.add("%" + keywords[i6] + "%");
                    }
                }
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 115:
                qb.setTables("artist_info");
                qb.appendWhere("_id=?");
                prependArgs.add(uri2.getPathSegments().get(3));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 116:
                if (projectionIn != null && projectionIn.length == 1 && selectionArgs == null && ((selection == null || selection.length() == 0) && projectionIn[0].equalsIgnoreCase("count(*)") && keywords != null)) {
                    qb.setTables("audio_meta");
                    projectionIn[0] = "count(distinct album_id)";
                    qb.appendWhere("is_music=1");
                } else {
                    qb.setTables("album_info");
                    for (int i7 = 0; keywords != null && i7 < keywords.length; i7++) {
                        if (i7 > 0) {
                            qb.appendWhere(" AND ");
                        }
                        qb.appendWhere("artist_key||album_key LIKE ? ESCAPE '\\'");
                        prependArgs.add("%" + keywords[i7] + "%");
                    }
                }
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 117:
                qb.setTables("album_info");
                qb.appendWhere("_id=?");
                prependArgs.add(uri2.getPathSegments().get(3));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 118:
                String aid = uri2.getPathSegments().get(3);
                qb.setTables("audio LEFT OUTER JOIN album_art ON audio.album_id=album_art.album_id");
                qb.appendWhere("is_music=1 AND audio.album_id IN (SELECT album_id FROM artists_albums_map WHERE artist_id=?)");
                prependArgs.add(aid);
                for (int i8 = 0; keywords != null && i8 < keywords.length; i8++) {
                    qb.appendWhere(" AND ");
                    qb.appendWhere("artist_key||album_key LIKE ? ESCAPE '\\'");
                    prependArgs.add("%" + keywords[i8] + "%");
                }
                groupBy = "audio.album_id";
                sArtistAlbumsMap.put("numsongs_by_artist", "count(CASE WHEN artist_id==" + aid + " THEN 'foo' ELSE NULL END) AS numsongs_by_artist");
                qb.setProjectionMap(sArtistAlbumsMap);
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 120:
                qb.setTables("album_art");
                qb.appendWhere("album_id=?");
                prependArgs.add(uri2.getPathSegments().get(3));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 200:
                qb.setTables("video");
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 201:
                qb.setTables("video");
                qb.appendWhere("_id=?");
                prependArgs.add(uri2.getPathSegments().get(3));
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 203:
                hasThumbnailId = true;
            case 202:
                if (!queryThumbnail(qb, uri2, "videothumbnails", "video_id", hasThumbnailId)) {
                    return null;
                }
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 400:
                Log.w(TAG, "Legacy media search Uri used. Please update your code.");
            case 401:
            case 402:
                return doAudioSearch(db, qb, uri2, projectionIn, selection, combine(prependArgs, selectionArgs), sort, table, limit);
            case 701:
            case 703:
                qb.appendWhere("_id=?");
                prependArgs.add(uri2.getPathSegments().get(2));
            case 700:
            case 702:
                qb.setTables("files");
                c = qb.query(db, projectionIn, selection, combine(prependArgs, selectionArgs), groupBy, null, sort, limit);
                if (c != null) {
                }
                break;
            case 704:
                int handle = Integer.parseInt(uri2.getPathSegments().get(2));
                return getObjectReferences(helper, db, handle);
            default:
                throw new IllegalStateException("Unknown URL: " + uri2.toString());
        }
    }

    private String[] combine(List<String> prepend, String[] userArgs) {
        int presize = prepend.size();
        if (presize != 0) {
            int usersize = userArgs != null ? userArgs.length : 0;
            String[] combined = new String[presize + usersize];
            for (int i = 0; i < presize; i++) {
                combined[i] = prepend.get(i);
            }
            for (int i2 = 0; i2 < usersize; i2++) {
                combined[presize + i2] = userArgs[i2];
            }
            return combined;
        }
        return userArgs;
    }

    private Cursor doAudioSearch(SQLiteDatabase db, SQLiteQueryBuilder qb, Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort, int mode, String limit) {
        String[] cols;
        String mSearchString = (uri.getPath().endsWith("/") ? "" : uri.getLastPathSegment()).replaceAll("  ", " ").trim().toLowerCase();
        String[] searchWords = mSearchString.length() > 0 ? mSearchString.split(" ") : new String[0];
        String[] wildcardWords = new String[searchWords.length];
        int len = searchWords.length;
        for (int i = 0; i < len; i++) {
            String key = MediaStore.Audio.keyFor(searchWords[i]);
            wildcardWords[i] = (searchWords[i].equals("a") || searchWords[i].equals("an") || searchWords[i].equals("the")) ? "%" : "%" + key.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        }
        String where = "";
        for (int i2 = 0; i2 < searchWords.length; i2++) {
            if (i2 == 0) {
                where = "match LIKE ? ESCAPE '\\'";
            } else {
                where = where + " AND match LIKE ? ESCAPE '\\'";
            }
        }
        qb.setTables("search");
        if (mode == 402) {
            cols = this.mSearchColsFancy;
        } else if (mode == 401) {
            cols = this.mSearchColsBasic;
        } else {
            cols = this.mSearchColsLegacy;
        }
        return qb.query(db, cols, where, wildcardWords, null, null, null, limit);
    }

    @Override
    public String getType(Uri url) {
        switch (URI_MATCHER.match(url)) {
            case 1:
            case 3:
                return "vnd.android.cursor.dir/image";
            case 2:
            case 101:
            case 113:
            case 201:
            case 701:
                Cursor c = null;
                try {
                    c = query(url, MIME_TYPE_PROJECTION, null, null, null);
                    if (c != null && c.getCount() == 1) {
                        c.moveToFirst();
                        String mimeType = c.getString(1);
                        c.deactivate();
                        return mimeType;
                    }
                } finally {
                    IoUtils.closeQuietly(c);
                }
                break;
            case 4:
            case 120:
                return "image/jpeg";
            case 100:
            case 108:
            case 112:
                return "vnd.android.cursor.dir/audio";
            case 102:
            case 106:
                return "vnd.android.cursor.dir/genre";
            case 103:
            case 107:
                return "vnd.android.cursor.item/genre";
            case 104:
            case 110:
                return "vnd.android.cursor.dir/playlist";
            case 105:
            case 111:
                return "vnd.android.cursor.item/playlist";
            case 200:
                return "vnd.android.cursor.dir/video";
        }
        throw new IllegalStateException("Unknown URL : " + url);
    }

    private ContentValues ensureFile(boolean internal, ContentValues initialValues, String preferredExtension, String directoryName) {
        String file = initialValues.getAsString("_data");
        if (TextUtils.isEmpty(file)) {
            String file2 = generateFileName(internal, preferredExtension, directoryName);
            ContentValues values = new ContentValues(initialValues);
            values.put("_data", file2);
            return values;
        }
        return initialValues;
    }

    private void sendObjectAdded(long objectHandle) {
        synchronized (this.mMtpServiceConnection) {
            if (this.mMtpService != null) {
                try {
                    this.mMtpService.sendObjectAdded((int) objectHandle);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in sendObjectAdded", e);
                    this.mMtpService = null;
                }
            }
        }
    }

    private void sendObjectRemoved(long objectHandle) {
        synchronized (this.mMtpServiceConnection) {
            if (this.mMtpService != null) {
                try {
                    this.mMtpService.sendObjectRemoved((int) objectHandle);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in sendObjectRemoved", e);
                    this.mMtpService = null;
                }
            }
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int match = URI_MATCHER.match(uri);
        if (match == 300) {
            return super.bulkInsert(uri, values);
        }
        DatabaseHelper helper = getDatabaseForUri(uri);
        if (helper == null) {
            throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        if (db == null) {
            throw new IllegalStateException("Couldn't open database for " + uri);
        }
        if (match == 111 || match == 112) {
            return playlistBulkInsert(db, uri, values);
        }
        if (match == 704) {
            int handle = Integer.parseInt(uri.getPathSegments().get(2));
            return setObjectReferences(helper, db, handle, values);
        }
        db.beginTransaction();
        ArrayList<Long> notifyRowIds = new ArrayList<>();
        try {
            int len = values.length;
            for (int i = 0; i < len; i++) {
                if (values[i] != null) {
                    insertInternal(uri, match, values[i], notifyRowIds);
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            notifyMtp(notifyRowIds);
            getContext().getContentResolver().notifyChange(uri, null);
            return len;
        } catch (Throwable th) {
            db.endTransaction();
            throw th;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        int match = URI_MATCHER.match(uri);
        ArrayList<Long> notifyRowIds = new ArrayList<>();
        Uri newUri = insertInternal(uri, match, initialValues, notifyRowIds);
        notifyMtp(notifyRowIds);
        if (newUri != null && match != 702) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return newUri;
    }

    private void notifyMtp(ArrayList<Long> rowIds) {
        int size = rowIds.size();
        for (int i = 0; i < size; i++) {
            sendObjectAdded(rowIds.get(i).longValue());
        }
    }

    private int playlistBulkInsert(SQLiteDatabase db, Uri uri, ContentValues[] values) {
        DatabaseUtils.InsertHelper helper = new DatabaseUtils.InsertHelper(db, "audio_playlists_map");
        int audioidcolidx = helper.getColumnIndex("audio_id");
        int playlistididx = helper.getColumnIndex("playlist_id");
        int playorderidx = helper.getColumnIndex("play_order");
        long playlistId = Long.parseLong(uri.getPathSegments().get(3));
        db.beginTransaction();
        try {
            int len = values.length;
            for (int i = 0; i < len; i++) {
                helper.prepareForInsert();
                long audioid = ((Number) values[i].get("audio_id")).longValue();
                helper.bind(audioidcolidx, audioid);
                helper.bind(playlistididx, playlistId);
                int playorder = ((Number) values[i].get("play_order")).intValue();
                helper.bind(playorderidx, playorder);
                helper.execute();
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            helper.close();
            getContext().getContentResolver().notifyChange(uri, null);
            return len;
        } catch (Throwable th) {
            db.endTransaction();
            helper.close();
            throw th;
        }
    }

    private long insertDirectory(DatabaseHelper helper, SQLiteDatabase db, String path) {
        ContentValues values = new ContentValues();
        values.put("format", (Integer) 12289);
        values.put("_data", path);
        values.put("parent", Long.valueOf(getParent(helper, db, path)));
        values.put("storage_id", Integer.valueOf(getStorageId(path)));
        File file = new File(path);
        if (file.exists()) {
            values.put("date_modified", Long.valueOf(file.lastModified() / 1000));
        }
        helper.mNumInserts++;
        long rowId = db.insert("files", "date_modified", values);
        sendObjectAdded(rowId);
        return rowId;
    }

    private long getParent(DatabaseHelper helper, SQLiteDatabase db, String path) {
        long id;
        int lastSlash = path.lastIndexOf(47);
        if (lastSlash > 0) {
            String parentPath = path.substring(0, lastSlash);
            for (int i = 0; i < mExternalStoragePaths.length; i++) {
                if (parentPath.equals(mExternalStoragePaths[i])) {
                    return 0L;
                }
            }
            Long cid = this.mDirectoryCache.get(parentPath);
            if (cid != null) {
                return cid.longValue();
            }
            String[] selargs = {parentPath};
            helper.mNumQueries++;
            Cursor c = db.query("files", sIdOnlyColumn, "_data=?", selargs, null, null, null);
            if (c != null) {
                try {
                    if (c.getCount() == 0) {
                        id = insertDirectory(helper, db, parentPath);
                    } else {
                        if (c.getCount() > 1) {
                            Log.e(TAG, "more than one match for " + parentPath);
                        }
                        c.moveToFirst();
                        id = c.getLong(0);
                    }
                } finally {
                    IoUtils.closeQuietly(c);
                }
            }
            this.mDirectoryCache.put(parentPath, Long.valueOf(id));
            return id;
        }
        return 0L;
    }

    private int getStorageId(String path) {
        int length;
        for (int i = 0; i < mExternalStoragePaths.length; i++) {
            String test = mExternalStoragePaths[i];
            if (path.startsWith(test) && (path.length() == (length = test.length()) || path.charAt(length) == '/')) {
                return MtpStorage.getStorageId(i);
            }
        }
        return MtpStorage.getStorageId(0);
    }

    private long insertFile(DatabaseHelper helper, Uri uri, ContentValues initialValues, int mediaType, boolean notify, ArrayList<Long> notifyRowIds) {
        long artistRowId;
        long albumRowId;
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = null;
        switch (mediaType) {
            case 1:
                values = ensureFile(helper.mInternal, initialValues, ".jpg", "Pictures");
                values.put("date_added", Long.valueOf(System.currentTimeMillis() / 1000));
                String data = values.getAsString("_data");
                if (!values.containsKey("_display_name")) {
                    computeDisplayName(data, values);
                }
                computeTakenTime(values);
                break;
            case 2:
                values = new ContentValues(initialValues);
                String albumartist = values.getAsString("album_artist");
                String compilation = values.getAsString("compilation");
                values.remove("compilation");
                Object so = values.get("artist");
                String s = so == null ? "" : so.toString();
                values.remove("artist");
                HashMap<String, Long> artistCache = helper.mArtistCache;
                String path = values.getAsString("_data");
                synchronized (artistCache) {
                    Long temp = artistCache.get(s);
                    if (temp == null) {
                        artistRowId = getKeyIdForName(helper, db, "artists", "artist_key", "artist", s, s, path, 0, null, artistCache, uri);
                    } else {
                        artistRowId = temp.longValue();
                    }
                    break;
                }
                String artist = s;
                Object so2 = values.get("album");
                String s2 = so2 == null ? "" : so2.toString();
                values.remove("album");
                HashMap<String, Long> albumCache = helper.mAlbumCache;
                synchronized (albumCache) {
                    int albumhash = 0;
                    if (albumartist != null) {
                        albumhash = albumartist.hashCode();
                    } else if (compilation == null || !compilation.equals("1")) {
                        albumhash = path.substring(0, path.lastIndexOf(47)).hashCode();
                    }
                    String cacheName = s2 + albumhash;
                    Long temp2 = albumCache.get(cacheName);
                    if (temp2 == null) {
                        albumRowId = getKeyIdForName(helper, db, "albums", "album_key", "album", s2, cacheName, path, albumhash, artist, albumCache, uri);
                    } else {
                        albumRowId = temp2.longValue();
                    }
                    break;
                }
                values.put("artist_id", Integer.toString((int) artistRowId));
                values.put("album_id", Integer.toString((int) albumRowId));
                Object so3 = values.getAsString("title");
                String s3 = so3 == null ? "" : so3.toString();
                values.put("title_key", MediaStore.Audio.keyFor(s3));
                values.remove("title");
                values.put("title", s3.trim());
                computeDisplayName(values.getAsString("_data"), values);
                break;
            case 3:
                values = ensureFile(helper.mInternal, initialValues, ".3gp", "video");
                String data2 = values.getAsString("_data");
                computeDisplayName(data2, values);
                computeTakenTime(values);
                break;
        }
        if (values == null) {
            values = new ContentValues(initialValues);
        }
        String path2 = values.getAsString("_data");
        if (path2 != null) {
            computeBucketValues(path2, values);
        }
        values.put("date_added", Long.valueOf(System.currentTimeMillis() / 1000));
        long rowId = 0;
        Integer i = values.getAsInteger("media_scanner_new_object_id");
        if (i != null) {
            rowId = i.intValue();
            ContentValues values2 = new ContentValues(values);
            values2.remove("media_scanner_new_object_id");
            values = values2;
        }
        String title = values.getAsString("title");
        if (title == null && path2 != null) {
            title = MediaFile.getFileTitle(path2);
        }
        values.put("title", title);
        String mimeType = values.getAsString("mime_type");
        Integer formatObject = values.getAsInteger("format");
        int format = formatObject == null ? 0 : formatObject.intValue();
        if (format == 0) {
            if (TextUtils.isEmpty(path2)) {
                if (mediaType == 4) {
                    values.put("format", (Integer) 47621);
                    path2 = mExternalStoragePaths[0] + "/Playlists/" + values.getAsString("name");
                    values.put("_data", path2);
                    values.put("parent", Long.valueOf(getParent(helper, db, path2)));
                } else {
                    Log.e(TAG, "path is empty in insertFile()");
                }
            } else {
                format = MediaFile.getFormatCode(path2, mimeType);
            }
        }
        if (format != 0) {
            values.put("format", Integer.valueOf(format));
            if (mimeType == null) {
                mimeType = MediaFile.getMimeTypeForFormatCode(format);
            }
        }
        if (mimeType == null && path2 != null) {
            mimeType = MediaFile.getMimeTypeForFile(path2);
        }
        if (mimeType != null) {
            values.put("mime_type", mimeType);
            if (mediaType == 0 && !MediaScanner.isNoMediaPath(path2)) {
                int fileType = MediaFile.getFileTypeForMimeType(mimeType);
                if (MediaFile.isAudioFileType(fileType)) {
                    mediaType = 2;
                } else if (MediaFile.isVideoFileType(fileType)) {
                    mediaType = 3;
                } else if (MediaFile.isImageFileType(fileType)) {
                    mediaType = 1;
                } else if (MediaFile.isPlayListFileType(fileType)) {
                    mediaType = 4;
                }
            }
        }
        values.put("media_type", Integer.valueOf(mediaType));
        if (rowId == 0) {
            if (mediaType == 4) {
                String name = values.getAsString("name");
                if (name == null && path2 == null) {
                    throw new IllegalArgumentException("no name was provided when inserting abstract playlist");
                }
            } else if (path2 == null) {
                throw new IllegalArgumentException("no path was provided when inserting new file");
            }
            if (path2 != null) {
                File file = new File(path2);
                if (file.exists()) {
                    values.put("date_modified", Long.valueOf(file.lastModified() / 1000));
                    if (!values.containsKey("_size")) {
                        values.put("_size", Long.valueOf(file.length()));
                    }
                    if (mediaType == 1 || mediaType == 3) {
                        computeTakenTime(values);
                    }
                }
            }
            Long parent = values.getAsLong("parent");
            if (parent == null && path2 != null) {
                long parentId = getParent(helper, db, path2);
                values.put("parent", Long.valueOf(parentId));
            }
            Integer storage = values.getAsInteger("storage_id");
            if (storage == null) {
                int storageId = getStorageId(path2);
                values.put("storage_id", Integer.valueOf(storageId));
            }
            helper.mNumInserts++;
            rowId = db.insert("files", "date_modified", values);
            if (rowId != -1 && notify) {
                notifyRowIds.add(Long.valueOf(rowId));
            }
        } else {
            helper.mNumUpdates++;
            db.update("files", values, "_id=?", new String[]{Long.toString(rowId)});
        }
        if (format == 12289) {
            this.mDirectoryCache.put(path2, Long.valueOf(rowId));
        }
        return rowId;
    }

    private Cursor getObjectReferences(DatabaseHelper helper, SQLiteDatabase db, int handle) {
        Cursor cursorRawQuery = null;
        helper.mNumQueries++;
        Cursor c = db.query("files", sMediaTableColumns, "_id=?", new String[]{Integer.toString(handle)}, null, null, null);
        if (c != null) {
            try {
                if (c.moveToNext()) {
                    long playlistId = c.getLong(0);
                    int mediaType = c.getInt(1);
                    if (mediaType == 4) {
                        helper.mNumQueries++;
                        cursorRawQuery = db.rawQuery("SELECT audio_id FROM audio_playlists_map WHERE playlist_id=? ORDER BY play_order", new String[]{Long.toString(playlistId)});
                    }
                }
            } finally {
                IoUtils.closeQuietly(c);
            }
        }
        return cursorRawQuery;
    }

    private int setObjectReferences(DatabaseHelper helper, SQLiteDatabase db, int handle, ContentValues[] values) {
        int added;
        long playlistId = 0;
        helper.mNumQueries++;
        Cursor c = db.query("files", sMediaTableColumns, "_id=?", new String[]{Integer.toString(handle)}, null, null, null);
        if (c != null) {
            try {
                if (c.moveToNext()) {
                    int mediaType = c.getInt(1);
                    if (mediaType == 4) {
                        playlistId = c.getLong(0);
                    } else {
                        return 0;
                    }
                }
            } finally {
            }
        }
        IoUtils.closeQuietly(c);
        if (playlistId == 0) {
            return 0;
        }
        helper.mNumDeletes++;
        db.delete("audio_playlists_map", "playlist_id=?", new String[]{Long.toString(playlistId)});
        int count = values.length;
        int added2 = 0;
        ContentValues[] valuesList = new ContentValues[count];
        int i = 0;
        while (true) {
            added = added2;
            if (i >= count) {
                break;
            }
            long audioId = 0;
            long objectId = values[i].getAsLong("_id").longValue();
            helper.mNumQueries++;
            c = db.query("files", sMediaTableColumns, "_id=?", new String[]{Long.toString(objectId)}, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToNext()) {
                        int mediaType2 = c.getInt(1);
                        if (mediaType2 == 2) {
                            audioId = c.getLong(0);
                            IoUtils.closeQuietly(c);
                            if (audioId == 0) {
                                ContentValues v = new ContentValues();
                                v.put("playlist_id", Long.valueOf(playlistId));
                                v.put("audio_id", Long.valueOf(audioId));
                                v.put("play_order", Integer.valueOf(added));
                                added2 = added + 1;
                                valuesList[added] = v;
                            } else {
                                added2 = added;
                            }
                        } else {
                            IoUtils.closeQuietly(c);
                            added2 = added;
                        }
                    } else {
                        IoUtils.closeQuietly(c);
                        if (audioId == 0) {
                        }
                    }
                } finally {
                }
            }
            i++;
        }
        if (added < count) {
            ContentValues[] newValues = new ContentValues[added];
            System.arraycopy(valuesList, 0, newValues, 0, added);
            valuesList = newValues;
        }
        return playlistBulkInsert(db, MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId), valuesList);
    }

    private void updateGenre(long rowId, String genre) {
        Uri uri;
        Cursor cursor = null;
        Uri genresUri = MediaStore.Audio.Genres.getContentUri("external");
        try {
            cursor = query(genresUri, GENRE_LOOKUP_PROJECTION, "name=?", new String[]{genre}, null);
            if (cursor == null || cursor.getCount() == 0) {
                ContentValues values = new ContentValues();
                values.put("name", genre);
                uri = insert(genresUri, values);
            } else {
                cursor.moveToNext();
                uri = ContentUris.withAppendedId(genresUri, cursor.getLong(0));
            }
            if (uri != null) {
                uri = Uri.withAppendedPath(uri, "members");
            }
            if (uri != null) {
                ContentValues values2 = new ContentValues();
                values2.put("audio_id", Long.valueOf(rowId));
                insert(uri, values2);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }

    private Uri insertInternal(Uri uri, int match, ContentValues initialValues, ArrayList<Long> notifyRowIds) {
        ContentValues values;
        String volumeName = getVolumeName(uri);
        if (match == 500) {
            this.mMediaScannerVolume = initialValues.getAsString("volume");
            DatabaseHelper database = getDatabaseForUri(Uri.parse("content://media/" + this.mMediaScannerVolume + "/audio"));
            if (database == null) {
                Log.w(TAG, "no database for scanned volume " + this.mMediaScannerVolume);
            } else {
                database.mScanStartTime = SystemClock.currentTimeMicro();
            }
            return MediaStore.getMediaScannerUri();
        }
        String genre = null;
        String path = null;
        if (initialValues != null) {
            genre = initialValues.getAsString("genre");
            initialValues.remove("genre");
            path = initialValues.getAsString("_data");
        }
        Uri newUri = null;
        DatabaseHelper helper = getDatabaseForUri(uri);
        if (helper == null && match != 300 && match != 705) {
            throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        SQLiteDatabase db = (match == 300 || match == 705) ? null : helper.getWritableDatabase();
        switch (match) {
            case 1:
                long rowId = insertFile(helper, uri, initialValues, 1, true, notifyRowIds);
                if (rowId > 0) {
                    MediaDocumentsProvider.onMediaStoreInsert(getContext(), volumeName, 1, rowId);
                    newUri = ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(volumeName), rowId);
                }
                break;
            case 3:
                ContentValues values2 = ensureFile(helper.mInternal, initialValues, ".jpg", "DCIM/.thumbnails");
                helper.mNumInserts++;
                long rowId2 = db.insert("thumbnails", "name", values2);
                if (rowId2 > 0) {
                    newUri = ContentUris.withAppendedId(MediaStore.Images.Thumbnails.getContentUri(volumeName), rowId2);
                }
                break;
            case 100:
                long rowId3 = insertFile(helper, uri, initialValues, 2, true, notifyRowIds);
                if (rowId3 > 0) {
                    MediaDocumentsProvider.onMediaStoreInsert(getContext(), volumeName, 2, rowId3);
                    newUri = ContentUris.withAppendedId(MediaStore.Audio.Media.getContentUri(volumeName), rowId3);
                    if (genre != null) {
                        updateGenre(rowId3, genre);
                    }
                }
                break;
            case 102:
                Long audioId = Long.valueOf(Long.parseLong(uri.getPathSegments().get(2)));
                ContentValues values3 = new ContentValues(initialValues);
                values3.put("audio_id", audioId);
                helper.mNumInserts++;
                long rowId4 = db.insert("audio_genres_map", "genre_id", values3);
                if (rowId4 > 0) {
                    newUri = ContentUris.withAppendedId(uri, rowId4);
                }
                break;
            case 104:
                Long audioId2 = Long.valueOf(Long.parseLong(uri.getPathSegments().get(2)));
                ContentValues values4 = new ContentValues(initialValues);
                values4.put("audio_id", audioId2);
                helper.mNumInserts++;
                long rowId5 = db.insert("audio_playlists_map", "playlist_id", values4);
                if (rowId5 > 0) {
                    newUri = ContentUris.withAppendedId(uri, rowId5);
                }
                break;
            case 106:
                helper.mNumInserts++;
                long rowId6 = db.insert("audio_genres", "audio_id", initialValues);
                if (rowId6 > 0) {
                    newUri = ContentUris.withAppendedId(MediaStore.Audio.Genres.getContentUri(volumeName), rowId6);
                }
                break;
            case 108:
                Long genreId = Long.valueOf(Long.parseLong(uri.getPathSegments().get(3)));
                ContentValues values5 = new ContentValues(initialValues);
                values5.put("genre_id", genreId);
                helper.mNumInserts++;
                long rowId7 = db.insert("audio_genres_map", "genre_id", values5);
                if (rowId7 > 0) {
                    newUri = ContentUris.withAppendedId(uri, rowId7);
                }
                break;
            case 110:
                ContentValues values6 = new ContentValues(initialValues);
                values6.put("date_added", Long.valueOf(System.currentTimeMillis() / 1000));
                long rowId8 = insertFile(helper, uri, values6, 4, true, notifyRowIds);
                if (rowId8 > 0) {
                    newUri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.getContentUri(volumeName), rowId8);
                }
                break;
            case 111:
            case 112:
                Long playlistId = Long.valueOf(Long.parseLong(uri.getPathSegments().get(3)));
                ContentValues values7 = new ContentValues(initialValues);
                values7.put("playlist_id", playlistId);
                helper.mNumInserts++;
                long rowId9 = db.insert("audio_playlists_map", "playlist_id", values7);
                if (rowId9 > 0) {
                    newUri = ContentUris.withAppendedId(uri, rowId9);
                }
                break;
            case 119:
                if (helper.mInternal) {
                    throw new UnsupportedOperationException("no internal album art allowed");
                }
                try {
                    values = ensureFile(false, initialValues, "", "Android/data/com.android.providers.media/albumthumbs");
                } catch (IllegalStateException e) {
                    values = initialValues;
                }
                helper.mNumInserts++;
                long rowId10 = db.insert("album_art", "_data", values);
                if (rowId10 > 0) {
                    newUri = ContentUris.withAppendedId(uri, rowId10);
                }
                break;
                break;
            case 200:
                long rowId11 = insertFile(helper, uri, initialValues, 3, true, notifyRowIds);
                if (rowId11 > 0) {
                    MediaDocumentsProvider.onMediaStoreInsert(getContext(), volumeName, 3, rowId11);
                    newUri = ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri(volumeName), rowId11);
                }
                break;
            case 202:
                ContentValues values8 = ensureFile(helper.mInternal, initialValues, ".jpg", "DCIM/.thumbnails");
                helper.mNumInserts++;
                long rowId12 = db.insert("videothumbnails", "name", values8);
                if (rowId12 > 0) {
                    newUri = ContentUris.withAppendedId(MediaStore.Video.Thumbnails.getContentUri(volumeName), rowId12);
                }
                break;
            case 300:
                String name = initialValues.getAsString("name");
                Uri attachedVolume = attachVolume(name);
                if (this.mMediaScannerVolume != null && this.mMediaScannerVolume.equals(name)) {
                    DatabaseHelper dbhelper = getDatabaseForUri(attachedVolume);
                    if (dbhelper == null) {
                        Log.e(TAG, "no database for attached volume " + attachedVolume);
                        return attachedVolume;
                    }
                    dbhelper.mScanStartTime = SystemClock.currentTimeMicro();
                    return attachedVolume;
                }
                return attachedVolume;
            case 700:
                long rowId13 = insertFile(helper, uri, initialValues, 0, true, notifyRowIds);
                if (rowId13 > 0) {
                    newUri = MediaStore.Files.getContentUri(volumeName, rowId13);
                }
                break;
            case 702:
                long rowId14 = insertFile(helper, uri, initialValues, 0, false, notifyRowIds);
                if (rowId14 > 0) {
                    newUri = MediaStore.Files.getMtpObjectsUri(volumeName, rowId14);
                }
                break;
            case 705:
                synchronized (this.mMtpServiceConnection) {
                    if (this.mMtpService == null) {
                        Context context = getContext();
                        context.bindService(new Intent(context, (Class<?>) MtpService.class), this.mMtpServiceConnection, 1);
                    }
                    break;
                }
                break;
            default:
                throw new UnsupportedOperationException("Invalid URI " + uri);
        }
        if (path != null) {
            if (path.toLowerCase(Locale.US).endsWith("/.nomedia")) {
                processNewNoMediaPath(helper, db, path);
            }
        }
        return newUri;
    }

    private void processNewNoMediaPath(final DatabaseHelper helper, final SQLiteDatabase db, final String path) {
        final File nomedia = new File(path);
        if (nomedia.exists()) {
            hidePath(helper, db, path);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SystemClock.sleep(2000L);
                    if (nomedia.exists()) {
                        MediaProvider.this.hidePath(helper, db, path);
                    } else {
                        Log.w(MediaProvider.TAG, "does not exist: " + path, new Exception());
                    }
                }
            }).start();
        }
    }

    private void hidePath(DatabaseHelper helper, SQLiteDatabase db, String path) {
        MediaScanner.clearMediaPathCache(true, false);
        File nomedia = new File(path);
        String hiddenroot = nomedia.isDirectory() ? path : nomedia.getParent();
        ContentValues mediatype = new ContentValues();
        mediatype.put("media_type", (Integer) 0);
        int numrows = db.update("files", mediatype, "_data >= ? AND _data < ?", new String[]{hiddenroot + "/", hiddenroot + "0"});
        helper.mNumUpdates += numrows;
        ContentResolver res = getContext().getContentResolver();
        res.notifyChange(Uri.parse("content://media/"), null);
    }

    private void processRemovedNoMediaPath(String path) {
        DatabaseHelper helper;
        MediaScanner.clearMediaPathCache(false, true);
        if (path.startsWith(mExternalStoragePaths[0])) {
            helper = getDatabaseForUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        } else {
            helper = getDatabaseForUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        new ScannerClient(getContext(), db, path);
    }

    private static final class ScannerClient implements MediaScannerConnection.MediaScannerConnectionClient {
        SQLiteDatabase mDb;
        String mPath;
        MediaScannerConnection mScannerConnection;

        public ScannerClient(Context context, SQLiteDatabase db, String path) {
            this.mPath = null;
            this.mDb = db;
            this.mPath = path;
            this.mScannerConnection = new MediaScannerConnection(context, this);
            this.mScannerConnection.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            Cursor c = this.mDb.query("files", MediaProvider.openFileColumns, "_data >= ? AND _data < ?", new String[]{this.mPath + "/", this.mPath + "0"}, null, null, null);
            while (c.moveToNext()) {
                try {
                    String d = c.getString(0);
                    File f = new File(d);
                    if (f.isFile()) {
                        this.mScannerConnection.scanFile(d, null);
                    }
                } finally {
                    IoUtils.closeQuietly(c);
                }
            }
            this.mScannerConnection.disconnect();
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        DatabaseHelper ihelper = getDatabaseForUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
        DatabaseHelper ehelper = getDatabaseForUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        SQLiteDatabase idb = ihelper.getWritableDatabase();
        idb.beginTransaction();
        SQLiteDatabase edb = null;
        if (ehelper != null) {
            edb = ehelper.getWritableDatabase();
            edb.beginTransaction();
        }
        try {
            ContentProviderResult[] result = super.applyBatch(operations);
            idb.setTransactionSuccessful();
            if (edb != null) {
                edb.setTransactionSuccessful();
            }
            ContentResolver res = getContext().getContentResolver();
            res.notifyChange(Uri.parse("content://media/"), null);
            return result;
        } finally {
            idb.endTransaction();
            if (edb != null) {
                edb.endTransaction();
            }
        }
    }

    private MediaThumbRequest requestMediaThumbnail(String path, Uri uri, int priority, long magic) throws Throwable {
        MediaThumbRequest req;
        synchronized (this.mMediaThumbQueue) {
            try {
                try {
                    req = new MediaThumbRequest(getContext().getContentResolver(), path, uri, priority, magic);
                    try {
                        this.mMediaThumbQueue.add(req);
                        Message msg = this.mThumbHandler.obtainMessage(2);
                        msg.sendToTarget();
                    } catch (Throwable th) {
                        t = th;
                        Log.w(TAG, t);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                t = th3;
                req = null;
            }
            return req;
        }
    }

    private String generateFileName(boolean internal, String preferredExtension, String directoryName) {
        String name = String.valueOf(System.currentTimeMillis());
        if (internal) {
            throw new UnsupportedOperationException("Writing to internal storage is not supported.");
        }
        return mExternalStoragePaths[0] + "/" + directoryName + "/" + name + preferredExtension;
    }

    private boolean ensureFileExists(Uri uri, String path) {
        File file = new File(path);
        if (file.exists()) {
            return true;
        }
        try {
            checkAccess(uri, file, 939524096);
            int secondSlash = path.indexOf(47, 1);
            if (secondSlash < 1) {
                return false;
            }
            String directoryPath = path.substring(0, secondSlash);
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                return false;
            }
            file.getParentFile().mkdirs();
            try {
                return file.createNewFile();
            } catch (IOException ioe) {
                Log.e(TAG, "File creation failed", ioe);
                return false;
            }
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    private static final class GetTableAndWhereOutParameter {
        public String table;
        public String where;

        private GetTableAndWhereOutParameter() {
        }
    }

    private void getTableAndWhere(Uri uri, int match, String userWhere, GetTableAndWhereOutParameter out) {
        String where = null;
        switch (match) {
            case 1:
                out.table = "files";
                where = "media_type=1";
                if (!TextUtils.isEmpty(userWhere)) {
                    if (!TextUtils.isEmpty(where)) {
                        out.where = where + " AND (" + userWhere + ")";
                        return;
                    } else {
                        out.where = userWhere;
                        return;
                    }
                }
                out.where = where;
                return;
            case 2:
                out.table = "files";
                where = "_id = " + uri.getPathSegments().get(3);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 4:
                where = "_id=" + uri.getPathSegments().get(3);
            case 3:
                out.table = "thumbnails";
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 100:
                out.table = "files";
                where = "media_type=2";
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 101:
                out.table = "files";
                where = "_id=" + uri.getPathSegments().get(3);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 102:
                out.table = "audio_genres";
                where = "audio_id=" + uri.getPathSegments().get(3);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 103:
                out.table = "audio_genres";
                where = "audio_id=" + uri.getPathSegments().get(3) + " AND genre_id=" + uri.getPathSegments().get(5);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 104:
                out.table = "audio_playlists";
                where = "audio_id=" + uri.getPathSegments().get(3);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 105:
                out.table = "audio_playlists";
                where = "audio_id=" + uri.getPathSegments().get(3) + " AND playlists_id=" + uri.getPathSegments().get(5);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 106:
                out.table = "audio_genres";
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 107:
                out.table = "audio_genres";
                where = "_id=" + uri.getPathSegments().get(3);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 108:
                out.table = "audio_genres";
                where = "genre_id=" + uri.getPathSegments().get(3);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 110:
                out.table = "files";
                where = "media_type=4";
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 111:
                out.table = "files";
                where = "_id=" + uri.getPathSegments().get(3);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 112:
                out.table = "audio_playlists_map";
                where = "playlist_id=" + uri.getPathSegments().get(3);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 113:
                out.table = "audio_playlists_map";
                where = "playlist_id=" + uri.getPathSegments().get(3) + " AND _id=" + uri.getPathSegments().get(5);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 120:
                out.table = "album_art";
                where = "album_id=" + uri.getPathSegments().get(3);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 200:
                out.table = "files";
                where = "media_type=3";
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 201:
                out.table = "files";
                where = "_id=" + uri.getPathSegments().get(3);
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 203:
                where = "_id=" + uri.getPathSegments().get(3);
            case 202:
                out.table = "videothumbnails";
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            case 701:
            case 703:
                where = "_id=" + uri.getPathSegments().get(2);
            case 700:
            case 702:
                out.table = "files";
                if (!TextUtils.isEmpty(userWhere)) {
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown or unsupported URL: " + uri.toString());
        }
    }

    @Override
    public int delete(Uri uri, String userWhere, String[] whereArgs) {
        Cursor cc;
        int count;
        String deleteparam;
        int count2;
        Uri uri2 = safeUncanonicalize(uri);
        int match = URI_MATCHER.match(uri2);
        if (match == 500) {
            if (this.mMediaScannerVolume == null) {
                return 0;
            }
            DatabaseHelper database = getDatabaseForUri(Uri.parse("content://media/" + this.mMediaScannerVolume + "/audio"));
            if (database == null) {
                Log.w(TAG, "no database for scanned volume " + this.mMediaScannerVolume);
            } else {
                database.mScanStopTime = SystemClock.currentTimeMicro();
                String msg = dump(database, false);
                logToDb(database.getWritableDatabase(), msg);
            }
            this.mMediaScannerVolume = null;
            return 1;
        }
        if (match == 301) {
            detachVolume(uri2);
            return 1;
        }
        if (match == 705) {
            synchronized (this.mMtpServiceConnection) {
                if (this.mMtpService != null) {
                    getContext().unbindService(this.mMtpServiceConnection);
                    count2 = 1;
                    this.mMtpService = null;
                } else {
                    count2 = 0;
                }
            }
            return count2;
        }
        String volumeName = getVolumeName(uri2);
        DatabaseHelper database2 = getDatabaseForUri(uri2);
        if (database2 == null) {
            throw new UnsupportedOperationException("Unknown URI: " + uri2 + " match: " + match);
        }
        database2.mNumDeletes++;
        SQLiteDatabase db = database2.getWritableDatabase();
        synchronized (sGetTableAndWhereParam) {
            getTableAndWhere(uri2, match, userWhere, sGetTableAndWhereParam);
            if (sGetTableAndWhereParam.table.equals("files") && ((deleteparam = uri2.getQueryParameter("deletedata")) == null || !deleteparam.equals("false"))) {
                database2.mNumQueries++;
                cc = db.query(sGetTableAndWhereParam.table, sMediaTypeDataId, sGetTableAndWhereParam.where, whereArgs, null, null, null);
                String[] idvalue = {""};
                String[] playlistvalues = {"", ""};
                while (cc.moveToNext()) {
                    try {
                        int mediaType = cc.getInt(0);
                        String data = cc.getString(1);
                        long id = cc.getLong(2);
                        if (mediaType == 1) {
                            deleteIfAllowed(uri2, data);
                            MediaDocumentsProvider.onMediaStoreDelete(getContext(), volumeName, 1, id);
                            idvalue[0] = String.valueOf(id);
                            database2.mNumQueries++;
                            cc = db.query("thumbnails", sDataOnlyColumn, "image_id=?", idvalue, null, null, null);
                            while (cc.moveToNext()) {
                                deleteIfAllowed(uri2, cc.getString(0));
                            }
                            database2.mNumDeletes++;
                            db.delete("thumbnails", "image_id=?", idvalue);
                            IoUtils.closeQuietly(cc);
                        } else if (mediaType == 3) {
                            deleteIfAllowed(uri2, data);
                            MediaDocumentsProvider.onMediaStoreDelete(getContext(), volumeName, 3, id);
                        } else if (mediaType == 2) {
                            if (database2.mInternal) {
                                continue;
                            } else {
                                MediaDocumentsProvider.onMediaStoreDelete(getContext(), volumeName, 2, id);
                                idvalue[0] = String.valueOf(id);
                                database2.mNumDeletes += 2;
                                db.delete("audio_genres_map", "audio_id=?", idvalue);
                                cc = db.query("audio_playlists_map", sPlaylistIdPlayOrder, "audio_id=?", idvalue, null, null, null);
                                while (cc.moveToNext()) {
                                    playlistvalues[0] = "" + cc.getLong(0);
                                    playlistvalues[1] = "" + cc.getInt(1);
                                    database2.mNumUpdates++;
                                    db.execSQL("UPDATE audio_playlists_map SET play_order=play_order-1 WHERE playlist_id=? AND play_order>?", playlistvalues);
                                }
                                db.delete("audio_playlists_map", "audio_id=?", idvalue);
                            }
                        } else if (mediaType == 4) {
                        }
                    } finally {
                    }
                }
                switch (match) {
                    case 3:
                    case 4:
                    case 202:
                    case 203:
                        break;
                    case 108:
                        break;
                    case 702:
                    case 703:
                        break;
                }
            } else {
                switch (match) {
                    case 3:
                    case 4:
                    case 202:
                    case 203:
                        cc = db.query(sGetTableAndWhereParam.table, sDataOnlyColumn, sGetTableAndWhereParam.where, whereArgs, null, null, null);
                        if (cc != null) {
                            while (cc.moveToNext()) {
                                try {
                                    deleteIfAllowed(uri2, cc.getString(0));
                                } finally {
                                }
                            }
                        }
                        database2.mNumDeletes++;
                        count = db.delete(sGetTableAndWhereParam.table, sGetTableAndWhereParam.where, whereArgs);
                        Uri notifyUri = Uri.parse("content://media/" + volumeName);
                        getContext().getContentResolver().notifyChange(notifyUri, null);
                        break;
                    case 108:
                        database2.mNumDeletes++;
                        count = db.delete("audio_genres_map", sGetTableAndWhereParam.where, whereArgs);
                        Uri notifyUri2 = Uri.parse("content://media/" + volumeName);
                        getContext().getContentResolver().notifyChange(notifyUri2, null);
                        break;
                    case 702:
                    case 703:
                        try {
                            this.mDisableMtpObjectCallbacks = true;
                            database2.mNumDeletes++;
                            count = db.delete("files", sGetTableAndWhereParam.where, whereArgs);
                            this.mDisableMtpObjectCallbacks = false;
                            Uri notifyUri22 = Uri.parse("content://media/" + volumeName);
                            getContext().getContentResolver().notifyChange(notifyUri22, null);
                        } catch (Throwable th) {
                            this.mDisableMtpObjectCallbacks = false;
                            throw th;
                        }
                        break;
                    default:
                        database2.mNumDeletes++;
                        count = db.delete(sGetTableAndWhereParam.table, sGetTableAndWhereParam.where, whereArgs);
                        Uri notifyUri222 = Uri.parse("content://media/" + volumeName);
                        getContext().getContentResolver().notifyChange(notifyUri222, null);
                        break;
                }
            }
        }
        return count;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if ("unhide".equals(method)) {
            processRemovedNoMediaPath(arg);
            return null;
        }
        throw new UnsupportedOperationException("Unsupported call: " + method);
    }

    @Override
    public int update(Uri uri, ContentValues initialValues, String userWhere, String[] whereArgs) {
        Cursor c;
        int count;
        String so;
        String so2;
        long albumRowId;
        long artistRowId;
        Uri uri2 = safeUncanonicalize(uri);
        int match = URI_MATCHER.match(uri2);
        DatabaseHelper helper = getDatabaseForUri(uri2);
        if (helper == null) {
            throw new UnsupportedOperationException("Unknown URI: " + uri2);
        }
        helper.mNumUpdates++;
        SQLiteDatabase db = helper.getWritableDatabase();
        String genre = null;
        if (initialValues != null) {
            genre = initialValues.getAsString("genre");
            initialValues.remove("genre");
        }
        synchronized (sGetTableAndWhereParam) {
            getTableAndWhere(uri2, match, userWhere, sGetTableAndWhereParam);
            if ((match == 702 || match == 703) && initialValues != null && initialValues.size() == 1) {
                String oldPath = null;
                String newPath = initialValues.getAsString("_data");
                this.mDirectoryCache.remove(newPath);
                File f = new File(newPath);
                if (newPath != null && f.isDirectory()) {
                    helper.mNumQueries++;
                    c = db.query(sGetTableAndWhereParam.table, PATH_PROJECTION, userWhere, whereArgs, null, null, null);
                    if (c != null) {
                        try {
                            if (c.moveToNext()) {
                                oldPath = c.getString(1);
                            }
                        } finally {
                        }
                    }
                    if (oldPath != null) {
                        this.mDirectoryCache.remove(oldPath);
                        helper.mNumUpdates++;
                        count = db.update(sGetTableAndWhereParam.table, initialValues, sGetTableAndWhereParam.where, whereArgs);
                        if (count > 0) {
                            Object[] bindArgs = {newPath, Integer.valueOf(oldPath.length() + 1), oldPath + "/", oldPath + "0", f.getName(), Integer.valueOf(f.toString().toLowerCase().hashCode())};
                            helper.mNumUpdates++;
                            db.execSQL("UPDATE files SET _data=?1||SUBSTR(_data, ?2),bucket_display_name=?5,bucket_id=?6 WHERE _data >= ?3 AND _data < ?4;", bindArgs);
                        }
                        if (count > 0 && !db.inTransaction()) {
                            getContext().getContentResolver().notifyChange(uri2, null);
                        }
                        if (f.getName().startsWith(".")) {
                            processNewNoMediaPath(helper, db, newPath);
                        }
                    }
                } else if (newPath.toLowerCase(Locale.US).endsWith("/.nomedia")) {
                    processNewNoMediaPath(helper, db, newPath);
                }
            }
            switch (match) {
                case 1:
                case 2:
                case 200:
                case 201:
                    ContentValues values = new ContentValues(initialValues);
                    values.remove("bucket_id");
                    values.remove("bucket_display_name");
                    String data = values.getAsString("_data");
                    if (data != null) {
                        computeBucketValues(data, values);
                    }
                    computeTakenTime(values);
                    helper.mNumUpdates++;
                    count = db.update(sGetTableAndWhereParam.table, values, sGetTableAndWhereParam.where, whereArgs);
                    if (count > 0 && values.getAsString("_data") != null) {
                        helper.mNumQueries++;
                        c = db.query(sGetTableAndWhereParam.table, READY_FLAG_PROJECTION, sGetTableAndWhereParam.where, whereArgs, null, null, null);
                        if (c != null) {
                            while (c.moveToNext()) {
                                try {
                                    long magic = c.getLong(2);
                                    if (magic == 0) {
                                        requestMediaThumbnail(c.getString(1), uri2, 10, 0L);
                                    }
                                } finally {
                                }
                                break;
                            }
                            IoUtils.closeQuietly(c);
                        }
                    }
                    if (count > 0 && !db.inTransaction()) {
                        getContext().getContentResolver().notifyChange(uri2, null);
                    }
                    break;
                case 100:
                case 101:
                    ContentValues values2 = new ContentValues(initialValues);
                    String albumartist = values2.getAsString("album_artist");
                    String compilation = values2.getAsString("compilation");
                    values2.remove("compilation");
                    String artist = values2.getAsString("artist");
                    values2.remove("artist");
                    if (artist != null) {
                        HashMap<String, Long> artistCache = helper.mArtistCache;
                        synchronized (artistCache) {
                            Long temp = artistCache.get(artist);
                            if (temp == null) {
                                artistRowId = getKeyIdForName(helper, db, "artists", "artist_key", "artist", artist, artist, null, 0, null, artistCache, uri2);
                            } else {
                                artistRowId = temp.longValue();
                            }
                            break;
                        }
                        values2.put("artist_id", Integer.toString((int) artistRowId));
                        so = values2.getAsString("album");
                        values2.remove("album");
                        if (so != null) {
                            String path = values2.getAsString("_data");
                            int albumHash = 0;
                            if (albumartist != null) {
                                albumHash = albumartist.hashCode();
                            } else {
                                if (compilation == null || !compilation.equals("1")) {
                                    if (path == null) {
                                        if (match == 100) {
                                            Log.w(TAG, "Possible multi row album name update without path could give wrong album key");
                                        } else {
                                            c = query(uri2, new String[]{"_data"}, null, null, null);
                                            if (c != null) {
                                                try {
                                                    int numrows = c.getCount();
                                                    if (numrows == 1) {
                                                        c.moveToFirst();
                                                        path = c.getString(0);
                                                    } else {
                                                        Log.e(TAG, "" + numrows + " rows for " + uri2);
                                                    }
                                                } finally {
                                                }
                                            }
                                        }
                                    }
                                    if (path != null) {
                                        albumHash = path.substring(0, path.lastIndexOf(47)).hashCode();
                                    }
                                }
                                if (count > 0) {
                                    getContext().getContentResolver().notifyChange(uri2, null);
                                }
                            }
                            String s = so.toString();
                            HashMap<String, Long> albumCache = helper.mAlbumCache;
                            synchronized (albumCache) {
                                String cacheName = s + albumHash;
                                Long temp2 = albumCache.get(cacheName);
                                if (temp2 == null) {
                                    albumRowId = getKeyIdForName(helper, db, "albums", "album_key", "album", s, cacheName, path, albumHash, artist, albumCache, uri2);
                                } else {
                                    albumRowId = temp2.longValue();
                                }
                                break;
                            }
                            values2.put("album_id", Integer.toString((int) albumRowId));
                        }
                        values2.remove("title_key");
                        so2 = values2.getAsString("title");
                        if (so2 != null) {
                            String s2 = so2.toString();
                            values2.put("title_key", MediaStore.Audio.keyFor(s2));
                            values2.remove("title");
                            values2.put("title", s2.trim());
                        }
                        helper.mNumUpdates++;
                        count = db.update(sGetTableAndWhereParam.table, values2, sGetTableAndWhereParam.where, whereArgs);
                        if (genre != null) {
                            if (count == 1 && match == 101) {
                                long rowId = Long.parseLong(uri2.getPathSegments().get(3));
                                updateGenre(rowId, genre);
                            } else {
                                Log.w(TAG, "ignoring genre in update: count = " + count + " match = " + match);
                            }
                        }
                        if (count > 0) {
                        }
                        break;
                    } else {
                        so = values2.getAsString("album");
                        values2.remove("album");
                        if (so != null) {
                        }
                        values2.remove("title_key");
                        so2 = values2.getAsString("title");
                        if (so2 != null) {
                        }
                        helper.mNumUpdates++;
                        count = db.update(sGetTableAndWhereParam.table, values2, sGetTableAndWhereParam.where, whereArgs);
                        if (genre != null) {
                        }
                        if (count > 0) {
                        }
                    }
                    break;
                case 113:
                    String moveit = uri2.getQueryParameter("move");
                    if (moveit != null) {
                        if (initialValues.containsKey("play_order")) {
                            int newpos = initialValues.getAsInteger("play_order").intValue();
                            List<String> segments = uri2.getPathSegments();
                            long playlist = Long.valueOf(segments.get(3)).longValue();
                            int oldpos = Integer.valueOf(segments.get(5)).intValue();
                            count = movePlaylistEntry(helper, db, playlist, oldpos, newpos);
                        } else {
                            throw new IllegalArgumentException("Need to specify play_order when using 'move' parameter");
                        }
                        break;
                    }
                default:
                    helper.mNumUpdates++;
                    count = db.update(sGetTableAndWhereParam.table, initialValues, sGetTableAndWhereParam.where, whereArgs);
                    if (count > 0) {
                    }
                    break;
            }
        }
        return count;
    }

    private int movePlaylistEntry(DatabaseHelper helper, SQLiteDatabase db, long playlist, int from, int to) {
        int numlines;
        if (from == to) {
            return 0;
        }
        db.beginTransaction();
        Cursor c = null;
        try {
            helper.mNumUpdates += 3;
            Cursor c2 = db.query("audio_playlists_map", new String[]{"play_order"}, "playlist_id=?", new String[]{"" + playlist}, null, null, "play_order", from + ",1");
            c2.moveToFirst();
            int from_play_order = c2.getInt(0);
            IoUtils.closeQuietly(c2);
            c = db.query("audio_playlists_map", new String[]{"play_order"}, "playlist_id=?", new String[]{"" + playlist}, null, null, "play_order", to + ",1");
            c.moveToFirst();
            int to_play_order = c.getInt(0);
            db.execSQL("UPDATE audio_playlists_map SET play_order=-1 WHERE play_order=" + from_play_order + " AND playlist_id=" + playlist);
            if (from < to) {
                db.execSQL("UPDATE audio_playlists_map SET play_order=play_order-1 WHERE play_order<=" + to_play_order + " AND play_order>" + from_play_order + " AND playlist_id=" + playlist);
                numlines = (to - from) + 1;
            } else {
                db.execSQL("UPDATE audio_playlists_map SET play_order=play_order+1 WHERE play_order>=" + to_play_order + " AND play_order<" + from_play_order + " AND playlist_id=" + playlist);
                numlines = (from - to) + 1;
            }
            db.execSQL("UPDATE audio_playlists_map SET play_order=" + to_play_order + " WHERE play_order=-1 AND playlist_id=" + playlist);
            db.setTransactionSuccessful();
            db.endTransaction();
            IoUtils.closeQuietly(c);
            Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI.buildUpon().appendEncodedPath(String.valueOf(playlist)).build();
            getContext().getContentResolver().notifyChange(uri, null);
            return numlines;
        } catch (Throwable th) {
            db.endTransaction();
            IoUtils.closeQuietly(c);
            throw th;
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Cursor c;
        Uri uri2 = safeUncanonicalize(uri);
        ParcelFileDescriptor pfd = null;
        if (URI_MATCHER.match(uri2) == 121) {
            DatabaseHelper database = getDatabaseForUri(uri2);
            if (database == null) {
                throw new IllegalStateException("Couldn't open database for " + uri2);
            }
            SQLiteDatabase db = database.getReadableDatabase();
            if (db == null) {
                throw new IllegalStateException("Couldn't open database for " + uri2);
            }
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            int songid = Integer.parseInt(uri2.getPathSegments().get(3));
            qb.setTables("audio_meta");
            qb.appendWhere("_id=" + songid);
            c = qb.query(db, new String[]{"_data", "album_id"}, null, null, null, null, null);
            try {
                if (c.moveToFirst()) {
                    String audiopath = c.getString(0);
                    int albumid = c.getInt(1);
                    Uri newUri = ContentUris.withAppendedId(ALBUMART_URI, albumid);
                    try {
                        pfd = openFileAndEnforcePathPermissionsHelper(newUri, mode);
                    } catch (FileNotFoundException e) {
                        pfd = getThumb(database, db, audiopath, albumid, null);
                    }
                }
                IoUtils.closeQuietly(c);
                return pfd;
            } finally {
            }
        }
        try {
            pfd = openFileAndEnforcePathPermissionsHelper(uri2, mode);
        } catch (FileNotFoundException ex) {
            if (mode.contains("w")) {
                throw ex;
            }
            if (URI_MATCHER.match(uri2) == 120) {
                DatabaseHelper database2 = getDatabaseForUri(uri2);
                if (database2 == null) {
                    throw ex;
                }
                SQLiteDatabase db2 = database2.getReadableDatabase();
                if (db2 == null) {
                    throw new IllegalStateException("Couldn't open database for " + uri2);
                }
                SQLiteQueryBuilder qb2 = new SQLiteQueryBuilder();
                int albumid2 = Integer.parseInt(uri2.getPathSegments().get(3));
                qb2.setTables("audio_meta");
                qb2.appendWhere("album_id=" + albumid2);
                c = qb2.query(db2, new String[]{"_data"}, null, null, null, null, "track");
                try {
                    if (c.moveToFirst()) {
                        String audiopath2 = c.getString(0);
                        pfd = getThumb(database2, db2, audiopath2, albumid2, uri2);
                    }
                } finally {
                }
            }
            if (pfd == null) {
                throw ex;
            }
        }
        return pfd;
    }

    private File queryForDataFile(Uri uri) throws FileNotFoundException {
        Cursor cursor = query(uri, new String[]{"_data"}, null, null, null);
        if (cursor == null) {
            throw new FileNotFoundException("Missing cursor for " + uri);
        }
        try {
            switch (cursor.getCount()) {
                case 0:
                    throw new FileNotFoundException("No entry for " + uri);
                case 1:
                    if (cursor.moveToFirst()) {
                        return new File(cursor.getString(0));
                    }
                    throw new FileNotFoundException("Unable to read entry for " + uri);
                default:
                    throw new FileNotFoundException("Multiple items at " + uri);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }

    private ParcelFileDescriptor openFileAndEnforcePathPermissionsHelper(Uri uri, String mode) throws FileNotFoundException {
        int modeBits = ParcelFileDescriptor.parseMode(mode);
        File file = queryForDataFile(uri);
        checkAccess(uri, file, modeBits);
        if (modeBits == 268435456) {
            file = Environment.maybeTranslateEmulatedPathToInternal(file);
        }
        return ParcelFileDescriptor.open(file, modeBits);
    }

    private void deleteIfAllowed(Uri uri, String path) {
        try {
            File file = new File(path);
            checkAccess(uri, file, 536870912);
            file.delete();
        } catch (Exception e) {
            Log.e(TAG, "Couldn't delete " + path);
        }
    }

    private void checkAccess(Uri uri, File file, int modeBits) throws FileNotFoundException {
        boolean isWrite = (536870912 & modeBits) != 0;
        try {
            String path = file.getCanonicalPath();
            Context c = getContext();
            boolean readGranted = false;
            boolean writeGranted = false;
            if (isWrite) {
                writeGranted = c.checkCallingOrSelfUriPermission(uri, 2) == 0;
            } else {
                readGranted = c.checkCallingOrSelfUriPermission(uri, 1) == 0;
            }
            if (path.startsWith(sExternalPath) || path.startsWith(sLegacyPath)) {
                if (isWrite) {
                    if (!writeGranted) {
                        c.enforceCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE", "External path: " + path);
                        return;
                    }
                    return;
                } else {
                    if (!readGranted) {
                        c.enforceCallingOrSelfPermission("android.permission.READ_EXTERNAL_STORAGE", "External path: " + path);
                        return;
                    }
                    return;
                }
            }
            if (path.startsWith(sCachePath)) {
                if ((isWrite && !writeGranted) || !readGranted) {
                    c.enforceCallingOrSelfPermission("android.permission.ACCESS_CACHE_FILESYSTEM", "Cache path: " + path);
                    return;
                }
                return;
            }
            if (isSecondaryExternalPath(path)) {
                if (!readGranted && c.checkCallingOrSelfPermission("android.permission.WRITE_MEDIA_STORAGE") == -1) {
                    c.enforceCallingOrSelfPermission("android.permission.READ_EXTERNAL_STORAGE", "External path: " + path);
                }
                if (isWrite && c.checkCallingOrSelfUriPermission(uri, 2) != 0) {
                    c.enforceCallingOrSelfPermission("android.permission.WRITE_MEDIA_STORAGE", "External path: " + path);
                    return;
                }
                return;
            }
            if (isWrite) {
                throw new FileNotFoundException("Can't access " + file);
            }
            checkWorldReadAccess(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to resolve canonical path for " + file, e);
        }
    }

    private boolean isSecondaryExternalPath(String path) {
        for (int i = mExternalStoragePaths.length - 1; i >= 0; i--) {
            if (path.startsWith(mExternalStoragePaths[i])) {
                return true;
            }
        }
        return false;
    }

    private void checkWorldReadAccess(String path) throws FileNotFoundException {
        try {
            StructStat stat = Os.stat(path);
            int accessBits = OsConstants.S_IROTH;
            if (OsConstants.S_ISREG(stat.st_mode) && (stat.st_mode & accessBits) == accessBits) {
                checkLeadingPathComponentsWorldExecutable(path);
                return;
            }
        } catch (ErrnoException e) {
        }
        throw new FileNotFoundException("Can't access " + path);
    }

    private void checkLeadingPathComponentsWorldExecutable(String filePath) throws FileNotFoundException {
        int accessBits = OsConstants.S_IXOTH;
        for (File parent = new File(filePath).getParentFile(); parent != null; parent = parent.getParentFile()) {
            if (!parent.exists()) {
                throw new FileNotFoundException("access denied");
            }
            try {
                StructStat stat = Os.stat(parent.getPath());
                if ((stat.st_mode & accessBits) != accessBits) {
                    throw new FileNotFoundException("Can't access " + filePath);
                }
            } catch (ErrnoException e) {
                throw new FileNotFoundException("Can't access " + filePath);
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

    private void makeThumbAsync(DatabaseHelper helper, SQLiteDatabase db, String path, long album_id) {
        synchronized (this.mPendingThumbs) {
            if (!this.mPendingThumbs.contains(path)) {
                this.mPendingThumbs.add(path);
                ThumbData d = new ThumbData();
                d.helper = helper;
                d.db = db;
                d.path = path;
                d.album_id = album_id;
                d.albumart_uri = ContentUris.withAppendedId(this.mAlbumArtBaseUri, album_id);
                synchronized (this.mThumbRequestStack) {
                    this.mThumbRequestStack.push(d);
                }
                Message msg = this.mThumbHandler.obtainMessage(1);
                msg.sendToTarget();
            }
        }
    }

    private static boolean isRootStorageDir(String artPath) {
        for (int i = 0; i < mExternalStoragePaths.length; i++) {
            if (mExternalStoragePaths[i] != null && artPath.equalsIgnoreCase(mExternalStoragePaths[i])) {
                return true;
            }
        }
        return false;
    }

    private static byte[] getCompressedAlbumArt(Context context, String path) throws Throwable {
        byte[] bArr;
        int lastSlash;
        FileInputStream stream;
        byte[] compressed = null;
        try {
            File f = new File(path);
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f, 268435456);
            MediaScanner scanner = new MediaScanner(context);
            compressed = scanner.extractAlbumArt(pfd.getFileDescriptor());
            pfd.close();
        } catch (IOException e) {
        }
        if (compressed != null || path == null || (lastSlash = path.lastIndexOf(47)) <= 0) {
            bArr = compressed;
        } else {
            String artPath = path.substring(0, lastSlash);
            String dwndir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            String bestmatch = null;
            synchronized (sFolderArtMap) {
                if (sFolderArtMap.containsKey(artPath)) {
                    bestmatch = sFolderArtMap.get(artPath);
                } else if (!isRootStorageDir(artPath) && !artPath.equalsIgnoreCase(dwndir)) {
                    File dir = new File(artPath);
                    String[] entrynames = dir.list();
                    if (entrynames == null) {
                        bArr = null;
                    } else {
                        bestmatch = null;
                        int matchlevel = 1000;
                        int i = entrynames.length - 1;
                        while (true) {
                            if (i < 0) {
                                break;
                            }
                            String entry = entrynames[i].toLowerCase();
                            if (entry.equals("albumart.jpg")) {
                                bestmatch = entrynames[i];
                                break;
                            }
                            if (entry.startsWith("albumart") && entry.endsWith("large.jpg") && matchlevel > 1) {
                                bestmatch = entrynames[i];
                                matchlevel = 1;
                            } else if (entry.contains("albumart") && entry.endsWith(".jpg") && matchlevel > 2) {
                                bestmatch = entrynames[i];
                                matchlevel = 2;
                            } else if (entry.endsWith(".jpg") && matchlevel > 3) {
                                bestmatch = entrynames[i];
                                matchlevel = 3;
                            } else if (entry.endsWith(".png") && matchlevel > 4) {
                                bestmatch = entrynames[i];
                                matchlevel = 4;
                            }
                            i--;
                        }
                        sFolderArtMap.put(artPath, bestmatch);
                    }
                }
                if (bestmatch != null) {
                    File file = new File(artPath, bestmatch);
                    if (file.exists()) {
                        FileInputStream stream2 = null;
                        try {
                            try {
                                compressed = new byte[(int) file.length()];
                                stream = new FileInputStream(file);
                            } catch (Throwable th) {
                                th = th;
                            }
                            try {
                                stream.read(compressed);
                                if (stream != null) {
                                    stream.close();
                                }
                            } catch (IOException e2) {
                                stream2 = stream;
                                compressed = null;
                                if (stream2 != null) {
                                    stream2.close();
                                }
                            } catch (OutOfMemoryError e3) {
                                ex = e3;
                                stream2 = stream;
                                Log.w(TAG, ex);
                                compressed = null;
                                if (stream2 != null) {
                                    stream2.close();
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                stream2 = stream;
                                if (stream2 != null) {
                                    stream2.close();
                                }
                                throw th;
                            }
                        } catch (IOException e4) {
                        } catch (OutOfMemoryError e5) {
                            ex = e5;
                        }
                    }
                }
                bArr = compressed;
            }
        }
        return bArr;
    }

    Uri getAlbumArtOutputUri(DatabaseHelper helper, SQLiteDatabase db, long album_id, Uri albumart_uri) {
        Uri out = null;
        if (albumart_uri != null) {
            Cursor c = query(albumart_uri, new String[]{"_data"}, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        String albumart_path = c.getString(0);
                        if (ensureFileExists(albumart_uri, albumart_path)) {
                            out = albumart_uri;
                        }
                    } else {
                        albumart_uri = null;
                    }
                } finally {
                    IoUtils.closeQuietly(c);
                }
            }
        }
        if (albumart_uri == null) {
            ContentValues initialValues = new ContentValues();
            initialValues.put("album_id", Long.valueOf(album_id));
            try {
                ContentValues values = ensureFile(false, initialValues, "", "Android/data/com.android.providers.media/albumthumbs");
                helper.mNumInserts++;
                long rowId = db.insert("album_art", "_data", values);
                if (rowId > 0) {
                    out = ContentUris.withAppendedId(ALBUMART_URI, rowId);
                    String albumart_path2 = values.getAsString("_data");
                    ensureFileExists(out, albumart_path2);
                    return out;
                }
                return out;
            } catch (IllegalStateException e) {
                Log.e(TAG, "error creating album thumb file");
                return out;
            }
        }
        return out;
    }

    private void writeAlbumArt(boolean need_to_recompress, Uri out, byte[] compressed, Bitmap bm) throws IOException {
        OutputStream outstream = null;
        try {
            outstream = getContext().getContentResolver().openOutputStream(out);
            if (!need_to_recompress) {
                outstream.write(compressed);
            } else if (!bm.compress(Bitmap.CompressFormat.JPEG, 85, outstream)) {
                throw new IOException("failed to compress bitmap");
            }
        } finally {
            IoUtils.closeQuietly(outstream);
        }
    }

    private ParcelFileDescriptor getThumb(DatabaseHelper helper, SQLiteDatabase db, String path, long album_id, Uri albumart_uri) {
        ThumbData d = new ThumbData();
        d.helper = helper;
        d.db = db;
        d.path = path;
        d.album_id = album_id;
        d.albumart_uri = albumart_uri;
        return makeThumbInternal(d);
    }

    private ParcelFileDescriptor makeThumbInternal(ThumbData d) throws Throwable {
        BitmapFactory.Options opts;
        int maximumThumbSize;
        Bitmap nbm;
        byte[] compressed = getCompressedAlbumArt(getContext(), d.path);
        if (compressed == null) {
            return null;
        }
        Bitmap bm = null;
        boolean need_to_recompress = true;
        try {
            opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            opts.inSampleSize = 1;
            BitmapFactory.decodeByteArray(compressed, 0, compressed.length, opts);
            Resources r = getContext().getResources();
            maximumThumbSize = r.getDimensionPixelSize(R.dimen.maximum_thumb_size);
        } catch (Exception e) {
        }
        while (true) {
            if (opts.outHeight <= maximumThumbSize && opts.outWidth <= maximumThumbSize) {
                break;
            }
            opts.outHeight /= 2;
            opts.outWidth /= 2;
            opts.inSampleSize *= 2;
            if (!need_to_recompress && bm == null) {
                return null;
            }
            if (d.albumart_uri != null) {
                try {
                    return ParcelFileDescriptor.fromData(compressed, "albumthumb");
                } catch (IOException e2) {
                }
            } else {
                d.db.beginTransaction();
                Uri out = null;
                ParcelFileDescriptor pfd = null;
                try {
                    out = getAlbumArtOutputUri(d.helper, d.db, d.album_id, d.albumart_uri);
                    if (out != null) {
                        writeAlbumArt(need_to_recompress, out, compressed, bm);
                        getContext().getContentResolver().notifyChange(MEDIA_URI, null);
                        pfd = openFileHelper(out, "r");
                        d.db.setTransactionSuccessful();
                        d.db.endTransaction();
                        if (bm != null) {
                            bm.recycle();
                        }
                        if (pfd != null || out == null) {
                            return pfd;
                        }
                        getContext().getContentResolver().delete(out, null, null);
                        return pfd;
                    }
                    d.db.endTransaction();
                    if (bm != null) {
                        bm.recycle();
                    }
                    if (0 == 0 && out != null) {
                        getContext().getContentResolver().delete(out, null, null);
                    }
                } catch (IOException e3) {
                    d.db.endTransaction();
                    if (bm != null) {
                        bm.recycle();
                    }
                    if (pfd == null && out != null) {
                        getContext().getContentResolver().delete(out, null, null);
                    }
                } catch (UnsupportedOperationException e4) {
                    d.db.endTransaction();
                    if (bm != null) {
                        bm.recycle();
                    }
                    if (pfd == null && out != null) {
                        getContext().getContentResolver().delete(out, null, null);
                    }
                } catch (Throwable th) {
                    d.db.endTransaction();
                    if (bm != null) {
                        bm.recycle();
                    }
                    if (pfd == null && out != null) {
                        getContext().getContentResolver().delete(out, null, null);
                    }
                    throw th;
                }
            }
            return null;
        }
        if (opts.inSampleSize == 1) {
            need_to_recompress = false;
        } else {
            opts.inJustDecodeBounds = false;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            bm = BitmapFactory.decodeByteArray(compressed, 0, compressed.length, opts);
            if (bm != null && bm.getConfig() == null && (nbm = bm.copy(Bitmap.Config.RGB_565, false)) != null && nbm != bm) {
                bm.recycle();
                bm = nbm;
            }
        }
        if (!need_to_recompress) {
        }
        if (d.albumart_uri != null) {
        }
        return null;
    }

    private long getKeyIdForName(DatabaseHelper helper, SQLiteDatabase db, String table, String keyField, String nameField, String rawName, String cacheName, String path, int albumHash, String artist, HashMap<String, Long> cache, Uri srcuri) {
        long rowId;
        if (rawName == null || rawName.length() == 0) {
            rawName = "<unknown>";
        }
        String k = MediaStore.Audio.keyFor(rawName);
        if (k == null) {
            Log.e(TAG, "null key", new Exception());
            return -1L;
        }
        boolean isAlbum = table.equals("albums");
        boolean isUnknown = "<unknown>".equals(rawName);
        if (isAlbum) {
            k = k + albumHash;
            if (isUnknown) {
                k = k + artist;
            }
        }
        String[] selargs = {k};
        helper.mNumQueries++;
        Cursor c = db.query(table, null, keyField + "=?", selargs, null, null, null);
        try {
            switch (c.getCount()) {
                case 0:
                    ContentValues otherValues = new ContentValues();
                    otherValues.put(keyField, k);
                    otherValues.put(nameField, rawName);
                    helper.mNumInserts++;
                    rowId = db.insert(table, "duration", otherValues);
                    if (path != null && isAlbum && !isUnknown) {
                        makeThumbAsync(helper, db, path, rowId);
                    }
                    if (rowId > 0) {
                        String volume = srcuri.toString().substring(16, 24);
                        Uri uri = Uri.parse("content://media/" + volume + "/audio/" + table + "/" + rowId);
                        getContext().getContentResolver().notifyChange(uri, null);
                    }
                    break;
                case 1:
                    c.moveToFirst();
                    rowId = c.getLong(0);
                    String currentFancyName = c.getString(2);
                    String bestName = makeBestName(rawName, currentFancyName);
                    if (!bestName.equals(currentFancyName)) {
                        ContentValues newValues = new ContentValues();
                        newValues.put(nameField, bestName);
                        helper.mNumUpdates++;
                        db.update(table, newValues, "rowid=" + Integer.toString((int) rowId), null);
                        String volume2 = srcuri.toString().substring(16, 24);
                        Uri uri2 = Uri.parse("content://media/" + volume2 + "/audio/" + table + "/" + rowId);
                        getContext().getContentResolver().notifyChange(uri2, null);
                    }
                    break;
                default:
                    Log.e(TAG, "Multiple entries in table " + table + " for key " + k);
                    rowId = -1;
                    break;
            }
            if (cache != null && !isUnknown) {
                cache.put(cacheName, Long.valueOf(rowId));
                return rowId;
            }
            return rowId;
        } finally {
            IoUtils.closeQuietly(c);
        }
    }

    String makeBestName(String one, String two) {
        String name;
        if (one.length() > two.length() || one.toLowerCase().compareTo(two.toLowerCase()) > 0) {
            name = one;
        } else {
            name = two;
        }
        if (name.endsWith(", the") || name.endsWith(",the") || name.endsWith(", an") || name.endsWith(",an") || name.endsWith(", a") || name.endsWith(",a")) {
            String fix = name.substring(name.lastIndexOf(44) + 1);
            return fix.trim() + " " + name.substring(0, name.lastIndexOf(44));
        }
        return name;
    }

    private DatabaseHelper getDatabaseForUri(Uri uri) {
        synchronized (this.mDatabases) {
            if (uri.getPathSegments().size() >= 1) {
                return this.mDatabases.get(uri.getPathSegments().get(0));
            }
            return null;
        }
    }

    static boolean isMediaDatabaseName(String name) {
        if ("internal.db".equals(name) || "external.db".equals(name)) {
            return true;
        }
        return name.startsWith("external-") && name.endsWith(".db");
    }

    static boolean isInternalMediaDatabaseName(String name) {
        return "internal.db".equals(name);
    }

    private Uri attachVolume(String volume) {
        DatabaseHelper helper;
        if (Binder.getCallingPid() != Process.myPid()) {
            throw new SecurityException("Opening and closing databases not allowed.");
        }
        synchronized (this.mDatabases) {
            if (this.mDatabases.get(volume) != null) {
                return Uri.parse("content://media/" + volume);
            }
            Context context = getContext();
            if ("internal".equals(volume)) {
                helper = new DatabaseHelper(context, "internal.db", true, false, this.mObjectRemovedCallback);
            } else if ("external".equals(volume)) {
                if (Environment.isExternalStorageRemovable()) {
                    StorageVolume actualVolume = this.mStorageManager.getPrimaryVolume();
                    int volumeId = actualVolume.getFatVolumeId();
                    if (volumeId == -1) {
                        String state = Environment.getExternalStorageState();
                        if ("mounted".equals(state) || "mounted_ro".equals(state)) {
                            Log.e(TAG, "Can't obtain external volume ID even though it's mounted.");
                        } else {
                            Log.i(TAG, "External volume is not (yet) mounted, cannot attach.");
                        }
                        throw new IllegalArgumentException("Can't obtain external volume ID for " + volume + " volume.");
                    }
                    String dbName = "external-" + Integer.toHexString(volumeId) + ".db";
                    helper = new DatabaseHelper(context, dbName, false, false, this.mObjectRemovedCallback);
                    this.mVolumeId = volumeId;
                } else {
                    File dbFile = context.getDatabasePath("external.db");
                    if (!dbFile.exists()) {
                        File recentDbFile = null;
                        String[] arr$ = context.databaseList();
                        for (String database : arr$) {
                            if (database.startsWith("external-") && database.endsWith(".db")) {
                                File file = context.getDatabasePath(database);
                                if (recentDbFile == null) {
                                    recentDbFile = file;
                                } else if (file.lastModified() > recentDbFile.lastModified()) {
                                    context.deleteDatabase(recentDbFile.getName());
                                    recentDbFile = file;
                                } else {
                                    context.deleteDatabase(file.getName());
                                }
                            }
                        }
                        if (recentDbFile != null) {
                            if (recentDbFile.renameTo(dbFile)) {
                                Log.d(TAG, "renamed database " + recentDbFile.getName() + " to external.db");
                            } else {
                                Log.e(TAG, "Failed to rename database " + recentDbFile.getName() + " to external.db");
                                dbFile = recentDbFile;
                            }
                        }
                    }
                    helper = new DatabaseHelper(context, dbFile.getName(), false, false, this.mObjectRemovedCallback);
                }
            } else {
                throw new IllegalArgumentException("There is no volume named " + volume);
            }
            this.mDatabases.put(volume, helper);
            if (!helper.mInternal) {
                createDefaultFolders(helper, helper.getWritableDatabase());
                File[] files = new File(mExternalStoragePaths[0], "Android/data/com.android.providers.media/albumthumbs").listFiles();
                HashSet<String> fileSet = new HashSet<>();
                for (int i = 0; files != null && i < files.length; i++) {
                    fileSet.add(files[i].getPath());
                }
                Cursor cursor = query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{"album_art"}, null, null, null);
                while (cursor != null) {
                    try {
                        if (!cursor.moveToNext()) {
                            break;
                        }
                        fileSet.remove(cursor.getString(0));
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(cursor);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(cursor);
                for (String filename : fileSet) {
                    new File(filename).delete();
                }
            }
            return Uri.parse("content://media/" + volume);
        }
    }

    private void detachVolume(Uri uri) {
        if (Binder.getCallingPid() != Process.myPid()) {
            throw new SecurityException("Opening and closing databases not allowed.");
        }
        String volume = uri.getPathSegments().get(0);
        if ("internal".equals(volume)) {
            throw new UnsupportedOperationException("Deleting the internal volume is not allowed");
        }
        if (!"external".equals(volume)) {
            throw new IllegalArgumentException("There is no volume named " + volume);
        }
        synchronized (this.mDatabases) {
            DatabaseHelper database = this.mDatabases.get(volume);
            if (database != null) {
                try {
                    File file = new File(database.getReadableDatabase().getPath());
                    file.setLastModified(System.currentTimeMillis());
                } catch (Exception e) {
                    Log.e(TAG, "Can't touch database file", e);
                }
                this.mDatabases.remove(volume);
                database.close();
                getContext().getContentResolver().notifyChange(uri, null);
            }
        }
    }

    private static String getVolumeName(Uri uri) {
        List<String> segments = uri.getPathSegments();
        if (segments == null || segments.size() <= 0) {
            return null;
        }
        return segments.get(0);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        Collection<DatabaseHelper> foo = this.mDatabases.values();
        for (DatabaseHelper dbh : foo) {
            writer.println(dump(dbh, true));
        }
        writer.flush();
    }

    private String dump(DatabaseHelper dbh, boolean dumpDbLog) {
        StringBuilder s = new StringBuilder();
        s.append(dbh.mName);
        s.append(": ");
        SQLiteDatabase db = dbh.getReadableDatabase();
        if (db == null) {
            s.append("null");
        } else {
            s.append("version " + db.getVersion() + ", ");
            Cursor c = db.query("files", new String[]{"count(*)"}, null, null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        int num = c.getInt(0);
                        s.append(num + " rows, ");
                    } else {
                        s.append("couldn't get row count, ");
                    }
                    IoUtils.closeQuietly(c);
                    s.append(dbh.mNumInserts + " inserts, ");
                    s.append(dbh.mNumUpdates + " updates, ");
                    s.append(dbh.mNumDeletes + " deletes, ");
                    s.append(dbh.mNumQueries + " queries, ");
                    if (dbh.mScanStartTime != 0) {
                        s.append("scan started " + DateUtils.formatDateTime(getContext(), dbh.mScanStartTime / 1000, 524305));
                        long now = dbh.mScanStopTime;
                        if (now < dbh.mScanStartTime) {
                            now = SystemClock.currentTimeMicro();
                        }
                        s.append(" (" + DateUtils.formatElapsedTime((now - dbh.mScanStartTime) / 1000000) + ")");
                        if (dbh.mScanStopTime < dbh.mScanStartTime) {
                            if (this.mMediaScannerVolume != null && dbh.mName.startsWith(this.mMediaScannerVolume)) {
                                s.append(" (ongoing)");
                            } else {
                                s.append(" (scanning " + this.mMediaScannerVolume + ")");
                            }
                        }
                    }
                    if (dumpDbLog) {
                        c = db.query("log", new String[]{"time", "message"}, null, null, null, null, "rowid");
                        if (c != null) {
                            while (c.moveToNext()) {
                                try {
                                    String when = c.getString(0);
                                    String msg = c.getString(1);
                                    s.append("\n" + when + " : " + msg);
                                } finally {
                                }
                            }
                        }
                    }
                } finally {
                }
            }
        }
        return s.toString();
    }
}
