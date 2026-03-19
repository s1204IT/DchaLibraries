package gov.nist.javax.sip.parser.extensions;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.extensions.Join;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;

public class JoinParser extends ParametersParser {
    public JoinParser(String str) {
        super(str);
    }

    protected JoinParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("parse");
        }
        try {
            headerName(TokenTypes.JOIN_TO);
            Join join = new Join();
            this.lexer.SPorHT();
            String strByteStringNoSemicolon = this.lexer.byteStringNoSemicolon();
            this.lexer.SPorHT();
            super.parse(join);
            join.setCallId(strByteStringNoSemicolon);
            return join;
        } finally {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }

    public static void main(String[] strArr) throws ParseException {
        String[] strArr2 = {"Join: 12345th5z8z\n", "Join: 12345th5z8z;to-tag=tozght6-45;from-tag=fromzght789-337-2\n"};
        for (int i = 0; i < strArr2.length; i++) {
            Join join = (Join) new JoinParser(strArr2[i]).parse();
            System.out.println("Parsing => " + strArr2[i]);
            System.out.print("encoded = " + join.encode() + "==> ");
            System.out.println("callId " + join.getCallId() + " from-tag=" + join.getFromTag() + " to-tag=" + join.getToTag());
        }
    }
}
