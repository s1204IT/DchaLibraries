package android.media;

import android.app.slice.Slice;
import android.media.SubtitleTrack;
import android.net.wifi.WifiEnterpriseConfig;
import android.provider.Telephony;
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
    String mId = "";
    boolean mPauseOnExit = false;
    int mWritingDirection = 100;
    String mRegionId = "";
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
            TextTrackCue textTrackCue = (TextTrackCue) obj;
            boolean z = this.mId.equals(textTrackCue.mId) && this.mPauseOnExit == textTrackCue.mPauseOnExit && this.mWritingDirection == textTrackCue.mWritingDirection && this.mRegionId.equals(textTrackCue.mRegionId) && this.mSnapToLines == textTrackCue.mSnapToLines && this.mAutoLinePosition == textTrackCue.mAutoLinePosition && (this.mAutoLinePosition || ((this.mLinePosition != null && this.mLinePosition.equals(textTrackCue.mLinePosition)) || (this.mLinePosition == null && textTrackCue.mLinePosition == null))) && this.mTextPosition == textTrackCue.mTextPosition && this.mSize == textTrackCue.mSize && this.mAlignment == textTrackCue.mAlignment && this.mLines.length == textTrackCue.mLines.length;
            if (z) {
                for (int i = 0; i < this.mLines.length; i++) {
                    if (!Arrays.equals(this.mLines[i], textTrackCue.mLines[i])) {
                        return false;
                    }
                }
            }
            return z;
        } catch (IncompatibleClassChangeError e) {
            return false;
        }
    }

    public StringBuilder appendStringsToBuilder(StringBuilder sb) {
        if (this.mStrings == null) {
            sb.append("null");
        } else {
            sb.append("[");
            String[] strArr = this.mStrings;
            int length = strArr.length;
            boolean z = true;
            int i = 0;
            while (i < length) {
                String str = strArr[i];
                if (!z) {
                    sb.append(", ");
                }
                if (str == null) {
                    sb.append("null");
                } else {
                    sb.append("\"");
                    sb.append(str);
                    sb.append("\"");
                }
                i++;
                z = false;
            }
            sb.append("]");
        }
        return sb;
    }

    public StringBuilder appendLinesToBuilder(StringBuilder sb) {
        if (this.mLines == null) {
            sb.append("null");
        } else {
            sb.append("[");
            TextTrackCueSpan[][] textTrackCueSpanArr = this.mLines;
            int length = textTrackCueSpanArr.length;
            int i = 0;
            boolean z = true;
            while (i < length) {
                TextTrackCueSpan[] textTrackCueSpanArr2 = textTrackCueSpanArr[i];
                if (!z) {
                    sb.append(", ");
                }
                if (textTrackCueSpanArr2 == null) {
                    sb.append("null");
                } else {
                    sb.append("\"");
                    int length2 = textTrackCueSpanArr2.length;
                    long j = -1;
                    int i2 = 0;
                    boolean z2 = true;
                    while (i2 < length2) {
                        TextTrackCueSpan textTrackCueSpan = textTrackCueSpanArr2[i2];
                        if (!z2) {
                            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        }
                        if (textTrackCueSpan.mTimestampMs != j) {
                            sb.append("<");
                            sb.append(WebVttParser.timeToString(textTrackCueSpan.mTimestampMs));
                            sb.append(">");
                            j = textTrackCueSpan.mTimestampMs;
                        }
                        sb.append(textTrackCueSpan.mText);
                        i2++;
                        z2 = false;
                    }
                    sb.append("\"");
                }
                i++;
                z = false;
            }
            sb.append("]");
        }
        return sb;
    }

    public String toString() {
        String str;
        String str2;
        StringBuilder sb = new StringBuilder();
        sb.append(WebVttParser.timeToString(this.mStartTimeMs));
        sb.append(" --> ");
        sb.append(WebVttParser.timeToString(this.mEndTimeMs));
        sb.append(" {id:\"");
        sb.append(this.mId);
        sb.append("\", pauseOnExit:");
        sb.append(this.mPauseOnExit);
        sb.append(", direction:");
        if (this.mWritingDirection == 100) {
            str = Slice.HINT_HORIZONTAL;
        } else if (this.mWritingDirection == 102) {
            str = "vertical_lr";
        } else {
            str = this.mWritingDirection == 101 ? "vertical_rl" : "INVALID";
        }
        sb.append(str);
        sb.append(", regionId:\"");
        sb.append(this.mRegionId);
        sb.append("\", snapToLines:");
        sb.append(this.mSnapToLines);
        sb.append(", linePosition:");
        sb.append(this.mAutoLinePosition ? "auto" : this.mLinePosition);
        sb.append(", textPosition:");
        sb.append(this.mTextPosition);
        sb.append(", size:");
        sb.append(this.mSize);
        sb.append(", alignment:");
        if (this.mAlignment == 202) {
            str2 = "end";
        } else if (this.mAlignment == 203) {
            str2 = "left";
        } else if (this.mAlignment == 200) {
            str2 = "middle";
        } else if (this.mAlignment == 204) {
            str2 = "right";
        } else {
            str2 = this.mAlignment == 201 ? Telephony.BaseMmsColumns.START : "INVALID";
        }
        sb.append(str2);
        sb.append(", text:");
        appendStringsToBuilder(sb).append("}");
        return sb.toString();
    }

    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public void onTime(long j) {
        for (TextTrackCueSpan[] textTrackCueSpanArr : this.mLines) {
            for (TextTrackCueSpan textTrackCueSpan : textTrackCueSpanArr) {
                textTrackCueSpan.mEnabled = j >= textTrackCueSpan.mTimestampMs;
            }
        }
    }
}
