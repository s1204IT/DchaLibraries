package gov.nist.javax.sip.parser;

import gov.nist.core.Separators;
import gov.nist.core.Token;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.Warning;
import gov.nist.javax.sip.header.WarningList;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class WarningParser extends HeaderParser {
    public WarningParser(String str) {
        super(str);
    }

    protected WarningParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        WarningList warningList = new WarningList();
        if (debug) {
            dbg_enter("WarningParser.parse");
        }
        try {
            headerName(TokenTypes.WARNING);
            while (this.lexer.lookAhead(0) != '\n') {
                Warning warning = new Warning();
                warning.setHeaderName("Warning");
                this.lexer.match(4095);
                try {
                    try {
                        warning.setCode(Integer.parseInt(this.lexer.getNextToken().getTokenValue()));
                        this.lexer.SPorHT();
                        this.lexer.match(4095);
                        Token nextToken = this.lexer.getNextToken();
                        if (this.lexer.lookAhead(0) == ':') {
                            this.lexer.match(58);
                            this.lexer.match(4095);
                            warning.setAgent(nextToken.getTokenValue() + Separators.COLON + this.lexer.getNextToken().getTokenValue());
                        } else {
                            warning.setAgent(nextToken.getTokenValue());
                        }
                        this.lexer.SPorHT();
                        warning.setText(this.lexer.quotedString());
                        this.lexer.SPorHT();
                        warningList.add(warning);
                        while (this.lexer.lookAhead(0) == ',') {
                            this.lexer.match(44);
                            this.lexer.SPorHT();
                            Warning warning2 = new Warning();
                            this.lexer.match(4095);
                            try {
                                try {
                                    warning2.setCode(Integer.parseInt(this.lexer.getNextToken().getTokenValue()));
                                    this.lexer.SPorHT();
                                    this.lexer.match(4095);
                                    Token nextToken2 = this.lexer.getNextToken();
                                    if (this.lexer.lookAhead(0) == ':') {
                                        this.lexer.match(58);
                                        this.lexer.match(4095);
                                        warning2.setAgent(nextToken2.getTokenValue() + Separators.COLON + this.lexer.getNextToken().getTokenValue());
                                    } else {
                                        warning2.setAgent(nextToken2.getTokenValue());
                                    }
                                    this.lexer.SPorHT();
                                    warning2.setText(this.lexer.quotedString());
                                    this.lexer.SPorHT();
                                    warningList.add(warning2);
                                } catch (NumberFormatException e) {
                                    throw createParseException(e.getMessage());
                                }
                            } catch (InvalidArgumentException e2) {
                                throw createParseException(e2.getMessage());
                            }
                        }
                    } catch (InvalidArgumentException e3) {
                        throw createParseException(e3.getMessage());
                    }
                } catch (NumberFormatException e4) {
                    throw createParseException(e4.getMessage());
                }
            }
            return warningList;
        } finally {
            if (debug) {
                dbg_leave("WarningParser.parse");
            }
        }
    }
}
