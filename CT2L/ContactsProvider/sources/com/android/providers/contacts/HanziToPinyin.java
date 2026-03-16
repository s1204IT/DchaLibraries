package com.android.providers.contacts;

import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import libcore.icu.Transliterator;

public class HanziToPinyin {
    private static HanziToPinyin sInstance;
    private Transliterator mAsciiTransliterator;
    private Transliterator mPinyinTransliterator;

    public static class Token {
        public String source;
        public String target;
        public int type;

        public Token() {
        }

        public Token(int type, String source, String target) {
            this.type = type;
            this.source = source;
            this.target = target;
        }
    }

    private HanziToPinyin() {
        try {
            this.mPinyinTransliterator = new Transliterator("Han-Latin/Names; Latin-Ascii; Any-Upper");
            this.mAsciiTransliterator = new Transliterator("Latin-Ascii");
        } catch (RuntimeException e) {
            Log.w("HanziToPinyin", "Han-Latin/Names transliterator data is missing, HanziToPinyin is disabled");
        }
    }

    public boolean hasChineseTransliterator() {
        return this.mPinyinTransliterator != null;
    }

    public static HanziToPinyin getInstance() {
        HanziToPinyin hanziToPinyin;
        synchronized (HanziToPinyin.class) {
            if (sInstance == null) {
                sInstance = new HanziToPinyin();
            }
            hanziToPinyin = sInstance;
        }
        return hanziToPinyin;
    }

    private void tokenize(char character, Token token) {
        token.source = Character.toString(character);
        if (character < 128) {
            token.type = 1;
            token.target = token.source;
            return;
        }
        if (character < 592 || (7680 <= character && character < 7935)) {
            token.type = 1;
            token.target = this.mAsciiTransliterator == null ? token.source : this.mAsciiTransliterator.transliterate(token.source);
            return;
        }
        token.type = 2;
        token.target = this.mPinyinTransliterator.transliterate(token.source);
        if (TextUtils.isEmpty(token.target) || TextUtils.equals(token.source, token.target)) {
            token.type = 3;
            token.target = token.source;
        }
    }

    public String transliterate(String input) {
        if (!hasChineseTransliterator() || TextUtils.isEmpty(input)) {
            return null;
        }
        return this.mPinyinTransliterator.transliterate(input);
    }

    public ArrayList<Token> getTokens(String input) {
        ArrayList<Token> tokens = new ArrayList<>();
        if (hasChineseTransliterator() && !TextUtils.isEmpty(input)) {
            int inputLength = input.length();
            StringBuilder sb = new StringBuilder();
            int tokenType = 1;
            Token token = new Token();
            for (int i = 0; i < inputLength; i++) {
                char character = input.charAt(i);
                if (Character.isSpaceChar(character)) {
                    if (sb.length() > 0) {
                        addToken(sb, tokens, tokenType);
                    }
                } else {
                    tokenize(character, token);
                    if (token.type == 2) {
                        if (sb.length() > 0) {
                            addToken(sb, tokens, tokenType);
                        }
                        tokens.add(token);
                        token = new Token();
                    } else {
                        if (tokenType != token.type && sb.length() > 0) {
                            addToken(sb, tokens, tokenType);
                        }
                        sb.append(token.target);
                    }
                    tokenType = token.type;
                }
            }
            if (sb.length() > 0) {
                addToken(sb, tokens, tokenType);
            }
        }
        return tokens;
    }

    private void addToken(StringBuilder sb, ArrayList<Token> tokens, int tokenType) {
        String str = sb.toString();
        tokens.add(new Token(tokenType, str, str));
        sb.setLength(0);
    }
}
