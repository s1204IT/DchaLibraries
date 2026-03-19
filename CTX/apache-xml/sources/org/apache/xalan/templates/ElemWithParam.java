package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.QName;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XRTreeFrag;
import org.apache.xpath.objects.XString;

public class ElemWithParam extends ElemTemplateElement {
    static final long serialVersionUID = -1070355175864326257L;
    int m_index;
    int m_qnameID;
    private XPath m_selectPattern = null;
    private QName m_qname = null;

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

    @Override
    public int getXSLToken() {
        return 2;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_WITHPARAM_STRING;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        XPath xPathRewriteChildToExpression;
        if (this.m_selectPattern == null && stylesheetRoot.getOptimizer() && (xPathRewriteChildToExpression = ElemVariable.rewriteChildToExpression(this)) != null) {
            this.m_selectPattern = xPathRewriteChildToExpression;
        }
        this.m_qnameID = stylesheetRoot.getComposeState().getQNameID(this.m_qname);
        super.compose(stylesheetRoot);
        Vector variableNames = stylesheetRoot.getComposeState().getVariableNames();
        if (this.m_selectPattern != null) {
            this.m_selectPattern.fixupVariables(variableNames, stylesheetRoot.getComposeState().getGlobalsSize());
        }
    }

    @Override
    public void setParentElem(ElemTemplateElement elemTemplateElement) {
        super.setParentElem(elemTemplateElement);
        elemTemplateElement.m_hasVariableDecl = true;
    }

    public XObject getValue(TransformerImpl transformerImpl, int i) throws TransformerException {
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
                xRTreeFrag = new XRTreeFrag(transformerImpl.transformToRTF(this), xPathContext, this);
            }
            return xRTreeFrag;
        } finally {
            xPathContext.popCurrentNode();
        }
    }

    @Override
    protected void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        if (z && this.m_selectPattern != null) {
            this.m_selectPattern.getExpression().callVisitors(this.m_selectPattern, xSLTVisitor);
        }
        super.callChildVisitors(xSLTVisitor, z);
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
