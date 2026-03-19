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
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import dalvik.system.CloseGuard;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class MtpDatabase implements AutoCloseable {
    static final int[] AUDIO_PROPERTIES;
    private static final int DEVICE_PROPERTIES_DATABASE_VERSION = 1;
    static final int[] FILE_PROPERTIES;
    private static final String FORMAT_PARENT_WHERE = "format=? AND parent=?";
    private static final String FORMAT_WHERE = "format=?";
    private static final String ID_WHERE = "_id=?";
    static final int[] IMAGE_PROPERTIES;
    private static final String PARENT_WHERE = "parent=?";
    private static final String PATH_WHERE = "_data=?";
    private static final String STORAGE_FORMAT_PARENT_WHERE = "storage_id=? AND format=? AND parent=?";
    private static final String STORAGE_FORMAT_WHERE = "storage_id=? AND format=?";
    private static final String STORAGE_PARENT_WHERE = "storage_id=? AND parent=?";
    private static final String STORAGE_WHERE = "storage_id=?";
    private static final String TAG = "MtpDatabase";
    static final int[] VIDEO_PROPERTIES;
    private int mBatteryLevel;
    private int mBatteryScale;
    private final Context mContext;
    private boolean mDatabaseModified;
    private SharedPreferences mDeviceProperties;
    private final ContentProviderClient mMediaProvider;
    private final MediaScanner mMediaScanner;
    private final String mMediaStoragePath;
    private long mNativeContext;
    private final Uri mObjectsUri;
    private final String mPackageName;
    private MtpServer mServer;
    private final String[] mSubDirectories;
    private String mSubDirectoriesWhere;
    private String[] mSubDirectoriesWhereArgs;
    private final String mVolumeName;
    private static final String[] ID_PROJECTION = {"_id"};
    private static final String[] PATH_PROJECTION = {"_id", "_data"};
    private static final String[] FORMAT_PROJECTION = {"_id", MediaStore.Files.FileColumns.FORMAT};
    private static final String[] PATH_FORMAT_PROJECTION = {"_id", "_data", MediaStore.Files.FileColumns.FORMAT};
    private static final String[] OBJECT_INFO_PROJECTION = {"_id", MediaStore.Files.FileColumns.STORAGE_ID, MediaStore.Files.FileColumns.FORMAT, "parent", "_data", "date_added", "date_modified"};
    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final HashMap<String, MtpStorage> mStorageMap = new HashMap<>();
    private final HashMap<Integer, MtpPropertyGroup> mPropertyGroupsByProperty = new HashMap<>();
    private final HashMap<Integer, MtpPropertyGroup> mPropertyGroupsByFormat = new HashMap<>();
    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                return;
            }
            MtpDatabase.this.mBatteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
            int newLevel = intent.getIntExtra("level", 0);
            if (newLevel == MtpDatabase.this.mBatteryLevel) {
                return;
            }
            MtpDatabase.this.mBatteryLevel = newLevel;
            if (MtpDatabase.this.mServer == null) {
                return;
            }
            MtpDatabase.this.mServer.sendDevicePropertyChanged(MtpConstants.DEVICE_PROPERTY_BATTERY_LEVEL);
        }
    };

    private final native void native_finalize();

    private final native void native_setup();

    static {
        System.loadLibrary("media_jni");
        FILE_PROPERTIES = new int[]{MtpConstants.PROPERTY_STORAGE_ID, MtpConstants.PROPERTY_OBJECT_FORMAT, MtpConstants.PROPERTY_PROTECTION_STATUS, MtpConstants.PROPERTY_OBJECT_SIZE, MtpConstants.PROPERTY_OBJECT_FILE_NAME, MtpConstants.PROPERTY_DATE_MODIFIED, MtpConstants.PROPERTY_PARENT_OBJECT, MtpConstants.PROPERTY_PERSISTENT_UID, MtpConstants.PROPERTY_NAME, MtpConstants.PROPERTY_DISPLAY_NAME, MtpConstants.PROPERTY_DATE_ADDED};
        AUDIO_PROPERTIES = new int[]{MtpConstants.PROPERTY_STORAGE_ID, MtpConstants.PROPERTY_OBJECT_FORMAT, MtpConstants.PROPERTY_PROTECTION_STATUS, MtpConstants.PROPERTY_OBJECT_SIZE, MtpConstants.PROPERTY_OBJECT_FILE_NAME, MtpConstants.PROPERTY_DATE_MODIFIED, MtpConstants.PROPERTY_PARENT_OBJECT, MtpConstants.PROPERTY_PERSISTENT_UID, MtpConstants.PROPERTY_NAME, MtpConstants.PROPERTY_DISPLAY_NAME, MtpConstants.PROPERTY_DATE_ADDED, MtpConstants.PROPERTY_ARTIST, MtpConstants.PROPERTY_ALBUM_NAME, MtpConstants.PROPERTY_ALBUM_ARTIST, MtpConstants.PROPERTY_TRACK, MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE, MtpConstants.PROPERTY_DURATION, MtpConstants.PROPERTY_GENRE, MtpConstants.PROPERTY_COMPOSER, MtpConstants.PROPERTY_AUDIO_WAVE_CODEC, MtpConstants.PROPERTY_BITRATE_TYPE, MtpConstants.PROPERTY_AUDIO_BITRATE, MtpConstants.PROPERTY_NUMBER_OF_CHANNELS, MtpConstants.PROPERTY_SAMPLE_RATE};
        VIDEO_PROPERTIES = new int[]{MtpConstants.PROPERTY_STORAGE_ID, MtpConstants.PROPERTY_OBJECT_FORMAT, MtpConstants.PROPERTY_PROTECTION_STATUS, MtpConstants.PROPERTY_OBJECT_SIZE, MtpConstants.PROPERTY_OBJECT_FILE_NAME, MtpConstants.PROPERTY_DATE_MODIFIED, MtpConstants.PROPERTY_PARENT_OBJECT, MtpConstants.PROPERTY_PERSISTENT_UID, MtpConstants.PROPERTY_NAME, MtpConstants.PROPERTY_DISPLAY_NAME, MtpConstants.PROPERTY_DATE_ADDED, MtpConstants.PROPERTY_ARTIST, MtpConstants.PROPERTY_ALBUM_NAME, MtpConstants.PROPERTY_DURATION, MtpConstants.PROPERTY_DESCRIPTION};
        IMAGE_PROPERTIES = new int[]{MtpConstants.PROPERTY_STORAGE_ID, MtpConstants.PROPERTY_OBJECT_FORMAT, MtpConstants.PROPERTY_PROTECTION_STATUS, MtpConstants.PROPERTY_OBJECT_SIZE, MtpConstants.PROPERTY_OBJECT_FILE_NAME, MtpConstants.PROPERTY_DATE_MODIFIED, MtpConstants.PROPERTY_PARENT_OBJECT, MtpConstants.PROPERTY_PERSISTENT_UID, MtpConstants.PROPERTY_NAME, MtpConstants.PROPERTY_DISPLAY_NAME, MtpConstants.PROPERTY_DATE_ADDED, MtpConstants.PROPERTY_DESCRIPTION};
    }

    public MtpDatabase(Context context, String volumeName, String storagePath, String[] subDirectories) {
        native_setup();
        this.mContext = context;
        this.mPackageName = context.getPackageName();
        this.mMediaProvider = context.getContentResolver().acquireContentProviderClient(MediaStore.AUTHORITY);
        this.mVolumeName = volumeName;
        this.mMediaStoragePath = storagePath;
        this.mObjectsUri = MediaStore.Files.getMtpObjectsUri(volumeName);
        this.mMediaScanner = new MediaScanner(context, this.mVolumeName);
        this.mSubDirectories = subDirectories;
        if (subDirectories != null) {
            StringBuilder builder = new StringBuilder();
            builder.append("(");
            int count = subDirectories.length;
            for (int i = 0; i < count; i++) {
                builder.append("_data=? OR _data LIKE ?");
                if (i != count - 1) {
                    builder.append(" OR ");
                }
            }
            builder.append(")");
            this.mSubDirectoriesWhere = builder.toString();
            this.mSubDirectoriesWhereArgs = new String[count * 2];
            int j = 0;
            for (String path : subDirectories) {
                int j2 = j + 1;
                this.mSubDirectoriesWhereArgs[j] = path;
                j = j2 + 1;
                this.mSubDirectoriesWhereArgs[j2] = path + "/%";
            }
        }
        initDeviceProperties(context);
        this.mCloseGuard.open("close");
    }

    public void setServer(MtpServer server) {
        this.mServer = server;
        try {
            this.mContext.unregisterReceiver(this.mBatteryReceiver);
        } catch (IllegalArgumentException e) {
        }
        if (server == null) {
            return;
        }
        this.mContext.registerReceiver(this.mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void close() {
        this.mCloseGuard.close();
        if (!this.mClosed.compareAndSet(false, true)) {
            return;
        }
        this.mMediaScanner.close();
        this.mMediaProvider.close();
        native_finalize();
    }

    protected void finalize() throws Throwable {
        try {
            this.mCloseGuard.warnIfOpen();
            close();
        } finally {
            super.finalize();
        }
    }

    public void addStorage(MtpStorage storage) {
        this.mStorageMap.put(storage.getPath(), storage);
    }

    public void removeStorage(MtpStorage storage) {
        this.mStorageMap.remove(storage.getPath());
    }

    private void initDeviceProperties(Context context) {
        this.mDeviceProperties = context.getSharedPreferences("device-properties", 0);
        File databaseFile = context.getDatabasePath("device-properties");
        if (!databaseFile.exists()) {
            return;
        }
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            try {
                db = context.openOrCreateDatabase("device-properties", 0, null);
                if (db != null && (c = db.query("properties", new String[]{"_id", "code", "value"}, null, null, null, null, null)) != null) {
                    SharedPreferences.Editor e = this.mDeviceProperties.edit();
                    while (c.moveToNext()) {
                        String name = c.getString(1);
                        String value = c.getString(2);
                        e.putString(name, value);
                    }
                    e.commit();
                }
                if (c != null) {
                    c.close();
                }
                if (db != null) {
                    db.close();
                }
            } catch (Exception e2) {
                Log.e(TAG, "failed to migrate device properties", e2);
                if (c != null) {
                    c.close();
                }
                if (db != null) {
                    db.close();
                }
            }
            context.deleteDatabase("device-properties");
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            if (db != null) {
                db.close();
            }
            throw th;
        }
    }

    private boolean inStorageSubDirectory(String path) {
        if (this.mSubDirectories == null) {
            return true;
        }
        if (path == null) {
            return false;
        }
        boolean allowed = false;
        int pathLength = path.length();
        for (int i = 0; i < this.mSubDirectories.length && !allowed; i++) {
            String subdir = this.mSubDirectories[i];
            int subdirLength = subdir.length();
            if (subdirLength < pathLength && path.charAt(subdirLength) == '/' && path.startsWith(subdir)) {
                allowed = true;
            }
        }
        return allowed;
    }

    private boolean isStorageSubDirectory(String path) {
        if (this.mSubDirectories == null) {
            return false;
        }
        for (int i = 0; i < this.mSubDirectories.length; i++) {
            if (path.equals(this.mSubDirectories[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean inStorageRoot(String path) {
        try {
            File f = new File(path);
            String canonical = f.getCanonicalPath();
            for (String root : this.mStorageMap.keySet()) {
                if (canonical.startsWith(root)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private int beginSendObject(String path, int format, int parent, int storageId, long size, long modified) {
        if (!inStorageRoot(path)) {
            Log.e(TAG, "attempt to put file outside of storage area: " + path);
            return -1;
        }
        if (!inStorageSubDirectory(path)) {
            return -1;
        }
        if (path != null) {
            Log.d(TAG, "beginSendObject: path = " + path);
            Cursor c = null;
            try {
                try {
                    c = this.mMediaProvider.query(this.mObjectsUri, ID_PROJECTION, PATH_WHERE, new String[]{path}, null, null);
                    if (c != null && c.getCount() > 0) {
                        Log.w(TAG, "file already exists in beginSendObject: " + path);
                        return -1;
                    }
                    if (c != null) {
                        c.close();
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in beginSendObject", e);
                    if (c != null) {
                        c.close();
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        this.mDatabaseModified = true;
        ContentValues values = new ContentValues();
        values.put("_data", path);
        values.put(MediaStore.Files.FileColumns.FORMAT, Integer.valueOf(format));
        values.put("parent", Integer.valueOf(parent));
        values.put(MediaStore.Files.FileColumns.STORAGE_ID, Integer.valueOf(storageId));
        values.put("_size", Long.valueOf(size));
        values.put("date_modified", Long.valueOf(modified));
        try {
            Uri uri = this.mMediaProvider.insert(this.mObjectsUri, values);
            if (uri != null) {
                return Integer.parseInt(uri.getPathSegments().get(2));
            }
            return -1;
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in beginSendObject", e2);
            return -1;
        }
    }

    private void endSendObject(String path, int handle, int format, boolean succeeded) {
        if (succeeded) {
            if (format == 47621) {
                String name = path;
                int lastSlash = path.lastIndexOf(47);
                if (lastSlash >= 0) {
                    name = path.substring(lastSlash + 1);
                }
                if (name.endsWith(".pla")) {
                    name = name.substring(0, name.length() - 4);
                }
                ContentValues values = new ContentValues(1);
                values.put("_data", path);
                values.put("name", name);
                values.put(MediaStore.Files.FileColumns.FORMAT, Integer.valueOf(format));
                values.put("date_modified", Long.valueOf(System.currentTimeMillis() / 1000));
                values.put(MediaStore.MediaColumns.MEDIA_SCANNER_NEW_OBJECT_ID, Integer.valueOf(handle));
                try {
                    this.mMediaProvider.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
                    return;
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in endSendObject", e);
                    return;
                }
            }
            this.mMediaScanner.scanMtpFile(path, handle, format);
            return;
        }
        deleteFile(handle);
    }

    private Cursor createObjectQuery(int storageID, int format, int parent) throws RemoteException {
        String where;
        String[] strArr;
        if (storageID == -1) {
            if (format == 0) {
                if (parent == 0) {
                    where = null;
                    strArr = null;
                } else {
                    if (parent == -1) {
                        parent = 0;
                    }
                    where = PARENT_WHERE;
                    strArr = new String[]{Integer.toString(parent)};
                }
            } else if (parent == 0) {
                where = FORMAT_WHERE;
                strArr = new String[]{Integer.toString(format)};
            } else {
                if (parent == -1) {
                    parent = 0;
                }
                where = FORMAT_PARENT_WHERE;
                strArr = new String[]{Integer.toString(format), Integer.toString(parent)};
            }
        } else if (format == 0) {
            if (parent == 0) {
                where = STORAGE_WHERE;
                strArr = new String[]{Integer.toString(storageID)};
            } else {
                if (parent == -1) {
                    parent = 0;
                }
                where = STORAGE_PARENT_WHERE;
                strArr = new String[]{Integer.toString(storageID), Integer.toString(parent)};
            }
        } else if (parent == 0) {
            where = STORAGE_FORMAT_WHERE;
            strArr = new String[]{Integer.toString(storageID), Integer.toString(format)};
        } else {
            if (parent == -1) {
                parent = 0;
            }
            where = STORAGE_FORMAT_PARENT_WHERE;
            strArr = new String[]{Integer.toString(storageID), Integer.toString(format), Integer.toString(parent)};
        }
        if (this.mSubDirectoriesWhere != null) {
            if (where == null) {
                where = this.mSubDirectoriesWhere;
                strArr = this.mSubDirectoriesWhereArgs;
            } else {
                where = where + " AND " + this.mSubDirectoriesWhere;
                String[] newWhereArgs = new String[strArr.length + this.mSubDirectoriesWhereArgs.length];
                int i = 0;
                while (i < strArr.length) {
                    newWhereArgs[i] = strArr[i];
                    i++;
                }
                for (int j = 0; j < this.mSubDirectoriesWhereArgs.length; j++) {
                    newWhereArgs[i] = this.mSubDirectoriesWhereArgs[j];
                    i++;
                }
                strArr = newWhereArgs;
            }
        }
        return this.mMediaProvider.query(this.mObjectsUri, ID_PROJECTION, where, strArr, null, null);
    }

    private int[] getObjectList(int storageID, int format, int parent) {
        Cursor c;
        AutoCloseable autoCloseable = null;
        try {
            try {
                c = createObjectQuery(storageID, format, parent);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in getObjectList", e);
                if (0 != 0) {
                    autoCloseable.close();
                }
            }
            if (c == null) {
                if (c != null) {
                    c.close();
                }
                return null;
            }
            int count = c.getCount();
            if (count <= 0) {
                if (c != null) {
                    c.close();
                }
                return null;
            }
            int[] result = new int[count];
            for (int i = 0; i < count; i++) {
                c.moveToNext();
                result[i] = c.getInt(0);
            }
            if (c != null) {
                c.close();
            }
            return result;
        } catch (Throwable th) {
            if (0 != 0) {
                autoCloseable.close();
            }
            throw th;
        }
    }

    private int getNumObjects(int storageID, int format, int parent) {
        Cursor c = null;
        try {
            try {
                c = createObjectQuery(storageID, format, parent);
                if (c != null) {
                    int count = c.getCount();
                    if (c != null) {
                        c.close();
                    }
                    return count;
                }
                if (c == null) {
                    return -1;
                }
                c.close();
                return -1;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in getNumObjects", e);
                if (c == null) {
                    return -1;
                }
                c.close();
                return -1;
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private int[] getSupportedPlaybackFormats() {
        return new int[]{12288, 12289, 12292, 12293, 12296, 12297, 12299, MtpConstants.FORMAT_EXIF_JPEG, MtpConstants.FORMAT_TIFF_EP, MtpConstants.FORMAT_BMP, MtpConstants.FORMAT_GIF, MtpConstants.FORMAT_JFIF, MtpConstants.FORMAT_PNG, MtpConstants.FORMAT_TIFF, MtpConstants.FORMAT_WMA, MtpConstants.FORMAT_OGG, MtpConstants.FORMAT_AAC, MtpConstants.FORMAT_MP4_CONTAINER, MtpConstants.FORMAT_MP2, MtpConstants.FORMAT_3GP_CONTAINER, MtpConstants.FORMAT_ABSTRACT_AV_PLAYLIST, MtpConstants.FORMAT_WPL_PLAYLIST, MtpConstants.FORMAT_M3U_PLAYLIST, MtpConstants.FORMAT_PLS_PLAYLIST, MtpConstants.FORMAT_XML_DOCUMENT, MtpConstants.FORMAT_FLAC, MtpConstants.FORMAT_DNG};
    }

    private int[] getSupportedCaptureFormats() {
        return null;
    }

    private int[] getSupportedObjectProperties(int format) {
        switch (format) {
            case 12296:
            case 12297:
            case MtpConstants.FORMAT_WMA:
            case MtpConstants.FORMAT_OGG:
            case MtpConstants.FORMAT_AAC:
                return AUDIO_PROPERTIES;
            case 12299:
            case MtpConstants.FORMAT_WMV:
            case MtpConstants.FORMAT_3GP_CONTAINER:
                return VIDEO_PROPERTIES;
            case MtpConstants.FORMAT_EXIF_JPEG:
            case MtpConstants.FORMAT_BMP:
            case MtpConstants.FORMAT_GIF:
            case MtpConstants.FORMAT_PNG:
            case MtpConstants.FORMAT_DNG:
                return IMAGE_PROPERTIES;
            default:
                return FILE_PROPERTIES;
        }
    }

    private int[] getSupportedDeviceProperties() {
        return new int[]{MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER, MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME, MtpConstants.DEVICE_PROPERTY_IMAGE_SIZE, MtpConstants.DEVICE_PROPERTY_BATTERY_LEVEL};
    }

    private MtpPropertyList getObjectPropertyList(int handle, int format, int property, int groupCode, int depth) {
        MtpPropertyGroup propertyGroup;
        Log.d(TAG, "getObjectPropertyList: handle = 0x" + Long.toHexString(handle) + ", property = 0x" + Long.toHexString(property));
        if (groupCode != 0) {
            Log.i(TAG, "getObjectPropertyList RESPONSE_SPECIFICATION_BY_GROUP_UNSUPPORTED");
            return new MtpPropertyList(0, MtpConstants.RESPONSE_SPECIFICATION_BY_GROUP_UNSUPPORTED);
        }
        if (property == -1) {
            if (format == 0 && handle != 0 && handle != -1) {
                format = getObjectFormat(handle);
            }
            propertyGroup = this.mPropertyGroupsByFormat.get(Integer.valueOf(format));
            if (propertyGroup == null) {
                int[] propertyList = getSupportedObjectProperties(format);
                propertyGroup = new MtpPropertyGroup(this, this.mMediaProvider, this.mVolumeName, propertyList);
                this.mPropertyGroupsByFormat.put(Integer.valueOf(format), propertyGroup);
            }
        } else {
            propertyGroup = this.mPropertyGroupsByProperty.get(Integer.valueOf(property));
            if (propertyGroup == null) {
                int[] propertyList2 = {property};
                propertyGroup = new MtpPropertyGroup(this, this.mMediaProvider, this.mVolumeName, propertyList2);
                this.mPropertyGroupsByProperty.put(Integer.valueOf(property), propertyGroup);
            }
        }
        return propertyGroup.getPropertyList(handle, format, depth);
    }

    private int renameFile(int handle, String newName) {
        Cursor c = null;
        String path = null;
        String[] whereArgs = {Integer.toString(handle)};
        try {
            try {
                c = this.mMediaProvider.query(this.mObjectsUri, PATH_PROJECTION, ID_WHERE, whereArgs, null, null);
                if (c != null && c.moveToNext()) {
                    path = c.getString(1);
                }
                if (c != null) {
                    c.close();
                }
                if (path == null) {
                    return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
                }
                if (isStorageSubDirectory(path)) {
                    return MtpConstants.RESPONSE_OBJECT_WRITE_PROTECTED;
                }
                File oldFile = new File(path);
                int lastSlash = path.lastIndexOf(47);
                if (lastSlash <= 1) {
                    return 8194;
                }
                String newPath = path.substring(0, lastSlash + 1) + newName;
                File newFile = new File(newPath);
                boolean success = oldFile.renameTo(newFile);
                if (!success) {
                    Log.w(TAG, "renaming " + path + " to " + newPath + " failed");
                    return 8194;
                }
                ContentValues values = new ContentValues();
                values.put("_data", newPath);
                int updated = 0;
                try {
                    updated = this.mMediaProvider.update(this.mObjectsUri, values, ID_WHERE, whereArgs);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in mMediaProvider.update", e);
                }
                if (updated == 0) {
                    Log.e(TAG, "Unable to update path for " + path + " to " + newPath);
                    newFile.renameTo(oldFile);
                    return 8194;
                }
                if (newFile.isDirectory()) {
                    if (oldFile.getName().startsWith(".") && !newPath.startsWith(".")) {
                        try {
                            this.mMediaProvider.call(MediaStore.UNHIDE_CALL, newPath, null);
                            return MtpConstants.RESPONSE_OK;
                        } catch (RemoteException e2) {
                            Log.e(TAG, "failed to unhide/rescan for " + newPath);
                            return MtpConstants.RESPONSE_OK;
                        }
                    }
                    return MtpConstants.RESPONSE_OK;
                }
                if (oldFile.getName().toLowerCase(Locale.US).equals(MediaStore.MEDIA_IGNORE_FILENAME) && !newPath.toLowerCase(Locale.US).equals(MediaStore.MEDIA_IGNORE_FILENAME)) {
                    try {
                        this.mMediaProvider.call(MediaStore.UNHIDE_CALL, oldFile.getParent(), null);
                        return MtpConstants.RESPONSE_OK;
                    } catch (RemoteException e3) {
                        Log.e(TAG, "failed to unhide/rescan for " + newPath);
                        return MtpConstants.RESPONSE_OK;
                    }
                }
                return MtpConstants.RESPONSE_OK;
            } catch (RemoteException e4) {
                Log.e(TAG, "RemoteException in getObjectFilePath", e4);
                if (c != null) {
                    c.close();
                }
                return 8194;
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private int setObjectProperty(int handle, int property, long intValue, String stringValue) {
        switch (property) {
            case MtpConstants.PROPERTY_OBJECT_FILE_NAME:
                return renameFile(handle, stringValue);
            default:
                return MtpConstants.RESPONSE_OBJECT_PROP_NOT_SUPPORTED;
        }
    }

    private int getDeviceProperty(int property, long[] outIntValue, char[] outStringValue) {
        Log.d(TAG, "getDeviceProperty  property = 0x" + Integer.toHexString(property));
        switch (property) {
            case MtpConstants.DEVICE_PROPERTY_IMAGE_SIZE:
                Display display = ((WindowManager) this.mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                int width = display.getMaximumSizeDimension();
                int height = display.getMaximumSizeDimension();
                String imageSize = Integer.toString(width) + "x" + Integer.toString(height);
                imageSize.getChars(0, imageSize.length(), outStringValue, 0);
                outStringValue[imageSize.length()] = 0;
                break;
            case MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER:
            case MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME:
                String value = this.mDeviceProperties.getString(Integer.toString(property), ProxyInfo.LOCAL_EXCL_LIST);
                int length = value.length();
                if (length > 255) {
                    length = 255;
                }
                value.getChars(0, length, outStringValue, 0);
                outStringValue[length] = 0;
                break;
        }
        return MtpConstants.RESPONSE_OK;
    }

    private int setDeviceProperty(int property, long intValue, String stringValue) {
        switch (property) {
            case MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER:
            case MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME:
                SharedPreferences.Editor e = this.mDeviceProperties.edit();
                e.putString(Integer.toString(property), stringValue);
                if (e.commit()) {
                    return MtpConstants.RESPONSE_OK;
                }
                return 8194;
            default:
                return MtpConstants.RESPONSE_DEVICE_PROP_NOT_SUPPORTED;
        }
    }

    private boolean getObjectInfo(int handle, int[] outStorageFormatParent, char[] outName, long[] outCreatedModified) {
        Cursor c = null;
        Log.d(TAG, "getObjectInfo");
        try {
            try {
                c = this.mMediaProvider.query(this.mObjectsUri, OBJECT_INFO_PROJECTION, ID_WHERE, new String[]{Integer.toString(handle)}, null, null);
                if (c == null || !c.moveToNext()) {
                    if (c == null) {
                        return false;
                    }
                    c.close();
                    return false;
                }
                outStorageFormatParent[0] = c.getInt(1);
                outStorageFormatParent[1] = c.getInt(2);
                outStorageFormatParent[2] = c.getInt(3);
                String path = c.getString(4);
                int lastSlash = path.lastIndexOf(47);
                int start = lastSlash >= 0 ? lastSlash + 1 : 0;
                int end = path.length();
                if (end - start > 255) {
                    end = start + 255;
                }
                path.getChars(start, end, outName, 0);
                outName[end - start] = 0;
                outCreatedModified[0] = c.getLong(5);
                outCreatedModified[1] = c.getLong(6);
                if (outCreatedModified[0] == 0) {
                    outCreatedModified[0] = outCreatedModified[1];
                }
                if (c != null) {
                    c.close();
                }
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in getObjectInfo", e);
                if (c == null) {
                    return false;
                }
                c.close();
                return false;
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private int getObjectFilePath(int handle, char[] outFilePath, long[] outFileLengthFormat) {
        Log.d(TAG, "getObjectFilePath handle = " + Integer.toHexString(handle));
        if (handle == 0) {
            this.mMediaStoragePath.getChars(0, this.mMediaStoragePath.length(), outFilePath, 0);
            outFilePath[this.mMediaStoragePath.length()] = 0;
            outFileLengthFormat[0] = 0;
            outFileLengthFormat[1] = 12289;
            return MtpConstants.RESPONSE_OK;
        }
        AutoCloseable autoCloseable = null;
        try {
            try {
                Cursor c = this.mMediaProvider.query(this.mObjectsUri, PATH_FORMAT_PROJECTION, ID_WHERE, new String[]{Integer.toString(handle)}, null, null);
                if (c == null || !c.moveToNext()) {
                    Log.e(TAG, "getObjectFilePath RESPONSE_INVALID_OBJECT_HANDLE, handle = " + handle);
                    if (c != null) {
                        c.close();
                    }
                    return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
                }
                String path = c.getString(1);
                path.getChars(0, path.length(), outFilePath, 0);
                outFilePath[path.length()] = 0;
                outFileLengthFormat[0] = new File(path).length();
                outFileLengthFormat[1] = c.getLong(2);
                Log.d(TAG, "getObjectFilePath RESPONSE_OK: path = " + path);
                if (c != null) {
                    c.close();
                }
                return MtpConstants.RESPONSE_OK;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in getObjectFilePath", e);
                if (0 != 0) {
                    autoCloseable.close();
                }
                return 8194;
            }
        } catch (Throwable th) {
            if (0 != 0) {
                autoCloseable.close();
            }
            throw th;
        }
    }

    private int getObjectFormat(int handle) {
        Cursor c = null;
        try {
            try {
                c = this.mMediaProvider.query(this.mObjectsUri, FORMAT_PROJECTION, ID_WHERE, new String[]{Integer.toString(handle)}, null, null);
                if (c == null || !c.moveToNext()) {
                    if (c != null) {
                        c.close();
                    }
                    return -1;
                }
                int i = c.getInt(1);
                if (c != null) {
                    c.close();
                }
                return i;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in getObjectFilePath", e);
                if (c != null) {
                    c.close();
                }
                return -1;
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private int deleteFile(int handle) {
        this.mDatabaseModified = true;
        Log.d(TAG, "deleteFile: handle = 0x" + Integer.toHexString(handle));
        Cursor c = null;
        try {
            try {
                c = this.mMediaProvider.query(this.mObjectsUri, PATH_FORMAT_PROJECTION, ID_WHERE, new String[]{Integer.toString(handle)}, null, null);
                if (c == null || !c.moveToNext()) {
                    if (c != null) {
                        c.close();
                    }
                    return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
                }
                String path = c.getString(1);
                int format = c.getInt(2);
                if (path == null || format == 0) {
                    if (c != null) {
                        c.close();
                    }
                    return 8194;
                }
                Log.d(TAG, "deleteFile: handle = 0x" + Integer.toHexString(handle) + ", path = " + path + ", format = 0x" + Integer.toHexString(format));
                if (isStorageSubDirectory(path)) {
                    if (c != null) {
                        c.close();
                    }
                    return MtpConstants.RESPONSE_OBJECT_WRITE_PROTECTED;
                }
                if (format == 12289) {
                    Uri uri = MediaStore.Files.getMtpObjectsUri(this.mVolumeName);
                    this.mMediaProvider.delete(uri, "_data LIKE ?1 AND lower(substr(_data,1,?2))=lower(?3)", new String[]{path + "/%", Integer.toString(path.length() + 1), path + "/"});
                }
                Uri uri2 = MediaStore.Files.getMtpObjectsUri(this.mVolumeName, handle);
                if (this.mMediaProvider.delete(uri2, null, null) <= 0) {
                    if (c != null) {
                        c.close();
                    }
                    return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
                }
                if (format != 12289 && path.toLowerCase(Locale.US).endsWith("/.nomedia")) {
                    try {
                        String parentPath = path.substring(0, path.lastIndexOf("/"));
                        this.mMediaProvider.call(MediaStore.UNHIDE_CALL, parentPath, null);
                    } catch (RemoteException e) {
                        Log.e(TAG, "failed to unhide/rescan for " + path);
                    }
                }
                if (c != null) {
                    c.close();
                }
                return MtpConstants.RESPONSE_OK;
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
                throw th;
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in deleteFile", e2);
            if (c != null) {
                c.close();
            }
            return 8194;
        }
    }

    private int[] getObjectReferences(int handle) {
        Uri uri = MediaStore.Files.getMtpReferencesUri(this.mVolumeName, handle);
        AutoCloseable autoCloseable = null;
        try {
            try {
                Cursor c = this.mMediaProvider.query(uri, ID_PROJECTION, null, null, null, null);
                if (c == null) {
                    if (c != null) {
                        c.close();
                    }
                    return null;
                }
                int count = c.getCount();
                if (count > 0) {
                    int[] result = new int[count];
                    for (int i = 0; i < count; i++) {
                        c.moveToNext();
                        result[i] = c.getInt(0);
                    }
                    if (c != null) {
                        c.close();
                    }
                    return result;
                }
                if (c != null) {
                    c.close();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in getObjectList", e);
                if (0 != 0) {
                    autoCloseable.close();
                }
            }
            return null;
        } catch (Throwable th) {
            if (0 != 0) {
                autoCloseable.close();
            }
            throw th;
        }
    }

    private int setObjectReferences(int handle, int[] references) {
        this.mDatabaseModified = true;
        Uri uri = MediaStore.Files.getMtpReferencesUri(this.mVolumeName, handle);
        int count = references.length;
        ContentValues[] valuesList = new ContentValues[count];
        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put("_id", Integer.valueOf(references[i]));
            valuesList[i] = values;
        }
        try {
            if (this.mMediaProvider.bulkInsert(uri, valuesList) > 0) {
                return MtpConstants.RESPONSE_OK;
            }
            return 8194;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setObjectReferences", e);
            return 8194;
        }
    }

    private void sessionStarted() {
        Log.d(TAG, "sessionStarted");
        this.mDatabaseModified = false;
    }

    private void sessionEnded() {
        Log.d(TAG, "sessionEnded, mDatabaseModified = " + this.mDatabaseModified);
        if (!this.mDatabaseModified) {
            return;
        }
        this.mContext.sendBroadcast(new Intent(MediaStore.ACTION_MTP_SESSION_END));
        this.mDatabaseModified = false;
    }
}
