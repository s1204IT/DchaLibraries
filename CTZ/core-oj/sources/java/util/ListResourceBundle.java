package java.util;

import sun.util.ResourceBundleEnumeration;

public abstract class ListResourceBundle extends ResourceBundle {
    private volatile Map<String, Object> lookup = null;

    protected abstract Object[][] getContents();

    @Override
    public final Object handleGetObject(String str) {
        if (this.lookup == null) {
            loadLookup();
        }
        if (str == null) {
            throw new NullPointerException();
        }
        return this.lookup.get(str);
    }

    @Override
    public Enumeration<String> getKeys() {
        if (this.lookup == null) {
            loadLookup();
        }
        ResourceBundle resourceBundle = this.parent;
        return new ResourceBundleEnumeration(this.lookup.keySet(), resourceBundle != null ? resourceBundle.getKeys() : null);
    }

    @Override
    protected Set<String> handleKeySet() {
        if (this.lookup == null) {
            loadLookup();
        }
        return this.lookup.keySet();
    }

    private synchronized void loadLookup() {
        if (this.lookup != null) {
            return;
        }
        Object[][] contents = getContents();
        HashMap map = new HashMap(contents.length);
        for (int i = 0; i < contents.length; i++) {
            String str = (String) contents[i][0];
            Object obj = contents[i][1];
            if (str == null || obj == null) {
                throw new NullPointerException();
            }
            map.put(str, obj);
        }
        this.lookup = map;
    }
}
