package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xpath.Expression;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.OpCodes;
import org.apache.xpath.compiler.OpMap;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.patterns.ContextMatchStepPattern;
import org.apache.xpath.patterns.FunctionPattern;
import org.apache.xpath.patterns.StepPattern;

public class WalkerFactory {
    public static final int BITMASK_TRAVERSES_OUTSIDE_SUBTREE = 234381312;
    public static final int BITS_COUNT = 255;
    public static final int BITS_RESERVED = 3840;
    public static final int BIT_ANCESTOR = 8192;
    public static final int BIT_ANCESTOR_OR_SELF = 16384;
    public static final int BIT_ANY_DESCENDANT_FROM_ROOT = 536870912;
    public static final int BIT_ATTRIBUTE = 32768;
    public static final int BIT_BACKWARDS_SELF = 268435456;
    public static final int BIT_CHILD = 65536;
    public static final int BIT_DESCENDANT = 131072;
    public static final int BIT_DESCENDANT_OR_SELF = 262144;
    public static final int BIT_FILTER = 67108864;
    public static final int BIT_FOLLOWING = 524288;
    public static final int BIT_FOLLOWING_SIBLING = 1048576;
    public static final int BIT_MATCH_PATTERN = Integer.MIN_VALUE;
    public static final int BIT_NAMESPACE = 2097152;
    public static final int BIT_NODETEST_ANY = 1073741824;
    public static final int BIT_PARENT = 4194304;
    public static final int BIT_PRECEDING = 8388608;
    public static final int BIT_PRECEDING_SIBLING = 16777216;
    public static final int BIT_PREDICATE = 4096;
    public static final int BIT_ROOT = 134217728;
    public static final int BIT_SELF = 33554432;
    static final boolean DEBUG_ITERATOR_CREATION = false;
    static final boolean DEBUG_PATTERN_CREATION = false;
    static final boolean DEBUG_WALKER_CREATION = false;

    static AxesWalker loadOneWalker(WalkingIterator lpi, Compiler compiler, int stepOpCodePos) throws TransformerException {
        int stepType = compiler.getOp(stepOpCodePos);
        if (stepType == -1) {
            return null;
        }
        AxesWalker firstWalker = createDefaultWalker(compiler, stepType, lpi, 0);
        firstWalker.init(compiler, stepOpCodePos, stepType);
        return firstWalker;
    }

    static AxesWalker loadWalkers(WalkingIterator lpi, Compiler compiler, int stepOpCodePos, int stepIndex) throws TransformerException {
        AxesWalker firstWalker = null;
        AxesWalker prevWalker = null;
        int analysis = analyze(compiler, stepOpCodePos, stepIndex);
        do {
            int stepType = compiler.getOp(stepOpCodePos);
            if (-1 == stepType) {
                break;
            }
            AxesWalker walker = createDefaultWalker(compiler, stepOpCodePos, lpi, analysis);
            walker.init(compiler, stepOpCodePos, stepType);
            walker.exprSetParent(lpi);
            if (firstWalker == null) {
                firstWalker = walker;
            } else {
                prevWalker.setNextWalker(walker);
                walker.setPrevWalker(prevWalker);
            }
            prevWalker = walker;
            stepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
        } while (stepOpCodePos >= 0);
        return firstWalker;
    }

    public static boolean isSet(int analysis, int bits) {
        return (analysis & bits) != 0;
    }

    public static void diagnoseIterator(String name, int analysis, Compiler compiler) {
        System.out.println(compiler.toString() + ", " + name + ", " + Integer.toBinaryString(analysis) + ", " + getAnalysisString(analysis));
    }

    public static DTMIterator newDTMIterator(Compiler compiler, int opPos, boolean isTopLevel) throws TransformerException {
        DTMIterator iter;
        int firstStepPos = OpMap.getFirstChildPos(opPos);
        int analysis = analyze(compiler, firstStepPos, 0);
        boolean isOneStep = isOneStep(analysis);
        if (isOneStep && walksSelfOnly(analysis) && isWild(analysis) && !hasPredicate(analysis)) {
            iter = new SelfIteratorNoPredicate(compiler, opPos, analysis);
        } else if (walksChildrenOnly(analysis) && isOneStep) {
            if (isWild(analysis) && !hasPredicate(analysis)) {
                iter = new ChildIterator(compiler, opPos, analysis);
            } else {
                iter = new ChildTestIterator(compiler, opPos, analysis);
            }
        } else if (isOneStep && walksAttributes(analysis)) {
            iter = new AttributeIterator(compiler, opPos, analysis);
        } else if (isOneStep && !walksFilteredList(analysis)) {
            if (!walksNamespaces(analysis) && (walksInDocOrder(analysis) || isSet(analysis, BIT_PARENT))) {
                iter = new OneStepIteratorForward(compiler, opPos, analysis);
            } else {
                iter = new OneStepIterator(compiler, opPos, analysis);
            }
        } else if (isOptimizableForDescendantIterator(compiler, firstStepPos, 0)) {
            iter = new DescendantIterator(compiler, opPos, analysis);
        } else if (isNaturalDocOrder(compiler, firstStepPos, 0, analysis)) {
            iter = new WalkingIterator(compiler, opPos, analysis, true);
        } else {
            iter = new WalkingIteratorSorted(compiler, opPos, analysis, true);
        }
        if (iter instanceof LocPathIterator) {
            ((LocPathIterator) iter).setIsTopLevel(isTopLevel);
        }
        return iter;
    }

    public static int getAxisFromStep(Compiler compiler, int stepOpCodePos) throws TransformerException {
        int stepType = compiler.getOp(stepOpCodePos);
        switch (stepType) {
            case 22:
            case 23:
            case 24:
            case 25:
                return 20;
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            default:
                throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
            case 37:
                return 0;
            case 38:
                return 1;
            case 39:
                return 2;
            case 40:
                return 3;
            case 41:
                return 4;
            case 42:
                return 5;
            case 43:
                return 6;
            case 44:
                return 7;
            case 45:
                return 10;
            case 46:
                return 11;
            case 47:
                return 12;
            case 48:
                return 13;
            case 49:
                return 9;
            case 50:
                return 19;
        }
    }

    public static int getAnalysisBitFromAxes(int axis) {
        switch (axis) {
            case 0:
                return BIT_ANCESTOR;
            case 1:
                return BIT_ANCESTOR_OR_SELF;
            case 2:
                return BIT_ATTRIBUTE;
            case 3:
                return 65536;
            case 4:
                return BIT_DESCENDANT;
            case 5:
            case 14:
                return BIT_DESCENDANT_OR_SELF;
            case 6:
                return BIT_FOLLOWING;
            case 7:
                return BIT_FOLLOWING_SIBLING;
            case 8:
            case 9:
                return BIT_NAMESPACE;
            case 10:
                return BIT_PARENT;
            case 11:
                return BIT_PRECEDING;
            case 12:
                return BIT_PRECEDING_SIBLING;
            case 13:
                return BIT_SELF;
            case 15:
            default:
                return BIT_FILTER;
            case 16:
            case 17:
            case 18:
                return BIT_ANY_DESCENDANT_FROM_ROOT;
            case 19:
                return BIT_ROOT;
            case 20:
                return BIT_FILTER;
        }
    }

    static boolean functionProximateOrContainsProximate(Compiler compiler, int opPos) {
        int endFunc = (compiler.getOp(opPos + 1) + opPos) - 1;
        int opPos2 = OpMap.getFirstChildPos(opPos);
        int funcID = compiler.getOp(opPos2);
        switch (funcID) {
            case 1:
            case 2:
                break;
            default:
                int i = 0;
                int p = opPos2 + 1;
                while (p < endFunc) {
                    int innerExprOpPos = p + 2;
                    compiler.getOp(innerExprOpPos);
                    boolean prox = isProximateInnerExpr(compiler, innerExprOpPos);
                    if (!prox) {
                        p = compiler.getNextOpPos(p);
                        i++;
                    }
                    break;
                }
                break;
        }
        return true;
    }

    static boolean isProximateInnerExpr(Compiler compiler, int opPos) {
        int op = compiler.getOp(opPos);
        int innerExprOpPos = opPos + 2;
        switch (op) {
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                int leftPos = OpMap.getFirstChildPos(op);
                int rightPos = compiler.getNextOpPos(leftPos);
                boolean isProx = isProximateInnerExpr(compiler, leftPos);
                if (isProx) {
                    return true;
                }
                boolean isProx2 = isProximateInnerExpr(compiler, rightPos);
                return isProx2;
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 23:
            case 24:
            default:
                return true;
            case 21:
            case 22:
            case 27:
            case 28:
                break;
            case 25:
                boolean isProx3 = functionProximateOrContainsProximate(compiler, opPos);
                if (isProx3) {
                    return true;
                }
                break;
            case 26:
                if (isProximateInnerExpr(compiler, innerExprOpPos)) {
                    return true;
                }
                break;
        }
    }

    public static boolean mightBeProximate(Compiler compiler, int opPos, int stepType) throws TransformerException {
        switch (stepType) {
            case 22:
            case 23:
            case 24:
            case 25:
                compiler.getArgLength(opPos);
                break;
            default:
                compiler.getArgLengthOfStep(opPos);
                break;
        }
        int predPos = compiler.getFirstPredicateOpPos(opPos);
        int count = 0;
        while (29 == compiler.getOp(predPos)) {
            count++;
            int innerExprOpPos = predPos + 2;
            int predOp = compiler.getOp(innerExprOpPos);
            switch (predOp) {
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    int leftPos = OpMap.getFirstChildPos(innerExprOpPos);
                    int rightPos = compiler.getNextOpPos(leftPos);
                    boolean isProx = isProximateInnerExpr(compiler, leftPos);
                    if (isProx) {
                        return true;
                    }
                    boolean isProx2 = isProximateInnerExpr(compiler, rightPos);
                    if (isProx2) {
                        return true;
                    }
                    break;
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 20:
                case 21:
                case 23:
                case 24:
                case 26:
                default:
                    return true;
                case 19:
                case 27:
                    return true;
                case 22:
                    return true;
                case 25:
                    boolean isProx3 = functionProximateOrContainsProximate(compiler, innerExprOpPos);
                    if (isProx3) {
                        return true;
                    }
                    break;
                case 28:
                    break;
            }
            predPos = compiler.getNextOpPos(predPos);
        }
        return false;
    }

    private static boolean isOptimizableForDescendantIterator(Compiler compiler, int stepOpCodePos, int stepIndex) throws TransformerException {
        int nextStepOpCodePos;
        int stepCount = 0;
        boolean foundDorDS = false;
        boolean foundSelf = false;
        boolean foundDS = false;
        int nodeTestType = OpCodes.NODETYPE_NODE;
        while (true) {
            int stepType = compiler.getOp(stepOpCodePos);
            if (-1 != stepType) {
                if ((nodeTestType != 1033 && nodeTestType != 35) || (stepCount = stepCount + 1) > 3) {
                    return false;
                }
                boolean mightBeProximate = mightBeProximate(compiler, stepOpCodePos, stepType);
                if (mightBeProximate) {
                    return false;
                }
                switch (stepType) {
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 37:
                    case 38:
                    case 39:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 49:
                    case 51:
                    case 52:
                    case 53:
                        return false;
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    default:
                        throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
                    case 40:
                        if (!foundDS && (!foundDorDS || !foundSelf)) {
                            return false;
                        }
                        nodeTestType = compiler.getStepTestType(stepOpCodePos);
                        nextStepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
                        if (nextStepOpCodePos < 0) {
                            if (-1 != compiler.getOp(nextStepOpCodePos) && compiler.countPredicates(stepOpCodePos) > 0) {
                                return false;
                            }
                            stepOpCodePos = nextStepOpCodePos;
                        }
                        break;
                    case 42:
                        foundDS = true;
                    case 41:
                        if (3 == stepCount) {
                            return false;
                        }
                        foundDorDS = true;
                        nodeTestType = compiler.getStepTestType(stepOpCodePos);
                        nextStepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
                        if (nextStepOpCodePos < 0) {
                        }
                        break;
                    case 48:
                        if (1 != stepCount) {
                            return false;
                        }
                        foundSelf = true;
                        nodeTestType = compiler.getStepTestType(stepOpCodePos);
                        nextStepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
                        if (nextStepOpCodePos < 0) {
                        }
                        break;
                    case 50:
                        if (1 != stepCount) {
                            return false;
                        }
                        nodeTestType = compiler.getStepTestType(stepOpCodePos);
                        nextStepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
                        if (nextStepOpCodePos < 0) {
                        }
                        break;
                }
            }
        }
    }

    private static int analyze(Compiler compiler, int stepOpCodePos, int stepIndex) throws TransformerException {
        int stepCount = 0;
        int analysisResult = 0;
        do {
            int stepType = compiler.getOp(stepOpCodePos);
            if (-1 != stepType) {
                stepCount++;
                boolean predAnalysis = analyzePredicate(compiler, stepOpCodePos, stepType);
                if (predAnalysis) {
                    analysisResult |= 4096;
                }
                switch (stepType) {
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                        analysisResult |= BIT_FILTER;
                        break;
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    default:
                        throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
                    case 37:
                        analysisResult |= BIT_ANCESTOR;
                        break;
                    case 38:
                        analysisResult |= BIT_ANCESTOR_OR_SELF;
                        break;
                    case 39:
                        analysisResult |= BIT_ATTRIBUTE;
                        break;
                    case 40:
                        analysisResult |= 65536;
                        break;
                    case 41:
                        analysisResult |= BIT_DESCENDANT;
                        break;
                    case 42:
                        if (2 == stepCount && 134217728 == analysisResult) {
                            analysisResult |= BIT_ANY_DESCENDANT_FROM_ROOT;
                        }
                        analysisResult |= BIT_DESCENDANT_OR_SELF;
                        break;
                    case 43:
                        analysisResult |= BIT_FOLLOWING;
                        break;
                    case 44:
                        analysisResult |= BIT_FOLLOWING_SIBLING;
                        break;
                    case 45:
                        analysisResult |= BIT_PARENT;
                        break;
                    case 46:
                        analysisResult |= BIT_PRECEDING;
                        break;
                    case 47:
                        analysisResult |= BIT_PRECEDING_SIBLING;
                        break;
                    case 48:
                        analysisResult |= BIT_SELF;
                        break;
                    case 49:
                        analysisResult |= BIT_NAMESPACE;
                        break;
                    case 50:
                        analysisResult |= BIT_ROOT;
                        break;
                    case 51:
                        analysisResult |= -2147450880;
                        break;
                    case 52:
                        analysisResult |= -2147475456;
                        break;
                    case 53:
                        analysisResult |= -2143289344;
                        break;
                }
                if (1033 == compiler.getOp(stepOpCodePos + 3)) {
                    analysisResult |= BIT_NODETEST_ANY;
                }
                stepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
            }
            return analysisResult | (stepCount & BITS_COUNT);
        } while (stepOpCodePos >= 0);
        return analysisResult | (stepCount & BITS_COUNT);
    }

    public static boolean isDownwardAxisOfMany(int axis) {
        return 5 == axis || 4 == axis || 6 == axis || 11 == axis;
    }

    static StepPattern loadSteps(MatchPatternIterator mpi, Compiler compiler, int stepOpCodePos, int stepIndex) throws TransformerException {
        StepPattern step = null;
        StepPattern firstStep = null;
        StepPattern prevStep = null;
        int analysis = analyze(compiler, stepOpCodePos, stepIndex);
        while (-1 != stepType) {
            step = createDefaultStepPattern(compiler, stepOpCodePos, mpi, analysis, firstStep, prevStep);
            if (firstStep == null) {
                firstStep = step;
            } else {
                step.setRelativePathPattern(prevStep);
            }
            prevStep = step;
            stepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
            if (stepOpCodePos < 0) {
                break;
            }
        }
        int axis = 13;
        StepPattern tail = step;
        StepPattern pat = step;
        while (pat != null) {
            int nextAxis = pat.getAxis();
            pat.setAxis(axis);
            int whatToShow = pat.getWhatToShow();
            if (whatToShow == 2 || whatToShow == 4096) {
                int newAxis = whatToShow == 2 ? 2 : 9;
                if (isDownwardAxisOfMany(axis)) {
                    StepPattern attrPat = new StepPattern(whatToShow, pat.getNamespace(), pat.getLocalName(), newAxis, 0);
                    XNumber score = pat.getStaticScore();
                    pat.setNamespace(null);
                    pat.setLocalName("*");
                    attrPat.setPredicates(pat.getPredicates());
                    pat.setPredicates(null);
                    pat.setWhatToShow(1);
                    StepPattern rel = pat.getRelativePathPattern();
                    pat.setRelativePathPattern(attrPat);
                    attrPat.setRelativePathPattern(rel);
                    attrPat.setStaticScore(score);
                    if (11 == pat.getAxis()) {
                        pat.setAxis(15);
                    } else if (4 == pat.getAxis()) {
                        pat.setAxis(5);
                    }
                    pat = attrPat;
                } else if (3 == pat.getAxis()) {
                    pat.setAxis(2);
                }
            }
            axis = nextAxis;
            tail = pat;
            pat = pat.getRelativePathPattern();
        }
        if (axis < 16) {
            StepPattern selfPattern = new ContextMatchStepPattern(axis, 13);
            XNumber score2 = tail.getStaticScore();
            tail.setRelativePathPattern(selfPattern);
            tail.setStaticScore(score2);
            selfPattern.setStaticScore(score2);
        }
        return step;
    }

    private static StepPattern createDefaultStepPattern(Compiler compiler, int opPos, MatchPatternIterator mpi, int analysis, StepPattern tail, StepPattern head) throws TransformerException {
        int axis;
        int predicateAxis;
        Expression expr;
        int stepType = compiler.getOp(opPos);
        compiler.getWhatToShow(opPos);
        StepPattern ai = null;
        switch (stepType) {
            case 22:
            case 23:
            case 24:
            case 25:
                switch (stepType) {
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                        expr = compiler.compile(opPos);
                        break;
                    default:
                        expr = compiler.compile(opPos + 2);
                        break;
                }
                axis = 20;
                predicateAxis = 20;
                ai = new FunctionPattern(expr, 20, 20);
                break;
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            default:
                throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
            case 37:
                axis = 4;
                predicateAxis = 0;
                break;
            case 38:
                axis = 5;
                predicateAxis = 1;
                break;
            case 39:
                axis = 10;
                predicateAxis = 2;
                break;
            case 40:
                axis = 10;
                predicateAxis = 3;
                break;
            case 41:
                axis = 0;
                predicateAxis = 4;
                break;
            case 42:
                axis = 1;
                predicateAxis = 5;
                break;
            case 43:
                axis = 11;
                predicateAxis = 6;
                break;
            case 44:
                axis = 12;
                predicateAxis = 7;
                break;
            case 45:
                axis = 3;
                predicateAxis = 10;
                break;
            case 46:
                axis = 6;
                predicateAxis = 11;
                break;
            case 47:
                axis = 7;
                predicateAxis = 12;
                break;
            case 48:
                axis = 13;
                predicateAxis = 13;
                break;
            case 49:
                axis = 10;
                predicateAxis = 9;
                break;
            case 50:
                axis = 19;
                predicateAxis = 19;
                ai = new StepPattern(1280, 19, 19);
                break;
        }
        if (ai == null) {
            int whatToShow = compiler.getWhatToShow(opPos);
            ai = new StepPattern(whatToShow, compiler.getStepNS(opPos), compiler.getStepLocalName(opPos), axis, predicateAxis);
        }
        int argLen = compiler.getFirstPredicateOpPos(opPos);
        ai.setPredicates(compiler.getCompiledPredicates(argLen));
        return ai;
    }

    static boolean analyzePredicate(Compiler compiler, int opPos, int stepType) throws TransformerException {
        switch (stepType) {
            case 22:
            case 23:
            case 24:
            case 25:
                compiler.getArgLength(opPos);
                break;
            default:
                compiler.getArgLengthOfStep(opPos);
                break;
        }
        int pos = compiler.getFirstPredicateOpPos(opPos);
        int nPredicates = compiler.countPredicates(pos);
        return nPredicates > 0;
    }

    private static AxesWalker createDefaultWalker(Compiler compiler, int opPos, WalkingIterator lpi, int analysis) {
        AxesWalker ai;
        int stepType = compiler.getOp(opPos);
        boolean simpleInit = false;
        int i = analysis & BITS_COUNT;
        switch (stepType) {
            case 22:
            case 23:
            case 24:
            case 25:
                ai = new FilterExprWalker(lpi);
                simpleInit = true;
                break;
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            default:
                throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
            case 37:
                ai = new ReverseAxesWalker(lpi, 0);
                break;
            case 38:
                ai = new ReverseAxesWalker(lpi, 1);
                break;
            case 39:
                ai = new AxesWalker(lpi, 2);
                break;
            case 40:
                ai = new AxesWalker(lpi, 3);
                break;
            case 41:
                ai = new AxesWalker(lpi, 4);
                break;
            case 42:
                ai = new AxesWalker(lpi, 5);
                break;
            case 43:
                ai = new AxesWalker(lpi, 6);
                break;
            case 44:
                ai = new AxesWalker(lpi, 7);
                break;
            case 45:
                ai = new ReverseAxesWalker(lpi, 10);
                break;
            case 46:
                ai = new ReverseAxesWalker(lpi, 11);
                break;
            case 47:
                ai = new ReverseAxesWalker(lpi, 12);
                break;
            case 48:
                ai = new AxesWalker(lpi, 13);
                break;
            case 49:
                ai = new AxesWalker(lpi, 9);
                break;
            case 50:
                ai = new AxesWalker(lpi, 19);
                break;
        }
        if (simpleInit) {
            ai.initNodeTest(-1);
        } else {
            int whatToShow = compiler.getWhatToShow(opPos);
            if ((whatToShow & 4163) == 0 || whatToShow == -1) {
                ai.initNodeTest(whatToShow);
            } else {
                ai.initNodeTest(whatToShow, compiler.getStepNS(opPos), compiler.getStepLocalName(opPos));
            }
        }
        return ai;
    }

    public static String getAnalysisString(int analysis) {
        StringBuffer buf = new StringBuffer();
        buf.append("count: " + getStepCount(analysis) + " ");
        if ((1073741824 & analysis) != 0) {
            buf.append("NTANY|");
        }
        if ((analysis & 4096) != 0) {
            buf.append("PRED|");
        }
        if ((analysis & BIT_ANCESTOR) != 0) {
            buf.append("ANC|");
        }
        if ((analysis & BIT_ANCESTOR_OR_SELF) != 0) {
            buf.append("ANCOS|");
        }
        if ((32768 & analysis) != 0) {
            buf.append("ATTR|");
        }
        if ((65536 & analysis) != 0) {
            buf.append("CH|");
        }
        if ((131072 & analysis) != 0) {
            buf.append("DESC|");
        }
        if ((262144 & analysis) != 0) {
            buf.append("DESCOS|");
        }
        if ((524288 & analysis) != 0) {
            buf.append("FOL|");
        }
        if ((1048576 & analysis) != 0) {
            buf.append("FOLS|");
        }
        if ((2097152 & analysis) != 0) {
            buf.append("NS|");
        }
        if ((4194304 & analysis) != 0) {
            buf.append("P|");
        }
        if ((8388608 & analysis) != 0) {
            buf.append("PREC|");
        }
        if ((16777216 & analysis) != 0) {
            buf.append("PRECS|");
        }
        if ((33554432 & analysis) != 0) {
            buf.append(".|");
        }
        if ((67108864 & analysis) != 0) {
            buf.append("FLT|");
        }
        if ((134217728 & analysis) != 0) {
            buf.append("R|");
        }
        return buf.toString();
    }

    public static boolean hasPredicate(int analysis) {
        return (analysis & 4096) != 0;
    }

    public static boolean isWild(int analysis) {
        return (1073741824 & analysis) != 0;
    }

    public static boolean walksAncestors(int analysis) {
        return isSet(analysis, 24576);
    }

    public static boolean walksAttributes(int analysis) {
        return (32768 & analysis) != 0;
    }

    public static boolean walksNamespaces(int analysis) {
        return (2097152 & analysis) != 0;
    }

    public static boolean walksChildren(int analysis) {
        return (65536 & analysis) != 0;
    }

    public static boolean walksDescendants(int analysis) {
        return isSet(analysis, 393216);
    }

    public static boolean walksSubtree(int analysis) {
        return isSet(analysis, 458752);
    }

    public static boolean walksSubtreeOnlyMaybeAbsolute(int analysis) {
        return (!walksSubtree(analysis) || walksExtraNodes(analysis) || walksUp(analysis) || walksSideways(analysis)) ? false : true;
    }

    public static boolean walksSubtreeOnly(int analysis) {
        return walksSubtreeOnlyMaybeAbsolute(analysis) && !isAbsolute(analysis);
    }

    public static boolean walksFilteredList(int analysis) {
        return isSet(analysis, BIT_FILTER);
    }

    public static boolean walksSubtreeOnlyFromRootOrContext(int analysis) {
        return (!walksSubtree(analysis) || walksExtraNodes(analysis) || walksUp(analysis) || walksSideways(analysis) || isSet(analysis, BIT_FILTER)) ? false : true;
    }

    public static boolean walksInDocOrder(int analysis) {
        return (walksSubtreeOnlyMaybeAbsolute(analysis) || walksExtraNodesOnly(analysis) || walksFollowingOnlyMaybeAbsolute(analysis)) && !isSet(analysis, BIT_FILTER);
    }

    public static boolean walksFollowingOnlyMaybeAbsolute(int analysis) {
        return (!isSet(analysis, 35127296) || walksSubtree(analysis) || walksUp(analysis) || walksSideways(analysis)) ? false : true;
    }

    public static boolean walksUp(int analysis) {
        return isSet(analysis, 4218880);
    }

    public static boolean walksSideways(int analysis) {
        return isSet(analysis, 26738688);
    }

    public static boolean walksExtraNodes(int analysis) {
        return isSet(analysis, 2129920);
    }

    public static boolean walksExtraNodesOnly(int analysis) {
        return (!walksExtraNodes(analysis) || isSet(analysis, BIT_SELF) || walksSubtree(analysis) || walksUp(analysis) || walksSideways(analysis) || isAbsolute(analysis)) ? false : true;
    }

    public static boolean isAbsolute(int analysis) {
        return isSet(analysis, 201326592);
    }

    public static boolean walksChildrenOnly(int analysis) {
        return (!walksChildren(analysis) || isSet(analysis, BIT_SELF) || walksExtraNodes(analysis) || walksDescendants(analysis) || walksUp(analysis) || walksSideways(analysis) || (isAbsolute(analysis) && !isSet(analysis, BIT_ROOT))) ? false : true;
    }

    public static boolean walksChildrenAndExtraAndSelfOnly(int analysis) {
        return (!walksChildren(analysis) || walksDescendants(analysis) || walksUp(analysis) || walksSideways(analysis) || (isAbsolute(analysis) && !isSet(analysis, BIT_ROOT))) ? false : true;
    }

    public static boolean walksDescendantsAndExtraAndSelfOnly(int analysis) {
        return (walksChildren(analysis) || !walksDescendants(analysis) || walksUp(analysis) || walksSideways(analysis) || (isAbsolute(analysis) && !isSet(analysis, BIT_ROOT))) ? false : true;
    }

    public static boolean walksSelfOnly(int analysis) {
        return (!isSet(analysis, BIT_SELF) || walksSubtree(analysis) || walksUp(analysis) || walksSideways(analysis) || isAbsolute(analysis)) ? false : true;
    }

    public static boolean walksUpOnly(int analysis) {
        return (walksSubtree(analysis) || !walksUp(analysis) || walksSideways(analysis) || isAbsolute(analysis)) ? false : true;
    }

    public static boolean walksDownOnly(int analysis) {
        return (!walksSubtree(analysis) || walksUp(analysis) || walksSideways(analysis) || isAbsolute(analysis)) ? false : true;
    }

    public static boolean walksDownExtraOnly(int analysis) {
        return (!walksSubtree(analysis) || !walksExtraNodes(analysis) || walksUp(analysis) || walksSideways(analysis) || isAbsolute(analysis)) ? false : true;
    }

    public static boolean canSkipSubtrees(int analysis) {
        return isSet(analysis, 65536) | walksSideways(analysis);
    }

    public static boolean canCrissCross(int analysis) {
        if (walksSelfOnly(analysis)) {
            return false;
        }
        if ((walksDownOnly(analysis) && !canSkipSubtrees(analysis)) || walksChildrenAndExtraAndSelfOnly(analysis) || walksDescendantsAndExtraAndSelfOnly(analysis) || walksUpOnly(analysis) || walksExtraNodesOnly(analysis) || !walksSubtree(analysis)) {
            return false;
        }
        return walksSideways(analysis) || walksUp(analysis) || canSkipSubtrees(analysis);
    }

    public static boolean isNaturalDocOrder(int analysis) {
        return (canCrissCross(analysis) || isSet(analysis, BIT_NAMESPACE) || walksFilteredList(analysis) || !walksInDocOrder(analysis)) ? false : true;
    }

    private static boolean isNaturalDocOrder(Compiler compiler, int stepOpCodePos, int stepIndex, int analysis) throws TransformerException {
        int nextStepOpCodePos;
        if (canCrissCross(analysis) || isSet(analysis, BIT_NAMESPACE)) {
            return false;
        }
        if (isSet(analysis, 1572864) && isSet(analysis, 25165824)) {
            return false;
        }
        int stepCount = 0;
        boolean foundWildAttribute = false;
        int potentialDuplicateMakingStepCount = 0;
        while (true) {
            int stepType = compiler.getOp(stepOpCodePos);
            if (-1 != stepType) {
                stepCount++;
                switch (stepType) {
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 37:
                    case 38:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 49:
                    case 52:
                    case 53:
                        if (potentialDuplicateMakingStepCount > 0) {
                            return false;
                        }
                        potentialDuplicateMakingStepCount++;
                        if (foundWildAttribute) {
                            return false;
                        }
                        nextStepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
                        if (nextStepOpCodePos >= 0) {
                            stepOpCodePos = nextStepOpCodePos;
                        }
                        break;
                        break;
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    default:
                        throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(stepType)}));
                    case 39:
                    case 51:
                        if (foundWildAttribute) {
                            return false;
                        }
                        String localName = compiler.getStepLocalName(stepOpCodePos);
                        if (localName.equals("*")) {
                            foundWildAttribute = true;
                        }
                        nextStepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
                        if (nextStepOpCodePos >= 0) {
                        }
                        break;
                    case 40:
                    case 48:
                    case 50:
                        if (foundWildAttribute) {
                        }
                        nextStepOpCodePos = compiler.getNextStepPos(stepOpCodePos);
                        if (nextStepOpCodePos >= 0) {
                        }
                        break;
                }
            }
        }
    }

    public static boolean isOneStep(int analysis) {
        return (analysis & BITS_COUNT) == 1;
    }

    public static int getStepCount(int analysis) {
        return analysis & BITS_COUNT;
    }
}
