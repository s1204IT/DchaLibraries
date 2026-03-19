package mf.org.apache.xerces.stax.events;

import java.io.IOException;
import java.io.Writer;
import mf.javax.xml.stream.Location;
import mf.javax.xml.stream.XMLStreamException;
import mf.javax.xml.stream.events.EntityDeclaration;
import mf.javax.xml.stream.events.EntityReference;

public final class EntityReferenceImpl extends XMLEventImpl implements EntityReference {
    private final EntityDeclaration fDecl;
    private final String fName;

    public EntityReferenceImpl(EntityDeclaration decl, Location location) {
        this(decl != null ? decl.getName() : "", decl, location);
    }

    public EntityReferenceImpl(String name, EntityDeclaration decl, Location location) {
        super(9, location);
        this.fName = name != null ? name : "";
        this.fDecl = decl;
    }

    public EntityDeclaration getDeclaration() {
        return this.fDecl;
    }

    @Override
    public String getName() {
        return this.fName;
    }

    @Override
    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        try {
            writer.write(38);
            writer.write(this.fName);
            writer.write(59);
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }
}
