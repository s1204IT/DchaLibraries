package org.apache.xpath;

import java.io.IOException;
import java.util.Vector;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.utils.SystemIDResolver;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class SourceTreeManager {
    private Vector m_sourceTree = new Vector();
    URIResolver m_uriResolver;

    public void reset() {
        this.m_sourceTree = new Vector();
    }

    public void setURIResolver(URIResolver uRIResolver) {
        this.m_uriResolver = uRIResolver;
    }

    public URIResolver getURIResolver() {
        return this.m_uriResolver;
    }

    public String findURIFromDoc(int i) {
        int size = this.m_sourceTree.size();
        for (int i2 = 0; i2 < size; i2++) {
            SourceTree sourceTree = (SourceTree) this.m_sourceTree.elementAt(i2);
            if (i == sourceTree.m_root) {
                return sourceTree.m_url;
            }
        }
        return null;
    }

    public Source resolveURI(String str, String str2, SourceLocator sourceLocator) throws TransformerException, IOException {
        Source sourceResolve;
        if (this.m_uriResolver != null) {
            sourceResolve = this.m_uriResolver.resolve(str2, str);
        } else {
            sourceResolve = null;
        }
        if (sourceResolve == null) {
            return new StreamSource(SystemIDResolver.getAbsoluteURI(str2, str));
        }
        return sourceResolve;
    }

    public void removeDocumentFromCache(int i) {
        if (-1 == i) {
            return;
        }
        for (int size = this.m_sourceTree.size() - 1; size >= 0; size--) {
            SourceTree sourceTree = (SourceTree) this.m_sourceTree.elementAt(size);
            if (sourceTree != null && sourceTree.m_root == i) {
                this.m_sourceTree.removeElementAt(size);
                return;
            }
        }
    }

    public void putDocumentInCache(int i, Source source) {
        int node = getNode(source);
        if (-1 != node) {
            if (node != i) {
                throw new RuntimeException("Programmer's Error!  putDocumentInCache found reparse of doc: " + source.getSystemId());
            }
            return;
        }
        if (source.getSystemId() != null) {
            this.m_sourceTree.addElement(new SourceTree(i, source.getSystemId()));
        }
    }

    public int getNode(Source source) {
        String systemId = source.getSystemId();
        if (systemId == null) {
            return -1;
        }
        int size = this.m_sourceTree.size();
        for (int i = 0; i < size; i++) {
            SourceTree sourceTree = (SourceTree) this.m_sourceTree.elementAt(i);
            if (systemId.equals(sourceTree.m_url)) {
                return sourceTree.m_root;
            }
        }
        return -1;
    }

    public int getSourceTree(String str, String str2, SourceLocator sourceLocator, XPathContext xPathContext) throws TransformerException {
        try {
            return getSourceTree(resolveURI(str, str2, sourceLocator), sourceLocator, xPathContext);
        } catch (IOException e) {
            throw new TransformerException(e.getMessage(), sourceLocator, e);
        }
    }

    public int getSourceTree(Source source, SourceLocator sourceLocator, XPathContext xPathContext) throws TransformerException {
        int node = getNode(source);
        if (-1 != node) {
            return node;
        }
        int toNode = parseToNode(source, sourceLocator, xPathContext);
        if (-1 != toNode) {
            putDocumentInCache(toNode, source);
        }
        return toNode;
    }

    public int parseToNode(Source source, SourceLocator sourceLocator, XPathContext xPathContext) throws TransformerException {
        DTM dtm;
        try {
            Object ownerObject = xPathContext.getOwnerObject();
            if (ownerObject != null && (ownerObject instanceof DTMWSFilter)) {
                dtm = xPathContext.getDTM(source, false, (DTMWSFilter) ownerObject, false, true);
            } else {
                dtm = xPathContext.getDTM(source, false, null, false, true);
            }
            return dtm.getDocument();
        } catch (Exception e) {
            throw new TransformerException(e.getMessage(), sourceLocator, e);
        }
    }

    public static XMLReader getXMLReader(Source source, SourceLocator sourceLocator) throws TransformerException, SAXException {
        try {
            XMLReader xMLReader = source instanceof SAXSource ? ((SAXSource) source).getXMLReader() : null;
            if (xMLReader == null) {
                try {
                    try {
                        SAXParserFactory sAXParserFactoryNewInstance = SAXParserFactory.newInstance();
                        sAXParserFactoryNewInstance.setNamespaceAware(true);
                        xMLReader = sAXParserFactoryNewInstance.newSAXParser().getXMLReader();
                    } catch (NoSuchMethodError e) {
                    } catch (ParserConfigurationException e2) {
                        throw new SAXException(e2);
                    }
                } catch (AbstractMethodError e3) {
                } catch (FactoryConfigurationError e4) {
                    throw new SAXException(e4.toString());
                }
                if (xMLReader == null) {
                    xMLReader = XMLReaderFactory.createXMLReader();
                }
            }
            try {
                xMLReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            } catch (SAXException e5) {
            }
            return xMLReader;
        } catch (SAXException e6) {
            throw new TransformerException(e6.getMessage(), sourceLocator, e6);
        }
    }
}
