package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.XML11Char;
import org.apache.xpath.XPathContext;
import org.xml.sax.SAXException;

public class ElemElement extends ElemUse {
    static final long serialVersionUID = -324619535592435183L;
    protected AVT m_name_avt = null;
    protected AVT m_namespace_avt = null;

    public void setName(AVT avt) {
        this.m_name_avt = avt;
    }

    public AVT getName() {
        return this.m_name_avt;
    }

    public void setNamespace(AVT avt) {
        this.m_namespace_avt = avt;
    }

    public AVT getNamespace() {
        return this.m_namespace_avt;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        StylesheetRoot.ComposeState composeState = stylesheetRoot.getComposeState();
        Vector variableNames = composeState.getVariableNames();
        if (this.m_name_avt != null) {
            this.m_name_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
        if (this.m_namespace_avt != null) {
            this.m_namespace_avt.fixupVariables(variableNames, composeState.getGlobalsSize());
        }
    }

    @Override
    public int getXSLToken() {
        return 46;
    }

    @Override
    public String getNodeName() {
        return "element";
    }

    protected String resolvePrefix(SerializationHandler serializationHandler, String str, String str2) throws TransformerException {
        return str;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        String strEvaluate;
        String prefixPart;
        String localPart;
        SerializationHandler serializationHandler = transformerImpl.getSerializationHandler();
        XPathContext xPathContext = transformerImpl.getXPathContext();
        int currentNode = xPathContext.getCurrentNode();
        String str = null;
        if (this.m_name_avt != null) {
            strEvaluate = this.m_name_avt.evaluate(xPathContext, currentNode, this);
        } else {
            strEvaluate = null;
        }
        String strEvaluate2 = "";
        if (strEvaluate != null && !this.m_name_avt.isSimple() && !XML11Char.isXML11ValidQName(strEvaluate)) {
            transformerImpl.getMsgMgr().warn(this, XSLTErrorResources.WG_ILLEGAL_ATTRIBUTE_VALUE, new Object[]{"name", strEvaluate});
            prefixPart = null;
        } else {
            if (strEvaluate != null) {
                prefixPart = QName.getPrefixPart(strEvaluate);
                if (this.m_namespace_avt != null) {
                    strEvaluate2 = this.m_namespace_avt.evaluate(xPathContext, currentNode, this);
                    if (strEvaluate2 == null || (prefixPart != null && prefixPart.length() > 0 && strEvaluate2.length() == 0)) {
                        transformerImpl.getMsgMgr().error(this, XSLTErrorResources.ER_NULL_URI_NAMESPACE);
                    } else {
                        String strResolvePrefix = resolvePrefix(serializationHandler, prefixPart, strEvaluate2);
                        if (strResolvePrefix == null) {
                            strResolvePrefix = "";
                        }
                        String str2 = strResolvePrefix;
                        if (str2.length() > 0) {
                            localPart = str2 + ":" + QName.getLocalPart(strEvaluate);
                        } else {
                            localPart = QName.getLocalPart(strEvaluate);
                        }
                        prefixPart = str2;
                        str = localPart;
                    }
                } else {
                    try {
                        String namespaceForPrefix = getNamespaceForPrefix(prefixPart);
                        if (namespaceForPrefix == null) {
                            try {
                                if (prefixPart.length() == 0) {
                                    strEvaluate2 = "";
                                } else if (namespaceForPrefix == null) {
                                    transformerImpl.getMsgMgr().warn(this, XSLTErrorResources.WG_COULD_NOT_RESOLVE_PREFIX, new Object[]{prefixPart});
                                    strEvaluate2 = namespaceForPrefix;
                                } else {
                                    strEvaluate2 = namespaceForPrefix;
                                }
                                str = strEvaluate;
                            } catch (Exception e) {
                                strEvaluate2 = namespaceForPrefix;
                                transformerImpl.getMsgMgr().warn(this, XSLTErrorResources.WG_COULD_NOT_RESOLVE_PREFIX, new Object[]{prefixPart});
                            }
                        }
                    } catch (Exception e2) {
                    }
                }
            } else {
                prefixPart = null;
            }
            str = strEvaluate;
        }
        constructNode(str, prefixPart, strEvaluate2, transformerImpl);
    }

    void constructNode(String str, String str2, String str3, TransformerImpl transformerImpl) throws TransformerException {
        try {
            SerializationHandler resultTreeHandler = transformerImpl.getResultTreeHandler();
            boolean z = true;
            if (str == null) {
                z = false;
            } else {
                if (str2 != null) {
                    resultTreeHandler.startPrefixMapping(str2, str3, true);
                }
                resultTreeHandler.startElement(str3, QName.getLocalPart(str), str);
                super.execute(transformerImpl);
            }
            transformerImpl.executeChildTemplates(this, z);
            if (str != null) {
                resultTreeHandler.endElement(str3, QName.getLocalPart(str), str);
                if (str2 != null) {
                    resultTreeHandler.endPrefixMapping(str2);
                }
            }
        } catch (SAXException e) {
            throw new TransformerException(e);
        }
    }

    @Override
    protected void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        if (z) {
            if (this.m_name_avt != null) {
                this.m_name_avt.callVisitors(xSLTVisitor);
            }
            if (this.m_namespace_avt != null) {
                this.m_namespace_avt.callVisitors(xSLTVisitor);
            }
        }
        super.callChildVisitors(xSLTVisitor, z);
    }
}
