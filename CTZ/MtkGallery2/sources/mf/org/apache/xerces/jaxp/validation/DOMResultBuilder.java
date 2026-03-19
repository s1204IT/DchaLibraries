package mf.org.apache.xerces.jaxp.validation;

import java.util.ArrayList;
import mf.javax.xml.transform.dom.DOMResult;
import mf.org.apache.xerces.dom.AttrImpl;
import mf.org.apache.xerces.dom.CoreDocumentImpl;
import mf.org.apache.xerces.dom.DOMMessageFormatter;
import mf.org.apache.xerces.dom.DocumentTypeImpl;
import mf.org.apache.xerces.dom.ElementImpl;
import mf.org.apache.xerces.dom.ElementNSImpl;
import mf.org.apache.xerces.dom.EntityImpl;
import mf.org.apache.xerces.dom.NotationImpl;
import mf.org.apache.xerces.dom.PSVIAttrNSImpl;
import mf.org.apache.xerces.dom.PSVIDocumentImpl;
import mf.org.apache.xerces.dom.PSVIElementNSImpl;
import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;
import mf.org.apache.xerces.xs.AttributePSVI;
import mf.org.apache.xerces.xs.ElementPSVI;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.CDATASection;
import mf.org.w3c.dom.Comment;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.DocumentType;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.Entity;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.Notation;
import mf.org.w3c.dom.ProcessingInstruction;
import mf.org.w3c.dom.Text;

final class DOMResultBuilder implements DOMDocumentHandler {
    private static final int[] kidOK = new int[13];
    private Node fCurrentNode;
    private Document fDocument;
    private CoreDocumentImpl fDocumentImpl;
    private Node fFragmentRoot;
    private boolean fIgnoreChars;
    private Node fNextSibling;
    private boolean fStorePSVI;
    private Node fTarget;
    private final ArrayList fTargetChildren = new ArrayList();
    private final QName fAttributeQName = new QName();

    static {
        kidOK[9] = 1410;
        int[] iArr = kidOK;
        int[] iArr2 = kidOK;
        int[] iArr3 = kidOK;
        kidOK[1] = 442;
        iArr3[5] = 442;
        iArr2[6] = 442;
        iArr[11] = 442;
        kidOK[2] = 40;
        kidOK[10] = 0;
        kidOK[7] = 0;
        kidOK[8] = 0;
        kidOK[3] = 0;
        kidOK[4] = 0;
        kidOK[12] = 0;
    }

    @Override
    public void setDOMResult(DOMResult result) {
        this.fCurrentNode = null;
        this.fFragmentRoot = null;
        this.fIgnoreChars = false;
        this.fTargetChildren.clear();
        if (result != null) {
            this.fTarget = result.getNode();
            this.fNextSibling = result.getNextSibling();
            this.fDocument = this.fTarget.getNodeType() == 9 ? (Document) this.fTarget : this.fTarget.getOwnerDocument();
            this.fDocumentImpl = this.fDocument instanceof CoreDocumentImpl ? (CoreDocumentImpl) this.fDocument : null;
            this.fStorePSVI = this.fDocument instanceof PSVIDocumentImpl;
            return;
        }
        this.fTarget = null;
        this.fNextSibling = null;
        this.fDocument = null;
        this.fDocumentImpl = null;
        this.fStorePSVI = false;
    }

    @Override
    public void doctypeDecl(DocumentType node) throws XNIException {
        if (this.fDocumentImpl != null) {
            DocumentType docType = this.fDocumentImpl.createDocumentType(node.getName(), node.getPublicId(), node.getSystemId());
            String internalSubset = node.getInternalSubset();
            if (internalSubset != null) {
                ((DocumentTypeImpl) docType).setInternalSubset(internalSubset);
            }
            NamedNodeMap oldMap = node.getEntities();
            NamedNodeMap newMap = docType.getEntities();
            int length = oldMap.getLength();
            for (int i = 0; i < length; i++) {
                Entity oldEntity = (Entity) oldMap.item(i);
                EntityImpl newEntity = (EntityImpl) this.fDocumentImpl.createEntity(oldEntity.getNodeName());
                newEntity.setPublicId(oldEntity.getPublicId());
                newEntity.setSystemId(oldEntity.getSystemId());
                newEntity.setNotationName(oldEntity.getNotationName());
                newMap.setNamedItem(newEntity);
            }
            NamedNodeMap oldMap2 = node.getNotations();
            NamedNodeMap newMap2 = docType.getNotations();
            int length2 = oldMap2.getLength();
            for (int i2 = 0; i2 < length2; i2++) {
                Notation oldNotation = (Notation) oldMap2.item(i2);
                NotationImpl newNotation = (NotationImpl) this.fDocumentImpl.createNotation(oldNotation.getNodeName());
                newNotation.setPublicId(oldNotation.getPublicId());
                newNotation.setSystemId(oldNotation.getSystemId());
                newMap2.setNamedItem(newNotation);
            }
            append(docType);
        }
    }

    @Override
    public void characters(Text node) throws XNIException {
        append(this.fDocument.createTextNode(node.getNodeValue()));
    }

    @Override
    public void cdata(CDATASection node) throws XNIException {
        append(this.fDocument.createCDATASection(node.getNodeValue()));
    }

    @Override
    public void comment(Comment node) throws XNIException {
        append(this.fDocument.createComment(node.getNodeValue()));
    }

    @Override
    public void processingInstruction(ProcessingInstruction node) throws XNIException {
        append(this.fDocument.createProcessingInstruction(node.getTarget(), node.getData()));
    }

    @Override
    public void setIgnoringCharacters(boolean ignore) {
        this.fIgnoreChars = ignore;
    }

    @Override
    public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        Element elem;
        int attrCount = attributes.getLength();
        if (this.fDocumentImpl == null) {
            elem = this.fDocument.createElementNS(element.uri, element.rawname);
            for (int i = 0; i < attrCount; i++) {
                attributes.getName(i, this.fAttributeQName);
                elem.setAttributeNS(this.fAttributeQName.uri, this.fAttributeQName.rawname, attributes.getValue(i));
            }
        } else {
            elem = this.fDocumentImpl.createElementNS(element.uri, element.rawname, element.localpart);
            for (int i2 = 0; i2 < attrCount; i2++) {
                attributes.getName(i2, this.fAttributeQName);
                AttrImpl attr = (AttrImpl) this.fDocumentImpl.createAttributeNS(this.fAttributeQName.uri, this.fAttributeQName.rawname, this.fAttributeQName.localpart);
                attr.setValue(attributes.getValue(i2));
                elem.setAttributeNodeNS(attr);
                AttributePSVI attrPSVI = (AttributePSVI) attributes.getAugmentations(i2).getItem(Constants.ATTRIBUTE_PSVI);
                if (attrPSVI != null) {
                    if (this.fStorePSVI) {
                        ((PSVIAttrNSImpl) attr).setPSVI(attrPSVI);
                    }
                    Object type = attrPSVI.getMemberTypeDefinition();
                    if (type == null) {
                        Object type2 = attrPSVI.getTypeDefinition();
                        if (type2 != null) {
                            attr.setType(type2);
                            if (((XSSimpleType) type2).isIDType()) {
                                ((ElementImpl) elem).setIdAttributeNode(attr, true);
                            }
                        }
                    } else {
                        attr.setType(type);
                        if (((XSSimpleType) type).isIDType()) {
                            ((ElementImpl) elem).setIdAttributeNode(attr, true);
                        }
                    }
                }
                attr.setSpecified(attributes.isSpecified(i2));
            }
        }
        append(elem);
        this.fCurrentNode = elem;
        if (this.fFragmentRoot == null) {
            this.fFragmentRoot = elem;
        }
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        startElement(element, attributes, augs);
        endElement(element, augs);
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        if (!this.fIgnoreChars) {
            append(this.fDocument.createTextNode(text.toString()));
        }
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        characters(text, augs);
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        ElementPSVI elementPSVI;
        if (augs != null && this.fDocumentImpl != null && (elementPSVI = (ElementPSVI) augs.getItem(Constants.ELEMENT_PSVI)) != null) {
            if (this.fStorePSVI) {
                ((PSVIElementNSImpl) this.fCurrentNode).setPSVI(elementPSVI);
            }
            XSTypeDefinition type = elementPSVI.getMemberTypeDefinition();
            if (type == null) {
                type = elementPSVI.getTypeDefinition();
            }
            ((ElementNSImpl) this.fCurrentNode).setType(type);
        }
        if (this.fCurrentNode == this.fFragmentRoot) {
            this.fCurrentNode = null;
            this.fFragmentRoot = null;
        } else {
            this.fCurrentNode = this.fCurrentNode.getParentNode();
        }
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
        int length = this.fTargetChildren.size();
        if (this.fNextSibling == null) {
            for (int i = 0; i < length; i++) {
                this.fTarget.appendChild((Node) this.fTargetChildren.get(i));
            }
            return;
        }
        for (int i2 = 0; i2 < length; i2++) {
            this.fTarget.insertBefore((Node) this.fTargetChildren.get(i2), this.fNextSibling);
        }
    }

    @Override
    public void setDocumentSource(XMLDocumentSource source) {
    }

    @Override
    public XMLDocumentSource getDocumentSource() {
        return null;
    }

    private void append(Node node) throws XNIException {
        if (this.fCurrentNode != null) {
            this.fCurrentNode.appendChild(node);
        } else {
            if ((kidOK[this.fTarget.getNodeType()] & (1 << node.getNodeType())) == 0) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null);
                throw new XNIException(msg);
            }
            this.fTargetChildren.add(node);
        }
    }
}
