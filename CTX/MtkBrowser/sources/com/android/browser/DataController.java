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
    private DataControllerHandler mDataHandler = new DataControllerHandler(this);

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

    class DataControllerHandler extends Thread {
        private BlockingQueue<DCMessage> mMessageQueue;
        final DataController this$0;

        public DataControllerHandler(DataController dataController) {
            super("DataControllerHandler");
            this.this$0 = dataController;
            this.mMessageQueue = new LinkedBlockingQueue();
        }

        private void doLoadThumbnail(Tab tab) throws Throwable {
            Cursor cursorQuery;
            byte[] blob;
            try {
                cursorQuery = this.this$0.mContext.getContentResolver().query(ContentUris.withAppendedId(BrowserProvider2.Thumbnails.CONTENT_URI, tab.getId()), new String[]{"_id", "thumbnail"}, null, null, null);
                try {
                    if (cursorQuery.moveToFirst() && !cursorQuery.isNull(1) && (blob = cursorQuery.getBlob(1)) != null && blob.length > 0) {
                        tab.updateCaptureFromBlob(blob);
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                cursorQuery = null;
            }
        }

        private void doQueryBookmarkStatus(String str, Object obj) throws Throwable {
            Cursor cursorQuery;
            boolean zMoveToFirst;
            Cursor cursor = null;
            Object[] objArr = 0;
            try {
                cursorQuery = this.this$0.mContext.getContentResolver().query(BookmarkUtils.getBookmarksUri(this.this$0.mContext), new String[]{"url"}, "url == ?", new String[]{str}, null);
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
            this.this$0.mCbHandler.obtainMessage(200, callbackContainer).sendToTarget();
        }

        private void doSaveThumbnail(Tab tab) {
            byte[] captureBlob = getCaptureBlob(tab);
            if (captureBlob == null) {
                return;
            }
            ContentResolver contentResolver = this.this$0.mContext.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put("_id", Long.valueOf(tab.getId()));
            contentValues.put("thumbnail", captureBlob);
            contentResolver.insert(BrowserProvider2.Thumbnails.CONTENT_URI, contentValues);
        }

        private void doUpdateHistoryTitle(String str, String str2) throws Throwable {
            String strFindHistoryUrlInBookmark = findHistoryUrlInBookmark(str);
            ContentResolver contentResolver = this.this$0.mContext.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put("title", str2);
            contentValues.put("url", strFindHistoryUrlInBookmark);
            if (contentResolver.update(BrowserContract.History.CONTENT_URI, contentValues, "url==?", new String[]{str}) > 0 || !strFindHistoryUrlInBookmark.endsWith("/")) {
                return;
            }
            contentResolver.update(BrowserContract.History.CONTENT_URI, contentValues, "url==?", new String[]{strFindHistoryUrlInBookmark.substring(0, strFindHistoryUrlInBookmark.lastIndexOf("/"))});
        }

        private void doUpdateVisitedHistory(String str) throws Throwable {
            Cursor cursorQuery;
            String strSubstring;
            String strFindHistoryUrlInBookmark = findHistoryUrlInBookmark(str);
            ContentResolver contentResolver = this.this$0.mContext.getContentResolver();
            try {
                Uri uri = BrowserContract.History.CONTENT_URI;
                if (str.endsWith("/")) {
                    strSubstring = str.substring(0, str.lastIndexOf("/"));
                } else {
                    strSubstring = str + "/";
                }
                cursorQuery = contentResolver.query(uri, new String[]{"_id", "visits"}, "url==? OR url==?", new String[]{str, strSubstring}, null);
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
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                cursorQuery = null;
            }
        }

        private String findHistoryUrlInBookmark(String str) throws Throwable {
            Cursor cursorQuery;
            String strSubstring;
            try {
                if (DataController.DEBUG) {
                    Log.d("DataController", "historyUrl is: " + str);
                }
                ContentResolver contentResolver = this.this$0.mContext.getContentResolver();
                Uri bookmarksUri = BookmarkUtils.getBookmarksUri(this.this$0.mContext);
                if (str.endsWith("/")) {
                    strSubstring = str.substring(0, str.lastIndexOf("/"));
                } else {
                    strSubstring = str + "/";
                }
                cursorQuery = contentResolver.query(bookmarksUri, new String[]{"url"}, "url == ? OR url == ?", new String[]{str, strSubstring}, null);
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
                        if (cursorQuery != null) {
                            cursorQuery.close();
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
                cursorQuery = null;
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
                if (this.this$0.mBuffer == null || this.this$0.mBuffer.limit() < byteArrayOutputStream.size()) {
                    this.this$0.mBuffer = ByteBuffer.allocate(byteArrayOutputStream.size());
                }
                this.this$0.mBuffer.put(byteArrayOutputStream.toByteArray());
                this.this$0.mBuffer.rewind();
                return this.this$0.mBuffer.array();
            }
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
                                this.this$0.mContext.getContentResolver().delete(ContentUris.withAppendedId(BrowserProvider2.Thumbnails.CONTENT_URI, ((Long) dCMessage.obj).longValue()), null, null);
                            } catch (Throwable th) {
                                return;
                            }
                            break;
                    }
                    break;
            }
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
    }

    interface OnQueryUrlIsBookmark {
        void onQueryUrlIsBookmark(String str, boolean z);
    }

    private DataController(Context context) {
        this.mContext = context.getApplicationContext();
        this.mDataHandler.start();
        this.mCbHandler = new Handler(this) {
            final DataController this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void handleMessage(Message message) {
                CallbackContainer callbackContainer = (CallbackContainer) message.obj;
                if (message.what != 200) {
                    return;
                }
                ((OnQueryUrlIsBookmark) callbackContainer.replyTo).onQueryUrlIsBookmark((String) callbackContainer.args[0], ((Boolean) callbackContainer.args[1]).booleanValue());
            }
        };
    }

    static DataController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DataController(context);
        }
        return sInstance;
    }

    public void deleteThumbnail(Tab tab) {
        this.mDataHandler.sendMessage(203, Long.valueOf(tab.getId()));
    }

    public void loadThumbnail(Tab tab) {
        this.mDataHandler.sendMessage(201, tab);
    }

    public void queryBookmarkStatus(String str, OnQueryUrlIsBookmark onQueryUrlIsBookmark) {
        if (str == null || str.trim().length() == 0) {
            onQueryUrlIsBookmark.onQueryUrlIsBookmark(str, false);
        } else {
            this.mDataHandler.sendMessage(200, str.trim(), onQueryUrlIsBookmark);
        }
    }

    public void saveThumbnail(Tab tab) {
        this.mDataHandler.sendMessage(202, tab);
    }

    public void updateHistoryTitle(String str, String str2) {
        this.mDataHandler.sendMessage(101, new String[]{str, str2});
    }

    public void updateVisitedHistory(String str) {
        this.mDataHandler.sendMessage(100, str);
    }
}
