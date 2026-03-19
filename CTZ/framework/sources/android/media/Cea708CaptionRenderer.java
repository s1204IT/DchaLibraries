package android.media;

import android.content.Context;
import android.media.SubtitleController;

public class Cea708CaptionRenderer extends SubtitleController.Renderer {
    private Cea708CCWidget mCCWidget;
    private final Context mContext;

    public Cea708CaptionRenderer(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean supports(MediaFormat mediaFormat) {
        if (mediaFormat.containsKey(MediaFormat.KEY_MIME)) {
            return "text/cea-708".equals(mediaFormat.getString(MediaFormat.KEY_MIME));
        }
        return false;
    }

    @Override
    public SubtitleTrack createTrack(MediaFormat mediaFormat) {
        if ("text/cea-708".equals(mediaFormat.getString(MediaFormat.KEY_MIME))) {
            if (this.mCCWidget == null) {
                this.mCCWidget = new Cea708CCWidget(this.mContext);
            }
            return new Cea708CaptionTrack(this.mCCWidget, mediaFormat);
        }
        throw new RuntimeException("No matching format: " + mediaFormat.toString());
    }
}
