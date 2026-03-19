package org.apache.harmony.xml.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;

public final class DocumentTypeImpl extends LeafNodeImpl implements DocumentType {
    private String publicId;
    private String qualifiedName;
    private String systemId;

    public DocumentTypeImpl(DocumentImpl documentImpl, String str, String str2, String str3) {
        super(documentImpl);
        if (str == null || "".equals(str)) {
            throw new DOMException((short) 14, str);
        }
        int iLastIndexOf = str.lastIndexOf(":");
        if (iLastIndexOf != -1) {
            String strSubstring = str.substring(0, iLastIndexOf);
            String strSubstring2 = str.substring(iLastIndexOf + 1);
            if (!DocumentImpl.isXMLIdentifier(strSubstring)) {
                throw new DOMException((short) 14, str);
            }
            if (!DocumentImpl.isXMLIdentifier(strSubstring2)) {
                throw new DOMException((short) 5, str);
            }
        } else if (!DocumentImpl.isXMLIdentifier(str)) {
            throw new DOMException((short) 5, str);
        }
        this.qualifiedName = str;
        this.publicId = str2;
        this.systemId = str3;
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
