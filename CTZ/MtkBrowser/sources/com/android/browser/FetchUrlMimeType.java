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

    public FetchUrlMimeType(Context context, DownloadManager.Request request, String str, String str2, String str3) {
        this.mContext = context.getApplicationContext();
        this.mRequest = request;
        this.mUri = str;
        this.mCookies = str2;
        this.mUserAgent = str3;
    }

    @Override
    public void run() throws Throwable {
        Throwable th;
        String contentType;
        IOException e;
        HttpURLConnection httpURLConnection;
        String str;
        String mimeTypeFromExtension;
        HttpURLConnection httpURLConnection2;
        String headerField;
        HttpURLConnection httpURLConnection3 = null;
        String str2 = null;
        try {
            httpURLConnection2 = (HttpURLConnection) new URL(this.mUri).openConnection();
            try {
                try {
                    httpURLConnection2.setRequestMethod("HEAD");
                    if (this.mUserAgent != null) {
                        httpURLConnection2.addRequestProperty("User-Agent", this.mUserAgent);
                    }
                    if (this.mCookies != null && this.mCookies.length() > 0) {
                        httpURLConnection2.addRequestProperty("Cookie", this.mCookies);
                    }
                } catch (IOException e2) {
                    e = e2;
                    contentType = null;
                    httpURLConnection = httpURLConnection2;
                }
            } catch (Throwable th2) {
                th = th2;
                httpURLConnection = httpURLConnection2;
                httpURLConnection3 = httpURLConnection;
            }
        } catch (IOException e3) {
            contentType = null;
            e = e3;
            httpURLConnection = null;
        } catch (Throwable th3) {
            th = th3;
        }
        if (httpURLConnection2.getResponseCode() != 501 && httpURLConnection2.getResponseCode() != 400) {
            if (httpURLConnection2.getResponseCode() != 200) {
            }
            if (httpURLConnection2 == null) {
            }
            if (str != null) {
                this.mRequest.setMimeType(mimeTypeFromExtension);
                str = mimeTypeFromExtension;
            }
            String strGuessFileName = URLUtil.guessFileName(this.mUri, str2, str);
            Log.d("Browser/FetchMimeType", "FetchUrlMimeType: Guess file name is: " + strGuessFileName + " mimeType is: " + str);
            this.mBrowserDownloadExt = Extensions.getDownloadPlugin(this.mContext);
            this.mBrowserDownloadExt.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(), this.mRequest, strGuessFileName, str);
            ((DownloadManager) this.mContext.getSystemService("download")).enqueue(this.mRequest);
            return;
        }
        Log.d("Browser/FetchMimeType", "FetchUrlMimeType:  use Get method");
        httpURLConnection2.disconnect();
        httpURLConnection2 = (HttpURLConnection) new URL(this.mUri).openConnection();
        try {
            if (this.mUserAgent != null) {
                httpURLConnection2.addRequestProperty("User-Agent", this.mUserAgent);
            }
            if (httpURLConnection2.getResponseCode() != 200) {
                contentType = httpURLConnection2.getContentType();
                if (contentType != null) {
                    try {
                        int iIndexOf = contentType.indexOf(59);
                        if (iIndexOf != -1) {
                            contentType = contentType.substring(0, iIndexOf);
                        }
                    } catch (IOException e4) {
                        e = e4;
                        httpURLConnection = httpURLConnection2;
                        try {
                            Log.e("FetchUrlMimeType", "Download failed: " + e);
                            if (httpURLConnection != null) {
                                httpURLConnection.disconnect();
                            }
                            str = contentType;
                        } catch (Throwable th4) {
                            th = th4;
                            httpURLConnection3 = httpURLConnection;
                            if (httpURLConnection3 != null) {
                            }
                            throw th;
                        }
                    }
                }
                headerField = httpURLConnection2.getHeaderField("Content-Disposition");
            } else {
                contentType = null;
                headerField = null;
            }
            if (httpURLConnection2 == null) {
                httpURLConnection2.disconnect();
                str = contentType;
                str2 = headerField;
            } else {
                str = contentType;
                str2 = headerField;
            }
        } catch (IOException e5) {
            e = e5;
            contentType = null;
            httpURLConnection = httpURLConnection2;
            Log.e("FetchUrlMimeType", "Download failed: " + e);
            if (httpURLConnection != null) {
            }
            str = contentType;
            if (str != null) {
            }
            String strGuessFileName2 = URLUtil.guessFileName(this.mUri, str2, str);
            Log.d("Browser/FetchMimeType", "FetchUrlMimeType: Guess file name is: " + strGuessFileName2 + " mimeType is: " + str);
            this.mBrowserDownloadExt = Extensions.getDownloadPlugin(this.mContext);
            this.mBrowserDownloadExt.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(), this.mRequest, strGuessFileName2, str);
            ((DownloadManager) this.mContext.getSystemService("download")).enqueue(this.mRequest);
            return;
        } catch (Throwable th5) {
            th = th5;
            httpURLConnection3 = httpURLConnection2;
        }
        if (str != null && ((str.equalsIgnoreCase("text/plain") || str.equalsIgnoreCase("application/octet-stream")) && (mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(this.mUri))) != null)) {
            this.mRequest.setMimeType(mimeTypeFromExtension);
            str = mimeTypeFromExtension;
        }
        String strGuessFileName22 = URLUtil.guessFileName(this.mUri, str2, str);
        Log.d("Browser/FetchMimeType", "FetchUrlMimeType: Guess file name is: " + strGuessFileName22 + " mimeType is: " + str);
        this.mBrowserDownloadExt = Extensions.getDownloadPlugin(this.mContext);
        this.mBrowserDownloadExt.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(), this.mRequest, strGuessFileName22, str);
        ((DownloadManager) this.mContext.getSystemService("download")).enqueue(this.mRequest);
        return;
        if (httpURLConnection3 != null) {
            httpURLConnection3.disconnect();
        }
        throw th;
    }
}
