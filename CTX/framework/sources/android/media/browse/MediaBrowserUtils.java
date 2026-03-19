package android.media.browse;

import android.os.Bundle;

public class MediaBrowserUtils {
    public static boolean areSameOptions(Bundle bundle, Bundle bundle2) {
        if (bundle == bundle2) {
            return true;
        }
        if (bundle == null) {
            if (bundle2.getInt(MediaBrowser.EXTRA_PAGE, -1) == -1 && bundle2.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1) == -1) {
                return true;
            }
            return false;
        }
        if (bundle2 == null) {
            if (bundle.getInt(MediaBrowser.EXTRA_PAGE, -1) == -1 && bundle.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1) == -1) {
                return true;
            }
            return false;
        }
        if (bundle.getInt(MediaBrowser.EXTRA_PAGE, -1) == bundle2.getInt(MediaBrowser.EXTRA_PAGE, -1) && bundle.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1) == bundle2.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1)) {
            return true;
        }
        return false;
    }

    public static boolean hasDuplicatedItems(Bundle bundle, Bundle bundle2) {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        if (bundle != null) {
            i = bundle.getInt(MediaBrowser.EXTRA_PAGE, -1);
        } else {
            i = -1;
        }
        if (bundle2 != null) {
            i2 = bundle2.getInt(MediaBrowser.EXTRA_PAGE, -1);
        } else {
            i2 = -1;
        }
        if (bundle != null) {
            i3 = bundle.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1);
        } else {
            i3 = -1;
        }
        if (bundle2 != null) {
            i4 = bundle2.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1);
        } else {
            i4 = -1;
        }
        int i8 = Integer.MAX_VALUE;
        if (i != -1 && i3 != -1) {
            i6 = i * i3;
            i5 = (i3 + i6) - 1;
        } else {
            i5 = Integer.MAX_VALUE;
            i6 = 0;
        }
        if (i2 != -1 && i4 != -1) {
            i7 = i4 * i2;
            i8 = (i4 + i7) - 1;
        } else {
            i7 = 0;
        }
        if (i6 > i7 || i7 > i5) {
            return i6 <= i8 && i8 <= i5;
        }
        return true;
    }
}
