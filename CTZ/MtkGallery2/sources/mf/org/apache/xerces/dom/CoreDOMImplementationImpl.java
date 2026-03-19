package mf.org.apache.xerces.dom;

import java.lang.ref.SoftReference;
import mf.org.apache.xerces.impl.RevalidationHandler;
import mf.org.apache.xerces.impl.dtd.XMLDTDLoader;
import mf.org.apache.xerces.parsers.DOMParserImpl;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xml.serialize.DOMSerializerImpl;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.DOMImplementation;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.DocumentType;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.ls.DOMImplementationLS;
import mf.org.w3c.dom.ls.LSInput;
import mf.org.w3c.dom.ls.LSOutput;
import mf.org.w3c.dom.ls.LSParser;
import mf.org.w3c.dom.ls.LSSerializer;

public class CoreDOMImplementationImpl implements DOMImplementation, DOMImplementationLS {
    private static final int SIZE = 2;
    static final CoreDOMImplementationImpl singleton = new CoreDOMImplementationImpl();
    private SoftReference[] schemaValidators = new SoftReference[2];
    private SoftReference[] xml10DTDValidators = new SoftReference[2];
    private SoftReference[] xml11DTDValidators = new SoftReference[2];
    private int freeSchemaValidatorIndex = -1;
    private int freeXML10DTDValidatorIndex = -1;
    private int freeXML11DTDValidatorIndex = -1;
    private int schemaValidatorsCurrentSize = 2;
    private int xml10DTDValidatorsCurrentSize = 2;
    private int xml11DTDValidatorsCurrentSize = 2;
    private SoftReference[] xml10DTDLoaders = new SoftReference[2];
    private SoftReference[] xml11DTDLoaders = new SoftReference[2];
    private int freeXML10DTDLoaderIndex = -1;
    private int freeXML11DTDLoaderIndex = -1;
    private int xml10DTDLoaderCurrentSize = 2;
    private int xml11DTDLoaderCurrentSize = 2;
    private int docAndDoctypeCounter = 0;

    public static DOMImplementation getDOMImplementation() {
        return singleton;
    }

    @Override
    public boolean hasFeature(String feature, String version) {
        boolean anyVersion = version == null || version.length() == 0;
        if (feature.equalsIgnoreCase("+XPath") && (anyVersion || version.equals("3.0"))) {
            try {
                Class xpathClass = ObjectFactory.findProviderClass("org.apache.xpath.domapi.XPathEvaluatorImpl", ObjectFactory.findClassLoader(), true);
                Class<?>[] interfaces = xpathClass.getInterfaces();
                for (int i = 0; i < interfaces.length && !interfaces[i].getName().equals("org.w3c.dom.xpath.XPathEvaluator"); i++) {
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        if (feature.startsWith("+")) {
            feature = feature.substring(1);
        }
        return (feature.equalsIgnoreCase("Core") && (anyVersion || version.equals("1.0") || version.equals("2.0") || version.equals("3.0"))) || (feature.equalsIgnoreCase("XML") && (anyVersion || version.equals("1.0") || version.equals("2.0") || version.equals("3.0"))) || ((feature.equalsIgnoreCase("XMLVersion") && (anyVersion || version.equals("1.0") || version.equals("1.1"))) || ((feature.equalsIgnoreCase("LS") && (anyVersion || version.equals("3.0"))) || (feature.equalsIgnoreCase("ElementTraversal") && (anyVersion || version.equals("1.0")))));
    }

    public DocumentType createDocumentType(String qualifiedName, String publicID, String systemID) {
        checkQName(qualifiedName);
        return new DocumentTypeImpl(null, qualifiedName, publicID, systemID);
    }

    final void checkQName(String qname) {
        int index = qname.indexOf(58);
        int lastIndex = qname.lastIndexOf(58);
        int length = qname.length();
        if (index == 0 || index == length - 1 || lastIndex != index) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
            throw new DOMException((short) 14, msg);
        }
        int start = 0;
        if (index > 0) {
            if (!XMLChar.isNCNameStart(qname.charAt(0))) {
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
                throw new DOMException((short) 5, msg2);
            }
            for (int i = 1; i < index; i++) {
                if (!XMLChar.isNCName(qname.charAt(i))) {
                    String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
                    throw new DOMException((short) 5, msg3);
                }
            }
            start = index + 1;
        }
        if (!XMLChar.isNCNameStart(qname.charAt(start))) {
            String msg4 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException((short) 5, msg4);
        }
        for (int i2 = start + 1; i2 < length; i2++) {
            if (!XMLChar.isNCName(qname.charAt(i2))) {
                String msg5 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
                throw new DOMException((short) 5, msg5);
            }
        }
    }

    public Document createDocument(String namespaceURI, String qualifiedName, DocumentType doctype) throws DOMException {
        if (doctype != null && doctype.getOwnerDocument() != null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
            throw new DOMException((short) 4, msg);
        }
        CoreDocumentImpl doc = createDocument(doctype);
        if (qualifiedName != null || namespaceURI != null) {
            Element e = doc.createElementNS(namespaceURI, qualifiedName);
            doc.appendChild(e);
        }
        return doc;
    }

    protected CoreDocumentImpl createDocument(DocumentType doctype) {
        return new CoreDocumentImpl(doctype);
    }

    public Object getFeature(String feature, String version) {
        if (singleton.hasFeature(feature, version)) {
            if (feature.equalsIgnoreCase("+XPath")) {
                try {
                    Class xpathClass = ObjectFactory.findProviderClass("org.apache.xpath.domapi.XPathEvaluatorImpl", ObjectFactory.findClassLoader(), true);
                    for (Class<?> cls : xpathClass.getInterfaces()) {
                        if (cls.getName().equals("org.w3c.dom.xpath.XPathEvaluator")) {
                            return xpathClass.newInstance();
                        }
                    }
                } catch (Exception e) {
                    return null;
                }
            } else {
                return singleton;
            }
        }
        return null;
    }

    public LSParser createLSParser(short mode, String schemaType) throws DOMException {
        if (mode != 1 || (schemaType != null && !"http://www.w3.org/2001/XMLSchema".equals(schemaType) && !XMLGrammarDescription.XML_DTD.equals(schemaType))) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
            throw new DOMException((short) 9, msg);
        }
        if (schemaType != null && schemaType.equals(XMLGrammarDescription.XML_DTD)) {
            return new DOMParserImpl("mf.org.apache.xerces.parsers.DTDConfiguration", schemaType);
        }
        return new DOMParserImpl("mf.org.apache.xerces.parsers.XIncludeAwareParserConfiguration", schemaType);
    }

    @Override
    public LSSerializer createLSSerializer() {
        try {
            Class serializerClass = ObjectFactory.findProviderClass("org.apache.xml.serializer.dom3.LSSerializerImpl", ObjectFactory.findClassLoader(), true);
            return (LSSerializer) serializerClass.newInstance();
        } catch (Exception e) {
            return new DOMSerializerImpl();
        }
    }

    public LSInput createLSInput() {
        return new DOMInputImpl();
    }

    synchronized RevalidationHandler getValidator(String schemaType, String xmlVersion) {
        if (schemaType == "http://www.w3.org/2001/XMLSchema") {
            while (this.freeSchemaValidatorIndex >= 0) {
                SoftReference ref = this.schemaValidators[this.freeSchemaValidatorIndex];
                RevalidationHandlerHolder holder = (RevalidationHandlerHolder) ref.get();
                if (holder != null && holder.handler != null) {
                    RevalidationHandler val = holder.handler;
                    holder.handler = null;
                    this.freeSchemaValidatorIndex--;
                    return val;
                }
                SoftReference[] softReferenceArr = this.schemaValidators;
                int i = this.freeSchemaValidatorIndex;
                this.freeSchemaValidatorIndex = i - 1;
                softReferenceArr[i] = null;
            }
            return (RevalidationHandler) ObjectFactory.newInstance("mf.org.apache.xerces.impl.xs.XMLSchemaValidator", ObjectFactory.findClassLoader(), true);
        }
        if (schemaType != XMLGrammarDescription.XML_DTD) {
            return null;
        }
        if ("1.1".equals(xmlVersion)) {
            while (this.freeXML11DTDValidatorIndex >= 0) {
                SoftReference ref2 = this.xml11DTDValidators[this.freeXML11DTDValidatorIndex];
                RevalidationHandlerHolder holder2 = (RevalidationHandlerHolder) ref2.get();
                if (holder2 != null && holder2.handler != null) {
                    RevalidationHandler val2 = holder2.handler;
                    holder2.handler = null;
                    this.freeXML11DTDValidatorIndex--;
                    return val2;
                }
                SoftReference[] softReferenceArr2 = this.xml11DTDValidators;
                int i2 = this.freeXML11DTDValidatorIndex;
                this.freeXML11DTDValidatorIndex = i2 - 1;
                softReferenceArr2[i2] = null;
            }
            return (RevalidationHandler) ObjectFactory.newInstance("mf.org.apache.xerces.impl.dtd.XML11DTDValidator", ObjectFactory.findClassLoader(), true);
        }
        while (this.freeXML10DTDValidatorIndex >= 0) {
            SoftReference ref3 = this.xml10DTDValidators[this.freeXML10DTDValidatorIndex];
            RevalidationHandlerHolder holder3 = (RevalidationHandlerHolder) ref3.get();
            if (holder3 != null && holder3.handler != null) {
                RevalidationHandler val3 = holder3.handler;
                holder3.handler = null;
                this.freeXML10DTDValidatorIndex--;
                return val3;
            }
            SoftReference[] softReferenceArr3 = this.xml10DTDValidators;
            int i3 = this.freeXML10DTDValidatorIndex;
            this.freeXML10DTDValidatorIndex = i3 - 1;
            softReferenceArr3[i3] = null;
        }
        return (RevalidationHandler) ObjectFactory.newInstance("mf.org.apache.xerces.impl.dtd.XMLDTDValidator", ObjectFactory.findClassLoader(), true);
    }

    synchronized void releaseValidator(String schemaType, String xmlVersion, RevalidationHandler validator) {
        RevalidationHandlerHolder holder;
        RevalidationHandlerHolder holder2;
        RevalidationHandlerHolder holder3;
        if (schemaType == "http://www.w3.org/2001/XMLSchema") {
            this.freeSchemaValidatorIndex++;
            if (this.schemaValidators.length == this.freeSchemaValidatorIndex) {
                this.schemaValidatorsCurrentSize += 2;
                SoftReference[] newarray = new SoftReference[this.schemaValidatorsCurrentSize];
                System.arraycopy(this.schemaValidators, 0, newarray, 0, this.schemaValidators.length);
                this.schemaValidators = newarray;
            }
            SoftReference ref = this.schemaValidators[this.freeSchemaValidatorIndex];
            if (ref != null && (holder3 = (RevalidationHandlerHolder) ref.get()) != null) {
                holder3.handler = validator;
                return;
            }
            this.schemaValidators[this.freeSchemaValidatorIndex] = new SoftReference(new RevalidationHandlerHolder(validator));
        } else if (schemaType == XMLGrammarDescription.XML_DTD) {
            if ("1.1".equals(xmlVersion)) {
                this.freeXML11DTDValidatorIndex++;
                if (this.xml11DTDValidators.length == this.freeXML11DTDValidatorIndex) {
                    this.xml11DTDValidatorsCurrentSize += 2;
                    SoftReference[] newarray2 = new SoftReference[this.xml11DTDValidatorsCurrentSize];
                    System.arraycopy(this.xml11DTDValidators, 0, newarray2, 0, this.xml11DTDValidators.length);
                    this.xml11DTDValidators = newarray2;
                }
                SoftReference ref2 = this.xml11DTDValidators[this.freeXML11DTDValidatorIndex];
                if (ref2 != null && (holder2 = (RevalidationHandlerHolder) ref2.get()) != null) {
                    holder2.handler = validator;
                    return;
                }
                this.xml11DTDValidators[this.freeXML11DTDValidatorIndex] = new SoftReference(new RevalidationHandlerHolder(validator));
            } else {
                this.freeXML10DTDValidatorIndex++;
                if (this.xml10DTDValidators.length == this.freeXML10DTDValidatorIndex) {
                    this.xml10DTDValidatorsCurrentSize += 2;
                    SoftReference[] newarray3 = new SoftReference[this.xml10DTDValidatorsCurrentSize];
                    System.arraycopy(this.xml10DTDValidators, 0, newarray3, 0, this.xml10DTDValidators.length);
                    this.xml10DTDValidators = newarray3;
                }
                SoftReference ref3 = this.xml10DTDValidators[this.freeXML10DTDValidatorIndex];
                if (ref3 != null && (holder = (RevalidationHandlerHolder) ref3.get()) != null) {
                    holder.handler = validator;
                    return;
                }
                this.xml10DTDValidators[this.freeXML10DTDValidatorIndex] = new SoftReference(new RevalidationHandlerHolder(validator));
            }
        }
    }

    final synchronized XMLDTDLoader getDTDLoader(String xmlVersion) {
        if ("1.1".equals(xmlVersion)) {
            while (this.freeXML11DTDLoaderIndex >= 0) {
                SoftReference ref = this.xml11DTDLoaders[this.freeXML11DTDLoaderIndex];
                XMLDTDLoaderHolder holder = (XMLDTDLoaderHolder) ref.get();
                if (holder != null && holder.loader != null) {
                    XMLDTDLoader val = holder.loader;
                    holder.loader = null;
                    this.freeXML11DTDLoaderIndex--;
                    return val;
                }
                SoftReference[] softReferenceArr = this.xml11DTDLoaders;
                int i = this.freeXML11DTDLoaderIndex;
                this.freeXML11DTDLoaderIndex = i - 1;
                softReferenceArr[i] = null;
            }
            return (XMLDTDLoader) ObjectFactory.newInstance("mf.org.apache.xerces.impl.dtd.XML11DTDProcessor", ObjectFactory.findClassLoader(), true);
        }
        while (this.freeXML10DTDLoaderIndex >= 0) {
            SoftReference ref2 = this.xml10DTDLoaders[this.freeXML10DTDLoaderIndex];
            XMLDTDLoaderHolder holder2 = (XMLDTDLoaderHolder) ref2.get();
            if (holder2 != null && holder2.loader != null) {
                XMLDTDLoader val2 = holder2.loader;
                holder2.loader = null;
                this.freeXML10DTDLoaderIndex--;
                return val2;
            }
            SoftReference[] softReferenceArr2 = this.xml10DTDLoaders;
            int i2 = this.freeXML10DTDLoaderIndex;
            this.freeXML10DTDLoaderIndex = i2 - 1;
            softReferenceArr2[i2] = null;
        }
        return new XMLDTDLoader();
    }

    final synchronized void releaseDTDLoader(String xmlVersion, XMLDTDLoader loader) {
        XMLDTDLoaderHolder holder;
        XMLDTDLoaderHolder holder2;
        if ("1.1".equals(xmlVersion)) {
            this.freeXML11DTDLoaderIndex++;
            if (this.xml11DTDLoaders.length == this.freeXML11DTDLoaderIndex) {
                this.xml11DTDLoaderCurrentSize += 2;
                SoftReference[] newarray = new SoftReference[this.xml11DTDLoaderCurrentSize];
                System.arraycopy(this.xml11DTDLoaders, 0, newarray, 0, this.xml11DTDLoaders.length);
                this.xml11DTDLoaders = newarray;
            }
            SoftReference ref = this.xml11DTDLoaders[this.freeXML11DTDLoaderIndex];
            if (ref != null && (holder2 = (XMLDTDLoaderHolder) ref.get()) != null) {
                holder2.loader = loader;
                return;
            }
            this.xml11DTDLoaders[this.freeXML11DTDLoaderIndex] = new SoftReference(new XMLDTDLoaderHolder(loader));
        } else {
            this.freeXML10DTDLoaderIndex++;
            if (this.xml10DTDLoaders.length == this.freeXML10DTDLoaderIndex) {
                this.xml10DTDLoaderCurrentSize += 2;
                SoftReference[] newarray2 = new SoftReference[this.xml10DTDLoaderCurrentSize];
                System.arraycopy(this.xml10DTDLoaders, 0, newarray2, 0, this.xml10DTDLoaders.length);
                this.xml10DTDLoaders = newarray2;
            }
            SoftReference ref2 = this.xml10DTDLoaders[this.freeXML10DTDLoaderIndex];
            if (ref2 != null && (holder = (XMLDTDLoaderHolder) ref2.get()) != null) {
                holder.loader = loader;
                return;
            }
            this.xml10DTDLoaders[this.freeXML10DTDLoaderIndex] = new SoftReference(new XMLDTDLoaderHolder(loader));
        }
    }

    protected synchronized int assignDocumentNumber() {
        int i;
        i = this.docAndDoctypeCounter + 1;
        this.docAndDoctypeCounter = i;
        return i;
    }

    protected synchronized int assignDocTypeNumber() {
        int i;
        i = this.docAndDoctypeCounter + 1;
        this.docAndDoctypeCounter = i;
        return i;
    }

    public LSOutput createLSOutput() {
        return new DOMOutputImpl();
    }

    static final class RevalidationHandlerHolder {
        RevalidationHandler handler;

        RevalidationHandlerHolder(RevalidationHandler handler) {
            this.handler = handler;
        }
    }

    static final class XMLDTDLoaderHolder {
        XMLDTDLoader loader;

        XMLDTDLoaderHolder(XMLDTDLoader loader) {
            this.loader = loader;
        }
    }
}
