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
    public void onData(String str) {
        this.mLine.append(str);
    }

    @Override
    public void onStart(String str, String[] strArr, String str2) {
    }

    @Override
    public void onEnd(String str) {
    }

    @Override
    public void onTimeStamp(long j) {
        if (this.mLine.length() > 0 && j != this.mLastTimestamp) {
            this.mCurrentLine.add(new TextTrackCueSpan(this.mLine.toString(), this.mLastTimestamp));
            this.mLine.delete(0, this.mLine.length());
        }
        this.mLastTimestamp = j;
    }

    @Override
    public void onLineEnd() {
        if (this.mLine.length() > 0) {
            this.mCurrentLine.add(new TextTrackCueSpan(this.mLine.toString(), this.mLastTimestamp));
            this.mLine.delete(0, this.mLine.length());
        }
        TextTrackCueSpan[] textTrackCueSpanArr = new TextTrackCueSpan[this.mCurrentLine.size()];
        this.mCurrentLine.toArray(textTrackCueSpanArr);
        this.mCurrentLine.clear();
        this.mLines.add(textTrackCueSpanArr);
    }

    public TextTrackCueSpan[][] getText() {
        if (this.mLine.length() > 0 || this.mCurrentLine.size() > 0) {
            onLineEnd();
        }
        TextTrackCueSpan[][] textTrackCueSpanArr = new TextTrackCueSpan[this.mLines.size()][];
        this.mLines.toArray(textTrackCueSpanArr);
        init();
        return textTrackCueSpanArr;
    }
}
