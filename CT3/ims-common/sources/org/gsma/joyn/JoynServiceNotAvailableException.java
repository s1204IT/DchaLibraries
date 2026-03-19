package org.gsma.joyn;

public class JoynServiceNotAvailableException extends JoynServiceException {
    static final long serialVersionUID = 1;

    public JoynServiceNotAvailableException() {
        super("joyn service not available");
    }
}
