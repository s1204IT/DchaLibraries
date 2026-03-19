package org.apache.http.message;

import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class BasicLineParser implements LineParser {
    public static final BasicLineParser DEFAULT = new BasicLineParser();
    protected final ProtocolVersion protocol;

    public BasicLineParser(ProtocolVersion protocolVersion) {
        this.protocol = protocolVersion == null ? HttpVersion.HTTP_1_1 : protocolVersion;
    }

    public BasicLineParser() {
        this(null);
    }

    public static final ProtocolVersion parseProtocolVersion(String str, LineParser lineParser) throws ParseException {
        if (str == null) {
            throw new IllegalArgumentException("Value to parse may not be null.");
        }
        if (lineParser == null) {
            lineParser = DEFAULT;
        }
        CharArrayBuffer charArrayBuffer = new CharArrayBuffer(str.length());
        charArrayBuffer.append(str);
        return lineParser.parseProtocolVersion(charArrayBuffer, new ParserCursor(0, str.length()));
    }

    @Override
    public ProtocolVersion parseProtocolVersion(CharArrayBuffer charArrayBuffer, ParserCursor parserCursor) throws ParseException {
        if (charArrayBuffer == null) {
            throw new IllegalArgumentException("Char array buffer may not be null");
        }
        if (parserCursor == null) {
            throw new IllegalArgumentException("Parser cursor may not be null");
        }
        String protocol = this.protocol.getProtocol();
        int length = protocol.length();
        int pos = parserCursor.getPos();
        int upperBound = parserCursor.getUpperBound();
        skipWhitespace(charArrayBuffer, parserCursor);
        int pos2 = parserCursor.getPos();
        int i = pos2 + length;
        if (i + 4 > upperBound) {
            throw new ParseException("Not a valid protocol version: " + charArrayBuffer.substring(pos, upperBound));
        }
        boolean z = true;
        for (int i2 = 0; z && i2 < length; i2++) {
            z = charArrayBuffer.charAt(pos2 + i2) == protocol.charAt(i2);
        }
        if (z) {
            z = charArrayBuffer.charAt(i) == '/';
        }
        if (!z) {
            throw new ParseException("Not a valid protocol version: " + charArrayBuffer.substring(pos, upperBound));
        }
        int i3 = pos2 + length + 1;
        int iIndexOf = charArrayBuffer.indexOf(46, i3, upperBound);
        if (iIndexOf == -1) {
            throw new ParseException("Invalid protocol version number: " + charArrayBuffer.substring(pos, upperBound));
        }
        try {
            int i4 = Integer.parseInt(charArrayBuffer.substringTrimmed(i3, iIndexOf));
            int i5 = iIndexOf + 1;
            int iIndexOf2 = charArrayBuffer.indexOf(32, i5, upperBound);
            if (iIndexOf2 == -1) {
                iIndexOf2 = upperBound;
            }
            try {
                int i6 = Integer.parseInt(charArrayBuffer.substringTrimmed(i5, iIndexOf2));
                parserCursor.updatePos(iIndexOf2);
                return createProtocolVersion(i4, i6);
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid protocol minor version number: " + charArrayBuffer.substring(pos, upperBound));
            }
        } catch (NumberFormatException e2) {
            throw new ParseException("Invalid protocol major version number: " + charArrayBuffer.substring(pos, upperBound));
        }
    }

    protected ProtocolVersion createProtocolVersion(int i, int i2) {
        return this.protocol.forVersion(i, i2);
    }

    @Override
    public boolean hasProtocolVersion(CharArrayBuffer charArrayBuffer, ParserCursor parserCursor) {
        if (charArrayBuffer == null) {
            throw new IllegalArgumentException("Char array buffer may not be null");
        }
        if (parserCursor == null) {
            throw new IllegalArgumentException("Parser cursor may not be null");
        }
        int pos = parserCursor.getPos();
        String protocol = this.protocol.getProtocol();
        int length = protocol.length();
        if (charArrayBuffer.length() < length + 4) {
            return false;
        }
        if (pos < 0) {
            pos = (charArrayBuffer.length() - 4) - length;
        } else if (pos == 0) {
            while (pos < charArrayBuffer.length() && HTTP.isWhitespace(charArrayBuffer.charAt(pos))) {
                pos++;
            }
        }
        int i = pos + length;
        if (i + 4 > charArrayBuffer.length()) {
            return false;
        }
        boolean z = true;
        for (int i2 = 0; z && i2 < length; i2++) {
            z = charArrayBuffer.charAt(pos + i2) == protocol.charAt(i2);
        }
        return z ? charArrayBuffer.charAt(i) == '/' : z;
    }

    public static final RequestLine parseRequestLine(String str, LineParser lineParser) throws ParseException {
        if (str == null) {
            throw new IllegalArgumentException("Value to parse may not be null.");
        }
        if (lineParser == null) {
            lineParser = DEFAULT;
        }
        CharArrayBuffer charArrayBuffer = new CharArrayBuffer(str.length());
        charArrayBuffer.append(str);
        return lineParser.parseRequestLine(charArrayBuffer, new ParserCursor(0, str.length()));
    }

    @Override
    public RequestLine parseRequestLine(CharArrayBuffer charArrayBuffer, ParserCursor parserCursor) throws ParseException {
        if (charArrayBuffer == null) {
            throw new IllegalArgumentException("Char array buffer may not be null");
        }
        if (parserCursor == null) {
            throw new IllegalArgumentException("Parser cursor may not be null");
        }
        int pos = parserCursor.getPos();
        int upperBound = parserCursor.getUpperBound();
        try {
            skipWhitespace(charArrayBuffer, parserCursor);
            int pos2 = parserCursor.getPos();
            int iIndexOf = charArrayBuffer.indexOf(32, pos2, upperBound);
            if (iIndexOf < 0) {
                throw new ParseException("Invalid request line: " + charArrayBuffer.substring(pos, upperBound));
            }
            String strSubstringTrimmed = charArrayBuffer.substringTrimmed(pos2, iIndexOf);
            parserCursor.updatePos(iIndexOf);
            skipWhitespace(charArrayBuffer, parserCursor);
            int pos3 = parserCursor.getPos();
            int iIndexOf2 = charArrayBuffer.indexOf(32, pos3, upperBound);
            if (iIndexOf2 < 0) {
                throw new ParseException("Invalid request line: " + charArrayBuffer.substring(pos, upperBound));
            }
            String strSubstringTrimmed2 = charArrayBuffer.substringTrimmed(pos3, iIndexOf2);
            parserCursor.updatePos(iIndexOf2);
            ProtocolVersion protocolVersion = parseProtocolVersion(charArrayBuffer, parserCursor);
            skipWhitespace(charArrayBuffer, parserCursor);
            if (!parserCursor.atEnd()) {
                throw new ParseException("Invalid request line: " + charArrayBuffer.substring(pos, upperBound));
            }
            return createRequestLine(strSubstringTrimmed, strSubstringTrimmed2, protocolVersion);
        } catch (IndexOutOfBoundsException e) {
            throw new ParseException("Invalid request line: " + charArrayBuffer.substring(pos, upperBound));
        }
    }

    protected RequestLine createRequestLine(String str, String str2, ProtocolVersion protocolVersion) {
        return new BasicRequestLine(str, str2, protocolVersion);
    }

    public static final StatusLine parseStatusLine(String str, LineParser lineParser) throws ParseException {
        if (str == null) {
            throw new IllegalArgumentException("Value to parse may not be null.");
        }
        if (lineParser == null) {
            lineParser = DEFAULT;
        }
        CharArrayBuffer charArrayBuffer = new CharArrayBuffer(str.length());
        charArrayBuffer.append(str);
        return lineParser.parseStatusLine(charArrayBuffer, new ParserCursor(0, str.length()));
    }

    @Override
    public StatusLine parseStatusLine(CharArrayBuffer charArrayBuffer, ParserCursor parserCursor) throws ParseException {
        String strSubstringTrimmed;
        if (charArrayBuffer == null) {
            throw new IllegalArgumentException("Char array buffer may not be null");
        }
        if (parserCursor == null) {
            throw new IllegalArgumentException("Parser cursor may not be null");
        }
        int pos = parserCursor.getPos();
        int upperBound = parserCursor.getUpperBound();
        try {
            ProtocolVersion protocolVersion = parseProtocolVersion(charArrayBuffer, parserCursor);
            skipWhitespace(charArrayBuffer, parserCursor);
            int pos2 = parserCursor.getPos();
            int iIndexOf = charArrayBuffer.indexOf(32, pos2, upperBound);
            if (iIndexOf < 0) {
                iIndexOf = upperBound;
            }
            try {
                int i = Integer.parseInt(charArrayBuffer.substringTrimmed(pos2, iIndexOf));
                if (iIndexOf < upperBound) {
                    strSubstringTrimmed = charArrayBuffer.substringTrimmed(iIndexOf, upperBound);
                } else {
                    strSubstringTrimmed = "";
                }
                return createStatusLine(protocolVersion, i, strSubstringTrimmed);
            } catch (NumberFormatException e) {
                throw new ParseException("Unable to parse status code from status line: " + charArrayBuffer.substring(pos, upperBound));
            }
        } catch (IndexOutOfBoundsException e2) {
            throw new ParseException("Invalid status line: " + charArrayBuffer.substring(pos, upperBound));
        }
    }

    protected StatusLine createStatusLine(ProtocolVersion protocolVersion, int i, String str) {
        return new BasicStatusLine(protocolVersion, i, str);
    }

    public static final Header parseHeader(String str, LineParser lineParser) throws ParseException {
        if (str == null) {
            throw new IllegalArgumentException("Value to parse may not be null");
        }
        if (lineParser == null) {
            lineParser = DEFAULT;
        }
        CharArrayBuffer charArrayBuffer = new CharArrayBuffer(str.length());
        charArrayBuffer.append(str);
        return lineParser.parseHeader(charArrayBuffer);
    }

    @Override
    public Header parseHeader(CharArrayBuffer charArrayBuffer) throws ParseException {
        return new BufferedHeader(charArrayBuffer);
    }

    protected void skipWhitespace(CharArrayBuffer charArrayBuffer, ParserCursor parserCursor) {
        int pos = parserCursor.getPos();
        int upperBound = parserCursor.getUpperBound();
        while (pos < upperBound && HTTP.isWhitespace(charArrayBuffer.charAt(pos))) {
            pos++;
        }
        parserCursor.updatePos(pos);
    }
}
