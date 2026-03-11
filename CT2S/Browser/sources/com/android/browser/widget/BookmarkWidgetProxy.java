package com.android.browser.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.browser.BrowserActivity;

public class BookmarkWidgetProxy extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.android.browser.widget.CHANGE_FOLDER".equals(intent.getAction())) {
            BookmarkThumbnailWidgetService.changeFolder(context, intent);
        } else {
            if ("show_browser".equals(intent.getAction())) {
                startActivity(context, new Intent("show_browser", null, context, BrowserActivity.class));
                return;
            }
            Intent view = new Intent(intent);
            view.setComponent(null);
            startActivity(context, view);
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
