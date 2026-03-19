package mf.org.apache.xerces.xpointer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xinclude.XIncludeHandler;
import mf.org.apache.xerces.xinclude.XIncludeNamespaceSupport;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLDocumentHandler;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLErrorHandler;

public final class XPointerHandler extends XIncludeHandler implements XPointerProcessor {
    protected XMLErrorHandler fErrorHandler;
    protected SymbolTable fSymbolTable;
    protected XMLErrorReporter fXPointerErrorReporter;
    protected ArrayList fXPointerParts;
    protected XPointerPart fXPointerPart = null;
    protected boolean fFoundMatchingPtrPart = false;
    private final String ELEMENT_SCHEME_NAME = "element";
    protected boolean fIsXPointerResolved = false;
    protected boolean fFixupBase = false;
    protected boolean fFixupLang = false;

    public XPointerHandler() {
        this.fXPointerParts = null;
        this.fSymbolTable = null;
        this.fXPointerParts = new ArrayList();
        this.fSymbolTable = new SymbolTable();
    }

    public XPointerHandler(SymbolTable symbolTable, XMLErrorHandler errorHandler, XMLErrorReporter errorReporter) {
        this.fXPointerParts = null;
        this.fSymbolTable = null;
        this.fXPointerParts = new ArrayList();
        this.fSymbolTable = symbolTable;
        this.fErrorHandler = errorHandler;
        this.fXPointerErrorReporter = errorReporter;
    }

    @Override
    public void setDocumentHandler(XMLDocumentHandler handler) {
        this.fDocumentHandler = handler;
    }

    @Override
    public void parseXPointer(String xpointer) throws XNIException {
        Scanner scanner;
        boolean success;
        String closeParen;
        int i;
        int closeParenCount;
        init();
        Tokens tokens = new Tokens(this, this.fSymbolTable, null);
        Scanner scanner2 = new Scanner(this.fSymbolTable) {
            {
                Scanner scanner3 = null;
            }

            @Override
            protected void addToken(Tokens tokens2, int token) throws XNIException {
                if (token == 0 || token == 1 || token == 3 || token == 4 || token == 2) {
                    super.addToken(tokens2, token);
                } else {
                    XPointerHandler.this.reportError("InvalidXPointerToken", new Object[]{tokens2.getTokenString(token)});
                }
            }
        };
        int length = xpointer.length();
        boolean success2 = scanner2.scanExpr(this.fSymbolTable, tokens, xpointer, 0, length);
        int i2 = 1;
        if (!success2) {
            reportError("InvalidXPointerExpression", new Object[]{xpointer});
        }
        while (tokens.hasMore()) {
            int token = tokens.nextToken();
            switch (token) {
                case 2:
                    scanner = scanner2;
                    success = success2;
                    int token2 = tokens.nextToken();
                    String shortHandPointerName = tokens.getTokenString(token2);
                    if (shortHandPointerName == null) {
                        reportError("InvalidXPointerExpression", new Object[]{xpointer});
                    }
                    ShortHandPointer shortHandPointer = new ShortHandPointer(this.fSymbolTable);
                    shortHandPointer.setSchemeName(shortHandPointerName);
                    this.fXPointerParts.add(shortHandPointer);
                    break;
                case 3:
                    int token3 = tokens.nextToken();
                    String prefix = tokens.getTokenString(token3);
                    int token4 = tokens.nextToken();
                    String localName = tokens.getTokenString(token4);
                    String schemeName = String.valueOf(prefix) + localName;
                    int token5 = tokens.nextToken();
                    String openParen = tokens.getTokenString(token5);
                    if (openParen != "XPTRTOKEN_OPEN_PAREN") {
                        if (token5 == 2) {
                            Object[] objArr = new Object[i2];
                            objArr[0] = xpointer;
                            reportError("MultipleShortHandPointers", objArr);
                        } else {
                            Object[] objArr2 = new Object[i2];
                            objArr2[0] = xpointer;
                            reportError("InvalidXPointerExpression", objArr2);
                        }
                    }
                    int openParenCount = 0 + i2;
                    while (tokens.hasMore()) {
                        int token6 = tokens.nextToken();
                        if (tokens.getTokenString(token6) != "XPTRTOKEN_OPEN_PAREN") {
                            int token7 = tokens.nextToken();
                            String schemeData = tokens.getTokenString(token7);
                            int token8 = tokens.nextToken();
                            closeParen = tokens.getTokenString(token8);
                            if (closeParen == "XPTRTOKEN_CLOSE_PAREN") {
                                scanner = scanner2;
                                success = success2;
                                i = 1;
                                reportError("SchemeDataNotFollowedByCloseParenthesis", new Object[]{xpointer});
                            } else {
                                scanner = scanner2;
                                success = success2;
                                i = 1;
                            }
                            closeParenCount = 0 + i;
                            while (tokens.hasMore() && tokens.getTokenString(tokens.peekToken()) == "XPTRTOKEN_OPEN_PAREN") {
                                closeParenCount++;
                            }
                            if (openParenCount != closeParenCount) {
                                reportError("UnbalancedParenthesisInXPointerExpression", new Object[]{xpointer, new Integer(openParenCount), new Integer(closeParenCount)});
                            }
                            if (!schemeName.equals("element")) {
                                ElementSchemePointer elementSchemePointer = new ElementSchemePointer(this.fSymbolTable, this.fErrorReporter);
                                elementSchemePointer.setSchemeName(schemeName);
                                elementSchemePointer.setSchemeData(schemeData);
                                try {
                                    elementSchemePointer.parseXPointer(schemeData);
                                    this.fXPointerParts.add(elementSchemePointer);
                                } catch (XNIException e) {
                                    throw new XNIException(e);
                                }
                            } else {
                                reportWarning("SchemeUnsupported", new Object[]{schemeName});
                            }
                        } else {
                            openParenCount++;
                        }
                        break;
                    }
                    int token72 = tokens.nextToken();
                    String schemeData2 = tokens.getTokenString(token72);
                    int token82 = tokens.nextToken();
                    closeParen = tokens.getTokenString(token82);
                    if (closeParen == "XPTRTOKEN_CLOSE_PAREN") {
                    }
                    closeParenCount = 0 + i;
                    while (tokens.hasMore()) {
                        closeParenCount++;
                    }
                    if (openParenCount != closeParenCount) {
                    }
                    if (!schemeName.equals("element")) {
                    }
                    break;
                default:
                    reportError("InvalidXPointerExpression", new Object[]{xpointer});
                    success2 = success2;
                    i2 = 1;
                    scanner2 = scanner2;
                    break;
            }
            scanner2 = scanner;
            success2 = success;
            i2 = 1;
        }
    }

    @Override
    public boolean resolveXPointer(QName element, XMLAttributes attributes, Augmentations augs, int event) throws XNIException {
        boolean resolved = false;
        if (!this.fFoundMatchingPtrPart) {
            for (int i = 0; i < this.fXPointerParts.size(); i++) {
                this.fXPointerPart = (XPointerPart) this.fXPointerParts.get(i);
                if (this.fXPointerPart.resolveXPointer(element, attributes, augs, event)) {
                    this.fFoundMatchingPtrPart = true;
                    resolved = true;
                }
            }
        } else if (this.fXPointerPart.resolveXPointer(element, attributes, augs, event)) {
            resolved = true;
        }
        if (!this.fIsXPointerResolved) {
            this.fIsXPointerResolved = resolved;
        }
        return resolved;
    }

    @Override
    public boolean isFragmentResolved() throws XNIException {
        boolean resolved = this.fXPointerPart != null ? this.fXPointerPart.isFragmentResolved() : false;
        if (!this.fIsXPointerResolved) {
            this.fIsXPointerResolved = resolved;
        }
        return resolved;
    }

    public boolean isChildFragmentResolved() throws XNIException {
        if (this.fXPointerPart == null) {
            return false;
        }
        boolean resolved = this.fXPointerPart.isChildFragmentResolved();
        return resolved;
    }

    @Override
    public boolean isXPointerResolved() throws XNIException {
        return this.fIsXPointerResolved;
    }

    public XPointerPart getXPointerPart() {
        return this.fXPointerPart;
    }

    private void reportError(String key, Object[] arguments) throws XNIException {
        throw new XNIException(this.fErrorReporter.getMessageFormatter(XPointerMessageFormatter.XPOINTER_DOMAIN).formatMessage(this.fErrorReporter.getLocale(), key, arguments));
    }

    private void reportWarning(String key, Object[] arguments) throws XNIException {
        this.fXPointerErrorReporter.reportError(XPointerMessageFormatter.XPOINTER_DOMAIN, key, arguments, (short) 0);
    }

    protected void initErrorReporter() {
        if (this.fXPointerErrorReporter == null) {
            this.fXPointerErrorReporter = new XMLErrorReporter();
        }
        if (this.fErrorHandler == null) {
            this.fErrorHandler = new XPointerErrorHandler();
        }
        this.fXPointerErrorReporter.putMessageFormatter(XPointerMessageFormatter.XPOINTER_DOMAIN, new XPointerMessageFormatter());
    }

    protected void init() {
        this.fXPointerParts.clear();
        this.fXPointerPart = null;
        this.fFoundMatchingPtrPart = false;
        this.fIsXPointerResolved = false;
        initErrorReporter();
    }

    public ArrayList getPointerParts() {
        return this.fXPointerParts;
    }

    private final class Tokens {
        private static final int INITIAL_TOKEN_COUNT = 256;
        private static final int XPTRTOKEN_CLOSE_PAREN = 1;
        private static final int XPTRTOKEN_OPEN_PAREN = 0;
        private static final int XPTRTOKEN_SCHEMEDATA = 4;
        private static final int XPTRTOKEN_SCHEMENAME = 3;
        private static final int XPTRTOKEN_SHORTHAND = 2;
        private int fCurrentTokenIndex;
        private SymbolTable fSymbolTable;
        private int fTokenCount;
        private HashMap fTokenNames;
        private int[] fTokens;
        private final String[] fgTokenNames;

        private Tokens(SymbolTable symbolTable) {
            this.fgTokenNames = new String[]{"XPTRTOKEN_OPEN_PAREN", "XPTRTOKEN_CLOSE_PAREN", "XPTRTOKEN_SHORTHAND", "XPTRTOKEN_SCHEMENAME", "XPTRTOKEN_SCHEMEDATA"};
            this.fTokens = new int[256];
            this.fTokenCount = 0;
            this.fTokenNames = new HashMap();
            this.fSymbolTable = symbolTable;
            this.fTokenNames.put(new Integer(0), "XPTRTOKEN_OPEN_PAREN");
            this.fTokenNames.put(new Integer(1), "XPTRTOKEN_CLOSE_PAREN");
            this.fTokenNames.put(new Integer(2), "XPTRTOKEN_SHORTHAND");
            this.fTokenNames.put(new Integer(3), "XPTRTOKEN_SCHEMENAME");
            this.fTokenNames.put(new Integer(4), "XPTRTOKEN_SCHEMEDATA");
        }

        Tokens(XPointerHandler xPointerHandler, SymbolTable symbolTable, Tokens tokens) {
            this(symbolTable);
        }

        private String getTokenString(int token) {
            return (String) this.fTokenNames.get(new Integer(token));
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
                XPointerHandler.this.reportError("XPointerProcessingError", null);
            }
            int[] iArr = this.fTokens;
            int i = this.fCurrentTokenIndex;
            this.fCurrentTokenIndex = i + 1;
            return iArr[i];
        }

        private int peekToken() throws XNIException {
            if (this.fCurrentTokenIndex == this.fTokenCount) {
                XPointerHandler.this.reportError("XPointerProcessingError", null);
            }
            return this.fTokens[this.fCurrentTokenIndex];
        }

        private String nextTokenAsString() throws XNIException {
            String tokenStrint = getTokenString(nextToken());
            if (tokenStrint == null) {
                XPointerHandler.this.reportError("XPointerProcessingError", null);
            }
            return tokenStrint;
        }
    }

    private class Scanner {
        private static final byte CHARTYPE_CARRET = 3;
        private static final byte CHARTYPE_CLOSE_PAREN = 5;
        private static final byte CHARTYPE_COLON = 10;
        private static final byte CHARTYPE_DIGIT = 9;
        private static final byte CHARTYPE_EQUAL = 11;
        private static final byte CHARTYPE_INVALID = 0;
        private static final byte CHARTYPE_LETTER = 12;
        private static final byte CHARTYPE_MINUS = 6;
        private static final byte CHARTYPE_NONASCII = 14;
        private static final byte CHARTYPE_OPEN_PAREN = 4;
        private static final byte CHARTYPE_OTHER = 1;
        private static final byte CHARTYPE_PERIOD = 7;
        private static final byte CHARTYPE_SLASH = 8;
        private static final byte CHARTYPE_UNDERSCORE = 13;
        private static final byte CHARTYPE_WHITESPACE = 2;
        private final byte[] fASCIICharMap;
        private SymbolTable fSymbolTable;

        private Scanner(SymbolTable symbolTable) {
            this.fASCIICharMap = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, CHARTYPE_WHITESPACE, CHARTYPE_WHITESPACE, 0, 0, CHARTYPE_WHITESPACE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, CHARTYPE_WHITESPACE, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OPEN_PAREN, CHARTYPE_CLOSE_PAREN, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_MINUS, CHARTYPE_PERIOD, CHARTYPE_SLASH, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_DIGIT, CHARTYPE_COLON, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_EQUAL, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_CARRET, CHARTYPE_UNDERSCORE, CHARTYPE_OTHER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_LETTER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER, CHARTYPE_OTHER};
            this.fSymbolTable = symbolTable;
        }

        Scanner(XPointerHandler xPointerHandler, SymbolTable symbolTable, Scanner scanner) {
            this(symbolTable);
        }

        private boolean scanExpr(SymbolTable symbolTable, Tokens tokens, String data, int currentOffset, int endOffset) throws XNIException {
            int ch;
            boolean isQName;
            String name = null;
            String prefix = null;
            String schemeData = null;
            StringBuffer schemeDataBuff = new StringBuffer();
            boolean isQName2 = false;
            int closeParen = 0;
            int closeParen2 = 0;
            int openParen = currentOffset;
            while (openParen != endOffset) {
                int ch2 = data.charAt(openParen);
                while (true) {
                    if ((ch2 == 32 || ch2 == 10 || ch2 == 9 || ch2 == 13) && (openParen = openParen + 1) != endOffset) {
                        ch2 = data.charAt(openParen);
                    }
                }
                if (openParen == endOffset) {
                    return true;
                }
                byte chartype = ch2 >= 128 ? CHARTYPE_NONASCII : this.fASCIICharMap[ch2];
                switch (chartype) {
                    case 1:
                    case 2:
                    case 3:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                        if (closeParen2 == 0) {
                            int nameOffset = openParen;
                            openParen = scanNCName(data, endOffset, openParen);
                            if (openParen == nameOffset) {
                                XPointerHandler.this.reportError("InvalidShortHandPointer", new Object[]{data});
                                return false;
                            }
                            String schemeData2 = schemeData;
                            boolean isQName3 = isQName2;
                            if (openParen < endOffset) {
                                ch = data.charAt(openParen);
                            } else {
                                ch = -1;
                            }
                            name = symbolTable.addSymbol(data.substring(nameOffset, openParen));
                            String prefix2 = XMLSymbols.EMPTY_STRING;
                            if (ch == 58) {
                                int currentOffset2 = openParen + 1;
                                if (currentOffset2 == endOffset) {
                                    return false;
                                }
                                data.charAt(currentOffset2);
                                prefix2 = name;
                                openParen = scanNCName(data, endOffset, currentOffset2);
                                if (openParen == currentOffset2) {
                                    return false;
                                }
                                if (openParen < endOffset) {
                                    ch = data.charAt(openParen);
                                } else {
                                    ch = -1;
                                }
                                isQName = true;
                                name = symbolTable.addSymbol(data.substring(currentOffset2, openParen));
                            } else {
                                isQName = isQName3;
                            }
                            prefix = prefix2;
                            if (openParen == endOffset) {
                                if (openParen == endOffset) {
                                    addToken(tokens, 2);
                                    tokens.addToken(name);
                                    isQName = false;
                                }
                            } else {
                                addToken(tokens, 3);
                                tokens.addToken(prefix);
                                tokens.addToken(name);
                                isQName = false;
                            }
                            closeParen = 0;
                            isQName2 = isQName;
                            schemeData = schemeData2;
                        } else {
                            String prefix3 = prefix;
                            boolean isQName4 = isQName2;
                            if (closeParen2 > 0 && closeParen == 0 && name != null) {
                                int dataOffset = openParen;
                                openParen = scanData(data, schemeDataBuff, endOffset, openParen);
                                if (openParen == dataOffset) {
                                    XPointerHandler.this.reportError("InvalidSchemeDataInXPointer", new Object[]{data});
                                    return false;
                                }
                                if (openParen < endOffset) {
                                    data.charAt(openParen);
                                }
                                String schemeData3 = symbolTable.addSymbol(schemeDataBuff.toString());
                                addToken(tokens, 4);
                                tokens.addToken(schemeData3);
                                closeParen2 = 0;
                                schemeDataBuff.delete(0, schemeDataBuff.length());
                                schemeData = schemeData3;
                                prefix = prefix3;
                                isQName2 = isQName4;
                            } else {
                                return false;
                            }
                        }
                        break;
                    case 4:
                        addToken(tokens, 0);
                        closeParen2++;
                        openParen++;
                        break;
                    case 5:
                        addToken(tokens, 1);
                        closeParen++;
                        openParen++;
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
                if (chartype != 12 && chartype != 13) {
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
                    if (chartype2 != 12 && chartype2 != 9 && chartype2 != 7 && chartype2 != 6 && chartype2 != 13) {
                        break;
                    }
                }
            }
            return currentOffset;
        }

        private int scanData(String data, StringBuffer schemeData, int endOffset, int currentOffset) {
            byte chartype;
            while (currentOffset != endOffset) {
                int ch = data.charAt(currentOffset);
                byte chartype2 = CHARTYPE_NONASCII;
                if (ch >= 128) {
                    chartype = 14;
                } else {
                    chartype = this.fASCIICharMap[ch];
                }
                if (chartype == 4) {
                    schemeData.append(ch);
                    int currentOffset2 = scanData(data, schemeData, endOffset, currentOffset + 1);
                    if (currentOffset2 == endOffset) {
                        return currentOffset2;
                    }
                    int ch2 = data.charAt(currentOffset2);
                    if (ch2 < 128) {
                        chartype2 = this.fASCIICharMap[ch2];
                    }
                    if (chartype2 != 5) {
                        return endOffset;
                    }
                    schemeData.append((char) ch2);
                    currentOffset = currentOffset2 + 1;
                } else {
                    if (chartype == 5) {
                        return currentOffset;
                    }
                    if (chartype == 3) {
                        currentOffset++;
                        int ch3 = data.charAt(currentOffset);
                        if (ch3 < 128) {
                            chartype2 = this.fASCIICharMap[ch3];
                        }
                        if (chartype2 != 3 && chartype2 != 4 && chartype2 != 5) {
                            break;
                        }
                        schemeData.append((char) ch3);
                        currentOffset++;
                    } else {
                        schemeData.append((char) ch);
                        currentOffset++;
                    }
                }
            }
            return currentOffset;
        }

        protected void addToken(Tokens tokens, int token) throws XNIException {
            tokens.addToken(token);
        }
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.comment(text, augs);
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.processingInstruction(target, data, augs);
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws IOException, XNIException {
        if (!resolveXPointer(element, attributes, augs, 0)) {
            if (this.fFixupBase) {
                processXMLBaseAttributes(attributes);
            }
            if (this.fFixupLang) {
                processXMLLangAttributes(attributes);
            }
            this.fNamespaceContext.setContextInvalid();
            return;
        }
        super.startElement(element, attributes, augs);
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws IOException, XNIException {
        if (!resolveXPointer(element, attributes, augs, 2)) {
            if (this.fFixupBase) {
                processXMLBaseAttributes(attributes);
            }
            if (this.fFixupLang) {
                processXMLLangAttributes(attributes);
            }
            this.fNamespaceContext.setContextInvalid();
            return;
        }
        super.emptyElement(element, attributes, augs);
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.characters(text, augs);
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.ignorableWhitespace(text, augs);
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        if (!resolveXPointer(element, null, augs, 1)) {
            return;
        }
        super.endElement(element, augs);
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.startCDATA(augs);
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
        if (!isChildFragmentResolved()) {
            return;
        }
        super.endCDATA(augs);
    }

    @Override
    public void setProperty(String propertyId, Object value) throws XMLConfigurationException {
        if (propertyId == "http://apache.org/xml/properties/internal/error-reporter") {
            if (value != null) {
                this.fXPointerErrorReporter = (XMLErrorReporter) value;
            } else {
                this.fXPointerErrorReporter = null;
            }
        }
        if (propertyId == "http://apache.org/xml/properties/internal/error-handler") {
            if (value != null) {
                this.fErrorHandler = (XMLErrorHandler) value;
            } else {
                this.fErrorHandler = null;
            }
        }
        if (propertyId == "http://apache.org/xml/features/xinclude/fixup-language") {
            if (value != null) {
                this.fFixupLang = ((Boolean) value).booleanValue();
            } else {
                this.fFixupLang = false;
            }
        }
        if (propertyId == "http://apache.org/xml/features/xinclude/fixup-base-uris") {
            if (value != null) {
                this.fFixupBase = ((Boolean) value).booleanValue();
            } else {
                this.fFixupBase = false;
            }
        }
        if (propertyId == "http://apache.org/xml/properties/internal/namespace-context") {
            this.fNamespaceContext = (XIncludeNamespaceSupport) value;
        }
        super.setProperty(propertyId, value);
    }
}
