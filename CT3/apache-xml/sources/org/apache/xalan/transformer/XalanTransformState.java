package org.apache.xalan.transformer;

import javax.xml.transform.Transformer;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.ref.DTMNodeIterator;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

public class XalanTransformState implements TransformState {
    Node m_node = null;
    ElemTemplateElement m_currentElement = null;
    ElemTemplate m_currentTemplate = null;
    ElemTemplate m_matchedTemplate = null;
    int m_currentNodeHandle = -1;
    Node m_currentNode = null;
    int m_matchedNode = -1;
    DTMIterator m_contextNodeList = null;
    boolean m_elemPending = false;
    TransformerImpl m_transformer = null;

    @Override
    public void setCurrentNode(Node n) {
        this.m_node = n;
    }

    @Override
    public void resetState(Transformer transformer) {
        if (transformer == 0 || !(transformer instanceof TransformerImpl)) {
            return;
        }
        this.m_transformer = transformer;
        this.m_currentElement = this.m_transformer.getCurrentElement();
        this.m_currentTemplate = this.m_transformer.getCurrentTemplate();
        this.m_matchedTemplate = this.m_transformer.getMatchedTemplate();
        int currentNodeHandle = this.m_transformer.getCurrentNode();
        DTM dtm = this.m_transformer.getXPathContext().getDTM(currentNodeHandle);
        this.m_currentNode = dtm.getNode(currentNodeHandle);
        this.m_matchedNode = this.m_transformer.getMatchedNode();
        this.m_contextNodeList = this.m_transformer.getContextNodeList();
    }

    @Override
    public ElemTemplateElement getCurrentElement() {
        if (this.m_elemPending) {
            return this.m_currentElement;
        }
        return this.m_transformer.getCurrentElement();
    }

    @Override
    public Node getCurrentNode() {
        if (this.m_currentNode != null) {
            return this.m_currentNode;
        }
        DTM dtm = this.m_transformer.getXPathContext().getDTM(this.m_transformer.getCurrentNode());
        return dtm.getNode(this.m_transformer.getCurrentNode());
    }

    @Override
    public ElemTemplate getCurrentTemplate() {
        if (this.m_elemPending) {
            return this.m_currentTemplate;
        }
        return this.m_transformer.getCurrentTemplate();
    }

    @Override
    public ElemTemplate getMatchedTemplate() {
        if (this.m_elemPending) {
            return this.m_matchedTemplate;
        }
        return this.m_transformer.getMatchedTemplate();
    }

    @Override
    public Node getMatchedNode() {
        if (this.m_elemPending) {
            DTM dtm = this.m_transformer.getXPathContext().getDTM(this.m_matchedNode);
            return dtm.getNode(this.m_matchedNode);
        }
        DTM dtm2 = this.m_transformer.getXPathContext().getDTM(this.m_transformer.getMatchedNode());
        return dtm2.getNode(this.m_transformer.getMatchedNode());
    }

    @Override
    public NodeIterator getContextNodeList() {
        if (this.m_elemPending) {
            return new DTMNodeIterator(this.m_contextNodeList);
        }
        return new DTMNodeIterator(this.m_transformer.getContextNodeList());
    }

    @Override
    public Transformer getTransformer() {
        return this.m_transformer;
    }
}
