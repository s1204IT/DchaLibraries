package java.io;

import java.util.Arrays;

public class StreamTokenizer {
    private static final byte CT_ALPHA = 4;
    private static final byte CT_COMMENT = 16;
    private static final byte CT_DIGIT = 2;
    private static final byte CT_QUOTE = 8;
    private static final byte CT_WHITESPACE = 1;
    private static final int NEED_CHAR = Integer.MAX_VALUE;
    private static final int SKIP_LF = 2147483646;
    public static final int TT_EOF = -1;
    public static final int TT_EOL = 10;
    private static final int TT_NOTHING = -4;
    public static final int TT_NUMBER = -2;
    public static final int TT_WORD = -3;
    private int LINENO;
    private char[] buf;
    private byte[] ctype;
    private boolean eolIsSignificantP;
    private boolean forceLower;
    private InputStream input;
    public double nval;
    private int peekc;
    private boolean pushedBack;
    private Reader reader;
    private boolean slashSlashCommentsP;
    private boolean slashStarCommentsP;
    public String sval;
    public int ttype;

    private StreamTokenizer() {
        this.reader = null;
        this.input = null;
        this.buf = new char[20];
        this.peekc = Integer.MAX_VALUE;
        this.LINENO = 1;
        this.eolIsSignificantP = false;
        this.slashSlashCommentsP = false;
        this.slashStarCommentsP = false;
        this.ctype = new byte[256];
        this.ttype = -4;
        wordChars(97, 122);
        wordChars(65, 90);
        wordChars(160, 255);
        whitespaceChars(0, 32);
        commentChar(47);
        quoteChar(34);
        quoteChar(39);
        parseNumbers();
    }

    @Deprecated
    public StreamTokenizer(InputStream inputStream) {
        this();
        if (inputStream == null) {
            throw new NullPointerException();
        }
        this.input = inputStream;
    }

    public StreamTokenizer(Reader reader) {
        this();
        if (reader == null) {
            throw new NullPointerException();
        }
        this.reader = reader;
    }

    public void resetSyntax() {
        int length = this.ctype.length;
        while (true) {
            length--;
            if (length >= 0) {
                this.ctype[length] = 0;
            } else {
                return;
            }
        }
    }

    public void wordChars(int i, int i2) {
        if (i < 0) {
            i = 0;
        }
        if (i2 >= this.ctype.length) {
            i2 = this.ctype.length - 1;
        }
        while (i <= i2) {
            byte[] bArr = this.ctype;
            bArr[i] = (byte) (bArr[i] | 4);
            i++;
        }
    }

    public void whitespaceChars(int i, int i2) {
        if (i < 0) {
            i = 0;
        }
        if (i2 >= this.ctype.length) {
            i2 = this.ctype.length - 1;
        }
        while (i <= i2) {
            this.ctype[i] = 1;
            i++;
        }
    }

    public void ordinaryChars(int i, int i2) {
        if (i < 0) {
            i = 0;
        }
        if (i2 >= this.ctype.length) {
            i2 = this.ctype.length - 1;
        }
        while (i <= i2) {
            this.ctype[i] = 0;
            i++;
        }
    }

    public void ordinaryChar(int i) {
        if (i >= 0 && i < this.ctype.length) {
            this.ctype[i] = 0;
        }
    }

    public void commentChar(int i) {
        if (i >= 0 && i < this.ctype.length) {
            this.ctype[i] = 16;
        }
    }

    public void quoteChar(int i) {
        if (i >= 0 && i < this.ctype.length) {
            this.ctype[i] = 8;
        }
    }

    public void parseNumbers() {
        for (int i = 48; i <= 57; i++) {
            byte[] bArr = this.ctype;
            bArr[i] = (byte) (bArr[i] | 2);
        }
        byte[] bArr2 = this.ctype;
        bArr2[46] = (byte) (bArr2[46] | 2);
        byte[] bArr3 = this.ctype;
        bArr3[45] = (byte) (bArr3[45] | 2);
    }

    public void eolIsSignificant(boolean z) {
        this.eolIsSignificantP = z;
    }

    public void slashStarComments(boolean z) {
        this.slashStarCommentsP = z;
    }

    public void slashSlashComments(boolean z) {
        this.slashSlashCommentsP = z;
    }

    public void lowerCaseMode(boolean z) {
        this.forceLower = z;
    }

    private int read() throws IOException {
        if (this.reader != null) {
            return this.reader.read();
        }
        if (this.input != null) {
            return this.input.read();
        }
        throw new IllegalStateException();
    }

    public int nextToken() throws IOException {
        byte b;
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        boolean z;
        int i7 = 0;
        if (this.pushedBack) {
            this.pushedBack = false;
            return this.ttype;
        }
        byte[] bArr = this.ctype;
        this.sval = null;
        int i8 = this.peekc;
        if (i8 < 0) {
            i8 = Integer.MAX_VALUE;
        }
        if (i8 == SKIP_LF) {
            i8 = read();
            if (i8 < 0) {
                this.ttype = -1;
                return -1;
            }
            if (i8 == 10) {
                i8 = Integer.MAX_VALUE;
            }
        }
        if (i8 == Integer.MAX_VALUE && (i8 = read()) < 0) {
            this.ttype = -1;
            return -1;
        }
        this.ttype = i8;
        this.peekc = Integer.MAX_VALUE;
        if (i8 < 256) {
            b = bArr[i8];
            while ((b & 1) != 0) {
                if (i8 == 13) {
                    this.LINENO++;
                    if (this.eolIsSignificantP) {
                        this.peekc = SKIP_LF;
                        this.ttype = 10;
                        return 10;
                    }
                    i8 = read();
                    if (i8 == 10) {
                        i8 = read();
                    }
                } else {
                    if (i8 == 10) {
                        this.LINENO++;
                        if (this.eolIsSignificantP) {
                            this.ttype = 10;
                            return 10;
                        }
                    }
                    i8 = read();
                }
                if (i8 < 0) {
                    this.ttype = -1;
                    return -1;
                }
                if (i8 < 256) {
                    b = bArr[i8];
                }
            }
            if ((b & 2) != 0) {
                if (i8 == 45) {
                    i8 = read();
                    if (i8 != 46 && (i8 < 48 || i8 > 57)) {
                        this.peekc = i8;
                        this.ttype = 45;
                        return 45;
                    }
                    z = true;
                } else {
                    z = false;
                }
                double d = 0.0d;
                int i9 = 0;
                while (true) {
                    if (i8 != 46 || i7 != 0) {
                        if (48 > i8 || i8 > 57) {
                            break;
                        }
                        d = (d * 10.0d) + ((double) (i8 - 48));
                        i9 += i7;
                    } else {
                        i7 = 1;
                    }
                    i8 = read();
                }
                this.peekc = i8;
                if (i9 != 0) {
                    double d2 = 10.0d;
                    for (int i10 = i9 - 1; i10 > 0; i10--) {
                        d2 *= 10.0d;
                    }
                    d /= d2;
                }
                if (z) {
                    d = -d;
                }
                this.nval = d;
                this.ttype = -2;
                return -2;
            }
            if ((b & 4) != 0) {
                int i11 = i8;
                int i12 = 0;
                while (true) {
                    if (i12 >= this.buf.length) {
                        this.buf = Arrays.copyOf(this.buf, this.buf.length * 2);
                    }
                    i6 = i12 + 1;
                    this.buf[i12] = (char) i11;
                    i11 = read();
                    if (((i11 < 0 ? (byte) 1 : i11 < 256 ? bArr[i11] : (byte) 4) & 6) == 0) {
                        break;
                    }
                    i12 = i6;
                }
                this.peekc = i11;
                this.sval = String.copyValueOf(this.buf, 0, i6);
                if (this.forceLower) {
                    this.sval = this.sval.toLowerCase();
                }
                this.ttype = -3;
                return -3;
            }
            if ((b & 8) != 0) {
                this.ttype = i8;
                int i13 = read();
                int i14 = 0;
                while (i13 >= 0 && i13 != this.ttype && i13 != 10 && i13 != 13) {
                    if (i13 == 92) {
                        i13 = read();
                        if (i13 >= 48 && i13 <= 55) {
                            int i15 = i13 - 48;
                            i5 = read();
                            if (48 <= i5 && i5 <= 55) {
                                i15 = (i15 << 3) + (i5 - 48);
                                i5 = read();
                                if (48 <= i5 && i5 <= 55 && i13 <= 51) {
                                    i15 = (i15 << 3) + (i5 - 48);
                                    i5 = read();
                                }
                            }
                            i13 = i15;
                        } else {
                            if (i13 == 102) {
                                i13 = 12;
                            } else if (i13 == 110) {
                                i13 = 10;
                            } else if (i13 == 114) {
                                i13 = 13;
                            } else if (i13 == 116) {
                                i13 = 9;
                            } else if (i13 != 118) {
                                switch (i13) {
                                    case 97:
                                        i13 = 7;
                                        break;
                                    case 98:
                                        i13 = 8;
                                        break;
                                }
                            } else {
                                i13 = 11;
                            }
                            i5 = read();
                        }
                        i4 = i5;
                    } else {
                        i4 = read();
                    }
                    if (i14 >= this.buf.length) {
                        this.buf = Arrays.copyOf(this.buf, this.buf.length * 2);
                    }
                    this.buf[i14] = (char) i13;
                    i13 = i4;
                    i14++;
                }
                if (i13 == this.ttype) {
                    i13 = Integer.MAX_VALUE;
                }
                this.peekc = i13;
                this.sval = String.copyValueOf(this.buf, 0, i14);
                return this.ttype;
            }
            if (i8 == 47 && (this.slashSlashCommentsP || this.slashStarCommentsP)) {
                int i16 = read();
                if (i16 == 42 && this.slashStarCommentsP) {
                    do {
                        int i17 = read();
                        if (i17 != 47 || i7 != 42) {
                            if (i17 == 13) {
                                this.LINENO++;
                                i17 = read();
                                if (i17 == 10) {
                                    i17 = read();
                                }
                            } else if (i17 == 10) {
                                this.LINENO++;
                                i17 = read();
                            }
                            i7 = i17;
                        } else {
                            return nextToken();
                        }
                    } while (i7 >= 0);
                    this.ttype = -1;
                    return -1;
                }
                if (i16 == 47 && this.slashSlashCommentsP) {
                    do {
                        i3 = read();
                        if (i3 == 10 || i3 == 13) {
                            break;
                        }
                    } while (i3 >= 0);
                    this.peekc = i3;
                    return nextToken();
                }
                if ((bArr[47] & 16) != 0) {
                    do {
                        i2 = read();
                        if (i2 == 10 || i2 == 13) {
                            break;
                        }
                    } while (i2 >= 0);
                    this.peekc = i2;
                    return nextToken();
                }
                this.peekc = i16;
                this.ttype = 47;
                return 47;
            }
            if ((b & 16) != 0) {
                do {
                    i = read();
                    if (i == 10 || i == 13) {
                        break;
                    }
                } while (i >= 0);
                this.peekc = i;
                return nextToken();
            }
            this.ttype = i8;
            return i8;
        }
        b = 4;
    }

    public void pushBack() {
        if (this.ttype != -4) {
            this.pushedBack = true;
        }
    }

    public int lineno() {
        return this.LINENO;
    }

    public String toString() {
        String str;
        int i = this.ttype;
        if (i == 10) {
            str = "EOL";
        } else {
            switch (i) {
                case -4:
                    str = "NOTHING";
                    break;
                case -3:
                    str = this.sval;
                    break;
                case -2:
                    str = "n=" + this.nval;
                    break;
                case -1:
                    str = "EOF";
                    break;
                default:
                    if (this.ttype < 256 && (this.ctype[this.ttype] & 8) != 0) {
                        str = this.sval;
                    } else {
                        str = new String(new char[]{'\'', (char) this.ttype, '\''});
                    }
                    break;
            }
        }
        return "Token[" + str + "], line " + this.LINENO;
    }
}
