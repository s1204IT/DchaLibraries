package android.text.util;

import com.android.internal.logging.nano.MetricsProto;

public class Rfc822Token {
    private String mAddress;
    private String mComment;
    private String mName;

    public Rfc822Token(String str, String str2, String str3) {
        this.mName = str;
        this.mAddress = str2;
        this.mComment = str3;
    }

    public String getName() {
        return this.mName;
    }

    public String getAddress() {
        return this.mAddress;
    }

    public String getComment() {
        return this.mComment;
    }

    public void setName(String str) {
        this.mName = str;
    }

    public void setAddress(String str) {
        this.mAddress = str;
    }

    public void setComment(String str) {
        this.mComment = str;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.mName != null && this.mName.length() != 0) {
            sb.append(quoteNameIfNecessary(this.mName));
            sb.append(' ');
        }
        if (this.mComment != null && this.mComment.length() != 0) {
            sb.append('(');
            sb.append(quoteComment(this.mComment));
            sb.append(") ");
        }
        if (this.mAddress != null && this.mAddress.length() != 0) {
            sb.append('<');
            sb.append(this.mAddress);
            sb.append('>');
        }
        return sb.toString();
    }

    public static String quoteNameIfNecessary(String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if ((cCharAt < 'A' || cCharAt > 'Z') && ((cCharAt < 'a' || cCharAt > 'z') && cCharAt != ' ' && (cCharAt < '0' || cCharAt > '9'))) {
                return '\"' + quoteName(str) + '\"';
            }
        }
        return str;
    }

    public static String quoteName(String str) {
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\\' || cCharAt == '\"') {
                sb.append('\\');
            }
            sb.append(cCharAt);
        }
        return sb.toString();
    }

    public static String quoteComment(String str) {
        int length = str.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '(' || cCharAt == ')' || cCharAt == '\\') {
                sb.append('\\');
            }
            sb.append(cCharAt);
        }
        return sb.toString();
    }

    public int hashCode() {
        int iHashCode = this.mName != null ? MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + this.mName.hashCode() : 17;
        if (this.mAddress != null) {
            iHashCode = (iHashCode * 31) + this.mAddress.hashCode();
        }
        if (this.mComment != null) {
            return this.mComment.hashCode() + (31 * iHashCode);
        }
        return iHashCode;
    }

    private static boolean stringEquals(String str, String str2) {
        if (str == null) {
            return str2 == null;
        }
        return str.equals(str2);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Rfc822Token)) {
            return false;
        }
        Rfc822Token rfc822Token = (Rfc822Token) obj;
        return stringEquals(this.mName, rfc822Token.mName) && stringEquals(this.mAddress, rfc822Token.mAddress) && stringEquals(this.mComment, rfc822Token.mComment);
    }
}
