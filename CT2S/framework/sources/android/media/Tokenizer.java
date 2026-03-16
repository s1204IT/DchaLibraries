package android.media;

import android.net.ProxyInfo;
import android.util.Log;

class Tokenizer {
    private static final String TAG = "Tokenizer";
    private int mHandledLen;
    private String mLine;
    private OnTokenListener mListener;
    private TokenizerPhase mPhase;
    private TokenizerPhase mDataTokenizer = new DataTokenizer();
    private TokenizerPhase mTagTokenizer = new TagTokenizer();

    interface OnTokenListener {
        void onData(String str);

        void onEnd(String str);

        void onLineEnd();

        void onStart(String str, String[] strArr, String str2);

        void onTimeStamp(long j);
    }

    interface TokenizerPhase {
        TokenizerPhase start();

        void tokenize();
    }

    static int access$108(Tokenizer x0) {
        int i = x0.mHandledLen;
        x0.mHandledLen = i + 1;
        return i;
    }

    static int access$112(Tokenizer x0, int x1) {
        int i = x0.mHandledLen + x1;
        x0.mHandledLen = i;
        return i;
    }

    class DataTokenizer implements TokenizerPhase {
        private StringBuilder mData;

        DataTokenizer() {
        }

        @Override
        public TokenizerPhase start() {
            this.mData = new StringBuilder();
            return this;
        }

        private boolean replaceEscape(String escape, String replacement, int pos) {
            if (!Tokenizer.this.mLine.startsWith(escape, pos)) {
                return false;
            }
            this.mData.append(Tokenizer.this.mLine.substring(Tokenizer.this.mHandledLen, pos));
            this.mData.append(replacement);
            Tokenizer.this.mHandledLen = escape.length() + pos;
            int pos2 = Tokenizer.this.mHandledLen - 1;
            return true;
        }

        @Override
        public void tokenize() {
            int end = Tokenizer.this.mLine.length();
            int pos = Tokenizer.this.mHandledLen;
            while (true) {
                if (pos >= Tokenizer.this.mLine.length()) {
                    break;
                }
                if (Tokenizer.this.mLine.charAt(pos) != '&') {
                    if (Tokenizer.this.mLine.charAt(pos) == '<') {
                        end = pos;
                        Tokenizer.this.mPhase = Tokenizer.this.mTagTokenizer.start();
                        break;
                    }
                } else if (replaceEscape("&amp;", "&", pos) || replaceEscape("&lt;", "<", pos) || replaceEscape("&gt;", ">", pos) || replaceEscape("&lrm;", "\u200e", pos) || replaceEscape("&rlm;", "\u200f", pos) || replaceEscape("&nbsp;", " ", pos)) {
                }
                pos++;
            }
            this.mData.append(Tokenizer.this.mLine.substring(Tokenizer.this.mHandledLen, end));
            Tokenizer.this.mListener.onData(this.mData.toString());
            this.mData.delete(0, this.mData.length());
            Tokenizer.this.mHandledLen = end;
        }
    }

    class TagTokenizer implements TokenizerPhase {
        private String mAnnotation;
        private boolean mAtAnnotation;
        private String mName;

        TagTokenizer() {
        }

        @Override
        public TokenizerPhase start() {
            this.mAnnotation = ProxyInfo.LOCAL_EXCL_LIST;
            this.mName = ProxyInfo.LOCAL_EXCL_LIST;
            this.mAtAnnotation = false;
            return this;
        }

        @Override
        public void tokenize() {
            if (!this.mAtAnnotation) {
                Tokenizer.access$108(Tokenizer.this);
            }
            if (Tokenizer.this.mHandledLen < Tokenizer.this.mLine.length()) {
                String[] parts = (this.mAtAnnotation || Tokenizer.this.mLine.charAt(Tokenizer.this.mHandledLen) == '/') ? Tokenizer.this.mLine.substring(Tokenizer.this.mHandledLen).split(">") : Tokenizer.this.mLine.substring(Tokenizer.this.mHandledLen).split("[\t\f >]");
                String part = Tokenizer.this.mLine.substring(Tokenizer.this.mHandledLen, Tokenizer.this.mHandledLen + parts[0].length());
                Tokenizer.access$112(Tokenizer.this, parts[0].length());
                if (this.mAtAnnotation) {
                    this.mAnnotation += " " + part;
                } else {
                    this.mName = part;
                }
            }
            this.mAtAnnotation = true;
            if (Tokenizer.this.mHandledLen < Tokenizer.this.mLine.length() && Tokenizer.this.mLine.charAt(Tokenizer.this.mHandledLen) == '>') {
                yield_tag();
                Tokenizer.this.mPhase = Tokenizer.this.mDataTokenizer.start();
                Tokenizer.access$108(Tokenizer.this);
            }
        }

        private void yield_tag() {
            if (this.mName.startsWith("/")) {
                Tokenizer.this.mListener.onEnd(this.mName.substring(1));
                return;
            }
            if (this.mName.length() > 0 && Character.isDigit(this.mName.charAt(0))) {
                try {
                    long timestampMs = WebVttParser.parseTimestampMs(this.mName);
                    Tokenizer.this.mListener.onTimeStamp(timestampMs);
                    return;
                } catch (NumberFormatException e) {
                    Log.d(Tokenizer.TAG, "invalid timestamp tag: <" + this.mName + ">");
                    return;
                }
            }
            this.mAnnotation = this.mAnnotation.replaceAll("\\s+", " ");
            if (this.mAnnotation.startsWith(" ")) {
                this.mAnnotation = this.mAnnotation.substring(1);
            }
            if (this.mAnnotation.endsWith(" ")) {
                this.mAnnotation = this.mAnnotation.substring(0, this.mAnnotation.length() - 1);
            }
            String[] classes = null;
            int dotAt = this.mName.indexOf(46);
            if (dotAt >= 0) {
                classes = this.mName.substring(dotAt + 1).split("\\.");
                this.mName = this.mName.substring(0, dotAt);
            }
            Tokenizer.this.mListener.onStart(this.mName, classes, this.mAnnotation);
        }
    }

    Tokenizer(OnTokenListener listener) {
        reset();
        this.mListener = listener;
    }

    void reset() {
        this.mPhase = this.mDataTokenizer.start();
    }

    void tokenize(String s) {
        this.mHandledLen = 0;
        this.mLine = s;
        while (this.mHandledLen < this.mLine.length()) {
            this.mPhase.tokenize();
        }
        if (!(this.mPhase instanceof TagTokenizer)) {
            this.mListener.onLineEnd();
        }
    }
}
