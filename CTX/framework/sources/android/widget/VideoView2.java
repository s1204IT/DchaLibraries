package android.widget;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.DataSourceDesc;
import android.media.MediaItem2;
import android.media.MediaMetadata2;
import android.media.SessionToken2;
import android.media.session.PlaybackState;
import android.media.update.ApiLoader;
import android.media.update.VideoView2Provider;
import android.media.update.ViewGroupHelper;
import android.media.update.ViewGroupProvider;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import com.android.internal.annotations.VisibleForTesting;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class VideoView2 extends ViewGroupHelper<VideoView2Provider> {
    public static final int VIEW_TYPE_SURFACEVIEW = 1;
    public static final int VIEW_TYPE_TEXTUREVIEW = 2;

    public interface OnCustomActionListener {
        void onCustomAction(String str, Bundle bundle);
    }

    public interface OnFullScreenRequestListener {
        void onFullScreenRequest(View view, boolean z);
    }

    @VisibleForTesting
    public interface OnViewTypeChangedListener {
        void onViewTypeChanged(View view, int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ViewType {
    }

    public VideoView2(Context context) {
        this(context, null);
    }

    public VideoView2(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public VideoView2(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public VideoView2(Context context, final AttributeSet attributeSet, final int i, final int i2) {
        super(new ViewGroupHelper.ProviderCreator() {
            @Override
            public final ViewGroupProvider createProvider(ViewGroupHelper viewGroupHelper, ViewGroupProvider viewGroupProvider, ViewGroupProvider viewGroupProvider2) {
                return ApiLoader.getProvider().createVideoView2((VideoView2) viewGroupHelper, viewGroupProvider, viewGroupProvider2, attributeSet, i, i2);
            }
        }, context, attributeSet, i, i2);
        ((VideoView2Provider) this.mProvider).initialize(attributeSet, i, i2);
    }

    public void setMediaControlView2(MediaControlView2 mediaControlView2, long j) {
        ((VideoView2Provider) this.mProvider).setMediaControlView2_impl(mediaControlView2, j);
    }

    public MediaControlView2 getMediaControlView2() {
        return ((VideoView2Provider) this.mProvider).getMediaControlView2_impl();
    }

    public void setMediaMetadata(MediaMetadata2 mediaMetadata2) {
        ((VideoView2Provider) this.mProvider).setMediaMetadata_impl(mediaMetadata2);
    }

    public MediaMetadata2 getMediaMetadata() {
        return ((VideoView2Provider) this.mProvider).getMediaMetadata_impl();
    }

    public android.media.session.MediaController getMediaController() {
        return ((VideoView2Provider) this.mProvider).getMediaController_impl();
    }

    public SessionToken2 getMediaSessionToken() {
        return ((VideoView2Provider) this.mProvider).getMediaSessionToken_impl();
    }

    public void setSubtitleEnabled(boolean z) {
        ((VideoView2Provider) this.mProvider).setSubtitleEnabled_impl(z);
    }

    public boolean isSubtitleEnabled() {
        return ((VideoView2Provider) this.mProvider).isSubtitleEnabled_impl();
    }

    public void setSpeed(float f) {
        ((VideoView2Provider) this.mProvider).setSpeed_impl(f);
    }

    public void setAudioFocusRequest(int i) {
        ((VideoView2Provider) this.mProvider).setAudioFocusRequest_impl(i);
    }

    public void setAudioAttributes(AudioAttributes audioAttributes) {
        ((VideoView2Provider) this.mProvider).setAudioAttributes_impl(audioAttributes);
    }

    public void setVideoPath(String str) {
        ((VideoView2Provider) this.mProvider).setVideoPath_impl(str);
    }

    public void setVideoUri(Uri uri) {
        ((VideoView2Provider) this.mProvider).setVideoUri_impl(uri);
    }

    public void setVideoUri(Uri uri, Map<String, String> map) {
        ((VideoView2Provider) this.mProvider).setVideoUri_impl(uri, map);
    }

    public void setMediaItem(MediaItem2 mediaItem2) {
        ((VideoView2Provider) this.mProvider).setMediaItem_impl(mediaItem2);
    }

    public void setDataSource(DataSourceDesc dataSourceDesc) {
        ((VideoView2Provider) this.mProvider).setDataSource_impl(dataSourceDesc);
    }

    public void setViewType(int i) {
        ((VideoView2Provider) this.mProvider).setViewType_impl(i);
    }

    public int getViewType() {
        return ((VideoView2Provider) this.mProvider).getViewType_impl();
    }

    public void setCustomActions(List<PlaybackState.CustomAction> list, Executor executor, OnCustomActionListener onCustomActionListener) {
        ((VideoView2Provider) this.mProvider).setCustomActions_impl(list, executor, onCustomActionListener);
    }

    @VisibleForTesting
    public void setOnViewTypeChangedListener(OnViewTypeChangedListener onViewTypeChangedListener) {
        ((VideoView2Provider) this.mProvider).setOnViewTypeChangedListener_impl(onViewTypeChangedListener);
    }

    public void setFullScreenRequestListener(OnFullScreenRequestListener onFullScreenRequestListener) {
        ((VideoView2Provider) this.mProvider).setFullScreenRequestListener_impl(onFullScreenRequestListener);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        ((VideoView2Provider) this.mProvider).onLayout_impl(z, i, i2, i3, i4);
    }
}
