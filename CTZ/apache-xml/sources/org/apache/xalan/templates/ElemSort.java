package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xpath.XPath;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

public class ElemSort extends ElemTemplateElement {
    static final long serialVersionUID = -4991510257335851938L;
    private XPath m_selectExpression = null;
    private AVT m_lang_avt = null;
    private AVT m_dataType_avt = null;
    private AVT m_order_avt = null;
    private AVT m_caseorder_avt = null;

    public void setSelect(XPath xPath) {
        if (xPath.getPatternString().indexOf("{") < 0) {
            this.m_selectExpression = xPath;
        } else {
            error(XSLTErrorResources.ER_NO_CURLYBRACE, null);
        }
    }

    public XPath getSelect() {
        return this.m_selectExpression;
    }

    public void setLang(AVT avt) {
        this.m_lang_avt = avt;
    }

    public AVT getLang() {
        return this.m_lang_avt;
    }

    public void setDataType(AVT avt) {
        this.m_dataType_avt = avt;
    }

    public AVT getDataType() {
        return this.m_dataType_avt;
    }

    public void setOrder(AVT avt) {
        this.m_order_avt = avt;
    }

    public AVT getOrder() {
        return this.m_order_avt;
    }

    public void setCaseOrder(AVT avt) {
        this.m_caseorder_avt = avt;
    }

    public AVT getCaseOrder() {
        return this.m_caseorder_avt;
    }

    @Override
    public int getXSLToken() {
        return 64;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_SORT_STRING;
    }

    @Override
    public Node appendChild(Node node) throws DOMException {
        error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{node.getNodeName(), getNodeName()});
        return null;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        StylesheetRoot.ComposeState composeState = stylesheetRoot.getComposeState();
        Vector variableNames = composeState.getVariableNames();
        if (this.m_caseorder_avt != null) {
            this.m_caseorder_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_dataType_avt != null) {
            this.m_dataType_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_lang_avt != null) {
            this.m_lang_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_order_avt != null) {
            this.m_order_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_selectExpression != null) {
            this.m_selectExpression.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
    }
}
