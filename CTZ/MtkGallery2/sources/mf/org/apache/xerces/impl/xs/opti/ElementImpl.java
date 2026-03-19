package mf.org.apache.xerces.impl.xs.opti;

import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;

public class ElementImpl extends DefaultElement {
    Attr[] attrs;
    int charOffset;
    int col;
    int column;
    String fAnnotation;
    String fSyntheticAnnotation;
    int line;
    int parentRow;
    int row;
    SchemaDOM schemaDOM;

    public ElementImpl(int line, int column, int offset) {
        this.row = -1;
        this.col = -1;
        this.parentRow = -1;
        this.nodeType = (short) 1;
        this.line = line;
        this.column = column;
        this.charOffset = offset;
    }

    public ElementImpl(int line, int column) {
        this(line, column, -1);
    }

    public ElementImpl(String prefix, String localpart, String rawname, String uri, int line, int column, int offset) {
        super(prefix, localpart, rawname, uri, (short) 1);
        this.row = -1;
        this.col = -1;
        this.parentRow = -1;
        this.line = line;
        this.column = column;
        this.charOffset = offset;
    }

    public ElementImpl(String prefix, String localpart, String rawname, String uri, int line, int column) {
        this(prefix, localpart, rawname, uri, line, column, -1);
    }

    @Override
    public Document getOwnerDocument() {
        return this.schemaDOM;
    }

    @Override
    public Node getParentNode() {
        return this.schemaDOM.relations[this.row][0];
    }

    @Override
    public boolean hasChildNodes() {
        if (this.parentRow == -1) {
            return false;
        }
        return true;
    }

    @Override
    public Node getFirstChild() {
        if (this.parentRow == -1) {
            return null;
        }
        return this.schemaDOM.relations[this.parentRow][1];
    }

    @Override
    public Node getLastChild() {
        if (this.parentRow == -1) {
            return null;
        }
        int i = 1;
        while (i < this.schemaDOM.relations[this.parentRow].length) {
            if (this.schemaDOM.relations[this.parentRow][i] != null) {
                i++;
            } else {
                return this.schemaDOM.relations[this.parentRow][i - 1];
            }
        }
        if (i == 1) {
            i++;
        }
        return this.schemaDOM.relations[this.parentRow][i - 1];
    }

    @Override
    public Node getPreviousSibling() {
        if (this.col == 1) {
            return null;
        }
        return this.schemaDOM.relations[this.row][this.col - 1];
    }

    @Override
    public Node getNextSibling() {
        if (this.col == this.schemaDOM.relations[this.row].length - 1) {
            return null;
        }
        return this.schemaDOM.relations[this.row][this.col + 1];
    }

    @Override
    public NamedNodeMap getAttributes() {
        return new NamedNodeMapImpl(this.attrs);
    }

    @Override
    public boolean hasAttributes() {
        return this.attrs.length != 0;
    }

    @Override
    public String getTagName() {
        return this.rawname;
    }

    @Override
    public String getAttribute(String name) {
        for (int i = 0; i < this.attrs.length; i++) {
            if (this.attrs[i].getName().equals(name)) {
                return this.attrs[i].getValue();
            }
        }
        return "";
    }

    @Override
    public Attr getAttributeNode(String name) {
        for (int i = 0; i < this.attrs.length; i++) {
            if (this.attrs[i].getName().equals(name)) {
                return this.attrs[i];
            }
        }
        return null;
    }

    @Override
    public String getAttributeNS(String namespaceURI, String localName) {
        for (int i = 0; i < this.attrs.length; i++) {
            if (this.attrs[i].getLocalName().equals(localName) && nsEquals(this.attrs[i].getNamespaceURI(), namespaceURI)) {
                return this.attrs[i].getValue();
            }
        }
        return "";
    }

    @Override
    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
        for (int i = 0; i < this.attrs.length; i++) {
            if (this.attrs[i].getName().equals(localName) && nsEquals(this.attrs[i].getNamespaceURI(), namespaceURI)) {
                return this.attrs[i];
            }
        }
        return null;
    }

    @Override
    public boolean hasAttribute(String name) {
        for (int i = 0; i < this.attrs.length; i++) {
            if (this.attrs[i].getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAttributeNS(String namespaceURI, String localName) {
        for (int i = 0; i < this.attrs.length; i++) {
            if (this.attrs[i].getName().equals(localName) && nsEquals(this.attrs[i].getNamespaceURI(), namespaceURI)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setAttribute(String name, String value) {
        for (int i = 0; i < this.attrs.length; i++) {
            if (this.attrs[i].getName().equals(name)) {
                this.attrs[i].setValue(value);
                return;
            }
        }
    }

    public int getLineNumber() {
        return this.line;
    }

    public int getColumnNumber() {
        return this.column;
    }

    public int getCharacterOffset() {
        return this.charOffset;
    }

    public String getAnnotation() {
        return this.fAnnotation;
    }

    public String getSyntheticAnnotation() {
        return this.fSyntheticAnnotation;
    }

    private static boolean nsEquals(String nsURI_1, String nsURI_2) {
        if (nsURI_1 == null) {
            return nsURI_2 == null;
        }
        return nsURI_1.equals(nsURI_2);
    }
}
