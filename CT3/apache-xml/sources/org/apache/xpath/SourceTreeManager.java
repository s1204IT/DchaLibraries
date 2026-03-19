package org.apache.xpath;

import java.io.IOException;
import java.util.Vector;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
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

    public void setURIResolver(URIResolver resolver) {
        this.m_uriResolver = resolver;
    }

    public URIResolver getURIResolver() {
        return this.m_uriResolver;
    }

    public String findURIFromDoc(int owner) {
        int n = this.m_sourceTree.size();
        for (int i = 0; i < n; i++) {
            SourceTree sTree = (SourceTree) this.m_sourceTree.elementAt(i);
            if (owner == sTree.m_root) {
                return sTree.m_url;
            }
        }
        return null;
    }

    public Source resolveURI(String base, String urlString, SourceLocator locator) throws TransformerException, IOException {
        Source source = null;
        if (this.m_uriResolver != null) {
            source = this.m_uriResolver.resolve(urlString, base);
        }
        if (source == null) {
            String uri = SystemIDResolver.getAbsoluteURI(urlString, base);
            return new StreamSource(uri);
        }
        return source;
    }

    public void removeDocumentFromCache(int n) {
        if (-1 == n) {
            return;
        }
        for (int i = this.m_sourceTree.size() - 1; i >= 0; i--) {
            SourceTree st = (SourceTree) this.m_sourceTree.elementAt(i);
            if (st != null && st.m_root == n) {
                this.m_sourceTree.removeElementAt(i);
                return;
            }
        }
    }

    public void putDocumentInCache(int n, Source source) {
        int cachedNode = getNode(source);
        if (-1 != cachedNode) {
            if (cachedNode != n) {
                throw new RuntimeException("Programmer's Error!  putDocumentInCache found reparse of doc: " + source.getSystemId());
            }
        } else {
            if (source.getSystemId() == null) {
                return;
            }
            this.m_sourceTree.addElement(new SourceTree(n, source.getSystemId()));
        }
    }

    public int getNode(Source source) {
        String url = source.getSystemId();
        if (url == null) {
            return -1;
        }
        int n = this.m_sourceTree.size();
        for (int i = 0; i < n; i++) {
            SourceTree sTree = (SourceTree) this.m_sourceTree.elementAt(i);
            if (url.equals(sTree.m_url)) {
                return sTree.m_root;
            }
        }
        return -1;
    }

    public int getSourceTree(String base, String urlString, SourceLocator locator, XPathContext xctxt) throws TransformerException {
        try {
            Source source = resolveURI(base, urlString, locator);
            return getSourceTree(source, locator, xctxt);
        } catch (IOException ioe) {
            throw new TransformerException(ioe.getMessage(), locator, ioe);
        }
    }

    public int getSourceTree(Source source, SourceLocator locator, XPathContext xctxt) throws TransformerException {
        int n = getNode(source);
        if (-1 != n) {
            return n;
        }
        int n2 = parseToNode(source, locator, xctxt);
        if (-1 != n2) {
            putDocumentInCache(n2, source);
        }
        return n2;
    }

    public int parseToNode(Source source, SourceLocator locator, XPathContext xctxt) throws TransformerException {
        DTM dtm;
        try {
            Object xowner = xctxt.getOwnerObject();
            if (xowner != null && (xowner instanceof DTMWSFilter)) {
                dtm = xctxt.getDTM(source, false, (DTMWSFilter) xowner, false, true);
            } else {
                dtm = xctxt.getDTM(source, false, null, false, true);
            }
            return dtm.getDocument();
        } catch (Exception e) {
            throw new TransformerException(e.getMessage(), locator, e);
        }
    }

    public static XMLReader getXMLReader(Source source, SourceLocator locator) throws TransformerException {
        try {
            XMLReader xMLReader = source instanceof SAXSource ? source.getXMLReader() : null;
            if (xMLReader == null) {
                try {
                    try {
                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        factory.setNamespaceAware(true);
                        SAXParser jaxpParser = factory.newSAXParser();
                        xMLReader = jaxpParser.getXMLReader();
                    } catch (ParserConfigurationException ex) {
                        throw new SAXException(ex);
                    }
                } catch (AbstractMethodError e) {
                } catch (NoSuchMethodError e2) {
                } catch (FactoryConfigurationError ex1) {
                    throw new SAXException(ex1.toString());
                }
                if (xMLReader == null) {
                    xMLReader = XMLReaderFactory.createXMLReader();
                }
            }
            try {
                xMLReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            } catch (SAXException e3) {
            }
            return xMLReader;
        } catch (SAXException se) {
            throw new TransformerException(se.getMessage(), locator, se);
        }
    }
}
