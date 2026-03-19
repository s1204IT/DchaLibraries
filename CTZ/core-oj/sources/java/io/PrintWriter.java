package java.io;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.AccessController;
import java.util.Formatter;
import java.util.Locale;
import java.util.Objects;
import sun.security.action.GetPropertyAction;

public class PrintWriter extends Writer {
    private final boolean autoFlush;
    private Formatter formatter;
    private final String lineSeparator;
    protected Writer out;
    private PrintStream psOut;
    private boolean trouble;

    private static Charset toCharset(String str) throws UnsupportedEncodingException {
        Objects.requireNonNull(str, "charsetName");
        try {
            return Charset.forName(str);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(str);
        }
    }

    public PrintWriter(Writer writer) {
        this(writer, false);
    }

    public PrintWriter(Writer writer, boolean z) {
        super(writer);
        this.trouble = false;
        this.psOut = null;
        this.out = writer;
        this.autoFlush = z;
        this.lineSeparator = (String) AccessController.doPrivileged(new GetPropertyAction("line.separator"));
    }

    public PrintWriter(OutputStream outputStream) {
        this(outputStream, false);
    }

    public PrintWriter(OutputStream outputStream, boolean z) {
        this(new BufferedWriter(new OutputStreamWriter(outputStream)), z);
        if (outputStream instanceof PrintStream) {
            this.psOut = (PrintStream) outputStream;
        }
    }

    public PrintWriter(String str) throws FileNotFoundException {
        this((Writer) new BufferedWriter(new OutputStreamWriter(new FileOutputStream(str))), false);
    }

    private PrintWriter(Charset charset, File file) throws FileNotFoundException {
        this((Writer) new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset)), false);
    }

    public PrintWriter(String str, String str2) throws UnsupportedEncodingException, FileNotFoundException {
        this(toCharset(str2), new File(str));
    }

    public PrintWriter(File file) throws FileNotFoundException {
        this((Writer) new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))), false);
    }

    public PrintWriter(File file, String str) throws UnsupportedEncodingException, FileNotFoundException {
        this(toCharset(str), file);
    }

    private void ensureOpen() throws IOException {
        if (this.out == null) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public void flush() {
        try {
            synchronized (this.lock) {
                ensureOpen();
                this.out.flush();
            }
        } catch (IOException e) {
            this.trouble = true;
        }
    }

    @Override
    public void close() {
        try {
            synchronized (this.lock) {
                if (this.out == null) {
                    return;
                }
                this.out.close();
                this.out = null;
            }
        } catch (IOException e) {
            this.trouble = true;
        }
    }

    public boolean checkError() {
        if (this.out != null) {
            flush();
        }
        if (this.out instanceof PrintWriter) {
            return ((PrintWriter) this.out).checkError();
        }
        if (this.psOut != null) {
            return this.psOut.checkError();
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
            synchronized (this.lock) {
                ensureOpen();
                this.out.write(i);
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
    }

    @Override
    public void write(char[] cArr, int i, int i2) {
        try {
            synchronized (this.lock) {
                ensureOpen();
                this.out.write(cArr, i, i2);
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
    }

    @Override
    public void write(char[] cArr) {
        write(cArr, 0, cArr.length);
    }

    @Override
    public void write(String str, int i, int i2) {
        try {
            synchronized (this.lock) {
                ensureOpen();
                this.out.write(str, i, i2);
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
    }

    @Override
    public void write(String str) {
        write(str, 0, str.length());
    }

    private void newLine() {
        try {
            synchronized (this.lock) {
                ensureOpen();
                this.out.write(this.lineSeparator);
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
        write(c);
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
        synchronized (this.lock) {
            print(z);
            println();
        }
    }

    public void println(char c) {
        synchronized (this.lock) {
            print(c);
            println();
        }
    }

    public void println(int i) {
        synchronized (this.lock) {
            print(i);
            println();
        }
    }

    public void println(long j) {
        synchronized (this.lock) {
            print(j);
            println();
        }
    }

    public void println(float f) {
        synchronized (this.lock) {
            print(f);
            println();
        }
    }

    public void println(double d) {
        synchronized (this.lock) {
            print(d);
            println();
        }
    }

    public void println(char[] cArr) {
        synchronized (this.lock) {
            print(cArr);
            println();
        }
    }

    public void println(String str) {
        synchronized (this.lock) {
            print(str);
            println();
        }
    }

    public void println(Object obj) {
        String strValueOf = String.valueOf(obj);
        synchronized (this.lock) {
            print(strValueOf);
            println();
        }
    }

    public PrintWriter printf(String str, Object... objArr) {
        return format(str, objArr);
    }

    public PrintWriter printf(Locale locale, String str, Object... objArr) {
        return format(locale, str, objArr);
    }

    public PrintWriter format(String str, Object... objArr) {
        try {
            synchronized (this.lock) {
                ensureOpen();
                if (this.formatter == null || this.formatter.locale() != Locale.getDefault()) {
                    this.formatter = new Formatter(this);
                }
                this.formatter.format(Locale.getDefault(), str, objArr);
                if (this.autoFlush) {
                    this.out.flush();
                }
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
        return this;
    }

    public PrintWriter format(Locale locale, String str, Object... objArr) {
        try {
            synchronized (this.lock) {
                ensureOpen();
                if (this.formatter == null || this.formatter.locale() != locale) {
                    this.formatter = new Formatter(this, locale);
                }
                this.formatter.format(locale, str, objArr);
                if (this.autoFlush) {
                    this.out.flush();
                }
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e2) {
            this.trouble = true;
        }
        return this;
    }

    @Override
    public PrintWriter append(CharSequence charSequence) {
        if (charSequence == null) {
            write("null");
        } else {
            write(charSequence.toString());
        }
        return this;
    }

    @Override
    public PrintWriter append(CharSequence charSequence, int i, int i2) {
        if (charSequence == null) {
            charSequence = "null";
        }
        write(charSequence.subSequence(i, i2).toString());
        return this;
    }

    @Override
    public PrintWriter append(char c) {
        write(c);
        return this;
    }
}
