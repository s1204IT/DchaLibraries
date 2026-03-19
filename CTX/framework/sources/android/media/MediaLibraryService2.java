package android.media;

import android.app.PendingIntent;
import android.media.MediaLibraryService2;
import android.media.MediaSession2;
import android.media.update.ApiLoader;
import android.media.update.MediaLibraryService2Provider;
import android.media.update.MediaSessionService2Provider;
import android.media.update.ProviderCreator;
import android.os.Bundle;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class MediaLibraryService2 extends MediaSessionService2 {
    public static final String SERVICE_INTERFACE = "android.media.MediaLibraryService2";

    @Override
    public abstract MediaLibrarySession onCreateSession(String str);

    public static final class MediaLibrarySession extends MediaSession2 {
        private final MediaLibraryService2Provider.MediaLibrarySessionProvider mProvider;

        public static class MediaLibrarySessionCallback extends MediaSession2.SessionCallback {
            public LibraryRoot onGetLibraryRoot(MediaLibrarySession mediaLibrarySession, MediaSession2.ControllerInfo controllerInfo, Bundle bundle) {
                return null;
            }

            public MediaItem2 onGetItem(MediaLibrarySession mediaLibrarySession, MediaSession2.ControllerInfo controllerInfo, String str) {
                return null;
            }

            public List<MediaItem2> onGetChildren(MediaLibrarySession mediaLibrarySession, MediaSession2.ControllerInfo controllerInfo, String str, int i, int i2, Bundle bundle) {
                return null;
            }

            public void onSubscribe(MediaLibrarySession mediaLibrarySession, MediaSession2.ControllerInfo controllerInfo, String str, Bundle bundle) {
            }

            public void onUnsubscribe(MediaLibrarySession mediaLibrarySession, MediaSession2.ControllerInfo controllerInfo, String str) {
            }

            public void onSearch(MediaLibrarySession mediaLibrarySession, MediaSession2.ControllerInfo controllerInfo, String str, Bundle bundle) {
            }

            public List<MediaItem2> onGetSearchResult(MediaLibrarySession mediaLibrarySession, MediaSession2.ControllerInfo controllerInfo, String str, int i, int i2, Bundle bundle) {
                return null;
            }
        }

        public static final class Builder extends MediaSession2.BuilderBase<MediaLibrarySession, Builder, MediaLibrarySessionCallback> {
            public Builder(final MediaLibraryService2 mediaLibraryService2, final Executor executor, final MediaLibrarySessionCallback mediaLibrarySessionCallback) {
                super(new ProviderCreator() {
                    @Override
                    public final Object createProvider(Object obj) {
                        return ApiLoader.getProvider().createMediaLibraryService2Builder(mediaLibraryService2, (MediaLibraryService2.MediaLibrarySession.Builder) ((MediaSession2.BuilderBase) obj), executor, mediaLibrarySessionCallback);
                    }
                });
            }

            @Override
            public Builder setPlayer(MediaPlayerBase mediaPlayerBase) {
                return (Builder) super.setPlayer(mediaPlayerBase);
            }

            @Override
            public Builder setPlaylistAgent(MediaPlaylistAgent mediaPlaylistAgent) {
                return (Builder) super.setPlaylistAgent(mediaPlaylistAgent);
            }

            @Override
            public Builder setVolumeProvider(VolumeProvider2 volumeProvider2) {
                return (Builder) super.setVolumeProvider(volumeProvider2);
            }

            @Override
            public Builder setSessionActivity(PendingIntent pendingIntent) {
                return (Builder) super.setSessionActivity(pendingIntent);
            }

            @Override
            public Builder setId(String str) {
                return (Builder) super.setId(str);
            }

            @Override
            public Builder setSessionCallback(Executor executor, MediaLibrarySessionCallback mediaLibrarySessionCallback) {
                return (Builder) super.setSessionCallback(executor, mediaLibrarySessionCallback);
            }

            @Override
            public MediaLibrarySession build() {
                return (MediaLibrarySession) super.build();
            }
        }

        public MediaLibrarySession(MediaLibraryService2Provider.MediaLibrarySessionProvider mediaLibrarySessionProvider) {
            super(mediaLibrarySessionProvider);
            this.mProvider = mediaLibrarySessionProvider;
        }

        public void notifyChildrenChanged(MediaSession2.ControllerInfo controllerInfo, String str, int i, Bundle bundle) {
            this.mProvider.notifyChildrenChanged_impl(controllerInfo, str, i, bundle);
        }

        public void notifyChildrenChanged(String str, int i, Bundle bundle) {
            this.mProvider.notifyChildrenChanged_impl(str, i, bundle);
        }

        public void notifySearchResultChanged(MediaSession2.ControllerInfo controllerInfo, String str, int i, Bundle bundle) {
            this.mProvider.notifySearchResultChanged_impl(controllerInfo, str, i, bundle);
        }
    }

    @Override
    MediaSessionService2Provider createProvider() {
        return ApiLoader.getProvider().createMediaLibraryService2(this);
    }

    public static final class LibraryRoot {
        public static final String EXTRA_OFFLINE = "android.media.extra.OFFLINE";
        public static final String EXTRA_RECENT = "android.media.extra.RECENT";
        public static final String EXTRA_SUGGESTED = "android.media.extra.SUGGESTED";
        private final MediaLibraryService2Provider.LibraryRootProvider mProvider;

        public LibraryRoot(String str, Bundle bundle) {
            this.mProvider = ApiLoader.getProvider().createMediaLibraryService2LibraryRoot(this, str, bundle);
        }

        public String getRootId() {
            return this.mProvider.getRootId_impl();
        }

        public Bundle getExtras() {
            return this.mProvider.getExtras_impl();
        }
    }
}
