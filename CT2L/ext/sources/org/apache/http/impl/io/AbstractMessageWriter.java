package org.apache.http.impl.io;

import java.io.IOException;
import java.util.Iterator;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.io.HttpMessageWriter;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.message.BasicLineFormatter;
import org.apache.http.message.LineFormatter;
import org.apache.http.params.HttpParams;
import org.apache.http.util.CharArrayBuffer;
import org.ccil.cowan.tagsoup.HTMLModels;

@Deprecated
public abstract class AbstractMessageWriter implements HttpMessageWriter {
    protected final CharArrayBuffer lineBuf;
    protected final LineFormatter lineFormatter;
    protected final SessionOutputBuffer sessionBuffer;

    protected abstract void writeHeadLine(HttpMessage httpMessage) throws IOException;

    public AbstractMessageWriter(SessionOutputBuffer buffer, LineFormatter formatter, HttpParams params) {
        if (buffer == null) {
            throw new IllegalArgumentException("Session input buffer may not be null");
        }
        this.sessionBuffer = buffer;
        this.lineBuf = new CharArrayBuffer(HTMLModels.M_DEF);
        this.lineFormatter = formatter == null ? BasicLineFormatter.DEFAULT : formatter;
    }

    @Override
    public void write(HttpMessage message) throws HttpException, IOException {
        if (message == null) {
            throw new IllegalArgumentException("HTTP message may not be null");
        }
        writeHeadLine(message);
        Iterator it = message.headerIterator();
        while (it.hasNext()) {
            Header header = (Header) it.next();
            this.sessionBuffer.writeLine(this.lineFormatter.formatHeader(this.lineBuf, header));
        }
        this.lineBuf.clear();
        this.sessionBuffer.writeLine(this.lineBuf);
    }
}
