package android.media;

import android.media.Tokenizer;
import java.util.Vector;

class UnstyledTextExtractor implements Tokenizer.OnTokenListener {
    long mLastTimestamp;
    StringBuilder mLine = new StringBuilder();
    Vector<TextTrackCueSpan[]> mLines = new Vector<>();
    Vector<TextTrackCueSpan> mCurrentLine = new Vector<>();

    UnstyledTextExtractor() {
        init();
    }

    private void init() {
        this.mLine.delete(0, this.mLine.length());
        this.mLines.clear();
        this.mCurrentLine.clear();
        this.mLastTimestamp = -1L;
    }

    @Override
    public void onData(String s) {
        this.mLine.append(s);
    }

    @Override
    public void onStart(String tag, String[] classes, String annotation) {
    }

    @Override
    public void onEnd(String tag) {
    }

    @Override
    public void onTimeStamp(long timestampMs) {
        if (this.mLine.length() > 0 && timestampMs != this.mLastTimestamp) {
            this.mCurrentLine.add(new TextTrackCueSpan(this.mLine.toString(), this.mLastTimestamp));
            this.mLine.delete(0, this.mLine.length());
        }
        this.mLastTimestamp = timestampMs;
    }

    @Override
    public void onLineEnd() {
        if (this.mLine.length() > 0) {
            this.mCurrentLine.add(new TextTrackCueSpan(this.mLine.toString(), this.mLastTimestamp));
            this.mLine.delete(0, this.mLine.length());
        }
        TextTrackCueSpan[] spans = new TextTrackCueSpan[this.mCurrentLine.size()];
        this.mCurrentLine.toArray(spans);
        this.mCurrentLine.clear();
        this.mLines.add(spans);
    }

    public TextTrackCueSpan[][] getText() {
        if (this.mLine.length() > 0 || this.mCurrentLine.size() > 0) {
            onLineEnd();
        }
        TextTrackCueSpan[][] lines = new TextTrackCueSpan[this.mLines.size()][];
        this.mLines.toArray(lines);
        init();
        return lines;
    }
}
