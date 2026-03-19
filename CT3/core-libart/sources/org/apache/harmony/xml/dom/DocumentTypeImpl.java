package org.apache.harmony.xml.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;

public final class DocumentTypeImpl extends LeafNodeImpl implements DocumentType {
    private String publicId;
    private String qualifiedName;
    private String systemId;

    public DocumentTypeImpl(DocumentImpl document, String qualifiedName, String publicId, String systemId) {
        super(document);
        if (qualifiedName == null || "".equals(qualifiedName)) {
            throw new DOMException((short) 14, qualifiedName);
        }
        int prefixSeparator = qualifiedName.lastIndexOf(":");
        if (prefixSeparator != -1) {
            String prefix = qualifiedName.substring(0, prefixSeparator);
            String localName = qualifiedName.substring(prefixSeparator + 1);
            if (!DocumentImpl.isXMLIdentifier(prefix)) {
                throw new DOMException((short) 14, qualifiedName);
            }
            if (!DocumentImpl.isXMLIdentifier(localName)) {
                throw new DOMException((short) 5, qualifiedName);
            }
        } else if (!DocumentImpl.isXMLIdentifier(qualifiedName)) {
            throw new DOMException((short) 5, qualifiedName);
        }
        this.qualifiedName = qualifiedName;
        this.publicId = publicId;
        this.systemId = systemId;
    }

    @Override
    public String getNodeName() {
        return this.qualifiedName;
    }

    @Override
    public short getNodeType() {
        return (short) 10;
    }

    @Override
    public NamedNodeMap getEntities() {
        return null;
    }

    @Override
    public String getInternalSubset() {
        return null;
    }

    @Override
    public String getName() {
        return this.qualifiedName;
    }

    @Override
    public NamedNodeMap getNotations() {
        return null;
    }

    @Override
    public String getPublicId() {
        return this.publicId;
    }

    @Override
    public String getSystemId() {
        return this.systemId;
    }

    @Override
    public String getTextContent() throws DOMException {
        return null;
    }
}
