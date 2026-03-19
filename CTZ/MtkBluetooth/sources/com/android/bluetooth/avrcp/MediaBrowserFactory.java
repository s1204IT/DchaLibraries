package com.android.bluetooth.avrcp;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import com.android.bluetooth.avrcp.MediaBrowser;
import com.android.internal.annotations.VisibleForTesting;

public final class MediaBrowserFactory {
    private static MediaBrowser sInjectedBrowser;

    static MediaBrowser wrap(android.media.browse.MediaBrowser mediaBrowser) {
        if (sInjectedBrowser != null) {
            return sInjectedBrowser;
        }
        if (mediaBrowser != null) {
            return new MediaBrowser(mediaBrowser);
        }
        return null;
    }

    static MediaBrowser make(Context context, ComponentName componentName, MediaBrowser.ConnectionCallback connectionCallback, Bundle bundle) {
        if (sInjectedBrowser != null) {
            sInjectedBrowser.testInit(context, componentName, connectionCallback, bundle);
            return sInjectedBrowser;
        }
        return new MediaBrowser(context, componentName, connectionCallback, bundle);
    }

    @VisibleForTesting
    static void inject(MediaBrowser mediaBrowser) {
        sInjectedBrowser = mediaBrowser;
    }
}
