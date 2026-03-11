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
import android.provider.BrowserContract;
import android.util.Log;
import com.android.browser.provider.BrowserProvider2;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DataController {
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

        DCMessage(int w, Object o) {
            this.what = w;
            this.obj = o;
        }
    }

    static DataController getInstance(Context c) {
        if (sInstance == null) {
            sInstance = new DataController(c);
        }
        return sInstance;
    }

    private DataController(Context c) {
        this.mContext = c.getApplicationContext();
        this.mDataHandler.start();
        this.mCbHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                CallbackContainer cc = (CallbackContainer) msg.obj;
                switch (msg.what) {
                    case 200:
                        OnQueryUrlIsBookmark cb = (OnQueryUrlIsBookmark) cc.replyTo;
                        String url = (String) cc.args[0];
                        boolean isBookmark = ((Boolean) cc.args[1]).booleanValue();
                        cb.onQueryUrlIsBookmark(url, isBookmark);
                        break;
                }
            }
        };
    }

    public void updateVisitedHistory(String url) {
        this.mDataHandler.sendMessage(100, url);
    }

    public void updateHistoryTitle(String url, String title) {
        this.mDataHandler.sendMessage(101, new String[]{url, title});
    }

    public void queryBookmarkStatus(String url, OnQueryUrlIsBookmark replyTo) {
        if (url == null || url.trim().length() == 0) {
            replyTo.onQueryUrlIsBookmark(url, false);
        } else {
            this.mDataHandler.sendMessage(200, url.trim(), replyTo);
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
        public void run() {
            setPriority(1);
            while (true) {
                try {
                    handleMessage(this.mMessageQueue.take());
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        void sendMessage(int what, Object obj) {
            DCMessage m = new DCMessage(what, obj);
            this.mMessageQueue.add(m);
        }

        void sendMessage(int what, Object obj, Object replyTo) {
            DCMessage m = new DCMessage(what, obj);
            m.replyTo = replyTo;
            this.mMessageQueue.add(m);
        }

        private void handleMessage(DCMessage msg) {
            switch (msg.what) {
                case 100:
                    doUpdateVisitedHistory((String) msg.obj);
                    break;
                case 101:
                    String[] args = (String[]) msg.obj;
                    doUpdateHistoryTitle(args[0], args[1]);
                    break;
                case 200:
                    doQueryBookmarkStatus((String) msg.obj, msg.replyTo);
                    break;
                case 201:
                    doLoadThumbnail((Tab) msg.obj);
                    break;
                case 202:
                    doSaveThumbnail((Tab) msg.obj);
                    break;
                case 203:
                    ContentResolver cr = DataController.this.mContext.getContentResolver();
                    try {
                        cr.delete(ContentUris.withAppendedId(BrowserProvider2.Thumbnails.CONTENT_URI, ((Long) msg.obj).longValue()), null, null);
                    } catch (Throwable th) {
                        return;
                    }
                    break;
            }
        }

        private byte[] getCaptureBlob(Tab tab) {
            byte[] bArrArray;
            synchronized (tab) {
                Bitmap capture = tab.getScreenshot();
                if (capture != null) {
                    if (DataController.this.mBuffer == null || DataController.this.mBuffer.limit() < capture.getByteCount()) {
                        DataController.this.mBuffer = ByteBuffer.allocate(capture.getByteCount());
                    }
                    capture.copyPixelsToBuffer(DataController.this.mBuffer);
                    DataController.this.mBuffer.rewind();
                    bArrArray = DataController.this.mBuffer.array();
                } else {
                    bArrArray = null;
                }
            }
            return bArrArray;
        }

        private void doSaveThumbnail(Tab tab) {
            byte[] blob = getCaptureBlob(tab);
            if (blob != null) {
                ContentResolver cr = DataController.this.mContext.getContentResolver();
                ContentValues values = new ContentValues();
                values.put("_id", Long.valueOf(tab.getId()));
                values.put("thumbnail", blob);
                cr.insert(BrowserProvider2.Thumbnails.CONTENT_URI, values);
            }
        }

        private void doLoadThumbnail(Tab tab) {
            byte[] data;
            ContentResolver cr = DataController.this.mContext.getContentResolver();
            Cursor c = null;
            try {
                Uri uri = ContentUris.withAppendedId(BrowserProvider2.Thumbnails.CONTENT_URI, tab.getId());
                c = cr.query(uri, new String[]{"_id", "thumbnail"}, null, null, null);
                if (c.moveToFirst() && (data = c.getBlob(1)) != null && data.length > 0) {
                    tab.updateCaptureFromBlob(data);
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        private void doUpdateVisitedHistory(String url) {
            ContentResolver cr = DataController.this.mContext.getContentResolver();
            Cursor c = null;
            try {
                c = cr.query(BrowserContract.History.CONTENT_URI, new String[]{"_id", "visits"}, "url=?", new String[]{url}, null);
                if (c.moveToFirst()) {
                    ContentValues values = new ContentValues();
                    values.put("visits", Integer.valueOf(c.getInt(1) + 1));
                    values.put("date", Long.valueOf(System.currentTimeMillis()));
                    cr.update(ContentUris.withAppendedId(BrowserContract.History.CONTENT_URI, c.getLong(0)), values, null, null);
                } else {
                    android.provider.Browser.truncateHistory(cr);
                    ContentValues values2 = new ContentValues();
                    values2.put("url", url);
                    values2.put("visits", (Integer) 1);
                    values2.put("date", Long.valueOf(System.currentTimeMillis()));
                    values2.put("title", url);
                    values2.put("created", (Integer) 0);
                    values2.put("user_entered", (Integer) 0);
                    cr.insert(BrowserContract.History.CONTENT_URI, values2);
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        private void doQueryBookmarkStatus(String url, Object replyTo) {
            Cursor cursor = null;
            boolean isBookmark = false;
            try {
                try {
                    cursor = DataController.this.mContext.getContentResolver().query(BookmarkUtils.getBookmarksUri(DataController.this.mContext), new String[]{"url"}, "url == ?", new String[]{url}, null);
                    isBookmark = cursor.moveToFirst();
                } catch (SQLiteException e) {
                    Log.e("DataController", "Error checking for bookmark: " + e);
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                CallbackContainer cc = new CallbackContainer();
                cc.replyTo = replyTo;
                cc.args = new Object[]{url, Boolean.valueOf(isBookmark)};
                DataController.this.mCbHandler.obtainMessage(200, cc).sendToTarget();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        private void doUpdateHistoryTitle(String url, String title) {
            ContentResolver cr = DataController.this.mContext.getContentResolver();
            ContentValues values = new ContentValues();
            values.put("title", title);
            cr.update(BrowserContract.History.CONTENT_URI, values, "url=?", new String[]{url});
        }
    }
}
