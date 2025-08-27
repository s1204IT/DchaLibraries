package com.android.browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/* loaded from: classes.dex */
public class OmacpSettingReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        boolean bookmarkAndHomePage;
        Log.d("@M_browser/OmacpSettingReceiver", "OmacpSettingReceiver action:" + intent.getAction());
        context.getContentResolver();
        if ("com.mediatek.omacp.settings".equals(intent.getAction())) {
            String stringExtra = intent.getStringExtra("NAME");
            if (stringExtra == null) {
                bookmarkAndHomePage = setBookmarkAndHomePage(context, intent, 1L);
            } else {
                Log.i("@M_browser/OmacpSettingReceiver", "folderName isn't null");
                bookmarkAndHomePage = setBookmarkAndHomePage(context, intent, AddBookmarkPage.addFolderToRoot(context, stringExtra));
            }
            sendSettingResult(context, bookmarkAndHomePage);
        }
        if ("com.mediatek.omacp.capability".equals(intent.getAction())) {
            sendCapabilityResult(context);
        }
    }

    private boolean setBookmarkAndHomePage(Context context, Intent intent, long j) {
        String strFixUrl;
        boolean z = false;
        if (-1 == j) {
            return false;
        }
        context.getContentResolver();
        ArrayList arrayList = (ArrayList) intent.getSerializableExtra("RESOURCE");
        if (arrayList == null) {
            Log.i("@M_browser/OmacpSettingReceiver", "resourceMapList is null");
            return false;
        }
        Log.i("@M_browser/OmacpSettingReceiver", "resourceMapList size:" + arrayList.size());
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            HashMap map = (HashMap) it.next();
            String str = (String) map.get("URI");
            String str2 = (String) map.get("NAME");
            String str3 = (String) map.get("STARTPAGE");
            if (str != null && (strFixUrl = UrlUtils.fixUrl(str)) != null) {
                String str4 = str2 == null ? strFixUrl : str2;
                String strSmartUrlFilter = UrlUtils.smartUrlFilter(strFixUrl);
                Bookmarks.addBookmark(context, false, strSmartUrlFilter, str4, null, j);
                if (!z && str3 != null && str3.equals("1")) {
                    setHomePage(context, strSmartUrlFilter);
                    z = true;
                }
                Log.i("@M_browser/OmacpSettingReceiver", "BOOKMARK_URI: " + strSmartUrlFilter);
                Log.i("@M_browser/OmacpSettingReceiver", "BOOKMARK_NAME: " + str4);
                Log.i("@M_browser/OmacpSettingReceiver", "STARTPAGE: " + str3);
            }
        }
        return true;
    }

    private boolean setHomePage(Context context, String str) {
        if (str == null || str.length() <= 0) {
            return false;
        }
        if (!str.startsWith("http:")) {
            str = "http://" + str;
        }
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editorEdit.putString("homepage", str);
        editorEdit.commit();
        return true;
    }

    private void sendSettingResult(Context context, boolean z) {
        Intent intent = new Intent("com.mediatek.omacp.settings.result");
        intent.putExtra("appId", "w2");
        intent.putExtra("result", z);
        Log.i("@M_browser/OmacpSettingReceiver", "Setting Broadcasting: " + intent);
        context.sendBroadcast(intent);
    }

    private void sendCapabilityResult(Context context) {
        Intent intent = new Intent("com.mediatek.omacp.capability.result");
        intent.putExtra("appId", "w2");
        intent.putExtra("browser", true);
        intent.putExtra("browser_bookmark_folder", true);
        intent.putExtra("browser_to_proxy", false);
        intent.putExtra("browser_to_napid", false);
        intent.putExtra("browser_bookmark_name", true);
        intent.putExtra("browser_bookmark", true);
        intent.putExtra("browser_username", false);
        intent.putExtra("browser_password", false);
        intent.putExtra("browser_homepage", true);
        Log.i("@M_browser/OmacpSettingReceiver", "Capability Broadcasting: " + intent);
        context.sendBroadcast(intent);
    }
}
