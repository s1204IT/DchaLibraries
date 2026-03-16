package android.media;

import android.media.SubtitleTrack;
import java.util.Vector;

class ClosedCaptionTrack extends SubtitleTrack {
    private final CCParser mCCParser;
    private final ClosedCaptionWidget mRenderingWidget;

    ClosedCaptionTrack(ClosedCaptionWidget renderingWidget, MediaFormat format) {
        super(format);
        this.mRenderingWidget = renderingWidget;
        this.mCCParser = new CCParser(renderingWidget);
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
