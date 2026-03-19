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

    public void eleminateRedundentLocals(ElemTemplateElement elemTemplateElement) {
        eleminateRedundent(elemTemplateElement, this.m_paths);
    }

    public void eleminateRedundentGlobals(StylesheetRoot stylesheetRoot) {
        eleminateRedundent(stylesheetRoot, this.m_absPaths);
    }

    protected void eleminateRedundent(ElemTemplateElement elemTemplateElement, Vector vector) {
        int size = vector.size();
        for (int i = 0; i < size; i++) {
            ExpressionOwner expressionOwner = (ExpressionOwner) vector.elementAt(i);
            if (expressionOwner == null || findAndEliminateRedundant(i + 1, i, expressionOwner, elemTemplateElement, vector) > 0) {
            }
        }
        eleminateSharedPartialPaths(elemTemplateElement, vector);
    }

    protected void eleminateSharedPartialPaths(ElemTemplateElement elemTemplateElement, Vector vector) {
        boolean z;
        MultistepExprHolder multistepExprHolderCreateMultistepExprList = createMultistepExprList(vector);
        if (multistepExprHolderCreateMultistepExprList != null) {
            if (vector != this.m_absPaths) {
                z = false;
            } else {
                z = true;
            }
            int i = multistepExprHolderCreateMultistepExprList.m_stepCount - 1;
            while (i >= 1) {
                MultistepExprHolder multistepExprHolderMatchAndEliminatePartialPaths = multistepExprHolderCreateMultistepExprList;
                while (multistepExprHolderCreateMultistepExprList != null && multistepExprHolderCreateMultistepExprList.m_stepCount >= i) {
                    multistepExprHolderMatchAndEliminatePartialPaths = matchAndEliminatePartialPaths(multistepExprHolderCreateMultistepExprList, multistepExprHolderMatchAndEliminatePartialPaths, z, i, elemTemplateElement);
                    multistepExprHolderCreateMultistepExprList = multistepExprHolderCreateMultistepExprList.m_next;
                }
                i--;
                multistepExprHolderCreateMultistepExprList = multistepExprHolderMatchAndEliminatePartialPaths;
            }
        }
    }

    protected MultistepExprHolder matchAndEliminatePartialPaths(MultistepExprHolder multistepExprHolder, MultistepExprHolder multistepExprHolder2, boolean z, int i, ElemTemplateElement elemTemplateElement) {
        MultistepExprHolder multistepExprHolder3;
        if (multistepExprHolder.m_exprOwner == null) {
            return multistepExprHolder2;
        }
        WalkingIterator walkingIterator = (WalkingIterator) multistepExprHolder.m_exprOwner.getExpression();
        if (partialIsVariable(multistepExprHolder, i)) {
            return multistepExprHolder2;
        }
        MultistepExprHolder multistepExprHolder4 = null;
        MultistepExprHolder multistepExprHolder5 = null;
        for (MultistepExprHolder multistepExprHolder6 = multistepExprHolder2; multistepExprHolder6 != null; multistepExprHolder6 = multistepExprHolder6.m_next) {
            if (multistepExprHolder6 != multistepExprHolder && multistepExprHolder6.m_exprOwner != null && stepsEqual(walkingIterator, (WalkingIterator) multistepExprHolder6.m_exprOwner.getExpression(), i)) {
                if (multistepExprHolder4 == null) {
                    try {
                        MultistepExprHolder multistepExprHolder7 = (MultistepExprHolder) multistepExprHolder.clone();
                        try {
                            multistepExprHolder.m_exprOwner = null;
                            multistepExprHolder4 = multistepExprHolder7;
                        } catch (CloneNotSupportedException e) {
                            multistepExprHolder4 = multistepExprHolder7;
                        }
                    } catch (CloneNotSupportedException e2) {
                    }
                    multistepExprHolder4.m_next = null;
                    multistepExprHolder3 = multistepExprHolder4;
                } else {
                    MultistepExprHolder multistepExprHolder8 = multistepExprHolder5;
                    multistepExprHolder3 = multistepExprHolder4;
                    multistepExprHolder4 = multistepExprHolder8;
                }
                try {
                    multistepExprHolder4.m_next = (MultistepExprHolder) multistepExprHolder6.clone();
                    multistepExprHolder6.m_exprOwner = null;
                } catch (CloneNotSupportedException e3) {
                }
                MultistepExprHolder multistepExprHolder9 = multistepExprHolder4.m_next;
                multistepExprHolder9.m_next = null;
                MultistepExprHolder multistepExprHolder10 = multistepExprHolder3;
                multistepExprHolder5 = multistepExprHolder9;
                multistepExprHolder4 = multistepExprHolder10;
            }
        }
        if (multistepExprHolder4 != null) {
            if (!z) {
                elemTemplateElement = findCommonAncestor(multistepExprHolder4);
            }
            ElemVariable elemVariableCreatePseudoVarDecl = createPseudoVarDecl(elemTemplateElement, createIteratorFromSteps((WalkingIterator) multistepExprHolder4.m_exprOwner.getExpression(), i), z);
            while (multistepExprHolder4 != null) {
                ExpressionOwner expressionOwner = multistepExprHolder4.m_exprOwner;
                expressionOwner.setExpression(changePartToRef(elemVariableCreatePseudoVarDecl.getName(), (WalkingIterator) expressionOwner.getExpression(), i, z));
                multistepExprHolder4 = multistepExprHolder4.m_next;
            }
        }
        return multistepExprHolder2;
    }

    boolean partialIsVariable(MultistepExprHolder multistepExprHolder, int i) {
        if (1 == i && (((WalkingIterator) multistepExprHolder.m_exprOwner.getExpression()).getFirstWalker() instanceof FilterExprWalker)) {
            return true;
        }
        return false;
    }

    protected void diagnoseLineNumber(Expression expression) {
        ElemTemplateElement elemFromExpression = getElemFromExpression(expression);
        System.err.println("   " + elemFromExpression.getSystemId() + " Line " + elemFromExpression.getLineNumber());
    }

    protected ElemTemplateElement findCommonAncestor(MultistepExprHolder multistepExprHolder) {
        int length = multistepExprHolder.getLength();
        ElemTemplateElement[] elemTemplateElementArr = new ElemTemplateElement[length];
        int[] iArr = new int[length];
        MultistepExprHolder multistepExprHolder2 = multistepExprHolder;
        int i = 10000;
        for (int i2 = 0; i2 < length; i2++) {
            ElemTemplateElement elemFromExpression = getElemFromExpression(multistepExprHolder2.m_exprOwner.getExpression());
            elemTemplateElementArr[i2] = elemFromExpression;
            int iCountAncestors = countAncestors(elemFromExpression);
            iArr[i2] = iCountAncestors;
            if (iCountAncestors < i) {
                i = iCountAncestors;
            }
            multistepExprHolder2 = multistepExprHolder2.m_next;
        }
        for (int i3 = 0; i3 < length; i3++) {
            if (iArr[i3] > i) {
                int i4 = iArr[i3] - i;
                for (int i5 = 0; i5 < i4; i5++) {
                    elemTemplateElementArr[i3] = elemTemplateElementArr[i3].getParentElem();
                }
            }
        }
        while (true) {
            int i6 = i - 1;
            if (i < 0) {
                assertion(false, "Could not find common ancestor!!!");
                return null;
            }
            ElemTemplateElement elemTemplateElement = elemTemplateElementArr[0];
            boolean z = true;
            int i7 = 1;
            while (true) {
                if (i7 >= length) {
                    break;
                }
                if (elemTemplateElement == elemTemplateElementArr[i7]) {
                    i7++;
                } else {
                    z = false;
                    break;
                }
            }
            if (z && isNotSameAsOwner(multistepExprHolder, elemTemplateElement) && elemTemplateElement.canAcceptVariables()) {
                return elemTemplateElement;
            }
            for (int i8 = 0; i8 < length; i8++) {
                elemTemplateElementArr[i8] = elemTemplateElementArr[i8].getParentElem();
            }
            i = i6;
        }
    }

    protected boolean isNotSameAsOwner(MultistepExprHolder multistepExprHolder, ElemTemplateElement elemTemplateElement) {
        while (multistepExprHolder != null) {
            if (getElemFromExpression(multistepExprHolder.m_exprOwner.getExpression()) == elemTemplateElement) {
                return false;
            }
            multistepExprHolder = multistepExprHolder.m_next;
        }
        return true;
    }

    protected int countAncestors(ElemTemplateElement elemTemplateElement) {
        int i = 0;
        while (elemTemplateElement != null) {
            i++;
            elemTemplateElement = elemTemplateElement.getParentElem();
        }
        return i;
    }

    protected void diagnoseMultistepList(int i, int i2, boolean z) {
        if (i > 0) {
            System.err.print("Found multistep matches: " + i + ", " + i2 + " length");
            if (z) {
                System.err.println(" (global)");
            } else {
                System.err.println();
            }
        }
    }

    protected LocPathIterator changePartToRef(QName qName, WalkingIterator walkingIterator, int i, boolean z) {
        boolean z2;
        Variable variable = new Variable();
        variable.setQName(qName);
        variable.setIsGlobal(z);
        if (z) {
            variable.setIndex(getElemFromExpression(walkingIterator).getStylesheetRoot().getVariablesAndParamsComposed().size() - 1);
        }
        AxesWalker firstWalker = walkingIterator.getFirstWalker();
        int i2 = 0;
        while (i2 < i) {
            if (firstWalker != null) {
                z2 = true;
            } else {
                z2 = false;
            }
            assertion(z2, "Walker should not be null!");
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
            int i2 = 1;
            AxesWalker axesWalker2 = axesWalker;
            while (i2 < i) {
                AxesWalker axesWalker3 = (AxesWalker) axesWalker2.getNextWalker().clone();
                axesWalker2.setNextWalker(axesWalker3);
                axesWalker3.setLocPathIterator(walkingIterator2);
                i2++;
                axesWalker2 = axesWalker3;
            }
            axesWalker2.setNextWalker(null);
            return walkingIterator2;
        } catch (CloneNotSupportedException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    protected boolean stepsEqual(WalkingIterator walkingIterator, WalkingIterator walkingIterator2, int i) {
        AxesWalker firstWalker = walkingIterator.getFirstWalker();
        boolean z = false;
        AxesWalker firstWalker2 = walkingIterator2.getFirstWalker();
        AxesWalker nextWalker = firstWalker;
        for (int i2 = 0; i2 < i; i2++) {
            if (nextWalker == null || firstWalker2 == null || !nextWalker.deepEquals(firstWalker2)) {
                return false;
            }
            nextWalker = nextWalker.getNextWalker();
            firstWalker2 = firstWalker2.getNextWalker();
        }
        if (nextWalker != null || firstWalker2 != null) {
            z = true;
        }
        assertion(z, "Total match is incorrect!");
        return true;
    }

    protected MultistepExprHolder createMultistepExprList(Vector vector) {
        int iCountSteps;
        int size = vector.size();
        MultistepExprHolder multistepExprHolderAddInSortedOrder = null;
        for (int i = 0; i < size; i++) {
            ExpressionOwner expressionOwner = (ExpressionOwner) vector.elementAt(i);
            if (expressionOwner != null && (iCountSteps = countSteps((LocPathIterator) expressionOwner.getExpression())) > 1) {
                if (multistepExprHolderAddInSortedOrder == null) {
                    multistepExprHolderAddInSortedOrder = new MultistepExprHolder(expressionOwner, iCountSteps, null);
                } else {
                    multistepExprHolderAddInSortedOrder = multistepExprHolderAddInSortedOrder.addInSortedOrder(expressionOwner, iCountSteps);
                }
            }
        }
        if (multistepExprHolderAddInSortedOrder == null || multistepExprHolderAddInSortedOrder.getLength() <= 1) {
            return null;
        }
        return multistepExprHolderAddInSortedOrder;
    }

    protected int findAndEliminateRedundant(int i, int i2, ExpressionOwner expressionOwner, ElemTemplateElement elemTemplateElement, Vector vector) throws DOMException {
        ElemTemplateElement elemTemplateElementFindCommonAncestor;
        MultistepExprHolder multistepExprHolder;
        MultistepExprHolder multistepExprHolder2;
        int size = vector.size();
        Expression expression = expressionOwner.getExpression();
        boolean z = vector == this.m_absPaths;
        LocPathIterator locPathIterator = (LocPathIterator) expression;
        int iCountSteps = countSteps(locPathIterator);
        int i3 = 0;
        MultistepExprHolder multistepExprHolder3 = null;
        MultistepExprHolder multistepExprHolder4 = null;
        for (int i4 = i; i4 < size; i4++) {
            ExpressionOwner expressionOwner2 = (ExpressionOwner) vector.elementAt(i4);
            if (expressionOwner2 != null) {
                Expression expression2 = expressionOwner2.getExpression();
                if (expression2.deepEquals(locPathIterator)) {
                    if (multistepExprHolder3 == null) {
                        multistepExprHolder2 = new MultistepExprHolder(expressionOwner, iCountSteps, null);
                        i3++;
                        multistepExprHolder = multistepExprHolder2;
                    } else {
                        MultistepExprHolder multistepExprHolder5 = multistepExprHolder4;
                        multistepExprHolder = multistepExprHolder3;
                        multistepExprHolder2 = multistepExprHolder5;
                    }
                    multistepExprHolder2.m_next = new MultistepExprHolder(expressionOwner2, iCountSteps, null);
                    MultistepExprHolder multistepExprHolder6 = multistepExprHolder2.m_next;
                    vector.setElementAt(null, i4);
                    i3++;
                    MultistepExprHolder multistepExprHolder7 = multistepExprHolder;
                    multistepExprHolder4 = multistepExprHolder6;
                    multistepExprHolder3 = multistepExprHolder7;
                }
            }
        }
        if (i3 == 0 && z) {
            multistepExprHolder3 = new MultistepExprHolder(expressionOwner, iCountSteps, null);
            i3++;
        }
        if (multistepExprHolder3 != null) {
            if (!z) {
                elemTemplateElementFindCommonAncestor = findCommonAncestor(multistepExprHolder3);
            } else {
                elemTemplateElementFindCommonAncestor = elemTemplateElement;
            }
            ElemVariable elemVariableCreatePseudoVarDecl = createPseudoVarDecl(elemTemplateElementFindCommonAncestor, (LocPathIterator) multistepExprHolder3.m_exprOwner.getExpression(), z);
            QName name = elemVariableCreatePseudoVarDecl.getName();
            while (multistepExprHolder3 != null) {
                changeToVarRef(name, multistepExprHolder3.m_exprOwner, vector, elemTemplateElementFindCommonAncestor);
                multistepExprHolder3 = multistepExprHolder3.m_next;
            }
            vector.setElementAt(elemVariableCreatePseudoVarDecl.getSelect(), i2);
        }
        return i3;
    }

    protected int oldFindAndEliminateRedundant(int i, int i2, ExpressionOwner expressionOwner, ElemTemplateElement elemTemplateElement, Vector vector) throws DOMException {
        int size = vector.size();
        Expression expression = expressionOwner.getExpression();
        boolean z = vector == this.m_absPaths;
        LocPathIterator locPathIterator = (LocPathIterator) expression;
        int i3 = 0;
        boolean z2 = false;
        QName name = null;
        for (int i4 = i; i4 < size; i4++) {
            ExpressionOwner expressionOwner2 = (ExpressionOwner) vector.elementAt(i4);
            if (expressionOwner2 != null) {
                Expression expression2 = expressionOwner2.getExpression();
                if (expression2.deepEquals(locPathIterator)) {
                    if (!z2) {
                        ElemVariable elemVariableCreatePseudoVarDecl = createPseudoVarDecl(elemTemplateElement, locPathIterator, z);
                        if (elemVariableCreatePseudoVarDecl == null) {
                            return 0;
                        }
                        name = elemVariableCreatePseudoVarDecl.getName();
                        changeToVarRef(name, expressionOwner, vector, elemTemplateElement);
                        vector.setElementAt(elemVariableCreatePseudoVarDecl.getSelect(), i2);
                        i3++;
                        z2 = true;
                    }
                    changeToVarRef(name, expressionOwner2, vector, elemTemplateElement);
                    vector.setElementAt(null, i4);
                    i3++;
                } else {
                    continue;
                }
            }
        }
        if (i3 == 0 && vector == this.m_absPaths) {
            ElemVariable elemVariableCreatePseudoVarDecl2 = createPseudoVarDecl(elemTemplateElement, locPathIterator, true);
            if (elemVariableCreatePseudoVarDecl2 == null) {
                return 0;
            }
            changeToVarRef(elemVariableCreatePseudoVarDecl2.getName(), expressionOwner, vector, elemTemplateElement);
            vector.setElementAt(elemVariableCreatePseudoVarDecl2.getSelect(), i2);
            return i3 + 1;
        }
        return i3;
    }

    protected int countSteps(LocPathIterator locPathIterator) {
        if (locPathIterator instanceof WalkingIterator) {
            int i = 0;
            for (AxesWalker firstWalker = ((WalkingIterator) locPathIterator).getFirstWalker(); firstWalker != null; firstWalker = firstWalker.getNextWalker()) {
                i++;
            }
            return i;
        }
        return 1;
    }

    protected void changeToVarRef(QName qName, ExpressionOwner expressionOwner, Vector vector, ElemTemplateElement elemTemplateElement) {
        Variable variableSafeAbsRef = vector == this.m_absPaths ? new VariableSafeAbsRef() : new Variable();
        variableSafeAbsRef.setQName(qName);
        if (vector == this.m_absPaths) {
            variableSafeAbsRef.setIndex(((StylesheetRoot) elemTemplateElement).getVariablesAndParamsComposed().size() - 1);
            variableSafeAbsRef.setIsGlobal(true);
        }
        expressionOwner.setExpression(variableSafeAbsRef);
    }

    private static synchronized int getPseudoVarID() {
        int i;
        i = m_uniquePseudoVarID;
        m_uniquePseudoVarID = i + 1;
        return i;
    }

    protected ElemVariable createPseudoVarDecl(ElemTemplateElement elemTemplateElement, LocPathIterator locPathIterator, boolean z) throws DOMException {
        QName qName = new QName(PSUEDOVARNAMESPACE, "#" + getPseudoVarID());
        if (z) {
            return createGlobalPseudoVarDecl(qName, (StylesheetRoot) elemTemplateElement, locPathIterator);
        }
        return createLocalPseudoVarDecl(qName, elemTemplateElement, locPathIterator);
    }

    protected ElemVariable createGlobalPseudoVarDecl(QName qName, StylesheetRoot stylesheetRoot, LocPathIterator locPathIterator) throws DOMException {
        ElemVariable elemVariable = new ElemVariable();
        elemVariable.setIsTopLevel(true);
        elemVariable.setSelect(new XPath(locPathIterator));
        elemVariable.setName(qName);
        Vector variablesAndParamsComposed = stylesheetRoot.getVariablesAndParamsComposed();
        elemVariable.setIndex(variablesAndParamsComposed.size());
        variablesAndParamsComposed.addElement(elemVariable);
        return elemVariable;
    }

    protected ElemVariable createLocalPseudoVarDecl(QName qName, ElemTemplateElement elemTemplateElement, LocPathIterator locPathIterator) throws DOMException {
        ElemVariablePsuedo elemVariablePsuedo = new ElemVariablePsuedo();
        elemVariablePsuedo.setSelect(new XPath(locPathIterator));
        elemVariablePsuedo.setName(qName);
        ElemVariable elemVariableAddVarDeclToElem = addVarDeclToElem(elemTemplateElement, locPathIterator, elemVariablePsuedo);
        locPathIterator.exprSetParent(elemVariableAddVarDeclToElem);
        return elemVariableAddVarDeclToElem;
    }

    protected ElemVariable addVarDeclToElem(ElemTemplateElement elemTemplateElement, LocPathIterator locPathIterator, ElemVariable elemVariable) throws DOMException {
        ElemTemplateElement firstChildElem = elemTemplateElement.getFirstChildElem();
        locPathIterator.callVisitors(null, this.m_varNameCollector);
        if (this.m_varNameCollector.getVarCount() > 0) {
            ElemVariable prevVariableElem = getPrevVariableElem(getElemFromExpression(locPathIterator));
            while (true) {
                if (prevVariableElem == null) {
                    break;
                }
                if (this.m_varNameCollector.doesOccur(prevVariableElem.getName())) {
                    elemTemplateElement = prevVariableElem.getParentElem();
                    firstChildElem = prevVariableElem.getNextSiblingElem();
                    break;
                }
                prevVariableElem = getPrevVariableElem(prevVariableElem);
            }
        }
        if (firstChildElem != null && 41 == firstChildElem.getXSLToken()) {
            if (!isParam(locPathIterator)) {
                while (firstChildElem != null) {
                    firstChildElem = firstChildElem.getNextSiblingElem();
                    if (firstChildElem != null && 41 != firstChildElem.getXSLToken()) {
                        break;
                    }
                }
            } else {
                return null;
            }
        }
        elemTemplateElement.insertBefore(elemVariable, firstChildElem);
        this.m_varNameCollector.reset();
        return elemVariable;
    }

    protected boolean isParam(ExpressionNode expressionNode) {
        while (expressionNode != null && !(expressionNode instanceof ElemTemplateElement)) {
            expressionNode = expressionNode.exprGetParent();
        }
        if (expressionNode != null) {
            for (ElemTemplateElement parentElem = (ElemTemplateElement) expressionNode; parentElem != null; parentElem = parentElem.getParentElem()) {
                int xSLToken = parentElem.getXSLToken();
                if (xSLToken == 19 || xSLToken == 25) {
                    return false;
                }
                if (xSLToken == 41) {
                    return true;
                }
            }
        }
        return false;
    }

    protected ElemVariable getPrevVariableElem(ElemTemplateElement elemTemplateElement) {
        int xSLToken;
        do {
            elemTemplateElement = getPrevElementWithinContext(elemTemplateElement);
            if (elemTemplateElement != null) {
                xSLToken = elemTemplateElement.getXSLToken();
                if (73 == xSLToken) {
                    break;
                }
            } else {
                return null;
            }
        } while (41 != xSLToken);
        return (ElemVariable) elemTemplateElement;
    }

    protected ElemTemplateElement getPrevElementWithinContext(ElemTemplateElement elemTemplateElement) {
        ElemTemplateElement previousSiblingElem = elemTemplateElement.getPreviousSiblingElem();
        if (previousSiblingElem == null) {
            previousSiblingElem = elemTemplateElement.getParentElem();
        }
        if (previousSiblingElem != null) {
            int xSLToken = previousSiblingElem.getXSLToken();
            if (28 == xSLToken || 19 == xSLToken || 25 == xSLToken) {
                return null;
            }
            return previousSiblingElem;
        }
        return previousSiblingElem;
    }

    protected ElemTemplateElement getElemFromExpression(Expression expression) {
        for (ExpressionNode expressionNodeExprGetParent = expression.exprGetParent(); expressionNodeExprGetParent != null; expressionNodeExprGetParent = expressionNodeExprGetParent.exprGetParent()) {
            if (expressionNodeExprGetParent instanceof ElemTemplateElement) {
                return (ElemTemplateElement) expressionNodeExprGetParent;
            }
        }
        throw new RuntimeException(XSLMessages.createMessage(XSLTErrorResources.ER_ASSERT_NO_TEMPLATE_PARENT, null));
    }

    public boolean isAbsolute(LocPathIterator locPathIterator) {
        int analysisBits = locPathIterator.getAnalysisBits();
        boolean z = WalkerFactory.isSet(analysisBits, WalkerFactory.BIT_ROOT) || WalkerFactory.isSet(analysisBits, WalkerFactory.BIT_ANY_DESCENDANT_FROM_ROOT);
        if (z) {
            return this.m_absPathChecker.checkAbsolute(locPathIterator);
        }
        return z;
    }

    @Override
    public boolean visitLocationPath(ExpressionOwner expressionOwner, LocPathIterator locPathIterator) {
        if (locPathIterator instanceof SelfIteratorNoPredicate) {
            return true;
        }
        if (locPathIterator instanceof WalkingIterator) {
            AxesWalker firstWalker = ((WalkingIterator) locPathIterator).getFirstWalker();
            if ((firstWalker instanceof FilterExprWalker) && firstWalker.getNextWalker() == null && (((FilterExprWalker) firstWalker).getInnerExpression() instanceof Variable)) {
                return true;
            }
        }
        if (isAbsolute(locPathIterator) && this.m_absPaths != null) {
            this.m_absPaths.addElement(expressionOwner);
        } else if (this.m_isSameContext && this.m_paths != null) {
            this.m_paths.addElement(expressionOwner);
        }
        return true;
    }

    @Override
    public boolean visitPredicate(ExpressionOwner expressionOwner, Expression expression) {
        boolean z = this.m_isSameContext;
        this.m_isSameContext = false;
        expression.callVisitors(expressionOwner, this);
        this.m_isSameContext = z;
        return false;
    }

    @Override
    public boolean visitTopLevelInstruction(ElemTemplateElement elemTemplateElement) {
        if (elemTemplateElement.getXSLToken() == 19) {
            return visitInstruction(elemTemplateElement);
        }
        return true;
    }

    @Override
    public boolean visitInstruction(ElemTemplateElement elemTemplateElement) {
        int xSLToken = elemTemplateElement.getXSLToken();
        if (xSLToken != 17 && xSLToken != 19 && xSLToken != 28) {
            if (xSLToken == 35 || xSLToken == 64) {
                boolean z = this.m_isSameContext;
                this.m_isSameContext = false;
                elemTemplateElement.callChildVisitors(this);
                this.m_isSameContext = z;
                return false;
            }
            return true;
        }
        if (xSLToken == 28) {
            ElemForEach elemForEach = (ElemForEach) elemTemplateElement;
            elemForEach.getSelect().callVisitors(elemForEach, this);
        }
        Vector vector = this.m_paths;
        this.m_paths = new Vector();
        elemTemplateElement.callChildVisitors(this, false);
        eleminateRedundentLocals(elemTemplateElement);
        this.m_paths = vector;
        return false;
    }

    protected void diagnoseNumPaths(Vector vector, int i, int i2) {
        if (i > 0) {
            if (vector == this.m_paths) {
                System.err.println("Eliminated " + i + " total paths!");
                System.err.println("Consolodated " + i2 + " redundent paths!");
                return;
            }
            System.err.println("Eliminated " + i + " total global paths!");
            System.err.println("Consolodated " + i2 + " redundent global paths!");
        }
    }

    private final void assertIsLocPathIterator(Expression expression, ExpressionOwner expressionOwner) throws RuntimeException {
        String str;
        if (!(expression instanceof LocPathIterator)) {
            if (expression instanceof Variable) {
                str = "Programmer's assertion: expr1 not an iterator: " + ((Variable) expression).getQName();
            } else {
                str = "Programmer's assertion: expr1 not an iterator: " + expression.getClass().getName();
            }
            throw new RuntimeException(str + ", " + expressionOwner.getClass().getName() + " " + expression.exprGetParent());
        }
    }

    private static void validateNewAddition(Vector vector, ExpressionOwner expressionOwner, LocPathIterator locPathIterator) throws RuntimeException {
        assertion(expressionOwner.getExpression() == locPathIterator, "owner.getExpression() != path!!!");
        int size = vector.size();
        for (int i = 0; i < size; i++) {
            ExpressionOwner expressionOwner2 = (ExpressionOwner) vector.elementAt(i);
            assertion(expressionOwner2 != expressionOwner, "duplicate owner on the list!!!");
            assertion(expressionOwner2.getExpression() != locPathIterator, "duplicate expression on the list!!!");
        }
    }

    protected static void assertion(boolean z, String str) {
        if (!z) {
            throw new RuntimeException(XSLMessages.createMessage(XSLTErrorResources.ER_ASSERT_REDUNDENT_EXPR_ELIMINATOR, new Object[]{str}));
        }
    }

    class MultistepExprHolder implements Cloneable {
        ExpressionOwner m_exprOwner;
        MultistepExprHolder m_next;
        final int m_stepCount;

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        MultistepExprHolder(ExpressionOwner expressionOwner, int i, MultistepExprHolder multistepExprHolder) {
            this.m_exprOwner = expressionOwner;
            RedundentExprEliminator.assertion(this.m_exprOwner != null, "exprOwner can not be null!");
            this.m_stepCount = i;
            this.m_next = multistepExprHolder;
        }

        MultistepExprHolder addInSortedOrder(ExpressionOwner expressionOwner, int i) {
            MultistepExprHolder multistepExprHolder = null;
            for (MultistepExprHolder multistepExprHolder2 = this; multistepExprHolder2 != null; multistepExprHolder2 = multistepExprHolder2.m_next) {
                if (i >= multistepExprHolder2.m_stepCount) {
                    MultistepExprHolder multistepExprHolder3 = RedundentExprEliminator.this.new MultistepExprHolder(expressionOwner, i, multistepExprHolder2);
                    if (multistepExprHolder == null) {
                        return multistepExprHolder3;
                    }
                    multistepExprHolder.m_next = multistepExprHolder3;
                    return this;
                }
                multistepExprHolder = multistepExprHolder2;
            }
            multistepExprHolder.m_next = RedundentExprEliminator.this.new MultistepExprHolder(expressionOwner, i, null);
            return this;
        }

        MultistepExprHolder unlink(MultistepExprHolder multistepExprHolder) {
            MultistepExprHolder multistepExprHolder2;
            MultistepExprHolder multistepExprHolder3 = null;
            for (MultistepExprHolder multistepExprHolder4 = this; multistepExprHolder4 != null; multistepExprHolder4 = multistepExprHolder4.m_next) {
                if (multistepExprHolder4 == multistepExprHolder) {
                    if (multistepExprHolder3 == null) {
                        multistepExprHolder2 = multistepExprHolder4.m_next;
                    } else {
                        multistepExprHolder3.m_next = multistepExprHolder4.m_next;
                        multistepExprHolder2 = this;
                    }
                    multistepExprHolder4.m_next = null;
                    return multistepExprHolder2;
                }
                multistepExprHolder3 = multistepExprHolder4;
            }
            RedundentExprEliminator.assertion(false, "unlink failed!!!");
            return null;
        }

        int getLength() {
            int i = 0;
            for (MultistepExprHolder multistepExprHolder = this; multistepExprHolder != null; multistepExprHolder = multistepExprHolder.m_next) {
                i++;
            }
            return i;
        }

        protected void diagnose() {
            System.err.print("Found multistep iterators: " + getLength() + "  ");
            MultistepExprHolder multistepExprHolder = this;
            while (multistepExprHolder != null) {
                System.err.print("" + multistepExprHolder.m_stepCount);
                multistepExprHolder = multistepExprHolder.m_next;
                if (multistepExprHolder != null) {
                    System.err.print(", ");
                }
            }
            System.err.println();
        }
    }
}
