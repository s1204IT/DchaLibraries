package org.apache.xml.serializer.dom3;

import org.w3c.dom.DOMLocator;
import org.w3c.dom.Node;

final class DOMLocatorImpl implements DOMLocator {
    private final int fByteOffset;
    private final int fColumnNumber;
    private final int fLineNumber;
    private final Node fRelatedNode;
    private final String fUri;
    private final int fUtf16Offset;

    DOMLocatorImpl() {
        this.fColumnNumber = -1;
        this.fLineNumber = -1;
        this.fRelatedNode = null;
        this.fUri = null;
        this.fByteOffset = -1;
        this.fUtf16Offset = -1;
    }

    DOMLocatorImpl(int i, int i2, String str) {
        this.fLineNumber = i;
        this.fColumnNumber = i2;
        this.fUri = str;
        this.fRelatedNode = null;
        this.fByteOffset = -1;
        this.fUtf16Offset = -1;
    }

    DOMLocatorImpl(int i, int i2, int i3, String str) {
        this.fLineNumber = i;
        this.fColumnNumber = i2;
        this.fUri = str;
        this.fUtf16Offset = i3;
        this.fRelatedNode = null;
        this.fByteOffset = -1;
    }

    DOMLocatorImpl(int i, int i2, int i3, Node node, String str) {
        this.fLineNumber = i;
        this.fColumnNumber = i2;
        this.fByteOffset = i3;
        this.fRelatedNode = node;
        this.fUri = str;
        this.fUtf16Offset = -1;
    }

    DOMLocatorImpl(int i, int i2, int i3, Node node, String str, int i4) {
        this.fLineNumber = i;
        this.fColumnNumber = i2;
        this.fByteOffset = i3;
        this.fRelatedNode = node;
        this.fUri = str;
        this.fUtf16Offset = i4;
    }

    @Override
    public int getLineNumber() {
        return this.fLineNumber;
    }

    @Override
    public int getColumnNumber() {
        return this.fColumnNumber;
    }

    @Override
    public String getUri() {
        return this.fUri;
    }

    @Override
    public Node getRelatedNode() {
        return this.fRelatedNode;
    }

    @Override
    public int getByteOffset() {
        return this.fByteOffset;
    }

    @Override
    public int getUtf16Offset() {
        return this.fUtf16Offset;
    }
}
