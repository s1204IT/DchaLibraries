package jp.co.omronsoft.iwnnime.ml;

import com.android.common.speech.LoggingEvents;
import jp.co.omronsoft.android.emoji.EmojiAssist;

public class WnnWord {
    public int attribute;
    public String candidate;
    public EmojiAssist.DecoEmojiTextInfo decoEmojiInfo;
    public String hint;
    public int id;
    public int lexicalCategory;
    public String stroke;
    public int symbolMode;

    public WnnWord() {
        this(0, LoggingEvents.EXTRA_CALLING_APP_NAME, LoggingEvents.EXTRA_CALLING_APP_NAME, LoggingEvents.EXTRA_CALLING_APP_NAME, 0, 0, null);
    }

    public WnnWord(String candidate, String stroke) {
        this(0, candidate, stroke, LoggingEvents.EXTRA_CALLING_APP_NAME, 0, 0, null);
    }

    public WnnWord(int id, String candidate, String stroke) {
        this(id, candidate, stroke, LoggingEvents.EXTRA_CALLING_APP_NAME, 0, 0, null);
    }

    public WnnWord(int id, String candidate, String stroke, int attribute) {
        this(id, candidate, stroke, LoggingEvents.EXTRA_CALLING_APP_NAME, attribute, 0, null);
    }

    public WnnWord(int id, String candidate, String stroke, int attribute, int lexicalCategory) {
        this(id, candidate, stroke, LoggingEvents.EXTRA_CALLING_APP_NAME, attribute, lexicalCategory, null);
    }

    public WnnWord(int id, String candidate, String stroke, String hint, int attribute, int lexicalCategory, EmojiAssist.DecoEmojiTextInfo decoEmojiInfo) {
        this.symbolMode = -1;
        this.id = id;
        this.candidate = candidate;
        this.stroke = stroke;
        this.hint = hint;
        this.attribute = attribute;
        this.lexicalCategory = lexicalCategory;
        this.decoEmojiInfo = decoEmojiInfo;
    }

    public int getSymbolMode() {
        return this.symbolMode;
    }

    public void setSymbolMode(int symbolMode) {
        this.symbolMode = symbolMode;
    }
}
