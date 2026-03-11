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
    private View.OnClickListener mOKListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!SiteNavigationAddDialog.this.save()) {
                return;
            }
            SiteNavigationAddDialog.this.setResult(-1, new Intent().putExtra("need_refresh", true));
            SiteNavigationAddDialog.this.finish();
        }
    };
    private View.OnClickListener mCancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SiteNavigationAddDialog.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(1);
        setContentView(R.layout.site_navigation_add);
        String name = null;
        String url = null;
        this.mMap = getIntent().getExtras();
        Log.d("@M_browser/AddSiteNavigationPage", "onCreate mMap is : " + this.mMap);
        if (this.mMap != null) {
            Bundle b = this.mMap.getBundle("websites");
            if (b != null) {
                this.mMap = b;
            }
            name = this.mMap.getString("name");
            url = this.mMap.getString("url");
            this.mIsAdding = this.mMap.getBoolean("isAdding");
        }
        this.mItemUrl = url;
        this.mItemName = name;
        this.mName = (EditText) findViewById(R.id.title);
        this.mName.setText(name);
        this.mAddress = (EditText) findViewById(R.id.address);
        if (url.startsWith("about:blank")) {
            this.mAddress.setText("about:blank");
        } else {
            this.mAddress.setText(url);
        }
        this.mDialogText = (TextView) findViewById(R.id.dialog_title);
        if (this.mIsAdding) {
            this.mDialogText.setText(R.string.add);
        }
        this.mButtonOK = (Button) findViewById(R.id.OK);
        this.mButtonOK.setOnClickListener(this.mOKListener);
        this.mButtonCancel = (Button) findViewById(R.id.cancel);
        this.mButtonCancel.setOnClickListener(this.mCancelListener);
        if (getWindow().getDecorView().isInTouchMode()) {
            return;
        }
        this.mButtonOK.requestFocus();
    }

    private class SaveSiteNavigationRunnable implements Runnable {
        private Message mMessage;

        public SaveSiteNavigationRunnable(Message msg) {
            this.mMessage = msg;
        }

        @Override
        public void run() {
            Bundle bundle = this.mMessage.getData();
            String title = bundle.getString("title");
            String url = bundle.getString("url");
            String itemUrl = bundle.getString("itemUrl");
            Boolean toDefaultThumbnail = Boolean.valueOf(bundle.getBoolean("toDefaultThumbnail"));
            ContentResolver cr = SiteNavigationAddDialog.this.getContentResolver();
            Cursor cursor = null;
            try {
                try {
                    Cursor cursor2 = cr.query(SiteNavigation.SITE_NAVIGATION_URI, new String[]{"_id"}, "url = ? COLLATE NOCASE", new String[]{itemUrl}, null);
                    if (cursor2 != null && cursor2.moveToFirst()) {
                        ContentValues values = new ContentValues();
                        values.put("title", title);
                        values.put("url", url);
                        values.put("website", "1");
                        if (toDefaultThumbnail.booleanValue()) {
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            Bitmap bm = BitmapFactory.decodeResource(SiteNavigationAddDialog.this.getResources(), R.raw.sitenavigation_thumbnail_default);
                            bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                            values.put("thumbnail", os.toByteArray());
                        }
                        Uri uri = ContentUris.withAppendedId(SiteNavigation.SITE_NAVIGATION_URI, cursor2.getLong(0));
                        Log.d("@M_browser/AddSiteNavigationPage", "SaveSiteNavigationRunnable uri is : " + uri);
                        cr.update(uri, values, null, null);
                    } else {
                        Log.e("@M_browser/AddSiteNavigationPage", "saveSiteNavigationItem the item does not exist!");
                    }
                    if (cursor2 == null) {
                        return;
                    }
                    cursor2.close();
                } catch (IllegalStateException e) {
                    Log.e("@M_browser/AddSiteNavigationPage", "saveSiteNavigationItem", e);
                    if (0 == 0) {
                        return;
                    }
                    cursor.close();
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    cursor.close();
                }
                throw th;
            }
        }
    }

    boolean save() {
        String name = this.mName.getText().toString().trim();
        String unfilteredUrl = UrlUtils.fixUrl(this.mAddress.getText().toString());
        boolean emptyTitle = name.length() == 0;
        boolean emptyUrl = unfilteredUrl.trim().length() == 0;
        Resources r = getResources();
        if (emptyTitle || emptyUrl) {
            if (emptyTitle) {
                this.mName.setError(r.getText(R.string.website_needs_title));
            }
            if (emptyUrl) {
                this.mAddress.setError(r.getText(R.string.website_needs_url));
                return false;
            }
            return false;
        }
        if (!name.equals(this.mItemName) && isSiteNavigationTitle(this, name)) {
            this.mName.setError(r.getText(R.string.duplicate_site_navigation_title));
            return false;
        }
        String url = unfilteredUrl.trim();
        try {
            if (!url.toLowerCase().startsWith("javascript:")) {
                URI uriObj = new URI(url);
                String scheme = uriObj.getScheme();
                if (!urlHasAcceptableScheme(url)) {
                    if (scheme != null) {
                        this.mAddress.setError(r.getText(R.string.site_navigation_cannot_save_url));
                        return false;
                    }
                    try {
                        WebAddress address = new WebAddress(unfilteredUrl);
                        if (address.getHost().length() == 0) {
                            throw new URISyntaxException("", "");
                        }
                        url = address.toString();
                    } catch (ParseException e) {
                        throw new URISyntaxException("", "");
                    }
                } else {
                    int iRet = -1;
                    if (url != null) {
                        iRet = url.indexOf("://");
                    }
                    if (iRet > 0 && url.indexOf("/", "://".length() + iRet) < 0) {
                        url = url + "/";
                        Log.d("@M_browser/AddSiteNavigationPage", "URL=" + url);
                    }
                }
                try {
                    byte[] bytes = url.getBytes("UTF-8");
                    if (url.length() != bytes.length) {
                        throw new URISyntaxException("", "");
                    }
                } catch (UnsupportedEncodingException e2) {
                    throw new URISyntaxException("", "");
                }
            }
            try {
                URL unModifyUrl = new URL(url);
                String path = unModifyUrl.getPath();
                if ((path.equals("/") && url.endsWith(".")) || (path.equals("") && url.endsWith(".."))) {
                    this.mAddress.setError(r.getText(R.string.bookmark_url_not_valid));
                    return false;
                }
                if (!this.mItemUrl.equals(url)) {
                    boolean exist = isSiteNavigationUrl(this, url, url);
                    if (exist) {
                        this.mAddress.setError(r.getText(R.string.duplicate_site_navigation_url));
                        return false;
                    }
                }
                if (url.startsWith("about:blank")) {
                    url = this.mItemUrl;
                }
                Bundle bundle = new Bundle();
                bundle.putString("title", name);
                bundle.putString("url", url);
                bundle.putString("itemUrl", this.mItemUrl);
                if (!this.mItemUrl.equals(url)) {
                    bundle.putBoolean("toDefaultThumbnail", true);
                } else {
                    bundle.putBoolean("toDefaultThumbnail", false);
                }
                Message msg = Message.obtain(this.mHandler, 100);
                msg.setData(bundle);
                Thread t = new Thread(new SaveSiteNavigationRunnable(msg));
                t.start();
                return true;
            } catch (MalformedURLException e3) {
                this.mAddress.setError(r.getText(R.string.bookmark_url_not_valid));
                return false;
            }
        } catch (URISyntaxException e4) {
            this.mAddress.setError(r.getText(R.string.bookmark_url_not_valid));
            return false;
        }
    }

    public static boolean isSiteNavigationUrl(Context context, String itemUrl, String originalUrl) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try {
            try {
                cursor = cr.query(SiteNavigation.SITE_NAVIGATION_URI, new String[]{"title"}, "url = ? COLLATE NOCASE OR url = ? COLLATE NOCASE", new String[]{itemUrl, originalUrl}, null);
            } catch (IllegalStateException e) {
                Log.e("@M_browser/AddSiteNavigationPage", "isSiteNavigationUrl", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (cursor != null && cursor.moveToFirst()) {
                Log.d("@M_browser/AddSiteNavigationPage", "isSiteNavigationUrl will return true.");
                return true;
            }
            if (cursor != null) {
                cursor.close();
            }
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static boolean isSiteNavigationTitle(Context context, String itemTitle) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try {
            try {
                cursor = cr.query(SiteNavigation.SITE_NAVIGATION_URI, new String[]{"title"}, "title = ?", new String[]{itemTitle}, null);
            } catch (IllegalStateException e) {
                Log.e("@M_browser/AddSiteNavigationPage", "isSiteNavigationTitle", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (cursor != null && cursor.moveToFirst()) {
                Log.d("@M_browser/AddSiteNavigationPage", "isSiteNavigationTitle will return true.");
                return true;
            }
            if (cursor != null) {
                cursor.close();
            }
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static boolean urlHasAcceptableScheme(String url) {
        if (url == null) {
            return false;
        }
        for (int i = 0; i < ACCEPTABLE_WEBSITE_SCHEMES.length; i++) {
            if (url.startsWith(ACCEPTABLE_WEBSITE_SCHEMES[i])) {
                return true;
            }
        }
        return false;
    }
}
