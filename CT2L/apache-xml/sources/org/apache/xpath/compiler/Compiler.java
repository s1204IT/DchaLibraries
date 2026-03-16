package org.apache.xpath.compiler;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xpath.Expression;
import org.apache.xpath.axes.UnionPathIterator;
import org.apache.xpath.axes.WalkerFactory;
import org.apache.xpath.functions.FuncExtFunction;
import org.apache.xpath.functions.FuncExtFunctionAvailable;
import org.apache.xpath.functions.Function;
import org.apache.xpath.functions.WrongNumberArgsException;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XString;
import org.apache.xpath.operations.And;
import org.apache.xpath.operations.Bool;
import org.apache.xpath.operations.Div;
import org.apache.xpath.operations.Equals;
import org.apache.xpath.operations.Gt;
import org.apache.xpath.operations.Gte;
import org.apache.xpath.operations.Lt;
import org.apache.xpath.operations.Lte;
import org.apache.xpath.operations.Minus;
import org.apache.xpath.operations.Mod;
import org.apache.xpath.operations.Mult;
import org.apache.xpath.operations.Neg;
import org.apache.xpath.operations.NotEquals;
import org.apache.xpath.operations.Number;
import org.apache.xpath.operations.Operation;
import org.apache.xpath.operations.Or;
import org.apache.xpath.operations.Plus;
import org.apache.xpath.operations.String;
import org.apache.xpath.operations.UnaryOperation;
import org.apache.xpath.operations.Variable;
import org.apache.xpath.patterns.FunctionPattern;
import org.apache.xpath.patterns.StepPattern;
import org.apache.xpath.patterns.UnionPattern;
import org.apache.xpath.res.XPATHErrorResources;

public class Compiler extends OpMap {
    private static final boolean DEBUG = false;
    private static long s_nextMethodId = 0;
    private int locPathDepth;
    private PrefixResolver m_currentPrefixResolver;
    ErrorListener m_errorHandler;
    private FunctionTable m_functionTable;
    SourceLocator m_locator;

    public Compiler(ErrorListener errorHandler, SourceLocator locator, FunctionTable fTable) {
        this.locPathDepth = -1;
        this.m_currentPrefixResolver = null;
        this.m_errorHandler = errorHandler;
        this.m_locator = locator;
        this.m_functionTable = fTable;
    }

    public Compiler() {
        this.locPathDepth = -1;
        this.m_currentPrefixResolver = null;
        this.m_errorHandler = null;
        this.m_locator = null;
    }

    public Expression compile(int opPos) throws TransformerException {
        int op = getOp(opPos);
        switch (op) {
            case 1:
                Expression expr = compile(opPos + 2);
                return expr;
            case 2:
                Expression expr2 = or(opPos);
                return expr2;
            case 3:
                Expression expr3 = and(opPos);
                return expr3;
            case 4:
                Expression expr4 = notequals(opPos);
                return expr4;
            case 5:
                Expression expr5 = equals(opPos);
                return expr5;
            case 6:
                Expression expr6 = lte(opPos);
                return expr6;
            case 7:
                Expression expr7 = lt(opPos);
                return expr7;
            case 8:
                Expression expr8 = gte(opPos);
                return expr8;
            case 9:
                Expression expr9 = gt(opPos);
                return expr9;
            case 10:
                Expression expr10 = plus(opPos);
                return expr10;
            case 11:
                Expression expr11 = minus(opPos);
                return expr11;
            case 12:
                Expression expr12 = mult(opPos);
                return expr12;
            case 13:
                Expression expr13 = div(opPos);
                return expr13;
            case 14:
                Expression expr14 = mod(opPos);
                return expr14;
            case 15:
                error(XPATHErrorResources.ER_UNKNOWN_OPCODE, new Object[]{"quo"});
                return null;
            case 16:
                Expression expr15 = neg(opPos);
                return expr15;
            case 17:
                Expression expr16 = string(opPos);
                return expr16;
            case 18:
                Expression expr17 = bool(opPos);
                return expr17;
            case 19:
                Expression expr18 = number(opPos);
                return expr18;
            case 20:
                Expression expr19 = union(opPos);
                return expr19;
            case 21:
                Expression expr20 = literal(opPos);
                return expr20;
            case 22:
                Expression expr21 = variable(opPos);
                return expr21;
            case 23:
                Expression expr22 = group(opPos);
                return expr22;
            case 24:
                Expression expr23 = compileExtension(opPos);
                return expr23;
            case 25:
                Expression expr24 = compileFunction(opPos);
                return expr24;
            case 26:
                Expression expr25 = arg(opPos);
                return expr25;
            case 27:
                Expression expr26 = numberlit(opPos);
                return expr26;
            case 28:
                Expression expr27 = locationPath(opPos);
                return expr27;
            case 29:
                return null;
            case 30:
                Expression expr28 = matchPattern(opPos + 2);
                return expr28;
            case 31:
                Expression expr29 = locationPathPattern(opPos);
                return expr29;
            default:
                error(XPATHErrorResources.ER_UNKNOWN_OPCODE, new Object[]{Integer.toString(getOp(opPos))});
                return null;
        }
    }

    private Expression compileOperation(Operation operation, int opPos) throws TransformerException {
        int leftPos = getFirstChildPos(opPos);
        int rightPos = getNextOpPos(leftPos);
        operation.setLeftRight(compile(leftPos), compile(rightPos));
        return operation;
    }

    private Expression compileUnary(UnaryOperation unary, int opPos) throws TransformerException {
        int rightPos = getFirstChildPos(opPos);
        unary.setRight(compile(rightPos));
        return unary;
    }

    protected Expression or(int opPos) throws TransformerException {
        return compileOperation(new Or(), opPos);
    }

    protected Expression and(int opPos) throws TransformerException {
        return compileOperation(new And(), opPos);
    }

    protected Expression notequals(int opPos) throws TransformerException {
        return compileOperation(new NotEquals(), opPos);
    }

    protected Expression equals(int opPos) throws TransformerException {
        return compileOperation(new Equals(), opPos);
    }

    protected Expression lte(int opPos) throws TransformerException {
        return compileOperation(new Lte(), opPos);
    }

    protected Expression lt(int opPos) throws TransformerException {
        return compileOperation(new Lt(), opPos);
    }

    protected Expression gte(int opPos) throws TransformerException {
        return compileOperation(new Gte(), opPos);
    }

    protected Expression gt(int opPos) throws TransformerException {
        return compileOperation(new Gt(), opPos);
    }

    protected Expression plus(int opPos) throws TransformerException {
        return compileOperation(new Plus(), opPos);
    }

    protected Expression minus(int opPos) throws TransformerException {
        return compileOperation(new Minus(), opPos);
    }

    protected Expression mult(int opPos) throws TransformerException {
        return compileOperation(new Mult(), opPos);
    }

    protected Expression div(int opPos) throws TransformerException {
        return compileOperation(new Div(), opPos);
    }

    protected Expression mod(int opPos) throws TransformerException {
        return compileOperation(new Mod(), opPos);
    }

    protected Expression neg(int opPos) throws TransformerException {
        return compileUnary(new Neg(), opPos);
    }

    protected Expression string(int opPos) throws TransformerException {
        return compileUnary(new String(), opPos);
    }

    protected Expression bool(int opPos) throws TransformerException {
        return compileUnary(new Bool(), opPos);
    }

    protected Expression number(int opPos) throws TransformerException {
        return compileUnary(new Number(), opPos);
    }

    protected Expression literal(int opPos) {
        return (XString) getTokenQueue().elementAt(getOp(getFirstChildPos(opPos)));
    }

    protected Expression numberlit(int opPos) {
        return (XNumber) getTokenQueue().elementAt(getOp(getFirstChildPos(opPos)));
    }

    protected Expression variable(int opPos) throws TransformerException {
        Variable var = new Variable();
        int opPos2 = getFirstChildPos(opPos);
        int nsPos = getOp(opPos2);
        String namespace = -2 == nsPos ? null : (String) getTokenQueue().elementAt(nsPos);
        String localname = (String) getTokenQueue().elementAt(getOp(opPos2 + 1));
        QName qname = new QName(namespace, localname);
        var.setQName(qname);
        return var;
    }

    protected Expression group(int opPos) throws TransformerException {
        return compile(opPos + 2);
    }

    protected Expression arg(int opPos) throws TransformerException {
        return compile(opPos + 2);
    }

    protected Expression union(int opPos) throws TransformerException {
        this.locPathDepth++;
        try {
            return UnionPathIterator.createUnionIterator(this, opPos);
        } finally {
            this.locPathDepth--;
        }
    }

    public int getLocationPathDepth() {
        return this.locPathDepth;
    }

    FunctionTable getFunctionTable() {
        return this.m_functionTable;
    }

    public Expression locationPath(int opPos) throws TransformerException {
        this.locPathDepth++;
        try {
            return (Expression) WalkerFactory.newDTMIterator(this, opPos, this.locPathDepth == 0);
        } finally {
            this.locPathDepth--;
        }
    }

    public Expression predicate(int opPos) throws TransformerException {
        return compile(opPos + 2);
    }

    protected Expression matchPattern(int i) throws TransformerException {
        Expression expressionCompile;
        this.locPathDepth++;
        int nextOpPos = i;
        int i2 = 0;
        while (getOp(nextOpPos) == 31) {
            try {
                nextOpPos = getNextOpPos(nextOpPos);
                i2++;
            } finally {
                this.locPathDepth--;
            }
        }
        if (i2 != 1) {
            UnionPattern unionPattern = new UnionPattern();
            StepPattern[] stepPatternArr = new StepPattern[i2];
            int i3 = 0;
            while (getOp(i) == 31) {
                int nextOpPos2 = getNextOpPos(i);
                stepPatternArr[i3] = (StepPattern) compile(i);
                i = nextOpPos2;
                i3++;
            }
            unionPattern.setPatterns(stepPatternArr);
        }
        return expressionCompile;
    }

    public Expression locationPathPattern(int opPos) throws TransformerException {
        return stepPattern(getFirstChildPos(opPos), 0, null);
    }

    public int getWhatToShow(int opPos) {
        int axesType = getOp(opPos);
        int testType = getOp(opPos + 3);
        switch (testType) {
            case 34:
                switch (axesType) {
                    case 39:
                    case 51:
                        return 2;
                    case 49:
                        return 4096;
                    case 52:
                    case 53:
                        return 1;
                    default:
                        return 1;
                }
            case 35:
                return 1280;
            case OpCodes.NODETYPE_COMMENT:
                return 128;
            case OpCodes.NODETYPE_TEXT:
                return 12;
            case OpCodes.NODETYPE_PI:
                return 64;
            case OpCodes.NODETYPE_NODE:
                switch (axesType) {
                    case 38:
                    case 42:
                    case 48:
                        return -1;
                    case 39:
                    case 51:
                        return 2;
                    case 40:
                    case 41:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 50:
                    default:
                        if (getOp(0) == 30) {
                            return -1283;
                        }
                        return -3;
                    case 49:
                        return 4096;
                }
            case OpCodes.NODETYPE_FUNCTEST:
                return 65536;
            default:
                return -1;
        }
    }

    protected StepPattern stepPattern(int opPos, int stepCount, StepPattern ancestorPattern) throws TransformerException {
        int argLen;
        StepPattern pattern;
        int stepType = getOp(opPos);
        if (-1 == stepType) {
            return null;
        }
        int endStep = getNextOpPos(opPos);
        switch (stepType) {
            case 25:
                argLen = getOp(opPos + 1);
                pattern = new FunctionPattern(compileFunction(opPos), 10, 3);
                break;
            case 50:
                argLen = getArgLengthOfStep(opPos);
                opPos = getFirstChildPosOfStep(opPos);
                pattern = new StepPattern(1280, 10, 3);
                break;
            case 51:
                argLen = getArgLengthOfStep(opPos);
                opPos = getFirstChildPosOfStep(opPos);
                pattern = new StepPattern(2, getStepNS(opPos), getStepLocalName(opPos), 10, 2);
                break;
            case 52:
                argLen = getArgLengthOfStep(opPos);
                opPos = getFirstChildPosOfStep(opPos);
                int what = getWhatToShow(opPos);
                if (1280 == what) {
                }
                pattern = new StepPattern(getWhatToShow(opPos), getStepNS(opPos), getStepLocalName(opPos), 0, 3);
                break;
            case 53:
                argLen = getArgLengthOfStep(opPos);
                opPos = getFirstChildPosOfStep(opPos);
                pattern = new StepPattern(getWhatToShow(opPos), getStepNS(opPos), getStepLocalName(opPos), 10, 3);
                break;
            default:
                error(XPATHErrorResources.ER_UNKNOWN_MATCH_OPERATION, null);
                return null;
        }
        pattern.setPredicates(getCompiledPredicates(opPos + argLen));
        if (ancestorPattern != null) {
            pattern.setRelativePathPattern(ancestorPattern);
        }
        StepPattern relativePathPattern = stepPattern(endStep, stepCount + 1, pattern);
        return relativePathPattern == null ? pattern : relativePathPattern;
    }

    public Expression[] getCompiledPredicates(int opPos) throws TransformerException {
        int count = countPredicates(opPos);
        if (count <= 0) {
            return null;
        }
        Expression[] predicates = new Expression[count];
        compilePredicates(opPos, predicates);
        return predicates;
    }

    public int countPredicates(int opPos) throws TransformerException {
        int count = 0;
        while (29 == getOp(opPos)) {
            count++;
            opPos = getNextOpPos(opPos);
        }
        return count;
    }

    private void compilePredicates(int opPos, Expression[] predicates) throws TransformerException {
        int i = 0;
        while (29 == getOp(opPos)) {
            predicates[i] = predicate(opPos);
            opPos = getNextOpPos(opPos);
            i++;
        }
    }

    Expression compileFunction(int opPos) throws TransformerException {
        Function func = null;
        int endFunc = (getOp(opPos + 1) + opPos) - 1;
        int opPos2 = getFirstChildPos(opPos);
        int funcID = getOp(opPos2);
        int opPos3 = opPos2 + 1;
        if (-1 != funcID) {
            func = this.m_functionTable.getFunction(funcID);
            if (func instanceof FuncExtFunctionAvailable) {
                ((FuncExtFunctionAvailable) func).setFunctionTable(this.m_functionTable);
            }
            func.postCompileStep(this);
            int i = 0;
            int p = opPos3;
            while (p < endFunc) {
                try {
                    func.setArg(compile(p), i);
                    p = getNextOpPos(p);
                    i++;
                } catch (WrongNumberArgsException wnae) {
                    String name = this.m_functionTable.getFunctionName(funcID);
                    this.m_errorHandler.fatalError(new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ONLY_ALLOWS, new Object[]{name, wnae.getMessage()}), this.m_locator));
                }
            }
            func.checkNumberArgs(i);
        } else {
            error(XPATHErrorResources.ER_FUNCTION_TOKEN_NOT_FOUND, null);
        }
        return func;
    }

    private synchronized long getNextMethodId() {
        long j;
        if (s_nextMethodId == Long.MAX_VALUE) {
            s_nextMethodId = 0L;
        }
        j = s_nextMethodId;
        s_nextMethodId = 1 + j;
        return j;
    }

    private Expression compileExtension(int opPos) throws TransformerException {
        int endExtFunc = (getOp(opPos + 1) + opPos) - 1;
        int opPos2 = getFirstChildPos(opPos);
        String ns = (String) getTokenQueue().elementAt(getOp(opPos2));
        int opPos3 = opPos2 + 1;
        String funcName = (String) getTokenQueue().elementAt(getOp(opPos3));
        int opPos4 = opPos3 + 1;
        Function extension = new FuncExtFunction(ns, funcName, String.valueOf(getNextMethodId()));
        int i = 0;
        while (opPos4 < endExtFunc) {
            try {
                int nextOpPos = getNextOpPos(opPos4);
                extension.setArg(compile(opPos4), i);
                opPos4 = nextOpPos;
                i++;
            } catch (WrongNumberArgsException e) {
            }
        }
        return extension;
    }

    public void warn(String msg, Object[] args) throws TransformerException {
        String fmsg = XSLMessages.createXPATHWarning(msg, args);
        if (this.m_errorHandler != null) {
            this.m_errorHandler.warning(new TransformerException(fmsg, this.m_locator));
        } else {
            System.out.println(fmsg + "; file " + this.m_locator.getSystemId() + "; line " + this.m_locator.getLineNumber() + "; column " + this.m_locator.getColumnNumber());
        }
    }

    public void assertion(boolean b, String msg) {
        if (!b) {
            String fMsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{msg});
            throw new RuntimeException(fMsg);
        }
    }

    @Override
    public void error(String msg, Object[] args) throws TransformerException {
        String fmsg = XSLMessages.createXPATHMessage(msg, args);
        if (this.m_errorHandler != null) {
            this.m_errorHandler.fatalError(new TransformerException(fmsg, this.m_locator));
            return;
        }
        throw new TransformerException(fmsg, (SAXSourceLocator) this.m_locator);
    }

    public PrefixResolver getNamespaceContext() {
        return this.m_currentPrefixResolver;
    }

    public void setNamespaceContext(PrefixResolver pr) {
        this.m_currentPrefixResolver = pr;
    }
}
