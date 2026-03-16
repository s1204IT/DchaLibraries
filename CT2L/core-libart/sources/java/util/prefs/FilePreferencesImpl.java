package java.util.prefs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

public class FilePreferencesImpl extends AbstractPreferences {
    private static final String PREFS_FILE_NAME = "prefs.xml";
    private File dir;
    private final String path;
    private Properties prefs;
    private File prefsFile;
    private Set<String> removed;
    private Set<String> updated;

    public FilePreferencesImpl(String path, boolean isUserNode) {
        super(null, "");
        this.removed = new HashSet();
        this.updated = new HashSet();
        this.path = path;
        this.userNode = isUserNode;
        initPrefs();
    }

    private FilePreferencesImpl(AbstractPreferences parent, String name) {
        super(parent, name);
        this.removed = new HashSet();
        this.updated = new HashSet();
        this.path = ((FilePreferencesImpl) parent).path + File.separator + name;
        initPrefs();
    }

    private void initPrefs() {
        this.dir = new File(this.path);
        this.newNode = !this.dir.exists();
        this.prefsFile = new File(this.path + File.separator + PREFS_FILE_NAME);
        this.prefs = XMLParser.readXmlPreferences(this.prefsFile);
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        String[] names = this.dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File parent, String name) {
                return new File(FilePreferencesImpl.this.path + File.separator + name).isDirectory();
            }
        });
        if (names == null) {
            throw new BackingStoreException("Cannot get child names for " + toString() + " (path is " + this.path + ")");
        }
        return names;
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        FilePreferencesImpl child = new FilePreferencesImpl(this, name);
        return child;
    }

    @Override
    protected void flushSpi() throws Throwable {
        try {
            if (!isRemoved()) {
                Properties currentPrefs = XMLParser.readXmlPreferences(this.prefsFile);
                Iterator<String> it = this.removed.iterator();
                while (it.hasNext()) {
                    currentPrefs.remove(it.next());
                }
                this.removed.clear();
                for (String str : this.updated) {
                    currentPrefs.put(str, this.prefs.get(str));
                }
                this.updated.clear();
                this.prefs = currentPrefs;
                XMLParser.writeXmlPreferences(this.prefsFile, this.prefs);
            }
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
    }

    @Override
    protected String getSpi(String key) {
        try {
            if (this.prefs == null) {
                this.prefs = XMLParser.readXmlPreferences(this.prefsFile);
            }
            return this.prefs.getProperty(key);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        Set<Object> ks = this.prefs.keySet();
        return (String[]) ks.toArray(new String[ks.size()]);
    }

    @Override
    protected void putSpi(String name, String value) {
        this.prefs.setProperty(name, value);
        this.updated.add(name);
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        this.prefsFile.delete();
        boolean removeSucceed = this.dir.delete();
        if (!removeSucceed) {
            throw new BackingStoreException("Cannot remove " + toString());
        }
    }

    @Override
    protected void removeSpi(String key) {
        this.prefs.remove(key);
        this.updated.remove(key);
        this.removed.add(key);
    }

    @Override
    protected void syncSpi() throws Throwable {
        flushSpi();
    }
}
