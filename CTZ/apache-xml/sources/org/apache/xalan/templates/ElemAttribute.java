package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.XML11Char;
import org.xml.sax.SAXException;

public class ElemAttribute extends ElemElement {
    static final long serialVersionUID = 8817220961566919187L;

    @Override
    public int getXSLToken() {
        return 48;
    }

    @Override
    public String getNodeName() {
        return "attribute";
    }

    @Override
    protected String resolvePrefix(SerializationHandler serializationHandler, String str, String str2) throws TransformerException {
        if (str == null) {
            return str;
        }
        if (str.length() == 0 || str.equals("xmlns")) {
            String prefix = serializationHandler.getPrefix(str2);
            if (prefix == null || prefix.length() == 0 || prefix.equals("xmlns")) {
                if (str2.length() > 0) {
                    return serializationHandler.getNamespaceMappings().generateNextPrefix();
                }
                return "";
            }
            return prefix;
        }
        return str;
    }

    protected boolean validateNodeName(String str) {
        if (str == null || str.equals("xmlns")) {
            return false;
        }
        return XML11Char.isXML11ValidQName(str);
    }

    @Override
    void constructNode(String str, String str2, String str3, TransformerImpl transformerImpl) throws TransformerException {
        if (str != null && str.length() > 0) {
            SerializationHandler serializationHandler = transformerImpl.getSerializationHandler();
            String strTransformToString = transformerImpl.transformToString(this);
            try {
                String localPart = QName.getLocalPart(str);
                if (str2 != null && str2.length() > 0) {
                    serializationHandler.addAttribute(str3, localPart, str, "CDATA", strTransformToString, true);
                } else {
                    serializationHandler.addAttribute("", localPart, str, "CDATA", strTransformToString, true);
                }
            } catch (SAXException e) {
            }
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

    @Override
    public void setName(AVT avt) {
        if (avt.isSimple() && avt.getSimpleString().equals("xmlns")) {
            throw new IllegalArgumentException();
        }
        super.setName(avt);
    }
}
