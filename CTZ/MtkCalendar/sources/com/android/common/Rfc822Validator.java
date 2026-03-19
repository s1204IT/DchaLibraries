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

    public Rfc822Validator(String str) {
        this.mDomain = str;
    }

    @Override
    public boolean isValid(CharSequence charSequence) {
        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(charSequence);
        return rfc822TokenArr.length == 1 && EMAIL_ADDRESS_PATTERN.matcher(rfc822TokenArr[0].getAddress()).matches();
    }

    public void setRemoveInvalid(boolean z) {
        this.mRemoveInvalid = z;
    }

    private String removeIllegalCharacters(String str) {
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt > ' ' && cCharAt <= '~' && cCharAt != '(' && cCharAt != ')' && cCharAt != '<' && cCharAt != '>' && cCharAt != '@' && cCharAt != ',' && cCharAt != ';' && cCharAt != ':' && cCharAt != '\\' && cCharAt != '\"' && cCharAt != '[' && cCharAt != ']') {
                sb.append(cCharAt);
            }
        }
        return sb.toString();
    }

    @Override
    public CharSequence fixText(CharSequence charSequence) {
        if (TextUtils.getTrimmedLength(charSequence) == 0) {
            return "";
        }
        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(charSequence);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rfc822TokenArr.length; i++) {
            String address = rfc822TokenArr[i].getAddress();
            if (!this.mRemoveInvalid || isValid(address)) {
                int iIndexOf = address.indexOf(64);
                if (iIndexOf < 0) {
                    if (this.mDomain != null) {
                        rfc822TokenArr[i].setAddress(removeIllegalCharacters(address) + "@" + this.mDomain);
                    }
                } else {
                    String strRemoveIllegalCharacters = removeIllegalCharacters(address.substring(0, iIndexOf));
                    if (!TextUtils.isEmpty(strRemoveIllegalCharacters)) {
                        String strRemoveIllegalCharacters2 = removeIllegalCharacters(address.substring(iIndexOf + 1));
                        boolean z = strRemoveIllegalCharacters2.length() == 0;
                        if (!z || this.mDomain != null) {
                            Rfc822Token rfc822Token = rfc822TokenArr[i];
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append(strRemoveIllegalCharacters);
                            sb2.append("@");
                            if (z) {
                                strRemoveIllegalCharacters2 = this.mDomain;
                            }
                            sb2.append(strRemoveIllegalCharacters2);
                            rfc822Token.setAddress(sb2.toString());
                        }
                    }
                }
                sb.append(rfc822TokenArr[i].toString());
                if (i + 1 < rfc822TokenArr.length) {
                    sb.append(", ");
                }
            }
        }
        return sb;
    }
}
