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

    Lexer(Compiler compiler, PrefixResolver resolver, XPathParser xpathProcessor) {
        this.m_compiler = compiler;
        this.m_namespaceContext = resolver;
        this.m_processor = xpathProcessor;
    }

    void tokenize(String pat) throws TransformerException {
        tokenize(pat, null);
    }

    void tokenize(String pat, Vector targetStrings) throws TransformerException {
        this.m_compiler.m_currentPattern = pat;
        this.m_patternMapSize = 0;
        int initTokQueueSize = (pat.length() < 500 ? pat.length() : 500) * 5;
        this.m_compiler.m_opMap = new OpMapVector(initTokQueueSize, 2500, 1);
        int nChars = pat.length();
        int startSubstring = -1;
        int posOfNSSep = -1;
        boolean isStartOfPat = true;
        boolean isAttrName = false;
        boolean isNum = false;
        int nesting = 0;
        int i = 0;
        while (i < nChars) {
            char c = pat.charAt(i);
            switch (c) {
                case '\t':
                case '\n':
                case '\r':
                case ' ':
                    if (startSubstring != -1) {
                        isNum = false;
                        isStartOfPat = mapPatternElemPos(nesting, isStartOfPat, isAttrName);
                        isAttrName = false;
                        if (-1 != posOfNSSep) {
                            posOfNSSep = mapNSTokens(pat, startSubstring, posOfNSSep, i);
                        } else {
                            addToTokenQueue(pat.substring(startSubstring, i));
                        }
                        startSubstring = -1;
                    }
                    break;
                case '!':
                case '$':
                case '(':
                case ')':
                case '*':
                case '+':
                case ',':
                case '/':
                case '<':
                case '=':
                case '>':
                case '[':
                case '\\':
                case ']':
                case '^':
                case '|':
                    if (startSubstring == -1) {
                        isNum = false;
                        isStartOfPat = mapPatternElemPos(nesting, isStartOfPat, isAttrName);
                        isAttrName = false;
                        if (-1 != posOfNSSep) {
                            posOfNSSep = mapNSTokens(pat, startSubstring, posOfNSSep, i);
                        } else {
                            addToTokenQueue(pat.substring(startSubstring, i));
                        }
                        startSubstring = -1;
                    } else if ('/' == c && isStartOfPat) {
                        isStartOfPat = mapPatternElemPos(nesting, isStartOfPat, isAttrName);
                    } else if ('*' == c) {
                        isStartOfPat = mapPatternElemPos(nesting, isStartOfPat, isAttrName);
                        isAttrName = false;
                    }
                    if (nesting == 0 && '|' == c) {
                        if (targetStrings != null) {
                            recordTokenString(targetStrings);
                        }
                        isStartOfPat = true;
                    }
                    if (')' != c || ']' == c) {
                        nesting--;
                    } else if ('(' == c || '[' == c) {
                        nesting++;
                    }
                    addToTokenQueue(pat.substring(i, i + 1));
                    break;
                case '\"':
                    if (startSubstring != -1) {
                        isNum = false;
                        isStartOfPat = mapPatternElemPos(nesting, isStartOfPat, isAttrName);
                        isAttrName = false;
                        if (-1 != posOfNSSep) {
                            posOfNSSep = mapNSTokens(pat, startSubstring, posOfNSSep, i);
                        } else {
                            addToTokenQueue(pat.substring(startSubstring, i));
                        }
                    }
                    startSubstring = i;
                    i++;
                    while (i < nChars) {
                        c = pat.charAt(i);
                        if (c != '\"') {
                            i++;
                        } else if (c != '\"' && i < nChars) {
                            addToTokenQueue(pat.substring(startSubstring, i + 1));
                            startSubstring = -1;
                        } else {
                            this.m_processor.error(XPATHErrorResources.ER_EXPECTED_DOUBLE_QUOTE, null);
                        }
                        break;
                    }
                    if (c != '\"') {
                        this.m_processor.error(XPATHErrorResources.ER_EXPECTED_DOUBLE_QUOTE, null);
                        break;
                    }
                    break;
                case '\'':
                    if (startSubstring != -1) {
                        isNum = false;
                        isStartOfPat = mapPatternElemPos(nesting, isStartOfPat, isAttrName);
                        isAttrName = false;
                        if (-1 != posOfNSSep) {
                            posOfNSSep = mapNSTokens(pat, startSubstring, posOfNSSep, i);
                        } else {
                            addToTokenQueue(pat.substring(startSubstring, i));
                        }
                    }
                    startSubstring = i;
                    i++;
                    while (i < nChars) {
                        c = pat.charAt(i);
                        if (c != '\'') {
                            i++;
                        } else if (c != '\'' && i < nChars) {
                            addToTokenQueue(pat.substring(startSubstring, i + 1));
                            startSubstring = -1;
                        } else {
                            this.m_processor.error(XPATHErrorResources.ER_EXPECTED_SINGLE_QUOTE, null);
                        }
                        break;
                    }
                    if (c != '\'') {
                        this.m_processor.error(XPATHErrorResources.ER_EXPECTED_SINGLE_QUOTE, null);
                        break;
                    }
                    break;
                case '-':
                    if ('-' == c) {
                        if (!isNum && startSubstring != -1) {
                            break;
                        } else {
                            isNum = false;
                            if (startSubstring == -1) {
                            }
                            if (nesting == 0) {
                                if (targetStrings != null) {
                                }
                                isStartOfPat = true;
                            }
                            if (')' != c) {
                                nesting--;
                                addToTokenQueue(pat.substring(i, i + 1));
                                break;
                            }
                        }
                    }
                    break;
                case ':':
                    if (i > 0) {
                        if (posOfNSSep == i - 1) {
                            if (startSubstring != -1 && startSubstring < i - 1) {
                                addToTokenQueue(pat.substring(startSubstring, i - 1));
                            }
                            isNum = false;
                            isAttrName = false;
                            startSubstring = -1;
                            posOfNSSep = -1;
                            addToTokenQueue(pat.substring(i - 1, i + 1));
                            break;
                        } else {
                            posOfNSSep = i;
                            if (-1 != startSubstring) {
                            }
                        }
                    } else {
                        if (-1 != startSubstring) {
                            startSubstring = i;
                            isNum = Character.isDigit(c);
                        } else if (isNum) {
                            isNum = Character.isDigit(c);
                        }
                        break;
                    }
                    break;
                case '@':
                    isAttrName = true;
                    if ('-' == c) {
                    }
                    break;
            }
            i++;
        }
        if (startSubstring != -1) {
            mapPatternElemPos(nesting, isStartOfPat, isAttrName);
            if (-1 != posOfNSSep || (this.m_namespaceContext != null && this.m_namespaceContext.handlesNullPrefixes())) {
                mapNSTokens(pat, startSubstring, posOfNSSep, nChars);
            } else {
                addToTokenQueue(pat.substring(startSubstring, nChars));
            }
        }
        if (this.m_compiler.getTokenQueueSize() == 0) {
            this.m_processor.error(XPATHErrorResources.ER_EMPTY_EXPRESSION, null);
        } else if (targetStrings != null) {
            recordTokenString(targetStrings);
        }
        this.m_processor.m_queueMark = 0;
    }

    private boolean mapPatternElemPos(int nesting, boolean isStart, boolean isAttrName) {
        if (nesting == 0) {
            if (this.m_patternMapSize >= this.m_patternMap.length) {
                int[] patternMap = this.m_patternMap;
                int len = this.m_patternMap.length;
                this.m_patternMap = new int[this.m_patternMapSize + 100];
                System.arraycopy(patternMap, 0, this.m_patternMap, 0, len);
            }
            if (!isStart) {
                this.m_patternMap[this.m_patternMapSize - 1] = r3[r4] - 10000;
            }
            this.m_patternMap[this.m_patternMapSize] = (this.m_compiler.getTokenQueueSize() - (isAttrName ? 1 : 0)) + TARGETEXTRA;
            this.m_patternMapSize++;
            return false;
        }
        return isStart;
    }

    private int getTokenQueuePosFromMap(int i) {
        int pos = this.m_patternMap[i];
        return pos >= TARGETEXTRA ? pos - 10000 : pos;
    }

    private final void resetTokenMark(int mark) {
        int qsz = this.m_compiler.getTokenQueueSize();
        XPathParser xPathParser = this.m_processor;
        if (mark <= 0) {
            mark = 0;
        } else if (mark <= qsz) {
            mark--;
        }
        xPathParser.m_queueMark = mark;
        if (this.m_processor.m_queueMark < qsz) {
            XPathParser xPathParser2 = this.m_processor;
            ObjectVector tokenQueue = this.m_compiler.getTokenQueue();
            XPathParser xPathParser3 = this.m_processor;
            int i = xPathParser3.m_queueMark;
            xPathParser3.m_queueMark = i + 1;
            xPathParser2.m_token = (String) tokenQueue.elementAt(i);
            this.m_processor.m_tokenChar = this.m_processor.m_token.charAt(0);
            return;
        }
        this.m_processor.m_token = null;
        this.m_processor.m_tokenChar = (char) 0;
    }

    final int getKeywordToken(String key) {
        try {
            Integer itok = (Integer) Keywords.getKeyWord(key);
            if (itok == null) {
                return 0;
            }
            int tok = itok.intValue();
            return tok;
        } catch (ClassCastException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    private void recordTokenString(Vector targetStrings) {
        int tokPos = getTokenQueuePosFromMap(this.m_patternMapSize - 1);
        resetTokenMark(tokPos + 1);
        if (this.m_processor.lookahead('(', 1)) {
            int tok = getKeywordToken(this.m_processor.m_token);
            switch (tok) {
                case 35:
                    targetStrings.addElement(PsuedoNames.PSEUDONAME_ROOT);
                    break;
                case 36:
                    targetStrings.addElement("*");
                    break;
                case OpCodes.NODETYPE_COMMENT:
                    targetStrings.addElement(PsuedoNames.PSEUDONAME_COMMENT);
                    break;
                case OpCodes.NODETYPE_TEXT:
                    targetStrings.addElement(PsuedoNames.PSEUDONAME_TEXT);
                    break;
                case OpCodes.NODETYPE_PI:
                    targetStrings.addElement("*");
                    break;
                case OpCodes.NODETYPE_NODE:
                    targetStrings.addElement("*");
                    break;
                default:
                    targetStrings.addElement("*");
                    break;
            }
            return;
        }
        if (this.m_processor.tokenIs('@')) {
            tokPos++;
            resetTokenMark(tokPos + 1);
        }
        if (this.m_processor.lookahead(':', 1)) {
            tokPos += 2;
        }
        targetStrings.addElement(this.m_compiler.getTokenQueue().elementAt(tokPos));
    }

    private final void addToTokenQueue(String s) {
        this.m_compiler.getTokenQueue().addElement(s);
    }

    private int mapNSTokens(String pat, int startSubstring, int posOfNSSep, int posOfScan) throws TransformerException {
        String uName;
        String prefix = "";
        if (startSubstring >= 0 && posOfNSSep >= 0) {
            prefix = pat.substring(startSubstring, posOfNSSep);
        }
        if (this.m_namespaceContext != null && !prefix.equals("*") && !prefix.equals("xmlns")) {
            try {
                if (prefix.length() > 0) {
                    uName = this.m_namespaceContext.getNamespaceForPrefix(prefix);
                } else {
                    uName = this.m_namespaceContext.getNamespaceForPrefix(prefix);
                }
            } catch (ClassCastException e) {
                uName = this.m_namespaceContext.getNamespaceForPrefix(prefix);
            }
        } else {
            uName = prefix;
        }
        if (uName != null && uName.length() > 0) {
            addToTokenQueue(uName);
            addToTokenQueue(":");
            String s = pat.substring(posOfNSSep + 1, posOfScan);
            if (s.length() > 0) {
                addToTokenQueue(s);
                return -1;
            }
            return -1;
        }
        this.m_processor.errorForDOM3("ER_PREFIX_MUST_RESOLVE", new String[]{prefix});
        return -1;
    }
}
