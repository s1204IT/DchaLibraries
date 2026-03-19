package mf.org.apache.xerces.dom;

public class DeferredDocumentTypeImpl extends DocumentTypeImpl implements DeferredNode {
    static final long serialVersionUID = -2172579663227313509L;
    protected transient int fNodeIndex;

    DeferredDocumentTypeImpl(DeferredDocumentImpl ownerDocument, int nodeIndex) {
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
        this.publicID = ownerDocument.getNodeValue(this.fNodeIndex);
        this.systemID = ownerDocument.getNodeURI(this.fNodeIndex);
        int extraDataIndex = ownerDocument.getNodeExtra(this.fNodeIndex);
        this.internalSubset = ownerDocument.getNodeValue(extraDataIndex);
    }

    @Override
    protected void synchronizeChildren() {
        boolean orig = ownerDocument().getMutationEvents();
        ownerDocument().setMutationEvents(false);
        needsSyncChildren(false);
        DeferredDocumentImpl ownerDocument = (DeferredDocumentImpl) this.ownerDocument;
        this.entities = new NamedNodeMapImpl(this);
        this.notations = new NamedNodeMapImpl(this);
        this.elements = new NamedNodeMapImpl(this);
        DeferredNode last = null;
        for (int index = ownerDocument.getLastChild(this.fNodeIndex); index != -1; index = ownerDocument.getPrevSibling(index)) {
            DeferredNode node = ownerDocument.getNodeObject(index);
            int type = node.getNodeType();
            if (type != 1) {
                if (type == 6) {
                    this.entities.setNamedItem(node);
                } else if (type == 12) {
                    this.notations.setNamedItem(node);
                } else if (type == 21) {
                    this.elements.setNamedItem(node);
                } else {
                    System.out.println("DeferredDocumentTypeImpl#synchronizeInfo: node.getNodeType() = " + ((int) node.getNodeType()) + ", class = " + node.getClass().getName());
                }
            } else if (((DocumentImpl) getOwnerDocument()).allowGrammarAccess) {
                insertBefore(node, last);
                last = node;
            }
        }
        ownerDocument().setMutationEvents(orig);
        setReadOnly(true, false);
    }
}
