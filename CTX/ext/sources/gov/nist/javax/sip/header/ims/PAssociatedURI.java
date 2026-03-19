package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.header.AddressParametersHeader;
import java.text.ParseException;
import javax.sip.address.URI;
import javax.sip.header.ExtensionHeader;

public class PAssociatedURI extends AddressParametersHeader implements PAssociatedURIHeader, SIPHeaderNamesIms, ExtensionHeader {
    public PAssociatedURI() {
        super("P-Associated-URI");
    }

    public PAssociatedURI(AddressImpl addressImpl) {
        super("P-Associated-URI");
        this.address = addressImpl;
    }

    public PAssociatedURI(GenericURI genericURI) {
        super("P-Associated-URI");
        this.address = new AddressImpl();
        this.address.setURI(genericURI);
    }

    @Override
    public String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.address.getAddressType() == 2) {
            stringBuffer.append(Separators.LESS_THAN);
        }
        stringBuffer.append(this.address.encode());
        if (this.address.getAddressType() == 2) {
            stringBuffer.append(Separators.GREATER_THAN);
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON + this.parameters.encode());
        }
        return stringBuffer.toString();
    }

    @Override
    public void setAssociatedURI(URI uri) throws NullPointerException {
        if (uri == null) {
            throw new NullPointerException("null URI");
        }
        this.address.setURI(uri);
    }

    @Override
    public URI getAssociatedURI() {
        return this.address.getURI();
    }

    @Override
    public Object clone() {
        PAssociatedURI pAssociatedURI = (PAssociatedURI) super.clone();
        if (this.address != null) {
            pAssociatedURI.address = (AddressImpl) this.address.clone();
        }
        return pAssociatedURI;
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }
}
