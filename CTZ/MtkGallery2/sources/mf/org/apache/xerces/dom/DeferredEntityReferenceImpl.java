package mf.org.apache.xerces.dom;

public class DeferredEntityReferenceImpl extends EntityReferenceImpl implements DeferredNode {
    static final long serialVersionUID = 390319091370032223L;
    protected transient int fNodeIndex;

    DeferredEntityReferenceImpl(DeferredDocumentImpl ownerDocument, int nodeIndex) {
        super(ownerDocument, null);
        this.fNodeIndex = nodeIndex;
        needsSyncData(true);
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
        this.baseURI = ownerDocument.getNodeValue(this.fNodeIndex);
    }

    @Override
    protected void synchronizeChildren() {
        needsSyncChildren(false);
        isReadOnly(false);
        DeferredDocumentImpl ownerDocument = (DeferredDocumentImpl) ownerDocument();
        ownerDocument.synchronizeChildren(this, this.fNodeIndex);
        setReadOnly(true, true);
    }
}
