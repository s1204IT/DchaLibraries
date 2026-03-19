package org.apache.xml.utils;

import java.util.Hashtable;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XMLReaderManager {
    private static final String NAMESPACES_FEATURE = "http://xml.org/sax/features/namespaces";
    private static final String NAMESPACE_PREFIXES_FEATURE = "http://xml.org/sax/features/namespace-prefixes";
    private static SAXParserFactory m_parserFactory;
    private static final XMLReaderManager m_singletonManager = new XMLReaderManager();
    private Hashtable m_inUse;
    private ThreadLocal m_readers;

    private XMLReaderManager() {
    }

    public static XMLReaderManager getInstance() {
        return m_singletonManager;
    }

    public synchronized XMLReader getXMLReader() throws SAXException {
        XMLReader xMLReader;
        XMLReader xMLReader2;
        if (this.m_readers == null) {
            this.m_readers = new ThreadLocal();
        }
        if (this.m_inUse == null) {
            this.m_inUse = new Hashtable();
        }
        xMLReader = (XMLReader) this.m_readers.get();
        boolean z = xMLReader != null;
        if (!z || this.m_inUse.get(xMLReader) == Boolean.TRUE) {
            try {
                try {
                    try {
                        xMLReader2 = XMLReaderFactory.createXMLReader();
                    } catch (Exception e) {
                        try {
                            if (m_parserFactory == null) {
                                m_parserFactory = SAXParserFactory.newInstance();
                                m_parserFactory.setNamespaceAware(true);
                            }
                            xMLReader2 = m_parserFactory.newSAXParser().getXMLReader();
                        } catch (ParserConfigurationException e2) {
                            throw e2;
                        }
                    }
                    xMLReader = xMLReader2;
                    try {
                        xMLReader.setFeature(NAMESPACES_FEATURE, true);
                        xMLReader.setFeature(NAMESPACE_PREFIXES_FEATURE, false);
                    } catch (SAXException e3) {
                    }
                } catch (ParserConfigurationException e4) {
                    throw new SAXException(e4);
                }
            } catch (AbstractMethodError e5) {
            } catch (NoSuchMethodError e6) {
            } catch (FactoryConfigurationError e7) {
                throw new SAXException(e7.toString());
            }
            if (!z) {
                this.m_readers.set(xMLReader);
                this.m_inUse.put(xMLReader, Boolean.TRUE);
            }
        } else {
            this.m_inUse.put(xMLReader, Boolean.TRUE);
        }
        return xMLReader;
    }

    public synchronized void releaseXMLReader(XMLReader xMLReader) {
        if (this.m_readers.get() == xMLReader && xMLReader != null) {
            this.m_inUse.remove(xMLReader);
        }
    }
}
