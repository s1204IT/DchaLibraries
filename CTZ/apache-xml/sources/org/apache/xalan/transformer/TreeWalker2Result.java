package org.apache.xalan.transformer;

import javax.xml.transform.TransformerException;
import org.apache.xalan.serialize.SerializerUtils;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.ref.DTMTreeWalker;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xpath.XPathContext;
import org.xml.sax.SAXException;

public class TreeWalker2Result extends DTMTreeWalker {
    SerializationHandler m_handler;
    int m_startNode;
    TransformerImpl m_transformer;

    public TreeWalker2Result(TransformerImpl transformerImpl, SerializationHandler serializationHandler) {
        super(serializationHandler, null);
        this.m_transformer = transformerImpl;
        this.m_handler = serializationHandler;
    }

    @Override
    public void traverse(int i) throws SAXException {
        this.m_dtm = this.m_transformer.getXPathContext().getDTM(i);
        this.m_startNode = i;
        super.traverse(i);
    }

    @Override
    protected void endNode(int i) throws SAXException {
        super.endNode(i);
        if (1 == this.m_dtm.getNodeType(i)) {
            this.m_transformer.getXPathContext().popCurrentNode();
        }
    }

    @Override
    protected void startNode(int i) throws SAXException {
        XPathContext xPathContext = this.m_transformer.getXPathContext();
        try {
            if (1 == this.m_dtm.getNodeType(i)) {
                xPathContext.pushCurrentNode(i);
                if (this.m_startNode != i) {
                    super.startNode(i);
                    return;
                }
                String nodeName = this.m_dtm.getNodeName(i);
                String localName = this.m_dtm.getLocalName(i);
                this.m_handler.startElement(this.m_dtm.getNamespaceURI(i), localName, nodeName);
                DTM dtm = this.m_dtm;
                int firstNamespaceNode = dtm.getFirstNamespaceNode(i, true);
                while (-1 != firstNamespaceNode) {
                    SerializerUtils.ensureNamespaceDeclDeclared(this.m_handler, dtm, firstNamespaceNode);
                    firstNamespaceNode = dtm.getNextNamespaceNode(i, firstNamespaceNode, true);
                }
                for (int firstAttribute = dtm.getFirstAttribute(i); -1 != firstAttribute; firstAttribute = dtm.getNextAttribute(firstAttribute)) {
                    SerializerUtils.addAttribute(this.m_handler, firstAttribute);
                }
                return;
            }
            xPathContext.pushCurrentNode(i);
            super.startNode(i);
            xPathContext.popCurrentNode();
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }
}
