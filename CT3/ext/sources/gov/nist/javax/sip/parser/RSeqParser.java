package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.RSeq;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class RSeqParser extends HeaderParser {
    public RSeqParser(String rseq) {
        super(rseq);
    }

    protected RSeqParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("RSeqParser.parse");
        }
        RSeq rseq = new RSeq();
        try {
            headerName(TokenTypes.RSEQ);
            rseq.setHeaderName("RSeq");
            String number = this.lexer.number();
            try {
                rseq.setSeqNumber(Long.parseLong(number));
                this.lexer.SPorHT();
                this.lexer.match(10);
                return rseq;
            } catch (InvalidArgumentException ex) {
                throw createParseException(ex.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("RSeqParser.parse");
            }
        }
    }
}
