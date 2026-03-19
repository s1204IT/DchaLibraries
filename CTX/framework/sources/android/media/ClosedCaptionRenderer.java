package android.media;

import android.content.Context;
import android.media.SubtitleController;

public class ClosedCaptionRenderer extends SubtitleController.Renderer {
    private Cea608CCWidget mCCWidget;
    private final Context mContext;

    public ClosedCaptionRenderer(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean supports(MediaFormat mediaFormat) {
        if (mediaFormat.containsKey(MediaFormat.KEY_MIME)) {
            return "text/cea-608".equals(mediaFormat.getString(MediaFormat.KEY_MIME));
        }
        return false;
    }

    @Override
    public SubtitleTrack createTrack(MediaFormat mediaFormat) {
        if ("text/cea-608".equals(mediaFormat.getString(MediaFormat.KEY_MIME))) {
            if (this.mCCWidget == null) {
                this.mCCWidget = new Cea608CCWidget(this.mContext);
            }
            return new Cea608CaptionTrack(this.mCCWidget, mediaFormat);
        }
        throw new RuntimeException("No matching format: " + mediaFormat.toString());
    }
}
