package mf.org.apache.xerces.dom;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;

public class NamedNodeMapImpl implements Serializable, NamedNodeMap {
    protected static final short CHANGED = 2;
    protected static final short HASDEFAULTS = 4;
    protected static final short READONLY = 1;
    static final long serialVersionUID = -7039242451046758020L;
    protected short flags;
    protected List nodes;
    protected NodeImpl ownerNode;

    protected NamedNodeMapImpl(NodeImpl ownerNode) {
        this.ownerNode = ownerNode;
    }

    @Override
    public int getLength() {
        if (this.nodes != null) {
            return this.nodes.size();
        }
        return 0;
    }

    @Override
    public Node item(int index) {
        if (this.nodes == null || index >= this.nodes.size()) {
            return null;
        }
        return (Node) this.nodes.get(index);
    }

    @Override
    public Node getNamedItem(String name) {
        int i = findNamePoint(name, 0);
        if (i < 0) {
            return null;
        }
        return (Node) this.nodes.get(i);
    }

    @Override
    public Node getNamedItemNS(String namespaceURI, String localName) {
        int i = findNamePoint(namespaceURI, localName);
        if (i < 0) {
            return null;
        }
        return (Node) this.nodes.get(i);
    }

    public Node setNamedItem(Node arg) throws DOMException {
        CoreDocumentImpl ownerDocument = this.ownerNode.ownerDocument();
        if (ownerDocument.errorChecking) {
            if (isReadOnly()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException((short) 7, msg);
            }
            if (arg.getOwnerDocument() != ownerDocument) {
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
                throw new DOMException((short) 4, msg2);
            }
        }
        String msg3 = arg.getNodeName();
        int i = findNamePoint(msg3, 0);
        if (i >= 0) {
            NodeImpl previous = (NodeImpl) this.nodes.get(i);
            this.nodes.set(i, arg);
            return previous;
        }
        int i2 = (-1) - i;
        if (this.nodes == null) {
            this.nodes = new ArrayList(5);
        }
        this.nodes.add(i2, arg);
        return null;
    }

    public Node setNamedItemNS(Node arg) throws DOMException {
        CoreDocumentImpl ownerDocument = this.ownerNode.ownerDocument();
        if (ownerDocument.errorChecking) {
            if (isReadOnly()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException((short) 7, msg);
            }
            if (arg.getOwnerDocument() != ownerDocument) {
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
                throw new DOMException((short) 4, msg2);
            }
        }
        String msg3 = arg.getNamespaceURI();
        int i = findNamePoint(msg3, arg.getLocalName());
        if (i >= 0) {
            NodeImpl previous = (NodeImpl) this.nodes.get(i);
            this.nodes.set(i, arg);
            return previous;
        }
        int i2 = findNamePoint(arg.getNodeName(), 0);
        if (i2 >= 0) {
            NodeImpl previous2 = (NodeImpl) this.nodes.get(i2);
            this.nodes.add(i2, arg);
            return previous2;
        }
        int i3 = (-1) - i2;
        if (this.nodes == null) {
            this.nodes = new ArrayList(5);
        }
        this.nodes.add(i3, arg);
        return null;
    }

    public Node removeNamedItem(String name) throws DOMException {
        if (isReadOnly()) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
            throw new DOMException((short) 7, msg);
        }
        int i = findNamePoint(name, 0);
        if (i < 0) {
            String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
            throw new DOMException((short) 8, msg2);
        }
        NodeImpl n = (NodeImpl) this.nodes.get(i);
        this.nodes.remove(i);
        return n;
    }

    public Node removeNamedItemNS(String namespaceURI, String name) throws DOMException {
        if (isReadOnly()) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
            throw new DOMException((short) 7, msg);
        }
        int i = findNamePoint(namespaceURI, name);
        if (i < 0) {
            String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
            throw new DOMException((short) 8, msg2);
        }
        NodeImpl n = (NodeImpl) this.nodes.get(i);
        this.nodes.remove(i);
        return n;
    }

    public NamedNodeMapImpl cloneMap(NodeImpl ownerNode) {
        NamedNodeMapImpl newmap = new NamedNodeMapImpl(ownerNode);
        newmap.cloneContent(this);
        return newmap;
    }

    protected void cloneContent(NamedNodeMapImpl srcmap) {
        int size;
        List srcnodes = srcmap.nodes;
        if (srcnodes != null && (size = srcnodes.size()) != 0) {
            if (this.nodes == null) {
                this.nodes = new ArrayList(size);
            } else {
                this.nodes.clear();
            }
            for (int i = 0; i < size; i++) {
                NodeImpl n = (NodeImpl) srcmap.nodes.get(i);
                NodeImpl clone = (NodeImpl) n.cloneNode(true);
                clone.isSpecified(n.isSpecified());
                this.nodes.add(clone);
            }
        }
    }

    void setReadOnly(boolean readOnly, boolean deep) {
        isReadOnly(readOnly);
        if (deep && this.nodes != null) {
            for (int i = this.nodes.size() - 1; i >= 0; i--) {
                ((NodeImpl) this.nodes.get(i)).setReadOnly(readOnly, deep);
            }
        }
    }

    boolean getReadOnly() {
        return isReadOnly();
    }

    protected void setOwnerDocument(CoreDocumentImpl doc) {
        if (this.nodes != null) {
            int size = this.nodes.size();
            for (int i = 0; i < size; i++) {
                ((NodeImpl) item(i)).setOwnerDocument(doc);
            }
        }
    }

    final boolean isReadOnly() {
        return (this.flags & 1) != 0;
    }

    final void isReadOnly(boolean value) {
        this.flags = (short) (value ? this.flags | 1 : this.flags & (-2));
    }

    final boolean changed() {
        return (this.flags & 2) != 0;
    }

    final void changed(boolean value) {
        this.flags = (short) (value ? this.flags | 2 : this.flags & (-3));
    }

    final boolean hasDefaults() {
        return (this.flags & 4) != 0;
    }

    final void hasDefaults(boolean value) {
        this.flags = (short) (value ? this.flags | 4 : this.flags & (-5));
    }

    protected int findNamePoint(String name, int start) {
        int i = 0;
        if (this.nodes != null) {
            int first = start;
            int last = this.nodes.size() - 1;
            while (first <= last) {
                i = (first + last) / 2;
                int test = name.compareTo(((Node) this.nodes.get(i)).getNodeName());
                if (test == 0) {
                    return i;
                }
                if (test < 0) {
                    last = i - 1;
                } else {
                    first = i + 1;
                }
            }
            if (first > i) {
                i = first;
            }
        }
        int first2 = (-1) - i;
        return first2;
    }

    protected int findNamePoint(String namespaceURI, String name) {
        if (this.nodes == null || name == null) {
            return -1;
        }
        int size = this.nodes.size();
        for (int i = 0; i < size; i++) {
            NodeImpl a = (NodeImpl) this.nodes.get(i);
            String aNamespaceURI = a.getNamespaceURI();
            String aLocalName = a.getLocalName();
            if (namespaceURI == null) {
                if (aNamespaceURI == null && (name.equals(aLocalName) || (aLocalName == null && name.equals(a.getNodeName())))) {
                    return i;
                }
            } else if (namespaceURI.equals(aNamespaceURI) && name.equals(aLocalName)) {
                return i;
            }
        }
        return -1;
    }

    protected boolean precedes(Node a, Node b) {
        if (this.nodes != null) {
            int size = this.nodes.size();
            for (int i = 0; i < size; i++) {
                Node n = (Node) this.nodes.get(i);
                if (n == a) {
                    return true;
                }
                if (n == b) {
                    return false;
                }
            }
        }
        return false;
    }

    protected void removeItem(int index) {
        if (this.nodes != null && index < this.nodes.size()) {
            this.nodes.remove(index);
        }
    }

    protected Object getItem(int index) {
        if (this.nodes != null) {
            return this.nodes.get(index);
        }
        return null;
    }

    protected int addItem(Node arg) {
        int i = findNamePoint(arg.getNamespaceURI(), arg.getLocalName());
        if (i >= 0) {
            this.nodes.set(i, arg);
        } else {
            i = findNamePoint(arg.getNodeName(), 0);
            if (i >= 0) {
                this.nodes.add(i, arg);
            } else {
                i = (-1) - i;
                if (this.nodes == null) {
                    this.nodes = new ArrayList(5);
                }
                this.nodes.add(i, arg);
            }
        }
        return i;
    }

    protected ArrayList cloneMap(ArrayList list) {
        if (list == null) {
            list = new ArrayList(5);
        }
        list.clear();
        if (this.nodes != null) {
            int size = this.nodes.size();
            for (int i = 0; i < size; i++) {
                list.add(this.nodes.get(i));
            }
        }
        return list;
    }

    protected int getNamedItemIndex(String namespaceURI, String localName) {
        return findNamePoint(namespaceURI, localName);
    }

    public void removeAll() {
        if (this.nodes != null) {
            this.nodes.clear();
        }
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        if (this.nodes != null) {
            this.nodes = new ArrayList(this.nodes);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        List oldNodes = this.nodes;
        if (oldNodes != null) {
            try {
                this.nodes = new Vector(oldNodes);
            } catch (Throwable th) {
                this.nodes = oldNodes;
                throw th;
            }
        }
        out.defaultWriteObject();
        this.nodes = oldNodes;
    }
}
