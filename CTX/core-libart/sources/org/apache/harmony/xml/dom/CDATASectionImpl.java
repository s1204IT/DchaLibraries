package org.apache.harmony.xml.dom;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Node;

public final class CDATASectionImpl extends TextImpl implements CDATASection {
    public CDATASectionImpl(DocumentImpl documentImpl, String str) {
        super(documentImpl, str);
    }

    @Override
    public String getNodeName() {
        return "#cdata-section";
    }

    @Override
    public short getNodeType() {
        return (short) 4;
    }

    public void split() {
        if (!needsSplitting()) {
            return;
        }
        Node parentNode = getParentNode();
        String[] strArrSplit = getData().split("\\]\\]>");
        parentNode.insertBefore(new CDATASectionImpl(this.document, strArrSplit[0] + "]]"), this);
        for (int i = 1; i < strArrSplit.length - 1; i++) {
            parentNode.insertBefore(new CDATASectionImpl(this.document, ">" + strArrSplit[i] + "]]"), this);
        }
        setData(">" + strArrSplit[strArrSplit.length - 1]);
    }

    public boolean needsSplitting() {
        return this.buffer.indexOf("]]>") != -1;
    }

    public TextImpl replaceWithText() {
        TextImpl textImpl = new TextImpl(this.document, getData());
        this.parent.insertBefore(textImpl, this);
        this.parent.removeChild(this);
        return textImpl;
    }
}
