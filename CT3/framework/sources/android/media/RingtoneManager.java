package android.media;

import android.Manifest;
import android.app.Activity;
import android.app.backup.FullBackup;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.DrmStore;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.database.SortCursor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import libcore.io.Streams;

public class RingtoneManager {
    public static final String ACTION_RINGTONE_PICKER = "android.intent.action.RINGTONE_PICKER";
    private static final int DRM_LEVEL_ALL = 4;
    private static final int DRM_LEVEL_FL = 1;
    private static final int DRM_LEVEL_SD = 2;
    private static final String EXTRA_DRM_LEVEL = "android.intent.extra.drm_level";
    public static final String EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS = "android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS";
    public static final String EXTRA_RINGTONE_DEFAULT_URI = "android.intent.extra.ringtone.DEFAULT_URI";
    public static final String EXTRA_RINGTONE_EXISTING_URI = "android.intent.extra.ringtone.EXISTING_URI";

    @Deprecated
    public static final String EXTRA_RINGTONE_INCLUDE_DRM = "android.intent.extra.ringtone.INCLUDE_DRM";
    public static final String EXTRA_RINGTONE_PICKED_POSITION = "android.intent.extra.ringtone.PICKED_POSITION";
    public static final String EXTRA_RINGTONE_PICKED_URI = "android.intent.extra.ringtone.PICKED_URI";
    public static final String EXTRA_RINGTONE_SHOW_DEFAULT = "android.intent.extra.ringtone.SHOW_DEFAULT";
    public static final String EXTRA_RINGTONE_SHOW_MORE_RINGTONES = "android.intent.extra.ringtone.SHOW_MORE_RINGTONES";
    public static final String EXTRA_RINGTONE_SHOW_SILENT = "android.intent.extra.ringtone.SHOW_SILENT";
    public static final String EXTRA_RINGTONE_TITLE = "android.intent.extra.ringtone.TITLE";
    public static final String EXTRA_RINGTONE_TYPE = "android.intent.extra.ringtone.TYPE";
    public static final int ID_COLUMN_INDEX = 0;
    private static final String TAG = "RingtoneManager";
    public static final int TITLE_COLUMN_INDEX = 1;
    public static final int TYPE_ALARM = 4;
    public static final int TYPE_ALL = 7;
    public static final int TYPE_NOTIFICATION = 2;
    public static final int TYPE_RINGTONE = 1;
    public static final int TYPE_SIP_CALL = 16;
    public static final int TYPE_VIDEO_CALL = 8;
    public static final int URI_COLUMN_INDEX = 2;
    private final Activity mActivity;
    private final Context mContext;
    private Cursor mCursor;
    private final List<String> mFilterColumns;
    private boolean mIncludeDrm;
    private Ringtone mPreviousRingtone;
    private boolean mStopPreviousRingtone;
    private int mType;
    private static final String[] INTERNAL_COLUMNS = {"_id", "title", "\"" + MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "\"", "title_key"};
    private static final String[] DRM_COLUMNS = {"_id", "title", "\"" + DrmStore.Audio.CONTENT_URI + "\"", "title AS title_key"};
    private static final String[] MEDIA_COLUMNS = {"_id", "title", "\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\"", "title_key", MediaStore.MediaColumns.IS_DRM, MediaStore.MediaColumns.DRM_METHOD};

    public RingtoneManager(Activity activity) {
        this.mType = 1;
        this.mFilterColumns = new ArrayList();
        this.mStopPreviousRingtone = true;
        this.mActivity = activity;
        this.mContext = activity;
        setType(this.mType);
    }

    public RingtoneManager(Context context) {
        this.mType = 1;
        this.mFilterColumns = new ArrayList();
        this.mStopPreviousRingtone = true;
        this.mActivity = null;
        this.mContext = context;
        setType(this.mType);
    }

    public void setType(int type) {
        if (this.mCursor != null) {
            throw new IllegalStateException("Setting filter columns should be done before querying for ringtones.");
        }
        this.mType = type;
        setFilterColumnsList(type);
    }

    public int inferStreamType() {
        switch (this.mType) {
            case 2:
                return 5;
            case 3:
            default:
                return 2;
            case 4:
                return 4;
        }
    }

    public void setStopPreviousRingtone(boolean stopPreviousRingtone) {
        this.mStopPreviousRingtone = stopPreviousRingtone;
    }

    public boolean getStopPreviousRingtone() {
        return this.mStopPreviousRingtone;
    }

    public void stopPreviousRingtone() {
        if (this.mPreviousRingtone == null) {
            return;
        }
        this.mPreviousRingtone.stop();
    }

    @Deprecated
    public boolean getIncludeDrm() {
        return false;
    }

    @Deprecated
    public void setIncludeDrm(boolean includeDrm) {
        if (!includeDrm) {
            return;
        }
        Log.w(TAG, "setIncludeDrm no longer supported");
    }

    public Cursor getCursor() {
        if (this.mCursor != null && this.mCursor.requery()) {
            Log.v(TAG, "getCursor with old cursor = " + this.mCursor);
            return this.mCursor;
        }
        Cursor internalCursor = getInternalRingtones();
        Cursor drmRingtones = this.mIncludeDrm ? getDrmRingtones() : null;
        Cursor mediaCursor = getMediaRingtones();
        this.mCursor = new SortCursor(new Cursor[]{internalCursor, drmRingtones, mediaCursor}, "title_key");
        Log.v(TAG, "mCursor.hashCode " + this.mCursor.hashCode());
        Log.v(TAG, "getCursor with new cursor = " + this.mCursor);
        return this.mCursor;
    }

    public Ringtone getRingtone(int position) {
        if (this.mStopPreviousRingtone && this.mPreviousRingtone != null) {
            this.mPreviousRingtone.stop();
        }
        this.mPreviousRingtone = getRingtone(this.mContext, getRingtoneUri(position), inferStreamType());
        return this.mPreviousRingtone;
    }

    public Uri getRingtoneUri(int position) {
        if (this.mCursor == null) {
            Log.v(TAG, "mCursor is null");
            return null;
        }
        try {
            if (this.mCursor.isClosed() || !this.mCursor.moveToPosition(position)) {
                Log.v(TAG, "mCursor position is wrong");
                return null;
            }
            return getUriFromCursor(this.mCursor);
        } catch (IllegalStateException e) {
            Log.v(TAG, "mCursor exception");
            return null;
        }
    }

    private static Uri getUriFromCursor(Cursor cursor) {
        return ContentUris.withAppendedId(Uri.parse(cursor.getString(2)), cursor.getLong(0));
    }

    public int getRingtonePosition(Uri ringtoneUri) {
        if (ringtoneUri == null) {
            return -1;
        }
        Cursor cursor = getCursor();
        cursor.getCount();
        return queryPosition(cursor, ringtoneUri);
    }

    private int validRingtoneForMTCall(Uri ringtoneUri) {
        if (ringtoneUri == null) {
            return -1;
        }
        Cursor cursor = getCursor();
        try {
            return queryPosition(cursor, ringtoneUri);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                Log.d(TAG, "Cursor already closed.");
            }
        }
    }

    private int queryPosition(Cursor cursor, Uri ringtoneUri) {
        int cursorCount = cursor.getCount();
        if (!cursor.moveToFirst()) {
            return -1;
        }
        Uri currentUri = null;
        String previousUriString = null;
        for (int i = 0; i < cursorCount; i++) {
            String uriString = cursor.getString(2);
            if (currentUri == null || !uriString.equals(previousUriString)) {
                currentUri = Uri.parse(uriString);
            }
            if (ringtoneUri.equals(ContentUris.withAppendedId(currentUri, cursor.getLong(0)))) {
                return i;
            }
            cursor.move(1);
            previousUriString = uriString;
        }
        return -1;
    }

    public static Uri getValidRingtoneUri(Context context) {
        RingtoneManager rm = new RingtoneManager(context);
        Uri uri = getValidRingtoneUriFromCursorAndClose(context, rm.getInternalRingtones());
        if (uri == null) {
            uri = getValidRingtoneUriFromCursorAndClose(context, rm.getMediaRingtones());
        }
        if (uri == null) {
            return getValidRingtoneUriFromCursorAndClose(context, rm.getDrmRingtones());
        }
        return uri;
    }

    private static Uri getValidRingtoneUriFromCursorAndClose(Context context, Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        Uri uri = null;
        if (cursor.moveToFirst()) {
            uri = getUriFromCursor(cursor);
        }
        cursor.close();
        return uri;
    }

    private Cursor getInternalRingtones() {
        return query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, INTERNAL_COLUMNS, constructBooleanTrueWhereClause(this.mFilterColumns, this.mIncludeDrm) + appendDrmToWhereClause(), null, "title_key");
    }

    private Cursor getDrmRingtones() {
        return query(DrmStore.Audio.CONTENT_URI, DRM_COLUMNS, null, null, "title");
    }

    private Cursor getMediaRingtones() {
        if (this.mContext.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) != 0) {
            Log.w(TAG, "No READ_EXTERNAL_STORAGE permission, ignoring ringtones on ext storage");
            return null;
        }
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED) || status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            return query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MEDIA_COLUMNS, constructBooleanTrueWhereClause(this.mFilterColumns, this.mIncludeDrm) + appendDrmToWhereClause(), null, "title_key");
        }
        return null;
    }

    private void setFilterColumnsList(int type) {
        List<String> columns = this.mFilterColumns;
        columns.clear();
        if ((type & 1) != 0) {
            columns.add(MediaStore.Audio.AudioColumns.IS_RINGTONE);
        }
        if ((type & 2) != 0) {
            columns.add(MediaStore.Audio.AudioColumns.IS_NOTIFICATION);
        }
        if ((type & 4) == 0) {
            return;
        }
        columns.add(MediaStore.Audio.AudioColumns.IS_ALARM);
    }

    private static String constructBooleanTrueWhereClause(List<String> columns, boolean includeDrm) {
        if (columns == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = columns.size() - 1; i >= 0; i--) {
            sb.append(columns.get(i)).append("=1 or ");
        }
        if (columns.size() > 0) {
            sb.setLength(sb.length() - 4);
        }
        sb.append(")");
        return sb.toString();
    }

    private String appendDrmToWhereClause() {
        if (this.mActivity == null) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(" and ");
        sb.append("(");
        sb.append(MediaStore.MediaColumns.IS_DRM).append("!=1");
        Intent it = this.mActivity.getIntent();
        if (it != null) {
            int extraValue = it.getIntExtra(EXTRA_DRM_LEVEL, 1);
            if (extraValue == 1) {
                sb.append(" or ").append(MediaStore.MediaColumns.DRM_METHOD).append("=1");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return this.mContext.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public static Ringtone getRingtone(Context context, Uri ringtoneUri) {
        return getRingtone(context, ringtoneUri, -1);
    }

    private static Ringtone getRingtone(Context context, Uri ringtoneUri, int streamType) {
        Log.d(TAG, "getRingtone() ringtoneUri = " + ringtoneUri + ", streamType = " + streamType);
        try {
            Ringtone r = new Ringtone(context, true);
            if (streamType >= 0) {
                r.setStreamType(streamType);
            }
            r.setUri(ringtoneUri);
            return r;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to open ringtone " + ringtoneUri + ": " + ex);
            return null;
        }
    }

    private static int validRingtoneUri(Context context, Uri ringtoneUri, int type) {
        RingtoneManager rm = new RingtoneManager(context);
        rm.setType(type);
        return rm.validRingtoneForMTCall(ringtoneUri);
    }

    public static Uri getActualDefaultRingtoneUri(Context context, int type) {
        String setting = getSettingForType(type);
        if (setting == null) {
            return null;
        }
        String uriString = Settings.System.getStringForUser(context.getContentResolver(), setting, context.getUserId());
        Log.i(TAG, "Get actual default ringtone uri= " + uriString);
        if (uriString != null) {
            return Uri.parse(uriString);
        }
        return null;
    }

    public static void setActualDefaultRingtoneUri(Context context, int type, Uri ringtoneUri) {
        Throwable th = null;
        ContentResolver resolver = context.getContentResolver();
        String setting = getSettingForType(type);
        if (setting == null) {
            return;
        }
        Settings.System.putStringForUser(resolver, setting, ringtoneUri != null ? ringtoneUri.toString() : null, context.getUserId());
        if (ringtoneUri != null) {
            Uri cacheUri = getCacheForType(type);
            InputStream in = null;
            OutputStream out = null;
            try {
                try {
                    in = openRingtone(context, ringtoneUri);
                    out = resolver.openOutputStream(cacheUri);
                    Streams.copy(in, out);
                    if (out != null) {
                        try {
                            out.close();
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable th3) {
                            th = th3;
                            if (th != null) {
                                if (th != th) {
                                    th.addSuppressed(th);
                                    th = th;
                                }
                            }
                            if (th != null) {
                            }
                            Log.i(TAG, "Set actual default ringtone uri= " + ringtoneUri);
                        }
                        th = th;
                        if (th != null) {
                            throw th;
                        }
                    } else {
                        th = th;
                        if (th != null) {
                        }
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Failed to cache ringtone: " + e);
                }
            } catch (Throwable th4) {
                th = th4;
                if (out == null) {
                }
                if (in != null) {
                }
                if (th != null) {
                }
            }
        }
        Log.i(TAG, "Set actual default ringtone uri= " + ringtoneUri);
    }

    private static InputStream openRingtone(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        try {
            return resolver.openInputStream(uri);
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Failed to open directly; attempting failover: " + e);
            IRingtonePlayer player = ((AudioManager) context.getSystemService(AudioManager.class)).getRingtonePlayer();
            try {
                return new ParcelFileDescriptor.AutoCloseInputStream(player.openRingtone(uri));
            } catch (Exception e2) {
                throw new IOException(e2);
            }
        }
    }

    private static String getSettingForType(int type) {
        if ((type & 1) != 0) {
            return Settings.System.RINGTONE;
        }
        if ((type & 2) != 0) {
            return Settings.System.NOTIFICATION_SOUND;
        }
        if ((type & 4) != 0) {
            return Settings.System.ALARM_ALERT;
        }
        return null;
    }

    public static Uri getCacheForType(int type) {
        if ((type & 1) != 0) {
            return Settings.System.RINGTONE_CACHE_URI;
        }
        if ((type & 2) != 0) {
            return Settings.System.NOTIFICATION_SOUND_CACHE_URI;
        }
        if ((type & 4) != 0) {
            return Settings.System.ALARM_ALERT_CACHE_URI;
        }
        return null;
    }

    public static boolean isDefault(Uri ringtoneUri) {
        return getDefaultType(ringtoneUri) != -1;
    }

    public static int getDefaultType(Uri defaultRingtoneUri) {
        if (defaultRingtoneUri == null) {
            return -1;
        }
        if (defaultRingtoneUri.equals(Settings.System.DEFAULT_RINGTONE_URI)) {
            return 1;
        }
        if (defaultRingtoneUri.equals(Settings.System.DEFAULT_NOTIFICATION_URI)) {
            return 2;
        }
        return defaultRingtoneUri.equals(Settings.System.DEFAULT_ALARM_ALERT_URI) ? 4 : -1;
    }

    public static Uri getDefaultUri(int type) {
        if ((type & 1) != 0) {
            return Settings.System.DEFAULT_RINGTONE_URI;
        }
        if ((type & 2) != 0) {
            return Settings.System.DEFAULT_NOTIFICATION_URI;
        }
        if ((type & 4) != 0) {
            return Settings.System.DEFAULT_ALARM_ALERT_URI;
        }
        return null;
    }

    public Cursor getNewCursor() {
        Cursor internalCursor = getInternalRingtones();
        Cursor drmRingtones = this.mIncludeDrm ? getDrmRingtones() : null;
        Cursor mediaCursor = getMediaRingtones();
        this.mCursor = new SortCursor(new Cursor[]{internalCursor, drmRingtones, mediaCursor}, "title_key");
        Log.v(TAG, "getNewCursor mCursor.hashCode " + this.mCursor.hashCode());
        Log.v(TAG, "getNewCursor with cursor = " + this.mCursor);
        return this.mCursor;
    }

    public static boolean isRingtoneExist(Context context, Uri uri) {
        boolean exist;
        if (uri == null) {
            Log.e(TAG, "Check ringtone exist with null uri!");
            return false;
        }
        try {
            AssetFileDescriptor fd = context.getContentResolver().openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            if (fd == null) {
                exist = false;
            } else {
                fd.close();
                exist = true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            exist = false;
        } catch (IOException e2) {
            e2.printStackTrace();
            exist = true;
        }
        Log.d(TAG, uri + " is exist " + exist);
        return exist;
    }
}
