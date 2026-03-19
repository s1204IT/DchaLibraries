package mf.org.apache.xerces.dom;

import mf.org.w3c.dom.Node;

public class DeferredElementDefinitionImpl extends ElementDefinitionImpl implements DeferredNode {
    static final long serialVersionUID = 6703238199538041591L;
    protected transient int fNodeIndex;

    DeferredElementDefinitionImpl(DeferredDocumentImpl ownerDocument, int nodeIndex) {
        super(ownerDocument, null);
        this.fNodeIndex = nodeIndex;
        needsSyncData(true);
        needsSyncChildren(true);
    }

    @Override
    public int getNodeIndex() {
        return this.fNodeIndex;
    }

    @Override
    protected void synchronizeData() {
        needsSyncData(false);
        DeferredDocumentImpl ownerDocument = (DeferredDocumentImpl) this.ownerDocument;
        this.name = ownerDocument.getNodeName(this.fNodeIndex);
    }

    @Override
    protected void synchronizeChildren() {
        boolean orig = this.ownerDocument.getMutationEvents();
        this.ownerDocument.setMutationEvents(false);
        needsSyncChildren(false);
        DeferredDocumentImpl ownerDocument = (DeferredDocumentImpl) this.ownerDocument;
        this.attributes = new NamedNodeMapImpl(ownerDocument);
        for (int nodeIndex = ownerDocument.getLastChild(this.fNodeIndex); nodeIndex != -1; nodeIndex = ownerDocument.getPrevSibling(nodeIndex)) {
            Node attr = ownerDocument.getNodeObject(nodeIndex);
            this.attributes.setNamedItem(attr);
        }
        ownerDocument.setMutationEvents(orig);
    }
}
