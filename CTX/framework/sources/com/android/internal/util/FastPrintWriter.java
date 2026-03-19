package com.android.internal.util;

import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import android.util.Printer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

public class FastPrintWriter extends PrintWriter {
    private final boolean mAutoFlush;
    private final int mBufferLen;
    private final ByteBuffer mBytes;
    private CharsetEncoder mCharset;
    private boolean mIoError;
    private final OutputStream mOutputStream;
    private int mPos;
    private final Printer mPrinter;
    private final String mSeparator;
    private final char[] mText;
    private final Writer mWriter;

    private static class DummyWriter extends Writer {
        private DummyWriter() {
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException("Shouldn't be here");
        }

        @Override
        public void flush() throws IOException {
            close();
        }

        @Override
        public void write(char[] cArr, int i, int i2) throws IOException {
            close();
        }
    }

    public FastPrintWriter(OutputStream outputStream) {
        this(outputStream, false, 8192);
    }

    public FastPrintWriter(OutputStream outputStream, boolean z) {
        this(outputStream, z, 8192);
    }

    public FastPrintWriter(OutputStream outputStream, boolean z, int i) {
        super(new DummyWriter(), z);
        if (outputStream == null) {
            throw new NullPointerException("out is null");
        }
        this.mBufferLen = i;
        this.mText = new char[i];
        this.mBytes = ByteBuffer.allocate(this.mBufferLen);
        this.mOutputStream = outputStream;
        this.mWriter = null;
        this.mPrinter = null;
        this.mAutoFlush = z;
        this.mSeparator = System.lineSeparator();
        initDefaultEncoder();
    }

    public FastPrintWriter(Writer writer) {
        this(writer, false, 8192);
    }

    public FastPrintWriter(Writer writer, boolean z) {
        this(writer, z, 8192);
    }

    public FastPrintWriter(Writer writer, boolean z, int i) {
        super(new DummyWriter(), z);
        if (writer == null) {
            throw new NullPointerException("wr is null");
        }
        this.mBufferLen = i;
        this.mText = new char[i];
        this.mBytes = null;
        this.mOutputStream = null;
        this.mWriter = writer;
        this.mPrinter = null;
        this.mAutoFlush = z;
        this.mSeparator = System.lineSeparator();
        initDefaultEncoder();
    }

    public FastPrintWriter(Printer printer) {
        this(printer, 512);
    }

    public FastPrintWriter(Printer printer, int i) {
        super((Writer) new DummyWriter(), true);
        if (printer == null) {
            throw new NullPointerException("pr is null");
        }
        this.mBufferLen = i;
        this.mText = new char[i];
        this.mBytes = null;
        this.mOutputStream = null;
        this.mWriter = null;
        this.mPrinter = printer;
        this.mAutoFlush = true;
        this.mSeparator = System.lineSeparator();
        initDefaultEncoder();
    }

    private final void initEncoder(String str) throws UnsupportedEncodingException {
        try {
            this.mCharset = Charset.forName(str).newEncoder();
            this.mCharset.onMalformedInput(CodingErrorAction.REPLACE);
            this.mCharset.onUnmappableCharacter(CodingErrorAction.REPLACE);
        } catch (Exception e) {
            throw new UnsupportedEncodingException(str);
        }
    }

    @Override
    public boolean checkError() {
        boolean z;
        flush();
        synchronized (this.lock) {
            z = this.mIoError;
        }
        return z;
    }

    @Override
    protected void clearError() {
        synchronized (this.lock) {
            this.mIoError = false;
        }
    }

    @Override
    protected void setError() {
        synchronized (this.lock) {
            this.mIoError = true;
        }
    }

    private final void initDefaultEncoder() {
        this.mCharset = Charset.defaultCharset().newEncoder();
        this.mCharset.onMalformedInput(CodingErrorAction.REPLACE);
        this.mCharset.onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    private void appendLocked(char c) throws IOException {
        int i = this.mPos;
        if (i >= this.mBufferLen - 1) {
            flushLocked();
            i = this.mPos;
        }
        this.mText[i] = c;
        this.mPos = i + 1;
    }

    private void appendLocked(String str, int i, int i2) throws IOException {
        int i3 = this.mBufferLen;
        if (i2 > i3) {
            int i4 = i2 + i;
            while (i < i4) {
                int i5 = i + i3;
                appendLocked(str, i, i5 < i4 ? i3 : i4 - i);
                i = i5;
            }
            return;
        }
        int i6 = this.mPos;
        if (i6 + i2 > i3) {
            flushLocked();
            i6 = this.mPos;
        }
        str.getChars(i, i + i2, this.mText, i6);
        this.mPos = i6 + i2;
    }

    private void appendLocked(char[] cArr, int i, int i2) throws IOException {
        int i3 = this.mBufferLen;
        if (i2 > i3) {
            int i4 = i2 + i;
            while (i < i4) {
                int i5 = i + i3;
                appendLocked(cArr, i, i5 < i4 ? i3 : i4 - i);
                i = i5;
            }
            return;
        }
        int i6 = this.mPos;
        if (i6 + i2 > i3) {
            flushLocked();
            i6 = this.mPos;
        }
        System.arraycopy(cArr, i, this.mText, i6, i2);
        this.mPos = i6 + i2;
    }

    private void flushBytesLocked() throws IOException {
        int iPosition;
        if (!this.mIoError && (iPosition = this.mBytes.position()) > 0) {
            this.mBytes.flip();
            this.mOutputStream.write(this.mBytes.array(), 0, iPosition);
            this.mBytes.clear();
        }
    }

    private void flushLocked() throws IOException {
        if (this.mPos > 0) {
            if (this.mOutputStream != null) {
                CharBuffer charBufferWrap = CharBuffer.wrap(this.mText, 0, this.mPos);
                CoderResult coderResultEncode = this.mCharset.encode(charBufferWrap, this.mBytes, true);
                while (!this.mIoError) {
                    if (coderResultEncode.isError()) {
                        throw new IOException(coderResultEncode.toString());
                    }
                    if (!coderResultEncode.isOverflow()) {
                        break;
                    }
                    flushBytesLocked();
                    coderResultEncode = this.mCharset.encode(charBufferWrap, this.mBytes, true);
                }
                if (!this.mIoError) {
                    flushBytesLocked();
                    this.mOutputStream.flush();
                }
            } else if (this.mWriter != null) {
                if (!this.mIoError) {
                    this.mWriter.write(this.mText, 0, this.mPos);
                    this.mWriter.flush();
                }
            } else {
                int length = this.mSeparator.length();
                if (length >= this.mPos) {
                    length = this.mPos;
                }
                int i = 0;
                while (i < length && this.mText[(this.mPos - 1) - i] == this.mSeparator.charAt((this.mSeparator.length() - 1) - i)) {
                    i++;
                }
                if (i >= this.mPos) {
                    this.mPrinter.println("");
                } else {
                    this.mPrinter.println(new String(this.mText, 0, this.mPos - i));
                }
            }
            this.mPos = 0;
        }
    }

    @Override
    public void flush() {
        synchronized (this.lock) {
            try {
                flushLocked();
                if (!this.mIoError) {
                    if (this.mOutputStream != null) {
                        this.mOutputStream.flush();
                    } else if (this.mWriter != null) {
                        this.mWriter.flush();
                    }
                }
            } catch (IOException e) {
                Log.w("FastPrintWriter", "Write failure", e);
                setError();
            }
        }
    }

    @Override
    public void close() {
        synchronized (this.lock) {
            try {
                flushLocked();
                if (this.mOutputStream != null) {
                    this.mOutputStream.close();
                } else if (this.mWriter != null) {
                    this.mWriter.close();
                }
            } catch (IOException e) {
                Log.w("FastPrintWriter", "Write failure", e);
                setError();
            }
        }
    }

    @Override
    public void print(char[] cArr) {
        synchronized (this.lock) {
            try {
                appendLocked(cArr, 0, cArr.length);
            } catch (IOException e) {
                Log.w("FastPrintWriter", "Write failure", e);
                setError();
            }
        }
    }

    @Override
    public void print(char c) {
        synchronized (this.lock) {
            try {
                appendLocked(c);
            } catch (IOException e) {
                Log.w("FastPrintWriter", "Write failure", e);
                setError();
            }
        }
    }

    @Override
    public void print(String str) {
        if (str == null) {
            str = String.valueOf((Object) null);
        }
        synchronized (this.lock) {
            try {
                appendLocked(str, 0, str.length());
            } catch (IOException e) {
                Log.w("FastPrintWriter", "Write failure", e);
                setError();
            }
        }
    }

    @Override
    public void print(int i) {
        if (i == 0) {
            print(WifiEnterpriseConfig.ENGINE_DISABLE);
        } else {
            super.print(i);
        }
    }

    @Override
    public void print(long j) {
        if (j == 0) {
            print(WifiEnterpriseConfig.ENGINE_DISABLE);
        } else {
            super.print(j);
        }
    }

    @Override
    public void println() {
        synchronized (this.lock) {
            try {
                appendLocked(this.mSeparator, 0, this.mSeparator.length());
            } catch (IOException e) {
                Log.w("FastPrintWriter", "Write failure", e);
                setError();
            }
            if (this.mAutoFlush) {
                flushLocked();
            }
        }
    }

    @Override
    public void println(int i) {
        if (i == 0) {
            println(WifiEnterpriseConfig.ENGINE_DISABLE);
        } else {
            super.println(i);
        }
    }

    @Override
    public void println(long j) {
        if (j == 0) {
            println(WifiEnterpriseConfig.ENGINE_DISABLE);
        } else {
            super.println(j);
        }
    }

    @Override
    public void println(char[] cArr) {
        print(cArr);
        println();
    }

    @Override
    public void println(char c) {
        print(c);
        println();
    }

    @Override
    public void write(char[] cArr, int i, int i2) {
        synchronized (this.lock) {
            try {
                appendLocked(cArr, i, i2);
            } catch (IOException e) {
                Log.w("FastPrintWriter", "Write failure", e);
                setError();
            }
        }
    }

    @Override
    public void write(int i) {
        synchronized (this.lock) {
            try {
                appendLocked((char) i);
            } catch (IOException e) {
                Log.w("FastPrintWriter", "Write failure", e);
                setError();
            }
        }
    }

    @Override
    public void write(String str) {
        synchronized (this.lock) {
            try {
                appendLocked(str, 0, str.length());
            } catch (IOException e) {
                Log.w("FastPrintWriter", "Write failure", e);
                setError();
            }
        }
    }

    @Override
    public void write(String str, int i, int i2) {
        synchronized (this.lock) {
            try {
                appendLocked(str, i, i2);
            } catch (IOException e) {
                Log.w("FastPrintWriter", "Write failure", e);
                setError();
            }
        }
    }

    @Override
    public PrintWriter append(CharSequence charSequence, int i, int i2) {
        if (charSequence == null) {
            charSequence = "null";
        }
        String string = charSequence.subSequence(i, i2).toString();
        write(string, 0, string.length());
        return this;
    }
}
