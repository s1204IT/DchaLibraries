package com.android.browser;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.webkit.WebView;
import com.android.browser.provider.BrowserContract;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
    public Void doInBackground(String... values) {
        if (this.mContentResolver != null) {
            this.mCursor = Bookmarks.queryCombinedForUrl(this.mContentResolver, this.mOriginalUrl, this.mUrl);
        }
        boolean inDatabase = this.mCursor != null && this.mCursor.getCount() > 0;
        if (inDatabase || this.mMessage != null) {
            HttpURLConnection httpURLConnection = null;
            try {
                try {
                    try {
                        URL url = new URL(values[0]);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        if (this.mUserAgent != null) {
                            connection.addRequestProperty("User-Agent", this.mUserAgent);
                        }
                        if (connection.getResponseCode() == 200) {
                            InputStream content = connection.getInputStream();
                            try {
                                ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
                                byte[] buff = new byte[1024];
                                int cnt = 0;
                                while (true) {
                                    int rc = content.read(buff, 0, 1024);
                                    if (rc <= 0) {
                                        break;
                                    }
                                    swapStream.write(buff, 0, rc);
                                    cnt += rc;
                                }
                                byte[] data = swapStream.toByteArray();
                                BitmapFactory.Options opts = new BitmapFactory.Options();
                                opts.inJustDecodeBounds = true;
                                BitmapFactory.decodeByteArray(data, 0, cnt, opts);
                                int width = opts.outWidth;
                                int height = opts.outHeight;
                                int limitWidth = this.mContext.getResources().getInteger(R.integer.image_width);
                                int limitHeight = this.mContext.getResources().getInteger(R.integer.image_height);
                                int scale = 1;
                                while (true) {
                                    if (width / scale <= limitWidth && height / scale <= limitHeight) {
                                        break;
                                    }
                                    scale *= 2;
                                }
                                opts.inJustDecodeBounds = false;
                                opts.inSampleSize = scale;
                                Bitmap icon = BitmapFactory.decodeByteArray(data, 0, cnt, opts);
                                if (inDatabase) {
                                    storeIcon(icon);
                                } else if (this.mMessage != null) {
                                    Bundle b = this.mMessage.getData();
                                    b.putParcelable("touch_icon", icon);
                                }
                            } finally {
                                try {
                                    content.close();
                                } catch (IOException e) {
                                }
                            }
                        }
                        if (connection != null) {
                            connection.disconnect();
                        }
                    } catch (Throwable th) {
                        if (0 != 0) {
                            httpURLConnection.disconnect();
                        }
                        throw th;
                    }
                } catch (IOException e2) {
                    if (0 != 0) {
                        httpURLConnection.disconnect();
                    }
                }
            } catch (ClassCastException e3) {
                Log.e("browser/DownloadTouchIcon", "Icon url cannot cast to HttpURLConnection:" + e3);
                if (0 != 0) {
                    httpURLConnection.disconnect();
                }
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
        if (this.mCursor == null) {
            return;
        }
        this.mCursor.close();
    }

    private void storeIcon(Bitmap icon) {
        if (this.mTab != null) {
            this.mTab.mTouchIconLoader = null;
        }
        if (icon == null || this.mCursor == null || isCancelled() || !this.mCursor.moveToFirst()) {
            return;
        }
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
