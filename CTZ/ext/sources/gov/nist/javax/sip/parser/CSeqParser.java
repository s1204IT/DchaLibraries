package gov.nist.javax.sip.parser;

import gov.nist.core.Debug;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.message.SIPRequest;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class CSeqParser extends HeaderParser {
    public CSeqParser(String str) {
        super(str);
    }

    protected CSeqParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        try {
            CSeq cSeq = new CSeq();
            this.lexer.match(TokenTypes.CSEQ);
            this.lexer.SPorHT();
            this.lexer.match(58);
            this.lexer.SPorHT();
            cSeq.setSeqNumber(Long.parseLong(this.lexer.number()));
            this.lexer.SPorHT();
            cSeq.setMethod(SIPRequest.getCannonicalName(method()));
            this.lexer.SPorHT();
            this.lexer.match(10);
            return cSeq;
        } catch (NumberFormatException e) {
            Debug.printStackTrace(e);
            throw createParseException("Number format exception");
        } catch (InvalidArgumentException e2) {
            Debug.printStackTrace(e2);
            throw createParseException(e2.getMessage());
        }
    }
}
