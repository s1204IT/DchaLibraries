package com.android.bluetooth.opp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class BluetoothOppReceiveFileInfo {
    private static final boolean D = true;
    private static final boolean V = false;
    private static String sDesiredStoragePath = null;
    public String mData;
    public String mFileName;
    public long mLength;
    public FileOutputStream mOutputStream;
    public int mStatus;

    public BluetoothOppReceiveFileInfo(String data, long length, int status) {
        this.mData = data;
        this.mStatus = status;
        this.mLength = length;
    }

    public BluetoothOppReceiveFileInfo(String filename, long length, FileOutputStream outputStream, int status) {
        this.mFileName = filename;
        this.mOutputStream = outputStream;
        this.mStatus = status;
        this.mLength = length;
    }

    public BluetoothOppReceiveFileInfo(int status) {
        this(null, 0L, null, status);
    }

    public static BluetoothOppReceiveFileInfo generateFileInfo(Context context, int id) throws Throwable {
        long length;
        String extension;
        ContentResolver contentResolver = context.getContentResolver();
        Uri contentUri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + id);
        String hint = null;
        String mimeType = null;
        Cursor metadataCursor = contentResolver.query(contentUri, new String[]{BluetoothShare.FILENAME_HINT, BluetoothShare.TOTAL_BYTES, BluetoothShare.MIMETYPE}, null, null, null);
        if (metadataCursor == null) {
            length = 0;
        } else {
            try {
                if (!metadataCursor.moveToFirst()) {
                    length = 0;
                } else {
                    hint = metadataCursor.getString(0);
                    length = metadataCursor.getInt(1);
                    try {
                        mimeType = metadataCursor.getString(2);
                    } catch (Throwable th) {
                        th = th;
                        metadataCursor.close();
                        throw th;
                    }
                }
                metadataCursor.close();
            } catch (Throwable th2) {
                th = th2;
            }
        }
        if (Environment.getExternalStorageState().equals("mounted")) {
            String root = Environment.getExternalStorageDirectory().getPath();
            File base = new File(root + Constants.DEFAULT_STORE_SUBDIR);
            if (!base.isDirectory() && !base.mkdir()) {
                Log.d(Constants.TAG, "Receive File aborted - can't create base directory " + base.getPath());
                return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
            }
            StatFs stat = new StatFs(base.getPath());
            if (((long) stat.getBlockSize()) * (((long) stat.getAvailableBlocks()) - 4) < length) {
                Log.d(Constants.TAG, "Receive File aborted - not enough free space");
                return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_ERROR_SDCARD_FULL);
            }
            String filename = choosefilename(hint);
            if (filename == null) {
                return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
            }
            int dotIndex = filename.lastIndexOf(".");
            if (dotIndex < 0) {
                if (mimeType == null) {
                    return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
                }
                extension = "";
            } else {
                extension = filename.substring(dotIndex);
                filename = filename.substring(0, dotIndex);
            }
            String fullfilename = chooseUniquefilename(base.getPath() + File.separator + filename, extension);
            if (!safeCanonicalPath(fullfilename)) {
                return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
            }
            if (fullfilename != null) {
                try {
                    new FileOutputStream(fullfilename).close();
                    int index = fullfilename.lastIndexOf(47) + 1;
                    if (index > 0) {
                        String displayName = fullfilename.substring(index);
                        ContentValues updateValues = new ContentValues();
                        updateValues.put(BluetoothShare.FILENAME_HINT, displayName);
                        context.getContentResolver().update(contentUri, updateValues, null, null);
                    }
                    return new BluetoothOppReceiveFileInfo(fullfilename, length, new FileOutputStream(fullfilename), 0);
                } catch (IOException e) {
                    Log.e(Constants.TAG, "Error when creating file " + fullfilename);
                    return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
                }
            }
            return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
        }
        Log.d(Constants.TAG, "Receive File aborted - no external storage");
        return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_ERROR_NO_SDCARD);
    }

    private static boolean safeCanonicalPath(String uniqueFileName) {
        try {
            File receiveFile = new File(uniqueFileName);
            if (sDesiredStoragePath == null) {
                sDesiredStoragePath = Environment.getExternalStorageDirectory().getPath() + Constants.DEFAULT_STORE_SUBDIR;
            }
            String canonicalPath = receiveFile.getCanonicalPath();
            return canonicalPath.startsWith(sDesiredStoragePath);
        } catch (IOException e) {
            return false;
        }
    }

    private static String chooseUniquefilename(String filename, String extension) {
        String fullfilename = filename + extension;
        if (!new File(fullfilename).exists()) {
            return fullfilename;
        }
        String filename2 = filename + Constants.filename_SEQUENCE_SEPARATOR;
        Random rnd = new Random(SystemClock.uptimeMillis());
        int sequence = 1;
        for (int magnitude = 1; magnitude < 1000000000; magnitude *= 10) {
            for (int iteration = 0; iteration < 9; iteration++) {
                String fullfilename2 = filename2 + sequence + extension;
                if (!new File(fullfilename2).exists()) {
                    return fullfilename2;
                }
                sequence += rnd.nextInt(magnitude) + 1;
            }
        }
        return null;
    }

    private static String choosefilename(String hint) {
        if (0 != 0 || hint == null || hint.endsWith("/") || hint.endsWith("\\")) {
            return null;
        }
        String hint2 = hint.replace('\\', '/').replaceAll("\\s", " ").replaceAll("[:\"<>*?|]", "_");
        int index = hint2.lastIndexOf(47) + 1;
        if (index > 0) {
            String filename = hint2.substring(index);
            return filename;
        }
        return hint2;
    }
}
