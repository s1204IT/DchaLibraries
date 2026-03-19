package mf.org.apache.xml.resolver;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xml.resolver.helpers.FileURL;
import mf.org.apache.xml.resolver.helpers.PublicId;
import mf.org.apache.xml.resolver.readers.CatalogReader;

public class Catalog {
    protected URL base;
    protected URL catalogCwd;
    protected Vector catalogEntries;
    protected Vector catalogFiles;
    protected CatalogManager catalogManager;
    protected Vector catalogs;
    protected boolean default_override;
    protected Vector localCatalogFiles;
    protected Vector localDelegate;
    protected Vector readerArr;
    protected Hashtable readerMap;
    public static final int BASE = CatalogEntry.addEntryType("BASE", 1);
    public static final int CATALOG = CatalogEntry.addEntryType("CATALOG", 1);
    public static final int DOCUMENT = CatalogEntry.addEntryType("DOCUMENT", 1);
    public static final int OVERRIDE = CatalogEntry.addEntryType("OVERRIDE", 1);
    public static final int SGMLDECL = CatalogEntry.addEntryType("SGMLDECL", 1);
    public static final int DELEGATE_PUBLIC = CatalogEntry.addEntryType("DELEGATE_PUBLIC", 2);
    public static final int DELEGATE_SYSTEM = CatalogEntry.addEntryType("DELEGATE_SYSTEM", 2);
    public static final int DELEGATE_URI = CatalogEntry.addEntryType("DELEGATE_URI", 2);
    public static final int DOCTYPE = CatalogEntry.addEntryType("DOCTYPE", 2);
    public static final int DTDDECL = CatalogEntry.addEntryType("DTDDECL", 2);
    public static final int ENTITY = CatalogEntry.addEntryType(SchemaSymbols.ATTVAL_ENTITY, 2);
    public static final int LINKTYPE = CatalogEntry.addEntryType("LINKTYPE", 2);
    public static final int NOTATION = CatalogEntry.addEntryType(SchemaSymbols.ATTVAL_NOTATION, 2);
    public static final int PUBLIC = CatalogEntry.addEntryType("PUBLIC", 2);
    public static final int SYSTEM = CatalogEntry.addEntryType("SYSTEM", 2);
    public static final int URI = CatalogEntry.addEntryType("URI", 2);
    public static final int REWRITE_SYSTEM = CatalogEntry.addEntryType("REWRITE_SYSTEM", 2);
    public static final int REWRITE_URI = CatalogEntry.addEntryType("REWRITE_URI", 2);
    public static final int SYSTEM_SUFFIX = CatalogEntry.addEntryType("SYSTEM_SUFFIX", 2);
    public static final int URI_SUFFIX = CatalogEntry.addEntryType("URI_SUFFIX", 2);

    public Catalog() {
        this.catalogEntries = new Vector();
        this.default_override = true;
        this.catalogManager = CatalogManager.getStaticManager();
        this.catalogFiles = new Vector();
        this.localCatalogFiles = new Vector();
        this.catalogs = new Vector();
        this.localDelegate = new Vector();
        this.readerMap = new Hashtable();
        this.readerArr = new Vector();
    }

    public Catalog(CatalogManager manager) {
        this.catalogEntries = new Vector();
        this.default_override = true;
        this.catalogManager = CatalogManager.getStaticManager();
        this.catalogFiles = new Vector();
        this.localCatalogFiles = new Vector();
        this.catalogs = new Vector();
        this.localDelegate = new Vector();
        this.readerMap = new Hashtable();
        this.readerArr = new Vector();
        this.catalogManager = manager;
    }

    public CatalogManager getCatalogManager() {
        return this.catalogManager;
    }

    public void setCatalogManager(CatalogManager manager) {
        this.catalogManager = manager;
    }

    public void addReader(String mimeType, CatalogReader reader) {
        if (this.readerMap.containsKey(mimeType)) {
            Integer pos = (Integer) this.readerMap.get(mimeType);
            this.readerArr.set(pos.intValue(), reader);
        } else {
            this.readerArr.add(reader);
            Integer pos2 = new Integer(this.readerArr.size() - 1);
            this.readerMap.put(mimeType, pos2);
        }
    }

    protected void copyReaders(Catalog newCatalog) {
        Vector mapArr = new Vector(this.readerMap.size());
        for (int count = 0; count < this.readerMap.size(); count++) {
            mapArr.add(null);
        }
        Enumeration en = this.readerMap.keys();
        while (en.hasMoreElements()) {
            String mimeType = (String) en.nextElement();
            Integer pos = (Integer) this.readerMap.get(mimeType);
            mapArr.set(pos.intValue(), mimeType);
        }
        for (int count2 = 0; count2 < mapArr.size(); count2++) {
            String mimeType2 = (String) mapArr.get(count2);
            Integer pos2 = (Integer) this.readerMap.get(mimeType2);
            newCatalog.addReader(mimeType2, (CatalogReader) this.readerArr.get(pos2.intValue()));
        }
    }

    protected Catalog newCatalog() {
        String catalogClass = getClass().getName();
        try {
            Catalog c = (Catalog) Class.forName(catalogClass).newInstance();
            c.setCatalogManager(this.catalogManager);
            copyReaders(c);
            return c;
        } catch (ClassCastException e) {
            this.catalogManager.debug.message(1, "Class Cast Exception: " + catalogClass);
            Catalog c2 = new Catalog();
            c2.setCatalogManager(this.catalogManager);
            copyReaders(c2);
            return c2;
        } catch (ClassNotFoundException e2) {
            this.catalogManager.debug.message(1, "Class Not Found Exception: " + catalogClass);
            Catalog c22 = new Catalog();
            c22.setCatalogManager(this.catalogManager);
            copyReaders(c22);
            return c22;
        } catch (IllegalAccessException e3) {
            this.catalogManager.debug.message(1, "Illegal Access Exception: " + catalogClass);
            Catalog c222 = new Catalog();
            c222.setCatalogManager(this.catalogManager);
            copyReaders(c222);
            return c222;
        } catch (InstantiationException e4) {
            this.catalogManager.debug.message(1, "Instantiation Exception: " + catalogClass);
            Catalog c2222 = new Catalog();
            c2222.setCatalogManager(this.catalogManager);
            copyReaders(c2222);
            return c2222;
        } catch (Exception e5) {
            this.catalogManager.debug.message(1, "Other Exception: " + catalogClass);
            Catalog c22222 = new Catalog();
            c22222.setCatalogManager(this.catalogManager);
            copyReaders(c22222);
            return c22222;
        }
    }

    public synchronized void parseCatalog(String fileName) throws IOException {
        this.default_override = this.catalogManager.getPreferPublic();
        this.catalogManager.debug.message(4, "Parse catalog: " + fileName);
        this.catalogFiles.addElement(fileName);
        parsePendingCatalogs();
    }

    protected synchronized void parsePendingCatalogs() throws IOException {
        if (!this.localCatalogFiles.isEmpty()) {
            Vector newQueue = new Vector();
            Enumeration q = this.localCatalogFiles.elements();
            while (q.hasMoreElements()) {
                newQueue.addElement(q.nextElement());
            }
            for (int curCat = 0; curCat < this.catalogFiles.size(); curCat++) {
                newQueue.addElement((String) this.catalogFiles.elementAt(curCat));
            }
            this.catalogFiles = newQueue;
            this.localCatalogFiles.clear();
        }
        Vector newQueue2 = this.catalogFiles;
        if (newQueue2.isEmpty() && !this.localDelegate.isEmpty()) {
            Enumeration e = this.localDelegate.elements();
            while (e.hasMoreElements()) {
                this.catalogEntries.addElement(e.nextElement());
            }
            this.localDelegate.clear();
        }
        while (!this.catalogFiles.isEmpty()) {
            String catfile = (String) this.catalogFiles.elementAt(0);
            try {
                this.catalogFiles.remove(0);
            } catch (ArrayIndexOutOfBoundsException e2) {
            }
            if (this.catalogEntries.size() == 0 && this.catalogs.size() == 0) {
                try {
                    parseCatalogFile(catfile);
                } catch (CatalogException ce) {
                    System.out.println("FIXME: " + ce.toString());
                }
            } else {
                this.catalogs.addElement(catfile);
            }
            if (!this.localCatalogFiles.isEmpty()) {
                Vector newQueue3 = new Vector();
                Enumeration q2 = this.localCatalogFiles.elements();
                while (q2.hasMoreElements()) {
                    newQueue3.addElement(q2.nextElement());
                }
                for (int curCat2 = 0; curCat2 < this.catalogFiles.size(); curCat2++) {
                    newQueue3.addElement((String) this.catalogFiles.elementAt(curCat2));
                }
                this.catalogFiles = newQueue3;
                this.localCatalogFiles.clear();
            }
            Vector newQueue4 = this.localDelegate;
            if (!newQueue4.isEmpty()) {
                Enumeration e3 = this.localDelegate.elements();
                while (e3.hasMoreElements()) {
                    this.catalogEntries.addElement(e3.nextElement());
                }
                this.localDelegate.clear();
            }
        }
        this.catalogFiles.clear();
    }

    protected synchronized void parseCatalogFile(String fileName) throws CatalogException, IOException {
        DataInputStream inStream;
        try {
            this.catalogCwd = FileURL.makeURL("basename");
        } catch (MalformedURLException e) {
            String userdir = System.getProperty("user.dir");
            this.catalogManager.debug.message(1, "Malformed URL on cwd", userdir.replace('\\', '/'));
            this.catalogCwd = null;
        }
        try {
            this.base = new URL(this.catalogCwd, fixSlashes(fileName));
        } catch (MalformedURLException e2) {
            try {
                this.base = new URL("file:" + fixSlashes(fileName));
            } catch (MalformedURLException e3) {
                this.catalogManager.debug.message(1, "Malformed URL on catalog filename", fixSlashes(fileName));
                this.base = null;
            }
        }
        this.catalogManager.debug.message(2, "Loading catalog", fileName);
        this.catalogManager.debug.message(4, "Default BASE", this.base.toString());
        String fileName2 = this.base.toString();
        boolean parsed = false;
        boolean notFound = false;
        for (int count = 0; !parsed && count < this.readerArr.size(); count++) {
            CatalogReader reader = (CatalogReader) this.readerArr.get(count);
            notFound = false;
            try {
                inStream = new DataInputStream(this.base.openStream());
            } catch (FileNotFoundException e4) {
                notFound = true;
            }
            try {
                reader.readCatalog(this, inStream);
                parsed = true;
            } catch (CatalogException ce) {
                if (ce.getExceptionType() == 7) {
                    if (!parsed) {
                    }
                }
            }
            try {
                inStream.close();
            } catch (IOException e5) {
            }
        }
        if (!parsed) {
            if (notFound) {
                this.catalogManager.debug.message(3, "Catalog does not exist", fileName2);
            } else {
                this.catalogManager.debug.message(1, "Failed to parse catalog", fileName2);
            }
        }
    }

    public String resolveDoctype(String entityName, String publicId, String systemId) throws IOException {
        String resolved;
        String resolved2;
        this.catalogManager.debug.message(3, "resolveDoctype(" + entityName + "," + publicId + "," + systemId + ")");
        String systemId2 = normalizeURI(systemId);
        if (publicId != null && publicId.startsWith("urn:publicid:")) {
            publicId = PublicId.decodeURN(publicId);
        }
        if (systemId2 != null && systemId2.startsWith("urn:publicid:")) {
            String systemId3 = PublicId.decodeURN(systemId2);
            if (publicId != null && !publicId.equals(systemId3)) {
                this.catalogManager.debug.message(1, "urn:publicid: system identifier differs from public identifier; using public identifier");
                systemId2 = null;
            } else {
                publicId = systemId3;
                systemId2 = null;
            }
        }
        if (systemId2 != null && (resolved2 = resolveLocalSystem(systemId2)) != null) {
            return resolved2;
        }
        if (publicId != null && (resolved = resolveLocalPublic(DOCTYPE, entityName, publicId, systemId2)) != null) {
            return resolved;
        }
        boolean over = this.default_override;
        Enumeration en = this.catalogEntries.elements();
        while (en.hasMoreElements()) {
            CatalogEntry e = (CatalogEntry) en.nextElement();
            if (e.getEntryType() == OVERRIDE) {
                over = e.getEntryArg(0).equalsIgnoreCase("YES");
            } else if (e.getEntryType() == DOCTYPE && e.getEntryArg(0).equals(entityName) && (over || systemId2 == null)) {
                return e.getEntryArg(1);
            }
        }
        return resolveSubordinateCatalogs(DOCTYPE, entityName, publicId, systemId2);
    }

    public String resolveDocument() throws IOException {
        this.catalogManager.debug.message(3, "resolveDocument");
        Enumeration en = this.catalogEntries.elements();
        while (en.hasMoreElements()) {
            CatalogEntry e = (CatalogEntry) en.nextElement();
            if (e.getEntryType() == DOCUMENT) {
                return e.getEntryArg(0);
            }
        }
        return resolveSubordinateCatalogs(DOCUMENT, null, null, null);
    }

    public String resolveEntity(String entityName, String publicId, String systemId) throws IOException {
        String resolved;
        String resolved2;
        this.catalogManager.debug.message(3, "resolveEntity(" + entityName + "," + publicId + "," + systemId + ")");
        String systemId2 = normalizeURI(systemId);
        if (publicId != null && publicId.startsWith("urn:publicid:")) {
            publicId = PublicId.decodeURN(publicId);
        }
        if (systemId2 != null && systemId2.startsWith("urn:publicid:")) {
            String systemId3 = PublicId.decodeURN(systemId2);
            if (publicId != null && !publicId.equals(systemId3)) {
                this.catalogManager.debug.message(1, "urn:publicid: system identifier differs from public identifier; using public identifier");
                systemId2 = null;
            } else {
                publicId = systemId3;
                systemId2 = null;
            }
        }
        if (systemId2 != null && (resolved2 = resolveLocalSystem(systemId2)) != null) {
            return resolved2;
        }
        if (publicId != null && (resolved = resolveLocalPublic(ENTITY, entityName, publicId, systemId2)) != null) {
            return resolved;
        }
        boolean over = this.default_override;
        Enumeration en = this.catalogEntries.elements();
        while (en.hasMoreElements()) {
            CatalogEntry e = (CatalogEntry) en.nextElement();
            if (e.getEntryType() == OVERRIDE) {
                over = e.getEntryArg(0).equalsIgnoreCase("YES");
            } else if (e.getEntryType() == ENTITY && e.getEntryArg(0).equals(entityName) && (over || systemId2 == null)) {
                return e.getEntryArg(1);
            }
        }
        return resolveSubordinateCatalogs(ENTITY, entityName, publicId, systemId2);
    }

    public String resolveNotation(String notationName, String publicId, String systemId) throws IOException {
        String resolved;
        String resolved2;
        this.catalogManager.debug.message(3, "resolveNotation(" + notationName + "," + publicId + "," + systemId + ")");
        String systemId2 = normalizeURI(systemId);
        if (publicId != null && publicId.startsWith("urn:publicid:")) {
            publicId = PublicId.decodeURN(publicId);
        }
        if (systemId2 != null && systemId2.startsWith("urn:publicid:")) {
            String systemId3 = PublicId.decodeURN(systemId2);
            if (publicId != null && !publicId.equals(systemId3)) {
                this.catalogManager.debug.message(1, "urn:publicid: system identifier differs from public identifier; using public identifier");
                systemId2 = null;
            } else {
                publicId = systemId3;
                systemId2 = null;
            }
        }
        if (systemId2 != null && (resolved2 = resolveLocalSystem(systemId2)) != null) {
            return resolved2;
        }
        if (publicId != null && (resolved = resolveLocalPublic(NOTATION, notationName, publicId, systemId2)) != null) {
            return resolved;
        }
        boolean over = this.default_override;
        Enumeration en = this.catalogEntries.elements();
        while (en.hasMoreElements()) {
            CatalogEntry e = (CatalogEntry) en.nextElement();
            if (e.getEntryType() == OVERRIDE) {
                over = e.getEntryArg(0).equalsIgnoreCase("YES");
            } else if (e.getEntryType() == NOTATION && e.getEntryArg(0).equals(notationName) && (over || systemId2 == null)) {
                return e.getEntryArg(1);
            }
        }
        return resolveSubordinateCatalogs(NOTATION, notationName, publicId, systemId2);
    }

    public String resolvePublic(String publicId, String systemId) throws IOException {
        String resolved;
        this.catalogManager.debug.message(3, "resolvePublic(" + publicId + "," + systemId + ")");
        String systemId2 = normalizeURI(systemId);
        if (publicId != null && publicId.startsWith("urn:publicid:")) {
            publicId = PublicId.decodeURN(publicId);
        }
        if (systemId2 != null && systemId2.startsWith("urn:publicid:")) {
            String systemId3 = PublicId.decodeURN(systemId2);
            if (publicId != null && !publicId.equals(systemId3)) {
                this.catalogManager.debug.message(1, "urn:publicid: system identifier differs from public identifier; using public identifier");
                systemId2 = null;
            } else {
                publicId = systemId3;
                systemId2 = null;
            }
        }
        if (systemId2 != null && (resolved = resolveLocalSystem(systemId2)) != null) {
            return resolved;
        }
        String resolved2 = resolveLocalPublic(PUBLIC, null, publicId, systemId2);
        if (resolved2 != null) {
            return resolved2;
        }
        return resolveSubordinateCatalogs(PUBLIC, null, publicId, systemId2);
    }

    protected synchronized String resolveLocalPublic(int entityType, String entityName, String publicId, String systemId) throws IOException {
        String resolved;
        String publicId2 = PublicId.normalize(publicId);
        if (systemId != null && (resolved = resolveLocalSystem(systemId)) != null) {
            return resolved;
        }
        boolean over = this.default_override;
        Enumeration en = this.catalogEntries.elements();
        while (en.hasMoreElements()) {
            CatalogEntry e = (CatalogEntry) en.nextElement();
            if (e.getEntryType() == OVERRIDE) {
                over = e.getEntryArg(0).equalsIgnoreCase("YES");
            } else if (e.getEntryType() == PUBLIC && e.getEntryArg(0).equals(publicId2) && (over || systemId == null)) {
                return e.getEntryArg(1);
            }
        }
        boolean over2 = this.default_override;
        Enumeration en2 = this.catalogEntries.elements();
        Vector delCats = new Vector();
        while (en2.hasMoreElements()) {
            CatalogEntry e2 = (CatalogEntry) en2.nextElement();
            if (e2.getEntryType() == OVERRIDE) {
                over2 = e2.getEntryArg(0).equalsIgnoreCase("YES");
            } else if (e2.getEntryType() == DELEGATE_PUBLIC && (over2 || systemId == null)) {
                String p = e2.getEntryArg(0);
                if (p.length() <= publicId2.length() && p.equals(publicId2.substring(0, p.length()))) {
                    delCats.addElement(e2.getEntryArg(1));
                }
            }
        }
        if (delCats.size() <= 0) {
            return null;
        }
        Enumeration enCats = delCats.elements();
        if (this.catalogManager.debug.getDebug() > 1) {
            this.catalogManager.debug.message(2, "Switching to delegated catalog(s):");
            while (enCats.hasMoreElements()) {
                String delegatedCatalog = (String) enCats.nextElement();
                this.catalogManager.debug.message(2, "\t" + delegatedCatalog);
            }
        }
        Catalog dcat = newCatalog();
        Enumeration enCats2 = delCats.elements();
        while (enCats2.hasMoreElements()) {
            String delegatedCatalog2 = (String) enCats2.nextElement();
            dcat.parseCatalog(delegatedCatalog2);
        }
        return dcat.resolvePublic(publicId2, null);
    }

    public String resolveSystem(String systemId) throws IOException {
        String resolved;
        this.catalogManager.debug.message(3, "resolveSystem(" + systemId + ")");
        String systemId2 = normalizeURI(systemId);
        if (systemId2 != null && systemId2.startsWith("urn:publicid:")) {
            return resolvePublic(PublicId.decodeURN(systemId2), null);
        }
        if (systemId2 != null && (resolved = resolveLocalSystem(systemId2)) != null) {
            return resolved;
        }
        return resolveSubordinateCatalogs(SYSTEM, null, null, systemId2);
    }

    protected String resolveLocalSystem(String systemId) throws IOException {
        String osname = System.getProperty("os.name");
        boolean windows = osname.indexOf("Windows") >= 0;
        Enumeration en = this.catalogEntries.elements();
        while (en.hasMoreElements()) {
            CatalogEntry e = (CatalogEntry) en.nextElement();
            if (e.getEntryType() == SYSTEM && (e.getEntryArg(0).equals(systemId) || (windows && e.getEntryArg(0).equalsIgnoreCase(systemId)))) {
                return e.getEntryArg(1);
            }
        }
        Enumeration en2 = this.catalogEntries.elements();
        String startString = null;
        String prefix = null;
        while (en2.hasMoreElements()) {
            CatalogEntry e2 = (CatalogEntry) en2.nextElement();
            if (e2.getEntryType() == REWRITE_SYSTEM) {
                String p = e2.getEntryArg(0);
                if (p.length() <= systemId.length() && p.equals(systemId.substring(0, p.length())) && (startString == null || p.length() > startString.length())) {
                    startString = p;
                    prefix = e2.getEntryArg(1);
                }
            }
        }
        if (prefix != null) {
            return String.valueOf(prefix) + systemId.substring(startString.length());
        }
        Enumeration en3 = this.catalogEntries.elements();
        String suffixString = null;
        String suffixURI = null;
        while (en3.hasMoreElements()) {
            CatalogEntry e3 = (CatalogEntry) en3.nextElement();
            if (e3.getEntryType() == SYSTEM_SUFFIX) {
                String p2 = e3.getEntryArg(0);
                if (p2.length() <= systemId.length() && systemId.endsWith(p2) && (suffixString == null || p2.length() > suffixString.length())) {
                    suffixString = p2;
                    suffixURI = e3.getEntryArg(1);
                }
            }
        }
        if (suffixURI != null) {
            return suffixURI;
        }
        Enumeration en4 = this.catalogEntries.elements();
        Vector delCats = new Vector();
        while (en4.hasMoreElements()) {
            CatalogEntry e4 = (CatalogEntry) en4.nextElement();
            if (e4.getEntryType() == DELEGATE_SYSTEM) {
                String p3 = e4.getEntryArg(0);
                if (p3.length() <= systemId.length() && p3.equals(systemId.substring(0, p3.length()))) {
                    delCats.addElement(e4.getEntryArg(1));
                }
            }
        }
        if (delCats.size() > 0) {
            Enumeration enCats = delCats.elements();
            if (this.catalogManager.debug.getDebug() > 1) {
                this.catalogManager.debug.message(2, "Switching to delegated catalog(s):");
                while (enCats.hasMoreElements()) {
                    String delegatedCatalog = (String) enCats.nextElement();
                    this.catalogManager.debug.message(2, "\t" + delegatedCatalog);
                }
            }
            Catalog dcat = newCatalog();
            Enumeration enCats2 = delCats.elements();
            while (enCats2.hasMoreElements()) {
                String delegatedCatalog2 = (String) enCats2.nextElement();
                dcat.parseCatalog(delegatedCatalog2);
            }
            return dcat.resolveSystem(systemId);
        }
        return null;
    }

    public String resolveURI(String uri) throws IOException {
        String resolved;
        this.catalogManager.debug.message(3, "resolveURI(" + uri + ")");
        String uri2 = normalizeURI(uri);
        if (uri2 != null && uri2.startsWith("urn:publicid:")) {
            return resolvePublic(PublicId.decodeURN(uri2), null);
        }
        if (uri2 != null && (resolved = resolveLocalURI(uri2)) != null) {
            return resolved;
        }
        return resolveSubordinateCatalogs(URI, null, null, uri2);
    }

    protected String resolveLocalURI(String uri) throws IOException {
        Enumeration en = this.catalogEntries.elements();
        while (en.hasMoreElements()) {
            CatalogEntry e = (CatalogEntry) en.nextElement();
            if (e.getEntryType() == URI && e.getEntryArg(0).equals(uri)) {
                return e.getEntryArg(1);
            }
        }
        Enumeration en2 = this.catalogEntries.elements();
        String startString = null;
        String prefix = null;
        while (en2.hasMoreElements()) {
            CatalogEntry e2 = (CatalogEntry) en2.nextElement();
            if (e2.getEntryType() == REWRITE_URI) {
                String p = e2.getEntryArg(0);
                if (p.length() <= uri.length() && p.equals(uri.substring(0, p.length())) && (startString == null || p.length() > startString.length())) {
                    startString = p;
                    prefix = e2.getEntryArg(1);
                }
            }
        }
        if (prefix != null) {
            return String.valueOf(prefix) + uri.substring(startString.length());
        }
        Enumeration en3 = this.catalogEntries.elements();
        String suffixString = null;
        String suffixURI = null;
        while (en3.hasMoreElements()) {
            CatalogEntry e3 = (CatalogEntry) en3.nextElement();
            if (e3.getEntryType() == URI_SUFFIX) {
                String p2 = e3.getEntryArg(0);
                if (p2.length() <= uri.length() && uri.endsWith(p2) && (suffixString == null || p2.length() > suffixString.length())) {
                    suffixString = p2;
                    suffixURI = e3.getEntryArg(1);
                }
            }
        }
        if (suffixURI != null) {
            return suffixURI;
        }
        Enumeration en4 = this.catalogEntries.elements();
        Vector delCats = new Vector();
        while (en4.hasMoreElements()) {
            CatalogEntry e4 = (CatalogEntry) en4.nextElement();
            if (e4.getEntryType() == DELEGATE_URI) {
                String p3 = e4.getEntryArg(0);
                if (p3.length() <= uri.length() && p3.equals(uri.substring(0, p3.length()))) {
                    delCats.addElement(e4.getEntryArg(1));
                }
            }
        }
        if (delCats.size() > 0) {
            Enumeration enCats = delCats.elements();
            if (this.catalogManager.debug.getDebug() > 1) {
                this.catalogManager.debug.message(2, "Switching to delegated catalog(s):");
                while (enCats.hasMoreElements()) {
                    String delegatedCatalog = (String) enCats.nextElement();
                    this.catalogManager.debug.message(2, "\t" + delegatedCatalog);
                }
            }
            Catalog dcat = newCatalog();
            Enumeration enCats2 = delCats.elements();
            while (enCats2.hasMoreElements()) {
                String delegatedCatalog2 = (String) enCats2.nextElement();
                dcat.parseCatalog(delegatedCatalog2);
            }
            return dcat.resolveURI(uri);
        }
        return null;
    }

    protected synchronized String resolveSubordinateCatalogs(int entityType, String entityName, String publicId, String systemId) throws IOException {
        Catalog c;
        for (int catPos = 0; catPos < this.catalogs.size(); catPos++) {
            try {
                c = (Catalog) this.catalogs.elementAt(catPos);
            } catch (ClassCastException e) {
                String catfile = (String) this.catalogs.elementAt(catPos);
                c = newCatalog();
                try {
                    try {
                        try {
                            c.parseCatalog(catfile);
                        } catch (MalformedURLException e2) {
                            this.catalogManager.debug.message(1, "Malformed Catalog URL", catfile);
                        }
                    } catch (FileNotFoundException e3) {
                        this.catalogManager.debug.message(1, "Failed to load catalog, file not found", catfile);
                    }
                } catch (IOException e4) {
                    this.catalogManager.debug.message(1, "Failed to load catalog, I/O error", catfile);
                }
                this.catalogs.setElementAt(c, catPos);
            }
            String resolved = null;
            if (entityType == DOCTYPE) {
                resolved = c.resolveDoctype(entityName, publicId, systemId);
            } else if (entityType == DOCUMENT) {
                resolved = c.resolveDocument();
            } else if (entityType == ENTITY) {
                resolved = c.resolveEntity(entityName, publicId, systemId);
            } else if (entityType == NOTATION) {
                resolved = c.resolveNotation(entityName, publicId, systemId);
            } else if (entityType == PUBLIC) {
                resolved = c.resolvePublic(publicId, systemId);
            } else if (entityType == SYSTEM) {
                resolved = c.resolveSystem(systemId);
            } else if (entityType == URI) {
                resolved = c.resolveURI(systemId);
            }
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    protected String fixSlashes(String sysid) {
        return sysid.replace('\\', '/');
    }

    protected String normalizeURI(String uriref) {
        String newRef = "";
        if (uriref == null) {
            return null;
        }
        try {
            byte[] bytes = uriref.getBytes("UTF-8");
            for (int count = 0; count < bytes.length; count++) {
                int ch = bytes[count] & 255;
                newRef = (ch <= 32 || ch > 127 || ch == 34 || ch == 60 || ch == 62 || ch == 92 || ch == 94 || ch == 96 || ch == 123 || ch == 124 || ch == 125 || ch == 127) ? String.valueOf(newRef) + encodedByte(ch) : String.valueOf(newRef) + ((char) bytes[count]);
            }
            return newRef;
        } catch (UnsupportedEncodingException e) {
            this.catalogManager.debug.message(1, "UTF-8 is an unsupported encoding!?");
            return uriref;
        }
    }

    protected String encodedByte(int b) {
        String hex = Integer.toHexString(b).toUpperCase();
        if (hex.length() < 2) {
            return "%0" + hex;
        }
        return "%" + hex;
    }
}
