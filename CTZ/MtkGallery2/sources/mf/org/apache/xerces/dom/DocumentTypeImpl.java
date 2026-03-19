package mf.org.apache.xerces.dom;

import java.util.Hashtable;
import mf.org.apache.xerces.dom.ParentNode;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.DocumentType;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.UserDataHandler;

public class DocumentTypeImpl extends ParentNode implements DocumentType {
    static final long serialVersionUID = 7751299192316526485L;
    private int doctypeNumber;
    protected NamedNodeMapImpl elements;
    protected NamedNodeMapImpl entities;
    protected String internalSubset;
    protected String name;
    protected NamedNodeMapImpl notations;
    protected String publicID;
    protected String systemID;
    private Hashtable userData;

    public DocumentTypeImpl(CoreDocumentImpl ownerDocument, String name) {
        super(ownerDocument);
        this.doctypeNumber = 0;
        this.userData = null;
        this.name = name;
        this.entities = new NamedNodeMapImpl(this);
        this.notations = new NamedNodeMapImpl(this);
        this.elements = new NamedNodeMapImpl(this);
    }

    public DocumentTypeImpl(CoreDocumentImpl ownerDocument, String qualifiedName, String publicID, String systemID) {
        this(ownerDocument, qualifiedName);
        this.publicID = publicID;
        this.systemID = systemID;
    }

    @Override
    public String getPublicId() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.publicID;
    }

    @Override
    public String getSystemId() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.systemID;
    }

    public void setInternalSubset(String internalSubset) {
        if (needsSyncData()) {
            synchronizeData();
        }
        this.internalSubset = internalSubset;
    }

    @Override
    public String getInternalSubset() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.internalSubset;
    }

    @Override
    public short getNodeType() {
        return (short) 10;
    }

    @Override
    public String getNodeName() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.name;
    }

    @Override
    public Node cloneNode(boolean deep) {
        DocumentTypeImpl newnode = (DocumentTypeImpl) super.cloneNode(deep);
        newnode.entities = this.entities.cloneMap(newnode);
        newnode.notations = this.notations.cloneMap(newnode);
        newnode.elements = this.elements.cloneMap(newnode);
        return newnode;
    }

    @Override
    public String getTextContent() throws DOMException {
        return null;
    }

    @Override
    public void setTextContent(String textContent) throws DOMException {
    }

    @Override
    public boolean isEqualNode(Node arg) {
        if (!super.isEqualNode(arg)) {
            return false;
        }
        if (needsSyncData()) {
            synchronizeData();
        }
        DocumentTypeImpl argDocType = (DocumentTypeImpl) arg;
        if ((getPublicId() == null && argDocType.getPublicId() != null) || ((getPublicId() != null && argDocType.getPublicId() == null) || ((getSystemId() == null && argDocType.getSystemId() != null) || ((getSystemId() != null && argDocType.getSystemId() == null) || ((getInternalSubset() == null && argDocType.getInternalSubset() != null) || (getInternalSubset() != null && argDocType.getInternalSubset() == null)))))) {
            return false;
        }
        if (getPublicId() != null && !getPublicId().equals(argDocType.getPublicId())) {
            return false;
        }
        if (getSystemId() != null && !getSystemId().equals(argDocType.getSystemId())) {
            return false;
        }
        if (getInternalSubset() != null && !getInternalSubset().equals(argDocType.getInternalSubset())) {
            return false;
        }
        NamedNodeMapImpl argEntities = argDocType.entities;
        if ((this.entities == null && argEntities != null) || (this.entities != null && argEntities == null)) {
            return false;
        }
        if (this.entities != null && argEntities != null) {
            if (this.entities.getLength() != argEntities.getLength()) {
                return false;
            }
            for (int index = 0; this.entities.item(index) != null; index++) {
                Node entNode1 = this.entities.item(index);
                Node entNode2 = argEntities.getNamedItem(entNode1.getNodeName());
                if (!((NodeImpl) entNode1).isEqualNode(entNode2)) {
                    return false;
                }
            }
        }
        NamedNodeMapImpl argNotations = argDocType.notations;
        if ((this.notations == null && argNotations != null) || (this.notations != null && argNotations == null)) {
            return false;
        }
        if (this.notations != null && argNotations != null) {
            if (this.notations.getLength() != argNotations.getLength()) {
                return false;
            }
            for (int index2 = 0; this.notations.item(index2) != null; index2++) {
                Node noteNode1 = this.notations.item(index2);
                Node noteNode2 = argNotations.getNamedItem(noteNode1.getNodeName());
                if (!((NodeImpl) noteNode1).isEqualNode(noteNode2)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    @Override
    protected void setOwnerDocument(CoreDocumentImpl doc) {
        super.setOwnerDocument(doc);
        this.entities.setOwnerDocument(doc);
        this.notations.setOwnerDocument(doc);
        this.elements.setOwnerDocument(doc);
    }

    @Override
    protected int getNodeNumber() {
        if (getOwnerDocument() != null) {
            return super.getNodeNumber();
        }
        if (this.doctypeNumber == 0) {
            CoreDOMImplementationImpl cd = (CoreDOMImplementationImpl) CoreDOMImplementationImpl.getDOMImplementation();
            this.doctypeNumber = cd.assignDocTypeNumber();
        }
        return this.doctypeNumber;
    }

    @Override
    public String getName() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.name;
    }

    @Override
    public NamedNodeMap getEntities() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return this.entities;
    }

    @Override
    public NamedNodeMap getNotations() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return this.notations;
    }

    @Override
    public void setReadOnly(boolean readOnly, boolean deep) {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        super.setReadOnly(readOnly, deep);
        this.elements.setReadOnly(readOnly, true);
        this.entities.setReadOnly(readOnly, true);
        this.notations.setReadOnly(readOnly, true);
    }

    public NamedNodeMap getElements() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return this.elements;
    }

    @Override
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        Object o;
        if (this.userData == null) {
            this.userData = new Hashtable();
        }
        if (data == null) {
            if (this.userData == null || (o = this.userData.remove(key)) == null) {
                return null;
            }
            ParentNode.UserDataRecord r = (ParentNode.UserDataRecord) o;
            return r.fData;
        }
        Object o2 = this.userData.put(key, new ParentNode.UserDataRecord(data, handler));
        if (o2 == null) {
            return null;
        }
        ParentNode.UserDataRecord r2 = (ParentNode.UserDataRecord) o2;
        return r2.fData;
    }

    @Override
    public Object getUserData(String key) {
        Object o;
        if (this.userData == null || (o = this.userData.get(key)) == null) {
            return null;
        }
        ParentNode.UserDataRecord r = (ParentNode.UserDataRecord) o;
        return r.fData;
    }

    @Override
    protected Hashtable getUserDataRecord() {
        return this.userData;
    }
}
