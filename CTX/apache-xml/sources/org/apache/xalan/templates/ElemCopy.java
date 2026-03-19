package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.serialize.SerializerUtils;
import org.apache.xalan.transformer.ClonerToResultTree;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.dtm.DTM;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xpath.XPathContext;
import org.xml.sax.SAXException;

public class ElemCopy extends ElemUse {
    static final long serialVersionUID = 5478580783896941384L;

    @Override
    public int getXSLToken() {
        return 9;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_COPY_STRING;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        XPathContext xPathContext = transformerImpl.getXPathContext();
        try {
            try {
                int currentNode = xPathContext.getCurrentNode();
                xPathContext.pushCurrentNode(currentNode);
                DTM dtm = xPathContext.getDTM(currentNode);
                short nodeType = dtm.getNodeType(currentNode);
                if (9 != nodeType && 11 != nodeType) {
                    SerializationHandler serializationHandler = transformerImpl.getSerializationHandler();
                    ClonerToResultTree.cloneToResultTree(currentNode, nodeType, dtm, serializationHandler, false);
                    if (1 == nodeType) {
                        super.execute(transformerImpl);
                        SerializerUtils.processNSDecls(serializationHandler, currentNode, nodeType, dtm);
                        transformerImpl.executeChildTemplates((ElemTemplateElement) this, true);
                        transformerImpl.getResultTreeHandler().endElement(dtm.getNamespaceURI(currentNode), dtm.getLocalName(currentNode), dtm.getNodeName(currentNode));
                    }
                } else {
                    super.execute(transformerImpl);
                    transformerImpl.executeChildTemplates((ElemTemplateElement) this, true);
                }
            } catch (SAXException e) {
                throw new TransformerException(e);
            }
        } finally {
            xPathContext.popCurrentNode();
        }
    }
}
