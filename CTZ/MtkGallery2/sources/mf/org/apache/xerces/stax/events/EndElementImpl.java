package mf.org.apache.xerces.stax.events;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import mf.javax.xml.namespace.QName;
import mf.javax.xml.stream.Location;
import mf.javax.xml.stream.XMLStreamException;
import mf.javax.xml.stream.events.EndElement;

public final class EndElementImpl extends ElementImpl implements EndElement {
    public EndElementImpl(QName name, Iterator namespaces, Location location) {
        super(name, false, namespaces, location);
    }

    @Override
    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        try {
            writer.write("</");
            QName name = getName();
            String prefix = name.getPrefix();
            if (prefix != null && prefix.length() > 0) {
                writer.write(prefix);
                writer.write(58);
            }
            writer.write(name.getLocalPart());
            writer.write(62);
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }
}
