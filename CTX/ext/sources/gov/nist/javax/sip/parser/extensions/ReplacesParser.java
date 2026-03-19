package gov.nist.javax.sip.parser.extensions;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.extensions.Replaces;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;

public class ReplacesParser extends ParametersParser {
    public ReplacesParser(String str) {
        super(str);
    }

    protected ReplacesParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("parse");
        }
        try {
            headerName(TokenTypes.REPLACES_TO);
            Replaces replaces = new Replaces();
            this.lexer.SPorHT();
            String strByteStringNoSemicolon = this.lexer.byteStringNoSemicolon();
            this.lexer.SPorHT();
            super.parse(replaces);
            replaces.setCallId(strByteStringNoSemicolon);
            return replaces;
        } finally {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }

    public static void main(String[] strArr) throws ParseException {
        String[] strArr2 = {"Replaces: 12345th5z8z\n", "Replaces: 12345th5z8z;to-tag=tozght6-45;from-tag=fromzght789-337-2\n"};
        for (int i = 0; i < strArr2.length; i++) {
            Replaces replaces = (Replaces) new ReplacesParser(strArr2[i]).parse();
            System.out.println("Parsing => " + strArr2[i]);
            System.out.print("encoded = " + replaces.encode() + "==> ");
            System.out.println("callId " + replaces.getCallId() + " from-tag=" + replaces.getFromTag() + " to-tag=" + replaces.getToTag());
        }
    }
}
