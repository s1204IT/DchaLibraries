package android.support.v4.media;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaSession2;
import java.util.ArrayList;
import java.util.List;

class MediaUtils2 {
    static final MediaBrowserServiceCompat.BrowserRoot sDefaultBrowserRoot = new MediaBrowserServiceCompat.BrowserRoot("android.media.MediaLibraryService2", null);

    static List<Bundle> convertToBundleList(Parcelable[] parcelableArr) {
        if (parcelableArr == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        for (Parcelable parcelable : parcelableArr) {
            arrayList.add((Bundle) parcelable);
        }
        return arrayList;
    }

    static List<MediaSession2.CommandButton> convertToCommandButtonList(Parcelable[] parcelableArr) {
        MediaSession2.CommandButton commandButtonFromBundle;
        ArrayList arrayList = new ArrayList();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= parcelableArr.length) {
                return arrayList;
            }
            if ((parcelableArr[i2] instanceof Bundle) && (commandButtonFromBundle = MediaSession2.CommandButton.fromBundle((Bundle) parcelableArr[i2])) != null) {
                arrayList.add(commandButtonFromBundle);
            }
            i = i2 + 1;
        }
    }

    static List<MediaItem2> convertToMediaItem2List(Parcelable[] parcelableArr) {
        MediaItem2 mediaItem2FromBundle;
        ArrayList arrayList = new ArrayList();
        if (parcelableArr != null) {
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= parcelableArr.length) {
                    break;
                }
                if ((parcelableArr[i2] instanceof Bundle) && (mediaItem2FromBundle = MediaItem2.fromBundle((Bundle) parcelableArr[i2])) != null) {
                    arrayList.add(mediaItem2FromBundle);
                }
                i = i2 + 1;
            }
        }
        return arrayList;
    }
}
