package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.XML11Char;
import org.apache.xpath.XPathContext;
import org.xml.sax.SAXException;

public class ElemPI extends ElemTemplateElement {
    static final long serialVersionUID = 5621976448020889825L;
    private AVT m_name_atv = null;

    public void setName(AVT avt) {
        this.m_name_atv = avt;
    }

    public AVT getName() {
        return this.m_name_atv;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        Vector variableNames = stylesheetRoot.getComposeState().getVariableNames();
        if (this.m_name_atv != null) {
            this.m_name_atv.fixupVariables(variableNames, stylesheetRoot.getComposeState().getGlobalsSize());
        }
    }

    @Override
    public int getXSLToken() {
        return 58;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_PI_STRING;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        XPathContext xPathContext = transformerImpl.getXPathContext();
        String strEvaluate = this.m_name_atv == null ? null : this.m_name_atv.evaluate(xPathContext, xPathContext.getCurrentNode(), this);
        if (strEvaluate == null) {
            return;
        }
        if (strEvaluate.equalsIgnoreCase("xml")) {
            transformerImpl.getMsgMgr().warn(this, XSLTErrorResources.WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML, new Object[]{"name", strEvaluate});
            return;
        }
        if (!this.m_name_atv.isSimple() && !XML11Char.isXML11ValidNCName(strEvaluate)) {
            transformerImpl.getMsgMgr().warn(this, XSLTErrorResources.WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME, new Object[]{"name", strEvaluate});
            return;
        }
        try {
            transformerImpl.getResultTreeHandler().processingInstruction(strEvaluate, transformerImpl.transformToString(this));
        } catch (SAXException e) {
            throw new TransformerException(e);
        }
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        int xSLToken = elemTemplateElement.getXSLToken();
        if (xSLToken != 9 && xSLToken != 17 && xSLToken != 28 && xSLToken != 30 && xSLToken != 42 && xSLToken != 50 && xSLToken != 78) {
            switch (xSLToken) {
                case 35:
                case 36:
                case 37:
                    break;
                default:
                    switch (xSLToken) {
                        case Constants.ELEMNAME_APPLY_IMPORTS:
                        case Constants.ELEMNAME_VARIABLE:
                        case Constants.ELEMNAME_COPY_OF:
                        case Constants.ELEMNAME_MESSAGE:
                            break;
                        default:
                            error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{elemTemplateElement.getNodeName(), getNodeName()});
                            break;
                    }
                    break;
            }
        }
        return super.appendChild(elemTemplateElement);
    }
}
