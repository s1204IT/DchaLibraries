package org.apache.xalan.serialize;

import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.dtm.DTM;
import org.apache.xml.serializer.NamespaceMappings;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.xml.sax.SAXException;

public class SerializerUtils {
    public static void addAttribute(SerializationHandler serializationHandler, int i) throws TransformerException {
        DTM dtm = ((TransformerImpl) serializationHandler.getTransformer()).getXPathContext().getDTM(i);
        if (isDefinedNSDecl(serializationHandler, i, dtm)) {
            return;
        }
        String namespaceURI = dtm.getNamespaceURI(i);
        if (namespaceURI == null) {
            namespaceURI = "";
        }
        try {
            serializationHandler.addAttribute(namespaceURI, dtm.getLocalName(i), dtm.getNodeName(i), "CDATA", dtm.getNodeValue(i), false);
        } catch (SAXException e) {
        }
    }

    public static void addAttributes(SerializationHandler serializationHandler, int i) throws TransformerException {
        DTM dtm = ((TransformerImpl) serializationHandler.getTransformer()).getXPathContext().getDTM(i);
        for (int firstAttribute = dtm.getFirstAttribute(i); -1 != firstAttribute; firstAttribute = dtm.getNextAttribute(firstAttribute)) {
            addAttribute(serializationHandler, firstAttribute);
        }
    }

    public static void outputResultTreeFragment(SerializationHandler serializationHandler, XObject xObject, XPathContext xPathContext) throws SAXException {
        int iRtf = xObject.rtf();
        DTM dtm = xPathContext.getDTM(iRtf);
        if (dtm != null) {
            for (int firstChild = dtm.getFirstChild(iRtf); -1 != firstChild; firstChild = dtm.getNextSibling(firstChild)) {
                serializationHandler.flushPending();
                if (dtm.getNodeType(firstChild) == 1 && dtm.getNamespaceURI(firstChild) == null) {
                    serializationHandler.startPrefixMapping("", "");
                }
                dtm.dispatchToEvents(firstChild, serializationHandler);
            }
        }
    }

    public static void processNSDecls(SerializationHandler serializationHandler, int i, int i2, DTM dtm) throws TransformerException {
        try {
            if (i2 == 1) {
                int firstNamespaceNode = dtm.getFirstNamespaceNode(i, true);
                while (-1 != firstNamespaceNode) {
                    String nodeNameX = dtm.getNodeNameX(firstNamespaceNode);
                    String namespaceURIFromPrefix = serializationHandler.getNamespaceURIFromPrefix(nodeNameX);
                    String nodeValue = dtm.getNodeValue(firstNamespaceNode);
                    if (!nodeValue.equalsIgnoreCase(namespaceURIFromPrefix)) {
                        serializationHandler.startPrefixMapping(nodeNameX, nodeValue, false);
                    }
                    firstNamespaceNode = dtm.getNextNamespaceNode(i, firstNamespaceNode, true);
                }
                return;
            }
            if (i2 == 13) {
                String nodeNameX2 = dtm.getNodeNameX(i);
                String namespaceURIFromPrefix2 = serializationHandler.getNamespaceURIFromPrefix(nodeNameX2);
                String nodeValue2 = dtm.getNodeValue(i);
                if (!nodeValue2.equalsIgnoreCase(namespaceURIFromPrefix2)) {
                    serializationHandler.startPrefixMapping(nodeNameX2, nodeValue2, false);
                }
            }
        } catch (SAXException e) {
            throw new TransformerException(e);
        }
    }

    public static boolean isDefinedNSDecl(SerializationHandler serializationHandler, int i, DTM dtm) {
        String namespaceURIFromPrefix;
        if (13 == dtm.getNodeType(i) && (namespaceURIFromPrefix = serializationHandler.getNamespaceURIFromPrefix(dtm.getNodeNameX(i))) != null && namespaceURIFromPrefix.equals(dtm.getStringValue(i))) {
            return true;
        }
        return false;
    }

    public static void ensureNamespaceDeclDeclared(SerializationHandler serializationHandler, DTM dtm, int i) throws SAXException {
        NamespaceMappings namespaceMappings;
        String nodeValue = dtm.getNodeValue(i);
        String nodeNameX = dtm.getNodeNameX(i);
        if (nodeValue != null && nodeValue.length() > 0 && nodeNameX != null && (namespaceMappings = serializationHandler.getNamespaceMappings()) != null) {
            String strLookupNamespace = namespaceMappings.lookupNamespace(nodeNameX);
            if (strLookupNamespace == null || !strLookupNamespace.equals(nodeValue)) {
                serializationHandler.startPrefixMapping(nodeNameX, nodeValue, false);
            }
        }
    }
}
