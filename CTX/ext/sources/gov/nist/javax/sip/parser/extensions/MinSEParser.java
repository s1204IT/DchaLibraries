package gov.nist.javax.sip.parser.extensions;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.extensions.MinSE;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class MinSEParser extends ParametersParser {
    public MinSEParser(String str) {
        super(str);
    }

    protected MinSEParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        MinSE minSE = new MinSE();
        if (debug) {
            dbg_enter("parse");
        }
        try {
            headerName(TokenTypes.MINSE_TO);
            try {
                try {
                    minSE.setExpires(Integer.parseInt(this.lexer.getNextId()));
                    this.lexer.SPorHT();
                    super.parse(minSE);
                    return minSE;
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
        for (String str : new String[]{"Min-SE: 30\n", "Min-SE: 45;some-param=somevalue\n"}) {
            MinSE minSE = (MinSE) new MinSEParser(str).parse();
            System.out.println("encoded = " + minSE.encode());
            System.out.println("\ntime=" + minSE.getExpires());
            if (minSE.getParameter("some-param") != null) {
                System.out.println("some-param=" + minSE.getParameter("some-param"));
            }
        }
    }
}
