package com.android.browser.sitenavigation;

import android.content.ContentProvider;
import android.content.ContentResolver;
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
import com.android.browser.R;
import com.android.browser.provider.BrowserProvider2;
import com.mediatek.browser.ext.IBrowserSiteNavigationExt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SiteNavigationProvider extends ContentProvider {
    private static final Uri NOTIFICATION_URI;
    private static final UriMatcher S_URI_MATCHER = new UriMatcher(-1);
    private SiteNavigationDatabaseHelper mOpenHelper;

    static {
        S_URI_MATCHER.addURI("com.android.browser.site_navigation", "websites", 0);
        S_URI_MATCHER.addURI("com.android.browser.site_navigation", "websites/#", 1);
        NOTIFICATION_URI = SiteNavigation.SITE_NAVIGATION_URI;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public boolean onCreate() {
        this.mOpenHelper = new SiteNavigationDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String str;
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("websites");
        switch (S_URI_MATCHER.match(uri)) {
            case 0:
                break;
            case 1:
                qb.appendWhere("_id=" + uri.getPathSegments().get(0));
                break;
            default:
                Log.e("@M_browser/SiteNavigationProvider", "SiteNavigationProvider query Unknown URI: " + uri);
                return null;
        }
        if (TextUtils.isEmpty(sortOrder)) {
            str = null;
        } else {
            str = sortOrder;
        }
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, str);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
        }
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (S_URI_MATCHER.match(uri)) {
            case 0:
                count = db.update("websites", values, selection, selectionArgs);
                break;
            case 1:
                String newIdSelection = "_id=" + uri.getLastPathSegment() + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
                try {
                    count = db.update("websites", values, newIdSelection, selectionArgs);
                } catch (SQLiteDiskIOException e) {
                    Log.e("browser/SiteNavigationProvider", "Here happened SQLiteDiskIOException");
                } catch (SQLiteFullException e2) {
                    Log.e("browser/SiteNavigationProvider", "Here happened SQLiteFullException");
                }
                break;
            default:
                Log.e("@M_browser/SiteNavigationProvider", "SiteNavigationProvider update Unknown URI: " + uri);
                return 0;
        }
        if (count > 0) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        try {
            ParcelFileDescriptor[] pipes = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor write = pipes[1];
            AssetFileDescriptor afd = new AssetFileDescriptor(write, 0L, -1L);
            new RequestHandlerSiteNavigation(getContext(), uri, afd.createOutputStream()).start();
            return pipes[0];
        } catch (IOException e) {
            Log.e("browser/SiteNavigationProvider", "Failed to handle request: " + uri, e);
            return null;
        }
    }

    private class SiteNavigationDatabaseHelper extends SQLiteOpenHelper {
        private IBrowserSiteNavigationExt mBrowserSiteNavigationExt;
        private Context mContext;

        public SiteNavigationDatabaseHelper(Context context) {
            super(context, "websites.db", (SQLiteDatabase.CursorFactory) null, 1);
            this.mBrowserSiteNavigationExt = null;
            this.mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTable(db);
            initTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        }

        private void createTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE websites (_id INTEGER PRIMARY KEY AUTOINCREMENT,url TEXT,title TEXT,created LONG,website INTEGER,thumbnail BLOB DEFAULT NULL,favicon BLOB DEFAULT NULL,default_thumb TEXT);");
        }

        private void initTable(SQLiteDatabase db) {
            int siteNavigationWebsiteNum;
            ByteArrayOutputStream os;
            ContentValues values;
            this.mBrowserSiteNavigationExt = Extensions.getSiteNavigationPlugin(this.mContext);
            CharSequence[] websites = this.mBrowserSiteNavigationExt.getPredefinedWebsites();
            if (websites == null) {
                websites = this.mContext.getResources().getTextArray(R.array.predefined_websites_default_optr);
            }
            if (websites == null) {
                return;
            }
            int websiteSize = websites.length;
            if (this.mContext.getResources().getBoolean(R.bool.isTablet)) {
                siteNavigationWebsiteNum = 8;
            } else {
                siteNavigationWebsiteNum = this.mBrowserSiteNavigationExt.getSiteNavigationCount();
                if (siteNavigationWebsiteNum == 0) {
                    siteNavigationWebsiteNum = 9;
                }
            }
            if (websiteSize > siteNavigationWebsiteNum * 3) {
                websiteSize = siteNavigationWebsiteNum * 3;
            }
            Bitmap bm = null;
            int i = 0;
            ContentValues values2 = null;
            ByteArrayOutputStream os2 = null;
            while (i < websiteSize) {
                try {
                    os = new ByteArrayOutputStream();
                    try {
                        String fileName = websites[i + 2].toString();
                        if (fileName != null) {
                            try {
                                if (fileName.length() != 0) {
                                    int id = this.mContext.getResources().getIdentifier(fileName, "raw", this.mContext.getPackageName());
                                    bm = BitmapFactory.decodeResource(this.mContext.getResources(), id);
                                } else {
                                    bm = BitmapFactory.decodeResource(this.mContext.getResources(), R.raw.sitenavigation_thumbnail_default);
                                }
                            } finally {
                                if (bm == null) {
                                    BitmapFactory.decodeResource(this.mContext.getResources(), R.raw.sitenavigation_thumbnail_default);
                                }
                            }
                        }
                        bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                        values = new ContentValues();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        e = e;
                        Log.e("@M_browser/SiteNavigationProvider", "initTable: ArrayIndexOutOfBoundsException: " + e);
                        return;
                    }
                } catch (ArrayIndexOutOfBoundsException e2) {
                    e = e2;
                }
                try {
                    CharSequence websiteDestination = BrowserProvider2.replaceSystemPropertyInString(this.mContext, websites[i + 1]);
                    values.put("url", websiteDestination.toString());
                    values.put("title", websites[i].toString());
                    values.put("created", "0");
                    values.put("website", "1");
                    values.put("thumbnail", os.toByteArray());
                    db.insertOrThrow("websites", "url", values);
                    i += 3;
                    values2 = values;
                    os2 = os;
                } catch (ArrayIndexOutOfBoundsException e3) {
                    e = e3;
                    Log.e("@M_browser/SiteNavigationProvider", "initTable: ArrayIndexOutOfBoundsException: " + e);
                    return;
                }
            }
            int i2 = websiteSize / 3;
            while (i2 < siteNavigationWebsiteNum) {
                ByteArrayOutputStream os3 = new ByteArrayOutputStream();
                Bitmap bm2 = BitmapFactory.decodeResource(this.mContext.getResources(), R.raw.sitenavigation_thumbnail_default);
                bm2.compress(Bitmap.CompressFormat.PNG, 100, os3);
                ContentValues values3 = new ContentValues();
                values3.put("url", "about:blank" + (i2 + 1));
                values3.put("title", "about:blank");
                values3.put("created", "0");
                values3.put("website", "1");
                values3.put("thumbnail", os3.toByteArray());
                db.insertOrThrow("websites", "url", values3);
                i2++;
                values2 = values3;
                os2 = os3;
            }
        }
    }
}
