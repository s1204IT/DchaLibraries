package java.text;

import java.io.Serializable;
import java.text.AttributedCharacterIterator;

public abstract class Format implements Serializable, Cloneable {
    private static final long serialVersionUID = -299282585814624189L;

    public abstract StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition);

    public abstract Object parseObject(String str, ParsePosition parsePosition);

    protected Format() {
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public final String format(Object object) {
        return format(object, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object object) {
        return new AttributedString(format(object)).getIterator();
    }

    public Object parseObject(String string) throws ParseException {
        ParsePosition position = new ParsePosition(0);
        Object result = parseObject(string, position);
        if (position.getIndex() == 0) {
            throw new ParseException("Parse failure", position.getErrorIndex());
        }
        return result;
    }

    static boolean upTo(String string, ParsePosition position, StringBuffer buffer, char stop) {
        int index = position.getIndex();
        int length = string.length();
        boolean lastQuote = false;
        boolean quote = false;
        int index2 = index;
        while (index2 < length) {
            int index3 = index2 + 1;
            char ch = string.charAt(index2);
            if (ch == '\'') {
                if (lastQuote) {
                    buffer.append('\'');
                }
                quote = !quote;
                lastQuote = true;
            } else {
                if (ch == stop && !quote) {
                    position.setIndex(index3);
                    return true;
                }
                lastQuote = false;
                buffer.append(ch);
            }
            index2 = index3;
        }
        position.setIndex(index2);
        return false;
    }

    static boolean upToWithQuotes(String string, ParsePosition position, StringBuffer buffer, char stop, char start) {
        int index = position.getIndex();
        int length = string.length();
        int count = 1;
        boolean quote = false;
        int index2 = index;
        while (index2 < length) {
            int index3 = index2 + 1;
            char ch = string.charAt(index2);
            if (ch == '\'') {
                quote = !quote;
            }
            if (!quote) {
                if (ch == stop) {
                    count--;
                }
                if (count == 0) {
                    position.setIndex(index3);
                    return true;
                }
                if (ch == start) {
                    count++;
                }
            }
            buffer.append(ch);
            index2 = index3;
        }
        throw new IllegalArgumentException("Unmatched braces in the pattern");
    }

    public static class Field extends AttributedCharacterIterator.Attribute {
        private static final long serialVersionUID = 276966692217360283L;

        protected Field(String fieldName) {
            super(fieldName);
        }
    }
}
