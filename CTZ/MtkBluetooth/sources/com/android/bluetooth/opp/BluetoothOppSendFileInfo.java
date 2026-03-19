package com.android.bluetooth.opp;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.v4.os.EnvironmentCompat;
import android.util.EventLog;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.android.bluetooth.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BluetoothOppSendFileInfo {
    private static final String TAG = "BluetoothOppSendFileInfo";
    public final String mData;
    public final String mFileName;
    public String mFilePath;
    public final FileInputStream mInputStream;
    public final long mLength;
    public final String mMimetype;
    public final int mStatus;
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;
    static final BluetoothOppSendFileInfo SEND_FILE_INFO_ERROR = new BluetoothOppSendFileInfo(null, null, 0, null, BluetoothShare.STATUS_FILE_ERROR);
    static final BluetoothOppSendFileInfo SEND_FILE_INFO_NOT_ACCEPTABLE = new BluetoothOppSendFileInfo(null, null, 0, null, BluetoothShare.STATUS_NOT_ACCEPTABLE);

    public BluetoothOppSendFileInfo(String str, String str2, long j, FileInputStream fileInputStream, int i) {
        this.mFileName = str;
        this.mMimetype = str2;
        this.mLength = j;
        this.mInputStream = fileInputStream;
        this.mStatus = i;
        this.mData = null;
    }

    public BluetoothOppSendFileInfo(String str, String str2, long j, int i) {
        this.mFileName = null;
        this.mInputStream = null;
        this.mData = str;
        this.mMimetype = str2;
        this.mLength = j;
        this.mStatus = i;
    }

    public static BluetoothOppSendFileInfo generateFileInfo(Context context, Uri uri, String str, boolean z) throws Throwable {
        long length;
        String str2;
        String str3;
        long streamSize;
        FileInputStream fileInputStreamCreateInputStream;
        AssetFileDescriptor assetFileDescriptorOpenAssetFileDescriptor;
        FileInputStream fileInputStreamCreateInputStream2;
        FileInputStream fileInputStream;
        Cursor cursorQuery;
        String string;
        String lastPathSegment;
        String mimeType = str;
        if (D) {
            Log.i(TAG, "generateFileInfo ++ uri = " + uri);
        }
        ContentResolver contentResolver = context.getContentResolver();
        String scheme = uri.getScheme();
        FileInputStream fileInputStream2 = null;
        if ("content".equals(scheme)) {
            if (z && BluetoothOppUtility.isForbiddenContent(uri)) {
                EventLog.writeEvent(1397638484, "179910660", -1, uri.toString());
                Log.e(TAG, "Content from forbidden URI is not allowed.");
                return SEND_FILE_INFO_ERROR;
            }
            String type = contentResolver.getType(uri);
            if ("".equals(type) && !"".equals(mimeType)) {
                if (V) {
                    Log.d(TAG, "contentType = " + type + " type = " + mimeType);
                }
            } else {
                mimeType = getMimeType(uri.toString(), mimeType);
                if (V) {
                    Log.d(TAG, "contentType = " + mimeType);
                }
            }
            String str4 = mimeType;
            try {
                cursorQuery = contentResolver.query(uri, new String[]{"_display_name", "_size"}, null, null, null);
            } catch (SQLiteException e) {
                e.printStackTrace();
                cursorQuery = null;
            } catch (SecurityException e2) {
                Log.e(TAG, "generateFileInfo: Permission error, could not access URI: " + uri);
                e2.printStackTrace();
                return SEND_FILE_INFO_ERROR;
            } catch (Exception e3) {
                Log.e(TAG, "generateFileInfo: Content error, could not access URI: " + uri);
                e3.printStackTrace();
                return SEND_FILE_INFO_NOT_ACCEPTABLE;
            }
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        int columnIndex = cursorQuery.getColumnIndex("_display_name");
                        int columnIndex2 = cursorQuery.getColumnIndex("_size");
                        if (columnIndex != -1) {
                            string = cursorQuery.getString(columnIndex);
                        } else {
                            string = null;
                        }
                        if (columnIndex2 != -1) {
                            length = cursorQuery.getLong(columnIndex2);
                        } else {
                            length = 0;
                        }
                        if (D) {
                            Log.d(TAG, "fileName = " + string + " length = " + length);
                        }
                    } else {
                        string = null;
                        length = 0;
                    }
                } finally {
                    cursorQuery.close();
                }
            } else {
                string = null;
                length = 0;
            }
            if (string == null) {
                String authority = uri.getAuthority();
                if (!"com.android.contacts".equals(authority)) {
                    lastPathSegment = getFileNameByUri(context, uri);
                } else {
                    lastPathSegment = uri.getLastPathSegment();
                }
                if (D) {
                    Log.d(TAG, "fileName = " + lastPathSegment + " authority = " + authority);
                }
                string = lastPathSegment;
            }
            str3 = string;
            str2 = str4;
        } else if ("file".equals(scheme)) {
            if (uri.getPath() == null) {
                Log.e(TAG, "Invalid URI path: " + uri);
                return SEND_FILE_INFO_ERROR;
            }
            if (z && !BluetoothOppUtility.isInValidStorageDir(uri)) {
                EventLog.writeEvent(1397638484, "35310991", -1, uri.getPath());
                Log.e(TAG, "File based URI not in Environment.getExternalStorageDirectory() is not allowed.");
                return SEND_FILE_INFO_ERROR;
            }
            String lastPathSegment2 = uri.getLastPathSegment();
            length = new File(uri.getPath()).length();
            str2 = mimeType;
            str3 = lastPathSegment2;
        } else {
            return SEND_FILE_INFO_ERROR;
        }
        if (scheme.equals("content")) {
            try {
                AssetFileDescriptor assetFileDescriptorOpenAssetFileDescriptor2 = contentResolver.openAssetFileDescriptor(uri, "r");
                long length2 = assetFileDescriptorOpenAssetFileDescriptor2.getLength();
                if (length != length2 && length2 > 0) {
                    Log.e(TAG, "Content provider length is wrong (" + Long.toString(length) + "), using stat length (" + Long.toString(length2) + ")");
                    length = length2;
                }
                try {
                    fileInputStreamCreateInputStream = assetFileDescriptorOpenAssetFileDescriptor2.createInputStream();
                    if (length == 0) {
                        try {
                            streamSize = getStreamSize(fileInputStreamCreateInputStream);
                            try {
                                Log.w(TAG, "File length not provided. Length from stream = " + streamSize);
                                assetFileDescriptorOpenAssetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r");
                            } catch (IOException e4) {
                            }
                        } catch (IOException e5) {
                            streamSize = length;
                        }
                        try {
                            fileInputStreamCreateInputStream2 = assetFileDescriptorOpenAssetFileDescriptor.createInputStream();
                            length = streamSize;
                        } catch (IOException e6) {
                            assetFileDescriptorOpenAssetFileDescriptor2 = assetFileDescriptorOpenAssetFileDescriptor;
                            try {
                                assetFileDescriptorOpenAssetFileDescriptor2.close();
                            } catch (IOException e7) {
                            }
                            fileInputStream2 = fileInputStreamCreateInputStream;
                            length = streamSize;
                        }
                    } else {
                        fileInputStreamCreateInputStream2 = fileInputStreamCreateInputStream;
                    }
                    fileInputStream2 = fileInputStreamCreateInputStream2;
                } catch (IOException e8) {
                    streamSize = length;
                    fileInputStreamCreateInputStream = null;
                }
            } catch (Exception e9) {
                e9.printStackTrace();
                return SEND_FILE_INFO_ERROR;
            }
        }
        if (fileInputStream2 == null) {
            try {
                FileInputStream fileInputStream3 = (FileInputStream) contentResolver.openInputStream(uri);
                if (length == 0) {
                    length = getStreamSize(fileInputStream3);
                    fileInputStream3 = (FileInputStream) contentResolver.openInputStream(uri);
                }
                fileInputStream = fileInputStream3;
            } catch (FileNotFoundException e10) {
                return SEND_FILE_INFO_ERROR;
            } catch (IOException e11) {
                return SEND_FILE_INFO_ERROR;
            }
        } else {
            fileInputStream = fileInputStream2;
        }
        if (length == 0) {
            Log.e(TAG, "Could not determine size of file");
            return SEND_FILE_INFO_ERROR;
        }
        if (length > 4294967295L) {
            Log.e(TAG, "File of size: " + length + " bytes can't be transferred");
            throw new IllegalArgumentException(context.getString(R.string.bluetooth_opp_file_limit_exceeded));
        }
        BluetoothOppSendFileInfo bluetoothOppSendFileInfo = new BluetoothOppSendFileInfo(str3, str2, length, fileInputStream, 0);
        bluetoothOppSendFileInfo.mFilePath = BluetoothOppUtility.getFilePath(context, uri);
        if (D) {
            Log.i(TAG, "generateFileInfo ++ info.mFilePath = " + bluetoothOppSendFileInfo.mFilePath);
        }
        return bluetoothOppSendFileInfo;
    }

    private static long getStreamSize(FileInputStream fileInputStream) throws IOException {
        byte[] bArr = new byte[4096];
        int i = fileInputStream.read(bArr, 0, 4096);
        long j = 0;
        while (i != -1) {
            j += (long) i;
            i = fileInputStream.read(bArr, 0, 4096);
        }
        return j;
    }

    private static String getFileNameByUri(Context context, Uri uri) throws Throwable {
        Cursor cursorQuery;
        String lastPathSegment = EnvironmentCompat.MEDIA_UNKNOWN;
        String[] strArr = {BluetoothShare._DATA};
        if (uri.getScheme().toString().equalsIgnoreCase("content")) {
            Cursor cursor = null;
            try {
                try {
                    cursorQuery = context.getContentResolver().query(uri, strArr, null, null, null);
                    if (cursorQuery != null) {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                lastPathSegment = Uri.parse(cursorQuery.getString(cursorQuery.getColumnIndexOrThrow(strArr[0]))).getLastPathSegment().toString();
                            }
                        } catch (Exception e) {
                            cursor = cursorQuery;
                            lastPathSegment = uri.getLastPathSegment();
                            if (cursor != null) {
                                cursor.close();
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
                    cursorQuery = cursor;
                }
            } catch (Exception e2) {
            }
        } else {
            lastPathSegment = EnvironmentCompat.MEDIA_UNKNOWN + "_" + uri.getLastPathSegment();
        }
        Log.i(TAG, "getFileNameByUri file name = " + lastPathSegment);
        return lastPathSegment;
    }

    public static String getMimeType(String str, String str2) {
        String mimeTypeFromExtension;
        String fileExtensionFromUrl = MimeTypeMap.getFileExtensionFromUrl(str);
        if (fileExtensionFromUrl != null) {
            mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtensionFromUrl);
        } else {
            mimeTypeFromExtension = null;
        }
        if (mimeTypeFromExtension != null || "".equals(str2)) {
            return "*/*";
        }
        return str2;
    }
}
