package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.serializer.NamespaceMappings;
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
    protected String resolvePrefix(SerializationHandler rhandler, String prefix, String nodeNamespace) throws TransformerException {
        if (prefix == null) {
            return prefix;
        }
        if (prefix.length() == 0 || prefix.equals("xmlns")) {
            String prefix2 = rhandler.getPrefix(nodeNamespace);
            if (prefix2 == null || prefix2.length() == 0 || prefix2.equals("xmlns")) {
                if (nodeNamespace.length() > 0) {
                    NamespaceMappings prefixMapping = rhandler.getNamespaceMappings();
                    return prefixMapping.generateNextPrefix();
                }
                return "";
            }
            return prefix2;
        }
        return prefix;
    }

    protected boolean validateNodeName(String nodeName) {
        if (nodeName == null || nodeName.equals("xmlns")) {
            return false;
        }
        return XML11Char.isXML11ValidQName(nodeName);
    }

    @Override
    void constructNode(String nodeName, String prefix, String nodeNamespace, TransformerImpl transformer) throws TransformerException {
        if (nodeName != null && nodeName.length() > 0) {
            SerializationHandler rhandler = transformer.getSerializationHandler();
            String val = transformer.transformToString(this);
            try {
                String localName = QName.getLocalPart(nodeName);
                if (prefix != null && prefix.length() > 0) {
                    rhandler.addAttribute(nodeNamespace, localName, nodeName, "CDATA", val, true);
                } else {
                    rhandler.addAttribute("", localName, nodeName, "CDATA", val, true);
                }
            } catch (SAXException e) {
            }
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

    @Override
    public void setName(AVT v) {
        if (v.isSimple() && v.getSimpleString().equals("xmlns")) {
            throw new IllegalArgumentException();
        }
        super.setName(v);
    }
}
