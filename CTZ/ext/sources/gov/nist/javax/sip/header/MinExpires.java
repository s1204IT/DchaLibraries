package gov.nist.javax.sip.header;

import javax.sip.InvalidArgumentException;
import javax.sip.header.MinExpiresHeader;

public class MinExpires extends SIPHeader implements MinExpiresHeader {
    private static final long serialVersionUID = 7001828209606095801L;
    protected int expires;

    public MinExpires() {
        super("Min-Expires");
    }

    @Override
    public String encodeBody() {
        return Integer.toString(this.expires);
    }

    @Override
    public int getExpires() {
        return this.expires;
    }

    @Override
    public void setExpires(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("bad argument " + i);
        }
        this.expires = i;
    }
}
