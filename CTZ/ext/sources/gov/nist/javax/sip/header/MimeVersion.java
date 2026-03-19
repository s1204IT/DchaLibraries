package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import javax.sip.InvalidArgumentException;
import javax.sip.header.MimeVersionHeader;

public class MimeVersion extends SIPHeader implements MimeVersionHeader {
    private static final long serialVersionUID = -7951589626435082068L;
    protected int majorVersion;
    protected int minorVersion;

    public MimeVersion() {
        super("MIME-Version");
    }

    @Override
    public int getMinorVersion() {
        return this.minorVersion;
    }

    @Override
    public int getMajorVersion() {
        return this.majorVersion;
    }

    @Override
    public void setMinorVersion(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("JAIN-SIP Exception, MimeVersion, setMinorVersion(), the minorVersion parameter is null");
        }
        this.minorVersion = i;
    }

    @Override
    public void setMajorVersion(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("JAIN-SIP Exception, MimeVersion, setMajorVersion(), the majorVersion parameter is null");
        }
        this.majorVersion = i;
    }

    @Override
    public String encodeBody() {
        return Integer.toString(this.majorVersion) + Separators.DOT + Integer.toString(this.minorVersion);
    }
}
