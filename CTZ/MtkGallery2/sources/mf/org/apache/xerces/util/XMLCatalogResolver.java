package mf.org.apache.xerces.util;

import java.io.IOException;
import mf.javax.xml.parsers.SAXParserFactory;
import mf.org.apache.xerces.dom.DOMInputImpl;
import mf.org.apache.xerces.jaxp.SAXParserFactoryImpl;
import mf.org.apache.xerces.util.URI;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLEntityResolver;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import mf.org.apache.xml.resolver.Catalog;
import mf.org.apache.xml.resolver.CatalogManager;
import mf.org.apache.xml.resolver.readers.SAXCatalogReader;
import mf.org.w3c.dom.ls.LSInput;
import mf.org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

public class XMLCatalogResolver implements XMLEntityResolver, LSResourceResolver, EntityResolver2 {
    private Catalog fCatalog;
    private boolean fCatalogsChanged;
    private String[] fCatalogsList;
    private boolean fPreferPublic;
    private CatalogManager fResolverCatalogManager;
    private boolean fUseLiteralSystemId;

    public XMLCatalogResolver() {
        this(null, true);
    }

    public XMLCatalogResolver(String[] catalogs) {
        this(catalogs, true);
    }

    public XMLCatalogResolver(String[] catalogs, boolean preferPublic) {
        this.fResolverCatalogManager = null;
        this.fCatalog = null;
        this.fCatalogsList = null;
        this.fCatalogsChanged = true;
        this.fPreferPublic = true;
        this.fUseLiteralSystemId = true;
        init(catalogs, preferPublic);
    }

    public final synchronized String[] getCatalogList() {
        return this.fCatalogsList != null ? (String[]) this.fCatalogsList.clone() : null;
    }

    public final synchronized void setCatalogList(String[] catalogs) {
        this.fCatalogsChanged = true;
        this.fCatalogsList = catalogs != null ? (String[]) catalogs.clone() : null;
    }

    public final synchronized void clear() {
        this.fCatalog = null;
    }

    public final boolean getPreferPublic() {
        return this.fPreferPublic;
    }

    public final void setPreferPublic(boolean preferPublic) {
        this.fPreferPublic = preferPublic;
        this.fResolverCatalogManager.setPreferPublic(preferPublic);
    }

    public final boolean getUseLiteralSystemId() {
        return this.fUseLiteralSystemId;
    }

    public final void setUseLiteralSystemId(boolean useLiteralSystemId) {
        this.fUseLiteralSystemId = useLiteralSystemId;
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        String resolvedId = null;
        if (publicId != null && systemId != null) {
            resolvedId = resolvePublic(publicId, systemId);
        } else if (systemId != null) {
            resolvedId = resolveSystem(systemId);
        }
        if (resolvedId != null) {
            InputSource source = new InputSource(resolvedId);
            source.setPublicId(publicId);
            return source;
        }
        return null;
    }

    @Override
    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
        String resolvedId = null;
        if (!getUseLiteralSystemId() && baseURI != null) {
            try {
                URI uri = new URI(new URI(baseURI), systemId);
                systemId = uri.toString();
            } catch (URI.MalformedURIException e) {
            }
        }
        if (publicId != null && systemId != null) {
            resolvedId = resolvePublic(publicId, systemId);
        } else if (systemId != null) {
            resolvedId = resolveSystem(systemId);
        }
        if (resolvedId != null) {
            InputSource source = new InputSource(resolvedId);
            source.setPublicId(publicId);
            return source;
        }
        return null;
    }

    @Override
    public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
        return null;
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) throws IOException {
        String resolvedId = null;
        if (namespaceURI != null) {
            try {
                resolvedId = resolveURI(namespaceURI);
                if (!getUseLiteralSystemId() && baseURI != null) {
                    try {
                        URI uri = new URI(new URI(baseURI), systemId);
                        systemId = uri.toString();
                    } catch (URI.MalformedURIException e) {
                    }
                }
                if (resolvedId == null) {
                    if (publicId != null && systemId != null) {
                        resolvedId = resolvePublic(publicId, systemId);
                    } else if (systemId != null) {
                        resolvedId = resolveSystem(systemId);
                    }
                }
            } catch (IOException e2) {
            }
        } else {
            if (!getUseLiteralSystemId()) {
                URI uri2 = new URI(new URI(baseURI), systemId);
                systemId = uri2.toString();
            }
            if (resolvedId == null) {
            }
        }
        if (resolvedId == null) {
            return null;
        }
        return new DOMInputImpl(publicId, resolvedId, baseURI);
    }

    @Override
    public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws IOException, XNIException {
        String resolvedId = resolveIdentifier(resourceIdentifier);
        if (resolvedId != null) {
            return new XMLInputSource(resourceIdentifier.getPublicId(), resolvedId, resourceIdentifier.getBaseSystemId());
        }
        return null;
    }

    public String resolveIdentifier(XMLResourceIdentifier resourceIdentifier) throws IOException, XNIException {
        String systemId;
        String resolvedId = null;
        String namespace = resourceIdentifier.getNamespace();
        if (namespace != null) {
            resolvedId = resolveURI(namespace);
        }
        if (resolvedId == null) {
            String publicId = resourceIdentifier.getPublicId();
            if (getUseLiteralSystemId()) {
                systemId = resourceIdentifier.getLiteralSystemId();
            } else {
                systemId = resourceIdentifier.getExpandedSystemId();
            }
            if (publicId != null && systemId != null) {
                String resolvedId2 = resolvePublic(publicId, systemId);
                return resolvedId2;
            }
            if (systemId != null) {
                String resolvedId3 = resolveSystem(systemId);
                return resolvedId3;
            }
            return resolvedId;
        }
        return resolvedId;
    }

    public final synchronized String resolveSystem(String systemId) throws IOException {
        if (this.fCatalogsChanged) {
            parseCatalogs();
            this.fCatalogsChanged = false;
        }
        return this.fCatalog != null ? this.fCatalog.resolveSystem(systemId) : null;
    }

    public final synchronized String resolvePublic(String publicId, String systemId) throws IOException {
        if (this.fCatalogsChanged) {
            parseCatalogs();
            this.fCatalogsChanged = false;
        }
        return this.fCatalog != null ? this.fCatalog.resolvePublic(publicId, systemId) : null;
    }

    public final synchronized String resolveURI(String uri) throws IOException {
        if (this.fCatalogsChanged) {
            parseCatalogs();
            this.fCatalogsChanged = false;
        }
        return this.fCatalog != null ? this.fCatalog.resolveURI(uri) : null;
    }

    private void init(String[] catalogs, boolean preferPublic) {
        this.fCatalogsList = catalogs != null ? (String[]) catalogs.clone() : null;
        this.fPreferPublic = preferPublic;
        this.fResolverCatalogManager = new CatalogManager();
        this.fResolverCatalogManager.setAllowOasisXMLCatalogPI(false);
        this.fResolverCatalogManager.setCatalogClassName("org.apache.xml.resolver.Catalog");
        this.fResolverCatalogManager.setCatalogFiles("");
        this.fResolverCatalogManager.setIgnoreMissingProperties(true);
        this.fResolverCatalogManager.setPreferPublic(this.fPreferPublic);
        this.fResolverCatalogManager.setRelativeCatalogs(false);
        this.fResolverCatalogManager.setUseStaticCatalog(false);
        this.fResolverCatalogManager.setVerbosity(0);
    }

    private void parseCatalogs() throws IOException {
        if (this.fCatalogsList != null) {
            this.fCatalog = new Catalog(this.fResolverCatalogManager);
            attachReaderToCatalog(this.fCatalog);
            for (int i = 0; i < this.fCatalogsList.length; i++) {
                String catalog = this.fCatalogsList[i];
                if (catalog != null && catalog.length() > 0) {
                    this.fCatalog.parseCatalog(catalog);
                }
            }
            return;
        }
        this.fCatalog = null;
    }

    private void attachReaderToCatalog(Catalog catalog) {
        SAXParserFactory spf = new SAXParserFactoryImpl();
        spf.setNamespaceAware(true);
        spf.setValidating(false);
        SAXCatalogReader saxReader = new SAXCatalogReader(spf);
        saxReader.setCatalogParser("urn:oasis:names:tc:entity:xmlns:xml:catalog", "catalog", "org.apache.xml.resolver.readers.OASISXMLCatalogReader");
        catalog.addReader("application/xml", saxReader);
    }
}
