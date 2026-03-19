package org.apache.http.impl.io;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.ParseException;
import org.apache.http.ProtocolException;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.params.HttpParams;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public abstract class AbstractMessageParser implements HttpMessageParser {
    protected final LineParser lineParser;
    private final int maxHeaderCount;
    private final int maxLineLen;
    private final SessionInputBuffer sessionBuffer;

    protected abstract HttpMessage parseHead(SessionInputBuffer sessionInputBuffer) throws HttpException, ParseException, IOException;

    public AbstractMessageParser(SessionInputBuffer buffer, LineParser parser, HttpParams params) {
        if (buffer == null) {
            throw new IllegalArgumentException("Session input buffer may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.sessionBuffer = buffer;
        this.maxHeaderCount = params.getIntParameter("http.connection.max-header-count", -1);
        this.maxLineLen = params.getIntParameter("http.connection.max-line-length", -1);
        this.lineParser = parser == null ? BasicLineParser.DEFAULT : parser;
    }

    public static Header[] parseHeaders(SessionInputBuffer inbuffer, int maxHeaderCount, int maxLineLen, LineParser parser) throws HttpException, IOException {
        char ch;
        if (inbuffer == null) {
            throw new IllegalArgumentException("Session input buffer may not be null");
        }
        if (parser == null) {
            parser = BasicLineParser.DEFAULT;
        }
        ArrayList headerLines = new ArrayList();
        CharArrayBuffer current = null;
        CharArrayBuffer charArrayBuffer = null;
        while (true) {
            if (current == null) {
                current = new CharArrayBuffer(64);
            } else {
                current.clear();
            }
            int l = inbuffer.readLine(current);
            if (l == -1 || current.length() < 1) {
                break;
            }
            if ((current.charAt(0) == ' ' || current.charAt(0) == '\t') && charArrayBuffer != null) {
                int i = 0;
                while (i < current.length() && ((ch = current.charAt(i)) == ' ' || ch == '\t')) {
                    i++;
                }
                if (maxLineLen > 0 && ((charArrayBuffer.length() + 1) + current.length()) - i > maxLineLen) {
                    throw new IOException("Maximum line length limit exceeded");
                }
                charArrayBuffer.append(' ');
                charArrayBuffer.append(current, i, current.length() - i);
            } else {
                headerLines.add(current);
                charArrayBuffer = current;
                current = null;
            }
            if (maxHeaderCount > 0 && headerLines.size() >= maxHeaderCount) {
                throw new IOException("Maximum header count exceeded");
            }
        }
    }

    @Override
    public HttpMessage parse() throws HttpException, IOException {
        try {
            HttpMessage message = parseHead(this.sessionBuffer);
            Header[] headers = parseHeaders(this.sessionBuffer, this.maxHeaderCount, this.maxLineLen, this.lineParser);
            message.setHeaders(headers);
            return message;
        } catch (ParseException px) {
            throw new ProtocolException(px.getMessage(), px);
        }
    }
}
