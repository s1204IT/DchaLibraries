package mf.org.w3c.dom.ls;

public class LSException extends RuntimeException {
    public short code;

    public LSException(short code, String message) {
        super(message);
        this.code = code;
    }
}
