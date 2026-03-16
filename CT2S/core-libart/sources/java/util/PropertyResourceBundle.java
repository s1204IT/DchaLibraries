package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class PropertyResourceBundle extends ResourceBundle {
    Properties resources;

    public PropertyResourceBundle(InputStream stream) throws IOException {
        if (stream == null) {
            throw new NullPointerException("stream == null");
        }
        this.resources = new Properties();
        this.resources.load(stream);
    }

    public PropertyResourceBundle(Reader reader) throws IOException {
        this.resources = new Properties();
        this.resources.load(reader);
    }

    @Override
    protected Set<String> handleKeySet() {
        return this.resources.stringPropertyNames();
    }

    private Enumeration<String> getLocalKeys() {
        return this.resources.propertyNames();
    }

    @Override
    public Enumeration<String> getKeys() {
        return this.parent == null ? getLocalKeys() : new Enumeration<String>() {
            Enumeration<String> local;
            String nextElement;
            Enumeration<String> pEnum;

            {
                this.local = PropertyResourceBundle.this.getLocalKeys();
                this.pEnum = PropertyResourceBundle.this.parent.getKeys();
            }

            private boolean findNext() {
                if (this.nextElement != null) {
                    return true;
                }
                while (this.pEnum.hasMoreElements()) {
                    String next = this.pEnum.nextElement();
                    if (!PropertyResourceBundle.this.resources.containsKey(next)) {
                        this.nextElement = next;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean hasMoreElements() {
                if (this.local.hasMoreElements()) {
                    return true;
                }
                return findNext();
            }

            @Override
            public String nextElement() {
                if (this.local.hasMoreElements()) {
                    return this.local.nextElement();
                }
                if (findNext()) {
                    String result = this.nextElement;
                    this.nextElement = null;
                    return result;
                }
                return this.pEnum.nextElement();
            }
        };
    }

    @Override
    public Object handleGetObject(String key) {
        return this.resources.get(key);
    }
}
