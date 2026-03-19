package com.android.bluetooth.opp;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.vcard.VCardConfig;
import com.google.android.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class BluetoothOppUtility {
    public static final String CALENDAR_AUTHORITY = "com.mediatek.calendarimporter";
    private static final String TAG = "BluetoothOppUtility";
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;
    private static final ConcurrentHashMap<Uri, ArrayList<BluetoothOppSendFileInfo>> sSendFileMap = new ConcurrentHashMap<>();
    private static final ArrayList<Uri> sUnstartedSendUriList = new ArrayList<>();
    private static ConcurrentHashMap<Uri, String> sSendFilePathMap = new ConcurrentHashMap<>();

    public static boolean isBluetoothShareUri(Uri uri) {
        return uri.toString().startsWith(BluetoothShare.CONTENT_URI.toString());
    }

    public static BluetoothOppTransferInfo queryRecord(Context context, Uri uri) {
        Cursor cursorQuery;
        BluetoothOppTransferInfo bluetoothOppTransferInfo = new BluetoothOppTransferInfo();
        try {
            cursorQuery = context.getContentResolver().query(uri, null, null, null, null);
        } catch (Exception e) {
            Log.e(TAG, "SQLite exception occur : " + e.toString());
            cursorQuery = null;
        }
        if (cursorQuery != null) {
            if (cursorQuery.moveToFirst()) {
                fillRecord(context, cursorQuery, bluetoothOppTransferInfo);
            }
            cursorQuery.close();
            return bluetoothOppTransferInfo;
        }
        if (V) {
            Log.v(TAG, "BluetoothOppManager Error: not got data from db for uri:" + uri);
        }
        return null;
    }

    public static void fillRecord(Context context, Cursor cursor, BluetoothOppTransferInfo bluetoothOppTransferInfo) {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothOppTransferInfo.mID = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
        bluetoothOppTransferInfo.mStatus = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
        bluetoothOppTransferInfo.mDirection = cursor.getInt(cursor.getColumnIndexOrThrow(BluetoothShare.DIRECTION));
        bluetoothOppTransferInfo.mTotalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(BluetoothShare.TOTAL_BYTES));
        bluetoothOppTransferInfo.mCurrentBytes = cursor.getLong(cursor.getColumnIndexOrThrow(BluetoothShare.CURRENT_BYTES));
        bluetoothOppTransferInfo.mTimeStamp = Long.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
        bluetoothOppTransferInfo.mDestAddr = cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.DESTINATION));
        bluetoothOppTransferInfo.mFileName = cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare._DATA));
        if (bluetoothOppTransferInfo.mFileName == null) {
            bluetoothOppTransferInfo.mFileName = cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.FILENAME_HINT));
        }
        if (bluetoothOppTransferInfo.mFileName == null) {
            bluetoothOppTransferInfo.mFileName = context.getString(R.string.unknown_file);
        }
        bluetoothOppTransferInfo.mFileUri = cursor.getString(cursor.getColumnIndexOrThrow("uri"));
        if (bluetoothOppTransferInfo.mFileUri != null) {
            bluetoothOppTransferInfo.mFileType = context.getContentResolver().getType(Uri.parse(bluetoothOppTransferInfo.mFileUri));
        } else {
            bluetoothOppTransferInfo.mFileType = context.getContentResolver().getType(Uri.parse(bluetoothOppTransferInfo.mFileName));
        }
        if (bluetoothOppTransferInfo.mFileType == null) {
            bluetoothOppTransferInfo.mFileType = cursor.getString(cursor.getColumnIndexOrThrow(BluetoothShare.MIMETYPE));
        }
        bluetoothOppTransferInfo.mDeviceName = BluetoothOppManager.getInstance(context).getDeviceName(defaultAdapter.getRemoteDevice(bluetoothOppTransferInfo.mDestAddr));
        bluetoothOppTransferInfo.mHandoverInitiated = cursor.getInt(cursor.getColumnIndexOrThrow("confirm")) == 5;
        if (V) {
            Log.v(TAG, "Get data from db:" + bluetoothOppTransferInfo.mFileName + bluetoothOppTransferInfo.mFileType + bluetoothOppTransferInfo.mDestAddr);
        }
    }

    public static ArrayList<String> queryTransfersInBatch(Context context, Long l) {
        Cursor cursorQuery;
        ArrayList<String> arrayListNewArrayList = Lists.newArrayList();
        try {
            cursorQuery = context.getContentResolver().query(BluetoothShare.CONTENT_URI, new String[]{BluetoothShare._DATA}, "timestamp == " + l, null, "_id");
        } catch (Exception e) {
            Log.e(TAG, "SQLite exception occur : " + e.toString());
            cursorQuery = null;
        }
        if (cursorQuery == null) {
            return null;
        }
        cursorQuery.moveToFirst();
        while (!cursorQuery.isAfterLast()) {
            String string = cursorQuery.getString(0);
            Uri uriFromFile = Uri.parse(string);
            if (uriFromFile.getScheme() == null) {
                uriFromFile = Uri.fromFile(new File(string));
            }
            arrayListNewArrayList.add(uriFromFile.toString());
            if (V) {
                Log.d(TAG, "Uri in this batch: " + uriFromFile.toString());
            }
            cursorQuery.moveToNext();
        }
        cursorQuery.close();
        return arrayListNewArrayList;
    }

    public static void openReceivedFile(Context context, String str, String str2, Long l, Uri uri) {
        Cursor cursorQuery;
        Uri uriFromFile;
        String string;
        if (str == null || str2 == null) {
            Log.e(TAG, "ERROR: Para fileName ==null, or mimetype == null");
            return;
        }
        if (!isBluetoothShareUri(uri)) {
            Log.e(TAG, "Trying to open a file that wasn't transfered over Bluetooth");
            return;
        }
        File file = new File(str);
        String str3 = null;
        if (!file.exists()) {
            Intent intent = new Intent(context, (Class<?>) BluetoothOppBtErrorActivity.class);
            intent.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            intent.putExtra("title", context.getString(R.string.not_exist_file));
            intent.putExtra("content", context.getString(R.string.not_exist_file_desc));
            context.startActivity(intent);
            if (V) {
                Log.d(TAG, "This uri will be deleted: " + uri);
            }
            context.getContentResolver().delete(uri, null, null);
            return;
        }
        try {
            uriFromFile = BluetoothOppFileProvider.getUriForFile(context, "com.android.bluetooth.opp.fileprovider", file);
            if (uriFromFile == null) {
                Log.w(TAG, "Cannot get content URI for the shared file");
                return;
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "FileProvider can't find path. " + e.toString());
            String lastPathSegment = uri.getLastPathSegment();
            try {
                cursorQuery = context.getContentResolver().query(BluetoothShare.CONTENT_URI, null, "_id = " + lastPathSegment, null, null);
            } catch (SQLiteException e2) {
                Log.e(TAG, "SQLite exception occur : " + e2.toString());
                cursorQuery = null;
            }
            if (cursorQuery == null) {
                Log.e(TAG, "cursor null !");
            }
            cursorQuery.moveToFirst();
            while (!cursorQuery.isAfterLast()) {
                if (V) {
                    Log.d(TAG, "cursor size = " + cursorQuery.getCount());
                }
                try {
                    string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("uri"));
                } catch (CursorIndexOutOfBoundsException e3) {
                    e = e3;
                    string = str3;
                }
                try {
                    if (V) {
                        Log.d(TAG, "uriString from db = " + string);
                    }
                } catch (CursorIndexOutOfBoundsException e4) {
                    e = e4;
                    Log.e(TAG, "SQLite exception occur : " + e.toString());
                    str3 = string;
                    cursorQuery.moveToNext();
                }
                str3 = string;
                cursorQuery.moveToNext();
            }
            uriFromFile = Uri.parse(str3);
        }
        if (V) {
            Log.d(TAG, "getUriForFile path = " + uriFromFile.toString());
        }
        if (uriFromFile != null && uriFromFile.getScheme() == null) {
            uriFromFile = Uri.fromFile(new File(str));
            if (V) {
                Log.d(TAG, "Uri fromFile = " + uriFromFile.toString());
            }
        }
        if (uriFromFile != null && isRecognizedFileType(context, uriFromFile, str2)) {
            Intent intent2 = new Intent("android.intent.action.VIEW");
            intent2.setDataAndTypeAndNormalize(uriFromFile, str2);
            context.getPackageManager().queryIntentActivities(intent2, 65536);
            intent2.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            intent2.addFlags(1);
            try {
                if (V) {
                    Log.d(TAG, "ACTION_VIEW intent sent out: " + uriFromFile + " / " + str2);
                }
                context.startActivity(intent2);
                return;
            } catch (ActivityNotFoundException e5) {
                if (V) {
                    Log.d(TAG, "no activity for handling ACTION_VIEW intent:  " + str2, e5);
                    return;
                }
                return;
            }
        }
        if (V) {
            Log.e(TAG, "openReceivedFile:: not recognized file");
        }
        Intent intent3 = new Intent(context, (Class<?>) BluetoothOppBtErrorActivity.class);
        intent3.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        intent3.putExtra("title", context.getString(R.string.unknown_file));
        intent3.putExtra("content", context.getString(R.string.unknown_file_desc));
        context.startActivity(intent3);
    }

    public static boolean isRecognizedFileType(Context context, Uri uri, String str) {
        if (D) {
            Log.d(TAG, "RecognizedFileType() fileUri: " + uri + " mimetype: " + str);
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndTypeAndNormalize(uri, str);
        if (context.getPackageManager().queryIntentActivities(intent, 65536).size() == 0) {
            if (D) {
                Log.d(TAG, "NO application to handle MIME type " + str);
            }
            return false;
        }
        return true;
    }

    public static void updateVisibilityToHidden(Context context, Uri uri) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BluetoothShare.VISIBILITY, (Integer) 1);
        context.getContentResolver().update(uri, contentValues, null, null);
    }

    public static String formatProgressText(long j, long j2) {
        double d;
        DecimalFormat decimalFormat = new DecimalFormat("0%");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        if (j > 0) {
            d = j2 / j;
        } else {
            d = 0.0d;
        }
        return decimalFormat.format(d);
    }

    public static String getStatusDescription(Context context, int i, String str) {
        if (i == 190) {
            return context.getString(R.string.status_pending);
        }
        if (i == 192) {
            return context.getString(R.string.status_running);
        }
        if (i == 200) {
            return context.getString(R.string.status_success);
        }
        if (i == 406) {
            return context.getString(R.string.status_not_accept);
        }
        if (i == 403) {
            return context.getString(R.string.status_forbidden);
        }
        if (i == 490) {
            return context.getString(R.string.status_canceled);
        }
        if (i == 492) {
            return context.getString(R.string.status_file_error);
        }
        if (i == 493) {
            return context.getString(R.string.status_no_sd_card);
        }
        if (i == 497) {
            return context.getString(R.string.status_connection_error);
        }
        if (i == 494) {
            return context.getString(R.string.bt_sm_2_1, str);
        }
        if (i == 400 || i == 411 || i == 412 || i == 495 || i == 496) {
            return context.getString(R.string.status_protocol_error);
        }
        return context.getString(R.string.status_unknown_error);
    }

    public static void retryTransfer(Context context, BluetoothOppTransferInfo bluetoothOppTransferInfo) {
        String str;
        ContentValues contentValues = new ContentValues();
        if (bluetoothOppTransferInfo.mFilePath != null) {
            str = bluetoothOppTransferInfo.mFilePath;
        } else {
            str = bluetoothOppTransferInfo.mFileName;
        }
        contentValues.put(BluetoothShare.CARRIER_NAME, str);
        contentValues.put("uri", bluetoothOppTransferInfo.mFileUri);
        contentValues.put(BluetoothShare.MIMETYPE, bluetoothOppTransferInfo.mFileType);
        contentValues.put(BluetoothShare.DESTINATION, bluetoothOppTransferInfo.mDestAddr);
        Uri uriInsert = context.getContentResolver().insert(BluetoothShare.CONTENT_URI, contentValues);
        if (V) {
            Log.v(TAG, "Insert contentUri: " + uriInsert + "  to device: " + bluetoothOppTransferInfo.mDeviceName);
        }
    }

    static Uri originalUri(Uri uri) {
        String string = uri.toString();
        int iLastIndexOf = string.lastIndexOf("@");
        if (iLastIndexOf != -1) {
            uri = Uri.parse(string.substring(0, iLastIndexOf));
        }
        if (V) {
            Log.v(TAG, "originalUri: " + uri);
        }
        return uri;
    }

    static Uri generateUri(Uri uri, BluetoothOppSendFileInfo bluetoothOppSendFileInfo) {
        String string = bluetoothOppSendFileInfo.toString();
        Uri uri2 = Uri.parse(uri + string.substring(string.lastIndexOf("@")));
        if (V) {
            Log.v(TAG, "generateUri: " + uri2);
        }
        return uri2;
    }

    static void putSendFileInfo(Uri uri, BluetoothOppSendFileInfo bluetoothOppSendFileInfo) {
        if (D) {
            Log.d(TAG, "putSendFileInfo: uri= " + uri + ", sendFileInfo= " + bluetoothOppSendFileInfo + ", path= " + bluetoothOppSendFileInfo.mFilePath);
        }
        if (bluetoothOppSendFileInfo == BluetoothOppSendFileInfo.SEND_FILE_INFO_ERROR || uri == null) {
            Log.e(TAG, "putSendFileInfo: bad sendFileInfo, URI: " + uri);
            return;
        }
        ArrayList<BluetoothOppSendFileInfo> arrayList = sSendFileMap.get(uri);
        if (arrayList == null) {
            ArrayList<BluetoothOppSendFileInfo> arrayList2 = new ArrayList<>();
            arrayList2.add(bluetoothOppSendFileInfo);
            sSendFileMap.put(uri, arrayList2);
            if (D) {
                Log.d(TAG, "putSendFileInfo: uri=" + uri + ", is a new uri, create ArrayList");
            }
        } else {
            if (uri.toString().contains("bluetooth_content_share.html") && arrayList.size() >= 1) {
                if (D) {
                    Log.d(TAG, "Clear same html files in list");
                }
                arrayList.clear();
            }
            arrayList.add(bluetoothOppSendFileInfo);
            if (D) {
                Log.d(TAG, "putSendFileInfo: uri=" + uri + ", already have, final size is " + arrayList.size());
            }
        }
        if (sSendFilePathMap != null && bluetoothOppSendFileInfo.mFilePath != null) {
            sSendFilePathMap.put(uri, bluetoothOppSendFileInfo.mFilePath);
        }
    }

    static BluetoothOppSendFileInfo getSendFileInfo(Uri uri) {
        if (uri == null) {
            Log.e(TAG, "getSendFileInfo: bad URI: " + uri);
            return BluetoothOppSendFileInfo.SEND_FILE_INFO_ERROR;
        }
        BluetoothOppSendFileInfo bluetoothOppSendFileInfo = null;
        ArrayList<BluetoothOppSendFileInfo> arrayList = sSendFileMap.get(uri);
        if (arrayList != null && !arrayList.isEmpty()) {
            bluetoothOppSendFileInfo = arrayList.get(0);
        }
        if (bluetoothOppSendFileInfo != null && V) {
            Log.d(TAG, "getSendFileInfo: uri= " + uri + ", info= " + bluetoothOppSendFileInfo + ", path= " + bluetoothOppSendFileInfo.mFilePath);
        }
        return bluetoothOppSendFileInfo != null ? bluetoothOppSendFileInfo : BluetoothOppSendFileInfo.SEND_FILE_INFO_ERROR;
    }

    static void closeSendFileInfo(Uri uri) {
        if (D) {
            Log.d(TAG, "closeSendFileInfo: uri=" + uri);
        }
        if (uri == null) {
            return;
        }
        BluetoothOppSendFileInfo bluetoothOppSendFileInfoRemove = null;
        ArrayList<BluetoothOppSendFileInfo> arrayList = sSendFileMap.get(uri);
        if (arrayList != null) {
            bluetoothOppSendFileInfoRemove = arrayList.remove(0);
            int size = arrayList.size();
            if (D) {
                Log.d(TAG, "closeSendFileInfo: uri=" + uri + ", ArrayList size = " + size);
            }
            if (size == 0) {
                sSendFileMap.remove(uri);
            }
        }
        if (bluetoothOppSendFileInfoRemove != null && bluetoothOppSendFileInfoRemove.mInputStream != null) {
            try {
                bluetoothOppSendFileInfoRemove.mInputStream.close();
            } catch (IOException e) {
            }
        }
        if (sSendFilePathMap != null && sSendFilePathMap.size() > 0) {
            sSendFilePathMap.remove(uri);
            if (D) {
                Log.d(TAG, "sSendFilePathMap: size =" + sSendFilePathMap.size());
            }
        }
    }

    public static String getFilePathFromUri(Uri uri) {
        String str;
        if (sSendFilePathMap != null) {
            str = sSendFilePathMap.get(uri);
        } else {
            str = null;
        }
        if (V) {
            Log.d(TAG, "getFilePathFromUri = " + str);
        }
        return str;
    }

    static void putPrepareSendingUri(Uri uri) {
        if (D) {
            Log.d(TAG, "putPrepareSendingUri: uri = " + uri);
        }
        sUnstartedSendUriList.add(uri);
    }

    static void putUnstartedSendUris(ArrayList<Uri> arrayList) {
        if (D) {
            Log.d(TAG, "putPrepareSendingUris");
        }
        sUnstartedSendUriList.addAll(arrayList);
    }

    static void removeUnstartedSendUris() {
        if (D) {
            Log.d(TAG, "removePrepareSendingUris");
        }
        sUnstartedSendUriList.clear();
    }

    static void closeUnstartedSendFileInfo() {
        synchronized (sUnstartedSendUriList) {
            if (D) {
                Log.d(TAG, "closeSendFileInfoForPreparing");
            }
            Iterator<Uri> it = sUnstartedSendUriList.iterator();
            while (it.hasNext()) {
                closeSendFileInfo(it.next());
            }
            removeUnstartedSendUris();
        }
    }

    static int getUnstartedSendCount() {
        int size = sUnstartedSendUriList.size();
        if (V) {
            Log.d(TAG, "getUnstartedSendCount = " + size);
        }
        return size;
    }

    static boolean isInExternalStorageDir(Uri uri) {
        if (!"file".equals(uri.getScheme())) {
            Log.e(TAG, "Not a file URI: " + uri);
            return false;
        }
        return isSameOrSubDirectory(Environment.getExternalStorageDirectory(), new File(uri.getCanonicalUri().getPath()));
    }

    static boolean isForbiddenContent(Uri uri) {
        if ("com.android.bluetooth.map.MmsFileProvider".equals(uri.getHost())) {
            return true;
        }
        return false;
    }

    static boolean isSameOrSubDirectory(File file, File file2) {
        try {
            File canonicalFile = file.getCanonicalFile();
            for (File canonicalFile2 = file2.getCanonicalFile(); canonicalFile2 != null; canonicalFile2 = canonicalFile2.getParentFile()) {
                if (canonicalFile.equals(canonicalFile2)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error while accessing file", e);
            return false;
        }
    }

    static boolean isInValidStorageDir(Uri uri) {
        if (D) {
            Log.d(TAG, "isInValidStorageDir uri = " + uri);
        }
        if (!"file".equals(uri.getScheme())) {
            Log.e(TAG, "Not a file URI: " + uri);
            return false;
        }
        File file = new File(uri.getCanonicalUri().getPath());
        if (D) {
            Log.d(TAG, "file path = " + file.getAbsolutePath());
        }
        return file.getAbsolutePath().startsWith("/storage");
    }

    static String getFilePath(Context context, Uri uri) {
        Cursor cursorQuery;
        if (V) {
            Log.i(TAG, "getFilePath::fileUri = " + uri.toString());
        }
        String authority = uri.getAuthority();
        if (!"content".equals(uri.getScheme()) || "com.android.contacts".equals(authority) || CALENDAR_AUTHORITY.equals(authority)) {
            if ("file".equals(uri.getScheme())) {
                return uri.getPath();
            }
            return null;
        }
        Log.d(TAG, "getFilePath:: content, not contact or caledar");
        String[] strArr = {BluetoothShare._DATA};
        try {
            context.grantUriPermission(context.getPackageName(), uri, 1);
            cursorQuery = context.getContentResolver().query(uri, strArr, null, null, null);
            context.revokeUriPermission(uri, 1);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "No permission to query provider for uri: " + uri.toString());
            cursorQuery = null;
        }
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    String string = cursorQuery.getString(cursorQuery.getColumnIndex(BluetoothShare._DATA));
                    cursorQuery.close();
                    return string;
                }
            } catch (Exception e2) {
                Log.e(TAG, "Cannot find _data column");
                return null;
            } finally {
                cursorQuery.close();
            }
        }
        Log.e(TAG, "getFilePath get file path fail");
        if (cursorQuery != null) {
        }
        return null;
    }

    static int getTotalTaskCount() {
        Log.i(TAG, "getTotalTaskCount ++");
        Iterator<Uri> it = sSendFileMap.keySet().iterator();
        int size = 0;
        while (it.hasNext()) {
            ArrayList<BluetoothOppSendFileInfo> arrayList = sSendFileMap.get(it.next());
            if (arrayList != null) {
                size += arrayList.size();
            }
        }
        Log.d(TAG, "getTotalTaskCount return " + size);
        return size;
    }

    protected static void cancelNotification(Context context) {
        ((NotificationManager) context.getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION)).cancel(BluetoothOppNotification.NOTIFICATION_ID_PROGRESS);
    }
}
