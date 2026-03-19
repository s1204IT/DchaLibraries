package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.QName;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XRTreeFrag;
import org.apache.xpath.objects.XRTreeFragSelectWrapper;
import org.apache.xpath.objects.XString;

public class ElemVariable extends ElemTemplateElement {
    static final long serialVersionUID = 9111131075322790061L;
    int m_frameSize;
    protected int m_index;
    private boolean m_isTopLevel;
    protected QName m_qname;
    private XPath m_selectPattern;

    public ElemVariable() {
        this.m_frameSize = -1;
        this.m_isTopLevel = false;
    }

    public void setIndex(int i) {
        this.m_index = i;
    }

    public int getIndex() {
        return this.m_index;
    }

    public void setSelect(XPath xPath) {
        this.m_selectPattern = xPath;
    }

    public XPath getSelect() {
        return this.m_selectPattern;
    }

    public void setName(QName qName) {
        this.m_qname = qName;
    }

    public QName getName() {
        return this.m_qname;
    }

    public void setIsTopLevel(boolean z) {
        this.m_isTopLevel = z;
    }

    public boolean getIsTopLevel() {
        return this.m_isTopLevel;
    }

    @Override
    public int getXSLToken() {
        return 73;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_VARIABLE_STRING;
    }

    public ElemVariable(ElemVariable elemVariable) throws TransformerException {
        this.m_frameSize = -1;
        this.m_isTopLevel = false;
        this.m_selectPattern = elemVariable.m_selectPattern;
        this.m_qname = elemVariable.m_qname;
        this.m_isTopLevel = elemVariable.m_isTopLevel;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        transformerImpl.getXPathContext().getVarStack().setLocalVariable(this.m_index, getValue(transformerImpl, transformerImpl.getXPathContext().getCurrentNode()));
    }

    public XObject getValue(TransformerImpl transformerImpl, int i) throws TransformerException {
        int iTransformToRTF;
        XObject xRTreeFrag;
        XPathContext xPathContext = transformerImpl.getXPathContext();
        xPathContext.pushCurrentNode(i);
        try {
            if (this.m_selectPattern != null) {
                xRTreeFrag = this.m_selectPattern.execute(xPathContext, i, this);
                xRTreeFrag.allowDetachToRelease(false);
            } else if (getFirstChildElem() == null) {
                xRTreeFrag = XString.EMPTYSTRING;
            } else {
                if (this.m_parentNode instanceof Stylesheet) {
                    iTransformToRTF = transformerImpl.transformToGlobalRTF(this);
                } else {
                    iTransformToRTF = transformerImpl.transformToRTF(this);
                }
                xRTreeFrag = new XRTreeFrag(iTransformToRTF, xPathContext, this);
            }
            return xRTreeFrag;
        } finally {
            xPathContext.popCurrentNode();
        }
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        XPath xPathRewriteChildToExpression;
        if (this.m_selectPattern == null && stylesheetRoot.getOptimizer() && (xPathRewriteChildToExpression = rewriteChildToExpression(this)) != null) {
            this.m_selectPattern = xPathRewriteChildToExpression;
        }
        StylesheetRoot.ComposeState composeState = stylesheetRoot.getComposeState();
        Vector variableNames = composeState.getVariableNames();
        if (this.m_selectPattern != null) {
            this.m_selectPattern.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (!(this.m_parentNode instanceof Stylesheet) && this.m_qname != null) {
            this.m_index = composeState.addVariableName(this.m_qname) - composeState.getGlobalsSize();
        } else if (this.m_parentNode instanceof Stylesheet) {
            composeState.resetStackFrameSize();
        }
        super.compose(stylesheetRoot);
    }

    @Override
    public void endCompose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.endCompose(stylesheetRoot);
        if (this.m_parentNode instanceof Stylesheet) {
            StylesheetRoot.ComposeState composeState = stylesheetRoot.getComposeState();
            this.m_frameSize = composeState.getFrameSize();
            composeState.resetStackFrameSize();
        }
    }

    static XPath rewriteChildToExpression(ElemTemplateElement elemTemplateElement) throws TransformerException {
        ElemTemplateElement firstChildElem = elemTemplateElement.getFirstChildElem();
        if (firstChildElem != null && firstChildElem.getNextSiblingElem() == null) {
            int xSLToken = firstChildElem.getXSLToken();
            if (30 == xSLToken) {
                ElemValueOf elemValueOf = (ElemValueOf) firstChildElem;
                if (!elemValueOf.getDisableOutputEscaping() && elemValueOf.getDOMBackPointer() == null) {
                    elemTemplateElement.m_firstChild = null;
                    return new XPath(new XRTreeFragSelectWrapper(elemValueOf.getSelect().getExpression()));
                }
            } else if (78 == xSLToken) {
                ElemTextLiteral elemTextLiteral = (ElemTextLiteral) firstChildElem;
                if (!elemTextLiteral.getDisableOutputEscaping() && elemTextLiteral.getDOMBackPointer() == null) {
                    XString xString = new XString(elemTextLiteral.getNodeValue());
                    elemTemplateElement.m_firstChild = null;
                    return new XPath(new XRTreeFragSelectWrapper(xString));
                }
            }
        }
        return null;
    }

    @Override
    public void recompose(StylesheetRoot stylesheetRoot) {
        stylesheetRoot.recomposeVariables(this);
    }

    @Override
    public void setParentElem(ElemTemplateElement elemTemplateElement) {
        super.setParentElem(elemTemplateElement);
        elemTemplateElement.m_hasVariableDecl = true;
    }

    @Override
    protected boolean accept(XSLTVisitor xSLTVisitor) {
        return xSLTVisitor.visitVariableOrParamDecl(this);
    }

    @Override
    protected void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        if (this.m_selectPattern != null) {
            this.m_selectPattern.getExpression().callVisitors(this.m_selectPattern, xSLTVisitor);
        }
        super.callChildVisitors(xSLTVisitor, z);
    }

    public boolean isPsuedoVar() {
        String namespaceURI = this.m_qname.getNamespaceURI();
        if (namespaceURI != null && namespaceURI.equals("http://xml.apache.org/xalan/psuedovar") && this.m_qname.getLocalName().startsWith("#")) {
            return true;
        }
        return false;
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        if (this.m_selectPattern != null) {
            error(XSLTErrorResources.ER_CANT_HAVE_CONTENT_AND_SELECT, new Object[]{"xsl:" + getNodeName()});
            return null;
        }
        return super.appendChild(elemTemplateElement);
    }
}
