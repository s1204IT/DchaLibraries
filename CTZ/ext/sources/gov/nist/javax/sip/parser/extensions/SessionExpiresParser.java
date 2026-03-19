package gov.nist.javax.sip.parser.extensions;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.extensions.SessionExpires;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class SessionExpiresParser extends ParametersParser {
    public SessionExpiresParser(String str) {
        super(str);
    }

    protected SessionExpiresParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        SessionExpires sessionExpires = new SessionExpires();
        if (debug) {
            dbg_enter("parse");
        }
        try {
            headerName(TokenTypes.SESSIONEXPIRES_TO);
            try {
                try {
                    sessionExpires.setExpires(Integer.parseInt(this.lexer.getNextId()));
                    this.lexer.SPorHT();
                    super.parse(sessionExpires);
                    return sessionExpires;
                } catch (NumberFormatException e) {
                    throw createParseException("bad integer format");
                }
            } catch (InvalidArgumentException e2) {
                throw createParseException(e2.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }

    public static void main(String[] strArr) throws ParseException {
        for (String str : new String[]{"Session-Expires: 30\n", "Session-Expires: 45;refresher=uac\n"}) {
            SessionExpires sessionExpires = (SessionExpires) new SessionExpiresParser(str).parse();
            System.out.println("encoded = " + sessionExpires.encode());
            System.out.println("\ntime=" + sessionExpires.getExpires());
            if (sessionExpires.getParameter(SessionExpires.REFRESHER) != null) {
                System.out.println("refresher=" + sessionExpires.getParameter(SessionExpires.REFRESHER));
            }
        }
    }
}
