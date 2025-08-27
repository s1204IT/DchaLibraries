package android.support.v4.media;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaSession2;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
class MediaUtils2 {
    static final MediaBrowserServiceCompat.BrowserRoot sDefaultBrowserRoot = new MediaBrowserServiceCompat.BrowserRoot("android.media.MediaLibraryService2", null);

    static List<MediaItem2> convertToMediaItem2List(Parcelable[] itemParcelableList) {
        MediaItem2 item;
        List<MediaItem2> playlist = new ArrayList<>();
        if (itemParcelableList != null) {
            for (int i = 0; i < itemParcelableList.length; i++) {
                if ((itemParcelableList[i] instanceof Bundle) && (item = MediaItem2.fromBundle((Bundle) itemParcelableList[i])) != null) {
                    playlist.add(item);
                }
            }
        }
        return playlist;
    }

    static List<Bundle> convertToBundleList(Parcelable[] array) {
        if (array == null) {
            return null;
        }
        List<Bundle> bundleList = new ArrayList<>();
        for (Parcelable p : array) {
            bundleList.add((Bundle) p);
        }
        return bundleList;
    }

    static List<MediaSession2.CommandButton> convertToCommandButtonList(Parcelable[] list) {
        MediaSession2.CommandButton button;
        List<MediaSession2.CommandButton> layout = new ArrayList<>();
        for (int i = 0; i < list.length; i++) {
            if ((list[i] instanceof Bundle) && (button = MediaSession2.CommandButton.fromBundle((Bundle) list[i])) != null) {
                layout.add(button);
            }
        }
        return layout;
    }
}
