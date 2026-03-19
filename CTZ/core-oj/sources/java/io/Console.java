package java.io;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Formatter;
import sun.nio.cs.StreamDecoder;
import sun.nio.cs.StreamEncoder;

public final class Console implements Flushable {
    static final boolean $assertionsDisabled = false;
    private static Console cons;
    private static boolean echoOff;
    private Charset cs;
    private Formatter formatter;
    private Writer out;
    private PrintWriter pw;
    private char[] rcb;
    private Object readLock;
    private Reader reader;
    private Object writeLock;

    private static native boolean echo(boolean z) throws IOException;

    private static native String encoding();

    private static native boolean istty();

    public PrintWriter writer() {
        return this.pw;
    }

    public Reader reader() {
        return this.reader;
    }

    public Console format(String str, Object... objArr) {
        this.formatter.format(str, objArr).flush();
        return this;
    }

    public Console printf(String str, Object... objArr) {
        return format(str, objArr);
    }

    public String readLine(String str, Object... objArr) {
        String str2;
        synchronized (this.writeLock) {
            synchronized (this.readLock) {
                if (str.length() != 0) {
                    this.pw.format(str, objArr);
                }
                try {
                    char[] cArr = readline(false);
                    if (cArr != null) {
                        str2 = new String(cArr);
                    } else {
                        str2 = null;
                    }
                } catch (IOException e) {
                    throw new IOError(e);
                }
            }
        }
        return str2;
    }

    public String readLine() {
        return readLine("", new Object[0]);
    }

    public char[] readPassword(String str, Object... objArr) {
        char[] cArr;
        synchronized (this.writeLock) {
            synchronized (this.readLock) {
                try {
                    try {
                        echoOff = echo(false);
                        IOError iOError = null;
                        try {
                            try {
                                if (str.length() != 0) {
                                    this.pw.format(str, objArr);
                                }
                                cArr = readline(true);
                                try {
                                    echoOff = echo(true);
                                } catch (IOException e) {
                                    iOError = new IOError(e);
                                }
                                if (iOError != null) {
                                    throw iOError;
                                }
                                this.pw.println();
                            } catch (Throwable th) {
                                try {
                                    echoOff = echo(true);
                                } catch (IOException e2) {
                                    iOError = new IOError(e2);
                                }
                                if (iOError != null) {
                                    throw iOError;
                                }
                                throw th;
                            }
                        } catch (IOException e3) {
                            IOError iOError2 = new IOError(e3);
                            try {
                                echoOff = echo(true);
                                throw iOError2;
                            } catch (IOException e4) {
                                iOError2.addSuppressed(e4);
                                throw iOError2;
                            }
                        }
                    } catch (IOException e5) {
                        throw new IOError(e5);
                    }
                } catch (Throwable th2) {
                    throw th2;
                }
            }
        }
        return cArr;
    }

    public char[] readPassword() {
        return readPassword("", new Object[0]);
    }

    @Override
    public void flush() {
        this.pw.flush();
    }

    private char[] readline(boolean z) throws IOException {
        int i = this.reader.read(this.rcb, 0, this.rcb.length);
        if (i < 0) {
            return null;
        }
        int i2 = i - 1;
        if (this.rcb[i2] == '\r') {
            i--;
        } else if (this.rcb[i2] == '\n' && i - 1 > 0 && this.rcb[i - 1] == '\r') {
            i--;
        }
        char[] cArr = new char[i];
        if (i > 0) {
            System.arraycopy((Object) this.rcb, 0, (Object) cArr, 0, i);
            if (z) {
                Arrays.fill(this.rcb, 0, i, ' ');
            }
        }
        return cArr;
    }

    private char[] grow() {
        char[] cArr = new char[this.rcb.length * 2];
        System.arraycopy((Object) this.rcb, 0, (Object) cArr, 0, this.rcb.length);
        this.rcb = cArr;
        return this.rcb;
    }

    class LineReader extends Reader {
        private Reader in;
        private char[] cb = new char[1024];
        private int nChars = 0;
        private int nextChar = 0;
        boolean leftoverLF = false;

        LineReader(Reader reader) {
            this.in = reader;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean ready() throws IOException {
            return this.in.ready();
        }

        @Override
        public int read(char[] cArr, int i, int i2) throws IOException {
            int i3;
            int i4;
            int i5 = i + i2;
            if (i >= 0 && i <= cArr.length && i2 >= 0 && i5 >= 0 && i5 <= cArr.length) {
                synchronized (Console.this.readLock) {
                    int length = i5;
                    boolean z = false;
                    char[] cArrGrow = cArr;
                    int i6 = i;
                    do {
                        if (this.nextChar >= this.nChars) {
                            do {
                                i4 = this.in.read(this.cb, 0, this.cb.length);
                            } while (i4 == 0);
                            if (i4 > 0) {
                                this.nChars = i4;
                                this.nextChar = 0;
                                if (i4 < this.cb.length) {
                                    int i7 = i4 - 1;
                                    if (this.cb[i7] != '\n' && this.cb[i7] != '\r') {
                                        z = true;
                                    }
                                }
                            } else {
                                int i8 = i6 - i;
                                if (i8 == 0) {
                                    return -1;
                                }
                                return i8;
                            }
                        }
                        if (this.leftoverLF && cArrGrow == Console.this.rcb && this.cb[this.nextChar] == '\n') {
                            this.nextChar++;
                        }
                        this.leftoverLF = false;
                        while (this.nextChar < this.nChars) {
                            int i9 = i6 + 1;
                            char c = this.cb[this.nextChar];
                            cArrGrow[i6] = c;
                            char[] cArr2 = this.cb;
                            int i10 = this.nextChar;
                            this.nextChar = i10 + 1;
                            cArr2[i10] = 0;
                            if (c == '\n') {
                                return i9 - i;
                            }
                            if (c == '\r') {
                                if (i9 == length) {
                                    if (cArrGrow == Console.this.rcb) {
                                        cArrGrow = Console.this.grow();
                                        int length2 = cArrGrow.length;
                                    } else {
                                        this.leftoverLF = true;
                                        return i9 - i;
                                    }
                                }
                                if (this.nextChar == this.nChars && this.in.ready()) {
                                    this.nChars = this.in.read(this.cb, 0, this.cb.length);
                                    this.nextChar = 0;
                                }
                                if (this.nextChar < this.nChars && this.cb[this.nextChar] == '\n') {
                                    i3 = i9 + 1;
                                    cArrGrow[i9] = '\n';
                                    this.nextChar++;
                                } else {
                                    i3 = i9;
                                }
                                return i3 - i;
                            }
                            if (i9 == length) {
                                if (cArrGrow == Console.this.rcb) {
                                    cArrGrow = Console.this.grow();
                                    length = cArrGrow.length;
                                } else {
                                    return i9 - i;
                                }
                            }
                            i6 = i9;
                        }
                    } while (!z);
                    return i6 - i;
                }
            }
            throw new IndexOutOfBoundsException();
        }
    }

    public static Console console() {
        if (istty()) {
            if (cons == null) {
                cons = new Console();
            }
            return cons;
        }
        return null;
    }

    private Console() {
        this(new FileInputStream(FileDescriptor.in), new FileOutputStream(FileDescriptor.out));
    }

    private Console(InputStream inputStream, OutputStream outputStream) {
        this.readLock = new Object();
        this.writeLock = new Object();
        String strEncoding = encoding();
        if (strEncoding != null) {
            try {
                this.cs = Charset.forName(strEncoding);
            } catch (Exception e) {
            }
        }
        if (this.cs == null) {
            this.cs = Charset.defaultCharset();
        }
        this.out = StreamEncoder.forOutputStreamWriter(outputStream, this.writeLock, this.cs);
        this.pw = new PrintWriter(this.out, true) {
            @Override
            public void close() {
            }
        };
        this.formatter = new Formatter(this.out);
        this.reader = new LineReader(StreamDecoder.forInputStreamReader(inputStream, this.readLock, this.cs));
        this.rcb = new char[1024];
    }
}
