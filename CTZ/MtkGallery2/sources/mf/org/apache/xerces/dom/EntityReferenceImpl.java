package mf.org.apache.xerces.dom;

import mf.org.apache.xerces.util.URI;
import mf.org.w3c.dom.DocumentType;
import mf.org.w3c.dom.EntityReference;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;

public class EntityReferenceImpl extends ParentNode implements EntityReference {
    static final long serialVersionUID = -7381452955687102062L;
    protected String baseURI;
    protected String name;

    public EntityReferenceImpl(CoreDocumentImpl ownerDoc, String name) {
        super(ownerDoc);
        this.name = name;
        isReadOnly(true);
        needsSyncChildren(true);
    }

    @Override
    public short getNodeType() {
        return (short) 5;
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
        EntityReferenceImpl er = (EntityReferenceImpl) super.cloneNode(deep);
        er.setReadOnly(true, deep);
        return er;
    }

    @Override
    public String getBaseURI() {
        NamedNodeMap entities;
        EntityImpl entDef;
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.baseURI == null) {
            DocumentType doctype = getOwnerDocument().getDoctype();
            if (doctype != null && (entities = doctype.getEntities()) != null && (entDef = (EntityImpl) entities.getNamedItem(getNodeName())) != null) {
                return entDef.getBaseURI();
            }
        } else if (this.baseURI != null && this.baseURI.length() != 0) {
            try {
                return new URI(this.baseURI).toString();
            } catch (URI.MalformedURIException e) {
                return null;
            }
        }
        return this.baseURI;
    }

    public void setBaseURI(String uri) {
        if (needsSyncData()) {
            synchronizeData();
        }
        this.baseURI = uri;
    }

    protected String getEntityRefValue() {
        String value;
        String value2;
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        if (this.firstChild != null) {
            if (this.firstChild.getNodeType() == 5) {
                value = ((EntityReferenceImpl) this.firstChild).getEntityRefValue();
            } else {
                if (this.firstChild.getNodeType() != 3) {
                    return null;
                }
                value = this.firstChild.getNodeValue();
            }
            if (this.firstChild.nextSibling == null) {
                return value;
            }
            StringBuffer buff = new StringBuffer(value);
            for (ChildNode next = this.firstChild.nextSibling; next != null; next = next.nextSibling) {
                if (next.getNodeType() == 5) {
                    value2 = ((EntityReferenceImpl) next).getEntityRefValue();
                } else {
                    if (next.getNodeType() != 3) {
                        return null;
                    }
                    value2 = next.getNodeValue();
                }
                buff.append(value2);
            }
            return buff.toString();
        }
        return "";
    }

    @Override
    protected void synchronizeChildren() {
        NamedNodeMap entities;
        EntityImpl entDef;
        needsSyncChildren(false);
        DocumentType doctype = getOwnerDocument().getDoctype();
        if (doctype == null || (entities = doctype.getEntities()) == null || (entDef = (EntityImpl) entities.getNamedItem(getNodeName())) == null) {
            return;
        }
        isReadOnly(false);
        for (Node defkid = entDef.getFirstChild(); defkid != null; defkid = defkid.getNextSibling()) {
            Node newkid = defkid.cloneNode(true);
            insertBefore(newkid, null);
        }
        setReadOnly(true, true);
    }

    @Override
    public void setReadOnly(boolean readOnly, boolean deep) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (deep) {
            if (needsSyncChildren()) {
                synchronizeChildren();
            }
            for (ChildNode mykid = this.firstChild; mykid != null; mykid = mykid.nextSibling) {
                mykid.setReadOnly(readOnly, true);
            }
        }
        isReadOnly(readOnly);
    }
}
