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
import java.io.UnsupportedEncodingException;
import java.util.Random;

public class BluetoothOppReceiveFileInfo {
    private static final int OPP_LENGTH_OF_FILE_NAME = 244;
    public String mData;
    public String mFileName;
    public long mLength;
    public FileOutputStream mOutputStream;
    public int mStatus;
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;
    private static String sDesiredStoragePath = null;

    public BluetoothOppReceiveFileInfo(String str, long j, int i) {
        this.mData = str;
        this.mStatus = i;
        this.mLength = j;
    }

    public BluetoothOppReceiveFileInfo(String str, long j, FileOutputStream fileOutputStream, int i) {
        this.mFileName = str;
        this.mOutputStream = fileOutputStream;
        this.mStatus = i;
        this.mLength = j;
    }

    public BluetoothOppReceiveFileInfo(int i) {
        this(null, 0L, null, i);
    }

    public static BluetoothOppReceiveFileInfo generateFileInfo(Context context, int i) {
        String str;
        String string;
        String str2;
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse(BluetoothShare.CONTENT_URI + "/" + i);
        Cursor cursorQuery = contentResolver.query(uri, new String[]{BluetoothShare.FILENAME_HINT, BluetoothShare.TOTAL_BYTES, BluetoothShare.MIMETYPE}, null, null, null);
        long j = 0;
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    String string2 = cursorQuery.getString(0);
                    long j2 = cursorQuery.getLong(1);
                    string = cursorQuery.getString(2);
                    str = string2;
                    j = j2;
                } else {
                    str = null;
                    string = null;
                }
            } finally {
                cursorQuery.close();
            }
        } else {
            str = null;
            string = null;
        }
        if (Environment.getExternalStorageState().equals("mounted")) {
            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/bluetooth");
            if (!file.isDirectory() && !file.mkdir()) {
                if (D) {
                    Log.d(Constants.TAG, "Receive File aborted - can't create base directory " + file.getPath());
                }
                return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
            }
            StatFs statFs = new StatFs(file.getPath());
            if (statFs.getBlockSizeLong() * (statFs.getAvailableBlocksLong() - 4) < j) {
                if (D) {
                    Log.d(Constants.TAG, "Receive File aborted - not enough free space");
                }
                return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_ERROR_SDCARD_FULL);
            }
            String strChoosefilename = choosefilename(str);
            if (strChoosefilename == null) {
                return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
            }
            int iLastIndexOf = strChoosefilename.lastIndexOf(".");
            if (iLastIndexOf < 0) {
                if (string == null) {
                    return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
                }
                str2 = "";
            } else {
                String strSubstring = strChoosefilename.substring(iLastIndexOf);
                strChoosefilename = strChoosefilename.substring(0, iLastIndexOf);
                str2 = strSubstring;
            }
            if (D) {
                Log.d(Constants.TAG, " File Name " + strChoosefilename);
            }
            if (strChoosefilename.getBytes().length > OPP_LENGTH_OF_FILE_NAME) {
                Log.i(Constants.TAG, " File Name Length :" + strChoosefilename.length());
                Log.i(Constants.TAG, " File Name Length in Bytes:" + strChoosefilename.getBytes().length);
                try {
                    byte[] bytes = strChoosefilename.getBytes("UTF-8");
                    byte[] bArr = new byte[OPP_LENGTH_OF_FILE_NAME];
                    System.arraycopy(bytes, 0, bArr, 0, OPP_LENGTH_OF_FILE_NAME);
                    strChoosefilename = new String(bArr, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e(Constants.TAG, "Exception: " + e);
                }
                if (D) {
                    Log.d(Constants.TAG, "File name is too long. Name is truncated as: " + strChoosefilename);
                }
            }
            String strChooseUniquefilename = chooseUniquefilename(file.getPath() + File.separator + strChoosefilename, str2);
            if (!safeCanonicalPath(strChooseUniquefilename)) {
                return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
            }
            if (V) {
                Log.v(Constants.TAG, "Generated received filename " + strChooseUniquefilename);
            }
            if (strChooseUniquefilename != null) {
                try {
                    new FileOutputStream(strChooseUniquefilename).close();
                    int iLastIndexOf2 = strChooseUniquefilename.lastIndexOf(47) + 1;
                    if (iLastIndexOf2 > 0) {
                        String strSubstring2 = strChooseUniquefilename.substring(iLastIndexOf2);
                        if (V) {
                            Log.v(Constants.TAG, "New display name " + strSubstring2);
                        }
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(BluetoothShare.FILENAME_HINT, strSubstring2);
                        context.getContentResolver().update(uri, contentValues, null, null);
                    }
                    return new BluetoothOppReceiveFileInfo(strChooseUniquefilename, j, new FileOutputStream(strChooseUniquefilename), 0);
                } catch (IOException e2) {
                    if (D) {
                        Log.e(Constants.TAG, "Error when creating file " + strChooseUniquefilename);
                    }
                    return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
                }
            }
            return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_FILE_ERROR);
        }
        if (D) {
            Log.d(Constants.TAG, "Receive File aborted - no external storage");
        }
        return new BluetoothOppReceiveFileInfo(BluetoothShare.STATUS_ERROR_NO_SDCARD);
    }

    private static boolean safeCanonicalPath(String str) {
        try {
            File file = new File(str);
            if (sDesiredStoragePath == null) {
                sDesiredStoragePath = Environment.getExternalStorageDirectory().getPath() + "/bluetooth";
            }
            if (!file.getCanonicalPath().startsWith(sDesiredStoragePath)) {
                return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String chooseUniquefilename(String str, String str2) {
        String str3 = str + str2;
        if (!new File(str3).exists()) {
            return str3;
        }
        String str4 = str + "-";
        Random random = new Random(SystemClock.uptimeMillis());
        int iNextInt = 1;
        for (int i = 1; i < 1000000000; i *= 10) {
            for (int i2 = 0; i2 < 9; i2++) {
                String str5 = str4 + iNextInt + str2;
                if (!new File(str5).exists()) {
                    return str5;
                }
                if (V) {
                    Log.v(Constants.TAG, "file with sequence number " + iNextInt + " exists");
                }
                iNextInt += random.nextInt(i) + 1;
            }
        }
        return null;
    }

    private static String choosefilename(String str) {
        if (str != null && !str.endsWith("/") && !str.endsWith("\\")) {
            String strReplaceAll = str.replace('\\', '/').replaceAll("\\s", " ").replaceAll("[:\"<>*?|]", "_");
            if (V) {
                Log.v(Constants.TAG, "getting filename from hint");
            }
            int iLastIndexOf = strReplaceAll.lastIndexOf(47) + 1;
            return iLastIndexOf > 0 ? strReplaceAll.substring(iLastIndexOf) : strReplaceAll;
        }
        return null;
    }
}
