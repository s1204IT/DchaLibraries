package org.apache.harmony.xml.dom;

import org.w3c.dom.DocumentFragment;

public class DocumentFragmentImpl extends InnerNodeImpl implements DocumentFragment {
    DocumentFragmentImpl(DocumentImpl document) {
        super(document);
    }

    @Override
    public String getNodeName() {
        return "#document-fragment";
    }

    @Override
    public short getNodeType() {
        return (short) 11;
    }
}
