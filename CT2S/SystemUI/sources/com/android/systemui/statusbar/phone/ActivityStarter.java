package com.android.systemui.statusbar.phone;

import android.app.PendingIntent;
import android.content.Intent;

public interface ActivityStarter {
    void startActivity(Intent intent, boolean z);

    void startPendingIntentDismissingKeyguard(PendingIntent pendingIntent);
}
