package android.media;

import android.media.SubtitleTrack;
import java.util.Vector;

class Cea608CaptionTrack extends SubtitleTrack {
    private final Cea608CCParser mCCParser;
    private final Cea608CCWidget mRenderingWidget;

    Cea608CaptionTrack(Cea608CCWidget renderingWidget, MediaFormat format) {
        super(format);
        this.mRenderingWidget = renderingWidget;
        this.mCCParser = new Cea608CCParser(this.mRenderingWidget);
    }

    @Override
    public void onData(byte[] data, boolean eos, long runID) {
        this.mCCParser.parse(data);
    }

    @Override
    public SubtitleTrack.RenderingWidget getRenderingWidget() {
        return this.mRenderingWidget;
    }

    @Override
    public void updateView(Vector<SubtitleTrack.Cue> activeCues) {
    }
}
