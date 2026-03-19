package android.widget;

import android.content.Context;
import android.media.SessionToken2;
import android.media.update.ApiLoader;
import android.media.update.MediaControlView2Provider;
import android.media.update.ViewGroupHelper;
import android.media.update.ViewGroupProvider;
import android.util.AttributeSet;
import android.view.View;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MediaControlView2 extends ViewGroupHelper<MediaControlView2Provider> {
    public static final int BUTTON_ASPECT_RATIO = 10;
    public static final int BUTTON_FFWD = 2;
    public static final int BUTTON_FULL_SCREEN = 7;
    public static final int BUTTON_MUTE = 9;
    public static final int BUTTON_NEXT = 4;
    public static final int BUTTON_OVERFLOW = 8;
    public static final int BUTTON_PLAY_PAUSE = 1;
    public static final int BUTTON_PREV = 5;
    public static final int BUTTON_REW = 3;
    public static final int BUTTON_SETTINGS = 11;
    public static final int BUTTON_SUBTITLE = 6;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Button {
    }

    public interface OnFullScreenListener {
        void onFullScreen(View view, boolean z);
    }

    public MediaControlView2(Context context) {
        this(context, null);
    }

    public MediaControlView2(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public MediaControlView2(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public MediaControlView2(Context context, final AttributeSet attributeSet, final int i, final int i2) {
        super(new ViewGroupHelper.ProviderCreator() {
            @Override
            public final ViewGroupProvider createProvider(ViewGroupHelper viewGroupHelper, ViewGroupProvider viewGroupProvider, ViewGroupProvider viewGroupProvider2) {
                return ApiLoader.getProvider().createMediaControlView2((MediaControlView2) viewGroupHelper, viewGroupProvider, viewGroupProvider2, attributeSet, i, i2);
            }
        }, context, attributeSet, i, i2);
        ((MediaControlView2Provider) this.mProvider).initialize(attributeSet, i, i2);
    }

    public void setMediaSessionToken(SessionToken2 sessionToken2) {
        ((MediaControlView2Provider) this.mProvider).setMediaSessionToken_impl(sessionToken2);
    }

    public void setOnFullScreenListener(OnFullScreenListener onFullScreenListener) {
        ((MediaControlView2Provider) this.mProvider).setOnFullScreenListener_impl(onFullScreenListener);
    }

    public void setController(android.media.session.MediaController mediaController) {
        ((MediaControlView2Provider) this.mProvider).setController_impl(mediaController);
    }

    public void setButtonVisibility(int i, int i2) {
        ((MediaControlView2Provider) this.mProvider).setButtonVisibility_impl(i, i2);
    }

    public void requestPlayButtonFocus() {
        ((MediaControlView2Provider) this.mProvider).requestPlayButtonFocus_impl();
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        ((MediaControlView2Provider) this.mProvider).onLayout_impl(z, i, i2, i3, i4);
    }
}
