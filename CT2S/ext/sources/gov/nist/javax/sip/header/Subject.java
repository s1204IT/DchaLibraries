package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.SubjectHeader;

public class Subject extends SIPHeader implements SubjectHeader {
    private static final long serialVersionUID = -6479220126758862528L;
    protected String subject;

    public Subject() {
        super("Subject");
    }

    @Override
    public String encodeBody() {
        return this.subject != null ? this.subject : "";
    }

    @Override
    public void setSubject(String subject) throws ParseException {
        if (subject == null) {
            throw new NullPointerException("JAIN-SIP Exception,  Subject, setSubject(), the subject parameter is null");
        }
        this.subject = subject;
    }

    @Override
    public String getSubject() {
        return this.subject;
    }
}
