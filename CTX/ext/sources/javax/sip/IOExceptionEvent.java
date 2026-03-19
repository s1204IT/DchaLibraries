package javax.sip;

import java.util.EventObject;

public class IOExceptionEvent extends EventObject {
    private String mHost;
    private int mPort;
    private String mTransport;

    public IOExceptionEvent(Object obj, String str, int i, String str2) {
        super(obj);
        this.mHost = str;
        this.mPort = i;
        this.mTransport = str2;
    }

    public String getHost() {
        return this.mHost;
    }

    public int getPort() {
        return this.mPort;
    }

    public String getTransport() {
        return this.mTransport;
    }
}
