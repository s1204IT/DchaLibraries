package mf.javax.xml.stream.events;

import java.util.List;

public interface DTD extends XMLEvent {
    String getDocumentTypeDeclaration();

    List getEntities();
}
