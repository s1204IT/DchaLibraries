package com.android.browser.sitenavigation;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ParseException;
import android.net.Uri;
import android.net.WebAddress;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.browser.R;
import com.android.browser.UrlUtils;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/* loaded from: classes.dex */
public class SiteNavigationAddDialog extends Activity {
    private static final String[] ACCEPTABLE_WEBSITE_SCHEMES = {"http:", "https:", "about:", "data:", "javascript:", "file:", "content:", "rtsp:"};
    private EditText mAddress;
    private Button mButtonCancel;
    private Button mButtonOK;
    private TextView mDialogText;
    private Handler mHandler;
    private boolean mIsAdding;
    private String mItemName;
    private String mItemUrl;
    private Bundle mMap;
    private EditText mName;
    private View.OnClickListener mOKListener = new View.OnClickListener() { // from class: com.android.browser.sitenavigation.SiteNavigationAddDialog.1
        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (SiteNavigationAddDialog.this.save()) {
                SiteNavigationAddDialog.this.setResult(-1, new Intent().putExtra("need_refresh", true));
                SiteNavigationAddDialog.this.finish();
            }
        }
    };
    private View.OnClickListener mCancelListener = new View.OnClickListener() { // from class: com.android.browser.sitenavigation.SiteNavigationAddDialog.2
        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            SiteNavigationAddDialog.this.finish();
        }
    };

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        String string;
        super.onCreate(bundle);
        requestWindowFeature(1);
        setContentView(R.layout.site_navigation_add);
        this.mMap = getIntent().getExtras();
        Log.d("@M_browser/AddSiteNavigationPage", "onCreate mMap is : " + this.mMap);
        String string2 = null;
        if (this.mMap != null) {
            Bundle bundle2 = this.mMap.getBundle("websites");
            if (bundle2 != null) {
                this.mMap = bundle2;
            }
            string2 = this.mMap.getString("name");
            string = this.mMap.getString("url");
            this.mIsAdding = this.mMap.getBoolean("isAdding");
        } else {
            string = null;
        }
        this.mItemUrl = string;
        this.mItemName = string2;
        this.mName = (EditText) findViewById(R.id.title);
        this.mName.setText(string2);
        this.mAddress = (EditText) findViewById(R.id.address);
        if (string.startsWith("about:blank")) {
            this.mAddress.setText("about:blank");
        } else {
            this.mAddress.setText(string);
        }
        this.mDialogText = (TextView) findViewById(R.id.dialog_title);
        if (this.mIsAdding) {
            this.mDialogText.setText(R.string.add);
        }
        this.mButtonOK = (Button) findViewById(R.id.OK);
        this.mButtonOK.setOnClickListener(this.mOKListener);
        this.mButtonCancel = (Button) findViewById(R.id.cancel);
        this.mButtonCancel.setOnClickListener(this.mCancelListener);
        if (!getWindow().getDecorView().isInTouchMode()) {
            this.mButtonOK.requestFocus();
        }
    }

    private class SaveSiteNavigationRunnable implements Runnable {
        private Message mMessage;

        public SaveSiteNavigationRunnable(Message message) {
            this.mMessage = message;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [221=5] */
        /* JADX WARN: Removed duplicated region for block: B:15:0x00af A[Catch: all -> 0x00aa, IllegalStateException -> 0x00ac, TRY_LEAVE, TryCatch #4 {IllegalStateException -> 0x00ac, all -> 0x00aa, blocks: (B:5:0x0042, B:7:0x0048, B:9:0x0064, B:10:0x0086, B:15:0x00af), top: B:31:0x0042 }] */
        @Override // java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void run() throws Throwable {
            Cursor cursorQuery;
            Bundle data = this.mMessage.getData();
            String string = data.getString("title");
            String string2 = data.getString("url");
            String string3 = data.getString("itemUrl");
            Boolean boolValueOf = Boolean.valueOf(data.getBoolean("toDefaultThumbnail"));
            ContentResolver contentResolver = SiteNavigationAddDialog.this.getContentResolver();
            Cursor cursor = null;
            try {
                try {
                    cursorQuery = contentResolver.query(SiteNavigation.SITE_NAVIGATION_URI, new String[]{"_id"}, "url = ? COLLATE NOCASE", new String[]{string3}, null);
                    if (cursorQuery != null) {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("title", string);
                                contentValues.put("url", string2);
                                contentValues.put("website", "1");
                                if (boolValueOf.booleanValue()) {
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    BitmapFactory.decodeResource(SiteNavigationAddDialog.this.getResources(), R.raw.sitenavigation_thumbnail_default).compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                                    contentValues.put("thumbnail", byteArrayOutputStream.toByteArray());
                                }
                                Uri uriWithAppendedId = ContentUris.withAppendedId(SiteNavigation.SITE_NAVIGATION_URI, cursorQuery.getLong(0));
                                Log.d("@M_browser/AddSiteNavigationPage", "SaveSiteNavigationRunnable uri is : " + uriWithAppendedId);
                                contentResolver.update(uriWithAppendedId, contentValues, null, null);
                            } else {
                                Log.e("@M_browser/AddSiteNavigationPage", "saveSiteNavigationItem the item does not exist!");
                            }
                        } catch (IllegalStateException e) {
                            e = e;
                            cursor = cursorQuery;
                            Log.e("@M_browser/AddSiteNavigationPage", "saveSiteNavigationItem", e);
                            if (cursor != null) {
                                cursor.close();
                                return;
                            }
                            return;
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
                } catch (Throwable th2) {
                    th = th2;
                    cursorQuery = cursor;
                }
            } catch (IllegalStateException e2) {
                e = e2;
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:48:0x0105  */
    /* JADX WARN: Removed duplicated region for block: B:49:0x0106 A[Catch: UnsupportedEncodingException -> 0x0110, URISyntaxException -> 0x01c2, TryCatch #3 {UnsupportedEncodingException -> 0x0110, blocks: (B:46:0x00f8, B:49:0x0106, B:50:0x010f), top: B:96:0x00f8, outer: #1 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    boolean save() throws URISyntaxException {
        String string;
        String strTrim = this.mName.getText().toString().trim();
        String strFixUrl = UrlUtils.fixUrl(this.mAddress.getText().toString());
        boolean z = strTrim.length() == 0;
        boolean z2 = strFixUrl.trim().length() == 0;
        Resources resources = getResources();
        if (z || z2) {
            if (z) {
                this.mName.setError(resources.getText(R.string.website_needs_title));
            }
            if (z2) {
                this.mAddress.setError(resources.getText(R.string.website_needs_url));
            }
            return false;
        }
        if (!strTrim.equals(this.mItemName) && isSiteNavigationTitle(this, strTrim)) {
            this.mName.setError(resources.getText(R.string.duplicate_site_navigation_title));
            return false;
        }
        String strTrim2 = strFixUrl.trim();
        try {
            if (!strTrim2.toLowerCase().startsWith("javascript:")) {
                String scheme = new URI(strTrim2).getScheme();
                try {
                    if (!urlHasAcceptableScheme(strTrim2)) {
                        if (scheme != null) {
                            this.mAddress.setError(resources.getText(R.string.site_navigation_cannot_save_url));
                            return false;
                        }
                        try {
                            WebAddress webAddress = new WebAddress(strFixUrl);
                            if (webAddress.getHost().length() == 0) {
                                throw new URISyntaxException("", "");
                            }
                            string = webAddress.toString();
                        } catch (ParseException e) {
                            throw new URISyntaxException("", "");
                        }
                    } else {
                        int iIndexOf = -1;
                        if (strTrim2 != null) {
                            iIndexOf = strTrim2.indexOf("://");
                        }
                        if (iIndexOf > 0 && strTrim2.indexOf("/", iIndexOf + "://".length()) < 0) {
                            string = strTrim2 + "/";
                            Log.d("@M_browser/AddSiteNavigationPage", "URL=" + string);
                        }
                        if (strTrim2.length() == strTrim2.getBytes("UTF-8").length) {
                            throw new URISyntaxException("", "");
                        }
                    }
                    if (strTrim2.length() == strTrim2.getBytes("UTF-8").length) {
                    }
                } catch (UnsupportedEncodingException e2) {
                    throw new URISyntaxException("", "");
                }
                strTrim2 = string;
            }
            try {
                String path = new URL(strTrim2).getPath();
                if ((path.equals("/") && strTrim2.endsWith(".")) || (path.equals("") && strTrim2.endsWith(".."))) {
                    this.mAddress.setError(resources.getText(R.string.bookmark_url_not_valid));
                    return false;
                }
                if (!this.mItemUrl.equals(strTrim2) && isSiteNavigationUrl(this, strTrim2, strTrim2)) {
                    this.mAddress.setError(resources.getText(R.string.duplicate_site_navigation_url));
                    return false;
                }
                if (strTrim2.startsWith("about:blank")) {
                    strTrim2 = this.mItemUrl;
                }
                Bundle bundle = new Bundle();
                bundle.putString("title", strTrim);
                bundle.putString("url", strTrim2);
                bundle.putString("itemUrl", this.mItemUrl);
                if (!this.mItemUrl.equals(strTrim2)) {
                    bundle.putBoolean("toDefaultThumbnail", true);
                } else {
                    bundle.putBoolean("toDefaultThumbnail", false);
                }
                Message messageObtain = Message.obtain(this.mHandler, 100);
                messageObtain.setData(bundle);
                new Thread(new SaveSiteNavigationRunnable(messageObtain)).start();
                return true;
            } catch (MalformedURLException e3) {
                this.mAddress.setError(resources.getText(R.string.bookmark_url_not_valid));
                return false;
            }
        } catch (URISyntaxException e4) {
            this.mAddress.setError(resources.getText(R.string.bookmark_url_not_valid));
            return false;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [373=6, 374=4] */
    public static boolean isSiteNavigationUrl(Context context, String str, String str2) throws Throwable {
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = context.getContentResolver().query(SiteNavigation.SITE_NAVIGATION_URI, new String[]{"title"}, "url = ? COLLATE NOCASE OR url = ? COLLATE NOCASE", new String[]{str, str2}, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            Log.d("@M_browser/AddSiteNavigationPage", "isSiteNavigationUrl will return true.");
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return true;
                        }
                    } catch (IllegalStateException e) {
                        e = e;
                        cursor = cursorQuery;
                        Log.e("@M_browser/AddSiteNavigationPage", "isSiteNavigationUrl", e);
                        if (cursor != null) {
                            cursor.close();
                        }
                        return false;
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
            } catch (IllegalStateException e2) {
                e = e2;
            }
            return false;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [395=6, 396=4] */
    public static boolean isSiteNavigationTitle(Context context, String str) throws Throwable {
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = context.getContentResolver().query(SiteNavigation.SITE_NAVIGATION_URI, new String[]{"title"}, "title = ?", new String[]{str}, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            Log.d("@M_browser/AddSiteNavigationPage", "isSiteNavigationTitle will return true.");
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return true;
                        }
                    } catch (IllegalStateException e) {
                        e = e;
                        cursor = cursorQuery;
                        Log.e("@M_browser/AddSiteNavigationPage", "isSiteNavigationTitle", e);
                        if (cursor != null) {
                            cursor.close();
                        }
                        return false;
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
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IllegalStateException e2) {
            e = e2;
        }
        return false;
    }

    private static boolean urlHasAcceptableScheme(String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < ACCEPTABLE_WEBSITE_SCHEMES.length; i++) {
            if (str.startsWith(ACCEPTABLE_WEBSITE_SCHEMES[i])) {
                return true;
            }
        }
        return false;
    }
}
