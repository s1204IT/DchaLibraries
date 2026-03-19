package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.text.MessagePattern;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

public class SelectFormat extends Format {
    static final boolean $assertionsDisabled = false;
    private static final long serialVersionUID = 2993154333257524984L;
    private transient MessagePattern msgPattern;
    private String pattern = null;

    public SelectFormat(String str) {
        applyPattern(str);
    }

    private void reset() {
        this.pattern = null;
        if (this.msgPattern != null) {
            this.msgPattern.clear();
        }
    }

    public void applyPattern(String str) {
        this.pattern = str;
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        try {
            this.msgPattern.parseSelectStyle(str);
        } catch (RuntimeException e) {
            reset();
            throw e;
        }
    }

    public String toPattern() {
        return this.pattern;
    }

    static int findSubMessage(MessagePattern messagePattern, int i, String str) {
        int iCountParts = messagePattern.countParts();
        int i2 = 0;
        do {
            int i3 = i + 1;
            MessagePattern.Part part = messagePattern.getPart(i);
            if (part.getType() == MessagePattern.Part.Type.ARG_LIMIT) {
                break;
            }
            if (messagePattern.partSubstringMatches(part, str)) {
                return i3;
            }
            if (i2 == 0 && messagePattern.partSubstringMatches(part, PluralRules.KEYWORD_OTHER)) {
                i2 = i3;
            }
            i = messagePattern.getLimitPartIndex(i3) + 1;
        } while (i < iCountParts);
        return i2;
    }

    public final String format(String str) {
        int index;
        if (!PatternProps.isIdentifier(str)) {
            throw new IllegalArgumentException("Invalid formatting argument.");
        }
        if (this.msgPattern == null || this.msgPattern.countParts() == 0) {
            throw new IllegalStateException("Invalid format error.");
        }
        int iFindSubMessage = findSubMessage(this.msgPattern, 0, str);
        if (!this.msgPattern.jdkAposMode()) {
            return this.msgPattern.getPatternString().substring(this.msgPattern.getPart(iFindSubMessage).getLimit(), this.msgPattern.getPatternIndex(this.msgPattern.getLimitPartIndex(iFindSubMessage)));
        }
        StringBuilder sb = null;
        int limit = this.msgPattern.getPart(iFindSubMessage).getLimit();
        while (true) {
            iFindSubMessage++;
            MessagePattern.Part part = this.msgPattern.getPart(iFindSubMessage);
            MessagePattern.Part.Type type = part.getType();
            index = part.getIndex();
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                break;
            }
            if (type == MessagePattern.Part.Type.SKIP_SYNTAX) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append((CharSequence) this.pattern, limit, index);
                limit = part.getLimit();
            } else if (type == MessagePattern.Part.Type.ARG_START) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append((CharSequence) this.pattern, limit, index);
                iFindSubMessage = this.msgPattern.getLimitPartIndex(iFindSubMessage);
                limit = this.msgPattern.getPart(iFindSubMessage).getLimit();
                MessagePattern.appendReducedApostrophes(this.pattern, index, limit, sb);
            }
        }
        if (sb == null) {
            return this.pattern.substring(limit, index);
        }
        sb.append((CharSequence) this.pattern, limit, index);
        return sb.toString();
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (obj instanceof String) {
            stringBuffer.append(format((String) obj));
            return stringBuffer;
        }
        throw new IllegalArgumentException("'" + obj + "' is not a String");
    }

    @Override
    public Object parseObject(String str, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SelectFormat selectFormat = (SelectFormat) obj;
        if (this.msgPattern != null) {
            return this.msgPattern.equals(selectFormat.msgPattern);
        }
        if (selectFormat.msgPattern == null) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        if (this.pattern != null) {
            return this.pattern.hashCode();
        }
        return 0;
    }

    public String toString() {
        return "pattern='" + this.pattern + "'";
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        if (this.pattern != null) {
            applyPattern(this.pattern);
        }
    }
}
