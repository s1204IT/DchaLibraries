package org.gsma.joyn;

public class JoynServiceNotRegisteredException extends JoynServiceException {
    static final long serialVersionUID = 1;

    public JoynServiceNotRegisteredException() {
        super("joyn service not registered");
    }
}
