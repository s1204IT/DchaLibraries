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
    private View.OnClickListener mOKListener = new View.OnClickListener(this) {
        final SiteNavigationAddDialog this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void onClick(View view) {
            if (this.this$0.save()) {
                this.this$0.setResult(-1, new Intent().putExtra("need_refresh", true));
                this.this$0.finish();
            }
        }
    };
    private View.OnClickListener mCancelListener = new View.OnClickListener(this) {
        final SiteNavigationAddDialog this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void onClick(View view) {
            this.this$0.finish();
        }
    };

    private class SaveSiteNavigationRunnable implements Runnable {
        private Message mMessage;
        final SiteNavigationAddDialog this$0;

        public SaveSiteNavigationRunnable(SiteNavigationAddDialog siteNavigationAddDialog, Message message) {
            this.this$0 = siteNavigationAddDialog;
            this.mMessage = message;
        }

        @Override
        public void run() throws Throwable {
            Cursor cursorQuery;
            Cursor cursor = null;
            Bundle data = this.mMessage.getData();
            String string = data.getString("title");
            String string2 = data.getString("url");
            String string3 = data.getString("itemUrl");
            boolean z = data.getBoolean("toDefaultThumbnail");
            ContentResolver contentResolver = this.this$0.getContentResolver();
            try {
                cursorQuery = contentResolver.query(SiteNavigation.SITE_NAVIGATION_URI, new String[]{"_id"}, "url = ? COLLATE NOCASE", new String[]{string3}, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("title", string);
                            contentValues.put("url", string2);
                            contentValues.put("website", "1");
                            if (Boolean.valueOf(z).booleanValue()) {
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                BitmapFactory.decodeResource(this.this$0.getResources(), 2131165227).compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
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
                        try {
                            Log.e("@M_browser/AddSiteNavigationPage", "saveSiteNavigationItem", e);
                            if (cursorQuery != null) {
                                cursorQuery.close();
                                return;
                            }
                            return;
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
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
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
    }

    public static boolean isSiteNavigationTitle(Context context, String str) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor = null;
        try {
            cursorQuery = context.getContentResolver().query(SiteNavigation.SITE_NAVIGATION_URI, new String[]{"title"}, "title = ?", new String[]{str}, null);
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
                    try {
                        Log.e("@M_browser/AddSiteNavigationPage", "isSiteNavigationTitle", e);
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
            }
            if (cursorQuery != null) {
                cursorQuery.close();
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
        return false;
    }

    public static boolean isSiteNavigationUrl(Context context, String str, String str2) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor = null;
        try {
            cursorQuery = context.getContentResolver().query(SiteNavigation.SITE_NAVIGATION_URI, new String[]{"title"}, "url = ? COLLATE NOCASE OR url = ? COLLATE NOCASE", new String[]{str, str2}, null);
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
                    try {
                        Log.e("@M_browser/AddSiteNavigationPage", "isSiteNavigationUrl", e);
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
            }
            if (cursorQuery != null) {
                cursorQuery.close();
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

    @Override
    protected void onCreate(Bundle bundle) {
        String str;
        String string;
        super.onCreate(bundle);
        requestWindowFeature(1);
        setContentView(2130968621);
        this.mMap = getIntent().getExtras();
        Log.d("@M_browser/AddSiteNavigationPage", "onCreate mMap is : " + this.mMap);
        if (this.mMap != null) {
            Bundle bundle2 = this.mMap.getBundle("websites");
            if (bundle2 != null) {
                this.mMap = bundle2;
            }
            string = this.mMap.getString("name");
            String string2 = this.mMap.getString("url");
            this.mIsAdding = this.mMap.getBoolean("isAdding");
            str = string2;
        } else {
            str = null;
            string = null;
        }
        this.mItemUrl = str;
        this.mItemName = string;
        this.mName = (EditText) findViewById(2131558407);
        this.mName.setText(string);
        this.mAddress = (EditText) findViewById(2131558456);
        if (str.startsWith("about:blank")) {
            this.mAddress.setText("about:blank");
        } else {
            this.mAddress.setText(str);
        }
        this.mDialogText = (TextView) findViewById(2131558512);
        if (this.mIsAdding) {
            this.mDialogText.setText(2131492899);
        }
        this.mButtonOK = (Button) findViewById(2131558463);
        this.mButtonOK.setOnClickListener(this.mOKListener);
        this.mButtonCancel = (Button) findViewById(2131558462);
        this.mButtonCancel.setOnClickListener(this.mCancelListener);
        if (getWindow().getDecorView().isInTouchMode()) {
            return;
        }
        this.mButtonOK.requestFocus();
    }

    boolean save() {
        String strTrim = this.mName.getText().toString().trim();
        String strFixUrl = UrlUtils.fixUrl(this.mAddress.getText().toString());
        boolean z = strTrim.length() == 0;
        boolean z2 = strFixUrl.trim().length() == 0;
        Resources resources = getResources();
        if (z || z2) {
            if (z) {
                this.mName.setError(resources.getText(2131492903));
            }
            if (!z2) {
                return false;
            }
            this.mAddress.setError(resources.getText(2131492904));
            return false;
        }
        if (!strTrim.equals(this.mItemName) && isSiteNavigationTitle(this, strTrim)) {
            this.mName.setError(resources.getText(2131492911));
            return false;
        }
        String strTrim2 = strFixUrl.trim();
        try {
            if (!strTrim2.toLowerCase().startsWith("javascript:")) {
                String scheme = new URI(strTrim2).getScheme();
                if (urlHasAcceptableScheme(strTrim2)) {
                    int iIndexOf = strTrim2 != null ? strTrim2.indexOf("://") : -1;
                    if (iIndexOf > 0 && strTrim2.indexOf("/", iIndexOf + "://".length()) < 0) {
                        strTrim2 = strTrim2 + "/";
                        Log.d("@M_browser/AddSiteNavigationPage", "URL=" + strTrim2);
                    }
                } else {
                    if (scheme != null) {
                        this.mAddress.setError(resources.getText(2131492905));
                        return false;
                    }
                    try {
                        WebAddress webAddress = new WebAddress(strFixUrl);
                        if (webAddress.getHost().length() == 0) {
                            throw new URISyntaxException("", "");
                        }
                        strTrim2 = webAddress.toString();
                    } catch (ParseException e) {
                        throw new URISyntaxException("", "");
                    }
                }
                try {
                    if (strTrim2.length() != strTrim2.getBytes("UTF-8").length) {
                        throw new URISyntaxException("", "");
                    }
                } catch (UnsupportedEncodingException e2) {
                    throw new URISyntaxException("", "");
                }
            }
            try {
                String path = new URL(strTrim2).getPath();
                if ((path.equals("/") && strTrim2.endsWith(".")) || (path.equals("") && strTrim2.endsWith(".."))) {
                    this.mAddress.setError(resources.getText(2131493015));
                    return false;
                }
                if (!this.mItemUrl.equals(strTrim2) && isSiteNavigationUrl(this, strTrim2, strTrim2)) {
                    this.mAddress.setError(resources.getText(2131492910));
                    return false;
                }
                if (strTrim2.startsWith("about:blank")) {
                    strTrim2 = this.mItemUrl;
                }
                Bundle bundle = new Bundle();
                bundle.putString("title", strTrim);
                bundle.putString("url", strTrim2);
                bundle.putString("itemUrl", this.mItemUrl);
                if (this.mItemUrl.equals(strTrim2)) {
                    bundle.putBoolean("toDefaultThumbnail", false);
                } else {
                    bundle.putBoolean("toDefaultThumbnail", true);
                }
                Message messageObtain = Message.obtain(this.mHandler, 100);
                messageObtain.setData(bundle);
                new Thread(new SaveSiteNavigationRunnable(this, messageObtain)).start();
                return true;
            } catch (MalformedURLException e3) {
                this.mAddress.setError(resources.getText(2131493015));
                return false;
            }
        } catch (URISyntaxException e4) {
            this.mAddress.setError(resources.getText(2131493015));
            return false;
        }
    }
}
