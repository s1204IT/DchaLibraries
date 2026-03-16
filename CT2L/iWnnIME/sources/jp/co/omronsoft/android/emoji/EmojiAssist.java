package jp.co.omronsoft.android.emoji;

import android.content.Context;
import android.inputmethodservice.ExtractEditText;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.common.speech.LoggingEvents;

public class EmojiAssist {
    public static final char DECO_EMOJI_CHAR = 12307;
    public static final String DECO_EMOJI_KEY = "decoEmoji";
    public static final int EMOJITYPE_COMB_ANY = 15;
    public static final int EMOJITYPE_COMB_PICTD = 7;
    public static final int EMOJITYPE_COMB_SQ = 3;
    public static final int EMOJITYPE_PICTD_RECT = 4;
    public static final int EMOJITYPE_PICTD_SQ20 = 1;
    public static final int EMOJITYPE_PICTD_SQNOT20 = 2;
    public static final int EMOJITYPE_PICTURE = 8;
    public static final String SPLIT_KEY = "\t";
    public static final int TYPE_TEXT_BOTH = 3;
    public static final int TYPE_TEXT_DECOEMOJI = 2;
    public static final int TYPE_TEXT_EMOJI = 1;
    public static final int TYPE_TEXT_NORMAL = 0;
    private static EmojiAssist mSelf = new EmojiAssist();

    public static class DecoEmojiTextInfo {
        private String mUri = LoggingEvents.EXTRA_CALLING_APP_NAME;
        private int mEmojiType = 0;
        private Context mContext = null;
        private int mWidth = 0;
        private int mHeight = 0;
        private int mKind = 0;
        private int mPop = 0;

        public void setUri(String uri) {
            this.mUri = uri;
        }

        public void setEmojiType(int emojiType) {
            this.mEmojiType = emojiType;
        }

        public void setContext(Context context) {
            this.mContext = context;
        }

        public void setWidth(int width) {
            this.mWidth = width;
        }

        public void setHeight(int height) {
            this.mHeight = height;
        }

        public void setKind(int kind) {
            this.mKind = kind;
        }

        public void setPop(int pop) {
            this.mPop = pop;
        }

        public String getUri() {
            return this.mUri;
        }

        public int getEmojiType() {
            return this.mEmojiType;
        }

        public Context getContext() {
            return this.mContext;
        }

        public int getWidth() {
            return this.mWidth;
        }

        public int getHeight() {
            return this.mHeight;
        }

        public int getKind() {
            return this.mKind;
        }

        public int getPop() {
            return this.mPop;
        }
    }

    private static class UpdateHandler extends Handler {
        public UpdateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
        }
    }

    private EmojiAssist() {
    }

    public static EmojiAssist getInstance() {
        return mSelf;
    }

    public void addView(TextView view) {
    }

    public void addView(TextView view, boolean scale) {
    }

    public void removeView(TextView view) {
    }

    public void clearView() {
    }

    public void startAnimation() {
    }

    public void stopAnimation() {
    }

    public void undressEmojiSpan(Spannable text, int from, int to, boolean annotation) {
    }

    public void dressEmojiSpan(Spannable text, int from, int to, float textSize) {
    }

    private void setVendorEmojiSpan(Spannable text, int start) {
    }

    public CharSequence getDecoEmojiText(String uri) {
        return null;
    }

    public CharSequence getDecoEmojiText(String uri, Context context) {
        return null;
    }

    public CharSequence getDecoEmojiText(DecoEmojiTextInfo info) {
        return null;
    }

    public int getEmojiFunctionType() {
        return 0;
    }

    public ExtractEditText replaceExtractEditText(ViewGroup view, int id) {
        return null;
    }

    public byte[] getEmojiImage(int code, float textsize) {
        return null;
    }

    public int checkTextData(Spannable text) {
        return 0;
    }

    public void setDisplayPop(boolean display) {
    }

    public boolean getDisplayPop() {
        return false;
    }

    public void setPictureScale(boolean scale) {
    }
}
