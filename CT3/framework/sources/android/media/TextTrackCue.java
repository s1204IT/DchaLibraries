package android.media;

import android.media.SubtitleTrack;
import android.net.ProxyInfo;
import android.net.wifi.WifiEnterpriseConfig;
import java.util.Arrays;

class TextTrackCue extends SubtitleTrack.Cue {
    static final int ALIGNMENT_END = 202;
    static final int ALIGNMENT_LEFT = 203;
    static final int ALIGNMENT_MIDDLE = 200;
    static final int ALIGNMENT_RIGHT = 204;
    static final int ALIGNMENT_START = 201;
    private static final String TAG = "TTCue";
    static final int WRITING_DIRECTION_HORIZONTAL = 100;
    static final int WRITING_DIRECTION_VERTICAL_LR = 102;
    static final int WRITING_DIRECTION_VERTICAL_RL = 101;
    boolean mAutoLinePosition;
    String[] mStrings;
    String mId = ProxyInfo.LOCAL_EXCL_LIST;
    boolean mPauseOnExit = false;
    int mWritingDirection = 100;
    String mRegionId = ProxyInfo.LOCAL_EXCL_LIST;
    boolean mSnapToLines = true;
    Integer mLinePosition = null;
    int mTextPosition = 50;
    int mSize = 100;
    int mAlignment = 200;
    TextTrackCueSpan[][] mLines = null;
    TextTrackRegion mRegion = null;

    TextTrackCue() {
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof TextTrackCue)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        try {
            boolean res = this.mId.equals(obj.mId) && this.mPauseOnExit == obj.mPauseOnExit && this.mWritingDirection == obj.mWritingDirection && this.mRegionId.equals(obj.mRegionId) && this.mSnapToLines == obj.mSnapToLines && this.mAutoLinePosition == obj.mAutoLinePosition && (this.mAutoLinePosition || ((this.mLinePosition != null && this.mLinePosition.equals(obj.mLinePosition)) || (this.mLinePosition == null && obj.mLinePosition == null))) && this.mTextPosition == obj.mTextPosition && this.mSize == obj.mSize && this.mAlignment == obj.mAlignment && this.mLines.length == obj.mLines.length;
            if (res) {
                for (int line = 0; line < this.mLines.length; line++) {
                    if (!Arrays.equals(this.mLines[line], obj.mLines[line])) {
                        return false;
                    }
                }
            }
            return res;
        } catch (IncompatibleClassChangeError e) {
            return false;
        }
    }

    public StringBuilder appendStringsToBuilder(StringBuilder builder) {
        if (this.mStrings == null) {
            builder.append("null");
        } else {
            builder.append("[");
            boolean first = true;
            for (String s : this.mStrings) {
                if (!first) {
                    builder.append(", ");
                }
                if (s == null) {
                    builder.append("null");
                } else {
                    builder.append("\"");
                    builder.append(s);
                    builder.append("\"");
                }
                first = false;
            }
            builder.append("]");
        }
        return builder;
    }

    public StringBuilder appendLinesToBuilder(StringBuilder builder) {
        if (this.mLines == null) {
            builder.append("null");
        } else {
            builder.append("[");
            boolean first = true;
            for (TextTrackCueSpan[] spans : this.mLines) {
                if (!first) {
                    builder.append(", ");
                }
                if (spans == null) {
                    builder.append("null");
                } else {
                    builder.append("\"");
                    boolean innerFirst = true;
                    long lastTimestamp = -1;
                    for (TextTrackCueSpan span : spans) {
                        if (!innerFirst) {
                            builder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        }
                        if (span.mTimestampMs != lastTimestamp) {
                            builder.append("<").append(WebVttParser.timeToString(span.mTimestampMs)).append(">");
                            lastTimestamp = span.mTimestampMs;
                        }
                        builder.append(span.mText);
                        innerFirst = false;
                    }
                    builder.append("\"");
                }
                first = false;
            }
            builder.append("]");
        }
        return builder;
    }

    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append(WebVttParser.timeToString(this.mStartTimeMs)).append(" --> ").append(WebVttParser.timeToString(this.mEndTimeMs)).append(" {id:\"").append(this.mId).append("\", pauseOnExit:").append(this.mPauseOnExit).append(", direction:").append(this.mWritingDirection == 100 ? "horizontal" : this.mWritingDirection == 102 ? "vertical_lr" : this.mWritingDirection == 101 ? "vertical_rl" : "INVALID").append(", regionId:\"").append(this.mRegionId).append("\", snapToLines:").append(this.mSnapToLines).append(", linePosition:").append(this.mAutoLinePosition ? "auto" : this.mLinePosition).append(", textPosition:").append(this.mTextPosition).append(", size:").append(this.mSize).append(", alignment:").append(this.mAlignment == 202 ? "end" : this.mAlignment == 203 ? "left" : this.mAlignment == 200 ? "middle" : this.mAlignment == 204 ? "right" : this.mAlignment == 201 ? "start" : "INVALID").append(", text:");
        appendStringsToBuilder(res).append("}");
        return res.toString();
    }

    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public void onTime(long timeMs) {
        for (TextTrackCueSpan[] line : this.mLines) {
            for (TextTrackCueSpan span : line) {
                span.mEnabled = timeMs >= span.mTimestampMs;
            }
        }
    }
}
