package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.text.MessagePattern;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

public class SelectFormat extends Format {

    static final boolean f89assertionsDisabled;
    private static final long serialVersionUID = 2993154333257524984L;
    private transient MessagePattern msgPattern;
    private String pattern = null;

    static {
        f89assertionsDisabled = !SelectFormat.class.desiredAssertionStatus();
    }

    public SelectFormat(String pattern) {
        applyPattern(pattern);
    }

    private void reset() {
        this.pattern = null;
        if (this.msgPattern == null) {
            return;
        }
        this.msgPattern.clear();
    }

    public void applyPattern(String pattern) {
        this.pattern = pattern;
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        try {
            this.msgPattern.parseSelectStyle(pattern);
        } catch (RuntimeException e) {
            reset();
            throw e;
        }
    }

    public String toPattern() {
        return this.pattern;
    }

    static int findSubMessage(MessagePattern pattern, int partIndex, String keyword) {
        int count = pattern.countParts();
        int msgStart = 0;
        do {
            int partIndex2 = partIndex + 1;
            MessagePattern.Part part = pattern.getPart(partIndex);
            MessagePattern.Part.Type type = part.getType();
            if (type == MessagePattern.Part.Type.ARG_LIMIT) {
                break;
            }
            if (!f89assertionsDisabled) {
                if (!(type == MessagePattern.Part.Type.ARG_SELECTOR)) {
                    throw new AssertionError();
                }
            }
            if (pattern.partSubstringMatches(part, keyword)) {
                return partIndex2;
            }
            if (msgStart == 0 && pattern.partSubstringMatches(part, PluralRules.KEYWORD_OTHER)) {
                msgStart = partIndex2;
            }
            partIndex = pattern.getLimitPartIndex(partIndex2) + 1;
        } while (partIndex < count);
        return msgStart;
    }

    public final String format(String keyword) {
        int index;
        if (!PatternProps.isIdentifier(keyword)) {
            throw new IllegalArgumentException("Invalid formatting argument.");
        }
        if (this.msgPattern == null || this.msgPattern.countParts() == 0) {
            throw new IllegalStateException("Invalid format error.");
        }
        int msgStart = findSubMessage(this.msgPattern, 0, keyword);
        if (!this.msgPattern.jdkAposMode()) {
            int msgLimit = this.msgPattern.getLimitPartIndex(msgStart);
            return this.msgPattern.getPatternString().substring(this.msgPattern.getPart(msgStart).getLimit(), this.msgPattern.getPatternIndex(msgLimit));
        }
        StringBuilder result = null;
        int prevIndex = this.msgPattern.getPart(msgStart).getLimit();
        int i = msgStart;
        while (true) {
            i++;
            MessagePattern.Part part = this.msgPattern.getPart(i);
            MessagePattern.Part.Type type = part.getType();
            index = part.getIndex();
            if (type == MessagePattern.Part.Type.MSG_LIMIT) {
                break;
            }
            if (type == MessagePattern.Part.Type.SKIP_SYNTAX) {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append((CharSequence) this.pattern, prevIndex, index);
                prevIndex = part.getLimit();
            } else if (type == MessagePattern.Part.Type.ARG_START) {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append((CharSequence) this.pattern, prevIndex, index);
                i = this.msgPattern.getLimitPartIndex(i);
                int index2 = this.msgPattern.getPart(i).getLimit();
                MessagePattern.appendReducedApostrophes(this.pattern, index, index2, result);
                prevIndex = index2;
            }
        }
        if (result == null) {
            return this.pattern.substring(prevIndex, index);
        }
        return result.append((CharSequence) this.pattern, prevIndex, index).toString();
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (!(obj instanceof String)) {
            throw new IllegalArgumentException("'" + obj + "' is not a String");
        }
        toAppendTo.append(format((String) obj));
        return toAppendTo;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SelectFormat sf = (SelectFormat) obj;
        return this.msgPattern == null ? sf.msgPattern == null : this.msgPattern.equals(sf.msgPattern);
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

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        if (this.pattern == null) {
            return;
        }
        applyPattern(this.pattern);
    }
}
