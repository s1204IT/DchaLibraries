package com.android.browser;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebIconDatabase;
import android.widget.Toast;
import com.android.browser.provider.BrowserContract;
import java.io.ByteArrayOutputStream;

public class Bookmarks {
    private static final String[] acceptableBookmarkSchemes = {"http:", "https:", "about:", "data:", "javascript:", "file:", "content:", "rtsp:"};

    static void addBookmark(Context context, boolean showToast, String url, String name, Bitmap thumbnail, long parent) {
        ContentValues values = new ContentValues();
        deleteSameTitle(context, name, parent);
        deleteSameUrl(context, url, parent);
        try {
            values.put("title", name);
            values.put("url", url);
            values.put("folder", (Integer) 0);
            values.put("thumbnail", bitmapToBytes(thumbnail));
            values.put("parent", Long.valueOf(parent));
            context.getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, values);
        } catch (IllegalStateException e) {
            Log.e("Bookmarks", "addBookmark", e);
        }
        if (!showToast) {
            return;
        }
        Toast.makeText(context, R.string.added_to_bookmarks, 1).show();
    }

    private static void deleteSameUrl(Context context, String url, long parent) {
        Log.d("browser/Bookmarks", "deleteSameUrl url:" + url);
        if (url == null || url.length() == 0) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("deleted", (Integer) 1);
        int count = context.getContentResolver().update(BrowserContract.Bookmarks.CONTENT_URI, values, "url =? AND parent =? AND deleted =?", new String[]{url, String.valueOf(parent), String.valueOf(0)});
        Log.d("browser/Bookmarks", "same url delete :" + count);
    }

    private static void deleteSameTitle(Context context, String name, long parent) {
        Log.d("browser/Bookmarks", "deleteSameTitle title:" + name);
        if (name == null || name.length() == 0) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("deleted", (Integer) 1);
        int count = context.getContentResolver().update(BrowserContract.Bookmarks.CONTENT_URI, values, "title =? AND parent =? AND deleted =? AND folder =0 ", new String[]{name, String.valueOf(parent), String.valueOf(0)});
        Log.d("browser/Bookmarks", "same title delete :" + count);
    }

    static void removeFromBookmarks(Context context, ContentResolver cr, String url, String title) {
        Cursor cursor = null;
        try {
            try {
                Uri uri = BookmarkUtils.getBookmarksUri(context);
                cursor = cr.query(uri, new String[]{"_id"}, "url = ? AND title = ?", new String[]{url, title}, null);
                if (!cursor.moveToFirst()) {
                    if (cursor != null) {
                        cursor.close();
                        return;
                    }
                    return;
                }
                WebIconDatabase.getInstance().releaseIconForPageUrl(url);
                Uri uri2 = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, cursor.getLong(0));
                cr.delete(uri2, null, null);
                if (context != null) {
                    Toast.makeText(context, R.string.removed_from_bookmarks, 1).show();
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (IllegalStateException e) {
                Log.e("Bookmarks", "removeFromBookmarks", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    private static byte[] bitmapToBytes(Bitmap bm) {
        if (bm == null) {
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, os);
        return os.toByteArray();
    }

    static boolean urlHasAcceptableScheme(String url) {
        if (url == null) {
            return false;
        }
        for (int i = 0; i < acceptableBookmarkSchemes.length; i++) {
            if (url.startsWith(acceptableBookmarkSchemes[i])) {
                return true;
            }
        }
        return false;
    }

    private static String modifyUrl(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    public static Cursor queryCombinedForUrl(ContentResolver cr, String originalUrl, String url) {
        if (cr == null || url == null) {
            return null;
        }
        if (originalUrl == null) {
            originalUrl = url;
        }
        String modifyOriginalUrl = modifyUrl(originalUrl);
        String modifyUrl = modifyUrl(url);
        String[] selArgs = {originalUrl, url, modifyOriginalUrl, modifyUrl};
        String[] projection = {"url"};
        return cr.query(BrowserContract.Combined.CONTENT_URI, projection, "url == ? OR url == ? OR url == ? OR url == ?", selArgs, null);
    }

    static String removeQuery(String url) {
        if (url == null) {
            return null;
        }
        int query = url.indexOf(63);
        if (query == -1) {
            return url;
        }
        String noQuery = url.substring(0, query);
        return noQuery;
    }

    static void updateFavicon(final ContentResolver cr, final String originalUrl, final String url, final Bitmap favicon) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... unused) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                favicon.compress(Bitmap.CompressFormat.PNG, 100, os);
                ContentValues values = new ContentValues();
                values.put("favicon", os.toByteArray());
                updateImages(cr, originalUrl, values);
                updateImages(cr, url, values);
                return null;
            }

            private void updateImages(ContentResolver cr2, String url2, ContentValues values) {
                String iurl = Bookmarks.removeQuery(url2);
                if (TextUtils.isEmpty(iurl)) {
                    return;
                }
                values.put("url_key", iurl);
                cr2.update(BrowserContract.Images.CONTENT_URI, values, null, null);
            }
        }.execute(new Void[0]);
    }

    static int getIdByNameOrUrl(ContentResolver cr, String name, String url, long parentId, long currentId) {
        String where = currentId > 0 ? "parent = ? AND (title = ? OR url = ? OR url = ?) AND folder= 0 AND _id <> " + currentId : "parent = ? AND (title = ? OR url = ? OR url = ?) AND folder= 0";
        Log.v("browser/Bookmarks", "getIdByNameOrUrl() sql:" + where);
        String[] projection = {"_id"};
        Uri uri = BrowserContract.Bookmarks.CONTENT_URI;
        String[] strArr = new String[4];
        strArr[0] = String.valueOf(parentId);
        strArr[1] = name;
        strArr[2] = url;
        strArr[3] = url.endsWith("/") ? url.substring(0, url.lastIndexOf("/")) : url + "/";
        Cursor cursor = cr.query(uri, projection, where, strArr, "_id DESC");
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
                return -1;
            } finally {
                cursor.close();
            }
        }
        return -1;
    }
}
