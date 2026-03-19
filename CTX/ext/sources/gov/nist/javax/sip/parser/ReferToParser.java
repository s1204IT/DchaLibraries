package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.AddressParametersHeader;
import gov.nist.javax.sip.header.ReferTo;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ReferToParser extends AddressParametersParser {
    public ReferToParser(String str) {
        super(str);
    }

    protected ReferToParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        headerName(TokenTypes.REFER_TO);
        ReferTo referTo = new ReferTo();
        super.parse((AddressParametersHeader) referTo);
        this.lexer.match(10);
        return referTo;
    }

    public static void main(String[] strArr) throws ParseException {
        for (String str : new String[]{"Refer-To: <sip:dave@denver.example.org?Replaces=12345%40192.168.118.3%3Bto-tag%3D12345%3Bfrom-tag%3D5FFE-3994>\n", "Refer-To: <sip:+1-650-555-2222@ss1.wcom.com;user=phone>;tag=5617\n", "Refer-To: T. A. Watson <sip:watson@bell-telephone.com>\n", "Refer-To: LittleGuy <sip:UserB@there.com>\n", "Refer-To: sip:mranga@120.6.55.9\n", "Refer-To: sip:mranga@129.6.55.9 ; tag=696928473514.129.6.55.9\n"}) {
            System.out.println("encoded = " + ((ReferTo) new ReferToParser(str).parse()).encode());
        }
    }
}
