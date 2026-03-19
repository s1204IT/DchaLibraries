package android.media;

import android.content.Context;
import android.media.MediaController2;
import android.media.update.ApiLoader;
import android.media.update.MediaBrowser2Provider;
import android.os.Bundle;
import java.util.List;
import java.util.concurrent.Executor;

public class MediaBrowser2 extends MediaController2 {
    private final MediaBrowser2Provider mProvider;

    public static class BrowserCallback extends MediaController2.ControllerCallback {
        public void onGetLibraryRootDone(MediaBrowser2 mediaBrowser2, Bundle bundle, String str, Bundle bundle2) {
        }

        public void onChildrenChanged(MediaBrowser2 mediaBrowser2, String str, int i, Bundle bundle) {
        }

        public void onGetChildrenDone(MediaBrowser2 mediaBrowser2, String str, int i, int i2, List<MediaItem2> list, Bundle bundle) {
        }

        public void onGetItemDone(MediaBrowser2 mediaBrowser2, String str, MediaItem2 mediaItem2) {
        }

        public void onSearchResultChanged(MediaBrowser2 mediaBrowser2, String str, int i, Bundle bundle) {
        }

        public void onGetSearchResultDone(MediaBrowser2 mediaBrowser2, String str, int i, int i2, List<MediaItem2> list, Bundle bundle) {
        }
    }

    public MediaBrowser2(Context context, SessionToken2 sessionToken2, Executor executor, BrowserCallback browserCallback) {
        super(context, sessionToken2, executor, browserCallback);
        this.mProvider = (MediaBrowser2Provider) getProvider();
    }

    @Override
    MediaBrowser2Provider createProvider(Context context, SessionToken2 sessionToken2, Executor executor, MediaController2.ControllerCallback controllerCallback) {
        return ApiLoader.getProvider().createMediaBrowser2(context, this, sessionToken2, executor, (BrowserCallback) controllerCallback);
    }

    public void getLibraryRoot(Bundle bundle) {
        this.mProvider.getLibraryRoot_impl(bundle);
    }

    public void subscribe(String str, Bundle bundle) {
        this.mProvider.subscribe_impl(str, bundle);
    }

    public void unsubscribe(String str) {
        this.mProvider.unsubscribe_impl(str);
    }

    public void getChildren(String str, int i, int i2, Bundle bundle) {
        this.mProvider.getChildren_impl(str, i, i2, bundle);
    }

    public void getItem(String str) {
        this.mProvider.getItem_impl(str);
    }

    public void search(String str, Bundle bundle) {
        this.mProvider.search_impl(str, bundle);
    }

    public void getSearchResult(String str, int i, int i2, Bundle bundle) {
        this.mProvider.getSearchResult_impl(str, i, i2, bundle);
    }
}
