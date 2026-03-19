package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.utils.IntStack;
import org.apache.xml.utils.QName;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.xml.sax.SAXException;

public class ElemApplyTemplates extends ElemCallTemplate {
    static final long serialVersionUID = 2903125371542621004L;
    private QName m_mode = null;
    private boolean m_isDefaultTemplate = false;

    public void setMode(QName qName) {
        this.m_mode = qName;
    }

    public QName getMode() {
        return this.m_mode;
    }

    public void setIsDefaultTemplate(boolean z) {
        this.m_isDefaultTemplate = z;
    }

    @Override
    public int getXSLToken() {
        return 50;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_APPLY_TEMPLATES_STRING;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        boolean z = false;
        transformerImpl.pushCurrentTemplateRuleIsNull(false);
        try {
            QName mode = transformerImpl.getMode();
            if (!this.m_isDefaultTemplate && ((mode == null && this.m_mode != null) || (mode != null && !mode.equals(this.m_mode)))) {
                z = true;
                transformerImpl.pushMode(this.m_mode);
            }
            transformSelectedNodes(transformerImpl);
        } finally {
            if (z) {
                transformerImpl.popMode();
            }
            transformerImpl.popCurrentTemplateRuleIsNull();
        }
    }

    @Override
    public void transformSelectedNodes(TransformerImpl transformerImpl) throws Throwable {
        int i;
        int i2;
        boolean z;
        boolean z2;
        DTMIterator dTMIteratorSortNodes;
        SerializationHandler serializationHandler;
        StylesheetRoot stylesheet;
        TemplateList templateListComposed;
        boolean quietConflictWarnings;
        DTM dtm;
        int i3;
        IntStack currentNodeStack;
        IntStack currentExpressionNodeStack;
        DTMIterator dTMIterator;
        int i4;
        StylesheetRoot stylesheetRoot;
        int i5;
        int stackFrame;
        int i6;
        XPathContext xPathContext = transformerImpl.getXPathContext();
        int currentNode = xPathContext.getCurrentNode();
        DTMIterator dTMIteratorAsIterator = this.m_selectExpression.asIterator(xPathContext, currentNode);
        VariableStack varStack = xPathContext.getVarStack();
        int paramElemCount = getParamElemCount();
        int stackFrame2 = varStack.getStackFrame();
        int i7 = -1;
        try {
            xPathContext.pushCurrentNode(-1);
            xPathContext.pushCurrentExpressionNode(-1);
            xPathContext.pushSAXLocatorNull();
            transformerImpl.pushElemTemplateElement(null);
            Vector vectorProcessSortKeys = this.m_sortElems == null ? null : transformerImpl.processSortKeys(this, currentNode);
            dTMIteratorSortNodes = vectorProcessSortKeys != null ? sortNodes(xPathContext, vectorProcessSortKeys, dTMIteratorAsIterator) : dTMIteratorAsIterator;
        } catch (SAXException e) {
            e = e;
        } catch (Throwable th) {
            th = th;
        }
        try {
            serializationHandler = transformerImpl.getSerializationHandler();
            stylesheet = transformerImpl.getStylesheet();
            templateListComposed = stylesheet.getTemplateListComposed();
            quietConflictWarnings = transformerImpl.getQuietConflictWarnings();
            dtm = xPathContext.getDTM(currentNode);
            if (paramElemCount > 0) {
                int iLink = varStack.link(paramElemCount);
                varStack.setStackFrame(stackFrame2);
                for (int i8 = 0; i8 < paramElemCount; i8++) {
                    varStack.setLocalVariable(i8, this.m_paramElems[i8].getValue(transformerImpl, currentNode), iLink);
                }
                varStack.setStackFrame(iLink);
                i3 = iLink;
            } else {
                i3 = -1;
            }
            xPathContext.pushContextNodeList(dTMIteratorSortNodes);
            try {
                currentNodeStack = xPathContext.getCurrentNodeStack();
                currentExpressionNodeStack = xPathContext.getCurrentExpressionNodeStack();
            } catch (SAXException e2) {
                e = e2;
                dTMIteratorAsIterator = dTMIteratorSortNodes;
                i = paramElemCount;
                i2 = stackFrame2;
            } catch (Throwable th2) {
                th = th2;
                dTMIteratorAsIterator = dTMIteratorSortNodes;
                i = paramElemCount;
                i2 = stackFrame2;
            }
        } catch (SAXException e3) {
            e = e3;
            dTMIteratorAsIterator = dTMIteratorSortNodes;
            i = paramElemCount;
            i2 = stackFrame2;
            z2 = false;
            try {
                transformerImpl.getErrorListener().fatalError(new TransformerException(e));
                if (i > 0) {
                }
                xPathContext.popSAXLocator();
                if (z2) {
                }
                transformerImpl.popElemTemplateElement();
                xPathContext.popCurrentExpressionNode();
                xPathContext.popCurrentNode();
                dTMIteratorAsIterator.detach();
                return;
            } catch (Throwable th3) {
                th = th3;
                z = z2;
                if (i > 0) {
                    varStack.unlink(i2);
                }
                xPathContext.popSAXLocator();
                if (z) {
                    xPathContext.popContextNodeList();
                }
                transformerImpl.popElemTemplateElement();
                xPathContext.popCurrentExpressionNode();
                xPathContext.popCurrentNode();
                dTMIteratorAsIterator.detach();
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            dTMIteratorAsIterator = dTMIteratorSortNodes;
            i = paramElemCount;
            i2 = stackFrame2;
            z = false;
            if (i > 0) {
            }
            xPathContext.popSAXLocator();
            if (z) {
            }
            transformerImpl.popElemTemplateElement();
            xPathContext.popCurrentExpressionNode();
            xPathContext.popCurrentNode();
            dTMIteratorAsIterator.detach();
            throw th;
        }
        while (true) {
            int iNextNode = dTMIteratorSortNodes.nextNode();
            if (i7 == iNextNode) {
                DTMIterator dTMIterator2 = dTMIteratorSortNodes;
                int i9 = stackFrame2;
                if (paramElemCount > 0) {
                    varStack.unlink(i9);
                }
                xPathContext.popSAXLocator();
                xPathContext.popContextNodeList();
                transformerImpl.popElemTemplateElement();
                xPathContext.popCurrentExpressionNode();
                xPathContext.popCurrentNode();
                dTMIteratorAsIterator = dTMIterator2;
                dTMIteratorAsIterator.detach();
                return;
            }
            try {
                currentNodeStack.setTop(iNextNode);
                currentExpressionNodeStack.setTop(iNextNode);
                if (xPathContext.getDTM(iNextNode) != dtm) {
                    dtm = xPathContext.getDTM(iNextNode);
                }
                DTM dtm2 = dtm;
                int expandedTypeID = dtm2.getExpandedTypeID(iNextNode);
                short nodeType = dtm2.getNodeType(iNextNode);
                IntStack intStack = currentExpressionNodeStack;
                StylesheetRoot stylesheetRoot2 = stylesheet;
                int i10 = stackFrame2;
                SerializationHandler serializationHandler2 = serializationHandler;
                dTMIterator = dTMIteratorSortNodes;
                IntStack intStack2 = currentNodeStack;
                int i11 = i3;
                int i12 = paramElemCount;
                int i13 = 0;
                try {
                    ElemTemplate templateFast = templateListComposed.getTemplateFast(xPathContext, iNextNode, expandedTypeID, transformerImpl.getMode(), -1, quietConflictWarnings, dtm2);
                    if (templateFast != null) {
                        i4 = iNextNode;
                        stylesheetRoot = stylesheetRoot2;
                        transformerImpl.setCurrentElement(templateFast);
                    } else if (nodeType != 9) {
                        if (nodeType != 11) {
                            switch (nodeType) {
                                case 1:
                                    break;
                                case 2:
                                case 3:
                                case 4:
                                    stylesheetRoot = stylesheetRoot2;
                                    try {
                                        transformerImpl.pushPairCurrentMatched(stylesheetRoot.getDefaultTextRule(), iNextNode);
                                        transformerImpl.setCurrentElement(stylesheetRoot.getDefaultTextRule());
                                        dtm2.dispatchCharactersEvents(iNextNode, serializationHandler2, false);
                                        transformerImpl.popCurrentMatched();
                                        i5 = i11;
                                        i = i12;
                                        stylesheet = stylesheetRoot;
                                        paramElemCount = i;
                                        serializationHandler = serializationHandler2;
                                        dtm = dtm2;
                                        i3 = i5;
                                        currentNodeStack = intStack2;
                                        currentExpressionNodeStack = intStack;
                                        stackFrame2 = i10;
                                        dTMIteratorSortNodes = dTMIterator;
                                        i7 = -1;
                                    } catch (SAXException e4) {
                                        e = e4;
                                        i2 = i10;
                                        dTMIteratorAsIterator = dTMIterator;
                                        i = i12;
                                        z2 = true;
                                        transformerImpl.getErrorListener().fatalError(new TransformerException(e));
                                        if (i > 0) {
                                        }
                                        xPathContext.popSAXLocator();
                                        if (z2) {
                                        }
                                        transformerImpl.popElemTemplateElement();
                                        xPathContext.popCurrentExpressionNode();
                                        xPathContext.popCurrentNode();
                                        dTMIteratorAsIterator.detach();
                                        return;
                                    } catch (Throwable th5) {
                                        th = th5;
                                        i2 = i10;
                                        dTMIteratorAsIterator = dTMIterator;
                                        i = i12;
                                        z = true;
                                        if (i > 0) {
                                        }
                                        xPathContext.popSAXLocator();
                                        if (z) {
                                        }
                                        transformerImpl.popElemTemplateElement();
                                        xPathContext.popCurrentExpressionNode();
                                        xPathContext.popCurrentNode();
                                        dTMIteratorAsIterator.detach();
                                        throw th;
                                    }
                                    break;
                                default:
                                    stylesheetRoot = stylesheetRoot2;
                                    i5 = i11;
                                    i = i12;
                                    stylesheet = stylesheetRoot;
                                    paramElemCount = i;
                                    serializationHandler = serializationHandler2;
                                    dtm = dtm2;
                                    i3 = i5;
                                    currentNodeStack = intStack2;
                                    currentExpressionNodeStack = intStack;
                                    stackFrame2 = i10;
                                    dTMIteratorSortNodes = dTMIterator;
                                    i7 = -1;
                                    break;
                            }
                        }
                        i4 = iNextNode;
                        stylesheetRoot = stylesheetRoot2;
                        templateFast = stylesheetRoot.getDefaultRule();
                    } else {
                        i4 = iNextNode;
                        stylesheetRoot = stylesheetRoot2;
                        templateFast = stylesheetRoot.getDefaultRootRule();
                    }
                    transformerImpl.pushPairCurrentMatched(templateFast, i4);
                    if (templateFast.m_frameSize > 0) {
                        xPathContext.pushRTFContext();
                        stackFrame = varStack.getStackFrame();
                        varStack.link(templateFast.m_frameSize);
                        if (templateFast.m_inArgsSize > 0) {
                            ElemTemplateElement firstChildElem = templateFast.getFirstChildElem();
                            int i14 = 0;
                            while (firstChildElem != null && 41 == firstChildElem.getXSLToken()) {
                                ElemParam elemParam = (ElemParam) firstChildElem;
                                int i15 = i13;
                                while (true) {
                                    i = i12;
                                    if (i15 < i) {
                                        try {
                                            if (this.m_paramElems[i15].m_qnameID == elemParam.m_qnameID) {
                                                i6 = i11;
                                                varStack.setLocalVariable(i14, varStack.getLocalVariable(i15, i6));
                                            } else {
                                                i15++;
                                                i12 = i;
                                            }
                                        } catch (SAXException e5) {
                                            e = e5;
                                            i2 = i10;
                                            dTMIteratorAsIterator = dTMIterator;
                                            z2 = true;
                                            transformerImpl.getErrorListener().fatalError(new TransformerException(e));
                                            if (i > 0) {
                                                varStack.unlink(i2);
                                            }
                                            xPathContext.popSAXLocator();
                                            if (z2) {
                                                xPathContext.popContextNodeList();
                                            }
                                            transformerImpl.popElemTemplateElement();
                                            xPathContext.popCurrentExpressionNode();
                                            xPathContext.popCurrentNode();
                                            dTMIteratorAsIterator.detach();
                                            return;
                                        } catch (Throwable th6) {
                                            th = th6;
                                            i2 = i10;
                                            dTMIteratorAsIterator = dTMIterator;
                                            z = true;
                                            if (i > 0) {
                                            }
                                            xPathContext.popSAXLocator();
                                            if (z) {
                                            }
                                            transformerImpl.popElemTemplateElement();
                                            xPathContext.popCurrentExpressionNode();
                                            xPathContext.popCurrentNode();
                                            dTMIteratorAsIterator.detach();
                                            throw th;
                                        }
                                    } else {
                                        i6 = i11;
                                    }
                                }
                                if (i15 == i) {
                                    varStack.setLocalVariable(i14, null);
                                }
                                i14++;
                                firstChildElem = firstChildElem.getNextSiblingElem();
                                i12 = i;
                                i11 = i6;
                                i13 = 0;
                            }
                            i5 = i11;
                            i = i12;
                        } else {
                            i5 = i11;
                            i = i12;
                        }
                    } else {
                        i5 = i11;
                        i = i12;
                        stackFrame = 0;
                    }
                    for (ElemTemplateElement elemTemplateElement = templateFast.m_firstChild; elemTemplateElement != null; elemTemplateElement = elemTemplateElement.m_nextSibling) {
                        xPathContext.setSAXLocator(elemTemplateElement);
                        try {
                            transformerImpl.pushElemTemplateElement(elemTemplateElement);
                            elemTemplateElement.execute(transformerImpl);
                            transformerImpl.popElemTemplateElement();
                        } catch (Throwable th7) {
                            transformerImpl.popElemTemplateElement();
                            throw th7;
                        }
                    }
                    if (templateFast.m_frameSize > 0) {
                        varStack.unlink(stackFrame);
                        xPathContext.popRTFContext();
                    }
                    transformerImpl.popCurrentMatched();
                    stylesheet = stylesheetRoot;
                    paramElemCount = i;
                    serializationHandler = serializationHandler2;
                    dtm = dtm2;
                    i3 = i5;
                    currentNodeStack = intStack2;
                    currentExpressionNodeStack = intStack;
                    stackFrame2 = i10;
                    dTMIteratorSortNodes = dTMIterator;
                    i7 = -1;
                } catch (SAXException e6) {
                    e = e6;
                    i = i12;
                } catch (Throwable th8) {
                    th = th8;
                    i = i12;
                }
            } catch (SAXException e7) {
                e = e7;
                dTMIterator = dTMIteratorSortNodes;
                i = paramElemCount;
                i2 = stackFrame2;
            } catch (Throwable th9) {
                th = th9;
                dTMIterator = dTMIteratorSortNodes;
                i = paramElemCount;
                i2 = stackFrame2;
            }
        }
    }
}
