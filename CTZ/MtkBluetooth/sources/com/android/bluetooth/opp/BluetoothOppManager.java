package com.android.bluetooth.opp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.bluetooth.R;
import com.android.vcard.VCardConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BluetoothOppManager {
    private static final int ALLOWED_INSERT_SHARE_THREAD_NUMBER = 3;
    private static final String ARRAYLIST_ITEM_SEPERATOR = ";";
    private static final String FILE_URI = "FILE_URI";
    private static final String FILE_URIS = "FILE_URIS";
    private static final String MIME_TYPE = "MIMETYPE";
    private static final String MIME_TYPE_MULTIPLE = "MIMETYPE_MULTIPLE";
    private static final String MULTIPLE_FLAG = "MULTIPLE_FLAG";
    private static final String OPP_PREFERENCE_FILE = "OPPMGR";
    private static final String SENDING_FLAG = "SENDINGFLAG";
    private static final String TAG = "BluetoothOppManager";
    private static final int WHITELIST_DURATION_MS = 15000;
    private static BluetoothOppManager sInstance;
    private BluetoothAdapter mAdapter;
    private Context mContext;
    private int mFileNumInBatch;
    private boolean mInitialized;
    private boolean mIsHandoverInitiated;
    private String mMimeTypeOfSendingFile;
    private String mMimeTypeOfSendingFiles;
    public boolean mMultipleFlag;
    public boolean mSendingFlag;
    private String mUriOfSendingFile;
    private ArrayList<Uri> mUrisOfSendingFiles;
    private static final boolean V = Constants.VERBOSE;
    private static final Object INSTANCE_LOCK = new Object();
    private int mInsertShareThreadNum = 0;
    private List<Pair<String, Long>> mWhitelist = new ArrayList();

    static int access$008(BluetoothOppManager bluetoothOppManager) {
        int i = bluetoothOppManager.mInsertShareThreadNum;
        bluetoothOppManager.mInsertShareThreadNum = i + 1;
        return i;
    }

    static int access$010(BluetoothOppManager bluetoothOppManager) {
        int i = bluetoothOppManager.mInsertShareThreadNum;
        bluetoothOppManager.mInsertShareThreadNum = i - 1;
        return i;
    }

    public static BluetoothOppManager getInstance(Context context) {
        BluetoothOppManager bluetoothOppManager;
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new BluetoothOppManager();
            }
            sInstance.init(context);
            bluetoothOppManager = sInstance;
        }
        return bluetoothOppManager;
    }

    private boolean init(Context context) {
        if (this.mInitialized) {
            return true;
        }
        this.mInitialized = true;
        this.mContext = context;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (this.mAdapter == null && V) {
            Log.v(TAG, "BLUETOOTH_SERVICE is not started! ");
        }
        restoreApplicationData();
        return true;
    }

    private void cleanupWhitelist() {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        Iterator<Pair<String, Long>> it = this.mWhitelist.iterator();
        while (it.hasNext()) {
            Pair<String, Long> next = it.next();
            if (jElapsedRealtime - ((Long) next.second).longValue() > 15000) {
                if (V) {
                    Log.v(TAG, "Cleaning out whitelist entry " + ((String) next.first));
                }
                it.remove();
            }
        }
    }

    public synchronized void addToWhitelist(String str) {
        if (str == null) {
            return;
        }
        Iterator<Pair<String, Long>> it = this.mWhitelist.iterator();
        while (it.hasNext()) {
            if (((String) it.next().first).equals(str)) {
                it.remove();
            }
        }
        this.mWhitelist.add(new Pair<>(str, Long.valueOf(SystemClock.elapsedRealtime())));
    }

    public synchronized boolean isWhitelisted(String str) {
        cleanupWhitelist();
        Iterator<Pair<String, Long>> it = this.mWhitelist.iterator();
        while (it.hasNext()) {
            if (((String) it.next().first).equals(str)) {
                return true;
            }
        }
        return false;
    }

    private void restoreApplicationData() {
        SharedPreferences sharedPreferences = this.mContext.getSharedPreferences(OPP_PREFERENCE_FILE, 0);
        this.mSendingFlag = sharedPreferences.getBoolean(SENDING_FLAG, false);
        this.mMimeTypeOfSendingFile = sharedPreferences.getString(MIME_TYPE, null);
        this.mUriOfSendingFile = sharedPreferences.getString(FILE_URI, null);
        this.mMimeTypeOfSendingFiles = sharedPreferences.getString(MIME_TYPE_MULTIPLE, null);
        this.mMultipleFlag = sharedPreferences.getBoolean(MULTIPLE_FLAG, false);
        if (V) {
            Log.v(TAG, "restoreApplicationData! " + this.mSendingFlag + this.mMultipleFlag + this.mMimeTypeOfSendingFile + this.mUriOfSendingFile);
        }
        String string = sharedPreferences.getString(FILE_URIS, null);
        this.mUrisOfSendingFiles = new ArrayList<>();
        if (string != null) {
            String[] strArrSplit = string.split(ARRAYLIST_ITEM_SEPERATOR);
            for (int i = 0; i < strArrSplit.length; i++) {
                this.mUrisOfSendingFiles.add(Uri.parse(strArrSplit[i]));
                if (V) {
                    Log.v(TAG, "Uri in batch:  " + Uri.parse(strArrSplit[i]));
                }
            }
        }
        this.mContext.getSharedPreferences(OPP_PREFERENCE_FILE, 0).edit().clear().apply();
    }

    private void storeApplicationData() {
        SharedPreferences.Editor editorEdit = this.mContext.getSharedPreferences(OPP_PREFERENCE_FILE, 0).edit();
        editorEdit.putBoolean(SENDING_FLAG, this.mSendingFlag);
        editorEdit.putBoolean(MULTIPLE_FLAG, this.mMultipleFlag);
        if (this.mMultipleFlag) {
            editorEdit.putString(MIME_TYPE_MULTIPLE, this.mMimeTypeOfSendingFiles);
            StringBuilder sb = new StringBuilder();
            int size = this.mUrisOfSendingFiles.size();
            for (int i = 0; i < size; i++) {
                sb.append(this.mUrisOfSendingFiles.get(i));
                sb.append(ARRAYLIST_ITEM_SEPERATOR);
            }
            editorEdit.putString(FILE_URIS, sb.toString());
            editorEdit.remove(MIME_TYPE);
            editorEdit.remove(FILE_URI);
        } else {
            editorEdit.putString(MIME_TYPE, this.mMimeTypeOfSendingFile);
            editorEdit.putString(FILE_URI, this.mUriOfSendingFile);
            editorEdit.remove(MIME_TYPE_MULTIPLE);
            editorEdit.remove(FILE_URIS);
        }
        editorEdit.apply();
        if (V) {
            Log.v(TAG, "Application data stored to SharedPreference! ");
        }
    }

    public void saveSendingFileInfo(String str, String str2, boolean z, boolean z2) throws IllegalArgumentException {
        synchronized (this) {
            this.mMultipleFlag = false;
            this.mMimeTypeOfSendingFile = str;
            this.mIsHandoverInitiated = z;
            Uri uri = Uri.parse(str2);
            BluetoothOppSendFileInfo bluetoothOppSendFileInfoGenerateFileInfo = BluetoothOppSendFileInfo.generateFileInfo(this.mContext, uri, str, z2);
            Uri uriGenerateUri = BluetoothOppUtility.generateUri(uri, bluetoothOppSendFileInfoGenerateFileInfo);
            BluetoothOppUtility.putSendFileInfo(uriGenerateUri, bluetoothOppSendFileInfoGenerateFileInfo);
            this.mUriOfSendingFile = uriGenerateUri.toString();
            storeApplicationData();
        }
    }

    public void saveSendingFileInfo(String str, ArrayList<Uri> arrayList, boolean z, boolean z2) throws IllegalArgumentException {
        synchronized (this) {
            this.mMultipleFlag = true;
            this.mMimeTypeOfSendingFiles = str;
            this.mUrisOfSendingFiles = new ArrayList<>();
            this.mIsHandoverInitiated = z;
            for (Uri uri : arrayList) {
                BluetoothOppSendFileInfo bluetoothOppSendFileInfoGenerateFileInfo = BluetoothOppSendFileInfo.generateFileInfo(this.mContext, uri, str, z2);
                Uri uriGenerateUri = BluetoothOppUtility.generateUri(uri, bluetoothOppSendFileInfoGenerateFileInfo);
                this.mUrisOfSendingFiles.add(uriGenerateUri);
                BluetoothOppUtility.putSendFileInfo(uriGenerateUri, bluetoothOppSendFileInfoGenerateFileInfo);
            }
            BluetoothOppUtility.putUnstartedSendUris(arrayList);
            storeApplicationData();
        }
    }

    public boolean isEnabled() {
        if (this.mAdapter != null) {
            return this.mAdapter.isEnabled();
        }
        if (V) {
            Log.v(TAG, "BLUETOOTH_SERVICE is not available! ");
            return false;
        }
        return false;
    }

    public void enableBluetooth() {
        if (this.mAdapter != null) {
            this.mAdapter.enable();
        }
    }

    public void disableBluetooth() {
        if (this.mAdapter != null) {
            this.mAdapter.disable();
        }
    }

    public String getDeviceName(BluetoothDevice bluetoothDevice) {
        String aliasName;
        if (bluetoothDevice != null) {
            aliasName = bluetoothDevice.getAliasName();
            if (aliasName == null) {
                aliasName = BluetoothOppPreference.getInstance(this.mContext).getName(bluetoothDevice);
            }
        } else {
            aliasName = null;
        }
        if (aliasName == null) {
            return this.mContext.getString(R.string.unknown_device);
        }
        return aliasName;
    }

    public int getBatchSize() {
        int i;
        synchronized (this) {
            i = this.mFileNumInBatch;
        }
        return i;
    }

    public void startTransfer(BluetoothDevice bluetoothDevice) {
        if (V) {
            Log.v(TAG, "Active InsertShareThread number is : " + this.mInsertShareThreadNum);
        }
        BluetoothOppUtility.removeUnstartedSendUris();
        synchronized (this) {
            if (this.mInsertShareThreadNum > 3) {
                Log.e(TAG, "Too many shares user triggered concurrently!");
                Intent intent = new Intent(this.mContext, (Class<?>) BluetoothOppBtErrorActivity.class);
                intent.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
                intent.putExtra("title", this.mContext.getString(R.string.enabling_progress_title));
                intent.putExtra("content", this.mContext.getString(R.string.ErrorTooManyRequests));
                this.mContext.startActivity(intent);
                return;
            }
            InsertShareInfoThread insertShareInfoThread = new InsertShareInfoThread(bluetoothDevice, this.mMultipleFlag, this.mMimeTypeOfSendingFile, this.mUriOfSendingFile, this.mMimeTypeOfSendingFiles, this.mUrisOfSendingFiles, this.mIsHandoverInitiated);
            if (this.mMultipleFlag) {
                this.mFileNumInBatch = this.mUrisOfSendingFiles.size();
            }
            insertShareInfoThread.start();
        }
    }

    private class InsertShareInfoThread extends Thread {
        private final boolean mIsHandoverInitiated;
        private final boolean mIsMultiple;
        private final BluetoothDevice mRemoteDevice;
        private final String mTypeOfMultipleFiles;
        private final String mTypeOfSingleFile;
        private final String mUri;
        private final ArrayList<Uri> mUris;

        InsertShareInfoThread(BluetoothDevice bluetoothDevice, boolean z, String str, String str2, String str3, ArrayList<Uri> arrayList, boolean z2) {
            super("Insert ShareInfo Thread");
            this.mRemoteDevice = bluetoothDevice;
            this.mIsMultiple = z;
            this.mTypeOfSingleFile = str;
            this.mUri = str2;
            this.mTypeOfMultipleFiles = str3;
            this.mUris = arrayList;
            this.mIsHandoverInitiated = z2;
            synchronized (BluetoothOppManager.this) {
                BluetoothOppManager.access$008(BluetoothOppManager.this);
            }
            if (BluetoothOppManager.V) {
                Log.v(BluetoothOppManager.TAG, "Thread id is: " + getId());
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(10);
            if (this.mRemoteDevice == null) {
                Log.e(BluetoothOppManager.TAG, "Target bt device is null!");
                return;
            }
            if (this.mIsMultiple) {
                insertMultipleShare();
            } else {
                insertSingleShare();
            }
            synchronized (BluetoothOppManager.this) {
                BluetoothOppManager.access$010(BluetoothOppManager.this);
            }
        }

        private void insertMultipleShare() {
            int size = this.mUris.size();
            Long lValueOf = Long.valueOf(System.currentTimeMillis());
            for (int i = 0; i < size; i++) {
                Uri uri = this.mUris.get(i);
                ContentValues contentValues = new ContentValues();
                contentValues.put("uri", uri.toString());
                ContentResolver contentResolver = BluetoothOppManager.this.mContext.getContentResolver();
                Uri uriOriginalUri = BluetoothOppUtility.originalUri(uri);
                String type = contentResolver.getType(uriOriginalUri);
                if (BluetoothOppManager.V) {
                    Log.v(BluetoothOppManager.TAG, "Got mimetype: " + type + "  Got uri: " + uriOriginalUri);
                }
                if (TextUtils.isEmpty(type)) {
                    type = this.mTypeOfMultipleFiles;
                }
                contentValues.put(BluetoothShare.CARRIER_NAME, BluetoothOppUtility.getFilePathFromUri(uriOriginalUri));
                contentValues.put(BluetoothShare.MIMETYPE, type);
                contentValues.put(BluetoothShare.DESTINATION, this.mRemoteDevice.getAddress());
                contentValues.put("timestamp", lValueOf);
                if (this.mIsHandoverInitiated) {
                    contentValues.put("confirm", (Integer) 5);
                }
                Uri uriInsert = BluetoothOppManager.this.mContext.getContentResolver().insert(BluetoothShare.CONTENT_URI, contentValues);
                if (BluetoothOppManager.V) {
                    Log.v(BluetoothOppManager.TAG, "Insert contentUri: " + uriInsert + "  to device: " + BluetoothOppManager.this.getDeviceName(this.mRemoteDevice));
                }
            }
        }

        private void insertSingleShare() {
            ContentValues contentValues = new ContentValues();
            contentValues.put("uri", this.mUri);
            contentValues.put(BluetoothShare.CARRIER_NAME, BluetoothOppUtility.getFilePathFromUri(Uri.parse(this.mUri)));
            contentValues.put(BluetoothShare.MIMETYPE, this.mTypeOfSingleFile);
            contentValues.put(BluetoothShare.DESTINATION, this.mRemoteDevice.getAddress());
            if (this.mIsHandoverInitiated) {
                contentValues.put("confirm", (Integer) 5);
            }
            Uri uriInsert = BluetoothOppManager.this.mContext.getContentResolver().insert(BluetoothShare.CONTENT_URI, contentValues);
            if (BluetoothOppManager.V) {
                Log.v(BluetoothOppManager.TAG, "Insert contentUri: " + uriInsert + "  to device: " + BluetoothOppManager.this.getDeviceName(this.mRemoteDevice));
            }
        }
    }

    void cleanUpSendingFileInfo() {
        synchronized (this) {
            if (V) {
                Log.v(TAG, "cleanUpSendingFileInfo: mMultipleFlag = " + this.mMultipleFlag);
            }
            if (!this.mMultipleFlag && this.mUriOfSendingFile != null) {
                BluetoothOppUtility.closeSendFileInfo(Uri.parse(this.mUriOfSendingFile));
            } else if (this.mUrisOfSendingFiles != null) {
                Iterator<Uri> it = this.mUrisOfSendingFiles.iterator();
                while (it.hasNext()) {
                    BluetoothOppUtility.closeSendFileInfo(it.next());
                }
            }
        }
    }
}
