package mf.org.apache.xerces.xpointer;

import java.util.HashMap;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLErrorHandler;

final class ElementSchemePointer implements XPointerPart {
    private int[] fChildSequence;
    private int[] fCurrentChildSequence;
    protected XMLErrorHandler fErrorHandler;
    protected XMLErrorReporter fErrorReporter;
    private String fSchemeData;
    private String fSchemeName;
    private ShortHandPointer fShortHandPointer;
    private String fShortHandPointerName;
    private SymbolTable fSymbolTable;
    private boolean fIsResolveElement = false;
    private boolean fIsElementFound = false;
    private boolean fWasOnlyEmptyElementFound = false;
    boolean fIsShortHand = false;
    int fFoundDepth = 0;
    private int fCurrentChildPosition = 1;
    private int fCurrentChildDepth = 0;
    private boolean fIsFragmentResolved = false;

    public ElementSchemePointer() {
    }

    public ElementSchemePointer(SymbolTable symbolTable) {
        this.fSymbolTable = symbolTable;
    }

    public ElementSchemePointer(SymbolTable symbolTable, XMLErrorReporter errorReporter) {
        this.fSymbolTable = symbolTable;
        this.fErrorReporter = errorReporter;
    }

    @Override
    public void parseXPointer(String xpointer) throws XNIException {
        init();
        Tokens tokens = new Tokens(this, this.fSymbolTable, null);
        Scanner scanner = new Scanner(this.fSymbolTable) {
            {
                Scanner scanner2 = null;
            }

            @Override
            protected void addToken(Tokens tokens2, int token) throws XNIException {
                if (token == 1 || token == 0) {
                    super.addToken(tokens2, token);
                } else {
                    ElementSchemePointer.this.reportError("InvalidElementSchemeToken", new Object[]{tokens2.getTokenString(token)});
                }
            }
        };
        int length = xpointer.length();
        boolean success = scanner.scanExpr(this.fSymbolTable, tokens, xpointer, 0, length);
        if (!success) {
            reportError("InvalidElementSchemeXPointer", new Object[]{xpointer});
        }
        int[] tmpChildSequence = new int[(tokens.getTokenCount() / 2) + 1];
        int i = 0;
        while (tokens.hasMore()) {
            int token = tokens.nextToken();
            switch (token) {
                case 0:
                    int token2 = tokens.nextToken();
                    this.fShortHandPointerName = tokens.getTokenString(token2);
                    this.fShortHandPointer = new ShortHandPointer(this.fSymbolTable);
                    this.fShortHandPointer.setSchemeName(this.fShortHandPointerName);
                    break;
                case 1:
                    tmpChildSequence[i] = tokens.nextToken();
                    i++;
                    break;
                default:
                    reportError("InvalidElementSchemeXPointer", new Object[]{xpointer});
                    break;
            }
        }
        this.fChildSequence = new int[i];
        this.fCurrentChildSequence = new int[i];
        System.arraycopy(tmpChildSequence, 0, this.fChildSequence, 0, i);
    }

    @Override
    public String getSchemeName() {
        return this.fSchemeName;
    }

    @Override
    public String getSchemeData() {
        return this.fSchemeData;
    }

    @Override
    public void setSchemeName(String schemeName) {
        this.fSchemeName = schemeName;
    }

    @Override
    public void setSchemeData(String schemeData) {
        this.fSchemeData = schemeData;
    }

    @Override
    public boolean resolveXPointer(QName element, XMLAttributes attributes, Augmentations augs, int event) throws XNIException {
        boolean isShortHandPointerResolved = false;
        if (this.fShortHandPointerName != null) {
            isShortHandPointerResolved = this.fShortHandPointer.resolveXPointer(element, attributes, augs, event);
            if (isShortHandPointerResolved) {
                this.fIsResolveElement = true;
                this.fIsShortHand = true;
            } else {
                this.fIsResolveElement = false;
            }
        } else {
            this.fIsResolveElement = true;
        }
        if (this.fChildSequence.length > 0) {
            this.fIsFragmentResolved = matchChildSequence(element, event);
        } else if (isShortHandPointerResolved && this.fChildSequence.length <= 0) {
            this.fIsFragmentResolved = isShortHandPointerResolved;
        } else {
            this.fIsFragmentResolved = false;
        }
        return this.fIsFragmentResolved;
    }

    protected boolean matchChildSequence(QName element, int event) throws XNIException {
        if (this.fCurrentChildDepth >= this.fCurrentChildSequence.length) {
            int[] tmpCurrentChildSequence = new int[this.fCurrentChildSequence.length];
            System.arraycopy(this.fCurrentChildSequence, 0, tmpCurrentChildSequence, 0, this.fCurrentChildSequence.length);
            this.fCurrentChildSequence = new int[this.fCurrentChildDepth * 2];
            System.arraycopy(tmpCurrentChildSequence, 0, this.fCurrentChildSequence, 0, tmpCurrentChildSequence.length);
        }
        if (this.fIsResolveElement) {
            if (event != 0) {
                if (event == 1) {
                    if (this.fCurrentChildDepth == this.fFoundDepth) {
                        this.fIsElementFound = true;
                    } else if ((this.fCurrentChildDepth < this.fFoundDepth && this.fFoundDepth != 0) || (this.fCurrentChildDepth > this.fFoundDepth && this.fFoundDepth == 0)) {
                        this.fIsElementFound = false;
                    }
                    this.fCurrentChildSequence[this.fCurrentChildDepth] = 0;
                    this.fCurrentChildDepth--;
                    this.fCurrentChildPosition = this.fCurrentChildSequence[this.fCurrentChildDepth] + 1;
                } else if (event == 2) {
                    this.fCurrentChildSequence[this.fCurrentChildDepth] = this.fCurrentChildPosition;
                    this.fCurrentChildPosition++;
                    if (checkMatch()) {
                        if (!this.fIsElementFound) {
                            this.fWasOnlyEmptyElementFound = true;
                        } else {
                            this.fWasOnlyEmptyElementFound = false;
                        }
                        this.fIsElementFound = true;
                    } else {
                        this.fIsElementFound = false;
                        this.fWasOnlyEmptyElementFound = false;
                    }
                }
            } else {
                this.fCurrentChildSequence[this.fCurrentChildDepth] = this.fCurrentChildPosition;
                this.fCurrentChildDepth++;
                this.fCurrentChildPosition = 1;
                if (this.fCurrentChildDepth <= this.fFoundDepth || this.fFoundDepth == 0) {
                    if (checkMatch()) {
                        this.fIsElementFound = true;
                        this.fFoundDepth = this.fCurrentChildDepth;
                    } else {
                        this.fIsElementFound = false;
                        this.fFoundDepth = 0;
                    }
                }
            }
        }
        return this.fIsElementFound;
    }

    protected boolean checkMatch() {
        if (!this.fIsShortHand) {
            if (this.fChildSequence.length > this.fCurrentChildDepth + 1) {
                return false;
            }
            for (int i = 0; i < this.fChildSequence.length; i++) {
                if (this.fChildSequence[i] != this.fCurrentChildSequence[i]) {
                    return false;
                }
            }
        } else {
            if (this.fChildSequence.length > this.fCurrentChildDepth + 1) {
                return false;
            }
            for (int i2 = 0; i2 < this.fChildSequence.length; i2++) {
                if (this.fCurrentChildSequence.length < i2 + 2 || this.fChildSequence[i2] != this.fCurrentChildSequence[i2 + 1]) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isFragmentResolved() throws XNIException {
        return this.fIsFragmentResolved;
    }

    @Override
    public boolean isChildFragmentResolved() {
        if (this.fIsShortHand && this.fShortHandPointer != null && this.fChildSequence.length <= 0) {
            return this.fShortHandPointer.isChildFragmentResolved();
        }
        if (this.fWasOnlyEmptyElementFound) {
            if (!this.fWasOnlyEmptyElementFound) {
                return true;
            }
        } else if (this.fIsFragmentResolved && this.fCurrentChildDepth >= this.fFoundDepth) {
            return true;
        }
        return false;
    }

    protected void reportError(String key, Object[] arguments) throws XNIException {
        throw new XNIException(this.fErrorReporter.getMessageFormatter(XPointerMessageFormatter.XPOINTER_DOMAIN).formatMessage(this.fErrorReporter.getLocale(), key, arguments));
    }

    protected void initErrorReporter() {
        if (this.fErrorReporter == null) {
            this.fErrorReporter = new XMLErrorReporter();
        }
        if (this.fErrorHandler == null) {
            this.fErrorHandler = new XPointerErrorHandler();
        }
        this.fErrorReporter.putMessageFormatter(XPointerMessageFormatter.XPOINTER_DOMAIN, new XPointerMessageFormatter());
    }

    protected void init() {
        this.fSchemeName = null;
        this.fSchemeData = null;
        this.fShortHandPointerName = null;
        this.fIsResolveElement = false;
        this.fIsElementFound = false;
        this.fWasOnlyEmptyElementFound = false;
        this.fFoundDepth = 0;
        this.fCurrentChildPosition = 1;
        this.fCurrentChildDepth = 0;
        this.fIsFragmentResolved = false;
        this.fShortHandPointer = null;
        initErrorReporter();
    }

    private final class Tokens {
        private static final int INITIAL_TOKEN_COUNT = 256;
        private static final int XPTRTOKEN_ELEM_CHILD = 1;
        private static final int XPTRTOKEN_ELEM_NCNAME = 0;
        private int fCurrentTokenIndex;
        private SymbolTable fSymbolTable;
        private int fTokenCount;
        private HashMap fTokenNames;
        private int[] fTokens;
        private final String[] fgTokenNames;

        private Tokens(SymbolTable symbolTable) {
            this.fgTokenNames = new String[]{"XPTRTOKEN_ELEM_NCNAME", "XPTRTOKEN_ELEM_CHILD"};
            this.fTokens = new int[256];
            this.fTokenCount = 0;
            this.fTokenNames = new HashMap();
            this.fSymbolTable = symbolTable;
            this.fTokenNames.put(new Integer(0), "XPTRTOKEN_ELEM_NCNAME");
            this.fTokenNames.put(new Integer(1), "XPTRTOKEN_ELEM_CHILD");
        }

        Tokens(ElementSchemePointer elementSchemePointer, SymbolTable symbolTable, Tokens tokens) {
            this(symbolTable);
        }

        private String getTokenString(int token) {
            return (String) this.fTokenNames.get(new Integer(token));
        }

        private Integer getToken(int token) {
            return (Integer) this.fTokenNames.get(new Integer(token));
        }

        private void addToken(String tokenStr) {
            Integer tokenInt = (Integer) this.fTokenNames.get(tokenStr);
            if (tokenInt == null) {
                tokenInt = new Integer(this.fTokenNames.size());
                this.fTokenNames.put(tokenInt, tokenStr);
            }
            addToken(tokenInt.intValue());
        }

        private void addToken(int token) {
            try {
                this.fTokens[this.fTokenCount] = token;
            } catch (ArrayIndexOutOfBoundsException e) {
                int[] oldList = this.fTokens;
                this.fTokens = new int[this.fTokenCount << 1];
                System.arraycopy(oldList, 0, this.fTokens, 0, this.fTokenCount);
                this.fTokens[this.fTokenCount] = token;
            }
            this.fTokenCount++;
        }

        private void rewind() {
            this.fCurrentTokenIndex = 0;
        }

        private boolean hasMore() {
            return this.fCurrentTokenIndex < this.fTokenCount;
        }

        private int nextToken() throws XNIException {
            if (this.fCurrentTokenIndex == this.fTokenCount) {
                ElementSchemePointer.this.reportError("XPointerElementSchemeProcessingError", null);
            }
            int[] iArr = this.fTokens;
            int i = this.fCurrentTokenIndex;
            this.fCurrentTokenIndex = i + 1;
            return iArr[i];
        }

        private int peekToken() throws XNIException {
            if (this.fCurrentTokenIndex == this.fTokenCount) {
                ElementSchemePointer.this.reportError("XPointerElementSchemeProcessingError", null);
            }
            return this.fTokens[this.fCurrentTokenIndex];
        }

        private String nextTokenAsString() throws XNIException {
            String s = getTokenString(nextToken());
            if (s == null) {
                ElementSchemePointer.this.reportError("XPointerElementSchemeProcessingError", null);
            }
            return s;
        }

        private int getTokenCount() {
            return this.fTokenCount;
        }
    }

    private class Scanner {
        private static final byte CHARTYPE_DIGIT = 5;
        private static final byte CHARTYPE_INVALID = 0;
        private static final byte CHARTYPE_LETTER = 6;
        private static final byte CHARTYPE_MINUS = 2;
        private static final byte CHARTYPE_NONASCII = 8;
        private static final byte CHARTYPE_OTHER = 1;
        private static final byte CHARTYPE_PERIOD = 3;
        private static final byte CHARTYPE_SLASH = 4;
        private static final byte CHARTYPE_UNDERSCORE = 7;
        private final byte[] fASCIICharMap;
        private SymbolTable fSymbolTable;

        private Scanner(SymbolTable symbolTable) {
            this.fASCIICharMap = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, CHARTYPE_OTHER, CHARTYPE_OTHER, 0, 0, CHARTYPE_OTHER, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_MINUS, CHARTYPE_MINUS, CHARTYPE_SLASH, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_UNDERSCORE, CHARTYPE_OTHER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER};
            this.fSymbolTable = symbolTable;
        }

        Scanner(ElementSchemePointer elementSchemePointer, SymbolTable symbolTable, Scanner scanner) {
            this(symbolTable);
        }

        private boolean scanExpr(SymbolTable symbolTable, Tokens tokens, String data, int currentOffset, int endOffset) throws XNIException {
            while (currentOffset != endOffset) {
                int ch = data.charAt(currentOffset);
                byte chartype = ch >= 128 ? CHARTYPE_NONASCII : this.fASCIICharMap[ch];
                switch (chartype) {
                    case 1:
                    case 2:
                    case 3:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                        int child = currentOffset;
                        currentOffset = scanNCName(data, endOffset, currentOffset);
                        if (currentOffset == child) {
                            ElementSchemePointer.this.reportError("InvalidNCNameInElementSchemeData", new Object[]{data});
                            return false;
                        }
                        if (currentOffset < endOffset) {
                            data.charAt(currentOffset);
                        }
                        String nameHandle = symbolTable.addSymbol(data.substring(child, currentOffset));
                        addToken(tokens, 0);
                        tokens.addToken(nameHandle);
                        break;
                        break;
                    case 4:
                        currentOffset++;
                        if (currentOffset == endOffset) {
                            return false;
                        }
                        addToken(tokens, 1);
                        int ch2 = data.charAt(currentOffset);
                        int child2 = 0;
                        while (ch2 >= 48 && ch2 <= 57) {
                            child2 = (child2 * 10) + (ch2 - 48);
                            currentOffset++;
                            if (currentOffset != endOffset) {
                                ch2 = data.charAt(currentOffset);
                            } else if (child2 == 0) {
                                tokens.addToken(child2);
                            } else {
                                ElementSchemePointer.this.reportError("InvalidChildSequenceCharacter", new Object[]{new Character((char) ch2)});
                                return false;
                            }
                            break;
                        }
                        if (child2 == 0) {
                        }
                        break;
                }
            }
            return true;
        }

        private int scanNCName(String data, int endOffset, int currentOffset) {
            int ch = data.charAt(currentOffset);
            if (ch >= 128) {
                if (!XMLChar.isNameStart(ch)) {
                    return currentOffset;
                }
            } else {
                byte chartype = this.fASCIICharMap[ch];
                if (chartype != 6 && chartype != 7) {
                    return currentOffset;
                }
            }
            while (true) {
                currentOffset++;
                if (currentOffset >= endOffset) {
                    break;
                }
                int ch2 = data.charAt(currentOffset);
                if (ch2 >= 128) {
                    if (!XMLChar.isName(ch2)) {
                        break;
                    }
                } else {
                    byte chartype2 = this.fASCIICharMap[ch2];
                    if (chartype2 != 6 && chartype2 != 5 && chartype2 != 3 && chartype2 != 2 && chartype2 != 7) {
                        break;
                    }
                }
            }
            return currentOffset;
        }

        protected void addToken(Tokens tokens, int token) throws XNIException {
            tokens.addToken(token);
        }
    }
}
