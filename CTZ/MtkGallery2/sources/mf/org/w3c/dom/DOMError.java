package mf.org.w3c.dom;

public interface DOMError {
    DOMLocator getLocation();

    String getMessage();

    short getSeverity();
}
