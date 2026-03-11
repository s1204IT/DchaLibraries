package com.android.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.WebAddress;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Toast;
import com.mediatek.browser.ext.IBrowserDownloadExt;
import java.io.File;
import java.net.URI;

public class DownloadHandler {
    private static final boolean DEBUG = Browser.DEBUG;
    private static IBrowserDownloadExt sBrowserDownloadExt = null;

    private static String encodePath(String str) {
        boolean z;
        char[] charArray = str.toCharArray();
        for (char c : charArray) {
            if (c == '[' || c == ']' || c == '|') {
                z = true;
                break;
            }
        }
        z = false;
        if (!z) {
            return str;
        }
        StringBuilder sb = new StringBuilder("");
        for (char c2 : charArray) {
            if (c2 == '[' || c2 == ']' || c2 == '|') {
                sb.append('%');
                sb.append(Integer.toHexString(c2));
            } else {
                sb.append(c2);
            }
        }
        return sb.toString();
    }

    public static void onDownloadStart(Activity activity, String str, String str2, String str3, String str4, String str5, boolean z, long j) {
        onDownloadStartNoStream(activity, str, str2, str3, str4, str5, z, j);
    }

    public static void onDownloadStartNoStream(Activity activity, String str, String str2, String str3, String str4, String str5, boolean z, long j) {
        int iIndexOf;
        String string;
        String[] strArr = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
        for (int i = 0; i < strArr.length; i++) {
            if (activity.checkSelfPermission(strArr[i]) != 0) {
                if (DEBUG) {
                    Log.d("browser/DLHandler", "no permission: " + strArr[i]);
                    return;
                }
                return;
            }
        }
        if (str4 != null && str4.startsWith("\"") && str4.endsWith("\"") && str4.length() > 2) {
            str4 = str4.substring(1, str4.length() - 1);
        }
        String strGuessFileName = URLUtil.guessFileName(str, str3, str4);
        if (DEBUG) {
            Log.d("browser/DLHandler", "Guess file name is: " + strGuessFileName + " mimetype is: " + str4);
        }
        String externalStorageState = Environment.getExternalStorageState();
        int i2 = 2131493220;
        if (!externalStorageState.equals("mounted")) {
            if (externalStorageState.equals("shared")) {
                string = activity.getString(2131493221);
            } else {
                string = activity.getString(2131493219, new Object[]{strGuessFileName});
                i2 = 2131493218;
            }
            new AlertDialog.Builder(activity).setTitle(i2).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(string).setPositiveButton(2131492964, (DialogInterface.OnClickListener) null).show();
            return;
        }
        String downloadPath = BrowserSettings.getInstance().getDownloadPath();
        if (downloadPath.startsWith("/storage/") && (iIndexOf = downloadPath.indexOf("/", "/storage/".length())) > 0) {
            String strSubstring = downloadPath.substring(0, iIndexOf);
            if (DEBUG) {
                Log.d("browser/DLHandler", "rootPath = " + strSubstring);
            }
            StorageVolume storageVolume = ((StorageManager) activity.getSystemService("storage")).getStorageVolume(new File(strSubstring));
            if (storageVolume == null) {
                if (DEBUG) {
                    Log.d("browser/DLHandler", "volume is null: " + downloadPath);
                }
            } else if (storageVolume.isRemovable() && !new File(strSubstring).canWrite()) {
                if (DEBUG) {
                    Log.d("browser/DLHandler", "  DownloadPath " + downloadPath + " can't write!");
                }
                new AlertDialog.Builder(activity).setTitle(2131492868).setIcon(android.R.drawable.ic_dialog_alert).setMessage(activity.getString(2131492869)).setPositiveButton(2131492964, (DialogInterface.OnClickListener) null).show();
                return;
            }
        }
        sBrowserDownloadExt = Extensions.getDownloadPlugin(activity);
        if (sBrowserDownloadExt.checkStorageBeforeDownload(activity, downloadPath, j)) {
            return;
        }
        try {
            WebAddress webAddress = new WebAddress(str);
            webAddress.setPath(encodePath(webAddress.getPath()));
            String string2 = webAddress.toString();
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(string2));
                request.setMimeType(str4);
                try {
                    sBrowserDownloadExt.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(), request, strGuessFileName, str4);
                    request.allowScanningByMediaScanner();
                    request.setDescription(webAddress.getHost());
                    String cookie = CookieManager.getInstance().getCookie(str, z);
                    request.addRequestHeader("cookie", cookie);
                    request.addRequestHeader("User-Agent", str2);
                    request.addRequestHeader("Referer", str5);
                    request.setNotificationVisibility(1);
                    if (str4 != null) {
                        new Thread("Browser download", (DownloadManager) activity.getSystemService("download"), request) {
                            final DownloadManager val$manager;
                            final DownloadManager.Request val$request;

                            {
                                this.val$manager = downloadManager;
                                this.val$request = request;
                            }

                            @Override
                            public void run() {
                                this.val$manager.enqueue(this.val$request);
                            }
                        }.start();
                    } else {
                        if (TextUtils.isEmpty(string2)) {
                            return;
                        }
                        try {
                            URI.create(string2);
                            new FetchUrlMimeType(activity, request, string2, cookie, str2).start();
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(activity, 2131493222, 0).show();
                            return;
                        }
                    }
                    sBrowserDownloadExt.showToastWithFileSize(activity, j, activity.getResources().getString(2131493223));
                    Intent intent = new Intent("android.intent.action.VIEW_DOWNLOADS");
                    intent.setFlags(268468224);
                    activity.startActivity(intent);
                } catch (IllegalStateException e2) {
                    Log.w("DLHandler", "Exception trying to create Download dir:", e2);
                    Toast.makeText(activity, 2131493220, 0).show();
                }
            } catch (IllegalArgumentException e3) {
                Toast.makeText(activity, 2131493222, 0).show();
            }
        } catch (Exception e4) {
            if (DEBUG) {
                Log.e("DLHandler", "Exception trying to parse url:" + str);
            }
        }
    }
}
