package com.mediatek.media.mediascanner;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MediaInserterExImpl {
    public static final String INSERT_TABLE_URI_KEY = "insert_table_uri_key";
    public static final int MSG_INSERT_ALL = 2;
    public static final int MSG_INSERT_FOLDER = 1;
    public static final int MSG_INSERT_TO_DATABASE = 0;
    public static final int MSG_SCAN_DIRECTORY = 10;
    public static final int MSG_SCAN_FINISH_WITH_THREADPOOL = 13;
    public static final int MSG_SCAN_SINGLE_FILE = 11;
    public static final int MSG_SHUTDOWN_THREADPOOL = 12;
    public static final int MSG_STOP_INSERT = 3;
    private final int mBufferSizePerUri;
    private Handler mInsertHanlder;
    private final HashMap<Uri, List<ContentValues>> mPriorityRowMap;
    private final ContentProviderClient mProvider;
    private final HashMap<Uri, List<ContentValues>> mRowMap;

    public MediaInserterExImpl(ContentProviderClient contentProviderClient, int i) {
        this.mRowMap = new HashMap<>();
        this.mPriorityRowMap = new HashMap<>();
        this.mProvider = contentProviderClient;
        this.mBufferSizePerUri = i;
    }

    public void insert(Uri uri, ContentValues contentValues) throws RemoteException {
        insert(uri, contentValues, false);
    }

    public void insertwithPriority(Uri uri, ContentValues contentValues) throws RemoteException {
        insert(uri, contentValues, true);
    }

    private void insert(Uri uri, ContentValues contentValues, boolean z) throws RemoteException {
        HashMap<Uri, List<ContentValues>> map = z ? this.mPriorityRowMap : this.mRowMap;
        List<ContentValues> arrayList = map.get(uri);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            map.put(uri, arrayList);
        }
        arrayList.add(new ContentValues(contentValues));
        if (arrayList.size() >= this.mBufferSizePerUri) {
            flushAllPriority();
            flush(uri, arrayList);
        }
    }

    public void flushAll() throws RemoteException {
        flushAllPriority();
        for (Uri uri : this.mRowMap.keySet()) {
            flush(uri, this.mRowMap.get(uri));
        }
        this.mRowMap.clear();
    }

    private void flushAllPriority() throws RemoteException {
        for (Uri uri : this.mPriorityRowMap.keySet()) {
            flushPriority(uri, this.mPriorityRowMap.get(uri));
        }
        this.mPriorityRowMap.clear();
    }

    private void flush(Uri uri, List<ContentValues> list) throws RemoteException {
        if (!list.isEmpty()) {
            if (this.mProvider != null) {
                this.mProvider.bulkInsert(uri, (ContentValues[]) list.toArray(new ContentValues[list.size()]));
                list.clear();
                return;
            }
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(INSERT_TABLE_URI_KEY, uri.toString());
            ArrayList arrayList = new ArrayList(list.size() + 1);
            arrayList.add(contentValues);
            arrayList.addAll(list);
            list.clear();
            this.mInsertHanlder.sendMessage(this.mInsertHanlder.obtainMessage(0, -1, -1, arrayList));
        }
    }

    public MediaInserterExImpl(Handler handler, int i) {
        this.mRowMap = new HashMap<>();
        this.mPriorityRowMap = new HashMap<>();
        this.mInsertHanlder = handler;
        this.mBufferSizePerUri = i;
        this.mProvider = null;
    }

    private void flushPriority(Uri uri, List<ContentValues> list) throws RemoteException {
        if (!list.isEmpty()) {
            if (this.mProvider != null) {
                this.mProvider.bulkInsert(uri, (ContentValues[]) list.toArray(new ContentValues[list.size()]));
                list.clear();
                return;
            }
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(INSERT_TABLE_URI_KEY, uri.toString());
            ArrayList arrayList = new ArrayList(list.size() + 1);
            arrayList.add(contentValues);
            arrayList.addAll(list);
            list.clear();
            this.mInsertHanlder.sendMessage(this.mInsertHanlder.obtainMessage(0, 1, -1, arrayList));
        }
    }
}
