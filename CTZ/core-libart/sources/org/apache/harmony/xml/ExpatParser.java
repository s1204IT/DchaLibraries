package org.apache.harmony.xml;

import android.icu.text.PluralRules;
import dalvik.annotation.optimization.ReachabilitySensitive;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import libcore.io.IoUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

class ExpatParser {
    private static final int BUFFER_SIZE = 8096;
    static final String CHARACTER_ENCODING = "UTF-16";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final String OUTSIDE_START_ELEMENT = "Attributes can only be used within the scope of startElement().";
    private static final int TIMEOUT = 20000;
    private int attributeCount;
    private long attributePointer;
    private final ExpatAttributes attributes;
    private final String encoding;
    private boolean inStartElement;
    private final Locator locator;

    @ReachabilitySensitive
    private long pointer;
    private final String publicId;
    private final String systemId;
    private final ExpatReader xmlReader;

    private native void appendBytes(long j, byte[] bArr, int i, int i2) throws SAXException, ExpatException;

    private native void appendChars(long j, char[] cArr, int i, int i2) throws SAXException, ExpatException;

    private native void appendString(long j, String str, boolean z) throws SAXException, ExpatException;

    private static native long cloneAttributes(long j, int i);

    private static native int column(long j);

    private static native long createEntityParser(long j, String str);

    private native long initialize(String str, boolean z);

    private static native int line(long j);

    private native void release(long j);

    private static native void releaseParser(long j);

    private static native void staticInitialize(String str);

    ExpatParser(String str, ExpatReader expatReader, boolean z, String str2, String str3) {
        this.inStartElement = false;
        this.attributeCount = -1;
        this.attributePointer = 0L;
        this.locator = new ExpatLocator();
        this.attributes = new CurrentAttributes();
        this.publicId = str2;
        this.systemId = str3;
        this.xmlReader = expatReader;
        this.encoding = str == null ? DEFAULT_ENCODING : str;
        this.pointer = initialize(this.encoding, z);
    }

    private ExpatParser(String str, ExpatReader expatReader, long j, String str2, String str3) {
        this.inStartElement = false;
        this.attributeCount = -1;
        this.attributePointer = 0L;
        this.locator = new ExpatLocator();
        this.attributes = new CurrentAttributes();
        this.encoding = str;
        this.xmlReader = expatReader;
        this.pointer = j;
        this.systemId = str3;
        this.publicId = str2;
    }

    void startElement(String str, String str2, String str3, long j, int i) throws SAXException {
        ContentHandler contentHandler = this.xmlReader.contentHandler;
        if (contentHandler == null) {
            return;
        }
        try {
            this.inStartElement = true;
            this.attributePointer = j;
            this.attributeCount = i;
            contentHandler.startElement(str, str2, str3, this.attributes);
        } finally {
            this.inStartElement = false;
            this.attributeCount = -1;
            this.attributePointer = 0L;
        }
    }

    void endElement(String str, String str2, String str3) throws SAXException {
        ContentHandler contentHandler = this.xmlReader.contentHandler;
        if (contentHandler != null) {
            contentHandler.endElement(str, str2, str3);
        }
    }

    void text(char[] cArr, int i) throws SAXException {
        ContentHandler contentHandler = this.xmlReader.contentHandler;
        if (contentHandler != null) {
            contentHandler.characters(cArr, 0, i);
        }
    }

    void comment(char[] cArr, int i) throws SAXException {
        LexicalHandler lexicalHandler = this.xmlReader.lexicalHandler;
        if (lexicalHandler != null) {
            lexicalHandler.comment(cArr, 0, i);
        }
    }

    void startCdata() throws SAXException {
        LexicalHandler lexicalHandler = this.xmlReader.lexicalHandler;
        if (lexicalHandler != null) {
            lexicalHandler.startCDATA();
        }
    }

    void endCdata() throws SAXException {
        LexicalHandler lexicalHandler = this.xmlReader.lexicalHandler;
        if (lexicalHandler != null) {
            lexicalHandler.endCDATA();
        }
    }

    void startNamespace(String str, String str2) throws SAXException {
        ContentHandler contentHandler = this.xmlReader.contentHandler;
        if (contentHandler != null) {
            contentHandler.startPrefixMapping(str, str2);
        }
    }

    void endNamespace(String str) throws SAXException {
        ContentHandler contentHandler = this.xmlReader.contentHandler;
        if (contentHandler != null) {
            contentHandler.endPrefixMapping(str);
        }
    }

    void startDtd(String str, String str2, String str3) throws SAXException {
        LexicalHandler lexicalHandler = this.xmlReader.lexicalHandler;
        if (lexicalHandler != null) {
            lexicalHandler.startDTD(str, str2, str3);
        }
    }

    void endDtd() throws SAXException {
        LexicalHandler lexicalHandler = this.xmlReader.lexicalHandler;
        if (lexicalHandler != null) {
            lexicalHandler.endDTD();
        }
    }

    void processingInstruction(String str, String str2) throws SAXException {
        ContentHandler contentHandler = this.xmlReader.contentHandler;
        if (contentHandler != null) {
            contentHandler.processingInstruction(str, str2);
        }
    }

    void notationDecl(String str, String str2, String str3) throws SAXException {
        DTDHandler dTDHandler = this.xmlReader.dtdHandler;
        if (dTDHandler != null) {
            dTDHandler.notationDecl(str, str2, str3);
        }
    }

    void unparsedEntityDecl(String str, String str2, String str3, String str4) throws SAXException {
        DTDHandler dTDHandler = this.xmlReader.dtdHandler;
        if (dTDHandler != null) {
            dTDHandler.unparsedEntityDecl(str, str2, str3, str4);
        }
    }

    void handleExternalEntity(String str, String str2, String str3) throws SAXException, IOException {
        EntityResolver entityResolver = this.xmlReader.entityResolver;
        if (entityResolver == null) {
            return;
        }
        if (this.systemId != null) {
            try {
                URI uri = new URI(str3);
                if (!uri.isAbsolute() && !uri.isOpaque()) {
                    str3 = new URI(this.systemId).resolve(uri).toString();
                }
            } catch (Exception e) {
                System.logI("Could not resolve '" + str3 + "' relative to '" + this.systemId + "' at " + this.locator, e);
            }
        }
        InputSource inputSourceResolveEntity = entityResolver.resolveEntity(str2, str3);
        if (inputSourceResolveEntity == null) {
            return;
        }
        String strPickEncoding = pickEncoding(inputSourceResolveEntity);
        long jCreateEntityParser = createEntityParser(this.pointer, str);
        try {
            parseExternalEntity(new EntityParser(strPickEncoding, this.xmlReader, jCreateEntityParser, inputSourceResolveEntity.getPublicId(), inputSourceResolveEntity.getSystemId()), inputSourceResolveEntity);
        } finally {
            releaseParser(jCreateEntityParser);
        }
    }

    private String pickEncoding(InputSource inputSource) {
        if (inputSource.getCharacterStream() != null) {
            return CHARACTER_ENCODING;
        }
        String encoding = inputSource.getEncoding();
        return encoding == null ? DEFAULT_ENCODING : encoding;
    }

    private void parseExternalEntity(ExpatParser expatParser, InputSource inputSource) throws SAXException, IOException {
        Reader characterStream = inputSource.getCharacterStream();
        if (characterStream != null) {
            try {
                expatParser.append("<externalEntity>");
                expatParser.parseFragment(characterStream);
                expatParser.append("</externalEntity>");
                return;
            } finally {
                IoUtils.closeQuietly(characterStream);
            }
        }
        InputStream byteStream = inputSource.getByteStream();
        if (byteStream != null) {
            try {
                expatParser.append("<externalEntity>".getBytes(expatParser.encoding));
                expatParser.parseFragment(byteStream);
                expatParser.append("</externalEntity>".getBytes(expatParser.encoding));
                return;
            } finally {
                IoUtils.closeQuietly(byteStream);
            }
        }
        String systemId = inputSource.getSystemId();
        if (systemId == null) {
            throw new ParseException("No input specified.", this.locator);
        }
        InputStream inputStreamOpenUrl = openUrl(systemId);
        try {
            expatParser.append("<externalEntity>".getBytes(expatParser.encoding));
            expatParser.parseFragment(inputStreamOpenUrl);
            expatParser.append("</externalEntity>".getBytes(expatParser.encoding));
        } finally {
            IoUtils.closeQuietly(inputStreamOpenUrl);
        }
    }

    void append(String str) throws SAXException {
        try {
            appendString(this.pointer, str, false);
        } catch (ExpatException e) {
            throw new ParseException(e.getMessage(), this.locator);
        }
    }

    void append(char[] cArr, int i, int i2) throws SAXException {
        try {
            appendChars(this.pointer, cArr, i, i2);
        } catch (ExpatException e) {
            throw new ParseException(e.getMessage(), this.locator);
        }
    }

    void append(byte[] bArr) throws SAXException {
        append(bArr, 0, bArr.length);
    }

    void append(byte[] bArr, int i, int i2) throws SAXException {
        try {
            appendBytes(this.pointer, bArr, i, i2);
        } catch (ExpatException e) {
            throw new ParseException(e.getMessage(), this.locator);
        }
    }

    void parseDocument(InputStream inputStream) throws SAXException, IOException {
        startDocument();
        parseFragment(inputStream);
        finish();
        endDocument();
    }

    void parseDocument(Reader reader) throws SAXException, IOException {
        startDocument();
        parseFragment(reader);
        finish();
        endDocument();
    }

    private void parseFragment(Reader reader) throws SAXException, IOException {
        char[] cArr = new char[4048];
        while (true) {
            int i = reader.read(cArr);
            if (i != -1) {
                try {
                    appendChars(this.pointer, cArr, 0, i);
                } catch (ExpatException e) {
                    throw new ParseException(e.getMessage(), this.locator);
                }
            } else {
                return;
            }
        }
    }

    private void parseFragment(InputStream inputStream) throws SAXException, IOException {
        byte[] bArr = new byte[BUFFER_SIZE];
        while (true) {
            int i = inputStream.read(bArr);
            if (i != -1) {
                try {
                    appendBytes(this.pointer, bArr, 0, i);
                } catch (ExpatException e) {
                    throw new ParseException(e.getMessage(), this.locator);
                }
            } else {
                return;
            }
        }
    }

    private void startDocument() throws SAXException {
        ContentHandler contentHandler = this.xmlReader.contentHandler;
        if (contentHandler != null) {
            contentHandler.setDocumentLocator(this.locator);
            contentHandler.startDocument();
        }
    }

    private void endDocument() throws SAXException {
        ContentHandler contentHandler = this.xmlReader.contentHandler;
        if (contentHandler != null) {
            contentHandler.endDocument();
        }
    }

    void finish() throws SAXException {
        try {
            appendString(this.pointer, "", true);
        } catch (ExpatException e) {
            throw new ParseException(e.getMessage(), this.locator);
        }
    }

    protected synchronized void finalize() throws Throwable {
        try {
            if (this.pointer != 0) {
                release(this.pointer);
                this.pointer = 0L;
            }
        } finally {
            super.finalize();
        }
    }

    static {
        staticInitialize("");
    }

    private int line() {
        return line(this.pointer);
    }

    private int column() {
        return column(this.pointer);
    }

    Attributes cloneAttributes() {
        if (!this.inStartElement) {
            throw new IllegalStateException(OUTSIDE_START_ELEMENT);
        }
        if (this.attributeCount != 0) {
            return new ClonedAttributes(this.pointer, cloneAttributes(this.attributePointer, this.attributeCount), this.attributeCount);
        }
        return ClonedAttributes.EMPTY;
    }

    private static class ClonedAttributes extends ExpatAttributes {
        private static final Attributes EMPTY = new ClonedAttributes(0, 0, 0);
        private final int length;
        private final long parserPointer;
        private long pointer;

        private ClonedAttributes(long j, long j2, int i) {
            this.parserPointer = j;
            this.pointer = j2;
            this.length = i;
        }

        @Override
        public long getParserPointer() {
            return this.parserPointer;
        }

        @Override
        public long getPointer() {
            return this.pointer;
        }

        @Override
        public int getLength() {
            return this.length;
        }

        protected synchronized void finalize() throws Throwable {
            try {
                if (this.pointer != 0) {
                    freeAttributes(this.pointer);
                    this.pointer = 0L;
                }
            } finally {
                super.finalize();
            }
        }
    }

    private class ExpatLocator implements Locator {
        private ExpatLocator() {
        }

        @Override
        public String getPublicId() {
            return ExpatParser.this.publicId;
        }

        @Override
        public String getSystemId() {
            return ExpatParser.this.systemId;
        }

        @Override
        public int getLineNumber() {
            return ExpatParser.this.line();
        }

        @Override
        public int getColumnNumber() {
            return ExpatParser.this.column();
        }

        public String toString() {
            return "Locator[publicId: " + ExpatParser.this.publicId + ", systemId: " + ExpatParser.this.systemId + ", line: " + getLineNumber() + ", column: " + getColumnNumber() + "]";
        }
    }

    private class CurrentAttributes extends ExpatAttributes {
        private CurrentAttributes() {
        }

        @Override
        public long getParserPointer() {
            return ExpatParser.this.pointer;
        }

        @Override
        public long getPointer() {
            if (ExpatParser.this.inStartElement) {
                return ExpatParser.this.attributePointer;
            }
            throw new IllegalStateException(ExpatParser.OUTSIDE_START_ELEMENT);
        }

        @Override
        public int getLength() {
            if (ExpatParser.this.inStartElement) {
                return ExpatParser.this.attributeCount;
            }
            throw new IllegalStateException(ExpatParser.OUTSIDE_START_ELEMENT);
        }
    }

    private static class ParseException extends SAXParseException {
        private ParseException(String str, Locator locator) {
            super(makeMessage(str, locator), locator);
        }

        private static String makeMessage(String str, Locator locator) {
            return makeMessage(str, locator.getLineNumber(), locator.getColumnNumber());
        }

        private static String makeMessage(String str, int i, int i2) {
            return "At line " + i + ", column " + i2 + PluralRules.KEYWORD_RULE_SEPARATOR + str;
        }
    }

    static InputStream openUrl(String str) throws IOException {
        try {
            URLConnection uRLConnectionOpenConnection = new URL(str).openConnection();
            uRLConnectionOpenConnection.setConnectTimeout(TIMEOUT);
            uRLConnectionOpenConnection.setReadTimeout(TIMEOUT);
            uRLConnectionOpenConnection.setDoInput(true);
            uRLConnectionOpenConnection.setDoOutput(false);
            return uRLConnectionOpenConnection.getInputStream();
        } catch (Exception e) {
            IOException iOException = new IOException("Couldn't open " + str);
            iOException.initCause(e);
            throw iOException;
        }
    }

    private static class EntityParser extends ExpatParser {
        private int depth;

        private EntityParser(String str, ExpatReader expatReader, long j, String str2, String str3) {
            super(str, expatReader, j, str2, str3);
            this.depth = 0;
        }

        @Override
        void startElement(String str, String str2, String str3, long j, int i) throws SAXException {
            int i2 = this.depth;
            this.depth = i2 + 1;
            if (i2 > 0) {
                super.startElement(str, str2, str3, j, i);
            }
        }

        @Override
        void endElement(String str, String str2, String str3) throws SAXException {
            int i = this.depth - 1;
            this.depth = i;
            if (i > 0) {
                super.endElement(str, str2, str3);
            }
        }

        @Override
        protected synchronized void finalize() throws Throwable {
        }
    }
}
