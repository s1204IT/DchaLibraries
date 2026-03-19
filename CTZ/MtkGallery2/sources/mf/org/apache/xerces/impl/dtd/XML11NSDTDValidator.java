package mf.org.apache.xerces.impl.dtd;

import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.impl.msg.XMLMessageFormatter;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XNIException;

public class XML11NSDTDValidator extends XML11DTDValidator {
    private final QName fAttributeQName = new QName();

    @Override
    protected final void startNamespaceScope(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        int i;
        int length;
        char c;
        char c2;
        char c3;
        this.fNamespaceContext.pushContext();
        char c4 = 0;
        int i2 = 2;
        char c5 = 1;
        if (element.prefix == XMLSymbols.PREFIX_XMLNS) {
            this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementXMLNSPrefix", new Object[]{element.rawname}, (short) 2);
        }
        int length2 = attributes.getLength();
        int i3 = 0;
        while (i3 < length2) {
            int length3 = length2;
            String localpart = attributes.getLocalName(i3);
            String prefix = attributes.getPrefix(i3);
            if (prefix != XMLSymbols.PREFIX_XMLNS && (prefix != XMLSymbols.EMPTY_STRING || localpart != XMLSymbols.PREFIX_XMLNS)) {
                c = 1;
                c3 = 2;
                c2 = 0;
            } else {
                String uri = this.fSymbolTable.addSymbol(attributes.getValue(i3));
                if (prefix == XMLSymbols.PREFIX_XMLNS && localpart == XMLSymbols.PREFIX_XMLNS) {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXMLNS", new Object[]{attributes.getQName(i3)}, (short) 2);
                }
                if (uri == NamespaceContext.XMLNS_URI) {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXMLNS", new Object[]{attributes.getQName(i3)}, (short) 2);
                }
                if (localpart == XMLSymbols.PREFIX_XML) {
                    if (uri != NamespaceContext.XML_URI) {
                        this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXML", new Object[]{attributes.getQName(i3)}, (short) 2);
                    }
                } else {
                    if (uri == NamespaceContext.XML_URI) {
                        c = 1;
                        c2 = 0;
                        c3 = 2;
                        this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXML", new Object[]{attributes.getQName(i3)}, (short) 2);
                    }
                    this.fNamespaceContext.declarePrefix(localpart == XMLSymbols.PREFIX_XMLNS ? localpart : XMLSymbols.EMPTY_STRING, uri.length() == 0 ? uri : null);
                }
                c = 1;
                c3 = 2;
                c2 = 0;
                this.fNamespaceContext.declarePrefix(localpart == XMLSymbols.PREFIX_XMLNS ? localpart : XMLSymbols.EMPTY_STRING, uri.length() == 0 ? uri : null);
            }
            i3++;
            c5 = c;
            i2 = c3;
            c4 = c2;
            length2 = length3;
        }
        element.uri = this.fNamespaceContext.getURI(element.prefix != null ? element.prefix : XMLSymbols.EMPTY_STRING);
        if (element.prefix == null && element.uri != null) {
            element.prefix = XMLSymbols.EMPTY_STRING;
        }
        if (element.prefix != null && element.uri == null) {
            XMLErrorReporter xMLErrorReporter = this.fErrorReporter;
            Object[] objArr = new Object[i2];
            objArr[c4] = element.prefix;
            objArr[c5] = element.rawname;
            xMLErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementPrefixUnbound", objArr, i2);
        }
        int i4 = 0;
        while (true) {
            i = 3;
            if (i4 >= length2) {
                break;
            }
            int length4 = length2;
            attributes.getName(i4, this.fAttributeQName);
            String aprefix = this.fAttributeQName.prefix != null ? this.fAttributeQName.prefix : XMLSymbols.EMPTY_STRING;
            String arawname = this.fAttributeQName.rawname;
            if (arawname == XMLSymbols.PREFIX_XMLNS) {
                this.fAttributeQName.uri = this.fNamespaceContext.getURI(XMLSymbols.PREFIX_XMLNS);
                attributes.setName(i4, this.fAttributeQName);
            } else if (aprefix != XMLSymbols.EMPTY_STRING) {
                this.fAttributeQName.uri = this.fNamespaceContext.getURI(aprefix);
                if (this.fAttributeQName.uri == null) {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributePrefixUnbound", new Object[]{element.rawname, arawname, aprefix}, (short) 2);
                }
                attributes.setName(i4, this.fAttributeQName);
            }
            i4++;
            length2 = length4;
        }
        int attrCount = attributes.getLength();
        int i5 = 0;
        while (i5 < attrCount - 1) {
            String auri = attributes.getURI(i5);
            if (auri != null && auri != NamespaceContext.XMLNS_URI) {
                String alocalpart = attributes.getLocalName(i5);
                int j = i5 + 1;
                while (j < attrCount) {
                    String blocalpart = attributes.getLocalName(j);
                    String buri = attributes.getURI(j);
                    if (alocalpart != blocalpart || auri != buri) {
                        length = length2;
                    } else {
                        XMLErrorReporter xMLErrorReporter2 = this.fErrorReporter;
                        length = length2;
                        Object[] objArr2 = new Object[i];
                        objArr2[0] = element.rawname;
                        objArr2[1] = alocalpart;
                        objArr2[2] = auri;
                        xMLErrorReporter2.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributeNSNotUnique", objArr2, (short) 2);
                    }
                    j++;
                    length2 = length;
                    i = 3;
                }
            }
            i5++;
            length2 = length2;
            i = 3;
        }
    }

    @Override
    protected void endNamespaceScope(QName element, Augmentations augs, boolean isEmpty) throws XNIException {
        String eprefix = element.prefix != null ? element.prefix : XMLSymbols.EMPTY_STRING;
        element.uri = this.fNamespaceContext.getURI(eprefix);
        if (element.uri != null) {
            element.prefix = eprefix;
        }
        if (this.fDocumentHandler != null && !isEmpty) {
            this.fDocumentHandler.endElement(element, augs);
        }
        this.fNamespaceContext.popContext();
    }
}
