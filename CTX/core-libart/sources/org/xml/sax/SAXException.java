package org.xml.sax;

public class SAXException extends Exception {
    private Exception exception;

    public SAXException() {
        this.exception = null;
    }

    public SAXException(String str) {
        super(str);
        this.exception = null;
    }

    public SAXException(Exception exc) {
        this.exception = exc;
    }

    public SAXException(String str, Exception exc) {
        super(str);
        this.exception = exc;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (message == null && this.exception != null) {
            return this.exception.getMessage();
        }
        return message;
    }

    public Exception getException() {
        return this.exception;
    }

    @Override
    public String toString() {
        if (this.exception != null) {
            return this.exception.toString();
        }
        return super.toString();
    }
}
