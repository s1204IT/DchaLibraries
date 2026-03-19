package com.android.bluetooth.avrcp;

import android.content.Context;
import android.media.session.MediaSession;
import com.android.internal.annotations.VisibleForTesting;

public final class MediaControllerFactory {
    private static MediaController sInjectedController;

    static MediaController wrap(android.media.session.MediaController mediaController) {
        if (sInjectedController != null) {
            return sInjectedController;
        }
        if (mediaController != null) {
            return new MediaController(mediaController);
        }
        return null;
    }

    static MediaController make(Context context, MediaSession.Token token) {
        return sInjectedController != null ? sInjectedController : new MediaController(context, token);
    }

    @VisibleForTesting
    static void inject(MediaController mediaController) {
        sInjectedController = mediaController;
    }
}
