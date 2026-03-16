package org.apache.harmony.xml.dom;

import org.w3c.dom.Entity;

public class EntityImpl extends NodeImpl implements Entity {
    private String notationName;
    private String publicID;
    private String systemID;

    EntityImpl(DocumentImpl document, String notationName, String publicID, String systemID) {
        super(document);
        this.notationName = notationName;
        this.publicID = publicID;
        this.systemID = systemID;
    }

    @Override
    public String getNodeName() {
        return getNotationName();
    }

    @Override
    public short getNodeType() {
        return (short) 6;
    }

    @Override
    public String getNotationName() {
        return this.notationName;
    }

    @Override
    public String getPublicId() {
        return this.publicID;
    }

    @Override
    public String getSystemId() {
        return this.systemID;
    }

    @Override
    public String getInputEncoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getXmlEncoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getXmlVersion() {
        throw new UnsupportedOperationException();
    }
}
