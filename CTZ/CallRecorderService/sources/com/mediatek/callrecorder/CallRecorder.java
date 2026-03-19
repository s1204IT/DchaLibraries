package com.mediatek.callrecorder;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Slog;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class CallRecorder extends Recorder {
    private static final String TAG = CallRecorder.class.getSimpleName();
    private static CallRecorder sCallRecorder;
    private String mAudioDBPlaylistName;
    private String mRequestedType;

    private CallRecorder(Context context) {
        super(context);
        this.mRequestedType = "audio/3gpp";
        this.mAudioDBPlaylistName = this.mContext.getString(R.string.str_db_playlist_name);
    }

    public static synchronized CallRecorder getInstance(Context context) {
        if (sCallRecorder == null) {
            sCallRecorder = new CallRecorder(context);
        }
        return sCallRecorder;
    }

    public boolean isRecording() {
        return sIsRecording;
    }

    @Override
    protected void onMediaServiceError() {
        log("onMediaServiceError, sIsRecording: " + sIsRecording);
        if (!sIsRecording) {
            return;
        }
        sIsRecording = false;
        stopRecording();
    }

    public void startRecord() {
        log("startRecord, mRequestedType = " + this.mRequestedType);
        if (sIsRecording) {
            log("return because recording is ongoing");
            return;
        }
        if (RecorderUtils.isStorageAvailable(this.mContext)) {
            sIsRecording = true;
            try {
                if ("audio/amr".equals(this.mRequestedType)) {
                    startRecording(3, ".amr");
                    return;
                }
                if (!"audio/3gpp".equals(this.mRequestedType) && !"audio/*".equals(this.mRequestedType)) {
                    sIsRecording = false;
                    throw new IllegalArgumentException("Invalid output file type requested");
                }
                startRecording(1, ".3gpp");
                return;
            } catch (IOException e) {
                Slog.e(TAG, "--------IOException occurred------");
                sIsRecording = false;
                return;
            }
        }
        sIsRecording = false;
        showToast(R.string.alert_storage_is_not_available);
        setState(0, true);
    }

    public void stopRecord() {
        if (!sIsRecording) {
            return;
        }
        sIsRecording = false;
        log("stopRecord");
        stopRecording();
        boolean zIsStorageAvailable = RecorderUtils.isStorageAvailable(this.mContext);
        log("stopRecord: storage available: " + zIsStorageAvailable);
        if (zIsStorageAvailable) {
            saveSample();
            showToastInClient(this.mContext.getResources().getString(R.string.confirm_save_info_saved_to) + "\n" + getExactRecordingPath(getRecordingPath()));
        } else {
            deleteSampleFile();
            showToastInClient(R.string.ext_media_badremoval_notification_title);
        }
        setState(0);
    }

    private boolean saveSample() {
        if (this.mSampleLength == 0) {
            return false;
        }
        try {
            return addToMediaDB(this.mSampleFile) != null;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    private Uri addToMediaDB(File file) {
        Uri uriInsert;
        ContentValues contentValues = new ContentValues();
        Date date = new Date(System.currentTimeMillis());
        DateFormat.getTimeFormat(this.mContext).format(date);
        DateFormat.getDateFormat(this.mContext).format(date);
        String absolutePath = file.getAbsolutePath();
        String strSubstring = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
        contentValues.put("title", strSubstring.substring(0, strSubstring.lastIndexOf(".")));
        contentValues.put("_data", file.getAbsolutePath());
        contentValues.put("mime_type", this.mRequestedType);
        contentValues.put("album", "PhoneRecord");
        contentValues.put("duration", Long.valueOf(this.mSampleLength));
        ContentResolver contentResolver = this.mContext.getContentResolver();
        try {
            uriInsert = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Cannot add to Media database: %s", e);
            uriInsert = null;
        }
        if (uriInsert == null) {
            Slog.e(TAG, "----- Unable to save recorded audio !!");
            return null;
        }
        if (getPlaylistId() == -1) {
            createPlaylist(contentResolver);
        }
        addToPlaylist(contentResolver, Integer.valueOf(uriInsert.getLastPathSegment()).intValue(), getPlaylistId());
        this.mContext.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", uriInsert));
        MediaScannerConnection.scanFile(this.mContext, new String[]{file.getAbsolutePath()}, null, null);
        return uriInsert;
    }

    private int getPlaylistId() {
        this.mAudioDBPlaylistName = this.mContext.getString(R.string.str_db_playlist_name);
        Cursor cursorQuery = query(MediaStore.Audio.Playlists.getContentUri("external"), new String[]{"_id"}, "name=?", new String[]{this.mAudioDBPlaylistName}, null);
        if (cursorQuery == null) {
            Slog.v(TAG, "query returns null");
        }
        int i = -1;
        if (cursorQuery != null) {
            cursorQuery.moveToFirst();
            if (!cursorQuery.isAfterLast()) {
                i = cursorQuery.getInt(0);
            }
            cursorQuery.close();
        }
        return i;
    }

    private Uri createPlaylist(ContentResolver contentResolver) {
        ContentValues contentValues = new ContentValues();
        this.mAudioDBPlaylistName = this.mContext.getString(R.string.str_db_playlist_name);
        contentValues.put("name", this.mAudioDBPlaylistName);
        Uri uriInsert = contentResolver.insert(MediaStore.Audio.Playlists.getContentUri("external"), contentValues);
        if (uriInsert == null) {
            Slog.e(TAG, "---- Unable to save recorded audio -----");
        }
        return uriInsert;
    }

    private void addToPlaylist(ContentResolver contentResolver, int i, long j) {
        Uri contentUri = MediaStore.Audio.Playlists.Members.getContentUri("external", j);
        Cursor cursorQuery = contentResolver.query(contentUri, new String[]{"count(*)"}, null, null, null);
        if (cursorQuery != null) {
            cursorQuery.moveToFirst();
            int i2 = cursorQuery.getInt(0);
            cursorQuery.close();
            ContentValues contentValues = new ContentValues();
            contentValues.put("play_order", Integer.valueOf(i2 + i));
            contentValues.put("audio_id", Integer.valueOf(i));
            contentResolver.insert(contentUri, contentValues);
        }
    }

    private Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        try {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            if (contentResolver == null) {
                return null;
            }
            return contentResolver.query(uri, strArr, str, strArr2, str2);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    protected String getExactRecordingPath(String str) {
        log("getExactRecordingPath(): path is: " + str);
        StorageVolume[] volumeList = ((StorageManager) this.mContext.getSystemService("storage")).getVolumeList();
        if (volumeList != null) {
            for (StorageVolume storageVolume : volumeList) {
                String description = storageVolume.getDescription(this.mContext);
                String str2 = storageVolume.getPath() + "/";
                log("getExactRecordingPath(): volDes is: " + description + ", volPath is: " + str2);
                if (str != null && str.indexOf(str2) > -1) {
                    String strSubstring = str.substring(str2.length() - 1);
                    String str3 = description + strSubstring;
                    log("getExactRecordingPath(): exactPath is: " + str3 + ", subPath is: " + strSubstring);
                    return str3;
                }
            }
        }
        return "";
    }

    private void log(String str) {
        Slog.d(TAG, str);
    }
}
