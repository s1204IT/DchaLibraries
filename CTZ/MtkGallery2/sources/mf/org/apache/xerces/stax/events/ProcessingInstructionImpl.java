package mf.org.apache.xerces.stax.events;

import java.io.IOException;
import java.io.Writer;
import mf.javax.xml.stream.Location;
import mf.javax.xml.stream.XMLStreamException;
import mf.javax.xml.stream.events.ProcessingInstruction;

public final class ProcessingInstructionImpl extends XMLEventImpl implements ProcessingInstruction {
    private final String fData;
    private final String fTarget;

    public ProcessingInstructionImpl(String target, String data, Location location) {
        super(3, location);
        this.fTarget = target != null ? target : "";
        this.fData = data;
    }

    @Override
    public String getTarget() {
        return this.fTarget;
    }

    @Override
    public String getData() {
        return this.fData;
    }

    @Override
    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        try {
            writer.write("<?");
            writer.write(this.fTarget);
            if (this.fData != null && this.fData.length() > 0) {
                writer.write(32);
                writer.write(this.fData);
            }
            writer.write("?>");
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }
}
