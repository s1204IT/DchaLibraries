package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.Privacy;
import gov.nist.javax.sip.header.ims.PrivacyList;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;

public class PrivacyParser extends HeaderParser implements TokenTypes {
    public PrivacyParser(String str) {
        super(str);
    }

    protected PrivacyParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("PrivacyParser.parse");
        }
        PrivacyList privacyList = new PrivacyList();
        try {
            headerName(TokenTypes.PRIVACY);
            while (this.lexer.lookAhead(0) != '\n') {
                this.lexer.SPorHT();
                Privacy privacy = new Privacy();
                privacy.setHeaderName("Privacy");
                this.lexer.match(4095);
                privacy.setPrivacy(this.lexer.getNextToken().getTokenValue());
                this.lexer.SPorHT();
                privacyList.add(privacy);
                while (this.lexer.lookAhead(0) == ';') {
                    this.lexer.match(59);
                    this.lexer.SPorHT();
                    Privacy privacy2 = new Privacy();
                    this.lexer.match(4095);
                    privacy2.setPrivacy(this.lexer.getNextToken().getTokenValue());
                    this.lexer.SPorHT();
                    privacyList.add(privacy2);
                }
            }
            return privacyList;
        } finally {
            if (debug) {
                dbg_leave("PrivacyParser.parse");
            }
        }
    }

    public static void main(String[] strArr) throws ParseException {
        for (String str : new String[]{"Privacy: none\n", "Privacy: none;id;user\n"}) {
            System.out.println("encoded = " + ((PrivacyList) new PrivacyParser(str).parse()).encode());
        }
    }
}
