package com.android.systemui.screenshot;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import com.android.systemui.R;

/* loaded from: classes.dex */
public class ScreenshotServiceErrorReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) throws Resources.NotFoundException {
        GlobalScreenshot.notifyScreenshotError(context, (NotificationManager) context.getSystemService("notification"), R.string.screenshot_failed_to_save_unknown_text);
    }
}
