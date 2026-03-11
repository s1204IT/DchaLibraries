package com.android.browser.sitenavigation;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import com.android.browser.Extensions;
import com.android.browser.provider.BrowserProvider2;
import com.mediatek.browser.ext.IBrowserSiteNavigationExt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SiteNavigationProvider extends ContentProvider {
    private static final Uri NOTIFICATION_URI;
    private static final UriMatcher S_URI_MATCHER = new UriMatcher(-1);
    private SiteNavigationDatabaseHelper mOpenHelper;

    private class SiteNavigationDatabaseHelper extends SQLiteOpenHelper {
        private IBrowserSiteNavigationExt mBrowserSiteNavigationExt;
        private Context mContext;
        final SiteNavigationProvider this$0;

        public SiteNavigationDatabaseHelper(SiteNavigationProvider siteNavigationProvider, Context context) {
            super(context, "websites.db", (SQLiteDatabase.CursorFactory) null, 1);
            this.this$0 = siteNavigationProvider;
            this.mBrowserSiteNavigationExt = null;
            this.mContext = context;
        }

        private void createTable(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE websites (_id INTEGER PRIMARY KEY AUTOINCREMENT,url TEXT,title TEXT,created LONG,website INTEGER,thumbnail BLOB DEFAULT NULL,favicon BLOB DEFAULT NULL,default_thumb TEXT);");
        }

        private void initTable(SQLiteDatabase sQLiteDatabase) {
            int i;
            this.mBrowserSiteNavigationExt = Extensions.getSiteNavigationPlugin(this.mContext);
            CharSequence[] predefinedWebsites = this.mBrowserSiteNavigationExt.getPredefinedWebsites();
            CharSequence[] textArray = predefinedWebsites == null ? this.mContext.getResources().getTextArray(2131230815) : predefinedWebsites;
            if (textArray == null) {
                return;
            }
            int length = textArray.length;
            if (this.mContext.getResources().getBoolean(2131296256)) {
                i = 8;
            } else {
                int siteNavigationCount = this.mBrowserSiteNavigationExt.getSiteNavigationCount();
                i = siteNavigationCount == 0 ? 9 : siteNavigationCount;
            }
            int i2 = i * 3;
            if (length <= i2) {
                i2 = length;
            }
            Bitmap bitmapDecodeResource = null;
            for (int i3 = 0; i3 < i2; i3 += 3) {
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    String string = textArray[i3 + 2].toString();
                    if (string != null) {
                        try {
                            bitmapDecodeResource = string.length() != 0 ? BitmapFactory.decodeResource(this.mContext.getResources(), this.mContext.getResources().getIdentifier(string, "raw", this.mContext.getPackageName())) : BitmapFactory.decodeResource(this.mContext.getResources(), 2131165227);
                        } finally {
                            if (bitmapDecodeResource == null) {
                                BitmapFactory.decodeResource(this.mContext.getResources(), 2131165227);
                            }
                        }
                    }
                    bitmapDecodeResource.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("url", BrowserProvider2.replaceSystemPropertyInString(this.mContext, textArray[i3 + 1]).toString());
                    contentValues.put("title", textArray[i3].toString());
                    contentValues.put("created", "0");
                    contentValues.put("website", "1");
                    contentValues.put("thumbnail", byteArrayOutputStream.toByteArray());
                    sQLiteDatabase.insertOrThrow("websites", "url", contentValues);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e("@M_browser/SiteNavigationProvider", "initTable: ArrayIndexOutOfBoundsException: " + e);
                    return;
                }
            }
            int i4 = i2 / 3;
            while (i4 < i) {
                ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
                BitmapFactory.decodeResource(this.mContext.getResources(), 2131165227).compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream2);
                ContentValues contentValues2 = new ContentValues();
                StringBuilder sb = new StringBuilder();
                sb.append("about:blank");
                i4++;
                sb.append(i4);
                contentValues2.put("url", sb.toString());
                contentValues2.put("title", "about:blank");
                contentValues2.put("created", "0");
                contentValues2.put("website", "1");
                contentValues2.put("thumbnail", byteArrayOutputStream2.toByteArray());
                sQLiteDatabase.insertOrThrow("websites", "url", contentValues2);
            }
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            createTable(sQLiteDatabase);
            initTable(sQLiteDatabase);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        }
    }

    static {
        S_URI_MATCHER.addURI("com.android.browser.site_navigation", "websites", 0);
        S_URI_MATCHER.addURI("com.android.browser.site_navigation", "websites/#", 1);
        NOTIFICATION_URI = SiteNavigation.SITE_NAVIGATION_URI;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public boolean onCreate() {
        this.mOpenHelper = new SiteNavigationDatabaseHelper(this, getContext());
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) {
        try {
            ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
            new RequestHandlerSiteNavigation(getContext(), uri, new AssetFileDescriptor(parcelFileDescriptorArrCreatePipe[1], 0L, -1L).createOutputStream()).start();
            return parcelFileDescriptorArrCreatePipe[0];
        } catch (IOException e) {
            Log.e("browser/SiteNavigationProvider", "Failed to handle request: " + uri, e);
            return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        Cursor cursorQuery = null;
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables("websites");
        switch (S_URI_MATCHER.match(uri)) {
            case 1:
                sQLiteQueryBuilder.appendWhere("_id=" + uri.getPathSegments().get(0));
            case 0:
                cursorQuery = sQLiteQueryBuilder.query(this.mOpenHelper.getReadableDatabase(), strArr, str, strArr2, null, null, TextUtils.isEmpty(str2) ? null : str2);
                if (cursorQuery != null) {
                    cursorQuery.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
                }
                return cursorQuery;
            default:
                Log.e("@M_browser/SiteNavigationProvider", "SiteNavigationProvider query Unknown URI: " + uri);
                return cursorQuery;
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int iUpdate;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        switch (S_URI_MATCHER.match(uri)) {
            case 0:
                iUpdate = writableDatabase.update("websites", contentValues, str, strArr);
                break;
            case 1:
                StringBuilder sb = new StringBuilder();
                sb.append("_id=");
                sb.append(uri.getLastPathSegment());
                sb.append(TextUtils.isEmpty(str) ? "" : " AND (" + str + ')');
                try {
                    iUpdate = writableDatabase.update("websites", contentValues, sb.toString(), strArr);
                } catch (SQLiteDiskIOException e) {
                    Log.e("browser/SiteNavigationProvider", "Here happened SQLiteDiskIOException");
                    iUpdate = 0;
                } catch (SQLiteFullException e2) {
                    Log.e("browser/SiteNavigationProvider", "Here happened SQLiteFullException");
                    iUpdate = 0;
                }
                break;
            default:
                Log.e("@M_browser/SiteNavigationProvider", "SiteNavigationProvider update Unknown URI: " + uri);
                return 0;
        }
        if (iUpdate <= 0) {
            return iUpdate;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return iUpdate;
    }
}
