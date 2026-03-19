package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import sun.util.ResourceBundleEnumeration;

public class PropertyResourceBundle extends ResourceBundle {
    private final Map<String, Object> lookup;

    public PropertyResourceBundle(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        this.lookup = new HashMap(properties);
    }

    public PropertyResourceBundle(Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        this.lookup = new HashMap(properties);
    }

    @Override
    public Object handleGetObject(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        return this.lookup.get(str);
    }

    @Override
    public Enumeration<String> getKeys() {
        ResourceBundle resourceBundle = this.parent;
        return new ResourceBundleEnumeration(this.lookup.keySet(), resourceBundle != null ? resourceBundle.getKeys() : null);
    }

    @Override
    protected Set<String> handleKeySet() {
        return this.lookup.keySet();
    }
}
