package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.AcceptEncoding;
import gov.nist.javax.sip.header.AcceptEncodingList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class AcceptEncodingParser extends HeaderParser {
    public AcceptEncodingParser(String str) {
        super(str);
    }

    protected AcceptEncodingParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        AcceptEncodingList acceptEncodingList = new AcceptEncodingList();
        if (debug) {
            dbg_enter("AcceptEncodingParser.parse");
        }
        try {
            headerName(TokenTypes.ACCEPT_ENCODING);
            if (this.lexer.lookAhead(0) == '\n') {
                acceptEncodingList.add(new AcceptEncoding());
            } else {
                while (this.lexer.lookAhead(0) != '\n') {
                    AcceptEncoding acceptEncoding = new AcceptEncoding();
                    if (this.lexer.lookAhead(0) != ';') {
                        this.lexer.match(4095);
                        acceptEncoding.setEncoding(this.lexer.getNextToken().getTokenValue());
                    }
                    while (this.lexer.lookAhead(0) == ';') {
                        this.lexer.match(59);
                        this.lexer.SPorHT();
                        this.lexer.match(113);
                        this.lexer.SPorHT();
                        this.lexer.match(61);
                        this.lexer.SPorHT();
                        this.lexer.match(4095);
                        try {
                            acceptEncoding.setQValue(Float.parseFloat(this.lexer.getNextToken().getTokenValue()));
                            this.lexer.SPorHT();
                        } catch (NumberFormatException e) {
                            throw createParseException(e.getMessage());
                        } catch (InvalidArgumentException e2) {
                            throw createParseException(e2.getMessage());
                        }
                    }
                    acceptEncodingList.add(acceptEncoding);
                    if (this.lexer.lookAhead(0) == ',') {
                        this.lexer.match(44);
                        this.lexer.SPorHT();
                    }
                }
            }
            return acceptEncodingList;
        } finally {
            if (debug) {
                dbg_leave("AcceptEncodingParser.parse");
            }
        }
    }
}
