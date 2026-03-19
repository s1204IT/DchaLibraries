package org.apache.harmony.xml.dom;

import org.w3c.dom.Comment;

public final class CommentImpl extends CharacterDataImpl implements Comment {
    CommentImpl(DocumentImpl documentImpl, String str) {
        super(documentImpl, str);
    }

    @Override
    public String getNodeName() {
        return "#comment";
    }

    @Override
    public short getNodeType() {
        return (short) 8;
    }

    public boolean containsDashDash() {
        return this.buffer.indexOf("--") != -1;
    }
}
