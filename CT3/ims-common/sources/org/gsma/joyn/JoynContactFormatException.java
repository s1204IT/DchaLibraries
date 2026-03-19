package org.gsma.joyn;

public class JoynContactFormatException extends JoynServiceException {
    static final long serialVersionUID = 1;

    public JoynContactFormatException() {
        super("joyn contact format not supported");
    }
}
