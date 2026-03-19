package com.android.browser;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.android.browser.provider.BrowserContract;
import com.android.browser.provider.BrowserProvider2;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DataController {
    private static final boolean DEBUG = Browser.DEBUG;
    private static DataController sInstance;
    private ByteBuffer mBuffer;
    private Handler mCbHandler;
    private Context mContext;
    private DataControllerHandler mDataHandler = new DataControllerHandler();

    interface OnQueryUrlIsBookmark {
        void onQueryUrlIsBookmark(String str, boolean z);
    }

    private static class CallbackContainer {
        Object[] args;
        Object replyTo;

        private CallbackContainer() {
        }
    }

    private static class DCMessage {
        Object obj;
        Object replyTo;
        int what;

        DCMessage(int i, Object obj) {
            this.what = i;
            this.obj = obj;
        }
    }

    static DataController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DataController(context);
        }
        return sInstance;
    }

    private DataController(Context context) {
        this.mContext = context.getApplicationContext();
        this.mDataHandler.start();
        this.mCbHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                CallbackContainer callbackContainer = (CallbackContainer) message.obj;
                if (message.what == 200) {
                    ((OnQueryUrlIsBookmark) callbackContainer.replyTo).onQueryUrlIsBookmark((String) callbackContainer.args[0], ((Boolean) callbackContainer.args[1]).booleanValue());
                }
            }
        };
    }

    public void updateVisitedHistory(String str) {
        this.mDataHandler.sendMessage(100, str);
    }

    public void updateHistoryTitle(String str, String str2) {
        this.mDataHandler.sendMessage(101, new String[]{str, str2});
    }

    public void queryBookmarkStatus(String str, OnQueryUrlIsBookmark onQueryUrlIsBookmark) {
        if (str == null || str.trim().length() == 0) {
            onQueryUrlIsBookmark.onQueryUrlIsBookmark(str, false);
        } else {
            this.mDataHandler.sendMessage(200, str.trim(), onQueryUrlIsBookmark);
        }
    }

    public void loadThumbnail(Tab tab) {
        this.mDataHandler.sendMessage(201, tab);
    }

    public void deleteThumbnail(Tab tab) {
        this.mDataHandler.sendMessage(203, Long.valueOf(tab.getId()));
    }

    public void saveThumbnail(Tab tab) {
        this.mDataHandler.sendMessage(202, tab);
    }

    class DataControllerHandler extends Thread {
        private BlockingQueue<DCMessage> mMessageQueue;

        public DataControllerHandler() {
            super("DataControllerHandler");
            this.mMessageQueue = new LinkedBlockingQueue();
        }

        @Override
        public void run() throws Throwable {
            setPriority(1);
            while (true) {
                try {
                    handleMessage(this.mMessageQueue.take());
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        void sendMessage(int i, Object obj) {
            this.mMessageQueue.add(new DCMessage(i, obj));
        }

        void sendMessage(int i, Object obj, Object obj2) {
            DCMessage dCMessage = new DCMessage(i, obj);
            dCMessage.replyTo = obj2;
            this.mMessageQueue.add(dCMessage);
        }

        private void handleMessage(DCMessage dCMessage) throws Throwable {
            int i = dCMessage.what;
            switch (i) {
                case 100:
                    doUpdateVisitedHistory((String) dCMessage.obj);
                    break;
                case 101:
                    String[] strArr = (String[]) dCMessage.obj;
                    doUpdateHistoryTitle(strArr[0], strArr[1]);
                    break;
                default:
                    switch (i) {
                        case 200:
                            doQueryBookmarkStatus((String) dCMessage.obj, dCMessage.replyTo);
                            break;
                        case 201:
                            doLoadThumbnail((Tab) dCMessage.obj);
                            break;
                        case 202:
                            doSaveThumbnail((Tab) dCMessage.obj);
                            break;
                        case 203:
                            try {
                                DataController.this.mContext.getContentResolver().delete(ContentUris.withAppendedId(BrowserProvider2.Thumbnails.CONTENT_URI, ((Long) dCMessage.obj).longValue()), null, null);
                            } catch (Throwable th) {
                                return;
                            }
                            break;
                    }
                    break;
            }
        }

        private byte[] getCaptureBlob(Tab tab) {
            synchronized (tab) {
                Bitmap screenshot = tab.getScreenshot();
                if (screenshot == null) {
                    return null;
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                if (DataController.this.mBuffer == null || DataController.this.mBuffer.limit() < byteArrayOutputStream.size()) {
                    DataController.this.mBuffer = ByteBuffer.allocate(byteArrayOutputStream.size());
                }
                DataController.this.mBuffer.put(byteArrayOutputStream.toByteArray());
                DataController.this.mBuffer.rewind();
                return DataController.this.mBuffer.array();
            }
        }

        private void doSaveThumbnail(Tab tab) {
            byte[] captureBlob = getCaptureBlob(tab);
            if (captureBlob != null) {
                ContentResolver contentResolver = DataController.this.mContext.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put("_id", Long.valueOf(tab.getId()));
                contentValues.put("thumbnail", captureBlob);
                contentResolver.insert(BrowserProvider2.Thumbnails.CONTENT_URI, contentValues);
            }
        }

        private void doLoadThumbnail(Tab tab) throws Throwable {
            byte[] blob;
            Cursor cursor = null;
            try {
                Cursor cursorQuery = DataController.this.mContext.getContentResolver().query(ContentUris.withAppendedId(BrowserProvider2.Thumbnails.CONTENT_URI, tab.getId()), new String[]{"_id", "thumbnail"}, null, null, null);
                try {
                    if (cursorQuery.moveToFirst() && !cursorQuery.isNull(1) && (blob = cursorQuery.getBlob(1)) != null && blob.length > 0) {
                        tab.updateCaptureFromBlob(blob);
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }

        private String findHistoryUrlInBookmark(String str) throws Throwable {
            String strSubstring;
            Cursor cursor = null;
            try {
                if (DataController.DEBUG) {
                    Log.d("DataController", "historyUrl is: " + str);
                }
                ContentResolver contentResolver = DataController.this.mContext.getContentResolver();
                Uri bookmarksUri = BookmarkUtils.getBookmarksUri(DataController.this.mContext);
                String[] strArr = {"url"};
                String[] strArr2 = new String[2];
                strArr2[0] = str;
                if (str.endsWith("/")) {
                    strSubstring = str.substring(0, str.lastIndexOf("/"));
                } else {
                    strSubstring = str + "/";
                }
                strArr2[1] = strSubstring;
                Cursor cursorQuery = contentResolver.query(bookmarksUri, strArr, "url == ? OR url == ?", strArr2, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToNext()) {
                            str = cursorQuery.getString(0);
                            if (DataController.DEBUG) {
                                Log.d("DataController", "Url in bookmark table is: " + str);
                                Log.d("DataController", "save url to history table is: " + str);
                            }
                        }
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
                return str;
            } catch (Throwable th2) {
                th = th2;
            }
        }

        private void doUpdateVisitedHistory(String str) throws Throwable {
            Throwable th;
            Cursor cursorQuery;
            String strSubstring;
            String strFindHistoryUrlInBookmark = findHistoryUrlInBookmark(str);
            ContentResolver contentResolver = DataController.this.mContext.getContentResolver();
            try {
                Uri uri = BrowserContract.History.CONTENT_URI;
                String[] strArr = {"_id", "visits"};
                String[] strArr2 = new String[2];
                strArr2[0] = str;
                if (str.endsWith("/")) {
                    strSubstring = str.substring(0, str.lastIndexOf("/"));
                } else {
                    strSubstring = str + "/";
                }
                strArr2[1] = strSubstring;
                cursorQuery = contentResolver.query(uri, strArr, "url==? OR url==?", strArr2, null);
                try {
                    if (cursorQuery.moveToFirst()) {
                        if (DataController.DEBUG) {
                            Log.d("DataController", "update history to " + strFindHistoryUrlInBookmark);
                        }
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("url", strFindHistoryUrlInBookmark);
                        contentValues.put("visits", Integer.valueOf(cursorQuery.getInt(1) + 1));
                        contentValues.put("date", Long.valueOf(System.currentTimeMillis()));
                        contentResolver.update(ContentUris.withAppendedId(BrowserContract.History.CONTENT_URI, cursorQuery.getLong(0)), contentValues, null, null);
                    } else {
                        if (DataController.DEBUG) {
                            Log.d("DataController", "insert new history to " + strFindHistoryUrlInBookmark);
                        }
                        com.android.browser.provider.Browser.truncateHistory(contentResolver);
                        ContentValues contentValues2 = new ContentValues();
                        contentValues2.put("url", strFindHistoryUrlInBookmark);
                        contentValues2.put("visits", (Integer) 1);
                        contentValues2.put("date", Long.valueOf(System.currentTimeMillis()));
                        contentValues2.put("title", strFindHistoryUrlInBookmark);
                        contentValues2.put("created", (Integer) 0);
                        contentValues2.put("user_entered", (Integer) 0);
                        contentResolver.insert(BrowserContract.History.CONTENT_URI, contentValues2);
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                cursorQuery = null;
            }
        }

        private void doQueryBookmarkStatus(String str, Object obj) throws Throwable {
            Cursor cursorQuery;
            boolean zMoveToFirst;
            Cursor cursor = null;
            Object[] objArr = 0;
            try {
                cursorQuery = DataController.this.mContext.getContentResolver().query(BookmarkUtils.getBookmarksUri(DataController.this.mContext), new String[]{"url"}, "url == ?", new String[]{str}, null);
                try {
                    try {
                        zMoveToFirst = cursorQuery.moveToFirst();
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                    } catch (SQLiteException e) {
                        e = e;
                        Log.e("DataController", "Error checking for bookmark: " + e);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        zMoveToFirst = false;
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            } catch (SQLiteException e2) {
                e = e2;
                cursorQuery = null;
            } catch (Throwable th2) {
                th = th2;
                if (cursor != null) {
                }
                throw th;
            }
            CallbackContainer callbackContainer = new CallbackContainer();
            callbackContainer.replyTo = obj;
            callbackContainer.args = new Object[]{str, Boolean.valueOf(zMoveToFirst)};
            DataController.this.mCbHandler.obtainMessage(200, callbackContainer).sendToTarget();
        }

        private void doUpdateHistoryTitle(String str, String str2) throws Throwable {
            String strFindHistoryUrlInBookmark = findHistoryUrlInBookmark(str);
            ContentResolver contentResolver = DataController.this.mContext.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put("title", str2);
            contentValues.put("url", strFindHistoryUrlInBookmark);
            if (contentResolver.update(BrowserContract.History.CONTENT_URI, contentValues, "url==?", new String[]{str}) <= 0 && strFindHistoryUrlInBookmark.endsWith("/")) {
                contentResolver.update(BrowserContract.History.CONTENT_URI, contentValues, "url==?", new String[]{strFindHistoryUrlInBookmark.substring(0, strFindHistoryUrlInBookmark.lastIndexOf("/"))});
            }
        }
    }
}
