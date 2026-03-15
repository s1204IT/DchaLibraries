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
    private static final boolean DEBUG = Browser.DEBUG;

    static void addBookmark(Context context, boolean z, String str, String str2, Bitmap bitmap, long j) {
        ContentValues contentValues = new ContentValues();
        deleteSameTitle(context, str2, j);
        deleteSameUrl(context, str, j);
        try {
            contentValues.put("title", str2);
            contentValues.put("url", str);
            contentValues.put("folder", (Integer) 0);
            contentValues.put("thumbnail", bitmapToBytes(bitmap));
            contentValues.put("parent", Long.valueOf(j));
            context.getContentResolver().insert(BrowserContract.Bookmarks.CONTENT_URI, contentValues);
        } catch (IllegalStateException e) {
            Log.e("Bookmarks", "addBookmark", e);
        }
        if (z) {
            Toast.makeText(context, 2131492955, 1).show();
        }
    }

    private static byte[] bitmapToBytes(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private static void deleteSameTitle(Context context, String str, long j) {
        Log.d("browser/Bookmarks", "deleteSameTitle title:" + str);
        if (str == null || str.length() == 0) {
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("deleted", (Integer) 1);
        Log.d("browser/Bookmarks", "same title delete :" + context.getContentResolver().update(BrowserContract.Bookmarks.CONTENT_URI, contentValues, "title =? AND parent =? AND deleted =? AND folder =0 ", new String[]{str, String.valueOf(j), String.valueOf(0)}));
    }

    private static void deleteSameUrl(Context context, String str, long j) {
        if (DEBUG) {
            Log.d("browser/Bookmarks", "deleteSameUrl url:" + str);
        }
        if (str == null || str.length() == 0) {
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("deleted", (Integer) 1);
        Log.d("browser/Bookmarks", "same url delete :" + context.getContentResolver().update(BrowserContract.Bookmarks.CONTENT_URI, contentValues, "url =? AND parent =? AND deleted =?", new String[]{str, String.valueOf(j), String.valueOf(0)}));
    }

    static int getIdByNameOrUrl(ContentResolver contentResolver, String str, String str2, long j, long j2) {
        String strSubstring;
        String str3 = "parent = ? AND (title = ? OR url = ? OR url = ?) AND folder= 0";
        if (j2 > 0) {
            str3 = "parent = ? AND (title = ? OR url = ? OR url = ?) AND folder= 0 AND _id <> " + j2;
        }
        Log.v("browser/Bookmarks", "getIdByNameOrUrl() sql:" + str3);
        Uri uri = BrowserContract.Bookmarks.CONTENT_URI;
        if (str2.endsWith("/")) {
            strSubstring = str2.substring(0, str2.lastIndexOf("/"));
        } else {
            strSubstring = str2 + "/";
        }
        Cursor cursorQuery = contentResolver.query(uri, new String[]{"_id"}, str3, new String[]{String.valueOf(j), str, str2, strSubstring}, "_id DESC");
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    return cursorQuery.getInt(0);
                }
            } finally {
                cursorQuery.close();
            }
        }
        return -1;
    }

    private static String modifyUrl(String str) {
        return str.endsWith("/") ? str.substring(0, str.length() - 1) : str;
    }

    public static Cursor queryCombinedForUrl(ContentResolver contentResolver, String str, String str2) {
        if (contentResolver == null || str2 == null) {
            return null;
        }
        if (str == null) {
            str = str2;
        }
        return contentResolver.query(BrowserContract.Combined.CONTENT_URI, new String[]{"url"}, "url == ? OR url == ? OR url == ? OR url == ?", new String[]{str, str2, modifyUrl(str), modifyUrl(str2)}, null);
    }

    static void removeFromBookmarks(Context context, ContentResolver contentResolver, String str, String str2) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor = null;
        try {
            cursorQuery = contentResolver.query(BookmarkUtils.getBookmarksUri(context), new String[]{"_id"}, "url = ? AND title = ?", new String[]{str, str2}, null);
            try {
                if (!cursorQuery.moveToFirst()) {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                        return;
                    }
                    return;
                }
                WebIconDatabase.getInstance().releaseIconForPageUrl(str);
                contentResolver.delete(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, cursorQuery.getLong(0)), null, null);
                if (context != null) {
                    Toast.makeText(context, 2131492956, 1).show();
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (IllegalStateException e) {
                e = e;
                try {
                    Log.e("Bookmarks", "removeFromBookmarks", e);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    cursorQuery = cursor;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                if (cursorQuery != null) {
                }
                throw th;
            }
        } catch (IllegalStateException e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th3) {
            th = th3;
            cursorQuery = cursor;
            if (cursorQuery != null) {
            }
            throw th;
        }
    }

    static String removeQuery(String str) {
        if (str == null) {
            return null;
        }
        int iIndexOf = str.indexOf(63);
        return iIndexOf != -1 ? str.substring(0, iIndexOf) : str;
    }

    static void updateFavicon(ContentResolver contentResolver, String str, String str2, Bitmap bitmap) {
        new AsyncTask<Void, Void, Void>(bitmap, contentResolver, str, str2) {
            final ContentResolver val$cr;
            final Bitmap val$favicon;
            final String val$originalUrl;
            final String val$url;

            {
                this.val$favicon = bitmap;
                this.val$cr = contentResolver;
                this.val$originalUrl = str;
                this.val$url = str2;
            }

            private void updateImages(ContentResolver contentResolver2, String str3, ContentValues contentValues) {
                String strRemoveQuery = Bookmarks.removeQuery(str3);
                if (TextUtils.isEmpty(strRemoveQuery)) {
                    return;
                }
                contentValues.put("url_key", strRemoveQuery);
                contentResolver2.update(BrowserContract.Images.CONTENT_URI, contentValues, null, null);
            }

            @Override
            protected Void doInBackground(Void... voidArr) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                this.val$favicon.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                ContentValues contentValues = new ContentValues();
                contentValues.put("favicon", byteArrayOutputStream.toByteArray());
                updateImages(this.val$cr, this.val$originalUrl, contentValues);
                updateImages(this.val$cr, this.val$url, contentValues);
                return null;
            }
        }.execute(new Void[0]);
    }

    static boolean urlHasAcceptableScheme(String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < acceptableBookmarkSchemes.length; i++) {
            if (str.startsWith(acceptableBookmarkSchemes[i])) {
                return true;
            }
        }
        return false;
    }
}
