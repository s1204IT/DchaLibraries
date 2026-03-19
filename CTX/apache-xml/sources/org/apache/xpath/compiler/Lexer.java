package org.apache.xpath.compiler;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.ObjectVector;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.res.XPATHErrorResources;

class Lexer {
    static final int TARGETEXTRA = 10000;
    private Compiler m_compiler;
    PrefixResolver m_namespaceContext;
    private int[] m_patternMap = new int[100];
    private int m_patternMapSize;
    XPathParser m_processor;

    Lexer(Compiler compiler, PrefixResolver prefixResolver, XPathParser xPathParser) {
        this.m_compiler = compiler;
        this.m_namespaceContext = prefixResolver;
        this.m_processor = xPathParser;
    }

    void tokenize(String str) throws TransformerException {
        tokenize(str, null);
    }

    void tokenize(String str, Vector vector) throws TransformerException {
        boolean zMapPatternElemPos;
        boolean z;
        this.m_compiler.m_currentPattern = str;
        boolean z2 = false;
        this.m_patternMapSize = 0;
        this.m_compiler.m_opMap = new OpMapVector((str.length() < 500 ? str.length() : 500) * 5, 2500, 1);
        int length = str.length();
        int i = 0;
        int i2 = 0;
        boolean z3 = false;
        boolean zIsDigit = false;
        int i3 = -1;
        int iMapNSTokens = -1;
        boolean zMapPatternElemPos2 = true;
        while (i < length) {
            char cCharAt = str.charAt(i);
            switch (cCharAt) {
                default:
                    switch (cCharAt) {
                        case ' ':
                            break;
                        case '!':
                            boolean z4 = z3;
                            if (i3 != -1) {
                                zMapPatternElemPos = mapPatternElemPos(i2, zMapPatternElemPos2, z4);
                                if (-1 != iMapNSTokens) {
                                    iMapNSTokens = mapNSTokens(str, i3, iMapNSTokens, i);
                                } else {
                                    addToTokenQueue(str.substring(i3, i));
                                }
                                i3 = -1;
                                z = false;
                                zIsDigit = false;
                            } else {
                                if ('/' == cCharAt && zMapPatternElemPos2) {
                                    zMapPatternElemPos2 = mapPatternElemPos(i2, zMapPatternElemPos2, z4);
                                } else if ('*' == cCharAt) {
                                    zMapPatternElemPos = mapPatternElemPos(i2, zMapPatternElemPos2, z4);
                                    z = false;
                                }
                                boolean z5 = zMapPatternElemPos2;
                                z = z4;
                                zMapPatternElemPos = z5;
                            }
                            if (i2 == 0 && '|' == cCharAt) {
                                if (vector != null) {
                                    recordTokenString(vector);
                                }
                                zMapPatternElemPos = true;
                            }
                            if (')' == cCharAt || ']' == cCharAt) {
                                i2--;
                            } else if ('(' == cCharAt || '[' == cCharAt) {
                                i2++;
                            }
                            addToTokenQueue(str.substring(i, i + 1));
                            z3 = z;
                            zMapPatternElemPos2 = zMapPatternElemPos;
                            break;
                        case '\"':
                            if (i3 != -1) {
                                zMapPatternElemPos2 = mapPatternElemPos(i2, zMapPatternElemPos2, z3);
                                if (-1 != iMapNSTokens) {
                                    iMapNSTokens = mapNSTokens(str, i3, iMapNSTokens, i);
                                } else {
                                    addToTokenQueue(str.substring(i3, i));
                                }
                                z3 = false;
                                zIsDigit = false;
                            }
                            int i4 = i + 1;
                            while (i4 < length && (cCharAt = str.charAt(i4)) != '\"') {
                                i4++;
                            }
                            if (cCharAt == '\"' && i4 < length) {
                                addToTokenQueue(str.substring(i, i4 + 1));
                                i = i4;
                                i3 = -1;
                            } else {
                                this.m_processor.error(XPATHErrorResources.ER_EXPECTED_DOUBLE_QUOTE, null);
                                i3 = i;
                                i = i4;
                            }
                            break;
                        default:
                            switch (cCharAt) {
                                case '\'':
                                    if (i3 != -1) {
                                        zMapPatternElemPos2 = mapPatternElemPos(i2, zMapPatternElemPos2, z3);
                                        if (-1 != iMapNSTokens) {
                                            iMapNSTokens = mapNSTokens(str, i3, iMapNSTokens, i);
                                        } else {
                                            addToTokenQueue(str.substring(i3, i));
                                        }
                                        z3 = z2;
                                        zIsDigit = z3;
                                    }
                                    int i5 = i + 1;
                                    while (i5 < length && (cCharAt = str.charAt(i5)) != '\'') {
                                        i5++;
                                    }
                                    if (cCharAt == '\'' && i5 < length) {
                                        addToTokenQueue(str.substring(i, i5 + 1));
                                        i = i5;
                                        i3 = -1;
                                    } else {
                                        this.m_processor.error(XPATHErrorResources.ER_EXPECTED_SINGLE_QUOTE, null);
                                        int i6 = i5;
                                        i3 = i;
                                        i = i6;
                                    }
                                    break;
                                case '-':
                                    if ('-' == cCharAt) {
                                        if (zIsDigit || i3 == -1) {
                                            zIsDigit = z2;
                                        }
                                        break;
                                    }
                                case '(':
                                case ')':
                                case '*':
                                case '+':
                                case ',':
                                    break;
                                default:
                                    switch (cCharAt) {
                                        default:
                                            switch (cCharAt) {
                                                default:
                                                    switch (cCharAt) {
                                                        case '\r':
                                                            break;
                                                        case '$':
                                                        case '/':
                                                        case '|':
                                                            break;
                                                        case ':':
                                                            if (i <= 0) {
                                                                if (-1 != i3) {
                                                                    zIsDigit = Character.isDigit(cCharAt);
                                                                    i3 = i;
                                                                } else if (zIsDigit) {
                                                                    zIsDigit = Character.isDigit(cCharAt);
                                                                }
                                                                break;
                                                            } else {
                                                                int i7 = i - 1;
                                                                if (iMapNSTokens == i7) {
                                                                    if (i3 != -1 && i3 < i7) {
                                                                        addToTokenQueue(str.substring(i3, i7));
                                                                    }
                                                                    addToTokenQueue(str.substring(i7, i + 1));
                                                                    z3 = z2;
                                                                    zIsDigit = z3;
                                                                    i3 = -1;
                                                                    iMapNSTokens = -1;
                                                                    break;
                                                                } else {
                                                                    iMapNSTokens = i;
                                                                    if (-1 != i3) {
                                                                    }
                                                                }
                                                            }
                                                            break;
                                                        case '@':
                                                            z3 = true;
                                                            if ('-' == cCharAt) {
                                                            }
                                                            break;
                                                    }
                                                case '[':
                                                case '\\':
                                                case ']':
                                                case '^':
                                                    break;
                                            }
                                        case '<':
                                        case '=':
                                        case '>':
                                            break;
                                    }
                                    break;
                            }
                            break;
                    }
                case '\t':
                case '\n':
                    if (i3 != -1) {
                        boolean zMapPatternElemPos3 = mapPatternElemPos(i2, zMapPatternElemPos2, z3);
                        if (-1 != iMapNSTokens) {
                            iMapNSTokens = mapNSTokens(str, i3, iMapNSTokens, i);
                        } else {
                            addToTokenQueue(str.substring(i3, i));
                        }
                        zMapPatternElemPos2 = zMapPatternElemPos3;
                        i3 = -1;
                        z3 = false;
                        zIsDigit = false;
                    }
                    break;
            }
            i++;
            z2 = false;
        }
        if (i3 != -1) {
            mapPatternElemPos(i2, zMapPatternElemPos2, z3);
            if (-1 != iMapNSTokens || (this.m_namespaceContext != null && this.m_namespaceContext.handlesNullPrefixes())) {
                mapNSTokens(str, i3, iMapNSTokens, length);
            } else {
                addToTokenQueue(str.substring(i3, length));
            }
        }
        if (this.m_compiler.getTokenQueueSize() == 0) {
            this.m_processor.error(XPATHErrorResources.ER_EMPTY_EXPRESSION, null);
        } else if (vector != null) {
            recordTokenString(vector);
        }
        this.m_processor.m_queueMark = 0;
    }

    private boolean mapPatternElemPos(int i, boolean z, boolean z2) {
        if (i != 0) {
            return z;
        }
        if (this.m_patternMapSize >= this.m_patternMap.length) {
            int[] iArr = this.m_patternMap;
            int length = this.m_patternMap.length;
            this.m_patternMap = new int[this.m_patternMapSize + 100];
            System.arraycopy(iArr, 0, this.m_patternMap, 0, length);
        }
        if (!z) {
            this.m_patternMap[this.m_patternMapSize - 1] = r4[r5] - 10000;
        }
        this.m_patternMap[this.m_patternMapSize] = (this.m_compiler.getTokenQueueSize() - (z2 ? 1 : 0)) + TARGETEXTRA;
        this.m_patternMapSize++;
        return false;
    }

    private int getTokenQueuePosFromMap(int i) {
        int i2 = this.m_patternMap[i];
        return i2 >= TARGETEXTRA ? i2 - 10000 : i2;
    }

    private final void resetTokenMark(int i) {
        int tokenQueueSize = this.m_compiler.getTokenQueueSize();
        XPathParser xPathParser = this.m_processor;
        if (i <= 0) {
            i = 0;
        } else if (i <= tokenQueueSize) {
            i--;
        }
        xPathParser.m_queueMark = i;
        if (this.m_processor.m_queueMark < tokenQueueSize) {
            XPathParser xPathParser2 = this.m_processor;
            ObjectVector tokenQueue = this.m_compiler.getTokenQueue();
            XPathParser xPathParser3 = this.m_processor;
            int i2 = xPathParser3.m_queueMark;
            xPathParser3.m_queueMark = i2 + 1;
            xPathParser2.m_token = (String) tokenQueue.elementAt(i2);
            this.m_processor.m_tokenChar = this.m_processor.m_token.charAt(0);
            return;
        }
        this.m_processor.m_token = null;
        this.m_processor.m_tokenChar = (char) 0;
    }

    final int getKeywordToken(String str) {
        try {
            Integer num = (Integer) Keywords.getKeyWord(str);
            if (num != null) {
                return num.intValue();
            }
            return 0;
        } catch (ClassCastException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    private void recordTokenString(Vector vector) {
        int tokenQueuePosFromMap = getTokenQueuePosFromMap(this.m_patternMapSize - 1);
        int i = tokenQueuePosFromMap + 1;
        resetTokenMark(i);
        if (this.m_processor.lookahead('(', 1)) {
            int keywordToken = getKeywordToken(this.m_processor.m_token);
            switch (keywordToken) {
                case 35:
                    vector.addElement(PsuedoNames.PSEUDONAME_ROOT);
                    break;
                case 36:
                    vector.addElement("*");
                    break;
                default:
                    switch (keywordToken) {
                        case OpCodes.NODETYPE_COMMENT:
                            vector.addElement(PsuedoNames.PSEUDONAME_COMMENT);
                            break;
                        case OpCodes.NODETYPE_TEXT:
                            vector.addElement(PsuedoNames.PSEUDONAME_TEXT);
                            break;
                        case OpCodes.NODETYPE_PI:
                            vector.addElement("*");
                            break;
                        case OpCodes.NODETYPE_NODE:
                            vector.addElement("*");
                            break;
                        default:
                            vector.addElement("*");
                            break;
                    }
                    break;
            }
        }
        if (this.m_processor.tokenIs('@')) {
            resetTokenMark(i + 1);
            tokenQueuePosFromMap = i;
        }
        if (this.m_processor.lookahead(':', 1)) {
            tokenQueuePosFromMap += 2;
        }
        vector.addElement(this.m_compiler.getTokenQueue().elementAt(tokenQueuePosFromMap));
    }

    private final void addToTokenQueue(String str) {
        this.m_compiler.getTokenQueue().addElement(str);
    }

    private int mapNSTokens(String str, int i, int i2, int i3) throws TransformerException {
        String namespaceForPrefix;
        String strSubstring = "";
        if (i >= 0 && i2 >= 0) {
            strSubstring = str.substring(i, i2);
        }
        if (this.m_namespaceContext != null && !strSubstring.equals("*") && !strSubstring.equals("xmlns")) {
            try {
                if (strSubstring.length() > 0) {
                    namespaceForPrefix = this.m_namespaceContext.getNamespaceForPrefix(strSubstring);
                } else {
                    namespaceForPrefix = this.m_namespaceContext.getNamespaceForPrefix(strSubstring);
                }
            } catch (ClassCastException e) {
                namespaceForPrefix = this.m_namespaceContext.getNamespaceForPrefix(strSubstring);
            }
        } else {
            namespaceForPrefix = strSubstring;
        }
        if (namespaceForPrefix != null && namespaceForPrefix.length() > 0) {
            addToTokenQueue(namespaceForPrefix);
            addToTokenQueue(":");
            String strSubstring2 = str.substring(i2 + 1, i3);
            if (strSubstring2.length() > 0) {
                addToTokenQueue(strSubstring2);
                return -1;
            }
            return -1;
        }
        this.m_processor.errorForDOM3("ER_PREFIX_MUST_RESOLVE", new String[]{strSubstring});
        return -1;
    }
}
