package org.apache.xpath.objects;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMManager;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.XPathContext;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

public class XNodeSetForDOM extends XNodeSet {
    static final long serialVersionUID = -8396190713754624640L;
    Object m_origObj;

    public XNodeSetForDOM(Node node, DTMManager dTMManager) {
        this.m_dtmMgr = dTMManager;
        this.m_origObj = node;
        int dTMHandleFromNode = dTMManager.getDTMHandleFromNode(node);
        setObject(new NodeSetDTM(dTMManager));
        ((NodeSetDTM) this.m_obj).addNode(dTMHandleFromNode);
    }

    public XNodeSetForDOM(XNodeSet xNodeSet) {
        super(xNodeSet);
        if (xNodeSet instanceof XNodeSetForDOM) {
            this.m_origObj = ((XNodeSetForDOM) xNodeSet).m_origObj;
        }
    }

    public XNodeSetForDOM(NodeList nodeList, XPathContext xPathContext) {
        this.m_dtmMgr = xPathContext.getDTMManager();
        this.m_origObj = nodeList;
        NodeSetDTM nodeSetDTM = new NodeSetDTM(nodeList, xPathContext);
        this.m_last = nodeSetDTM.getLength();
        setObject(nodeSetDTM);
    }

    public XNodeSetForDOM(NodeIterator nodeIterator, XPathContext xPathContext) {
        this.m_dtmMgr = xPathContext.getDTMManager();
        this.m_origObj = nodeIterator;
        NodeSetDTM nodeSetDTM = new NodeSetDTM(nodeIterator, xPathContext);
        this.m_last = nodeSetDTM.getLength();
        setObject(nodeSetDTM);
    }

    @Override
    public Object object() {
        return this.m_origObj;
    }

    @Override
    public NodeIterator nodeset() throws TransformerException {
        return this.m_origObj instanceof NodeIterator ? (NodeIterator) this.m_origObj : super.nodeset();
    }

    @Override
    public NodeList nodelist() throws TransformerException {
        return this.m_origObj instanceof NodeList ? (NodeList) this.m_origObj : super.nodelist();
    }
}
