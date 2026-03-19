package android.mtp;

import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScanner;
import android.mtp.MtpStorageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import com.google.android.collect.Sets;
import dalvik.system.CloseGuard;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MtpDatabase implements AutoCloseable {
    private static final int[] AUDIO_PROPERTIES;
    private static final int[] DEVICE_PROPERTIES;
    private static final int[] FILE_PROPERTIES;
    private static final int[] IMAGE_PROPERTIES;
    private static final String NO_MEDIA = ".nomedia";
    private static final String PATH_WHERE = "_data=?";
    private static final int[] PLAYBACK_FORMATS;
    private static final String SD_RO_PATH = "/storage/";
    private static final String SD_RW_PATH = "/mnt/media_rw/";
    private static final int[] VIDEO_PROPERTIES;
    private int mBatteryLevel;
    private int mBatteryScale;
    private final Context mContext;
    private SharedPreferences mDeviceProperties;
    private int mDeviceType;
    private MtpStorageManager mManager;
    private final ContentProviderClient mMediaProvider;
    private final MediaScanner mMediaScanner;
    private long mNativeContext;
    private final Uri mObjectsUri;
    private MtpServer mServer;
    private final String mVolumeName;
    private static final String TAG = MtpDatabase.class.getSimpleName();
    private static final String[] ID_PROJECTION = {"_id"};
    private static final String[] PATH_PROJECTION = {"_data"};
    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final HashMap<String, MtpStorage> mStorageMap = new HashMap<>();
    private final HashMap<Integer, MtpPropertyGroup> mPropertyGroupsByProperty = new HashMap<>();
    private final HashMap<Integer, MtpPropertyGroup> mPropertyGroupsByFormat = new HashMap<>();
    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                MtpDatabase.this.mBatteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
                int intExtra = intent.getIntExtra("level", 0);
                if (intExtra != MtpDatabase.this.mBatteryLevel) {
                    MtpDatabase.this.mBatteryLevel = intExtra;
                    if (MtpDatabase.this.mServer != null) {
                        MtpDatabase.this.mServer.sendDevicePropertyChanged(MtpConstants.DEVICE_PROPERTY_BATTERY_LEVEL);
                    }
                }
            }
        }
    };

    private final native void native_finalize();

    private final native void native_setup();

    static {
        System.loadLibrary("media_jni");
        PLAYBACK_FORMATS = new int[]{12288, 12289, 12292, 12293, 12296, 12297, 12299, MtpConstants.FORMAT_EXIF_JPEG, MtpConstants.FORMAT_TIFF_EP, MtpConstants.FORMAT_BMP, MtpConstants.FORMAT_GIF, MtpConstants.FORMAT_JFIF, MtpConstants.FORMAT_PNG, MtpConstants.FORMAT_TIFF, MtpConstants.FORMAT_WMA, MtpConstants.FORMAT_OGG, MtpConstants.FORMAT_AAC, MtpConstants.FORMAT_MP4_CONTAINER, MtpConstants.FORMAT_MP2, MtpConstants.FORMAT_3GP_CONTAINER, MtpConstants.FORMAT_ABSTRACT_AV_PLAYLIST, MtpConstants.FORMAT_WPL_PLAYLIST, MtpConstants.FORMAT_M3U_PLAYLIST, MtpConstants.FORMAT_PLS_PLAYLIST, MtpConstants.FORMAT_XML_DOCUMENT, MtpConstants.FORMAT_FLAC, MtpConstants.FORMAT_DNG, MtpConstants.FORMAT_HEIF};
        FILE_PROPERTIES = new int[]{MtpConstants.PROPERTY_STORAGE_ID, MtpConstants.PROPERTY_OBJECT_FORMAT, MtpConstants.PROPERTY_PROTECTION_STATUS, MtpConstants.PROPERTY_OBJECT_SIZE, MtpConstants.PROPERTY_OBJECT_FILE_NAME, MtpConstants.PROPERTY_DATE_MODIFIED, MtpConstants.PROPERTY_PERSISTENT_UID, MtpConstants.PROPERTY_PARENT_OBJECT, MtpConstants.PROPERTY_NAME, MtpConstants.PROPERTY_DISPLAY_NAME, MtpConstants.PROPERTY_DATE_ADDED};
        AUDIO_PROPERTIES = new int[]{MtpConstants.PROPERTY_ARTIST, MtpConstants.PROPERTY_ALBUM_NAME, MtpConstants.PROPERTY_ALBUM_ARTIST, MtpConstants.PROPERTY_TRACK, MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE, MtpConstants.PROPERTY_DURATION, MtpConstants.PROPERTY_GENRE, MtpConstants.PROPERTY_COMPOSER, MtpConstants.PROPERTY_AUDIO_WAVE_CODEC, MtpConstants.PROPERTY_BITRATE_TYPE, MtpConstants.PROPERTY_AUDIO_BITRATE, MtpConstants.PROPERTY_NUMBER_OF_CHANNELS, MtpConstants.PROPERTY_SAMPLE_RATE};
        VIDEO_PROPERTIES = new int[]{MtpConstants.PROPERTY_ARTIST, MtpConstants.PROPERTY_ALBUM_NAME, MtpConstants.PROPERTY_DURATION, MtpConstants.PROPERTY_DESCRIPTION};
        IMAGE_PROPERTIES = new int[]{MtpConstants.PROPERTY_DESCRIPTION};
        DEVICE_PROPERTIES = new int[]{MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER, MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME, MtpConstants.DEVICE_PROPERTY_IMAGE_SIZE, MtpConstants.DEVICE_PROPERTY_BATTERY_LEVEL, MtpConstants.DEVICE_PROPERTY_PERCEIVED_DEVICE_TYPE};
    }

    private int[] getSupportedObjectProperties(int i) {
        switch (i) {
            case 12296:
            case 12297:
            case MtpConstants.FORMAT_WMA:
            case MtpConstants.FORMAT_OGG:
            case MtpConstants.FORMAT_AAC:
                return IntStream.concat(Arrays.stream(FILE_PROPERTIES), Arrays.stream(AUDIO_PROPERTIES)).toArray();
            case 12299:
            case MtpConstants.FORMAT_WMV:
            case MtpConstants.FORMAT_3GP_CONTAINER:
                return IntStream.concat(Arrays.stream(FILE_PROPERTIES), Arrays.stream(VIDEO_PROPERTIES)).toArray();
            case MtpConstants.FORMAT_EXIF_JPEG:
            case MtpConstants.FORMAT_BMP:
            case MtpConstants.FORMAT_GIF:
            case MtpConstants.FORMAT_PNG:
            case MtpConstants.FORMAT_DNG:
            case MtpConstants.FORMAT_HEIF:
                return IntStream.concat(Arrays.stream(FILE_PROPERTIES), Arrays.stream(IMAGE_PROPERTIES)).toArray();
            default:
                return FILE_PROPERTIES;
        }
    }

    private int[] getSupportedDeviceProperties() {
        return DEVICE_PROPERTIES;
    }

    private int[] getSupportedPlaybackFormats() {
        return PLAYBACK_FORMATS;
    }

    private int[] getSupportedCaptureFormats() {
        return null;
    }

    public MtpDatabase(Context context, String str, String[] strArr) throws Throwable {
        native_setup();
        this.mContext = context;
        this.mMediaProvider = context.getContentResolver().acquireContentProviderClient(MediaStore.AUTHORITY);
        this.mVolumeName = str;
        this.mObjectsUri = MediaStore.Files.getMtpObjectsUri(str);
        this.mMediaScanner = new MediaScanner(context, this.mVolumeName);
        this.mManager = new MtpStorageManager(new MtpStorageManager.MtpNotifier() {
            @Override
            public void sendObjectAdded(int i) {
                if (MtpDatabase.this.mServer != null) {
                    MtpDatabase.this.mServer.sendObjectAdded(i);
                }
            }

            @Override
            public void sendObjectRemoved(int i) {
                if (MtpDatabase.this.mServer != null) {
                    MtpDatabase.this.mServer.sendObjectRemoved(i);
                }
            }
        }, strArr == null ? null : Sets.newHashSet(strArr));
        initDeviceProperties(context);
        this.mDeviceType = SystemProperties.getInt("sys.usb.mtp.device_type", 0);
        this.mCloseGuard.open("close");
    }

    public void setServer(MtpServer mtpServer) {
        this.mServer = mtpServer;
        try {
            this.mContext.unregisterReceiver(this.mBatteryReceiver);
        } catch (IllegalArgumentException e) {
        }
        if (mtpServer != null) {
            this.mContext.registerReceiver(this.mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }

    @Override
    public void close() {
        this.mManager.close();
        this.mCloseGuard.close();
        if (this.mClosed.compareAndSet(false, true)) {
            this.mMediaScanner.close();
            if (this.mMediaProvider != null) {
                this.mMediaProvider.close();
            }
            native_finalize();
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    public void addStorage(StorageVolume storageVolume) {
        MtpStorage mtpStorageAddMtpStorage = this.mManager.addMtpStorage(storageVolume);
        this.mStorageMap.put(storageVolume.getPath(), mtpStorageAddMtpStorage);
        if (this.mServer != null) {
            this.mServer.addStorage(mtpStorageAddMtpStorage);
        }
    }

    public void removeStorage(StorageVolume storageVolume) {
        MtpStorage mtpStorage = this.mStorageMap.get(storageVolume.getPath());
        if (mtpStorage == null) {
            return;
        }
        if (this.mServer != null) {
            this.mServer.removeStorage(mtpStorage);
        }
        this.mManager.removeMtpStorage(mtpStorage);
        this.mStorageMap.remove(storageVolume.getPath());
    }

    private void initDeviceProperties(Context context) throws Throwable {
        SQLiteDatabase sQLiteDatabaseOpenOrCreateDatabase;
        Cursor cursorQuery;
        Exception e;
        this.mDeviceProperties = context.getSharedPreferences("device-properties", 0);
        if (context.getDatabasePath("device-properties").exists()) {
            Cursor cursor = null;
            try {
                sQLiteDatabaseOpenOrCreateDatabase = context.openOrCreateDatabase("device-properties", 0, null);
                if (sQLiteDatabaseOpenOrCreateDatabase != null) {
                    try {
                        cursorQuery = sQLiteDatabaseOpenOrCreateDatabase.query("properties", new String[]{"_id", "code", "value"}, null, null, null, null, null);
                        if (cursorQuery != null) {
                            try {
                                try {
                                    SharedPreferences.Editor editorEdit = this.mDeviceProperties.edit();
                                    while (cursorQuery.moveToNext()) {
                                        editorEdit.putString(cursorQuery.getString(1), cursorQuery.getString(2));
                                    }
                                    editorEdit.commit();
                                } catch (Exception e2) {
                                    e = e2;
                                    Log.e(TAG, "failed to migrate device properties", e);
                                    if (cursorQuery != null) {
                                        cursorQuery.close();
                                    }
                                    if (sQLiteDatabaseOpenOrCreateDatabase != null) {
                                    }
                                    context.deleteDatabase("device-properties");
                                }
                            } catch (Throwable th) {
                                th = th;
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                if (sQLiteDatabaseOpenOrCreateDatabase != null) {
                                    sQLiteDatabaseOpenOrCreateDatabase.close();
                                }
                                throw th;
                            }
                        }
                        cursor = cursorQuery;
                    } catch (Exception e3) {
                        cursorQuery = null;
                        e = e3;
                    } catch (Throwable th2) {
                        th = th2;
                        cursorQuery = null;
                        if (cursorQuery != null) {
                        }
                        if (sQLiteDatabaseOpenOrCreateDatabase != null) {
                        }
                        throw th;
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e4) {
                cursorQuery = null;
                e = e4;
                sQLiteDatabaseOpenOrCreateDatabase = null;
            } catch (Throwable th3) {
                th = th3;
                sQLiteDatabaseOpenOrCreateDatabase = null;
                cursorQuery = null;
            }
            if (sQLiteDatabaseOpenOrCreateDatabase != null) {
                sQLiteDatabaseOpenOrCreateDatabase.close();
            }
            context.deleteDatabase("device-properties");
        }
    }

    private int beginSendObject(String str, int i, int i2, int i3) {
        MtpStorageManager.MtpObject storageRoot = i2 == 0 ? this.mManager.getStorageRoot(i3) : this.mManager.getObject(i2);
        if (storageRoot == null) {
            return -1;
        }
        return this.mManager.beginSendObject(storageRoot, Paths.get(str, new String[0]).getFileName().toString(), i);
    }

    private void endSendObject(int i, boolean z) throws Throwable {
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        if (object == null || !this.mManager.endSendObject(object, z)) {
            Log.e(TAG, "Failed to successfully end send object");
            return;
        }
        if (z) {
            String string = object.getPath().toString();
            if (string.startsWith(SD_RW_PATH)) {
                string = string.replace(SD_RW_PATH, SD_RO_PATH);
            }
            int format = object.getFormat();
            ContentValues contentValues = new ContentValues();
            contentValues.put("_data", string);
            contentValues.put("format", Integer.valueOf(format));
            contentValues.put("_size", Long.valueOf(object.getSize()));
            contentValues.put("date_modified", Long.valueOf(object.getModifiedTime()));
            try {
                if (object.getParent().isRoot()) {
                    contentValues.put("parent", (Integer) 0);
                } else {
                    int iFindInMedia = findInMedia(object.getParent().getPath());
                    if (iFindInMedia != -1) {
                        contentValues.put("parent", Integer.valueOf(iFindInMedia));
                    } else {
                        return;
                    }
                }
                Uri uriInsert = this.mMediaProvider.insert(this.mObjectsUri, contentValues);
                if (uriInsert != null) {
                    rescanFile(string, Integer.parseInt(uriInsert.getPathSegments().get(2)), format);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in beginSendObject", e);
            }
        }
    }

    private void rescanFile(String str, int i, int i2) throws Throwable {
        String strSubstring;
        if (i2 == 47621) {
            int iLastIndexOf = str.lastIndexOf(47);
            if (iLastIndexOf >= 0) {
                strSubstring = str.substring(iLastIndexOf + 1);
            } else {
                strSubstring = str;
            }
            if (strSubstring.endsWith(".pla")) {
                strSubstring = strSubstring.substring(0, strSubstring.length() - 4);
            }
            ContentValues contentValues = new ContentValues(1);
            contentValues.put("_data", str);
            contentValues.put("name", strSubstring);
            contentValues.put("format", Integer.valueOf(i2));
            contentValues.put("date_modified", Long.valueOf(System.currentTimeMillis() / 1000));
            contentValues.put(MediaStore.MediaColumns.MEDIA_SCANNER_NEW_OBJECT_ID, Integer.valueOf(i));
            try {
                this.mMediaProvider.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValues);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in endSendObject", e);
                return;
            }
        }
        this.mMediaScanner.scanMtpFile(str, i, i2);
    }

    private int[] getObjectList(int i, int i2, int i3) {
        Stream<MtpStorageManager.MtpObject> objects = this.mManager.getObjects(i3, i2, i);
        if (objects == null) {
            return null;
        }
        return objects.mapToInt(new ToIntFunction() {
            @Override
            public final int applyAsInt(Object obj) {
                return ((MtpStorageManager.MtpObject) obj).getId();
            }
        }).toArray();
    }

    private int getNumObjects(int i, int i2, int i3) {
        Stream<MtpStorageManager.MtpObject> objects = this.mManager.getObjects(i3, i2, i);
        if (objects == null) {
            return -1;
        }
        return (int) objects.count();
    }

    private MtpPropertyList getObjectPropertyList(int i, int i2, int i3, int i4, int i5) throws Throwable {
        MtpPropertyGroup mtpPropertyGroup;
        if (i3 == 0) {
            if (i4 == 0) {
                return new MtpPropertyList(MtpConstants.RESPONSE_PARAMETER_NOT_SUPPORTED);
            }
            return new MtpPropertyList(MtpConstants.RESPONSE_SPECIFICATION_BY_GROUP_UNSUPPORTED);
        }
        if (i5 == -1 && (i == 0 || i == -1)) {
            i5 = 0;
            i = -1;
        }
        if (i5 != 0 && i5 != 1) {
            return new MtpPropertyList(MtpConstants.RESPONSE_SPECIFICATION_BY_DEPTH_UNSUPPORTED);
        }
        Stream<MtpStorageManager.MtpObject> streamOf = Stream.of((Object[]) new MtpStorageManager.MtpObject[0]);
        if (i == -1) {
            streamOf = this.mManager.getObjects(0, i2, -1);
            if (streamOf == null) {
                return new MtpPropertyList(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
            }
        } else if (i != 0) {
            MtpStorageManager.MtpObject object = this.mManager.getObject(i);
            if (object == null) {
                return new MtpPropertyList(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
            }
            if (object.getFormat() == i2 || i2 == 0) {
                streamOf = Stream.of(object);
            }
        }
        if (i == 0 || i5 == 1) {
            if (i == 0) {
                i = -1;
            }
            Stream<MtpStorageManager.MtpObject> objects = this.mManager.getObjects(i, i2, -1);
            if (objects == null) {
                return new MtpPropertyList(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
            }
            streamOf = Stream.concat(streamOf, objects);
        }
        MtpPropertyList mtpPropertyList = new MtpPropertyList(MtpConstants.RESPONSE_OK);
        for (MtpStorageManager.MtpObject mtpObject : streamOf) {
            if (i3 == -1) {
                MtpPropertyGroup mtpPropertyGroup2 = this.mPropertyGroupsByFormat.get(Integer.valueOf(mtpObject.getFormat()));
                if (mtpPropertyGroup2 == null) {
                    mtpPropertyGroup = new MtpPropertyGroup(this.mMediaProvider, this.mVolumeName, getSupportedObjectProperties(i2));
                    this.mPropertyGroupsByFormat.put(Integer.valueOf(i2), mtpPropertyGroup);
                } else {
                    mtpPropertyGroup = mtpPropertyGroup2;
                }
            } else {
                int[] iArr = {i3};
                mtpPropertyGroup = this.mPropertyGroupsByProperty.get(Integer.valueOf(i3));
                if (mtpPropertyGroup == null) {
                    mtpPropertyGroup = new MtpPropertyGroup(this.mMediaProvider, this.mVolumeName, iArr);
                    this.mPropertyGroupsByProperty.put(Integer.valueOf(i3), mtpPropertyGroup);
                }
            }
            int propertyList = mtpPropertyGroup.getPropertyList(mtpObject, mtpPropertyList);
            if (propertyList != 8193) {
                return new MtpPropertyList(propertyList);
            }
        }
        return mtpPropertyList;
    }

    private int renameFile(int i, String str) {
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        if (object == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        Path path = object.getPath();
        if (!this.mManager.beginRenameObject(object, str)) {
            return 8194;
        }
        Path path2 = object.getPath();
        boolean zRenameTo = path.toFile().renameTo(path2.toFile());
        try {
            Os.access(path.toString(), OsConstants.F_OK);
            Os.access(path2.toString(), OsConstants.F_OK);
        } catch (ErrnoException e) {
        }
        if (!this.mManager.endRenameObject(object, path.getFileName().toString(), zRenameTo)) {
            Log.e(TAG, "Failed to end rename object");
        }
        if (!zRenameTo) {
            return 8194;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("_data", path2.toString());
        try {
            this.mMediaProvider.update(this.mObjectsUri, contentValues, PATH_WHERE, new String[]{path.toString()});
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in mMediaProvider.update", e2);
        }
        if (object.isDir()) {
            if (path.getFileName().startsWith(".") && !path2.startsWith(".")) {
                try {
                    this.mMediaProvider.call(MediaStore.UNHIDE_CALL, path2.toString(), null);
                    return MtpConstants.RESPONSE_OK;
                } catch (RemoteException e3) {
                    Log.e(TAG, "failed to unhide/rescan for " + path2);
                    return MtpConstants.RESPONSE_OK;
                }
            }
            return MtpConstants.RESPONSE_OK;
        }
        if (path.getFileName().toString().toLowerCase(Locale.US).equals(".nomedia") && !path2.getFileName().toString().toLowerCase(Locale.US).equals(".nomedia")) {
            try {
                this.mMediaProvider.call(MediaStore.UNHIDE_CALL, path.getParent().toString(), null);
                return MtpConstants.RESPONSE_OK;
            } catch (RemoteException e4) {
                Log.e(TAG, "failed to unhide/rescan for " + path2);
                return MtpConstants.RESPONSE_OK;
            }
        }
        return MtpConstants.RESPONSE_OK;
    }

    private int beginMoveObject(int i, int i2, int i3) {
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        MtpStorageManager.MtpObject storageRoot = i2 == 0 ? this.mManager.getStorageRoot(i3) : this.mManager.getObject(i2);
        if (object == null || storageRoot == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        if (this.mManager.beginMoveObject(object, storageRoot)) {
            return MtpConstants.RESPONSE_OK;
        }
        return 8194;
    }

    private void endMoveObject(int i, int i2, int i3, int i4, int i5, boolean z) throws Throwable {
        int iFindInMedia;
        MtpStorageManager.MtpObject storageRoot = i == 0 ? this.mManager.getStorageRoot(i3) : this.mManager.getObject(i);
        MtpStorageManager.MtpObject storageRoot2 = i2 == 0 ? this.mManager.getStorageRoot(i4) : this.mManager.getObject(i2);
        String name = this.mManager.getObject(i5).getName();
        if (storageRoot2 == null || storageRoot == null || !this.mManager.endMoveObject(storageRoot, storageRoot2, name, z)) {
            Log.e(TAG, "Failed to end move object");
            return;
        }
        MtpStorageManager.MtpObject object = this.mManager.getObject(i5);
        if (!z || object == null) {
            return;
        }
        ContentValues contentValues = new ContentValues();
        Path pathResolve = storageRoot2.getPath().resolve(name);
        Path pathResolve2 = storageRoot.getPath().resolve(name);
        if (pathResolve.toString().startsWith(SD_RW_PATH)) {
            pathResolve = Paths.get(pathResolve.toString().replace(SD_RW_PATH, SD_RO_PATH), new String[0]);
        }
        if (pathResolve2.toString().startsWith(SD_RW_PATH)) {
            pathResolve2 = Paths.get(pathResolve2.toString().replace(SD_RW_PATH, SD_RO_PATH), new String[0]);
        }
        contentValues.put("_data", pathResolve.toString());
        if (object.getParent().isRoot()) {
            contentValues.put("parent", (Integer) 0);
        } else {
            int iFindInMedia2 = findInMedia(pathResolve.getParent());
            if (iFindInMedia2 != -1) {
                contentValues.put("parent", Integer.valueOf(iFindInMedia2));
            } else {
                deleteFromMedia(pathResolve2, object.isDir());
                return;
            }
        }
        String[] strArr = {pathResolve2.toString()};
        try {
            if (!storageRoot.isRoot()) {
                iFindInMedia = findInMedia(pathResolve2.getParent());
            } else {
                iFindInMedia = -1;
            }
            if (!storageRoot.isRoot() && iFindInMedia == -1) {
                contentValues.put("format", Integer.valueOf(object.getFormat()));
                contentValues.put("_size", Long.valueOf(object.getSize()));
                contentValues.put("date_modified", Long.valueOf(object.getModifiedTime()));
                Uri uriInsert = this.mMediaProvider.insert(this.mObjectsUri, contentValues);
                if (uriInsert != null) {
                    rescanFile(pathResolve.toString(), Integer.parseInt(uriInsert.getPathSegments().get(2)), object.getFormat());
                    return;
                }
                return;
            }
            this.mMediaProvider.update(this.mObjectsUri, contentValues, PATH_WHERE, strArr);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in mMediaProvider.update", e);
        }
    }

    private int beginCopyObject(int i, int i2, int i3) {
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        MtpStorageManager.MtpObject storageRoot = i2 == 0 ? this.mManager.getStorageRoot(i3) : this.mManager.getObject(i2);
        if (object == null || storageRoot == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        return this.mManager.beginCopyObject(object, storageRoot);
    }

    private void endCopyObject(int i, boolean z) throws Throwable {
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        if (object == null || !this.mManager.endCopyObject(object, z)) {
            Log.e(TAG, "Failed to end copy object");
            return;
        }
        if (!z) {
            return;
        }
        String string = object.getPath().toString();
        if (string.startsWith(SD_RW_PATH)) {
            string = string.replace(SD_RW_PATH, SD_RO_PATH);
        }
        int format = object.getFormat();
        ContentValues contentValues = new ContentValues();
        contentValues.put("_data", string);
        contentValues.put("format", Integer.valueOf(format));
        contentValues.put("_size", Long.valueOf(object.getSize()));
        contentValues.put("date_modified", Long.valueOf(object.getModifiedTime()));
        try {
            if (object.getParent().isRoot()) {
                contentValues.put("parent", (Integer) 0);
            } else {
                int iFindInMedia = findInMedia(object.getParent().getPath());
                if (iFindInMedia != -1) {
                    contentValues.put("parent", Integer.valueOf(iFindInMedia));
                } else {
                    return;
                }
            }
            if (object.isDir()) {
                this.mMediaScanner.scanDirectories(new String[]{string});
                return;
            }
            Uri uriInsert = this.mMediaProvider.insert(this.mObjectsUri, contentValues);
            if (uriInsert != null) {
                rescanFile(string, Integer.parseInt(uriInsert.getPathSegments().get(2)), format);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in beginSendObject", e);
        }
    }

    private int setObjectProperty(int i, int i2, long j, String str) {
        if (i2 == 56327) {
            return renameFile(i, str);
        }
        return MtpConstants.RESPONSE_OBJECT_PROP_NOT_SUPPORTED;
    }

    private int getDeviceProperty(int i, long[] jArr, char[] cArr) {
        if (i == 20481) {
            jArr[0] = this.mBatteryLevel;
            jArr[1] = this.mBatteryScale;
            return MtpConstants.RESPONSE_OK;
        }
        if (i == 20483) {
            Display defaultDisplay = ((WindowManager) this.mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            String str = Integer.toString(defaultDisplay.getMaximumSizeDimension()) + "x" + Integer.toString(defaultDisplay.getMaximumSizeDimension());
            str.getChars(0, str.length(), cArr, 0);
            cArr[str.length()] = 0;
            return MtpConstants.RESPONSE_OK;
        }
        if (i != 54279) {
            switch (i) {
                case MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER:
                case MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME:
                    String string = this.mDeviceProperties.getString(Integer.toString(i), "");
                    int length = string.length();
                    if (length > 255) {
                        length = 255;
                    }
                    string.getChars(0, length, cArr, 0);
                    cArr[length] = 0;
                    break;
            }
            return MtpConstants.RESPONSE_OK;
        }
        jArr[0] = this.mDeviceType;
        return MtpConstants.RESPONSE_OK;
    }

    private int setDeviceProperty(int i, long j, String str) {
        switch (i) {
            case MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER:
            case MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME:
                SharedPreferences.Editor editorEdit = this.mDeviceProperties.edit();
                editorEdit.putString(Integer.toString(i), str);
                if (editorEdit.commit()) {
                    return MtpConstants.RESPONSE_OK;
                }
                return 8194;
            default:
                return MtpConstants.RESPONSE_DEVICE_PROP_NOT_SUPPORTED;
        }
    }

    private boolean getObjectInfo(int i, int[] iArr, char[] cArr, long[] jArr) {
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        if (object == null) {
            return false;
        }
        iArr[0] = object.getStorageId();
        iArr[1] = object.getFormat();
        iArr[2] = object.getParent().isRoot() ? 0 : object.getParent().getId();
        int iMin = Integer.min(object.getName().length(), 255);
        object.getName().getChars(0, iMin, cArr, 0);
        cArr[iMin] = 0;
        jArr[0] = object.getModifiedTime();
        jArr[1] = object.getModifiedTime();
        return true;
    }

    private int getObjectFilePath(int i, char[] cArr, long[] jArr) {
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        if (object == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        String string = object.getPath().toString();
        int iMin = Integer.min(string.length(), 4096);
        string.getChars(0, iMin, cArr, 0);
        cArr[iMin] = 0;
        jArr[0] = object.getSize();
        jArr[1] = object.getFormat();
        return MtpConstants.RESPONSE_OK;
    }

    private int getObjectFormat(int i) {
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        if (object == null) {
            return -1;
        }
        return object.getFormat();
    }

    private int beginDeleteObject(int i) {
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        if (object == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        if (!this.mManager.beginRemoveObject(object)) {
            return 8194;
        }
        return MtpConstants.RESPONSE_OK;
    }

    private void endDeleteObject(int i, boolean z) {
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        if (object == null) {
            return;
        }
        if (!this.mManager.endRemoveObject(object, z)) {
            Log.e(TAG, "Failed to end remove object");
        }
        if (z) {
            deleteFromMedia(object.getPath(), object.isDir());
        }
    }

    private int findInMedia(Path path) throws Throwable {
        int i = -1;
        i = -1;
        i = -1;
        i = -1;
        ?? r1 = 0;
        r1 = 0;
        r1 = 0;
        try {
            try {
                Cursor cursorQuery = this.mMediaProvider.query(this.mObjectsUri, ID_PROJECTION, PATH_WHERE, new String[]{path.toString()}, null, null);
                if (cursorQuery != null) {
                    try {
                        boolean zMoveToNext = cursorQuery.moveToNext();
                        r1 = zMoveToNext;
                        if (zMoveToNext) {
                            int i2 = cursorQuery.getInt(0);
                            i = i2;
                            r1 = i2;
                        }
                    } catch (RemoteException e) {
                        r1 = cursorQuery;
                        Log.e(TAG, "Error finding " + path + " in MediaProvider");
                        if (r1 != 0) {
                            r1.close();
                        }
                    } catch (Throwable th) {
                        th = th;
                        r1 = cursorQuery;
                        if (r1 != 0) {
                            r1.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (RemoteException e2) {
        }
        return i;
    }

    private void deleteFromMedia(Path path, boolean z) {
        if (z) {
            try {
                this.mMediaProvider.delete(this.mObjectsUri, "_data LIKE ?1 AND lower(substr(_data,1,?2))=lower(?3)", new String[]{path + "/%", Integer.toString(path.toString().length() + 1), path.toString() + "/"});
            } catch (Exception e) {
                Log.d(TAG, "Failed to delete " + path + " from MediaProvider");
                return;
            }
        }
        if (this.mMediaProvider.delete(this.mObjectsUri, PATH_WHERE, new String[]{path.toString()}) > 0) {
            if (!z && path.toString().toLowerCase(Locale.US).endsWith(".nomedia")) {
                try {
                    this.mMediaProvider.call(MediaStore.UNHIDE_CALL, path.getParent().toString(), null);
                    return;
                } catch (RemoteException e2) {
                    Log.e(TAG, "failed to unhide/rescan for " + path);
                    return;
                }
            }
            return;
        }
        Log.i(TAG, "Mediaprovider didn't delete " + path);
    }

    private int[] getObjectReferences(int i) throws Throwable {
        ?? FindInMedia;
        Throwable th;
        Cursor cursorQuery;
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        if (object == null || (FindInMedia = findInMedia(object.getPath())) == -1) {
            return null;
        }
        try {
            try {
                cursorQuery = this.mMediaProvider.query(MediaStore.Files.getMtpReferencesUri(this.mVolumeName, (long) FindInMedia), PATH_PROJECTION, null, null, null, null);
                if (cursorQuery == null) {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return null;
                }
                try {
                    ArrayList arrayList = new ArrayList();
                    while (cursorQuery.moveToNext()) {
                        MtpStorageManager.MtpObject byPath = this.mManager.getByPath(cursorQuery.getString(0));
                        if (byPath != null) {
                            arrayList.add(Integer.valueOf(byPath.getId()));
                        }
                    }
                    int[] array = arrayList.stream().mapToInt(new ToIntFunction() {
                        @Override
                        public final int applyAsInt(Object obj) {
                            return ((Integer) obj).intValue();
                        }
                    }).toArray();
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return array;
                } catch (RemoteException e) {
                    e = e;
                    Log.e(TAG, "RemoteException in getObjectList", e);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return null;
                }
            } catch (Throwable th2) {
                th = th2;
                if (FindInMedia != 0) {
                    FindInMedia.close();
                }
                throw th;
            }
        } catch (RemoteException e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th3) {
            th = th3;
            FindInMedia = 0;
            if (FindInMedia != 0) {
            }
            throw th;
        }
    }

    private int setObjectReferences(int i, int[] iArr) throws Throwable {
        int iFindInMedia;
        MtpStorageManager.MtpObject object = this.mManager.getObject(i);
        if (object == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        int iFindInMedia2 = findInMedia(object.getPath());
        if (iFindInMedia2 == -1) {
            return 8194;
        }
        Uri mtpReferencesUri = MediaStore.Files.getMtpReferencesUri(this.mVolumeName, iFindInMedia2);
        ArrayList arrayList = new ArrayList();
        for (int i2 : iArr) {
            MtpStorageManager.MtpObject object2 = this.mManager.getObject(i2);
            if (object2 != null && (iFindInMedia = findInMedia(object2.getPath())) != -1) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("_id", Integer.valueOf(iFindInMedia));
                arrayList.add(contentValues);
            }
        }
        try {
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setObjectReferences", e);
        }
        if (this.mMediaProvider.bulkInsert(mtpReferencesUri, (ContentValues[]) arrayList.toArray(new ContentValues[0])) <= 0) {
            return 8194;
        }
        return MtpConstants.RESPONSE_OK;
    }
}
