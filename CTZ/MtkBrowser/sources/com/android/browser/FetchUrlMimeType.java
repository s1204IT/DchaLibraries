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
        HttpURLConnection httpURLConnection;
        Throwable th;
        HttpURLConnection httpURLConnection2;
        String str;
        String headerField;
        String mimeTypeFromExtension;
        String str2 = null;
        try {
            try {
                httpURLConnection2 = (HttpURLConnection) new URL(this.mUri).openConnection();
                try {
                    httpURLConnection2.setRequestMethod("HEAD");
                    if (this.mUserAgent != null) {
                        httpURLConnection2.addRequestProperty("User-Agent", this.mUserAgent);
                    }
                    if (this.mCookies != null && this.mCookies.length() > 0) {
                        httpURLConnection2.addRequestProperty("Cookie", this.mCookies);
                    }
                    if (httpURLConnection2.getResponseCode() == 501 || httpURLConnection2.getResponseCode() == 400) {
                        Log.d("Browser/FetchMimeType", "FetchUrlMimeType:  use Get method");
                        httpURLConnection2.disconnect();
                        HttpURLConnection httpURLConnection3 = (HttpURLConnection) new URL(this.mUri).openConnection();
                        try {
                            if (this.mUserAgent != null) {
                                httpURLConnection3.addRequestProperty("User-Agent", this.mUserAgent);
                            }
                            httpURLConnection2 = httpURLConnection3;
                            if (httpURLConnection2.getResponseCode() != 200) {
                                String contentType = httpURLConnection2.getContentType();
                                if (contentType != null) {
                                    try {
                                        int iIndexOf = contentType.indexOf(59);
                                        if (iIndexOf != -1) {
                                            contentType = contentType.substring(0, iIndexOf);
                                        }
                                    } catch (IOException e) {
                                        str = contentType;
                                        e = e;
                                        Log.e("FetchUrlMimeType", "Download failed: " + e);
                                        if (httpURLConnection2 != null) {
                                            httpURLConnection2.disconnect();
                                        }
                                        String str3 = str;
                                        headerField = null;
                                        str2 = str3;
                                    }
                                }
                                headerField = httpURLConnection2.getHeaderField("Content-Disposition");
                                str2 = contentType;
                            } else {
                                headerField = null;
                            }
                            if (httpURLConnection2 != null) {
                                httpURLConnection2.disconnect();
                            }
                        } catch (IOException e2) {
                            str = null;
                            e = e2;
                            httpURLConnection2 = httpURLConnection3;
                            Log.e("FetchUrlMimeType", "Download failed: " + e);
                            if (httpURLConnection2 != null) {
                            }
                            String str32 = str;
                            headerField = null;
                            str2 = str32;
                            if (str2 != null) {
                                this.mRequest.setMimeType(mimeTypeFromExtension);
                                str2 = mimeTypeFromExtension;
                            }
                            String strGuessFileName = URLUtil.guessFileName(this.mUri, headerField, str2);
                            Log.d("Browser/FetchMimeType", "FetchUrlMimeType: Guess file name is: " + strGuessFileName + " mimeType is: " + str2);
                            this.mBrowserDownloadExt = Extensions.getDownloadPlugin(this.mContext);
                            this.mBrowserDownloadExt.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(), this.mRequest, strGuessFileName, str2);
                            ((DownloadManager) this.mContext.getSystemService("download")).enqueue(this.mRequest);
                        } catch (Throwable th2) {
                            th = th2;
                            httpURLConnection = httpURLConnection3;
                            if (httpURLConnection != null) {
                                httpURLConnection.disconnect();
                            }
                            throw th;
                        }
                    } else {
                        if (httpURLConnection2.getResponseCode() != 200) {
                        }
                        if (httpURLConnection2 != null) {
                        }
                    }
                } catch (IOException e3) {
                    e = e3;
                    str = null;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (IOException e4) {
            e = e4;
            httpURLConnection2 = null;
            str = null;
        } catch (Throwable th4) {
            httpURLConnection = null;
            th = th4;
        }
        if (str2 != null && ((str2.equalsIgnoreCase("text/plain") || str2.equalsIgnoreCase("application/octet-stream")) && (mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(this.mUri))) != null)) {
            this.mRequest.setMimeType(mimeTypeFromExtension);
            str2 = mimeTypeFromExtension;
        }
        String strGuessFileName2 = URLUtil.guessFileName(this.mUri, headerField, str2);
        Log.d("Browser/FetchMimeType", "FetchUrlMimeType: Guess file name is: " + strGuessFileName2 + " mimeType is: " + str2);
        this.mBrowserDownloadExt = Extensions.getDownloadPlugin(this.mContext);
        this.mBrowserDownloadExt.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(), this.mRequest, strGuessFileName2, str2);
        ((DownloadManager) this.mContext.getSystemService("download")).enqueue(this.mRequest);
    }
}
