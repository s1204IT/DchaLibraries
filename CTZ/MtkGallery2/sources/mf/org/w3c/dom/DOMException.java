package mf.org.w3c.dom;

public class DOMException extends RuntimeException {
    static final long serialVersionUID = 6627732366795969916L;
    public short code;

    public DOMException(short code, String message) {
        super(message);
        this.code = code;
    }
}
