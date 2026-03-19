package mf.org.apache.xerces.dom;

import java.util.ArrayList;
import mf.org.w3c.dom.CharacterData;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.DocumentFragment;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.ranges.Range;
import mf.org.w3c.dom.ranges.RangeException;

public class RangeImpl implements Range {
    static final int CLONE_CONTENTS = 2;
    static final int DELETE_CONTENTS = 3;
    static final int EXTRACT_CONTENTS = 1;
    private boolean fDetach;
    private DocumentImpl fDocument;
    private Node fEndContainer;
    private Node fStartContainer;
    private Node fInsertNode = null;
    private Node fDeleteNode = null;
    private Node fSplitNode = null;
    private boolean fInsertedFromRange = false;
    private Node fRemoveChild = null;
    private int fStartOffset = 0;
    private int fEndOffset = 0;

    public RangeImpl(DocumentImpl document) {
        this.fDetach = false;
        this.fDocument = document;
        this.fStartContainer = document;
        this.fEndContainer = document;
        this.fDetach = false;
    }

    @Override
    public Node getStartContainer() {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        return this.fStartContainer;
    }

    @Override
    public int getStartOffset() {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        return this.fStartOffset;
    }

    @Override
    public Node getEndContainer() {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        return this.fEndContainer;
    }

    @Override
    public int getEndOffset() {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        return this.fEndOffset;
    }

    public boolean getCollapsed() {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        return this.fStartContainer == this.fEndContainer && this.fStartOffset == this.fEndOffset;
    }

    public Node getCommonAncestorContainer() {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        ArrayList startV = new ArrayList();
        for (Node node = this.fStartContainer; node != null; node = node.getParentNode()) {
            startV.add(node);
        }
        ArrayList endV = new ArrayList();
        for (Node node2 = this.fEndContainer; node2 != null; node2 = node2.getParentNode()) {
            endV.add(node2);
        }
        int s = startV.size() - 1;
        Object result = null;
        for (int e = endV.size() - 1; s >= 0 && e >= 0 && startV.get(s) == endV.get(e); e--) {
            result = startV.get(s);
            s--;
        }
        return (Node) result;
    }

    @Override
    public void setStart(Node refNode, int offset) throws DOMException, RangeException {
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (!isLegalContainer(refNode)) {
                throw new RangeExceptionImpl((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if (this.fDocument != refNode.getOwnerDocument() && this.fDocument != refNode) {
                throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        checkIndex(refNode, offset);
        this.fStartContainer = refNode;
        this.fStartOffset = offset;
        if (getCommonAncestorContainer() == null || (this.fStartContainer == this.fEndContainer && this.fEndOffset < this.fStartOffset)) {
            collapse(true);
        }
    }

    @Override
    public void setEnd(Node refNode, int offset) throws DOMException, RangeException {
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (!isLegalContainer(refNode)) {
                throw new RangeExceptionImpl((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if (this.fDocument != refNode.getOwnerDocument() && this.fDocument != refNode) {
                throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        checkIndex(refNode, offset);
        this.fEndContainer = refNode;
        this.fEndOffset = offset;
        if (getCommonAncestorContainer() == null || (this.fStartContainer == this.fEndContainer && this.fEndOffset < this.fStartOffset)) {
            collapse(false);
        }
    }

    public void setStartBefore(Node refNode) throws RangeException {
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (!hasLegalRootContainer(refNode) || !isLegalContainedNode(refNode)) {
                throw new RangeExceptionImpl((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if (this.fDocument != refNode.getOwnerDocument() && this.fDocument != refNode) {
                throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        this.fStartContainer = refNode.getParentNode();
        int i = 0;
        for (Node n = refNode; n != null; n = n.getPreviousSibling()) {
            i++;
        }
        this.fStartOffset = i - 1;
        if (getCommonAncestorContainer() == null || (this.fStartContainer == this.fEndContainer && this.fEndOffset < this.fStartOffset)) {
            collapse(true);
        }
    }

    public void setStartAfter(Node refNode) throws RangeException {
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (!hasLegalRootContainer(refNode) || !isLegalContainedNode(refNode)) {
                throw new RangeExceptionImpl((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if (this.fDocument != refNode.getOwnerDocument() && this.fDocument != refNode) {
                throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        this.fStartContainer = refNode.getParentNode();
        int i = 0;
        for (Node n = refNode; n != null; n = n.getPreviousSibling()) {
            i++;
        }
        this.fStartOffset = i;
        if (getCommonAncestorContainer() == null || (this.fStartContainer == this.fEndContainer && this.fEndOffset < this.fStartOffset)) {
            collapse(true);
        }
    }

    public void setEndBefore(Node refNode) throws RangeException {
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (!hasLegalRootContainer(refNode) || !isLegalContainedNode(refNode)) {
                throw new RangeExceptionImpl((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if (this.fDocument != refNode.getOwnerDocument() && this.fDocument != refNode) {
                throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        this.fEndContainer = refNode.getParentNode();
        int i = 0;
        for (Node n = refNode; n != null; n = n.getPreviousSibling()) {
            i++;
        }
        this.fEndOffset = i - 1;
        if (getCommonAncestorContainer() == null || (this.fStartContainer == this.fEndContainer && this.fEndOffset < this.fStartOffset)) {
            collapse(false);
        }
    }

    public void setEndAfter(Node refNode) throws RangeException {
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (!hasLegalRootContainer(refNode) || !isLegalContainedNode(refNode)) {
                throw new RangeExceptionImpl((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if (this.fDocument != refNode.getOwnerDocument() && this.fDocument != refNode) {
                throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        this.fEndContainer = refNode.getParentNode();
        int i = 0;
        for (Node n = refNode; n != null; n = n.getPreviousSibling()) {
            i++;
        }
        this.fEndOffset = i;
        if (getCommonAncestorContainer() == null || (this.fStartContainer == this.fEndContainer && this.fEndOffset < this.fStartOffset)) {
            collapse(false);
        }
    }

    public void collapse(boolean toStart) {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        if (toStart) {
            this.fEndContainer = this.fStartContainer;
            this.fEndOffset = this.fStartOffset;
        } else {
            this.fStartContainer = this.fEndContainer;
            this.fStartOffset = this.fEndOffset;
        }
    }

    public void selectNode(Node refNode) throws RangeException {
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (!isLegalContainer(refNode.getParentNode()) || !isLegalContainedNode(refNode)) {
                throw new RangeExceptionImpl((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if (this.fDocument != refNode.getOwnerDocument() && this.fDocument != refNode) {
                throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        Node parent = refNode.getParentNode();
        if (parent != null) {
            this.fStartContainer = parent;
            this.fEndContainer = parent;
            int i = 0;
            for (Node n = refNode; n != null; n = n.getPreviousSibling()) {
                i++;
            }
            this.fStartOffset = i - 1;
            this.fEndOffset = this.fStartOffset + 1;
        }
    }

    public void selectNodeContents(Node refNode) throws RangeException {
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (!isLegalContainer(refNode)) {
                throw new RangeExceptionImpl((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if (this.fDocument != refNode.getOwnerDocument() && this.fDocument != refNode) {
                throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        this.fStartContainer = refNode;
        this.fEndContainer = refNode;
        Node first = refNode.getFirstChild();
        this.fStartOffset = 0;
        if (first == null) {
            this.fEndOffset = 0;
            return;
        }
        int i = 0;
        for (Node n = first; n != null; n = n.getNextSibling()) {
            i++;
        }
        this.fEndOffset = i;
    }

    public short compareBoundaryPoints(short how, Range sourceRange) throws DOMException {
        Node endPointA;
        Node endPointB;
        int offsetA;
        int offsetB;
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if ((this.fDocument != sourceRange.getStartContainer().getOwnerDocument() && this.fDocument != sourceRange.getStartContainer() && sourceRange.getStartContainer() != null) || (this.fDocument != sourceRange.getEndContainer().getOwnerDocument() && this.fDocument != sourceRange.getEndContainer() && sourceRange.getStartContainer() != null)) {
                throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        if (how == 0) {
            endPointA = sourceRange.getStartContainer();
            endPointB = this.fStartContainer;
            offsetA = sourceRange.getStartOffset();
            offsetB = this.fStartOffset;
        } else if (how == 1) {
            endPointA = sourceRange.getStartContainer();
            endPointB = this.fEndContainer;
            offsetA = sourceRange.getStartOffset();
            offsetB = this.fEndOffset;
        } else if (how == 3) {
            endPointA = sourceRange.getEndContainer();
            endPointB = this.fStartContainer;
            offsetA = sourceRange.getEndOffset();
            offsetB = this.fStartOffset;
        } else {
            endPointA = sourceRange.getEndContainer();
            endPointB = this.fEndContainer;
            offsetA = sourceRange.getEndOffset();
            offsetB = this.fEndOffset;
        }
        if (endPointA == endPointB) {
            if (offsetA < offsetB) {
                return (short) 1;
            }
            return offsetA == offsetB ? (short) 0 : (short) -1;
        }
        Node c = endPointB;
        for (Node p = c.getParentNode(); p != null; p = p.getParentNode()) {
            if (p != endPointA) {
                c = p;
            } else {
                int index = indexOf(c, endPointA);
                if (offsetA <= index) {
                    return (short) 1;
                }
                return (short) -1;
            }
        }
        Node c2 = endPointA;
        for (Node p2 = c2.getParentNode(); p2 != null; p2 = p2.getParentNode()) {
            if (p2 != endPointB) {
                c2 = p2;
            } else {
                int index2 = indexOf(c2, endPointB);
                if (index2 < offsetB) {
                    return (short) 1;
                }
                return (short) -1;
            }
        }
        int depthDiff = 0;
        for (Node n = endPointA; n != null; n = n.getParentNode()) {
            depthDiff++;
        }
        for (Node n2 = endPointB; n2 != null; n2 = n2.getParentNode()) {
            depthDiff--;
        }
        while (depthDiff > 0) {
            endPointA = endPointA.getParentNode();
            depthDiff--;
        }
        while (depthDiff < 0) {
            endPointB = endPointB.getParentNode();
            depthDiff++;
        }
        Node pA = endPointA.getParentNode();
        for (Node pB = endPointB.getParentNode(); pA != pB; pB = pB.getParentNode()) {
            endPointA = pA;
            endPointB = pB;
            pA = pA.getParentNode();
        }
        for (Node n3 = endPointA.getNextSibling(); n3 != null; n3 = n3.getNextSibling()) {
            if (n3 == endPointB) {
                return (short) 1;
            }
        }
        return (short) -1;
    }

    public void deleteContents() throws DOMException {
        traverseContents(3);
    }

    public DocumentFragment extractContents() throws DOMException {
        return traverseContents(1);
    }

    public DocumentFragment cloneContents() throws DOMException {
        return traverseContents(2);
    }

    public void insertNode(Node newNode) throws DOMException, RangeException {
        if (newNode == null) {
            return;
        }
        int type = newNode.getNodeType();
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (this.fDocument != newNode.getOwnerDocument()) {
                throw new DOMException((short) 4, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
            if (type == 2 || type == 6 || type == 12 || type == 9) {
                throw new RangeExceptionImpl((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
        }
        int currentChildren = 0;
        this.fInsertedFromRange = true;
        if (this.fStartContainer.getNodeType() == 3) {
            Node parent = this.fStartContainer.getParentNode();
            int currentChildren2 = parent.getChildNodes().getLength();
            Node cloneCurrent = this.fStartContainer.cloneNode(false);
            ((TextImpl) cloneCurrent).setNodeValueInternal(cloneCurrent.getNodeValue().substring(this.fStartOffset));
            ((TextImpl) this.fStartContainer).setNodeValueInternal(this.fStartContainer.getNodeValue().substring(0, this.fStartOffset));
            Node next = this.fStartContainer.getNextSibling();
            if (next != null) {
                if (parent != null) {
                    parent.insertBefore(newNode, next);
                    parent.insertBefore(cloneCurrent, next);
                }
            } else if (parent != null) {
                parent.appendChild(newNode);
                parent.appendChild(cloneCurrent);
            }
            if (this.fEndContainer == this.fStartContainer) {
                this.fEndContainer = cloneCurrent;
                this.fEndOffset -= this.fStartOffset;
            } else if (this.fEndContainer == parent) {
                this.fEndOffset += parent.getChildNodes().getLength() - currentChildren2;
            }
            signalSplitData(this.fStartContainer, cloneCurrent, this.fStartOffset);
        } else {
            if (this.fEndContainer == this.fStartContainer) {
                currentChildren = this.fEndContainer.getChildNodes().getLength();
            }
            Node current = this.fStartContainer.getFirstChild();
            for (int i = 0; i < this.fStartOffset && current != null; i++) {
                current = current.getNextSibling();
            }
            if (current != null) {
                this.fStartContainer.insertBefore(newNode, current);
            } else {
                this.fStartContainer.appendChild(newNode);
            }
            if (this.fEndContainer == this.fStartContainer && this.fEndOffset != 0) {
                this.fEndOffset += this.fEndContainer.getChildNodes().getLength() - currentChildren;
            }
        }
        this.fInsertedFromRange = false;
    }

    public void surroundContents(Node newParent) throws DOMException, RangeException {
        if (newParent == null) {
            return;
        }
        int type = newParent.getNodeType();
        if (this.fDocument.errorChecking) {
            if (this.fDetach) {
                throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (type == 2 || type == 6 || type == 12 || type == 10 || type == 9 || type == 11) {
                throw new RangeExceptionImpl((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
        }
        Node realStart = this.fStartContainer;
        Node realEnd = this.fEndContainer;
        if (this.fStartContainer.getNodeType() == 3) {
            realStart = this.fStartContainer.getParentNode();
        }
        if (this.fEndContainer.getNodeType() == 3) {
            realEnd = this.fEndContainer.getParentNode();
        }
        if (realStart != realEnd) {
            throw new RangeExceptionImpl((short) 1, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "BAD_BOUNDARYPOINTS_ERR", null));
        }
        DocumentFragment frag = extractContents();
        insertNode(newParent);
        newParent.appendChild(frag);
        selectNode(newParent);
    }

    public Range cloneRange() {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        Range range = this.fDocument.createRange();
        range.setStart(this.fStartContainer, this.fStartOffset);
        range.setEnd(this.fEndContainer, this.fEndOffset);
        return range;
    }

    public String toString() {
        Node node;
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        Node node2 = this.fStartContainer;
        Node stopNode = this.fEndContainer;
        StringBuffer sb = new StringBuffer();
        if (this.fStartContainer.getNodeType() == 3 || this.fStartContainer.getNodeType() == 4) {
            if (this.fStartContainer == this.fEndContainer) {
                sb.append(this.fStartContainer.getNodeValue().substring(this.fStartOffset, this.fEndOffset));
                return sb.toString();
            }
            sb.append(this.fStartContainer.getNodeValue().substring(this.fStartOffset));
            node = nextNode(node2, true);
        } else {
            node = node2.getFirstChild();
            if (this.fStartOffset > 0) {
                for (int counter = 0; counter < this.fStartOffset && node != null; counter++) {
                    node = node.getNextSibling();
                }
            }
            if (node == null) {
                node = nextNode(this.fStartContainer, false);
            }
        }
        if (this.fEndContainer.getNodeType() != 3 && this.fEndContainer.getNodeType() != 4) {
            int i = this.fEndOffset;
            stopNode = this.fEndContainer.getFirstChild();
            while (i > 0 && stopNode != null) {
                i--;
                stopNode = stopNode.getNextSibling();
            }
            if (stopNode == null) {
                stopNode = nextNode(this.fEndContainer, false);
            }
        }
        while (node != stopNode && node != null) {
            if (node.getNodeType() == 3 || node.getNodeType() == 4) {
                sb.append(node.getNodeValue());
            }
            node = nextNode(node, true);
        }
        if (this.fEndContainer.getNodeType() == 3 || this.fEndContainer.getNodeType() == 4) {
            sb.append(this.fEndContainer.getNodeValue().substring(0, this.fEndOffset));
        }
        return sb.toString();
    }

    public void detach() {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        this.fDetach = true;
        this.fDocument.removeRange(this);
    }

    void signalSplitData(Node node, Node newNode, int offset) {
        this.fSplitNode = node;
        this.fDocument.splitData(node, newNode, offset);
        this.fSplitNode = null;
    }

    void receiveSplitData(Node node, Node newNode, int offset) {
        if (node == null || newNode == null || this.fSplitNode == node) {
            return;
        }
        if (node == this.fStartContainer && this.fStartContainer.getNodeType() == 3 && this.fStartOffset > offset) {
            this.fStartOffset -= offset;
            this.fStartContainer = newNode;
        }
        if (node == this.fEndContainer && this.fEndContainer.getNodeType() == 3 && this.fEndOffset > offset) {
            this.fEndOffset -= offset;
            this.fEndContainer = newNode;
        }
    }

    void deleteData(CharacterData node, int offset, int count) {
        this.fDeleteNode = node;
        node.deleteData(offset, count);
        this.fDeleteNode = null;
    }

    void receiveDeletedText(CharacterDataImpl node, int offset, int count) {
        if (node == null || this.fDeleteNode == node) {
            return;
        }
        if (node == this.fStartContainer) {
            if (this.fStartOffset > offset + count) {
                this.fStartOffset = (this.fStartOffset - (offset + count)) + offset;
            } else if (this.fStartOffset > offset) {
                this.fStartOffset = offset;
            }
        }
        if (node == this.fEndContainer) {
            if (this.fEndOffset > offset + count) {
                this.fEndOffset = (this.fEndOffset - (offset + count)) + offset;
            } else if (this.fEndOffset > offset) {
                this.fEndOffset = offset;
            }
        }
    }

    void insertData(CharacterData node, int index, String insert) {
        this.fInsertNode = node;
        node.insertData(index, insert);
        this.fInsertNode = null;
    }

    void receiveInsertedText(CharacterDataImpl node, int index, int len) {
        if (node == null || this.fInsertNode == node) {
            return;
        }
        if (node == this.fStartContainer && index < this.fStartOffset) {
            this.fStartOffset += len;
        }
        if (node == this.fEndContainer && index < this.fEndOffset) {
            this.fEndOffset += len;
        }
    }

    void receiveReplacedText(CharacterDataImpl node) {
        if (node == null) {
            return;
        }
        if (node == this.fStartContainer) {
            this.fStartOffset = 0;
        }
        if (node == this.fEndContainer) {
            this.fEndOffset = 0;
        }
    }

    public void insertedNodeFromDOM(Node node) {
        if (node == null || this.fInsertNode == node || this.fInsertedFromRange) {
            return;
        }
        Node parent = node.getParentNode();
        if (parent == this.fStartContainer) {
            int index = indexOf(node, this.fStartContainer);
            if (index < this.fStartOffset) {
                this.fStartOffset++;
            }
        }
        if (parent == this.fEndContainer) {
            int index2 = indexOf(node, this.fEndContainer);
            if (index2 < this.fEndOffset) {
                this.fEndOffset++;
            }
        }
    }

    Node removeChild(Node parent, Node child) {
        this.fRemoveChild = child;
        Node n = parent.removeChild(child);
        this.fRemoveChild = null;
        return n;
    }

    void removeNode(Node node) {
        if (node == null || this.fRemoveChild == node) {
            return;
        }
        Node parent = node.getParentNode();
        if (parent == this.fStartContainer) {
            int index = indexOf(node, this.fStartContainer);
            if (index < this.fStartOffset) {
                this.fStartOffset--;
            }
        }
        if (parent == this.fEndContainer) {
            int index2 = indexOf(node, this.fEndContainer);
            if (index2 < this.fEndOffset) {
                this.fEndOffset--;
            }
        }
        if (parent != this.fStartContainer || parent != this.fEndContainer) {
            if (isAncestorOf(node, this.fStartContainer)) {
                this.fStartContainer = parent;
                this.fStartOffset = indexOf(node, parent);
            }
            if (isAncestorOf(node, this.fEndContainer)) {
                this.fEndContainer = parent;
                this.fEndOffset = indexOf(node, parent);
            }
        }
    }

    private DocumentFragment traverseContents(int how) throws DOMException {
        if (this.fStartContainer == null || this.fEndContainer == null) {
            return null;
        }
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        if (this.fStartContainer == this.fEndContainer) {
            return traverseSameContainer(how);
        }
        int endContainerDepth = 0;
        Node c = this.fEndContainer;
        for (Node p = c.getParentNode(); p != null; p = p.getParentNode()) {
            Node p2 = this.fStartContainer;
            if (p == p2) {
                return traverseCommonStartContainer(c, how);
            }
            endContainerDepth++;
            c = p;
        }
        int startContainerDepth = 0;
        Node c2 = this.fStartContainer;
        for (Node p3 = c2.getParentNode(); p3 != null; p3 = p3.getParentNode()) {
            if (p3 == this.fEndContainer) {
                return traverseCommonEndContainer(c2, how);
            }
            startContainerDepth++;
            c2 = p3;
        }
        int depthDiff = startContainerDepth - endContainerDepth;
        Node startNode = this.fStartContainer;
        while (depthDiff > 0) {
            startNode = startNode.getParentNode();
            depthDiff--;
        }
        Node endNode = this.fEndContainer;
        while (depthDiff < 0) {
            endNode = endNode.getParentNode();
            depthDiff++;
        }
        Node sp = startNode.getParentNode();
        for (Node ep = endNode.getParentNode(); sp != ep; ep = ep.getParentNode()) {
            startNode = sp;
            endNode = ep;
            sp = sp.getParentNode();
        }
        return traverseCommonAncestors(startNode, endNode, how);
    }

    private DocumentFragment traverseSameContainer(int how) {
        DocumentFragment frag = null;
        if (how != 3) {
            frag = this.fDocument.createDocumentFragment();
        }
        if (this.fStartOffset == this.fEndOffset) {
            return frag;
        }
        short nodeType = this.fStartContainer.getNodeType();
        if (nodeType == 3 || nodeType == 4 || nodeType == 8 || nodeType == 7) {
            String s = this.fStartContainer.getNodeValue();
            String sub = s.substring(this.fStartOffset, this.fEndOffset);
            if (how != 2) {
                ((CharacterDataImpl) this.fStartContainer).deleteData(this.fStartOffset, this.fEndOffset - this.fStartOffset);
                collapse(true);
            }
            if (how == 3) {
                return null;
            }
            if (nodeType == 3) {
                frag.appendChild(this.fDocument.createTextNode(sub));
            } else if (nodeType == 4) {
                frag.appendChild(this.fDocument.createCDATASection(sub));
            } else if (nodeType == 8) {
                frag.appendChild(this.fDocument.createComment(sub));
            } else {
                frag.appendChild(this.fDocument.createProcessingInstruction(this.fStartContainer.getNodeName(), sub));
            }
            return frag;
        }
        Node n = getSelectedNode(this.fStartContainer, this.fStartOffset);
        int cnt = this.fEndOffset - this.fStartOffset;
        while (cnt > 0) {
            Node sibling = n.getNextSibling();
            Node xferNode = traverseFullySelected(n, how);
            if (frag != null) {
                frag.appendChild(xferNode);
            }
            cnt--;
            n = sibling;
        }
        if (how != 2) {
            collapse(true);
        }
        return frag;
    }

    private DocumentFragment traverseCommonStartContainer(Node endAncestor, int how) {
        DocumentFragment frag = null;
        if (how != 3) {
            frag = this.fDocument.createDocumentFragment();
        }
        Node n = traverseRightBoundary(endAncestor, how);
        if (frag != null) {
            frag.appendChild(n);
        }
        int endIdx = indexOf(endAncestor, this.fStartContainer);
        int cnt = endIdx - this.fStartOffset;
        if (cnt <= 0) {
            if (how != 2) {
                setEndBefore(endAncestor);
                collapse(false);
            }
            return frag;
        }
        Node n2 = endAncestor.getPreviousSibling();
        while (cnt > 0) {
            Node sibling = n2.getPreviousSibling();
            Node xferNode = traverseFullySelected(n2, how);
            if (frag != null) {
                frag.insertBefore(xferNode, frag.getFirstChild());
            }
            cnt--;
            n2 = sibling;
        }
        if (how != 2) {
            setEndBefore(endAncestor);
            collapse(false);
        }
        return frag;
    }

    private DocumentFragment traverseCommonEndContainer(Node startAncestor, int how) {
        DocumentFragment frag = null;
        if (how != 3) {
            frag = this.fDocument.createDocumentFragment();
        }
        Node n = traverseLeftBoundary(startAncestor, how);
        if (frag != null) {
            frag.appendChild(n);
        }
        int startIdx = indexOf(startAncestor, this.fEndContainer);
        int cnt = this.fEndOffset - (startIdx + 1);
        Node n2 = startAncestor.getNextSibling();
        while (cnt > 0) {
            Node sibling = n2.getNextSibling();
            Node xferNode = traverseFullySelected(n2, how);
            if (frag != null) {
                frag.appendChild(xferNode);
            }
            cnt--;
            n2 = sibling;
        }
        if (how != 2) {
            setStartAfter(startAncestor);
            collapse(true);
        }
        return frag;
    }

    private DocumentFragment traverseCommonAncestors(Node startAncestor, Node endAncestor, int how) {
        DocumentFragment frag = null;
        if (how != 3) {
            frag = this.fDocument.createDocumentFragment();
        }
        Node n = traverseLeftBoundary(startAncestor, how);
        if (frag != null) {
            frag.appendChild(n);
        }
        Node commonParent = startAncestor.getParentNode();
        int startOffset = indexOf(startAncestor, commonParent);
        int endOffset = indexOf(endAncestor, commonParent);
        Node sibling = startAncestor.getNextSibling();
        for (int cnt = endOffset - (startOffset + 1); cnt > 0; cnt--) {
            Node nextSibling = sibling.getNextSibling();
            Node n2 = traverseFullySelected(sibling, how);
            if (frag != null) {
                frag.appendChild(n2);
            }
            sibling = nextSibling;
        }
        Node n3 = traverseRightBoundary(endAncestor, how);
        if (frag != null) {
            frag.appendChild(n3);
        }
        if (how != 2) {
            setStartAfter(startAncestor);
            collapse(true);
        }
        return frag;
    }

    private Node traverseRightBoundary(Node root, int how) {
        Node next = getSelectedNode(this.fEndContainer, this.fEndOffset - 1);
        boolean isFullySelected = next != this.fEndContainer;
        if (next == root) {
            return traverseNode(next, isFullySelected, false, how);
        }
        Node parent = next.getParentNode();
        Node clonedParent = traverseNode(parent, false, false, how);
        while (parent != null) {
            while (next != null) {
                Node prevSibling = next.getPreviousSibling();
                Node clonedChild = traverseNode(next, isFullySelected, false, how);
                if (how != 3) {
                    clonedParent.insertBefore(clonedChild, clonedParent.getFirstChild());
                }
                isFullySelected = true;
                next = prevSibling;
            }
            if (parent == root) {
                return clonedParent;
            }
            next = parent.getPreviousSibling();
            parent = parent.getParentNode();
            Node clonedGrandParent = traverseNode(parent, false, false, how);
            if (how != 3) {
                clonedGrandParent.appendChild(clonedParent);
            }
            clonedParent = clonedGrandParent;
        }
        return null;
    }

    private Node traverseLeftBoundary(Node root, int how) {
        Node next = getSelectedNode(getStartContainer(), getStartOffset());
        boolean isFullySelected = next != getStartContainer();
        if (next == root) {
            return traverseNode(next, isFullySelected, true, how);
        }
        Node parent = next.getParentNode();
        Node clonedParent = traverseNode(parent, false, true, how);
        while (parent != null) {
            while (next != null) {
                Node nextSibling = next.getNextSibling();
                Node clonedChild = traverseNode(next, isFullySelected, true, how);
                if (how != 3) {
                    clonedParent.appendChild(clonedChild);
                }
                isFullySelected = true;
                next = nextSibling;
            }
            if (parent == root) {
                return clonedParent;
            }
            next = parent.getNextSibling();
            parent = parent.getParentNode();
            Node clonedGrandParent = traverseNode(parent, false, true, how);
            if (how != 3) {
                clonedGrandParent.appendChild(clonedParent);
            }
            clonedParent = clonedGrandParent;
        }
        return null;
    }

    private Node traverseNode(Node n, boolean isFullySelected, boolean isLeft, int how) {
        if (isFullySelected) {
            return traverseFullySelected(n, how);
        }
        short nodeType = n.getNodeType();
        if (nodeType == 3 || nodeType == 4 || nodeType == 8 || nodeType == 7) {
            return traverseCharacterDataNode(n, isLeft, how);
        }
        return traversePartiallySelected(n, how);
    }

    private Node traverseFullySelected(Node n, int how) {
        switch (how) {
            case 1:
                if (n.getNodeType() == 10) {
                    throw new DOMException((short) 3, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null));
                }
                return n;
            case 2:
                return n.cloneNode(true);
            case 3:
                n.getParentNode().removeChild(n);
                return null;
            default:
                return null;
        }
    }

    private Node traversePartiallySelected(Node n, int how) {
        switch (how) {
        }
        return null;
    }

    private Node traverseCharacterDataNode(Node n, boolean isLeft, int how) {
        String newNodeValue;
        String oldNodeValue;
        String txtValue = n.getNodeValue();
        if (isLeft) {
            int offset = getStartOffset();
            newNodeValue = txtValue.substring(offset);
            oldNodeValue = txtValue.substring(0, offset);
        } else {
            int offset2 = getEndOffset();
            newNodeValue = txtValue.substring(0, offset2);
            oldNodeValue = txtValue.substring(offset2);
        }
        if (how != 2) {
            n.setNodeValue(oldNodeValue);
        }
        if (how == 3) {
            return null;
        }
        Node newNode = n.cloneNode(false);
        newNode.setNodeValue(newNodeValue);
        return newNode;
    }

    void checkIndex(Node refNode, int offset) throws DOMException {
        if (offset < 0) {
            throw new DOMException((short) 1, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INDEX_SIZE_ERR", null));
        }
        int type = refNode.getNodeType();
        if (type == 3 || type == 4 || type == 8 || type == 7) {
            if (offset > refNode.getNodeValue().length()) {
                throw new DOMException((short) 1, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INDEX_SIZE_ERR", null));
            }
        } else if (offset > refNode.getChildNodes().getLength()) {
            throw new DOMException((short) 1, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INDEX_SIZE_ERR", null));
        }
    }

    private Node getRootContainer(Node node) {
        if (node == null) {
            return null;
        }
        while (node.getParentNode() != null) {
            node = node.getParentNode();
        }
        return node;
    }

    private boolean isLegalContainer(Node node) {
        if (node == null) {
            return false;
        }
        while (node != null) {
            short nodeType = node.getNodeType();
            if (nodeType == 6 || nodeType == 10 || nodeType == 12) {
                return false;
            }
            node = node.getParentNode();
        }
        return true;
    }

    private boolean hasLegalRootContainer(Node node) {
        if (node == null) {
            return false;
        }
        Node rootContainer = getRootContainer(node);
        short nodeType = rootContainer.getNodeType();
        if (nodeType != 2 && nodeType != 9 && nodeType != 11) {
            return false;
        }
        return true;
    }

    private boolean isLegalContainedNode(Node node) {
        short nodeType;
        if (node != null && (nodeType = node.getNodeType()) != 2 && nodeType != 6 && nodeType != 9) {
            switch (nodeType) {
            }
            return false;
        }
        return false;
    }

    Node nextNode(Node node, boolean visitChildren) {
        Node result;
        if (node == null) {
            return null;
        }
        if (visitChildren && (result = node.getFirstChild()) != null) {
            return result;
        }
        Node result2 = node.getNextSibling();
        if (result2 != null) {
            return result2;
        }
        for (Node parent = node.getParentNode(); parent != null && parent != this.fDocument; parent = parent.getParentNode()) {
            Node result3 = parent.getNextSibling();
            if (result3 != null) {
                return result3;
            }
        }
        return null;
    }

    boolean isAncestorOf(Node a, Node b) {
        for (Node node = b; node != null; node = node.getParentNode()) {
            if (node == a) {
                return true;
            }
        }
        return false;
    }

    int indexOf(Node child, Node parent) {
        if (child.getParentNode() != parent) {
            return -1;
        }
        int i = 0;
        for (Node node = parent.getFirstChild(); node != child; node = node.getNextSibling()) {
            i++;
        }
        return i;
    }

    private Node getSelectedNode(Node container, int offset) {
        if (container.getNodeType() == 3 || offset < 0) {
            return container;
        }
        Node child = container.getFirstChild();
        while (child != null && offset > 0) {
            offset--;
            child = child.getNextSibling();
        }
        if (child != null) {
            return child;
        }
        return container;
    }
}
