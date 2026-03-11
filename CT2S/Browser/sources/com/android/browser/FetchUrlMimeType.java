package com.android.browser;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Proxy;
import android.net.http.AndroidHttpClient;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import java.io.IOException;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.params.ConnRouteParams;

class FetchUrlMimeType extends Thread {
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
        AndroidHttpClient client = AndroidHttpClient.newInstance(this.mUserAgent);
        try {
            HttpHost httpHost = Proxy.getPreferredHttpHost(this.mContext, this.mUri);
            if (httpHost != null) {
                ConnRouteParams.setDefaultProxy(client.getParams(), httpHost);
            }
            HttpHead request = new HttpHead(this.mUri);
            if (this.mCookies != null && this.mCookies.length() > 0) {
                request.addHeader("Cookie", this.mCookies);
            }
            String mimeType = null;
            String contentDisposition = null;
            try {
                HttpResponse response = client.execute(request);
                if (response.getStatusLine().getStatusCode() == 200) {
                    Header header = response.getFirstHeader("Content-Type");
                    if (header != null && (semicolonIndex = (mimeType = header.getValue()).indexOf(59)) != -1) {
                        mimeType = mimeType.substring(0, semicolonIndex);
                    }
                    Header contentDispositionHeader = response.getFirstHeader("Content-Disposition");
                    if (contentDispositionHeader != null) {
                        contentDisposition = contentDispositionHeader.getValue();
                    }
                }
            } catch (IOException e) {
                if (request != null) {
                    request.abort();
                }
            } catch (IllegalArgumentException e2) {
                if (request != null) {
                    request.abort();
                }
            } finally {
                client.close();
            }
            if (mimeType != null) {
                if ((mimeType.equalsIgnoreCase("text/plain") || mimeType.equalsIgnoreCase("application/octet-stream")) && (newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(this.mUri))) != null) {
                    mimeType = newMimeType;
                    this.mRequest.setMimeType(newMimeType);
                }
                String filename = URLUtil.guessFileName(this.mUri, contentDisposition, mimeType);
                String value = BrowserSettings.getInstance().getDownloadDir();
                this.mRequest.setDestinationInExternalPublicDir(value, filename);
            }
            DownloadManager manager = (DownloadManager) this.mContext.getSystemService("download");
            manager.enqueue(this.mRequest);
        } catch (IllegalArgumentException ex) {
            Log.e("FetchUrlMimeType", "Download failed: " + ex);
        }
    }
}
