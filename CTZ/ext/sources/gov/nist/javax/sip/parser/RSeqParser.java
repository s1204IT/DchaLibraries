package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.RSeq;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class RSeqParser extends HeaderParser {
    public RSeqParser(String str) {
        super(str);
    }

    protected RSeqParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("RSeqParser.parse");
        }
        RSeq rSeq = new RSeq();
        try {
            headerName(TokenTypes.RSEQ);
            rSeq.setHeaderName("RSeq");
            try {
                rSeq.setSeqNumber(Long.parseLong(this.lexer.number()));
                this.lexer.SPorHT();
                this.lexer.match(10);
                return rSeq;
            } catch (InvalidArgumentException e) {
                throw createParseException(e.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("RSeqParser.parse");
            }
        }
    }
}
