package org.gsma.joyn;

public class JoynServiceException extends Exception {
    static final long serialVersionUID = 1;

    public JoynServiceException(String error) {
        super(error);
    }
}
