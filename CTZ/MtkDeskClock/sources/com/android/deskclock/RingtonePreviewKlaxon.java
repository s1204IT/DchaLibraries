package com.android.deskclock;

import android.content.Context;
import android.net.Uri;

public final class RingtonePreviewKlaxon {
    private static AsyncRingtonePlayer sAsyncRingtonePlayer;

    private RingtonePreviewKlaxon() {
    }

    public static void stop(Context context) {
        LogUtils.i("RingtonePreviewKlaxon.stop()", new Object[0]);
        getAsyncRingtonePlayer(context).stop();
    }

    public static void start(Context context, Uri uri) {
        stop(context);
        LogUtils.i("RingtonePreviewKlaxon.start()", new Object[0]);
        getAsyncRingtonePlayer(context).play(uri, 0L);
    }

    private static synchronized AsyncRingtonePlayer getAsyncRingtonePlayer(Context context) {
        if (sAsyncRingtonePlayer == null) {
            sAsyncRingtonePlayer = new AsyncRingtonePlayer(context.getApplicationContext());
        }
        return sAsyncRingtonePlayer;
    }
}
