package gov.nist.javax.sip.header.extensions;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.ExtensionHeader;

public class MinSE extends ParametersHeader implements ExtensionHeader, MinSEHeader {
    public static final String NAME = "Min-SE";
    private static final long serialVersionUID = 3134344915465784267L;
    public int expires;

    public MinSE() {
        super("Min-SE");
    }

    @Override
    public String encodeBody() {
        String string = Integer.toString(this.expires);
        if (!this.parameters.isEmpty()) {
            return string + Separators.SEMICOLON + this.parameters.encode();
        }
        return string;
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }

    public int getExpires() {
        return this.expires;
    }

    public void setExpires(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("bad argument " + i);
        }
        this.expires = i;
    }
}
