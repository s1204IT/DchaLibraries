package mf.javax.xml.namespace;

public interface NamespaceContext {
    String getNamespaceURI(String str);

    String getPrefix(String str);
}
