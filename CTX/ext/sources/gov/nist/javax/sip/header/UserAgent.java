package gov.nist.javax.sip.header;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.sip.header.UserAgentHeader;

public class UserAgent extends SIPHeader implements UserAgentHeader {
    private static final long serialVersionUID = 4561239179796364295L;
    protected List productTokens;

    private String encodeProduct() {
        StringBuffer stringBuffer = new StringBuffer();
        ListIterator listIterator = this.productTokens.listIterator();
        while (listIterator.hasNext()) {
            stringBuffer.append((String) listIterator.next());
        }
        return stringBuffer.toString();
    }

    @Override
    public void addProductToken(String str) {
        this.productTokens.add(str);
    }

    public UserAgent() {
        super("User-Agent");
        this.productTokens = new LinkedList();
    }

    @Override
    public String encodeBody() {
        return encodeProduct();
    }

    @Override
    public ListIterator getProduct() {
        if (this.productTokens == null || this.productTokens.isEmpty()) {
            return null;
        }
        return this.productTokens.listIterator();
    }

    @Override
    public void setProduct(List list) throws ParseException {
        if (list == null) {
            throw new NullPointerException("JAIN-SIP Exception, UserAgent, setProduct(), the  product parameter is null");
        }
        this.productTokens = list;
    }

    @Override
    public Object clone() {
        UserAgent userAgent = (UserAgent) super.clone();
        if (this.productTokens != null) {
            userAgent.productTokens = new LinkedList(this.productTokens);
        }
        return userAgent;
    }
}
