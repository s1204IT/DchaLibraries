package gov.nist.javax.sip.parser;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.StatusLine;
import java.text.ParseException;

public class StatusLineParser extends Parser {
    public StatusLineParser(String str) {
        this.lexer = new Lexer("status_lineLexer", str);
    }

    public StatusLineParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("status_lineLexer");
    }

    protected int statusCode() throws ParseException {
        String strNumber = this.lexer.number();
        if (debug) {
            dbg_enter("statusCode");
        }
        try {
            try {
                return Integer.parseInt(strNumber);
            } catch (NumberFormatException e) {
                throw new ParseException(this.lexer.getBuffer() + Separators.COLON + e.getMessage(), this.lexer.getPtr());
            }
        } finally {
            if (debug) {
                dbg_leave("statusCode");
            }
        }
    }

    protected String reasonPhrase() throws ParseException {
        return this.lexer.getRest().trim();
    }

    public StatusLine parse() throws ParseException {
        try {
            if (debug) {
                dbg_enter("parse");
            }
            StatusLine statusLine = new StatusLine();
            statusLine.setSipVersion(sipVersion());
            this.lexer.SPorHT();
            statusLine.setStatusCode(statusCode());
            this.lexer.SPorHT();
            statusLine.setReasonPhrase(reasonPhrase());
            this.lexer.SPorHT();
            return statusLine;
        } finally {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }
}
