package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.AcceptEncoding;
import gov.nist.javax.sip.header.AcceptEncodingList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class AcceptEncodingParser extends HeaderParser {
    public AcceptEncodingParser(String acceptEncoding) {
        super(acceptEncoding);
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
                        Token value = this.lexer.getNextToken();
                        acceptEncoding.setEncoding(value.getTokenValue());
                    }
                    while (this.lexer.lookAhead(0) == ';') {
                        this.lexer.match(59);
                        this.lexer.SPorHT();
                        this.lexer.match(113);
                        this.lexer.SPorHT();
                        this.lexer.match(61);
                        this.lexer.SPorHT();
                        this.lexer.match(4095);
                        Token value2 = this.lexer.getNextToken();
                        try {
                            try {
                                float qv = Float.parseFloat(value2.getTokenValue());
                                acceptEncoding.setQValue(qv);
                                this.lexer.SPorHT();
                            } catch (NumberFormatException ex) {
                                throw createParseException(ex.getMessage());
                            }
                        } catch (InvalidArgumentException ex2) {
                            throw createParseException(ex2.getMessage());
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
