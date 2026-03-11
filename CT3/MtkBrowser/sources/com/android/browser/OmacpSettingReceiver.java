package com.android.browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;

public class OmacpSettingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean result;
        Log.d("@M_browser/OmacpSettingReceiver", "OmacpSettingReceiver action:" + intent.getAction());
        context.getContentResolver();
        if ("com.mediatek.omacp.settings".equals(intent.getAction())) {
            String folderName = intent.getStringExtra("NAME");
            if (folderName == null) {
                result = setBookmarkAndHomePage(context, intent, 1L);
            } else {
                Log.i("@M_browser/OmacpSettingReceiver", "folderName isn't null");
                long folderId = AddBookmarkPage.addFolderToRoot(context, folderName);
                result = setBookmarkAndHomePage(context, intent, folderId);
            }
            sendSettingResult(context, result);
        }
        if (!"com.mediatek.omacp.capability".equals(intent.getAction())) {
            return;
        }
        sendCapabilityResult(context);
    }

    private boolean setBookmarkAndHomePage(Context context, Intent intent, long folderId) {
        String formattedUrl;
        if (-1 == folderId) {
            return false;
        }
        context.getContentResolver();
        ArrayList<HashMap<String, String>> resourceMapList = (ArrayList) intent.getSerializableExtra("RESOURCE");
        if (resourceMapList == null) {
            Log.i("@M_browser/OmacpSettingReceiver", "resourceMapList is null");
            return false;
        }
        Log.i("@M_browser/OmacpSettingReceiver", "resourceMapList size:" + resourceMapList.size());
        boolean hasSetStartPage = false;
        for (HashMap<String, String> item : resourceMapList) {
            String url = item.get("URI");
            String name = item.get("NAME");
            String startPage = item.get("STARTPAGE");
            if (url != null && (formattedUrl = UrlUtils.fixUrl(url)) != null) {
                if (name == null) {
                    name = formattedUrl;
                }
                Bookmarks.addBookmark(context, false, formattedUrl, name, null, folderId);
                if (!hasSetStartPage && startPage != null && startPage.equals("1")) {
                    setHomePage(context, formattedUrl);
                    hasSetStartPage = true;
                }
                Log.i("@M_browser/OmacpSettingReceiver", "BOOKMARK_URI: " + formattedUrl);
                Log.i("@M_browser/OmacpSettingReceiver", "BOOKMARK_NAME: " + name);
                Log.i("@M_browser/OmacpSettingReceiver", "STARTPAGE: " + startPage);
            }
        }
        return true;
    }

    private boolean setHomePage(Context context, String url) {
        if (url == null || url.length() <= 0) {
            return false;
        }
        if (!url.startsWith("http:")) {
            url = "http://" + url;
        }
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = p.edit();
        editor.putString("homepage", url);
        editor.commit();
        return true;
    }

    private void sendSettingResult(Context context, boolean result) {
        Intent intent = new Intent("com.mediatek.omacp.settings.result");
        intent.putExtra("appId", "w2");
        intent.putExtra("result", result);
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
