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

    public Compiler(ErrorListener errorListener, SourceLocator sourceLocator, FunctionTable functionTable) {
        this.locPathDepth = -1;
        this.m_currentPrefixResolver = null;
        this.m_errorHandler = errorListener;
        this.m_locator = sourceLocator;
        this.m_functionTable = functionTable;
    }

    public Compiler() {
        this.locPathDepth = -1;
        this.m_currentPrefixResolver = null;
        this.m_errorHandler = null;
        this.m_locator = null;
    }

    public Expression compile(int i) throws TransformerException {
        switch (getOp(i)) {
            case 1:
                return compile(i + 2);
            case 2:
                return or(i);
            case 3:
                return and(i);
            case 4:
                return notequals(i);
            case 5:
                return equals(i);
            case 6:
                return lte(i);
            case 7:
                return lt(i);
            case 8:
                return gte(i);
            case 9:
                return gt(i);
            case 10:
                return plus(i);
            case 11:
                return minus(i);
            case 12:
                return mult(i);
            case 13:
                return div(i);
            case 14:
                return mod(i);
            case 15:
                error(XPATHErrorResources.ER_UNKNOWN_OPCODE, new Object[]{"quo"});
                return null;
            case 16:
                return neg(i);
            case 17:
                return string(i);
            case 18:
                return bool(i);
            case 19:
                return number(i);
            case 20:
                return union(i);
            case 21:
                return literal(i);
            case 22:
                return variable(i);
            case 23:
                return group(i);
            case 24:
                return compileExtension(i);
            case 25:
                return compileFunction(i);
            case 26:
                return arg(i);
            case 27:
                return numberlit(i);
            case 28:
                return locationPath(i);
            case 29:
                return null;
            case 30:
                return matchPattern(i + 2);
            case 31:
                return locationPathPattern(i);
            default:
                error(XPATHErrorResources.ER_UNKNOWN_OPCODE, new Object[]{Integer.toString(getOp(i))});
                return null;
        }
    }

    private Expression compileOperation(Operation operation, int i) throws TransformerException {
        int firstChildPos = getFirstChildPos(i);
        operation.setLeftRight(compile(firstChildPos), compile(getNextOpPos(firstChildPos)));
        return operation;
    }

    private Expression compileUnary(UnaryOperation unaryOperation, int i) throws TransformerException {
        unaryOperation.setRight(compile(getFirstChildPos(i)));
        return unaryOperation;
    }

    protected Expression or(int i) throws TransformerException {
        return compileOperation(new Or(), i);
    }

    protected Expression and(int i) throws TransformerException {
        return compileOperation(new And(), i);
    }

    protected Expression notequals(int i) throws TransformerException {
        return compileOperation(new NotEquals(), i);
    }

    protected Expression equals(int i) throws TransformerException {
        return compileOperation(new Equals(), i);
    }

    protected Expression lte(int i) throws TransformerException {
        return compileOperation(new Lte(), i);
    }

    protected Expression lt(int i) throws TransformerException {
        return compileOperation(new Lt(), i);
    }

    protected Expression gte(int i) throws TransformerException {
        return compileOperation(new Gte(), i);
    }

    protected Expression gt(int i) throws TransformerException {
        return compileOperation(new Gt(), i);
    }

    protected Expression plus(int i) throws TransformerException {
        return compileOperation(new Plus(), i);
    }

    protected Expression minus(int i) throws TransformerException {
        return compileOperation(new Minus(), i);
    }

    protected Expression mult(int i) throws TransformerException {
        return compileOperation(new Mult(), i);
    }

    protected Expression div(int i) throws TransformerException {
        return compileOperation(new Div(), i);
    }

    protected Expression mod(int i) throws TransformerException {
        return compileOperation(new Mod(), i);
    }

    protected Expression neg(int i) throws TransformerException {
        return compileUnary(new Neg(), i);
    }

    protected Expression string(int i) throws TransformerException {
        return compileUnary(new String(), i);
    }

    protected Expression bool(int i) throws TransformerException {
        return compileUnary(new Bool(), i);
    }

    protected Expression number(int i) throws TransformerException {
        return compileUnary(new Number(), i);
    }

    protected Expression literal(int i) {
        return (XString) getTokenQueue().elementAt(getOp(getFirstChildPos(i)));
    }

    protected Expression numberlit(int i) {
        return (XNumber) getTokenQueue().elementAt(getOp(getFirstChildPos(i)));
    }

    protected Expression variable(int i) throws TransformerException {
        Variable variable = new Variable();
        int firstChildPos = getFirstChildPos(i);
        int op = getOp(firstChildPos);
        variable.setQName(new QName(-2 == op ? null : (String) getTokenQueue().elementAt(op), (String) getTokenQueue().elementAt(getOp(firstChildPos + 1))));
        return variable;
    }

    protected Expression group(int i) throws TransformerException {
        return compile(i + 2);
    }

    protected Expression arg(int i) throws TransformerException {
        return compile(i + 2);
    }

    protected Expression union(int i) throws TransformerException {
        this.locPathDepth++;
        try {
            return UnionPathIterator.createUnionIterator(this, i);
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

    public Expression locationPath(int i) throws TransformerException {
        this.locPathDepth++;
        try {
            return (Expression) WalkerFactory.newDTMIterator(this, i, this.locPathDepth == 0);
        } finally {
            this.locPathDepth--;
        }
    }

    public Expression predicate(int i) throws TransformerException {
        return compile(i + 2);
    }

    protected Expression matchPattern(int i) throws TransformerException {
        this.locPathDepth++;
        int i2 = 0;
        int nextOpPos = i;
        int i3 = 0;
        while (getOp(nextOpPos) == 31) {
            try {
                nextOpPos = getNextOpPos(nextOpPos);
                i3++;
            } finally {
                this.locPathDepth--;
            }
        }
        if (i3 == 1) {
            return compile(i);
        }
        UnionPattern unionPattern = new UnionPattern();
        StepPattern[] stepPatternArr = new StepPattern[i3];
        while (getOp(i) == 31) {
            int nextOpPos2 = getNextOpPos(i);
            stepPatternArr[i2] = (StepPattern) compile(i);
            i2++;
            i = nextOpPos2;
        }
        unionPattern.setPatterns(stepPatternArr);
        return unionPattern;
    }

    public Expression locationPathPattern(int i) throws TransformerException {
        return stepPattern(getFirstChildPos(i), 0, null);
    }

    public int getWhatToShow(int i) {
        int op = getOp(i);
        int op2 = getOp(i + 3);
        switch (op2) {
            case 34:
                if (op != 39) {
                    if (op == 49) {
                        return 4096;
                    }
                    switch (op) {
                        case 51:
                            break;
                        case 52:
                        case 53:
                            return 1;
                        default:
                            return 1;
                    }
                }
                return 2;
            case 35:
                return 1280;
            default:
                switch (op2) {
                    case OpCodes.NODETYPE_COMMENT:
                        return 128;
                    case OpCodes.NODETYPE_TEXT:
                        return 12;
                    case OpCodes.NODETYPE_PI:
                        return 64;
                    case OpCodes.NODETYPE_NODE:
                        switch (op) {
                            case 38:
                            case 42:
                            case 48:
                                return -1;
                            case 39:
                            case 51:
                                return 2;
                            case 49:
                                return 4096;
                            default:
                                if (getOp(0) == 30) {
                                    return -1283;
                                }
                                return -3;
                        }
                    case OpCodes.NODETYPE_FUNCTEST:
                        return 65536;
                    default:
                        return -1;
                }
        }
    }

    protected StepPattern stepPattern(int i, int i2, StepPattern stepPattern) throws TransformerException {
        int op;
        StepPattern functionPattern;
        int firstChildPosOfStep;
        StepPattern stepPattern2;
        int op2 = getOp(i);
        if (-1 == op2) {
            return null;
        }
        int nextOpPos = getNextOpPos(i);
        if (op2 == 25) {
            op = getOp(i + 1);
            functionPattern = new FunctionPattern(compileFunction(i), 10, 3);
        } else {
            switch (op2) {
                case 50:
                    op = getArgLengthOfStep(i);
                    i = getFirstChildPosOfStep(i);
                    functionPattern = new StepPattern(1280, 10, 3);
                    break;
                case 51:
                    op = getArgLengthOfStep(i);
                    firstChildPosOfStep = getFirstChildPosOfStep(i);
                    functionPattern = new StepPattern(2, getStepNS(i), getStepLocalName(i), 10, 2);
                    functionPattern.setPredicates(getCompiledPredicates(firstChildPosOfStep + op));
                    if (stepPattern != null) {
                        functionPattern.setRelativePathPattern(stepPattern);
                    }
                    stepPattern2 = stepPattern(nextOpPos, i2 + 1, functionPattern);
                    if (stepPattern2 == null) {
                        break;
                    }
                    break;
                case 52:
                    op = getArgLengthOfStep(i);
                    firstChildPosOfStep = getFirstChildPosOfStep(i);
                    if (1280 == getWhatToShow(i)) {
                    }
                    functionPattern = new StepPattern(getWhatToShow(i), getStepNS(i), getStepLocalName(i), 0, 3);
                    functionPattern.setPredicates(getCompiledPredicates(firstChildPosOfStep + op));
                    if (stepPattern != null) {
                    }
                    stepPattern2 = stepPattern(nextOpPos, i2 + 1, functionPattern);
                    if (stepPattern2 == null) {
                    }
                    break;
                case 53:
                    op = getArgLengthOfStep(i);
                    firstChildPosOfStep = getFirstChildPosOfStep(i);
                    functionPattern = new StepPattern(getWhatToShow(i), getStepNS(i), getStepLocalName(i), 10, 3);
                    functionPattern.setPredicates(getCompiledPredicates(firstChildPosOfStep + op));
                    if (stepPattern != null) {
                    }
                    stepPattern2 = stepPattern(nextOpPos, i2 + 1, functionPattern);
                    if (stepPattern2 == null) {
                    }
                    break;
                default:
                    error(XPATHErrorResources.ER_UNKNOWN_MATCH_OPERATION, null);
                    break;
            }
            return null;
        }
        firstChildPosOfStep = i;
        functionPattern.setPredicates(getCompiledPredicates(firstChildPosOfStep + op));
        if (stepPattern != null) {
        }
        stepPattern2 = stepPattern(nextOpPos, i2 + 1, functionPattern);
        if (stepPattern2 == null) {
            return stepPattern2;
        }
    }

    public Expression[] getCompiledPredicates(int i) throws TransformerException {
        int iCountPredicates = countPredicates(i);
        if (iCountPredicates > 0) {
            Expression[] expressionArr = new Expression[iCountPredicates];
            compilePredicates(i, expressionArr);
            return expressionArr;
        }
        return null;
    }

    public int countPredicates(int i) throws TransformerException {
        int i2 = 0;
        while (29 == getOp(i)) {
            i2++;
            i = getNextOpPos(i);
        }
        return i2;
    }

    private void compilePredicates(int i, Expression[] expressionArr) throws TransformerException {
        int i2 = 0;
        while (29 == getOp(i)) {
            expressionArr[i2] = predicate(i);
            i = getNextOpPos(i);
            i2++;
        }
    }

    Expression compileFunction(int i) throws TransformerException {
        int op = (getOp(i + 1) + i) - 1;
        int firstChildPos = getFirstChildPos(i);
        int op2 = getOp(firstChildPos);
        int nextOpPos = firstChildPos + 1;
        if (-1 != op2) {
            Function function = this.m_functionTable.getFunction(op2);
            if (function instanceof FuncExtFunctionAvailable) {
                ((FuncExtFunctionAvailable) function).setFunctionTable(this.m_functionTable);
            }
            function.postCompileStep(this);
            int i2 = 0;
            while (nextOpPos < op) {
                try {
                    function.setArg(compile(nextOpPos), i2);
                    nextOpPos = getNextOpPos(nextOpPos);
                    i2++;
                } catch (WrongNumberArgsException e) {
                    this.m_errorHandler.fatalError(new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ONLY_ALLOWS, new Object[]{this.m_functionTable.getFunctionName(op2), e.getMessage()}), this.m_locator));
                }
            }
            function.checkNumberArgs(i2);
            return function;
        }
        error(XPATHErrorResources.ER_FUNCTION_TOKEN_NOT_FOUND, null);
        return null;
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

    private Expression compileExtension(int i) throws TransformerException {
        int op = (getOp(i + 1) + i) - 1;
        int firstChildPos = getFirstChildPos(i);
        String str = (String) getTokenQueue().elementAt(getOp(firstChildPos));
        int i2 = firstChildPos + 1;
        String str2 = (String) getTokenQueue().elementAt(getOp(i2));
        int i3 = i2 + 1;
        FuncExtFunction funcExtFunction = new FuncExtFunction(str, str2, String.valueOf(getNextMethodId()));
        int i4 = 0;
        while (i3 < op) {
            try {
                int nextOpPos = getNextOpPos(i3);
                funcExtFunction.setArg(compile(i3), i4);
                i4++;
                i3 = nextOpPos;
            } catch (WrongNumberArgsException e) {
            }
        }
        return funcExtFunction;
    }

    public void warn(String str, Object[] objArr) throws TransformerException {
        String strCreateXPATHWarning = XSLMessages.createXPATHWarning(str, objArr);
        if (this.m_errorHandler != null) {
            this.m_errorHandler.warning(new TransformerException(strCreateXPATHWarning, this.m_locator));
            return;
        }
        System.out.println(strCreateXPATHWarning + "; file " + this.m_locator.getSystemId() + "; line " + this.m_locator.getLineNumber() + "; column " + this.m_locator.getColumnNumber());
    }

    public void assertion(boolean z, String str) {
        if (!z) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{str}));
        }
    }

    @Override
    public void error(String str, Object[] objArr) throws TransformerException {
        String strCreateXPATHMessage = XSLMessages.createXPATHMessage(str, objArr);
        if (this.m_errorHandler != null) {
            this.m_errorHandler.fatalError(new TransformerException(strCreateXPATHMessage, this.m_locator));
            return;
        }
        throw new TransformerException(strCreateXPATHMessage, (SAXSourceLocator) this.m_locator);
    }

    public PrefixResolver getNamespaceContext() {
        return this.m_currentPrefixResolver;
    }

    public void setNamespaceContext(PrefixResolver prefixResolver) {
        this.m_currentPrefixResolver = prefixResolver;
    }
}
