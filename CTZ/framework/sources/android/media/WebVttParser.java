package android.media;

import android.app.Instrumentation;
import android.net.wifi.WifiEnterpriseConfig;
import android.provider.BrowserContract;
import android.provider.SettingsStringUtil;
import android.provider.Telephony;
import android.text.format.DateUtils;
import android.util.Log;
import com.android.internal.app.DumpHeapActivity;
import java.util.Vector;

class WebVttParser {
    private static final String TAG = "WebVttParser";
    private TextTrackCue mCue;
    private WebVttCueListener mListener;
    private final Phase mSkipRest = new Phase() {
        @Override
        public void parse(String str) {
        }
    };
    private final Phase mParseStart = new Phase() {
        @Override
        public void parse(String str) {
            if (str.startsWith("\ufeff")) {
                str = str.substring(1);
            }
            if (!str.equals("WEBVTT") && !str.startsWith("WEBVTT ") && !str.startsWith("WEBVTT\t")) {
                WebVttParser.this.log_warning("Not a WEBVTT header", str);
                WebVttParser.this.mPhase = WebVttParser.this.mSkipRest;
            } else {
                WebVttParser.this.mPhase = WebVttParser.this.mParseHeader;
            }
        }
    };
    private final Phase mParseHeader = new Phase() {
        static final boolean $assertionsDisabled = false;

        TextTrackRegion parseRegion(String str) {
            TextTrackRegion textTrackRegion = new TextTrackRegion();
            for (String str2 : str.split(" +")) {
                int iIndexOf = str2.indexOf(61);
                if (iIndexOf > 0 && iIndexOf != str2.length() - 1) {
                    String strSubstring = str2.substring(0, iIndexOf);
                    String strSubstring2 = str2.substring(iIndexOf + 1);
                    if (strSubstring.equals(Instrumentation.REPORT_KEY_IDENTIFIER)) {
                        textTrackRegion.mId = strSubstring2;
                    } else if (strSubstring.equals("width")) {
                        try {
                            textTrackRegion.mWidth = WebVttParser.parseFloatPercentage(strSubstring2);
                        } catch (NumberFormatException e) {
                            WebVttParser.this.log_warning("region setting", strSubstring, "has invalid value", e.getMessage(), strSubstring2);
                        }
                    } else if (strSubstring.equals("lines")) {
                        if (strSubstring2.matches(".*[^0-9].*")) {
                            WebVttParser.this.log_warning("lines", strSubstring, "contains an invalid character", strSubstring2);
                        } else {
                            try {
                                textTrackRegion.mLines = Integer.parseInt(strSubstring2);
                            } catch (NumberFormatException e2) {
                                WebVttParser.this.log_warning("region setting", strSubstring, "is not numeric", strSubstring2);
                            }
                        }
                    } else if (strSubstring.equals("regionanchor") || strSubstring.equals("viewportanchor")) {
                        int iIndexOf2 = strSubstring2.indexOf(",");
                        if (iIndexOf2 < 0) {
                            WebVttParser.this.log_warning("region setting", strSubstring, "contains no comma", strSubstring2);
                        } else {
                            String strSubstring3 = strSubstring2.substring(0, iIndexOf2);
                            String strSubstring4 = strSubstring2.substring(iIndexOf2 + 1);
                            try {
                                float floatPercentage = WebVttParser.parseFloatPercentage(strSubstring3);
                                try {
                                    float floatPercentage2 = WebVttParser.parseFloatPercentage(strSubstring4);
                                    if (strSubstring.charAt(0) == 'r') {
                                        textTrackRegion.mAnchorPointX = floatPercentage;
                                        textTrackRegion.mAnchorPointY = floatPercentage2;
                                    } else {
                                        textTrackRegion.mViewportAnchorPointX = floatPercentage;
                                        textTrackRegion.mViewportAnchorPointY = floatPercentage2;
                                    }
                                } catch (NumberFormatException e3) {
                                    WebVttParser.this.log_warning("region setting", strSubstring, "has invalid y component", e3.getMessage(), strSubstring4);
                                }
                            } catch (NumberFormatException e4) {
                                WebVttParser.this.log_warning("region setting", strSubstring, "has invalid x component", e4.getMessage(), strSubstring3);
                            }
                        }
                    } else if (strSubstring.equals("scroll")) {
                        if (!strSubstring2.equals("up")) {
                            WebVttParser.this.log_warning("region setting", strSubstring, "has invalid value", strSubstring2);
                        } else {
                            textTrackRegion.mScrollValue = 301;
                        }
                    }
                }
            }
            return textTrackRegion;
        }

        @Override
        public void parse(String str) {
            if (str.length() == 0) {
                WebVttParser.this.mPhase = WebVttParser.this.mParseCueId;
                return;
            }
            if (str.contains("-->")) {
                WebVttParser.this.mPhase = WebVttParser.this.mParseCueTime;
                WebVttParser.this.mPhase.parse(str);
                return;
            }
            int iIndexOf = str.indexOf(58);
            if (iIndexOf <= 0 || iIndexOf >= str.length() - 1) {
                WebVttParser.this.log_warning("meta data header has invalid format", str);
            }
            String strSubstring = str.substring(0, iIndexOf);
            String strSubstring2 = str.substring(iIndexOf + 1);
            if (strSubstring.equals("Region")) {
                WebVttParser.this.mListener.onRegionParsed(parseRegion(strSubstring2));
            }
        }
    };
    private final Phase mParseCueId = new Phase() {
        static final boolean $assertionsDisabled = false;

        @Override
        public void parse(String str) {
            if (str.length() == 0) {
                return;
            }
            if (str.equals("NOTE") || str.startsWith("NOTE ")) {
                WebVttParser.this.mPhase = WebVttParser.this.mParseCueText;
            }
            WebVttParser.this.mCue = new TextTrackCue();
            WebVttParser.this.mCueTexts.clear();
            WebVttParser.this.mPhase = WebVttParser.this.mParseCueTime;
            if (str.contains("-->")) {
                WebVttParser.this.mPhase.parse(str);
            } else {
                WebVttParser.this.mCue.mId = str;
            }
        }
    };
    private final Phase mParseCueTime = new Phase() {
        static final boolean $assertionsDisabled = false;

        @Override
        public void parse(String str) {
            String strSubstring;
            int iIndexOf = str.indexOf("-->");
            if (iIndexOf < 0) {
                WebVttParser.this.mCue = null;
                WebVttParser.this.mPhase = WebVttParser.this.mParseCueId;
                return;
            }
            String strTrim = str.substring(0, iIndexOf).trim();
            String strReplaceFirst = str.substring(iIndexOf + 3).replaceFirst("^\\s+", "").replaceFirst("\\s+", WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            int iIndexOf2 = strReplaceFirst.indexOf(32);
            if (iIndexOf2 > 0) {
                strSubstring = strReplaceFirst.substring(0, iIndexOf2);
            } else {
                strSubstring = strReplaceFirst;
            }
            String strSubstring2 = iIndexOf2 > 0 ? strReplaceFirst.substring(iIndexOf2 + 1) : "";
            WebVttParser.this.mCue.mStartTimeMs = WebVttParser.parseTimestampMs(strTrim);
            WebVttParser.this.mCue.mEndTimeMs = WebVttParser.parseTimestampMs(strSubstring);
            for (String str2 : strSubstring2.split(" +")) {
                int iIndexOf3 = str2.indexOf(58);
                if (iIndexOf3 > 0 && iIndexOf3 != str2.length() - 1) {
                    String strSubstring3 = str2.substring(0, iIndexOf3);
                    String strSubstring4 = str2.substring(iIndexOf3 + 1);
                    if (strSubstring3.equals(TtmlUtils.TAG_REGION)) {
                        WebVttParser.this.mCue.mRegionId = strSubstring4;
                    } else if (strSubstring3.equals("vertical")) {
                        if (strSubstring4.equals("rl")) {
                            WebVttParser.this.mCue.mWritingDirection = 101;
                        } else if (strSubstring4.equals("lr")) {
                            WebVttParser.this.mCue.mWritingDirection = 102;
                        } else {
                            WebVttParser.this.log_warning("cue setting", strSubstring3, "has invalid value", strSubstring4);
                        }
                    } else if (strSubstring3.equals("line")) {
                        try {
                            if (strSubstring4.endsWith("%")) {
                                WebVttParser.this.mCue.mSnapToLines = false;
                                WebVttParser.this.mCue.mLinePosition = Integer.valueOf(WebVttParser.parseIntPercentage(strSubstring4));
                            } else if (strSubstring4.matches(".*[^0-9].*")) {
                                WebVttParser.this.log_warning("cue setting", strSubstring3, "contains an invalid character", strSubstring4);
                            } else {
                                WebVttParser.this.mCue.mSnapToLines = true;
                                WebVttParser.this.mCue.mLinePosition = Integer.valueOf(Integer.parseInt(strSubstring4));
                            }
                        } catch (NumberFormatException e) {
                            WebVttParser.this.log_warning("cue setting", strSubstring3, "is not numeric or percentage", strSubstring4);
                        }
                    } else if (strSubstring3.equals(BrowserContract.Bookmarks.POSITION)) {
                        try {
                            WebVttParser.this.mCue.mTextPosition = WebVttParser.parseIntPercentage(strSubstring4);
                        } catch (NumberFormatException e2) {
                            WebVttParser.this.log_warning("cue setting", strSubstring3, "is not numeric or percentage", strSubstring4);
                        }
                    } else if (strSubstring3.equals(DumpHeapActivity.KEY_SIZE)) {
                        try {
                            WebVttParser.this.mCue.mSize = WebVttParser.parseIntPercentage(strSubstring4);
                        } catch (NumberFormatException e3) {
                            WebVttParser.this.log_warning("cue setting", strSubstring3, "is not numeric or percentage", strSubstring4);
                        }
                    } else if (strSubstring3.equals("align")) {
                        if (strSubstring4.equals(Telephony.BaseMmsColumns.START)) {
                            WebVttParser.this.mCue.mAlignment = 201;
                        } else if (strSubstring4.equals("middle")) {
                            WebVttParser.this.mCue.mAlignment = 200;
                        } else if (strSubstring4.equals("end")) {
                            WebVttParser.this.mCue.mAlignment = 202;
                        } else if (strSubstring4.equals("left")) {
                            WebVttParser.this.mCue.mAlignment = 203;
                        } else if (strSubstring4.equals("right")) {
                            WebVttParser.this.mCue.mAlignment = 204;
                        } else {
                            WebVttParser.this.log_warning("cue setting", strSubstring3, "has invalid value", strSubstring4);
                        }
                    }
                }
            }
            if (WebVttParser.this.mCue.mLinePosition != null || WebVttParser.this.mCue.mSize != 100 || WebVttParser.this.mCue.mWritingDirection != 100) {
                WebVttParser.this.mCue.mRegionId = "";
            }
            WebVttParser.this.mPhase = WebVttParser.this.mParseCueText;
        }
    };
    private final Phase mParseCueText = new Phase() {
        @Override
        public void parse(String str) {
            if (str.length() != 0) {
                if (WebVttParser.this.mCue != null) {
                    WebVttParser.this.mCueTexts.add(str);
                }
            } else {
                WebVttParser.this.yieldCue();
                WebVttParser.this.mPhase = WebVttParser.this.mParseCueId;
            }
        }
    };
    private Phase mPhase = this.mParseStart;
    private String mBuffer = "";
    private Vector<String> mCueTexts = new Vector<>();

    interface Phase {
        void parse(String str);
    }

    WebVttParser(WebVttCueListener webVttCueListener) {
        this.mListener = webVttCueListener;
    }

    public static float parseFloatPercentage(String str) throws NumberFormatException {
        if (!str.endsWith("%")) {
            throw new NumberFormatException("does not end in %");
        }
        String strSubstring = str.substring(0, str.length() - 1);
        if (strSubstring.matches(".*[^0-9.].*")) {
            throw new NumberFormatException("contains an invalid character");
        }
        try {
            float f = Float.parseFloat(strSubstring);
            if (f < 0.0f || f > 100.0f) {
                throw new NumberFormatException("is out of range");
            }
            return f;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("is not a number");
        }
    }

    public static int parseIntPercentage(String str) throws NumberFormatException {
        if (!str.endsWith("%")) {
            throw new NumberFormatException("does not end in %");
        }
        String strSubstring = str.substring(0, str.length() - 1);
        if (strSubstring.matches(".*[^0-9].*")) {
            throw new NumberFormatException("contains an invalid character");
        }
        try {
            int i = Integer.parseInt(strSubstring);
            if (i < 0 || i > 100) {
                throw new NumberFormatException("is out of range");
            }
            return i;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("is not a number");
        }
    }

    public static long parseTimestampMs(String str) throws NumberFormatException {
        if (!str.matches("(\\d+:)?[0-5]\\d:[0-5]\\d\\.\\d{3}")) {
            throw new NumberFormatException("has invalid format");
        }
        String[] strArrSplit = str.split("\\.", 2);
        long j = 0;
        for (String str2 : strArrSplit[0].split(SettingsStringUtil.DELIMITER)) {
            j = (j * 60) + Long.parseLong(str2);
        }
        return (j * 1000) + Long.parseLong(strArrSplit[1]);
    }

    public static String timeToString(long j) {
        return String.format("%d:%02d:%02d.%03d", Long.valueOf(j / 3600000), Long.valueOf((j / DateUtils.MINUTE_IN_MILLIS) % 60), Long.valueOf((j / 1000) % 60), Long.valueOf(j % 1000));
    }

    public void parse(String str) {
        boolean z;
        this.mBuffer = (this.mBuffer + str.replace("\u0000", "�")).replace("\r\n", "\n");
        if (this.mBuffer.endsWith("\r")) {
            this.mBuffer = this.mBuffer.substring(0, this.mBuffer.length() - 1);
            z = true;
        } else {
            z = false;
        }
        String[] strArrSplit = this.mBuffer.split("[\r\n]");
        for (int i = 0; i < strArrSplit.length - 1; i++) {
            this.mPhase.parse(strArrSplit[i]);
        }
        this.mBuffer = strArrSplit[strArrSplit.length - 1];
        if (z) {
            this.mBuffer += "\r";
        }
    }

    public void eos() {
        if (this.mBuffer.endsWith("\r")) {
            this.mBuffer = this.mBuffer.substring(0, this.mBuffer.length() - 1);
        }
        this.mPhase.parse(this.mBuffer);
        this.mBuffer = "";
        yieldCue();
        this.mPhase = this.mParseStart;
    }

    public void yieldCue() {
        if (this.mCue != null && this.mCueTexts.size() > 0) {
            this.mCue.mStrings = new String[this.mCueTexts.size()];
            this.mCueTexts.toArray(this.mCue.mStrings);
            this.mCueTexts.clear();
            this.mListener.onCueParsed(this.mCue);
        }
        this.mCue = null;
    }

    private void log_warning(String str, String str2, String str3, String str4, String str5) {
        Log.w(getClass().getName(), str + " '" + str2 + "' " + str3 + " ('" + str5 + "' " + str4 + ")");
    }

    private void log_warning(String str, String str2, String str3, String str4) {
        Log.w(getClass().getName(), str + " '" + str2 + "' " + str3 + " ('" + str4 + "')");
    }

    private void log_warning(String str, String str2) {
        Log.w(getClass().getName(), str + " ('" + str2 + "')");
    }
}
