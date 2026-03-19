package org.apache.xalan.transformer;

import javax.xml.transform.TransformerException;
import org.apache.xalan.serialize.SerializerUtils;
import org.apache.xml.dtm.DTM;
import org.apache.xml.serializer.SerializationHandler;
import org.xml.sax.SAXException;

public class ClonerToResultTree {
    public static void cloneToResultTree(int i, int i2, DTM dtm, SerializationHandler serializationHandler, boolean z) throws TransformerException {
        try {
            switch (i2) {
                case 1:
                    String namespaceURI = dtm.getNamespaceURI(i);
                    if (namespaceURI == null) {
                        namespaceURI = "";
                    }
                    serializationHandler.startElement(namespaceURI, dtm.getLocalName(i), dtm.getNodeNameX(i));
                    if (z) {
                        SerializerUtils.addAttributes(serializationHandler, i);
                        SerializerUtils.processNSDecls(serializationHandler, i, i2, dtm);
                        return;
                    }
                    return;
                case 2:
                    SerializerUtils.addAttribute(serializationHandler, i);
                    return;
                case 3:
                    dtm.dispatchCharactersEvents(i, serializationHandler, false);
                    return;
                case 4:
                    serializationHandler.startCDATA();
                    dtm.dispatchCharactersEvents(i, serializationHandler, false);
                    serializationHandler.endCDATA();
                    return;
                case 5:
                    serializationHandler.entityReference(dtm.getNodeNameX(i));
                    return;
                case 6:
                case 10:
                case 12:
                default:
                    throw new TransformerException("Can't clone node: " + dtm.getNodeName(i));
                case 7:
                    serializationHandler.processingInstruction(dtm.getNodeNameX(i), dtm.getNodeValue(i));
                    return;
                case 8:
                    dtm.getStringValue(i).dispatchAsComment(serializationHandler);
                    return;
                case 9:
                case 11:
                    return;
                case 13:
                    SerializerUtils.processNSDecls(serializationHandler, i, 13, dtm);
                    return;
            }
        } catch (SAXException e) {
            throw new TransformerException(e);
        }
    }
}
