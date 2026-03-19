package mf.org.apache.xml.resolver.readers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Hashtable;
import mf.javax.xml.parsers.ParserConfigurationException;
import mf.javax.xml.parsers.SAXParser;
import mf.javax.xml.parsers.SAXParserFactory;
import mf.org.apache.xml.resolver.Catalog;
import mf.org.apache.xml.resolver.CatalogException;
import mf.org.apache.xml.resolver.CatalogManager;
import mf.org.apache.xml.resolver.helpers.Debug;
import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;

public class SAXCatalogReader implements CatalogReader, ContentHandler, DocumentHandler {
    private boolean abandonHope;
    private Catalog catalog;
    protected Debug debug;
    private ClassLoader loader;
    protected Hashtable namespaceMap;
    protected String parserClass;
    protected SAXParserFactory parserFactory;
    private SAXCatalogParser saxParser;

    public SAXCatalogReader() {
        this.parserFactory = null;
        this.parserClass = null;
        this.namespaceMap = new Hashtable();
        this.saxParser = null;
        this.abandonHope = false;
        this.loader = null;
        this.debug = CatalogManager.getStaticManager().debug;
        this.parserFactory = null;
        this.parserClass = null;
    }

    public SAXCatalogReader(SAXParserFactory parserFactory) {
        this.parserFactory = null;
        this.parserClass = null;
        this.namespaceMap = new Hashtable();
        this.saxParser = null;
        this.abandonHope = false;
        this.loader = null;
        this.debug = CatalogManager.getStaticManager().debug;
        this.parserFactory = parserFactory;
    }

    public void setCatalogParser(String namespaceURI, String rootElement, String parserClass) {
        String namespaceURI2 = namespaceURI != null ? namespaceURI.trim() : "";
        this.namespaceMap.put("{" + namespaceURI2 + "}" + rootElement, parserClass);
    }

    public String getCatalogParser(String namespaceURI, String rootElement) {
        String namespaceURI2 = namespaceURI != null ? namespaceURI.trim() : "";
        return (String) this.namespaceMap.get("{" + namespaceURI2 + "}" + rootElement);
    }

    @Override
    public void readCatalog(Catalog catalog, InputStream is) throws CatalogException, IOException {
        if (this.parserFactory == null && this.parserClass == null) {
            this.debug.message(1, "Cannot read SAX catalog without a parser");
            throw new CatalogException(6);
        }
        this.debug = catalog.getCatalogManager().debug;
        EntityResolver bResolver = catalog.getCatalogManager().getBootstrapResolver();
        this.catalog = catalog;
        try {
            if (this.parserFactory != null) {
                SAXParser parser = this.parserFactory.newSAXParser();
                SAXParserHandler spHandler = new SAXParserHandler();
                spHandler.setContentHandler(this);
                if (bResolver != null) {
                    spHandler.setEntityResolver(bResolver);
                }
                parser.parse(new InputSource(is), spHandler);
                return;
            }
            Parser parser2 = (Parser) Class.forName(this.parserClass, true, this.loader != null ? this.loader : getClass().getClassLoader()).newInstance();
            parser2.setDocumentHandler(this);
            if (bResolver != null) {
                parser2.setEntityResolver(bResolver);
            }
            parser2.parse(new InputSource(is));
        } catch (ClassNotFoundException e) {
            throw new CatalogException(6);
        } catch (IllegalAccessException e2) {
            throw new CatalogException(6);
        } catch (InstantiationException e3) {
            throw new CatalogException(6);
        } catch (ParserConfigurationException e4) {
            throw new CatalogException(5);
        } catch (SAXException se) {
            Exception e5 = se.getException();
            UnknownHostException uhe = new UnknownHostException();
            FileNotFoundException fnfe = new FileNotFoundException();
            if (e5 != null) {
                if (e5.getClass() == uhe.getClass()) {
                    throw new CatalogException(7, e5.toString());
                }
                if (e5.getClass() == fnfe.getClass()) {
                    throw new CatalogException(7, e5.toString());
                }
            }
            throw new CatalogException(se);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        if (this.saxParser != null) {
            this.saxParser.setDocumentLocator(locator);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        this.saxParser = null;
        this.abandonHope = false;
    }

    @Override
    public void endDocument() throws SAXException {
        if (this.saxParser != null) {
            this.saxParser.endDocument();
        }
    }

    @Override
    public void startElement(String name, AttributeList atts) throws SAXException {
        String namespaceURI;
        if (this.abandonHope) {
            return;
        }
        if (this.saxParser == null) {
            String prefix = "";
            if (name.indexOf(58) > 0) {
                prefix = name.substring(0, name.indexOf(58));
            }
            String localName = name;
            if (localName.indexOf(58) > 0) {
                localName = localName.substring(localName.indexOf(58) + 1);
            }
            if (prefix.equals("")) {
                namespaceURI = atts.getValue("xmlns");
            } else {
                namespaceURI = atts.getValue("xmlns:" + prefix);
            }
            String saxParserClass = getCatalogParser(namespaceURI, localName);
            if (saxParserClass == null) {
                this.abandonHope = true;
                if (namespaceURI == null) {
                    this.debug.message(2, "No Catalog parser for " + name);
                    return;
                }
                this.debug.message(2, "No Catalog parser for {" + namespaceURI + "}" + name);
                return;
            }
            try {
                this.saxParser = (SAXCatalogParser) Class.forName(saxParserClass, true, this.loader != null ? this.loader : getClass().getClassLoader()).newInstance();
                this.saxParser.setCatalog(this.catalog);
                this.saxParser.startDocument();
                this.saxParser.startElement(name, atts);
                return;
            } catch (ClassCastException cce) {
                this.saxParser = null;
                this.abandonHope = true;
                this.debug.message(2, cce.toString());
                return;
            } catch (ClassNotFoundException cnfe) {
                this.saxParser = null;
                this.abandonHope = true;
                this.debug.message(2, cnfe.toString());
                return;
            } catch (IllegalAccessException iae) {
                this.saxParser = null;
                this.abandonHope = true;
                this.debug.message(2, iae.toString());
                return;
            } catch (InstantiationException ie) {
                this.saxParser = null;
                this.abandonHope = true;
                this.debug.message(2, ie.toString());
                return;
            }
        }
        this.saxParser.startElement(name, atts);
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (this.abandonHope) {
            return;
        }
        if (this.saxParser == null) {
            String saxParserClass = getCatalogParser(namespaceURI, localName);
            if (saxParserClass == null) {
                this.abandonHope = true;
                if (namespaceURI == null) {
                    this.debug.message(2, "No Catalog parser for " + localName);
                    return;
                }
                this.debug.message(2, "No Catalog parser for {" + namespaceURI + "}" + localName);
                return;
            }
            try {
                this.saxParser = (SAXCatalogParser) Class.forName(saxParserClass, true, this.loader != null ? this.loader : getClass().getClassLoader()).newInstance();
                this.saxParser.setCatalog(this.catalog);
                this.saxParser.startDocument();
                this.saxParser.startElement(namespaceURI, localName, qName, atts);
                return;
            } catch (ClassCastException cce) {
                this.saxParser = null;
                this.abandonHope = true;
                this.debug.message(2, cce.toString());
                return;
            } catch (ClassNotFoundException cnfe) {
                this.saxParser = null;
                this.abandonHope = true;
                this.debug.message(2, cnfe.toString());
                return;
            } catch (IllegalAccessException iae) {
                this.saxParser = null;
                this.abandonHope = true;
                this.debug.message(2, iae.toString());
                return;
            } catch (InstantiationException ie) {
                this.saxParser = null;
                this.abandonHope = true;
                this.debug.message(2, ie.toString());
                return;
            }
        }
        this.saxParser.startElement(namespaceURI, localName, qName, atts);
    }

    @Override
    public void endElement(String name) throws SAXException {
        if (this.saxParser != null) {
            this.saxParser.endElement(name);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (this.saxParser != null) {
            this.saxParser.endElement(namespaceURI, localName, qName);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.saxParser != null) {
            this.saxParser.characters(ch, start, length);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (this.saxParser != null) {
            this.saxParser.ignorableWhitespace(ch, start, length);
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (this.saxParser != null) {
            this.saxParser.processingInstruction(target, data);
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (this.saxParser != null) {
            this.saxParser.startPrefixMapping(prefix, uri);
        }
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (this.saxParser != null) {
            this.saxParser.endPrefixMapping(prefix);
        }
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        if (this.saxParser != null) {
            this.saxParser.skippedEntity(name);
        }
    }
}
