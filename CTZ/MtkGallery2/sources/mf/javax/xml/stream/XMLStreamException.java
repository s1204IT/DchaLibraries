package mf.javax.xml.stream;

public class XMLStreamException extends Exception {
    protected Location location;
    protected Throwable nested;

    public XMLStreamException() {
    }

    public XMLStreamException(String msg) {
        super(msg);
    }

    public XMLStreamException(Throwable th) {
        super(th);
        this.nested = th;
    }

    public Throwable getNestedException() {
        return this.nested;
    }

    public Location getLocation() {
        return this.location;
    }
}
