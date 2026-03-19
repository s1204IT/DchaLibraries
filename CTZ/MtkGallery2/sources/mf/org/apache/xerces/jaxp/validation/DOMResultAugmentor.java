package mf.org.apache.xerces.jaxp.validation;

import mf.javax.xml.transform.dom.DOMResult;
import mf.org.apache.xerces.dom.AttrImpl;
import mf.org.apache.xerces.dom.CoreDocumentImpl;
import mf.org.apache.xerces.dom.ElementImpl;
import mf.org.apache.xerces.dom.ElementNSImpl;
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
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.ProcessingInstruction;
import mf.org.w3c.dom.Text;

final class DOMResultAugmentor implements DOMDocumentHandler {
    private final QName fAttributeQName = new QName();
    private final DOMValidatorHelper fDOMValidatorHelper;
    private Document fDocument;
    private CoreDocumentImpl fDocumentImpl;
    private boolean fIgnoreChars;
    private boolean fStorePSVI;

    public DOMResultAugmentor(DOMValidatorHelper helper) {
        this.fDOMValidatorHelper = helper;
    }

    @Override
    public void setDOMResult(DOMResult result) {
        this.fIgnoreChars = false;
        if (result != null) {
            Node target = result.getNode();
            this.fDocument = target.getNodeType() == 9 ? (Document) target : target.getOwnerDocument();
            this.fDocumentImpl = this.fDocument instanceof CoreDocumentImpl ? (CoreDocumentImpl) this.fDocument : null;
            this.fStorePSVI = this.fDocument instanceof PSVIDocumentImpl;
            return;
        }
        this.fDocument = null;
        this.fDocumentImpl = null;
        this.fStorePSVI = false;
    }

    @Override
    public void doctypeDecl(DocumentType node) throws XNIException {
    }

    @Override
    public void characters(Text node) throws XNIException {
    }

    @Override
    public void cdata(CDATASection node) throws XNIException {
    }

    @Override
    public void comment(Comment node) throws XNIException {
    }

    @Override
    public void processingInstruction(ProcessingInstruction node) throws XNIException {
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
        Element currentElement = (Element) this.fDOMValidatorHelper.getCurrentElement();
        NamedNodeMap attrMap = currentElement.getAttributes();
        int oldLength = attrMap.getLength();
        if (this.fDocumentImpl != null) {
            for (int i = 0; i < oldLength; i++) {
                AttrImpl attr = (AttrImpl) attrMap.item(i);
                AttributePSVI attrPSVI = (AttributePSVI) attributes.getAugmentations(i).getItem(Constants.ATTRIBUTE_PSVI);
                if (attrPSVI != null && processAttributePSVI(attr, attrPSVI)) {
                    ((ElementImpl) currentElement).setIdAttributeNode(attr, true);
                }
            }
        }
        int newLength = attributes.getLength();
        if (newLength > oldLength) {
            if (this.fDocumentImpl == null) {
                for (int i2 = oldLength; i2 < newLength; i2++) {
                    attributes.getName(i2, this.fAttributeQName);
                    currentElement.setAttributeNS(this.fAttributeQName.uri, this.fAttributeQName.rawname, attributes.getValue(i2));
                }
                return;
            }
            for (int i3 = oldLength; i3 < newLength; i3++) {
                attributes.getName(i3, this.fAttributeQName);
                AttrImpl attr2 = (AttrImpl) this.fDocumentImpl.createAttributeNS(this.fAttributeQName.uri, this.fAttributeQName.rawname, this.fAttributeQName.localpart);
                attr2.setValue(attributes.getValue(i3));
                currentElement.setAttributeNodeNS(attr2);
                AttributePSVI attrPSVI2 = (AttributePSVI) attributes.getAugmentations(i3).getItem(Constants.ATTRIBUTE_PSVI);
                if (attrPSVI2 != null && processAttributePSVI(attr2, attrPSVI2)) {
                    ((ElementImpl) currentElement).setIdAttributeNode(attr2, true);
                }
                attr2.setSpecified(false);
            }
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
            Element currentElement = (Element) this.fDOMValidatorHelper.getCurrentElement();
            currentElement.appendChild(this.fDocument.createTextNode(text.toString()));
        }
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        characters(text, augs);
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        ElementPSVI elementPSVI;
        Node currentElement = this.fDOMValidatorHelper.getCurrentElement();
        if (augs != null && this.fDocumentImpl != null && (elementPSVI = (ElementPSVI) augs.getItem(Constants.ELEMENT_PSVI)) != null) {
            if (this.fStorePSVI) {
                ((PSVIElementNSImpl) currentElement).setPSVI(elementPSVI);
            }
            XSTypeDefinition type = elementPSVI.getMemberTypeDefinition();
            if (type == null) {
                type = elementPSVI.getTypeDefinition();
            }
            ((ElementNSImpl) currentElement).setType(type);
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
    }

    @Override
    public void setDocumentSource(XMLDocumentSource source) {
    }

    @Override
    public XMLDocumentSource getDocumentSource() {
        return null;
    }

    private boolean processAttributePSVI(AttrImpl attr, AttributePSVI attrPSVI) {
        if (this.fStorePSVI) {
            ((PSVIAttrNSImpl) attr).setPSVI(attrPSVI);
        }
        Object type = attrPSVI.getMemberTypeDefinition();
        if (type == null) {
            Object type2 = attrPSVI.getTypeDefinition();
            if (type2 != null) {
                attr.setType(type2);
                return ((XSSimpleType) type2).isIDType();
            }
            return false;
        }
        attr.setType(type);
        return ((XSSimpleType) type).isIDType();
    }
}
