package gov.nist.javax.sip.parser;

import gov.nist.core.Separators;
import gov.nist.core.Token;
import gov.nist.javax.sip.header.InReplyTo;
import gov.nist.javax.sip.header.InReplyToList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class InReplyToParser extends HeaderParser {
    public InReplyToParser(String str) {
        super(str);
    }

    protected InReplyToParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("InReplyToParser.parse");
        }
        InReplyToList inReplyToList = new InReplyToList();
        try {
            headerName(TokenTypes.IN_REPLY_TO);
            while (this.lexer.lookAhead(0) != '\n') {
                InReplyTo inReplyTo = new InReplyTo();
                inReplyTo.setHeaderName("In-Reply-To");
                this.lexer.match(4095);
                Token nextToken = this.lexer.getNextToken();
                if (this.lexer.lookAhead(0) == '@') {
                    this.lexer.match(64);
                    this.lexer.match(4095);
                    inReplyTo.setCallId(nextToken.getTokenValue() + Separators.AT + this.lexer.getNextToken().getTokenValue());
                } else {
                    inReplyTo.setCallId(nextToken.getTokenValue());
                }
                this.lexer.SPorHT();
                inReplyToList.add(inReplyTo);
                while (this.lexer.lookAhead(0) == ',') {
                    this.lexer.match(44);
                    this.lexer.SPorHT();
                    InReplyTo inReplyTo2 = new InReplyTo();
                    this.lexer.match(4095);
                    Token nextToken2 = this.lexer.getNextToken();
                    if (this.lexer.lookAhead(0) == '@') {
                        this.lexer.match(64);
                        this.lexer.match(4095);
                        inReplyTo2.setCallId(nextToken2.getTokenValue() + Separators.AT + this.lexer.getNextToken().getTokenValue());
                    } else {
                        inReplyTo2.setCallId(nextToken2.getTokenValue());
                    }
                    inReplyToList.add(inReplyTo2);
                }
            }
            return inReplyToList;
        } finally {
            if (debug) {
                dbg_leave("InReplyToParser.parse");
            }
        }
    }
}
