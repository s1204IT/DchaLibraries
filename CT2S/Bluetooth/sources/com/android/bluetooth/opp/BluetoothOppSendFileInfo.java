package com.android.bluetooth.opp;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.EventLog;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BluetoothOppSendFileInfo {
    private static final boolean D = true;
    static final BluetoothOppSendFileInfo SEND_FILE_INFO_ERROR = new BluetoothOppSendFileInfo(null, null, 0, null, BluetoothShare.STATUS_FILE_ERROR);
    private static final String TAG = "BluetoothOppSendFileInfo";
    private static final boolean V = false;
    public final String mData;
    public final String mFileName;
    public final FileInputStream mInputStream;
    public final long mLength;
    public final String mMimetype;
    public final int mStatus;

    public BluetoothOppSendFileInfo(String fileName, String type, long length, FileInputStream inputStream, int status) {
        this.mFileName = fileName;
        this.mMimetype = type;
        this.mLength = length;
        this.mInputStream = inputStream;
        this.mStatus = status;
        this.mData = null;
    }

    public BluetoothOppSendFileInfo(String data, String type, long length, int status) {
        this.mFileName = null;
        this.mInputStream = null;
        this.mData = data;
        this.mMimetype = type;
        this.mLength = length;
        this.mStatus = status;
    }

    public static String getFileName(String path) {
        if (path == null) {
            return null;
        }
        String[] component = path.split("/");
        return component[component.length - 1];
    }

    public static BluetoothOppSendFileInfo generateFileInfo(Context context, Uri uri, String type, boolean fromExternal) throws Throwable {
        String fileName;
        String contentType;
        long length;
        Cursor metadataCursor;
        ContentResolver contentResolver = context.getContentResolver();
        String scheme = uri.getScheme();
        if ("content".equals(scheme)) {
            String contentType2 = contentResolver.getType(uri);
            try {
                metadataCursor = contentResolver.query(uri, new String[]{"_display_name", "_size"}, null, null, null);
            } catch (SQLiteException e) {
                metadataCursor = null;
            }
            if (metadataCursor == null) {
                length = 0;
                fileName = null;
            } else {
                try {
                    if (!metadataCursor.moveToFirst()) {
                        length = 0;
                        fileName = null;
                    } else {
                        fileName = metadataCursor.getString(0);
                        try {
                            length = metadataCursor.getInt(1);
                        } catch (Throwable th) {
                            th = th;
                        }
                        try {
                            Log.d(TAG, "fileName = " + fileName + " length = " + length);
                        } catch (Throwable th2) {
                            th = th2;
                            metadataCursor.close();
                            throw th;
                        }
                    }
                    metadataCursor.close();
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            if (fileName == null) {
                fileName = uri.getLastPathSegment();
            }
            contentType = contentType2;
        } else if ("file".equals(scheme)) {
            if (uri.getPath() == null) {
                Log.e(TAG, "Invalid URI path: " + uri);
                return SEND_FILE_INFO_ERROR;
            }
            if (fromExternal && !BluetoothOppUtility.isInExternalStorageDir(uri)) {
                EventLog.writeEvent(1397638484, "35310991", -1, uri.getPath());
                Log.e(TAG, "File based URI not in Environment.getExternalStorageDirectory() is not allowed.");
                return SEND_FILE_INFO_ERROR;
            }
            fileName = uri.getLastPathSegment();
            contentType = type;
            File f = new File(uri.getPath());
            length = f.length();
        } else {
            return SEND_FILE_INFO_ERROR;
        }
        FileInputStream is = null;
        if (scheme.equals("content")) {
            try {
                AssetFileDescriptor fd = contentResolver.openAssetFileDescriptor(uri, "r");
                long statLength = fd.getLength();
                if (length != statLength && statLength > 0) {
                    Log.e(TAG, "Content provider length is wrong (" + Long.toString(length) + "), using stat length (" + Long.toString(statLength) + ")");
                    length = statLength;
                }
                try {
                    is = fd.createInputStream();
                } catch (IOException e2) {
                    try {
                        fd.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (FileNotFoundException e4) {
            }
        }
        if (is == null) {
            try {
                is = (FileInputStream) contentResolver.openInputStream(uri);
            } catch (FileNotFoundException e5) {
                return SEND_FILE_INFO_ERROR;
            }
        }
        if (length == 0) {
            try {
                length = is.available();
            } catch (IOException e6) {
                Log.e(TAG, "Read stream exception: ", e6);
                return SEND_FILE_INFO_ERROR;
            }
        }
        return new BluetoothOppSendFileInfo(getFileName(fileName), contentType, length, is, 0);
    }
}
