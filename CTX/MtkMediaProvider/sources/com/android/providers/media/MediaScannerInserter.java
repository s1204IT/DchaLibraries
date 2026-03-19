package com.android.providers.media;

import android.content.ContentValues;
import android.content.Context;
import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ICancellationSignal;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MediaScannerInserter {
    private Context mContext;
    private Handler mInsertHanlder;
    private IContentProvider mMediaProvider;
    private String mPackageName;
    private Handler mServiceHandler;
    private static final Uri AUDIO_URI = MediaStore.Audio.Media.getContentUri("external");
    private static final Uri IMAGE_URI = MediaStore.Images.Media.getContentUri("external");
    private static final Uri VIDEO_URI = MediaStore.Video.Media.getContentUri("external");
    protected static final Uri FILE_URI = MediaStore.Files.getContentUri("external");
    private boolean mHasStoppedInsert = false;
    private HashMap<Uri, List<ContentValues>> mNormalMap = new HashMap<>(4);

    public MediaScannerInserter(Context context, Handler handler) {
        this.mContext = context;
        this.mServiceHandler = handler;
        this.mPackageName = this.mContext.getPackageName();
        this.mMediaProvider = this.mContext.getContentResolver().acquireProvider("media");
        HandlerThread handlerThread = new HandlerThread("MediaInserterExImpl");
        handlerThread.start();
        this.mInsertHanlder = new MediaInsertHandler(handlerThread.getLooper());
    }

    public Handler getInsertHandler() {
        return this.mInsertHanlder;
    }

    public void release() {
        MtkLog.v("MediaScannerInserter", "release MediaScannerInserter");
        if (this.mInsertHanlder != null) {
            this.mInsertHanlder.getLooper().quit();
            this.mInsertHanlder = null;
        }
        this.mServiceHandler = null;
        this.mContext = null;
        this.mMediaProvider = null;
    }

    private class MediaInsertHandler extends Handler {
        public MediaInsertHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                List list = (List) message.obj;
                Uri uri = Uri.parse(((ContentValues) list.remove(0)).getAsString("insert_table_uri_key"));
                if (message.arg1 > 0) {
                    MediaScannerInserter.this.insertPriority(uri, list);
                    return;
                } else if (MediaScannerInserter.AUDIO_URI.equals(uri) || MediaScannerInserter.VIDEO_URI.equals(uri) || MediaScannerInserter.IMAGE_URI.equals(uri)) {
                    MediaScannerInserter.this.insertMedia(uri, list);
                    return;
                } else {
                    MediaScannerInserter.this.insertNormal(uri, list);
                    return;
                }
            }
            if (message.what != 1) {
                if (message.what == 2) {
                    MediaScannerInserter.this.flushAll();
                    MtkLog.v("MediaScannerInserter", "All entries have been inserted, scan finished");
                    MediaScannerInserter.this.mServiceHandler.sendEmptyMessage(13);
                    return;
                } else {
                    if (message.what == 3) {
                        MediaScannerInserter.this.stopInsert();
                        return;
                    }
                    MtkLog.w("MediaScannerInserter", "unsupport message " + message.what);
                    return;
                }
            }
            List<ContentValues> list2 = (List) message.obj;
            ArrayList arrayList = new ArrayList(500);
            for (ContentValues contentValues : list2) {
                String asString = contentValues.getAsString("_data");
                if (asString != null && !MediaScannerInserter.this.isExistInDatabase(asString)) {
                    arrayList.add(contentValues);
                }
                if (arrayList.size() >= 500) {
                    MediaScannerInserter.this.flush(MediaScannerInserter.FILE_URI, arrayList);
                    arrayList.clear();
                }
            }
            if (!arrayList.isEmpty()) {
                MediaScannerInserter.this.flush(MediaScannerInserter.FILE_URI, arrayList);
            }
        }
    }

    private void insertPriority(Uri uri, List<ContentValues> list) {
        flush(uri, list);
    }

    private void insertMedia(Uri uri, List<ContentValues> list) {
        flush(uri, list);
    }

    private void insertNormal(Uri uri, List<ContentValues> list) {
        insert(uri, list);
    }

    private void insert(Uri uri, List<ContentValues> list) {
        List<ContentValues> list2 = this.mNormalMap.get(uri);
        if (list2 == null) {
            this.mNormalMap.put(uri, list);
            return;
        }
        int size = list.size();
        int size2 = list2.size();
        if (size + size2 <= 500) {
            list2.addAll(list);
        } else if (size > size2) {
            flush(uri, list);
            this.mNormalMap.put(uri, list2);
        } else {
            flush(uri, list2);
            this.mNormalMap.put(uri, list);
        }
    }

    private void flush(Uri uri, List<ContentValues> list) {
        if (this.mHasStoppedInsert) {
            MtkLog.d("MediaScannerInserter", "skip flush to database because has stopped inserting");
            return;
        }
        System.currentTimeMillis();
        try {
            this.mMediaProvider.bulkInsert(this.mPackageName, uri, (ContentValues[]) list.toArray(new ContentValues[list.size()]));
        } catch (Exception e) {
            MtkLog.e("MediaScannerInserter", "bulkInsert with Exception for " + uri, e);
        }
        System.currentTimeMillis();
    }

    private void flushAll() {
        for (Uri uri : this.mNormalMap.keySet()) {
            flush(uri, this.mNormalMap.get(uri));
        }
        this.mNormalMap.clear();
    }

    private boolean isExistInDatabase(String str) throws Throwable {
        boolean z;
        Bundle bundle = new Bundle();
        bundle.putString("_data", str);
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = this.mMediaProvider.query(this.mPackageName, FILE_URI, (String[]) null, bundle, (ICancellationSignal) null);
                if (cursorQuery != null) {
                    try {
                        z = cursorQuery.moveToFirst();
                    } catch (Exception e) {
                        e = e;
                        cursor = cursorQuery;
                        MtkLog.e("MediaScannerInserter", "Check isExistInDatabase with Exception for " + str, e);
                        if (cursor != null) {
                            cursor.close();
                        }
                        return true;
                    } catch (Throwable th) {
                        th = th;
                        cursor = cursorQuery;
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return z;
            } catch (Exception e2) {
                e = e2;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void stopInsert() {
        MtkLog.d("MediaScannerInserter", "stopInsert so remove insert msg and clear insert cache");
        if (this.mInsertHanlder == null) {
            return;
        }
        this.mHasStoppedInsert = true;
        this.mInsertHanlder.removeCallbacksAndMessages(0);
        this.mInsertHanlder.removeCallbacksAndMessages(1);
        this.mNormalMap.clear();
    }
}
