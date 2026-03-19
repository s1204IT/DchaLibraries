package mf.org.apache.xerces.impl.xs.opti;

import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.CDATASection;
import mf.org.w3c.dom.Comment;
import mf.org.w3c.dom.DOMConfiguration;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.DOMImplementation;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.DocumentFragment;
import mf.org.w3c.dom.DocumentType;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.EntityReference;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.NodeList;
import mf.org.w3c.dom.ProcessingInstruction;
import mf.org.w3c.dom.Text;

public class DefaultDocument extends NodeImpl implements Document {
    private String fDocumentURI = null;

    public DefaultDocument() {
        this.nodeType = (short) 9;
    }

    @Override
    public String getNodeName() {
        return "#document";
    }

    @Override
    public DocumentType getDoctype() {
        return null;
    }

    @Override
    public DOMImplementation getImplementation() {
        return null;
    }

    @Override
    public Element getDocumentElement() {
        return null;
    }

    public NodeList getElementsByTagName(String tagname) {
        return null;
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        return null;
    }

    public Element getElementById(String elementId) {
        return null;
    }

    @Override
    public Node importNode(Node importedNode, boolean deep) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Element createElement(String tagName) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public DocumentFragment createDocumentFragment() {
        return null;
    }

    @Override
    public Text createTextNode(String data) {
        return null;
    }

    @Override
    public Comment createComment(String data) {
        return null;
    }

    @Override
    public CDATASection createCDATASection(String data) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Attr createAttribute(String name) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public EntityReference createEntityReference(String name) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public String getInputEncoding() {
        return null;
    }

    @Override
    public String getXmlEncoding() {
        return null;
    }

    public boolean getXmlStandalone() {
        throw new DOMException((short) 9, "Method not supported");
    }

    public void setXmlStandalone(boolean standalone) {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public String getXmlVersion() {
        return null;
    }

    public void setXmlVersion(String version) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public boolean getStrictErrorChecking() {
        return false;
    }

    public void setStrictErrorChecking(boolean strictErrorChecking) {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public String getDocumentURI() {
        return this.fDocumentURI;
    }

    public void setDocumentURI(String documentURI) {
        this.fDocumentURI = documentURI;
    }

    @Override
    public Node adoptNode(Node source) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public void normalizeDocument() {
        throw new DOMException((short) 9, "Method not supported");
    }

    public DOMConfiguration getDomConfig() {
        throw new DOMException((short) 9, "Method not supported");
    }

    public Node renameNode(Node n, String namespaceURI, String name) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }
}
