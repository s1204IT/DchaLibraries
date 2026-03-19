package org.apache.http.message;

import org.apache.http.FormattedHeader;
import org.apache.http.HeaderElement;
import org.apache.http.ParseException;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class BufferedHeader implements FormattedHeader, Cloneable {
    private final CharArrayBuffer buffer;
    private final String name;
    private final int valuePos;

    public BufferedHeader(CharArrayBuffer charArrayBuffer) throws ParseException {
        if (charArrayBuffer == null) {
            throw new IllegalArgumentException("Char array buffer may not be null");
        }
        int iIndexOf = charArrayBuffer.indexOf(58);
        if (iIndexOf == -1) {
            throw new ParseException("Invalid header: " + charArrayBuffer.toString());
        }
        String strSubstringTrimmed = charArrayBuffer.substringTrimmed(0, iIndexOf);
        if (strSubstringTrimmed.length() == 0) {
            throw new ParseException("Invalid header: " + charArrayBuffer.toString());
        }
        this.buffer = charArrayBuffer;
        this.name = strSubstringTrimmed;
        this.valuePos = iIndexOf + 1;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getValue() {
        return this.buffer.substringTrimmed(this.valuePos, this.buffer.length());
    }

    @Override
    public HeaderElement[] getElements() throws ParseException {
        ParserCursor parserCursor = new ParserCursor(0, this.buffer.length());
        parserCursor.updatePos(this.valuePos);
        return BasicHeaderValueParser.DEFAULT.parseElements(this.buffer, parserCursor);
    }

    @Override
    public int getValuePos() {
        return this.valuePos;
    }

    @Override
    public CharArrayBuffer getBuffer() {
        return this.buffer;
    }

    public String toString() {
        return this.buffer.toString();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
