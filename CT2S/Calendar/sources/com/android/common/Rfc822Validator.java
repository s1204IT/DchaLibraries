package com.android.common;

import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.widget.AutoCompleteTextView;
import java.util.regex.Pattern;

@Deprecated
public class Rfc822Validator implements AutoCompleteTextView.Validator {
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile("((?!\\s)[\\.\\w!#$%&'*+\\-/=?^`{|}~\u0080-\ufffe])+@(([a-zA-Z0-9 -\ud7ff豈-﷏ﷰ-\uffef][a-zA-Z0-9 -\ud7ff豈-﷏ﷰ-\uffef\\-]{0,61})?[a-zA-Z0-9 -\ud7ff豈-﷏ﷰ-\uffef]\\.)+[a-zA-Z0-9 -\ud7ff豈-﷏ﷰ-\uffef][a-zA-Z0-9 -\ud7ff豈-﷏ﷰ-\uffef\\-]{0,61}[a-zA-Z0-9 -\ud7ff豈-﷏ﷰ-\uffef]");
    private String mDomain;
    private boolean mRemoveInvalid = false;

    public Rfc822Validator(String domain) {
        this.mDomain = domain;
    }

    @Override
    public boolean isValid(CharSequence text) {
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(text);
        return tokens.length == 1 && EMAIL_ADDRESS_PATTERN.matcher(tokens[0].getAddress()).matches();
    }

    public void setRemoveInvalid(boolean remove) {
        this.mRemoveInvalid = remove;
    }

    private String removeIllegalCharacters(String s) {
        StringBuilder result = new StringBuilder();
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c > ' ' && c <= '~' && c != '(' && c != ')' && c != '<' && c != '>' && c != '@' && c != ',' && c != ';' && c != ':' && c != '\\' && c != '\"' && c != '[' && c != ']') {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Override
    public CharSequence fixText(CharSequence cs) {
        if (TextUtils.getTrimmedLength(cs) == 0) {
            return "";
        }
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(cs);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String text = tokens[i].getAddress();
            if (!this.mRemoveInvalid || isValid(text)) {
                int index = text.indexOf(64);
                if (index < 0) {
                    if (this.mDomain != null) {
                        tokens[i].setAddress(removeIllegalCharacters(text) + "@" + this.mDomain);
                    }
                } else {
                    String fix = removeIllegalCharacters(text.substring(0, index));
                    if (!TextUtils.isEmpty(fix)) {
                        String domain = removeIllegalCharacters(text.substring(index + 1));
                        boolean emptyDomain = domain.length() == 0;
                        if (!emptyDomain || this.mDomain != null) {
                            Rfc822Token rfc822Token = tokens[i];
                            StringBuilder sbAppend = new StringBuilder().append(fix).append("@");
                            if (emptyDomain) {
                                domain = this.mDomain;
                            }
                            rfc822Token.setAddress(sbAppend.append(domain).toString());
                        }
                    }
                }
                sb.append(tokens[i].toString());
                if (i + 1 < tokens.length) {
                    sb.append(", ");
                }
            }
        }
        return sb;
    }
}
