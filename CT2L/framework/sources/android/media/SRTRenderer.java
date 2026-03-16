package android.media;

import android.content.Context;
import android.media.SubtitleController;
import android.os.Handler;

public class SRTRenderer extends SubtitleController.Renderer {
    private final Context mContext;
    private final Handler mEventHandler;
    private final boolean mRender;
    private WebVttRenderingWidget mRenderingWidget;

    public SRTRenderer(Context context) {
        this(context, null);
    }

    SRTRenderer(Context mContext, Handler mEventHandler) {
        this.mContext = mContext;
        this.mRender = mEventHandler == null;
        this.mEventHandler = mEventHandler;
    }

    @Override
    public boolean supports(MediaFormat format) {
        if (format.containsKey(MediaFormat.KEY_MIME) && format.getString(MediaFormat.KEY_MIME).equals(MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP)) {
            return this.mRender == (format.getInteger(MediaFormat.KEY_IS_TIMED_TEXT, 0) == 0);
        }
        return false;
    }

    @Override
    public SubtitleTrack createTrack(MediaFormat format) {
        if (this.mRender && this.mRenderingWidget == null) {
            this.mRenderingWidget = new WebVttRenderingWidget(this.mContext);
        }
        return this.mRender ? new SRTTrack(this.mRenderingWidget, format) : new SRTTrack(this.mEventHandler, format);
    }
}
