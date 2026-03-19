package org.apache.xalan.templates;

import java.util.Vector;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionNode;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPath;
import org.apache.xpath.axes.AxesWalker;
import org.apache.xpath.axes.FilterExprIteratorSimple;
import org.apache.xpath.axes.FilterExprWalker;
import org.apache.xpath.axes.LocPathIterator;
import org.apache.xpath.axes.SelfIteratorNoPredicate;
import org.apache.xpath.axes.WalkerFactory;
import org.apache.xpath.axes.WalkingIterator;
import org.apache.xpath.operations.Variable;
import org.apache.xpath.operations.VariableSafeAbsRef;
import org.w3c.dom.DOMException;

public class RedundentExprEliminator extends XSLTVisitor {
    public static final boolean DEBUG = false;
    public static final boolean DIAGNOSE_MULTISTEPLIST = false;
    public static final boolean DIAGNOSE_NUM_PATHS_REDUCED = false;
    static final String PSUEDOVARNAMESPACE = "http://xml.apache.org/xalan/psuedovar";
    private static int m_uniquePseudoVarID = 1;
    AbsPathChecker m_absPathChecker = new AbsPathChecker();
    VarNameCollector m_varNameCollector = new VarNameCollector();
    boolean m_isSameContext = true;
    Vector m_absPaths = new Vector();
    Vector m_paths = null;

    public void eleminateRedundentLocals(ElemTemplateElement psuedoVarRecipient) {
        eleminateRedundent(psuedoVarRecipient, this.m_paths);
    }

    public void eleminateRedundentGlobals(StylesheetRoot stylesheet) {
        eleminateRedundent(stylesheet, this.m_absPaths);
    }

    protected void eleminateRedundent(ElemTemplateElement psuedoVarRecipient, Vector paths) {
        int n = paths.size();
        int numPathsEliminated = 0;
        int numUniquePathsEliminated = 0;
        for (int i = 0; i < n; i++) {
            ExpressionOwner owner = (ExpressionOwner) paths.elementAt(i);
            if (owner != null) {
                int found = findAndEliminateRedundant(i + 1, i, owner, psuedoVarRecipient, paths);
                if (found > 0) {
                    numUniquePathsEliminated++;
                }
                numPathsEliminated += found;
            }
        }
        eleminateSharedPartialPaths(psuedoVarRecipient, paths);
    }

    protected void eleminateSharedPartialPaths(ElemTemplateElement psuedoVarRecipient, Vector paths) {
        MultistepExprHolder list = createMultistepExprList(paths);
        if (list == null) {
            return;
        }
        boolean isGlobal = paths == this.m_absPaths;
        int longestStepsCount = list.m_stepCount;
        for (int i = longestStepsCount - 1; i >= 1; i--) {
            for (MultistepExprHolder next = list; next != null && next.m_stepCount >= i; next = next.m_next) {
                list = matchAndEliminatePartialPaths(next, list, isGlobal, i, psuedoVarRecipient);
            }
        }
    }

    protected MultistepExprHolder matchAndEliminatePartialPaths(MultistepExprHolder testee, MultistepExprHolder head, boolean isGlobal, int lengthToTest, ElemTemplateElement varScope) {
        if (testee.m_exprOwner == null) {
            return head;
        }
        WalkingIterator iter1 = (WalkingIterator) testee.m_exprOwner.getExpression();
        if (partialIsVariable(testee, lengthToTest)) {
            return head;
        }
        MultistepExprHolder matchedPaths = null;
        MultistepExprHolder matchedPathsTail = null;
        for (MultistepExprHolder meh = head; meh != null; meh = meh.m_next) {
            if (meh != testee && meh.m_exprOwner != null) {
                WalkingIterator iter2 = (WalkingIterator) meh.m_exprOwner.getExpression();
                if (stepsEqual(iter1, iter2, lengthToTest)) {
                    if (matchedPaths == null) {
                        try {
                            matchedPaths = (MultistepExprHolder) testee.clone();
                            testee.m_exprOwner = null;
                        } catch (CloneNotSupportedException e) {
                        }
                        matchedPathsTail = matchedPaths;
                        matchedPathsTail.m_next = null;
                    }
                    try {
                        matchedPathsTail.m_next = (MultistepExprHolder) meh.clone();
                        meh.m_exprOwner = null;
                    } catch (CloneNotSupportedException e2) {
                    }
                    matchedPathsTail = matchedPathsTail.m_next;
                    matchedPathsTail.m_next = null;
                }
            }
        }
        if (matchedPaths != null) {
            ElemTemplateElement root = isGlobal ? varScope : findCommonAncestor(matchedPaths);
            WalkingIterator sharedIter = (WalkingIterator) matchedPaths.m_exprOwner.getExpression();
            WalkingIterator newIter = createIteratorFromSteps(sharedIter, lengthToTest);
            ElemVariable var = createPseudoVarDecl(root, newIter, isGlobal);
            while (matchedPaths != null) {
                ExpressionOwner owner = matchedPaths.m_exprOwner;
                WalkingIterator iter = (WalkingIterator) owner.getExpression();
                LocPathIterator newIter2 = changePartToRef(var.getName(), iter, lengthToTest, isGlobal);
                owner.setExpression(newIter2);
                matchedPaths = matchedPaths.m_next;
            }
        }
        return head;
    }

    boolean partialIsVariable(MultistepExprHolder testee, int lengthToTest) {
        if (1 == lengthToTest) {
            WalkingIterator wi = (WalkingIterator) testee.m_exprOwner.getExpression();
            return wi.getFirstWalker() instanceof FilterExprWalker;
        }
        return false;
    }

    protected void diagnoseLineNumber(Expression expr) {
        ElemTemplateElement e = getElemFromExpression(expr);
        System.err.println("   " + e.getSystemId() + " Line " + e.getLineNumber());
    }

    protected ElemTemplateElement findCommonAncestor(MultistepExprHolder head) {
        int numExprs = head.getLength();
        ElemTemplateElement[] elems = new ElemTemplateElement[numExprs];
        int[] ancestorCounts = new int[numExprs];
        MultistepExprHolder next = head;
        int shortestAncestorCount = 10000;
        for (int i = 0; i < numExprs; i++) {
            ElemTemplateElement elem = getElemFromExpression(next.m_exprOwner.getExpression());
            elems[i] = elem;
            int numAncestors = countAncestors(elem);
            ancestorCounts[i] = numAncestors;
            if (numAncestors < shortestAncestorCount) {
                shortestAncestorCount = numAncestors;
            }
            next = next.m_next;
        }
        for (int i2 = 0; i2 < numExprs; i2++) {
            if (ancestorCounts[i2] > shortestAncestorCount) {
                int numStepCorrection = ancestorCounts[i2] - shortestAncestorCount;
                for (int j = 0; j < numStepCorrection; j++) {
                    elems[i2] = elems[i2].getParentElem();
                }
            }
        }
        while (true) {
            int shortestAncestorCount2 = shortestAncestorCount;
            shortestAncestorCount = shortestAncestorCount2 - 1;
            if (shortestAncestorCount2 >= 0) {
                boolean areEqual = true;
                ElemTemplateElement first = elems[0];
                int i3 = 1;
                while (true) {
                    if (i3 >= numExprs) {
                        break;
                    }
                    if (first == elems[i3]) {
                        i3++;
                    } else {
                        areEqual = false;
                        break;
                    }
                }
                if (areEqual && isNotSameAsOwner(head, first) && first.canAcceptVariables()) {
                    return first;
                }
                for (int i4 = 0; i4 < numExprs; i4++) {
                    elems[i4] = elems[i4].getParentElem();
                }
            } else {
                assertion(false, "Could not find common ancestor!!!");
                return null;
            }
        }
    }

    protected boolean isNotSameAsOwner(MultistepExprHolder head, ElemTemplateElement ete) {
        for (MultistepExprHolder next = head; next != null; next = next.m_next) {
            ElemTemplateElement elemOwner = getElemFromExpression(next.m_exprOwner.getExpression());
            if (elemOwner == ete) {
                return false;
            }
        }
        return true;
    }

    protected int countAncestors(ElemTemplateElement elem) {
        int count = 0;
        while (elem != null) {
            count++;
            elem = elem.getParentElem();
        }
        return count;
    }

    protected void diagnoseMultistepList(int matchCount, int lengthToTest, boolean isGlobal) {
        if (matchCount <= 0) {
            return;
        }
        System.err.print("Found multistep matches: " + matchCount + ", " + lengthToTest + " length");
        if (isGlobal) {
            System.err.println(" (global)");
        } else {
            System.err.println();
        }
    }

    protected LocPathIterator changePartToRef(QName qName, WalkingIterator walkingIterator, int i, boolean z) {
        Variable variable = new Variable();
        variable.setQName(qName);
        variable.setIsGlobal(z);
        if (z) {
            variable.setIndex(getElemFromExpression(walkingIterator).getStylesheetRoot().getVariablesAndParamsComposed().size() - 1);
        }
        int i2 = 0;
        AxesWalker firstWalker = walkingIterator.getFirstWalker();
        while (i2 < i) {
            assertion(firstWalker != null, "Walker should not be null!");
            i2++;
            firstWalker = firstWalker.getNextWalker();
        }
        if (firstWalker != null) {
            FilterExprWalker filterExprWalker = new FilterExprWalker(walkingIterator);
            filterExprWalker.setInnerExpression(variable);
            filterExprWalker.exprSetParent(walkingIterator);
            filterExprWalker.setNextWalker(firstWalker);
            firstWalker.setPrevWalker(filterExprWalker);
            walkingIterator.setFirstWalker(filterExprWalker);
            return walkingIterator;
        }
        FilterExprIteratorSimple filterExprIteratorSimple = new FilterExprIteratorSimple(variable);
        filterExprIteratorSimple.exprSetParent(walkingIterator.exprGetParent());
        return filterExprIteratorSimple;
    }

    protected WalkingIterator createIteratorFromSteps(WalkingIterator walkingIterator, int i) {
        WalkingIterator walkingIterator2 = new WalkingIterator(walkingIterator.getPrefixResolver());
        try {
            AxesWalker axesWalker = (AxesWalker) walkingIterator.getFirstWalker().clone();
            walkingIterator2.setFirstWalker(axesWalker);
            axesWalker.setLocPathIterator(walkingIterator2);
            AxesWalker axesWalker2 = axesWalker;
            for (int i2 = 1; i2 < i; i2++) {
                AxesWalker axesWalker3 = (AxesWalker) axesWalker2.getNextWalker().clone();
                axesWalker2.setNextWalker(axesWalker3);
                axesWalker3.setLocPathIterator(walkingIterator2);
                axesWalker2 = axesWalker3;
            }
            axesWalker2.setNextWalker(null);
            return walkingIterator2;
        } catch (CloneNotSupportedException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    protected boolean stepsEqual(WalkingIterator iter1, WalkingIterator iter2, int numSteps) {
        AxesWalker aw1 = iter1.getFirstWalker();
        AxesWalker aw2 = iter2.getFirstWalker();
        for (int i = 0; i < numSteps; i++) {
            if (aw1 == null || aw2 == null || !aw1.deepEquals(aw2)) {
                return false;
            }
            aw1 = aw1.getNextWalker();
            aw2 = aw2.getNextWalker();
        }
        assertion((aw1 == null && aw2 == null) ? false : true, "Total match is incorrect!");
        return true;
    }

    protected MultistepExprHolder createMultistepExprList(Vector paths) {
        MultistepExprHolder first = null;
        int n = paths.size();
        for (int i = 0; i < n; i++) {
            ExpressionOwner eo = (ExpressionOwner) paths.elementAt(i);
            if (eo != null) {
                LocPathIterator lpi = (LocPathIterator) eo.getExpression();
                int numPaths = countSteps(lpi);
                if (numPaths > 1) {
                    if (first == null) {
                        first = new MultistepExprHolder(eo, numPaths, null);
                    } else {
                        first = first.addInSortedOrder(eo, numPaths);
                    }
                }
            }
        }
        if (first == null || first.getLength() <= 1) {
            return null;
        }
        return first;
    }

    protected int findAndEliminateRedundant(int start, int firstOccuranceIndex, ExpressionOwner firstOccuranceOwner, ElemTemplateElement psuedoVarRecipient, Vector paths) throws DOMException {
        MultistepExprHolder head = null;
        MultistepExprHolder tail = null;
        int numPathsFound = 0;
        int n = paths.size();
        Expression expr1 = firstOccuranceOwner.getExpression();
        boolean isGlobal = paths == this.m_absPaths;
        LocPathIterator lpi = (LocPathIterator) expr1;
        int stepCount = countSteps(lpi);
        for (int j = start; j < n; j++) {
            ExpressionOwner owner2 = (ExpressionOwner) paths.elementAt(j);
            if (owner2 != null) {
                Expression expr2 = owner2.getExpression();
                boolean isEqual = expr2.deepEquals(lpi);
                if (isEqual) {
                    if (head == null) {
                        head = new MultistepExprHolder(firstOccuranceOwner, stepCount, null);
                        tail = head;
                        numPathsFound++;
                    }
                    tail.m_next = new MultistepExprHolder(owner2, stepCount, null);
                    tail = tail.m_next;
                    paths.setElementAt(null, j);
                    numPathsFound++;
                }
            }
        }
        if (numPathsFound == 0 && isGlobal) {
            head = new MultistepExprHolder(firstOccuranceOwner, stepCount, null);
            numPathsFound++;
        }
        if (head != null) {
            ElemTemplateElement root = isGlobal ? psuedoVarRecipient : findCommonAncestor(head);
            LocPathIterator sharedIter = (LocPathIterator) head.m_exprOwner.getExpression();
            ElemVariable var = createPseudoVarDecl(root, sharedIter, isGlobal);
            QName uniquePseudoVarName = var.getName();
            while (head != null) {
                ExpressionOwner owner = head.m_exprOwner;
                changeToVarRef(uniquePseudoVarName, owner, paths, root);
                head = head.m_next;
            }
            paths.setElementAt(var.getSelect(), firstOccuranceIndex);
        }
        return numPathsFound;
    }

    protected int oldFindAndEliminateRedundant(int start, int firstOccuranceIndex, ExpressionOwner firstOccuranceOwner, ElemTemplateElement psuedoVarRecipient, Vector paths) throws DOMException {
        QName uniquePseudoVarName = null;
        boolean foundFirst = false;
        int numPathsFound = 0;
        int n = paths.size();
        Expression expr1 = firstOccuranceOwner.getExpression();
        boolean isGlobal = paths == this.m_absPaths;
        LocPathIterator lpi = (LocPathIterator) expr1;
        for (int j = start; j < n; j++) {
            ExpressionOwner owner2 = (ExpressionOwner) paths.elementAt(j);
            if (owner2 != null) {
                Expression expr2 = owner2.getExpression();
                boolean isEqual = expr2.deepEquals(lpi);
                if (isEqual) {
                    if (!foundFirst) {
                        foundFirst = true;
                        ElemVariable var = createPseudoVarDecl(psuedoVarRecipient, lpi, isGlobal);
                        if (var == null) {
                            return 0;
                        }
                        uniquePseudoVarName = var.getName();
                        changeToVarRef(uniquePseudoVarName, firstOccuranceOwner, paths, psuedoVarRecipient);
                        paths.setElementAt(var.getSelect(), firstOccuranceIndex);
                        numPathsFound++;
                    }
                    changeToVarRef(uniquePseudoVarName, owner2, paths, psuedoVarRecipient);
                    paths.setElementAt(null, j);
                    numPathsFound++;
                } else {
                    continue;
                }
            }
        }
        if (numPathsFound == 0 && paths == this.m_absPaths) {
            ElemVariable var2 = createPseudoVarDecl(psuedoVarRecipient, lpi, true);
            if (var2 == null) {
                return 0;
            }
            QName uniquePseudoVarName2 = var2.getName();
            changeToVarRef(uniquePseudoVarName2, firstOccuranceOwner, paths, psuedoVarRecipient);
            paths.setElementAt(var2.getSelect(), firstOccuranceIndex);
            return numPathsFound + 1;
        }
        return numPathsFound;
    }

    protected int countSteps(LocPathIterator locPathIterator) {
        if (locPathIterator instanceof WalkingIterator) {
            int count = 0;
            for (AxesWalker aw = locPathIterator.getFirstWalker(); aw != null; aw = aw.getNextWalker()) {
                count++;
            }
            return count;
        }
        return 1;
    }

    protected void changeToVarRef(QName varName, ExpressionOwner owner, Vector paths, ElemTemplateElement psuedoVarRecipient) {
        Variable varRef = paths == this.m_absPaths ? new VariableSafeAbsRef() : new Variable();
        varRef.setQName(varName);
        if (paths == this.m_absPaths) {
            StylesheetRoot root = (StylesheetRoot) psuedoVarRecipient;
            Vector globalVars = root.getVariablesAndParamsComposed();
            varRef.setIndex(globalVars.size() - 1);
            varRef.setIsGlobal(true);
        }
        owner.setExpression(varRef);
    }

    private static synchronized int getPseudoVarID() {
        int i;
        i = m_uniquePseudoVarID;
        m_uniquePseudoVarID = i + 1;
        return i;
    }

    protected ElemVariable createPseudoVarDecl(ElemTemplateElement psuedoVarRecipient, LocPathIterator lpi, boolean isGlobal) throws DOMException {
        QName uniquePseudoVarName = new QName(PSUEDOVARNAMESPACE, "#" + getPseudoVarID());
        if (isGlobal) {
            return createGlobalPseudoVarDecl(uniquePseudoVarName, (StylesheetRoot) psuedoVarRecipient, lpi);
        }
        return createLocalPseudoVarDecl(uniquePseudoVarName, psuedoVarRecipient, lpi);
    }

    protected ElemVariable createGlobalPseudoVarDecl(QName uniquePseudoVarName, StylesheetRoot stylesheetRoot, LocPathIterator lpi) throws DOMException {
        ElemVariable psuedoVar = new ElemVariable();
        psuedoVar.setIsTopLevel(true);
        XPath xpath = new XPath(lpi);
        psuedoVar.setSelect(xpath);
        psuedoVar.setName(uniquePseudoVarName);
        Vector globalVars = stylesheetRoot.getVariablesAndParamsComposed();
        psuedoVar.setIndex(globalVars.size());
        globalVars.addElement(psuedoVar);
        return psuedoVar;
    }

    protected ElemVariable createLocalPseudoVarDecl(QName uniquePseudoVarName, ElemTemplateElement psuedoVarRecipient, LocPathIterator lpi) throws DOMException {
        ElemVariable psuedoVar = new ElemVariablePsuedo();
        XPath xpath = new XPath(lpi);
        psuedoVar.setSelect(xpath);
        psuedoVar.setName(uniquePseudoVarName);
        ElemVariable var = addVarDeclToElem(psuedoVarRecipient, lpi, psuedoVar);
        lpi.exprSetParent(var);
        return var;
    }

    protected ElemVariable addVarDeclToElem(ElemTemplateElement psuedoVarRecipient, LocPathIterator lpi, ElemVariable psuedoVar) throws DOMException {
        ElemTemplateElement ete = psuedoVarRecipient.getFirstChildElem();
        lpi.callVisitors(null, this.m_varNameCollector);
        if (this.m_varNameCollector.getVarCount() > 0) {
            ElemTemplateElement baseElem = getElemFromExpression(lpi);
            ElemVariable varElem = getPrevVariableElem(baseElem);
            while (true) {
                if (varElem == null) {
                    break;
                }
                if (this.m_varNameCollector.doesOccur(varElem.getName())) {
                    psuedoVarRecipient = varElem.getParentElem();
                    ete = varElem.getNextSiblingElem();
                    break;
                }
                varElem = getPrevVariableElem(varElem);
            }
        }
        if (ete != null && 41 == ete.getXSLToken()) {
            if (!isParam(lpi)) {
                while (ete != null) {
                    ete = ete.getNextSiblingElem();
                    if (ete != null && 41 != ete.getXSLToken()) {
                        break;
                    }
                }
            } else {
                return null;
            }
        }
        psuedoVarRecipient.insertBefore(psuedoVar, ete);
        this.m_varNameCollector.reset();
        return psuedoVar;
    }

    protected boolean isParam(ExpressionNode expr) {
        while (expr != null && !(expr instanceof ElemTemplateElement)) {
            expr = expr.exprGetParent();
        }
        if (expr != null) {
            for (ElemTemplateElement ete = (ElemTemplateElement) expr; ete != null; ete = ete.getParentElem()) {
                int type = ete.getXSLToken();
                switch (type) {
                    case 19:
                    case 25:
                        return false;
                    case 41:
                        return true;
                    default:
                        break;
                }
            }
        }
        return false;
    }

    protected ElemVariable getPrevVariableElem(ElemTemplateElement elem) {
        int type;
        do {
            elem = getPrevElementWithinContext(elem);
            if (elem == null) {
                return null;
            }
            type = elem.getXSLToken();
            if (73 == type) {
                break;
            }
        } while (41 != type);
        return (ElemVariable) elem;
    }

    protected ElemTemplateElement getPrevElementWithinContext(ElemTemplateElement elem) {
        ElemTemplateElement prev = elem.getPreviousSiblingElem();
        if (prev == null) {
            prev = elem.getParentElem();
        }
        if (prev != null) {
            int type = prev.getXSLToken();
            if (28 == type || 19 == type || 25 == type) {
                return null;
            }
            return prev;
        }
        return prev;
    }

    protected ElemTemplateElement getElemFromExpression(Expression expression) {
        for (?? ExprGetParent = expression.exprGetParent(); ExprGetParent != 0; ExprGetParent = ExprGetParent.exprGetParent()) {
            if (ExprGetParent instanceof ElemTemplateElement) {
                return ExprGetParent;
            }
        }
        throw new RuntimeException(XSLMessages.createMessage(XSLTErrorResources.ER_ASSERT_NO_TEMPLATE_PARENT, null));
    }

    public boolean isAbsolute(LocPathIterator path) {
        boolean isAbs;
        int analysis = path.getAnalysisBits();
        if (WalkerFactory.isSet(analysis, WalkerFactory.BIT_ROOT)) {
            isAbs = true;
        } else {
            isAbs = WalkerFactory.isSet(analysis, WalkerFactory.BIT_ANY_DESCENDANT_FROM_ROOT);
        }
        if (isAbs) {
            boolean isAbs2 = this.m_absPathChecker.checkAbsolute(path);
            return isAbs2;
        }
        return isAbs;
    }

    @Override
    public boolean visitLocationPath(ExpressionOwner owner, LocPathIterator locPathIterator) {
        if (locPathIterator instanceof SelfIteratorNoPredicate) {
            return true;
        }
        if (locPathIterator instanceof WalkingIterator) {
            ?? firstWalker = locPathIterator.getFirstWalker();
            if ((firstWalker instanceof FilterExprWalker) && firstWalker.getNextWalker() == null) {
                Expression exp = firstWalker.getInnerExpression();
                if (exp instanceof Variable) {
                    return true;
                }
            }
        }
        if (isAbsolute(locPathIterator) && this.m_absPaths != null) {
            this.m_absPaths.addElement(owner);
        } else if (this.m_isSameContext && this.m_paths != null) {
            this.m_paths.addElement(owner);
        }
        return true;
    }

    @Override
    public boolean visitPredicate(ExpressionOwner owner, Expression pred) {
        boolean savedIsSame = this.m_isSameContext;
        this.m_isSameContext = false;
        pred.callVisitors(owner, this);
        this.m_isSameContext = savedIsSame;
        return false;
    }

    @Override
    public boolean visitTopLevelInstruction(ElemTemplateElement elem) {
        int type = elem.getXSLToken();
        switch (type) {
            case 19:
                return visitInstruction(elem);
            default:
                return true;
        }
    }

    @Override
    public boolean visitInstruction(ElemTemplateElement elem) {
        int type = elem.getXSLToken();
        switch (type) {
            case 17:
            case 19:
            case 28:
                if (type == 28) {
                    ElemForEach efe = (ElemForEach) elem;
                    Expression select = efe.getSelect();
                    select.callVisitors(efe, this);
                }
                Vector savedPaths = this.m_paths;
                this.m_paths = new Vector();
                elem.callChildVisitors(this, false);
                eleminateRedundentLocals(elem);
                this.m_paths = savedPaths;
                break;
            case 35:
            case 64:
                boolean savedIsSame = this.m_isSameContext;
                this.m_isSameContext = false;
                elem.callChildVisitors(this);
                this.m_isSameContext = savedIsSame;
                break;
        }
        return false;
    }

    protected void diagnoseNumPaths(Vector paths, int numPathsEliminated, int numUniquePathsEliminated) {
        if (numPathsEliminated <= 0) {
            return;
        }
        if (paths == this.m_paths) {
            System.err.println("Eliminated " + numPathsEliminated + " total paths!");
            System.err.println("Consolodated " + numUniquePathsEliminated + " redundent paths!");
        } else {
            System.err.println("Eliminated " + numPathsEliminated + " total global paths!");
            System.err.println("Consolodated " + numUniquePathsEliminated + " redundent global paths!");
        }
    }

    private final void assertIsLocPathIterator(Expression expression, ExpressionOwner eo) throws RuntimeException {
        if (expression instanceof LocPathIterator) {
            return;
        }
        String errMsg = expression instanceof Variable ? "Programmer's assertion: expr1 not an iterator: " + expression.getQName() : "Programmer's assertion: expr1 not an iterator: " + expression.getClass().getName();
        throw new RuntimeException(errMsg + ", " + eo.getClass().getName() + " " + expression.exprGetParent());
    }

    private static void validateNewAddition(Vector paths, ExpressionOwner owner, LocPathIterator path) throws RuntimeException {
        assertion(owner.getExpression() == path, "owner.getExpression() != path!!!");
        int n = paths.size();
        for (int i = 0; i < n; i++) {
            ExpressionOwner ew = (ExpressionOwner) paths.elementAt(i);
            assertion(ew != owner, "duplicate owner on the list!!!");
            assertion(ew.getExpression() != path, "duplicate expression on the list!!!");
        }
    }

    protected static void assertion(boolean b, String msg) {
        if (b) {
        } else {
            throw new RuntimeException(XSLMessages.createMessage(XSLTErrorResources.ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR, new Object[]{msg}));
        }
    }

    class MultistepExprHolder implements Cloneable {
        ExpressionOwner m_exprOwner;
        MultistepExprHolder m_next;
        final int m_stepCount;

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        MultistepExprHolder(ExpressionOwner exprOwner, int stepCount, MultistepExprHolder next) {
            this.m_exprOwner = exprOwner;
            RedundentExprEliminator.assertion(this.m_exprOwner != null, "exprOwner can not be null!");
            this.m_stepCount = stepCount;
            this.m_next = next;
        }

        MultistepExprHolder addInSortedOrder(ExpressionOwner exprOwner, int stepCount) {
            MultistepExprHolder multistepExprHolder = null;
            for (MultistepExprHolder next = this; next != null; next = next.m_next) {
                if (stepCount >= next.m_stepCount) {
                    MultistepExprHolder newholder = RedundentExprEliminator.this.new MultistepExprHolder(exprOwner, stepCount, next);
                    if (multistepExprHolder == null) {
                        return newholder;
                    }
                    multistepExprHolder.m_next = newholder;
                    return this;
                }
                multistepExprHolder = next;
            }
            multistepExprHolder.m_next = RedundentExprEliminator.this.new MultistepExprHolder(exprOwner, stepCount, null);
            return this;
        }

        MultistepExprHolder unlink(MultistepExprHolder itemToRemove) {
            MultistepExprHolder first = this;
            MultistepExprHolder multistepExprHolder = null;
            for (MultistepExprHolder next = this; next != null; next = next.m_next) {
                if (next == itemToRemove) {
                    if (multistepExprHolder == null) {
                        first = next.m_next;
                    } else {
                        multistepExprHolder.m_next = next.m_next;
                    }
                    next.m_next = null;
                    return first;
                }
                multistepExprHolder = next;
            }
            RedundentExprEliminator.assertion(false, "unlink failed!!!");
            return null;
        }

        int getLength() {
            int count = 0;
            for (MultistepExprHolder next = this; next != null; next = next.m_next) {
                count++;
            }
            return count;
        }

        protected void diagnose() {
            System.err.print("Found multistep iterators: " + getLength() + "  ");
            MultistepExprHolder next = this;
            while (next != null) {
                System.err.print("" + next.m_stepCount);
                next = next.m_next;
                if (next != null) {
                    System.err.print(", ");
                }
            }
            System.err.println();
        }
    }
}
