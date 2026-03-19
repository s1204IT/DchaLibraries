package android.media;

class TextTrackCueSpan {
    boolean mEnabled;
    String mText;
    long mTimestampMs;

    TextTrackCueSpan(String text, long timestamp) {
        this.mTimestampMs = timestamp;
        this.mText = text;
        this.mEnabled = this.mTimestampMs < 0;
    }

    public boolean equals(Object obj) {
        if ((obj instanceof TextTrackCueSpan) && this.mTimestampMs == obj.mTimestampMs) {
            return this.mText.equals(obj.mText);
        }
        return false;
    }
}
