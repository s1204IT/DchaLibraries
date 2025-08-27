package com.android.browser.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.browser.BrowserActivity;

/* loaded from: classes.dex */
public class BookmarkWidgetProxy extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if ("com.android.browser.widget.CHANGE_FOLDER".equals(intent.getAction())) {
            BookmarkThumbnailWidgetService.changeFolder(context, intent);
        } else {
            if ("show_browser".equals(intent.getAction())) {
                startActivity(context, new Intent("show_browser", null, context, BrowserActivity.class));
                return;
            }
            Intent intent2 = new Intent(intent);
            intent2.setComponent(null);
            startActivity(context, intent2);
        }
    }

    void startActivity(Context context, Intent intent) {
        try {
            intent.addFlags(268435456);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w("BookmarkWidgetProxy", "Failed to start intent activity", e);
        }
    }
}
