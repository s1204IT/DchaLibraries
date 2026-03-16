package com.android.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.WebAddress;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Toast;

public class DownloadHandler {
    public static void onDownloadStart(Activity activity, String url, String userAgent, String contentDisposition, String mimetype, String referer, boolean privateBrowsing) {
        if (contentDisposition == null || !contentDisposition.regionMatches(true, 0, "attachment", 0, 10)) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setDataAndType(Uri.parse(url), mimetype);
            ResolveInfo info = activity.getPackageManager().resolveActivity(intent, 65536);
            if (info != null) {
                ComponentName myName = activity.getComponentName();
                if (!myName.getPackageName().equals(info.activityInfo.packageName) || !myName.getClassName().equals(info.activityInfo.name)) {
                    try {
                        activity.startActivity(intent);
                        return;
                    } catch (ActivityNotFoundException ex) {
                        Log.d("DLHandler", "activity not found for " + mimetype + " over " + Uri.parse(url).getScheme(), ex);
                    }
                }
            }
        }
        onDownloadStartNoStream(activity, url, userAgent, contentDisposition, mimetype, referer, privateBrowsing);
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
        if (needed) {
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
        return path;
    }

    static void onDownloadStartNoStream(Activity activity, String url, String userAgent, String contentDisposition, String mimetype, String referer, boolean privateBrowsing) {
        String msg;
        int title;
        String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
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
        try {
            WebAddress webAddress = new WebAddress(url);
            webAddress.setPath(encodePath(webAddress.getPath()));
            String addressString = webAddress.toString();
            Uri uri = Uri.parse(addressString);
            try {
                final DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setMimeType(mimetype);
                try {
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                    request.allowScanningByMediaScanner();
                    request.setDescription(webAddress.getHost());
                    String cookies = CookieManager.getInstance().getCookie(url, privateBrowsing);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.addRequestHeader("Referer", referer);
                    request.setNotificationVisibility(1);
                    if (mimetype == null) {
                        if (!TextUtils.isEmpty(addressString)) {
                            new FetchUrlMimeType(activity, request, addressString, cookies, userAgent).start();
                        } else {
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
                    Toast.makeText(activity, R.string.download_pending, 0).show();
                } catch (IllegalStateException ex) {
                    Log.w("DLHandler", "Exception trying to create Download dir:", ex);
                    Toast.makeText(activity, R.string.download_sdcard_busy_dlg_title, 0).show();
                }
            } catch (IllegalArgumentException e) {
                Toast.makeText(activity, R.string.cannot_download, 0).show();
            }
        } catch (Exception e2) {
            Log.e("DLHandler", "Exception trying to parse url:" + url);
        }
    }
}
