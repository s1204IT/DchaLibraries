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
import org.apache.xpath.objects.XObject;
import org.xml.sax.SAXException;

public class ElemApplyTemplates extends ElemCallTemplate {
    static final long serialVersionUID = 2903125371542621004L;
    private QName m_mode = null;
    private boolean m_isDefaultTemplate = false;

    public void setMode(QName mode) {
        this.m_mode = mode;
    }

    public QName getMode() {
        return this.m_mode;
    }

    public void setIsDefaultTemplate(boolean b) {
        this.m_isDefaultTemplate = b;
    }

    @Override
    public int getXSLToken() {
        return 50;
    }

    @Override
    public void compose(StylesheetRoot sroot) throws TransformerException {
        super.compose(sroot);
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_APPLY_TEMPLATES_STRING;
    }

    @Override
    public void execute(TransformerImpl transformer) throws TransformerException {
        transformer.pushCurrentTemplateRuleIsNull(false);
        boolean pushMode = false;
        try {
            QName mode = transformer.getMode();
            if (!this.m_isDefaultTemplate && ((mode == null && this.m_mode != null) || (mode != null && !mode.equals(this.m_mode)))) {
                pushMode = true;
                transformer.pushMode(this.m_mode);
            }
            transformSelectedNodes(transformer);
        } finally {
            if (pushMode) {
                transformer.popMode();
            }
            transformer.popCurrentTemplateRuleIsNull();
        }
    }

    @Override
    public void transformSelectedNodes(TransformerImpl transformer) throws TransformerException {
        Vector vectorProcessSortKeys;
        SerializationHandler rth;
        StylesheetRoot sroot;
        TemplateList tl;
        boolean quiet;
        DTM dtm;
        int argsFrame;
        IntStack currentNodes;
        IntStack currentExpressionNodes;
        int currentFrameBottom;
        XPathContext xctxt = transformer.getXPathContext();
        int sourceNode = xctxt.getCurrentNode();
        DTMIterator sourceNodes = this.m_selectExpression.asIterator(xctxt, sourceNode);
        VariableStack vars = xctxt.getVarStack();
        int nParams = getParamElemCount();
        int thisframe = vars.getStackFrame();
        boolean pushContextNodeListFlag = false;
        try {
            try {
                xctxt.pushCurrentNode(-1);
                xctxt.pushCurrentExpressionNode(-1);
                xctxt.pushSAXLocatorNull();
                transformer.pushElemTemplateElement(null);
                if (this.m_sortElems == null) {
                    vectorProcessSortKeys = null;
                } else {
                    vectorProcessSortKeys = transformer.processSortKeys(this, sourceNode);
                }
                if (vectorProcessSortKeys != null) {
                    sourceNodes = sortNodes(xctxt, vectorProcessSortKeys, sourceNodes);
                }
                rth = transformer.getSerializationHandler();
                sroot = transformer.getStylesheet();
                tl = sroot.getTemplateListComposed();
                quiet = transformer.getQuietConflictWarnings();
                dtm = xctxt.getDTM(sourceNode);
                argsFrame = -1;
                if (nParams > 0) {
                    argsFrame = vars.link(nParams);
                    vars.setStackFrame(thisframe);
                    for (int i = 0; i < nParams; i++) {
                        ElemWithParam ewp = this.m_paramElems[i];
                        XObject obj = ewp.getValue(transformer, sourceNode);
                        vars.setLocalVariable(i, obj, argsFrame);
                    }
                    vars.setStackFrame(argsFrame);
                }
                xctxt.pushContextNodeList(sourceNodes);
                pushContextNodeListFlag = true;
                currentNodes = xctxt.getCurrentNodeStack();
                currentExpressionNodes = xctxt.getCurrentExpressionNodeStack();
            } catch (SAXException se) {
                transformer.getErrorListener().fatalError(new TransformerException(se));
                if (nParams > 0) {
                    vars.unlink(thisframe);
                }
                xctxt.popSAXLocator();
                if (pushContextNodeListFlag) {
                    xctxt.popContextNodeList();
                }
                transformer.popElemTemplateElement();
                xctxt.popCurrentExpressionNode();
                xctxt.popCurrentNode();
                sourceNodes.detach();
                return;
            }
        } catch (Throwable th) {
            if (nParams > 0) {
            }
            xctxt.popSAXLocator();
            if (pushContextNodeListFlag) {
            }
            transformer.popElemTemplateElement();
            xctxt.popCurrentExpressionNode();
            xctxt.popCurrentNode();
            sourceNodes.detach();
            throw th;
        }
        while (true) {
            int child = sourceNodes.nextNode();
            if (-1 != child) {
                currentNodes.setTop(child);
                currentExpressionNodes.setTop(child);
                if (xctxt.getDTM(child) != dtm) {
                    dtm = xctxt.getDTM(child);
                }
                int exNodeType = dtm.getExpandedTypeID(child);
                int nodeType = dtm.getNodeType(child);
                QName mode = transformer.getMode();
                ElemTemplate template = tl.getTemplateFast(xctxt, child, exNodeType, mode, -1, quiet, dtm);
                if (template == null) {
                    switch (nodeType) {
                        case 1:
                        case 11:
                            template = sroot.getDefaultRule();
                            break;
                        case 2:
                        case 3:
                        case 4:
                            transformer.pushPairCurrentMatched(sroot.getDefaultTextRule(), child);
                            transformer.setCurrentElement(sroot.getDefaultTextRule());
                            dtm.dispatchCharactersEvents(child, rth, false);
                            transformer.popCurrentMatched();
                            continue;
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 10:
                        default:
                            continue;
                        case 9:
                            template = sroot.getDefaultRootRule();
                            break;
                    }
                } else {
                    transformer.setCurrentElement(template);
                }
                transformer.pushPairCurrentMatched(template, child);
                if (template.m_frameSize > 0) {
                    xctxt.pushRTFContext();
                    currentFrameBottom = vars.getStackFrame();
                    vars.link(template.m_frameSize);
                    if (template.m_inArgsSize > 0) {
                        int paramIndex = 0;
                        for (ElemTemplateElement elem = template.getFirstChildElem(); elem != null && 41 == elem.getXSLToken(); elem = elem.getNextSiblingElem()) {
                            ElemParam ep = (ElemParam) elem;
                            int i2 = 0;
                            while (true) {
                                if (i2 < nParams) {
                                    ElemWithParam ewp2 = this.m_paramElems[i2];
                                    if (ewp2.m_qnameID != ep.m_qnameID) {
                                        i2++;
                                    } else {
                                        XObject obj2 = vars.getLocalVariable(i2, argsFrame);
                                        vars.setLocalVariable(paramIndex, obj2);
                                    }
                                }
                            }
                            if (i2 == nParams) {
                                vars.setLocalVariable(paramIndex, null);
                            }
                            paramIndex++;
                        }
                    }
                } else {
                    currentFrameBottom = 0;
                }
                for (ElemTemplateElement t = template.m_firstChild; t != null; t = t.m_nextSibling) {
                    xctxt.setSAXLocator(t);
                    try {
                        transformer.pushElemTemplateElement(t);
                        t.execute(transformer);
                        transformer.popElemTemplateElement();
                    } catch (Throwable th2) {
                        transformer.popElemTemplateElement();
                        throw th2;
                    }
                }
                if (template.m_frameSize > 0) {
                    vars.unlink(currentFrameBottom);
                    xctxt.popRTFContext();
                }
                transformer.popCurrentMatched();
            } else {
                if (nParams > 0) {
                    vars.unlink(thisframe);
                }
                xctxt.popSAXLocator();
                if (1 != 0) {
                    xctxt.popContextNodeList();
                }
                transformer.popElemTemplateElement();
                xctxt.popCurrentExpressionNode();
                xctxt.popCurrentNode();
                sourceNodes.detach();
                return;
            }
            if (nParams > 0) {
                vars.unlink(thisframe);
            }
            xctxt.popSAXLocator();
            if (pushContextNodeListFlag) {
                xctxt.popContextNodeList();
            }
            transformer.popElemTemplateElement();
            xctxt.popCurrentExpressionNode();
            xctxt.popCurrentNode();
            sourceNodes.detach();
            throw th;
        }
    }
}
