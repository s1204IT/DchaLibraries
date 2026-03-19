package com.android.mtkex.chips;

import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;

public class MTKRecipient {
    private long mContactId;
    private long mDataId;
    private String mDestination;
    private String mDisplayName;
    private String mFormatString;

    public MTKRecipient() {
        this.mContactId = -3L;
        this.mDataId = -3L;
        this.mDisplayName = "";
        this.mDestination = "";
    }

    public MTKRecipient(String str, String str2) {
        this(-1L, -1L, str, str2);
    }

    public MTKRecipient(long j, long j2, String str, String str2) {
        this.mContactId = j;
        this.mDataId = j2;
        this.mDisplayName = str;
        this.mDestination = str2;
    }

    public String getDisplayName() {
        return this.mDisplayName;
    }

    public String getDestination() {
        if (this.mContactId == -2) {
            return tokenizeDestination(this.mDestination);
        }
        return this.mDestination;
    }

    public String getFormatString() {
        if (this.mFormatString != null) {
            return this.mFormatString;
        }
        String str = "";
        if (this.mDestination != null && !textIsAllBlank(this.mDestination)) {
            if (this.mDisplayName != null && !textIsAllBlank(this.mDisplayName)) {
                String strReplaceAll = this.mDisplayName.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").replaceAll("\n", " ");
                if (strReplaceAll.matches(".*[\\(\\)<>@,;:\\\\\".\\[\\]].*") && !strReplaceAll.matches("^\".*\"$")) {
                    strReplaceAll = "\"" + strReplaceAll + "\"";
                }
                str = strReplaceAll + " <" + this.mDestination + ">, ";
            } else {
                str = this.mDestination + ", ";
            }
        }
        this.mFormatString = str;
        return str;
    }

    private String tokenizeDestination(String str) {
        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(str);
        if (rfc822TokenArr != null && rfc822TokenArr.length > 0) {
            return rfc822TokenArr[0].getAddress();
        }
        return str;
    }

    private boolean textIsAllBlank(String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }
}
