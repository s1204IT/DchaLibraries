package com.android.internal.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import org.xmlpull.v1.XmlSerializer;

public class FastXmlSerializer implements XmlSerializer {
    private static final int DEFAULT_BUFFER_LEN = 32768;
    private static final String[] ESCAPE_TABLE = {"&#0;", "&#1;", "&#2;", "&#3;", "&#4;", "&#5;", "&#6;", "&#7;", "&#8;", "&#9;", "&#10;", "&#11;", "&#12;", "&#13;", "&#14;", "&#15;", "&#16;", "&#17;", "&#18;", "&#19;", "&#20;", "&#21;", "&#22;", "&#23;", "&#24;", "&#25;", "&#26;", "&#27;", "&#28;", "&#29;", "&#30;", "&#31;", null, null, "&quot;", null, null, null, "&amp;", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "&lt;", null, "&gt;", null};
    private static String sSpace = "                                                              ";
    private final int mBufferLen;
    private ByteBuffer mBytes;
    private CharsetEncoder mCharset;
    private boolean mInTag;
    private boolean mIndent;
    private boolean mLineStart;
    private int mNesting;
    private OutputStream mOutputStream;
    private int mPos;
    private final char[] mText;
    private Writer mWriter;

    public FastXmlSerializer() {
        this(32768);
    }

    public FastXmlSerializer(int i) {
        this.mIndent = false;
        this.mNesting = 0;
        this.mLineStart = true;
        this.mBufferLen = i <= 0 ? 32768 : i;
        this.mText = new char[this.mBufferLen];
        this.mBytes = ByteBuffer.allocate(this.mBufferLen);
    }

    private void append(char c) throws IOException {
        int i = this.mPos;
        if (i >= this.mBufferLen - 1) {
            flush();
            i = this.mPos;
        }
        this.mText[i] = c;
        this.mPos = i + 1;
    }

    private void append(String str, int i, int i2) throws IOException {
        if (i2 > this.mBufferLen) {
            int i3 = i2 + i;
            while (i < i3) {
                int i4 = this.mBufferLen + i;
                append(str, i, i4 < i3 ? this.mBufferLen : i3 - i);
                i = i4;
            }
            return;
        }
        int i5 = this.mPos;
        if (i5 + i2 > this.mBufferLen) {
            flush();
            i5 = this.mPos;
        }
        str.getChars(i, i + i2, this.mText, i5);
        this.mPos = i5 + i2;
    }

    private void append(char[] cArr, int i, int i2) throws IOException {
        if (i2 > this.mBufferLen) {
            int i3 = i2 + i;
            while (i < i3) {
                int i4 = this.mBufferLen + i;
                append(cArr, i, i4 < i3 ? this.mBufferLen : i3 - i);
                i = i4;
            }
            return;
        }
        int i5 = this.mPos;
        if (i5 + i2 > this.mBufferLen) {
            flush();
            i5 = this.mPos;
        }
        System.arraycopy(cArr, i, this.mText, i5, i2);
        this.mPos = i5 + i2;
    }

    private void append(String str) throws IOException {
        append(str, 0, str.length());
    }

    private void appendIndent(int i) throws IOException {
        int length = i * 4;
        if (length > sSpace.length()) {
            length = sSpace.length();
        }
        append(sSpace, 0, length);
    }

    private void escapeAndAppendString(String str) throws IOException {
        String str2;
        int length = str.length();
        char length2 = (char) ESCAPE_TABLE.length;
        String[] strArr = ESCAPE_TABLE;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            if (cCharAt < length2 && (str2 = strArr[cCharAt]) != null) {
                if (i2 < i) {
                    append(str, i2, i - i2);
                }
                i2 = i + 1;
                append(str2);
            }
            i++;
        }
        if (i2 < i) {
            append(str, i2, i - i2);
        }
    }

    private void escapeAndAppendString(char[] cArr, int i, int i2) throws IOException {
        String str;
        char length = (char) ESCAPE_TABLE.length;
        String[] strArr = ESCAPE_TABLE;
        int i3 = i2 + i;
        int i4 = i;
        while (i < i3) {
            char c = cArr[i];
            if (c < length && (str = strArr[c]) != null) {
                if (i4 < i) {
                    append(cArr, i4, i - i4);
                }
                i4 = i + 1;
                append(str);
            }
            i++;
        }
        if (i4 < i) {
            append(cArr, i4, i - i4);
        }
    }

    @Override
    public XmlSerializer attribute(String str, String str2, String str3) throws IllegalStateException, IOException, IllegalArgumentException {
        append(' ');
        if (str != null) {
            append(str);
            append(':');
        }
        append(str2);
        append("=\"");
        escapeAndAppendString(str3);
        append('\"');
        this.mLineStart = false;
        return this;
    }

    @Override
    public void cdsect(String str) throws IllegalStateException, IOException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void comment(String str) throws IllegalStateException, IOException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void docdecl(String str) throws IllegalStateException, IOException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void endDocument() throws IllegalStateException, IOException, IllegalArgumentException {
        flush();
    }

    @Override
    public XmlSerializer endTag(String str, String str2) throws IllegalStateException, IOException, IllegalArgumentException {
        this.mNesting--;
        if (this.mInTag) {
            append(" />\n");
        } else {
            if (this.mIndent && this.mLineStart) {
                appendIndent(this.mNesting);
            }
            append("</");
            if (str != null) {
                append(str);
                append(':');
            }
            append(str2);
            append(">\n");
        }
        this.mLineStart = true;
        this.mInTag = false;
        return this;
    }

    @Override
    public void entityRef(String str) throws IllegalStateException, IOException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    private void flushBytes() throws IOException {
        int iPosition = this.mBytes.position();
        if (iPosition > 0) {
            this.mBytes.flip();
            this.mOutputStream.write(this.mBytes.array(), 0, iPosition);
            this.mBytes.clear();
        }
    }

    @Override
    public void flush() throws IOException {
        if (this.mPos > 0) {
            if (this.mOutputStream != null) {
                CharBuffer charBufferWrap = CharBuffer.wrap(this.mText, 0, this.mPos);
                CoderResult coderResultEncode = this.mCharset.encode(charBufferWrap, this.mBytes, true);
                while (!coderResultEncode.isError()) {
                    if (coderResultEncode.isOverflow()) {
                        flushBytes();
                        coderResultEncode = this.mCharset.encode(charBufferWrap, this.mBytes, true);
                    } else {
                        flushBytes();
                        this.mOutputStream.flush();
                    }
                }
                throw new IOException(coderResultEncode.toString());
            }
            this.mWriter.write(this.mText, 0, this.mPos);
            this.mWriter.flush();
            this.mPos = 0;
        }
    }

    @Override
    public int getDepth() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getFeature(String str) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNamespace() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPrefix(String str, boolean z) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String str) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void ignorableWhitespace(String str) throws IllegalStateException, IOException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processingInstruction(String str) throws IllegalStateException, IOException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFeature(String str, boolean z) throws IllegalStateException, IllegalArgumentException {
        if (str.equals("http://xmlpull.org/v1/doc/features.html#indent-output")) {
            this.mIndent = true;
            return;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOutput(OutputStream outputStream, String str) throws IllegalStateException, IOException, IllegalArgumentException {
        if (outputStream == null) {
            throw new IllegalArgumentException();
        }
        try {
            this.mCharset = Charset.forName(str).newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            this.mOutputStream = outputStream;
        } catch (IllegalCharsetNameException e) {
            throw ((UnsupportedEncodingException) new UnsupportedEncodingException(str).initCause(e));
        } catch (UnsupportedCharsetException e2) {
            throw ((UnsupportedEncodingException) new UnsupportedEncodingException(str).initCause(e2));
        }
    }

    @Override
    public void setOutput(Writer writer) throws IllegalStateException, IOException, IllegalArgumentException {
        this.mWriter = writer;
    }

    @Override
    public void setPrefix(String str, String str2) throws IllegalStateException, IOException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(String str, Object obj) throws IllegalStateException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startDocument(String str, Boolean bool) throws IllegalStateException, IOException, IllegalArgumentException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='utf-8' standalone='");
        sb.append(bool.booleanValue() ? "yes" : "no");
        sb.append("' ?>\n");
        append(sb.toString());
        this.mLineStart = true;
    }

    @Override
    public XmlSerializer startTag(String str, String str2) throws IllegalStateException, IOException, IllegalArgumentException {
        if (this.mInTag) {
            append(">\n");
        }
        if (this.mIndent) {
            appendIndent(this.mNesting);
        }
        this.mNesting++;
        append('<');
        if (str != null) {
            append(str);
            append(':');
        }
        append(str2);
        this.mInTag = true;
        this.mLineStart = false;
        return this;
    }

    @Override
    public XmlSerializer text(char[] cArr, int i, int i2) throws IllegalStateException, IOException, IllegalArgumentException {
        if (this.mInTag) {
            append(">");
            this.mInTag = false;
        }
        escapeAndAppendString(cArr, i, i2);
        if (this.mIndent) {
            this.mLineStart = cArr[(i + i2) - 1] == '\n';
        }
        return this;
    }

    @Override
    public XmlSerializer text(String str) throws IllegalStateException, IOException, IllegalArgumentException {
        boolean z = false;
        if (this.mInTag) {
            append(">");
            this.mInTag = false;
        }
        escapeAndAppendString(str);
        if (this.mIndent) {
            if (str.length() > 0 && str.charAt(str.length() - 1) == '\n') {
                z = true;
            }
            this.mLineStart = z;
        }
        return this;
    }
}
