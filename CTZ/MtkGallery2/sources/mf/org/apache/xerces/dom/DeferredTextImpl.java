package mf.org.apache.xerces.dom;

public class DeferredTextImpl extends TextImpl implements DeferredNode {
    static final long serialVersionUID = 2310613872100393425L;
    protected transient int fNodeIndex;

    DeferredTextImpl(DeferredDocumentImpl ownerDocument, int nodeIndex) {
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
        DeferredDocumentImpl ownerDocument = (DeferredDocumentImpl) ownerDocument();
        this.data = ownerDocument.getNodeValueString(this.fNodeIndex);
        isIgnorableWhitespace(ownerDocument.getNodeExtra(this.fNodeIndex) == 1);
    }
}
