package org.apache.xpath.compiler;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.templates.Constants;
import org.apache.xml.utils.ObjectVector;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathProcessorException;
import org.apache.xpath.domapi.XPathStylesheetDOM3Exception;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XString;
import org.apache.xpath.res.XPATHErrorResources;

public class XPathParser {
    public static final String CONTINUE_AFTER_FATAL_ERROR = "CONTINUE_AFTER_FATAL_ERROR";
    protected static final int FILTER_MATCH_FAILED = 0;
    protected static final int FILTER_MATCH_PREDICATES = 2;
    protected static final int FILTER_MATCH_PRIMARY = 1;
    private ErrorListener m_errorListener;
    private FunctionTable m_functionTable;
    PrefixResolver m_namespaceContext;
    private OpMap m_ops;
    SourceLocator m_sourceLocator;
    transient String m_token;
    transient char m_tokenChar = 0;
    int m_queueMark = 0;

    public XPathParser(ErrorListener errorListener, SourceLocator sourceLocator) {
        this.m_errorListener = errorListener;
        this.m_sourceLocator = sourceLocator;
    }

    public void initXPath(Compiler compiler, String str, PrefixResolver prefixResolver) throws TransformerException {
        this.m_ops = compiler;
        this.m_namespaceContext = prefixResolver;
        this.m_functionTable = compiler.getFunctionTable();
        new Lexer(compiler, prefixResolver, this).tokenize(str);
        this.m_ops.setOp(0, 1);
        this.m_ops.setOp(1, 2);
        try {
            nextToken();
            Expr();
            if (this.m_token != null) {
                String str2 = "";
                while (this.m_token != null) {
                    str2 = str2 + "'" + this.m_token + "'";
                    nextToken();
                    if (this.m_token != null) {
                        str2 = str2 + ", ";
                    }
                }
                error(XPATHErrorResources.ER_EXTRA_ILLEGAL_TOKENS, new Object[]{str2});
            }
        } catch (XPathProcessorException e) {
            if (CONTINUE_AFTER_FATAL_ERROR.equals(e.getMessage())) {
                initXPath(compiler, "/..", prefixResolver);
            } else {
                throw e;
            }
        }
        compiler.shrink();
    }

    public void initMatchPattern(Compiler compiler, String str, PrefixResolver prefixResolver) throws TransformerException {
        this.m_ops = compiler;
        this.m_namespaceContext = prefixResolver;
        this.m_functionTable = compiler.getFunctionTable();
        new Lexer(compiler, prefixResolver, this).tokenize(str);
        this.m_ops.setOp(0, 30);
        this.m_ops.setOp(1, 2);
        nextToken();
        Pattern();
        if (this.m_token != null) {
            String str2 = "";
            while (this.m_token != null) {
                str2 = str2 + "'" + this.m_token + "'";
                nextToken();
                if (this.m_token != null) {
                    str2 = str2 + ", ";
                }
            }
            error(XPATHErrorResources.ER_EXTRA_ILLEGAL_TOKENS, new Object[]{str2});
        }
        this.m_ops.setOp(this.m_ops.getOp(1), -1);
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        this.m_ops.shrink();
    }

    public void setErrorHandler(ErrorListener errorListener) {
        this.m_errorListener = errorListener;
    }

    public ErrorListener getErrorListener() {
        return this.m_errorListener;
    }

    final boolean tokenIs(String str) {
        return this.m_token != null ? this.m_token.equals(str) : str == null;
    }

    final boolean tokenIs(char c) {
        return this.m_token != null && this.m_tokenChar == c;
    }

    final boolean lookahead(char c, int i) {
        int i2 = this.m_queueMark + i;
        if (i2 > this.m_ops.getTokenQueueSize() || i2 <= 0 || this.m_ops.getTokenQueueSize() == 0) {
            return false;
        }
        String str = (String) this.m_ops.m_tokenQueue.elementAt(i2 - 1);
        return str.length() == 1 && str.charAt(0) == c;
    }

    private final boolean lookbehind(char c, int i) {
        char cCharAt;
        int i2 = this.m_queueMark - (i + 1);
        if (i2 < 0) {
            return false;
        }
        String str = (String) this.m_ops.m_tokenQueue.elementAt(i2);
        if (str.length() != 1) {
            return false;
        }
        if (str != null) {
            cCharAt = str.charAt(0);
        } else {
            cCharAt = '|';
        }
        return cCharAt != '|' && cCharAt == c;
    }

    private final boolean lookbehindHasToken(int i) {
        char cCharAt;
        if (this.m_queueMark - i <= 0) {
            return false;
        }
        String str = (String) this.m_ops.m_tokenQueue.elementAt(this.m_queueMark - (i - 1));
        if (str != null) {
            cCharAt = str.charAt(0);
        } else {
            cCharAt = '|';
        }
        return cCharAt != '|';
    }

    private final boolean lookahead(String str, int i) {
        if (this.m_queueMark + i > this.m_ops.getTokenQueueSize()) {
            return str == null;
        }
        String str2 = (String) this.m_ops.m_tokenQueue.elementAt(this.m_queueMark + (i - 1));
        return str2 != null ? str2.equals(str) : str == null;
    }

    private final void nextToken() {
        if (this.m_queueMark < this.m_ops.getTokenQueueSize()) {
            ObjectVector objectVector = this.m_ops.m_tokenQueue;
            int i = this.m_queueMark;
            this.m_queueMark = i + 1;
            this.m_token = (String) objectVector.elementAt(i);
            this.m_tokenChar = this.m_token.charAt(0);
            return;
        }
        this.m_token = null;
        this.m_tokenChar = (char) 0;
    }

    private final String getTokenRelative(int i) {
        int i2 = this.m_queueMark + i;
        if (i2 > 0 && i2 < this.m_ops.getTokenQueueSize()) {
            return (String) this.m_ops.m_tokenQueue.elementAt(i2);
        }
        return null;
    }

    private final void prevToken() {
        if (this.m_queueMark > 0) {
            this.m_queueMark--;
            this.m_token = (String) this.m_ops.m_tokenQueue.elementAt(this.m_queueMark);
            this.m_tokenChar = this.m_token.charAt(0);
        } else {
            this.m_token = null;
            this.m_tokenChar = (char) 0;
        }
    }

    private final void consumeExpected(String str) throws TransformerException {
        if (tokenIs(str)) {
            nextToken();
        } else {
            error(XPATHErrorResources.ER_EXPECTED_BUT_FOUND, new Object[]{str, this.m_token});
            throw new XPathProcessorException(CONTINUE_AFTER_FATAL_ERROR);
        }
    }

    private final void consumeExpected(char c) throws TransformerException {
        if (tokenIs(c)) {
            nextToken();
        } else {
            error(XPATHErrorResources.ER_EXPECTED_BUT_FOUND, new Object[]{String.valueOf(c), this.m_token});
            throw new XPathProcessorException(CONTINUE_AFTER_FATAL_ERROR);
        }
    }

    void warn(String str, Object[] objArr) throws TransformerException {
        String strCreateXPATHWarning = XSLMessages.createXPATHWarning(str, objArr);
        ErrorListener errorListener = getErrorListener();
        if (errorListener != null) {
            errorListener.warning(new TransformerException(strCreateXPATHWarning, this.m_sourceLocator));
        } else {
            System.err.println(strCreateXPATHWarning);
        }
    }

    private void assertion(boolean z, String str) {
        if (!z) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{str}));
        }
    }

    void error(String str, Object[] objArr) throws TransformerException {
        String strCreateXPATHMessage = XSLMessages.createXPATHMessage(str, objArr);
        ErrorListener errorListener = getErrorListener();
        TransformerException transformerException = new TransformerException(strCreateXPATHMessage, this.m_sourceLocator);
        if (errorListener != null) {
            errorListener.fatalError(transformerException);
            return;
        }
        throw transformerException;
    }

    void errorForDOM3(String str, Object[] objArr) throws TransformerException {
        String strCreateXPATHMessage = XSLMessages.createXPATHMessage(str, objArr);
        ErrorListener errorListener = getErrorListener();
        XPathStylesheetDOM3Exception xPathStylesheetDOM3Exception = new XPathStylesheetDOM3Exception(strCreateXPATHMessage, this.m_sourceLocator);
        if (errorListener != null) {
            errorListener.fatalError(xPathStylesheetDOM3Exception);
            return;
        }
        throw xPathStylesheetDOM3Exception;
    }

    protected String dumpRemainingTokenQueue() {
        int i = this.m_queueMark;
        if (i < this.m_ops.getTokenQueueSize()) {
            String str = "\n Remaining tokens: (";
            while (i < this.m_ops.getTokenQueueSize()) {
                int i2 = i + 1;
                str = str + " '" + ((String) this.m_ops.m_tokenQueue.elementAt(i)) + "'";
                i = i2;
            }
            return str + ")";
        }
        return "";
    }

    final int getFunctionToken(String str) {
        try {
            Object objLookupNodeTest = Keywords.lookupNodeTest(str);
            if (objLookupNodeTest == null) {
                objLookupNodeTest = this.m_functionTable.getFunctionID(str);
            }
            return ((Integer) objLookupNodeTest).intValue();
        } catch (ClassCastException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    void insertOp(int i, int i2, int i3) {
        int op = this.m_ops.getOp(1);
        for (int i4 = op - 1; i4 >= i; i4--) {
            this.m_ops.setOp(i4 + i2, this.m_ops.getOp(i4));
        }
        this.m_ops.setOp(i, i3);
        this.m_ops.setOp(1, op + i2);
    }

    void appendOp(int i, int i2) {
        int op = this.m_ops.getOp(1);
        this.m_ops.setOp(op, i2);
        this.m_ops.setOp(op + 1, i);
        this.m_ops.setOp(1, op + i);
    }

    protected void Expr() throws TransformerException {
        OrExpr();
    }

    protected void OrExpr() throws TransformerException {
        int op = this.m_ops.getOp(1);
        AndExpr();
        if (this.m_token != null && tokenIs("or")) {
            nextToken();
            insertOp(op, 2, 2);
            OrExpr();
            this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
        }
    }

    protected void AndExpr() throws TransformerException {
        int op = this.m_ops.getOp(1);
        EqualityExpr(-1);
        if (this.m_token != null && tokenIs("and")) {
            nextToken();
            insertOp(op, 2, 3);
            AndExpr();
            this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
        }
    }

    protected int EqualityExpr(int i) throws TransformerException {
        int op = this.m_ops.getOp(1);
        if (-1 == i) {
            i = op;
        }
        RelationalExpr(-1);
        if (this.m_token != null) {
            if (tokenIs('!') && lookahead('=', 1)) {
                nextToken();
                nextToken();
                insertOp(i, 2, 4);
                int op2 = this.m_ops.getOp(1) - i;
                int iEqualityExpr = EqualityExpr(i);
                this.m_ops.setOp(iEqualityExpr + 1, this.m_ops.getOp(iEqualityExpr + op2 + 1) + op2);
                return iEqualityExpr + 2;
            }
            if (tokenIs('=')) {
                nextToken();
                insertOp(i, 2, 5);
                int op3 = this.m_ops.getOp(1) - i;
                int iEqualityExpr2 = EqualityExpr(i);
                this.m_ops.setOp(iEqualityExpr2 + 1, this.m_ops.getOp(iEqualityExpr2 + op3 + 1) + op3);
                return iEqualityExpr2 + 2;
            }
            return i;
        }
        return i;
    }

    protected int RelationalExpr(int i) throws TransformerException {
        int op = this.m_ops.getOp(1);
        if (-1 == i) {
            i = op;
        }
        AdditiveExpr(-1);
        if (this.m_token != null) {
            if (tokenIs('<')) {
                nextToken();
                if (tokenIs('=')) {
                    nextToken();
                    insertOp(i, 2, 6);
                } else {
                    insertOp(i, 2, 7);
                }
                int op2 = this.m_ops.getOp(1) - i;
                int iRelationalExpr = RelationalExpr(i);
                this.m_ops.setOp(iRelationalExpr + 1, this.m_ops.getOp(iRelationalExpr + op2 + 1) + op2);
                return iRelationalExpr + 2;
            }
            if (tokenIs('>')) {
                nextToken();
                if (tokenIs('=')) {
                    nextToken();
                    insertOp(i, 2, 8);
                } else {
                    insertOp(i, 2, 9);
                }
                int op3 = this.m_ops.getOp(1) - i;
                int iRelationalExpr2 = RelationalExpr(i);
                this.m_ops.setOp(iRelationalExpr2 + 1, this.m_ops.getOp(iRelationalExpr2 + op3 + 1) + op3);
                return iRelationalExpr2 + 2;
            }
            return i;
        }
        return i;
    }

    protected int AdditiveExpr(int i) throws TransformerException {
        int op = this.m_ops.getOp(1);
        if (-1 == i) {
            i = op;
        }
        MultiplicativeExpr(-1);
        if (this.m_token != null) {
            if (tokenIs('+')) {
                nextToken();
                insertOp(i, 2, 10);
                int op2 = this.m_ops.getOp(1) - i;
                int iAdditiveExpr = AdditiveExpr(i);
                this.m_ops.setOp(iAdditiveExpr + 1, this.m_ops.getOp(iAdditiveExpr + op2 + 1) + op2);
                return iAdditiveExpr + 2;
            }
            if (tokenIs('-')) {
                nextToken();
                insertOp(i, 2, 11);
                int op3 = this.m_ops.getOp(1) - i;
                int iAdditiveExpr2 = AdditiveExpr(i);
                this.m_ops.setOp(iAdditiveExpr2 + 1, this.m_ops.getOp(iAdditiveExpr2 + op3 + 1) + op3);
                return iAdditiveExpr2 + 2;
            }
            return i;
        }
        return i;
    }

    protected int MultiplicativeExpr(int i) throws TransformerException {
        int op = this.m_ops.getOp(1);
        if (-1 == i) {
            i = op;
        }
        UnaryExpr();
        if (this.m_token != null) {
            if (tokenIs('*')) {
                nextToken();
                insertOp(i, 2, 12);
                int op2 = this.m_ops.getOp(1) - i;
                int iMultiplicativeExpr = MultiplicativeExpr(i);
                this.m_ops.setOp(iMultiplicativeExpr + 1, this.m_ops.getOp(iMultiplicativeExpr + op2 + 1) + op2);
                return iMultiplicativeExpr + 2;
            }
            if (tokenIs("div")) {
                nextToken();
                insertOp(i, 2, 13);
                int op3 = this.m_ops.getOp(1) - i;
                int iMultiplicativeExpr2 = MultiplicativeExpr(i);
                this.m_ops.setOp(iMultiplicativeExpr2 + 1, this.m_ops.getOp(iMultiplicativeExpr2 + op3 + 1) + op3);
                return iMultiplicativeExpr2 + 2;
            }
            if (tokenIs("mod")) {
                nextToken();
                insertOp(i, 2, 14);
                int op4 = this.m_ops.getOp(1) - i;
                int iMultiplicativeExpr3 = MultiplicativeExpr(i);
                this.m_ops.setOp(iMultiplicativeExpr3 + 1, this.m_ops.getOp(iMultiplicativeExpr3 + op4 + 1) + op4);
                return iMultiplicativeExpr3 + 2;
            }
            if (tokenIs("quo")) {
                nextToken();
                insertOp(i, 2, 15);
                int op5 = this.m_ops.getOp(1) - i;
                int iMultiplicativeExpr4 = MultiplicativeExpr(i);
                this.m_ops.setOp(iMultiplicativeExpr4 + 1, this.m_ops.getOp(iMultiplicativeExpr4 + op5 + 1) + op5);
                return iMultiplicativeExpr4 + 2;
            }
            return i;
        }
        return i;
    }

    protected void UnaryExpr() throws TransformerException {
        boolean z;
        int op = this.m_ops.getOp(1);
        if (this.m_tokenChar == '-') {
            nextToken();
            appendOp(2, 16);
            z = true;
        } else {
            z = false;
        }
        UnionExpr();
        if (z) {
            this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
        }
    }

    protected void StringExpr() throws TransformerException {
        int op = this.m_ops.getOp(1);
        appendOp(2, 17);
        Expr();
        this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
    }

    protected void BooleanExpr() throws TransformerException {
        int op = this.m_ops.getOp(1);
        appendOp(2, 18);
        Expr();
        int op2 = this.m_ops.getOp(1) - op;
        if (op2 == 2) {
            error(XPATHErrorResources.ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL, null);
        }
        this.m_ops.setOp(op + 1, op2);
    }

    protected void NumberExpr() throws TransformerException {
        int op = this.m_ops.getOp(1);
        appendOp(2, 19);
        Expr();
        this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
    }

    protected void UnionExpr() throws TransformerException {
        int op = this.m_ops.getOp(1);
        boolean z = false;
        while (true) {
            PathExpr();
            if (tokenIs('|')) {
                if (!z) {
                    insertOp(op, 2, 20);
                    z = true;
                }
                nextToken();
            } else {
                this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
                return;
            }
        }
    }

    protected void PathExpr() throws TransformerException {
        boolean z;
        int op = this.m_ops.getOp(1);
        int iFilterExpr = FilterExpr();
        if (iFilterExpr != 0) {
            if (iFilterExpr != 2) {
                z = false;
            } else {
                z = true;
            }
            if (tokenIs('/')) {
                nextToken();
                if (!z) {
                    insertOp(op, 2, 28);
                    z = true;
                }
                if (!RelativeLocationPath()) {
                    error(XPATHErrorResources.ER_EXPECTED_REL_LOC_PATH, null);
                }
            }
            if (z) {
                this.m_ops.setOp(this.m_ops.getOp(1), -1);
                this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
                this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
                return;
            }
            return;
        }
        LocationPath();
    }

    protected int FilterExpr() throws TransformerException {
        int op = this.m_ops.getOp(1);
        if (PrimaryExpr()) {
            if (!tokenIs('[')) {
                return 1;
            }
            insertOp(op, 2, 28);
            while (tokenIs('[')) {
                Predicate();
            }
            return 2;
        }
        return 0;
    }

    protected boolean PrimaryExpr() throws TransformerException {
        int op = this.m_ops.getOp(1);
        if (this.m_tokenChar == '\'' || this.m_tokenChar == '\"') {
            appendOp(2, 21);
            Literal();
            this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
            return true;
        }
        if (this.m_tokenChar == '$') {
            nextToken();
            appendOp(2, 22);
            QName();
            this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
            return true;
        }
        if (this.m_tokenChar == '(') {
            nextToken();
            appendOp(2, 23);
            Expr();
            consumeExpected(')');
            this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
            return true;
        }
        if (this.m_token != null && (('.' == this.m_tokenChar && this.m_token.length() > 1 && Character.isDigit(this.m_token.charAt(1))) || Character.isDigit(this.m_tokenChar))) {
            appendOp(2, 27);
            Number();
            this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
            return true;
        }
        if (lookahead('(', 1) || (lookahead(':', 1) && lookahead('(', 3))) {
            return FunctionCall();
        }
        return false;
    }

    protected void Argument() throws TransformerException {
        int op = this.m_ops.getOp(1);
        appendOp(2, 26);
        Expr();
        this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
    }

    protected boolean FunctionCall() throws TransformerException {
        int op = this.m_ops.getOp(1);
        if (lookahead(':', 1)) {
            appendOp(4, 24);
            int i = op + 1;
            this.m_ops.setOp(i + 1, this.m_queueMark - 1);
            nextToken();
            consumeExpected(':');
            this.m_ops.setOp(i + 2, this.m_queueMark - 1);
            nextToken();
        } else {
            int functionToken = getFunctionToken(this.m_token);
            if (-1 == functionToken) {
                error(XPATHErrorResources.ER_COULDNOT_FIND_FUNCTION, new Object[]{this.m_token});
            }
            switch (functionToken) {
                case OpCodes.NODETYPE_COMMENT:
                case OpCodes.NODETYPE_TEXT:
                case OpCodes.NODETYPE_PI:
                case OpCodes.NODETYPE_NODE:
                    return false;
                default:
                    appendOp(3, 25);
                    this.m_ops.setOp(op + 1 + 1, functionToken);
                    nextToken();
                    break;
            }
        }
        consumeExpected('(');
        while (!tokenIs(')') && this.m_token != null) {
            if (tokenIs(',')) {
                error(XPATHErrorResources.ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG, null);
            }
            Argument();
            if (!tokenIs(')')) {
                consumeExpected(',');
                if (tokenIs(')')) {
                    error(XPATHErrorResources.ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG, null);
                }
            }
        }
        consumeExpected(')');
        this.m_ops.setOp(this.m_ops.getOp(1), -1);
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
        return true;
    }

    protected void LocationPath() throws TransformerException {
        int op = this.m_ops.getOp(1);
        appendOp(2, 28);
        boolean z = tokenIs('/');
        if (z) {
            appendOp(4, 50);
            this.m_ops.setOp(this.m_ops.getOp(1) - 2, 4);
            this.m_ops.setOp(this.m_ops.getOp(1) - 1, 35);
            nextToken();
        } else if (this.m_token == null) {
            error(XPATHErrorResources.ER_EXPECTED_LOC_PATH_AT_END_EXPR, null);
        }
        if (this.m_token != null && !RelativeLocationPath() && !z) {
            error(XPATHErrorResources.ER_EXPECTED_LOC_PATH, new Object[]{this.m_token});
        }
        this.m_ops.setOp(this.m_ops.getOp(1), -1);
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
    }

    protected boolean RelativeLocationPath() throws TransformerException {
        if (!Step()) {
            return false;
        }
        while (tokenIs('/')) {
            nextToken();
            if (!Step()) {
                error(XPATHErrorResources.ER_EXPECTED_LOC_STEP, null);
            }
        }
        return true;
    }

    protected boolean Step() throws TransformerException {
        int op = this.m_ops.getOp(1);
        boolean z = tokenIs('/');
        if (z) {
            nextToken();
            appendOp(2, 42);
            this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
            this.m_ops.setOp(this.m_ops.getOp(1), OpCodes.NODETYPE_NODE);
            this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
            int i = op + 1;
            this.m_ops.setOp(i + 1, this.m_ops.getOp(1) - op);
            this.m_ops.setOp(i, this.m_ops.getOp(1) - op);
            op = this.m_ops.getOp(1);
        }
        if (tokenIs(Constants.ATTRVAL_THIS)) {
            nextToken();
            if (tokenIs('[')) {
                error(XPATHErrorResources.ER_PREDICATE_ILLEGAL_SYNTAX, null);
            }
            appendOp(4, 48);
            this.m_ops.setOp(this.m_ops.getOp(1) - 2, 4);
            this.m_ops.setOp(this.m_ops.getOp(1) - 1, OpCodes.NODETYPE_NODE);
        } else if (tokenIs(Constants.ATTRVAL_PARENT)) {
            nextToken();
            appendOp(4, 45);
            this.m_ops.setOp(this.m_ops.getOp(1) - 2, 4);
            this.m_ops.setOp(this.m_ops.getOp(1) - 1, OpCodes.NODETYPE_NODE);
        } else if (tokenIs('*') || tokenIs('@') || tokenIs('_') || (this.m_token != null && Character.isLetter(this.m_token.charAt(0)))) {
            Basis();
            while (tokenIs('[')) {
                Predicate();
            }
            this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
        } else {
            if (z) {
                error(XPATHErrorResources.ER_EXPECTED_LOC_STEP, null);
            }
            return false;
        }
        return true;
    }

    protected void Basis() throws TransformerException {
        int iAxisName;
        int op = this.m_ops.getOp(1);
        if (lookahead("::", 1)) {
            iAxisName = AxisName();
            nextToken();
            nextToken();
        } else if (tokenIs('@')) {
            iAxisName = 39;
            appendOp(2, 39);
            nextToken();
        } else {
            iAxisName = 40;
            appendOp(2, 40);
        }
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        NodeTest(iAxisName);
        this.m_ops.setOp(op + 1 + 1, this.m_ops.getOp(1) - op);
    }

    protected int AxisName() throws TransformerException {
        Object axisName = Keywords.getAxisName(this.m_token);
        if (axisName == null) {
            error(XPATHErrorResources.ER_ILLEGAL_AXIS_NAME, new Object[]{this.m_token});
        }
        int iIntValue = ((Integer) axisName).intValue();
        appendOp(2, iIntValue);
        return iIntValue;
    }

    protected void NodeTest(int i) throws TransformerException {
        if (lookahead('(', 1)) {
            Object nodeType = Keywords.getNodeType(this.m_token);
            if (nodeType == null) {
                error(XPATHErrorResources.ER_UNKNOWN_NODETYPE, new Object[]{this.m_token});
                return;
            }
            nextToken();
            int iIntValue = ((Integer) nodeType).intValue();
            this.m_ops.setOp(this.m_ops.getOp(1), iIntValue);
            this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
            consumeExpected('(');
            if (1032 == iIntValue && !tokenIs(')')) {
                Literal();
            }
            consumeExpected(')');
            return;
        }
        this.m_ops.setOp(this.m_ops.getOp(1), 34);
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        if (lookahead(':', 1)) {
            if (tokenIs('*')) {
                this.m_ops.setOp(this.m_ops.getOp(1), -3);
            } else {
                this.m_ops.setOp(this.m_ops.getOp(1), this.m_queueMark - 1);
                if (!Character.isLetter(this.m_tokenChar) && !tokenIs('_')) {
                    error(XPATHErrorResources.ER_EXPECTED_NODE_TEST, null);
                }
            }
            nextToken();
            consumeExpected(':');
        } else {
            this.m_ops.setOp(this.m_ops.getOp(1), -2);
        }
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        if (tokenIs('*')) {
            this.m_ops.setOp(this.m_ops.getOp(1), -3);
        } else {
            this.m_ops.setOp(this.m_ops.getOp(1), this.m_queueMark - 1);
            if (!Character.isLetter(this.m_tokenChar) && !tokenIs('_')) {
                error(XPATHErrorResources.ER_EXPECTED_NODE_TEST, null);
            }
        }
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        nextToken();
    }

    protected void Predicate() throws TransformerException {
        if (tokenIs('[')) {
            nextToken();
            PredicateExpr();
            consumeExpected(']');
        }
    }

    protected void PredicateExpr() throws TransformerException {
        int op = this.m_ops.getOp(1);
        appendOp(2, 29);
        Expr();
        this.m_ops.setOp(this.m_ops.getOp(1), -1);
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
    }

    protected void QName() throws TransformerException {
        if (lookahead(':', 1)) {
            this.m_ops.setOp(this.m_ops.getOp(1), this.m_queueMark - 1);
            this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
            nextToken();
            consumeExpected(':');
        } else {
            this.m_ops.setOp(this.m_ops.getOp(1), -2);
            this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        }
        this.m_ops.setOp(this.m_ops.getOp(1), this.m_queueMark - 1);
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        nextToken();
    }

    protected void NCName() {
        this.m_ops.setOp(this.m_ops.getOp(1), this.m_queueMark - 1);
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        nextToken();
    }

    protected void Literal() throws TransformerException {
        int length = this.m_token.length() - 1;
        char c = this.m_tokenChar;
        char cCharAt = this.m_token.charAt(length);
        if ((c == '\"' && cCharAt == '\"') || (c == '\'' && cCharAt == '\'')) {
            int i = this.m_queueMark - 1;
            this.m_ops.m_tokenQueue.setElementAt(null, i);
            this.m_ops.m_tokenQueue.setElementAt(new XString(this.m_token.substring(1, length)), i);
            this.m_ops.setOp(this.m_ops.getOp(1), i);
            this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
            nextToken();
            return;
        }
        error(XPATHErrorResources.ER_PATTERN_LITERAL_NEEDS_BE_QUOTED, new Object[]{this.m_token});
    }

    protected void Number() throws TransformerException {
        double dDoubleValue;
        if (this.m_token != null) {
            try {
            } catch (NumberFormatException e) {
                dDoubleValue = XPath.MATCH_SCORE_QNAME;
                error(XPATHErrorResources.ER_COULDNOT_BE_FORMATTED_TO_NUMBER, new Object[]{this.m_token});
            }
            if (this.m_token.indexOf(101) > -1 || this.m_token.indexOf(69) > -1) {
                throw new NumberFormatException();
            }
            dDoubleValue = Double.valueOf(this.m_token).doubleValue();
            this.m_ops.m_tokenQueue.setElementAt(new XNumber(dDoubleValue), this.m_queueMark - 1);
            this.m_ops.setOp(this.m_ops.getOp(1), this.m_queueMark - 1);
            this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
            nextToken();
        }
    }

    protected void Pattern() throws TransformerException {
        while (true) {
            LocationPathPattern();
            if (tokenIs('|')) {
                nextToken();
            } else {
                return;
            }
        }
    }

    protected void LocationPathPattern() throws TransformerException {
        char c;
        int op = this.m_ops.getOp(1);
        appendOp(2, 31);
        if (lookahead('(', 1) && (tokenIs("id") || tokenIs("key"))) {
            IdKeyPattern();
            if (tokenIs('/')) {
                nextToken();
                if (tokenIs('/')) {
                    appendOp(4, 52);
                    nextToken();
                } else {
                    appendOp(4, 53);
                }
                this.m_ops.setOp(this.m_ops.getOp(1) - 2, 4);
                this.m_ops.setOp(this.m_ops.getOp(1) - 1, OpCodes.NODETYPE_FUNCTEST);
                c = 2;
            } else {
                c = 0;
            }
        } else if (tokenIs('/')) {
            if (lookahead('/', 1)) {
                appendOp(4, 52);
                nextToken();
                c = 2;
            } else {
                appendOp(4, 50);
                c = 1;
            }
            this.m_ops.setOp(this.m_ops.getOp(1) - 2, 4);
            this.m_ops.setOp(this.m_ops.getOp(1) - 1, 35);
            nextToken();
        } else {
            c = 2;
        }
        if (c != 0) {
            if (!tokenIs('|') && this.m_token != null) {
                RelativePathPattern();
            } else if (c == 2) {
                error(XPATHErrorResources.ER_EXPECTED_REL_PATH_PATTERN, null);
            }
        }
        this.m_ops.setOp(this.m_ops.getOp(1), -1);
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        this.m_ops.setOp(op + 1, this.m_ops.getOp(1) - op);
    }

    protected void IdKeyPattern() throws TransformerException {
        FunctionCall();
    }

    protected void RelativePathPattern() throws TransformerException {
        boolean zStepPattern = StepPattern(false);
        while (tokenIs('/')) {
            nextToken();
            zStepPattern = StepPattern(!zStepPattern);
        }
    }

    protected boolean StepPattern(boolean z) throws TransformerException {
        return AbbreviatedNodeTestStep(z);
    }

    protected boolean AbbreviatedNodeTestStep(boolean z) throws TransformerException {
        int op;
        int op2 = this.m_ops.getOp(1);
        boolean z2 = false;
        int i = 53;
        if (tokenIs('@')) {
            appendOp(2, 51);
            nextToken();
            i = 51;
        } else {
            if (lookahead("::", 1)) {
                if (tokenIs("attribute")) {
                    appendOp(2, 51);
                    i = 51;
                    op = -1;
                } else if (tokenIs("child")) {
                    op = this.m_ops.getOp(1);
                    appendOp(2, 53);
                } else {
                    error(XPATHErrorResources.ER_AXES_NOT_ALLOWED, new Object[]{this.m_token});
                    op = -1;
                    i = -1;
                }
                nextToken();
                nextToken();
            } else if (tokenIs('/')) {
                if (!z) {
                    error(XPATHErrorResources.ER_EXPECTED_STEP_PATTERN, null);
                }
                appendOp(2, 52);
                nextToken();
                i = 52;
            } else {
                op = this.m_ops.getOp(1);
                appendOp(2, 53);
            }
            this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
            NodeTest(i);
            int i2 = op2 + 1;
            this.m_ops.setOp(i2 + 1, this.m_ops.getOp(1) - op2);
            while (tokenIs('[')) {
                Predicate();
            }
            if (op > -1 && tokenIs('/') && lookahead('/', 1)) {
                this.m_ops.setOp(op, 52);
                nextToken();
                z2 = true;
            }
            this.m_ops.setOp(i2, this.m_ops.getOp(1) - op2);
            return z2;
        }
        op = -1;
        this.m_ops.setOp(1, this.m_ops.getOp(1) + 1);
        NodeTest(i);
        int i22 = op2 + 1;
        this.m_ops.setOp(i22 + 1, this.m_ops.getOp(1) - op2);
        while (tokenIs('[')) {
        }
        if (op > -1) {
            this.m_ops.setOp(op, 52);
            nextToken();
            z2 = true;
        }
        this.m_ops.setOp(i22, this.m_ops.getOp(1) - op2);
        return z2;
    }
}
