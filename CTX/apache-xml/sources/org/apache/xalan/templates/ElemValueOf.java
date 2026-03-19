package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.xml.sax.SAXException;

public class ElemValueOf extends ElemTemplateElement {
    static final long serialVersionUID = 3490728458007586786L;
    private XPath m_selectExpression = null;
    private boolean m_isDot = false;
    private boolean m_disableOutputEscaping = false;

    public void setSelect(XPath xPath) {
        if (xPath != null) {
            String patternString = xPath.getPatternString();
            this.m_isDot = patternString != null && patternString.equals(Constants.ATTRVAL_THIS);
        }
        this.m_selectExpression = xPath;
    }

    public XPath getSelect() {
        return this.m_selectExpression;
    }

    public void setDisableOutputEscaping(boolean z) {
        this.m_disableOutputEscaping = z;
    }

    public boolean getDisableOutputEscaping() {
        return this.m_disableOutputEscaping;
    }

    @Override
    public int getXSLToken() {
        return 30;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        Vector variableNames = stylesheetRoot.getComposeState().getVariableNames();
        if (this.m_selectExpression != null) {
            this.m_selectExpression.fixupVariables(variableNames, stylesheetRoot.getComposeState().getGlobalsSize());
        }
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_VALUEOF_STRING;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        XPathContext xPathContext = transformerImpl.getXPathContext();
        SerializationHandler resultTreeHandler = transformerImpl.getResultTreeHandler();
        try {
            xPathContext.pushNamespaceContext(this);
            int currentNode = xPathContext.getCurrentNode();
            xPathContext.pushCurrentNodeAndExpression(currentNode, currentNode);
            if (this.m_disableOutputEscaping) {
                resultTreeHandler.processingInstruction("javax.xml.transform.disable-output-escaping", "");
            }
            try {
                this.m_selectExpression.getExpression().executeCharsToContentHandler(xPathContext, resultTreeHandler);
            } finally {
                if (this.m_disableOutputEscaping) {
                    resultTreeHandler.processingInstruction("javax.xml.transform.enable-output-escaping", "");
                }
                xPathContext.popNamespaceContext();
                xPathContext.popCurrentNodeAndExpression();
            }
        } catch (RuntimeException e) {
            TransformerException transformerException = new TransformerException(e);
            transformerException.setLocator(this);
            throw transformerException;
        } catch (SAXException e2) {
            throw new TransformerException(e2);
        }
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{elemTemplateElement.getNodeName(), getNodeName()});
        return null;
    }

    @Override
    protected void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        if (z) {
            this.m_selectExpression.getExpression().callVisitors(this.m_selectExpression, xSLTVisitor);
        }
        super.callChildVisitors(xSLTVisitor, z);
    }
}
