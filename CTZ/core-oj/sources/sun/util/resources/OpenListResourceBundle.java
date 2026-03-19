package sun.util.resources;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import sun.util.ResourceBundleEnumeration;

public abstract class OpenListResourceBundle extends ResourceBundle {
    private volatile Set<String> keyset;
    private volatile Map<String, Object> lookup = null;

    protected abstract Object[][] getContents();

    protected OpenListResourceBundle() {
    }

    @Override
    protected Object handleGetObject(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        loadLookupTablesIfNecessary();
        return this.lookup.get(str);
    }

    @Override
    public Enumeration<String> getKeys() {
        ResourceBundle resourceBundle = this.parent;
        return new ResourceBundleEnumeration(handleKeySet(), resourceBundle != null ? resourceBundle.getKeys() : null);
    }

    @Override
    protected Set<String> handleKeySet() {
        loadLookupTablesIfNecessary();
        return this.lookup.keySet();
    }

    @Override
    public Set<String> keySet() {
        if (this.keyset != null) {
            return this.keyset;
        }
        Set<String> setCreateSet = createSet();
        setCreateSet.addAll(handleKeySet());
        if (this.parent != null) {
            setCreateSet.addAll(this.parent.keySet());
        }
        synchronized (this) {
            if (this.keyset == null) {
                this.keyset = setCreateSet;
            }
        }
        return this.keyset;
    }

    void loadLookupTablesIfNecessary() {
        if (this.lookup == null) {
            loadLookup();
        }
    }

    private void loadLookup() {
        Object[][] contents = getContents();
        Map<String, Object> mapCreateMap = createMap(contents.length);
        for (int i = 0; i < contents.length; i++) {
            String str = (String) contents[i][0];
            Object obj = contents[i][1];
            if (str == null || obj == null) {
                throw new NullPointerException();
            }
            mapCreateMap.put(str, obj);
        }
        synchronized (this) {
            if (this.lookup == null) {
                this.lookup = mapCreateMap;
            }
        }
    }

    protected <K, V> Map<K, V> createMap(int i) {
        return new HashMap(i);
    }

    protected <E> Set<E> createSet() {
        return new HashSet();
    }
}
