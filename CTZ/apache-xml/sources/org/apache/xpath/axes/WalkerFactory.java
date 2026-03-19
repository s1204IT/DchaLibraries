package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xpath.Expression;
import org.apache.xpath.compiler.Compiler;
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

    static AxesWalker loadOneWalker(WalkingIterator walkingIterator, Compiler compiler, int i) throws TransformerException {
        int op = compiler.getOp(i);
        if (op != -1) {
            AxesWalker axesWalkerCreateDefaultWalker = createDefaultWalker(compiler, op, walkingIterator, 0);
            axesWalkerCreateDefaultWalker.init(compiler, i, op);
            return axesWalkerCreateDefaultWalker;
        }
        return null;
    }

    static AxesWalker loadWalkers(WalkingIterator walkingIterator, Compiler compiler, int i, int i2) throws TransformerException {
        int iAnalyze = analyze(compiler, i, i2);
        AxesWalker axesWalker = null;
        AxesWalker axesWalker2 = null;
        while (true) {
            int op = compiler.getOp(i);
            if (-1 == op) {
                break;
            }
            AxesWalker axesWalkerCreateDefaultWalker = createDefaultWalker(compiler, i, walkingIterator, iAnalyze);
            axesWalkerCreateDefaultWalker.init(compiler, i, op);
            axesWalkerCreateDefaultWalker.exprSetParent(walkingIterator);
            if (axesWalker != null) {
                axesWalker2.setNextWalker(axesWalkerCreateDefaultWalker);
                axesWalkerCreateDefaultWalker.setPrevWalker(axesWalker2);
            } else {
                axesWalker = axesWalkerCreateDefaultWalker;
            }
            i = compiler.getNextStepPos(i);
            if (i < 0) {
                break;
            }
            axesWalker2 = axesWalkerCreateDefaultWalker;
        }
        return axesWalker;
    }

    public static boolean isSet(int i, int i2) {
        return (i & i2) != 0;
    }

    public static void diagnoseIterator(String str, int i, Compiler compiler) {
        System.out.println(compiler.toString() + ", " + str + ", " + Integer.toBinaryString(i) + ", " + getAnalysisString(i));
    }

    public static DTMIterator newDTMIterator(Compiler compiler, int i, boolean z) throws TransformerException {
        LocPathIterator walkingIteratorSorted;
        int firstChildPos = OpMap.getFirstChildPos(i);
        int iAnalyze = analyze(compiler, firstChildPos, 0);
        boolean zIsOneStep = isOneStep(iAnalyze);
        if (zIsOneStep && walksSelfOnly(iAnalyze) && isWild(iAnalyze) && !hasPredicate(iAnalyze)) {
            walkingIteratorSorted = new SelfIteratorNoPredicate(compiler, i, iAnalyze);
        } else if (walksChildrenOnly(iAnalyze) && zIsOneStep) {
            if (isWild(iAnalyze) && !hasPredicate(iAnalyze)) {
                walkingIteratorSorted = new ChildIterator(compiler, i, iAnalyze);
            } else {
                walkingIteratorSorted = new ChildTestIterator(compiler, i, iAnalyze);
            }
        } else if (zIsOneStep && walksAttributes(iAnalyze)) {
            walkingIteratorSorted = new AttributeIterator(compiler, i, iAnalyze);
        } else if (zIsOneStep && !walksFilteredList(iAnalyze)) {
            if (!walksNamespaces(iAnalyze) && (walksInDocOrder(iAnalyze) || isSet(iAnalyze, BIT_PARENT))) {
                walkingIteratorSorted = new OneStepIteratorForward(compiler, i, iAnalyze);
            } else {
                walkingIteratorSorted = new OneStepIterator(compiler, i, iAnalyze);
            }
        } else if (isOptimizableForDescendantIterator(compiler, firstChildPos, 0)) {
            walkingIteratorSorted = new DescendantIterator(compiler, i, iAnalyze);
        } else if (isNaturalDocOrder(compiler, firstChildPos, 0, iAnalyze)) {
            walkingIteratorSorted = new WalkingIterator(compiler, i, iAnalyze, true);
        } else {
            walkingIteratorSorted = new WalkingIteratorSorted(compiler, i, iAnalyze, true);
        }
        if (walkingIteratorSorted instanceof LocPathIterator) {
            walkingIteratorSorted.setIsTopLevel(z);
        }
        return walkingIteratorSorted;
    }

    public static int getAxisFromStep(Compiler compiler, int i) throws TransformerException {
        int op = compiler.getOp(i);
        switch (op) {
            case 22:
            case 23:
            case 24:
            case 25:
                return 20;
            default:
                switch (op) {
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
                    default:
                        throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(op)}));
                }
        }
    }

    public static int getAnalysisBitFromAxes(int i) {
        switch (i) {
        }
        return BIT_FILTER;
    }

    static boolean functionProximateOrContainsProximate(Compiler compiler, int i) {
        int op = (compiler.getOp(i + 1) + i) - 1;
        int firstChildPos = OpMap.getFirstChildPos(i);
        switch (compiler.getOp(firstChildPos)) {
            case 1:
            case 2:
                break;
            default:
                int nextOpPos = firstChildPos + 1;
                while (nextOpPos < op) {
                    int i2 = nextOpPos + 2;
                    compiler.getOp(i2);
                    if (!isProximateInnerExpr(compiler, i2)) {
                        nextOpPos = compiler.getNextOpPos(nextOpPos);
                    }
                    break;
                }
                break;
        }
        return true;
    }

    static boolean isProximateInnerExpr(Compiler compiler, int i) {
        int op = compiler.getOp(i);
        int i2 = i + 2;
        switch (op) {
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                int firstChildPos = OpMap.getFirstChildPos(op);
                int nextOpPos = compiler.getNextOpPos(firstChildPos);
                if (!isProximateInnerExpr(compiler, firstChildPos) && !isProximateInnerExpr(compiler, nextOpPos)) {
                }
                break;
            default:
                switch (op) {
                    case 21:
                    case 22:
                        break;
                    default:
                        switch (op) {
                            case 25:
                                if (functionProximateOrContainsProximate(compiler, i)) {
                                }
                                break;
                            case 26:
                                if (isProximateInnerExpr(compiler, i2)) {
                                }
                                break;
                        }
                        break;
                }
                break;
        }
        return true;
    }

    public static boolean mightBeProximate(org.apache.xpath.compiler.Compiler r3, int r4, int r5) throws javax.xml.transform.TransformerException {
        switch (r5) {
            case 22:
            case 23:
            case 24:
            case 25:
                r3.getArgLength(r4);
                break;
            default:
                r3.getArgLengthOfStep(r4);
                break;
        }
        r4 = r3.getFirstPredicateOpPos(r4);
        while (29 == r3.getOp(r4)) {
            r5 = r4 + 2;
            r0 = r3.getOp(r5);
            if (r0 == 19 || r0 == 22) {
                return true;
            } else {
                if (r0 != 25) {
                    switch (r0) {
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                            r5 = org.apache.xpath.compiler.OpMap.getFirstChildPos(r5);
                            r0 = r3.getNextOpPos(r5);
                            if (!isProximateInnerExpr(r3, r5) && !isProximateInnerExpr(r3, r0)) {
                                continue;
                                break;
                            } else {
                                return true;
                            }
                            break;
                        default:
                            switch (r0) {
                            }
                            return true;
                    }
                } else {
                    if (functionProximateOrContainsProximate(r3, r5)) {
                        return true;
                    }
                }
                r4 = r3.getNextOpPos(r4);
            }
        }
        return false;
    }

    private static boolean isOptimizableForDescendantIterator(Compiler compiler, int i, int i2) throws TransformerException {
        int nextStepPos;
        int stepTestType = 1033;
        int i3 = 0;
        boolean z = false;
        boolean z2 = false;
        boolean z3 = false;
        while (true) {
            int op = compiler.getOp(i);
            if (-1 != op) {
                if ((stepTestType != 1033 && stepTestType != 35) || (i3 = i3 + 1) > 3 || mightBeProximate(compiler, i, op)) {
                    return false;
                }
                switch (op) {
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                        break;
                    default:
                        switch (op) {
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
                                break;
                            case 40:
                                if (!z && (!z2 || !z3)) {
                                }
                                stepTestType = compiler.getStepTestType(i);
                                nextStepPos = compiler.getNextStepPos(i);
                                if (nextStepPos < 0) {
                                    if (-1 != compiler.getOp(nextStepPos) && compiler.countPredicates(i) > 0) {
                                        return false;
                                    }
                                    i = nextStepPos;
                                }
                                break;
                            case 42:
                                z = true;
                            case 41:
                                if (3 == i3) {
                                    return false;
                                }
                                z2 = true;
                                stepTestType = compiler.getStepTestType(i);
                                nextStepPos = compiler.getNextStepPos(i);
                                if (nextStepPos < 0) {
                                }
                                break;
                            case 48:
                                if (1 != i3) {
                                    return false;
                                }
                                z3 = true;
                                stepTestType = compiler.getStepTestType(i);
                                nextStepPos = compiler.getNextStepPos(i);
                                if (nextStepPos < 0) {
                                }
                                break;
                            case 50:
                                if (1 != i3) {
                                    return false;
                                }
                                stepTestType = compiler.getStepTestType(i);
                                nextStepPos = compiler.getNextStepPos(i);
                                if (nextStepPos < 0) {
                                }
                                break;
                            default:
                                throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(op)}));
                        }
                        break;
                }
            }
        }
    }

    private static int analyze(Compiler compiler, int i, int i2) throws TransformerException {
        int i3 = 0;
        int i4 = 0;
        do {
            int op = compiler.getOp(i);
            if (-1 != op) {
                i3++;
                if (analyzePredicate(compiler, i, op)) {
                    i4 |= 4096;
                }
                switch (op) {
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                        i4 |= BIT_FILTER;
                        break;
                    default:
                        switch (op) {
                            case 37:
                                i4 |= BIT_ANCESTOR;
                                break;
                            case 38:
                                i4 |= BIT_ANCESTOR_OR_SELF;
                                break;
                            case 39:
                                i4 |= BIT_ATTRIBUTE;
                                break;
                            case 40:
                                i4 |= 65536;
                                break;
                            case 41:
                                i4 |= BIT_DESCENDANT;
                                break;
                            case 42:
                                if (2 == i3 && 134217728 == i4) {
                                    i4 |= BIT_ANY_DESCENDANT_FROM_ROOT;
                                }
                                i4 |= BIT_DESCENDANT_OR_SELF;
                                break;
                            case 43:
                                i4 |= BIT_FOLLOWING;
                                break;
                            case 44:
                                i4 |= BIT_FOLLOWING_SIBLING;
                                break;
                            case 45:
                                i4 |= BIT_PARENT;
                                break;
                            case 46:
                                i4 |= BIT_PRECEDING;
                                break;
                            case 47:
                                i4 |= BIT_PRECEDING_SIBLING;
                                break;
                            case 48:
                                i4 |= BIT_SELF;
                                break;
                            case 49:
                                i4 |= BIT_NAMESPACE;
                                break;
                            case 50:
                                i4 |= BIT_ROOT;
                                break;
                            case 51:
                                i4 |= -2147450880;
                                break;
                            case 52:
                                i4 |= -2147475456;
                                break;
                            case 53:
                                i4 |= -2143289344;
                                break;
                            default:
                                throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(op)}));
                        }
                        break;
                }
                if (1033 == compiler.getOp(i + 3)) {
                    i4 |= BIT_NODETEST_ANY;
                }
                i = compiler.getNextStepPos(i);
            }
            return (i3 & BITS_COUNT) | i4;
        } while (i >= 0);
        return (i3 & BITS_COUNT) | i4;
    }

    public static boolean isDownwardAxisOfMany(int i) {
        return 5 == i || 4 == i || 6 == i || 11 == i;
    }

    static StepPattern loadSteps(MatchPatternIterator matchPatternIterator, Compiler compiler, int i, int i2) throws TransformerException {
        int iAnalyze = analyze(compiler, i, i2);
        int nextStepPos = i;
        StepPattern stepPatternCreateDefaultStepPattern = null;
        StepPattern stepPattern = null;
        StepPattern stepPattern2 = null;
        while (-1 != compiler.getOp(nextStepPos)) {
            stepPatternCreateDefaultStepPattern = createDefaultStepPattern(compiler, nextStepPos, matchPatternIterator, iAnalyze, stepPattern, stepPattern2);
            if (stepPattern != null) {
                stepPatternCreateDefaultStepPattern.setRelativePathPattern(stepPattern2);
            } else {
                stepPattern = stepPatternCreateDefaultStepPattern;
            }
            nextStepPos = compiler.getNextStepPos(nextStepPos);
            if (nextStepPos < 0) {
                break;
            }
            stepPattern2 = stepPatternCreateDefaultStepPattern;
        }
        StepPattern relativePathPattern = stepPatternCreateDefaultStepPattern;
        StepPattern stepPattern3 = relativePathPattern;
        int i3 = 13;
        while (relativePathPattern != null) {
            int axis = relativePathPattern.getAxis();
            relativePathPattern.setAxis(i3);
            int whatToShow = relativePathPattern.getWhatToShow();
            if (whatToShow == 2 || whatToShow == 4096) {
                int i4 = whatToShow == 2 ? 2 : 9;
                if (isDownwardAxisOfMany(i3)) {
                    StepPattern stepPattern4 = new StepPattern(whatToShow, relativePathPattern.getNamespace(), relativePathPattern.getLocalName(), i4, 0);
                    XNumber staticScore = relativePathPattern.getStaticScore();
                    relativePathPattern.setNamespace(null);
                    relativePathPattern.setLocalName("*");
                    stepPattern4.setPredicates(relativePathPattern.getPredicates());
                    relativePathPattern.setPredicates(null);
                    relativePathPattern.setWhatToShow(1);
                    StepPattern relativePathPattern2 = relativePathPattern.getRelativePathPattern();
                    relativePathPattern.setRelativePathPattern(stepPattern4);
                    stepPattern4.setRelativePathPattern(relativePathPattern2);
                    stepPattern4.setStaticScore(staticScore);
                    if (11 == relativePathPattern.getAxis()) {
                        relativePathPattern.setAxis(15);
                    } else if (4 == relativePathPattern.getAxis()) {
                        relativePathPattern.setAxis(5);
                    }
                    relativePathPattern = stepPattern4;
                } else if (3 == relativePathPattern.getAxis()) {
                    relativePathPattern.setAxis(2);
                }
            }
            stepPattern3 = relativePathPattern;
            relativePathPattern = relativePathPattern.getRelativePathPattern();
            i3 = axis;
        }
        if (i3 < 16) {
            ContextMatchStepPattern contextMatchStepPattern = new ContextMatchStepPattern(i3, 13);
            XNumber staticScore2 = stepPattern3.getStaticScore();
            stepPattern3.setRelativePathPattern(contextMatchStepPattern);
            stepPattern3.setStaticScore(staticScore2);
            contextMatchStepPattern.setStaticScore(staticScore2);
        }
        return stepPatternCreateDefaultStepPattern;
    }

    private static StepPattern createDefaultStepPattern(Compiler compiler, int i, MatchPatternIterator matchPatternIterator, int i2, StepPattern stepPattern, StepPattern stepPattern2) throws TransformerException {
        Expression expressionCompile;
        int i3;
        int op = compiler.getOp(i);
        compiler.getWhatToShow(i);
        int i4 = 11;
        int i5 = 6;
        StepPattern functionPattern = null;
        switch (op) {
            case 22:
            case 23:
            case 24:
            case 25:
                switch (op) {
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                        expressionCompile = compiler.compile(i);
                        break;
                    default:
                        expressionCompile = compiler.compile(i + 2);
                        break;
                }
                functionPattern = new FunctionPattern(expressionCompile, 20, 20);
                i4 = 20;
                i5 = i4;
                StepPattern stepPattern3 = functionPattern == null ? new StepPattern(compiler.getWhatToShow(i), compiler.getStepNS(i), compiler.getStepLocalName(i), i4, i5) : functionPattern;
                stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                return stepPattern3;
            default:
                switch (op) {
                    case 37:
                        i4 = 4;
                        i5 = 0;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 38:
                        i4 = 5;
                        i5 = 1;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 39:
                        i3 = 2;
                        i5 = i3;
                        i4 = 10;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 40:
                        i5 = 3;
                        i4 = 10;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 41:
                        i5 = 4;
                        i4 = 0;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 42:
                        i5 = 5;
                        i4 = 1;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 43:
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 44:
                        i4 = 12;
                        i5 = 7;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 45:
                        i4 = 3;
                        i5 = 10;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 46:
                        i5 = 11;
                        i4 = 6;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 47:
                        i5 = 12;
                        i4 = 7;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 48:
                        i4 = 13;
                        i5 = i4;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 49:
                        i3 = 9;
                        i5 = i3;
                        i4 = 10;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    case 50:
                        functionPattern = new StepPattern(1280, 19, 19);
                        i4 = 19;
                        i5 = i4;
                        if (functionPattern == null) {
                        }
                        stepPattern3.setPredicates(compiler.getCompiledPredicates(compiler.getFirstPredicateOpPos(i)));
                        return stepPattern3;
                    default:
                        throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(op)}));
                }
        }
    }

    static boolean analyzePredicate(Compiler compiler, int i, int i2) throws TransformerException {
        switch (i2) {
            case 22:
            case 23:
            case 24:
            case 25:
                compiler.getArgLength(i);
                break;
            default:
                compiler.getArgLengthOfStep(i);
                break;
        }
        return compiler.countPredicates(compiler.getFirstPredicateOpPos(i)) > 0;
    }

    private static AxesWalker createDefaultWalker(Compiler compiler, int i, WalkingIterator walkingIterator, int i2) {
        AxesWalker filterExprWalker;
        int op = compiler.getOp(i);
        boolean z = false;
        switch (op) {
            case 22:
            case 23:
            case 24:
            case 25:
                filterExprWalker = new FilterExprWalker(walkingIterator);
                z = true;
                break;
            default:
                switch (op) {
                    case 37:
                        filterExprWalker = new ReverseAxesWalker(walkingIterator, 0);
                        break;
                    case 38:
                        filterExprWalker = new ReverseAxesWalker(walkingIterator, 1);
                        break;
                    case 39:
                        filterExprWalker = new AxesWalker(walkingIterator, 2);
                        break;
                    case 40:
                        filterExprWalker = new AxesWalker(walkingIterator, 3);
                        break;
                    case 41:
                        filterExprWalker = new AxesWalker(walkingIterator, 4);
                        break;
                    case 42:
                        filterExprWalker = new AxesWalker(walkingIterator, 5);
                        break;
                    case 43:
                        filterExprWalker = new AxesWalker(walkingIterator, 6);
                        break;
                    case 44:
                        filterExprWalker = new AxesWalker(walkingIterator, 7);
                        break;
                    case 45:
                        filterExprWalker = new ReverseAxesWalker(walkingIterator, 10);
                        break;
                    case 46:
                        filterExprWalker = new ReverseAxesWalker(walkingIterator, 11);
                        break;
                    case 47:
                        filterExprWalker = new ReverseAxesWalker(walkingIterator, 12);
                        break;
                    case 48:
                        filterExprWalker = new AxesWalker(walkingIterator, 13);
                        break;
                    case 49:
                        filterExprWalker = new AxesWalker(walkingIterator, 9);
                        break;
                    case 50:
                        filterExprWalker = new AxesWalker(walkingIterator, 19);
                        break;
                    default:
                        throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(op)}));
                }
                break;
        }
        if (z) {
            filterExprWalker.initNodeTest(-1);
        } else {
            int whatToShow = compiler.getWhatToShow(i);
            if ((whatToShow & 4163) == 0 || whatToShow == -1) {
                filterExprWalker.initNodeTest(whatToShow);
            } else {
                filterExprWalker.initNodeTest(whatToShow, compiler.getStepNS(i), compiler.getStepLocalName(i));
            }
        }
        return filterExprWalker;
    }

    public static String getAnalysisString(int i) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("count: " + getStepCount(i) + " ");
        if ((1073741824 & i) != 0) {
            stringBuffer.append("NTANY|");
        }
        if ((i & 4096) != 0) {
            stringBuffer.append("PRED|");
        }
        if ((i & BIT_ANCESTOR) != 0) {
            stringBuffer.append("ANC|");
        }
        if ((i & BIT_ANCESTOR_OR_SELF) != 0) {
            stringBuffer.append("ANCOS|");
        }
        if ((32768 & i) != 0) {
            stringBuffer.append("ATTR|");
        }
        if ((65536 & i) != 0) {
            stringBuffer.append("CH|");
        }
        if ((131072 & i) != 0) {
            stringBuffer.append("DESC|");
        }
        if ((262144 & i) != 0) {
            stringBuffer.append("DESCOS|");
        }
        if ((524288 & i) != 0) {
            stringBuffer.append("FOL|");
        }
        if ((1048576 & i) != 0) {
            stringBuffer.append("FOLS|");
        }
        if ((2097152 & i) != 0) {
            stringBuffer.append("NS|");
        }
        if ((4194304 & i) != 0) {
            stringBuffer.append("P|");
        }
        if ((8388608 & i) != 0) {
            stringBuffer.append("PREC|");
        }
        if ((16777216 & i) != 0) {
            stringBuffer.append("PRECS|");
        }
        if ((33554432 & i) != 0) {
            stringBuffer.append(".|");
        }
        if ((67108864 & i) != 0) {
            stringBuffer.append("FLT|");
        }
        if ((i & BIT_ROOT) != 0) {
            stringBuffer.append("R|");
        }
        return stringBuffer.toString();
    }

    public static boolean hasPredicate(int i) {
        return (i & 4096) != 0;
    }

    public static boolean isWild(int i) {
        return (i & BIT_NODETEST_ANY) != 0;
    }

    public static boolean walksAncestors(int i) {
        return isSet(i, 24576);
    }

    public static boolean walksAttributes(int i) {
        return (i & BIT_ATTRIBUTE) != 0;
    }

    public static boolean walksNamespaces(int i) {
        return (i & BIT_NAMESPACE) != 0;
    }

    public static boolean walksChildren(int i) {
        return (i & 65536) != 0;
    }

    public static boolean walksDescendants(int i) {
        return isSet(i, 393216);
    }

    public static boolean walksSubtree(int i) {
        return isSet(i, 458752);
    }

    public static boolean walksSubtreeOnlyMaybeAbsolute(int i) {
        return (!walksSubtree(i) || walksExtraNodes(i) || walksUp(i) || walksSideways(i)) ? false : true;
    }

    public static boolean walksSubtreeOnly(int i) {
        return walksSubtreeOnlyMaybeAbsolute(i) && !isAbsolute(i);
    }

    public static boolean walksFilteredList(int i) {
        return isSet(i, BIT_FILTER);
    }

    public static boolean walksSubtreeOnlyFromRootOrContext(int i) {
        return (!walksSubtree(i) || walksExtraNodes(i) || walksUp(i) || walksSideways(i) || isSet(i, BIT_FILTER)) ? false : true;
    }

    public static boolean walksInDocOrder(int i) {
        return (walksSubtreeOnlyMaybeAbsolute(i) || walksExtraNodesOnly(i) || walksFollowingOnlyMaybeAbsolute(i)) && !isSet(i, BIT_FILTER);
    }

    public static boolean walksFollowingOnlyMaybeAbsolute(int i) {
        return (!isSet(i, 35127296) || walksSubtree(i) || walksUp(i) || walksSideways(i)) ? false : true;
    }

    public static boolean walksUp(int i) {
        return isSet(i, 4218880);
    }

    public static boolean walksSideways(int i) {
        return isSet(i, 26738688);
    }

    public static boolean walksExtraNodes(int i) {
        return isSet(i, 2129920);
    }

    public static boolean walksExtraNodesOnly(int i) {
        return (!walksExtraNodes(i) || isSet(i, BIT_SELF) || walksSubtree(i) || walksUp(i) || walksSideways(i) || isAbsolute(i)) ? false : true;
    }

    public static boolean isAbsolute(int i) {
        return isSet(i, 201326592);
    }

    public static boolean walksChildrenOnly(int i) {
        return (!walksChildren(i) || isSet(i, BIT_SELF) || walksExtraNodes(i) || walksDescendants(i) || walksUp(i) || walksSideways(i) || (isAbsolute(i) && !isSet(i, BIT_ROOT))) ? false : true;
    }

    public static boolean walksChildrenAndExtraAndSelfOnly(int i) {
        return (!walksChildren(i) || walksDescendants(i) || walksUp(i) || walksSideways(i) || (isAbsolute(i) && !isSet(i, BIT_ROOT))) ? false : true;
    }

    public static boolean walksDescendantsAndExtraAndSelfOnly(int i) {
        return (walksChildren(i) || !walksDescendants(i) || walksUp(i) || walksSideways(i) || (isAbsolute(i) && !isSet(i, BIT_ROOT))) ? false : true;
    }

    public static boolean walksSelfOnly(int i) {
        return (!isSet(i, BIT_SELF) || walksSubtree(i) || walksUp(i) || walksSideways(i) || isAbsolute(i)) ? false : true;
    }

    public static boolean walksUpOnly(int i) {
        return (walksSubtree(i) || !walksUp(i) || walksSideways(i) || isAbsolute(i)) ? false : true;
    }

    public static boolean walksDownOnly(int i) {
        return (!walksSubtree(i) || walksUp(i) || walksSideways(i) || isAbsolute(i)) ? false : true;
    }

    public static boolean walksDownExtraOnly(int i) {
        return (!walksSubtree(i) || !walksExtraNodes(i) || walksUp(i) || walksSideways(i) || isAbsolute(i)) ? false : true;
    }

    public static boolean canSkipSubtrees(int i) {
        return walksSideways(i) | isSet(i, 65536);
    }

    public static boolean canCrissCross(int i) {
        if (walksSelfOnly(i)) {
            return false;
        }
        return ((walksDownOnly(i) && !canSkipSubtrees(i)) || walksChildrenAndExtraAndSelfOnly(i) || walksDescendantsAndExtraAndSelfOnly(i) || walksUpOnly(i) || walksExtraNodesOnly(i) || !walksSubtree(i) || (!walksSideways(i) && !walksUp(i) && !canSkipSubtrees(i))) ? false : true;
    }

    public static boolean isNaturalDocOrder(int i) {
        return (canCrissCross(i) || isSet(i, BIT_NAMESPACE) || walksFilteredList(i) || !walksInDocOrder(i)) ? false : true;
    }

    private static boolean isNaturalDocOrder(Compiler compiler, int i, int i2, int i3) throws TransformerException {
        if (canCrissCross(i3) || isSet(i3, BIT_NAMESPACE)) {
            return false;
        }
        if (isSet(i3, 1572864) && isSet(i3, 25165824)) {
            return false;
        }
        int i4 = 0;
        boolean z = false;
        do {
            int op = compiler.getOp(i);
            if (-1 != op) {
                switch (op) {
                    default:
                        switch (op) {
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
                                break;
                            case 39:
                            case 51:
                                if (z) {
                                    return false;
                                }
                                if (compiler.getStepLocalName(i).equals("*")) {
                                    z = true;
                                }
                                i = compiler.getNextStepPos(i);
                                break;
                            case 40:
                            case 48:
                            case 50:
                                if (z) {
                                    return false;
                                }
                                i = compiler.getNextStepPos(i);
                                break;
                            default:
                                throw new RuntimeException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", new Object[]{Integer.toString(op)}));
                        }
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                        if (i4 > 0) {
                            return false;
                        }
                        i4++;
                        if (z) {
                        }
                        i = compiler.getNextStepPos(i);
                        break;
                }
            }
            return true;
        } while (i >= 0);
        return true;
    }

    public static boolean isOneStep(int i) {
        return (i & BITS_COUNT) == 1;
    }

    public static int getStepCount(int i) {
        return i & BITS_COUNT;
    }
}
