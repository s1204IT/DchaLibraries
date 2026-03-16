package java.io;

import dalvik.bytecode.Opcodes;
import java.util.Locale;

public class StreamTokenizer {
    private static final byte TOKEN_COMMENT = 1;
    private static final byte TOKEN_DIGIT = 16;
    private static final byte TOKEN_QUOTE = 2;
    private static final byte TOKEN_WHITE = 4;
    private static final byte TOKEN_WORD = 8;
    public static final int TT_EOF = -1;
    public static final int TT_EOL = 10;
    public static final int TT_NUMBER = -2;
    private static final int TT_UNKNOWN = -4;
    public static final int TT_WORD = -3;
    private boolean forceLowercase;
    private Reader inReader;
    private InputStream inStream;
    private boolean isEOLSignificant;
    private boolean lastCr;
    private int lineNumber;
    public double nval;
    private int peekChar;
    private boolean pushBackToken;
    private boolean slashSlashComments;
    private boolean slashStarComments;
    public String sval;
    private byte[] tokenTypes;
    public int ttype;

    private StreamTokenizer() {
        this.ttype = -4;
        this.tokenTypes = new byte[256];
        this.lineNumber = 1;
        this.peekChar = -2;
        wordChars(65, 90);
        wordChars(97, 122);
        wordChars(Opcodes.OP_AND_LONG, Opcodes.OP_CONST_CLASS_JUMBO);
        whitespaceChars(0, 32);
        commentChar(47);
        quoteChar(34);
        quoteChar(39);
        parseNumbers();
    }

    @Deprecated
    public StreamTokenizer(InputStream is) {
        this();
        if (is == null) {
            throw new NullPointerException("is == null");
        }
        this.inStream = is;
    }

    public StreamTokenizer(Reader r) {
        this();
        if (r == null) {
            throw new NullPointerException("r == null");
        }
        this.inReader = r;
    }

    public void commentChar(int ch) {
        if (ch >= 0 && ch < this.tokenTypes.length) {
            this.tokenTypes[ch] = 1;
        }
    }

    public void eolIsSignificant(boolean flag) {
        this.isEOLSignificant = flag;
    }

    public int lineno() {
        return this.lineNumber;
    }

    public void lowerCaseMode(boolean flag) {
        this.forceLowercase = flag;
    }

    public int nextToken() throws IOException {
        int currentChar;
        int currentChar2;
        if (this.pushBackToken) {
            this.pushBackToken = false;
            if (this.ttype != -4) {
                return this.ttype;
            }
        }
        this.sval = null;
        int currentChar3 = this.peekChar == -2 ? read() : this.peekChar;
        if (this.lastCr && currentChar3 == 10) {
            this.lastCr = false;
            currentChar3 = read();
        }
        if (currentChar3 == -1) {
            this.ttype = -1;
            return -1;
        }
        byte currentType = currentChar3 > 255 ? (byte) 8 : this.tokenTypes[currentChar3];
        while ((currentType & 4) != 0) {
            if (currentChar3 == 13) {
                this.lineNumber++;
                if (this.isEOLSignificant) {
                    this.lastCr = true;
                    this.peekChar = -2;
                    this.ttype = 10;
                    return 10;
                }
                currentChar3 = read();
                if (currentChar3 == 10) {
                    currentChar3 = read();
                }
            } else if (currentChar3 == 10) {
                this.lineNumber++;
                if (this.isEOLSignificant) {
                    this.peekChar = -2;
                    this.ttype = 10;
                    return 10;
                }
                currentChar3 = read();
            } else {
                currentChar3 = read();
            }
            if (currentChar3 == -1) {
                this.ttype = -1;
                return -1;
            }
            currentType = currentChar3 > 255 ? (byte) 8 : this.tokenTypes[currentChar3];
        }
        if ((currentType & 16) != 0) {
            StringBuilder digits = new StringBuilder(20);
            boolean haveDecimal = false;
            boolean checkJustNegative = currentChar3 == 45;
            while (true) {
                if (currentChar3 == 46) {
                    haveDecimal = true;
                }
                digits.append((char) currentChar3);
                currentChar3 = read();
                if (currentChar3 < 48 || currentChar3 > 57) {
                    if (haveDecimal || currentChar3 != 46) {
                        break;
                    }
                }
            }
            this.peekChar = currentChar3;
            if (checkJustNegative && digits.length() == 1) {
                this.ttype = 45;
                return 45;
            }
            try {
                this.nval = Double.valueOf(digits.toString()).doubleValue();
            } catch (NumberFormatException e) {
                this.nval = 0.0d;
            }
            this.ttype = -2;
            return -2;
        }
        if ((currentType & 8) != 0) {
            StringBuilder word = new StringBuilder(20);
            while (true) {
                word.append((char) currentChar3);
                currentChar3 = read();
                if (currentChar3 == -1 || (currentChar3 < 256 && (this.tokenTypes[currentChar3] & Character.OTHER_PUNCTUATION) == 0)) {
                    break;
                }
            }
            this.peekChar = currentChar3;
            this.sval = word.toString();
            if (this.forceLowercase) {
                this.sval = this.sval.toLowerCase(Locale.getDefault());
            }
            this.ttype = -3;
            return -3;
        }
        if (currentType == 2) {
            int matchQuote = currentChar3;
            StringBuilder quoteString = new StringBuilder();
            int peekOne = read();
            while (peekOne >= 0 && peekOne != matchQuote && peekOne != 13 && peekOne != 10) {
                boolean readPeek = true;
                if (peekOne == 92) {
                    int c1 = read();
                    if (c1 <= 55 && c1 >= 48) {
                        int digitValue = c1 - 48;
                        int c12 = read();
                        if (c12 > 55 || c12 < 48) {
                            readPeek = false;
                        } else {
                            digitValue = (digitValue * 8) + (c12 - 48);
                            c12 = read();
                            if (digitValue > 31 || c12 > 55 || c12 < 48) {
                                readPeek = false;
                            } else {
                                digitValue = (digitValue * 8) + (c12 - 48);
                            }
                        }
                        if (!readPeek) {
                            quoteString.append((char) digitValue);
                            peekOne = c12;
                        } else {
                            peekOne = digitValue;
                        }
                    } else {
                        switch (c1) {
                            case Opcodes.OP_SGET_WIDE:
                                peekOne = 7;
                                break;
                            case Opcodes.OP_SGET_OBJECT:
                                peekOne = 8;
                                break;
                            case Opcodes.OP_SGET_SHORT:
                                peekOne = 12;
                                break;
                            case Opcodes.OP_INVOKE_VIRTUAL:
                                peekOne = 10;
                                break;
                            case Opcodes.OP_INVOKE_INTERFACE:
                                peekOne = 13;
                                break;
                            case Opcodes.OP_INVOKE_VIRTUAL_RANGE:
                                peekOne = 9;
                                break;
                            case Opcodes.OP_INVOKE_DIRECT_RANGE:
                                peekOne = 11;
                                break;
                            default:
                                peekOne = c1;
                                break;
                        }
                    }
                }
                if (readPeek) {
                    quoteString.append((char) peekOne);
                    peekOne = read();
                }
            }
            if (peekOne == matchQuote) {
                peekOne = read();
            }
            this.peekChar = peekOne;
            this.ttype = matchQuote;
            this.sval = quoteString.toString();
            return this.ttype;
        }
        if (currentChar3 == 47 && (this.slashSlashComments || this.slashStarComments)) {
            currentChar3 = read();
            if (currentChar3 == 42 && this.slashStarComments) {
                int peekOne2 = read();
                while (true) {
                    int currentChar4 = peekOne2;
                    peekOne2 = read();
                    if (currentChar4 == -1) {
                        this.peekChar = -1;
                        this.ttype = -1;
                        return -1;
                    }
                    if (currentChar4 == 13) {
                        if (peekOne2 == 10) {
                            peekOne2 = read();
                        }
                        this.lineNumber++;
                    } else if (currentChar4 == 10) {
                        this.lineNumber++;
                    } else if (currentChar4 == 42 && peekOne2 == 47) {
                        this.peekChar = read();
                        return nextToken();
                    }
                }
            } else {
                if (currentChar3 == 47 && this.slashSlashComments) {
                    do {
                        currentChar2 = read();
                        if (currentChar2 < 0 || currentChar2 == 13) {
                            break;
                        }
                    } while (currentChar2 != 10);
                    this.peekChar = currentChar2;
                    return nextToken();
                }
                if (currentType != 1) {
                    this.peekChar = currentChar3;
                    this.ttype = 47;
                    return 47;
                }
            }
        }
        if (currentType == 1) {
            do {
                currentChar = read();
                if (currentChar < 0 || currentChar == 13) {
                    break;
                }
            } while (currentChar != 10);
            this.peekChar = currentChar;
            return nextToken();
        }
        this.peekChar = read();
        this.ttype = currentChar3;
        return currentChar3;
    }

    public void ordinaryChar(int ch) {
        if (ch >= 0 && ch < this.tokenTypes.length) {
            this.tokenTypes[ch] = 0;
        }
    }

    public void ordinaryChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi > this.tokenTypes.length) {
            hi = this.tokenTypes.length - 1;
        }
        for (int i = low; i <= hi; i++) {
            this.tokenTypes[i] = 0;
        }
    }

    public void parseNumbers() {
        for (int i = 48; i <= 57; i++) {
            byte[] bArr = this.tokenTypes;
            bArr[i] = (byte) (bArr[i] | 16);
        }
        byte[] bArr2 = this.tokenTypes;
        bArr2[46] = (byte) (bArr2[46] | 16);
        byte[] bArr3 = this.tokenTypes;
        bArr3[45] = (byte) (bArr3[45] | 16);
    }

    public void pushBack() {
        this.pushBackToken = true;
    }

    public void quoteChar(int ch) {
        if (ch >= 0 && ch < this.tokenTypes.length) {
            this.tokenTypes[ch] = 2;
        }
    }

    private int read() throws IOException {
        return this.inStream == null ? this.inReader.read() : this.inStream.read();
    }

    public void resetSyntax() {
        for (int i = 0; i < 256; i++) {
            this.tokenTypes[i] = 0;
        }
    }

    public void slashSlashComments(boolean flag) {
        this.slashSlashComments = flag;
    }

    public void slashStarComments(boolean flag) {
        this.slashStarComments = flag;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Token[");
        switch (this.ttype) {
            case -3:
                result.append(this.sval);
                break;
            case -2:
                result.append("n=");
                result.append(this.nval);
                break;
            case -1:
                result.append("EOF");
                break;
            case 10:
                result.append("EOL");
                break;
            default:
                if (this.ttype == -4 || this.tokenTypes[this.ttype] == 2) {
                    result.append(this.sval);
                } else {
                    result.append('\'');
                    result.append((char) this.ttype);
                    result.append('\'');
                }
                break;
        }
        result.append("], line ");
        result.append(this.lineNumber);
        return result.toString();
    }

    public void whitespaceChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi > this.tokenTypes.length) {
            hi = this.tokenTypes.length - 1;
        }
        for (int i = low; i <= hi; i++) {
            this.tokenTypes[i] = 4;
        }
    }

    public void wordChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi > this.tokenTypes.length) {
            hi = this.tokenTypes.length - 1;
        }
        for (int i = low; i <= hi; i++) {
            byte[] bArr = this.tokenTypes;
            bArr[i] = (byte) (bArr[i] | 8);
        }
    }
}
