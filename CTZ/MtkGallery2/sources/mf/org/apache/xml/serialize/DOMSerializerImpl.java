package mf.org.apache.xml.serialize;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import mf.org.apache.xerces.dom.CoreDocumentImpl;
import mf.org.apache.xerces.dom.DOMErrorImpl;
import mf.org.apache.xerces.dom.DOMLocatorImpl;
import mf.org.apache.xerces.dom.DOMMessageFormatter;
import mf.org.apache.xerces.dom.DOMNormalizer;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.util.NamespaceSupport;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XML11Char;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.xs.XSSimpleTypeDefinition;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.Comment;
import mf.org.w3c.dom.DOMConfiguration;
import mf.org.w3c.dom.DOMErrorHandler;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.DocumentFragment;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.ProcessingInstruction;
import mf.org.w3c.dom.ls.LSException;
import mf.org.w3c.dom.ls.LSSerializer;

public class DOMSerializerImpl implements DOMConfiguration, LSSerializer {
    protected short features;
    private XML11Serializer xml11Serializer;
    private DOMErrorHandler fErrorHandler = null;
    private final DOMErrorImpl fError = new DOMErrorImpl();
    private final DOMLocatorImpl fLocator = new DOMLocatorImpl();
    private XMLSerializer serializer = new XMLSerializer();

    public DOMSerializerImpl() {
        this.features = (short) 0;
        this.features = (short) (this.features | 1);
        this.features = (short) (this.features | 4);
        this.features = (short) (this.features | 32);
        this.features = (short) (this.features | 8);
        this.features = (short) (this.features | 16);
        this.features = (short) (this.features | 2);
        this.features = (short) (this.features | XSSimpleTypeDefinition.FACET_TOTALDIGITS);
        this.features = (short) (this.features | XSSimpleTypeDefinition.FACET_FRACTIONDIGITS);
        this.features = (short) (this.features | 64);
        this.features = (short) (this.features | XSSimpleTypeDefinition.FACET_MININCLUSIVE);
        initSerializer(this.serializer);
    }

    @Override
    public String writeToString(Node wnode) throws LSException, DOMException {
        XMLSerializer ser;
        String ver = _getXmlVersion(wnode);
        if (ver != null && ver.equals("1.1")) {
            if (this.xml11Serializer == null) {
                this.xml11Serializer = new XML11Serializer();
                initSerializer(this.xml11Serializer);
            }
            copySettings(this.serializer, this.xml11Serializer);
            ser = this.xml11Serializer;
        } else {
            ser = this.serializer;
        }
        StringWriter destination = new StringWriter();
        try {
            try {
                try {
                    prepareForSerialization(ser, wnode);
                    ser._format.setEncoding("UTF-16");
                    ser.setOutputCharStream(destination);
                    if (wnode.getNodeType() == 9) {
                        ser.serialize((Document) wnode);
                    } else if (wnode.getNodeType() == 11) {
                        ser.serialize((DocumentFragment) wnode);
                    } else if (wnode.getNodeType() == 1) {
                        ser.serialize((Element) wnode);
                    } else {
                        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "unable-to-serialize-node", null);
                        if (ser.fDOMErrorHandler != null) {
                            DOMErrorImpl error = new DOMErrorImpl();
                            error.fType = "unable-to-serialize-node";
                            error.fMessage = msg;
                            error.fSeverity = (short) 3;
                            ser.fDOMErrorHandler.handleError(error);
                        }
                        throw new LSException((short) 82, msg);
                    }
                    ser.clearDocumentState();
                    return destination.toString();
                } catch (IOException ioe) {
                    throw new DOMException((short) 2, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "STRING_TOO_LONG", new Object[]{ioe.getMessage()}));
                }
            } catch (LSException lse) {
                throw lse;
            } catch (RuntimeException e) {
                if (e != DOMNormalizer.abort) {
                    throw ((LSException) DOMUtil.createLSException((short) 82, e).fillInStackTrace());
                }
                ser.clearDocumentState();
                return null;
            }
        } catch (Throwable th) {
            ser.clearDocumentState();
            throw th;
        }
    }

    private void initSerializer(XMLSerializer ser) {
        ser.fNSBinder = new NamespaceSupport();
        ser.fLocalNSBinder = new NamespaceSupport();
        ser.fSymbolTable = new SymbolTable();
    }

    private void copySettings(XMLSerializer src, XMLSerializer dest) {
        dest.fDOMErrorHandler = this.fErrorHandler;
        dest._format.setEncoding(src._format.getEncoding());
        dest._format.setLineSeparator(src._format.getLineSeparator());
        dest.fDOMFilter = src.fDOMFilter;
    }

    private void prepareForSerialization(XMLSerializer ser, Node node) {
        Document document;
        ser.reset();
        ser.features = this.features;
        ser.fDOMErrorHandler = this.fErrorHandler;
        ser.fNamespaces = (this.features & 1) != 0;
        ser.fNamespacePrefixes = (this.features & XSSimpleTypeDefinition.FACET_TOTALDIGITS) != 0;
        ser._format.setIndenting((this.features & XSSimpleTypeDefinition.FACET_ENUMERATION) != 0);
        ser._format.setOmitComments((this.features & 32) == 0);
        ser._format.setOmitXMLDeclaration((this.features & XSSimpleTypeDefinition.FACET_MININCLUSIVE) == 0);
        if ((this.features & 2) != 0) {
            boolean verifyNames = true;
            if (node.getNodeType() == 9) {
                document = (Document) node;
            } else {
                document = node.getOwnerDocument();
            }
            try {
                Method versionChanged = document.getClass().getMethod("isXMLVersionChanged()", new Class[0]);
                if (versionChanged != null) {
                    verifyNames = ((Boolean) versionChanged.invoke(document, null)).booleanValue();
                }
            } catch (Exception e) {
            }
            if (node.getFirstChild() != null) {
                while (node != null) {
                    verify(node, verifyNames, false);
                    Node next = node.getFirstChild();
                    while (true) {
                        if (next == null) {
                            next = node.getNextSibling();
                            if (next == null) {
                                node = node.getParentNode();
                                if (node == node) {
                                    next = null;
                                    break;
                                }
                                next = node.getNextSibling();
                            }
                        }
                    }
                    node = next;
                }
                return;
            }
            verify(node, verifyNames, false);
        }
    }

    private void verify(Node node, boolean verifyNames, boolean xml11Version) {
        boolean wellformed;
        int type = node.getNodeType();
        this.fLocator.fRelatedNode = node;
        switch (type) {
            case 1:
                if (verifyNames) {
                    boolean wellformed2 = (this.features & 1) != 0 ? CoreDocumentImpl.isValidQName(node.getPrefix(), node.getLocalName(), xml11Version) : CoreDocumentImpl.isXMLName(node.getNodeName(), xml11Version);
                    if (!wellformed2 && this.fErrorHandler != null) {
                        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "wf-invalid-character-in-node-name", new Object[]{"Element", node.getNodeName()});
                        DOMNormalizer.reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg, (short) 3, "wf-invalid-character-in-node-name");
                    }
                }
                boolean wellformed3 = node.hasAttributes();
                NamedNodeMap attributes = wellformed3 ? node.getAttributes() : null;
                if (attributes != null) {
                    int i = 0;
                    while (i < i) {
                        Attr attr = (Attr) attributes.item(i);
                        this.fLocator.fRelatedNode = attr;
                        int i2 = i;
                        NamedNodeMap attributes2 = attributes;
                        DOMNormalizer.isAttrValueWF(this.fErrorHandler, this.fError, this.fLocator, attributes, attr, attr.getValue(), xml11Version);
                        if (verifyNames) {
                            boolean wellformed4 = CoreDocumentImpl.isXMLName(attr.getNodeName(), xml11Version);
                            if (!wellformed4) {
                                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "wf-invalid-character-in-node-name", new Object[]{"Attr", node.getNodeName()});
                                DOMNormalizer.reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg2, (short) 3, "wf-invalid-character-in-node-name");
                            }
                        }
                        i = i2 + 1;
                        attributes = attributes2;
                        break;
                    }
                }
                break;
            case 3:
                DOMNormalizer.isXMLCharWF(this.fErrorHandler, this.fError, this.fLocator, node.getNodeValue(), xml11Version);
                break;
            case 4:
                DOMNormalizer.isXMLCharWF(this.fErrorHandler, this.fError, this.fLocator, node.getNodeValue(), xml11Version);
                break;
            case 5:
                if (verifyNames && (this.features & 4) != 0) {
                    CoreDocumentImpl.isXMLName(node.getNodeName(), xml11Version);
                }
                break;
            case 7:
                ProcessingInstruction pinode = (ProcessingInstruction) node;
                String target = pinode.getTarget();
                if (verifyNames) {
                    if (xml11Version) {
                        wellformed = XML11Char.isXML11ValidName(target);
                    } else {
                        wellformed = XMLChar.isValidName(target);
                    }
                    if (!wellformed) {
                        String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "wf-invalid-character-in-node-name", new Object[]{"Element", node.getNodeName()});
                        DOMNormalizer.reportDOMError(this.fErrorHandler, this.fError, this.fLocator, msg3, (short) 3, "wf-invalid-character-in-node-name");
                    }
                }
                DOMNormalizer.isXMLCharWF(this.fErrorHandler, this.fError, this.fLocator, pinode.getData(), xml11Version);
                break;
            case 8:
                if ((this.features & 32) != 0) {
                    DOMNormalizer.isCommentWF(this.fErrorHandler, this.fError, this.fLocator, ((Comment) node).getData(), xml11Version);
                }
                break;
        }
        this.fLocator.fRelatedNode = null;
    }

    private String _getXmlVersion(Node node) {
        Document doc = node.getNodeType() == 9 ? (Document) node : node.getOwnerDocument();
        if (doc != null && DocumentMethods.fgDocumentMethodsAvailable) {
            try {
                return (String) DocumentMethods.fgDocumentGetXmlVersionMethod.invoke(doc, null);
            } catch (ThreadDeath td) {
                throw td;
            } catch (VirtualMachineError vme) {
                throw vme;
            } catch (Throwable th) {
            }
        }
        return null;
    }

    static class DocumentMethods {
        private static Method fgDocumentGetInputEncodingMethod;
        private static Method fgDocumentGetXmlEncodingMethod;
        private static Method fgDocumentGetXmlVersionMethod;
        private static boolean fgDocumentMethodsAvailable;

        static {
            fgDocumentGetXmlVersionMethod = null;
            fgDocumentGetInputEncodingMethod = null;
            fgDocumentGetXmlEncodingMethod = null;
            fgDocumentMethodsAvailable = false;
            try {
                fgDocumentGetXmlVersionMethod = Document.class.getMethod("getXmlVersion", new Class[0]);
                fgDocumentGetInputEncodingMethod = Document.class.getMethod("getInputEncoding", new Class[0]);
                fgDocumentGetXmlEncodingMethod = Document.class.getMethod("getXmlEncoding", new Class[0]);
                fgDocumentMethodsAvailable = true;
            } catch (Exception e) {
                fgDocumentGetXmlVersionMethod = null;
                fgDocumentGetInputEncodingMethod = null;
                fgDocumentGetXmlEncodingMethod = null;
                fgDocumentMethodsAvailable = false;
            }
        }
    }
}
