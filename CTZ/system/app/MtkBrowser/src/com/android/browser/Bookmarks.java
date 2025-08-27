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

/* loaded from: classes.dex */
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
            Toast.makeText(context, R.string.added_to_bookmarks, 1).show();
        }
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

    private static void deleteSameTitle(Context context, String str, long j) {
        Log.d("browser/Bookmarks", "deleteSameTitle title:" + str);
        if (str == null || str.length() == 0) {
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("deleted", (Integer) 1);
        Log.d("browser/Bookmarks", "same title delete :" + context.getContentResolver().update(BrowserContract.Bookmarks.CONTENT_URI, contentValues, "title =? AND parent =? AND deleted =? AND folder =0 ", new String[]{str, String.valueOf(j), String.valueOf(0)}));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [184=5] */
    static void removeFromBookmarks(Context context, ContentResolver contentResolver, String str, String str2) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor = null;
        try {
            try {
                cursorQuery = contentResolver.query(BookmarkUtils.getBookmarksUri(context), new String[]{"_id"}, "url = ? AND title = ?", new String[]{str, str2}, null);
                try {
                } catch (IllegalStateException e) {
                    e = e;
                    cursor = cursorQuery;
                    Log.e("Bookmarks", "removeFromBookmarks", e);
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            } catch (IllegalStateException e2) {
                e = e2;
            }
            if (!cursorQuery.moveToFirst()) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } else {
                WebIconDatabase.getInstance().releaseIconForPageUrl(str);
                contentResolver.delete(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, cursorQuery.getLong(0)), null, null);
                if (context != null) {
                    Toast.makeText(context, R.string.removed_from_bookmarks, 1).show();
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        } catch (Throwable th2) {
            th = th2;
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

    private static String modifyUrl(String str) {
        if (str.endsWith("/")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
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

    static String removeQuery(String str) {
        if (str == null) {
            return null;
        }
        int iIndexOf = str.indexOf(63);
        if (iIndexOf != -1) {
            return str.substring(0, iIndexOf);
        }
        return str;
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.browser.Bookmarks$1] */
    static void updateFavicon(final ContentResolver contentResolver, final String str, final String str2, final Bitmap bitmap) {
        new AsyncTask<Void, Void, Void>() { // from class: com.android.browser.Bookmarks.1
            /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
            @Override // android.os.AsyncTask
            protected Void doInBackground(Void... voidArr) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                ContentValues contentValues = new ContentValues();
                contentValues.put("favicon", byteArrayOutputStream.toByteArray());
                updateImages(contentResolver, str, contentValues);
                updateImages(contentResolver, str2, contentValues);
                return null;
            }

            private void updateImages(ContentResolver contentResolver2, String str3, ContentValues contentValues) {
                String strRemoveQuery = Bookmarks.removeQuery(str3);
                if (!TextUtils.isEmpty(strRemoveQuery)) {
                    contentValues.put("url_key", strRemoveQuery);
                    contentResolver2.update(BrowserContract.Images.CONTENT_URI, contentValues, null, null);
                }
            }
        }.execute(new Void[0]);
    }

    static int getIdByNameOrUrl(ContentResolver contentResolver, String str, String str2, long j, long j2) {
        String strSubstring;
        String str3 = "parent = ? AND (title = ? OR url = ? OR url = ?) AND folder= 0";
        if (j2 > 0) {
            str3 = "parent = ? AND (title = ? OR url = ? OR url = ?) AND folder= 0 AND _id <> " + j2;
        }
        Log.v("browser/Bookmarks", "getIdByNameOrUrl() sql:" + str3);
        String[] strArr = {"_id"};
        Uri uri = BrowserContract.Bookmarks.CONTENT_URI;
        String[] strArr2 = new String[4];
        strArr2[0] = String.valueOf(j);
        strArr2[1] = str;
        strArr2[2] = str2;
        if (str2.endsWith("/")) {
            strSubstring = str2.substring(0, str2.lastIndexOf("/"));
        } else {
            strSubstring = str2 + "/";
        }
        strArr2[3] = strSubstring;
        Cursor cursorQuery = contentResolver.query(uri, strArr, str3, strArr2, "_id DESC");
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    return cursorQuery.getInt(0);
                }
                return -1;
            } finally {
                cursorQuery.close();
            }
        }
        return -1;
    }
}
