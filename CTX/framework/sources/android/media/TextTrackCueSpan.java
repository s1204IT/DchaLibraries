package android.media;

class TextTrackCueSpan {
    boolean mEnabled;
    String mText;
    long mTimestampMs;

    TextTrackCueSpan(String str, long j) {
        this.mTimestampMs = j;
        this.mText = str;
        this.mEnabled = this.mTimestampMs < 0;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof TextTrackCueSpan)) {
            return false;
        }
        TextTrackCueSpan textTrackCueSpan = (TextTrackCueSpan) obj;
        return this.mTimestampMs == textTrackCueSpan.mTimestampMs && this.mText.equals(textTrackCueSpan.mText);
    }
}
