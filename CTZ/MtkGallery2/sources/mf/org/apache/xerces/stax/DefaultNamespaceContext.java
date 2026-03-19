package mf.org.apache.xerces.stax;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import mf.javax.xml.namespace.NamespaceContext;

public final class DefaultNamespaceContext implements NamespaceContext {
    private static final DefaultNamespaceContext DEFAULT_NAMESPACE_CONTEXT_INSTANCE = new DefaultNamespaceContext();

    private DefaultNamespaceContext() {
    }

    public static DefaultNamespaceContext getInstance() {
        return DEFAULT_NAMESPACE_CONTEXT_INSTANCE;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null.");
        }
        if ("xml".equals(prefix)) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        if ("xmlns".equals(prefix)) {
            return "http://www.w3.org/2000/xmlns/";
        }
        return "";
    }

    @Override
    public String getPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("Namespace URI cannot be null.");
        }
        if ("http://www.w3.org/XML/1998/namespace".equals(namespaceURI)) {
            return "xml";
        }
        if ("http://www.w3.org/2000/xmlns/".equals(namespaceURI)) {
            return "xmlns";
        }
        return null;
    }

    public Iterator getPrefixes(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("Namespace URI cannot be null.");
        }
        if ("http://www.w3.org/XML/1998/namespace".equals(namespaceURI)) {
            return new Iterator() {
                boolean more = true;

                @Override
                public boolean hasNext() {
                    return this.more;
                }

                @Override
                public Object next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    this.more = false;
                    return "xml";
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        if ("http://www.w3.org/2000/xmlns/".equals(namespaceURI)) {
            return new Iterator() {
                boolean more = true;

                @Override
                public boolean hasNext() {
                    return this.more;
                }

                @Override
                public Object next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    this.more = false;
                    return "xmlns";
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        return Collections.EMPTY_LIST.iterator();
    }
}
