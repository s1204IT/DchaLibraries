package mf.javax.xml.stream;

import mf.javax.xml.namespace.NamespaceContext;

public interface XMLStreamReader {
    int getAttributeCount();

    String getAttributeLocalName(int i);

    String getAttributeNamespace(int i);

    String getAttributePrefix(int i);

    String getAttributeType(int i);

    String getAttributeValue(int i);

    String getCharacterEncodingScheme();

    int getEventType();

    String getLocalName();

    Location getLocation();

    NamespaceContext getNamespaceContext();

    int getNamespaceCount();

    String getNamespacePrefix(int i);

    String getNamespaceURI();

    String getPIData();

    String getPITarget();

    String getPrefix();

    Object getProperty(String str) throws IllegalArgumentException;

    String getText();

    char[] getTextCharacters();

    int getTextLength();

    int getTextStart();

    String getVersion();

    boolean hasNext() throws XMLStreamException;

    boolean isAttributeSpecified(int i);

    int next() throws XMLStreamException;

    boolean standaloneSet();
}
