package org.apache.harmony.xml.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class TextImpl extends CharacterDataImpl implements Text {
    public TextImpl(DocumentImpl documentImpl, String str) {
        super(documentImpl, str);
    }

    @Override
    public String getNodeName() {
        return "#text";
    }

    @Override
    public short getNodeType() {
        return (short) 3;
    }

    @Override
    public final Text splitText(int i) throws DOMException {
        TextImpl textImplCreateTextNode = this.document.createTextNode(substringData(i, getLength() - i));
        deleteData(0, i);
        Node nextSibling = getNextSibling();
        if (nextSibling == null) {
            getParentNode().appendChild(textImplCreateTextNode);
        } else {
            getParentNode().insertBefore(textImplCreateTextNode, nextSibling);
        }
        return this;
    }

    @Override
    public final boolean isElementContentWhitespace() {
        return false;
    }

    @Override
    public final String getWholeText() {
        StringBuilder sb = new StringBuilder();
        for (TextImpl textImplFirstTextNodeInCurrentRun = firstTextNodeInCurrentRun(); textImplFirstTextNodeInCurrentRun != null; textImplFirstTextNodeInCurrentRun = textImplFirstTextNodeInCurrentRun.nextTextNode()) {
            textImplFirstTextNodeInCurrentRun.appendDataTo(sb);
        }
        return sb.toString();
    }

    @Override
    public final Text replaceWholeText(String str) throws DOMException {
        Node parentNode = getParentNode();
        TextImpl textImplFirstTextNodeInCurrentRun = firstTextNodeInCurrentRun();
        TextImpl textImpl = null;
        while (textImplFirstTextNodeInCurrentRun != null) {
            if (textImplFirstTextNodeInCurrentRun == this && str != null && str.length() > 0) {
                setData(str);
                textImplFirstTextNodeInCurrentRun = textImplFirstTextNodeInCurrentRun.nextTextNode();
                textImpl = this;
            } else {
                TextImpl textImplNextTextNode = textImplFirstTextNodeInCurrentRun.nextTextNode();
                parentNode.removeChild(textImplFirstTextNodeInCurrentRun);
                textImplFirstTextNodeInCurrentRun = textImplNextTextNode;
            }
        }
        return textImpl;
    }

    private TextImpl firstTextNodeInCurrentRun() {
        TextImpl textImpl = this;
        for (Node previousSibling = getPreviousSibling(); previousSibling != null; previousSibling = previousSibling.getPreviousSibling()) {
            short nodeType = previousSibling.getNodeType();
            if (nodeType != 3 && nodeType != 4) {
                break;
            }
            textImpl = (TextImpl) previousSibling;
        }
        return textImpl;
    }

    private TextImpl nextTextNode() {
        Node nextSibling = getNextSibling();
        if (nextSibling == null) {
            return null;
        }
        short nodeType = nextSibling.getNodeType();
        if (nodeType != 3 && nodeType != 4) {
            return null;
        }
        return (TextImpl) nextSibling;
    }

    public final TextImpl minimize() {
        if (getLength() == 0) {
            this.parent.removeChild(this);
            return null;
        }
        Node previousSibling = getPreviousSibling();
        if (previousSibling == null || previousSibling.getNodeType() != 3) {
            return this;
        }
        TextImpl textImpl = (TextImpl) previousSibling;
        textImpl.buffer.append(this.buffer);
        this.parent.removeChild(this);
        return textImpl;
    }
}
