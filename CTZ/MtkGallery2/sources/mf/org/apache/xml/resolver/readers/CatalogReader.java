package mf.org.apache.xml.resolver.readers;

import java.io.IOException;
import java.io.InputStream;
import mf.org.apache.xml.resolver.Catalog;
import mf.org.apache.xml.resolver.CatalogException;

public interface CatalogReader {
    void readCatalog(Catalog catalog, InputStream inputStream) throws CatalogException, IOException;
}
