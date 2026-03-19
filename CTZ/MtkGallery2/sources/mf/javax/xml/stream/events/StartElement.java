package mf.javax.xml.stream.events;

import java.util.Iterator;
import mf.javax.xml.namespace.NamespaceContext;
import mf.javax.xml.namespace.QName;

public interface StartElement extends XMLEvent {
    Iterator getAttributes();

    QName getName();

    NamespaceContext getNamespaceContext();

    Iterator getNamespaces();
}
