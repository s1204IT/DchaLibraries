package java.io;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Formatter;
import java.util.Locale;

public class PrintStream extends FilterOutputStream implements Appendable, Closeable {
    private final boolean autoFlush;
    private OutputStreamWriter charOut;
    private Charset charset;
    private boolean closing;
    private Formatter formatter;
    private BufferedWriter textOut;
    private boolean trouble;

    private static <T> T requireNonNull(T t, String str) {
        if (t == null) {
            throw new NullPointerException(str);
        }
        return t;
    }

    private static Charset toCharset(String str) throws UnsupportedEncodingException {
        requireNonNull(str, "charsetName");
        try {
            return Charset.forName(str);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(str);
        }
    }

    private PrintStream(boolean z, OutputStream outputStream) {
        super(outputStream);
        this.trouble = false;
        this.closing = false;
        this.autoFlush = z;
    }

    private PrintStream(boolean z, OutputStream outputStream, Charset charset) {
        super(outputStream);
        this.trouble = false;
        this.closing = false;
        this.autoFlush = z;
        this.charset = charset;
    }

    private PrintStream(boolean z, Charset charset, OutputStream outputStream) throws UnsupportedEncodingException {
        this(z, outputStream, charset);
    }

    public PrintStream(OutputStream outputStream) {
        this(outputStream, false);
    }

    public PrintStream(OutputStream outputStream, boolean z) {
        this(z, (OutputStream) requireNonNull(outputStream, "Null output stream"));
    }

    public PrintStream(OutputStream outputStream, boolean z, String str) throws UnsupportedEncodingException {
        this(z, (OutputStream) requireNonNull(outputStream, "Null output stream"), toCharset(str));
    }

    public PrintStream(String str) throws FileNotFoundException {
        this(false, (OutputStream) new FileOutputStream(str));
    }

    public PrintStream(String str, String str2) throws UnsupportedEncodingException, FileNotFoundException {
        this(false, toCharset(str2), (OutputStream) new FileOutputStream(str));
    }

    public PrintStream(File file) throws FileNotFoundException {
        this(false, (OutputStream) new FileOutputStream(file));
    }

    public PrintStream(File file, String str) throws UnsupportedEncodingException, FileNotFoundException {
        this(false, toCharset(str), (OutputStream) new FileOutputStream(file));
    }

    private void ensureOpen() throws IOException {
        if (this.out == null) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public void flush() {
        synchronized (this) {
            try {
                ensureOpen();
                this.out.flush();
            } catch (IOException e) {
                this.trouble = true;
            }
        }
    }

    private BufferedWriter getTextOut() {
        if (this.textOut == null) {
            this.charOut = this.charset != null ? new OutputStreamWriter(this, this.charset) : new OutputStreamWriter(this);
            this.textOut = new BufferedWriter(this.charOut);
        }
        return this.textOut;
    }

    @Override
    public void close() {
        synchronized (this) {
            if (!this.closing) {
                this.closing = true;
                try {
                    if (this.textOut != null) {
                        this.textOut.close();
                    }
                    this.out.close();
                } catch (IOException e) {
                    this.trouble = true;
                }
                this.textOut = null;
                this.charOut = null;
                this.out = null;
            }
        }
    }

    public boolean checkError() {
        if (this.out != null) {
            flush();
        }
        if (this.out instanceof PrintStream) {
            return ((PrintStream) this.out).checkError();
        }
        return this.trouble;
    }

    protected void setError() {
        this.trouble = true;
    }

    protected void clearError() {
        this.trouble = false;
    }

    @Override
    public void write(int i) {
        try {
            synchronized (this) {
                ensureOpen();
                this.out.write(i);
                if (i == 10 && this.autoFlush) {
                    this.out.flush();
                }
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
    }

    @Override
    public void write(byte[] bArr, int i, int i2) {
        try {
            synchronized (this) {
                ensureOpen();
                this.out.write(bArr, i, i2);
                if (this.autoFlush) {
                    this.out.flush();
                }
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
    }

    private void write(char[] cArr) {
        try {
            synchronized (this) {
                ensureOpen();
                BufferedWriter textOut = getTextOut();
                textOut.write(cArr);
                textOut.flushBuffer();
                this.charOut.flushBuffer();
                if (this.autoFlush) {
                    for (char c : cArr) {
                        if (c == '\n') {
                            this.out.flush();
                        }
                    }
                }
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
    }

    private void write(String str) {
        try {
            synchronized (this) {
                ensureOpen();
                BufferedWriter textOut = getTextOut();
                textOut.write(str);
                textOut.flushBuffer();
                this.charOut.flushBuffer();
                if (this.autoFlush && str.indexOf(10) >= 0) {
                    this.out.flush();
                }
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
    }

    private void newLine() {
        try {
            synchronized (this) {
                ensureOpen();
                BufferedWriter textOut = getTextOut();
                textOut.newLine();
                textOut.flushBuffer();
                this.charOut.flushBuffer();
                if (this.autoFlush) {
                    this.out.flush();
                }
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
    }

    public void print(boolean z) {
        write(z ? "true" : "false");
    }

    public void print(char c) {
        write(String.valueOf(c));
    }

    public void print(int i) {
        write(String.valueOf(i));
    }

    public void print(long j) {
        write(String.valueOf(j));
    }

    public void print(float f) {
        write(String.valueOf(f));
    }

    public void print(double d) {
        write(String.valueOf(d));
    }

    public void print(char[] cArr) {
        write(cArr);
    }

    public void print(String str) {
        if (str == null) {
            str = "null";
        }
        write(str);
    }

    public void print(Object obj) {
        write(String.valueOf(obj));
    }

    public void println() {
        newLine();
    }

    public void println(boolean z) {
        synchronized (this) {
            print(z);
            newLine();
        }
    }

    public void println(char c) {
        synchronized (this) {
            print(c);
            newLine();
        }
    }

    public void println(int i) {
        synchronized (this) {
            print(i);
            newLine();
        }
    }

    public void println(long j) {
        synchronized (this) {
            print(j);
            newLine();
        }
    }

    public void println(float f) {
        synchronized (this) {
            print(f);
            newLine();
        }
    }

    public void println(double d) {
        synchronized (this) {
            print(d);
            newLine();
        }
    }

    public void println(char[] cArr) {
        synchronized (this) {
            print(cArr);
            newLine();
        }
    }

    public void println(String str) {
        synchronized (this) {
            print(str);
            newLine();
        }
    }

    public void println(Object obj) {
        String strValueOf = String.valueOf(obj);
        synchronized (this) {
            print(strValueOf);
            newLine();
        }
    }

    public PrintStream printf(String str, Object... objArr) {
        return format(str, objArr);
    }

    public PrintStream printf(Locale locale, String str, Object... objArr) {
        return format(locale, str, objArr);
    }

    public PrintStream format(String str, Object... objArr) {
        try {
            synchronized (this) {
                ensureOpen();
                if (this.formatter == null || this.formatter.locale() != Locale.getDefault()) {
                    this.formatter = new Formatter((Appendable) this);
                }
                this.formatter.format(Locale.getDefault(), str, objArr);
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
        return this;
    }

    public PrintStream format(Locale locale, String str, Object... objArr) {
        try {
            synchronized (this) {
                ensureOpen();
                if (this.formatter == null || this.formatter.locale() != locale) {
                    this.formatter = new Formatter(this, locale);
                }
                this.formatter.format(locale, str, objArr);
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence charSequence) {
        if (charSequence == null) {
            print("null");
        } else {
            print(charSequence.toString());
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence charSequence, int i, int i2) {
        if (charSequence == null) {
            charSequence = "null";
        }
        write(charSequence.subSequence(i, i2).toString());
        return this;
    }

    @Override
    public PrintStream append(char c) {
        print(c);
        return this;
    }
}
