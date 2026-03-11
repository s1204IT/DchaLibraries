package com.android.browser;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Proxy;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.provider.BrowserContract;
import android.webkit.WebView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRouteParams;

class DownloadTouchIcon extends AsyncTask<String, Void, Void> {
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private Cursor mCursor;
    private Message mMessage;
    private final String mOriginalUrl;
    Tab mTab;
    private final String mUrl;
    private final String mUserAgent;

    public DownloadTouchIcon(Tab tab, Context ctx, ContentResolver cr, WebView view) {
        this.mTab = tab;
        this.mContext = ctx.getApplicationContext();
        this.mContentResolver = cr;
        this.mOriginalUrl = view.getOriginalUrl();
        this.mUrl = view.getUrl();
        this.mUserAgent = view.getSettings().getUserAgentString();
    }

    public DownloadTouchIcon(Context ctx, ContentResolver cr, String url) {
        this.mTab = null;
        this.mContext = ctx.getApplicationContext();
        this.mContentResolver = cr;
        this.mOriginalUrl = null;
        this.mUrl = url;
        this.mUserAgent = null;
    }

    public DownloadTouchIcon(Context context, Message msg, String userAgent) {
        this.mMessage = msg;
        this.mContext = context.getApplicationContext();
        this.mContentResolver = null;
        this.mOriginalUrl = null;
        this.mUrl = null;
        this.mUserAgent = userAgent;
    }

    @Override
    public Void doInBackground(String... values) throws Throwable {
        HttpGet request;
        HttpEntity entity;
        InputStream content;
        if (this.mContentResolver != null) {
            this.mCursor = Bookmarks.queryCombinedForUrl(this.mContentResolver, this.mOriginalUrl, this.mUrl);
        }
        boolean inDatabase = this.mCursor != null && this.mCursor.getCount() > 0;
        String url = values[0];
        if (inDatabase || this.mMessage != null) {
            AndroidHttpClient client = null;
            HttpGet request2 = null;
            try {
                try {
                    client = AndroidHttpClient.newInstance(this.mUserAgent);
                    HttpHost httpHost = Proxy.getPreferredHttpHost(this.mContext, url);
                    if (httpHost != null) {
                        ConnRouteParams.setDefaultProxy(client.getParams(), httpHost);
                    }
                    request = new HttpGet(url);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Exception e) {
            }
            try {
                HttpClientParams.setRedirecting(client.getParams(), true);
                HttpResponse response = client.execute(request);
                if (response.getStatusLine().getStatusCode() == 200 && (entity = response.getEntity()) != null && (content = entity.getContent()) != null) {
                    Bitmap icon = BitmapFactory.decodeStream(content, null, null);
                    if (inDatabase) {
                        storeIcon(icon);
                    } else if (this.mMessage != null) {
                        Bundle b = this.mMessage.getData();
                        b.putParcelable("touch_icon", icon);
                    }
                }
                if (client != null) {
                    client.close();
                }
            } catch (Exception e2) {
                request2 = request;
                if (request2 != null) {
                    request2.abort();
                }
                if (client != null) {
                    client.close();
                }
            } catch (Throwable th2) {
                th = th2;
                if (client != null) {
                    client.close();
                }
                throw th;
            }
        }
        if (this.mCursor != null) {
            this.mCursor.close();
        }
        if (this.mMessage == null) {
            return null;
        }
        this.mMessage.sendToTarget();
        return null;
    }

    @Override
    protected void onCancelled() {
        if (this.mCursor != null) {
            this.mCursor.close();
        }
    }

    private void storeIcon(Bitmap icon) {
        if (this.mTab != null) {
            this.mTab.mTouchIconLoader = null;
        }
        if (icon != null && this.mCursor != null && !isCancelled() && this.mCursor.moveToFirst()) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 100, os);
            ContentValues values = new ContentValues();
            values.put("touch_icon", os.toByteArray());
            do {
                values.put("url_key", this.mCursor.getString(0));
                this.mContentResolver.update(BrowserContract.Images.CONTENT_URI, values, null, null);
            } while (this.mCursor.moveToNext());
        }
    }
}
