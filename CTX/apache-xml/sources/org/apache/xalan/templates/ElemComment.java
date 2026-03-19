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
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        try {
            transformerImpl.getResultTreeHandler().comment(transformerImpl.transformToString(this));
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
