package org.apache.http.message;

import java.util.NoSuchElementException;
import org.apache.http.FormattedHeader;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HeaderIterator;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class BasicHeaderElementIterator implements HeaderElementIterator {
    private CharArrayBuffer buffer;
    private HeaderElement currentElement;
    private ParserCursor cursor;
    private final HeaderIterator headerIt;
    private final HeaderValueParser parser;

    public BasicHeaderElementIterator(HeaderIterator headerIterator, HeaderValueParser headerValueParser) {
        this.currentElement = null;
        this.buffer = null;
        this.cursor = null;
        if (headerIterator == null) {
            throw new IllegalArgumentException("Header iterator may not be null");
        }
        if (headerValueParser == null) {
            throw new IllegalArgumentException("Parser may not be null");
        }
        this.headerIt = headerIterator;
        this.parser = headerValueParser;
    }

    public BasicHeaderElementIterator(HeaderIterator headerIterator) {
        this(headerIterator, BasicHeaderValueParser.DEFAULT);
    }

    private void bufferHeaderValue() {
        this.cursor = null;
        this.buffer = null;
        while (this.headerIt.hasNext()) {
            Header headerNextHeader = this.headerIt.nextHeader();
            if (headerNextHeader instanceof FormattedHeader) {
                FormattedHeader formattedHeader = (FormattedHeader) headerNextHeader;
                this.buffer = formattedHeader.getBuffer();
                this.cursor = new ParserCursor(0, this.buffer.length());
                this.cursor.updatePos(formattedHeader.getValuePos());
                return;
            }
            String value = headerNextHeader.getValue();
            if (value != null) {
                this.buffer = new CharArrayBuffer(value.length());
                this.buffer.append(value);
                this.cursor = new ParserCursor(0, this.buffer.length());
                return;
            }
        }
    }

    private void parseNextElement() {
        HeaderElement headerElement;
        loop0: while (true) {
            if (this.headerIt.hasNext() || this.cursor != null) {
                if (this.cursor == null || this.cursor.atEnd()) {
                    bufferHeaderValue();
                }
                if (this.cursor != null) {
                    while (!this.cursor.atEnd()) {
                        headerElement = this.parser.parseHeaderElement(this.buffer, this.cursor);
                        if (headerElement.getName().length() != 0 || headerElement.getValue() != null) {
                            break loop0;
                        }
                    }
                    if (this.cursor.atEnd()) {
                        this.cursor = null;
                        this.buffer = null;
                    }
                }
            } else {
                return;
            }
        }
        this.currentElement = headerElement;
    }

    @Override
    public boolean hasNext() {
        if (this.currentElement == null) {
            parseNextElement();
        }
        return this.currentElement != null;
    }

    @Override
    public HeaderElement nextElement() throws NoSuchElementException {
        if (this.currentElement == null) {
            parseNextElement();
        }
        if (this.currentElement == null) {
            throw new NoSuchElementException("No more header elements available");
        }
        HeaderElement headerElement = this.currentElement;
        this.currentElement = null;
        return headerElement;
    }

    @Override
    public final Object next() throws NoSuchElementException {
        return nextElement();
    }

    @Override
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Remove not supported");
    }
}
