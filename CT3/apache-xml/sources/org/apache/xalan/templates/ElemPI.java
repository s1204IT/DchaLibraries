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

    public void setName(AVT v) {
        this.m_name_atv = v;
    }

    public AVT getName() {
        return this.m_name_atv;
    }

    @Override
    public void compose(StylesheetRoot sroot) throws TransformerException {
        super.compose(sroot);
        Vector vnames = sroot.getComposeState().getVariableNames();
        if (this.m_name_atv == null) {
            return;
        }
        this.m_name_atv.fixupVariables(vnames, sroot.getComposeState().getGlobalsSize());
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
    public void execute(TransformerImpl transformer) throws TransformerException {
        XPathContext xctxt = transformer.getXPathContext();
        int sourceNode = xctxt.getCurrentNode();
        String piName = this.m_name_atv != null ? this.m_name_atv.evaluate(xctxt, sourceNode, this) : null;
        if (piName == null) {
            return;
        }
        if (piName.equalsIgnoreCase("xml")) {
            transformer.getMsgMgr().warn(this, XSLTErrorResources.WG_PROCESSINGINSTRUCTION_NAME_CANT_BE_XML, new Object[]{"name", piName});
            return;
        }
        if (!this.m_name_atv.isSimple() && !XML11Char.isXML11ValidNCName(piName)) {
            transformer.getMsgMgr().warn(this, XSLTErrorResources.WG_PROCESSINGINSTRUCTION_NOTVALID_NCNAME, new Object[]{"name", piName});
            return;
        }
        String data = transformer.transformToString(this);
        try {
            transformer.getResultTreeHandler().processingInstruction(piName, data);
        } catch (SAXException se) {
            throw new TransformerException(se);
        }
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement newChild) {
        int type = newChild.getXSLToken();
        switch (type) {
            case 9:
            case 17:
            case 28:
            case 30:
            case 35:
            case 36:
            case 37:
            case 42:
            case 50:
            case Constants.ELEMNAME_APPLY_IMPORTS:
            case Constants.ELEMNAME_VARIABLE:
            case Constants.ELEMNAME_COPY_OF:
            case Constants.ELEMNAME_MESSAGE:
            case Constants.ELEMNAME_TEXTLITERALRESULT:
                break;
            default:
                error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{newChild.getNodeName(), getNodeName()});
                break;
        }
        return super.appendChild(newChild);
    }
}
