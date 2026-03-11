package com.android.browser;

import android.app.DownloadManager;
import android.content.Context;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import com.mediatek.browser.ext.IBrowserDownloadExt;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class FetchUrlMimeType extends Thread {
    private IBrowserDownloadExt mBrowserDownloadExt = null;
    private Context mContext;
    private String mCookies;
    private DownloadManager.Request mRequest;
    private String mUri;
    private String mUserAgent;

    public FetchUrlMimeType(Context context, DownloadManager.Request request, String uri, String cookies, String userAgent) {
        this.mContext = context.getApplicationContext();
        this.mRequest = request;
        this.mUri = uri;
        this.mCookies = cookies;
        this.mUserAgent = userAgent;
    }

    @Override
    public void run() {
        String newMimeType;
        int semicolonIndex;
        String mimeType = null;
        String contentDisposition = null;
        HttpURLConnection connection = null;
        try {
            try {
                URL url = new URL(this.mUri);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                if (this.mUserAgent != null) {
                    connection.addRequestProperty("User-Agent", this.mUserAgent);
                }
                if (this.mCookies != null && this.mCookies.length() > 0) {
                    connection.addRequestProperty("Cookie", this.mCookies);
                }
                if (connection.getResponseCode() == 501 || connection.getResponseCode() == 400) {
                    Log.d("Browser/FetchMimeType", "FetchUrlMimeType:  use Get method");
                    connection.disconnect();
                    URL url2 = new URL(this.mUri);
                    connection = (HttpURLConnection) url2.openConnection();
                    if (this.mUserAgent != null) {
                        connection.addRequestProperty("User-Agent", this.mUserAgent);
                    }
                }
                if (connection.getResponseCode() == 200) {
                    mimeType = connection.getContentType();
                    if (mimeType != null && (semicolonIndex = mimeType.indexOf(59)) != -1) {
                        mimeType = mimeType.substring(0, semicolonIndex);
                    }
                    contentDisposition = connection.getHeaderField("Content-Disposition");
                }
            } catch (IOException ioe) {
                Log.e("FetchUrlMimeType", "Download failed: " + ioe);
                if (connection != null) {
                    connection.disconnect();
                }
            }
            if (mimeType != null && ((mimeType.equalsIgnoreCase("text/plain") || mimeType.equalsIgnoreCase("application/octet-stream")) && (newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(this.mUri))) != null)) {
                mimeType = newMimeType;
                this.mRequest.setMimeType(newMimeType);
            }
            String filename = URLUtil.guessFileName(this.mUri, contentDisposition, mimeType);
            Log.d("Browser/FetchMimeType", "FetchUrlMimeType: Guess file name is: " + filename + " mimeType is: " + mimeType);
            this.mBrowserDownloadExt = Extensions.getDownloadPlugin(this.mContext);
            this.mBrowserDownloadExt.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(), this.mRequest, filename, mimeType);
            DownloadManager manager = (DownloadManager) this.mContext.getSystemService("download");
            manager.enqueue(this.mRequest);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
