package mf.org.apache.xml.resolver.helpers;

import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class BootstrapResolver implements EntityResolver {
    private Hashtable publicMap = new Hashtable();
    private Hashtable systemMap = new Hashtable();
    private Hashtable uriMap = new Hashtable();

    public BootstrapResolver() {
        URL url = getClass().getResource("/org/apache/xml/resolver/etc/catalog.dtd");
        if (url != null) {
            this.publicMap.put("-//OASIS//DTD XML Catalogs V1.0//EN", url.toString());
            this.systemMap.put("http://www.oasis-open.org/committees/entity/release/1.0/catalog.dtd", url.toString());
        }
        URL url2 = getClass().getResource("/org/apache/xml/resolver/etc/catalog.rng");
        if (url2 != null) {
            this.uriMap.put("http://www.oasis-open.org/committees/entity/release/1.0/catalog.rng", url2.toString());
        }
        URL url3 = getClass().getResource("/org/apache/xml/resolver/etc/catalog.xsd");
        if (url3 != null) {
            this.uriMap.put("http://www.oasis-open.org/committees/entity/release/1.0/catalog.xsd", url3.toString());
        }
        URL url4 = getClass().getResource("/org/apache/xml/resolver/etc/xcatalog.dtd");
        if (url4 != null) {
            this.publicMap.put("-//DTD XCatalog//EN", url4.toString());
        }
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) {
        String resolved = null;
        if (systemId != null && this.systemMap.containsKey(systemId)) {
            resolved = (String) this.systemMap.get(systemId);
        } else if (publicId != null && this.publicMap.containsKey(publicId)) {
            resolved = (String) this.publicMap.get(publicId);
        }
        if (resolved == null) {
            return null;
        }
        try {
            InputSource iSource = new InputSource(resolved);
            iSource.setPublicId(publicId);
            URL url = new URL(resolved);
            InputStream iStream = url.openStream();
            iSource.setByteStream(iStream);
            return iSource;
        } catch (Exception e) {
            return null;
        }
    }
}
