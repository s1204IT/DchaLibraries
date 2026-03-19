package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.xml.sax.SAXException;

public class ElemComment extends ElemTemplateElement {
    static final long serialVersionUID = -8813199122875770142L;

    @Override
    public int getXSLToken() {
        return 59;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_COMMENT_STRING;
    }

    @Override
    public void execute(TransformerImpl transformer) throws TransformerException {
        try {
            String data = transformer.transformToString(this);
            transformer.getResultTreeHandler().comment(data);
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
