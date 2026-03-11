package com.android.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.WebAddress;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Toast;
import com.mediatek.browser.ext.IBrowserDownloadExt;
import com.mediatek.storage.StorageManagerEx;
import java.io.File;
import java.net.URI;

public class DownloadHandler {
    private static IBrowserDownloadExt sBrowserDownloadExt = null;

    public static void onDownloadStart(Activity activity, String url, String userAgent, String contentDisposition, String mimetype, String referer, boolean privateBrowsing, long contentLength) {
        onDownloadStartNoStream(activity, url, userAgent, contentDisposition, mimetype, referer, privateBrowsing, contentLength);
    }

    private static String encodePath(String path) {
        char[] chars = path.toCharArray();
        boolean needed = false;
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                needed = true;
                break;
            }
        }
        if (!needed) {
            return path;
        }
        StringBuilder sb = new StringBuilder("");
        for (char c2 : chars) {
            if (c2 == '[' || c2 == ']' || c2 == '|') {
                sb.append('%');
                sb.append(Integer.toHexString(c2));
            } else {
                sb.append(c2);
            }
        }
        return sb.toString();
    }

    public static void onDownloadStartNoStream(Activity activity, String url, String userAgent, String contentDisposition, String mimetype, String referer, boolean privateBrowsing, long contentLength) {
        int i;
        String msg;
        int title;
        if (mimetype != null && mimetype.startsWith("\"") && mimetype.endsWith("\"") && mimetype.length() > 2) {
            mimetype = mimetype.substring(1, mimetype.length() - 1);
        }
        String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
        Log.d("browser/DLHandler", "Guess file name is: " + filename + " mimetype is: " + mimetype);
        String status = Environment.getExternalStorageState();
        if (!status.equals("mounted")) {
            if (status.equals("shared")) {
                msg = activity.getString(R.string.download_sdcard_busy_dlg_msg);
                title = R.string.download_sdcard_busy_dlg_title;
            } else {
                msg = activity.getString(R.string.download_no_sdcard_dlg_msg, new Object[]{filename});
                title = R.string.download_no_sdcard_dlg_title;
            }
            new AlertDialog.Builder(activity).setTitle(title).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(msg).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
            return;
        }
        String mDownloadPath = BrowserSettings.getInstance().getDownloadPath();
        if (mDownloadPath.startsWith("/storage/") && (i = mDownloadPath.indexOf("/", "/storage/".length())) > 0) {
            String rootPath = mDownloadPath.substring(0, i);
            Log.d("browser/DLHandler", "rootPath = " + rootPath);
            if (StorageManagerEx.isExternalSDCard(rootPath) && !new File(rootPath).canWrite()) {
                Log.d("browser/DLHandler", "  DownloadPath " + mDownloadPath + " can't write!");
                String mMsg = activity.getString(R.string.download_path_unavailable_dlg_msg);
                new AlertDialog.Builder(activity).setTitle(R.string.download_path_unavailable_dlg_title).setIcon(android.R.drawable.ic_dialog_alert).setMessage(mMsg).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
                return;
            }
        }
        sBrowserDownloadExt = Extensions.getDownloadPlugin(activity);
        if (sBrowserDownloadExt.checkStorageBeforeDownload(activity, mDownloadPath, contentLength)) {
            return;
        }
        try {
            WebAddress webAddress = new WebAddress(url);
            webAddress.setPath(encodePath(webAddress.getPath()));
            String addressString = webAddress.toString();
            Uri uri = Uri.parse(addressString);
            try {
                final DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setMimeType(mimetype);
                try {
                    sBrowserDownloadExt.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(), request, filename, mimetype);
                    request.allowScanningByMediaScanner();
                    request.setDescription(webAddress.getHost());
                    String cookies = CookieManager.getInstance().getCookie(url, privateBrowsing);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.addRequestHeader("Referer", referer);
                    request.setNotificationVisibility(1);
                    request.setUserAgent(userAgent);
                    if (mimetype == null) {
                        if (TextUtils.isEmpty(addressString)) {
                            return;
                        }
                        try {
                            URI.create(addressString);
                            new FetchUrlMimeType(activity, request, addressString, cookies, userAgent).start();
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(activity, R.string.cannot_download, 0).show();
                            return;
                        }
                    } else {
                        final DownloadManager manager = (DownloadManager) activity.getSystemService("download");
                        new Thread("Browser download") {
                            @Override
                            public void run() {
                                manager.enqueue(request);
                            }
                        }.start();
                    }
                    sBrowserDownloadExt.showToastWithFileSize(activity, contentLength, activity.getResources().getString(R.string.download_pending));
                    Intent pageView = new Intent("android.intent.action.VIEW_DOWNLOADS");
                    pageView.setFlags(268468224);
                    activity.startActivity(pageView);
                } catch (IllegalStateException ex) {
                    Log.w("DLHandler", "Exception trying to create Download dir:", ex);
                    Toast.makeText(activity, R.string.download_sdcard_busy_dlg_title, 0).show();
                }
            } catch (IllegalArgumentException e2) {
                Toast.makeText(activity, R.string.cannot_download, 0).show();
            }
        } catch (Exception e3) {
            Log.e("DLHandler", "Exception trying to parse url:" + url);
        }
    }
}
