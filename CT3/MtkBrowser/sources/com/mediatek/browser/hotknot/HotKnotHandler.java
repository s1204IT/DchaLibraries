package com.mediatek.browser.hotknot;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;
import com.mediatek.hotknot.HotKnotAdapter;
import com.mediatek.hotknot.HotKnotMessage;

public class HotKnotHandler {
    private static HotKnotAdapter mHotKnotAdapter = null;
    private static Activity mActivity = null;

    public static void hotKnotInit(Activity activity) {
        mActivity = activity;
        mHotKnotAdapter = HotKnotAdapter.getDefaultAdapter(mActivity);
        if (mHotKnotAdapter == null) {
            Log.d("browser/HotKnotHandler", "hotKnotInit fail, hotKnotAdapter is null");
        } else {
            Log.d("browser/HotKnotHandler", "hotKnotInit completed");
        }
    }

    public static boolean isHotKnotSupported() {
        if (mHotKnotAdapter != null) {
            return true;
        }
        return false;
    }

    public static void hotKnotStart(String url) {
        Log.d("browser/HotKnotHandler", "hotKnotStart, url:" + url);
        if (mHotKnotAdapter == null) {
            Log.e("browser/HotKnotHandler", "hotKnotStart fail, hotKnotAdapter is null");
            return;
        }
        if (url == null || url.length() == 0) {
            StringBuilder sbAppend = new StringBuilder().append("hotKnotStart fail, url:");
            if (url == null) {
                url = "url";
            }
            Log.e("browser/HotKnotHandler", sbAppend.append(url).toString());
            return;
        }
        Parcelable hotKnotMessage = new HotKnotMessage("com.mediatek.browser.hotknot/com.mediatek.browser.hotknot.MIME_TYPE", url.getBytes());
        Intent intent = new Intent("com.mediatek.hotknot.action.SHARE");
        intent.putExtra("com.mediatek.hotknot.extra.SHARE_MSG", hotKnotMessage);
        intent.addFlags(134742016);
        mActivity.startActivity(intent);
    }
}
