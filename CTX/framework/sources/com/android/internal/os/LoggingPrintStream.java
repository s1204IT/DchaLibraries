package com.android.internal.os;

import com.android.internal.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Formatter;
import java.util.Locale;

@VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
public abstract class LoggingPrintStream extends PrintStream {
    private final StringBuilder builder;
    private CharBuffer decodedChars;
    private CharsetDecoder decoder;
    private ByteBuffer encodedBytes;
    private final Formatter formatter;

    protected abstract void log(String str);

    protected LoggingPrintStream() {
        super(new OutputStream() {
            @Override
            public void write(int i) throws IOException {
                throw new AssertionError();
            }
        });
        this.builder = new StringBuilder();
        this.formatter = new Formatter(this.builder, (Locale) null);
    }

    @Override
    public synchronized void flush() {
        flush(true);
    }

    private void flush(boolean z) {
        int length = this.builder.length();
        int i = 0;
        while (i < length) {
            int iIndexOf = this.builder.indexOf("\n", i);
            if (iIndexOf == -1) {
                break;
            }
            log(this.builder.substring(i, iIndexOf));
            i = iIndexOf + 1;
        }
        if (!z) {
            this.builder.delete(0, i);
            return;
        }
        if (i < length) {
            log(this.builder.substring(i));
        }
        this.builder.setLength(0);
    }

    @Override
    public void write(int i) {
        write(new byte[]{(byte) i}, 0, 1);
    }

    @Override
    public void write(byte[] bArr) {
        write(bArr, 0, bArr.length);
    }

    @Override
    public synchronized void write(byte[] bArr, int i, int i2) {
        CoderResult coderResultDecode;
        if (this.decoder == null) {
            this.encodedBytes = ByteBuffer.allocate(80);
            this.decodedChars = CharBuffer.allocate(80);
            this.decoder = Charset.defaultCharset().newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        }
        int i3 = i2 + i;
        while (i < i3) {
            int iMin = Math.min(this.encodedBytes.remaining(), i3 - i);
            this.encodedBytes.put(bArr, i, iMin);
            i += iMin;
            this.encodedBytes.flip();
            do {
                coderResultDecode = this.decoder.decode(this.encodedBytes, this.decodedChars, false);
                this.decodedChars.flip();
                this.builder.append((CharSequence) this.decodedChars);
                this.decodedChars.clear();
            } while (coderResultDecode.isOverflow());
            this.encodedBytes.compact();
        }
        flush(false);
    }

    @Override
    public boolean checkError() {
        return false;
    }

    @Override
    protected void setError() {
    }

    @Override
    public void close() {
    }

    @Override
    public PrintStream format(String str, Object... objArr) {
        return format(Locale.getDefault(), str, objArr);
    }

    @Override
    public PrintStream printf(String str, Object... objArr) {
        return format(str, objArr);
    }

    @Override
    public PrintStream printf(Locale locale, String str, Object... objArr) {
        return format(locale, str, objArr);
    }

    @Override
    public synchronized PrintStream format(Locale locale, String str, Object... objArr) {
        if (str == null) {
            throw new NullPointerException("format");
        }
        this.formatter.format(locale, str, objArr);
        flush(false);
        return this;
    }

    @Override
    public synchronized void print(char[] cArr) {
        this.builder.append(cArr);
        flush(false);
    }

    @Override
    public synchronized void print(char c) {
        this.builder.append(c);
        if (c == '\n') {
            flush(false);
        }
    }

    @Override
    public synchronized void print(double d) {
        this.builder.append(d);
    }

    @Override
    public synchronized void print(float f) {
        this.builder.append(f);
    }

    @Override
    public synchronized void print(int i) {
        this.builder.append(i);
    }

    @Override
    public synchronized void print(long j) {
        this.builder.append(j);
    }

    @Override
    public synchronized void print(Object obj) {
        this.builder.append(obj);
        flush(false);
    }

    @Override
    public synchronized void print(String str) {
        this.builder.append(str);
        flush(false);
    }

    @Override
    public synchronized void print(boolean z) {
        this.builder.append(z);
    }

    @Override
    public synchronized void println() {
        flush(true);
    }

    @Override
    public synchronized void println(char[] cArr) {
        this.builder.append(cArr);
        flush(true);
    }

    @Override
    public synchronized void println(char c) {
        this.builder.append(c);
        flush(true);
    }

    @Override
    public synchronized void println(double d) {
        this.builder.append(d);
        flush(true);
    }

    @Override
    public synchronized void println(float f) {
        this.builder.append(f);
        flush(true);
    }

    @Override
    public synchronized void println(int i) {
        this.builder.append(i);
        flush(true);
    }

    @Override
    public synchronized void println(long j) {
        this.builder.append(j);
        flush(true);
    }

    @Override
    public synchronized void println(Object obj) {
        this.builder.append(obj);
        flush(true);
    }

    @Override
    public synchronized void println(String str) {
        if (this.builder.length() == 0 && str != null) {
            int length = str.length();
            int i = 0;
            while (i < length) {
                int iIndexOf = str.indexOf(10, i);
                if (iIndexOf == -1) {
                    break;
                }
                log(str.substring(i, iIndexOf));
                i = iIndexOf + 1;
            }
            if (i < length) {
                log(str.substring(i));
            }
        } else {
            this.builder.append(str);
            flush(true);
        }
    }

    @Override
    public synchronized void println(boolean z) {
        this.builder.append(z);
        flush(true);
    }

    @Override
    public synchronized PrintStream append(char c) {
        print(c);
        return this;
    }

    @Override
    public synchronized PrintStream append(CharSequence charSequence) {
        this.builder.append(charSequence);
        flush(false);
        return this;
    }

    @Override
    public synchronized PrintStream append(CharSequence charSequence, int i, int i2) {
        this.builder.append(charSequence, i, i2);
        flush(false);
        return this;
    }
}
