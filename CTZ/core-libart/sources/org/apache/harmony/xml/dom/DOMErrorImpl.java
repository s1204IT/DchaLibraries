package org.apache.harmony.xml.dom;

import org.w3c.dom.DOMError;
import org.w3c.dom.DOMLocator;
import org.w3c.dom.Node;

public final class DOMErrorImpl implements DOMError {
    private static final DOMLocator NULL_DOM_LOCATOR = new DOMLocator() {
        @Override
        public int getLineNumber() {
            return -1;
        }

        @Override
        public int getColumnNumber() {
            return -1;
        }

        @Override
        public int getByteOffset() {
            return -1;
        }

        @Override
        public int getUtf16Offset() {
            return -1;
        }

        @Override
        public Node getRelatedNode() {
            return null;
        }

        @Override
        public String getUri() {
            return null;
        }
    };
    private final short severity;
    private final String type;

    public DOMErrorImpl(short s, String str) {
        this.severity = s;
        this.type = str;
    }

    @Override
    public short getSeverity() {
        return this.severity;
    }

    @Override
    public String getMessage() {
        return this.type;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public Object getRelatedException() {
        return null;
    }

    @Override
    public Object getRelatedData() {
        return null;
    }

    @Override
    public DOMLocator getLocation() {
        return NULL_DOM_LOCATOR;
    }
}
